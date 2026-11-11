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
package de.gematik.zts.termzilla.terminologies.loinc.memory;

import static de.gematik.zts.termzilla.terminologies.loinc.LoincConstants.LOINC_PARTTYPENAME_NOT_IN_PART_FILE;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import de.gematik.zts.termzilla.terminologies.loinc.exceptions.LoincInitializationException;
import de.gematik.zts.termzilla.terminologies.loinc.helper.LoincReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincAnswerListVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincAnswerVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincConceptVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincLinguisticVariantVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincPartLinkVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincPartVersionFromChangeSnapshotLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincPartVersionFromHierarchyLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.loader.LoincPartVersionLoader;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LinguisticVariantVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LoincPartLinkVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

/**
 * In-memory index for versioned LOINC artifacts loaded from one or more release files.
 *
 * <p>The representation stores concepts, parts, part links, linguistic variants, answers, and
 * answer lists with version-aware retrieval semantics.
 */
@Slf4j
public class LoincInMemoryRepresentation {

  // --------------------------------------------------------------------------------------------

  // LOINC versions (e.g. 2.66, 2.67, ...)
  @Getter private final Set<LoincVersion> loincVersionList = new LinkedHashSet<>();
  private final Map<String, LoincVersion> versionMap = new HashMap<>();

  // --------------------------------------------------------------------------------------------

  // Classic LOINC concepts (e.g. 100000-9)
  private final List<ConceptVersion> conceptVersionList = new ArrayList<>();

  @Getter
  private final MultiValuedMap<String, ConceptVersion> loincNumberToConceptVersionMap =
      new ArrayListValuedHashMap<>();

  // --------------------------------------------------------------------------------------------
  // Loinc Parts (e.g. LP248772-8)
  private final List<PartVersion> partVersionList = new ArrayList<>();

  @Getter
  private final MultiValuedMap<String, PartVersion> partNumberToPartVersionMap =
      new ArrayListValuedHashMap<>();

  private final MultiValuedMap<String, PartVersion> partNameToPartVersionMap =
      new ArrayListValuedHashMap<>();

  // --------------------------------------------------------------------------------------------
  // LoincPartLinks

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      componentLoincNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      componentPartNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      propertyLoincNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      propertyPartNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      timeLoincNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      timePartNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      systemLoincNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      systemPartNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      scaleLoincNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  @Getter
  private final MultiValuedMap<String, LoincPartLinkVersion>
      scalePartNumberToLoincPartLinkVersionMap = new ArrayListValuedHashMap<>();

  // --------------------------------------------------------------------------------------------
  // Linguistic variants
  private final List<LinguisticVariantVersion> linguisticVariantVersionList = new ArrayList<>();

  @Getter
  private final MultiValuedMap<String, LinguisticVariantVersion>
      loincNumberToLinguisticVariantVersionMap = new ArrayListValuedHashMap<>();

  private Set<String> currentLinguisticVariantKeys = new HashSet<>();

  // --------------------------------------------------------------------------------------------
  // Answers
  private final List<AnswerVersion> answerVersionList = new ArrayList<>();

  @Getter
  private final MultiValuedMap<String, AnswerVersion> answerStringIdToAnswerVersionMap =
      new ArrayListValuedHashMap<>();

  private Set<String> currentAnswerStringIds = new HashSet<>();

  @Getter
  private final MultiValuedMap<String, AnswerVersion> answerStringIdToAnswerVersionMapUnVersioned =
      new ArrayListValuedHashMap<>();

  // --------------------------------------------------------------------------------------------
  // AnswerList
  private final Set<AnswerListVersion> answerListVersionList = new LinkedHashSet<>();

  private Set<String> currentAnswerListIds = new HashSet<>();

  @Getter
  private final MultiValuedMap<String, AnswerListVersion> answerListIdToAnswerListVersionMap =
      new ArrayListValuedHashMap<>();

  // ================================================================================

  /**
   * Creates the in-memory representation and initializes it from the configured release collection.
   *
   * @param properties LOINC module configuration
   * @throws InvalidConfigurationException if configuration is invalid or release artifacts cannot
   *     be initialized
   */
  public LoincInMemoryRepresentation(LoincConfigProperties properties)
      throws InvalidConfigurationException {

    log.info("");
    log.info(
        "######################################################################################");
    log.info("Generating LOINC In-Memory-Model");

    // Initialize in-memory structures from release files
    initializeFromRfCollection(
        properties.getLoincRfCollectionLocation(),
        properties.getLoincUnzipLocation(),
        properties.getLoincLanguages());

    log.info("LOINC In-Memory-Model initialized!");
  }

  // ================================================================================

