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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Tests for the regex pattern validation in {@link
 * ExpandFilterValidator#validateRegexPattern(String)}.
 *
 * <p>Verifies that supported regex features are accepted and unsupported features (which differ
 * between Lucene and Java regex engines) are rejected with an {@link InvalidRequestException}.
 */
@DisplayName("Regex Pattern Validation")
class RegexPatternValidationTest {

  @Nested
  @DisplayName("Supported patterns should be accepted")
  class SupportedPatterns {

    @ParameterizedTest(name = "Pattern: {0}")
    @MethodSource(
        "de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand"
            + ".RegexPatternValidationTest#supportedPatterns")
    @DisplayName("Should accept supported regex pattern")
    void shouldAcceptSupportedPattern(String regex, String description) {
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern(regex))
          .as("Pattern '%s' (%s) should be accepted", regex, description)
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should accept null and empty patterns")
    void shouldAcceptNullAndEmpty(String regex) {
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern(regex))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Unsupported patterns should be rejected")
  class UnsupportedPatterns {

    @ParameterizedTest(name = "Pattern: {0}")
    @MethodSource(
        "de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand"
            + ".RegexPatternValidationTest#unsupportedPatterns")
    @DisplayName("Should reject unsupported regex pattern")
    void shouldRejectUnsupportedPattern(String regex, String description) {
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexPattern(regex))
          .as("Pattern '%s' (%s) should be rejected", regex, description)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported features");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("Should accept escaped backslash followed by digit (not a backreference)")
    void shouldAcceptEscapedBackslashFollowedByDigit() {
      // \\1 in Java string = \1 in regex = backreference → rejected
      // \\\\1 in Java string = \\1 in regex = literal backslash + digit 1 → accepted
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern("\\\\1"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept \\d, \\w, \\s shorthands (supported in Lucene 10.x)")
    void shouldAcceptCharacterClassShorthands() {
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern("\\d+"))
          .doesNotThrowAnyException();
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern("\\w+"))
          .doesNotThrowAnyException();
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern(".*\\s.*"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept normal groups (non-capturing syntax is different from flags)")
    void shouldAcceptNormalGroups() {
      assertThatCode(() -> ExpandFilterValidator.validateRegexPattern("(A|B)[0-9]+"))
          .doesNotThrowAnyException();
    }
  }

  // ==============================================================================================
  // Pattern providers
  // ==============================================================================================

  static Stream<Arguments> supportedPatterns() {
    return Stream.of(
        // Literals
        Arguments.of("active", "Exact literal"),
        Arguments.of("A01\\.1", "Escaped dot"),

        // Character classes
        Arguments.of("[A-Z][0-9]+", "Range class with quantifier"),
        Arguments.of("[^0-9]+", "Negated class"),
        Arguments.of("[A-Za-z0-9_]+", "Alphanumeric with underscore"),

        // Shorthands (Lucene 10.x compatible)
        Arguments.of("\\d+", "Digit shorthand"),
        Arguments.of("\\w+", "Word shorthand"),
        Arguments.of("\\s", "Whitespace shorthand"),
        Arguments.of("\\D+", "Non-digit shorthand"),
        Arguments.of("\\W+", "Non-word shorthand"),
        Arguments.of("\\S+", "Non-whitespace shorthand"),

        // Quantifiers
        Arguments.of("A[0-9]*", "Zero or more"),
        Arguments.of("[0-9]+", "One or more"),
        Arguments.of("[0-9]?", "Optional"),
        Arguments.of("[0-9]{3}", "Exact count"),
        Arguments.of("[0-9]{2,5}", "Range count"),

        // Alternation and grouping
        Arguments.of("active|inactive", "Alternation"),
        Arguments.of("(A|B)[0-9]+", "Grouped alternation"),
        Arguments.of("([A-Z])[0-9]+(\\.[0-9]+)?", "Optional group"),

        // Wildcards
        Arguments.of(".*", "Match everything"),
        Arguments.of("A.*", "Starts with"),
        Arguments.of(".*diabetes.*", "Contains substring"),

        // Complex patterns
        Arguments.of("[A-Z][0-9]{2}(\\.[0-9]+)?", "ICD-style code"),
        Arguments.of("(http|urn):.*", "URI pattern"),
        Arguments.of("http://snomed\\.info/.*", "Escaped URI"));
  }

  static Stream<Arguments> unsupportedPatterns() {
    return Stream.of(
        // Inline flags
        Arguments.of("(?i)active", "Case-insensitive flag"),
        Arguments.of("(?m)^line$", "Multiline flag"),
        Arguments.of("(?s).*", "Dotall flag"),
        Arguments.of("(?x) a b c", "Comments flag"),
        Arguments.of("(?u)\\w+", "Unicode flag"),
        Arguments.of("(?-i)active", "Negated flag"),

        // Lookahead
        Arguments.of("A(?=0).*", "Positive lookahead"),
        Arguments.of("A(?!B).*", "Negative lookahead"),

        // Lookbehind
        Arguments.of("(?<=A)B", "Positive lookbehind"),
        Arguments.of("(?<!A)B", "Negative lookbehind"),

        // Atomic groups
        Arguments.of("(?>A+)B", "Atomic group"),

        // Named groups
        Arguments.of("(?<name>A+)", "Named group"),

        // Backreferences
        Arguments.of("(A)\\1", "Backreference \\1"),
        Arguments.of("(.)(.)\\2\\1", "Multiple backreferences"),

        // Possessive quantifiers
        Arguments.of("A*+B", "Possessive star"),
        Arguments.of("A++B", "Possessive plus"),
        Arguments.of("A?+B", "Possessive optional"),

        // Unicode categories
        Arguments.of("\\p{L}+", "Unicode letter category"),
        Arguments.of("\\P{N}+", "Negated unicode number category"),
        Arguments.of("\\p{Lu}", "Uppercase letter category"),

        // Anchors (Lucene treats ^ and $ as literal characters, not as anchors)
        Arguments.of("^active", "Leading anchor"),
        Arguments.of("active$", "Trailing anchor"),
        Arguments.of("^active$", "Both anchors"),
        Arguments.of("^[A-Z][0-9]+$", "Anchors with character classes"),
        Arguments.of("^.*$", "Anchors with wildcard"));
  }
}
