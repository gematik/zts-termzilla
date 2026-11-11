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
package de.gematik.zts.termzilla.terminologies.fhirterms;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.common.manager.AbstractTerminologyManager;
import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloadException;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloader;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloaderAuthConfig;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloaderConfig;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloaderEndpointConfig;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.ConfigManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfigEntry;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link TerminologyManager} implementation
 * for FHIR package based terminologies.
 *
 * <p>Initializes FHIR package resources, optionally auto-downloads configured packages, and wires
 * CodeSystem, ValueSet, and ConceptMap services. Initialization is conditional on {@code
 * fhirterms.start}; when disabled, service accessors remain {@code null} via the shared base
 * manager state.
 */
@Slf4j
public class FhirTermsTerminologyManager extends AbstractTerminologyManager {

  private static final String TERMINOLOGY_MANAGER_NAME = "Basic FHIR Terminology Manager";

  private final FhirConfigProperties properties;

  @Getter private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates and initializes a new {@code FhirTermsTerminologyManager}.
   *
   * <p>If {@code configProperties} is {@code null}, configuration is loaded via {@link
   * ConfigManager}. FHIR package processing is started only when {@code fhirterms.start} is
   * enabled.
   *
   * @param baseWorkingDirectory the base directory for indexes and temporary files
   * @param defaultScope the default scope used while loading package resources
   * @param configProperties FHIR configuration, or {@code null} to load defaults
   * @throws InvalidConfigurationException if required configuration values are missing or invalid
   */
  public FhirTermsTerminologyManager(
      Path baseWorkingDirectory, String defaultScope, FhirConfigProperties configProperties)
      throws InvalidConfigurationException {

    // Initialize configuration handling
    ConfigManager configManager = new ConfigManager();

    // Configuration properties supplied directly
    if (configProperties != null) {
      this.properties = configProperties;
    } else {
      this.properties = configManager.getConfiguration();
    }

    if (this.properties == null) {
      throw new InvalidConfigurationException("FhirConfigProperties must not be null");
    }

    // Transfer base working directory to configuration
    if (baseWorkingDirectory == null) {
      throw new InvalidConfigurationException("BaseWorkingPath must not be null");
    } else {
      this.properties.setBaseWorkingDirectory(baseWorkingDirectory);
    }

    // TODO: Validate configuration

    // Validate default scope
    if (StringUtils.isEmpty(defaultScope)) {
      throw new InvalidConfigurationException("Default scope must not be empty");
    }

    // Initialize data model when FHIR terms support is enabled
    boolean shouldStart = properties.isFhirTermsStart();
    if (shouldStart) {
      downloadConfiguredPackagesIfEnabled();
      this.fhirTermsInMemoryRepresentation =
          new FhirTermsInMemoryRepresentation(properties, defaultScope);
    } else {
      this.fhirTermsInMemoryRepresentation = null;
    }

    // Initialize manager and create services in correct dependency order
    initializeManagerAndServices(TERMINOLOGY_MANAGER_NAME, shouldStart);
  }

  @Override
  protected CodeSystemSvcInterface createCodeSystemService() {
    return new FhirTermsCodeSystemService(this);
  }

  @Override
  protected ValueSetSvcInterface createValueSetService() {
    // Can now safely call this.cs() during ValueSet service construction
    return new FhirTermsValueSetService(this);
  }

  @Override
  protected ConceptMapSvcInterface createConceptMapService() {
    return new FhirTermsConceptMapService(this);
  }

  /**
   * Downloads configured packages when auto-download is enabled.
   *
   * @throws InvalidConfigurationException if mandatory package server settings are invalid
   */
  private void downloadConfiguredPackagesIfEnabled() throws InvalidConfigurationException {
    if (!properties.isPackageAutoDownloadEnabled()) {
      return;
    }

    if (StringUtils.isBlank(properties.getFhirTermsPackageFolderLocation())) {
      throw new InvalidConfigurationException(
          "fhirterms.packageFolderLocation must be configured when package auto-download is enabled");
    }

    Path targetDirectory = Path.of(properties.getFhirTermsPackageFolderLocation());

    List<PackageServerConfigEntry> configuredServers = properties.getPackageServers();
    if (configuredServers.isEmpty()) {
      throw new InvalidConfigurationException(
          "fhirterms.packageServerConfig.servers must be configured when package auto-download is enabled");
    }

    for (PackageServerConfigEntry server : configuredServers) {
      if (StringUtils.isBlank(server.url())) {
        throw new InvalidConfigurationException(
            "Each server in fhirterms.packageServerConfig.servers must define a non-empty url");
      }

      if (server.packages() == null || server.packages().isEmpty()) {
        log.warn(
            "Server '{}' ({}) is configured, but has no packages assigned",
            server.name(),
            server.url());
        continue;
      }

      downloadPackagesForServer(server, targetDirectory);
    }
  }

