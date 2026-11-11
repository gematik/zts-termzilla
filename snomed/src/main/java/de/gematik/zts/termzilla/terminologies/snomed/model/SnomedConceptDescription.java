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

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a SNOMED concept description entry including language and case-significance metadata.
 *
 * <p>The instance stores one description term and its RF2 attributes, plus associated {@link
 * SnomedLanguage} acceptability entries used for language-specific display resolution.
 */
@Getter
@Setter
public class SnomedConceptDescription {

  private String id;
  private String effectiveTime;
  private boolean active;
  private String moduleId;
  private String conceptId;
  private String languageCode;
  private String typeId;
  private String term;
  private String caseSignificanceId;

  private List<SnomedLanguage> languageList;

  // -----------------------------------------------------------------------------------------------

  /**
   * Returns language entries matching the requested language code.
   *
   * <p>Multiple entries may match one language code. For example, requesting {@code en} may return
   * entries for both {@code en-GB} and {@code en-US}.
   *
   * @param languageCode language code (for example {@code en}, {@code en-GB}, {@code en-US}, {@code
   *     de}, {@code de-DE}); if {@code null}, all languages are considered
   * @return matching language entries from {@link #languageList}
   */
  public List<SnomedLanguage> getLanguageList(String languageCode) {

    if (languageCode == null) {
      languageCode = SnomedReleaseVersionConstants.LANGUAGE_ALL;
    }

    List<String> languageRefsetIdList =
        Objects.requireNonNullElse(
            SnomedReleaseVersionConstants.LANGUAGE_REFSET_MAP.get(languageCode),
            SnomedReleaseVersionConstants.LANGUAGE_REFSET_MAP.get(
                SnomedReleaseVersionConstants.LANGUAGE_DEFAULT));

    return languageList.stream()
        .filter(entry -> languageRefsetIdList.contains(entry.getRefsetId()))
        .toList();
  }

  // -----------------------------------------------------------------------------------------------

  /**
   * Validates a provided display string against this description's case-significance rules.
   *
   * @param providedDisplay display value to validate
   * @return {@code true} if the value matches according to case-significance semantics
   */
  public boolean isValid(String providedDisplay) {

    return switch (caseSignificanceId) {
      // Entire term case sensitive (core metadata concept)
      case SC_CASESENSITIVE -> term.equals(providedDisplay);
      // Entire term case insensitive (core metadata concept)
      case SC_CASESINSENSITIVE -> term.equalsIgnoreCase(providedDisplay);
      // Only initial character case sensitive (core metadata concept)
      case SC_INITIALCHARACTERCASEINSENSITIVE ->
          term.startsWith(providedDisplay.substring(0, 1))
              && term.substring(1).equalsIgnoreCase(providedDisplay.substring(1));
      default -> false;
    };
  }

  // -----------------------------------------------------------------------------------------------

  /**
   * Returns a compact textual representation of this description.
   *
   * @return id, term, language code, active flag, and effective time
   */
  @Override
  public String toString() {
    return id + " " + term + " (" + languageCode + ") " + active + " " + effectiveTime;
  }
}
