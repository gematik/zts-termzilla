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
package de.gematik.zts.termzilla.terminologies.loinc.provider.cs.lookup;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.LoincTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincDesignationManager;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincFhirDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.helper.LoincLanguageHelper;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import de.gematik.zts.termzilla.terminologies.loinc.provider.AdapterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * Implements the FHIR {@code $lookup} operation for LOINC.
 *
 * <p>Resolves a LOINC concept by code and returns its properties, designations, and display in a
 * {@link CsLookupResult}. Supports both {@code system+code} and {@code Coding} input variants as
 * defined by the FHIR specification.
 */
@Slf4j
public class LoincCsLookupOperation {

  private final LoincCodeSystemService loincCodeSystemService;
  private final Loinc loinc;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new instance and initialises all dependencies from the given service.
   *
   * @param loincCodeSystemService the LOINC code system service providing access to the terminology
   *     model and version mappings
   */
  public LoincCsLookupOperation(LoincCodeSystemService loincCodeSystemService) {
    this.loincCodeSystemService = loincCodeSystemService;
    LoincTerminologyManager loincTerminologyManager =
        loincCodeSystemService.getLoincTerminologyManager();
    this.loinc = loincTerminologyManager.getLoinc();
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Executes the {@code $lookup} operation for the given request parameters.
   *
   * <p>Accepts either a {@code Coding} parameter or explicit {@code system} and {@code code}
   * parameters. When a {@code Coding} is provided, its fields are extracted and the call is
   * delegated to the overloaded variant that takes individual string parameters.
   *
   * @param param the parsed lookup request parameters
   * @return the lookup result containing display, properties, and designations
   * @throws InvalidRequestException if neither a coding nor a system parameter is present
   * @throws UnsupportedTerminologyException if the requested LOINC version is not available
   */
  public CsLookupResult lookupCode(CsLookupParameter param)
      throws InvalidRequestException, UnsupportedTerminologyException, NotImplementedException {

    // Ergänzende Validierung der Parameter, die über die Standard-Validierung der HAPI-Parameter
    // hinausgeht
    LoincCsLookupParameterValidator.validateParameters(param);

    Coding codingParam = param.getCodingParam();

    // Variante 1: Ein Coding wurde übergeben
    if (codingParam != null && !codingParam.isEmpty()) {
      param.setSystemParam(new UriType(codingParam.getSystem()));
      param.setVersionParam(new StringType(codingParam.getVersion()));
      param.setCodeParam(new CodeType(codingParam.getCode()));
      param.setCodingParam(null);

      // Erneuter Aufruf der Methode mit den jetzt gesetzten system-, version- und code-Parametern,
      // damit die Logik für die System+Code Variante weiterverwendet werden kann
      return lookupCode(param);
    }

    // Variante 2: Es wurden system- und code-Parameter übergeben
    if (param.getSystemParam() != null) {

      // The system for the code that is to be located
      String system = param.getSystemParam().getValue();

      // The version of the system, if one was provided in the source data
      String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;

      // The code that is to be located. If a code is provided, a system must be provided
      String code = param.getCodeParam() != null ? param.getCodeParam().getValue() : null;

      String displayLanguageParameter =
          param.getDisplayLanguageParam() != null
              ? param.getDisplayLanguageParam().getValue()
              : null;

      // The requested language for display (see $expand.displayLanguage)
      LanguageParameter displayLangParam =
          displayLanguageParameter != null
              ? LanguageParameter.parse(displayLanguageParameter)
              : null;

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
              .displayLanguageParameter(displayLangParam)
              .systemDefaultLanguage(param.getLanguageDefaultSystemLanguage())
              .build();

      // A property that the client wishes to be returned in the output. If no properties are
      // specified,
      // the server chooses what to return. The following properties are defined for all code
      // systems: url,
      // name, version (code system info) and code information: display, definition, designation,
      // parent and child, and for designations, lang.X where X is a designation language code.
      // Some of the properties are returned explicit in named parameters (when the names match),
      // and the rest (except for lang.X) in the property parameter group
      Set<String> requestedProperties =
          param.getPropertyParam() != null
              ? param.getPropertyParam().stream()
                  .map(CodeType::getValue)
                  .filter(StringUtils::isNotEmpty)
                  .collect(Collectors.toSet())
              : new HashSet<>();

      // Code nachschlagen
      return lookupCode(system, version, code, lsp, requestedProperties);

    } else {
      // Hier sollten wir eigentlich nicht landen, da die Parameter bereits zuvor validiert wurden,
      // aber sicherheitshalber fangen wir diesen Fall hier nochmal ab
      throw new InvalidRequestException("Neither system nor coding parameter is present.");
    }
  }

  /**
   * Executes the {@code $lookup} operation for a concept identified by system, version, and code.
   *
   * <p>Resolves the requested LOINC version first, then fetches the concept from the in-memory
   * model. Display is determined via {@link LoincDesignationManager} using the language selection
   * priority chain. Properties and designations are added based on {@code requestedPropertySet};
   * when the set is empty, all default properties are returned.
   *
   * @param system the LOINC system URL
   * @param version the requested FHIR version string, or {@code null} to use the default
   * @param code the LOINC concept code to look up
   * @param languageSelectionParameter the full language selection priority chain for display
   *     resolution
   * @param requestedPropertySet the set of property names requested by the client
   * @return the lookup result
   * @throws UnsupportedTerminologyException if the requested version is not supported
   */
  public CsLookupResult lookupCode(
      String system,
      String version,
      String code,
      LanguageSelectionParameter languageSelectionParameter,
      Set<String> requestedPropertySet)
      throws UnsupportedTerminologyException {

    log.debug(
        "invoke $lookup with system='{}', version='{}', code='{}', requestedProperties='{}'",
        system,
        version,
        code,
        requestedPropertySet);

    // Ermitteln der zu verwendenden LOINC-Version
    LoincVersion loincVersion;
    if (StringUtils.isNoneEmpty(version)) {
      loincVersion = loincCodeSystemService.getFhirVersionToLoincVersionMap().get(version);
      if (loincVersion == null) {
        throw new UnsupportedTerminologyException(
            "The requested LOINC version '" + version + "' is not supported.");
      }
    } else {
      loincVersion = loinc.getDefaultLoincVersion();
    }

    // Vorbereiten des Ergebnisobjektes
    CsLookupResult csLookupResult = new CsLookupResult();
    csLookupResult.setSearchedForSystem(system);
    csLookupResult.setSearchedForCode(code);
    csLookupResult.setSearchedForProperties(requestedPropertySet);

    try {
      // Abrufen des Konzepts aus dem Modell. Wir suchen explizit auch nach inaktiven Konzepten, da
      // hierzu im FHIR-Standard nichts weiter gesagt wird
      Set<String> languageSet = LoincLanguageHelper.buildLanguageSet(languageSelectionParameter);
      LoincConcept concept = loinc.getLoincConcept(code, loincVersion, true, languageSet);

      // Kennzeichnen, dass wir ein Konzept gefunden haben
      csLookupResult.setFound(true);

      // Hinzufügen der CodeSystem-Bezeichnung zum Ergebnis. Wir nutzen hier CodeSystem.name der
      // jeweiligen Version der FHIR-Ressource (z.B. "LOINC")
      csLookupResult.setCodeSystemDisplayName(
          loincCodeSystemService
              .getVersionToCodeSystemMap()
              .get(loincVersion.getVersionString())
              .getName());

      // Hinzufügen des Version-Strings zum Ergebnis (z.B. "2.76")
      csLookupResult.setCodeSystemVersion(loincVersion.getVersionString());

      // Aufbauen des DesignationManagers und Sammeln der Designations des Konzepts
      String defaultDesignationType = LoincDesignationManager.resolveDefaultDesignationType(code);
      LoincDesignationManager designationManager =
          new LoincDesignationManager(defaultDesignationType);
      designationManager.collectDesignationsFromLoincConcept(concept);

      // Hinzufügen des Display-Wertes zum Ergebnis
      LoincFhirDesignation displayDesignation =
          designationManager.getDisplay(languageSelectionParameter);
      csLookupResult.setCodeDisplay(
          displayDesignation != null ? displayDesignation.getValue() : null);

      // Hinzufügen der Properties zum Ergebnis
      addConceptProperties(csLookupResult, requestedPropertySet, concept, loincVersion);

      // Hinzufügen der Designations zum Ergebnis
      addConceptDesignations(csLookupResult, requestedPropertySet, designationManager);

    } catch (UnknownConceptException | InactiveConceptException e) {
      log.info(e.getMessage());
      throw new ResourceNotFoundException(e.getMessage());
    }

    return csLookupResult;
  }

  /**
   * Populates the lookup result with LOINC-specific concept properties based on the requested
   * property set.
   *
   * <p>If no specific properties are requested, the set is expanded to include all implicitly
   * defined default properties ({@code url}, {@code inactive}, {@code child}, {@code parent},
   * {@code COMPONENT}, {@code SYSTEM}, {@code PROPERTY}, {@code SCALE_TYP}, {@code TIME_ASPCT},
   * {@code designation}). This reflects the server's default behaviour, which the FHIR
   * specification leaves to the server's discretion.
   *
   * <p>The {@code url} property is added explicitly with the LOINC canonical URL. All other
   * LOINC-specific properties are delegated to {@link
   * AdapterHelper#convertLoincConceptToFhirConceptPropertyList}.
   *
   * @param csLookupResult the result object to add the properties to
   * @param requestedPropertySet the set of property codes requested by the caller; may be mutated
   *     if it is empty
   * @param concept the resolved LOINC concept
   * @param loincVersion the LOINC version to query
   */
  private void addConceptProperties(
      CsLookupResult csLookupResult,
      Set<String> requestedPropertySet,
      LoincConcept concept,
      LoincVersion loincVersion) {

    // Wenn keine spezifischen Properties angefragt wurden, fügen wir alle verfügbaren Properties
    // hinzu
    // Dies ist unser Default behaviour, da der FHIR-Standard hier dem Server die Freiheit lässt, zu
    // entscheiden, was zurückgegeben wird, solange es sinnvoll ist.
    if (requestedPropertySet.isEmpty()) {
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_URL);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_INACTIVE);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_CHILD);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_PARENT);
      requestedPropertySet.add(ConceptVersion.NAME_COMPONENT);
      requestedPropertySet.add(ConceptVersion.NAME_SYSTEM);
      requestedPropertySet.add(ConceptVersion.NAME_PROPERTY);
      requestedPropertySet.add(ConceptVersion.NAME_SCALE_TYP);
      requestedPropertySet.add(ConceptVersion.NAME_TIME_ASPCT);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_DESIGNATION);
    }

    // Hinzufügen der 'url'-Property (falls gewünscht)
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_URL)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_URL,
                  new StringType(LoincConstants.TERMINOLOGY_CANONICAL_NAME)));
    }

    // Hinzufügen der LOINC-spezifischen Properties zum Ergebnis
    csLookupResult
        .getProperties()
        .addAll(
            AdapterHelper.convertLoincConceptToFhirConceptPropertyList(
                concept, requestedPropertySet, loincVersion, loinc));
  }

  /**
   * Adds designations to the lookup result if the caller requested them.
   *
   * <p>Designations are included when the requested property set contains {@code designation} or
   * any {@code lang.X} property (e.g. {@code lang.de-DE}). In the latter case, only designations
   * matching the extracted BCP 47 language codes are added; unsupported language tags (as
   * determined by {@link LanguageRegistry#isSupported}) are silently ignored. If only {@code
   * designation} is requested without any {@code lang.X} constraint, all available designations are
   * included.
   *
   * @param csLookupResult the result object to add the designations to
   * @param requestedPropertySet the set of property codes requested by the caller
   * @param designationManager the manager holding all collected designations for the concept
   */
  private void addConceptDesignations(
      CsLookupResult csLookupResult,
      Set<String> requestedPropertySet,
      LoincDesignationManager designationManager) {

    // care about language-specific designations (lang.X) if either 'designation' or any 'lang.X'
    // property is requested
    boolean hasLangXProperty = requestedPropertySet.stream().anyMatch(p -> p.startsWith("lang."));
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_DESIGNATION)
        || hasLangXProperty) {

      // Extrahieren von validen BCP 47 language codes from lang.X properties (e.g. "lang.de-DE" ->
      // "de-DE")
      Set<String> langCodes =
          requestedPropertySet.stream()
              .filter(p -> p.startsWith("lang."))
              .map(p -> p.substring(5))
              .filter(LanguageRegistry::isSupported)
              .collect(Collectors.toSet());

      List<LoincFhirDesignation> designationsToAdd;
      if (!langCodes.isEmpty()) {
        // Filtern nach Sprachen
        designationsToAdd = designationManager.getDesignationsByLanguages(langCodes);
      } else {
        // Alle Designations
        designationsToAdd = designationManager.getFullDesignationList();
      }

      for (LoincFhirDesignation designation : designationsToAdd) {
        csLookupResult.getDesignations().add(toConceptDesignation(designation));
      }
    }
  }

  /**
   * Converts a {@link LoincFhirDesignation} to the API-level {@link ConceptDesignation}
   * representation.
   *
   * @param designation the LOINC-specific designation to convert
   * @return the corresponding {@link ConceptDesignation}
   */
  private ConceptDesignation toConceptDesignation(LoincFhirDesignation designation) {
    ConceptDesignation cd = new ConceptDesignation();
    if (designation.getLanguage() != null) {
      cd.setLanguage(designation.getLanguage().getValue());
    }
    if (designation.getUse() != null) {
      cd.setUseSystem(designation.getUse().getSystem());
      cd.setUseCode(designation.getUse().getCode());
      cd.setUseDisplay(designation.getUse().getDisplay());
    }
    cd.setValue(designation.getValue());
    return cd;
  }
}
