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
package de.gematik.zts.termzilla.terminologies.snomed;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.EXPRESSION_PROCESSING_DISABLED_MESSAGE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.EXPRESSION_PROCESSING_RESTRICTED_MESSAGE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.formatExpressionProcessingDisabledMessage;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SnomedConstants}. */
@DisplayName("SnomedConstants")
class SnomedConstantsTest {

  @Nested
  @DisplayName("formatExpressionProcessingDisabledMessage()")
  class FormatExpressionProcessingDisabledMessageTests {

    @Test
    @DisplayName("should refer to the configuration property outside restricted mode")
    void shouldReferToConfigurationOutsideRestrictedMode() {
      // WHEN
      String message = formatExpressionProcessingDisabledMessage("123", false);

      // THEN
      assertThat(message).isEqualTo(String.format(EXPRESSION_PROCESSING_DISABLED_MESSAGE, "123"));
    }

    @Test
    @DisplayName("should refer to restricted mode in restricted mode")
    void shouldReferToRestrictedModeInRestrictedMode() {
      // WHEN
      String message = formatExpressionProcessingDisabledMessage("123", true);

      // THEN
      assertThat(message)
          .isEqualTo(String.format(EXPRESSION_PROCESSING_RESTRICTED_MESSAGE, "123"))
          .doesNotContain("expressionProcessingEnabled");
    }
  }
}
