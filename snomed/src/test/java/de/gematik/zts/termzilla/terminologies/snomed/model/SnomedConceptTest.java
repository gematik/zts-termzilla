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
package de.gematik.zts.termzilla.terminologies.snomed.model;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_ACCEPTABLE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_FULLYSPECIFIEDNAME;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_PREFERRED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SYNONYM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests for {@link SnomedConcept}.
 *
 * <p>These tests document the current behaviour of display-value selection, language filtering, and
 * term lookup logic. They serve as a safety net for future refactoring of the display-term
 * selection out of the model class.
 */
@DisplayName("SnomedConcept Tests")
class SnomedConceptTest {

  private static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_GERMAN =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  private static final String REFSET_EN_US = SnomedReleaseVersionConstants.EN_US;
  private static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  private static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;

  // ──────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates a concept with a complete set of English descriptions: an FSN and a synonym, both
   * preferred in en-US and en-GB.
   */
  private static SnomedConcept conceptWithEnglishDescriptions() {
    SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("12345");

    SnomedConceptDescription fsn =
        SnomedDesignationTestDataBuilder.createDescription(
            "en", SC_FULLYSPECIFIEDNAME, "Diabetes mellitus (disorder)");
    fsn.getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_CORE, REFSET_EN_US, SC_PREFERRED));
    fsn.getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_CORE, REFSET_EN_GB, SC_PREFERRED));

    SnomedConceptDescription synonym =
        SnomedDesignationTestDataBuilder.createDescription("en", SC_SYNONYM, "Diabetes mellitus");
    synonym
        .getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_CORE, REFSET_EN_US, SC_PREFERRED));
    synonym
        .getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_CORE, REFSET_EN_GB, SC_PREFERRED));

    concept.getSnomedConceptDescriptionList().add(fsn);
    concept.getSnomedConceptDescriptionList().add(synonym);
    return concept;
  }

  /** Creates a concept with both English and German descriptions. */
  private static SnomedConcept conceptWithBilingualDescriptions() {
    SnomedConcept concept = conceptWithEnglishDescriptions();

    SnomedConceptDescription germanFsn =
        SnomedDesignationTestDataBuilder.createDescription(
            "de", SC_FULLYSPECIFIEDNAME, "Diabetes mellitus (Störung)");
    germanFsn
        .getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_GERMAN, REFSET_DE_DE, SC_PREFERRED));

    SnomedConceptDescription germanSynonym =
        SnomedDesignationTestDataBuilder.createDescription("de", SC_SYNONYM, "Zuckerkrankheit");
    germanSynonym
        .getLanguageList()
        .add(
            SnomedDesignationTestDataBuilder.createLanguage(
                MODULE_GERMAN, REFSET_DE_DE, SC_PREFERRED));

    concept.getSnomedConceptDescriptionList().add(germanFsn);
    concept.getSnomedConceptDescriptionList().add(germanSynonym);
    return concept;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Constructor
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize with empty description list")
    void shouldInitializeWithEmptyDescriptionList() {
      // WHEN
      SnomedConcept concept = new SnomedConcept();

      // THEN
      assertThat(concept.getSnomedConceptDescriptionList())
          .as("New concept should have an empty but non-null description list")
          .isNotNull()
          .isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getSnomedConceptDescriptionList(String)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getSnomedConceptDescriptionList(String) Tests")
  class GetDescriptionListByLanguageTests {

    private SnomedConcept concept;

    @BeforeEach
    void setUp() {
      concept = conceptWithBilingualDescriptions();
    }

    @Test
    @DisplayName("Should return English descriptions for language code 'en'")
    void shouldReturnEnglishDescriptionsForEn() {
      // WHEN
      List<SnomedConceptDescription> result = concept.getSnomedConceptDescriptionList("en");

      // THEN
      // NOTE: Each description is added once per matching language entry (see B8).
      // Both FSN and synonym have en-US AND en-GB language entries, so each is added twice.
      assertThat(result)
          .as("Should return English descriptions — duplicated per matching refset entry (see B8)")
          .hasSize(4)
          .extracting(SnomedConceptDescription::getLanguageCode)
          .containsOnly("en");
    }

    @Test
    @DisplayName("Should return German descriptions for language code 'de-DE'")
    void shouldReturnGermanDescriptionsForDeDE() {
      // WHEN
      List<SnomedConceptDescription> result = concept.getSnomedConceptDescriptionList("de-DE");

      // THEN
      assertThat(result)
          .as("Should return all German descriptions (FSN + synonym)")
          .hasSize(2)
          .extracting(SnomedConceptDescription::getLanguageCode)
          .containsOnly("de");
    }

    @Test
    @DisplayName("Should return empty list for null language code")
    void shouldReturnEmptyListForNullLanguage() {
      // WHEN
      List<SnomedConceptDescription> result =
          concept.getSnomedConceptDescriptionList((String) null);

      // THEN
      assertThat(result).as("Null language code should return empty list").isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for unknown language code")
    void shouldReturnEmptyListForUnknownLanguage() {
      // WHEN
      List<SnomedConceptDescription> result = concept.getSnomedConceptDescriptionList("fr");

      // THEN
      assertThat(result).as("Unknown language code should return empty list").isEmpty();
    }

    @Test
    @DisplayName("Should return only descriptions matching specific refset for 'en-US'")
    void shouldFilterBySpecificRefsetForEnUS() {
      // GIVEN – concept with FSN that is preferred in en-US but NOT in en-GB
      SnomedConcept snomedConcept = SnomedDesignationTestDataBuilder.createConcept("99999");

      SnomedConceptDescription fsnOnlyUs =
          SnomedDesignationTestDataBuilder.createDescription(
              "en", SC_FULLYSPECIFIEDNAME, "Test concept (finding)");
      fsnOnlyUs
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_US, SC_PREFERRED));
      // No en-GB language entry

      snomedConcept.getSnomedConceptDescriptionList().add(fsnOnlyUs);

      // WHEN
      List<SnomedConceptDescription> resultEnUs =
          snomedConcept.getSnomedConceptDescriptionList("en-US");
      List<SnomedConceptDescription> resultEnGb =
          snomedConcept.getSnomedConceptDescriptionList("en-GB");

      // THEN
      assertThat(resultEnUs).as("en-US should find the description with en-US refset").hasSize(1);
      assertThat(resultEnGb)
          .as("en-GB should not find the description without en-GB refset")
          .isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getSnomedConceptDescriptionList(List<String>)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getSnomedConceptDescriptionList(List<String>) Tests")
  class GetDescriptionListByLanguageListTests {

    @Test
    @DisplayName("Should aggregate descriptions for multiple languages")
    void shouldAggregateDescriptionsForMultipleLanguages() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN
      List<SnomedConceptDescription> result =
          concept.getSnomedConceptDescriptionList(List.of("de-DE", "en"));

      // THEN
      // de-DE: 2 descriptions × 1 refset each = 2
      // en:    2 descriptions × 2 refsets each = 4 (see B8)
      assertThat(result)
          .as("Should contain both German and English descriptions (with en duplicates, see B8)")
          .hasSize(6);
    }

    @Test
    @DisplayName("Should return empty list when all languages are unknown")
    void shouldReturnEmptyListWhenAllLanguagesUnknown() {
      // GIVEN
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      List<SnomedConceptDescription> result =
          concept.getSnomedConceptDescriptionList(List.of("fr", "it"));

      // THEN
      assertThat(result).as("Unknown languages should produce empty result").isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getTermForTypeLanguageAndAcceptability()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getTermForTypeLanguageAndAcceptability() Tests")
  class GetTermForTypeLanguageAndAcceptabilityTests {

    private SnomedConcept concept;

    @BeforeEach
    void setUp() {
      concept = conceptWithBilingualDescriptions();
    }

    @Test
    @DisplayName("Should return preferred FSN for English")
    void shouldReturnPreferredFsnForEnglish() {
      // WHEN
      String result =
          concept.getTermForTypeLanguageAndAcceptability(SC_FULLYSPECIFIEDNAME, "en", SC_PREFERRED);

      // THEN
      assertThat(result)
          .as("Should return the English FSN term")
          .isEqualTo("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should return preferred synonym for German")
    void shouldReturnPreferredSynonymForGerman() {
      // WHEN
      String result =
          concept.getTermForTypeLanguageAndAcceptability(SC_SYNONYM, "de-DE", SC_PREFERRED);

      // THEN
      assertThat(result).as("Should return the German synonym term").isEqualTo("Zuckerkrankheit");
    }

    @Test
    @DisplayName("Should return null for null language code")
    void shouldReturnNullForNullLanguage() {
      // WHEN
      String result =
          concept.getTermForTypeLanguageAndAcceptability(SC_FULLYSPECIFIEDNAME, null, SC_PREFERRED);

      // THEN
      assertThat(result).as("Null language code should return null").isNull();
    }

    @Test
    @DisplayName("Should return null when no matching type exists")
    void shouldReturnNullWhenNoMatchingType() {
      // GIVEN – concept with only FSN, no synonym
      SnomedConcept fsnOnlyConcept = SnomedDesignationTestDataBuilder.createConcept("11111");

      SnomedConceptDescription fsn =
          SnomedDesignationTestDataBuilder.createDescription(
              "en", SC_FULLYSPECIFIEDNAME, "Test (finding)");
      fsn.getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_US, SC_PREFERRED));

      fsnOnlyConcept.getSnomedConceptDescriptionList().add(fsn);

      // WHEN
      String result =
          fsnOnlyConcept.getTermForTypeLanguageAndAcceptability(SC_SYNONYM, "en", SC_PREFERRED);

      // THEN
      assertThat(result).as("Should return null when requested type is not available").isNull();
    }

    @Test
    @DisplayName("Should return null when acceptability does not match")
    void shouldReturnNullWhenAcceptabilityDoesNotMatch() {
      // GIVEN – concept with only preferred descriptions, asking for acceptable
      SnomedConcept snomedConcept = conceptWithEnglishDescriptions();

      // WHEN
      String result =
          snomedConcept.getTermForTypeLanguageAndAcceptability(
              SC_FULLYSPECIFIEDNAME, "en", SC_ACCEPTABLE);

      // THEN
      assertThat(result)
          .as("Should return null when no description matches the requested acceptability")
          .isNull();
    }

    @Test
    @DisplayName("Should prefer refset order when multiple refsets match")
    void shouldPreferRefsetOrderWhenMultipleRefsetsMatch() {
      // GIVEN – concept where the en-GB FSN has a different term than en-US FSN
      // The LANGUAGE_REFSET_MAP for "en" lists [EN_GB, EN_US], so en-GB has higher priority
      SnomedConcept snomedConcept = SnomedDesignationTestDataBuilder.createConcept("22222");

      SnomedConceptDescription fsnGb =
          SnomedDesignationTestDataBuilder.createDescription(
              "en", SC_FULLYSPECIFIEDNAME, "Colour (finding)");
      fsnGb
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_GB, SC_PREFERRED));

      SnomedConceptDescription fsnUs =
          SnomedDesignationTestDataBuilder.createDescription(
              "en", SC_FULLYSPECIFIEDNAME, "Color (finding)");
      fsnUs
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_US, SC_PREFERRED));

      snomedConcept.getSnomedConceptDescriptionList().add(fsnGb);
      snomedConcept.getSnomedConceptDescriptionList().add(fsnUs);

      // WHEN – query for "en" (which maps to [EN_GB, EN_US] in LANGUAGE_REFSET_MAP)
      String result =
          snomedConcept.getTermForTypeLanguageAndAcceptability(
              SC_FULLYSPECIFIEDNAME, "en", SC_PREFERRED);

      // THEN – en-GB should win because it is listed first in the refset order
      assertThat(result)
          .as("Should prefer en-GB over en-US because LANGUAGE_REFSET_MAP lists en-GB first")
          .isEqualTo("Colour (finding)");
    }

    @Test
    @DisplayName("Should use wildcard refsets for unknown language code")
    void shouldUseWildcardRefsetsForUnknownLanguage() {
      // GIVEN – a concept with descriptions
      SnomedConcept snomedConcept = conceptWithEnglishDescriptions();

      // WHEN – query with an unknown language code (not in LANGUAGE_REFSET_MAP)
      // The implementation falls back to LANGUAGE_ALL refsets via requireNonNullElse
      String result =
          snomedConcept.getTermForTypeLanguageAndAcceptability(
              SC_FULLYSPECIFIEDNAME, "xx-unknown", SC_PREFERRED);

      // THEN – unknown language has no short mapping, so getSnomedConceptDescriptionList
      // returns empty list, meaning no descriptions are iterated at all
      assertThat(result)
          .as(
              "Unknown language code should return null because description list filtering"
                  + " returns empty for unmapped language codes")
          .isNull();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getDefaultDisplayValue()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getDefaultDisplayValue() Tests")
  class GetDefaultDisplayValueTests {

    @Test
    @DisplayName("Should return preferred FSN in default language (English)")
    void shouldReturnPreferredFsnInDefaultLanguage() {
      // GIVEN
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      String result = concept.getDefaultDisplayValue();

      // THEN
      assertThat(result)
          .as("Default display should be the preferred English FSN")
          .isEqualTo("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should fall back to synonym when FSN is missing")
    void shouldFallBackToSynonymWhenFsnMissing() {
      // GIVEN – concept with only synonym, no FSN
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("33333");

      SnomedConceptDescription synonym =
          SnomedDesignationTestDataBuilder.createDescription("en", SC_SYNONYM, "Heart attack");
      synonym
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_US, SC_PREFERRED));

      concept.getSnomedConceptDescriptionList().add(synonym);

      // WHEN
      String result = concept.getDefaultDisplayValue();

      // THEN
      assertThat(result)
          .as("Should fall back to preferred synonym when FSN is not available")
          .isEqualTo("Heart attack");
    }

    @Test
    @DisplayName("Should return null when no descriptions match at all")
    void shouldReturnNullWhenNoDescriptionsMatchAtAll() {
      // GIVEN – empty concept
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("44444");

      // WHEN
      String result = concept.getDefaultDisplayValue();

      // THEN
      assertThat(result)
          .as("Should return null when no descriptions are available (per TODO comment)")
          .isNull();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getDefaultDisplayValue(String)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getDefaultDisplayValue(String) Tests")
  class GetDefaultDisplayValueByLanguageTests {

    @Test
    @DisplayName("Should return German FSN when queried with de-DE")
    void shouldReturnGermanFsnForDeDE() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN
      String result = concept.getDefaultDisplayValue("de-DE");

      // THEN
      assertThat(result)
          .as("Should return the preferred German FSN")
          .isEqualTo("Diabetes mellitus (Störung)");
    }

    @Test
    @DisplayName("Should fall back to default language when requested language has no match")
    void shouldFallBackToDefaultLanguageWhenRequestedLanguageHasNoMatch() {
      // GIVEN – concept with only English descriptions
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN – request German
      String result = concept.getDefaultDisplayValue("de-DE");

      // THEN – should fall back to default language (English)
      assertThat(result)
          .as("Should fall back to default language FSN when requested language is unavailable")
          .isEqualTo("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName(
        "Should follow priority: FSN preferred → synonym preferred → default FSN → default synonym")
    void shouldFollowPriorityOrder() {
      // GIVEN – concept with only a German synonym (no FSN in German, nothing preferred)
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("55555");

      // Add German synonym as preferred
      SnomedConceptDescription germanSynonym =
          SnomedDesignationTestDataBuilder.createDescription("de", SC_SYNONYM, "Zuckerkrankheit");
      germanSynonym
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_GERMAN, REFSET_DE_DE, SC_PREFERRED));

      concept.getSnomedConceptDescriptionList().add(germanSynonym);

      // WHEN – request German (no FSN available in German)
      String result = concept.getDefaultDisplayValue("de-DE");

      // THEN – should fall back to preferred synonym in German (step 2 in the algorithm)
      assertThat(result)
          .as("Should return preferred German synonym when no German FSN exists")
          .isEqualTo("Zuckerkrankheit");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getDefaultDisplayValue(List<String>)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getDefaultDisplayValue(List<String>) Tests")
  class GetDefaultDisplayValueByLanguageListTests {

    @Test
    @DisplayName("Should return first matching language from ordered list")
    void shouldReturnFirstMatchingLanguage() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN – German first, then English
      String result = concept.getDefaultDisplayValue(List.of("de-DE", "en"));

      // THEN
      assertThat(result)
          .as("Should return German display value because de-DE is first in the list")
          .isEqualTo("Diabetes mellitus (Störung)");
    }

    @Test
    @DisplayName("Should skip unavailable languages and return next match")
    void shouldSkipUnavailableLanguagesAndReturnNextMatch() {
      // GIVEN – concept with only English
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN – request French first, then English
      String result = concept.getDefaultDisplayValue(List.of("fr", "en"));

      // THEN
      assertThat(result)
          .as("Should skip unavailable 'fr' and return English display value")
          .isEqualTo("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should throw RuntimeException when no language matches at all")
    void shouldThrowRuntimeExceptionWhenNoLanguageMatches() {
      // GIVEN – empty concept
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("66666");

      List<String> languageList = List.of("fr", "it");
      // WHEN / THEN
      assertThatThrownBy(() -> concept.getDefaultDisplayValue(languageList))
          .as("Should throw RuntimeException when no language produces a display value")
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Unexpected behavior.");
    }
  }
}
