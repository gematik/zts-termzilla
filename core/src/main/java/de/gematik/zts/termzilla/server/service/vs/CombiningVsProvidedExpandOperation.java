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
package de.gematik.zts.termzilla.server.service.vs;

import static de.gematik.zts.termzilla.TermzillaConstants.MESSAGE_CODE_SYSTEM_INCLUDE_NOT_SUPPORTED;
import static de.gematik.zts.termzilla.TermzillaConstants.MESSAGE_VALUE_SET_INCLUDE_HAS_NO_SYSTEM;
import static de.gematik.zts.termzilla.TermzillaConstants.MESSAGE_VALUE_SET_INCLUDE_HAS_NO_VERSION;
import static de.gematik.zts.termzilla.TermzillaConstants.MESSAGE_VALUE_SET_INCLUDE_NOT_SUPPORTED;
import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_COMPOSE_PART_TYPE;
import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.constants.FhirExtensionUrls;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.ResourceOrigin;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.common.helper.ExpansionHelper;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import de.gematik.zts.termzilla.generic.MapUtils;
import de.gematik.zts.termzilla.server.cache.ExpansionCacheKey;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.common.ComposePartType;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.CanonicalPair;

/**
 * Utility class providing ValueSet expansion operations for explicitly provided ValueSet resources.
 *
 * <p>This class handles the expansion of {@link ValueSet} resources by processing {@code
 * ValueSet.compose} elements, resolving include/exclude definitions, and building the resulting
 * expansion. It delegates terminology-specific operations to the appropriate {@link
 * TerminologyManager} implementations discovered via SPI.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Processing {@code ValueSet.compose.include} and {@code ValueSet.compose.exclude} elements
 *   <li>Handling system-based and valueSet-based include definitions
 *   <li>Managing version resolution via {@code system-version}, {@code check-system-version}, and
 *       {@code force-system-version} parameters
 *   <li>Building expansion results with paging support ({@code count}, {@code offset})
 *   <li>Caching expansion results for performance optimization
 * </ul>
 *
 * @see CombiningTerminologyService
 * @see VsExpandParameter
 * @see VsExpandPartResult
 */
@Slf4j
public class CombiningVsProvidedExpandOperation {

  // TODO: Move these constants into a centralized configuration.
  public static final int DEFAULT_COUNT_PARAM_VALUE = 100;
  public static final int MAX_COUNT_PARAM_VALUE = 1000000;

  /** Carries shared parameters used while resolving includes/excludes in compose expansion. */
  private record ExpansionResolutionContext(
      String scope,
      Map<String, String> systemVersionMap,
      Map<String, String> checkSystemVersionMap,
      Map<String, String> forceSystemVersionMap,
      String filter,
      boolean includeInactiveConcepts,
      java.util.Set<String> resolutionPath,
      ProcessingContext processingContext) {

    /**
     * Creates a context for the root ValueSet of an expansion.
     *
     * @param scope the expansion scope
     * @param systemVersionMap resolved system-version parameters
     * @param checkSystemVersionMap resolved check-system-version parameters
     * @param forceSystemVersionMap resolved force-system-version parameters
     * @param filter the text filter
     * @param includeInactiveConcepts whether inactive concepts are included
     * @param rootValueSetKey the canonical identity (url|version) of the root ValueSet
     * @param rootOrigin the origin of the root ValueSet
     */
    static ExpansionResolutionContext root(
        String scope,
        Map<String, String> systemVersionMap,
        Map<String, String> checkSystemVersionMap,
        Map<String, String> forceSystemVersionMap,
        String filter,
        boolean includeInactiveConcepts,
        String rootValueSetKey,
        ResourceOrigin rootOrigin) {
      final Set<String> path = new HashSet<>();
      if (rootValueSetKey != null) {
        path.add(rootValueSetKey);
      }
      return new ExpansionResolutionContext(
          scope,
          systemVersionMap,
          checkSystemVersionMap,
          forceSystemVersionMap,
          filter,
          includeInactiveConcepts,
          path,
          new ProcessingContext(rootOrigin));
    }
  }

  /** Carries shared inputs required while constructing the expansion element. */
  private record ExpansionElementContext(
      VsExpandParameter vsExpandParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      long activeConceptCount,
      String scope,
      LanguageSelectionParameter languageSelectionParameter) {}

  /** Carries shared options used when creating {@code expansion.contains} components. */
  private record ContainsCreationContext(
      String scope,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      java.util.Map<String, SupplementRequest> supplements) {}

  // ----------------------------------------------------------------------------------------

  private CombiningVsProvidedExpandOperation() {}

  // ----------------------------------------------------------------------------------------

  /**
   * Expands an explicitly provided ValueSet according to the given parameters.
   *
   * <p>This method processes a directly provided ValueSet resource (not just a reference) by
   * resolving all include and exclude elements from {@code ValueSet.compose} and merging the
   * results into an expansion.
   *
   * @param combiningTerminologyService the terminology service that orchestrates the expansion
   * @param vsExpandParameter parameters controlling the expansion behavior
   * @param scope the scope in which the expansion is performed
   * @param languageSystemDefault default language configured for the system
   * @return the expanded ValueSet with populated {@code ValueSet.expansion} element
   * @throws UnsupportedTerminologyException if a referenced terminology is not supported
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnknownScopeException if the specified scope is not recognized
   * @throws UnknownConceptException if a referenced concept cannot be found
   */
  public static ValueSet expandExplicitValueSet(
      CombiningTerminologyService combiningTerminologyService,
      VsExpandParameter vsExpandParameter,
      String scope,
      LanguageParameter languageSystemDefault)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug(
        "-----> expandExplicitValueSet() called for scope '{}' with parameters: {}",
        scope,
        vsExpandParameter.toString());

    // Validate the input parameters to catch the most serious errors early.
    CombiningVsExpandParameterValidator.validateParameters(vsExpandParameter);

    // Calculate the cache key from the supplied parameters and check whether an expansion has
    // already been generated for this key.
    log.debug("Calculating cacheKey and checking for cached expansion");

    ExpansionCacheKey cacheKey = new ExpansionCacheKey(scope, vsExpandParameter);
    ValueSet vs =
        combiningTerminologyService.getCacheManager().getExpansionCache().getIfPresent(cacheKey);
    if (vs != null) {
      log.debug("Found cached expansion for key {}", cacheKey);
      return vs;
    }

    ValueSet resultVs = prepareResultValueSet(vsExpandParameter);
    boolean includeInactiveConcepts = prepareValueSetForExpansion(resultVs);

