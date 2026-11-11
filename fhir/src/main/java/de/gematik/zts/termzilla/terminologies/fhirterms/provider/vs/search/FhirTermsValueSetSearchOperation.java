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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.search;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.helper.ValueSetSearchHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;

@Slf4j
public class FhirTermsValueSetSearchOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  public FhirTermsValueSetSearchOperation(FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  /**
   * Sucht nach ValueSets, die den übergebenen Suchparametern entsprechen und gibt die Ids der
   * gefundenen ValueSets zurück.
   *
   * @param scope Suchbereich
   * @param vsSearchParameter Suchparameter
   * @return Liste von Ids der gefundenen ValueSets
   * @throws InvalidRequestException Wird geworfen, wenn die Anfrage ungültig ist
   */
  public List<IdType> searchValueSets(String scope, VsSearchParameter vsSearchParameter)
      throws InvalidRequestException, NotImplementedException, UnknownScopeException {

    if (!fhirTermsInMemoryRepresentation.getScopeMap().containsKey(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' not found");
    }

    // Wir starten mit der Suche nach Codes, falls ein Code-Parameter übergeben wurde
    if (vsSearchParameter.getCode().isPresent()) {
      throw new NotImplementedException("'code' search is not implemented yet");
    }

    // _content-Query, um nach Inhalten zu suchen
    if (vsSearchParameter.getContent().isPresent()) {
      throw new NotImplementedException("'_content' search is not implemented yet");
    }

    // Metadaten-Query, um nach konkreten Metadaten zu suchen
    Query metadateQuery = ValueSetSearchHelper.prepareMetadataQuery(vsSearchParameter, scope, null);

    // Suche über den Suchindex anstoßen
    return ValueSetSearchHelper.executeQuery(
        fhirTermsInMemoryRepresentation.getLuceneSearcher(), metadateQuery);
  }
}
