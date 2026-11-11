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

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.lucene.SnomedIndex;
import de.gematik.zts.termzilla.terminologies.snomed.mapping.SnomedMapper;
import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.service.EclEvaluationService;
import de.gematik.zts.termzilla.terminologies.snomed.service.MrcmRuleService;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides the central API for working with SNOMED CT data.
 *
 * <p>This class initializes and coordinates access to the in-memory model, graph representation,
 * and Lucene index, and exposes operations for concept lookup, hierarchy traversal, and expression
 * constraint evaluation.
 */
@Slf4j
public class Snomed {

  @Getter
  private final List<SnomedReleaseVersion> supportedSnomedReleaseVersions = new ArrayList<>();

  @Getter private SnomedReleaseVersion defaultSnomedReleaseVersion;

  private final SnomedIndex snomedIndex;
  @Getter private final SnomedGraphRepresentation snomedGraph;
  @Getter private final SnomedInMemoryRepresentation snomedInMemoryRepresentation;

  @Getter private final MrcmRuleService mrcmRuleService;

  @Getter private final EclEvaluationService eclEvaluationService;

  @Getter private final String releaseDataHash;

  // =======================================================================

  /**
   * Creates a fully initialized SNOMED service instance from the configured release package.
   *
   * <p>The constructor extracts the release, builds all internal representations, derives
   * supported/default versions, initializes dependent services, and calculates a release hash for
   * caching purposes.
   *
   * @param properties configuration values for locating and loading SNOMED resources
   * @throws InvalidConfigurationException if the SNOMED configuration is incomplete or invalid
   */
  public Snomed(SnomedConfigProperties properties) throws InvalidConfigurationException {

    // Unzip the SNOMED CT release package.
    SnomedReleaseFileHelper.unzip(
        Paths.get(properties.getSnomedReleaseFileLocation()), properties.getSnomedUnzipLocation());

    // Initialize the in-memory representation.
    snomedInMemoryRepresentation = new SnomedInMemoryRepresentation(properties);

    // Initialize the graph representation.
    snomedGraph = new SnomedGraphRepresentation(properties);

    // Initialize the Lucene index.
    snomedIndex = new SnomedIndex(properties);

    // Remove temporary unzipped files.
    SnomedReleaseFileHelper.deleteUnzippedFiles(properties.getSnomedUnzipLocation());

    // Resolve supported versions from loaded data.
    setSupportedReleaseVersions();

    // Determine the default version (expected to be the most recent one).
    setDefaultReleaseVersion();

    // Initialize MRCM rule service.
    mrcmRuleService =
        new MrcmRuleService(snomedInMemoryRepresentation, defaultSnomedReleaseVersion);

    // Initialize ECL evaluation service.
    eclEvaluationService = new EclEvaluationService(snomedGraph);

    // Calculate a stable hash to uniquely identify the release data in cache keys.
    releaseDataHash =
        SnomedReleaseFileHelper.calculateReleaseFileHash(
            Paths.get(properties.getSnomedReleaseFileLocation()));
  }

  // =======================================================================
  // ReleaseVersion-management related methods
  // =======================================================================

  /** Populates the list of supported SNOMED CT release versions from loaded data. */
  private void setSupportedReleaseVersions() {

    try {
      this.supportedSnomedReleaseVersions.addAll(
          snomedInMemoryRepresentation.getSupportedSnomedReleaseVersions());
    } catch (UnsupportedTerminologyException e) {
      log.error("Error while preparing supported terminology versions: {}", e.getMessage());
    }
  }

  /** Sets the default SNOMED CT release version to the highest supported version. */
  private void setDefaultReleaseVersion() {
    this.defaultSnomedReleaseVersion = Collections.max(this.supportedSnomedReleaseVersions);
    log.info("Default version for SNOMED-CT set to '{}'.", this.defaultSnomedReleaseVersion);
  }

  // =======================================================================
  // Working with concepts
  // =======================================================================

