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
package de.gematik.zts.termzilla.api.service;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;

/**
 * Service interface for managing and querying FHIR ConceptMap resources. Provides methods for
 * retrieval and search operations on ConceptMaps.
 */
public interface ConceptMapSvcInterface {

  // Query methods for supported ConceptMaps

  /**
   * Retrieves a list of canonical URLs for all ConceptMaps managed by the respective
   * TerminologyManager.
   *
   * @return list of supported ConceptMap canonical URLs
   */
  default List<String> getSupportedConceptMapCanonicalList() {
    return new ArrayList<>();
  }

  /**
   * Retrieves all ConceptMaps managed by the respective TerminologyManager using FULL retrieval
   * mode.
   *
   * @return list of all managed ConceptMaps
   */
  default List<ConceptMap> getConceptMapList() {
    return getConceptMapList(ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves all ConceptMaps managed by the respective TerminologyManager with specified retrieval
   * mode.
   *
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return list of all managed ConceptMaps
   */
  default List<ConceptMap> getConceptMapList(ResourceRetrievalMode retrievalMode) {
    return new ArrayList<>();
  }

  // Retrieval methods for individual ConceptMaps

  /**
   * Retrieves a single ConceptMap by its ID using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the ConceptMap
   * @return the requested ConceptMap
   * @throws ResourceNotFoundException if the ConceptMap is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ConceptMap getConceptMap(String scope, IdType theId)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getConceptMap(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a single ConceptMap by its ID with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the ConceptMap
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested ConceptMap
   * @throws ResourceNotFoundException if the ConceptMap is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ConceptMap getConceptMap(String scope, IdType theId, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "ConceptMap with 'id' = " + theId.getIdPart() + " not found.");
  }

  /**
   * Retrieves ConceptMaps by canonical URL using FULL retrieval mode. Since multiple versions of
   * the ConceptMap may exist, a list is returned.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ConceptMaps
   * @return list of ConceptMaps matching the canonical URL
   * @throws ResourceNotFoundException if no ConceptMaps are found
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<ConceptMap> getConceptMapList(String scope, String url)
      throws ResourceNotFoundException, UnknownScopeException {
    return getConceptMapList(scope, url, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves ConceptMaps by canonical URL with specified retrieval mode. Since multiple versions
   * of the ConceptMap may exist, a list is returned.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ConceptMaps
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return list of ConceptMaps matching the canonical URL
   * @throws ResourceNotFoundException if no ConceptMaps are found
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<ConceptMap> getConceptMapList(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException {
    throw new ResourceNotFoundException("ConceptMap with 'url' = " + url + " not found.");
  }

  /**
   * Retrieves a ConceptMap by its canonical URL and version using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ConceptMap
   * @param version the version of the ConceptMap
   * @return the requested ConceptMap
   * @throws ResourceNotFoundException if the ConceptMap is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ConceptMap getConceptMap(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getConceptMap(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a ConceptMap by its canonical URL and version with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ConceptMap
   * @param version the version of the ConceptMap
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested ConceptMap
   * @throws ResourceNotFoundException if the ConceptMap is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ConceptMap getConceptMap(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "ConceptMap with 'url' = " + url + " and 'version' = " + version + " not found.");
  }

  // Search methods

  /**
   * Searches for ConceptMaps based on provided search parameters.
   *
   * @param scope the scope in which to perform the search
   * @param cmSearchParameter search parameters according to FHIR specification
   * @return list of found ConceptMap IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<IdType> searchConceptMap(String scope, CmSearchParameter cmSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return new ArrayList<>();
  }
}
