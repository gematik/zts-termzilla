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
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_CONCEPT_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_CONCEPT_DEFINITION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_BOOLEAN_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_CODE_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_CODING_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_CODING_DISPLAY;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_CODING_SYSTEM;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_DATETIME_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_DECIMAL_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_INTEGER_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_PROP_STRING_VALUE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_URL;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_VERSION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_DE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_EN;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_FR;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_URL;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_VERSION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.createCodeSystemWithAllPropertyTypes;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.createCodeSystemWithMultiLanguageDesignations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the FHIR {@code CodeSystem/$lookup} operation.
 *
 * <p>Focuses on correct serialization of concept properties and designations in the {@code
 * Parameters} response.
 */
@Slf4j
@DisplayName("CodeSystem $lookup Integration Tests")
public class CodeSystemLookupConceptIT extends BaseFhirIT {

  private static final String SCOPE = "fhir";
  private static final String PACKAGE_NAME = "bfarm.terminologien.lookuptests";
  private static final String PACKAGE_VERSION = "2026.0.0";

  // -------------------------------------------------------------------------------------------------------------------
  // Test data preparation
  // -------------------------------------------------------------------------------------------------------------------

  @Override
  protected void copyData(Path packageLoadTempDir) {
    try {
      prepareTestDataPackage(packageLoadTempDir);
    } catch (PackageGenerationException e) {
      throw new RuntimeException(e);
    }
  }

  private void prepareTestDataPackage(Path packageLoadTempDir) throws PackageGenerationException {
    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(PACKAGE_NAME)
            .packageVersion(PACKAGE_VERSION)
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createCodeSystemWithAllPropertyTypes());
    resources.add(createCodeSystemWithMultiLanguageDesignations());
    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: Properties
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Property handling")
  class PropertyTests {

    @Test
    @DisplayName("Should return 'url' property by default when no property is requested")
    void shouldReturnUrlPropertyByDefault() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — only "url" is in the default property set
      assertThat(findProperties(result, "url")).hasSize(1);
      Parameters.ParametersParameterComponent urlProp = findProperties(result, "url").getFirst();
      assertThat(getPartStringValue(urlProp, "value")).isEqualTo(CS1_URL);
    }

