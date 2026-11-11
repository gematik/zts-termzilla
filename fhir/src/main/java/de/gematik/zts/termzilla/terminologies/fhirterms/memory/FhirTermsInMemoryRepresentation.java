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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_REDUCED_RESOURCE;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.DataIndex;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.FhirModelIndexWriter;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.exceptions.FhirTermsInitializationExceptionException;
import de.gematik.zts.termzilla.terminologies.fhirterms.graph.FhirTermGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.Scope;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.TRKey;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader.FhirTermsResourceLoader;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cm.ConceptMapHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.ValueSetHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;

@Getter
@Slf4j
public class FhirTermsInMemoryRepresentation {

  // --------------------------------------------------------------------------------
  // Resource-specific data structures
  // --------------------------------------------------------------------------------

  // Default-Scope for all Resources (e.g. 'fhir')
  private final String defaultScope;

  // Map to easily find all scopes
  private final Map<String, Scope> scopeMap = new HashMap<>();

  // Simple mapping of generated IDs to the corresponding resources
  private final Map<String, MetadataResource> idToResourceMap = new HashMap<>();

  // MultiValuedMap to store all Graph representations for all CodeSystem versions
  private final Map<String, FhirTermGraphRepresentation> codeSystemVersionsGraphs = new HashMap<>();

  private final String releaseDataHash;

  // --------------------------------------------------------------------------------
  // Concept-specific data structures
  // --------------------------------------------------------------------------------

  // Lucene index for searching concepts
  private DataIndex conceptIndex;
  @Getter private IndexSearcher luceneSearcher;

  // TODO: Find out what this map was intended for; currently it only consumes unnecessary memory.
  // MultiValuedMap mapping concept codes to CodeSystems
  // private final MultiValuedMap<String, CodeSystem> codeSystemConceptCodeMap =
  // new ArrayListValuedHashMap<>();

  // --------------------------------------------------------------------------------
  // Constructor
  // --------------------------------------------------------------------------------

  public FhirTermsInMemoryRepresentation(FhirConfigProperties properties, String defaultScope)
      throws InvalidConfigurationException {

    // Set the default scope according to the external configuration
    this.defaultScope = defaultScope;

    log.info(
        "######################################################################################");
    log.info("Generating FhirTerms In-Memory-Model");

    // Create the Lucene index
    log.info("Creating empty Lucene Index");
    createLuceneIndex(properties.getFhirTermsIndex());

    // Load resources from the FHIR packages in the input directory. The result is enriched
    // resources with new IDs and additional metadata (e.g. implicit package scopes). The resources
    // are stored in idToResourceMap.
    log.info("Loading CodeSystems/ValueSets/ConceptMaps from file system");
    FhirTermsResourceLoader loader = new FhirTermsResourceLoader(properties, this);
    loader.load();

    log.info("Applying default scope to Resources");
    applyDefaultScopeToResources();

    // Initialize the set of all supported scopes
    log.info("Initializing scope access structures");
    initializeScopes();

    log.info("Initializing stripped versions of all Resources for search operations");
    initializeStrippedResourceVersions();

    log.info("Initializing Lucene Index - Terminology Metadata Search Index");
    initializeTerminologyMetadataSearchIndex();

    log.info("Initializing Lucene Index - CodeSystem Concept Search Index");
    initializeCodeSystemConceptSearchIndex();

    log.info("Initializing graph representations");
    initializeCodeSystemGraphRepresentations();

    log.info("Initializing ValueSet representations");
    initializeValueSetRepresentations();

    log.info("Initializing ConceptMap representations");
    initializeConceptMapRepresentations();

    log.info("FhirTerms In-Memory-Model initialized!");

    releaseDataHash =
        loader.calculateReleaseFileHash(properties.getFhirTermsPackageFolderLocation());
  }

  // --------------------------------------------------------------------------------
  // Methods for initializing the data structures
  // --------------------------------------------------------------------------------

