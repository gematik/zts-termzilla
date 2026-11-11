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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExpandFilterValidator}.
 *
 * <p>Verifies that all upfront filter validations (property existence, operator support, regex
 * syntax, regex property type, hierarchy prerequisites, and Lucene compatibility classification)
 * work correctly.
 */
@DisplayName("ExpandFilterValidator")
class ExpandFilterValidatorTest {

  private static final String TEST_PROPERTY = "status";
  private static final String TEST_VALUE = "active";

  // ============================================================================================
  // validateFilters
  // ============================================================================================

  @Nested
  @DisplayName("validateFilters")
  class ValidateFiltersTests {

    @Test
    @DisplayName("Should accept valid EQUAL filter on known property")
    void shouldAcceptValidEqualFilter() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateFilters(List.of(filter), codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject filter with unknown property")
    void shouldRejectFilterWithUnknownProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("unknownProperty", ValueSet.FilterOperator.EQUAL, TEST_VALUE);
      List<ValueSet.ConceptSetFilterComponent> filters = List.of(filter);

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateFilters(filters, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported property")
          .hasMessageContaining("unknownProperty");
    }

    @Test
    @DisplayName("Should validate all filters and fail on first invalid one")
    void shouldValidateAllFiltersAndFailOnFirstInvalid() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent validFilter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, TEST_VALUE);
      ValueSet.ConceptSetFilterComponent invalidFilter =
          createFilter("missingProperty", ValueSet.FilterOperator.EQUAL, TEST_VALUE);
      List<ValueSet.ConceptSetFilterComponent> filters = List.of(validFilter, invalidFilter);

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateFilters(filters, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("missingProperty");
    }
  }

  // ============================================================================================
  // validatePropertyExists
  // ============================================================================================

  @Nested
  @DisplayName("validatePropertyExists")
  class ValidatePropertyExistsTests {

    @Test
    @DisplayName("Should accept explicitly defined property")
    void shouldAcceptExplicitlyDefinedProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validatePropertyExists(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept implicit 'code' property")
    void shouldAcceptImplicitCodeProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("code", ValueSet.FilterOperator.EQUAL, "A01");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validatePropertyExists(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept implicit 'display' property")
    void shouldAcceptImplicitDisplayProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("display", ValueSet.FilterOperator.EQUAL, "test");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validatePropertyExists(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept implicit 'parent' property")
    void shouldAcceptImplicitParentProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("parent", ValueSet.FilterOperator.EXISTS, "true");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validatePropertyExists(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject unknown property")
    void shouldRejectUnknownProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("nonexistent", ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validatePropertyExists(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(ExpandFilterValidator.UNSUPPORTED_FILTER_PROPERTY_MESSAGE);
    }
  }

  // ============================================================================================
  // validateOperatorSupported
  // ============================================================================================

  @Nested
  @DisplayName("validateOperatorSupported")
  class ValidateOperatorSupportedTests {

    @Test
    @DisplayName("Should accept EQUAL operator")
    void shouldAcceptEqualOperator() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      assertThatCode(() -> ExpandFilterValidator.validateOperatorSupported(filter))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept REGEX operator")
    void shouldAcceptRegexOperator() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.REGEX, ".*");

      assertThatCode(() -> ExpandFilterValidator.validateOperatorSupported(filter))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept ISA operator")
    void shouldAcceptIsaOperator() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("concept", ValueSet.FilterOperator.ISA, "A01");

      assertThatCode(() -> ExpandFilterValidator.validateOperatorSupported(filter))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept EXISTS operator")
    void shouldAcceptExistsOperator() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("parent", ValueSet.FilterOperator.EXISTS, "true");

      assertThatCode(() -> ExpandFilterValidator.validateOperatorSupported(filter))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept all other supported operators")
    void shouldAcceptAllSupportedOperators() {
      ValueSet.FilterOperator[] supportedOps = {
        ValueSet.FilterOperator.EQUAL,
        ValueSet.FilterOperator.REGEX,
        ValueSet.FilterOperator.EXISTS,
        ValueSet.FilterOperator.ISA,
        ValueSet.FilterOperator.ISNOTA,
        ValueSet.FilterOperator.GENERALIZES,
        ValueSet.FilterOperator.DESCENDENTOF,
        ValueSet.FilterOperator.IN
      };

      for (ValueSet.FilterOperator op : supportedOps) {
        ValueSet.ConceptSetFilterComponent filter = createFilter(TEST_PROPERTY, op, TEST_VALUE);

        assertThatCode(() -> ExpandFilterValidator.validateOperatorSupported(filter))
            .as("Operator %s should be accepted", op)
            .doesNotThrowAnyException();
      }
    }

    @Test
    @DisplayName("Should reject NOTIN operator")
    void shouldRejectNotInOperator() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.NOTIN, TEST_VALUE);

      assertThatThrownBy(() -> ExpandFilterValidator.validateOperatorSupported(filter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported operation");
    }
  }

  // ============================================================================================
  // validateRegexFilter
  // ============================================================================================

  @Nested
  @DisplayName("validateRegexFilter")
  class ValidateRegexFilterTests {

    @Test
    @DisplayName("Should skip validation for non-regex operators")
    void shouldSkipValidationForNonRegexOperator() {
      // GIVEN - EQUAL filter with an unsupported regex pattern as value (should be ignored)
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, "(?i)active");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept regex on code property")
    void shouldAcceptRegexOnCodeProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("code", ValueSet.FilterOperator.REGEX, "[A-Z][0-9]+");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept regex on string property")
    void shouldAcceptRegexOnStringProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty("description", CodeSystem.PropertyType.STRING);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("description", ValueSet.FilterOperator.REGEX, ".*diabetes.*");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept regex on CODE type property")
    void shouldAcceptRegexOnCodeTypeProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.REGEX, "active|inactive");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject regex with unsupported pattern syntax")
    void shouldRejectRegexWithUnsupportedSyntax() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty(TEST_PROPERTY, CodeSystem.PropertyType.CODE);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.REGEX, "(?i)active");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported features");
    }

    @Test
    @DisplayName("Should reject regex on parent property")
    void shouldRejectRegexOnParentProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("parent", ValueSet.FilterOperator.REGEX, ".*");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported combination")
          .hasMessageContaining("parent");
    }

