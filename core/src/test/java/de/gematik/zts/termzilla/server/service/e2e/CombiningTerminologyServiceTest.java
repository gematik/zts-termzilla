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
package de.gematik.zts.termzilla.server.service.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.fhir.CombiningTerminologyServiceTool;
import de.gematik.zts.fhir.CombiningTestPackageHelper;
import de.gematik.zts.fhir.FhirTerminologyTool;
import de.gematik.zts.fhir.Helper;
import de.gematik.zts.fhir.ValuSetBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Execution(ExecutionMode.SAME_THREAD)
class CombiningTerminologyServiceTest {

  private static final String DEFAULT_SCOPE = "fhir";

  private static final String FHIR_BASE_URL = "https://tx.fhir.org/r4/";
  private static final FhirTerminologyTool tt = new FhirTerminologyTool(FHIR_BASE_URL);

  private static CombiningTerminologyServiceTool ctsTool;
  private static CombiningTerminologyService combiningService;
  private static ConfigProperties mockProperties;
  private static Path baseWorkingDirectory;

  private static FhirTermsTerminologyManager fhirManager;
  private static FhirConfigProperties fhirProperties;
  private static Path fhirPackageLoadPath;

  @BeforeAll
  static void setUpAll() throws InvalidConfigurationException, IOException {

    // Setup FHIR Terminology Manager Configuration
    fhirPackageLoadPath = Files.createTempDirectory("fhir-terms");
    fhirProperties = new FhirConfigProperties();
    fhirProperties.setFhirTermsStart(true);
    fhirProperties.setFhirTermsLoadDependencies(false);
    fhirProperties.setFhirTermsPackageFolderLocation(fhirPackageLoadPath.toString());

    // Setup Combining Terminology Service Configuration
    baseWorkingDirectory = Files.createTempDirectory("working");
    mockProperties = mock(ConfigProperties.class);
    when(mockProperties.getBaseWorkingDir()).thenReturn(baseWorkingDirectory.toString());

    setupScenario00();
  }

  @AfterAll
  static void tearDownAll() throws IOException {
    CombiningTestPackageHelper.deletePackageDirectory(fhirPackageLoadPath);
  }

  // ==================================================================

  private static void setupScenario00() throws InvalidConfigurationException {
    CombiningTestPackageHelper.copyTestPackage(
        "testpackages/hl7.fhir.r4.core-4.0.1.tgz", fhirPackageLoadPath);
    CombiningTestPackageHelper.copyTestPackage(
        "testpackages/bfarm.terminologien.internal-2025.0.0.tgz", fhirPackageLoadPath);
    CombiningTestPackageHelper.copyTestPackage(
        "testpackages/bfarm.internal.misc-1.0.0-rc.20250621074552.tgz", fhirPackageLoadPath);

    fhirManager =
        new FhirTermsTerminologyManager(baseWorkingDirectory, DEFAULT_SCOPE, fhirProperties);
    combiningService = new CombiningTerminologyService(List.of(fhirManager), mockProperties);
    ctsTool = new CombiningTerminologyServiceTool(combiningService);
  }

  // ----------------------------------------------------------------------------------------

  @Nested
  class ValidateCodeValueSetTest {

    @ParameterizedTest
    @CsvSource({
      "http://hl7.org/fhir/versioning-policy,versioned",
      "http://terminology.hl7.org/CodeSystem/need,none",
    })
    void validateCodeVs_provideVsValidateCodeOnly(String url, String code)
        throws NotImplementedException,
            UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException {
      VsValidateCodeParameter validateParameter =
          VsValidateCodeParameter.builder()
              .valueSetParam(ValuSetBuilder.createAllConceptsValueSet(url, null))
              .codeParam(new TokenParam(code))
              .inferSystemParam(new BooleanType(true))
              .build();

      ValidateCodeResult result = combiningService.validateCodeVs(DEFAULT_SCOPE, validateParameter);
      assertTrue(result.isResult(), "The code should be valid in the ValueSet with URL: " + url);
    }

    @ParameterizedTest
    @CsvSource({
      "http://hl7.org/fhir/ValueSet/location-mode,instance",
    })
    void validateCodeVs_referencedVsValidateCodeOnly(String url, String code)
        throws NotImplementedException,
            UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException {
      VsValidateCodeParameter validateParameter =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam(url))
              .codeParam(new TokenParam(code))
              .inferSystemParam(new BooleanType(true))
              .build();

      ValidateCodeResult result = combiningService.validateCodeVs(DEFAULT_SCOPE, validateParameter);
      assertTrue(result.isResult(), "The code should be valid in the ValueSet with URL: " + url);
    }

