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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerAuthConfig;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfig;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver.PackageServerConfigEntry;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FhirTermsTerminologyManagerTest {

  private static final String DEFAULT_SCOPE = "fhir";

  private Path workingPath;
  private Path loadPath;
  private Path indexPath;
  private Path unzipPath;
  private FhirConfigProperties mockProperties;
  private FhirTermsTerminologyManager manager;

  @BeforeEach
  void setUp() throws InvalidConfigurationException, IOException {

    workingPath = Files.createTempDirectory("working");
    loadPath = Files.createTempDirectory("fhir-terms");
    indexPath = Files.createTempDirectory("lucene");
    unzipPath = Files.createTempDirectory("unzip");

    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.0.tgz", loadPath);

    mockProperties = mock(FhirConfigProperties.class);
    when(mockProperties.isFhirTermsStart()).thenReturn(true);
    when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(loadPath.toString());
    when(mockProperties.getFhirTermsIndex()).thenReturn(indexPath);
    when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipPath);

    manager = new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, mockProperties);
  }

  @AfterEach
  void tearDown() throws IOException {

    TestPackageHelper.deletePackageDirectory(unzipPath);
    TestPackageHelper.deletePackageDirectory(indexPath);
    TestPackageHelper.deletePackageDirectory(loadPath);
    TestPackageHelper.deletePackageDirectory(workingPath);
  }

  @Test
  @DisplayName("Constructor with valid parameters should initialize manager")
  void testConstructorWithValidParameters() throws InvalidConfigurationException {
    manager = new FhirTermsTerminologyManager(loadPath, DEFAULT_SCOPE, mockProperties);
    assertNotNull(manager);
    assertNotNull(manager.getFhirTermsInMemoryRepresentation());
  }

  @Test
  @DisplayName("Constructor with null path should throw InvalidConfigurationException")
  void testConstructorWithNullPath() {
    assertThrows(
        InvalidConfigurationException.class,
        () -> new FhirTermsTerminologyManager(null, "default", null));
  }

  @Test
  @DisplayName("isInitialized should return true after successful initialization")
  void testIsInitialized() throws InvalidConfigurationException {
    manager = new FhirTermsTerminologyManager(loadPath, DEFAULT_SCOPE, mockProperties);
    assertTrue(manager.isInitialized());
  }

  @Test
  @DisplayName("getTerminologyManagerName should return correct name")
  void testGetTerminologyManagerName() throws InvalidConfigurationException {
    manager = new FhirTermsTerminologyManager(loadPath, DEFAULT_SCOPE, mockProperties);
    assertEquals("Basic FHIR Terminology Manager", manager.getTerminologyManagerName());
  }

  @Test
  @DisplayName(
      "getSupportedScopes should return set with two scopes (default scope + loaded packages scope)")
  void testGetSupportedScopes() {
    Set<String> scopes = manager.getSupportedScopes();
    assertNotNull(scopes);
    assertEquals(2, scopes.size());
    assertTrue(scopes.contains(DEFAULT_SCOPE));
    assertTrue(scopes.contains("bfarm.terminologien.test-2024.0.0"));
  }

  @Test
  @DisplayName("cs should return non-null CodeSystemSvcInterface")
  void testCs() {
    CodeSystemSvcInterface csInterface = manager.cs();
    assertNotNull(csInterface);
  }

  @Test
  @DisplayName("vs should return non-null ValueSetSvcInterface")
  void testVs() {
    ValueSetSvcInterface vsInterface = manager.vs();
    assertNotNull(vsInterface);
  }

  @Test
  @DisplayName("cm should return non-null ConceptMapSvcInterface")
  void testCm() {
    ConceptMapSvcInterface cmInterface = manager.cm();
    assertNotNull(cmInterface);
  }

  @Test
  @DisplayName("toString should return correct string representation")
  void testToString() {
    String expected = "FhirTermsTerminologyManager (Basic FHIR Terminology Manager)";
    assertEquals(expected, manager.toString());
  }

  @Nested
  @DisplayName("constructor package download behavior")
  class ConstructorPackageDownloadBehaviorTests {

    @Test
    @DisplayName("should initialize when auto-download is disabled and no servers are configured")
    void shouldInitializeWhenAutoDownloadIsDisabledAndNoServersAreConfigured() throws Exception {
      // GIVEN
      FhirConfigProperties config = createPackageDownloadConfig(loadPath, false, List.of());

      // WHEN
      FhirTermsTerminologyManager termsManager =
          new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config);

      // THEN
      assertThat(termsManager.isInitialized()).isTrue();
      assertThat(termsManager.getSupportedScopes()).contains(DEFAULT_SCOPE);
    }

    @Test
    @DisplayName("should fail when auto-download is enabled and package folder is missing")
    void shouldFailWhenAutoDownloadIsEnabledAndPackageFolderIsMissing() {
      // GIVEN
      PackageServerConfigEntry server =
          packageServer("https://example.org", "bfarm.terminologien.test@2024.0.0");
      FhirConfigProperties config = createPackageDownloadConfig(null, true, List.of(server));

      // WHEN THEN
      assertThatThrownBy(() -> new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("packageFolderLocation");
    }

    @Test
    @DisplayName("should fail when auto-download is enabled and no servers are configured")
    void shouldFailWhenAutoDownloadIsEnabledAndNoServersAreConfigured() {
      // GIVEN
      FhirConfigProperties config = createPackageDownloadConfig(loadPath, true, List.of());

      // WHEN THEN
      assertThatThrownBy(() -> new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("packageServerConfig.servers");
    }

    @Test
    @DisplayName("should fail when configured server url is blank")
    void shouldFailWhenConfiguredServerUrlIsBlank() {
      // GIVEN
      PackageServerConfigEntry server = packageServer("   ", "bfarm.terminologien.test@2024.0.0");
      FhirConfigProperties config = createPackageDownloadConfig(loadPath, true, List.of(server));

      // WHEN THEN
      assertThatThrownBy(() -> new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("non-empty url");
    }

    @Test
    @DisplayName("should fail when package entry format is invalid")
    void shouldFailWhenPackageEntryFormatIsInvalid() {
      // GIVEN
      PackageServerConfigEntry server = packageServer("https://example.org", "invalid-entry");
      FhirConfigProperties config = createPackageDownloadConfig(loadPath, true, List.of(server));

      // WHEN THEN
      assertThatThrownBy(() -> new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Invalid package name/version entry");
    }

    @Test
    @DisplayName("should wrap package download exceptions when registry download fails")
    void shouldWrapPackageDownloadExceptionsWhenRegistryDownloadFails() throws Exception {
      // GIVEN
      Path emptyPackageDir = Files.createTempDirectory(workingPath, "auto-dl-wrap-");
      try (FailingRegistryServer server = FailingRegistryServer.withDownloadStatus(500)) {
        PackageServerConfigEntry packageServer =
            packageServer(server.getBaseUrl(), "bfarm.terminologien.test@2024.0.0");
        FhirConfigProperties config =
            createPackageDownloadConfig(emptyPackageDir, true, List.of(packageServer));

        // WHEN THEN
        assertThatThrownBy(
                () -> new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config))
            .isInstanceOf(InvalidConfigurationException.class)
            .hasMessageContaining("Failed to auto-download configured FHIR packages")
            .hasMessageContaining("Status: 500");
      }
    }

    @Test
    @DisplayName("should skip auto-download when package file already exists")
    void shouldSkipAutoDownloadWhenPackageFileAlreadyExists() throws Exception {
      // GIVEN
      Path existingPackageFile = loadPath.resolve("bfarm.terminologien.test-2024.0.0.tgz");
      String existingPackageSha1 = calculateSha1(existingPackageFile);

      try (FailingRegistryServer server =
          FailingRegistryServer.withDownloadStatus(500, existingPackageSha1)) {
        PackageServerConfigEntry packageServer =
            packageServer(server.getBaseUrl(), "bfarm.terminologien.test@2024.0.0");
        FhirConfigProperties config =
            createPackageDownloadConfig(loadPath, true, List.of(packageServer));

        // WHEN
        FhirTermsTerminologyManager termsManager =
            new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, config);

        // THEN
        assertThat(termsManager.isInitialized()).isTrue();
        assertThat(server.getDownloadRequests()).isZero();
      }
    }
  }

  private String calculateSha1(Path filePath) throws IOException {
    try (InputStream inputStream = Files.newInputStream(filePath)) {
      return DigestUtils.sha1Hex(inputStream);
    }
  }

  private FhirConfigProperties createPackageDownloadConfig(
      Path packageDir, boolean autoDownloadEnabled, List<PackageServerConfigEntry> servers) {
    FhirConfigProperties config = new FhirConfigProperties();
    config.setBaseWorkingDirectory(workingPath);
    config.setFhirTermsStart(true);
    config.setPackageAutoDownloadEnabled(autoDownloadEnabled);
    config.setPackageServerConfig(new PackageServerConfig(servers));
    config.setFhirTermsPackageFolderLocation(packageDir == null ? null : packageDir.toString());
    config.setVersion("test-version");
    return config;
  }

  private PackageServerConfigEntry packageServer(String url, String packageEntry) {
    return new PackageServerConfigEntry(
        "test-server",
        url,
        new PackageServerAuthConfig(AuthenticationMode.NONE, null, null, null, null, null),
        null,
        Set.of(packageEntry));
  }

  private static final class FailingRegistryServer implements AutoCloseable {

    private final HttpServer httpServer;
    private final AtomicInteger downloadRequests;

    private FailingRegistryServer(HttpServer httpServer) {
      this.httpServer = httpServer;
      this.downloadRequests = new AtomicInteger(0);
    }

    static FailingRegistryServer withDownloadStatus(int statusCode) throws IOException {
      return withDownloadStatus(statusCode, "ca3cc32692594fef390ca56591832d7e73b2963f");
    }

    static FailingRegistryServer withDownloadStatus(int statusCode, String shaSum)
        throws IOException {
      HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      FailingRegistryServer server = new FailingRegistryServer(httpServer);

      String metadataPayload =
          """
          {
            "name": "bfarm.terminologien.test",
            "versions": {
              "2024.0.0": {
                "protected": false,
                "dist": {
                  "shasum": "%s"
                }
              }
            }
          }
          """
              .formatted(shaSum);

      httpServer.createContext(
          "/packages/bfarm.terminologien.test",
          exchange -> server.respond(exchange, 200, metadataPayload, "application/json"));

      httpServer.createContext(
          "/packages/bfarm.terminologien.test/2024.0.0",
          exchange -> {
            server.downloadRequests.incrementAndGet();
            server.respond(exchange, statusCode, "download failed", "text/plain");
          });

      httpServer.start();
      return server;
    }

    String getBaseUrl() {
      return "http://127.0.0.1:" + httpServer.getAddress().getPort();
    }

    int getDownloadRequests() {
      return downloadRequests.get();
    }

    @Override
    public void close() {
      httpServer.stop(0);
    }

    private void respond(HttpExchange exchange, int statusCode, String payload, String contentType)
        throws IOException {
      exchange.getResponseHeaders().add("Content-Type", contentType);
      byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(statusCode, bytes.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(bytes);
      }
      exchange.close();
    }
  }
}
