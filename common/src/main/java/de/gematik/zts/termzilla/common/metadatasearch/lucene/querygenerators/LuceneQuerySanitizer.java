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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for sanitizing user input before using it in Lucene QueryParser.
 *
 * <p>This is specifically needed when constructing queries with QueryParser, as it interprets
 * special Lucene syntax characters. When using TermQuery or other query constructors directly,
 * sanitization is not needed as they treat input as literal values.
 *
 * <p>Lucene special characters that need escaping: + - & | ! ( ) { } [ ] ^ " ~ * ? : \ /
 */
@Slf4j
@UtilityClass
public class LuceneQuerySanitizer {

  private static final int MAX_QUERY_LENGTH = 1000;

  // Characters that have special meaning in Lucene QueryParser
  private static final String LUCENE_SPECIAL_CHARS = "+-&|!(){}[]^\"~*?:\\/";

  /**
   * Sanitizes user input for safe use in Lucene QueryParser.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Validates length to prevent DoS
   *   <li>Removes control characters
   *   <li>Escapes Lucene special characters
   *   <li>Normalizes whitespace
   * </ul>
   *
   * @param input the user input to sanitize
   * @return sanitized input safe for QueryParser
   * @throws IllegalArgumentException if input is too long
   */
  public static String sanitizeForQueryParser(String input) {
    if (StringUtils.isBlank(input)) {
      return input;
    }

    // Check length to prevent DoS
    if (input.length() > MAX_QUERY_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Query too long: %d characters (max: %d)", input.length(), MAX_QUERY_LENGTH));
    }

    // Remove control characters (except newline and tab)
    String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

    // Normalize whitespace
    sanitized = sanitized.replaceAll("\\s+", " ").trim();

    // Escape Lucene special characters
    sanitized = escapeLuceneSpecialChars(sanitized);

    if (!input.equals(sanitized)) {
      log.debug("Sanitized query input: '{}' -> '{}'", input, sanitized);
    }

    return sanitized;
  }

  /**
   * Escapes Lucene special characters by prefixing them with backslash.
   *
   * <p>Special characters: + - & | ! ( ) { } [ ] ^ " ~ * ? : \ /
   *
   * @param input the input string
   * @return string with escaped special characters
   */
  public static String escapeLuceneSpecialChars(String input) {
    if (StringUtils.isBlank(input)) {
      return input;
    }

    StringBuilder sb = new StringBuilder();
    for (char c : input.toCharArray()) {
      if (LUCENE_SPECIAL_CHARS.indexOf(c) != -1) {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Sanitizes input for use in wildcard queries.
   *
   * <p>This allows wildcards (* and ?) but escapes other special characters. Use this when you want
   * to support wildcard searches.
   *
   * @param input the input string
   * @return sanitized string with wildcards preserved
   */
  public static String sanitizeForWildcardQuery(String input) {
    if (StringUtils.isBlank(input)) {
      return input;
    }

    // Check length
    if (input.length() > MAX_QUERY_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Query too long: %d characters (max: %d)", input.length(), MAX_QUERY_LENGTH));
    }

    // Remove control characters
    String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

    // Escape special chars except * and ?
    StringBuilder sb = new StringBuilder();
    for (char c : sanitized.toCharArray()) {
      if (LUCENE_SPECIAL_CHARS.indexOf(c) != -1 && c != '*' && c != '?') {
        sb.append('\\');
      }
      sb.append(c);
    }

    return sb.toString();
  }

  /**
   * Validates that input doesn't contain potentially dangerous patterns that could cause
   * performance issues or unexpected behavior.
   *
   * @param input the input to validate
   * @return true if input is safe
   */
  public static boolean isSafeInput(String input) {
    if (StringUtils.isBlank(input)) {
      return true;
    }

    // Check length
    if (input.length() > MAX_QUERY_LENGTH) {
      return false;
    }

    // Check for excessive wildcards (can cause performance issues)
    long wildcardCount = input.chars().filter(ch -> ch == '*' || ch == '?').count();
    if (wildcardCount > 10) {
      log.warn("Excessive wildcards in query: {}", wildcardCount);
      return false;
    }

    // Check for leading wildcards (can be slow)
    if (input.trim().startsWith("*") || input.trim().startsWith("?")) {
      log.warn("Leading wildcard in query");
      return false;
    }

    return true;
  }
}