    @ParameterizedTest
    @CsvSource({
      "http://hl7.org/fhir/versioning-policy,versioned",
      "http://terminology.hl7.org/CodeSystem/need,none"
    })
    @DisplayName("validateCodeVs returns true when only coding is provided")
    void validateCodeVs_codingParameterOnly(String url, String code)
        throws NotImplementedException,
            UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException {

      // GIVEN
      Coding coding = new Coding(url, code, null);
      VsValidateCodeParameter validateParameter =
          VsValidateCodeParameter.builder()
              .valueSetParam(ValuSetBuilder.createAllConceptsValueSet(url, null))
              .codingParam(coding)
              .build();

      // WHEN
      ValidateCodeResult result = combiningService.validateCodeVs(DEFAULT_SCOPE, validateParameter);

      // THEN
      assertTrue(result.isResult(), "The coding should be valid in the ValueSet with URL: " + url);
    }

    @ParameterizedTest
    @CsvSource({"http://hl7.org/fhir/versioning-policy,versioned"})
    @DisplayName("validateCodeVs returns true when codeableConcept has at least one valid coding")
    void validateCodeVs_codeableConceptWithOneValidCoding(String url, String validCode)
        throws NotImplementedException,
            UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException {

      // GIVEN
      CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.addCoding(new Coding(url, "definitely-not-a-valid-code", null));
      codeableConcept.addCoding(new Coding(url, validCode, null));

      VsValidateCodeParameter validateParameter =
          VsValidateCodeParameter.builder()
              .valueSetParam(ValuSetBuilder.createAllConceptsValueSet(url, null))
              .codeableConceptParam(codeableConcept)
              .build();

      // WHEN
      ValidateCodeResult result = combiningService.validateCodeVs(DEFAULT_SCOPE, validateParameter);

      // THEN
      assertTrue(
          result.isResult(),
          "The codeableConcept should be valid if one coding is in the ValueSet.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://hl7.org/fhir/versioning-policy"})
    @DisplayName("validateCodeVs returns false when codeableConcept contains no valid codings")
    void validateCodeVs_codeableConceptWithoutValidCoding(String url)
        throws NotImplementedException,
            UnsupportedTerminologyException,
            UnknownScopeException,
            UnknownConceptException {

      // GIVEN
      CodeableConcept codeableConcept = new CodeableConcept();
      codeableConcept.addCoding(new Coding(url, "invalid-code-a", null));
      codeableConcept.addCoding(new Coding(url, "invalid-code-b", null));

      VsValidateCodeParameter validateParameter =
          VsValidateCodeParameter.builder()
              .valueSetParam(ValuSetBuilder.createAllConceptsValueSet(url, null))
              .codeableConceptParam(codeableConcept)
              .build();

      // WHEN
      ValidateCodeResult result = combiningService.validateCodeVs(DEFAULT_SCOPE, validateParameter);

      // THEN
      assertFalse(
          result.isResult(), "The codeableConcept should be invalid when all codings are invalid.");
      assertEquals("None of the provided Codings is valid in the ValueSet.", result.getMessage());
    }
  }

  // ----------------------------------------------------------------------------------------

  @Tag("E2E")
  @Disabled(
      "Disabled because tx.fhi.org does not always respond in time, causing test failures. This should not be run in regular test runs anyways, but can be enabled for manual testing of the expansion functionality.")
  @Nested
  class ExpandValueSetTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "http://hl7.org/fhir/versioning-policy",
          "http://terminology.hl7.org/CodeSystem/need",
          "http://terminology.hl7.org/CodeSystem/questionnaire-usage-mode"
        })
    void expandVS_urlSet(String url)
        throws NotImplementedException,
            UnknownConceptException,
            UnsupportedTerminologyException,
            UnknownScopeException {

      ValueSet vs = ValuSetBuilder.createAllConceptsValueSet(url, null);

      VsExpandParameter expandParameter = VsExpandParameter.builder().valueSetParam(vs).build();

      List<ValueSet.ValueSetExpansionContainsComponent> expandResultLocal =
          ctsTool.expandVs(expandParameter, DEFAULT_SCOPE);

      System.out.println(
          FhirContext.forR4().newJsonParser().setPrettyPrint(true).encodeResourceToString(vs));

      expandParameter.setOffsetParam(null);
      List<ValueSet.ValueSetExpansionContainsComponent> expandResultRemote =
          tt.expandVs(expandParameter);

      assertTrue(
          Helper.hasSameContent(expandResultLocal, expandResultRemote),
          "The local and remote expansion results should have the same content.");
    }
  }

  // ----------------------------------------------------------------------------------------

}
