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
package de.gematik.zts.termzilla.terminologies.loinc.provider.vs.expand;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.LoincTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.LoincValueSetService;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincDesignationManager;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincFhirDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.helper.LoincLanguageHelper;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Handles the expansion of LOINC ValueSet include/exclude elements as part of the FHIR {@code
 * $expand} operation.
 *
 * <p>This class processes both extensional (explicitly enumerated codes) and intensional
 * (filter-based) ValueSet definitions for the LOINC terminology. It resolves version selection
 * based on FHIR parameter priorities and creates the appropriate expansion results.
 *
 * @see VsExpandPartResult
 * @see LoincValueSetService
 */
@Slf4j
public class LoincVsExplicitExpandOperation {

  private final LoincTerminologyManager loincTerminologyManager;
  private final Loinc loinc;

  private final Map<String, LoincVersion> fhirVersionToLoincVersionMap;

  /**
   * Creates a new expansion operation handler.
   *
   * @param loincValueSetService the LOINC ValueSet service providing access to the terminology
   *     manager
   */
  public LoincVsExplicitExpandOperation(LoincValueSetService loincValueSetService) {
    this.loincTerminologyManager = loincValueSetService.getLoincTerminologyManager();

    this.loinc = loincTerminologyManager.getLoinc();

    this.fhirVersionToLoincVersionMap =
        ((LoincCodeSystemService) loincTerminologyManager.cs()).getFhirVersionToLoincVersionMap();
  }

