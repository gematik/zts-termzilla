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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_INSTANT;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

/**
 * Query generator for FHIR instant date search parameters.
 *
 * <p>This class provides methods for building queries for FHIR instant-type date parameters, which
 * represent a single point in time with precision down to milliseconds. Instant parameters are used
 * for timestamp-based searches on FHIR resources.
 *
 * <p><b>Supported FHIR date types:</b>
 *
 * <ul>
 *   <li>Instant - A precise point in time (e.g., lastUpdated, date fields)
 * </ul>
 *
 * <p><b>Supported search prefixes:</b>
 *
 * <ul>
 *   <li>{@code eq} (equals) - Matches dates within the specified precision range
 *   <li>{@code ne} (not equals) - Excludes dates within the specified precision range
 *   <li>{@code lt} (less than) - Matches dates before the specified instant
 *   <li>{@code le} (less than or equals) - Matches dates before or at the specified instant
 *   <li>{@code gt} (greater than) - Matches dates after the specified instant
 *   <li>{@code ge} (greater than or equals) - Matches dates after or at the specified instant
 *   <li>{@code :missing} - Search for presence/absence of the date parameter
 * </ul>
 *
 * <p><b>Unsupported prefixes (throw {@link NotImplementedException}):</b>
 *
 * <ul>
 *   <li>{@code sa} (starts after) - Not yet implemented
 *   <li>{@code eb} (ends before) - Not yet implemented
 *   <li>{@code ap} (approximate) - Not yet implemented
 * </ul>
 *
 * <p><b>Date Precision Handling:</b>
 *
 * <p>The query generator handles different date precisions by converting them to millisecond
 * ranges:
 *
 * <ul>
 *   <li>YEAR - First to last millisecond of the year
 *   <li>MONTH - First to last millisecond of the month
 *   <li>DAY - First to last millisecond of the day
 *   <li>MINUTE - First to last millisecond of the minute
 *   <li>SECOND - First to last millisecond of the second
 *   <li>MILLI - Exact millisecond timestamp
 * </ul>
 *
 * <p><b>Query Structure:</b>
 *
 * <p>Date queries use Lucene's {@link LongPoint} for efficient range searches on the indexed field
 * {@code paramName_date_instant}.
 *
 * <p><b>FHIR Examples:</b>
 *
 * <ul>
 *   <li>{@code ?date=2023-01-15} - Resources with date on January 15, 2023
 *   <li>{@code ?date=gt2023} - Resources with dates after 2023
 *   <li>{@code ?date=le2023-12-31} - Resources with dates before or on Dec 31, 2023
 *   <li>{@code ?_lastUpdated=ge2024-01-01T00:00:00Z} - Resources updated since Jan 1, 2024
 * </ul>
 *
 * <p><b>Note:</b> Timezone handling needs consideration as a single day might have more than 24
 * hours depending on daylight saving time transitions.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * InstantParameterQueryGenerator generator = new InstantParameterQueryGenerator();
 * Query query = generator.prepareQuery(paramData);
 * }</pre>
 *
 * @see AbstractDateParameterQueryGenerator
 * @see org.apache.lucene.document.LongPoint
 */
public final class InstantParameterQueryGenerator extends AbstractDateParameterQueryGenerator {

  /**
   * Prepares a Lucene range query for instant date values stored as long millisecond timestamps.
   *
   * <p>This is the core method that creates Lucene {@link LongPoint} range queries for efficient
   * date range searches. All date queries ultimately use this method after calculating the
   * appropriate millisecond boundaries based on precision and comparison operators.
   *
   * <p>The query targets the field {@code paramName_date_instant} which stores dates as long values
   * representing milliseconds since epoch.
   *
   * @param paramName the FHIR search parameter name
   * @param from the start of the range (inclusive) in milliseconds since epoch
   * @param to the end of the range (inclusive) in milliseconds since epoch
   * @return a LongPoint range query for the specified date range
   */
  @Override
  protected Query prepareLongRangeQuery(String paramName, long from, long to) {
    String inField = paramName + SEPARATOR + SUFFIX_DATE_INSTANT;
    return LongPoint.newRangeQuery(inField, from, to);
  }
}
