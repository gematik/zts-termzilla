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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.provision.PackageProvisionSettings;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.provision.ProvisionSettings;
import de.gematik.zts.termzilla.terminologies.fhirterms.exceptions.ResourceLoadingException;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.helper.FhirPackageHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.helper.FileHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packageindex.PackageIndex;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packageindex.PackageIndexFile;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packagemanifest.PackageManifest;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Loads FHIR terminology resources from packaged tar balls into an in-memory representation.
 *
 * <p>Processes FHIR packages (tar balls) containing {@link CodeSystem}, {@link ValueSet}, and
 * {@link ConceptMap} resources. Extracts package contents, validates resources, and populates the
 * in-memory representation for terminology operations.
 */
@Slf4j
public class FhirTermsResourceLoader {

  private static final String PACKAGE_INDEX_FILE = ".index.json";
  private static final String PACKAGE_MANIFEST_FILE = "package.json";
  private static final String PACKAGE_FOLDER = "package";
  private static final String FILE_EXTENSION_JSON = ".json";
  private static final String FILE_EXTENSION_XML = ".xml";
  private static final String FILE_EXTENSION_TGZ = ".tgz";
  private static final String FILE_EXTENSION_TAR_GZ = ".tar.gz";

  private static final List<String> RESOURCE_FILES_TO_PROCESS =
      List.of(
          CodeSystem.class.getSimpleName(),
          ValueSet.class.getSimpleName(),
          ConceptMap.class.getSimpleName());

  private final FhirContext fhirContext = FhirContext.forR4();
  private final IParser jsonParser = fhirContext.newJsonParser();
  private final IParser xmlParser = fhirContext.newXmlParser();
  private final List<Path> processedTarBalls = new ArrayList<>();
  private final List<PackageManifest> processedPackages = new ArrayList<>();
  private final MultiValuedMap<Path, String> dependencyRetrievalFails =
      new ArrayListValuedHashMap<>();
  private final List<String> resourceLoadErrors = new ArrayList<>();
  private final List<Path> tarBallFails = new ArrayList<>();

  private final FhirTermsInMemoryRepresentation inMemoryRepresentation;
  private final FhirConfigProperties properties;

  /**
   * Constructs a resource loader with configuration and target in-memory representation.
   *
   * @param properties configuration properties for FHIR terms processing
   * @param fhirTermsInMemoryRepresentation target storage for loaded resources
   * @throws InvalidConfigurationException if required configuration is missing
   */
  public FhirTermsResourceLoader(
      FhirConfigProperties properties,
      FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation)
      throws InvalidConfigurationException {

    if (properties == null) {
      throw new InvalidConfigurationException(
          "No config properties defined. Please check the configuration");
    }
    this.properties = properties;

    this.inMemoryRepresentation = fhirTermsInMemoryRepresentation;
  }

