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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfigEntry;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/** Unit tests for package server configuration via environment variables. */
@DisplayName("ConfigManager package server env vars")
class ConfigManagerPackageServerEnvVarTest {

  private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @AfterEach
  void tearDown() {
    releaseRegisteredConfig();
  }

  @Nested
  @DisplayName("getConfiguration()")
  class GetConfigurationTests {

    @Test
    @DisplayName("should inject package server settings from environment variables")
    void shouldInjectPackageServerSettingsFromEnvironmentVariables() throws Exception {
      // GIVEN
      EnvironmentVariables environmentVariables =
          new EnvironmentVariables()
              .set("FHIRTERMS_START", "true")
              .set("FHIRTERMS_VERSION", "1.0.0-env")
              .set("FHIRTERMS_PACKAGEFOLDERLOCATION", "/tmp/fhir-packages")
              .set("FHIRTERMS_PACKAGEAUTODOWNLOADENABLED", "true")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__NAME", "Env BfArM")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__URL", "https://terminologien.bfarm.de")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__AUTH_TYPE", "BFARM_AUTO_TOKEN")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_PACKAGEPATHPREFIX",
                  "/packages")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_CATALOGPATH",
                  "/packages/catalog")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__SKIPDEPRECATEDUNLISTEDPACKAGEVERSIONS",
                  "true")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_0",
                  "bfarm.terminologien.icd10gm@2024.0.0")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_1",
                  "bfarm.terminologien.ops@2024.0.0")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__NAME", "Env FHIR Packages")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__URL", "https://packages.fhir.org")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__AUTH_TYPE", "NONE")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__ENDPOINTS_PACKAGEPATHPREFIX", "")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__ENDPOINTS_CATALOGPATH", "/catalog")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_1__PACKAGES_0", "hl7.fhir.r4.core@4.0.1");

      // WHEN
      environmentVariables.execute(
          () -> {
            releaseRegisteredConfig();
            ConfigManager configManager = new ConfigManager();
            FhirConfigProperties config = configManager.getConfiguration();

            // THEN
            assertThat(config.isPackageAutoDownloadEnabled()).isTrue();
            assertThat(config.getPackageServers()).hasSize(2);

            PackageServerConfigEntry
                firstServer = config.getPackageServers().getFirst();
            PackageServerConfigEntry
                secondServer = config.getPackageServers().get(1);

            assertThat(firstServer.name()).isEqualTo("Env BfArM");
            assertThat(firstServer.url()).isEqualTo("https://terminologien.bfarm.de");
            assertThat(firstServer.auth().type()).isEqualTo(AuthenticationMode.BFARM_AUTO_TOKEN);
            assertThat(firstServer.endpoints().packagePathPrefix()).isEqualTo("/packages");
            assertThat(firstServer.endpoints().catalogPath()).isEqualTo("/packages/catalog");
            assertThat(firstServer.skipDeprecatedUnlistedPackageVersions()).isTrue();
            assertThat(firstServer.packages())
                .containsExactlyInAnyOrder(
                    "bfarm.terminologien.icd10gm@2024.0.0", "bfarm.terminologien.ops@2024.0.0");

            assertThat(secondServer.name()).isEqualTo("Env FHIR Packages");
            assertThat(secondServer.url()).isEqualTo("https://packages.fhir.org");
            assertThat(secondServer.auth().type()).isEqualTo(AuthenticationMode.NONE);
            assertThat(secondServer.endpoints().packagePathPrefix()).isEmpty();
            assertThat(secondServer.endpoints().catalogPath()).isEqualTo("/catalog");
            assertThat(secondServer.skipDeprecatedUnlistedPackageVersions()).isFalse();
            assertThat(secondServer.packages()).containsExactlyInAnyOrder("hl7.fhir.r4.core@4.0.1");
          });
    }

    @Test
    @DisplayName(
        "should replace package list from lower-priority sources when env packages are set")
    void shouldReplacePackageListFromLowerPrioritySourcesWhenEnvPackagesAreSet() throws Exception {
      // GIVEN
      EnvironmentVariables environmentVariables =
          new EnvironmentVariables()
              .set("FHIRTERMS_START", "true")
              .set("FHIRTERMS_VERSION", "1.0.0-env")
              .set("FHIRTERMS_PACKAGEFOLDERLOCATION", "/tmp/fhir-packages")
              .set("FHIRTERMS_PACKAGEAUTODOWNLOADENABLED", "true")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__NAME", "Env BfArM")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__URL", "https://terminologien.bfarm.de")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__AUTH_TYPE", "BFARM_AUTO_TOKEN")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_PACKAGEPATHPREFIX",
                  "/packages")
              .set(
                  "FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__ENDPOINTS_CATALOGPATH",
                  "/packages/catalog")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_0", "env.package.only@1.0.0");

      // WHEN
      environmentVariables.execute(
          () -> {
            releaseRegisteredConfig();
            ConfigManager configManager = new ConfigManager();
            FhirConfigProperties config = configManager.getConfiguration();

            // THEN
            assertThat(config.getPackageServers()).isNotEmpty();
            assertThat(config.getPackageServers().getFirst().packages())
                .containsExactlyInAnyOrder("env.package.only@1.0.0");
          });
    }

    @Test
    @DisplayName("should ignore malformed package env var keys")
    void shouldIgnoreMalformedPackageEnvVarKeys() throws Exception {
      // GIVEN
      EnvironmentVariables environmentVariables =
          new EnvironmentVariables()
              .set("FHIRTERMS_START", "true")
              .set("FHIRTERMS_VERSION", "1.0.0-env")
              .set("FHIRTERMS_PACKAGEFOLDERLOCATION", "/tmp/fhir-packages")
              .set("FHIRTERMS_PACKAGEAUTODOWNLOADENABLED", "true")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__NAME", "Env BfArM")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__URL", "https://terminologien.bfarm.de")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__AUTH_TYPE", "NONE")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_0", "valid.package@1.0.0")
              .set("FHIRTERMS_PACKAGESERVERCONFIG_SERVERS_0__PACKAGES_A", "ignored.package@9.9.9");

      // WHEN
      environmentVariables.execute(
          () -> {
            releaseRegisteredConfig();
            ConfigManager configManager = new ConfigManager();
            FhirConfigProperties config = configManager.getConfiguration();

            // THEN
            assertThat(config.getPackageServers()).isNotEmpty();
            assertThat(config.getPackageServers().getFirst().packages())
                .containsExactlyInAnyOrder("valid.package@1.0.0");
          });
    }
  }

  private void releaseRegisteredConfig() {
    try {
      ConfigProviderResolver resolver = ConfigProviderResolver.instance();
      resolver.releaseConfig(resolver.getConfig(classLoader));
    } catch (IllegalStateException _) {
      // no registered config for this test
    }
  }
}
