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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.CodeSystem;

/**
 * Utility class for matching concept property values against expected values.
 *
 * <p>Provides methods to check whether a concept property matches a given type, equals a specific
 * value, or matches a regular expression. Supports the FHIR property types: string, boolean, code,
 * decimal, integer, coding, and datetime.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ConceptPropertyMatcher {

  /**
   * Checks whether the concept property type matches the defined property type.
   *
   * @param cpc the concept property component to check
   * @param pc the property definition containing the expected type
   * @return {@code true} if the types match, {@code false} otherwise
   */
  static boolean hasMatchingPropertyType(
      CodeSystem.ConceptPropertyComponent cpc, CodeSystem.PropertyComponent pc) {

    return switch (pc.getType()) {
      case BOOLEAN -> cpc.hasValueBooleanType();
      case CODE -> cpc.hasValueCodeType();
      case CODING -> cpc.hasValueCoding();
      case DATETIME -> cpc.hasValueDateTimeType();
      case DECIMAL -> cpc.hasValueDecimalType();
      case INTEGER -> cpc.hasValueIntegerType();
      case STRING -> cpc.hasValueStringType();
      default -> false;
    };
  }

  /**
   * Checks whether the concept property value matches the expected value.
   *
   * <p>Supports comparison for string, boolean, code, decimal, and integer property types.
   *
   * @param cpc the concept property component to check
   * @param expectedValue the expected value to match against
   * @return {@code true} if the values match, {@code false} otherwise
   * @throws InvalidRequestException if the property type is Coding or DateTime (not supported)
   */
  static boolean hasMatchingPropertyValue(
      CodeSystem.ConceptPropertyComponent cpc, String expectedValue) {

    if (cpc.hasValueStringType()) {
      // Simple string comparison
      return cpc.getValueStringType().getValue().equals(expectedValue);
    } else if (cpc.hasValueBooleanType()) {
      // Boolean-Wert-Vergleich (true/false)
      return cpc.getValueBooleanType().getValue().toString().equals(expectedValue);
    } else if (cpc.hasValueCodeType()) {
      // Code-Wert-Vergleich
      return cpc.getValueCodeType().getValue().equals(expectedValue);
    } else if (cpc.hasValueCoding()) {
      throw new InvalidRequestException("Coding type is not supported in filter");
    } else if (cpc.hasValueDateTimeType()) {
      throw new InvalidRequestException("DateTimeType type is not supported in filter");
    } else if (cpc.hasValueDecimalType()) {
      // TODO: testen
      return cpc.getValueDecimalType().getValue().equals(new BigDecimal(expectedValue));
    } else if (cpc.hasValueIntegerType()) {
      // TODO: testen
      return cpc.getValueIntegerType().getValue().longValue() == Long.parseLong(expectedValue);
    }

    // Wenn wir hier ankommen, dann haben wir einen Typ, den wir nicht unterstützen
    return false;
  }

  /**
   * Checks whether the concept property value matches the specified regular expression.
   *
   * <p>Only supports string and code property types for regex matching.
   *
   * @param cpc the concept property component to check
   * @param regEx the regular expression to match against
   * @return {@code true} if the value matches the regex, {@code false} otherwise
   */
  static boolean hasRegExMatchingPropertyValue(
      CodeSystem.ConceptPropertyComponent cpc, String regEx) {

    if (cpc.hasValueStringType()) {
      // Einfacher String-Vergleich
      return cpc.getValueStringType().getValue().matches(regEx);
    } else if (cpc.hasValueCodeType()) {
      // Code-Wert-Vergleich
      return cpc.getValueCodeType().getValue().matches(regEx);
    }

    // Wenn wir hier ankommen, dann haben wir einen Typ, den wir nicht unterstützen
    return false;
  }
}
