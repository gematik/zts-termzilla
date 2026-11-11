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
package de.gematik.zts.termzilla.testing.webui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonBundleAsserts;
import de.gematik.zts.termzilla.testing.common.asserts.CommonMetadateResourceAsserts;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MetadataResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiTerminologyResourceCommonDetailsIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("get resources in scope by url and version with _summary=true")
  @CsvSource({
    "fhir, CodeSystem, http://fhir.de/CodeSystem/bfarm/icd-10-gm, 2025",
    "fhir, ValueSet, http://fhir.de/ValueSet/bfarm/icd-10-gm, 2025",
    "fhir, ConceptMap, https://terminologien.bfarm.de/fhir/ConceptMap/icd10gm-ueberleitung-alt-neu, 2025",
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
        (MetadataResource) searchBundle.getEntry().getFirst().getResource(), canonicalUrl, version);
  }

  @ParameterizedTest
  @DisplayName("get resources in scope by url and version with _summary=true")
  @CsvSource(
      delimiter = '|',
      value = {
        "fhir| CodeSystem| url,version| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025",
        "fhir| CodeSystem| url,version,description| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025",
        "fhir| ValueSet| url,version,name| http://fhir.de/ValueSet/bfarm/icd-10-gm| 2025",
        "fhir| ConceptMap| url,version| https://terminologien.bfarm.de/fhir/ConceptMap/icd10gm-ueberleitung-alt-neu| 2025",
      })
  void testGetResourceElementsByUrlAndVersionInScope(
      String scope, String resourceType, String elements, String canonicalUrl, String version) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN
    Bundle searchBundle =
        client
            .search()
            .byUrl(resourceType + "?url=" + canonicalUrl + "&version=" + version)
            .elementsSubset(elements.split(","))
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    CommonBundleAsserts.assertBundleHasValidId(searchBundle);
    CommonBundleAsserts.assertBundleHasCorrectType(searchBundle, Bundle.BundleType.SEARCHSET);
    CommonBundleAsserts.assertBundleHasCorrectTotalValue(searchBundle, 1, scope);
    CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(searchBundle, 1);
    CommonBundleAsserts.assertBundleHasCorrectTypesOfEntries(searchBundle, resourceType);
    CommonBundleAsserts.assertBundleEntriesHaveOnlyRequestedElements(
        searchBundle, List.of(elements.split(",")));
    CommonMetadateResourceAsserts.asserMetadataResourceHasCorrectUrlAndVersion(
        (MetadataResource) searchBundle.getEntry().getFirst().getResource(), canonicalUrl, version);
  }

  @ParameterizedTest
  @DisplayName("read resources in scope")
  @CsvSource(
      delimiter = '|',
      value = {
        "fhir| CodeSystem | http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025",
        "fhir| ValueSet | http://fhir.de/ValueSet/bfarm/icd-10-gm| 2025",
        "fhir| ConceptMap| https://terminologien.bfarm.de/fhir/ConceptMap/icd10gm-ueberleitung-alt-neu | 2026",
        "bfarm.terminologien.icd10gm-2025.0.0| CodeSystem|  http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025",
        "bfarm.terminologien.icd10gm-2026.0.0| ValueSet|  http://fhir.de/ValueSet/bfarm/icd-10-gm| 2026",
        "bfarm.terminologien.icd10gm-2026.0.0| ConceptMap| https://terminologien.bfarm.de/fhir/ConceptMap/icd10gm-ueberleitung-alt-neu | 2026",
      })
  void testReadResourceByIdInScope(
      String scope, String resourceType, String canonicalUrl, String version) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    Bundle searchBundle =
        client
            .search()
            .byUrl(resourceType + "?url=" + canonicalUrl + "&version=" + version)
            .elementsSubset("url", "version")
            .returnBundle(Bundle.class)
            .execute();

    String resourceId =
        searchBundle.getEntry().getFirst().getResource().getIdElement().toVersionless().getIdPart();

    // WHEN + THEN
    IBaseResource resource = client.read().resource(resourceType).withId(resourceId).execute();
    assertNotNull(resource, "Resource should exist in scope " + scope);
    assertEquals(
        resource.getIdElement().getIdPart(), resourceId, "Resource ID should match requested ID");
  }

  @ParameterizedTest
  @DisplayName("read non existent resources in scope")
  @CsvSource(
      delimiter = '|',
      value = {
        "fhir| CodeSystem | aaaa",
        "fhir| ValueSet | aaaa",
        "fhir| ConceptMap| haaaa",
        "bfarm.terminologien.icd10gm-2025.0.0| CodeSystem|  aaaa",
        "bfarm.terminologien.icd10gm-2026.0.0| ValueSet|  aaaaa",
        "bfarm.terminologien.icd10gm-2026.0.0| ConceptMap| aaaaa",
      })
  void testReadResourceByIdInScopeDoesntExist(
      String scope, String resourceType, String resourceId) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN + THEN
    var operation = client.read().resource(resourceType).withId(resourceId);
    assertThrows(
        ResourceNotFoundException.class,
        operation::execute,
        "Expected ResourceNotFoundException when resource should not exist");
  }
}