  /**
   * Expands a single {@code include} or {@code exclude} element from a ValueSet compose.
   *
   * <p>Determines the appropriate LOINC version based on parameter priority:
   *
   * <ol>
   *   <li>{@code force-system-version} - overrides all other version specifications
   *   <li>Version defined in the include element
   *   <li>{@code system-version} parameter
   *   <li>{@code check-system-version} parameter
   *   <li>System default version
   * </ol>
   *
   * <p>Delegates to either extensional or intensional expansion based on whether explicit concept
   * codes are present in the include element.
   *
   * @param includeElement the include/exclude element to expand
   * @param includeInactiveConcepts whether to include inactive concepts in the result
   * @param systemVersion version to use if not specified in the include element
   * @param checkSystemVersion version to validate against the include element version
   * @param forceSystemVersion version that overrides all other version specifications
   * @return the result containing expanded concepts and version selection information
   * @throws NotImplementedException if the expansion uses unsupported features (e.g., filters)
   * @throws InvalidRequestException if version validation fails or the requested version is not
   *     supported
   */
  public VsExpandPartResult expandInclude(
      ValueSet.ConceptSetComponent includeElement,
      boolean includeInactiveConcepts,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion,
      String resultFilter)
      throws NotImplementedException {

    VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

    // Basisprüfung für das include
    // TODO: vgl. SNOMED - validateIncludeElement(includeElement)

    // Angefragtes System - sollte eigentlich immer 'http://loinc.org' sein
    String includeElementSystem = includeElement.getSystem();

    // Version des angefragten CodeSystems aus der 'include'-Definition (muss nicht zwingend
    // vorhanden sein)
    String includeElementVersion = includeElement.getVersion();

    // Prüfen der Include-Version gegen den 'check-system-version'-Parameter
    if (includeElement.hasVersion()
        && checkSystemVersion != null
        && !includeElementVersion.equals(checkSystemVersion)) {
      throw new InvalidRequestException(
          "The version for the system "
              + includeElementSystem
              + " defined in the include does not match the version defined in the 'check-system-version' parameter: "
              + includeElementVersion
              + " != "
              + checkSystemVersion);
    }

    // Ermitteln der für die Expansion zu verwendenden Version
    String versionToUse = null;
    VsExpandPartResult.VersionSelectionStrategy versionSelectionStrategy =
        VsExpandPartResult.VersionSelectionStrategy.UNKNOWN;

    // Ermitteln, mit welcher Version wir arbeiten wollen. Hinweis: system-version,
    // check-system-version und force-system-version können nicht gleichzeitig gesetzt sein
    if (forceSystemVersion != null) {
      // PRIO 1: Wenn eine forceVersion angegeben ist, dann müssen wir diese zwingend verwenden.
      // Andere Angaben, z.B. aus der include-Definition, werden ignoriert
      versionToUse = forceSystemVersion;
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE;
    } else if (includeElement.hasVersion()) {
      // PRIO 2: Wenn eine Version im Include-Element angegeben ist, dann verwenden wir diese
      versionToUse = includeElement.getVersion();
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.DEFINED;
    } else if (systemVersion != null) {
      // PRIO 3: Wenn die Version im Include-Element nicht angegeben ist, dann verwenden wir die
      // Version aus dem 'system-version'-Parameter, sofern dieser definiert ist
      versionToUse = systemVersion;
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET;
    } else if (checkSystemVersion != null) {
      // PRIO 3: Wenn die Version im Include-Element nicht angegeben ist, dann verwenden wir die
      // Version aus dem 'check-system-version'-Parameter, sofern dieser definiert ist
      versionToUse = checkSystemVersion;
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK;
    }

    LoincVersion loincVersion;
    if (versionToUse == null) {
      loincVersion = loincTerminologyManager.getLoinc().getDefaultLoincVersion();
      versionToUse = loincVersion.getVersionString();
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT;
    } else {
      loincVersion = fhirVersionToLoincVersionMap.get(versionToUse);
      if (loincVersion == null) {
        throw new InvalidRequestException(
            "The given LOINC version '"
                + versionToUse
                + "' is not supported by the LOINC Terminology Manager.");
      }
    }

    // Übernehmen der versionSelectionStartegy in das Ergebnisobjekt
    vsExpandIncludeResult
        .getSystemVersionSelectionStrategy()
        .put(includeElementSystem + "|" + versionToUse, versionSelectionStrategy);

    // Prüfen, ob es sich um ein extensional oder intensional definiertes ValueSet handelt
    if (includeElement.hasConcept()) {
      // Fall 1: Extensional definiertes ValueSet (konkrete Codes sind einzeln angegeben)

      //  Erzeugen der Expansion für das aktuelle Include
      expandExtensionalInclude(
          includeElement,
          loincVersion,
          resultFilter,
          includeInactiveConcepts,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());
    } else {
      // Fall 2: Intensional definiertes ValueSet

      expandIntensionalInclude(
          includeElement,
          loincVersion,
          resultFilter,
          includeInactiveConcepts,
          vsExpandIncludeResult.getConceptBaseInfos());
    }

    return vsExpandIncludeResult;
  }

