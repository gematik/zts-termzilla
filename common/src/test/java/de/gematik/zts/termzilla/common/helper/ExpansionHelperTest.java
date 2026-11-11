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
package de.gematik.zts.termzilla.common.helper;

import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.ACTIVE_ONLY_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.CHECK_SYSTEM_VERSION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.COUNT_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.DESIGNATION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.DISPLAY_LANGUAGE_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.FILTER_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.FORCE_SYSTEM_VERSION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.INCLUDE_DEFINITION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.INCLUDE_DESIGNATIONS_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.OFFSET_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.SYSTEM_VERSION_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult.VersionSelectionStrategy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExpansionHelper}.
 *
 * <p>Verifies that expansion parameters and version selection strategies are correctly added to the
 * ValueSet expansion output.
 */
@DisplayName("ExpansionHelper Tests")
class ExpansionHelperTest {

  private ValueSet resultVs;

  @BeforeEach
  void setUp() {
    resultVs = new ValueSet();
    resultVs.getExpansion(); // Sicherstellen, dass die Expansion initialisiert ist
  }

  // ---------------------------------------------------------------------------
  // addUsedParametersToExpansion
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("addUsedParametersToExpansion")
  class AddUsedParametersToExpansionTests {

    @Test
    @DisplayName("Should add no parameters when all values are null")
    void shouldAddNoParametersWhenAllValuesAreNull() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }

