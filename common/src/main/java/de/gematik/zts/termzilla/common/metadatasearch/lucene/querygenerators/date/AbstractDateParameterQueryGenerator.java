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

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * Abstract base class for generating Lucene queries for FHIR date search parameters.
 *
 * <p>This class provides common functionality for both instant and period date queries, including
 * parameter parsing, precision handling, and query routing. Subclasses must implement {@link
 * #prepareLongRangeQuery(String, long, long)} to define the specific query logic for their date
 * type (instant vs period).
 *
 * <p><b>Supported search prefixes:</b>
 *
 * <ul>
 *   <li>{@code eq} (equals) - Matches dates within the specified precision range
 *   <li>{@code ne} (not equals) - Excludes dates within the specified precision range
 *   <li>{@code lt} (less than) - Matches dates before the specified date
 *   <li>{@code le} (less than or equals) - Matches dates before or at the specified date
 *   <li>{@code gt} (greater than) - Matches dates after the specified date
 *   <li>{@code ge} (greater than or equals) - Matches dates after or at the specified date
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
 * <p><b>Note:</b> Timezone handling needs consideration as a single day might have more than 24
 * hours depending on daylight saving time transitions.
 *
 * @see InstantParameterQueryGenerator
 * @see PeriodParameterQueryGenerator
 * @see QueryCombinators
 * @see DateUtil
 * @see org.apache.lucene.document.LongPoint
 */
public abstract class AbstractDateParameterQueryGenerator {

  /**
   * Prepares a Lucene query for FHIR date search parameters from ParamData.
   *
   * <p>This method handles AND/OR combinations of date parameters. FHIR allows comma-separated
   * values (OR) and multiple parameter instances (AND):
   *
   * <ul>
   *   <li>{@code ?date=2023-01-01,2023-12-31} - OR combination (either date must match)
   *   <li>{@code ?date=ge2023-01-01&date=le2023-12-31} - AND combination (both conditions must
   *       match)
   * </ul>
   *
   * @param andListParamData the parameter data containing DateAndListParam
   * @return a Lucene query combining all date conditions with appropriate AND/OR logic
   * @throws NotImplementedException if an unsupported search prefix is encountered
   */
  public Query prepareQuery(ParamData andListParamData) throws NotImplementedException {
    DateAndListParam andListParam = (DateAndListParam) andListParamData.getParam();
    List<Query> andQueryList = new ArrayList<>();

    for (DateOrListParam orListParam : andListParam.getValuesAsQueryTokens()) {
      List<Query> orQueryList = new ArrayList<>();
      for (DateParam param : orListParam.getValuesAsQueryTokens()) {
        orQueryList.add(prepareQuery(andListParamData.getParamName(), param));
      }
      andQueryList.add(QueryCombinators.prepareOrQuery(orQueryList));
    }
    return QueryCombinators.prepareAndQuery(andQueryList);
  }

  /**
   * Prepares a Lucene query for a single FHIR date parameter.
   *
   * <p>This method routes the query generation based on the parameter's prefix:
   *
   * <ul>
   *   <li>No prefix or {@code eq} - Equality search within precision range
   *   <li>{@code ne} - Not equals (negation)
   *   <li>{@code lt} - Less than
   *   <li>{@code le} - Less than or equals
   *   <li>{@code gt} - Greater than
   *   <li>{@code ge} - Greater than or equals
   *   <li>{@code :missing} - Presence/absence search
   *   <li>Other prefixes - Throw NotImplementedException
   * </ul>
   *
   * @param paramName the FHIR search parameter name (e.g., "date", "_lastUpdated",
   *     "effectivePeriod")
   * @param param the date parameter containing the value, precision, and prefix
   * @return a Lucene query for the date parameter
   * @throws NotImplementedException if an unsupported prefix is used
   */
  public Query prepareQuery(String paramName, DateParam param) throws NotImplementedException {
    if (param.isEmpty() && param.getMissing() == null) {
      return MatchAllDocsQuery.INSTANCE;
    }

    if (param.getMissing() != null) {
      // :missing search modifier
      return QueryCombinators.prepareMissingModifierQuery(paramName, param);
    }

    ParamPrefixEnum prefix = param.getPrefix() != null ? param.getPrefix() : ParamPrefixEnum.EQUAL;

    return switch (prefix) {
      case EQUAL -> prepareEqualsQuery(paramName, param);
      case NOT_EQUAL -> prepareNotEqualsQuery(paramName, param);
      case LESSTHAN -> prepareLessThanQuery(paramName, param);
      case LESSTHAN_OR_EQUALS -> prepareLessThanOrEqualsQuery(paramName, param);
      case GREATERTHAN -> prepareGreaterThanQuery(paramName, param);
      case GREATERTHAN_OR_EQUALS -> prepareGreaterThanOrEqualsQuery(paramName, param);
      case STARTS_AFTER -> prepareStartsAfterQuery(paramName, param);
      case ENDS_BEFORE -> prepareEndsBeforeQuery(paramName, param);
      case APPROXIMATE -> prepareApproximateQuery(paramName, param);
      default ->
          throw new NotImplementedException(
              "Prefix " + prefix.getValue() + " not implemented for date parameters.");
    };
  }

  /**
   * Prepares the underlying Lucene range query for the specified date range.
   *
   * <p>This is the core method that subclasses must implement to define their specific query logic.
   * The method receives millisecond timestamps and must create the appropriate Lucene query for the
   * date type (instant vs period).
   *
   * <p>All date queries ultimately use this method after calculating the appropriate millisecond
   * boundaries based on precision and comparison operators.
   *
   * @param paramName the FHIR search parameter name
   * @param from the start of the range (inclusive) in milliseconds since epoch
   * @param to the end of the range (inclusive) in milliseconds since epoch
   * @return a Lucene query for the specified date range
   */
  protected abstract Query prepareLongRangeQuery(String paramName, long from, long to);

  /**
   * Calculates the date range boundaries based on the parameter's precision.
   *
   * <p>This method converts a FHIR date parameter with its precision into a millisecond range
   * [from, to] that represents the full extent of that precision level.
   *
   * <p><b>Examples:</b>
   *
   * <ul>
   *   <li>2023 (YEAR) → [2023-01-01T00:00:00.000, 2023-12-31T23:59:59.999]
   *   <li>2023-06 (MONTH) → [2023-06-01T00:00:00.000, 2023-06-30T23:59:59.999]
   *   <li>2023-06-15 (DAY) → [2023-06-15T00:00:00.000, 2023-06-15T23:59:59.999]
   * </ul>
   *
   * @param param the date parameter with value and precision
   * @return a DateRange object containing the from and to timestamps in milliseconds
   * @throws IllegalArgumentException if an unsupported precision is encountered
   */
  protected DateRange calculatePrecisionRange(DateParam param) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(param.getValue());

    // TODO think about time zone fun as a single day might have more than 24 hours

    long from;
    long to;

    switch (param.getPrecision()) {
      case YEAR:
        // First to last millisecond of the year
        from = DateUtil.getFirstInstantOfYear(calendar.get(Calendar.YEAR));
        to = DateUtil.getLastInstantOfYear(calendar.get(Calendar.YEAR));
        break;
      case MONTH:
        // First to last millisecond of the month
        from =
            DateUtil.getFirstInstantOfYearMonth(
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        to =
            DateUtil.getLastInstantOfYearMonth(
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
        break;
      case DAY:
        // First to last millisecond of the day
        from =
            DateUtil.getFirstInstantOfYearMonthDay(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        to =
            DateUtil.getLastInstantOfYearMonthDay(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        break;
      case MINUTE:
        // First to last millisecond of the minute
        from =
            DateUtil.getFirstInstantOfYearMonthDayHourMinute(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
        to =
            DateUtil.getLastInstantOfYearMonthDayHourMinute(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
        break;
      case SECOND:
        // First to last millisecond of the second
        from =
            DateUtil.getFirstInstantOfYearMonthDayHourMinuteSecond(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
        to =
            DateUtil.getLastInstantOfYearMonthDayHourMinuteSecond(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
        break;
      case MILLI:
        // Highest precision we can get, therefore from and to are equal
        from = param.getValue().getTime();
        to = param.getValue().getTime();
        break;
      default:
        throw new IllegalArgumentException("Unsupported precision: " + param.getPrecision());
    }

    return new DateRange(from, to);
  }

  /**
   * Prepares an equality query for dates with precision-based range matching.
   *
   * <p>Date equality in FHIR is precision-aware. A date is considered equal if it falls anywhere
   * within the precision range of the specified value:
   *
   * <ul>
   *   <li>Year precision: 2023 matches 2023-01-01T00:00:00.000 to 2023-12-31T23:59:59.999
   *   <li>Month precision: 2023-06 matches 2023-06-01T00:00:00.000 to 2023-06-30T23:59:59.999
   *   <li>Day precision: 2023-06-15 matches 2023-06-15T00:00:00.000 to 2023-06-15T23:59:59.999
   *   <li>Minute/Second precision: Similar range expansion to precision boundaries
   *   <li>Millisecond precision: Exact timestamp match
   * </ul>
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=2023-06} matches all resources with dates in June 2023
   *   <li>{@code ?effectivePeriod=2023-06} matches all periods overlapping June 2023
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a range query covering the precision range of the specified date
   */
  protected Query prepareEqualsQuery(String paramName, DateParam param) {
    DateRange range = calculatePrecisionRange(param);
    return prepareLongRangeQuery(paramName, range.from(), range.to());
  }

  /**
   * Prepares a not-equals query for dates (negation of equals).
   *
   * <p>Creates a boolean query that matches all documents EXCEPT those matching the equals query.
   * Implemented as: {@code MUST(MatchAllDocsQuery) AND MUST_NOT(equalsQuery)}
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=ne2023-06} matches all resources with dates NOT in June 2023
   *   <li>{@code ?effectivePeriod=ne2023-06} matches all periods NOT overlapping June 2023
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a boolean query that excludes matching dates
   */
  protected Query prepareNotEqualsQuery(String paramName, DateParam param) {
    Query equalsQuery = prepareEqualsQuery(paramName, param);
    // There might be a more elegant way to do this
    return new BooleanQuery.Builder()
        .add(equalsQuery, BooleanClause.Occur.MUST_NOT)
        .add(MatchAllDocsQuery.INSTANCE, BooleanClause.Occur.MUST)
        .build();
  }

  /**
   * Prepares a less-than query for dates.
   *
   * <p>Matches all dates strictly before the first instant of the specified date's precision range.
   * For example, {@code lt2023-06} matches all dates before 2023-06-01T00:00:00.000
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=lt2023-06-15} matches all dates before June 15, 2023
   *   <li>{@code ?effectivePeriod=lt2023-06-15} matches all periods ending before June 15, 2023
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a range query matching dates before the specified instant
   */
  protected Query prepareLessThanQuery(String paramName, DateParam param) {
    DateRange range = calculatePrecisionRange(param);
    return prepareLongRangeQuery(paramName, 0, range.from() - 1);
  }

  /**
   * Prepares a less-than-or-equals query for dates.
   *
   * <p>Matches all dates before or within the last instant of the specified date's precision range.
   * For example, {@code le2023-06} matches all dates up to and including 2023-06-30T23:59:59.999
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=le2023-12-31} matches all dates up to and including December 31, 2023
   *   <li>{@code ?effectivePeriod=le2023-12-31} matches all periods ending up to and including
   *       December 31, 2023
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a range query matching dates before or at the specified instant
   */
  protected Query prepareLessThanOrEqualsQuery(String paramName, DateParam param) {
    DateRange range = calculatePrecisionRange(param);
    return prepareLongRangeQuery(paramName, 0, range.to());
  }

  /**
   * Prepares a greater-than query for dates.
   *
   * <p>Matches all dates strictly after the last instant of the specified date's precision range.
   * For example, {@code gt2023-06} matches all dates after 2023-06-30T23:59:59.999
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=gt2023-01-01} matches all dates after January 1, 2023
   *   <li>{@code ?effectivePeriod=gt2023-01-01} matches all periods starting after January 1, 2023
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a range query matching dates after the specified instant
   */
  protected Query prepareGreaterThanQuery(String paramName, DateParam param) {
    DateRange range = calculatePrecisionRange(param);
    return prepareLongRangeQuery(paramName, range.to() + 1, Long.MAX_VALUE);
  }

  /**
   * Prepares a greater-than-or-equals query for dates.
   *
   * <p>Matches all dates after or within the first instant of the specified date's precision range.
   * For example, {@code ge2023-06} matches all dates from 2023-06-01T00:00:00.000 onwards
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?date=ge2024-01-01} matches all dates from January 1, 2024 onwards
   *   <li>{@code ?_lastUpdated=ge2024-01-01T00:00:00Z} - Resources updated since Jan 1, 2024
   *   <li>{@code ?effectivePeriod=ge2024-01-01} matches all periods starting from January 1, 2024
   *       onwards
   * </ul>
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter with value and precision
   * @return a range query matching dates after or at the specified instant
   */
  protected Query prepareGreaterThanOrEqualsQuery(String paramName, DateParam param) {
    DateRange range = calculatePrecisionRange(param);
    return prepareLongRangeQuery(paramName, range.from(), Long.MAX_VALUE);
  }

  /**
   * Prepares a starts-after query for dates (not yet implemented).
   *
   * <p>The {@code sa} (starts after) prefix would match periods that start after the specified
   * date. This is typically used for period-based searches rather than instant searches.
   *
   * <p>For periods, this would be distinct from {@code gt} which uses the period end for
   * comparison.
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - prefix not yet supported
   */
  protected Query prepareStartsAfterQuery(String paramName, DateParam param)
      throws NotImplementedException {
    throw new NotImplementedException("Not implemented yet.");
  }

  /**
   * Prepares an ends-before query for dates (not yet implemented).
   *
   * <p>The {@code eb} (ends before) prefix would match periods that end before the specified date.
   * This is typically used for period-based searches rather than instant searches.
   *
   * <p>For periods, this would be distinct from {@code lt} which uses the period end for
   * comparison.
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - prefix not yet supported
   */
  protected Query prepareEndsBeforeQuery(String paramName, DateParam param)
      throws NotImplementedException {
    throw new NotImplementedException("Not implemented yet.");
  }

  /**
   * Prepares an approximate query for dates (not yet implemented).
   *
   * <p>The {@code ap} (approximate) prefix would match dates within an approximate range of the
   * specified date, with implementation-defined tolerance.
   *
   * @param paramName the FHIR search parameter name
   * @param param the date parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - prefix not yet supported
   */
  protected Query prepareApproximateQuery(String paramName, DateParam param)
      throws NotImplementedException {
    throw new NotImplementedException("Not implemented yet.");
  }

  /**
   * Simple record to hold date range boundaries.
   *
   * @param from the start of the range (inclusive) in milliseconds since epoch
   * @param to the end of the range (inclusive) in milliseconds since epoch
   */
  protected record DateRange(long from, long to) {}
}
