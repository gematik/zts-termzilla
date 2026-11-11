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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.CodeSystem;

/**
 * Utility class for building expansion result sets from concept definitions.
 *
 * <p>Provides methods to extract {@link ConceptBaseInfo} from CodeSystem concepts and to convert
 * sets of code strings into result sets, applying text filtering and active-status checks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ExpandResultHelper {

  /**
   * Extracts base concept information and adds it to the result set if applicable.
   *
   * <p>Only adds the concept if it is active or if inactive concepts should be included.
   *
   * @param system the CodeSystem URL
   * @param version the CodeSystem version
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add the concept to
   * @param concept the concept definition to extract information from
   * @return the created concept base info
   */
  static ConceptBaseInfo extractBaseConcept(
      String system,
      String version,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem.ConceptDefinitionComponent concept) {
    boolean isActive = CodeSystemHelper.isConceptActive(concept);

    ConceptBaseInfo conceptBaseInfo =
        ConceptBaseInfo.builder()
            .system(system)
            .version(version)
            .code(concept.getCode())
            .active(isActive)
            .build();

    // Wir prüfen erst hier, ob wir das Konzept zur Ergebnismenge hinzufügen
    // wollen, da getSnomedConcept() bei includeInactive sonst eine Exception
    // werfen würde.
    if (conceptBaseInfo.isActive() || includeInactiveConcepts) {
      filterResultSet.add(conceptBaseInfo);
    }

    return conceptBaseInfo;
  }

  /**
   * Adds codes from a result set to the filter result set as {@link ConceptBaseInfo} objects.
   *
   * <p>For each code, the concept is looked up in the CodeSystem and filtered against the text
   * filter before being added to the result set.
   *
   * @param resultCodes the set of codes to add
   * @param filterResultSet the target set to add concept base info objects to
   * @param codeSystem the CodeSystem containing the concept definitions
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param resultFilter the text filter to apply on concept definition content
   */
  static void addCodesToResultSet(
      Set<String> resultCodes,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem,
      boolean includeInactiveConcepts,
      String resultFilter)
      throws UnknownConceptException {

    if (resultCodes != null && !resultCodes.isEmpty()) {
      for (String resultCode : resultCodes) {
        CodeSystem.ConceptDefinitionComponent concept =
            CodeSystemHelper.findCodeInCodeSystem(codeSystem, resultCode);

        if (ExpandTextFilter.matchesFilter(codeSystem.getUrl(), concept, resultFilter)) {
          extractBaseConcept(
              codeSystem.getUrl(),
              codeSystem.getVersion(),
              includeInactiveConcepts,
              filterResultSet,
              concept);
        }
      }
    }
  }
}
