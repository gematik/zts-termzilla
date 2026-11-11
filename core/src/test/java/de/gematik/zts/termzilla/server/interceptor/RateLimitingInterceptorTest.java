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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RateLimitingInterceptor}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingInterceptor Tests")
class RateLimitingInterceptorTest {

  @Mock private RequestDetails requestDetails;

  // Use short window duration for testing (2 seconds instead of 60)
  private static final long TEST_WINDOW_DURATION_MILLIS = 2000;

  // ==================================================================================================
  // Constructor Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 100, 1000})
    @DisplayName("Should create interceptor with various rate limits")
    void shouldCreateInterceptorWithVariousRateLimits(int maxRequestsPerMinute) {
      // GIVEN / WHEN / THEN
      assertThatCode(() -> new RateLimitingInterceptor(maxRequestsPerMinute))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create interceptor with custom window duration")
    void shouldCreateInterceptorWithCustomWindowDuration() {
      // GIVEN / WHEN / THEN
      assertThatCode(() -> new RateLimitingInterceptor(10, TEST_WINDOW_DURATION_MILLIS))
          .doesNotThrowAnyException();
    }
  }

  // ==================================================================================================
  // Rate Limiting Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Rate Limiting Tests")
  class RateLimitingTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 50})
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit(int maxRequestsPerWindow) {
      // GIVEN
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN / THEN
      for (int i = 0; i < maxRequestsPerWindow; i++) {
        assertThatCode(() -> interceptor.checkRateLimit(requestDetails)).doesNotThrowAnyException();
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    @DisplayName("Should reject request exceeding rate limit")
    void shouldRejectRequestExceedingRateLimit(int maxRequestsPerWindow) {
      // GIVEN
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      for (int i = 0; i < maxRequestsPerWindow; i++) {
        interceptor.checkRateLimit(requestDetails);
      }

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    @DisplayName("Should track different clients separately")
    void shouldTrackDifferentClientsSeparately() {
      // GIVEN
      int maxRequestsPerWindow = 2;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);

      RequestDetails client1Request = requestDetails;
      when(client1Request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      RequestDetails client2Request = org.mockito.Mockito.mock(RequestDetails.class);
      when(client2Request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.2");

      // WHEN / THEN
      // Client 1 makes 2 requests (at limit)
      assertThatCode(
              () -> {
                interceptor.checkRateLimit(client1Request);
                interceptor.checkRateLimit(client1Request);
              })
          .doesNotThrowAnyException();

      // Client 2 should still be able to make requests
      assertThatCode(
              () -> {
                interceptor.checkRateLimit(client2Request);
                interceptor.checkRateLimit(client2Request);
              })
          .doesNotThrowAnyException();

      // Both clients should be blocked on next request
      assertThatThrownBy(() -> interceptor.checkRateLimit(client1Request))
          .isInstanceOf(InvalidRequestException.class);

      assertThatThrownBy(() -> interceptor.checkRateLimit(client2Request))
          .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should reset rate limit after time window expires")
    void shouldResetRateLimitAfterTimeWindow() {
      // GIVEN
      int maxRequestsPerWindow = 2;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      // Make requests up to limit
      interceptor.checkRateLimit(requestDetails);
      interceptor.checkRateLimit(requestDetails);

      // Verify limit is reached
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");

      // THEN
      // Wait for the rate limit window to reset
      await()
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(100))
          .untilAsserted(
              () ->
                  assertThatCode(() -> interceptor.checkRateLimit(requestDetails))
                      .doesNotThrowAnyException());
    }

    @Test
    @DisplayName("Should gradually allow requests as time window slides")
    void shouldGraduallyAllowRequestsAsTimeWindowSlides() {
      // GIVEN
      int maxRequestsPerWindow = 3;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      // Fill up the rate limit
      for (int i = 0; i < maxRequestsPerWindow; i++) {
        interceptor.checkRateLimit(requestDetails);
      }

      // Verify rate limit is reached
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);

      // THEN
      // After window reset, should be able to make requests again
      await()
          .atMost(Duration.ofSeconds(5))
          .pollDelay(Duration.ofMillis(TEST_WINDOW_DURATION_MILLIS + 100))
          .untilAsserted(
              () ->
                  assertThatCode(() -> interceptor.checkRateLimit(requestDetails))
                      .doesNotThrowAnyException());
    }
  }

  // ==================================================================================================
  // Client Identification Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Client Identification Tests")
  class ClientIdentificationTests {

    @Test
    @DisplayName("Should identify client by X-Forwarded-For header")
    void shouldIdentifyClientByXForwardedForHeader() {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    @ParameterizedTest
    @MethodSource("multipleIpAddresses")
    @DisplayName("Should extract first IP from X-Forwarded-For header with multiple IPs")
    void shouldExtractFirstIpFromXForwardedForHeader(String forwardedFor) {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    static Stream<String> multipleIpAddresses() {
      return Stream.of(
          "192.168.1.1, 10.0.0.1, 172.16.0.1",
          "10.0.0.1, 192.168.1.1",
          "  192.168.1.1  , 10.0.0.1");
    }

    @Test
    @DisplayName("Should fall back to X-Real-IP header when X-Forwarded-For is absent")
    void shouldFallBackToXRealIpHeader() {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);

      lenient().when(requestDetails.getHeader("X-Forwarded-For")).thenReturn(null);
      when(requestDetails.getHeader("X-Real-IP")).thenReturn("192.168.1.1");

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    @ParameterizedTest
    @MethodSource("fallbackScenarios")
    @DisplayName("Should handle various header fallback scenarios correctly")
    void shouldHandleVariousHeaderFallbackScenarios(String xForwardedFor, String xRealIp) {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);

      lenient().when(requestDetails.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
      lenient().when(requestDetails.getHeader("X-Real-IP")).thenReturn(xRealIp);

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");
    }

    static Stream<Arguments> fallbackScenarios() {
      return Stream.of(
          Arguments.of(null, "192.168.1.1"),
          Arguments.of("", "192.168.1.1"),
          Arguments.of(null, null),
          Arguments.of("", ""));
    }

    @Test
    @DisplayName("Should use unknown identifier when no IP headers are present")
    void shouldUseUnknownIdentifierWhenNoIpHeadersPresent() {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);

      lenient().when(requestDetails.getHeader("X-Forwarded-For")).thenReturn(null);
      lenient().when(requestDetails.getHeader("X-Real-IP")).thenReturn(null);

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");
    }
  }

  // ==================================================================================================
  // Edge Cases
  // ==================================================================================================

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should enforce rate limit of 1 request per window")
    void shouldEnforceRateLimitOfOne() {
      // GIVEN
      int maxRequestsPerWindow = 1;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      interceptor.checkRateLimit(requestDetails);

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should handle high rate limit efficiently")
    void shouldHandleHighRateLimitEfficiently() {
      // GIVEN
      int maxRequestsPerWindow = 1000;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN / THEN
      for (int i = 0; i < maxRequestsPerWindow; i++) {
        assertThatCode(() -> interceptor.checkRateLimit(requestDetails)).doesNotThrowAnyException();
      }

      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should handle concurrent requests from same client safely")
    void shouldHandleConcurrentRequestsFromSameClientSafely() throws InterruptedException {
      // GIVEN
      int maxRequestsPerWindow = 10;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      Thread[] threads = new Thread[15];
      for (int i = 0; i < 15; i++) {
        threads[i] =
            new Thread(
                () -> {
                  try {
                    interceptor.checkRateLimit(requestDetails);
                  } catch (InvalidRequestException _) {
                    // Expected for some threads
                  }
                });
        threads[i].start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should maintain rate limit accuracy under rapid sequential requests")
    void shouldMaintainRateLimitAccuracyUnderRapidRequests() {
      // GIVEN
      int maxRequestsPerWindow = 5;
      RateLimitingInterceptor interceptor =
          new RateLimitingInterceptor(maxRequestsPerWindow, TEST_WINDOW_DURATION_MILLIS);
      when(requestDetails.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

      // WHEN
      for (int i = 0; i < maxRequestsPerWindow; i++) {
        interceptor.checkRateLimit(requestDetails);
      }

      // THEN
      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");

      assertThatThrownBy(() -> interceptor.checkRateLimit(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Rate limit exceeded");
    }
  }
}
