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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Centralizes all upfront validation of ValueSet expand filter definitions.
 *
 * <p>Before filter processing begins, {@link #validateFilters(List, CodeSystem)} validates every
 * filter in the include element against the referenced {@link CodeSystem}. This ensures that
 * invalid or unsupported filter combinations are rejected early with a clear error message, rather
 * than failing deep inside individual filter processors.
 *
 * <p>Validated aspects:
 *
 * <ul>
 *   <li>Property existence — the filter property must be defined (explicitly or implicitly) in the
 *       CodeSystem
 *   <li>Operator support — only known operators are accepted
 *   <li>Regex syntax — regex filter values must only use features supported by both Lucene and Java
 *   <li>Regex property type — regex filters are only valid on string and code properties
 *   <li>Hierarchy prerequisites — ISA, ISNOTA, GENERALIZES, and DESCENDENTOF require an is-a
 *       hierarchy meaning and the {@code concept} property
 * </ul>
 *
 * @see FhirTermsVsExplicitExpandOperation
 */
final class ExpandFilterValidator {

  /** Error message prefix for unsupported filter properties. */
  static final String UNSUPPORTED_FILTER_PROPERTY_MESSAGE =
      "The given ValueSet definition includes a filter with an unsupported property: ";

  /**
   * Pattern that detects unsupported regex features which are not available in both the Lucene
   * automaton engine and the Java regex engine's common intersection.
   *
   * <p>Detected unsupported constructs:
   *
   * <ul>
   *   <li>Inline flags: {@code (?i)}, {@code (?m)}, {@code (?s)}, {@code (?x)}, {@code (?u)},
   *       {@code (?d)}, and their negations {@code (?-i)} etc.
   *   <li>Lookahead: {@code (?=...)}, {@code (?!...)}
   *   <li>Lookbehind: {@code (?<=...)}, {@code (?<!...)}
   *   <li>Atomic groups: {@code (?>...)}
   *   <li>Named groups: {@code (?<name>...)}
   *   <li>Backreferences: {@code \1} through {@code \9}
   *   <li>Possessive quantifiers: {@code *+}, {@code ++}, {@code ?+}
   *   <li>Unicode categories: {@code \p{...}}, {@code \P{...}}
   *   <li>Anchors: {@code ^}, {@code $} — Java treats them as anchors (redundant in full-match
   *       mode), but Lucene treats them as literal characters, causing divergent results
   * </ul>
   */
  @SuppressWarnings("java:S125")
  // Regexp comment is considered as "commented-out code" by Sonar, but it's actually a detailed
  // explanation of the regex pattern
  private static final Pattern UNSUPPORTED_REGEX_FEATURES =
      Pattern.compile(
          // Inline flags, lookahead, lookbehind, atomic groups, named groups
          "\\(\\?[=!<>imsxud-]"
              // Backreferences: \1 through \9 (not preceded by another backslash)
              + "|(?<!\\\\)\\\\[1-9]"
              // Possessive quantifiers: *+ ++ ?+ (but not escaped)
              + "|(?<!\\\\)[*+?]\\+"
              // Unicode categories: \p{...} and \P{...}
              + "|(?<!\\\\)\\\\[pP]\\{"
              // Anchors: $ anywhere, ^ outside character classes (detected via dedicated method)
              + "|\\$");

  private ExpandFilterValidator() {
    // Utility class — prevent instantiation
  }

  /**
   * Validates all filters in the given include element against the referenced {@link CodeSystem}.
   *
   * <p>Each filter is checked for property existence, operator support, regex syntax validity,
   * regex property type compatibility, and hierarchy prerequisites. Validation stops at the first
   * error and throws an {@link InvalidRequestException}.
   *
   * @param filters the filter list from the include element
   * @param codeSystem the CodeSystem referenced by the include element
   * @throws InvalidRequestException if any filter is invalid or unsupported
   */
  static void validateFilters(
      List<ValueSet.ConceptSetFilterComponent> filters, CodeSystem codeSystem) {

    for (ValueSet.ConceptSetFilterComponent filter : filters) {
      validatePropertyExists(filter, codeSystem);
      validateOperatorSupported(filter);
      validateRegexFilter(filter, codeSystem);
      validateHierarchyFilter(filter, codeSystem);
    }
  }

  /**
   * Validates that the filter property is defined in the CodeSystem.
   *
   * @param filter the filter to validate
   * @param codeSystem the CodeSystem to check against
   * @throws InvalidRequestException if the property is not found
   */
  static void validatePropertyExists(
      ValueSet.ConceptSetFilterComponent filter, CodeSystem codeSystem) {

    String property = filter.getProperty();
    CodeSystemHelper.getProperty(codeSystem, property)
        .orElseThrow(
            () -> new InvalidRequestException(UNSUPPORTED_FILTER_PROPERTY_MESSAGE + property));
  }

  /**
   * Validates that the filter operator is one of the supported operators.
   *
   * @param filter the filter to validate
   * @throws InvalidRequestException if the operator is not supported
   */
  static void validateOperatorSupported(ValueSet.ConceptSetFilterComponent filter) {
    ValueSet.FilterOperator op = filter.getOp();
    switch (op) {
      case EQUAL, REGEX, EXISTS, ISA, ISNOTA, GENERALIZES, DESCENDENTOF, IN:
        // Unterstützte Operatoren
        break;
      default:
        throw new InvalidRequestException(
            "The given ValueSet definition includes a filter with an unsupported operation: "
                + op.toCode());
    }
  }

  /**
   * Validates regex-specific constraints: pattern syntax and property type compatibility.
   *
   * <p>Only applies to filters with the REGEX operator. Skipped for all other operators.
   *
   * @param filter the filter to validate
   * @param codeSystem the CodeSystem for property type resolution
   * @throws InvalidRequestException if the regex pattern uses unsupported features or the property
   *     type is incompatible with regex filtering
   */
  static void validateRegexFilter(
      ValueSet.ConceptSetFilterComponent filter, CodeSystem codeSystem) {

    if (filter.getOp() != ValueSet.FilterOperator.REGEX) {
      return;
    }

    // Regex-Syntax validieren
    validateRegexPattern(filter.getValue());

    // Regex auf parent/child ist nicht unterstützt
    String property = filter.getProperty();
    if (Objects.equals(property, PROPERTY_NAME_CHILD)
        || Objects.equals(property, PROPERTY_NAME_PARENT)) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes an unsupported combination of filter operation "
              + "'regex' and property '"
              + property
              + "'");
    }

    // "code" ist eine implizite Property, die immer regex-kompatibel ist
    if (Objects.equals(property, "code")) {
      return;
    }

    // Prüfen, ob der Property-Typ regex-kompatibel ist (nur string und code)
    CodeSystem.PropertyComponent propertyComponent =
        CodeSystemHelper.getProperty(codeSystem, property).orElse(null);
    if (propertyComponent != null && !hasSupportedRegexPropertyType(propertyComponent)) {
      throw new InvalidRequestException(
          "The regex filter operator is not supported for property '"
              + property
              + "' of type '"
              + propertyComponent.getType().toCode()
              + "'. Only string and code properties support regex filtering.");
    }
  }

  /**
   * Validates hierarchy-specific constraints for ISA, ISNOTA, GENERALIZES, and DESCENDENTOF
   * operators.
   *
   * <p>Requires that the CodeSystem has an is-a hierarchy meaning and that the filter property is
   * {@code concept}.
   *
   * @param filter the filter to validate
   * @param codeSystem the CodeSystem to check hierarchy constraints against
   * @throws InvalidRequestException if hierarchy prerequisites are not met
   */
  static void validateHierarchyFilter(
      ValueSet.ConceptSetFilterComponent filter, CodeSystem codeSystem) {

    ValueSet.FilterOperator op = filter.getOp();
    if (op != ValueSet.FilterOperator.ISA
        && op != ValueSet.FilterOperator.ISNOTA
        && op != ValueSet.FilterOperator.GENERALIZES
        && op != ValueSet.FilterOperator.DESCENDENTOF) {
      return;
    }

    if (!hasIsAHierarchyMeaning(codeSystem)) {
      throw new InvalidRequestException(
          "The hierarchyMeaning ('"
              + codeSystem.getHierarchyMeaning()
              + "') of the referenced CodeSystem is not usable with an '"
              + filter.getOp()
              + "' filter-operator.");
    }

    if (!Objects.equals(filter.getProperty(), "concept")) {
      throw new InvalidRequestException(
          "The given ValueSet filter definition has an invalid property-operator-combination: "
              + filter.getProperty()
              + " ("
              + filter.getOp()
              + ")");
    }
  }

  /**
   * Validates that a regex filter value only uses features supported by both the Lucene automaton
   * engine and Java's regex engine.
   *
   * @param regex the regex pattern to validate
   * @throws InvalidRequestException if the pattern uses unsupported features
   */
  static void validateRegexPattern(String regex) {
    if (regex == null || regex.isEmpty()) {
      return;
    }

    if (UNSUPPORTED_REGEX_FEATURES.matcher(regex).find() || containsAnchorCaret(regex)) {
      throw new InvalidRequestException(
          "The regex filter value '"
              + regex
              + "' uses unsupported features. "
              + "Only the common regex subset of Lucene and Java is supported. "
              + "Unsupported constructs include: inline flags (?i), lookahead (?=), "
              + "lookbehind (?<=), backreferences (\\1), possessive quantifiers (*+), "
              + "Unicode categories (\\p{}), and anchors (^ $).");
    }
  }

  /**
   * Detects {@code ^} used as an anchor (outside character classes).
   *
   * <p>Inside a character class ({@code [^...]}), the caret acts as negation and is allowed. This
   * method tracks whether the current position is inside a character class to distinguish the two
   * cases. Escaped brackets ({@code \[}, {@code \]}) and escaped carets ({@code \^}) are ignored.
   *
   * @param regex the regex pattern to check
   * @return {@code true} if the pattern contains a caret anchor outside a character class
   */
  private static boolean containsAnchorCaret(String regex) {
    boolean insideCharClass = false;
    boolean skipNext = false;

    for (int i = 0; i < regex.length(); i++) {
      char c = regex.charAt(i);

      if (skipNext) {
        skipNext = false;
        continue;
      }

      // Skip escaped characters
      if (c == '\\' && i + 1 < regex.length()) {
        skipNext = true;
      } else if (c == '[') {
        insideCharClass = true;
      } else if (c == ']' && insideCharClass) {
        insideCharClass = false;
      } else if (c == '^' && !insideCharClass) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines whether a filter can be executed via Lucene.
   *
   * <p>Only EQUAL and REGEX filters on non-hierarchy properties are Lucene-compatible. Parent/child
   * filters and hierarchy-based operations require graph traversal.
   *
   * @param filter the filter to classify
   * @return {@code true} if the filter can be executed via Lucene, {@code false} otherwise
   */
  static boolean isLuceneCompatibleFilter(ValueSet.ConceptSetFilterComponent filter) {
    String property = filter.getProperty();
    boolean isHierarchyProperty =
        Objects.equals(property, PROPERTY_NAME_PARENT)
            || Objects.equals(property, PROPERTY_NAME_CHILD);

    if (isHierarchyProperty) {
      return false;
    }

    ValueSet.FilterOperator op = filter.getOp();
    return op == ValueSet.FilterOperator.EQUAL || op == ValueSet.FilterOperator.REGEX;
  }

  /**
   * Checks whether the property type supports regex filtering.
   *
   * <p>Only string and code property types are supported for regex filtering.
   *
   * @param pc the property component to check
   * @return {@code true} if the property supports regex filtering, {@code false} otherwise
   */
  static boolean hasSupportedRegexPropertyType(CodeSystem.PropertyComponent pc) {
    return Objects.equals(pc.getType(), CodeSystem.PropertyType.CODE)
        || Objects.equals(pc.getType(), CodeSystem.PropertyType.STRING);
  }

  /**
   * Checks whether the CodeSystem has an is-a hierarchy meaning.
   *
   * @param codeSystem the CodeSystem to check
   * @return {@code true} if the CodeSystem has is-a hierarchy meaning, {@code false} otherwise
   */
  private static boolean hasIsAHierarchyMeaning(CodeSystem codeSystem) {
    return codeSystem.hasHierarchyMeaning()
        && Objects.equals(
            codeSystem.getHierarchyMeaning(), CodeSystem.CodeSystemHierarchyMeaning.ISA);
  }
}
