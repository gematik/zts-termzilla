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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import de.gematik.zts.termzilla.terminologies.loinc.exceptions.LoincInitializationException;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LinguisticVariantVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LoincPartLinkVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link LoincInMemoryRepresentation}. */
@DisplayName("LoincInMemoryRepresentation Tests")
class LoincInMemoryRepresentationTest {

  private static final List<String> LANGUAGES = List.of("de-DE");
  private static final String CSV_HEADER_ANSWERLIST =
      "\"AnswerListId\",\"AnswerListName\",\"AnswerStringId\",\"DisplayText\"\n";
  private static final String CSV_HEADER_LINGUISTIC_VARIANT =
      "\"LOINC_NUM\",\"COMPONENT\",\"PROPERTY\",\"TIME_ASPCT\",\"SYSTEM\",\"SCALE_TYP\",\"METHOD_TYP\",\"CLASS\",\"SHORTNAME\",\"LONG_COMMON_NAME\",\"RELATEDNAMES2\",\"LinguisticVariantDisplayName\"\n";

  private LoincInMemoryRepresentation createSubject(Path tempDir)
      throws InvalidConfigurationException {
    Path releaseCollectionDirectory = tempDir.resolve("release-collection");
    Path workingDirectory = tempDir.resolve("work");
    try {
      Files.createDirectories(releaseCollectionDirectory);
      Files.createDirectories(workingDirectory);
      createMinimalReleaseZip(releaseCollectionDirectory.resolve("Loinc_2.66.zip"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    LoincConfigProperties properties =
        LoincInMemoryRepresentationTestDataHelper.configProperties(
            releaseCollectionDirectory.toString(), workingDirectory, LANGUAGES);

    return new LoincInMemoryRepresentation(properties);
  }

  private void createMinimalReleaseZip(Path zipPath) throws Exception {
    createReleaseZip(zipPath, CSV_HEADER_ANSWERLIST);
  }

  private LoincInMemoryRepresentation createSubjectWithReleaseFiles(
      Path tempDir, Map<String, String> zipFileToAnswerListCsvContent)
      throws InvalidConfigurationException {
    Path releaseCollectionDirectory = tempDir.resolve("release-collection");
    Path workingDirectory = tempDir.resolve("work");
    try {
      Files.createDirectories(releaseCollectionDirectory);
      Files.createDirectories(workingDirectory);
      for (Map.Entry<String, String> entry : zipFileToAnswerListCsvContent.entrySet()) {
        createReleaseZip(releaseCollectionDirectory.resolve(entry.getKey()), entry.getValue());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    LoincConfigProperties properties =
        LoincInMemoryRepresentationTestDataHelper.configProperties(
            releaseCollectionDirectory.toString(), workingDirectory, LANGUAGES);

    return new LoincInMemoryRepresentation(properties);
  }

  private LoincInMemoryRepresentation createSubjectWithReleaseFilesAndLinguisticVariants(
      Path tempDir, Map<String, String> zipFileToLinguisticVariantCsvContent)
      throws InvalidConfigurationException {
    Path releaseCollectionDirectory = tempDir.resolve("release-collection");
    Path workingDirectory = tempDir.resolve("work");
    try {
      Files.createDirectories(releaseCollectionDirectory);
      Files.createDirectories(workingDirectory);
      for (Map.Entry<String, String> entry : zipFileToLinguisticVariantCsvContent.entrySet()) {
        createReleaseZip(
            releaseCollectionDirectory.resolve(entry.getKey()),
            CSV_HEADER_ANSWERLIST,
            entry.getValue());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    LoincConfigProperties properties =
        LoincInMemoryRepresentationTestDataHelper.configProperties(
            releaseCollectionDirectory.toString(), workingDirectory, LANGUAGES);

    return new LoincInMemoryRepresentation(properties);
  }

  private void createReleaseZip(Path zipPath, String answerListCsvContent) throws Exception {
    createReleaseZip(zipPath, answerListCsvContent, CSV_HEADER_LINGUISTIC_VARIANT);
  }

  private void createReleaseZip(
      Path zipPath, String answerListCsvContent, String linguisticVariantCsvContent)
      throws Exception {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      addZipEntry(zipOutputStream, "Loinc.csv", "\"LOINC_NUM\",\"VersionLastChanged\"\n");
      addZipEntry(
          zipOutputStream,
          "Part.csv",
          "\"PartNumber\",\"PartTypeName\",\"PartName\",\"PartDisplayName\",\"Status\"\n");
      addZipEntry(
          zipOutputStream,
          "ComponentHierarchyBySystem.csv",
          "\"PATH_TO_ROOT\",\"SEQUENCE\",\"IMMEDIATE_PARENT\",\"CODE\",\"CODE_TEXT\"\n");
      addZipEntry(
          zipOutputStream,
          "PartChangeSnapshot.csv",
          "\"VersionEffective\",\"PartNumber\",\"PROPERTY\",\"ValuePrior\",\"ValueCurrent\",\"ChangeReason\"\n");
      addZipEntry(
          zipOutputStream,
          "LoincPartLink_Primary.csv",
          "\"LoincNumber\",\"PartNumber\",\"PartTypeName\"\n");
      addZipEntry(zipOutputStream, "AnswerList.csv", answerListCsvContent);
      addZipEntry(zipOutputStream, "deDE15LinguisticVariant.csv", linguisticVariantCsvContent);
    }
  }

  private void addZipEntry(ZipOutputStream zipOutputStream, String fileName, String content)
      throws Exception {
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOutputStream.putNextEntry(zipEntry);
    zipOutputStream.write(content.getBytes());
    zipOutputStream.closeEntry();
  }

  private String answerListCsvRow(
      String answerListId, String answerListName, String answerStringId, String displayText) {
    return "\""
        + answerListId
        + "\",\""
        + answerListName
        + "\",\""
        + answerStringId
        + "\",\""
        + displayText
        + "\"\n";
  }

  private String linguisticVariantCsvRow(String loincNumber, String longCommonName) {
    return "\""
        + loincNumber
        + "\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\""
        + longCommonName
        + "\",\"\",\"\"\n";
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw InvalidConfigurationException when release directory does not exist")
    void shouldThrowWhenReleaseDirectoryDoesNotExist(@TempDir Path tempDir) {
      // GIVEN
      Path workingDirectory = tempDir.resolve("work");
      LoincConfigProperties properties =
          LoincInMemoryRepresentationTestDataHelper.configProperties(
              tempDir.resolve("missing-release-collection").toString(),
              workingDirectory,
              LANGUAGES);

      // WHEN / THEN
      assertThatThrownBy(() -> new LoincInMemoryRepresentation(properties))
          .as("A missing release collection directory must fail during initialization")
          .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    @DisplayName(
        "Should throw LoincInitializationException when release directory has no zip files")
    void shouldThrowWhenReleaseDirectoryContainsNoReleaseZipFiles(@TempDir Path tempDir) {
      // GIVEN
      Path releaseCollectionDirectory = tempDir.resolve("release-collection");
      Path workingDirectory = tempDir.resolve("work");
      try {
        Files.createDirectories(releaseCollectionDirectory);
        Files.createDirectories(workingDirectory);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      LoincConfigProperties properties =
          LoincInMemoryRepresentationTestDataHelper.configProperties(
              releaseCollectionDirectory.toString(), workingDirectory, LANGUAGES);

      // WHEN / THEN
      assertThatThrownBy(() -> new LoincInMemoryRepresentation(properties))
          .as("An existing but empty release collection must fail fast")
          .isInstanceOf(LoincInitializationException.class)
          .hasMessageContaining("No LOINC release files found in given location");
    }
  }

  @Nested
  @DisplayName("addLoincVersion() and getLoincVersion() Tests")
  class LoincVersionMethodsTests {

    @Test
    @DisplayName("Should return the added version by version string")
    void shouldReturnAddedVersionByVersionString(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      LoincVersion loincVersion =
          LoincInMemoryRepresentationTestDataHelper.loincVersion(266, "2.66");

      // WHEN
      subject.addLoincVersion(loincVersion);
      Optional<LoincVersion> result = subject.getLoincVersion("2.66");

      // THEN
      assertThat(result)
          .as("Added LOINC version should be retrievable by its version string")
          .contains(loincVersion);
    }
  }

  @Nested
  @DisplayName("addConcept() and concept getter Tests")
  class ConceptMethodsTests {

    @Test
    @DisplayName("Should avoid duplicate concepts with same LOINC number and VersionLastChanged")
    void shouldAvoidDuplicateConcepts(@TempDir Path tempDir) throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      ConceptVersion conceptVersion =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("1234-5", "2.66", 266);

      // WHEN
      subject.addConcept(conceptVersion);
      subject.addConcept(conceptVersion);
      Collection<ConceptVersion> concepts = subject.getConceptVersionListByLoincNumber("1234-5");

      // THEN
      assertThat(concepts)
          .as("Concepts with same VersionLastChanged are treated as duplicates")
          .containsExactly(conceptVersion);
    }

    @Test
    @DisplayName("Should return latest concept version not newer than requested LOINC version")
    void shouldReturnLatestConceptVersionAtOrBeforeRequestedVersion(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      ConceptVersion concept266 =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("1234-5", "2.66", 266);
      ConceptVersion concept267 =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("1234-5", "2.67", 267);
      subject.addConcept(concept266);
      subject.addConcept(concept267);
      LoincVersion targetVersion =
          LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67");

      // WHEN
      Optional<ConceptVersion> result = subject.getConceptVersion("1234-5", targetVersion);

      // THEN
      assertThat(result)
          .as("The concept with highest validFromVersion <= requested version should be selected")
          .contains(concept267);
    }

    @Test
    @DisplayName("Should return one latest concept per LOINC number for requested version")
    void shouldReturnLatestConceptPerLoincNumberInConceptVersionList(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      ConceptVersion conceptA266 =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("1000-1", "2.66", 266);
      ConceptVersion conceptA267 =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("1000-1", "2.67", 267);
      ConceptVersion conceptB266 =
          LoincInMemoryRepresentationTestDataHelper.conceptVersion("2000-2", "2.66", 266);
      subject.addConcept(conceptA266);
      subject.addConcept(conceptA267);
      subject.addConcept(conceptB266);

      // WHEN
      List<ConceptVersion> result =
          subject.getConceptVersionList(
              LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));

      // THEN
      assertThat(result)
          .as("The list should contain only latest concepts per LOINC number")
          .containsExactlyInAnyOrder(conceptA267, conceptB266);
    }
  }

  @Nested
  @DisplayName("addPart(), addPartHierarchy(), addPartChangeSnapshot() and part getter Tests")
  class PartMethodsTests {

    @Test
    @DisplayName("Should avoid duplicate parts for identical part content")
    void shouldAvoidDuplicateParts(@TempDir Path tempDir) throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      PartVersion part =
          LoincInMemoryRepresentationTestDataHelper.partVersion(
              "LP1-1", "COMPONENT", "Part Name", "Part Display", "ACTIVE", 266);

      // WHEN
      subject.addPart(part);
      subject.addPart(part);
      Collection<PartVersion> parts = subject.getPartVersionListByPartNumber("LP1-1");

      // THEN
      assertThat(parts).as("Equivalent part objects should be stored once").containsExactly(part);
    }

    @Test
    @DisplayName("Should mark hierarchy-only parts as NOT_IN_PART_FILE")
    void shouldMarkHierarchyOnlyPartAsNotInPartFile(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      PartVersion hierarchyPart =
          LoincInMemoryRepresentationTestDataHelper.partVersion(
              "LP2-2", "IGNORED", "Hierarchy Name", "Hierarchy Display", "ACTIVE", 266);

      // WHEN
      subject.addPartHierarchy(hierarchyPart);
      Optional<PartVersion> result =
          subject.getPartVersionByPartNumber(
              "LP2-2", LoincInMemoryRepresentationTestDataHelper.loincVersion(266, "2.66"));

      // THEN
      assertThat(result)
          .as("Parts only known from hierarchy must be tagged with NOT_IN_PART_FILE")
          .isPresent()
          .get()
          .extracting(PartVersion::getPartTypeName)
          .isEqualTo(LoincConstants.LOINC_PARTTYPENAME_NOT_IN_PART_FILE);
    }

    @Test
    @DisplayName("Should add change snapshot entry when status changed and keep name metadata")
    void shouldAddPartFromChangeSnapshotWhenStatusChanges(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      PartVersion activePart =
          LoincInMemoryRepresentationTestDataHelper.partVersion(
              "LP3-3", "COMPONENT", "Stable Name", "Stable Display", "ACTIVE", 266);
      subject.addPart(activePart);
      PartVersion inactiveSnapshot =
          LoincInMemoryRepresentationTestDataHelper.partVersion(
              "LP3-3", "UNKNOWN", "Changed Name", "Changed Display", "INACTIVE", 267);

      // WHEN
      subject.addPartChangeSnapshot(inactiveSnapshot);
      Optional<PartVersion> result =
          subject.getPartVersionByPartNumber(
              "LP3-3", LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));

      // THEN
      assertThat(result)
          .as("Snapshot with changed status should be added and carry forward old name/type fields")
          .isPresent()
          .get()
          .satisfies(
              partVersion ->
                  assertThat(partVersion)
                      .extracting(
                          PartVersion::getPartTypeName,
                          PartVersion::getPartName,
                          PartVersion::getPartDisplayName,
                          PartVersion::getStatus)
                      .containsExactly("COMPONENT", "Stable Name", "Stable Display", "INACTIVE"));
    }

    @Test
    @DisplayName("Should return part by part name for requested version")
    void shouldReturnPartByPartName(@TempDir Path tempDir) throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      PartVersion partVersion =
          LoincInMemoryRepresentationTestDataHelper.partVersion(
              "LP4-4", "SYSTEM", "Part Name Query", "Part Display", "ACTIVE", 266);
      subject.addPart(partVersion);

      // WHEN
      Optional<PartVersion> result =
          subject.getPartVersionByPartName(
              "Part Name Query",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(266, "2.66"));

      // THEN
      assertThat(result)
          .as("Part lookup by name should return the matching versioned part")
          .contains(partVersion);
    }
  }

  @Nested
  @DisplayName("addLoincPartLink() Tests")
  class AddLoincPartLinkTests {

    @ParameterizedTest
    @DisplayName("Should route link to matching type-specific maps")
    @CsvSource({
      "COMPONENT,1,0,0,0,0",
      "PROPERTY,0,1,0,0,0",
      "TIME,0,0,1,0,0",
      "SYSTEM,0,0,0,1,0",
      "SCALE,0,0,0,0,1"
    })
    void shouldRouteToTypeSpecificMaps(
        String partType,
        int expectedComponent,
        int expectedProperty,
        int expectedTime,
        int expectedSystem,
        int expectedScale,
        @TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      LoincPartLinkVersion linkVersion =
          LoincInMemoryRepresentationTestDataHelper.loincPartLinkVersion(
              "1234-5", "LP10-1", partType, 266);

      // WHEN
      subject.addLoincPartLink(linkVersion);

      // THEN
      assertThat(subject.getComponentLoincNumberToLoincPartLinkVersionMap().get("1234-5"))
          .as("Component map size should match expected routing result")
          .hasSize(expectedComponent);
      assertThat(subject.getPropertyLoincNumberToLoincPartLinkVersionMap().get("1234-5"))
          .as("Property map size should match expected routing result")
          .hasSize(expectedProperty);
      assertThat(subject.getTimeLoincNumberToLoincPartLinkVersionMap().get("1234-5"))
          .as("Time map size should match expected routing result")
          .hasSize(expectedTime);
      assertThat(subject.getSystemLoincNumberToLoincPartLinkVersionMap().get("1234-5"))
          .as("System map size should match expected routing result")
          .hasSize(expectedSystem);
      assertThat(subject.getScaleLoincNumberToLoincPartLinkVersionMap().get("1234-5"))
          .as("Scale map size should match expected routing result")
          .hasSize(expectedScale);
    }

    @Test
    @DisplayName("Should add link to both maps and avoid duplicates in addLoincPartLinkComponent")
    void shouldAddAndDeduplicateInAddLoincPartLinkComponent(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      LoincPartLinkVersion loincPartLinkVersion =
          LoincInMemoryRepresentationTestDataHelper.loincPartLinkVersion(
              "1234-5", "LP20-2", "COMPONENT", 266);
      MultiValuedMap<String, LoincPartLinkVersion> mapLoincNumber = new ArrayListValuedHashMap<>();
      MultiValuedMap<String, LoincPartLinkVersion> mapPartNumber = new ArrayListValuedHashMap<>();

      // WHEN
      subject.addLoincPartLinkComponent(loincPartLinkVersion, mapLoincNumber, mapPartNumber);
      subject.addLoincPartLinkComponent(loincPartLinkVersion, mapLoincNumber, mapPartNumber);

      // THEN
      assertThat(mapLoincNumber.get("1234-5"))
          .as("Loinc-number map should contain the link exactly once")
          .containsExactly(loincPartLinkVersion);
      assertThat(mapPartNumber.get("LP20-2"))
          .as("Part-number map should contain the link exactly once")
          .containsExactly(loincPartLinkVersion);
    }
  }

  @Nested
  @DisplayName("addLinguisticVariant() and linguistic variant getter Tests")
  class LinguisticVariantMethodsTests {

    @Test
    @DisplayName("Should return latest linguistic variant for language at requested version")
    void shouldReturnLatestLinguisticVariant(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      LinguisticVariantVersion de266 =
          LoincInMemoryRepresentationTestDataHelper.linguisticVariantVersion(
              "1234-5", "de-DE", 266, "Langname 266");
      LinguisticVariantVersion de267 =
          LoincInMemoryRepresentationTestDataHelper.linguisticVariantVersion(
              "1234-5", "de-DE", 267, "Langname 267");
      subject.addLinguisticVariant(de266);
      subject.addLinguisticVariant(de267);

      // WHEN
      Optional<LinguisticVariantVersion> result =
          subject.getLinguisticVariantVersion(
              "1234-5",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"),
              "de-DE");

      // THEN
      assertThat(result)
          .as("Latest linguistic variant matching language and version should be returned")
          .contains(de267);
    }

    @Test
    @DisplayName("Should return all linguistic variants for a LOINC number")
    void shouldReturnAllLinguisticVariantsByLoincNumber(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      LinguisticVariantVersion deVariant =
          LoincInMemoryRepresentationTestDataHelper.linguisticVariantVersion(
              "3333-3", "de-DE", 266, "Langname DE");
      LinguisticVariantVersion enVariant =
          LoincInMemoryRepresentationTestDataHelper.linguisticVariantVersion(
              "3333-3", "en-US", 267, "Long Name EN");
      subject.addLinguisticVariant(deVariant);
      subject.addLinguisticVariant(enVariant);

      // WHEN
      Collection<LinguisticVariantVersion> result =
          subject.getLinguisticVariantVersionListByLoincNumber("3333-3");

      // THEN
      assertThat(result)
          .as("All linguistic variants for the same LOINC number should be returned")
          .containsExactlyInAnyOrder(deVariant, enVariant);
    }

    @Test
    @DisplayName("Should return empty for removed linguistic variant in newer version")
    void shouldReturnEmptyForRemovedLinguisticVariant(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      Map<String, String> linguisticVariantContentByZip = new LinkedHashMap<>();
      linguisticVariantContentByZip.put(
          "Loinc_2.66.zip",
          CSV_HEADER_LINGUISTIC_VARIANT + linguisticVariantCsvRow("1234-5", "Langname 266"));
      linguisticVariantContentByZip.put("Loinc_2.67.zip", CSV_HEADER_LINGUISTIC_VARIANT);
      LoincInMemoryRepresentation subject =
          createSubjectWithReleaseFilesAndLinguisticVariants(
              tempDir, linguisticVariantContentByZip);

      // WHEN
      Optional<LinguisticVariantVersion> result266 =
          subject.getLinguisticVariantVersion(
              "1234-5",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(266, "2.66"),
              "de-DE");
      Optional<LinguisticVariantVersion> result267 =
          subject.getLinguisticVariantVersion(
              "1234-5",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"),
              "de-DE");

      // THEN
      assertThat(result266)
          .as("Translation should still be available for the version where it exists")
          .isPresent()
          .get()
          .extracting(LinguisticVariantVersion::getLongCommonName)
          .isEqualTo("Langname 266");
      assertThat(result267)
          .as("Translation should be unavailable after it was removed in a later release")
          .isEmpty();
    }

    @Test
    @DisplayName("Should return translation again when removed translation is reintroduced")
    void shouldReturnReintroducedLinguisticVariant(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      Map<String, String> linguisticVariantContentByZip = new LinkedHashMap<>();
      linguisticVariantContentByZip.put(
          "Loinc_2.66.zip",
          CSV_HEADER_LINGUISTIC_VARIANT + linguisticVariantCsvRow("1234-5", "Langname 266"));
      linguisticVariantContentByZip.put("Loinc_2.67.zip", CSV_HEADER_LINGUISTIC_VARIANT);
      linguisticVariantContentByZip.put(
          "Loinc_2.68.zip",
          CSV_HEADER_LINGUISTIC_VARIANT + linguisticVariantCsvRow("1234-5", "Langname 268"));
      LoincInMemoryRepresentation subject =
          createSubjectWithReleaseFilesAndLinguisticVariants(
              tempDir, linguisticVariantContentByZip);

      // WHEN
      Optional<LinguisticVariantVersion> result267 =
          subject.getLinguisticVariantVersion(
              "1234-5",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"),
              "de-DE");
      Optional<LinguisticVariantVersion> result268 =
          subject.getLinguisticVariantVersion(
              "1234-5",
              LoincInMemoryRepresentationTestDataHelper.loincVersion(268, "2.68"),
              "de-DE");

      // THEN
      assertThat(result267)
          .as("Translation should not be available in the removed version")
          .isEmpty();
      assertThat(result268)
          .as("Translation should be available again after reintroduction")
          .isPresent()
          .get()
          .extracting(LinguisticVariantVersion::getLongCommonName)
          .isEqualTo("Langname 268");
    }
  }

  @Nested
  @DisplayName("addAnswer() and answer getter Tests")
  class AnswerMethodsTests {

    @Test
    @DisplayName("Should keep answer history and return latest answer at requested version")
    void shouldReturnLatestAnswerVersion(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      AnswerVersion answer266 =
          LoincInMemoryRepresentationTestDataHelper.answerVersion(
              "LA1-1", "Display A", "ACTIVE", 266);
      AnswerVersion answer267 =
          LoincInMemoryRepresentationTestDataHelper.answerVersion(
              "LA1-1", "Display B", "ACTIVE", 267);
      subject.addAnswer(answer266);
      subject.addAnswer(answer267);

      // WHEN
      Optional<AnswerVersion> result =
          subject.getAnswerVersion(
              "LA1-1", LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));

      // THEN
      assertThat(result)
          .as("Answer with highest validFromVersion <= requested version should be selected")
          .contains(answer267);
    }

    @Test
    @DisplayName("Should return full answer history by answer string id")
    void shouldReturnAnswerHistoryByAnswerStringId(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      AnswerVersion answerVersion266 =
          LoincInMemoryRepresentationTestDataHelper.answerVersion(
              "LA9-9", "Display 266", "ACTIVE", 266);
      AnswerVersion answerVersion267 =
          LoincInMemoryRepresentationTestDataHelper.answerVersion(
              "LA9-9", "Display 267", "ACTIVE", 267);
      subject.addAnswer(answerVersion266);
      subject.addAnswer(answerVersion267);

      // WHEN
      Collection<AnswerVersion> result = subject.getAnswerVersionListByAnswerStringId("LA9-9");

      // THEN
      assertThat(result)
          .as("Answer history should contain all stored versions for the answer id")
          .containsExactlyInAnyOrder(answerVersion266, answerVersion267);
    }
  }

  @Nested
  @DisplayName("addAnswerListsForCurrentVersion() and answer list getter Tests")
  class AnswerListMethodsTests {

    @Test
    @DisplayName("Should add changed answer list version and keep previous version")
    void shouldAddChangedAnswerListVersion(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      AnswerListVersion baseVersion =
          LoincInMemoryRepresentationTestDataHelper.answerListVersion(
              "LL1-1", "List Name", "ACTIVE", 266, Set.of("LA1-1"));
      subject.addAnswerList(baseVersion);

      AnswerListVersion changedVersion =
          LoincInMemoryRepresentationTestDataHelper.answerListVersion(
              "LL1-1", "List Name", "ACTIVE", 267, Set.of("LA1-1", "LA2-2"));
      Map<String, AnswerListVersion> answerListMap = Map.of("LL1-1", changedVersion);

      // WHEN
      subject.addAnswerListsForCurrentVersion(answerListMap);
      Collection<AnswerListVersion> versions =
          subject.getAnswerListVersionListByAnswerListId("LL1-1");
      Optional<AnswerListVersion> selected =
          subject.getAnswerListVersion(
              "LL1-1", LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));

      // THEN
      assertThat(versions)
          .as("Changed answer list content should create an additional version entry")
          .hasSize(2);
      assertThat(selected)
          .as("Latest version should be selected for requested LOINC version")
          .contains(changedVersion);
    }

    @Test
    @DisplayName("Should not add duplicate answer list version when latest is unchanged")
    void shouldNotAddDuplicateAnswerListVersion(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      LoincInMemoryRepresentation subject = createSubject(tempDir);
      AnswerListVersion version =
          LoincInMemoryRepresentationTestDataHelper.answerListVersion(
              "LL2-2", "List Name", "ACTIVE", 266, Set.of("LA1-1"));
      subject.addAnswerList(version);
      Map<String, AnswerListVersion> answerListMap = Map.of("LL2-2", version);

      // WHEN
      subject.addAnswerListsForCurrentVersion(answerListMap);
      Collection<AnswerListVersion> versions =
          subject.getAnswerListVersionListByAnswerListId("LL2-2");

      // THEN
      assertThat(versions)
          .as("Unchanged answer list version must not be duplicated")
          .containsExactly(version);
    }
  }

  @Nested
  @DisplayName("Initialization edge case tests for answer and answer list inactivation")
  class InactivationDuringLoadingTests {

    @Test
    @DisplayName("Should mark removed answers and answer lists as INACTIVE in the next release")
    void shouldMarkRemovedAnswerAndAnswerListAsInactive(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      String answerListCsv266 =
          CSV_HEADER_ANSWERLIST
              + answerListCsvRow("LL1-1", "Answer List One", "LA1-1", "Display One");
      String answerListCsv267 = CSV_HEADER_ANSWERLIST;
      Map<String, String> releases = new LinkedHashMap<>();
      releases.put("Loinc_2.66.zip", answerListCsv266);
      releases.put("Loinc_2.67.zip", answerListCsv267);

      // WHEN
      LoincInMemoryRepresentation subject = createSubjectWithReleaseFiles(tempDir, releases);
      Collection<AnswerVersion> answerHistory =
          subject.getAnswerVersionListByAnswerStringId("LA1-1");
      Collection<AnswerListVersion> answerListHistory =
          subject.getAnswerListVersionListByAnswerListId("LL1-1");
      Optional<AnswerVersion> latestAnswer =
          subject.getAnswerVersion(
              "LA1-1", LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));
      Optional<AnswerListVersion> latestAnswerList =
          subject.getAnswerListVersion(
              "LL1-1", LoincInMemoryRepresentationTestDataHelper.loincVersion(267, "2.67"));

      // THEN
      assertThat(answerHistory)
          .as("Removed answer must keep active history and add one INACTIVE version")
          .hasSize(2);
      assertThat(answerHistory.stream().map(AnswerVersion::getStatus).collect(Collectors.toSet()))
          .as("Answer history should contain both ACTIVE and INACTIVE states")
          .containsExactlyInAnyOrder("ACTIVE", "INACTIVE");
      assertThat(latestAnswer)
          .as("Latest answer version in 2.67 should be INACTIVE")
          .isPresent()
          .get()
          .satisfies(
              answerVersion ->
                  assertThat(answerVersion)
                      .extracting(
                          AnswerVersion::getStatus,
                          AnswerVersion::getValidFromVersion,
                          AnswerVersion::getAnswerStringId,
                          AnswerVersion::getDisplayText)
                      .containsExactly("INACTIVE", 267, "LA1-1", "Display One"));

      assertThat(answerListHistory)
          .as("Removed answer list must keep active history and add one INACTIVE version")
          .hasSize(2);
      assertThat(
              answerListHistory.stream()
                  .map(AnswerListVersion::getStatus)
                  .collect(Collectors.toSet()))
          .as("Answer list history should contain both ACTIVE and INACTIVE states")
          .containsExactlyInAnyOrder("ACTIVE", "INACTIVE");
      assertThat(latestAnswerList)
          .as("Latest answer list version in 2.67 should be INACTIVE")
          .isPresent()
          .get()
          .satisfies(
              answerListVersion ->
                  assertThat(answerListVersion)
                      .extracting(
                          AnswerListVersion::getStatus,
                          AnswerListVersion::getValidFromVersion,
                          AnswerListVersion::getAnswerListId,
                          AnswerListVersion::getAnswerListName)
                      .containsExactly("INACTIVE", 267, "LL1-1", "Answer List One"));
    }

    @Test
    @DisplayName(
        "Should not create additional INACTIVE entries when already inactive in prior release")
    void shouldNotCreateRepeatedInactiveEntries(@TempDir Path tempDir)
        throws InvalidConfigurationException {
      // GIVEN
      String answerListCsv266 =
          CSV_HEADER_ANSWERLIST
              + answerListCsvRow("LL9-9", "Answer List Nine", "LA9-9", "Display Nine");
      String answerListCsv267 = CSV_HEADER_ANSWERLIST;
      String answerListCsv268 = CSV_HEADER_ANSWERLIST;
      Map<String, String> releases = new LinkedHashMap<>();
      releases.put("Loinc_2.66.zip", answerListCsv266);
      releases.put("Loinc_2.67.zip", answerListCsv267);
      releases.put("Loinc_2.68.zip", answerListCsv268);

      // WHEN
      LoincInMemoryRepresentation subject = createSubjectWithReleaseFiles(tempDir, releases);
      Collection<AnswerVersion> answerHistory =
          subject.getAnswerVersionListByAnswerStringId("LA9-9");
      Collection<AnswerListVersion> answerListHistory =
          subject.getAnswerListVersionListByAnswerListId("LL9-9");

      // THEN
      assertThat(answerHistory)
          .as("Only one INACTIVE answer version should be added even if removal persists")
          .hasSize(2);
      assertThat(
              answerHistory.stream()
                  .filter(answerVersion -> answerVersion.getStatus().equals("INACTIVE"))
                  .map(AnswerVersion::getValidFromVersion)
                  .collect(Collectors.toSet()))
          .as("INACTIVE answer version should be created exactly once at first removal")
          .containsExactly(267);

      assertThat(answerListHistory)
          .as("Only one INACTIVE answer list version should be added even if removal persists")
          .hasSize(2);
      assertThat(
              answerListHistory.stream()
                  .filter(answerListVersion -> answerListVersion.getStatus().equals("INACTIVE"))
                  .map(AnswerListVersion::getValidFromVersion)
                  .collect(Collectors.toSet()))
          .as("INACTIVE answer list version should be created exactly once at first removal")
          .containsExactly(267);
    }
  }
}
