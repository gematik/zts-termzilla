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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_START;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

/**
 * Query generator for FHIR period date search parameters.
 *
 * <p>This class provides methods for building queries for FHIR period-type date parameters, which
 * represent a time interval with a start and end date. Period parameters are used for range-based
 * searches on FHIR resources that have temporal extent.
 *
 * <p><b>Supported FHIR date types:</b>
 *
 * <ul>
 *   <li>Period - A time range with start and end boundaries (e.g., effectivePeriod)
 * </ul>
 *
 * <p><b>Period Search Semantics:</b>
 *
 * <p>Period searches use interval overlap logic. A period matches a search date if:
 *
 * <ul>
 *   <li>The period's start is before or at the search date's end, AND
 *   <li>The period's end is after or at the search date's start
 * </ul>
 *
 * <p>This ensures that periods overlapping with the search range are found, regardless of whether
 * they fully contain, are contained by, or partially overlap the search range.
 *
 * <p><b>Supported search prefixes:</b>
 *
 * <ul>
 *   <li>{@code eq} (equals) - Matches periods overlapping the specified date range
 *   <li>{@code ne} (not equals) - Excludes periods overlapping the specified date range
 *   <li>{@code lt} (less than) - Matches periods ending before the specified date
 *   <li>{@code le} (less than or equals) - Matches periods ending before or at the specified date
 *   <li>{@code gt} (greater than) - Matches periods starting after the specified date
 *   <li>{@code ge} (greater than or equals) - Matches periods starting after or at the specified
 *       date
 *   <li>{@code :missing} - Search for presence/absence of the period parameter
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
 * ranges, same as instant dates:
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
 * <p>Period queries use Lucene's {@link LongPoint} for efficient range searches on two indexed
 * fields:
 *
 * <ul>
 *   <li>{@code paramName_date_start} - The period's start timestamp
 *   <li>{@code paramName_date_end} - The period's end timestamp
 * </ul>
 *
 * <p>The overlap logic is implemented as: {@code (start <= searchEnd) AND (end >= searchStart)} OR
 * {@code start IN [searchStart, searchEnd]} OR {@code end IN [searchStart, searchEnd]}
 *
 * <p><b>FHIR Examples:</b>
 *
 * <ul>
 *   <li>{@code ?effectivePeriod=2023-01-15} - Periods overlapping January 15, 2023
 *   <li>{@code ?effectivePeriod=gt2023} - Periods starting after 2023
 *   <li>{@code ?effectivePeriod=le2023-12-31} - Periods ending before or on Dec 31, 2023
 * </ul>
 *
 * <p><b>Note:</b> Timezone handling needs consideration as a single day might have more than 24
 * hours depending on daylight saving time transitions.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * PeriodParameterQueryGenerator generator = new PeriodParameterQueryGenerator();
 * Query query = generator.prepareQuery(paramData);
 * }</pre>
 *
 * @see AbstractDateParameterQueryGenerator
 * @see QueryCombinators
 * @see org.apache.lucene.document.LongPoint
 * @see InstantParameterQueryGenerator
 */
public final class PeriodParameterQueryGenerator extends AbstractDateParameterQueryGenerator {

  /**
   * Prepares a Lucene range query for period values using interval overlap logic.
   *
   * <p>This is the core method that creates Lucene {@link LongPoint} range queries for efficient
   * period overlap searches. The query matches periods that overlap with the search range
   * [from,to].
   *
   * <p>A period [periodStart, periodEnd] overlaps with search range [from, to] if:
   *
   * <ul>
   *   <li>The period starts within the search range: {@code periodStart IN [from, to]}, OR
   *   <li>The period ends within the search range: {@code periodEnd IN [from, to]}, OR
   *   <li>The period fully encompasses the search range: {@code (periodStart <= to) AND (periodEnd
   *       >= from)}
   * </ul>
   *
   * <p>The query targets two fields:
   *
   * <ul>
   *   <li>{@code paramName_date_start} - The period's start timestamp
   *   <li>{@code paramName_date_end} - The period's end timestamp
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param from the start of the search range (inclusive) in milliseconds since epoch
   * @param to the end of the search range (inclusive) in milliseconds since epoch
   * @return a Lucene query matching periods that overlap with the specified date range
   */
  @Override
  protected Query prepareLongRangeQuery(String paramName, long from, long to) {
    String startField = paramName + SEPARATOR + SUFFIX_DATE_START;
    String endField = paramName + SEPARATOR + SUFFIX_DATE_END;

    // Build the overlap query using OR of three conditions:
    // 1. Period start falls within search range
    // 2. Period end falls within search range
    // 3. Period fully encompasses search range (start before or at 'to' AND end after or at 'from')
    List<Query> orQueryList = new ArrayList<>();

    // Condition 1: Period start within search range
    orQueryList.add(LongPoint.newRangeQuery(startField, from, to));

    // Condition 2: Period end within search range
    orQueryList.add(LongPoint.newRangeQuery(endField, from, to));

    // Condition 3: Period encompasses search range
    List<Query> andQueryList = new ArrayList<>();
    andQueryList.add(LongPoint.newRangeQuery(startField, Long.MIN_VALUE, to));
    andQueryList.add(LongPoint.newRangeQuery(endField, from, Long.MAX_VALUE));
    orQueryList.add(QueryCombinators.prepareAndQuery(andQueryList));

    return QueryCombinators.prepareOrQuery(orQueryList);
  }
}
