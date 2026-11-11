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

import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.traverse.DepthFirstIterator;

// TODO: Nochmal über case-sensitivity nachdenken
// TODO: nochmal über den Einfluss von Supplements nachdenken

/**
 * Graph-based representation of FHIR terminology hierarchies. Provides navigation through
 * parent-child relationships in a {@link CodeSystem}.
 */
@Slf4j
public class FhirTermGraphRepresentation {

  private final Graph<String, RelationshipEdge> parentChildGraph;

  @Getter private final String version;

  @Getter private final Set<String> vertices = new HashSet<>();

  /**
   * Constructs a graph representation from a FHIR {@link CodeSystem}. Creates bidirectional edges
   * for parent-child relationships based on concept hierarchy and explicit parent/child properties.
   *
   * @param codeSystem the FHIR code system to represent as a graph
   */
  public FhirTermGraphRepresentation(CodeSystem codeSystem) {

    log.debug(
        "creating parent-child relation graph for CodeSystem {} with version {}",
        codeSystem.getUrl(),
        codeSystem.getVersion());

    version = codeSystem.getVersion();

    parentChildGraph =
        GraphTypeBuilder.directed()
            .allowingMultipleEdges(true)
            .allowingSelfLoops(false)
            .vertexClass(String.class)
            .edgeClass(RelationshipEdge.class)
            .buildGraph();

    List<CodeSystem.ConceptDefinitionComponent> allConcepts =
        CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

    // add vertices first
    allConcepts.forEach(
        concept -> {
          if (concept.hasCode()) {
            String code = concept.getCode();
            parentChildGraph.addVertex(code);
            vertices.add(code);
          } else {
            log.error(
                "Skipping vertex - Concept has no code : code --> {}, display --> {}",
                concept.getDisplay(),
                concept.getCode());
          }
        });

    // Add concepts as vertices and create edges recursively
    for (CodeSystem.ConceptDefinitionComponent concept : codeSystem.getConcept()) {
      addEdges(parentChildGraph, concept);
    }
  }

  /**
   * Creates bidirectional parent-child edges between two concepts.
   *
   * @param graph the graph to add edges to
   * @param parentCode the parent concept code
   * @param childCode the child concept code
   */
  private static void addParentChildEdges(
      Graph<String, RelationshipEdge> graph, String parentCode, String childCode) {

    // add edge from parent to child
    graph.addEdge(
        parentCode,
        childCode,
        new RelationshipEdge(RelationshipEdge.RelationshipType.CHILD, parentCode, childCode));
    // add edge from child to parent
    graph.addEdge(
        childCode,
        parentCode,
        new RelationshipEdge(RelationshipEdge.RelationshipType.PARENT, childCode, parentCode));
  }

  /**
   * Recursively adds edges for a concept and its descendants. Processes both hierarchical nesting
   * and explicit parent/child properties.
   *
   * @param graph the graph to add edges to
   * @param concept the concept to process
   */
  private static void addEdges(
      Graph<String, RelationshipEdge> graph, CodeSystem.ConceptDefinitionComponent concept) {
    if (!concept.hasCode()) {
      log.error(
          "Skipping edge - Concept has no code : code --> {}, display --> {}",
          concept.getDisplay(),
          concept.getCode());
      return;
    }
    String parentCode = concept.getCode();

    // check if we have a parent or child property
    List<CodeSystem.ConceptPropertyComponent> parentChildProps =
        concept.getProperty().stream()
            .filter(prop -> prop.getCode().equals("parent") || prop.getCode().equals("child"))
            .toList();

    if (!parentChildProps.isEmpty()) {
      parentChildProps.forEach(
          prop -> {
            if (prop.getCode().equals("parent")) {
              String targetCode = prop.getValueCodeType().getCode();
              addParentChildEdges(graph, targetCode, parentCode);
            } else if (prop.getCode().equals("child")) {
              String targetCode = prop.getValueCodeType().getCode();
              addParentChildEdges(graph, parentCode, targetCode);
            }
          });
    }

    for (CodeSystem.ConceptDefinitionComponent childConcept : concept.getConcept()) {
      String childCode = childConcept.getCode();
      addParentChildEdges(graph, parentCode, childCode);
      addEdges(graph, childConcept); // Recursively add edges for subconcepts
    }
  }