    @Test
    @DisplayName("Should add displayLanguage parameter")
    void shouldAddDisplayLanguageParameter() {
      // GIVEN
      CodeType displayLanguage = new CodeType("de");
      VsExpandParameter params =
          VsExpandParameter.builder().displayLanguageParam(displayLanguage).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName())
          .isEqualTo(DISPLAY_LANGUAGE_PARAM);
    }

    @Test
    @DisplayName("Should add activeOnly parameter")
    void shouldAddActiveOnlyParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().activeOnlyParam(new BooleanType(true)).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName())
          .isEqualTo(ACTIVE_ONLY_PARAM);
    }

    @Test
    @DisplayName("Should add includeDesignations parameter")
    void shouldAddIncludeDesignationsParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().includeDesignationsParam(new BooleanType(true)).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName())
          .isEqualTo(INCLUDE_DESIGNATIONS_PARAM);
    }

    @Test
    @DisplayName("Should add multiple designation parameters")
    void shouldAddMultipleDesignationParameters() {
      // GIVEN
      List<StringType> designations =
          List.of(
              new StringType("http://snomed.info/sct|900000000000003001"),
              new StringType("http://snomed.info/sct|900000000000013009"));
      VsExpandParameter params = VsExpandParameter.builder().designationParam(designations).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(2);
      assertThat(resultVs.getExpansion().getParameter())
          .allSatisfy(p -> assertThat(p.getName()).isEqualTo(DESIGNATION_PARAM));
    }

    @Test
    @DisplayName("Should not add designation parameter when list is null")
    void shouldNotAddDesignationParameterWhenNull() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().designationParam(null).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }

    @Test
    @DisplayName("Should add includeDefinition parameter")
    void shouldAddIncludeDefinitionParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().includeDefinitionParam(new BooleanType(true)).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName())
          .isEqualTo(INCLUDE_DEFINITION_PARAM);
    }

    @Test
    @DisplayName("Should add count parameter")
    void shouldAddCountParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().countParam(new IntegerType(100)).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName()).isEqualTo(COUNT_PARAM);
    }

    @Test
    @DisplayName("Should add offset parameter")
    void shouldAddOffsetParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().offsetParam(new IntegerType(10)).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName()).isEqualTo(OFFSET_PARAM);
    }

    @Test
    @DisplayName("Should add filter parameter")
    void shouldAddFilterParameter() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder().filterParam(new StringType("heart")).build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      assertThat(resultVs.getExpansion().getParameter().get(0).getName()).isEqualTo(FILTER_PARAM);
    }

    @Test
    @DisplayName("Should add all parameters when all are set")
    void shouldAddAllParametersWhenAllAreSet() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder()
              .displayLanguageParam(new CodeType("en"))
              .activeOnlyParam(new BooleanType(true))
              .includeDesignationsParam(new BooleanType(false))
              .designationParam(List.of(new StringType("designation-1")))
              .includeDefinitionParam(new BooleanType(true))
              .countParam(new IntegerType(50))
              .offsetParam(new IntegerType(0))
              .filterParam(new StringType("test"))
              .build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(8);
    }

    @Test
    @DisplayName("Should not add parameter when BooleanType is empty")
    void shouldNotAddParameterWhenBooleanTypeIsEmpty() {
      // GIVEN
      VsExpandParameter params =
          VsExpandParameter.builder()
              .activeOnlyParam(new BooleanType())
              .includeDesignationsParam(new BooleanType())
              .includeDefinitionParam(new BooleanType())
              .build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }
  }

  // ---------------------------------------------------------------------------
  // Version selection strategy parameters
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Version Selection Strategy Parameters")
  class VersionSelectionStrategyTests {

    @Test
    @DisplayName("Should add force-system-version for EXTERNALLY_FORCE strategy")
    void shouldAddForceSystemVersionForExternallyForceStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|1.0.0", VersionSelectionStrategy.EXTERNALLY_FORCE);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      ValueSet.ValueSetExpansionParameterComponent param =
          resultVs.getExpansion().getParameter().get(0);
      assertThat(param.getName()).isEqualTo(FORCE_SYSTEM_VERSION_PARAM);
      assertThat(param.getValue().primitiveValue()).isEqualTo("http://example.org|1.0.0");
    }

    @Test
    @DisplayName("Should add check-system-version for EXTERNALLY_CHECK strategy")
    void shouldAddCheckSystemVersionForExternallyCheckStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|2.0.0", VersionSelectionStrategy.EXTERNALLY_CHECK);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      ValueSet.ValueSetExpansionParameterComponent param =
          resultVs.getExpansion().getParameter().get(0);
      assertThat(param.getName()).isEqualTo(CHECK_SYSTEM_VERSION_PARAM);
      assertThat(param.getValue().primitiveValue()).isEqualTo("http://example.org|2.0.0");
    }

    @Test
    @DisplayName("Should add system-version for EXTERNALLY_SET strategy")
    void shouldAddSystemVersionForExternallySetStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|3.0.0", VersionSelectionStrategy.EXTERNALLY_SET);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      ValueSet.ValueSetExpansionParameterComponent param =
          resultVs.getExpansion().getParameter().get(0);
      assertThat(param.getName()).isEqualTo(SYSTEM_VERSION_PARAM);
      assertThat(param.getValue().primitiveValue()).isEqualTo("http://example.org|3.0.0");
    }

    @Test
    @DisplayName("Should add system-version for SYSTEM_DEFAULT strategy")
    void shouldAddSystemVersionForSystemDefaultStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|4.0.0", VersionSelectionStrategy.SYSTEM_DEFAULT);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(1);
      ValueSet.ValueSetExpansionParameterComponent param =
          resultVs.getExpansion().getParameter().get(0);
      assertThat(param.getName()).isEqualTo(SYSTEM_VERSION_PARAM);
      assertThat(param.getValue().primitiveValue()).isEqualTo("http://example.org|4.0.0");
    }

    @Test
    @DisplayName("Should not add parameter for UNKNOWN strategy")
    void shouldNotAddParameterForUnknownStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|1.0.0", VersionSelectionStrategy.UNKNOWN);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }

    @Test
    @DisplayName("Should not add parameter for DEFINED strategy")
    void shouldNotAddParameterForDefinedStrategy() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of("http://example.org|1.0.0", VersionSelectionStrategy.DEFINED);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }

    @Test
    @DisplayName("Should add parameters for multiple version strategies")
    void shouldAddParametersForMultipleVersionStrategies() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();
      Map<String, VersionSelectionStrategy> strategyMap =
          Map.of(
              "http://example.org/cs-a|1.0.0", VersionSelectionStrategy.EXTERNALLY_FORCE,
              "http://example.org/cs-b|2.0.0", VersionSelectionStrategy.SYSTEM_DEFAULT);

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, strategyMap);

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).hasSize(2);
      assertThat(resultVs.getExpansion().getParameter())
          .extracting(ValueSet.ValueSetExpansionParameterComponent::getName)
          .containsExactlyInAnyOrder(FORCE_SYSTEM_VERSION_PARAM, SYSTEM_VERSION_PARAM);
    }

    @Test
    @DisplayName("Should add no strategy parameters when map is empty")
    void shouldAddNoStrategyParametersWhenMapIsEmpty() {
      // GIVEN
      VsExpandParameter params = VsExpandParameter.builder().build();

      // WHEN
      ExpansionHelper.addUsedParametersToExpansion(params, resultVs, Collections.emptyMap());

      // THEN
      assertThat(resultVs.getExpansion().getParameter()).isEmpty();
    }
  }
}
