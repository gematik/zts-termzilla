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
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
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
 * Unit tests for {@link OperationControlInterceptor}.
 *
 * <p>Tests the interceptor's ability to control access to FHIR operations based on configuration
 * settings.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationControlInterceptor Tests")
class OperationControlInterceptorTest {

  @Mock private ConfigProperties configProperties;

  @Mock private RequestDetails requestDetails;

  private OperationControlInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new OperationControlInterceptor(configProperties);
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
      assertThatCode(() -> new OperationControlInterceptor(config)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Metadata Request Tests")
  class MetadataRequestTests {

    @Test
    @DisplayName("Should allow request when resource type is null")
    void shouldAllowRequestWhenResourceTypeIsNull() {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.METADATA, requestDetails))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("READ Operation Tests")
  class ReadOperationTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
          ConfigProperties.RESOURCE_TYPE_VALUE_SET,
          ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP
        })
    @DisplayName("Should allow READ operation when enabled for resource type")
    void shouldAllowReadOperationWhenEnabled(String resourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
          ConfigProperties.RESOURCE_TYPE_VALUE_SET,
          ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP
        })
    @DisplayName("Should deny READ operation when disabled for resource type")
    void shouldDenyReadOperationWhenDisabled(String resourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(false);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining("READ operation is not enabled for " + resourceType);
    }
  }

  @Nested
  @DisplayName("SEARCH Operation Tests")
  class SearchOperationTests {

    @ParameterizedTest
    @ValueSource(
        strings = {
          ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
          ConfigProperties.RESOURCE_TYPE_VALUE_SET,
          ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP
        })
    @DisplayName("Should allow SEARCH operation when enabled for resource type")
    void shouldAllowSearchOperationWhenEnabled(String resourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.SEARCH_TYPE, requestDetails))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM,
          ConfigProperties.RESOURCE_TYPE_VALUE_SET,
          ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP
        })
    @DisplayName("Should deny SEARCH operation when disabled for resource type")
    void shouldDenySearchOperationWhenDisabled(String resourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(false);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.SEARCH_TYPE, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining("SEARCH operation is not enabled for " + resourceType);
    }
  }

  @Nested
  @DisplayName("Custom Operation Tests")
  class CustomOperationTests {

    @ParameterizedTest
    @MethodSource("provideEnabledOperations")
    @DisplayName("Should allow custom operation when enabled")
    void shouldAllowCustomOperationWhenEnabled(String resourceType, String operation) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(operation);
      when(configProperties.isOperationEnabled(resourceType, operation)).thenReturn(true);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.EXTENDED_OPERATION_TYPE, requestDetails))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("provideDisabledOperations")
    @DisplayName("Should deny custom operation when disabled")
    void shouldDenyCustomOperationWhenDisabled(String resourceType, String operation) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(operation);
      when(configProperties.isOperationEnabled(resourceType, operation)).thenReturn(false);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.EXTENDED_OPERATION_TYPE, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining(operation + " operation is not enabled for " + resourceType);
    }

    @Test
    @DisplayName("Should skip operation check when operation is null")
    void shouldSkipOperationCheckWhenOperationIsNull() {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM))
          .thenReturn(true);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideEnabledOperations() {
      return Stream.of(
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, ConfigProperties.OPERATION_LOOKUP),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, ConfigProperties.OPERATION_VALIDATE_CODE),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, ConfigProperties.OPERATION_EXPAND),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET, ConfigProperties.OPERATION_VALIDATE_CODE),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, ConfigProperties.OPERATION_TRANSLATE));
    }

    private static Stream<Arguments> provideDisabledOperations() {
      return Stream.of(
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, ConfigProperties.OPERATION_SUBSUMES),
          Arguments.of(
              ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, ConfigProperties.OPERATION_CLOSURE),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, "$unknown-operation"));
    }
  }

  @Nested
  @DisplayName("Combined Operation Tests")
  class CombinedOperationTests {

    @Test
    @DisplayName("Should allow READ when enabled but deny custom operation when disabled")
    void shouldAllowReadButDenyCustomOperation() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      String operation = ConfigProperties.OPERATION_SUBSUMES;

      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);

      // WHEN & THEN - READ should succeed
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();

      // GIVEN - now test custom operation
      when(requestDetails.getOperation()).thenReturn(operation);
      when(configProperties.isOperationEnabled(resourceType, operation)).thenReturn(false);

      // WHEN & THEN - custom operation should fail
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining(operation + " operation is not enabled for " + resourceType);
    }

    @Test
    @DisplayName("Should check all operation types independently")
    void shouldCheckAllOperationTypesIndependently() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;

      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(false);

      // WHEN & THEN - READ should succeed
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();

      // WHEN & THEN - SEARCH should fail
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.SEARCH_TYPE, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining("SEARCH operation is not enabled for " + resourceType);
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should allow unknown operation type when no checks apply")
    void shouldAllowUnknownOperationType() {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);
      when(requestDetails.getOperation()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(
                      RestOperationTypeEnum.UPDATE, requestDetails))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow unknown resource types")
    void shouldAllowUnknownResourceTypes() {
      // GIVEN
      String unknownResourceType = "UnknownResource";
      when(requestDetails.getResourceName()).thenReturn(unknownResourceType);
      when(requestDetails.getOperation()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow OperationDefinition resource type")
    void shouldAllowOperationDefinitionResourceType() {
      // GIVEN
      String operationDefinitionResourceType = "OperationDefinition";
      when(requestDetails.getResourceName()).thenReturn(operationDefinitionResourceType);
      when(requestDetails.getOperation()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Patient", "Observation", "Condition", "Medication"})
    @DisplayName("Should allow non-restricted FHIR resource types")
    void shouldAllowNonRestrictedFhirResourceTypes(String resourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(resourceType);
      when(requestDetails.getOperation()).thenReturn(null);

      // WHEN & THEN
      assertThatCode(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("provideRestrictedResourceTypes")
    @DisplayName("Should only restrict configured resource types")
    void shouldOnlyRestrictConfiguredResourceTypes(String restrictedResourceType) {
      // GIVEN
      when(requestDetails.getResourceName()).thenReturn(restrictedResourceType);
      when(requestDetails.getOperation()).thenReturn(null);
      when(configProperties.isReadEnabledForResource(restrictedResourceType)).thenReturn(false);

      // WHEN & THEN
      assertThatThrownBy(
              () ->
                  interceptor.incomingRequestPreHandled(RestOperationTypeEnum.READ, requestDetails))
          .isInstanceOf(MethodNotAllowedException.class)
          .hasMessageContaining("READ operation is not enabled for " + restrictedResourceType);
    }

    private static Stream<Arguments> provideRestrictedResourceTypes() {
      return Stream.of(
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP));
    }
  }
}