  /**
   * Adds the defaultScope to the latest instances of a resource version. This is necessary because
   * patches, e.g. to ICD-10-GM (corrigenda), can result in multiple instances of the same version
   * being in circulation.
   */
  private void applyDefaultScopeToResources() {

    // Sort all resources by packageName and packageVersion (ascending)
    // Assumption: newer packages also contain newer versions of the resources
    ArrayList<MetadataResource> sortedList = new ArrayList<>(idToResourceMap.values());
    sortedList.sort(new ResourcePackageVersionComparator());

    // Iterate over the sorted list and add resources to a map keyed by URL and version. As a
    // result, the last resource added should always be the latest, replacing older instances of
    // resources with the same version.
    Map<TRKey, MetadataResource> defaultScopedResourceMap = new HashMap<>();
    for (MetadataResource resource : sortedList) {
      TRKey key = new TRKey(resource);
      defaultScopedResourceMap.put(key, resource);
    }

    // Set the defaultScope for all remaining resources
    for (MetadataResource resource : defaultScopedResourceMap.values()) {
      // Load the scope set from the resource's user data
      Set<String> scopeSet =
          (Set<String>) resource.getUserData(ResourceUserData.USERDATA_SCOPE_SET);
      // Create a new scope set if the resource does not yet have one
      if (scopeSet == null) {
        scopeSet = new HashSet<>();
        resource.setUserData(ResourceUserData.USERDATA_SCOPE_SET, scopeSet);
      }
      // Add the defaultScope
      scopeSet.add(defaultScope);
    }
  }

  /**
   * Initializes the ScopeMap (all scopes) based on the scope information attached to the resources
   * (from the user data).
   */
  private void initializeScopes() {

    // Always initialize the defaultScope in case no resources were loaded unexpectedly
    scopeMap.computeIfAbsent(defaultScope, Scope::new);

    // Iterate over all loaded resources
    for (MetadataResource resource : idToResourceMap.values()) {

      // Extract the scopes from the resource's user data
      Set<String> scopeSet =
          (Set<String>) resource.getUserData(ResourceUserData.USERDATA_SCOPE_SET);

      if (scopeSet != null) {
        // Iterate over all scopes and create or update the Scope objects
        for (String scopeId : scopeSet) {
          scopeMap.computeIfAbsent(scopeId, Scope::new).addTerminologyResource(resource);
        }
      }
    }

    // Iterate over all scopes and sort/configure the Scope objects
    for (Scope scope : scopeMap.values()) {
      scope.configureTerminologyResources();
    }
  }

  /**
   * Initializes the Lucene index for concepts. The index is created in the specified directory.
   * If the index already exists, it is overwritten.
   *
   * @param indexLocation directory in which to create the index
   */
  private void createLuceneIndex(Path indexLocation) {

    conceptIndex = new DataIndex(indexLocation);
    var conceptWriter = conceptIndex.createFhirModelIndexWriter(IndexWriterConfig.OpenMode.CREATE);
    conceptWriter.close();
  }

  /** Initializes the Lucene index for metadata searches */
  private void initializeTerminologyMetadataSearchIndex() {

    try (var writer = conceptIndex.createFhirModelIndexWriter(IndexWriterConfig.OpenMode.APPEND)) {

      idToResourceMap.values().forEach(resource -> indexResourceMetadata(writer, resource));

    } catch (Exception e) {
      log.error("Error while initializing Terminology Metadata Search Index", e);
    }

    // Prepare the searcher
    try {
      luceneSearcher = conceptIndex.createSearcher();
    } catch (IOException e) {
      throw new FhirTermsInitializationExceptionException("Failed to create IndexSearcher", e);
    }
  }

  /** Initializes the Lucene query structure for concepts */
  private void initializeCodeSystemConceptSearchIndex() {

    try (var writer = conceptIndex.createFhirModelIndexWriter(IndexWriterConfig.OpenMode.APPEND)) {

      idToResourceMap
          .values()
          .forEach(
              resource -> {
                if (resource.getResourceType() == ResourceType.CodeSystem) {
                  CodeSystem codeSystem = (CodeSystem) resource;
                  log.debug("Indexing concepts of {}", codeSystem.getUrl());
                  indexCodeSystemConcepts(writer, codeSystem, codeSystem.getConcept());
                }
              });

    } catch (Exception e) {
      log.error("Error while initializing CodeSystem Concept Search Index", e);
    }

    // Prepare the searcher
    try {
      luceneSearcher = conceptIndex.createSearcher();
    } catch (IOException e) {
      throw new FhirTermsInitializationExceptionException("Failed to create IndexSearcher", e);
    }
  }

  /**
   * Indexes the metadata of a resource (e.g. CodeSystem, ValueSet, ConceptMap).
   *
   * @param writer IndexWriter used for Lucene indexing
   * @param resource FHIR resource
   */
  private void indexResourceMetadata(FhirModelIndexWriter writer, Resource resource) {
    writer.indexResourceMetadata(resource);
  }

