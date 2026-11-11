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

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Unit tests for {@link SearchParameterValidationInterceptor}.
 *
 * <p>Tests the interceptor's ability to validate search parameters against configured allowed
 * parameters for FHIR search operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchParameterValidationInterceptor Tests")
class SearchParameterValidationInterceptorTest {

  @Mock private ConfigProperties configProperties;

  @Mock private RequestDetails requestDetails;

  @Mock private HttpServletRequest httpServletRequest;

  @Mock private HttpServletResponse httpServletResponse;

  private SearchParameterValidationInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new SearchParameterValidationInterceptor(configProperties);
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
      assertThatCode(() -> new SearchParameterValidationInterceptor(config))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Non-Search Operation Tests")
  class NonSearchOperationTests {

    @ParameterizedTest
    @MethodSource("provideNonSearchOperationTypes")
    @DisplayName("Should allow non-search operations without validation")
    void shouldAllowNonSearchOperationsWithoutValidation(RestOperationTypeEnum operationType) {
      // GIVEN
      when(requestDetails.getRestOperationType()).thenReturn(operationType);

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
      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideNonSearchOperationTypes() {
      return Stream.of(
          Arguments.of(RestOperationTypeEnum.READ),
          Arguments.of(RestOperationTypeEnum.CREATE),
          Arguments.of(RestOperationTypeEnum.UPDATE),
          Arguments.of(RestOperationTypeEnum.DELETE),
          Arguments.of(RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE),
          Arguments.of(RestOperationTypeEnum.EXTENDED_OPERATION_TYPE),
          Arguments.of(RestOperationTypeEnum.METADATA));
    }
  }

  @Nested
  @DisplayName("No Parameter Restrictions Tests")
  class NoParameterRestrictionsTests {

