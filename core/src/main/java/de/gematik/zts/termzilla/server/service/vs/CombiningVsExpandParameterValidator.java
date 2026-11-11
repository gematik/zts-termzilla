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
package de.gematik.zts.termzilla.server.service.vs;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Validates input parameters for the {@code ValueSet/$expand} operation.
 *
 * <p>The validator enforces required and mutually exclusive parameters, rejects currently
 * unsupported parameters and unsupported embedded ValueSet content, validates system-version
 * combinations, and checks the optional date constraint.
 */
@Getter
@Setter
public class CombiningVsExpandParameterValidator extends CommonParameterValidator {

  /** Utility class constructor. */
  private CombiningVsExpandParameterValidator() {}

  /**
   * Validates the provided expand parameters against all supported rules.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if one or more validation rules are violated
   */
  public static void validateParameters(VsExpandParameter parameters)
      throws InvalidRequestException {
    failOnUnsupportedParameters(parameters);
    failOnEmptyFilterParameter(parameters);
    failOnMissingUrlOrValueSetParameter(parameters);
    failOnUrlAndValueSetParameter(parameters);
    failOnValueSetVersionWithoutUrl(parameters);
    failOnInvalidSystemVersionParameterCombinations(parameters);
    failOnUnsupportedValueSetContent(parameters);
    failOnConceptAndFilterInSameComposePart(parameters);
    failOnCountZeroAndOffsetSet(parameters);
    validateDateNotInFuture(parameters);
  }

  /**
   * Request-boundary validation for the public {@code $expand} operation.
   *
   * <p>Applies rules that must hold only for external API calls and not for internal reuse (e.g.
   * {@code $validate-code}, which legitimately expands a ValueSet by URL while relying on default
   * version resolution).
   *
   * <p>ZTS requires an explicit {@code valueSetVersion} when selecting a ValueSet by canonical
   * {@code url} at the public API boundary.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if {@code url} is present without {@code valueSetVersion}
   */
  public static void validateRequestBoundaryParameters(VsExpandParameter parameters)
      throws InvalidRequestException {
    failOnUrlWithoutValueSetVersion(parameters);
  }

  /**
   * Ensures that when {@code count} is set to 0, the {@code offset} parameter is not provided, as
   * it would be meaningless in that context.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if {@code count} is 0 and {@code offset} is set
   */
  private static void failOnCountZeroAndOffsetSet(VsExpandParameter parameters)
      throws InvalidRequestException {
    if (parameters.getCountParam() != null
        && parameters.getCountParam().hasValue()
        && parameters.getCountParam().getValue() == 0
        && parameters.getOffsetParam() != null
        && parameters.getOffsetParam().hasValue()) {
      throw new InvalidRequestException(
          "The '"
              + VsExpandParameter.OFFSET_PARAM
              + "' parameter must not be provided when 'count' is set to 0.");
    }
  }

  /**
   * Ensures that if the optional {@code filter} parameter is provided, it must not be empty, as an
   * empty filter would be semantically invalid.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if the {@code filter} parameter is present but empty
   */
  private static void failOnEmptyFilterParameter(VsExpandParameter parameters)
      throws InvalidRequestException {
    if (parameters.getFilterParam() != null
        && parameters.getFilterParam().hasValue()
        && parameters.getFilterParam().getValue().isEmpty()) {
      throw new InvalidRequestException(
          "The '" + VsExpandParameter.FILTER_PARAM + "' parameter must not be empty.");
    }
  }

  /**
   * Ensures that at least one resource selector is provided. Either {@code url} or {@code valueSet}
   * must be present.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if both {@code url} and {@code valueSet} are missing
   */
  private static void failOnMissingUrlOrValueSetParameter(VsExpandParameter parameters)
      throws InvalidRequestException {

    if ((parameters.getUrlParam() == null || parameters.getUrlParam().isEmpty())
        && (parameters.getValueSetParam() == null || parameters.getValueSetParam().isEmpty())) {
      throw new InvalidRequestException(
          "Either the '"
              + VsExpandParameter.URL_PARAM
              + "' or the '"
              + VsExpandParameter.VALUESET_PARAM
              + "' parameter must be provided.");
    }
  }

  /**
   * Ensures that {@code url} and {@code valueSet} are not provided at the same time.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if both {@code url} and {@code valueSet} are present
   */
  private static void failOnUrlAndValueSetParameter(VsExpandParameter parameters)
      throws InvalidRequestException {

    if (parameters.getUrlParam() != null
        && !parameters.getUrlParam().isEmpty()
        && parameters.getValueSetParam() != null
        && !parameters.getValueSetParam().isEmpty()) {
      throw new InvalidRequestException(
          "The '"
              + VsExpandParameter.URL_PARAM
              + "' and the '"
              + VsExpandParameter.VALUESET_PARAM
              + "' parameter must not be provided at the same time.");
    }
  }

