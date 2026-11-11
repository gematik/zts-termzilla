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
package de.gematik.zts.termzilla.terminologies.snomed;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
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
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.provider.AdapterHelper;
import de.gematik.zts.termzilla.terminologies.snomed.provider.cs.Snomed2FhirCodeSystemAdapter;
import de.gematik.zts.termzilla.terminologies.snomed.provider.cs.findmatches.SnomedCodeSystemFindMatchesOperation;
import de.gematik.zts.termzilla.terminologies.snomed.provider.cs.lookup.SnomedCodeSystemLookupOperation;
import de.gematik.zts.termzilla.terminologies.snomed.provider.cs.search.SnomedCodeSystemSearchOperation;
import de.gematik.zts.termzilla.terminologies.snomed.provider.cs.validatecode.SnomedCodeSystemValidateCodeOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

/**
 * SNOMED CT implementation of the {@link CodeSystemSvcInterface}.
 *
 * <p>Manages FHIR {@link CodeSystem} resources derived from the loaded SNOMED CT release data and
 * delegates FHIR operations ({@code $lookup}, {@code $validate-code}, {@code $find-matches},
 * search) to specialised operation classes. Each supported SNOMED CT release version is represented
 * as an individual {@link CodeSystem} resource and indexed by its FHIR version string.
 *
 * <p>On construction, the service builds the internal CodeSystem index from the {@link Snomed}
 * model, initialises a mapping from FHIR version strings to {@link SnomedReleaseVersion} instances,
 * and creates all operation handlers.
 *
 * @see SnomedCodeSystemLookupOperation
 * @see SnomedCodeSystemValidateCodeOperation
 * @see SnomedCodeSystemFindMatchesOperation
 * @see SnomedCodeSystemSearchOperation
 */
@Slf4j
public class SnomedCodeSystemService implements CodeSystemSvcInterface {

  @Getter private final Snomed snomed;
  @Getter private final SnomedTerminologyManager snomedTerminologyManager;

  private final List<String> supportedCodeSystemCanonicalList = new ArrayList<>();
  private final List<CodeSystem> codeSystemList = new ArrayList<>();
  private final Map<String, CodeSystem> idToCodeSystemMap = new HashMap<>();

  /** Maps FHIR version strings to their corresponding {@link SnomedReleaseVersion}. */
  @Getter
  private final Map<String, SnomedReleaseVersion> fhirVersionToSnomedReleaseVersionMap =
      new HashMap<>();

  private final ProvisionMode provisionMode;

  private final SnomedCodeSystemLookupOperation snomedCodeSystemLookupOperation;
  private final SnomedCodeSystemFindMatchesOperation snomedCodeSystemFindMatchesOperation;
  private final SnomedCodeSystemValidateCodeOperation snomedCodeSystemValidateCodeOperation;
  private final SnomedCodeSystemSearchOperation snomedCodeSystemSearchOperation;

  // --------------------------------------------------------------------------------

  /**
   * Creates a new service instance and initialises all internal indexes and operation handlers.
   *
   * <p>Derives {@link CodeSystem} resources from the SNOMED CT model, builds canonical URL and
   * version mappings, sets the {@link ProvisionMode} (defaulting to {@code SUPPORTED}), and
   * instantiates the operation classes for {@code $lookup}, {@code $validate-code}, {@code
   * $find-matches}, and search.
   *
   * @param snomedTerminologyManager the terminology manager providing access to the SNOMED CT
   *     model, configuration properties, and release versions
   */
  public SnomedCodeSystemService(SnomedTerminologyManager snomedTerminologyManager) {
    this.snomedTerminologyManager = snomedTerminologyManager;
    this.snomed = snomedTerminologyManager.getSnomed();

    // set provision mode if provided, otherwise SNOMED is SUPPORTED by default
    this.provisionMode =
        snomedTerminologyManager.getProperties().getProvisionMode() != null
            ? snomedTerminologyManager.getProperties().getProvisionMode()
            : ProvisionMode.SUPPORTED;

    // Create a list of CodeSystem Resources based on Snomed Representation
    this.codeSystemList.addAll(Snomed2FhirCodeSystemAdapter.calculateSupportedCodeSystems(snomed));

    // Mapping from CodeSystem-Id to CodeSystem-Objekt
    this.codeSystemList.forEach(
        codeSystem -> {
          codeSystem.setUserData(ResourceUserData.USERDATA_PROVISION_MODE, provisionMode);

          idToCodeSystemMap.put(codeSystem.getId(), codeSystem);
        });

    // Create list with supported CodeSystem Canonicals
    this.supportedCodeSystemCanonicalList.add(SnomedConstants.TERMINOLOGY_CANONICAL_NAME);

    List<SnomedReleaseVersion> snomedReleaseVersionList =
        new ArrayList<>(snomed.getSupportedSnomedReleaseVersions());

    // TODO: Check if this is correct
    snomedReleaseVersionList.forEach(
        version ->
            supportedCodeSystemCanonicalList.add(
                SnomedConstants.TERMINOLOGY_CANONICAL_NAME
                    .concat("|")
                    .concat(version.getFhirVersion())));

    // Initialize map to map FHIR versions to SNOMED versions
    for (SnomedReleaseVersion snomedReleaseVersion : snomedReleaseVersionList) {
      fhirVersionToSnomedReleaseVersionMap.put(
          AdapterHelper.convertSnomedReleaseVersionToFhirVersion(snomedReleaseVersion),
          snomedReleaseVersion);
    }

    // Initialize Operations
    this.snomedCodeSystemSearchOperation = new SnomedCodeSystemSearchOperation(this);
    this.snomedCodeSystemValidateCodeOperation = new SnomedCodeSystemValidateCodeOperation(this);
    this.snomedCodeSystemLookupOperation = new SnomedCodeSystemLookupOperation(this);
    this.snomedCodeSystemFindMatchesOperation = new SnomedCodeSystemFindMatchesOperation(this);
  }

