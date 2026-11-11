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
package de.gematik.zts.termzilla.testutils.packages.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.MetadataResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PackageGeneration}.
 *
 * <p>Tests verify the FHIR npm package generation functionality, including:
 *
 * <ul>
 *   <li>Creation of valid tar.gz archives
 *   <li>Correct package structure and content
 *   <li>Validation of package name and version patterns
 * </ul>
 */
@DisplayName("PackageGeneration Tests")
class PackageGenerationTest {

  private static final String TEST_PACKAGE_NAME = "test.fhir.package";
  private static final String TEST_PACKAGE_VERSION = "1.0.0";
  private static final String TEST_CANONICAL = "http://example.org/fhir";

  @TempDir Path tempOutputDirectory;

  @Nested
  @DisplayName("generatePackage Method Tests")
  class GeneratePackageTests {

    @Test
    @DisplayName("Should create a tar.gz archive in the output directory")
    void shouldCreateTarGzArchiveInOutputDirectory() throws PackageGenerationException {
      // GIVEN
      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      List<MetadataResource> resources = List.of(createTestCodeSystem("test-cs", "1.0.0"));

      // WHEN
      packageGeneration.generatePackage(resources, tempOutputDirectory);

      // THEN
      Path expectedPackageFile =
          tempOutputDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");

      assertThat(expectedPackageFile).exists().isRegularFile().hasExtension("tgz");
    }

    @Test
    @DisplayName("Should create a valid gzip-compressed tar archive")
    void shouldCreateValidGzipCompressedTarArchive() throws PackageGenerationException {
      // GIVEN
      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      List<MetadataResource> resources = List.of(createTestCodeSystem("test-cs", "1.0.0"));

      // WHEN
      packageGeneration.generatePackage(resources, tempOutputDirectory);

      // THEN
      Path packageFile =
          tempOutputDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");

      // Verify it's a valid gzip file by attempting to read it
      assertThatCode(
              () -> {
                try (FileInputStream fis = new FileInputStream(packageFile.toFile());
                    GZIPInputStream gzis = new GZIPInputStream(fis);
                    TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

                  TarArchiveEntry entry;
                  boolean hasEntries = false;
                  while ((entry = tais.getNextEntry()) != null) {
                    hasEntries = true;
                    assertThat(entry.getName()).isNotEmpty();
                  }
                  assertThat(hasEntries).as("Archive should contain at least one entry").isTrue();
                }
              })
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should include package.json in the archive")
    void shouldIncludePackageJsonInArchive() throws PackageGenerationException, IOException {
      // GIVEN
      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      List<MetadataResource> resources = List.of(createTestCodeSystem("test-cs", "1.0.0"));

      // WHEN
      packageGeneration.generatePackage(resources, tempOutputDirectory);

      // THEN
      Path packageFile =
          tempOutputDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");

      boolean containsPackageJson = archiveContainsFile(packageFile, "package/package.json");
      assertThat(containsPackageJson).as("Archive should contain package/package.json").isTrue();
    }

    @Test
    @DisplayName("Should include .index.json in the archive")
    void shouldIncludeIndexJsonInArchive() throws PackageGenerationException, IOException {
      // GIVEN
      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      List<MetadataResource> resources = List.of(createTestCodeSystem("test-cs", "1.0.0"));

      // WHEN
      packageGeneration.generatePackage(resources, tempOutputDirectory);

      // THEN
      Path packageFile =
          tempOutputDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");

      boolean containsIndexJson = archiveContainsFile(packageFile, "package/.index.json");
      assertThat(containsIndexJson).as("Archive should contain package/.index.json").isTrue();
    }

    @Test
    @DisplayName("Should include resource files in the archive")
    void shouldIncludeResourceFilesInArchive() throws PackageGenerationException, IOException {
      // GIVEN
      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      CodeSystem codeSystem = createTestCodeSystem("my-codesystem", "1.0.0");
      List<MetadataResource> resources = List.of(codeSystem);

      // WHEN
      packageGeneration.generatePackage(resources, tempOutputDirectory);

      // THEN
      Path packageFile =
          tempOutputDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");

      boolean containsResourceFile =
          archiveContainsFile(packageFile, "package/CodeSystem-my-codesystem.json");
      assertThat(containsResourceFile)
          .as("Archive should contain the CodeSystem resource file")
          .isTrue();
    }

    @Test
    @DisplayName("Should create output directory if it does not exist")
    void shouldCreateOutputDirectoryIfNotExists() throws PackageGenerationException {
      // GIVEN
      Path nonExistentDirectory = tempOutputDirectory.resolve("new-subdirectory");
      assertThat(nonExistentDirectory).doesNotExist();

      PackageGeneration packageGeneration =
          PackageGeneration.builder()
              .packageName(TEST_PACKAGE_NAME)
              .packageVersion(TEST_PACKAGE_VERSION)
              .canonical(TEST_CANONICAL)
              .build();

      List<MetadataResource> resources = List.of(createTestCodeSystem("test-cs", "1.0.0"));

      // WHEN
      packageGeneration.generatePackage(resources, nonExistentDirectory);

      // THEN
      assertThat(nonExistentDirectory).exists().isDirectory();

      Path expectedPackageFile =
          nonExistentDirectory.resolve(TEST_PACKAGE_NAME + "-" + TEST_PACKAGE_VERSION + ".tgz");
      assertThat(expectedPackageFile).exists();
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("Should throw exception for invalid package name")
    void shouldThrowExceptionForInvalidPackageName() {
      // GIVEN
      String invalidPackageName = "Invalid Package Name!"; // Contains spaces and uppercase

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  PackageGeneration.builder()
                      .packageName(invalidPackageName)
                      .packageVersion(TEST_PACKAGE_VERSION)
                      .canonical(TEST_CANONICAL)
                      .build())
          .isInstanceOf(PackageGenerationException.class)
          .hasMessageContaining("Package name does not match");
    }

    @Test
    @DisplayName("Should throw exception for invalid package version")
    void shouldThrowExceptionForInvalidPackageVersion() {
      // GIVEN
      String invalidVersion = "not-a-version";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  PackageGeneration.builder()
                      .packageName(TEST_PACKAGE_NAME)
                      .packageVersion(invalidVersion)
                      .canonical(TEST_CANONICAL)
                      .build())
          .isInstanceOf(PackageGenerationException.class)
          .hasMessageContaining("Package version does not match");
    }
  }