  /**
   * Rejects unsupported parameters for the current implementation.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if any unsupported parameter is present
   */
  private static void failOnUnsupportedParameters(VsExpandParameter parameters)
      throws InvalidRequestException {

    if (parameters.getContextParam() != null
        || parameters.getContextDirectionParam() != null
        || parameters.getDateParam() != null
        || parameters.getExcludeNotForUIParam() != null
        || parameters.getExcludePostcoordinatedParam() != null) {
      throw new InvalidRequestException(
          "The '"
              + VsExpandParameter.CONTEXT_PARAM
              + "', '"
              + VsExpandParameter.CONTEXT_DIRECTION_PARAM
              + "', '"
              + VsExpandParameter.DATE_PARAM
              + "', '"
              + VsExpandParameter.EXCLUDE_NOT_FOR_UI_PARAM
              + "' and '"
              + VsExpandParameter.EXCLUDE_POSTCOORDINATED_PARAM
              + "' parameters are not supported yet.");
    }
  }

  /**
   * Validates syntax and cross-parameter constraints of {@code system-version}, {@code
   * check-system-version}, and {@code force-system-version}.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if a version token format is invalid, duplicates exist, or
   *     incompatible combinations are found
   */
  private static void failOnInvalidSystemVersionParameterCombinations(
      VsExpandParameter parameters) {

    HashSet<String> systemVersionSet =
        validateVersionParameter(parameters.getSystemVersionParam(), "system-version");

    HashSet<String> checkSystemVersionSet =
        validateVersionParameter(parameters.getCheckSystemVersionParam(), "check-system-version");

    HashSet<String> forceSystemVersionSet =
        validateVersionParameter(parameters.getForceSystemVersionParam(), "force-system-version");

    validateNoParameterOverlap(
        systemVersionSet, forceSystemVersionSet, "system-version/force-system-version");
    validateNoParameterOverlap(
        systemVersionSet, checkSystemVersionSet, "system-version/check-system-version");
    validateNoParameterOverlap(
        checkSystemVersionSet, forceSystemVersionSet, "check-system-version/force-system-version");
  }

  /**
   * Validates a version parameter list for correct format and uniqueness.
   *
   * @param parameterList the list of canonical type parameter values to validate
   * @param parameterName the name of the parameter for error messages
   * @return set of unique system identifiers found in the parameter
   * @throws InvalidRequestException if format is invalid or duplicates exist
   */
  private static HashSet<String> validateVersionParameter(
      List<CanonicalType> parameterList, String parameterName) {

    HashSet<String> systemSet = new HashSet<>();

    if (parameterList == null) {
      return systemSet;
    }

    for (CanonicalType param : parameterList) {
      String value = param.getValueAsString();
      String[] parts = value.split("\\|");

      if (parts.length != 2) {
        throw new InvalidRequestException(
            String.format("Invalid '%s' Parameter: %s", parameterName, value));
      }

      String systemId = parts[0];
      if (!systemSet.add(systemId)) {
        throw new InvalidRequestException(
            String.format(
                "Invalid '%s' Parameter. Duplicate values for: %s", parameterName, systemId));
      }
    }

    return systemSet;
  }

  /**
   * Validates that two parameter sets have no overlapping system identifiers.
   *
   * @param firstSet the first set of system identifiers
   * @param secondSet the second set of system identifiers
   * @param parameterCombination description of the parameter combination for error messages
   * @throws InvalidRequestException if overlapping identifiers are found
   */
  private static void validateNoParameterOverlap(
      Set<String> firstSet, Set<String> secondSet, String parameterCombination) {

    Set<String> overlap = new HashSet<>(firstSet);
    overlap.retainAll(secondSet);

    if (!overlap.isEmpty()) {
      throw new InvalidRequestException(
          String.format("Invalid '%s' parameter combination: %s", parameterCombination, overlap));
    }
  }

  /**
   * Validates embedded ValueSet content and rejects unsupported constructs.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if the embedded ValueSet is structurally invalid or contains
   *     unsupported compose features
   */
  private static void failOnUnsupportedValueSetContent(VsExpandParameter parameters) {
    // Ensure an embedded ValueSet is present before deep validation.
    ValueSet valueSet = parameters.getValueSetParam();
    if (valueSet != null && !valueSet.isEmpty()) {

      if (!valueSet.hasCompose()) {
        throw new InvalidRequestException(
            "No 'compose' element is specified in the ValueSet: Nothing to expand ...");
      }

      if (valueSet.getCompose().hasLockedDateElement()) {
        throw new InvalidRequestException(
            "ValueSets with 'locked-date' elements are currently not supported for expansion");
      }

      ValueSet.ValueSetComposeComponent compose = valueSet.getCompose();

      for (var include : compose.getInclude()) {
        if (!include.hasSystem() && !include.hasValueSet()) {
          throw new InvalidRequestException(
              "Neither 'system' nor 'valueSet' is specified in the ValueSet 'include'");
        }
      }
    }
  }

