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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DEFINITION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DISPLAY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SYNONYM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_TYPE;
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

import de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.math.BigDecimal;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Type;

/**
 * Utility class for creating Lucene index fields from FHIR {@link
 * org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent} data.
 *
 * <p>Provides static methods to add various concept-level fields to a Lucene {@link Document},
 * including basic concept fields (code, display, definition), designation fields (synonyms), and
 * property fields for all supported FHIR property types.
 *
 * <p>Supported FHIR property types for indexing:
 *
 * <ul>
 *   <li>{@code string} — indexed as {@link StringField} for exact matching
 *   <li>{@code code} — indexed as {@link StringField} for exact matching
 *   <li>{@code Coding} — indexed as {@link StringField} with multiple {@code system|version#code}
 *       combinations for flexible token matching
 *   <li>{@code boolean} — indexed as {@link StringField} with {@code "true"}/{@code "false"} values
 *   <li>{@code integer} — indexed as {@link LongPoint} for numeric exact and range queries, with a
 *       {@link StoredField} for value retrieval
 *   <li>{@code decimal} — dual-indexed as {@link DoublePoint} for numeric range queries and as
 *       {@link StringField} with the exact {@link BigDecimal} string representation for lossless
 *       equality matching
 *   <li>{@code dateTime} — indexed as a date period with two {@link LongPoint} fields ({@code
 *       date_period_start} and {@code date_period_end}) using {@link
 *       DateUtil
 *       DateUtil} for FHIR-conformant precision-aware range queries
 * </ul>
 *
 * @see ConceptComponentFieldGeneratorHelper
 * @see
 *     ConceptComponentFieldNames
 */
@Slf4j
public class ConceptComponentFieldGeneratorHelper {

  private ConceptComponentFieldGeneratorHelper() {
    // Private constructor - utility class should not be instantiated
  }

  // TODO: ggf. für die Speicherung der Serialisierung aufbewahren

  /**
   * Stores a serialized resource as a binary stored field in the document.
   *
   * @param document the Lucene document to add the field to
   * @param resource the serialized resource bytes
   */
  public static void addStoredFieldToDocument(Document document, byte[] resource) {
    document.add(new StoredField("_content", resource));
  }

  /**
   * Adds a type field to the document for filtering by FHIR resource or component type.
   *
   * <p>Field name: {@code _type_exact}
   *
   * @param document the Lucene document to add the field to
   * @param type the type value (e.g., {@code "ConceptDefinitionComponent"})
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addTypeFieldToDocument(
      Document document, String type, Field.Store fieldStore) {

    document.add(new StringField(NAME_TYPE + SEPARATOR + SUFFIX_TOKEN_EXACT, type, fieldStore));
  }

  /**
   * Adds one {@code scope} field per scope entry to the document for multi-tenant filtering.
   *
   * @param document the Lucene document to add the fields to
   * @param scopeSet the set of scope identifiers
   * @param fieldStore whether to store the field values for retrieval
   */
  public static void addScopeSetToDocument(
      Document document, Set<String> scopeSet, Field.Store fieldStore) {

    for (String scope : scopeSet) {
      document.add(new StringField(NAME_SCOPE, scope, fieldStore));
    }
  }

