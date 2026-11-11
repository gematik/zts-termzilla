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
package de.gematik.zts.termzilla.server.service;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.server.search.FhirResourceComparator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Generic search operation handler for FHIR terminology resources.
 *
 * <p>This class provides common search functionality including:
 *
 * <ul>
 *   <li>Aggregating search results from multiple terminology managers
 *   <li>Loading resources for sorting
 *   <li>Applying sort criteria with fallback to defaults
 *   <li>Extracting sorted resource IDs
 * </ul>
 *
 * @param <T> the search parameter type extending {@link TerminologyResourceSearchParameter}
 * @param <R> the FHIR resource type (CodeSystem, ValueSet, or ConceptMap)
 */
@Slf4j
public class CombiningGenericSearchOperation<
    T extends TerminologyResourceSearchParameter, R extends IBaseResource> {

  private final CombiningTerminologyService combiningTerminologyService;
  private final Class<R> resourceClass;

  /** Functional interface for executing a search on a single terminology manager. */
  @FunctionalInterface
  public interface ManagerSearchFunction<T> {

    /**
     * Executes the search operation on a terminology manager.
     *
     * @param manager the terminology manager
     * @param scope the search scope
     * @param searchParameter the search parameters
     * @return list of matching resource IDs
     * @throws NotImplementedException if the operation is not implemented
     * @throws UnknownScopeException if the scope is unknown
     */
    List<IdType> search(TerminologyManager manager, String scope, T searchParameter)
        throws NotImplementedException, UnknownScopeException;
  }

  /**
   * Creates a new generic search operation handler.
   *
   * @param combiningTerminologyService the combining terminology service
   * @param resourceClass the class of the FHIR resource being searched
   */
  public CombiningGenericSearchOperation(
      CombiningTerminologyService combiningTerminologyService, Class<R> resourceClass) {
    this.combiningTerminologyService = combiningTerminologyService;
    this.resourceClass = resourceClass;
  }

  /**
   * Executes a search operation across all terminology managers and returns sorted resource IDs.
   *
   * @param searchParameter the search parameters
   * @param scope the search scope
   * @param searchFunction the function to execute search on each manager
   * @return list of sorted resource IDs
   * @throws InvalidRequestException if the request is invalid
   */
  public List<IdType> searchAndSort(
      T searchParameter, String scope, ManagerSearchFunction<T> searchFunction)
      throws InvalidRequestException {

    log.debug("Executing {} search operation for scope: {}", resourceClass.getSimpleName(), scope);

    // Collect results from all terminology managers
    List<IdType> resourceIdList = collectSearchResults(searchParameter, scope, searchFunction);

    // Determine sort criteria
    List<String> sortCriteria = extractSortCriteria(searchParameter);
    if (sortCriteria.isEmpty()) {
      sortCriteria = getDefaultSortCriteria();
      log.debug("No sort parameters provided, using default sort: {}", sortCriteria);
    }

    // Load and sort resources
    return sortResources(resourceIdList, sortCriteria, scope);
  }

  /**
   * Collects search results from all terminology managers.
   *
   * @param searchParameter the search parameters
   * @param scope the search scope
   * @param searchFunction the function to execute search on each manager
   * @return aggregated list of resource IDs
   * @throws InvalidRequestException if the request is invalid
   */
  private List<IdType> collectSearchResults(
      T searchParameter, String scope, ManagerSearchFunction<T> searchFunction)
      throws InvalidRequestException {

    List<IdType> resourceIdList = new ArrayList<>();

    for (TerminologyManager manager : combiningTerminologyService.getTerminologyManagerList()) {
      try {
        List<IdType> managerResults = searchFunction.search(manager, scope, searchParameter);
        resourceIdList.addAll(managerResults);
      } catch (NotImplementedException | UnknownScopeException e) {
        throw new InvalidRequestException(e);
      }
    }

    log.debug(
        "Collected {} {} IDs from all managers",
        resourceIdList.size(),
        resourceClass.getSimpleName());
    return resourceIdList;
  }

  /**
   * Loads resources and applies sorting.
   *
   * @param resourceIdList list of resource IDs to load
   * @param sortCriteria sort criteria to apply
   * @param scope the search scope
   * @return list of sorted resource IDs
   */
  private List<IdType> sortResources(
      List<IdType> resourceIdList, List<String> sortCriteria, String scope) {

    if (resourceIdList.isEmpty()) {
      return List.of();
    }

    log.debug("Loading {} resources for sorting by: {}", resourceIdList.size(), sortCriteria);

    // Load resources
    List<IBaseResource> resources =
        resourceIdList.stream()
            .map(id -> loadResource(id, scope))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // Sort resources
    Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
    if (comparator != null) {
      resources.sort(comparator);
      log.debug("Sorted {} resources by: {}", resources.size(), sortCriteria);
    }

    // Extract sorted IDs
    return resources.stream()
        .map(resource -> new IdType(resource.getIdElement().getIdPart()))
        .toList();
  }

  /**
   * Loads a single resource by ID.
   *
   * @param id the resource ID
   * @param scope the search scope
   * @return the loaded resource, or null if not found
   */
  private IBaseResource loadResource(IdType id, String scope) {
    if (resourceClass.equals(CodeSystem.class)) {
      return combiningTerminologyService.getCodeSystem(
          scope, id, combiningTerminologyService.getConfigProperties().getResourceRetrievalMode());
    } else if (resourceClass.equals(ValueSet.class)) {
      return combiningTerminologyService.getValueSet(
          scope, id, combiningTerminologyService.getConfigProperties().getResourceRetrievalMode());
    } else if (resourceClass.equals(ConceptMap.class)) {
      return combiningTerminologyService.getConceptMap(
          scope, id, combiningTerminologyService.getConfigProperties().getResourceRetrievalMode());
    }
    throw new IllegalStateException("Unsupported resource type: " + resourceClass.getName());
  }

  /**
   * Extracts sort criteria from search parameters.
   *
   * @param searchParameter the search parameters
   * @return list of sort field names (with '-' prefix for descending order), or empty list if none
   */
  private List<String> extractSortCriteria(T searchParameter) {
    return searchParameter
        .getSort()
        .map(
            tokenOrListParam ->
                tokenOrListParam.getValuesAsQueryTokens().stream()
                    .filter(token -> token.getValue() != null && !token.getValue().isEmpty())
                    .map(TokenParam::getValue)
                    .toList())
        .orElseGet(List::of);
  }

  /**
   * Gets default sort criteria based on resource type.
   *
   * @return list of default sort field names
   */
  private List<String> getDefaultSortCriteria() {
    if (resourceClass.equals(CodeSystem.class)) {
      return combiningTerminologyService
          .getConfigProperties()
          .getCodeSystemDefaultSearchSort()
          .stream()
          .toList();
    } else if (resourceClass.equals(ValueSet.class)) {
      return combiningTerminologyService
          .getConfigProperties()
          .getValueSetDefaultSearchSort()
          .stream()
          .toList();
    } else if (resourceClass.equals(ConceptMap.class)) {
      return combiningTerminologyService
          .getConfigProperties()
          .getConceptMapDefaultSearchSort()
          .stream()
          .toList();
    }
    throw new IllegalStateException("Unsupported resource type: " + resourceClass.getName());
  }
}
