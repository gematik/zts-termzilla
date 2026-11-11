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
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import java.util.HashMap;
import java.util.Map;
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

/** Unit tests for {@link BasicSecurityInterceptor}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("BasicSecurityInterceptor Tests")
class BasicSecurityInterceptorTest {

  @Mock private ConfigProperties configProperties;

  @Mock private RequestDetails requestDetails;

  // ==================================================================================================
  // Constructor Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @ParameterizedTest
    @MethodSource("validationConfigurations")
    @DisplayName("Should create interceptor with various configurations")
    void shouldCreateInterceptorWithVariousConfigurations(int maxRequestSize, int maxParamLength) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(maxRequestSize);
      when(configProperties.getMaxParameterLength()).thenReturn(maxParamLength);

      // WHEN / THEN
      assertThatCode(() -> new BasicSecurityInterceptor(configProperties))
          .doesNotThrowAnyException();
    }

    static Stream<Arguments> validationConfigurations() {
      return Stream.of(
          Arguments.of(10485760, 1000), // Both enabled
          Arguments.of(0, 0), // Both disabled
          Arguments.of(10485760, 0), // Only size validation
          Arguments.of(0, 1000) // Only parameter validation
          );
    }
  }

  // ==================================================================================================
  // Request Size Validation Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Request Size Validation Tests")
  class RequestSizeValidationTests {

    @ParameterizedTest
    @ValueSource(strings = {"1000", "5000", "10485760"})
    @DisplayName("Should accept POST requests within size limit")
    void shouldAcceptPostRequestsWithinSizeLimit(String contentLength) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(1000);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getHeader("Content-Length")).thenReturn(contentLength);
      when(requestDetails.getParameters()).thenReturn(new HashMap<>());

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000", "5000", "999999999"})
    @DisplayName("Should reject POST requests exceeding size limit")
    void shouldRejectPostRequestsExceedingSizeLimit(String contentLength) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(1000);
      when(configProperties.getMaxParameterLength()).thenReturn(1000);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getHeader("Content-Length")).thenReturn(contentLength);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatThrownBy(() -> interceptor.validateRequest(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Request body too large");
    }

    @Test
    @DisplayName("Should skip size validation when disabled")
    void shouldSkipSizeValidationWhenDisabled() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(0);
      when(configProperties.getMaxParameterLength()).thenReturn(1000);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getParameters()).thenReturn(new HashMap<>());

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidContentLengthHeaders")
    @DisplayName("Should handle invalid or missing Content-Length headers")
    void shouldHandleInvalidOrMissingContentLengthHeaders(String contentLength) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(1000);
      when(configProperties.getMaxParameterLength()).thenReturn(1000);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getHeader("Content-Length")).thenReturn(contentLength);
      when(requestDetails.getParameters()).thenReturn(new HashMap<>());

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    static Stream<String> invalidContentLengthHeaders() {
      return Stream.of(null, "invalid", "", "not-a-number");
    }

    @ParameterizedTest
    @MethodSource("nonPostRequestTypes")
    @DisplayName("Should not validate size for non-POST requests")
    void shouldNotValidateSizeForNonPostRequests(RequestTypeEnum requestType) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(1000);
      when(configProperties.getMaxParameterLength()).thenReturn(1000);
      when(requestDetails.getRequestType()).thenReturn(requestType);
      when(requestDetails.getParameters()).thenReturn(new HashMap<>());

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    static Stream<RequestTypeEnum> nonPostRequestTypes() {
      return Stream.of(
          RequestTypeEnum.GET, RequestTypeEnum.PUT, RequestTypeEnum.DELETE, RequestTypeEnum.PATCH);
    }
  }

  // ==================================================================================================
  // Parameter Length Validation Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Parameter Length Validation Tests")
  class ParameterLengthValidationTests {

    @ParameterizedTest
    @MethodSource("validParameters")
    @DisplayName("Should accept parameters within length limit")
    void shouldAcceptParametersWithinLengthLimit(Map<String, String[]> params) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    static Stream<Map<String, String[]>> validParameters() {
      return Stream.of(
          createParams("code", "12345"),
          createParams("system", "http://example.com"),
          createParams("filter", "test"),
          new HashMap<>() // Empty params
          );
    }

    @ParameterizedTest
    @MethodSource("invalidParameters")
    @DisplayName("Should reject parameters exceeding length limit")
    void shouldRejectParametersExceedingLengthLimit(
        Map<String, String[]> params, String expectedParamName) {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(10);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatThrownBy(() -> interceptor.validateRequest(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter '" + expectedParamName + "' too long");
    }

    static Stream<Arguments> invalidParameters() {
      return Stream.of(
          Arguments.of(createParams("filter", "this_is_a_very_long_parameter_value"), "filter"),
          Arguments.of(createParams("code", "a".repeat(100)), "code"),
          Arguments.of(
              createParams("system", "http://very-long-url.com/with/many/segments"), "system"));
    }

    @Test
    @DisplayName("Should skip parameter validation when disabled")
    void shouldSkipParameterValidationWhenDisabled() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(0);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle multiple parameter values with mixed validity")
    void shouldHandleMultipleParameterValues() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(10);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);

      Map<String, String[]> params = new HashMap<>();
      params.put("code", new String[] {"short", "this_is_too_long"});
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatThrownBy(() -> interceptor.validateRequest(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'code' too long");
    }

    @Test
    @DisplayName("Should handle null parameter values gracefully")
    void shouldHandleNullParameterValues() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);

      Map<String, String[]> params = new HashMap<>();
      params.put("code", new String[] {null, "valid"});
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty string parameter values")
    void shouldHandleEmptyStringParameterValues() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);

      Map<String, String[]> params = new HashMap<>();
      params.put("code", new String[] {"", "valid", ""});
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle parameter array with all null values")
    void shouldHandleParameterArrayWithAllNullValues() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);

      Map<String, String[]> params = new HashMap<>();
      params.put("code", new String[] {null, null, null});
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null parameters map")
    void shouldHandleNullParametersMap() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10485760);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.GET);
      when(requestDetails.getParameters()).thenReturn(null);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    private static Map<String, String[]> createParams(String key, String value) {
      Map<String, String[]> params = new HashMap<>();
      params.put(key, new String[] {value});
      return params;
    }
  }

  // ==================================================================================================
  // Combined Validation Tests
  // ==================================================================================================

  @Nested
  @DisplayName("Combined Validation Tests")
  class CombinedValidationTests {

    @Test
    @DisplayName("Should validate both size and parameters for POST request")
    void shouldValidateBothSizeAndParametersForPostRequest() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(10000);
      when(configProperties.getMaxParameterLength()).thenReturn(100);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getHeader("Content-Length")).thenReturn("5000");

      Map<String, String[]> params = new HashMap<>();
      params.put("code", new String[] {"valid"});
      when(requestDetails.getParameters()).thenReturn(params);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail on first validation error")
    void shouldFailOnFirstValidationError() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(1000);
      when(configProperties.getMaxParameterLength()).thenReturn(10);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);
      when(requestDetails.getHeader("Content-Length")).thenReturn("2000");

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatThrownBy(() -> interceptor.validateRequest(requestDetails))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Request body too large");
    }

    @Test
    @DisplayName("Should accept request when all validations are disabled")
    void shouldAcceptRequestWhenAllValidationsDisabled() {
      // GIVEN
      when(configProperties.getMaxRequestSizeBytes()).thenReturn(0);
      when(configProperties.getMaxParameterLength()).thenReturn(0);
      when(requestDetails.getRequestType()).thenReturn(RequestTypeEnum.POST);

      BasicSecurityInterceptor interceptor = new BasicSecurityInterceptor(configProperties);

      // WHEN / THEN
      assertThatCode(() -> interceptor.validateRequest(requestDetails)).doesNotThrowAnyException();
    }
  }
}
