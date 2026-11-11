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
package de.gematik.zts.termzilla.server.interceptor;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CACHE_CONTROL;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.IF_NONE_MATCH;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.VARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link HttpCachingInterceptor}.
 *
 * <p>Covers {@code Cache-Control} header construction and {@code If-None-Match} / ETag negotiation
 * via the HAPI FHIR server hook methods.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpCachingInterceptor Tests")
class HttpCachingInterceptorTest {

  @Mock private ConfigProperties configProperties;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  private HttpCachingInterceptor interceptor;

  private static final String CONFIG_AND_DATA_STATE_HASH = "ABCD1234";

  @BeforeEach
  void setUp() {
    interceptor = new HttpCachingInterceptor(configProperties, CONFIG_AND_DATA_STATE_HASH);
  }

  /**
   * Computes the expected ETag for a given URL using the same algorithm as {@link
   * HttpCachingInterceptor}.
   */
  private String computeExpectedETag(String url) {
    String input = CONFIG_AND_DATA_STATE_HASH + "#" + url;
    return "W/\"" + DigestUtils.sha256Hex(input.getBytes(StandardCharsets.UTF_8)) + "\"";
  }

  // ==================================================================================================
  // Cache-Control Header Tests
  // ==================================================================================================

  @Nested
  @DisplayName("buildCacheControlHeader Tests")
  class BuildCacheControlHeaderTests {

