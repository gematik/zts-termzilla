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
package de.gematik.zts.termzilla.testing.expand;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.DEFAULT_CS_URL_PREFIX;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.DEFAULT_VS_URL_PREFIX;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS1_2025_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS1_2026_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS2_2026_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_A;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_B;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_C;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_D;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCSS00_C;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCSS00_D;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A0;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B0;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_C1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_D2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_G2X;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS00_A;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS00_B;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS01;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS02;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS03;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS04;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS05;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS06;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS07;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS08;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS09;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS10;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS11;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS12;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS13;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS14;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS15;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS16;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS17;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS18;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloadException;
import de.gematik.zts.termzilla.common.packages.downloader.PackageDownloader;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonBundleAsserts;
import de.gematik.zts.termzilla.testing.common.asserts.CommonMetadateResourceAsserts;
import de.gematik.zts.termzilla.testing.common.asserts.CommonValueSetExpansionAsserts;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import de.gematik.zts.termzilla.testutils.valueset.ValueSetGenerator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
public class ExpandExpectedBehaviourIT extends BaseFhirIT {

  public static final String EXPAND_PACKAGE_NAME = "bfarm.terminologien.expandtests";
  public static final String EXPAND_PACKAGE_VERSION_2025 = "2025.0.0";
  public static final String EXPAND_PACKAGE_VERSION_2026 = "2026.0.0";
  public static final String LANGUAGE_EXPAND_PACKAGE_NAME =
      "bfarm.terminologien.expandlanguagetests";
  public static final String LANGUAGE_EXPAND_PACKAGE_VERSION_2026 = "1.0.0";
  public static final String SYSTEM_VERSION = "system-version";
  public static final String CHECK_SYSTEM_VERSION = "check-system-version";
  public static final String FORCE_SYSTEM_VERSION = "force-system-version";
  public static final String DISPLAY_LANGUAGE = "displayLanguage";
  public static final String CS_SEX_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject";
  public static final String VS_SEX_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-sexreject";

  // -------------------------------------------------------------------------------------------------------------------
  // Test data preparation
  // -------------------------------------------------------------------------------------------------------------------

  @Override
  protected void copyData(Path packageLoadTempDir) throws PackageDownloadException {
    try (PackageDownloader downloader =
        new PackageDownloader(
            120,
            "https://terminologien.bfarm.de",
            AuthenticationMode.BFARM_AUTO_TOKEN,
            null,
            null)) {
      downloader.downloadPackage("bfarm.terminologien.icd10gm", "2026.0.0", packageLoadTempDir);
      downloader.downloadPackage("bfarm.terminologien.icd10gm", "2025.0.0", packageLoadTempDir);
    }

    try {
      prepareTestDataPackage2025(packageLoadTempDir);
      prepareTestDataPackage2026(packageLoadTempDir);
      prepareLanguageTestDataPackage(packageLoadTempDir);
    } catch (PackageGenerationException e) {
      throw new RuntimeException(e);
    }
  }

