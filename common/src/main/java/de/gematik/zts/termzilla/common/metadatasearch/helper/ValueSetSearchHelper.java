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

import static de.gematik.zts.termzilla.common.metadatasearch.helper.CommonParametersSearchHelper.addReferenceParam;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_URI_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ValueSetFieldNames.NAME_REFERENCE;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.exceptions.LuceneDocumentRetrievalException;
import de.gematik.zts.termzilla.common.metadatasearch.exceptions.LuceneIndexSearchException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.CombinedParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;

/**
 * Utility class for building and executing Lucene queries for FHIR ValueSet resource searches.
 *
 * <p>Constructs composite queries combining scope/type filter conditions with FHIR search parameter
 * conditions. Supports standard terminology resource parameters as well as the ValueSet-specific
 * {@code reference} parameter.
 *
 * @see FhirSearchHelper
 * @see TerminologyResourceSearchHelper
 * @see VsSearchParameter
 */
@Slf4j
public class ValueSetSearchHelper {

  private ValueSetSearchHelper() {
    /* This utility class should not be instantiated */
  }

  /** Lucene field name for exact {@code ValueSet.url} URI matching. */
  public static final String VS_URL_FIELD = NAME_URL + SEPARATOR + SUFFIX_URI_EXACT;

  /** Lucene field name for exact {@code ValueSet.version} matching. */
  public static final String VS_VERSION_FIELD = NAME_VERSION + SEPARATOR + SUFFIX_TOKEN_EXACT;

  /** Shared instance for combining multiple search parameter queries with AND logic. */
  public static final CombinedParameterQueryGenerator combinedParameterQueryGenerator =
      new CombinedParameterQueryGenerator();

  /**
   * Builds a Lucene query for searching ValueSet resources by combining a scope/type filter with
   * metadata search parameter conditions.
   *
   * @param vsSearchParameter the ValueSet search parameters
   * @param scope the scope identifier for multi-tenancy isolation
   * @param idPreFilter optional list of resource IDs to pre-filter results
   * @return a Lucene {@link Query} combining filter and metadata search conditions
   * @throws InvalidRequestException if the search parameters contain invalid values
   * @throws NotImplementedException if unsupported search parameters or modifiers are used
   */
  public static Query prepareMetadataQuery(
      VsSearchParameter vsSearchParameter, String scope, List<IdType> idPreFilter)
      throws InvalidRequestException, NotImplementedException {

    // Filter-Query um die Suche auf den jeweiligen Scope, Ressourcentyp und IdPreFilter zu
    // beschränken
    Query filterQuery = FhirSearchHelper.prepareFilterQuery(scope, "ValueSet", idPreFilter);

    // Suchparameter vorbereiten und daraus die Suchanfrage generieren
    List<ParamData> searchParamDataList = prepareParamDataList(vsSearchParameter);
    Query metadateQuery = combinedParameterQueryGenerator.prepareAndListQuery(searchParamDataList);

    // Kombination der Filter- und Metadaten-Query zurückgeben
    return QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, metadateQuery);
  }

  /**
   * Converts ValueSet search parameters into a list of {@link ParamData} objects for Lucene query
   * generation.
   *
   * <p>Delegates standard terminology resource parameters to {@link
   * TerminologyResourceSearchHelper} and handles the ValueSet-specific {@code reference} parameter
   * directly.
   *
   * @param vsSearchParameter the ValueSet search parameters
   * @return a list of {@link ParamData} objects for all active search conditions
   */
  public static List<ParamData> prepareParamDataList(VsSearchParameter vsSearchParameter) {

    List<ParamData> searchParamList = new ArrayList<>();

    TerminologyResourceSearchHelper.prepareIdParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareSourceParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareUrlParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareVersionParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDateParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareEffectiveParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareStatusParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareNameParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareTitleParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDescriptionParam(vsSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.preparePublisherParam(vsSearchParameter, searchParamList);

    prepareReferenceParam(vsSearchParameter, searchParamList);

    return searchParamList;
  }

  /**
   * Adds the {@code reference} search parameter as a canonical reference query, if present.
   *
   * @param vsSearchParameter the ValueSet search parameters
   * @param searchParamList the list to add the parameter to
   */
  private static void prepareReferenceParam(
      VsSearchParameter vsSearchParameter, List<ParamData> searchParamList) {
    var uriParamOpt = vsSearchParameter.getReference();

    // if the reference parameter is present, prepare it
    // TODO we are converting to a reference param here ... is that okay?
    // we could search for an uri otherwise, but therefore ore work needs to be done to support uri
    // + version search
    uriParamOpt.ifPresent(
        uriParam ->
            addReferenceParam(
                Optional.of(
                    (ReferenceParam)
                        new ReferenceParam(uriParam.getValue()).setMissing(uriParam.getMissing())),
                NAME_REFERENCE,
                searchParamList));
  }

  // ================================================================================================
  // Query Execution
  // ================================================================================================

  /**
   * Executes a Lucene query against the ValueSet index and returns matching resource IDs.
   *
   * @param indexSearcher the Lucene {@link IndexSearcher} to execute the query against
   * @param query the prepared Lucene {@link Query}
   * @return list of {@link IdType} objects representing the IDs of matching ValueSet resources
   * @throws InvalidRequestException if the query execution fails due to invalid parameters
   * @throws RuntimeException if an I/O error occurs during query execution
   */
  public static List<IdType> executeQuery(IndexSearcher indexSearcher, Query query)
      throws InvalidRequestException {

    List<IdType> csIdList = new ArrayList<>();

    try {

      log.debug("{}", query);

      int hitsPerPage = Integer.MAX_VALUE;

      var topDocs = indexSearcher.search(query, hitsPerPage);
      Stream.of(topDocs.scoreDocs)
          .map(
              scoreDoc -> {
                try {
                  return indexSearcher.storedFields().document(scoreDoc.doc); // Dokumente laden
                } catch (Exception e) {
                  throw new LuceneDocumentRetrievalException(e);
                }
              })
          .forEach(
              doc -> {
                String id = null;
                String url = null;
                String version = null;
                if (doc.getField(FhirSearchHelper.RESOURCE_ID_FIELD) != null) {
                  id = doc.getField(FhirSearchHelper.RESOURCE_ID_FIELD).stringValue();
                }
                if (doc.getField(VS_URL_FIELD) != null) {
                  url = doc.getField(VS_URL_FIELD).stringValue();
                }
                if (doc.getField(VS_VERSION_FIELD) != null) {
                  version = doc.getField(VS_VERSION_FIELD).stringValue();
                }
                log.debug("ValueSet: {} {} | {} --> {}", id, url, version);

                csIdList.add(new IdType(id));
              });

      log.debug("{}", csIdList);

      return csIdList;

    } catch (IOException e) {
      throw new LuceneIndexSearchException(e);
    }
  }
}
