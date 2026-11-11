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

import static de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper.RESOURCE_TYPE_FIELD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE_LOWERCASE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DEFINITION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DISPLAY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SYNONYM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_STANDARD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_TOKEN_EXACT;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.AnalyzerUtils;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.FhirStringAnalyzerMultipleToken;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;

/**
 * Utility class for generating Lucene queries used to search for FHIR CodeSystem concepts.
 *
 * <p>Provides query builders for CodeSystem search operations and the {@code $find-matches}
 * operation, handling different search modes such as exact, standard, and contains matching with
 * support for case-sensitive and case-insensitive code systems.
 */
@Slf4j
public class ConceptQueryGenerator {

  private ConceptQueryGenerator() {
    /* This utility class should not be instantiated */
  }

  // =================================================================================================
  // CodeSystem Search Operation
  // =================================================================================================

  /**
   * Builds a Lucene query that searches for concepts matching the given code parameter.
   *
   * <p>Without a modifier, the query matches the code value exactly (case-sensitive and
   * case-insensitive). With the {@code :text} modifier, a display text search is performed instead.
   *
   * @param codeParam the code token to search for, optionally including a search modifier
   * @return a Lucene query matching concepts by code or display text
   * @throws NotImplementedException if the code parameter uses an unsupported modifier
   */
  public static Query prepareCodeParamQuery(TokenParam codeParam) throws NotImplementedException {

    List<Query> andQueries = new ArrayList<>();

    // Suche nach dem übergebenen Code. Hierbei müssen wir die möglichen Modifier beachten, da diese
    // teilweise eine vollständig andere Suchlogik erforderlich machen
    if (codeParam.getModifier() == null) {
      // Für den Fall, dass wir CodeSysteme haben, die nicht caseSensitive sind, müssen wir
      // auch nach der normalisierten Form des Codes (alles lowercase) suchen
      List<Query> orQueries = new ArrayList<>();
      orQueries.add(prepareExactCodeQuery(codeParam.getValue()));
      orQueries.add(prepareExactCodeLowerCaseQuery(codeParam.getValue()));
      andQueries.add(QueryCombinators.prepareOrQuery(orQueries));
    } else {

      // derzeit unterstützen wir lediglich den TEXT-Modifier. Die anderen sind hier vermutlich auch
      // nicht sonderlich sinnvoll
      switch (codeParam.getModifier()) {
        case TEXT:
          List<Query> orQueries = new ArrayList<>();
          orQueries.add(prepareStandardDisplayQuery(codeParam.getValue()));
          andQueries.add(QueryCombinators.prepareOrQuery(orQueries));
          break;
        case NOT:
          throw new NotImplementedException("Code search with NOT modifier not implemented");
        case ABOVE:
          throw new NotImplementedException("Code search with ABOVE modifier not implemented");
        case BELOW:
          throw new NotImplementedException("Code search with BELOW modifier not implemented");
        case IN:
          throw new NotImplementedException("Code search with IN modifier not implemented");
        case NOT_IN:
          throw new NotImplementedException("Code search with NOT_IN modifier not implemented");
        case OF_TYPE:
          throw new NotImplementedException("Code search with OF_TYPE modifier not implemented");
        default:
          break;
      }
    }

    return QueryCombinators.prepareAndQuery(andQueries);
  }

  // =================================================================================================

