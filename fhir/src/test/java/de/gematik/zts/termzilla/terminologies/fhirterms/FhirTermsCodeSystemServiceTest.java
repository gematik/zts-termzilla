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
package de.gematik.zts.termzilla.terminologies.fhirterms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Execution(ExecutionMode.SAME_THREAD)
class FhirTermsCodeSystemServiceTest {

  private static final String DEFAULT_SCOPE = "fhir";

  private static Path workingPath;
  private Path loadPath;
  private Path indexPath;
  private static Path unzipPath;
  private static FhirConfigProperties mockProperties;
  private FhirTermsTerminologyManager manager;
  private FhirTermsCodeSystemService css;

  @BeforeAll
  static void setUpAll() throws IOException {
    workingPath = Files.createTempDirectory("working");
    unzipPath = Files.createTempDirectory("unzip");
    mockProperties = mock(FhirConfigProperties.class);
    when(mockProperties.isFhirTermsStart()).thenReturn(true);
    when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipPath);
  }

  @BeforeEach
  void setUp() throws IOException {
    loadPath = Files.createTempDirectory("fhir-terms");
    indexPath = Files.createTempDirectory("lucene");
    when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(loadPath.toString());
    when(mockProperties.getFhirTermsIndex()).thenReturn(indexPath);
  }

  @AfterEach
  void tearDown() throws IOException {
    TestPackageHelper.deletePackageDirectory(loadPath);
    TestPackageHelper.deletePackageDirectory(indexPath);
  }

  @AfterAll
  static void tearDownAll() throws IOException {
    TestPackageHelper.deletePackageDirectory(unzipPath);
    TestPackageHelper.deletePackageDirectory(workingPath);
  }

  void setupScenario00() throws InvalidConfigurationException {
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.0.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.1.tgz", loadPath);
    manager = new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, mockProperties);
    css = (FhirTermsCodeSystemService) manager.cs();
  }

  void setupScenario01() throws InvalidConfigurationException {
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.0.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.1.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2025.0.0.tgz", loadPath);
    manager = new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, mockProperties);
    css = (FhirTermsCodeSystemService) manager.cs();
  }

  void setupScenario02() throws InvalidConfigurationException {
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.0.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2024.0.1.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.test-2025.0.0.tgz", loadPath);
    TestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.othertest-1.0.0.tgz", loadPath);
    manager = new FhirTermsTerminologyManager(workingPath, DEFAULT_SCOPE, mockProperties);
    css = (FhirTermsCodeSystemService) manager.cs();
  }

  // ----------------------------------------------------------------------------------------

  @Nested
  class GetSupportedCodeSystemCanonicalsListTest {

    @Test
    void getSupportedCodeSystemCanonicalsList00() throws InvalidConfigurationException {
      setupScenario00();

      List<String> csList = css.getSupportedCodeSystemCanonicalsList();
      assertNotNull(csList);
      assertFalse(csList.isEmpty());
      assertEquals(1, csList.size());
    }

    @Test
    void getSupportedCodeSystemCanonicalsList01() throws InvalidConfigurationException {
      setupScenario01();

      List<String> csList = css.getSupportedCodeSystemCanonicalsList();
      assertNotNull(csList);
      assertFalse(csList.isEmpty());
      assertEquals(1, csList.size());
    }

    @Test
    void getSupportedCodeSystemCanonicalsList02() throws InvalidConfigurationException {
      setupScenario02();

      List<String> csList = css.getSupportedCodeSystemCanonicalsList();
      assertNotNull(csList);
      assertFalse(csList.isEmpty());
      assertEquals(2, csList.size());
    }
  }

  // ----------------------------------------------------------------------------------------

  @Nested
  class GetCodeSystemListTest {

    @Test
    void getCodeSystemList() throws InvalidConfigurationException {
      setupScenario02();

      List<CodeSystem> csList = css.getCodeSystemList();
      assertNotNull(csList);
      assertFalse(csList.isEmpty());
      assertEquals(4, csList.size());
    }
  }

  // ----------------------------------------------------------------------------------------

  @Nested
  class GetDefaultCodeSystemByUrlAndScopeTest {

    static final String CS_URL = "https://terminologien.bfarm.de/fhir/CodeSystem/test";

    @Test
    void getDefaultCodeSystemByUrlAndScope_unsupportedScope() throws InvalidConfigurationException {
      setupScenario02();

      assertThrows(
          UnknownScopeException.class,
          () -> css.getDefaultCodeSystemByUrlAndScope("GIBTSNICHT", CS_URL));
    }

    @Test
    void getDefaultCodeSystemByUrlAndScope_unsupportedTerminology()
        throws InvalidConfigurationException {
      setupScenario02();

      assertThrows(
          UnsupportedTerminologyException.class,
          () ->
              css.getDefaultCodeSystemByUrlAndScope(
                  DEFAULT_SCOPE, "https://example.com/fhir/CodeSystem/gibtsnicht"));
    }

    @ParameterizedTest
    @CsvSource({
      "fhir, 2025, 1",
      "bfarm.terminologien.test-2024.0.0, 2024, 1",
      "bfarm.terminologien.test-2024.0.1, 2024, 2",
      "bfarm.terminologien.test-2025.0.0, 2025, 1"
    })
    void getDefaultCodeSystemByUrlAndScope(
        String scope, String expectedVersion, int expectedConcepts)
        throws InvalidConfigurationException,
            UnsupportedTerminologyException,
            UnknownScopeException {
      setupScenario02();

      CodeSystem cs = css.getDefaultCodeSystemByUrlAndScope(scope, CS_URL);
      assertNotNull(cs);
      assertEquals(CS_URL, cs.getUrl());
      assertEquals(expectedVersion, cs.getVersion());
      assertEquals(expectedConcepts, cs.getConcept().size());
    }
  }

  @Nested
  class GetCodeSystemByScopeAndIdTest {

    static final String CS_URL = "https://terminologien.bfarm.de/fhir/CodeSystem/test";

    @Test
    void getCodeSystemByScopeAndIdTest_unknownScope() throws InvalidConfigurationException {
      setupScenario02();

      assertThrows(
          UnknownScopeException.class, () -> css.getCodeSystem("GIBTSNICHT", new IdType("123")));
    }

    @Test
    void getCodeSystemByScopeAndIdTest_unknownTerminology() throws InvalidConfigurationException {
      setupScenario02();

      assertThrows(
          UnsupportedTerminologyException.class,
          () -> css.getCodeSystem(DEFAULT_SCOPE, new IdType("123")));
    }

    @Test
    void getCodeSystemByScopeAndIdTest_existingId()
        throws InvalidConfigurationException,
            UnknownScopeException,
            UnsupportedTerminologyException {
      setupScenario02();

      CodeSystem cs = css.getDefaultCodeSystemByUrlAndScope(DEFAULT_SCOPE, CS_URL);
      IdType id = cs.getIdElement();

      cs = css.getCodeSystem(DEFAULT_SCOPE, id);
      assertNotNull(cs);
      assertEquals(CS_URL, cs.getUrl());
      assertEquals(id.getIdPart(), cs.getIdElement().getIdPart());
    }
  }

  // ----------------------------------------------------------------------------------------

  @Nested
  @DisplayName("search CodeSystem tests")
  class SearchCodeSystemTest {

    static final String CS_URL = "https://terminologien.bfarm.de/fhir/CodeSystem/test";

    @Test
    @DisplayName("searchCodeSystem with unknown scope should throw UnknownScopeException")
    void searchCodeSystem_unknownScope() throws InvalidConfigurationException {
      setupScenario02();
      CsSearchParameter csSearchParameter = CsSearchParameter.builder().build();
      csSearchParameter.setUrlParam(new UriParam(CS_URL));

      assertThrows(
          UnknownScopeException.class, () -> css.searchCodeSystem("GIBTSNICHT", csSearchParameter));
    }

    @ParameterizedTest
    @CsvSource({
      "fhir, 2",
      "bfarm.terminologien.test-2024.0.0, 1",
      "bfarm.terminologien.test-2024.0.1, 1",
      "bfarm.terminologien.test-2025.0.0, 1"
    })
    @DisplayName(
        "The search for a certain CodeSystem URL should return the correct number of resources within different scopes")
    void searchCodeSystem_url(String scope, int expectedResourceCount)
        throws InvalidConfigurationException, NotImplementedException, UnknownScopeException {
      setupScenario02();

      CsSearchParameter csSearchParameter = CsSearchParameter.builder().build();
      csSearchParameter.setUrlParam(new UriParam(CS_URL));

      List<IdType> resultList = css.searchCodeSystem(scope, csSearchParameter);
      assertNotNull(resultList);
      assertEquals(expectedResourceCount, resultList.size());
    }

    @Test
    void searchCodeSystem_urlAndVersion()
        throws InvalidConfigurationException, NotImplementedException, UnknownScopeException {
      setupScenario02();

      CsSearchParameter csSearchParameter = CsSearchParameter.builder().build();
      csSearchParameter.setUrlParam(new UriParam(CS_URL));
      csSearchParameter.setVersionParam(new TokenParam("2024"));

      List<IdType> resultList = css.searchCodeSystem(DEFAULT_SCOPE, csSearchParameter);
      assertNotNull(resultList);
      assertEquals(1, resultList.size());
    }
  }
}
