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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PackageServerConfigConverter}. */
@DisplayName("PackageServerConfigConverter")
class PackageServerConfigConverterTest {

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
  @DisplayName("convert(String)")
  class ConvertTests {

    @Test
    @DisplayName("should parse multiple servers with auth and indexed packages")
    void shouldParseMultipleServersWithAuthAndIndexedPackages() {
      // GIVEN
      registerConfig(PackageServerConfigDataHelper.twoServerProperties());
      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty())
          .hasSize(2)
          .extracting(PackageServerConfigEntry::name, PackageServerConfigEntry::url)
          .containsExactly(
              tuple("ZTS BfArM", "https://terminologien.bfarm.de"),
              tuple("FHIR Simplifier", "https://simplifier.net"));

      assertThat(config.getServersOrEmpty().get(0).auth().type())
          .isEqualTo(AuthenticationMode.BFARM_AUTO_TOKEN);
      assertThat(config.getServersOrEmpty().get(1).auth().type())
          .isEqualTo(AuthenticationMode.BASIC_AUTH);
      assertThat(config.getServersOrEmpty().get(1).auth().user()).isEqualTo("user");
      assertThat(config.getServersOrEmpty().get(1).auth().password()).isEqualTo("password");
      assertThat(config.getServersOrEmpty().get(0).packages())
          .containsExactlyInAnyOrder(
              "bfarm.terminologien.icd10gm@2024.0.0", "bfarm.terminologien.ops@2024.0.0");

      assertThat(config.getServersOrEmpty().get(0).endpoints()).isNotNull();
      assertThat(config.getServersOrEmpty().get(0).endpoints().packagePathPrefix())
          .isEqualTo("/packages");
      assertThat(config.getServersOrEmpty().get(0).endpoints().catalogPath())
          .isEqualTo("/packages/catalog");
      assertThat(config.getServersOrEmpty().get(0).skipDeprecatedUnlistedPackageVersions())
          .isTrue();
      assertThat(config.getServersOrEmpty().get(1).endpoints()).isNotNull();
      assertThat(config.getServersOrEmpty().get(1).endpoints().packagePathPrefix()).isEmpty();
      assertThat(config.getServersOrEmpty().get(1).endpoints().catalogPath()).isEqualTo("/catalog");
      assertThat(config.getServersOrEmpty().get(1).skipDeprecatedUnlistedPackageVersions())
          .isFalse();
    }