  private void prepareTestDataPackage2025(Path packageLoadTempDir)
      throws PackageGenerationException {
    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(EXPAND_PACKAGE_NAME)
            .packageVersion(EXPAND_PACKAGE_VERSION_2025)
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createCodeSystem_CS1_2025_deDE_deDE());
    resources.add(createValueSet_VS00_A());
    resources.add(createValueSet_VS04());
    resources.add(createValueSet_VS05());
    resources.add(createValueSet_VS06());
    resources.add(createValueSet_VS07());
    resources.add(createValueSet_VS10());
    resources.add(createValueSet_VS12());
    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  private void prepareTestDataPackage2026(Path packageLoadTempDir)
      throws PackageGenerationException {
    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(EXPAND_PACKAGE_NAME)
            .packageVersion(EXPAND_PACKAGE_VERSION_2026)
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createCodeSystem_CS1_2026_deDE_deDE());
    resources.add(createCodeSystem_CS2_2026_deDE_deDE());
    resources.add(createValueSet_VS00_B());
    resources.add(createValueSet_VS01());
    resources.add(createValueSet_VS02());
    resources.add(createValueSet_VS03());
    resources.add(createValueSet_VS08());
    resources.add(createValueSet_VS09());
    resources.add(createValueSet_VS11());
    resources.add(createValueSet_VS13());
    resources.add(createValueSet_VS14());
    resources.add(createValueSet_VS15());
    resources.add(createValueSet_VS16());
    resources.add(createValueSet_VS17());
    resources.add(createValueSet_VS18());
    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  private void prepareLanguageTestDataPackage(Path packageLoadTempDir)
      throws PackageGenerationException {
    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(LANGUAGE_EXPAND_PACKAGE_NAME)
            .packageVersion(LANGUAGE_EXPAND_PACKAGE_VERSION_2026)
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createCodeSystem_LCS00_A());
    resources.add(createCodeSystem_LCS00_B());
    resources.add(createCodeSystem_LCS00_C());
    resources.add(createCodeSystem_LCSS00_C());
    resources.add(createCodeSystem_LCS00_D());
    resources.add(createCodeSystem_LCSS00_D());

    resources.add(createValueSet_LVS00_A0());
    resources.add(createValueSet_LVS00_A1());
    resources.add(createValueSet_LVS00_A2());
    resources.add(createValueSet_LVS00_B0());
    resources.add(createValueSet_LVS00_B1());
    resources.add(createValueSet_LVS00_B2());
    resources.add(createValueSet_LVS00_C1());
    resources.add(createValueSet_LVS00_D2());

    resources.add(createValueSet_LVS00_G2X());

    packageGeneration.generatePackage(resources, packageLoadTempDir);
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Test Existence of Test Data
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Testdata Generation Tests")
  class TestdataGenerationTests {