  /**
   * Builds a Lucene query for the {@code _content} search parameter, searching across all concept
   * fields including code, display, synonyms, and other metadata content.
   *
   * <p>No modifiers are supported for this parameter. The search combines exact code matching,
   * standard display/synonym matching, and contains-based metadata content matching as an OR query.
   *
   * @param contentParam the content parameter value to search for
   * @return a Lucene query matching concepts across all content fields
   * @throws NotImplementedException if a modifier is used with the content parameter
   */
  public static Query prepareContentParamQuery(StringParam contentParam)
      throws NotImplementedException {

    List<Query> andQueries = new ArrayList<>();

    // Für den _content-Parameter werden keine Modifier unterstützt, da diese vermutlich relativ
    // sinnlos wären, da es sich bei '_content' eh um eine special Operation handelt, die nach
    // Belieben implementiert werden kann
    if (contentParam.isExact() || contentParam.isContains() || contentParam.isText()) {
      throw new NotImplementedException("No modifiers are supported for _content search.");
    }

    // Inhaltliche Suche - Diese muss als ODER-Suche ausgestaltet werden, da wir ja über den
    // gesamten Inhalt suchen
    List<Query> orQueries = new ArrayList<>();

    if (StringUtils.isNoneEmpty(contentParam.getValue())) {
      // Suche nach Metadaten-Inhalten
      orQueries.add(prepareContainsModifierQuery("_content", contentParam.getValue()));

      // Suche nach passenden Codes
      orQueries.add(prepareExactCodeQuery(contentParam.getValue()));
      orQueries.add(prepareExactCodeLowerCaseQuery(contentParam.getValue()));
      // Suche nach passenden Displays
      orQueries.add(prepareStandardDisplayQuery(contentParam.getValue()));
      // Suche nach passenden Synonymen
      orQueries.add(prepareStandardSynonymQuery(contentParam.getValue()));
    }

    // Hinzufügen der Inhaltssuche
    if (!orQueries.isEmpty()) {
      andQueries.add(QueryCombinators.prepareOrQuery(orQueries));
    }

    return QueryCombinators.prepareAndQuery(andQueries);
  }

  // =================================================================================================
  // CodeSystem $find-matches Operation
  // =================================================================================================

  /**
   * Builds a mandatory pre-filter query for the {@code $find-matches} operation that restricts
   * results to concept entries within the specified scope and code system.
   *
   * @param scope the scope to restrict the search to
   * @param codeSystem the code system whose concepts should be searched
   * @return a Lucene filter query combining type, scope, and ID constraints
   */
  public static Query prepareFindMatchesPreFilterQuery(String scope, CodeSystem codeSystem) {

    List<Query> preFilterQueryList = new ArrayList<>();
    preFilterQueryList.add(prepareConceptDefinitionComponentQuery());
    preFilterQueryList.add(prepareScopeQuery(scope));
    preFilterQueryList.add(prepareIdPreFilterQuery(List.of(codeSystem.getIdElement())));
    return QueryCombinators.prepareFilterQuery(preFilterQueryList);
  }

  /**
   * Builds Lucene queries for each property filter used in the {@code $find-matches} operation.
   *
   * <p>Handles special properties ({@code code}, {@code display}, {@code definition}) with
   * dedicated query logic, including case-sensitivity awareness from the code system. Other
   * properties are matched exactly based on their FHIR type ({@code string}, {@code code}, or
   * {@code Coding}).
   *
   * @param codeSystem the code system used to determine case-sensitivity behavior
   * @param propertyFilterList the property filters with codes and values to search for
   * @param exact whether to perform exact matching only (no partial/fuzzy results)
   * @return a list of pairs mapping property codes to their corresponding Lucene queries
   * @throws NotImplementedException if a property value uses an unsupported FHIR type
   */
  public static List<Pair<String, Query>> prepareFindMatchesPropertyQueries(
      CodeSystem codeSystem, List<PropertyFilter> propertyFilterList, boolean exact)
      throws NotImplementedException {

    // Wir müssen die Suchanfragen so feintunen, dass wir case-sensitive und case-insensitive
    // CodeSysteme anders behandeln. Dazu ist erforderlich zu wissen, welche Anforderungen das
    // CodeSystem stellt
    boolean isCaseSensitive = !codeSystem.hasCaseSensitive() || codeSystem.getCaseSensitive();

    List<Pair<String, Query>> queryList = new ArrayList<>();

    // Iterieren über alle Properties
    for (PropertyFilter propertyFilter : propertyFilterList) {
      if (propertyFilter.code().equals("code")
          && propertyFilter.value() instanceof CodeType codeType) {
        // Spezielle Behandlung einer 'code' Property
        Query codeQuery =
            prepareCodePropertyQuery(
                codeType.getValue(),
                isCaseSensitive,
                exact,
                propertyFilter.getExactMatchBoost(),
                propertyFilter.getPartialMatchBoost());
        queryList.add(Pair.of("code", codeQuery));

      } else if (propertyFilter.code().equals("display")
          && propertyFilter.value() instanceof StringType stringType) {
        // Spezielle Behandlung einer 'display' Property
        Query displayQuery =
            prepareDisplayPropertyQuery(
                stringType.getValue(),
                exact,
                propertyFilter.getExactMatchBoost(),
                propertyFilter.getPartialMatchBoost());
        queryList.add(Pair.of("display", displayQuery));

      } else if (propertyFilter.code().equals("definition")
          && propertyFilter.value() instanceof StringType stringType) {
        // Spezielle Behandlung einer 'definition' Property
        Query definitionQuery =
            prepareDefinitionPropertyQuery(
                stringType.getValue(),
                exact,
                propertyFilter.getExactMatchBoost(),
                propertyFilter.getPartialMatchBoost());
        queryList.add(Pair.of("definition", definitionQuery));
      } else {

        // Behandlung der restlichen Properties
        Query propertyQuery;
        switch (propertyFilter.value().fhirType()) {
          case "string":
            propertyQuery =
                new BoostQuery(
                    new ConstantScoreQuery(
                        prepareExactStringPropertyQuery(
                            propertyFilter.code(), propertyFilter.value().toString())),
                    propertyFilter.getExactMatchBoost());
            break;
          case "code":
            propertyQuery =
                new BoostQuery(
                    new ConstantScoreQuery(
                        prepareExactCodePropertyQuery(
                            propertyFilter.code(), propertyFilter.value().toString())),
                    propertyFilter.getExactMatchBoost());
            break;
          case "Coding":
            propertyQuery =
                new BoostQuery(
                    new ConstantScoreQuery(
                        prepareExactCodingPropertyQuery(
                            propertyFilter.code(), (Coding) propertyFilter.value())),
                    propertyFilter.getExactMatchBoost());
            break;
          default:
            throw new NotImplementedException(
                "Currently only a small subset of properties is supported");
        }
        queryList.add(Pair.of(propertyFilter.code(), propertyQuery));
      }
    }

    return queryList;
  }