  /**
   * Loads FHIR terminology resources from configured package location into memory.
   *
   * <p>Extracts tar ball packages, parses contained resources, validates them, and populates the
   * in-memory representation. Logs statistics about loaded resources and any failures.
   */
  public void load() {

    // Relevante Pfade aus der Konfiguration lesen
    Path packageFolder = Path.of(properties.getFhirTermsPackageFolderLocation());
    Path packageUnzipLocation = properties.getFhirTermsUnzipLocation();

    try {

      // Laden von Paketinhalten
      unzipAndLoadPackageContent(packageFolder, packageUnzipLocation);

      // Hinweis: Früher haben wir an dieser Stelle auch das Laden von Einzeldateien zugelassen.
      // Hiervon würde ich in diesem Code erstmal Abstand nehmen wollen, da es die Komplexität
      // maßgeblich
      // erhöht und wir vom Modell Single-Source-Of-Truth abweichen: Nur Pakete sind für mich
      // abschließend qualitätsgesicherte Artefakte. Einzeldateien können das fragile
      // Abhängikeitsverhältnis extrem stören.

      // Anzeige der geladenen Ressourcen

      processedPackages.stream()
          .collect(
              Collectors.groupingBy(
                  PackageManifest::getName,
                  Collectors.mapping(PackageManifest::getVersion, Collectors.toList())))
          .forEach(
              (name, versions) -> {
                Collections.sort(versions);
                log.info("loaded {} with versions {}", name, String.join(", ", versions));
              });

      int loadedCodeSystems =
          inMemoryRepresentation.getIdToResourceMap().values().stream()
              .filter(CodeSystem.class::isInstance)
              .toList()
              .size();
      log.info("loaded #{} CodeSystems", loadedCodeSystems);
      int loadedValueSets =
          inMemoryRepresentation.getIdToResourceMap().values().stream()
              .filter(ValueSet.class::isInstance)
              .toList()
              .size();
      log.info("loaded #{} ValueSets", loadedValueSets);
      int loadedConceptMaps =
          inMemoryRepresentation.getIdToResourceMap().values().stream()
              .filter(ConceptMap.class::isInstance)
              .toList()
              .size();
      log.info("loaded #{} ConceptMaps", loadedConceptMaps);

      log.debug(
          "############################## PROCESSED TAR BALLS ##############################");
      processedTarBalls.stream().map(Path::getFileName).forEach(path -> log.debug(path.toString()));
      log.debug(
          "#################################################################################");
      if (!dependencyRetrievalFails.isEmpty()) {
        log.error(
            "############################## FAILED DEPENDENCIES ##############################");
        dependencyRetrievalFails
            .asMap()
            .forEach(
                (key, value) -> {
                  String message = "package: ".concat(key.getFileName().toString()).concat(" --> ");
                  log.error(message);
                  value.forEach(log::error);
                });
        log.error(
            "#################################################################################");
      }

      if (!tarBallFails.isEmpty()) {
        log.error("############################## PROCESSING FAILS ##############################");
        tarBallFails.stream().map(Path::getFileName).forEach(path -> log.error(path.toString()));
        log.error("##############################################################################");
      }

      if (!resourceLoadErrors.isEmpty()) {
        log.error(
            "############################## CODESYSTEM ERRORS ##############################");
        resourceLoadErrors.forEach(log::error);
        log.error(
            "###############################################################################");
      }

    } catch (IOException e) {
      log.error("failed to process package folder {}", e.getMessage(), e);
    }
  }

  /**
   * Extracts and loads FHIR package contents recursively.
   *
   * <p>Processes all tar ball files in the package folder, extracts them to the unzip location, and
   * loads their resources. Recursively calls itself to handle package dependencies.
   *
   * @param packageFolder package directory containing tar balls
   * @param packageUnzipLocation target directory for extracted package contents
   * @throws IOException if file operations fail
   */
  private void unzipAndLoadPackageContent(Path packageFolder, Path packageUnzipLocation)
      throws IOException {

    // iterate over all tgz files in the package folder
    try (Stream<Path> paths = Files.list(packageFolder)) {

      // Liste der Paketdateien erstellen
      List<Path> tarFiles = collectTarFiles(paths);

      // TODO: Sortierung der Tarballs nach Package und Versionsnummer
      //  -> das ist wichtig, damit wir immer die neuesten Versionen zuerst laden. Insbesondere bei
      //     Systemen, bei denen sich die Versionsnummer bei Korrekturen nicht ändert, ist das
      //     unerlässlich.

      // Wenn alle tarballs bereits verarbeitet wurden, wird die Methode beendet (beendet die
      // rekursive Aufrufstruktur)
      if (tarFiles.size() == processedTarBalls.size()) {
        return;
      }

      // ObjectMapper erstellen und so konfigurieren, dass er bei der Verarbeitung unbekannter
      // Eigenschaften/Elemente abbricht
      ObjectMapper mapper =
          new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      // Über alle Tarballs iterieren
      for (Path tarFile : tarFiles) {
        processTarFile(tarFile, packageUnzipLocation, mapper);
      }

      // Rekursiver Methodenaufruf, um auch die heruntergeladenen Abhängigkeiten verarbeiten zu
      // können.
      unzipAndLoadPackageContent(packageFolder, packageUnzipLocation);
    }
  }

