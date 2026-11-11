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
package de.gematik.zts.termzilla.testing.snomed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Integration tests for the SNOMED CT restricted mode ({@code SNOMED_RESTRICTEDMODE=true}).
 *
 * <p>In restricted mode, concept relationships, the definition status and expressions are not
 * provided. ValueSets that are part of the service's content may still use hierarchy-based filters,
 * ValueSets provided within the request may not.
 */
@Slf4j
@DisplayName("SNOMED CT Restricted Mode Integration Tests")
class SnomedRestrictedModeIT extends BaseFhirIT {

  private static final String SNOMED_ARTIFICIAL_RELEASE =
      "testresources/SnomedCT_Germany-EditionRelease_ARTIFICIAL_20251115T120000Z.zip";
  private static final String SCOPE = "fhir";
  private static final String SNOMED_VERSION =
      "http://snomed.info/sct/11000274103/version/20251115";

  /** Concept "Difficulty breathing" with the child concept "Dyspnoe". */
  private static final String CONCEPT_DIFFICULTY_BREATHING = "230145002";

  private static final String CONCEPT_DYSPNOE = "267036007";

  /** Reference set "Lateralizable body structure reference set" and some active members. */
  private static final String REFSET_LATERALIZABLE = "723264001";

  private static final List<String> REFSET_LATERALIZABLE_MEMBERS =
      List.of("120575009", "66019005", "699611007");

  private static final String RESTRICTED_MODE = "restricted mode";

  private static final String PACKAGE_NAME = "bfarm.terminologien.snomedrestrictedtests";
  private static final String PACKAGE_VERSION = "1.0.0";
  private static final String STORED_VS_URL =
      "http://example.org/fhir/ValueSet/snomed-difficulty-breathing";
  private static final String STORED_VS_VERSION = "1.0.0";

  // -------------------------------------------------------------------------------------------------------------------
  // Test data preparation
  // -------------------------------------------------------------------------------------------------------------------

  private Path snomedLoadTempDir;

