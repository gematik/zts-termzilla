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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_URI;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_URI;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.SUFFIX_URI_EXACT;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
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
 * Utility class for building and executing Lucene queries for FHIR ConceptMap resource searches.
 *
 * <p>Constructs composite queries combining scope/type filter conditions with FHIR search parameter
 * conditions. Supports standard terminology resource parameters as well as ConceptMap-specific
 * parameters (source, source-uri, source-system, target, target-uri, target-system).
 *
 * @see FhirSearchHelper
 * @see TerminologyResourceSearchHelper
 * @see CmSearchParameter
 */
@Slf4j
public class ConceptMapSearchHelper {

  private ConceptMapSearchHelper() {
    /* This utility class should not be instantiated */
  }

  /** Lucene field name for exact {@code ConceptMap.url} URI matching. */
  public static final String CM_URL_FIELD = NAME_URL + SEPARATOR + SUFFIX_URI_EXACT;

  /** Lucene field name for exact {@code ConceptMap.version} matching. */
  public static final String CM_VERSION_FIELD = NAME_VERSION + SEPARATOR + SUFFIX_TOKEN_EXACT;

  /** Shared instance for combining multiple search parameter queries with AND logic. */
  public static final CombinedParameterQueryGenerator combinedParameterQueryGenerator =
      new CombinedParameterQueryGenerator();

  /**
   * Builds a Lucene query for searching ConceptMap resources by combining a scope/type filter with
   * metadata search parameter conditions.
   *
   * @param cmSearchParameter the ConceptMap search parameters
   * @param scope the scope identifier for multi-tenancy isolation
   * @param idPreFilter optional list of resource IDs to pre-filter results
   * @return a Lucene {@link Query} combining filter and metadata search conditions
   * @throws InvalidRequestException if the search parameters contain invalid values
   * @throws NotImplementedException if unsupported search parameters or modifiers are used
   */
  public static Query prepareMetadataQuery(
      CmSearchParameter cmSearchParameter, String scope, List<IdType> idPreFilter)
      throws InvalidRequestException, NotImplementedException {

    // Build filter query to restrict search by scope, resource type, and ID pre-filter
    Query filterQuery = FhirSearchHelper.prepareFilterQuery(scope, "ConceptMap", idPreFilter);

    // Prepare search parameters and generate the search query
    List<ParamData> searchParamDataList = prepareParamDataList(cmSearchParameter);
    Query metadateQuery = combinedParameterQueryGenerator.prepareAndListQuery(searchParamDataList);

    // Return combined filter and metadata query
    return QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, metadateQuery);
  }

  /**
   * Converts ConceptMap search parameters into a list of {@link ParamData} objects for Lucene query
   * generation.
   *
   * <p>Delegates standard terminology resource parameters to {@link
   * TerminologyResourceSearchHelper} and handles ConceptMap-specific parameters (source,
   * source-uri, source-system, target, target-uri, target-system) directly.
   *
   * @param cmSearchParameter the ConceptMap search parameters
   * @return a list of {@link ParamData} objects for all active search conditions
   */
  public static List<ParamData> prepareParamDataList(CmSearchParameter cmSearchParameter) {

    List<ParamData> searchParamList = new ArrayList<>();

    TerminologyResourceSearchHelper.prepareIdParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareSourceParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareUrlParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareVersionParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDateParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareEffectiveParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareStatusParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareNameParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareTitleParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDescriptionParam(cmSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.preparePublisherParam(cmSearchParameter, searchParamList);

    // source ConceptMap.source as canonical (VS)
    addReferenceParam(
        cmSearchParameter.getSourceCanonical(), NAME_SOURCE_CANONICAL, searchParamList);
    // source-uri (type reference) ConceptMap.source as uri
    addReferenceParam(cmSearchParameter.getSourceUri(), NAME_SOURCE_URI, searchParamList);
    // source-system (type uri) ConceptMap.group.source
    var sourceSystemOpt = cmSearchParameter.getSourceSystem();
    // TODO we are converting to a reference param here ... is that okay?
    // we could search for an uri otherwise, but therefore ore work needs to be done to support uri
    // + version search
    sourceSystemOpt.ifPresent(
        uriParam ->
            addReferenceParam(
                Optional.of(
                    (ReferenceParam)
                        new ReferenceParam(uriParam.getValue()).setMissing(uriParam.getMissing())),
                NAME_SOURCE_SYSTEM,
                searchParamList));

    // target ConceptMap.target as canonical (VS)
    addReferenceParam(
        cmSearchParameter.getTargetCanonical(), NAME_TARGET_CANONICAL, searchParamList);
    // target-uri (type reference) ConceptMap.target as uri
    addReferenceParam(cmSearchParameter.getTargetUri(), NAME_TARGET_URI, searchParamList);

    // target-system (type uri) ConceptMap.group.target
    var targetSystemOpt = cmSearchParameter.getTargetSystem();
    // TODO we are converting to a reference param here ... is that okay?
    // we could search for an uri otherwise, but therefore ore work needs to be done to support uri
    // + version search
    targetSystemOpt.ifPresent(
        uriParam ->
            addReferenceParam(
                Optional.of(
                    (ReferenceParam)
                        new ReferenceParam(uriParam.getValue()).setMissing(uriParam.getMissing())),
                NAME_TARGET_SYSTEM,
                searchParamList));

    // TODO source-code?
    // TODO target-code?

    return searchParamList;
  }

  // ================================================================================================
  // Query Execution
  // ================================================================================================

  /**
   * Executes a Lucene query against the ConceptMap index and returns matching resource IDs.
   *
   * @param indexSearcher the Lucene {@link IndexSearcher} to execute the query against
   * @param query the prepared Lucene {@link Query}
   * @return list of {@link IdType} objects representing the IDs of matching ConceptMap resources
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
                  return indexSearcher.storedFields().document(scoreDoc.doc);
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
                if (doc.getField(CM_URL_FIELD) != null) {
                  url = doc.getField(CM_URL_FIELD).stringValue();
                }
                if (doc.getField(CM_VERSION_FIELD) != null) {
                  version = doc.getField(CM_VERSION_FIELD).stringValue();
                }
                log.debug("ConceptMap: {} {} | {} --> {}", id, url, version);

                csIdList.add(new IdType(id));
              });

      log.info("{}", csIdList);

      return csIdList;

    } catch (IOException e) {
      throw new LuceneIndexSearchException(e);
    }
  }
}