    @ParameterizedTest
    @DisplayName("get resources in scope by url and version with _summary=true")
    @CsvSource({
      "fhir, ValueSet, http://example.org/fhir/ValueSet/VS00, 2025",
    })
    void testGetResourceSummaryByUrlAndVersionInScope(
        String scope, String resourceType, String canonicalUrl, String version) {

      // GIVEN
      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      // WHEN
      Bundle searchBundle =
          client
              .search()
              .byUrl(resourceType + "?url=" + canonicalUrl + "&version=" + version)
              .summaryMode(SummaryEnum.TRUE)
              .returnBundle(Bundle.class)
              .execute();

      // THEN
      CommonBundleAsserts.assertBundleHasValidId(searchBundle);
      CommonBundleAsserts.assertBundleHasCorrectType(searchBundle, Bundle.BundleType.SEARCHSET);
      CommonBundleAsserts.assertBundleHasSubsettedMetaTag(searchBundle);
      CommonBundleAsserts.assertBundleHasCorrectTotalValue(searchBundle, 1, scope);
      CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(searchBundle, 1);
      CommonMetadateResourceAsserts.asserMetadataResourceHasCorrectUrlAndVersion(
          (MetadataResource) searchBundle.getEntry().getFirst().getResource(),
          canonicalUrl,
          version);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Parameter Combination Tests 'xyz-system-version'
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("ExpandParameterCombinationsTests")
  class ExpandParameterCombinationsTests {

    @ParameterizedTest
    @DisplayName("ValueSet expansion - expansion by VS-url and VS-version")
    @CsvSource(
        delimiter = '|',
        nullValues = {"NULL"},
        value = {
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | NULL | 2026 | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2026 | NULL | NULL | 2026 | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | NULL | NULL | 2025 | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2026 | NULL | NULL | 2026 | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'system-version/force-system-version' parameter combination",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | true | Invalid 'system-version/check-system-version' parameter combination",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'check-system-version/force-system-version' parameter combination",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | XYZ | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2026 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | XYZ | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2025 | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2026 | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2026 | NULL | NULL | NULL | NULL | 2026 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | NULL | NULL | 2025 | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'system-version/force-system-version' parameter combination",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | true | Invalid 'system-version/check-system-version' parameter combination",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'check-system-version/force-system-version' parameter combination",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | XYZ | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2026 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
          "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
        })
    void testExpandPredefiniedValueSet(
        String scope,
        String referencedCsUrl,
        String referencedCsVersionInVS,
        String systemVersion,
        String checkSystemVersion,
        String forceSystemVersion,
        String expectedSystemVersion,
        String expectedCheckSystemVersion,
        String expectedForceSystemVersion,
        String expectedResultVersion,
        boolean expectException,
        String expectedErrorMessage) {

      // GIVEN
      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      Parameters expansionParams =
          prepareExpansionParams(
              referencedCsUrl,
              referencedCsVersionInVS,
              systemVersion,
              checkSystemVersion,
              forceSystemVersion);

      // WHEN

      if (expectException) {

        var operation =
            client
                .operation()
                .onType("ValueSet")
                .named("expand")
                .withParameters(expansionParams)
                .returnResourceType(ValueSet.class);
        assertThatThrownBy(operation::execute)
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(expectedErrorMessage);

      } else {

        ValueSet expandedValueSet =
            client
                .operation()
                .onType("ValueSet")
                .named("expand")
                .withParameters(expansionParams)
                .returnResourceType(ValueSet.class)
                .execute();

        // THEN
        CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

        // make sure we have the expected CS Versions for all expandednconcepts
        assertThat(expandedValueSet.getExpansion().getContains())
            .as("Concepts in Expansion should have the correct CodeSystem version")
            .allSatisfy(
                concept -> {
                  assertThat(concept.getSystem()).isEqualTo(referencedCsUrl);
                  assertThat(concept.getVersion()).isEqualTo(expectedResultVersion);
                });

        // system-version
        if (expectedSystemVersion != null) {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'system-version' parameter expected, but not found")
              .filteredOn(parameter -> parameter.getName().equals(SYSTEM_VERSION))
              .isNotEmpty()
              .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
              .asString()
              .contains(referencedCsUrl + "|" + expectedSystemVersion);
        } else {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'system-version' parameter NOT expected, but found")
              .filteredOn(parameter -> parameter.getName().equals(SYSTEM_VERSION))
              .isEmpty();
        }

        // check-system-version
        if (expectedCheckSystemVersion != null) {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'check-system-version' parameter expected, but not found")
              .filteredOn(parameter -> parameter.getName().equals(CHECK_SYSTEM_VERSION))
              .isNotEmpty()
              .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
              .asString()
              .contains(referencedCsUrl + "|" + expectedCheckSystemVersion);
        } else {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'check-system-version' parameter NOT expected, but found")
              .filteredOn(parameter -> parameter.getName().equals(CHECK_SYSTEM_VERSION))
              .isEmpty();
        }

        // force-system-version
        if (expectedForceSystemVersion != null) {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'force-system-version' parameter expected, but not found")
              .filteredOn(parameter -> parameter.getName().equals(FORCE_SYSTEM_VERSION))
              .isNotEmpty()
              .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
              .asString()
              .contains(referencedCsUrl + "|" + expectedForceSystemVersion);
        } else {
          assertThat(expandedValueSet.getExpansion().getParameter())
              .as("'force-system-version' parameter NOT expected, but found")
              .filteredOn(parameter -> parameter.getName().equals(FORCE_SYSTEM_VERSION))
              .isEmpty();
        }
      }
    }

    // ---------------------------------------------------------------------------------------------

  }

  // -------------------------------------------------------------------------------------------------------------------
  // Handcrafted Tests for Correct Compination Handling
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("HandCraftedScenarioTests")
  class ExpandHandCraftedScenariosTests {

    @ParameterizedTest
    @DisplayName("VS00_A: 1 include: include[0].system=CS1 include[0].version=2025")
    @ValueSource(strings = {"fhir"})
    void scenarioVS00ATest(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS00", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(3)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"));
    }

