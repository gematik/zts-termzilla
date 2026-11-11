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

import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.LANGUAGE_DEFAULT;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Domain model representing a SNOMED CT concept with its descriptions and language entries.
 *
 * <p>Provides methods for retrieving descriptions filtered by language, and for selecting the
 * appropriate display term based on type (FSN/synonym), language, and acceptability priority.
 */
@Getter
@Setter
public class SnomedConcept {

  private String id;
  private String effectiveTime;
  private boolean active;
  private String moduleId;
  private String definitionStatusId;

  private List<SnomedConceptDescription> snomedConceptDescriptionList;

  // -----------------------------------------------------------------------------------------------

  public SnomedConcept() {
    this.snomedConceptDescriptionList = new ArrayList<>();
  }

  // -----------------------------------------------------------------------------------------------

  /**
   * Returns descriptions matching any of the given language codes.
   *
   * @param languageCodeList language codes (e.g., de-DE, de, en-GB, en, en-US)
   * @return descriptions matching any of the given languages
   */
  public List<SnomedConceptDescription> getSnomedConceptDescriptionList(
      List<String> languageCodeList) {

    List<SnomedConceptDescription> resultList = new ArrayList<>();

    for (String language : languageCodeList) {
      resultList.addAll(getSnomedConceptDescriptionList(language));
    }

    return resultList;
  }

  /**
   * Returns descriptions matching the given language code. Complex language codes (e.g., {@code
   * de-DE}) are mapped to their base language code (e.g., {@code de}) since RF2 release files use
   * base language codes in description entries.
   *
   * @param languageCode language code (e.g., en, en-GB, en-US, de, de-DE)
   * @return descriptions matching the given language
   */
  public List<SnomedConceptDescription> getSnomedConceptDescriptionList(String languageCode) {

    List<SnomedConceptDescription> resultList = new ArrayList<>();

    if (languageCode == null) {
      return new ArrayList<>();
    }

    // Map complex language codes to base language codes (e.g., 'de-DE' -> 'de')
    String languageShort =
        SnomedReleaseVersionConstants.LANGUAGE_LANGUAGE_SHORT_MAP.get(languageCode);
    if (languageShort == null) {
      // Unknown language code — no matching descriptions possible
      return new ArrayList<>();
    }
    List<String> languageRefsetIdList =
        SnomedReleaseVersionConstants.LANGUAGE_REFSET_MAP.get(languageCode);

    // Filter descriptions by base language code
    List<SnomedConceptDescription> shortLanguageDescriptionList =
        snomedConceptDescriptionList.stream()
            .filter(description -> description.getLanguageCode().equals(languageShort))
            .toList();

    for (SnomedConceptDescription description : shortLanguageDescriptionList) {
      // Add description for each matching language refset entry
      for (SnomedLanguage language : description.getLanguageList()) {
        if (languageRefsetIdList.contains(language.getRefsetId())) {
          resultList.add(description);
        }
      }
    }

    return resultList;
  }

  // -----------------------------------------------------------------------------------------------

  // TODO: Might be removed in future version (because of SnomedDesignationManager)

  /**
   * Returns the FHIR display value using the default language.
   *
   * @return display text, or {@code null} if no matching term is found
   */
  public String getDefaultDisplayValue() {
    return getDefaultDisplayValue(LANGUAGE_DEFAULT);
  }

  /**
   * Returns the FHIR display value matching the first language in the list that produces a result.
   * Languages are tried in list order, so the list should be sorted by priority.
   *
   * <p>Delegates to {@link DisplayTermSelector#selectDisplayTerm(SnomedConcept, List)}.
   *
   * @param languageCodeList language codes in priority order (e.g., de-DE, de, en-GB, en, en-US)
   * @return display text
   * @throws RuntimeException if no language in the list produces a display value
   */
  public String getDefaultDisplayValue(List<String> languageCodeList) {
    return DisplayTermSelector.selectDisplayTerm(this, languageCodeList)
        .orElseThrow(() -> new RuntimeException("Unexpected behavior."));
  }

  /**
   * Returns the FHIR display value for the given language code using the 4-step priority fallback
   * strategy in {@link DisplayTermSelector}.
   *
   * @param languageCode language code (e.g., en, en-GB, en-US, de, de-DE)
   * @return display text, or {@code null} if no matching term is found
   */
  public String getDefaultDisplayValue(String languageCode) {
    return DisplayTermSelector.selectDisplayTerm(this, languageCode).orElse(null);
  }

  /**
   * Returns the display term for a description matching the given type, language, and
   * acceptability.
   *
   * <p>Delegates to {@link
   * DisplayTermSelector#findTermForTypeLanguageAndAcceptability(SnomedConcept, String, String,
   * String)}.
   *
   * @param typeId description type identifier (FSN, synonym, etc.)
   * @param languageCode language code (e.g., en, en-GB, en-US, de, de-DE)
   * @param acceptability acceptability identifier (preferred, acceptable, etc.)
   * @return display text, or {@code null} if no matching term is found
   */
  public String getTermForTypeLanguageAndAcceptability(
      String typeId, String languageCode, String acceptability) {
    return DisplayTermSelector.findTermForTypeLanguageAndAcceptability(
            this, typeId, languageCode, acceptability)
        .orElse(null);
  }
}
