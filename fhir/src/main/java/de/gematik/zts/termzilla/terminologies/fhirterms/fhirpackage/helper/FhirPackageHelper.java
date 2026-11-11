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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;

/**
 * Helper class for FHIR package operations including compression, decompression, and TAR archive
 * handling.
 */
@Slf4j
public class FhirPackageHelper {

  private FhirPackageHelper() {}

  /**
   * Compresses a file using GZIP compression.
   *
   * @param source the file to compress
   * @param destination the target path for the compressed file
   * @throws IllegalArgumentException if source or destination is invalid
   * @throws IOException if an I/O error occurs during compression
   */
  public static void compress(Path source, Path destination)
      throws IllegalArgumentException, IOException {

    FhirPackageHelper.assertSource(source);
    FhirPackageHelper.assertDestination(destination);

    if (Files.isDirectory(destination)) {
      throw new IllegalArgumentException("Destination is directory");
    }

    try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(source));
        GzipCompressorOutputStream compressed =
            new GzipCompressorOutputStream(Files.newOutputStream(destination))) {

      FileHelper.copy(input, compressed);
    }
  }

  /**
   * Decompresses a GZIP compressed file. If destination is a directory, the decompressed file will
   * be created within it using the source filename without extension.
   *
   * @param source the compressed file to decompress
   * @param destination the target path or directory for the decompressed file
   * @return the path to the decompressed file
   * @throws IOException if an I/O error occurs during decompression
   */
  public static Path decompress(Path source, Path destination) throws IOException {
    FhirPackageHelper.assertSource(source);
    FhirPackageHelper.assertDestination(destination);

    if (Files.isDirectory(destination)) {
      String decompressedFilename = FhirPackageHelper.getDecompressedFilename(source);
      destination = destination.resolve(decompressedFilename);
    }

    try (GzipCompressorInputStream compressed =
            new GzipCompressorInputStream(Files.newInputStream(source));
        OutputStream output = Files.newOutputStream(destination)) {

      FileHelper.copy(compressed, output);
      return destination;
    }
  }

  /**
   * Derives the decompressed filename by removing the file extension.
   *
   * @param source the source file path
   * @return the filename without extension
   */
  private static String getDecompressedFilename(Path source) {
    return FilenameUtils.removeExtension(source.getFileName().toString());
  }

  /**
   * Extracts a TAR archive to the specified destination directory.
   *
   * @param archive the TAR archive to extract
   * @param destination the target directory for extraction
   * @throws IOException if an I/O error occurs during extraction
   */
  public static void unTar(Path archive, Path destination) throws IOException {

    FhirPackageHelper.assertUntarSource(archive);
    FileHelper.requireDirectory(destination);

    try (TarArchiveInputStream input =
        new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
      FhirPackageHelper.unTar(input, destination);
    }
  }

  /**
   * Extracts entries from a TAR archive input stream to the destination directory. Creates
   * directories as needed and preserves the archive structure.
   *
   * @param input the TAR archive input stream
   * @param destination the target directory for extraction
   * @throws IOException if an I/O error occurs during extraction
   */
  private static void unTar(TarArchiveInputStream input, Path destination) throws IOException {
    TarArchiveEntry entry;
    Path normalizedDestinationPath = destination.toAbsolutePath().normalize();

    while ((entry = input.getNextEntry()) != null) {
      Path file = normalizedDestinationPath.resolve(entry.getName()).normalize();

      if (!file.startsWith(normalizedDestinationPath)) {
        throw new IOException("ZIP entry is outside the destination directory: " + entry.getName());
      }

      if (entry.isDirectory()) {
        Files.createDirectories(file);
      } else {
        Files.createDirectories(file.getParent());
        FileHelper.copy(input, file);
      }
    }
  }

  /**
   * Creates a TAR archive containing the specified source files.
   *
   * @param archive the name of the archive file to create
   * @param destination the directory where the archive will be created
   * @param sourceFiles the files to include in the archive
   * @return the path to the created archive file
   * @throws IOException if an I/O error occurs during archive creation
   */
  public static Path createTarArchive(String archive, Path destination, Path... sourceFiles)
      throws IOException {

    FileHelper.requireDirectory(destination);

    Path archiveFile = destination.resolve(archive);
    ensureExists(archiveFile);

    try (TarArchiveOutputStream outputStream =
        new TarArchiveOutputStream(Files.newOutputStream(archiveFile))) {
      outputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

      writeToArchive(sourceFiles, outputStream);

      outputStream.flush();
    }

    return archiveFile;
  }

  /**
   * Ensures the archive file exists, creating it if necessary. Ignores {@link
   * FileAlreadyExistsException} if the file already exists.
   *
   * @param archiveFile the archive file path
   * @throws IOException if an I/O error occurs during file creation
   */
  private static void ensureExists(Path archiveFile) throws IOException {
    try {
      Files.createFile(archiveFile);
    } catch (FileAlreadyExistsException _) {
      // swallowing exception, we don't care
    }
  }

  /**
   * Writes the specified source files to the TAR archive.
   *
   * @param sourceFiles the files to add to the archive
   * @param tarArchive the TAR archive output stream
   * @throws IOException if an I/O error occurs or a source file is not found or readable
   */
  private static void writeToArchive(Path[] sourceFiles, TarArchiveOutputStream tarArchive)
      throws IOException {

    for (Path sourceFile : sourceFiles) {
      if (!Files.exists(sourceFile)) {
        throw new FileNotFoundException(sourceFile.toString());
      } else if (!Files.isReadable(sourceFile)) {
        throw new FileNotFoundException(sourceFile.toString() + " (Permission denied)");
      }

      writeToArchive(sourceFile.getParent(), Stream.of(sourceFile), tarArchive);
    }
  }

  /**
   * Recursively writes source paths to the archive, preserving directory structure relative to the
   * parent path.
   *
   * @param parent the parent path used for calculating relative paths
   * @param sources the stream of source paths to add
   * @param archive the TAR archive output stream
   * @throws IOException if an I/O error occurs during writing
   */
  private static void writeToArchive(
      Path parent, Stream<Path> sources, TarArchiveOutputStream archive) throws IOException {
    for (Path source : sources.toList()) {

      String relativePath = parent.relativize(source).toString();

      createArchiveEntry(source, relativePath, archive);

      if (Files.isDirectory(source)) {
        writeToArchive(parent, Files.list(source), archive);
      }
    }
  }

  /**
   * Creates and writes a single archive entry for the given file. For non-directory entries, copies
   * the file content to the archive.
   *
   * @param file the file to add to the archive
   * @param entryName the entry name within the archive
   * @param archive the TAR archive output stream
   * @throws IOException if an I/O error occurs during entry creation
   */
  private static void createArchiveEntry(
      Path file, String entryName, TarArchiveOutputStream archive) throws IOException {
    var entry = archive.createArchiveEntry(file, entryName);
    archive.putArchiveEntry(entry);

    if (!entry.isDirectory()) {
      InputStream input = null;
      try {
        input = Files.newInputStream(file);
        FileHelper.copy(input, archive);
      } finally {
        FileHelper.closeQuietly(input);
      }
    }

    archive.closeArchiveEntry();
  }

  /**
   * Validates that the source path is a readable file.
   *
   * @param source the source path to validate
   * @throws IllegalArgumentException if source is null, a directory, or not readable
   * @throws FileNotFoundException if source does not exist
   */
  private static void assertSource(Path source)
      throws IllegalArgumentException, FileNotFoundException {
    if (source == null) {
      throw new IllegalArgumentException("Source is null");
    } else if (Files.isDirectory(source)) {
      throw new IllegalArgumentException("Source " + source + " is a directory.");
    } else if (!Files.exists(source)) {
      throw new FileNotFoundException(source.toString());
    } else if (!Files.isReadable(source)) {
      throw new IllegalArgumentException("Can not read from source " + source);
    }
  }

  /**
   * Validates that the destination path is writable.
   *
   * @param destination the destination path to validate
   * @throws IllegalArgumentException if destination is null or not writable
   */
  private static void assertDestination(Path destination) {
    if (destination == null) {
      throw new IllegalArgumentException("Destination is null");
    } else if (Files.isDirectory(destination)) {
      if (!Files.isWritable(destination)) {
        throw new IllegalArgumentException("Can not write to destination " + destination);
      }
    } else if (Files.exists(destination) && !Files.isWritable(destination)) {
      throw new IllegalArgumentException("Can not write to destination " + destination);
    }
  }

  /**
   * Validates that the TAR archive source is a readable file.
   *
   * @param tarArchive the TAR archive path to validate
   * @throws FileNotFoundException if the archive does not exist
   * @throws IllegalArgumentException if the archive is a directory or not readable
   */
  private static void assertUntarSource(Path tarArchive)
      throws FileNotFoundException, IllegalArgumentException {
    if (Files.isDirectory(tarArchive)) {
      throw new IllegalArgumentException(
          "Unable to untar " + tarArchive + ". Source is a directory.");
    } else if (!Files.exists(tarArchive)) {
      throw new FileNotFoundException(tarArchive.toString());
    } else if (!Files.isReadable(tarArchive)) {
      throw new IllegalArgumentException(
          "Unable to untar " + tarArchive + ". Can not read from source.");
    }
  }
}
