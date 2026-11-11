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
package de.gematik.zts.termzilla.configuration;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.AUTHORIZATION;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CONTENT_TYPE;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ConfigProperties}.
 *
 * <p>Tests the configuration properties validation and helper methods.
 */
@DisplayName("ConfigProperties Tests")
class ConfigPropertiesTest {

  private ConfigProperties configProperties;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    configProperties = new ConfigProperties();
    setupDefaultValidConfiguration();
  }

  /**
   * Sets up a minimal valid configuration to avoid validation errors in tests that don't
   * specifically test validation.
   */
  private void setupDefaultValidConfiguration() {
    configProperties.setVersion("0.8.15");
    configProperties.setBaseWorkingDir(tempDir.toString());
    configProperties.setSystemLanguage("de-DE");
    configProperties.setCorsAllowedOrigins(List.of("http://localhost:3000"));
    configProperties.setCorsAllowedMethods(List.of("GET", "POST"));
    configProperties.setCorsAllowedHeaders(List.of("*"));
    configProperties.setCorsExposedHeaders(List.of());
    configProperties.setRateLimitEnabled(false);
    configProperties.setRateLimitRequestsPerMinute(100);
    configProperties.setMaxParameterLength(1000);
    configProperties.setMaxRequestSizeBytes(10485760);
    configProperties.setCachingEnabled(true);
    configProperties.setCachingMaxAge(600);
    configProperties.setCachingSharedMaxAge(-1);
    configProperties.setCachingMustRevalidate(true);
    configProperties.setCachingStaleWhileRevalidate(-1);
    configProperties.setPagingProviderSize(1000);
    configProperties.setDefaultPageSize(20);
    configProperties.setMaximumPageSize(100);
    configProperties.setSystemLevelBatchEnabled(true);
    configProperties.setSystemLevelBatchMaxBatchSize(1000);
    configProperties.setCapabilitiesName("TERMzilla");
    configProperties.setCapabilitiesTitle("TERMzilla Server");
    configProperties.setCapabilitiesDescription("Test Server");
    configProperties.setCapabilitiesPublisher("Test Publisher");
    configProperties.setCapabilitiesStatus("active");
    configProperties.setCapabilitiesExperimental(true);
    configProperties.setCapabilitiesSoftwareName("TERMzilla");
    configProperties.setCapabilitiesSoftwareVersion("1.0.0");
    configProperties.setCapabilitiesSoftwareReleaseDate("2023-10-01");
    configProperties.setCapabilitiesRestSecurityDescription("Test security");
  }

  @Nested
  @DisplayName("Validation Tests - Base Working Directory")
  class BaseWorkingDirValidationTests {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should fail validation when base working directory is null or empty")
    void shouldFailValidationWhenBaseWorkingDirIsNullOrEmpty(String baseWorkingDir) {
      // GIVEN
      configProperties.setBaseWorkingDir(baseWorkingDir);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Base working directory is not configured");
    }

    @Test
    @DisplayName("Should fail validation when base working directory does not exist")
    void shouldFailValidationWhenBaseWorkingDirDoesNotExist() {
      // GIVEN
      String nonExistentDir = tempDir.resolve("non-existent").toString();
      configProperties.setBaseWorkingDir(nonExistentDir);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Base working directory does not exist or is not a directory");
    }

    @Test
    @DisplayName("Should fail validation when base working directory is a file")
    void shouldFailValidationWhenBaseWorkingDirIsFile() throws IOException {
      // GIVEN
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);
      configProperties.setBaseWorkingDir(file.toString());

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Base working directory does not exist or is not a directory");
    }

    @Test
    @DisplayName("Should fail validation when base working directory is not writable")
    void shouldFailValidationWhenBaseWorkingDirIsNotWritable() throws IOException {
      // GIVEN
      Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectories(readOnlyDir);

      boolean isReadOnly = false;

      try {
        // Try POSIX permissions first (Linux/Unix)
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("r-xr-xr-x");
        Files.setPosixFilePermissions(readOnlyDir, permissions);
        isReadOnly = !Files.isWritable(readOnlyDir);
      } catch (UnsupportedOperationException _) {
        // Fallback for Windows
        isReadOnly = readOnlyDir.toFile().setWritable(false) && !Files.isWritable(readOnlyDir);
      }

      // Skip test if we cannot make directory read-only
      assumeThat(isReadOnly)
          .as("Cannot create read-only directory in this environment (possibly running as root)")
          .isTrue();

      configProperties.setBaseWorkingDir(readOnlyDir.toString());

      try {
        // WHEN & THEN
        assertThatThrownBy(() -> configProperties.validate())
            .isInstanceOf(InvalidConfigurationException.class)
            .hasMessageContaining("Base working directory is not writable");
      } finally {
        // CLEANUP - restore write permissions
        try {
          Set<PosixFilePermission> writePermissions = PosixFilePermissions.fromString("rwxrwxrwx");
          Files.setPosixFilePermissions(readOnlyDir, writePermissions);
        } catch (UnsupportedOperationException _) {
          readOnlyDir.toFile().setWritable(true);
        }
      }
    }

    @Test
    @DisplayName("Should pass validation with valid writable directory")
    void shouldPassValidationWithValidWritableDirectory() {
      // GIVEN
      configProperties.setBaseWorkingDir(tempDir.toString());

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Tests - System Language")
  class SystemLanguageValidationTests {

    @ParameterizedTest
    @ValueSource(strings = {"de-DE", "en-US", "fr-FR", "es-ES"})
    @DisplayName("Should pass validation with supported language codes")
    void shouldPassValidationWithSupportedLanguageCodes(String languageCode) {
      // GIVEN
      configProperties.setSystemLanguage(languageCode);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "xx-XX", "de_DE", "123"})
    @DisplayName("Should fail validation with unsupported language codes")
    void shouldFailValidationWithUnsupportedLanguageCodes(String languageCode) {
      // GIVEN
      configProperties.setSystemLanguage(languageCode);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Configured systemLanguage is not supported");
    }
  }

  @Nested
  @DisplayName("Validation Tests - CORS Configuration")
  class CorsConfigurationValidationTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "http://localhost:3000",
          "https://app.example.com",
          "https://app.example.com:8080",
          "http://192.168.1.1:3000"
        })
    @DisplayName("Should pass validation with valid CORS origins")
    void shouldPassValidationWithValidCorsOrigins(String origin) {
      // GIVEN
      configProperties.setCorsAllowedOrigins(List.of(origin));

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-url", "just-text", "://missing-scheme"})
    @DisplayName("Should fail validation with invalid CORS origins")
    void shouldFailValidationWithInvalidCorsOrigins(String invalidOrigin) {
      // GIVEN
      configProperties.setCorsAllowedOrigins(List.of(invalidOrigin));

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Invalid CORS allowed origin");
    }

    @Test
    @DisplayName("Should pass validation with multiple valid CORS origins")
    void shouldPassValidationWithMultipleValidCorsOrigins() {
      // GIVEN
      configProperties.setCorsAllowedOrigins(
          List.of("http://localhost:3000", "https://app.example.com", "https://admin.example.com"));

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"})
    @DisplayName("Should pass validation with valid HTTP methods")
    void shouldPassValidationWithValidHttpMethods(String method) {
      // GIVEN
      configProperties.setCorsAllowedMethods(List.of(method));

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "CONNECT"})
    @DisplayName("Should fail validation with invalid HTTP methods")
    void shouldFailValidationWithInvalidHttpMethods(String method) {
      // GIVEN
      configProperties.setCorsAllowedMethods(List.of(method));

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Invalid CORS allowed method");
    }

    @Test
    @DisplayName("Should pass validation with multiple valid HTTP methods")
    void shouldPassValidationWithMultipleValidHttpMethods() {
      // GIVEN
      configProperties.setCorsAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Tests - Rate Limiting")
  class RateLimitingValidationTests {

    @Test
    @DisplayName("Should pass validation when rate limiting is disabled")
    void shouldPassValidationWhenRateLimitingIsDisabled() {
      // GIVEN
      configProperties.setRateLimitEnabled(false);
      configProperties.setRateLimitRequestsPerMinute(-1); // Invalid value, but should be ignored

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000, Integer.MAX_VALUE})
    @DisplayName("Should pass validation with valid rate limit values")
    void shouldPassValidationWithValidRateLimitValues(int requestsPerMinute) {
      // GIVEN
      configProperties.setRateLimitEnabled(true);
      configProperties.setRateLimitRequestsPerMinute(requestsPerMinute);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should fail validation with invalid rate limit values when enabled")
    void shouldFailValidationWithInvalidRateLimitValues(int requestsPerMinute) {
      // GIVEN
      configProperties.setRateLimitEnabled(true);
      configProperties.setRateLimitRequestsPerMinute(requestsPerMinute);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Rate limit requests per minute must be greater than 0");
    }
  }

  @Nested
  @DisplayName("Validation Tests - Query parameter Security")
  class QuerySecurityValidationTests {

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should fail validation with invalid query parameter max length")
    void shouldFailValidationWithInvalidQueryMaxLength(int maxLength) {
      // GIVEN
      configProperties.setMaxParameterLength(maxLength);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Query parameter max length must be non-negative (0 = disabled).");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("Should fail validation with invalid query max results")
    void shouldFailValidationWithInvalidQueryMaxResults(int maxResults) {
      // GIVEN
      configProperties.setMaxParameterLength(maxResults);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Query parameter max length must be non-negative (0 = disabled).");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 100, 1000, 10000})
    @DisplayName("Should pass validation with valid query settings")
    void shouldPassValidationWithValidQuerySettings(int value) {
      // GIVEN
      configProperties.setMaxParameterLength(value);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Tests - Paging Configuration")
  class PagingConfigurationValidationTests {

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should fail validation with invalid paging provider size")
    void shouldFailValidationWithInvalidPagingProviderSize(int size) {
      // GIVEN
      configProperties.setPagingProviderSize(size);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Paging provider size must be greater than 0");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should fail validation with invalid default page size")
    void shouldFailValidationWithInvalidDefaultPageSize(int size) {
      // GIVEN
      configProperties.setDefaultPageSize(size);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Default page size must be greater than 0");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should fail validation with invalid maximum page size")
    void shouldFailValidationWithInvalidMaximumPageSize(int size) {
      // GIVEN
      configProperties.setMaximumPageSize(size);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Maximum page size must be greater than 0");
    }

    @Test
    @DisplayName("Should fail validation when default page size exceeds maximum")
    void shouldFailValidationWhenDefaultPageSizeExceedsMaximum() {
      // GIVEN
      configProperties.setDefaultPageSize(100);
      configProperties.setMaximumPageSize(50);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Default page size cannot be greater than maximum page size");
    }

    @ParameterizedTest
    @MethodSource("provideValidPagingSettings")
    @DisplayName("Should pass validation with valid paging settings")
    void shouldPassValidationWithValidPagingSettings(
        int providerSize, int defaultSize, int maxSize) {
      // GIVEN
      configProperties.setPagingProviderSize(providerSize);
      configProperties.setDefaultPageSize(defaultSize);
      configProperties.setMaximumPageSize(maxSize);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideValidPagingSettings() {
      return Stream.of(
          Arguments.of(1000, 20, 100),
          Arguments.of(500, 10, 50),
          Arguments.of(2000, 50, 200),
          Arguments.of(1000, 100, 100) // Equal default and max is valid
          );
    }
  }

  @Nested
  @DisplayName("Defaults Tests – CDN-Optimized Mode")
  class CdnOptimizedDefaultTests {

    @Test
    @DisplayName("Should default cachingCdnOptimized to false")
    void cachingCdnOptimizedDefaultsToFalse() {
      // GIVEN — fresh instance with no cdnOptimized property set (setupDefaultValidConfiguration
      // does not set cachingCdnOptimized, so the field retains its Java default)

      // WHEN
      boolean result = configProperties.isCachingCdnOptimized();

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should set and get cachingCdnOptimized to true")
    void cachingCdnOptimizedCanBeSetToTrue() {
      // GIVEN
      configProperties.setCachingCdnOptimized(true);

      // WHEN
      boolean result = configProperties.isCachingCdnOptimized();

      // THEN
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("Validation Tests - System Level Operations")
  class SystemLevelOperationsValidationTests {

    @Test
    @DisplayName("Should fail validation when batch is enabled and max batch size is non-positive")
    void shouldFailValidationWhenBatchEnabledAndMaxBatchSizeIsNonPositive() {
      // GIVEN
      configProperties.setSystemLevelBatchEnabled(true);
      configProperties.setSystemLevelBatchMaxBatchSize(0);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(
              "System-level batch max size must be greater than 0 when batch is enabled");
    }

    @Test
    @DisplayName("Should pass validation when batch is disabled and max batch size is non-positive")
    void shouldPassValidationWhenBatchDisabledAndMaxBatchSizeIsNonPositive() {
      // GIVEN
      configProperties.setSystemLevelBatchEnabled(false);
      configProperties.setSystemLevelBatchMaxBatchSize(0);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Tests - Caching Configuration")
  class CachingConfigurationValidationTests {

    @Test
    @DisplayName("Should throw when must-revalidate and stale-while-revalidate are combined")
    void throwsWhenMustRevalidateAndStaleWhileRevalidateCombined() {
      // GIVEN
      configProperties.setCachingMustRevalidate(true);
      configProperties.setCachingStaleWhileRevalidate(60);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(
              "must-revalidate and stale-while-revalidate are mutually exclusive");
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -10, Integer.MIN_VALUE})
    @DisplayName("Should fail validation when max-age is negative")
    void shouldFailValidationWhenMaxAgeIsNegative(int maxAge) {
      // GIVEN
      configProperties.setCachingMaxAge(maxAge);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Caching max-age must be non-negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -10, Integer.MIN_VALUE})
    @DisplayName("Should fail validation when shared max-age is less than -1")
    void shouldFailValidationWhenSharedMaxAgeIsLessThanMinusOne(int sharedMaxAge) {
      // GIVEN
      configProperties.setCachingSharedMaxAge(sharedMaxAge);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Caching shared max-age must be -1 (disabled) or non-negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, -10, Integer.MIN_VALUE})
    @DisplayName("Should fail validation when stale-while-revalidate is less than -1")
    void shouldFailValidationWhenStaleWhileRevalidateIsLessThanMinusOne(int staleWhileRevalidate) {
      // GIVEN
      configProperties.setCachingStaleWhileRevalidate(staleWhileRevalidate);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(
              "Caching stale-while-revalidate must be -1 (disabled) or non-negative");
    }

    @Test
    @DisplayName(
        "Should pass validation with stale-while-revalidate when must-revalidate is disabled")
    void shouldPassValidationWithStaleWhileRevalidateWhenMustRevalidateIsDisabled() {
      // GIVEN
      configProperties.setCachingMustRevalidate(false);
      configProperties.setCachingStaleWhileRevalidate(60);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should pass validation with default caching settings")
    void shouldPassValidationWithDefaultCachingSettings() {
      // GIVEN — defaults already set by setupDefaultValidConfiguration

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Tests - Capabilities Configuration")
  class CapabilitiesConfigurationValidationTests {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should fail validation when capabilities status is null or empty")
    void shouldFailValidationWhenCapabilitiesStatusIsNullOrEmpty(String status) {
      // GIVEN
      configProperties.setCapabilitiesStatus(status);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Capabilities status must be configured");
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "draft", "retired", "unknown"})
    @DisplayName("Should pass validation with valid publication status")
    void shouldPassValidationWithValidPublicationStatus(String status) {
      // GIVEN
      configProperties.setCapabilitiesStatus(status);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "ACTIVEe", "Drift", "pending"})
    @DisplayName("Should fail validation with invalid publication status")
    void shouldFailValidationWithInvalidPublicationStatus(String status) {
      // GIVEN
      configProperties.setCapabilitiesStatus(status);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Invalid capabilities status");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"2023-10-01", "2024-01-15", "2023-10-01T12:00:00Z", "2023-10-01T12:00:00+01:00"})
    @DisplayName("Should pass validation with valid ISO 8601 date formats")
    void shouldPassValidationWithValidIso8601DateFormats(String date) {
      // GIVEN
      configProperties.setCapabilitiesSoftwareReleaseDate(date);

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023/10/01", "01-10-2023", "invalid-date", "2023-13-01", "2023-10-32"})
    @DisplayName("Should fail validation with invalid date formats")
    void shouldFailValidationWithInvalidDateFormats(String date) {
      // GIVEN
      configProperties.setCapabilitiesSoftwareReleaseDate(date);

      // WHEN & THEN
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("Invalid software release date format");
    }
  }

  @Nested
  @DisplayName("Helper Method Tests - isReadEnabledForResource")
  class IsReadEnabledForResourceTests {

    @ParameterizedTest
    @MethodSource("provideResourceTypesWithReadSettings")
    @DisplayName("Should return correct read enabled status for resource types")
    void shouldReturnCorrectReadEnabledStatus(String resourceType, boolean expectedEnabled) {
      // GIVEN
      configProperties.setCodeSystemReadEnabled(true);
      configProperties.setValueSetReadEnabled(false);
      configProperties.setConceptMapReadEnabled(true);

      // WHEN
      boolean result = configProperties.isReadEnabledForResource(resourceType);

      // THEN
      assertThat(result).isEqualTo(expectedEnabled);
    }

    @Test
    @DisplayName("Should return false for unknown resource types")
    void shouldReturnFalseForUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "UnknownResource";

      // WHEN
      boolean result = configProperties.isReadEnabledForResource(unknownResourceType);

      // THEN
      assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideResourceTypesWithReadSettings() {
      return Stream.of(
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, false),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, true));
    }
  }

  @Nested
  @DisplayName("Helper Method Tests - isSearchEnabledForResource")
  class IsSearchEnabledForResourceTests {

    @ParameterizedTest
    @MethodSource("provideResourceTypesWithSearchSettings")
    @DisplayName("Should return correct search enabled status for resource types")
    void shouldReturnCorrectSearchEnabledStatus(String resourceType, boolean expectedEnabled) {
      // GIVEN
      configProperties.setCodeSystemSearchEnabled(false);
      configProperties.setValueSetSearchEnabled(true);
      configProperties.setConceptMapSearchEnabled(false);

      // WHEN
      boolean result = configProperties.isSearchEnabledForResource(resourceType);

      // THEN
      assertThat(result).isEqualTo(expectedEnabled);
    }

    @Test
    @DisplayName("Should return false for unknown resource types")
    void shouldReturnFalseForUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "Patient";

      // WHEN
      boolean result = configProperties.isSearchEnabledForResource(unknownResourceType);

      // THEN
      assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideResourceTypesWithSearchSettings() {
      return Stream.of(
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, false),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, false));
    }
  }

  @Nested
  @DisplayName("Helper Method Tests - getAllowedSearchParamsForResource")
  class GetAllowedSearchParamsForResourceTests {

    @Test
    @DisplayName("Should return configured search parameters for CodeSystem")
    void shouldReturnConfiguredSearchParametersForCodeSystem() {
      // GIVEN
      Set<String> allowedParams = Set.of("url", "version", "name");
      configProperties.setAllowedCodeSystemSearchParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedSearchParamsForResource(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return configured search parameters for ValueSet")
    void shouldReturnConfiguredSearchParametersForValueSet() {
      // GIVEN
      Set<String> allowedParams = Set.of("url", "name", "status");
      configProperties.setAllowedValueSetSearchParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedSearchParamsForResource(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return configured search parameters for ConceptMap")
    void shouldReturnConfiguredSearchParametersForConceptMap() {
      // GIVEN
      Set<String> allowedParams = Set.of("url", "source", "target");
      configProperties.setAllowedConceptMapSearchParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedSearchParamsForResource(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return null for unknown resource types")
    void shouldReturnNullForUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "Observation";

      // WHEN
      Set<String> result = configProperties.getAllowedSearchParamsForResource(unknownResourceType);

      // THEN
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("Helper Method Tests - isOperationEnabled")
  class IsOperationEnabledTests {

    @ParameterizedTest
    @MethodSource("provideCodeSystemOperations")
    @DisplayName("Should return correct enabled status for CodeSystem operations")
    void shouldReturnCorrectEnabledStatusForCodeSystemOperations(
        String operation, boolean expectedEnabled) {
      // GIVEN
      configProperties.setCodeSystemLookupEnabled(true);
      configProperties.setCodeSystemValidateCodeEnabled(false);
      configProperties.setCodeSystemFindMatchesEnabled(true);
      configProperties.setCodeSystemSubsumesEnabled(false);

      // WHEN
      boolean result =
          configProperties.isOperationEnabled(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, operation);

      // THEN
      assertThat(result).isEqualTo(expectedEnabled);
    }

    @ParameterizedTest
    @MethodSource("provideValueSetOperations")
    @DisplayName("Should return correct enabled status for ValueSet operations")
    void shouldReturnCorrectEnabledStatusForValueSetOperations(
        String operation, boolean expectedEnabled) {
      // GIVEN
      configProperties.setValueSetExpandEnabled(true);
      configProperties.setValueSetValidateCodeEnabled(false);

      // WHEN
      boolean result =
          configProperties.isOperationEnabled(ConfigProperties.RESOURCE_TYPE_VALUE_SET, operation);

      // THEN
      assertThat(result).isEqualTo(expectedEnabled);
    }

    @ParameterizedTest
    @MethodSource("provideConceptMapOperations")
    @DisplayName("Should return correct enabled status for ConceptMap operations")
    void shouldReturnCorrectEnabledStatusForConceptMapOperations(
        String operation, boolean expectedEnabled) {
      // GIVEN
      configProperties.setConceptMapTranslateEnabled(false);
      configProperties.setConceptMapClosureEnabled(true);

      // WHEN
      boolean result =
          configProperties.isOperationEnabled(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, operation);

      // THEN
      assertThat(result).isEqualTo(expectedEnabled);
    }

    @Test
    @DisplayName("Should return false when resource type is null")
    void shouldReturnFalseWhenResourceTypeIsNull() {
      // GIVEN
      String operation = ConfigProperties.OPERATION_LOOKUP;

      // WHEN
      boolean result = configProperties.isOperationEnabled(null, operation);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when operation is null")
    void shouldReturnFalseWhenOperationIsNull() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      // WHEN
      boolean result = configProperties.isOperationEnabled(resourceType, null);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for unknown operations")
    void shouldReturnFalseForUnknownOperations() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String unknownOperation = "$unknown-operation";

      // WHEN
      boolean result = configProperties.isOperationEnabled(resourceType, unknownOperation);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for unknown resource types")
    void shouldReturnFalseForUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "Patient";
      String operation = ConfigProperties.OPERATION_LOOKUP;

      // WHEN
      boolean result = configProperties.isOperationEnabled(unknownResourceType, operation);

      // THEN
      assertThat(result).isFalse();
    }

    private static Stream<Arguments> provideCodeSystemOperations() {
      return Stream.of(
          Arguments.of(ConfigProperties.OPERATION_LOOKUP, true),
          Arguments.of(ConfigProperties.OPERATION_VALIDATE_CODE, false),
          Arguments.of(ConfigProperties.OPERATION_FIND_MATCHES, true),
          Arguments.of(ConfigProperties.OPERATION_SUBSUMES, false));
    }

    private static Stream<Arguments> provideValueSetOperations() {
      return Stream.of(
          Arguments.of(ConfigProperties.OPERATION_EXPAND, true),
          Arguments.of(ConfigProperties.OPERATION_VALIDATE_CODE, false));
    }

    private static Stream<Arguments> provideConceptMapOperations() {
      return Stream.of(
          Arguments.of(ConfigProperties.OPERATION_TRANSLATE, false),
          Arguments.of(ConfigProperties.OPERATION_CLOSURE, true));
    }
  }

  @Nested
  @DisplayName("Helper Method Tests - getAllowedOperationParams")
  class GetAllowedOperationParamsTests {

    @Test
    @DisplayName("Should return configured parameters for CodeSystem lookup operation")
    void shouldReturnConfiguredParametersForCodeSystemLookup() {
      // GIVEN
      Set<String> allowedParams = Set.of("code", "system", "version");
      configProperties.setAllowedCodeSystemLookupParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedOperationParams(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, ConfigProperties.OPERATION_LOOKUP);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return configured parameters for ValueSet expand operation")
    void shouldReturnConfiguredParametersForValueSetExpand() {
      // GIVEN
      Set<String> allowedParams = Set.of("url", "filter", "count");
      configProperties.setAllowedValueSetExpandParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedOperationParams(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET, ConfigProperties.OPERATION_EXPAND);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return configured parameters for ConceptMap translate operation")
    void shouldReturnConfiguredParametersForConceptMapTranslate() {
      // GIVEN
      Set<String> allowedParams = Set.of("code", "system", "source", "target");
      configProperties.setAllowedConceptMapTranslateParams(allowedParams);

      // WHEN
      Set<String> result =
          configProperties.getAllowedOperationParams(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, ConfigProperties.OPERATION_TRANSLATE);

      // THEN
      assertThat(result).isEqualTo(allowedParams);
    }

    @Test
    @DisplayName("Should return empty set when resource type is null")
    void shouldReturnEmptySetWhenResourceTypeIsNull() {
      // GIVEN
      String operation = ConfigProperties.OPERATION_LOOKUP;

      // WHEN
      Set<String> result = configProperties.getAllowedOperationParams(null, operation);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when operation is null")
    void shouldReturnEmptySetWhenOperationIsNull() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      // WHEN
      Set<String> result = configProperties.getAllowedOperationParams(resourceType, null);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set for unknown operations")
    void shouldReturnEmptySetForUnknownOperations() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String unknownOperation = "$unknown";

      // WHEN
      Set<String> result =
          configProperties.getAllowedOperationParams(resourceType, unknownOperation);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set for unknown resource types")
    void shouldReturnEmptySetForUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "Patient";
      String operation = ConfigProperties.OPERATION_LOOKUP;

      // WHEN
      Set<String> result =
          configProperties.getAllowedOperationParams(unknownResourceType, operation);

      // THEN
      assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideAllOperationParameterCombinations")
    @DisplayName("Should return correct parameters for all operation combinations")
    void shouldReturnCorrectParametersForAllOperationCombinations(
        String resourceType, String operation, Set<String> expectedParams) {
      // GIVEN
      setupAllOperationParameters();

      // WHEN
      Set<String> result = configProperties.getAllowedOperationParams(resourceType, operation);

      // THEN
      assertThat(result).isEqualTo(expectedParams);
    }

    private void setupAllOperationParameters() {
      configProperties.setAllowedCodeSystemLookupParams(Set.of("code", "system"));
      configProperties.setAllowedCodeSystemValidateCodeParams(Set.of("code", "display"));
      configProperties.setAllowedCodeSystemFindMatchesParams(Set.of("system", "property"));
      configProperties.setAllowedCodeSystemSubsumesParams(Set.of("codeA", "codeB"));
      configProperties.setAllowedValueSetExpandParams(Set.of("url", "filter"));
      configProperties.setAllowedValueSetValidateCodeParams(Set.of("code", "system"));
      configProperties.setAllowedConceptMapTranslateParams(Set.of("code", "source"));
      configProperties.setAllowedConceptMapClosureParams(Set.of("name", "concept"));
    }

    private static Stream<Arguments> provideAllOperationParameterCombinations() {
      return Stream.of(
          // CodeSystem operations
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
              ConfigProperties.OPERATION_LOOKUP,
              Set.of("code", "system")),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
              ConfigProperties.OPERATION_VALIDATE_CODE,
              Set.of("code", "display")),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
              ConfigProperties.OPERATION_FIND_MATCHES,
              Set.of("system", "property")),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
              ConfigProperties.OPERATION_SUBSUMES,
              Set.of("codeA", "codeB")),
          // ValueSet operations
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET,
              ConfigProperties.OPERATION_EXPAND,
              Set.of("url", "filter")),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET,
              ConfigProperties.OPERATION_VALIDATE_CODE,
              Set.of("code", "system")),
          // ConceptMap operations
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP,
              ConfigProperties.OPERATION_TRANSLATE,
              Set.of("code", "source")),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP,
              ConfigProperties.OPERATION_CLOSURE,
              Set.of("name", "concept")));
    }
  }

  @Nested
  @DisplayName("Integration Tests - Complete Validation")
  class CompleteValidationTests {

    @Test
    @DisplayName("Should pass validation with all valid settings")
    void shouldPassValidationWithAllValidSettings() throws IOException {
      // GIVEN
      Path workingDir = tempDir.resolve("working");
      Files.createDirectories(workingDir);

      configProperties.setBaseWorkingDir(workingDir.toString());
      configProperties.setSystemLanguage("en-US");
      configProperties.setCorsAllowedOrigins(List.of("https://example.com"));
      configProperties.setCorsAllowedMethods(List.of("GET", "POST"));
      configProperties.setRateLimitEnabled(true);
      configProperties.setRateLimitRequestsPerMinute(200);
      configProperties.setMaxParameterLength(1000);
      configProperties.setMaxRequestSizeBytes(5000);
      configProperties.setPagingProviderSize(2000);
      configProperties.setDefaultPageSize(50);
      configProperties.setMaximumPageSize(200);
      configProperties.setCapabilitiesStatus("draft");
      configProperties.setCapabilitiesSoftwareReleaseDate("2024-01-15T10:30:00Z");

      // WHEN & THEN
      assertThatCode(() -> configProperties.validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail validation with multiple invalid settings")
    void shouldFailValidationWithMultipleInvalidSettings() {
      // GIVEN
      configProperties.setBaseWorkingDir("/non/existent/path");
      configProperties.setSystemLanguage("invalid-lang");
      configProperties.setRateLimitEnabled(true);
      configProperties.setRateLimitRequestsPerMinute(0);

      // WHEN & THEN
      // Note: Validation stops at first error, so we only check that it throws
      assertThatThrownBy(() -> configProperties.validate())
          .isInstanceOf(InvalidConfigurationException.class);
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("Should have correct resource type constants")
    void shouldHaveCorrectResourceTypeConstants() {
      // WHEN & THEN
      assertThat(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM).isEqualTo("CodeSystem");
      assertThat(ConfigProperties.RESOURCE_TYPE_VALUE_SET).isEqualTo("ValueSet");
      assertThat(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP).isEqualTo("ConceptMap");
    }

    @Test
    @DisplayName("Should have correct operation constants")
    void shouldHaveCorrectOperationConstants() {
      // WHEN & THEN
      assertThat(ConfigProperties.OPERATION_CLOSURE).isEqualTo("$closure");
      assertThat(ConfigProperties.OPERATION_TRANSLATE).isEqualTo("$translate");
      assertThat(ConfigProperties.OPERATION_VALIDATE_CODE).isEqualTo("$validate-code");
      assertThat(ConfigProperties.OPERATION_EXPAND).isEqualTo("$expand");
      assertThat(ConfigProperties.OPERATION_SUBSUMES).isEqualTo("$subsumes");
      assertThat(ConfigProperties.OPERATION_FIND_MATCHES).isEqualTo("$find-matches");
      assertThat(ConfigProperties.OPERATION_LOOKUP).isEqualTo("$lookup");
    }
  }

  @Nested
  @DisplayName("Getters and Setters Tests")
  class GettersAndSettersTests {

    @Test
    @DisplayName("Should get and set all core configuration properties")
    void shouldGetAndSetAllCoreConfigurationProperties() {
      // GIVEN & WHEN
      configProperties.setBaseWorkingDir("/test/path");
      configProperties.setSystemLanguage("fr-FR");

      // THEN
      assertThat(configProperties.getBaseWorkingDir()).isEqualTo("/test/path");
      assertThat(configProperties.getSystemLanguage()).isEqualTo("fr-FR");
    }

    @Test
    @DisplayName("Should get and set all security configuration properties")
    void shouldGetAndSetAllSecurityConfigurationProperties() {
      // GIVEN & WHEN
      List<String> origins = List.of("https://example.com");
      List<String> methods = List.of("GET", "POST", "PUT");
      List<String> headers = List.of(CONTENT_TYPE, AUTHORIZATION);
      List<String> exposedHeaders = List.of(LOCATION);

      configProperties.setCorsAllowedOrigins(origins);
      configProperties.setCorsAllowedMethods(methods);
      configProperties.setCorsAllowedHeaders(headers);
      configProperties.setCorsAllowCredentials(false);
      configProperties.setCorsExposedHeaders(exposedHeaders);
      configProperties.setRateLimitEnabled(true);
      configProperties.setRateLimitRequestsPerMinute(150);
      configProperties.setMaxParameterLength(600);
      configProperties.setMaxRequestSizeBytes(10485760);

      // THEN
      assertThat(configProperties.getCorsAllowedOrigins()).isEqualTo(origins);
      assertThat(configProperties.getCorsAllowedMethods()).isEqualTo(methods);
      assertThat(configProperties.getCorsAllowedHeaders()).isEqualTo(headers);
      assertThat(configProperties.isCorsAllowCredentials()).isFalse();
      assertThat(configProperties.getCorsExposedHeaders()).isEqualTo(exposedHeaders);
      assertThat(configProperties.isRateLimitEnabled()).isTrue();
      assertThat(configProperties.getRateLimitRequestsPerMinute()).isEqualTo(150);
      assertThat(configProperties.getMaxParameterLength()).isEqualTo(600);
      assertThat(configProperties.getMaxRequestSizeBytes()).isEqualTo(10485760);
    }

    @Test
    @DisplayName("Should get and set all paging configuration properties")
    void shouldGetAndSetAllPagingConfigurationProperties() {
      // GIVEN & WHEN
      configProperties.setPagingProviderSize(2000);
      configProperties.setDefaultPageSize(50);
      configProperties.setMaximumPageSize(200);

      // THEN
      assertThat(configProperties.getPagingProviderSize()).isEqualTo(2000);
      assertThat(configProperties.getDefaultPageSize()).isEqualTo(50);
      assertThat(configProperties.getMaximumPageSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should get and set system-level batch configuration")
    void shouldGetAndSetSystemLevelBatchConfiguration() {
      // GIVEN & WHEN
      configProperties.setSystemLevelBatchEnabled(false);
      configProperties.setSystemLevelBatchMaxBatchSize(250);

      // THEN
      assertThat(configProperties.isSystemLevelBatchEnabled()).isFalse();
      assertThat(configProperties.getSystemLevelBatchMaxBatchSize()).isEqualTo(250);
    }

    @Test
    @DisplayName("Should get and set all capabilities configuration properties")
    void shouldGetAndSetAllCapabilitiesConfigurationProperties() {
      // GIVEN & WHEN
      configProperties.setCapabilitiesName("CustomServer");
      configProperties.setCapabilitiesTitle("Custom Title");
      configProperties.setCapabilitiesDescription("Custom Description");
      configProperties.setCapabilitiesPublisher("Custom Publisher");
      configProperties.setCapabilitiesStatus("draft");
      configProperties.setCapabilitiesExperimental(false);
      configProperties.setCapabilitiesSoftwareName("CustomSoftware");
      configProperties.setCapabilitiesSoftwareVersion("2.0.0");
      configProperties.setCapabilitiesSoftwareReleaseDate("2024-02-01");
      configProperties.setCapabilitiesRestSecurityDescription("Custom security");

      // THEN
      assertThat(configProperties.getCapabilitiesName()).isEqualTo("CustomServer");
      assertThat(configProperties.getCapabilitiesTitle()).isEqualTo("Custom Title");
      assertThat(configProperties.getCapabilitiesDescription()).isEqualTo("Custom Description");
      assertThat(configProperties.getCapabilitiesPublisher()).isEqualTo("Custom Publisher");
      assertThat(configProperties.getCapabilitiesStatus()).isEqualTo("draft");
      assertThat(configProperties.isCapabilitiesExperimental()).isFalse();
      assertThat(configProperties.getCapabilitiesSoftwareName()).isEqualTo("CustomSoftware");
      assertThat(configProperties.getCapabilitiesSoftwareVersion()).isEqualTo("2.0.0");
      assertThat(configProperties.getCapabilitiesSoftwareReleaseDate()).isEqualTo("2024-02-01");
      assertThat(configProperties.getCapabilitiesRestSecurityDescription())
          .isEqualTo("Custom security");
    }

    @Test
    @DisplayName("Should get and set all operation enabled flags")
    void shouldGetAndSetAllOperationEnabledFlags() {
      // GIVEN & WHEN
      configProperties.setCodeSystemReadEnabled(false);
      configProperties.setCodeSystemSearchEnabled(true);
      configProperties.setCodeSystemLookupEnabled(false);
      configProperties.setCodeSystemValidateCodeEnabled(true);
      configProperties.setCodeSystemFindMatchesEnabled(false);
      configProperties.setCodeSystemSubsumesEnabled(true);

      configProperties.setValueSetReadEnabled(true);
      configProperties.setValueSetSearchEnabled(false);
      configProperties.setValueSetExpandEnabled(true);
      configProperties.setValueSetValidateCodeEnabled(false);

      configProperties.setConceptMapReadEnabled(false);
      configProperties.setConceptMapSearchEnabled(true);
      configProperties.setConceptMapTranslateEnabled(false);
      configProperties.setConceptMapClosureEnabled(true);

      // THEN
      assertThat(configProperties.isCodeSystemReadEnabled()).isFalse();
      assertThat(configProperties.isCodeSystemSearchEnabled()).isTrue();
      assertThat(configProperties.isCodeSystemLookupEnabled()).isFalse();
      assertThat(configProperties.isCodeSystemValidateCodeEnabled()).isTrue();
      assertThat(configProperties.isCodeSystemFindMatchesEnabled()).isFalse();
      assertThat(configProperties.isCodeSystemSubsumesEnabled()).isTrue();

      assertThat(configProperties.isValueSetReadEnabled()).isTrue();
      assertThat(configProperties.isValueSetSearchEnabled()).isFalse();
      assertThat(configProperties.isValueSetExpandEnabled()).isTrue();
      assertThat(configProperties.isValueSetValidateCodeEnabled()).isFalse();

      assertThat(configProperties.isConceptMapReadEnabled()).isFalse();
      assertThat(configProperties.isConceptMapSearchEnabled()).isTrue();
      assertThat(configProperties.isConceptMapTranslateEnabled()).isFalse();
      assertThat(configProperties.isConceptMapClosureEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should get and set all allowed parameter sets")
    void shouldGetAndSetAllAllowedParameterSets() {
      // GIVEN & WHEN
      Set<String> codeSystemSearchParams = Set.of("url", "code");
      Set<String> codeSystemLookupParams = Set.of("code", "system");
      Set<String> valueSetSearchParams = Set.of("url", "name");
      Set<String> valueSetExpandParams = Set.of("url", "filter");
      Set<String> conceptMapSearchParams = Set.of("url", "source");
      Set<String> conceptMapTranslateParams = Set.of("code", "target");

      configProperties.setAllowedCodeSystemSearchParams(codeSystemSearchParams);
      configProperties.setAllowedCodeSystemLookupParams(codeSystemLookupParams);
      configProperties.setAllowedValueSetSearchParams(valueSetSearchParams);
      configProperties.setAllowedValueSetExpandParams(valueSetExpandParams);
      configProperties.setAllowedConceptMapSearchParams(conceptMapSearchParams);
      configProperties.setAllowedConceptMapTranslateParams(conceptMapTranslateParams);

      // THEN
      assertThat(configProperties.getAllowedCodeSystemSearchParams())
          .isEqualTo(codeSystemSearchParams);
      assertThat(configProperties.getAllowedCodeSystemLookupParams())
          .isEqualTo(codeSystemLookupParams);
      assertThat(configProperties.getAllowedValueSetSearchParams()).isEqualTo(valueSetSearchParams);
      assertThat(configProperties.getAllowedValueSetExpandParams()).isEqualTo(valueSetExpandParams);
      assertThat(configProperties.getAllowedConceptMapSearchParams())
          .isEqualTo(conceptMapSearchParams);
      assertThat(configProperties.getAllowedConceptMapTranslateParams())
          .isEqualTo(conceptMapTranslateParams);
    }
  }
}
