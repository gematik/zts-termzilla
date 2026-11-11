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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_TYPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_MISSING;

import ca.uhn.fhir.rest.param.BaseParam;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Utility class providing static helper methods for combining and constructing Lucene queries.
 *
 * <p>This class serves as a central location for query combination operations used throughout the
 * FHIR search parameter query generation process. All methods are stateless and thread-safe.
 *
 * <p><b>Query Combination Patterns:</b>
 *
 * <ul>
 *   <li><b>FILTER</b> - Boolean clause that filters results without affecting scoring
 *   <li><b>MUST</b> - Boolean AND operation (all clauses must match)
 *   <li><b>SHOULD</b> - Boolean OR operation (at least one clause must match)
 *   <li><b>MUST_NOT</b> - Boolean negation (excludes matching documents)
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Combine multiple queries with AND logic
 * List<Query> queries = List.of(nameQuery, statusQuery, dateQuery);
 * Query combinedQuery = QueryCombinators.prepareAndQuery(queries);
 *
 * // Add filter for resource type and scope
 * Query filterQuery = QueryCombinators.prepareFilterQuery(List.of(scopeQuery, typeQuery));
 * Query finalQuery = QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, combinedQuery);
 * }</pre>
 *
 * @see org.apache.lucene.search.BooleanQuery
 * @see org.apache.lucene.search.BooleanClause
 */
@Slf4j
public final class QueryCombinators {

  /** Private constructor to prevent instantiation of utility class. */
  private QueryCombinators() {
    throw new AssertionError("QueryCombinators is a utility class and should not be instantiated");
  }

  // ================================================================================
  // Basic Query Combination Methods
  // ================================================================================

  /**
   * Combines multiple queries using FILTER clauses (Boolean AND without scoring).
   *
   * <p>FILTER clauses are efficient for filtering results because they:
   *
   * <ul>
   *   <li>Do not contribute to document scoring
   *   <li>Can be cached by Lucene for better performance
   *   <li>Are executed before scoring queries
   * </ul>
   *
   * <p>Use this for constraints like scope filtering, resource type filtering, or any other
   * criteria that should narrow results without affecting relevance scores.
   *
   * @param queryList list of queries to combine with FILTER logic (all must match)
   * @return a BooleanQuery with all input queries as FILTER clauses
   * @throws IllegalArgumentException if queryList is null
   */
  public static Query prepareFilterQuery(List<Query> queryList) {
    if (queryList == null) {
      throw new IllegalArgumentException("Query list cannot be null");
    }

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (Query query : queryList) {
      if (query != null) {
        builder.add(query, BooleanClause.Occur.FILTER);
      } else {
        log.warn("Null query encountered in filter query list, skipping");
      }
    }
    return builder.build();
  }

  /**
   * Combines multiple queries using MUST clauses (Boolean AND with scoring).
   *
   * <p>MUST clauses require all queries to match and contribute to document scoring. This is used
   * for search parameters where all conditions must be satisfied.
   *
   * <p><b>FHIR Example:</b> {@code ?name=SNOMED&status=active} requires both conditions to match.
   *
   * @param queryList list of queries to combine with AND logic (all must match)
   * @return a BooleanQuery with all input queries as MUST clauses
   * @throws IllegalArgumentException if queryList is null
   */
  public static Query prepareAndQuery(List<Query> queryList) {
    if (queryList == null) {
      throw new IllegalArgumentException("Query list cannot be null");
    }

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (Query query : queryList) {
      if (query != null) {
        builder.add(query, BooleanClause.Occur.MUST);
      } else {
        log.warn("Null query encountered in AND query list, skipping");
      }
    }
    return builder.build();
  }

  /**
   * Combines multiple queries using SHOULD clauses (Boolean OR with scoring).
   *
   * <p>SHOULD clauses create an OR condition where at least one query must match. All matching
   * clauses contribute to the document score.
   *
   * <p><b>FHIR Example:</b> {@code ?name=SNOMED,LOINC} matches resources where name is either
   * SNOMED OR LOINC.
   *
   * @param queryList list of queries to combine with OR logic (at least one must match)
   * @return a BooleanQuery with all input queries as SHOULD clauses
   * @throws IllegalArgumentException if queryList is null
   */
  public static Query prepareOrQuery(List<Query> queryList) {
    if (queryList == null) {
      throw new IllegalArgumentException("Query list cannot be null");
    }

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (Query query : queryList) {
      if (query != null) {
        builder.add(query, BooleanClause.Occur.SHOULD);
      } else {
        log.warn("Null query encountered in OR query list, skipping");
      }
    }
    return builder.build();
  }

  // ================================================================================
  // Combined Query Construction Methods
  // ================================================================================

