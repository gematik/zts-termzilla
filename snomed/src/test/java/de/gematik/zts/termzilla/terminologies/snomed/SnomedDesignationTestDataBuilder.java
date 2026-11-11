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
package de.gematik.zts.termzilla.terminologies.snomed;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import java.util.ArrayList;

/**
 * Provides static factory methods for creating SNOMED CT test data objects.
 *
 * <p>Intended for use across multiple test classes to avoid duplication of common object
 * construction logic for {@link SnomedConcept}, {@link SnomedConceptDescription}, and {@link
 * SnomedLanguage}.
 */
public final class SnomedDesignationTestDataBuilder {

  private SnomedDesignationTestDataBuilder() {}

  /**
   * Creates an active {@link SnomedConcept} with the given identifier.
   *
   * @param id the concept identifier
   * @return an active concept with an empty description list
   */
  public static SnomedConcept createConcept(String id) {
    SnomedConcept concept = new SnomedConcept();
    concept.setId(id);
    concept.setActive(true);
    return concept;
  }

  /**
   * Creates an active {@link SnomedConceptDescription} with the given properties.
   *
   * @param languageCode the BCP 47 language code
   * @param typeId the SNOMED description type identifier
   * @param term the description term text
   * @return an active description with an empty language list
   */
  public static SnomedConceptDescription createDescription(
      String languageCode, String typeId, String term) {
    return createDescription(languageCode, typeId, term, true);
  }

  /**
   * Creates a {@link SnomedConceptDescription} with the given properties and active status.
   *
   * @param languageCode the BCP 47 language code
   * @param typeId the SNOMED description type identifier
   * @param term the description term text
   * @param active whether the description is active
   * @return a description with an empty language list
   */
  public static SnomedConceptDescription createDescription(
      String languageCode, String typeId, String term, boolean active) {
    SnomedConceptDescription description = new SnomedConceptDescription();
    description.setId("desc-" + term.hashCode());
    description.setActive(active);
    description.setLanguageCode(languageCode);
    description.setTypeId(typeId);
    description.setTerm(term);
    description.setLanguageList(new ArrayList<>());
    return description;
  }

  /**
   * Creates an active {@link SnomedLanguage} entry.
   *
   * @param moduleId the module identifier
   * @param refsetId the language reference set identifier
   * @param acceptabilityId the acceptability identifier
   * @return an active language entry
   */
  public static SnomedLanguage createLanguage(
      String moduleId, String refsetId, String acceptabilityId) {
    return createLanguage(moduleId, refsetId, acceptabilityId, true);
  }

  /**
   * Creates a {@link SnomedLanguage} entry with the given active status.
   *
   * @param moduleId the module identifier
   * @param refsetId the language reference set identifier
   * @param acceptabilityId the acceptability identifier
   * @param active whether the language entry is active
   * @return a language entry with the given properties
   */
  public static SnomedLanguage createLanguage(
      String moduleId, String refsetId, String acceptabilityId, boolean active) {
    SnomedLanguage language = new SnomedLanguage();
    language.setActive(active);
    language.setModuleId(moduleId);
    language.setRefsetId(refsetId);
    language.setAcceptabilityId(acceptabilityId);
    return language;
  }
}
