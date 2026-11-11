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
package de.gematik.zts.termzilla.terminologies.snomed.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SnomedVsExpandParameterValidator Tests")
class SnomedVsExpandParameterValidatorTest {

  @Nested
  @DisplayName("failOnHierarchyBasedFilters")
  class FailOnHierarchyBasedFiltersTests {

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
      "constraint, =",
      "parent, =",
      "child, =",
      "concept, is-a",
      "concept, descendent-of"
    })
    @DisplayName("Should reject hierarchy-based filters")
    void shouldRejectHierarchyBasedFilters(String property, String operator) {
      // GIVEN
      ValueSet.ConceptSetComponent include =
          includeWithFilter(property, ValueSet.FilterOperator.fromCode(operator));

      // WHEN / THEN
      assertThatThrownBy(
              () -> SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(include))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessage(
              String.format(
                  SnomedVsExpandParameterValidator.HIERARCHY_BASED_FILTER_RESTRICTED_MESSAGE,
                  property,
                  operator));
    }

    @Test
    @DisplayName("Should allow concept in filter")
    void shouldAllowConceptInFilter() {
      // GIVEN
      ValueSet.ConceptSetComponent include =
          includeWithFilter("concept", ValueSet.FilterOperator.IN);

      // WHEN / THEN
      assertThatCode(() -> SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(include))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow include without filters")
    void shouldAllowIncludeWithoutFilters() {
      // GIVEN
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.addConcept().setCode("138875005");

      // WHEN / THEN
      assertThatCode(() -> SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(include))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should ignore filters without property or operator")
    void shouldIgnoreFiltersWithoutPropertyOrOperator() {
      // GIVEN
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.addFilter().setOp(ValueSet.FilterOperator.ISA).setValue("138875005");
      include.addFilter().setProperty("concept").setValue("138875005");

      // WHEN / THEN
      assertThatCode(() -> SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(include))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject hierarchy-based property without operator")
    void shouldRejectHierarchyBasedPropertyWithoutOperator() {
      // GIVEN
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.addFilter().setProperty("parent").setValue("138875005");

      // WHEN / THEN
      assertThatThrownBy(
              () -> SnomedVsExpandParameterValidator.failOnHierarchyBasedFilters(include))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("'parent '");
    }

    private ValueSet.ConceptSetComponent includeWithFilter(
        String property, ValueSet.FilterOperator operator) {
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.setSystem("http://snomed.info/sct");
      include.addFilter().setProperty(property).setOp(operator).setValue("138875005");
      return include;
    }
  }
}
