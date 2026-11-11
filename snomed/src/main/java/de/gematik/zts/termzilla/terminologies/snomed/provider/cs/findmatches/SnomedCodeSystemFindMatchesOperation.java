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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.findmatches;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CODE;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DISPLAY;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_INITIALCHARACTERCASEINSENSITIVE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.api.parameters.model.Match;
import de.gematik.zts.termzilla.api.parameters.model.PropertyValuePair;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.PropertyFilter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.SearchResult;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignationManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Type;

// TODO: separaten Refactoring Task für diese Klasse aufmachen! Das ist aktuell eine PoC
// Implementierung,
//  die wir in den nächsten Wochen noch deutlich verbessern und erweitern wollen/müssen.
// TODO: Überlegen, ob wir sie erstmal für die WebUI-Variante entfernen, da wir in der ersten
//  Ausbaustufe noch nicht die Suche über alles haben

/**
 * Implements the FHIR {@code CodeSystem/$find-matches} operation for SNOMED CT code systems.
 *
 * <p>Processes property-based search requests by dispatching to dedicated handlers depending on the
 * filter type: hierarchy queries ({@code parent}, {@code child}) use the SNOMED graph model,
 * display text searches use the Lucene index, and code lookups use the in-memory model. Results are
 * aggregated and graded into exact, partial, and unmatched categories.
 */
@Slf4j
public class SnomedCodeSystemFindMatchesOperation {

  private final SnomedCodeSystemService snomedCsService;
  private final Snomed snomed;
  private final boolean restrictedMode;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new instance backed by the given {@link SnomedCodeSystemService}.
   *
   * @param snomedCodeSystemService the service providing access to SNOMED code system data, the
   *     terminology manager, and the in-memory SNOMED model
   */
  public SnomedCodeSystemFindMatchesOperation(SnomedCodeSystemService snomedCodeSystemService) {
    this.snomedCsService = snomedCodeSystemService;
    this.snomed = snomedCodeSystemService.getSnomedTerminologyManager().getSnomed();
    this.restrictedMode = snomedCodeSystemService.getSnomedTerminologyManager().isRestrictedMode();
  }

  // ----------------------------------------------------------------------------------------
  // CodeSystem/$find-matches
  // ----------------------------------------------------------------------------------------

