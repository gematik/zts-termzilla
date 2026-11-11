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

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_INITIALCHARACTERCASEINSENSITIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SnomedConceptDescription}. */
@DisplayName("SnomedConceptDescription")
class SnomedConceptDescriptionTest {

  @Nested
  @DisplayName("getLanguageList(String)")
  class GetLanguageListTests {

    @Test
    @DisplayName("should return matching entries for generic English language")
    void shouldReturnMatchingEntriesForGenericEnglishLanguage() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionWithLanguages(
              List.of(
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.EN_GB),
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.EN_US),
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.DE_DE)));

      // WHEN
      List<SnomedLanguage> result =
          description.getLanguageList(SnomedReleaseVersionConstants.LANGUAGE_EN);

      // THEN
      assertThat(result)
          .extracting(SnomedLanguage::getRefsetId)
          .containsExactly(
              SnomedReleaseVersionConstants.EN_GB, SnomedReleaseVersionConstants.EN_US);
    }

    @Test
    @DisplayName("should return all configured languages when language code is null")
    void shouldReturnAllConfiguredLanguagesWhenLanguageCodeIsNull() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionWithLanguages(
              List.of(
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.EN_GB),
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.EN_US),
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.DE_DE)));

      // WHEN
      List<SnomedLanguage> result = description.getLanguageList(null);

      // THEN
      assertThat(result)
          .extracting(SnomedLanguage::getRefsetId)
          .containsExactly(
              SnomedReleaseVersionConstants.EN_GB,
              SnomedReleaseVersionConstants.EN_US,
              SnomedReleaseVersionConstants.DE_DE);
    }

    @Test
    @DisplayName("should fall back to default language when language code is unknown")
    void shouldFallBackToDefaultLanguageWhenLanguageCodeIsUnknown() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionWithLanguages(
              List.of(
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.EN_GB),
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.DE_DE)));

      // WHEN
      List<SnomedLanguage> result = description.getLanguageList("fr-FR");

      // THEN
      assertThat(result)
          .extracting(SnomedLanguage::getRefsetId)
          .containsExactly(SnomedReleaseVersionConstants.EN_GB);
    }

    @Test
    @DisplayName("should return empty list when no entries match requested language")
    void shouldReturnEmptyListWhenNoEntriesMatchRequestedLanguage() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionWithLanguages(
              List.of(
                  SnomedConceptDescriptionTestDataHelper.languageEntry(
                      SnomedReleaseVersionConstants.DE_DE)));

      // WHEN
      List<SnomedLanguage> result =
          description.getLanguageList(SnomedReleaseVersionConstants.LANGUAGE_EN);

      // THEN
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("isValid(String)")
  class IsValidTests {

    @Test
    @DisplayName("should require exact match for case sensitive terms")
    void shouldRequireExactMatchForCaseSensitiveTerms() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionForValidation(
              "Heart Attack", SC_CASESENSITIVE);

      // WHEN
      boolean exactMatch = description.isValid("Heart Attack");
      boolean caseChangedMatch = description.isValid("heart attack");

      // THEN
      assertThat(exactMatch).isTrue();
      assertThat(caseChangedMatch).isFalse();
    }

    @Test
    @DisplayName("should allow case insensitive match for case insensitive terms")
    void shouldAllowCaseInsensitiveMatchForCaseInsensitiveTerms() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionForValidation(
              "Heart Attack", SC_CASESINSENSITIVE);

      // WHEN
      boolean result = description.isValid("heart attack");

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName(
        "should require first character case match for initial-character-case-insensitive terms")
    void shouldRequireFirstCharacterCaseMatchForInitialCharacterCaseInsensitiveTerms() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionForValidation(
              "Diabetes", SC_INITIALCHARACTERCASEINSENSITIVE);

      // WHEN
      boolean matchingFirstCharacterCase = description.isValid("DiAbEtEs");
      boolean differentFirstCharacterCase = description.isValid("diAbEtEs");

      // THEN
      assertThat(matchingFirstCharacterCase).isTrue();
      assertThat(differentFirstCharacterCase).isFalse();
    }

    @Test
    @DisplayName("should return false for unknown case significance")
    void shouldReturnFalseForUnknownCaseSignificance() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionForValidation(
              "Diabetes", "unknown-case-significance-id");

      // WHEN
      boolean result = description.isValid("Diabetes");

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName(
        "should throw exception for empty provided display in initial-character-case-insensitive mode")
    void shouldThrowExceptionForEmptyProvidedDisplayInInitialCharacterCaseInsensitiveMode() {
      // GIVEN
      SnomedConceptDescription description =
          SnomedConceptDescriptionTestDataHelper.descriptionForValidation(
              "Diabetes", SC_INITIALCHARACTERCASEINSENSITIVE);

      // WHEN THEN
      assertThatThrownBy(() -> description.isValid(""))
          .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
  }
}
