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
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand.ExpansionContainsComponentBuilder;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand.FhirTermsVsExplicitExpandOperation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.search.FhirTermsValueSetSearchOperation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * ValueSet service implementation for generic FHIR terminology packages.
 *
 * <p>Provides ValueSet operations for terminologies loaded from FHIR packages, including:
 *
 * <ul>
 *   <li>ValueSet retrieval by ID, URL, and version
 *   <li>ValueSet search with FHIR search parameters
 *   <li>ValueSet expansion for include/exclude elements
 *   <li>Expansion contains component creation with designation support
 * </ul>
 *
 * @see ValueSetSvcInterface
 * @see FhirTermsTerminologyManager
 */
@Slf4j
public class FhirTermsValueSetService implements ValueSetSvcInterface {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  private final FhirTermsValueSetSearchOperation fhirTermsValueSetSearchOperation;
  private final FhirTermsVsExplicitExpandOperation fhirTermsVsExplicitExpandOperation;
  private final ExpansionContainsComponentBuilder expansionContainsComponentBuilder;

  /**
   * Creates a new ValueSet service for FHIR terminology packages.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to in-memory
   *     representations and operations
   */
  public FhirTermsValueSetService(FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
    this.fhirTermsValueSetSearchOperation =
        new FhirTermsValueSetSearchOperation(fhirTermsTerminologyManager);
    this.fhirTermsVsExplicitExpandOperation =
        new FhirTermsVsExplicitExpandOperation(fhirTermsTerminologyManager);
    this.expansionContainsComponentBuilder =
        new ExpansionContainsComponentBuilder(
            fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation());
  }

  // Query methods for supported ValueSets

  /** {@inheritDoc} */
  @Override
  public List<String> getSupportedValueSetCanonicalList() {
    return fhirTermsInMemoryRepresentation.getSupportedCanonicals(ValueSet.class);
  }

  /** {@inheritDoc} */
  @Override
  public List<ValueSet> getValueSetList() {
    return getValueSetList(ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public List<ValueSet> getValueSetList(ResourceRetrievalMode retrievalMode) {
    return fhirTermsInMemoryRepresentation.getResourceList(ValueSet.class, retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValueSetSupported(String scope, String url, String version)
      throws UnknownScopeException {
    try {
      if (this.getValueSet(scope, url, version) != null) {
        return true;
      }
    } catch (UnsupportedTerminologyException _) {
      return false;
    }

    return false;
  }

  // Retrieval methods for individual ValueSets

  /** {@inheritDoc} */
  @Override
  public ValueSet getValueSet(String scope, IdType theId)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return getValueSet(scope, theId, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public ValueSet getValueSet(String scope, IdType theId, ResourceRetrievalMode mode)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByIdAndScope(
        ValueSet.class, scope, theId.getIdPart(), mode);
  }

  /** {@inheritDoc} */
  @Override
  public List<ValueSet> getValueSetList(String scope, String url) throws UnknownScopeException {
    return getValueSetList(scope, url, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public List<ValueSet> getValueSetList(
      String scope, String url, ResourceRetrievalMode retrievalMode) throws UnknownScopeException {
    return fhirTermsInMemoryRepresentation.getResourceListByUrlAndScope(
        ValueSet.class, url, scope, retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public ValueSet getValueSet(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getValueSet(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public ValueSet getValueSet(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
        ValueSet.class, scope, url, version, retrievalMode);
  }

  // Search methods

  /** {@inheritDoc} */
  @Override
  public List<IdType> searchValueSet(String scope, VsSearchParameter vsSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return fhirTermsValueSetSearchOperation.searchValueSets(scope, vsSearchParameter);
  }

  // Expansion methods

  /**
   * {@inheritDoc}
   *
   * <p>Supports both extensional (explicit codes) and intensional (filter-based) ValueSet
   * definitions from FHIR packages.
   */
  @Override
  public VsExpandPartResult expandInclude(
      String scope,
      ValueSet.ConceptSetComponent include,
      boolean includeInactiveConcepts,
      String version,
      String checkVersion,
      String forceVersion,
      String filter)
      throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {

    log.debug("expandInclude() - started");

    // Perform the expansion
    VsExpandPartResult vsExpandIncludeResult =
        this.fhirTermsVsExplicitExpandOperation.processIncludeOrExcludeElement(
            include, includeInactiveConcepts, version, checkVersion, forceVersion, filter, scope);

    log.debug(
        "expandInclude() - returning {} conceptBaseInfos",
        vsExpandIncludeResult.getConceptBaseInfos().size());

    return vsExpandIncludeResult;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates expansion components for concepts from FHIR package CodeSystems, including display
   * values and optionally designations based on language selection parameters.
   */
  @Override
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap)
      throws UnsupportedTerminologyException, UnknownScopeException {

    return this.expansionContainsComponentBuilder.createExpansionContainsComponent(
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        languageSelectionParameter,
        explicitConceptReferenceMap,
        scope);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Applies resolved CodeSystem supplements (designations/displays) to the expansion component.
   */
  @Override
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      java.util.Map<
              String,
        SupplementRequest>
          supplements)
      throws UnsupportedTerminologyException, UnknownScopeException {

    return this.expansionContainsComponentBuilder.createExpansionContainsComponent(
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        languageSelectionParameter,
        explicitConceptReferenceMap,
        scope,
        supplements);
  }
}