  /**
   * Finds all concepts in a SNOMED CT code system that match the given search criteria.
   *
   * @param scope the scope of the request
   * @param param the search criteria, including system URL, version, match mode, and properties
   * @return the result containing all matched concepts with their graded match details
   * @throws InvalidRequestException if the request parameters are invalid or incomplete, or if a
   *     property filter on concept relationships is used in restricted mode
   * @throws UnsupportedTerminologyException if the requested code system or version is not
   *     supported by this terminology manager
   * @throws NotImplementedException if a requested property filter type is not yet implemented
   * @throws UnknownScopeException if the requested scope is not known
   */
  public CsFindMatchesResult findMatches(String scope, CsFindMatchesParameter param)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException {

    // Validierung der übergebenen $find-matches Parameter im Kontext des SnomedTerminologyManagers
    new SnomedCodeSystemFindMatchesParameterValidator(restrictedMode).validateParameters(param);

    // Überführen der Suchkriterien in eine für uns angenehmere Form
    String system = param.getSystemParam().getValue();
    String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
            .systemDefaultLanguage(param.getLanguageDefaultSystemLanguage())
            .build();

    // Ermitteln der SNOMED ReleaseVersion, die wir für die Suche benötigen
    SnomedReleaseVersion releaseVersion;
    if (version != null) {
      releaseVersion = snomedCsService.getFhirVersionToSnomedReleaseVersionMap().get(version);
    } else {
      releaseVersion = snomedCsService.getSnomed().getDefaultSnomedReleaseVersion();
    }

    if (releaseVersion == null) {
      throw new UnsupportedTerminologyException(
          "The requested version '"
              + version
              + "' is not supported by the SnomedTerminologyManager.");
    }

    CodeSystem cs = snomedCsService.getCodeSystem(scope, system, version);

    // Extrahieren des exact-Parameters über den wir die Art der Suche steuern
    boolean exactMatchRequested = param.getExactParam() != null && param.getExactParam().getValue();

    // Extrahieren der Property-Filter aus dem Param-Objekt
    List<PropertyFilter> propertyFilterList =
        prepareAndCheckPropertyFilter(
            param.getPropertyParamList() != null
                ? param.getPropertyParamList()
                : new ArrayList<>());

    List<GradedCoding> tempResultList = new ArrayList<>();

    // Iterieren über alle PropertyFilter und Suchen nach passenden Konzepten
    // 1. parent-child-Suchen müssen wir über das Graph-Modell abhandeln
    // 2. Suchen nach display-Texten muss über den Lucene-Index erfolgen
    // 3. Suchen nach code können performant über das InMemory-Modell erfolgen
    // TODO: Unterstützen weiterer wichtiger property-filter (z.B. inactive, sufficientlyDefined,
    // designation etc.)
    //  Das impliziert u.U. aber auch eine Erweiterung des Lucene-Indexes, um kombinierte
    // Suchanfragen zu stellen.
    //  Hierzu müssen wir aber sicherlich nochmal auf das Thema Kombinatorik und Relevanzbewertung
    // schauen, damit
    //  wir nicht zu viele irrelevante Ergebnisse zurückliefern, wenn wir mehrere Filter haben.
    for (PropertyFilter propertyFilter : propertyFilterList) {
      List<GradedCoding> partial =
          switch (propertyFilter.code()) {
            case PROPERTY_NAME_PARENT, PROPERTY_NAME_CHILD ->
                processParentChildFilter(propertyFilter, releaseVersion, cs, lsp);
            case PROPERTY_NAME_DISPLAY ->
                processDisplayFilter(propertyFilter, releaseVersion, cs, lsp);
            case PROPERTY_NAME_CODE -> processCodeFilter(propertyFilter, releaseVersion, cs, lsp);
            default ->
                throw new NotImplementedException(
                    "Processing for property filter with code '"
                        + propertyFilter.code()
                        + "' is not implemented");
          };
      tempResultList.addAll(partial);
    }

    // TODO: Sinnvolles kombinieren der Ergebnisse aus den verschiedenen Suchen

    return generateMatchesResultFromFilterAndResultList(
        cs, exactMatchRequested, propertyFilterList, tempResultList);
  }

  /**
   * Searches for a SNOMED concept matching the given code filter using the in-memory model.
   *
   * @param propertyFilter the property filter specifying the requested concept code
   * @param releaseVersion the SNOMED release version to search in
   * @param cs the code system providing system URL and version metadata
   * @param languageSelectionParameter the language selection criteria for resolving display values
   * @return a singleton list containing the matched {@link GradedCoding}, or an empty list if no
   *     concept with the given code exists in the specified release version
   */
  private List<GradedCoding> processCodeFilter(
      PropertyFilter propertyFilter,
      SnomedReleaseVersion releaseVersion,
      CodeSystem cs,
      LanguageSelectionParameter languageSelectionParameter) {
    // Zu suchende conceptId
    String requestedConceptId = propertyFilter.value().toString();

    // ConceptId-Suche über das InMemory-Modell
    try {
      SnomedConcept concept =
          snomed.getSnomedConceptWithDescription(requestedConceptId, releaseVersion, true);
      SnomedDesignationManager sdm = new SnomedDesignationManager();
      sdm.collectDesignationsFromSnomedConcept(concept);

      // Erstellen eines ScoredCoding-Objekts für das gefundene Konzept
      return List.of(
          new GradedCoding(
              new Coding()
                  .setSystem(cs.getUrl())
                  .setVersion(cs.getVersion())
                  .setCode(concept.getId())
                  .setDisplay(sdm.getDisplay(languageSelectionParameter).getValue()),
              true,
              propertyFilter));

    } catch (UnknownConceptException _) {
      // Wenn das Konzept nicht gefunden wurde, können wir damit sehr gut leben. :-)
      log.debug(
          "Concept with id {} not found in SnomedTerminologyManager for release version {}",
          requestedConceptId,
          releaseVersion.getFhirVersion());
      return List.of();
    }
  }

