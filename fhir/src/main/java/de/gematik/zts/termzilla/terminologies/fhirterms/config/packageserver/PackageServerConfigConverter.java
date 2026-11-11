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
package de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver;

import de.gematik.zts.termzilla.common.config.ConfigurationInjector.TypeConverter;
import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/** Converter that builds {@link PackageServerConfig} from indexed MicroProfile properties. */
public class PackageServerConfigConverter implements TypeConverter<PackageServerConfig> {

  private static final String SERVERS_PREFIX = "fhirterms.packageServerConfig.servers[";
  private static final Pattern SERVER_INDEX_PATTERN =
      Pattern.compile("^fhirterms\\.packageServerConfig\\.servers\\[(\\d+)]\\..+$");
  private static final Pattern PACKAGE_INDEX_PATTERN =
      Pattern.compile("^fhirterms\\.packageServerConfig\\.servers\\[(\\d+)]\\.packages\\[(\\d+)]$");
  private static final Pattern SERVER_INDEX_CANONICAL_PATTERN =
      Pattern.compile("^fhirterms_packageserverconfig_servers_(\\d+)_.+$");
  private static final Pattern PACKAGE_INDEX_CANONICAL_PATTERN =
      Pattern.compile("^fhirterms_packageserverconfig_servers_(\\d+)_packages_(\\d+)$");

  /**
   * Converts package server configuration from MicroProfile properties.
   *
   * @param value raw configuration value (unused, conversion is property-based)
   * @return parsed package server configuration
   */
  @Override
  public PackageServerConfig convert(String value) {
    Config config = ConfigProvider.getConfig();
    List<Integer> serverIndices = collectServerIndices(config);

    List<PackageServerConfigEntry> servers = new ArrayList<>();
    for (Integer serverIndex : serverIndices) {
      servers.add(parseServer(config, serverIndex));
    }

    return new PackageServerConfig(servers);
  }

  /**
   * Collects all configured server indices from supported property key formats.
   *
   * @param config active MicroProfile config
   * @return sorted list of discovered server indices
   */
  private List<Integer> collectServerIndices(Config config) {
    Set<Integer> indices = new LinkedHashSet<>();
    for (String propertyName : config.getPropertyNames()) {
      Matcher matcher = SERVER_INDEX_PATTERN.matcher(propertyName);
      if (matcher.matches()) {
        indices.add(Integer.parseInt(matcher.group(1)));
        continue;
      }

      String canonicalPropertyName = toCanonicalPropertyName(propertyName);
      Matcher canonicalMatcher = SERVER_INDEX_CANONICAL_PATTERN.matcher(canonicalPropertyName);
      if (canonicalMatcher.matches()) {
        indices.add(Integer.parseInt(canonicalMatcher.group(1)));
      }
    }
    return indices.stream().sorted().toList();
  }

  /**
   * Parses one server configuration entry for the given index.
   *
   * @param config active MicroProfile config
   * @param index server index
   * @return parsed package server entry
   */
  private PackageServerConfigEntry parseServer(Config config, int index) {
    String prefix = SERVERS_PREFIX + index + "].";

    String url = getOptionalString(config, prefix + "url").orElse("");
    String name = getOptionalString(config, prefix + "name").orElse("server-" + index);

    AuthenticationMode authMode =
        parseAuthMode(getOptionalString(config, prefix + "auth.type").orElse("NONE"));
    String authUser = getOptionalString(config, prefix + "auth.user").orElse(null);
    String authPassword = getOptionalString(config, prefix + "auth.password").orElse(null);
    String authToken = getOptionalString(config, prefix + "auth.token").orElse(null);
    String authApiKey = getOptionalString(config, prefix + "auth.apiKey").orElse(null);
    String authApiKeyHeader = getOptionalString(config, prefix + "auth.apiKeyHeader").orElse(null);

    PackageServerAuthConfig authConfig =
        new PackageServerAuthConfig(
            authMode, authUser, authPassword, authToken, authApiKey, authApiKeyHeader);
    PackageServerEndpointConfig endpointConfig = parseEndpointConfig(config, prefix);
    boolean skipDeprecatedUnlistedPackageVersions =
        getOptionalBoolean(config, prefix + "skipDeprecatedUnlistedPackageVersions").orElse(false);
    Set<String> packages = collectPackages(config, index, prefix);

    return new PackageServerConfigEntry(
        name, url, authConfig, endpointConfig, skipDeprecatedUnlistedPackageVersions, packages);
  }

