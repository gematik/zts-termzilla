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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Downloads FHIR packages from configurable package registries. */
@Slf4j
public class PackageDownloader implements AutoCloseable {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String URL_PATH_SEPARATOR = "/";
  private static final String DEFAULT_API_KEY_HEADER = "X-API-Key";
  private static final String DEFAULT_BASE_URL = "https://terminologien.bfarm.de";
  private static final String PACKAGE_DOWNLOAD_PATH = "/packages";
  private static final String PACKAGE_CATALOG_PATH = "/packages/catalog";
  private static final String TOKEN_GENERATION_PATH = "/api/generate-token";

  private static final Set<String> CATALOG_WILDCARD_BLOCKLIST_BASE_URLS =
      Set.of(
          "https://packages.simplifier.net",
          "http://packages.fhir.org",
          "http://packages2.fhir.org");

  private static final Pattern PACKAGE_NAME_PATTERN =
      Pattern.compile("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*){1,10}$");
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?(\\+[a-zA-Z0-9.-]+)?$");

  private static final int DEFAULT_TIMEOUT_SECONDS = 120;
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final String baseUrl;
  private final AuthenticationMode authenticationMode;
  private final String basicAuthUsername;
  private final String basicAuthPassword;
  private final OkHttpClient httpClient;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String bearerToken;
  private final String apiKey;
  private final String apiKeyHeader;

  @Setter private String authToken;

  private final String packagePathPrefix;
  private final String packageCatalogPath;
  private final boolean skipDeprecatedUnlistedPackageVersions;

  /** Creates a new downloader with default timeout settings. */
  public PackageDownloader() {
    this(DEFAULT_TIMEOUT_SECONDS, DEFAULT_BASE_URL, AuthenticationMode.NONE, null, null);
  }

  /**
   * Creates a new downloader with a custom timeout.
   *
   * @param timeoutSeconds timeout for HTTP requests in seconds
   */
  public PackageDownloader(int timeoutSeconds) {
    this(timeoutSeconds, DEFAULT_BASE_URL, AuthenticationMode.NONE, null, null);
  }

  /**
   * Creates a new downloader with default timeout and custom registry base URL.
   *
   * @param baseUrl package registry base URL
   */
  public PackageDownloader(String baseUrl) {
    this(DEFAULT_TIMEOUT_SECONDS, baseUrl, AuthenticationMode.NONE, null, null);
  }

  /**
   * Creates a new downloader with custom timeout and custom registry base URL.
   *
   * @param timeoutSeconds timeout for HTTP requests in seconds
   * @param baseUrl package registry base URL
   */
  public PackageDownloader(int timeoutSeconds, String baseUrl) {
    this(timeoutSeconds, baseUrl, AuthenticationMode.NONE, null, null);
  }

  /**
   * Creates a new downloader with explicit authentication configuration.
   *
   * @param timeoutSeconds timeout for HTTP requests in seconds
   * @param baseUrl package registry base URL
   * @param authenticationMode authentication mode
   * @param basicAuthUsername username for {@link AuthenticationMode#BASIC_AUTH}
   * @param basicAuthPassword password for {@link AuthenticationMode#BASIC_AUTH}
   */
  public PackageDownloader(
      int timeoutSeconds,
      String baseUrl,
      AuthenticationMode authenticationMode,
      String basicAuthUsername,
      String basicAuthPassword) {
    this(
        timeoutSeconds,
        baseUrl,
        authenticationMode,
        new PackageDownloaderConfig(
            new PackageDownloaderAuthConfig(basicAuthUsername, basicAuthPassword, null, null, null),
            PackageDownloaderEndpointConfig.defaults()));
  }

  /**
   * Creates a new downloader with explicit authentication values and default endpoint paths.
   *
   * @param timeoutSeconds timeout for HTTP requests in seconds
   * @param baseUrl package registry base URL
   * @param authenticationMode authentication mode
   * @param authConfig authentication values
   */
  public PackageDownloader(
      int timeoutSeconds,
      String baseUrl,
      AuthenticationMode authenticationMode,
      PackageDownloaderAuthConfig authConfig) {
    this(
        timeoutSeconds,
        baseUrl,
        authenticationMode,
        new PackageDownloaderConfig(authConfig, PackageDownloaderEndpointConfig.defaults()));
  }