  /**
   * Recursively indexes concepts in a CodeSystem.
   *
   * @param writer IndexWriter used for Lucene indexing
   * @param codeSystem CodeSystem resource
   * @param conceptList list of concepts
   */
  private void indexCodeSystemConcepts(
      FhirModelIndexWriter writer,
      CodeSystem codeSystem,
      List<CodeSystem.ConceptDefinitionComponent> conceptList) {

    conceptList.forEach(
        concept -> {
          writer.indexConcept(concept, codeSystem);

          // Recurse to handle nested CodeSystem resources as well
          if (concept.hasConcept()) {
            indexCodeSystemConcepts(writer, codeSystem, concept.getConcept());
          }
        });
  }

  /** Initializes the graph representations for all CodeSystem versions */
  private void initializeCodeSystemGraphRepresentations() {
    idToResourceMap
        .values()
        .forEach(
            resource -> {
              if (resource.getResourceType() == ResourceType.CodeSystem) {
                CodeSystem codeSystem = (CodeSystem) resource;
                FhirTermGraphRepresentation graphRepresentation =
                    new FhirTermGraphRepresentation(codeSystem);
                codeSystemVersionsGraphs.put(getIdForCodeSystem(codeSystem), graphRepresentation);
              }
            });
  }

  private void initializeStrippedResourceVersions() {

    idToResourceMap.values().stream()
        .forEach(
            resource -> {
              switch (resource.getResourceType()) {
                case CodeSystem -> {
                  CodeSystem codeSystem = (CodeSystem) resource;
                  CodeSystem reducedVersion = CodeSystemHelper.createStrippedVersion(codeSystem);
                  codeSystem.setUserData(USERDATA_REDUCED_RESOURCE, reducedVersion);
                }
                case ValueSet -> {
                  ValueSet valueSet = (ValueSet) resource;
                  ValueSet reducedVersion = ValueSetHelper.createStrippedVersion(valueSet);
                  valueSet.setUserData(USERDATA_REDUCED_RESOURCE, reducedVersion);
                }
                case ConceptMap -> {
                  ConceptMap conceptMap = (ConceptMap) resource;
                  ConceptMap reducedVersion = ConceptMapHelper.createStrippedVersion(conceptMap);
                  conceptMap.setUserData(USERDATA_REDUCED_RESOURCE, reducedVersion);
                }
                default -> { // no action needed
                }
              }
            });
  }

  // TODO: Ensure that we do not load ValueSets without a ValueSet definition. If I remember
  // correctly, the definition is optional.

  /** Initializes the ValueSet representation by removing the expansion component */
  private void initializeValueSetRepresentations() {

    idToResourceMap.values().stream()
        .filter(ValueSet.class::isInstance)
        .forEach(
            resource -> {
              ValueSet valueSet = (ValueSet) resource;
              valueSet.setExpansion(null);
            });
  }

  /** Initializes the ConceptMap representation; nothing needs to be done here currently */
  private void initializeConceptMapRepresentations() {
    log.debug("Initialize ConceptMap Representation");
  }

  /**
   * Finds a CodeSystem ID using its canonical URL and version, falling back to the identifier,
   * which may itself be indexed.
   *
   * @param codeSystem reference to the CodeSystem
   * @return graph representation of the CodeSystem
   */
  private String getIdForCodeSystem(CodeSystem codeSystem) {
    String url = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    if (url != null && !url.isBlank() && version != null && !version.isBlank()) {
      return url + "|" + version;
    }

    return codeSystem.getId();
  }

  /**
   * Finds the graph representation of a CodeSystem using its canonical URL and version, or its ID
   * if those are not available.
   *
   * @param codeSystem reference to the CodeSystem
   * @return graph representation of the CodeSystem
   */
  public FhirTermGraphRepresentation getGraphForCodeSystem(CodeSystem codeSystem) {
    String url = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    if (url != null && !url.isBlank() && version != null && !version.isBlank()) {
      return codeSystemVersionsGraphs.get(url + "|" + version);
    }

    return codeSystemVersionsGraphs.get(codeSystem.getId());
  }

  // --------------------------------------------------------------------------------
  // Generic helper methods for CodeSystems, ValueSets, and ConceptMaps
  // --------------------------------------------------------------------------------

  /**
   * Returns the canonical URLs of all resources of the specified type managed by this
   * TerminologyManager.
   *
   * @param <T> the type of MetadataResource to filter for
   * @param resourceClass the class of the resource type to filter for
   * @return list of canonical URLs for resources of the specified type
   * @throws IllegalArgumentException if resourceClass is null
   */
  public <T extends MetadataResource> List<String> getSupportedCanonicals(Class<T> resourceClass) {
    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }

    Set<String> supportedCanonicals =
        scopeMap.get(defaultScope).getSortedResourceList().stream()
            .filter(resourceClass::isInstance)
            .map(MetadataResource::getUrl)
            .collect(Collectors.toSet());

