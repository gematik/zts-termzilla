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

import java.util.List;

/** Test data helper for {@link SnomedConceptDescription} unit tests. */
public final class SnomedConceptDescriptionTestDataHelper {

  private SnomedConceptDescriptionTestDataHelper() {}

  /**
   * Creates a description with the given language entries.
   *
   * @param languageList language entries attached to the description
   * @return a description instance with initialized language list
   */
  public static SnomedConceptDescription descriptionWithLanguages(
      List<SnomedLanguage> languageList) {
    SnomedConceptDescription description = new SnomedConceptDescription();
    description.setLanguageList(languageList);
    return description;
  }

  /**
   * Creates a description configured for case-significance validation.
   *
   * @param term term text
   * @param caseSignificanceId SNOMED case significance concept id
   * @return configured description instance
   */
  public static SnomedConceptDescription descriptionForValidation(
      String term, String caseSignificanceId) {
    SnomedConceptDescription description = new SnomedConceptDescription();
    description.setTerm(term);
    description.setCaseSignificanceId(caseSignificanceId);
    return description;
  }

  /**
   * Creates a language entry with the given language refset id.
   *
   * @param refsetId language refset id
   * @return configured language entry
   */
  public static SnomedLanguage languageEntry(String refsetId) {
    SnomedLanguage language = new SnomedLanguage();
    language.setRefsetId(refsetId);
    return language;
  }
}