  /**
   * Adds a resource identifier field to the document.
   *
   * <p>Field name: {@code _id_tokenexact}
   *
   * @param document the Lucene document to add the field to
   * @param id the resource identifier value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addIdFieldToDocument(Document document, String id, Field.Store fieldStore) {

    document.add(new StringField(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT, id, fieldStore));
  }

  /**
   * Adds a CodeSystem system URL field to the document with exact matching and sort support.
   *
   * <p>Fields created: {@code system_exact} and {@code system_sort}
   *
   * @param document the Lucene document to add the fields to
   * @param system the CodeSystem canonical URL
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addSystemFieldToDocument(
      Document document, String system, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_SYSTEM + SEPARATOR + SUFFIX_STRING_EXACT, system, fieldStore));

    document.add(
        new SortedDocValuesField(NAME_SYSTEM + SEPARATOR + SUFFIX_SORT, new BytesRef(system)));
  }

  /**
   * Adds a CodeSystem version field to the document with exact matching and sort support.
   *
   * <p>Fields created: {@code version_exact} and {@code version_sort}
   *
   * @param document the Lucene document to add the fields to
   * @param version the CodeSystem version string
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addVersionFieldToDocument(
      Document document, String version, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_VERSION + SEPARATOR + SUFFIX_STRING_EXACT, version, fieldStore));

    document.add(
        new SortedDocValuesField(NAME_VERSION + SEPARATOR + SUFFIX_SORT, new BytesRef(version)));
  }

  /**
   * Adds a concept code field to the document with exact token matching, prefix-based standard
   * search, and sort support.
   *
   * <p>Fields created: {@code code_tokenexact}, {@code code_standard}, and {@code code_sort}
   *
   * @param document the Lucene document to add the fields to
   * @param code the concept code value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodeFieldToDocument(
      Document document, String code, Field.Store fieldStore) {

    document.add(new StringField(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT, code, fieldStore));

    document.add(new TextField(NAME_CODE + SEPARATOR + SUFFIX_STRING_STANDARD, code, fieldStore));

    document.add(new SortedDocValuesField(NAME_CODE + SEPARATOR + SUFFIX_SORT, new BytesRef(code)));
  }

  /**
   * Adds a concept code field to the document without sort support. Used for alternative codes that
   * should be searchable but not affect sort order.
   *
   * <p>Fields created: {@code code_tokenexact} and {@code code_standard}
   *
   * @param document the Lucene document to add the fields to
   * @param code the alternative code value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodeFieldNoSortToDocument(
      Document document, String code, Field.Store fieldStore) {

    document.add(new StringField(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT, code, fieldStore));

    document.add(new TextField(NAME_CODE + SEPARATOR + SUFFIX_STRING_STANDARD, code, fieldStore));
  }

  /**
   * Adds a lowercase code field to the document for case-insensitive code matching. Used when a
   * CodeSystem is configured as not case-sensitive.
   *
   * <p>Fields created: {@code code_lowercase_tokenexact} and {@code code_lowercase_standard}
   *
   * @param document the Lucene document to add the fields to
   * @param code the code value (should be pre-lowercased by the caller)
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodeLowerCaseFieldToDocument(
      Document document, String code, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_TOKEN_EXACT, code, fieldStore));

    document.add(
        new TextField(NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_STRING_STANDARD, code, fieldStore));
  }

  /**
   * Adds a concept display field to the document with exact, contains, and standard search
   * variants.
   *
   * <p>Fields created: {@code display_exact}, {@code display_contains}, and {@code
   * display_standard}
   *
   * @param document the Lucene document to add the fields to
   * @param display the display text value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addDisplayFieldToDocument(
      Document document, String display, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_DISPLAY + SEPARATOR + SUFFIX_STRING_EXACT, display, fieldStore));
    document.add(
        new TextField(NAME_DISPLAY + SEPARATOR + SUFFIX_STRING_CONTAINS, display, fieldStore));
    document.add(
        new TextField(NAME_DISPLAY + SEPARATOR + SUFFIX_STRING_STANDARD, display, fieldStore));
  }

  /**
   * Adds a synonym field to the document with exact, contains, and standard search variants.
   *
   * <p>Fields created: {@code synonym_exact}, {@code synonym_contains}, and {@code
   * synonym_standard}
   *
   * @param document the Lucene document to add the fields to
   * @param display the synonym text value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addSynonymFieldToDocument(
      Document document, String display, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_EXACT, display, fieldStore));
    document.add(
        new TextField(NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_CONTAINS, display, fieldStore));
    document.add(
        new TextField(NAME_SYNONYM + SEPARATOR + SUFFIX_STRING_STANDARD, display, fieldStore));
  }

  /**
   * Adds a concept definition field to the document with exact, contains, and standard search
   * variants.
   *
   * <p>Fields created: {@code definition_exact}, {@code definition_contains}, and {@code
   * definition_standard}
   *
   * @param document the Lucene document to add the fields to
   * @param definition the definition text value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addDefinitionFieldToDocument(
      Document document, String definition, Field.Store fieldStore) {

    document.add(
        new StringField(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_EXACT, definition, fieldStore));
    document.add(
        new TextField(
            NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_CONTAINS, definition, fieldStore));
    document.add(
        new TextField(
            NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_STANDARD, definition, fieldStore));
  }

  /**
   * Dispatches a FHIR concept property value to the appropriate type-specific indexing method based
   * on the property's FHIR type.
   *
   * <p>Supported types: {@code string}, {@code code}, {@code Coding}, {@code boolean}, {@code
   * integer}, {@code decimal}, {@code dateTime}. Unsupported types are logged as warnings.
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the FHIR {@link Type} value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addPropertyFieldToDocument(
      Document document, String propertyName, Type propertyValue, Field.Store fieldStore) {

    switch (propertyValue.fhirType()) {
      case "code":
        addCodePropertyFieldToDocument(
            document, propertyName, (CodeType) propertyValue, fieldStore);
        break;
      case "Coding":
        addCodingPropertyFieldToDocument(
            document, propertyName, (Coding) propertyValue, fieldStore);
        break;
      case "string":
        addStringPropertyFieldToDocument(
            document, propertyName, propertyValue.toString(), fieldStore);
        break;
      case "integer":
        addIntegerPropertyFieldToDocument(
            document, propertyName, ((IntegerType) propertyValue).getValue(), fieldStore);
        break;
      case "boolean":
        addBooleanPropertyFieldToDocument(
            document, propertyName, ((BooleanType) propertyValue).booleanValue(), fieldStore);
        break;
      case "dateTime":
        addDateTimePropertyFieldToDocument(
            document, propertyName, (DateTimeType) propertyValue, fieldStore);
        break;
      case "decimal":
        addDecimalPropertyFieldToDocument(
            document, propertyName, ((DecimalType) propertyValue).getValue(), fieldStore);
        break;
      default:
        log.warn("Property Type not supported: {}", propertyValue.fhirType());
    }
  }

  // --------------------------------------------------------------------------------
  // 'string' Property Searches

  /**
   * Indexes a string property value as a {@link StringField} for exact matching.
   *
   * <p>Field name: {@code property_{propertyName}_exact}
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the string value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addStringPropertyFieldToDocument(
      Document document, String propertyName, String propertyValue, Field.Store fieldStore) {

    document.add(
        new StringField(
            NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_STRING_EXACT,
            propertyValue,
            fieldStore));
  }

  // 'code' Property Searches

  /**
   * Indexes a code-typed property value as a {@link StringField} for exact matching.
   *
   * <p>Field name: {@code property_{propertyName}_exact}
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the code value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodePropertyFieldToDocument(
      Document document, String propertyName, CodeType propertyValue, Field.Store fieldStore) {

    document.add(
        new StringField(
            NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_STRING_EXACT,
            propertyValue.getValue(),
            fieldStore));
  }

  /**
   * Indexes a Coding-typed property value with multiple {@code system|version#code} combinations
   * for flexible token matching.
   *
   * <p>Creates multiple field entries under {@code property_{propertyName}_tokenexact} with
   * different combinations of system, version, and code to support partial matching patterns (e.g.,
   * system-only, code-only, system+code without version).
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the {@link Coding} value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodingPropertyFieldToDocument(
      Document document, String propertyName, Coding propertyValue, Field.Store fieldStore) {

    String system = propertyValue.getSystem();
    String version = propertyValue.getVersion();
    String code = propertyValue.getCode();

    String fieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_TOKEN_EXACT;

    if (system != null && version != null && code != null) {
      document.add(new StringField(fieldName, system + "|" + version + "#" + code, fieldStore));
    }
    if (system != null && version != null) {
      document.add(new StringField(fieldName, system + "|" + version + "#", fieldStore));
    }
    if (system != null && code != null) {
      document.add(new StringField(fieldName, system + "|#" + code, fieldStore));
    }
    if (system != null) {
      document.add(new StringField(fieldName, system + "|#", fieldStore));
    }
    if (version != null && code != null) {
      document.add(new StringField(fieldName, "|" + version + "#" + code, fieldStore));
    }
    if (version != null) {
      document.add(new StringField(fieldName, "|" + version + "#", fieldStore));
    }
    if (code != null) {
      document.add(new StringField(fieldName, "|#" + code, fieldStore));
    }
  }

  // --------------------------------------------------------------------------------
  // 'boolean' Property Searches

  /**
   * Indexes a boolean property value as a string field for exact matching.
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the boolean value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addBooleanPropertyFieldToDocument(
      Document document, String propertyName, boolean propertyValue, Field.Store fieldStore) {

    document.add(
        new StringField(
            NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_STRING_EXACT,
            String.valueOf(propertyValue),
            fieldStore));
  }

  // --------------------------------------------------------------------------------
  // 'integer' Property Searches

  /**
   * Indexes an integer property value as a {@link LongPoint} for numeric queries and a {@link
   * StoredField} for value retrieval.
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the integer value to index
   * @param fieldStore whether to store the field value for retrieval (stored via {@link
   *     StoredField} regardless)
   */
  public static void addIntegerPropertyFieldToDocument(
      Document document, String propertyName, int propertyValue, Field.Store fieldStore) {

    String fieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_LONG_POINT;
    document.add(new LongPoint(fieldName, propertyValue));
    // TODO should we always store this? Then we should remove the parameter
    if (fieldStore == Field.Store.YES) {
      document.add(new StoredField(fieldName, propertyValue));
    }
  }

