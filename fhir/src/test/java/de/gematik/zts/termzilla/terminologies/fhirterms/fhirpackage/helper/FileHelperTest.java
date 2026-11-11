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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link FileHelper}. */
@DisplayName("FileHelper Tests")
class FileHelperTest {

  @Nested
  @DisplayName("createTemporaryDirectory Tests")
  class CreateTemporaryDirectoryTests {

    @Test
    @DisplayName("Should create temporary directory with default prefix")
    void shouldCreateTempDirectoryWithDefaultPrefix() {
      // GIVEN & WHEN
      Path tempDir = FileHelper.createTemporaryDirectory();

      // THEN
      assertThat(tempDir).exists().isDirectory();
      assertThat(tempDir.getFileName().toString()).startsWith("termzilla");

      // Cleanup
      tempDir.toFile().delete();
    }

    @Test
    @DisplayName("Should create temporary directory with custom prefix")
    void shouldCreateTempDirectoryWithCustomPrefix() {
      // GIVEN
      String customPrefix = "myprefix";

      // WHEN
      Path tempDir = FileHelper.createTemporaryDirectory(customPrefix);

      // THEN
      assertThat(tempDir).exists().isDirectory();
      assertThat(tempDir.getFileName().toString()).startsWith(customPrefix);

      // Cleanup
      tempDir.toFile().delete();
    }

    @ParameterizedTest
    @ValueSource(strings = {"test\\invalid", "test:invalid", "test*invalid", "test?invalid"})
    @DisplayName("Should replace illegal characters in prefix with underscore")
    void shouldReplaceIllegalCharactersInPrefix(String illegalPrefix) {
      // GIVEN & WHEN
      Path tempDir = FileHelper.createTemporaryDirectory(illegalPrefix);

      // THEN
      assertThat(tempDir).exists().isDirectory();
      assertThat(tempDir.getFileName().toString())
          .doesNotContain("\\", ":", "*", "?", "<", ">", "|", "\"", "/");

      // Cleanup
      tempDir.toFile().delete();
    }
  }

  @Nested
  @DisplayName("createTemporaryFile Tests")
  class CreateTemporaryFileTests {

    @Test
    @DisplayName("Should create temporary file with specified suffix")
    void shouldCreateTempFileWithSuffix() {
      // GIVEN
      String suffix = ".txt";

      // WHEN
      Path tempFile = FileHelper.createTemporaryFile(suffix);

      // THEN
      assertThat(tempFile).exists().isRegularFile();
      assertThat(tempFile.getFileName().toString()).startsWith("termzilla").endsWith(suffix);

      // Cleanup
      tempFile.toFile().delete();
    }

    @ParameterizedTest
    @ValueSource(strings = {".json", ".zip", ".xml", ".tar.gz"})
    @DisplayName("Should create temporary file with various suffixes")
    void shouldCreateTempFileWithVariousSuffixes(String suffix) {
      // GIVEN & WHEN
      Path tempFile = FileHelper.createTemporaryFile(suffix);

      // THEN
      assertThat(tempFile).exists().isRegularFile();
      assertThat(tempFile.getFileName().toString()).endsWith(suffix);

      // Cleanup
      tempFile.toFile().delete();
    }
  }

  @Nested
  @DisplayName("closeQuietly Tests")
  class CloseQuietlyTests {