    @Test
    @DisplayName("Should return 'url' property with the CodeSystem URL when explicitly requested")
    void shouldReturnUrlPropertyWhenExplicitlyRequested() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "url");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "url")).hasSize(1);
      assertThat(getPartStringValue(findProperties(result, "url").getFirst(), "value"))
          .isEqualTo(CS1_URL);
    }

    @Test
    @DisplayName("Should return 'definition' property when requested and concept has a definition")
    void shouldReturnDefinitionPropertyWhenRequestedAndPresent() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "definition");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "definition")).hasSize(1);
      assertThat(getPartStringValue(findProperties(result, "definition").getFirst(), "value"))
          .isEqualTo(CS1_CONCEPT_DEFINITION);
    }

    @Test
    @DisplayName("Should not return 'definition' property when it is not requested")
    void shouldNotReturnDefinitionWhenNotRequested() {
      // GIVEN — only "url" in default property set
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "definition")).isEmpty();
    }

    @Test
    @DisplayName("Should return 'string' property with correct StringType value")
    void shouldReturnStringTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propStringType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propStringType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(StringType.class);
      assertThat(((StringType) valuePart.getValue()).getValue()).isEqualTo(CS1_PROP_STRING_VALUE);
    }

    @Test
    @DisplayName("Should return 'code' property with correct CodeType value")
    void shouldReturnCodeTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propCodeType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propCodeType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(CodeType.class);
      assertThat(((CodeType) valuePart.getValue()).getValue()).isEqualTo(CS1_PROP_CODE_VALUE);
    }

    @Test
    @DisplayName("Should return 'boolean' property with correct BooleanType value")
    void shouldReturnBooleanTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propBooleanType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propBooleanType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(BooleanType.class);
      assertThat(((BooleanType) valuePart.getValue()).booleanValue())
          .isEqualTo(CS1_PROP_BOOLEAN_VALUE);
    }

    @Test
    @DisplayName("Should return 'coding' property with correct system, code, and display")
    void shouldReturnCodingTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propCodingType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — value part contains a Coding
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propCodingType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(Coding.class);
      Coding coding = (Coding) valuePart.getValue();
      assertThat(coding.getSystem()).isEqualTo(CS1_PROP_CODING_SYSTEM);
      assertThat(coding.getCode()).isEqualTo(CS1_PROP_CODING_CODE);
      assertThat(coding.getDisplay()).isEqualTo(CS1_PROP_CODING_DISPLAY);
    }

    @Test
    @DisplayName("Should return 'integer' property with correct IntegerType value")
    void shouldReturnIntegerTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propIntegerType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propIntegerType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(IntegerType.class);
      assertThat(((IntegerType) valuePart.getValue()).getValue()).isEqualTo(CS1_PROP_INTEGER_VALUE);
    }

    @Test
    @DisplayName("Should return 'dateTime' property with correct DateTimeType value")
    void shouldReturnDateTimeTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propDateTimeType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propDateTimeType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(DateTimeType.class);
      assertThat(((DateTimeType) valuePart.getValue()).getValueAsString())
          .isEqualTo(CS1_PROP_DATETIME_VALUE);
    }

    @Test
    @DisplayName("Should return 'decimal' property with correct DecimalType value")
    void shouldReturnDecimalTypeProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "propDecimalType");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      Parameters.ParametersParameterComponent prop =
          findProperties(result, "propDecimalType").getFirst();
      Parameters.ParametersParameterComponent valuePart = getValuePart(prop);
      assertThat(valuePart.getValue()).isInstanceOf(DecimalType.class);
      assertThat(((DecimalType) valuePart.getValue()).getValue())
          .isEqualByComparingTo(CS1_PROP_DECIMAL_VALUE);
    }

    @Test
    @DisplayName("Should not return a concept property when it is not requested")
    void shouldNotReturnPropertyWhenNotRequested() {
      // GIVEN — request only "url", not "propStringType"
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, null);

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(findProperties(result, "propStringType")).isEmpty();
      assertThat(findProperties(result, "propCodeType")).isEmpty();
      assertThat(findProperties(result, "propBooleanType")).isEmpty();
      assertThat(findProperties(result, "propCodingType")).isEmpty();
      assertThat(findProperties(result, "propIntegerType")).isEmpty();
      assertThat(findProperties(result, "propDateTimeType")).isEmpty();
      assertThat(findProperties(result, "propDecimalType")).isEmpty();
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when code does not exist in the CodeSystem")
    void shouldThrowWhenCodeNotFound() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS1_URL, CS1_VERSION, "NONEXISTENT_CODE", null);

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
    @DisplayName("Should return all concept designations when 'designation' is requested")
    void shouldReturnAllDesignationsWhenRequested() {
      // GIVEN — CS2 concept has display + 3 explicit designations (de-DE, en, fr)
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, "designation");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — synthetic display designation + 3 explicit designations
      List<Parameters.ParametersParameterComponent> designations = findDesignations(result);
      assertThat(designations).isNotEmpty();

      // All three language designations must be present
      assertThat(findDesignationForLanguage(result, "de-DE")).isPresent();
      assertThat(findDesignationForLanguage(result, "en")).isPresent();
      assertThat(findDesignationForLanguage(result, "fr")).isPresent();
    }

    @Test
    @DisplayName("Should return designation values correctly for each language")
    void shouldReturnCorrectDesignationValuesPerLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, "designation");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertDesignationValue(result, "de-DE", CS2_CONCEPT_DISPLAY_DE);
      assertDesignationValue(result, "en", CS2_CONCEPT_DISPLAY_EN);
      assertDesignationValue(result, "fr", CS2_CONCEPT_DISPLAY_FR);
    }

    @Test
    @DisplayName("Should return display in the requested displayLanguage")
    void shouldReturnDisplayInRequestedDisplayLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, "en");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_EN);
    }

    @Test
    @DisplayName("Should return German display when displayLanguage matches CodeSystem language")
    void shouldReturnDisplayInCodeSystemLanguageWhenMatchingDisplayLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, "de-DE");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_DE);
    }

    @Test
    @DisplayName("Should return display with data-absent-reason when displayLanguage has no match")
    void shouldReturnDataAbsentReasonWhenDisplayLanguageHasNoMatch() {
      // GIVEN — CodeSystem has no Japanese designations
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildLookupParamsWithDisplayLanguage(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, "ja");

      // WHEN
      Parameters result = executeLookup(client, params);

      // THEN — display parameter is present but blank, with a data-absent-reason extension
      Parameters.ParametersParameterComponent displayParam =
          findParameter(result, "display").orElseThrow();
      StringType displayValue = (StringType) displayParam.getValue();
      assertThat(displayValue.hasExtension()).isTrue();
      assertThat(displayValue.getValueAsString()).isNullOrEmpty();
    }

    @Test
    @DisplayName("Should return display in the language requested via Accept-Language header")
    void shouldReturnDisplayViaAcceptLanguageHeader() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, null);

      // WHEN
      Parameters result = executeLookupWithAcceptLanguage(client, params, "fr");

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_FR);
    }

    @Test
    @DisplayName(
        "Should fall back to CodeSystem-language display when Accept-Language has no match")
    void shouldFallBackToCodeSystemLanguageWhenAcceptLanguageHasNoMatch() {
      // GIVEN — no Japanese designation; system default (de-DE) matches CS language
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildLookupParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, null);

      // WHEN
      Parameters result = executeLookupWithAcceptLanguage(client, params, "ja");

      // THEN — falls back to CS language display
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_DE);
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

  /** Asserts that a designation for the given language exists and carries the expected value. */
  private static void assertDesignationValue(
      Parameters result, String language, String expectedValue) {
    Parameters.ParametersParameterComponent designation =
        findDesignationForLanguage(result, language)
            .orElseThrow(
                () -> new AssertionError("No designation found for language '" + language + "'"));
    assertThat(getPartStringValue(designation, "value"))
        .as("designation value for language '%s'", language)
        .isEqualTo(expectedValue);
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