  /**
   * Searches for SNOMED concepts matching the given display text filter using the Lucene index.
   *
   * <p>Each candidate concept is re-evaluated for exact match quality by applying the SNOMED
   * case-significance rules defined on the individual descriptions.
   *
   * @param propertyFilter the property filter specifying the requested display text
   * @param releaseVersion the SNOMED release version to search in
   * @param cs the code system providing system URL and version metadata
   * @param languageSelectionParameter the language selection criteria for resolving display values
   * @return a list of {@link GradedCoding} instances, each flagged as exact or partial match
   * @throws InvalidRequestException if the filter value is {@code null} or shorter than 3
   *     characters
   */
  private List<GradedCoding> processDisplayFilter(
      PropertyFilter propertyFilter,
      SnomedReleaseVersion releaseVersion,
      CodeSystem cs,
      LanguageSelectionParameter languageSelectionParameter) {
    // Anzahl der Buchstaben festlegen, die mindestens anzugeben sind, damit die Suche nicht zu
    // teuer wird
    if (propertyFilter.value() == null || (propertyFilter.value().toString().length() < 3)) {
      throw new InvalidRequestException(
          "Property 'display' must be set and have a minimum length of 3 characters");
    }

    // Display-Text-Suche über den Lucene-Index
    SearchResult sr =
        snomed.getAllConceptsWithDescriptionFilter(
            propertyFilter.value().toString(), releaseVersion, true, 0, Integer.MAX_VALUE);

    List<GradedCoding> scoredCodingList = new ArrayList<>();

    for (SnomedConcept concept : sr.getResultList()) {

      // TODO: Hier habe ich ein komisches Gefühl ... unbedingt nochmal reviewen, wenn es dazu ein
      // Ticket gibt
      // Wir speichern im Index nur die tokenisierte Variante der descriptions, entsprechende
      // testen wir jetzt noch mal für alle Konzepte auf Description-Gleichheit. Das ist bei
      // SNOMED etwas aufwändiger, da jedes Konzept genau beschreibt, wie es case-significance
      // versteht. vgl. https://confluence.ihtsdotools.org/display/DOCEG/Case+Significance
      boolean isExactMatch = false;
      for (SnomedConceptDescription description : concept.getSnomedConceptDescriptionList()) {
        isExactMatch =
            matchWithCaseSignificance(
                description.getTerm(),
                propertyFilter.value().toString(),
                description.getCaseSignificanceId());
        if (isExactMatch) {
          break;
        }
      }

      SnomedDesignationManager sdm = new SnomedDesignationManager();
      sdm.collectDesignationsFromSnomedConcept(concept);

      // Erstellen eines ScoredCoding-Objekts für jedes gefundene Konzept
      scoredCodingList.add(
          new GradedCoding(
              new Coding()
                  .setSystem(cs.getUrl())
                  .setVersion(cs.getVersion())
                  .setCode(concept.getId())
                  .setDisplay(sdm.getDisplay(languageSelectionParameter).getValue()),
              isExactMatch,
              propertyFilter));
    }

    return scoredCodingList;
  }

