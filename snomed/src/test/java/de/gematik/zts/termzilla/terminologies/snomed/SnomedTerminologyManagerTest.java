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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link SnomedTerminologyManager}. */
@DisplayName("SnomedTerminologyManager")
class SnomedTerminologyManagerTest {

  @Nested
  @DisplayName("resolveExpressionProcessingEnabled()")
  class ResolveExpressionProcessingEnabledTests {

    @ParameterizedTest(name = "restrictedMode={0}, expressionProcessingEnabled={1} -> {2}")
    @CsvSource({
      "false, true, true",
      "false, false, false",
      "true, true, false",
      "true, false, false"
    })
    @DisplayName("should disable expression processing whenever restricted mode is active")
    void shouldDisableExpressionProcessingInRestrictedMode(
        boolean restrictedMode, boolean expressionProcessingEnabled, boolean expected) {
      // GIVEN
      SnomedConfigProperties properties = new SnomedConfigProperties();
      properties.setRestrictedMode(restrictedMode);
      properties.setExpressionProcessingEnabled(expressionProcessingEnabled);

      // WHEN
      boolean result = SnomedTerminologyManager.resolveExpressionProcessingEnabled(properties);

      // THEN
      assertThat(result).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should report restricted mode as disabled when SNOMED is not started")
    void shouldReportRestrictedModeDisabledWhenNotStarted(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      SnomedConfigProperties properties = new SnomedConfigProperties();
      properties.setSnomedStart(false);
      properties.setRestrictedMode(true);

      // WHEN
      SnomedTerminologyManager manager =
          new SnomedTerminologyManager(tempDir, "default", properties);

      // THEN
      assertThat(manager.isInitialized()).isFalse();
      assertThat(manager.isRestrictedMode()).isFalse();
      assertThat(manager.isExpressionProcessingEnabled()).isFalse();
    }
  }
}
