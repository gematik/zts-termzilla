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
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ProvisionSettings}. */
@DisplayName("ProvisionSettings")
class ProvisionSettingsTest {

  @Nested
  @DisplayName("addPackageSettings()")
  class AddPackageSettingsTests {

    @Test
    @DisplayName("should add settings for new package")
    void shouldAddSettingsForNewPackage() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("1.0.0");

      // WHEN
      settings.addPackageSettings(packageId, packageSettings);

      // THEN
      assertThat(settings.getPackages())
          .containsKey(packageId)
          .extracting(map -> map.get(packageId))
          .asInstanceOf(LIST)
          .hasSize(1)
          .contains(packageSettings);
    }

    @Test
    @DisplayName("should add multiple settings for same package")
    void shouldAddMultipleSettingsForSamePackage() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings settings1 =
          PackageProvisionSettingsDataHelper.createWithVersion("1.0.0");
      PackageProvisionSettings settings2 =
          PackageProvisionSettingsDataHelper.createWithVersion("2.0.0");

      // WHEN
      settings.addPackageSettings(packageId, settings1);
      settings.addPackageSettings(packageId, settings2);

      // THEN
      assertThat(settings.getPackages().get(packageId))
          .hasSize(2)
          .containsExactly(settings1, settings2);
    }

    @Test
    @DisplayName("should add settings for different packages")
    void shouldAddSettingsForDifferentPackages() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId1 = "test.package1";
      String packageId2 = "test.package2";
      PackageProvisionSettings packageSettings1 =
          PackageProvisionSettingsDataHelper.createWithVersion("1.0.0");
      PackageProvisionSettings packageSettings2 =
          PackageProvisionSettingsDataHelper.createWithVersion("2.0.0");

      // WHEN
      settings.addPackageSettings(packageId1, packageSettings1);
      settings.addPackageSettings(packageId2, packageSettings2);

      // THEN
      assertThat(settings.getPackages()).hasSize(2).containsKeys(packageId1, packageId2);
      assertThat(settings.getPackages().get(packageId1)).containsExactly(packageSettings1);
      assertThat(settings.getPackages().get(packageId2)).containsExactly(packageSettings2);
    }
  }

  @Nested
  @DisplayName("getSettingsForPackage()")
  class GetSettingsForPackageTests {

    @Test
    @DisplayName("should return matching settings for package and version")
    void shouldReturnMatchingSettingsForPackageAndVersion() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("1.0.0");
      packageSettings.setMode(ProvisionMode.PREDEFINED);
      settings.addPackageSettings(packageId, packageSettings);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage(packageId, "1.0.0");

      // THEN
      assertThat(result)
          .isNotNull()
          .isSameAs(packageSettings)
          .extracting(PackageProvisionSettings::getMode)
          .isEqualTo(ProvisionMode.PREDEFINED);
    }

    @Test
    @DisplayName("should return first matching settings when multiple match")
    void shouldReturnFirstMatchingSettingsWhenMultipleMatch() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings settings1 =
          PackageProvisionSettingsDataHelper.createWithVersion("*");
      settings1.setMode(ProvisionMode.PREDEFINED);
      PackageProvisionSettings settings2 =
          PackageProvisionSettingsDataHelper.createWithVersion("^1.0.0");
      settings2.setMode(ProvisionMode.SUPPORTED);
      settings.addPackageSettings(packageId, settings1);
      settings.addPackageSettings(packageId, settings2);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage(packageId, "1.5.0");

      // THEN
      assertThat(result).isSameAs(settings1);
    }

    @Test
    @DisplayName("should return default settings when package not found")
    void shouldReturnDefaultSettingsWhenPackageNotFound() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      PackageProvisionSettings defaultSettings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.SUPPORTED);
      settings.setDefaultSettings(defaultSettings);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage("unknown.package", "1.0.0");

      // THEN
      assertThat(result).isSameAs(defaultSettings);
    }

    @Test
    @DisplayName("should return default settings when version does not match")
    void shouldReturnDefaultSettingsWhenVersionDoesNotMatch() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("^1.0.0");
      settings.addPackageSettings(packageId, packageSettings);
      PackageProvisionSettings defaultSettings =
          PackageProvisionSettingsDataHelper.createWithMode(ProvisionMode.PREDEFINED);
      settings.setDefaultSettings(defaultSettings);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage(packageId, "2.0.0");

      // THEN
      assertThat(result).isSameAs(defaultSettings);
    }

    @Test
    @DisplayName("should return null when no default settings and package not found")
    void shouldReturnNullWhenNoDefaultSettingsAndPackageNotFound() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage("unknown.package", "1.0.0");

      // THEN
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle wildcard version constraint")
    void shouldHandleWildcardVersionConstraint() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("*");
      packageSettings.setMode(ProvisionMode.PREDEFINED);
      settings.addPackageSettings(packageId, packageSettings);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage(packageId, "99.99.99");

      // THEN
      assertThat(result)
          .isSameAs(packageSettings)
          .extracting(PackageProvisionSettings::getMode)
          .isEqualTo(ProvisionMode.PREDEFINED);
    }

    @Test
    @DisplayName("should handle caret range version constraint")
    void shouldHandleCaretRangeVersionConstraint() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("^1.0.0");
      packageSettings.setMode(ProvisionMode.PREDEFINED);
      settings.addPackageSettings(packageId, packageSettings);

      // WHEN
      PackageProvisionSettings result = settings.getSettingsForPackage(packageId, "1.5.0");

      // THEN
      assertThat(result)
          .isSameAs(packageSettings)
          .extracting(PackageProvisionSettings::getMode)
          .isEqualTo(ProvisionMode.PREDEFINED);
    }
  }

  @Nested
  @DisplayName("hasPackageSettings()")
  class HasPackageSettingsTests {

    @Test
    @DisplayName("should return true when package has settings")
    void shouldReturnTrueWhenPackageHasSettings() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      PackageProvisionSettings packageSettings =
          PackageProvisionSettingsDataHelper.createWithVersion("1.0.0");
      settings.addPackageSettings(packageId, packageSettings);

      // WHEN
      boolean hasSettings = settings.hasPackageSettings(packageId);

      // THEN
      assertThat(hasSettings).isTrue();
    }

    @Test
    @DisplayName("should return false when package has no settings")
    void shouldReturnFalseWhenPackageHasNoSettings() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();

      // WHEN
      boolean hasSettings = settings.hasPackageSettings("unknown.package");

      // THEN
      assertThat(hasSettings).isFalse();
    }

    @Test
    @DisplayName("should return true when package has multiple settings")
    void shouldReturnTrueWhenPackageHasMultipleSettings() {
      // GIVEN
      ProvisionSettings settings = new ProvisionSettings();
      String packageId = "test.package";
      settings.addPackageSettings(
          packageId, PackageProvisionSettingsDataHelper.createWithVersion("1.0.0"));
      settings.addPackageSettings(
          packageId, PackageProvisionSettingsDataHelper.createWithVersion("2.0.0"));

      // WHEN
      boolean hasSettings = settings.hasPackageSettings(packageId);

      // THEN
      assertThat(hasSettings).isTrue();
    }
  }
}
