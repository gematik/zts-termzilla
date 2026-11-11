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
package de.gematik.zts.termzilla.terminologies.loinc.helper;

import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import java.util.HashSet;
import java.util.Set;

public class LoincLanguageHelper {

  private LoincLanguageHelper() {}

  /**
   * Derives a set of language tags from all levels of the given {@link LanguageSelectionParameter}.
   *
   * <p>The resulting set always contains {@link LoincConstants#LOINC_LANGUAGE} ({@code en-US})
   * because the LOINC designation manager falls back to it for display resolution.
   *
   * @param lsp the language selection parameter aggregating all language sources
   * @return a non-empty set of BCP 47 language tags
   */
  public static Set<String> buildLanguageSet(LanguageSelectionParameter lsp) {
    Set<String> languageSet = new HashSet<>();
    languageSet.add(LoincConstants.LOINC_LANGUAGE);

    collectLanguageTags(lsp.getDisplayLanguageParameter(), languageSet);
    collectLanguageTags(lsp.getAcceptLanguageHeader(), languageSet);
    collectLanguageTags(lsp.getSystemDefaultLanguage(), languageSet);
    collectLanguageTags(lsp.getVsLanguage(), languageSet);
    return languageSet;
  }

  /**
   * Adds all language tags from the given {@link LanguageParameter} to the target set.
   *
   * @param param the language parameter to extract tags from, may be {@code null}
   * @param target the set to add tags to
   */
  private static void collectLanguageTags(LanguageParameter param, Set<String> target) {
    if (param != null) {
      for (LanguageParameter.LanguageRange range : param.getRanges()) {
        target.add(range.tag());
      }
    }
  }
}
