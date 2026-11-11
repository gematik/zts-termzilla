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
package de.gematik.zts.termzilla.server.filter;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CdnOptimizedFilter}.
 *
 * <p>Verifies that the filter strips {@code Accept} and {@code Accept-Language} headers when
 * CDN-optimized mode is enabled, and passes all headers through unchanged when disabled.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CdnOptimizedFilter Tests")
class CdnOptimizedFilterTest {

  @Mock private ConfigProperties configProperties;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  // ==================================================================================================
  // Filter Disabled Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Filter Disabled Tests")
  class FilterDisabledTests {

    @Test
    @DisplayName("Should pass original request unchanged when CDN-optimized mode is disabled")
    void doesNotStripHeadersWhenDisabled() throws Exception {
      // GIVEN
      when(configProperties.isCachingCdnOptimized()).thenReturn(false);
      CdnOptimizedFilter filter = new CdnOptimizedFilter(configProperties);

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      verify(filterChain).doFilter(request, response);
    }
  }

  // ==================================================================================================
  // Filter Enabled – Header Stripping Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Filter Enabled – Header Stripping Tests")
  class FilterEnabledTests {

    private CdnOptimizedFilter buildEnabledFilter() {
      when(configProperties.isCachingCdnOptimized()).thenReturn(true);
      return new CdnOptimizedFilter(configProperties);
    }

    @Test
    @DisplayName("Should wrap request so that Accept header returns null")
    void stripsAcceptHeaderWhenEnabled() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();
      // lenient: the stub establishes what the mock *would* return if the wrapper called super.
      // The wrapper must return null without calling super – if it does call super, the test fails.
      lenient().when(request.getHeader(ACCEPT)).thenReturn("application/fhir+xml");

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN — capture the wrapped request forwarded to the chain
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      assertThat(wrapped.getHeader(ACCEPT)).isNull();
    }

    @Test
    @DisplayName("Should wrap request so that Accept-Language header returns null")
    void stripsAcceptLanguageHeaderWhenEnabled() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();
      // lenient: the stub establishes what the mock *would* return if the wrapper called super.
      lenient().when(request.getHeader(ACCEPT_LANGUAGE)).thenReturn("de-DE");

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      assertThat(wrapped.getHeader(ACCEPT_LANGUAGE)).isNull();
    }

    @Test
    @DisplayName("Should strip Accept header regardless of case")
    void stripsHeaderCaseInsensitively() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();
      // lenient: covers the case-folded variant – the wrapper must suppress it regardless of case.
      lenient().when(request.getHeader(ACCEPT)).thenReturn("application/json");

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      assertThat(wrapped.getHeader(ACCEPT)).isNull();
    }

    @Test
    @DisplayName("Should pass non-stripped headers through unchanged")
    void passesOtherHeadersThroughWhenEnabled() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();
      when(request.getHeader(CONTENT_TYPE)).thenReturn("application/json");

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      assertThat(wrapped.getHeader(CONTENT_TYPE)).isEqualTo("application/json");
    }

    @Test
    @DisplayName("Should return empty enumeration for getHeaders on stripped header")
    void getHeadersReturnsEmptyEnumerationForStrippedHeader() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      Enumeration<String> acceptHeaders = wrapped.getHeaders(ACCEPT);
      assertThat(Collections.list(acceptHeaders)).isEmpty();
    }

    @Test
    @DisplayName("Should exclude stripped headers from getHeaderNames")
    void getHeaderNamesExcludesStrippedHeaders() throws Exception {
      // GIVEN
      CdnOptimizedFilter filter = buildEnabledFilter();
      when(request.getHeaderNames())
          .thenReturn(Collections.enumeration(List.of(ACCEPT, ACCEPT_LANGUAGE, CONTENT_TYPE)));

      // WHEN
      filter.doFilter(request, response, filterChain);

      // THEN
      ArgumentCaptor<ServletRequest> captor = ArgumentCaptor.forClass(ServletRequest.class);
      verify(filterChain).doFilter(captor.capture(), org.mockito.ArgumentMatchers.any());
      HttpServletRequest wrapped = (HttpServletRequest) captor.getValue();
      List<String> headerNames = Collections.list(wrapped.getHeaderNames());
      assertThat(headerNames).doesNotContain(ACCEPT, ACCEPT_LANGUAGE).contains(CONTENT_TYPE);
    }
  }

  // ==================================================================================================
  // Non-HTTP Request Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Non-HTTP Request Tests")
  class NonHttpRequestTests {

    @Test
    @DisplayName("Should pass non-HTTP request unchanged even when CDN-optimized mode is enabled")
    void passesNonHttpRequestThroughWhenEnabled() throws Exception {
      // GIVEN
      when(configProperties.isCachingCdnOptimized()).thenReturn(true);
      CdnOptimizedFilter filter = new CdnOptimizedFilter(configProperties);
      ServletRequest nonHttpRequest = mock(ServletRequest.class);

      // WHEN
      filter.doFilter(nonHttpRequest, response, filterChain);

      // THEN
      verify(filterChain).doFilter(nonHttpRequest, response);
    }
  }
}
