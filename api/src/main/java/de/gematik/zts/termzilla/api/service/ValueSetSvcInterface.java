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
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Service interface for managing and querying FHIR ValueSet resources. Provides methods for
 * retrieval, search, expansion, and validation operations on ValueSets.
 */
public interface ValueSetSvcInterface {

  // Query methods for supported ValueSets

  /**
   * Retrieves a list of canonical URLs for all ValueSets managed by the respective
   * TerminologyManager.
   *
   * @return list of supported ValueSet canonical URLs
   */
  default List<String> getSupportedValueSetCanonicalList() {
    return new ArrayList<>();
  }

  /**
   * Retrieves all ValueSets managed by the respective TerminologyManager using FULL retrieval mode.
   *
   * @return list of all managed ValueSets
   */
  default List<ValueSet> getValueSetList() {
    return getValueSetList(ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves all ValueSets managed by the respective TerminologyManager with specified retrieval
   * mode.
   *
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return list of all managed ValueSets
   */
  default List<ValueSet> getValueSetList(ResourceRetrievalMode retrievalMode) {
    return new ArrayList<>();
  }

  /**
   * Checks whether the TerminologyManager supports a specific ValueSet in a specific scope. This is
   * required, for example, for implicit ValueSets.
   *
   * @param scope the scope in which to check
   * @param url the canonical URL of the ValueSet
   * @param version the version of the ValueSet
   * @return {@code true} if the ValueSet is supported, {@code false} otherwise
   * @throws UnknownScopeException if the scope is unknown
   */
  default boolean isValueSetSupported(String scope, String url, String version)
      throws UnknownScopeException {
    return false;
  }

  // Retrieval methods for individual ValueSets

  /**
   * Retrieves a single ValueSet by its ID using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the ValueSet
   * @return the requested ValueSet
   * @throws ResourceNotFoundException if the ValueSet is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ValueSet getValueSet(String scope, IdType theId)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getValueSet(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a single ValueSet by its ID with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param theId the ID of the ValueSet
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested ValueSet
   * @throws ResourceNotFoundException if the ValueSet is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ValueSet getValueSet(String scope, IdType theId, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "ValueSet with 'id' = " + theId.getIdPart() + " not found.");
  }

  /**
   * Retrieves ValueSets by canonical URL using FULL retrieval mode. Since multiple versions of the
   * ValueSet may exist, a list is returned.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ValueSets
   * @return list of ValueSets matching the canonical URL
   * @throws ResourceNotFoundException if no ValueSets are found
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<ValueSet> getValueSetList(String scope, String url)
      throws ResourceNotFoundException, UnknownScopeException {
    return getValueSetList(scope, url, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves ValueSets by canonical URL with specified retrieval mode. Since multiple versions of
   * the ValueSet may exist, a list is returned.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ValueSets
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return list of ValueSets matching the canonical URL
   * @throws ResourceNotFoundException if no ValueSets are found
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<ValueSet> getValueSetList(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException {
    throw new ResourceNotFoundException("ValueSet with 'url' = " + url + " not found.");
  }

  /**
   * Retrieves a ValueSet by its canonical URL and version using FULL retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ValueSet
   * @param version the version of the ValueSet
   * @return the requested ValueSet
   * @throws ResourceNotFoundException if the ValueSet is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ValueSet getValueSet(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getValueSet(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * Retrieves a ValueSet by its canonical URL and version with specified retrieval mode.
   *
   * @param scope the scope in which to perform the retrieval
   * @param url the canonical URL of the ValueSet
   * @param version the version of the ValueSet
   * @param retrievalMode the mode for resource retrieval (FULL or STRIPPED)
   * @return the requested ValueSet
   * @throws ResourceNotFoundException if the ValueSet is not found
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnsupportedTerminologyException if the terminology is not supported
   */
  default ValueSet getValueSet(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    throw new ResourceNotFoundException(
        "ValueSet with 'url' = " + url + " and 'version' = " + version + " not found.");
  }

  // Search methods

  /**
   * Searches for ValueSets based on provided search parameters.
   *
   * @param scope the scope in which to perform the search
   * @param vsSearchParameter search parameters according to FHIR specification
   * @return list of found ValueSet IDs
   * @throws NotImplementedException if the method is not implemented
   * @throws UnknownScopeException if the scope is unknown
   */
  default List<IdType> searchValueSet(String scope, VsSearchParameter vsSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return new ArrayList<>();
  }

  // Expansion methods

  /**
   * Expands a ValueSet based on provided expansion parameters.
   *
   * @param scope the scope in which to perform the expansion
   * @param vsExpandParameter expansion parameters according to FHIR specification
   * @return expanded ValueSet
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default ValueSet expandValueSet(String scope, VsExpandParameter vsExpandParameter)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    throw new NotImplementedException("ValueSet expansion not supported");
  }

  /**
   * Expands a single 'include' or 'exclude' element of a ValueSet definition.
   *
   * @param scope the scope in which to perform the expansion
   * @param include the concept set component to expand
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param version the version to use for expansion
   * @param checkVersion the version to check against
   * @param forceVersion the version to force
   * @return expansion result for the concept set component
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnknownConceptException if a concept is unknown
   */
  default VsExpandPartResult expandInclude(
      String scope,
      ValueSet.ConceptSetComponent include,
      boolean includeInactiveConcepts,
      String version,
      String checkVersion,
      String forceVersion,
      String filter)
      throws NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {
    throw new NotImplementedException("ValueSet expansion not supported");
  }

  /**
   * Expands a single 'include' or 'exclude' element of a ValueSet definition.
   *
   * <p>In addition to the expansion settings, the parameters carry the {@link ProcessingContext},
   * which tells the terminology, among other things, whether the ValueSet is part of the service's
   * content or was provided within the request. Terminologies that do not distinguish between
   * these cases do not need to override this method; it delegates to {@link
   * #expandInclude(String, ValueSet.ConceptSetComponent, boolean, String, String, String,
   * String)}.
   *
   * @param scope the scope in which to perform the expansion
   * @param include the concept set component to expand
   * @param parameter the expansion settings and the processing context
   * @return expansion result for the concept set component
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   * @throws UnknownConceptException if a concept is unknown
   */
  default VsExpandPartResult expandInclude(
      String scope, ValueSet.ConceptSetComponent include, VsExpandIncludeParameter parameter)
      throws NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {
    return expandInclude(
        scope,
        include,
        parameter.includeInactiveConcepts(),
        parameter.version(),
        parameter.checkVersion(),
        parameter.forceVersion(),
        parameter.filter());
  }

  /**
   * Creates an expansion contains component for a ValueSet expansion.
   *
   * @param scope the scope in which to create the component
   * @param conceptBaseInfo base information about the concept
   * @param includeDesignations whether to include designations
   * @param designationParamValueList list of designation parameter values
   * @param displayLanguage the display language
   * @param languageSelectionParameter language selection parameters
   * @param explicitConceptReferenceMap map of explicit concept references
   * @return the created expansion contains component
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    throw new NotImplementedException("ValueSet expansion not supported");
  }

  /**
   * Creates an expansion contains component, additionally applying resolved CodeSystem supplements.
   *
   * <p>The default implementation ignores the supplements and delegates to {@link
   * #createExpansionContainsComponent(String, ConceptBaseInfo, boolean, List, String,
   * LanguageSelectionParameter, MultiValuedMap)} to preserve backward compatibility for terminology
   * managers that do not support supplements.
   *
   * @param scope the scope in which to create the component
   * @param conceptBaseInfo base information about the concept
   * @param includeDesignations whether to include designations
   * @param designationParamValueList list of designation parameter values
   * @param displayLanguage the display language
   * @param languageSelectionParameter language selection parameters
   * @param explicitConceptReferenceMap map of explicit concept references
   * @param supplements resolved supplements keyed by base CodeSystem canonical URL (may be {@code
   *     null} or empty)
   * @return the created expansion contains component
   * @throws NotImplementedException if the method is not implemented
   * @throws UnsupportedTerminologyException if the terminology is not supported
   * @throws UnknownScopeException if the scope is unknown
   */
  default ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      Map<String, SupplementRequest> supplements)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    return createExpansionContainsComponent(
        scope,
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        displayLanguage,
        languageSelectionParameter,
        explicitConceptReferenceMap);
  }
}