    @Test
    @DisplayName("Should close closeable without throwing exception")
    void shouldCloseCloseableWithoutException() {
      // GIVEN
      ByteArrayOutputStream stream = new ByteArrayOutputStream();

      // WHEN & THEN
      assertThatCode(() -> FileHelper.closeQuietly(stream)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null closeable gracefully")
    void shouldHandleNullCloseable() {
      // GIVEN
      Closeable closeable = null;

      // WHEN & THEN
      assertThatCode(() -> FileHelper.closeQuietly(closeable)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should suppress IOException during close")
    void shouldSuppressIOException() {
      // GIVEN
      Closeable faultyCloseable =
          () -> {
            throw new IOException("Close failed");
          };

      // WHEN & THEN
      assertThatCode(() -> FileHelper.closeQuietly(faultyCloseable)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("requireDirectory Tests")
  class RequireDirectoryTests {

    @Test
    @DisplayName("Should accept existing writable directory")
    void shouldAcceptExistingDirectory(@TempDir Path tempDir) {
      // GIVEN & WHEN & THEN
      assertThatCode(() -> FileHelper.requireDirectory(tempDir)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create directory if it does not exist")
    void shouldCreateDirectoryIfNotExists(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path newDir = tempDir.resolve("newdir");

      // WHEN
      FileHelper.requireDirectory(newDir);

      // THEN
      assertThat(newDir).exists().isDirectory();
    }

    @Test
    @DisplayName("Should throw exception if path is a file")
    void shouldThrowExceptionIfPathIsFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);

      // WHEN & THEN
      assertThatThrownBy(() -> FileHelper.requireDirectory(file))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("exists and is a file");
    }
  }

  @Nested
  @DisplayName("relativePath Tests")
  class RelativePathTests {

    @Test
    @DisplayName("Should calculate relative path correctly")
    void shouldCalculateRelativePath(@TempDir Path tempDir) throws IOException {
      // GIVEN
      File root = tempDir.toFile();
      File child = tempDir.resolve("subdir").resolve("file.txt").toFile();
      Files.createDirectories(child.getParentFile().toPath());
      Files.createFile(child.toPath());

      // WHEN
      String relativePath = FileHelper.relativePath(root, child);

      // THEN
      assertThat(relativePath).isEqualTo("subdir" + File.separator + "file.txt");
    }

    @Test
    @DisplayName("Should handle nested directories")
    void shouldHandleNestedDirectories(@TempDir Path tempDir) throws IOException {
      // GIVEN
      File root = tempDir.toFile();
      File nested = tempDir.resolve("a").resolve("b").resolve("c").resolve("file.txt").toFile();
      Files.createDirectories(nested.getParentFile().toPath());
      Files.createFile(nested.toPath());

      // WHEN
      String relativePath = FileHelper.relativePath(root, nested);

      // THEN
      assertThat(relativePath)
          .isEqualTo(
              "a" + File.separator + "b" + File.separator + "c" + File.separator + "file.txt");
    }
  }

  @Nested
  @DisplayName("copy Tests")
  class CopyTests {

    @Test
    @DisplayName("Should copy data from input stream to output stream")
    void shouldCopyStreamToStream() throws IOException {
      // GIVEN
      byte[] data = "Hello, World!".getBytes();
      ByteArrayInputStream input = new ByteArrayInputStream(data);
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      // WHEN
      long bytesCopied = FileHelper.copy(input, output);

      // THEN
      assertThat(bytesCopied).isEqualTo(data.length);
      assertThat(output.toByteArray()).isEqualTo(data);
    }

    @Test
    @DisplayName("Should copy input stream to file")
    void shouldCopyStreamToFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      byte[] data = "Test content".getBytes();
      ByteArrayInputStream input = new ByteArrayInputStream(data);
      Path destination = tempDir.resolve("output.txt");

      // WHEN
      FileHelper.copy(input, destination);

      // THEN
      assertThat(destination).exists().isRegularFile();
      assertThat(Files.readAllBytes(destination)).isEqualTo(data);
    }

    @Test
    @DisplayName("Should copy empty stream")
    void shouldCopyEmptyStream() throws IOException {
      // GIVEN
      ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      // WHEN
      long bytesCopied = FileHelper.copy(input, output);

      // THEN
      assertThat(bytesCopied).isZero();
      assertThat(output.toByteArray()).isEmpty();
    }

    @Test
    @DisplayName("Should copy file to target directory")
    void shouldCopyFileToDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceFile = tempDir.resolve("source.txt");
      Files.writeString(sourceFile, "content");
      Path targetDir = tempDir.resolve("target");

      // WHEN
      Path copiedFile = FileHelper.copy(targetDir, sourceFile);

      // THEN
      assertThat(copiedFile).exists().isRegularFile();
      assertThat(Files.readString(copiedFile)).isEqualTo("content");
      assertThat(copiedFile).hasParentRaw(targetDir);
    }
  }

  @Nested
  @DisplayName("filesContainedIn Tests")
  class FilesContainedInTests {

    @Test
    @DisplayName("Should return files in directory")
    void shouldReturnFilesInDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Files.createFile(tempDir.resolve("file1.txt"));
      Files.createFile(tempDir.resolve("file2.txt"));

      // WHEN
      File[] files = FileHelper.filesContainedIn(tempDir.toFile());

      // THEN
      assertThat(files)
          .isNotNull()
          .hasSize(2)
          .extracting(File::getName)
          .containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }

    @Test
    @DisplayName("Should return single file if source is not a directory")
    void shouldReturnSingleFileIfNotDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);

      // WHEN
      File[] files = FileHelper.filesContainedIn(file.toFile());

      // THEN
      assertThat(files).hasSize(1).contains(file.toFile());
    }
  }

  @Nested
  @DisplayName("deleteFiles Tests")
  class DeleteFilesTests {

    @Test
    @DisplayName("Should delete single file")
    void shouldDeleteSingleFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);

      // WHEN
      FileHelper.deleteFiles(file);

      // THEN
      assertThat(file).doesNotExist();
    }

