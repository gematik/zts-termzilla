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
package de.gematik.zts.termzilla.common.packages.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for package downloader configuration records. */
@DisplayName("PackageDownloader configuration records")
class PackageDownloaderConfigurationTest {

  @Nested
  @DisplayName("PackageDownloaderAuthConfig")
  class AuthConfigTests {

    @Test
    @DisplayName("should create empty auth config via none()")
    void shouldCreateEmptyAuthConfigViaNone() {
      // GIVEN

      // WHEN
      PackageDownloaderAuthConfig authConfig = PackageDownloaderAuthConfig.none();

      // THEN
      assertThat(authConfig)
          .extracting(
              PackageDownloaderAuthConfig::basicAuthUsername,
              PackageDownloaderAuthConfig::basicAuthPassword,
              PackageDownloaderAuthConfig::bearerToken,
              PackageDownloaderAuthConfig::apiKey,
              PackageDownloaderAuthConfig::apiKeyHeader)
          .containsExactly(null, null, null, null, null);
    }
  }

  @Nested
  @DisplayName("PackageDownloaderEndpointConfig")
  class EndpointConfigTests {

    @Test
    @DisplayName("should create default endpoint config via defaults()")
    void shouldCreateDefaultEndpointConfigViaDefaults() {
      // GIVEN

      // WHEN
      PackageDownloaderEndpointConfig endpointConfig = PackageDownloaderEndpointConfig.defaults();

      // THEN
      assertThat(endpointConfig)
          .extracting(
              PackageDownloaderEndpointConfig::packagePathPrefix,
              PackageDownloaderEndpointConfig::packageCatalogPath)
          .containsExactly(null, null);
    }
  }

  @Nested
  @DisplayName("PackageDownloaderConfig")
  class DownloaderConfigTests {

    @Test
    @DisplayName("should create default downloader config via defaults()")
    void shouldCreateDefaultDownloaderConfigViaDefaults() {
      // GIVEN

      // WHEN
      PackageDownloaderConfig downloaderConfig = PackageDownloaderConfig.defaults();

      // THEN
      assertThat(downloaderConfig.authConfig()).isEqualTo(PackageDownloaderAuthConfig.none());
      assertThat(downloaderConfig.endpointConfig())
          .isEqualTo(PackageDownloaderEndpointConfig.defaults());
      assertThat(downloaderConfig.skipDeprecatedUnlistedPackageVersions()).isFalse();
    }
  }
}