  /**
   * Checks whether a concept is active in the given release version.
   *
   * @param conceptId the concept identifier to check
   * @param snomedReleaseVersion the release version used for the lookup
   * @return {@code true} if the concept is active, otherwise {@code false}
   * @throws UnknownConceptException if the concept cannot be found for the given inputs
   */
  public boolean isConceptActive(String conceptId, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException {

    SnomedConcept concept = getSnomedConcept(conceptId, snomedReleaseVersion, true);
    return concept.isActive();
  }

  /**
   * Loads a concept by {@code conceptId} for a specific release version.
   *
   * @param conceptId the concept identifier to load
   * @param snomedReleaseVersion the release version used for the lookup; defaults to the current
   *     default version when {@code null}
   * @param includeInactive controls whether inactive concepts may be returned
   * @return the loaded concept representation
   * @throws UnknownConceptException if the concept is missing, ambiguous, or inactive while {@code
   *     includeInactive} is {@code false}
   */
  public SnomedConcept getSnomedConcept(
      String conceptId, SnomedReleaseVersion snomedReleaseVersion, boolean includeInactive)
      throws UnknownConceptException {

    // Perform initial input validation.
    if (conceptId == null) {
      throw new UnknownConceptException("The provided code was 'null'.");
    }

    // Fall back to the default version when none is provided.
    if (snomedReleaseVersion == null) {
      snomedReleaseVersion = defaultSnomedReleaseVersion;
    }

    // Query the in-memory representation for matching concepts.
    List<Concept> conceptList =
        snomedInMemoryRepresentation.getConceptListByConceptIdSet(
            Set.of(conceptId), snomedReleaseVersion, includeInactive);

    // If no concept exists, treat the code as unknown.
    if (conceptList.isEmpty()) {
      throw new UnknownConceptException(
          "Concept '"
              + conceptId
              + "' not found in '"
              + SnomedConstants.TERMINOLOGY_CANONICAL_NAME
              + "' for version '"
              + snomedReleaseVersion.getFhirVersion()
              + "'");
    }

    if (conceptList.size() > 1) {
      throw new UnknownConceptException(
          "More than one concept found for code '"
              + conceptId
              + "' in '"
              + SnomedConstants.TERMINOLOGY_CANONICAL_NAME
              + "' for version '"
              + snomedReleaseVersion.getFhirVersion()
              + " That should not happen!");
    }

    // At this point exactly one concept must remain.
    Concept mostRecentConcept = conceptList.getFirst();

    SnomedConcept snomedConcept = SnomedMapper.toSnomedConcept(mostRecentConcept);

    // Reject inactive concepts when inactive results are not allowed.
    if (!includeInactive && !snomedConcept.isActive()) {
      throw new UnknownConceptException(
          "Concept '"
              + conceptId
              + "' is INACTIVE in '"
              + SnomedConstants.TERMINOLOGY_CANONICAL_NAME
              + "' for version '"
              + snomedReleaseVersion.getFhirVersion()
              + "'");
    }

    return snomedConcept;
  }

  /**
   * Loads a concept with all descriptions for a specific release version.
   *
   * @param conceptId the concept identifier to load
   * @param releaseVersion the release version used for the lookup; defaults to the current default
   *     version when {@code null}
   * @param includeInactive controls whether inactive concepts may be returned
   * @return the loaded concept including its descriptions
   * @throws UnknownConceptException if the concept cannot be found for the given inputs
   */
  public SnomedConcept getSnomedConceptWithDescription(
      String conceptId, SnomedReleaseVersion releaseVersion, boolean includeInactive)
      throws UnknownConceptException {

    // Perform initial input validation.
    if (conceptId == null) {
      throw new UnknownConceptException("The provided conceptId was 'null'.");
    }

    // Fall back to the default version when none is provided.
    if (releaseVersion == null) {
      releaseVersion = defaultSnomedReleaseVersion;
    }

    // Load the concept from the in-memory representation.
    SnomedConcept concept = getSnomedConcept(conceptId, releaseVersion, includeInactive);

    // Attach descriptions from the in-memory representation.
    concept.setSnomedConceptDescriptionList(
        getSnomedConceptDescriptions(
            conceptId,
            releaseVersion,
            false)); // Concepts are returned with active descriptions only.

    return concept;
  }

  /**
   * Loads descriptions for a concept in a specific release version.
   *
   * @param conceptId the concept identifier for which descriptions are loaded
   * @param snomedReleaseVersion the release version used for the lookup; defaults to the current
   *     default version when {@code null}
   * @param incudeInactive controls whether inactive descriptions should be included
   * @return all matching descriptions for the concept
   * @throws UnknownConceptException if no descriptions can be found for the concept
   */
  public List<SnomedConceptDescription> getSnomedConceptDescriptions(
      String conceptId, SnomedReleaseVersion snomedReleaseVersion, boolean incudeInactive)
      throws UnknownConceptException {

    // Perform initial input validation.
    if (conceptId == null) {
      throw new UnknownConceptException("The provided code was 'null'.");
    }

    // Fall back to the default version when none is provided.
    if (snomedReleaseVersion == null) {
      snomedReleaseVersion = defaultSnomedReleaseVersion;
    }

    // Query all descriptions linked to the concept.
    List<Description> descriptionList =
        snomedInMemoryRepresentation.getDescriptionListByConceptIdSet(
            Set.of(conceptId), snomedReleaseVersion, incudeInactive);

    // If no descriptions exist, treat the concept as unknown in this context.
    if (descriptionList.isEmpty()) {
      throw new UnknownConceptException(
          "Description for concept '"
              + conceptId
              + "' not found in '"
              + SnomedConstants.TERMINOLOGY_CANONICAL_NAME
              + "' for version '"
              + snomedReleaseVersion.getFhirVersion()
              + "'");
    }

    // Prepare the result list returned to callers.
    List<SnomedConceptDescription> snomedConceptDescriptions = new ArrayList<>();

    // Map RF2 descriptions and apply active/inactive filtering.
    for (Description description : descriptionList) {
      SnomedConceptDescription snomedConceptDescription =
          SnomedMapper.toSnomedConceptDescription(description);

      if (snomedConceptDescription.isActive()) {
        snomedConceptDescriptions.add(snomedConceptDescription);
      }
      if (incudeInactive && !snomedConceptDescription.isActive()) {
        snomedConceptDescriptions.add(snomedConceptDescription);
      }
    }

    // Resolve acceptability information for each description.
    for (SnomedConceptDescription description : snomedConceptDescriptions) {
      try {
        description.setLanguageList(
            getConceptDescriptionLanguage(
                description.getId(), snomedReleaseVersion, incudeInactive));
      } catch (UnknownConceptException _) {
        log.warn("No acceptability entry found in Language RefSet for {}", description.getId());
        description.setLanguageList(new ArrayList<>());
      }
    }

    return snomedConceptDescriptions;
  }

  /**
   * Gets the acceptability for a given description id. The acceptability is described in a specific
   * SNOMED CT reference set.
   *
   * @param descriptionId The id of the description for which the acceptability should be returned.
   * @param snomedReleaseVersion The version of SNOMED CT that should be used for the lookup.
   * @param includeInactive Controls whether inactive concepts should be returned or not. If set to
   *     false only active concepts will be returned. If an inactive concept is requested, an
   *     UnknownConceptException will be thrown.
   * @return returns the acceptability for a given description id
   * @throws UnknownConceptException if the description id is null or no acceptability entry exists
   */
  // Note: includeInactive is currently not passed to the query (hardcoded false in
  // getLanguageListByDescriptionIdSet). Verify whether this is intentional or a bug.
  public List<SnomedLanguage> getConceptDescriptionLanguage(
      String descriptionId, SnomedReleaseVersion snomedReleaseVersion, boolean includeInactive)
      throws UnknownConceptException {

    // Perform initial input validation.
    if (descriptionId == null) {
      throw new UnknownConceptException("The provided description id was 'null'.");
    }

    // Fall back to the default version when none is provided.
    if (snomedReleaseVersion == null) {
      snomedReleaseVersion = defaultSnomedReleaseVersion;
    }

    List<Language> languageList =
        snomedInMemoryRepresentation.getLanguageListByDescriptionIdSet(
            Set.of(descriptionId), snomedReleaseVersion, false);

    List<SnomedLanguage> resultList = SnomedMapper.toSnomedLanguageList(languageList);

    // If no language entry exists, treat the description as unknown in this context.
    if (languageList.isEmpty()) {
      throw new UnknownConceptException(
          "DescriptionAcceptability for description '"
              + descriptionId
              + "' not found in '"
              + SnomedConstants.TERMINOLOGY_CANONICAL_NAME
              + "' for version '"
              + snomedReleaseVersion.getFhirVersion()
              + "'");
    }

    return resultList;
  }

  /**
   * Searches concepts whose descriptions match the given filter term.
   *
   * <p>The operation first performs a term query against the Lucene index and then resolves the
   * resulting concept identifiers against the in-memory model for the requested release version.
   *
   * @param filter the free-text term used for description matching
   * @param releaseVersion the release version used for the lookup
   * @param includeInactive controls whether inactive concepts should be part of the result
   * @param offset zero-based index of the first result to return
   * @param count maximum number of results to return
   * @return a {@link SearchResult} containing paged matching concepts and total count
   */
  public SearchResult getAllConceptsWithDescriptionFilter(
      String filter,
      SnomedReleaseVersion releaseVersion,
      boolean includeInactive,
      long offset,
      long count) {

    // Prepare result container.
    SearchResult searchResult = new SearchResult();
    searchResult.setOffset(offset);

    // -----------------------------------------------------------------------
    // Query Lucene for matching terms from the description reference file.
    Set<String> conceptIdSet = snomedIndex.queryActiveTerms(filter, releaseVersion);
    log.debug(
        "Found {} unique concepts in lucene index for filter {}", conceptIdSet.size(), filter);

    // Resolve matching concept IDs against the in-memory model for the selected release version.
    List<Concept> conceptList =
        snomedInMemoryRepresentation
            .getConceptListByConceptIdSet(conceptIdSet, releaseVersion, includeInactive)
            .stream()
            .sorted(Comparator.comparing(Concept::getId))
            .toList();

    // Store total hit count before pagination.
    searchResult.setTotal(conceptList.size());
    log.info(
        "Found {} unique concepts after applying releaseVersion and includeInactive filter",
        conceptList.size());

    // Apply offset/count pagination.
    List<Concept> conceptListAfterApplyingOffsetAndCount =
        conceptList.stream().skip(offset).limit(count).toList();

    // Build final result objects including concept descriptions.
    List<SnomedConcept> resultList = new ArrayList<>();
    conceptListAfterApplyingOffsetAndCount.forEach(
        concept -> {
          SnomedConcept snomedConcept;
          try {
            // Load concept including descriptions from the in-memory representation.
            snomedConcept =
                this.getSnomedConceptWithDescription(
                    concept.getId(), releaseVersion, includeInactive);
            resultList.add(snomedConcept);
          } catch (UnknownConceptException e) {
            // This should not happen because concepts were already filtered via in-memory lookups.
            log.error(
                "Concept with id {} was found in the Lucene index but could not be found in the In-Memory-Model. That should not happen! Error: {}",
                concept.getId(),
                e.getMessage());
          }
        });

    // Write computed results to the result container.
    searchResult.setResultList(resultList);

    return searchResult;
  }

  // =======================================================================
  // Graph-related methods
  // =======================================================================

  /**
   * Returns all ancestor concept IDs of a concept including the concept itself.
   *
   * @param conceptId the concept identifier to resolve
   * @param releaseVersion the release version used for hierarchy traversal
   * @return ancestor concept IDs including {@code conceptId}
   * @throws UnknownConceptException if the concept does not exist in the graph for the given
   *     release version
   */
  public Set<String> getAncestorsOrSelf(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    return snomedGraph.getAllAncestorsOrSelf(conceptId, releaseVersion);
  }

  /**
   * Returns all direct child concept IDs of a concept for a specific release version.
   *
   * @param conceptId the concept identifier whose children should be resolved
   * @param releaseVersion the release version used for hierarchy traversal
   * @return direct child concept IDs
   * @throws UnknownConceptException if the concept does not exist in the graph for the given
   *     release version
   */
  public Set<String> getDirectChildren(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    return snomedGraph.getDirectChildren(conceptId, releaseVersion);
  }

  // =======================================================================
  // Expressions related methods – delegated to EclEvaluationService
  // =======================================================================

  /**
   * Evaluates whether a concept is included in the result of an ECL expression.
   *
   * @param conceptId the concept identifier to check
   * @param expression the ECL expression to evaluate
   * @param releaseVersion the release version used for expression evaluation
   * @return {@code true} if the concept is part of the expression result, otherwise {@code false}
   * @throws SimpleExpressionConstraintException if the expression cannot be parsed or evaluated
   */
  public boolean checkIfConceptIsIncludedInExpressionConstraintResult(
      String conceptId, String expression, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {
    return eclEvaluationService.checkIfConceptIsIncludedInExpressionConstraintResult(
        conceptId, expression, releaseVersion);
  }

  /**
   * Resolves an ECL expression to the set of matching concept IDs.
   *
   * @param expression the ECL expression to resolve
   * @param releaseVersion the release version used for expression evaluation
   * @return all concept IDs that satisfy the expression
   * @throws SimpleExpressionConstraintException if the expression cannot be parsed or evaluated
   */
  public Set<String> resolveExpressionConstraint(
      String expression, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {
    return eclEvaluationService.resolveExpressionConstraint(expression, releaseVersion);
  }
}
