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
package de.gematik.zts.termzilla.testing;

import static org.awaitility.Awaitility.await;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.Termzilla;
import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloadException;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloader;
import de.gematik.zts.termzilla.server.LazyTerminologyServer;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.nio.file.Path;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Abstract base class for FHIR integration tests.
 *
 * <p>Provides a fully configured test environment including:
 *
 * <ul>
 *   <li>Automatic test package deployment to temporary directories
 *   <li>Spring Boot application context with custom environment configuration
 *   <li>HAPI FHIR client connected to the test server instance
 * </ul>
 *
 * <p>The test environment uses per-class lifecycle and manual Spring context management to ensure
 * proper environment variable initialization, which cannot be reliably achieved with
 * {@code @SpringBootTest} annotation.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseFhirIT {

  /** Maximum time to wait for server initialization. */
  private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);

  /** Polling interval for readiness checks. */
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

  /** Temporary directory for the base working directory during test execution. */
  @TempDir private static Path baseWorkingTempDir;

  /** Temporary directory where test packages are copied for loading. */
  @TempDir private static Path packageLoadTempDir;

  /** Localhost URL prefix for the test server. */
  protected static final String LOCALHOST = "http://localhost";

  /** Port assigned to the test server instance. */
  protected String port;

  /** Management port for actuator endpoints. */
  protected String managementPort;

  /** Spring application context for the running test server. */
  protected ConfigurableApplicationContext context;

  protected FhirContext fhirContext = FhirContext.forR4Cached();

  /**
   * Initializes the test environment before all tests in the class.
   *
   * <p>Performs the following setup steps:
   *
   * <ol>
   *   <li>Copies required test packages to the temporary package directory
   *   <li>Configures environment variables for base working directory and package location
   *   <li>Starts the Spring Boot application with {@link Termzilla} on a random port
   *   <li>Creates and configures a HAPI FHIR R4 client for the test server
   * </ol>
   *
   * @throws Exception if test package deployment or application startup fails
   */
  @BeforeAll
  void setUp() throws Exception {

    copyData(packageLoadTempDir);

    log.info("Using package directory: {}", packageLoadTempDir);
    log.info("Using working directory: {}", baseWorkingTempDir);

    // Set environment variables and manually start Spring context
    EnvironmentVariables environmentVariables =
        new EnvironmentVariables()
            .set("CORE_BASEWORKINGDIR", baseWorkingTempDir.toString())
            .set(
                "CORE_SECURITY_RATELIMIT_ENABLED",
                "false") // disable rate limiting for tests to avoid interference with test
            // execution
            .set("CORE_RESOURCERETRIEVALMODE", "FULL")
            .set("FHIRTERMS_START", "true")
            .set("FHIRTERMS_PROVISIONMODE", "predefined")
            .set("FHIRTERMS_PROVISIONSETTINGSPATH", "")
            .set("FHIRTERMS_PACKAGEAUTODOWNLOADENABLED", "false")
            .set("FHIRTERMS_PACKAGEFOLDERLOCATION", packageLoadTempDir.toString());

    configureEnvironment(environmentVariables);

    environmentVariables.execute(
        () -> {
          // Verify that environment variable is set
          log.info("FHIRTERMS_START = {}", System.getenv("FHIRTERMS_START"));

          // Manually start Spring Boot application with environment variables set
          // (using @SpringBootTest caused issues with late environment variable initialization)
          SpringApplication app = new SpringApplication(Termzilla.class);
          // Force random port assignment
          System.setProperty("server.port", "0");
          System.setProperty("management.server.port", "0");
          context = app.run();

          // Determine assigned port
          port = context.getEnvironment().getProperty("local.server.port");
          managementPort = context.getEnvironment().getProperty("local.management.port");
          log.info("Server started on port: {}", port);
          log.info("Management server started on port: {}", managementPort);

          // Wait for LazyTerminologyServer to be fully initialized
          waitForServerReady();
        });
  }

  /**
   * Hook for subclasses to set additional or override environment variables before the Spring
   * context is started. Called once per test class, after the base variables are configured and
   * before {@code app.run()}.
   *
   * @param env the {@link EnvironmentVariables} instance to extend
   */
  protected void configureEnvironment(EnvironmentVariables env) {
    // no-op by default
  }

  protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
    try (PackageDownloader downloader =
        new PackageDownloader(
            120,
            "https://terminologien.bfarm.de",
            AuthenticationMode.BFARM_AUTO_TOKEN,
            null,
            null)) {
      downloader.downloadPackage("bfarm.terminologien.icd10gm", "2026.0.0", packageLoadTempDir);
      downloader.downloadPackage("bfarm.terminologien.icd10gm", "2025.0.0", packageLoadTempDir);
    }

    // Copy test packages to temporary directory
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.0.tgz", packageLoadTempDir);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.1.tgz", packageLoadTempDir);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2025.0.0.tgz", packageLoadTempDir);
  }

  /**
   * Waits for the LazyTerminologyServer to complete initialization using Awaitility.
   *
   * <p>This method polls the server's initialization status at regular intervals and fails the test
   * if initialization doesn't complete within the timeout period.
   *
   * @throws org.awaitility.core.ConditionTimeoutException if the server doesn't become ready within
   *     the timeout
   */
  private void waitForServerReady() {
    log.info("Waiting for TerminologyServer initialization...");

    LazyTerminologyServer lazyServer = context.getBean(LazyTerminologyServer.class);

    await()
        .atMost(MAX_WAIT_TIME)
        .pollInterval(POLL_INTERVAL)
        .pollDelay(Duration.ofSeconds(5)) // Wait 5s before first check
        .conditionEvaluationListener(
            condition ->
                log.info(
                    "Server initialization check: {} seconds elapsed",
                    condition.getElapsedTimeInMS() / 1000))
        .until(lazyServer::isInitialized);

    log.info("TerminologyServer initialization complete! Tests can now run.");
  }

  /**
   * Cleans up test resources after all tests in the class have completed.
   *
   * <p>Gracefully shuts down the Spring application context if it was successfully initialized.
   */
  @AfterAll
  void tearDown() {
    if (context != null) {
      log.info("Shutting down Spring context...");
      context.close();
    }
  }

  // ==================== Helper Methods ====================

  protected IGenericClient getFhirClient(String scope) {
    String baseUrl = LOCALHOST + ":" + port + "/" + scope;
    return fhirContext.newRestfulGenericClient(baseUrl);
  }
}