    @Test
    @DisplayName("Should delete directory with contents recursively")
    void shouldDeleteDirectoryRecursively(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("file1.txt"));
      Files.createFile(subDir.resolve("file2.txt"));

      // WHEN
      FileHelper.deleteFiles(subDir);

      // THEN
      assertThat(subDir).doesNotExist();
    }

    @Test
    @DisplayName("Should delete nested directory structure")
    void shouldDeleteNestedStructure(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path nested = tempDir.resolve("a").resolve("b").resolve("c");
      Files.createDirectories(nested);
      Files.createFile(nested.resolve("file.txt"));

      // WHEN
      FileHelper.deleteFiles(tempDir.resolve("a"));

      // THEN
      assertThat(tempDir.resolve("a")).doesNotExist();
    }
  }

  @Nested
  @DisplayName("findChildren Tests")
  class FindChildrenTests {

    @Test
    @DisplayName("Should find all regular files in directory")
    void shouldFindAllFiles(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Files.createFile(tempDir.resolve("file1.txt"));
      Files.createFile(tempDir.resolve("file2.txt"));
      Path subDir = tempDir.resolve("subdir");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("file3.txt"));

      // WHEN
      List<Path> children = FileHelper.findChildren(tempDir);

      // THEN
      assertThat(children)
          .hasSize(3)
          .extracting(Path::toString)
          .containsExactlyInAnyOrder(
              "file1.txt", "file2.txt", "subdir" + File.separator + "file3.txt");
    }

    @Test
    @DisplayName("Should return empty list for non-directory")
    void shouldReturnEmptyListForNonDirectory(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path file = tempDir.resolve("file.txt");
      Files.createFile(file);

      // WHEN
      List<Path> children = FileHelper.findChildren(file);

      // THEN
      assertThat(children).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty directory")
    void shouldReturnEmptyListForEmptyDirectory(@TempDir Path tempDir) {
      // GIVEN & WHEN
      List<Path> children = FileHelper.findChildren(tempDir);

      // THEN
      assertThat(children).isEmpty();
    }
  }

  @Nested
  @DisplayName("findChildrenAbsolute Tests")
  class FindChildrenAbsoluteTests {

    @Test
    @DisplayName("Should return absolute paths for all files")
    void shouldReturnAbsolutePaths(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Files.createFile(tempDir.resolve("file1.txt"));
      Files.createFile(tempDir.resolve("file2.txt"));

      // WHEN
      List<Path> children = FileHelper.findChildrenAbsolute(tempDir);

      // THEN
      assertThat(children)
          .hasSize(2)
          .allMatch(Path::isAbsolute)
          .extracting(path -> path.getFileName().toString())
          .containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }
  }

  @Nested
  @DisplayName("toAbsolute Tests")
  class ToAbsoluteTests {

    @Test
    @DisplayName("Should convert relative paths to absolute")
    void shouldConvertToAbsolute(@TempDir Path tempDir) {
      // GIVEN
      List<Path> relativePaths = List.of(Path.of("file1.txt"), Path.of("subdir", "file2.txt"));

      // WHEN
      List<Path> absolutePaths = FileHelper.toAbsolute(tempDir, relativePaths).toList();

      // THEN
      assertThat(absolutePaths)
          .hasSize(2)
          .allMatch(Path::isAbsolute)
          .containsExactly(
              tempDir.resolve("file1.txt"), tempDir.resolve("subdir").resolve("file2.txt"));
    }
  }

  @Nested
  @DisplayName("copyFilesToDestination Tests")
  class CopyFilesToDestinationTests {

    @Test
    @DisplayName("Should copy multiple files to destination")
    void shouldCopyMultipleFiles(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceDir = tempDir.resolve("source");
      Path destDir = tempDir.resolve("dest");
      Files.createDirectories(sourceDir);
      Files.createDirectories(destDir);

      File file1 = sourceDir.resolve("file1.txt").toFile();
      File file2 = sourceDir.resolve("file2.txt").toFile();
      Files.writeString(file1.toPath(), "content1");
      Files.writeString(file2.toPath(), "content2");

      // WHEN
      FileHelper.copyFilesToDestination(destDir.toFile(), List.of(file1, file2));

      // THEN
      assertThat(destDir.resolve("file1.txt")).exists().hasContent("content1");
      assertThat(destDir.resolve("file2.txt")).exists().hasContent("content2");
    }

    @Test
    @DisplayName("Should replace existing files")
    void shouldReplaceExistingFiles(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path sourceDir = tempDir.resolve("source");
      Path destDir = tempDir.resolve("dest");
      Files.createDirectories(sourceDir);
      Files.createDirectories(destDir);

      File sourceFile = sourceDir.resolve("file.txt").toFile();
      Files.writeString(sourceFile.toPath(), "new content");

      Path existingFile = destDir.resolve("file.txt");
      Files.writeString(existingFile, "old content");

      // WHEN
      FileHelper.copyFilesToDestination(destDir.toFile(), List.of(sourceFile));

      // THEN
      assertThat(existingFile).hasContent("new content");
    }
  }

  @Nested
  @DisplayName("unzip Tests")
  class UnzipTests {

    @Test
    @DisplayName("Should extract zip file successfully")
    void shouldExtractZipFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path zipFile = createTestZipFile(tempDir, "file1.txt", "file2.txt");
      Path outputDir = tempDir.resolve("output");
      Files.createDirectories(outputDir);

      // WHEN
      FileHelper.unzip(zipFile, outputDir);

      // THEN
      assertThat(outputDir.resolve("file1.txt")).exists().hasContent("Content of file1.txt");
      assertThat(outputDir.resolve("file2.txt")).exists().hasContent("Content of file2.txt");
    }

    @Test
    @DisplayName("Should create directories from zip entries")
    void shouldCreateDirectories(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path zipFile = createTestZipFileWithDirectory(tempDir);
      Path outputDir = tempDir.resolve("output");
      Files.createDirectories(outputDir);

      // WHEN
      FileHelper.unzip(zipFile, outputDir);

      // THEN
      assertThat(outputDir.resolve("subdir")).exists().isDirectory();
      assertThat(outputDir.resolve("subdir").resolve("nested.txt"))
          .exists()
          .hasContent("Nested content");
    }

    @Test
    @DisplayName("Should prevent Zip Slip attack by throwing exception")
    void shouldPreventZipSlip(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path zipFile = createMaliciousZipFile(tempDir);
      Path outputDir = tempDir.resolve("output");
      Files.createDirectories(outputDir);

      // WHEN & THEN
      assertThatThrownBy(() -> FileHelper.unzip(zipFile, outputDir))
          .isInstanceOf(UncheckedIOException.class)
          .hasMessageContaining("ZIP extraction failed")
          .hasCauseInstanceOf(IOException.class)
          .cause()
          .hasMessageContaining("Entry is outside of the target dir");

      // Verify malicious file was not created outside output directory
      assertThat(tempDir.resolve("malicious.txt")).doesNotExist();
      assertThat(outputDir.getParent().resolve("malicious.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Should prevent extraction when entry count exceeds limit")
    void shouldPreventExtractionWhenEntryCountExceedsLimit(@TempDir Path tempDir)
        throws IOException {
      // GIVEN
      Path zipFile = createZipWithTooManyEntries(tempDir);
      Path outputDir = tempDir.resolve("output");
      Files.createDirectories(outputDir);

      // WHEN & THEN
      assertThatThrownBy(() -> FileHelper.unzip(zipFile, outputDir))
          .isInstanceOf(UncheckedIOException.class)
          .hasMessageContaining("ZIP extraction failed")
          .hasCauseInstanceOf(IOException.class)
          .cause()
          .hasMessageContaining("too many entries");
    }

    @Test
    @DisplayName("Should detect zip bomb through total size limit")
    void shouldDetectZipBombByTotalSize(@TempDir Path tempDir) throws IOException {
      // GIVEN - Create zip exceeding 1GB limit
      Path zipFile = createLargeZip(tempDir);
      Path outputDir = tempDir.resolve("output");
      Files.createDirectories(outputDir);

      // WHEN & THEN
      assertThatThrownBy(() -> FileHelper.unzip(zipFile, outputDir))
          .isInstanceOf(UncheckedIOException.class)
          .hasMessageContaining("ZIP extraction failed")
          .hasCauseInstanceOf(IOException.class)
          .cause()
          .hasMessageContaining("too large when uncompressed");
    }

    private Path createLargeZip(Path tempDir) throws IOException {
      Path zipFile = tempDir.resolve("large.zip");

      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
        // Create 11 files of 100 MB each = 1.1 GB total (exceeds 1 GB limit)
        byte[] chunk = new byte[10_000_000]; // 10 MB chunks
        Arrays.fill(chunk, (byte) 'X');

        for (int fileNum = 0; fileNum < 11; fileNum++) {
          ZipEntry entry = new ZipEntry("largefile" + fileNum + ".txt");
          zos.putNextEntry(entry);

          // Write 10 chunks = 100 MB per file
          for (int i = 0; i < 10; i++) {
            zos.write(chunk);
          }
          zos.closeEntry();
        }
      }

      return zipFile;
    }

    private Path createTestZipFile(Path tempDir, String... fileNames) throws IOException {
      Path zipFile = tempDir.resolve("test.zip");
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
        for (String fileName : fileNames) {
          ZipEntry entry = new ZipEntry(fileName);
          zos.putNextEntry(entry);
          zos.write(("Content of " + fileName).getBytes());
          zos.closeEntry();
        }
      }
      return zipFile;
    }

    private Path createTestZipFileWithDirectory(Path tempDir) throws IOException {
      Path zipFile = tempDir.resolve("test.zip");
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
        // Add directory entry
        ZipEntry dirEntry = new ZipEntry("subdir/");
        zos.putNextEntry(dirEntry);
        zos.closeEntry();

        // Add file in directory
        ZipEntry fileEntry = new ZipEntry("subdir/nested.txt");
        zos.putNextEntry(fileEntry);
        zos.write("Nested content".getBytes());
        zos.closeEntry();
      }
      return zipFile;
    }

    private Path createMaliciousZipFile(Path tempDir) throws IOException {
      Path zipFile = tempDir.resolve("malicious.zip");
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
        // Try to escape with ../
        ZipEntry entry = new ZipEntry("../malicious.txt");
        zos.putNextEntry(entry);
        zos.write("Malicious content".getBytes());
        zos.closeEntry();
      }
      return zipFile;
    }

    private Path createZipWithTooManyEntries(Path tempDir) throws IOException {
      Path zipFile = tempDir.resolve("manyentries.zip");

      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
        // Create more entries than MAX_ENTRY_COUNT (10,000 + 1)
        for (int i = 0; i < 10_001; i++) {
          ZipEntry entry = new ZipEntry("file" + i + ".txt");
          zos.putNextEntry(entry);
          zos.write(("content" + i).getBytes(StandardCharsets.UTF_8));
          zos.closeEntry();
        }
      }

      return zipFile;
    }
  }

  @Nested
  @DisplayName("moveFile Tests")
  class MoveFileTests {

    @Test
    @DisplayName("Should move file successfully")
    void shouldMoveFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path source = tempDir.resolve("source.txt");
      Path target = tempDir.resolve("subdir").resolve("target.txt");
      Files.writeString(source, "content");

      // WHEN
      boolean result = FileHelper.moveFile(source, target);

      // THEN
      assertThat(result).isTrue();
      assertThat(source).doesNotExist();
      assertThat(target).exists().hasContent("content");
    }

    @Test
    @DisplayName("Should create parent directories when moving")
    void shouldCreateParentDirectories(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path source = tempDir.resolve("source.txt");
      Path target = tempDir.resolve("a").resolve("b").resolve("c").resolve("target.txt");
      Files.writeString(source, "content");

      // WHEN
      boolean result = FileHelper.moveFile(source, target);

      // THEN
      assertThat(result).isTrue();
      assertThat(target.getParent()).exists().isDirectory();
      assertThat(target).exists();
    }

    @Test
    @DisplayName("Should replace existing file")
    void shouldReplaceExistingFile(@TempDir Path tempDir) throws IOException {
      // GIVEN
      Path source = tempDir.resolve("source.txt");
      Path target = tempDir.resolve("target.txt");
      Files.writeString(source, "new content");
      Files.writeString(target, "old content");

      // WHEN
      boolean result = FileHelper.moveFile(source, target);

      // THEN
      assertThat(result).isTrue();
      assertThat(target).hasContent("new content");
    }

    @Test
    @DisplayName("Should return false when source does not exist")
    void shouldReturnFalseWhenSourceNotExists(@TempDir Path tempDir) {
      // GIVEN
      Path source = tempDir.resolve("nonexistent.txt");
      Path target = tempDir.resolve("target.txt");

      // WHEN
      boolean result = FileHelper.moveFile(source, target);

      // THEN
      assertThat(result).isFalse();
    }
  }
}