  /**
   * Downloads all configured packages for a single package server entry.
   *
   * @param server the configured package server entry
   * @param targetDirectory the target directory for downloaded package archives
   * @throws InvalidConfigurationException if package configuration is invalid or download fails
   */
  private void downloadPackagesForServer(PackageServerConfigEntry server, Path targetDirectory)
      throws InvalidConfigurationException {

    AuthenticationMode authenticationMode =
        server.auth() == null ? AuthenticationMode.NONE : server.auth().type();
    String basicAuthUsername = server.auth() == null ? null : server.auth().user();
    String basicAuthPassword = server.auth() == null ? null : server.auth().password();
    String bearerToken = server.auth() == null ? null : server.auth().token();
    String apiKey = server.auth() == null ? null : server.auth().apiKey();
    String apiKeyHeader = server.auth() == null ? null : server.auth().apiKeyHeader();
    String packagePathPrefix =
        server.endpoints() == null ? null : server.endpoints().packagePathPrefix();
    String catalogPath = server.endpoints() == null ? null : server.endpoints().catalogPath();

    String serverName = StringUtils.defaultIfBlank(server.name(), server.url());

    if (authenticationMode == AuthenticationMode.BFARM_AUTO_TOKEN) {
      log.warn(
          "Server '{}' is configured with BFARM_AUTO_TOKEN. By using this mode, protected package "
              + "downloads are performed with automatic token handling and acceptance of the BfArM "
              + "download conditions is assumed: https://terminologien.bfarm.de/download-conditions.html",
          serverName);
    }

    try (PackageDownloader packageDownloader =
        new PackageDownloader(
            120,
            server.url(),
            authenticationMode,
            new PackageDownloaderConfig(
                new PackageDownloaderAuthConfig(
                    basicAuthUsername, basicAuthPassword, bearerToken, apiKey, apiKeyHeader),
                new PackageDownloaderEndpointConfig(packagePathPrefix, catalogPath),
                server.skipDeprecatedUnlistedPackageVersions()))) {

      List<String> configuredPackageEntries =
          server.packages() == null ? List.of() : new ArrayList<>(server.packages());
      List<String> resolvedPackageEntries =
          resolveConfiguredPackageEntries(packageDownloader, configuredPackageEntries);

      for (String packageEntry : resolvedPackageEntries) {
        PackageNameVersion packageNameVersion = parsePackageNameVersionEntry(packageEntry);

        log.info(
            "Automatically downloading FHIR package {} from server '{}' ({}) using auth mode {}",
            packageEntry,
            serverName,
            server.url(),
            authenticationMode);
        packageDownloader.downloadPackage(
            packageNameVersion.packageName(), packageNameVersion.version(), targetDirectory);
      }
    } catch (PackageDownloadException | IllegalArgumentException e) {
      throw new InvalidConfigurationException(
          "Failed to auto-download configured FHIR packages from server '"
              + serverName
              + "': "
              + e.getMessage());
    }
  }

  /**
   * Resolves configured package entries, including wildcard expansion, and removes duplicates.
   *
   * @param packageDownloader downloader used for package list resolution
   * @param configuredPackageEntries configured package entries
   * @return normalized, de-duplicated package name/version entries
   * @throws PackageDownloadException if wildcard resolution fails
   */
  private List<String> resolveConfiguredPackageEntries(
      PackageDownloader packageDownloader, List<String> configuredPackageEntries)
      throws PackageDownloadException {
    if (configuredPackageEntries.isEmpty()) {
      return List.of();
    }

    List<String> resolvedPackageEntries =
        packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);

    LinkedHashSet<String> deduplicatedPackageEntries = new LinkedHashSet<>();
    for (String packageEntry : resolvedPackageEntries) {
      if (StringUtils.isNotBlank(packageEntry)) {
        deduplicatedPackageEntries.add(packageEntry.trim());
      }
    }

    return new ArrayList<>(deduplicatedPackageEntries);
  }

  /**
   * Parses a single package entry in the form {@code package.name@version}.
   *
   * @param packageEntry package entry to parse
   * @return parsed package name/version pair
   * @throws InvalidConfigurationException if the entry format is invalid
   */
  private PackageNameVersion parsePackageNameVersionEntry(String packageEntry)
      throws InvalidConfigurationException {
    if ("*".equals(StringUtils.trimToEmpty(packageEntry))) {
      throw new InvalidConfigurationException(
          "Wildcard package entry '*' must be resolved before parsing");
    }

    if (StringUtils.isBlank(packageEntry) || !packageEntry.contains("@")) {
      throw new InvalidConfigurationException(
          "Invalid package name/version entry '"
              + packageEntry
              + "'. Expected format: package.name@x.y.z or '*'");
    }

    int separatorIndex = packageEntry.lastIndexOf('@');
    String packageName = packageEntry.substring(0, separatorIndex).trim();
    String packageVersion = packageEntry.substring(separatorIndex + 1).trim();

    if (StringUtils.isBlank(packageName) || StringUtils.isBlank(packageVersion)) {
      throw new InvalidConfigurationException(
          "Invalid package name/version entry '"
              + packageEntry
              + "'. Expected format: package.name@x.y.z or '*'");
    }

    return new PackageNameVersion(packageName, packageVersion);
  }

  /** Immutable package identifier consisting of package name and version. */
  private record PackageNameVersion(String packageName, String version) {}

  @Override
  public Set<String> getSupportedScopes() {
    if (fhirTermsInMemoryRepresentation != null) {
      return fhirTermsInMemoryRepresentation.getScopeMap().keySet();
    }
    return Set.of();
  }

  @Override
  public String getConfigurationAndDataStateHash() {
    return buildConfigurationAndDataStateHash(
        properties.toString(), fhirTermsInMemoryRepresentation.getReleaseDataHash());
  }

  @Override
  public String toString() {
    return "FhirTermsTerminologyManager (" + TERMINOLOGY_MANAGER_NAME + ")";
  }
}