  @Override
  protected void copyData(Path packageLoadTempDir) {
    TestPackageHelper.copyTestPackage(SNOMED_ARTIFICIAL_RELEASE, packageLoadTempDir);
    snomedLoadTempDir = packageLoadTempDir;

    // Gespeichertes ValueSet mit hierarchiebasiertem Filter, das zum Bestand des Servers gehört
    try {
      PackageGeneration.builder()
          .packageName(PACKAGE_NAME)
          .packageVersion(PACKAGE_VERSION)
          .build()
          .generatePackage(List.<MetadataResource>of(createStoredValueSet()), packageLoadTempDir);
    } catch (PackageGenerationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void configureEnvironment(EnvironmentVariables env) {
    super.configureEnvironment(env);
    env.set("SNOMED_START", "true");
    env.set("SNOMED_RESTRICTEDMODE", "true");
    env.set(
        "SNOMED_RELEASEFILE",
        snomedLoadTempDir.toString()
            + "/"
            + SNOMED_ARTIFICIAL_RELEASE.substring(SNOMED_ARTIFICIAL_RELEASE.lastIndexOf('/') + 1));
  }

  private static ValueSet createStoredValueSet() {
    ValueSet valueSet = new ValueSet();
    valueSet.setId("snomed-difficulty-breathing-" + STORED_VS_VERSION);
    valueSet.setUrl(STORED_VS_URL);
    valueSet.setVersion(STORED_VS_VERSION);
    valueSet.setName("SnomedDifficultyBreathing");
    valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    valueSet
        .getCompose()
        .addInclude(snomedFilterInclude("concept", "is-a", CONCEPT_DIFFICULTY_BREATHING));
    return valueSet;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: CodeSystem/$lookup
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("CodeSystem $lookup")
  class LookupTests {

    @ParameterizedTest(name = "property ''{0}''")
    @ValueSource(strings = {"parent", "child", "sufficientlyDefined"})
    @DisplayName("Should reject restricted properties with 400")
    void shouldRejectRestrictedProperties(String property) {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = lookupParams(CONCEPT_DYSPNOE);
      params.addParameter().setName("property").setValue(new CodeType(property));

      // WHEN / THEN
      assertThatThrownBy(() -> execute(client, "CodeSystem", "lookup", params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(RESTRICTED_MODE);
    }

    @Test
    @DisplayName("Should return only unrestricted default properties")
    void shouldReturnOnlyUnrestrictedDefaultProperties() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);

      // WHEN
      Parameters result = execute(client, "CodeSystem", "lookup", lookupParams(CONCEPT_DYSPNOE));

      // THEN
      assertThat(propertyCodes(result))
          .contains("url", "inactive", "moduleId")
          .doesNotContain("parent", "child", "sufficientlyDefined");
      assertThat(result.getParameter())
          .anyMatch(parameter -> "designation".equals(parameter.getName()));
    }

    @Test
    @DisplayName("Should reject SNOMED CT expressions with 400")
    void shouldRejectExpressions() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = lookupParams(CONCEPT_DYSPNOE + " : 363698007 = 39607008");

      // WHEN / THEN
      assertThatThrownBy(() -> execute(client, "CodeSystem", "lookup", params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(RESTRICTED_MODE);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: CodeSystem/$find-matches
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("CodeSystem $find-matches")
  class FindMatchesTests {

    @ParameterizedTest(name = "property ''{0}''")
    @ValueSource(strings = {"parent", "child"})
    @DisplayName("Should reject relationship property filters with 400")
    void shouldRejectRelationshipPropertyFilters(String property) {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = findMatchesParams(property, new CodeType(CONCEPT_DIFFICULTY_BREATHING));

      // WHEN / THEN
      assertThatThrownBy(() -> execute(client, "CodeSystem", "find-matches", params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(RESTRICTED_MODE);
    }

    @Test
    @DisplayName("Should still find concepts by display")
    void shouldStillFindConceptsByDisplay() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = findMatchesParams("display", new StringType("Dyspnoe"));

      // WHEN
      Parameters result = execute(client, "CodeSystem", "find-matches", params);

      // THEN
      assertThat(result.getParameter()).isNotEmpty();
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: ValueSet/$expand
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("ValueSet $expand")
  class ExpandTests {

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
      "concept, is-a, " + CONCEPT_DIFFICULTY_BREATHING,
      "concept, descendent-of, " + CONCEPT_DIFFICULTY_BREATHING,
      "constraint, =, <<" + CONCEPT_DIFFICULTY_BREATHING,
      "parent, =, " + CONCEPT_DIFFICULTY_BREATHING,
      "child, =, " + CONCEPT_DYSPNOE
    })
    @DisplayName("Should reject hierarchy-based filters of provided ValueSets with 400")
    void shouldRejectHierarchyBasedFiltersOfProvidedValueSets(
        String property, String operator, String value) {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = new Parameters();
      params
          .addParameter()
          .setName("valueSet")
          .setResource(providedValueSet(property, operator, value));

      // WHEN / THEN
      assertThatThrownBy(() -> expand(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(RESTRICTED_MODE);
    }

    @Test
    @DisplayName("Should expand concept in filter of provided ValueSets")
    void shouldExpandConceptInFilterOfProvidedValueSets() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = new Parameters();
      params
          .addParameter()
          .setName("valueSet")
          .setResource(providedValueSet("concept", "in", REFSET_LATERALIZABLE));

      // WHEN
      ValueSet result = expand(client, params);

      // THEN
      assertThat(expansionCodes(result)).containsAll(REFSET_LATERALIZABLE_MEMBERS);
    }

    @Test
    @DisplayName("Should expand hierarchy-based filters of stored ValueSets")
    void shouldExpandHierarchyBasedFiltersOfStoredValueSets() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = new Parameters();
      params.addParameter().setName("url").setValue(new UriType(STORED_VS_URL));
      params.addParameter().setName("valueSetVersion").setValue(new StringType(STORED_VS_VERSION));

      // WHEN
      ValueSet result = expand(client, params);

      // THEN
      assertThat(expansionCodes(result)).contains(CONCEPT_DIFFICULTY_BREATHING, CONCEPT_DYSPNOE);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: ValueSet/$validate-code
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("ValueSet $validate-code")
  class ValueSetValidateCodeTests {

    @Test
    @DisplayName("Should reject hierarchy-based filters of provided ValueSets with 400")
    void shouldRejectHierarchyBasedFiltersOfProvidedValueSets() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = validateCodeParams(CONCEPT_DYSPNOE);
      params
          .addParameter()
          .setName("valueSet")
          .setResource(providedValueSet("concept", "is-a", CONCEPT_DIFFICULTY_BREATHING));

      // WHEN / THEN
      assertThatThrownBy(() -> execute(client, "ValueSet", "validate-code", params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(RESTRICTED_MODE);
    }

    @Test
    @DisplayName("Should validate codes against hierarchy-based filters of stored ValueSets")
    void shouldValidateCodesAgainstStoredValueSets() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = validateCodeParams(CONCEPT_DYSPNOE);
      params.addParameter().setName("url").setValue(new UriType(STORED_VS_URL));
      params.addParameter().setName("valueSetVersion").setValue(new StringType(STORED_VS_VERSION));

      // WHEN
      Parameters result = execute(client, "ValueSet", "validate-code", params);

      // THEN
      assertThat(result.getParameterBool("result")).isTrue();
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------------------------------------------------

  private static Parameters execute(
      IGenericClient client, String resourceType, String operation, Parameters params) {
    return client
        .operation()
        .onType(resourceType)
        .named(operation)
        .withParameters(params)
        .returnResourceType(Parameters.class)
        .execute();
  }

  private static ValueSet expand(IGenericClient client, Parameters params) {
    return client
        .operation()
        .onType("ValueSet")
        .named("expand")
        .withParameters(params)
        .returnResourceType(ValueSet.class)
        .execute();
  }

  private static Parameters lookupParams(String code) {
    Parameters params = new Parameters();
    params.addParameter().setName("system").setValue(new UriType(CanonicalUrl.SNOMEDCT));
    params.addParameter().setName("version").setValue(new StringType(SNOMED_VERSION));
    params.addParameter().setName("code").setValue(new CodeType(code));
    return params;
  }

  private static Parameters findMatchesParams(String propertyCode, Type value) {
    Parameters params = new Parameters();
    params.addParameter().setName("system").setValue(new UriType(CanonicalUrl.SNOMEDCT));
    params.addParameter().setName("version").setValue(new StringType(SNOMED_VERSION));
    params.addParameter().setName("exact").setValue(new BooleanType(false));
    Parameters.ParametersParameterComponent property = params.addParameter().setName("property");
    property.addPart().setName("code").setValue(new CodeType(propertyCode));
    property.addPart().setName("value").setValue(value);
    return params;
  }

  private static Parameters validateCodeParams(String code) {
    Parameters params = new Parameters();
    params.addParameter().setName("code").setValue(new CodeType(code));
    params.addParameter().setName("system").setValue(new UriType(CanonicalUrl.SNOMEDCT));
    return params;
  }

  private static ValueSet providedValueSet(String property, String operator, String value) {
    ValueSet valueSet = new ValueSet();
    valueSet.setUrl("http://example.org/fhir/ValueSet/provided");
    valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    valueSet.getCompose().addInclude(snomedFilterInclude(property, operator, value));
    return valueSet;
  }

  private static ValueSet.ConceptSetComponent snomedFilterInclude(
      String property, String operator, String value) {
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(CanonicalUrl.SNOMEDCT);
    include
        .addFilter()
        .setProperty(property)
        .setOp(ValueSet.FilterOperator.fromCode(operator))
        .setValue(value);
    return include;
  }

  private static List<String> propertyCodes(Parameters result) {
    return result.getParameter().stream()
        .filter(parameter -> "property".equals(parameter.getName()))
        .flatMap(parameter -> parameter.getPart().stream())
        .filter(part -> "code".equals(part.getName()) && part.getValue() instanceof CodeType)
        .map(part -> ((CodeType) part.getValue()).getValue())
        .toList();
  }

  private static List<String> expansionCodes(ValueSet result) {
    return result.getExpansion().getContains().stream()
        .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
        .toList();
  }
}
