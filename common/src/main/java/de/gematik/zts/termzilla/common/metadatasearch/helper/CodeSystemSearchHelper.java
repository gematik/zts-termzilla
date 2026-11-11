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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_CONTENT_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SUPPLEMENTS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_URI_EXACT;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.exceptions.LuceneDocumentRetrievalException;
import de.gematik.zts.termzilla.common.metadatasearch.exceptions.LuceneIndexSearchException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.CombinedParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;

/**
 * Utility class for building and executing Lucene queries for FHIR CodeSystem resource searches.
 *
 * <p>Constructs composite queries combining scope/type filter conditions with FHIR search parameter
 * conditions. Supports standard terminology resource parameters (_id, url, version, name, title,
 * description, publisher, status, date, effective) as well as CodeSystem-specific parameters
 * (system, content-mode, supplements, source).
 *
 * @see FhirSearchHelper
 * @see TerminologyResourceSearchHelper
 * @see CsSearchParameter
 */
@Slf4j
public final class CodeSystemSearchHelper {

  /** Lucene field name for exact {@code CodeSystem.system} URI matching. */
  public static final String CS_SYSTEM_FIELD = NAME_SYSTEM + SEPARATOR + SUFFIX_URI_EXACT;

  /** Lucene field name for exact {@code CodeSystem.version} matching. */
  public static final String CS_VERSION_FIELD = NAME_VERSION + SEPARATOR + SUFFIX_TOKEN_EXACT;

  /** Shared instance for combining multiple search parameter queries with AND logic. */
  private static final CombinedParameterQueryGenerator COMBINED_PARAMETER_QUERY_GENERATOR =
      new CombinedParameterQueryGenerator();

  /** Private constructor to prevent instantiation. */
  private CodeSystemSearchHelper() {
    throw new AssertionError(
        "CodeSystemSearchHelper is a utility class and should not be instantiated");
  }

  /**
   * Builds a Lucene query for searching CodeSystem resources by combining a scope/type filter with
   * metadata search parameter conditions.
   *
   * @param csSearchParameter the CodeSystem search parameters
   * @param scope the scope identifier for multi-tenancy isolation
   * @param idPreFilter optional list of resource IDs to pre-filter results
   * @param isCapabilitySearch whether to use capability-based filtering (excludes provision filter)
   * @return a Lucene {@link Query} combining filter and metadata search conditions
   * @throws InvalidRequestException if the search parameters contain invalid values
   * @throws NotImplementedException if unsupported search parameters or modifiers are used
   */
  public static Query prepareMetadataQuery(
      CsSearchParameter csSearchParameter,
      String scope,
      List<IdType> idPreFilter,
      boolean isCapabilitySearch)
      throws InvalidRequestException, NotImplementedException {

    // Build filter query to restrict search by scope, resource type, and ID pre-filter
    Query filterQuery =
        isCapabilitySearch
            ? FhirSearchHelper.prepareFilterQueryWithoutProvision(scope, "CodeSystem", idPreFilter)
            : FhirSearchHelper.prepareFilterQuery(scope, "CodeSystem", idPreFilter);
    log.debug("CodeSystem filter query: {}", filterQuery);

    // Prepare search parameters and generate the search query
    List<ParamData> searchParamDataList = prepareParamDataList(csSearchParameter);
    Query metadateQuery =
        COMBINED_PARAMETER_QUERY_GENERATOR.prepareAndListQuery(searchParamDataList);
    log.debug("CodeSystem metadata query: {}", metadateQuery);

    // Return combined filter and metadata query
    return QueryCombinators.prepareCombinedFilterMustQuery(filterQuery, metadateQuery);
  }

  /**
   * Converts CodeSystem search parameters into a list of {@link ParamData} objects for Lucene query
   * generation.
   *
   * <p>Delegates standard terminology resource parameters to {@link
   * TerminologyResourceSearchHelper} and handles CodeSystem-specific parameters (system,
   * content-mode, supplements) directly.
   *
   * @param csSearchParameter the CodeSystem search parameters
   * @return a list of {@link ParamData} objects for all active search conditions
   */
  public static List<ParamData> prepareParamDataList(CsSearchParameter csSearchParameter) {

    List<ParamData> searchParamList = new ArrayList<>();

    // Standard terminology resource parameters
    TerminologyResourceSearchHelper.prepareIdParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareSourceParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareUrlParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareVersionParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDateParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareEffectiveParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareStatusParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareNameParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareTitleParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.prepareDescriptionParam(csSearchParameter, searchParamList);
    TerminologyResourceSearchHelper.preparePublisherParam(csSearchParameter, searchParamList);

    // CodeSystem-specific parameters
    prepareSystemParam(csSearchParameter, searchParamList);
    prepareContentModeParam(csSearchParameter, searchParamList);
    prepareSupplementsParam(csSearchParameter, searchParamList);

    return searchParamList;
  }

