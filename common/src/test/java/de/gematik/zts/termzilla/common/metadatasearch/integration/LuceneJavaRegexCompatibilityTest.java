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
package de.gematik.zts.termzilla.common.metadatasearch.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that the supported regex subset produces identical results on both Lucene's {@link
 * RegexpQuery} (automaton-based) and Java's {@link Pattern} engine.
 *
 * <p>This test indexes a fixed set of sample values into an in-memory Lucene index and then runs
 * the same regex pattern against both engines, asserting that the matched values are identical.
 *
 * <p>The test corpus covers typical FHIR property values: codes, display strings, numeric strings,
 * URIs, and mixed alphanumeric identifiers.
 *
 * @see RegexpQuery
 * @see Pattern
 */
@DisplayName("Lucene vs Java Regex Compatibility")
// @Disabled("Only used to validate a common det of RegEx features in Lucene and Java")
class LuceneJavaRegexCompatibilityTest {

  private static final String FIELD_NAME = "value";

  /**
   * Test corpus of realistic FHIR property values. Covers codes, display texts, numeric strings,
   * URIs, and mixed identifiers.
   */
  private static final List<String> TEST_VALUES =
      List.of(
          // Simple codes
          "A01",
          "A01.1",
          "A02",
          "A99",
          "A123",
          "B01",
          "B12.3",
          "Z99",
          // Numeric strings
          "100",
          "200",
          "12345",
          "42",
          "0",
          "007",
          // Display-like strings
          "active",
          "inactive",
          "retired",
          "deprecated",
          "Diabetes mellitus",
          "Type 2 diabetes",
          "Hypertension",
          // URIs
          "http://snomed.info/sct",
          "http://loinc.org",
          "urn:oid:2.16.840",
          // Mixed identifiers
          "SNOMED-CT",
          "ICD-10-GM",
          "OPS-2024",
          "LOINC_2.78",
          // Edge cases
          "",
          "a",
          "Z",
          "abc123",
          "123abc",
          "a.b.c",
          "a-b-c");

  private static Directory directory;
  private static IndexSearcher searcher;
  private static DirectoryReader reader;

