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
package de.gematik.zts.termzilla.testing.cs.validatecode;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_CONCEPT_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_CONCEPT_DISPLAY;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_URL;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS1_VERSION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_DE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_EN;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_CONCEPT_DISPLAY_FR;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_URL;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS2_VERSION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS3_CONCEPT_CODE;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS3_URL;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.CS3_VERSION;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.createCaseSensitiveCodeSystem;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.createCodeSystemWithAllPropertyTypes;
import static de.gematik.zts.termzilla.testing.data.CodeSystemLookupTestResourceGenerator.createCodeSystemWithMultiLanguageDesignations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the FHIR {@code CodeSystem/$validate-code} operation.
 *
 * <p>Covers code validation, display validation with and without {@code displayLanguage}, display
 * selection via {@code Accept-Language}, and rejection of unsupported parameters.
 */
@Slf4j
@DisplayName("CodeSystem $validate-code Integration Tests")
public class CodeSystemValidateCodeIT extends BaseFhirIT {

  private static final String SCOPE = "fhir";
  private static final String PACKAGE_NAME = "bfarm.terminologien.validatecodetests";
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
    resources.add(createCaseSensitiveCodeSystem());
    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: ValidCodeTests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Valid code validation")
  class ValidCodeTests {

    @Test
    @DisplayName(
        "Should return result=true and a non-null display for a valid code without display parameter")
    void shouldReturnTrueForValidCode() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
      assertThat(getDisplayValue(result)).isNotNull();
    }

