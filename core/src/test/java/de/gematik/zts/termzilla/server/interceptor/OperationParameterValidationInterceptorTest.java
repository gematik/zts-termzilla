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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link OperationParameterValidationInterceptor}.
 *
 * <p>Tests the interceptor's ability to validate operation parameters against configured allowed
 * parameters for FHIR operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationParameterValidationInterceptor Tests")
class OperationParameterValidationInterceptorTest {

  @Mock private ConfigProperties configProperties;

  @Mock private RequestDetails requestDetails;

  @Mock private HttpServletRequest httpServletRequest;

  @Mock private HttpServletResponse httpServletResponse;

  @Mock private FhirContext fhirContext;

  @Mock private IParser jsonParser;

  private OperationParameterValidationInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new OperationParameterValidationInterceptor(configProperties, fhirContext);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create interceptor with valid config")
    void shouldCreateInterceptorWithValidConfig() {
      // GIVEN
      ConfigProperties config = new ConfigProperties();

      // WHEN & THEN
      assertThatCode(() -> new OperationParameterValidationInterceptor(config, fhirContext))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Non-Operation Request Tests")
  class NonOperationRequestTests {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should allow request when operation is null or empty")
    void shouldAllowRequestWhenOperationIsNullOrEmpty(String operation) {
      // GIVEN
      when(requestDetails.getOperation()).thenReturn(operation);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow request when resource type is null")
    void shouldAllowRequestWhenResourceTypeIsNull() {
      // GIVEN
      when(requestDetails.getOperation()).thenReturn("$expand");
      when(requestDetails.getResourceName()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("No Parameter Restrictions Tests")
  class NoParameterRestrictionsTests {

    @Test
    @DisplayName("Should allow request when no parameter restrictions configured")
    void shouldAllowRequestWhenNoRestrictionsConfigured() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation)).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow request when allowed parameters set is empty")
    void shouldAllowRequestWhenAllowedParametersSetIsEmpty() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(Collections.emptySet());

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Query Parameter Validation Tests")
  class QueryParameterValidationTests {

    @Test
    @DisplayName("Should allow request with valid query parameters")
    void shouldAllowRequestWithValidQueryParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter", "count");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(
              "url", "http://example.com",
              "filter", "test",
              "count", "10");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject request with unauthorized query parameter")
    void shouldRejectRequestWithUnauthorizedQueryParameter() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(
              "url", "http://example.com",
              "unauthorized", "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'unauthorized' is not allowed")
          .hasMessageContaining(operation)
          .hasMessageContaining(resourceType);
    }

    @ParameterizedTest
    @MethodSource("provideMultipleUnauthorizedParameters")
    @DisplayName("Should reject request with multiple unauthorized parameters")
    void shouldRejectRequestWithMultipleUnauthorizedParameters(Map<String, String[]> queryParams) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("is not allowed");
    }

    private static Stream<Arguments> provideMultipleUnauthorizedParameters() {
      return Stream.of(
          Arguments.of(
              ParameterTestDataHelper.createQueryParams(
                  "code", "123", "invalid1", "val1", "invalid2", "val2")),
          Arguments.of(ParameterTestDataHelper.createQueryParams("unauthorized", "value")));
    }
  }

  @Nested
  @DisplayName("POST Body Parameter Validation Tests")
  class PostBodyParameterValidationTests {

    @Test
    @DisplayName("Should allow POST request with valid Parameters resource")
    void shouldAllowPostRequestWithValidParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter", "count");
      Parameters parameters =
          ParameterTestDataHelper.createParameters(
              "url", "http://example.com",
              "filter", "test");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      stubParsedBody(parameters);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow useSupplement in a POST Parameters resource")
    void shouldAllowUseSupplementInPostParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "useSupplement");
      Parameters parameters =
          ParameterTestDataHelper.createParameters(
              "url", "http://example.com/ValueSet/example",
              "useSupplement", "http://example.com/CodeSystem/supplement");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      stubParsedBody(parameters);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject POST request with unauthorized parameter in body")
    void shouldRejectPostRequestWithUnauthorizedParameter() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter");
      Parameters parameters =
          ParameterTestDataHelper.createParameters(
              "url", "http://example.com",
              "unauthorized", "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      stubParsedBody(parameters);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'unauthorized' is not allowed")
          .hasMessageContaining(operation)
          .hasMessageContaining(resourceType);
    }

    @Test
    @DisplayName("Should allow POST request when resource is not Parameters")
    void shouldAllowPostRequestWhenResourceIsNotParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      stubParsedBody(new Patient());

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE", "PATCH"})
    @DisplayName("Should not validate body parameters for non-POST methods")
    void shouldNotValidateBodyParametersForNonPostMethods(String httpMethod) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn(httpMethod);
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Nested Parameter Tests")
  class NestedParameterTests {