  /**
   * Adds the {@code supplements} search parameter as a canonical reference query, if present.
   *
   * @param csSearchParameter the CodeSystem search parameters
   * @param searchParamList the list to add the parameter to
   */
  private static void prepareSupplementsParam(
      CsSearchParameter csSearchParameter, List<ParamData> searchParamList) {

    csSearchParameter
        .getSupplements()
        .ifPresent(
            supplements -> {
              ReferenceAndListParam supplementsParam =
                  new ReferenceAndListParam().addAnd(new ReferenceOrListParam().addOr(supplements));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_SUPPLEMENTS)
                      .paramQueryType(ParamData.CANONICAL_REFERENCE_PARAM)
                      .param(supplementsParam)
                      .build());
            });
  }

  /**
   * Adds the {@code content-mode} search parameter as a code token query, if present.
   *
   * @param csSearchParameter the CodeSystem search parameters
   * @param searchParamList the list to add the parameter to
   */
  private static void prepareContentModeParam(
      CsSearchParameter csSearchParameter, List<ParamData> searchParamList) {

    csSearchParameter
        .getContentMode()
        .ifPresent(
            contentMode -> {
              TokenAndListParam contentModeParam = new TokenAndListParam().addAnd(contentMode);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_CONTENT_MODE)
                      .paramQueryType(ParamData.CODE_TOKEN_PARAM)
                      .param(contentModeParam)
                      .build());
            });
  }

  /**
   * Adds the {@code system} search parameter as a URI query, if present.
   *
   * @param csSearchParameter the CodeSystem search parameters
   * @param searchParamList the list to add the parameter to
   */
  private static void prepareSystemParam(
      CsSearchParameter csSearchParameter, List<ParamData> searchParamList) {

    csSearchParameter
        .getSystem()
        .ifPresent(
            system -> {
              UriAndListParam systemParam =
                  new UriAndListParam().addAnd(new UriOrListParam().addOr(system));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_SYSTEM)
                      .paramQueryType(ParamData.URI_PARAM)
                      .param(systemParam)
                      .build());
            });
  }

  // ================================================================================================
  // Query Execution
  // ================================================================================================

  /**
   * Executes a Lucene query against the CodeSystem index and returns matching resource IDs.
   *
   * @param indexSearcher the Lucene {@link IndexSearcher} to execute the query against
   * @param query the prepared Lucene {@link Query}
   * @return list of {@link IdType} objects representing the IDs of matching CodeSystem resources
   * @throws InvalidRequestException if sort parameters are invalid
   * @throws RuntimeException if an I/O error occurs during query execution
   */
  public static List<IdType> executeQuery(IndexSearcher indexSearcher, Query query)
      throws InvalidRequestException {

    List<IdType> csIdList = new ArrayList<>();

    try {

      log.debug("Executing CodeSystem query: {}", query);

      int hitsPerPage = Integer.MAX_VALUE;

      // Execute query
      var topDocs = indexSearcher.search(query, hitsPerPage);

      // Process results: load documents and extract IDs
      Stream.of(topDocs.scoreDocs)
          .map(
              scoreDoc -> {
                try {
                  return indexSearcher.storedFields().document(scoreDoc.doc);
                } catch (Exception e) {
                  throw new LuceneDocumentRetrievalException(
                      "Failed to load document from index", e);
                }
              })
          .forEach(
              doc -> {
                String id = null;

                // Extract field values from document and add to result list
                if (doc.getField(FhirSearchHelper.RESOURCE_ID_FIELD) != null) {
                  id = doc.getField(FhirSearchHelper.RESOURCE_ID_FIELD).stringValue();
                  csIdList.add(new IdType(id));
                }

                if (log.isDebugEnabled()) {
                  buildDebugLog(doc, id);
                }
              });

      log.debug("Query returned {} CodeSystem(s)", csIdList.size());

      return csIdList;

    } catch (IOException e) {
      throw new LuceneIndexSearchException("Failed to execute CodeSystem search query", e);
    }
  }

  private static void buildDebugLog(Document doc, String id) {
    String system = null;
    String version = null;
    List<String> scopeList = new ArrayList<>();

    if (doc.getField(CS_SYSTEM_FIELD) != null) {
      system = doc.getField(CS_SYSTEM_FIELD).stringValue();
    }

    if (doc.getField(CS_VERSION_FIELD) != null) {
      version = doc.getField(CS_VERSION_FIELD).stringValue();
    }

    for (IndexableField scopeField : doc.getFields(NAME_SCOPE)) {
      scopeList.add(scopeField.stringValue());
    }

    log.debug(
        "Found CodeSystem: id={}, system={}, version={}, scopeList={}",
      id,
        system,
        version,
        scopeList);
  }
}
