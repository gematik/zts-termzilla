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
package de.gematik.zts.termzilla.api.parameters;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;

@Getter
@Setter
public class ValidateCodeResult {

  // R4: True if the concept details supplied are valid
  private boolean result = false;
  // R4: Error details, if result = false. If this is provided when result = true, the message
  // carries hints and warnings
  private String message;
  // R4: A valid display for the concept if the system wishes to display this to a user
  private String display;
  // R5: The code that was validated
  private String code;
  // R5: The system for the code that was validated
  private String system;
  // R5: The version of the system of the code that was validated
  private String version;
  // R5: A codeableConcept containing codings for all the validated codes
  private CodeableConcept codeableConcept;
  // R5: List of itemised issues with paths constrained to simple FHIRPath. Examples are
  // CodeableConcept, CodeableConcept.coding[0], CodeableConcept.coding[1].display, or
  // Coding.display
  private OperationOutcome issues;

  public IBaseParameters toParameters(FhirContext context) {

    // Parameters Ressource anlegen
    IBaseParameters returnParameters = ParametersUtil.newInstance(context);

    // result zum Ergebnis hinzufügen
    ParametersUtil.addParameterToParametersBoolean(
        context, returnParameters, "result", this.isResult());

    // message zum Ergebnis hinzufügen
    if (StringUtils.isNotBlank(this.getMessage())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, "message", this.getMessage());
    }

    // display zum Ergebnis hinzufügen
    if (StringUtils.isNotBlank(this.getDisplay())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, "display", this.getDisplay());
    }

    // code zum Ergebnis hinzufügen
    if (StringUtils.isNotBlank(this.getCode())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, "code", this.getCode());
    }

    // system zum Ergebnis hinzufügen
    if (StringUtils.isNotBlank(this.getSystem())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, "system", this.getSystem());
    }

    // version zum Ergebnis hinzufügen
    if (StringUtils.isNotBlank(this.getVersion())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, "version", this.getVersion());
    }

    // codeableConcept zum Ergebnis hinzufügen
    if (this.getCodeableConcept() != null) {
      ParametersUtil.addParameterToParameters(
          context, returnParameters, "codeableConcept", this.getCodeableConcept());
    }

    // issues zum Ergebnis hinzufügen
    if (this.getIssues() != null) {
      ParametersUtil.addParameterToParameters(
          context, returnParameters, "issues", this.getIssues());
    }

    return returnParameters;
  }
}
