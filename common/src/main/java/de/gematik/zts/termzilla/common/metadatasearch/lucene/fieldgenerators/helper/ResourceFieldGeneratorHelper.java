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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_DATE_INSTANT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_DATE_START;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_LOWERCASE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_MISSING;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_REFERENCE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_STRING_STANDARD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URI_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URI_SCHEME_LOWERCASE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URL_HOST_LOWERCASE_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URL_PATH_BELOW_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URL_PATH_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URL_PORT_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_URL_QUERY_EXACT;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import de.gematik.zts.termzilla.common.tools.UriHelper;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Period;

/**
 * Utility class providing static helper methods for indexing FHIR resource elements into Apache
 * Lucene documents.
 *
 * <p>This class serves as the core indexing layer for FHIR search parameters, converting FHIR
 * resource elements into appropriately-typed Lucene fields to enable FHIR-compliant search
 * operations as defined in the FHIR R4 specification.
 *
 * <p><b>Supported FHIR Search Parameter Types:</b>
 *
 * <ul>
 *   <li><b>date</b> - Date/time parameters (instant, datetime, period)
 *   <li><b>string</b> - Text parameters with multiple search variants
 *   <li><b>token</b> - Coded values, identifiers, booleans
 *   <li><b>reference</b> - Canonical references
 *   <li><b>uri</b> - URI/URL parameters with component-level indexing
 * </ul>
 *
 * <p><b>Not yet implemented:</b> number, quantity, composite, special
 *
 * <p><b>Design Principles:</b>
 *
 * <ul>
 *   <li><b>Multi-variant indexing:</b> Each parameter is indexed in multiple ways to support
 *       different FHIR search modifiers (e.g., :exact, :contains, :below)
 *   <li><b>Missing field tracking:</b> Special fields track presence/absence to support :missing
 *       modifier
 *   <li><b>Case-insensitive token search:</b> Token values are lowercased for flexible matching
 *   <li><b>Field naming convention:</b> {paramName}_{SUFFIX} pattern for all generated fields
 * </ul>
 *
 * @see <a href="https://hl7.org/fhir/R4/search.html">FHIR R4 Search Specification</a>
 * @see org.apache.lucene.document.Document
 */
@Slf4j
public class ResourceFieldGeneratorHelper {

  private ResourceFieldGeneratorHelper() {
    // Utility class - prevent instantiation
  }

  /**
   * Stores the complete serialized FHIR resource in the Lucene document.
   *
   * <p>This allows retrieval of the original FHIR resource from the index without needing to access
   * an external data store.
   *
   * @param document the Lucene document to add the field to
   * @param resource the serialized FHIR resource as byte array (typically JSON or XML)
   */
  public static void addStoredFieldToDocument(Document document, byte[] resource) {
    document.add(new StoredField("_content", resource));
  }

  /**
   * Adds a field indicating whether a specific parameter is present or absent in the resource.
   *
   * <p>This enables FHIR's <code>:missing</code> search modifier, allowing queries like: <code>
   * ?name:missing=true</code> (find resources without a name) or <code>?description:missing=false
   * </code> (find resources with a description).
   *
   * <p>Field created: <code>{paramName}_missing</code> with value "true" or "false"
   *
   * @param document the Lucene document to add the field to
   * @param paramName the FHIR search parameter name (e.g., "name", "description")
   * @param isMissing true if the field is absent in the resource, false if present
   * @see <a href="https://hl7.org/fhir/R4/search.html#missing">FHIR :missing modifier</a>
   */
  public static void addInformationMissingFieldToDocument(
      Document document, String paramName, boolean isMissing) {
    if (isMissing) {
      document.add(new StringField(paramName + SEPARATOR + SUFFIX_MISSING, "true", Field.Store.NO));
    } else {
      document.add(
          new StringField(paramName + SEPARATOR + SUFFIX_MISSING, "false", Field.Store.NO));
    }
  }

  // ================================================================================
  // FHIR Search Parameter Type Implementations
  // ================================================================================

  // --------------------------------------------------------------------------------
  // 'number' Parameter Searches
  // --------------------------------------------------------------------------------

