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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.CodingConceptProperty;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.graph.FhirTermGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link FhirTermsCodeSystemLookupOperation}.
 *
 * <p>Verifies parameter validation, code lookup result states, language-aware display selection,
 * designation collection, and property handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FhirTermsCodeSystemLookupOperation Tests")
class FhirTermsCodeSystemLookupOperationTest {

  private static final String SCOPE = "fhir";
  private static final String SYSTEM = "http://example.org/CodeSystem/test";
  private static final String VERSION = "1.0.0";
  private static final String CODE = "A1";
  private static final String CS_LANGUAGE = "en";
  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";

  @Mock private FhirTermsTerminologyManager terminologyManager;
  @Mock private FhirTermsInMemoryRepresentation inMemoryRepresentation;
  @Mock private FhirTermGraphRepresentation mockGraph;

  private FhirTermsCodeSystemLookupOperation lookupOperation;

  @BeforeEach
  void setUp() {
    when(terminologyManager.getFhirTermsInMemoryRepresentation())
        .thenReturn(inMemoryRepresentation);
    lookupOperation = new FhirTermsCodeSystemLookupOperation(terminologyManager);
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  private static CodeSystem createCodeSystem() {
    CodeSystem cs = new CodeSystem();
    cs.setId("test-cs");
    cs.setUrl(SYSTEM);
    cs.setName("TestCodeSystem");
    cs.setVersion(VERSION);
    cs.setLanguage(CS_LANGUAGE);
    cs.setCaseSensitive(true);
    return cs;
  }

  private static CodeSystem.ConceptDefinitionComponent createConcept(String code, String display) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode(code);
    if (display != null) {
      concept.setDisplay(display);
    }
    return concept;
  }

  private static CodeSystem.ConceptDefinitionDesignationComponent createDesignation(
      String language, String useSystem, String useCode, String value) {
    CodeSystem.ConceptDefinitionDesignationComponent designation =
        new CodeSystem.ConceptDefinitionDesignationComponent();
    if (language != null) {
      designation.setLanguage(language);
    }
    designation.setUse(new Coding(useSystem, useCode, null));
    designation.setValue(value);
    return designation;
  }

  /**
   * Builds a minimal lookup parameter with system and code only and no explicit property
   * constraints. When no properties are specified, the server expands the requested set to the
   * default: {@code url}, {@code inactive}, {@code parent}, {@code child}, {@code designation}, and
   * all properties declared on the CodeSystem.
   */
  private static CsLookupParameter buildParam(String system, String code) {
    return CsLookupParameter.builder()
        .systemParam(new UriType(system))
        .codeParam(new CodeType(code))
        .languageDefaultSystemLanguage(LanguageParameter.parse(CS_LANGUAGE))
        .build();
  }

  /**
   * Builds a lookup parameter that explicitly requests the given properties. When properties are
   * specified, the default "url" property is NOT added automatically.
   */
  private static CsLookupParameter buildParamWithProperties(
      String system, String code, String... properties) {
    return CsLookupParameter.builder()
        .systemParam(new UriType(system))
        .codeParam(new CodeType(code))
        .languageDefaultSystemLanguage(LanguageParameter.parse(CS_LANGUAGE))
        .propertyParam(java.util.Arrays.stream(properties).map(CodeType::new).toList())
        .build();
  }

  private static CsLookupParameter buildParamWithDisplayLanguage(
      String system, String code, String displayLanguage) {
    return CsLookupParameter.builder()
        .systemParam(new UriType(system))
        .codeParam(new CodeType(code))
        .languageDefaultSystemLanguage(LanguageParameter.parse(CS_LANGUAGE))
        .displayLanguageParam(new CodeType(displayLanguage))
        .build();
  }

  private static CsLookupParameter buildParamWithAcceptLanguage(
      String system, String code, String acceptLanguage) {
    return CsLookupParameter.builder()
        .systemParam(new UriType(system))
        .codeParam(new CodeType(code))
        .languageDefaultSystemLanguage(LanguageParameter.parse(CS_LANGUAGE))
        .languageAcceptLanguageHeader(LanguageParameter.parse(acceptLanguage))
        .build();
  }

