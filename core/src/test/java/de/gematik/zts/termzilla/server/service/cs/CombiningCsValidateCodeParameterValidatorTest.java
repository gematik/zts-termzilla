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

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import java.util.List;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CombiningCsValidateCodeParameterValidator}. */
@DisplayName("CombiningCsValidateCodeParameterValidator")
class CombiningCsValidateCodeParameterValidatorTest {

  private static final String URL = "http://example.org/CodeSystem/test";
  private static final String CODE = "12345";

  @Nested
  @DisplayName("validateParameters")
  class ValidateParametersTests {

    @Test
    @DisplayName("should throw when displayLanguage is set without any display information")
    void shouldThrowWhenDisplayLanguageIsSetWithoutAnyDisplayInformation() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(URL))
              .codeParam(new CodeType(CODE))
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM)
          .hasMessageContaining(CsValidateCodeParameter.DISPLAY_PARAM);
    }

    @Test
    @DisplayName("should pass when displayLanguage is set and display parameter is present")
    void shouldPassWhenDisplayLanguageIsSetAndDisplayParameterIsPresent() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(URL))
              .codeParam(new CodeType(CODE))
              .displayParam(new StringType("A display text"))
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should pass when displayLanguage is set and coding display is present")
    void shouldPassWhenDisplayLanguageIsSetAndCodingDisplayIsPresent() {
      // GIVEN
      Coding coding = new Coding("http://example.org/system", CODE, "Display from coding");
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .codingParam(coding)
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when displayLanguage is set and coding has no display")
    void shouldThrowWhenDisplayLanguageIsSetAndCodingHasNoDisplay() {
      // GIVEN
      Coding coding = new Coding("http://example.org/system", CODE, null);
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .codingParam(coding)
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM)
          .hasMessageContaining(CsValidateCodeParameter.CODING_PARAM);
    }

    @Test
    @DisplayName(
        "should pass when displayLanguage is set and codeableConcept coding display is present")
    void shouldPassWhenDisplayLanguageIsSetAndCodeableConceptCodingDisplayIsPresent() {
      // GIVEN
      CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.setCoding(
          List.of(new Coding("http://example.org/system", CODE, "Display from concept coding")));

      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .codeableConceptParam(codeableConcept)
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when displayLanguage is set and codeableConcept has no display")
    void shouldThrowWhenDisplayLanguageIsSetAndCodeableConceptHasNoDisplay() {
      // GIVEN
      CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.setCoding(List.of(new Coding("http://example.org/system", CODE, null)));

      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .codeableConceptParam(codeableConcept)
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM)
          .hasMessageContaining(CsValidateCodeParameter.CODEABLECONCEPT_PARAM);
    }

    @Test
    @DisplayName(
        "should throw when displayLanguage is set and only codeableConcept text is present")
    void shouldThrowWhenDisplayLanguageIsSetAndOnlyCodeableConceptTextIsPresent() {
      // GIVEN
      CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.setText("Display from concept text");
      codeableConcept.setCoding(List.of(new Coding("http://example.org/system", CODE, null)));

      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .codeableConceptParam(codeableConcept)
              .displayLanguageParam(new CodeType("de"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM)
          .hasMessageContaining(CsValidateCodeParameter.CODEABLECONCEPT_PARAM)
          .hasMessageContaining("coding.display");
    }

    @Test
    @DisplayName("should pass when displayLanguage is empty")
    void shouldPassWhenDisplayLanguageIsEmpty() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(URL))
              .codeParam(new CodeType(CODE))
              .displayLanguageParam(new CodeType(""))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw when date parameter is provided")
    void shouldThrowWhenDateParameterIsProvided() {
      // GIVEN
      CsValidateCodeParameter parameters =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(URL))
              .codeParam(new CodeType(CODE))
              .dateParam(new org.hl7.fhir.r4.model.DateTimeType("2026-01-01"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> CombiningCsValidateCodeParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(CsValidateCodeParameter.DATE_PARAM)
          .hasMessageContaining("not supported");
    }
  }
}