  /**
   * Creates a new downloader with explicit authentication and endpoint path configuration.
   *
   * @param timeoutSeconds timeout for HTTP requests in seconds
   * @param baseUrl package registry base URL
   * @param authenticationMode authentication mode
   * @param config combined authentication and endpoint settings
   */
  public PackageDownloader(
      int timeoutSeconds,
      String baseUrl,
      AuthenticationMode authenticationMode,
      PackageDownloaderConfig config) {
    PackageDownloaderConfig safeConfig =
        config == null ? PackageDownloaderConfig.defaults() : config;
    PackageDownloaderAuthConfig safeAuthConfig =
        safeConfig.authConfig() == null
            ? PackageDownloaderAuthConfig.none()
            : safeConfig.authConfig();
    PackageDownloaderEndpointConfig safeEndpointConfig =
        safeConfig.endpointConfig() == null
            ? PackageDownloaderEndpointConfig.defaults()
            : safeConfig.endpointConfig();

    this.baseUrl = normalizeBaseUrl(baseUrl);
    this.authenticationMode =
        authenticationMode == null ? AuthenticationMode.NONE : authenticationMode;
    this.basicAuthUsername = safeAuthConfig.basicAuthUsername();
    this.basicAuthPassword = safeAuthConfig.basicAuthPassword();
    this.bearerToken = safeAuthConfig.bearerToken();
    this.apiKey = safeAuthConfig.apiKey();
    String configuredApiKeyHeader = safeAuthConfig.apiKeyHeader();
    this.apiKeyHeader =
        configuredApiKeyHeader == null || configuredApiKeyHeader.isBlank()
            ? DEFAULT_API_KEY_HEADER
            : configuredApiKeyHeader;

    this.packagePathPrefix = normalizePackagePathPrefix(safeEndpointConfig.packagePathPrefix());
    this.packageCatalogPath =
        normalizeAbsolutePath(safeEndpointConfig.packageCatalogPath(), PACKAGE_CATALOG_PATH);
    this.skipDeprecatedUnlistedPackageVersions = safeConfig.skipDeprecatedUnlistedPackageVersions();
    validateAuthenticationConfiguration();

    this.httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(new UserAgentInterceptor())
            .build();

    String effectivePackagePathPrefix =
        this.packagePathPrefix.isEmpty() ? URL_PATH_SEPARATOR : this.packagePathPrefix;
    log.info(
        "Package downloader endpoint configuration: baseUrl='{}', packagePathPrefix='{}', catalogPath='{}', skipDeprecatedUnlistedPackageVersions={}",
        this.baseUrl,
        effectivePackagePathPrefix,
        this.packageCatalogPath,
        this.skipDeprecatedUnlistedPackageVersions);
  }

  /**
   * Downloads a FHIR package to the provided target directory.
   *
   * @param packageName the package name
   * @param packageVersion the package version
   * @param targetDirectory target directory where the package will be written
   * @return path to the downloaded archive
   * @throws PackageDownloadException if download fails
   */
  public Path downloadPackage(String packageName, String packageVersion, String targetDirectory)
      throws PackageDownloadException {
    return downloadPackage(packageName, packageVersion, Paths.get(targetDirectory));
  }

  /**
   * Downloads a FHIR package to the provided target directory.
   *
   * @param packageName the package name
   * @param packageVersion the package version
   * @param targetDirectory target directory where the package will be written
   * @return path to the downloaded archive
   * @throws PackageDownloadException if download fails
   */
  public Path downloadPackage(String packageName, String packageVersion, Path targetDirectory)
      throws PackageDownloadException {

    validatePackageName(packageName);
    validatePackageVersion(packageVersion);
    ensureDirectoryExists(targetDirectory);

    JsonNode packageVersionNode = resolvePackageVersionNode(packageName, packageVersion);
    String expectedShaSum = resolveExpectedShaSum(packageVersionNode, packageName, packageVersion);

    if (authenticationMode == AuthenticationMode.BFARM_AUTO_TOKEN) {
      ensureTokenForProtectedPackage(packageName, packageVersion, packageVersionNode);
    }

    String fileName = String.format("%s-%s.tgz", packageName, packageVersion);
    Path targetFile = targetDirectory.resolve(fileName);

    if (Files.exists(targetFile)) {
      String existingFileShaSum = calculateFileSha1(targetFile);
      if (existingFileShaSum.equalsIgnoreCase(expectedShaSum)) {
        log.info(
            "Skipping download for {}@{} because existing file checksum matches metadata ({})",
            packageName,
            packageVersion,
            expectedShaSum);
        return targetFile;
      }

      log.warn(
          "Checksum mismatch for existing file {} (expected={}, actual={}). Re-downloading package.",
          targetFile,
          expectedShaSum,
          existingFileShaSum);
    }

    log.info("Downloading package {}@{} to {}", packageName, packageVersion, targetFile);

    String downloadUrl = buildPackageDownloadUrl(packageName, packageVersion);
    Path downloadedFile = downloadPackageFromUrl(downloadUrl, targetFile);
    verifyFileSha1(downloadedFile, expectedShaSum, packageName, packageVersion);
    return downloadedFile;
  }

