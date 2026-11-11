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

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.findmatches.FhirTermsCodeSystemFindMatchesOperation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.lookup.FhirTermsCodeSystemLookupOperation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.search.FhirTermsCodeSystemSearchOperation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.validatecode.FhirTermsCodeSystemValidateCodeOperation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

/**
 * CodeSystem service implementation for generic FHIR terminology packages.
 *
 * <p>Provides CodeSystem operations for terminologies loaded from FHIR packages, including:
 *
 * <ul>
 *   <li>CodeSystem retrieval by ID, URL, and version
 *   <li>CodeSystem search with FHIR search parameters
 *   <li>Code validation against CodeSystems
 *   <li>Code lookup operations
 *   <li>Finding matching codes based on search criteria
 * </ul>
 *
 * @see CodeSystemSvcInterface
 * @see FhirTermsTerminologyManager
 */
@Slf4j
public class FhirTermsCodeSystemService implements CodeSystemSvcInterface {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  private final FhirTermsCodeSystemSearchOperation fhirTermsCodeSystemSearchOperation;
  private final FhirTermsCodeSystemValidateCodeOperation fhirTermsCodeSystemValidateCodeOperation;
  private final FhirTermsCodeSystemLookupOperation fhirTermsCodeSystemLookupOperation;
  private final FhirTermsCodeSystemFindMatchesOperation fhirTermsCodeSystemFindMatchesOperation;

  /**
   * Creates a new CodeSystem service for FHIR terminology packages.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to in-memory
   *     representations and operations
   */
  public FhirTermsCodeSystemService(FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();

    this.fhirTermsCodeSystemSearchOperation =
        new FhirTermsCodeSystemSearchOperation(fhirTermsTerminologyManager);
    this.fhirTermsCodeSystemValidateCodeOperation =
        new FhirTermsCodeSystemValidateCodeOperation(fhirTermsTerminologyManager);
    this.fhirTermsCodeSystemLookupOperation =
        new FhirTermsCodeSystemLookupOperation(fhirTermsTerminologyManager);
    this.fhirTermsCodeSystemFindMatchesOperation =
        new FhirTermsCodeSystemFindMatchesOperation(fhirTermsTerminologyManager);
  }

  // Query methods for supported CodeSystems

  /** {@inheritDoc} */
  @Override
  public List<String> getSupportedCodeSystemCanonicalsList() {
    if (fhirTermsInMemoryRepresentation != null) {
      return fhirTermsInMemoryRepresentation.getSupportedCanonicals(CodeSystem.class);
    } else {
      return List.of();
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<CodeSystem> getCodeSystemList() {
    return getCodeSystemList(ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public List<CodeSystem> getCodeSystemList(ResourceRetrievalMode retrievalMode) {
    if (fhirTermsInMemoryRepresentation != null) {
      return fhirTermsInMemoryRepresentation.getResourceList(CodeSystem.class, retrievalMode);
    } else {
      return List.of();
    }
  }

  // Retrieval methods for individual CodeSystems

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return getCodeSystem(scope, theId, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId, ResourceRetrievalMode retrievalMode)
      throws UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByIdAndScope(
        CodeSystem.class, scope, theId.getIdPart(), retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getCodeSystem(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
        CodeSystem.class, scope, url, version, retrievalMode);
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(String scope, String url)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    return getDefaultCodeSystemByUrlAndScope(scope, url, ResourceRetrievalMode.FULL);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Note: The CodeSystem selected when no version is specified is considered the 'default'
   * CodeSystem. This assumption is valid because scopes are structured accordingly.
   */
  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    // Note: The CodeSystem selected when no version is specified is our sought 'default'
    // CodeSystem.
    // We can assume this because we have structured the scopes accordingly.
    return fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
        CodeSystem.class, scope, url, null, retrievalMode);
  }

  // Search methods

  /** {@inheritDoc} */
  @Override
  public List<IdType> searchCodeSystem(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws NotImplementedException, UnknownScopeException {
    return fhirTermsCodeSystemSearchOperation.searchCodeSystems(
        scope, csSearchParameter, isCapabilitySearch);
  }

  /** {@inheritDoc} */
  @Override
  public List<IdType> searchCodeSystemsByCode(
      String scope, TokenParam codeParam, List<IdType> preFilter)
      throws NotImplementedException, UnknownScopeException {
    return fhirTermsCodeSystemSearchOperation.searchCodeSystemsByCode(scope, codeParam, preFilter);
  }

  /** {@inheritDoc} */
  @Override
  public List<IdType> searchCodeSystemsByContent(
      String scope, StringParam contentParam, List<IdType> preFilter)
      throws NotImplementedException, UnknownScopeException {
    return fhirTermsCodeSystemSearchOperation.searchCodeSystemsByContent(
        scope, contentParam, preFilter);
  }

  // Operation methods

  /**
   * {@inheritDoc}
   *
   * <p>Supports three parameter combinations:
   *
   * <ul>
   *   <li>system parameter - validates code against specified CodeSystem
   *   <li>coding parameter - validates using Coding datatype (not yet implemented)
   *   <li>codeableConcept parameter - validates using CodeableConcept datatype (not yet
   *       implemented)
   * </ul>
   */
  @Override
  public ValidateCodeResult validateCode(
      String scope,
      CsValidateCodeParameter csValidateCodeParameter,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException {

    return this.fhirTermsCodeSystemValidateCodeOperation.validateCode(
        scope, csValidateCodeParameter, languageSystemDefault);
  }

  /** {@inheritDoc} */
  @Override
  public CsLookupResult lookupCode(String scope, CsLookupParameter csLookupParameter)
      throws UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException,
          NotImplementedException {
    return this.fhirTermsCodeSystemLookupOperation.lookupCode(scope, csLookupParameter);
  }

  /** {@inheritDoc} */
  @Override
  public CsFindMatchesResult findMatches(
      String scope, CsFindMatchesParameter csFindMatchesParameter)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    return this.fhirTermsCodeSystemFindMatchesOperation.findMatches(csFindMatchesParameter, scope);
  }
}
