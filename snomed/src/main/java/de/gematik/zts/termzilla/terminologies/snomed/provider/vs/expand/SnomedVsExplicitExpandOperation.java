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
package de.gematik.zts.termzilla.terminologies.snomed.provider.vs.expand;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CONCEPT;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.FILTER_PROPERTY_CONSTRAINT;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.formatExpressionProcessingDisabledMessage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedValueSetService;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.RuleValidationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.UnknownRefsetException;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignation;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignationManager;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.ValueSet;
import org.snomed.langauges.ecl.domain.refinement.Operator;
import org.snomed.languages.scg.domain.model.Expression;

// TODO: Doppelte Konzepte (z.B. bei 2 ECLs) aussortieren
// TODO: Refactoring

/**
 * Handles the expansion of SNOMED CT ValueSet include/exclude elements as part of the FHIR {@code
 * $expand} operation.
 *
 * <p>This class processes both extensional (explicitly enumerated codes) and intensional
 * (filter-based) ValueSet definitions for the SNOMED CT terminology. It supports:
 *
 * <ul>
 *   <li>Extensional expansion with explicit concept codes and SNOMED CT expressions
 *   <li>Intensional expansion with filters: {@code concept}, {@code constraint}, {@code parent},
 *       {@code child}
 *   <li>Version selection based on FHIR parameter priorities
 *   <li>Reference set membership filtering
 * </ul>
 *
 * @see VsExpandPartResult
 * @see SnomedValueSetService
 */
@Slf4j
public class SnomedVsExplicitExpandOperation {

  private final SnomedTerminologyManager snomedTerminologyManager;
  private final Snomed snomed;

  private final Map<String, SnomedReleaseVersion> fhirVersionToSnomedReleaseVersionMap;

  /**
   * Creates a new expansion operation handler.
   *
   * @param snomedValueSetService the SNOMED CT ValueSet service providing access to the terminology
   *     manager
   */
  public SnomedVsExplicitExpandOperation(SnomedValueSetService snomedValueSetService) {
    this.snomedTerminologyManager = snomedValueSetService.getSnomedTerminologyManager();
    this.snomed = snomedValueSetService.getSnomed();

    this.fhirVersionToSnomedReleaseVersionMap =
        ((SnomedCodeSystemService) snomedTerminologyManager.cs())
            .getFhirVersionToSnomedReleaseVersionMap();
  }

