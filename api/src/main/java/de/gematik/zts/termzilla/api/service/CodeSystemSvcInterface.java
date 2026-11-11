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

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

/**
 * Service interface for managing and querying FHIR CodeSystem resources. Provides methods for
 * retrieval, search, validation, and lookup operations on CodeSystems.
 */
public interface CodeSystemSvcInterface {

  // Query methods for supported CodeSystems

  /**
   * Retrieves a list of canonical URLs for all CodeSystems managed by the respective
   * TerminologyManager.
   *
   * @return list of supported CodeSystem canonical URLs
   */
  default List<String> getSupportedCodeSystemCanonicalsList() {
    return new ArrayList<>();
  }

  /**
   * Retrieves all CodeSystems managed by the respective TerminologyManager. Note: No scope
   * restriction is applied; CodeSystems from all scopes are returned.
   *
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return list of supported CodeSystems
   */
  default List<CodeSystem> getCodeSystemList(ResourceRetrievalMode retrievalMode) {
    return new ArrayList<>();
  }

  /**
   * Retrieves all CodeSystems managed by the respective TerminologyManager. Note: No scope
   * restriction is applied; CodeSystems from all scopes are returned. Uses FULL retrieval mode by
   * default.
   *
   * @return list of supported CodeSystems
   */
  default List<CodeSystem> getCodeSystemList() {
    return getCodeSystemList(ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves all CodeSystems managed by the respective TerminologyManager filtered by URL and
   * scope.
   *
   * @param url the canonical URL of the CodeSystem
   * @param scope the scope in which to search
   * @return list of supported CodeSystems matching the criteria
   */
  default List<CodeSystem> getCodeSystemListByUrlAndScope(String url, String scope) {
    return new ArrayList<>();
  }

  // Retrieval methods for individual CodeSystems

  /**
   * Retrieves a single CodeSystem by its ID using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the CodeSystem
   * @return the requested CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getCodeSystem(String scope, IdType theId)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getCodeSystem(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a single CodeSystem by its ID with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the CodeSystem
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getCodeSystem(String scope, IdType theId, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "CodeSystem with id " + theId + " not found in scope " + scope);
  }

  /**
   * Retrieves a single CodeSystem by its URL and version using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the CodeSystem
   * @param version the version of the CodeSystem
   * @return the requested CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getCodeSystem(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getCodeSystem(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a single CodeSystem by its URL and version with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the CodeSystem
   * @param version the version of the CodeSystem
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getCodeSystem(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "CodeSystem with system "
            + url
            + " and version "
            + version
            + " not found in scope "
            + scope);
  }

  /**
   * Retrieves the default CodeSystem by URL and scope using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the CodeSystem
   * @return the default CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getDefaultCodeSystemByUrlAndScope(String scope, String url)
      throws ResourceNotFoundException,
          NotImplementedException,
          UnknownScopeException,
          UnsupportedTerminologyException {
    return getDefaultCodeSystemByUrlAndScope(scope, url, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves the default CodeSystem by URL and scope with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the CodeSystem
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the default CodeSystem
   * @throws ResourceNotFoundException if the CodeSystem is not found
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default CodeSystem getDefaultCodeSystemByUrlAndScope(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException,
          NotImplementedException,
          UnknownScopeException,
          UnsupportedTerminologyException {
    throw new NotImplementedException(
        "Method 'getDefaultCodeSystemByUrlAndScope' not implemented yet");
  }

  // Search methods

  /**
   * Searches for CodeSystems based on provided search parameters.
   *
   * @param scope the scope in which to perform the search
   * @param csSearchParameter search parameters according to FHIR specification
   * @return list of found CodeSystem IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<IdType> searchCodeSystem(String scope, CsSearchParameter csSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return searchCodeSystem(scope, csSearchParameter, false);
  }

  /**
   * Searches for CodeSystems based on provided search parameters.
   *
   * @param scope the scope in which to perform the search
   * @param csSearchParameter search parameters according to FHIR specification
   * @param isCapabilitySearch indicates whether the search is performed as part of a
   *     CapabilityStatement request
   * @return list of found CodeSystem IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  List<IdType> searchCodeSystem(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws NotImplementedException, UnknownScopeException;

  /**
   * Searches for CodeSystems based on a token parameter (for code).
   *
   * @param scope the scope in which to perform the search
   * @param codeParam search parameter according to FHIR specification
   * @param preFilter list of IdType objects to restrict the search to specific CodeSystems
   * @return list of found CodeSystem IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<IdType> searchCodeSystemsByCode(
      String scope, TokenParam codeParam, List<IdType> preFilter)
      throws NotImplementedException, UnknownScopeException {
    throw new NotImplementedException("search by code is not implemented yet");
  }

  /**
   * Searches for CodeSystems based on a string parameter (for _content).
   *
   * @param scope the scope in which to perform the search
   * @param contentParam search parameter according to FHIR specification
   * @param preFilter list of IdType objects to restrict the search to specific CodeSystems
   * @return list of found CodeSystem IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<IdType> searchCodeSystemsByContent(
      String scope, StringParam contentParam, List<IdType> preFilter)
      throws NotImplementedException, UnknownScopeException {
    throw new NotImplementedException("search by _content is not implemented yet");
  }

  // Operation methods

  /**
   * Validates a code based on provided parameters.
   *
   * @param scope the scope in which to perform the validation
   * @param csValidateCodeParameter parameters for code validation
   * @param languageSystemDefault the default language parameter derived from the system
   *     configuration, used as a fallback when no language is explicitly requested
   * @return validation result as parameter object
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default ValidateCodeResult validateCode(
      String scope,
      CsValidateCodeParameter csValidateCodeParameter,
      LanguageParameter languageSystemDefault)
      throws NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {
    throw new NotImplementedException("validate code is not implemented yet");
  }

  /**
   * Looks up a code based on provided parameters.
   *
   * @param scope the scope in which to perform the lookup
   * @param csLookupParameter parameters for code lookup
   * @return lookup result as parameter object
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default CsLookupResult lookupCode(String scope, CsLookupParameter csLookupParameter)
      throws NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {
    throw new NotImplementedException("lookup code is not implemented yet");
  }

  /**
   * Searches for matching codes based on provided parameters.
   *
   * @param scope the scope in which to perform the search
   * @param csFindMatchesParameter parameters for finding matching codes
   * @return search result as parameter object
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default CsFindMatchesResult findMatches(
      String scope, CsFindMatchesParameter csFindMatchesParameter)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    throw new NotImplementedException("find matches is not implemented yet");
  }
}
