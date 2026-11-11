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
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for package server configuration loaded from YAML sources. */
@DisplayName("ConfigManager package server yaml source")
class ConfigManagerPackageServerYamlSourceTest {

  private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @AfterEach
  void tearDown() {
    releaseRegisteredConfig();
  }

  @Nested
  @DisplayName("getConfiguration()")
  class GetConfigurationTests {

    @Test
    @DisplayName("should load package server config from yaml source")
    void shouldLoadPackageServerConfigFromYamlSource() throws IOException {
      // GIVEN
      String yamlContent =
          """
          fhirterms:
            start: true
            version: 1.0.0-yaml
            packageFolderLocation: /tmp/fhir-packages
            packageAutoDownloadEnabled: true
            packageServerConfig:
              servers:
                - name: YAML Registry
                  url: https://yaml.example.org
                  auth:
                    type: BEARER_TOKEN
                    token: yaml-token
                  endpoints:
                    packagePathPrefix: /packages
                    catalogPath: /packages/catalog
                  packages:
                    - yaml.one@1.0.0
                    - yaml.two@2.0.0
          """;
      Path yamlFile = Files.createTempFile("termzilla-fhir-config-", ".yaml");
      Files.writeString(yamlFile, yamlContent);
      yamlFile.toFile().deleteOnExit();

      ConfigSource yamlSource = new YamlConfigSource(yamlFile.toUri().toURL(), 290);
      registerConfigSources(yamlSource);

      // WHEN
      ConfigManager configManager = new ConfigManager();
      FhirConfigProperties config = configManager.getConfiguration();

      // THEN
      assertThat(config.isPackageAutoDownloadEnabled()).isTrue();
      assertThat(config.getPackageServers()).hasSize(1);
      assertThat(config.getPackageServers().getFirst().name()).isEqualTo("YAML Registry");
      assertThat(config.getPackageServers().getFirst().url()).isEqualTo("https://yaml.example.org");
      assertThat(config.getPackageServers().getFirst().auth().type())
          .isEqualTo(AuthenticationMode.BEARER_TOKEN);
      assertThat(config.getPackageServers().getFirst().auth().token()).isEqualTo("yaml-token");
      assertThat(config.getPackageServers().getFirst().endpoints().packagePathPrefix())
          .isEqualTo("/packages");
      assertThat(config.getPackageServers().getFirst().endpoints().catalogPath())
          .isEqualTo("/packages/catalog");
      assertThat(config.getPackageServers().getFirst().packages())
          .containsExactlyInAnyOrder("yaml.one@1.0.0", "yaml.two@2.0.0");
    }
  }

  private void registerConfigSources(ConfigSource... configSources) {
    releaseRegisteredConfig();
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
    builder.withSources(configSources);
    SmallRyeConfig config = builder.build();
    ConfigProviderResolver.instance().registerConfig(config, classLoader);
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