    return List.copyOf(supportedCanonicals);
  }

  /**
   * Returns all resources of the specified type managed by this TerminologyManager (without scope
   * restriction).
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param mode mode for resource retrieval (complete or reduced)
   * @return list of resources of the specified type
   * @throws IllegalArgumentException if resourceClass or mode is null
   */
  public <T extends MetadataResource> List<T> getResourceList(
      Class<T> resourceClass, ResourceRetrievalMode mode) {
    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    List<T> resources =
        idToResourceMap.values().stream()
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast)
            .toList();

    return applyRetrievalMode(resources, mode);
  }

  /**
   * Returns a single resource of the specified type by its ID and scope. Allows choosing between
   * complete and reduced versions.
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param scope the scope to search in
   * @param id the ID of the resource
   * @param mode mode for resource retrieval (complete or reduced)
   * @return the requested resource
   * @throws UnknownScopeException if the scope is not supported
   * @throws UnsupportedTerminologyException if the resource is not found in the scope or is of
   *     wrong type
   * @throws IllegalArgumentException if resourceClass, scope, id, or mode is null
   */
  public <T extends MetadataResource> T getResourceByIdAndScope(
      Class<T> resourceClass, String scope, String id, ResourceRetrievalMode mode)
      throws UnknownScopeException, UnsupportedTerminologyException {

    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (scope == null) {
      throw new IllegalArgumentException("Scope must not be null");
    }
    if (id == null) {
      throw new IllegalArgumentException("ID must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    // Check if the requested scope is supported
    if (!scopeMap.containsKey(scope)) {
      throw new UnknownScopeException("Scope " + scope + " not supported");
    }

    // Check if the requested ID exists in the scope
    if (!scopeMap.get(scope).getIncludedResourcesById().contains(id)) {
      throw new UnsupportedTerminologyException(
          "Resource with id " + id + " not found in scope " + scope);
    }

    // Retrieve the resource
    MetadataResource resource = idToResourceMap.get(id);

    // Verify the resource is of the expected type
    if (!resourceClass.isInstance(resource)) {
      throw new UnsupportedTerminologyException(
          "Resource with id " + id + " is not of type " + resourceClass.getSimpleName());
    }

    T typedResource = resourceClass.cast(resource);

    return applyRetrievalMode(typedResource, mode);
  }

  /**
   * Searches for a resource of the specified type by its canonical URL and version. Allows choosing
   * between complete and reduced versions.
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param scope the scope to search in
   * @param url the canonical URL of the resource
   * @param version the version of the resource
   * @param mode mode for resource retrieval (complete or reduced)
   * @return the requested resource
   * @throws UnknownScopeException if the scope is not supported
   * @throws UnsupportedTerminologyException if the resource is not found in the scope or is of
   *     wrong type
   * @throws IllegalArgumentException if resourceClass, scope, url, version, or mode is null
   */
  public <T extends MetadataResource> T getResourceByUrlAndVersion(
      Class<T> resourceClass, String scope, String url, String version, ResourceRetrievalMode mode)
      throws UnknownScopeException, UnsupportedTerminologyException {

    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (scope == null) {
      throw new IllegalArgumentException("Scope must not be null");
    }
    if (url == null) {
      throw new IllegalArgumentException("URL must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    // Check if the requested scope is supported
    if (!scopeMap.containsKey(scope)) {
      throw new UnknownScopeException("Scope " + scope + " not supported");
    }

    // Retrieve the matching resource ID from the scope
    String resourceIdPart = scopeMap.get(scope).getResourceMap().get(new TRKey(url, version));
    if (resourceIdPart == null) {
      throw new UnsupportedTerminologyException(
          resourceClass.getSimpleName()
              + " with url "
              + url
              + " and version "
              + version
              + " not supported in scope "
              + scope);
    }

    MetadataResource resource = idToResourceMap.get(resourceIdPart);

    // Verify the resource is of the expected type
    if (!resourceClass.isInstance(resource)) {
      throw new UnsupportedTerminologyException(
          "Resource with url "
              + url
              + " and version "
              + version
              + " is not of type "
              + resourceClass.getSimpleName());
    }

    T typedResource = resourceClass.cast(resource);

    return applyRetrievalMode(typedResource, mode);
  }

  /**
   * Returns all resources of the specified type managed by this TerminologyManager for a specific
   * scope.
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param scope the scope of the request
   * @param mode mode for resource retrieval (complete or reduced)
   * @return list of resources of the specified type within the given scope
   * @throws UnknownScopeException if the scope is not supported
   * @throws IllegalArgumentException if resourceClass, scope, or mode is null
   */
  public <T extends MetadataResource> List<T> getResourceListByScope(
      Class<T> resourceClass, String scope, ResourceRetrievalMode mode)
      throws UnknownScopeException {

    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (scope == null) {
      throw new IllegalArgumentException("Scope must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    // Check if the requested scope is supported
    if (!scopeMap.containsKey(scope)) {
      throw new UnknownScopeException("Scope " + scope + " not supported");
    }

    List<T> resources =
        scopeMap.get(scope).getSortedResourceList().stream()
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast)
            .toList();

    return applyRetrievalMode(resources, mode);
  }

  /**
   * Returns a single resource of the specified type by its ID. Allows choosing between complete and
   * reduced versions.
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param id the ID of the resource
   * @param mode mode for resource retrieval (complete or reduced)
   * @return the requested resource, or null if not found
   * @throws UnsupportedTerminologyException if the resource exists but is of wrong type
   * @throws IllegalArgumentException if resourceClass, id, or mode is null
   */
  public <T extends MetadataResource> T getResourceById(
      Class<T> resourceClass, String id, ResourceRetrievalMode mode)
      throws UnsupportedTerminologyException {

    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (id == null) {
      throw new IllegalArgumentException("ID must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    MetadataResource resource = idToResourceMap.get(id);

    // Return null if resource doesn't exist
    if (resource == null) {
      return null;
    }

    // Verify the resource is of the expected type
    if (!resourceClass.isInstance(resource)) {
      throw new UnsupportedTerminologyException(
          "Resource with id " + id + " is not of type " + resourceClass.getSimpleName());
    }

    T typedResource = resourceClass.cast(resource);

    return applyRetrievalMode(typedResource, mode);
  }

  /**
   * Returns all resources of the specified type with a matching canonical URL within a specific
   * scope.
   *
   * @param <T> the type of MetadataResource to retrieve
   * @param resourceClass the class of the resource type to retrieve
   * @param url the canonical URL to filter by
   * @param scope the scope to search in
   * @param mode mode for resource retrieval (complete or reduced)
   * @return list of resources matching the URL within the given scope
   * @throws UnknownScopeException if the scope is not supported
   * @throws IllegalArgumentException if resourceClass, url, scope, or mode is null
   */
  public <T extends MetadataResource> List<T> getResourceListByUrlAndScope(
      Class<T> resourceClass, String url, String scope, ResourceRetrievalMode mode)
      throws UnknownScopeException {

    if (resourceClass == null) {
      throw new IllegalArgumentException("Resource class must not be null");
    }
    if (url == null) {
      throw new IllegalArgumentException("URL must not be null");
    }
    if (scope == null) {
      throw new IllegalArgumentException("Scope must not be null");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Resource retrieval mode must not be null");
    }

    // Check if the requested scope is supported
    if (!scopeMap.containsKey(scope)) {
      throw new UnknownScopeException("Scope " + scope + " not supported");
    }

    List<T> resources =
        scopeMap.get(scope).getSortedResourceList().stream()
            .filter(resourceClass::isInstance)
            .map(resourceClass::cast)
            .filter(resource -> resource.getUrl().equals(url))
            .toList();

    return applyRetrievalMode(resources, mode);
  }

  /**
   * Applies the retrieval mode to the list of resources.
   *
   * @param <T> the type of MetadataResource
   * @param resources list of resources
   * @param mode retrieval mode (COMPLETE or REDUCED)
   * @return list of resources according to the specified mode
   */
  private <T extends MetadataResource> List<T> applyRetrievalMode(
      List<T> resources, ResourceRetrievalMode mode) {

    return resources.stream().map(resource -> applyRetrievalMode(resource, mode)).toList();
  }

  /**
   * Applies the retrieval mode to a single resource.
   *
   * @param <T> the type of the metadata resource
   * @param resource the resource to process
   * @param mode the retrieval mode (FULL or STRIPPED)
   * @return the resource according to the specified mode
   */
  private <T extends MetadataResource> T applyRetrievalMode(
      T resource, ResourceRetrievalMode mode) {

    return switch (mode) {
      case FULL -> resource;
      case STRIPPED -> getReducedOrOriginal(resource);
    };
  }

  /**
   * Returns the reduced version of the resource if available; otherwise, returns the original.
   *
   * @param resource the resource
   * @return reduced or original version
   */
  private <T extends MetadataResource> T getReducedOrOriginal(T resource) {
    if (resource.hasUserData(USERDATA_REDUCED_RESOURCE)) {
      return (T) resource.getUserData(USERDATA_REDUCED_RESOURCE);
    }
    return resource;
  }
}
