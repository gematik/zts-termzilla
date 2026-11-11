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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests für die FHIR String Analyzer.
 *
 * <p>Diese Tests überprüfen, ob die Tokenisierung und Normalisierung korrekt funktionieren. Das ist
 * kritisch, weil Index und Search die gleichen Analyzer verwenden MÜSSEN, damit indizierte Begriffe
 * auch gefunden werden können.
 */
@DisplayName("FHIR String Analyzer Tests")
class FhirStringAnalyzerTest {

  // ========================================================================
  // TEIL 1: SingleToken Analyzer Tests
  // ========================================================================

  @Nested
  @DisplayName("SingleToken Analyzer")
  class SingleTokenAnalyzerTests {

    /**
     * Der SingleToken Analyzer wird für :exact Searches verwendet. Er behandelt den gesamten String
     * als EIN Token (KeywordTokenizer).
     */
    private final FhirStringAnalyzerSingleToken analyzer = new FhirStringAnalyzerSingleToken();

    @Test
    @DisplayName("Einfacher Text wird lowercase konvertiert")
    void shouldConvertToLowercase() {
      // GIVEN: Ein String mit Großbuchstaben
      String input = "SNOMED CT";

      // WHEN: Wir analysieren den String
      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      // THEN:
      // - Wir erwarten EIN Token (weil KeywordTokenizer)
      // - Der Token sollte lowercase sein
      assertEquals(1, tokens.size(), "SingleToken Analyzer sollte genau 1 Token erzeugen");
      assertEquals("snomed ct", tokens.get(0), "Text sollte in lowercase konvertiert werden");
    }

    @Test
    @DisplayName("Multiple Whitespaces werden normalisiert")
    void shouldNormalizeWhitespace() {
      // WHY: Oft haben Bezeichnungen inkonsistente Leerzeichen
      // z.B. "SNOMED  CT" (2 Spaces) vs "SNOMED CT" (1 Space)
      // Diese sollten als identisch behandelt werden

      String inputWithMultipleSpaces = "SNOMED    CT   International";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, inputWithMultipleSpaces);

      assertEquals(1, tokens.size());
      assertEquals(
          "snomed ct international",
          tokens.get(0),
          "Multiple Whitespaces sollten auf ein einzelnes Space reduziert werden");
    }

    @Test
    @DisplayName("Führende/Nachfolgende Whitespaces werden entfernt")
    void shouldTrimWhitespace() {
      String inputWithLeadingTrailing = "  LOINC  ";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, inputWithLeadingTrailing);