  // --------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   *
   * <p>Returns the SNOMED CT canonical URL and all versioned canonical URLs (e.g. {@code
   * http://snomed.info/sct|<version>}) for the loaded release versions. Returns an empty list if
   * the SNOMED CT model is not initialised.
   */
  @Override
  public List<String> getSupportedCodeSystemCanonicalsList() {
    if (snomed != null) {
      return supportedCodeSystemCanonicalList;
    } else {
      return List.of();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns all SNOMED CT {@link CodeSystem} resources derived from the loaded release versions.
   * Returns an empty list if the SNOMED CT model is not initialised.
   */
  @Override
  public List<CodeSystem> getCodeSystemList() {
    if (snomed != null) {
      return codeSystemList;
    } else {
      return List.of();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Falls back to the default SNOMED CT release version when {@code version} is {@code null}.
   */
  @Override
  public CodeSystem getCodeSystem(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {

    String finalVersion =
        version != null ? version : snomed.getDefaultSnomedReleaseVersion().getFhirVersion();
    return codeSystemList.stream()
        .filter(codeSystem -> codeSystem.getUrl().equals(url))
        .filter(codeSystem -> codeSystem.getVersion().equals(finalVersion))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "CodeSystem with url '"
                        + url
                        + "' and version '"
                        + version
                        + "' not found in scope '"
                        + scope
                        + "'"));
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {

    return getCodeSystem(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Uses the default SNOMED CT release version to determine the matching {@link CodeSystem}.
   */
  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException {
    String defaultFhirVersion = snomed.getDefaultSnomedReleaseVersion().getFhirVersion();
    return codeSystemList.stream()
        .filter(codeSystem -> codeSystem.getUrl().equals(url))
        .filter(codeSystem -> codeSystem.getVersion().equals(defaultFhirVersion))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "CodeSystem with url '" + url + "' not found in scope '" + scope + "'"));
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(String scope, String url)
      throws ResourceNotFoundException {
    return getDefaultCodeSystemByUrlAndScope(scope, url, ResourceRetrievalMode.FULL);
  }

  // --------------------------------------------------------------------------------

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId, ResourceRetrievalMode retrievalMode) {
    return idToCodeSystemMap.get(theId.getIdPart());
  }

  /** {@inheritDoc} */
  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId) {
    return getCodeSystem(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Returns an empty list when the provision mode is {@link ProvisionMode#SUPPORTED} and the
   * search is not a capability search, since SNOMED CT resources are only available for operations
   * in that mode.
   */
  @Override
  public List<IdType> searchCodeSystem(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws NotImplementedException {
    // return an empty list if provision mode is SUPPORTED
    // TODO this needs to be refined, when we want to make it more fine grained than setting the
    // provision mode per version
    if (!isCapabilitySearch && this.provisionMode == ProvisionMode.SUPPORTED) {
      return List.of();
    }
    return snomedCodeSystemSearchOperation.searchCodeSystems(csSearchParameter);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NotImplementedException always — search by code is not yet supported for SNOMED CT
   */
  // TODO: OPtional gerade ziehen
  @Override
  public List<IdType> searchCodeSystemsByCode(
      String scope, TokenParam codeParam, List<IdType> preFilter) throws NotImplementedException {
    // TODO: Implementierung für Snomed
    throw new NotImplementedException("searchCodeSystem is not yet implemented.");
  }

  /**
   * {@inheritDoc}
   *
   * @throws NotImplementedException always — search by content is not yet supported for SNOMED CT
   */
  @Override
  public List<IdType> searchCodeSystemsByContent(
      String scope, StringParam contentParam, List<IdType> preFilter)
      throws NotImplementedException {
    // TODO: Implementierung für Snomed
    throw new NotImplementedException("searchCodeSystem is not yet implemented.");
  }

  // ----------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to {@link SnomedCodeSystemValidateCodeOperation}, which resolves the SNOMED
   * release version, looks up the concept or validates the SCG expression, and optionally checks
   * the provided display value against the available designations.
   */
  @Override
  public ValidateCodeResult validateCode(
      String scope,
      CsValidateCodeParameter csValidateCodeParameter,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException, NotImplementedException {

    return this.snomedCodeSystemValidateCodeOperation.validateCode(
        csValidateCodeParameter, languageSystemDefault);
  }

  // ----------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to {@link SnomedCodeSystemLookupOperation} which resolves the code as a SNOMED CT
   * concept or post-coordinated expression.
   */
  @Override
  public CsLookupResult lookupCode(String scope, CsLookupParameter csLookupParameter)
      throws InvalidRequestException, UnsupportedTerminologyException, NotImplementedException {
    return snomedCodeSystemLookupOperation.lookupCode(csLookupParameter);
  }

  // ----------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   *
   * <p>Delegates to {@link SnomedCodeSystemFindMatchesOperation}.
   */
  @Override
  public CsFindMatchesResult findMatches(
      String scope, CsFindMatchesParameter csFindMatchesParameter)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {
    return snomedCodeSystemFindMatchesOperation.findMatches(scope, csFindMatchesParameter);
  }
}