  // --------------------------------------------------------------------------------
  // 'decimal' Property Searches

  /**
   * Indexes a decimal property value using dual indexing: a {@link DoublePoint} for numeric range
   * queries and a {@link StringField} with the exact {@link BigDecimal} string representation for
   * lossless equality matching.
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the decimal value to index
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addDecimalPropertyFieldToDocument(
      Document document, String propertyName, BigDecimal propertyValue, Field.Store fieldStore) {

    // Numeric field for range queries
    String numericFieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_DOUBLE_POINT;
    document.add(new DoublePoint(numericFieldName, propertyValue.doubleValue()));
    document.add(new StoredField(numericFieldName, propertyValue.doubleValue()));

    // Exact string field for lossless BigDecimal equality matching
    document.add(
        new StringField(
            NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_STRING_EXACT,
            propertyValue.toPlainString(),
            fieldStore));
  }

  // --------------------------------------------------------------------------------
  // 'dateTime' Property Searches

  /**
   * Indexes a dateTime property value as a date period with start and end boundaries, consistent
   * with the date indexing pattern used in {@link ResourceFieldGeneratorHelper}.
   *
   * <p>FHIR DateTimeType values carry variable precision (year, month, day, instant). This method
   * expands the value into an implicit period using {@link DateUtil} and creates two {@link
   * LongPoint} fields for the start and end boundaries, enabling FHIR-conformant range queries.
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li>{@code property_{propertyName}_date_period_start} — first instant of the precision range
   *   <li>{@code property_{propertyName}_date_period_end} — last instant of the precision range
   * </ul>
   *
   * @param document the Lucene document to add fields to
   * @param propertyName the property code identifying the property
   * @param propertyValue the FHIR {@link DateTimeType} value with variable precision
   * @param fieldStore whether to store the field value for retrieval (stored via {@link
   *     StoredField} regardless)
   * @see DateUtil#getFirstInstantOfDateTime(DateTimeType)
   * @see DateUtil#getLastInstantOfDateTime(DateTimeType)
   */
  public static void addDateTimePropertyFieldToDocument(
      Document document, String propertyName, DateTimeType propertyValue, Field.Store fieldStore) {

    String fieldPrefix = NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR;

    long startInstant = DateUtil.getFirstInstantOfDateTime(propertyValue);
    long endInstant = DateUtil.getLastInstantOfDateTime(propertyValue);

    document.add(new LongPoint(fieldPrefix + SUFFIX_DATE_START, startInstant));
    // TODO should we always store this? Then we should remove the parameter
    if (fieldStore == Field.Store.YES) {
      document.add(new StoredField(fieldPrefix + SUFFIX_DATE_START, startInstant));
    }

    document.add(new LongPoint(fieldPrefix + SUFFIX_DATE_END, endInstant));
    // TODO should we always store this? Then we should remove the parameter
    if (fieldStore == Field.Store.YES) {
      document.add(new StoredField(fieldPrefix + SUFFIX_DATE_END, endInstant));
    }
  }

