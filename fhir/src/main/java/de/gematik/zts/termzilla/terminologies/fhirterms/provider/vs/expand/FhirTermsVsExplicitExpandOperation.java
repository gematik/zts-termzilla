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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.IndexSearcher;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;

// TODO: implement more filter
// TODO: care about supplements

/**
 * Orchestrates explicit ValueSet expansion operations for generic FHIR terminology packages.
 *
 * <p>This class coordinates the expansion logic for ValueSets that reference CodeSystems loaded
 * from FHIR packages. It supports both extensional (explicit code enumeration) and intensional
 * (filter-based) ValueSet definitions, delegating the actual processing to specialized classes:
 *
 * <ul>
 *   <li>{@link InMemoryFilterProcessor} - In-memory filter processing
 *   <li>{@link VsExpandLuceneHelper} - Lucene-accelerated filter processing
 *   <li>{@link ExpandTextFilter} - Text filter matching
 *   <li>{@link ExpandResultHelper} - Result set building
 *   <li>{@link ExpandFilterValidator} - Filter validation
 * </ul>
 *
 * @see VsExpandPartResult
 */
@Slf4j
public class FhirTermsVsExplicitExpandOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;
  private final IndexSearcher luceneSearcher;
  private final InMemoryFilterProcessor inMemoryFilterProcessor;

  /**
   * Creates a new explicit expand operation handler.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to in-memory data
   */
  public FhirTermsVsExplicitExpandOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
    this.luceneSearcher = fhirTermsInMemoryRepresentation.getLuceneSearcher();
    this.inMemoryFilterProcessor = new InMemoryFilterProcessor(fhirTermsInMemoryRepresentation);
  }

  /**
   * Expands a single include or exclude element from a ValueSet compose.
   *
   * <p>Creates a new result object and populates it with expanded concepts. Version selection
   * follows the FHIR specification priority:
   *
   * <ol>
   *   <li>{@code force-system-version} - overrides all other version specifications
   *   <li>Version from include element - if explicitly specified
   *   <li>{@code system-version} - if include has no version
   *   <li>{@code check-system-version} - validates and uses if include has no version
   *   <li>System default - falls back to the CodeSystem's default version
   * </ol>
   *
   * @param includeOrExcludeElement the include/exclude element defining which concepts to expand
   * @param includeInactiveConcepts whether inactive concepts should be included in the result
   * @param systemVersion the system-version parameter value for version fallback
   * @param checkSystemVersion the check-system-version parameter for version validation
   * @param forceSystemVersion the force-system-version parameter to override all version settings
   * @param filter the text filter to apply on concept content (code, display, designations)
   * @param scope the scope in which the expansion is performed
   * @return the result containing expanded concepts and version selection information
   * @throws UnsupportedTerminologyException if the referenced CodeSystem is not supported
   * @throws UnknownScopeException if the scope is not recognized
   * @throws UnknownConceptException if a referenced concept is not found in the CodeSystem
   */
  public VsExpandPartResult processIncludeOrExcludeElement(
      ValueSet.ConceptSetComponent includeOrExcludeElement,
      boolean includeInactiveConcepts,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion,
      String filter,
      String scope)
      throws UnsupportedTerminologyException, UnknownScopeException, UnknownConceptException {

    VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();
    String includeOrExcludeElementSystem = includeOrExcludeElement.getSystem();

    // Prüfen der include/exclude-Version gegen den 'check-system-version'-Parameter
    validateCheckSystemVersion(includeOrExcludeElement, checkSystemVersion);

    // Ermitteln der für die Expansion zu verwendenden Version (PRIOs 1–3)
    VersionResolution versionResolution =
        resolveVersion(
            includeOrExcludeElement, systemVersion, checkSystemVersion, forceSystemVersion);
    String versionToUse = versionResolution.version();
    VsExpandPartResult.VersionSelectionStrategy versionSelectionStrategy =
        versionResolution.strategy();

    // Laden des passenden CodeSystems. Sollte versionToUse noch "null" sein, wird uns der Server
    // seine default-Version (meist die aktuelle) zurückliefern
    CodeSystem codeSystem =
        fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class,
            scope,
            includeOrExcludeElementSystem,
            versionToUse,
            ResourceRetrievalMode.FULL);

    if (codeSystem == null) {
      throw new InvalidRequestException(
          "The requested CodeSystem '"
              + includeOrExcludeElementSystem
              + "' with version '"
              + versionToUse
              + "' is not supported within the current scope: '"
              + scope
              + "'");
    }

    // Annahme: Supplements können nicht expanded werden -> Konzept muss noch final abgestimmt
    // werden
    if ((codeSystem.hasContent() && codeSystem.getContent() == CodeSystemContentMode.SUPPLEMENT)
        || (codeSystem.hasSupplements())) {
      throw new InvalidRequestException(
          "The referenced CodeSystem is defined as a supplement and cannot be expanded. URL: "
              + codeSystem.getUrl());
    }

    // PRIO 4: if version to use is still null, use the version of the CodeSystem we received as
    // system default
    if (versionToUse == null) {
      versionToUse = codeSystem.getVersion();
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT;
    }

    // Jetzt sind wir sicher, dass wir eine versionSelectionStrategy haben. Also lass uns diese im
    // Ergebnisobjekt setzen
    vsExpandIncludeResult
        .getSystemVersionSelectionStrategy()
        .put(includeOrExcludeElementSystem + "|" + versionToUse, versionSelectionStrategy);

    // Prüfen, ob es sich um ein extensional oder intensional definiertes 'include/exclude'-Element
    // handelt
    if (includeOrExcludeElement.hasConcept()) {
      // Fall 1: extensional definiertes Element (konkrete Codes sind einzeln angegeben)
      processExtensionalDefinition(
          includeOrExcludeElement,
          filter,
          includeInactiveConcepts,
          vsExpandIncludeResult,
          codeSystem);
    } else if (includeOrExcludeElement.hasFilter()) {
      // Fall 2: intensional definiertes ValueSet mit Filter
      processIntensionalDefinition(
          includeOrExcludeElement,
          filter,
          includeInactiveConcepts,
          vsExpandIncludeResult,
          codeSystem,
          scope);
    } else {
      // Fall 3: alle Konzepte des angegebenen Systems
      processAllConcepts(
          includeOrExcludeElement,
          filter,
          includeInactiveConcepts,
          vsExpandIncludeResult,
          codeSystem);
    }

    return vsExpandIncludeResult;
  }

  /** Holds the resolved version string and the strategy that was used to select it. */
  private record VersionResolution(
      String version, VsExpandPartResult.VersionSelectionStrategy strategy) {}

  /**
   * Partitions a list of filters into Lucene-compatible groups (EQUAL, REGEX) and remaining filters
   * that require in-memory processing.
   */
  private record PartitionedFilters(
      List<ValueSet.ConceptSetFilterComponent> luceneEqualsFilters,
      List<ValueSet.ConceptSetFilterComponent> luceneRegexFilters,
      List<ValueSet.ConceptSetFilterComponent> otherFilters) {

    boolean hasLuceneFilters() {
      return !luceneEqualsFilters.isEmpty() || !luceneRegexFilters.isEmpty();
    }

    static PartitionedFilters from(List<ValueSet.ConceptSetFilterComponent> filters) {
      List<ValueSet.ConceptSetFilterComponent> equals = new ArrayList<>();
      List<ValueSet.ConceptSetFilterComponent> regex = new ArrayList<>();
      List<ValueSet.ConceptSetFilterComponent> other = new ArrayList<>();

      for (ValueSet.ConceptSetFilterComponent filter : filters) {
        if (ExpandFilterValidator.isLuceneCompatibleFilter(filter)) {
          if (filter.getOp() == ValueSet.FilterOperator.EQUAL) {
            equals.add(filter);
          } else {
            regex.add(filter);
          }
        } else {
          other.add(filter);
        }
      }
      return new PartitionedFilters(equals, regex, other);
    }
  }

  /**
   * Validates that the version specified in the include/exclude element is consistent with the
   * {@code check-system-version} parameter.
   *
   * @param includeOrExcludeElement the include/exclude element to validate
   * @param checkSystemVersion the check-system-version parameter value
   * @throws InvalidRequestException if the versions do not match
   */
  private void validateCheckSystemVersion(
      ValueSet.ConceptSetComponent includeOrExcludeElement, String checkSystemVersion) {
    if (includeOrExcludeElement.hasVersion()
        && checkSystemVersion != null
        && !includeOrExcludeElement.getVersion().equals(checkSystemVersion)) {
      throw new InvalidRequestException(
          "The version for the system "
              + includeOrExcludeElement.getSystem()
              + " defined in the include/exclude does not match the version defined"
              + " in the 'check-system-version' parameter: "
              + includeOrExcludeElement.getVersion()
              + " != "
              + checkSystemVersion);
    }
  }

  /**
   * Resolves the CodeSystem version to use based on FHIR parameter priority (PRIOs 1–3).
   *
   * <p>Priority 4 (system default from the loaded CodeSystem) is handled by the caller after the
   * CodeSystem lookup, since it depends on the loaded resource.
   *
   * @param includeOrExcludeElement the include/exclude element potentially containing a version
   * @param systemVersion the system-version parameter value
   * @param checkSystemVersion the check-system-version parameter value
   * @param forceSystemVersion the force-system-version parameter value
   * @return the resolved version and the strategy used
   */
  private VersionResolution resolveVersion(
      ValueSet.ConceptSetComponent includeOrExcludeElement,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion) {

    // Ermitteln, mit welcher Version wir arbeiten wollen. Hinweis: system-version,
    // check-system-version und force-system-version können nicht gleichzeitig gesetzt sein
    if (forceSystemVersion != null) {
      // PRIO 1: Wenn eine forceVersion angegeben ist, dann müssen wir diese zwingend verwenden.
      // Andere Angaben, z.B. aus der include-Definition, werden ignoriert
      return new VersionResolution(
          forceSystemVersion, VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE);
    } else if (includeOrExcludeElement.hasVersion()) {
      // PRIO 2: Wenn eine Version im Include-Element angegeben ist, dann verwenden wir diese
      return new VersionResolution(
          includeOrExcludeElement.getVersion(),
          VsExpandPartResult.VersionSelectionStrategy.DEFINED);
    } else if (systemVersion != null) {
      // PRIO 3: Wenn die Version im Include-Element nicht angegeben ist, dann verwenden wir die
      // Version aus dem 'system-version'-Parameter, sofern dieser definiert ist
      return new VersionResolution(
          systemVersion, VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET);
    } else if (checkSystemVersion != null) {
      // PRIO 3: Wenn die Version im Include-Element nicht angegeben ist, dann verwenden wir die
      // Version aus dem 'check-system-version'-Parameter, sofern dieser definiert ist
      return new VersionResolution(
          checkSystemVersion, VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK);
    }
    return new VersionResolution(null, VsExpandPartResult.VersionSelectionStrategy.UNKNOWN);
  }

  /**
   * Expands all codes from a CodeSystem when no filter or explicit concepts are specified.
   *
   * @param includeOrExcludeElement the include element containing the system reference
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param vsExpandIncludeResult the result object to populate
   * @param codeSystem the CodeSystem to expand all concepts from
   */
  // Package-protected for testing
  void processAllConcepts(
      ConceptSetComponent includeOrExcludeElement,
      String resultFilter,
      boolean includeInactiveConcepts,
      VsExpandPartResult vsExpandIncludeResult,
      CodeSystem codeSystem) {

    List<CodeSystem.ConceptDefinitionComponent> allConcepts =
        CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);
    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {
      if (!ExpandTextFilter.matchesFilter(codeSystem.getUrl(), concept, resultFilter)) {
        continue;
      }

      ExpandResultHelper.extractBaseConcept(
          codeSystem.getUrl(),
          codeSystem.getVersion(),
          includeInactiveConcepts,
          vsExpandIncludeResult.getConceptBaseInfos(),
          concept);
    }
  }

  /**
   * Expands an extensionally defined include element with explicitly enumerated concepts.
   *
   * <p>For each concept reference in the include element, the concept is looked up in the
   * CodeSystem. A concept is included in the result only if either the reference or the definition
   * matches the provided text filter. The explicit concept references are stored in the result for
   * later use during detailed expansion (e.g., designation resolution).
   *
   * @param includeElement the include element containing explicit concept references
   * @param filter the text filter to apply on concept reference and definition content (code,
   *     display, designations)
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param vsExpandIncludeResult the result object to populate with expanded concepts
   * @param codeSystem the CodeSystem containing the concept definitions
   * @throws UnknownConceptException if a referenced concept is not found in the CodeSystem
   */
  // Package-protected for testing
  void processExtensionalDefinition(
      ConceptSetComponent includeElement,
      String filter,
      boolean includeInactiveConcepts,
      VsExpandPartResult vsExpandIncludeResult,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    // Iterieren über alle im Include-Element angegebenen Konzeptreferenzen
    for (ValueSet.ConceptReferenceComponent conceptReferenceComponent :
        includeElement.getConcept()) {

      // Falls kein Code gesetzt wurde, failen wir frühzeitig
      if (!conceptReferenceComponent.hasCode()) {
        throw new InvalidRequestException("Missing code in ValueSet include / exclude");
      }

      // Übernahme der language-Informationen des zugrundeliegenden ValueSets (vermittelt durch
      // include) in die einzelnen Komponenten
      if (!conceptReferenceComponent.hasUserData(USERDATA_LANGUAGE)) {
        conceptReferenceComponent.setUserData(
            USERDATA_LANGUAGE, includeElement.getUserData(USERDATA_LANGUAGE));
      }

      try {

        // Abruf des Konzeptes aus dem In-Memory-Modell
        CodeSystem.ConceptDefinitionComponent concept =
            CodeSystemHelper.findCodeInCodeSystem(codeSystem, conceptReferenceComponent.getCode());

        // Abgleich mit Werten aus der ValueSet-Definition
        boolean referenceMatchesFilter =
            ExpandTextFilter.matchesFilter(conceptReferenceComponent, filter);
        // Abgleich mit Werten aus den Konzepten des CodeSystems
        boolean definitionMatchesFilter =
            ExpandTextFilter.matchesFilter(codeSystem.getUrl(), concept, filter);

        if (!referenceMatchesFilter && !definitionMatchesFilter) {
          continue;
        }

        // determine if concept status is active
        // TODO can we only determine this when a property is defined with "code": "status", "uri":
        // "http://hl7.org/fhir/concept-properties#status",

        // Erzeugen der ConceptBasiInfo
        ConceptBaseInfo conceptBaseInfo =
            ExpandResultHelper.extractBaseConcept(
                codeSystem.getUrl(),
                codeSystem.getVersion(),
                includeInactiveConcepts,
                vsExpandIncludeResult.getConceptBaseInfos(),
                concept);

        // Wir merken uns hier die ConceptReferenceComponent (ANgaben aus VS), um sie später für die
        // feingliedrigere
        // Auflösung nutzen zu können
        vsExpandIncludeResult
            .getExplicitConceptReferences()
            .put(conceptBaseInfo.hashCode(), conceptReferenceComponent);

      } catch (UnknownConceptException e) {
        log.error(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Expands an intensionally defined include/exclude element using filters.
   *
   * <p>Processes all filters defined in the include element. Concepts must match all filters to be
   * included in the result (AND semantics between filters).
   *
   * <p>When the CodeSystem exceeds the Lucene threshold, compatible filters (EQUAL and REGEX on
   * non-hierarchy properties) are executed as a single combined Lucene query for better
   * performance. Graph-based and other filters are processed using the existing in-memory logic,
   * and results are intersected.
   *
   * @param includeElement the include/exclude element containing filter definitions
   * @param resultFilter the filter string to apply to concept properties (e.g., code, display,
   *     definition)
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param vsExpandIncludeResult the result object to populate with matching concepts
   * @param codeSystem the CodeSystem to filter concepts from
   * @param scope the scope in which the expansion is performed
   */
  void processIntensionalDefinition(
      ValueSet.ConceptSetComponent includeElement,
      String resultFilter,
      boolean includeInactiveConcepts,
      VsExpandPartResult vsExpandIncludeResult,
      CodeSystem codeSystem,
      String scope)
      throws UnknownConceptException {

    // Alle Filter vorab validieren (Property-Existenz, Operator, Regex-Syntax, Hierarchie etc.)
    ExpandFilterValidator.validateFilters(includeElement.getFilter(), codeSystem);

    VsExpandLuceneHelper luceneHelper = new VsExpandLuceneHelper(luceneSearcher, scope, codeSystem);

    // Lucene ist erst ab einer bestimmten Konzeptanzahl schneller, deshalb unterscheiden wir hier
    // die Wege
    if (luceneHelper.shouldUseLucene()) {
      processIntensionalDefinitionWithLucene(
          includeElement,
          resultFilter,
          includeInactiveConcepts,
          vsExpandIncludeResult,
          codeSystem,
          luceneHelper);
    } else {
      processIntensionalDefinitionInMemory(
          includeElement, resultFilter, includeInactiveConcepts, vsExpandIncludeResult, codeSystem);
    }
  }

  /**
   * Lucene-accelerated intensional expansion path.
   *
   * <p>Partitions filters into Lucene-compatible (EQUAL/REGEX on non-hierarchy properties) and
   * graph/other filters. Lucene filters are executed as a single combined query. Graph filters use
   * existing in-memory logic. Results are intersected.
   *
   * @param includeElement the include/exclude element containing filter definitions
   * @param resultFilter the text filter to apply on concept content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param vsExpandIncludeResult the result object to populate with matching concepts
   * @param codeSystem the CodeSystem to filter concepts from
   * @param luceneHelper the Lucene helper providing index access for this CodeSystem
   * @throws UnknownConceptException if a concept cannot be resolved during post-filtering
   */
  private void processIntensionalDefinitionWithLucene(
      ValueSet.ConceptSetComponent includeElement,
      String resultFilter,
      boolean includeInactiveConcepts,
      VsExpandPartResult vsExpandIncludeResult,
      CodeSystem codeSystem,
      VsExpandLuceneHelper luceneHelper)
      throws UnknownConceptException {

    // Aufteilen der Filter nach Verantwortlichkeiten
    PartitionedFilters partitioned = PartitionedFilters.from(includeElement.getFilter());

    // TODO: Momentan wenden wir den resultFilter in einer In-Memory-Variante an. Vermutlich ließe
    //  sich hier auch nochmal richtig Performance gewinnen, wenn wir das über Lucene machen

    // Lucene-kompatible Filter als kombinierte Query ausführen
    Set<ConceptBaseInfo> luceneResultSet = null;
    if (partitioned.hasLuceneFilters()) {
      Set<String> luceneCodes =
          luceneHelper.executeCombinedFilters(
              partitioned.luceneEqualsFilters(), partitioned.luceneRegexFilters());

      luceneResultSet = new HashSet<>();
      ExpandResultHelper.addCodesToResultSet(
          luceneCodes, luceneResultSet, codeSystem, includeInactiveConcepts, resultFilter);
    }

    // Verbleibende Filter in-memory verarbeiten
    Set<ConceptBaseInfo> otherResultSet =
        processFiltersWithIntersection(
            partitioned.otherFilters(), resultFilter, includeInactiveConcepts, codeSystem);

    // Ergebnisse schneiden und zuweisen
    vsExpandIncludeResult
        .getConceptBaseInfos()
        .addAll(intersectNullable(luceneResultSet, otherResultSet));
  }

  /**
   * In-memory intensional expansion path, used when the concept count is below the Lucene
   * threshold.
   *
   * <p>Iterates over all filters in the include element and intersects their results (AND
   * semantics).
   *
   * @param includeElement the include/exclude element containing filter definitions
   * @param resultFilter the text filter to apply on concept content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param vsExpandIncludeResult the result object to populate with matching concepts
   * @param codeSystem the CodeSystem to filter concepts from
   * @throws UnknownConceptException if a concept cannot be resolved during filtering
   */
  private void processIntensionalDefinitionInMemory(
      ValueSet.ConceptSetComponent includeElement,
      String resultFilter,
      boolean includeInactiveConcepts,
      VsExpandPartResult vsExpandIncludeResult,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    Set<ConceptBaseInfo> includeResultSet =
        processFiltersWithIntersection(
            includeElement.getFilter(), resultFilter, includeInactiveConcepts, codeSystem);

    if (includeResultSet != null) {
      vsExpandIncludeResult.getConceptBaseInfos().addAll(includeResultSet);
    }
  }

  /**
   * Processes a list of filterList using in-memory logic and intersects their results (AND
   * semantics).
   *
   * @param filterList the filterList to process
   * @param resultFilter the text filter to apply on concept content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param codeSystem the CodeSystem to filter concepts from
   * @return the intersected result set, or {@code null} if the filter list is empty
   * @throws UnknownConceptException if a concept cannot be resolved during filtering
   */
  private Set<ConceptBaseInfo> processFiltersWithIntersection(
      List<ValueSet.ConceptSetFilterComponent> filterList,
      String resultFilter,
      boolean includeInactiveConcepts,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    Set<ConceptBaseInfo> result = null;
    for (ValueSet.ConceptSetFilterComponent filter : filterList) {
      Set<ConceptBaseInfo> filterResult = new HashSet<>();
      inMemoryFilterProcessor.processFilter(
          filter, resultFilter, includeInactiveConcepts, filterResult, codeSystem);

      if (result == null) {
        result = filterResult;
      } else {
        result.retainAll(filterResult);
      }
    }
    return result;
  }

  /**
   * Intersects two nullable result sets. Returns the intersection if both are present, the non-null
   * set if only one is present, or an empty set if both are {@code null}.
   *
   * @param first the first result set (may be {@code null})
   * @param second the second result set (may be {@code null})
   * @return the intersected result, never {@code null}
   */
  private Set<ConceptBaseInfo> intersectNullable(
      Set<ConceptBaseInfo> first, Set<ConceptBaseInfo> second) {
    if (first != null && second != null) {
      first.retainAll(second);
      return first;
    }
    if (first != null) {
      return first;
    }
    if (second != null) {
      return second;
    }
    return Set.of();
  }
}