  /**
   * Resolves SNOMED concepts related by hierarchy to the concept specified in the filter.
   *
   * <p>For a {@code parent} filter, all direct children of the target concept are returned because
   * they share the given concept as a parent. For a {@code child} filter, the direct parents are
   * returned.
   *
   * @param propertyFilter the property filter specifying the target concept and the hierarchy
   *     direction ({@code parent} or {@code child})
   * @param releaseVersion the SNOMED release version to search in
   * @param cs the code system providing system URL and version metadata
   * @param languageSelectionParameter the language selection criteria for resolving display values
   * @return a list of {@link GradedCoding} instances for all resolved hierarchy-related concepts
   */
  private List<GradedCoding> processParentChildFilter(
      PropertyFilter propertyFilter,
      SnomedReleaseVersion releaseVersion,
      CodeSystem cs,
      LanguageSelectionParameter languageSelectionParameter) {
    Set<String> conceptSet;

    try {
      if (propertyFilter.code().equals(PROPERTY_NAME_CHILD)) {
        // 'child'-Filter

        conceptSet =
            snomed
                .getSnomedGraph()
                .getDirectParents(propertyFilter.value().toString(), releaseVersion);
      } else {
        // 'parent'-Filter

        // Suchen nach allen Kindkonzepten des übergebenen Konzeptes, da diese das gesuchte
        // 'parent'-Konzept haben
        conceptSet =
            snomed
                .getSnomedGraph()
                .getDirectChildren(propertyFilter.value().toString(), releaseVersion);
      }
    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(e.getMessage(), e);
    }

    // Laden der Konzepte aus dem InMemory-Modell
    List<GradedCoding> results = new ArrayList<>();
    for (String conceptId : conceptSet) {
      try {
        // Laden des Konzeptes und seiner descriptions aus dem InMemory-Modell
        SnomedConcept sc = snomed.getSnomedConceptWithDescription(conceptId, releaseVersion, true);
        SnomedDesignationManager sdm = new SnomedDesignationManager();
        sdm.collectDesignationsFromSnomedConcept(sc);

        // Erstellen eines GradedCoding-Objekts für das gefundene Konzept
        results.add(
            new GradedCoding(
                new Coding()
                    .setSystem(cs.getUrl())
                    .setVersion(cs.getVersion())
                    .setCode(sc.getId())
                    .setDisplay(sdm.getDisplay(languageSelectionParameter).getValue()),
                true,
                propertyFilter));
      } catch (UnknownConceptException _) {
        log.error(
            "Concept with id {} not found in SnomedTerminologyManager for release version {}: "
                + "We didn't expect this because we got this concept id from the SNOMED Graph",
            conceptId,
            releaseVersion.getFhirVersion());
      }
    }
    return results;
  }

