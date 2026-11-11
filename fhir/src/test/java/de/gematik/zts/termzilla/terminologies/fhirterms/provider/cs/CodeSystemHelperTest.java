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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CodeSystemHelper}.
 *
 * <p>Covers the map-based {@code findCodeInCodeSystem(CodeSystem, String)} overload, including
 * case-sensitivity handling, deep hierarchy traversal, exception message formatting, and the
 * caching behaviour via {@link ResourceUserData#USERDATA_CONCEPTS_AS_MAP}.
 */
@DisplayName("CodeSystemHelper Tests")
class CodeSystemHelperTest {

  private static final String CS_URL = "http://example.org/CodeSystem/test";
  private static final String CS_VERSION = "1.0.0";

  // ---------------------------------------------------------------------------
  // findCodeInCodeSystem(CodeSystem, String)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findCodeInCodeSystem(CodeSystem, String)")
  class FindCodeInCodeSystemTests {

    @Test
    @DisplayName("should return concept when code is found at top level")
    void shouldReturnConceptWhenCodeFoundAtTopLevel() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));
      codeSystem.addConcept(createConcept("B", "Concept B"));

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "A");

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getCode()).isEqualTo("A");
      assertThat(result.getDisplay()).isEqualTo("Concept A");
    }

    @Test
    @DisplayName("should return concept when code is found in a child concept")
    void shouldReturnConceptWhenCodeFoundInChild() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      CodeSystem.ConceptDefinitionComponent parent = createConcept("PARENT", "Parent");
      parent.addConcept(createConcept("CHILD", "Child"));
      codeSystem.addConcept(parent);

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "CHILD");

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getCode()).isEqualTo("CHILD");
    }

    @Test
    @DisplayName("should return concept when code is found deep in a multi-level hierarchy")
    void shouldReturnConceptWhenCodeFoundDeepInHierarchy() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      CodeSystem.ConceptDefinitionComponent level1 = createConcept("LEVEL1", "Level 1");
      CodeSystem.ConceptDefinitionComponent level2 = createConcept("LEVEL2", "Level 2");
      CodeSystem.ConceptDefinitionComponent level3 = createConcept("LEVEL3", "Level 3");
      level2.addConcept(level3);
      level1.addConcept(level2);
      codeSystem.addConcept(level1);

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "LEVEL3");

      // THEN
      assertThat(result).isNotNull();
      assertThat(result.getCode()).isEqualTo("LEVEL3");
    }

    @Test
    @DisplayName("should throw UnknownConceptException when code is not found")
    void shouldThrowUnknownConceptExceptionWhenCodeNotFound() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("EXISTING", "Existing Concept"));

      // WHEN / THEN
      assertThatThrownBy(() -> CodeSystemHelper.findCodeInCodeSystem(codeSystem, "UNKNOWN"))
          .isInstanceOf(UnknownConceptException.class);
    }

    @Test
    @DisplayName("should include code, URL, and version in exception message")
    void shouldIncludeCodeUrlAndVersionInExceptionMessage() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();

      // WHEN / THEN
      assertThatThrownBy(() -> CodeSystemHelper.findCodeInCodeSystem(codeSystem, "MISSING"))
          .isInstanceOf(UnknownConceptException.class)
          .hasMessageContaining("MISSING")
          .hasMessageContaining(CS_URL)
          .hasMessageContaining(CS_VERSION);
    }

    @Test
    @DisplayName("should not append null to exception message when CodeSystem has no version")
    void shouldNotAppendNullVersionWhenCodeSystemHasNoVersion() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setUrl(CS_URL);
      codeSystem.setCaseSensitive(true);

      // WHEN / THEN
      assertThatThrownBy(() -> CodeSystemHelper.findCodeInCodeSystem(codeSystem, "MISSING"))
          .isInstanceOf(UnknownConceptException.class)
          .hasMessageNotContaining("null");
    }

    @Test
    @DisplayName("should find code when CodeSystem is case-sensitive and case matches exactly")
    void shouldFindCodeWhenCaseSensitiveAndCaseMatches() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("MixedCase", "Mixed Case Concept"));

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "MixedCase");

      // THEN
      assertThat(result.getCode()).isEqualTo("MixedCase");
    }

    @Test
    @DisplayName("should throw exception when CodeSystem is case-sensitive and case does not match")
    void shouldThrowExceptionWhenCaseSensitiveAndCaseMismatch() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("MixedCase", "Mixed Case Concept"));

      // WHEN / THEN
      assertThatThrownBy(() -> CodeSystemHelper.findCodeInCodeSystem(codeSystem, "mixedcase"))
          .isInstanceOf(UnknownConceptException.class);
    }

    @Test
    @DisplayName("should find code case-insensitively when CodeSystem is not case-sensitive")
    void shouldFindCodeCaseInsensitivelyWhenNotCaseSensitive() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setUrl(CS_URL);
      codeSystem.setVersion(CS_VERSION);
      codeSystem.setCaseSensitive(false);
      codeSystem.addConcept(createConcept("mixedcode", "Mixed Code Concept"));

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "MIXEDCODE");

      // THEN
      assertThat(result.getCode()).isEqualTo("mixedcode");
    }

    @Test
    @DisplayName("should populate USERDATA_CONCEPTS_AS_MAP after first call")
    void shouldPopulateCacheAfterFirstCall() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("CODE", "Concept"));

      // WHEN
      CodeSystemHelper.findCodeInCodeSystem(codeSystem, "CODE");

      // THEN
      assertThat(codeSystem.hasUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP)).isTrue();
    }

    @Test
    @DisplayName("should use pre-populated USERDATA_CONCEPTS_AS_MAP cache without rebuilding")
    void shouldUsePrePopulatedCacheWhenPresent() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      // No real concepts on the CodeSystem – only in the pre-built cache map
      CodeSystem.ConceptDefinitionComponent cachedConcept = createConcept("CACHED", "Cached");
      Map<String, CodeSystem.ConceptDefinitionComponent> prebuiltMap = new HashMap<>();
      prebuiltMap.put("CACHED", cachedConcept);
      codeSystem.setUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP, prebuiltMap);

      // WHEN
      CodeSystem.ConceptDefinitionComponent result =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, "CACHED");

      // THEN
      assertThat(result).isSameAs(cachedConcept);
    }

    @Test
    @DisplayName("should reuse the same map instance on repeated calls")
    void shouldReuseCachedMapOnRepeatedCalls() throws UnknownConceptException {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("CODE", "Concept"));

      // WHEN – first call populates the cache
      CodeSystemHelper.findCodeInCodeSystem(codeSystem, "CODE");
      Object mapAfterFirstCall = codeSystem.getUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP);

      // WHEN – second call must reuse it
      CodeSystemHelper.findCodeInCodeSystem(codeSystem, "CODE");
      Object mapAfterSecondCall = codeSystem.getUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP);

      // THEN
      assertThat(mapAfterSecondCall).isSameAs(mapAfterFirstCall);
    }
  }

  // ---------------------------------------------------------------------------
  // getCachedAllConceptsAsList(CodeSystem)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("getCachedAllConceptsAsList(CodeSystem)")
  class GetCachedAllConceptsAsListTests {

    @Test
    @DisplayName("should return all top-level concepts")
    void shouldReturnAllTopLevelConcepts() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));
      codeSystem.addConcept(createConcept("B", "Concept B"));

      // WHEN
      List<ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(result).extracting(ConceptDefinitionComponent::getCode).containsExactly("A", "B");
    }

    @Test
    @DisplayName("should flatten nested concepts into a single list")
    void shouldFlattenNestedConcepts() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      ConceptDefinitionComponent parent = createConcept("PARENT", "Parent");
      parent.addConcept(createConcept("CHILD", "Child"));
      codeSystem.addConcept(parent);
      codeSystem.addConcept(createConcept("SIBLING", "Sibling"));

      // WHEN
      List<ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(result)
          .extracting(ConceptDefinitionComponent::getCode)
          .containsExactly("PARENT", "CHILD", "SIBLING");
    }

    @Test
    @DisplayName("should flatten deeply nested hierarchy")
    void shouldFlattenDeeplyNestedHierarchy() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      ConceptDefinitionComponent level1 = createConcept("L1", "Level 1");
      ConceptDefinitionComponent level2 = createConcept("L2", "Level 2");
      ConceptDefinitionComponent level3 = createConcept("L3", "Level 3");
      level2.addConcept(level3);
      level1.addConcept(level2);
      codeSystem.addConcept(level1);

      // WHEN
      List<ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(result)
          .extracting(ConceptDefinitionComponent::getCode)
          .containsExactly("L1", "L2", "L3");
    }

    @Test
    @DisplayName("should return empty list for CodeSystem without concepts")
    void shouldReturnEmptyListForCodeSystemWithoutConcepts() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();

      // WHEN
      List<ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return unmodifiable list")
    void shouldReturnUnmodifiableList() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      List<ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      ConceptDefinitionComponent conceptToAdd = createConcept("X", "X");
      assertThatThrownBy(() -> result.add(conceptToAdd))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should populate USERDATA_CONCEPTS_AS_LIST after first call")
    void shouldPopulateCacheAfterFirstCall() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(codeSystem.hasUserData(ResourceUserData.USERDATA_CONCEPTS_AS_LIST)).isTrue();
    }

    @Test
    @DisplayName("should reuse the same list instance on repeated calls")
    void shouldReuseCachedListOnRepeatedCalls() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      List<ConceptDefinitionComponent> firstCall =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);
      List<ConceptDefinitionComponent> secondCall =
          CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

      // THEN
      assertThat(secondCall).isSameAs(firstCall);
    }
  }

  // ---------------------------------------------------------------------------
  // getCachedAllConceptsAsMap(CodeSystem)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("getCachedAllConceptsAsMap(CodeSystem)")
  class GetCachedAllConceptsAsMapTests {

    @Test
    @DisplayName("should return all top-level concepts keyed by code")
    void shouldReturnAllTopLevelConceptsKeyedByCode() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));
      codeSystem.addConcept(createConcept("B", "Concept B"));

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).containsOnlyKeys("A", "B");
      assertThat(result.get("A").getDisplay()).isEqualTo("Concept A");
      assertThat(result.get("B").getDisplay()).isEqualTo("Concept B");
    }

    @Test
    @DisplayName("should flatten nested concepts into the map")
    void shouldFlattenNestedConceptsIntoMap() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      ConceptDefinitionComponent parent = createConcept("PARENT", "Parent");
      parent.addConcept(createConcept("CHILD", "Child"));
      codeSystem.addConcept(parent);

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).containsOnlyKeys("PARENT", "CHILD");
    }

    @Test
    @DisplayName("should flatten deeply nested hierarchy into the map")
    void shouldFlattenDeeplyNestedHierarchyIntoMap() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      ConceptDefinitionComponent level1 = createConcept("L1", "Level 1");
      ConceptDefinitionComponent level2 = createConcept("L2", "Level 2");
      ConceptDefinitionComponent level3 = createConcept("L3", "Level 3");
      level2.addConcept(level3);
      level1.addConcept(level2);
      codeSystem.addConcept(level1);

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).containsOnlyKeys("L1", "L2", "L3");
    }

    @Test
    @DisplayName("should return empty map for CodeSystem without concepts")
    void shouldReturnEmptyMapForCodeSystemWithoutConcepts() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should store keys in lower case when CodeSystem is not case-sensitive")
    void shouldStoreLowerCaseKeysWhenNotCaseSensitive() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setUrl(CS_URL);
      codeSystem.setVersion(CS_VERSION);
      codeSystem.setCaseSensitive(false);
      codeSystem.addConcept(createConcept("MixedCase", "Mixed"));

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).containsKey("mixedcase").doesNotContainKey("MixedCase");
    }

    @Test
    @DisplayName("should preserve original case in keys when CodeSystem is case-sensitive")
    void shouldPreserveOriginalCaseWhenCaseSensitive() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("MixedCase", "Mixed"));

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(result).containsKey("MixedCase").doesNotContainKey("mixedcase");
    }

    @Test
    @DisplayName("should return unmodifiable map")
    void shouldReturnUnmodifiableMap() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      Map<String, ConceptDefinitionComponent> result =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      ConceptDefinitionComponent conceptToPut = createConcept("X", "X");
      assertThatThrownBy(() -> result.put("X", conceptToPut))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should populate USERDATA_CONCEPTS_AS_MAP after first call")
    void shouldPopulateCacheAfterFirstCall() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(codeSystem.hasUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP)).isTrue();
    }

    @Test
    @DisplayName("should reuse the same map instance on repeated calls")
    void shouldReuseCachedMapOnRepeatedCalls() {
      // GIVEN
      CodeSystem codeSystem = createCaseSensitiveCodeSystem();
      codeSystem.addConcept(createConcept("A", "Concept A"));

      // WHEN
      Map<String, ConceptDefinitionComponent> firstCall =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);
      Map<String, ConceptDefinitionComponent> secondCall =
          CodeSystemHelper.getCachedAllConceptsAsMap(codeSystem);

      // THEN
      assertThat(secondCall).isSameAs(firstCall);
    }
  }

  // ---------------------------------------------------------------------------
  // Helper methods
  // ---------------------------------------------------------------------------

  private static CodeSystem createCaseSensitiveCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(CS_URL);
    codeSystem.setVersion(CS_VERSION);
    codeSystem.setCaseSensitive(true);
    return codeSystem;
  }

  private static CodeSystem.ConceptDefinitionComponent createConcept(String code, String display) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode(code);
    concept.setDisplay(display);
    return concept;
  }
}