    @ParameterizedTest
    @DisplayName("VS00_B: 1 include: include[0].system=CS1 include[0].version=2026")
    @ValueSource(strings = {"fhir"})
    void scenarioVS00BTest(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS00", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A04"));
    }

    @ParameterizedTest
    @DisplayName("VS01: 1 include: include[0].system=CS1")
    @ValueSource(strings = {"fhir"})
    void scenarioVS01Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS01", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A04"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS02: 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS1 include[1].version=2026")
    @ValueSource(strings = {"fhir"})
    void scenarioVS02Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS02", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS1-2026#A01, CS1-2026#A02, CS1-2026#A03,
      // CS1-2026#A04
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(7)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A04"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS03: 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS2 include[1].version=2026")
    @ValueSource(strings = {"fhir"})
    void scenarioVS03Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS03", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(6)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B02"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS04: 1 include (extensional): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[0].concept[1].code=A02")
    @ValueSource(strings = {"fhir"})
    void scenarioVS04Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS04", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A02
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"),
              tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS05: 1 include (extensional): include[0].system=CS1 include[0].concept[0].code=A01")
    @ValueSource(strings = {"fhir"})
    void scenarioVS05Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS05", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A01
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(1)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS06: 1 include (extensional with display): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[0].concept[0].display=\"Custom Display\"")
    @ValueSource(strings = {"fhir"})
    void scenarioVS06Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS06", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01 (with custom display)
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(1)
          .extracting("system", "version", "code", "display")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01", "Custom Display"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS07: 1 include + 1 exclude: include[0].system=CS1 include[0].version=2025 exclude[0].system=CS1 exclude[0].version=2025 exclude[0].concept[0].code=A02")
    @ValueSource(strings = {"fhir"})
    void scenarioVS07Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS07", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS08: 1 include + 1 exclude: include[0].system=CS1 include[0].version=2026 exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A01 exclude[0].concept[1].code=A04")
    @ValueSource(strings = {"fhir"})
    void scenarioVS08Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS08", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A02, CS1-2026#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS09: 2 includes + 1 exclude: include[0].system=CS1 include[0].version=2025 include[1].system=CS1 include[1].version=2026 exclude[0].system=CS1 exclude[0].version=2025")
    @ValueSource(strings = {"fhir"})
    void scenarioVS09Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS09", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A04"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS10: 1 include (intensional with filter '='): include[0].system=CS1 include[0].version=2025 include[0].filter[0].property=code include[0].filter[0].op='=' include[0].filter[0].value=A01")
    @ValueSource(strings = {"fhir"})
    void scenarioVS10Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS10", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(1)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS11: 1 include (intensional with filter regex): include[0].system=CS1 include[0].version=2026 include[0].filter[0].property=code include[0].filter[0].op=regex include[0].filter[0].value=A0[12]")
    @ValueSource(strings = {"fhir"})
    void scenarioVS11Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS11", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A01, CS1-2026#A02
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"));
    }

    @ParameterizedTest
    @DisplayName("VS12: 1 include (valueSet import): include[0].valueSet=VS00_A")
    @ValueSource(strings = {"fhir"})
    void scenarioVS12Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS12", "2025");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A02, CS1-2025#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(3)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS13: 2 includes (valueSet import + system): include[0].valueSet=VS00_A include[1].system=CS2 include[1].version=2026")
    @ValueSource(strings = {"fhir"})
    void scenarioVS13Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS13", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(6)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A03"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS14: 1 include (extensional) + 1 exclude (extensional): include[0].system=CS1 include[0].version=2026 include[0].concept[0].code=A01 include[0].concept[1].code=A02 include[0].concept[2].code=A03 exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A02")
    @ValueSource(strings = {"fhir"})
    void scenarioVS14Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS14", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A01, CS1-2026#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS15: 2 includes (mixed): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[1].system=CS2 include[1].version=2026")
    @ValueSource(strings = {"fhir"})
    void scenarioVS15Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS15", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2025#A01, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2025", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS2", "2026", "B03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS16: 1 include (multiple filters AND): include[0].system=CS1 include[0].version=2026 include[0].filter[0].property=code include[0].filter[0].op=regex include[0].filter[0].value=A.* include[0].filter[1].property=code include[0].filter[1].op=regex include[0].filter[1].value=.*[34]")
    @ValueSource(strings = {"fhir"})
    void scenarioVS16Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS16", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A03, CS1-2026#A04
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A04"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS17: 1 include (valueSet import) + 1 exclude: include[0].valueSet=VS00_B exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A04")
    @ValueSource(strings = {"fhir"})
    void scenarioVS17Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS17", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A01, CS1-2026#A02, CS1-2026#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(3)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A01"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"));
    }

    @ParameterizedTest
    @DisplayName(
        "VS18: 1 include (2 valueSet imports): include[0].valueSet[0]=VS00_B include[0].valueSet[1]=VS08")
    @ValueSource(strings = {"fhir"})
    void scenarioVS18Test(String scope) {
      // GIVEN + WHEN
      ValueSet expandedValueSet = createExpansion(scope, null, null, "VS18", "2026");

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we get all expanded concepts
      // CS1-2026#A02, CS1-2026#A03
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A02"))
          .contains(tuple(DEFAULT_CS_URL_PREFIX + "CS1", "2026", "A03"));
    }

    // -----------------------------------------------------------------------------------------------------------------

  }

  // -------------------------------------------------------------------------------------------------------------------
  // Mor handcrafted Tests
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("DifferentScenarioTests")
  class ExpandDifferentScenariosTests {

    @ParameterizedTest
    @DisplayName(
        "One system include without predefined version and 'check-system-version' with unknown version")
    @ValueSource(strings = {"fhir"})
    void oneSystemIncludesWithoutPredefinedVersionAndCheckSystemVersionWithUnknownVersion(
        String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL);
      vs.getCompose().addInclude(include1);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);
      expansionParams.addParameter(
          "check-system-version", new CanonicalType(CS_SEX_REJECT_URL + "|UNKNOWN"));

      // WHEN + THEN
      var operation =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class);
      assertThatThrownBy(operation::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not supported in scope");
    }

    @ParameterizedTest
    @DisplayName(
        "One system include with predefined version and 'check-system-version' with unknown version")
    @ValueSource(strings = {"fhir"})
    void oneSystemIncludeWithPredefinedVersionAndCheckSystemVersionWithUnknownVersion(
        String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String csVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion);
      vs.getCompose().addInclude(include1);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);
      expansionParams.addParameter(
          "check-system-version", new CanonicalType(CS_SEX_REJECT_URL + "|UNKNOWN"));

      var operation =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class);
      // WHEN + THEN
      assertThatThrownBy(operation::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(
              "defined in the include/exclude does not match the version defined in the 'check-system-version'");
    }

    @ParameterizedTest
    @DisplayName("Two system includes with same system and different predefined version")
    @ValueSource(strings = {"fhir"})
    void twoSystemIncludesWithSameVersionAndTwoDifferentPredefinedVersions(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String csVersion1 = "2025";
      String csVersion2 = "2026";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion1);
      vs.getCompose().addInclude(include1);

      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion2);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN

      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, csVersion1, "9"),
              tuple(CS_SEX_REJECT_URL, csVersion1, "K"),
              tuple(CS_SEX_REJECT_URL, csVersion2, "9"),
              tuple(CS_SEX_REJECT_URL, csVersion2, "K"));
    }

    @ParameterizedTest
    @DisplayName("Two system includes with same system and same predefined version")
    @ValueSource(strings = {"fhir"})
    void twoSystemIncludesWithIdenticalPredefinedVersions(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String csVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion);
      vs.getCompose().addInclude(include1);

      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN

      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, csVersion, "9"), tuple(CS_SEX_REJECT_URL, csVersion, "K"));
    }

    @ParameterizedTest
    @DisplayName("Two system includes with same system and without predefined version")
    @ValueSource(strings = {"fhir"})
    void twoSystemIncludesWithoutPredefinedVersions(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL);
      vs.getCompose().addInclude(include1);

      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.setSystem(CS_SEX_REJECT_URL);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN

      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(CS_SEX_REJECT_URL, "2026", "9"), tuple(CS_SEX_REJECT_URL, "2026", "K"));
    }

    @ParameterizedTest
    @DisplayName(
        "Two system includes with same system and one predefined version, one without version")
    @ValueSource(strings = {"fhir"})
    void twoSystemIncludesOnePredefinedOneWithoutVersion(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String csVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion);
      vs.getCompose().addInclude(include1);

      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.setSystem(CS_SEX_REJECT_URL);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getParameter())
          .as("'system-version' parameter expected, but not found")
          .filteredOn(parameter -> parameter.getName().equals(SYSTEM_VERSION))
          .isNotEmpty()
          .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
          .asString()
          .contains(CS_SEX_REJECT_URL + "|2026");

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, "2025", "9"),
              tuple(CS_SEX_REJECT_URL, "2025", "K"),
              tuple(CS_SEX_REJECT_URL, "2026", "9"),
              tuple(CS_SEX_REJECT_URL, "2026", "K"));
    }

    @ParameterizedTest
    @DisplayName("One ValueSet include with ValueSet url-only canonical")
    @ValueSource(strings = {"fhir"})
    void oneValueSetIncludesWithValueSetUrlOnlyCanonical(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL);
      vs.getCompose().addInclude(include1);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN + THEN
      var operation =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class);
      assertThatThrownBy(operation::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("The ValueSet referenced in the include")
          .hasMessageContaining("has no version part set");
    }

    @ParameterizedTest
    @DisplayName("Two ValueSet includes with identical ValueSet url|version canonical")
    @ValueSource(strings = {"fhir", "bfarm.terminologien.icd10gm-2025.0.0"})
    void twoValueSetIncludesWithIdenticalValueSetUrlVersionCanonical(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String vsVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion);
      vs.getCompose().addInclude(include1);
      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, vsVersion, "9"), tuple(CS_SEX_REJECT_URL, vsVersion, "K"));
    }

    @ParameterizedTest
    @DisplayName("Two ValueSet includes with identical ValueSet url but different version")
    @ValueSource(strings = {"fhir"})
    void twoValueSetIncludesWithIdenticalValueSetUrlButDifferentVersion(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String vsVersion1 = "2025";
      String vsVersion2 = "2026";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion1);
      vs.getCompose().addInclude(include1);
      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion2);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(4)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, vsVersion1, "9"),
              tuple(CS_SEX_REJECT_URL, vsVersion1, "K"),
              tuple(CS_SEX_REJECT_URL, vsVersion2, "9"),
              tuple(CS_SEX_REJECT_URL, vsVersion2, "K"));
    }

    @ParameterizedTest
    @DisplayName("Two mixed includes with identical ValueSet url version combination")
    @ValueSource(strings = {"fhir"})
    void twoMixedIncludesWithIdenticalValueSetUrlVersionCombination(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String version = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + version);
      vs.getCompose().addInclude(include1);
      ValueSet.ConceptSetComponent include2 = new ValueSet.ConceptSetComponent();
      include2.setSystem(CS_SEX_REJECT_URL).setVersion(version);
      vs.getCompose().addInclude(include2);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(tuple(CS_SEX_REJECT_URL, version, "9"), tuple(CS_SEX_REJECT_URL, version, "K"));
    }

    @ParameterizedTest
    @DisplayName("One ValueSet include with two identical ValueSet url|version canonicals")
    @ValueSource(strings = {"fhir", "bfarm.terminologien.icd10gm-2025.0.0"})
    void oneValueSetIncludeWithTwoIdenticalValueSetUrlVersionCanonicals(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String vsVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion);
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion);
      vs.getCompose().addInclude(include1);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the correct system, version and code")
          .hasSize(2)
          .extracting("system", "version", "code")
          .contains(
              tuple(CS_SEX_REJECT_URL, vsVersion, "9"), tuple(CS_SEX_REJECT_URL, vsVersion, "K"));
    }

    @ParameterizedTest
    @DisplayName("One ValueSet include with two identical ValueSet url|version canonicals")
    @ValueSource(strings = {"fhir"})
    void oneValueSetIncludeWithTwoIdenticalValueSetUrlsButDifferentVersions(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);

      String vsVersion1 = "2025";
      String vsVersion2 = "2026";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include1 = new ValueSet.ConceptSetComponent();
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion1);
      include1.addValueSet(VS_SEX_REJECT_URL + "|" + vsVersion2);
      vs.getCompose().addInclude(include1);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN
      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      assertThat(expandedValueSet.getExpansion().getContains())
          .as("There should be no concepts in the expansion")
          .isEmpty();
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Disabled("Is only a template")
    @ParameterizedTest
    @DisplayName("Template")
    @ValueSource(strings = {"fhir", "bfarm.terminologien.icd10gm-2025.0.0"})
    void shouldWorkAsTemplate(String scope) {
      // GIVEN

      // Create FHIR client for the given path
      IGenericClient client = getFhirClient(scope);
      String csVersion = "2025";

      ValueSet vs = new ValueSet();
      vs.setCompose(new ValueSet.ValueSetComposeComponent());
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.setSystem(CS_SEX_REJECT_URL).setVersion(csVersion);
      vs.getCompose().addInclude(include);

      Parameters expansionParams = new Parameters();
      expansionParams.addParameter().setName("valueSet").setResource(vs);

      // WHEN

      ValueSet expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in Expansion should have the correct CodeSystem version")
          .allSatisfy(
              concept -> {
                assertThat(concept.getSystem()).isEqualTo(CS_SEX_REJECT_URL);
                assertThat(concept.getVersion()).isEqualTo(csVersion);
              });
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Language Param Checks
  // -------------------------------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Real World Inspired Language Scenario Tests")
  class ExpandLanguageScenariosTests {

    @ParameterizedTest
    @DisplayName("ValueSet expansion - correct language handling")
    @CsvSource(
        delimiter = '|',
        nullValues = {"NULL"},
        value = {
          "LS000_A | fhir | NULL | NULL | LVS00| A0 | LCS00 | A | M | Müll | NULL",
          "LS000_B | fhir | de | NULL | LVS00| A0 | LCS00 | A | M | Müll | NULL",
          "LS000_C | fhir | de-DE | NULL | LVS00| A0 | LCS00 | A | M | Müll | NULL",
          "LS000_D | fhir | de-AT | NULL | LVS00| A0 | LCS00 | A | M | Müll | NULL",
          "LS000_E | fhir | en | NULL | LVS00| A0 | LCS00 | A | M | NULL | NULL",
          "LS000_F | fhir | en-GB | NULL | LVS00| A0 | LCS00 | A | M | NULL | NULL",
          "LS001_A | fhir | NULL | NULL | LVS00| A1 | LCS00 | A | M | Müll | NULL",
          "LS002_A | fhir | NULL | NULL | LVS00| A2 | LCS00 | A | M | Müll | NULL",
          "LS003_A | fhir | NULL | NULL | LVS00| B0 | LCS00 | B | M | Müll | NULL",
          "LS004_A | fhir | NULL | NULL | LVS00| B1 | LCS00 | B | M | Müll | NULL",
          "LS005_A | fhir | NULL | NULL | LVS00| B2 | LCS00 | B | M | Müll | NULL",
          "LS006_A | fhir | NULL | NULL | LVS00| C1 | LCS00 | C | M | NULL | NULL",
          "LS007_A | fhir | NULL | NULL | LVS00| D2 | LCS00 | D | M | Müll | NULL",
          "LS007_B | fhir | de | NULL | LVS00| D2 | LCS00 | D | M | Müll | NULL",
          "LS007_C | fhir | de-DE | NULL | LVS00| D2 | LCS00 | D | M | Müll | NULL",
          "LS007_D | fhir | de-AT | NULL | LVS00| D2 | LCS00 | D | M | Mist | NULL",
          "LS007_E | fhir | en | NULL | LVS00| D2 | LCS00 | D | M | rubbish | NULL",
          "LS007_F | fhir | en-GB | NULL | LVS00| D2 | LCS00 | D | M | rubbish | NULL",
          "LS007_G | fhir | en-US | NULL | LVS00| D2 | LCS00 | D | M | trash | NULL",
          "LS007_H | fhir | es | NULL | LVS00| D2 | LCS00 | D | M | NULL | NULL",
        })
    void scenarioLxxxTest(
        String scenarioId,
        String scope,
        String displayLanguageParameter,
        String acceptLanguageHeader,
        String valueSetUrlSuffix,
        String valueSetVersion,
        String expectedCsUrlSuffix,
        String expectedCsVersion,
        String expectedCode,
        String expectedDisplayValue,
        String expectedDisplayLanguageReturnParameter) {
      // GIVEN + WHEN
      ValueSet expandedValueSet =
          createExpansion(
              scope,
              displayLanguageParameter,
              acceptLanguageHeader,
              valueSetUrlSuffix,
              valueSetVersion);

      // THEN
      CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);

      // check displayLanguageReturnParameter
      if (expectedDisplayLanguageReturnParameter != null) {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'displayLanguage' parameter has correct value")
            .filteredOn(parameter -> parameter.getName().equals(DISPLAY_LANGUAGE))
            .isNotEmpty()
            .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueCodeType)
            .asString()
            .isEqualTo(expectedDisplayLanguageReturnParameter);
      }

      // make sure we get all expanded concepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in expansion should have the expected system, version and code")
          .hasSize(1)
          .extracting("system", "version", "code", "display")
          .contains(
              tuple(
                  DEFAULT_CS_URL_PREFIX + expectedCsUrlSuffix,
                  expectedCsVersion,
                  expectedCode,
                  expectedDisplayValue));
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Helper Methods
  // -------------------------------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareExpansionParams(
      String referencedCsUrl,
      String referencedCsVersionInVS,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion) {
    Parameters expansionParams = new Parameters();
    expansionParams
        .addParameter()
        .setName("valueSet")
        .setResource(
            ValueSetGenerator.generateAllConceptsValueSet(
                referencedCsUrl, referencedCsVersionInVS));
    if (systemVersion != null) {
      expansionParams
          .addParameter()
          .setName(SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + systemVersion));
    }
    if (checkSystemVersion != null) {
      expansionParams
          .addParameter()
          .setName(CHECK_SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + checkSystemVersion));
    }
    if (forceSystemVersion != null) {
      expansionParams
          .addParameter()
          .setName(FORCE_SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + forceSystemVersion));
    }

    return expansionParams;
  }

  private ValueSet createExpansion(
      String scope,
      String displayLanguage,
      String acceptLanguageHeader,
      String valueSetUrlSuffix,
      String valueSetVersion) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);
    Parameters expansionParams =
        createExpansionParameters(displayLanguage, valueSetUrlSuffix, valueSetVersion);
    // WHEN
    ValueSet expandedValueSet;

    if (acceptLanguageHeader != null) {
      expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .withAdditionalHeader(ACCEPT_LANGUAGE, acceptLanguageHeader)
              .returnResourceType(ValueSet.class)
              .execute();
    } else {
      expandedValueSet =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class)
              .execute();
    }
    return expandedValueSet;
  }

  private static @NonNull Parameters createExpansionParameters(
      String displayLanguage, String valueSetUrlSuffix, String valueSetVersion) {
    Parameters expansionParams = new Parameters();
    if (displayLanguage != null) {
      expansionParams
          .addParameter()
          .setName("displayLanguage")
          .setValue(new CodeType(displayLanguage));
    }
    expansionParams
        .addParameter()
        .setName("url")
        .setValue(new UriType(DEFAULT_VS_URL_PREFIX + valueSetUrlSuffix));
    expansionParams
        .addParameter()
        .setName("valueSetVersion")
        .setValue(new StringType(valueSetVersion));
    return expansionParams;
  }
}
