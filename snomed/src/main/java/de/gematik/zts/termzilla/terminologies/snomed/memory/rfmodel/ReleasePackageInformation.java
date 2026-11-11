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
package de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** Represents metadata parsed from the SNOMED release package information file. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleasePackageInformation {

  /** Release effective time in RF2 date format. */
  private String effectiveTime;

  /** Name of the previously published package, if available. */
  private String previousPublishedPackage;

  /** Language reference set entries included in the release package metadata. */
  private List<LanguageRefset> languageRefsets;

  /** Language reference set descriptor from release package metadata. */
  @Getter
  @Setter
  public static class LanguageRefset {

    /** Reference set identifier. */
    private String id;

    /** Human-readable term of the language reference set. */
    private String term;
  }

  /**
   * Returns supported display languages derived from configured language reference sets.
   *
   * <p>The resulting list contains both full locale codes (for example {@code de-DE}) and
   * corresponding short codes (for example {@code de}), preserving first-seen order and avoiding
   * duplicates.
   *
   * @return ordered unique list of supported display language codes
   */
  public List<String> getSupportedDisplayLanguages() {

    Set<String> languageSet = new HashSet<>();
    ArrayList<String> languageList = new ArrayList<>();

    for (LanguageRefset refset : languageRefsets) {
      // Full locale code, for example de-DE or en-US.
      String language = SnomedReleaseVersionConstants.REFSET_LANGUAGE_MAP.get(refset.getId());

      // Add full locale code only once.
      if (!languageSet.contains(language)) {
        languageSet.add(language);
        languageList.add(language);
      }

      // Derive short language code.
      String languageShort =
          SnomedReleaseVersionConstants.LANGUAGE_LANGUAGE_SHORT_MAP.get(language);

      // Add short language code only once.
      if (!languageSet.contains(languageShort)) {
        languageSet.add(languageShort);
        languageList.add(languageShort);
      }
    }

    return languageList;
  }
}