    @Test
    @DisplayName(
        "Should return result=false and a message containing the code and system for an unknown code")
    void shouldReturnFalseForUnknownCode() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS1_URL, CS1_VERSION, "NONEXISTENT");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isFalse();
      assertThat(getMessageValue(result)).contains("NONEXISTENT").contains(CS1_URL);
    }

    @Test
    @DisplayName(
        "Should return result=true when an explicit version is provided together with a valid code")
    void shouldReturnTrueForValidCodeWithVersion() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName("Should return result=true when the code is supplied via the coding parameter")
    void shouldReturnTrueForValidCodeViaCodingParameter() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Coding coding = new Coding(CS1_URL, CS1_CONCEPT_CODE, null);
      coding.setVersion(CS1_VERSION);
      Parameters params =
          buildValidateCodeParams(null, null, null, null, null, null, coding, null, null);

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: DisplayValidationWithoutDisplayLanguageTests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation without displayLanguage")
  class DisplayValidationWithoutDisplayLanguageTests {

    @Test
    @DisplayName(
        "Should return result=true when the provided display matches the concept's primary display")
    void shouldReturnTrueWhenDisplayMatchesConceptDisplay() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplay(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, CS1_CONCEPT_DISPLAY);

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=true when the provided display matches an explicit designation without use-coding")
    void shouldReturnTrueWhenDisplayMatchesExplicitDesignation() {
      // GIVEN — CS1/A03 has a designation with value "A03 Designation" and no use-coding
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplay(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "A03 Designation");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=true for a case-insensitive display match on a non-case-sensitive CodeSystem")
    void shouldReturnTrueForCaseInsensitiveDisplayMatch() {
      // GIVEN — CS1 has caseSensitive=false (default)
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplay(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "a03 display");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false and a message containing the wrong display when the display does not match")
    void shouldReturnFalseWhenDisplayDoesNotMatch() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParamsWithDisplay(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, "WRONG");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isFalse();
      assertThat(getMessageValue(result)).contains("WRONG");
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: DisplayValidationWithDisplayLanguageTests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Display validation with displayLanguage")
  class DisplayValidationWithDisplayLanguageTests {

    @Test
    @DisplayName(
        "Should return result=true when the display matches the designation for the requested language")
    void shouldReturnTrueWhenDisplayMatchesForRequestedLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplayAndLanguage(
              CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, CS2_CONCEPT_DISPLAY_EN, "en");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false when the display does not match the designation for the requested language")
    void shouldReturnFalseWhenDisplayDoesNotMatchForLanguage() {
      // GIVEN — display is German but displayLanguage=en is requested
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplayAndLanguage(
              CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, CS2_CONCEPT_DISPLAY_DE, "en");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isFalse();
      assertThat(getMessageValue(result)).contains(CS2_CONCEPT_DISPLAY_DE).contains("en");
    }

    @Test
    @DisplayName(
        "Should return result=false when the requested displayLanguage has no designation on the concept")
    void shouldReturnFalseWhenDisplayLanguageHasNoDesignation() {
      // GIVEN — CS1/A03 has no English designation
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplayAndLanguage(
              CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, CS1_CONCEPT_DISPLAY, "en");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isFalse();
      assertThat(getMessageValue(result)).contains("en").contains(CS1_CONCEPT_CODE);
    }

    @Test
    @DisplayName(
        "Should return result=true when BCP 47 language expansion resolves the displayLanguage to a matching designation")
    void shouldReturnTrueForBcp47LanguageExpansion() {
      // GIVEN — CS2/B01 has a de-DE designation; requesting displayLanguage=de expands to de-DE
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplayAndLanguage(
              CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE, CS2_CONCEPT_DISPLAY_DE, "de");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isTrue();
    }

    @Test
    @DisplayName(
        "Should return result=false with a case-sensitivity hint when display casing does not match on a case-sensitive CodeSystem")
    void shouldReturnFalseForCaseMismatchOnCaseSensitiveSystem() {
      // GIVEN — CS3 has caseSensitive=true; concept display is "C01 Display"
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildParamsWithDisplayAndLanguage(
              CS3_URL, CS3_VERSION, CS3_CONCEPT_CODE, "c01 display", "de-DE");

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getResultValue(result)).isFalse();
      assertThat(getMessageValue(result)).containsIgnoringCase("case");
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: DisplaySelectionTests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Display selection in response")
  class DisplaySelectionTests {

    @Test
    @DisplayName(
        "Should return the CodeSystem-language display by default when no Accept-Language is set")
    void shouldReturnDisplayInCodeSystemLanguageByDefault() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCode(client, params);

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS1_CONCEPT_DISPLAY);
    }

    @Test
    @DisplayName("Should return the French display when Accept-Language=fr is set")
    void shouldReturnDisplayForFrenchAcceptLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCodeWithAcceptLanguage(client, params, "fr");

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_FR);
    }

    @Test
    @DisplayName("Should return the English display when Accept-Language=en is set")
    void shouldReturnDisplayForEnglishAcceptLanguage() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCodeWithAcceptLanguage(client, params, "en");

      // THEN
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_EN);
    }

    @Test
    @DisplayName(
        "Should fall back to the CodeSystem-language display when Accept-Language has no matching designation")
    void shouldFallBackToCodeSystemLanguageDisplayWhenAcceptLanguageHasNoMatch() {
      // GIVEN — no Japanese designation available for CS2/B01
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = buildParams(CS2_URL, CS2_VERSION, CS2_CONCEPT_CODE);

      // WHEN
      Parameters result = executeValidateCodeWithAcceptLanguage(client, params, "ja");

      // THEN — falls back to CodeSystem default language display (de-DE)
      assertThat(getDisplayValue(result)).isEqualTo(CS2_CONCEPT_DISPLAY_DE);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Tests: UnsupportedParameterTests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Unsupported parameters")
  class UnsupportedParameterTests {

    @Test
    @DisplayName("Should throw an exception when the date parameter is provided")
    void shouldFailWhenDateParameterIsProvided() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildValidateCodeParams(
              CS1_URL,
              CS1_VERSION,
              CS1_CONCEPT_CODE,
              null,
              null,
              null,
              null,
              null,
              new DateTimeType("2024-01-01"));

      // WHEN / THEN
      assertThatThrownBy(() -> executeValidateCode(client, params))
          .isInstanceOf(BaseServerResponseException.class);
    }

    @Test
    @DisplayName("Should throw an exception when the abstract parameter is provided and is false")
    void shouldFailWhenAbstractParameterWithValueFalseIsProvided() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          buildValidateCodeParams(
              CS1_URL, CS1_VERSION, CS1_CONCEPT_CODE, null, null, false, null, null, null);

      // WHEN / THEN
      assertThatThrownBy(() -> executeValidateCode(client, params))
          .isInstanceOf(BaseServerResponseException.class);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // HTTP helpers
  // -------------------------------------------------------------------------------------------------------------------

  private Parameters executeValidateCode(IGenericClient client, Parameters params) {
    return client
        .operation()
        .onType("CodeSystem")
        .named("validate-code")
        .withParameters(params)
        .returnResourceType(Parameters.class)
        .execute();
  }

  private Parameters executeValidateCodeWithAcceptLanguage(
      IGenericClient client, Parameters params, String acceptLanguage) {
    return client
        .operation()
        .onType("CodeSystem")
        .named("validate-code")
        .withParameters(params)
        .withAdditionalHeader(ACCEPT_LANGUAGE, acceptLanguage)
        .returnResourceType(Parameters.class)
        .execute();
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Parameter builders
  // -------------------------------------------------------------------------------------------------------------------

  private static @NonNull Parameters buildParams(String url, String version, String code) {
    return buildValidateCodeParams(url, version, code, null, null, null, null, null, null);
  }

  private static @NonNull Parameters buildParamsWithDisplay(
      String url, String version, String code, String display) {
    return buildValidateCodeParams(url, version, code, display, null, null, null, null, null);
  }

  private static @NonNull Parameters buildParamsWithDisplayAndLanguage(
      String url, String version, String code, String display, String displayLanguage) {
    return buildValidateCodeParams(
        url, version, code, display, displayLanguage, null, null, null, null);
  }

  private static @NonNull Parameters buildValidateCodeParams(
      String url,
      String version,
      String code,
      String display,
      String displayLanguage,
      Boolean abstractValue,
      Coding coding,
      CodeSystem codeSystem,
      DateTimeType dateTime) {
    Parameters params = new Parameters();
    if (url != null) {
      params.addParameter().setName("url").setValue(new UriType(url));
    }
    if (version != null) {
      params.addParameter().setName("version").setValue(new StringType(version));
    }
    if (code != null) {
      params.addParameter().setName("code").setValue(new CodeType(code));
    }
    if (display != null) {
      params.addParameter().setName("display").setValue(new StringType(display));
    }
    if (displayLanguage != null) {
      params.addParameter().setName("displayLanguage").setValue(new CodeType(displayLanguage));
    }
    if (abstractValue != null) {
      params.addParameter().setName("abstract").setValue(new BooleanType(abstractValue));
    }
    if (coding != null) {
      params.addParameter().setName("coding").setValue(coding);
    }
    if (codeSystem != null) {
      params.addParameter().setName("codeSystem").setResource(codeSystem);
    }
    if (dateTime != null) {
      params.addParameter().setName("date").setValue(dateTime);
    }

    return params;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Assertion helpers
  // -------------------------------------------------------------------------------------------------------------------

  /** Returns the first top-level parameter with the given name, or empty. */
  private static Optional<Parameters.ParametersParameterComponent> findParameter(
      Parameters result, String name) {
    return result.getParameter().stream().filter(p -> name.equals(p.getName())).findFirst();
  }

  /** Returns the {@code boolean} value of the {@code result} parameter. */
  private static boolean getResultValue(Parameters result) {
    Parameters.ParametersParameterComponent param =
        findParameter(result, "result")
            .orElseThrow(() -> new AssertionError("No 'result' parameter found in response"));
    return ((BooleanType) param.getValue()).booleanValue();
  }

  /** Returns the string value of the {@code display} parameter, or {@code null} if absent. */
  private static String getDisplayValue(Parameters result) {
    return findParameter(result, "display")
        .map(p -> ((StringType) p.getValue()).getValue())
        .orElse(null);
  }

  /** Returns the string value of the {@code message} parameter, or {@code null} if absent. */
  private static String getMessageValue(Parameters result) {
    return findParameter(result, "message")
        .map(p -> ((StringType) p.getValue()).getValue())
        .orElse(null);
  }
}