  /**
   * Generates a {@link CsFindMatchesResult} from the preliminary result list and the given property
   * filters.
   *
   * <p>Consolidates duplicate concept matches across multiple property filters into a single {@link
   * Match} structure per concept, assigns each property to the exact, partial, or unmatched
   * category, and removes irrelevant entries before returning the final result.
   *
   * @param cs the code system in which the search was performed
   * @param isExactMatchSearch whether an exact match was requested
   * @param propertyFilterList the list of property filters extracted from the request
   * @param resultList the preliminary list of graded candidate concepts
   * @return a {@link CsFindMatchesResult} containing the consolidated and filtered matches
   */
  private CsFindMatchesResult generateMatchesResultFromFilterAndResultList(
      CodeSystem cs,
      boolean isExactMatchSearch,
      List<PropertyFilter> propertyFilterList,
      List<GradedCoding> resultList) {

    // Unter Umständen haben wir in den verschiedenen PropertyFiltern die gleichen Konzepte die
    // matchen, entsprechend bauen wir uns eine saubere Struktur auf und verhindern doppelte Matches
    Map<String, Match> matchMap = new HashMap<>();

    // Ergebnis-Objekt initialisieren
    CsFindMatchesResult result = new CsFindMatchesResult();

    // Frühzeitiges Abbrechen, falls keine Ergebnisse gefunden wurden
    if (resultList.isEmpty()) {
      log.debug("No matches found for the given filters in CodeSystem {}", cs.getUrl());
      return result;
    }

    // Iterieren über vorläufige Ergebnisliste
    for (GradedCoding gradedCoding : resultList) {

      Coding coding = gradedCoding.getCoding();
      // Ergänzen des codings um die System- und Version-Informationen (falls noch nicht vorhanden)
      coding.setSystem(cs.getUrl());
      coding.setVersion(cs.getVersion());

      boolean isExactMatch = gradedCoding.isExactMatch();

      // Ermitteln des Match-Objekts für das aktuelle Coding
      // Erzeugen eines Match-Objekts, falls es noch nicht existiert
      if (!matchMap.containsKey(coding.getCode())) {
        matchMap.put(coding.getCode(), new Match(coding));
      }
      Match match = matchMap.get(coding.getCode());

      // Ermitteln, welche Properties gematcht haben und welche nicht. Hierbei müssen wir
      // unterscheiden, welcher Arbeitsmodus (exakt vs nicht-exakt) gewählt wurde
      if (isExactMatchSearch) {

        applyPropertyFilterToMatchesForExactSearch(
            propertyFilterList, gradedCoding, isExactMatch, match);
      } else {

        applyPropertyFilterToMatchesForInexactSearch(
            propertyFilterList, gradedCoding, isExactMatch, match);
      }

      // Bereinigen des Match-Objekts
      cleanUpMatch(match);
    }

    // Filtern der Match-Objekte: wir benötigen mindestens ein Exact- oder Partial-Match, um
    // überhaupt einen relevanten Match zu haben. Entsprechend filtern wir die
    // matchMap und entfernen alle Einträge, die keine relevanten Matches haben
    List<Match> filteredMatchList =
        matchMap.values().stream()
            .filter(m -> !m.getExactMatchList().isEmpty() || !m.getPartialMatchList().isEmpty())
            .toList();

    // Sofern es sich um eine exakte Suche handelt, müssen wir sicherstellen, dass mindestens ein
    // Eintrag in der Liste ein exaktes Match ist. Andernfalls ist die Suche nicht erfolgreich und
    // wir geben eine leere Liste zurück.
    if (isExactMatchSearch) {
      boolean atLeastOneExactMatch =
          filteredMatchList.stream().anyMatch(m -> !m.getExactMatchList().isEmpty());

      if (!atLeastOneExactMatch) {
        log.debug("No exact matches found for the given filters in CodeSystem {}", cs.getUrl());
        return result; // Leeres Ergebnis zurückgeben
      }
    }

    // Hinzufügen der Match-Objekte zur Ergebnisliste, die mindestens ein Exact- oder Partial-Match
    // haben
    result.getMatchList().addAll(filteredMatchList);

    // TODO: Sortieren der Liste nach Relevanz der Treffer - Hierfür benötigen wir einen eigenen
    //  Ansatz, der berücksichtigt, welche Art von Match wir haben. Vermutlich sind matches vom Typ
    //  Code deutlich wichtiger als andere Matches.

    return result;
  }

  /**
   * Assigns each property filter to the appropriate match category of the given {@link Match} for
   * inexact search mode.
   *
   * <p>A filter is placed in the exact match list if it triggered a confirmed exact match, in the
   * partial match list if it triggered a non-exact match for the given coding, and in the unmatched
   * list otherwise.
   *
   * @param propertyFilterList the list of all property filters from the request
   * @param gradedCoding the graded coding holding the filter that produced this result
   * @param isExactMatch whether the coding was classified as an exact match
   * @param match the match object to populate
   */
  private static void applyPropertyFilterToMatchesForInexactSearch(
      List<PropertyFilter> propertyFilterList,
      GradedCoding gradedCoding,
      boolean isExactMatch,
      Match match) {
    // Wir iterieren über die einzelnen PropertyFilter und ordnen diese korrekt dem jeweiligen
    // Match zu.
    for (PropertyFilter propertyFilter : propertyFilterList) {

      if (isExactMatch && propertyFilter == gradedCoding.getMatchingPropertyFilter()) {
        match
            .getExactMatchList()
            .add(new PropertyValuePair(propertyFilter.code(), propertyFilter.value()));
      } else if (propertyFilter == gradedCoding.getMatchingPropertyFilter()) {
        match
            .getPartialMatchList()
            .add(new PropertyValuePair(propertyFilter.code(), propertyFilter.value()));
      } else {
        match
            .getUnmatchedList()
            .add(new PropertyValuePair(propertyFilter.code(), propertyFilter.value()));
      }
    }
  }

