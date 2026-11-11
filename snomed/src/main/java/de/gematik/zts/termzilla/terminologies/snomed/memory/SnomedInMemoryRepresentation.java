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
package de.gematik.zts.termzilla.terminologies.snomed.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.UnknownRefsetException;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.memory.caching.MemoryCacheKeys;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.ConceptLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.DescriptionLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.LanguageLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.MRCMAttributeDomainLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.MRCMAttributeRangeLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.MRCMDomainLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.ModuleDependencyLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.RefSetLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.ReleasePackageInformation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.RfEntry;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.ModuleDependency;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

/**
 * In-memory representation of SNOMED RF2 content used for terminology operations.
 *
 * <p>This component loads RF2 release files, stores entities in optimized collections, and provides
 * release-version-aware query operations for concepts, descriptions, language entries, reference
 * sets, and MRCM artifacts.
 *
 * <p>The class is intentionally optimized for repeated lookup/filter operations after startup: raw
 * RF2 rows are normalized into type-specific lists and lookup maps, while release-specific
 * projection logic resolves "latest valid entry up to release date" semantics on demand.
 */
@Slf4j
public class SnomedInMemoryRepresentation {

  /**
   * Estimated sizes for SNOMED CT International Edition. Slightly over-sized to avoid any resizing.
   * These values are based on the November 2025 Germany Edition and should be reviewed
   * periodically.
   */
  private static final int ESTIMATED_CONCEPTS = 800_000;

  private static final int ESTIMATED_DESCRIPTIONS = 4_000_000;
  private static final int ESTIMATED_LANGUAGES = 5_000_000;
  private static final int ESTIMATED_MODULE_DEPENDENCIES = 400;
  private static final int ESTIMATED_MRCM_DOMAINS = 200;
  private static final int ESTIMATED_MRCM_ATTRIBUTE_DOMAINS = 300;
  private static final int ESTIMATED_MRCM_ATTRIBUTE_RANGES = 400;

  @Getter private ReleasePackageInformation rpi = null;

  // Stores already computed concept lists per release version to avoid repeating expensive
  // filter/sort/reduce operations across requests.
  private final HashMap<String, List<Concept>> releaseVersionConceptListMap = new HashMap<>();

  private final List<Concept> conceptList = new ArrayList<>(ESTIMATED_CONCEPTS);
  private final MultiValuedMap<String, Concept> conceptIdToConceptMap =
      new ArrayListValuedHashMap<>();

  private final List<Description> descriptionList = new ArrayList<>(ESTIMATED_DESCRIPTIONS);
  private final MultiValuedMap<String, Description> conceptIdToDescriptionMap =
      new ArrayListValuedHashMap<>();
  private final MultiValuedMap<String, Description> descriptionIdToDescriptionMap =
      new ArrayListValuedHashMap<>();

  private final List<Language> languageList = new ArrayList<>(ESTIMATED_LANGUAGES);
  private final MultiValuedMap<String, Language> descriptionIdToLanguageMap =
      new ArrayListValuedHashMap<>();

  private final List<ModuleDependency> moduleDependencyList =
      new ArrayList<>(ESTIMATED_MODULE_DEPENDENCIES);
  private final MultiValuedMap<String, ModuleDependency> moduleIdToModuleDependencyMap =
      new ArrayListValuedHashMap<>();

  private final List<MrcmDomain> mrcmDomainList = new ArrayList<>(ESTIMATED_MRCM_DOMAINS);
  private final MultiValuedMap<String, MrcmDomain> conceptIdToMRCMDomainMap =
      new ArrayListValuedHashMap<>();

  private final List<MrcmAttributeDomain> mrcmAttributeDomainList =
      new ArrayList<>(ESTIMATED_MRCM_ATTRIBUTE_DOMAINS);
  private final MultiValuedMap<String, MrcmAttributeDomain> attributeIdToMRCMAttributeDomainMap =
      new ArrayListValuedHashMap<>();

  private final List<MrcmAttributeRange> mrcmAttributeRangeList =
      new ArrayList<>(ESTIMATED_MRCM_ATTRIBUTE_RANGES);
  private final MultiValuedMap<String, MrcmAttributeRange> attributeIdToMRCMAttributeRangeMap =
      new ArrayListValuedHashMap<>();

  private final MultiValuedMap<String, RfEntry> refSetIdToConceptIdMap =
      new ArrayListValuedHashMap<>();

  // ================================================================================

  /**
   * Package-private constructor for unit testing. Initializes all collection fields (via field
   * initializers) without loading any RF2 files from the filesystem.
   */
  SnomedInMemoryRepresentation() {
    // Intentionally empty constructor for tests:
    // all collections/maps are initialized by field declarations, so no filesystem interaction is
    // required in unit tests that populate the model programmatically.
  }

  // Package-private accessors used by tests to assert loader/index wiring without exposing
  // internals through the public API.

  MultiValuedMap<String, Concept> getConceptIdToConceptMap() {
    return conceptIdToConceptMap;
  }

