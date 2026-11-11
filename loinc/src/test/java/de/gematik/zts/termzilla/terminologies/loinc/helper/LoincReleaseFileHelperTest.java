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
package de.gematik.zts.termzilla.terminologies.loinc.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link LoincReleaseFileHelper}.
 *
 * <p>All methods under test are static, so no instantiation or mocking is required. File system
 * interactions are isolated via {@link TempDir}.
 */
@DisplayName("LoincReleaseFileHelper Tests")
class LoincReleaseFileHelperTest {

  @TempDir Path tempDir;

  // ---------------------------------------------------------------------------
  // Test data helpers
  // ---------------------------------------------------------------------------

  /** Creates a ZIP file at {@code directory/fileName} with one entry per map entry. */
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
      zos.putNextEntry(new ZipEntry("AccessoryFiles/"));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("AccessoryFiles/test.csv"));
      zos.write("dir-content".getBytes());
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
    @DisplayName("Should throw InvalidConfigurationException when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
      // GIVEN
      String nonExistentPath = tempDir.resolve("non-existent").toString();

      // WHEN / THEN
      assertThatThrownBy(() -> LoincReleaseFileHelper.calculateReleaseFileHash(nonExistentPath))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(nonExistentPath);
    }

    @Test
    @DisplayName("Should return hash of empty string when no LOINC ZIP files are found")
    void shouldReturnHashOfEmptyStringWhenNoMatchingFiles() throws InvalidConfigurationException {
      // GIVEN
      // tempDir is empty — no files matching FILE_RF_ZIP_PATTERN

      // WHEN
      String hash = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // THEN
      assertThat(hash)
          .as("Hash over zero files must equal SHA-256 of the empty string")
          .isEqualTo(DigestUtils.sha256Hex(""));
    }

    @Test
    @DisplayName("Should return a valid SHA-256 hash for a single LOINC ZIP file")
    void shouldReturnValidHashForSingleLoincZipFile()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "loinc-content");

      // WHEN
      String hash = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // THEN
      assertThat(hash)
          .as("Hash should be a 64-character lowercase hex string")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should exclude files not matching the LOINC ZIP naming pattern")
    void shouldExcludeNonMatchingFilesFromHash() throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "loinc-content");
      String hashBefore = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // WHEN — add non-matching files to the same directory
      Files.writeString(tempDir.resolve("other.zip"), "other-content");
      Files.writeString(tempDir.resolve("readme.txt"), "readme");
      String hashAfter = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // THEN
      assertThat(hashAfter)
          .as("Files not matching the LOINC ZIP pattern must not affect the combined hash")
          .isEqualTo(hashBefore);
    }

    @Test
    @DisplayName("Should produce a deterministic hash for unchanged directory contents")
    void shouldProduceDeterministicHash() throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "loinc-content");

      // WHEN
      String firstHash = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());
      String secondHash = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // THEN
      assertThat(firstHash)
          .as("Hash must be identical across repeated calls for the same directory contents")
          .isEqualTo(secondHash);
    }

    @Test
    @DisplayName("Should produce a different hash when a LOINC ZIP file is added")
    void shouldProduceDifferentHashWhenFileIsAdded()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "content-78");
      String hashBefore = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // WHEN
      createZipFile(tempDir, "Loinc_2.79.zip", "Loinc.csv", "content-79");
      String hashAfter = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());

      // THEN
      assertThat(hashAfter)
          .as("Hash must change when a new LOINC release file is added to the directory")
          .isNotEqualTo(hashBefore);
    }

    @Test
    @DisplayName("Should include LOINC ZIP files found recursively in subdirectories")
    void shouldIncludeFilesFromSubdirectories() throws IOException, InvalidConfigurationException {
      // GIVEN — the LOINC ZIP is only in a subdirectory, not at root level
      Path subDir = tempDir.resolve("release-2.79");
      Files.createDirectories(subDir);
      createZipFile(subDir, "Loinc_2.79.zip", "Loinc.csv", "content-79");

      // WHEN — root search traverses recursively and finds the same file as the subdir search
      String hashFromRoot = LoincReleaseFileHelper.calculateReleaseFileHash(tempDir.toString());
      String hashFromSubDir = LoincReleaseFileHelper.calculateReleaseFileHash(subDir.toString());

      // THEN
      assertThat(hashFromRoot)
          .as(
              "Recursive traversal from root must find the same LOINC ZIP as a direct subdir search")
          .isEqualTo(hashFromSubDir);
    }
  }

  // ===========================================================================
  // getMultipleReleaseFilePaths
  // ===========================================================================

  @Nested
  @DisplayName("getMultipleReleaseFilePaths Tests")
  class GetMultipleReleaseFilePathsTests {

    @Test
    @DisplayName("Should throw InvalidConfigurationException when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
      // GIVEN
      String nonExistentPath = tempDir.resolve("non-existent").toString();

      // WHEN / THEN
      assertThatThrownBy(() -> LoincReleaseFileHelper.getMultipleReleaseFilePaths(nonExistentPath))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(nonExistentPath);
    }

    @Test
    @DisplayName("Should return an empty list when no LOINC ZIP files are present")
    void shouldReturnEmptyListWhenNoMatchingFiles() throws InvalidConfigurationException {
      // GIVEN
      // tempDir is empty

      // WHEN
      List<String> result = LoincReleaseFileHelper.getMultipleReleaseFilePaths(tempDir.toString());

      // THEN
      assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Loinc_2.78.zip", "Loinc_2.79.zip", "Loinc_2.80.zip"})
    @DisplayName("Should match files conforming to the LOINC ZIP naming pattern")
    void shouldMatchValidLoincZipFileNames(String fileName)
        throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, fileName, "Loinc.csv", "content");

      // WHEN
      List<String> result = LoincReleaseFileHelper.getMultipleReleaseFilePaths(tempDir.toString());

      // THEN
      assertThat(result)
          .as("File '%s' must be matched by FILE_RF_ZIP_PATTERN", fileName)
          .hasSize(1)
          .first()
          .asString()
          .endsWith(fileName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"other.zip", "Loinc_3.78.zip", "loinc_2.78.zip", "Loinc_2.78.tar"})
    @DisplayName("Should not match files that do not conform to the LOINC ZIP naming pattern")
    void shouldNotMatchNonLoincFiles(String fileName)
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(tempDir.resolve(fileName), "content");

      // WHEN
      List<String> result = LoincReleaseFileHelper.getMultipleReleaseFilePaths(tempDir.toString());

      // THEN
      assertThat(result)
          .as("File '%s' must not be matched by FILE_RF_ZIP_PATTERN", fileName)
          .isEmpty();
    }

    @Test
    @DisplayName("Should return results in sorted order")
    void shouldReturnResultsInSortedOrder() throws IOException, InvalidConfigurationException {
      // GIVEN
      createZipFile(tempDir, "Loinc_2.80.zip", "Loinc.csv", "content-80");
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "content-78");
      createZipFile(tempDir, "Loinc_2.79.zip", "Loinc.csv", "content-79");

      // WHEN
      List<String> result = LoincReleaseFileHelper.getMultipleReleaseFilePaths(tempDir.toString());

      // THEN
      assertThat(result)
          .as("Returned paths must be sorted in ascending order")
          .isSortedAccordingTo(String::compareTo);
    }

    @Test
    @DisplayName("Should find LOINC ZIP files recursively in subdirectories")
    void shouldFindFilesRecursively() throws IOException, InvalidConfigurationException {
      // GIVEN
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "content-78");
      createZipFile(subDir, "Loinc_2.79.zip", "Loinc.csv", "content-79");

      // WHEN
      List<String> result = LoincReleaseFileHelper.getMultipleReleaseFilePaths(tempDir.toString());

      // THEN
      assertThat(result)
          .as("Recursive traversal must find LOINC ZIP files in subdirectories")
          .hasSize(2);
    }
  }

  // ===========================================================================
  // getSingleReleaseFilePath
  // ===========================================================================

  @Nested
  @DisplayName("getSingleReleaseFilePath Tests")
  class GetSingleReleaseFilePathTests {

    @Test
    @DisplayName("Should throw InvalidConfigurationException when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
      // GIVEN
      Path nonExistent = tempDir.resolve("non-existent");

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  LoincReleaseFileHelper.getSingleReleaseFilePath(
                      nonExistent, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(nonExistent.toString());
    }

    @Test
    @DisplayName("Should return null when no file matches the pattern")
    void shouldReturnNullWhenNoMatchingFile() throws InvalidConfigurationException {
      // GIVEN
      // tempDir is empty

      // WHEN
      Path result =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return the path of the matching file")
    void shouldReturnPathOfMatchingFile() throws IOException, InvalidConfigurationException {
      // GIVEN
      Path conceptsFile = Files.createFile(tempDir.resolve("Loinc.csv"));

      // WHEN
      Path result =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result)
          .as("Should return the path of the matched Loinc.csv file")
          .isEqualTo(conceptsFile);
    }

    @Test
    @DisplayName("Should find matching files in subdirectories")
    void shouldFindFilesInSubdirectories() throws IOException, InvalidConfigurationException {
      // GIVEN
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      Path conceptsFile = Files.createFile(subDir.resolve("Loinc.csv"));

      // WHEN
      Path result =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN);

      // THEN
      assertThat(result)
          .as("Recursive search must find Loinc.csv in a subdirectory")
          .isEqualTo(conceptsFile);
    }

    @Test
    @DisplayName("Should match by file name only, not by full path")
    void shouldMatchByFileNameOnly() throws IOException, InvalidConfigurationException {
      // GIVEN
      // A directory whose name matches the pattern, containing an unrelated file
      Path dirNamedAsPattern = tempDir.resolve("Loinc.csv");
      Files.createDirectories(dirNamedAsPattern);
      Files.createFile(dirNamedAsPattern.resolve("unrelated.txt"));

      // WHEN
      Path result =
          LoincReleaseFileHelper.getSingleReleaseFilePath(
              tempDir, LoincReleaseFileHelper.FILE_CONCEPTS_PATTERN);

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
      String nonExistentZip = tempDir.resolve("missing.zip").toString();
      Path unzipLocation = tempDir.resolve("output");

      // WHEN / THEN
      assertThatThrownBy(() -> LoincReleaseFileHelper.unzip(nonExistentZip, unzipLocation))
          .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    @DisplayName("Should extract ZIP file contents into the destination directory")
    void shouldExtractZipContentsIntoDestination()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile = createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "loinc-data");
      Path unzipLocation = tempDir.resolve("output");

      // WHEN
      LoincReleaseFileHelper.unzip(zipFile.toString(), unzipLocation);

      // THEN
      assertThat(unzipLocation.resolve("Loinc.csv"))
          .as("Extracted file must exist at the destination")
          .exists();
      assertThat(Files.readString(unzipLocation.resolve("Loinc.csv")))
          .as("Extracted file content must match the original ZIP entry")
          .isEqualTo("loinc-data");
    }

    @Test
    @DisplayName("Should create the destination directory if it does not yet exist")
    void shouldCreateDestinationDirectoryIfMissing()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile = createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "content");
      Path unzipLocation = tempDir.resolve("new-output-dir");

      // WHEN
      LoincReleaseFileHelper.unzip(zipFile.toString(), unzipLocation);

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

      Path zipFile = createZipFile(tempDir, "Loinc_2.78.zip", "Loinc.csv", "new-content");

      // WHEN
      LoincReleaseFileHelper.unzip(zipFile.toString(), unzipLocation);

      // THEN
      assertThat(staleFile)
          .as("Stale file from a previous extraction must be removed before the new extraction")
          .doesNotExist();
      assertThat(unzipLocation.resolve("Loinc.csv"))
          .as("Newly extracted file must be present after the extraction")
          .exists();
    }

    @Test
    @DisplayName("Should extract directory entries as actual directories")
    void shouldExtractDirectoryEntriesAsDirectories()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path zipFile = createZipWithDirectory(tempDir, "Loinc_2.78.zip");
      Path unzipLocation = tempDir.resolve("output");

      // WHEN
      LoincReleaseFileHelper.unzip(zipFile.toString(), unzipLocation);

      // THEN
      assertThat(unzipLocation.resolve("AccessoryFiles"))
          .as("A directory entry in the ZIP must be extracted as an actual directory")
          .isDirectory();
      assertThat(unzipLocation.resolve("AccessoryFiles").resolve("test.csv"))
          .as("A file entry inside a ZIP directory must also be extracted")
          .exists();
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
      assertThatCode(() -> LoincReleaseFileHelper.deleteUnzippedFiles(nonExistent))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should delete the directory and all its files")
    void shouldDeleteDirectoryAndAllFiles() throws IOException {
      // GIVEN
      Path targetDir = tempDir.resolve("to-delete");
      Files.createDirectories(targetDir);
      Files.createFile(targetDir.resolve("file-a.csv"));
      Files.createFile(targetDir.resolve("file-b.csv"));

      // WHEN
      LoincReleaseFileHelper.deleteUnzippedFiles(targetDir);

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
      Files.createFile(targetDir.resolve("root.csv"));
      Files.createFile(subDir.resolve("nested.csv"));

      // WHEN
      LoincReleaseFileHelper.deleteUnzippedFiles(targetDir);

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
      Path fileInSibling = Files.createFile(siblingDir.resolve("keep.csv"));

      // WHEN
      LoincReleaseFileHelper.deleteUnzippedFiles(targetDir);

      // THEN
      assertThat(fileInSibling).as("Files in sibling directories must remain untouched").exists();
    }
  }
}