  // =================================================================================================
  // $find-matches specific Helper Methods for generating Lucene queries
  // =================================================================================================

  /**
   * Builds a Lucene query for a code property, respecting the case-sensitivity of the code system.
   *
   * <p>For case-sensitive code systems, an exact term match is used. For case-insensitive systems,
   * the code is normalized to lowercase before matching. When {@code exact} is {@code false},
   * additional standard (prefix-based) queries are included for partial matching.
   *
   * @param code the code value to search for
   * @param isCaseSensitive whether the code system is case-sensitive
   * @param exact whether to restrict to exact matches only
   * @param scoreExact boost factor applied to exact match results
   * @param scorePartial boost factor applied to partial match results
   * @return a Lucene query combining exact and optional partial code matches
   */
  private static Query prepareCodePropertyQuery(
      String code, boolean isCaseSensitive, boolean exact, float scoreExact, float scorePartial) {

    List<Query> codePropertyQueryList = new ArrayList<>();

    if (isCaseSensitive) {
      // Fall 1: Case-sensitive CodeSysteme
      codePropertyQueryList.add(
          new BoostQuery(new ConstantScoreQuery(prepareExactCodeQuery(code)), scoreExact));
      if (!exact) {
        codePropertyQueryList.add(new BoostQuery(prepareStandardCodeQuery(code), scorePartial));
      }
    } else {
      // Fall 2: Case-insensitive CodeSysteme
      codePropertyQueryList.add(
          new BoostQuery(
              new ConstantScoreQuery(prepareExactCodeLowerCaseQuery((code.toLowerCase()))),
              scoreExact));
      if (!exact) {
        codePropertyQueryList.add(
            new BoostQuery(prepareStandardCodeLowerCaseQuery((code)), scorePartial));
      }
    }

    return QueryCombinators.prepareOrQuery(codePropertyQueryList);
  }