  /**
   * Initializes the in-memory representation from a LOINC release collection, i.e. a sequence of
   * consecutive LOINC versions.
   *
   * @param loincRfCollectionLocation path to the LOINC release collection. The folder must contain
   *     LOINC release files either directly or in subfolders. File names must follow the LOINC
   *     naming convention.
   * @param unzipLocation path where release files are extracted
   * @param languageList languages for which linguistic variants are loaded
   */
  private void initializeFromRfCollection(
      String loincRfCollectionLocation, Path unzipLocation, List<String> languageList)
      throws InvalidConfigurationException {

    log.info("Initializing LOINC In-Memory-Model from release collection");

    // Determine all LOINC release files in sorted order
    List<String> loincRfFiles =
        LoincReleaseFileHelper.getMultipleReleaseFilePaths(loincRfCollectionLocation);

    if (loincRfFiles.isEmpty()) {
      throw new LoincInitializationException(
          "No LOINC release files found in given location. Please check your configuration.");
    }

    // Required to validate that versions are consecutive
    int lastVersion = 0;

    // Iterate over all discovered LOINC release files
    for (String loincRfFile : loincRfFiles) {

      // Extract LOINC release file
      LoincReleaseFileHelper.unzip(loincRfFile, unzipLocation);

      // Determine version from file name
      int validFromVersion = 0;
      Pattern pattern = Pattern.compile("Loinc_(2\\.\\d{2})?\\.zip$");
      Matcher matcher = pattern.matcher(loincRfFile);
      if (matcher.find()) {
        String extractedVersion = matcher.group(1);
        String version = extractedVersion.replace(".", "");
        validFromVersion = Integer.parseInt(version);

        // Check whether LOINC release versions are consecutive
        if (lastVersion != 0 && lastVersion != validFromVersion - 1) {
          throw new LoincInitializationException(
              "LOINC release file versions are not consecutive. Please check your release file collection and add missing files.");
        }
        lastVersion = validFromVersion;

        // Check whether LOINC version is >= 2.66
        if (validFromVersion < 266) {
          throw new LoincInitializationException(
              "LOINC versions prior to "
                  + extractedVersion
                  + " are not supported. as no ChangeSnapshot is provided. Please use a LOINC version >= 2.66");
        }

        // Create LoincVersion object and add it to list and map
        LoincVersion loincVersion =
            LoincVersion.builder()
                .versionNumber(validFromVersion)
                .versionString(extractedVersion)
                .build();
        loincVersionList.add(loincVersion);
        versionMap.put(extractedVersion, loincVersion);
      }

      // Load LOINC release data into the index
      initializeFromRfFiles(unzipLocation, validFromVersion, languageList);

      // Delete extracted LOINC release files
      LoincReleaseFileHelper.deleteUnzippedFiles(unzipLocation);
    }

    // Sort all MultiValueMaps ascending according to validFromVersion
    sortMultiValueMaps();
  }

  /**
   * Initializes the index with data from extracted release files.
   *
   * @param loincUnzipLocation path containing extracted release files
   * @param validFromVersion version number that becomes valid for loaded records
   * @param languageList languages for linguistic variants
   */
  private void initializeFromRfFiles(
      Path loincUnzipLocation, int validFromVersion, List<String> languageList) {
    log.info("Initializing LOINC In-Memory-Model from release files");

    String conceptFile;
    String partFile;
    String loincPartLinkFile;
    String linguisticVariantFile;
    String answerListFile;
    String componentHierarchyFile;
    String partChangeSnapshotFile;

    try {
      // check if zip file exists in given location
      log.info("Trying to load LOINC from {}", loincUnzipLocation);
      if (!Files.exists(loincUnzipLocation)) {
        throw new LoincInitializationException("Directory not found. Please adjust configuration");
      }

      // Create Models
      long start = System.currentTimeMillis();

      // --------------------------------------------------------------------------------------------
      // Load concepts
      conceptFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN)
              .toAbsolutePath()
              .toString();
      LoincConceptVersionLoader conceptLoader = new LoincConceptVersionLoader();
      conceptLoader.load(conceptFile, validFromVersion, this);

      // --------------------------------------------------------------------------------------------
      // Load parts
      partFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_PART_PATTERN)
              .toAbsolutePath()
              .toString();
      LoincPartVersionLoader partLoader = new LoincPartVersionLoader();
      partLoader.load(partFile, validFromVersion, this);

