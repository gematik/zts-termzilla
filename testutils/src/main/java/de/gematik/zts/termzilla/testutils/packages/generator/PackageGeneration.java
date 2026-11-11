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

import static de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationConstants.PACKAGE_INDEX_FILENAME;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.zts.termzilla.testutils.packages.generator.helper.ArchiveAndCompressionHelper;
import de.gematik.zts.termzilla.testutils.packages.generator.helper.FileHelper;
import de.gematik.zts.termzilla.testutils.packages.generator.model.NpmPackage;
import de.gematik.zts.termzilla.testutils.packages.generator.model.PackageIndex;
import de.gematik.zts.termzilla.testutils.packages.generator.model.PackageIndexFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.OperationOutcome;

/**
 * Generates distributable FHIR npm packages from lists of {@link MetadataResource} instances.
 *
 * <p>Core responsibilities:
 *
 * <ul>
 *   <li>Validating provided package name and version against npm-compatible patterns.
 *   <li>Creating a temporary working directory and writing {@code package.json} (optionally
 *       applying a template).
 *   <li>Serializing FHIR resources into deterministic file names and producing the index ({@code
 *       .index.json}).
 *   <li>Building a {@code .tar} archive and compressing it to {@code .tgz}.
 *   <li>Logging and recording informational/warning messages into an {@link OperationOutcome}.
 * </ul>
 *
 * <p>The class hides transient filesystem operations and enforces naming conventions for package
 * artifacts. Instances are constructed via Lombok {@link Builder} to ensure validation.
 */
@Slf4j
public class PackageGeneration {

  private static final String PACKAGE_AUTHOR = "BfArM";

  static final String PACKAGE_NAME_PATTERN = "^(?=.{1,214}$)(?![._])[a-z0-9][a-z0-9._-]*$";
  static final String PACKAGE_VERSION_PATTERN =
      "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$";
  private final IParser jsonParser;

  private final String canonical;
  private final String packageVersion;
  private final String packageName;

  private final String packageTitle;
  private final String packageDescription;
  private final String packageAuthor;
  private final Date packageDate;

  /**
   * Constructs a new {@code PackageGeneration} instance, validating package name and version
   * against defined patterns. Initializes a pretty-printing JSON parser bound to a cached R4 {@link
   * FhirContext}. If an {@link OperationOutcome} is provided it is reused, otherwise a new one is
   * created.
   *
   * @throws PackageGenerationException if name or version violate their required patterns
   */
  @Builder
  private PackageGeneration(
      String packageName,
      String packageVersion,
      String canonical,
      String packageTitle,
      String packageDescription,
      String packageAuthor,
      Date packageDate)
      throws PackageGenerationException {

    if (!packageVersion.matches(PACKAGE_VERSION_PATTERN)) {
      throw new PackageGenerationException(
          String.format(
              "Package version does not match the required pattern for npm package versions %s",
              PACKAGE_VERSION_PATTERN));
    }
    if (!packageName.matches(PACKAGE_NAME_PATTERN)) {
      throw new PackageGenerationException(
          String.format(
              "Package name does not match the required pattern for npm package names %s",
              PACKAGE_NAME_PATTERN));
    }

    this.packageName = packageName;
    this.packageVersion = packageVersion;
    this.canonical = canonical;
    this.packageTitle = packageTitle;
    this.packageDescription = packageDescription;
    this.packageAuthor = packageAuthor;
    this.packageDate = packageDate;

    FhirContext ctx = FhirContext.forR4Cached();
    this.jsonParser = ctx.newJsonParser().setPrettyPrint(true);
  }

  /**
   * Orchestrates full FHIR package creation: writes manifest, applies template (if configured),
   * serializes resources and index, archives and copies the resulting {@code .tgz} to the output
   * directory. Captures progress and warnings in {@link OperationOutcome}.
   *
   * @param resourceList list of FHIR metadata resources to include
   * @param outputDirectory destination directory for the package
   * @throws PackageGenerationException on any failure during generation
   */
  public void generatePackage(List<MetadataResource> resourceList, Path outputDirectory)
      throws PackageGenerationException {

    Path fhirPackageFile = null;

    try {
      // FHIR Package erzeugen
      fhirPackageFile = createFhirPackage(resourceList);

      // FHIR Package in das Ausgabeverzeichnis kopieren
      copyFhirPackageToOutputDirectory(fhirPackageFile, outputDirectory);

    } catch (Exception e) {
      log.error(e.getMessage());
      throw new PackageGenerationException(e.getMessage());
    } finally {

      // Löschen der temporären Dateien
      try {
        if (fhirPackageFile != null) {
          log.debug("Deleting directory: " + fhirPackageFile.getParent().toAbsolutePath());
          FileHelper.deleteFiles(fhirPackageFile.getParent());
        }
      } catch (Exception e) {
        log.warn("Error while deleting temp files: " + e.getMessage());
      }
    }
  }