  /**
   * Computes a combined SHA-256 hash over the contents of all FHIR package tar balls found in the
   * given directory.
   *
   * <p>Package files are located via {@link #collectTarFiles(Stream)}. The paths are sorted before
   * hashing to guarantee a deterministic result regardless of file system traversal order. Each
   * file's content is hashed individually, and a final SHA-256 hash is computed over the
   * concatenation of all per-file hashes.
   *
   * @param fhirTermsPackageFolderLocation the directory to search for FHIR package tar balls
   * @return a hex-encoded SHA-256 hash representing the combined state of all found package files
   * @throws InvalidConfigurationException if the directory does not exist, cannot be listed, or any
   *     package file cannot be read
   */
  public String calculateReleaseFileHash(String fhirTermsPackageFolderLocation)
      throws InvalidConfigurationException {

    Path packageFolder = Path.of(fhirTermsPackageFolderLocation);
    if (!Files.exists(packageFolder)) {
      throw new InvalidConfigurationException(
          "Directory ('" + fhirTermsPackageFolderLocation + "') not found.");
    }

    List<Path> tarFiles;
    try (Stream<Path> paths = Files.list(packageFolder)) {
      tarFiles = collectTarFiles(paths).stream().sorted().toList();
    } catch (IOException e) {
      throw new InvalidConfigurationException("Failed to list package folder: " + e.getMessage());
    }

    StringBuilder combinedHashes = new StringBuilder();
    for (Path tarFile : tarFiles) {
      try (InputStream is = Files.newInputStream(tarFile)) {
        combinedHashes.append(DigestUtils.sha256Hex(is));
      } catch (IOException e) {
        throw new InvalidConfigurationException(
            "Could not generate release file hash for path: " + tarFile + ": " + e.getMessage());
      }
    }
    return DigestUtils.sha256Hex(combinedHashes.toString());
  }

  /**
   * Collects all tar ball files from the provided path stream.
   *
   * @param paths stream of paths to filter
   * @return list of tar ball file paths
   */
  private List<Path> collectTarFiles(Stream<Path> paths) {
    return paths
        .filter(Files::isRegularFile)
        .filter(
            path ->
                path.toString().endsWith(FILE_EXTENSION_TGZ)
                    || path.toString().endsWith(FILE_EXTENSION_TAR_GZ))
        .toList();
  }

  /**
   * Processes a single tar ball file by extracting and loading its contents.
   *
   * <p>Skips already processed files, extracts the package, loads manifest and index, and processes
   * all contained resources.
   *
   * @param tarFile tar ball file to process
   * @param packageUnzipLocation target directory for extraction
   * @param mapper JSON object mapper for manifest/index parsing
   */
  private void processTarFile(Path tarFile, Path packageUnzipLocation, ObjectMapper mapper) {
    try {
      // Bei bereits verarbeiteten Tarballs wird der Vorgang übersprungen
      if (processedTarBalls.contains(tarFile)) {
        return;
      }

      // Inhalte extrahieren
      log.debug("Unzipping FhirTerms Package: {}", tarFile);
      Path tempDir = FileHelper.createTemporaryDirectory();
      Path unzipLocation =
          packageUnzipLocation.resolve(
              tarFile.getFileName().toString().replace(FILE_EXTENSION_TGZ, ""));
      Path decompressed = FhirPackageHelper.decompress(tarFile, tempDir);
      FhirPackageHelper.unTar(decompressed, unzipLocation);
      FileHelper.deleteFiles(tempDir);

      // Hauptablageort für die entpackten Dateien
      Path packageDirectory = unzipLocation.resolve(PACKAGE_FOLDER);

      // Package Manifest Datei (package.json) laden und (falls gewünscht) Abhängigkeiten
      // auflösen
      PackageManifest packageManifest = loadPackageManifest(packageDirectory, mapper);

      // Package Index Datei laden (.index.json) und Ressourcen verarbeiten
      loadPackageResources(packageDirectory, packageManifest, mapper);

      // Hinzufügen des verarbeiteten Tarballs zur Liste
      processedTarBalls.add(tarFile);

      // Add to list of processed packages
      if (packageManifest != null) {
        processedPackages.add(packageManifest);
      }

    } catch (IOException | IllegalArgumentException e) {
      // Fehler beim Entpacken oder Laden der Dateien werden "protokolliert"
      log.error("Failed to process tar file: {}", tarFile, e);
      tarBallFails.add(tarFile);
    }
  }

