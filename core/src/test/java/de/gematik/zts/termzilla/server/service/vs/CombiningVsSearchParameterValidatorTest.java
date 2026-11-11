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
package de.gematik.zts.termzilla.server.service.vs;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CombiningVsSearchParameterValidator}. */
@DisplayName("CombiningVsSearchParameterValidator")
class CombiningVsSearchParameterValidatorTest {

  @Nested
  @DisplayName("validateParameters")
  class ValidateParametersTests {

    @Test
    @DisplayName("should throw when expansion parameter is provided")
    void shouldThrowWhenExpansionParameterIsProvided() {
      // GIVEN
      VsSearchParameter parameters =
          VsSearchParameter.builder()
              .expansionParam(new UriParam("http://example.org/expansion"))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsSearchParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsSearchParameter.SP_EXPANSION)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("should pass when expansion parameter is not provided")
    void shouldPassWhenExpansionParameterIsNotProvided() {
      // GIVEN
      VsSearchParameter parameters =
          VsSearchParameter.builder()
              .urlParam(new UriParam("http://example.org/ValueSet/test"))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsSearchParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }
  }
}
