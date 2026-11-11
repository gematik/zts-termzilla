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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE_LOWERCASE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SYNONYM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DATE_START;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_DOUBLE_POINT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_LONG_POINT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_LOWERCASE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_STANDARD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link ConceptComponentFieldGeneratorHelper}.
 *
 * <p>Verifies that all supported FHIR property types are correctly indexed as Lucene document
 * fields with appropriate field types and naming conventions.
 */
@DisplayName("ConceptComponentFieldGeneratorHelper Tests")
class ConceptComponentFieldGeneratorHelperTest {

  private static final Field.Store FIELD_STORE = Field.Store.YES;

  private Document document;

  @BeforeEach
  void setUp() {
    document = new Document();
  }

  // ========================================================================
  // Helper Methods
  // ========================================================================

  private boolean hasField(String fieldName) {
    return document.getField(fieldName) != null;
  }

  private String getFieldValue(String fieldName) {
    return document.get(fieldName);
  }

  private List<String> getAllFieldValues(String fieldName) {
    return document.getFields().stream()
        .filter(f -> f.name().equals(fieldName))
        .map(IndexableField::stringValue)
        .toList();
  }

  private static String propertyFieldName(String propertyName, String suffix) {
    return NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + suffix;
  }

  // ========================================================================
  // Dispatch / Routing Tests
  // ========================================================================

  @Nested
  @DisplayName("addPropertyFieldToDocument — Dispatch")
  class AddPropertyFieldToDocumentTests {

    @Test
    @DisplayName("Should delegate string property to string indexing method")
    void shouldDelegateStringProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createStringProperty("test-value");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "status", property, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName("status", SUFFIX_STRING_EXACT);
      assertThat(hasField(fieldName))
          .as("String property should create an exact string field")
          .isTrue();
      assertThat(getFieldValue(fieldName)).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Should delegate code property to code indexing method")
    void shouldDelegateCodeProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createCodeProperty("active");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "status", property, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName("status", SUFFIX_STRING_EXACT);
      assertThat(hasField(fieldName))
          .as("Code property should create an exact string field")
          .isTrue();
      assertThat(getFieldValue(fieldName)).isEqualTo("active");
    }

    @Test
    @DisplayName("Should delegate Coding property to coding indexing method")
    void shouldDelegateCodingProperty() {
      // GIVEN
      Type property =
          ConceptPropertyTestDataHelper.createCodingProperty("http://example.org", "1.0", "ABC");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "classification", property, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName("classification", SUFFIX_TOKEN_EXACT);
      assertThat(hasField(fieldName))
          .as("Coding property should create a token exact field")
          .isTrue();
    }