  // ------------------------------------------------------------------------------------------------
  // Helper Methods
  // ------------------------------------------------------------------------------------------------

  /**
   * Creates a test CodeSystem with the specified ID and version.
   *
   * @param id the CodeSystem ID
   * @param version the CodeSystem version
   * @return a configured CodeSystem for testing
   */
  private CodeSystem createTestCodeSystem(String id, String version) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(id);
    codeSystem.setUrl(TEST_CANONICAL + "/CodeSystem/" + id);
    codeSystem.setVersion(version);
    codeSystem.setName("TestCodeSystem");
    codeSystem.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
    codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    codeSystem.addConcept().setCode("TEST-CODE").setDisplay("Test Code Display");
    return codeSystem;
  }

  /**
   * Checks if the tar.gz archive contains a file with the specified name.
   *
   * @param archivePath path to the tar.gz archive
   * @param fileName the file name to search for
   * @return true if the file is found in the archive
   * @throws IOException if reading the archive fails
   */
  private boolean archiveContainsFile(Path archivePath, String fileName) throws IOException {
    try (FileInputStream fis = new FileInputStream(archivePath.toFile());
        GZIPInputStream gzis = new GZIPInputStream(fis);
        TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

      TarArchiveEntry entry;
      while ((entry = tais.getNextEntry()) != null) {
        if (entry.getName().equals(fileName)) {
          return true;
        }
      }
    }
    return false;
  }
}
