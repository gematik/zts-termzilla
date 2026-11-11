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
package de.gematik.zts.termzilla.server.service.cs;

import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Validator for CodeSystem lookup operation parameters. Ensures that either system+code or coding
 * parameter combination is provided according to FHIR specification.
 */
@Getter
@Setter
public class CombiningCsLookupParameterValidator extends CommonParameterValidator {

  private CombiningCsLookupParameterValidator() {}

  /**
   * Validates that the required parameter combinations for a CodeSystem lookup operation are
   * correctly provided. According to FHIR specification, a client must provide either system+code
   * parameters or a coding parameter.
   *
   * @param parameters the lookup parameters to validate
   * @throws IllegalArgumentException if parameter combination is invalid or required values are
   *     missing
   */
  public static void validateParameters(CsLookupParameter parameters)
      throws IllegalArgumentException {
    validateCodeOrCoding(parameters);
    validateDateNotInFuture(parameters);
  }

  /**
   * Validates that exactly one of the two allowed parameter combinations is used: either
   * system+code parameters or coding parameter with required values.
   *
   * @param parameters the lookup parameters to validate
   * @throws IllegalArgumentException if both combinations are used, neither is used, or required
   *     values are missing
   */
  private static void validateCodeOrCoding(CsLookupParameter parameters)
      throws IllegalArgumentException {

    // According to specification: When invoking this operation, a client SHALL provide both a
    // system and
    // a code, either using the system+code parameters, or in the coding parameter. Other parameters
    // are optional.

    boolean useSystemAndCodeParams = hasSystemAndCodeParams(parameters);
    boolean useCodingParam = hasCodingParam(parameters);

    // Validate that exactly one parameter combination is used
    validateExclusiveParameterUsage(useSystemAndCodeParams, useCodingParam);

    // Validate the parameter combination that is being used
    if (useSystemAndCodeParams) {
      validateSystemAndCodeParams(parameters);
    } else {
      validateCodingParam(parameters);
    }
  }

  /**
   * Checks whether both system and code parameters are provided.
   *
   * @param parameters the lookup parameters to check
   * @return {@code true} if both system and code parameters are present
   */
  private static boolean hasSystemAndCodeParams(CsLookupParameter parameters) {
    return parameters.getCodeParam() != null
        && parameters.getCodeParam().hasValue()
        && parameters.getSystemParam() != null
        && parameters.getSystemParam().hasValue();
  }

  /**
   * Checks whether the coding parameter is provided.
   *
   * @param parameters the lookup parameters to check
   * @return {@code true} if coding parameter is present
   */
  private static boolean hasCodingParam(CsLookupParameter parameters) {
    return parameters.getCodingParam() != null;
  }

  /**
   * Validates that exactly one parameter combination is used (XOR validation). Either system+code
   * parameters or coding parameter must be provided, but not both or neither.
   *
   * @param useSystemAndCodeParams whether system and code parameters are present
   * @param useCodingParam whether coding parameter is present
   * @throws IllegalArgumentException if both combinations are used or neither is used
   */
  private static void validateExclusiveParameterUsage(
      boolean useSystemAndCodeParams, boolean useCodingParam) {

    // If both parameter combinations are set or none, an error occurs
    if (useSystemAndCodeParams == useCodingParam) {
      throw new IllegalArgumentException(
          "Either use '"
              + CsLookupParameter.CODE_PARAM
              + "'+'"
              + CsLookupParameter.SYSTEM_PARAM
              + "' or '"
              + CsLookupParameter.CODING_PARAM
              + "' Parameter.");
    }
  }

  /**
   * Validates that both system and code parameters contain non-empty values.
   *
   * @param parameters the lookup parameters to validate
   * @throws IllegalArgumentException if either system or code parameter is missing or empty
   */
  private static void validateSystemAndCodeParams(CsLookupParameter parameters) {
    // When using the system+code parameter combination, both parameters must be set
    if (!parameters.getCodeParam().hasValue()) {
      throw new IllegalArgumentException(
          "Parameter " + CsLookupParameter.CODE_PARAM + " must be set.");
    }
    if (!parameters.getSystemParam().hasValue()) {
      throw new IllegalArgumentException(
          "Parameter " + CsLookupParameter.SYSTEM_PARAM + " must be set");
    }
  }

  /**
   * Validates that the coding parameter contains both code and system values.
   *
   * @param parameters the lookup parameters to validate
   * @throws IllegalArgumentException if code or system is missing from the coding parameter
   */
  private static void validateCodingParam(CsLookupParameter parameters) {
    // When using the coding parameter combination, code and system values must be set in the Coding
    if (isNullOrEmpty(parameters.getCodingParam().getCode())) {
      throw new IllegalArgumentException(
          "Parameter " + CsLookupParameter.CODING_PARAM + " must have a code.");
    }
    if (isNullOrEmpty(parameters.getCodingParam().getSystem())) {
      throw new IllegalArgumentException(
          "Parameter " + CsLookupParameter.CODING_PARAM + " must have a system.");
    }
  }

  /**
   * Checks if a string value is null or empty.
   *
   * @param value the string to check
   * @return {@code true} if the value is null or empty
   */
  private static boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  /**
   * Validates that the optional date parameter is not set to a future point in time.
   *
   * @param parameters the lookup parameters to validate
   * @throws IllegalArgumentException if the date parameter is set to a future date
   */
  private static void validateDateNotInFuture(CsLookupParameter parameters)
      throws IllegalArgumentException {
    if (parameters.getDateParam() == null || !parameters.getDateParam().hasValue()) {
      return;
    }

    if (parameters.getDateParam().getValue().after(new Date())) {
      throw new IllegalArgumentException(
          "Parameter " + CsLookupParameter.DATE_PARAM + " must not be in the future.");
    }
  }
}
