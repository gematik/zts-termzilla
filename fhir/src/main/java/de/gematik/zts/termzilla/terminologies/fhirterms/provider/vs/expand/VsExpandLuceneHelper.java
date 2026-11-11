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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_END;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_DATE_START;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_LONG_POINT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_ID;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.automaton.RegExp;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Encapsulates Lucene query building and execution for the ValueSet {@code $expand} operation.
 *
 * <p>This helper decides whether Lucene-based filtering should be used based on a concept count
 * threshold, builds appropriate Lucene queries for equals and regex filter operations, and executes
 * them against the concept index.
 *
 * <p>The Lucene path is used when the concept count exceeds {@link #LUCENE_THRESHOLD}, providing
 * significantly better performance for large CodeSystems compared to in-memory iteration.
 *
 * @see FhirTermsVsExplicitExpandOperation
 */
@Slf4j
class VsExpandLuceneHelper {

  static final int LUCENE_THRESHOLD = 1000;
  private static final int HITS_PER_PAGE = 100000;

  private final IndexSearcher luceneSearcher;
  private final String scope;
  private final CodeSystem codeSystem;
  private final int conceptCount;

  /**
   * Creates a new Lucene helper for a specific expansion context.
   *
   * @param luceneSearcher the Lucene index searcher
   * @param scope the scope in which the expansion is performed
   * @param codeSystem the CodeSystem being expanded
   */
  VsExpandLuceneHelper(IndexSearcher luceneSearcher, String scope, CodeSystem codeSystem) {
    this.luceneSearcher = luceneSearcher;
    this.scope = scope;
    this.codeSystem = codeSystem;

    // Nur traversieren wenn ein Searcher vorhanden ist — sonst wird Lucene ohnehin nicht genutzt
    if (luceneSearcher != null) {
      this.conceptCount = CodeSystemHelper.getCachedAllConceptsAsList(codeSystem).size();
    } else {
      this.conceptCount = 0;
    }
  }

  /**
   * Determines whether Lucene-based filtering should be used.
   *
   * @return {@code true} if the concept count meets or exceeds the threshold and a searcher is
   *     available
   */
  boolean shouldUseLucene() {
    return luceneSearcher != null && conceptCount >= LUCENE_THRESHOLD;
  }

  /**
   * Executes a combined query containing both equals and regex filters.
   *
   * @param equalsFilters the equals filter components
   * @param regexFilters the regex filter components
   * @return the set of concept codes matching all filters
   */
  Set<String> executeCombinedFilters(
      List<ValueSet.ConceptSetFilterComponent> equalsFilters,
      List<ValueSet.ConceptSetFilterComponent> regexFilters) {

    List<Query> mustClauses = new ArrayList<>();

    for (ValueSet.ConceptSetFilterComponent filter : equalsFilters) {
      mustClauses.add(buildPropertyEqualsQuery(filter.getProperty(), filter.getValue()));
    }
    for (ValueSet.ConceptSetFilterComponent filter : regexFilters) {
      mustClauses.add(buildPropertyRegexQuery(filter.getProperty(), filter.getValue()));
    }

    Query combinedQuery = buildFinalQuery(mustClauses);

    log.info("Combined Query: {}", combinedQuery);
    return executeAndReturnCodes(combinedQuery);
  }

  // ================================================================================================
  // Internal query building methods
  // ================================================================================================

  /**
   * Builds the pre-filter query that restricts results to the correct scope, resource type, and
   * CodeSystem ID.
   */
  private Query buildPreFilterQuery() {
    List<Query> filterClauses = new ArrayList<>();

    // Scope filter
    filterClauses.add(new TermQuery(new Term(NAME_SCOPE, scope)));

    // Resource type filter: ConceptDefinitionComponent
    filterClauses.add(
        new TermQuery(
            new Term(FhirSearchHelper.RESOURCE_TYPE_FIELD, "ConceptDefinitionComponent")));

    // CodeSystem ID filter
    filterClauses.add(
        new TermQuery(new Term(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT, codeSystem.getIdPart())));

    return QueryCombinators.prepareFilterQuery(filterClauses);
  }

  /**
   * Builds a Lucene query for an equals filter on the given property.
   *
   * <p>Resolves the property type from the CodeSystem definition and selects the appropriate query
   * strategy (TermQuery for string/code/boolean, LongPoint for integer, etc.).
   *
   * @param propertyCode the property code
   * @param value the expected value as string
   * @return the Lucene query for this property filter
   */
  Query buildPropertyEqualsQuery(String propertyCode, String value) {
    // Special handling for "code" property
    if ("code".equals(propertyCode)) {
      return new TermQuery(new Term(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT, value));
    }

    // Resolve property type from CodeSystem
    CodeSystem.PropertyComponent property =
        CodeSystemHelper.getProperty(codeSystem, propertyCode)
            .orElseThrow(
                () ->
                    new InvalidRequestException(
                        ExpandFilterValidator.UNSUPPORTED_FILTER_PROPERTY_MESSAGE + propertyCode));

    String fieldPrefix = NAME_PREFIX_PROPERTY + SEPARATOR + propertyCode + SEPARATOR;

    return switch (property.getType()) {
      case STRING -> new TermQuery(new Term(fieldPrefix + SUFFIX_STRING_EXACT, value));
      case CODE -> new TermQuery(new Term(fieldPrefix + SUFFIX_TOKEN_EXACT, value));
      case BOOLEAN -> new TermQuery(new Term(fieldPrefix + SUFFIX_TOKEN_EXACT, value));
      case INTEGER -> {
        long longValue = Long.parseLong(value);
        yield LongPoint.newExactQuery(fieldPrefix + SUFFIX_LONG_POINT, longValue);
      }
      case DECIMAL -> new TermQuery(new Term(fieldPrefix + SUFFIX_STRING_EXACT, value));
      case DATETIME -> buildDateTimeOverlapQuery(fieldPrefix, value);
      case CODING -> new TermQuery(new Term(fieldPrefix + SUFFIX_TOKEN_EXACT, value));
      default ->
          throw new InvalidRequestException(
              "Unsupported property type for Lucene equals filter: " + property.getType());
    };
  }

  /**
   * Builds a Lucene query for a regex filter on the given property.
   *
   * <p>Uses {@link RegExp#NONE} to disable all optional Lucene-specific syntax extensions ({@code
   * &}, {@code ~}, {@code @}, {@code #}, {@code <n-m>}, {@code "..."}). This ensures that the regex
   * is interpreted using only the base syntax that is functionally equivalent to Java's {@link
   * java.util.regex.Pattern} engine.
   *
   * @param propertyCode the property code
   * @param regex the regular expression to match
   * @return the Lucene RegexpQuery for this property filter
   * @see ExpandFilterValidator#validateRegexPattern(String)
   */
  Query buildPropertyRegexQuery(String propertyCode, String regex) {
    // Special handling for "code" property
    if ("code".equals(propertyCode)) {
      return new RegexpQuery(
          new Term(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT, regex), RegExp.NONE);
    }

    String fieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyCode + SEPARATOR + SUFFIX_STRING_EXACT;
    return new RegexpQuery(new Term(fieldName, regex), RegExp.NONE);
  }

  /**
   * Executes a Lucene query and returns the matching concept codes.
   *
   * <p>Uses {@link SortedDocValues} on the {@code code_sort} field for efficient code extraction,
   * avoiding the overhead of loading full stored documents per hit.
   *
   * @param query the Lucene query to execute
   * @return the set of concept codes from matching documents
   */
  Set<String> executeAndReturnCodes(Query query) {
    log.debug("Executing Lucene expand query: {}", query);

    try {
      Set<String> resultCodes = new HashSet<>();
      TopDocs topDocs = luceneSearcher.search(query, HITS_PER_PAGE);
      String docValuesFieldName = NAME_CODE + SEPARATOR + SUFFIX_SORT;

      List<LeafReaderContext> leaves = luceneSearcher.getIndexReader().leaves();

      // Cache the DocValues instance per leaf segment to avoid repeated lookups
      LeafReaderContext currentLeaf = null;
      SortedDocValues currentDocValues = null;

      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        LeafReaderContext leafContext = findLeafContext(leaves, scoreDoc.doc);

        // Nur bei Segmentwechsel neue DocValues-Instanz holen
        if (leafContext != currentLeaf) {
          currentLeaf = leafContext;
          currentDocValues = leafContext.reader().getSortedDocValues(docValuesFieldName);
        }

        if (currentDocValues != null) {
          int localDocId = scoreDoc.doc - leafContext.docBase;
          if (currentDocValues.advanceExact(localDocId)) {
            resultCodes.add(currentDocValues.lookupOrd(currentDocValues.ordValue()).utf8ToString());
          }
        }
      }

      log.debug("Lucene expand query returned {} codes", resultCodes.size());
      return resultCodes;

    } catch (IOException e) {
      throw new InvalidRequestException("Error while executing Lucene expand query: ", e);
    }
  }

  /** Finds the {@link LeafReaderContext} that contains the given global document ID. */
  private static LeafReaderContext findLeafContext(
      List<LeafReaderContext> leaves, int globalDocId) {
    for (LeafReaderContext leaf : leaves) {
      if (globalDocId < leaf.docBase + leaf.reader().maxDoc()) {
        return leaf;
      }
    }
    throw new IllegalStateException("No leaf context found for document ID: " + globalDocId);
  }

  // ================================================================================================
  // Private helper methods
  // ================================================================================================

  /** Combines the pre-filter query with the given must clauses into a final query. */
  private Query buildFinalQuery(List<Query> mustClauses) {
    Query preFilter = buildPreFilterQuery();

    if (mustClauses.isEmpty()) {
      return preFilter;
    }

    Query mustQuery = QueryCombinators.prepareAndQuery(mustClauses);
    return QueryCombinators.prepareCombinedFilterMustQuery(preFilter, mustQuery);
  }

  /**
   * Builds a DateTime overlap query where the indexed date range overlaps with the filter value.
   *
   * <p>Uses the pattern: indexed_start <= filterEnd AND indexed_end >= filterStart.
   */
  private Query buildDateTimeOverlapQuery(String fieldPrefix, String value) {
    DateTimeType dateTime = new DateTimeType(value);
    long filterStart = DateUtil.getFirstInstantOfDateTime(dateTime);
    long filterEnd = DateUtil.getLastInstantOfDateTime(dateTime);

    Query startBeforeEnd =
        LongPoint.newRangeQuery(fieldPrefix + SUFFIX_DATE_START, Long.MIN_VALUE, filterEnd);
    Query endAfterStart =
        LongPoint.newRangeQuery(fieldPrefix + SUFFIX_DATE_END, filterStart, Long.MAX_VALUE);

    return QueryCombinators.prepareAndQuery(List.of(startBeforeEnd, endAfterStart));
  }
}