    @Test
    @DisplayName("Should reject regex on child property")
    void shouldRejectRegexOnChildProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("child", ValueSet.FilterOperator.REGEX, ".*");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported combination")
          .hasMessageContaining("child");
    }

    @Test
    @DisplayName("Should reject regex on boolean property")
    void shouldRejectRegexOnBooleanProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty("active", CodeSystem.PropertyType.BOOLEAN);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("active", ValueSet.FilterOperator.REGEX, "true");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not supported")
          .hasMessageContaining("boolean");
    }

    @Test
    @DisplayName("Should reject regex on integer property")
    void shouldRejectRegexOnIntegerProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithProperty("weight", CodeSystem.PropertyType.INTEGER);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("weight", ValueSet.FilterOperator.REGEX, "[0-9]+");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateRegexFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not supported")
          .hasMessageContaining("integer");
    }
  }

  // ============================================================================================
  // validateHierarchyFilter
  // ============================================================================================

  @Nested
  @DisplayName("validateHierarchyFilter")
  class ValidateHierarchyFilterTests {

    @Test
    @DisplayName("Should skip validation for non-hierarchy operators")
    void shouldSkipValidationForNonHierarchyOperators() {
      // GIVEN - EQUAL filter with invalid hierarchy property
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("someProperty", ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept ISA filter with is-a hierarchy and concept property")
    void shouldAcceptIsaFilterWithIsaHierarchy() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.ISA);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("concept", ValueSet.FilterOperator.ISA, "A01");

      // WHEN / THEN
      assertThatCode(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject ISA filter without hierarchy meaning")
    void shouldRejectIsaFilterWithoutHierarchyMeaning() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("concept", ValueSet.FilterOperator.ISA, "A01");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("hierarchyMeaning");
    }

    @Test
    @DisplayName("Should reject ISA filter with wrong hierarchy meaning")
    void shouldRejectIsaFilterWithWrongHierarchyMeaning() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.PARTOF);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("concept", ValueSet.FilterOperator.ISA, "A01");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("hierarchyMeaning");
    }

    @Test
    @DisplayName("Should reject ISA filter with wrong property")
    void shouldRejectIsaFilterWithWrongProperty() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.ISA);
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("status", ValueSet.FilterOperator.ISA, "A01");

      // WHEN / THEN
      assertThatThrownBy(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("invalid property-operator-combination");
    }

    @Test
    @DisplayName("Should validate all hierarchy operators")
    void shouldValidateAllHierarchyOperators() {
      // GIVEN
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.ISA);

      ValueSet.FilterOperator[] hierarchyOps = {
        ValueSet.FilterOperator.ISA,
        ValueSet.FilterOperator.ISNOTA,
        ValueSet.FilterOperator.GENERALIZES,
        ValueSet.FilterOperator.DESCENDENTOF
      };

      for (ValueSet.FilterOperator op : hierarchyOps) {
        ValueSet.ConceptSetFilterComponent filter = createFilter("concept", op, "A01");

        assertThatCode(() -> ExpandFilterValidator.validateHierarchyFilter(filter, codeSystem))
            .as("Operator %s with concept property and is-a hierarchy should be accepted", op)
            .doesNotThrowAnyException();
      }
    }
  }

  // ============================================================================================
  // isLuceneCompatibleFilter
  // ============================================================================================

  @Nested
  @DisplayName("isLuceneCompatibleFilter")
  class IsLuceneCompatibleFilterTests {

    @Test
    @DisplayName("Should return true for EQUAL filter on non-hierarchy property")
    void shouldReturnTrueForEqualOnNonHierarchyProperty() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isTrue();
    }

    @Test
    @DisplayName("Should return true for REGEX filter on non-hierarchy property")
    void shouldReturnTrueForRegexOnNonHierarchyProperty() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter(TEST_PROPERTY, ValueSet.FilterOperator.REGEX, ".*");

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isTrue();
    }

    @Test
    @DisplayName("Should return false for EQUAL filter on parent property")
    void shouldReturnFalseForEqualOnParentProperty() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("parent", ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isFalse();
    }

    @Test
    @DisplayName("Should return false for EQUAL filter on child property")
    void shouldReturnFalseForEqualOnChildProperty() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("child", ValueSet.FilterOperator.EQUAL, TEST_VALUE);

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isFalse();
    }

    @Test
    @DisplayName("Should return false for ISA filter")
    void shouldReturnFalseForIsaFilter() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("concept", ValueSet.FilterOperator.ISA, "A01");

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isFalse();
    }

    @Test
    @DisplayName("Should return false for EXISTS filter")
    void shouldReturnFalseForExistsFilter() {
      ValueSet.ConceptSetFilterComponent filter =
          createFilter("parent", ValueSet.FilterOperator.EXISTS, "true");

      assertThat(ExpandFilterValidator.isLuceneCompatibleFilter(filter)).isFalse();
    }
  }

  // ============================================================================================
  // hasSupportedRegexPropertyType
  // ============================================================================================

  @Nested
  @DisplayName("hasSupportedRegexPropertyType")
  class HasSupportedRegexPropertyTypeTests {

    @Test
    @DisplayName("Should return true for CODE type")
    void shouldReturnTrueForCodeType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.CODE);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isTrue();
    }

    @Test
    @DisplayName("Should return true for STRING type")
    void shouldReturnTrueForStringType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.STRING);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isTrue();
    }

    @Test
    @DisplayName("Should return false for BOOLEAN type")
    void shouldReturnFalseForBooleanType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.BOOLEAN);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isFalse();
    }

    @Test
    @DisplayName("Should return false for INTEGER type")
    void shouldReturnFalseForIntegerType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.INTEGER);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isFalse();
    }

    @Test
    @DisplayName("Should return false for DECIMAL type")
    void shouldReturnFalseForDecimalType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.DECIMAL);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isFalse();
    }

    @Test
    @DisplayName("Should return false for DATETIME type")
    void shouldReturnFalseForDateTimeType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.DATETIME);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isFalse();
    }

    @Test
    @DisplayName("Should return false for CODING type")
    void shouldReturnFalseForCodingType() {
      CodeSystem.PropertyComponent property =
          new CodeSystem.PropertyComponent().setType(CodeSystem.PropertyType.CODING);

      assertThat(ExpandFilterValidator.hasSupportedRegexPropertyType(property)).isFalse();
    }
  }

  // ============================================================================================
  // Helper methods
  // ============================================================================================

  private static ValueSet.ConceptSetFilterComponent createFilter(
      String property, ValueSet.FilterOperator op, String value) {
    return new ValueSet.ConceptSetFilterComponent().setProperty(property).setOp(op).setValue(value);
  }

  private static CodeSystem createCodeSystemWithProperty(
      String propertyCode, CodeSystem.PropertyType type) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.addProperty().setCode(propertyCode).setType(type);
    return codeSystem;
  }
}