  private void stubCodeSystem(CodeSystem codeSystem)
      throws UnsupportedTerminologyException, UnknownScopeException {
    doReturn(codeSystem)
        .when(inMemoryRepresentation)
        .getResourceByUrlAndVersion(
            CodeSystem.class, SCOPE, SYSTEM, null, ResourceRetrievalMode.FULL);
  }

  // ==========================================================================
  // Test classes
  // ==========================================================================

  @Nested
  @DisplayName("Parameter validation")
  class ParameterValidationTests {

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when neither system nor coding parameter is present")
    void shouldThrowWhenNeitherSystemNorCodingPresent() {
      // GIVEN
      CsLookupParameter param = CsLookupParameter.builder().build();

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(SCOPE, param))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Neither system nor coding parameter is present.");
    }

    @Test
    @DisplayName("Should delegate to system+code path when coding parameter is used")
    void shouldDelegateToSystemCodePathWhenCodingParamIsUsed()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param =
          CsLookupParameter.builder().codingParam(new Coding(SYSTEM, CODE, null)).build();

      // WHEN — coding fields are extracted and delegated to the system+code path
      param.setLanguageDefaultSystemLanguage(LanguageParameter.parse(CS_LANGUAGE));
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.isFound()).isTrue();
      assertThat(result.getSearchedForCode()).isEqualTo(CODE);
    }
  }

  @Nested
  @DisplayName("Code not found")
  class LookupCodeNotFoundTests {

    @Test
    @DisplayName("Should throw UnknownConceptException when code does not exist in the CodeSystem")
    void shouldThrowUnknownConceptExceptionWhenCodeNotInCodeSystem()
        throws UnsupportedTerminologyException, UnknownScopeException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // no concepts
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, "UNKNOWN_CODE");

      // WHEN / THEN
      assertThatThrownBy(() -> lookupOperation.lookupCode(SCOPE, param))
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  @Nested
  @DisplayName("CodeSystem name resolution")
  class CodeSystemNameTests {

    @Test
    @DisplayName("Should use CodeSystem name as display name when name is present")
    void shouldUseCsNameAsDisplayName()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setName("MyCodeSystem");
      codeSystem.setTitle("My CS Title");
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeSystemDisplayName()).isEqualTo("MyCodeSystem");
    }

    @Test
    @DisplayName("Should use CodeSystem title when name is absent")
    void shouldUseTitleWhenNameAbsent()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setName(null);
      codeSystem.setTitle("My CS Title");
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeSystemDisplayName()).isEqualTo("My CS Title");
    }

    @Test
    @DisplayName("Should use CodeSystem URL when neither name nor title is present")
    void shouldUseUrlWhenNeitherNameNorTitlePresent()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setName(null);
      codeSystem.setTitle(null);
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeSystemDisplayName()).isEqualTo(SYSTEM);
    }
  }

  @Nested
  @DisplayName("Version handling")
  class VersionTests {

    @Test
    @DisplayName("Should set version from CodeSystem when it provides one")
    void shouldSetVersionFromCodeSystem()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setVersion(VERSION);
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeSystemVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Should not set version when CodeSystem has no version")
    void shouldNotSetVersionWhenCodeSystemHasNoVersion()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setVersion(null);
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeSystemVersion()).isNull();
    }
  }

  @Nested
  @DisplayName("Display selection")
  class DisplaySelectionTests {

    @Test
    @DisplayName("Should return display in CodeSystem language when resolved via system default")
    void shouldReturnDisplayInSystemDefaultLanguage()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParam(SYSTEM, CODE);

      // WHEN — system default = "en", which matches CS language
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo("English Display");
    }

    @Test
    @DisplayName(
        "Should return display in matching displayLanguage when a designation for that language exists")
    void shouldReturnDisplayInMatchingDisplayLanguage()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "English Display");
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithDisplayLanguage(SYSTEM, CODE, "de");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo("Deutsches Display");
    }

    @Test
    @DisplayName(
        "Should return null display when displayLanguage does not match any designation (strict)")
    void shouldReturnNullDisplayWhenDisplayLanguageDoesNotMatch()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — only English display, no German designation
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithDisplayLanguage(SYSTEM, CODE, "de");

      // WHEN — displayLanguage is strict: no fallback if not found
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeDisplay()).isNull();
    }

    @Test
    @DisplayName(
        "Should return display via Accept-Language header when a matching designation exists")
    void shouldReturnDisplayViaAcceptLanguageHeader()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "English Display");
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithAcceptLanguage(SYSTEM, CODE, "de");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo("Deutsches Display");
    }

    @Test
    @DisplayName(
        "Should fall back to system default language when Accept-Language has no matching designation")
    void shouldFallBackToSystemDefaultWhenAcceptLanguageHasNoMatch()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — only English display, no German designation
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithAcceptLanguage(SYSTEM, CODE, "de");

      // WHEN — Accept-Language "de" has no match, falls through to system default "en"
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getCodeDisplay()).isEqualTo("English Display");
    }
  }

  @Nested
  @DisplayName("Designation collection")
  class DesignationTests {

    @Test
    @DisplayName(
        "Should include the CS display as a designation with display when 'designation' is requested")
    void shouldIncludeCsDisplayAsDesignation()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "designation");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getDesignations())
          .hasSize(1)
          .extracting(
              ConceptDesignation::getLanguage,
              ConceptDesignation::getUseSystem,
              ConceptDesignation::getUseCode,
              ConceptDesignation::getValue)
          .containsExactly(
              tuple(CS_LANGUAGE, null, null, "English Display"));
    }

    @Test
    @DisplayName("Should include explicit CS concept designations when 'designation' is requested")
    void shouldIncludeExplicitCsDesignations()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — concept has no display, only an explicit designation
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.setLanguage(null);
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, null);
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "designation");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getDesignations())
          .hasSize(1)
          .extracting(
              ConceptDesignation::getLanguage,
              ConceptDesignation::getUseCode,
              ConceptDesignation::getValue)
          .containsExactly(tuple("de", DISPLAY_USE_CODE, "Deutsches Display"));
    }

    @Test
    @DisplayName(
        "Should include both the CS display designation and explicit designations in order when 'designation' is requested")
    void shouldIncludeBothCsDisplayAndExplicitDesignations()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "English Display");
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "designation");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — CS display first, then explicit designations
      assertThat(result.getDesignations())
          .hasSize(2)
          .extracting(ConceptDesignation::getValue)
          .containsExactly("English Display", "Deutsches Display");
    }

    @Test
    @DisplayName(
        "Should return empty designation list when 'designation' is requested but concept has none")
    void shouldReturnEmptyDesignationListWhenConceptHasNone()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, null));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "designation");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getDesignations()).isEmpty();
    }

    @Test
    @DisplayName(
        "Should use CS language as fallback when explicit designation has no language and 'designation' is requested")
    void shouldUseCsLanguageFallbackForDesignationWithoutLanguage()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, null);
      concept
          .getDesignation()
          .add(
              createDesignation(
                  null, DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Nameless Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "designation");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — fallback to CS language "en"
      assertThat(result.getDesignations())
          .hasSize(1)
          .extracting(ConceptDesignation::getLanguage)
          .containsExactly(CS_LANGUAGE);
    }

    @Test
    @DisplayName("Should return only designations matching the requested 'lang.X' language code")
    void shouldFilterDesignationsByLangXProperty()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "English Display");
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "lang.de");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — only German designation is returned; the English display does not match "de"
      assertThat(result.getDesignations())
          .hasSize(1)
          .extracting(ConceptDesignation::getLanguage, ConceptDesignation::getValue)
          .containsExactly(tuple("de", "Deutsches Display"));
    }

    @Test
    @DisplayName("Should return all designations when the 'lang.X' language code is not supported")
    void shouldReturnAllDesignationsWhenLangXLanguageIsUnsupported()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // language = "en"
      codeSystem.getConcept().add(createConcept(CODE, "English Display"));
      stubCodeSystem(codeSystem);
      // "xx-XX" is not a known BCP 47 language tag in the LanguageRegistry
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "lang.xx-XX");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — unsupported language is filtered out; langCodes is empty → all designations returned
      assertThat(result.getDesignations())
          .hasSize(1)
          .extracting(ConceptDesignation::getValue)
          .containsExactly("English Display");
    }

    @Test
    @DisplayName(
        "Should not include designations when neither 'designation' nor any 'lang.X' property is requested")
    void shouldNotIncludeDesignationsWhenNeitherDesignationNorLangXIsRequested()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "English Display");
      concept
          .getDesignation()
          .add(
              createDesignation(
                  "de", DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, "Deutsches Display"));
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "url");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getDesignations()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Property handling")
  class PropertyTests {

    @Test
    @DisplayName("Should include the 'url' property with the CodeSystem URL by default")
    void shouldIncludeUrlPropertyByDefault()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param =
          buildParam(SYSTEM, CODE); // no propertyParam → default set is expanded

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "url".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(BaseTypeConceptProperty.class)
          .extracting(p -> ((BaseTypeConceptProperty) p).getValue())
          .isInstanceOf(StringType.class)
          .extracting(s -> ((StringType) s).getValue())
          .isEqualTo(SYSTEM);
    }

    @Test
    @DisplayName(
        "Should include 'definition' property when it is requested and the concept has a definition")
    void shouldIncludeDefinitionPropertyWhenRequestedAndConceptHasDefinition()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      concept.setDefinition("A detailed definition of concept A1.");
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "definition");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "definition".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(BaseTypeConceptProperty.class)
          .extracting(p -> ((BaseTypeConceptProperty) p).getValue())
          .isInstanceOf(StringType.class)
          .extracting(s -> ((StringType) s).getValue())
          .isEqualTo("A detailed definition of concept A1.");
    }

    @Test
    @DisplayName(
        "Should not include 'definition' property when it is requested but the concept has no definition")
    void shouldNotIncludeDefinitionPropertyWhenConceptHasNoDefinition()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "Display")); // no definition
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "definition");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties()).noneMatch(p -> "definition".equals(p.getPropertyName()));
    }

    @Test
    @DisplayName("Should not include 'definition' property when it is not requested")
    void shouldNotIncludeDefinitionPropertyWhenNotRequested()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      concept.setDefinition("A detailed definition.");
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param =
          buildParam(
              SYSTEM,
              CODE); // empty propertyParam → default set expanded, does not include "definition"

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties()).noneMatch(p -> "definition".equals(p.getPropertyName()));
    }
  }

  @Nested
  @DisplayName("Concept property handling")
  class ConceptPropertyTests {

    @Test
    @DisplayName("Should include an explicit string concept property when it is requested")
    void shouldIncludeExplicitStringPropertyWhenRequested()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      CodeSystem.ConceptPropertyComponent property = new CodeSystem.ConceptPropertyComponent();
      property.setCode("customProp");
      property.setValue(new StringType("customValue"));
      concept.getProperty().add(property);
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "customProp");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "customProp".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(BaseTypeConceptProperty.class)
          .extracting(p -> ((BaseTypeConceptProperty) p).getValue())
          .isInstanceOf(StringType.class)
          .extracting(s -> ((StringType) s).getValue())
          .isEqualTo("customValue");
    }

    @Test
    @DisplayName("Should include a Coding concept property as CodingConceptProperty when requested")
    void shouldIncludeCodingPropertyAsConceptProperty()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      CodeSystem.ConceptPropertyComponent property = new CodeSystem.ConceptPropertyComponent();
      property.setCode("myCoding");
      property.setValue(new Coding("http://example.org/sys", "propCode", "Prop Display"));
      concept.getProperty().add(property);
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "myCoding");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "myCoding".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(CodingConceptProperty.class)
          .satisfies(
              p -> {
                CodingConceptProperty cp = (CodingConceptProperty) p;
                assertThat(cp.getCodeSystem()).isEqualTo("http://example.org/sys");
                assertThat(cp.getCode()).isEqualTo("propCode");
                assertThat(cp.getDisplay()).isEqualTo("Prop Display");
              });
    }

    @Test
    @DisplayName(
        "Should skip 'parent' from the concept's own property list because it is a hierarchy property derived from the graph")
    void shouldSkipHierarchyPropertyFromConceptOwnPropertyList()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — concept has a "parent" property in its own list, but no graph is available
      CodeSystem codeSystem = createCodeSystem();
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      CodeSystem.ConceptPropertyComponent parentProp = new CodeSystem.ConceptPropertyComponent();
      parentProp.setCode("parent");
      parentProp.setValue(new CodeType("B1"));
      concept.getProperty().add(parentProp);
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "parent");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — graph is null → addParentProperties adds nothing
      //        concept's own "parent" property is skipped by isHierarchyProperty check
      assertThat(result.getProperties()).noneMatch(p -> "parent".equals(p.getPropertyName()));
    }

    @Test
    @DisplayName("Should not add any 'parent' properties when no concept graph is available")
    void shouldNotAddParentPropertiesWhenNoGraphIsAvailable()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — no graph stub → getGraphForCodeSystem returns null
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "parent");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties()).noneMatch(p -> "parent".equals(p.getPropertyName()));
    }

    @Test
    @DisplayName("Should not add any 'child' properties when no concept graph is available")
    void shouldNotAddChildPropertiesWhenNoGraphIsAvailable()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — no graph stub → getGraphForCodeSystem returns null
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "child");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties()).noneMatch(p -> "child".equals(p.getPropertyName()));
    }

    @Test
    @DisplayName("Should add 'parent' properties from the graph when 'parent' is requested")
    void shouldAddParentPropertiesFromGraphWhenRequested()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // id = "test-cs"
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      when(inMemoryRepresentation.getGraphForCodeSystem(codeSystem)).thenReturn(mockGraph);
      when(mockGraph.getAllConceptsWithDirectChild(CODE)).thenReturn(Set.of("PARENT_CODE"));
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "parent");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "parent".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(BaseTypeConceptProperty.class)
          .extracting(p -> ((BaseTypeConceptProperty) p).getValue())
          .isInstanceOf(CodeType.class)
          .extracting(v -> ((CodeType) v).getValue())
          .isEqualTo("PARENT_CODE");
    }

    @Test
    @DisplayName("Should add 'child' properties from the graph when 'child' is requested")
    void shouldAddChildPropertiesFromGraphWhenRequested()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystem(); // id = "test-cs"
      codeSystem.getConcept().add(createConcept(CODE, "Display"));
      stubCodeSystem(codeSystem);
      when(inMemoryRepresentation.getGraphForCodeSystem(codeSystem)).thenReturn(mockGraph);
      when(mockGraph.getAllConceptsWithDirectParent(CODE)).thenReturn(Set.of("CHILD_CODE"));
      CsLookupParameter param = buildParamWithProperties(SYSTEM, CODE, "child");

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN
      assertThat(result.getProperties())
          .filteredOn(p -> "child".equals(p.getPropertyName()))
          .hasSize(1)
          .first()
          .isInstanceOf(BaseTypeConceptProperty.class)
          .extracting(p -> ((BaseTypeConceptProperty) p).getValue())
          .isInstanceOf(CodeType.class)
          .extracting(v -> ((CodeType) v).getValue())
          .isEqualTo("CHILD_CODE");
    }

    @Test
    @DisplayName(
        "Should expand the default property set with properties declared on the CodeSystem")
    void shouldExpandDefaultPropertySetWithCodeSystemDeclaredProperties()
        throws UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException,
            NotImplementedException {
      // GIVEN — CS declares a "status" property; concept carries that property
      CodeSystem codeSystem = createCodeSystem();
      codeSystem.addProperty().setCode("status");
      CodeSystem.ConceptDefinitionComponent concept = createConcept(CODE, "Display");
      CodeSystem.ConceptPropertyComponent statusProp = new CodeSystem.ConceptPropertyComponent();
      statusProp.setCode("status");
      statusProp.setValue(new CodeType("active"));
      concept.getProperty().add(statusProp);
      codeSystem.getConcept().add(concept);
      stubCodeSystem(codeSystem);
      CsLookupParameter param =
          buildParam(SYSTEM, CODE); // empty propertyParam → default set expanded

      // WHEN
      CsLookupResult result = lookupOperation.lookupCode(SCOPE, param);

      // THEN — "status" is added to the default set because CS declares it
      assertThat(result.getProperties())
          .filteredOn(p -> "status".equals(p.getPropertyName()))
          .isNotEmpty();
    }
  }
}
