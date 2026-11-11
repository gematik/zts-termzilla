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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Injects configuration values from MicroProfile Config into fields annotated with {@link Value}.
 * Supports automatic type conversion for primitives, collections, maps, arrays, and custom types.
 *
 * <p>Provides default converters for common Java types and allows registration of custom converters
 * for application-specific types.
 */
public class ConfigurationInjector {

  private final Config config;
  private final Map<Class<?>, TypeConverter<?>> converters;

  /** Creates a new configuration injector with default type converters registered. */
  public ConfigurationInjector() {
    this.config = ConfigProvider.getConfig();
    this.converters = new HashMap<>();
    registerDefaultConverters();
  }

  /**
   * Registers default type converters for common Java types including primitives, file system
   * types, network types, temporal types, and other standard types.
   */
  private void registerDefaultConverters() {
    // String (identity)
    registerConverter(String.class, value -> value);

    // Primitives and wrappers
    registerConverter(Integer.class, Integer::parseInt);
    registerConverter(int.class, Integer::parseInt);
    registerConverter(Long.class, Long::parseLong);
    registerConverter(long.class, Long::parseLong);
    registerConverter(Boolean.class, Boolean::parseBoolean);
    registerConverter(boolean.class, Boolean::parseBoolean);
    registerConverter(Double.class, Double::parseDouble);
    registerConverter(double.class, Double::parseDouble);
    registerConverter(Float.class, Float::parseFloat);
    registerConverter(float.class, Float::parseFloat);
    registerConverter(Short.class, Short::parseShort);
    registerConverter(short.class, Short::parseShort);
    registerConverter(Byte.class, Byte::parseByte);
    registerConverter(byte.class, Byte::parseByte);
    registerConverter(Character.class, value -> value.isEmpty() ? null : value.charAt(0));
    registerConverter(char.class, value -> value.isEmpty() ? '\0' : value.charAt(0));

    // File system
    registerConverter(Path.class, Paths::get);
    registerConverter(File.class, File::new);

    // Network
    registerConverter(URI.class, URI::create);
    registerConverter(
        URL.class,
        value -> {
          try {
            return new URI(value).toURL();
          } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + value, e);
          }
        });

    // Time
    registerConverter(Duration.class, Duration::parse);
    registerConverter(Period.class, Period::parse);
    registerConverter(LocalDate.class, LocalDate::parse);
    registerConverter(LocalDateTime.class, LocalDateTime::parse);

    // Other common types
    registerConverter(Charset.class, Charset::forName);
    registerConverter(Pattern.class, Pattern::compile);
    registerConverter(UUID.class, UUID::fromString);
  }

  /**
   * Registers a custom type converter for a specific type.
   *
   * @param type the target type for conversion
   * @param converter the converter function
   * @param <T> the type parameter
   */
  public <T> void registerConverter(Class<T> type, TypeConverter<T> converter) {
    converters.put(type, converter);
  }

  /**
   * Injects configuration values into all fields annotated with {@link Value}.
   *
   * @param instance the object instance to inject into
   * @param <T> the type of the instance
   * @return the same instance with injected values
   * @throws PropertyInjectionException if injection fails
   */
  public <T> T inject(T instance) {
    Class<?> clazz = instance.getClass();

    for (Field field : getAllFields(clazz)) {
      if (field.isAnnotationPresent(Value.class)) {
        injectField(instance, field);
      }
    }

    return instance;
  }

  /**
   * Creates a new instance of the specified class and injects configuration values.
   *
   * @param clazz the class to instantiate
   * @param <T> the type parameter
   * @return a new instance with injected configuration values
   * @throws PropertyInjectionException if instance creation or injection fails
   */
  public <T> T create(Class<T> clazz) {
    try {
      T instance = clazz.getDeclaredConstructor().newInstance();
      return inject(instance);
    } catch (Exception e) {
      throw new PropertyInjectionException("Failed to create instance of " + clazz.getName(), e);
    }
  }

  /**
   * Retrieves all fields from the class hierarchy, excluding {@link Object}.
   *
   * @param clazz the class to inspect
   * @return list of all declared fields in the class hierarchy
   */
  private List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    while (clazz != null && clazz != Object.class) {
      fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    return fields;
  }

  /**
   * Injects a configuration value into a single field.
   *
   * @param instance the object instance
   * @param field the field to inject into
   * @throws PropertyInjectionException if injection fails
   */
  @SuppressWarnings("java:S3011") // Reflection is needed for configuration injection
  private void injectField(Object instance, Field field) {
    Value annotation = field.getAnnotation(Value.class);
    String expression = annotation.value();

    PropertyExpression parsed = PropertyExpression.parse(expression);

    Object value = getConfigValue(parsed.key(), field, parsed.defaultValue());

    field.setAccessible(true);
    try {
      field.set(instance, value);
    } catch (IllegalAccessException e) {
      throw new PropertyInjectionException("Failed to inject field: " + field.getName(), e);
    } catch (IllegalArgumentException e) {
      throw new PropertyInjectionException(
          "Failed to inject field: "
              + field.getName()
              + " - type mismatch. Expected: "
              + field.getType()
              + ", got: "
              + (value != null ? value.getClass() : "null"),
          e);
    }
  }

  /**
   * Retrieves and converts a configuration value for the specified field.
   *
   * @param key the configuration key
   * @param field the target field
   * @param defaultValue the default value if configuration is not found
   * @return the converted configuration value
   * @throws PropertyInjectionException if value retrieval or conversion fails
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object getConfigValue(String key, Field field, String defaultValue) {
    Class<?> targetType = field.getType();

    try {
      return resolveConfigValue(key, field, defaultValue, targetType);
    } catch (IllegalArgumentException e) {
      throw handleIllegalArgumentException(e, key, defaultValue, targetType);
    } catch (Exception e) {
      return handleGeneralException(e, key, field, defaultValue, targetType);
    }
  }

  /**
   * Resolves the configuration value based on the target type.
   *
   * @param key the configuration key
   * @param field the target field
   * @param defaultValue the default value
   * @param targetType the target type for conversion
   * @return the resolved and converted value
   */
  private Object resolveConfigValue(
      String key, Field field, String defaultValue, Class<?> targetType) {
    // Check if we have a registered converter
    if (converters.containsKey(targetType)) {
      return handleConverterType(key, defaultValue, targetType);
    }

    // Handle collections
    if (List.class.isAssignableFrom(targetType)) {
      return getListValue(key, field, defaultValue);
    }

    if (Set.class.isAssignableFrom(targetType)) {
      List<?> list = getListValue(key, field, defaultValue);
      return new LinkedHashSet<>(list);
    }

    if (Map.class.isAssignableFrom(targetType)) {
      return getMapValue(key, field, defaultValue);
    }

    // Handle enums
    if (targetType.isEnum()) {
      return handleEnumType(key, defaultValue, targetType);
    }

    // Handle arrays
    if (targetType.isArray()) {
      return getArrayValue(key, field, defaultValue);
    }

    throw new UnsupportedOperationException(
        "Unsupported type: "
            + targetType
            + ". Register a custom converter or use a supported type.");
  }

  /**
   * Handles conversion using a registered converter.
   *
   * @param key the configuration key
   * @param defaultValue the default value
   * @param targetType the target type
   * @return the converted value
   */
  private Object handleConverterType(String key, String defaultValue, Class<?> targetType) {
    String value = config.getOptionalValue(key, String.class).orElse(defaultValue);
    if (value == null || value.isEmpty()) {
      return getDefaultForType(targetType);
    }
    return convertWithConverter(converters.get(targetType), value, key, targetType);
  }

  /**
   * Handles enum type conversion.
   *
   * @param key the configuration key
   * @param defaultValue the default value
   * @param targetType the enum type
   * @return the enum constant or {@code null}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object handleEnumType(String key, String defaultValue, Class<?> targetType) {
    String value = config.getOptionalValue(key, String.class).orElse(defaultValue);
    if (value != null && !value.isEmpty()) {
      return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
    }
    return null;
  }

  /**
   * Handles {@link IllegalArgumentException} during conversion with enhanced error messages.
   *
   * @param e the original exception
   * @param key the configuration key
   * @param defaultValue the default value
   * @param targetType the target type
   * @return a {@link PropertyInjectionException} with detailed error information
   */
  private PropertyInjectionException handleIllegalArgumentException(
      IllegalArgumentException e, String key, String defaultValue, Class<?> targetType) {

    if (targetType.isEnum()) {
      String value = config.getOptionalValue(key, String.class).orElse(defaultValue);
      return new PropertyInjectionException(
          "Failed to convert '"
              + value
              + "' to enum "
              + targetType.getSimpleName()
              + " for key: "
              + key
              + ". Valid values: "
              + Arrays.toString(targetType.getEnumConstants()),
          e);
    }

    return new PropertyInjectionException(
        "Failed to convert value for key: " + key + ", target type: " + targetType.getSimpleName(),
        e);
  }

  /**
   * Handles general exceptions during conversion with fallback to default value.
   *
   * @param e the original exception
   * @param key the configuration key
   * @param field the target field
   * @param defaultValue the default value
   * @param targetType the target type
   * @return the converted default value
   * @throws PropertyInjectionException if conversion fails
   */
  private Object handleGeneralException(
      Exception e, String key, Field field, String defaultValue, Class<?> targetType) {

    if (defaultValue != null) {
      try {
        return convertValue(defaultValue, targetType);
      } catch (Exception ex) {
        throw new PropertyInjectionException(
            "Failed to convert default value '" + defaultValue + "' for key: " + key, ex);
      }
    }

    throw new PropertyInjectionException(
        "Failed to get config value for key: "
            + key
            + ", field: "
            + field.getName()
            + ", type: "
            + targetType.getSimpleName(),
        e);
  }

  /** Default values for primitive types. */
  private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS =
      Map.of(
          boolean.class,
          false,
          char.class,
          '\0',
          byte.class,
          (byte) 0,
          short.class,
          (short) 0,
          int.class,
          0,
          long.class,
          0L,
          float.class,
          0.0f,
          double.class,
          0.0);

  /**
   * Returns the default value for primitive types.
   *
   * @param type the primitive type
   * @return the default value or {@code null} for non-primitive types
   */
  private Object getDefaultForType(Class<?> type) {
    return PRIMITIVE_DEFAULTS.get(type);
  }

  /**
   * Retrieves and converts a comma-separated list value.
   *
   * @param key the configuration key
   * @param field the target field with generic type information
   * @param defaultValue the default value
   * @return a list of converted elements
   */
  private List<?> getListValue(String key, Field field, String defaultValue) {
    Optional<String> value = config.getOptionalValue(key, String.class);

    String stringValue = value.orElse(defaultValue);
    if (stringValue == null || stringValue.isEmpty()) {
      return Collections.emptyList();
    }

    // Get the generic type of the list
    Type genericType = field.getGenericType();
    Class<?> elementType = String.class;

    if (genericType instanceof ParameterizedType paramType) {
      Type[] typeArgs = paramType.getActualTypeArguments();
      if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
        elementType = (Class<?>) typeArgs[0];
      }
    }

    // Split by comma and convert
    String[] parts = stringValue.split(",");
    List<Object> result = new ArrayList<>();

    for (String part : parts) {
      part = part.trim();
      if (!part.isEmpty()) {
        result.add(convertValue(part, elementType));
      }
    }

    return result;
  }

  /**
   * Retrieves and converts a map value from format {@code key1=value1,key2=value2}.
   *
   * @param key the configuration key
   * @param field the target field with generic type information
   * @param defaultValue the default value
   * @return a map with converted keys and values
   */
  private Map<?, ?> getMapValue(String key, Field field, String defaultValue) {
    String stringValue = config.getOptionalValue(key, String.class).orElse(defaultValue);

    if (stringValue == null || stringValue.isEmpty()) {
      return Collections.emptyMap();
    }

    MapTypeInfo typeInfo = extractMapTypeInfo(field);
    return parseMapFromString(stringValue, typeInfo.keyType, typeInfo.valueType);
  }

  /**
   * Extracts generic type information from a map field.
   *
   * @param field the map field
   * @return type information for keys and values
   */
  private MapTypeInfo extractMapTypeInfo(Field field) {
    Type genericType = field.getGenericType();
    Class<?> keyType = String.class;
    Class<?> valueType = String.class;

    if (genericType instanceof ParameterizedType paramType) {
      Type[] typeArgs = paramType.getActualTypeArguments();
      if (typeArgs.length >= 2) {
        if (typeArgs[0] instanceof Class) {
          keyType = (Class<?>) typeArgs[0];
        }
        if (typeArgs[1] instanceof Class) {
          valueType = (Class<?>) typeArgs[1];
        }
      }
    }

    return new MapTypeInfo(keyType, valueType);
  }

  /**
   * Parses a string into a map with converted keys and values.
   *
   * @param stringValue the string in format {@code key1=value1,key2=value2}
   * @param keyType the target type for keys
   * @param valueType the target type for values
   * @return the parsed and converted map
   */
  private Map<Object, Object> parseMapFromString(
      String stringValue, Class<?> keyType, Class<?> valueType) {
    Map<Object, Object> result = new LinkedHashMap<>();

    for (String pair : stringValue.split(",")) {
      String trimmedPair = pair.trim();
      if (!trimmedPair.isEmpty()) {
        String[] kv = trimmedPair.split("=", 2);
        if (kv.length == 2) {
          Object k = convertValue(kv[0].trim(), keyType);
          Object v = convertValue(kv[1].trim(), valueType);
          result.put(k, v);
        }
      }
    }

    return result;
  }

  /** Holds generic type information for map fields. */
  private record MapTypeInfo(Class<?> keyType, Class<?> valueType) {}

  /**
   * Retrieves and converts an array value from a comma-separated string.
   *
   * @param key the configuration key
   * @param field the target array field
   * @param defaultValue the default value
   * @return an array with converted elements
   */
  private Object getArrayValue(String key, Field field, String defaultValue) {
    Class<?> componentType = field.getType().getComponentType();
    List<?> list = getListValueForType(key, componentType, defaultValue);

    Object array = java.lang.reflect.Array.newInstance(componentType, list.size());
    for (int i = 0; i < list.size(); i++) {
      java.lang.reflect.Array.set(array, i, list.get(i));
    }
    return array;
  }

  /**
   * Retrieves and converts a list value for a specific element type.
   *
   * @param key the configuration key
   * @param elementType the target element type
   * @param defaultValue the default value
   * @return a list of converted elements
   */
  private List<?> getListValueForType(String key, Class<?> elementType, String defaultValue) {
    Optional<String> value = config.getOptionalValue(key, String.class);
    String stringValue = value.orElse(defaultValue);

    if (stringValue == null || stringValue.isEmpty()) {
      return Collections.emptyList();
    }

    String[] parts = stringValue.split(",");
    List<Object> result = new ArrayList<>();

    for (String part : parts) {
      part = part.trim();
      if (!part.isEmpty()) {
        result.add(convertValue(part, elementType));
      }
    }

    return result;
  }

  /**
   * Converts a string value to the specified target type.
   *
   * @param value the string value to convert
   * @param targetType the target type
   * @return the converted value
   * @throws PropertyInjectionException if conversion fails
   * @throws UnsupportedOperationException if no converter is registered for the type
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object convertValue(String value, Class<?> targetType) {
    if (value == null) {
      return null;
    }

    // Use registered converter if available
    if (converters.containsKey(targetType)) {
      return convertWithConverter(converters.get(targetType), value, value, targetType);
    }

    // Handle enums
    if (targetType.isEnum()) {
      try {
        return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new PropertyInjectionException(
            "Failed to convert value '"
                + value
                + "' to enum "
                + targetType.getSimpleName()
                + ". Valid values: "
                + Arrays.toString(targetType.getEnumConstants()),
            e);
      }
    }

    throw new UnsupportedOperationException(
        "Cannot convert to type: " + targetType + ". Register a custom converter.");
  }

  /** Parses property expressions in the format {@code ${key:defaultValue}} or {@code ${key}}. */
  private record PropertyExpression(String key, String defaultValue) {

    /**
     * Parses a property expression string.
     *
     * @param expression the expression to parse
     * @return the parsed property expression
     */
    public static PropertyExpression parse(String expression) {
      if (expression.startsWith("${") && expression.endsWith("}")) {
        String content = expression.substring(2, expression.length() - 1);
        int colonIndex = content.indexOf(':');

        if (colonIndex > 0) {
          return new PropertyExpression(
              content.substring(0, colonIndex).trim(), content.substring(colonIndex + 1).trim());
        }
        return new PropertyExpression(content.trim(), null);
      }
      return new PropertyExpression(expression, null);
    }
  }

  /**
   * Converts a value using the provided converter with exception handling.
   *
   * @param converter the type converter
   * @param value the string value to convert
   * @param key the configuration key (for error messages)
   * @param targetType the target type (for error messages)
   * @return the converted value
   * @throws PropertyInjectionException if conversion fails
   */
  private Object convertWithConverter(
      TypeConverter<?> converter, String value, String key, Class<?> targetType) {
    try {
      return converter.convert(value);
    } catch (PropertyInjectionException e) {
      throw e; // Re-throw as-is
    } catch (Exception e) {
      throw new PropertyInjectionException(
          "Failed to convert value '"
              + value
              + "' for key '"
              + key
              + "' to type "
              + targetType.getSimpleName(),
          e);
    }
  }

  /**
   * Functional interface for converting string values to specific types.
   *
   * @param <T> the target type
   */
  @FunctionalInterface
  public interface TypeConverter<T> {

    /**
     * Converts a string value to the target type.
     *
     * @param value the string value to convert
     * @return the converted value
     * @throws PropertyInjectionException if conversion fails
     */
    T convert(String value) throws PropertyInjectionException;
  }
}
