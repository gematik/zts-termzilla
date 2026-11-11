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
package de.gematik.zts.termzilla.terminologies.snomed.graph;

import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GraphTraversalService}.
 *
 * <p>Uses the default test hierarchy from {@code SnomedTestData}:
 *
 * <pre>
 *   ROOT
 *    ├── CLINICAL_FINDING
 *    │    ├── DISORDER
 *    │    │    ├── DIABETES
 *    │    │    └── HYPERTENSION
 *    │    └── FINDING
 *    └── PROCEDURE
 *         └── SURGERY
 * </pre>
 */
@DisplayName("GraphTraversalService Tests")
class GraphTraversalServiceTest {

  private Graph<String, RelationshipEdge> graph;
  private EdgeReversedGraph<String, RelationshipEdge> reversedGraph;
  private SnomedReleaseVersion coreVersion;

  @BeforeEach
  void setUp() {
    graph = defaultHierarchy();
    reversedGraph = new EdgeReversedGraph<>(graph);
    coreVersion = defaultCoreVersion();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // traverseDescendants()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("traverseDescendants() Tests")
  class TraverseDescendantsTests {

    @Test
    @DisplayName("Should return all descendants including self for root concept")
    void shouldReturnAllDescendantsIncludingSelfForRoot() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(reversedGraph, CONCEPT_ROOT, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Root's descendants-or-self should include all 8 concepts")
          .containsExactlyInAnyOrder(
              CONCEPT_ROOT,
              CONCEPT_CLINICAL_FINDING,
              CONCEPT_DISORDER,
              CONCEPT_DIABETES,
              CONCEPT_HYPERTENSION,
              CONCEPT_FINDING,
              CONCEPT_PROCEDURE,
              CONCEPT_SURGERY);
    }

    @Test
    @DisplayName("Should return all descendants excluding self for root concept")
    void shouldReturnAllDescendantsExcludingSelfForRoot() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(
              reversedGraph, CONCEPT_ROOT, coreVersion, false);

      // THEN
      assertThat(result)
          .as("Root's descendants should include all concepts except root")
          .hasSize(7)
          .doesNotContain(CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should return subtree for intermediate concept")
    void shouldReturnSubtreeForIntermediateConcept() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(
              reversedGraph, CONCEPT_DISORDER, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Disorder's descendants-or-self should include Disorder, Diabetes, Hypertension")
          .containsExactlyInAnyOrder(CONCEPT_DISORDER, CONCEPT_DIABETES, CONCEPT_HYPERTENSION);
    }

    @Test
    @DisplayName("Should return only self for leaf concept with includeSelf=true")
    void shouldReturnOnlySelfForLeafConcept() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(
              reversedGraph, CONCEPT_DIABETES, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Leaf concept's descendants-or-self should only contain itself")
          .containsExactly(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("Should return empty set for leaf concept with includeSelf=false")
    void shouldReturnEmptyForLeafConceptWithoutSelf() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(
              reversedGraph, CONCEPT_DIABETES, coreVersion, false);

      // THEN
      assertThat(result).as("Leaf concept's descendants without self should be empty").isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // traverseAncestors()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("traverseAncestors() Tests")
  class TraverseAncestorsTests {

    @Test
    @DisplayName("Should return full ancestor chain including self for leaf concept")
    void shouldReturnFullAncestorChainIncludingSelf() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseAncestors(graph, CONCEPT_DIABETES, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Diabetes ancestors-or-self should be: Diabetes → Disorder → Clinical finding → Root")
          .containsExactlyInAnyOrder(
              CONCEPT_DIABETES, CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should return ancestors excluding self for leaf concept")
    void shouldReturnAncestorsExcludingSelf() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseAncestors(graph, CONCEPT_DIABETES, coreVersion, false);

      // THEN
      assertThat(result)
          .as("Diabetes ancestors should not include itself")
          .containsExactlyInAnyOrder(CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT)
          .doesNotContain(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("Should return only self for root concept with includeSelf=true")
    void shouldReturnOnlySelfForRootConcept() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseAncestors(graph, CONCEPT_ROOT, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Root has no ancestors, so ancestors-or-self should only contain root itself")
          .containsExactly(CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should return empty set for root concept with includeSelf=false")
    void shouldReturnEmptyForRootWithoutSelf() {
      // WHEN
      Set<String> result =
          GraphTraversalService.traverseAncestors(graph, CONCEPT_ROOT, coreVersion, false);

      // THEN
      assertThat(result)
          .as("Root has no ancestors, so result without self should be empty")
          .isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // findRoots()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("findRoots() Tests")
  class FindRootsTests {

    @Test
    @DisplayName("Should find root concept in default hierarchy")
    void shouldFindRootInDefaultHierarchy() {
      // WHEN
      Set<String> roots = GraphTraversalService.findRoots(graph, coreVersion);

      // THEN
      assertThat(roots)
          .as("Only the root concept should have no active outgoing IS-A edges")
          .containsExactly(CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should include concepts with only inactive edges as roots")
    void shouldIncludeConceptsWithOnlyInactiveEdges() {
      // GIVEN — add a concept with only an inactive edge to root
      Graph<String, RelationshipEdge> testGraph = emptyGraph();
      addIsARelationship(testGraph, "rel-1", "A", "B", EFFECTIVE_TIME_V1, true);
      addIsARelationship(testGraph, "rel-2", "C", "B", EFFECTIVE_TIME_V1, false);

      // WHEN
      Set<String> roots = GraphTraversalService.findRoots(testGraph, coreVersion);

      // THEN
      assertThat(roots)
          .as("B (no outgoing) and C (only inactive outgoing) should both be roots")
          .containsExactlyInAnyOrder("B", "C");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Version-aware edge filtering
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Version-aware edge filtering Tests")
  class VersionFilteringTests {

    @Test
    @DisplayName("Should exclude inactive edges during traversal")
    void shouldExcludeInactiveEdges() {
      // GIVEN — graph where DIABETES→DISORDER is inactive
      Graph<String, RelationshipEdge> testGraph = emptyGraph();
      addIsARelationship(testGraph, "rel-1", CONCEPT_DISORDER, CONCEPT_ROOT, EFFECTIVE_TIME_V1);
      addIsARelationship(
          testGraph, "rel-2", CONCEPT_DIABETES, CONCEPT_DISORDER, EFFECTIVE_TIME_V1, false);

      EdgeReversedGraph<String, RelationshipEdge> reversed = new EdgeReversedGraph<>(testGraph);

      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(reversed, CONCEPT_ROOT, coreVersion, true);

      // THEN
      assertThat(result)
          .as("Diabetes should not be reachable via inactive edge")
          .containsExactlyInAnyOrder(CONCEPT_ROOT, CONCEPT_DISORDER)
          .doesNotContain(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("Should exclude edges from non-relevant modules")
    void shouldExcludeEdgesFromNonRelevantModules() {
      // GIVEN — graph with an edge in the German module, queried with core-only version
      Graph<String, RelationshipEdge> testGraph = emptyGraph();
      addIsARelationship(testGraph, "rel-1", CONCEPT_DISORDER, CONCEPT_ROOT, EFFECTIVE_TIME_V1);

      // Add edge in German module manually
      testGraph.addVertex(CONCEPT_DIABETES);
      RelationshipEdge germanEdge =
          new RelationshipEdge(
              "rel-2", MODULE_GERMAN, new RelationshipState(EFFECTIVE_TIME_V1, true));
      testGraph.addEdge(CONCEPT_DIABETES, CONCEPT_DISORDER, germanEdge);

      EdgeReversedGraph<String, RelationshipEdge> reversed = new EdgeReversedGraph<>(testGraph);

      // Core-only version has no German module in relevantModuleVersions
      SnomedReleaseVersion coreOnlyVersion = coreVersion(DATE_V1);

      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(reversed, CONCEPT_ROOT, coreOnlyVersion, true);

      // THEN
      assertThat(result)
          .as("Diabetes should not be reachable via German-module edge with core-only version")
          .containsExactlyInAnyOrder(CONCEPT_ROOT, CONCEPT_DISORDER)
          .doesNotContain(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("Should include edges from German module when using German version")
    void shouldIncludeEdgesFromGermanModuleWithGermanVersion() {
      // GIVEN — graph with an edge in the German module
      Graph<String, RelationshipEdge> testGraph = emptyGraph();
      addIsARelationship(testGraph, "rel-1", CONCEPT_DISORDER, CONCEPT_ROOT, EFFECTIVE_TIME_V1);

      testGraph.addVertex(CONCEPT_DIABETES);
      RelationshipEdge germanEdge =
          new RelationshipEdge(
              "rel-2", MODULE_GERMAN, new RelationshipState(EFFECTIVE_TIME_V2, true));
      testGraph.addEdge(CONCEPT_DIABETES, CONCEPT_DISORDER, germanEdge);

      EdgeReversedGraph<String, RelationshipEdge> reversed = new EdgeReversedGraph<>(testGraph);

      // German version includes both core and German modules
      SnomedReleaseVersion germanVersion = germanVersion(DATE_V2, DATE_V1);

      // WHEN
      Set<String> result =
          GraphTraversalService.traverseDescendants(reversed, CONCEPT_ROOT, germanVersion, true);

      // THEN
      assertThat(result)
          .as("Diabetes should be reachable via German-module edge with German version")
          .containsExactlyInAnyOrder(CONCEPT_ROOT, CONCEPT_DISORDER, CONCEPT_DIABETES);
    }
  }
}