  /**
   * Validates that the optional date parameter is not set to a future point in time.
   *
   * @param parameters the expand parameters to validate
   * @throws IllegalArgumentException if the date parameter is set to a future date
   */
  private static void validateDateNotInFuture(VsExpandParameter parameters)
      throws IllegalArgumentException {
    if (parameters.getDateParam() == null || !parameters.getDateParam().hasValue()) {
      return;
    }

    if (parameters.getDateParam().getValue().after(new Date())) {
      throw new IllegalArgumentException(
          "Parameter " + VsExpandParameter.DATE_PARAM + " must not be in the future.");
    }
  }

  /**
   * Ensures that {@code valueSetVersion} is only provided together with {@code url}.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if {@code valueSetVersion} is present without {@code url}
   */
  private static void failOnValueSetVersionWithoutUrl(VsExpandParameter parameters)
      throws InvalidRequestException {
    boolean hasValueSetVersion =
        parameters.getValueSetVersionParam() != null
            && !parameters.getValueSetVersionParam().isEmpty();
    boolean hasUrl = parameters.getUrlParam() != null && !parameters.getUrlParam().isEmpty();

    if (hasValueSetVersion && !hasUrl) {
      throw new InvalidRequestException(
          "The '"
              + VsExpandParameter.VALUESET_VERSION_PARAM
              + "' parameter requires the '"
              + VsExpandParameter.URL_PARAM
              + "' parameter and must not be used with an inline '"
              + VsExpandParameter.VALUESET_PARAM
              + "'.");
    }
  }

  /**
   * Ensures that a {@code url}-based request carries an explicit {@code valueSetVersion}.
   *
   * <p>ZTS requires an explicit ValueSet version when resolving by canonical URL.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if {@code url} is present without {@code valueSetVersion}
   */
  private static void failOnUrlWithoutValueSetVersion(VsExpandParameter parameters)
      throws InvalidRequestException {
    boolean hasUrl = parameters.getUrlParam() != null && !parameters.getUrlParam().isEmpty();
    boolean hasValueSetVersion =
        parameters.getValueSetVersionParam() != null
            && !parameters.getValueSetVersionParam().isEmpty();

    if (hasUrl && !hasValueSetVersion) {
      throw new InvalidRequestException(
          "The '"
              + VsExpandParameter.URL_PARAM
              + "' parameter requires an explicit '"
              + VsExpandParameter.VALUESET_VERSION_PARAM
              + "' parameter.");
    }
  }

  /**
   * Ensures that a single include or exclude element does not mix explicit concepts and filters.
   *
   * <p>FHIR does not allow {@code concept} and {@code filter} in the same {@code compose.include}
   * or {@code compose.exclude} element.
   *
   * @param parameters the operation parameters to validate
   * @throws InvalidRequestException if a compose part contains both concepts and filters
   */
  private static void failOnConceptAndFilterInSameComposePart(VsExpandParameter parameters)
      throws InvalidRequestException {
    ValueSet valueSet = parameters.getValueSetParam();
    if (valueSet == null || valueSet.isEmpty() || !valueSet.hasCompose()) {
      return;
    }

    ValueSet.ValueSetComposeComponent compose = valueSet.getCompose();
    for (ValueSet.ConceptSetComponent include : compose.getInclude()) {
      failOnConceptAndFilterInComponent(include, "include");
    }
    for (ValueSet.ConceptSetComponent exclude : compose.getExclude()) {
      failOnConceptAndFilterInComponent(exclude, "exclude");
    }
  }

  /**
   * Validates a single include/exclude component for the concept+filter constraint.
   *
   * @param component the include or exclude component
   * @param partName the name used in the error message ({@code include} or {@code exclude})
   * @throws InvalidRequestException if both concept and filter are present
   */
  private static void failOnConceptAndFilterInComponent(
      ValueSet.ConceptSetComponent component, String partName) throws InvalidRequestException {
    if (component.hasConcept() && component.hasFilter()) {
      throw new InvalidRequestException(
          "The ValueSet '"
              + partName
              + "' element must not contain both 'concept' and 'filter' definitions.");
    }
  }
}
