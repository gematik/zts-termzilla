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
package de.gematik.zts.termzilla.terminologies.snomed;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.snomed.provider.vs.expand.SnomedVsExplicitExpandOperation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * SNOMED-specific {@link ValueSetSvcInterface} implementation.
 *
 * <p>The service keeps track of supported explicit and implicit ValueSet canonical URIs and
 * delegates expansion-related operations to dedicated SNOMED operation classes.
 */
@Slf4j
public class SnomedValueSetService implements ValueSetSvcInterface {

  @Getter private final Snomed snomed;
  @Getter private final SnomedTerminologyManager snomedTerminologyManager;

  private final Set<String> explicitVsUriSet = new HashSet<>();
  private final Set<String> implicitVsUriSet = new HashSet<>();

  // Operations
  SnomedVsExplicitExpandOperation snomedVsExplicitExpandOperation;

  // --------------------------------------------------------------------------------

  /**
   * Creates a SNOMED ValueSet service backed by the given terminology manager.
   *
   * @param snomedTerminologyManager manager that provides access to SNOMED resources and services
   */
  public SnomedValueSetService(SnomedTerminologyManager snomedTerminologyManager) {

    this.snomedTerminologyManager = snomedTerminologyManager;
    this.snomed = snomedTerminologyManager.getSnomed();

    // Initialize Operations
    this.snomedVsExplicitExpandOperation = new SnomedVsExplicitExpandOperation(this);
  }

  // ----------------------------------------------------------------------------------------
  // Initialization methods
  // ----------------------------------------------------------------------------------------

  /**
   * Registers explicit ValueSet canonical URIs supported by this service.
   *
   * @param uriSet canonical URIs of explicitly managed ValueSets
   */
  public void registerSupportedExplicitValueSetUri(Set<String> uriSet) {
    log.debug("Registering explicit ValueSets with URIs: {}", uriSet);
    explicitVsUriSet.addAll(uriSet);
  }

  /**
   * Registers URI patterns for implicitly defined ValueSets supported by this service.
   *
   * @param uriPatternSet URI patterns for implicit ValueSet handling
   */
  public void registerSupportedImplicitValueSetUriPatterns(Set<String> uriPatternSet) {
    log.debug("Registering implicit ValueSets with URI Patterns: {}", uriPatternSet);
    implicitVsUriSet.addAll(uriPatternSet);
  }

  // --------------------------------------------------------------------------------

  /**
   * Checks whether a ValueSet identified by canonical URL and version is supported in a scope.
   *
   * @param scope request scope
   * @param url canonical URL of the ValueSet
   * @param version ValueSet version
   * @return {@code true} if the ValueSet is supported, otherwise {@code false}
   * @throws UnknownScopeException if the scope is not known
   */
  @Override
  public boolean isValueSetSupported(String scope, String url, String version)
      throws UnknownScopeException {
    // TODO: Implement for SNOMED.
    return false;
  }

  /**
   * Returns all supported ValueSet canonical URLs known to this service.
   *
   * @return list of supported canonical URLs
   */
  @Override
  public List<String> getSupportedValueSetCanonicalList() {
    // TODO: Implement for SNOMED.
    return new ArrayList<>();
  }

  /**
   * Returns all ValueSets managed by the underlying terminology manager.
   *
   * <p>No scope filtering is applied, so ValueSets from all scopes may be returned.
   *
   * @return all managed ValueSets across scopes
   */
  @Override
  public List<ValueSet> getValueSetList() {
    // TODO: Implement for SNOMED.
    return List.of();
  }

  /**
   * Returns a single ValueSet by id within the given scope.
   *
   * @param scope request scope
   * @param theId id of the requested ValueSet
   * @param retrievalMode controls whether reduced or full resource content is loaded
   * @return the matching ValueSet
   */
  @Override
  public ValueSet getValueSet(String scope, IdType theId, ResourceRetrievalMode retrievalMode) {
    // TODO: Implement for SNOMED.
    return null;
  }

