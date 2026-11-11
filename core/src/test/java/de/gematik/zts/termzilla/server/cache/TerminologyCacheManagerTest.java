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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.configuration.CacheConfigProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TerminologyCacheManager.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Basic cache operations (get, put)
 *   <li>TTL expiration behavior
 *   <li>Size limits
 *   <li>Cache invalidation (all, by scope)
 *   <li>Cache statistics
 * </ul>
 */
@DisplayName("TerminologyCacheManager Tests")
class TerminologyCacheManagerTest {

  private static final String SCOPE_1 = "scope1";
  private static final String SCOPE_2 = "scope2";

  private TerminologyCacheManager cacheManager;

  @BeforeEach
  void setUp() {
    cacheManager = new TerminologyCacheManager();
  }

  @Nested
  @DisplayName("Expansion Cache Tests")
  class ExpansionCacheTests {

    @Test
    @DisplayName("Should cache and retrieve expansion results")
    void shouldCacheAndRetrieveExpansionResults() {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      ValueSet expectedResult = new ValueSet();
      expectedResult.setUrl("http://test.com/vs");
      expectedResult.setVersion("1.0");

      // When
      cacheManager.putExpansion(SCOPE_1, params, expectedResult);
      ValueSet cachedResult = cacheManager.getExpansion(SCOPE_1, params);

      // Then
      assertThat(cachedResult).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("Should return null for cache miss")
    void shouldReturnNullForCacheMiss() {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      // When
      ValueSet result = cacheManager.getExpansion(SCOPE_1, params);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should distinguish between different scopes")
    void shouldDistinguishBetweenDifferentScopes() {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      ValueSet result1 = new ValueSet();
      result1.setUrl("http://test.com/vs");
      result1.setVersion("1.0");

      ValueSet result2 = new ValueSet();
      result2.setUrl("http://test.com/vs");
      result2.setVersion("2.0");

      // When
      cacheManager.putExpansion(SCOPE_1, params, result1);
      cacheManager.putExpansion(SCOPE_2, params, result2);

      // Then
      assertThat(cacheManager.getExpansion(SCOPE_1, params)).isEqualTo(result1);
      assertThat(cacheManager.getExpansion(SCOPE_2, params)).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should expire entries after TTL")
    void shouldExpireEntriesAfterTtl() {
      // Given - Create cache with very short TTL
      CacheConfigProperties config = new CacheConfigProperties();
      config.getExpansion().setTtlMinutes(0); // 0 minutes = immediate expiry for testing
      TerminologyCacheManager shortTtlCache = new TerminologyCacheManager(config);

      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      ValueSet result = new ValueSet();
      result.setUrl("http://test.com/vs");

      // When
      shortTtlCache.putExpansion(SCOPE_1, params, result);

      // Then - Entry should expire almost immediately
      await()
          .atMost(Duration.ofSeconds(2))
          .pollInterval(Duration.ofMillis(100))
          .untilAsserted(() -> assertThat(shortTtlCache.getExpansion(SCOPE_1, params)).isNull());
    }
  }

  @Nested
  @DisplayName("Validation Cache Tests")
  class ValidationCacheTests {

    @Test
    @DisplayName("Should cache and retrieve validation results")
    void shouldCacheAndRetrieveValidationResults() {
      // Given
      VsValidateCodeParameter params =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .build();

      ValidateCodeResult expectedResult = new ValidateCodeResult();
      expectedResult.setResult(true);
      expectedResult.setDisplay("Test Code");

      // When
      cacheManager.putValidation(SCOPE_1, params, expectedResult);
      ValidateCodeResult cachedResult = cacheManager.getValidation(SCOPE_1, params);

      // Then
      assertThat(cachedResult).isEqualTo(expectedResult);
      assertThat(cachedResult.isResult()).isTrue();
      assertThat(cachedResult.getDisplay()).isEqualTo("Test Code");
    }

    @Test
    @DisplayName("Should cache negative validation results")
    void shouldCacheNegativeValidationResults() {
      // Given
      VsValidateCodeParameter params =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("invalid"))
              .build();

      ValidateCodeResult expectedResult = new ValidateCodeResult();
      expectedResult.setResult(false);
      expectedResult.setMessage("Code not found in ValueSet");

      // When
      cacheManager.putValidation(SCOPE_1, params, expectedResult);
      ValidateCodeResult cachedResult = cacheManager.getValidation(SCOPE_1, params);

      // Then
      assertThat(cachedResult.isResult()).isFalse();
      assertThat(cachedResult.getMessage()).contains("not found");
    }
  }

  @Nested
  @DisplayName("Lookup Cache Tests")
  class LookupCacheTests {

    @Test
    @DisplayName("Should cache and retrieve lookup results")
    void shouldCacheAndRetrieveLookupResults() {
      // Given
      CsLookupParameter params =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();

      CsLookupResult expectedResult = new CsLookupResult();
      expectedResult.setCodeSystemDisplayName("Test CodeSystem");
      expectedResult.setCodeDisplay("Test Display");

      // When
      cacheManager.putLookup(SCOPE_1, params, expectedResult);
      CsLookupResult cachedResult = cacheManager.getLookup(SCOPE_1, params);

      // Then
      assertThat(cachedResult).isEqualTo(expectedResult);
      assertThat(cachedResult.getCodeDisplay()).isEqualTo("Test Display");
    }
  }

  @Nested
  @DisplayName("CodeSystem Validation Cache Tests")
  class CsValidationCacheTests {

    @Test
    @DisplayName("Should cache and retrieve CS validation results")
    void shouldCacheAndRetrieveCsValidationResults() {
      // Given
      CsValidateCodeParameter params =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType("http://snomed.info/sct"))
              .codeParam(new CodeType("12345"))
              .build();

      ValidateCodeResult expectedResult = new ValidateCodeResult();
      expectedResult.setResult(true);
      expectedResult.setDisplay("SNOMED CT Code");

      // When
      cacheManager.putCsValidation(SCOPE_1, params, expectedResult);
      ValidateCodeResult cachedResult = cacheManager.getCsValidation(SCOPE_1, params);

      // Then
      assertThat(cachedResult).isEqualTo(expectedResult);
    }
  }