  MultiValuedMap<String, Description> getConceptIdToDescriptionMap() {
    return conceptIdToDescriptionMap;
  }

  MultiValuedMap<String, Description> getDescriptionIdToDescriptionMap() {
    return descriptionIdToDescriptionMap;
  }

  MultiValuedMap<String, Language> getDescriptionIdToLanguageMap() {
    return descriptionIdToLanguageMap;
  }

  MultiValuedMap<String, RfEntry> getRefSetIdToConceptIdMap() {
    return refSetIdToConceptIdMap;
  }

  MultiValuedMap<String, ModuleDependency> getModuleIdToModuleDependencyMap() {
    return moduleIdToModuleDependencyMap;
  }

  MultiValuedMap<String, MrcmDomain> getConceptIdToMRCMDomainMap() {
    return conceptIdToMRCMDomainMap;
  }

  MultiValuedMap<String, MrcmAttributeDomain> getAttributeIdToMRCMAttributeDomainMap() {
    return attributeIdToMRCMAttributeDomainMap;
  }

  MultiValuedMap<String, MrcmAttributeRange> getAttributeIdToMRCMAttributeRangeMap() {
    return attributeIdToMRCMAttributeRangeMap;
  }

  /**
   * Creates and initializes the in-memory model from configured RF2 sources.
   *
   * @param properties SNOMED configuration including unzip and index locations
   * @throws InvalidConfigurationException if loading or parsing RF2 sources fails
   */
  public SnomedInMemoryRepresentation(SnomedConfigProperties properties)
      throws InvalidConfigurationException {

    log.info("");
    log.info(
        "######################################################################################");
    log.info("Generating In-Memory-Model");

    initializeFromRfFiles(properties.getSnomedUnzipLocation(), properties.getSnomedIndexLocation());
    log.info("In-Memory-Model initialized!");
  }

  // ================================================================================

