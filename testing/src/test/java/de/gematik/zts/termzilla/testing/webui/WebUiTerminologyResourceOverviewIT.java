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

import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonBundleAsserts;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiTerminologyResourceOverviewIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("get total count of resources in scope")
  @CsvSource({
    "fhir, CodeSystem, 26",
    "fhir, ValueSet, 26",
    "fhir, ConceptMap, 4",
    "bfarm.terminologien.test-2024.0.0, CodeSystem, 1",
    "bfarm.terminologien.test-2024.0.0, ValueSet, 0",
    "bfarm.terminologien.test-2024.0.0, ConceptMap, 0",
    "bfarm.terminologien.test-2024.0.1, CodeSystem, 1",
    "bfarm.terminologien.test-2024.0.1, ValueSet, 0",
    "bfarm.terminologien.test-2024.0.1, ConceptMap, 0",
    "bfarm.terminologien.test-2025.0.0, CodeSystem, 1",
    "bfarm.terminologien.test-2025.0.0, ValueSet, 0",
    "bfarm.terminologien.test-2025.0.0, ConceptMap, 0",
    "bfarm.terminologien.icd10gm-2025.0.0, CodeSystem, 12",
    "bfarm.terminologien.icd10gm-2025.0.0, ValueSet, 13",
    "bfarm.terminologien.icd10gm-2025.0.0, ConceptMap, 2",
    "bfarm.terminologien.icd10gm-2026.0.0, CodeSystem, 12",
    "bfarm.terminologien.icd10gm-2026.0.0, ValueSet, 13",
    "bfarm.terminologien.icd10gm-2026.0.0, ConceptMap, 2"
  })
  void testGetTotalCountOfResourcesInScope(String scope, String resourceType, int expectedTotal) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN
    // Execute search with _summary=count (no resources should be returned, only total number)
    // Using byUrl() to directly specify the search query
    Bundle searchBundle =
        client
            .search()
            .byUrl(resourceType)
            .summaryMode(SummaryEnum.COUNT)
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    CommonBundleAsserts.assertBundleHasCorrectTotalValue(searchBundle, expectedTotal, scope);
    CommonBundleAsserts.assertBundleHasNoEntries(searchBundle);
    CommonBundleAsserts.assertBundleHasValidId(searchBundle);
    CommonBundleAsserts.assertBundleHasCorrectType(searchBundle, Bundle.BundleType.SEARCHSET);
    CommonBundleAsserts.assertBundleHasSubsettedMetaTag(searchBundle);
  }

  @ParameterizedTest
  @DisplayName("Get summaries of all resources in scope (on a single page)")
  @CsvSource({
    "fhir, CodeSystem, 26",
    "fhir, ValueSet, 26",
    "fhir, ConceptMap, 4",
    "bfarm.terminologien.test-2024.0.0, CodeSystem, 1",
    "bfarm.terminologien.test-2024.0.0, ValueSet, 0",
    "bfarm.terminologien.test-2024.0.0, ConceptMap, 0",
    "bfarm.terminologien.test-2024.0.1, CodeSystem, 1",
    "bfarm.terminologien.test-2024.0.1, ValueSet, 0",
    "bfarm.terminologien.test-2024.0.1, ConceptMap, 0",
    "bfarm.terminologien.test-2025.0.0, CodeSystem, 1",
    "bfarm.terminologien.test-2025.0.0, ValueSet, 0",
    "bfarm.terminologien.test-2025.0.0, ConceptMap, 0",
    "bfarm.terminologien.icd10gm-2025.0.0, CodeSystem, 12",
    "bfarm.terminologien.icd10gm-2025.0.0, ValueSet, 13",
    "bfarm.terminologien.icd10gm-2025.0.0, ConceptMap, 2",
    "bfarm.terminologien.icd10gm-2026.0.0, CodeSystem, 12",
    "bfarm.terminologien.icd10gm-2026.0.0, ValueSet, 13",
    "bfarm.terminologien.icd10gm-2026.0.0, ConceptMap, 2"
  })
  void testGetSummariesOfAllResourcesInScopeSinglePage(
      String scope, String resourceType, int expectedTotal) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN
    // Execute search with count=100 (all resources should normally be returned in a single page)
    Bundle searchBundle =
        client
            .search()
            .byUrl(resourceType)
            .count(100) // Set high count to get all resources in a single page
            .summaryMode(SummaryEnum.TRUE)
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    CommonBundleAsserts.assertBundleHasCorrectTotalValue(searchBundle, expectedTotal, scope);
    CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(searchBundle, expectedTotal);
    CommonBundleAsserts.assertBundleHasCorrectTypesOfEntries(searchBundle, resourceType);
    CommonBundleAsserts.assertBundleHasValidId(searchBundle);
    CommonBundleAsserts.assertBundleHasCorrectType(searchBundle, Bundle.BundleType.SEARCHSET);
    CommonBundleAsserts.assertBundleHasSubsettedMetaTag(searchBundle);
    CommonBundleAsserts.assertBundleEntriesHaveSubsettedMetaTag(searchBundle);
  }

  @ParameterizedTest
  @DisplayName("Get summaries of all resources in scope (on multiple pages)")
  @CsvSource({
    "fhir, CodeSystem, 26, 1",
    "fhir, ValueSet, 26, 1",
    "fhir, ConceptMap, 4, 1",
    "bfarm.terminologien.test-2024.0.0, CodeSystem, 1, 1",
    "bfarm.terminologien.test-2024.0.0, ValueSet, 0, 1",
    "bfarm.terminologien.test-2024.0.0, ConceptMap, 0, 1",
    "bfarm.terminologien.test-2024.0.1, CodeSystem, 1, 1",
    "bfarm.terminologien.test-2024.0.1, ValueSet, 0, 1",
    "bfarm.terminologien.test-2024.0.1, ConceptMap, 0, 1",
    "bfarm.terminologien.test-2025.0.0, CodeSystem, 1, 1",
    "bfarm.terminologien.test-2025.0.0, ValueSet, 0, 1",
    "bfarm.terminologien.test-2025.0.0, ConceptMap, 0, 1",
    "bfarm.terminologien.icd10gm-2025.0.0, CodeSystem, 12, 2",
    "bfarm.terminologien.icd10gm-2025.0.0, ValueSet, 13, 2",
    "bfarm.terminologien.icd10gm-2025.0.0, ConceptMap, 2, 2",
    "bfarm.terminologien.icd10gm-2026.0.0, CodeSystem, 12, 10",
    "bfarm.terminologien.icd10gm-2026.0.0, ValueSet, 13, 10",
    "bfarm.terminologien.icd10gm-2026.0.0, ConceptMap, 2, 10"
  })
  void testGetSummariesOfAllResourcesInScopeMultiplePages(
      String scope, String resourceType, int expectedTotal, int pageSize) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN + THEN
    // Execute search with count=0 (no resources should be returned, only total number)
    // Using byUrl() to directly specify the search query
    Bundle searchBundlePage =
        client
            .search()
            .byUrl(resourceType)
            .count(pageSize)
            .summaryMode(SummaryEnum.TRUE)
            .returnBundle(Bundle.class)
            .execute();

    int entryTotalCount = 0;
    int pageTotalCount = 0;
    int expectedEntriesOnPage = 0;

    while (searchBundlePage != null) {

      entryTotalCount = entryTotalCount + searchBundlePage.getEntry().size();
      pageTotalCount += 1;
      expectedEntriesOnPage = Math.min(pageSize, expectedTotal - (pageSize * (pageTotalCount - 1)));

      CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(
          searchBundlePage, expectedEntriesOnPage);
      CommonBundleAsserts.assertBundleHasCorrectTypesOfEntries(searchBundlePage, resourceType);
      CommonBundleAsserts.assertBundleHasValidId(searchBundlePage);
      CommonBundleAsserts.assertBundleHasCorrectType(searchBundlePage, Bundle.BundleType.SEARCHSET);
      CommonBundleAsserts.assertBundleHasSubsettedMetaTag(searchBundlePage);
      CommonBundleAsserts.assertBundleEntriesHaveSubsettedMetaTag(searchBundlePage);

      // Load next page if available
      if (searchBundlePage.getLink(Bundle.LINK_NEXT) != null) {
        searchBundlePage =
            client
                .search()
                .byUrl(searchBundlePage.getLink(Bundle.LINK_NEXT).getUrl())
                .summaryMode(SummaryEnum.TRUE)
                .returnBundle(Bundle.class)
                .execute();
        // TODO: Bug bei HAPI FHIR melden?
        // summary mode scheint hier nicht berücksichtigt zu werden:
        // client.loadPage().next(searchBundlePage).summaryMode(SummaryEnum.TRUE).execute()
      } else {
        break;
      }
    }

    assertEquals(
        expectedTotal,
        entryTotalCount,
        "Total number of Resource entries does not match expected total.");
    if (expectedTotal == 0) {
      assertEquals(1, pageTotalCount, "Total number of pages does not match expected page count.");
    } else {
      assertEquals(
          (int) Math.ceil((double) expectedTotal / pageSize),
          pageTotalCount,
          "Total number of pages does not match expected page count.");
    }
  }

  @ParameterizedTest
  @DisplayName(
      "Search for all resources in a given 'scope', only return specific elements in results (use multiple result pages if necessary)")
  @CsvSource(
      delimiter = '|',
      value = {
        "fhir| CodeSystem| url,version,name| 26| 1",
        "fhir| ValueSet| url,version,name| 26| 1",
        "fhir| ConceptMap| url,version,name| 4| 1",
        "bfarm.terminologien.test-2024.0.0| CodeSystem| url,version| 1| 1",
        "bfarm.terminologien.test-2024.0.0| ValueSet| url| 0| 1",
        "bfarm.terminologien.test-2024.0.0| ConceptMap| url| 0| 1",
        "bfarm.terminologien.test-2024.0.1| CodeSystem| url| 1| 1",
        "bfarm.terminologien.test-2024.0.1| ValueSet| url| 0| 1",
        "bfarm.terminologien.test-2024.0.1| ConceptMap| url| 0| 1",
        "bfarm.terminologien.test-2025.0.0| CodeSystem| url| 1| 1",
        "bfarm.terminologien.test-2025.0.0| ValueSet| url| 0| 1",
        "bfarm.terminologien.test-2025.0.0| ConceptMap| url| 0| 1",
        "bfarm.terminologien.icd10gm-2025.0.0| CodeSystem| version,name| 12| 2",
        "bfarm.terminologien.icd10gm-2025.0.0| ValueSet| url| 13| 2",
        "bfarm.terminologien.icd10gm-2025.0.0| ConceptMap| url| 2| 2",
        "bfarm.terminologien.icd10gm-2026.0.0| CodeSystem| url| 12| 10",
        "bfarm.terminologien.icd10gm-2026.0.0| ValueSet| url| 13| 10",
        "bfarm.terminologien.icd10gm-2026.0.0| ConceptMap| url| 2| 10"
      })
  void testSearchResourcesInGivenScopeNoFilterOnlyReturnSpecificElementsResultsOnMultiplePages(
      String scope, String resourceType, String elements, int expectedTotal, int pageSize) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN + THEN
    // Execute search with count=0 (no resources should be returned, only total number)
    // Using byUrl() to directly specify the search query
    Bundle searchBundlePage =
        client
            .search()
            .byUrl(resourceType)
            .count(pageSize)
            .elementsSubset(elements)
            .returnBundle(Bundle.class)
            .execute();

    int entryTotalCount = 0;
    int pageTotalCount = 0;
    int expectedEntriesOnPage = 0;

    while (searchBundlePage != null) {

      entryTotalCount = entryTotalCount + searchBundlePage.getEntry().size();
      pageTotalCount += 1;
      expectedEntriesOnPage = Math.min(pageSize, expectedTotal - (pageSize * (pageTotalCount - 1)));

      CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(
          searchBundlePage, expectedEntriesOnPage);
      CommonBundleAsserts.assertBundleHasCorrectTypesOfEntries(searchBundlePage, resourceType);
      CommonBundleAsserts.assertBundleHasValidId(searchBundlePage);
      CommonBundleAsserts.assertBundleHasCorrectType(searchBundlePage, Bundle.BundleType.SEARCHSET);
      // assertBundleHasSubsettedMetaTag(searchBundlePage); --> Not set when using _elements
      CommonBundleAsserts.assertBundleEntriesHaveSubsettedMetaTag(searchBundlePage);
      CommonBundleAsserts.assertBundleEntriesHaveOnlyRequestedElements(
          searchBundlePage, List.of(elements.split(",")));

      // Load next page if available
      if (searchBundlePage.getLink(Bundle.LINK_NEXT) != null) {
        searchBundlePage = client.loadPage().next(searchBundlePage).execute();
      } else {
        break;
      }
    }

    assertEquals(
        expectedTotal,
        entryTotalCount,
        "Total number of Resource entries does not match expected total.");
    if (expectedTotal == 0) {
      assertEquals(1, pageTotalCount, "Total number of pages does not match expected page count.");
    } else {
      assertEquals(
          (int) Math.ceil((double) expectedTotal / pageSize),
          pageTotalCount,
          "Total number of pages does not match expected page count.");
    }
  }

  @ParameterizedTest
  @DisplayName(
      "Search for TerminologyResource in a given 'scope' by 'meta.source', only return specific elements in results (use multiple result pages if necessary)")
  @CsvSource(
      delimiter = '|',
      emptyValue = "",
      nullValues = {"NULL"},
      value = {
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2025.0.0| CodeSystem| url,version| 12| 10",
        "fhir| :below| https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm| CodeSystem| url,version,name,title| 24| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2025.0.0| ValueSet| url| 13| 10",
        "fhir| :below| https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm| ValueSet| url| 26| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2025.0.0| ConceptMap| url| 2| 10",
        "fhir| :below| https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm| ConceptMap| url| 4| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2026.0.0| CodeSystem| url| 12| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2026.0.0| ValueSet| url| 13| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2026.0.0| ConceptMap| url| 2| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.icd10gm/2026.0.0| CodeSystem| url| 12| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.test/2024.0.0| CodeSystem| url| 0| 10",
        // CS not in scope (overwritten by same version in bugfix package)
        "bfarm.terminologien.test-2024.0.0| | https://terminologien.bfarm.de/packages/bfarm.terminologien.test/2024.0.0| CodeSystem| url| 1| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.test/2024.0.1| CodeSystem| url| 1| 10",
        "fhir| | https://terminologien.bfarm.de/packages/bfarm.terminologien.test/2025.0.0| CodeSystem| url| 1| 10",
      })
  void
      testSearchTerminologyResourceInGivenScopeBySourceOnlyReturnSpecificElementsResultsOnMultiplePages(
          String scope,
          String modifier,
          String sourceUrl,
          String resourceType,
          String elements,
          int expectedTotal,
          int pageSize) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // WHEN + THEN
    // Execute search with count=0 (no resources should be returned, only total number)
    // Using byUrl() to directly specify the search query
    Bundle searchBundlePage =
        client
            .search()
            .byUrl(resourceType + "?_source" + (modifier == null ? "" : modifier) + "=" + sourceUrl)
            .count(pageSize)
            .elementsSubset(elements)
            .returnBundle(Bundle.class)
            .execute();

    int entryTotalCount = 0;
    int pageTotalCount = 0;
    int expectedEntriesOnPage;

    while (searchBundlePage != null) {

      entryTotalCount = entryTotalCount + searchBundlePage.getEntry().size();
      pageTotalCount += 1;
      expectedEntriesOnPage = Math.min(pageSize, expectedTotal - (pageSize * (pageTotalCount - 1)));

      CommonBundleAsserts.assertBundleHasCorrectNumberOfEntries(
          searchBundlePage, expectedEntriesOnPage);
      CommonBundleAsserts.assertBundleHasCorrectTypesOfEntries(searchBundlePage, resourceType);
      CommonBundleAsserts.assertBundleHasValidId(searchBundlePage);
      CommonBundleAsserts.assertBundleHasCorrectType(searchBundlePage, Bundle.BundleType.SEARCHSET);
      // assertBundleHasSubsettedMetaTag(searchBundlePage); --> Not set for bundle when using
      // _elements
      CommonBundleAsserts.assertBundleEntriesHaveSubsettedMetaTag(searchBundlePage);
      CommonBundleAsserts.assertBundleEntriesHaveOnlyRequestedElements(
          searchBundlePage, List.of(elements.split(",")));

      // Load next page if available
      if (searchBundlePage.getLink(Bundle.LINK_NEXT) != null) {
        searchBundlePage = client.loadPage().next(searchBundlePage).execute();
      } else {
        break;
      }
    }

    assertEquals(
        expectedTotal,
        entryTotalCount,
        "Total number of Resource entries does not match expected total.");
    if (expectedTotal == 0) {
      assertEquals(1, pageTotalCount, "Total number of pages does not match expected page count.");
    } else {
      assertEquals(
          (int) Math.ceil((double) expectedTotal / pageSize),
          pageTotalCount,
          "Total number of pages does not match expected page count.");
    }
  }
}
