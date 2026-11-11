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
package de.gematik.zts.termzilla.server.service.cs;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.common.metadatasearch.helper.SystemVersionIdInfo;
import de.gematik.zts.termzilla.server.service.CombiningGenericSearchOperation;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

@Slf4j
public class CombiningCsSearchOperation {

  private CombiningCsSearchOperation() {
    // Private constructor to prevent instantiation
  }

  /**
   * Searches for CodeSystems matching the search parameters.
   *
   * @param combiningTerminologyService the combining terminology service
   * @param csSearchParameter the search parameters
   * @param scope the search scope
   * @param isCapabilitySearch whether this is a capability search
   * @return list of matching CodeSystem IDs, sorted according to search parameters
   * @throws InvalidRequestException if the request is invalid
   */
  public static List<IdType> searchCodeSystems(
      CombiningTerminologyService combiningTerminologyService,
      CsSearchParameter csSearchParameter,
      String scope,
      boolean isCapabilitySearch)
      throws InvalidRequestException {

    CombiningGenericSearchOperation<CsSearchParameter, CodeSystem> searchOperation =
        new CombiningGenericSearchOperation<>(combiningTerminologyService, CodeSystem.class);

    return searchOperation.searchAndSort(
        csSearchParameter,
        scope,
        (manager, s, param) -> manager.cs().searchCodeSystem(s, param, isCapabilitySearch));
  }

  /**
   * Sucht nach CodeSystemen, die dem angegebenen Inhalt entsprechen. Die Suche wird auf den
   * angegebene Scope und preFilter eingeschränkt
   *
   * @param combiningTerminologyService
   * @param contentParam Suchparameter
   * @param scope
   * @param preFilter
   * @return
   * @throws InvalidRequestException
   */
  public static List<IdType> searchCodeSystemsByContent(
      CombiningTerminologyService combiningTerminologyService,
      StringParam contentParam,
      String scope,
      List<IdType> preFilter)
      throws InvalidRequestException {

    // Hinweis: Wir benötigen mehrere Suchanfragen, deren Ergebnisse kombiniert werden:
    // (1) Eine Anfrage erfolgt gegen den MetadataSearch-Index
    // (2) Weitere Anfragen werden gegen die einzelnen TerminologyManager-Konzept-Indices gestellt

    // (1) Suchanfrage gegen den MetadataSearch-Index
    CsSearchParameter csSearchParameter = CsSearchParameter.builder().build();
    csSearchParameter.setContentParam(contentParam);

    // Suche über den Suchindex anstoßen
    List<SystemVersionIdInfo> resultList = new ArrayList<>();

    Set<IdType> metadataSearchResultSet = new HashSet<>();

    // Ergebnisse in eine Liste von Ids umwandeln
    for (SystemVersionIdInfo systemVersionIdInfo : resultList) {
      metadataSearchResultSet.add(systemVersionIdInfo.getId());
    }

    // (2) Anfragen gegen die einzelnen TerminologyManager-Konzept-Indices
    Set<IdType> codeSearchResultSet = new HashSet<>();
    for (TerminologyManager manager : combiningTerminologyService.getTerminologyManagerList()) {
      try {
        codeSearchResultSet.addAll(
            manager.cs().searchCodeSystemsByContent(scope, contentParam, preFilter));
      } catch (NotImplementedException | UnknownScopeException e) {
        throw new InvalidRequestException(e);
      }
    }

    // Ergebnisse kombinieren
    metadataSearchResultSet.addAll(codeSearchResultSet);

    return metadataSearchResultSet.stream().toList();
  }
}
