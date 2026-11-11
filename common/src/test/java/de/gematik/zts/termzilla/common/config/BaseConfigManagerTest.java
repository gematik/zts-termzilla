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

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BaseConfigManager}. Tests configuration management and injection
 * capabilities.
 */
@DisplayName("BaseConfigManager Tests")
class BaseConfigManagerTest {

  private SmallRyeConfig config;
  private ClassLoader classLoader;

  @BeforeEach
  void setUp() {
    classLoader = Thread.currentThread().getContextClassLoader();

    // Release any existing config first
    try {
      ConfigProviderResolver resolver = ConfigProviderResolver.instance();
      resolver.releaseConfig(resolver.getConfig(classLoader));
    } catch (IllegalStateException _) {
      // No config registered yet, that's fine
    }

    // Create a new SmallRye config for each test
    config =
        new SmallRyeConfigBuilder()
            .addDefaultSources()
            .withDefaultValue("test.value", "default-test-value")
            .withDefaultValue("custom.value", "default-custom-value")
            .build();

    // Register the config
    ConfigProviderResolver.instance().registerConfig(config, classLoader);
  }

  @AfterEach
  void tearDown() {
    // Clean up the config
    try {
      ConfigProviderResolver.instance().releaseConfig(config);
    } catch (IllegalStateException _) {
      // Config already released, that's fine
    }
  }

