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
package de.gematik.zts.termzilla.terminologies.loinc.provider.cs.lookup;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.DateTimeType;

/**
 * Validates request parameters for the LOINC {@code $lookup} operation.
 *
 * <p>Performs LOINC-specific parameter validation before the lookup is executed. Currently rejects
 * parameters that are not yet supported by the LOINC implementation.
 */
@Getter
@Setter
public class LoincCsLookupParameterValidator {

  /** Private constructor to prevent instantiation of this utility class. */
  private LoincCsLookupParameterValidator() {}

  /**
   * Validates the given {@link CsLookupParameter} for unsupported parameter combinations.
   *
   * <p>Currently rejects the {@code date} parameter with a {@link NotImplementedException}.
   *
   * @param parameters the lookup parameters to check
   * @throws NotImplementedException if any unsupported parameter is present
   */
  public static void validateParameters(CsLookupParameter parameters)
      throws NotImplementedException {
    failOnDateParam(parameters.getDateParam());
  }

  /**
   * Throws a {@link NotImplementedException} if the {@code date} parameter is set.
   *
   * @param dateParam the date parameter value, or {@code null} if not provided
   * @throws NotImplementedException if {@code dateParam} is not {@code null}
   */
  private static void failOnDateParam(DateTimeType dateParam) throws NotImplementedException {
    if (dateParam != null) {
      throw new NotImplementedException(
          "Parameter '" + CsLookupParameter.DATE_PARAM + "' currently not supported.");
    }
  }
}
