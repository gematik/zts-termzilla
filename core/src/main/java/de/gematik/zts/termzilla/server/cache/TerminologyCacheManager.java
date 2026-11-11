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
package de.gematik.zts.termzilla.server.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.configuration.CacheConfigProperties;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Central cache manager for terminology operations.
 *
 * <p>Provides type-safe caching for expensive terminology operations:
 *
 * <ul>
 *   <li>ValueSet expansions ($expand)
 *   <li>ValueSet code validations ($validate-code)
 *   <li>CodeSystem code validations ($validate-code)
 *   <li>Code lookups ($lookup)
 * </ul>
 *
 * <p>Uses Caffeine for high-performance caching with configurable TTL and size limits.
 */
@Slf4j
public class TerminologyCacheManager {

  public static final String CACHE_NAME_LOOKUPS = "lookups";
  public static final String CACHE_NAME_SEARCHES = "searches";
  public static final String CACHE_NAME_EXPANSIONS = "expansions";
  @Getter private final Cache<ExpansionCacheKey, ValueSet> expansionCache;

  @Getter private final Cache<ValidationCacheKey, ValidateCodeResult> validationCache;

  @Getter private final Cache<CsValidationCacheKey, ValidateCodeResult> csValidationCache;

  @Getter private final Cache<LookupCacheKey, CsLookupResult> lookupCache;

  @Getter private final Cache<SearchCacheKey, List<IdType>> searchCache;

  /** Creates a cache manager with default configuration. */
  public TerminologyCacheManager() {
    this(new CacheConfigProperties());
  }

  /**
   * Creates a cache manager with custom configuration.
   *
   * @param config Cache configuration properties
   */
  public TerminologyCacheManager(CacheConfigProperties config) {
    log.info("Initializing TerminologyCacheManager with Caffeine");

    this.expansionCache =
        buildCache(
            CACHE_NAME_EXPANSIONS,
            config.getExpansion().getMaxSize(),
            config.getExpansion().getTtlMinutes());

    this.validationCache =
        buildCache(
            "validations",
            config.getValidation().getMaxSize(),
            config.getValidation().getTtlMinutes());

    this.csValidationCache =
        buildCache(
            "cs-validations",
            config.getValidation().getMaxSize(),
            config.getValidation().getTtlMinutes());

    this.lookupCache =
        buildCache(
            CACHE_NAME_LOOKUPS,
            config.getLookup().getMaxSize(),
            config.getLookup().getTtlMinutes());

    this.searchCache =
        buildCache(
            CACHE_NAME_SEARCHES,
            config.getSearch().getMaxSize(),
            config.getSearch().getTtlMinutes());

    log.info("Cache manager initialized:");
    logCacheConfig(
        CACHE_NAME_EXPANSIONS,
        config.getExpansion().getMaxSize(),
        config.getExpansion().getTtlMinutes());
    logCacheConfig(
        "validations (VS)",
        config.getValidation().getMaxSize(),
        config.getValidation().getTtlMinutes());
    logCacheConfig(
        "validations (CS)",
        config.getValidation().getMaxSize(),
        config.getValidation().getTtlMinutes());
    logCacheConfig(
        CACHE_NAME_LOOKUPS, config.getLookup().getMaxSize(), config.getLookup().getTtlMinutes());
    logCacheConfig(
        CACHE_NAME_SEARCHES, config.getSearch().getMaxSize(), config.getSearch().getTtlMinutes());
  }

  // ========================================================================================
  // Expansion Cache Methods
  // ========================================================================================

  /**
   * Gets a cached ValueSet expansion.
   *
   * @param scope Terminology scope
   * @param params Expansion parameters
   * @return Cached ValueSet if present, null otherwise
   */
  public ValueSet getExpansion(String scope, VsExpandParameter params) {
    ExpansionCacheKey key = new ExpansionCacheKey(scope, params);
    ValueSet result = expansionCache.getIfPresent(key);
    if (result != null) {
      log.debug("Cache hit for expansion: {}", key);
    }
    return result;
  }

  /**
   * Caches a ValueSet expansion result.
   *
   * @param scope Terminology scope
   * @param params Expansion parameters
   * @param result Expansion result to cache
   */
  public void putExpansion(String scope, VsExpandParameter params, ValueSet result) {
    ExpansionCacheKey key = new ExpansionCacheKey(scope, params);
    expansionCache.put(key, result);
    log.debug("Cached expansion result: {}", key);
  }

  // ========================================================================================
  // Validation Cache Methods
  // ========================================================================================

  /**
   * Gets a cached validation result.
   *
   * @param scope Terminology scope
   * @param params Validation parameters
   * @return Cached validation result if present, null otherwise
   */
  public ValidateCodeResult getValidation(String scope, VsValidateCodeParameter params) {
    ValidationCacheKey key = new ValidationCacheKey(scope, params);
    ValidateCodeResult result = validationCache.getIfPresent(key);
    if (result != null) {
      log.debug("Cache hit for validation: {}", key);
    }
    return result;
  }

  /**
   * Caches a validation result.
   *
   * @param scope Terminology scope
   * @param params Validation parameters
   * @param result Validation result to cache
   */
  public void putValidation(
      String scope, VsValidateCodeParameter params, ValidateCodeResult result) {
    ValidationCacheKey key = new ValidationCacheKey(scope, params);
    validationCache.put(key, result);
    log.debug("Cached validation result: {}", key);
  }

