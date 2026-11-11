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
package de.gematik.zts.termzilla.terminologies.fhirterms.config;

import de.gematik.zts.termzilla.common.config.Value;
import de.gematik.zts.termzilla.common.config.provision.ProvisionAware;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfig;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfigEntry;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.provision.ProvisionSettings;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.provision.ProvisionSettingsException;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.provision.ProvisionSettingsLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@Slf4j
@EqualsAndHashCode
public class FhirConfigProperties implements ProvisionAware {

  private static final String INDEX_DIR = "fhir_index";
  private static final String UNZIP_DIR = "fhir_unzip";

  private Path baseWorkingDirectory;

  @Value("${fhirterms.start:false}")
  private boolean fhirTermsStart;

  // TODO: not used anymore
  @Value("${fhirterms.loadDependencies:false}")
  private boolean fhirTermsLoadDependencies;

  @Value("${fhirterms.packageFolderLocation}")
  private String fhirTermsPackageFolderLocation;

  @Value("${fhirterms.additionalResourcesFolderLocation:}")
  private String additionalResourcesFolderLocation;

  @Value("${fhirterms.enrichmentFolderLocation:}")
  private String enrichmentFolderLocation;

  @Value("${fhirterms.packageAutoDownloadEnabled:false}")
  private boolean packageAutoDownloadEnabled;

  /** Typed package server configuration loaded via a custom converter. */
  @Value("${fhirterms.packageServerConfig:__AUTO__}")
  private PackageServerConfig packageServerConfig;

  @Value("${fhirterms.provisionMode:PREDEFINED}")
  private ProvisionMode provisionMode;

  @Value("${fhirterms.provisionSettingsPath:}")
  private Path provisionSettingsPath;

  private ProvisionSettings provisionSettings;

  @Value("${fhirterms.version}")
  private String version;

  /**
   * Returns the provision settings, loading them lazily if needed.
   *
   * <p>Settings are only loaded if {@link #provisionSettingsPath} is configured. Returns {@code
   * null} if no path is specified.
   *
   * @return the provision settings, or {@code null} if not configured
   * @throws RuntimeException if settings path is configured but loading fails
   */
  public ProvisionSettings getProvisionSettings() {
    if (provisionSettings == null && hasProvisionSettings()) {
      loadProvisionSettings();
    }
    return provisionSettings;
  }

  /**
   * Checks if provision settings are configured.
   *
   * @return {@code true} if a provision settings path is specified
   */
  public boolean hasProvisionSettings() {
    return provisionSettingsPath != null;
  }

  /**
   * Loads provision settings from the configured file.
   *
   * @throws RuntimeException if loading fails
   */
  private void loadProvisionSettings() {
    try {
      ProvisionSettingsLoader loader = new ProvisionSettingsLoader();
      provisionSettings = loader.load(provisionSettingsPath);

      log.info("Loaded provision settings from: {}", provisionSettingsPath);

    } catch (IOException e) {
      throw new ProvisionSettingsException(
          "Failed to load provision settings from: " + provisionSettingsPath, e);
    } catch (ProvisionSettingsException e) {
      // Add context and rethrow
      throw new ProvisionSettingsException(
          "Invalid provision settings in: " + provisionSettingsPath, e);
    }
  }

  public Path getFhirTermsIndex() {
    return baseWorkingDirectory.resolve(INDEX_DIR);
  }

  public Path getFhirTermsUnzipLocation() {
    return baseWorkingDirectory.resolve(UNZIP_DIR);
  }

  /**
   * Returns configured package servers.
   *
   * @return configured servers or an empty list
   */
  public List<PackageServerConfigEntry> getPackageServers() {
    if (packageServerConfig == null) {
      return List.of();
    }
    return packageServerConfig.getServersOrEmpty();
  }
}
