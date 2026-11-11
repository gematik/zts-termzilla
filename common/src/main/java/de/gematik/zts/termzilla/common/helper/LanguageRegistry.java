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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry for managing language codes and their relationships according to BCP 47 standard.
 *
 * <p>This class provides functionality for:
 *
 * <ul>
 *   <li>Language code validation
 *   <li>Language hierarchy resolution (e.g., "de-AT" → "de")
 *   <li>Dialect and regional variant lookup
 *   <li>Language generalization for fallback scenarios
 * </ul>
 *
 * <p><b>Language Code Format:</b> Language codes follow the BCP 47 standard (RFC 5646):
 *
 * <ul>
 *   <li>Language: {@code en}, {@code de}, {@code fr}
 *   <li>Language-Region: {@code en-US}, {@code de-DE}, {@code fr-FR}
 *   <li>Language-Script-Region: {@code az-Cyrl-AZ}, {@code sr-Cyrl-BA}
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 * // Check if a language code is supported
 * boolean isSupported = LanguageRegistry.isSupported("de-AT");
 *
 *
 * // Get all variants of a language
 * Set<String> germanVariants = LanguageRegistry.getLanguageVariants("de");
 * // Returns: ["de", "de-DE", "de-AT", "de-CH", "de-LI", "de-LU"]
 *
 *
 * // Get the base language for a variant
 * String baseLanguage = LanguageRegistry.getBaseLanguage("de-AT");
 * // Returns: "de"
 *
 *
 * // Get language hierarchy for fallback
 * List<String> hierarchy = LanguageRegistry.getLanguageHierarchy("de-AT");
 * // Returns: ["de-AT", "de"]
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe as all data structures are immutable after
 * initialization.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5646">RFC 5646 - BCP 47</a>
 */
public final class LanguageRegistry {

  /**
   * Map of base language codes to their variants (including the base language itself). Example:
   * "de" → ["de", "de-DE", "de-AT", "de-CH", "de-LI", "de-LU"]
   */
  private static final Map<String, Set<String>> LANGUAGE_VARIANTS;

  /**
   * Map of language codes (including variants) to their base language. Example: "de-AT" → "de",
   * "en-US" → "en"
   */
  private static final Map<String, String> BASE_LANGUAGE_MAP;

  /** Set of all supported language codes (both base languages and variants). */
  private static final Set<String> ALL_SUPPORTED_LANGUAGES;

