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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ConceptPropertyMatcher}. */
@DisplayName("ConceptPropertyMatcher Tests")
class ConceptPropertyMatcherTest {

  @Nested
  @DisplayName("hasMatchingPropertyType")
  class HasMatchingPropertyTypeTests {

    @Test
    @DisplayName("should return true when string property type matches")
    void shouldReturnTrueWhenStringPropertyTypeMatches() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent conceptProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("stringProp")
              .setValue(new StringType("alpha"));
      CodeSystem.PropertyComponent propertyDefinition =
          new CodeSystem.PropertyComponent()
              .setCode("stringProp")
              .setType(CodeSystem.PropertyType.STRING);

      // WHEN
      boolean result =
          ConceptPropertyMatcher.hasMatchingPropertyType(conceptProperty, propertyDefinition);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when property type does not match")
    void shouldReturnFalseWhenPropertyTypeDoesNotMatch() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent conceptProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("stringProp")
              .setValue(new StringType("alpha"));
      CodeSystem.PropertyComponent propertyDefinition =
          new CodeSystem.PropertyComponent()
              .setCode("stringProp")
              .setType(CodeSystem.PropertyType.INTEGER);

      // WHEN
      boolean result =
          ConceptPropertyMatcher.hasMatchingPropertyType(conceptProperty, propertyDefinition);

      // THEN
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("hasMatchingPropertyValue")
  class HasMatchingPropertyValueTests {

    @Test
    @DisplayName("should match supported primitive values")
    void shouldMatchSupportedPrimitiveValues() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent stringProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("stringProp")
              .setValue(new StringType("alpha"));
      CodeSystem.ConceptPropertyComponent booleanProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("boolProp")
              .setValue(new BooleanType(true));
      CodeSystem.ConceptPropertyComponent codeProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("codeProp")
              .setValue(new CodeType("A1"));
      CodeSystem.ConceptPropertyComponent decimalProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("decimalProp")
              .setValue(new DecimalType("1.50"));
      CodeSystem.ConceptPropertyComponent integerProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("intProp")
              .setValue(new IntegerType(42));

      // WHEN
      boolean stringMatches =
          ConceptPropertyMatcher.hasMatchingPropertyValue(stringProperty, "alpha");
      boolean booleanMatches =
          ConceptPropertyMatcher.hasMatchingPropertyValue(booleanProperty, "true");
      boolean codeMatches = ConceptPropertyMatcher.hasMatchingPropertyValue(codeProperty, "A1");
      boolean decimalMatches =
          ConceptPropertyMatcher.hasMatchingPropertyValue(decimalProperty, "1.50");
      boolean integerMatches =
          ConceptPropertyMatcher.hasMatchingPropertyValue(integerProperty, "42");

      // THEN
      assertThat(stringMatches && booleanMatches && codeMatches && decimalMatches && integerMatches)
          .isTrue();
    }

    @Test
    @DisplayName("should throw for unsupported coding and datetime value comparisons")
    void shouldThrowForUnsupportedCodingAndDateTimeValueComparisons() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent codingProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("codingProp")
              .setValue(new Coding("http://example.org/system", "1", "X1"));
      CodeSystem.ConceptPropertyComponent dateProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("dateProp")
              .setValue(new DateTimeType("2024-01-01"));

      // WHEN THEN
      assertThatThrownBy(
              () ->
                  ConceptPropertyMatcher.hasMatchingPropertyValue(
                      codingProperty, "http://example.org/system|1#X1"))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Coding type is not supported");

      assertThatThrownBy(
              () -> ConceptPropertyMatcher.hasMatchingPropertyValue(dateProperty, "2024-01-01"))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("DateTimeType type is not supported");
    }

    @Test
    @DisplayName("should return false when property has unsupported value type")
    void shouldReturnFalseWhenPropertyHasUnsupportedValueType() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent unsupportedProperty =
          new CodeSystem.ConceptPropertyComponent().setCode("unsupported");

      // WHEN
      boolean result =
          ConceptPropertyMatcher.hasMatchingPropertyValue(unsupportedProperty, "any-value");

      // THEN
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("hasRegExMatchingPropertyValue")
  class HasRegExMatchingPropertyValueTests {

    @Test
    @DisplayName("should match regex for string and code values")
    void shouldMatchRegexForStringAndCodeValues() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent stringProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("stringProp")
              .setValue(new StringType("alpha-beta"));
      CodeSystem.ConceptPropertyComponent codeProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("codeProp")
              .setValue(new CodeType("A1"));

      // WHEN
      boolean stringMatches =
          ConceptPropertyMatcher.hasRegExMatchingPropertyValue(stringProperty, "alpha.*");
      boolean codeMatches =
          ConceptPropertyMatcher.hasRegExMatchingPropertyValue(codeProperty, "A.*");

      // THEN
      assertThat(stringMatches && codeMatches).isTrue();
    }

    @Test
    @DisplayName("should return false for unsupported regex value types")
    void shouldReturnFalseForUnsupportedRegexValueTypes() {
      // GIVEN
      CodeSystem.ConceptPropertyComponent unsupportedProperty =
          new CodeSystem.ConceptPropertyComponent()
              .setCode("boolProp")
              .setValue(new BooleanType(true));

      // WHEN
      boolean result =
          ConceptPropertyMatcher.hasRegExMatchingPropertyValue(unsupportedProperty, ".*");

      // THEN
      assertThat(result).isFalse();
    }
  }
}
