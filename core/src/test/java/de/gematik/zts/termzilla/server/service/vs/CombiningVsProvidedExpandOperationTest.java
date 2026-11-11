/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 */
package de.gematik.zts.termzilla.server.service.vs;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.ResourceOrigin;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link CombiningVsProvidedExpandOperation}.
 *
 * <p>Covers dependency-graph behavior of the core expansion engine, in particular cycle detection.
 */
@DisplayName("CombiningVsProvidedExpandOperation Tests")
class CombiningVsProvidedExpandOperationTest {

  private static final String SCOPE = "fhir";
  private static final String VS_A_URL = "http://example.org/fhir/ValueSet/vs-a";
  private static final String VS_B_URL = "http://example.org/fhir/ValueSet/vs-b";
  private static final String VERSION = "1.0.0";

  private CombiningTerminologyService terminologyService;

  @BeforeEach
  void setUp() throws InvalidConfigurationException {
    ConfigProperties properties = new ConfigProperties();
    properties.setSystemLanguage("de-DE");
    properties.setBaseWorkingDir("/tmp/termzilla-test");
    terminologyService = new CombiningTerminologyService(List.of(), properties);
  }

  @Nested
  @DisplayName("Cycle Detection Tests")
  class CycleDetectionTests {

    @Test
    @DisplayName("Should throw exception when two ValueSets reference each other")
    void shouldThrowExceptionWhenValueSetDependencyIsCyclic()
        throws UnknownScopeException, UnsupportedTerminologyException {
      // GIVEN: VS-A includes VS-B, VS-B includes VS-A
      ValueSet vsA = createValueSetReferencing(VS_A_URL, VS_B_URL);
      ValueSet vsB = createValueSetReferencing(VS_B_URL, VS_A_URL);

      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);
      when(vsSvc.getValueSet(SCOPE, VS_A_URL, VERSION)).thenReturn(vsA);
      when(vsSvc.getValueSet(SCOPE, VS_B_URL, VERSION)).thenReturn(vsB);

      terminologyService.getTerminologyManagerValueSetMap().put(VS_A_URL, manager);
      terminologyService.getTerminologyManagerValueSetMap().put(VS_B_URL, manager);

      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(vsA).build();
      final var languageDefault = terminologyService.getLanguageSystemDefault();
      // WHEN / THEN: cyclic dependency must be detected, not cause a StackOverflowError
      assertThatThrownBy(
              () ->
                  CombiningVsProvidedExpandOperation.expandExplicitValueSet(
                      terminologyService, parameters, SCOPE, languageDefault))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Cyclic");
    }

    @Test
    @DisplayName("Should throw exception when a ValueSet references itself")
    void shouldThrowExceptionWhenValueSetReferencesItself()
        throws UnknownScopeException, UnsupportedTerminologyException {
      // GIVEN: VS-A includes VS-A
      ValueSet vsA = createValueSetReferencing(VS_A_URL, VS_A_URL);

      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);
      when(vsSvc.getValueSet(SCOPE, VS_A_URL, VERSION)).thenReturn(vsA);

