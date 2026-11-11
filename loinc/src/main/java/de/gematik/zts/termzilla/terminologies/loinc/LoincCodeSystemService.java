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
package de.gematik.zts.termzilla.terminologies.loinc;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
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
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincCodeSystemHelper;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.lookup.LoincCsLookupOperation;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.search.LoincCsSearchOperation;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.validatecode.LoincCsValidateCodeOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

@Slf4j
public class LoincCodeSystemService implements CodeSystemSvcInterface {

  @Getter private final Loinc loinc;
  @Getter private final LoincTerminologyManager loincTerminologyManager;

  // Internal CodeSystem Index
  private List<String> supportedCodeSystemCanonicalList = new ArrayList<>();
  @Getter private List<CodeSystem> codeSystemList = new ArrayList<>();
  @Getter private Map<String, CodeSystem> versionToCodeSystemMap = new HashMap<>();
  @Getter private Map<String, CodeSystem> idToCodeSystemMap = new HashMap<>();
  @Getter private final Map<String, LoincVersion> fhirVersionToLoincVersionMap = new HashMap<>();

  private final ProvisionMode provisionMode;

  // Operationen
  private LoincCsLookupOperation loincCsLookupOperation;
  private LoincCsSearchOperation loincCsSearchOperation;
  private LoincCsValidateCodeOperation loincCsValidateCodeOperation;

  // --------------------------------------------------------------------------------

  public LoincCodeSystemService(LoincTerminologyManager loincTerminologyManager) {
    this.loincTerminologyManager = loincTerminologyManager;
    this.loinc = loincTerminologyManager.getLoinc();

    // set provision mode if provided, otherwise Loinc is SUPPORTED by default
    this.provisionMode =
        loincTerminologyManager.getProperties().getProvisionMode() != null
            ? loincTerminologyManager.getProperties().getProvisionMode()
            : ProvisionMode.SUPPORTED;

    // Initialize codeSystemList
    this.codeSystemList.addAll(initializeCodeSystemList());
    this.supportedCodeSystemCanonicalList.add(LoincConstants.TERMINOLOGY_CANONICAL_NAME);
    this.codeSystemList.forEach(
        codeSystem -> {
          codeSystem.setUserData(ResourceUserData.USERDATA_PROVISION_MODE, provisionMode);

          idToCodeSystemMap.put(codeSystem.getId(), codeSystem);
          versionToCodeSystemMap.put(codeSystem.getVersion(), codeSystem);
          supportedCodeSystemCanonicalList.add(
              LoincConstants.TERMINOLOGY_CANONICAL_NAME
                  .concat("|")
                  .concat(codeSystem.getVersion()));
        });

    // Initialize map to map FHIR versions to LOINC versions
    for (LoincVersion loincVersion : loinc.getSupportedLoincVersions()) {
      fhirVersionToLoincVersionMap.put(loincVersion.getVersionString(), loincVersion);
    }

    // Initialize Operations
    this.loincCsLookupOperation = new LoincCsLookupOperation(this);
    this.loincCsValidateCodeOperation = new LoincCsValidateCodeOperation(this);
    this.loincCsSearchOperation = new LoincCsSearchOperation(this);
  }

  // ----------------------------------------------------------------------------------------
  // Initialization methods
  // ----------------------------------------------------------------------------------------

  private List<CodeSystem> initializeCodeSystemList() {
    List<CodeSystem> resultList = new ArrayList<>();

    loinc
        .getSupportedLoincVersions()
        .forEach(
            version ->
                resultList.add(
                    LoincCodeSystemHelper.getLoincCodeSystem(version.getVersionString())));

    return resultList;
  }

  // --------------------------------------------------------------------------------

  @Override
  public List<String> getSupportedCodeSystemCanonicalsList() {
    if (loinc != null) {
      return supportedCodeSystemCanonicalList;
    } else {
      return List.of();
    }
  }

  @Override
  public List<CodeSystem> getCodeSystemList() {
    if (loinc != null) {
      return codeSystemList;
    } else {
      return List.of();
    }
  }

  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, NotImplementedException {
    String defaultFhirVersion = loinc.getDefaultLoincVersion().getVersionString();
    return codeSystemList.stream()
        .filter(codeSystem -> codeSystem.getUrl().equals(url))
        .filter(codeSystem -> codeSystem.getVersion().equals(defaultFhirVersion))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "CodeSystem with url '" + url + "' not found in scope '" + scope + "'"));
  }

  @Override
  public CodeSystem getDefaultCodeSystemByUrlAndScope(String scope, String url)
      throws ResourceNotFoundException, NotImplementedException {
    return getDefaultCodeSystemByUrlAndScope(scope, url, ResourceRetrievalMode.FULL);
  }

  // --------------------------------------------------------------------------------

  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId, ResourceRetrievalMode retrievalMode) {
    return idToCodeSystemMap.get(theId.getIdPart());
  }

  @Override
  public CodeSystem getCodeSystem(String scope, IdType theId) {
    return getCodeSystem(scope, theId, ResourceRetrievalMode.FULL);
  }

  @Override
  public List<IdType> searchCodeSystem(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws NotImplementedException {
    // return an empty list if provision mode is SUPPORTED
    if (!isCapabilitySearch && this.provisionMode == ProvisionMode.SUPPORTED) {
      return List.of();
    }
    return loincCsSearchOperation.searchCodeSystems(csSearchParameter);
  }

  @Override
  public List<IdType> searchCodeSystemsByCode(
      String scope, TokenParam codeParam, List<IdType> preFilter) throws NotImplementedException {
    throw new NotImplementedException("searchCodeSystem is not yet implemented.");
  }

  @Override
  public List<IdType> searchCodeSystemsByContent(
      String scope, StringParam contentParam, List<IdType> preFilter)
      throws NotImplementedException {
    throw new NotImplementedException("searchCodeSystem is not yet implemented.");
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Überprüfen eines Codes anhand übergebener Parameter
   *
   * @param scope
   * @param csValidateCodeParameter Parameter für die Validierung eines Codes
   * @return Ergebnis der Validierung
   * @throws InvalidRequestException Anfrage ungültig
   * @throws NotImplementedException Methode oder Parameterkonstellation ist noch nicht oder nur
   *     teilweise implementiert
   * @throws UnsupportedTerminologyException Die im Request angegebene Terminologie wird nicht
   *     unterstützt
   */
  @Override
  public ValidateCodeResult validateCode(
      String scope,
      CsValidateCodeParameter csValidateCodeParameter,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException, NotImplementedException, UnsupportedTerminologyException {

    return loincCsValidateCodeOperation.validateCode(
        csValidateCodeParameter, languageSystemDefault);
  }

  // ----------------------------------------------------------------------------------------

  @Override
  public CsLookupResult lookupCode(String scope, CsLookupParameter csLookupParameter)
      throws UnsupportedTerminologyException, NotImplementedException {
    return loincCsLookupOperation.lookupCode(csLookupParameter);
  }

  // ----------------------------------------------------------------------------------------

  @Override
  public CsFindMatchesResult findMatches(
      String scope, CsFindMatchesParameter csFindMatchesParameter)
      throws NotImplementedException, UnsupportedTerminologyException {
    throw new NotImplementedException("Not yet implemented.");
  }
}
