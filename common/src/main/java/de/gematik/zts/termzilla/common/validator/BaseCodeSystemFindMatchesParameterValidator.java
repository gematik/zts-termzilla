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
package de.gematik.zts.termzilla.common.validator;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;

/**
 * Base validator for the FHIR {@code CodeSystem/$find-matches} operation parameters.
 *
 * <p>Validates that the required parameters ({@code system}, {@code exact}, and at least one {@code
 * property}) are present, and that each {@code property} parameter has a valid structure containing
 * a single {@code code} part of type {@link CodeType} and an optional {@code value} part of a
 * supported FHIR type.
 *
 * <p>Subclasses can override individual validation methods to add terminology-specific constraints,
 * such as limiting the number of allowed properties or rejecting sub-properties.
 *
 * @see CsFindMatchesParameter
 */
public class BaseCodeSystemFindMatchesParameterValidator {

  /**
   * Validates all {@code $find-matches} parameters by delegating to the individual validation
   * methods. Subclasses may override this method to add or reorder validation steps.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if any required parameter is missing or malformed
   */
  public void validateParameters(CsFindMatchesParameter parameters) {
    validateSystemExists(parameters);
    validateExactExists(parameters);
    validateProperties(parameters);
  }

  /**
   * Validates that the {@code system} parameter is present and has a value.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if the {@code system} parameter is missing or empty
   */
  protected void validateSystemExists(CsFindMatchesParameter parameters) {
    if (parameters.getSystemParam() == null || !parameters.getSystemParam().hasValue()) {
      throw new InvalidRequestException(
          "A 'system' parameter is required when using $find-matches");
    }
  }

  /**
   * Validates that the {@code exact} parameter is present and has a value.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if the {@code exact} parameter is missing or empty
   */
  protected void validateExactExists(CsFindMatchesParameter parameters) {
    if (parameters.getExactParam() == null || !parameters.getExactParam().hasValue()) {
      throw new InvalidRequestException(
          "At least one 'exact' parameter is required when using $find-matches");
    }
  }

  /**
   * Validates all {@code property} parameters. Ensures that at least one property exists and that
   * each individual property has a valid structure.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if no property is present or any property is malformed
   */
  protected void validateProperties(CsFindMatchesParameter parameters) {
    validateAtLeastOnePropertyExists(parameters);

    if (parameters.getPropertyParamList() != null) {
      parameters.getPropertyParamList().forEach(this::validateProperty);
    }
  }

  /**
   * Validates a single {@code property} parameter by checking that it contains the required {@code
   * part} elements with valid {@code code} and {@code value} entries.
   *
   * @param property the property parameter component to validate
   * @throws InvalidRequestException if the property structure is invalid
   */
  protected void validateProperty(Parameters.ParametersParameterComponent property) {
    validatePartExists(property);
    validateCode(property);
    validateValue(property);
  }

  /**
   * Validates that at least one {@code property} parameter is present and the list is not empty.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if no property parameter exists
   */
  protected void validateAtLeastOnePropertyExists(CsFindMatchesParameter parameters) {
    if (parameters.getPropertyParamList() == null || parameters.getPropertyParamList().isEmpty()) {
      throw new InvalidRequestException(
          "At least one 'property' parameter is required when using $find-matches");
    }
  }

  /**
   * Validates that the given property parameter contains at least one {@code part} element.
   *
   * @param propertyParam the property parameter component to check
   * @throws InvalidRequestException if no parts are found
   */
  protected void validatePartExists(Parameters.ParametersParameterComponent propertyParam) {
    if (propertyParam.getPart().isEmpty()) {
      throw new InvalidRequestException("Parameter 'property' is invalid: No 'part' found");
    }
  }

  /**
   * Validates that the property parameter contains exactly one {@code code} part of type {@link
   * CodeType}.
   *
   * @param propertyParam the property parameter component to check
   * @throws InvalidRequestException if the {@code code} part is missing, duplicated, or not of type
   *     {@link CodeType}
   */
  protected void validateCode(Parameters.ParametersParameterComponent propertyParam) {
    if (propertyParam.getPart().stream()
            .filter(part -> part.getName().equals("code"))
            .toList()
            .size()
        != 1) {
      throw new InvalidRequestException(
          "Parameter 'property' is invalid: A single 'name=code' Part is required");
    }

    if (propertyParam.getPart().stream()
            .filter(part -> part.getName().equals("code"))
            .filter(part -> part.getValue() instanceof CodeType)
            .toList()
            .size()
        != 1) {
      throw new InvalidRequestException(
          "Parameter 'property' is invalid: The 'code' Part must be of type CodeType");
    }
  }

  /**
   * Validates the optional {@code value} part of a property parameter. Ensures that at most one
   * {@code value} part exists and that its type is one of the allowed FHIR types: {@code code},
   * {@link Coding}, {@code string}, {@code boolean}, {@code integer}, or {@code dateTime}.
   *
   * @param propertyParam the property parameter component to check
   * @throws InvalidRequestException if the {@code value} part has invalid cardinality or an
   *     unsupported type
   */
  protected void validateValue(Parameters.ParametersParameterComponent propertyParam) {
    if (propertyParam.getPart().stream()
            .filter(part -> part.getName().equals("value"))
            .toList()
            .size()
        > 1) {
      throw new InvalidRequestException(
          "Parameter 'property' is invalid: Allowed cardinality for value in part is '0..1'");
    }

    if (!propertyParam.getPart().stream()
        .filter(part -> part.getName().equals("value"))
        .filter(
            part ->
                !(part.getValue() instanceof CodeType
                    || part.getValue() instanceof Coding
                    || part.getValue() instanceof StringType
                    || part.getValue() instanceof BooleanType
                    || part.getValue() instanceof IntegerType
                    || part.getValue() instanceof DateTimeType))
        .toList()
        .isEmpty()) {
      throw new InvalidRequestException(
          "Parameter 'property' is invalid: The 'value' Part must be of type code, Coding, string, boolean, integer or dateTime");
    }
  }

  /**
   * Validates that the property parameter does not contain nested sub-properties.
   *
   * @param propertyParam the property parameter component to check
   * @throws InvalidRequestException if sub-properties are found
   */
  protected void validateNoSubProperty(Parameters.ParametersParameterComponent propertyParam) {
    // TODO: Implement this method
  }
}
