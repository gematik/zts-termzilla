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
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CombiningVsValidateCodeParameterValidator extends CommonParameterValidator {

  private CombiningVsValidateCodeParameterValidator() {}

  public static void validateParameters(VsValidateCodeParameter parameters)
      throws InvalidRequestException {
    failOnUnsupportedParameters(parameters);
    failOnMissingUrlOrValueSetParameter(parameters);
    failOnCodeXorCodableConceptXorCodingParameter(parameters);
    failOnSystemWithoutCodeParameter(parameters);
    failOnCodeWithoutSystemParameter(parameters);
  }

  // Bestimmte Parameter unterstützen wir bisher einfach nicht
  private static void failOnUnsupportedParameters(VsValidateCodeParameter parameters)
      throws InvalidRequestException {

    if (parameters.getContext().isPresent()
        || parameters.getAbstract().isPresent()
        || parameters.getDate().isPresent()) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.CONTEXT_PARAM
              + "', '"
              + VsValidateCodeParameter.ABSTRACT_PARAM
              + "' and '"
              + VsValidateCodeParameter.DATE_PARAM
              + "' parameters are not supported yet.");
    }
  }

  // Entweder eine url oder ein valueSet muss angegeben sein
  private static void failOnMissingUrlOrValueSetParameter(VsValidateCodeParameter parameters)
      throws InvalidRequestException {

    if (parameters.getUrl().isEmpty() && parameters.getValueSet().isEmpty()
        || parameters.getUrl().isPresent() && parameters.getValueSet().isPresent()) {
      throw new InvalidRequestException(
          "Either the '"
              + VsValidateCodeParameter.URL_PARAM
              + "' or the '"
              + VsValidateCodeParameter.VALUESET_PARAM
              + "' parameter must be provided.");
    }
  }

  private static void failOnCodeXorCodableConceptXorCodingParameter(
      VsValidateCodeParameter parameters) throws InvalidRequestException {
    if ((parameters.getSystem().isPresent()
            || parameters.getSystemVersion().isPresent()
            || parameters.getCode().isPresent()
            || parameters.getDisplay().isPresent())
        && parameters.getCoding().isPresent()) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.CODE_PARAM
              + "' and the '"
              + VsValidateCodeParameter.CODING_PARAM
              + "' parameter must not be provided at the same time.");
    }
    if ((parameters.getSystem().isPresent()
            || parameters.getSystemVersion().isPresent()
            || parameters.getCode().isPresent()
            || parameters.getDisplay().isPresent())
        && parameters.getCodeableConcept().isPresent()) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.CODE_PARAM
              + "' and the '"
              + VsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameter must not be provided at the same time.");
    }
    if (parameters.getCoding().isPresent() && parameters.getCodeableConcept().isPresent()) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.CODING_PARAM
              + "' and the '"
              + VsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameter must not be provided at the same time.");
    }
    if (parameters.getCode().isEmpty()
        && parameters.getCoding().isEmpty()
        && parameters.getCodeableConcept().isEmpty()) {
      throw new InvalidRequestException(
          "At least one of the '"
              + VsValidateCodeParameter.CODE_PARAM
              + "', '"
              + VsValidateCodeParameter.CODING_PARAM
              + "' or '"
              + VsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameters must be provided");
    }
  }

  private static void failOnSystemWithoutCodeParameter(VsValidateCodeParameter parameters)
      throws InvalidRequestException {
    if (parameters.getSystem().isPresent() && parameters.getCode().isEmpty()) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.CODE_PARAM
              + "' parameter must be provided if the '"
              + VsValidateCodeParameter.SYSTEM_PARAM
              + "' parameter is provided.");
    }
  }

  private static void failOnCodeWithoutSystemParameter(VsValidateCodeParameter parameters)
      throws InvalidRequestException {
    if (parameters.getSystem().isEmpty()
        && parameters.getCode().isPresent()
        && !parameters.getInferSystem().orElse(false)) {
      throw new InvalidRequestException(
          "The '"
              + VsValidateCodeParameter.SYSTEM_PARAM
              + "' parameter must be provided if the '"
              + VsValidateCodeParameter.CODE_PARAM
              + "' parameter is provided.");
    }
  }
}
