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
package de.gematik.zts.termzilla.terminologies.fhirterms.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FhirTermGraphRepresentation}. */
@DisplayName("FhirTermGraphRepresentation Tests")
class FhirTermGraphRepresentationTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create graph from simple hierarchical code system")
    void shouldCreateGraphFromSimpleHierarchy() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();

      // WHEN
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // THEN
      assertThat(graph.getVersion()).isEqualTo("1.0.0");
      assertThat(graph.getVertices()).containsExactlyInAnyOrder("A", "A1", "A2", "B");
    }

    @Test
    @DisplayName("Should create graph from code system with parent properties")
    void shouldCreateGraphFromParentProperties() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createCodeSystemWithParentProperties();

      // WHEN
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // THEN
      assertThat(graph.getVertices()).containsExactlyInAnyOrder("ROOT", "CHILD1", "CHILD2");
    }

    @Test
    @DisplayName("Should create graph from code system with child properties")
    void shouldCreateGraphFromChildProperties() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createCodeSystemWithChildProperties();

      // WHEN
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // THEN
      assertThat(graph.getVertices()).containsExactlyInAnyOrder("ROOT", "CHILD1", "CHILD2");
    }

    @Test
    @DisplayName("Should handle empty code system")
    void shouldHandleEmptyCodeSystem() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createEmptyCodeSystem();

      // WHEN
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // THEN
      assertThat(graph.getVertices()).isEmpty();
    }

    @Test
    @DisplayName("Should skip concepts without code")
    void shouldSkipConceptsWithoutCode() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createCodeSystemWithMissingCodes();

      // WHEN
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // THEN
      assertThat(graph.getVertices()).containsExactly("VALID_CODE");
    }
  }

  @Nested
  @DisplayName("getRoots Tests")
  class GetRootsTests {

    @Test
    @DisplayName("Should return single root for simple hierarchy")
    void shouldReturnSingleRoot() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> roots = graph.getRoots();

      // THEN
      assertThat(roots).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    @DisplayName("Should return multiple roots for forest structure")
    void shouldReturnMultipleRoots() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createForestStructureCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> roots = graph.getRoots();

      // THEN
      assertThat(roots).containsExactlyInAnyOrder("ROOT1", "ROOT2");
    }

    @Test
    @DisplayName("Should return empty set when no roots exist")
    void shouldReturnEmptySetWhenNoRoots() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createEmptyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> roots = graph.getRoots();

      // THEN
      assertThat(roots).isEmpty();
    }
  }

  @Nested
  @DisplayName("getLeaves Tests")
  class GetLeavesTests {

    @Test
    @DisplayName("Should return leaf nodes for simple hierarchy")
    void shouldReturnLeafNodes() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> leaves = graph.getLeaves();

      // THEN
      assertThat(leaves).containsExactlyInAnyOrder("A1", "A2", "B");
    }

    @Test
    @DisplayName("Should return all nodes when no hierarchy exists")
    void shouldReturnAllNodesWhenNoHierarchy() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createFlatCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> leaves = graph.getLeaves();

      // THEN
      assertThat(leaves).containsExactlyInAnyOrder("CODE1", "CODE2", "CODE3");
    }

    @Test
    @DisplayName("Should return empty set when no leaves exist")
    void shouldReturnEmptySetWhenNoLeaves() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createEmptyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> leaves = graph.getLeaves();

      // THEN
      assertThat(leaves).isEmpty();
    }
  }

  @Nested
  @DisplayName("getAllDescendants Tests")
  class GetAllDescendantsTests {

    @Test
    @DisplayName("Should return all descendants for root node")
    void shouldReturnAllDescendantsForRoot() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> descendants = graph.getAllDescendants("A");

      // THEN
      assertThat(descendants).containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    @DisplayName("Should return empty set for leaf node")
    void shouldReturnEmptySetForLeafNode() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> descendants = graph.getAllDescendants("A1");

      // THEN
      assertThat(descendants).isEmpty();
    }

    @Test
    @DisplayName("Should return all descendants in deep hierarchy")
    void shouldReturnAllDescendantsInDeepHierarchy() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createDeepHierarchyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> descendants = graph.getAllDescendants("LEVEL1");

      // THEN
      assertThat(descendants).containsExactlyInAnyOrder("LEVEL2", "LEVEL3", "LEVEL4");
    }

    @Test
    @DisplayName("Should not include the concept itself in descendants")
    void shouldNotIncludeConceptItselfInDescendants() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> descendants = graph.getAllDescendants("A");

      // THEN
      assertThat(descendants).isNotEmpty().doesNotContain("A");
    }
  }

  @Nested
  @DisplayName("getAllAncestors Tests")
  class GetAllAncestorsTests {

    @Test
    @DisplayName("Should return all ancestors for leaf node")
    void shouldReturnAllAncestorsForLeaf() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> ancestors = graph.getAllAncestors("A1");

      // THEN
      assertThat(ancestors).containsExactly("A");
    }

    @Test
    @DisplayName("Should return empty set for root node")
    void shouldReturnEmptySetForRootNode() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> ancestors = graph.getAllAncestors("A");

      // THEN
      assertThat(ancestors).isEmpty();
    }

    @Test
    @DisplayName("Should return all ancestors in deep hierarchy")
    void shouldReturnAllAncestorsInDeepHierarchy() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createDeepHierarchyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> ancestors = graph.getAllAncestors("LEVEL4");

      // THEN
      assertThat(ancestors).containsExactlyInAnyOrder("LEVEL1", "LEVEL2", "LEVEL3");
    }

    @Test
    @DisplayName("Should not include the concept itself in ancestors")
    void shouldNotIncludeConceptItselfInAncestors() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> ancestors = graph.getAllAncestors("A1");

      // THEN
      assertThat(ancestors).isNotEmpty().doesNotContain("A1");
    }
  }

  @Nested
  @DisplayName("getAllConceptsWithDirectParent Tests")
  class GetAllConceptsWithDirectParentTests {

    @Test
    @DisplayName("Should return direct children of parent concept")
    void shouldReturnDirectChildren() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directChildren = graph.getAllConceptsWithDirectParent("A");

      // THEN
      assertThat(directChildren).containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    @DisplayName("Should return empty set for leaf nodes")
    void shouldReturnEmptySetForLeafNodes() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directChildren = graph.getAllConceptsWithDirectParent("A1");

      // THEN
      assertThat(directChildren).isEmpty();
    }

    @Test
    @DisplayName("Should only return direct children, not all descendants")
    void shouldOnlyReturnDirectChildren() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createDeepHierarchyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directChildren = graph.getAllConceptsWithDirectParent("LEVEL1");

      // THEN
      assertThat(directChildren).containsExactly("LEVEL2").doesNotContain("LEVEL3", "LEVEL4");
    }
  }

  @Nested
  @DisplayName("getAllConceptsWithDirectChild Tests")
  class GetAllConceptsWithDirectChildTests {

    @Test
    @DisplayName("Should return direct parents of child concept")
    void shouldReturnDirectParents() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directParents = graph.getAllConceptsWithDirectChild("A1");

      // THEN
      assertThat(directParents).containsExactly("A");
    }

    @Test
    @DisplayName("Should return empty set for root nodes")
    void shouldReturnEmptySetForRootNodes() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createSimpleHierarchicalCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directParents = graph.getAllConceptsWithDirectChild("A");

      // THEN
      assertThat(directParents).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple parents in diamond structure")
    void shouldHandleMultipleParents() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createDiamondStructureCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directParents = graph.getAllConceptsWithDirectChild("BOTTOM");

      // THEN
      assertThat(directParents).containsExactlyInAnyOrder("LEFT", "RIGHT");
    }

    @Test
    @DisplayName("Should only return direct parents, not all ancestors")
    void shouldOnlyReturnDirectParents() {
      // GIVEN
      CodeSystem codeSystem = CodeSystemTestDataHelper.createDeepHierarchyCodeSystem();
      FhirTermGraphRepresentation graph = new FhirTermGraphRepresentation(codeSystem);

      // WHEN
      Set<String> directParents = graph.getAllConceptsWithDirectChild("LEVEL4");

      // THEN
      assertThat(directParents).containsExactly("LEVEL3").doesNotContain("LEVEL1", "LEVEL2");
    }
  }
}
