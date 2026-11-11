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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cm.search;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.helper.ConceptMapSearchHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;

@Slf4j
public class FhirTermsConceptMapSearchOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  public FhirTermsConceptMapSearchOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  /**
   * Sucht nach ConceptMaps, die den übergebenen Suchparametern entsprechen und gibt die Ids der
   * gefundenen ConceptMaps zurück.
   *
   * @param scope Suchbereich
   * @param cmSearchParameter Suchparameter
   * @return Liste von Ids der gefundenen ConceptMaps
   * @throws InvalidRequestException Wird geworfen, wenn die Anfrage ungültig ist
   */
  public List<IdType> searchConceptMaps(String scope, CmSearchParameter cmSearchParameter)
      throws InvalidRequestException, NotImplementedException, UnknownScopeException {

    if (!fhirTermsInMemoryRepresentation.getScopeMap().containsKey(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' not found");
    }

    // _content-Query, um nach Inhalten zu suchen
    if (cmSearchParameter.getContent().isPresent()) {
      throw new NotImplementedException("'_content' search is not implemented yet");
    }

    // Metadaten-Query, um nach konkreten Metadaten zu suchen
    Query metadateQuery =
        ConceptMapSearchHelper.prepareMetadataQuery(cmSearchParameter, scope, null);

    // Suche über den Suchindex anstoßen
    return ConceptMapSearchHelper.executeQuery(
        fhirTermsInMemoryRepresentation.getLuceneSearcher(), metadateQuery);
  }
}
