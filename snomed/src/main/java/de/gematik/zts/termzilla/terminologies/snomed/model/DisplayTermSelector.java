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

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_FULLYSPECIFIEDNAME;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_PREFERRED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SYNONYM;
import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.LANGUAGE_DEFAULT;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// TODO: This might be refactored into the direction of SnomedDesignationManager

/**
 * Selects the appropriate FHIR display term for a {@link SnomedConcept} based on language
 * preferences.
 *
 * <p>This class extracts the display-term selection logic that was previously embedded in {@link
 * SnomedConcept}. It applies a prioritised fallback strategy per language code:
 *
 * <ol>
 *   <li>Preferred FSN in the requested language
 *   <li>Preferred synonym in the requested language
 *   <li>Preferred FSN in the default language
 *   <li>Preferred synonym in the default language
 * </ol>
 *
 * <p>When multiple language codes are provided, they are tried in list order and the first language
 * that produces a result wins.
 */
public final class DisplayTermSelector {

  private DisplayTermSelector() {}

  /**
   * Selects the display term for the given concept using the provided language codes in priority
   * order. Returns the result of the first language code that produces a match.
   *
   * @param concept the concept to select a display term for
   * @param languageCodes language codes in priority order (e.g., de-DE, de, en-GB, en)
   * @return the display term, or empty if no language produces a result
   */
  public static Optional<String> selectDisplayTerm(
      SnomedConcept concept, List<String> languageCodes) {

    for (String languageCode : languageCodes) {
      Optional<String> result = selectDisplayTerm(concept, languageCode);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }

  /**
   * Selects the display term for the given concept and language code using the 4-step priority
   * fallback strategy.
   *
   * @param concept the concept to select a display term for
   * @param languageCode language code (e.g., en, en-GB, en-US, de, de-DE)
   * @return the display term, or empty if no matching term is found
   */
  public static Optional<String> selectDisplayTerm(SnomedConcept concept, String languageCode) {

    // 1. Preferred FSN in the requested language
    Optional<String> result =
        findTermForTypeLanguageAndAcceptability(
            concept, SC_FULLYSPECIFIEDNAME, languageCode, SC_PREFERRED);
    if (result.isPresent()) {
      return result;
    }

    // 2. Preferred synonym in the requested language
    result =
        findTermForTypeLanguageAndAcceptability(concept, SC_SYNONYM, languageCode, SC_PREFERRED);
    if (result.isPresent()) {
      return result;
    }

    // 3. Preferred FSN in the default language (fallback)
    result =
        findTermForTypeLanguageAndAcceptability(
            concept, SC_FULLYSPECIFIEDNAME, LANGUAGE_DEFAULT, SC_PREFERRED);
    if (result.isPresent()) {
      return result;
    }

    // 4. Preferred synonym in the default language (fallback)
    return findTermForTypeLanguageAndAcceptability(
        concept, SC_SYNONYM, LANGUAGE_DEFAULT, SC_PREFERRED);
  }

  /**
   * Finds the display term for a description matching the given type, language, and acceptability.
   * Language reference sets are iterated in priority order (the list from {@link
   * SnomedReleaseVersionConstants#LANGUAGE_REFSET_MAP} is pre-sorted by priority).
   *
   * @param concept the concept to search descriptions in
   * @param typeId description type identifier (FSN, synonym, etc.)
   * @param languageCode language code (e.g., en, en-GB, en-US, de, de-DE)
   * @param acceptability acceptability identifier (preferred, acceptable, etc.)
   * @return the display term, or empty if no matching term is found
   */
  static Optional<String> findTermForTypeLanguageAndAcceptability(
      SnomedConcept concept, String typeId, String languageCode, String acceptability) {

    if (languageCode == null) {
      return Optional.empty();
    }

    // Resolve language reference set IDs; fall back to all-language refsets if unknown
    List<String> languageRefsetIdList =
        Objects.requireNonNullElse(
            SnomedReleaseVersionConstants.LANGUAGE_REFSET_MAP.get(languageCode),
            SnomedReleaseVersionConstants.LANGUAGE_REFSET_MAP.get(
                SnomedReleaseVersionConstants.LANGUAGE_ALL));

    // Iterate over descriptions matching the language's base code
    for (SnomedConceptDescription conceptDescription :
        concept.getSnomedConceptDescriptionList(languageCode)) {

      // Filter by requested description type (FSN, synonym, etc.)
      if (conceptDescription.getTypeId().equals(typeId)) {

        // Iterate refset IDs in priority order — first match wins
        for (String languageRefsetId : languageRefsetIdList) {

          boolean matches =
              conceptDescription.getLanguageList().stream()
                  .anyMatch(
                      language ->
                          language.getAcceptabilityId().equals(acceptability)
                              && language.getRefsetId().equals(languageRefsetId));

          if (matches) {
            return Optional.of(conceptDescription.getTerm());
          }
        }
      }
    }

    return Optional.empty();
  }
}