  /**
   * Expands a single {@code include} or {@code exclude} element from a ValueSet compose.
   *
   * <p>Determines the appropriate SNOMED CT version based on parameter priority:
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
   * <p>In restricted mode, hierarchy-based filters are rejected for ValueSets provided within the
   * request (see {@link ProcessingContext}), before any access to the concept graph.
   *
   * @param includeElement the include/exclude element to expand
   * @param parameter expansion settings ({@code includeInactiveConcepts}, {@code version} used if
   *     not specified in the include element, {@code checkVersion} validated against the include
   *     element version, {@code forceVersion} overriding all other version specifications, result
   *     {@code filter}) and the processing context of the ValueSet
   * @return the result containing expanded concepts and version selection information
   * @throws InvalidRequestException if version validation fails, the requested version is not
   *     supported, filter-less intensional expansion is attempted, or a hierarchy-based filter is
   *     used for a provided ValueSet in restricted mode
   */
  public VsExpandPartResult expandInclude(
      ValueSet.ConceptSetComponent includeElement, VsExpandIncludeParameter parameter) {

    boolean includeInactiveConcepts = parameter.includeInactiveConcepts();
    String systemVersion = parameter.version();
    String checkSystemVersion = parameter.checkVersion();
    String forceSystemVersion = parameter.forceVersion();
    String resultFilter = parameter.filter();

    VsExpandPartResult vsExpandIncludeResult = new VsExpandPartResult();

    // Basisprüfungen für das Include
    SnomedVsExpandParameterValidator.validateIncludeElement(
        includeElement, fhirVersionToSnomedReleaseVersionMap);
    failOnRestrictedFilters(includeElement, parameter.processingContext());

    // Angefragtes System - sollte eigentlich immer 'http://snomed.info/sct' sein
    String includeElementSystem = includeElement.getSystem();

    // Version des angefragten CodeSystems aus der 'include'-Definition (muss nicht zwingend
    // vorhanden sein)
    String includeElementVersion = includeElement.getVersion();

    // Prüfen der Include-Version gegen den 'check-system-version'-Parameter
    if ((includeElement.hasVersion() && checkSystemVersion != null)
        && (!includeElementVersion.equals(checkSystemVersion))) {
      throw new InvalidRequestException(
          "The version for the system "
              + includeElementSystem
              + " defined in the include does not match the version defined in the 'check-system-version' parameter: "
              + includeElement.getVersion()
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

    SnomedReleaseVersion releaseVersion;
    if (versionToUse == null) {
      releaseVersion = snomed.getDefaultSnomedReleaseVersion();
      versionToUse = releaseVersion.getFhirVersion();
      versionSelectionStrategy = VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT;
    } else {
      releaseVersion = fhirVersionToSnomedReleaseVersionMap.get(versionToUse);
      if (releaseVersion == null) {
        throw new InvalidRequestException(
            "The given SNOMED version '"
                + versionToUse
                + "' is not supported by the SNOMED Terminology Manager.");
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
          releaseVersion,
          resultFilter,
          includeInactiveConcepts,
          vsExpandIncludeResult.getExplicitConceptReferences(),
          vsExpandIncludeResult.getConceptBaseInfos());
    } else {
      // Fall 2: Intensional definiertes ValueSet
      // hier müssen wir zwei Varianten unterscheiden:
      // (1) Es gibt einen/mehrere Filter -> Nur die Konzepte, die dem Filter entsprechen, werden
      // selektiert
      // (2) Es gibt keinen Filter -> Alle Konzepte des angegebenen Systems werden selektiert

      if (includeElement.hasFilter()) {
        // Variante 1: Es gibt einen oder mehrere Filter
        expandIntensionalInclude(
            includeElement,
            releaseVersion,
            resultFilter,
            includeInactiveConcepts,
            vsExpandIncludeResult.getConceptBaseInfos());
      } else {
        // Variante 2: Es gibt keinen Filter
        // TODO: Hier müssen wir eigentlich alle Konzepte des angegebenen Systems zurückgeben.
        //  Das ist aktuell nicht gewünscht und aus Performance-Grüngen vermutlich auch ein
        // Desaster.
        //  Vielleicht müssen wir das perspektivisch nochmal überdenken, sobald die Filter-Variante
        //  implementiert ist. Aktuell werfen wir hier einfach eine Exception.

        throw new InvalidRequestException(
            "The given ValueSet definition includes all SNOMED concepts. This is currently not supported by the SNOMED ValueSet Expansion.");
      }
    }

    return vsExpandIncludeResult;
  }

  /**
   * Rejects hierarchy-based filters of ValueSets provided within the request if SNOMED CT is
   * provided in restricted mode. ValueSets that are part of the service's content are not affected.
   *
   * @param includeElement the include/exclude element whose filters are checked
   * @param processingContext the processing context describing the origin of the ValueSet
   * @throws InvalidRequestException if a hierarchy-based filter is not available
   */
  private void failOnRestrictedFilters(
      ValueSet.ConceptSetComponent includeElement, ProcessingContext processingContext) {
    if (snomedTerminologyManager.isRestrictedMode() && !processingContext.isStored()) {
      SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(includeElement);
    }
  }

  /**
   * Expands an extensionally defined include element containing explicitly enumerated concept
   * codes.
   *
   * <p>Iterates over all concept references in the include element and attempts to resolve each as
   * either a SNOMED CT concept or a SNOMED CT Compositional Grammar expression. Expressions are
   * validated against the MRCM rules.
   *
   * @param includeElement the include element containing explicit concept references
   * @param releaseVersion the SNOMED CT release version to use for concept retrieval
   * @param includeInactiveConcepts whether to include inactive concepts in the result
   * @param explicitConceptReferenceMap map to store concept references for later designation
   *     resolution
   * @param conceptBaseInfoSortedSet sorted set to collect the expanded concept information
   * @throws InvalidRequestException if a referenced concept is unknown, missing a code, or an
   *     expression fails validation
   */
  void expandExtensionalInclude(
      ValueSet.ConceptSetComponent includeElement,
      SnomedReleaseVersion releaseVersion,
      String resultFilter,
      boolean includeInactiveConcepts,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      SortedSet<ConceptBaseInfo> conceptBaseInfoSortedSet) {

    String system = includeElement.getSystem();
    ExtensionalProcessingContext context =
        new ExtensionalProcessingContext(
            system,
            releaseVersion,
            resultFilter,
            includeInactiveConcepts,
            explicitConceptReferenceMap,
            conceptBaseInfoSortedSet);

    for (ValueSet.ConceptReferenceComponent conceptReferenceComponent :
        includeElement.getConcept()) {
      processExtensionalConceptReference(includeElement, conceptReferenceComponent, context);
    }
  }

  /**
   * Processes one explicit concept reference from an extensional include definition.
   *
   * @param includeElement the parent include element
   * @param conceptReferenceComponent the explicit concept reference to resolve
   * @param context extensional expansion context shared across loop iterations
   */
  private void processExtensionalConceptReference(
      ValueSet.ConceptSetComponent includeElement,
      ValueSet.ConceptReferenceComponent conceptReferenceComponent,
      ExtensionalProcessingContext context) {

    if (!conceptReferenceComponent.hasUserData(USERDATA_LANGUAGE)) {
      conceptReferenceComponent.setUserData(
          USERDATA_LANGUAGE, includeElement.getUserData(USERDATA_LANGUAGE));
    }

    if (!conceptReferenceComponent.hasCode()) {
      throw new InvalidRequestException("Missing code in ValueSet include / exclude");
    }

    String code = conceptReferenceComponent.getCode();

    try {
      SnomedConcept concept = snomed.getSnomedConcept(code, context.releaseVersion(), true);
      addResolvedConceptIfMatching(conceptReferenceComponent, concept, context);
    } catch (UnknownConceptException e) {

      if (!snomedTerminologyManager.isExpressionProcessingEnabled()) {
        throw new InvalidRequestException(
            formatExpressionProcessingDisabledMessage(
                code, snomedTerminologyManager.isRestrictedMode()),
            e);
      }

      addExpressionIfMatching(conceptReferenceComponent, code, context, e);
    }
  }

  /**
   * Adds a resolved SNOMED concept to the result when it matches the result filter.
   *
   * @param conceptReferenceComponent explicit concept reference from the ValueSet
   * @param concept resolved SNOMED concept
   * @param context extensional expansion context shared across loop iterations
   */
  private void addResolvedConceptIfMatching(
      ValueSet.ConceptReferenceComponent conceptReferenceComponent,
      SnomedConcept concept,
      ExtensionalProcessingContext context) {

    boolean referenceMatchesFilter =
        matchesFilter(conceptReferenceComponent, context.resultFilter());
    boolean conceptMatchesFilter = matchesFilter(context.system(), concept, context.resultFilter());

    if (referenceMatchesFilter || conceptMatchesFilter) {
      ConceptBaseInfo conceptBaseInfo =
          ConceptBaseInfo.builder()
              .system(context.system())
              .version(context.releaseVersion().getFhirVersion())
              .code(concept.getId())
              .active(concept.isActive())
              .build();

      if (conceptBaseInfo.isActive() || context.includeInactiveConcepts()) {
        context.conceptBaseInfoSortedSet().add(conceptBaseInfo);
      }

      context
          .explicitConceptReferenceMap()
          .put(conceptBaseInfo.hashCode(), conceptReferenceComponent);
    }
  }

  /**
   * Tries expression fallback for an unknown explicit concept code and adds it if matching.
   *
   * @param conceptReferenceComponent explicit concept reference from the ValueSet
   * @param code explicit code value from the ValueSet
   * @param context extensional expansion context shared across loop iterations
   * @param originalException the original concept-resolution exception
   */
  private void addExpressionIfMatching(
      ValueSet.ConceptReferenceComponent conceptReferenceComponent,
      String code,
      ExtensionalProcessingContext context,
      UnknownConceptException originalException) {

    try {
      // try to parse the expression
      Expression expression = snomedTerminologyManager.getExpressionParser().parseExpression(code);
      snomedTerminologyManager
          .getScgProcessor()
          .validateAgainstMRCM(expression, context.releaseVersion());

      boolean referenceMatchesFilter =
          matchesFilter(conceptReferenceComponent, context.resultFilter());
      boolean expressionMatchesFilter =
          matchesFilter(context.system(), code, context.resultFilter());

      if (referenceMatchesFilter || expressionMatchesFilter) {
        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(context.system())
                .version(context.releaseVersion().getFhirVersion())
                .code(code)
                .active(true)
                .build();

        context.conceptBaseInfoSortedSet().add(conceptBaseInfo);
        context
            .explicitConceptReferenceMap()
            .put(conceptBaseInfo.hashCode(), conceptReferenceComponent);
      }

    } catch (UnsupportedTerminologyException ex) {
      log.warn(
          "Unsupported terminology version: {} This should normally be checked before. Please investigate the problem.",
          ex.getMessage());
      throw new InvalidRequestException(
          "The given ValueSet definition refers to SNOMED concept with an unknown/unsupported release version: "
              + originalException.getMessage());
    } catch (InactiveConceptException ex) {
      log.debug("The requested concept seems to be inactive: {}", ex.getMessage());
      throw new InvalidRequestException(
          "The SNOMED expression contains a concept that seems to be inactive: " + ex.getMessage());
    } catch (UnknownConceptException ex) {
      log.debug("The requested concept is unknown: {}", ex.getMessage());
      throw new InvalidRequestException(
          "The SNOMED expression contains a concept that is unknown: " + ex.getMessage());
    } catch (RuleValidationException ex) {
      log.debug("The requested concept cannot be validated against MRCM: {}", ex.getMessage());
      throw new InvalidRequestException(
          "The SNOMED expression cannot be validated against MRCM:" + ex.getMessage());
    } catch (SimpleExpressionConstraintException ex) {
      log.debug("The requested concept seems to have an constraint issue: {}", ex.getMessage());
      throw new InvalidRequestException(
          "The SNOMED expression seems to have an constraint issue:" + ex.getMessage());
    }
  }

  /**
   * Expands an intensionally defined include element using filters.
   *
   * <p>Processes all filters in the include element with AND semantics (concepts must match all
   * filters). Supported filter properties:
   *
   * <ul>
   *   <li>{@code concept} with operators: {@code is-a}, {@code descendent-of}, {@code in}
   *   <li>{@code constraint} with operator {@code =} for ECL expressions
   *   <li>{@code parent} with operator {@code =}
   *   <li>{@code child} with operator {@code =}
   * </ul>
   *
   * @param includeElement the include element defining the filter criteria
   * @param releaseVersion the SNOMED CT release version to use for concept retrieval
   * @param includeInactiveConcepts whether to include inactive concepts in the result
   * @param finalResultSet sorted set to collect the expanded concept information
   * @throws InvalidRequestException if an unsupported filter property or operator is used
   */
  void expandIntensionalInclude(
      ValueSet.ConceptSetComponent includeElement,
      SnomedReleaseVersion releaseVersion,
      String resultFilter,
      boolean includeInactiveConcepts,
      SortedSet<ConceptBaseInfo> finalResultSet) {

    // Angefragtes System - sollte eigentlich immer 'http://snomed.info/sct' sein
    String system = includeElement.getSystem();

    // temporäres Set, in das wir die Konzepte einfügen, die dem Filter entsprechen
    Set<ConceptBaseInfo> includeResultSet = null;

    // Iterieren über alle im Include-Element angegebenen Filter. Hierbei ist zu berücksichtigen,
    // dass Konzepte alle Filter erfüllen müssen
    for (ValueSet.ConceptSetFilterComponent filter : includeElement.getFilter()) {

      // hier packen wir die Ergebnisse hinein, die dem aktuellen Filter entsprechen
      Set<ConceptBaseInfo> filterResultSet = new HashSet<>();

      String property = filter.getProperty();

      switch (property) {
        case PROPERTY_NAME_CONCEPT ->
            // Verarbeiten von "concept"-Filtern
            processConceptFilter(
                filter, system, releaseVersion, includeInactiveConcepts, filterResultSet);
        case FILTER_PROPERTY_CONSTRAINT ->
            // Verarbeitung von ECL - hier sind wir mit der aktuellen Version nur in der Lage einen
            // sehr eingeschränkten Umfang der ECL zu unterstützen (<, <<, >, >>). Vermutlich reicht
            // das aber.
            processConstraintFilter(
                filter, system, releaseVersion, includeInactiveConcepts, filterResultSet);
        case "expressions" ->
            // TODO
            throw new InvalidRequestException(
                "The given ValueSet definition includes a filter with an unsupported property (expressions). This is currently not supported by the SNOMED ValueSet Expansion.");
        case PROPERTY_NAME_PARENT ->
            processParentFilter(
                filter, system, releaseVersion, includeInactiveConcepts, filterResultSet);
        case PROPERTY_NAME_CHILD ->
            processChildFilter(
                filter, system, releaseVersion, includeInactiveConcepts, filterResultSet);
        default ->
            // TODO
            throw new InvalidRequestException(
                "The given ValueSet definition includes a filter with an unsupported property. This is currently not supported by the SNOMED ValueSet Expansion.");
      }

      // Zusammenführen der Ergebnisse aus den einzelnen Filtern
      if (includeResultSet == null) {
        // Das ist der erste Filter, den wir verarbeiten. Wir übernehmen einfach die Ergebnisse
        includeResultSet = filterResultSet;
      } else {
        // Wir müssen die Ergebnisse aus dem aktuellen Filter mit denen aus den vorherigen
        // Filtern zusammenführen
        includeResultSet.retainAll(filterResultSet);
      }
    }

    finalResultSet.addAll(
        includeResultSet == null
            ? new HashSet<>()
            : applyResultFilter(includeResultSet, system, releaseVersion, resultFilter));
  }

  // TODO check if this can be done differently / more efficiently.
  private Set<ConceptBaseInfo> applyResultFilter(
      Set<ConceptBaseInfo> conceptSet,
      String system,
      SnomedReleaseVersion releaseVersion,
      String resultFilter) {

    if (resultFilter == null || resultFilter.isBlank()) {
      return conceptSet;
    }

    Set<ConceptBaseInfo> filteredConceptSet = new HashSet<>();

    for (ConceptBaseInfo conceptBaseInfo : conceptSet) {
      if (matchesFilter(system, conceptBaseInfo.getCode(), resultFilter)) {
        filteredConceptSet.add(conceptBaseInfo);
        continue;
      }

      try {
        SnomedConcept concept =
            snomed.getSnomedConceptWithDescription(conceptBaseInfo.getCode(), releaseVersion, true);
        if (matchesFilter(system, concept, resultFilter)) {
          filteredConceptSet.add(conceptBaseInfo);
        }
      } catch (UnknownConceptException _) {
        log.debug(
            "Unable to resolve SNOMED concept '{}' for result filtering.",
            conceptBaseInfo.getCode());
      }
    }

    return filteredConceptSet;
  }

  /**
   * Processes a {@code concept} filter to select concepts based on hierarchy or reference set
   * membership.
   *
   * @param filter the filter component containing property, operator, and value
   * @param includeElementSystem the system URI for the result concepts
   * @param releaseVersion the SNOMED CT release version to use
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet set to collect matching concepts
   * @throws InvalidRequestException if the filter uses an unsupported operator or references an
   *     unknown concept/refset
   */
  private void processConceptFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet) {

    String value = filter.getValue();
    String op = filter.getOp().toCode();

    switch (op) {
      case "is-a" ->
          processConceptFilterIsA(
              value,
              includeElementSystem,
              releaseVersion,
              includeInactiveConcepts,
              filterResultSet);
      case "descendent-of" ->
          processConceptFilterDescendentOf(
              value, includeElementSystem, releaseVersion, filterResultSet);
      case "in" ->
          processConceptFilterIn(value, includeElementSystem, releaseVersion, filterResultSet);
      default ->
          throw new InvalidRequestException(
              "The given ValueSet definition includes a filter with an unsupported property/operation combination.");
    }
  }

  /** Resolves {@code concept is-a <code>} using descendants-or-self semantics. */
  private void processConceptFilterIsA(
      String value,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet) {

    // We need to verify whether the requested concept exists and is active.
    // Only then can we resolve active hierarchy relations.
    try {
      if (snomed.isConceptActive(value, releaseVersion)) {
        // Case 1: requested concept exists and is active. Resolve descendants-or-self.
        for (String concept :
            snomed.getSnomedGraph().getAllDescendantsOrSelf(value, releaseVersion)) {
          ConceptBaseInfo conceptBaseInfo =
              ConceptBaseInfo.builder()
                  .system(includeElementSystem)
                  .version(releaseVersion.getFhirVersion())
                  .code(concept)
                  .active(true)
                  .build();
          filterResultSet.add(conceptBaseInfo);
        }
      } else if (includeInactiveConcepts) {
        // Case 2: requested concept exists but is inactive.
        // In SNOMED CT it has no active relations, so we only return the concept itself.
        filterResultSet.add(
            ConceptBaseInfo.builder()
                .system(includeElementSystem)
                .version(releaseVersion.getFhirVersion())
                .code(value)
                .active(false)
                .build());
      }
    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a SNOMED concept in the filter definition that is not known to the SNOMED terminology service: "
              + e.getMessage());
    }
  }

