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

import static ca.uhn.fhir.rest.param.UriParamQualifierEnum.ABOVE;
import static ca.uhn.fhir.rest.param.UriParamQualifierEnum.BELOW;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.server.service.CombiningGenericSearchParameterValidator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CombiningVsSearchParameterValidator extends CombiningGenericSearchParameterValidator {

  private CombiningVsSearchParameterValidator() {}

  public static void validateParameters(VsSearchParameter parameters)
      throws IllegalArgumentException {
    failOnUnsupportedParameters(parameters);
    validateVersion(parameters);
    validateContent(parameters);
    validateReference(parameters);
  }

  /**
   * Rejects currently unsupported search parameters.
   *
   * @param parameters the search parameters to validate
   * @throws InvalidRequestException if an unsupported parameter is provided
   */
  private static void failOnUnsupportedParameters(VsSearchParameter parameters)
      throws InvalidRequestException {
    CombiningGenericSearchParameterValidator.failOnUnsupportedParameters(
        parameters.getExpansion().isPresent(), VsSearchParameter.SP_EXPANSION);
  }

  /**
   * Überprüft, ob die Version bereits in einem anderen Parameter ('url') gesetzt ist. Wenn dies der
   * Fall ist, wird eine Exception geworfen.
   *
   * @param parameters Die Parameter, die überprüft werden sollen
   * @throws IllegalArgumentException Wenn die Version bereits in einem anderen Parameter gesetzt
   *     ist
   */
  private static void validateVersion(VsSearchParameter parameters)
      throws IllegalArgumentException {
    CombiningGenericSearchParameterValidator.validateVersionAgainstUrl(parameters);
  }

  /**
   * Checks whether the 'reference' parameter is correctly set. The 'reference' parameter does not
   * support qualifiers 'above' and 'below'.
   *
   * @param parameters The parameters to be checked
   * @throws InvalidRequestException if the 'reference' parameter contains unsupported qualifiers
   */
  private static void validateReference(VsSearchParameter parameters)
      throws InvalidRequestException {
    var referenceOpt = parameters.getReference();
    if (referenceOpt.isPresent()) {
      var referenceParam = referenceOpt.get();
      var qualifier = referenceParam.getQualifier();
      if (qualifier != null && (qualifier.equals(ABOVE) || qualifier.equals(BELOW))) {
        throw new InvalidRequestException(
            "Qualifier 'above' and 'below' are not supported for 'reference'-Search-Parameter");
      }
    }
  }
}
