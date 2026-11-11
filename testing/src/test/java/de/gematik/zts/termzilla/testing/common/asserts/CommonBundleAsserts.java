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
package de.gematik.zts.termzilla.testing.common.asserts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;

/**
 * Common assertion utilities for FHIR {@link Bundle} validation in integration tests.
 *
 * <p>Provides reusable assertion methods for verifying Bundle structure, content, and FHIR element
 * subsetting behavior.
 */
public class CommonBundleAsserts {

  /**
   * Asserts that the Bundle has the expected total count.
   *
   * @param bundle the Bundle to check
   * @param expectedTotal the expected total count
   * @param scope additional context for the error message
   */
  public static void assertBundleHasCorrectTotalValue(
      Bundle bundle, int expectedTotal, String scope) {
    assertEquals(
        expectedTotal, bundle.getTotal(), "CodeSystem count not correct for path " + scope);
  }

  /**
   * Asserts that the Bundle has a valid UUID as its ID element.
   *
   * @param bundle the Bundle to check
   */
  public static void assertBundleHasValidId(Bundle bundle) {
    assertTrue(bundle.hasId(), "Search Bundle should have an id element");
    String bundleId = bundle.getIdPart();
    assertNotNull(bundleId, "Search Bundle id should not be null");
    assertFalse(bundleId.isEmpty(), "Search Bundle id should not be empty");
    assertDoesNotThrow(() -> UUID.fromString(bundleId), "Search Bundle id should be a valid UUID");
  }

  /**
   * Asserts that the Bundle contains no entries.
   *
   * @param bundle the Bundle to check
   */
  public static void assertBundleHasNoEntries(Bundle bundle) {
    assertFalse(
        bundle.hasEntry(),
        "No CodeSystem resources should be returned as entries when '_count=0' is used");
  }

  /**
   * Asserts that the Bundle contains the correct number of entries.
   *
   * @param bundle the Bundle to check
   */
  public static void assertBundleHasCorrectNumberOfEntries(Bundle bundle, int expectedEntries) {
    assertEquals(
        expectedEntries,
        bundle.getEntry().size(),
        "Number of CodeSystem entries in Bundle is not correct");
  }

  /**
   * Asserts that all entries in the Bundle are of the expected resource type.
   *
   * @param searchBundle the Bundle to check
   * @param resourceClass the expected resource class (must extend {@link Resource})
   * @param <T> the type of the FHIR resource
   */
  public static <T extends Resource> void assertBundleHasCorrectTypesOfEntries(
      Bundle searchBundle, String resourceClass) {
    for (Bundle.BundleEntryComponent entry : searchBundle.getEntry()) {
      Resource resource = entry.getResource();
      assertNotNull(resource, "Bundle entry resource should not be null");
      assertEquals(
          resourceClass,
          resource.getClass().getSimpleName(),
          "Bundle entry resource should be of type " + resourceClass);
    }
  }

  /**
   * Asserts that the Bundle has the expected type.
   *
   * @param searchBundle the Bundle to check
   * @param bundleType the expected {@link Bundle.BundleType}
   */
  public static void assertBundleHasCorrectType(Bundle searchBundle, Bundle.BundleType bundleType) {
    assertEquals(
        bundleType,
        searchBundle.getType(),
        "Bundle type is not correct, expected: " + bundleType.toCode());
  }

  /**
   * Asserts that the Bundle has the SUBSETTED meta tag.
   *
   * <p>Checks for a {@link Coding} with system {@code
   * http://terminology.hl7.org/CodeSystem/v3-ObservationValue} and code {@code SUBSETTED}.
   *
   * @param searchBundle the Bundle to check
   */
  public static void assertBundleHasSubsettedMetaTag(Bundle searchBundle) {
    assertTrue(searchBundle.hasMeta(), "Bundle should have meta element");

    List<Coding> tagList = searchBundle.getMeta().getTag();
    assertNotNull(tagList, "Bundle meta should have tag list");
    assertFalse(tagList.isEmpty(), "Bundle meta tag list should not be empty");

    // Check if SUBSETTED tag exists using Stream API
    boolean hasSubsettedTag =
        tagList.stream()
            .anyMatch(
                coding ->
                    "http://terminology.hl7.org/CodeSystem/v3-ObservationValue"
                            .equals(coding.getSystem())
                        && "SUBSETTED".equals(coding.getCode()));

    assertTrue(
        hasSubsettedTag,
        "Bundle meta should contain SUBSETTED tag "
            + "(system: http://terminology.hl7.org/CodeSystem/v3-ObservationValue, code: SUBSETTED)");
  }

