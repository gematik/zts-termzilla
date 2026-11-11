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

import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.CONTENT_MODE_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.ID_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.NAME_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.STATUS_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.SUPPLEMENTS_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.SYSTEM_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.SYSTEM_WITH_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemSearchHelperTestDataHelper.URL_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_CONTENT_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SUPPLEMENTS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_NAME;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_STATUS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import java.util.List;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CodeSystemSearchHelper}. Tests focus on query preparation and parameter
 * handling for CodeSystem searches.
 */
@DisplayName("CodeSystemSearchHelper Tests")
class CodeSystemSearchHelperTest {

  private static final String TEST_SCOPE = "test-scope";
  private static final String CODE_SYSTEM_RESOURCE_TYPE = "CodeSystem";

  @Nested
  @DisplayName("prepareParamDataList Tests")
  class PrepareParamDataListTests {

    @Test
    @DisplayName("Should return empty list when no search parameters are provided")
    void shouldReturnEmptyListWhenNoSearchParameters() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should add system parameter when provided")
    void shouldAddSystemParameterWhenProvided() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      UriParam systemParam = CodeSystemSearchHelperTestDataHelper.uriParam(SYSTEM_VALUE);
      searchParameter.setSystemParam(systemParam);

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SYSTEM);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.URI_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(UriAndListParam.class);
                UriAndListParam andListParam = (UriAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(UriParam::getValue)
                    .containsExactly(SYSTEM_VALUE);
              });
    }

    @Test
    @DisplayName("Should split system version and create system plus version params")
    void shouldSplitSystemVersionAndCreateSystemPlusVersionParams() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setSystemParam(
          CodeSystemSearchHelperTestDataHelper.uriParam(SYSTEM_WITH_VERSION));

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(ParamData::getParamName)
          .containsExactlyInAnyOrder(NAME_SYSTEM, NAME_VERSION);
      assertThat(result)
          .filteredOn(paramData -> NAME_SYSTEM.equals(paramData.getParamName()))
          .first()
          .satisfies(
              paramData -> {
                UriAndListParam andListParam = (UriAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(UriParam::getValue)
                    .containsExactly(SYSTEM_VALUE);
              });
      assertThat(result)
          .filteredOn(paramData -> NAME_VERSION.equals(paramData.getParamName()))
          .first()
          .satisfies(
              paramData -> {
                TokenAndListParam andListParam = (TokenAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(TokenParam::getValue)
                    .containsExactly("2.3.4");
              });
    }

    @Test
    @DisplayName("Should add content-mode parameter when provided")
    void shouldAddContentModeParameterWhenProvided() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setContentModeParam(
          CodeSystemSearchHelperTestDataHelper.tokenParam(CONTENT_MODE_VALUE));

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_CONTENT_MODE);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.CODE_TOKEN_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(TokenAndListParam.class);
                TokenAndListParam andListParam = (TokenAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(TokenParam::getValue)
                    .containsExactly(CONTENT_MODE_VALUE);
              });
    }

    @Test
    @DisplayName("Should add supplements parameter when provided")
    void shouldAddSupplementsParameterWhenProvided() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setSupplementsParam(
          CodeSystemSearchHelperTestDataHelper.referenceParam(SUPPLEMENTS_VALUE));

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SUPPLEMENTS);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
                ReferenceAndListParam andListParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(ReferenceParam::getValue)
                    .containsExactly(SUPPLEMENTS_VALUE);
              });
    }

    @Test
    @DisplayName("Should add multiple CodeSystem-specific parameters when provided")
    void shouldAddMultipleCodeSystemSpecificParametersWhenProvided() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setSystemParam(CodeSystemSearchHelperTestDataHelper.uriParam(SYSTEM_VALUE));
      searchParameter.setContentModeParam(
          CodeSystemSearchHelperTestDataHelper.tokenParam(CONTENT_MODE_VALUE));
      searchParameter.setSupplementsParam(
          CodeSystemSearchHelperTestDataHelper.referenceParam(SUPPLEMENTS_VALUE));

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(3)
          .extracting(ParamData::getParamName)
          .containsExactlyInAnyOrder(NAME_SYSTEM, NAME_CONTENT_MODE, NAME_SUPPLEMENTS);
    }

    @Test
    @DisplayName("Should include terminology resource and CodeSystem parameters when mixed")
    void shouldIncludeTerminologyResourceAndCodeSystemParametersWhenMixed() {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setIdParam(CodeSystemSearchHelperTestDataHelper.tokenParam(ID_VALUE));
      searchParameter.setUrlParam(CodeSystemSearchHelperTestDataHelper.uriParam(URL_VALUE));
      searchParameter.setNameParam(CodeSystemSearchHelperTestDataHelper.stringParam(NAME_VALUE));
      searchParameter.setStatusParam(CodeSystemSearchHelperTestDataHelper.tokenParam(STATUS_VALUE));
      searchParameter.setSystemParam(CodeSystemSearchHelperTestDataHelper.uriParam(SYSTEM_VALUE));

      // WHEN
      List<ParamData> result = CodeSystemSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(5);
      assertThat(result)
          .extracting(ParamData::getParamName)
          .containsExactlyInAnyOrder(NAME_ID, NAME_URL, NAME_NAME, NAME_STATUS, NAME_SYSTEM);
    }
  }

  @Nested
  @DisplayName("prepareMetadataQuery Tests")
  class PrepareMetadataQueryTests {

    @Test
    @DisplayName("Should create valid query with no search parameters")
    void shouldCreateValidQueryWithNoSearchParameters()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null, false);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
    }

    @Test
    @DisplayName("Should create query with filter for CodeSystem resource type")
    void shouldCreateQueryWithFilterForCodeSystemResourceType()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null, false);

      // THEN
      assertThat(query).isNotNull();
      assertThat(query.toString()).contains(CODE_SYSTEM_RESOURCE_TYPE);
    }

    @Test
    @DisplayName("Should create query with scope filter")
    void shouldCreateQueryWithScopeFilter()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null, false);

      // THEN
      assertThat(query).isNotNull();
      assertThat(query.toString()).contains(TEST_SCOPE);
    }

    @Test
    @DisplayName("Should create query with ID pre-filter when provided")
    void shouldCreateQueryWithIdPreFilterWhenProvided()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      List<IdType> idPreFilter = List.of(new IdType("CodeSystem/1"), new IdType("CodeSystem/2"));

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(
              searchParameter, TEST_SCOPE, idPreFilter, false);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
    }

    @Test
    @DisplayName("Should create query for capability search")
    void shouldCreateQueryForCapabilitySearch()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null, true);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      assertThat(query.toString()).contains(CODE_SYSTEM_RESOURCE_TYPE, TEST_SCOPE);
    }

    @Test
    @DisplayName("Should create query combining multiple search parameters")
    void shouldCreateQueryCombiningMultipleSearchParameters()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CsSearchParameter searchParameter =
          CodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      searchParameter.setSystemParam(CodeSystemSearchHelperTestDataHelper.uriParam(SYSTEM_VALUE));
      searchParameter.setContentModeParam(
          CodeSystemSearchHelperTestDataHelper.tokenParam(CONTENT_MODE_VALUE));
      searchParameter.setStatusParam(CodeSystemSearchHelperTestDataHelper.tokenParam(STATUS_VALUE));

      // WHEN
      Query query =
          CodeSystemSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null, false);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      assertThat(query.toString()).contains(NAME_SYSTEM, NAME_CONTENT_MODE, NAME_STATUS);
    }
  }
}
