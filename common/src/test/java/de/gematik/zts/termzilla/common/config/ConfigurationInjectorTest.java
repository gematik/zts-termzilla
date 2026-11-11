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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ConfigurationInjector}. Tests configuration injection, type conversion, and
 * error handling.
 */
@DisplayName("ConfigurationInjector Tests")
class ConfigurationInjectorTest {

  private SmallRyeConfig config;
  private ConfigurationInjector injector;
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
    config = new SmallRyeConfigBuilder().addDefaultSources().build();

    // Register the config
    ConfigProviderResolver.instance().registerConfig(config, classLoader);

    injector = new ConfigurationInjector();
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

    // Recreate injector to use new config
    injector = new ConfigurationInjector();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create injector with default converters registered")
    void shouldCreateInjectorWithDefaultConverters() {
      // GIVEN / WHEN
      ConfigurationInjector newInjector = new ConfigurationInjector();

      // THEN
      assertThat(newInjector).isNotNull();
    }
  }

  @Nested
  @DisplayName("registerConverter Tests")
  class RegisterConverterTests {

    @Test
    @DisplayName("Should register custom converter and use it for conversion")
    void shouldRegisterCustomConverter() {
      // GIVEN
      reconfigureWith(Map.of("test.custom", "custom-value"));

      injector.registerConverter(CustomType.class, CustomType::new);
      TestConfigWithCustomType testConfig = new TestConfigWithCustomType();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.customValue).isNotNull();
      assertThat(testConfig.customValue.value()).isEqualTo("custom-value");
    }

    @Test
    @DisplayName("Should override existing converter")
    void shouldOverrideExistingConverter() {
      // GIVEN
      reconfigureWith(Map.of("test.string", "value"));

      injector.registerConverter(String.class, value -> "overridden-" + value);
      TestConfig testConfig = new TestConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.stringValue).isEqualTo("overridden-value");
    }
  }

  @Nested
  @DisplayName("inject Tests")
  class InjectTests {

    @Test
    @DisplayName("Should inject string value from config")
    void shouldInjectStringValue() {
      // GIVEN
      reconfigureWith(Map.of("test.string", "test-value"));
      TestConfig testConfig = new TestConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.stringValue).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Should inject default value when config key not found")
    void shouldInjectDefaultValue() {
      // GIVEN
      reconfigureWith(Map.of());
      TestConfigWithDefaults testConfig = new TestConfigWithDefaults();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.valueWithDefault).isEqualTo("default-value");
    }

    @Test
    @DisplayName("Should inject values into inherited fields")
    void shouldInjectInheritedFields() {
      // GIVEN
      reconfigureWith(
          Map.of(
              "parent.value", "parent-value",
              "child.value", "child-value"));
      ChildConfig childConfig = new ChildConfig();

      // WHEN
      injector.inject(childConfig);

      // THEN
      assertThat(childConfig.parentValue).isEqualTo("parent-value");
      assertThat(childConfig.childValue).isEqualTo("child-value");
    }

    @Test
    @DisplayName("Should inject into private fields")
    void shouldInjectIntoPrivateFields() {
      // GIVEN
      reconfigureWith(Map.of("test.private", "private-value"));
      TestConfigWithPrivateField testConfig = new TestConfigWithPrivateField();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.getPrivateValue()).isEqualTo("private-value");
    }

    @Test
    @DisplayName("Should handle missing config gracefully with null for wrapper types")
    void shouldHandleMissingConfigWithNull() {
      // GIVEN
      reconfigureWith(Map.of());
      TestConfigNullable testConfig = new TestConfigNullable();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.nullableValue).isNull();
    }
  }

  @Nested
  @DisplayName("create Tests")
  class CreateTests {

    @Test
    @DisplayName("Should create instance and inject values")
    void shouldCreateInstanceAndInject() {
      // GIVEN
      reconfigureWith(Map.of("test.string", "injected-value"));

      // WHEN
      TestConfig result = injector.create(TestConfig.class);

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.stringValue).isEqualTo("injected-value");
    }

    @Test
    @DisplayName("Should throw PropertyInjectionException when class has no default constructor")
    void shouldThrowExceptionWhenNoDefaultConstructor() {
      // GIVEN / WHEN / THEN
      assertThatThrownBy(() -> injector.create(ConfigWithoutDefaultConstructor.class))
          .isInstanceOf(PropertyInjectionException.class)
          .hasMessageContaining("Failed to create instance");
    }
  }

  @Nested
  @DisplayName("Primitive Type Conversion Tests")
  class PrimitiveTypeConversionTests {

    @ParameterizedTest
    @CsvSource({"true, true", "false, false", "TRUE, true", "FALSE, false"})
    @DisplayName("Should convert boolean values")
    void shouldConvertBooleanValues(String configValue, boolean expected) {
      // GIVEN
      reconfigureWith(Map.of("test.boolean", configValue));
      TestPrimitiveConfig testConfig = new TestPrimitiveConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.booleanValue).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"42", "0", "-100", "2147483647"})
    @DisplayName("Should convert integer values")
    void shouldConvertIntegerValues(String configValue) {
      // GIVEN
      reconfigureWith(Map.of("test.int", configValue));
      TestPrimitiveConfig testConfig = new TestPrimitiveConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.intValue).isEqualTo(Integer.parseInt(configValue));
    }

    @Test
    @DisplayName("Should convert long values")
    void shouldConvertLongValues() {
      // GIVEN
      reconfigureWith(Map.of("test.long", "9223372036854775807"));
      TestPrimitiveConfig testConfig = new TestPrimitiveConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.longValue).isEqualTo(9223372036854775807L);
    }

    @Test
    @DisplayName("Should convert double values")
    void shouldConvertDoubleValues() {
      // GIVEN
      reconfigureWith(Map.of("test.double", "3.14159"));
      TestPrimitiveConfig testConfig = new TestPrimitiveConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.doubleValue).isEqualTo(3.14159);
    }

    @Test
    @DisplayName("Should use default primitive values when config is empty")
    void shouldUseDefaultPrimitiveValues() {
      // GIVEN
      reconfigureWith(Map.of());
      TestPrimitiveConfigWithDefaults testConfig = new TestPrimitiveConfigWithDefaults();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.intValue).isEqualTo(42);
      assertThat(testConfig.longValue).isEqualTo(100L);
      assertThat(testConfig.booleanValue).isTrue();
      assertThat(testConfig.doubleValue).isEqualTo(3.14);
    }
  }

  @Nested
  @DisplayName("Collection Type Conversion Tests")
  class CollectionTypeConversionTests {

    @Test
    @DisplayName("Should convert comma-separated string to List")
    void shouldConvertToList() {
      // GIVEN
      reconfigureWith(Map.of("test.list", "value1,value2,value3"));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.listValue).hasSize(3).containsExactly("value1", "value2", "value3");
    }

    @Test
    @DisplayName("Should convert to List with trimmed values")
    void shouldConvertToListWithTrimmedValues() {
      // GIVEN
      reconfigureWith(Map.of("test.list", " value1 , value2 , value3 "));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.listValue).containsExactly("value1", "value2", "value3");
    }

    @Test
    @DisplayName("Should convert to empty List when config is empty")
    void shouldConvertToEmptyList() {
      // GIVEN
      reconfigureWith(Map.of("test.list", ""));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.listValue).isEmpty();
    }

    @Test
    @DisplayName("Should convert to Set")
    void shouldConvertToSet() {
      // GIVEN
      reconfigureWith(Map.of("test.set", "value1,value2,value3"));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.setValue)
          .hasSize(3)
          .containsExactlyInAnyOrder("value1", "value2", "value3");
    }

    @Test
    @DisplayName("Should convert to Set with duplicates removed")
    void shouldConvertToSetWithDuplicatesRemoved() {
      // GIVEN
      reconfigureWith(Map.of("test.set", "value1,value2,value1,value3"));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.setValue)
          .hasSize(3)
          .containsExactlyInAnyOrder("value1", "value2", "value3");
    }

    @Test
    @DisplayName("Should convert to List of Integers")
    void shouldConvertToListOfIntegers() {
      // GIVEN
      reconfigureWith(Map.of("test.intList", "1,2,3,4,5"));
      TestCollectionConfig testConfig = new TestCollectionConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.intListValue).hasSize(5).containsExactly(1, 2, 3, 4, 5);
    }
  }

  @Nested
  @DisplayName("Map Type Conversion Tests")
  class MapTypeConversionTests {

    @Test
    @DisplayName("Should convert to Map with String keys and values")
    void shouldConvertToMap() {
      // GIVEN
      reconfigureWith(Map.of("test.map", "key1=value1,key2=value2,key3=value3"));
      TestMapConfig testConfig = new TestMapConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.mapValue)
          .hasSize(3)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2")
          .containsEntry("key3", "value3");
    }

    @Test
    @DisplayName("Should convert to Map with trimmed keys and values")
    void shouldConvertToMapWithTrimmedValues() {
      // GIVEN
      reconfigureWith(Map.of("test.map", " key1 = value1 , key2 = value2 "));
      TestMapConfig testConfig = new TestMapConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.mapValue)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("Should convert to empty Map when config is empty")
    void shouldConvertToEmptyMap() {
      // GIVEN
      reconfigureWith(Map.of("test.map", ""));
      TestMapConfig testConfig = new TestMapConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.mapValue).isEmpty();
    }

    @Test
    @DisplayName("Should convert to Map with Integer keys and values")
    void shouldConvertToMapWithIntegerTypes() {
      // GIVEN
      reconfigureWith(Map.of("test.intMap", "1=100,2=200,3=300"));
      TestMapConfig testConfig = new TestMapConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.intMapValue)
          .hasSize(3)
          .containsEntry(1, 100)
          .containsEntry(2, 200)
          .containsEntry(3, 300);
    }

    @Test
    @DisplayName("Should skip invalid map entries without equals sign")
    void shouldSkipInvalidMapEntries() {
      // GIVEN
      reconfigureWith(Map.of("test.map", "key1=value1,invalid,key2=value2"));
      TestMapConfig testConfig = new TestMapConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.mapValue)
          .hasSize(2)
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2");
    }
  }

  @Nested
  @DisplayName("Array Type Conversion Tests")
  class ArrayTypeConversionTests {

    @Test
    @DisplayName("Should convert to String array")
    void shouldConvertToStringArray() {
      // GIVEN
      reconfigureWith(Map.of("test.stringArray", "value1,value2,value3"));
      TestArrayConfig testConfig = new TestArrayConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.stringArray).hasSize(3).containsExactly("value1", "value2", "value3");
    }

    @Test
    @DisplayName("Should convert to int array")
    void shouldConvertToIntArray() {
      // GIVEN
      reconfigureWith(Map.of("test.intArray", "1,2,3,4,5"));
      TestArrayConfig testConfig = new TestArrayConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.intArray).hasSize(5).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Should convert to empty array")
    void shouldConvertToEmptyArray() {
      // GIVEN
      reconfigureWith(Map.of("test.stringArray", ""));
      TestArrayConfig testConfig = new TestArrayConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.stringArray).isEmpty();
    }
  }

  @Nested
  @DisplayName("Enum Type Conversion Tests")
  class EnumTypeConversionTests {

    @Test
    @DisplayName("Should convert to enum value")
    void shouldConvertToEnum() {
      // GIVEN
      reconfigureWith(Map.of("test.enum", "VALUE_ONE"));
      TestEnumConfig testConfig = new TestEnumConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.enumValue).isEqualTo(TestEnum.VALUE_ONE);
    }

    @Test
    @DisplayName("Should convert to enum value case-insensitive")
    void shouldConvertToEnumCaseInsensitive() {
      // GIVEN
      reconfigureWith(Map.of("test.enum", "value_two"));
      TestEnumConfig testConfig = new TestEnumConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.enumValue).isEqualTo(TestEnum.VALUE_TWO);
    }

    @Test
    @DisplayName("Should throw PropertyInjectionException for invalid enum value")
    void shouldThrowExceptionForInvalidEnumValue() {
      // GIVEN
      reconfigureWith(Map.of("test.enum", "INVALID_VALUE"));
      TestEnumConfig testConfig = new TestEnumConfig();

      // WHEN / THEN
      assertThatThrownBy(() -> injector.inject(testConfig))
          .isInstanceOf(PropertyInjectionException.class)
          .hasMessageContaining("Failed to convert")
          .hasMessageContaining("to enum")
          .hasMessageContaining("Valid values:");
    }
  }

  @Nested
  @DisplayName("Special Type Conversion Tests")
  class SpecialTypeConversionTests {

    @Test
    @DisplayName("Should convert to Path")
    void shouldConvertToPath() {
      // GIVEN
      reconfigureWith(Map.of("test.path", "/tmp/test"));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.pathValue).isEqualTo(Paths.get("/tmp/test"));
    }

    @Test
    @DisplayName("Should convert to File")
    void shouldConvertToFile() {
      // GIVEN
      reconfigureWith(Map.of("test.file", "/tmp/test.txt"));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.fileValue).isEqualTo(new File("/tmp/test.txt"));
    }

    @Test
    @DisplayName("Should convert to URI")
    void shouldConvertToURI() {
      // GIVEN
      reconfigureWith(Map.of("test.uri", "https://example.com"));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.uriValue).isEqualTo(URI.create("https://example.com"));
    }

    @Test
    @DisplayName("Should convert to Duration")
    void shouldConvertToDuration() {
      // GIVEN
      reconfigureWith(Map.of("test.duration", "PT1H30M"));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.durationValue).isEqualTo(Duration.parse("PT1H30M"));
    }

    @Test
    @DisplayName("Should convert to LocalDate")
    void shouldConvertToLocalDate() {
      // GIVEN
      reconfigureWith(Map.of("test.localDate", "2024-01-15"));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.localDateValue).isEqualTo(LocalDate.parse("2024-01-15"));
    }

    @Test
    @DisplayName("Should convert to UUID")
    void shouldConvertToUUID() {
      // GIVEN
      String uuidString = "550e8400-e29b-41d4-a716-446655440000";
      reconfigureWith(Map.of("test.uuid", uuidString));
      TestSpecialTypesConfig testConfig = new TestSpecialTypesConfig();

      // WHEN
      injector.inject(testConfig);

      // THEN
      assertThat(testConfig.uuidValue).isEqualTo(UUID.fromString(uuidString));
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName(
        "Should throw PropertyInjectionException wrapping UnsupportedOperationException for unsupported type")
    void shouldThrowExceptionForUnsupportedType() {
      // GIVEN
      reconfigureWith(Map.of("test.unsupported", "value"));
      TestUnsupportedTypeConfig testConfig = new TestUnsupportedTypeConfig();

      // WHEN / THEN
      assertThatThrownBy(() -> injector.inject(testConfig))
          .isInstanceOf(PropertyInjectionException.class)
          .hasMessageContaining("Failed to get config value for key: test.unsupported")
          .hasCauseInstanceOf(UnsupportedOperationException.class)
          .hasRootCauseMessage(
              "Unsupported type: class de.gematik.zts.termzilla.common.config.ConfigurationInjectorTest$UnsupportedType. Register a custom converter or use a supported type.");
    }

    @Test
    @DisplayName("Should throw PropertyInjectionException when converter fails")
    void shouldThrowExceptionWhenConverterFails() {
      // GIVEN
      reconfigureWith(Map.of("test.custom", "value"));

      injector.registerConverter(
          CustomType.class,
          value -> {
            throw new RuntimeException("Conversion failed");
          });

      TestConfigWithCustomType testConfig = new TestConfigWithCustomType();

      // WHEN / THEN
      assertThatThrownBy(() -> injector.inject(testConfig))
          .isInstanceOf(PropertyInjectionException.class)
          .hasMessageContaining("Failed to get config value")
          .hasCauseInstanceOf(PropertyInjectionException.class);

      // Verify the cause chain contains our conversion error
      assertThatThrownBy(() -> injector.inject(testConfig))
          .isInstanceOf(PropertyInjectionException.class)
          .cause()
          .hasMessageContaining(
              "Failed to convert value 'value' for key 'test.custom' to type CustomType")
          .hasCauseInstanceOf(RuntimeException.class)
          .hasRootCauseMessage("Conversion failed");
    }

    @Test
    @DisplayName("Should throw PropertyInjectionException for invalid number format")
    void shouldThrowExceptionForInvalidNumberFormat() {
      // GIVEN
      reconfigureWith(Map.of("test.int", "not-a-number"));
      TestPrimitiveConfig testConfig = new TestPrimitiveConfig();

      // WHEN / THEN
      assertThatThrownBy(() -> injector.inject(testConfig))
          .isInstanceOf(PropertyInjectionException.class);
    }
  }

  // Test helper classes (same as before)
  static class TestConfig {

    @Value("${test.string}")
    String stringValue;
  }

  static class TestConfigWithDefaults {

    @Value("${test.missing:default-value}")
    String valueWithDefault;

    @Value("${test.int:42}")
    int intWithDefault;
  }

  static class TestConfigNullable {

    @Value("${test.nullable}")
    String nullableValue;
  }

  static class ParentConfig {

    @Value("${parent.value}")
    String parentValue;
  }

  static class ChildConfig extends ParentConfig {

    @Value("${child.value}")
    String childValue;
  }

  static class TestConfigWithPrivateField {

    @Value("${test.private}")
    private String privateValue;

    public String getPrivateValue() {
      return privateValue;
    }
  }

  static class ConfigWithoutDefaultConstructor {

    public ConfigWithoutDefaultConstructor(String value) {
      // Constructor with parameter to prevent default constructor
    }
  }

  static class TestPrimitiveConfig {

    @Value("${test.boolean}")
    boolean booleanValue;

    @Value("${test.int}")
    int intValue;

    @Value("${test.long}")
    long longValue;

    @Value("${test.double}")
    double doubleValue;
  }

  static class TestPrimitiveConfigWithDefaults {

    @Value("${test.int:42}")
    int intValue;

    @Value("${test.long:100}")
    long longValue;

    @Value("${test.boolean:true}")
    boolean booleanValue;

    @Value("${test.double:3.14}")
    double doubleValue;
  }

  static class TestCollectionConfig {

    @Value("${test.list}")
    List<String> listValue;

    @Value("${test.set}")
    Set<String> setValue;

    @Value("${test.intList}")
    List<Integer> intListValue;
  }

  static class TestMapConfig {

    @Value("${test.map}")
    Map<String, String> mapValue;

    @Value("${test.intMap}")
    Map<Integer, Integer> intMapValue;
  }

  static class TestArrayConfig {

    @Value("${test.stringArray}")
    String[] stringArray;

    @Value("${test.intArray}")
    int[] intArray;
  }

  enum TestEnum {
    VALUE_ONE,
    VALUE_TWO,
    VALUE_THREE
  }

  static class TestEnumConfig {

    @Value("${test.enum}")
    TestEnum enumValue;
  }

  static class TestSpecialTypesConfig {

    @Value("${test.path}")
    Path pathValue;

    @Value("${test.file}")
    File fileValue;

    @Value("${test.uri}")
    URI uriValue;

    @Value("${test.duration}")
    Duration durationValue;

    @Value("${test.localDate}")
    LocalDate localDateValue;

    @Value("${test.uuid}")
    UUID uuidValue;
  }

  record CustomType(String value) {}

  static class TestConfigWithCustomType {

    @Value("${test.custom}")
    CustomType customValue;
  }

  static class UnsupportedType {
    // Intentionally unsupported type
  }

  static class TestUnsupportedTypeConfig {

    @Value("${test.unsupported}")
    UnsupportedType unsupportedValue;
  }
}