  /**
   * Combines a filter query with a must query for efficient constrained searching.
   *
   * <p>This pattern is commonly used in FHIR search operations where:
   *
   * <ul>
   *   <li><b>Filter query</b> - Constrains scope and resource type (no scoring, cacheable)
   *   <li><b>Must query</b> - Contains actual search parameters (with scoring)
   * </ul>
   *
   * <p><b>Important:</b> If the mustQuery is empty (toString().isEmpty()), it will NOT be added to
   * avoid returning zero results. An empty MUST clause would cause Lucene to match nothing.
   *
   * <p><b>Example structure:</b>
   *
   * <pre>{@code
   * BooleanQuery {
   *   FILTER: scope="default" AND _type="codesystem"  // Narrow to specific resources
   *   MUST: name="SNOMED" AND status="active"         // Actual search criteria
   * }
   * }</pre>
   *
   * @param filterQuery the filter query (typically scope + resource type constraints)
   * @param mustQuery the must query (typically search parameter queries)
   * @return a BooleanQuery combining both queries appropriately
   * @throws IllegalArgumentException if filterQuery is null
   */
  public static Query prepareCombinedFilterMustQuery(Query filterQuery, Query mustQuery) {
    if (filterQuery == null) {
      throw new IllegalArgumentException("Filter query cannot be null");
    }

    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(filterQuery, BooleanClause.Occur.FILTER);

    // Important: Only add mustQuery if it's not empty
    // An empty MUST clause would result in zero matches
    if (mustQuery != null && !mustQuery.toString().isEmpty()) {
      builder.add(mustQuery, BooleanClause.Occur.MUST);
    } else if (mustQuery == null) {
      log.debug("Must query is null, only applying filter query");
    } else {
      log.debug("Must query is empty, only applying filter query");
    }

    return builder.build();
  }

  // ================================================================================
  // FHIR-Specific Query Helpers
  // ================================================================================

  /**
   * Creates a query for the FHIR :missing modifier.
   *
   * <p>The :missing modifier allows searching for resources where a specific parameter is present
   * or absent. This is implemented by querying the special {@code {paramName}_missing} field that
   * is indexed for all FHIR search parameters.
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?name:missing=true} - Find resources without a name
   *   <li>{@code ?description:missing=false} - Find resources with a description
   * </ul>
   *
   * <p><b>Implementation Note:</b> The corresponding field generator must create the {@code
   * {paramName}_missing} field during indexing with value "true" or "false".
   *
   * @param paramName the FHIR search parameter name (e.g., "name", "description")
   * @param param the FHIR parameter containing the missing flag
   * @return a TermQuery for the missing field
   * @throws IllegalArgumentException if paramName is null or empty
   * @throws NullPointerException if param or param.getMissing() is null
   * @see <a href="https://hl7.org/fhir/R4/search.html#missing">FHIR :missing modifier</a>
   */
  public static Query prepareMissingModifierQuery(String paramName, BaseParam param) {
    if (paramName == null || paramName.isEmpty()) {
      throw new IllegalArgumentException("Parameter name cannot be null or empty");
    }
    if (param == null || param.getMissing() == null) {
      throw new NullPointerException("Parameter and missing flag cannot be null");
    }

    String inField = paramName + SEPARATOR + SUFFIX_MISSING;
    String termString = param.getMissing().booleanValue() ? "true" : "false";

    return new TermQuery(new Term(inField, termString));
  }

  /**
   * Creates a resource type filtering query for narrowing search to specific FHIR resource types.
   *
   * <p>This method is used to restrict searches to a specific resource type (e.g., only search
   * CodeSystem resources, not ValueSets). The resource type is stored in the {@code _type} field
   * during indexing.
   *
   * <p><b>Example:</b> When searching {@code /CodeSystem?name=SNOMED}, this creates a query for
   * {@code _type=codesystem} to ensure only CodeSystem resources are matched.
   *
   * @param resourceType the FHIR resource type (e.g., "CodeSystem", "ValueSet")
   * @param query the main search query to combine with the resource type filter
   * @return a BooleanQuery combining resource type filter with the main query
   * @throws IllegalArgumentException if resourceType or query is null
   */
  public static Query prepareResourceTypeFilteringQuery(String resourceType, Query query) {
    if (resourceType == null || resourceType.isEmpty()) {
      throw new IllegalArgumentException("Resource type cannot be null or empty");
    }
    if (query == null) {
      throw new IllegalArgumentException("Query cannot be null");
    }

    Query resourceTypeQuery = new TermQuery(new Term(NAME_TYPE, resourceType.toLowerCase()));
    return new BooleanQuery.Builder()
        .add(resourceTypeQuery, BooleanClause.Occur.MUST)
        .add(query, BooleanClause.Occur.MUST)
        .build();
  }

  // ================================================================================
  // Utility Methods
  // ================================================================================

  /**
   * Checks if a query is effectively empty (would match all or no documents without constraints).
   *
   * <p>This is useful to avoid adding empty MUST clauses which would result in zero matches.
   *
   * @param query the query to check
   * @return true if the query is null or has an empty string representation
   */
  public static boolean isQueryEmpty(Query query) {
    return query == null || query.toString().isEmpty();
  }

  /**
   * Creates a simple exact-match term query for a field.
   *
   * <p>This is a convenience method for creating exact-match queries without manually constructing
   * Term objects.
   *
   * @param fieldName the Lucene field name
   * @param value the exact value to match
   * @return a TermQuery for exact matching
   * @throws IllegalArgumentException if fieldName or value is null or empty
   */
  public static Query createTermQuery(String fieldName, String value) {
    if (fieldName == null || fieldName.isEmpty()) {
      throw new IllegalArgumentException("Field name cannot be null or empty");
    }
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Value cannot be null or empty");
    }

    return new TermQuery(new Term(fieldName, value));
  }
}
