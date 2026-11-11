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
package de.gematik.zts.termzilla.testing.cs.lookup;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Integration tests for the FHIR {@code CodeSystem/$lookup} operation.
 *
 * <p>Focuses on correct serialization of concept properties and designations in the {@code
 * Parameters} response.
 */
@Slf4j
@DisplayName("CodeSystem $lookup Integration Tests")
public class CodeSystemLookupConceptSnomedIT extends BaseFhirIT {

  private static final String SNOMED_ARTIFICIAL_RELEASE =
      "testresources/SnomedCT_Germany-EditionRelease_ARTIFICIAL_20251115T120000Z.zip";
  private static final String SCOPE = "fhir";
  private static final String SNOMED_VERSION =
      "http://snomed.info/sct/11000274103/version/20251115";
  private static final String SNOMED_CONCEPT_CONJUNCTIVITIS = "9826008";
  private static final String SNOMED_CONCEPT_DYSPNOE = "267036007";
  private static final String SNOMED_CONCEPT_DYSPNOE_PARENT = "230145002";
  private static final String SNOMED_CONCEPT_DYSPNOE_DISPLAY_DE_DE = "Dyspnoe";
  private static final String SNOMED_CONCEPT_DYSPNOE_DISPLAY_EN_GB = "Dyspnoea";
  private static final String SNOMED_CONCEPT_DYSPNOE_DISPLAY_EN_US = "Dyspnea";
  private static final String SNOMED_CORE_MODULE_ID = "900000000000207008";
  private static final String SNOMED_DISPLAY_NAME = "SNOMED_CT";

  // -------------------------------------------------------------------------------------------------------------------
  // Test data preparation
  // -------------------------------------------------------------------------------------------------------------------

  private Path snomedLoadTempDir;

  @Override
  protected void copyData(Path packageLoadTempDir) {
    // Copy test packages to temporary directory
    TestPackageHelper.copyTestPackage(SNOMED_ARTIFICIAL_RELEASE, packageLoadTempDir);
    snomedLoadTempDir = packageLoadTempDir;
  }

  @Override
  protected void configureEnvironment(EnvironmentVariables env) {
    super.configureEnvironment(env);
    env.set("FHIRTERMS_START", "false");
    env.set("SNOMED_START", "true");
    env.set("SNOMED_RESTRICTEDMODE", "false");
    env.set(
        "SNOMED_RELEASEFILE",
        snomedLoadTempDir.toString()
            + "/"
            + SNOMED_ARTIFICIAL_RELEASE.substring(SNOMED_ARTIFICIAL_RELEASE.lastIndexOf('/') + 1));
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: Basic response structure
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Basic Response Structure handling")
  class BasicResponseStructureTests {

    @Test
    @DisplayName("Should return 'name' of the CodeSystem (SNOMED_CT) as String value")
    void shouldReturnCorrectName() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_CONJUNCTIVITIS, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — name parameter must be present and carry the expected display name as a string
      Parameters.ParametersParameterComponent nameParam =
          findParameter(result, "name")
              .orElseThrow(() -> new AssertionError("No 'name' parameter found in response"));
      assertThat(nameParam.getValue()).isInstanceOf(StringType.class);
      assertThat(((StringType) nameParam.getValue()).getValue()).isEqualTo(SNOMED_DISPLAY_NAME);
    }

    @Test
    @DisplayName("Should return 'version' of the CodeSystem as String value")
    void shouldReturnCorrectVersion() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_CONJUNCTIVITIS, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — version parameter must be present and match the requested version
      Parameters.ParametersParameterComponent versionParam =
          findParameter(result, "version")
              .orElseThrow(() -> new AssertionError("No 'version' parameter found in response"));
      assertThat(versionParam.getValue()).isInstanceOf(StringType.class);
      assertThat(((StringType) versionParam.getValue()).getValue()).isEqualTo(SNOMED_VERSION);
    }

    @Test
    @DisplayName("Should return 'display' of the concept as a non-blank String value")
    void shouldReturnNonBlankDisplay() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_CONJUNCTIVITIS, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — display parameter must be present and carry a non-blank StringType value
      Parameters.ParametersParameterComponent displayParam =
          findParameter(result, "display")
              .orElseThrow(() -> new AssertionError("No 'display' parameter found in response"));
      assertThat(displayParam.getValue()).isInstanceOf(StringType.class);
      assertThat(((StringType) displayParam.getValue()).getValue()).isNotBlank();
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: Properties
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Property handling")
  class PropertyTests {

