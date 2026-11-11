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

import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads and parses provision settings from YAML files.
 *
 * <p>Supports multiple configurations per package for different version ranges.
 */
public class ProvisionSettingsLoader {

  private final Yaml yaml;

  /** Creates a new loader instance with default YAML configuration. */
  public ProvisionSettingsLoader() {
    LoaderOptions loaderOptions = new LoaderOptions();
    this.yaml = new Yaml(loaderOptions);
  }

  /**
   * Loads provision settings from a file path.
   *
   * @param filePath path to the YAML file
   * @return parsed provision settings
   * @throws IOException if the file cannot be read
   * @throws ProvisionSettingsException if parsing fails or the file structure is invalid
   */
  public ProvisionSettings load(Path filePath) throws IOException, ProvisionSettingsException {
    if (filePath == null) {
      throw new ProvisionSettingsException("Provision settings file path is null");
    }

    if (!Files.exists(filePath)) {
      throw new ProvisionSettingsException("Provision settings file not found: " + filePath);
    }

    try (InputStream inputStream = Files.newInputStream(filePath)) {
      return load(inputStream);
    }
  }

  /**
   * Loads provision settings from an input stream.
   *
   * @param inputStream the YAML input stream
   * @return parsed provision settings
   * @throws ProvisionSettingsException if parsing fails or the structure is invalid
   */
  @SuppressWarnings("unchecked")
  public ProvisionSettings load(InputStream inputStream) throws ProvisionSettingsException {
    try {
      Map<String, Object> rawData = yaml.load(inputStream);

      if (rawData == null || !rawData.containsKey("provision-settings")) {
        throw new ProvisionSettingsException(
            "Invalid provision settings file: missing 'provision-settings' root key");
      }

      Map<String, Object> settingsMap = (Map<String, Object>) rawData.get("provision-settings");

      return parseProvisionSettings(settingsMap);

    } catch (ProvisionSettingsException e) {
      throw e;
    } catch (Exception e) {
      throw new ProvisionSettingsException(
          "Failed to parse provision settings: " + e.getMessage(), e);
    }
  }

  /**
   * Parses the root provision settings map.
   *
   * @param settingsMap the settings map from YAML
   * @return parsed provision settings
   * @throws ProvisionSettingsException if the structure is invalid
   */
  @SuppressWarnings("unchecked")
  private ProvisionSettings parseProvisionSettings(Map<String, Object> settingsMap)
      throws ProvisionSettingsException {
    ProvisionSettings settings = new ProvisionSettings();

    // Parse default settings
    if (settingsMap.containsKey("default")) {
      Map<String, Object> defaultMap = (Map<String, Object>) settingsMap.get("default");
      settings.setDefaultSettings(parsePackageSettings(defaultMap));
    }

    // Parse packages list
    if (settingsMap.containsKey("packages")) {
      Object packagesObj = settingsMap.get("packages");

      if (packagesObj instanceof List) {
        List<Map<String, Object>> packagesList = (List<Map<String, Object>>) packagesObj;

        for (Map<String, Object> packageMap : packagesList) {
          if (!packageMap.containsKey("id")) {
            throw new ProvisionSettingsException("Package configuration missing 'id' field");
          }

          String packageId = (String) packageMap.get("id");
          PackageProvisionSettings packageSettings = parsePackageSettings(packageMap);
          settings.addPackageSettings(packageId, packageSettings);
        }
      } else {
        throw new ProvisionSettingsException("'packages' must be a list");
      }
    }

    return settings;
  }

  /**
   * Parses a single package settings map.
   *
   * @param packageMap the package settings map from YAML
   * @return parsed package provision settings
   * @throws ProvisionSettingsException if the mode value is invalid
   */
  @SuppressWarnings("unchecked")
  private PackageProvisionSettings parsePackageSettings(Map<String, Object> packageMap)
      throws ProvisionSettingsException {
    PackageProvisionSettings packageSettings = new PackageProvisionSettings();

    // Parse version
    if (packageMap.containsKey("version")) {
      packageSettings.setVersion((String) packageMap.get("version"));
    }

    // Parse mode
    if (packageMap.containsKey("mode")) {
      String modeStr = ((String) packageMap.get("mode")).toUpperCase();
      try {
        packageSettings.setMode(ProvisionMode.valueOf(modeStr));
      } catch (IllegalArgumentException _) {
        throw new ProvisionSettingsException(
            "Invalid provision mode: " + modeStr + ". Must be 'supported' or 'predefined'");
      }
    }

    // Parse supported list
    if (packageMap.containsKey("supported")) {
      Object supportedObj = packageMap.get("supported");
      if (supportedObj instanceof List) {
        packageSettings.setSupported((List<String>) supportedObj);
      }
    }

    // Parse predefined list
    if (packageMap.containsKey("predefined")) {
      Object predefinedObj = packageMap.get("predefined");
      if (predefinedObj instanceof List) {
        packageSettings.setPredefined((List<String>) predefinedObj);
      }
    }

    return packageSettings;
  }
}
