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
package de.gematik.zts.termzilla.server.service.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Custom assertion helpers for terminology service testing.
 *
 * <p>Provides domain-specific assertions that make tests more readable and maintainable.
 */
public class AssertionHelpers {

  // ==================================================================================================
  // ValidateCodeResult Assertions
  // ==================================================================================================

  /**
   * Asserts that a validate-code result is valid.
   *
   * @param result the validation result to check
   * @param message optional assertion message
   */
  public static void assertValidateCodeResultValid(ValidateCodeResult result, String message) {
    assertNotNull(result, "ValidateCodeResult should not be null");
    assertTrue(result.isResult(), message != null ? message : "Code should be valid");
  }

  /**
   * Asserts that a validate-code result is valid.
   *
   * @param result the validation result to check
   */
  public static void assertValidateCodeResultValid(ValidateCodeResult result) {
    assertValidateCodeResultValid(result, null);
  }

  /**
   * Asserts that a validate-code result is invalid.
   *
   * @param result the validation result to check
   * @param message optional assertion message
   */
  public static void assertValidateCodeResultInvalid(ValidateCodeResult result, String message) {
    assertNotNull(result, "ValidateCodeResult should not be null");
    assertFalse(result.isResult(), message != null ? message : "Code should be invalid");
  }

  /**
   * Asserts that a validate-code result is invalid.
   *
   * @param result the validation result to check
   */
  public static void assertValidateCodeResultInvalid(ValidateCodeResult result) {
    assertValidateCodeResultInvalid(result, null);
  }

  /**
   * Asserts that a validate-code result has the expected display.
   *
   * @param result the validation result
   * @param expectedDisplay the expected display value
   */
  public static void assertValidateCodeResultDisplay(
      ValidateCodeResult result, String expectedDisplay) {
    assertNotNull(result, "ValidateCodeResult should not be null");
    assertEquals(expectedDisplay, result.getDisplay(), "Display value should match expected value");
  }

  /**
   * Asserts complete validate-code result including validity and display.
   *
   * @param result the validation result
   * @param expectedValid whether the code should be valid
   * @param expectedDisplay the expected display value (can be null)
   */
  public static void assertValidateCodeResult(
      ValidateCodeResult result, boolean expectedValid, String expectedDisplay) {
    assertNotNull(result, "ValidateCodeResult should not be null");
    assertEquals(expectedValid, result.isResult(), "Validation result should match expected");

    if (expectedDisplay != null) {
      assertValidateCodeResultDisplay(result, expectedDisplay);
    }
  }

  // ==================================================================================================
  // CsLookupResult Assertions
  // ==================================================================================================

  /**
   * Asserts that a lookup result contains a display value.
   *
   * @param result the lookup result
   */
  public static void assertLookupResultHasDisplay(CsLookupResult result) {
    assertNotNull(result, "CsLookupResult should not be null");
    assertNotNull(result.getCodeDisplay(), "Lookup result should have a display value");
    assertFalse(result.getCodeDisplay().isEmpty(), "Display value should not be empty");
  }

  /**
   * Asserts that a lookup result has the expected display value.
   *
   * @param result the lookup result
   * @param expectedDisplay the expected display
   */
  public static void assertLookupResultDisplay(CsLookupResult result, String expectedDisplay) {
    assertNotNull(result, "CsLookupResult should not be null");
    assertEquals(expectedDisplay, result.getCodeDisplay(), "Display should match expected value");
  }

  /**
   * Asserts that a lookup result has the expected code.
   *
   * @param result the lookup result
   * @param expectedCode the expected code
   */
  public static void assertLookupResultCode(CsLookupResult result, String expectedCode) {
    assertNotNull(result, "CsLookupResult should not be null");
    assertEquals(expectedCode, result.getSearchedForCode(), "Code should match expected value");
  }

  // ==================================================================================================
  // CodeSystem Assertions
  // ==================================================================================================

  /**
   * Asserts that a CodeSystem has the expected URL.
   *
   * @param codeSystem the CodeSystem resource
   * @param expectedUrl the expected canonical URL
   */
  public static void assertCodeSystemUrl(CodeSystem codeSystem, String expectedUrl) {
    assertNotNull(codeSystem, "CodeSystem should not be null");
    assertEquals(expectedUrl, codeSystem.getUrl(), "CodeSystem URL should match expected");
  }

