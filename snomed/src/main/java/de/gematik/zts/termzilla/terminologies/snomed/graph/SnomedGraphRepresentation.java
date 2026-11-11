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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.GraphTraversalException;
import de.gematik.zts.termzilla.terminologies.snomed.graph.caching.GraphCacheKeys;
import de.gematik.zts.termzilla.terminologies.snomed.graph.loader.RelationsLoader;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.Graph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 * Graph-based representation of SNOMED relationships for version-aware hierarchy traversal.
 *
 * <p>The graph is initialized from RF2 relationship files and provides traversal helpers for
 * parent/child, ancestor, and descendant queries.
 */
@Slf4j
public class SnomedGraphRepresentation {

  private final Graph<String, RelationshipEdge> versionGraph;
  private final EdgeReversedGraph<String, RelationshipEdge> reversedVersionGraph;

  private final LoadingCache<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>>
      versionNodeDecendantsOrSelfCache;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates the SNOMED graph representation and initializes it from unpacked release files.
   *
   * @param properties SNOMED configuration properties containing input locations
   * @throws InvalidConfigurationException if graph initialization fails due to invalid setup
   */
  public SnomedGraphRepresentation(SnomedConfigProperties properties)
      throws InvalidConfigurationException {

    log.info("");
    log.info(
        "######################################################################################");
    log.info("Generating Graph-Model");

    // Create an empty directed graph.
    versionGraph = getNewDirectedGraph();

    versionNodeDecendantsOrSelfCache = buildDescendantsCache();

    // Initialize the graph with data loaded from release files.
    initializeFromRfFiles(properties.getSnomedUnzipLocation());

    // Initialize the reversed graph for descendant traversal operations.
    reversedVersionGraph = new EdgeReversedGraph<>(versionGraph);
  }