  /**
   * Returns a single ValueSet by id within the given scope.
   *
   * <p>Delegates to {@link #getValueSet(String, IdType, ResourceRetrievalMode)} with {@link
   * ResourceRetrievalMode#FULL}.
   *
   * @param scope request scope
   * @param theId id of the requested ValueSet
   * @return the matching ValueSet
   */
  @Override
  public ValueSet getValueSet(String scope, IdType theId) {
    // TODO: Implement for SNOMED.
    return getValueSet(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * Returns ValueSets for a canonical URL, filtered by the given scope.
   *
   * <p>Multiple versions can exist for the same canonical URL, so a list is returned.
   *
   * @param scope request scope
   * @param url canonical URL of the ValueSets
   * @return list of ValueSets matching URL and scope
   */
  @Override
  public List<ValueSet> getValueSetList(String scope, String url) {
    // TODO: Implement for SNOMED.
    return List.of();
  }

  /**
   * Returns a ValueSet by canonical URL and version for the given scope.
   *
   * <p>Scope constraints are applied because different scope-specific variants can exist.
   *
   * @param scope request scope
   * @param url canonical URL of the ValueSet
   * @param version ValueSet version
   * @param retrievalMode controls whether reduced or full resource content is loaded
   * @return ValueSet matching URL, version, and scope
   * @throws ResourceNotFoundException if no matching ValueSet exists
   * @throws UnknownScopeException if the scope is not known
   * @throws UnsupportedTerminologyException if terminology-specific handling is not supported
   */
  @Override
  public ValueSet getValueSet(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    // TODO: Implement for SNOMED.
    return null;
  }

  /**
   * Returns a ValueSet by canonical URL and version for the given scope.
   *
   * <p>Delegates to {@link #getValueSet(String, String, String, ResourceRetrievalMode)} with {@link
   * ResourceRetrievalMode#FULL}.
   *
   * @param scope request scope
   * @param url canonical URL of the ValueSet
   * @param version ValueSet version
   * @return ValueSet matching URL, version, and scope
   * @throws ResourceNotFoundException if no matching ValueSet exists
   * @throws UnknownScopeException if the scope is not known
   * @throws UnsupportedTerminologyException if terminology-specific handling is not supported
   */
  @Override
  public ValueSet getValueSet(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {
    // TODO: Implement for SNOMED.
    return getValueSet(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * Searches ValueSets by the given search parameters within a scope.
   *
   * <p>Search parameters follow the FHIR search semantics represented by {@link VsSearchParameter}.
   *
   * @param scope scope to search in
   * @param vsSearchParameter search parameters aligned with the FHIR specification
   * @return list of ids of matching ValueSets
   * @throws NotImplementedException if the search behavior is not fully implemented
   * @throws UnknownScopeException if the scope is not known
   */
  @Override
  public List<IdType> searchValueSet(String scope, VsSearchParameter vsSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    // TODO: Implement for SNOMED.
    return List.of();
  }

  /**
   * Creates a {@link ValueSet.ValueSetExpansionContainsComponent} for one concept.
   *
   * @param scope scope used for expansion
   * @param conceptBaseInfo concept to include in the expansion
   * @param includeDesignations indicates whether designations should be included
   * @param designationParamValueList designation parameters from the request
   * @param displayLanguage preferred display language for returned designations
   * @param languageSelectionParameter language selection rules for designation processing
   * @param explicitConceptReferenceMap map of explicit concept references grouped by include index
   * @return expansion contains component for the concept
   * @throws UnsupportedTerminologyException if terminology-specific handling is not supported
   */
  @Override
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap)
      throws UnsupportedTerminologyException {

    return this.snomedVsExplicitExpandOperation.createExpansionContainsComponent(
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        languageSelectionParameter,
        explicitConceptReferenceMap);
  }

  /**
   * Expands one include or exclude element of a potentially complex ValueSet definition.
   *
   * <p>The origin of the ValueSet is unknown here, so it is treated as provided within the request
   * (see {@link ProcessingContext#provided()}).
   *
   * @param scope scope used for expansion
   * @param include include element from {@code ValueSet.compose.include}
   * @param includeInactiveConcepts whether inactive concepts should be included
   * @param version default version to use when not explicitly provided
   * @param checkVersion version that should be checked for compatibility
   * @param forceVersion version that should be forced for expansion
   * @param filter optional filter expression applied during expansion
   * @return result of the include or exclude expansion
   */
  @Override
  public VsExpandPartResult expandInclude(
      String scope,
      ValueSet.ConceptSetComponent include,
      boolean includeInactiveConcepts,
      String version,
      String checkVersion,
      String forceVersion,
      String filter) {

    // Ohne bekannte Herkunft wird das ValueSet wie ein im Request übergebenes behandelt
    return expandInclude(
        scope,
        include,
        VsExpandIncludeParameter.builder()
            .includeInactiveConcepts(includeInactiveConcepts)
            .version(version)
            .checkVersion(checkVersion)
            .forceVersion(forceVersion)
            .filter(filter)
            .processingContext(ProcessingContext.provided())
            .build());
  }

  /**
   * Expands one include or exclude element of a potentially complex ValueSet definition, taking the
   * origin of the ValueSet into account. In restricted mode, hierarchy-based filters are only
   * available for ValueSets that are part of the service's content.
   *
   * @param scope scope used for expansion
   * @param include include element from {@code ValueSet.compose.include}
   * @param parameter expansion settings and processing context of the ValueSet
   * @return result of the include or exclude expansion
   */
  @Override
  public VsExpandPartResult expandInclude(
      String scope, ValueSet.ConceptSetComponent include, VsExpandIncludeParameter parameter) {
    return this.snomedVsExplicitExpandOperation.expandInclude(include, parameter);
  }
}
