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
package de.gematik.zts.termzilla.common.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link UnsupportedCsValidateCodeParameterValidator}. */
@DisplayName("UnsupportedCsValidateCodeParameterValidator")
class UnsupportedCsValidateCodeParameterValidatorTest {

  @Nested
  @DisplayName("validateUnsupportedParameters")
  class ValidateUnsupportedParametersTests {

    private static Stream<Arguments> unsupportedParameterMutations() {
      return Stream.of(
          Arguments.of(
              CsValidateCodeParameter.DATE_PARAM,
              (Consumer<CsValidateCodeParameter>)
                  parameters -> parameters.setDateParam(new DateTimeType("2024-01-01"))),
          Arguments.of(
              CsValidateCodeParameter.CODING_PARAM,
              (Consumer<CsValidateCodeParameter>)
                  parameters -> parameters.setCodingParam(new Coding())),
          Arguments.of(
              CsValidateCodeParameter.CODEABLECONCEPT_PARAM,
              (Consumer<CsValidateCodeParameter>)
                  parameters -> parameters.setCodeableConceptParam(new CodeableConcept())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedParameterMutations")
    @DisplayName("Should throw NotImplementedException when an unsupported parameter is provided")
    void shouldThrowNotImplementedExceptionWhenUnsupportedParameterIsProvided(
        String expectedMessagePart, Consumer<CsValidateCodeParameter> mutation) {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameterTestDataHelper.createMinimalParameter();
      mutation.accept(parameters);

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  UnsupportedCsValidateCodeParameterValidator.validateUnsupportedParameters(
                      parameters))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining(expectedMessagePart);
    }

    @Test
    @DisplayName("Should throw NotImplementedException when abstract is false")
    void shouldThrowNotImplementedExceptionWhenAbstractIsFalse() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameterTestDataHelper.createMinimalParameter();
      parameters.setAbstractParam(new BooleanType(false));

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  UnsupportedCsValidateCodeParameterValidator.validateUnsupportedParameters(
                      parameters))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining(CsValidateCodeParameter.ABSTRACT_PARAM);
    }

    @Test
    @DisplayName("Should not throw when abstract is true")
    void shouldNotThrowWhenAbstractIsTrue() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameterTestDataHelper.createMinimalParameter();
      parameters.setAbstractParam(new BooleanType(true));

      // WHEN / THEN
      assertThatCode(
              () ->
                  UnsupportedCsValidateCodeParameterValidator.validateUnsupportedParameters(
                      parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should not throw when only supported parameters are provided")
    void shouldNotThrowWhenOnlySupportedParametersAreProvided() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameterTestDataHelper.createMinimalParameter();

      // WHEN / THEN
      assertThatCode(
              () ->
                  UnsupportedCsValidateCodeParameterValidator.validateUnsupportedParameters(
                      parameters))
          .doesNotThrowAnyException();
    }
  }
}
