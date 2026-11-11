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
package de.gematik.zts.termzilla.testing.caching;

import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CACHE_CONTROL;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CONTENT_TYPE;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ETAG;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.IF_NONE_MATCH;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.VARY;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.testing.BaseFhirIT;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * Integration tests for HTTP caching behaviour when CDN-optimized mode is active ({@code
 * core.caching.cdnOptimized=true}).
 *
 * <p>Verifies that:
 *
 * <ul>
 *   <li>{@code Vary: Accept, Accept-Language} is <em>not</em> sent
 *   <li>{@code Accept} and {@code Accept-Language} headers from the client are ignored
 *   <li>{@code ETag} and {@code If-None-Match} negotiation still works correctly
 * </ul>
 */
@Slf4j
public class CdnOptimizedCachingIT extends BaseFhirIT {

  /** Regex pattern for a weak SHA-256 ETag value. */
  private static final String ETAG_PATTERN = "W/\"[0-9a-f]{64}\"";

  /** A fake ETag value that will never match any server-generated ETag. */
  private static final String UNKNOWN_ETAG = "W/\"" + "0".repeat(64) + "\"";

  /** Reusable HTTP client for raw request/response access. */
  private final OkHttpClient httpClient = new OkHttpClient();

  // ==================== Configuration ====================

  @Override
  protected void configureEnvironment(EnvironmentVariables env) {
    env.set("CORE_CACHING_CDNOPTIMIZED", "true");
  }

  // ==================== Test data ====================

  /**
   * Provides display-name and relative URL path pairs for parametrized GET tests.
   *
   * @return stream of arguments containing display name and path
   */
  static Stream<Arguments> fhirGetUrlPaths() {
    return Stream.of(
        Arguments.of("CodeSystem SEARCH", "fhir/CodeSystem"),
        Arguments.of("ValueSet SEARCH", "fhir/ValueSet"),
        Arguments.of("ConceptMap SEARCH", "fhir/ConceptMap"),
        Arguments.of(
            "CodeSystem $lookup (GET)",
            "fhir/CodeSystem/$lookup?system=http://fhir.de/CodeSystem/bfarm/icd-10-gm&version=2026&code=A00"));
  }

  // ==================== Helper methods ====================

  private String buildUrl(String path) {
    return LOCALHOST + ":" + port + "/" + path;
  }

  /**
   * Captures selected response headers before the OkHttp {@link Response} is closed.
   *
   * @param statusCode the HTTP response status code
   * @param etagHeader the value of the {@code ETag} header, or {@code null} if absent
   * @param varyHeader the value of the {@code Vary} header, or {@code null} if absent
   * @param cacheControlHeader the value of the {@code Cache-Control} header, or {@code null} if
   *     absent
   * @param contentTypeHeader the value of the {@code Content-Type} header, or {@code null} if
   *     absent
   */
  private record HttpResult(
      int statusCode,
      String etagHeader,
      String varyHeader,
      String cacheControlHeader,
      String contentTypeHeader) {}

  private HttpResult sendGet(String url) throws IOException {
    Request request = new Request.Builder().url(url).get().build();
    try (Response response = httpClient.newCall(request).execute()) {
      return new HttpResult(
          response.code(),
          response.header(ETAG),
          response.header(VARY),
          response.header(CACHE_CONTROL),
          response.header(CONTENT_TYPE));
    }
  }

  private HttpResult sendGetWithAccept(String url, String acceptHeader) throws IOException {
    Request request = new Request.Builder().url(url).header(ACCEPT, acceptHeader).get().build();
    try (Response response = httpClient.newCall(request).execute()) {
      return new HttpResult(
          response.code(),
          response.header(ETAG),
          response.header(VARY),
          response.header(CACHE_CONTROL),
          response.header(CONTENT_TYPE));
    }
  }

