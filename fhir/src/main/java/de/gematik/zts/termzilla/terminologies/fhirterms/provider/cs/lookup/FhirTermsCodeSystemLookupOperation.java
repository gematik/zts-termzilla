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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.lookup;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DEFINITION;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_URL;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.CodingConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.interfaces.properties.GenericConceptProperty;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.graph.FhirTermGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignation;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignationManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeSystem.PropertyComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;

// TODO handle abstract parameter
// TODO handle inactive parameter
// TODO care about postcoordinated content (if applicable)

/**
 * Implements the FHIR {@code CodeSystem/$lookup} operation for generic FHIR package terminologies.
 *
 * <p>Given a code and a CodeSystem canonical URL, this operation retrieves the corresponding
 * concept from the in-memory representation and assembles a {@link CsLookupResult} containing:
 *
 * <ul>
 *   <li>The CodeSystem name and version
 *   <li>A language-aware display value, selected according to a four-level priority chain: {@code
 *       displayLanguage} parameter &gt; {@code Accept-Language} header &gt; system default language
 *       &gt; CodeSystem language
 *   <li>All designations of the concept, including a synthetic display designation derived from
 *       {@link CodeSystem.ConceptDefinitionComponent#getDisplay()}
 *   <li>Concept properties requested by the caller, including {@code url}, {@code definition},
 *       hierarchy properties ({@code parent}, {@code child}), and explicit concept properties
 * </ul>
 *
 * <p>Two input variants are supported: {@code system} + {@code code} (standard case) and {@code
 * coding} (currently not implemented and rejected with an appropriate error).
 *
 * @see CsLookupResult
 * @see FhirDesignationManager
 */