    @Test
    @DisplayName("Should allow search when no parameter restrictions configured")
    void shouldAllowSearchWhenNoRestrictionsConfigured() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "name", "test",
              "status", "active");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType)).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow search when allowed parameters set is empty")
    void shouldAllowSearchWhenAllowedParametersSetIsEmpty() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams("name", "test");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
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
  @DisplayName("Valid Search Parameter Tests")
  class ValidSearchParameterTests {

    @Test
    @DisplayName("Should allow search with valid parameters")
    void shouldAllowSearchWithValidParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "version", "name", "status");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "name", "test",
              "status", "active");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow search with single valid parameter")
    void shouldAllowSearchWithSingleValidParameter() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      Set<String> allowedParams = Set.of("url", "code", "system");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams("url", "http://example.com");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow search with empty parameters")
    void shouldAllowSearchWithEmptyParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(Collections.emptyMap());
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Invalid Search Parameter Tests")
  class InvalidSearchParameterTests {

    @Test
    @DisplayName("Should reject search with unauthorized parameter")
    void shouldRejectSearchWithUnauthorizedParameter() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "unauthorized", "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search parameter 'unauthorized' is not allowed")
          .hasMessageContaining(resourceType)
          .hasMessageContaining(allowedParams.toString());
    }

    @Test
    @DisplayName("Should reject search with only unauthorized parameters")
    void shouldRejectSearchWithOnlyUnauthorizedParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      Set<String> allowedParams = Set.of("url", "code");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "invalid1", "value1",
              "invalid2", "value2");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("is not allowed");
    }

    @Test
    @DisplayName("Should reject search with mixed valid and invalid parameters")
    void shouldRejectSearchWithMixedValidAndInvalidParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "name", "test",
              "forbidden", "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search parameter 'forbidden' is not allowed");
    }
  }

  @Nested
  @DisplayName("FHIR Control Parameter Tests")
  class FhirControlParameterTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "_count", "_offset", "_sort", "_format", "_pretty",
          "_total", "_summary", "_elements", "_include", "_revinclude",
          "_contained", "_containedType"
        })
    @DisplayName("Should allow FHIR control parameters")
    void shouldAllowFhirControlParameters(String controlParam) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com", controlParam, "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow multiple FHIR control parameters")
    void shouldAllowMultipleFhirControlParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "_count", "10",
              "_offset", "0",
              "_sort", "name",
              "_format", "json");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow only FHIR control parameters without search parameters")
    void shouldAllowOnlyFhirControlParametersWithoutSearchParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "_count", "20",
              "_format", "json");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Parameter Modifier Tests")
  class ParameterModifierTests {

    @ParameterizedTest
    @MethodSource("provideParametersWithModifiers")
    @DisplayName("Should allow parameters with modifiers when base parameter is allowed")
    void shouldAllowParametersWithModifiersWhenBaseIsAllowed(
        String paramWithModifier, String baseParam) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of(baseParam, "url");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com", paramWithModifier, "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject parameters with modifiers when base parameter is not allowed")
    void shouldRejectParametersWithModifiersWhenBaseIsNotAllowed() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "version");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "name:exact", "test");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search parameter 'name:exact' is not allowed");
    }

    @Test
    @DisplayName("Should allow multiple parameters with different modifiers")
    void shouldAllowMultipleParametersWithDifferentModifiers() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("name", "title", "url");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "name:exact", "test",
              "title:contains", "value",
              "url", "http://example.com");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideParametersWithModifiers() {
      return Stream.of(
          Arguments.of("name:exact", "name"),
          Arguments.of("name:contains", "name"),
          Arguments.of("title:text", "title"),
          Arguments.of("status:not", "status"),
          Arguments.of("date:gt", "date"),
          Arguments.of("date:lt", "date"),
          Arguments.of("code:in", "code"),
          Arguments.of("identifier:of-type", "identifier"));
    }
  }

  @Nested
  @DisplayName("Combined Scenario Tests")
  class CombinedScenarioTests {

    @Test
    @DisplayName("Should allow search with valid parameters, modifiers, and control parameters")
    void shouldAllowSearchWithValidParametersModifiersAndControlParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name", "status");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "name:contains", "test",
              "status", "active",
              "_count", "10",
              "_sort", "name",
              "_format", "json");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject search when one parameter is invalid among many valid ones")
    void shouldRejectSearchWhenOneParameterIsInvalidAmongManyValidOnes() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      Set<String> allowedParams = Set.of("url", "code", "system");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "url", "http://example.com",
              "code", "123",
              "system", "http://system.com",
              "unauthorized", "value",
              "_count", "10");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Search parameter 'unauthorized' is not allowed");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle parameter with colon but no modifier value")
    void shouldHandleParameterWithColonButNoModifierValue() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams("name:", "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle parameter with multiple colons")
    void shouldHandleParameterWithMultipleColons() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "identifier");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams("identifier:of-type:text", "value");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle parameter with special characters in name")
    void shouldHandleParameterWithSpecialCharactersInName() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("_id", "system-version");
      Map<String, String[]> params =
          SearchParameterTestDataHelper.createSearchParams(
              "_id", "123",
              "system-version", "1.0");

      when(requestDetails.getRestOperationType()).thenReturn(RestOperationTypeEnum.SEARCH_TYPE);
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(httpServletRequest.getParameterMap()).thenReturn(params);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPostProcessed(
                      requestDetails, httpServletRequest, httpServletResponse))
          .doesNotThrowAnyException();
    }
  }

  /** Helper class for creating test data for search parameter validation tests. */
  static class SearchParameterTestDataHelper {

    /**
     * Creates a map of search parameters from key-value pairs.
     *
     * @param keyValuePairs alternating keys and values
     * @return a map of parameter names to value arrays
     * @throws IllegalArgumentException if the number of arguments is not even
     */
    static Map<String, String[]> createSearchParams(String... keyValuePairs) {
      if (keyValuePairs.length % 2 != 0) {
        throw new IllegalArgumentException("Key-value pairs must be even");
      }

      Map<String, String[]> params = new HashMap<>();
      for (int i = 0; i < keyValuePairs.length; i += 2) {
        params.put(keyValuePairs[i], new String[] {keyValuePairs[i + 1]});
      }
      return params;
    }
  }
}