      // Load additional parts from part hierarchy. Not all parts are listed in Parts.csv.
      // Important: this must run after loading Parts.csv.
      componentHierarchyFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_PART_HIERARCHY_PATTERN)
              .toAbsolutePath()
              .toString();
      LoincPartVersionFromHierarchyLoader partHierarchyLoader =
          new LoincPartVersionFromHierarchyLoader();
      partHierarchyLoader.load(componentHierarchyFile, validFromVersion, this);

      // --------------------------------------------------------------------------------------------
      // Load part change snapshot
      partChangeSnapshotFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_PART_CHANGESNAPSHOT_PATTERN)
              .toAbsolutePath()
              .toString();
      LoincPartVersionFromChangeSnapshotLoader partChangeSnapshotLoader =
          new LoincPartVersionFromChangeSnapshotLoader();
      partChangeSnapshotLoader.load(partChangeSnapshotFile, validFromVersion, this);

      // --------------------------------------------------------------------------------------------
      // Load LOINC part links
      loincPartLinkFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_LOINC_PARTLINK_PATTERN)
              .toAbsolutePath()
              .toString();
      LoincPartLinkVersionLoader loincPartLinkLoader = new LoincPartLinkVersionLoader();
      loincPartLinkLoader.load(loincPartLinkFile, validFromVersion, this);

      // --------------------------------------------------------------------------------------------
      // Load answers
      answerListFile =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
                  loincUnzipLocation, LoincReleaseFileHelper.FILE_ANSWERLIST_PATTERN)
              .toAbsolutePath()
              .toString();
      currentAnswerStringIds = new HashSet<>();
      LoincAnswerVersionLoader answerLoader = new LoincAnswerVersionLoader();
      answerLoader.load(answerListFile, validFromVersion, this);

      // Compare current answers with previous versions so removed answers can be marked accordingly
      Set<String> removedAnswerStringIds = new HashSet<>(answerStringIdToAnswerVersionMap.keySet());
      removedAnswerStringIds.removeAll(currentAnswerStringIds);
      // The result now contains answer IDs that no longer exist in the current version.
      removedAnswerStringIds.stream()
          .forEach(
              answerId ->
                  // Resolve matching versions
                  answerStringIdToAnswerVersionMap.get(answerId).stream()
                      // Sort by ValidFromVersion and use latest entry
                      .max(Comparator.comparing(AnswerVersion::getValidFromVersion))
                      // Only active answers must be deactivated
                      .filter(answerVersion -> answerVersion.getStatus().equals("ACTIVE"))
                      .ifPresent(
                          answerVersion -> {
                            AnswerVersion answerVersionInactive = new AnswerVersion();
                            answerVersionInactive.setValidFromVersion(validFromVersion);
                            answerVersionInactive.setStatus("INACTIVE");
                            answerVersionInactive.setAnswerStringId(
                                answerVersion.getAnswerStringId());
                            answerVersionInactive.setDisplayText(answerVersion.getDisplayText());
                            answerVersionList.add(answerVersionInactive);
                            answerStringIdToAnswerVersionMap.put(
                                answerVersionInactive.getAnswerStringId(), answerVersionInactive);
                          }));

      // Load answer lists

      // This set stores all answer lists present in the currently loaded LOINC version
      currentAnswerListIds = new HashSet<>();

      // Load answer lists from release file
      LoincAnswerListVersionLoader answerListLoader = new LoincAnswerListVersionLoader();
      answerListLoader.load(answerListFile, validFromVersion, this);

      // Compare current answer lists with previous versions so removed lists can be marked
      Set<String> removedAnswerListIds = new HashSet<>(answerListIdToAnswerListVersionMap.keySet());
      removedAnswerListIds.removeAll(currentAnswerListIds);

      // The result now contains answer list IDs that no longer exist in the current version.
      removedAnswerListIds.stream()
          .forEach(
              answerListId ->
                  // Resolve matching versions
                  answerListIdToAnswerListVersionMap.get(answerListId).stream()
                      // Sort by ValidFromVersion and use latest entry
                      .max(Comparator.comparing(AnswerListVersion::getValidFromVersion))
                      // Only active answer lists must be deactivated
                      .filter(answerListVersion -> answerListVersion.getStatus().equals("ACTIVE"))
                      .ifPresent(
                          answerListVersion -> {
                            AnswerListVersion answerListVersionInactive = new AnswerListVersion();
                            answerListVersionInactive.setValidFromVersion(validFromVersion);
                            answerListVersionInactive.setStatus("INACTIVE");
                            answerListVersionInactive.setAnswerListId(
                                answerListVersion.getAnswerListId());
                            answerListVersionInactive.setAnswerListName(
                                answerListVersion.getAnswerListName());

                            answerListVersionList.add(answerListVersionInactive);
                            answerListIdToAnswerListVersionMap.put(
                                answerListVersionInactive.getAnswerListId(),
                                answerListVersionInactive);
                          }));

      // Load linguistic variants. Multiple languages are supported, therefore this runs per
      // configured language.
      currentLinguisticVariantKeys = new HashSet<>();
      for (String language : languageList) {

        String lvFilePattern = LoincConstants.LANGUAGE_TO_LV_PATTERN_MAP.get(language);
        if (lvFilePattern == null) {
          log.warn(
              "No linguistic variant file pattern found for language: {} Could not load translations.",
              language);
        }

        if (LoincReleaseFileHelper.getSingleReleaseFilePath(loincUnzipLocation, lvFilePattern)
            != null) {

          linguisticVariantFile =
              LoincReleaseFileHelper.getSingleReleaseFilePath(loincUnzipLocation, lvFilePattern)
                  .toAbsolutePath()
                  .toString();
        } else {
          linguisticVariantFile = null;
        }

        if (linguisticVariantFile != null) {
          LoincLinguisticVariantVersionLoader linguisticVariantLoader =
              new LoincLinguisticVariantVersionLoader();
          linguisticVariantLoader.load(linguisticVariantFile, validFromVersion, language, this);
        }
      }

      // Mark translations as inactive if they were present in a previous version but are missing
      // in the current release.
      Map<String, LinguisticVariantVersion> latestLinguisticVariantsByKey =
          getLatestLinguisticVariantsByKey();
      Set<String> removedLinguisticVariantKeys =
          determineRemovedActiveLinguisticVariantKeys(latestLinguisticVariantsByKey);
      markRemovedLinguisticVariantsInactive(
          removedLinguisticVariantKeys, latestLinguisticVariantsByKey, validFromVersion);

      long stop = System.currentTimeMillis();
      log.info("Duration to load LOINC artifacts into In-Memory-Model: {} ms", (stop - start));

    } catch (Exception e) {
      log.error("Error while loading LOINC artifacts into In-Memory-Model: {}", e.getMessage());
      throw new LoincInitializationException(e);
    }
  }

  /**
   * Determines translation keys that were active in the previous state but are absent in the
   * currently loaded release.
   *
   * @param latestLinguisticVariantsByKey latest known linguistic variants keyed by {@code
   *     loincNumber|language}
   * @return keys of active linguistic variants that were removed in the current release
   */
  private Set<String> determineRemovedActiveLinguisticVariantKeys(
      Map<String, LinguisticVariantVersion> latestLinguisticVariantsByKey) {
    Set<String> removedLinguisticVariantKeys =
        latestLinguisticVariantsByKey.values().stream()
            .filter(
                linguisticVariantVersion ->
                    LinguisticVariantVersion.STATUS_ACTIVE.equals(
                        linguisticVariantVersion.getStatus()))
            .map(this::getLinguisticVariantKey)
            .collect(Collectors.toSet());
    removedLinguisticVariantKeys.removeAll(currentLinguisticVariantKeys);
    return removedLinguisticVariantKeys;
  }

  /**
   * Creates inactive linguistic-variant versions for all removed translation keys.
   *
   * @param removedLinguisticVariantKeys keys that are no longer present in the current release
   * @param latestLinguisticVariantsByKey latest known linguistic variants keyed by {@code
   *     loincNumber|language}
   * @param validFromVersion version that becomes valid for generated inactive entries
   */
  private void markRemovedLinguisticVariantsInactive(
      Set<String> removedLinguisticVariantKeys,
      Map<String, LinguisticVariantVersion> latestLinguisticVariantsByKey,
      int validFromVersion) {

    removedLinguisticVariantKeys.forEach(
        linguisticVariantKey -> {
          LinguisticVariantVersion previousLinguisticVariant =
              latestLinguisticVariantsByKey.get(linguisticVariantKey);
          if (previousLinguisticVariant != null) {
            log.info(
                "Concept translation removed for LOINC {} and language {} in version {}. Marking translation as INACTIVE.",
                previousLinguisticVariant.getLoincNumber(),
                previousLinguisticVariant.getLanguage(),
                validFromVersion);

            LinguisticVariantVersion linguisticVariantVersionInactive =
                new LinguisticVariantVersion();
            linguisticVariantVersionInactive.setValidFromVersion(validFromVersion);
            linguisticVariantVersionInactive.setStatus(LinguisticVariantVersion.STATUS_INACTIVE);
            linguisticVariantVersionInactive.setLanguage(previousLinguisticVariant.getLanguage());
            linguisticVariantVersionInactive.setLoincNumber(
                previousLinguisticVariant.getLoincNumber());
            linguisticVariantVersionList.add(linguisticVariantVersionInactive);
            loincNumberToLinguisticVariantVersionMap.put(
                linguisticVariantVersionInactive.getLoincNumber(),
                linguisticVariantVersionInactive);
          }
        });
  }

  // ==============================================================================
  // Methods used by loaders to populate the in-memory index
  // ==============================================================================

  /**
   * Adds a LOINC version to version lookup structures.
   *
   * @param loincVersion version metadata to add
   */
  public void addLoincVersion(LoincVersion loincVersion) {
    loincVersionList.add(loincVersion);
    versionMap.put(loincVersion.getVersionString(), loincVersion);
  }

  // --------------------------------------------------------------------------------------------
  // TODO: PROBLEM: If an entry changes and later reverts back, the current logic can still break.
  //  Ideally, the most recently changed entry should always be selected. Concepts should be less
  //  affected due to the getVersionLastChanged field.
  // --------------------------------------------------------------------------------------------

  /**
   * Adds a concept version to the index if an equivalent version is not already present.
   *
   * @param conceptVersion concept version to add
   */
  public void addConcept(ConceptVersion conceptVersion) {

    // Determine all identical concept versions
    Set<ConceptVersion> existingConcepts =
        loincNumberToConceptVersionMap.get(conceptVersion.getLoincNum()).stream()
            .filter(
                existingConcept ->
                    existingConcept
                        .getVersionLastChanged()
                        .equals(conceptVersion.getVersionLastChanged()))
            .collect(Collectors.toSet());

    // Add concept version only if no identical entry exists
    if (existingConcepts.isEmpty()) {
      conceptVersionList.add(conceptVersion);
      loincNumberToConceptVersionMap.put(conceptVersion.getLoincNum(), conceptVersion);
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Adds a part version to the index if an equivalent version is not already present.
   *
   * @param partVersion part version to add
   */
  public void addPart(PartVersion partVersion) {

    // TODO: PROBLEM: see note above - maybe already addressed via map handling

    // Determine all identical part versions
    Set<PartVersion> existingParts =
        partNumberToPartVersionMap.get(partVersion.getPartNumber()).stream()
            .sorted(Comparator.comparing(PartVersion::getValidFromVersion))
            .collect(
                Collectors.toMap(
                    PartVersion::getPartNumber, // key extraction
                    obj -> obj, // value mapping (identity)
                    (existing, replacement) ->
                        replacement // Collision handling: keep replacement object
                    ))
            .values()
            .stream()
            .filter(existingPart -> existingPart.equals(partVersion))
            .collect(Collectors.toSet());

    // Add part version only if no identical entry exists
    if (existingParts.isEmpty()) {
      partVersionList.add(partVersion);
      partNumberToPartVersionMap.put(partVersion.getPartNumber(), partVersion);
      partNameToPartVersionMap.put(partVersion.getPartName(), partVersion);
    }
  }

  /**
   * Adds a part from hierarchy data, applying special handling for parts that are not available in
   * the main parts file.
   *
   * @param partVersion part version derived from hierarchy input
   */
  public void addPartHierarchy(PartVersion partVersion) {

    // Determine matching part versions. For hierarchy-based loading only PartNumber is reliable.
    Set<PartVersion> existingParts =
        new HashSet<>(
            partNumberToPartVersionMap.get(partVersion.getPartNumber()).stream()
                .sorted(Comparator.comparing(PartVersion::getValidFromVersion))
                .collect(
                    Collectors.toMap(
                        PartVersion::getPartNumber, // key extraction
                        obj -> obj, // value mapping (identity)
                        (existing, replacement) ->
                            replacement // Collision handling: keep replacement object
                        ))
                .values());

    // Add part if no matching part version exists.
    if (existingParts.isEmpty()) {
      // Case 1: no matching part exists -> treat as special part not present in Parts.csv
      partVersion.setPartTypeName(LOINC_PARTTYPENAME_NOT_IN_PART_FILE);
      partVersionList.add(partVersion);
      partNumberToPartVersionMap.put(partVersion.getPartNumber(), partVersion);
      partNameToPartVersionMap.put(partVersion.getPartName(), partVersion);
    } else {
      // Case 2: a part exists -> could be a standard part or a special part from an older version
      PartVersion existingPartVersion = existingParts.iterator().next();
      if (existingPartVersion.getPartTypeName().equals(LOINC_PARTTYPENAME_NOT_IN_PART_FILE)
          && !existingPartVersion.getPartName().equals(partVersion.getPartName())) {
        partVersion.setPartTypeName(LOINC_PARTTYPENAME_NOT_IN_PART_FILE);
        partVersionList.add(partVersion);
        partNumberToPartVersionMap.put(partVersion.getPartNumber(), partVersion);
        partNameToPartVersionMap.put(partVersion.getPartName(), partVersion);
      }
    }
  }

  /**
   * Adds a part version from the part change snapshot when the status differs from the latest known
   * version.
   *
   * @param partVersion part version from change snapshot
   */
  public void addPartChangeSnapshot(PartVersion partVersion) {

    // Determine relevant part versions
    Set<PartVersion> existingParts =
        new HashSet<>(
                partNumberToPartVersionMap.get(partVersion.getPartNumber()).stream()
                    // Sort part versions by release version
                    .sorted(Comparator.comparing(PartVersion::getValidFromVersion))
                    // Reduce to latest version
                    .collect(
                        Collectors.toMap(
                            PartVersion::getPartNumber, // key extraction
                            obj -> obj, // value mapping (identity)
                            (existing, replacement) ->
                                replacement // Collision handling: keep replacement object
                            ))
                    .values())
            .stream()
                // We filter out entries where the status has not changed
                .filter(existingPart -> !existingPart.getStatus().equals(partVersion.getStatus()))
                .collect(Collectors.toSet());

    // If status differs from predecessor, add the new version and copy fields unavailable in
    // ChangeSnapshot from previous version.
    if (!existingParts.isEmpty()) {
      PartVersion existingPartVersion = existingParts.iterator().next();
      partVersion.setPartTypeName(existingPartVersion.getPartTypeName());
      partVersion.setPartName(existingPartVersion.getPartName());
      partVersion.setPartDisplayName(existingPartVersion.getPartDisplayName());
      partVersionList.add(partVersion);
      partNumberToPartVersionMap.put(partVersion.getPartNumber(), partVersion);
      partNameToPartVersionMap.put(partVersion.getPartName(), partVersion);
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Routes a part link to the corresponding type-specific maps.
   *
   * @param loincPartLinkVersion part link version to add
   */
  public void addLoincPartLink(LoincPartLinkVersion loincPartLinkVersion) {

    switch (loincPartLinkVersion.getPartTypeName()) {
      case LoincConstants.LOINC_PARTTYPENAME_COMPONENT:
        addLoincPartLinkComponent(
            loincPartLinkVersion,
            componentLoincNumberToLoincPartLinkVersionMap,
            componentPartNumberToLoincPartLinkVersionMap);
        break;
      case LoincConstants.LOINC_PARTTYPENAME_SYSTEM:
        addLoincPartLinkComponent(
            loincPartLinkVersion,
            systemLoincNumberToLoincPartLinkVersionMap,
            systemPartNumberToLoincPartLinkVersionMap);
        break;
      case LoincConstants.LOINC_PARTTYPENAME_TIME:
        addLoincPartLinkComponent(
            loincPartLinkVersion,
            timeLoincNumberToLoincPartLinkVersionMap,
            timePartNumberToLoincPartLinkVersionMap);
        break;
      case LoincConstants.LOINC_PARTTYPENAME_SCALE:
        addLoincPartLinkComponent(
            loincPartLinkVersion,
            scaleLoincNumberToLoincPartLinkVersionMap,
            scalePartNumberToLoincPartLinkVersionMap);
        break;
      case LoincConstants.LOINC_PARTTYPENAME_PROPERTY:
        addLoincPartLinkComponent(
            loincPartLinkVersion,
            propertyLoincNumberToLoincPartLinkVersionMap,
            propertyPartNumberToLoincPartLinkVersionMap);
        break;
      default:
        break;
    }
  }

  /**
   * Adds a part link to both LOINC-number and part-number maps if it is not already present.
   *
   * @param loincPartLinkVersion part link version to add
   * @param mapLoincNumber map keyed by LOINC number
   * @param mapPartNumber map keyed by part number
   */
  public void addLoincPartLinkComponent(
      LoincPartLinkVersion loincPartLinkVersion,
      MultiValuedMap<String, LoincPartLinkVersion> mapLoincNumber,
      MultiValuedMap<String, LoincPartLinkVersion> mapPartNumber) {

    Set<LoincPartLinkVersion> existingLoincPartLinks =
        mapLoincNumber.get(loincPartLinkVersion.getLoincNumber()).stream()
            .sorted(Comparator.comparing(LoincPartLinkVersion::getValidFromVersion))
            .collect(
                Collectors.toMap(
                    LoincPartLinkVersion::getLoincNumber, // key extraction
                    obj -> obj, // value mapping (identity)
                    (existing, replacement) ->
                        replacement // collision handling (use replacement object)
                    ))
            .values()
            .stream()
            .filter(existingLoincPartLink -> existingLoincPartLink.equals(loincPartLinkVersion))
            .collect(Collectors.toSet());

    if (existingLoincPartLinks.isEmpty()) {
      mapLoincNumber.put(loincPartLinkVersion.getLoincNumber(), loincPartLinkVersion);
      mapPartNumber.put(loincPartLinkVersion.getPartNumber(), loincPartLinkVersion);
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Adds a linguistic variant version to the index if an equivalent entry is not already present.
   *
   * @param linguisticVariantVersion linguistic variant version to add
   */
  public void addLinguisticVariant(LinguisticVariantVersion linguisticVariantVersion) {

    currentLinguisticVariantKeys.add(getLinguisticVariantKey(linguisticVariantVersion));

    if (linguisticVariantVersion.getStatus() == null) {
      linguisticVariantVersion.setStatus(LinguisticVariantVersion.STATUS_ACTIVE);
    }

    // Determine all identical linguistic variant versions
    Set<LinguisticVariantVersion> existingLinguisticVariants =
        loincNumberToLinguisticVariantVersionMap
            .get(linguisticVariantVersion.getLoincNumber())
            .stream()
            .sorted(Comparator.comparing(LinguisticVariantVersion::getValidFromVersion))
            .collect(
                Collectors.toMap(
                    LinguisticVariantVersion::getLoincNumber, // key extraction
                    obj -> obj, // value mapping (identity)
                    (existing, replacement) ->
                        replacement // collision handling (use replacement object)
                    ))
            .values()
            .stream()
            .filter(
                existingLinguisticVariant ->
                    existingLinguisticVariant.equals(linguisticVariantVersion))
            .collect(Collectors.toSet());

    // Add linguistic variant only if no identical entry exists
    if (existingLinguisticVariants.isEmpty()) {
      linguisticVariantVersionList.add(linguisticVariantVersion);
      loincNumberToLinguisticVariantVersionMap.put(
          linguisticVariantVersion.getLoincNumber(), linguisticVariantVersion);
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Adds an answer version to the index if an equivalent entry is not already present.
   *
   * @param answerVersion answer version to add
   */
  public void addAnswer(AnswerVersion answerVersion) {

    // Needed to detect answer IDs that were removed compared to the previous version
    currentAnswerStringIds.add(answerVersion.getAnswerStringId());

    // Determine all identical answer versions
    Set<AnswerVersion> existingAnswers =
        answerStringIdToAnswerVersionMap.get(answerVersion.getAnswerStringId()).stream()
            .sorted(Comparator.comparing(AnswerVersion::getValidFromVersion))
            .collect(
                Collectors.toMap(
                    AnswerVersion::getAnswerStringId, // key extraction
                    obj -> obj, // value mapping (identity)
                    (existing, replacement) ->
                        replacement // collision handling (use replacement object)
                    ))
            .values()
            .stream()
            .filter(existingAnswer -> existingAnswer.equals(answerVersion))
            .collect(Collectors.toSet());

    // Add answer version only if no identical entry exists
    if (existingAnswers.isEmpty()) {
      answerVersionList.add(answerVersion);
      answerStringIdToAnswerVersionMap.put(answerVersion.getAnswerStringId(), answerVersion);
    }
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Adds answer list versions for the current release version.
   *
   * @param answerListMap map of answer list IDs to answer list versions
   */
  public void addAnswerListsForCurrentVersion(Map<String, AnswerListVersion> answerListMap) {

    // Needed to detect answer lists that were removed compared to the previous version
    currentAnswerListIds.addAll(answerListMap.keySet());

    // Iterate over all answer list versions in the current release
    for (AnswerListVersion alv : answerListMap.values()) {

      // Resolve the answer list version with the highest ValidFromVersion
      AnswerListVersion latestAnswerListVersion =
          answerListIdToAnswerListVersionMap.get(alv.getAnswerListId()).stream()
              .max(Comparator.comparing(AnswerListVersion::getValidFromVersion))
              .orElse(null);

      // If there is no existing version, add the provided one directly
      if (latestAnswerListVersion == null) {
        answerListVersionList.add(alv);
        answerListIdToAnswerListVersionMap.put(alv.getAnswerListId(), alv);
        continue;
      }

      if (!latestAnswerListVersion.equals(alv)) {
        // If latest version differs from input version, add the new version
        log.info("{}", alv.toString());
        answerListVersionList.add(alv);
        answerListIdToAnswerListVersionMap.put(alv.getAnswerListId(), alv);

      } else {
        // If latest version equals input version, no action is required
        log.info(
            "AnswerListVersion with AnswerListId {} and ValidFromVersion {} already exists. No new version added.",
            alv.getAnswerListId(),
            alv.getValidFromVersion());
      }
    }
  }

  /**
   * Adds an answer list version to the index if an equivalent entry is not already present.
   *
   * @param answerListVersion answer list version to add
   */
  public void addAnswerList(AnswerListVersion answerListVersion) {

    currentAnswerListIds.add(answerListVersion.getAnswerListId());

    // Determine all identical answer list versions
    Set<AnswerListVersion> existingAnswerLists =
        answerListIdToAnswerListVersionMap.get(answerListVersion.getAnswerListId()).stream()
            .sorted(Comparator.comparing(AnswerListVersion::getValidFromVersion))
            .collect(
                Collectors.toMap(
                    AnswerListVersion::getAnswerListId, // key extraction
                    obj -> obj, // value mapping (identity)
                    (existing, replacement) ->
                        replacement // collision handling (use replacement object)
                    ))
            .values()
            .stream()
            .filter(existingAnswerList -> existingAnswerList.equals(answerListVersion))
            .collect(Collectors.toSet());

    // Add answer list version only if no identical entry exists
    if (existingAnswerLists.isEmpty()) {
      answerListVersionList.add(answerListVersion);
      answerListIdToAnswerListVersionMap.put(
          answerListVersion.getAnswerListId(), answerListVersion);
    }
  }

  // ==============================================================================
  // Version related operations
  // ==============================================================================

  /**
   * Returns a LOINC version by its textual version identifier.
   *
   * @param version LOINC version string (for example {@code 2.67})
   * @return matching version wrapped in {@link Optional}
   */
  public Optional<LoincVersion> getLoincVersion(String version) {
    return Optional.ofNullable(versionMap.get(version));
  }

  /**
   * Sort all MultiValuesMaps that require sorting at the beginning to avoid multiple sorting later.
   */
  private void sortMultiValueMaps() {
    // Classic LOINC concepts
    sortValuesByValidFromVersion(
        loincNumberToConceptVersionMap, ConceptVersion::getValidFromVersion);
  }

  /**
   * Sort ascending by ValidFromVersion.
   *
   * @param mapNum MapNumber
   * @param validFromExtractor Loinc validFromExtractor
   */
  private <V> void sortValuesByValidFromVersion(
      MultiValuedMap<String, V> mapNum, ToIntFunction<V> validFromExtractor) {
    Comparator<V> comparator = Comparator.comparingInt(validFromExtractor);
    mapNum.asMap().values().forEach(versions -> ((List<V>) versions).sort(comparator));
  }

  // ==============================================================================
  // Concept related operations
  // ==============================================================================

  /**
   * Returns all stored concept versions for a LOINC number.
   *
   * @param loincNum LOINC number
   * @return all known versions for the given LOINC number
   */
  public Collection<ConceptVersion> getConceptVersionListByLoincNumber(String loincNum) {
    return loincNumberToConceptVersionMap.get(loincNum);
  }

  /**
   * Returns the concept version valid for the requested LOINC version.
   *
   * @param loincNum LOINC number
   * @param version target LOINC version
   * @return latest concept version valid at the requested version
   */
  public Optional<ConceptVersion> getConceptVersion(String loincNum, LoincVersion version) {
    return loincNumberToConceptVersionMap.get(loincNum).stream()
        .filter(
            conceptVersion -> conceptVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching concept version
        .max(Comparator.comparing(ConceptVersion::getValidFromVersion));
  }

  /**
   * Returns the concept snapshot for all LOINC numbers at the requested version.
   *
   * @param version target LOINC version
   * @return list containing one versioned concept per LOINC number
   */
  public List<ConceptVersion> getConceptVersionList(LoincVersion version) {
    return loincNumberToConceptVersionMap.values().stream()
        .filter(
            conceptVersion -> conceptVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching concept version
        .collect(
            Collectors.toMap(
                ConceptVersion::getLoincNum, // key extraction
                obj -> obj, // value mapping (identity)
                (existing, replacement) ->
                    replacement // collision handling (use replacement object)
                ))
        .values()
        .stream()
        .toList();
  }

  // ==============================================================================
  // Part related operations
  // ==============================================================================

  /**
   * Returns all stored part versions for a part number.
   *
   * @param loincNumber part number
   * @return all known versions for the part number
   */
  public Collection<PartVersion> getPartVersionListByPartNumber(String loincNumber) {
    return partNumberToPartVersionMap.get(loincNumber);
  }

  /**
   * Returns the part version valid for the requested version by part number.
   *
   * @param partNum part number
   * @param version target LOINC version
   * @return latest part version valid at the requested version
   */
  public Optional<PartVersion> getPartVersionByPartNumber(String partNum, LoincVersion version) {
    return partNumberToPartVersionMap.get(partNum).stream()
        .filter(partVersion -> partVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching part version
        .max(Comparator.comparing(PartVersion::getValidFromVersion));
  }

  /**
   * Returns the part version valid for the requested version by part name.
   *
   * @param partName part name
   * @param version target LOINC version
   * @return latest part version valid at the requested version
   */
  public Optional<PartVersion> getPartVersionByPartName(String partName, LoincVersion version) {
    return partNameToPartVersionMap.get(partName).stream()
        .filter(partVersion -> partVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching part version
        .max(Comparator.comparing(PartVersion::getValidFromVersion));
  }

  // ==============================================================================
  // Linguistic variant related operations
  // ==============================================================================

  /**
   * Returns all stored linguistic variant versions for a LOINC number.
   *
   * @param loincNum LOINC number
   * @return all linguistic variants for the given LOINC number
   */
  public Collection<LinguisticVariantVersion> getLinguisticVariantVersionListByLoincNumber(
      String loincNum) {
    return loincNumberToLinguisticVariantVersionMap.get(loincNum);
  }

  /**
   * Returns the active linguistic variant valid for a version and language.
   *
   * @param loincNum LOINC number
   * @param version target LOINC version
   * @param language language code
   * @return active linguistic variant if available for the requested context
   */
  public Optional<LinguisticVariantVersion> getLinguisticVariantVersion(
      String loincNum, LoincVersion version, String language) {

    return loincNumberToLinguisticVariantVersionMap.get(loincNum).stream()
        // Filter by language
        .filter(conceptVersion -> conceptVersion.getLanguage().equals(language))
        // Filter by valid-from version
        .filter(
            conceptVersion -> conceptVersion.getValidFromVersion() <= version.getVersionNumber())
        .max(Comparator.comparing(LinguisticVariantVersion::getValidFromVersion))
        .filter(
            linguisticVariantVersion ->
                LinguisticVariantVersion.STATUS_ACTIVE.equals(
                    linguisticVariantVersion.getStatus()));
  }

  /**
   * Returns the latest linguistic variant version for each logical translation key.
   *
   * @return map keyed by {@code loincNumber|language} containing latest versions
   */
  private Map<String, LinguisticVariantVersion> getLatestLinguisticVariantsByKey() {
    return loincNumberToLinguisticVariantVersionMap.values().stream()
        .collect(
            Collectors.toMap(
                this::getLinguisticVariantKey,
                linguisticVariantVersion -> linguisticVariantVersion,
                (existing, replacement) ->
                    existing.getValidFromVersion() >= replacement.getValidFromVersion()
                        ? existing
                        : replacement));
  }

  /**
   * Builds the internal key used to identify one translation stream across versions.
   *
   * @param linguisticVariantVersion linguistic variant
   * @return key in the form {@code loincNumber|language}
   */
  private String getLinguisticVariantKey(LinguisticVariantVersion linguisticVariantVersion) {
    return linguisticVariantVersion.getLoincNumber() + "|" + linguisticVariantVersion.getLanguage();
  }

  // ==============================================================================
  // Answer related operations
  // ==============================================================================

  /**
   * Returns all stored versions for an answer string ID.
   *
   * @param answerStringId answer string ID
   * @return all known versions for the answer string ID
   */
  public Collection<AnswerVersion> getAnswerVersionListByAnswerStringId(String answerStringId) {
    return answerStringIdToAnswerVersionMap.get(answerStringId);
  }

  /**
   * Returns the answer version valid for the requested version.
   *
   * @param answerStringId answer string ID
   * @param version target LOINC version
   * @return latest answer version valid at the requested version
   */
  public Optional<AnswerVersion> getAnswerVersion(String answerStringId, LoincVersion version) {
    return answerStringIdToAnswerVersionMap.get(answerStringId).stream()
        .filter(
            conceptVersion -> conceptVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching answer version
        .max(Comparator.comparing(AnswerVersion::getValidFromVersion));
  }

  // ==============================================================================
  // Answer list related operations
  // ==============================================================================

  /**
   * Returns all stored versions for an answer list ID.
   *
   * @param answerListId answer list ID
   * @return all known versions for the answer list
   */
  public Collection<AnswerListVersion> getAnswerListVersionListByAnswerListId(String answerListId) {
    return answerListIdToAnswerListVersionMap.get(answerListId);
  }

  /**
   * Returns the answer list version valid for the requested version.
   *
   * @param answerListId answer list ID
   * @param version target LOINC version
   * @return latest answer list version valid at the requested version
   */
  public Optional<AnswerListVersion> getAnswerListVersion(
      String answerListId, LoincVersion version) {

    return answerListIdToAnswerListVersionMap.get(answerListId).stream()
        .filter(
            conceptVersion -> conceptVersion.getValidFromVersion() <= version.getVersionNumber())
        // The last element in the stream is the matching answer list version
        .max(Comparator.comparing(AnswerListVersion::getValidFromVersion));
  }
}
