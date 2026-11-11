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
package de.gematik.zts.termzilla.testutils.packages.generator.helper;

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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Provides static helper methods for gzip compression/decompression of single files, creation of
 * tar archives and extraction of tar archives. Centralizes validation of {@link Path} arguments
 * (existence, readability, writability) and stream/resource handling to reduce boilerplate in
 * callers and ensure consistent error signaling. Non-instantiable utility class.
 */
@Slf4j
public class ArchiveAndCompressionHelper {

  /** Hidden constructor prevents instantiation of this utility class. */
  private ArchiveAndCompressionHelper() {}

  /**
   * Compresses the given readable regular file using gzip into {@code destination}. Destination
   * must not be a directory and must be writable (or creatable).
   *
   * @param source existing readable non-directory file to compress
   * @param destination target file path for gzip output
   * @throws IllegalArgumentException if source/destination validation fails
   * @throws IOException if an I/O error occurs during compression
   */
  public static void compress(Path source, Path destination)
      throws IllegalArgumentException, IOException {

    ArchiveAndCompressionHelper.assertSource(source);
    ArchiveAndCompressionHelper.assertDestination(destination);

    if (Files.isDirectory(destination)) {
      throw new IllegalArgumentException("Destination is directory");
    }

    try (BufferedInputStream input = new BufferedInputStream(Files.newInputStream(source))) {
      try (CompressorOutputStream<OutputStream> compressed =
          new GzipCompressorOutputStream(Files.newOutputStream(destination))) {
        FileHelper.copy(input, compressed);
      }
    }
  }

  /**
   * Decompresses a gzip file. If {@code destination} is a directory a file name derived from the
   * source (without {@code .tgz}) is used inside that directory.
   *
   * @param source gzip-compressed file
   * @param destination output file path or writable directory
   * @return resulting decompressed file path
   * @throws IllegalArgumentException if validation fails
   * @throws IOException if an I/O error occurs
   */
  public static Path decompress(Path source, Path destination) throws IOException {
    ArchiveAndCompressionHelper.assertSource(source);
    ArchiveAndCompressionHelper.assertDestination(destination);

    if (Files.isDirectory(destination)) {
      String decompressedFilename = ArchiveAndCompressionHelper.getDecompressedFilename(source);
      destination = destination.resolve(decompressedFilename);
    }

    try (CompressorInputStream compressed =
        new GzipCompressorInputStream(Files.newInputStream(source))) {
      try (OutputStream output = Files.newOutputStream(destination)) {
        FileHelper.copy(compressed, output);
        return destination;
      }
    }
  }

  /**
   * Derives the decompressed filename by removing the {@code .tgz} suffix.
   *
   * @param source compressed file path
   * @return filename without {@code .tgz}
   */
  private static String getDecompressedFilename(Path source) {
    String fileName = source.getFileName().toString();
    return fileName.substring(0, fileName.length() - ".tgz".length());
  }

  /**
   * Extracts a {@code .tar} archive into the target directory, creating necessary directories.
   * Existing files may be overwritten.
   *
   * @param archive tar archive file
   * @param destination directory for extracted content (created if missing)
   * @throws IllegalArgumentException if archive validation fails
   * @throws IOException if extraction fails
   */
  public static void unTar(Path archive, Path destination) throws IOException {

    ArchiveAndCompressionHelper.assertUntarSource(archive);
    FileHelper.requireDirectory(destination);

    try (ArchiveInputStream<?> input =
        new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
      ArchiveAndCompressionHelper.unTar(input, destination);
    }
  }

  /**
   * Iteratively extracts all entries from the given archive stream into {@code destination}.
   * Directories are created as needed.
   *
   * @param input archive input stream
   * @param destination directory receiving extracted entries
   * @throws IOException if reading or writing fails
   */
  private static void unTar(ArchiveInputStream<?> input, Path destination) throws IOException {
    ArchiveEntry entry;
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
   * Creates a tar archive file with the provided source files/directories (recursively included).
   *
   * @param archive filename of the archive to create (relative to {@code destination})
   * @param destination directory to place the archive file
   * @param sourceFiles files or directories to include; must exist & be readable
   * @return path of created archive file
   * @throws IOException if archive creation or writing fails
   */
  public static Path createTarArchive(String archive, Path destination, Path... sourceFiles)
      throws IOException {

    // check if destination is a directory
    FileHelper.requireDirectory(destination);

    // create new file in destination directory
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
   * Ensures the archive file exists; silently ignores if it already exists.
   *
   * @param archiveFile archive file path
   * @throws IOException if creation fails for other reasons
   */
  private static void ensureExists(Path archiveFile) throws IOException {
    try {
      Files.createFile(archiveFile);
    } catch (FileAlreadyExistsException _) {
      // swallowing exception, we don't care
    }
  }

  /**
   * Adds each provided source file or directory recursively to the tar archive.
   *
   * @param sourceFiles root files/directories to archive
   * @param tarArchive output tar stream
   * @throws IOException if I/O errors occur reading sources or writing entries
   */
  private static void writeToArchive(Path[] sourceFiles, TarArchiveOutputStream tarArchive)
      throws IOException {

    for (Path sourceFile : sourceFiles) {
      if (!Files.exists(sourceFile)) {
        throw new FileNotFoundException(sourceFile.toString());
      } else if (!Files.isReadable(sourceFile)) {
        throw new FileNotFoundException(sourceFile + " (Permission denied)");
      }

      writeToArchive(sourceFile.getParent(), Stream.of(sourceFile), tarArchive);
    }
  }

  /**
   * Recursively writes sources to the archive using names relative to {@code parent}.
   *
   * @param parent base path used to compute relative entry names
   * @param sources stream of source files/directories
   * @param archive target archive output stream
   * @param <E> concrete {@link ArchiveEntry} type of the archive
   * @throws IOException if entry creation or data transfer fails
   */
  private static <E extends ArchiveEntry> void writeToArchive(
      Path parent, Stream<Path> sources, ArchiveOutputStream<E> archive) throws IOException {
    for (Path source : sources.toList()) {

      String relativePath = parent.relativize(source).toString();

      createArchiveEntry(source, relativePath, archive);

      if (Files.isDirectory(source)) {
        writeToArchive(parent, Files.list(source), archive);
      }
    }
  }

  /**
   * Creates an archive entry for {@code file} and copies its content if it is a regular file.
   *
   * @param file file or directory to add
   * @param entryName relative entry name inside the archive
   * @param archive archive output stream
   * @param <E> concrete {@link ArchiveEntry} type
   * @throws IOException if I/O errors occur
   */
  private static <E extends ArchiveEntry> void createArchiveEntry(
      Path file, String entryName, ArchiveOutputStream<E> archive) throws IOException {
    E entry = archive.createArchiveEntry(file, entryName);
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
   * Validates that {@code source} is an existing, readable regular file.
   *
   * @param source path to validate
   * @throws IllegalArgumentException if {@code source} is {@code null}, a directory or unreadable
   * @throws FileNotFoundException if the file does not exist
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
   * Validates that {@code destination} is writable (for directories or existing files).
   * Non-existing files are assumed creatable.
   *
   * @param destination target path
   * @throws IllegalArgumentException if {@code destination} is {@code null} or not writable
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
   * Validates the tar archive path before extraction.
   *
   * @param tarArchive archive file path
   * @throws FileNotFoundException if missing
   * @throws IllegalArgumentException if path is a directory or unreadable
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
