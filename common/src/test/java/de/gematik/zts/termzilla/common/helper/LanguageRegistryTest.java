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
package de.gematik.zts.termzilla.common.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link LanguageRegistry}.
 *
 * <p>This test class verifies the functionality of the language registry including:
 *
 * <ul>
 *   <li>Language code validation
 *   <li>Language variant resolution
 *   <li>Base language extraction
 *   <li>Language hierarchy generation for fallback scenarios
 * </ul>
 */
@DisplayName("LanguageRegistry Tests")
class LanguageRegistryTest {

  // Test constants for commonly used language codes
  private static final String GERMAN_BASE = "de";
  private static final String GERMAN_AUSTRIA = "de-AT";
  private static final String SERBIAN_CYRILLIC = "sr-Cyrl-BA";
  private static final String UNKNOWN_LANGUAGE = "xx-YY";

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw AssertionError when attempting to instantiate utility class")
    void shouldThrowAssertionErrorWhenInstantiating() {
      // GIVEN
      // A utility class that should not be instantiated

      // WHEN & THEN
      assertThatThrownBy(
              () -> {
                java.lang.reflect.Constructor<LanguageRegistry> constructor =
                    LanguageRegistry.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
              })
          .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
          .hasCauseInstanceOf(AssertionError.class)
          .cause()
          .hasMessageContaining("utility class");
    }
  }

  @Nested
  @DisplayName("isSupported() Tests")
  class IsSupportedTests {

    @ParameterizedTest
    @DisplayName("Should return true for supported base languages")
    @ValueSource(strings = {"de", "en", "fr", "es", "it", "pt", "ru", "zh", "ja", "ko"})
    void shouldReturnTrueForSupportedBaseLanguages(String languageCode) {
      // GIVEN
      // A supported base language code

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported).as("Language code '%s' should be supported", languageCode).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should return true for supported language variants")
    @ValueSource(strings = {"de-AT", "de-CH", "en-US", "en-GB", "fr-FR", "es-ES", "pt-BR"})
    void shouldReturnTrueForSupportedLanguageVariants(String languageCode) {
      // GIVEN
      // A supported language variant code

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported)
          .as("Language variant '%s' should be supported", languageCode)
          .isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should return true for supported language with script")
    @ValueSource(strings = {"sr-Cyrl-BA", "az-Cyrl-AZ", "uz-Cyrl-UZ"})
    void shouldReturnTrueForSupportedLanguagesWithScript(String languageCode) {
      // GIVEN
      // A supported language code with script specification

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported)
          .as("Language code with script '%s' should be supported", languageCode)
          .isTrue();
    }

    @ParameterizedTest
    @DisplayName("Should return false for unsupported language codes")
    @ValueSource(strings = {"xx", "yy-ZZ", "unknown", "test-TEST"})
    void shouldReturnFalseForUnsupportedLanguageCodes(String languageCode) {
      // GIVEN
      // An unsupported language code

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported)
          .as("Language code '%s' should not be supported", languageCode)
          .isFalse();
    }

    @Test
    @DisplayName("Should return false for null language code")
    void shouldReturnFalseForNullLanguageCode() {
      // GIVEN
      String languageCode = null;

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported).as("Null language code should not be supported").isFalse();
    }

    @Test
    @DisplayName("Should return false for empty language code")
    void shouldReturnFalseForEmptyLanguageCode() {
      // GIVEN
      String languageCode = "";

      // WHEN
      boolean isSupported = LanguageRegistry.isSupported(languageCode);

      // THEN
      assertThat(isSupported).as("Empty language code should not be supported").isFalse();
    }
  }

  @Nested
  @DisplayName("getLanguageVariants() Tests")
  class GetLanguageVariantsTests {

    @Test
    @DisplayName("Should return all German variants including base language")
    void shouldReturnAllGermanVariants() {
      // GIVEN
      String baseLanguage = GERMAN_BASE;

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // THEN
      assertThat(variants)
          .as("German variants should include base and all regional variants")
          .containsExactlyInAnyOrder("de", "de-DE", "de-AT", "de-CH", "de-LI", "de-LU");
    }

    @Test
    @DisplayName("Should return all English variants including base language")
    void shouldReturnAllEnglishVariants() {
      // GIVEN
      String baseLanguage = "en";

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // THEN
      assertThat(variants)
          .as("English variants should include base and all regional variants")
          .contains("en", "en-US", "en-GB", "en-AU", "en-CA", "en-IE")
          .hasSizeGreaterThan(10);
    }

    @Test
    @DisplayName("Should return all Spanish variants")
    void shouldReturnAllSpanishVariants() {
      // GIVEN
      String baseLanguage = "es";

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // THEN
      assertThat(variants)
          .as("Spanish variants should include many Latin American countries")
          .contains("es", "es-ES", "es-MX", "es-AR", "es-CO", "es-CL")
          .hasSizeGreaterThan(15);
    }

    @Test
    @DisplayName("Should return variants for language with script specification")
    void shouldReturnVariantsForLanguageWithScript() {
      // GIVEN
      String baseLanguage = "az"; // Azerbaijani

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // THEN
      assertThat(variants)
          .as("Azerbaijani variants should include Cyrillic script variant")
          .containsExactlyInAnyOrder("az", "az-AZ", "az-Cyrl-AZ");
    }

    @Test
    @DisplayName("Should return empty set for unsupported base language")
    void shouldReturnEmptySetForUnsupportedBaseLanguage() {
      // GIVEN
      String unsupportedLanguage = UNKNOWN_LANGUAGE;

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(unsupportedLanguage);

      // THEN
      assertThat(variants).as("Unsupported language should return empty set").isEmpty();
    }

    @Test
    @DisplayName("Should return empty set for null base language")
    void shouldReturnEmptySetForNullBaseLanguage() {
      // GIVEN
      String languageCode = null;

      // WHEN
      Set<String> variants = LanguageRegistry.getLanguageVariants(languageCode);

      // THEN
      assertThat(variants).as("Null language code should return empty set").isEmpty();
    }

    @Test
    @DisplayName("Should return immutable set")
    void shouldReturnImmutableSet() {
      // GIVEN
      String baseLanguage = GERMAN_BASE;
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // WHEN & THEN
      assertThatThrownBy(() -> variants.add("de-XX"))
          .as("Returned set should be immutable")
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @DisplayName("Should return consistent results for multiple calls")
    @ValueSource(strings = {"de", "en", "fr", "es"})
    void shouldReturnConsistentResultsForMultipleCalls(String baseLanguage) {
      // GIVEN
      // A base language code

      // WHEN
      Set<String> firstCall = LanguageRegistry.getLanguageVariants(baseLanguage);
      Set<String> secondCall = LanguageRegistry.getLanguageVariants(baseLanguage);

      // THEN
      assertThat(firstCall).as("Multiple calls should return same set").isEqualTo(secondCall);
    }
  }

  @Nested
  @DisplayName("getBaseLanguage() Tests")
  class GetBaseLanguageTests {

    @ParameterizedTest
    @DisplayName("Should extract base language from regional variants")
    @CsvSource({
      "de-AT, de",
      "de-CH, de",
      "de-DE, de",
      "en-US, en",
      "en-GB, en",
      "fr-FR, fr",
      "es-ES, es",
      "pt-BR, pt"
    })
    void shouldExtractBaseLanguageFromRegionalVariants(String variant, String expectedBase) {
      // GIVEN
      // A regional language variant

      // WHEN
      String baseLanguage = LanguageRegistry.getBaseLanguage(variant);

      // THEN
      assertThat(baseLanguage)
          .as("Base language for '%s' should be '%s'", variant, expectedBase)
          .isEqualTo(expectedBase);
    }

    @ParameterizedTest
    @DisplayName("Should extract base language from script variants")
    @CsvSource({"sr-Cyrl-BA, sr", "az-Cyrl-AZ, az", "uz-Cyrl-UZ, uz"})
    void shouldExtractBaseLanguageFromScriptVariants(String variant, String expectedBase) {
      // GIVEN
      // A language variant with script specification

      // WHEN
      String baseLanguage = LanguageRegistry.getBaseLanguage(variant);

      // THEN
      assertThat(baseLanguage)
          .as("Base language for script variant '%s' should be '%s'", variant, expectedBase)
          .isEqualTo(expectedBase);
    }

    @ParameterizedTest
    @DisplayName("Should return itself for base languages")
    @ValueSource(strings = {"de", "en", "fr", "es", "it", "pt", "ru", "zh", "ja"})
    void shouldReturnItselfForBaseLanguages(String baseLanguage) {
      // GIVEN
      // A base language code without region

      // WHEN
      String result = LanguageRegistry.getBaseLanguage(baseLanguage);

      // THEN
      assertThat(result)
          .as("Base language '%s' should return itself", baseLanguage)
          .isEqualTo(baseLanguage);
    }

    @Test
    @DisplayName("Should return null for unsupported language code")
    void shouldReturnNullForUnsupportedLanguageCode() {
      // GIVEN
      String unsupportedLanguage = UNKNOWN_LANGUAGE;

      // WHEN
      String baseLanguage = LanguageRegistry.getBaseLanguage(unsupportedLanguage);

      // THEN
      assertThat(baseLanguage).as("Unsupported language should return null").isNull();
    }

    @Test
    @DisplayName("Should return null for null language code")
    void shouldReturnNullForNullLanguageCode() {
      // GIVEN
      String languageCode = null;

      // WHEN
      String baseLanguage = LanguageRegistry.getBaseLanguage(languageCode);

      // THEN
      assertThat(baseLanguage).as("Null language code should return null").isNull();
    }

    @Test
    @DisplayName("Should return null for empty language code")
    void shouldReturnNullForEmptyLanguageCode() {
      // GIVEN
      String languageCode = "";

      // WHEN
      String baseLanguage = LanguageRegistry.getBaseLanguage(languageCode);

      // THEN
      assertThat(baseLanguage).as("Empty language code should return null").isNull();
    }
  }

  @Nested
  @DisplayName("getLanguageHierarchy() Tests")
  class GetLanguageHierarchyTests {

    @Test
    @DisplayName("Should return hierarchy from specific to general for regional variant")
    void shouldReturnHierarchyForRegionalVariant() {
      // GIVEN
      String languageCode = GERMAN_AUSTRIA;

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // THEN
      assertThat(hierarchy)
          .as("Hierarchy should go from specific to general")
          .containsExactly("de-AT", "de");
    }

    @Test
    @DisplayName("Should return hierarchy for script variant")
    void shouldReturnHierarchyForScriptVariant() {
      // GIVEN
      String languageCode = SERBIAN_CYRILLIC;

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // THEN
      assertThat(hierarchy)
          .as("Hierarchy for script variant should include base language")
          .containsExactly("sr-Cyrl-BA", "sr");
    }

    @Test
    @DisplayName("Should return single element for base language")
    void shouldReturnSingleElementForBaseLanguage() {
      // GIVEN
      String languageCode = GERMAN_BASE;

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // THEN
      assertThat(hierarchy)
          .as("Base language hierarchy should contain only itself")
          .containsExactly("de");
    }

    @Test
    @DisplayName("Should return empty list for unsupported language")
    void shouldReturnEmptyListForUnsupportedLanguage() {
      // GIVEN
      String unsupportedLanguage = UNKNOWN_LANGUAGE;

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(unsupportedLanguage);

      // THEN
      assertThat(hierarchy).as("Unsupported language should return empty list").isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for null language code")
    void shouldReturnEmptyListForNullLanguageCode() {
      // GIVEN
      String languageCode = null;

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // THEN
      assertThat(hierarchy).as("Null language code should return empty list").isEmpty();
    }

    @Test
    @DisplayName("Should return immutable list")
    void shouldReturnImmutableList() {
      // GIVEN
      String languageCode = GERMAN_AUSTRIA;
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // WHEN & THEN
      assertThatThrownBy(() -> hierarchy.add("de-XX"))
          .as("Returned list should be immutable")
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @DisplayName("Should return correct hierarchy for various language codes")
    @CsvSource({
      "en-US, 2",
      "en-GB, 2",
      "fr-FR, 2",
      "es-MX, 2",
      "pt-BR, 2",
      "zh-CN, 2",
      "en, 1",
      "de, 1",
      "fr, 1"
    })
    void shouldReturnCorrectHierarchySize(String languageCode, int expectedSize) {
      // GIVEN
      // A language code with known hierarchy

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(languageCode);

      // THEN
      assertThat(hierarchy)
          .as("Hierarchy for '%s' should have %d elements", languageCode, expectedSize)
          .hasSize(expectedSize);
    }
  }

  @Nested
  @DisplayName("getAllSupportedLanguages() Tests")
  class GetAllSupportedLanguagesTests {

    @Test
    @DisplayName("Should return non-empty set of supported languages")
    void shouldReturnNonEmptySet() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // THEN
      assertThat(allLanguages)
          .as("Registry should contain supported languages")
          .isNotEmpty()
          .hasSizeGreaterThan(100);
    }

    @Test
    @DisplayName("Should include common base languages")
    void shouldIncludeCommonBaseLanguages() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // THEN
      assertThat(allLanguages)
          .as("Should contain common base languages")
          .contains("de", "en", "fr", "es", "it", "pt", "ru", "zh", "ja", "ko");
    }

    @Test
    @DisplayName("Should include common language variants")
    void shouldIncludeCommonLanguageVariants() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // THEN
      assertThat(allLanguages)
          .as("Should contain common language variants")
          .contains("de-DE", "de-AT", "en-US", "en-GB", "fr-FR", "es-ES");
    }

    @Test
    @DisplayName("Should return immutable set")
    void shouldReturnImmutableSet() {
      // GIVEN
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // WHEN & THEN
      assertThatThrownBy(() -> allLanguages.add("xx-YY"))
          .as("Returned set should be immutable")
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should return same set on multiple calls")
    void shouldReturnSameSetOnMultipleCalls() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> firstCall = LanguageRegistry.getAllSupportedLanguages();
      Set<String> secondCall = LanguageRegistry.getAllSupportedLanguages();

      // THEN
      assertThat(firstCall).as("Multiple calls should return same set").isEqualTo(secondCall);
    }
  }

  @Nested
  @DisplayName("getBaseLanguages() Tests")
  class GetBaseLanguagesTests {

    @Test
    @DisplayName("Should return non-empty set of base languages")
    void shouldReturnNonEmptySet() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();

      // THEN
      assertThat(baseLanguages)
          .as("Registry should contain base languages")
          .isNotEmpty()
          .hasSizeGreaterThan(50);
    }

    @Test
    @DisplayName("Should include common base languages")
    void shouldIncludeCommonBaseLanguages() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();

      // THEN
      assertThat(baseLanguages)
          .as("Should contain common base languages")
          .contains("de", "en", "fr", "es", "it", "pt", "ru", "zh", "ja", "ko", "ar", "hi");
    }

    @Test
    @DisplayName("Should not include regional variants")
    void shouldNotIncludeRegionalVariants() {
      // GIVEN
      // The language registry

      // WHEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();

      // THEN
      assertThat(baseLanguages)
          .as("Should not be empty before checking for exclusions")
          .isNotEmpty()
          .as("Should not contain regional variants")
          .doesNotContain("de-AT", "de-CH", "en-US", "en-GB", "fr-FR", "es-ES");
    }

    @Test
    @DisplayName("Should return immutable set")
    void shouldReturnImmutableSet() {
      // GIVEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();

      // WHEN & THEN
      assertThatThrownBy(() -> baseLanguages.add("xx"))
          .as("Returned set should be immutable")
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should be subset of all supported languages")
    void shouldBeSubsetOfAllSupportedLanguages() {
      // GIVEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // WHEN & THEN
      assertThat(allLanguages)
          .as("All supported languages should contain all base languages")
          .containsAll(baseLanguages);
    }

    @Test
    @DisplayName("Should have fewer elements than all supported languages")
    void shouldHaveFewerElementsThanAllSupportedLanguages() {
      // GIVEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // WHEN & THEN
      assertThat(baseLanguages)
          .as("Base languages should be fewer than all supported languages")
          .hasSizeLessThan(allLanguages.size());
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should maintain consistency between variants and base language mapping")
    void shouldMaintainConsistencyBetweenVariantsAndBaseLanguageMapping() {
      // GIVEN
      String baseLanguage = GERMAN_BASE;
      Set<String> variants = LanguageRegistry.getLanguageVariants(baseLanguage);

      // WHEN & THEN
      // All variants should map back to the same base language
      for (String variant : variants) {
        assertThat(LanguageRegistry.getBaseLanguage(variant))
            .as("Variant '%s' should map to base language '%s'", variant, baseLanguage)
            .isEqualTo(baseLanguage);
      }
    }

    @Test
    @DisplayName("Should provide valid hierarchy for all supported languages")
    void shouldProvideValidHierarchyForAllSupportedLanguages() {
      // GIVEN
      Set<String> allLanguages = LanguageRegistry.getAllSupportedLanguages();

      // WHEN & THEN
      for (String language : allLanguages) {
        List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(language);

        assertThat(hierarchy)
            .as("Language '%s' should have non-empty hierarchy", language)
            .isNotEmpty();

        assertThat(hierarchy.get(0))
            .as("First element in hierarchy should be the language itself")
            .isEqualTo(language);
      }
    }

    @Test
    @DisplayName("Should correctly identify all base languages as supported")
    void shouldCorrectlyIdentifyAllBaseLanguagesAsSupported() {
      // GIVEN
      Set<String> baseLanguages = LanguageRegistry.getBaseLanguages();

      // WHEN & THEN
      for (String baseLanguage : baseLanguages) {
        assertThat(LanguageRegistry.isSupported(baseLanguage))
            .as("Base language '%s' should be supported", baseLanguage)
            .isTrue();
      }
    }

    @ParameterizedTest
    @DisplayName("Should resolve language fallback chain correctly")
    @CsvSource({"de-AT, de", "en-US, en", "fr-CA, fr", "pt-BR, pt", "es-MX, es"})
    void shouldResolveLanguageFallbackChainCorrectly(
        String specificLanguage, String fallbackLanguage) {
      // GIVEN
      // A specific language variant

      // WHEN
      List<String> hierarchy = LanguageRegistry.getLanguageHierarchy(specificLanguage);

      // THEN
      assertThat(hierarchy)
          .as("Hierarchy should contain both specific and fallback language")
          .containsExactly(specificLanguage, fallbackLanguage);

      assertThat(LanguageRegistry.isSupported(specificLanguage))
          .as("Specific language '%s' should be supported", specificLanguage)
          .isTrue();

      assertThat(LanguageRegistry.isSupported(fallbackLanguage))
          .as("Fallback language '%s' should be supported", fallbackLanguage)
          .isTrue();
    }
  }
}