  /**
   * Creates a complete FHIR npm package in a temporary directory: prepares manifest, applies
   * optional template, writes resource files and index, and produces a compressed {@code .tgz}
   * archive.
   *
   * @param parsedResources resources to package
   * @return path to the generated {@code .tgz} file
   * @throws PackageGenerationException if any step fails (IO, template, archive creation)
   */
  @SuppressWarnings("java:S5443")
  private Path createFhirPackage(List<MetadataResource> parsedResources)
      throws PackageGenerationException {

    // Metadaten für das Package Manifests (package.json)
    NpmPackage manifest = new NpmPackage();
    manifest.setCanonical(canonical);
    manifest.setVersion(packageVersion);
    manifest.setName(packageName);
    manifest.setTitle(packageTitle);
    manifest.setDescription(packageDescription);
    manifest.setDate(
        packageDate != null ? generatePackageDate(packageDate) : generatePackageDate(new Date()));
    manifest.setAuthor(StringUtils.isNoneEmpty(packageAuthor) ? packageAuthor : PACKAGE_AUTHOR);
    manifest.setFhirVersions(List.of("4.0.1"));
    manifest.setDependencies(Map.of("hl7.fhir.r4.core", "4.0.1"));

    String packageNameAndVersion = packageName + "-" + manifest.getVersion();

    Path tempWorkingDir;
    Path packageDir;
    try {

      if (SystemUtils.IS_OS_UNIX) {
        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        tempWorkingDir = Files.createTempDirectory(packageNameAndVersion + "-", attr);
      } else {
        tempWorkingDir = Files.createTempDirectory(packageNameAndVersion + "-");
      }

      packageDir =
          tempWorkingDir.toAbsolutePath().resolve(PackageGenerationConstants.PACKAGE_DIR_NAME);
      Files.createDirectories(packageDir);
    } catch (IOException e) {
      throw new PackageGenerationException(
          "Error while creating temporary directories: " + e.getMessage(), e);
    }

    // Erstellen der Manifest-Datei (package.json)
    createPackageManifest(packageDir, manifest);

    // Erstellen der Index-Datei (.index.json) und kopieren der indizierten Ressourcen
    generatePackageContentAndIndexFromResources(packageDir, parsedResources);

    // Erstellen des FHIR Packages (.tgz)
    Path fhirPackageFile;
    try {
      var packageAsTarFile = Files.createTempFile(tempWorkingDir, "package", ".tar");
      fhirPackageFile = tempWorkingDir.resolve(packageNameAndVersion + ".tgz");

      try {
        log.debug("Compressing tar archive.");
        packageAsTarFile =
            ArchiveAndCompressionHelper.createTarArchive(
                packageAsTarFile.getFileName().toString(),
                packageAsTarFile.getParent(),
                packageDir);

        FileHelper.requireDirectory(packageAsTarFile.getParent());
        log.debug("Creating package file {}", fhirPackageFile);
        ArchiveAndCompressionHelper.compress(packageAsTarFile, fhirPackageFile);
      } finally {
        // delete temporary file
        Files.deleteIfExists(packageAsTarFile);
      }
    } catch (Exception e) {
      throw new PackageGenerationException(
          "Error while creating package file (.tgz): " + e.getMessage(), e);
    }

    return fhirPackageFile;
  }

  /**
   * Creates and writes the {@code package.json} manifest file in the given output directory.
   * Delegates to {@link #writeManifestFile(Path, NpmPackage)} and wraps exceptions into a {@link
   * PackageGenerationException}.
   *
   * @param outputDirectory directory to write to
   * @param packageManifest manifest data object
   * @return path to the created manifest file
   * @throws PackageGenerationException if writing fails
   */
  public static Path createPackageManifest(Path outputDirectory, NpmPackage packageManifest)
      throws PackageGenerationException {

    try {
      return writeManifestFile(outputDirectory, packageManifest);
    } catch (Exception e) {
      throw new PackageGenerationException(
          "Error while creating package manifest file: " + e.getMessage(), e);
    }
  }

  /**
   * Writes the {@link NpmPackage} manifest to disk. Performs no validation beyond logging the
   * target directory.
   *
   * @param outputDirectory directory to write to
   * @param packageManifest manifest object
   * @return path to the written file
   * @throws IOException if writing fails
   */
  public static Path writeManifestFile(Path outputDirectory, NpmPackage packageManifest)
      throws IOException {

    // create temporary package manifest file from object
    log.debug("Writing package manifest to directory: {}", outputDirectory.toAbsolutePath());
    return NpmPackage.write(outputDirectory, packageManifest);
  }

