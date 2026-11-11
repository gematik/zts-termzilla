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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.string;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_STANDARD;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.AnalyzerUtils;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.FhirStringAnalyzerMultipleToken;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.LuceneQuerySanitizer;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Builds Lucene queries for FHIR string search parameters.
 *
 * <p>Supports standard prefix behavior and the modifiers {@code :exact}, {@code :contains}, and
 * {@code :missing}.
 */
@Slf4j
public class StringParameterQueryGenerator {

  private FhirStringAnalyzerMultipleToken fhirStringAnalyzerMultipleToken;

  /** Creates a generator with the analyzer used for string contains parsing. */
  public StringParameterQueryGenerator() {
    fhirStringAnalyzerMultipleToken = new FhirStringAnalyzerMultipleToken();
  }

  /**
   * Creates a query for a {@link StringAndListParam} using FHIR AND/OR semantics.
   *
   * @param andListParamData parameter wrapper containing the parameter name and value
   * @return composed Lucene query for all AND/OR branches
   * @throws NotImplementedException if a contained modifier is not supported
   */
  public Query prepareQuery(ParamData andListParamData) throws NotImplementedException {

    StringAndListParam andListParam = (StringAndListParam) andListParamData.getParam();
    List<Query> andQueryList = new ArrayList<>();

    for (StringOrListParam orListParam : andListParam.getValuesAsQueryTokens()) {
      List<Query> orQueryList = new ArrayList<>();
      for (StringParam param : orListParam.getValuesAsQueryTokens()) {
        orQueryList.add(prepareQuery(andListParamData.getParamName(), param));
      }
      andQueryList.add(QueryCombinators.prepareOrQuery(orQueryList));
    }
    return QueryCombinators.prepareAndQuery(andQueryList);
  }

  /**
   * Creates a query for one string parameter value.
   *
   * @param paramName search parameter name
   * @param param string parameter value
   * @return Lucene query for the selected string matching mode
   * @throws NotImplementedException if unsupported or unsafe input is detected
   */
  public Query prepareQuery(String paramName, StringParam param) throws NotImplementedException {

    Query query;

    if (param == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.isEmpty() && param.getMissing() == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.isExact()) {
      // :exact search modifier
      query = prepareExactModifierQuery(paramName, param);
    } else if (param.isContains()) {
      // :contains search modifier
      query = prepareContainsModifierQuery(paramName, param);
    } else if (param.getMissing() != null) {
      // :missing search modifier
      query = QueryCombinators.prepareMissingModifierQuery(paramName, param);
    } else {
      // standard search
      query = prepareStandardQuery(paramName, param);
    }

    return query;
  }

  /**
   * Creates an exact match query for the {@code :exact} modifier.
   *
   * @param paramName search parameter name
   * @param param string parameter value
   * @return exact {@link TermQuery} against the exact string field
   */
  public Query prepareExactModifierQuery(String paramName, StringParam param) {

    // The :exact modifier returns results that match the entire supplied parameter, including
    // casing and accents.
    String termString = param.getValue();
    String inField = paramName + SEPARATOR + SUFFIX_STRING_EXACT;

    return new TermQuery(new Term(inField, termString));
  }

  /**
   * Creates a contains query for the {@code :contains} modifier.
   *
   * <p>Input is sanitized, normalized, and translated into a wildcard query that requires all
   * non-empty terms.
   *
   * @param paramName search parameter name
   * @param param string parameter value
   * @return parsed Lucene query for contains semantics
   * @throws NotImplementedException if the input is unsafe or parsing fails
   */
  public Query prepareContainsModifierQuery(String paramName, StringParam param)
      throws NotImplementedException {

    // The :contains modifier returns results that include the supplied parameter value anywhere
    // within the field being searched.

    String inField = paramName + SEPARATOR + SUFFIX_STRING_CONTAINS;

    String termString = param.getValue();

    // SECURITY: Validate input before processing
    if (!LuceneQuerySanitizer.isSafeInput(termString)) {
      throw new NotImplementedException("Query contains unsafe patterns");
    }

    // Normalize the phrase
    termString = AnalyzerUtils.normalizePhrase(termString);
    String[] terms = termString.split("\\s+");

    // Build query string with wildcards
    StringBuilder queryBuilder = new StringBuilder();
    for (String term : terms) {
      if (StringUtils.isNotBlank(term)) {
        // SECURITY: Escape special Lucene characters in each term
        String safeTerm = LuceneQuerySanitizer.escapeLuceneSpecialChars(term);
        queryBuilder.append("+").append(safeTerm).append("* ");
      }
    }

    String queryString = queryBuilder.toString().trim();

    if (StringUtils.isBlank(queryString)) {
      throw new NotImplementedException("Empty query after sanitization");
    }

    Query query;
    try {
      query = new QueryParser(inField, fhirStringAnalyzerMultipleToken).parse(queryString);
      log.debug("Created contains query: field={}, query={}", inField, queryString);
    } catch (ParseException e) {
      log.error("Failed to parse query: {}", queryString, e);
      throw new NotImplementedException("Query not supported by Server: " + e.getMessage());
    }

    return query;
  }

  /**
   * Creates a query for the {@code :text} modifier.
   *
   * @param paramName search parameter name
   * @param param string parameter value
   * @return query for text semantics
   * @throws NotImplementedException always, because this modifier is not supported yet
   */
  public Query prepareTextModifierQuery(String paramName, StringParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":text modifier not implemented yet in search engine");
  }

  /**
   * Creates the default prefix query for string parameters.
   *
   * @param paramName search parameter name
   * @param param string parameter value
   * @return prefix query against the normalized standard string field
   */
  public Query prepareStandardQuery(String paramName, StringParam param) {

    // Default string search such as "name=Universitatsklinik Char".
    // This search is case-insensitive and ignores accents, punctuation,
    // and non-significant whitespace differences.

    String inField = paramName + SEPARATOR + SUFFIX_STRING_STANDARD;
    String termString = param.getValue();

    termString = AnalyzerUtils.normalizePhrase(termString);
    return new PrefixQuery(new Term(inField, termString));
  }
}
