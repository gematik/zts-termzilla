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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Static utility class for generating Lucene queries for FHIR token search parameters.
 *
 * <p>This class provides static methods for building queries for FHIR token-type search parameters
 * such as identifier, boolean, code, coding, and codeableConcept. Token parameters support
 * system|value pair matching with various search modifiers.
 *
 * <p><b>Supported token types:</b>
 *
 * <ul>
 *   <li>Identifier - Resource identifiers with optional system
 *   <li>Boolean - true/false values
 *   <li>Code - Coded values from a fixed set
 *   <li>Coding - System|code pairs
 *   <li>CodeableConcept - Multiple coding values
 * </ul>
 *
 * <p><b>Supported search modifiers:</b>
 *
 * <ul>
 *   <li>Standard search - Exact match on system|value or value
 *   <li>{@code :not} - Negation (exclude matching resources)
 *   <li>{@code :missing} - Search for presence/absence of the parameter
 * </ul>
 *
 * <p><b>Unsupported modifiers (throw {@link NotImplementedException}):</b>
 *
 * <ul>
 *   <li>{@code :text} - Text search on display value
 *   <li>{@code :above} - Hierarchical search (ancestor codes)
 *   <li>{@code :below} - Hierarchical search (descendant codes)
 *   <li>{@code :in} - ValueSet membership
 *   <li>{@code :not-in} - ValueSet exclusion
 *   <li>{@code :of-type} - Identifier type filtering
 * </ul>
 *
 * <p><b>Query Structure:</b>
 *
 * <p>Token queries are indexed in the field {@code paramName_tokenlowercaseexact} with values in
 * the format:
 *
 * <ul>
 *   <li>{@code system|value} - When both system and value are provided
 *   <li>{@code value} - When only value is provided
 * </ul>
 *
 * <p>All values are lowercased for case-insensitive matching.
 *
 * <p><b>FHIR Examples:</b>
 *
 * <ul>
 *   <li>{@code ?identifier=http://acme.org|12345} - Exact system and value
 *   <li>{@code ?identifier=12345} - Value only
 *   <li>{@code ?status=active} - Code value
 *   <li>{@code ?status:not=retired} - Negation
 *   <li>{@code ?category:missing=true} - Missing parameter
 * </ul>
 *
 * @see QueryCombinators
 * @see IdTokenParameterQueryGenerator
 * @see org.apache.lucene.search.TermQuery
 */
@Slf4j
public final class TokenParameterQueryGenerator {

  /** Private constructor to prevent instantiation of utility class. */
  private TokenParameterQueryGenerator() {
    throw new AssertionError(
        "TokenParameterQueryGenerator is a utility class and should not be instantiated");
  }

  /**
   * Prepares a Lucene query for FHIR token search parameters from ParamData.
   *
   * <p>This method handles AND/OR combinations of token parameters. FHIR allows comma-separated
   * values (OR) and multiple parameter instances (AND):
   *
   * <ul>
   *   <li>{@code ?identifier=val1,val2} - OR combination (at least one must match)
   *   <li>{@code ?identifier=val1&identifier=val2} - AND combination (both must match)
   * </ul>
   *
   * @param andListParamData the parameter data containing TokenAndListParam
   * @return a Lucene query combining all token conditions with appropriate AND/OR logic
   * @throws NotImplementedException if an unsupported modifier is encountered
   */
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
   * Prepares a Lucene query for a single FHIR token parameter.
   *
   * <p>This method routes the query generation based on the parameter's modifier:
   *
   * <ul>
   *   <li>No modifier - Standard exact match search
   *   <li>{@code :not} - Negated search (exclude matching resources)
   *   <li>{@code :missing} - Presence/absence search
   *   <li>Other modifiers - Throw NotImplementedException
   * </ul>
   *
   * @param paramName the FHIR search parameter name (e.g., "identifier", "status")
   * @param param the token parameter containing value, optional system, and modifiers
   * @return a Lucene query for the token parameter
   * @throws NotImplementedException if an unsupported modifier is used
   */
  public static Query prepareQuery(String paramName, TokenParam param)
      throws NotImplementedException {

    Query query;
    if (param == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.isEmpty() && param.getMissing() == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.getMissing() != null) {
      // :missing search modifier
      query = QueryCombinators.prepareMissingModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.TEXT) {
      // :text search modifier
      query = prepareTextModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.NOT) {
      // :not search modifier
      query = prepareNotModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.ABOVE) {
      // :above search modifier
      query = prepareAboveModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.BELOW) {
      // :below search modifier
      query = prepareBelowModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.IN) {
      // :in search modifier
      query = prepareInModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.NOT_IN) {
      // :not-in search modifier
      query = prepareNotInModifierQuery(paramName, param);
    } else if (param.getModifier() == TokenParamModifier.OF_TYPE) {
      // :of-type search modifier
      query = prepareOfTypeModifierQuery(paramName, param);
    } else {
      // standard search
      query = prepareStandardQuery(paramName, param);
    }

    return query;
  }

  /**
   * Prepares a query for the {@code :text} modifier (not yet implemented).
   *
   * <p>The :text modifier searches the display/text portion of coded values rather than the code
   * itself.
   *
   * @param paramName the search parameter name
   * @param param the token parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareTextModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":text modifier not implemented yet in search engine");
  }

  /**
   * Prepares a negated query for the {@code :not} modifier.
   *
   * <p>Creates a query that matches all documents EXCEPT those matching the token value.
   * Implemented as: {@code MUST(MatchAllDocsQuery) AND MUST_NOT(standardQuery)}
   *
   * <p><b>FHIR Example:</b> {@code ?status:not=retired} matches all resources where status is NOT
   * retired.
   *
   * @param paramName the search parameter name
   * @param param the token parameter containing the value to exclude
   * @return a BooleanQuery that excludes matching documents
   * @throws NotImplementedException if the standard query cannot be prepared
   */
  public static Query prepareNotModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    Query standardQuery = prepareStandardQuery(paramName, param);

    return new BooleanQuery.Builder()
        .add(standardQuery, BooleanClause.Occur.MUST_NOT)
        .add(MatchAllDocsQuery.INSTANCE, BooleanClause.Occur.MUST)
        .build();
  }

  /**
   * Prepares a query for the {@code :above} modifier (not yet implemented).
   *
   * <p>The :above modifier performs hierarchical code search, matching the specified code and all
   * its ancestor codes in a code system hierarchy.
   *
   * @param paramName the search parameter name
   * @param param the token parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareAboveModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":above modifier not implemented yet in search engine");
  }

  /**
   * Prepares a query for the {@code :below} modifier (not yet implemented).
   *
   * <p>The :below modifier performs hierarchical code search, matching the specified code and all
   * its descendant codes in a code system hierarchy.
   *
   * @param paramName the search parameter name
   * @param param the token parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareBelowModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":below modifier not implemented yet in search engine");
  }

  /**
   * Prepares a query for the {@code :in} modifier (not yet implemented).
   *
   * <p>The :in modifier searches for codes that are members of a specified ValueSet.
   *
   * @param paramName the search parameter name
   * @param param the token parameter containing ValueSet reference
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareInModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":in modifier not implemented yet in search engine");
  }

  /**
   * Prepares a query for the {@code :not-in} modifier (not yet implemented).
   *
   * <p>The :not-in modifier searches for codes that are NOT members of a specified ValueSet.
   *
   * @param paramName the search parameter name
   * @param param the token parameter containing ValueSet reference
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareNotInModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":not-in modifier not implemented yet in search engine");
  }

  /**
   * Prepares a query for the {@code :of-type} modifier (not yet implemented).
   *
   * <p>The :of-type modifier filters identifier searches by identifier type (e.g., MRN, SSN).
   *
   * @param paramName the search parameter name
   * @param param the token parameter containing identifier type
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareOfTypeModifierQuery(String paramName, TokenParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":of-type modifier not implemented yet in search engine");
  }

  // TODO: check behavior against FHIR spec

  /**
   * Prepares a standard exact-match query for a token parameter.
   *
   * <p>This method creates a {@link TermQuery} for exact matching on token values. The query
   * targets the {@code paramName_tokenlowercaseexact} field with lowercased values in the format:
   *
   * <ul>
   *   <li>{@code system|value} - When both system and value are provided
   *   <li>{@code value} - When only value is provided (system is null)
   * </ul>
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?identifier=http://hospital.org|12345} → Query: "http://hospital.org|12345"
   *   <li>{@code ?identifier=12345} → Query: "12345"
   *   <li>{@code ?status=active} → Query: "active"
   * </ul>
   *
   * @param paramName the search parameter name
   * @param param the token parameter containing value and optional system
   * @return a TermQuery for exact matching
   * @throws NotImplementedException if neither system nor value is provided
   */
  public static Query prepareStandardQuery(String paramName, TokenParam param)
      throws NotImplementedException {

    String paramValueSystem = param.getSystem();
    String paramValueValue = param.getValue();

    String termString;
    if (paramValueSystem != null && paramValueValue != null) {
      termString = paramValueSystem.toLowerCase() + "|" + paramValueValue.toLowerCase();
    } else if (paramValueSystem == null && paramValueValue != null) {
      termString = paramValueValue.toLowerCase();
    } else {
      throw new NotImplementedException("Scenario not implemented yet in search engine");
    }

    String inField = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

    return new TermQuery(new Term(inField, termString));
  }
}
