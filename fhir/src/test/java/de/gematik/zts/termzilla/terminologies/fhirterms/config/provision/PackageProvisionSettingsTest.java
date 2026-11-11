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
package de.gematik.zts.termzilla.terminologies.fhirterms.config.provision;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link PackageProvisionSettings}. */
@DisplayName("PackageProvisionSettings")
class PackageProvisionSettingsTest {

  @Nested
  @DisplayName("getEffectiveMode()")
  class GetEffectiveModeTests {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
        http://example.org/fhir/CodeSystem/*                  | http://example.org/fhir/CodeSystem/test                 | wildcard at end matches
        http://example.org/fhir/CodeSystem/exact              | http://example.org/fhir/CodeSystem/exact                | exact URL without wildcard matches
        http://example.org/fhir/ValueSet/administrative-*     | http://example.org/fhir/ValueSet/administrative-gender  | wildcard at end with prefix matches
        */CodeSystem/test                                     | http://example.org/fhir/CodeSystem/test                 | wildcard at beginning matches
        http://example.org/*/CodeSystem/test                  | http://example.org/fhir/CodeSystem/test                 | wildcard in middle matches
        http://example.org/fhir/CodeSystem/test.value         | http://example.org/fhir/CodeSystem/test.value           | special regex characters are escaped
        """)
    @DisplayName("should return SUPPORTED when URL matches supported pattern")
    void shouldReturnSupportedWhenUrlMatchesSupportedPattern(
        String pattern, String resourceUrl, String description) {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.PREDEFINED);
      settings.setSupported(List.of(pattern));

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should return PREDEFINED when URL matches predefined pattern")
    void shouldReturnPredefinedWhenUrlMatchesPredefinedPattern() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.SUPPORTED);
      settings.setPredefined(List.of("http://example.org/fhir/ValueSet/*"));
      String resourceUrl = "http://example.org/fhir/ValueSet/administrative";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.PREDEFINED);
    }

    @Test
    @DisplayName("should return default mode when URL matches no exception pattern")
    void shouldReturnDefaultModeWhenUrlMatchesNoExceptionPattern() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.PREDEFINED);
      settings.setSupported(List.of("http://example.org/fhir/CodeSystem/*"));
      String resourceUrl = "http://other.org/fhir/ValueSet/test";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.PREDEFINED);
    }

    @Test
    @DisplayName("should return SUPPORTED as fallback when mode is null")
    void shouldReturnSupportedAsFallbackWhenModeIsNull() {
      // GIVEN
      PackageProvisionSettings settings = new PackageProvisionSettings();
      String resourceUrl = "http://example.org/fhir/CodeSystem/test";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should prioritize supported over predefined when URL matches both")
    void shouldPrioritizeSupportedOverPredefinedWhenUrlMatchesBoth() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.PREDEFINED);
      settings.setSupported(List.of("http://example.org/fhir/*"));
      settings.setPredefined(List.of("http://example.org/fhir/CodeSystem/*"));
      String resourceUrl = "http://example.org/fhir/CodeSystem/test";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should not match when exact URL differs")
    void shouldNotMatchWhenExactUrlDiffers() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.SUPPORTED);
      settings.setPredefined(List.of("http://example.org/fhir/CodeSystem/exact"));
      String resourceUrl = "http://example.org/fhir/CodeSystem/different";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should handle empty exception lists")
    void shouldHandleEmptyExceptionLists() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.PREDEFINED);
      String resourceUrl = "http://example.org/fhir/CodeSystem/test";

      // WHEN
      ProvisionMode effectiveMode = settings.getEffectiveMode(resourceUrl);

      // THEN
      assertThat(effectiveMode).isEqualTo(ProvisionMode.PREDEFINED);
    }
  }

  @Nested
  @DisplayName("matchesVersion()")
  class MatchesVersionTests {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
        *               | 1.5.0          | true  | wildcard matches any version

        1.5.0           | 1.5.0          | true  | exact version matches
        custom-version  | custom-version | true  | exact string match when semver parsing fails
        """)
    @DisplayName("should match version using wildcard, exact, or string matching")
    void shouldMatchVersionUsingWildcardExactOrStringMatching(
        String constraint, String actualVersion, boolean expected, String description) {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithVersion(constraint);

      // WHEN
      boolean matches = settings.matchesVersion(actualVersion);

      // THEN
      assertThat(matches).isEqualTo(expected);
    }

    @Test
    @DisplayName("should match when version constraint is null")
    void shouldMatchWhenVersionConstraintIsNull() {
      // GIVEN
      PackageProvisionSettings settings = new PackageProvisionSettings();
      String actualVersion = "1.5.0";

      // WHEN
      boolean matches = settings.matchesVersion(actualVersion);

      // THEN
      assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("should not match different exact version")
    void shouldNotMatchDifferentExactVersion() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithVersion("1.5.0");
      String actualVersion = "1.6.0";

      // WHEN
      boolean matches = settings.matchesVersion(actualVersion);

      // THEN
      assertThat(matches).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
      "^1.0.0, 1.5.0, true",
      "^1.0.0, 1.0.0, true",
      "^1.0.0, 1.9.9, true",
      "^1.0.0, 2.0.0, false",
      "^1.0.0, 0.9.0, false",
      "~1.2.3, 1.2.3, true",
      "~1.2.3, 1.2.4, true",
      "~1.2.3, 1.2.9, true",
      "~1.2.3, 1.3.0, false",
      "~1.2.3, 1.1.0, false",
      ">=1.0.0 <2.0.0, 1.0.0, true",
      ">=1.0.0 <2.0.0, 1.5.0, true",
      ">=1.0.0 <2.0.0, 1.9.9, true",
      ">=1.0.0 <2.0.0, 2.0.0, false",
      ">=1.0.0 <2.0.0, 0.9.0, false"
    })
    @DisplayName("should handle semver range constraints")
    void shouldHandleSemverRangeConstraints(String constraint, String version, boolean expected) {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithVersion(constraint);

      // WHEN
      boolean matches = settings.matchesVersion(version);

      // THEN
      assertThat(matches).isEqualTo(expected);
    }

    @Test
    @DisplayName("should not match when semver parsing fails and strings differ")
    void shouldNotMatchWhenSemverParsingFailsAndStringsDiffer() {
      // GIVEN
      PackageProvisionSettings settings =
          PackageProvisionSettingsDataHelper.createWithVersion("custom-version");
      String actualVersion = "other-version";

      // WHEN
      boolean matches = settings.matchesVersion(actualVersion);

      // THEN
      assertThat(matches).isFalse();
    }
  }
}