      terminologyService.getTerminologyManagerValueSetMap().put(VS_A_URL, manager);

      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(vsA).build();
      final var languageDefault = terminologyService.getLanguageSystemDefault();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  CombiningVsProvidedExpandOperation.expandExplicitValueSet(
                      terminologyService, parameters, SCOPE, languageDefault))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Cyclic");
    }
  }

  @Nested
  @DisplayName("Exclude System Tests")
  class ExcludeSystemTests {

    @Test
    @DisplayName("Should remove concepts of the excluded system from the expansion")
    void shouldRemoveConceptsOfExcludedSystem() throws Exception {
      // GIVEN: a ValueSet including two systems, and exclude-system removing one of them
      String csA = "http://example.org/fhir/CodeSystem/cs-a";
      String csB = "http://example.org/fhir/CodeSystem/cs-b";

      ValueSet vs = new ValueSet();
      vs.setUrl("http://example.org/fhir/ValueSet/vs");
      vs.setVersion(VERSION);
      ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
      compose.addInclude(new ValueSet.ConceptSetComponent().setSystem(csA).setVersion(VERSION));
      compose.addInclude(new ValueSet.ConceptSetComponent().setSystem(csB).setVersion(VERSION));
      vs.setCompose(compose);

      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);

      VsExpandPartResult resA = resultWith(csA, "A1");
      VsExpandPartResult resB = resultWith(csB, "B1");
      when(vsSvc.expandInclude(
              Mockito.eq(SCOPE),
              Mockito.argThat(c -> csA.equals(c.getSystem())),
              Mockito.any()))
          .thenReturn(resA);
      when(vsSvc.expandInclude(
              Mockito.eq(SCOPE),
              Mockito.argThat(c -> csB.equals(c.getSystem())),
              Mockito.any()))
          .thenReturn(resB);

      terminologyService.getTerminologyManagerCodeSystemMap().put(csA, manager);
      terminologyService.getTerminologyManagerCodeSystemMap().put(csB, manager);

      // Stub the contains-component creation so the expansion actually contains entries.
      when(vsSvc.createExpansionContainsComponent(
              Mockito.any(),
              Mockito.any(),
              Mockito.anyBoolean(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any()))
          .thenAnswer(
              inv -> {
                ConceptBaseInfo info = inv.getArgument(1);
                return new ValueSet.ValueSetExpansionContainsComponent()
                    .setSystem(info.getSystem())
                    .setVersion(info.getVersion())
                    .setCode(info.getCode());
              });

      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .excludeSystemParam(List.of(new CanonicalType(csB + "|" + VERSION)))
              .build();

      // WHEN
      ValueSet expanded =
          CombiningVsProvidedExpandOperation.expandExplicitValueSet(
              terminologyService, parameters, SCOPE, terminologyService.getLanguageSystemDefault());

      // THEN: only the csA concept remains; csB was removed by exclude-system
      org.assertj.core.api.Assertions.assertThat(expanded.getExpansion().getContains())
          .hasSize(1)
          .allSatisfy(c -> Assertions.assertThat(c.getSystem()).isEqualTo(csA));
    }

    private VsExpandPartResult resultWith(String system, String code) {
      VsExpandPartResult r = new VsExpandPartResult();
      r.getConceptBaseInfos()
          .add(
              ConceptBaseInfo.builder()
                  .system(system)
                  .version(VERSION)
                  .code(code)
                  .active(true)
                  .build());
      return r;
    }
  }

  @Nested
  @DisplayName("Use Supplement Tests")
  class UseSupplementTests {

    private static final String CS_URL = "http://example.org/fhir/CodeSystem/cs";
    private static final String SUPPLEMENT_URL = "http://example.org/fhir/CodeSystem/cs-de";

    @Test
    @DisplayName("Should reject a malformed useSupplement canonical")
    void shouldRejectMalformedUseSupplement() {
      // GIVEN
      ValueSet vs = simpleValueSet();
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .useSupplementParam(
                  List.of(new org.hl7.fhir.r4.model.CanonicalType("not-a-canonical")))
              .build();

      final var languageDefault = terminologyService.getLanguageSystemDefault();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  CombiningVsProvidedExpandOperation.expandExplicitValueSet(
                      terminologyService, parameters, SCOPE, languageDefault))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("useSupplement")
          .hasMessageContaining("malformed");
    }

    @Test
    @DisplayName("Should reject a useSupplement that cannot be resolved")
    void shouldRejectUnresolvableUseSupplement() {
      // GIVEN: no terminology manager knows the supplement
      ValueSet vs = simpleValueSet();
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .useSupplementParam(List.of(new CanonicalType(SUPPLEMENT_URL + "|1.0.0")))
              .build();

      final var languageDefault = terminologyService.getLanguageSystemDefault();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  CombiningVsProvidedExpandOperation.expandExplicitValueSet(
                      terminologyService, parameters, SCOPE, languageDefault))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("could not be resolved");
    }

    @Test
    @DisplayName("Should reject conflicting versions of the same supplement")
    void shouldRejectConflictingSupplementVersions() throws Exception {
      // GIVEN: two useSupplement values for the same canonical with different versions
      ValueSet vs = simpleValueSet();

      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class);
      CodeSystemSvcInterface csSvc = mock(CodeSystemSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);
      when(manager.cs()).thenReturn(csSvc);

      final var supplementV1 = supplement("1.0.0");
      final var supplementV2 = supplement("2.0.0");
      when(csSvc.getCodeSystem(SCOPE, SUPPLEMENT_URL, "1.0.0")).thenReturn(supplementV1);
      when(csSvc.getCodeSystem(SCOPE, SUPPLEMENT_URL, "2.0.0")).thenReturn(supplementV2);

      terminologyService.getTerminologyManagerCodeSystemMap().put(CS_URL, manager);
      terminologyService.getTerminologyManagerCodeSystemMap().put(SUPPLEMENT_URL, manager);

      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .useSupplementParam(
                  List.of(
                      new CanonicalType(SUPPLEMENT_URL + "|1.0.0"),
                      new CanonicalType(SUPPLEMENT_URL + "|2.0.0")))
              .build();

      final var languageDefault = terminologyService.getLanguageSystemDefault();
      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  CombiningVsProvidedExpandOperation.expandExplicitValueSet(
                      terminologyService, parameters, SCOPE, languageDefault))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Conflicting versions of the same supplement");
    }

    @Test
    @DisplayName("Should apply a valid useSupplement and record it in expansion.parameter")
    void shouldApplyAndRecordValidUseSupplement() throws Exception {
      // GIVEN: a ValueSet over CS_URL and a resolvable supplement
      ValueSet vs = simpleValueSet();

      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface vsSvc = mock(ValueSetSvcInterface.class);
      CodeSystemSvcInterface csSvc = mock(CodeSystemSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);
      when(manager.cs()).thenReturn(csSvc);

      CodeSystem supplement = supplement("1.0.0");
      when(csSvc.getCodeSystem(SCOPE, SUPPLEMENT_URL, "1.0.0")).thenReturn(supplement);

      // The base system include expands to one concept
      VsExpandPartResult result = new VsExpandPartResult();
      result
          .getConceptBaseInfos()
          .add(
              ConceptBaseInfo.builder()
                  .system(CS_URL)
                  .version(VERSION)
                  .code("A1")
                  .active(true)
                  .build());
      when(vsSvc.expandInclude(
              Mockito.eq(SCOPE),
              Mockito.any(),
              Mockito.any()))
          .thenReturn(result);
      when(vsSvc.createExpansionContainsComponent(
              Mockito.any(),
              Mockito.any(),
              Mockito.anyBoolean(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any()))
          .thenAnswer(
              inv -> {
                ConceptBaseInfo info =
                    inv.getArgument(1);
                return new ValueSet.ValueSetExpansionContainsComponent()
                    .setSystem(info.getSystem())
                    .setVersion(info.getVersion())
                    .setCode(info.getCode());
              });

      terminologyService.getTerminologyManagerCodeSystemMap().put(CS_URL, manager);
      terminologyService.getTerminologyManagerCodeSystemMap().put(SUPPLEMENT_URL, manager);

      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .useSupplementParam(List.of(new CanonicalType(SUPPLEMENT_URL + "|1.0.0")))
              .build();

      // WHEN
      ValueSet expanded =
          CombiningVsProvidedExpandOperation.expandExplicitValueSet(
              terminologyService, parameters, SCOPE, terminologyService.getLanguageSystemDefault());

      // THEN: the supplement is recorded as '[base]#supplement' = 'supplement|version'
      Assertions.assertThat(expanded.getExpansion().getParameter())
          .anySatisfy(
              p -> {
                Assertions.assertThat(p.getName()).isEqualTo(CS_URL + "#supplement");
                org.assertj.core.api.Assertions.assertThat(p.getValue().primitiveValue())
                    .isEqualTo(SUPPLEMENT_URL + "|1.0.0");
              });
    }

    private ValueSet simpleValueSet() {
      ValueSet vs = new ValueSet();
      vs.setUrl("http://example.org/fhir/ValueSet/vs");
      vs.setVersion(VERSION);
      ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
      compose.addInclude(new ValueSet.ConceptSetComponent().setSystem(CS_URL).setVersion(VERSION));
      vs.setCompose(compose);
      return vs;
    }

    private org.hl7.fhir.r4.model.CodeSystem supplement(String version) {
      final var supplement = new CodeSystem();
      supplement.setUrl(SUPPLEMENT_URL);
      supplement.setVersion(version);
      supplement.setContent(CodeSystem.CodeSystemContentMode.SUPPLEMENT);
      supplement.setSupplements(CS_URL);
      return supplement;
    }
  }

  @Nested
  @DisplayName("Processing Context Tests")
  class ProcessingContextTests {

    private static final String CS_URL = "http://example.org/fhir/CodeSystem/cs";

    private ValueSetSvcInterface vsSvc;

    @BeforeEach
    void setUpCodeSystemManager() throws Exception {
      TerminologyManager manager = mock(TerminologyManager.class);
      vsSvc = mock(ValueSetSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(vsSvc);
      when(vsSvc.expandInclude(
              Mockito.any(),
              Mockito.any(),
              Mockito.any()))
          .thenAnswer(_ -> new VsExpandPartResult());
      terminologyService.getTerminologyManagerCodeSystemMap().put(CS_URL, manager);
    }

    @Test
    @DisplayName("Should expand includes and excludes of a provided ValueSet as provided")
    void shouldExpandProvidedValueSetAsProvided() throws Exception {
      // GIVEN: the origin is not set explicitly, so the default applies
      ValueSet vs = valueSetWithSystemIncludeAndExclude();
      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(vs).build();

      // WHEN
      CombiningVsProvidedExpandOperation.expandExplicitValueSet(
          terminologyService, parameters, SCOPE, terminologyService.getLanguageSystemDefault());

      // THEN
      verifyExpandIncludeCalls(2, ProcessingContext.provided());
      verifyExpandIncludeCalls(0, ProcessingContext.stored());
    }

    @Test
    @DisplayName("Should expand includes and excludes of a stored ValueSet as stored")
    void shouldExpandStoredValueSetAsStored() throws Exception {
      // GIVEN
      ValueSet vs = valueSetWithSystemIncludeAndExclude();
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .resourceOrigin(ResourceOrigin.STORED)
              .build();

      // WHEN
      CombiningVsProvidedExpandOperation.expandExplicitValueSet(
          terminologyService, parameters, SCOPE, terminologyService.getLanguageSystemDefault());

      // THEN
      verifyExpandIncludeCalls(2, ProcessingContext.stored());
      verifyExpandIncludeCalls(0, ProcessingContext.provided());
    }

    @Test
    @DisplayName("Should expand a stored ValueSet referenced by a provided ValueSet as stored")
    void shouldExpandReferencedValueSetAsStored() throws Exception {
      // GIVEN: a provided VS-A references the stored VS-B, which includes the code system
      ValueSet vsA = createValueSetReferencing(VS_A_URL, VS_B_URL);
      ValueSet vsB = valueSetWithSystemInclude(VS_B_URL);
      registerStoredValueSet(VS_B_URL, vsB);

      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(vsA).build();

      // WHEN
      CombiningVsProvidedExpandOperation.expandExplicitValueSet(
          terminologyService, parameters, SCOPE, terminologyService.getLanguageSystemDefault());

      // THEN
      verifyExpandIncludeCalls(1, ProcessingContext.stored());
      verifyExpandIncludeCalls(0, ProcessingContext.provided());
    }

    @Test
    @DisplayName("Should expand a ValueSet requested by url as stored")
    void shouldExpandValueSetRequestedByUrlAsStored() throws Exception {
      // GIVEN
      registerStoredValueSet(VS_A_URL, valueSetWithSystemInclude(VS_A_URL));
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .urlParam(new UriType(VS_A_URL))
              .valueSetVersionParam(new StringType(VERSION))
              .build();

      // WHEN
      terminologyService.expandValueSet(SCOPE, parameters);

      // THEN
      Assertions.assertThat(parameters.getResourceOrigin()).isEqualTo(ResourceOrigin.STORED);
      verifyExpandIncludeCalls(1, ProcessingContext.stored());
      verifyExpandIncludeCalls(0, ProcessingContext.provided());
    }

    private void verifyExpandIncludeCalls(int times, ProcessingContext processingContext)
        throws Exception {
      verify(vsSvc, times(times))
          .expandInclude(
              Mockito.any(),
              Mockito.any(),
              Mockito.argThat(
                  (VsExpandIncludeParameter p) ->
                      processingContext.equals(p.processingContext())));
    }

    private void registerStoredValueSet(String url, ValueSet valueSet) throws Exception {
      TerminologyManager manager = mock(TerminologyManager.class);
      ValueSetSvcInterface storedVsSvc = mock(ValueSetSvcInterface.class);
      when(manager.isInitialized()).thenReturn(true);
      when(manager.vs()).thenReturn(storedVsSvc);
      when(storedVsSvc.getValueSet(SCOPE, url, VERSION)).thenReturn(valueSet);
      terminologyService.getTerminologyManagerValueSetMap().put(url, manager);
    }

    private ValueSet valueSetWithSystemInclude(String url) {
      ValueSet vs = new ValueSet();
      vs.setUrl(url);
      vs.setVersion(VERSION);
      vs.getCompose()
          .addInclude(new ValueSet.ConceptSetComponent().setSystem(CS_URL).setVersion(VERSION));
      return vs;
    }

    private ValueSet valueSetWithSystemIncludeAndExclude() {
      ValueSet vs = valueSetWithSystemInclude(VS_A_URL);
      ValueSet.ConceptSetComponent exclude =
          new ValueSet.ConceptSetComponent().setSystem(CS_URL).setVersion(VERSION);
      exclude.addConcept().setCode("excluded");
      vs.getCompose().addExclude(exclude);
      return vs;
    }
  }

  /**
   * Creates a ValueSet whose compose includes a reference to another ValueSet.
   *
   * @param ownUrl the canonical URL of the created ValueSet
   * @param referencedUrl the canonical URL of the referenced ValueSet
   * @return the ValueSet fixture
   */
  private static ValueSet createValueSetReferencing(String ownUrl, String referencedUrl) {
    ValueSet valueSet = new ValueSet();
    valueSet.setUrl(ownUrl);
    valueSet.setVersion(VERSION);
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.addValueSet(referencedUrl + "|" + VERSION);
    compose.addInclude(include);
    valueSet.setCompose(compose);
    return valueSet;
  }
}