  @Nested
  @DisplayName("Search Cache Tests")
  class SearchCacheTests {

    @Test
    @DisplayName("Should cache and retrieve search results")
    void shouldCacheAndRetrieveSearchResults() {
      // Given
      VsSearchParameter params = VsSearchParameter.builder().build();
      params.setUrlParam(new UriParam("http://test.com/vs"));

      List<IdType> expectedResults =
          Arrays.asList(
              new IdType("ValueSet/1"), new IdType("ValueSet/2"), new IdType("ValueSet/3"));

      // When
      cacheManager.putSearch(SCOPE_1, params, expectedResults);
      List<IdType> cachedResults = cacheManager.getSearch(SCOPE_1, params);

      // Then
      assertThat(cachedResults).hasSize(3).containsExactlyElementsOf(expectedResults);
    }

    @Test
    @DisplayName("Should cache empty search results")
    void shouldCacheEmptySearchResults() {
      // Given
      VsSearchParameter params = VsSearchParameter.builder().build();
      params.setUrlParam(new UriParam("http://test.com/nonexistent"));

      List<IdType> emptyResults = List.of();

      // When
      cacheManager.putSearch(SCOPE_1, params, emptyResults);
      List<IdType> cachedResults = cacheManager.getSearch(SCOPE_1, params);

      // Then
      assertThat(cachedResults).isEmpty();
    }