  public static void assertBundleEntriesHaveSubsettedMetaTag(Bundle searchBundle) {
    for (Bundle.BundleEntryComponent entry : searchBundle.getEntry()) {
      Resource resource = entry.getResource();
      assertNotNull(resource, "Bundle entry resource should not be null");
      assertTrue(resource.hasMeta(), "Resource should have meta element");

      List<Coding> tagList = resource.getMeta().getTag();
      assertNotNull(tagList, "Resource meta should have tag list");
      assertFalse(tagList.isEmpty(), "Resource meta tag list should not be empty");

      // Check if SUBSETTED tag exists using Stream API
      boolean hasSubsettedTag =
          tagList.stream()
              .anyMatch(
                  coding ->
                      "http://terminology.hl7.org/CodeSystem/v3-ObservationValue"
                              .equals(coding.getSystem())
                          && "SUBSETTED".equals(coding.getCode()));

      assertTrue(
          hasSubsettedTag,
          "Resource meta should contain SUBSETTED tag "
              + "(system: http://terminology.hl7.org/CodeSystem/v3-ObservationValue, code: SUBSETTED)");
    }
  }

  /**
   * Asserts that all {@link MetadataResource} entries in the Bundle contain only the requested
   * elements.
   *
   * <p>This assertion validates FHIR's {@code _elements} parameter behavior by checking that:
   *
   * <ul>
   *   <li>Resources contain all requested elements
   *   <li>Resources do not contain non-requested elements
   * </ul>
   *
   * <p><b>Supported elements:</b> {@code url}, {@code version}, {@code name}, {@code status},
   * {@code title}, {@code description}
   *
   * @param searchBundle the Bundle containing resources to validate
   * @param elements list of element names that should be present in each resource
   * @throws AssertionError if any resource contains unexpected elements or lacks requested elements
   * @throws IllegalArgumentException if any requested element is not in the supported list
   */
  public static void assertBundleEntriesHaveOnlyRequestedElements(
      Bundle searchBundle, List<String> elements) {

    // Only a small set of elements is currently supported for this test scenario
    List<String> allowedElementsInTest =
        List.of("url", "version", "name", "status", "title", "description");

    // assert that only allowed elements are requested
    for (String element : elements) {
      assertTrue(
          allowedElementsInTest.contains(element),
          "Element '" + element + "' is not supported in this test scenario");
    }

    for (Bundle.BundleEntryComponent entry : searchBundle.getEntry()) {
      MetadataResource resource = (MetadataResource) entry.getResource();

      if (elements.contains("url")) {
        assertTrue(resource.hasUrl(), "Resource should have 'url' element");
      } else {
        assertFalse(resource.hasUrl(), "Resource should NOT have 'url' element");
      }

      if (elements.contains("version")) {
        assertTrue(resource.hasVersion(), "Resource should have 'version' element");
      } else {
        assertFalse(resource.hasVersion(), "Resource should NOT have 'version' element");
      }

      if (elements.contains("name")) {
        assertTrue(resource.hasName(), "Resource should have 'name' element");
      } else {
        assertFalse(resource.hasName(), "Resource should NOT have 'name' element");
      }

      if (elements.contains("status")) {
        assertTrue(resource.hasStatus(), "Resource should have 'status' element");
      } else {
        assertFalse(resource.hasStatus(), "Resource should NOT have 'status' element");
      }

      if (elements.contains("title")) {
        assertTrue(resource.hasTitle(), "Resource should have 'title' element");
      } else {
        assertFalse(resource.hasTitle(), "Resource should NOT have 'title' element");
      }

      if (elements.contains("description")) {
        assertTrue(resource.hasDescription(), "Resource should have 'description' element");
      } else {
        assertFalse(resource.hasDescription(), "Resource should NOT have 'description' element");
      }
    }
  }
}
