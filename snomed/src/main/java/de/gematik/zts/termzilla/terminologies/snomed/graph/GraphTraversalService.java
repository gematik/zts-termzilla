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

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.traverse.BreadthFirstIterator;

/**
 * Stateless utility for version-aware traversal of the SNOMED CT concept hierarchy graph.
 *
 * <p>All methods filter edges by module relevance and active status for the given {@link
 * SnomedReleaseVersion}, ensuring that only edges valid for that version are followed during
 * traversal.
 */
public final class GraphTraversalService {

  private GraphTraversalService() {}

  /**
   * Traverses all descendants of the given concept in the hierarchy.
   *
   * <p>To find descendants, pass the <em>reversed</em> graph (edges point from parent to child in
   * the reversed view). The BFS follows outgoing edges in the reversed graph, which correspond to
   * incoming IS-A edges in the normal graph.
   *
   * @param reversedGraph the reversed concept hierarchy graph
   * @param startId the concept to start from
   * @param version the release version for edge filtering
   * @param includeSelf whether to include {@code startId} in the result
   * @return the set of descendant concept IDs (and optionally self)
   */
  public static Set<String> traverseDescendants(
      Graph<String, RelationshipEdge> reversedGraph,
      String startId,
      SnomedReleaseVersion version,
      boolean includeSelf) {
    Set<String> result = traverse(reversedGraph, startId, version);
    if (!includeSelf) {
      result.remove(startId);
    }
    return result;
  }

  /**
   * Traverses all ancestors of the given concept in the hierarchy.
   *
   * <p>Pass the normal graph (edges point from child to parent via IS-A). The BFS follows outgoing
   * IS-A edges to walk up the hierarchy.
   *
   * @param graph the concept hierarchy graph
   * @param startId the concept to start from
   * @param version the release version for edge filtering
   * @param includeSelf whether to include {@code startId} in the result
   * @return the set of ancestor concept IDs (and optionally self)
   */
  public static Set<String> traverseAncestors(
      Graph<String, RelationshipEdge> graph,
      String startId,
      SnomedReleaseVersion version,
      boolean includeSelf) {
    Set<String> result = traverse(graph, startId, version);
    if (!includeSelf) {
      result.remove(startId);
    }
    return result;
  }

  /**
   * Finds all vertices in the graph that have no active outgoing edges for the given version.
   *
   * <p>In the normal IS-A graph, these are concepts with no active parent relationships — typically
   * only the SNOMED CT root concept for a valid release version. Inactive concepts whose edges are
   * all inactive for the given version will also appear.
   *
   * @param graph the concept hierarchy graph
   * @param version the release version for edge filtering
   * @return the set of concept IDs that have no active parents in the given version
   */
  public static Set<String> findRoots(
      Graph<String, RelationshipEdge> graph, SnomedReleaseVersion version) {
    Map<String, String> relevantModuleVersions = version.getRelevantModuleVersions();
    Set<String> relevantModules = relevantModuleVersions.keySet();

    return graph.vertexSet().stream()
        .filter(
            vertex ->
                graph.outgoingEdgesOf(vertex).stream()
                    .noneMatch(
                        edge -> isActiveForVersion(edge, relevantModules, relevantModuleVersions)))
        .collect(Collectors.toSet());
  }

  /**
   * Core BFS traversal that filters edges by version relevance and active status.
   *
   * <p>Starting from {@code startId}, this method performs a breadth-first traversal of the given
   * graph, following only edges that belong to a relevant module and are active at that module's
   * effective time for the given release version. The start vertex is always included in the
   * result.
   *
   * @param graph the graph to traverse
   * @param startId the vertex to start from
   * @param version the release version for edge filtering
   * @return the set of reachable vertex IDs (including {@code startId})
   */
  static Set<String> traverse(
      Graph<String, RelationshipEdge> graph, String startId, SnomedReleaseVersion version) {

    Map<String, String> relevantModuleVersions = version.getRelevantModuleVersions();
    Set<String> relevantModules = relevantModuleVersions.keySet();

    Set<String> resultSet = new HashSet<>();
    BreadthFirstIterator<String, RelationshipEdge> iterator =
        new BreadthFirstIterator<>(graph, startId) {
          @Override
          protected Set<RelationshipEdge> selectOutgoingEdges(String vertex) {
            Set<RelationshipEdge> filtered = new HashSet<>();
            graph.outgoingEdgesOf(vertex).stream()
                .filter(edge -> isActiveForVersion(edge, relevantModules, relevantModuleVersions))
                .forEach(filtered::add);
            return filtered;
          }
        };

    while (iterator.hasNext()) {
      resultSet.add(iterator.next());
    }
    return resultSet;
  }

  /**
   * Checks whether the given edge belongs to a relevant module and is active in that module's
   * version.
   */
  private static boolean isActiveForVersion(
      RelationshipEdge edge,
      Set<String> relevantModules,
      Map<String, String> relevantModuleVersions) {
    if (!relevantModules.contains(edge.getModuleId())) {
      return false;
    }
    long moduleEffectiveTime = Long.parseLong(relevantModuleVersions.get(edge.getModuleId()));
    return edge.isActive(moduleEffectiveTime);
  }
}