    @ParameterizedTest
    @ValueSource(strings = {"property.code", "property.value", "property.subproperty.nested"})
    @DisplayName("Should allow nested parameters when base parameter is allowed")
    void shouldAllowNestedParametersWhenBaseIsAllowed(String nestedParam) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system", "property");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(nestedParam, "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject nested parameters when base parameter is not allowed")
    void shouldRejectNestedParametersWhenBaseIsNotAllowed() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams("unauthorized.nested", "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'unauthorized.nested' is not allowed");
    }
  }

  @Nested
  @DisplayName("Array Parameter Tests")
  class ArrayParameterTests {

    @ParameterizedTest
    @ValueSource(strings = {"property[0]", "property[1]", "property[99]"})
    @DisplayName("Should allow array parameters when base parameter is allowed")
    void shouldAllowArrayParametersWhenBaseIsAllowed(String arrayParam) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system", "property");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(arrayParam, "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject array parameters when base parameter is not allowed")
    void shouldRejectArrayParametersWhenBaseIsNotAllowed() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams("unauthorized[0]", "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'unauthorized[0]' is not allowed");
    }
  }

  @Nested
  @DisplayName("Combined Validation Tests")
  class CombinedValidationTests {

    @Test
    @DisplayName("Should validate both query and body parameters for POST request")
    void shouldValidateBothQueryAndBodyParametersForPost() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter", "count");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams("url", "http://example.com");
      Parameters bodyParameters =
          ParameterTestDataHelper.createParameters("filter", "test", "count", "10");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);
      stubParsedBody(bodyParameters);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject POST request when query parameter is invalid even if body is valid")
    void shouldRejectPostRequestWhenQueryParameterIsInvalid() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams("unauthorized", "value");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Parameter 'unauthorized' is not allowed");
    }

    @Test
    @DisplayName("Should allow mixed parameter types including nested and array")
    void shouldAllowMixedParameterTypes() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_LOOKUP;
      Set<String> allowedParams = Set.of("code", "system", "property", "designation");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(
              "code", "123",
              "property.code", "display",
              "designation[0]", "en");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty query parameters map")
    void shouldHandleEmptyQueryParametersMap() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle Parameters resource with no parameters")
    void shouldHandleParametersResourceWithNoParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url");
      Parameters emptyParameters = new Parameters();

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      stubParsedBody(emptyParameters);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle parameter with special characters in name")
    void shouldHandleParameterWithSpecialCharacters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("_include", "system-version");
      Map<String, String[]> queryParams =
          ParameterTestDataHelper.createQueryParams(
              "_include", "ValueSet:compose",
              "system-version", "1.0");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getParameterMap()).thenReturn(queryParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow POST request when request body is malformed JSON")
    void shouldAllowPostRequestWhenBodyIsMalformedJson() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      String operation = ConfigProperties.OPERATION_EXPAND;
      Set<String> allowedParams = Set.of("url", "filter");

      when(requestDetails.getOperation()).thenReturn(operation);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(configProperties.getAllowedOperationParams(resourceType, operation))
          .thenReturn(allowedParams);
      when(httpServletRequest.getMethod()).thenReturn("POST");
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      when(requestDetails.loadRequestContents())
          .thenReturn("{invalid-json".getBytes(StandardCharsets.UTF_8));
      when(fhirContext.newJsonParser()).thenReturn(jsonParser);
      when(jsonParser.parseResource(anyString()))
          .thenThrow(new DataFormatException("Malformed JSON"));

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  /** Helper class for creating test data for parameter validation tests. */
  static class ParameterTestDataHelper {

    /**
     * Creates a map of query parameters from key-value pairs.
     *
     * @param keyValuePairs alternating keys and values
     * @return a map of parameter names to value arrays
     * @throws IllegalArgumentException if the number of arguments is not even
     */
    static Map<String, String[]> createQueryParams(String... keyValuePairs) {
      if (keyValuePairs.length % 2 != 0) {
        throw new IllegalArgumentException("Key-value pairs must be even");
      }

      Map<String, String[]> params = new HashMap<>();
      for (int i = 0; i < keyValuePairs.length; i += 2) {
        params.put(keyValuePairs[i], new String[] {keyValuePairs[i + 1]});
      }
      return params;
    }

    /**
     * Creates a FHIR Parameters resource from key-value pairs.
     *
     * @param keyValuePairs alternating parameter names and values
     * @return a Parameters resource with the specified parameters
     * @throws IllegalArgumentException if the number of arguments is not even
     */
    static Parameters createParameters(String... keyValuePairs) {
      if (keyValuePairs.length % 2 != 0) {
        throw new IllegalArgumentException("Key-value pairs must be even");
      }

      Parameters parameters = new Parameters();
      for (int i = 0; i < keyValuePairs.length; i += 2) {
        parameters
            .addParameter()
            .setName(keyValuePairs[i])
            .setValue(new StringType(keyValuePairs[i + 1]));
      }
      return parameters;
    }
  }

  private void stubParsedBody(Resource parsedResource) {
    when(requestDetails.loadRequestContents())
        .thenReturn("{\"resourceType\":\"Parameters\"}".getBytes(StandardCharsets.UTF_8));
    when(fhirContext.newJsonParser()).thenReturn(jsonParser);
    when(jsonParser.parseResource(anyString())).thenReturn(parsedResource);
  }
}