  /**
   * Expands an extensionally defined include element containing explicitly enumerated concept
   * codes.
   *
   * <p>Iterates over all concept references in the include element, retrieves the corresponding
   * LOINC concepts, and adds them to the result set. Invalid or unknown codes result in an
   * exception.
   *
   * @param includeElement the include element containing explicit concept references
   * @param version the LOINC version to use for concept retrieval
   * @param includeInactiveConcepts whether to include inactive concepts in the result
   * @param explicitConceptReferenceMap map to store concept references for later designation
   *     resolution
   * @param conceptBaseInfoSortedSet sorted set to collect the expanded concept information
   * @throws InvalidRequestException if a referenced concept is unknown or missing a code
   */
  void expandExtensionalInclude(
      ValueSet.ConceptSetComponent includeElement,
      LoincVersion version,
      String resultFilter,
      boolean includeInactiveConcepts,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      SortedSet<ConceptBaseInfo> conceptBaseInfoSortedSet) {

    // Requested system (should usually be 'http://loinc.org')
    String system = includeElement.getSystem();
    Set<String> filteringLanguageSet = buildFilteringLanguageSet(resultFilter);

    // Iterieren über alle im Include-Element angegebenen Konzeptreferenzen
    for (ValueSet.ConceptReferenceComponent conceptReferenceComponent :
        includeElement.getConcept()) {

      if (!conceptReferenceComponent.hasCode()) {
        throw new InvalidRequestException("Missing code in ValueSet include / exclude");
      }
      // Angefragter Code
      String code = conceptReferenceComponent.getCode();

      try {
        // Resolve concept with all configured languages when filter evaluation is requested.
        LoincConcept concept = loinc.getLoincConcept(code, version, true, filteringLanguageSet);

        boolean referenceMatchesFilter = matchesFilter(conceptReferenceComponent, resultFilter);
        boolean conceptMatchesFilter = matchesFilter(system, concept, resultFilter);
        if (!referenceMatchesFilter && !conceptMatchesFilter) {
          continue;
        }

        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(system)
                .version(version.getVersionString())
                .code(concept.getCode())
                .active(concept.isActive())
                .build();

        // Wir prüfen erst hier, ob wir das Konzept zur Ergebnismenge hinzufügen wollen, da
        // getLoincConcept() bei includeInactive sonst eine Exception werfen würde.
        if (conceptBaseInfo.isActive() || includeInactiveConcepts) {
          conceptBaseInfoSortedSet.add(conceptBaseInfo);
        }

        // Wir merken uns hier die ConceptReferenceComponent, um sie später für die feingliedrigere
        // Auflösung nutzen zu können
        explicitConceptReferenceMap.put(conceptBaseInfo.hashCode(), conceptReferenceComponent);

      } catch (UnknownConceptException e) {
        throw new InvalidRequestException(
            "The given ValueSet definition includes a LOINC concept that is not known to the LOINC terminology service: "
                + e.getMessage());
      } catch (InactiveConceptException _) {
        // Sofern wir explizit inaktive Konzepte nicht zurückgeben sollen, ignorieren wir die
        // entsprechende Exception. Das sollte in diesem Kontext jedoch nicht passieren
        log.warn("Inactive concept ignored: {} This shouldn't normally happen here", code);
      }
    }
  }

  /**
   * Expands an intensionally defined include element without explicit concept codes.
   *
   * <p>When no filter is specified, includes all concepts from the LOINC system for the given
   * version. Filter-based expansion is currently not supported.
   *
   * @param includeElement the include element defining the expansion criteria
   * @param releaseVersion the LOINC version to use for concept retrieval
   * @param includeInactiveConcepts whether to include inactive concepts in the result
   * @param finalResultSet sorted set to collect the expanded concept information
   * @throws NotImplementedException if the include element contains filters
   */
  void expandIntensionalInclude(
      ValueSet.ConceptSetComponent includeElement,
      LoincVersion releaseVersion,
      String resultFilter,
      boolean includeInactiveConcepts,
      SortedSet<ConceptBaseInfo> finalResultSet)
      throws NotImplementedException {

    // hier müssen wir zwei Varianten unterscheiden:
    // (1) Es gibt einen/mehrere Filter -> Nur die Konzepte, die dem Filter entsprechen, werden
    // selektiert
    // (2) Es gibt keinen Filter -> Alle Konzepte des angegebenen Systems werden selektiert

    if (includeElement.hasFilter()) {
      // Variante 1: Es gibt einen oder mehrere Filter
      throw new NotImplementedException(
          "The given ValueSet definition includes filters. This is currently not supported by the LOINC Terminology Manager.");
    } else {

      // Variante 2: Es gibt keinen Filter
      Set<String> filteringLanguageSet = buildFilteringLanguageSet(resultFilter);

      // TODO: prüfen, ob das mit den inactiven Konzepte so funktioniert, wie es soll
      loinc.getLoincInMemoryRepresentation().getConceptVersionList(releaseVersion).stream()
          .filter(concept -> includeInactiveConcepts || concept.getStatus().matches("ACTIVE"))
          .forEach(
              concept -> {
                if (StringUtils.isNoneBlank(resultFilter)) {
                  try {
                    LoincConcept loincConcept =
                        loinc.getLoincConcept(
                            concept.getLoincNum(), releaseVersion, true, filteringLanguageSet);
                    if (!matchesFilter(includeElement.getSystem(), loincConcept, resultFilter)) {
                      return;
                    }
                  } catch (UnknownConceptException | InactiveConceptException _) {
                    throw new InvalidRequestException(
                        "Unable to resolve LOINC concept for result filtering: "
                            + concept.getLoincNum());
                  }
                }

                ConceptBaseInfo conceptBaseInfo =
                    ConceptBaseInfo.builder()
                        .system(includeElement.getSystem())
                        .version(releaseVersion.getVersionString())
                        .code(concept.getLoincNum())
                        .active(concept.getStatus().matches("ACTIVE"))
                        .build();

                // Wir fügen das Konzept der Ergebnismenge hinzu
                finalResultSet.add(conceptBaseInfo);
              });

    }
  }