  // --------------------------------------------------------------------------------
  // 'date' Parameter Searches
  // --------------------------------------------------------------------------------

  /**
   * Indexes a FHIR instant (precise point in time) as a date parameter.
   *
   * <p>An instant represents an exact timestamp with millisecond precision. This method creates
   * three fields to enable various date search operations:
   *
   * <ul>
   *   <li><code>{paramName}_date_instant</code> - The exact timestamp
   *   <li><code>{paramName}_date_period_start</code> - Start boundary (same as instant)
   *   <li><code>{paramName}_date_period_end</code> - End boundary (same as instant)
   * </ul>
   *
   * <p>All timestamps are stored as LongPoint values (milliseconds since epoch) to enable efficient
   * range queries.
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "_lastUpdated")
   * @param paramValue the FHIR InstantType value to index
   * @see org.hl7.fhir.r4.model.InstantType
   */
  public static void addInstantDateParamFieldToDocument(
      Document document, String paramName, InstantType paramValue) {
    document.add(
        new LongPoint(
            paramName + SEPARATOR + SUFFIX_DATE_INSTANT, paramValue.getValue().getTime()));

    document.add(
        new LongPoint(paramName + SEPARATOR + SUFFIX_DATE_START, paramValue.getValue().getTime()));

    document.add(
        new LongPoint(paramName + SEPARATOR + SUFFIX_DATE_END, paramValue.getValue().getTime()));
  }

  /**
   * Indexes a FHIR datetime with variable precision as a date parameter.
   *
   * <p>FHIR DateTimeType supports multiple precision levels (year, year-month, date, datetime).
   * This method expands the datetime into an implicit period representing its precision:
   *
   * <ul>
   *   <li><b>2024</b> → start: 2024-01-01 00:00:00.000, end: 2024-12-31 23:59:59.999
   *   <li><b>2024-03</b> → start: 2024-03-01 00:00:00.000, end: 2024-03-31 23:59:59.999
   *   <li><b>2024-03-15</b> → start: 2024-03-15 00:00:00.000, end: 2024-03-15 23:59:59.999
   * </ul>
   *
   * <p>This expansion ensures correct range query behavior for searches like <code>?date=2024-03
   * </code> (matches all dates in March 2024).
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_date_period_start</code> - First instant of the datetime period
   *   <li><code>{paramName}_date_period_end</code> - Last instant of the datetime period
   *   <li><code>{paramName}_sort</code> - Sortable value (uses start instant)
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "date")
   * @param paramValue the FHIR DateTimeType value with variable precision
   * @see DateUtil#getFirstInstantOfDateTime(DateTimeType)
   * @see DateUtil#getLastInstantOfDateTime(DateTimeType)
   * @see org.hl7.fhir.r4.model.DateTimeType
   */
  public static void addDateTimeDateParamFieldToDocument(
      Document document, String paramName, DateTimeType paramValue) {

    document.add(
        new LongPoint(
            paramName + SEPARATOR + SUFFIX_DATE_START,
            DateUtil.getFirstInstantOfDateTime(paramValue)));

    document.add(
        new LongPoint(
            paramName + SEPARATOR + SUFFIX_DATE_END,
            DateUtil.getLastInstantOfDateTime(paramValue)));

    document.add(
        new NumericDocValuesField(
            paramName + SEPARATOR + SUFFIX_SORT, DateUtil.getFirstInstantOfDateTime(paramValue)));
  }