  /**
   * Loads and parses the package manifest file ({@code package.json}).
   *
   * @param packageDirectory directory containing the package
   * @param mapper JSON object mapper
   * @return parsed package manifest, or {@code null} if file doesn't exist
   * @throws IOException if reading or parsing fails
   */
  private PackageManifest loadPackageManifest(Path packageDirectory, ObjectMapper mapper)
      throws IOException {
    Path packageManifestFile = packageDirectory.resolve(PACKAGE_MANIFEST_FILE);

    if (Files.exists(packageManifestFile)) {
      return mapper.readValue(packageManifestFile.toFile(), PackageManifest.class);
    }

    return null;
  }

  /**
   * Loads FHIR resources from a package directory.
   *
   * <p>Uses package index file if available to selectively load resources, otherwise loads all JSON
   * and XML files from the directory.
   *
   * @param packageDirectory directory containing package resources
   * @param packageManifest package manifest for metadata
   * @param mapper JSON object mapper for index parsing
   * @throws IOException if file operations fail
   */
  private void loadPackageResources(
      Path packageDirectory, PackageManifest packageManifest, ObjectMapper mapper)
      throws IOException {
    Path packageIndexFile = packageDirectory.resolve(PACKAGE_INDEX_FILE);

    // Sofern es eine Package Index Datei gibt, werden lediglich die Ressourcen geladen, die
    // in der Datei aufgeführt sind. Ansonsten werden alle Ressourcen im Ordner geladen.
    if (Files.exists(packageIndexFile)) {
      PackageIndex packageIndex = mapper.readValue(packageIndexFile.toFile(), PackageIndex.class);

      List<PackageIndexFile> resourceFiles =
          packageIndex.getFiles().stream()
              .filter(file -> RESOURCE_FILES_TO_PROCESS.contains(file.getResourceType()))
              .toList();

      // Alle aufgeführten Ressourcen laden und verarbeiten
      for (PackageIndexFile file : resourceFiles) {
        Path resourceFile = packageDirectory.resolve(file.getFilename());
        if (Files.exists(resourceFile)) {
          parseResourceFile(resourceFile, packageManifest);
        }
      }

    } else {
      // Es existiert keine Paket-Index-Datei, daher werden alle Ressourcen im Ordner geladen
      log.warn(
          "Package Index not found: {}. Going to load all Resources from folder {}",
          packageIndexFile,
          packageDirectory);
      loadResourcesFromFolder(packageDirectory, packageManifest);
    }
  }