  /**
   * Creates an expansion contains component for a single concept.
   *
   * <p>Builds a FHIR {@link ValueSet.ValueSetExpansionContainsComponent} with system, version,
   * code, display, and optionally designations. Collects all designations via {@link
   * #collectDesignations(LoincConcept, Collection)} and delegates display selection to {@link
   * LoincDesignationManager#getDisplay(LanguageSelectionParameter)} and designation filtering to
   * {@link LoincDesignationManager#selectDesignations(List)}.
   *
   * @param conceptBaseInfo the concept to create the expansion component for
   * @param includeDesignations whether to include designations in the result
   * @param designationParamValueList filter for specific designations (empty means include all)
   * @param languageSelectionParameter the language selection parameters defining display resolution
   *     priority
   * @param explicitConceptReferenceMap map of explicit concept references for designation and
   *     extension resolution
   * @return the populated expansion contains component
   * @throws InvalidRequestException if the concept cannot be found
   */
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap) {

    // Build the expansion component skeleton.
    ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
        new ValueSet.ValueSetExpansionContainsComponent();

    expansionContainsComponent.setSystem(conceptBaseInfo.getSystem());
    expansionContainsComponent.setVersion(conceptBaseInfo.getVersion());
    expansionContainsComponent.setCode(conceptBaseInfo.getCode());
    expansionContainsComponent.setInactive(!conceptBaseInfo.isActive());

    try {

      // Resolve the LOINC release version.
      LoincVersion loincVersion = fhirVersionToLoincVersionMap.get(conceptBaseInfo.getVersion());

      // Load the concept with all candidate languages for display/designation selection.
      Set<String> languageSet = LoincLanguageHelper.buildLanguageSet(languageSelectionParameter);
      LoincConcept concept =
          loinc.getLoincConcept(conceptBaseInfo.getCode(), loincVersion, true, languageSet);

      // Collect designations from terminology/value set sources.
      Collection<ValueSet.ConceptReferenceComponent> explicitConceptReferences =
          explicitConceptReferenceMap.get(conceptBaseInfo.hashCode());

      LoincDesignationManager designationManager =
          collectDesignations(concept, explicitConceptReferences);

      // Resolve and set display according to language priority configuration.
      LoincFhirDesignation displayDesignation =
          designationManager.getDisplay(languageSelectionParameter);
      if (displayDesignation != null) {
        expansionContainsComponent.setDisplay(displayDesignation.getValue());
      }

      // Copy extensions from explicit concept references.
      for (ValueSet.ConceptReferenceComponent conceptReferenceComponent :
          explicitConceptReferences) {
        if (conceptReferenceComponent.hasExtension()) {
          for (Extension extension : conceptReferenceComponent.getExtension()) {
            expansionContainsComponent.addExtension(extension);
          }
        }
      }

      // Add selected designations when requested.
      if (includeDesignations) {
        for (LoincFhirDesignation designation :
            designationManager.selectDesignations(designationParamValueList)) {
          expansionContainsComponent.addDesignation(
              LoincDesignationManager.toDesignationComponent(designation));
        }
      }

    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a LOINC concept that is not known to the LOINC terminology service: "
              + e.getMessage());
    } catch (InactiveConceptException e) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a LOINC concept that is inactive, but includeInactive is set to false: "
              + e.getMessage());
    }

    return expansionContainsComponent;
  }

  /**
   * Collects all available designations for a LOINC concept into a {@link LoincDesignationManager}.
   *
   * <p>Current behavior collects designations from the LOINC concept model only.
   *
   * @param concept the LOINC concept containing designations
   * @param explicitConceptReferences the ValueSet concept references for this concept (reserved for
   *     future enrichment)
   * @return a populated {@link LoincDesignationManager} ready for display selection
   */
  LoincDesignationManager collectDesignations(
      LoincConcept concept,
      Collection<ValueSet.ConceptReferenceComponent> explicitConceptReferences) {

    String defaultDesignationType =
        LoincDesignationManager.resolveDefaultDesignationType(concept.getCode());

    LoincDesignationManager designationManager =
        new LoincDesignationManager(defaultDesignationType);

    // Collect all designations from the LOINC concept model
    designationManager.collectDesignationsFromLoincConcept(concept);

    return designationManager;
  }

  /**
   * Evaluates whether a resolved LOINC concept matches the given {@code resultFilter}.
   *
   * <p>Matching is case-insensitive and checks system URI, concept code, definition, and all
   * available designation values.
   *
   * @param system the code system URI associated with the concept
   * @param concept the resolved LOINC concept to evaluate
   * @param filter the raw filter text from the expand request
   * @return {@code true} if the concept matches or if the filter is blank, otherwise {@code false}
   */
  private static boolean matchesFilter(String system, LoincConcept concept, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(system, filter)
        || containsFilterValue(concept.getCode(), filter)
        || containsFilterValue(concept.getDefinition(), filter)
        || concept.getDesignationList().stream()
            .map(LoincDesignation::getDesignation)
            .anyMatch(designation -> containsFilterValue(designation, filter));
  }

  /**
   * Evaluates whether an explicit ValueSet concept reference matches the given {@code
   * resultFilter}.
   *
   * <p>Matching is case-insensitive and checks code, display, and explicit designation values
   * defined on the reference.
   *
   * @param conceptReferenceComponent the explicit concept reference to evaluate
   * @param filter the raw filter text from the expand request
   * @return {@code true} if the reference matches or if the filter is blank, otherwise {@code
   *     false}
   */
  private static boolean matchesFilter(
      ValueSet.ConceptReferenceComponent conceptReferenceComponent, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(conceptReferenceComponent.getCode(), filter)
        || containsFilterValue(conceptReferenceComponent.getDisplay(), filter)
        || conceptReferenceComponent.getDesignation().stream()
            .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
            .anyMatch(designation -> containsFilterValue(designation, filter));
  }

  /**
   * Performs a case-insensitive containment check for filter matching.
   *
   * @param candidate the candidate text to inspect
   * @param filter the filter text to look for
   * @return {@code true} if candidate contains filter (case-insensitive), otherwise {@code false}
   */
  private static boolean containsFilterValue(String candidate, String filter) {
    return candidate != null
        && candidate.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
  }

  /**
   * Builds the language set used for concept loading during result filtering.
   *
   * <p>When no filter is provided, an empty set is returned to avoid loading additional linguistic
   * variants. When a filter is provided, all configured LOINC languages are returned so filtering
   * can evaluate all available designations.
   *
   * @param resultFilter the raw result filter value from the expand request
   * @return an empty set when no filter is present, otherwise all configured supported languages
   */
  private Set<String> buildFilteringLanguageSet(String resultFilter) {
    if (StringUtils.isAllBlank(resultFilter)) {
      return Collections.emptySet();
    }

    return Set.copyOf(loinc.getSupportedLanguages());
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

}
