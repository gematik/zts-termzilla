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
package de.gematik.zts.termzilla.server.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Interceptor that validates search parameters against configured allowed parameters.
 *
 * <p>This interceptor ensures that only explicitly allowed search parameters are used in FHIR
 * search operations. It automatically allows FHIR control parameters (e.g., _count, _sort) and
 * supports parameter modifiers (e.g., name:exact, date:gt).
 */
@Slf4j
@Component
@Interceptor
public class SearchParameterValidationInterceptor {

  private final ConfigProperties config;

  private static final Map<String, Set<String>> RESTRICTED_ELEMENTS_BY_RESOURCE =
      Map.of(
          "CodeSystem", Set.of("concept"),
          "ValueSet", Set.of("compose", "expansion"),
          "ConceptMap", Set.of("group"));

  /**
   * Constructs a new SearchParameterValidationInterceptor with the specified configuration.
   *
   * @param config the configuration properties containing allowed search parameters
   */
  public SearchParameterValidationInterceptor(ConfigProperties config) {
    this.config = config;
  }

  /**
   * Intercepts incoming search requests after initial processing to validate search parameters.
   *
   * <p>This method validates search parameters against the configured allowed parameters for the
   * specific resource type. FHIR control parameters are always allowed, and parameter modifiers are
   * properly handled by extracting the base parameter name.
   *
   * @param theRequestDetails the details of the incoming request
   * @param theRequest the HTTP servlet request
   * @param theResponse the HTTP servlet response
   * @throws InvalidRequestException if any search parameter is not allowed for the resource type
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void incomingRequestPostProcessed(
      RequestDetails theRequestDetails,
      HttpServletRequest theRequest,
      HttpServletResponse theResponse) {

    if (theRequestDetails.getRestOperationType() != RestOperationTypeEnum.SEARCH_TYPE) {
      return;
    }

    String resourceType = theRequestDetails.getResourceName();
    if (resourceType == null) {
      return;
    }

    Map<String, String[]> params = theRequest.getParameterMap();

    // check if we are in resource retrieval mode = STRIPPED, we require either _elements or
    // _summary to be present to avoid performance issues
    if (config.getResourceRetrievalMode() == ResourceRetrievalMode.STRIPPED) {
      if (!params.containsKey(Constants.PARAM_ELEMENTS)
          && !params.containsKey(Constants.PARAM_SUMMARY)) {
        log.warn(
            "Search request without _elements or _summary parameter while resource retrieval mode is STRIPPED. This may lead to performance issues. Consider adding _elements or _summary to your search request or use the read operation to retrieve the complete resource directly.");
        throw new InvalidRequestException(
            "Search request without _elements or _summary parameter while resource retrieval mode is STRIPPED. This may lead to performance issues. Consider adding _elements or _summary to your search request or use the read operation to retrieve the complete resource directly.");
      }
      validateElementsParameter(resourceType, params);
    }

    Set<String> allowedParams = config.getAllowedSearchParamsForResource(resourceType);

    if (allowedParams == null || allowedParams.isEmpty()) {
      log.debug("No search parameter restrictions for resource type: {}", resourceType);
      return; // No restrictions
    }

    for (String paramName : params.keySet()) {
      // Skip FHIR control parameters (these are always allowed)
      if (isFhirControlParameter(paramName)) {
        continue;
      }

      // Extract parameter name without modifiers (e.g., "name:exact" -> "name")
      String baseParamName = extractBaseParamName(paramName);

      if (!allowedParams.contains(baseParamName)) {
        log.warn("Unauthorized search parameter: {} for resource: {}", paramName, resourceType);
        throw new InvalidRequestException(
            "Search parameter '"
                + paramName
                + "' is not allowed for "
                + resourceType
                + ". Allowed parameters: "
                + allowedParams);
      }
    }

    log.debug("Search parameters validated successfully for {}", resourceType);
  }

  /**
   * Validates that restricted elements are not requested via the _elements parameter.
   *
   * <p>Certain resource types have elements that should not be retrieved via the _elements
   * parameter due to performance or data volume concerns. This method checks if any restricted
   * elements are requested and throws an exception if found.
   *
   * @param resourceType the FHIR resource type being searched
   * @param params the request parameters
   * @throws InvalidRequestException if restricted elements are requested for the resource type
   */
  private void validateElementsParameter(String resourceType, Map<String, String[]> params) {
    if (!params.containsKey(Constants.PARAM_ELEMENTS)) {
      return;
    }

    Set<String> restrictedElements = RESTRICTED_ELEMENTS_BY_RESOURCE.get(resourceType);
    if (restrictedElements == null || restrictedElements.isEmpty()) {
      return;
    }

    String[] elementsValues = params.get(Constants.PARAM_ELEMENTS);
    if (elementsValues == null || elementsValues.length == 0) {
      return;
    }

    // Parse all requested elements (can be comma-separated in a single value or multiple values)
    Set<String> requestedElements =
        Arrays.stream(elementsValues)
            .flatMap(value -> Arrays.stream(value.split(",")))
            .map(String::trim)
            .filter(element -> !element.isEmpty())
            .collect(java.util.stream.Collectors.toSet());

    // Check if any restricted elements are requested
    for (String restrictedElement : restrictedElements) {
      if (requestedElements.contains(restrictedElement)) {
        log.warn(
            "Restricted element '{}' requested via _elements for resource type: {}",
            restrictedElement,
            resourceType);
        throw new InvalidRequestException(
            "Element '"
                + restrictedElement
                + "' is not allowed in _elements parameter for "
                + resourceType
                + ". Restricted elements: "
                + restrictedElements);
      }
    }

    log.debug("_elements parameter validated successfully for {}", resourceType);
  }

  /**
   * Checks if a parameter is a FHIR control parameter.
   *
   * <p>FHIR control parameters are standard parameters that control the behavior of the search
   * operation (e.g., pagination, formatting, includes) and are always allowed.
   *
   * @param paramName the parameter name to check
   * @return {@code true} if the parameter is a FHIR control parameter, {@code false} otherwise
   */
  private boolean isFhirControlParameter(String paramName) {
    return paramName.startsWith(Constants.PARAM_COUNT)
        || paramName.startsWith(Constants.PARAM_OFFSET)
        || paramName.startsWith(Constants.PARAM_SORT)
        || paramName.startsWith(Constants.PARAM_FORMAT)
        || paramName.startsWith(Constants.PARAM_PRETTY)
        || paramName.startsWith(Constants.PARAM_SEARCH_TOTAL_MODE)
        || paramName.startsWith(Constants.PARAM_SUMMARY)
        || paramName.startsWith(Constants.PARAM_ELEMENTS)
        || paramName.startsWith(Constants.PARAM_INCLUDE)
        || paramName.startsWith(Constants.PARAM_REVINCLUDE)
        || paramName.startsWith(Constants.PARAM_CONTAINED)
        || paramName.startsWith(Constants.PARAM_CONTAINED_TYPE);
  }

  /**
   * Extracts the base parameter name by removing modifiers.
   *
   * <p>FHIR search parameters can have modifiers separated by colons (e.g., "name:exact",
   * "date:gt"). This method extracts the base parameter name for validation purposes.
   *
   * @param paramName the full parameter name including any modifiers
   * @return the base parameter name without modifiers
   */
  private String extractBaseParamName(String paramName) {
    // Handle modifiers like :exact, :contains, :text, etc.
    int colonIndex = paramName.indexOf(':');
    if (colonIndex > 0) {
      return paramName.substring(0, colonIndex);
    }
    return paramName;
  }
}