    @Test
    @DisplayName(
        "Should return all default SNOMED properties when no property is explicitly requested")
    void shouldReturnAllDefaultPropertiesWhenNoneRequested() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — all default SNOMED properties must be present in the response
      assertThat(findProperties(result, "url")).hasSize(1);
      assertThat(findProperties(result, "inactive")).hasSize(1);
      assertThat(findProperties(result, "parent")).isNotEmpty();
      assertThat(findProperties(result, "sufficientlyDefined")).hasSize(1);
      assertThat(findProperties(result, "moduleId")).hasSize(1);
      assertThat(findDesignations(result)).isNotEmpty();
    }

    @Test
    @DisplayName("Should return 'inactive' as false for the active concept 267036007")
    void shouldReturnInactiveAsFalseForActiveConcept() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "inactive");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent inactiveProp =
          findProperties(result, "inactive").stream()
              .findFirst()
              .orElseThrow(() -> new AssertionError("No 'inactive' property found in response"));
      Parameters.ParametersParameterComponent valuePart = getValuePart(inactiveProp);
      assertThat(valuePart.getValue()).isInstanceOf(BooleanType.class);
      assertThat(((BooleanType) valuePart.getValue()).booleanValue()).isFalse();
    }

    @Test
    @DisplayName("Should return 'sufficientlyDefined' as false for the primitive concept 267036007")
    void shouldReturnSufficientlyDefinedAsFalseForPrimitiveConcept() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "sufficientlyDefined");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "sufficientlyDefined").stream()
              .findFirst()
              .orElseThrow(
                  () -> new AssertionError("No 'sufficientlyDefined' property found in response"));
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(BooleanType.class);
      assertThat(((BooleanType) valuePart.getValue()).booleanValue()).isFalse();
    }

    @Test
    @DisplayName("Should return 'moduleId' property with the SNOMED CT core module identifier")
    void shouldReturnModuleIdWithCoreModuleIdentifier() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "moduleId");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "moduleId").stream()
              .findFirst()
              .orElseThrow(() -> new AssertionError("No 'moduleId' property found in response"));
      assertThat(getPartStringValue(prop, "value")).isEqualTo(SNOMED_CORE_MODULE_ID);
    }

    @Test
    @DisplayName("Should return 'url' property with the SNOMED CT canonical system URL")
    void shouldReturnUrlPropertyWithSnomedCanonicalUrl() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "url");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "url")).hasSize(1);
      assertThat(getPartStringValue(findProperties(result, "url").getFirst(), "value"))
          .isEqualTo(CanonicalUrl.SNOMEDCT);
    }

    @Test
    @DisplayName("Should return 'parent' property containing the direct parent concept 230145002")
    void shouldReturnParentPropertyWithDirectParentConcept() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "parent");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — at least one parent must be returned and must include the expected concept code
      List<Parameters.ParametersParameterComponent> parents = findProperties(result, "parent");
      assertThat(parents).isNotEmpty();
      boolean parentFound =
          parents.stream()
              .anyMatch(
                  p -> {
                    Parameters.ParametersParameterComponent valuePart = getValuePart(p);
                    return valuePart.getValue() instanceof CodeType ct
                        && SNOMED_CONCEPT_DYSPNOE_PARENT.equals(ct.getValue());
                  });
      assertThat(parentFound)
          .as(
              "Expected parent concept '%s' not found in parent properties",
              SNOMED_CONCEPT_DYSPNOE_PARENT)
          .isTrue();
    }

    @Test
    @DisplayName("Should not return SNOMED-specific properties when only 'url' is requested")
    void shouldNotReturnSnomedSpecificPropertiesWhenOnlyUrlIsRequested() {
      // GIVEN — explicitly request only 'url'; no other properties should appear
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "url");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "inactive")).isEmpty();
      assertThat(findProperties(result, "sufficientlyDefined")).isEmpty();
      assertThat(findProperties(result, "moduleId")).isEmpty();
      assertThat(findProperties(result, "parent")).isEmpty();
      assertThat(findProperties(result, "child")).isEmpty();
      assertThat(findDesignations(result)).isEmpty();
    }

    @Test
    @DisplayName(
        "Should throw InvalidRequestException when the concept code does not exist in SNOMED CT")
    void shouldThrowWhenConceptCodeDoesNotExist() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, "9999999999", null);

      // WHEN / THEN
      assertThatThrownBy(() -> executeLookup(client, params))
          .isInstanceOf(InvalidRequestException.class);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: Designations
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Designation handling")
  class DesignationTests {

    @Test
    @DisplayName(
        "Should return German and English designations when 'designation' property is requested")
    void shouldReturnGermanAndEnglishDesignationsWhenDesignationRequested() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "designation");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — both German (de-DE) and English (en-GB, en-US) designations must be present
      assertThat(findDesignations(result)).isNotEmpty();
      assertThat(findDesignationForLanguage(result, "de-DE")).isPresent();
      assertThat(findDesignationForLanguage(result, "en-GB")).isPresent();
      assertThat(findDesignationForLanguage(result, "en-US")).isPresent();
    }

    @Test
    @DisplayName(
        "Should return the preferred German designation 'Dyspnoe' among de-DE designations")
    void shouldReturnPreferredGermanDesignationDyspnoe() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "designation");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — the preferred synonym 'Dyspnoe' must appear as a de-DE designation
      boolean dyspnoePresent =
          findDesignations(result).stream()
              .anyMatch(
                  d ->
                      "de-DE".equals(getCodePartValue(d, "language"))
                          && SNOMED_CONCEPT_DYSPNOE_DISPLAY_DE_DE.equals(
                              getPartStringValue(d, "value")));
      assertThat(dyspnoePresent)
          .as(
              "Expected designation '%s' for language 'de-DE' not found",
              SNOMED_CONCEPT_DYSPNOE_DISPLAY_DE_DE)
          .isTrue();
    }

    @Test
    @DisplayName("Should return display 'Dyspnoe' when displayLanguage is 'de-DE'")
    void shouldReturnGermanDisplayWhenDisplayLanguageIsDeDE() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "de-DE");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(SNOMED_CONCEPT_DYSPNOE_DISPLAY_DE_DE);
    }

    @Test
    @DisplayName("Should return display 'Dyspnoea' when displayLanguage is 'en-GB'")
    void shouldReturnBritishEnglishDisplayWhenDisplayLanguageIsEnGB() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "en-GB");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(SNOMED_CONCEPT_DYSPNOE_DISPLAY_EN_GB);
    }

    @Test
    @DisplayName("Should return display 'Dyspnea' when displayLanguage is 'en-US'")
    void shouldReturnAmericanEnglishDisplayWhenDisplayLanguageIsEnUS() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "en-US");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(SNOMED_CONCEPT_DYSPNOE_DISPLAY_EN_US);
    }

    @Test
    @DisplayName("Should return German display 'Dyspnoe' via Accept-Language header 'de-DE'")
    void shouldReturnGermanDisplayViaAcceptLanguageHeaderDeDE() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, null);

      // WHEN
      Parameters result = executeLookupWithAcceptLanguage(client, params, "de-DE");

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(SNOMED_CONCEPT_DYSPNOE_DISPLAY_DE_DE);
    }

    @Test
    @DisplayName(
        "Should return British English display 'Dyspnoea' via Accept-Language header 'en-GB'")
    void shouldReturnBritishEnglishDisplayViaAcceptLanguageHeaderEnGB() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, null);

      // WHEN
      Parameters result = executeLookupWithAcceptLanguage(client, params, "en-GB");

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(SNOMED_CONCEPT_DYSPNOE_DISPLAY_EN_GB);
    }

    @Test
    @DisplayName(
        "Should return display with data-absent-reason extension when displayLanguage has no match")
    void shouldReturnDataAbsentReasonForUnsupportedDisplayLanguage() {
      // GIVEN — SNOMED CT does not contain Japanese designations for this concept
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "ja");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — display parameter is present but blank, with a data-absent-reason extension
      Parameters.ParametersParameterComponent displayParam =
          findParameter(result, "display")
              .orElseThrow(() -> new AssertionError("No 'display' parameter found in response"));
      StringType displayValue = (StringType) displayParam.getValue();
      assertThat(displayValue.hasExtension()).isTrue();
      assertThat(displayValue.getValueAsString()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should return only de-DE designations when 'lang.de-DE' property is requested")
    void shouldReturnOnlyGermanDesignationsWhenLangDeDERequested() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(
              CanonicalUrl.SNOMEDCT, SNOMED_VERSION, SNOMED_CONCEPT_DYSPNOE, "lang.de-DE");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — all returned designations must carry the 'de-DE' language tag
      assertThat(findDesignations(result))
          .isNotEmpty()
          .allMatch(
              d -> "de-DE".equals(getCodePartValue(d, "language")),
              "all designations should have language 'de-DE'");
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // HTTP helpers
  // -------------------------------------------------------------------------------------------------------------------

  private Parameters executeLookup(IGenericClient client, Parameters params) {
    return client
        .operation()
        .onType("CodeSystem")
        .named("lookup")
        .withParameters(params)
        .returnResourceType(Parameters.class)
        .execute();
  }

  private Parameters executeLookupWithAcceptLanguage(
      IGenericClient client, Parameters params, String acceptLanguage) {
    return client
        .operation()
        .onType("CodeSystem")
        .named("lookup")
        .withParameters(params)
        .withAdditionalHeader(ACCEPT_LANGUAGE, acceptLanguage)
        .returnResourceType(Parameters.class)
        .execute();
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Parameter builders
  // -------------------------------------------------------------------------------------------------------------------

  private static @NonNull Parameters buildLookupParams(
      String system, String version, String code, String requestedProperties) {
    Parameters params = new Parameters();
    params.addParameter().setName("system").setValue(new UriType(system));
    params.addParameter().setName("version").setValue(new StringType(version));
    params.addParameter().setName("code").setValue(new CodeType(code));
    if (requestedProperties != null) {
      for (String property : requestedProperties.split(",")) {
        params.addParameter().setName("property").setValue(new CodeType(property.strip()));
      }
    }
    return params;
  }

  private static @NonNull Parameters buildLookupParamsWithDisplayLanguage(
      String system, String version, String code, String displayLanguage) {
    Parameters params = buildLookupParams(system, version, code, null);
    params.addParameter().setName("displayLanguage").setValue(new CodeType(displayLanguage));
    return params;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Assertion helpers
  // -------------------------------------------------------------------------------------------------------------------

  /** Returns all top-level parameters with the given name. */
  private static List<Parameters.ParametersParameterComponent> findAllParameters(
      Parameters result, String name) {
    return result.getParameter().stream().filter(p -> name.equals(p.getName())).toList();
  }

  /** Returns the first top-level parameter with the given name, or empty. */
  private static Optional<Parameters.ParametersParameterComponent> findParameter(
      Parameters result, String name) {
    return result.getParameter().stream().filter(p -> name.equals(p.getName())).findFirst();
  }

  /**
   * Returns all {@code property} parameters whose {@code code} part matches {@code propertyCode}.
   */
  private static List<Parameters.ParametersParameterComponent> findProperties(
      Parameters result, String propertyCode) {
    return findAllParameters(result, "property").stream()
        .filter(
            p ->
                p.getPart().stream()
                    .anyMatch(
                        part ->
                            "code".equals(part.getName())
                                && part.getValue() instanceof CodeType ct
                                && propertyCode.equals(ct.getValue())))
        .toList();
  }

  /** Returns all {@code designation} parameters. */
  private static List<Parameters.ParametersParameterComponent> findDesignations(Parameters result) {
    return findAllParameters(result, "designation");
  }

  /**
   * Returns the first {@code designation} parameter whose {@code language} part matches the given
   * language tag, or empty.
   */
  private static Optional<Parameters.ParametersParameterComponent> findDesignationForLanguage(
      Parameters result, String language) {
    return findDesignations(result).stream()
        .filter(d -> language.equals(getCodePartValue(d, "language")))
        .findFirst();
  }

  /** Returns the string value of the {@code display} top-level parameter. */
  private static String getDisplayValue(Parameters result) {
    Parameters.ParametersParameterComponent displayParam =
        findParameter(result, "display")
            .orElseThrow(() -> new AssertionError("No 'display' parameter found in response"));
    return ((StringType) displayParam.getValue()).getValue();
  }

  /** Returns the {@code value} part of a {@code property} parameter group. */
  private static Parameters.ParametersParameterComponent getValuePart(
      Parameters.ParametersParameterComponent propertyParam) {
    return propertyParam.getPart().stream()
        .filter(part -> "value".equals(part.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Property parameter has no 'value' part"));
  }

  /** Returns the string value of a named part within a parameter group. */
  private static String getPartStringValue(
      Parameters.ParametersParameterComponent param, String partName) {
    return param.getPart().stream()
        .filter(part -> partName.equals(part.getName()))
        .filter(part -> part.getValue() instanceof StringType)
        .map(part -> ((StringType) part.getValue()).getValue())
        .findFirst()
        .orElse(null);
  }

  /** Returns the code value of a named CodeType part within a parameter group. */
  private static String getCodePartValue(
      Parameters.ParametersParameterComponent param, String partName) {
    return param.getPart().stream()
        .filter(part -> partName.equals(part.getName()))
        .filter(part -> part.getValue() instanceof CodeType)
        .map(part -> ((CodeType) part.getValue()).getValue())
        .findFirst()
        .orElse(null);
  }
}