    // Resolve and validate any CodeSystem supplements declared via the R4 'valueset-supplement'
    // extension. The resolved supplements are stored on the parameters so that the terminology
    // managers can apply their designations, and are recorded in the expansion parameters.
    resolveSupplements(combiningTerminologyService, resultVs, vsExpandParameter, scope);


    // -----------------------------------------
    // Handling of the 'system-version' parameter
    // "Specifies a version to use for a system, if the value set does not specify which one to use.
    // The format is the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56"
    Map<String, String> systemVersionMap =
        extractXxxSystemVersionParameter(
            vsExpandParameter.getSystemVersionParam(), "system-version");

    // -----------------------------------------
    // Handling of the 'check-system-version' parameter
    // "Edge Case: Specifies a version to use for a system. If a value set specifies a different
    // version, an error is returned instead of the expansion. The format is the same as a canonical
    // URL: [system]|[version] - e.g. http://loinc.org|2.56"
    Map<String, String> checkSystemVersionMap =
        extractXxxSystemVersionParameter(
            vsExpandParameter.getCheckSystemVersionParam(), "check-system-version");

    // -----------------------------------------
    // Handling of the 'force-system-version' parameter
    // "Edge Case: Specifies a version to use for a system. This parameter overrides any specified
    // version in the value set (and any it depends on). The format is the same as a canonical URL:
    // [system]|[version] - e.g. http://loinc.org|2.56. Note that this has obvious safety issues, in
    // that it may result in a value set expansion giving a different list of codes that is both
    // wrong and unsafe, and implementers should only use this capability reluctantly. It primarily
    // exists to deal with situations where specifications have fallen into decay as time passes. If
    // the value is override, the version used SHALL explicitly be represented in the expansion
    // parameters"
    Map<String, String> forceSystemVersionMap =
        extractXxxSystemVersionParameter(
            vsExpandParameter.getForceSystemVersionParam(), "force-system-version");

    LanguageSelectionParameter languageSelectionParameter =
        createLanguageSelectionParameter(resultVs, vsExpandParameter, languageSystemDefault);

    String filter =
        vsExpandParameter.getFilterParam() != null
            ? vsExpandParameter.getFilterParam().getValue()
            : null;
    // TODO: Implement caching?

    // -----------------------------------------
    // The actual expansion starts here.
    VsExpandPartResult finalExpandResult =
        processValueSetCompose(
            combiningTerminologyService,
            resultVs.getCompose(),
            ExpansionResolutionContext.root(
                scope,
                systemVersionMap,
                checkSystemVersionMap,
                forceSystemVersionMap,
                filter,
                includeInactiveConcepts,
                valueSetIdentity(resultVs),
                vsExpandParameter.getResourceOrigin()));


    // Number of active concepts in the result set. This is needed later to return the number of
    // concepts in the request result (paging support).
    long activeConceptCount = countActiveConcepts(finalExpandResult.getConceptBaseInfos());

    // Transfer and enrich the results from the resolved includes in the expansion.
    createExpansionElement(
        combiningTerminologyService,
        resultVs,
        finalExpandResult.getConceptBaseInfos(),
        new ExpansionElementContext(
            vsExpandParameter,
            finalExpandResult.getExplicitConceptReferences(),
            activeConceptCount,
            scope,
            languageSelectionParameter));

    // Remove the ValueSet definition (ValueSet.compose) if requested.
    // Note: By default, the definition is included in the response.
    if (vsExpandParameter.getIncludeDefinitionParam() != null
        && !vsExpandParameter.getIncludeDefinitionParam().isEmpty()
        && !vsExpandParameter.getIncludeDefinitionParam().booleanValue()) {
      resultVs.setCompose(null);
    }

    // Add a unique identifier for the expansion.
    resultVs.getExpansion().setIdentifier("urn:uuid:" + UUID.randomUUID());

    // Add a timestamp.
    resultVs.getExpansion().setTimestamp(new Date());

    // Ensure that the expansion also records which parameters were used to create it.
    ExpansionHelper.addUsedParametersToExpansion(
        vsExpandParameter, resultVs, finalExpandResult.getSystemVersionSelectionStrategy());

    // Record the applied supplements in the expansion parameters using the
    // '[base CodeSystem canonical]#supplement' convention.
    addSupplementParameters(resultVs, vsExpandParameter);

    // Add the result to the cache.
    combiningTerminologyService
        .getCacheManager()
        .getExpansionCache()
        .put(cacheKey, resultVs.copy());

