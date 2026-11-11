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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link PackageDownloader}. */
@DisplayName("PackageDownloader")
class PackageDownloaderTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should create downloader with default constructor")
    void shouldCreateDownloaderWithDefaultConstructor() {
      // GIVEN

      // WHEN THEN
      assertThatCode(
              () -> {
                PackageDownloader packageDownloader = new PackageDownloader();
                packageDownloader.close();
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should create downloader with custom timeout")
    void shouldCreateDownloaderWithCustomTimeout() {
      // GIVEN
      int timeoutSeconds = 5;

      // WHEN THEN
      assertThatCode(
              () -> {
                PackageDownloader packageDownloader = new PackageDownloader(timeoutSeconds);
                packageDownloader.close();
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should reject empty base url")
    void shouldRejectEmptyBaseUrl() {
      // GIVEN

      // WHEN THEN
      assertThatThrownBy(PackageDownloaderTest.this::createDownloaderWithEmptyBaseUrlAndClose)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Registry base URL cannot be empty");
    }

    @Test
    @DisplayName("should reject basic auth mode when username is missing")
    void shouldRejectBasicAuthModeWhenUsernameIsMissing() {
      // GIVEN

      // WHEN THEN
      assertThatThrownBy(
              PackageDownloaderTest.this::createDownloaderWithMissingBasicAuthUsernameAndClose)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("basicAuthUsername must be configured");
    }

    @Test
    @DisplayName("should reject bearer token mode when token is missing")
    void shouldRejectBearerTokenModeWhenTokenIsMissing() {
      // GIVEN

      // WHEN THEN
      assertThatThrownBy(PackageDownloaderTest.this::createDownloaderWithMissingBearerTokenAndClose)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("bearerToken must be configured");
    }

    @Test
    @DisplayName("should reject api key mode when api key is missing")
    void shouldRejectApiKeyModeWhenApiKeyIsMissing() {
      // GIVEN

      // WHEN THEN
      assertThatThrownBy(PackageDownloaderTest.this::createDownloaderWithMissingApiKeyAndClose)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("apiKey must be configured");
    }

    @Test
    @DisplayName("should create downloader with combined config constructor")
    void shouldCreateDownloaderWithCombinedConfigConstructor() {
      // GIVEN
      PackageDownloaderConfig downloaderConfig =
          new PackageDownloaderConfig(
              new PackageDownloaderAuthConfig("user", "password", null, null, null),
              new PackageDownloaderEndpointConfig("/packages", "/packages/catalog"));

      // WHEN THEN
      assertThatCode(
              () -> {
                PackageDownloader packageDownloader =
                    new PackageDownloader(
                        5, "https://example.org", AuthenticationMode.BASIC_AUTH, downloaderConfig);
                packageDownloader.close();
              })
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("downloadPackage(String, String, Path)")
  class DownloadPackagePathTests {

    @ParameterizedTest
    @MethodSource(
        "de.gematik.zts.termzilla.common.packages.downloader.PackageDownloaderTestDataHelper#invalidPackageNames")
    @DisplayName("should reject invalid package names")
    void shouldRejectInvalidPackageNames(String packageName, @TempDir Path tempDir) {
      // GIVEN
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();

      // WHEN THEN
      try (PackageDownloader packageDownloader = new PackageDownloader()) {
        assertThatThrownBy(
                () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
            .isInstanceOf(PackageDownloadException.class);
      }
    }

    @ParameterizedTest
    @MethodSource(
        "de.gematik.zts.termzilla.common.packages.downloader.PackageDownloaderTestDataHelper#invalidPackageVersions")
    @DisplayName("should reject invalid package versions")
    void shouldRejectInvalidPackageVersions(String packageVersion, @TempDir Path tempDir) {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();

      // WHEN THEN
      try (PackageDownloader packageDownloader = new PackageDownloader()) {
        assertThatThrownBy(
                () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
            .isInstanceOf(PackageDownloadException.class);
      }
    }

    @Test
    @DisplayName("should reject target path that is not a directory")
    void shouldRejectTargetPathThatIsNotDirectory(@TempDir Path tempDir) throws Exception {
      // GIVEN
      Path filePath = tempDir.resolve("not-a-directory.txt");
      Files.writeString(filePath, "content");
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();

      // WHEN THEN
      try (PackageDownloader packageDownloader = new PackageDownloader()) {
        assertThatThrownBy(
                () -> packageDownloader.downloadPackage(packageName, packageVersion, filePath))
            .isInstanceOf(PackageDownloadException.class)
            .hasMessageContaining("not a directory");
      }
    }

    @Test
    @DisplayName("should generate token before download when BFARM package version is protected")
    void shouldGenerateTokenBeforeDownloadWhenBfarmPackageVersionIsProtected(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, true)) {

        // WHEN
        Path downloadedFile;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10, server.getBaseUrl(), AuthenticationMode.BFARM_AUTO_TOKEN, null, null)) {
          downloadedFile = packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(Files.readString(downloadedFile))
            .as("protected package should be downloaded using generated token")
            .isEqualTo(StubRegistryServer.PACKAGE_CONTENT);
        assertThat(server.getMetadataRequests()).isEqualTo(1);
        assertThat(server.getTokenRequests()).isEqualTo(1);
        assertThat(server.getDownloadAuthorizationHeader()).isEqualTo("Bearer test-token");
      }
    }

    @Test
    @DisplayName("should skip token generation when BFARM package version is not protected")
    void shouldSkipTokenGenerationWhenBfarmPackageVersionIsNotProtected(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        Path downloadedFile;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10, server.getBaseUrl(), AuthenticationMode.BFARM_AUTO_TOKEN, null, null)) {
          downloadedFile = packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(Files.readString(downloadedFile))
            .as("unprotected package should be downloaded without requesting token")
            .isEqualTo(StubRegistryServer.PACKAGE_CONTENT);
        assertThat(server.getMetadataRequests()).isEqualTo(1);
        assertThat(server.getTokenRequests()).isZero();
        assertThat(server.getDownloadAuthorizationHeader()).isNull();
      }
    }

    @Test
    @DisplayName("should fail when metadata does not contain requested version")
    void shouldFailWhenMetadataDoesNotContainRequestedVersion(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forMetadataWithoutRequestedVersion(packageName, packageVersion)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10, server.getBaseUrl(), AuthenticationMode.BFARM_AUTO_TOKEN, null, null)) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("does not provide information for version");
        }
      }
    }

    @Test
    @DisplayName("should skip download when target file checksum already matches metadata")
    void shouldSkipDownloadWhenTargetFileChecksumAlreadyMatchesMetadata(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      Path targetFile = tempDir.resolve(packageName + "-" + packageVersion + ".tgz");
      Files.writeString(targetFile, StubRegistryServer.PACKAGE_CONTENT);

      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        Path downloadedFile;
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          downloadedFile = packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(downloadedFile).isEqualTo(targetFile);
        assertThat(Files.readString(targetFile)).isEqualTo(StubRegistryServer.PACKAGE_CONTENT);
        assertThat(server.getDownloadRequests()).isZero();
      }
    }

    @Test
    @DisplayName("should re-download package when existing file checksum mismatches metadata")
    void shouldRedownloadPackageWhenExistingFileChecksumMismatchesMetadata(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      Path targetFile = tempDir.resolve(packageName + "-" + packageVersion + ".tgz");
      Files.writeString(targetFile, "corrupted-content");

      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        Path downloadedFile;
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          downloadedFile = packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(downloadedFile).isEqualTo(targetFile);
        assertThat(Files.readString(targetFile)).isEqualTo(StubRegistryServer.PACKAGE_CONTENT);
        assertThat(server.getDownloadRequests()).isEqualTo(1);
      }
    }

    @Test
    @DisplayName("should fail and remove target file when downloaded checksum mismatches metadata")
    void shouldFailAndRemoveTargetFileWhenDownloadedChecksumMismatchesMetadata(
        @TempDir Path tempDir) throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      Path targetFile = tempDir.resolve(packageName + "-" + packageVersion + ".tgz");

      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackageWithMetadataShaSum(
              packageName,
              packageVersion,
              false,
              PackageDownloaderTestDataHelper.differentContentShaSum())) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Checksum verification failed");
        }

        // THEN
        assertThat(Files.exists(targetFile)).isFalse();
      }
    }
  }

  @Nested
  @DisplayName("resolvePackageNameVersionEntries(List<String>)")
  class ResolvePackageNameVersionEntriesTests {

    @Test
    @DisplayName("should return configured package entries unchanged when wildcard is not used")
    void shouldReturnConfiguredPackageEntriesUnchangedWhenWildcardIsNotUsed() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries =
          List.of("bfarm.terminologien.icd10gm@2024.0.0", "bfarm.terminologien.loinc@2.81.0");
      try (StubRegistryServer server = StubRegistryServer.forCatalogOnly()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries).containsExactlyElementsOf(configuredPackageEntries);
        assertThat(server.getCatalogRequests()).isZero();
      }
    }

    @Test
    @DisplayName("should expand wildcard to all package entries from catalog")
    void shouldExpandWildcardToAllPackageEntriesFromCatalog() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");
      try (StubRegistryServer server = StubRegistryServer.forCatalogOnly()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries)
            .containsExactly(
                "bfarm.terminologien.icd10gm@2024.0.0",
                "bfarm.terminologien.icd10gm@2025.0.0",
                "bfarm.terminologien.loinc@2.81.0");
        assertThat(server.getCatalogRequests()).isEqualTo(1);
      }
    }

    @Test
    @DisplayName(
        "should merge explicit package entries with catalog entries when wildcard is present")
    void shouldMergeExplicitPackageEntriesWithCatalogEntriesWhenWildcardIsPresent()
        throws Exception {
      // GIVEN
      List<String> configuredPackageEntries =
          List.of("bfarm.terminologien.icd10gm@2024.0.0", "*", "bfarm.terminologien.custom@1.0.0");
      try (StubRegistryServer server = StubRegistryServer.forCatalogOnly()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries)
            .containsExactly(
                "bfarm.terminologien.icd10gm@2024.0.0",
                "bfarm.terminologien.custom@1.0.0",
                "bfarm.terminologien.icd10gm@2025.0.0",
                "bfarm.terminologien.loinc@2.81.0");
        assertThat(server.getCatalogRequests()).isEqualTo(1);
      }
    }

    @Test
    @DisplayName("should throw NullPointerException when configured package entries are null")
    void shouldThrowNullPointerExceptionWhenConfiguredPackageEntriesAreNull() throws Exception {
      // GIVEN
      try (StubRegistryServer server = StubRegistryServer.forCatalogOnly()) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(() -> packageDownloader.resolvePackageNameVersionEntries(null))
              .isInstanceOf(NullPointerException.class)
              .hasMessageContaining("configuredPackageEntries cannot be null");
        }
      }
    }

    @Test
    @DisplayName("should fail when catalog response is not an array")
    void shouldFailWhenCatalogResponseIsNotAnArray() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");
      try (StubRegistryServer server = StubRegistryServer.forCatalogWithInvalidShape()) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () ->
                      packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Package catalog response is not an array");
        }
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "https://packages.simplifier.net",
          "http://packages.fhir.org/",
          "http://packages2.fhir.org/"
        })
    @DisplayName("should reject wildcard expansion for blocked registry base urls")
    void shouldRejectWildcardExpansionForBlockedRegistryBaseUrls(String blockedBaseUrl) {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");

      // WHEN THEN
      try (PackageDownloader packageDownloader = new PackageDownloader(blockedBaseUrl)) {
        assertThatThrownBy(
                () -> packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries))
            .isInstanceOf(PackageDownloadException.class)
            .hasMessageContaining("Catalog lookup and wildcard '*' are not supported")
            .hasMessageContaining("Configure explicit package entries");
      }
    }

    @Test
    @DisplayName("should skip deprecated and unlisted package versions when configured")
    void shouldSkipDeprecatedAndUnlistedPackageVersionsWhenConfigured() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");
      try (StubRegistryServer server = StubRegistryServer.forCatalogWithDeprecatedAndUnlisted()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog"),
                    true))) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries).containsExactly("bfarm.terminologien.icd10gm@2024.0.0");
        assertThat(server.getCatalogRequests()).isEqualTo(1);
        assertThat(server.getMetadataRequests()).isEqualTo(2);
      }
    }

    @Test
    @DisplayName(
        "should include deprecated and unlisted package versions when skipping is disabled")
    void shouldIncludeDeprecatedAndUnlistedPackageVersionsWhenSkippingIsDisabled()
        throws Exception {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");
      try (StubRegistryServer server = StubRegistryServer.forCatalogWithDeprecatedAndUnlisted()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog"),
                    false))) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries)
            .containsExactly(
                "bfarm.terminologien.icd10gm@2024.0.0",
                "bfarm.terminologien.icd10gm@2025.0.0",
                "bfarm.terminologien.loinc@2.81.0");
        assertThat(server.getCatalogRequests()).isEqualTo(1);
        assertThat(server.getMetadataRequests()).isZero();
      }
    }

    @Test
    @DisplayName("should skip deprecated and unlisted explicit package entries when configured")
    void shouldSkipDeprecatedAndUnlistedExplicitPackageEntriesWhenConfigured() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries =
          List.of(
              "bfarm.terminologien.icd10gm@2025.0.0",
              "bfarm.terminologien.icd10gm@2024.0.0",
              "bfarm.terminologien.loinc@2.81.0");
      try (StubRegistryServer server = StubRegistryServer.forCatalogWithDeprecatedAndUnlisted()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog"),
                    true))) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries).containsExactly("bfarm.terminologien.icd10gm@2024.0.0");
        assertThat(server.getCatalogRequests()).isZero();
        assertThat(server.getMetadataRequests()).isEqualTo(2);
      }
    }

    @Test
    @DisplayName(
        "should keep deprecated and unlisted explicit package entries when skipping is disabled")
    void shouldKeepDeprecatedAndUnlistedExplicitPackageEntriesWhenSkippingIsDisabled()
        throws Exception {
      // GIVEN
      List<String> configuredPackageEntries =
          List.of(
              "bfarm.terminologien.icd10gm@2025.0.0",
              "bfarm.terminologien.icd10gm@2024.0.0",
              "bfarm.terminologien.loinc@2.81.0");
      try (StubRegistryServer server = StubRegistryServer.forCatalogWithDeprecatedAndUnlisted()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog"),
                    false))) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries).containsExactlyElementsOf(configuredPackageEntries);
        assertThat(server.getCatalogRequests()).isZero();
        assertThat(server.getMetadataRequests()).isZero();
      }
    }
  }

  @Nested
  @DisplayName("downloadPackage(String, String, String)")
  class DownloadPackageStringTargetTests {

    @Test
    @DisplayName("should reject null package name before any network call")
    void shouldRejectNullPackageNameBeforeAnyNetworkCall(@TempDir Path tempDir) {
      // GIVEN
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      String targetDirectory = tempDir.toString();

      // WHEN THEN
      try (PackageDownloader packageDownloader = new PackageDownloader()) {
        assertThatThrownBy(
                () -> packageDownloader.downloadPackage(null, packageVersion, targetDirectory))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Package name cannot be null");
      }
    }
  }

  @Nested
  @DisplayName("close()")
  class CloseTests {

    @Test
    @DisplayName("should close downloader without throwing")
    void shouldCloseDownloaderWithoutThrowing() {
      // GIVEN

      // WHEN THEN
      assertThatCode(
              () -> {
                try (PackageDownloader packageDownloader = new PackageDownloader()) {
                  assertThat(packageDownloader).isNotNull();
                }
              })
          .doesNotThrowAnyException();
    }
  }

  private void createDownloaderWithEmptyBaseUrlAndClose() {
    PackageDownloader packageDownloader = new PackageDownloader("   ");
    packageDownloader.close();
  }

  private void createDownloaderWithMissingBasicAuthUsernameAndClose() {
    PackageDownloader packageDownloader =
        new PackageDownloader(
            10, "https://example.org", AuthenticationMode.BASIC_AUTH, null, "secret");
    packageDownloader.close();
  }

  private void createDownloaderWithMissingBearerTokenAndClose() {
    PackageDownloader packageDownloader =
        new PackageDownloader(
            10,
            "https://example.org",
            AuthenticationMode.BEARER_TOKEN,
            new PackageDownloaderAuthConfig(null, null, null, null, null));
    packageDownloader.close();
  }

  private void createDownloaderWithMissingApiKeyAndClose() {
    PackageDownloader packageDownloader =
        new PackageDownloader(
            10,
            "https://example.org",
            AuthenticationMode.API_KEY,
            new PackageDownloaderAuthConfig(null, null, null, null, "X-API-Key"));
    packageDownloader.close();
  }

  @Nested
  @DisplayName("registry endpoint layout fallback")
  class RegistryEndpointLayoutFallbackTests {

    @Test
    @DisplayName("should download package when registry uses root path layout")
    void shouldDownloadPackageWhenRegistryUsesRootPathLayout(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackageRootLayout(packageName, packageVersion, false)) {

        // WHEN
        Path downloadedFile;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("", "/catalog")))) {
          downloadedFile = packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(Files.readString(downloadedFile)).isEqualTo(StubRegistryServer.PACKAGE_CONTENT);
        assertThat(server.getMetadataRequests()).isEqualTo(1);
        assertThat(server.getTokenRequests()).isZero();
        assertThat(server.getDownloadAuthorizationHeader()).isNull();
      }
    }

    @Test
    @DisplayName("should resolve package entries when registry uses root catalog path")
    void shouldResolvePackageEntriesWhenRegistryUsesRootCatalogPath() throws Exception {
      // GIVEN
      List<String> configuredPackageEntries = List.of("*");
      try (StubRegistryServer server = StubRegistryServer.forCatalogOnlyRootLayout()) {

        // WHEN
        List<String> resolvedPackageEntries;
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.NONE,
                new PackageDownloaderConfig(
                    PackageDownloaderAuthConfig.none(),
                    new PackageDownloaderEndpointConfig("", "/catalog")))) {
          resolvedPackageEntries =
              packageDownloader.resolvePackageNameVersionEntries(configuredPackageEntries);
        }

        // THEN
        assertThat(resolvedPackageEntries)
            .containsExactly(
                "bfarm.terminologien.icd10gm@2024.0.0",
                "bfarm.terminologien.icd10gm@2025.0.0",
                "bfarm.terminologien.loinc@2.81.0");
        assertThat(server.getCatalogRequests()).isEqualTo(1);
      }
    }
  }

  @Nested
  @DisplayName("authentication headers")
  class AuthenticationHeaderTests {

    @Test
    @DisplayName("should send bearer token header for BEARER_TOKEN mode")
    void shouldSendBearerTokenHeaderForBearerTokenMode(@TempDir Path tempDir) throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.BEARER_TOKEN,
                new PackageDownloaderConfig(
                    new PackageDownloaderAuthConfig(null, null, "test-bearer-token", null, null),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog")))) {
          packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(server.getDownloadAuthorizationHeader()).isEqualTo("Bearer test-bearer-token");
      }
    }

    @Test
    @DisplayName("should send basic auth header for BASIC_AUTH mode")
    void shouldSendBasicAuthHeaderForBasicAuthMode(@TempDir Path tempDir) throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.BASIC_AUTH,
                "test-user",
                "test-password")) {
          packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(server.getDownloadAuthorizationHeader())
            .isEqualTo("Basic dGVzdC11c2VyOnRlc3QtcGFzc3dvcmQ=");
      }
    }

    @Test
    @DisplayName("should send api key using default header when header is not configured")
    void shouldSendApiKeyUsingDefaultHeaderWhenHeaderIsNotConfigured(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forSinglePackage(packageName, packageVersion, false)) {

        // WHEN
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10,
                server.getBaseUrl(),
                AuthenticationMode.API_KEY,
                new PackageDownloaderConfig(
                    new PackageDownloaderAuthConfig(null, null, null, "test-api-key", null),
                    new PackageDownloaderEndpointConfig("/packages", "/packages/catalog")))) {
          packageDownloader.downloadPackage(packageName, packageVersion, tempDir);
        }

        // THEN
        assertThat(server.getDownloadApiKeyHeader()).isEqualTo("test-api-key");
      }
    }
  }

  @Nested
  @DisplayName("unsuccessful responses")
  class UnsuccessfulResponsesTests {

    @Test
    @DisplayName("should fail with package not found message when server returns 404")
    void shouldFailWithPackageNotFoundMessageWhenServerReturns404(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forDownloadWithStatus(packageName, packageVersion, 404)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Package not found")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(404));
        }
      }
    }

    @Test
    @DisplayName("should fail with authentication message when server returns 401")
    void shouldFailWithAuthenticationMessageWhenServerReturns401(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forDownloadWithStatus(packageName, packageVersion, 401)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Authentication required or access denied")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(401));
        }
      }
    }

    @Test
    @DisplayName("should fail with authentication message when server returns 403")
    void shouldFailWithAuthenticationMessageWhenServerReturns403(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forDownloadWithStatus(packageName, packageVersion, 403)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Authentication required or access denied")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(403));
        }
      }
    }

    @Test
    @DisplayName("should fail with generic message when server returns 500")
    void shouldFailWithGenericMessageWhenServerReturns500(@TempDir Path tempDir) throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server =
          StubRegistryServer.forDownloadWithStatus(packageName, packageVersion, 500)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Failed to download package. Status: 500")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(500));
        }
      }
    }

    @Test
    @DisplayName("should fail when metadata lookup returns non-success response")
    void shouldFailWhenMetadataLookupReturnsNonSuccessResponse(@TempDir Path tempDir)
        throws Exception {
      // GIVEN
      String packageName = PackageDownloaderTestDataHelper.validPackageName();
      String packageVersion = PackageDownloaderTestDataHelper.validPackageVersion();
      try (StubRegistryServer server = StubRegistryServer.forMetadataStatus(packageName, 500)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader =
            new PackageDownloader(
                10, server.getBaseUrl(), AuthenticationMode.BFARM_AUTO_TOKEN, null, null)) {
          assertThatThrownBy(
                  () -> packageDownloader.downloadPackage(packageName, packageVersion, tempDir))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Failed to retrieve metadata")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(500));
        }
      }
    }

    @Test
    @DisplayName("should fail when token generation returns non-success response")
    void shouldFailWhenTokenGenerationReturnsNonSuccessResponse() throws Exception {
      // GIVEN
      try (StubRegistryServer server = StubRegistryServer.forTokenStatus(500)) {

        // WHEN THEN
        try (PackageDownloader packageDownloader = new PackageDownloader(server.getBaseUrl())) {
          assertThatThrownBy(
                  () -> packageDownloader.generateToken(List.of("bfarm.terminologien.icd10gm")))
              .isInstanceOf(PackageDownloadException.class)
              .hasMessageContaining("Failed to generate authentication token")
              .satisfies(
                  throwable ->
                      assertThat(((PackageDownloadException) throwable).getStatusCode())
                          .isEqualTo(500));
        }
      }
    }
  }

  private static final class StubRegistryServer implements AutoCloseable {

    private static final String PACKAGE_CONTENT = "test-package-content";

    private enum EndpointLayout {
      PACKAGES_PREFIX,
      ROOT
    }

    private final HttpServer httpServer;
    private final AtomicInteger metadataRequests;
    private final AtomicInteger tokenRequests;
    private final AtomicInteger catalogRequests;
    private final AtomicInteger downloadRequests;
    private String downloadAuthorizationHeader;
    private String downloadApiKeyHeader;

    private StubRegistryServer(HttpServer httpServer) {
      this.httpServer = httpServer;
      this.metadataRequests = new AtomicInteger(0);
      this.tokenRequests = new AtomicInteger(0);
      this.catalogRequests = new AtomicInteger(0);
      this.downloadRequests = new AtomicInteger(0);
    }

    static StubRegistryServer forSinglePackage(
        String packageName, String packageVersion, boolean isProtected) throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.registerSinglePackageEndpoints(
          packageName,
          packageVersion,
          isProtected,
          PackageDownloaderTestDataHelper.packageContentShaSum(),
          EndpointLayout.PACKAGES_PREFIX);
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forSinglePackageWithMetadataShaSum(
        String packageName, String packageVersion, boolean isProtected, String metadataShaSum)
        throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.registerSinglePackageEndpoints(
          packageName, packageVersion, isProtected, metadataShaSum, EndpointLayout.PACKAGES_PREFIX);
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forSinglePackageRootLayout(
        String packageName, String packageVersion, boolean isProtected) throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.registerSinglePackageEndpoints(
          packageName,
          packageVersion,
          isProtected,
          PackageDownloaderTestDataHelper.packageContentShaSum(),
          EndpointLayout.ROOT);
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forMetadataWithoutRequestedVersion(
        String packageName, String packageVersion) throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);

      String metadataPayload =
          PackageDownloaderTestDataHelper.packageMetadataPayload(packageName, "2025.0.0", true);
      server.httpServer.createContext(
          "/packages/" + packageName,
          exchange -> server.respondJson(exchange, metadataPayload, server.metadataRequests));

      server.httpServer.createContext(
          "/api/generate-token",
          exchange -> {
            server.tokenRequests.incrementAndGet();
            server.respondJson(exchange, "{\"token\":\"test-token\"}", null);
          });

      server.httpServer.createContext(
          "/packages/" + packageName + "/" + packageVersion,
          exchange -> server.respondText(exchange, 500, "should not be called"));

      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forCatalogOnly() throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.registerCatalogEndpoint(EndpointLayout.PACKAGES_PREFIX);
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forCatalogOnlyRootLayout() throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.registerCatalogEndpoint(EndpointLayout.ROOT);
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forCatalogWithInvalidShape() throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.httpServer.createContext(
          "/packages/catalog",
          exchange -> server.respondJson(exchange, "{\"catalog\":true}", server.catalogRequests));
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forCatalogWithDeprecatedAndUnlisted() throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);

      server.registerCatalogEndpoint(EndpointLayout.PACKAGES_PREFIX);

      String firstPackageMetadata =
          """
          {
            "name": "bfarm.terminologien.icd10gm",
            "versions": {
              "2024.0.0": {
                "dist": {
                  "shasum": "ca3cc32692594fef390ca56591832d7e73b2963f"
                }
              },
              "2025.0.0": {
                "unlisted": "This version is deprecated. Please use a different version.",
                "dist": {
                  "shasum": "ca3cc32692594fef390ca56591832d7e73b2963f"
                }
              }
            }
          }
          """;

      String secondPackageMetadata =
          """
          {
            "name": "bfarm.terminologien.loinc",
            "versions": {
              "2.81.0": {
                "deprecated": "Use package version 2.82.0 instead.",
                "dist": {
                  "shasum": "ca3cc32692594fef390ca56591832d7e73b2963f"
                }
              }
            }
          }
          """;

      server.httpServer.createContext(
          "/packages/bfarm.terminologien.icd10gm",
          exchange -> server.respondJson(exchange, firstPackageMetadata, server.metadataRequests));
      server.httpServer.createContext(
          "/packages/bfarm.terminologien.loinc",
          exchange -> server.respondJson(exchange, secondPackageMetadata, server.metadataRequests));

      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forDownloadWithStatus(
        String packageName, String packageVersion, int statusCode) throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      String metadataPayload =
          PackageDownloaderTestDataHelper.packageMetadataPayload(
              packageName,
              packageVersion,
              false,
              PackageDownloaderTestDataHelper.packageContentShaSum());
      server.httpServer.createContext(
          "/packages/" + packageName,
          exchange -> server.respondJson(exchange, metadataPayload, server.metadataRequests));
      server.httpServer.createContext(
          "/packages/" + packageName + "/" + packageVersion,
          exchange -> server.respondText(exchange, statusCode, "download failed"));
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forMetadataStatus(String packageName, int statusCode)
        throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.httpServer.createContext(
          "/packages/" + packageName,
          exchange -> server.respondText(exchange, statusCode, "metadata failed"));
      server.httpServer.start();
      return server;
    }

    static StubRegistryServer forTokenStatus(int statusCode) throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      StubRegistryServer server = new StubRegistryServer(httpServer);
      server.httpServer.createContext(
          "/api/generate-token",
          exchange -> server.respondText(exchange, statusCode, "token failed"));
      server.httpServer.start();
      return server;
    }

    String getBaseUrl() {
      InetSocketAddress address = httpServer.getAddress();
      return "http://127.0.0.1:" + address.getPort();
    }

    int getMetadataRequests() {
      return metadataRequests.get();
    }

    int getTokenRequests() {
      return tokenRequests.get();
    }

    int getCatalogRequests() {
      return catalogRequests.get();
    }

    int getDownloadRequests() {
      return downloadRequests.get();
    }

    String getDownloadAuthorizationHeader() {
      return downloadAuthorizationHeader;
    }

    String getDownloadApiKeyHeader() {
      return downloadApiKeyHeader;
    }

    @Override
    public void close() {
      httpServer.stop(0);
    }

    private void respondJson(HttpExchange exchange, String payload, AtomicInteger requestCounter)
        throws IOException {
      if (requestCounter != null) {
        requestCounter.incrementAndGet();
      }

      exchange.getResponseHeaders().add("Content-Type", "application/json");
      writePayload(exchange, 200, payload);
    }

    private void respondText(HttpExchange exchange, int statusCode, String payload)
        throws IOException {
      exchange.getResponseHeaders().add("Content-Type", "text/plain");
      writePayload(exchange, statusCode, payload);
    }

    private void writePayload(HttpExchange exchange, int statusCode, String payload)
        throws IOException {
      byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(statusCode, bytes.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(bytes);
      }
      exchange.close();
    }

    private void registerSinglePackageEndpoints(
        String packageName,
        String packageVersion,
        boolean isProtected,
        String metadataShaSum,
        EndpointLayout endpointLayout) {
      String metadataPath =
          endpointLayout == EndpointLayout.PACKAGES_PREFIX
              ? "/packages/" + packageName
              : "/" + packageName;
      String downloadPath =
          endpointLayout == EndpointLayout.PACKAGES_PREFIX
              ? "/packages/" + packageName + "/" + packageVersion
              : "/" + packageName + "/" + packageVersion;

      String metadataPayload =
          PackageDownloaderTestDataHelper.packageMetadataPayload(
              packageName, packageVersion, isProtected, metadataShaSum);

      httpServer.createContext(
          metadataPath, exchange -> respondJson(exchange, metadataPayload, metadataRequests));

      httpServer.createContext(
          "/api/generate-token",
          exchange -> {
            tokenRequests.incrementAndGet();
            respondJson(exchange, "{\"token\":\"test-token\"}", null);
          });

      httpServer.createContext(
          downloadPath,
          exchange -> {
            downloadRequests.incrementAndGet();
            downloadAuthorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
            downloadApiKeyHeader = exchange.getRequestHeaders().getFirst("X-API-Key");
            if (isProtected && !"Bearer test-token".equals(downloadAuthorizationHeader)) {
              respondText(exchange, 401, "missing token");
              return;
            }
            respondText(exchange, 200, PACKAGE_CONTENT);
          });
    }

    private void registerCatalogEndpoint(EndpointLayout endpointLayout) {
      String catalogPath =
          endpointLayout == EndpointLayout.PACKAGES_PREFIX ? "/packages/catalog" : "/catalog";
      httpServer.createContext(
          catalogPath,
          exchange ->
              respondJson(
                  exchange,
                  PackageDownloaderTestDataHelper.packageCatalogPayload(),
                  catalogRequests));
    }
  }
}