  static {
    LANGUAGE_VARIANTS = initializeLanguageVariants();
    BASE_LANGUAGE_MAP = initializeBaseLanguageMap();
    ALL_SUPPORTED_LANGUAGES =
        Collections.unmodifiableSet(
            LANGUAGE_VARIANTS.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
  }

  /** Private constructor to prevent instantiation. */
  private LanguageRegistry() {
    throw new AssertionError("LanguageRegistry is a utility class and should not be instantiated");
  }

  /**
   * Checks if a language code is supported.
   *
   * @param languageCode the language code to check (e.g., "de", "de-AT")
   * @return true if the language code is supported, false otherwise
   */
  public static boolean isSupported(String languageCode) {
    return languageCode != null && ALL_SUPPORTED_LANGUAGES.contains(languageCode);
  }

  /**
   * Gets all variants of a base language, including the base language itself.
   *
   * <p>Example: For "de", returns ["de", "de-DE", "de-AT", "de-CH", "de-LI", "de-LU"]
   *
   * @param baseLanguage the base language code (e.g., "de", "en")
   * @return an unmodifiable set of all variants, or an empty set if not found
   */
  public static Set<String> getLanguageVariants(String baseLanguage) {
    return LANGUAGE_VARIANTS.getOrDefault(baseLanguage, Collections.emptySet());
  }

  /**
   * Gets the base language code for a given language code.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>"de-AT" → "de"
   *   <li>"en-US" → "en"
   *   <li>"de" → "de" (returns itself for base languages)
   *   <li>"unknown" → null
   * </ul>
   *
   * @param languageCode the language code (can be base or variant)
   * @return the base language code, or null if not found
   */
  public static String getBaseLanguage(String languageCode) {
    if (languageCode == null) {
      return null;
    }
    return BASE_LANGUAGE_MAP.get(languageCode);
  }

  /**
   * Gets the language hierarchy for fallback resolution.
   *
   * <p>Returns a list of language codes from most specific to least specific. This is useful for
   * implementing language fallback logic.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>"de-AT" → ["de-AT", "de"]
   *   <li>"en-US" → ["en-US", "en"]
   *   <li>"de" → ["de"]
   *   <li>"sr-Cyrl-BA" → ["sr-Cyrl-BA", "sr"]
   * </ul>
   *
   * @param languageCode the language code
   * @return a list of language codes for fallback, or empty list if not supported
   */
  public static List<String> getLanguageHierarchy(String languageCode) {
    if (!isSupported(languageCode)) {
      return Collections.emptyList();
    }

    List<String> hierarchy = new ArrayList<>();
    hierarchy.add(languageCode);

    String baseLanguage = getBaseLanguage(languageCode);
    if (baseLanguage != null && !baseLanguage.equals(languageCode)) {
      hierarchy.add(baseLanguage);
    }

    return Collections.unmodifiableList(hierarchy);
  }

  /**
   * Gets all supported language codes (both base languages and variants).
   *
   * @return an unmodifiable set of all supported language codes
   */
  public static Set<String> getAllSupportedLanguages() {
    return ALL_SUPPORTED_LANGUAGES;
  }

  /**
   * Gets all base languages (languages without region/script qualifiers).
   *
   * @return an unmodifiable set of base language codes
   */
  public static Set<String> getBaseLanguages() {
    return LANGUAGE_VARIANTS.keySet();
  }

  /**
   * Initializes the language variants map. This method extracts the language data into a more
   * maintainable structure.
   */
  private static Map<String, Set<String>> initializeLanguageVariants() {
    Map<String, Set<String>> variants = new HashMap<>();

    // Germanic languages
    addLanguage(variants, "de", "de-DE", "de-AT", "de-CH", "de-LI", "de-LU");
    addLanguage(
        variants, "en", "en-GB", "en-US", "en-AU", "en-BZ", "en-CA", "en-CB", "en-IE", "en-JM",
        "en-NZ", "en-PH", "en-TT", "en-ZA", "en-ZW");
    addLanguage(variants, "nl", "nl-BE", "nl-NL");
    addLanguage(variants, "af", "af-ZA");
    addLanguage(variants, "da", "da-DK");
    addLanguage(variants, "sv", "sv-FI", "sv-SE");
    addLanguage(variants, "nb", "nb-NO");
    addLanguage(variants, "nn", "nn-NO");
    addLanguage(variants, "is", "is-IS");
    addLanguage(variants, "fo", "fo-FO");

    // Romance languages
    addLanguage(
        variants, "es", "es-AR", "es-BO", "es-CL", "es-CO", "es-CR", "es-DO", "es-EC", "es-ES",
        "es-GT", "es-HN", "es-MX", "es-NI", "es-PA", "es-PE", "es-PR", "es-PY", "es-SV", "es-UY",
        "es-VE");
    addLanguage(variants, "fr", "fr-BE", "fr-CA", "fr-CH", "fr-FR", "fr-LU", "fr-MC");
    addLanguage(variants, "it", "it-CH", "it-IT");
    addLanguage(variants, "pt", "pt-BR", "pt-PT");
    addLanguage(variants, "ro", "ro-RO");
    addLanguage(variants, "ca", "ca-ES");
    addLanguage(variants, "gl", "gl-ES");

    // Slavic languages
    addLanguage(variants, "ru", "ru-RU");
    addLanguage(variants, "pl", "pl-PL");
    addLanguage(variants, "cs", "cs-CZ");
    addLanguage(variants, "sk", "sk-SK");
    addLanguage(variants, "uk", "uk-UA");
    addLanguage(variants, "be", "be-BY");
    addLanguage(variants, "bg", "bg-BG");
    addLanguage(variants, "hr", "hr-BA", "hr-HR");
    addLanguage(variants, "sl", "sl-SI");
    addLanguage(variants, "mk", "mk-MK");
    addLanguage(variants, "bs", "bs-BA");
    addLanguage(variants, "sr", "sr-BA", "sr-Cyrl-BA", "sr-SP", "sr-Cyrl-SP");

    // Asian languages
    addLanguage(variants, "zh", "zh-CN", "zh-HK", "zh-MO", "zh-SG", "zh-TW");
    addLanguage(variants, "ja", "ja-JP");
    addLanguage(variants, "ko", "ko-KR");
    addLanguage(variants, "th", "th-TH");
    addLanguage(variants, "vi", "vi-VN");
    addLanguage(variants, "id", "id-ID");
    addLanguage(variants, "ms", "ms-BN", "ms-MY");
    addLanguage(variants, "tl", "tl-PH");

    // Indo-Aryan languages
    addLanguage(variants, "hi", "hi-IN");
    addLanguage(variants, "ur", "ur-PK");
    addLanguage(variants, "pa", "pa-IN");
    addLanguage(variants, "gu", "gu-IN");
    addLanguage(variants, "mr", "mr-IN");
    addLanguage(variants, "sa", "sa-IN");
    addLanguage(variants, "kok", "kok-IN");

    // Dravidian languages
    addLanguage(variants, "ta", "ta-IN");
    addLanguage(variants, "te", "te-IN");
    addLanguage(variants, "kn", "kn-IN");

    // Semitic languages
    // Note: Arabic base language "ar" exists but has no standalone usage in many contexts
    addLanguage(
        variants, "ar", "ar-AE", "ar-BH", "ar-DZ", "ar-EG", "ar-IQ", "ar-JO", "ar-KW", "ar-LB",
        "ar-LY", "ar-MA", "ar-OM", "ar-QA", "ar-SA", "ar-SY", "ar-TN", "ar-YE");
    addLanguage(variants, "he", "he-IL");
    addLanguage(variants, "syr", "syr-SY");

    // Other Indo-European languages
    addLanguage(variants, "el", "el-GR");
    addLanguage(variants, "hy", "hy-AM");
    addLanguage(variants, "fa", "fa-IR");
    addLanguage(variants, "ps", "ps-AR");

    // Turkic languages
    addLanguage(variants, "tr", "tr-TR");
    addLanguage(variants, "az", "az-AZ", "az-Cyrl-AZ");
    addLanguage(variants, "uz", "uz-UZ", "uz-Cyrl-UZ");
    addLanguage(variants, "kk", "kk-KZ");
    addLanguage(variants, "ky", "ky-KG");
    addLanguage(variants, "tt", "tt-RU");

    // Uralic languages
    addLanguage(variants, "fi", "fi-FI");
    addLanguage(variants, "et", "et-EE");
    addLanguage(variants, "hu", "hu-HU");

    // Other European languages
    addLanguage(variants, "eu", "eu-ES"); // Basque
    addLanguage(variants, "cy", "cy-GB"); // Welsh
    addLanguage(variants, "ga"); // Irish (base only, no regional variants)
    addLanguage(variants, "mt", "mt-MT"); // Maltese
    addLanguage(variants, "sq", "sq-AL"); // Albanian
    addLanguage(variants, "lt", "lt-LT"); // Lithuanian
    addLanguage(variants, "lv", "lv-LV"); // Latvian

    // African languages
    addLanguage(variants, "sw", "sw-KE"); // Swahili
    addLanguage(variants, "zu", "zu-ZA"); // Zulu
    addLanguage(variants, "xh", "xh-ZA"); // Xhosa
    addLanguage(variants, "tn", "tn-ZA"); // Tswana
    addLanguage(variants, "ns", "ns-ZA"); // Northern Sotho
    addLanguage(variants, "ts"); // Tsonga (base only)

    // Caucasian languages
    addLanguage(variants, "ka", "ka-GE"); // Georgian

    // Uralic minority languages
    addLanguage(variants, "se", "se-FI", "se-NO", "se-SE"); // Northern Sami

    // Austronesian languages
    addLanguage(variants, "mi", "mi-NZ"); // Maori

    // Quechuan languages
    addLanguage(variants, "qu", "qu-BO", "qu-EC", "qu-PE");

    // Mongolic languages
    addLanguage(variants, "mn", "mn-MN");

    // Constructed languages
    addLanguage(variants, "eo"); // Esperanto (base only)

    // Other
    addLanguage(variants, "dv", "dv-MV"); // Dhivehi

    return Collections.unmodifiableMap(variants);
  }

  /**
   * Helper method to add a language and its variants to the map. The base language is automatically
   * included in the variants set.
   *
   * @param map the map to add to
   * @param baseLanguage the base language code (e.g., "de", "en")
   * @param additionalVariants optional additional variants (e.g., "de-AT", "de-CH")
   */
  private static void addLanguage(
      Map<String, Set<String>> map, String baseLanguage, String... additionalVariants) {
    if (baseLanguage == null || baseLanguage.isEmpty()) {
      return;
    }

    // Create set with base language first, then add variants
    Set<String> variantSet = new LinkedHashSet<>();
    variantSet.add(baseLanguage); // Always include the base language itself

    if (additionalVariants != null && additionalVariants.length > 0) {
      variantSet.addAll(Arrays.asList(additionalVariants));
    }

    map.put(baseLanguage, Collections.unmodifiableSet(variantSet));
  }

  /** Initializes the base language map (reverse lookup from variant to base language). */
  private static Map<String, String> initializeBaseLanguageMap() {
    Map<String, String> baseMap = new HashMap<>();

    for (Map.Entry<String, Set<String>> entry : LANGUAGE_VARIANTS.entrySet()) {
      String baseLanguage = entry.getKey();
      for (String variant : entry.getValue()) {
        baseMap.put(variant, baseLanguage);
      }
    }

    return Collections.unmodifiableMap(baseMap);
  }
}