  /**
   * Resolves configured package name/version entries and expands wildcard entries via catalog
   * lookup.
   *
   * @param configuredPackageEntries configured entries like {@code package.name@x.y.z} or {@code *}
   * @return resolved package name/version entries in deterministic order without duplicates
   * @throws PackageDownloadException if wildcard expansion cannot be resolved
   */
  public List<String> resolvePackageNameVersionEntries(List<String> configuredPackageEntries)
      throws PackageDownloadException {

    Objects.requireNonNull(configuredPackageEntries, "configuredPackageEntries cannot be null");

    Set<String> resolvedPackageEntries = new LinkedHashSet<>();
    boolean wildcardRequested = false;

    for (String packageEntry : configuredPackageEntries) {
      if (packageEntry == null || packageEntry.isBlank()) {
        continue;
      }

      String trimmedPackageEntry = packageEntry.trim();
      if ("*".equals(trimmedPackageEntry)) {
        wildcardRequested = true;
      } else {
        resolvedPackageEntries.add(trimmedPackageEntry);
      }
    }

    if (wildcardRequested) {
      ensureCatalogAndWildcardSupported();
      resolvedPackageEntries.addAll(resolvePackageNameVersionEntriesFromCatalog());
    }

    if (!skipDeprecatedUnlistedPackageVersions) {
      return List.copyOf(resolvedPackageEntries);
    }

    return filterDeprecatedAndUnlistedPackageEntries(resolvedPackageEntries);
  }

  /**
   * Downloads a package using an explicit authentication token.
   *
   * @param packageName the package name
   * @param packageVersion the package version
   * @param targetDirectory target directory where the package will be written
   * @param authToken token used for protected packages
   * @return path to the downloaded archive
   * @throws PackageDownloadException if download fails
   */
  public Path downloadPackage(
      String packageName, String packageVersion, Path targetDirectory, String authToken)
      throws PackageDownloadException {

    this.authToken = authToken;
    return downloadPackage(packageName, packageVersion, targetDirectory);
  }

