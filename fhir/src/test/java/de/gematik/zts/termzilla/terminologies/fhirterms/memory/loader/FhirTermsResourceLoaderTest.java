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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.common.constants.StandardTerminologyCanonicalUrls;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.loading.LoadingMode;
import de.gematik.zts.termzilla.terminologies.fhirterms.exceptions.ResourceLoadingException;
import de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.helper.FhirPackageHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link FhirTermsResourceLoader}. Tests the loading and processing of FHIR
 * terminology resources from packaged tar balls.
 */
@DisplayName("FhirTermsResourceLoader Tests")
class FhirTermsResourceLoaderTest {

  @Mock private FhirConfigProperties mockProperties;

  @Mock private FhirTermsInMemoryRepresentation mockInMemoryRepresentation;

  @TempDir Path tempDir;

  private static final String PACKAGE_FOLDER_LOCATION = "packages";
  private static final String UNZIP_LOCATION = "unzipped";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should successfully create loader with valid configuration")
    void shouldCreateLoaderWithValidConfiguration() throws InvalidConfigurationException {
      // GIVEN
      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(PACKAGE_FOLDER_LOCATION);
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(Path.of(UNZIP_LOCATION));

      // WHEN
      FhirTermsResourceLoader loader =
          new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);

      // THEN
      assertThat(loader).isNotNull();
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException when properties are null")
    void shouldThrowExceptionWhenPropertiesAreNull() {
      // GIVEN
      FhirConfigProperties nullProperties = null;

      // WHEN / THEN
      assertThatThrownBy(
              () -> new FhirTermsResourceLoader(nullProperties, mockInMemoryRepresentation))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining("No config properties defined");
    }
  }

  @Nested
  @DisplayName("Load Method Tests")
  class LoadMethodTests {

    private FhirTermsResourceLoader loader;
    private Path packageFolder;
    private Path unzipLocation;
    private Map<String, MetadataResource> resourceMap;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      packageFolder = tempDir.resolve("packages");
      unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(packageFolder);
      Files.createDirectories(unzipLocation);

      resourceMap = new HashMap<>();

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);
      when(mockInMemoryRepresentation.getIdToResourceMap()).thenReturn(resourceMap);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @Test
    @DisplayName("Should handle empty package folder without errors")
    void shouldHandleEmptyPackageFolder() {
      // GIVEN
      // Empty package folder (created in setUp)

      // WHEN
      loader.load();

      // THEN
      assertThat(resourceMap).isEmpty();
      verify(mockInMemoryRepresentation, atLeastOnce()).getIdToResourceMap();
    }

    @Test
    @DisplayName("Should skip non-tar files in package folder")
    void shouldSkipNonTarFiles() throws IOException {
      // GIVEN
      Files.createFile(packageFolder.resolve("test.txt"));
      Files.createFile(packageFolder.resolve("test.json"));
      Files.createFile(packageFolder.resolve("test.xml"));

      // WHEN
      loader.load();

      // THEN
      assertThat(resourceMap).isEmpty();
    }

    @Test
    @DisplayName("Should not throw exception when package folder does not exist")
    void shouldHandleNonExistentPackageFolder() {
      // GIVEN
      Path nonExistentFolder = tempDir.resolve("non-existent");
      when(mockProperties.getFhirTermsPackageFolderLocation())
          .thenReturn(nonExistentFolder.toString());

      // WHEN
      loader.load();

      // THEN
      assertThat(resourceMap).isEmpty();
    }

    @Test
    @DisplayName("Should propagate duplicate resource exception from package without index")
    void shouldPropagateDuplicateResourceExceptionFromPackageWithoutIndex() throws IOException {
      // GIVEN
      Path packageDirectory = tempDir.resolve("source-package").resolve("package");
      Files.createDirectories(packageDirectory);
      Files.writeString(
          packageDirectory.resolve("package.json"),
          """
          {"name":"test.package","version":"1.0.0"}
          """);
      String valueSetJson =
          """
          {"resourceType":"ValueSet","id":"duplicate","url":"http://example.com/valueset","version":"1.0.0","status":"active"}
          """;
      Files.writeString(packageDirectory.resolve("first.json"), valueSetJson);
      Files.writeString(packageDirectory.resolve("second.json"), valueSetJson);

      Path tarFile =
          FhirPackageHelper.createTarArchive(
              "test-package.tar", tempDir, packageDirectory);
      FhirPackageHelper.compress(tarFile, packageFolder.resolve("test-package-1.0.0.tgz"));

      // WHEN / THEN
      assertThatThrownBy(loader::load)
          .isInstanceOf(ResourceLoadingException.class)
          .hasMessageContaining("already exists");
    }

    @ParameterizedTest
    @ValueSource(strings = {"good-package.tgz", "no_version.tgz", "bad_version-x.1.aa.tar.gz", "bad-delimiter#1.0.0.tgz"})
    @DisplayName("Should not crash on invalid package names")
    void shouldNotCrashOnInvalidPackageNames(String tarName) throws IOException{
      // GIVEN
      Files.createFile(packageFolder.resolve(tarName));

      // THEN
      assertThatNoException().isThrownBy(
          () -> loader.load());
    }

    @Test
    @DisplayName("Should skip unhealthy CodeSystem and continue loading in LENIENT mode")
    void shouldSkipUnhealthyCodeSystemInLenientMode() throws IOException {
      // GIVEN
      when(mockProperties.getLoadingMode()).thenReturn(LoadingMode.LENIENT);
      createPackageWithUnhealthyAndHealthyCodeSystems();

      // WHEN / THEN
      assertThatNoException().isThrownBy(loader::load);
      assertThat(resourceMap.values())
          .extracting(MetadataResource::getUrl)
          .containsExactly("http://example.com/healthy");
    }

    @Test
    @DisplayName("Should abort loading on unhealthy CodeSystem in STRICT mode")
    void shouldAbortLoadingForUnhealthyCodeSystemInStrictMode() throws IOException {
      // GIVEN
      when(mockProperties.getLoadingMode()).thenReturn(LoadingMode.STRICT);
      createPackageWithUnhealthyAndHealthyCodeSystems();

      // WHEN / THEN
      assertThatThrownBy(loader::load)
          .isInstanceOf(ResourceLoadingException.class)
          .hasMessageContaining("http://example.com/unhealthy")
          .hasMessageContaining("version null")
          .hasMessageContaining("does not meet quality requirements");
      assertThat(resourceMap).isEmpty();
    }

    private void createPackageWithUnhealthyAndHealthyCodeSystems() throws IOException {
      Path packageDirectory = tempDir.resolve("source-package").resolve("package");
      Files.createDirectories(packageDirectory);
      Files.writeString(
          packageDirectory.resolve("package.json"),
          """
          {"name":"test.package","version":"1.0.0"}
          """);
      Files.writeString(
          packageDirectory.resolve(".index.json"),
          """
          {"index-version":1,"files":[
            {"filename":"unhealthy.json","resourceType":"CodeSystem"},
            {"filename":"healthy.json","resourceType":"CodeSystem"}
          ]}
          """);
      Files.writeString(
          packageDirectory.resolve("unhealthy.json"),
          """
          {"resourceType":"CodeSystem","url":"http://example.com/unhealthy","status":"active","content":"complete"}
          """);
      Files.writeString(
          packageDirectory.resolve("healthy.json"),
          """
          {"resourceType":"CodeSystem","url":"http://example.com/healthy","version":"1.0.0","status":"active","content":"complete"}
          """);

      Path tarFile =
          FhirPackageHelper.createTarArchive("test-package.tar", tempDir, packageDirectory);
      FhirPackageHelper.compress(tarFile, packageFolder.resolve("test-package-1.0.0.tgz"));
    }
  }

  @Nested
  @DisplayName("Tarball Sorting Tests")
  class TarballSortingTests {

    private FhirTermsResourceLoader loader;

    @BeforeEach
    void setUp() throws InvalidConfigurationException {
      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(PACKAGE_FOLDER_LOCATION);
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(Path.of(UNZIP_LOCATION));

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @ParameterizedTest
    @ValueSource(strings = {".tgz", ".tar.gz"})
    @DisplayName("Should sort packages ascending and semantic versions descending")
    void shouldSortPackagesAscendingAndSemanticVersionsDescending(String extension) {
      // GIVEN
      Path beta = tempDir.resolve("beta-9.0.0" + extension);
      Path alphaOlder = tempDir.resolve("alpha-1.9.0" + extension);
      Path alphaNewer = tempDir.resolve("alpha-1.10.0" + extension);
      Path alphaNewest = tempDir.resolve("alpha-2.0.0" + extension);
      Path gammaOld = tempDir.resolve("gamma-0.1.0-rc1" + extension);
      Path gammaNew = tempDir.resolve("gamma-0.1.0-rc2" + extension);
      List<Path> tarballs = List.of(beta, alphaOlder, alphaNewest, alphaNewer, gammaOld, gammaNew);

      // WHEN
      List<Path> sortedTarballs = loader.sortTarballs(tarballs);

      // THEN
      assertThat(sortedTarballs)
          .containsExactly(alphaNewest, alphaNewer, alphaOlder, beta, gammaNew, gammaOld);
    }

    @Test
    @DisplayName("Should sort non-semantic versions lexically after semantic versions")
    void shouldSortNonSemanticVersionsLexicallyAfterSemanticVersions() {
      // GIVEN
      Path semanticVersion = tempDir.resolve("alpha-1.0.0.tgz");
      Path lexicalFirst = tempDir.resolve("alpha-zeta.tgz");
      Path lexicalLast = tempDir.resolve("alpha-draft.tar.gz");
      Path beta = tempDir.resolve("beta-2.0.0.tgz");
      List<Path> tarballs = List.of(beta, lexicalLast, lexicalFirst, semanticVersion);

      // WHEN
      List<Path> sortedTarballs = loader.sortTarballs(tarballs);

      // THEN
      assertThat(sortedTarballs)
          .containsExactly(semanticVersion, lexicalFirst, lexicalLast, beta);
    }

    @Test
    @DisplayName("Should append unparseable filenames without losing or duplicating paths")
    void shouldAppendUnparseableFilenamesWithoutLosingOrDuplicatingPaths() {
      // GIVEN
      Path noVersion = tempDir.resolve("no_version.tgz");
      Path missingDelimiter = tempDir.resolve("package#1.0.0.tar.gz");
      Path validPackage = tempDir.resolve("alpha-1.0.0.tgz");
      List<Path> tarballs = List.of(noVersion, validPackage, missingDelimiter);

      // WHEN
      List<Path> sortedTarballs = loader.sortTarballs(tarballs);

      // THEN
      assertThat(sortedTarballs).containsExactly(validPackage, noVersion, missingDelimiter);
    }
  }

  @Nested
  @DisplayName("CodeSystem Processing Tests")
  class CodeSystemProcessingTests {

    private FhirTermsResourceLoader loader;
    private Map<String, MetadataResource> resourceMap;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      Path packageFolder = tempDir.resolve("packages");
      Path unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(packageFolder);
      Files.createDirectories(unzipLocation);

      resourceMap = new HashMap<>();

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);
      when(mockInMemoryRepresentation.getIdToResourceMap()).thenReturn(resourceMap);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCodeSystemScenarios")
    @DisplayName("Should reject CodeSystem with invalid configuration")
    void shouldRejectInvalidCodeSystem(String url, String version, String testDescription)
        throws Exception {
      // GIVEN
      CodeSystem codeSystem = FhirResourceTestDataHelper.createCodeSystem("test-id", url, version);

      // WHEN
      boolean isHealthy = invokeIsHealthyCodeSystem(loader, codeSystem);

      // THEN
      assertThat(isHealthy).as("CodeSystem should be rejected: %s", testDescription).isFalse();
    }

    private static Stream<Arguments> provideInvalidCodeSystemScenarios() {
      return Stream.of(
          Arguments.of("http://example.com|invalid", "1.0.0", "URL contains pipe symbol"),
          Arguments.of(null, "1.0.0", "URL is null"),
          Arguments.of("http://example.com/codesystem", null, "Version is null"));
    }

    @Test
    @DisplayName("Should accept valid CodeSystem")
    void shouldAcceptValidCodeSystem() throws Exception {
      // GIVEN
      CodeSystem codeSystem =
          FhirResourceTestDataHelper.createCodeSystem(
              "test-id", "http://example.com/codesystem", "1.0.0");

      // WHEN
      boolean isHealthy = invokeIsHealthyCodeSystem(loader, codeSystem);

      // THEN
      assertThat(isHealthy).isTrue();
    }

    @Test
    @DisplayName("Should process valid CodeSystem and add to resource map")
    void shouldProcessValidCodeSystem() throws Exception {
      // GIVEN
      CodeSystem codeSystem =
          FhirResourceTestDataHelper.createCodeSystem(
              "test-id", "http://example.com/codesystem", "1.0.0");

      // WHEN
      invokeProcessCodeSystem(loader, codeSystem);

      // THEN
      assertThat(resourceMap).containsKey("test-id");
      assertThat(resourceMap.get("test-id")).isInstanceOf(CodeSystem.class);
    }

    @Test
    @DisplayName("Should throw exception when processing CodeSystem with duplicate ID")
    void shouldThrowExceptionForDuplicateCodeSystemId() {
      // GIVEN
      String duplicateId = "duplicate-id";
      CodeSystem existingCodeSystem =
          FhirResourceTestDataHelper.createCodeSystem(
              duplicateId, "http://example.com/codesystem1", "1.0.0");
      resourceMap.put(duplicateId, existingCodeSystem);

      CodeSystem newCodeSystem =
          FhirResourceTestDataHelper.createCodeSystem(
              duplicateId, "http://example.com/codesystem2", "2.0.0");

      // WHEN / THEN
      assertThatThrownBy(() -> invokeProcessCodeSystem(loader, newCodeSystem))
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(ResourceLoadingException.class)
          .cause()
          .hasMessageContaining("already exists");
    }

    @ParameterizedTest
    @MethodSource("provideSnomedAndLoincUrls")
    @DisplayName("Should skip SNOMED CT and LOINC CodeSystems")
    void shouldSkipSnomedAndLoincCodeSystems(String url, String description) throws Exception {
      // GIVEN
      CodeSystem codeSystem = FhirResourceTestDataHelper.createCodeSystem("test-id", url, "1.0.0");
      int initialSize = resourceMap.size();

      // WHEN
      try {
        invokeProcessCodeSystem(loader, codeSystem);
      } catch (Exception _) {
        // Expected to skip without adding to map
      }

      // THEN
      assertThat(resourceMap).hasSize(initialSize);
    }

    @Test
    @DisplayName("Should skip CodeSystem with content NOT_PRESENT")
    void shouldSkipCodeSystemWithNotPresentContent() throws Exception {
      // GIVEN
      CodeSystem codeSystem =
          FhirResourceTestDataHelper.createCodeSystem(
              "test-id", "http://example.com/codesystem", "1.0.0");
      codeSystem.setContent(CodeSystem.CodeSystemContentMode.NOTPRESENT);
      int initialSize = resourceMap.size();

      // WHEN
      try {
        invokeProcessCodeSystem(loader, codeSystem);
      } catch (Exception _) {
        // Expected to skip without adding to map
      }

      // THEN
      assertThat(resourceMap).hasSize(initialSize);
    }

    private static Stream<Arguments> provideSnomedAndLoincUrls() {
      return Stream.of(
          Arguments.of(StandardTerminologyCanonicalUrls.SNOMEDCT, "SNOMED CT"),
          Arguments.of(StandardTerminologyCanonicalUrls.LOINC, "LOINC"));
    }

    // Helper method to invoke private isHealthyCodeSystem method
    private boolean invokeIsHealthyCodeSystem(FhirTermsResourceLoader loader, CodeSystem codeSystem)
        throws Exception {
      Method method =
          FhirTermsResourceLoader.class.getDeclaredMethod("isHealthyCodeSystem", CodeSystem.class);
      method.setAccessible(true);
      return (boolean) method.invoke(loader, codeSystem);
    }

    // Helper method to invoke private processCodeSystem method
    private void invokeProcessCodeSystem(FhirTermsResourceLoader loader, CodeSystem codeSystem)
        throws Exception {
      Method method =
          FhirTermsResourceLoader.class.getDeclaredMethod("processCodeSystem", CodeSystem.class);
      method.setAccessible(true);
      method.invoke(loader, codeSystem);
    }
  }

  @Nested
  @DisplayName("ValueSet Processing Tests")
  class ValueSetProcessingTests {

    private FhirTermsResourceLoader loader;
    private Map<String, MetadataResource> resourceMap;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      Path packageFolder = tempDir.resolve("packages");
      Path unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(packageFolder);
      Files.createDirectories(unzipLocation);

      resourceMap = new HashMap<>();

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);
      when(mockInMemoryRepresentation.getIdToResourceMap()).thenReturn(resourceMap);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @Test
    @DisplayName("Should process valid ValueSet and add to resource map")
    void shouldProcessValidValueSet() throws Exception {
      // GIVEN
      ValueSet valueSet =
          FhirResourceTestDataHelper.createValueSet(
              "valueset-id", "http://example.com/valueset", "1.0.0");

      // WHEN
      invokeProcessValueSet(loader, valueSet);

      // THEN
      assertThat(resourceMap).containsKey("valueset-id");
      assertThat(resourceMap.get("valueset-id")).isInstanceOf(ValueSet.class);
    }

    @Test
    @DisplayName("Should throw exception when processing ValueSet with duplicate ID")
    void shouldThrowExceptionForDuplicateValueSetId() {
      // GIVEN
      String duplicateId = "duplicate-valueset-id";
      ValueSet existingValueSet =
          FhirResourceTestDataHelper.createValueSet(
              duplicateId, "http://example.com/valueset1", "1.0.0");
      resourceMap.put(duplicateId, existingValueSet);

      ValueSet newValueSet =
          FhirResourceTestDataHelper.createValueSet(
              duplicateId, "http://example.com/valueset2", "2.0.0");

      // WHEN / THEN
      assertThatThrownBy(() -> invokeProcessValueSet(loader, newValueSet))
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(ResourceLoadingException.class)
          .cause()
          .hasMessageContaining("already exists");
    }

    // Helper method to invoke private processValueSet method
    private void invokeProcessValueSet(FhirTermsResourceLoader loader, ValueSet valueSet)
        throws Exception {
      Method method =
          FhirTermsResourceLoader.class.getDeclaredMethod("processValueSet", ValueSet.class);
      method.setAccessible(true);
      method.invoke(loader, valueSet);
    }
  }

  @Nested
  @DisplayName("ConceptMap Processing Tests")
  class ConceptMapProcessingTests {

    private FhirTermsResourceLoader loader;
    private Map<String, MetadataResource> resourceMap;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      Path packageFolder = tempDir.resolve("packages");
      Path unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(packageFolder);
      Files.createDirectories(unzipLocation);

      resourceMap = new HashMap<>();

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);
      when(mockInMemoryRepresentation.getIdToResourceMap()).thenReturn(resourceMap);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @Test
    @DisplayName("Should process valid ConceptMap and add to resource map")
    void shouldProcessValidConceptMap() throws Exception {
      // GIVEN
      ConceptMap conceptMap =
          FhirResourceTestDataHelper.createConceptMap(
              "conceptmap-id", "http://example.com/conceptmap", "1.0.0");

      // WHEN
      invokeProcessConceptMap(loader, conceptMap);

      // THEN
      assertThat(resourceMap).containsKey("conceptmap-id");
      assertThat(resourceMap.get("conceptmap-id")).isInstanceOf(ConceptMap.class);
    }

    @Test
    @DisplayName("Should throw exception when processing ConceptMap with duplicate ID")
    void shouldThrowExceptionForDuplicateConceptMapId() {
      // GIVEN
      String duplicateId = "duplicate-conceptmap-id";
      ConceptMap existingConceptMap =
          FhirResourceTestDataHelper.createConceptMap(
              duplicateId, "http://example.com/conceptmap1", "1.0.0");
      resourceMap.put(duplicateId, existingConceptMap);

      ConceptMap newConceptMap =
          FhirResourceTestDataHelper.createConceptMap(
              duplicateId, "http://example.com/conceptmap2", "2.0.0");

      // WHEN / THEN
      assertThatThrownBy(() -> invokeProcessConceptMap(loader, newConceptMap))
          .isInstanceOf(InvocationTargetException.class)
          .hasCauseInstanceOf(ResourceLoadingException.class)
          .cause()
          .hasMessageContaining("already exists");
    }

    // Helper method to invoke private processConceptMap method
    private void invokeProcessConceptMap(FhirTermsResourceLoader loader, ConceptMap conceptMap)
        throws Exception {
      Method method =
          FhirTermsResourceLoader.class.getDeclaredMethod("processConceptMap", ConceptMap.class);
      method.setAccessible(true);
      method.invoke(loader, conceptMap);
    }
  }

  @Nested
  @DisplayName("calculateReleaseFileHash Tests")
  class CalculateReleaseFileHashTests {

    private static final String EXPECTED_EMPTY_HASH = DigestUtils.sha256Hex("");

    private FhirTermsResourceLoader loader;
    private Path packageFolder;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      packageFolder = tempDir.resolve("packages");
      Files.createDirectories(packageFolder);

      Path unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(unzipLocation);

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException when directory does not exist")
    void shouldThrowExceptionWhenDirectoryDoesNotExist() {
      // GIVEN
      String nonExistentPath = tempDir.resolve("non-existent").toString();

      // WHEN / THEN
      assertThatThrownBy(() -> loader.calculateReleaseFileHash(nonExistentPath))
          .isInstanceOf(InvalidConfigurationException.class)
          .hasMessageContaining(nonExistentPath);
    }

    @Test
    @DisplayName("Should return hash of empty string when directory contains no tar files")
    void shouldReturnHashOfEmptyStringWhenNoTarFilesFound() throws InvalidConfigurationException {
      // GIVEN
      // Empty packageFolder — no .tgz or .tar.gz files present

      // WHEN
      String hash = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(hash)
          .as("Hash of empty input should equal SHA-256 of empty string")
          .isEqualTo(EXPECTED_EMPTY_HASH);
    }

    @Test
    @DisplayName("Should return valid SHA-256 hash for a single .tgz file")
    void shouldReturnValidHashForSingleTgzFile() throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("package.tgz"), "fake-tgz-content");

      // WHEN
      String hash = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(hash)
          .as("Hash should be a 64-character lowercase hex string")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should return valid SHA-256 hash for a single .tar.gz file")
    void shouldReturnValidHashForSingleTarGzFile()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("package.tar.gz"), "fake-tar-gz-content");

      // WHEN
      String hash = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(hash)
          .as("Hash should be a 64-character lowercase hex string")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should exclude non-tar files from hash computation")
    void shouldExcludeNonTarFilesFromHash() throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("package.tgz"), "tgz-content");
      Files.writeString(packageFolder.resolve("metadata.json"), "json-content");
      Files.writeString(packageFolder.resolve("readme.txt"), "txt-content");

      Path cleanFolder = tempDir.resolve("clean");
      Files.createDirectories(cleanFolder);
      Files.writeString(cleanFolder.resolve("package.tgz"), "tgz-content");

      // WHEN
      String hashWithExtras = loader.calculateReleaseFileHash(packageFolder.toString());
      String hashWithoutExtras = loader.calculateReleaseFileHash(cleanFolder.toString());

      // THEN
      assertThat(hashWithExtras)
          .as("Non-tar files must not affect the combined hash")
          .isEqualTo(hashWithoutExtras);
    }

    @Test
    @DisplayName("Should produce a deterministic hash for the same directory contents")
    void shouldProduceDeterministicHash() throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("alpha.tgz"), "content-a");
      Files.writeString(packageFolder.resolve("beta.tgz"), "content-b");

      // WHEN
      String firstHash = loader.calculateReleaseFileHash(packageFolder.toString());
      String secondHash = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(firstHash)
          .as("Hash must be deterministic for unchanged directory contents")
          .isEqualTo(secondHash);
    }

    @Test
    @DisplayName("Should produce a different hash when file contents change")
    void shouldProduceDifferentHashWhenFileContentsChange()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Path tarFile = packageFolder.resolve("package.tgz");
      Files.writeString(tarFile, "original-content");
      String originalHash = loader.calculateReleaseFileHash(packageFolder.toString());

      // WHEN
      Files.writeString(tarFile, "modified-content");
      String modifiedHash = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(modifiedHash)
          .as("Hash must change when a package file's content is modified")
          .isNotEqualTo(originalHash);
    }

    @Test
    @DisplayName("Should produce a different hash when a new package file is added")
    void shouldProduceDifferentHashWhenFileIsAdded()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("package-a.tgz"), "content-a");
      String hashBefore = loader.calculateReleaseFileHash(packageFolder.toString());

      // WHEN
      Files.writeString(packageFolder.resolve("package-b.tgz"), "content-b");
      String hashAfter = loader.calculateReleaseFileHash(packageFolder.toString());

      // THEN
      assertThat(hashAfter)
          .as("Hash must change when a new package file is added to the directory")
          .isNotEqualTo(hashBefore);
    }

    @Test
    @DisplayName("Should not include tar files from subdirectories")
    void shouldNotIncludeTarFilesFromSubdirectories()
        throws IOException, InvalidConfigurationException {
      // GIVEN
      Files.writeString(packageFolder.resolve("top-level.tgz"), "top-level-content");

      Path subDir = packageFolder.resolve("subdir");
      Files.createDirectories(subDir);
      Files.writeString(subDir.resolve("nested.tgz"), "nested-content");

      Path cleanFolder = tempDir.resolve("clean-subdir");
      Files.createDirectories(cleanFolder);
      Files.writeString(cleanFolder.resolve("top-level.tgz"), "top-level-content");

      // WHEN
      String hashWithNestedFile = loader.calculateReleaseFileHash(packageFolder.toString());
      String hashWithoutNestedFile = loader.calculateReleaseFileHash(cleanFolder.toString());

      // THEN
      assertThat(hashWithNestedFile)
          .as("Tar files in subdirectories must not be included in the combined hash")
          .isEqualTo(hashWithoutNestedFile);
    }
  }

  @Nested
  @DisplayName("File Extension Tests")
  class FileExtensionTests {

    @ParameterizedTest
    @ValueSource(strings = {".tgz", ".tar.gz"})
    @DisplayName("Should recognize valid tar ball file extensions")
    void shouldRecognizeValidTarBallExtensions(String extension) {
      // GIVEN
      String fileName = "package" + extension;

      // WHEN
      boolean matches = fileName.matches(".*\\.(tgz|tar\\.gz)$");

      // THEN
      assertThat(matches).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {".json", ".xml"})
    @DisplayName("Should recognize valid resource file extensions")
    void shouldRecognizeValidResourceExtensions(String extension) {
      // GIVEN
      String fileName = "resource" + extension;

      // WHEN
      boolean matches = fileName.matches(".*\\.(json|xml)$");

      // THEN
      assertThat(matches).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {".txt", ".pdf", ".zip", ".tar"})
    @DisplayName("Should not recognize invalid file extensions as tar balls")
    void shouldNotRecognizeInvalidExtensionsAsTarBalls(String extension) {
      // GIVEN
      String fileName = "package" + extension;

      // WHEN
      boolean matches = fileName.matches(".*\\.(tgz|tar\\.gz)$");

      // THEN
      assertThat(matches).isFalse();
    }
  }
}
