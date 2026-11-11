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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.terminologies.snomed.graph.caching.GraphCacheKeys;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

/**
 * Characterisation tests for {@link SnomedGraphRepresentation}.
 *
 * <p>These tests document the current behaviour of the graph traversal methods for IS-A
 * relationship queries. They use a small hand-built concept hierarchy (from {@link
 * SnomedTestData#defaultHierarchy()}) and bypass the file-loading constructor via reflection, since
 * the production constructor is coupled to the file system.
 *
 * <p>The test hierarchy is:
 *
 * <pre>
 *   ROOT (138875005)
 *    ├── CLINICAL_FINDING (404684003)
 *    │    ├── DISORDER (64572001)
 *    │    │    ├── DIABETES (73211009)
 *    │    │    └── HYPERTENSION (38341003)
 *    │    └── FINDING (441742003)
 *    └── PROCEDURE (71388002)
 *         └── SURGERY (387713003)
 * </pre>
 */
@DisplayName("SnomedGraphRepresentation Tests")
class SnomedGraphRepresentationTest {

  private SnomedGraphRepresentation representation;
  private SnomedReleaseVersion coreVersion;

  @BeforeEach
  void setUp() throws Exception {
    Graph<String, RelationshipEdge> graph = SnomedTestData.defaultHierarchy();
    coreVersion = SnomedTestData.defaultCoreVersion();
    representation = createTestableInstance(graph);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // hasActiveRelations()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("hasActiveRelations() Tests")
  class HasActiveRelationsTests {

    @Test
    @DisplayName("Should return true for concept with active outgoing IS-A relationships")
    void shouldReturnTrueForConceptWithActiveRelationships() throws Exception {
      // GIVEN – DIABETES has an outgoing edge to DISORDER

      // WHEN
      boolean result =
          representation.hasActiveRelations(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(result).as("Concept with active IS-A relationships should return true").isTrue();
    }

    @Test
    @DisplayName("Should return true for root concept (has incoming edges from children)")
    void shouldReturnTrueForRootConcept() throws Exception {
      // GIVEN – ROOT has incoming edges from CLINICAL_FINDING and PROCEDURE

      // WHEN
      boolean result = representation.hasActiveRelations(SnomedTestData.CONCEPT_ROOT, coreVersion);

      // THEN
      assertThat(result).as("Root concept has incoming edges and should return true").isTrue();
    }

    @Test
    @DisplayName("Should throw exception for unknown concept id")
    void shouldThrowExceptionForUnknownConcept() {
      // GIVEN – concept not in graph

      // WHEN / THEN
      assertThatThrownBy(
              () -> representation.hasActiveRelations(SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .as("Unknown concept should throw UnknownConceptException")
          .isInstanceOf(UnknownConceptException.class);
    }

    @Test
    @DisplayName("Should return false when relationships exist but are inactive for the version")
    void shouldReturnFalseForInactiveRelationships() throws Exception {
      // GIVEN – build a graph with only inactive edges
      Graph<String, RelationshipEdge> graph = SnomedTestData.emptyGraph();
      SnomedTestData.addIsARelationship(
          graph,
          "rel-1",
          SnomedTestData.CONCEPT_DIABETES,
          SnomedTestData.CONCEPT_DISORDER,
          SnomedTestData.EFFECTIVE_TIME_V1,
          false);
      SnomedGraphRepresentation inactiveRep = createTestableInstance(graph);

      // WHEN
      boolean result = inactiveRep.hasActiveRelations(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(result)
          .as("Concept with only inactive relationships should return false")
          .isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getDirectParents()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getDirectParents() Tests")
  class GetDirectParentsTests {

    @Test
    @DisplayName("Should return direct parent concepts")
    void shouldReturnDirectParents() throws Exception {
      // GIVEN – DIABETES is a child of DISORDER

      // WHEN
      Set<String> parents =
          representation.getDirectParents(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(parents)
          .as("DIABETES should have exactly one parent: DISORDER")
          .containsExactly(SnomedTestData.CONCEPT_DISORDER);
    }

    @Test
    @DisplayName("Should return multiple parents when concept has multiple IS-A relationships")
    void shouldReturnMultipleParents() throws Exception {
      // GIVEN – CLINICAL_FINDING and PROCEDURE are both children of ROOT
      // Let's check DISORDER which has only one parent (CLINICAL_FINDING)

      // WHEN
      Set<String> parents =
          representation.getDirectParents(SnomedTestData.CONCEPT_CLINICAL_FINDING, coreVersion);

      // THEN
      assertThat(parents)
          .as("CLINICAL_FINDING should have ROOT as parent")
          .containsExactly(SnomedTestData.CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should return empty set for root concept (no parents)")
    void shouldReturnEmptySetForRootConcept() throws Exception {
      // GIVEN – ROOT has no outgoing IS-A edges

      // WHEN
      Set<String> parents =
          representation.getDirectParents(SnomedTestData.CONCEPT_ROOT, coreVersion);

      // THEN
      assertThat(parents).as("Root concept should have no parents").isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for unknown concept id")
    void shouldThrowExceptionForUnknownConcept() {
      // GIVEN – concept not in graph

      // WHEN / THEN
      assertThatThrownBy(
              () -> representation.getDirectParents(SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .as("Unknown concept should throw UnknownConceptException")
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getDirectChildren()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getDirectChildren() Tests")
  class GetDirectChildrenTests {

    @Test
    @DisplayName("Should return direct child concepts")
    void shouldReturnDirectChildren() throws Exception {
      // GIVEN – DISORDER has two children: DIABETES and HYPERTENSION

      // WHEN
      Set<String> children =
          representation.getDirectChildren(SnomedTestData.CONCEPT_DISORDER, coreVersion);

      // THEN
      assertThat(children)
          .as("DISORDER should have DIABETES and HYPERTENSION as children")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_DIABETES, SnomedTestData.CONCEPT_HYPERTENSION);
    }

    @Test
    @DisplayName("Should return empty set for leaf concept (no children)")
    void shouldReturnEmptySetForLeafConcept() throws Exception {
      // GIVEN – DIABETES is a leaf node

      // WHEN
      Set<String> children =
          representation.getDirectChildren(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(children).as("Leaf concept should have no children").isEmpty();
    }

    @Test
    @DisplayName("Should return children from both subtrees for root")
    void shouldReturnChildrenOfRoot() throws Exception {
      // GIVEN – ROOT has two children: CLINICAL_FINDING and PROCEDURE

      // WHEN
      Set<String> children =
          representation.getDirectChildren(SnomedTestData.CONCEPT_ROOT, coreVersion);

      // THEN
      assertThat(children)
          .as("ROOT should have CLINICAL_FINDING and PROCEDURE as children")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_CLINICAL_FINDING, SnomedTestData.CONCEPT_PROCEDURE);
    }

    @Test
    @DisplayName("Should throw exception for unknown concept id")
    void shouldThrowExceptionForUnknownConcept() {
      // GIVEN – concept not in graph

      // WHEN / THEN
      assertThatThrownBy(
              () -> representation.getDirectChildren(SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .as("Unknown concept should throw UnknownConceptException")
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getAllDescendantsOrSelf()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getAllDescendantsOrSelf() Tests")
  class GetAllDescendantsOrSelfTests {

    @Test
    @DisplayName("Should return self and all transitive descendants")
    void shouldReturnSelfAndAllDescendants() throws Exception {
      // GIVEN – CLINICAL_FINDING → DISORDER → DIABETES, HYPERTENSION; FINDING

      // WHEN
      Set<String> result =
          representation.getAllDescendantsOrSelf(
              SnomedTestData.CONCEPT_CLINICAL_FINDING, coreVersion);

      // THEN
      assertThat(result)
          .as("Should include CLINICAL_FINDING itself plus all its descendants")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_CLINICAL_FINDING,
              SnomedTestData.CONCEPT_DISORDER,
              SnomedTestData.CONCEPT_DIABETES,
              SnomedTestData.CONCEPT_HYPERTENSION,
              SnomedTestData.CONCEPT_FINDING);
    }

    @Test
    @DisplayName("Should return only self for leaf concept")
    void shouldReturnOnlySelfForLeafConcept() throws Exception {
      // GIVEN – SURGERY has no children

      // WHEN
      Set<String> result =
          representation.getAllDescendantsOrSelf(SnomedTestData.CONCEPT_SURGERY, coreVersion);

      // THEN
      assertThat(result)
          .as("Leaf concept should return only itself")
          .containsExactly(SnomedTestData.CONCEPT_SURGERY);
    }

    @Test
    @DisplayName("Should return all concepts when starting from root")
    void shouldReturnAllConceptsFromRoot() throws Exception {
      // GIVEN – ROOT is the top of the hierarchy

      // WHEN
      Set<String> result =
          representation.getAllDescendantsOrSelf(SnomedTestData.CONCEPT_ROOT, coreVersion);

      // THEN
      assertThat(result)
          .as("Starting from ROOT should return all 8 concepts")
          .hasSize(8)
          .contains(
              SnomedTestData.CONCEPT_ROOT,
              SnomedTestData.CONCEPT_CLINICAL_FINDING,
              SnomedTestData.CONCEPT_PROCEDURE,
              SnomedTestData.CONCEPT_DISORDER,
              SnomedTestData.CONCEPT_FINDING,
              SnomedTestData.CONCEPT_DIABETES,
              SnomedTestData.CONCEPT_HYPERTENSION,
              SnomedTestData.CONCEPT_SURGERY);
    }

    @Test
    @DisplayName("Should throw UnknownConceptException for unknown concept id")
    void shouldThrowUnknownConceptForUnknownConcept() {
      // GIVEN / WHEN / THEN
      assertThatThrownBy(
              () ->
                  representation.getAllDescendantsOrSelf(
                      SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getAllDescendants()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getAllDescendants() Tests")
  class GetAllDescendantsTests {

    @Test
    @DisplayName("Should return all transitive descendants without self")
    void shouldReturnDescendantsWithoutSelf() throws Exception {
      // GIVEN – DISORDER has DIABETES and HYPERTENSION

      // WHEN
      Set<String> result =
          representation.getAllDescendants(SnomedTestData.CONCEPT_DISORDER, coreVersion);

      // THEN
      assertThat(result)
          .as("Should contain DIABETES and HYPERTENSION but NOT DISORDER itself")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_DIABETES, SnomedTestData.CONCEPT_HYPERTENSION)
          .doesNotContain(SnomedTestData.CONCEPT_DISORDER);
    }

    @Test
    @DisplayName("Should return empty set for leaf concept")
    void shouldReturnEmptySetForLeafConcept() throws Exception {
      // GIVEN – DIABETES has no children

      // WHEN
      Set<String> result =
          representation.getAllDescendants(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(result).as("Leaf concept should have no descendants").isEmpty();
    }

    @Test
    @DisplayName("Should throw UnknownConceptException for unknown concept id")
    void shouldThrowUnknownConceptForUnknownConcept() {
      // GIVEN / WHEN / THEN
      assertThatThrownBy(
              () -> representation.getAllDescendants(SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getAllAncestorsOrSelf()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getAllAncestorsOrSelf() Tests")
  class GetAllAncestorsOrSelfTests {

    @Test
    @DisplayName("Should return self and all transitive ancestors")
    void shouldReturnSelfAndAllAncestors() throws Exception {
      // GIVEN – DIABETES → DISORDER → CLINICAL_FINDING → ROOT

      // WHEN
      Set<String> result =
          representation.getAllAncestorsOrSelf(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(result)
          .as("Should include DIABETES, DISORDER, CLINICAL_FINDING, and ROOT")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_DIABETES,
              SnomedTestData.CONCEPT_DISORDER,
              SnomedTestData.CONCEPT_CLINICAL_FINDING,
              SnomedTestData.CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should return only self for root concept")
    void shouldReturnOnlySelfForRoot() throws Exception {
      // GIVEN – ROOT has no parents

      // WHEN
      Set<String> result =
          representation.getAllAncestorsOrSelf(SnomedTestData.CONCEPT_ROOT, coreVersion);

      // THEN
      assertThat(result)
          .as("Root concept should only return itself")
          .containsExactly(SnomedTestData.CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should traverse correct path for concept in procedure subtree")
    void shouldTraverseCorrectPathForProcedureSubtree() throws Exception {
      // GIVEN – SURGERY → PROCEDURE → ROOT

      // WHEN
      Set<String> result =
          representation.getAllAncestorsOrSelf(SnomedTestData.CONCEPT_SURGERY, coreVersion);

      // THEN
      assertThat(result)
          .as("SURGERY ancestors should be PROCEDURE and ROOT")
          .containsExactlyInAnyOrder(
              SnomedTestData.CONCEPT_SURGERY,
              SnomedTestData.CONCEPT_PROCEDURE,
              SnomedTestData.CONCEPT_ROOT);
    }

    @Test
    @DisplayName("Should throw UnknownConceptException for unknown concept id")
    void shouldThrowUnknownConceptForUnknownConcept() {
      // GIVEN / WHEN / THEN
      assertThatThrownBy(
              () ->
                  representation.getAllAncestorsOrSelf(SnomedTestData.CONCEPT_UNKNOWN, coreVersion))
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getConceptsWithNoParents()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getConceptsWithNoParents() Tests")
  class GetConceptsWithNoParentsTests {

    @Test
    @DisplayName("Should throw NotImplementedException (current behaviour)")
    void shouldThrowNotImplementedException() {
      // GIVEN / WHEN / THEN
      assertThatThrownBy(() -> representation.getConceptsWithNoParents(coreVersion))
          .as("Method is not implemented and should throw NotImplementedException")
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("not implemented");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Version-aware edge filtering
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Version-aware edge filtering Tests")
  class VersionAwareFilteringTests {

    @Test
    @DisplayName("Should exclude inactive edges from direct children results")
    void shouldExcludeInactiveEdgesFromDirectChildren() throws Exception {
      // GIVEN – DISORDER has two children, but one edge is inactive
      Graph<String, RelationshipEdge> graph = SnomedTestData.emptyGraph();
      SnomedTestData.addIsARelationship(
          graph,
          "rel-1",
          SnomedTestData.CONCEPT_DIABETES,
          SnomedTestData.CONCEPT_DISORDER,
          SnomedTestData.EFFECTIVE_TIME_V1,
          true);
      SnomedTestData.addIsARelationship(
          graph,
          "rel-2",
          SnomedTestData.CONCEPT_HYPERTENSION,
          SnomedTestData.CONCEPT_DISORDER,
          SnomedTestData.EFFECTIVE_TIME_V1,
          false);
      SnomedGraphRepresentation rep = createTestableInstance(graph);

      // WHEN
      Set<String> children = rep.getDirectChildren(SnomedTestData.CONCEPT_DISORDER, coreVersion);

      // THEN
      assertThat(children)
          .as("Only active edges should be considered")
          .containsExactly(SnomedTestData.CONCEPT_DIABETES)
          .doesNotContain(SnomedTestData.CONCEPT_HYPERTENSION);
    }

    @Test
    @DisplayName("Should only consider edges from relevant modules")
    void shouldOnlyConsiderRelevantModuleEdges() throws Exception {
      // GIVEN – edge belongs to German module, but version is core-only
      Graph<String, RelationshipEdge> graph = SnomedTestData.emptyGraph();
      graph.addVertex(SnomedTestData.CONCEPT_DIABETES);
      graph.addVertex(SnomedTestData.CONCEPT_DISORDER);
      RelationshipEdge germanEdge =
          new RelationshipEdge(
              "rel-de-1",
              SnomedTestData.MODULE_GERMAN,
              new RelationshipState(SnomedTestData.EFFECTIVE_TIME_V1, true));
      graph.addEdge(SnomedTestData.CONCEPT_DIABETES, SnomedTestData.CONCEPT_DISORDER, germanEdge);
      SnomedGraphRepresentation rep = createTestableInstance(graph);

      // WHEN – query with core-only version (no German module in dependencies)
      Set<String> parents = rep.getDirectParents(SnomedTestData.CONCEPT_DIABETES, coreVersion);

      // THEN
      assertThat(parents).as("Edges from non-relevant modules should be filtered out").isEmpty();
    }

    @Test
    @DisplayName("Should consider edges whose effectiveTime is within the version date")
    void shouldFilterEdgesByVersionDate() throws Exception {
      // GIVEN – edge activated in V2, query with V1 (earlier)
      Graph<String, RelationshipEdge> graph = SnomedTestData.emptyGraph();
      SnomedTestData.addIsARelationship(
          graph,
          "rel-1",
          SnomedTestData.CONCEPT_DIABETES,
          SnomedTestData.CONCEPT_DISORDER,
          SnomedTestData.EFFECTIVE_TIME_V2,
          true);

      SnomedReleaseVersion v1 = SnomedTestData.coreVersion(SnomedTestData.DATE_V1);
      SnomedReleaseVersion v2 = SnomedTestData.coreVersion(SnomedTestData.DATE_V2);
      SnomedGraphRepresentation rep = createTestableInstance(graph);

      // WHEN
      Set<String> parentsAtV1 = rep.getDirectParents(SnomedTestData.CONCEPT_DIABETES, v1);
      Set<String> parentsAtV2 = rep.getDirectParents(SnomedTestData.CONCEPT_DIABETES, v2);

      // THEN
      assertThat(parentsAtV1).as("Edge activated at V2 should not be visible at V1").isEmpty();
      assertThat(parentsAtV2)
          .as("Edge activated at V2 should be visible at V2")
          .containsExactly(SnomedTestData.CONCEPT_DISORDER);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Test infrastructure – bypasses file-loading constructor via reflection
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates a {@link SnomedGraphRepresentation} instance with an injected graph, bypassing the
   * file-loading constructor. Uses Objenesis (available via Mockito dependency) for instantiation
   * without calling the constructor.
   *
   * <p>This is necessary because the production constructor is tightly coupled to the file system
   * and requires a real SNOMED RF2 release to be present on disk.
   */
  private static SnomedGraphRepresentation createTestableInstance(
      Graph<String, RelationshipEdge> graph) throws Exception {

    SnomedGraphRepresentation instance =
        new ObjenesisStd().newInstance(SnomedGraphRepresentation.class);

    EdgeReversedGraph<String, RelationshipEdge> reversedGraph = new EdgeReversedGraph<>(graph);

    setPrivateField(instance, "versionGraph", graph);
    setPrivateField(instance, "reversedVersionGraph", reversedGraph);

    // Set up the descendants cache with a loader that delegates to the private traversal method
    LoadingCache<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>> cache =
        CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                new CacheLoader<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>>() {
                  @Override
                  public Set<String> load(GraphCacheKeys.SnomedVersionStringCacheKey key)
                      throws Exception {
                    // Replicate what getAllDescendantsOrSelfInternal does:
                    // it calls getAllDescendantsOrSelf(versionGraph, conceptId, version)
                    // which traverses reversedVersionGraph via BFS
                    Set<String> resultSet = new HashSet<>();
                    try {
                      BreadthFirstIterator<String, RelationshipEdge> iterator =
                          new BreadthFirstIterator<>(reversedGraph, key.cacheValue()) {
                            @Override
                            protected Set<RelationshipEdge> selectOutgoingEdges(String vertex) {
                              Set<RelationshipEdge> outgoingEdges = new HashSet<>();
                              SnomedReleaseVersion version = key.snomedReleaseVersion();
                              reversedGraph.outgoingEdgesOf(vertex).stream()
                                  .filter(
                                      edge ->
                                          version
                                              .getRelevantModuleVersions()
                                              .containsKey(edge.getModuleId()))
                                  .filter(
                                      edge ->
                                          edge.isActive(
                                              Long.parseLong(
                                                  version
                                                      .getRelevantModuleVersions()
                                                      .get(edge.getModuleId()))))
                                  .forEach(outgoingEdges::add);
                              return outgoingEdges;
                            }
                          };
                      while (iterator.hasNext()) {
                        resultSet.add(iterator.next());
                      }
                    } catch (IllegalArgumentException _) {
                      throw new UnknownConceptException(
                          "Unknown SNOMED concept id in graph traversal: " + key.cacheValue());
                    }
                    return resultSet;
                  }
                });
    setPrivateField(instance, "versionNodeDecendantsOrSelfCache", cache);

    return instance;
  }

  private static void setPrivateField(Object target, String fieldName, Object value)
      throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