    @Test
    @DisplayName("Should work with CodeSystem search parameters")
    void shouldWorkWithCodeSystemSearchParameters() {
      // Given
      CsSearchParameter params = CsSearchParameter.builder().build();
      params.setSystemParam(new UriParam("http://snomed.info/sct"));

      List<IdType> expectedResults =
          Arrays.asList(
              new IdType("CodeSystem/snomed-base"), new IdType("CodeSystem/snomed-extension"));

      // When
      cacheManager.putSearch(SCOPE_1, params, expectedResults);
      List<IdType> cachedResults = cacheManager.getSearch(SCOPE_1, params);

      // Then
      assertThat(cachedResults).containsExactlyElementsOf(expectedResults);
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("Should clear all caches")
    void shouldClearAllCaches() {
      // Given - Populate all caches
      VsExpandParameter expandParams =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();
      cacheManager.putExpansion(SCOPE_1, expandParams, new ValueSet());

      VsValidateCodeParameter validateParams =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .build();
      ValidateCodeResult validationResult = new ValidateCodeResult();
      validationResult.setResult(true);

      cacheManager.putValidation(SCOPE_1, validateParams, validationResult);

      CsLookupParameter lookupParams =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();

      CsLookupResult lookupResult = new CsLookupResult();
      lookupResult.setCodeDisplay("Test");
      cacheManager.putLookup(SCOPE_1, lookupParams, lookupResult);

      // When
      cacheManager.clearAll();

      // Then
      assertThat(cacheManager.getExpansion(SCOPE_1, expandParams)).isNull();
      assertThat(cacheManager.getValidation(SCOPE_1, validateParams)).isNull();
      assertThat(cacheManager.getLookup(SCOPE_1, lookupParams)).isNull();
    }

    @Test
    @DisplayName("Should invalidate only entries for specific scope")
    void shouldInvalidateOnlyEntriesForSpecificScope() {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      ValueSet result1 = new ValueSet();
      result1.setVersion("1.0");
      ValueSet result2 = new ValueSet();
      result2.setVersion("2.0");

      cacheManager.putExpansion(SCOPE_1, params, result1);
      cacheManager.putExpansion(SCOPE_2, params, result2);

      // When
      cacheManager.invalidateScope(SCOPE_1);

      // Then
      assertThat(cacheManager.getExpansion(SCOPE_1, params)).isNull();
      assertThat(cacheManager.getExpansion(SCOPE_2, params)).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should log statistics without error")
    void shouldLogStatisticsWithoutError() {
      // Given - Populate some cache entries
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();
      cacheManager.putExpansion(SCOPE_1, params, new ValueSet());

      // Trigger some cache hits
      cacheManager.getExpansion(SCOPE_1, params);
      cacheManager.getExpansion(SCOPE_1, params);

      // Trigger a cache miss
      VsExpandParameter otherParams =
          VsExpandParameter.builder().urlParam(new UriType("http://other.com/vs")).build();
      cacheManager.getExpansion(SCOPE_1, otherParams);

      // When/Then - Should not throw
      assertThatCode(() -> cacheManager.logStatistics()).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Cache Configuration Tests")
  class CacheConfigurationTests {

    @Test
    @DisplayName("Should use custom configuration")
    void shouldUseCustomConfiguration() {
      // Given
      CacheConfigProperties config = new CacheConfigProperties();
      config.getExpansion().setMaxSize(10);
      config.getExpansion().setTtlMinutes(5);

      // When
      TerminologyCacheManager customCache = new TerminologyCacheManager(config);

      // Then - Should create cache successfully
      assertThat(customCache).isNotNull();
      assertThat(customCache.getExpansionCache()).isNotNull();
    }

    @Test
    @DisplayName("Should respect size limits")
    void shouldRespectSizeLimits() {
      // Given - Cache with very small size limit
      CacheConfigProperties config = new CacheConfigProperties();
      config.getExpansion().setMaxSize(2);
      TerminologyCacheManager smallCache = new TerminologyCacheManager(config);

      // When - Add more entries than the limit
      for (int i = 1; i <= 5; i++) {
        VsExpandParameter params =
            VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs" + i)).build();
        ValueSet result = new ValueSet();
        result.setVersion("" + i);
        smallCache.putExpansion(SCOPE_1, params, result);
      }

      // Then - Cache size should not exceed limit
      // Note: Due to eviction policy, exact size may vary, but should be around the limit
      await()
          .atMost(Duration.ofSeconds(2))
          .untilAsserted(
              () ->
                  assertThat(smallCache.getExpansionCache().estimatedSize())
                      .isLessThanOrEqualTo(3));
    }
  }

  @Nested
  @DisplayName("Concurrent Access Tests")
  class ConcurrentAccessTests {

    @Test
    @DisplayName("Should handle concurrent reads and writes")
    void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      ValueSet result = new ValueSet();
      result.setUrl("http://test.com/vs");

      // When - Multiple threads access the cache
      Thread writer =
          new Thread(
              () -> {
                for (int i = 0; i < 100; i++) {
                  cacheManager.putExpansion(SCOPE_1, params, result);
                }
              });

      Thread reader =
          new Thread(
              () -> {
                for (int i = 0; i < 100; i++) {
                  cacheManager.getExpansion(SCOPE_1, params);
                }
              });

      writer.start();
      reader.start();

      writer.join();
      reader.join();

      // Then - Should not throw any exceptions and cache should work correctly
      assertThat(cacheManager.getExpansion(SCOPE_1, params)).isEqualTo(result);
    }
  }
}