  // ========================================================================================
  // Lookup Cache Methods
  // ========================================================================================

  /**
   * Gets a cached lookup result.
   *
   * @param scope Terminology scope
   * @param params Lookup parameters
   * @return Cached lookup result if present, null otherwise
   */
  public CsLookupResult getLookup(String scope, CsLookupParameter params) {
    LookupCacheKey key = new LookupCacheKey(scope, params);
    CsLookupResult result = lookupCache.getIfPresent(key);
    if (result != null) {
      log.debug("Cache hit for lookup: {}", key);
    }
    return result;
  }

  /**
   * Caches a lookup result.
   *
   * @param scope Terminology scope
   * @param params Lookup parameters
   * @param result Lookup result to cache
   */
  public void putLookup(String scope, CsLookupParameter params, CsLookupResult result) {
    LookupCacheKey key = new LookupCacheKey(scope, params);
    lookupCache.put(key, result);
    log.debug("Cached lookup result: {}", key);
  }

  // ========================================================================================
  // CodeSystem Validation Cache Methods
  // ========================================================================================

  /**
   * Gets a cached CodeSystem validation result.
   *
   * @param scope Terminology scope
   * @param params Validation parameters
   * @return Cached validation result if present, null otherwise
   */
  public ValidateCodeResult getCsValidation(String scope, CsValidateCodeParameter params) {
    CsValidationCacheKey key = new CsValidationCacheKey(scope, params);
    ValidateCodeResult result = csValidationCache.getIfPresent(key);
    if (result != null) {
      log.debug("Cache hit for CS validation: {}", key);
    }
    return result;
  }

  /**
   * Caches a CodeSystem validation result.
   *
   * @param scope Terminology scope
   * @param params Validation parameters
   * @param result Validation result to cache
   */
  public void putCsValidation(
      String scope, CsValidateCodeParameter params, ValidateCodeResult result) {
    CsValidationCacheKey key = new CsValidationCacheKey(scope, params);
    csValidationCache.put(key, result);
    log.debug("Cached CS validation result: {}", key);
  }

  // ========================================================================================
  // Search Cache Methods
  // ========================================================================================

  /** Gets a cached search result. */
  public List<IdType> getSearch(String scope, TerminologyResourceSearchParameter params) {
    SearchCacheKey key = new SearchCacheKey(scope, params);
    List<IdType> result = searchCache.getIfPresent(key);
    if (result != null) {
      log.debug("Cache hit for search: {}", key);
    }
    return result;
  }

  /** Caches a search result. */
  public void putSearch(
      String scope, TerminologyResourceSearchParameter params, List<IdType> result) {
    SearchCacheKey key = new SearchCacheKey(scope, params);
    searchCache.put(key, result);
    log.debug("Cached search result: {}", key);
  }

  // ========================================================================================
  // Cache Management
  // ========================================================================================

  /** Clears all caches. */
  public void clearAll() {
    log.info("Clearing all caches");
    expansionCache.invalidateAll();
    validationCache.invalidateAll();
    csValidationCache.invalidateAll();
    lookupCache.invalidateAll();
    searchCache.invalidateAll();
  }

  /**
   * Invalidates all cache entries for a specific scope.
   *
   * @param scope The scope to invalidate
   */
  public void invalidateScope(String scope) {
    log.info("Invalidating all caches for scope: {}", scope);
    expansionCache.asMap().keySet().removeIf(key -> scope.equals(key.scope()));
    validationCache.asMap().keySet().removeIf(key -> scope.equals(key.scope()));
    csValidationCache.asMap().keySet().removeIf(key -> scope.equals(key.scope()));
    lookupCache.asMap().keySet().removeIf(key -> scope.equals(key.scope()));
    searchCache.asMap().keySet().removeIf(key -> scope.equals(key.scope()));
  }

  /** Logs current cache statistics. */
  public void logStatistics() {
    logCacheStats(CACHE_NAME_EXPANSIONS, expansionCache.stats(), expansionCache.estimatedSize());
    logCacheStats("validations (VS)", validationCache.stats(), validationCache.estimatedSize());
    logCacheStats("validations (CS)", csValidationCache.stats(), csValidationCache.estimatedSize());
    logCacheStats(CACHE_NAME_LOOKUPS, lookupCache.stats(), lookupCache.estimatedSize());
    logCacheStats(CACHE_NAME_SEARCHES, searchCache.stats(), searchCache.estimatedSize());
  }

  // ========================================================================================
  // Helper Methods
  // ========================================================================================

  private <K, V> Cache<K, V> buildCache(String name, int maxSize, int ttlMinutes) {
    log.debug("building cache for {} with maxSize={} and ttl={}min", name, maxSize, ttlMinutes);
    return Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
        .recordStats()
        .build();
  }

  private void logCacheConfig(String name, int maxSize, int ttlMinutes) {
    log.info("  - {}: maxSize={}, TTL={}min", name, maxSize, ttlMinutes);
  }

  private void logCacheStats(String name, CacheStats stats, long size) {
    log.info(
        "Cache [{}] - Hits: {}, Misses: {}, Hit Rate: {:.2f}%, Evictions: {}, Size: {}",
        name,
        stats.hitCount(),
        stats.missCount(),
        stats.hitRate() * 100,
        stats.evictionCount(),
        size);
  }
}
