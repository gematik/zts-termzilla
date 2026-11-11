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
package de.gematik.zts.termzilla.terminologies.fhirterms;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cm.search.FhirTermsConceptMapSearchOperation;
import java.util.List;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;

/**
 * ConceptMap service implementation for generic FHIR terminology packages.
 *
 * <p>Provides ConceptMap operations for terminologies loaded from FHIR packages, including:
 *
 * <ul>
 *   <li>ConceptMap retrieval by ID, URL, and version
 *   <li>ConceptMap search with FHIR search parameters
 *   <li>Support checking for specific ConceptMaps
 * </ul>
 *
 * @see ConceptMapSvcInterface
 * @see FhirTermsTerminologyManager
 */
public class FhirTermsConceptMapService implements ConceptMapSvcInterface {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  private final FhirTermsConceptMapSearchOperation fhirTermsConceptMapSearchOperation;

  /**
   * Creates a new ConceptMap service for FHIR terminology packages.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to in-memory
   *     representations and operations
   */
  public FhirTermsConceptMapService(FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();

    this.fhirTermsConceptMapSearchOperation =
        new FhirTermsConceptMapSearchOperation(fhirTermsTerminologyManager);
  }

  // Query methods for supported ConceptMaps

  /** {@inheritDoc} */
  @Override
  public List<String> getSupportedConceptMapCanonicalList() {
    if (fhirTermsInMemoryRepresentation != null) {
      return fhirTermsInMemoryRepresentation.getSupportedCanonicals(ConceptMap.class);
    } else {
      return List.of();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<ConceptMap> getConceptMapList() {
    return getConceptMapList(ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public List<ConceptMap> getConceptMapList(ResourceRetrievalMode retrievalMode) {
    if (fhirTermsInMemoryRepresentation != null) {
      return fhirTermsInMemoryRepresentation.getResourceList(ConceptMap.class, retrievalMode);
    } else {
      return List.of();
    }
  }
  
  // Retrieval methods for individual ConceptMaps

  /** {@inheritDoc} */
  @Override
  public ConceptMap getConceptMap(String scope, IdType theId)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return getConceptMap(scope, theId, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public ConceptMap getConceptMap(String scope, IdType theId, ResourceRetrievalMode retrievalMode)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByIdAndScope(
        ConceptMap.class, scope, theId.getIdPart(), retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public List<ConceptMap> getConceptMapList(String scope, String url)
      throws ResourceNotFoundException, UnknownScopeException {
    return getConceptMapList(scope, url, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public List<ConceptMap> getConceptMapList(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException {
    return fhirTermsInMemoryRepresentation.getResourceListByUrlAndScope(
        ConceptMap.class, url, scope, retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public ConceptMap getConceptMap(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getConceptMap(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public ConceptMap getConceptMap(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
        ConceptMap.class, scope, url, version, retrievalMode);
  }

  // Search methods

  /** {@inheritDoc} */
  @Override
  public List<IdType> searchConceptMap(String scope, CmSearchParameter cmSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return fhirTermsConceptMapSearchOperation.searchConceptMaps(scope, cmSearchParameter);
  }
}
