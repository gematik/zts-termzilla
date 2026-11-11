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
package de.gematik.zts.termzilla.server.service;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.InvalidParameterException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ResourceOrigin;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.api.service.Terminology;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.TerminologyProvider;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.cache.TerminologyCacheManager;
import de.gematik.zts.termzilla.server.service.cm.CombiningCmSearchOperation;
import de.gematik.zts.termzilla.server.service.cm.CombiningCmSearchParameterValidator;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsFindMatchesOperation;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsFindMatchesParameterValidator;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsLookupOperation;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsSearchOperation;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsSearchParameterValidator;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsValidateCodeOperation;
import de.gematik.zts.termzilla.server.service.cs.CombiningCsValidateCodeParameterValidator;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsExpandParameterValidator;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsProvidedExpandOperation;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsSearchOperation;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsSearchParameterValidator;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsValidateCodeOperation;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsValidateCodeParameterValidator;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Central service that aggregates terminology operations across all registered {@link
 * TerminologyManager} implementations.
 *
 * <p>On construction, {@link TerminologyProvider} implementations are discovered via the Java SPI
 * mechanism and their managers are registered. Incoming FHIR requests for CodeSystem, ValueSet, and
 * ConceptMap operations are routed to the responsible manager based on canonical URL lookup maps.
 * Results are cached via {@link TerminologyCacheManager}.
 */
@Slf4j
public class CombiningTerminologyService {

  private static final String RESOURCE_NOT_FOUND_MESSAGE_TEMPLATE =
      "%s with id '%s' not found in scope '%s'";

  // Liste aller registrierten TerminologyManager
  @Getter final List<TerminologyManager> terminologyManagerList = new ArrayList<>();

  @Getter private final Set<String> supportedScopes = new HashSet<>();

  // Centralized cache manager with Caffeine
  @Getter private final TerminologyCacheManager cacheManager = new TerminologyCacheManager();

  @Getter
  private final Map<String, TerminologyManager> terminologyManagerCodeSystemMap = new HashMap<>();

  @Getter
  private final Map<String, TerminologyManager> terminologyManagerValueSetMap = new HashMap<>();

  @Getter
  private final Map<String, TerminologyManager> terminologyManagerConceptMapMap = new HashMap<>();

  @Getter private final LanguageParameter languageSystemDefault;

  @Getter private final String configAndDataStateHash;

  @Getter private final ConfigProperties configProperties;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a service instance by discovering all {@link TerminologyProvider} implementations via
   * the Java SPI mechanism and initializing their managers.
   *
   * @param properties the application configuration
   * @throws InvalidConfigurationException if a provider fails to create its manager
   */
  public CombiningTerminologyService(ConfigProperties properties)
      throws InvalidConfigurationException {
    this(createManagersFromProviders(properties), properties);
  }

  /**
   * Creates a service instance from a pre-built list of managers, intended primarily for testing.
   *
   * <p>Only managers that report {@link TerminologyManager#isInitialized()} as {@code true} are
   * registered.
   *
   * @param terminologyManagerList the managers to register
   * @param properties the application configuration
   * @throws InvalidConfigurationException if {@code properties} is {@code null}
   */
  public CombiningTerminologyService(
      List<TerminologyManager> terminologyManagerList, ConfigProperties properties)
      throws InvalidConfigurationException {

    if (properties == null) {
      throw new InvalidConfigurationException("ConfigProperties must not be null");
    }

    languageSystemDefault = LanguageParameter.parse(properties.getSystemLanguage());

    for (TerminologyManager manager : terminologyManagerList) {
      if (manager.isInitialized()) {
        registerManager(manager);
      }
    }

    configAndDataStateHash = buildConfigAndDataStateHash(this.terminologyManagerList, properties);
    configProperties = properties;
  }

  // ----------------------------------------------------------------------------------------
  // Initialization methods
  // ----------------------------------------------------------------------------------------

