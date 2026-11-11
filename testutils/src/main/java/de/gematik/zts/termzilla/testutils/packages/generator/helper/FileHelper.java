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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility helper for simple file and stream operations required during FHIR package processing.
 * Provides methods to ensure writable directories exist, copy byte streams or files, delete
 * directory trees and close {@link Closeable} resources quietly without surfacing secondary errors.
 * All operations are synchronous and intentionally minimal to keep IO predictable.
 */
@Slf4j
public class FileHelper {

  /** Hidden constructor to prevent instantiation of utility class. */
  private FileHelper() {}

  /**
   * Closes the provided {@link Closeable} suppressing any {@link IOException}. Intended for use in
   * {@code finally} blocks where failure to close should not abort the primary operation.
   *
   * @param closeable resource to close; ignored if {@code null}
   */
  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException _) {
        log.debug("IOException while closing Closable");
      }
    }
  }

  /**
   * Ensures the given {@code destination} exists as a writable directory. Creates all missing
   * intermediate directories if necessary. Rejects existing regular files at the provided path and
   * non-writable directories.
   *
   * @param destination target directory path
   * @throws IllegalArgumentException if path points to a regular file or is not writable
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
   * Copies all bytes from {@code input} to {@code output} using an internal buffer.
   *
   * @param input source stream
   * @param output destination stream
   * @return number of bytes copied
   * @throws IOException if reading or writing fails
   */
  public static long copy(final InputStream input, final OutputStream output) throws IOException {
    final byte[] buffer = new byte[8024];
    int n;
    long count = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copies all bytes from {@code source} stream to the file {@code destination}. The destination
   * file is created or truncated if it already exists. Stream is closed after completion.
   *
   * @param source input stream supplying bytes
   * @param destination target file path
   * @throws IOException if writing fails
   */
  public static void copy(InputStream source, Path destination) throws IOException {
    OutputStream output = null;

    try {
      output = Files.newOutputStream(destination);
      copy(source, output);
    } finally {
      closeQuietly(output);
    }
  }

  /**
   * Copies the file {@code file} into the directory {@code targetDir}, creating the directory tree
   * if needed. Returns the full path of the copied file.
   *
   * @param targetDir destination directory
   * @param file source file to copy
   * @return path to the copied file inside {@code targetDir}
   * @throws IOException if copying fails
   */
  public static Path copy(Path targetDir, Path file) throws IOException {
    Files.createDirectories(targetDir);
    Path targetPath = targetDir.resolve(file.getFileName());
    return Files.copy(file, targetPath);
  }

  /**
   * Recursively deletes the file or directory tree rooted at {@code fileToBeDeleted}. Non-deleted
   * files (e.g., due to OS locks) are logged as warnings; processing continues for remaining
   * entries.
   *
   * @param fileToBeDeleted root path to delete
   * @throws IOException if walking the file tree fails
   */
  public static void deleteFiles(Path fileToBeDeleted) throws IOException {

    Path rootPath = fileToBeDeleted.toAbsolutePath();

    try (Stream<Path> walk = Files.walk(rootPath)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (IOException _) {
                  log.warn("Could not delete file: {}", path.toAbsolutePath());
                }
              });
    }
  }
}
