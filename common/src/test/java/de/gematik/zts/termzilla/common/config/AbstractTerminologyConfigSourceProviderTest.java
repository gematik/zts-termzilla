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
package de.gematik.zts.termzilla.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link AbstractTerminologyConfigSourceProvider}.
 *
 * <p>Verifies configuration loading behavior including file discovery, validation, convention-based
 * defaults, and error handling.
 */
@DisplayName("AbstractTerminologyConfigSourceProvider Tests")
class AbstractTerminologyConfigSourceProviderTest {

  private static final String TEST_RESOURCE_PATH = "/test-application.yaml";
  private static final String MALFORMED_RESOURCE_PATH = "/malformed.yaml";
  private static final String TEST_TERMINOLOGY = "Test";

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize with terminology name")
    void shouldInitializeWithTerminologyName() {
      // GIVEN & WHEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);

      // THEN
      assertThat(provider.getTerminologyName())
          .isEqualTo(TEST_TERMINOLOGY + "ConfigSourceProvider");
    }

    @Test
    @DisplayName("Should derive property prefix from terminology name")
    void shouldDerivePropertyPrefixFromTerminologyName() {
      // GIVEN & WHEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("MyTerm");

      // THEN
      assertThat(provider.getPropertyPrefix()).isEqualTo("myterm");
    }

