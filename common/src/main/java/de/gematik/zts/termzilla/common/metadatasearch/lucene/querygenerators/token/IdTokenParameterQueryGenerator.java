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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_EXACT;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Static utility class for generating Lucene queries for FHIR resource identifiers (_id parameter).
 *
 * <p>This class provides static methods for building queries specific to resource ID searches.
 * Resource IDs are treated as exact-match tokens and do not support hierarchical or text-based
 * search modifiers.
 *
 * <p>Supported operations:
 *
 * <ul>
 *   <li>Standard exact match search (e.g., {@code _id=123})
 *   <li>{@code :not} modifier for negation (e.g., {@code _id:not=123})
 * </ul>
 *
 * <p>Unsupported modifiers that will throw {@link NotImplementedException}:
 *
 * <ul>
 *   <li>{@code :missing} - Every resource has a logical ID
 *   <li>{@code :text} - ID search is always exact
 *   <li>{@code :above} - ID search is always exact
 *   <li>{@code :below} - ID search is always exact
 *   <li>{@code :in} - ID search is always exact
 *   <li>{@code :not-in} - ID search is always exact
 *   <li>{@code :of-type} - ID search is always exact
 * </ul>
 *
 * @see org.apache.lucene.search.TermQuery
 */
@Slf4j
public final class IdTokenParameterQueryGenerator {

  /**
   * Set of token parameter modifiers that are not supported for ID searches. All these modifiers
   * are incompatible with exact ID matching.
   */
  private static final Set<TokenParamModifier> UNSUPPORTED_MODIFIERS =
      Set.of(
          TokenParamModifier.TEXT,
          TokenParamModifier.ABOVE,
          TokenParamModifier.BELOW,
          TokenParamModifier.IN,
          TokenParamModifier.NOT_IN,
          TokenParamModifier.OF_TYPE);

  /** Private constructor to prevent instantiation of utility class. */
  private IdTokenParameterQueryGenerator() {
    throw new AssertionError(
        "IdTokenParameterQueryGenerator is a utility class and should not be instantiated");
  }

  public static Query prepareQuery(ParamData andListParamData) throws NotImplementedException {

    TokenAndListParam andListParam = (TokenAndListParam) andListParamData.getParam();
    List<Query> andQueryList = new ArrayList<>();

    for (TokenOrListParam orListParam : andListParam.getValuesAsQueryTokens()) {
      List<Query> orQueryList = new ArrayList<>();
      for (TokenParam param : orListParam.getValuesAsQueryTokens()) {
        orQueryList.add(prepareQuery(andListParamData.getParamName(), param));
      }
      andQueryList.add(QueryCombinators.prepareOrQuery(orQueryList));
    }
    return QueryCombinators.prepareAndQuery(andQueryList);
  }

  /**
   * Prepares a Lucene query for FHIR resource ID search parameters.
   *
   * <p>This method enforces restrictions specific to resource ID searches. Most search modifiers
   * are rejected because resource IDs require exact matching only.
   *
   * @param paramName the name of the search parameter (typically "_id")
   * @param param the token parameter containing the ID value and optional modifiers
   * @return a Lucene {@link Query} for matching resources by ID
   * @throws NotImplementedException if an unsupported search modifier is used
   */
  public static Query prepareQuery(String paramName, TokenParam param)
      throws NotImplementedException {

    Query query;

    if (param.isEmpty() && param.getMissing() == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.getMissing() != null) {
      // :missing search modifier
      throw new NotImplementedException(
          "The '_id' parameter is not combinable with the ':missing' modifier. Every resource has its own logical 'id'!");
    } else if (param.getModifier() == TokenParamModifier.NOT) {
      // :not search modifier
      query = prepareNotModifierQuery(paramName, param);
    } else if (param.getModifier() != null && UNSUPPORTED_MODIFIERS.contains(param.getModifier())) {
      // Unsupported search modifiers
      throw new NotImplementedException(
          String.format(
              "The '_id' parameter is not combinable with the '%s' modifier. Searching for 'id' is always an exact search!",
              param.getModifier().getValue()));
    } else {
      // standard search
      query = prepareStandardQuery(paramName, param);
    }

    return query;
  }

  /**
   * Prepares a standard exact-match Lucene query for a resource ID.
   *
   * <p>This method creates a {@link TermQuery} that performs case-sensitive exact matching on the
   * resource ID value. Unlike token parameter queries, this method does not process system values
   * or perform case normalization, as resource IDs are treated as literal identifiers.
   *
   * <p>The query targets the indexed field with the pattern: {@code paramName_tokenexact}
   *
   * @param paramName the name of the search parameter (typically "_id")
   * @param param the token parameter containing the ID value
   * @return a {@link TermQuery} for exact ID matching
   * @throws NotImplementedException if no ID value is provided
   */
  public static Query prepareStandardQuery(String paramName, TokenParam param)
      throws NotImplementedException {

    String paramValueValue = param.getValue();

    String termString;
    if (paramValueValue != null) {
      termString = paramValueValue;
    } else {
      throw new NotImplementedException(
          "You have to provide at least an 'id' when using '_id' search parameter.");
    }

    String inField = paramName + SEPARATOR + SUFFIX_TOKEN_EXACT;

    return new TermQuery(new Term(inField, termString));
  }

  /**
   * Prepares a negated query for the :not modifier.
   *
   * <p>Creates a query that matches all documents EXCEPT those with the specified ID. Implemented
   * as: {@code MUST(MatchAllDocsQuery) AND MUST_NOT(standardQuery)}
   *
   * @param paramName the name of the search parameter (typically "_id")
   * @param param the token parameter containing the ID value to exclude
   * @return a {@link BooleanQuery} that matches all resources except the specified ID
   * @throws NotImplementedException if the standard query cannot be prepared
   */
  private static Query prepareNotModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    Query standardQuery = prepareStandardQuery(paramName, param);

    return new BooleanQuery.Builder()
        .add(standardQuery, BooleanClause.Occur.MUST_NOT)
        .add(MatchAllDocsQuery.INSTANCE, BooleanClause.Occur.MUST)
        .build();
  }
}
