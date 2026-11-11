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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.search;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemConceptSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;

@Slf4j
public class FhirTermsCodeSystemSearchOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  public FhirTermsCodeSystemSearchOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  /**
   * Sucht nach CodeSystemen, die den übergebenen Suchparametern entsprechen und gibt die Ids der
   * gefundenen CodeSysteme zurück.
   *
   * @param scope Suchbereich
   * @param csSearchParameter Suchparameter
   * @return Liste von Ids der gefundenen CodeSysteme
   * @throws InvalidRequestException Wird geworfen, wenn die Anfrage ungültig ist
   */
  public List<IdType> searchCodeSystems(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws InvalidRequestException, NotImplementedException, UnknownScopeException {

    if (!fhirTermsInMemoryRepresentation.getScopeMap().containsKey(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' not found");
    }

    List<IdType> preliminaryCodeSearchResultList = new ArrayList<>();

    // Wir starten mit der Suche nach Codes, falls ein Code-Parameter übergeben wurde
    var codeOpt = csSearchParameter.getCode();
    if (codeOpt.isPresent()) {
      // TODO this is not working for a reason
      List<IdType> codeSearchResultList = searchCodeSystemsByCode(scope, codeOpt.get(), null);

      // Wenn die Ergebnisliste leer ist, dann brauchen wir auch nicht mehr nach anderen
      // Informationen suchen und können das Ergebnis zurückgeben.
      if (codeSearchResultList.isEmpty()) {
        return codeSearchResultList;
      }

      // Wenn keine weiteren Parameter vorhanden sind und die Ergebnisse auch nicht sortiert werden
      // sollen, dann können wir hier aufhören und das Ergebnis zurückgeben
      if (!csSearchParameter.isContentSearch()
          && !csSearchParameter.isMetadataSearch()
          && !csSearchParameter.isSortSearch()) {
        return codeSearchResultList;
      }

      // Zwischenspeichern der Suchergebnisse für die weitere Suche
      preliminaryCodeSearchResultList = codeSearchResultList;
    }

    // _content-Query, um nach Inhalten zu suchen
    var contentOpt = csSearchParameter.getContent();
    if (contentOpt.isPresent()) {

      // Content-Suche anstoßen und dabei die vorherigen Suchergebnisse berücksichtigen
      List<IdType> contentSearchResultList =
          searchCodeSystemsByContent(scope, contentOpt.get(), preliminaryCodeSearchResultList);

      // Wenn die Ergebnisliste leer ist, dann brauchen wir auch nicht mehr nach anderen
      // Informationen suchen und können das Ergebnis zurückgeben.
      if (contentSearchResultList.isEmpty()) {
        return contentSearchResultList;
      }

      // Wenn keine weiteren Parameter vorhanden sind und die Ergebnisse auch nicht sortiert werden
      // sollen, dann können wir hier aufhören und das Ergebnis zurückgeben
      if (!csSearchParameter.isMetadataSearch() && !csSearchParameter.isSortSearch()) {
        return contentSearchResultList;
      }

      // Zwischenspeichern der Suchergebnisse für die weitere Suche
      preliminaryCodeSearchResultList = contentSearchResultList;
    }

    // Metadaten-Query, um nach konkreten Metadaten zu suchen
    Query metadateQuery =
        CodeSystemSearchHelper.prepareMetadataQuery(
            csSearchParameter, scope, preliminaryCodeSearchResultList, isCapabilitySearch);
    // Suche über den Suchindex anstoßen
    return CodeSystemSearchHelper.executeQuery(
        fhirTermsInMemoryRepresentation.getLuceneSearcher(), metadateQuery);
  }

  /**
   * Sucht alle (über die übergebene Liste referenzierten) CodeSyteme, die einen bestimmten Code
   * enthalten
   *
   * @param scope Suchbereich
   * @param codeParam Suchparameter (Code)
   * @param idPreFilter Liste mit IdType-Objekten, die die Suche auf bestimmte CodeSysteme
   *     einschränken
   * @return Liste mit IdType-Objekten, die auf die gefundenen CodeSysteme verweisen
   */
  public List<IdType> searchCodeSystemsByCode(
      String scope, TokenParam codeParam, List<IdType> idPreFilter)
      throws NotImplementedException, UnknownScopeException {

    if (!fhirTermsInMemoryRepresentation.getScopeMap().containsKey(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' not found");
    }

    // Lucene-Query vorbereiten
    Query conceptQuery =
        CodeSystemConceptSearchHelper.prepareCodeSystemConceptQuery(scope, codeParam, idPreFilter);

    // Suche über den Suchindex anstoßen und Ergebnisse zurückgeben
    return CodeSystemConceptSearchHelper.executeCodeSystemConceptQuery(
        fhirTermsInMemoryRepresentation.getLuceneSearcher(), conceptQuery);
  }

  /**
   * Sucht nach CodeSystem-Ressourcen, die einen bestimmten Inhalt haben. Die Suche ist dabei
   * relativ ungenau und erstreckt sich über ausgewählte Felder, die bei der Indizierung
   * entsprechend zusammengezogen wurden sowie auf Konzept-Codes, Display-Werte und zusätzliche
   * Synonyme
   *
   * @param scope Suchbereich
   * @param contentParam Suchparameter (Inhalt)
   * @param preFilter Liste mit IdType-Objekten, die die Suche auf bestimmte CodeSysteme
   *     einschränken
   * @return Liste mit IdType-Objekten, die auf die gefundenen CodeSysteme verweisen
   * @throws NotImplementedException Sofern, die Methode noch nicht implementiert wurde, wird eine
   *     NotImplementedException geworfen
   */
  public List<IdType> searchCodeSystemsByContent(
      String scope, StringParam contentParam, List<IdType> preFilter)
      throws NotImplementedException, UnknownScopeException {

    if (!fhirTermsInMemoryRepresentation.getScopeMap().containsKey(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' not found");
    }

    Query contentQuery =
        CodeSystemConceptSearchHelper.prepareCodeSystemContentQuery(contentParam, scope, preFilter);

    // Suche über den Suchindex anstoßen und Ergebnisse zurückgeben
    return CodeSystemConceptSearchHelper.executeCodeSystemConceptQuery(
        fhirTermsInMemoryRepresentation.getLuceneSearcher(), contentQuery);
  }
}
