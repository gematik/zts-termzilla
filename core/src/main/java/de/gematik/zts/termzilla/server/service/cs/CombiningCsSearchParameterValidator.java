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

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.server.service.CombiningGenericSearchParameterValidator;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.codesystems.CodesystemContentMode;

/**
 * Validator for combining code system search parameters.
 *
 * <p>This validator ensures that search parameters for code system operations are correctly
 * combined and do not contain conflicting or invalid combinations.
 */
@Getter
@Setter
public class CombiningCsSearchParameterValidator extends CombiningGenericSearchParameterValidator {

  private CombiningCsSearchParameterValidator() {}

  /**
   * Validates all search parameters for code system operations.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if parameters contain invalid combinations
   * @throws InvalidRequestException if content parameter contains invalid modifiers
   */
  public static void validateParameters(CsSearchParameter parameters)
      throws IllegalArgumentException {
    failOnUnsupportedParameters(parameters);
    validateSystemOrUrl(parameters);
    validateVersion(parameters);
    validateContent(parameters);
    validateContentMode(parameters);
  }

  /**
   * Rejects currently unsupported search parameters.
   *
   * @param parameters the search parameters to validate
   * @throws InvalidRequestException if an unsupported parameter is provided
   */
  private static void failOnUnsupportedParameters(CsSearchParameter parameters)
      throws InvalidRequestException {
    CombiningGenericSearchParameterValidator.failOnUnsupportedParameters(
        parameters.getLanguage().isPresent(), CsSearchParameter.SP_LANGUAGE);
  }

  /**
   * Validates that either system or URL is set, but not both simultaneously.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if both system and URL are set
   */
  private static void validateSystemOrUrl(CsSearchParameter parameters)
      throws IllegalArgumentException {

    // We don't want both parameters to be set. This can only lead to confusing situations
    if (parameters.getSystem().isPresent() && parameters.getUrl().isPresent()) {
      throw new IllegalArgumentException(
          "Either use '" + CodeSystem.SP_URL + "' or '" + CodeSystem.SP_SYSTEM + "' Parameter.");
    }
  }

  /**
   * Validates content mode parameter and its compatibility with other parameters.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if content mode is incompatible with other parameters
   */
  private static void validateContentMode(CsSearchParameter parameters)
      throws IllegalArgumentException {
    var contentMode = parameters.getContent();

    if (contentMode.isPresent()) {
      String contentModeValue = contentMode.get().getValue();

      if (contentModeValue.equals(CodesystemContentMode.NOTPRESENT.toCode())) {
        validateNotPresentContentMode(parameters);
      } else if (!contentModeValue.equals(CodesystemContentMode.SUPPLEMENT.toCode())) {
        validateNonSupplementContentMode(parameters);
      }
    }
  }

  /**
   * Validates parameters when content mode is 'not-present'. Language and code parameters must not
   * be used with 'not-present' content mode.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if incompatible parameters are present
   */
  private static void validateNotPresentContentMode(CsSearchParameter parameters)
      throws IllegalArgumentException {

    if (parameters.getLanguage().isPresent()) {
      throw new IllegalArgumentException(
          "Language parameter must not be used when content mode 'not-present' is requested.");
    }

    if (parameters.getCode().isPresent()) {
      throw new IllegalArgumentException(
          "Code parameter must not be used when content mode 'not-present' is requested.");
    }
  }

  /**
   * Validates parameters when content mode is not 'supplement'. Supplements parameter must only be
   * used with 'supplement' content mode.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if supplements parameter is present with non-supplement
   *     content mode
   */
  private static void validateNonSupplementContentMode(CsSearchParameter parameters)
      throws IllegalArgumentException {

    if (parameters.getSupplements().isPresent()) {
      throw new IllegalArgumentException(
          "Supplements parameter must not be used when content mode is not 'supplement'.");
    }
  }

  /**
   * Validates that version is not set in both URL/System and version parameter simultaneously.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if version is set in both parameters
   */
  private static void validateVersion(CsSearchParameter parameters)
      throws IllegalArgumentException {
    CombiningGenericSearchParameterValidator.validateVersionAgainstUrl(parameters);
    CombiningGenericSearchParameterValidator.validateVersionNotDuplicated(
        parameters, parameters.getSystem().orElse(null), CsSearchParameter.SP_SYSTEM);
  }

  /**
   * Validates that content parameter does not contain any modifiers.
   *
   * @param parameters the search parameters to validate
   * @throws InvalidRequestException if content parameter contains modifiers
   */
  private static void validateContent(CsSearchParameter parameters) throws InvalidRequestException {
    CombiningGenericSearchParameterValidator.validateContent(parameters);
  }
}