  /**
   * Serializes all provided resources into JSON files and creates the package index file ({@code
   * .index.json}) listing each resource entry.
   *
   * @param outputDirectory directory to write resource files and index into
   * @param parsedResources list of resources to serialize
   * @return path to the written index file
   * @throws PackageGenerationException if any serialization or index operation fails
   */
  private Path generatePackageContentAndIndexFromResources(
      Path outputDirectory, List<MetadataResource> parsedResources)
      throws PackageGenerationException {

    // The package index lists all resources contained within the FHIR package
    PackageIndex packageIndex = new PackageIndex();

    // Iterate over all metadata resources (e.g. CodeSystem, ValueSet, ConceptMap, ...)
    for (MetadataResource currentResource : parsedResources) {

      try {
        // Wir benötigen für jede Ressource einen 'entry' im package index
        PackageIndexFile packageFile = new PackageIndexFile();

        // Durchsetzen der Namenskonvention für die Dateinamen: Resource-Typ + "-" + Resource-id +
        // ".json"
        String fileName = currentResource.fhirType() + "-" + currentResource.getIdPart() + ".json";
        packageFile.setFilename(fileName);
        packageFile.setResourceType(currentResource.fhirType());
        packageFile.setId(currentResource.getIdElement().getIdPart());
        packageFile.setUrl(currentResource.getUrl());
        packageFile.setVersion(currentResource.getVersion());

        // Sofern es sich um eine CodeSystem Ressource handelt, müssen wir auch content und
        // supplements mit in den package index aufnehmen
        if (currentResource instanceof CodeSystem cs) {
          if (cs.hasContent()) {
            packageFile.setContent(cs.getContent().toCode());
          }
          if (cs.hasSupplements()) {
            packageFile.setSupplements(cs.getSupplements());
          }
        }

        // lets add this file entry to the package index
        packageIndex.addFileToIndex(packageFile);

        // serialize FHIR resource to package directory
        // remember that the spec asks for JSON files
        Path destinationFileName = outputDirectory.resolve(fileName);
        writeResource(currentResource, destinationFileName);

      } catch (Exception e) {
        throw new PackageGenerationException(
            "Error while creating package index object and/or serializing FHIR resource files: "
                + e.getMessage(),
            e);
      }
    }

    // serialize package index to file
    try {
      ObjectMapper mapper = new ObjectMapper();
      Path packageIndexFile = outputDirectory.resolve(PACKAGE_INDEX_FILENAME);
      log.debug("Writing package index file to: " + packageIndexFile.toAbsolutePath());
      mapper.writerWithDefaultPrettyPrinter().writeValue(packageIndexFile.toFile(), packageIndex);

      return packageIndexFile;

    } catch (Exception e) {
      throw new PackageGenerationException(
          "Error while serializing package index file: " + e.getMessage(), e);
    }
  }

  /**
   * Serializes a single FHIR {@link IBaseResource} instance to the given path in JSON form using
   * the configured pretty-print parser.
   *
   * @param resource resource to write
   * @param path destination file path
   * @throws IOException if writing fails
   */
  private void writeResource(IBaseResource resource, Path path) throws IOException {
    String notificationString = jsonParser.encodeResourceToString(resource);
    log.debug("Writing FHIR resource data to {}", path);
    Files.writeString(path, notificationString, StandardCharsets.UTF_8);
  }

  /**
   * Copies the generated package archive into the specified output directory, creating the
   * directory structure if absent. Reports progress via {@link OperationOutcome}.
   *
   * @param fhirPackage source archive file
   * @param outputDirectory destination directory
   * @throws PackageGenerationException if creation or copy fails
   */
  private void copyFhirPackageToOutputDirectory(Path fhirPackage, Path outputDirectory)
      throws PackageGenerationException {

    // create destination directory, if it not exists already
    if (!Files.isDirectory(outputDirectory, LinkOption.NOFOLLOW_LINKS)) {
      try {
        Files.createDirectories(outputDirectory);
      } catch (IOException e) {
        throw new PackageGenerationException("Error while creating package output directory.", e);
      }
    }

    // copy FHIR package to output location
    try {
      Path sourceFile = fhirPackage.toAbsolutePath();
      Path destinationFile = outputDirectory.toAbsolutePath().resolve(fhirPackage.getFileName());
      Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new PackageGenerationException(
          "Error while copying FHIR package file to output directory.", e);
    }
  }

  /**
   * Formats a date into the deterministic package timestamp pattern {@code yyyyMMddHHmmss}.
   *
   * @param date date to format
   * @return formatted timestamp string
   */
  private static String generatePackageDate(Date date) {
    String format = "yyyyMMddHHmmss";
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    return sdf.format(date);
  }
}