    @Test
    @DisplayName("Should delegate boolean property to boolean indexing method")
    void shouldDelegateBooleanProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createBooleanProperty(true);

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "active", property, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName("active", SUFFIX_STRING_EXACT);
      assertThat(hasField(fieldName))
          .as("Boolean property should create an exact string field")
          .isTrue();
      assertThat(getFieldValue(fieldName)).isEqualTo("true");
    }

    @Test
    @DisplayName("Should delegate integer property to integer indexing method")
    void shouldDelegateIntegerProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createIntegerProperty(42);

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "weight", property, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName("weight", SUFFIX_LONG_POINT);
      assertThat(hasField(fieldName))
          .as("Integer property should create a LongPoint field")
          .isTrue();
    }

    @Test
    @DisplayName("Should delegate decimal property to decimal indexing method")
    void shouldDelegateDecimalProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createDecimalProperty("3.14");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "score", property, FIELD_STORE);

      // THEN
      String numericFieldName = propertyFieldName("score", SUFFIX_DOUBLE_POINT);
      String exactFieldName = propertyFieldName("score", SUFFIX_STRING_EXACT);
      assertThat(hasField(numericFieldName))
          .as("Decimal property should create a DoublePoint field")
          .isTrue();
      assertThat(hasField(exactFieldName))
          .as("Decimal property should also create an exact string field")
          .isTrue();
    }

    @Test
    @DisplayName("Should delegate dateTime property to dateTime indexing method")
    void shouldDelegateDateTimeProperty() {
      // GIVEN
      Type property = ConceptPropertyTestDataHelper.createDateTimeProperty("2024-06-15");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
          document, "effective", property, FIELD_STORE);

      // THEN
      String startFieldName = propertyFieldName("effective", SUFFIX_DATE_START);
      String endFieldName = propertyFieldName("effective", SUFFIX_DATE_END);
      assertThat(hasField(startFieldName))
          .as("DateTime property should create a date_period_start field")
          .isTrue();
      assertThat(hasField(endFieldName))
          .as("DateTime property should create a date_period_end field")
          .isTrue();
    }
  }

  // ========================================================================
  // Boolean Property Tests
  // ========================================================================

  @Nested
  @DisplayName("addBooleanPropertyFieldToDocument")
  class AddBooleanPropertyFieldToDocumentTests {

    private static final String PROPERTY_NAME = "active";
    private static final String FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_STRING_EXACT;

    @Test
    @DisplayName("Should index true value as 'true' string")
    void shouldIndexTrueValue() {
      // GIVEN
      boolean value = true;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(FIELD_NAME))
          .as("Boolean true should be stored as string 'true'")
          .isEqualTo("true");
    }

    @Test
    @DisplayName("Should index false value as 'false' string")
    void shouldIndexFalseValue() {
      // GIVEN
      boolean value = false;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(FIELD_NAME))
          .as("Boolean false should be stored as string 'false'")
          .isEqualTo("false");
    }

    @Test
    @DisplayName("Should use property name in field name")
    void shouldUsePropertyNameInFieldName() {
      // GIVEN
      String customPropertyName = "deprecated";

      // WHEN
      ConceptComponentFieldGeneratorHelper.addBooleanPropertyFieldToDocument(
          document, customPropertyName, true, FIELD_STORE);

      // THEN
      String expectedFieldName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_STRING_EXACT;
      assertThat(hasField(expectedFieldName))
          .as("Field name should contain the property name '%s'", customPropertyName)
          .isTrue();
    }
  }

  // ========================================================================
  // Integer Property Tests
  // ========================================================================

  @Nested
  @DisplayName("addIntegerPropertyFieldToDocument")
  class AddIntegerPropertyFieldToDocumentTests {

    private static final String PROPERTY_NAME = "weight";
    private static final String FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_LONG_POINT;

    @Test
    @DisplayName("Should index positive integer as LongPoint")
    void shouldIndexPositiveInteger() {
      // GIVEN
      int value = 42;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(FIELD_NAME))
          .as("Positive integer should create a LongPoint field")
          .isTrue();
    }

    @Test
    @DisplayName("Should index negative integer as LongPoint")
    void shouldIndexNegativeInteger() {
      // GIVEN
      int value = -1;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(FIELD_NAME))
          .as("Negative integer should create a LongPoint field")
          .isTrue();
    }

    @Test
    @DisplayName("Should index zero as LongPoint")
    void shouldIndexZero() {
      // GIVEN
      int value = 0;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(FIELD_NAME)).as("Zero should create a LongPoint field").isTrue();
    }

    @Test
    @DisplayName("Should store integer value as StoredField for retrieval")
    void shouldStoreValueForRetrieval() {
      // GIVEN
      int value = 42;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN — LongPoint + StoredField should produce two fields with the same name
      List<IndexableField> fields =
          document.getFields().stream().filter(f -> f.name().equals(FIELD_NAME)).toList();
      assertThat(fields).as("Should have both a LongPoint and a StoredField").hasSize(2);
    }

    @Test
    @DisplayName("Should handle Integer.MAX_VALUE without overflow")
    void shouldHandleMaxValue() {
      // GIVEN
      int value = Integer.MAX_VALUE;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(FIELD_NAME))
          .as("Integer.MAX_VALUE should be indexable without overflow")
          .isTrue();
    }

    @Test
    @DisplayName("Should use property name in field name")
    void shouldUsePropertyNameInFieldName() {
      // GIVEN
      String customPropertyName = "priority";

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIntegerPropertyFieldToDocument(
          document, customPropertyName, 1, FIELD_STORE);

      // THEN
      String expectedFieldName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_LONG_POINT;
      assertThat(hasField(expectedFieldName))
          .as("Field name should contain the property name '%s'", customPropertyName)
          .isTrue();
    }
  }

  // ========================================================================
  // Decimal Property Tests
  // ========================================================================

  @Nested
  @DisplayName("addDecimalPropertyFieldToDocument")
  class AddDecimalPropertyFieldToDocumentTests {

    private static final String PROPERTY_NAME = "score";
    private static final String NUMERIC_FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_DOUBLE_POINT;
    private static final String EXACT_FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_STRING_EXACT;

    @Test
    @DisplayName("Should index decimal as DoublePoint field")
    void shouldIndexDecimalAsDoublePoint() {
      // GIVEN
      java.math.BigDecimal value = new java.math.BigDecimal("3.14");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(NUMERIC_FIELD_NAME))
          .as("Decimal should create a DoublePoint field")
          .isTrue();
    }

    @Test
    @DisplayName("Should index decimal as exact string field for lossless matching")
    void shouldIndexDecimalAsExactStringField() {
      // GIVEN
      java.math.BigDecimal value = new java.math.BigDecimal("3.14");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(EXACT_FIELD_NAME))
          .as("Decimal should also be stored as exact string for lossless matching")
          .isEqualTo("3.14");
    }

    @Test
    @DisplayName("Should preserve high precision in exact string field")
    void shouldPreserveHighPrecision() {
      // GIVEN
      java.math.BigDecimal value = new java.math.BigDecimal("1.23456789012345");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(EXACT_FIELD_NAME))
          .as("High precision decimal should be preserved in exact string field")
          .isEqualTo("1.23456789012345");
    }

    @Test
    @DisplayName("Should handle negative decimal values")
    void shouldHandleNegativeDecimal() {
      // GIVEN
      java.math.BigDecimal value = new java.math.BigDecimal("-0.5");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(NUMERIC_FIELD_NAME))
          .as("Negative decimal should create a DoublePoint field")
          .isTrue();
      assertThat(getFieldValue(EXACT_FIELD_NAME))
          .as("Negative decimal should be preserved in exact string field")
          .isEqualTo("-0.5");
    }

    @Test
    @DisplayName("Should store DoublePoint value for retrieval via StoredField")
    void shouldStoreDoubleValueForRetrieval() {
      // GIVEN
      java.math.BigDecimal value = new java.math.BigDecimal("99.99");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN — DoublePoint + StoredField + StringField = 3 fields total (2 for numeric name, 1 for
      // exact name)
      List<IndexableField> numericFields =
          document.getFields().stream().filter(f -> f.name().equals(NUMERIC_FIELD_NAME)).toList();
      assertThat(numericFields)
          .as("Should have both a DoublePoint and a StoredField for the numeric field name")
          .hasSize(2);
    }

    @Test
    @DisplayName("Should use property name in field names")
    void shouldUsePropertyNameInFieldNames() {
      // GIVEN
      String customPropertyName = "dosage";
      java.math.BigDecimal value = new java.math.BigDecimal("2.5");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDecimalPropertyFieldToDocument(
          document, customPropertyName, value, FIELD_STORE);

      // THEN
      String expectedNumericName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_DOUBLE_POINT;
      String expectedExactName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_STRING_EXACT;
      assertThat(hasField(expectedNumericName))
          .as("Numeric field name should contain the property name '%s'", customPropertyName)
          .isTrue();
      assertThat(hasField(expectedExactName))
          .as("Exact field name should contain the property name '%s'", customPropertyName)
          .isTrue();
    }
  }

  // ========================================================================
  // DateTime Property Tests
  // ========================================================================

  @Nested
  @DisplayName("addDateTimePropertyFieldToDocument")
  class AddDateTimePropertyFieldToDocumentTests {

    private static final String PROPERTY_NAME = "effective";
    private static final String START_FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_DATE_START;
    private static final String END_FIELD_NAME =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_NAME + SEPARATOR + SUFFIX_DATE_END;

    @Test
    @DisplayName("Should store both LongPoint and StoredField for each boundary")
    void shouldStoreBothFieldTypesForEachBoundary() {
      // GIVEN
      DateTimeType value = new DateTimeType("2024-06-15");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDateTimePropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN — LongPoint + StoredField = 2 fields per boundary
      List<IndexableField> startFields =
          document.getFields().stream().filter(f -> f.name().equals(START_FIELD_NAME)).toList();
      List<IndexableField> endFields =
          document.getFields().stream().filter(f -> f.name().equals(END_FIELD_NAME)).toList();
      assertThat(startFields)
          .as("Should have both a LongPoint and a StoredField for start")
          .hasSize(2);
      assertThat(endFields).as("Should have both a LongPoint and a StoredField for end").hasSize(2);
    }

    @Test
    @DisplayName("Should expand year precision to full year range")
    void shouldExpandYearPrecision() {
      // GIVEN
      DateTimeType value = new DateTimeType("2024");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDateTimePropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN — start and end should differ (year spans multiple instants)
      List<IndexableField> startFields =
          document.getFields().stream().filter(f -> f.name().equals(START_FIELD_NAME)).toList();
      List<IndexableField> endFields =
          document.getFields().stream().filter(f -> f.name().equals(END_FIELD_NAME)).toList();
      assertThat(startFields).as("Year precision should create start fields").hasSize(2);
      assertThat(endFields).as("Year precision should create end fields").hasSize(2);
    }

    @DisplayName(
        "Should create date_period_start and date_period_end fields for supported precisions")
    @ParameterizedTest(name = "precision={0}, value={1}")
    @CsvSource({"day,2024-06-15", "month,2024-06", "instant,2024-06-15T08:30:00.000Z"})
    void shouldCreateStartAndEndFieldsForSupportedPrecisions(
        String precision, String dateTimeValue) {
      // GIVEN
      DateTimeType value = new DateTimeType(dateTimeValue);

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDateTimePropertyFieldToDocument(
          document, PROPERTY_NAME, value, FIELD_STORE);

      // THEN
      assertThat(hasField(START_FIELD_NAME))
          .as("%s precision should create start field", precision)
          .isTrue();
      assertThat(hasField(END_FIELD_NAME))
          .as("%s precision should create end field", precision)
          .isTrue();
    }

    @Test
    @DisplayName("Should use property name in field names")
    void shouldUsePropertyNameInFieldNames() {
      // GIVEN
      String customPropertyName = "validFrom";
      DateTimeType value = new DateTimeType("2024-06-15");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addDateTimePropertyFieldToDocument(
          document, customPropertyName, value, FIELD_STORE);

      // THEN
      String expectedStartName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_DATE_START;
      String expectedEndName =
          NAME_PREFIX_PROPERTY + SEPARATOR + customPropertyName + SEPARATOR + SUFFIX_DATE_END;
      assertThat(hasField(expectedStartName))
          .as("Start field name should contain the property name '%s'", customPropertyName)
          .isTrue();
      assertThat(hasField(expectedEndName))
          .as("End field name should contain the property name '%s'", customPropertyName)
          .isTrue();
    }
  }

  // ========================================================================
  // Additional uncovered helper methods
  // ========================================================================

  @Nested
  @DisplayName("addStoredFieldToDocument")
  class AddStoredFieldToDocumentTests {

    @Test
    @DisplayName("Should store serialized resource bytes in _content field")
    void shouldStoreSerializedResourceBytes() {
      // GIVEN
      byte[] resource = new byte[] {1, 2, 3, 4};

      // WHEN
      ConceptComponentFieldGeneratorHelper.addStoredFieldToDocument(document, resource);

      // THEN
      byte[] storedValue =
          Arrays.copyOfRange(
              document.getBinaryValue("_content").bytes,
              document.getBinaryValue("_content").offset,
              document.getBinaryValue("_content").offset
                  + document.getBinaryValue("_content").length);
      assertThat(storedValue).isEqualTo(resource);
    }
  }

  @Nested
  @DisplayName("addSystemFieldToDocument")
  class AddSystemFieldToDocumentTests {

    @Test
    @DisplayName("Should create exact and sort fields for system")
    void shouldCreateExactAndSortFieldsForSystem() {
      // GIVEN
      String system = "http://example.org/system";
      String exactFieldName = NAME_SYSTEM + SEPARATOR + SUFFIX_STRING_EXACT;
      String sortFieldName = NAME_SYSTEM + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addSystemFieldToDocument(document, system, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(exactFieldName)).isEqualTo(system);
      assertThat(hasField(sortFieldName)).isTrue();
    }
  }

  @Nested
  @DisplayName("addVersionFieldToDocument")
  class AddVersionFieldToDocumentTests {

    @Test
    @DisplayName("Should create exact and sort fields for version")
    void shouldCreateExactAndSortFieldsForVersion() {
      // GIVEN
      String version = "2025";
      String exactFieldName = NAME_VERSION + SEPARATOR + SUFFIX_STRING_EXACT;
      String sortFieldName = NAME_VERSION + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addVersionFieldToDocument(
          document, version, FIELD_STORE);

      // THEN
      assertThat(getFieldValue(exactFieldName)).isEqualTo(version);
      assertThat(hasField(sortFieldName)).isTrue();
    }
  }

  @Nested
  @DisplayName("addCodeFieldNoSortToDocument")
  class AddCodeFieldNoSortToDocumentTests {

    @Test
    @DisplayName("Should create token and standard fields without sort field")
    void shouldCreateTokenAndStandardFieldsWithoutSortField() {
      // GIVEN
      String code = "ABC-123";
      String tokenFieldName = NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT;
      String standardFieldName = NAME_CODE + SEPARATOR + SUFFIX_STRING_STANDARD;
      String sortFieldName = NAME_CODE + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodeFieldNoSortToDocument(
          document, code, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(tokenFieldName)).containsExactly(code);
      assertThat(getAllFieldValues(standardFieldName)).containsExactly(code);
      assertThat(hasField(sortFieldName)).isFalse();
    }
  }

  @Nested
  @DisplayName("addCodeLowerCaseFieldToDocument")
  class AddCodeLowerCaseFieldToDocumentTests {

    @Test
    @DisplayName("Should create lowercase token and standard fields")
    void shouldCreateLowercaseTokenAndStandardFields() {
      // GIVEN
      String lowerCaseCode = "abc-123";
      String tokenFieldName = NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_TOKEN_EXACT;
      String standardFieldName = NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_STRING_STANDARD;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodeLowerCaseFieldToDocument(
          document, lowerCaseCode, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(tokenFieldName)).containsExactly(lowerCaseCode);
      assertThat(getAllFieldValues(standardFieldName)).containsExactly(lowerCaseCode);
    }
  }

  @Nested
  @DisplayName("addSynonymFieldToDocument")
  class AddSynonymFieldToDocumentTests {

    @Test
    @DisplayName("Should create exact contains and standard synonym fields")
    void shouldCreateExactContainsAndStandardSynonymFields() {
      // GIVEN
      String synonym = "Common display";
      String exactFieldName = NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_EXACT;
      String containsFieldName = NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_CONTAINS;
      String standardFieldName = NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_STANDARD;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addSynonymFieldToDocument(
          document, synonym, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(exactFieldName)).containsExactly(synonym);
      assertThat(getAllFieldValues(containsFieldName)).containsExactly(synonym);
      assertThat(getAllFieldValues(standardFieldName)).containsExactly(synonym);
    }
  }

  @Nested
  @DisplayName("addIdTokenParamFieldToDocument")
  class AddIdTokenParamFieldToDocumentTests {

    @Test
    @DisplayName("Should create token and sort field when sort is enabled")
    void shouldCreateTokenAndSortFieldWhenSortIsEnabled() {
      // GIVEN
      String paramName = "identifier";
      String paramValue = "id-123";
      String tokenFieldName = paramName + SEPARATOR + SUFFIX_TOKEN_EXACT;
      String sortFieldName = paramName + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIdTokenParamFieldToDocument(
          document, paramName, paramValue, true);

      // THEN
      assertThat(getFieldValue(tokenFieldName)).isEqualTo(paramValue);
      assertThat(hasField(sortFieldName)).isTrue();
    }

    @Test
    @DisplayName("Should create only token field when sort is disabled")
    void shouldCreateOnlyTokenFieldWhenSortIsDisabled() {
      // GIVEN
      String paramName = "identifier";
      String paramValue = "id-123";
      String tokenFieldName = paramName + SEPARATOR + SUFFIX_TOKEN_EXACT;
      String sortFieldName = paramName + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addIdTokenParamFieldToDocument(
          document, paramName, paramValue, false);

      // THEN
      assertThat(getFieldValue(tokenFieldName)).isEqualTo(paramValue);
      assertThat(hasField(sortFieldName)).isFalse();
    }
  }

  @Nested
  @DisplayName("addBooleanTokenParamFieldToDocument")
  class AddBooleanTokenParamFieldToDocumentTests {

    @Test
    @DisplayName("Should index plain and special-values boolean token variants")
    void shouldIndexPlainAndSpecialValuesBooleanTokenVariants() {
      // GIVEN
      String paramName = "active";
      String fieldName = paramName + SEPARATOR + SUFFIX_LOWERCASE_EXACT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addBooleanTokenParamFieldToDocument(
          document, paramName, Boolean.TRUE, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName))
          .containsExactlyInAnyOrder("true", "http://hl7.org/fhir/special-values|true");
    }
  }

  @Nested
  @DisplayName("addCodeTokenParamFieldToDocument")
  class AddCodeTokenParamFieldToDocumentTests {

    @Test
    @DisplayName("Should index plain and system|code variants and create sort field")
    void shouldIndexPlainAndSystemCodeVariantsAndCreateSortField() {
      // GIVEN
      String paramName = "status";
      String system = "http://example.org/system";
      String code = "active";
      String fieldName = paramName + SEPARATOR + SUFFIX_LOWERCASE_EXACT;
      String sortFieldName = paramName + SEPARATOR + SUFFIX_SORT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodeTokenParamFieldToDocument(
          document, paramName, system, code, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName))
          .containsExactlyInAnyOrder("active", "http://example.org/system|active");
      assertThat(hasField(sortFieldName)).isTrue();
    }
  }

  @Nested
  @DisplayName("addCodingTokenParamFieldToDocument")
  class AddCodingTokenParamFieldToDocumentTests {

    @Test
    @DisplayName("Should index lowercase variants when system and code are present")
    void shouldIndexLowercaseVariantsWhenSystemAndCodeArePresent() {
      // GIVEN
      String paramName = "designation";
      String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
          document, paramName, "HTTP://EXAMPLE.ORG/SYSTEM", "CODE-1", FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName))
          .containsExactlyInAnyOrder(
              "http://example.org/system|", "code-1", "http://example.org/system|code-1");
    }

    @Test
    @DisplayName("Should index code-only variants when system is missing")
    void shouldIndexCodeOnlyVariantsWhenSystemIsMissing() {
      // GIVEN
      String paramName = "designation";
      String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
          document, paramName, null, "CODE-1", FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName)).containsExactlyInAnyOrder("code-1", "|code-1");
    }

    @Test
    @DisplayName("Should index system-only variant when code is missing")
    void shouldIndexSystemOnlyVariantWhenCodeIsMissing() {
      // GIVEN
      String paramName = "designation";
      String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
          document, paramName, "HTTP://EXAMPLE.ORG/SYSTEM", null, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName)).containsExactly("http://example.org/system|");
    }

    @Test
    @DisplayName("Should not create fields when both system and code are missing")
    void shouldNotCreateFieldsWhenBothSystemAndCodeAreMissing() {
      // GIVEN
      String paramName = "designation";
      String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
          document, paramName, null, null, FIELD_STORE);

      // THEN
      assertThat(getAllFieldValues(fieldName)).isEmpty();
    }
  }

  // ========================================================================
  // Existing Property Types — Regression Tests
  // ========================================================================

  @Nested
  @DisplayName("addStringPropertyFieldToDocument")
  class AddStringPropertyFieldToDocumentTests {

    @Test
    @DisplayName("Should index string property as exact StringField")
    void shouldIndexStringPropertyAsExactField() {
      // GIVEN
      String propertyName = "comment";
      String value = "This is a test comment";

      // WHEN
      ConceptComponentFieldGeneratorHelper.addStringPropertyFieldToDocument(
          document, propertyName, value, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(propertyName, SUFFIX_STRING_EXACT);
      assertThat(getFieldValue(fieldName)).isEqualTo(value);
    }

    @Test
    @DisplayName("Should use property name in field name")
    void shouldUsePropertyNameInFieldName() {
      // GIVEN
      String propertyName = "description";

      // WHEN
      ConceptComponentFieldGeneratorHelper.addStringPropertyFieldToDocument(
          document, propertyName, "value", FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(propertyName, SUFFIX_STRING_EXACT);
      assertThat(hasField(fieldName)).isTrue();
    }
  }

  @Nested
  @DisplayName("addCodePropertyFieldToDocument")
  class AddCodePropertyFieldToDocumentTests {

    @Test
    @DisplayName("Should index code property as exact StringField")
    void shouldIndexCodePropertyAsExactField() {
      // GIVEN
      String propertyName = "status";
      CodeType codeValue = new CodeType("active");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodePropertyFieldToDocument(
          document, propertyName, codeValue, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(propertyName, SUFFIX_STRING_EXACT);
      assertThat(getFieldValue(fieldName)).isEqualTo("active");
    }

    @Test
    @DisplayName("Should use property name in field name")
    void shouldUsePropertyNameInFieldName() {
      // GIVEN
      String propertyName = "type";
      CodeType codeValue = new CodeType("code");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodePropertyFieldToDocument(
          document, propertyName, codeValue, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(propertyName, SUFFIX_STRING_EXACT);
      assertThat(hasField(fieldName)).isTrue();
    }
  }

  @Nested
  @DisplayName("addCodingPropertyFieldToDocument")
  class AddCodingPropertyFieldToDocumentTests {

    private static final String PROPERTY_NAME = "classification";

    @Test
    @DisplayName("Should index full Coding with system, version, and code")
    void shouldIndexFullCoding() {
      // GIVEN
      Coding coding = new Coding();
      coding.setSystem("http://example.org");
      coding.setVersion("1.0");
      coding.setCode("ABC");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingPropertyFieldToDocument(
          document, PROPERTY_NAME, coding, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(PROPERTY_NAME, SUFFIX_TOKEN_EXACT);
      List<String> values = getAllFieldValues(fieldName);
      assertThat(values)
          .as("Full Coding should produce system|version#code combination")
          .contains("http://example.org|1.0#ABC");
    }

    @Test
    @DisplayName("Should index partial Coding combinations for flexible matching")
    void shouldIndexPartialCodingCombinations() {
      // GIVEN
      Coding coding = new Coding();
      coding.setSystem("http://example.org");
      coding.setVersion("1.0");
      coding.setCode("ABC");

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingPropertyFieldToDocument(
          document, PROPERTY_NAME, coding, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(PROPERTY_NAME, SUFFIX_TOKEN_EXACT);
      List<String> values = getAllFieldValues(fieldName);
      assertThat(values)
          .as("Should contain multiple partial combinations for flexible token matching")
          .contains(
              "http://example.org|1.0#ABC",
              "http://example.org|1.0#",
              "http://example.org|#ABC",
              "http://example.org|#",
              "|1.0#ABC",
              "|1.0#",
              "|#ABC");
    }

    @Test
    @DisplayName("Should handle Coding with null components")
    void shouldHandleNullComponents() {
      // GIVEN
      Coding coding = new Coding();
      coding.setCode("ABC");
      // system and version are null

      // WHEN
      ConceptComponentFieldGeneratorHelper.addCodingPropertyFieldToDocument(
          document, PROPERTY_NAME, coding, FIELD_STORE);

      // THEN
      String fieldName = propertyFieldName(PROPERTY_NAME, SUFFIX_TOKEN_EXACT);
      List<String> values = getAllFieldValues(fieldName);
      assertThat(values)
          .as("Coding with null system/version should only produce code-based combinations")
          .contains("|#ABC")
          .doesNotContain("null|#ABC");
    }
  }
}
