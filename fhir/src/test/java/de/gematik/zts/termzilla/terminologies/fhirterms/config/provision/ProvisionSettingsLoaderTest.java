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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link ProvisionSettingsLoader}. */
@DisplayName("ProvisionSettingsLoader")
class ProvisionSettingsLoaderTest {

  private ProvisionSettingsLoader loader;

  @BeforeEach
  void setUp() {
    loader = new ProvisionSettingsLoader();
  }

  @Nested
  @DisplayName("load(Path)")
  class LoadFromPathTests {

    @Test
    @DisplayName("should load valid provision settings from file")
    void shouldLoadValidProvisionSettingsFromFile(@TempDir Path tempDir)
        throws IOException, ProvisionSettingsException {
      // GIVEN
      String yaml = ProvisionSettingsYamlHelper.createCompleteYaml();
      Path yamlFile = tempDir.resolve("provision-settings.yaml");
      Files.writeString(yamlFile, yaml);

      // WHEN
      ProvisionSettings settings = loader.load(yamlFile);

      // THEN
      assertThat(settings).isNotNull();
      assertThat(settings.getDefaultSettings()).isNull(); // Changed: no default in example YAML
      assertThat(settings.getPackages()).isNotEmpty().hasSize(4);
    }

    @Test
    @DisplayName("should throw exception when file path is null")
    void shouldThrowExceptionWhenFilePathIsNull() {
      // GIVEN
      Path nullPath = null;

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(nullPath))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("file path is null");
    }