  /**
   * Parses optional endpoint path settings for a server entry.
   *
   * @param config active MicroProfile config
   * @param prefix server property prefix
   * @return endpoint config or {@code null} when no endpoint keys are configured
   */
  private PackageServerEndpointConfig parseEndpointConfig(Config config, String prefix) {
    String packagePathPrefixKey = prefix + "endpoints.packagePathPrefix";
    String catalogPathKey = prefix + "endpoints.catalogPath";

    boolean hasPackagePathPrefix = hasProperty(config, packagePathPrefixKey);
    boolean hasCatalogPath = hasProperty(config, catalogPathKey);

    if (!hasPackagePathPrefix && !hasCatalogPath) {
      return null;
    }

    String packagePathPrefix =
        hasPackagePathPrefix
            ? config.getOptionalValue(packagePathPrefixKey, String.class).orElse("").trim()
            : null;
    String catalogPath =
        hasCatalogPath
            ? config.getOptionalValue(catalogPathKey, String.class).orElse("").trim()
            : null;

    return new PackageServerEndpointConfig(packagePathPrefix, catalogPath);
  }

  /**
   * Checks whether a configuration key exists in canonical or original form.
   *
   * @param config active MicroProfile config
   * @param key property key to check
   * @return {@code true} when the property exists
   */
  private boolean hasProperty(Config config, String key) {
    String canonicalKey = toCanonicalPropertyName(key);
    for (String propertyName : config.getPropertyNames()) {
      if (propertyName.equals(key) || toCanonicalPropertyName(propertyName).equals(canonicalKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Collects package entries for a server, preserving configured order and removing duplicates.
   *
   * @param config active MicroProfile config
   * @param serverIndex server index
   * @param prefix server property prefix
   * @return package entries in {@code package.name@version} format
   */
  private Set<String> collectPackages(Config config, int serverIndex, String prefix) {
    Set<IndexedResolvedConfigValue> resolvedPackages = new LinkedHashSet<>();

    for (String propertyName : config.getPropertyNames()) {
      Matcher matcher = PACKAGE_INDEX_PATTERN.matcher(propertyName);
      if (matcher.matches() && Integer.parseInt(matcher.group(1)) == serverIndex) {
        int packageIndex = Integer.parseInt(matcher.group(2));
        addResolvedPackageValue(config, propertyName, packageIndex, resolvedPackages);
        continue;
      }

      String canonicalPropertyName = toCanonicalPropertyName(propertyName);
      Matcher canonicalMatcher = PACKAGE_INDEX_CANONICAL_PATTERN.matcher(canonicalPropertyName);
      if (canonicalMatcher.matches()
          && Integer.parseInt(canonicalMatcher.group(1)) == serverIndex) {
        int packageIndex = Integer.parseInt(canonicalMatcher.group(2));
        addResolvedPackageValue(config, propertyName, packageIndex, resolvedPackages);
      }
    }

    if (!resolvedPackages.isEmpty()) {
      int highestSourceOrdinal =
          resolvedPackages.stream()
              .mapToInt(packageValue -> packageValue.resolvedValue().sourceOrdinal())
              .max()
              .orElse(Integer.MIN_VALUE);

      String winningSourceName =
          resolvedPackages.stream()
              .map(IndexedResolvedConfigValue::resolvedValue)
              .filter(packageValue -> packageValue.sourceOrdinal() == highestSourceOrdinal)
              .map(ResolvedConfigValue::sourceName)
              .findFirst()
              .orElse(ResolvedConfigValue.NON_SMALLRYE_SOURCE_NAME);

      Map<Integer, ResolvedConfigValue> packageValuesByIndex = new HashMap<>();
      for (IndexedResolvedConfigValue indexedResolvedValue : resolvedPackages) {
        ResolvedConfigValue resolvedValue = indexedResolvedValue.resolvedValue();
        if (resolvedValue.sourceOrdinal() != highestSourceOrdinal
            || !resolvedValue.sourceName().equals(winningSourceName)) {
          continue;
        }

        packageValuesByIndex.putIfAbsent(indexedResolvedValue.index(), resolvedValue);
      }

      return packageValuesByIndex.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(entry -> entry.getValue().value())
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // fallback: allow one comma-separated packages property for convenience
    String fallbackPackages = getOptionalString(config, prefix + "packages").orElse("");
    if (fallbackPackages.isBlank()) {
      return Set.of();
    }
    return List.of(fallbackPackages.split(",")).stream()
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /** Chooses the package value from the highest-priority config source for one package index. */
  private void addResolvedPackageValue(
      Config config,
      String propertyName,
      int packageIndex,
      Set<IndexedResolvedConfigValue> resolvedPackages) {
    resolveConfigValue(config, propertyName)
        .ifPresent(
            resolvedValue ->
                resolvedPackages.add(new IndexedResolvedConfigValue(packageIndex, resolvedValue)));
  }

  /** Resolves a property's string value together with its config source ordinal. */
  private Optional<ResolvedConfigValue> resolveConfigValue(Config config, String propertyName) {
    if (config instanceof SmallRyeConfig smallRyeConfig) {
      ConfigValue configValue = smallRyeConfig.getConfigValue(propertyName);
      if (configValue != null
          && configValue.getValue() != null
          && !configValue.getValue().isBlank()) {
        return Optional.of(
            new ResolvedConfigValue(
                configValue.getValue().trim(),
                configValue.getConfigSourceOrdinal(),
                configValue.getConfigSourceName()));
      }
    }

    return config
        .getOptionalValue(propertyName, String.class)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(
            value ->
                new ResolvedConfigValue(
                    value, Integer.MIN_VALUE, ResolvedConfigValue.NON_SMALLRYE_SOURCE_NAME));
  }

  /**
   * Parses the authentication mode including accepted aliases.
   *
   * @param rawMode raw configured auth mode
   * @return normalized authentication mode
   */
  private AuthenticationMode parseAuthMode(String rawMode) {
    String normalizedMode = rawMode.trim().toUpperCase();
    if ("BASIC".equals(normalizedMode)) {
      return AuthenticationMode.BASIC_AUTH;
    }
    if ("BEARER".equals(normalizedMode)) {
      return AuthenticationMode.BEARER_TOKEN;
    }
    if ("APIKEY".equals(normalizedMode)) {
      return AuthenticationMode.API_KEY;
    }
    return AuthenticationMode.valueOf(normalizedMode);
  }

  /**
   * Reads an optional boolean property and supports canonical key variants.
   *
   * @param config active MicroProfile config
   * @param key property key
   * @return optional parsed boolean value
   */
  private Optional<Boolean> getOptionalBoolean(Config config, String key) {
    Optional<String> directValue = getOptionalString(config, key);
    if (directValue.isPresent()) {
      return Optional.of(Boolean.parseBoolean(directValue.get()));
    }

    String canonicalKey = toCanonicalPropertyName(key);
    for (String propertyName : config.getPropertyNames()) {
      if (toCanonicalPropertyName(propertyName).equals(canonicalKey)) {
        Optional<String> rawValue =
            config
                .getOptionalValue(propertyName, String.class)
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        if (rawValue.isPresent()) {
          return Optional.of(Boolean.parseBoolean(rawValue.get()));
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Reads an optional string property and trims surrounding whitespace.
   *
   * @param config active MicroProfile config
   * @param key property key
   * @return optional trimmed string value
   */
  private Optional<String> getOptionalString(Config config, String key) {
    return config.getOptionalValue(key, String.class).map(String::trim);
  }

  /**
   * Converts a property key into a canonical underscore-based representation.
   *
   * @param propertyName original property key
   * @return canonical key used for tolerant key matching
   */
  private String toCanonicalPropertyName(String propertyName) {
    return propertyName
        .toLowerCase(Locale.ROOT)
        .replace('[', '_')
        .replace(']', '_')
        .replace('.', '_')
        .replace('-', '_')
        .replaceAll("_+", "_");
  }

  /** Holds a resolved config value and the originating source ordinal. */
  private record ResolvedConfigValue(String value, int sourceOrdinal, String sourceName) {

    private static final String NON_SMALLRYE_SOURCE_NAME = "__non_smallrye_config__";
  }

  /** Holds an indexed package value including its source metadata. */
  private record IndexedResolvedConfigValue(int index, ResolvedConfigValue resolvedValue) {}
}
