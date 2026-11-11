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
package de.gematik.zts.termzilla.server.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CapabilityStatementEnhancer}.
 *
 * <p>Tests the enhancer's ability to modify and filter CapabilityStatements based on configuration
 * settings.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CapabilityStatementEnhancer Tests")
class CapabilityStatementEnhancerTest {

  @Mock private ConfigProperties configProperties;

  private CapabilityStatement capabilityStatement;

  @BeforeEach
  void setUp() {
    capabilityStatement = CapabilityStatementTestDataHelper.createBasicCapabilityStatement();

    // Set up default lenient stubbings for all resource types to avoid strict stubbing errors
    lenient().when(configProperties.isReadEnabledForResource(anyString())).thenReturn(true);
    lenient().when(configProperties.isSearchEnabledForResource(anyString())).thenReturn(true);
    lenient()
        .when(configProperties.getAllowedSearchParamsForResource(anyString()))
        .thenReturn(null);
    lenient().when(configProperties.isOperationEnabled(anyString(), anyString())).thenReturn(true);

    // Set up default capability statement configuration values
    lenient()
        .when(configProperties.getCapabilitiesName())
        .thenReturn("TERMzillaTerminologyServices");
    lenient()
        .when(configProperties.getCapabilitiesTitle())
        .thenReturn("TERMzilla Terminology Services");
    lenient()
        .when(configProperties.getCapabilitiesDescription())
        .thenReturn("Terminology Service for FHIR Terminologies");
    lenient().when(configProperties.getCapabilitiesPublisher()).thenReturn("Fraunhofer FOKUS");
    lenient().when(configProperties.getCapabilitiesStatus()).thenReturn("active");
    lenient().when(configProperties.isCapabilitiesExperimental()).thenReturn(true);
    lenient().when(configProperties.getCapabilitiesSoftwareName()).thenReturn("TERMzilla");
    lenient().when(configProperties.getCapabilitiesSoftwareVersion()).thenReturn("0.1.0");
    lenient().when(configProperties.getCapabilitiesSoftwareReleaseDate()).thenReturn("2023-10-01");
    lenient()
        .when(configProperties.getCapabilitiesRestSecurityDescription())
        .thenReturn("No security mechanisms are enabled for this instance");
  }

  @Nested
  @DisplayName("Basic Enhancement Tests")
  class BasicEnhancementTests {

    @Test
    @DisplayName("Should set basic metadata fields from configuration")
    void shouldSetBasicMetadataFieldsFromConfiguration() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      when(configProperties.getCapabilitiesName()).thenReturn("CustomServerName");
      when(configProperties.getCapabilitiesTitle()).thenReturn("Custom Server Title");
      when(configProperties.getCapabilitiesDescription()).thenReturn("Custom Description");
      when(configProperties.getCapabilitiesPublisher()).thenReturn("Custom Publisher");
      when(configProperties.getCapabilitiesStatus()).thenReturn("draft");
      when(configProperties.isCapabilitiesExperimental()).thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(statement.getName()).isEqualTo("CustomServerName");
      assertThat(statement.getTitle()).isEqualTo("Custom Server Title");
      assertThat(statement.getDescription()).isEqualTo("Custom Description");
      assertThat(statement.getExperimental()).isFalse();
      assertThat(statement.getPublisher()).isEqualTo("Custom Publisher");
      assertThat(statement.getStatus()).isEqualTo(Enumerations.PublicationStatus.DRAFT);
      assertThat(statement.getKind())
          .isEqualTo(CapabilityStatement.CapabilityStatementKind.INSTANCE);
      assertThat(statement.getFhirVersion()).isEqualTo(Enumerations.FHIRVersion._4_0_1);
    }