    @Test
    @DisplayName("should throw exception when file does not exist")
    void shouldThrowExceptionWhenFileDoesNotExist(@TempDir Path tempDir) {
      // GIVEN
      Path nonExistentFile = tempDir.resolve("non-existent.yaml");

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(nonExistentFile))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("file not found");
    }
  }

  @Nested
  @DisplayName("load(InputStream)")
  class LoadFromInputStreamTests {

    @Test
    @DisplayName("should load complete provision settings")
    void shouldLoadCompleteProvisionSettings() throws ProvisionSettingsException {
      // GIVEN
      String yaml = ProvisionSettingsYamlHelper.createCompleteYaml();
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      assertThat(settings).isNotNull();
      assertThat(settings.getDefaultSettings()).isNull(); // Changed: no default in example YAML
      assertThat(settings.getPackages()).hasSize(4);
    }

    @Test
    @DisplayName("should load default settings correctly")
    void shouldLoadDefaultSettingsCorrectly() throws ProvisionSettingsException {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithDefaultSettings(ProvisionMode.PREDEFINED);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      assertThat(settings.getDefaultSettings())
          .isNotNull()
          .extracting(PackageProvisionSettings::getMode)
          .isEqualTo(ProvisionMode.PREDEFINED);
    }

    @Test
    @DisplayName("should load package with wildcard version")
    void shouldLoadPackageWithWildcardVersion() throws ProvisionSettingsException {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithSinglePackage(
              "bfarm.terminologien.orphacodes", "*", ProvisionMode.SUPPORTED, null, null);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("bfarm.terminologien.orphacodes", "1.0.0");
      assertThat(packageSettings)
          .isNotNull()
          .satisfies(
              ps -> {
                assertThat(ps.getVersion()).isEqualTo("*");
                assertThat(ps.getMode()).isEqualTo(ProvisionMode.SUPPORTED);
              });
    }

    @Test
    @DisplayName("should load package with caret version constraint")
    void shouldLoadPackageWithCaretVersionConstraint() throws ProvisionSettingsException {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithSinglePackage(
              "bfarm.terminologien.ops",
              "^2022.0.0",
              ProvisionMode.PREDEFINED,
              List.of("http://fhir.de/CodeSystem/bfarm/ops"),
              null);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("bfarm.terminologien.ops", "2022.1.0");
      assertThat(packageSettings)
          .isNotNull()
          .satisfies(
              ps -> {
                assertThat(ps.getVersion()).isEqualTo("^2022.0.0");
                assertThat(ps.getMode()).isEqualTo(ProvisionMode.PREDEFINED);
                assertThat(ps.getSupported())
                    .containsExactly("http://fhir.de/CodeSystem/bfarm/ops");
              });
    }

    @Test
    @DisplayName("should load package with supported list")
    void shouldLoadPackageWithSupportedList() throws ProvisionSettingsException {
      // GIVEN
      List<String> supportedUrls =
          List.of(
              "https://terminologien.bfarm.de/fhir/ValueSet/*",
              "http://example.org/fhir/CodeSystem/test");
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithSinglePackage(
              "test.package", "*", ProvisionMode.PREDEFINED, supportedUrls, null);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("test.package", "1.0.0");
      assertThat(packageSettings.getSupported())
          .hasSize(2)
          .containsExactlyElementsOf(supportedUrls);
    }

    @Test
    @DisplayName("should load package with predefined list")
    void shouldLoadPackageWithPredefinedList() throws ProvisionSettingsException {
      // GIVEN
      List<String> predefinedUrls =
          List.of(
              "http://example.org/fhir/ValueSet/administrative-*",
              "http://example.org/fhir/CodeSystem/exact");
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithSinglePackage(
              "test.package", "*", ProvisionMode.SUPPORTED, null, predefinedUrls);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("test.package", "1.0.0");
      assertThat(packageSettings.getPredefined())
          .hasSize(2)
          .containsExactlyElementsOf(predefinedUrls);
    }

    @Test
    @DisplayName("should load multiple packages with same id")
    void shouldLoadMultiplePackagesWithSameId() throws ProvisionSettingsException {
      // GIVEN
      String yaml = ProvisionSettingsYamlHelper.createYamlWithMultipleVersionsOfSamePackage();
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      assertThat(settings.getPackages().get("bfarm.terminologien.ops")).hasSize(2);
      PackageProvisionSettings settings2022 =
          settings.getSettingsForPackage("bfarm.terminologien.ops", "2022.1.0");
      PackageProvisionSettings settings2023 =
          settings.getSettingsForPackage("bfarm.terminologien.ops", "2023.1.0");
      assertThat(settings2022.getMode()).isEqualTo(ProvisionMode.PREDEFINED);
      assertThat(settings2023.getMode()).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should load package without version constraint")
    void shouldLoadPackageWithoutVersionConstraint() throws ProvisionSettingsException {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithPackageWithoutVersion(
              "bfarm.terminologien.loinc", ProvisionMode.SUPPORTED);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("bfarm.terminologien.loinc", "1.0.0");
      assertThat(packageSettings)
          .isNotNull()
          .satisfies(
              ps -> {
                assertThat(ps.getVersion()).isNull();
                assertThat(ps.getMode()).isEqualTo(ProvisionMode.SUPPORTED);
              });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidYamlStructures")
    @DisplayName("should throw exception for invalid YAML structure")
    void shouldThrowExceptionForInvalidYamlStructure(String yaml, String description) {
      // GIVEN
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(inputStream))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("missing 'provision-settings' root key");
    }

    private static Stream<Arguments> provideInvalidYamlStructures() {
      return Stream.of(
          Arguments.of("invalid:\n  key: value", "root key is missing"),
          Arguments.of("", "YAML is empty"));
    }

    @Test
    @DisplayName("should throw exception when package is missing id")
    void shouldThrowExceptionWhenPackageIsMissingId() {
      // GIVEN
      String yaml =
          """
          provision-settings:
            packages:
              - version: "*"

                mode: supported
          """;
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(inputStream))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("missing 'id' field");
    }

    @Test
    @DisplayName("should throw exception when mode is invalid")
    void shouldThrowExceptionWhenModeIsInvalid() {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithInvalidMode(
              "bfarm.terminologien.test", "invalid_mode");
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(inputStream))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("Invalid provision mode");
    }

    @Test
    @DisplayName("should throw exception when packages is not a list")
    void shouldThrowExceptionWhenPackagesIsNotAList() {
      // GIVEN
      String yaml =
          """
          provision-settings:
            packages: "not a list"
          """;
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(inputStream))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("'packages' must be a list");
    }

    @Test
    @DisplayName("should throw exception when YAML is malformed")
    void shouldThrowExceptionWhenYamlIsMalformed() {
      // GIVEN
      String yaml = "provision-settings:\n  packages:\n    - id: test\n      mode: [unclosed";
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN & THEN
      assertThatThrownBy(() -> loader.load(inputStream))
          .isInstanceOf(ProvisionSettingsException.class)
          .hasMessageContaining("Failed to parse provision settings");
    }

    @Test
    @DisplayName("should handle mode case insensitivity")
    void shouldHandleModeCaseInsensitivity() throws ProvisionSettingsException {
      // GIVEN
      String yaml =
          ProvisionSettingsYamlHelper.createYamlWithSinglePackage(
              "test.package", "*", "SuPpOrTeD", null, null);
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      PackageProvisionSettings packageSettings =
          settings.getSettingsForPackage("test.package", "1.0.0");
      assertThat(packageSettings.getMode()).isEqualTo(ProvisionMode.SUPPORTED);
    }

    @Test
    @DisplayName("should load settings without default configuration")
    void shouldLoadSettingsWithoutDefaultConfiguration() throws ProvisionSettingsException {
      // GIVEN
      String yaml = ProvisionSettingsYamlHelper.createYamlWithoutDefaultSettings();
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      assertThat(settings.getDefaultSettings()).isNull();
      assertThat(settings.getPackages()).isNotEmpty();
    }

    @Test
    @DisplayName("should load settings without packages")
    void shouldLoadSettingsWithoutPackages() throws ProvisionSettingsException {
      // GIVEN
      String yaml = ProvisionSettingsYamlHelper.createYamlWithOnlyDefaultSettings();
      InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

      // WHEN
      ProvisionSettings settings = loader.load(inputStream);

      // THEN
      assertThat(settings.getDefaultSettings()).isNotNull();
      assertThat(settings.getPackages()).isEmpty();
    }
  }
}