  /**
   * Generates an authentication token for protected packages.
   *
   * @param packageNames package names requiring access
   * @return generated token
   * @throws PackageDownloadException if token generation fails
   */
  public String generateToken(List<String> packageNames) throws PackageDownloadException {
    log.info("Generating authentication token for packages: {}", packageNames);

    String tokenUrl = baseUrl + TOKEN_GENERATION_PATH;

    try {
      TokenRequest tokenRequest = new TokenRequest(packageNames);
      String requestBody = OBJECT_MAPPER.writeValueAsString(tokenRequest);

      Request request =
          new Request.Builder().url(tokenUrl).post(RequestBody.create(requestBody, JSON)).build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new PackageDownloadException(
              "Failed to generate authentication token. Status: " + response.code(),
              response.code());
        }

        ResponseBody body = response.body();
        if (body == null) {
          throw new PackageDownloadException(
              "Empty response body from token generation", response.code());
        }

        TokenResponse tokenResponse = OBJECT_MAPPER.readValue(body.string(), TokenResponse.class);
        this.authToken = tokenResponse.getToken();

        log.info("Successfully generated authentication token");
        return this.authToken;
      }

    } catch (IOException e) {
      throw new PackageDownloadException("Failed to generate authentication token", e);
    }
  }

  /**
   * Executes the package download request and writes the response body to a target file.
   *
   * @param url URL used for package download
   * @param targetFile file that receives the downloaded package archive
   * @return path to the downloaded file
   * @throws PackageDownloadException if the request fails or the payload cannot be written
   */
  private Path downloadPackageFromUrl(String url, Path targetFile) throws PackageDownloadException {
    Request.Builder requestBuilder = new Request.Builder().url(url).get();

    applyAuthentication(requestBuilder);

    Request request = requestBuilder.build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          throw new PackageDownloadException(
              String.format("Package not found: %s", url), response.code());
        } else if (response.code() == 401 || response.code() == 403) {
          throw new PackageDownloadException(
              "Authentication required or access denied. Package may be protected.",
              response.code());
        } else {
          throw new PackageDownloadException(
              String.format("Failed to download package. Status: %d", response.code()),
              response.code());
        }
      }

      ResponseBody body = response.body();
      if (body == null) {
        throw new PackageDownloadException(
            "Empty response body from package download", response.code());
      }

      try (InputStream inputStream = body.byteStream()) {
        Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
      }

      long fileSize = Files.size(targetFile);
      log.info("Successfully downloaded package to {} ({} bytes)", targetFile, fileSize);

      return targetFile;

    } catch (IOException e) {
      throw new PackageDownloadException("Failed to download package from " + url, e);
    }
  }

  /**
   * Applies the configured authentication headers to an outgoing request.
   *
   * @param requestBuilder builder to enrich with authentication headers
   * @throws PackageDownloadException if the configured authentication mode is unsupported
   */
  private void applyAuthentication(Request.Builder requestBuilder) throws PackageDownloadException {
    switch (authenticationMode) {
      case NONE:
        return;
      case BASIC_AUTH:
        String username =
            Objects.requireNonNull(basicAuthUsername, "Basic auth username cannot be null");
        String password =
            Objects.requireNonNull(basicAuthPassword, "Basic auth password cannot be null");
        String credentials = username + ":" + password;
        String encodedCredentials =
            Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        requestBuilder.addHeader(AUTHORIZATION_HEADER, "Basic " + encodedCredentials);
        return;
      case BFARM_AUTO_TOKEN:
        if (authToken != null && !authToken.isEmpty()) {
          requestBuilder.addHeader(AUTHORIZATION_HEADER, "Bearer " + authToken);
        }
        return;
      case BEARER_TOKEN:
        requestBuilder.addHeader(AUTHORIZATION_HEADER, "Bearer " + bearerToken);
        return;
      case API_KEY:
        requestBuilder.addHeader(apiKeyHeader, apiKey);
        return;
      default:
        throw new PackageDownloadException(
            "Unsupported authentication mode: " + authenticationMode);
    }
  }

  /** Validates that mode-specific authentication inputs are present. */
  private void validateAuthenticationConfiguration() {
    if (authenticationMode == AuthenticationMode.BASIC_AUTH) {
      if (basicAuthUsername == null || basicAuthUsername.isBlank()) {
        throw new IllegalArgumentException(
            "basicAuthUsername must be configured for BASIC_AUTH mode");
      }
      if (basicAuthPassword == null || basicAuthPassword.isBlank()) {
        throw new IllegalArgumentException(
            "basicAuthPassword must be configured for BASIC_AUTH mode");
      }
    }

    if (authenticationMode == AuthenticationMode.BEARER_TOKEN
        && (bearerToken == null || bearerToken.isBlank())) {
      throw new IllegalArgumentException("bearerToken must be configured for BEARER_TOKEN mode");
    }

    if (authenticationMode == AuthenticationMode.API_KEY && (apiKey == null || apiKey.isBlank())) {
      throw new IllegalArgumentException("apiKey must be configured for API_KEY mode");
    }
  }

  /**
   * Validates package name syntax before building remote URLs.
   *
   * @param packageName package name to validate
   * @throws PackageDownloadException if the package name is invalid
   */
  private void validatePackageName(String packageName) throws PackageDownloadException {
    Objects.requireNonNull(packageName, "Package name cannot be null");

    if (packageName.trim().isEmpty()) {
      throw new PackageDownloadException("Package name cannot be empty");
    }

    if (!PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
      throw new PackageDownloadException(
          "Invalid package name format. Expected pattern: [a-z][a-z0-9-]*(.[a-z][a-z0-9-]*)+");
    }
  }

  /**
   * Validates semantic version syntax for package lookup.
   *
   * @param packageVersion package version to validate
   * @throws PackageDownloadException if the package version is invalid
   */
  private void validatePackageVersion(String packageVersion) throws PackageDownloadException {
    Objects.requireNonNull(packageVersion, "Package version cannot be null");

    if (packageVersion.trim().isEmpty()) {
      throw new PackageDownloadException("Package version cannot be empty");
    }

    if (!VERSION_PATTERN.matcher(packageVersion).matches()) {
      throw new PackageDownloadException(
          "Invalid package version format. Expected semantic versioning (e.g., 1.0.0, 1.0.0-rc.1)");
    }
  }

  /**
   * Ensures the target download directory exists and is writable as a directory.
   *
   * @param directory target directory for downloaded package files
   * @throws PackageDownloadException if directory creation or validation fails
   */
  private void ensureDirectoryExists(Path directory) throws PackageDownloadException {
    try {
      if (!Files.exists(directory)) {
        Files.createDirectories(directory);
        log.debug("Created target directory: {}", directory);
      } else if (!Files.isDirectory(directory)) {
        throw new PackageDownloadException(
            "Target path exists but is not a directory: " + directory);
      }
    } catch (IOException e) {
      throw new PackageDownloadException("Failed to create target directory: " + directory, e);
    }
  }

  /**
   * Normalizes and validates a registry base URL.
   *
   * @param rawBaseUrl configured raw base URL
   * @return normalized base URL without trailing slash
   */
  private String normalizeBaseUrl(String rawBaseUrl) {
    Objects.requireNonNull(rawBaseUrl, "Registry base URL cannot be null");

    String normalized = rawBaseUrl.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Registry base URL cannot be empty");
    }
    if (normalized.endsWith("/")) {
      return normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  /**
   * Normalizes the configured package path prefix and maps root paths to an empty prefix.
   *
   * @param rawPathPrefix configured package path prefix
   * @return normalized package path prefix used for package URLs
   */
  private String normalizePackagePathPrefix(String rawPathPrefix) {
    // Explicit empty value means root layout (e.g. https://packages.fhir.org/{package}).
    if (rawPathPrefix != null && rawPathPrefix.trim().isEmpty()) {
      return "";
    }

    String normalizedPath = normalizeAbsolutePath(rawPathPrefix, PACKAGE_DOWNLOAD_PATH);
    return URL_PATH_SEPARATOR.equals(normalizedPath) ? "" : normalizedPath;
  }

  /**
   * Normalizes an absolute endpoint path by applying defaults and trimming redundant separators.
   *
   * @param rawPath configured raw path
   * @param defaultPath fallback path when configuration is missing or blank
   * @return normalized absolute path without trailing slash
   */
  private String normalizeAbsolutePath(String rawPath, String defaultPath) {
    String normalizedPath = rawPath == null ? defaultPath : rawPath.trim();
    if (normalizedPath.isEmpty()) {
      normalizedPath = defaultPath;
    }

    if (!normalizedPath.startsWith(URL_PATH_SEPARATOR)) {
      normalizedPath = URL_PATH_SEPARATOR + normalizedPath;
    }

    if (normalizedPath.length() > 1 && normalizedPath.endsWith(URL_PATH_SEPARATOR)) {
      normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
    }

    return normalizedPath;
  }

  /** Closes HTTP resources used by the downloader. */
  @Override
  public void close() {
    httpClient.dispatcher().executorService().shutdown();
    httpClient.connectionPool().evictAll();
  }

  /**
   * Ensures a bearer token is present when the requested package version is marked as protected.
   *
   * @param packageName requested package name
   * @param packageVersion requested package version
   * @param packageVersionNode metadata node for the requested version
   * @throws PackageDownloadException if metadata lookup fails or token generation fails
   */
  private void ensureTokenForProtectedPackage(
      String packageName, String packageVersion, JsonNode packageVersionNode)
      throws PackageDownloadException {
    if (!isPackageVersionProtected(packageName, packageVersion, packageVersionNode)) {
      return;
    }

    log.info("Package {}@{} is protected, generating token", packageName, packageVersion);
    generateToken(List.of(packageName));
  }

  /**
   * Checks package metadata for the protected flag of a specific version.
   *
   * @param packageName package name
   * @param packageVersion package version
   * @return {@code true} when the version is protected
   * @throws PackageDownloadException if the metadata cannot be read or does not contain the version
   */
  private boolean isPackageVersionProtected(
      String packageName, String packageVersion, JsonNode packageVersionNode)
      throws PackageDownloadException {
    JsonNode protectedNode = packageVersionNode.path("protected");
    if (protectedNode.isMissingNode() || protectedNode.isNull()) {
      throw new PackageDownloadException(
          "Package metadata for "
              + packageName
              + " does not provide a protected flag for version "
              + packageVersion);
    }

    return protectedNode.asBoolean(false);
  }

  /**
   * Resolves all package name/version entries from the server catalog endpoint.
   *
   * @return entries in format {@code package.name@x.y.z}
   * @throws PackageDownloadException if catalog retrieval fails
   */
  private List<String> resolvePackageNameVersionEntriesFromCatalog()
      throws PackageDownloadException {
    ensureCatalogAndWildcardSupported();

    JsonNode catalogNode = executeJsonGet(baseUrl + packageCatalogPath);

    if (!catalogNode.isArray()) {
      throw new PackageDownloadException("Package catalog response is not an array");
    }

    Set<String> packageEntries = new LinkedHashSet<>();
    for (JsonNode packageNode : catalogNode) {
      addCatalogPackageEntries(packageNode, packageEntries);
    }

    return List.copyOf(packageEntries);
  }

  /**
   * Ensures that catalog lookup and wildcard expansion are allowed for the configured registry.
   *
   * @throws PackageDownloadException if catalog lookup is blocked for the configured base URL
   */
  private void ensureCatalogAndWildcardSupported() throws PackageDownloadException {
    if (!isCatalogAndWildcardBlockedForBaseUrl()) {
      return;
    }

    throw new PackageDownloadException(
        "Catalog lookup and wildcard '*' are not supported for registry base URL "
            + baseUrl
            + ". Configure explicit package entries as package.name@x.y.z.");
  }

  /**
   * Checks whether the configured registry base URL is blocked for catalog and wildcard usage.
   *
   * @return {@code true} if catalog lookup must be rejected for the configured base URL
   */
  private boolean isCatalogAndWildcardBlockedForBaseUrl() {
    for (String blockedBaseUrl : CATALOG_WILDCARD_BLOCKLIST_BASE_URLS) {
      if (blockedBaseUrl.equalsIgnoreCase(baseUrl)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Builds the metadata URL for a package.
   *
   * @param packageName package name
   * @return metadata endpoint URL for the package
   */
  private String buildPackageMetadataUrl(String packageName) {
    return baseUrl + packagePathPrefix + "/" + packageName;
  }

  /**
   * Builds the package archive download URL for a specific version.
   *
   * @param packageName package name
   * @param packageVersion package version
   * @return download endpoint URL for the package version
   */
  private String buildPackageDownloadUrl(String packageName, String packageVersion) {
    return baseUrl + packagePathPrefix + "/" + packageName + "/" + packageVersion;
  }

  /**
   * Executes a JSON GET request and parses the response body.
   *
   * @param url endpoint URL
   * @return parsed JSON body
   * @throws PackageDownloadException if the request fails or response body is invalid
   */
  private JsonNode executeJsonGet(String url) throws PackageDownloadException {
    Request.Builder requestBuilder = new Request.Builder().url(url).get();
    applyAuthentication(requestBuilder);
    Request request = requestBuilder.build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new PackageDownloadException(
            "Failed to retrieve metadata from " + url + ". Status: " + response.code(),
            response.code());
      }

      return OBJECT_MAPPER.readTree(response.body().string());
    } catch (IOException e) {
      throw new PackageDownloadException("Failed to retrieve metadata from " + url, e);
    }
  }

  /**
   * Extracts and adds valid package name/version entries from one catalog entry.
   *
   * @param packageNode one package entry from the catalog response
   * @param packageEntries mutable target set collecting {@code package@version} entries
   */
  private void addCatalogPackageEntries(JsonNode packageNode, Set<String> packageEntries) {
    JsonNode nameNode = packageNode.path("name");
    if (!nameNode.isTextual()) {
      return;
    }

    String packageName = nameNode.asText().trim();
    if (packageName.isEmpty()) {
      return;
    }

    JsonNode versionsNode = packageNode.path("packageVersions");
    if (!versionsNode.isArray()) {
      return;
    }

    for (JsonNode versionNode : versionsNode) {
      if (versionNode.isTextual()) {
        String packageVersion = versionNode.asText().trim();
        if (!packageVersion.isEmpty()) {
          packageEntries.add(packageName + "@" + packageVersion);
        }
      }
    }
  }

  /**
   * Filters out deprecated/unlisted package versions from resolved package entries.
   *
   * @param packageEntries resolved package entries
   * @return package entries without deprecated/unlisted versions
   * @throws PackageDownloadException if metadata lookup fails
   */
  private List<String> filterDeprecatedAndUnlistedPackageEntries(Set<String> packageEntries)
      throws PackageDownloadException {
    Map<String, JsonNode> packageMetadataByName = new HashMap<>();
    Set<String> filteredPackageEntries = new LinkedHashSet<>();

    for (String packageEntry : packageEntries) {
      PackageCoordinate packageCoordinate = parsePackageCoordinate(packageEntry);
      if (packageCoordinate == null) {
        filteredPackageEntries.add(packageEntry);
        continue;
      }

      JsonNode packageMetadataNode = packageMetadataByName.get(packageCoordinate.packageName());
      if (packageMetadataNode == null) {
        packageMetadataNode =
            executeJsonGet(buildPackageMetadataUrl(packageCoordinate.packageName()));
        packageMetadataByName.put(packageCoordinate.packageName(), packageMetadataNode);
      }

      JsonNode packageVersionNode =
          packageMetadataNode.path("versions").path(packageCoordinate.packageVersion());
      if (!shouldSkipDeprecatedOrUnlistedPackageVersion(packageVersionNode, packageEntry)) {
        filteredPackageEntries.add(packageEntry);
      }
    }

    return List.copyOf(filteredPackageEntries);
  }

  /**
   * Parses a package coordinate in format {@code package.name@x.y.z}.
   *
   * @param packageEntry configured package entry
   * @return parsed coordinate or {@code null} if format does not include name and version
   */
  private PackageCoordinate parsePackageCoordinate(String packageEntry) {
    int separatorIndex = packageEntry.lastIndexOf('@');
    if (separatorIndex <= 0 || separatorIndex >= packageEntry.length() - 1) {
      return null;
    }

    String packageName = packageEntry.substring(0, separatorIndex).trim();
    String packageVersion = packageEntry.substring(separatorIndex + 1).trim();
    if (packageName.isEmpty() || packageVersion.isEmpty()) {
      return null;
    }

    return new PackageCoordinate(packageName, packageVersion);
  }

  /**
   * Determines whether a package version should be skipped due to deprecated/unlisted metadata.
   *
   * @param packageVersionNode metadata node for one package version
   * @param packageEntry package entry in format {@code package.name@x.y.z}
   * @return {@code true} when the version should be skipped
   */
  private boolean shouldSkipDeprecatedOrUnlistedPackageVersion(
      JsonNode packageVersionNode, String packageEntry) {
    if (packageVersionNode.isMissingNode() || packageVersionNode.isNull()) {
      return false;
    }

    String deprecationOrUnlistedMessage = resolveDeprecationOrUnlistedMessage(packageVersionNode);
    if (deprecationOrUnlistedMessage == null) {
      return false;
    }

    log.warn(
        "Skipping package {} because it is deprecated/unlisted: {}",
        packageEntry,
        deprecationOrUnlistedMessage);
    return true;
  }

  /**
   * Resolves deprecated/unlisted metadata message for one package version.
   *
   * @param packageVersionNode metadata node for one package version
   * @return combined message or {@code null} when the version is neither deprecated nor unlisted
   */
  private String resolveDeprecationOrUnlistedMessage(JsonNode packageVersionNode) {
    String deprecatedMessage = extractPackageStatusMessage(packageVersionNode, "deprecated");
    String unlistedMessage = extractPackageStatusMessage(packageVersionNode, "unlisted");

    if (deprecatedMessage == null && unlistedMessage == null) {
      return null;
    }

    if (deprecatedMessage != null && unlistedMessage != null) {
      return "deprecated: " + deprecatedMessage + "; unlisted: " + unlistedMessage;
    }

    if (deprecatedMessage != null) {
      return "deprecated: " + deprecatedMessage;
    }

    return "unlisted: " + unlistedMessage;
  }

  /**
   * Resolves one status message for a package version metadata field.
   *
   * @param packageVersionNode metadata node for one package version
   * @param statusFieldName metadata field name, e.g. {@code deprecated} or {@code unlisted}
   * @return status message or {@code null} when the status is not active
   */
  private String extractPackageStatusMessage(JsonNode packageVersionNode, String statusFieldName) {
    JsonNode statusNode = packageVersionNode.path(statusFieldName);
    if (statusNode.isMissingNode() || statusNode.isNull()) {
      return null;
    }

    if (statusNode.isBoolean()) {
      if (!statusNode.asBoolean(false)) {
        return null;
      }
      return "marked as " + statusFieldName;
    }

    String statusText = statusNode.asText().trim();
    return statusText.isEmpty() ? "marked as " + statusFieldName : statusText;
  }

  /**
   * Resolves metadata for a specific package version from the package metadata document.
   *
   * @param packageName package name
   * @param packageVersion package version
   * @return metadata node for the requested package version
   * @throws PackageDownloadException if metadata cannot be retrieved or version metadata is missing
   */
  private JsonNode resolvePackageVersionNode(String packageName, String packageVersion)
      throws PackageDownloadException {
    JsonNode metadataNode = executeJsonGet(buildPackageMetadataUrl(packageName));
    JsonNode packageVersionNode = metadataNode.path("versions").path(packageVersion);

    if (packageVersionNode.isMissingNode() || packageVersionNode.isNull()) {
      throw new PackageDownloadException(
          "Package metadata for "
              + packageName
              + " does not provide information for version "
              + packageVersion);
    }

    return packageVersionNode;
  }

  /**
   * Reads the expected SHA-1 checksum from package metadata.
   *
   * @param packageVersionNode metadata node for the requested package version
   * @param packageName package name
   * @param packageVersion package version
   * @return normalized lowercase SHA-1 checksum from {@code dist.shasum}
   * @throws PackageDownloadException if checksum metadata is missing or invalid
   */
  private String resolveExpectedShaSum(
      JsonNode packageVersionNode, String packageName, String packageVersion)
      throws PackageDownloadException {
    JsonNode shaSumNode = packageVersionNode.path("dist").path("shasum");

    if (!shaSumNode.isTextual() || shaSumNode.asText().trim().isEmpty()) {
      throw new PackageDownloadException(
          "Package metadata for "
              + packageName
              + " does not provide a dist.shasum for version "
              + packageVersion);
    }

    return shaSumNode.asText().trim().toLowerCase();
  }

  /**
   * Verifies that the downloaded file checksum matches metadata and removes invalid files.
   *
   * @param filePath path to the downloaded archive
   * @param expectedShaSum expected SHA-1 checksum from metadata
   * @param packageName package name
   * @param packageVersion package version
   * @throws PackageDownloadException if checksum verification fails
   */
  private void verifyFileSha1(
      Path filePath, String expectedShaSum, String packageName, String packageVersion)
      throws PackageDownloadException {
    String actualShaSum = calculateFileSha1(filePath);
    if (actualShaSum.equalsIgnoreCase(expectedShaSum)) {
      return;
    }

    try {
      Files.deleteIfExists(filePath);
    } catch (IOException deleteException) {
      log.warn(
          "Failed to delete package file with invalid checksum: {}", filePath, deleteException);
    }

    throw new PackageDownloadException(
        "Checksum verification failed for "
            + packageName
            + "@"
            + packageVersion
            + ". Expected "
            + expectedShaSum
            + " but found "
            + actualShaSum);
  }

  /**
   * Calculates the SHA-1 checksum of a file.
   *
   * @param filePath file to hash
   * @return lowercase hexadecimal SHA-1 checksum
   * @throws PackageDownloadException if hashing cannot be performed
   */
  @SuppressWarnings("java:S4790")
  // SHA-1 is required for compatibility with package metadata, not used for security purposes
  private String calculateFileSha1(Path filePath) throws PackageDownloadException {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new PackageDownloadException("SHA-1 algorithm is not available", e);
    }

    byte[] buffer = new byte[8192];
    try (InputStream inputStream = Files.newInputStream(filePath)) {
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        messageDigest.update(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new PackageDownloadException("Failed to calculate checksum for " + filePath, e);
    }

    byte[] digest = messageDigest.digest();
    StringBuilder builder = new StringBuilder(digest.length * 2);
    for (byte digestByte : digest) {
      builder.append(String.format("%02x", digestByte));
    }
    return builder.toString();
  }

  private record PackageCoordinate(String packageName, String packageVersion) {}
}
