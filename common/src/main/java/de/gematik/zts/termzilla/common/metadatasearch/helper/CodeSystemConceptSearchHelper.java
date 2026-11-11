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
package de.gematik.zts.termzilla.common.metadatasearch.helper;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DISPLAY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_TOKEN_EXACT;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.helper.model.ScoredCoding;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ConceptQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;

/**
 * Utility class for building and executing Lucene queries against the concept index of a code
 * system.
 *
 * <p>Provides methods to construct queries based on code or content search parameters, execute them
 * against a Lucene {@link IndexSearcher}, and return results as resource IDs or scored codings.
 */
@Slf4j
public class CodeSystemConceptSearchHelper {

  private CodeSystemConceptSearchHelper() {
    /* This utility class should not be instantiated */
  }

  /**
   * Builds a Lucene query that searches for code system concepts matching the given code parameter.
   *
   * <p>The query is scoped to the given tenant and optionally restricted to a pre-filtered set of
   * resource IDs.
   *
   * @param scope the tenant scope to restrict the search to
   * @param codeParam the code token to search for
   * @param idPreFilter an optional list of resource IDs to restrict the search to
   * @return a combined Lucene query ready for execution
   * @throws InvalidRequestException if the query parameters are invalid
   * @throws NotImplementedException if a required query feature is not yet implemented
   */
  public static Query prepareCodeSystemConceptQuery(
      String scope, TokenParam codeParam, List<IdType> idPreFilter)
      throws InvalidRequestException, NotImplementedException {

    // Filter-Query, um die Suche auf den jeweiligen Scope, Ressourcentyp und IdPreFilter zu
    // beschränken
    Query filterQuery =
        FhirSearchHelper.prepareFilterQuery(scope, "ConceptDefinitionComponent", idPreFilter);

    // Vorbereiten der Lucene Query
    Query codeQuery = ConceptQueryGenerator.prepareCodeParamQuery(codeParam);

    // Kombination der Filter- und Metadaten-Query zurückgeben
    return QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, codeQuery);
  }

  /**
   * Builds a Lucene query that searches for code system concepts matching the given content
   * parameter.
   *
   * <p>Unlike {@link #prepareCodeSystemConceptQuery}, this query is not restricted to a specific
   * resource type, enabling cross-type full-text search within the given scope.
   *
   * @param contentParam the content string to search for
   * @param scope the tenant scope to restrict the search to
   * @param idPreFilter an optional list of resource IDs to restrict the search to
   * @return a combined Lucene query ready for execution
   * @throws InvalidRequestException if the query parameters are invalid
   * @throws NotImplementedException if a required query feature is not yet implemented
   */
  public static Query prepareCodeSystemContentQuery(
      StringParam contentParam, String scope, List<IdType> idPreFilter)
      throws InvalidRequestException, NotImplementedException {

    // Filter-Query um die Suche auf den jeweiligen Scope, Ressourcentyp und IdPreFilter zu
    // beschränken. Hier nutzen wir keine Ressourcentypfilterung, da wir Typübergreifend suchen
    // möchten.
    Query filterQuery = FhirSearchHelper.prepareFilterQuery(scope, null, idPreFilter);

    // Vorbereiten und Ausführen der Lucene Query
    Query contentQuery = ConceptQueryGenerator.prepareContentParamQuery(contentParam);

    // Kombination der Filter- und Conten-Query zurückgeben
    return QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, contentQuery);
  }

  // ================================================================================================

  /**
   * Executes the given Lucene query and returns the matching resource IDs as a deduplicated list.
   *
   * @param conceptSearcher the Lucene index searcher to execute the query against
   * @param query the Lucene query to execute
   * @return a list of {@link IdType} representing the matched code system resource IDs
   * @throws InvalidRequestException if the query execution or document loading fails
   */
  public static List<IdType> executeCodeSystemConceptQuery(
      IndexSearcher conceptSearcher, Query query) throws InvalidRequestException {

    log.debug("{}", query);

    try {

      Set<String> resultSet = new HashSet<>();
      int hitsPerPage = 100000;

      TopDocs topDocs = conceptSearcher.search(query, hitsPerPage);
      Stream.of(topDocs.scoreDocs)
          .map(
              scoreDoc -> {
                try {
                  return conceptSearcher.storedFields().document(scoreDoc.doc); // Dokumente laden
                } catch (Exception e) {
                  throw new InvalidRequestException("Error while loading Lucene documents: ", e);
                }
              })
          .forEach(
              doc -> {
                log.debug("Lucene Document: {}", doc.toString());
                String id = doc.getField(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT).stringValue();
                resultSet.add(id);
              });

      List<IdType> csList = new ArrayList<>();
      resultSet.forEach(id -> csList.add(new IdType(id)));

      log.debug("{}", csList);

      return csList;

    } catch (IOException e) {
      throw new InvalidRequestException("Error while executing Lucene Query: ", e);
    }
  }

  // ====================================================================================================

  /**
   * Executes the given Lucene query and returns the results as a list of {@link ScoredCoding}
   * instances, each containing the concept code, display value, and the Lucene relevance score.
   *
   * @param query the Lucene query to execute
   * @param conceptSearcher the Lucene index searcher to execute the query against
   * @return a list of scored codings representing the matched concepts
   * @throws InvalidRequestException if the query execution or document loading fails
   */
  public static List<ScoredCoding> processQueryAndReturnConceptSet(
      Query query, IndexSearcher conceptSearcher) throws InvalidRequestException {

    log.debug("{}", query);

    try {

      List<ScoredCoding> resultSet = new ArrayList<>();
      int hitsPerPage = 100000;

      TopDocs topDocs = conceptSearcher.search(query, hitsPerPage);

      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        extractResults(conceptSearcher, scoreDoc, resultSet);
      }

      return resultSet;

    } catch (IOException e) {
      throw new InvalidRequestException("Error while executing Lucene Query: ", e);
    }
  }

  /**
   * Extracts the code and display values from a single Lucene search hit and adds a {@link
   * ScoredCoding} to the result list.
   *
   * @param conceptSearcher the Lucene index searcher used to load the stored document fields
   * @param scoreDoc the Lucene score document representing the search hit
   * @param resultSet the list to add the extracted scored coding to
   * @throws InvalidRequestException if the document cannot be loaded from the index
   */
  private static void extractResults(
      IndexSearcher conceptSearcher, ScoreDoc scoreDoc, List<ScoredCoding> resultSet) {
    try {
      float score = scoreDoc.score;
      Document doc = conceptSearcher.storedFields().document(scoreDoc.doc); // Dokumente laden
      String code = doc.getField(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT).stringValue();
      // TODO: Ist vielleicht nicht immer sinnvoll, den Display-Wert aus dem Index zu nehmen, da
      // dieser nicht
      //  immer die beste Darstellung sein muss (z.B. in Bezug auf bestimmte Sprachvorlieben).
      // Aktuell rendern
      //  wir den Display-Wert nochmal neu. Vielleicht kann die Übernahme an dieser Stelle
      // entfallen.
      String display = doc.getField(NAME_DISPLAY + SEPARATOR + SUFFIX_STRING_EXACT).stringValue();
      log.debug("{} {} {}", score, code, display);
      resultSet.add(new ScoredCoding(new Coding().setCode(code).setDisplay(display), score));

    } catch (Exception e) {
      throw new InvalidRequestException("Error while loading Lucene documents: ", e);
    }
  }
}
