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

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;

/**
 * Shared validator for unsupported {@code CodeSystem/$validate-code} parameters.
 *
 * <p>Rejects parameter combinations that are syntactically valid in FHIR but currently not
 * implemented by terminology-specific modules.
 */
public final class UnsupportedCsValidateCodeParameterValidator {

  /** Private constructor to prevent instantiation of this utility class. */
  private UnsupportedCsValidateCodeParameterValidator() {}

  /**
   * Validates unsupported parameters for {@code CodeSystem/$validate-code}.
   *
   * <p>Rejects {@code date}, {@code coding}, and {@code codeableConcept}. For the {@code abstract}
   * parameter, only {@code false} is rejected and {@code true} is accepted.
   *
   * @param parameters the validate-code parameters to check
   * @throws NotImplementedException if an unsupported parameter or value is present
   */
  public static void validateUnsupportedParameters(CsValidateCodeParameter parameters)
      throws NotImplementedException {

    failOnDateParam(parameters.getDateParam());
    failOnCodingParam(parameters.getCodingParam());
    failOnCodeableConceptParam(parameters.getCodeableConceptParam());
    failOnAbstractParam(parameters.getAbstractParam());
  }

  private static void failOnDateParam(DateTimeType dateParam) throws NotImplementedException {
    if (dateParam != null) {
      throw new NotImplementedException(
          "Parameter '" + CsValidateCodeParameter.DATE_PARAM + "' currently not supported.");
    }
  }

  private static void failOnAbstractParam(BooleanType abstractParam)
      throws NotImplementedException {
    if (abstractParam != null && !abstractParam.booleanValue()) {
      throw new NotImplementedException(
          "Parameter '" + CsValidateCodeParameter.ABSTRACT_PARAM + "' currently not supported.");
    }
  }

  private static void failOnCodingParam(Coding coding) throws NotImplementedException {
    if (coding != null) {
      throw new NotImplementedException(
          "Parameter '" + CsValidateCodeParameter.CODING_PARAM + "' currently not supported.");
    }
  }

  private static void failOnCodeableConceptParam(CodeableConcept cc)
      throws NotImplementedException {
    if (cc != null) {
      throw new NotImplementedException(
          "Parameter '"
              + CsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' currently not supported.");
    }
  }
}
