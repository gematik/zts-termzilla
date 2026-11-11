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

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple rate limiting interceptor using a sliding window approach.
 *
 * <p>Limits the number of requests per client per time window to prevent denial of service attacks.
 */
@Slf4j
@Interceptor
public class RateLimitingInterceptor {

  private final Cache<String, RequestWindow> requestWindows =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(10)).maximumSize(10_000).build();
  private final int maxRequestsPerWindow;
  private final long windowDurationMillis;
  private final long cleanupThresholdMillis;

  /**
   * Creates a rate limiting interceptor with default 1-minute window.
   *
   * @param maxRequestsPerMinute maximum requests allowed per minute
   */
  public RateLimitingInterceptor(int maxRequestsPerMinute) {
    this(maxRequestsPerMinute, 60000); // 1 minute default
  }

  /**
   * Creates a rate limiting interceptor with custom window duration. Useful for testing with
   * shorter windows.
   *
   * @param maxRequestsPerWindow maximum requests allowed per window
   * @param windowDurationMillis duration of the rate limit window in milliseconds
   */
  public RateLimitingInterceptor(int maxRequestsPerWindow, long windowDurationMillis) {
    this.maxRequestsPerWindow = maxRequestsPerWindow;
    this.windowDurationMillis = windowDurationMillis;
    this.cleanupThresholdMillis = windowDurationMillis * 2;

    log.info(
        "Rate limiting enabled: {} requests per {} ms window",
        maxRequestsPerWindow,
        windowDurationMillis);

    // Cleanup old entries periodically
    startCleanupTask();
  }

  /**
   * Checks if the request should be rate limited.
   *
   * @param requestDetails the request details
   * @throws InvalidRequestException if rate limit is exceeded
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
  public void checkRateLimit(RequestDetails requestDetails) {
    String clientId = getClientIdentifier(requestDetails);
    long now = System.currentTimeMillis();

    RequestWindow window;
    if (requestWindows.asMap().containsKey(clientId)) {
      window = requestWindows.asMap().get(clientId);
    } else {
      window = new RequestWindow(now);
      requestWindows.put(clientId, window);
    }

    // Reset window if it has expired
    if (now - window.windowStart > windowDurationMillis) {
      window.reset(now);
    }

    int count = window.count.incrementAndGet();

    if (count > maxRequestsPerWindow) {
      log.warn("Rate limit exceeded for client: {} (count: {})", clientId, count);
      throw new InvalidRequestException(
          String.format(
              "Rate limit exceeded. Maximum %d requests per %d seconds allowed. Please try again later.",
              maxRequestsPerWindow, windowDurationMillis / 1000));
    }
  }

  /**
   * Gets a unique identifier for the client.
   *
   * @param requestDetails the request details
   * @return the client identifier
   */
  private String getClientIdentifier(RequestDetails requestDetails) {
    // Try X-Forwarded-For first (if behind proxy)
    String forwardedFor = requestDetails.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
      return forwardedFor.split(",")[0].trim();
    }

    // Try X-Real-IP
    String realIp = requestDetails.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isEmpty()) {
      return realIp;
    }

    // Fall back to a default identifier
    return "unknown";
  }

  /**
   * Starts a background cleanup task that periodically removes expired rate limit tracking entries.
   */
  private void startCleanupTask() {
    Thread.ofVirtual()
        .name("RateLimitCleanup")
        .start(
            () -> {
              while (true) {
                try {
                  Thread.sleep(windowDurationMillis);
                  long now = System.currentTimeMillis();
                  requestWindows
                      .asMap()
                      .entrySet()
                      .removeIf(
                          entry -> now - entry.getValue().windowStart > cleanupThresholdMillis);
                } catch (InterruptedException _) {
                  Thread.currentThread().interrupt();
                  break;
                }
              }
            });
  }

  /** Represents a time window for counting requests. */
  private static class RequestWindow {

    volatile long windowStart;
    final AtomicInteger count;

    RequestWindow(long windowStart) {
      this.windowStart = windowStart;
      this.count = new AtomicInteger(0);
    }

    void reset(long newWindowStart) {
      this.windowStart = newWindowStart;
      this.count.set(0);
    }
  }
}
