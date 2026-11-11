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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_PROVISION_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_TYPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.SUFFIX_TOKEN_EXACT;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hl7.fhir.r4.model.IdType;

/**
 * Utility class for building Lucene filter queries common to all FHIR resource searches.
 *
 * <p>Constructs pre-filter queries that narrow search results by scope (tenant), resource type,
 * provision mode, and optional ID pre-filters. Filter conditions are combined with AND logic,
 * except for ID filters which use OR logic.
 *
 * @see QueryCombinators
 */
public final class FhirSearchHelper {

  /** Lucene field name for exact resource type matching. */
  public static final String RESOURCE_TYPE_FIELD = NAME_TYPE + SEPARATOR + SUFFIX_TOKEN_EXACT;

  /** Lucene field name for exact resource ID matching. */
  public static final String RESOURCE_ID_FIELD = NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT;

  /** Private constructor to prevent instantiation. */
  private FhirSearchHelper() {
    throw new AssertionError("FhirSearchHelper is a utility class and should not be instantiated");
  }

  /**
   * Builds a Lucene filter query combining scope, provision mode ({@code PREDEFINED}), optional
   * resource type, and optional ID pre-filter conditions.
   *
   * @param scope the scope identifier for multi-tenancy isolation
   * @param resourceType the FHIR resource type to filter by (e.g., "CodeSystem"), or {@code null}
   * @param idPreFilter optional list of resource IDs to filter by (OR logic), or {@code null}
   * @return a Lucene {@link Query} representing the combined filter conditions
   * @throws InvalidRequestException if the request contains invalid filter parameters
   */
  public static Query prepareFilterQuery(
      String scope, String resourceType, List<IdType> idPreFilter) throws InvalidRequestException {

    List<Query> filterQueryList = new ArrayList<>();

    // Filter by scope (tenant ID)
    filterQueryList.add(new TermQuery(new Term(NAME_SCOPE, scope)));

    // Filter by provision mode (only allow 'PREDEFINED')
    filterQueryList.add(
        new TermQuery(new Term(NAME_PROVISION_MODE, ProvisionMode.PREDEFINED.name())));

    // Filter by resource type (e.g., CodeSystem, ValueSet)
    if (resourceType != null) {
      // TODO: Check whether we should better generate the query using some of the custom
      //  QueryGenerartor classes
      filterQueryList.add(new TermQuery(new Term(RESOURCE_TYPE_FIELD, resourceType)));
    }

    // Filter by ID pre-filter, if provided and non-empty
    if (idPreFilter != null && !idPreFilter.isEmpty()) {

      List<Query> idFilterQueryList = new ArrayList<>();
      for (IdType idType : idPreFilter) {
        // TODO: Check whether we should better generate the query using some of the custom
        //  QueryGenerartor classes
        idFilterQueryList.add(new TermQuery(new Term(RESOURCE_ID_FIELD, idType.getIdPart())));
      }

      filterQueryList.add(QueryCombinators.prepareOrQuery(idFilterQueryList));
    }

    return QueryCombinators.prepareFilterQuery(filterQueryList);
  }

  /**
   * Builds a Lucene filter query combining scope, optional resource type, and optional ID
   * pre-filter conditions, without restricting by provision mode.
   *
   * <p>Unlike {@link #prepareFilterQuery(String, String, List)}, this variant does not add a
   * provision mode filter, making it suitable for capability-based searches that should include all
   * resources regardless of their provision mode.
   *
   * @param scope the scope identifier for multi-tenancy isolation
   * @param resourceType the FHIR resource type to filter by (e.g., "CodeSystem"), or {@code null}
   * @param idPreFilter optional list of resource IDs to filter by (OR logic), or {@code null}
   * @return a Lucene {@link Query} representing the combined filter conditions
   * @throws InvalidRequestException if the request contains invalid filter parameters
   */
  public static Query prepareFilterQueryWithoutProvision(
      String scope, String resourceType, List<IdType> idPreFilter) throws InvalidRequestException {

    List<Query> filterQueryList = new ArrayList<>();

    // Filter by scope (tenant ID)
    filterQueryList.add(new TermQuery(new Term(NAME_SCOPE, scope)));

    // Filter by resource type (e.g., CodeSystem, ValueSet)
    if (resourceType != null) {
      // TODO: Check whether we should better generate the query using some of the custom
      //  QueryGenerartor classes
      filterQueryList.add(new TermQuery(new Term(RESOURCE_TYPE_FIELD, resourceType)));
    }

    // Filter by ID pre-filter, if provided and non-empty
    if (idPreFilter != null && !idPreFilter.isEmpty()) {

      List<Query> idFilterQueryList = new ArrayList<>();
      for (IdType idType : idPreFilter) {
        // TODO: Check whether we should better generate the query using some of the custom
        //  QueryGenerartor classes
        idFilterQueryList.add(new TermQuery(new Term(RESOURCE_ID_FIELD, idType.getIdPart())));
      }

      filterQueryList.add(QueryCombinators.prepareOrQuery(idFilterQueryList));
    }

    return QueryCombinators.prepareFilterQuery(filterQueryList);
  }
}