  /**
   * Asserts that a CodeSystem contains a specific concept.
   *
   * @param codeSystem the CodeSystem resource
   * @param code the concept code to find
   */
  public static void assertCodeSystemContainsConcept(CodeSystem codeSystem, String code) {
    assertNotNull(codeSystem, "CodeSystem should not be null");
    boolean found =
        codeSystem.getConcept().stream().anyMatch(concept -> concept.getCode().equals(code));
    assertTrue(found, "CodeSystem should contain concept with code: " + code);
  }

  /**
   * Asserts that a CodeSystem has a specific number of concepts.
   *
   * @param codeSystem the CodeSystem resource
   * @param expectedCount the expected number of concepts
   */
  public static void assertCodeSystemConceptCount(CodeSystem codeSystem, int expectedCount) {
    assertNotNull(codeSystem, "CodeSystem should not be null");
    assertEquals(
        expectedCount,
        codeSystem.getConcept().size(),
        "CodeSystem should have expected number of concepts");
  }

  // ==================================================================================================
  // ValueSet Assertions
  // ==================================================================================================

  /**
   * Asserts that a ValueSet has the expected URL.
   *
   * @param valueSet the ValueSet resource
   * @param expectedUrl the expected canonical URL
   */
  public static void assertValueSetUrl(ValueSet valueSet, String expectedUrl) {
    assertNotNull(valueSet, "ValueSet should not be null");
    assertEquals(expectedUrl, valueSet.getUrl(), "ValueSet URL should match expected");
  }

  /**
   * Asserts that a ValueSet has an expansion.
   *
   * @param valueSet the ValueSet resource
   */
  public static void assertValueSetHasExpansion(ValueSet valueSet) {
    assertNotNull(valueSet, "ValueSet should not be null");
    assertNotNull(valueSet.getExpansion(), "ValueSet should have an expansion");
  }

  /**
   * Asserts that a ValueSet expansion contains a specific number of concepts.
   *
   * @param valueSet the ValueSet resource
   * @param expectedCount the expected number of concepts in expansion
   */
  public static void assertValueSetExpansionSize(ValueSet valueSet, int expectedCount) {
    assertValueSetHasExpansion(valueSet);
    assertEquals(
        expectedCount,
        valueSet.getExpansion().getContains().size(),
        "ValueSet expansion should have expected size");
  }

  /**
   * Asserts that a ValueSet expansion contains a specific code.
   *
   * @param valueSet the ValueSet resource
   * @param code the code to find
   */
  public static void assertValueSetExpansionContainsCode(ValueSet valueSet, String code) {
    assertValueSetHasExpansion(valueSet);
    boolean found =
        valueSet.getExpansion().getContains().stream()
            .anyMatch(contains -> contains.getCode().equals(code));
    assertTrue(found, "ValueSet expansion should contain code: " + code);
  }

  // ==================================================================================================
  // Search Result Assertions
  // ==================================================================================================

  /**
   * Asserts that a search result list has the expected size.
   *
   * @param results the search result list
   * @param expectedSize the expected size
   */
  public static void assertSearchResultSize(List<IdType> results, int expectedSize) {
    assertNotNull(results, "Search results should not be null");
    assertEquals(expectedSize, results.size(), "Search results should have expected size");
  }

  /**
   * Asserts that search results are not empty.
   *
   * @param results the search result list
   */
  public static void assertSearchResultsNotEmpty(List<IdType> results) {
    assertNotNull(results, "Search results should not be null");
    assertFalse(results.isEmpty(), "Search results should not be empty");
  }

  /**
   * Asserts that search results contain a specific ID.
   *
   * @param results the search result list
   * @param expectedId the expected ID
   */
  public static void assertSearchResultsContainId(List<IdType> results, String expectedId) {
    assertSearchResultsNotEmpty(results);
    boolean found = results.stream().anyMatch(id -> id.getIdPart().equals(expectedId));
    assertTrue(found, "Search results should contain ID: " + expectedId);
  }

  /**
   * Asserts that all search results have the expected resource type.
   *
   * @param results the search result list
   * @param expectedResourceType the expected resource type
   */
  public static void assertSearchResultsResourceType(
      List<IdType> results, String expectedResourceType) {
    assertSearchResultsNotEmpty(results);
    boolean allMatch =
        results.stream().allMatch(id -> id.getResourceType().equals(expectedResourceType));
    assertTrue(allMatch, "All search results should have resource type: " + expectedResourceType);
  }
}
