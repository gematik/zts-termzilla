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
package de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link FhirPackageHelper}. */
@DisplayName("FhirPackageHelper Tests")
class FhirPackageHelperTest {

  @Nested
  @DisplayName("compress Tests")
  class CompressTests {

    @Test
    @DisplayName("Should compress file successfully")
    void shouldCompressFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Path compressedFile = tempDir.resolve("compressed.gz");
      Files.writeString(sourceFile, "Test content for compression");

      // WHEN
      FhirPackageHelper.compress(sourceFile, compressedFile);

      // THEN
      assertThat(compressedFile).exists().isRegularFile();

      // Verify content can be decompressed
      try (GzipCompressorInputStream gzis =
          new GzipCompressorInputStream(Files.newInputStream(compressedFile))) {
        String decompressed = new String(gzis.readAllBytes());
        assertThat(decompressed).isEqualTo("Test content for compression");
      }
    }

    @Test
    @DisplayName("Should throw exception when source is null")
    void shouldThrowExceptionWhenSourceIsNull(@TempDir Path tempDir) {
      // GIVEN
      Path destination = tempDir.resolve("output.gz");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.compress(null, destination))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Source is null");
    }

    @Test
    @DisplayName("Should throw exception when source is directory")
    void shouldThrowExceptionWhenSourceIsDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceDir = tempDir.resolve("sourcedir");
      Files.createDirectories(sourceDir);
      Path destination = tempDir.resolve("output.gz");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.compress(sourceDir, destination))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("is a directory");
    }

    @Test
    @DisplayName("Should throw exception when source does not exist")
    void shouldThrowExceptionWhenSourceNotExists(@TempDir Path tempDir) {
      // GIVEN
      Path nonExistent = tempDir.resolve("nonexistent.txt");
      Path destination = tempDir.resolve("output.gz");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.compress(nonExistent, destination))
          .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when destination is null")
    void shouldThrowExceptionWhenDestinationIsNull(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path source = tempDir.resolve("source.txt");
      Files.writeString(source, "content");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.compress(source, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Destination is null");
    }

    @Test
    @DisplayName("Should throw exception when destination is directory")
    void shouldThrowExceptionWhenDestinationIsDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path source = tempDir.resolve("source.txt");
      Files.writeString(source, "content");
      Path destDir = tempDir.resolve("destdir");
      Files.createDirectories(destDir);

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.compress(source, destDir))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Destination is directory");
    }
  }

  @Nested
  @DisplayName("decompress Tests")
  class DecompressTests {

    @Test
    @DisplayName("Should decompress file to specified destination")
    void shouldDecompressFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      String content = "Original content";
      Path compressedFile = createGzipFile(tempDir, "compressed.gz", content);
      Path destination = tempDir.resolve("decompressed.txt");

      // WHEN
      Path result = FhirPackageHelper.decompress(compressedFile, destination);

      // THEN
      assertThat(result).isEqualTo(destination).exists().hasContent(content);
    }

    @Test
    @DisplayName("Should decompress file to directory with derived filename")
    void shouldDecompressToDirectoryWithDerivedName(@TempDir Path tempDir) throws IOException {
      // GIVEN
      String content = "Test content";
      Path compressedFile = createGzipFile(tempDir, "archive.txt.gz", content);
      Path destDir = tempDir.resolve("output");
      Files.createDirectories(destDir);

      // WHEN
      Path result = FhirPackageHelper.decompress(compressedFile, destDir);

      // THEN
      assertThat(result).hasFileName("archive.txt").hasParent(destDir).hasContent(content);
    }

    @Test
    @DisplayName("Should remove extension when decompressing to directory")
    void shouldRemoveExtension(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path compressedFile = createGzipFile(tempDir, "file.json.gz", "{}");
      Path destDir = tempDir.resolve("output");
      Files.createDirectories(destDir);

      // WHEN
      Path result = FhirPackageHelper.decompress(compressedFile, destDir);

      // THEN
      assertThat(result.getFileName()).hasToString("file.json");
    }

    @Test
    @DisplayName("Should throw exception when source does not exist")
    void shouldThrowExceptionWhenSourceNotExists(@TempDir Path tempDir) {
      // GIVEN
      Path nonExistent = tempDir.resolve("nonexistent.gz");
      Path destination = tempDir.resolve("output.txt");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.decompress(nonExistent, destination))
          .isInstanceOf(FileNotFoundException.class);
    }

    private Path createGzipFile(Path tempDir, String filename, String content) throws IOException {
      Path gzipFile = tempDir.resolve(filename);
      try (var gzos =
          new org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream(
              Files.newOutputStream(gzipFile))) {
        gzos.write(content.getBytes());
      }
      return gzipFile;
    }
  }

  @Nested
  @DisplayName("unTar Tests")
  class UnTarTests {

    @Test
    @DisplayName("Should extract tar archive successfully")
    void shouldExtractTarArchive(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path tarFile = createTarArchive(tempDir, "archive.tar", "file1.txt", "file2.txt");
      Path destination = tempDir.resolve("extracted");
      Files.createDirectories(destination);

      // WHEN
      FhirPackageHelper.unTar(tarFile, destination);

      // THEN
      assertThat(destination.resolve("file1.txt")).exists().hasContent("Content of file1.txt");
      assertThat(destination.resolve("file2.txt")).exists().hasContent("Content of file2.txt");
    }

    @Test
    @DisplayName("Should create directories from tar entries")
    void shouldCreateDirectories(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path tarFile = createTarArchiveWithDirectories(tempDir);
      Path destination = tempDir.resolve("extracted");
      Files.createDirectories(destination);

      // WHEN
      FhirPackageHelper.unTar(tarFile, destination);

      // THEN
      assertThat(destination.resolve("subdir")).exists().isDirectory();
      assertThat(destination.resolve("subdir").resolve("nested.txt"))
          .exists()
          .hasContent("Nested content");
    }

    @Test
    @DisplayName("Should throw exception when archive is directory")
    void shouldThrowExceptionWhenArchiveIsDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path directory = tempDir.resolve("notanarchive");
      Files.createDirectories(directory);
      Path destination = tempDir.resolve("output");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.unTar(directory, destination))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unable to untar")
          .hasMessageContaining("is a directory");
    }

    @Test
    @DisplayName("Should throw exception when archive does not exist")
    void shouldThrowExceptionWhenArchiveNotExists(@TempDir Path tempDir) {
      // GIVEN
      Path nonExistent = tempDir.resolve("nonexistent.tar");
      Path destination = tempDir.resolve("output");

      // WHEN & THEN
      assertThatThrownBy(() -> FhirPackageHelper.unTar(nonExistent, destination))
          .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("Should create destination directory if not exists")
    void shouldCreateDestinationDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path tarFile = createTarArchive(tempDir, "archive.tar", "file.txt");
      Path destination = tempDir.resolve("newdir").resolve("extracted");

      // WHEN
      FhirPackageHelper.unTar(tarFile, destination);

      // THEN
      assertThat(destination).exists().isDirectory();
      assertThat(destination.resolve("file.txt")).exists();
    }

    private Path createTarArchive(Path tempDir, String archiveName, String... fileNames)
        throws IOException {
      Path tarFile = tempDir.resolve(archiveName);
      try (TarArchiveOutputStream taos =
          new TarArchiveOutputStream(Files.newOutputStream(tarFile))) {
        for (String fileName : fileNames) {
          TarArchiveEntry entry = new TarArchiveEntry(fileName);
          byte[] content = ("Content of " + fileName).getBytes();
          entry.setSize(content.length);
          taos.putArchiveEntry(entry);
          taos.write(content);
          taos.closeArchiveEntry();
        }
      }
      return tarFile;
    }

    private Path createTarArchiveWithDirectories(Path tempDir) throws IOException {
      Path tarFile = tempDir.resolve("archive.tar");
      try (TarArchiveOutputStream taos =
          new TarArchiveOutputStream(Files.newOutputStream(tarFile))) {
        // Add directory entry
        TarArchiveEntry dirEntry = new TarArchiveEntry("subdir/");
        taos.putArchiveEntry(dirEntry);
        taos.closeArchiveEntry();

        // Add file in directory
        TarArchiveEntry fileEntry = new TarArchiveEntry("subdir/nested.txt");
        byte[] content = "Nested content".getBytes();
        fileEntry.setSize(content.length);
        taos.putArchiveEntry(fileEntry);
        taos.write(content);
        taos.closeArchiveEntry();
      }
      return tarFile;
    }
  }

  @Nested
  @DisplayName("createTarArchive Tests")
  class CreateTarArchiveTests {

    @Test
    @DisplayName("Should create tar archive with single file")
    void shouldCreateTarArchiveWithSingleFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Files.writeString(sourceFile, "File content");
      Path destination = tempDir.resolve("archives");
      Files.createDirectories(destination);

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", destination, sourceFile);

      // THEN
      assertThat(archive).exists().hasFileName("test.tar");

      List<String> entries = extractTarEntries(archive);
      assertThat(entries).containsExactly("source.txt");
    }

    @Test
    @DisplayName("Should create tar archive with multiple files")
    void shouldCreateTarArchiveWithMultipleFiles(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path file1 = tempDir.resolve("file1.txt");
      Path file2 = tempDir.resolve("file2.txt");
      Files.writeString(file1, "Content 1");
      Files.writeString(file2, "Content 2");
      Path destination = tempDir.resolve("archives");
      Files.createDirectories(destination);

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", destination, file1, file2);

      // THEN
      List<String> entries = extractTarEntries(archive);
      assertThat(entries).containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }

    @Test
    @DisplayName("Should create tar archive with directory structure")
    void shouldCreateTarArchiveWithDirectories(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceDir = tempDir.resolve("source");
      Files.createDirectories(sourceDir);
      Files.writeString(sourceDir.resolve("file1.txt"), "Content 1");

      Path subDir = sourceDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.writeString(subDir.resolve("file2.txt"), "Content 2");

      Path destination = tempDir.resolve("archives");
      Files.createDirectories(destination);

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", destination, sourceDir);

      // THEN
      List<String> entries = extractTarEntries(archive);
      // TAR archives add trailing slashes to directory entries
      assertThat(entries)
          .contains("source/", "source/file1.txt", "source/subdir/", "source/subdir/file2.txt");
    }

    @Test
    @DisplayName("Should throw exception when source file does not exist")
    void shouldThrowExceptionWhenSourceNotExists(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path nonExistent = tempDir.resolve("nonexistent.txt");
      Path destination = tempDir.resolve("archives");
      Files.createDirectories(destination);

      // WHEN & THEN
      assertThatThrownBy(
              () -> FhirPackageHelper.createTarArchive("test.tar", destination, nonExistent))
          .isInstanceOf(FileNotFoundException.class)
          .hasMessageContaining(nonExistent.toString());
    }

    @Test
    @DisplayName("Should throw exception when destination is not a directory")
    void shouldThrowExceptionWhenDestinationNotDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Files.writeString(sourceFile, "content");
      Path destination = tempDir.resolve("notadir.txt");
      Files.createFile(destination);

      // WHEN & THEN
      assertThatThrownBy(
              () -> FhirPackageHelper.createTarArchive("test.tar", destination, sourceFile))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should create destination directory if not exists")
    void shouldCreateDestinationIfNotExists(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Files.writeString(sourceFile, "content");
      Path destination = tempDir.resolve("newdir");

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", destination, sourceFile);

      // THEN
      assertThat(destination).exists().isDirectory();
      assertThat(archive).exists();
    }

    @Test
    @DisplayName("Should handle existing archive file")
    void shouldHandleExistingArchiveFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Files.writeString(sourceFile, "content");
      Path destination = tempDir.resolve("archives");
      Files.createDirectories(destination);
      Path existingArchive = destination.resolve("test.tar");
      Files.createFile(existingArchive);

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", destination, sourceFile);

      // THEN
      assertThat(archive).exists();
    }

    private List<String> extractTarEntries(Path tarFile) throws IOException {
      List<String> entries = new ArrayList<>();
      try (TarArchiveInputStream tais =
          new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(tarFile)))) {
        TarArchiveEntry entry;
        while ((entry = tais.getNextEntry()) != null) {
          entries.add(entry.getName());
        }
      }
      return entries;
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should compress and decompress file successfully")
    void shouldCompressAndDecompress(@TempDir Path tempDir) throws IOException {
      // GIVEN
      String originalContent =
          "This is the original content that will be compressed and decompressed. ".repeat(100);
      Path originalFile = tempDir.resolve("original.txt");
      Files.writeString(originalFile, originalContent);
      Path compressedFile = tempDir.resolve("compressed.gz");
      Path decompressedFile = tempDir.resolve("decompressed.txt");

      // WHEN
      FhirPackageHelper.compress(originalFile, compressedFile);
      FhirPackageHelper.decompress(compressedFile, decompressedFile);

      // THEN
      assertThat(compressedFile).exists();
      assertThat(decompressedFile).exists().hasContent(originalContent);
      // With larger content, compression should be effective
      assertThat(Files.size(compressedFile)).isLessThan(Files.size(originalFile));
    }

    @Test
    @DisplayName("Should create and extract tar archive successfully")
    void shouldCreateAndExtractTarArchive(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceDir = tempDir.resolve("source");
      Files.createDirectories(sourceDir);
      Files.writeString(sourceDir.resolve("file1.txt"), "Content 1");
      Files.writeString(sourceDir.resolve("file2.txt"), "Content 2");

      Path archiveDir = tempDir.resolve("archives");
      Path extractDir = tempDir.resolve("extracted");

      // WHEN
      Path archive = FhirPackageHelper.createTarArchive("test.tar", archiveDir, sourceDir);
      FhirPackageHelper.unTar(archive, extractDir);

      // THEN
      assertThat(extractDir.resolve("source").resolve("file1.txt"))
          .exists()
          .hasContent("Content 1");
      assertThat(extractDir.resolve("source").resolve("file2.txt"))
          .exists()
          .hasContent("Content 2");
    }

    @Test
    @DisplayName("Should handle complete FHIR package workflow")
    void shouldHandleCompleteWorkflow(@TempDir Path tempDir) throws IOException {
      // GIVEN - Create package structure
      Path packageDir = tempDir.resolve("package");
      Files.createDirectories(packageDir);
      Files.writeString(packageDir.resolve("package.json"), "{\"name\":\"test-package\"}");

      Path resourcesDir = packageDir.resolve("resources");
      Files.createDirectories(resourcesDir);
      Files.writeString(
          resourcesDir.resolve("CodeSystem.json"), "{\"resourceType\":\"CodeSystem\"}");

      // WHEN - Create tar, compress it
      Path tarFile = FhirPackageHelper.createTarArchive("package.tar", tempDir, packageDir);
      Path gzFile = tempDir.resolve("package.tgz");
      FhirPackageHelper.compress(tarFile, gzFile);

      // Extract and decompress
      Path decompressedTar =
          FhirPackageHelper.decompress(gzFile, tempDir.resolve("decompressed.tar"));
      Path extractedDir = tempDir.resolve("extracted");
      FhirPackageHelper.unTar(decompressedTar, extractedDir);

      // THEN
      assertThat(extractedDir.resolve("package").resolve("package.json"))
          .exists()
          .hasContent("{\"name\":\"test-package\"}");
      assertThat(extractedDir.resolve("package").resolve("resources").resolve("CodeSystem.json"))
          .exists()
          .hasContent("{\"resourceType\":\"CodeSystem\"}");
    }
  }
}
