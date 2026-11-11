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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.validatecode;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.common.validator.UnsupportedCsValidateCodeParameterValidator;

/**
 * Utility class for validating input parameters of the FHIR package {@code
 * CodeSystem/$validate-code} operation.
 *
 * <p>Delegates unsupported-parameter checks to {@link UnsupportedCsValidateCodeParameterValidator}.
 * Unsupported parameters cause a {@link NotImplementedException} to be thrown early, before any
 * terminology lookup is attempted.
 */
public class FhirTermsCsValidateCodeParameterValidator {

  /** Private constructor to prevent instantiation of this utility class. */
  private FhirTermsCsValidateCodeParameterValidator() {}

  /**
   * Validates the given {@link CsValidateCodeParameter} for unsupported parameter combinations.
   *
   * <p>Currently rejects the {@code date}, {@code coding}, and {@code codeableConcept} parameters.
   * For the {@code abstract} parameter, only the value {@code false} is rejected; {@code true} is
   * accepted.
   *
   * @param parameters the validate-code parameters to check
   * @throws NotImplementedException if any unsupported parameter or parameter value is present
   */
  public static void validateParameters(CsValidateCodeParameter parameters)
      throws NotImplementedException {
    UnsupportedCsValidateCodeParameterValidator.validateUnsupportedParameters(parameters);
  }
}
