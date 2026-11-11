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
package de.gematik.zts.termzilla.server.service.cm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link CombiningCmSearchParameterValidator}. */
@DisplayName("CombiningCmSearchParameterValidator")
class CombiningCmSearchParameterValidatorTest {

  private static final String SOURCE_CANONICAL = "ConceptMap/source";
  private static final String SOURCE_URI = "http://example.org/source";
  private static final String TARGET_CANONICAL = "ConceptMap/target";
  private static final String TARGET_URI = "http://example.org/target";

  @Nested
  @DisplayName("validateParameters")
  class ValidateParametersTests {

    @Test
    @DisplayName("should pass when sourceCanonical and targetUri are set")
    void shouldPassWhenSourceCanonicalAndTargetUriAreSet() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameterTestDataHelper.withSourceCanonicalAndTargetUri();

      // WHEN / THEN
      assertThatCode(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when sourceCanonical and sourceUri are set together")
    void shouldThrowWhenSourceCanonicalAndSourceUriAreSetTogether() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .sourceCanonicalParam(new ReferenceParam(SOURCE_CANONICAL))
              .sourceUriParam(new ReferenceParam(SOURCE_URI))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(CmSearchParameter.SP_SOURCE_CANONICAL)
          .hasMessageContaining(CmSearchParameter.SP_SOURCE_URI);
    }

    @Test
    @DisplayName("should throw when targetCanonical and targetUri are set together")
    void shouldThrowWhenTargetCanonicalAndTargetUriAreSetTogether() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .targetCanonicalParam(new ReferenceParam(TARGET_CANONICAL))
              .targetUriParam(new ReferenceParam(TARGET_URI))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(CmSearchParameter.SP_TARGET_CANONICAL)
          .hasMessageContaining(CmSearchParameter.SP_TARGET_URI);
    }

    @Test
    @DisplayName("should throw when version is set and url already contains a version")
    void shouldThrowWhenVersionIsSetAndUrlAlreadyContainsAVersion() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .urlParam(new UriParam("http://example.org/ConceptMap/test|1.0.0"))
              .versionParam(new TokenParam("2.0.0"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Version must not be set in 'url' parameter");
    }

    @Test
    @DisplayName("should pass when version is set and url has no embedded version")
    void shouldPassWhenVersionIsSetAndUrlHasNoEmbeddedVersion() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .urlParam(new UriParam("http://example.org/ConceptMap/test"))
              .versionParam(new TokenParam("2.0.0"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("contentParametersWithUnsupportedModifier")
    @DisplayName("should throw when _content uses unsupported modifiers")
    void shouldThrowWhenContentUsesUnsupportedModifiers(
        StringParam contentParam, String modifierName) {
      // GIVEN
      CmSearchParameter parameters = CmSearchParameterTestDataHelper.withContent(contentParam);

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .as("Validation should fail for modifier: %s", modifierName)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("No modifiers allowed")
          .hasMessageContaining("_content");
    }

    @Test
    @DisplayName("should pass when _content has no modifiers")
    void shouldPassWhenContentHasNoModifiers() {
      // GIVEN
      StringParam contentParam = new StringParam("regular-content");
      CmSearchParameter parameters = CmSearchParameterTestDataHelper.withContent(contentParam);

      // WHEN / THEN
      assertThatCode(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when unsupported qualifier is used for sourceSystem")
    void shouldThrowWhenUnsupportedQualifierIsUsedForSourceSystem() {
      // GIVEN
      UriParam sourceSystem = new UriParam(SOURCE_URI);
      sourceSystem.setQualifier(UriParamQualifierEnum.BELOW);

      CmSearchParameter parameters =
          CmSearchParameter.builder().sourceSystemParam(sourceSystem).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Only Qualifier 'missing'")
          .hasMessageContaining(CmSearchParameter.SP_SOURCE_SYSTEM);
    }

    @Test
    @DisplayName("should pass when missing modifier is used for targetUri")
    void shouldPassWhenMissingModifierIsUsedForTargetUri() {
      // GIVEN
      ReferenceParam targetUri = new ReferenceParam(TARGET_URI);
      targetUri.setMissing(true);

      CmSearchParameter parameters = CmSearchParameter.builder().targetUriParam(targetUri).build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when depends-on is provided")
    void shouldThrowWhenDependsOnIsProvided() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .dependsOnParam(new UriParam("http://example.org/depends"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CmSearchParameter.SP_DEPENDS_ON)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should throw when product is provided")
    void shouldThrowWhenProductIsProvided() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder()
              .productParam(new UriParam("http://example.org/product"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CmSearchParameter.SP_PRODUCT)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should throw when source-code is provided")
    void shouldThrowWhenSourceCodeIsProvided() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder().sourceCodeParam(new TokenParam("code-a")).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CmSearchParameter.SP_SOURCE_CODE)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should throw when target-code is provided")
    void shouldThrowWhenTargetCodeIsProvided() {
      // GIVEN
      CmSearchParameter parameters =
          CmSearchParameter.builder().targetCodeParam(new TokenParam("code-b")).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningCmSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CmSearchParameter.SP_TARGET_CODE)
          .hasMessageContaining("not supported");
    }

    private static Stream<Arguments> contentParametersWithUnsupportedModifier() {
      StringParam exactParam = new StringParam("content");
      exactParam.setExact(true);

      StringParam containsParam = new StringParam("content");
      containsParam.setContains(true);

      StringParam textParam = new StringParam("content");
      textParam.setText(true);

      StringParam missingParam = new StringParam();
      missingParam.setMissing(true);

      return Stream.of(
          Arguments.of(exactParam, "exact"),
          Arguments.of(containsParam, "contains"),
          Arguments.of(textParam, "text"),
          Arguments.of(missingParam, "missing=true"));
    }
  }

  private static final class CmSearchParameterTestDataHelper {

    private CmSearchParameterTestDataHelper() {}

    private static CmSearchParameter withSourceCanonicalAndTargetUri() {
      return CmSearchParameter.builder()
          .sourceCanonicalParam(new ReferenceParam(SOURCE_CANONICAL))
          .targetUriParam(new ReferenceParam(TARGET_URI))
          .build();
    }

    private static CmSearchParameter withContent(StringParam contentParam) {
      return CmSearchParameter.builder().contentParam(contentParam).build();
    }
  }
}