@Slf4j
public class FhirTermsCodeSystemLookupOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates a new lookup operation handler.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to in-memory data
   */
  public FhirTermsCodeSystemLookupOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  /**
   * Performs a {@code CodeSystem/$lookup} operation for the given parameters.
   *
   * <p>Accepts two input variants:
   *
   * <ol>
   *   <li>{@code system} + {@code code} — the standard lookup case
   *   <li>{@code coding} — the fields are extracted and the call is delegated to the {@code system}
   *       + {@code code} path
   * </ol>
   *
   * <p>If neither variant is present, an {@link InvalidRequestException} is thrown.
   *
   * @param scope the scope in which the lookup is performed
   * @param param the parsed FHIR {@code $lookup} input parameters
   * @return the lookup result containing display, designations, and properties
   * @throws InvalidRequestException if the parameter combination is invalid or unsupported
   * @throws UnsupportedTerminologyException if the referenced CodeSystem is not available
   * @throws UnknownScopeException if the given scope is not recognized
   * @throws UnknownConceptException if the specified code is not found in the CodeSystem
   */
  public CsLookupResult lookupCode(String scope, CsLookupParameter param)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException,
          NotImplementedException {

    log.debug("Received $lookup request with parameters: {}", param);

    // Parametervalidierung
    FhirTermsCsLookupParameterValidator.validateParameters(param);

    Coding codingParam = param.getCodingParam();

    // Variante 1: Ein Coding wurde übergeben
    if (codingParam != null && !codingParam.isEmpty()) {
      param.setSystemParam(new UriType(codingParam.getSystem()));
      param.setVersionParam(new StringType(codingParam.getVersion()));
      param.setCodeParam(new CodeType(codingParam.getCode()));
      param.setCodingParam(null);

      // Erneuter Aufruf der Methode mit den jetzt gesetzten system-, version- und code-Parametern,
      // damit die Logik für die System+Code Variante weiterverwendet werden kann
      return lookupCode(scope, param);
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
      return lookupCode(scope, system, version, code, lsp, requestedProperties);

    } else {
      // Hier sollten wir eigentlich nicht landen, da die Parameter bereits zuvor validiert wurden,
      // aber sicherheitshalber fangen wir diesen Fall hier nochmal ab
      throw new InvalidRequestException("Neither system nor coding parameter is present.");
    }
  }

  /**
   * Performs the actual code lookup against the in-memory representation and assembles the result.
   *
   * <p>Retrieves the CodeSystem by URL and version, searches for the concept, and — if found —
   * populates the result with name, version, display, designations, and the requested properties.
   * If the concept is not found, the result is returned with {@code found} set to {@code false}.
   *
   * @param scope the scope identifying the set of loaded terminologies
   * @param system the canonical URL of the CodeSystem
   * @param version the requested CodeSystem version, or {@code null} for the default version
   * @param code the code to look up
   * @param languageSelectionParameter the assembled language priority chain for display selection
   * @param requestedPropertySet the set of property codes requested by the caller
   * @return the populated lookup result
   * @throws InvalidRequestException if the request cannot be processed
   * @throws UnsupportedTerminologyException if the CodeSystem is not available in the given scope
   * @throws UnknownScopeException if the scope is not recognized
   * @throws UnknownConceptException if the specified code is not found in the CodeSystem
   */
  private CsLookupResult lookupCode(
      String scope,
      String system,
      String version,
      String code,
      LanguageSelectionParameter languageSelectionParameter,
      Set<String> requestedPropertySet)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {

    // CodeSystem aus In-Memory-Repräsentation holen
    CodeSystem codeSystem =
        fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class, scope, system, version, ResourceRetrievalMode.FULL);

    // Konzept heraussuchen
    CodeSystem.ConceptDefinitionComponent concept =
        CodeSystemHelper.findCodeInCodeSystem(codeSystem, code);

    // Vorbereiten des Lookup-Ergebnisses mit den gesuchten Parametern (unabhängig vom
    // Lookup-Erfolg)
    CsLookupResult csLookupResult =
        initializeLookupResult(system, version, code, requestedPropertySet);

    // Okay, wir scheinen das Konzept gefunden zu haben, jetzt sammeln wir alle Informationen
    // zusammen,
    // die es zum Konzept gibt, und packen sie in das Ergebnis
    csLookupResult.setFound(true);

    // wir überschreiben nochmal den code, um sicherzugehen, dass es keine Probleme mit
    // Groß-/Kleinschreibung gibt,
    // da FHIR-Codes mglw. nicht case-sensitive sind, wir dennoch den Originalcode aus der CS
    // zurückgeben wollen
    csLookupResult.setSearchedForCode(concept.getCode());

    // set 'name' parameter
    csLookupResult.setCodeSystemDisplayName(determineCodeSystemDisplayName(codeSystem));

    // set 'version' parameter
    if (codeSystem.hasVersion()) {
      csLookupResult.setCodeSystemVersion(codeSystem.getVersion());
    }

    // collect all designations for language-aware display selection and designation export
    String csLanguage = codeSystem.getLanguage();
    FhirDesignationManager designationManager = new FhirDesignationManager();
    designationManager.collectDesignationsFromFhirConcept(concept, csLanguage);

    // TODO: über zusätzliche Designations in supplements nachdenken

    // set 'display' parameter – language-aware via designation manager
    FhirDesignation displayDesignation = designationManager.getDisplay(languageSelectionParameter);
    if (displayDesignation != null) {
      csLookupResult.setCodeDisplay(displayDesignation.getValue());
    }

    // Properties setzen
    addConceptProperties(csLookupResult, requestedPropertySet, codeSystem, concept);

    // Designations setzen
    addConceptDesignations(csLookupResult, requestedPropertySet, designationManager);

    return csLookupResult;
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
      FhirDesignationManager designationManager) {
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

      List<FhirDesignation> designationsToAdd;
      if (!langCodes.isEmpty()) {
        // Filtern nach Sprachen
        designationsToAdd = designationManager.getDesignationsByLanguages(langCodes);
      } else {
        // Alle Designations
        designationsToAdd = designationManager.getFullDesignationList();
      }

      for (FhirDesignation designation : designationsToAdd) {
        csLookupResult.getDesignations().add(toConceptDesignation(designation));
      }
    }
  }

  /**
   * Creates a pre-populated {@link CsLookupResult} with the search parameters.
   *
   * <p>The result is initialized with the system, version, code, and requested property set so that
   * these values are always present in the response, regardless of whether the code was found.
   *
   * @param system the CodeSystem canonical URL that was searched
   * @param version the CodeSystem version that was searched, or {@code null}
   * @param code the code that was searched
   * @param requestedPropertySet the set of property codes requested by the caller
   * @return an initialized result object with {@code found} not yet set
   */
  private CsLookupResult initializeLookupResult(
      String system, String version, String code, Set<String> requestedPropertySet) {
    CsLookupResult csLookupResult = new CsLookupResult();
    csLookupResult.setSearchedForSystem(system);
    csLookupResult.setCodeSystemVersion(version);
    csLookupResult.setSearchedForCode(code);
    csLookupResult.setSearchedForProperties(requestedPropertySet);
    return csLookupResult;
  }

  /**
   * Determines the display name for the CodeSystem in the following priority order: {@link
   * CodeSystem#getName()} &gt; {@link CodeSystem#getTitle()} &gt; {@link CodeSystem#getUrl()}.
   *
   * @param codeSystem the CodeSystem resource
   * @return the display name, never {@code null}
   */
  private String determineCodeSystemDisplayName(CodeSystem codeSystem) {
    if (codeSystem.hasName()) {
      return codeSystem.getName();
    }
    if (codeSystem.hasTitle()) {
      return codeSystem.getTitle();
    }
    return codeSystem.getUrl();
  }

  /**
   * Adds {@code parent} properties to the result for all direct parents of the given code.
   *
   * <p>Parents are resolved from the {@link FhirTermGraphRepresentation} of the CodeSystem. If no
   * graph is available, the method returns without adding any properties.
   *
   * @param codeSystem the CodeSystem whose graph is queried
   * @param code the code whose direct parents are to be resolved
   * @param csLookupResult the result object to add the parent properties to
   */
  private void addParentProperties(
      CodeSystem codeSystem, String code, CsLookupResult csLookupResult) {
    FhirTermGraphRepresentation graph =
        fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem);

    if (graph == null) {
      return;
    }

    for (String parent : graph.getAllConceptsWithDirectChild(code)) {
      csLookupResult
          .getProperties()
          .add(new BaseTypeConceptProperty(PROPERTY_NAME_PARENT, new CodeType(parent)));
    }
  }

  /**
   * Adds {@code child} properties to the result for all direct children of the given code.
   *
   * <p>Children are resolved from the {@link FhirTermGraphRepresentation} of the CodeSystem. If no
   * graph is available, the method returns without adding any properties.
   *
   * @param codeSystem the CodeSystem whose graph is queried
   * @param code the code whose direct children are to be resolved
   * @param csLookupResult the result object to add the child properties to
   */
  private void addChildProperties(
      CodeSystem codeSystem, String code, CsLookupResult csLookupResult) {
    FhirTermGraphRepresentation graph =
        fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem);

    if (graph == null) {
      return;
    }

    for (String child : graph.getAllConceptsWithDirectParent(code)) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  ConceptProperties.PROPERTY_NAME_CHILD, new CodeType(child)));
    }
  }

  /**
   * Populates the given lookup result with concept properties based on the requested property set.
   *
   * <p>If no specific properties are requested, the set is expanded to include all implicitly
   * defined properties ({@code url}, {@code inactive}, {@code parent}, {@code child}) as well as
   * all explicit properties declared on the {@link CodeSystem}. This reflects the server's default
   * behaviour, which the FHIR specification leaves to the server's discretion.
   *
   * <p>The following properties are handled explicitly:
   *
   * <ul>
   *   <li>{@code url} — the canonical URL of the CodeSystem
   *   <li>{@code definition} — the concept definition, if present
   *   <li>{@code parent} — direct parent codes, resolved from the concept graph
   *   <li>{@code child} — direct child codes, resolved from the concept graph
   * </ul>
   *
   * <p>All remaining requested properties are read directly from the concept's own property list,
   * with hierarchy properties ({@code parent}, {@code child}) excluded to avoid duplication.
   *
   * @param csLookupResult the result object to add the properties to
   * @param requestedPropertySet the set of property codes requested by the caller; may be mutated
   *     if it is empty
   * @param codeSystem the CodeSystem providing metadata and property declarations
   * @param concept the matched concept whose properties are to be collected
   */
  private void addConceptProperties(
      CsLookupResult csLookupResult,
      Set<String> requestedPropertySet,
      CodeSystem codeSystem,
      ConceptDefinitionComponent concept) {

    // Wenn keine spezifischen Properties angefragt wurden, fügen wir alle verfügbaren Properties
    // hinzu
    // Dies ist unser Default behaviour, da der FHIR-Standard hier dem Server die Freiheit lässt, zu
    // entscheiden, was zurückgegeben wird, solange es sinnvoll ist.
    if (requestedPropertySet.isEmpty()) {
      // Hinzufügen der impliziten Properties
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_URL);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_INACTIVE);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_CHILD);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_PARENT);
      requestedPropertySet.add(ConceptProperties.PROPERTY_NAME_DESIGNATION);

      // Hinzufügen der expliziten Properties
      for (PropertyComponent pc : codeSystem.getProperty()) {
        requestedPropertySet.add(pc.getCode());
      }
    }

    // set 'url' property (falls gewünscht)
    if (requestedPropertySet.contains(PROPERTY_NAME_URL)) {
      csLookupResult
          .getProperties()
          .add(new BaseTypeConceptProperty(PROPERTY_NAME_URL, new StringType(codeSystem.getUrl())));
    }

    // set 'definition' property
    if (requestedPropertySet.contains(PROPERTY_NAME_DEFINITION) && concept.hasDefinition()) {
      csLookupResult
          .getProperties()
          .add(
              new BaseTypeConceptProperty(
                  PROPERTY_NAME_DEFINITION, new StringType(concept.getDefinition())));
    }

    // set 'parent' properties
    if (requestedPropertySet.contains(PROPERTY_NAME_PARENT)) {
      addParentProperties(codeSystem, concept.getCode(), csLookupResult);
    }

    // set 'child' properties
    if (requestedPropertySet.contains(PROPERTY_NAME_CHILD)) {
      addChildProperties(codeSystem, concept.getCode(), csLookupResult);
    }

    for (CodeSystem.ConceptPropertyComponent property : concept.getProperty()) {
      log.debug("Property: {} {}", property.getCode(), property.getValue().fhirType());

      // skip if property not requested or hierarchy property (derived through graph)
      if (!requestedPropertySet.contains(property.getCode())
          || isHierarchyProperty(property.getCode())) {
        continue;
      }

      GenericConceptProperty conceptProperty = createConceptProperty(property);
      csLookupResult.getProperties().add(conceptProperty);
    }
  }

  /**
   * Returns whether the given property code represents a hierarchy property.
   *
   * <p>Hierarchy properties ({@code parent} and {@code child}) are derived from the concept graph
   * and must not be added again from the concept's own property list.
   *
   * @param propertyCode the property code to check
   * @return {@code true} if the code is {@code parent} or {@code child}
   */
  private boolean isHierarchyProperty(String propertyCode) {
    return propertyCode.equals(PROPERTY_NAME_PARENT) || propertyCode.equals(PROPERTY_NAME_CHILD);
  }

  /**
   * Converts a FHIR {@link CodeSystem.ConceptPropertyComponent} into a {@link
   * GenericConceptProperty}.
   *
   * <p>If the property value is a {@link Coding}, a {@link CodingConceptProperty} is returned with
   * all coding fields populated. For all other FHIR primitive or complex types, a {@link
   * BaseTypeConceptProperty} wrapping the raw value is returned.
   *
   * @param property the FHIR concept property to convert
   * @return the corresponding {@link GenericConceptProperty} representation
   */
  private GenericConceptProperty createConceptProperty(
      CodeSystem.ConceptPropertyComponent property) {
    Type propertyValue = property.getValue();

    if (propertyValue instanceof Coding coding) {
      return new CodingConceptProperty(
          property.getCode(),
          coding.getCode(),
          coding.getSystem(),
          coding.getVersion(),
          coding.getDisplay());

    } else {
      return new BaseTypeConceptProperty(property.getCode(), propertyValue);
    }
  }

  /**
   * Converts a {@link FhirDesignation} into a {@link ConceptDesignation} for inclusion in the
   * lookup result.
   *
   * @param designation the source designation to convert
   * @return a new {@link ConceptDesignation} with language, use, and value copied from the source
   */
  private ConceptDesignation toConceptDesignation(FhirDesignation designation) {
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