  /**
   * Builds the descendants-or-self cache.
   *
   * @return cache keyed by release version and concept id
   */
  @SuppressWarnings("NullableProblems")
  private LoadingCache<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>>
      buildDescendantsCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(100) // Maximum cache size.
        .build(
            new CacheLoader<>() {
              @Override
              public Set<String> load(GraphCacheKeys.SnomedVersionStringCacheKey key)
                  throws Exception {
                log.debug("Loading descendants-or-self for cache");
                return getAllDescendantsOrSelfInternal(
                    key.cacheValue(), key.snomedReleaseVersion()); // Cacheable traversal method.
              }
            });
  }

  // ----------------------------------------------------------------------------------------
  // Methods that create and initialize the graph
  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new empty directed graph configured for SNOMED relationship modeling.
   *
   * @return new graph instance
   */
  private static Graph<String, RelationshipEdge> getNewDirectedGraph() {
    return GraphTypeBuilder.directed()
        .allowingMultipleEdges(true)
        .allowingSelfLoops(false)
        .vertexClass(String.class)
        .edgeClass(RelationshipEdge.class)
        .buildGraph();
  }

  /**
   * Initializes the graph from the unpacked SNOMED RF2 release files.
   *
   * @param snomedUnzipLocation path to the directory containing unpacked release files
   * @throws InvalidConfigurationException if required files are missing or loading fails
   */
  private void initializeFromRfFiles(Path snomedUnzipLocation)
      throws InvalidConfigurationException {

    log.info("Initializing Graph-Model from unpacked Release Files in: {}", snomedUnzipLocation);

    String relationshipsFile;

    try {
      log.info("Trying to load SNOMED-CT from {}", snomedUnzipLocation);
      if (!Files.exists(snomedUnzipLocation)) {
        throw new InvalidConfigurationException(
            "SNOMED unzip Directory not found. Please check the configuration");
      }

      relationshipsFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_RELATIONSHIPS_PATTERN)
              .toAbsolutePath()
              .toString();

      // Create Graphs
      long start = System.currentTimeMillis();
      RelationsLoader loader = new RelationsLoader();
      loader.load(relationshipsFile, versionGraph);

      // Seal all edges to prevent accidental mutation during query operations
      versionGraph.edgeSet().forEach(RelationshipEdge::seal);

      long stop = System.currentTimeMillis();
      log.info("Duration to load SNOMED CT artifacts into Graph-Model: {} ms", (stop - start));

    } catch (Exception e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  // ----------------------------------------------------------------------------------------
  // Methods that provide graph query capabilities
  // ----------------------------------------------------------------------------------------

  /**
   * Checks whether the concept has at least one active relationship in the given release version.
   *
   * @param conceptId SNOMED concept id
   * @param version SNOMED release version
   * @return {@code true} if at least one active relationship exists, otherwise {@code false}
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   */
  public boolean hasActiveRelations(String conceptId, SnomedReleaseVersion version)
      throws UnknownConceptException {
    return hasActiveRelations(versionGraph, conceptId, version);
  }

  /**
   * Checks whether the concept has at least one active relationship in the provided graph.
   *
   * @param graph graph that stores relationships
   * @param conceptId SNOMED concept id
   * @param version SNOMED release version
   * @return {@code true} if at least one active relationship exists for the concept
   */
  private boolean hasActiveRelations(
      Graph<String, RelationshipEdge> graph, String conceptId, SnomedReleaseVersion version)
      throws UnknownConceptException {

    // Modules relevant for this release version.
    Set<String> relevantModules = version.getRelevantModuleVersions().keySet();

    // Load all incoming and outgoing edges.
    Set<RelationshipEdge> edges = executeGraphOperation(conceptId, () -> graph.edgesOf(conceptId));

    return edges.stream()
        // Keep only edges that belong to relevant modules.
        .filter(edge -> relevantModules.contains(edge.getModuleId()))
        // Keep edges that are active for the selected module version.
        .anyMatch(
            edge ->
                edge.isActive(
                    Long.parseLong(version.getRelevantModuleVersions().get(edge.getModuleId()))));
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns concept ids that have no parents in the given release version.
   *
   * <p>This operation is currently not implemented. Originally this method was intended to select
   * the root concept(s) of the hierarchy, but also inactive concepts have no relationships and
   * would be classified as root concepts by this method.
   *
   * @param releaseVersion SNOMED release version
   * @return set of concept ids without parents
   * @throws NotImplementedException always, because this method is not implemented yet
   */
  public Set<String> getConceptsWithNoParents(SnomedReleaseVersion releaseVersion)
      throws NotImplementedException {
    return getConceptsWithNoParents(versionGraph, releaseVersion);
  }

  /**
   * Returns concept ids that have no parents in the provided graph and release version.
   *
   * @param graph graph that stores relationships
   * @param version SNOMED release version
   * @return set of concept ids without parents
   * @throws NotImplementedException always, because this method is not implemented yet
   */
  @SuppressWarnings({"java:S1172", "java:S1854", "java:S1481"})
  // unused parameters for now, as the method is not implemented yet
  private Set<String> getConceptsWithNoParents(
      Graph<String, RelationshipEdge> graph, SnomedReleaseVersion version)
      throws NotImplementedException {

    // Modules relevant for this release version.
    Set<String> relevantModules = version.getRelevantModuleVersions().keySet();

    throw new NotImplementedException("getConceptsWithNoParents not implemented yet");
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns direct parent concept ids for the given concept and release version.
   *
   * @param conceptId SNOMED concept id
   * @param releaseVersion SNOMED release version
   * @return set of direct parent concept ids
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   */
  public Set<String> getDirectParents(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    return getDirectParents(versionGraph, conceptId, releaseVersion);
  }

  /**
   * Returns direct parent concept ids using the provided relationship graph.
   *
   * @param graph graph that stores relationships
   * @param conceptId SNOMED concept id
   * @param version SNOMED release version
   * @return set of direct parent concept ids
   */
  private Set<String> getDirectParents(
      Graph<String, RelationshipEdge> graph, String conceptId, SnomedReleaseVersion version)
      throws UnknownConceptException {

    // Modules relevant for this release version.
    Set<String> relevantModules = version.getRelevantModuleVersions().keySet();

    // For an 'is-a' relation, parent concepts are targets of outgoing edges.
    Set<RelationshipEdge> outgoingEdges =
        executeGraphOperation(conceptId, () -> graph.outgoingEdgesOf(conceptId));

    // Result set containing concept ids of direct parents.
    HashSet<String> directAncestors = new HashSet<>();

    outgoingEdges.stream()
        // Keep only edges that belong to relevant modules.
        .filter(edge -> relevantModules.contains(edge.getModuleId()))
        // Keep edges that are active for the selected module version.
        .filter(
            edge ->
                edge.isActive(
                    Long.parseLong(version.getRelevantModuleVersions().get(edge.getModuleId()))))
        .forEach(edge -> directAncestors.add(graph.getEdgeTarget(edge)));

    return directAncestors;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns direct child concept ids for the given concept and release version - so called
   * "inferred children".
   *
   * @param conceptId SNOMED concept id
   * @param releaseVersion SNOMED release version
   * @return set of direct child concept ids
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   */
  public Set<String> getDirectChildren(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    return getDirectChildren(versionGraph, conceptId, releaseVersion);
  }

  /**
   * Returns direct child concept ids using the provided relationship graph.
   *
   * @param graph graph that stores relationships
   * @param conceptId SNOMED concept id
   * @param version SNOMED release version
   * @return set of direct child concept ids
   */
  private Set<String> getDirectChildren(
      Graph<String, RelationshipEdge> graph, String conceptId, SnomedReleaseVersion version)
      throws UnknownConceptException {

    // Modules relevant for this release version.
    Set<String> relevantModules = version.getRelevantModuleVersions().keySet();

    // For an 'is-a' relation, child concepts are sources of incoming edges.
    Set<RelationshipEdge> incomingEdges =
        executeGraphOperation(conceptId, () -> graph.incomingEdgesOf(conceptId));

    // Result set containing concept ids of direct children.
    HashSet<String> directDescendant = new HashSet<>();

    incomingEdges.stream()
        // Keep only edges that belong to relevant modules.
        .filter(edge -> relevantModules.contains(edge.getModuleId()))
        // Keep edges that are active for the selected module version.
        .filter(
            edge ->
                edge.isActive(
                    Long.parseLong(version.getRelevantModuleVersions().get(edge.getModuleId()))))
        .forEach(edge -> directDescendant.add(graph.getEdgeSource(edge)));

    return directDescendant;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns the given concept and all of its descendants for the selected SNOMED release version.
   *
   * <p>The result contains the transitive closure of inferred child relationships, not only direct
   * children but also descendants at arbitrary depth. The returned set always includes the provided
   * {@code conceptId} itself.
   *
   * <p>Results are cached per pair of release version and concept id to speed up repeated
   * traversals.
   *
   * @param conceptId SNOMED concept id used as traversal root
   * @param releaseVersion SNOMED release version that defines active relationships
   * @return set containing the concept itself and all inferred descendants
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   * @throws GraphTraversalException if cached traversal computation fails
   */
  public Set<String> getAllDescendantsOrSelf(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    GraphCacheKeys.SnomedVersionStringCacheKey key =
        new GraphCacheKeys.SnomedVersionStringCacheKey(releaseVersion, conceptId);
    // Return the cached result, or compute and cache it.
    try {
      return versionNodeDecendantsOrSelfCache.get(key);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof UnknownConceptException unknownConceptException) {
        throw unknownConceptException;
      }
      throw new GraphTraversalException(
          "Failed to compute descendants-or-self for concept '" + conceptId + "'", e);
    }
  }

  /**
   * Returns all descendants of the given concept for the selected SNOMED release version.
   *
   * <p>The traversal follows inferred child relationships transitively, so the result contains not
   * only direct children but also deeper descendants. In contrast to {@link
   * #getAllDescendantsOrSelf(String, SnomedReleaseVersion)}, the root concept itself is removed
   * from the returned set.
   *
   * <p>Results are obtained via the same descendants-or-self cache and then post-processed to
   * exclude {@code conceptId}.
   *
   * @param conceptId SNOMED concept id used as traversal root
   * @param releaseVersion SNOMED release version that defines active relationships
   * @return set of inferred descendant concept ids without the root concept
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   * @throws GraphTraversalException if cached traversal computation fails
   */
  public Set<String> getAllDescendants(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    GraphCacheKeys.SnomedVersionStringCacheKey key =
        new GraphCacheKeys.SnomedVersionStringCacheKey(releaseVersion, conceptId);
    // Return the cached result, or compute and cache it.
    try {
      Set<String> returnSet = new HashSet<>(versionNodeDecendantsOrSelfCache.get(key));
      // Remove the concept itself.
      returnSet.remove(conceptId);
      return returnSet;
    } catch (ExecutionException e) {
      if (e.getCause() instanceof UnknownConceptException unknownConceptException) {
        throw unknownConceptException;
      }
      throw new GraphTraversalException(
          "Failed to compute descendants for concept '" + conceptId + "'", e);
    }
  }

  /**
   * Computes descendants-or-self for one concept without directly interacting with the public cache
   * API.
   *
   * <p>This internal helper performs a full inferred descendant traversal based on the reversed
   * graph representation and includes the root concept itself in the result. It is used as the
   * underlying computation function for the cache loader.
   *
   * @param conceptId SNOMED concept id used as traversal root
   * @param releaseVersion SNOMED release version that defines active relationships
   * @return set containing the concept itself and all inferred descendants
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   */
  private Set<String> getAllDescendantsOrSelfInternal(
      String conceptId, SnomedReleaseVersion releaseVersion) throws UnknownConceptException {
    return executeGraphOperation(
        conceptId,
        () ->
            GraphTraversalService.traverseDescendants(
                reversedVersionGraph, conceptId, releaseVersion, true));
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns the given concept and all of its ancestors for the selected SNOMED release version.
   *
   * <p>The result contains the transitive closure of inferred parent relationships, not only direct
   * parents but also all higher hierarchy levels. The returned set includes the provided {@code
   * conceptId} itself.
   *
   * @param conceptId SNOMED concept id used as traversal root
   * @param releaseVersion SNOMED release version that defines active relationships
   * @return set containing the concept itself and all inferred ancestors
   * @throws UnknownConceptException if {@code conceptId} is unknown in the graph
   */
  public Set<String> getAllAncestorsOrSelf(String conceptId, SnomedReleaseVersion releaseVersion)
      throws UnknownConceptException {
    return executeGraphOperation(
        conceptId,
        () ->
            GraphTraversalService.traverseAncestors(versionGraph, conceptId, releaseVersion, true));
  }

  private <T> T executeGraphOperation(String conceptId, Supplier<T> graphOperation)
      throws UnknownConceptException {
    try {
      return graphOperation.get();
    } catch (IllegalArgumentException _) {
      throw new UnknownConceptException(
          "Unknown SNOMED concept id in graph traversal: " + conceptId);
    }
  }

  // ----------------------------------------------------------------------------------------
  // ----------------------------------------------------------------------------------------

}