  /**
   * Assigns each property filter to the appropriate match category of the given {@link Match} for
   * exact search mode.
   *
   * <p>In exact mode, partial matches are not considered. A filter is placed in the exact match
   * list only if it triggered a confirmed exact match for the given coding; all other filters go to
   * the unmatched list.
   *
   * @param propertyFilterList the list of all property filters from the request
   * @param gradedCoding the graded coding holding the filter that produced this result
   * @param isExactMatch whether the coding was classified as an exact match
   * @param match the match object to populate
   */
  private static void applyPropertyFilterToMatchesForExactSearch(
      List<PropertyFilter> propertyFilterList,
      GradedCoding gradedCoding,
      boolean isExactMatch,
      Match match) {

    // Wir iterieren über die einzelnen PropertyFilter und ordnen diese korrekt dem jeweiligen
    // Match zu. Im exact-Szenario müssen wir uns nicht um partial-Matches kümmern.
    for (PropertyFilter propertyFilter : propertyFilterList) {

      if (isExactMatch && propertyFilter == gradedCoding.getMatchingPropertyFilter()) {

        match
            .getExactMatchList()
            .add(new PropertyValuePair(propertyFilter.code(), propertyFilter.value()));
      } else {
        match
            .getUnmatchedList()
            .add(new PropertyValuePair(propertyFilter.code(), propertyFilter.value()));
      }
    }
  }