    @Test
    @DisplayName("Should generate config files based on terminology name")
    void shouldGenerateConfigFilesBasedOnTerminologyName() {
      // GIVEN & WHEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("Custom");

      // THEN
      assertThat(provider.getConfigFiles())
          .containsExactly(
              "custom-application.yaml",
              "custom-application.yml",
              "application.yaml",
              "application.yml");
    }
  }

  @Nested
  @DisplayName("getTerminologyName() Tests")
  class GetTerminologyNameTests {

    @Test
    @DisplayName("Should return terminology name with ConfigSourceProvider suffix")
    void shouldReturnTerminologyNameWithSuffix() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("Fhir");

      // WHEN
      String result = provider.getTerminologyName();

      // THEN
      assertThat(result).isEqualTo("FhirConfigSourceProvider");
    }

    @Test
    @DisplayName("Should handle single character terminology names")
    void shouldHandleSingleCharacterTerminologyNames() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider = new TestTerminologyConfigSourceProvider("X");

      // WHEN
      String result = provider.getTerminologyName();

      // THEN
      assertThat(result).isEqualTo("XConfigSourceProvider");
    }
  }

  @Nested
  @DisplayName("getPropertyPrefix() Tests")
  class GetPropertyPrefixTests {

    @Test
    @DisplayName("Should return lowercase terminology name")
    void shouldReturnLowercaseTerminologyName() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("UPPERCASE");

      // WHEN
      String result = provider.getPropertyPrefix();

      // THEN
      assertThat(result).isEqualTo("uppercase");
    }

    @Test
    @DisplayName("Should handle mixed case terminology names")
    void shouldHandleMixedCaseTerminologyNames() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("MiXeDCaSe");

      // WHEN
      String result = provider.getPropertyPrefix();

      // THEN
      assertThat(result).isEqualTo("mixedcase");
    }

    @Test
    @DisplayName("Should allow override of property prefix")
    void shouldAllowOverrideOfPropertyPrefix() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("Test") {
            @Override
            protected String getPropertyPrefix() {
              return "custom-prefix";
            }
          };

      // WHEN
      String result = provider.getPropertyPrefix();

      // THEN
      assertThat(result).isEqualTo("custom-prefix");
    }
  }

  @Nested
  @DisplayName("getConfigFiles() Tests")
  class GetConfigFilesTests {

    @Test
    @DisplayName("Should return convention-based config file names")
    void shouldReturnConventionBasedConfigFileNames() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("loinc");

      // WHEN
      String[] result = provider.getConfigFiles();

      // THEN
      assertThat(result)
          .containsExactly(
              "loinc-application.yaml",
              "loinc-application.yml",
              "application.yaml",
              "application.yml");
    }

    @Test
    @DisplayName("Should prioritize terminology-specific files over generic ones")
    void shouldPrioritizeTerminologySpecificFiles() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider("snomed");

      // WHEN
      String[] result = provider.getConfigFiles();

      // THEN
      assertThat(result[0]).isEqualTo("snomed-application.yaml");
      assertThat(result[2]).isEqualTo("application.yaml");
    }
  }

  @Nested
  @DisplayName("isValidConfigFile() Tests")
  class IsValidConfigFileTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "file:/path/to/termzilla-test/test-application.yaml",
          "jar:file:/lib/test.jar!/test/config.yaml",
          "file:/modules/test/application.yaml",
          "file:/config/test-application.yml"
        })
    @DisplayName("Should validate URLs containing terminology patterns")
    void shouldValidateUrlsContainingTerminologyPatterns(String urlString) throws Exception {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      URL testUrl = new URI(urlString).toURL();

      // WHEN
      boolean result = provider.isValidConfigFile(testUrl);

      // THEN
      assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "file:/path/to/other-module/application.yaml",
          "jar:file:/lib/different.jar!/config.yaml",
          "file:/config/unrelated-application.yml"
        })
    @DisplayName("Should reject URLs without terminology patterns")
    void shouldRejectUrlsWithoutTerminologyPatterns(String urlString) throws Exception {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      URL testUrl = new URI(urlString).toURL();

      // WHEN
      boolean result = provider.isValidConfigFile(testUrl);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle URLs with special characters")
    void shouldHandleUrlsWithSpecialCharacters() throws Exception {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      URL testUrl = new URI("file:/path/with%20spaces/test-application.yaml").toURL();

      // WHEN
      boolean result = provider.isValidConfigFile(testUrl);

      // THEN
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("getConfigSources() Tests")
  class GetConfigSourcesTests {

    @Test
    @DisplayName("Should return empty list when no config files are found")
    void shouldReturnEmptyListWhenNoConfigFilesFound() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      when(classLoader.getResources(anyString())).thenReturn(Collections.emptyEnumeration());

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should load valid config file successfully")
    void shouldLoadValidConfigFileSuccessfully() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL validConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      Enumeration<URL> urls = createEnumeration(validConfigUrl);
      when(classLoader.getResources("test-application.yaml")).thenReturn(urls);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isNotEmpty().hasSize(1);
    }

    @Test
    @DisplayName("Should skip invalid config files")
    void shouldSkipInvalidConfigFiles() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY, false);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL invalidConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      Enumeration<URL> urls = createEnumeration(invalidConfigUrl);
      when(classLoader.getResources(anyString())).thenReturn(urls);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should try multiple config file names in order")
    void shouldTryMultipleConfigFileNamesInOrder() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL validConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      when(classLoader.getResources("test-application.yaml"))
          .thenReturn(Collections.emptyEnumeration());
      when(classLoader.getResources("test-application.yml"))
          .thenReturn(Collections.emptyEnumeration());
      when(classLoader.getResources("application.yaml"))
          .thenReturn(createEnumeration(validConfigUrl));
      when(classLoader.getResources("application.yml")).thenReturn(Collections.emptyEnumeration());

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isNotEmpty().hasSize(1);
    }

    @Test
    @DisplayName("Should stop searching after finding valid config")
    void shouldStopSearchingAfterFindingValidConfig() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL validConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      when(classLoader.getResources("test-application.yaml"))
          .thenReturn(createEnumeration(validConfigUrl));

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should handle IOException during resource loading")
    void shouldHandleIOExceptionDuringResourceLoading() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      when(classLoader.getResources(anyString())).thenThrow(new IOException("Test exception"));

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should load first valid config from multiple URLs")
    void shouldLoadFirstValidConfigFromMultipleUrls() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL validConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      Enumeration<URL> urls = createEnumeration(validConfigUrl, validConfigUrl);
      when(classLoader.getResources("test-application.yaml")).thenReturn(urls);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should handle null classloader gracefully")
    void shouldHandleNullClassloaderGracefully() {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(null);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle null enumeration from classloader")
    void shouldHandleNullEnumerationFromClassloader() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      when(classLoader.getResources(anyString())).thenReturn(null);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty config files array")
    void shouldHandleEmptyConfigFilesArray() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY, new String[0]);
      ClassLoader classLoader = mock(ClassLoader.class);
      when(classLoader.getResources(anyString())).thenReturn(Collections.emptyEnumeration());

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle malformed YAML files gracefully")
    void shouldHandleMalformedYamlFilesGracefully() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL malformedUrl = getTestResourceUrl(MALFORMED_RESOURCE_PATH);
      Enumeration<URL> urls = createEnumeration(malformedUrl);
      when(classLoader.getResources("test-application.yaml")).thenReturn(urls);

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should load config when valid file found after invalid ones")
    void shouldLoadConfigWhenValidFileFoundAfterInvalidOnes() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL malformedUrl = getTestResourceUrl(MALFORMED_RESOURCE_PATH);
      URL validUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      when(classLoader.getResources("test-application.yaml"))
          .thenReturn(createEnumeration(malformedUrl));
      when(classLoader.getResources("test-application.yml"))
          .thenReturn(Collections.emptyEnumeration());
      when(classLoader.getResources("application.yaml")).thenReturn(createEnumeration(validUrl));

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should return result as iterable")
    void shouldReturnResultAsIterable() throws IOException {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY);
      ClassLoader classLoader = mock(ClassLoader.class);
      URL validConfigUrl = getTestResourceUrl(TEST_RESOURCE_PATH);
      when(classLoader.getResources("test-application.yaml"))
          .thenReturn(createEnumeration(validConfigUrl));

      // WHEN
      Iterable<ConfigSource> result = provider.getConfigSources(classLoader);

      // THEN
      assertThat(result).isInstanceOf(Iterable.class).isInstanceOf(List.class);
    }
  }

  @Nested
  @DisplayName("Convention Override Tests")
  class ConventionOverrideTests {

    @Test
    @DisplayName("Should allow complete override of config file names")
    void shouldAllowCompleteOverrideOfConfigFileNames() {
      // GIVEN
      String[] customFiles = {"custom1.yaml", "custom2.yml"};
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY, customFiles);

      // WHEN
      String[] result = provider.getConfigFiles();

      // THEN
      assertThat(result).containsExactly("custom1.yaml", "custom2.yml");
    }

    @Test
    @DisplayName("Should allow override of validation logic")
    void shouldAllowOverrideOfValidationLogic() throws Exception {
      // GIVEN
      TestTerminologyConfigSourceProvider provider =
          new TestTerminologyConfigSourceProvider(TEST_TERMINOLOGY) {
            @Override
            protected boolean isValidConfigFile(URL yamlUrl) {
              return yamlUrl.toString().contains("always-valid");
            }
          };
      URL testUrl = new URI("file:/path/always-valid/config.yaml").toURL();

      // WHEN
      boolean result = provider.isValidConfigFile(testUrl);

      // THEN
      assertThat(result).isTrue();
    }
  }

  // Helper Methods

  /**
   * Creates an enumeration from the provided URLs.
   *
   * @param urls the URLs to include in the enumeration
   * @return an enumeration containing the provided URLs
   */
  private Enumeration<URL> createEnumeration(URL... urls) {
    Vector<URL> vector = new Vector<>();
    Collections.addAll(vector, urls);
    return vector.elements();
  }

  /**
   * Retrieves a test resource URL from the classpath.
   *
   * @param resourcePath the path to the resource
   * @return the URL of the resource
   * @throws IllegalStateException if the resource cannot be found
   */
  private URL getTestResourceUrl(String resourcePath) {
    URL url = getClass().getResource(resourcePath);
    if (url == null) {
      throw new IllegalStateException("Test resource not found: " + resourcePath);
    }
    return url;
  }

  // Test Implementation

  /**
   * Concrete test implementation of {@link AbstractTerminologyConfigSourceProvider} for testing
   * purposes.
   */
  private static class TestTerminologyConfigSourceProvider
      extends AbstractTerminologyConfigSourceProvider {

    private final boolean shouldValidate;
    private final String[] customConfigFiles;

    public TestTerminologyConfigSourceProvider(String terminology) {
      this(terminology, true);
    }

    public TestTerminologyConfigSourceProvider(String terminology, boolean shouldValidate) {
      this(terminology, shouldValidate, null);
    }

    public TestTerminologyConfigSourceProvider(String terminology, String[] configFiles) {
      this(terminology, true, configFiles);
    }

    public TestTerminologyConfigSourceProvider(
        String terminology, boolean shouldValidate, String[] configFiles) {
      super(terminology);
      this.shouldValidate = shouldValidate;
      this.customConfigFiles = configFiles;
    }

    @Override
    protected String[] getConfigFiles() {
      return customConfigFiles != null ? customConfigFiles : super.getConfigFiles();
    }

    @Override
    protected boolean isValidConfigFile(URL yamlUrl) {
      if (yamlUrl == null || !shouldValidate) {
        return false;
      }
      String urlPath = yamlUrl.toString();
      String lower = getPropertyPrefix();
      return urlPath.contains("/termzilla-" + lower + "/")
          || urlPath.contains("/" + lower + "/")
          || urlPath.contains(lower + "-application");
    }
  }
}
