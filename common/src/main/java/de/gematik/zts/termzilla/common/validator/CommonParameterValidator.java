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

import ca.uhn.fhir.rest.param.BaseParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CommonParameterValidator {

  protected CommonParameterValidator() {}

  public static void processMissingParamPart(BaseParam param, String paramName)
      throws InvalidRequestException {
    if (param != null && param.getMissing() != null && param.getMissing().booleanValue()) {
      throw new InvalidRequestException("Missing modifier MUST NOT be set for '" + paramName + "'");
    }
  }

  public static void processParamQualifier(BaseParam param, String paramName)
      throws InvalidRequestException {
    if (param != null && param.getQueryParameterQualifier() != null) {
      throw new InvalidRequestException(
          "Parameter qualifier are NOT supported for '" + paramName + "'");
    }
  }
}