    return resultVs;
  }

  /**
   * Validates the request and returns a copy of the provided ValueSet for in-place expansion.
   *
   * @throws InvalidRequestException if no ValueSet is provided in the expand parameters
   */
  private static ValueSet prepareResultValueSet(VsExpandParameter vsExpandParameter) {
    // Extract the ValueSet from the request parameters. Validation should already ensure that a
    // ValueSet is present, but check again anyway.
    if (vsExpandParameter.getValueSetParam() == null
        || vsExpandParameter.getValueSetParam().isEmpty()) {
      throw new InvalidRequestException("No ValueSet provided for expansion.");
    }
    return vsExpandParameter.getValueSetParam().copy();
  }

  /**
   * Prepares compose-related metadata on the ValueSet and returns inactive-concept handling.
   *
   * <p>This keeps the original preparation order and side effects unchanged.
   */
  private static boolean prepareValueSetForExpansion(ValueSet resultVs)
      throws NotImplementedException {
    // Add the language information to the compose element; it is needed later to determine the
    // language of displays and designations in the ValueSet.
    ValueSet.ValueSetComposeComponent compose = resultVs.getCompose();
    if (compose != null) {
      compose.setUserData(USERDATA_LANGUAGE, resultVs.getLanguage());
    }

    // Remove any existing expansion from the ValueSet because we cannot be sure it was created in
    // the same context (time and parameters).
    if (resultVs.hasExpansion()) {
      log.debug("Removing existing expansion from ValueSet");
      resultVs.setExpansion(null);
    }

    // Information from ValueSet.compose.inactive controls whether inactive concepts are included in
    // the ValueSet. inactive=true means inactive concepts are included. By default, we assume that
    // inactive concepts should be included.
    boolean includeInactiveConcepts = true;
    if (resultVs.getCompose().hasInactive()) {
      includeInactiveConcepts = resultVs.getCompose().getInactive();
    }

    return includeInactiveConcepts;
  }

  /** Creates the language selection context for resolving displays and designations. */
  private static LanguageSelectionParameter createLanguageSelectionParameter(
      ValueSet resultVs,
      VsExpandParameter vsExpandParameter,
      LanguageParameter languageSystemDefault) {

    // -----------------------------------------
    // Prepare the language parameter information needed later to determine the correct displays and
    // designations.

    String displayLanguage = null;
    if (vsExpandParameter.getDisplayLanguageParam() != null
        && !vsExpandParameter.getDisplayLanguageParam().isEmpty()) {
      displayLanguage = vsExpandParameter.getDisplayLanguageParam().getCode();
    }

    LanguageParameter languageDisplayLanguageParameter =
        displayLanguage != null ? LanguageParameter.parse(displayLanguage) : null;

    // Also use a language code specified by the ValueSet, if present.
    LanguageParameter vsLanguageParameter = null;
    if (resultVs.hasLanguage() && LanguageRegistry.isSupported(resultVs.getLanguage())) {
      vsLanguageParameter = LanguageParameter.parse(resultVs.getLanguage());
    }

    return LanguageSelectionParameter.builder()
        .systemDefaultLanguage(languageSystemDefault)
        .acceptLanguageHeader(vsExpandParameter.getLanguageAcceptLanguageHeader())
        .displayLanguageParameter(languageDisplayLanguageParameter)
        .vsLanguage(vsLanguageParameter)
        .build();
  }

  /** Counts active concepts in the expansion result set. */
  private static long countActiveConcepts(SortedSet<ConceptBaseInfo> conceptBaseInfos) {
    long activeConceptCount = 0;
    for (ConceptBaseInfo conceptBaseInfo : conceptBaseInfos) {
      if (conceptBaseInfo.isActive()) {
        activeConceptCount++;
      }
    }
    return activeConceptCount;
  }

  /**
   * Processes the compose element of a ValueSet by expanding all include and exclude definitions.
   *
   * <p>Iterates over all {@code compose.include} elements and unions their expansion results. Then
   * iterates over all {@code compose.exclude} elements and removes their expansion results from the
   * union, as specified by the FHIR R4 composition rules.
   *
   * @param combiningTerminologyService the terminology service for delegation
   * @param compose the ValueSet compose element containing include and exclude definitions
   * @param context shared include/exclude resolution settings
   * @return the combined expansion result after processing all includes and excludes
   * @throws UnsupportedTerminologyException if a referenced terminology is not supported
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnknownScopeException if the specified scope is not recognized
   * @throws UnknownConceptException if a referenced concept cannot be found
   */
  // TODO: Prevent self-references, which would otherwise cause an infinite loop.
  private static VsExpandPartResult processValueSetCompose(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet.ValueSetComposeComponent compose,
      ExpansionResolutionContext context)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Result object; initially, it contains no results.
    VsExpandPartResult vsExpansionResult = new VsExpandPartResult();

    // -------------------------------------------------------------------
    // Iterate over all include elements of ValueSet.compose from here.
    for (ValueSet.ConceptSetComponent include : compose.getInclude()) {

      // Copy the language information to the include user data.
      include.setUserData(USERDATA_LANGUAGE, compose.getUserData(USERDATA_LANGUAGE));

      // Mark this as an include (required for filter processing).
      include.setUserData(USERDATA_COMPOSE_PART_TYPE, ComposePartType.INCLUDE);

      // Expand all included elements. This should also recursively include all referenced
      // ValueSet.compose.include.valueSet elements.
      var vsExpandIncludeResult =
          expandValueSetInternal(combiningTerminologyService, include, context);

      // The specification requires the results of individual includes to be combined (see
      // https://hl7.org/fhir/R4/valueset.html#compositions). Accordingly, merge the result sets.

      // add the results to the finalResultSet
      vsExpansionResult.getConceptBaseInfos().addAll(vsExpandIncludeResult.getConceptBaseInfos());
      // and to the conceptReferenceMap
      vsExpansionResult
          .getExplicitConceptReferences()
          .putAll(vsExpandIncludeResult.getExplicitConceptReferences());
      // remember the used version selection strategies for later use
      vsExpansionResult
          .getSystemVersionSelectionStrategy()
          .putAll(vsExpandIncludeResult.getSystemVersionSelectionStrategy());
    }

    // Iterate over all exclude elements of ValueSet.compose from here.
    for (ValueSet.ConceptSetComponent exclude : compose.getExclude()) {

      // Copy the language information to the exclude user data.
      exclude.setUserData(USERDATA_LANGUAGE, compose.getUserData(USERDATA_LANGUAGE));

      // Mark this as an exclude (may be required for filter processing).
      exclude.setUserData(USERDATA_COMPOSE_PART_TYPE, ComposePartType.EXCLUDE);

      // invoke the expansion for a single exclude component
      // that will recursively resolve any ValueSet.compose.include.valueSet
      var vsExpandExcludeResult =
          expandValueSetInternal(
              combiningTerminologyService,
              exclude,
              new ExpansionResolutionContext(
                  context.scope,
                  context.systemVersionMap,
                  context.checkSystemVersionMap,
                  context.forceSystemVersionMap,
                  null,
                  context.includeInactiveConcepts,
                  context.resolutionPath,
                  context.processingContext));

      // The specification requires the results of individual excludes to be removed from the
      // overall result set (see https://hl7.org/fhir/R4/valueset.html#compositions).

      // remove the results from the finalResultSet
      vsExpansionResult
          .getConceptBaseInfos()
          .removeAll(vsExpandExcludeResult.getConceptBaseInfos());

      MapUtils.removeElements(
          vsExpansionResult.getExplicitConceptReferences(),
          vsExpandExcludeResult.getExplicitConceptReferences());

      // remember the used version selection strategies
      vsExpansionResult
          .getSystemVersionSelectionStrategy()
          .putAll(vsExpandExcludeResult.getSystemVersionSelectionStrategy());
    }

    return vsExpansionResult;
  }

  // -------------------------------------------------------------------------------------------------------------------

  private static Map<String, String> extractXxxSystemVersionParameter(
      List<CanonicalType> xSystemVersionParamList, String paramName) {
    Map<String, String> xSystemVersionMap = new HashMap<>();
    // Iterate over all supplied parameter values and add them to the map.

    if (xSystemVersionParamList != null) {

      for (CanonicalType xSystemVersionParam : xSystemVersionParamList) {

        CanonicalPair canonicalPair = new CanonicalPair(xSystemVersionParam.getValue());
        boolean isFullyDefined =
            canonicalPair.getUrl() != null && canonicalPair.hasNonEmptyVersion();

        if (isFullyDefined) {
          // Throw an exception if multiple versions were specified for a CodeSystem.
          if (xSystemVersionMap.containsKey(canonicalPair.getUrl())) {
            throw new InvalidRequestException(
                "The given '"
                    + paramName
                    + "' parameter contains multiple versions for the same system."
                    + "Please provide only one version per system.");
          }
          // Add the system URL and version to the map.
          xSystemVersionMap.put(canonicalPair.getUrl(), canonicalPair.getVersion());
        } else {
          // Return an error message if the parameter is not formatted correctly.
          throw new InvalidRequestException(
              "The given '"
                  + paramName
                  + "' parameter is not in the correct format. Please use the "
                  + "format 'system|version'.");
        }
      }
    }
    return xSystemVersionMap;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Expands an include or exclude element using the {@code system} reference.
   *
   * <p>Handles expansion based on CodeSystem references according to FHIR R4 rules:
   *
   * <ul>
   *   <li>No concept or filter specified: all codes in the system are included
   *   <li>Concept specified: only the enumerated codes are selected
   *   <li>Filter specified: codes matching the filter criteria are selected
   * </ul>
   *
   * @param combiningTerminologyService the terminology service for delegation
   * @param includeOrExclude the include or exclude element to expand
   * @param context shared include/exclude resolution settings
   * @return the expansion result containing matched concepts
   * @throws UnsupportedTerminologyException if the referenced CodeSystem is not supported
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnknownScopeException if the specified scope is not recognized
   * @throws UnknownConceptException if a referenced concept cannot be found
   */
  private static VsExpandPartResult expandIncludeBySystem(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet.ConceptSetComponent includeOrExclude,
      ExpansionResolutionContext context)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug("expandIncludeBySystem() - started");

    // Scenario 1: expand by system only.
    // If a System only is specified, the following rules apply:
    //    no concept or filter: All codes in the system are included
    //    concept: Only the enumerated codes are selected
    //    filter: Any codes meeting the filter criteria are selected

    // Find the responsible TerminologyManager using the system's canonical URL.
    String system = includeOrExclude.getSystem();
    TerminologyManager terminologyManager =
        combiningTerminologyService.getTerminologyManagerCodeSystemMap().get(system);

    if (terminologyManager == null) {
      throw new UnsupportedTerminologyException(
          String.format(MESSAGE_CODE_SYSTEM_INCLUDE_NOT_SUPPORTED, system));
    }

    // Resolve the include using the responsible TerminologyManager.
    VsExpandIncludeParameter parameter =
        VsExpandIncludeParameter.builder()
            .includeInactiveConcepts(context.includeInactiveConcepts)
            .version(context.systemVersionMap.get(system))
            .checkVersion(context.checkSystemVersionMap.get(system))
            .forceVersion(context.forceSystemVersionMap.get(system))
            .filter(context.filter)
            .processingContext(context.processingContext)
            .build();
    return terminologyManager.vs().expandInclude(context.scope, includeOrExclude, parameter);
  }

  private static VsExpandPartResult expandIncludeByValueSetList(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet.ConceptSetComponent includeOrExclude,
      ExpansionResolutionContext context)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug("expandIncludeByValueSetList() " + includeOrExclude.getValueSet());

    // If the ValueSet definition contains references to multiple ValueSets within a single
    // ValueSet.compose.include the resulting expansion includes only those concepts that are in
    // each expansion of the included ValueSets
    // (rf. https://hl7.org/fhir/R4/valueset.html#expansion)

    // initialize a result that will hold the intersection of the valueSet-includes
    VsExpandPartResult intersectionResult = new VsExpandPartResult();
    boolean firstIteration = true;

    // iterate over all valueSet-Elements that are contained in the includeOrExclude (0..*)
    for (var canonical : includeOrExclude.getValueSet()) {

      // Check that the canonical reference is well-formed.
      String canonicalValue = canonical.getValue();

      // TODO : we check here for a fully defined canonical (with system and veriosn), but in
      // resolveIncludedValueSet there is a branch that also allows for a canonical with just a
      // system.
      //  We should think about if we want to allow this or if we want to enforce the presence of
      // both system and version in the canonical.
      CanonicalPair canonicalPair = parseAndValidateCanonical(canonicalValue);

      // detect cyclic ValueSet dependencies before recursing. The node identity is the
      // canonical URL plus the effective version.
      String referencedIdentity = canonicalPair.getUrl() + "|" + canonicalPair.getVersion();
      if (context.resolutionPath.contains(referencedIdentity)) {
        throw new InvalidRequestException(
            "Cyclic ValueSet dependency detected while resolving '"
                + referencedIdentity
                + "'. The ValueSet dependency graph must be acyclic.");
      }

      ValueSet includedVs =
          resolveIncludedValueSet(
              combiningTerminologyService, context.scope, canonicalPair, canonicalValue);

      // Copy the language information to the compose element so that the language of any display
      // and designation values in the ValueSet can be determined later.
      addComposeLanguageData(includedVs);

      // Expand the resolved ValueSet, recursing if necessary. Extend the resolution path with the
      // identity of the referenced ValueSet to detect cyclic references. The referenced ValueSet
      // was resolved from the service's content, so it is processed as a stored ValueSet, even if
      // the referencing ValueSet was provided within the request.
      java.util.Set<String> childPath = new java.util.HashSet<>(context.resolutionPath);
      childPath.add(referencedIdentity);
      VsExpandPartResult vsExpansionResult =
          processValueSetCompose(
              combiningTerminologyService,
              includedVs.getCompose(),
              new ExpansionResolutionContext(
                  context.scope,
                  context.systemVersionMap,
                  context.checkSystemVersionMap,
                  context.forceSystemVersionMap,
                  context.filter,
                  context.includeInactiveConcepts,
                  childPath,
                  ProcessingContext.stored()));

      firstIteration =
          mergeIntersectionResult(intersectionResult, vsExpansionResult, firstIteration);

      log.debug(
          "intersectionResult.conceptBaseInfos size: {}",
          intersectionResult.getConceptBaseInfos().size());
    }

    log.debug(
        "expandIncludeByValueSet() - returning {} conceptBaseInfos",
        intersectionResult.getConceptBaseInfos().size());

    // return the resulting intersection here
    return intersectionResult;
  }

  /**
   * Parses and validates a canonical value set reference.
   *
   * @param canonicalValue canonical string in ValueSet reference format
   * @return parsed CanonicalPair containing system and version information
   * @throws InvalidRequestException if system or version is missing
   */
  private static CanonicalPair parseAndValidateCanonical(String canonicalValue) {
    CanonicalPair canonicalPair = new CanonicalPair(canonicalValue);
    if (canonicalPair.getUrl() == null || canonicalPair.getUrl().isEmpty()) {
      throw new InvalidRequestException(
          String.format(MESSAGE_VALUE_SET_INCLUDE_HAS_NO_SYSTEM, canonicalValue));
    }
    if (!canonicalPair.hasNonEmptyVersion()) {
      throw new InvalidRequestException(
          String.format(MESSAGE_VALUE_SET_INCLUDE_HAS_NO_VERSION, canonicalValue));
    }
    return canonicalPair;
  }

  /**
   * Resolves a referenced ValueSet via the responsible terminology manager.
   *
   * @param combiningTerminologyService service providing terminology managers
   * @param scope expansion scope
   * @param canonicalPair parsed system/version information
   * @param canonicalValue original canonical value used for exception messages
   * @return resolved ValueSet resource
   * @throws UnsupportedTerminologyException if the referenced ValueSet cannot be resolved
   */
  private static ValueSet resolveIncludedValueSet(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      CanonicalPair canonicalPair,
      String canonicalValue)
      throws UnsupportedTerminologyException, UnknownScopeException {

    // Check whether the referenced ValueSet is supported.
    TerminologyManager terminologyManager =
        combiningTerminologyService.getTerminologyManagerValueSetMap().get(canonicalPair.getUrl());
    if (terminologyManager == null) {
      throw new UnsupportedTerminologyException(
          String.format(MESSAGE_VALUE_SET_INCLUDE_NOT_SUPPORTED, canonicalPair.getUrl()));
    }

    ValueSet includedVs = null;

    try {
      if (StringUtils.isNotEmpty(canonicalPair.getUrl()) && canonicalPair.getVersion().isEmpty()) {
        // VS url present, version missing
        // let the system select the default version
        includedVs = terminologyManager.vs().getValueSet(scope, canonicalPair.getUrl(), null);

      } else if (StringUtils.isNotEmpty(canonicalPair.getUrl())
          && canonicalPair.hasNonEmptyVersion()) {

        // VS url and version present
        // let the system select the defined version
        includedVs =
            terminologyManager
                .vs()
                .getValueSet(scope, canonicalPair.getUrl(), canonicalPair.getVersion());
      }
    } catch (ResourceNotFoundException | UnsupportedTerminologyException _) {
      throw new UnsupportedTerminologyException(
          String.format(MESSAGE_VALUE_SET_INCLUDE_NOT_SUPPORTED, canonicalValue));
    }

    return includedVs;
  }

  /** Adds language metadata from the ValueSet root onto its compose element when available. */
  private static void addComposeLanguageData(ValueSet includedVs) {
    if (includedVs.hasCompose()) {
      includedVs.getCompose().setUserData(USERDATA_LANGUAGE, includedVs.getLanguage());
    }
  }

  /**
   * Computes the canonical identity (url|version) of a ValueSet for cycle detection.
   *
   * @param valueSet the ValueSet
   * @return the identity string, or {@code null} if the ValueSet has no URL
   */
  private static String valueSetIdentity(ValueSet valueSet) {
    if (valueSet == null || !valueSet.hasUrl()) {
      return null;
    }
    return valueSet.getUrl() + "|" + (valueSet.hasVersion() ? valueSet.getVersion() : "");
  }

  // ----------------------------------------------------------------------------------------------
  // Supplement handling
  // ----------------------------------------------------------------------------------------------

  /**
   * Extracts, resolves, and validates the CodeSystem supplements declared by the ValueSet via the
   * R4 {@code valueset-supplement} extension.
   *
   * <p>Each resolved supplement is validated against the base CodeSystem it targets and stored on
   * the {@link VsExpandParameter#getResolvedSupplements()} map keyed by the base CodeSystem
   * canonical URL. A supplement that cannot be resolved or is not applicable fails the whole
   * expansion (no partial expansion).
   *
   * @param combiningTerminologyService the terminology service for CodeSystem resolution
   * @param valueSet the ValueSet being expanded (carrying the extensions)
   * @param vsExpandParameter the expansion parameters to enrich with resolved supplements
   * @param scope the expansion scope
   */
  private static void resolveSupplements(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet valueSet,
      VsExpandParameter vsExpandParameter,
      String scope) {

    List<CanonicalType> supplementReferences = extractSupplementReferences(valueSet);

    Map<String, SupplementRequest> resolved = new HashMap<>();

    // 1. ValueSet-declared supplements (R4 valueset-supplement extension)
    for (CanonicalType reference : supplementReferences) {
      var supplementRequest =
          resolveSupplement(combiningTerminologyService, reference.getValue(), scope);
      putSupplement(resolved, supplementRequest);
    }

    // 2. Request-level supplements (useSupplement parameter, ZTS compatibility extension).
    //    These are merged with the ValueSet-declared supplements; conflicting versions of the same
    //    supplement canonical are rejected.
    if (vsExpandParameter.getUseSupplementParam() != null) {
      for (CanonicalType reference : vsExpandParameter.getUseSupplementParam()) {
        // the useSupplement value must be a valid canonical
        if (reference == null
            || reference.getValue() == null
            || reference.getValue().isEmpty()
            || !reference.getValue().contains("://")) {
          throw new InvalidRequestException(
              "The 'useSupplement' parameter carries a malformed canonical: "
                  + (reference == null ? "null" : reference.getValue()));
        }
        var supplementRequest =
            resolveSupplement(combiningTerminologyService, reference.getValue(), scope);
        putSupplement(resolved, supplementRequest);
      }
    }

    if (!resolved.isEmpty()) {
      vsExpandParameter.setResolvedSupplements(resolved);
    }
  }

  /**
   * Adds a resolved supplement to the map, rejecting conflicting versions of the same supplement
   * canonical.
   *
   * @param resolved the accumulated supplements keyed by base CodeSystem canonical URL
   * @param supplementRequest the supplement to add
   * @throws InvalidRequestException if a conflicting version of the same supplement is present
   */
  private static void putSupplement(
      Map<String, SupplementRequest> resolved, SupplementRequest supplementRequest) {
    String baseUrl = supplementRequest.baseSystemUrl();
    var existing = resolved.get(baseUrl);
    if (existing != null
        && !existing
            .expansionParameterValue()
            .equals(supplementRequest.expansionParameterValue())) {
      throw new InvalidRequestException(
          "Conflicting versions of the same supplement canonical '"
              + supplementRequest.canonicalUrl()
              + "' were requested.");
    }
    resolved.put(baseUrl, supplementRequest);
  }

  /**
   * Extracts the {@code valueset-supplement} extension values from a ValueSet.
   *
   * @param valueSet the ValueSet to inspect
   * @return the list of supplement canonical references (empty if none)
   */
  private static List<CanonicalType> extractSupplementReferences(ValueSet valueSet) {
    List<CanonicalType> references = new ArrayList<>();
    if (valueSet == null) {
      return references;
    }
    valueSet
        .getExtensionsByUrl(
            FhirExtensionUrls
                .EXTENSION_VALUESET_SUPPLEMENT)
        .forEach(
            extension -> {
              // the extension must carry a valueCanonical
              if (!(extension.getValue() instanceof CanonicalType canonical)
                  || canonical.getValue() == null
                  || canonical.getValue().isEmpty()) {
                throw new InvalidRequestException(
                    "The 'valueset-supplement' extension must carry a 'valueCanonical'.");
              }
              references.add(canonical);
            });
    return references;
  }

  /**
   * Resolves a single supplement canonical reference to a validated {@link SupplementRequest}.
   *
   * @param combiningTerminologyService the terminology service for CodeSystem resolution
   * @param canonicalValue the supplement canonical ({@code url} or {@code url|version})
   * @param scope the expansion scope
   * @return the resolved and validated supplement request
   * @throws InvalidRequestException if the reference is malformed, unresolvable, or invalid
   */
  private static SupplementRequest resolveSupplement(
      CombiningTerminologyService combiningTerminologyService,
      String canonicalValue,
      String scope) {

    // the extension value must be a valid canonical
    CanonicalPair pair = new CanonicalPair(canonicalValue);
    if (pair.getUrl() == null || pair.getUrl().isEmpty()) {
      throw new InvalidRequestException(
          "The 'valueset-supplement' extension carries an invalid canonical: " + canonicalValue);
    }

    String supplementUrl = pair.getUrl();
    String supplementVersion = pair.hasNonEmptyVersion() ? pair.getVersion() : null;

    // the supplement CodeSystem must resolve
    CodeSystem supplement =
        findCodeSystem(combiningTerminologyService, scope, supplementUrl, supplementVersion);
    if (supplement == null) {
      throw new InvalidRequestException(
          "The requested supplement '"
              + canonicalValue
              + "' could not be resolved in scope '"
              + scope
              + "'.");
    }

    // structural supplement validation
    if (!supplement.hasContent()
        || supplement.getContent() != CodeSystem.CodeSystemContentMode.SUPPLEMENT) {
      throw new InvalidRequestException(
          "The CodeSystem '" + supplementUrl + "' is not a supplement (content != 'supplement').");
    }
    if (!supplement.hasSupplements()) {
      throw new InvalidRequestException(
          "The supplement '"
              + supplementUrl
              + "' does not declare a base CodeSystem ('supplements' is missing).");
    }

    String baseSystemUrl =
        supplement.getSupplements().contains("|")
            ? supplement.getSupplements().substring(0, supplement.getSupplements().indexOf('|'))
            : supplement.getSupplements();

    return new SupplementRequest(supplementUrl, supplementVersion, baseSystemUrl, supplement);
  }

  /**
   * Resolves a CodeSystem by canonical URL and optional version via the responsible terminology
   * manager.
   *
   * @param combiningTerminologyService the terminology service
   * @param scope the scope
   * @param url the CodeSystem canonical URL
   * @param version the optional version
   * @return the resolved CodeSystem, or {@code null} if not found
   */
  private static CodeSystem findCodeSystem(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      String url,
      String version) {
    TerminologyManager manager =
        combiningTerminologyService.getTerminologyManagerCodeSystemMap().get(url);
    if (manager == null) {
      return null;
    }
    try {
      return manager.cs().getCodeSystem(scope, url, version);
    } catch (RuntimeException | UnknownScopeException | UnsupportedTerminologyException _) {
      return null;
    }
  }

  /**
   * Records the applied supplements in {@code ValueSet.expansion.parameter} using the {@code [base
   * CodeSystem canonical]#supplement} convention.
   *
   * @param resultVs the result ValueSet
   * @param vsExpandParameter the expansion parameters carrying the resolved supplements
   */
  private static void addSupplementParameters(
      ValueSet resultVs, VsExpandParameter vsExpandParameter) {
    if (vsExpandParameter.getResolvedSupplements() == null) {
      return;
    }
    for (var supplement : vsExpandParameter.getResolvedSupplements().values()) {
      resultVs
          .getExpansion()
          .addParameter()
          .setName(supplement.expansionParameterName())
          .setValue(new UriType(supplement.expansionParameterValue()));
    }
  }

  /**
   * Merges a single expansion result into the running intersection.
   *
   * @param intersectionResult current aggregated intersection
   * @param vsExpansionResult result of the currently processed referenced ValueSet
   * @param firstIteration whether the current merge is the first iteration
   * @return {@code false} after first merge, otherwise unchanged
   */
  private static boolean mergeIntersectionResult(
      VsExpandPartResult intersectionResult,
      VsExpandPartResult vsExpansionResult,
      boolean firstIteration) {

    if (firstIteration) {
      // First iteration: copy the results.
      intersectionResult.getConceptBaseInfos().addAll(vsExpansionResult.getConceptBaseInfos());
      intersectionResult
          .getExplicitConceptReferences()
          .putAll(vsExpansionResult.getExplicitConceptReferences());
      firstIteration = false;
    } else {
      // Subsequent iterations: compute the intersection.
      intersectionResult.getConceptBaseInfos().retainAll(vsExpansionResult.getConceptBaseInfos());
      intersectionResult.setExplicitConceptReferences(
          MapUtils.findIntersection(
              intersectionResult.getExplicitConceptReferences(),
              vsExpansionResult.getExplicitConceptReferences()));
    }

    // remember the used version selection strategies
    intersectionResult
        .getSystemVersionSelectionStrategy()
        .putAll(vsExpansionResult.getSystemVersionSelectionStrategy());

    return firstIteration;
  }

  /**
   * Resolves and expands a single include or exclude element of a ValueSet.
   *
   * <p>Handles three scenarios according to FHIR R4 specification:
   *
   * <ul>
   *   <li>System only: delegates to {@link #expandIncludeBySystem}
   *   <li>ValueSet only: delegates to {@link #expandIncludeByValueSetList}
   *   <li>Both system and valueSet: computes intersection of both expansions
   * </ul>
   *
   * @param combiningTerminologyService the terminology service for delegation
   * @param includeOrExclude a single include or exclude element from {@code ValueSet.compose}
   * @param context shared include/exclude resolution settings
   * @return the expansion result containing matched concepts
   * @throws UnsupportedTerminologyException if a referenced terminology is not supported
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnknownScopeException if the specified scope is not recognized
   * @throws UnknownConceptException if a referenced concept cannot be found
   */
  private static VsExpandPartResult expandValueSetInternal(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet.ConceptSetComponent includeOrExclude,
      ExpansionResolutionContext context)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Scenario 1: include with a 'system' element (no 'valueSet' element).
    if (includeOrExclude.hasSystem() && !includeOrExclude.hasValueSet()) {
      log.debug(
          "expandValueSetInternal() - system: {} (version={}, filter={}, concepts={})",
          includeOrExclude.getSystem(),
          includeOrExclude.getVersion(),
          includeOrExclude.getFilter(),
          includeOrExclude.getConcept());

      return expandIncludeBySystem(combiningTerminologyService, includeOrExclude, context);
    }

    // Scenario 2: include with one or more 'valueSet' elements (no 'system' element).
    else if (includeOrExclude.hasValueSet() && !includeOrExclude.hasSystem()) {
      log.debug("expandValueSetInternal() - valueSet: {}", includeOrExclude.getValueSet());

      return expandIncludeByValueSetList(combiningTerminologyService, includeOrExclude, context);
    }

    // Scenario 3: include with a 'system' element AND one or more 'valueSet' elements.
    else if (includeOrExclude.hasSystem() && includeOrExclude.hasValueSet()) {
      log.debug(
          "expandValueSetInternal() - system: {}, valueSet: {}",
          includeOrExclude.getSystem(),
          includeOrExclude.getValueSet());

      // Expand the system.
      var combinedResult =
          expandIncludeBySystem(combiningTerminologyService, includeOrExclude, context);

      // Expand ValueSet.compose.include.valueSet.
      var vsExpansionResultByValueSet =
          expandIncludeByValueSetList(combiningTerminologyService, includeOrExclude, context);

      // Compute the intersection of the ConceptBaseInfo and ConceptReferenceComponent maps.
      combinedResult
          .getConceptBaseInfos()
          .retainAll(vsExpansionResultByValueSet.getConceptBaseInfos());
      combinedResult.setExplicitConceptReferences(
          MapUtils.findIntersection(
              combinedResult.getExplicitConceptReferences(),
              vsExpansionResultByValueSet.getExplicitConceptReferences()));

      // Return the result.
      return combinedResult;

    } else {
      // This case should not occur because it is already checked during validation.
      throw new InvalidRequestException(
          "Neither 'system' nor 'valueSet' is specified in the ValueSet 'include'");
    }
  }

  /**
   * Creates the expansion element and populates it with the resulting concepts.
   *
   * <p>Applies paging parameters ({@code count}, {@code offset}) and filters based on {@code
   * activeOnly} parameter. Also handles language preferences for display values.
   *
   * @param combiningTerminologyService the terminology service for creating contains components
   * @param resultVs the ValueSet to populate with the expansion
   * @param finalResultSet the set of concepts to include in the expansion
   * @param context shared settings and request data needed for expansion creation
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnsupportedTerminologyException if a referenced terminology is not supported
   * @throws UnknownScopeException if the specified scope is not recognized
   */
  private static void createExpansionElement(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet resultVs,
      SortedSet<ConceptBaseInfo> finalResultSet,
      ExpansionElementContext context)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {

    long count = resolveCount(context.vsExpandParameter);
    long offset = resolveOffset(context.vsExpandParameter);
    String displayLanguage = resolveDisplayLanguage(resultVs, context.vsExpandParameter);
    boolean includeInactiveConcepts = resolveIncludeInactiveConcepts(context.vsExpandParameter);
    boolean includeDesignations = resolveIncludeDesignations(context.vsExpandParameter);
    List<String> designationParamValueList =
        extractDesignationParamValues(context.vsExpandParameter);

    // Apply the 'exclude-system' parameter: remove all concepts belonging to an excluded
    // CodeSystem (optionally restricted to a specific version) before paging.
    applyExcludeSystem(finalResultSet, context.vsExpandParameter);

    validateOffsetInRange(
        offset, includeInactiveConcepts, finalResultSet, context.activeConceptCount);

    List<ConceptBaseInfo> returnList =
        buildReturnList(finalResultSet, includeInactiveConcepts, offset, count);

    ValueSet.ValueSetExpansionComponent resultExpansion = resultVs.getExpansion();
    applyExpansionMeta(
        resultExpansion,
        includeInactiveConcepts,
        finalResultSet,
        context.activeConceptCount,
        offset);

    addExpansionContainsElements(
        combiningTerminologyService,
        resultExpansion,
        returnList,
        new ContainsCreationContext(
            context.scope,
            includeDesignations,
            designationParamValueList,
            displayLanguage,
            context.languageSelectionParameter,
            context.explicitConceptReferenceMap,
            context.vsExpandParameter.getResolvedSupplements()));
  }

  /**
   * Resolves the effective {@code count} paging parameter.
   *
   * <p>Uses {@link #DEFAULT_COUNT_PARAM_VALUE} when no value is provided and caps to {@link
   * #MAX_COUNT_PARAM_VALUE}.
   */
  private static long resolveCount(VsExpandParameter vsExpandParameter) {
    long count = DEFAULT_COUNT_PARAM_VALUE;
    if (vsExpandParameter.getCountParam() != null && !vsExpandParameter.getCountParam().isEmpty()) {
      count = vsExpandParameter.getCountParam().getValue();
      if (count > MAX_COUNT_PARAM_VALUE) {
        count = MAX_COUNT_PARAM_VALUE;
      }
    }
    return count;
  }

  /**
   * Resolves the effective {@code offset} paging parameter and validates it is not negative.
   *
   * @throws InvalidRequestException if {@code offset} is less than {@code 0}
   */
  private static long resolveOffset(VsExpandParameter vsExpandParameter) {
    long offset = 0;
    if (vsExpandParameter.getOffsetParam() != null
        && !vsExpandParameter.getOffsetParam().isEmpty()) {
      offset = vsExpandParameter.getOffsetParam().getValue();
      if (offset < 0) {
        throw new InvalidRequestException(
            "The given offset value is less than 0. This is not for ValueSet Expansion.");
      }
    }
    return offset;
  }

  /**
   * Resolves the preferred display language.
   *
   * <p>The ValueSet language is used first and overridden by {@code displayLanguage} parameter if
   * present.
   */
  private static String resolveDisplayLanguage(
      ValueSet resultVs, VsExpandParameter vsExpandParameter) {
    String displayLanguage = null;

    if (resultVs.hasLanguage()) {
      displayLanguage = resultVs.getLanguage();
    }

    if (vsExpandParameter.getDisplayLanguageParam() != null
        && !vsExpandParameter.getDisplayLanguageParam().isEmpty()) {
      displayLanguage = vsExpandParameter.getDisplayLanguageParam().getCode();
    }

    return displayLanguage;
  }

  /** Resolves whether inactive concepts should be included in the expansion. */
  private static boolean resolveIncludeInactiveConcepts(VsExpandParameter vsExpandParameter) {
    return vsExpandParameter.getActiveOnlyParam() == null
        || vsExpandParameter.getActiveOnlyParam().isEmpty()
        || !vsExpandParameter.getActiveOnlyParam().booleanValue();
  }

  /** Resolves whether designations should be included in expansion contains entries. */
  private static boolean resolveIncludeDesignations(VsExpandParameter vsExpandParameter) {
    return vsExpandParameter.getIncludeDesignationsParam() != null
        && vsExpandParameter.getIncludeDesignationsParam().booleanValue();
  }

  /** Extracts raw designation parameter values from the request. */
  private static List<String> extractDesignationParamValues(VsExpandParameter vsExpandParameter) {
    List<String> designationParamValueList = new ArrayList<>();
    if (vsExpandParameter.getDesignationParam() != null) {
      for (StringType designationParam : vsExpandParameter.getDesignationParam()) {
        designationParamValueList.add(designationParam.getValue());
      }
    }
    return designationParamValueList;
  }

  /**
   * Validates that {@code offset} is within the addressable range for the current result set.
   *
   * @throws InvalidRequestException if {@code offset} exceeds available concepts
   */
  private static void validateOffsetInRange(
      long offset,
      boolean includeInactiveConcepts,
      SortedSet<ConceptBaseInfo> finalResultSet,
      long activeConceptCount) {

    if (offset != 0) {
      if (includeInactiveConcepts) {
        if (offset > finalResultSet.size() - 1) {
          throw new InvalidRequestException(
              "The given offset value is greater than the number of concepts that can be returned by the ValueSet Expansion.");
        }
      } else {
        if (offset > activeConceptCount - 1) {
          throw new InvalidRequestException(
              "The given offset value is greater than the number of active concepts that can be returned by the ValueSet Expansion.");
        }
      }
    }
  }

  /** Builds the page result list by applying active-state filtering and paging parameters. */
  private static List<ConceptBaseInfo> buildReturnList(
      SortedSet<ConceptBaseInfo> finalResultSet,
      boolean includeInactiveConcepts,
      long offset,
      long count) {

    List<ConceptBaseInfo> returnList = new ArrayList<>();

    finalResultSet.stream()
        .filter(conceptBaseInfo -> conceptBaseInfo.isActive() || includeInactiveConcepts)
        .skip(offset)
        .limit(count)
        .forEach(returnList::add);

    return returnList;
  }

  /**
   * Applies the {@code exclude-system} parameter by removing all concepts that belong to an
   * excluded CodeSystem.
   *
   * <p>Each {@code exclude-system} value is a canonical {@code [system]} or {@code
   * [system]|[version]}. A value without a version excludes all versions of that system; a value
   * with a version excludes only that version. Exclusion is applied before paging.
   *
   * @param finalResultSet the mutable result set to filter
   * @param vsExpandParameter the expansion parameters carrying {@code exclude-system}
   */
  private static void applyExcludeSystem(
      SortedSet<ConceptBaseInfo> finalResultSet, VsExpandParameter vsExpandParameter) {
    List<CanonicalType> excludeSystem = vsExpandParameter.getExcludeSystemParam();
    if (excludeSystem == null || excludeSystem.isEmpty()) {
      return;
    }

    Map<String, String> excludedVersionBySystem = new HashMap<>();
    for (CanonicalType canonical : excludeSystem) {
      CanonicalPair pair = new CanonicalPair(canonical.getValue());
      if (pair.getUrl() != null && !pair.getUrl().isEmpty()) {
        excludedVersionBySystem.put(
            pair.getUrl(), pair.hasNonEmptyVersion() ? pair.getVersion() : null);
      }
    }

    finalResultSet.removeIf(
        concept -> {
          if (!excludedVersionBySystem.containsKey(concept.getSystem())) {
            return false;
          }
          String excludedVersion = excludedVersionBySystem.get(concept.getSystem());
          return excludedVersion == null || excludedVersion.equals(concept.getVersion());
        });
  }

  /** Applies expansion metadata such as total concept count and offset. */
  private static void applyExpansionMeta(
      ValueSet.ValueSetExpansionComponent resultExpansion,
      boolean includeInactiveConcepts,
      SortedSet<ConceptBaseInfo> finalResultSet,
      long activeConceptCount,
      long offset) {

    if (includeInactiveConcepts) {
      resultExpansion.setTotal(finalResultSet.size());
    } else {
      resultExpansion.setTotalElement(new IntegerType(activeConceptCount));
    }

    resultExpansion.setOffsetElement(new IntegerType(offset));
  }

  /**
   * Adds {@code expansion.contains} elements for all concepts in the result page.
   *
   * @throws NotImplementedException if a required feature is not yet implemented
   * @throws UnsupportedTerminologyException if a referenced terminology is not supported
   * @throws UnknownScopeException if the specified scope is not recognized
   */
  private static void addExpansionContainsElements(
      CombiningTerminologyService combiningTerminologyService,
      ValueSet.ValueSetExpansionComponent resultExpansion,
      List<ConceptBaseInfo> returnList,
      ContainsCreationContext context)
      throws NotImplementedException, UnsupportedTerminologyException, UnknownScopeException {

    for (ConceptBaseInfo conceptBaseInfo : returnList) {

      TerminologyManager terminologyManager =
          combiningTerminologyService
              .getTerminologyManagerCodeSystemMap()
              .get(conceptBaseInfo.getSystem());

      resultExpansion.addContains(
          terminologyManager
              .vs()
              .createExpansionContainsComponent(
                  context.scope,
                  conceptBaseInfo,
                  context.includeDesignations,
                  context.designationParamValueList,
                  context.displayLanguage,
                  context.languageSelectionParameter,
                  context.explicitConceptReferenceMap,
                  context.supplements));
    }
  }
}