  /**
   * Performs depth-first search starting from a vertex, following edges of the specified type.
   *
   * @param vertex the starting vertex
   * @param relationshipType the type of edges to follow
   * @return set of all reachable vertices including the starting vertex
   */
  private Set<String> depthFirstSearch(
      String vertex, RelationshipEdge.RelationshipType relationshipType) {

    Set<String> resultSet = new HashSet<>();
    DepthFirstIterator<String, RelationshipEdge> iterator =
        new DepthFirstIterator<>(parentChildGraph, vertex) {
          @Override
          protected Set<RelationshipEdge> selectOutgoingEdges(String vertex) {
            return parentChildGraph.outgoingEdgesOf(vertex).stream()
                .filter(
                    relationshipEdge ->
                        relationshipEdge.getRelationshipType().equals(relationshipType))
                .collect(Collectors.toSet());
          }
        };

    while (iterator.hasNext()) {
      resultSet.add(iterator.next());
    }

    return resultSet;
  }

  /**
   * Finds all leaf concepts (concepts with no children).
   *
   * @return set of concept codes representing leaf nodes
   */
  public Set<String> getLeaves() {
    Set<String> results = new HashSet<>();

    for (String vertex : parentChildGraph.vertexSet()) {
      boolean isLeaf =
          parentChildGraph.outgoingEdgesOf(vertex).stream()
              .noneMatch(
                  relationshipEdge ->
                      relationshipEdge
                          .getRelationshipType()
                          .equals(RelationshipEdge.RelationshipType.CHILD));
      if (isLeaf) {
        results.add(vertex);
      }
    }
    return results;
  }

  /**
   * Finds all root concepts (concepts with no parents).
   *
   * @return set of concept codes representing root nodes
   */
  public Set<String> getRoots() {
    Set<String> results = new HashSet<>();

    for (String vertex : parentChildGraph.vertexSet()) {
      boolean isRoot =
          parentChildGraph.outgoingEdgesOf(vertex).stream()
              .noneMatch(
                  relationshipEdge ->
                      relationshipEdge
                          .getRelationshipType()
                          .equals(RelationshipEdge.RelationshipType.PARENT));
      if (isRoot) {
        results.add(vertex);
      }
    }
    return results;
  }

  /**
   * Returns concepts filtered by the existence (or absence) of parent or child relationships.
   *
   * <ul>
   *   <li>{@code child=true, exists=true} — concepts that have at least one child (non-leaf)
   *   <li>{@code child=true, exists=false} — concepts that have no children (leaves)
   *   <li>{@code child=false, exists=true} — concepts that have at least one parent (non-root)
   *   <li>{@code child=false, exists=false} — concepts that have no parent (roots)
   * </ul>
   *
   * @param child whether the filter refers to child relationships
   * @param exists whether to return concepts where the relationship exists or does not exist
   * @return the set of concept codes matching the criteria
   */
  public Set<String> getConceptsByRelationshipExistence(boolean child, boolean exists) {
    if (child && exists) {
      Set<String> result = new HashSet<>(vertices);
      result.removeAll(getLeaves());
      return result;
    } else if (child) {
      return getLeaves();
    } else if (exists) {
      Set<String> result = new HashSet<>(vertices);
      result.removeAll(getRoots());
      return result;
    } else {
      return getRoots();
    }
  }

  /**
   * Retrieves all descendants of a concept (children, grandchildren, etc.).
   *
   * @param conceptId the concept code
   * @return set of all descendant concept codes, excluding the concept itself
   */
  public Set<String> getAllDescendants(String conceptId) {
    Set<String> results = depthFirstSearch(conceptId, RelationshipEdge.RelationshipType.CHILD);
    results.remove(conceptId);
    return results;
  }

  /**
   * Retrieves all ancestors of a concept (parents, grandparents, etc.).
   *
   * @param conceptId the concept code
   * @return set of all ancestor concept codes, excluding the concept itself
   */
  public Set<String> getAllAncestors(String conceptId) {
    Set<String> results = depthFirstSearch(conceptId, RelationshipEdge.RelationshipType.PARENT);
    results.remove(conceptId);
    return results;
  }

  /**
   * Retrieves all direct children of a concept.
   *
   * @param conceptId the concept code
   * @return set of direct child concept codes
   */
  public Set<String> getAllConceptsWithDirectParent(String conceptId) {

    return parentChildGraph.outgoingEdgesOf(conceptId).stream()
        .filter(
            relationshipEdge ->
                relationshipEdge
                    .getRelationshipType()
                    .equals(RelationshipEdge.RelationshipType.CHILD))
        .map(parentChildGraph::getEdgeTarget)
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves all direct parents of a concept.
   *
   * @param conceptId the concept code
   * @return set of direct parent concept codes
   */
  public Set<String> getAllConceptsWithDirectChild(String conceptId) {

    return parentChildGraph.outgoingEdgesOf(conceptId).stream()
        .filter(
            relationshipEdge ->
                relationshipEdge
                    .getRelationshipType()
                    .equals(RelationshipEdge.RelationshipType.PARENT))
        .map(parentChildGraph::getEdgeTarget)
        .collect(Collectors.toSet());
  }
}