  /**
   * Builds a Lucene query for the definition property.
   *
   * <p>Always includes an exact match query. When {@code exact} is {@code false}, additional
   * standard and contains queries are included for partial matching.
   *
   * @param definition the definition text to search for
   * @param exact whether to restrict to exact matches only
   * @param scoreExact boost factor applied to exact match results
   * @param scorePartial boost factor applied to partial match results
   * @return a Lucene query combining exact and optional partial definition matches
   */
  private static Query prepareDefinitionPropertyQuery(
      String definition, boolean exact, float scoreExact, float scorePartial) {

    List<Query> codePropertyQueryList = new ArrayList<>();

    codePropertyQueryList.add(
        new BoostQuery(
            new ConstantScoreQuery(prepareExactStringQuery(NAME_DEFINITION, definition)),
            scoreExact));

    if (!exact) {
      Query standardQuery = prepareStandardStringQuery(NAME_DEFINITION, definition);
      Query containsQuery = prepareContainsModifierQuery(NAME_DEFINITION, definition);
      Query partialMatchQuery =
          new BoostQuery(
              new ConstantScoreQuery(
                  QueryCombinators.prepareOrQuery(List.of(standardQuery, containsQuery))),
              scorePartial);

      codePropertyQueryList.add(partialMatchQuery);
    }

    return QueryCombinators.prepareOrQuery(codePropertyQueryList);
  }

  /**
   * Builds a Lucene query for the display property, including optional synonym matching.
   *
   * <p>Always includes an exact display match. When {@code exact} is {@code false}, additional
   * standard and contains queries for both display and synonym fields are included.
   *
   * @param display the display text to search for
   * @param exact whether to restrict to exact matches only
   * @param exactScore boost factor applied to exact match results
   * @param partialScore boost factor applied to partial match results
   * @return a Lucene query combining exact and optional partial display/synonym matches
   */
  private static Query prepareDisplayPropertyQuery(
      String display, boolean exact, float exactScore, float partialScore) {

    List<Query> codePropertyQueryList = new ArrayList<>();

    codePropertyQueryList.add(
        new BoostQuery(new ConstantScoreQuery(prepareExactDisplayQuery(display)), exactScore));

    // Für den Fall, dass wir keine exakte Suche durchführen, fügen wir noch weitere Abfragen hinzu,
    // die ungenaue Ergebnisse liefern
    if (!exact) {
      Query standardDisplayQuery = prepareStandardDisplayQuery(display);
      Query containsDisplayQuery = prepareContainsDisplayQuery(display);
      Query standardSynonymQuery = prepareStandardSynonymQuery(display);
      Query containsSynonymQuery = prepareContainsSynonymQuery(display);

      Query partialMatchQuery =
          new BoostQuery(
              new ConstantScoreQuery(
                  QueryCombinators.prepareOrQuery(
                      List.of(
                          standardDisplayQuery,
                          containsDisplayQuery,
                          standardSynonymQuery,
                          containsSynonymQuery))),
              partialScore);

      codePropertyQueryList.add(partialMatchQuery);
    }

    return QueryCombinators.prepareOrQuery(codePropertyQueryList);
  }

  // =================================================================================================
  // Generic Helper Methods for generating Lucene queries
  // =================================================================================================

  /**
   * Builds a Lucene query that restricts results to CodeSystem concept entries only.
   *
   * @return a Lucene term query matching {@code ConceptDefinitionComponent} type entries
   */
  private static Query prepareConceptDefinitionComponentQuery() {

    String inField = RESOURCE_TYPE_FIELD;

    return new TermQuery(new Term(inField, "ConceptDefinitionComponent"));
  }

  /**
   * Builds a Lucene query that matches entries belonging to the specified scope.
   *
   * @param scope the scope identifier to filter by
   * @return a Lucene term query for the given scope
   */
  private static Query prepareScopeQuery(String scope) {
    return new TermQuery(new Term(NAME_SCOPE, scope));
  }

  /**
   * Builds a Lucene query that matches entries whose resource ID is contained in the given list.
   *
   * @param preFilter the list of resource IDs to filter by
   * @return a Lucene OR query matching any of the specified resource IDs
   */
  private static Query prepareIdPreFilterQuery(List<IdType> preFilter) {

    List<Query> orQueries = new ArrayList<>();
    for (IdType idType : preFilter) {
      String inField = NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT;

      orQueries.add(new TermQuery(new Term(inField, idType.getIdPart())));
    }

    return QueryCombinators.prepareOrQuery(orQueries);
  }

  /**
   * Builds a Lucene query for an exact, case-sensitive code match.
   *
   * @param code the code value to search for
   * @return a Lucene term query matching the code exactly
   */
  private static Query prepareExactCodeQuery(String code) {

    String inField = NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT;

    return new TermQuery(new Term(inField, code));
  }