  /** Helper method to reconfigure with specific values */
  private void reconfigureWith(Map<String, String> properties) {
    // Release current config
    try {
      ConfigProviderResolver.instance().releaseConfig(config);
    } catch (IllegalStateException _) {
      // Already released
    }

    // Build new config with properties
    SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().addDefaultSources();

    properties.forEach(builder::withDefaultValue);

    config = builder.build();
    ConfigProviderResolver.instance().registerConfig(config, classLoader);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create config manager and inject configuration properties")
    void shouldCreateConfigManagerAndInjectProperties() {
      // GIVEN
      reconfigureWith(Map.of("test.value", "test-value"));

      // WHEN
      TestConfigManager configManager = new TestConfigManager();

      // THEN
      assertThat(configManager).isNotNull();
      assertThat(configManager.getConfiguration()).isNotNull();
      assertThat(configManager.getConfiguration().testValue).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Should inject default values when config keys not found")
    void shouldInjectDefaultValues() {
      // GIVEN
      reconfigureWith(Map.of());

      // WHEN
      TestConfigManagerWithDefaults configManager = new TestConfigManagerWithDefaults();

      // THEN
      assertThat(configManager.getConfiguration()).isNotNull();
      assertThat(configManager.getConfiguration().valueWithDefault).isEqualTo("default-value");
    }
  }

  @Nested
  @DisplayName("getConfiguration Tests")
  class GetConfigurationTests {

    @Test
    @DisplayName("Should return injected configuration properties")
    void shouldReturnInjectedConfiguration() {
      // GIVEN
      reconfigureWith(Map.of("test.value", "config-value"));
      TestConfigManager configManager = new TestConfigManager();

      // WHEN
      TestProperties testConfig = configManager.getConfiguration();

      // THEN
      assertThat(testConfig).isNotNull();
      assertThat(testConfig.testValue).isEqualTo("config-value");
    }

    @Test
    @DisplayName("Should return same configuration instance on multiple calls")
    void shouldReturnSameConfigurationInstance() {
      // GIVEN
      TestConfigManager configManager = new TestConfigManager();

      // WHEN
      TestProperties config1 = configManager.getConfiguration();
      TestProperties config2 = configManager.getConfiguration();

      // THEN
      assertThat(config1).isSameAs(config2);
    }
  }

  @Nested
  @DisplayName("inject Tests")
  class InjectTests {

    @Test
    @DisplayName("Should inject values into provided object")
    void shouldInjectValuesIntoProvidedObject() {
      // GIVEN
      reconfigureWith(
          Map.of(
              "test.value", "test-value",
              "custom.value", "custom-value"));

      TestConfigManager configManager = new TestConfigManager();
      CustomConfig customConfig = new CustomConfig();

      // WHEN
      CustomConfig result = configManager.inject(customConfig);

      // THEN
      assertThat(result).isSameAs(customConfig);
      assertThat(result.customValue).isEqualTo("custom-value");
    }

    @Test
    @DisplayName("Should return same object instance after injection")
    void shouldReturnSameObjectInstance() {
      // GIVEN
      TestConfigManager configManager = new TestConfigManager();
      CustomConfig customConfig = new CustomConfig();

      // WHEN
      CustomConfig result = configManager.inject(customConfig);

      // THEN
      assertThat(result).isSameAs(customConfig);
    }
  }

  @Nested
  @DisplayName("getModuleName Tests")
  class GetModuleNameTests {

    @Test
    @DisplayName("Should return module name from subclass")
    void shouldReturnModuleName() {
      // GIVEN
      TestConfigManager configManager = new TestConfigManager();

      // WHEN
      String moduleName = configManager.getModuleName();

      // THEN
      assertThat(moduleName).isEqualTo("TEST");
    }
  }

  @Nested
  @DisplayName("getInjector Tests")
  class GetInjectorTests {

    @Test
    @DisplayName("Should return injector instance")
    void shouldReturnInjector() {
      // GIVEN
      TestConfigManager configManager = new TestConfigManager();

      // WHEN
      ConfigurationInjector injector = configManager.getInjector();

      // THEN
      assertThat(injector).isNotNull();
    }

    @Test
    @DisplayName("Should allow custom converter registration")
    void shouldAllowCustomConverterRegistration() {
      // GIVEN
      TestConfigManager configManager = new TestConfigManager();
      ConfigurationInjector injector = configManager.getInjector();

      // WHEN
      injector.registerConverter(CustomType.class, CustomType::new);

      // THEN
      assertThat(injector).isNotNull();
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should support complete configuration lifecycle")
    void shouldSupportCompleteConfigurationLifecycle() {
      // GIVEN
      reconfigureWith(
          Map.of(
              "test.value", "initial-value",
              "custom.value", "custom-value"));

      // WHEN
      TestConfigManager configManager = new TestConfigManager();
      TestProperties testConfig = configManager.getConfiguration();
      CustomConfig customConfig = new CustomConfig();
      configManager.inject(customConfig);

      // THEN
      assertThat(testConfig.testValue).isEqualTo("initial-value");
      assertThat(customConfig.customValue).isEqualTo("custom-value");
      assertThat(configManager.getModuleName()).isEqualTo("TEST");
    }

    @Test
    @DisplayName("Should support custom converter registration in constructor")
    void shouldSupportCustomConverterRegistrationInConstructor() {
      // GIVEN
      reconfigureWith(Map.of("custom.type", "custom-value"));

      // WHEN
      ConfigManagerWithCustomConverter configManager = new ConfigManagerWithCustomConverter();
      PropertiesWithCustomType properties = configManager.getConfiguration();

      // THEN
      assertThat(properties.customType).isNotNull();
      assertThat(properties.customType.getValue()).isEqualTo("custom-value");
    }

    @Test
    @DisplayName("Should handle multiple config properties")
    void shouldHandleMultipleConfigProperties() {
      // GIVEN
      reconfigureWith(
          Map.of(
              "test.string", "string-value",
              "test.int", "42",
              "test.boolean", "true"));

      // WHEN
      ComplexConfigManager configManager = new ComplexConfigManager();
      ComplexProperties properties = configManager.getConfiguration();

      // THEN
      assertThat(properties.stringValue).isEqualTo("string-value");
      assertThat(properties.intValue).isEqualTo(42);
      assertThat(properties.booleanValue).isTrue();
    }
  }

  // Test helper classes (same as before)
  static class TestProperties {

    @Value("${test.value}")
    String testValue;
  }

  static class TestConfigManager extends BaseConfigManager<TestProperties> {

    @Override
    protected TestProperties createConfigProperties() {
      return new TestProperties();
    }

    @Override
    protected String getModuleName() {
      return "TEST";
    }
  }

  static class TestPropertiesWithDefaults {

    @Value("${test.value:default-value}")
    String valueWithDefault;
  }

  static class TestConfigManagerWithDefaults extends BaseConfigManager<TestPropertiesWithDefaults> {

    @Override
    protected TestPropertiesWithDefaults createConfigProperties() {
      return new TestPropertiesWithDefaults();
    }

    @Override
    protected String getModuleName() {
      return "TEST_DEFAULTS";
    }
  }

  static class CustomConfig {

    @Value("${custom.value}")
    String customValue;
  }

  static class CustomType {

    private final String value;

    public CustomType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  static class PropertiesWithCustomType {

    @Value("${custom.type}")
    CustomType customType;
  }

  static class ConfigManagerWithCustomConverter
      extends BaseConfigManager<PropertiesWithCustomType> {

    @Override
    protected void configureInjector(ConfigurationInjector injector) {
      injector.registerConverter(CustomType.class, CustomType::new);
    }

    @Override
    protected PropertiesWithCustomType createConfigProperties() {
      return new PropertiesWithCustomType();
    }

    @Override
    protected String getModuleName() {
      return "CUSTOM";
    }
  }

  static class ComplexProperties {

    @Value("${test.string}")
    String stringValue;

    @Value("${test.int}")
    int intValue;

    @Value("${test.boolean}")
    boolean booleanValue;
  }

  static class ComplexConfigManager extends BaseConfigManager<ComplexProperties> {

    @Override
    protected ComplexProperties createConfigProperties() {
      return new ComplexProperties();
    }

    @Override
    protected String getModuleName() {
      return "COMPLEX";
    }
  }
}
