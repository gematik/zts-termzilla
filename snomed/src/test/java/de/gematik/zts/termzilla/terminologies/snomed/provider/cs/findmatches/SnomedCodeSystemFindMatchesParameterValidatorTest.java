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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.findmatches;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import java.util.Arrays;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link SnomedCodeSystemFindMatchesParameterValidator}. */
@DisplayName("SnomedCodeSystemFindMatchesParameterValidator")
class SnomedCodeSystemFindMatchesParameterValidatorTest {

  private static final String CONCEPT_ID = "73211009";

  private static CsFindMatchesParameter withPropertyFilters(String... propertyCodes) {
    return CsFindMatchesParameter.builder()
        .systemParam(new UriType("http://snomed.info/sct"))
        .exactParam(new BooleanType(true))
        .propertyParamList(
            Arrays.stream(propertyCodes)
                .map(SnomedCodeSystemFindMatchesParameterValidatorTest::propertyFilter)
                .toList())
        .build();
  }

  private static Parameters.ParametersParameterComponent propertyFilter(String propertyCode) {
    Parameters.ParametersParameterComponent property =
        new Parameters.ParametersParameterComponent();
    property.setName(CsFindMatchesParameter.PROPERTY_PARAM);
    property.addPart().setName("code").setValue(new CodeType(propertyCode));
    property.addPart().setName("value").setValue(new CodeType(CONCEPT_ID));
    return property;
  }

  @Nested
  @DisplayName("validateParameters()")
  class ValidateParametersTests {

    @Test
    @DisplayName("should accept relationship property filters outside restricted mode")
    void shouldAcceptRelationshipFiltersOutsideRestrictedMode() {
      // GIVEN
      CsFindMatchesParameter parameters = withPropertyFilters("parent", "child");
      SnomedCodeSystemFindMatchesParameterValidator validator =
          new SnomedCodeSystemFindMatchesParameterValidator(false);

      // WHEN / THEN
      assertThatCode(() -> validator.validateParameters(parameters)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should accept code and display filters in restricted mode")
    void shouldAcceptUnrestrictedFiltersInRestrictedMode() {
      // GIVEN
      CsFindMatchesParameter parameters = withPropertyFilters("code", "display");
      SnomedCodeSystemFindMatchesParameterValidator validator =
          new SnomedCodeSystemFindMatchesParameterValidator(true);

      // WHEN / THEN
      assertThatCode(() -> validator.validateParameters(parameters)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "property ''{0}''")
    @ValueSource(strings = {"parent", "child"})
    @DisplayName("should reject relationship property filters in restricted mode")
    void shouldRejectRelationshipFiltersInRestrictedMode(String propertyCode) {
      // GIVEN
      CsFindMatchesParameter parameters = withPropertyFilters("code", propertyCode);
      SnomedCodeSystemFindMatchesParameterValidator validator =
          new SnomedCodeSystemFindMatchesParameterValidator(true);

      // WHEN / THEN
      assertThatThrownBy(() -> validator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("'" + propertyCode + "'")
          .hasMessageContaining("restricted mode");
    }
  }
}
