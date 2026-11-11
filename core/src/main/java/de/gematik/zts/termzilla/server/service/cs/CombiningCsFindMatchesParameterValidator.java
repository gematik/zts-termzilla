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

import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.common.validator.BaseCodeSystemFindMatchesParameterValidator;

/**
 * Parameter validator for the {@code CodeSystem/$find-matches} operation at the combining service
 * level.
 *
 * <p>Does not add any validation beyond the base class. All standard checks (system, exact,
 * property structure) are inherited from {@link BaseCodeSystemFindMatchesParameterValidator}.
 */
public class CombiningCsFindMatchesParameterValidator
    extends BaseCodeSystemFindMatchesParameterValidator {

  /**
   * Convenience method that creates a new validator instance and validates the given parameters.
   *
   * @param parameters the {@code $find-matches} parameters to validate
   * @throws ca.uhn.fhir.rest.server.exceptions.InvalidRequestException if any parameter is missing
   *     or malformed
   */
  public static void validate(CsFindMatchesParameter parameters) {
    new CombiningCsFindMatchesParameterValidator().validateParameters(parameters);
  }
}
