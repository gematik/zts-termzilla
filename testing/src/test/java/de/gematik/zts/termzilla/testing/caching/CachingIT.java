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
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ACCEPT_LANGUAGE;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CACHE_CONTROL;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.CONTENT_TYPE;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.ETAG;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.IF_NONE_MATCH;
import static de.gematik.zts.termzilla.api.constants.HttpHeaderConstants.VARY;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import java.io.IOException;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for HTTP caching behaviour of the terminology server.
 *
 * <p>Verifies that the {@code HttpCachingInterceptor} correctly:
 *
 * <ul>
 *   <li>Sets {@code ETag} response headers on GET requests
 *   <li>Sets {@code Cache-Control} and {@code Vary} response headers on GET requests
 *   <li>Returns deterministic ETags for identical requests
 *   <li>Returns HTTP 304 Not Modified when a matching {@code If-None-Match} header is sent
 * </ul>
 */
@Slf4j
public class CachingIT extends BaseFhirIT {

  /** Regex pattern for a weak SHA-256 ETag value. */
  private static final String ETAG_PATTERN = "W/\"[0-9a-f]{64}\"";

  /** A fake ETag value that will never match any server-generated ETag. */
  private static final String UNKNOWN_ETAG = "W/\"" + "0".repeat(64) + "\"";

  /** Reusable HTTP client for raw request/response access. */
  private final OkHttpClient httpClient = new OkHttpClient();

  /** Absolute URL for a CodeSystem READ request, resolved in {@link #resolveResourceIds()}. */
  private String validCodeSystemReadUrl;

  /** Absolute URL for a ValueSet READ request, resolved in {@link #resolveResourceIds()}. */
  private String validValueSetReadUrl;

  // ==================== Test data ====================

  /**
   * Provides display-name and relative URL path pairs for parametrized GET tests.
   *
   * @return stream of arguments containing display name and path
   */
  static Stream<Arguments> fhirGetUrlPaths() {
    return Stream.of(
        Arguments.of("CodeSystem SEARCH", "fhir/CodeSystem"),
        Arguments.of(
            "CodeSystem SEARCH with URL filter",
            "fhir/CodeSystem?url=http://fhir.de/CodeSystem/bfarm/icd-10-gm"),
        Arguments.of("ValueSet SEARCH", "fhir/ValueSet"),
        Arguments.of("ConceptMap SEARCH", "fhir/ConceptMap"),
        Arguments.of(
            "CodeSystem $lookup (GET)",
            "fhir/CodeSystem/$lookup?system=http://fhir.de/CodeSystem/bfarm/icd-10-gm&version=2026&code=A00"));
  }

  // ==================== Setup ====================

  /**
   * Resolves valid resource IDs for CodeSystem and ValueSet READ tests.
   *
   * <p>Uses the HAPI FHIR client to perform a bundle search and extracts the first available
   * resource ID from the response.
   */
  @BeforeAll
  void resolveResourceIds() {
    IGenericClient client = getFhirClient("fhir");

    Bundle codeSystemBundle =
        client.search().forResource(CodeSystem.class).returnBundle(Bundle.class).execute();
    String codeSystemId =
        codeSystemBundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
    validCodeSystemReadUrl = buildUrl("fhir/CodeSystem/" + codeSystemId);
    log.info("Resolved CodeSystem READ URL: {}", validCodeSystemReadUrl);

    Bundle valueSetBundle =
        client.search().forResource(ValueSet.class).returnBundle(Bundle.class).execute();
    String valueSetId = valueSetBundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
    validValueSetReadUrl = buildUrl("fhir/ValueSet/" + valueSetId);
    log.info("Resolved ValueSet READ URL: {}", validValueSetReadUrl);
  }

  // ==================== Helper methods ====================

  /**
   * Captures the HTTP status code and selected response headers from a single response.
   *
   * <p>Used to extract the data we need from an OkHttp {@link Response} before closing it.
   *
   * @param statusCode the HTTP response status code
   * @param etagHeader the value of the {@code ETag} response header, or {@code null} if absent
   * @param varyHeader the value of the {@code Vary} response header, or {@code null} if absent
   * @param cacheControlHeader the value of the {@code Cache-Control} response header, or {@code
   *     null} if absent
   * @param contentTypeHeader the value of the {@code Content-Type} response header, or {@code null}
   *     if absent
   */
  private record HttpResult(
      int statusCode,
      String etagHeader,
      String varyHeader,
      String cacheControlHeader,
      String contentTypeHeader) {}

  private String buildUrl(String path) {
    return LOCALHOST + ":" + port + "/" + path;
  }

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

  /** Tests that verify the presence and absence of the {@code ETag} header in responses. */
  @Nested
  class ETagHeaderPresence {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("ETag header is set on GET response")
    void etagHeaderIsSetOnGetResponse(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).as("HTTP status code for %s", displayName).isEqualTo(200);
      assertThat(result.etagHeader())
          .as("ETag header must be present for %s", displayName)
          .isNotNull()
          .matches(ETAG_PATTERN);
    }