    @Test
    @DisplayName("Should return no-store when caching is disabled")
    void setsNoCacheHeaderWhenCachingDisabled() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(false);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).isEqualTo("no-store");
    }

    @Test
    @DisplayName("Should return public, max-age when caching is enabled with minimal config")
    void setsPublicMaxAgeWhenCachingEnabled() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(600);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(-1);
      when(configProperties.isCachingMustRevalidate()).thenReturn(false);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(-1);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).isEqualTo("public, max-age=600");
    }

    @Test
    @DisplayName("Should include s-maxage when shared max-age is positive")
    void includesSMaxAgeWhenSharedMaxAgeIsPositive() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(300);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(600);
      when(configProperties.isCachingMustRevalidate()).thenReturn(false);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(-1);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).contains("s-maxage=600");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("omittedDirectiveCases")
    @DisplayName("Should omit directive when related config is disabled")
    void omitsDirectiveWhenRelatedConfigIsDisabled(
        String caseName,
        int sharedMaxAge,
        boolean mustRevalidate,
        int staleWhileRevalidate,
        String omittedDirective) {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(600);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(sharedMaxAge);
      when(configProperties.isCachingMustRevalidate()).thenReturn(mustRevalidate);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(staleWhileRevalidate);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).as("Case: %s", caseName).doesNotContain(omittedDirective);
    }

    private static Stream<Arguments> omittedDirectiveCases() {
      return Stream.of(
          arguments("omitsSMaxAgeWhenSharedMaxAgeIsMinusOne", -1, false, -1, "s-maxage"),
          arguments("omitsMustRevalidateWhenDisabled", -1, false, 60, "must-revalidate"),
          arguments(
              "omitsStaleWhileRevalidateWhenMinusOne", -1, true, -1, "stale-while-revalidate"));
    }

    @Test
    @DisplayName("Should include must-revalidate when enabled")
    void includesMustRevalidateWhenEnabled() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(600);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(-1);
      when(configProperties.isCachingMustRevalidate()).thenReturn(true);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(-1);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).contains("must-revalidate");
    }

    @Test
    @DisplayName("Should include stale-while-revalidate when positive")
    void includesStaleWhileRevalidateWhenPositive() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(600);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(-1);
      when(configProperties.isCachingMustRevalidate()).thenReturn(false);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(60);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).contains("stale-while-revalidate=60");
    }

    @Test
    @DisplayName("Should produce full directive string with all options enabled")
    void producesFullDirectiveStringWithAllOptionsEnabled() {
      // GIVEN
      when(configProperties.isCachingEnabled()).thenReturn(true);
      when(configProperties.getCachingMaxAge()).thenReturn(300);
      when(configProperties.getCachingSharedMaxAge()).thenReturn(600);
      when(configProperties.isCachingMustRevalidate()).thenReturn(true);
      when(configProperties.getCachingStaleWhileRevalidate()).thenReturn(-1);

      // WHEN
      String header = HttpCachingInterceptor.buildCacheControlHeader(configProperties);

      // THEN
      assertThat(header).isEqualTo("public, max-age=300, s-maxage=600, must-revalidate");
    }
  }

  @Nested
  @DisplayName("outgoingResponse – Cache-Control Integration Tests")
  class OutgoingResponseCacheControlTests {

    @Test
    @DisplayName("Should set Cache-Control header on GET response")
    void setsCacheControlOnGetResponse() {
      // GIVEN
      // isCachingEnabled() returns false by default (Mockito mock default for boolean),
      // so cacheControlHeader was pre-computed as "no-store" during construction.
      when(request.getMethod()).thenReturn("GET");
      when(request.getAttribute("calculatedETag")).thenReturn("W/\"abc123\"");

      // WHEN
      interceptor.outgoingResponse(request, response);

      // THEN
      ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
      verify(response)
          .setHeader(org.mockito.ArgumentMatchers.eq(CACHE_CONTROL), headerValue.capture());
      assertThat(headerValue.getValue()).isEqualTo("no-store");
    }

    @Test
    @DisplayName("Should not set Cache-Control on POST request")
    void doesNotSetCacheControlOnPostRequest() {
      // GIVEN
      when(request.getMethod()).thenReturn("POST");

      // WHEN
      interceptor.outgoingResponse(request, response);

      // THEN
      verify(response, never())
          .setHeader(
              org.mockito.ArgumentMatchers.eq(CACHE_CONTROL),
              org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Should add Vary header with Accept and Accept-Language on GET response")
    void setsVaryHeaderOnGetResponse() {
      // GIVEN
      when(request.getMethod()).thenReturn("GET");
      when(request.getAttribute("calculatedETag")).thenReturn("W/\"abc123\"");

      // WHEN
      interceptor.outgoingResponse(request, response);

      // THEN
      verify(response).addHeader(VARY, HttpCachingInterceptor.VARY_HEADERS);
    }

    @Test
    @DisplayName("Should not add Vary header on POST request")
    void doesNotSetVaryHeaderOnPostRequest() {
      // GIVEN
      when(request.getMethod()).thenReturn("POST");

      // WHEN
      interceptor.outgoingResponse(request, response);

      // THEN
      verify(response, never()).addHeader(eq(VARY), anyString());
    }

    @Test
    @DisplayName("Should not set Vary header when CDN-optimized mode is enabled")
    void doesNotSetVaryHeaderInCdnOptimizedMode() {
      // GIVEN
      when(configProperties.isCachingCdnOptimized()).thenReturn(true);
      HttpCachingInterceptor cdnInterceptor =
          new HttpCachingInterceptor(configProperties, "ABCD1234");
      when(request.getMethod()).thenReturn("GET");
      when(request.getAttribute("calculatedETag")).thenReturn("W/\"abc123\"");

      // WHEN
      cdnInterceptor.outgoingResponse(request, response);

      // THEN
      verify(response, never()).addHeader(eq(VARY), anyString());
    }

    @Test
    @DisplayName("Should set Vary header when CDN-optimized mode is disabled")
    void setsVaryHeaderWhenNotCdnOptimized() {
      // GIVEN — isCachingCdnOptimized() returns false by default (Mockito mock default for boolean)
      HttpCachingInterceptor defaultInterceptor =
          new HttpCachingInterceptor(configProperties, "ABCD1234");
      when(request.getMethod()).thenReturn("GET");
      when(request.getAttribute("calculatedETag")).thenReturn("W/\"abc123\"");

      // WHEN
      defaultInterceptor.outgoingResponse(request, response);

      // THEN
      verify(response).addHeader(VARY, HttpCachingInterceptor.VARY_HEADERS);
    }
  }

  // ==================================================================================================
  // If-None-Match / ETag Negotiation Tests
  // ==================================================================================================

  @Nested
  @DisplayName("If-None-Match / ETag Negotiation Tests")
  class IncomingRequestIfNoneMatchTests {

    @BeforeEach
    void setUpGetRequest() {
      when(request.getMethod()).thenReturn("GET");
      when(request.getRequestURL())
          .thenReturn(new StringBuffer("http://localhost/fhir/CodeSystem"));
    }

    @Test
    @DisplayName("Should return 304 when ETag matches If-None-Match header")
    void returns304WhenETagMatches() {
      // GIVEN — calculate expected ETag using the same algorithm as the interceptor
      String expectedETag = computeExpectedETag("http://localhost/fhir/CodeSystem");
      when(request.getHeader(IF_NONE_MATCH)).thenReturn(expectedETag);

      // WHEN
      boolean result = interceptor.incomingRequestProcessed(request, response);

      // THEN
      assertThat(result).isFalse();
      verify(response).setStatus(304);
    }

    @Test
    @DisplayName("Should return 200 and continue processing when ETag does not match")
    void returns200WhenETagDoesNotMatch() {
      // GIVEN
      when(request.getHeader(IF_NONE_MATCH)).thenReturn("W/\"does-not-match\"");

      // WHEN
      boolean result = interceptor.incomingRequestProcessed(request, response);

      // THEN
      assertThat(result).isTrue();
      verify(response, never()).setStatus(304);
    }

    @Test
    @DisplayName("Should return 304 for wildcard If-None-Match")
    void returns304ForWildcard() {
      // GIVEN
      when(request.getHeader(IF_NONE_MATCH)).thenReturn("*");

      // WHEN
      boolean result = interceptor.incomingRequestProcessed(request, response);

      // THEN
      assertThat(result).isFalse();
      verify(response).setStatus(304);
    }

    @Test
    @DisplayName("Should return 304 for strong ETag form matching weak ETag")
    void returns304ForStrongETagForm() {
      // GIVEN — strong ETag with same opaque-tag as the computed weak ETag (strip W/ prefix)
      String weakETag = computeExpectedETag("http://localhost/fhir/CodeSystem");
      String opaqueTag = weakETag.substring(2); // strip "W/" to get the strong form
      when(request.getHeader(IF_NONE_MATCH)).thenReturn(opaqueTag);

      // WHEN
      boolean result = interceptor.incomingRequestProcessed(request, response);

      // THEN
      assertThat(result).isFalse();
      verify(response).setStatus(304);
    }
  }
}
