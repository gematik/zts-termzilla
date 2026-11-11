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

import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Utility class for populating {@link ValueSet} expansion metadata.
 *
 * <p>Provides methods to transfer the input parameters of an {@code $expand} operation and the
 * resolved {@link VsExpandPartResult.VersionSelectionStrategy version selection strategies} into
 * the {@link ValueSet#getExpansion() expansion} section of the result ValueSet, as required by the
 * FHIR specification for reproducible expansions.
 */
public class ExpansionHelper {

  private ExpansionHelper() {
    throw new AssertionError("ExpansionHelper is a utility class and should not be instantiated");
  }

  /**
   * Adds the parameters used during expansion to the expansion section of the result ValueSet.
   *
   * <p>Non-null, non-empty input parameters (displayLanguage, activeOnly, filter, count, offset,
   * etc.) are added as named expansion parameters. Version selection strategies are mapped to the
   * corresponding FHIR parameters ({@code force-system-version}, {@code check-system-version},
   * {@code system-version}).
   *
   * @param vsExpandParameter the input parameters of the {@code $expand} operation
   * @param resultVs the result ValueSet whose expansion section receives the parameters
   * @param versionSelectionStrategyMap maps each CodeSystem URL (with version) to the strategy that
   *     determined its version
   */
  public static void addUsedParametersToExpansion(
      VsExpandParameter vsExpandParameter,
      ValueSet resultVs,
      Map<String, VsExpandPartResult.VersionSelectionStrategy> versionSelectionStrategyMap) {

    addStandardExpansionParameters(vsExpandParameter, resultVs);
    addVersionStrategyParameters(resultVs, versionSelectionStrategyMap);
  }

  /**
   * Adds all standard {@code $expand} input parameters to the expansion when present.
   *
   * <p>This includes display-language, active-only, include-designations, designation,
   * include-definition, count, offset, and filter.
   *
   * @param vsExpandParameter the parsed {@code $expand} request parameters
   * @param resultVs the result ValueSet whose expansion section is updated
   */
  private static void addStandardExpansionParameters(
      VsExpandParameter vsExpandParameter, ValueSet resultVs) {
    addExpansionParameterIfPresent(
        resultVs, DISPLAY_LANGUAGE_PARAM, vsExpandParameter.getDisplayLanguageParam());
    addExpansionParameterIfPresent(
        resultVs, ACTIVE_ONLY_PARAM, vsExpandParameter.getActiveOnlyParam());
    addExpansionParameterIfPresent(
        resultVs, INCLUDE_DESIGNATIONS_PARAM, vsExpandParameter.getIncludeDesignationsParam());
    addDesignationParameters(vsExpandParameter.getDesignationParam(), resultVs);
    addExpansionParameterIfPresent(
        resultVs, INCLUDE_DEFINITION_PARAM, vsExpandParameter.getIncludeDefinitionParam());
    addExpansionParameterIfPresent(resultVs, COUNT_PARAM, vsExpandParameter.getCountParam());
    addExpansionParameterIfPresent(resultVs, OFFSET_PARAM, vsExpandParameter.getOffsetParam());
    addExpansionParameterIfPresent(resultVs, FILTER_PARAM, vsExpandParameter.getFilterParam());
    addUseSupplementParameters(vsExpandParameter.getUseSupplementParam(), resultVs);
  }

  /**
   * Adds a single expansion parameter when the given FHIR value is non-null and non-empty.
   *
   * @param resultVs the result ValueSet whose expansion section is updated
   * @param name the parameter name to write
   * @param value the parameter value to write if present
   */
  private static void addExpansionParameterIfPresent(ValueSet resultVs, String name, Type value) {
    if (value != null && !value.isEmpty()) {
      resultVs.getExpansion().addParameter().setName(name).setValue(value);
    }
  }

  /**
   * Adds repeated {@code designation} parameters to the result expansion.
   *
   * @param designationParameters the raw designation parameter values, possibly {@code null}
   * @param resultVs the result ValueSet whose expansion section is updated
   */
  private static void addDesignationParameters(
      List<StringType> designationParameters, ValueSet resultVs) {
    if (designationParameters == null) {
      return;
    }

    for (StringType designationParam : designationParameters) {
      resultVs.getExpansion().addParameter().setName(DESIGNATION_PARAM).setValue(designationParam);
    }
  }

  /**
   * Adds repeated {@code useSupplement} parameters to the result expansion.
   *
   * @param useSupplementParameters the raw useSupplement parameter values, possibly {@code null}
   * @param resultVs the result ValueSet whose expansion section is updated
   */
  private static void addUseSupplementParameters(
      List<CanonicalType> useSupplementParameters, ValueSet resultVs) {
    if (useSupplementParameters == null) {
      return;
    }
    for (CanonicalType useSupplement : useSupplementParameters) {
      if (useSupplement != null && !useSupplement.isEmpty()) {
        resultVs
            .getExpansion()
            .addParameter()
            .setName(
                VsExpandParameter
                    .USE_SUPPLEMENT_PARAM)
            .setValue(useSupplement);
      }
    }
  }

  /**
   * Adds expansion parameters that encode the resolved version-selection strategies.
   *
   * @param resultVs the result ValueSet whose expansion section is updated
   * @param versionSelectionStrategyMap mapping from {@code system|version} to applied strategy
   */
  private static void addVersionStrategyParameters(
      ValueSet resultVs,
      Map<String, VsExpandPartResult.VersionSelectionStrategy> versionSelectionStrategyMap) {

    // Add the corresponding FHIR parameters for the resolved version selection strategy.
    for (Map.Entry<String, VsExpandPartResult.VersionSelectionStrategy> entry :
        versionSelectionStrategyMap.entrySet()) {
      addVersionStrategyParameter(resultVs, entry.getKey(), entry.getValue());
    }
  }

  /**
   * Adds one expansion parameter for a single version-selection strategy entry, if supported.
   *
   * @param resultVs the result ValueSet whose expansion section is updated
   * @param systemVersion the {@code system|version} token
   * @param strategy the applied version-selection strategy
   */
  private static void addVersionStrategyParameter(
      ValueSet resultVs,
      String systemVersion,
      VsExpandPartResult.VersionSelectionStrategy strategy) {
    String parameterName = resolveVersionSelectionParameterName(strategy);
    if (parameterName != null) {
      resultVs
          .getExpansion()
          .addParameter()
          .setName(parameterName)
          .setValue(new StringType(systemVersion));
    }
  }

  /**
   * Resolves the FHIR expansion parameter name for a version-selection strategy.
   *
   * @param strategy the strategy to map
   * @return the matching parameter name, or {@code null} if no parameter should be added
   */
  private static String resolveVersionSelectionParameterName(
      VsExpandPartResult.VersionSelectionStrategy strategy) {
    if (strategy == VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_FORCE) {
      return FORCE_SYSTEM_VERSION_PARAM;
    }
    if (strategy == VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_CHECK) {
      return CHECK_SYSTEM_VERSION_PARAM;
    }
    if (strategy == VsExpandPartResult.VersionSelectionStrategy.EXTERNALLY_SET
        || strategy == VsExpandPartResult.VersionSelectionStrategy.SYSTEM_DEFAULT) {
      return SYSTEM_VERSION_PARAM;
    }
    return null;
  }
}