  /**
   * Builds a Lucene query for a standard (prefix-based) code match.
   *
   * @param code the code value to search for
   * @return a Lucene prefix query for the code field
   */
  private static Query prepareStandardCodeQuery(String code) {
    return prepareStandardStringQuery(NAME_CODE, code);
  }

  /**
   * Builds a Lucene query for an exact code match using the lowercase-normalized code field,
   * supporting case-insensitive code systems.
   *
   * @param code the code value to search for (will be lowercased)
   * @return a Lucene term query matching the lowercased code exactly
   */
  private static Query prepareExactCodeLowerCaseQuery(String code) {

    String inField = NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_TOKEN_EXACT;

    return new TermQuery(new Term(inField, code.toLowerCase()));
  }

  /**
   * Builds a Lucene query for a standard (prefix-based) code match using the lowercase code field.
   *
   * @param code the code value to search for
   * @return a Lucene prefix query for the lowercase code field
   */
  private static Query prepareStandardCodeLowerCaseQuery(String code) {
    return prepareStandardStringQuery(NAME_CODE_LOWERCASE, code);
  }

  /**
   * Builds a Lucene query for an exact display text match.
   *
   * @param display the display text to search for
   * @return a Lucene term query matching the display exactly
   */
  private static Query prepareExactDisplayQuery(String display) {
    return prepareExactStringQuery(NAME_DISPLAY, display);
  }

  /**
   * Builds a Lucene query for a standard (prefix-based) display text match.
   *
   * @param display the display text to search for
   * @return a Lucene prefix query for the display field
   */
  private static Query prepareStandardDisplayQuery(String display) {
    return prepareStandardStringQuery(NAME_DISPLAY, display);
  }

  /**
   * Builds a Lucene query for a contains-based display text match.
   *
   * @param display the display text to search for
   * @return a Lucene query matching the display text anywhere in the field
   */
  private static Query prepareContainsDisplayQuery(String display) {
    return prepareContainsModifierQuery(NAME_DISPLAY, display);
  }

  /**
   * Builds a Lucene query for a standard (prefix-based) synonym text match.
   *
   * @param synonym the synonym text to search for
   * @return a Lucene prefix query for the synonym field
   */
  private static Query prepareStandardSynonymQuery(String synonym) {
    return prepareStandardStringQuery(NAME_SYNONYM, synonym);
  }

  /**
   * Builds a Lucene query for a contains-based synonym text match.
   *
   * @param synonym the synonym text to search for
   * @return a Lucene query matching the synonym text anywhere in the field
   */
  private static Query prepareContainsSynonymQuery(String synonym) {
    return prepareContainsModifierQuery(NAME_SYNONYM, synonym);
  }

  /**
   * Builds a Lucene query for an exact string match on the specified field.
   *
   * @param stringProperty the field name to search in
   * @param propertyValue the exact value to match
   * @return a Lucene term query for the exact string field
   */
  private static Query prepareExactStringQuery(String stringProperty, String propertyValue) {

    String inField = stringProperty + SEPARATOR + SUFFIX_STRING_EXACT;

    return new TermQuery(new Term(inField, propertyValue));
  }

  /**
   * Builds a Lucene query for a standard (prefix-based) string match on the specified field.
   *
   * <p>The property value is normalized using {@link AnalyzerUtils#normalizePhrase(String)} before
   * constructing a prefix query.
   *
   * @param stringProperty the field name to search in
   * @param propertyValue the value to match as a prefix
   * @return a Lucene prefix query for the standard string field
   */
  private static Query prepareStandardStringQuery(String stringProperty, String propertyValue) {

    String inField = stringProperty + SEPARATOR + SUFFIX_STRING_STANDARD;

    String termString = AnalyzerUtils.normalizePhrase(propertyValue);
    return new PrefixQuery(new Term(inField, termString));
  }

  /**
   * Builds a Lucene query for an exact string match on a named concept property.
   *
   * @param propertyName the property name to search in
   * @param propertyValue the exact value to match
   * @return a Lucene term query for the property's exact string field
   */
  private static Query prepareExactStringPropertyQuery(String propertyName, String propertyValue) {

    String inField =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_STRING_EXACT;

    return new TermQuery(new Term(inField, propertyValue));
  }

