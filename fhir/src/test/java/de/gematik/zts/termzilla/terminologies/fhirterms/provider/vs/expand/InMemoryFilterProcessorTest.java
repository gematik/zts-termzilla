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
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link InMemoryFilterProcessor} covering matcher-driven filter paths. */
@DisplayName("InMemoryFilterProcessor Tests")
class InMemoryFilterProcessorTest {

  @Nested
  @DisplayName("processFilter unsupported operators")
  class UnsupportedOperatorTests {

    @ParameterizedTest
    @EnumSource(
        value = ValueSet.FilterOperator.class,
        names = {"ISA", "ISNOTA", "GENERALIZES", "DESCENDENTOF", "IN"})
    @DisplayName("should fail because of unsupported filter operation")
    void shouldFailBecauseOfUnsupportedFilterOperation(ValueSet.FilterOperator operator) {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(operator, "code", "C1");
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN THEN
      assertThatThrownBy(() -> processor.processFilter(filter, null, true, resultSet, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported filter operation")
          .hasMessageContaining(operator.name())
          .hasMessageContaining(InMemoryFilterProcessorTestDataHelper.SYSTEM)
          .hasMessageContaining(InMemoryFilterProcessorTestDataHelper.VERSION);
    }
  }

  @Nested
  @DisplayName("processFilter equals loop")
  class EqualsLoopTests {

    @ParameterizedTest
    @CsvSource({
      "stringProp,alpha-beta,C1",
      "codeProp,A1,C1",
      "boolProp,true,C1",
      "decimalProp,1.50,C1",
      "intProp,42,C1"
    })
    @DisplayName("should match property values across supported matcher types")
    void shouldMatchPropertyValuesAcrossSupportedMatcherTypes(
        String property, String value, String expectedCode) throws Exception {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(
              ValueSet.FilterOperator.EQUAL, property, value);
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN
      processor.processFilter(filter, null, true, resultSet, codeSystem);

      // THEN
      assertThat(InMemoryFilterProcessorTestDataHelper.extractCodes(resultSet))
          .containsExactly(expectedCode);
    }

    @Test
    @DisplayName("should throw for coding property value comparisons")
    void shouldThrowForCodingPropertyValueComparisons() {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(
              ValueSet.FilterOperator.EQUAL, "codingProp", "http://example.org/system|1#X1");
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN THEN
      assertThatThrownBy(() -> processor.processFilter(filter, null, true, resultSet, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Coding type is not supported");
    }

    @Test
    @DisplayName("should throw for dateTime property value comparisons")
    void shouldThrowForDateTimePropertyValueComparisons() {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(
              ValueSet.FilterOperator.EQUAL, "dateProp", "2024-01-01");
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN THEN
      assertThatThrownBy(() -> processor.processFilter(filter, null, true, resultSet, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("DateTimeType type is not supported");
    }
  }

  @Nested
  @DisplayName("processFilter regex loop")
  class RegexLoopTests {

    @ParameterizedTest
    @CsvSource({"stringProp,alpha.*,C1", "codeProp,A.*,C1"})
    @DisplayName("should match regex for string and code property types")
    void shouldMatchRegexForStringAndCodePropertyTypes(
        String property, String regex, String expectedCode) throws Exception {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(
              ValueSet.FilterOperator.REGEX, property, regex);
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN
      processor.processFilter(filter, null, true, resultSet, codeSystem);

      // THEN
      assertThat(InMemoryFilterProcessorTestDataHelper.extractCodes(resultSet))
          .containsExactly(expectedCode);
    }

    @Test
    @DisplayName("should not match regex for unsupported property value types")
    void shouldNotMatchRegexForUnsupportedPropertyValueTypes() throws Exception {
      // GIVEN
      InMemoryFilterProcessor processor =
          new InMemoryFilterProcessor(mock(FhirTermsInMemoryRepresentation.class));
      CodeSystem codeSystem =
          InMemoryFilterProcessorTestDataHelper.createCodeSystemWithPropertyValues();
      ValueSet.ConceptSetFilterComponent filter =
          InMemoryFilterProcessorTestDataHelper.createFilter(
              ValueSet.FilterOperator.REGEX, "intProp", "42");
      Set<ConceptBaseInfo> resultSet = new HashSet<>();

      // WHEN
      processor.processFilter(filter, null, true, resultSet, codeSystem);

      // THEN
      assertThat(resultSet).isEmpty();
    }
  }
}
