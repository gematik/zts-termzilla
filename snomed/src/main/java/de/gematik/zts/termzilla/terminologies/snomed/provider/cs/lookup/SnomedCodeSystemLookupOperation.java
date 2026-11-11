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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.lookup;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SUFFICENTLYDEFINED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.formatExpressionProcessingDisabledMessage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.RuleValidationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignation;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignationManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.snomed.languages.scg.SCGException;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Implements the FHIR {@code $lookup} operation for SNOMED CT.
 *
 * <p>Resolves a SNOMED CT concept or post-coordinated expression by code and returns its
 * properties, designations, and hierarchical relationships in a {@link CsLookupResult}. Supports
 * both {@code system+code} and {@code Coding} input variants as defined by the FHIR specification.
 */
@Slf4j
public class SnomedCodeSystemLookupOperation {

  private final SnomedCodeSystemService snomedCsService;
  private final SCGExpressionParser expressionParser;
  private final DemisSCGProcessor demisSCGProcessor;
  private final Snomed snomed;
  private final boolean restrictedMode;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new instance and initialises all dependencies from the given service.
   *
   * @param snomedCodeSystemService the SNOMED CT code system service providing access to the
   *     terminology model, expression parser, and SCG processor
   */
  public SnomedCodeSystemLookupOperation(SnomedCodeSystemService snomedCodeSystemService) {
    this.snomedCsService = snomedCodeSystemService;
    this.snomed = snomedCodeSystemService.getSnomedTerminologyManager().getSnomed();
    this.expressionParser =
        snomedCodeSystemService.getSnomedTerminologyManager().getExpressionParser();
    this.demisSCGProcessor =
        snomedCodeSystemService.getSnomedTerminologyManager().getScgProcessor();
    this.restrictedMode = snomedCodeSystemService.getSnomedTerminologyManager().isRestrictedMode();
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
   * @throws InvalidRequestException if neither a coding nor a system parameter is present, or if a
   *     property is requested that is not available in restricted mode
   * @throws UnsupportedTerminologyException if the requested SNOMED CT version is not available
   */
  public CsLookupResult lookupCode(CsLookupParameter param)
      throws InvalidRequestException, UnsupportedTerminologyException, NotImplementedException {

    log.debug("invoke $lookup with parameters: {}", param);

    // SNOMED-spezifische Validierung der Input-Parameter
    SnomedCsLookupParameterValidator.validateParameters(param, restrictedMode);

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
   * <p>Resolves the code first as a SNOMED CT concept and, if not found, as a post-coordinated SCG
   * expression validated against the MRCM. Properties included in the result are determined by
   * {@code requestedPropertySet}; when the set is empty, {@code url} is returned by default.
   *
   * <p>Package-private on purpose: parameters are validated in {@link
   * #lookupCode(CsLookupParameter)}, which is the only entry point for callers outside this
   * package.
   *
   * @param system the SNOMED CT system URL
   * @param version the requested release version, or {@code null} to use the default
   * @param code the concept code or SCG expression to look up
   * @param languageSelectionParameter the full language selection priority chain for display
   *     resolution
   * @param requestedPropertySet the set of property names requested by the client
   * @return the lookup result
   * @throws InvalidRequestException if the code is neither a valid concept nor a valid SCG
   *     expression
   * @throws UnsupportedTerminologyException if the requested version is not supported
   */
  CsLookupResult lookupCode(
      String system,
      String version,
      String code,
      LanguageSelectionParameter languageSelectionParameter,
      Set<String> requestedPropertySet)
      throws InvalidRequestException, UnsupportedTerminologyException {

    // Ermitteln der zu verwendenden SNOMED-ReleaseVersion
    SnomedReleaseVersion releaseVersion;
    if (StringUtils.isNoneEmpty(version)) {
      releaseVersion = snomedCsService.getFhirVersionToSnomedReleaseVersionMap().get(version);
      if (releaseVersion == null) {
        throw new UnsupportedTerminologyException(
            "The requested SNOMED CT version '" + version + "' is not supported.");
      }
    } else {
      releaseVersion = snomed.getDefaultSnomedReleaseVersion();
    }

    // Vorbereiten des DesignationHandlings
    SnomedDesignationManager designationManager = new SnomedDesignationManager();

    // Vorbereiten des Ergebnisses
    CsLookupResult csLookupResult = new CsLookupResult();
    csLookupResult.setSearchedForSystem(system);
    csLookupResult.setSearchedForCode(code);
    csLookupResult.setSearchedForProperties(requestedPropertySet);

    // Expect the worst
    csLookupResult.setFound(false);

    // Hinzufügen der CodeSystem-Bezeichnung zum Ergebnis
    csLookupResult.setCodeSystemDisplayName(SnomedConstants.TERMINOLOGY_DISPLAY_NAME);

    // Hinzufügen des Version-Strings zum Ergebnis
    csLookupResult.setCodeSystemVersion(releaseVersion.getFhirVersion());

    // Prüfen, ob es sich um ein gültiges Konzept handelt
    SnomedConcept concept = null;
    boolean conceptIsExpression = false;
    try {
      // Abrufen des Konzepts aus dem Modell. Wir suchen explizit auch nach inaktiven Konzepten, da
      // hierzu im FHIR-Standard nichts weiter gesagt wird
      concept = snomed.getSnomedConceptWithDescription(code, releaseVersion, true);

      // Hinzufügen der Concept Descriptions zum DesignationMananger
      designationManager.collectDesignationsFromSnomedConcept(concept);

    } catch (UnknownConceptException e) {

      if (!snomedCsService.getSnomedTerminologyManager().isExpressionProcessingEnabled()) {
        throw new InvalidRequestException(
            formatExpressionProcessingDisabledMessage(code, restrictedMode), e);
      }
      // Jetzt müssen wir noch als fall-back checken, ob es sich um eine valide Expression handelt
      try {
        // try to parse the expression
        Expression expression = expressionParser.parseExpression(code);
        demisSCGProcessor.validateAgainstMRCM(expression, releaseVersion);
        conceptIsExpression = true;
        // TODO: Aktuell berücksichtigen wir hier noch nicht den Sprachparameter. Dies macht den
        //  Umbau des SCGProcessors notwendig.
        designationManager.collectDesignationsFromSnomedExpression(
            expression, releaseVersion, snomedCsService.getSnomedTerminologyManager());

        // Hinzufügen des Display-Wertes zum Ergebnis
        csLookupResult.setCodeDisplay(demisSCGProcessor.toTermString(expression, releaseVersion));

      } catch (UnknownConceptException
          | UnsupportedTerminologyException
          | RuleValidationException
          | SimpleExpressionConstraintException
          | InactiveConceptException
          | SCGException ex) {
        throw new InvalidRequestException(
            "The provided code '"
                + code
                + "' is neither a valid SNOMED CT concept code nor a valid SNOMED CT expression: ",
            ex);

        // TODO: können inaktive Konzepte auch in einer Expression vorkommen und falls ja, in
        //  welcher Konstellation?

      }
    }

    // Okay, wir haben entweder ein gültiges Konzept oder eine gültige Expression gefunden.
    // In beiden Fällen können wir jetzt weitere Informationen beschaffen, die wir für die Antwort
    // brauchen.
    csLookupResult.setFound(true);

    // 'display' setzen
    SnomedDesignation displayDesignation =
        designationManager.getDisplay(languageSelectionParameter);
    csLookupResult.setCodeDisplay(
        displayDesignation != null ? displayDesignation.getValue() : null);

    // Wenn es sich NICHT um eine Expression handelt, besorgen wir uns noch weitere Informationen
    if (!conceptIsExpression) {

      addConceptProperties(csLookupResult, code, requestedPropertySet, releaseVersion, concept);

      addConceptDesignations(csLookupResult, requestedPropertySet, designationManager);
    }

    return csLookupResult;
  }

  /**
   * Populates the lookup result with SNOMED CT-specific concept properties based on the requested
   * property set.
   *
   * <p>If no specific properties are requested, the set is expanded to include all implicitly
   * defined properties ({@code url}, {@code inactive}, {@code child}, {@code parent}, {@code
   * sufficientlyDefined}, {@code moduleId}, {@code designation}). This reflects the server's
   * default behaviour, which the FHIR specification leaves to the server's discretion. In
   * restricted mode, {@code child}, {@code parent} and {@code sufficientlyDefined} are omitted.
   *
   * <p>The following properties are handled:
   *
   * <ul>
   *   <li>{@code url} — the canonical SNOMED CT system URL
   *   <li>{@code child} — direct child concept codes, resolved from the concept graph
   *   <li>{@code parent} — direct parent concept codes, resolved from the concept graph
   *   <li>{@code inactive} — whether the concept is inactive
   *   <li>{@code sufficientlyDefined} — whether the concept is sufficiently defined
   *   <li>{@code moduleId} — the SNOMED CT module the concept belongs to
   * </ul>
   *
   * @param csLookupResult the result object to add the properties to
   * @param code the concept code whose properties are to be collected
   * @param requestedPropertySet the set of property codes requested by the caller; may be mutated
   *     if it is empty
   * @param releaseVersion the SNOMED CT release version to query
   * @param concept the resolved SNOMED CT concept
   */
  private void addConceptProperties(
      CsLookupResult csLookupResult,
      String code,
      Set<String> requestedPropertySet,
      SnomedReleaseVersion releaseVersion,
      SnomedConcept concept) {

    // Wenn keine spezifischen Properties angefragt wurden, fügen wir alle verfügbaren Properties
    // hinzu
    // Dies ist unser Default behaviour, da der FHIR-Standard hier dem Server die Freiheit lässt, zu
    // entscheiden, was zurückgegeben wird, solange es sinnvoll ist.
    if (requestedPropertySet.isEmpty()) {
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_URL);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_INACTIVE);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_MODULE_ID);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_DESIGNATION);

      // Im restricted mode werden keine Konzeptbeziehungen und kein Definitionsstatus ausgeliefert
      if (!restrictedMode) {
        requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_CHILD);
        requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_PARENT);
        requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED);
      }
    }

    // set 'url' property (falls gewünscht)
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_URL)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_URL, new StringType(CanonicalUrl.SNOMEDCT)));
    }

    // Hinzufügen der 'child' properties
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_CHILD)) {
      addChildProperties(code, releaseVersion, csLookupResult);
    }

    // Hinzufügen der 'parent' properties
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_PARENT)) {
      addParentProperties(code, releaseVersion, csLookupResult);
    }

    // Hinzufügen der 'inactive'-Property
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_INACTIVE)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_INACTIVE, new BooleanType(!concept.isActive())));
    }

    // Hinzufügen der 'sufficientlyDefined'-Property
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED,
                  new BooleanType(SC_SUFFICENTLYDEFINED.equals(concept.getDefinitionStatusId()))));
    }

    // Hinzufügen der 'moduleId'-Property
    if (requestedPropertySet.contains(ConceptProperties.PROPERTY_NAME_MODULE_ID)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_MODULE_ID,
                  new StringType(concept.getModuleId())));
    }
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
      SnomedDesignationManager designationManager) {

    // care about language-specific designations (lang.X) if either 'designation' or any 'lang.X'
    // property is requested
    boolean hasLangXProperty = requestedPropertySet.stream().anyMatch(p -> p.startsWith("lang."));
    if (requestedPropertySet.contains("designation") || hasLangXProperty) {

      // Extrahieren von validen BCP 47 language codes from lang.X properties (e.g. "lang.de-DE" ->
      // "de-DE")
      Set<String> langCodes =
          requestedPropertySet.stream()
              .filter(p -> p.startsWith("lang."))
              .map(p -> p.substring(5))
              .filter(LanguageRegistry::isSupported)
              .collect(Collectors.toSet());

      List<SnomedDesignation> designationsToAdd;
      if (!langCodes.isEmpty()) {
        // Filtern nach Sprachen
        designationsToAdd = designationManager.getDesignationsByLanguages(langCodes);
      } else {
        // Alle Designations
        designationsToAdd = designationManager.getFullDesignationList();
      }

      for (SnomedDesignation designation : designationsToAdd) {
        csLookupResult.getDesignations().add(toConceptDesignation(designation));
      }
    }
  }

  /**
   * Adds all direct children of the given concept as {@code child} properties to the result.
   *
   * @param code the concept code whose children are to be added
   * @param releaseVersion the SNOMED CT release version to query
   * @param csLookupResult the result object to populate
   */
  private void addChildProperties(
      String code, SnomedReleaseVersion releaseVersion, CsLookupResult csLookupResult) {
    // Information über graph-Model holen
    try {
      snomed
          .getSnomedGraph()
          .getDirectChildren(code, releaseVersion)
          .forEach(
              childConceptId ->
                  // Hinzufügen der 'child'-Property zur
                  csLookupResult
                      .getProperties()
                      .add(
                          new BaseTypeConceptProperty(
                              ConceptProperties.PROPERTY_NAME_CHILD,
                              new CodeType(childConceptId))));
    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(e.getMessage(), e);
    }
  }

  /**
   * Adds all direct parents of the given concept as {@code parent} properties to the result.
   *
   * @param code the concept code whose parents are to be added
   * @param releaseVersion the SNOMED CT release version to query
   * @param csLookupResult the result object to populate
   */
  private void addParentProperties(
      String code, SnomedReleaseVersion releaseVersion, CsLookupResult csLookupResult) {
    // Information über graph-Model holen
    try {
      snomed
          .getSnomedGraph()
          .getDirectParents(code, releaseVersion)
          .forEach(
              parentConceptId ->
                  // Hinzufügen der 'parent'-Property
                  csLookupResult
                      .getProperties()
                      .add(
                          new BaseTypeConceptProperty(
                              ConceptProperties.PROPERTY_NAME_PARENT,
                              new CodeType(parentConceptId))));
    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(e.getMessage(), e);
    }
  }

  /**
   * Converts a {@link SnomedDesignation} to the API-level {@link ConceptDesignation}
   * representation.
   *
   * @param designation the SNOMED-specific designation to convert
   * @return the corresponding {@link ConceptDesignation}
   */
  private ConceptDesignation toConceptDesignation(SnomedDesignation designation) {
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
