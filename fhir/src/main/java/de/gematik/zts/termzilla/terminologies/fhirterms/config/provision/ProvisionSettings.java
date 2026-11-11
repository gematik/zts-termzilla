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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Root configuration for package provision settings.
 *
 * <p>Manages default settings and package-specific configurations loaded from external YAML files.
 * Supports multiple version-specific configurations per package.
 */
@Getter
@Setter
@ToString
public class ProvisionSettings {

  /** Default settings applied to packages without specific configuration. */
  private PackageProvisionSettings defaultSettings;

  /**
   * Package-specific provision settings.
   *
   * <p>Each package can have multiple configurations for different version ranges. Key: package ID,
   * Value: list of version-specific settings.
   */
  private Map<String, List<PackageProvisionSettings>> packages = new HashMap<>();

  /**
   * Adds a package configuration to the settings.
   *
   * @param packageId the package identifier (e.g., {@code "de.basisprofil"})
   * @param settings the provision settings for this package
   */
  public void addPackageSettings(String packageId, PackageProvisionSettings settings) {
    packages.computeIfAbsent(packageId, k -> new ArrayList<>()).add(settings);
  }

  /**
   * Retrieves provision settings for a specific package and version.
   *
   * <p>Searches through all configurations for the package to find the first matching version.
   * Falls back to {@link #defaultSettings} if no match is found.
   *
   * @param packageId the package identifier (e.g., {@code "de.basisprofil"})
   * @param packageVersion the package version (e.g., {@code "1.5.0"})
   * @return the provision settings for this package version, or default settings if no match is
   *     found
   */
  public PackageProvisionSettings getSettingsForPackage(String packageId, String packageVersion) {
    List<PackageProvisionSettings> settingsList = packages.get(packageId);

    if (settingsList != null) {
      for (PackageProvisionSettings settings : settingsList) {
        if (settings.matchesVersion(packageVersion)) {
          return settings;
        }
      }
    }

    return defaultSettings;
  }

  /**
   * Checks if a specific package has custom settings configured.
   *
   * @param packageId the package identifier
   * @return {@code true} if custom settings exist, {@code false} otherwise
   */
  public boolean hasPackageSettings(String packageId) {
    return packages.containsKey(packageId);
  }
}