  /**
   * Builds a Lucene query for an exact code match on a named concept property.
   *
   * Delegates to the string property query builder because
   * both property types use the same Lucene field structure.
   * The separate method preserves the distinction between
   * FHIR code and string properties.
   *
   * @param propertyName the property name to search in
   * @param propertyValue the exact code value to match
   * @return a Lucene term query for the property's exact string field
   */
  private static Query prepareExactCodePropertyQuery(String propertyName, String propertyValue) {
    return prepareExactStringPropertyQuery(propertyName, propertyValue);
  }

  /**
   * Builds a Lucene query for an exact match on a {@link Coding}-typed concept property.
   *
   * <p>The query encodes the Coding's system, version, and code into a composite token format
   * ({@code system|version#code}) for exact matching. At least one of system, version, or code must
   * be present.
   *
   * @param propertyName the property name to search in
   * @param propertyValue the Coding value to match
   * @return a Lucene term query for the property's exact token field
   * @throws NotImplementedException if none of system, version, or code is provided
   */
  private static Query prepareExactCodingPropertyQuery(String propertyName, Coding propertyValue)
      throws NotImplementedException {

    String inField =
        NAME_PREFIX_PROPERTY + SEPARATOR + propertyName + SEPARATOR + SUFFIX_TOKEN_EXACT;

    String system = propertyValue.getSystem();
    String version = propertyValue.getVersion();
    String code = propertyValue.getCode();

    if (system != null && version != null && code != null) {
      return new TermQuery(new Term(inField, system + "|" + version + "#" + code));
    } else if (system != null && code != null) {
      return new TermQuery(new Term(inField, system + "|#" + code));
    } else if (system != null && version != null) {
      return new TermQuery(new Term(inField, system + "|" + version + "#"));
    } else if (system != null) {
      return new TermQuery(new Term(inField, system + "|#"));
    } else if (version != null && code != null) {
      return new TermQuery(new Term(inField, "|" + version + "#" + code));
    } else if (version != null) {
      return new TermQuery(new Term(inField, "|" + version + "#"));
    } else if (code != null) {
      return new TermQuery(new Term(inField, "|#" + code));
    } else {
      throw new NotImplementedException(
          "Coding property search not supported in the provided Coding constellation");
    }
  }

  /**
   * Builds a Lucene query implementing the FHIR {@code :contains} modifier search behavior.
   *
   * <p>Performs a case-insensitive and combining character-insensitive search where the search text
   * is matched anywhere in the field. Each term in the input is normalized, escaped for Lucene
   * special characters, and combined with wildcard suffixes.
   *
   * @param paramName the field name to search in
   * @param param the search text to match
   * @return a Lucene query matching the text anywhere within the specified field
   * @throws InvalidRequestException if the input contains unsafe patterns or is empty after
   *     sanitization
   */
  private static Query prepareContainsModifierQuery(String paramName, String param) {

    // The :contains modifier returns results that include the supplied parameter value anywhere
    // within the field being searched.
    // (case insensitive and combining character-insensitive, search text matched anywhere in the
    // string)

    String inField = paramName + SEPARATOR + SUFFIX_STRING_CONTAINS;

    // Validate input safety before processing
    if (!LuceneQuerySanitizer.isSafeInput(param)) {
      throw new InvalidRequestException("Query contains unsafe patterns");
    }

    // Normalize the phrase
    String termString = AnalyzerUtils.normalizePhrase(param);
    String[] terms = termString.split("\\s+"); // Use regex for better whitespace handling

    // Build query string with wildcards
    StringBuilder queryBuilder = new StringBuilder();
    for (String term : terms) {
      if (StringUtils.isNotBlank(term)) {
        // Escape special Lucene characters in each term before adding wildcard
        String safeTerm = LuceneQuerySanitizer.escapeLuceneSpecialChars(term);
        queryBuilder.append("+").append(safeTerm).append("* ");
      }
    }

    String queryString = queryBuilder.toString().trim();

    if (StringUtils.isBlank(queryString)) {
      throw new InvalidRequestException("Empty query after sanitization");
    }

    Query query;
    try {
      query = new QueryParser(inField, new FhirStringAnalyzerMultipleToken()).parse(queryString);
      log.debug("Created contains query: field={}, query={}", inField, queryString);
    } catch (ParseException e) {
      log.error("Failed to parse query: {}", queryString, e);
      throw new InvalidRequestException("Query not supported by Server: " + e.getMessage());
    }

    return query;
  }
}
