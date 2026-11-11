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

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Utility class providing file system operations for SNOMED CT release files.
 *
 * <p>Covers unpacking of RF2 release ZIP archives, locating specific release files by name pattern,
 * computing file hashes for integrity checks, and cleaning up previously extracted content. The
 * file name pattern constants defined here match the standard SNOMED CT RF2 naming conventions.
 */
@Slf4j
public class SnomedReleaseFileHelper {

  public static final String FILE_RELEASE_PACKAGE_INFORMATION_PATTERN =
      "^release_package_information.*";
  public static final String FILE_CONCEPTS_PATTERN = "^sct2_Concept_Full.*";
  public static final String FILE_DESCRIPTIONS_PATTERN = "^sct2_Description_Full.*";
  public static final String FILE_RELATIONSHIPS_PATTERN = "^sct2_Relationship_Full.*";
  public static final String FILE_MRCM_ATTRIBUTE_DOMAINS_PATTERN =
      "^der2_cissccRefset_MRCMAttributeDomainFull.*";
  public static final String FILE_MRCM_ATTRIBUTE_RANGE_PATTERN =
      "^der2_ssccRefset_MRCMAttributeRangeFull.*";
  public static final String FILE_MRCM_DOMAIN_PATTERN = "^der2_sssssssRefset_MRCMDomainFull.*";
  public static final String FILE_MODULE_DEPENDENCY_PATTERN =
      "^der2_ssRefset_ModuleDependencyFull.*";
  public static final String FILE_LANGUAGE_PATTERN = "^der2_cRefset_LanguageFull.*";

  public static final String FILE_REFSET_ASSOCIATION_PATTERN = "^der2_cRefset_AssociationFull.*";
  public static final String FILE_REFSET_ATTRIBUTEVALUE_PATTERN =
      "^der2_cRefset_AttributeValueFull.*";
  public static final String FILE_REFSET_SIMPLE_PATTERN = "^der2_Refset_SimpleFull.*";

  private SnomedReleaseFileHelper() {
    /* This utility class should not be instantiated */
  }

  /**
   * Computes the SHA-256 hash of the file at the given path.
   *
   * @param filePath the path to the file to hash
   * @return the hex-encoded SHA-256 hash of the file contents
   * @throws SnomedDataLoadingException if the file cannot be read
   */
  public static String calculateReleaseFileHash(Path filePath) {
    // SHA256 Hash der Datei berechnen
    try (InputStream is = Files.newInputStream(filePath)) {
      return DigestUtils.sha256Hex(is);
    } catch (IOException e) {
      throw new SnomedDataLoadingException(
          "Could not generate Release File Hash for path: " + filePath + "Error: ", e);
    }
  }

  /**
   * Searches recursively for the first file whose name matches the given regular expression pattern
   * within the specified directory.
   *
   * @param unzipLocation the root directory to search in
   * @param matchPattern a regular expression matched against file names (not full paths)
   * @return the {@link Path} of the first matching file, or {@code null} if none is found
   * @throws SnomedDataLoadingException if the directory does not exist or cannot be traversed
   */
  public static Path getSingleReleaseFilePath(Path unzipLocation, String matchPattern) {

    if (!Files.exists(unzipLocation)) {
      throw new SnomedDataLoadingException("Directory ('" + unzipLocation + "') not found.", null);
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
      throw new SnomedDataLoadingException(
          "Error while loading release file: " + e.getMessage(), null);
    }
  }

  /**
   * Unpacks a SNOMED CT RF2 release ZIP archive into the given destination directory.
   *
   * <p>Any previously extracted content at {@code unzipLocation} is deleted before extraction
   * begins. The destination directory is created if it does not yet exist.
   *
   * @param snomedReleaseFileLocation the path to the SNOMED CT RF2 ZIP archive
   * @param unzipLocation the directory into which the archive is extracted
   * @throws InvalidConfigurationException if the ZIP file does not exist, the destination directory
   *     cannot be created, or extraction fails
   */
  // TODO check if we can use the FilHelper.unzip method here instead of implementing unzip again
  // (woud require moving it to the common package)
  public static void unzip(Path snomedReleaseFileLocation, Path unzipLocation)
      throws InvalidConfigurationException {

    log.info("");
    log.info(
        "#### Pre-Processing Snomed Release File #######################################################");

    long startTime = System.currentTimeMillis();
    deleteUnzippedFiles(unzipLocation);

    // check if zip file exists in given location
    log.info("Trying to unpack: '{}'", snomedReleaseFileLocation);
    if (!Files.exists(snomedReleaseFileLocation)) {
      throw new InvalidConfigurationException(
          "Snomed Release File not found. Please adjust configuration");
    }

    log.info("Release file found. Unpacking may take a while ...");

    // create tmp dir
    try {
      Files.createDirectories(unzipLocation);
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to create destination directory: " + e.getMessage());
    }
    log.info("Creating unzipLocationPath: {}", unzipLocation);

    // Benötigte Dateien entpacken
    extractZipEntries(snomedReleaseFileLocation, unzipLocation);
    log.info("Unpacking took {} seconds", (System.currentTimeMillis() - startTime) / 1000);
  }

  /**
   * Extracts all entries of the given ZIP archive into the destination directory.
   *
   * @param zipFilePath the path to the ZIP archive to extract
   * @param destDirectoryPath the directory into which entries are written
   * @throws InvalidConfigurationException if an I/O error occurs during extraction
   */
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
        "#### Post-Processing Snomed Release File #######################################################");
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
                  log.error("Error deleting file: {}", e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error while deleting unzipped files", e);
    }
  }

  /**
   * Splits a tab-separated line into an array of field values using index-based parsing instead of
   * regex-based {@link String#split}.
   *
   * @param line the tab-separated line to split
   * @param expected the expected number of fields (used to pre-size the array and to validate the
   *     input)
   * @return array of field values with exactly {@code expected} elements
   * @throws SnomedDataLoadingException if the line contains fewer than {@code expected} fields
   */
  public static String[] splitLine(String line, int expected) {
    String[] result = new String[expected];
    int idx = 0;
    int start = 0;
    int tab;
    while ((tab = line.indexOf('\t', start)) >= 0 && idx < expected - 1) {
      result[idx++] = line.substring(start, tab);
      start = tab + 1;
    }
    result[idx] = line.substring(start);
    if (idx < expected - 1) {
      throw new SnomedDataLoadingException(
          String.format(
              "Expected %d tab-separated fields but found %d in line: %.100s",
              expected, idx + 1, line),
          null);
    }
    return result;
  }
}