  /**
   * Indexes a FHIR period (time range with start and end) as a date parameter.
   *
   * <p>A Period represents an interval of time with optional start and end boundaries. Missing
   * boundaries are represented as unbounded:
   *
   * <ul>
   *   <li><b>No start</b> → uses Long.MIN_VALUE (matches "any time before end")
   *   <li><b>No end</b> → uses Long.MAX_VALUE (matches "any time after start")
   * </ul>
   *
   * <p>This enables queries for ongoing periods or historical periods without defined start dates.
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_date_period_start</code> - Period start (or Long.MIN_VALUE)
   *   <li><code>{paramName}_date_period_end</code> - Period end (or Long.MAX_VALUE)
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name
   * @param paramValue the FHIR Period value to index
   * @see org.hl7.fhir.r4.model.Period
   */
  public static void addPeriodDateParamFieldToDocument(
      Document document, String paramName, Period paramValue) {

    if (paramValue.getStart() != null) {
      document.add(
          new LongPoint(
              paramName + SEPARATOR + SUFFIX_DATE_START,
              DateUtil.getFirstInstantOfDateTime(paramValue.getStartElement())));
    } else {
      document.add(new LongPoint(paramName + SEPARATOR + SUFFIX_DATE_START, Long.MIN_VALUE));
    }

    if (paramValue.getEnd() != null) {
      document.add(
          new LongPoint(
              paramName + SEPARATOR + SUFFIX_DATE_END,
              DateUtil.getLastInstantOfDateTime(paramValue.getEndElement())));
    } else {
      document.add(new LongPoint(paramName + SEPARATOR + SUFFIX_DATE_END, Long.MAX_VALUE));
    }
  }

  // --------------------------------------------------------------------------------
  // 'string' Parameter Searches
  // --------------------------------------------------------------------------------

  /**
   * Indexes a text string with multiple search variants to support different FHIR search modifiers.
   *
   * <p>String parameters are indexed in four different ways to enable flexible text searching:
   *
   * <ul>
   *   <li><b>exact</b> - Case-sensitive exact match (StringField, non-tokenized)
   *   <li><b>contains</b> - Partial substring matching (TextField with tokenization)
   *   <li><b>standard</b> - Standard tokenized full-text search (TextField)
   *   <li><b>sort</b> - Byte-sorted field for alphabetical sorting (SortedDocValuesField)
   * </ul>
   *
   * <p>FHIR search modifier support:
   *
   * <ul>
   *   <li><code>?name=SNOMED</code> → uses standard field
   *   <li><code>?name:exact=SNOMED CT</code> → uses exact field
   *   <li><code>?name:contains=SNOMED</code> → uses contains field
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "name", "title", "description")
   * @param paramValue the text value to index
   * @param fieldStore whether to store the original value (YES) or only index it (NO)
   * @see <a href="https://hl7.org/fhir/R4/search.html#string">FHIR string search</a>
   */
  public static void addStringParamFieldToDocument(
      Document document, String paramName, String paramValue, Field.Store fieldStore) {

    document.add(
        new StringField(paramName + SEPARATOR + SUFFIX_STRING_EXACT, paramValue, fieldStore));
    document.add(
        new TextField(paramName + SEPARATOR + SUFFIX_STRING_CONTAINS, paramValue, fieldStore));
    document.add(
        new TextField(paramName + SEPARATOR + SUFFIX_STRING_STANDARD, paramValue, fieldStore));
    document.add(
        new SortedDocValuesField(paramName + SEPARATOR + SUFFIX_SORT, new BytesRef(paramValue)));
  }

  // --------------------------------------------------------------------------------
  // 'token' Parameter Searches
  // --------------------------------------------------------------------------------

  /**
   * Indexes a simple identifier token (typically used for resource IDs).
   *
   * <p>This method creates an exact-match token field and optionally a sort field. The sort field
   * should be omitted when the parameter can have multiple values (Lucene's SortedDocValuesField
   * allows only one value per document).
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_tokenexact</code> - Exact token value (always stored)
   *   <li><code>{paramName}_sort</code> - Sort field (optional)
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "_id")
   * @param paramValue the identifier value
   * @param addSortField whether to add a sort field (false for multi-valued parameters)
   */
  public static void addIdTokenParamFieldToDocument(
      Document document, String paramName, String paramValue, boolean addSortField) {

    document.add(
        new StringField(paramName + SEPARATOR + SUFFIX_TOKEN_EXACT, paramValue, Field.Store.YES));

    // There are cases where we don't need or cannot use a sort field (e.g., multi-valued fields)
    if (addSortField) {
      document.add(
          new SortedDocValuesField(paramName + SEPARATOR + SUFFIX_SORT, new BytesRef(paramValue)));
    }
  }