  /**
   * Initializes in-memory data structures from unpacked release files.
   *
   * <p>The method resolves all required RF2 file paths, loads each file through the dedicated
   * loader, and wires parsed entities into list/map indexes used by terminology queries.
   */
  private void initializeFromRfFiles(Path snomedUnzipLocation, Path snomedIndexLocation)
      throws InvalidConfigurationException {

    log.info(
        "Initializing In-Memory-Model from unpacked Release Files in: {}", snomedIndexLocation);

    try {
      // Validate that the expected unpacked RF2 directory exists before resolving concrete files.
      log.info("Trying to load SNOMED-CT from {}", snomedUnzipLocation);
      Path snomedUnzippedReleaseFilesPath = snomedUnzipLocation;
      if (!Files.exists(snomedUnzippedReleaseFilesPath)) {
        throw new InvalidConfigurationException(
            "Directory with unzipped SNOMED Release File not found. Please check the configuration");
      }

      String releasePackageInformationFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation,
                  SnomedReleaseFileHelper.FILE_RELEASE_PACKAGE_INFORMATION_PATTERN)
              .toAbsolutePath()
              .toString();

      ObjectMapper objectMapper = new ObjectMapper();

      // Deserialize release metadata JSON. This metadata is later used for language/display
      // capability and package-specific release context.
      this.rpi =
          objectMapper.readValue(
              new File(releasePackageInformationFile), ReleasePackageInformation.class);

      String conceptFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN)
              .toAbsolutePath()
              .toString();
      String descriptionFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_DESCRIPTIONS_PATTERN)
              .toAbsolutePath()
              .toString();
      String languageFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_LANGUAGE_PATTERN)
              .toAbsolutePath()
              .toString();

      String moduleDependencyFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_MODULE_DEPENDENCY_PATTERN)
              .toAbsolutePath()
              .toString();

      String mrcmDomainFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_MRCM_DOMAIN_PATTERN)
              .toAbsolutePath()
              .toString();

      String mrcmAttributeDomainFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_MRCM_ATTRIBUTE_DOMAINS_PATTERN)
              .toAbsolutePath()
              .toString();

      String mrcmAttributeRangeFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_MRCM_ATTRIBUTE_RANGE_PATTERN)
              .toAbsolutePath()
              .toString();

      String refSetAssociation =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_REFSET_ASSOCIATION_PATTERN)
              .toAbsolutePath()
              .toString();

      String refSetAttributeValue =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_REFSET_ATTRIBUTEVALUE_PATTERN)
              .toAbsolutePath()
              .toString();

      String refSetSimple =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_REFSET_SIMPLE_PATTERN)
              .toAbsolutePath()
              .toString();

      // Load all RF2 artifacts into the in-memory representation. The order here is explicit and
      // mirrors dependencies used by later lookup operations.
      long start = System.currentTimeMillis();

      ConceptLoader conceptLoader = new ConceptLoader();
      conceptLoader.load(conceptFile, this);

      DescriptionLoader loader = new DescriptionLoader();
      loader.load(descriptionFile, this);

      LanguageLoader languageLoader = new LanguageLoader();
      languageLoader.load(languageFile, this);

      ModuleDependencyLoader moduleDependencyLoader = new ModuleDependencyLoader();
      moduleDependencyLoader.load(moduleDependencyFile, this);

      MRCMDomainLoader mrcmDomainLoader = new MRCMDomainLoader();
      mrcmDomainLoader.load(mrcmDomainFile, this);

      MRCMAttributeDomainLoader mrcmAttributeDomainLoader = new MRCMAttributeDomainLoader();
      mrcmAttributeDomainLoader.load(mrcmAttributeDomainFile, this);

      MRCMAttributeRangeLoader mrcmAttributeRangeLoader = new MRCMAttributeRangeLoader();
      mrcmAttributeRangeLoader.load(mrcmAttributeRangeFile, this);

      RefSetLoader refSetLoader = new RefSetLoader();
      refSetLoader.load(refSetAssociation, this);
      refSetLoader.load(refSetAttributeValue, this);
      refSetLoader.load(refSetSimple, this);

      long stop = System.currentTimeMillis();
      log.info("Duration to load SNOMED CT artifacts into In-Memory-Model: {} ms", (stop - start));

    } catch (Exception e) {
      log.error("Error while loading SNOMED CT artifacts into In-Memory-Model: {}", e.getMessage());
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  // ==============================================================================
  // Methods used by specialized loader classes to append entities and maintain lookup indexes.
  // ==============================================================================

  /**
   * Adds a concept to the concept list and concept-id index.
   *
   * @param concept concept entry to add
   */
  public void addConcept(Concept concept) {
    conceptList.add(concept);
    conceptIdToConceptMap.put(concept.getId(), concept);
  }

  /**
   * Adds a description to the description list and related id indexes.
   *
   * @param description description entry to add
   */
  public void addDescription(Description description) {
    descriptionList.add(description);
    conceptIdToDescriptionMap.put(description.getConceptId(), description);
    descriptionIdToDescriptionMap.put(description.getId(), description);
  }

  /**
   * Adds a language reference set entry and updates language/refset indexes.
   *
   * @param language language entry to add
   */
  public void addLanguage(Language language) {
    languageList.add(language);
    descriptionIdToLanguageMap.put(language.getReferencedComponentId(), language);
    refSetIdToConceptIdMap.put(language.getRefsetId(), language);
  }

  /**
   * Adds a reference set entry to the refset-id index.
   *
   * @param refSet reference set entry to add
   */
  public void addRefSet(RefSet refSet) {
    refSetIdToConceptIdMap.put(refSet.getRefsetId(), refSet);
  }

  /**
   * Adds a module dependency to the list and module-id index.
   *
   * @param moduleDependency module dependency entry to add
   */
  public void addModuleDependency(ModuleDependency moduleDependency) {
    moduleDependencyList.add(moduleDependency);
    moduleIdToModuleDependencyMap.put(moduleDependency.getModuleId(), moduleDependency);
  }

  // TODO: Validate whether indexing MRCM domains by referencedComponentId is the right lookup key
  // for all downstream query paths.
  /**
   * Adds an MRCM domain entry and indexes it by referenced component id.
   *
   * @param mrcmDomain MRCM domain entry to add
   */
  public void addMRCMDomain(MrcmDomain mrcmDomain) {
    mrcmDomainList.add(mrcmDomain);
    conceptIdToMRCMDomainMap.put(mrcmDomain.getReferencedComponentId(), mrcmDomain);
  }

  // TODO: Validate whether referencedComponentId is the correct and sufficient index key for
  // MRCM attribute domain retrieval in all callers.
  /**
   * Adds an MRCM attribute domain entry and indexes it by referenced component id.
   *
   * @param mrcmAttributeDomain MRCM attribute domain entry to add
   */
  public void addMRCMAttributeDomain(MrcmAttributeDomain mrcmAttributeDomain) {
    mrcmAttributeDomainList.add(mrcmAttributeDomain);
    attributeIdToMRCMAttributeDomainMap.put(
        mrcmAttributeDomain.getReferencedComponentId(), mrcmAttributeDomain);
  }

  // TODO: Validate whether referencedComponentId is the correct and sufficient index key for
  // MRCM attribute range retrieval in all callers.
  /**
   * Adds an MRCM attribute range entry and indexes it by referenced component id.
   *
   * @param mrcmAttributeRange MRCM attribute range entry to add
   */
  public void addMRCMAttributeRange(MrcmAttributeRange mrcmAttributeRange) {
    mrcmAttributeRangeList.add(mrcmAttributeRange);
    attributeIdToMRCMAttributeRangeMap.put(
        mrcmAttributeRange.getReferencedComponentId(), mrcmAttributeRange);
  }

  // ==============================================================================
  // Metadata Operations
  // ==============================================================================

  /**
   * Returns all module ids known from loaded module dependency entries.
   *
   * @return set of available module ids
   */
  public Set<String> getModuleIdSet() {
    return Set.copyOf(moduleIdToModuleDependencyMap.keySet());
  }

  // ==============================================================================
  // ReleaseVersion related Operations
  // ==============================================================================

  /**
   * Builds the list of supported release versions from loaded module dependency data.
   *
   * <p>Only explicitly supported modules are considered. Versions marked as invalid in {@link
   * SnomedReleaseVersionConstants#INVALID_VERSIONS} are excluded.
   *
   * @return sorted list of supported release versions
   * @throws UnsupportedTerminologyException currently part of public API contract
   */
  public List<SnomedReleaseVersion> getSupportedSnomedReleaseVersions()
      throws UnsupportedTerminologyException {

    log.debug("getSupportedSnomedReleaseVersions()");

    // Collect all release versions derived from module dependencies; sorting happens implicitly by
    // the TreeSet used per module.
    ArrayList<SnomedReleaseVersion> supportedVersions = new ArrayList<>();

    // Read module ids from the module dependency refset.
    Set<String> moduleSet = moduleIdToModuleDependencyMap.keySet();
    log.debug("Found {} modules: {}", moduleSet.size(), moduleSet);

    // Restrict processing to modules explicitly supported by this deployment profile. This avoids
    // returning incidental module ids that may be present in RF2 but not supported by API logic.
    // TODO: Re-evaluate this restriction against product requirements; relaxing it could expose
    // additional module versions that are present in RF2 but not officially supported by API
    // consumers.
    moduleSet.retainAll(SnomedReleaseVersionConstants.SUPPORTED_MODULES);

    // For each supported module, gather all effectiveTime values seen in module dependency rows.
    // Those effective times represent candidate release versions.
    for (String module : moduleSet) {
      log.debug(
          "Observing Module: {} ({})",
          module,
          Objects.requireNonNullElse(
              SnomedReleaseVersionConstants.MODULE_NAME_MAP.get(module), "UNKNOWN"));

      TreeSet<String> moduleVersionSet = new TreeSet<>();
      moduleIdToModuleDependencyMap.get(module).stream()
          .forEach(
              moduleDependency ->
                  moduleVersionSet.add(String.valueOf(moduleDependency.getEffectiveTime())));

      for (String moduleVersion : moduleVersionSet) {
        // Ignore known invalid release versions. Some RF2 packages may contain technical entries
        // that are not official published releases.
        if (SnomedReleaseVersionConstants.INVALID_VERSIONS.containsKey(module)
            && SnomedReleaseVersionConstants.INVALID_VERSIONS.get(module).contains(moduleVersion)) {
          log.debug(
              "Ignoring invalid version {} for module {} ({})",
              moduleVersion,
              module,
              SnomedReleaseVersionConstants.MODULE_NAME_MAP.get(module));
        } else {
          // Resolve dependency versions needed to fully describe the release context for this
          // module version.
          Map<String, String> dependencies = getModuleDependencies(module, moduleVersion);

          // Register the generated release version.
          supportedVersions.add(new SnomedReleaseVersion(module, moduleVersion, dependencies));
        }
      }
      log.debug("Found {} valid versions for module {}", supportedVersions.size(), module);
    }

    log.info("Supported versions: " + supportedVersions);
    return List.copyOf(supportedVersions);
  }

  /**
   * Resolves module dependencies for a specific module/version combination.
   *
   * @param module module id
   * @param moduleVersion effective time of the selected module version
   * @return map of dependent module ids to target effective times
   */
  private Map<String, String> getModuleDependencies(String module, String moduleVersion) {

    log.debug("getModuleDependencies(module={}, moduleVersion={})", module, moduleVersion);

    // Prepare result map.
    Map<String, String> dependencies = new HashMap<>();

    // Resolve dependencies for the selected module/version.
    moduleIdToModuleDependencyMap.get(module).stream()
        // Filter by the requested module version.
        .filter(
            moduleDependency ->
                moduleDependency.getEffectiveTime() == Long.parseLong(moduleVersion))
        // Register each dependency in the result map.
        .forEach(
            moduleDependency ->
                dependencies.put(
                    moduleDependency.getReferencedComponentId(),
                    String.valueOf(moduleDependency.getTargetEffectiveTime())));

    return dependencies;
  }

  // ==============================================================================
  // Concept related Operations
  // ==============================================================================

  // Guava cache for release-specific concept projections.
  // Building a projection is comparatively expensive, so repeated requests should reuse results.
  private final LoadingCache<MemoryCacheKeys.ReleaseVersionInactiveCacheKey, List<Concept>>
      releaseVersionInactiveCache =
          CacheBuilder.newBuilder()
              .maximumSize(
                  100) // Trade-off: bounded memory footprint vs. cache hit rate for versions.
              .build(
                  new CacheLoader<MemoryCacheKeys.ReleaseVersionInactiveCacheKey, List<Concept>>() {
                    @Override
                    public List<Concept> load(MemoryCacheKeys.ReleaseVersionInactiveCacheKey key)
                        throws Exception {
                      log.info(
                          "Loading releaseVersionSpecificConceptList for the first time: {} {}",
                          key.snomedReleaseVersion().getFhirVersion(),
                          key.inactive());
                      // Delegate to the underlying computation method.
                      return getConceptList(key.snomedReleaseVersion(), key.inactive());
                    }
                  });

  /**
   * Returns concepts for the given release version using a cache-backed lookup.
   *
   * @param releaseVersion target release version
   * @param inactive whether inactive concepts should be included
   * @return release-filtered concept list, or an empty list if cache loading fails
   */
  public List<Concept> getConceptListCached(SnomedReleaseVersion releaseVersion, boolean inactive) {
    try {
      return List.copyOf(
          releaseVersionInactiveCache.get(
              new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersion, inactive)));
    } catch (Exception e) {
      log.error("Error while loading cached releaseVersionSpecificConceptList", e);
      return Collections.emptyList();
    }
  }

  /**
   * Computes the concept list relevant for a release version.
   *
   * <p>The algorithm applies three core constraints: module relevance, module-specific
   * effectiveTime upper bounds, and active/inactive visibility. Because RF2 contains historical
   * versions per id, the stream is sorted by effectiveTime and then reduced to keep only the latest
   * valid state per concept id.
   *
   * @param releaseVersion target release version
   * @param inactive whether inactive concepts should be included
   * @return filtered and normalized concept list
   */
  public List<Concept> getConceptList(SnomedReleaseVersion releaseVersion, boolean inactive) {

    // Cache miss path: recompute the projection for this release version from the full concept set.
    log.info(
        "Generating and caching releaseVersion specific concept list for '{}'",
        releaseVersion.getFhirVersion());

    // Prepare result list.
    List<Concept> releaseVersionSpecificConceptList = new ArrayList<>();

    // Map of module -> effectiveTime upper bound for the selected release version. This allows
    // one query to combine base module and dependent modules with individual version cutoffs.
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    conceptList.stream()
        // Module filter: keeps only concepts that belong to module ids included in this release
        // context.
        .filter(concept -> relevantModules.containsKey(concept.getModuleId()))
        // Version cutoff filter: for each concept, compare its effectiveTime against the
        // module-specific upper bound from relevantModules.
        .filter(
            concept -> {
              String module = concept.getModuleId();
              long version = Long.parseLong(relevantModules.get(module));
              return concept.getEffectiveTime() <= version;
            })
        // Sort ascending so map collision handling can deterministically keep the latest element.
        .sorted(Comparator.comparing(Concept::getEffectiveTime))
        .collect(
            Collectors.toMap(
                Concept::getId, // Key extraction.
                obj -> obj, // Value mapping.
                (existing, replacement) ->
                    replacement // Duplicate id means newer effectiveTime; keep replacement.
                ))
        .values()
        .stream()
        // Active visibility filter controlled by caller.
        .filter(concept -> concept.isActive() || (!concept.isActive() && inactive))
        .sorted(Comparator.comparing(Concept::getId))
        .forEachOrdered(releaseVersionSpecificConceptList::add);

    releaseVersionConceptListMap.put(releaseVersion.toString(), releaseVersionSpecificConceptList);

    return List.copyOf(releaseVersionSpecificConceptList);
  }

  /**
   * Counts concepts available for a release version.
   *
   * @param releaseVersion target release version
   * @param inactive whether inactive concepts should be included
   * @return number of matching concepts
   */
  public long countAllConcepts(SnomedReleaseVersion releaseVersion, boolean inactive) {
    return getConceptListCached(releaseVersion, inactive).size();
  }

  /**
   * Returns a paged stream of concepts for a release version.
   *
   * @param releaseVersion target release version
   * @param offset zero-based start offset
   * @param count maximum number of entries to return
   * @param inactive whether inactive concepts should be included
   * @return stream view over the requested concept slice
   */
  public Stream<Concept> getAllConcepts(
      SnomedReleaseVersion releaseVersion, long offset, long count, boolean inactive) {
    return getConceptListCached(releaseVersion, inactive).stream().skip(offset).limit(count);
  }

  /**
   * Returns concepts for the given concept ids, constrained to one release version.
   *
   * @param conceptIdSet concept ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive concepts should be included
   * @return matching concepts reduced to the latest version per id
   *     <p>The method first narrows candidates through an id-based pre-filter map and then applies
   *     the same release-version normalization strategy used by {@link
   *     #getConceptList(SnomedReleaseVersion, boolean)}.
   */
  public List<Concept> getConceptListByConceptIdSet(
      Set<String> conceptIdSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    // Resolve modules relevant for the selected release version.
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    log.debug("Relevant Modules: {}", relevantModules);

    // Pre-filter by concept id using the prepared lookup map.
    List<Concept> preFilteredConceptList = new ArrayList<>();
    conceptIdSet.forEach(
        conceptId -> {
          if (conceptIdToConceptMap.containsKey(conceptId)) {
            preFilteredConceptList.addAll(conceptIdToConceptMap.get(conceptId));
          }
        });

    // Assumption: this method is mostly called with comparatively small id sets. The pre-filter
    // therefore keeps downstream processing small and predictable.

    List<Concept> filteredAndOrderedConceptList =
        preFilteredConceptList.stream()
            // Keep only concepts from modules relevant for this release context.
            .filter(concept -> relevantModules.containsKey(concept.getModuleId()))
            // Keep only concept versions visible at release time.
            .filter(
                concept -> {
                  String module = concept.getModuleId();
                  long version = Long.parseLong(relevantModules.get(module));
                  return concept.getEffectiveTime() <= version;
                })
            // Normalize historical versions by id: latest effectiveTime wins.
            .sorted(Comparator.comparing(Concept::getEffectiveTime))
            .collect(
                Collectors.toMap(
                    Concept::getId, // Key extraction.
                    obj -> obj, // Value mapping.
                    (existing, replacement) -> replacement // On collision, keep the latest entry.
                    ))
            .values()
            .stream()
            // Apply active visibility requested by caller.
            .filter(concept -> concept.isActive() || (!concept.isActive() && includeInactive))
            .toList();

    if (log.isDebugEnabled()) {
      log.debug("Concepts filtered and ordered for conceptIdSet {}: ", conceptIdSet);
      filteredAndOrderedConceptList.stream()
          .forEachOrdered(
              concept ->
                  log.debug(
                      "Concept: {} {} {}",
                      concept.getId(),
                      concept.isActive(),
                      concept.getEffectiveTime()));
    }

    return filteredAndOrderedConceptList;
  }

  // ==============================================================================

  /**
   * Returns descriptions for the given concept ids, constrained to one release version.
   *
   * @param conceptIdSet concept ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive descriptions should be included
   * @return matching descriptions reduced to the latest version per id
   *     <p>This is concept-centric description retrieval. It is typically used when concept matches
   *     are already known and corresponding description states are required for the same release
   *     context.
   */
  public List<Description> getDescriptionListByConceptIdSet(
      Set<String> conceptIdSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    // Resolve modules relevant for the selected release version.
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    // Pre-filter by concept id using the prepared concept-to-description index. This avoids a full
    // scan over all descriptions.
    List<Description> preFilteredDescriptionList = new ArrayList<>();
    conceptIdSet.stream()
        .forEach(
            concept -> {
              if (conceptIdToDescriptionMap.containsKey(concept)) {
                preFilteredDescriptionList.addAll(conceptIdToDescriptionMap.get(concept));
              }
            });

    List<Description> filteredAndOrderedDescriptionList =
        preFilteredDescriptionList.stream()
            // TODO: The concept-id containment check is currently redundant because the list is
            // already pre-filtered by conceptIdToDescriptionMap; keep commented until this query
            // path is refactored.
            // Restrict to modules in scope for the selected release version.
            .filter(description -> relevantModules.containsKey(description.getModuleId()))
            // Keep description versions visible at release time.
            .filter(
                description -> {
                  String module = description.getModuleId();
                  long version = Long.parseLong(relevantModules.get(module));
                  return description.getEffectiveTime() <= version;
                })
            // Reduce historical versions: latest entry per description id wins.
            .sorted(Comparator.comparing(Description::getEffectiveTime))
            .collect(
                Collectors.toMap(
                    Description::getId, // Key extraction.
                    obj -> obj, // Value mapping.
                    (existing, replacement) -> replacement // On collision, keep the latest entry.
                    ))
            .values()
            .stream()
            // Apply active/inactive filter.
            .filter(concept -> concept.isActive() || (!concept.isActive() && includeInactive))
            .toList();

    if (log.isDebugEnabled()) {
      log.debug("Descriptions filtered and ordered for conceptIdSet {}: ", conceptIdSet);
      filteredAndOrderedDescriptionList.stream()
          .forEachOrdered(
              description ->
                  log.debug(
                      "Description {} {} {} {}",
                      description.getId(),
                      description.isActive(),
                      description.getEffectiveTime(),
                      description.getTerm()));
    }

    return filteredAndOrderedDescriptionList;
  }

  // ==============================================================================

  /**
   * Returns descriptions for the given description ids, constrained to one release version.
   *
   * @param descriptionIdSet description ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive descriptions should be included
   * @return matching descriptions reduced to the latest version per id
   *     <p>This is description-centric retrieval and complements {@link
   *     #getDescriptionListByConceptIdSet(Set, SnomedReleaseVersion, boolean)} when description ids
   *     are already known.
   */
  public List<Description> getDescriptionListByDescriptionIdSet(
      Set<String> descriptionIdSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    // Resolve modules relevant for the selected release version.
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    // Pre-filter by description id to keep subsequent stream processing limited to relevant
    // candidates.
    List<Description> preFilteredDescriptionList = new ArrayList<>();
    descriptionIdSet.stream()
        .forEach(
            description -> {
              if (descriptionIdToDescriptionMap.containsKey(description)) {
                preFilteredDescriptionList.addAll(descriptionIdToDescriptionMap.get(description));
              }
            });

    List<Description> filteredAndOrderedDescriptionList =
        preFilteredDescriptionList.stream()
            // TODO: The concept-id containment check is currently redundant because candidates are
            // pre-filtered from descriptionIdToDescriptionMap; keep commented until query flow is
            // consolidated.
            // Keep only entries from modules relevant for this release version.
            .filter(description -> relevantModules.containsKey(description.getModuleId()))
            // Keep entries effective on or before the configured module version.
            .filter(
                description -> {
                  String module = description.getModuleId();
                  long version = Long.parseLong(relevantModules.get(module));
                  return description.getEffectiveTime() <= version;
                })
            // Sort by effective time so the later map step keeps the latest entry per id.
            .sorted(Comparator.comparing(Description::getEffectiveTime))
            .collect(
                Collectors.toMap(
                    Description::getId, // Key extraction.
                    obj -> obj, // Value mapping.
                    (existing, replacement) -> replacement // On collision, keep the latest entry.
                    ))
            .values()
            .stream()
            // Apply active/inactive filter.
            .filter(concept -> concept.isActive() || (!concept.isActive() && includeInactive))
            .toList();

    if (log.isDebugEnabled()) {
      filteredAndOrderedDescriptionList.stream()
          .forEachOrdered(
              description ->
                  log.debug(
                      "{} {} {} {}",
                      description.getId(),
                      description.isActive(),
                      description.getEffectiveTime(),
                      description.getTerm()));
    }

    return filteredAndOrderedDescriptionList;
  }

  // ==============================================================================

  /**
   * Returns language entries for the given description ids and release version.
   *
   * @param descriptionIdSet description ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive language entries should be included
   * @return matching language entries reduced to the latest version per id
   *     <p>Language reference set entries are versioned like other RF2 artifacts. This method
   *     applies release cutoffs and keeps only the latest state per language entry id.
   */
  public List<Language> getLanguageListByDescriptionIdSet(
      Set<String> descriptionIdSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    // Resolve modules relevant for the selected release version.
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    // Pre-filter language entries by referenced description id.
    List<Language> preFilteredLanguageList = new ArrayList<>();
    descriptionIdSet.forEach(
        descriptionId -> {
          if (descriptionIdToLanguageMap.containsKey(descriptionId)) {
            preFilteredLanguageList.addAll(descriptionIdToLanguageMap.get(descriptionId));
          }
        });

    List<Language> filteredLanguageList =
        preFilteredLanguageList.stream()
            // Module scope filter.
            .filter(language -> relevantModules.containsKey(language.getModuleId()))
            // Release cutoff filter.
            .filter(
                language -> {
                  String module = language.getModuleId();
                  long version = Long.parseLong(relevantModules.get(module));
                  return language.getEffectiveTime() <= version;
                })
            // Normalize historical versions by id.
            .sorted(Comparator.comparing(Language::getEffectiveTime))
            .collect(
                Collectors.toMap(
                    Language::getId, // Key extraction.
                    obj -> obj, // Value mapping.
                    (existing, replacement) -> replacement // On collision, keep the latest entry.
                    ))
            .values()
            .stream()
            // Apply active/inactive filter.
            .filter(language -> language.isActive() || (!language.isActive() && includeInactive))
            .toList();

    if (log.isDebugEnabled()) {
      filteredLanguageList.stream()
          .forEachOrdered(
              language ->
                  log.debug(
                      "{} {} {}",
                      language.isActive(),
                      language.getEffectiveTime(),
                      language.getReferencedComponentId(),
                      language.getAcceptabilityId()));
    }

    return filteredLanguageList;
  }

  // ==============================================================================
  // RefSet related Operations
  // ==============================================================================

  /**
   * Returns all available reference set identifiers.
   *
   * @return immutable set of reference set ids
   */
  public Set<String> getRefSetIdList() {
    return Set.copyOf(refSetIdToConceptIdMap.keySet());
  }

  /**
   * Returns reference set entries for one refset id and release version.
   *
   * @param refSetId reference set id
   * @param releaseVersion target release version
   * @param includeInactive whether inactive entries should be included
   * @return filtered reference set entries
   * @throws UnknownRefsetException if the refset id is not known
   */
  public List<RefSet> getRefSetList(
      String refSetId, SnomedReleaseVersion releaseVersion, boolean includeInactive)
      throws UnknownRefsetException {

    if (!refSetIdToConceptIdMap.containsKey(refSetId)) {
      throw new UnknownRefsetException("Unknown RefSet: " + refSetId);
    }

    List<RefSet> refSetEntries =
        refSetIdToConceptIdMap.get(refSetId).stream()
            .filter(RefSet.class::isInstance)
            .map(RefSet.class::cast)
            .toList();

    return filterByReleaseVersion(refSetEntries, releaseVersion, includeInactive);
  }

  // ==============================================================================
  // MRCM related Operations
  // ==============================================================================

  /**
   * Returns MRCM domain entries for the given release version.
   *
   * @param releaseVersion target release version
   * @param includeInactive whether inactive entries should be included
   * @return filtered MRCM domain entries reduced to the latest version per id
   */
  public List<MrcmDomain> getMrcmDomainList(
      SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    log.debug("getMrcmDomainList('{}, {}')", releaseVersion.getFhirVersion(), includeInactive);

    return filterByReleaseVersion(mrcmDomainList, releaseVersion, includeInactive);
  }

  /**
   * Returns MRCM attribute domain entries for the provided attributes and release version.
   *
   * @param attributeSet attribute ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive entries should be included
   * @return filtered MRCM attribute domain entries
   */
  public List<MrcmAttributeDomain> getMrcmAttributeDomainList(
      Set<String> attributeSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    log.debug("etAttributeDomain({}, {}, {})", attributeSet, releaseVersion, includeInactive);

    List<MrcmAttributeDomain> preFilteredAttributeDomainList = new ArrayList<>();

    // Filter by attribute id.
    attributeSet.stream()
        .forEach(
            attributeId -> {
              if (attributeIdToMRCMAttributeDomainMap.containsKey(attributeId)) {
                preFilteredAttributeDomainList.addAll(
                    attributeIdToMRCMAttributeDomainMap.get(attributeId));
              }
            });

    // Reuse shared release-version filtering logic to avoid duplicating module/version/inactive
    // handling and latest-entry normalization.
    return filterByReleaseVersion(preFilteredAttributeDomainList, releaseVersion, includeInactive);
  }

  /**
   * Returns MRCM attribute range entries for the provided attributes and release version.
   *
   * @param attributeSet attribute ids to resolve
   * @param releaseVersion target release version
   * @param includeInactive whether inactive entries should be included
   * @return filtered MRCM attribute range entries
   */
  public List<MrcmAttributeRange> getMrcmAttributeRangeList(
      Set<String> attributeSet, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    log.debug(
        "getAttributeRangeOrdered({}, {}, {})", attributeSet, releaseVersion, includeInactive);

    // Prepare pre-filtered list.
    List<MrcmAttributeRange> preFilteredAttributeRangeList = new ArrayList<>();

    // Filter by attribute id.
    attributeSet.stream()
        .forEach(
            attributeId -> {
              if (attributeIdToMRCMAttributeRangeMap.containsKey(attributeId)) {
                preFilteredAttributeRangeList.addAll(
                    attributeIdToMRCMAttributeRangeMap.get(attributeId));
              }
            });

    // Reuse shared release-version filtering logic to avoid duplicating module/version/inactive
    // handling and latest-entry normalization.
    return filterByReleaseVersion(preFilteredAttributeRangeList, releaseVersion, includeInactive);
  }

  // ==============================================================================
  // Other Operations
  // ==============================================================================

  /**
   * Filters RF entries by release-version constraints and active flag.
   *
   * <p>For each entry id, only the latest effective-time entry is kept. This provides a consistent
   * "state at release date" view across RF2 artifact types.
   *
   * @param input input entries
   * @param releaseVersion target release version
   * @param includeInactive whether inactive entries should be included
   * @param <T> RF entry type
   * @return filtered entries reduced to the latest version per id
   */
  public <T extends RfEntry> List<T> filterByReleaseVersion(
      List<T> input, SnomedReleaseVersion releaseVersion, boolean includeInactive) {

    log.debug("filterByReleaseVersion({}, {}, {})", input, releaseVersion, includeInactive);

    // list of modules relevant for the selected release version
    Map<String, String> relevantModules = releaseVersion.getRelevantModuleVersions();

    // Core normalization pipeline used by multiple query methods.
    // Steps: module scope filter -> release cutoff filter -> latest-by-id reduction ->
    // active/inactive visibility filter.
    List<T> filteredAndOrderedEntries =
        input.stream()
            // Keep only entries from modules relevant for this release version.
            .filter(entry -> relevantModules.containsKey(entry.getModuleId()))
            // Keep entries effective on or before the configured module version.
            .filter(
                entry -> {
                  String module = entry.getModuleId();
                  long version = Long.parseLong(relevantModules.get(module));
                  return entry.getEffectiveTime() <= version;
                })
            // Sort by effective time so the later map step keeps the latest entry per id.
            .sorted(Comparator.comparing(RfEntry::getEffectiveTime))
            .collect(
                Collectors.toMap(
                    RfEntry::getId, // Key extraction.
                    entry -> entry, // Value mapping.
                    (existing, replacement) -> replacement)) // On collision, keep latest entry.
            .values()
            .stream()
            // Optionally include inactive entries.
            .filter(entry -> entry.isActive() || (!entry.isActive() && includeInactive))
            .toList();

    // Debug logging of the final result set.
    if (log.isDebugEnabled()) {
      log.debug(
          "RfEntries filtered and ordered for releaseVersion {}: ",
          releaseVersion.getFhirVersion());
      filteredAndOrderedEntries.forEach(
          entry ->
              log.debug(
                  "RfEntry {} {} {}", entry.getId(), entry.isActive(), entry.getEffectiveTime()));
    }

    return filteredAndOrderedEntries;
  }
}
