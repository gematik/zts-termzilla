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
package de.gematik.zts.termzilla.server.service.cm;

import static ca.uhn.fhir.rest.api.Constants.PARAMQUALIFIER_MISSING;

import ca.uhn.fhir.rest.param.BaseParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.server.service.CombiningGenericSearchParameterValidator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CombiningCmSearchParameterValidator extends CombiningGenericSearchParameterValidator {

  private CombiningCmSearchParameterValidator() {}

  public static void validateParameters(CmSearchParameter parameters)
      throws IllegalArgumentException {
    failOnUnsupportedParameters(parameters);
    validateVersion(parameters);
    validateContent(parameters);
    validateCanonicalAndUriExclusivity(parameters);

    validateBaseParam(
        parameters.getSourceCanonical().orElse(null), CmSearchParameter.SP_SOURCE_CANONICAL);
    validateBaseParam(parameters.getSourceUri().orElse(null), CmSearchParameter.SP_SOURCE_URI);

    validateBaseParam(
        parameters.getSourceSystem().orElse(null), CmSearchParameter.SP_SOURCE_SYSTEM);

    validateBaseParam(
        parameters.getTargetCanonical().orElse(null), CmSearchParameter.SP_TARGET_CANONICAL);
    validateBaseParam(parameters.getTargetUri().orElse(null), CmSearchParameter.SP_TARGET_URI);

    validateBaseParam(
        parameters.getTargetSystem().orElse(null), CmSearchParameter.SP_TARGET_SYSTEM);
  }

  /**
   * Rejects parameters that are currently not supported by this validator.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if unsupported parameters are provided
   */
  private static void failOnUnsupportedParameters(CmSearchParameter parameters)
      throws InvalidRequestException {
    CombiningGenericSearchParameterValidator.failOnUnsupportedParameters(
        parameters.getDependsOn().isPresent()
            || parameters.getProduct().isPresent()
            || parameters.getSourceCode().isPresent()
            || parameters.getTargetCode().isPresent(),
        CmSearchParameter.SP_DEPENDS_ON,
        CmSearchParameter.SP_PRODUCT,
        CmSearchParameter.SP_SOURCE_CODE,
        CmSearchParameter.SP_TARGET_CODE);
  }

  /**
   * Überprüft, ob die Version bereits in einem anderen Parameter ('url') gesetzt ist. Wenn dies der
   * Fall ist, wird eine Exception geworfen.
   *
   * @param parameters Die Parameter, die überprüft werden sollen
   * @throws IllegalArgumentException Wenn die Version bereits in einem anderen Parameter gesetzt
   *     ist
   */
  private static void validateVersion(CmSearchParameter parameters)
      throws IllegalArgumentException {
    CombiningGenericSearchParameterValidator.validateVersionAgainstUrl(parameters);
  }

  /**
   * Validates that content parameter does not contain any modifiers.
   *
   * @param parameters the search parameters to validate
   * @throws InvalidRequestException if content parameter contains modifiers
   */
  private static void validateContent(CmSearchParameter parameters) throws InvalidRequestException {
    CombiningGenericSearchParameterValidator.validateContent(parameters);
  }

  /**
   * Checks whether the provided parameter is correctly set. Currently, only modifier 'missing' is
   * supported for BaseParam (including ReferenceParam and UriParam). If the parameter is not
   * correctly set, an InvalidRequestException is thrown.
   *
   * @param baseParam The parameter to be checked (nullable)
   * @param paramName The name of the parameter (for error messaging)
   * @throws InvalidRequestException if the parameter contains unsupported qualifiers
   */
  private static void validateBaseParam(BaseParam baseParam, String paramName)
      throws InvalidRequestException {

    if (baseParam != null) {
      String qualifier = baseParam.getQueryParameterQualifier();
      if (qualifier != null && !qualifier.equals(PARAMQUALIFIER_MISSING)) {
        throw new InvalidRequestException(
            String.format(
                "Only Qualifier 'missing' is currently supported for '%s'-Search-Parameter",
                paramName));
      }
    }
  }

  /**
   * Validates that canonical and URI variants are not used at the same time for source and target.
   *
   * @param parameters the parameters to validate
   * @throws IllegalArgumentException if both variants are set for source or target
   */
  private static void validateCanonicalAndUriExclusivity(CmSearchParameter parameters)
      throws IllegalArgumentException {
    if (parameters.getSourceCanonical().isPresent() && parameters.getSourceUri().isPresent()) {
      throw new IllegalArgumentException(
          "Either use '"
              + CmSearchParameter.SP_SOURCE_CANONICAL
              + "' or '"
              + CmSearchParameter.SP_SOURCE_URI
              + "' parameter, but not both.");
    }

    if (parameters.getTargetCanonical().isPresent() && parameters.getTargetUri().isPresent()) {
      throw new IllegalArgumentException(
          "Either use '"
              + CmSearchParameter.SP_TARGET_CANONICAL
              + "' or '"
              + CmSearchParameter.SP_TARGET_URI
              + "' parameter, but not both.");
    }
  }
}