  /**
   * Indexes a boolean value as a token parameter.
   *
   * <p>Boolean tokens are indexed in two formats to support searches with or without the FHIR
   * special-values system:
   *
   * <ul>
   *   <li><code>"true"</code> or <code>"false"</code> - Simple boolean value
   *   <li><code>"http://hl7.org/fhir/special-values|true"</code> - Full system|code format
   * </ul>
   *
   * <p>This allows queries like:
   *
   * <ul>
   *   <li><code>?active=true</code>
   *   <li><code>?active=http://hl7.org/fhir/special-values|true</code>
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "active")
   * @param active the boolean value to index
   * @param fieldStore whether to store the original value
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
   * Indexes a code from a code system as a token parameter.
   *
   * <p>Codes are indexed in two formats to enable flexible searching:
   *
   * <ul>
   *   <li><b>code only</b> - Allows searching without specifying the system
   *   <li><b>system|code</b> - Allows precise searching with the coding system
   * </ul>
   *
   * <p>Example: For code "active" from system "http://hl7.org/fhir/publication-status":
   *
   * <ul>
   *   <li><code>?status=active</code> → matches code-only field
   *   <li><code>?status=http://hl7.org/fhir/publication-status|active</code> → matches system|code
   *       field
   * </ul>
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_lowercaseexact</code> - Both code and system|code variants
   *   <li><code>{paramName}_sort</code> - Sortable field (uses code value)
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "status", "content-mode")
   * @param system the code system URI
   * @param code the code value
   * @param fieldStore whether to store the original value
   * @see <a href="https://hl7.org/fhir/R4/search.html#token">FHIR token search</a>
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
   * Indexes a FHIR Identifier (system + value pair) as a token parameter.
   *
   * <p>Identifiers are complex structures with a system (namespace) and value. To support all valid
   * FHIR identifier search patterns, this method creates multiple field variants:
   *
   * <ul>
   *   <li><b>system|</b> - Matches all identifiers from a specific system
   *   <li><b>value</b> - Matches identifiers by value only (any system)
   *   <li><b>|value</b> - Matches identifiers with empty system and specific value
   *   <li><b>system|value</b> - Matches exact system and value combination
   * </ul>
   *
   * <p>All values are converted to lowercase for case-insensitive searching.
   *
   * <p>Example: Identifier with system="http://example.org/ids" and value="ABC123" supports:
   *
   * <ul>
   *   <li><code>?identifier=ABC123</code> → matches value field
   *   <li><code>?identifier=http://example.org/ids|</code> → matches all IDs from that system
   *   <li><code>?identifier=http://example.org/ids|ABC123</code> → matches exact identifier
   *   <li><code>?identifier=|ABC123</code> → matches only if system is empty
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "identifier")
   * @param hasElementInResource whether the identifier element exists in the resource (currently
   *     unused)
   * @param paramValueSystem the identifier system (namespace), may be null
   * @param paramValueValue the identifier value, may be null
   * @param fieldStore whether to store the original value
   * @see <a href="https://hl7.org/fhir/R4/datatypes.html#Identifier">FHIR Identifier datatype</a>
   */
  public static void addIdentifierTokenParamFieldToDocument(
      Document document,
      String paramName,
      @SuppressWarnings("java:S1172") boolean hasElementInResource,
      String paramValueSystem,
      String paramValueValue,
      Field.Store fieldStore) {

    // Case-insensitive search: all values are converted to lowercase
    String fieldName = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;
    String fieldValue = "";

    if (paramValueSystem != null) {
      fieldValue = paramValueSystem.toLowerCase() + "|";
      document.add(new StringField(fieldName, fieldValue, fieldStore));
      logField(fieldName, fieldValue);
    }

    if (paramValueValue != null) {
      fieldValue = paramValueValue.toLowerCase();
      document.add(new StringField(fieldName, fieldValue, fieldStore));
      logField(fieldName, fieldValue);
    }

    if (paramValueSystem == null && paramValueValue != null) {
      fieldValue = "|" + paramValueValue.toLowerCase();
      document.add(new StringField(fieldName, fieldValue, fieldStore));
      logField(fieldName, fieldValue);
    }

    if (paramValueSystem != null && paramValueValue != null) {
      fieldValue = paramValueSystem.toLowerCase() + "|" + paramValueValue.toLowerCase();
      document.add(new StringField(fieldName, fieldValue, fieldStore));
      logField(fieldName, fieldValue);
    }
  }

