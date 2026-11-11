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
package de.gematik.zts.termzilla.server.service.cs;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.codesystems.CodesystemContentMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link CombiningCsSearchParameterValidator}. */
@DisplayName("CombiningCsSearchParameterValidator")
class CombiningCsSearchParameterValidatorTest {

  private static final String DEFAULT_SYSTEM = "http://system.test";
  private static final String DEFAULT_URL = "http://url.test";

  @Nested
  @DisplayName("validateParameters()")
  class ValidateParametersTests {

    @Test
    @DisplayName("should pass when only system parameter is provided")
    void shouldPassWhenOnlySystemParameterIsProvided() {
      // GIVEN
      CsSearchParameter parameters = CsSearchParameterTestDataHelper.withSystem();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should pass when only url parameter is provided")
    void shouldPassWhenOnlyUrlParameterIsProvided() {
      // GIVEN
      CsSearchParameter parameters = CsSearchParameterTestDataHelper.withUrl();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when system and url are provided at the same time")
    void shouldThrowWhenSystemAndUrlAreProvidedAtTheSameTime() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .systemParam(new UriParam("http://system.test"))
              .urlParam(new UriParam("http://url.test"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Either use")
          .hasMessageContaining("url")
          .hasMessageContaining("system");
    }

    @Test
    @DisplayName("should throw when url contains a version and version parameter is also set")
    void shouldThrowWhenUrlContainsVersionAndVersionParameterIsAlsoSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .urlParam(new UriParam("http://url.test|1.0.0"))
              .versionParam(new TokenParam("2.0.0"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Version must not be set in 'url' parameter");
    }

    @Test
    @DisplayName("should pass when url has no embedded version and version parameter is set")
    void shouldPassWhenUrlHasNoEmbeddedVersionAndVersionParameterIsSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .urlParam(new UriParam("http://url.test"))
              .versionParam(new TokenParam("2.0.0"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "modifier: {1}")
    @MethodSource("contentParametersWithModifier")
    @DisplayName("should throw when content parameter uses unsupported modifiers")
    void shouldThrowWhenContentParameterUsesUnsupportedModifiers(
        StringParam contentParam, String modifierName) {
      // GIVEN
      CsSearchParameter parameters = CsSearchParameterTestDataHelper.withContent(contentParam);

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .as("Validation should fail for modifier: %s", modifierName)
          .isInstanceOf(ca.uhn.fhir.rest.server.exceptions.InvalidRequestException.class)
          .hasMessageContaining("No modifiers allowed")
          .hasMessageContaining("_content");
    }

    @Test
    @DisplayName("should pass when content parameter has no modifiers")
    void shouldPassWhenContentParameterHasNoModifiers() {
      // GIVEN
      StringParam contentParam = new StringParam("regular-content");
      CsSearchParameter parameters = CsSearchParameterTestDataHelper.withContent(contentParam);

      // WHEN / THEN
      assertThatCode(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when content is not-present and language parameter is also set")
    void shouldThrowWhenContentIsNotPresentAndLanguageParameterIsAlsoSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .contentParam(new StringParam(CodesystemContentMode.NOTPRESENT.toCode()))
              .languageParam(new TokenParam("de"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(ca.uhn.fhir.rest.server.exceptions.InvalidRequestException.class)
          .hasMessageContaining(CsSearchParameter.SP_LANGUAGE)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should throw when content is not-present and code parameter is also set")
    void shouldThrowWhenContentIsNotPresentAndCodeParameterIsAlsoSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .contentParam(new StringParam(CodesystemContentMode.NOTPRESENT.toCode()))
              .codeParam(new TokenParam("12345"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Code parameter must not be used")
          .hasMessageContaining("not-present");
    }

    @Test
    @DisplayName("should throw when content is not supplement and supplements parameter is set")
    void shouldThrowWhenContentIsNotSupplementAndSupplementsParameterIsSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .contentParam(new StringParam("complete"))
              .supplementsParam(new ReferenceParam("CodeSystem/supplement"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Supplements parameter must not be used");
    }

    @Test
    @DisplayName("should pass when content is supplement and supplements parameter is set")
    void shouldPassWhenContentIsSupplementAndSupplementsParameterIsSet() {
      // GIVEN
      CsSearchParameter parameters =
          CsSearchParameter.builder()
              .contentParam(new StringParam(CodesystemContentMode.SUPPLEMENT.toCode()))
              .supplementsParam(new ReferenceParam("CodeSystem/supplement"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    private static Stream<Arguments> contentParametersWithModifier() {
      StringParam exact = new StringParam("content");
      exact.setExact(true);

      StringParam contains = new StringParam("content");
      contains.setContains(true);

      StringParam text = new StringParam("content");
      text.setText(true);

      StringParam missing = new StringParam();
      missing.setMissing(true);

      return Stream.of(
          Arguments.of(exact, "exact"),
          Arguments.of(contains, "contains"),
          Arguments.of(text, "text"),
          Arguments.of(missing, "missing=true"));
    }
  }

  private static final class CsSearchParameterTestDataHelper {

    private CsSearchParameterTestDataHelper() {}

    private static CsSearchParameter withSystem() {
      return CsSearchParameter.builder().systemParam(new UriParam(DEFAULT_SYSTEM)).build();
    }

    private static CsSearchParameter withUrl() {
      return CsSearchParameter.builder().urlParam(new UriParam(DEFAULT_URL)).build();
    }

    private static CsSearchParameter withContent(StringParam content) {
      return CsSearchParameter.builder().contentParam(content).build();
    }
  }
}