  // --------------------------------------------------------------------------------
  // 'token' Parameter Searches

  /**
   * Adds an identifier token parameter field to the document for exact token matching.
   *
   * <p>Fields created: {@code {paramName}_tokenexact} and optionally {@code {paramName}_sort}
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the search parameter name
   * @param paramValue the identifier value
   * @param addSortField whether to also create a sort-capable doc values field
   */
  public static void addIdTokenParamFieldToDocument(
      Document document, String paramName, String paramValue, boolean addSortField) {

    document.add(
        new StringField(paramName + SEPARATOR + SUFFIX_TOKEN_EXACT, paramValue, Field.Store.YES));

    // Es gibt einzelne Fälle, in denen wir das Sortierfeld nicht benötigen bzw. nicht nutzen können
    if (addSortField) {
      document.add(
          new SortedDocValuesField(paramName + SEPARATOR + SUFFIX_SORT, new BytesRef(paramValue)));
    }
  }

  /**
   * Adds a boolean token parameter field to the document, including both a plain value and an
   * extended FHIR special-values system representation.
   *
   * <p>Fields created: {@code {paramName}_lowercaseexact} with values {@code "true"/"false"} and
   * {@code "http://hl7.org/fhir/special-values|true/false"}
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the search parameter name
   * @param active the boolean value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addBooleanTokenParamFieldToDocument(
      Document document, String paramName, Boolean active, Field.Store fieldStore) {

    String fieldName = paramName + SEPARATOR + SUFFIX_LOWERCASE_EXACT;

    String fieldValue = String.valueOf(active.booleanValue());
    String extendedFieldValue = "http://hl7.org/fhir/special-values|" + active.booleanValue();

    document.add(new StringField(fieldName, fieldValue, fieldStore));
    document.add(new StringField(fieldName, extendedFieldValue, fieldStore));
  }

  /**
   * Adds a code token parameter field to the document with both code-only and system|code
   * representations, plus a sort field.
   *
   * <p>Fields created: {@code {paramName}_lowercaseexact} and {@code {paramName}_sort}
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the search parameter name
   * @param system the code system URI
   * @param code the code value
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodeTokenParamFieldToDocument(
      Document document, String paramName, String system, String code, Field.Store fieldStore) {

    String fieldName = paramName + SEPARATOR + SUFFIX_LOWERCASE_EXACT;

    String fieldValue = code;
    String extendedFieldValue = system + "|" + code;

    document.add(new StringField(fieldName, fieldValue, fieldStore));
    document.add(new StringField(fieldName, extendedFieldValue, fieldStore));
    document.add(new SortedDocValuesField(paramName + SEPARATOR + SUFFIX_SORT, new BytesRef(code)));
  }

  /**
   * Adds a Coding token parameter field to the document with multiple lowercase search
   * representations for flexible token matching (system|, code, |code, system|code).
   *
   * <p>Fields created: {@code {paramName}_lowercaseexact} with various system/code combinations
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the search parameter name
   * @param system the coding system URI (may be {@code null})
   * @param code the coding code value (may be {@code null})
   * @param fieldStore whether to store the field value for retrieval
   */
  public static void addCodingTokenParamFieldToDocument(
      Document document, String paramName, String system, String code, Field.Store fieldStore) {

    // TODO we will go for case insensitive search, therefore everything will be converted to
    // lowercase
    String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

    String fieldValue = "";

    if (system != null) {
      fieldValue = system.toLowerCase() + "|";
      logField(fieldName, fieldValue);
      document.add(new StringField(fieldName, fieldValue, fieldStore));
    }

    if (code != null) {
      fieldValue = code.toLowerCase();
      logField(fieldName, fieldValue);
      document.add(new StringField(fieldName, fieldValue, fieldStore));
    }

    if (system == null && code != null) {
      fieldValue = "|" + code.toLowerCase();
      logField(fieldName, fieldValue);
      document.add(new StringField(fieldName, fieldValue, fieldStore));
    }

    if (system != null && code != null) {
      fieldValue = system.toLowerCase() + "|" + code.toLowerCase();
      logField(fieldName, fieldValue);
      document.add(new StringField(fieldName, fieldValue, fieldStore));
    }
  }

  /** Logs a field name/value pair at debug level. */
  private static void logField(String fieldName, String fieldValue) {
    log.debug("{} = {}", fieldName, fieldValue);
  }
}