  /**
   * Indexes a FHIR Coding as a token parameter.
   *
   * <p>A Coding is a reference to a code defined in a terminology. This method creates multiple
   * field variants to support flexible searching patterns, similar to Identifier indexing.
   *
   * <p>Field variants created (all lowercase):
   *
   * <ul>
   *   <li><b>system|</b> - All codes from this system
   *   <li><b>code</b> - Code value only
   *   <li><b>|code</b> - Code with empty system
   *   <li><b>system|code</b> - Exact system and code combination
   * </ul>
   *
   * <p>Example: Coding with system="http://loinc.org" and code="12345-6" enables:
   *
   * <ul>
   *   <li><code>?code=12345-6</code> → matches code-only field
   *   <li><code>?code=http://loinc.org|</code> → matches all LOINC codes
   *   <li><code>?code=http://loinc.org|12345-6</code> → matches exact coding
   * </ul>
   *
   * <p><b>Known Limitation:</b> The <code>display</code> parameter is currently not indexed for
   * text search.
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name
   * @param system the coding system URI, may be null
   * @param code the code value, may be null
   * @param display the human-readable display text (currently not indexed)
   * @param fieldStore whether to store the original value
   * @see <a href="https://hl7.org/fhir/R4/datatypes.html#Coding">FHIR Coding datatype</a>
   */
  public static void addCodingTokenParamFieldToDocument(
      Document document,
      String paramName,
      String system,
      String code,
      @SuppressWarnings("java:S1172") String display,
      Field.Store fieldStore) {

    // Case-insensitive search: all values are converted to lowercase
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

  // --------------------------------------------------------------------------------
  // 'reference' Parameter Searches
  // --------------------------------------------------------------------------------
  // Note: If the field should be sortable, it must occur only once per document

  /**
   * Indexes a canonical reference (canonical URL) as a reference parameter.
   *
   * <p>Canonical references are used to refer to definitional resources like CodeSystem, ValueSet,
   * StructureDefinition, etc. They follow the pattern: <code>url[|version]</code>
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_referenceexact</code> - Exact canonical URL (with optional version)
   *   <li><code>{paramName}_sort</code> - Sort field (optional, only if isSortable=true)
   * </ul>
   *
   * <p><b>Important:</b> The sort field can only be created if the parameter occurs at most once
   * per document. Multi-valued reference fields cannot be sorted due to Lucene's
   * SortedDocValuesField limitation.
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "url", "system", "valueset")
   * @param paramValue the canonical URL (may include |version suffix)
   * @param fieldStore whether to store the original value
   * @param isSortable whether to create a sort field (must be false for multi-valued fields)
   * @see <a href="https://hl7.org/fhir/R4/references.html#canonical">FHIR Canonical References</a>
   */
  public static void addCanonicalReferenceParamFieldToDocument(
      Document document,
      String paramName,
      String paramValue,
      Field.Store fieldStore,
      boolean isSortable) {

    document.add(
        new StringField(paramName + SEPARATOR + SUFFIX_REFERENCE_EXACT, paramValue, fieldStore));

    if (isSortable) {
      document.add(
          new SortedDocValuesField(paramName + SEPARATOR + SUFFIX_SORT, new BytesRef(paramValue)));
    }
  }

  // --------------------------------------------------------------------------------
  // 'composite' Parameter Searches
  // --------------------------------------------------------------------------------

  // --------------------------------------------------------------------------------
  // 'quantity' Parameter Searches
  // --------------------------------------------------------------------------------

  // --------------------------------------------------------------------------------
  // 'uri' Parameter Searches
  // --------------------------------------------------------------------------------

  /**
   * Indexes a URI parameter with component-level breakdown for flexible searching.
   *
   * <p>This is the main entry point for URI indexing. It validates the URI, extracts the scheme,
   * and delegates to specialized handlers based on the URI type.
   *
   * <p><b>Supported URI schemes:</b>
   *
   * <ul>
   *   <li><b>http/https</b> - HTTP(S) URLs with full component breakdown (host, path, port, query)
   *   <li><b>urn</b> - URN identifiers (e.g., urn:oid:2.16.840.1.113883.6.96)
   * </ul>
   *
   * <p><b>Validation and Error Handling:</b>
   *
   * <ul>
   *   <li>Malformed URIs containing pipe symbols (|) are sanitized by removing everything after the
   *       pipe
   *   <li>Missing URI scheme throws IllegalArgumentException
   *   <li>Unsupported URI schemes throw IllegalArgumentException
   * </ul>
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_urischemelowercaseexact</code> - URI scheme (http, https, urn)
   *   <li>Additional fields depending on scheme (see {@link #addUrlParamFieldToDocument} or {@link
   *       #addUrnParamFieldToDocument})
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name (e.g., "url", "system")
   * @param paramValue the URI to index
   * @param fieldStore whether to store the original value
   * @throws IllegalArgumentException if URI is invalid or uses unsupported scheme
   * @see <a href="https://hl7.org/fhir/R4/search.html#uri">FHIR URI search</a>
   */
  public static void addUriParamFieldToDocument(
      Document document, String paramName, String paramValue, Field.Store fieldStore) {

    // Defensive handling: Some CodeSystems have malformed URLs containing pipe symbols
    // This breaks indexing, so we sanitize by removing everything after the pipe
    if (paramValue.contains("|")) {
      log.warn(
          "URI is malformed (contains '|' symbol): '{}' -> '|.*$' will be removed from uri for indexing",
          paramValue);
      paramValue = paramValue.replaceAll("\\|.*$", "");
      log.warn(paramValue);
    }

    try {
      URI.create(paramValue);
    } catch (IllegalArgumentException _) {
      log.warn("URI is malformed: '{}'", paramValue);
    }

    URI uri = URI.create(paramValue);

    if (uri.getScheme() == null) {
      throw new IllegalArgumentException("URI scheme is missing: " + paramValue);
    } else {
      // Index URI scheme (e.g., urn, http, https) as lowercase
      document.add(
          new StringField(
              paramName + SEPARATOR + SUFFIX_URI_SCHEME_LOWERCASE_EXACT,
              uri.getScheme().toLowerCase(),
              fieldStore));

      // Route to specialized handler based on scheme
      // Currently supported: http, https, and urn(:oid)
      if (uri.getScheme().toLowerCase().contentEquals("http")
          || uri.getScheme().toLowerCase().contentEquals("https")) {
        // Process standard HTTP/HTTPS URL - this covers the majority of cases
        addUrlParamFieldToDocument(document, paramName, paramValue, fieldStore);
      } else if (uri.getScheme().contentEquals("urn")) {
        // Process URN with OID
        addUrnParamFieldToDocument(document, paramName, paramValue, fieldStore);
      } else {
        throw new IllegalArgumentException("URI scheme not supported: " + uri.toString());
      }
    }
  }

  /**
   * Indexes an HTTP/HTTPS URL with detailed component breakdown for flexible searching.
   *
   * <p>This private method performs comprehensive URL indexing by decomposing the URL into its
   * components and creating specialized search fields for each part. This enables granular search
   * capabilities and supports FHIR's <code>:below</code> modifier.
   *
   * <p><b>URL Components Indexed:</b>
   *
   * <ul>
   *   <li><b>Host</b> - Domain name (lowercase, e.g., "example.org")
   *   <li><b>Path</b> - URL path (case-sensitive, e.g., "/fhir/CodeSystem/snomed")
   *   <li><b>Port</b> - Port number if non-default (e.g., "8080")
   *   <li><b>Query</b> - Query string if present (case-sensitive)
   *   <li><b>Complete URI</b> - Full normalized URL
   *   <li><b>Path hierarchy</b> - All parent paths for :below modifier support
   * </ul>
   *
   * <p><b>Path Hierarchy Example:</b>
   *
   * <p>For URL <code>http://example.org:8080/fhir/CodeSystem/snomed</code>, the following hierarchy
   * fields are created to support <code>?url:below=http://example.org/fhir</code>:
   *
   * <pre>
   * http://example.org:8080
   * http://example.org:8080/
   * http://example.org:8080/fhir
   * http://example.org:8080/fhir/
   * http://example.org:8080/fhir/CodeSystem
   * http://example.org:8080/fhir/CodeSystem/
   * </pre>
   *
   * <p><b>Fields created:</b>
   *
   * <ul>
   *   <li><code>{paramName}_urlhostlowercaseexact</code> - Hostname (lowercase)
   *   <li><code>{paramName}_urlpathexact</code> - Path (case-sensitive)
   *   <li><code>{paramName}_urlportexact</code> - Port (if present)
   *   <li><code>{paramName}_urlqueryexact</code> - Query string (if present)
   *   <li><code>{paramName}_uriexact</code> - Complete URL
   *   <li><code>{paramName}_sort</code> - Sortable field
   *   <li><code>{paramName}_urlpathbelowexact</code> - Path hierarchy components (multiple values)
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name
   * @param paramValue the URL to index
   * @param fieldStore whether to store the original value
   * @throws IllegalArgumentException if URL is malformed
   * @see UriHelper#normalizeUrl(String)
   * @see <a href="https://hl7.org/fhir/R4/search.html#uri">FHIR :below modifier</a>
   */
  private static void addUrlParamFieldToDocument(
      Document document, String paramName, String paramValue, Field.Store fieldStore) {

    URL url = parseAndNormalizeUrl(paramValue);

    indexHostComponent(document, paramName, url, fieldStore);
    indexPathComponent(document, paramName, url, fieldStore);
    indexPortComponent(document, paramName, url, fieldStore);
    indexQueryComponent(document, paramName, url, fieldStore);
    indexCompleteUri(document, paramName, url.toString(), fieldStore);
    indexPathHierarchyForBelowModifier(document, paramName, url, fieldStore);
  }

  /**
   * Parses and normalizes the URL string.
   *
   * @param paramValue the URL string to parse
   * @return parsed URL object
   * @throws IllegalArgumentException if URL is malformed
   */
  private static URL parseAndNormalizeUrl(String paramValue) {
    try {
      return new URI(UriHelper.normalizeUrl(paramValue)).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalArgumentException("URL is malformed: " + paramValue, e);
    }
  }

  /** Indexes the hostname component (lowercase, case-insensitive). */
  private static void indexHostComponent(
      Document document, String paramName, URL url, Field.Store fieldStore) {

    if (StringUtils.isNotEmpty(url.getHost())) {
      String fieldName = paramName + SEPARATOR + SUFFIX_URL_HOST_LOWERCASE_EXACT;
      document.add(new StringField(fieldName, url.getHost().toLowerCase(), fieldStore));
    }
  }

  /** Indexes the path component (case-sensitive). */
  private static void indexPathComponent(
      Document document, String paramName, URL url, Field.Store fieldStore) {

    if (StringUtils.isNotEmpty(url.getPath())) {
      String fieldName = paramName + SEPARATOR + SUFFIX_URL_PATH_EXACT;
      document.add(new StringField(fieldName, url.getPath(), fieldStore));
    }
  }

  /** Indexes the port component if non-default. */
  private static void indexPortComponent(
      Document document, String paramName, URL url, Field.Store fieldStore) {

    if (url.getPort() != -1) {
      String fieldName = paramName + SEPARATOR + SUFFIX_URL_PORT_EXACT;
      document.add(new StringField(fieldName, String.valueOf(url.getPort()), fieldStore));
    }
  }

  /**
   * Indexes the query string component (case-sensitive).
   */
  private static void indexQueryComponent(
      Document document, String paramName, URL url, Field.Store fieldStore) {

    if (StringUtils.isNotEmpty(url.getQuery())) {
      String fieldName = paramName + SEPARATOR + SUFFIX_URL_QUERY_EXACT;
      document.add(new StringField(fieldName, url.getQuery(), fieldStore));
    }
  }

  /**
   * Indexes the complete URI and sortable field.
   * should be used here. Previous implementation used uri.toString() but caused incorrect indexing.
   */
  private static void indexCompleteUri(
      Document document, String paramName, String paramValue, Field.Store fieldStore) {

    String uriFieldName = paramName + SEPARATOR + SUFFIX_URI_EXACT;
    document.add(new StringField(uriFieldName, paramValue, fieldStore));

    String sortFieldName = paramName + SEPARATOR + SUFFIX_SORT;
    document.add(new SortedDocValuesField(sortFieldName, new BytesRef(paramValue)));
  }

  /**
   * Indexes path hierarchy for :below modifier support.
   *
   * <p>Creates hierarchical path entries enabling searches for all resources under a given path.
   * For example, "/fhir/CodeSystem/snomed" creates entries for "/fhir" and "/fhir/CodeSystem".
   */
  private static void indexPathHierarchyForBelowModifier(
      Document document, String paramName, URL url, Field.Store fieldStore) {

    if (StringUtils.isEmpty(url.getPath())) {
      return;
    }

    List<String> hierarchyPaths = buildPathHierarchy(url);
    String fieldName = paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT;

    for (String hierarchyPath : hierarchyPaths) {
      document.add(new StringField(fieldName, hierarchyPath, fieldStore));
    }
  }

  /**
   * Builds the path hierarchy list for :below modifier functionality.
   *
   * @param url the URL to decompose
   * @return list of hierarchical path strings
   */
  private static List<String> buildPathHierarchy(URL url) {
    List<String> hierarchyPaths = new ArrayList<>();
    String[] pathParts = url.getPath().split("/");

    if (pathParts.length <= 1) {
      return hierarchyPaths;
    }

    // Build base URI (protocol + host + port)
    StringBuilder baseUri =
        new StringBuilder().append(url.getProtocol()).append("://").append(url.getHost());

    if (url.getPort() != -1) {
      baseUri.append(":").append(url.getPort());
    }

    // Remove last part since it's not part of the "below" hierarchy
    String[] hierarchyParts = Arrays.copyOfRange(pathParts, 0, pathParts.length - 1);
    StringBuilder currentPath = new StringBuilder(baseUri);

    for (String part : hierarchyParts) {
      currentPath.append(part);
      hierarchyPaths.add(currentPath.toString());

      currentPath.append("/");
      hierarchyPaths.add(currentPath.toString());
    }

    return hierarchyPaths;
  }

  /**
   * Indexes a URN (Uniform Resource Name) as a URI parameter.
   *
   * <p>URNs are persistent, location-independent identifiers following the pattern <code>
   * urn:nid:nss</code> where:
   *
   * <ul>
   *   <li><b>nid</b> - Namespace identifier (e.g., "oid", "uuid", "isbn")
   *   <li><b>nss</b> - Namespace-specific string (e.g., "2.16.840.1.113883.6.96" for SNOMED CT OID)
   * </ul>
   *
   * <p><b>Current Implementation:</b> This is a minimal implementation that only stores the
   * complete URN without component decomposition. Future enhancements could parse and index URN
   * components separately.
   *
   * <p>Example URNs:
   *
   * <ul>
   *   <li><code>urn:oid:2.16.840.1.113883.6.96</code> - SNOMED CT OID
   *   <li><code>urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6</code> - UUID
   *   <li><code>urn:ietf:rfc:3986</code> - IETF RFC reference
   * </ul>
   *
   * <p>Fields created:
   *
   * <ul>
   *   <li><code>{paramName}_uriexact</code> - Complete URN
   * </ul>
   *
   * @param document the Lucene document to add the fields to
   * @param paramName the FHIR search parameter name
   * @param paramValue the URN to index
   * @param fieldStore whether to store the original value
   * @see <a href="https://tools.ietf.org/html/rfc8141">RFC 8141 - URN Syntax</a>
   */
  private static void addUrnParamFieldToDocument(
      Document document, String paramName, String paramValue, Field.Store fieldStore) {

    document.add(new StringField(paramName + SEPARATOR + SUFFIX_URI_EXACT, paramValue, fieldStore));
  }

  // --------------------------------------------------------------------------------
  // 'special' Parameter Searches
  // --------------------------------------------------------------------------------

  private static void logField(String fieldName, String fieldValue) {
    log.debug("{} = {}", fieldName, fieldValue);
  }
}
