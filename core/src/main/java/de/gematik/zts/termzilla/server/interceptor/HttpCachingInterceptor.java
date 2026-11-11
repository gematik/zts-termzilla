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
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ETAG;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.IF_NONE_MATCH;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.VARY;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
@Interceptor
public class HttpCachingInterceptor {

  /**
   * Fixed {@code Vary} header value added to all GET/HEAD responses. Instructs caches to maintain
   * separate entries per {@code Accept} (JSON vs. XML format) and {@code Accept-Language}
   * (designation language). {@code Accept-Encoding} is handled separately by the Spring Boot
   * compression filter.
   */
  static final String VARY_HEADERS = "Accept, Accept-Language";

  private final String configHash;
  private final String cacheControlHeader;
  private final boolean cdnOptimized;

  public HttpCachingInterceptor(ConfigProperties config, String configAndDataStateHash) {
    // HEX representation of the hash code of the config for easier readability in logs
    this.configHash = configAndDataStateHash;
    this.cacheControlHeader = buildCacheControlHeader(config);
    this.cdnOptimized = config.isCachingCdnOptimized();
  }

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
  public boolean incomingRequestProcessed(
      HttpServletRequest request, HttpServletResponse response) {

    log.debug("Processing incoming request: {} {}", request.getMethod(), request.getRequestURL());

    // Eindeutigen Hash berechnen basierend auf der aktuellen Konfiguration und der angeforderten
    // URL.
    // Dieser Hash dient als Grundlage für die ETag-Generierung.
    String input = configHash + "#" + request.getRequestURL().toString();
    String calculatedETag =
        "W/\"" + DigestUtils.sha256Hex(input.getBytes(StandardCharsets.UTF_8)) + "\"";
    log.debug("Calculated ETag: {}", calculatedETag);

    // Überprüfen, ob der Client einen If-None-Match Header gesendet hat und ob dieser mit dem
    // berechneten
    // ETag übereinstimmt. Bei Übereinstimmung wird ein 304 Not Modified zurückgegeben.
    String ifNoneMatchHeader = request.getHeader(IF_NONE_MATCH);
    if (ifNoneMatchHeader != null) {
      log.debug("If-None-Match header received: {}", ifNoneMatchHeader);
      if (weakMatchesAny(calculatedETag, ifNoneMatchHeader)) {
        log.debug("ETag matches. Returning 304 Not Modified.");
        response.setStatus(304);
        return false;
      } else {
        log.debug("ETag does not match. Continuing with request processing.");
      }
    }

    // Den berechneten ETag als Request-Attribut speichern, damit er im outgoingResponse-Hook
    // verfügbar
    // ist, um ihn im Response-Header zu setzen.
    request.setAttribute("calculatedETag", calculatedETag);

    return true;
  }

  /**
   * Performs weak ETag comparison as defined by RFC 7232 §2.3.2.
   *
   * <p>The {@code If-None-Match} header may contain multiple comma-separated ETags or the wildcard
   * {@code *}. The weak comparison function ignores the {@code W/} prefix and compares only the
   * opaque-tag portion.
   *
   * @param calculatedETag the server-computed ETag value
   * @param ifNoneMatchHeader the raw {@code If-None-Match} header value
   * @return {@code true} if any provided ETag weakly matches the calculated one
   */
  private static boolean weakMatchesAny(String calculatedETag, String ifNoneMatchHeader) {
    if ("*".equals(ifNoneMatchHeader.trim())) {
      return true;
    }

    String calculatedOpaqueTag = extractOpaqueTag(calculatedETag);

    if (calculatedOpaqueTag == null) {
      return false;
    }

    for (String candidate : ifNoneMatchHeader.split(",")) {
      String candidateOpaqueTag = extractOpaqueTag(candidate.trim());
      if (calculatedOpaqueTag.equals(candidateOpaqueTag)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Extracts the opaque-tag from an ETag value by stripping the {@code W/} prefix. The opaque-tag
   * includes the mandatory surrounding quotes. Returns {@code null} if the ETag is malformed
   * (missing quotes).
   */
  private static String extractOpaqueTag(String etag) {
    String tag = etag;
    if (tag.startsWith("W/")) {
      tag = tag.substring(2);
    }
    if (tag.startsWith("\"") && tag.endsWith("\"") && tag.length() >= 2) {
      return tag;
    }
    return null;
  }

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public boolean outgoingResponse(HttpServletRequest request, HttpServletResponse response) {
    if ("GET".equalsIgnoreCase(request.getMethod())
        || "HEAD".equalsIgnoreCase(request.getMethod())) {
      String calculatedETag = (String) request.getAttribute("calculatedETag");
      if (calculatedETag != null) {
        response.setHeader(ETAG, calculatedETag);
        log.debug("Set ETag header on GET response: {}", calculatedETag);
      }
      response.setHeader(CACHE_CONTROL, cacheControlHeader);
      log.debug("Set Cache-Control header: {}", cacheControlHeader);
      if (!cdnOptimized) {
        response.addHeader(VARY, VARY_HEADERS);
        log.debug("Added Vary header: {}", VARY_HEADERS);
      }
    }
    return true;
  }

  /**
   * Builds the {@code Cache-Control} header value from the given caching configuration. Called once
   * during construction; the result is cached for the lifetime of the interceptor.
   *
   * @param config the configuration to read caching settings from
   * @return the {@code Cache-Control} directive string
   */
  static String buildCacheControlHeader(ConfigProperties config) {
    if (!config.isCachingEnabled()) {
      return "no-store";
    }

    StringBuilder directives = new StringBuilder("public");
    directives.append(", max-age=").append(config.getCachingMaxAge());

    if (config.getCachingSharedMaxAge() != -1) {
      directives.append(", s-maxage=").append(config.getCachingSharedMaxAge());
    }

    if (config.isCachingMustRevalidate()) {
      directives.append(", must-revalidate");
    }

    if (config.getCachingStaleWhileRevalidate() != -1) {
      directives
          .append(", stale-while-revalidate=")
          .append(config.getCachingStaleWhileRevalidate());
    }

    return directives.toString();
  }
}