  @BeforeAll
  static void setupIndex() throws IOException {
    directory = new ByteBuffersDirectory();
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig())) {
      for (String value : TEST_VALUES) {
        Document doc = new Document();
        doc.add(new StringField(FIELD_NAME, value, Field.Store.YES));
        writer.addDocument(doc);
      }
      writer.commit();
    }
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);
  }

  @AfterAll
  static void tearDown() throws IOException {
    if (reader != null) {
      reader.close();
    }
    if (directory != null) {
      directory.close();
    }
  }

  // ==============================================================================================
  // Supported patterns — must produce identical results on both engines
  // ==============================================================================================

  @Nested
  @DisplayName("Supported patterns (functional intersection)")
  class SupportedPatterns {

    @ParameterizedTest(name = "Pattern: {0}")
    @MethodSource(
        "de.gematik.zts.termzilla.common.metadatasearch.integration"
            + ".LuceneJavaRegexCompatibilityTest#supportedPatterns")
    @DisplayName("Should produce identical results on Lucene and Java")
    void shouldProduceIdenticalResults(String regex, String description) throws IOException {
      Set<String> javaResults = matchWithJava(regex);
      Set<String> luceneResults = matchWithLucene(regex);

      assertThat(luceneResults)
          .as("Pattern '%s' (%s) should match the same values on both engines", regex, description)
          .isEqualTo(javaResults);

      // Sicherstellen, dass der Test nicht trivial ist (mindestens ein Treffer bei den meisten)
      // Einige Patterns dürfen leer sein (z.B. sehr spezifische)
    }

    @ParameterizedTest(name = "Pattern: {0}")
    @MethodSource(
        "de.gematik.zts.termzilla.common.metadatasearch.integration"
            + ".LuceneJavaRegexCompatibilityTest#supportedPatterns")
    @DisplayName("Should have at least one match for supported patterns (non-trivial test)")
    void shouldHaveAtLeastOneMatch(String regex, String description) {
      Set<String> results = matchWithJava(regex);

      // Die meisten Patterns sollten mindestens einen Treffer haben
      // Ausnahme: Sehr spezifische Patterns die gegen den Corpus nicht matchen
      if (!regex.equals("X[0-9]{3}") && !regex.equals("\\W+")) {
        assertThat(results)
            .as("Pattern '%s' (%s) should match at least one test value", regex, description)
            .isNotEmpty();
      }
    }
  }

  // ==============================================================================================
  // Unsupported Java-only patterns — must NOT be used in production filters
  // ==============================================================================================

  @Nested
  @DisplayName("Unsupported patterns (Java-only, not available in Lucene)")
  class UnsupportedPatterns {

    @Test
    @DisplayName("Inline flags (?i) should NOT be used — Lucene does not support them")
    void inlineFlagsNotSupported() throws IOException {
      // Java supports (?i) for case-insensitive matching
      Set<String> javaResults = matchWithJava("(?i)active");
      assertThat(javaResults).contains("active");

      // Lucene does not support (?i) — it's parsed differently
      Set<String> luceneResults = matchWithLucene("(?i)active");
      assertThat(luceneResults)
          .as("Lucene does not support inline flags (?i)")
          .isNotEqualTo(javaResults);
    }

    @Test
    @DisplayName("Lookahead (?=...) should NOT be used — Lucene does not support it")
    void lookaheadNotSupported() throws IOException {
      // Java supports positive lookahead
      Set<String> javaResults = matchWithJava("A(?=0).*");
      assertThat(javaResults)
          .as("Java should match values starting with A followed by 0")
          .isNotEmpty();

      // Lucene parses (?=...) differently — results will differ
      Set<String> luceneResults = matchWithLucene("A(?=0).*");
      assertThat(luceneResults)
          .as("Lucene does not support lookahead — results should differ from Java")
          .isNotEqualTo(javaResults);
    }

    @Test
    @DisplayName("Backreferences (\\1) should NOT be used — Lucene does not support them")
    void backreferencesNotSupported() throws IOException {
      // Java supports backreferences — match repeated characters like "00" in "007"
      Set<String> javaResults = matchWithJava(".*(.)\\1.*");
      assertThat(javaResults).as("Java should match values with repeated characters").isNotEmpty();

      // Lucene does not support backreferences
      Set<String> luceneResults = matchWithLucene(".*(.)\\1.*");
      assertThat(luceneResults)
          .as("Lucene does not support backreferences — results should differ from Java")
          .isNotEqualTo(javaResults);
    }

    @Test
    @DisplayName("Anchors ^ and $ should NOT be used — Lucene treats them as literal characters")
    void anchorsNotSupported() throws IOException {
      // Java treats ^ and $ as anchors (redundant in full-match mode, so "^active$" == "active")
      Set<String> javaResults = matchWithJava("^active$");
      assertThat(javaResults)
          .as("Java should match 'active' with redundant anchors")
          .containsExactly("active");

      // Lucene treats ^ and $ as literal characters, so "^active$" matches nothing
      Set<String> luceneResults = matchWithLucene("^active$");
      assertThat(luceneResults)
          .as("Lucene treats ^ and $ as literals — no match expected")
          .isEmpty();
    }

    @Test
    @DisplayName("Leading anchor ^ should NOT be used — Lucene treats it as literal character")
    void leadingAnchorNotSupported() throws IOException {
      Set<String> javaResults = matchWithJava("^active");
      assertThat(javaResults).containsExactly("active");

      Set<String> luceneResults = matchWithLucene("^active");
      assertThat(luceneResults)
          .as("Lucene treats ^ as literal — results should differ from Java")
          .isNotEqualTo(javaResults);
    }

    @Test
    @DisplayName("Trailing anchor $ should NOT be used — Lucene treats it as literal character")
    void trailingAnchorNotSupported() throws IOException {
      Set<String> javaResults = matchWithJava("active$");
      assertThat(javaResults).containsExactly("active");

      Set<String> luceneResults = matchWithLucene("active$");
      assertThat(luceneResults)
          .as("Lucene treats $ as literal — results should differ from Java")
          .isNotEqualTo(javaResults);
    }
  }

  // ==============================================================================================
  // Pattern provider
  // ==============================================================================================

  /**
   * Provides regex patterns that represent the supported functional intersection of Lucene 10.x and
   * Java regex engines. Each pattern is annotated with a description of the feature it tests.
   */
  static Stream<Arguments> supportedPatterns() {
    return Stream.of(
        // --- Literal matching ---
        Arguments.of("active", "Exact literal match"),
        Arguments.of("A01", "Exact code match"),
        Arguments.of("http://loinc.org", "Literal URI match"),

        // --- Dot (any single character) ---
        Arguments.of("A.1", "Dot matches any single character"),
        Arguments.of("A...", "Multiple dots"),

        // --- Character classes ---
        Arguments.of("A[0-9][0-9]", "Character class with digit range"),
        Arguments.of("[A-Z][0-9]+", "Leading uppercase letter followed by digits"),
        Arguments.of("[a-z]+", "Lowercase letters only"),
        Arguments.of("[A-Za-z]+", "Mixed case letters"),
        Arguments.of("[A-Za-z0-9]+", "Alphanumeric characters"),
        Arguments.of("[abc].*", "Enumerated character class"),

        // --- Negated character classes ---
        Arguments.of("[^0-9]+", "Negated digit class (non-digits only)"),
        Arguments.of("[^a-z]+", "Negated lowercase class"),

        // --- Quantifiers ---
        Arguments.of("A[0-9]*", "Zero or more digits after A"),
        Arguments.of("A[0-9]+", "One or more digits after A"),
        Arguments.of("[0-9][0-9]?", "Optional second digit"),
        Arguments.of("[0-9]{3}", "Exactly 3 digits"),
        Arguments.of("[0-9]{2,5}", "2 to 5 digits"),
        Arguments.of("X[0-9]{3}", "Specific prefix with exact digit count"),

        // --- Alternation ---
        Arguments.of("active|inactive", "Simple alternation"),
        Arguments.of("A01|B01|Z99", "Multiple alternation"),
        Arguments.of("(active|retired)", "Grouped alternation"),

        // --- Wildcards (dot + quantifier) ---
        Arguments.of(".*", "Match everything"),
        Arguments.of("A.*", "Starts with A"),
        Arguments.of(".*[0-9]", "Ends with a digit"),
        Arguments.of(".*diabetes.*", "Contains substring (lowercase)"),
        Arguments.of(".*mellitus", "Ends with substring"),

        // --- Grouping ---
        Arguments.of("(A|B)[0-9]+", "Group with alternation followed by digits"),
        Arguments.of("([A-Z])[0-9]+(\\.[0-9]+)?", "Optional decimal part"),

        // --- Escaped special characters ---
        Arguments.of("A01\\.1", "Escaped dot for literal match"),
        Arguments.of("ICD-10-GM", "Literal hyphens (no escaping needed)"),
        Arguments.of("LOINC_2\\.78", "Underscore and escaped dot"),
        Arguments.of("a\\.b\\.c", "Multiple escaped dots"),
        Arguments.of("http://snomed\\.info/sct", "Escaped dots in URI"),

        // --- Character class shorthands (supported since Lucene 10.x) ---
        Arguments.of("\\d+", "Digit shorthand"),
        Arguments.of("\\w+", "Word character shorthand"),
        Arguments.of(".*\\s.*", "Whitespace shorthand"),
        Arguments.of("\\D+", "Non-digit shorthand"),
        Arguments.of("\\W+", "Non-word character shorthand"),

        // --- Combined patterns ---
        Arguments.of("[A-Z][0-9]{2}(\\.[0-9]+)?", "ICD-style code pattern"),
        Arguments.of("(http|urn):.*", "URI-like pattern with protocol alternation"),
        Arguments.of("[A-Z]+-[0-9]+(-[A-Z]+)?", "Hyphenated identifier pattern"));
  }

  // ==============================================================================================
  // Engine execution
  // ==============================================================================================

  /**
   * Matches the test corpus against the given regex using Java's {@link String#matches(String)}.
   *
   * <p>Note: {@code String.matches()} implicitly anchors the pattern (equivalent to {@code
   * ^regex$}). Lucene's {@link RegexpQuery} also matches the entire field value, so both engines
   * have the same anchoring semantics.
   *
   * @param regex the regular expression to match
   * @return the set of matching values from the test corpus
   */
  private static Set<String> matchWithJava(String regex) {
    Set<String> results = new HashSet<>();
    for (String value : TEST_VALUES) {
      try {
        if (value.matches(regex)) {
          results.add(value);
        }
      } catch (Exception _) {
        // Pattern may be invalid for Java — return what we have
        break;
      }
    }
    return results;
  }

  /**
   * Matches the indexed test corpus against the given regex using Lucene's {@link RegexpQuery}.
   *
   * @param regex the regular expression to match
   * @return the set of matching values from the index
   */
  private static Set<String> matchWithLucene(String regex) throws IOException {
    RegexpQuery query = new RegexpQuery(new Term(FIELD_NAME, regex));
    TopDocs topDocs = searcher.search(query, TEST_VALUES.size());
    Set<String> results = new HashSet<>();

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      Document doc = searcher.storedFields().document(scoreDoc.doc);
      results.add(doc.get(FIELD_NAME));
    }

    return results;
  }
}
