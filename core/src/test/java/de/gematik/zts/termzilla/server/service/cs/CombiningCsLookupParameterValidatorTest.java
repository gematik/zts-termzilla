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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import java.util.Date;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link CombiningCsLookupParameterValidator}. */
@DisplayName("CombiningCsLookupParameterValidator")
class CombiningCsLookupParameterValidatorTest {

  private static final String TEST_SYSTEM = "http://system";
  private static final String TEST_CODE = "123";

  @Nested
  @DisplayName("validateParameters")
  class ValidateParametersTests {

    @Test
    @DisplayName("should pass when system and code are provided")
    void shouldPassWhenSystemAndCodeAreProvidedAndDateIsAbsent() {
      // GIVEN
      CsLookupParameter parameters = createSystemAndCodeParameters();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should pass when coding is provided with code and system")
    void shouldPassWhenCodingIsProvidedWithCodeAndSystemAndDateIsAbsent() {
      // GIVEN
      CsLookupParameter parameters = createCodingParameters();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when neither system+code nor coding is provided")
    void shouldThrowWhenNeitherSystemCodeNorCodingIsProvided() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("Either use");
    }

    @Test
    @DisplayName("should throw when both system+code and coding are provided")
    void shouldThrowWhenBothSystemCodeAndCodingAreProvided() {
      // GIVEN
      CsLookupParameter parameters = createSystemAndCodeParameters();
      parameters.setCodingParam(new Coding("http://coding-system", "ABC", null));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("Either use");
    }

    @Test
    @DisplayName("should throw when coding is provided without code")
    void shouldThrowWhenCodingIsProvidedWithoutCode() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();
      parameters.setCodingParam(new Coding("http://coding-system", "", null));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("must have a code");
    }

    @Test
    @DisplayName("should throw when coding is provided without system")
    void shouldThrowWhenCodingIsProvidedWithoutSystem() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();
      parameters.setCodingParam(new Coding("", "ABC", null));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("must have a system");
    }

    @Test
    @DisplayName("should throw when only system is provided")
    void shouldThrowWhenOnlySystemIsProvided() {
      // GIVEN
      CsLookupParameter parameters =
          CsLookupParameter.builder().systemParam(new UriType(TEST_SYSTEM)).build();

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("Either use");
    }

    @Test
    @DisplayName("should throw when only code is provided")
    void shouldThrowWhenOnlyCodeIsProvided() {
      // GIVEN
      CsLookupParameter parameters =
          CsLookupParameter.builder().codeParam(new CodeType(TEST_CODE)).build();

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("Either use");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPartialSystemAndCodeParameters")
    @DisplayName("should throw when system and code are partially provided")
    void shouldThrowWhenSystemAndCodeArePartiallyProvided(
        String scenario, CsLookupParameter parameters) {
      // GIVEN

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("Either use");
    }

    @Test
    @DisplayName("should throw when coding is provided with null code")
    void shouldThrowWhenCodingIsProvidedWithNullCode() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();
      parameters.setCodingParam(new Coding("http://coding-system", null, null));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("must have a code");
    }

    @Test
    @DisplayName("should throw when coding is provided with null system")
    void shouldThrowWhenCodingIsProvidedWithNullSystem() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();
      parameters.setCodingParam(new Coding(null, "ABC", null));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("must have a system");
    }

    @Test
    @DisplayName("should throw when date is in the future")
    void shouldThrowWhenDateIsInTheFuture() {
      // GIVEN
      CsLookupParameter parameters = createSystemAndCodeParameters();
      parameters.setDateParam(new org.hl7.fhir.r4.model.DateTimeType(new Date(Long.MAX_VALUE)));

      // WHEN / THEN
      assertThatIllegalArgumentException()
          .isThrownBy(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .withMessageContaining("must not be in the future");
    }

    @Test
    @DisplayName("should pass when date is in the past")
    void shouldPassWhenDateIsInThePast() {
      // GIVEN
      CsLookupParameter parameters = createSystemAndCodeParameters();
      parameters.setDateParam(new org.hl7.fhir.r4.model.DateTimeType(new Date(0L)));

      // WHEN / THEN
      assertThatCode(() -> CombiningCsLookupParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    private static Stream<Arguments> invalidPartialSystemAndCodeParameters() {
      CsLookupParameter onlySystem =
          CsLookupParameter.builder().systemParam(new UriType(TEST_SYSTEM)).build();
      CsLookupParameter onlyCode =
          CsLookupParameter.builder().codeParam(new CodeType(TEST_CODE)).build();
      CsLookupParameter emptySystemWithCode =
          CsLookupParameter.builder()
              .systemParam(new UriType(""))
              .codeParam(new CodeType(TEST_CODE))
              .build();
      CsLookupParameter emptyCodeWithSystem =
          CsLookupParameter.builder()
              .systemParam(new UriType(TEST_SYSTEM))
              .codeParam(new CodeType(""))
              .build();

      return Stream.of(
          Arguments.of("system without code", onlySystem),
          Arguments.of("code without system", onlyCode),
          Arguments.of("empty system with code", emptySystemWithCode),
          Arguments.of("empty code with system", emptyCodeWithSystem));
    }
  }

  private static CsLookupParameter createSystemAndCodeParameters() {
    return CsLookupParameter.builder()
        .systemParam(new UriType(TEST_SYSTEM))
        .codeParam(new CodeType(TEST_CODE))
        .build();
  }

  private static CsLookupParameter createCodingParameters() {
    return CsLookupParameter.builder()
        .codingParam(new Coding("http://coding-system", "ABC", null))
        .build();
  }
}
