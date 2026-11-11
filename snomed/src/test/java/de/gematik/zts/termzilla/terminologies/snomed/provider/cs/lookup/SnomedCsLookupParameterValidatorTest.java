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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.lookup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import java.util.Arrays;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SnomedCsLookupParameterValidator}. */
@DisplayName("SnomedCsLookupParameterValidator")
class SnomedCsLookupParameterValidatorTest {

  private static CsLookupParameter withProperties(String... properties) {
    return CsLookupParameter.builder()
        .propertyParam(Arrays.stream(properties).map(CodeType::new).toList())
        .build();
  }

  @Nested
  @DisplayName("validateParameters()")
  class ValidateParametersTests {

    @Test
    @DisplayName("should reject the date parameter")
    void shouldRejectDateParameter() {
      // GIVEN
      CsLookupParameter parameters =
          CsLookupParameter.builder().dateParam(new DateTimeType("2024-01-01")).build();

      // WHEN / THEN
      assertThatThrownBy(
              () -> SnomedCsLookupParameterValidator.validateParameters(parameters, false))
          .isInstanceOf(NotImplementedException.class);
    }

    @Test
    @DisplayName("should accept missing property parameters in restricted mode")
    void shouldAcceptMissingPropertiesInRestrictedMode() {
      // GIVEN
      CsLookupParameter parameters = CsLookupParameter.builder().build();

      // WHEN / THEN
      assertThatCode(() -> SnomedCsLookupParameterValidator.validateParameters(parameters, true))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should accept restricted properties outside restricted mode")
    void shouldAcceptRestrictedPropertiesOutsideRestrictedMode() {
      // GIVEN
      CsLookupParameter parameters = withProperties("child", "parent", "sufficientlyDefined");

      // WHEN / THEN
      assertThatCode(() -> SnomedCsLookupParameterValidator.validateParameters(parameters, false))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should accept unrestricted properties in restricted mode")
    void shouldAcceptUnrestrictedPropertiesInRestrictedMode() {
      // GIVEN
      CsLookupParameter parameters =
          withProperties("url", "inactive", "moduleId", "designation", "lang.de");

      // WHEN / THEN
      assertThatCode(() -> SnomedCsLookupParameterValidator.validateParameters(parameters, true))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should reject restricted properties in restricted mode")
    void shouldRejectRestrictedPropertiesInRestrictedMode() {
      // GIVEN
      CsLookupParameter parameters = withProperties("sufficientlyDefined", "url");

      // WHEN / THEN
      assertThatThrownBy(
              () -> SnomedCsLookupParameterValidator.validateParameters(parameters, true))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("[sufficientlyDefined]");
    }
  }
}