      assertEquals(1, tokens.size());
      assertEquals("loinc", tokens.get(0), "Whitespace am Anfang/Ende sollte entfernt werden");
    }

    @Test
    @DisplayName("ASCII-Folding konvertiert Umlaute")
    void shouldFoldASCII() {
      // WHY: Deutsche Begriffe enthalten Umlaute, aber Suchen erfolgen oft ohne
      // z.B. Suche nach "Grosse" sollte "Größe" finden

      String inputWithUmlauts = "Körpergröße";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, inputWithUmlauts);

      assertEquals(1, tokens.size());
      assertEquals(
          "korpergrosse",
          tokens.get(0),
          "Umlaute sollten ASCII-konform konvertiert werden (ö->o, ß->ss)");
    }

    @Test
    @DisplayName("Sonderzeichen bleiben ERHALTEN")
    void shouldKeepSpecialCharacters() {
      // IMPORTANT: SingleToken Analyzer verwendet KeywordTokenizer
      // -> Sonderzeichen werden NICHT durch Spaces ersetzt!

      String inputWithSpecialChars = "ICD-10-GM";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, inputWithSpecialChars);

      assertEquals(1, tokens.size());
      assertEquals(
          "icd-10-gm",
          tokens.get(0),
          "Sonderzeichen wie '-' sollten im SingleToken Analyzer ERHALTEN bleiben");
    }
  }

  // ========================================================================
  // TEIL 2: MultipleToken Analyzer Tests
  // ========================================================================

  @Nested
  @DisplayName("MultipleToken Analyzer")
  class MultipleTokenAnalyzerTests {

    /**
     * Der MultipleToken Analyzer wird für :contains Searches verwendet. Er splittet den String in
     * mehrere Tokens (WhitespaceTokenizer). Zusätzlich ersetzt er Sonderzeichen durch Spaces!
     */
    private final FhirStringAnalyzerMultipleToken analyzer = new FhirStringAnalyzerMultipleToken();

    @Test
    @DisplayName("Text wird in mehrere Tokens gesplittet")
    void shouldSplitIntoMultipleTokens() {
      // WHY: Für Teilstring-Suchen müssen wir einzelne Wörter matchen können

      String input = "SNOMED CT International";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      // DANN: 3 separate Tokens
      assertEquals(3, tokens.size(), "MultipleToken sollte bei Whitespaces splitten");
      assertEquals("snomed", tokens.get(0));
      assertEquals("ct", tokens.get(1));
      assertEquals("international", tokens.get(2));
    }

    @Test
    @DisplayName("Sonderzeichen werden zu Spaces konvertiert")
    void shouldConvertSpecialCharsToSpaces() {
      // WHY: Codes wie "ICD-10-GM" sollen auch ohne Bindestriche gefunden werden
      // z.B. Suche nach "ICD 10" sollte "ICD-10-GM" finden

      String input = "ICD-10-GM";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      // Bindestriche werden zu Spaces -> "ICD 10 GM" -> 3 Tokens
      assertEquals(3, tokens.size(), "Bindestriche sollten zu Splits führen");
      assertEquals("icd", tokens.get(0));
      assertEquals("10", tokens.get(1));
      assertEquals("gm", tokens.get(2));
    }

    @Test
    @DisplayName("Klammern werden zu Spaces konvertiert")
    void shouldConvertBracketsToSpaces() {
      // WHY: Oft haben Bezeichnungen Zusatzinfos in Klammern
      // z.B. "Diabetes (Typ 2)"

      String input = "Diabetes(Typ 2)";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      // "()" werden zu Spaces -> "Diabetes Typ 2" -> 3 Tokens
      assertEquals(3, tokens.size());
      assertTrue(tokens.contains("diabetes"));
      assertTrue(tokens.contains("typ"));
      assertTrue(tokens.contains("2"));
    }

    @Test
    @DisplayName("Verschiedene Klammer-Typen werden behandelt")
    void shouldHandleVariousBrackets() {
      // Der Analyzer sollte [], (), {} ersetzen

      String input = "Test[1]/Value{2}-(3)";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      // Alle Sonderzeichen werden zu Spaces
      // "Test 1  Value 2   3 " -> "Test", "1", "Value", "2", "3"
      assertEquals(5, tokens.size());
      assertTrue(tokens.contains("test"));
      assertTrue(tokens.contains("1"));
      assertTrue(tokens.contains("value"));
      assertTrue(tokens.contains("2"));
      assertTrue(tokens.contains("3"));
    }

    @Test
    @DisplayName("ASCII-Folding funktioniert auch bei Multiple Tokens")
    void shouldFoldASCIIInMultipleTokens() {
      String input = "Körpergröße Messung";

      List<String> tokens = AnalyzerUtils.displayTokens(analyzer, input);

      assertEquals(2, tokens.size());
      assertEquals("korpergrosse", tokens.get(0));
      assertEquals("messung", tokens.get(1));
    }
  }

  // ========================================================================
  // TEIL 3: Vergleichstests - KRITISCH für Index/Search Konsistenz!
  // ========================================================================

  @Nested
  @DisplayName("Analyzer Vergleichs- und Konsistenztests")
  class AnalyzerComparisonTests {

    private final FhirStringAnalyzerSingleToken singleTokenAnalyzer =
        new FhirStringAnalyzerSingleToken();
    private final FhirStringAnalyzerMultipleToken multipleTokenAnalyzer =
        new FhirStringAnalyzerMultipleToken();

    @Test
    @DisplayName("KRITISCH: normalizePhrase verwendet SingleToken Analyzer")
    void normalizePhrase_shouldUseSingleTokenAnalyzer() {
      // WHY: Die normalizePhrase() Methode in AnalyzerUtils wird vermutlich
      // sowohl beim Indexieren als auch beim Suchen verwendet.
      // Wir müssen sicherstellen, dass sie konsistent arbeitet!

      String input = "SNOMED CT";

      String normalized = AnalyzerUtils.normalizePhrase(input);
      List<String> singleTokenResult = AnalyzerUtils.displayTokens(singleTokenAnalyzer, input);

      assertEquals(
          singleTokenResult.get(0),
          normalized,
          "normalizePhrase sollte das gleiche Ergebnis wie SingleToken Analyzer liefern");
    }

    @Test
    @DisplayName("KRITISCH: Gleicher Input durch beide Analyzer - unterschiedliche Ergebnisse")
    void differentAnalyzers_shouldProduceDifferentResults() {
      // WHY: Dies demonstriert, WARUM es wichtig ist, den richtigen Analyzer zu wählen!

      String input = "ICD-10-GM";

      List<String> singleTokens = AnalyzerUtils.displayTokens(singleTokenAnalyzer, input);
      List<String> multipleTokens = AnalyzerUtils.displayTokens(multipleTokenAnalyzer, input);

      // SingleToken: 1 Token mit Bindestrichen
      assertEquals(1, singleTokens.size());
      assertEquals("icd-10-gm", singleTokens.get(0));

      // MultipleToken: 3 separate Tokens
      assertEquals(3, multipleTokens.size());
      assertEquals("icd", multipleTokens.get(0));
      assertEquals("10", multipleTokens.get(1));
      assertEquals("gm", multipleTokens.get(2));
    }
  }

  // ========================================================================
  // TEIL 4: Edge Cases
  // ========================================================================

  @Nested
  @DisplayName("Edge Cases und Grenzfälle")
  class EdgeCaseTests {

    private final FhirStringAnalyzerSingleToken singleTokenAnalyzer =
        new FhirStringAnalyzerSingleToken();
    private final FhirStringAnalyzerMultipleToken multipleTokenAnalyzer =
        new FhirStringAnalyzerMultipleToken();

    // TODO: Die  Edge Cases (leerer String, nur whitespaceds ) implementieren. Überprüfen, ob
    // Implementierung von AnalyzerUtils
    //  überhaupt richtig ist.

    @Test
    @DisplayName("Edge Case: Nur Sonderzeichen")
    void shouldHandleOnlySpecialChars() {
      String input = "---///---";

      List<String> singleTokens = AnalyzerUtils.displayTokens(singleTokenAnalyzer, input);
      List<String> multipleTokens = AnalyzerUtils.displayTokens(multipleTokenAnalyzer, input);

      // SingleToken: behält Sonderzeichen
      assertEquals(1, singleTokens.size());
      assertEquals("---///---", singleTokens.get(0));

      // MultipleToken: ersetzt durch Spaces -> keine Tokens übrig
      assertTrue(
          multipleTokens.isEmpty(),
          "Nur Sonderzeichen sollten nach Konvertierung zu Spaces keine Tokens mehr ergeben");
    }
  }
}