    @Test
    @DisplayName("should parse comma-separated package fallback")
    void shouldParseCommaSeparatedPackageFallback() {
      // GIVEN
      registerConfig(PackageServerConfigDataHelper.commaSeparatedPackageProperties());
      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().get(0).packages())
          .containsExactlyInAnyOrder(
              "bfarm.terminologien.icd10gm@2024.0.0", "bfarm.terminologien.ops@2024.0.0");
      assertThat(config.getServersOrEmpty().get(0).name()).isEqualTo("server-0");
      assertThat(config.getServersOrEmpty().get(0).auth().type())
          .isEqualTo(AuthenticationMode.NONE);
      assertThat(config.getServersOrEmpty().get(0).skipDeprecatedUnlistedPackageVersions())
          .isFalse();
    }

    @Test
    @DisplayName("should fail for unknown auth mode")
    void shouldFailForUnknownAuthMode() {
      // GIVEN
      registerConfig(
          Map.of(
              "fhirterms.packageServerConfig.servers[0].url", "https://example.org",
              "fhirterms.packageServerConfig.servers[0].auth.type", "UNKNOWN"));
      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN THEN
      assertThatThrownBy(() -> converter.convert("__AUTO__"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("should parse bearer token and api key authentication properties")
    void shouldParseBearerTokenAndApiKeyAuthenticationProperties() {
      // GIVEN
      registerConfig(
          Map.of(
              "fhirterms.packageServerConfig.servers[0].url", "https://example-bearer.org",
              "fhirterms.packageServerConfig.servers[0].auth.type", "BEARER_TOKEN",
              "fhirterms.packageServerConfig.servers[0].auth.token", "bearer-123",
              "fhirterms.packageServerConfig.servers[0].packages[0]", "pkg.one@1.0.0",
              "fhirterms.packageServerConfig.servers[1].url", "https://example-api-key.org",
              "fhirterms.packageServerConfig.servers[1].auth.type", "API_KEY",
              "fhirterms.packageServerConfig.servers[1].auth.apiKey", "key-123",
              "fhirterms.packageServerConfig.servers[1].auth.apiKeyHeader", "X-Custom-Key",
              "fhirterms.packageServerConfig.servers[1].packages[0]", "pkg.two@2.0.0"));
      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(2);
      assertThat(config.getServersOrEmpty().get(0).auth().type())
          .isEqualTo(AuthenticationMode.BEARER_TOKEN);
      assertThat(config.getServersOrEmpty().get(0).auth().token()).isEqualTo("bearer-123");
      assertThat(config.getServersOrEmpty().get(1).auth().type())
          .isEqualTo(AuthenticationMode.API_KEY);
      assertThat(config.getServersOrEmpty().get(1).auth().apiKey()).isEqualTo("key-123");
      assertThat(config.getServersOrEmpty().get(1).auth().apiKeyHeader()).isEqualTo("X-Custom-Key");
    }

    @Test
    @DisplayName("should replace full package list with values from higher-priority source")
    void shouldReplaceFullPackageListWithValuesFromHigherPrioritySource() {
      // GIVEN
      Map<String, String> lowPriorityProperties = new LinkedHashMap<>();
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "low.package.0@1.0.0");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[1]", "low.package.1@1.0.0");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[2]", "low.package.2@1.0.0");

      Map<String, String> highPriorityProperties = new LinkedHashMap<>();
      highPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      highPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "high.package.0@2.0.0");
      highPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[1]", "high.package.1@2.0.0");

      ConfigSource lowPrioritySource =
          new MapBackedConfigSource("low-priority-properties", lowPriorityProperties, 200) {};
      ConfigSource highPrioritySource =
          new MapBackedConfigSource("high-priority-properties", highPriorityProperties, 300) {};
      registerConfigSources(lowPrioritySource, highPrioritySource);

      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().getFirst().packages())
          .containsExactlyInAnyOrder("high.package.0@2.0.0", "high.package.1@2.0.0");
    }

    @Test
    @DisplayName(
        "should not merge lower-priority package indices when higher-priority source is partial")
    void shouldNotMergeLowerPriorityPackageIndicesWhenHigherPrioritySourceIsPartial() {
      // GIVEN
      Map<String, String> lowPriorityProperties = new LinkedHashMap<>();
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "low.package.0@1.0.0");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[1]", "low.package.1@1.0.0");
      lowPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[2]", "low.package.2@1.0.0");

      Map<String, String> highPriorityProperties = new LinkedHashMap<>();
      highPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      highPriorityProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "high.package.only@2.0.0");

      ConfigSource lowPrioritySource =
          new MapBackedConfigSource("low-priority-properties", lowPriorityProperties, 200) {};
      ConfigSource highPrioritySource =
          new MapBackedConfigSource("high-priority-properties", highPriorityProperties, 300) {};
      registerConfigSources(lowPrioritySource, highPrioritySource);

      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().getFirst().packages())
          .containsExactlyInAnyOrder("high.package.only@2.0.0");
    }

    @Test
    @DisplayName("should pick first-seen source for whole package section when ordinals are equal")
    void shouldPickFirstSeenSourceForWholePackageSectionWhenOrdinalsAreEqual() {
      // GIVEN
      Map<String, String> firstSourceProperties = new LinkedHashMap<>();
      firstSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      firstSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "first.package.0@1.0.0");
      firstSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[1]", "first.package.1@1.0.0");

      Map<String, String> secondSourceProperties = new LinkedHashMap<>();
      secondSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      secondSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[0]", "second.package.0@2.0.0");
      secondSourceProperties.put(
          "fhirterms.packageServerConfig.servers[0].packages[2]", "second.package.2@2.0.0");

      ConfigSource firstSource =
          new MapBackedConfigSource("first-source", firstSourceProperties, 300) {};
      ConfigSource secondSource =
          new MapBackedConfigSource("second-source", secondSourceProperties, 300) {};
      registerConfigSources(firstSource, secondSource);

      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().getFirst().packages())
          .containsExactlyInAnyOrder("first.package.0@1.0.0", "first.package.1@1.0.0");
    }

    @Test
    @DisplayName("should prefer indexed package list over comma-separated fallback")
    void shouldPreferIndexedPackageListOverCommaSeparatedFallback() {
      // GIVEN
      Map<String, String> properties = new LinkedHashMap<>();
      properties.put("fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      properties.put(
          "fhirterms.packageServerConfig.servers[0].packages",
          "fallback.one@1.0.0,fallback.two@1.0.0");
      properties.put("fhirterms.packageServerConfig.servers[0].packages[0]", "indexed.one@2.0.0");
      properties.put("fhirterms.packageServerConfig.servers[0].packages[1]", "indexed.two@2.0.0");
      registerConfig(properties);

      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().getFirst().packages())
          .containsExactlyInAnyOrder("indexed.one@2.0.0", "indexed.two@2.0.0");
    }

    @Test
    @DisplayName("should keep sparse package indices sorted")
    void shouldKeepSparsePackageIndicesSorted() {
      // GIVEN
      Map<String, String> properties = new LinkedHashMap<>();
      properties.put("fhirterms.packageServerConfig.servers[0].url", "https://example.org");
      properties.put("fhirterms.packageServerConfig.servers[0].packages[3]", "pkg.three@1.0.0");
      properties.put("fhirterms.packageServerConfig.servers[0].packages[1]", "pkg.one@1.0.0");
      registerConfig(properties);

      PackageServerConfigConverter converter = new PackageServerConfigConverter();

      // WHEN
      PackageServerConfig config = converter.convert("__AUTO__");

      // THEN
      assertThat(config.getServersOrEmpty()).hasSize(1);
      assertThat(config.getServersOrEmpty().getFirst().packages())
          .containsExactlyInAnyOrder("pkg.one@1.0.0", "pkg.three@1.0.0");
    }

    @Test
    @DisplayName("should resolve package values via non-SmallRye fallback path")
    void shouldResolvePackageValuesViaNonSmallRyeFallbackPath() throws Exception {
      // GIVEN
      PackageServerConfigConverter converter = new PackageServerConfigConverter();
      Config config = mock(Config.class);
      when(config.getOptionalValue(
              "fhirterms.packageServerConfig.servers[0].packages[0]", String.class))
          .thenReturn(Optional.of("  fallback.path.package@1.0.0  "));

      Method resolveConfigValue =
          PackageServerConfigConverter.class.getDeclaredMethod(
              "resolveConfigValue", Config.class, String.class);
      resolveConfigValue.setAccessible(true);

      // WHEN
      Optional<?> resolvedValue =
          (Optional<?>)
              resolveConfigValue.invoke(
                  converter, config, "fhirterms.packageServerConfig.servers[0].packages[0]");

      // THEN
      assertThat(resolvedValue).isPresent();
      Object valueHolder = resolvedValue.get();
      Method valueMethod = valueHolder.getClass().getDeclaredMethod("value");
      Method sourceOrdinalMethod = valueHolder.getClass().getDeclaredMethod("sourceOrdinal");
      assertThat(valueMethod.invoke(valueHolder)).isEqualTo("fallback.path.package@1.0.0");
      assertThat(sourceOrdinalMethod.invoke(valueHolder)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("should resolve skipDeprecated via canonical key scan loop")
    void shouldResolveSkipDeprecatedViaCanonicalKeyScanLoop() throws Exception {
      // GIVEN
      PackageServerConfigConverter converter = new PackageServerConfigConverter();
      Config config = mock(Config.class);
      when(config.getPropertyNames())
          .thenReturn(
              Set.of(
                  "fhirterms_packageserverconfig_servers_0_skipdeprecatedunlistedpackageversions"));
      when(config.getOptionalValue(
              "fhirterms.packageServerConfig.servers[0].skipDeprecatedUnlistedPackageVersions",
              String.class))
          .thenReturn(Optional.empty());
      when(config.getOptionalValue(
              "fhirterms_packageserverconfig_servers_0_skipdeprecatedunlistedpackageversions",
              String.class))
          .thenReturn(Optional.of(" true "));

      Method getOptionalBoolean =
          PackageServerConfigConverter.class.getDeclaredMethod(
              "getOptionalBoolean", Config.class, String.class);
      getOptionalBoolean.setAccessible(true);

      // WHEN
      @SuppressWarnings("unchecked")
      Optional<Boolean> resolvedBoolean =
          (Optional<Boolean>)
              getOptionalBoolean.invoke(
                  converter,
                  config,
                  "fhirterms.packageServerConfig.servers[0].skipDeprecatedUnlistedPackageVersions");

      // THEN
      assertThat(resolvedBoolean).isPresent().contains(true);
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

  private void registerConfigSources(ConfigSource... configSources) {
    try {
      ConfigProviderResolver resolver = ConfigProviderResolver.instance();
      resolver.releaseConfig(resolver.getConfig(classLoader));
    } catch (IllegalStateException _) {
      // no config registered yet
    }

    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
    builder.withSources(configSources);
    SmallRyeConfig config = builder.build();
    ConfigProviderResolver.instance().registerConfig(config, classLoader);
  }
}