    @Test
    @DisplayName("ETag header is set on CodeSystem READ response")
    void etagIsSetOnCodeSystemRead() throws IOException {

      // GIVEN
      String url = validCodeSystemReadUrl;

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).isEqualTo(200);
      assertThat(result.etagHeader()).isNotNull().matches(ETAG_PATTERN);
    }

    @Test
    @DisplayName("ETag header is set on ValueSet READ response")
    void etagIsSetOnValueSetRead() throws IOException {

      // GIVEN
      String url = validValueSetReadUrl;

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).isEqualTo(200);
      assertThat(result.etagHeader()).isNotNull().matches(ETAG_PATTERN);
    }
  }

  /**
   * Tests that verify ETags are produced deterministically across repeated and different requests.
   */
  @Nested
  class ETagDeterminism {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Same URL produces the same ETag on repeated requests")
    void sameUrlProducesSameETagOnRepeatedRequests(String displayName, String path)
        throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult firstResult = sendGet(url);
      HttpResult secondResult = sendGet(url);

      // THEN
      assertThat(firstResult.etagHeader())
          .as("ETag missing from first response for %s", displayName)
          .isNotNull();
      assertThat(firstResult.etagHeader())
          .as("ETag must be identical for repeated requests to %s", displayName)
          .isEqualTo(secondResult.etagHeader());
    }

    @Test
    @DisplayName("Different URLs produce different ETags")
    void differentUrlsProduceDifferentETags() throws IOException {

      // GIVEN
      String codeSystemUrl = buildUrl("fhir/CodeSystem");
      String valueSetUrl = buildUrl("fhir/ValueSet");

      // WHEN
      HttpResult codeSystemResult = sendGet(codeSystemUrl);
      HttpResult valueSetResult = sendGet(valueSetUrl);

      // THEN
      assertThat(codeSystemResult.etagHeader())
          .as("ETag missing from CodeSystem response")
          .isNotNull();
      assertThat(codeSystemResult.etagHeader())
          .as("ETags for different URLs must differ")
          .isNotEqualTo(valueSetResult.etagHeader());
    }
  }

  /** Tests that verify correct handling of the {@code If-None-Match} request header. */
  @Nested
  class IfNoneMatchHandling {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Returns HTTP 304 when ETag matches")
    void returns304WhenETagMatches(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);
      String etag = sendGet(url).etagHeader();
      assertThat(etag).as("ETag missing from initial GET for %s", displayName).isNotNull();

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, etag);

      // THEN
      assertThat(result.statusCode())
          .as("Expected HTTP 304 for %s with matching ETag", displayName)
          .isEqualTo(304);
    }

    @Test
    @DisplayName("Returns HTTP 304 when ETag matches on CodeSystem READ")
    void returns304OnCodeSystemReadWithMatchingETag() throws IOException {

      // GIVEN
      String url = validCodeSystemReadUrl;
      String etag = sendGet(url).etagHeader();
      assertThat(etag).as("ETag missing from initial CodeSystem READ").isNotNull();

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, etag);

      // THEN
      assertThat(result.statusCode()).isEqualTo(304);
    }

    @Test
    @DisplayName("Returns HTTP 304 when ETag matches on ValueSet READ")
    void returns304OnValueSetReadWithMatchingETag() throws IOException {

      // GIVEN
      String url = validValueSetReadUrl;
      String etag = sendGet(url).etagHeader();
      assertThat(etag).as("ETag missing from initial ValueSet READ").isNotNull();

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, etag);

      // THEN
      assertThat(result.statusCode()).isEqualTo(304);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Returns HTTP 200 when ETag does not match")
    void returns200WhenETagDoesNotMatch(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, UNKNOWN_ETAG);

      // THEN
      assertThat(result.statusCode())
          .as("Expected HTTP 200 for %s with non-matching ETag", displayName)
          .isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Returns HTTP 304 for wildcard If-None-Match")
    void returns304ForWildcardIfNoneMatch(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, "*");

      // THEN
      assertThat(result.statusCode())
          .as("Expected HTTP 304 for %s with wildcard If-None-Match", displayName)
          .isEqualTo(304);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName(
        "Returns HTTP 304 when strong ETag form matches (weak comparison ignores W/ prefix)")
    void returns304ForStrongETagForm(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);
      String weakETag = sendGet(url).etagHeader();
      assertThat(weakETag).as("ETag missing from initial GET for %s", displayName).isNotNull();
      // Strip the W/ prefix to form a strong ETag representation
      String strongETag = weakETag.substring(2);

      // WHEN
      HttpResult result = sendGetWithIfNoneMatch(url, strongETag);

      // THEN
      assertThat(result.statusCode())
          .as("Expected HTTP 304 for %s: weak comparison must match strong ETag form", displayName)
          .isEqualTo(304);
    }
  }

  /**
   * Tests that verify the presence and value of {@code Vary} and {@code Cache-Control} response
   * headers in normal (non-CDN-optimized) mode.
   */
  @Nested
  class ResponseHeadersHandling {

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Vary header contains Accept and Accept-Language on GET response")
    void varyHeaderContainsAcceptAndAcceptLanguageOnGetResponse(String displayName, String path)
        throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).as("HTTP status code for %s", displayName).isEqualTo(200);
      assertThat(result.varyHeader())
          .as("Vary header must be present for %s", displayName)
          .isNotNull()
          .contains(ACCEPT)
          .contains(ACCEPT_LANGUAGE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "de.gematik.zts.termzilla.testing.caching.CachingIT#fhirGetUrlPaths")
    @DisplayName("Cache-Control header is set on GET response")
    void cacheControlHeaderIsSetOnGetResponse(String displayName, String path) throws IOException {

      // GIVEN
      String url = buildUrl(path);

      // WHEN
      HttpResult result = sendGet(url);

      // THEN
      assertThat(result.statusCode()).as("HTTP status code for %s", displayName).isEqualTo(200);
      assertThat(result.cacheControlHeader())
          .as("Cache-Control header must be present for %s", displayName)
          .isNotNull()
          .contains("max-age");
    }
  }
}
