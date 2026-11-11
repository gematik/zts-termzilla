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

import ca.uhn.fhir.parser.DataFormatException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Central configuration properties for the terminology server, injected from Spring property
 * sources ({@code application.properties}, environment variables, system properties).
 *
 * <p>All properties are validated at startup via {@link #validate()} to ensure the server operates
 * with a consistent and valid configuration.
 *
 * @see InvalidConfigurationException
 */
@Configuration
@Getter
@Setter
@Slf4j
public class ConfigProperties {

  public static final String RESOURCE_TYPE_CODE_SYSTEM = "CodeSystem";
  public static final String RESOURCE_TYPE_VALUE_SET = "ValueSet";
  public static final String RESOURCE_TYPE_CONCEPT_MAP = "ConceptMap";
  public static final String OPERATION_CLOSURE = "$closure";
  public static final String OPERATION_TRANSLATE = "$translate";
  public static final String OPERATION_VALIDATE_CODE = "$validate-code";
  public static final String OPERATION_EXPAND = "$expand";
  public static final String OPERATION_SUBSUMES = "$subsumes";
  public static final String OPERATION_FIND_MATCHES = "$find-matches";
  public static final String OPERATION_LOOKUP = "$lookup";

  // ========================================
  // Core Configuration
  // ========================================

  /** Version of the terminology server, used for logging and in the CapabilityStatement. */
  @Value("${core.version}")
  private String version;

  /** Base directory for indexes and temporary files. Must exist and be writable. */
  @Value("${core.baseWorkingDir}")
  private String baseWorkingDir;

  /** BCP 47 language code used as the server-wide default language. Defaults to {@code de-DE}. */
  @Value("${core.systemLanguage:de-DE}")
  private String systemLanguage;

  /**
   * Determines the mode resources can be retrieved in searches. When set to {@code FULL}, all
   * resource content is available for retrieval and included in search results. When set to {@code
   * STRIPPED}, only metadata is available for retrieval and included in search results, while the
   * actual content of resources is omitted. This can be used to improve performance and reduce
   * bandwidth usage when clients only need metadata (e.g., for search result listings) but not the
   * full resource content. This setting has impact on the behaviour of search operations (if set to
   * {@code STRIPPED}, search can only be used if _summary or _elements parameters are used).
   */
  @Value("${core.resourceRetrievalMode:STRIPPED}")
  private ResourceRetrievalMode resourceRetrievalMode;

  // ========================================
  // Security Configuration
  // ========================================

  /** List of allowed CORS origins. */
  @Value("${core.security.cors.allowedOrigins:}")
  private List<String> corsAllowedOrigins;

  /** List of allowed HTTP methods for CORS. */
  @Value("${core.security.cors.allowedMethods:GET,POST,OPTIONS}")
  private List<String> corsAllowedMethods;

  /** List of allowed headers for CORS. */
  @Value("${core.security.cors.allowedHeaders:*}")
  private List<String> corsAllowedHeaders;

  /** Whether credentials are allowed in CORS requests. */
  @Value("${core.security.cors.allowCredentials:true}")
  private boolean corsAllowCredentials;

  /** List of headers to expose in CORS responses. */
  @Value("${core.security.cors.exposedHeaders:}")
  private List<String> corsExposedHeaders;

  /** Whether rate limiting is enabled. */
  @Value("${core.security.rateLimit.enabled:false}")
  private boolean rateLimitEnabled;

  /** Maximum number of requests allowed per minute per client. */
  @Value("${core.security.rateLimit.requestsPerMinute:100}")
  private int rateLimitRequestsPerMinute;

  /** Maximum allowed query parameter length. */
  @Value("${core.security.request.maxParameterLength:1000}")
  private int maxParameterLength;

  /** Maximum allowed request body size in bytes. */
  @Value("${core.security.request.maxRequestSizeBytes:10485760}")
  private int maxRequestSizeBytes;

  // ========================================
  // Caching Configuration
  // ========================================

  /**
   * Whether {@code Cache-Control} response headers are enabled. When {@code false}, the server
   * sends {@code Cache-Control: no-store} on every response.
   */
  @Value("${core.caching.enabled:true}")
  private boolean cachingEnabled;

  /** Client cache lifetime in seconds ({@code max-age} directive). */
  @Value("${core.caching.maxAge:300}")
  private int cachingMaxAge;

  /**
   * Shared cache lifetime in seconds ({@code s-maxage} directive for nginx/CDN). Set to {@code -1}
   * to omit the directive; shared caches then fall back to {@code max-age}.
   */
  @Value("${core.caching.sharedMaxAge:-1}")
  private int cachingSharedMaxAge;

  /** Whether the {@code must-revalidate} directive is included in {@code Cache-Control} headers. */
  @Value("${core.caching.mustRevalidate:true}")
  private boolean cachingMustRevalidate;

  /**
   * Seconds a stale response may be served while revalidation happens in the background ({@code
   * stale-while-revalidate} directive). Effective for browser clients; nginx ignores this directive
   * from upstream headers. Set to {@code -1} to omit the directive.
   */
  @Value("${core.caching.staleWhileRevalidate:-1}")
  private int cachingStaleWhileRevalidate;

  /**
   * When {@code true}, the server ignores incoming {@code Accept} and {@code Accept-Language}
   * headers and uses its configured defaults instead. Eliminates the need for {@code Vary: Accept,
   * Accept-Language} and improves CDN cache efficiency.
   */
  @Value("${core.caching.cdnOptimized:false}")
  private boolean cachingCdnOptimized;

  // ========================================
  // Paging Configuration
  // ========================================

  /** Size of the internal paging provider buffer. */
  @Value("${core.paging.pagingProviderSize:1000}")
  private int pagingProviderSize;

  /** Default page size when not specified in the request. */
  @Value("${core.paging.defaultPageSize:20}")
  private int defaultPageSize;

  /** Maximum allowed page size in a single request. */
  @Value("${core.paging.maximumPageSize:100}")
  private int maximumPageSize;

  // ========================================
  // Capabilities Configuration
  // ========================================

  /** Name of the terminology server as exposed in the CapabilityStatement. */
  @Value("${core.capabilities.name:TERMzillaTerminologyServices}")
  private String capabilitiesName;

  /** Human-readable title of the terminology server. */
  @Value("${core.capabilities.title:TERMzilla Terminology Services}")
  private String capabilitiesTitle;

  /** Description of the terminology server's purpose and functionality. */
  @Value("${core.capabilities.description:Terminology Service for FHIR Terminologies}")
  private String capabilitiesDescription;

  /** Publisher of the terminology server. */
  @Value("${core.capabilities.publisher:Fraunhofer FOKUS}")
  private String capabilitiesPublisher;

  @Value("${core.capabilities.advertisedUrl}")
  private String capabilitiesAdvertisedUrl;

  /** Publication status of the CapabilityStatement (e.g., active, draft). */
  @Value("${core.capabilities.status:active}")
  private String capabilitiesStatus;

  /** Whether this is an experimental server instance. */
  @Value("${core.capabilities.experimental:true}")
  private boolean capabilitiesExperimental;

  /** Name of the software implementing the terminology server. */
  @Value("${core.capabilities.softwareName:TERMzilla}")
  private String capabilitiesSoftwareName;

  /** Version of the software. */
  @Value("${core.capabilities.softwareVersion:0.1.0}")
  private String capabilitiesSoftwareVersion;

  /** Release date of the software version (ISO 8601 format). */
  @Value("${core.capabilities.softwareReleaseDate:2023-10-01}")
  private String capabilitiesSoftwareReleaseDate;

  /** Description of the security mechanisms in place. */
  @Value(
      "${core.capabilities.restSecurityDescription:No security mechanisms are enabled for this instance}")
  private String capabilitiesRestSecurityDescription;

  // ========================================
  // System Level Operations
  // ========================================

  /** Whether system-level batch processing is enabled. */
  @Value("${core.operations.systemLevel.batch.enabled:true}")
  private boolean systemLevelBatchEnabled;

  /** Maximum number of entries allowed in one batch request. */
  @Value("${core.operations.systemLevel.batch.maxBatchSize:1000}")
  private int systemLevelBatchMaxBatchSize;

  // ========================================
  // CodeSystem Operations
  // ========================================

  @Value("${core.operations.codesystem.read.enabled:true}")
  private boolean codeSystemReadEnabled;

  @Value("${core.operations.codesystem.search.enabled:true}")
  private boolean codeSystemSearchEnabled;

  @Value("${core.operations.codesystem.lookup.enabled:true}")
  private boolean codeSystemLookupEnabled;

  @Value("${core.operations.codesystem.validateCode.enabled:true}")
  private boolean codeSystemValidateCodeEnabled;

  @Value("${core.operations.codesystem.findMatches.enabled:true}")
  private boolean codeSystemFindMatchesEnabled;

  @Value("${core.operations.codesystem.subsumes.enabled:true}")
  private boolean codeSystemSubsumesEnabled;

  @Value("${core.operations.codesystem.search.allowedParams:}")
  private Set<String> allowedCodeSystemSearchParams;

  @Value("${core.operations.codesystem.search.defaultSort:url,version}")
  private Set<String> codeSystemDefaultSearchSort;

  @Value("${core.operations.codesystem.lookup.allowedParams:}")
  private Set<String> allowedCodeSystemLookupParams;

  @Value("${core.operations.codesystem.validateCode.allowedParams:}")
  private Set<String> allowedCodeSystemValidateCodeParams;

  @Value("${core.operations.codesystem.findMatches.allowedParams:}")
  private Set<String> allowedCodeSystemFindMatchesParams;

  @Value("${core.operations.codesystem.subsumes.allowedParams:}")
  private Set<String> allowedCodeSystemSubsumesParams;

  // ========================================
  // ValueSet Operations
  // ========================================

  @Value("${core.operations.valueset.read.enabled:true}")
  private boolean valueSetReadEnabled;

  @Value("${core.operations.valueset.search.enabled:true}")
  private boolean valueSetSearchEnabled;

  @Value("${core.operations.valueset.expand.enabled:true}")
  private boolean valueSetExpandEnabled;

  @Value("${core.operations.valueset.validateCode.enabled:true}")
  private boolean valueSetValidateCodeEnabled;

  @Value("${core.operations.valueset.search.allowedParams:}")
  private Set<String> allowedValueSetSearchParams;

  @Value("${core.operations.valueset.search.defaultSort:url,version}")
  private Set<String> valueSetDefaultSearchSort;

  @Value("${core.operations.valueset.expand.allowedParams:}")
  private Set<String> allowedValueSetExpandParams;

  @Value("${core.operations.valueset.validateCode.allowedParams:}")
  private Set<String> allowedValueSetValidateCodeParams;

  // ========================================
  // ConceptMap Operations
  // ========================================

  @Value("${core.operations.conceptmap.read.enabled:true}")
  private boolean conceptMapReadEnabled;

  @Value("${core.operations.conceptmap.search.enabled:true}")
  private boolean conceptMapSearchEnabled;

  @Value("${core.operations.conceptmap.translate.enabled:true}")
  private boolean conceptMapTranslateEnabled;

  @Value("${core.operations.conceptmap.closure.enabled:true}")
  private boolean conceptMapClosureEnabled;

  @Value("${core.operations.conceptmap.search.allowedParams:}")
  private Set<String> allowedConceptMapSearchParams;

  @Value("${core.operations.conceptmap.search.defaultSort:url,version}")
  private Set<String> conceptMapDefaultSearchSort;

  @Value("${core.operations.conceptmap.translate.allowedParams:}")
  private Set<String> allowedConceptMapTranslateParams;

  @Value("${core.operations.conceptmap.closure.allowedParams:}")
  private Set<String> allowedConceptMapClosureParams;

  // ========================================
  // Validation
  // ========================================

  /**
   * Validates all configuration properties.
   *
   * @throws InvalidConfigurationException if any property is missing, invalid, or refers to a
   *     non-existent or non-writable directory
   */
  public void validate() throws InvalidConfigurationException {
    log.info("Validating configuration properties ...");

    validateVersion();
    validateBaseWorkingDir();
    validateSystemLanguage();
    validateSecurityConfiguration();
    validateCachingConfiguration();
    validatePagingConfiguration();
    validateSystemLevelOperationsConfiguration();
    validateCapabilitiesConfiguration();
  }

  /**
   * Validates that {@link #version} is configured and not empty.
   *
   * @throws InvalidConfigurationException if version is missing or empty
   */
  private void validateVersion() throws InvalidConfigurationException {
    if (version == null || version.isEmpty()) {
      String message = "Version must be configured and cannot be empty.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates that {@link #systemLanguage} is a recognized BCP 47 language code supported by {@link
   * LanguageRegistry}.
   *
   * @throws InvalidConfigurationException if the language code is not supported
   */
  private void validateSystemLanguage() throws InvalidConfigurationException {
    if (!LanguageRegistry.isSupported(systemLanguage)) {
      String message = "Configured systemLanguage is not supported: " + systemLanguage;
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates that {@link #baseWorkingDir} is configured, points to an existing directory, and is
   * writable.
   *
   * @throws InvalidConfigurationException if the directory is not configured, does not exist, is
   *     not a directory, or is not writable
   */
  private void validateBaseWorkingDir() throws InvalidConfigurationException {
    if (baseWorkingDir == null || baseWorkingDir.isEmpty()) {
      String message = "Base working directory is not configured.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    Path basePath = Path.of(baseWorkingDir);

    if (!basePath.toFile().exists() || !basePath.toFile().isDirectory()) {
      String message =
          "Base working directory does not exist or is not a directory: " + baseWorkingDir;
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (!basePath.toFile().canWrite()) {
      String message = "Base working directory is not writable: " + baseWorkingDir;
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates security-related configuration properties.
   *
   * @throws InvalidConfigurationException if security configuration is invalid
   */
  private void validateSecurityConfiguration() throws InvalidConfigurationException {

    // Validate CORS allowed methods contains only valid HTTP methods
    List<String> validHttpMethods =
        Arrays.stream(HttpMethod.values()).map(HttpMethod::name).toList();
    for (String method : corsAllowedMethods) {
      if (!validHttpMethods.contains(method.toUpperCase())) {
        String message =
            "Invalid CORS allowed method: " + method + ". Must be one of: " + validHttpMethods;
        log.error(message);
        throw new InvalidConfigurationException(message);
      }
    }

    // Validate origins to be valid URLs (basic validation)
    for (String origin : corsAllowedOrigins) {
      try {
        URI.create(origin).toURL();
      } catch (IllegalArgumentException | MalformedURLException _) {
        String message = "Invalid CORS allowed origin: " + origin + ". Must be a valid URL.";
        log.error(message);
        throw new InvalidConfigurationException(message);
      }
    }

    // Validate rate limiting configuration
    if (rateLimitEnabled && rateLimitRequestsPerMinute <= 0) {
      String message =
          "Rate limit requests per minute must be greater than 0 when rate limiting is enabled.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    // Allow 0 for maxParameterLength (means disabled)
    if (maxParameterLength < 0) {
      String message = "Query parameter max length must be non-negative (0 = disabled).";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    // Allow 0 for maxRequestSizeBytes (means disabled)
    if (maxRequestSizeBytes < 0) {
      String message = "Maximum request size in bytes must be non-negative (0 = disabled).";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates caching-related configuration properties.
   *
   * @throws InvalidConfigurationException if caching configuration is invalid
   */
  private void validateCachingConfiguration() throws InvalidConfigurationException {
    if (cachingMaxAge < 0) {
      String message = "Caching max-age must be non-negative.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (cachingSharedMaxAge < -1) {
      String message = "Caching shared max-age must be -1 (disabled) or non-negative.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (cachingStaleWhileRevalidate < -1) {
      String message = "Caching stale-while-revalidate must be -1 (disabled) or non-negative.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (cachingMustRevalidate && cachingStaleWhileRevalidate >= 0) {
      String message =
          "must-revalidate and stale-while-revalidate are mutually exclusive: must-revalidate "
              + "forbids stale responses while stale-while-revalidate allows them.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates paging-related configuration properties.
   *
   * @throws InvalidConfigurationException if paging configuration is invalid
   */
  private void validatePagingConfiguration() throws InvalidConfigurationException {
    if (pagingProviderSize <= 0) {
      String message = "Paging provider size must be greater than 0.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (defaultPageSize <= 0) {
      String message = "Default page size must be greater than 0.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (maximumPageSize <= 0) {
      String message = "Maximum page size must be greater than 0.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    if (defaultPageSize > maximumPageSize) {
      String message = "Default page size cannot be greater than maximum page size.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates system-level operation configuration properties.
   *
   * @throws InvalidConfigurationException if system-level operation configuration is invalid
   */
  private void validateSystemLevelOperationsConfiguration() throws InvalidConfigurationException {
    if (systemLevelBatchEnabled && systemLevelBatchMaxBatchSize <= 0) {
      String message = "System-level batch max size must be greater than 0 when batch is enabled.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  /**
   * Validates capabilities-related configuration properties.
   *
   * @throws InvalidConfigurationException if capabilities configuration is invalid
   */
  private void validateCapabilitiesConfiguration() throws InvalidConfigurationException {

    // status must be configured and must be a valid PublicationStatus value
    if (capabilitiesStatus == null || capabilitiesStatus.isEmpty()) {
      String message = "Capabilities status must be configured.";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    try {
      Enumerations.PublicationStatus.fromCode(capabilitiesStatus.toLowerCase());
    } catch (FHIRException _) {
      String validStatuses =
          Arrays.stream(Enumerations.PublicationStatus.values())
              .map(Enumerations.PublicationStatus::toCode)
              .toList()
              .toString();
      String message =
          "Invalid capabilities status: "
              + capabilitiesStatus
              + " must be one of: "
              + validStatuses;
      log.error(message);
      throw new InvalidConfigurationException(message);
    }

    // software release date must be in valid ISO 8601 format (e.g., 2023-10-01 or
    // 2023-10-01T12:00:00Z)
    try {
      new DateTimeType(capabilitiesSoftwareReleaseDate);
    } catch (DataFormatException _) {
      String message =
          "Invalid software release date format: "
              + capabilitiesSoftwareReleaseDate
              + " must be in ISO 8601 format (e.g., 2023-10-01 or 2023-10-01T12:00:00Z)";
      log.error(message);
      throw new InvalidConfigurationException(message);
    }
  }

  // ========================================
  // Helper Methods
  // ========================================

  /**
   * Check if READ operation is enabled for the given resource type.
   *
   * @param resourceType the FHIR resource type
   * @return {@code true} if READ is enabled, {@code false} otherwise
   */
  public boolean isReadEnabledForResource(String resourceType) {
    return switch (resourceType) {
      case RESOURCE_TYPE_CODE_SYSTEM -> codeSystemReadEnabled;
      case RESOURCE_TYPE_VALUE_SET -> valueSetReadEnabled;
      case RESOURCE_TYPE_CONCEPT_MAP -> conceptMapReadEnabled;
      default -> false;
    };
  }

  /**
   * Check if SEARCH operation is enabled for the given resource type.
   *
   * @param resourceType the FHIR resource type
   * @return {@code true} if SEARCH is enabled, {@code false} otherwise
   */
  public boolean isSearchEnabledForResource(String resourceType) {
    return switch (resourceType) {
      case RESOURCE_TYPE_CODE_SYSTEM -> codeSystemSearchEnabled;
      case RESOURCE_TYPE_VALUE_SET -> valueSetSearchEnabled;
      case RESOURCE_TYPE_CONCEPT_MAP -> conceptMapSearchEnabled;
      default -> false;
    };
  }

  /**
   * Get allowed search parameters for the given resource type.
   *
   * @param resourceType the FHIR resource type
   * @return a set of allowed search parameter names, or {@code null} if not applicable
   */
  public Set<String> getAllowedSearchParamsForResource(String resourceType) {
    return switch (resourceType) {
      case RESOURCE_TYPE_CODE_SYSTEM -> allowedCodeSystemSearchParams;
      case RESOURCE_TYPE_VALUE_SET -> allowedValueSetSearchParams;
      case RESOURCE_TYPE_CONCEPT_MAP -> allowedConceptMapSearchParams;
      default -> null;
    };
  }

  /**
   * Check if a specific operation is enabled for a resource type.
   *
   * @param resourceType the FHIR resource type
   * @param operation the operation name (e.g., {@code $lookup})
   * @return {@code true} if the operation is enabled, {@code false} otherwise
   */
  public boolean isOperationEnabled(String resourceType, String operation) {
    if (resourceType == null || operation == null) {
      return false;
    }

    return switch (resourceType) {
      case RESOURCE_TYPE_CODE_SYSTEM -> isCodeSystemOperationEnabled(operation);
      case RESOURCE_TYPE_VALUE_SET -> isValueSetOperationEnabled(operation);
      case RESOURCE_TYPE_CONCEPT_MAP -> isConceptMapOperationEnabled(operation);
      default -> false;
    };
  }

  /**
   * Get allowed parameters for a specific operation.
   *
   * @param resourceType the FHIR resource type
   * @param operation the operation name (e.g., {@code $lookup})
   * @return a set of allowed parameter names, or an empty set if not applicable
   */
  public Set<String> getAllowedOperationParams(String resourceType, String operation) {
    if (resourceType == null || operation == null) {
      return Collections.emptySet();
    }

    return switch (resourceType) {
      case RESOURCE_TYPE_CODE_SYSTEM -> getCodeSystemOperationParams(operation);
      case RESOURCE_TYPE_VALUE_SET -> getValueSetOperationParams(operation);
      case RESOURCE_TYPE_CONCEPT_MAP -> getConceptMapOperationParams(operation);
      default -> Collections.emptySet();
    };
  }

  // ========================================
  // Private Helper Methods
  // ========================================

  private boolean isCodeSystemOperationEnabled(String operation) {
    return switch (operation) {
      case OPERATION_LOOKUP -> codeSystemLookupEnabled;
      case OPERATION_VALIDATE_CODE -> codeSystemValidateCodeEnabled;
      case OPERATION_FIND_MATCHES -> codeSystemFindMatchesEnabled;
      case OPERATION_SUBSUMES -> codeSystemSubsumesEnabled;
      default -> false;
    };
  }

  private Set<String> getCodeSystemOperationParams(String operation) {
    return switch (operation) {
      case OPERATION_LOOKUP -> allowedCodeSystemLookupParams;
      case OPERATION_VALIDATE_CODE -> allowedCodeSystemValidateCodeParams;
      case OPERATION_FIND_MATCHES -> allowedCodeSystemFindMatchesParams;
      case OPERATION_SUBSUMES -> allowedCodeSystemSubsumesParams;
      default -> Collections.emptySet();
    };
  }

  private boolean isValueSetOperationEnabled(String operation) {
    return switch (operation) {
      case OPERATION_EXPAND -> valueSetExpandEnabled;
      case OPERATION_VALIDATE_CODE -> valueSetValidateCodeEnabled;
      default -> false;
    };
  }

  private Set<String> getValueSetOperationParams(String operation) {
    return switch (operation) {
      case OPERATION_EXPAND -> allowedValueSetExpandParams;
      case OPERATION_VALIDATE_CODE -> allowedValueSetValidateCodeParams;
      default -> Collections.emptySet();
    };
  }

  private boolean isConceptMapOperationEnabled(String operation) {
    return switch (operation) {
      case OPERATION_TRANSLATE -> conceptMapTranslateEnabled;
      case OPERATION_CLOSURE -> conceptMapClosureEnabled;
      default -> false;
    };
  }

  private Set<String> getConceptMapOperationParams(String operation) {
    return switch (operation) {
      case OPERATION_TRANSLATE -> allowedConceptMapTranslateParams;
      case OPERATION_CLOSURE -> allowedConceptMapClosureParams;
      default -> Collections.emptySet();
    };
  }
}