  /**
   * Removes duplicate entries from all match category lists and ensures mutual exclusivity between
   * the lists.
   *
   * <p>Entries already present in the exact match list are removed from the partial and unmatched
   * lists. Entries already present in the partial match list are removed from the unmatched list.
   *
   * @param match the {@link Match} object to clean up
   */
  private void cleanUpMatch(Match match) {

    // Entfernen von doppelten Einträgen in den Listen
    match.setExactMatchList(
        new ArrayList<>(match.getExactMatchList().stream().distinct().toList()));
    match.setPartialMatchList(
        new ArrayList<>(match.getPartialMatchList().stream().distinct().toList()));
    match.setUnmatchedList(new ArrayList<>(match.getUnmatchedList().stream().distinct().toList()));

    // Entfernen von Einträgen aus PartialMatchList und UnmatchedList, die in ExactMatchList
    // enthalten sind

    match.getPartialMatchList().removeIf(match.getExactMatchList()::contains);
    match.getUnmatchedList().removeIf(match.getExactMatchList()::contains);

    // Entfernen von Einträgen aus UnmatchedList, die in PartialMatchList enthalten sind
    match.getUnmatchedList().removeIf(match.getPartialMatchList()::contains);
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Extracts and validates all {@code property} parameters from the given FHIR parameter list.
   *
   * <p>Each property parameter is parsed into a {@link PropertyFilter} capturing the property code,
   * value, and its position in the request. The resulting list is then validated against the
   * properties supported by the code system.
   *
   * @param propertyParameterList the list of raw FHIR {@code property} parameters
   * @return an ordered list of {@link PropertyFilter} instances ready for processing
   * @throws InvalidRequestException if a required parameter is missing, empty, or malformed
   */
  private List<PropertyFilter> prepareAndCheckPropertyFilter(
      List<Parameters.ParametersParameterComponent> propertyParameterList)
      throws InvalidRequestException {

    // Laden der property-Properties aus den übergebenen Parametern
    // TODO: ggf. prüfen wir das schon vorher
    List<Parameters.ParametersParameterComponent> propertyParamList =
        propertyParameterList.stream().filter(p -> p.getName().equals("property")).toList();
    if (propertyParamList.isEmpty()) {
      throw new InvalidRequestException("Parameter 'property' is missing");
    }

    // Ermitteln der Property-Namen und der angefragten Typen
    List<PropertyFilter> propertyFilterList = new ArrayList<>();

    int position = 0;

    for (Parameters.ParametersParameterComponent propertyParam : propertyParamList) {
      if (propertyParam.getPart() == null || propertyParam.getPart().isEmpty()) {
        throw new InvalidRequestException("Parameter 'property' is empty");
      }

      List<Parameters.ParametersParameterComponent> partList = propertyParam.getPart();

      Optional<String> tempPropertyCode = extractCodeFromPartList(partList);
      Optional<Type> tempPropertyValue = extractValueFromPartList(partList);

      // Hinzufügen der gefundenen Property-Namen zur Liste
      if (tempPropertyCode.isPresent()) {
        propertyFilterList.add(
            new PropertyFilter(tempPropertyCode.get(), tempPropertyValue.orElse(null), position));
        position++;
      }
    }

    // TODO PropertyFilter-Liste auf Duplikate prüfen und diese entfernen, danach neu Nummerieren

    // Überprüfen, ob die Properties in der übergebenen Form vom CodeSystem unterstützt werden
    validateMatchPropertiesAgainstCodeSystem(propertyFilterList);

    return propertyFilterList;
  }

  /**
   * Extracts the {@code value} part from the given list of parameter parts.
   *
   * @param partList the list of parameter parts to search
   * @return an {@link Optional} containing the value if a {@code value} part is present, or empty
   *     if not found
   */
  private static Optional<Type> extractValueFromPartList(
      List<Parameters.ParametersParameterComponent> partList) {
    return partList.stream()
        .filter(p -> p.getName().equals("value"))
        .map(ParametersParameterComponent::getValue)
        .findFirst();
  }

  /**
   * Extracts the {@code code} part from the given list of parameter parts.
   *
   * @param partList the list of parameter parts to search
   * @return an {@link Optional} containing the code value if a {@code code} part of type {@link
   *     org.hl7.fhir.r4.model.CodeType} is present, or empty if not found
   */
  private static Optional<String> extractCodeFromPartList(
      List<Parameters.ParametersParameterComponent> partList) {
    return partList.stream()
        .filter(p -> p.getName().equals(PROPERTY_NAME_CODE))
        .filter(p -> p.getValue() instanceof CodeType)
        .map(p -> ((CodeType) p.getValue()).getValue())
        .findFirst();
  }

  /**
   * Validates that all requested property filters are supported by the code system and that their
   * value types are compatible with the expected property types.
   *
   * @param propertyFilterList the list of property filters to validate
   * @throws InvalidRequestException if an unsupported property is requested or a value type does
   *     not match the expected type for that property
   */
  private void validateMatchPropertiesAgainstCodeSystem(List<PropertyFilter> propertyFilterList)
      throws InvalidRequestException {

    Map<String, CodeSystem.PropertyComponent> propertiesSupportedByCodeSystem = new HashMap<>();
    // Im restricted mode stehen keine Filter auf Konzeptbeziehungen zur Verfügung
    if (!restrictedMode) {
      propertiesSupportedByCodeSystem.put(
          PROPERTY_NAME_CHILD,
          new CodeSystem.PropertyComponent()
              .setCode(PROPERTY_NAME_CHILD)
              .setType(CodeSystem.PropertyType.CODE));
      propertiesSupportedByCodeSystem.put(
          PROPERTY_NAME_PARENT,
          new CodeSystem.PropertyComponent()
              .setCode(PROPERTY_NAME_PARENT)
              .setType(CodeSystem.PropertyType.CODE));
    }
    propertiesSupportedByCodeSystem.put(
        PROPERTY_NAME_CODE,
        new CodeSystem.PropertyComponent()
            .setCode(PROPERTY_NAME_CODE)
            .setType(CodeSystem.PropertyType.CODE));
    propertiesSupportedByCodeSystem.put(
        PROPERTY_NAME_DISPLAY,
        new CodeSystem.PropertyComponent()
            .setCode(PROPERTY_NAME_DISPLAY)
            .setType(CodeSystem.PropertyType.STRING));

    for (PropertyFilter propertyFilter : propertyFilterList) {
      String requestPropertyCode = propertyFilter.code();
      Type requestPropertyValue = propertyFilter.value();

      CodeSystem.PropertyComponent codeSystemProperty =
          propertiesSupportedByCodeSystem.get(requestPropertyCode);

      // Sofern die Property nicht unterstützt wird, wird eine Exception geworfen
      if (codeSystemProperty == null) {
        throw new InvalidRequestException(
            "Property '"
                + requestPropertyCode
                + "' is not supported by the referenced CodeSystem. Please use one of the following properties: "
                + propertiesSupportedByCodeSystem.keySet());
      }

      // Sofern die Property unterstützt wird, wird überprüft, ob der Typ der Property mit dem Typ
      // des übergebenen Wertes übereinstimmt
      if (!codeSystemProperty.getType().toCode().equals(requestPropertyValue.fhirType())) {
        log.info("RQ Property: {}|{}", requestPropertyCode, requestPropertyValue.fhirType());
        log.info(
            "CS Property: {}|{}",
            codeSystemProperty.getCode(),
            codeSystemProperty.getType().toCode());
        throw new InvalidRequestException(
            "Property '"
                + requestPropertyCode
                + "' with provided value type '"
                + requestPropertyValue.fhirType()
                + "' is not supported by the referenced CodeSystem. The proper type would be: '"
                + codeSystemProperty.getType().toCode()
                + "'");
      }
    }
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

  /**
   * Compares two SNOMED description terms while respecting the SNOMED case-significance rules.
   *
   * @param term1 the first term to compare
   * @param term2 the second term to compare
   * @param caseSignificanceId the SNOMED concept ID defining how case significance is applied
   * @return {@code true} if the terms match according to the given case-significance rules, {@code
   *     false} otherwise
   * @throws IllegalArgumentException if the given {@code caseSignificanceId} is unknown
   */
  public static boolean matchWithCaseSignificance(
      String term1, String term2, String caseSignificanceId) {
    if (term1 == null || term2 == null || caseSignificanceId == null) {
      return false;
    }

    switch (caseSignificanceId) {
      case SC_CASESINSENSITIVE:
        return term1.equalsIgnoreCase(term2);

      case SC_INITIALCHARACTERCASEINSENSITIVE:
        if (term1.length() != term2.length()) {
          return false;
        }
        if (!equalsIgnoreCaseFirstChar(term1, term2)) {
          return false;
        }
        return term1.substring(1).equals(term2.substring(1));

      case SC_CASESENSITIVE:
        return term1.equals(term2);

      default:
        throw new IllegalArgumentException("Unknown caseSignificanceId: " + caseSignificanceId);
    }
  }

  /**
   * Checks whether the first characters of two strings are equal, ignoring case.
   *
   * @param a the first string
   * @param b the second string
   * @return {@code true} if both strings are non-empty and their first characters match
   *     case-insensitively, {@code false} otherwise
   */
  private static boolean equalsIgnoreCaseFirstChar(String a, String b) {
    if (a.isEmpty() || b.isEmpty()) {
      return false;
    }
    return Character.toLowerCase(a.charAt(0)) == Character.toLowerCase(b.charAt(0));
  }
}
