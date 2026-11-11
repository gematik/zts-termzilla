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
package de.gematik.zts.termzilla.terminologies.snomed.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link SnomedReleaseFileHelper}.
 *
 * <p>All methods under test are static, so no instantiation or mocking is required. File system
 * interactions are isolated via {@link TempDir}.
 */
@DisplayName("SnomedReleaseFileHelper Tests")
class SnomedReleaseFileHelperTest {

  @TempDir Path tempDir;

  // ---------------------------------------------------------------------------
  // Test data helpers
  // ---------------------------------------------------------------------------

  /** Creates a plain file at {@code directory/fileName} with the given content. */
  private static Path createFile(Path directory, String fileName, String content)
      throws IOException {
    Path filePath = directory.resolve(fileName);
    Files.writeString(filePath, content);
    return filePath;
  }

  /** Creates a ZIP file at {@code directory/fileName} with one entry. */
  private static Path createZipFile(
      Path directory, String fileName, String entryName, String entryContent) throws IOException {
    Path zipPath = directory.resolve(fileName);
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(entryContent.getBytes());
      zos.closeEntry();
    }
    return zipPath;
  }

  /** Creates a ZIP file that contains a directory entry followed by a file entry inside it. */
  private static Path createZipWithDirectory(Path directory, String fileName) throws IOException {
    Path zipPath = directory.resolve(fileName);
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      zos.putNextEntry(new ZipEntry("Full/"));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("Full/sct2_Concept_Full_INT_20240101.txt"));
      zos.write("concept-data".getBytes());
      zos.closeEntry();
    }
    return zipPath;
  }

  // ===========================================================================
  // calculateReleaseFileHash
  // ===========================================================================

  @Nested
  @DisplayName("calculateReleaseFileHash Tests")
  class CalculateReleaseFileHashTests {

    @Test
    @DisplayName("Should throw SnomedDataLoadingException when file does not exist")
    void shouldThrowExceptionWhenFileDoesNotExist() {
      // GIVEN
      Path nonExistent = tempDir.resolve("missing.zip");

      // WHEN / THEN
      assertThatThrownBy(() -> SnomedReleaseFileHelper.calculateReleaseFileHash(nonExistent))
          .isInstanceOf(SnomedDataLoadingException.class);
    }

    @Test
    @DisplayName("Should return a valid SHA-256 hash for an existing file")
    void shouldReturnValidHashForExistingFile() throws IOException {
      // GIVEN
      Path snomedFile = createFile(tempDir, "snomed-release.zip", "snomed-content");

      // WHEN
      String hash = SnomedReleaseFileHelper.calculateReleaseFileHash(snomedFile);

      // THEN
      assertThat(hash)
          .as("Hash should be a 64-character lowercase hex string")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should return the correct SHA-256 hash matching DigestUtils")
    void shouldReturnCorrectSha256Hash() throws IOException {
      // GIVEN
      Path snomedFile = createFile(tempDir, "snomed-release.zip", "snomed-content");
      String expectedHash = DigestUtils.sha256Hex("snomed-content");

      // WHEN
      String hash = SnomedReleaseFileHelper.calculateReleaseFileHash(snomedFile);

      // THEN
      assertThat(hash)
          .as("Computed hash must match the SHA-256 digest of the file content")
          .isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Should produce a deterministic hash for the same file")
    void shouldProduceDeterministicHash() throws IOException {
      // GIVEN
      Path snomedFile = createFile(tempDir, "snomed-release.zip", "snomed-content");

      // WHEN
      String firstHash = SnomedReleaseFileHelper.calculateReleaseFileHash(snomedFile);
      String secondHash = SnomedReleaseFileHelper.calculateReleaseFileHash(snomedFile);

      // THEN
      assertThat(firstHash)
          .as("Hash must be identical across repeated calls for the same file")
          .isEqualTo(secondHash);
    }

    @Test
    @DisplayName("Should produce different hashes for files with different content")
    void shouldProduceDifferentHashesForDifferentContent() throws IOException {
      // GIVEN
      Path fileA = createFile(tempDir, "snomed-a.zip", "content-version-a");
      Path fileB = createFile(tempDir, "snomed-b.zip", "content-version-b");

      // WHEN
      String hashA = SnomedReleaseFileHelper.calculateReleaseFileHash(fileA);
      String hashB = SnomedReleaseFileHelper.calculateReleaseFileHash(fileB);

      // THEN
      assertThat(hashA)
          .as("Files with different content must produce different hashes")
          .isNotEqualTo(hashB);
    }
  }

  // ===========================================================================
  // getSingleReleaseFilePath
  // ===========================================================================

  @Nested
  @DisplayName("getSingleReleaseFilePath Tests")
  class GetSingleReleaseFilePathTests {

    @Test
    @DisplayName("Should throw SnomedDataLoadingException when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
      // GIVEN
      Path nonExistent = tempDir.resolve("non-existent");

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  SnomedReleaseFileHelper.getSingleReleaseFilePath(
                      nonExistent, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN))
          .isInstanceOf(SnomedDataLoadingException.class)
          .hasMessageContaining(nonExistent.toString());
    }

    @Test
    @DisplayName("Should return null when no file matches the pattern")
    void shouldReturnNullWhenNoMatchingFile() {
      // GIVEN
      // tempDir is empty

      // WHEN
      Path result =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return the path of the matching file")
    void shouldReturnPathOfMatchingFile() throws IOException {
      // GIVEN
      Path conceptsFile = Files.createFile(tempDir.resolve("sct2_Concept_Full_INT_20240101.txt"));

      // WHEN
      Path result =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result)
          .as("Should return the path of the matched concepts file")
          .isEqualTo(conceptsFile);
    }

    @Test
    @DisplayName("Should find matching files in subdirectories")
    void shouldFindFilesInSubdirectories() throws IOException {
      // GIVEN
      Path subDir = tempDir.resolve("Full");
      Files.createDirectories(subDir);
      Path conceptsFile = Files.createFile(subDir.resolve("sct2_Concept_Full_INT_20240101.txt"));

      // WHEN
      Path result =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result)
          .as("Recursive search must find the concepts file in a subdirectory")
          .isEqualTo(conceptsFile);
    }

    @Test
    @DisplayName("Should match by file name only, not by full path")
    void shouldMatchByFileNameOnly() throws IOException {
      // GIVEN
      // A directory whose name matches the pattern, containing an unrelated file
      Path dirNamedAsPattern = tempDir.resolve("sct2_Concept_Full_INT_20240101.txt");
      Files.createDirectories(dirNamedAsPattern);
      Files.createFile(dirNamedAsPattern.resolve("unrelated.txt"));

      // WHEN
      Path result =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, SnomedReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result)
          .as(
              "A directory whose name matches the pattern must not be returned; "
                  + "files inside it must also not match via the directory name")
          .isNull();
    }
  }

  // ===========================================================================
  // unzip
  // ===========================================================================

  @Nested
  @DisplayName("unzip Tests")
  class UnzipTests {

    @Test
    @DisplayName("Should throw InvalidConfigurationException when ZIP file does not exist")
    void shouldThrowExceptionWhenZipFileDoesNotExist() {
      // GIVEN
      Path nonExistentZip = tempDir.resolve("missing.zip");
      Path unzipLocation = tempDir.resolve("output");

      // WHEN / THEN
      assertThatThrownBy(() -> SnomedReleaseFileHelper.unzip(nonExistentZip, unzipLocation))
          .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    @DisplayName("Should extract ZIP file contents into the destination directory")
    void shouldExtractZipContentsIntoDestination()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile =
          createZipFile(
              tempDir, "snomed-release.zip", "sct2_Concept_Full_INT_20240101.txt", "concept-data");
      Path unzipLocation = tempDir.resolve("output");

      // WHEN
      SnomedReleaseFileHelper.unzip(zipFile, unzipLocation);

      // THEN
      assertThat(unzipLocation.resolve("sct2_Concept_Full_INT_20240101.txt"))
          .as("Extracted file must exist at the destination")
          .exists();
      assertThat(Files.readString(unzipLocation.resolve("sct2_Concept_Full_INT_20240101.txt")))
          .as("Extracted file content must match the original ZIP entry")
          .isEqualTo("concept-data");
    }

    @Test
    @DisplayName("Should create the destination directory if it does not yet exist")
    void shouldCreateDestinationDirectoryIfMissing()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile =
          createZipFile(
              tempDir, "snomed-release.zip", "sct2_Concept_Full_INT_20240101.txt", "content");
      Path unzipLocation = tempDir.resolve("new-output-dir");

      // WHEN
      SnomedReleaseFileHelper.unzip(zipFile, unzipLocation);

      // THEN
      assertThat(unzipLocation)
          .as("Destination directory must be created during unzip")
          .isDirectory();
    }

    @Test
    @DisplayName("Should delete existing content at destination before extracting")
    void shouldDeleteExistingContentBeforeExtracting()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path unzipLocation = tempDir.resolve("output");
      Files.createDirectories(unzipLocation);
      Path staleFile = Files.createFile(unzipLocation.resolve("stale.txt"));

      Path zipFile =
          createZipFile(
              tempDir, "snomed-release.zip", "sct2_Concept_Full_INT_20240101.txt", "new-content");

      // WHEN
      SnomedReleaseFileHelper.unzip(zipFile, unzipLocation);

      // THEN
      assertThat(staleFile)
          .as("Stale file from a previous extraction must be removed before the new extraction")
          .doesNotExist();
      assertThat(unzipLocation.resolve("sct2_Concept_Full_INT_20240101.txt"))
          .as("Newly extracted file must be present after the extraction")
          .exists();
    }

    @Test
    @DisplayName("Should extract directory entries as actual directories")
    void shouldExtractDirectoryEntriesAsDirectories()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile = createZipWithDirectory(tempDir, "snomed-release.zip");
      Path unzipLocation = tempDir.resolve("output");

      // WHEN
      SnomedReleaseFileHelper.unzip(zipFile, unzipLocation);

      // THEN
      assertThat(unzipLocation.resolve("Full"))
          .as("A directory entry in the ZIP must be extracted as an actual directory")
          .isDirectory();
      assertThat(unzipLocation.resolve("Full").resolve("sct2_Concept_Full_INT_20240101.txt"))
          .as("A file entry inside a ZIP directory must also be extracted")
          .exists();
    }
  }

  // ===========================================================================
  // splitLine
  // ===========================================================================

  @Nested
  @DisplayName("splitLine Tests")
  class SplitLineTests {

    @Test
    @DisplayName("Should split a line with exactly the expected number of fields")
    void shouldSplitLineWithExpectedFieldCount() {
      // GIVEN
      String line = "id\t20240101\t1\t900000000000207008\t900000000000074008";

      // WHEN
      String[] result = SnomedReleaseFileHelper.splitLine(line, 5);

      // THEN
      assertThat(result)
          .as("Should return exactly 5 fields from a 5-field line")
          .containsExactly("id", "20240101", "1", "900000000000207008", "900000000000074008");
    }

    @Test
    @DisplayName(
        "Should include trailing content in last field when line has more fields than expected")
    void shouldIncludeTrailingContentInLastField() {
      // GIVEN
      String line = "id\t20240101\t1\t900000000000207008\textra1\textra2";

      // WHEN
      String[] result = SnomedReleaseFileHelper.splitLine(line, 4);

      // THEN
      assertThat(result).as("Should return exactly 4 elements").hasSize(4);
      assertThat(result[3])
          .as("The last field should contain the remainder of the line (unsplit)")
          .isEqualTo("900000000000207008\textra1\textra2");
    }

    @Test
    @DisplayName("Should throw SnomedDataLoadingException when line has fewer fields than expected")
    void shouldThrowWhenLineTooShort() {
      // GIVEN
      String line = "id\t20240101\t1";

      // WHEN / THEN
      assertThatThrownBy(() -> SnomedReleaseFileHelper.splitLine(line, 5))
          .isInstanceOf(SnomedDataLoadingException.class)
          .hasMessageContaining("Expected 5")
          .hasMessageContaining("found 3");
    }

    @Test
    @DisplayName(
        "Should throw SnomedDataLoadingException for a line with no tabs when multiple fields expected")
    void shouldThrowForLineWithNoTabs() {
      // GIVEN
      String line = "single-value-no-tabs";

      // WHEN / THEN
      assertThatThrownBy(() -> SnomedReleaseFileHelper.splitLine(line, 5))
          .isInstanceOf(SnomedDataLoadingException.class)
          .hasMessageContaining("Expected 5")
          .hasMessageContaining("found 1");
    }

    @Test
    @DisplayName("Should handle a single-field line when expected count is 1")
    void shouldHandleSingleFieldLine() {
      // GIVEN
      String line = "only-one-field";

      // WHEN
      String[] result = SnomedReleaseFileHelper.splitLine(line, 1);

      // THEN
      assertThat(result)
          .as("A single-field line with expected=1 should succeed")
          .containsExactly("only-one-field");
    }

    @Test
    @DisplayName("Should preserve empty fields between consecutive tabs")
    void shouldPreserveEmptyFields() {
      // GIVEN
      String line = "id\t\t1\t\tvalue";

      // WHEN
      String[] result = SnomedReleaseFileHelper.splitLine(line, 5);

      // THEN
      assertThat(result)
          .as("Empty fields between consecutive tabs must be preserved as empty strings")
          .containsExactly("id", "", "1", "", "value");
    }

    @Test
    @DisplayName("Should include truncated line content in error message")
    void shouldIncludeLineContentInErrorMessage() {
      // GIVEN
      String line = "short\tline";

      // WHEN / THEN
      assertThatThrownBy(() -> SnomedReleaseFileHelper.splitLine(line, 10))
          .isInstanceOf(SnomedDataLoadingException.class)
          .hasMessageContaining("short");
    }
  }

  // ===========================================================================
  // deleteUnzippedFiles
  // ===========================================================================

  @Nested
  @DisplayName("deleteUnzippedFiles Tests")
  class DeleteUnzippedFilesTests {

    @Test
    @DisplayName("Should return without error when the path does not exist")
    void shouldReturnWithoutErrorWhenPathDoesNotExist() {
      // GIVEN
      Path nonExistent = tempDir.resolve("non-existent");

      // WHEN / THEN
      assertThatCode(() -> SnomedReleaseFileHelper.deleteUnzippedFiles(nonExistent))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should delete the directory and all its files")
    void shouldDeleteDirectoryAndAllFiles() throws IOException {
      // GIVEN
      Path targetDir = tempDir.resolve("to-delete");
      Files.createDirectories(targetDir);
      Files.createFile(targetDir.resolve("file-a.txt"));
      Files.createFile(targetDir.resolve("file-b.txt"));

      // WHEN
      SnomedReleaseFileHelper.deleteUnzippedFiles(targetDir);

      // THEN
      assertThat(targetDir)
          .as("The target directory itself must be deleted along with its contents")
          .doesNotExist();
    }

    @Test
    @DisplayName("Should recursively delete files and subdirectories")
    void shouldRecursivelyDeleteFilesAndSubdirectories() throws IOException {
      // GIVEN
      Path targetDir = tempDir.resolve("to-delete");
      Path subDir = targetDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(targetDir.resolve("root.txt"));
      Files.createFile(subDir.resolve("nested.txt"));

      // WHEN
      SnomedReleaseFileHelper.deleteUnzippedFiles(targetDir);

      // THEN
      assertThat(targetDir)
          .as("Root directory and all nested contents must be deleted")
          .doesNotExist();
    }

    @Test
    @DisplayName("Should not affect sibling directories")
    void shouldNotAffectSiblingDirectories() throws IOException {
      // GIVEN
      Path targetDir = tempDir.resolve("to-delete");
      Path siblingDir = tempDir.resolve("sibling");
      Files.createDirectories(targetDir);
      Files.createDirectories(siblingDir);
      Path fileInSibling = Files.createFile(siblingDir.resolve("keep.txt"));

      // WHEN
      SnomedReleaseFileHelper.deleteUnzippedFiles(targetDir);

      // THEN
      assertThat(fileInSibling).as("Files in sibling directories must remain untouched").exists();
    }
  }
}
