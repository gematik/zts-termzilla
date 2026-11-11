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

import de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DisplayTermSelector}. */
@DisplayName("DisplayTermSelector Tests")
class DisplayTermSelectorTest {

  private static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_GERMAN =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  private static final String REFSET_EN_US = SnomedReleaseVersionConstants.EN_US;
  private static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  private static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;

  // ──────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ──────────────────────────────────────────────────────────────────────────

  /** Creates a concept with English FSN and synonym, both preferred in en-US and en-GB. */
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
  // selectDisplayTerm(SnomedConcept, String)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("selectDisplayTerm(SnomedConcept, String) Tests")
  class SelectDisplayTermByLanguageTests {

    @Test
    @DisplayName("Should return preferred FSN for English")
    void shouldReturnPreferredFsnForEnglish() {
      // GIVEN
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "en");

      // THEN
      assertThat(result)
          .as("Should return the preferred English FSN")
          .hasValue("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should return preferred German FSN for de-DE")
    void shouldReturnPreferredGermanFsnForDeDE() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "de-DE");

      // THEN
      assertThat(result)
          .as("Should return the preferred German FSN")
          .hasValue("Diabetes mellitus (Störung)");
    }

    @Test
    @DisplayName("Should fall back to synonym when FSN is missing")
    void shouldFallBackToSynonymWhenFsnMissing() {
      // GIVEN — concept with only synonym, no FSN
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
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "en");

      // THEN
      assertThat(result)
          .as("Should fall back to preferred synonym when FSN is not available")
          .hasValue("Heart attack");
    }

    @Test
    @DisplayName("Should fall back to default language when requested language has no match")
    void shouldFallBackToDefaultLanguageWhenRequestedLanguageHasNoMatch() {
      // GIVEN — concept with only English descriptions
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN — request German
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "de-DE");

      // THEN — should fall back to default language (English)
      assertThat(result)
          .as("Should fall back to default language FSN when requested language is unavailable")
          .hasValue("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName(
        "Should follow priority: FSN preferred → synonym preferred → default FSN → default synonym")
    void shouldFollowPriorityOrder() {
      // GIVEN — concept with only a German synonym (no FSN in German)
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("55555");

      SnomedConceptDescription germanSynonym =
          SnomedDesignationTestDataBuilder.createDescription("de", SC_SYNONYM, "Zuckerkrankheit");
      germanSynonym
          .getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_GERMAN, REFSET_DE_DE, SC_PREFERRED));

      concept.getSnomedConceptDescriptionList().add(germanSynonym);

      // WHEN — request German (no FSN available in German)
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "de-DE");

      // THEN — should fall back to preferred synonym in German (step 2 in the algorithm)
      assertThat(result)
          .as("Should return preferred German synonym when no German FSN exists")
          .hasValue("Zuckerkrankheit");
    }

    @Test
    @DisplayName("Should return empty for concept with no descriptions")
    void shouldReturnEmptyForConceptWithNoDescriptions() {
      // GIVEN
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("44444");

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "en");

      // THEN
      assertThat(result).as("Should return empty when no descriptions are available").isEmpty();
    }

    @Test
    @DisplayName("Should return empty for unknown language with no default fallback")
    void shouldReturnEmptyForUnknownLanguageWithNoDefaultFallback() {
      // GIVEN — concept with no descriptions at all
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("77777");

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, "xx-unknown");

      // THEN
      assertThat(result).as("Unknown language with no descriptions should return empty").isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // selectDisplayTerm(SnomedConcept, List<String>)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("selectDisplayTerm(SnomedConcept, List<String>) Tests")
  class SelectDisplayTermByLanguageListTests {

    @Test
    @DisplayName("Should return first matching language from ordered list")
    void shouldReturnFirstMatchingLanguage() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN — German first, then English
      Optional<String> result =
          DisplayTermSelector.selectDisplayTerm(concept, List.of("de-DE", "en"));

      // THEN
      assertThat(result)
          .as("Should return German display value because de-DE is first in the list")
          .hasValue("Diabetes mellitus (Störung)");
    }

    @Test
    @DisplayName("Should skip unavailable languages and return next match")
    void shouldSkipUnavailableLanguagesAndReturnNextMatch() {
      // GIVEN — concept with only English
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN — request French first, then English
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, List.of("fr", "en"));

      // THEN
      assertThat(result)
          .as("Should skip unavailable 'fr' and return English display value")
          .hasValue("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should return empty when no language matches and no default fallback")
    void shouldReturnEmptyWhenNoLanguageMatches() {
      // GIVEN — empty concept
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("66666");

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, List.of("fr", "it"));

      // THEN
      assertThat(result)
          .as("Should return empty when no language produces a display value")
          .isEmpty();
    }

    @Test
    @DisplayName("Should return empty for empty language list")
    void shouldReturnEmptyForEmptyLanguageList() {
      // GIVEN
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      Optional<String> result = DisplayTermSelector.selectDisplayTerm(concept, List.of());

      // THEN
      assertThat(result).as("Empty language list should produce empty result").isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // findTermForTypeLanguageAndAcceptability()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("findTermForTypeLanguageAndAcceptability() Tests")
  class FindTermForTypeLanguageAndAcceptabilityTests {

    @Test
    @DisplayName("Should return preferred FSN for English")
    void shouldReturnPreferredFsnForEnglish() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_FULLYSPECIFIEDNAME, "en", SC_PREFERRED);

      // THEN
      assertThat(result)
          .as("Should return the English FSN term")
          .hasValue("Diabetes mellitus (disorder)");
    }

    @Test
    @DisplayName("Should return preferred synonym for German")
    void shouldReturnPreferredSynonymForGerman() {
      // GIVEN
      SnomedConcept concept = conceptWithBilingualDescriptions();

      // WHEN
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_SYNONYM, "de-DE", SC_PREFERRED);

      // THEN
      assertThat(result).as("Should return the German synonym term").hasValue("Zuckerkrankheit");
    }

    @Test
    @DisplayName("Should return empty for null language code")
    void shouldReturnEmptyForNullLanguage() {
      // GIVEN
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_FULLYSPECIFIEDNAME, null, SC_PREFERRED);

      // THEN
      assertThat(result).as("Null language code should return empty").isEmpty();
    }

    @Test
    @DisplayName("Should return empty when no matching type exists")
    void shouldReturnEmptyWhenNoMatchingType() {
      // GIVEN — concept with only FSN, no synonym
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("11111");

      SnomedConceptDescription fsn =
          SnomedDesignationTestDataBuilder.createDescription(
              "en", SC_FULLYSPECIFIEDNAME, "Test (finding)");
      fsn.getLanguageList()
          .add(
              SnomedDesignationTestDataBuilder.createLanguage(
                  MODULE_CORE, REFSET_EN_US, SC_PREFERRED));

      concept.getSnomedConceptDescriptionList().add(fsn);

      // WHEN
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_SYNONYM, "en", SC_PREFERRED);

      // THEN
      assertThat(result).as("Should return empty when requested type is not available").isEmpty();
    }

    @Test
    @DisplayName("Should return empty when acceptability does not match")
    void shouldReturnEmptyWhenAcceptabilityDoesNotMatch() {
      // GIVEN — concept with only preferred descriptions, asking for acceptable
      SnomedConcept concept = conceptWithEnglishDescriptions();

      // WHEN
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_FULLYSPECIFIEDNAME, "en", SC_ACCEPTABLE);

      // THEN
      assertThat(result)
          .as("Should return empty when no description matches the requested acceptability")
          .isEmpty();
    }

    @Test
    @DisplayName("Should prefer refset order when multiple refsets match")
    void shouldPreferRefsetOrderWhenMultipleRefsetsMatch() {
      // GIVEN — concept where en-GB and en-US FSNs have different terms
      // LANGUAGE_REFSET_MAP for "en" lists [EN_GB, EN_US], so en-GB has higher priority
      SnomedConcept concept = SnomedDesignationTestDataBuilder.createConcept("22222");

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

      concept.getSnomedConceptDescriptionList().add(fsnGb);
      concept.getSnomedConceptDescriptionList().add(fsnUs);

      // WHEN — query for "en" (which maps to [EN_GB, EN_US] in LANGUAGE_REFSET_MAP)
      Optional<String> result =
          DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
              concept, SC_FULLYSPECIFIEDNAME, "en", SC_PREFERRED);

      // THEN — en-GB should win because it is listed first in the refset order
      assertThat(result)
          .as("Should prefer en-GB over en-US because LANGUAGE_REFSET_MAP lists en-GB first")
          .hasValue("Colour (finding)");
    }
  }
}