  /**
   * Processes and validates a {@link CodeSystem} resource before adding to in-memory storage.
   *
   * <p>Validates quality requirements, filters out SNOMED CT and LOINC resources, and adds
   * qualified resources to the in-memory representation.
   *
   * @param codeSystem code system resource to process
   * @throws ResourceLoadingException if resource with same ID already exists
   */
  private void processCodeSystem(CodeSystem codeSystem) {

    // TODO: Umgang mit Supplement CodeSystem Ressourcen klären: Diese müssen ja eigentlich andere
    //  CodeSysteme - sofern vorhanden - ergänzen. Das wird u.U. ein Reihenfolgeproblem beim laden.
    //  Ferner dürfen Supplements nicht mit den üblichen Operationen genutzt werden. Das ist aber an
    //  anderer Stelle zu klären. Die Frage ist, ob Supplements überhaupt auftauchen sollten?! Im
    //  Package ist das sicherlich okay, aber im Server?

    // CodeSystem-Ressourcen, die nicht unseren Qualitätsstandards entsprechen, werden ignoriert
    // TODO: Vielleicht müssen wir an dieser STelle noch strikter sein und den Startvorgang des
    //  Servers abbrechen. Ggf. sollte sich das Verhalten auch konfigurieren lassen
    if (!isHealthyCodeSystem(codeSystem)) {
      return;
    }

    // TODO: Wir brauchen einen generischen Mechanismus, um urls zu claimen, die vom Loader nicht
    //  angezogen werden dürfen oder bei denen die Verarbeitung komplett abbricht.
    // Sofern es sich um SNOMED-CT oder LOINC handelt, ignorieren wir die Ressourcen
    if (codeSystem.getUrl().equals(CanonicalUrl.SNOMEDCT)
        || codeSystem.getUrl().equals(CanonicalUrl.LOINC)) {
      log.warn(
          "Skip loading CodeSystem with url {} (remember: SNOMED and LOINC are supported by different Terminology Managers)",
          codeSystem.getUrl());
      return;
    }

    // TODO: Wir sollten nochmal überlegen, ob wir aufs erste auch "fragment" verbieten wollen.
    // Sofern das CodeSystem als Content NOT-PRESENT hat, ignorieren wir die Ressource
    if (codeSystem.getContent().equals(CodeSystem.CodeSystemContentMode.NOTPRESENT)) {
      log.warn("CodeSystem {} (Content = NOT-PRESENT) --> skipping", codeSystem.getUrl());
      return;
    }

    // TODO: Weitere Überlegungen bezüglich hierarchischer Strukturen anstellen und überlegen, ob
    //  diese immer als parent und child Properties abgebildet werden sollten. Implikationen
    //  bezüglich der CS-Metadaten (property) beachten, d.h. ggf. Property-Definition ergänzen.

    // Hinzufügen der Ressource zur flachen InMemory-Repräsentation
    if (inMemoryRepresentation.getIdToResourceMap().containsKey(codeSystem.getIdPart())) {
      // TODO: Anscheinend wird diese Exception irgendwo gefangen und führt nicht zum Abbruch des
      //  Servers. Das ist nicht gut, da wir hier einen Fehler haben, der nicht ignoriert werden
      throw new ResourceLoadingException(
          "CodeSystem with id "
              + codeSystem.getIdPart()
              + " already exists in InMemoryRepresentation");
    }
    inMemoryRepresentation.getIdToResourceMap().put(codeSystem.getIdPart(), codeSystem);

    // TODO: Wir haben noch eine offene Frage bezüglich des Umgangs mit alternativen Canonicals,
    //  die im Identifier-Element als official gepflegt werden. Es hat sich nicht als praktikabel
    //  herausgestellt, diese direkt unter ihrem System zu registrieren.

  }

  /**
   * Processes and validates a {@link ValueSet} resource before adding to in-memory storage.
   *
   * @param valueSet value set resource to process
   * @throws ResourceLoadingException if resource with same ID already exists
   */
  private void processValueSet(ValueSet valueSet) {

    // TODO: ValueSet-Ressourcen, die nicht unseren Qualitätsstandards entsprechen, werden ignoriert

    // TODO: Wir brauchen einen generischen Mechanismus, um urls zu claimen, die vom Loader nicht
    //  angezogen werden dürfen oder bei denen die Verarbeitung komplett abbricht.

    // TODO: Überlegen, wie wir an dieser Stelle mit Expansions umgehen wollen

    // Hinzufügen der Ressource zur flachen InMemory-Repräsentation
    if (inMemoryRepresentation.getIdToResourceMap().containsKey(valueSet.getIdPart())) {
      // TODO: Anscheinend wird diese Exception irgendwo gefangen und führt nicht zum Abbruch des
      //  Servers. Das ist nicht gut, da wir hier einen Fehler haben, der nicht ignoriert werden
      throw new ResourceLoadingException(
          "ValueSet with id " + valueSet.getIdPart() + " already exists in InMemoryRepresentation");
    }
    inMemoryRepresentation.getIdToResourceMap().put(valueSet.getIdPart(), valueSet);
  }

  /**
   * Processes and validates a {@link ConceptMap} resource before adding to in-memory storage.
   *
   * @param conceptMap concept map resource to process
   * @throws ResourceLoadingException if resource with same ID already exists
   */
  private void processConceptMap(ConceptMap conceptMap) {

    // TODO: ConceptMap-Ressourcen, die nicht unseren Qualitätsstandards entsprechen, werden
    // ignoriert

    // TODO: Wir brauchen einen generischen Mechanismus, um urls zu claimen, die vom Loader nicht
    //  angezogen werden dürfen oder bei denen die Verarbeitung komplett abbricht.

    // Hinzufügen der Ressource zur flachen InMemory-Repräsentation
    if (inMemoryRepresentation.getIdToResourceMap().containsKey(conceptMap.getIdPart())) {
      // TODO: Anscheinend wird diese Exception irgendwo gefangen und führt nicht zum Abbruch des
      //  Servers. Das ist nicht gut, da wir hier einen Fehler haben, der nicht ignoriert werden
      throw new ResourceLoadingException(
          "ConceptMap with id "
              + conceptMap.getIdPart()
              + " already exists in InMemoryRepresentation");
    }
    inMemoryRepresentation.getIdToResourceMap().put(conceptMap.getIdPart(), conceptMap);
  }