  /**
   * Creates {@link TerminologyManager} instances for all registered {@link TerminologyProvider}
   * implementations discovered via the Java SPI mechanism.
   *
   * @param properties the application configuration used to initialize each provider
   * @return the list of created managers, including both initialized and uninitialized ones
   * @throws InvalidConfigurationException if a provider fails to create its manager
   */
  private static List<TerminologyManager> createManagersFromProviders(ConfigProperties properties)
      throws InvalidConfigurationException {

    // Initialisieren aller verfügbaren TerminologyManager. Diese werden unter
    // 'resources/META-INF/services' registriert
    List<TerminologyProvider> terminologyProviders = Terminology.providers();
    List<TerminologyManager> managers = new ArrayList<>();

    int parallelTasks =
        Math.min(terminologyProviders.size(), Runtime.getRuntime().availableProcessors());

    try (ExecutorService providerExecutor = Executors.newFixedThreadPool(parallelTasks)) {
      // TODO: make default Scope configurable
      List<Callable<TerminologyManager>> tasks =
          terminologyProviders.stream()
              .map(
                  provider ->
                      (Callable<TerminologyManager>)
                          () -> provider.create(Path.of(properties.getBaseWorkingDir()), "fhir"))
              .toList();

      List<Future<TerminologyManager>> futures = providerExecutor.invokeAll(tasks);

      for (Future<TerminologyManager> future : futures) {
        managers.add(future.get());
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      throw new InvalidConfigurationException(
          "Terminology provider initialization was interrupted");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      throw new InvalidConfigurationException(
          "Terminology provider initialization failed: " + cause.getMessage());
    }

    return managers;
  }

  /**
   * Registers an initialized {@link TerminologyManager} by adding it to all internal lookup
   * structures.
   *
   * @param manager the manager to register; must be initialized
   */
  private void registerManager(TerminologyManager manager) {
    terminologyManagerList.add(manager);
    supportedScopes.addAll(manager.getSupportedScopes());
    manager
        .cs()
        .getSupportedCodeSystemCanonicalsList()
        .forEach(system -> terminologyManagerCodeSystemMap.put(system, manager));
    manager
        .vs()
        .getSupportedValueSetCanonicalList()
        .forEach(system -> terminologyManagerValueSetMap.put(system, manager));
    manager
        .cm()
        .getSupportedConceptMapCanonicalList()
        .forEach(system -> terminologyManagerConceptMapMap.put(system, manager));
  }

  /**
   * Computes a combined SHA-256 hash over the given configuration and the state of all managers.
   *
   * <p>Managers are sorted by name before hashing to ensure a deterministic result independent of
   * registration order.
   *
   * @param managers the list of registered managers to include in the hash
   * @param properties the configuration whose hash code contributes to the result
   * @return a hex-encoded SHA-256 hash representing the combined configuration and data state
   */
  private static String buildConfigAndDataStateHash(
      List<TerminologyManager> managers, ConfigProperties properties) {

    StringBuilder hashInputBuilder = new StringBuilder();

    // Konfiguration des core Moduls
    hashInputBuilder.append(DigestUtils.sha256Hex(properties.toString()));

    // Konfiguration und Datenstatus der einzelnen TerminologyManager
    managers.stream()
        .sorted(Comparator.comparing(TerminologyManager::getTerminologyManagerName))
        .forEach(manager -> hashInputBuilder.append(manager.getConfigurationAndDataStateHash()));

    log.debug(
        "Combined configuration and data state hash: {}",
        DigestUtils.sha256Hex(hashInputBuilder.toString()));

    return DigestUtils.sha256Hex(hashInputBuilder.toString());
  }

  // ----------------------------------------------------------------------------------------
  // Helfermethoden
  // ----------------------------------------------------------------------------------------

  /**
   * Returns the {@link TerminologyManager} responsible for the terminology at the given canonical
   * URL.
   *
   * @param url the canonical URL of the FHIR terminology resource
   * @return the responsible manager
   * @throws UnsupportedTerminologyException if no manager is registered for the given URL
   */
  public TerminologyManager getResponsibleTerminologyManager(String url)
      throws UnsupportedTerminologyException {
    if (terminologyManagerCodeSystemMap.get(url) != null) {
      return terminologyManagerCodeSystemMap.get(url);
    } else {
      throw new UnsupportedTerminologyException(
          "The requested terminology '" + url + "' is not supported.");
    }
  }

  /**
   * Verifies that the given scope is supported by at least one registered manager.
   *
   * @param scope the scope identifier to check
   * @throws UnknownScopeException if the scope is not supported
   */
  public void checkScope(String scope) throws UnknownScopeException {
    if (!supportedScopes.contains(scope)) {
      throw new UnknownScopeException("Scope '" + scope + "' is not supported.");
    }
  }

  // ----------------------------------------------------------------------------------------
  // CodeSystem methods
  // ----------------------------------------------------------------------------------------

  /**
   * Returns the default CodeSystem for the given scope and canonical URL in the specified retrieval
   * mode.
   *
   * @param scope the scope to search in
   * @param url the canonical URL of the CodeSystem
   * @param retrievalMode whether to return the full or stripped resource
   * @throws ResourceNotFoundException if no matching CodeSystem is found
   */
  public CodeSystem getDefaultCodeSystemByScopeAndUrl(
      String scope, String url, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException {

    // Suche nach dem CodeSystem in den einzelnen TerminologyManagern
    return terminologyManagerList.parallelStream()
        .map(
            manager -> {
              try {
                return manager.cs().getDefaultCodeSystemByUrlAndScope(scope, url, retrievalMode);
              } catch (Exception _) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "CodeSystem with url '" + url + "' not found in scope '" + scope + "'"));
  }

  /**
   * Returns the full default CodeSystem for the given scope and canonical URL.
   *
   * @param scope the scope to search in
   * @param url the canonical URL of the CodeSystem
   * @throws ResourceNotFoundException if no matching CodeSystem is found
   */
  public CodeSystem getDefaultCodeSystemByScopeAndUrl(String scope, String url)
      throws ResourceNotFoundException {

    // Suche nach dem CodeSystem in den einzelnen TerminologyManagern
    return getDefaultCodeSystemByScopeAndUrl(scope, url, ResourceRetrievalMode.FULL);
  }

  /**
   * Returns the CodeSystem with the given ID within the specified scope and retrieval mode.
   *
   * @param scope the scope the CodeSystem must belong to
   * @param id the FHIR resource ID of the CodeSystem
   * @param retrievalMode whether to return the full or stripped resource
   * @throws ResourceNotFoundException if no matching CodeSystem is found
   */
  public CodeSystem getCodeSystem(String scope, IdType id, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException {

    // Suche nach dem CodeSystem in den einzelnen TerminologyManagern
    return terminologyManagerList.parallelStream()
        // .map(manager -> manager.cs().getCodeSystem(id, scope))
        .map(
            manager -> {
              try {
                return manager.cs().getCodeSystem(scope, id, retrievalMode);
              } catch (Exception _) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    String.format(RESOURCE_NOT_FOUND_MESSAGE_TEMPLATE, "CodeSystem", id, scope)));
  }

  /**
   * Returns the full CodeSystem with the given ID within the specified scope.
   *
   * @param scope the scope the CodeSystem must belong to
   * @param id the FHIR resource ID of the CodeSystem
   * @throws ResourceNotFoundException if no matching CodeSystem is found
   */
  public CodeSystem getCodeSystem(String scope, IdType id) throws ResourceNotFoundException {

    // Suche nach dem CodeSystem in den einzelnen TerminologyManagern
    return getCodeSystem(scope, id, ResourceRetrievalMode.FULL);
  }

  /**
   * Searches for CodeSystems within the given scope using the supplied search parameters.
   *
   * <p>Results are served from cache if available; otherwise the search is executed and cached.
   *
   * @param scope the scope to search in
   * @param csSearchParameter the search parameters
   * @param isCapabilitySearch whether the search is part of a capability statement query
   * @return the IDs of all matching CodeSystems
   * @throws InvalidRequestException if the search parameters are invalid
   */
  public List<IdType> searchCodeSystem(
      String scope, CsSearchParameter csSearchParameter, boolean isCapabilitySearch)
      throws InvalidRequestException {

    String scopeKey = isCapabilitySearch ? scope + "_capability" : scope;

    // Check cache first
    List<IdType> cachedResult = cacheManager.getSearch(scopeKey, csSearchParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // TODO parameter validation should be done as a post action of the builder in the Parameter
    // class itself
    // Parametervalidierung
    CombiningCsSearchParameterValidator.validateParameters(csSearchParameter);

    // Weiterreichen der Anfrage an die zuständige Operation
    List<IdType> contentResultList =
        CombiningCsSearchOperation.searchCodeSystems(
            this, csSearchParameter, scope, isCapabilitySearch);

    log.debug("ContentSearchResults: {}", contentResultList);

    // Cache the result
    cacheManager.putSearch(scopeKey, csSearchParameter, contentResultList);

    return contentResultList;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Looks up a code in a CodeSystem and returns concept details.
   *
   * <p>Results are served from cache if available.
   *
   * @param scope the scope to search in
   * @param csLookupParameter the lookup parameters including system and code
   * @return the lookup result containing concept properties
   * @throws InvalidRequestException if the parameters are invalid
   * @throws NotImplementedException if the operation is not supported by the responsible manager
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws UnknownScopeException if the scope is not supported
   */
  public CsLookupResult lookupCode(String scope, CsLookupParameter csLookupParameter)
      throws InvalidRequestException,
          NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          InvalidParameterException,
          UnknownConceptException {

    log.debug("Lookup Code: {}", csLookupParameter.toString());

    // set system language default for use in the lookup operation and potential delegation to other
    // managers
    csLookupParameter.setLanguageDefaultSystemLanguage(languageSystemDefault);

    // Check cache first
    CsLookupResult cachedResult = cacheManager.getLookup(scope, csLookupParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // Lookup-Operation ausführen und das Ergebnis zurückgeben
    CsLookupResult result = CombiningCsLookupOperation.lookupCode(this, scope, csLookupParameter);

    // Cache the lookup result
    cacheManager.putLookup(scope, csLookupParameter, result);

    return result;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Validates a code against a CodeSystem using the supplied parameters.
   *
   * <p>Results are served from cache if available.
   *
   * @param scope the scope to validate within
   * @param csValidateCodeParameter the validation parameters
   * @return the validation result
   * @throws InvalidRequestException if the parameters are invalid
   * @throws NotImplementedException if the operation is not supported by the responsible manager
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws UnknownScopeException if the scope is not supported
   */
  public ValidateCodeResult validateCode(
      String scope, CsValidateCodeParameter csValidateCodeParameter)
      throws InvalidRequestException,
          NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug("{}", csValidateCodeParameter.toString());

    // Check cache first
    ValidateCodeResult cachedResult = cacheManager.getCsValidation(scope, csValidateCodeParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // Parametervalidierung
    CombiningCsValidateCodeParameterValidator.validateParameters(csValidateCodeParameter);

    // Validierungs-Operation ausführen und das Ergebnis zurückgeben
    ValidateCodeResult result =
        CombiningCsValidateCodeOperation.validateCode(
            this, csValidateCodeParameter, scope, languageSystemDefault);

    // Cache the validation result
    cacheManager.putCsValidation(scope, csValidateCodeParameter, result);

    return result;
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Finds concepts matching the given compositional expression within a CodeSystem.
   *
   * @param scope the scope to search in
   * @param csFindMatchesParameter the matching parameters, including the mandatory {@code system}
   *     URL
   * @return the matching result
   * @throws InvalidRequestException if the parameters are invalid or {@code system} is absent
   * @throws NotImplementedException if the operation is not supported by the responsible manager
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws UnknownScopeException if the scope is not supported
   */
  public CsFindMatchesResult findMatches(
      String scope, CsFindMatchesParameter csFindMatchesParameter)
      throws InvalidRequestException,
          NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException {

    log.debug("{}", csFindMatchesParameter.toString());

    // set system language default for use in the find-matches operation and potential delegation to
    // other managers
    csFindMatchesParameter.setLanguageDefaultSystemLanguage(languageSystemDefault);

    // Parametervalidierung
    // TODO discuss: this calls the abstract class method, which is overridden in the individual
    // managers.
    // This results in multiple validations of the same parameters, which is not ideal. We should
    // refactor this to avoid redundant validations.
    // Though if the modules are used standalone, they also need parameter validation
    new CombiningCsFindMatchesParameterValidator().validateParameters(csFindMatchesParameter);

    return CombiningCsFindMatchesOperation.findMatches(this, scope, csFindMatchesParameter);
  }

  // ----------------------------------------------------------------------------------------
  // ValueSet methods
  // ----------------------------------------------------------------------------------------

  /**
   * Returns the ValueSet with the given ID within the specified scope and retrieval mode.
   *
   * @param scope the scope the ValueSet must belong to
   * @param id the FHIR resource ID of the ValueSet
   * @param retrievalMode whether to return the full or stripped resource
   * @throws ResourceNotFoundException if no matching ValueSet is found
   */
  public ValueSet getValueSet(String scope, IdType id, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException {

    return terminologyManagerList.parallelStream()
        .map(
            manager -> {
              try {
                return manager.vs().getValueSet(scope, id, retrievalMode);
              } catch (Exception _) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    String.format(RESOURCE_NOT_FOUND_MESSAGE_TEMPLATE, "ValueSet", id, scope)));
  }

  /**
   * Returns the full ValueSet with the given ID within the specified scope.
   *
   * @param scope the scope the ValueSet must belong to
   * @param id the FHIR resource ID of the ValueSet
   * @throws ResourceNotFoundException if no matching ValueSet is found
   */
  public ValueSet getValueSet(String scope, IdType id) throws ResourceNotFoundException {

    return getValueSet(scope, id, ResourceRetrievalMode.FULL);
  }

  // ==========================================================================================

  /**
   * Searches for ValueSets within the given scope using the supplied search parameters.
   *
   * <p>Results are served from cache if available. Searching by {@code code} parameter is not yet
   * supported and will throw {@link NotImplementedException}.
   *
   * @param scope the scope to search in
   * @param vsSearchParameter the search parameters
   * @return the IDs of all matching ValueSets
   * @throws NotImplementedException if the {@code code} search parameter is present
   */
  public List<IdType> searchValueSet(String scope, VsSearchParameter vsSearchParameter)
      throws NotImplementedException {

    // Check cache first
    List<IdType> cachedResult = cacheManager.getSearch(scope, vsSearchParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // TODO parameter validation should be done as a post action of the builder in the Parameter
    // class itself
    // Parametervalidierung
    CombiningVsSearchParameterValidator.validateParameters(vsSearchParameter);

    // Wenn ein 'code'-Parameter übergeben wurde, müssen wir anders agieren, als bei den anderen
    // Szenarien. Deshalb entfernen wir ihn erstmal aus der Parameterliste und merken ihn uns für
    // spätere Verabeitungsschritte. TODO: Aktuell failen wir einfach
    Optional<TokenParam> codeParam = vsSearchParameter.getCode();
    if (codeParam.isPresent()) {
      vsSearchParameter.setCodeParam(null);
      throw new NotImplementedException(
          "Die Suche nach ValueSets anhand eines Codes ist noch nicht implementiert.");
    }

    // Suche nach CodeSystemen anhand der verbliebenen Parameter innerhalb des Suchindex
    List<IdType> tempResultList =
        CombiningVsSearchOperation.searchValueSets(this, vsSearchParameter, scope);

    // Cache the result
    cacheManager.putSearch(scope, vsSearchParameter, tempResultList);

    return tempResultList;
  }

  // ==========================================================================================

  /**
   * Expands a ValueSet identified either by its canonical URL or by an inline ValueSet resource.
   *
   * <p>When a URL is provided, the responsible manager is located via the registry or by querying
   * all registered managers. The resolved ValueSet is then expanded recursively. Results are served
   * from cache if available.
   *
   * @param scope the scope to expand within
   * @param vsExpandParameter the expansion parameters; must contain either a URL or an inline
   *     ValueSet
   * @return the expanded ValueSet
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws NotImplementedException if the operation is not supported by the responsible manager
   * @throws UnknownScopeException if the scope is not supported
   * @throws UnknownConceptException if a referenced concept is unknown
   */
  public ValueSet expandValueSet(String scope, VsExpandParameter vsExpandParameter)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Check cache first
    ValueSet cachedResult = cacheManager.getExpansion(scope, vsExpandParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // Parametervalidierung
    CombiningVsExpandParameterValidator.validateParameters(vsExpandParameter);

    // Verarbeitung in Abhängigkeit des gewählten Input-Parameters (url oder valueSet)
    // Fall 1: Es wurde eine ValueSet-Url angegeben. Jetzt müssen wir dieses VS laden, um es
    // expanden zu können.
    if (hasUrlParameter(vsExpandParameter)) {
      return expandValueSetFromUrl(scope, vsExpandParameter);
    }

    // Fall 2: Es wurde ein ValueSet-Objekt im Request übergeben
    if (hasInlineValueSet(vsExpandParameter)) {
      return expandProvidedValueSet(scope, vsExpandParameter);
    }

    // Hier sollten wir eigentlich nie landen, da die Parametervalidierung das verhindern müsste.
    throw new InvalidRequestException("Either 'url' or 'valueSet' must be present");
  }

  /**
   * Expands a ValueSet referenced by URL by resolving the responsible manager and rewriting the
   * request to an inline ValueSet expansion.
   *
   * @param scope the scope to expand within
   * @param vsExpandParameter the expansion parameters containing URL and optional version
   * @return the expanded ValueSet
   * @throws UnsupportedTerminologyException if the referenced ValueSet cannot be resolved
   * @throws NotImplementedException if expansion is not supported by the responsible manager
   * @throws UnknownScopeException if the scope is not supported
   * @throws UnknownConceptException if a referenced concept is unknown
   */
  private ValueSet expandValueSetFromUrl(String scope, VsExpandParameter vsExpandParameter)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // url und version des referenzierten ValueSets ermitteln
    String url = vsExpandParameter.getUrlParam().getValue();
    String version =
        (vsExpandParameter.getValueSetVersionParam() != null
                && !vsExpandParameter.getValueSetVersionParam().isEmpty())
            ? vsExpandParameter.getValueSetVersionParam().getValueAsString()
            : null;

    // Heraussuchen des für das referenzierte ValueSet zuständigen TerminologyManagers.
    // Da nicht alle ValueSets explizit beschrieben und somit auffindbar sind, müssen wir
    // zweistufig vorgehen:
    TerminologyManager tm = resolveValueSetTerminologyManager(scope, url, version);

    // Abrufen des ValueSets für den jeweiligen Scope vom zuständigen TerminologyManager
    ValueSet vs = tm.vs().getValueSet(scope, url, version);

    // Sollte eigentlich nicht passieren, wenn die einzelnen TerminologyManager sauber
    // implementiert sind. Wir prüfen es aber trotzdem, um eine klare Fehlermeldung zu
    // liefern.
    if (vs == null) {
      throw new UnsupportedTerminologyException(
          "The ValueSet with the url '" + url + "' is not supported in scope '" + scope + "'.");
    }

    // Wir ändern die Parameter des VsExpandParameter-Objekts, um das Valueset jetzt direkt
    // einzubetten. Die Referenz - bestehend aus URL und Version - entfernen wir hingegen
    vsExpandParameter.setValueSetParam(vs.copy());
    vsExpandParameter.setUrlParam(null);
    vsExpandParameter.setValueSetVersionParam(null);

    // Das eingebettete ValueSet stammt aus dem Bestand des Servers und nicht aus dem Request
    vsExpandParameter.setResourceOrigin(ResourceOrigin.STORED);

    // Erneutes Aufrufen der expandValueSet-Methode mit verändertem Parameter-Objekt
    return this.expandValueSet(scope, vsExpandParameter);
  }

  /**
   * Resolves the responsible terminology manager for a ValueSet URL and optional version.
   *
   * @param scope the scope in which support should be checked
   * @param url the ValueSet canonical URL
   * @param version the optional ValueSet version
   * @return the responsible terminology manager
   * @throws UnsupportedTerminologyException if no manager supports the referenced ValueSet
   * @throws UnknownScopeException if the scope is not supported
   */
  private TerminologyManager resolveValueSetTerminologyManager(
      String scope, String url, String version)
      throws UnsupportedTerminologyException, UnknownScopeException {

    // 1. Wir schauen, ob der TerminologyManager bereits in der Map für explizitdefinierte
    // ValueSets hinterlegt ist
    TerminologyManager tm = this.getTerminologyManagerValueSetMap().get(url);

    // 2. Wenn nicht, fragen wir alle TerminologyManager, ob sie das ValueSet unterstützen
    if (tm == null) {
      for (TerminologyManager manager : terminologyManagerList) {
        if (manager.vs().isValueSetSupported(scope, url, version)) {
          tm = manager;
          break;
        }
      }

      // Falls wir immer noch keinen zuständigen TerminologyManager gefunden haben, werfen wir
      // eine UnsupportedTerminologyException
      if (tm == null) {
        throw new UnsupportedTerminologyException(
            "The ValueSet with the url '" + url + "' is not supported by the service.");
      }
    }

    return tm;
  }

  /**
   * Expands an inline ValueSet and stores the result in the expansion cache.
   *
   * @param scope the scope to expand within
   * @param vsExpandParameter the expansion parameters containing an inline ValueSet
   * @return the expanded ValueSet
   * @throws NotImplementedException if expansion is not supported by the responsible manager
   * @throws UnknownScopeException if the scope is not supported
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws UnknownConceptException if a referenced concept is unknown
   */
  private ValueSet expandProvidedValueSet(String scope, VsExpandParameter vsExpandParameter)
      throws NotImplementedException,
          UnknownScopeException,
          UnsupportedTerminologyException,
          UnknownConceptException {

    // Expandieren des übergebenen ValueSets
    ValueSet result =
        CombiningVsProvidedExpandOperation.expandExplicitValueSet(
            this, vsExpandParameter, scope, languageSystemDefault);

    // Cache the expansion result
    cacheManager.putExpansion(scope, vsExpandParameter, result);

    return result;
  }

  /**
   * Checks whether the expand parameters contain a non-empty {@code url} parameter.
   *
   * @param vsExpandParameter the expansion parameters to inspect
   * @return {@code true} if a non-empty URL parameter is present, otherwise {@code false}
   */
  private boolean hasUrlParameter(VsExpandParameter vsExpandParameter) {
    return vsExpandParameter.getUrlParam() != null && !vsExpandParameter.getUrlParam().isEmpty();
  }

  /**
   * Checks whether the expand parameters contain a non-empty inline {@code valueSet}.
   *
   * @param vsExpandParameter the expansion parameters to inspect
   * @return {@code true} if a non-empty inline ValueSet is present, otherwise {@code false}
   */
  private boolean hasInlineValueSet(VsExpandParameter vsExpandParameter) {
    return vsExpandParameter.getValueSetParam() != null
        && !vsExpandParameter.getValueSetParam().isEmpty();
  }

  // ==========================================================================================

  /**
   * Validates a code against a ValueSet using the supplied parameters.
   *
   * <p>Results are served from cache if available.
   *
   * @param scope the scope to validate within
   * @param vsValidateCodeParameter the validation parameters
   * @return the validation result
   * @throws InvalidRequestException if the parameters are invalid
   * @throws NotImplementedException if the operation is not supported by the responsible manager
   * @throws UnsupportedTerminologyException if the referenced terminology is not supported
   * @throws UnknownScopeException if the scope is not supported
   */
  public ValidateCodeResult validateCodeVs(
      String scope, VsValidateCodeParameter vsValidateCodeParameter)
      throws InvalidRequestException,
          NotImplementedException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug(vsValidateCodeParameter.toString());

    // Check cache first
    ValidateCodeResult cachedResult = cacheManager.getValidation(scope, vsValidateCodeParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // Parametervalidierung
    CombiningVsValidateCodeParameterValidator.validateParameters(vsValidateCodeParameter);

    // Validierungs-Operation ausführen und das Ergebnis zurückgeben
    ValidateCodeResult result =
        CombiningVsValidateCodeOperation.validateCode(this, vsValidateCodeParameter, scope);

    // Cache the validation result
    cacheManager.putValidation(scope, vsValidateCodeParameter, result);

    return result;
  }

  // ----------------------------------------------------------------------------------------
  // ConceptMap methods
  // ----------------------------------------------------------------------------------------

  /**
   * Returns the ConceptMap with the given ID within the specified scope and retrieval mode.
   *
   * @param scope the scope the ConceptMap must belong to
   * @param id the FHIR resource ID of the ConceptMap
   * @param retrievalMode whether to return the full or stripped resource
   * @throws ResourceNotFoundException if no matching ConceptMap is found
   */
  public ConceptMap getConceptMap(String scope, IdType id, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException {

    return terminologyManagerList.parallelStream()
        .map(
            manager -> {
              try {
                return manager.cm().getConceptMap(scope, id, retrievalMode);
              } catch (Exception _) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    String.format(RESOURCE_NOT_FOUND_MESSAGE_TEMPLATE, "ConceptMap", id, scope)));
  }

  /**
   * Returns the full ConceptMap with the given ID within the specified scope.
   *
   * @param scope the scope the ConceptMap must belong to
   * @param id the FHIR resource ID of the ConceptMap
   * @throws ResourceNotFoundException if no matching ConceptMap is found
   */
  public ConceptMap getConceptMap(String scope, IdType id) throws ResourceNotFoundException {

    return getConceptMap(scope, id, ResourceRetrievalMode.FULL);
  }

  // ==========================================================================================

  /**
   * Searches for ConceptMaps within the given scope using the supplied search parameters.
   *
   * <p>Results are served from cache if available.
   *
   * @param scope the scope to search in
   * @param cmSearchParameter the search parameters
   * @return the IDs of all matching ConceptMaps
   * @throws NotImplementedException if the operation is not supported
   */
  public List<IdType> searchConceptMap(String scope, CmSearchParameter cmSearchParameter)
      throws NotImplementedException {

    // Check cache first
    List<IdType> cachedResult = cacheManager.getSearch(scope, cmSearchParameter);
    if (cachedResult != null) {
      return cachedResult;
    }

    // TODO parameter validation should be done as a post action of the builder in the Parameter
    // class itself
    // Parametervalidierung
    CombiningCmSearchParameterValidator.validateParameters(cmSearchParameter);

    // Suche nach CodeSystemen anhand der verbliebenen Parameter innerhalb des Suchindex
    List<IdType> tempResultList =
        CombiningCmSearchOperation.searchConceptMaps(this, cmSearchParameter, scope);

    // Cache the result
    cacheManager.putSearch(scope, cmSearchParameter, tempResultList);

    return tempResultList;
  }

  // ==========================================================================================
}