  /** Resolves {@code concept descendent-of <code>} using descendants-only semantics. */
  private void processConceptFilterDescendentOf(
      String value,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      Set<ConceptBaseInfo> filterResultSet) {

    // We need to verify whether the requested concept exists and is active.
    // Only then can we resolve active hierarchy relations.
    try {
      if (snomed.isConceptActive(value, releaseVersion)) {
        // Case 1: requested concept exists and is active. Resolve descendants only.
        for (String concept : snomed.getSnomedGraph().getAllDescendants(value, releaseVersion)) {
          ConceptBaseInfo conceptBaseInfo =
              ConceptBaseInfo.builder()
                  .system(includeElementSystem)
                  .version(releaseVersion.getFhirVersion())
                  .code(concept)
                  .active(true)
                  .build();
          filterResultSet.add(conceptBaseInfo);
        }
      } else {
        // Case 2: requested concept exists but is inactive.
        // It has no active descendants, so nothing is added.
        log.debug("The requested concept seems to be inactive and has no relations.");
      }
    } catch (UnknownConceptException e) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a SNOMED concept in the filter definition that is not known to the SNOMED terminology service: "
              + e.getMessage());
    }
  }

  /** Resolves {@code concept in <refset>} by loading all referenced components from the refset. */
  private void processConceptFilterIn(
      String value,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      Set<ConceptBaseInfo> filterResultSet) {

    // Select a set of concepts based on their membership of a SNOMED CT reference set
    // e.g.: concept in 31000274107 --> all concepts in the Microorganism Reference Set
    try {
      for (RefSet concept :
          snomed.getSnomedInMemoryRepresentation().getRefSetList(value, releaseVersion, false)) {

        if (concept instanceof Language) {
          throw new InvalidRequestException(
              "The given ValueSet definition includes a filter with an unsupported RefSet-Type. "
                  + "Language-type RefSets can't be resolved as SNOMED Descriptions are referenced, wich are not valid"
                  + "FHIR-Concepts.");
        }

        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(includeElementSystem)
                .version(releaseVersion.getFhirVersion())
                .code(concept.getReferencedComponentId())
                .active(true)
                .build();
        filterResultSet.add(conceptBaseInfo);
      }
    } catch (UnknownRefsetException _) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a filter with an unsupported RefSet as value for: 'concept in.'");
    }
  }

  /**
   * Processes a {@code constraint} filter using SNOMED CT Expression Constraint Language (ECL).
   *
   * <p>Only the {@code =} operator is supported for constraint filters.
   *
   * @param filter the filter component containing the ECL expression as value
   * @param includeElementSystem the system URI for the result concepts
   * @param releaseVersion the SNOMED CT release version to use
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet set to collect matching concepts
   * @throws InvalidRequestException if the operator is not {@code =} or the ECL expression is
   *     invalid
   */
  @SuppressWarnings("java:S1172") // will be used once imlpemented
  // TODO implement inactive concepts
  private void processConstraintFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet) {

    String value = filter.getValue();
    String op = filter.getOp().toCode();

    // We only support the '=' operator for constraints (cp.
    // https://hl7.org/fhir/R4/snomedct.html#4.3.1.0.8.3)
    if (!op.equals("=")) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a filter with an unsupported property/operation combination.");
    }

    try {
      // Validate Expression
      for (String concept : snomed.resolveExpressionConstraint(value, releaseVersion)) {
        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(includeElementSystem)
                .version(releaseVersion.getFhirVersion())
                .code(concept)
                .active(true)
                .build();
        filterResultSet.add(conceptBaseInfo);
      }
    } catch (SimpleExpressionConstraintException e) {
      throw new InvalidRequestException(e.getMessage());
    }
  }

  /**
   * Processes a {@code parent} filter to select direct parent concepts.
   *
   * <p>Internally converts the filter to an ECL constraint using the {@code <!} (childOf) operator.
   * Only the {@code =} operator is supported.
   *
   * @param filter the filter component containing the child concept code as value
   * @param includeElementSystem the system URI for the result concepts
   * @param releaseVersion the SNOMED CT release version to use
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet set to collect matching parent concepts
   * @throws InvalidRequestException if the operator is not {@code =} or the constraint resolution
   *     fails
   */
  @SuppressWarnings("java:S1172") // will be used once imlpemented
  // TODO implement inactive concepts
  private void processParentFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet) {

    String value = filter.getValue();
    String op = filter.getOp().toCode();

    // Bei 'parent' unterstützen wir nur den '=' Operator
    if (!op.equals("=")) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a filter with an unsupported property/operation combination.");
    }

    try {

      // Umwandeln des 'parent'-Filters in einen 'constraint'-Filter ('<!' childOf)
      value = Operator.childof.getText() + value;

      // Auflösen des Constraints
      for (String concept : snomed.resolveExpressionConstraint(value, releaseVersion)) {
        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(includeElementSystem)
                .version(releaseVersion.getFhirVersion())
                .code(concept)
                .active(true)
                .build();
        filterResultSet.add(conceptBaseInfo);
      }
    } catch (SimpleExpressionConstraintException e) {
      throw new InvalidRequestException(e.getMessage());
    }
  }

  /**
   * Processes a {@code child} filter to select direct child concepts.
   *
   * <p>Internally converts the filter to an ECL constraint using the {@code >!} (parentOf)
   * operator. Only the {@code =} operator is supported.
   *
   * @param filter the filter component containing the parent concept code as value
   * @param includeElementSystem the system URI for the result concepts
   * @param releaseVersion the SNOMED CT release version to use
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet set to collect matching child concepts
   * @throws InvalidRequestException if the operator is not {@code =} or the constraint resolution
   *     fails
   */
  @SuppressWarnings("java:S1172") // will be used once imlpemented
  // TODO implement inactive concepts
  private void processChildFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String includeElementSystem,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet) {

    String value = filter.getValue();
    String op = filter.getOp().toCode();

    // Bei 'parent' unterstützen wir nur den '=' Operator
    if (!op.equals("=")) {
      throw new InvalidRequestException(
          "The given ValueSet definition includes a filter with an unsupported property/operation combination.");
    }

    try {

      // Umwandeln des 'child'-Filters in einen 'constraint'-Filter ('>!' parentOf)
      value = Operator.parentof.getText() + value;

      // Auflösen des Constraints
      for (String concept : snomed.resolveExpressionConstraint(value, releaseVersion)) {
        ConceptBaseInfo conceptBaseInfo =
            ConceptBaseInfo.builder()
                .system(includeElementSystem)
                .version(releaseVersion.getFhirVersion())
                .code(concept)
                .active(true)
                .build();
        filterResultSet.add(conceptBaseInfo);
      }
    } catch (SimpleExpressionConstraintException e) {
      throw new InvalidRequestException(e.getMessage());
    }
  }

  /**
   * Creates an expansion contains component for a single concept.
   *
   * <p>Builds a FHIR {@link ValueSet.ValueSetExpansionContainsComponent} with system, version,
   * code, display, and optionally designations. Handles both regular SNOMED CT concepts and
   * Compositional Grammar expressions. Display values from explicit concept references take
   * precedence over values from the SNOMED CT model.
   *
   * @param conceptBaseInfo the concept to create the expansion component for
   * @param includeDesignations whether to include designations in the result
   * @param designationParamValueList filter for specific designations (empty means include all)
   * @param languageSelectionParameter the language selection parameters controlling display and
   *     designation language resolution
   * @param explicitConceptReferenceMap map of explicit concept references for designation and
   *     extension resolution
   * @return the populated expansion contains component
   * @throws InvalidRequestException if the concept or expression cannot be resolved
   */
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap) {

    // Erzeugen der ExpansionContainsComponent und Setzen der grundlegenden Informationen (System,
    // Code, Version)
    ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
        new ValueSet.ValueSetExpansionContainsComponent();

    expansionContainsComponent.setSystem(conceptBaseInfo.getSystem());
    expansionContainsComponent.setVersion(conceptBaseInfo.getVersion());
    expansionContainsComponent.setCode(conceptBaseInfo.getCode());

    // TODO: überlegen, ob wir diese Informationen überhaupt setzen wollen, wenn sie gar nicht
    // angefordert wurde
    expansionContainsComponent.setInactive(!conceptBaseInfo.isActive());

    // Bestimmen der zu nutzenden SNOMED-Version auf Grundlage der FHIR-Version, die in
    // conceptBaseInfo angegeben ist
    SnomedReleaseVersion releaseVersion =
        fhirVersionToSnomedReleaseVersionMap.get(conceptBaseInfo.getVersion());

    SnomedConcept concept = null;
    Expression expression = null;

    SnomedDesignationManager designationManager = new SnomedDesignationManager();

    // Designations, die aus den ValueSet-Definitionen abgeleitet wurden, zum DesignationManager
    // hinzufügen.
    designationManager.collectDesignationsFromConceptReferenceComponents(
        explicitConceptReferenceMap.get(conceptBaseInfo.hashCode()));

    try {
      // Abruf des Konzeptes aus dem Modell
      concept =
          snomed.getSnomedConceptWithDescription(conceptBaseInfo.getCode(), releaseVersion, true);

      // Zusammensuchen aller Designations, die wir aus SNOMED selbst ableiten können
      designationManager.collectDesignationsFromSnomedConcept(concept);

      // Setzen des Displays auf Grundlage des Konzeptes
      SnomedDesignation displayDesignation =
          designationManager.getDisplay(languageSelectionParameter);
      if (displayDesignation != null) {
        expansionContainsComponent.setDisplay(displayDesignation.getValue());
      }

    } catch (UnknownConceptException e) {
      // Auch hier müssen wir den Fall berücksichtigen, dass es sich um eine Expression handelt

      if (!snomedTerminologyManager.isExpressionProcessingEnabled()) {
        throw new InvalidRequestException(
            formatExpressionProcessingDisabledMessage(
                conceptBaseInfo.getCode(), snomedTerminologyManager.isRestrictedMode()),
            e);
      }
      try {
        // try to parse the expression
        expression =
            snomedTerminologyManager
                .getExpressionParser()
                .parseExpression(conceptBaseInfo.getCode());

        expansionContainsComponent.setCode(expression.toString());

        designationManager.collectDesignationsFromSnomedExpression(
            expression, releaseVersion, snomedTerminologyManager);

        // Setzen des Displays auf Grundlage der Expression
        SnomedDesignation displayDesignation =
            designationManager.getDisplay(languageSelectionParameter);
        if (displayDesignation != null) {
          expansionContainsComponent.setDisplay(displayDesignation.getValue());
        }

      } catch (InvalidRequestException ex) {
        throw ex;
      } catch (Exception _) {
        throw new InvalidRequestException(
            "The given ValueSet definition includes a SNOMED concept that is not known to the SNOMED terminology service: "
                + e.getMessage());
      }
    }

    // TODO: Diskussion -> Übernahme der in der ValueSet-Definition angegebenen Extensions

    // Setzen der Designations, falls gewünscht
    if (includeDesignations) {
      // Designations sollen eingebettet werden
      boolean includeContextExtension =
          snomedTerminologyManager.getProperties().isContextExtensionInDesignations();

      List<ValueSet.ConceptReferenceDesignationComponent> designationComponents =
          designationManager.selectDesignations(designationParamValueList).stream()
              .map(d -> SnomedDesignationManager.toDesignationComponent(d, includeContextExtension))
              .collect(Collectors.toCollection(ArrayList::new));

      if (!includeContextExtension) {
        deduplicateDesignationComponents(designationComponents);
      }

      for (ValueSet.ConceptReferenceDesignationComponent component : designationComponents) {
        expansionContainsComponent.addDesignation(component);
      }
    }

    return expansionContainsComponent;
  }

  /**
   * Removes duplicate designation components from the given list in-place.
   *
   * <p>When context extensions are disabled, designations from different language reference sets
   * (e.g. en-GB and en-US) may produce identical components (same language, use, and value). This
   * method uses {@link ValueSet.ConceptReferenceDesignationComponent#equalsDeep(Base)} to identify
   * duplicates and retains only the first occurrence.
   *
   * @param components the mutable list of designation components to deduplicate
   */
  static void deduplicateDesignationComponents(
      List<ValueSet.ConceptReferenceDesignationComponent> components) {

    List<ValueSet.ConceptReferenceDesignationComponent> deduplicatedComponents = new ArrayList<>();

    for (ValueSet.ConceptReferenceDesignationComponent component : components) {
      boolean alreadyPresent =
          deduplicatedComponents.stream().anyMatch(existing -> existing.equalsDeep(component));
      if (!alreadyPresent) {
        deduplicatedComponents.add(component);
      }
    }

    components.clear();
    components.addAll(deduplicatedComponents);
  }

  private static boolean matchesFilter(String system, SnomedConcept concept, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(system, filter)
        || containsFilterValue(concept.getId(), filter)
        || concept.getSnomedConceptDescriptionList().stream()
            .map(SnomedConceptDescription::getTerm)
            .anyMatch(term -> containsFilterValue(term, filter));
  }

  private static boolean matchesFilter(String system, String code, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(system, filter) || containsFilterValue(code, filter);
  }

  private static boolean matchesFilter(
      ValueSet.ConceptReferenceComponent conceptReferenceComponent, String filter) {
    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(conceptReferenceComponent.getCode(), filter)
        || containsFilterValue(conceptReferenceComponent.getDisplay(), filter)
        || conceptReferenceComponent.getDesignation().stream()
            .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
            .anyMatch(term -> containsFilterValue(term, filter));
  }

  private static boolean containsFilterValue(String candidate, String filter) {
    return candidate != null
        && candidate.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
  }

  /**
   * Carries reusable state for extensional include processing to keep helper signatures compact.
   */
  private record ExtensionalProcessingContext(
      String system,
      SnomedReleaseVersion releaseVersion,
      String resultFilter,
      boolean includeInactiveConcepts,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      SortedSet<ConceptBaseInfo> conceptBaseInfoSortedSet) {}

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

}
