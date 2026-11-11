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
package de.gematik.zts.termzilla.terminologies.loinc.provider.vs.expand;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.ValueSet;

// TODO this class is currently not being used
@Getter
@Setter
public class LoincVsExpandParameterValidator extends CommonParameterValidator {

  private LoincVsExpandParameterValidator() {
    // private constructor to prevent instantiation
  }

  public static void validateParameters(VsExpandParameter parameters)
      throws InvalidRequestException {
    failOnUrlParam(parameters);
    failOnImproperValueSet(parameters);
  }

  // Aktuell unterstützen wir keine impliziten oder expliziten Loinc-ValueSets. Entsprechend können
  // wir immer eine Fehlermeldung zurückgeben.
  private static void failOnUrlParam(VsExpandParameter parameters) throws InvalidRequestException {

    if (parameters.getUrlParam() != null && !parameters.getUrlParam().isEmpty()) {
      throw new InvalidRequestException(
          "No ValueSet with url '" + parameters.getUrlParam().getValue() + "' found.");
    }
  }

  private static void failOnImproperValueSet(VsExpandParameter parameters)
      throws InvalidRequestException {

    ValueSet valueSet = parameters.getValueSetParam();

    if (valueSet != null && !valueSet.isEmpty()) {
      failOnValueSetMissingCompose(valueSet);
      failOnValueSetMultipleIncludes(valueSet);
      failOnValueSetNonLoincIncludes(valueSet);
      failOnValueSetHasExclude(valueSet);
    }
  }

  private static void failOnValueSetMissingCompose(ValueSet valueSet)
      throws InvalidRequestException {
    // Ohne ein compose-Element können wir das ValueSet nicht expandieren
    if (!valueSet.hasCompose()) {
      throw new InvalidRequestException(
          "ValueSet Expansion requires a compose-element in the resource");
    }
  }

  private static void failOnValueSetMultipleIncludes(ValueSet valueSet)
      throws InvalidRequestException {
    if (valueSet.hasCompose() && valueSet.getCompose().getInclude().size() != 1) {
      // Derzeit unterstützen wir genau ein include-Element
      throw new InvalidRequestException(
          "ValueSet Expansion currently supports only a single include-element in the compose-element");
    }
  }

  private static void failOnValueSetNonLoincIncludes(ValueSet valueSet)
      throws InvalidRequestException {
    if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
      // Derzeit unterstützen wir genau ein include-Element

      for (ValueSet.ConceptSetComponent include : valueSet.getCompose().getInclude()) {
        if (!include.getSystem().equals(LoincConstants.TERMINOLOGY_CANONICAL_NAME)) {
          throw new InvalidRequestException(
              "ValueSet Expansion currently supports only the expansion of LOINC-based ValueSets");
        }
      }
    }
  }

  private static void failOnValueSetHasExclude(ValueSet valueSet) throws InvalidRequestException {
    if (valueSet.hasCompose() && valueSet.getCompose().hasExclude()) {
      // Exclusion wird durch uns nicht unterstützt
      throw new InvalidRequestException(
          "ValueSet Expansion currently does not support the exclusion of codes");
    }
  }
}
