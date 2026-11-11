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

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Utility class providing file system operations for LOINC release files.
 *
 * <p>Covers locating release ZIP archives and individual release files by name pattern, unpacking
 * ZIP archives, and cleaning up previously extracted content. The file name pattern constants
 * defined here match the standard LOINC release file naming conventions.
 */
@Slf4j
public class LoincReleaseFileHelper {

  private LoincReleaseFileHelper() {
    /* This utility class should not be instantiated */
  }

  public static final String FILE_RF_ZIP_PATTERN = "^Loinc_2\\.\\d{2}\\.zip";
  public static final String FILE_CONCEPTS_PATTERN = "^Loinc\\.csv";
  public static final String FILE_PART_PATTERN = "^Part\\.csv";
  public static final String FILE_PART_HIERARCHY_PATTERN =
      "^(ComponentHierarchyBySystem|MultiAxialHierarchy)\\.csv";
  public static final String FILE_PART_CHANGESNAPSHOT_PATTERN = "^PartChangeSnapshot\\.csv";
  public static final String FILE_LOINC_PARTLINK_PATTERN = "^LoincPartLink_Primary\\.csv";
  public static final String FILE_ANSWERLIST_PATTERN = "^AnswerList\\.csv";

  /**
   * Computes a combined SHA-256 hash over the contents of all LOINC release ZIP files found in the
   * given directory.
   *
   * <p>Release files are located via {@link #getMultipleReleaseFilePaths(String)}. Each file's
   * content is hashed individually, and a final SHA-256 hash is computed over the concatenation of
   * all per-file hashes.
   *
   * @param releaseFileCollectionLocation the root directory to search for LOINC release ZIP files
   * @return a hex-encoded SHA-256 hash representing the combined state of all found release files
   * @throws InvalidConfigurationException if the directory does not exist, cannot be traversed, or
   *     any release file cannot be read
   */
  public static String calculateReleaseFileHash(String releaseFileCollectionLocation)
      throws InvalidConfigurationException {

    List<String> releaseFilePaths = getMultipleReleaseFilePaths(releaseFileCollectionLocation);

    StringBuilder combinedHashes = new StringBuilder();
    for (String releaseFilePath : releaseFilePaths) {
      try (InputStream is = Files.newInputStream(Path.of(releaseFilePath))) {
        combinedHashes.append(DigestUtils.sha256Hex(is));
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            "Could not generate release file hash for path: "
                + releaseFilePath
                + ": "
                + e.getMessage());
      }
    }
    return DigestUtils.sha256Hex(combinedHashes.toString());
  }

  /**
   * Searches recursively for all LOINC release ZIP files within the given directory.
   *
   * @param releaseFileCollectionLocation the root directory to search in
   * @return a sorted list of absolute path strings for all matching ZIP files
   * @throws InvalidConfigurationException if the directory does not exist or cannot be traversed
   */
  public static List<String> getMultipleReleaseFilePaths(String releaseFileCollectionLocation)
      throws InvalidConfigurationException {

    Path workingDirectory = Path.of(releaseFileCollectionLocation);

    if (!Files.exists(workingDirectory)) {
      throw new InvalidConfigurationException(
          "Directory ('" + releaseFileCollectionLocation + "') not found.");
    }

    List<String> foundFiles = new ArrayList<>();

    try (Stream<Path> stream = Files.walk(workingDirectory)) {
      stream
          .filter(file -> !Files.isDirectory(file))
          .filter(file -> file.getFileName().toString().matches(FILE_RF_ZIP_PATTERN))
          .map(Path::toString)
          .forEach(
              f -> {
                log.info("Found file: {}", f);
                foundFiles.add(f);
              });
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Error while loading LOINC release file: " + e.getMessage());
    }

    return foundFiles.stream().sorted().toList();
  }

  /**
   * Searches recursively for the first file whose name matches the given regular expression pattern
   * within the specified directory.
   *
   * @param unzipLocation the root directory to search in
   * @param matchPattern a regular expression matched against file names (not full paths)
   * @return the {@link Path} of the first matching file, or {@code null} if none is found
   * @throws InvalidConfigurationException if the directory does not exist or cannot be traversed
   */
  public static Path getSingleReleaseFilePath(Path unzipLocation, String matchPattern)
      throws InvalidConfigurationException {

    if (!Files.exists(unzipLocation)) {
      throw new InvalidConfigurationException("Directory ('" + unzipLocation + "') not found.");
    }

    try (Stream<Path> stream = Files.walk(unzipLocation)) {
      Path result =
          stream
              .filter(file -> !Files.isDirectory(file))
              .filter(file -> file.getFileName().toString().matches(matchPattern))
              .findFirst()
              .orElse(null);
      log.debug("Found file: {}", result);
      return result;
    } catch (IOException e) {
      log.error("Error while loading release file", e);
      throw new InvalidConfigurationException(
          "Error while loading release file: " + e.getMessage());
    }
  }

  /**
   * Unpacks a LOINC release ZIP archive into the given destination directory.
   *
   * <p>Any previously extracted content at {@code unzipLocation} is deleted before extraction
   * begins. The destination directory is created if it does not yet exist.
   *
   * @param loincReleaseFileCollectionLocation the path to the LOINC release ZIP archive
   * @param unzipLocation the directory into which the archive is extracted
   * @throws InvalidConfigurationException if the ZIP file does not exist, the destination directory
   *     cannot be created, or extraction fails
   */
  public static void unzip(String loincReleaseFileCollectionLocation, Path unzipLocation)
      throws InvalidConfigurationException {

    log.info("");
    log.info(
        "#### Pre-Processing Loinc Release File #######################################################");

    long startTime = System.currentTimeMillis();
    deleteUnzippedFiles(unzipLocation);

    log.info("Trying to unpack: '{}'", loincReleaseFileCollectionLocation);
    Path zipFilePath = Path.of(loincReleaseFileCollectionLocation);
    if (!Files.exists(zipFilePath)) {
      throw new InvalidConfigurationException(
          "LOINC release file not found. Please adjust configuration");
    }

    log.info("Release file found. Unpacking may take a while ...");

    try {
      Files.createDirectories(unzipLocation);
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to create destination directory: " + e.getMessage());
    }
    log.info("Creating unzipLocationPath: {}", unzipLocation);

    extractZipEntries(zipFilePath, unzipLocation);
    log.info("Unpacking took {} seconds", (System.currentTimeMillis() - startTime) / 1000);
  }

  /**
   * Extracts all entries of the given ZIP archive into the destination directory.
   *
   * @param zipFilePath the path to the ZIP archive to extract
   * @param destDirectoryPath the directory into which entries are written
   * @throws InvalidConfigurationException if an I/O error occurs during extraction
   */
  // TODO check if we can use the FilHelper.unzip method here instead of implementing unzip again
  // (woud require moving it to the common package)
  @SuppressWarnings(
      "java:S5042") // We control the input files that are processed, therefore this is acceptable
  private static void extractZipEntries(Path zipFilePath, Path destDirectoryPath)
    throws InvalidConfigurationException {
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath))) {
      ZipEntry entry;
      Path normalizedDestinationPath = destDirectoryPath.toAbsolutePath().normalize();

      while ((entry = zis.getNextEntry()) != null) {
        Path entryPath = normalizedDestinationPath.resolve(entry.getName()).normalize();

        if (!entryPath.startsWith(normalizedDestinationPath)) {
          throw new IOException("ZIP entry is outside the destination directory: " + entry.getName());
        }

        if (entry.isDirectory()) {
          Files.createDirectories(entryPath);
          log.debug("Created directory: {}", entryPath);
        } else {
          Files.createDirectories(entryPath.getParent());
          Files.copy(zis, entryPath);
          log.debug("Created file: {}", entryPath);
        }

        zis.closeEntry();
      }
    } catch (IOException e) {
      log.error("Error while extracting ZIP file: {}", e.getMessage());
      throw new InvalidConfigurationException("Error while extracting ZIP file: " + e.getMessage());
    }
  }

  /**
   * Recursively deletes all files and directories at the given path.
   *
   * <p>If the path does not exist, the method returns without taking any action. Individual
   * deletion failures are logged but do not abort the overall operation.
   *
   * @param unzipLocation the root path to delete
   */
  public static void deleteUnzippedFiles(Path unzipLocation) {
    if (!Files.exists(unzipLocation)) {
      return;
    }

    log.info("");
    log.info(
        "#### Post-Processing Loinc Release File #######################################################");
    log.info("Deleting unzipLocationPath: {}", unzipLocation);

    try (Stream<Path> stream = Files.walk(unzipLocation)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  log.debug("Deleting: {}", p.getFileName());
                  Files.delete(p);
                } catch (IOException e) {
                  log.error("Error while deleting file: {}", p, e);
                }
              });
    } catch (IOException e) {
      log.error("Error while deleting unzipped files", e);
    }
  }
}