  /**
   * Validates whether a {@link CodeSystem} meets minimum quality requirements.
   *
   * <p>Checks for pipe symbols in URL, presence of URL and version elements.
   *
   * @param codeSystem code system to validate
   * @return {@code true} if resource meets quality requirements, {@code false} otherwise
   */
  private boolean isHealthyCodeSystem(CodeSystem codeSystem) {

    // Wir erwarten, dass geladene CodeSysteme eine URL und eine Version haben
    if (codeSystem.getUrl() == null || codeSystem.getVersion() == null) {
      log.warn(
          "Skip loading CodeSystem with url '{}' and version '{}' (systen and version needed)",
          codeSystem.getUrl(),
          codeSystem.getVersion());
      return false;
    }
    // Wir haben ein paar defekte CodeSystem-Ressourcen mit Pipe-Symbolen in der canonical URL.
    // Diese verarbeiten wir erstmal nicht, da sie nur Ärger bereiten.
    if (codeSystem.getUrl().contains("|")) {
      log.warn(
          "Skip loading CodeSystem with url '{}' (We don't allow Pipe-Symbols within an URL)",
          codeSystem.getUrl());
      return false;
    }

    // Anscheinend gab es keine schwerwiegenden Problem
    return true;
  }

  /**
   * Parses a resource file and adds it to the in-memory representation.
   *
   * <p>Selects appropriate parser based on file extension, enriches resource metadata with package
   * information, generates new ID, and delegates to type-specific processor.
   *
   * @param file resource file to parse
   * @param packageManifest package manifest providing metadata
   */
  private void parseResourceFile(Path file, PackageManifest packageManifest) {

    // Manifest-Dateien werden ignoriert
    if (file.getFileName().toString().equals(PACKAGE_MANIFEST_FILE)) {
      return;
    }

    try {
      // Selektieren des passenden Parsers anhand der Dateiendung
      IParser parser = jsonParser;
      if (file.getFileName().toString().endsWith(FILE_EXTENSION_XML)) {
        parser = xmlParser;
      }

      // SHA256 Hash der Datei berechnen
      String resourceHash;
      try (InputStream is = Files.newInputStream(file)) {
        resourceHash = DigestUtils.sha256Hex(is);
      }

      // Ressource parsen
      IBaseResource resource = parser.parseResource(Files.readString(file));
      resource.setUserData(ResourceUserData.USERDATA_RESOURCEHASH, resourceHash);

      // Hinzufügen des Package-Namens und der Version als Source (für spätere Suche)
      if (packageManifest != null) {

        // Nur für die bfarm-Terminologien können wir die Annahme treffen, dass sie aus der
        // offiziellen NPM-Registry stammen. Entsprechend kennen wir damit die ursprüngliche
        // "Source".
        // Alle anderen Ressourcen bekommen keine source Information gesetzt. Zusätzlich wird bei
        // nicht vom BfArM stammenden Ressourcen, die id entfernt und neu gesetzt.
        if (packageManifest.getName().startsWith("bfarm.terminologien.")) {

          Meta metadata = (Meta) resource.getMeta();
          metadata.setSource(
              "https://terminologien.bfarm.de/packages/"
                  .concat(packageManifest.getName())
                  .concat("/")
                  .concat(packageManifest.getVersion()));
        }

        resource.setUserData(ResourceUserData.USERDATA_PACKAGENAME, packageManifest.getName());
        resource.setUserData(
            ResourceUserData.USERDATA_PACKAGEVERSION, packageManifest.getVersion());
      }

      // apply provision settings
      applyProvisionSettings(resource, packageManifest);

      // TODO: vernünftigen Umgang mit Scoping überlegen. Das ist erstmal nur eine fürchterlich
      //  schlechte Variante
      Set<String> scopeSet = new HashSet<>();
      String packageScope = packageManifest.getName() + "-" + packageManifest.getVersion();
      scopeSet.add(packageScope);
      resource.setUserData(ResourceUserData.USERDATA_SCOPE_SET, scopeSet);

      // Neugenerierung des 'id'-Elementes - Wir generieren für jede Ressource eine neue name based
      // UUID, um sicherzustellen, dass wir jede geladene Ressource eindeutig identifizieren
      // können. Somit haben auch CodeSysteme mit gleicher url und gleicher version grundsetzlich
      // verschiedene UUIDs, wenn sie aus verschiedenen Paketen stammen. Dies ist zum Beispiel im
      // Kontext von ICD-10-GM notwendig, wenn eine Corrigenda erscheint.
      LoaderHelper.applyNewId(resource, resourceHash, packageManifest);

      // Setzen eines Zeitstempels - Dieser entspricht prinzipiell dem Zeitpunkt, an dem die
      // Ressource geladen wurde und wird später für das Setzen von Caching-Headern verwendet
      resource.getMeta().setLastUpdated(new Date());

      // Verarbeitung der Ressource und Übernahme in die InMemory-Repräsentation (idToResourceMap)
      if (resource instanceof CodeSystem codeSystem) {
        processCodeSystem(codeSystem);
      } else if (resource instanceof ValueSet valueSet) {
        processValueSet(valueSet);
      } else if (resource instanceof ConceptMap conceptMap) {
        processConceptMap(conceptMap);
      }
    } catch (Exception e) {
      log.error("failed to parse resource from file {} - {} ", file, e.getMessage());
      resourceLoadErrors.add(file.getFileName().toString());
    }
  }