    @Test
    @DisplayName("Should set software component from configuration")
    void shouldSetSoftwareComponentFromConfiguration() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      when(configProperties.getCapabilitiesSoftwareName()).thenReturn("CustomSoftware");
      when(configProperties.getCapabilitiesSoftwareVersion()).thenReturn("2.0.0");
      when(configProperties.getCapabilitiesSoftwareReleaseDate()).thenReturn("2024-01-15");

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(statement.getSoftware()).isNotNull();
      assertThat(statement.getSoftware().getName()).isEqualTo("CustomSoftware");
      assertThat(statement.getSoftware().getVersion()).isEqualTo("2.0.0");
      assertThat(statement.getSoftware().getReleaseDateElement().getValueAsString())
          .isEqualTo("2024-01-15");
    }

    @Test
    @DisplayName("Should configure REST security settings from configuration")
    void shouldConfigureRestSecuritySettingsFromConfiguration() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createBasicCapabilityStatement();
      when(configProperties.getCapabilitiesRestSecurityDescription())
          .thenReturn("Custom security description");

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      List<CapabilityStatement.CapabilityStatementRestComponent> restComponents =
          statement.getRest();
      assertThat(restComponents).isNotEmpty();

      CapabilityStatement.CapabilityStatementRestComponent serverRest =
          restComponents.stream()
              .filter(rest -> rest.getMode() == CapabilityStatement.RestfulCapabilityMode.SERVER)
              .findFirst()
              .orElseThrow();

      assertThat(serverRest.getSecurity().getCors()).isTrue();
      assertThat(serverRest.getSecurity().getDescription())
          .isEqualTo("Custom security description");
    }

    @ParameterizedTest
    @MethodSource("providePublicationStatuses")
    @DisplayName("Should handle different publication statuses")
    void shouldHandleDifferentPublicationStatuses(
        String statusCode, Enumerations.PublicationStatus expectedStatus) {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      when(configProperties.getCapabilitiesStatus()).thenReturn(statusCode);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(statement.getStatus()).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> providePublicationStatuses() {
      return Stream.of(
          Arguments.of("active", Enumerations.PublicationStatus.ACTIVE),
          Arguments.of("draft", Enumerations.PublicationStatus.DRAFT),
          Arguments.of("retired", Enumerations.PublicationStatus.RETIRED),
          Arguments.of("unknown", Enumerations.PublicationStatus.UNKNOWN));
    }
  }

  @Nested
  @DisplayName("System Interaction Enhancement Tests")
  class SystemInteractionEnhancementTests {

    @Test
    @DisplayName("Should replace TRANSACTION interaction with BATCH on server REST")
    void shouldReplaceTransactionInteractionWithBatchOnServerRest() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createCapabilityStatementWithSystemInteractions(
              CapabilityStatement.SystemRestfulInteraction.TRANSACTION);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(
              CapabilityStatementTestDataHelper.hasSystemInteraction(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.TRANSACTION))
          .isFalse();
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.BATCH))
          .isEqualTo(1);
    }

    @Test
    @DisplayName("Should not add BATCH interaction when TRANSACTION is not present")
    void shouldNotAddBatchInteractionWhenTransactionIsNotPresent() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.BATCH))
          .isZero();
    }

    @Test
    @DisplayName("Should replace TRANSACTION with BATCH only for server REST components")
    void shouldReplaceTransactionWithBatchOnlyForServerRestComponents() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      statement
          .addRest()
          .setMode(CapabilityStatement.RestfulCapabilityMode.SERVER)
          .addInteraction()
          .setCode(CapabilityStatement.SystemRestfulInteraction.TRANSACTION);
      statement
          .addRest()
          .setMode(CapabilityStatement.RestfulCapabilityMode.CLIENT)
          .addInteraction()
          .setCode(CapabilityStatement.SystemRestfulInteraction.TRANSACTION);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(
              CapabilityStatementTestDataHelper.hasSystemInteraction(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.TRANSACTION))
          .isFalse();
      assertThat(
              CapabilityStatementTestDataHelper.hasSystemInteraction(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.CLIENT,
                  CapabilityStatement.SystemRestfulInteraction.TRANSACTION))
          .isTrue();
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.BATCH))
          .isEqualTo(1);
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.CLIENT,
                  CapabilityStatement.SystemRestfulInteraction.BATCH))
          .isZero();
    }

    @Test
    @DisplayName(
        "Should remove TRANSACTION and keep single BATCH when BATCH already exists on server REST")
    void shouldRemoveTransactionAndKeepSingleBatchWhenBatchAlreadyExistsOnServerRest() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      rest.addInteraction().setCode(CapabilityStatement.SystemRestfulInteraction.TRANSACTION);
      rest.addInteraction().setCode(CapabilityStatement.SystemRestfulInteraction.BATCH);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.TRANSACTION))
          .isZero();
      assertThat(
              CapabilityStatementTestDataHelper.countSystemInteractions(
                  statement,
                  CapabilityStatement.RestfulCapabilityMode.SERVER,
                  CapabilityStatement.SystemRestfulInteraction.BATCH))
          .isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Interaction Filtering Tests")
  class InteractionFilteringTests {

    @Test
    @DisplayName("Should keep READ interaction when enabled")
    void shouldKeepReadInteractionWhenEnabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.READ))
          .isTrue();
    }

    @Test
    @DisplayName("Should remove READ interaction when disabled")
    void shouldRemoveReadInteractionWhenDisabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.READ))
          .isFalse();
    }

    @Test
    @DisplayName("Should keep SEARCH interaction when enabled")
    void shouldKeepSearchInteractionWhenEnabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE))
          .isTrue();
    }

    @Test
    @DisplayName("Should remove SEARCH interaction when disabled")
    void shouldRemoveSearchInteractionWhenDisabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE))
          .isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideResourceTypesWithInteractionSettings")
    @DisplayName("Should filter interactions correctly for all resource types")
    void shouldFilterInteractionsCorrectlyForAllResourceTypes(
        String resourceType, boolean readEnabled, boolean searchEnabled) {
      // GIVEN
      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(readEnabled);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(searchEnabled);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.READ))
          .isEqualTo(readEnabled);
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE))
          .isEqualTo(searchEnabled);
    }

    private static Stream<Arguments> provideResourceTypesWithInteractionSettings() {
      return Stream.of(
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, true, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, true, false),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, false, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM, false, false),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, true, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_VALUE_SET, false, false),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, true, true),
          Arguments.of(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP, false, false));
    }
  }

  @Nested
  @DisplayName("Search Parameter Filtering Tests")
  class SearchParameterFilteringTests {

    @Test
    @DisplayName("Should keep allowed search parameters")
    void shouldKeepAllowedSearchParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name", "status");

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "url")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "name")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "status")).isTrue();
    }

    @Test
    @DisplayName("Should remove disallowed search parameters")
    void shouldRemoveDisallowedSearchParameters() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "url")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "name")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "version")).isFalse();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "title")).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideFhirControlParameters")
    @DisplayName("Should always keep FHIR control parameters")
    void shouldAlwaysKeepFhirControlParameters(String controlParam) {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;
      Set<String> allowedParams = Set.of("url"); // Limited set

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, controlParam)).isTrue();
    }

    @Test
    @DisplayName("Should clear all search parameters when search is disabled")
    void shouldClearAllSearchParametersWhenSearchIsDisabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(resource.getSearchParam()).isEmpty();
    }

    @Test
    @DisplayName("Should keep all parameters when no restrictions configured")
    void shouldKeepAllParametersWhenNoRestrictionsConfigured() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType)).thenReturn(null);

      int originalParamCount =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType)
              .getSearchParam()
              .size();

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(resource.getSearchParam()).hasSize(originalParamCount);
    }

    @Test
    @DisplayName("Should keep all parameters when empty set is configured")
    void shouldKeepAllParametersWhenEmptySetIsConfigured() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType)).thenReturn(Set.of());

      int originalParamCount =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType)
              .getSearchParam()
              .size();

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(resource.getSearchParam()).hasSize(originalParamCount);
    }

    private static Stream<Arguments> provideFhirControlParameters() {
      return Stream.of(
          Arguments.of("_id"),
          Arguments.of("_content"),
          Arguments.of("_sort"),
          Arguments.of("_source"));
    }
  }

  @Nested
  @DisplayName("Operation Filtering Tests")
  class OperationFilteringTests {

    @Test
    @DisplayName("Should keep enabled operations")
    void shouldKeepEnabledOperations() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;

      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_EXPAND))
          .thenReturn(true);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_VALIDATE_CODE))
          .thenReturn(true);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "expand")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "validate-code"))
          .isTrue();
    }

    @Test
    @DisplayName("Should remove disabled operations")
    void shouldRemoveDisabledOperations() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_LOOKUP))
          .thenReturn(true);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_VALIDATE_CODE))
          .thenReturn(true);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_SUBSUMES))
          .thenReturn(false);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_FIND_MATCHES))
          .thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "lookup")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "validate-code"))
          .isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "subsumes")).isFalse();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "find-matches"))
          .isFalse();
    }

    @Test
    @DisplayName("Should remove all operations when all are disabled")
    void shouldRemoveAllOperationsWhenAllAreDisabled() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP;

      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_TRANSLATE))
          .thenReturn(false);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_CLOSURE))
          .thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(resource.getOperation()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Combined Filtering Tests")
  class CombinedFilteringTests {

    @Test
    @DisplayName("Should apply all filters together correctly")
    void shouldApplyAllFiltersTogether() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;
      Set<String> allowedParams = Set.of("url", "name");

      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType))
          .thenReturn(allowedParams);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_EXPAND))
          .thenReturn(true);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_VALIDATE_CODE))
          .thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      // Check interactions
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.READ))
          .isTrue();
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE))
          .isTrue();

      // Check search parameters
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "url")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "name")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasSearchParam(resource, "version")).isFalse();

      // Check operations
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "expand")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "validate-code"))
          .isFalse();
    }

    @Test
    @DisplayName("Should handle completely disabled resource")
    void shouldHandleCompletelyDisabledResource() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM;

      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(false);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(false);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_LOOKUP))
          .thenReturn(false);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_VALIDATE_CODE))
          .thenReturn(false);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_SUBSUMES))
          .thenReturn(false);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_FIND_MATCHES))
          .thenReturn(false);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(resource.getInteraction()).isEmpty();
      assertThat(resource.getSearchParam()).isEmpty();
      assertThat(resource.getOperation()).isEmpty();
    }

    @Test
    @DisplayName("Should handle fully enabled resource with no restrictions")
    void shouldHandleFullyEnabledResourceWithNoRestrictions() {
      // GIVEN
      String resourceType = ConfigProperties.RESOURCE_TYPE_VALUE_SET;

      when(configProperties.isReadEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.isSearchEnabledForResource(resourceType)).thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(resourceType)).thenReturn(null);
      when(configProperties.isOperationEnabled(resourceType, ConfigProperties.OPERATION_EXPAND))
          .thenReturn(true);
      when(configProperties.isOperationEnabled(
              resourceType, ConfigProperties.OPERATION_VALIDATE_CODE))
          .thenReturn(true);

      CapabilityStatement.CapabilityStatementRestResourceComponent originalResource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);
      int originalSearchParamCount = originalResource.getSearchParam().size();

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(capabilityStatement, resourceType);

      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.READ))
          .isTrue();
      assertThat(
              CapabilityStatementTestDataHelper.hasInteraction(
                  resource, CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE))
          .isTrue();
      assertThat(resource.getSearchParam()).hasSize(originalSearchParamCount);
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "expand")).isTrue();
      assertThat(CapabilityStatementTestDataHelper.hasOperation(resource, "validate-code"))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty capability statement")
    void shouldHandleEmptyCapabilityStatement() {
      // GIVEN
      CapabilityStatement emptyStatement = new CapabilityStatement();
      emptyStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(emptyStatement, configProperties);

      // THEN
      assertThat(emptyStatement.getName()).isEqualTo("TERMzillaTerminologyServices");
      assertThat(emptyStatement.getSoftware()).isNotNull();
    }

    @Test
    @DisplayName("Should handle capability statement with no REST component")
    void shouldHandleCapabilityStatementWithNoRestComponent() {
      // GIVEN
      CapabilityStatement statement = new CapabilityStatement();

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      assertThat(statement.getName()).isEqualTo("TERMzillaTerminologyServices");
    }

    @Test
    @DisplayName("Should handle resource with no interactions")
    void shouldHandleResourceWithNoInteractions() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(
              statement, ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);
      assertThat(resource.getInteraction()).isEmpty();
    }

    @Test
    @DisplayName("Should handle resource with no search parameters")
    void shouldHandleResourceWithNoSearchParameters() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_VALUE_SET);

      when(configProperties.isSearchEnabledForResource(ConfigProperties.RESOURCE_TYPE_VALUE_SET))
          .thenReturn(true);
      when(configProperties.getAllowedSearchParamsForResource(
              ConfigProperties.RESOURCE_TYPE_VALUE_SET))
          .thenReturn(Set.of("url"));

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(
              statement, ConfigProperties.RESOURCE_TYPE_VALUE_SET);
      assertThat(resource.getSearchParam()).isEmpty();
    }

    @Test
    @DisplayName("Should handle resource with no operations")
    void shouldHandleResourceWithNoOperations() {
      // GIVEN
      CapabilityStatement statement =
          CapabilityStatementTestDataHelper.createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP);

      // WHEN
      CapabilityStatementEnhancer.enhanceStatement(statement, configProperties);

      // THEN
      CapabilityStatement.CapabilityStatementRestResourceComponent resource =
          CapabilityStatementTestDataHelper.findResource(
              statement, ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP);
      assertThat(resource.getOperation()).isEmpty();
    }
  }

  /** Helper class for creating test data for CapabilityStatement tests. */
  static class CapabilityStatementTestDataHelper {

    /** Creates a minimal CapabilityStatement for testing. */
    static CapabilityStatement createMinimalCapabilityStatement() {
      CapabilityStatement statement = new CapabilityStatement();
      statement.setStatus(Enumerations.PublicationStatus.DRAFT);
      return statement;
    }

    /** Creates a minimal CapabilityStatement with server-level system interactions. */
    static CapabilityStatement createCapabilityStatementWithSystemInteractions(
        CapabilityStatement.SystemRestfulInteraction... interactions) {
      CapabilityStatement statement = createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

      for (CapabilityStatement.SystemRestfulInteraction interaction : interactions) {
        rest.addInteraction().setCode(interaction);
      }

      return statement;
    }

    /** Creates a basic CapabilityStatement with all resource types and common operations. */
    static CapabilityStatement createBasicCapabilityStatement() {
      CapabilityStatement statement = createMinimalCapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent rest =
          statement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

      // Add CodeSystem resource
      CapabilityStatement.CapabilityStatementRestResourceComponent codeSystemResource =
          rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_CODE_SYSTEM);
      addInteractions(codeSystemResource);
      addCodeSystemSearchParams(codeSystemResource);
      addCodeSystemOperations(codeSystemResource);

      // Add ValueSet resource
      CapabilityStatement.CapabilityStatementRestResourceComponent valueSetResource =
          rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_VALUE_SET);
      addInteractions(valueSetResource);
      addValueSetSearchParams(valueSetResource);
      addValueSetOperations(valueSetResource);

      // Add ConceptMap resource
      CapabilityStatement.CapabilityStatementRestResourceComponent conceptMapResource =
          rest.addResource().setType(ConfigProperties.RESOURCE_TYPE_CONCEPT_MAP);
      addInteractions(conceptMapResource);
      addConceptMapSearchParams(conceptMapResource);
      addConceptMapOperations(conceptMapResource);

      return statement;
    }

    private static void addInteractions(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.READ);
      resource.addInteraction().setCode(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE);
    }

    private static void addCodeSystemSearchParams(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addSearchParam().setName("_id").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("_content").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("_sort").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("_source").setType(Enumerations.SearchParamType.URI);
      resource.addSearchParam().setName("url").setType(Enumerations.SearchParamType.URI);
      resource.addSearchParam().setName("version").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("name").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("title").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("status").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("code").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("system").setType(Enumerations.SearchParamType.URI);
    }

    private static void addValueSetSearchParams(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addSearchParam().setName("_id").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("_content").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("_sort").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("_source").setType(Enumerations.SearchParamType.URI);
      resource.addSearchParam().setName("url").setType(Enumerations.SearchParamType.URI);
      resource.addSearchParam().setName("version").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("name").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("title").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("status").setType(Enumerations.SearchParamType.TOKEN);
    }

    private static void addConceptMapSearchParams(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addSearchParam().setName("_id").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("_content").setType(Enumerations.SearchParamType.STRING);
      resource.addSearchParam().setName("url").setType(Enumerations.SearchParamType.URI);
      resource.addSearchParam().setName("version").setType(Enumerations.SearchParamType.TOKEN);
      resource.addSearchParam().setName("name").setType(Enumerations.SearchParamType.STRING);
    }

    private static void addCodeSystemOperations(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addOperation().setName("lookup");
      resource.addOperation().setName("validate-code");
      resource.addOperation().setName("subsumes");
      resource.addOperation().setName("find-matches");
    }

    private static void addValueSetOperations(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addOperation().setName("expand");
      resource.addOperation().setName("validate-code");
    }

    private static void addConceptMapOperations(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource) {
      resource.addOperation().setName("translate");
      resource.addOperation().setName("closure");
    }

    /** Finds a resource component by type. */
    static CapabilityStatement.CapabilityStatementRestResourceComponent findResource(
        CapabilityStatement statement, String resourceType) {
      return statement.getRest().stream()
          .filter(rest -> rest.getMode() == CapabilityStatement.RestfulCapabilityMode.SERVER)
          .flatMap(rest -> rest.getResource().stream())
          .filter(resource -> resource.getType().equals(resourceType))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Resource not found: " + resourceType));
    }

    /** Checks if a resource has a specific interaction. */
    static boolean hasInteraction(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource,
        CapabilityStatement.TypeRestfulInteraction interaction) {
      return resource.getInteraction().stream().anyMatch(i -> i.getCode() == interaction);
    }

    /** Checks if a resource has a specific search parameter. */
    static boolean hasSearchParam(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource, String paramName) {
      return resource.getSearchParam().stream()
          .anyMatch(param -> param.getName().equals(paramName));
    }

    /** Checks if a resource has a specific operation. */
    static boolean hasOperation(
        CapabilityStatement.CapabilityStatementRestResourceComponent resource,
        String operationName) {
      return resource.getOperation().stream().anyMatch(op -> op.getName().equals(operationName));
    }

    /** Checks whether a REST component mode exposes a specific system interaction. */
    static boolean hasSystemInteraction(
        CapabilityStatement statement,
        CapabilityStatement.RestfulCapabilityMode mode,
        CapabilityStatement.SystemRestfulInteraction interaction) {
      return countSystemInteractions(statement, mode, interaction) > 0;
    }

    /** Counts occurrences of a system interaction for a REST component mode. */
    static long countSystemInteractions(
        CapabilityStatement statement,
        CapabilityStatement.RestfulCapabilityMode mode,
        CapabilityStatement.SystemRestfulInteraction interaction) {
      return statement.getRest().stream()
          .filter(rest -> rest.getMode() == mode)
          .flatMap(rest -> rest.getInteraction().stream())
          .filter(systemInteraction -> systemInteraction.getCode() == interaction)
          .count();
    }
  }
}
