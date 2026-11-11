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
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfigDataHelper;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for package server configuration injection via {@link ConfigManager}. */
@DisplayName("ConfigManager package server configuration")
class ConfigManagerPackageServerConfigTest {

  private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @AfterEach
  void tearDown() {
    try {
      ConfigProviderResolver resolver = ConfigProviderResolver.instance();
      resolver.releaseConfig(resolver.getConfig(classLoader));
    } catch (IllegalStateException _) {
      // no registered config for this test
    }
  }

  @Nested
  @DisplayName("getConfiguration()")
  class GetConfigurationTests {

    @Test
    @DisplayName("should inject typed package server config from indexed properties")
    void shouldInjectTypedPackageServerConfigFromIndexedProperties() {
      // GIVEN
      Map<String, String> properties =
          new LinkedHashMap<>(PackageServerConfigDataHelper.twoServerProperties());
      properties.put("fhirterms.start", "true");
      properties.put("fhirterms.version", "1.0.0-test");
      properties.put("fhirterms.packageFolderLocation", "/tmp/packages");
      properties.put("fhirterms.packageAutoDownloadEnabled", "true");
      registerConfig(properties);

      // WHEN
      ConfigManager configManager = new ConfigManager();
      FhirConfigProperties config = configManager.getConfiguration();

      // THEN
      assertThat(config.isPackageAutoDownloadEnabled()).isTrue();
      assertThat(config.getPackageServers()).hasSize(2);
      assertThat(config.getPackageServers().get(0).auth().type())
          .isEqualTo(AuthenticationMode.BFARM_AUTO_TOKEN);
      assertThat(config.getPackageServers().get(1).auth().type())
          .isEqualTo(AuthenticationMode.BASIC_AUTH);
      assertThat(config.getPackageServers().get(1).auth().user()).isEqualTo("user");
      assertThat(config.getPackageServers().get(1).packages())
          .containsExactlyInAnyOrder("my.package.core@1.2.3");
      assertThat(config.getPackageServers().get(0).endpoints()).isNotNull();
      assertThat(config.getPackageServers().get(0).endpoints().packagePathPrefix())
          .isEqualTo("/packages");
      assertThat(config.getPackageServers().get(0).endpoints().catalogPath())
          .isEqualTo("/packages/catalog");
      assertThat(config.getPackageServers().get(0).skipDeprecatedUnlistedPackageVersions())
          .isTrue();
      assertThat(config.getPackageServers().get(1).endpoints()).isNotNull();
      assertThat(config.getPackageServers().get(1).endpoints().packagePathPrefix()).isEmpty();
      assertThat(config.getPackageServers().get(1).endpoints().catalogPath()).isEqualTo("/catalog");
      assertThat(config.getPackageServers().get(1).skipDeprecatedUnlistedPackageVersions())
          .isFalse();
    }
  }

  private void registerConfig(Map<String, String> properties) {
    try {
      ConfigProviderResolver resolver = ConfigProviderResolver.instance();
      resolver.releaseConfig(resolver.getConfig(classLoader));
    } catch (IllegalStateException _) {
      // no config registered yet
    }

    // Build an isolated config to avoid environment/system property interference.
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
    properties.forEach(builder::withDefaultValue);
    SmallRyeConfig config = builder.build();
    ConfigProviderResolver.instance().registerConfig(config, classLoader);
  }
}