  private HttpResult sendGetWithIfNoneMatch(String url, String etag) throws IOException {
    Request request = new Request.Builder().url(url).header(IF_NONE_MATCH, etag).get().build();
    try (Response response = httpClient.newCall(request).execute()) {
      return new HttpResult(
          response.code(),
          response.header(ETAG),
          response.header(VARY),
          response.header(CACHE_CONTROL),
          response.header(CONTENT_TYPE));
    }
  }

  // ==================== Nested test classes ====================

  /** Tests that verify {@code Vary} header behaviour in CDN-optimized mode. */
  @Nested
  class VaryHeaderHandling {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CdnOptimizedCachingIT#fhirGetUrlPaths")
    @DisplayName("Vary header does not contain Accept or Accept-Language in CDN-optimized mode")
    void varyHeaderDoesNotContainAcceptOrAcceptLanguage(String displayName, String path)
        throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).as("HTTP status code for %s", displayName).isEqualTo(200);
      // Split Vary value into individual tokens (e.g. "Accept-Encoding, Accept" →
      // ["Accept-Encoding", "Accept"])
      // and verify that neither "Accept" nor "Accept-Language" is present as an exact token.
      // This avoids false positives from "Accept-Encoding" containing "Accept" as a substring.
      List<String> varyTokens =
          result.varyHeader() == null
              ? Collections.emptyList()
              : Arrays.stream(result.varyHeader().split(",")).map(String::trim).toList();
      assertThat(varyTokens)
          .as("Vary tokens must not include Accept or Accept-Language for %s", displayName)
          .isEmpty(); // In CDN-optimized mode, Vary should not include Accept or Accept-Language,
      // so the list should be empty.
    }
  }

  /**
   * Tests that verify that incoming {@code Accept} headers are ignored in CDN-optimized mode, so
   * the server always responds with its default format (JSON).
   */
  @Nested
  class AcceptHeaderIgnored {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CdnOptimizedCachingIT#fhirGetUrlPaths")
    @DisplayName("Response Content-Type is JSON even when Accept: application/fhir+xml is sent")
    void responseIsJsonEvenWhenXmlAcceptHeaderSent(String displayName, String path)
        throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN — explicitly request XML, the server must ignore it in CDN-optimized mode
      HttpResult result = sendGetWithAccept(url, "application/fhir+xml");

      // THEN
      assertThat(result.statusCode()).as("HTTP status code for %s", displayName).isEqualTo(200);
      assertThat(result.contentTypeHeader())
          .as("Content-Type must be JSON for %s because Accept header is ignored", displayName)
          .isNotNull()
          .contains("application/fhir+json");
    }
  }

  /**
   * Tests that verify ETag and {@code If-None-Match} behaviour is unaffected by CDN-optimized mode.
   */
  @Nested
  class ETagBehaviourUnchanged {

    @Test
    @DisplayName("ETag header is still set on GET response in CDN-optimized mode")
    void etagIsStillSetInCdnOptimizedMode() throws IOException {

      // GIVEN
      String url = buildUrl("fhir/CodeSystem");

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).isEqualTo(200);
      assertThat(result.etagHeader()).isNotNull().matches(ETAG_PATTERN);
    }

    @Test
    @DisplayName("HTTP 304 is still returned for matching If-None-Match in CDN-optimized mode")
    void returns304ForMatchingETagInCdnOptimizedMode() throws IOException {

      // GIVEN
      String url = buildUrl("fhir/CodeSystem");
      String etag = sendGet(url).etagHeader();
      assertThat(etag).as("ETag missing from initial GET").isNotNull();

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, etag);

      // THEN
      assertThat(result.statusCode()).isEqualTo(304);
    }

    @Test
    @DisplayName("HTTP 200 is returned for non-matching If-None-Match in CDN-optimized mode")
    void returns200ForNonMatchingETagInCdnOptimizedMode() throws IOException {

      // GIVEN
      String url = buildUrl("fhir/CodeSystem");

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, UNKNOWN_ETAG);

      // THEN
      assertThat(result.statusCode()).isEqualTo(200);
    }
  }
}