  private void applyProvisionSettings(IBaseResource resource, PackageManifest packageManifest) {
    // if we are not dealing with a metadata resource, we cannot apply specific provision settings
    if (!(resource instanceof MetadataResource metadataResource)) {
      return;
    }
    // first get the mode defined for the module (if none is set, we assume it is PREDEFINED)
    ProvisionMode effectiveMode =
        properties.getProvisionMode() != null
            ? properties.getProvisionMode()
            : ProvisionMode.PREDEFINED;
    // get the provision settings from the configuration
    ProvisionSettings provisionSettings =
        properties.hasProvisionSettings() ? properties.getProvisionSettings() : null;
    // then check if there are specific settings for this package
    // Check if there are specific settings for this package
    if (packageManifest != null
        && provisionSettings != null
        && provisionSettings.hasPackageSettings(packageManifest.getName())) {

      PackageProvisionSettings packageSettings =
          provisionSettings.getSettingsForPackage(
              packageManifest.getName(), packageManifest.getVersion());

      if (packageSettings != null) {
        // Override mode if there is a specific setting for this resource (will keep default
        // otherwise)
        effectiveMode = packageSettings.getEffectiveMode(metadataResource.getUrl());
      }
    }
    // store the information in the resource userdata
    resource.setUserData(ResourceUserData.USERDATA_PROVISION_MODE, effectiveMode);
    log.debug(
        "Set provision mode {} for resource {} in package {} with version {}",
        effectiveMode,
        metadataResource.getUrl(),
        packageManifest != null ? packageManifest.getName() : "unknown",
        packageManifest != null ? packageManifest.getVersion() : "unknown");
  }

  /**
   * Loads all JSON and XML resources from a directory when no package index is available.
   *
   * @param inputFolder directory to scan for resources
   * @param packageManifest package manifest for metadata
   */
  private void loadResourcesFromFolder(Path inputFolder, PackageManifest packageManifest) {
    try (Stream<Path> stream = Files.walk(inputFolder)) {
      var allJsonAndXmlFiles =
          stream
              .filter(
                  file ->
                      !Files.isDirectory(file)
                          && (file.getFileName().toString().endsWith(FILE_EXTENSION_JSON)
                              || file.getFileName().toString().endsWith(FILE_EXTENSION_XML)))
              .collect(Collectors.toSet());

      for (var resourceFile : allJsonAndXmlFiles) {
        parseResourceFile(resourceFile, packageManifest);
      }

    } catch (Exception _) {
      log.error("failed to walk directory {} ", inputFolder);
    }
  }
}
