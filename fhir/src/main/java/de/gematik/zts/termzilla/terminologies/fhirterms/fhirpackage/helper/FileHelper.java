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

import static java.util.function.Function.identity;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing file system operations including temporary file creation, copying,
 * deletion, and ZIP archive handling.
 */
@Slf4j
public class FileHelper {

  private static final Pattern ILLEGAL_FILE_CHARS_PATTERN = Pattern.compile("[\\\\/:\"*?<>|]");
  private static final String TEMPORARY_FILE_PREFIX = "termzilla";
  private static final int BUFFER_SIZE = 8024;

  private static final long MAX_UNCOMPRESSED_SIZE = 1_000_000_000L; // 1 GB
  private static final int MAX_ENTRY_COUNT = 10_000;
  private static final double MAX_COMPRESSION_RATIO = 100.0;

  private FileHelper() {}

  /**
   * Creates a temporary directory with the default prefix.
   *
   * @return path to the created temporary directory
   * @throws UncheckedIOException if directory creation fails
   */
  public static Path createTemporaryDirectory() {
    return createTemporaryDirectory(TEMPORARY_FILE_PREFIX);
  }

  /**
   * Creates a temporary directory with the specified prefix. Illegal file name characters in the
   * prefix are replaced with underscore.
   *
   * @param prefix the file name prefix for the temporary directory
   * @return path to the created temporary directory
   * @throws UncheckedIOException if directory creation fails
   */
  @SuppressWarnings("java:S5443")
  public static Path createTemporaryDirectory(String prefix) {
    String actualPrefix = ILLEGAL_FILE_CHARS_PATTERN.matcher(prefix).replaceAll("_");
    try {
      // Create temp directory with restrictive permissions from the start
      FileAttribute<Set<PosixFilePermission>> attrs =
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

      return Files.createTempDirectory(actualPrefix, attrs);

    } catch (UnsupportedOperationException _) {
      // POSIX not supported (e.g., Windows) - fallback to default creation
      try {
        return Files.createTempDirectory(actualPrefix);
      } catch (IOException ioException) {
        throw new UncheckedIOException(
            "Failed during creation of a temporary directory.", ioException);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed during creation of a temporary directory.", e);
    }
  }

  /**
   * Creates a temporary file with the specified suffix.
   *
   * @param suffix the file suffix (e.g., {@code .zip} or {@code .json})
   * @return path to the created temporary file
   * @throws UncheckedIOException if file creation fails
   */
  @SuppressWarnings("java:S5443")
  public static Path createTemporaryFile(String suffix) {
    try {
      FileAttribute<Set<PosixFilePermission>> attrs =
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

      return Files.createTempFile(TEMPORARY_FILE_PREFIX, suffix, attrs);

    } catch (UnsupportedOperationException _) {
      // POSIX not supported - fallback to default
      try {
        return Files.createTempFile(TEMPORARY_FILE_PREFIX, suffix);
      } catch (IOException ioException) {
        throw new UncheckedIOException("Failed during creation of a temporary file.", ioException);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed during creation of a temporary file.", e);
    }
  }

  /**
   * Closes a {@link Closeable} resource, suppressing any {@link IOException}.
   *
   * @param closeable the resource to close, may be null
   */
  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        log.debug("Exception occurred while closing resource", e);
      }
    }
  }

  /**
   * Ensures the destination path is a writable directory, creating it if necessary.
   *
   * @param destination the directory path to validate or create
   * @throws IllegalArgumentException if destination is a file or not writable
   * @throws IOException if directory creation fails
   */
  public static void requireDirectory(Path destination)
      throws IllegalArgumentException, IOException {
    if (Files.isRegularFile(destination)) {
      throw new IllegalArgumentException(
          destination + " exists and is a file, directory or path expected.");
    } else if (!Files.exists(destination)) {
      Files.createDirectories(destination);
    }

    if (!Files.isWritable(destination)) {
      throw new IllegalArgumentException("Can not write to destination " + destination);
    }
  }

  /**
   * Calculates the relative path from root to node.
   *
   * @param root the root directory
   * @param node the target file or directory
   * @return the relative path from root to node
   * @throws IOException if canonical paths cannot be resolved
   */
  public static String relativePath(File root, File node) throws IOException {
    String rootPath = root.getCanonicalPath();
    String nodePath = node.getCanonicalPath();

    return nodePath.substring(rootPath.length() + 1);
  }

  /**
   * Copies data from an input stream to an output stream.
   *
   * @param input the source input stream
   * @param output the target output stream
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs during copying
   */
  public static long copy(final InputStream input, final OutputStream output) throws IOException {
    final byte[] buffer = new byte[BUFFER_SIZE];
    int n;
    long count = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copies data from an input stream to a file at the specified destination.
   *
   * @param source the source input stream
   * @param destination the target file path
   * @throws IOException if an I/O error occurs during copying
   */
  public static void copy(InputStream source, Path destination) throws IOException {
    try (OutputStream output = Files.newOutputStream(destination)) {
      copy(source, output);
    }
  }

  /**
   * Returns an array of files contained in the source. If source is a directory, returns its
   * children; otherwise returns the source itself.
   *
   * @param source the source file or directory
   * @return array of contained files
   */
  public static File[] filesContainedIn(File source) {
    if (source.isDirectory()) {
      return source.listFiles();
    } else {
      return new File[] {source};
    }
  }

  /**
   * Recursively deletes a file or directory and all its contents.
   *
   * @param fileToBeDeleted the file or directory to delete
   * @throws IOException if deletion fails
   */
  public static void deleteFiles(Path fileToBeDeleted) throws IOException {

    Path rootPath = fileToBeDeleted.toAbsolutePath();

    try (Stream<Path> walk = Files.walk(rootPath)) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  /**
   * Finds all regular files in the given directory, returning paths relative to {@code dir}.
   *
   * @param dir the directory to search
   * @return list of relative paths to regular files, or empty list if dir is not a directory
   * @throws UncheckedIOException if reading children fails
   */
  public static List<Path> findChildren(Path dir) {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    var absRoot = dir.toAbsolutePath();
    return mapChildrenAbsolute(absRoot, absRoot::relativize);
  }

  /**
   * Finds all regular files in the given directory, returning absolute paths.
   *
   * @param dir the directory to search
   * @return list of absolute paths to regular files, or empty list if dir is not a directory
   * @throws UncheckedIOException if reading children fails
   */
  public static List<Path> findChildrenAbsolute(Path dir) {
    return mapChildrenAbsolute(dir, identity());
  }

  /**
   * Maps all regular files in a directory using the provided mapper function.
   *
   * @param dir the directory to search
   * @param mapper the function to apply to each absolute file path
   * @param <T> the type of the mapped result
   * @return list of mapped results
   * @throws UncheckedIOException if reading children fails
   */
  private static <T> List<T> mapChildrenAbsolute(Path dir, Function<Path, T> mapper) {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    var absRoot = dir.toAbsolutePath();
    try (var children = Files.walk(absRoot)) {
      return children.filter(Files::isRegularFile).map(Path::toAbsolutePath).map(mapper).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Converts relative paths to absolute paths by resolving them against the root.
   *
   * @param root the root directory
   * @param relativeToRoot the list of relative paths
   * @return stream of absolute paths
   */
  public static Stream<Path> toAbsolute(Path root, List<Path> relativeToRoot) {
    return relativeToRoot.stream().map(root::resolve);
  }

  /**
   * Copies multiple files to the specified destination directory, replacing existing files.
   *
   * @param directory the target directory
   * @param allFiles the files to copy
   * @throws IOException if copying fails
   */
  public static void copyFilesToDestination(File directory, List<File> allFiles)
      throws IOException {

    for (File currentFile : allFiles) {

      Path sourceFile = Paths.get(currentFile.getAbsolutePath());
      Path destinationFile = Paths.get(directory.getAbsolutePath() + "/" + currentFile.getName());
      Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Copies a file to the target directory, creating directories as needed.
   *
   * @param targetDir the target directory
   * @param file the file to copy
   * @return the path to the copied file
   * @throws IOException if copying fails
   */
  public static Path copy(Path targetDir, Path file) throws IOException {
    Files.createDirectories(targetDir);
    Path targetPath = targetDir.resolve(file.getFileName());
    return Files.copy(file, targetPath);
  }

  /**
   * Extracts a ZIP archive to the specified output directory. Handles both directory and file
   * entries, creating parent directories as needed. Validates entries to prevent Zip Slip
   * vulnerabilities.
   *
   * @param zipFile the ZIP archive to extract
   * @param outputDirectory the target directory for extraction
   */
  @SuppressWarnings(
      "java:S5042") // We control the input files that are processed, therefore this is acceptable
  public static void unzip(Path zipFile, Path outputDirectory) {
    long totalBytesWritten = 0;
    int entryCount = 0;

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {

      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {

        // Check entry count
        entryCount++;
        if (entryCount > MAX_ENTRY_COUNT) {
          throw new IOException("ZIP file contains too many entries (potential zip bomb)");
        }

        // Validate path traversal
        validateZipEntry(zipEntry, outputDirectory);

        // Check compression ratio if metadata is available
        long compressedSize = zipEntry.getCompressedSize();
        long declaredSize = zipEntry.getSize();

        if (declaredSize > 0 && compressedSize > 0) {
          double ratio = (double) declaredSize / compressedSize;
          if (ratio > MAX_COMPRESSION_RATIO) {
            throw new IOException("Entry has suspicious compression ratio: " + zipEntry.getName());
          }
        }

        // Check declared size if available
        if (declaredSize > MAX_UNCOMPRESSED_SIZE) {
          throw new IOException("Single entry exceeds size limit: " + zipEntry.getName());
        }

        // Extract and track actual bytes written
        long bytesWritten = extractZipEntry(zis, zipEntry, outputDirectory);
        totalBytesWritten += bytesWritten;

        // Check total extracted size
        if (totalBytesWritten > MAX_UNCOMPRESSED_SIZE) {
          throw new IOException("ZIP file is too large when uncompressed (potential zip bomb)");
        }

        zis.closeEntry();
      }

    } catch (IOException e) {
      log.error("Failed to unzip {} to destination {}", zipFile.getFileName(), outputDirectory, e);
      throw new UncheckedIOException("ZIP extraction failed", e);
    }
  }

  /**
   * Validates a ZIP entry to prevent Zip Slip vulnerabilities. Ensures the entry will be extracted
   * within the target directory.
   *
   * @param zipEntry the ZIP entry to validate
   * @param outputDirectory the target extraction directory
   * @throws IOException if the entry path is outside the target directory
   */
  private static void validateZipEntry(ZipEntry zipEntry, Path outputDirectory) throws IOException {
    File destFile = new File(outputDirectory.toFile(), zipEntry.getName());
    String destDirPath = outputDirectory.toFile().getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    // Prevent path traversal (Zip Slip)
    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    // Additional: Check for suspicious patterns
    String name = zipEntry.getName();
    if (name.contains("..") || name.startsWith("/") || name.contains("\0")) {
      throw new IOException("Entry has suspicious name: " + name);
    }
  }

  /**
   * Extracts a single ZIP entry to the output directory.
   *
   * @param zis the ZIP input stream
   * @param zipEntry the ZIP entry to extract
   * @param outputDirectory the target directory
   * @return the number of bytes written
   * @throws IOException if extraction fails
   */
  private static long extractZipEntry(ZipInputStream zis, ZipEntry zipEntry, Path outputDirectory)
    throws IOException {
    Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
    Path newPath = normalizedOutputDirectory.resolve(zipEntry.getName()).normalize();

    if (!newPath.startsWith(normalizedOutputDirectory)) {
      throw new IOException("ZIP entry is outside the destination directory: " + zipEntry.getName());
    }

    if (zipEntry.isDirectory()) {
      createDirectory(newPath.toFile());
      return 0;
    } else {
      createParentDirectory(newPath.toFile());
      return writeFileContent(zis, newPath.toFile());
    }
  }

  /**
   * Creates a directory if it doesn't exist.
   *
   * @param directory the directory to create
   * @throws IOException if directory creation fails
   */
  private static void createDirectory(File directory) throws IOException {
    if (!directory.isDirectory() && !directory.mkdirs()) {
      throw new IOException("Failed to create directory " + directory);
    }
  }

  /**
   * Creates the parent directory of a file if it doesn't exist.
   *
   * @param file the file whose parent directory should be created
   * @throws IOException if directory creation fails
   */
  private static void createParentDirectory(File file) throws IOException {
    File parent = file.getParentFile();
    if (!parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Failed to create directory " + parent);
    }
  }

  /**
   * Writes the content from a ZIP input stream to a file.
   *
   * @param zis the ZIP input stream
   * @param file the target file
   * @return the number of bytes written
   * @throws IOException if writing fails
   */
  private static long writeFileContent(ZipInputStream zis, File file) throws IOException {
    byte[] buffer = new byte[1024];
    long totalWritten = 0;

    try (FileOutputStream fos = new FileOutputStream(file)) {
      int len;
      while ((len = zis.read(buffer)) > 0) {
        fos.write(buffer, 0, len);
        totalWritten += len;
      }
    }

    return totalWritten;
  }

  /**
   * Moves a file from source to target, creating parent directories as needed. Replaces existing
   * files at the target location.
   *
   * @param source the source file path
   * @param target the target file path
   * @return {@code true} if the move was successful, {@code false} otherwise
   */
  public static boolean moveFile(Path source, Path target) {

    try {

      Files.createDirectories(target.getParent());

      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

      return true;
    } catch (IOException _) {
      return false;
    }
  }
}
