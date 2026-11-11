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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_URI;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_URI;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link ConceptMapSearchHelper}. Tests focus on query preparation and parameter
 * handling for ConceptMap searches.
 */
@DisplayName("ConceptMapSearchHelper Tests")
class ConceptMapSearchHelperTest {

  private static final String TEST_SCOPE = "test-scope";
  private static final String CONCEPT_MAP_RESOURCE_TYPE = "ConceptMap";

  @Nested
  @DisplayName("prepareParamDataList Tests")
  class PrepareParamDataListTests {

    @Test
    @DisplayName("Should return empty list when no search parameters are provided")
    void shouldReturnEmptyListWhenNoSearchParameters() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should add source canonical parameter when provided")
    void shouldAddSourceCanonicalParameterWhenProvided() {
      // GIVEN
      ReferenceParam sourceCanonical = new ReferenceParam("http://example.org/vs/source");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(sourceCanonical);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_CANONICAL);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add source canonical parameter with version")
    void shouldAddSourceCanonicalParameterWithVersion() {
      // GIVEN
      ReferenceParam sourceCanonical = new ReferenceParam("http://example.org/vs/source|1.0.0");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(sourceCanonical);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_CANONICAL);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(refParam.getValuesAsQueryTokens()).hasSize(1);
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/vs/source|1.0.0");
              });
    }

    @Test
    @DisplayName("Should add source URI parameter when provided")
    void shouldAddSourceUriParameterWhenProvided() {
      // GIVEN
      ReferenceParam sourceUri = new ReferenceParam("http://example.org/vs/source");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceUriParam(sourceUri);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_URI);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add source URI parameter with version")
    void shouldAddSourceUriParameterWithVersion() {
      // GIVEN
      ReferenceParam sourceUri = new ReferenceParam("http://example.org/vs/source|2.5.0");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceUriParam(sourceUri);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_URI);
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/vs/source|2.5.0");
              });
    }

    @Test
    @DisplayName("Should add source system parameter when provided")
    void shouldAddSourceSystemParameterWhenProvided() {
      // GIVEN
      UriParam sourceSystem = new UriParam("http://example.org/cs/source");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceSystemParam(sourceSystem);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_SYSTEM);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                // Note: UriParam is converted to ReferenceParam in the implementation
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add source system parameter with version")
    void shouldAddSourceSystemParameterWithVersion() {
      // GIVEN
      UriParam sourceSystem = new UriParam("http://example.org/cs/source|3.0.0");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceSystemParam(sourceSystem);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE_SYSTEM);
                // UriParam converted to ReferenceParam - verify the value is preserved
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/cs/source|3.0.0");
              });
    }

    @Test
    @DisplayName("Should add target canonical parameter when provided")
    void shouldAddTargetCanonicalParameterWhenProvided() {
      // GIVEN
      ReferenceParam targetCanonical = new ReferenceParam("http://example.org/vs/target");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetCanonicalParam(targetCanonical);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_TARGET_CANONICAL);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add target canonical parameter with version")
    void shouldAddTargetCanonicalParameterWithVersion() {
      // GIVEN
      ReferenceParam targetCanonical = new ReferenceParam("http://example.org/vs/target|4.2.1");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetCanonicalParam(targetCanonical);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/vs/target|4.2.1");
              });
    }

    @Test
    @DisplayName("Should add target URI parameter when provided")
    void shouldAddTargetUriParameterWhenProvided() {
      // GIVEN
      ReferenceParam targetUri = new ReferenceParam("http://example.org/vs/target");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetUriParam(targetUri);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_TARGET_URI);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add target URI parameter with version")
    void shouldAddTargetUriParameterWithVersion() {
      // GIVEN
      ReferenceParam targetUri = new ReferenceParam("http://example.org/vs/target|5.0.0");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetUriParam(targetUri);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/vs/target|5.0.0");
              });
    }

    @Test
    @DisplayName("Should add target system parameter when provided")
    void shouldAddTargetSystemParameterWhenProvided() {
      // GIVEN
      UriParam targetSystem = new UriParam("http://example.org/cs/target");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetSystemParam(targetSystem);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_TARGET_SYSTEM);
                assertThat(paramData.getParamQueryType())
                    .isEqualTo(ParamData.CANONICAL_REFERENCE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);
              });
    }

    @Test
    @DisplayName("Should add target system parameter with version")
    void shouldAddTargetSystemParameterWithVersion() {
      // GIVEN
      UriParam targetSystem = new UriParam("http://example.org/cs/target|6.1.0");
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setTargetSystemParam(targetSystem);

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                assertThat(
                        refParam
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValuesAsQueryTokens()
                            .getFirst()
                            .getValue())
                    .isEqualTo("http://example.org/cs/target|6.1.0");
              });
    }

    @Test
    @DisplayName("Should add multiple parameters when provided")
    void shouldAddMultipleParametersWhenProvided() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));
      searchParameter.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target"));
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs/source"));
      searchParameter.setTargetSystemParam(new UriParam("http://example.org/cs/target"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(4);
      assertThat(result)
          .extracting(ParamData::getParamName)
          .containsExactlyInAnyOrder(
              NAME_SOURCE_CANONICAL, NAME_TARGET_CANONICAL, NAME_SOURCE_SYSTEM, NAME_TARGET_SYSTEM);
    }

    @Test
    @DisplayName("Should add all source and target parameters when provided")
    void shouldAddAllSourceAndTargetParametersWhenProvided() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));
      searchParameter.setSourceUriParam(new ReferenceParam("http://example.org/vs/source-uri"));
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs/source"));
      searchParameter.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target"));
      searchParameter.setTargetUriParam(new ReferenceParam("http://example.org/vs/target-uri"));
      searchParameter.setTargetSystemParam(new UriParam("http://example.org/cs/target"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(6);
      assertThat(result)
          .extracting(ParamData::getParamName)
          .containsExactlyInAnyOrder(
              NAME_SOURCE_CANONICAL,
              NAME_SOURCE_URI,
              NAME_SOURCE_SYSTEM,
              NAME_TARGET_CANONICAL,
              NAME_TARGET_URI,
              NAME_TARGET_SYSTEM);
    }

    @Test
    @DisplayName("Should add all parameters with versions when provided")
    void shouldAddAllParametersWithVersionsWhenProvided() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(
          new ReferenceParam("http://example.org/vs/source|1.0"));
      searchParameter.setSourceUriParam(new ReferenceParam("http://example.org/vs/source-uri|2.0"));
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs/source|3.0"));
      searchParameter.setTargetCanonicalParam(
          new ReferenceParam("http://example.org/vs/target|4.0"));
      searchParameter.setTargetUriParam(new ReferenceParam("http://example.org/vs/target-uri|5.0"));
      searchParameter.setTargetSystemParam(new UriParam("http://example.org/cs/target|6.0"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .hasSize(6)
          .allMatch(
              paramData -> {
                ReferenceAndListParam refParam = (ReferenceAndListParam) paramData.getParam();
                String value =
                    refParam
                        .getValuesAsQueryTokens()
                        .getFirst()
                        .getValuesAsQueryTokens()
                        .getFirst()
                        .getValue();
                return value.contains("|");
              });
    }

    @Test
    @DisplayName("Should include terminology resource parameters when provided")
    void shouldIncludeTerminologyResourceParametersWhenProvided() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setIdParam(new TokenParam("test-id"));
      searchParameter.setUrlParam(new UriParam("http://example.org/cm"));
      searchParameter.setNameParam(new StringParam("TestConceptMap"));
      searchParameter.setStatusParam(new TokenParam("active"));
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSizeGreaterThanOrEqualTo(5);
      assertThat(result).extracting(ParamData::getParamName).contains(NAME_SOURCE_CANONICAL);
    }

    @ParameterizedTest
    @MethodSource("provideParametersWithVersions")
    @DisplayName("Should correctly handle parameters with various version formats")
    void shouldCorrectlyHandleParametersWithVariousVersionFormats(
        String paramValue, String expectedValue, String description) {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam(paramValue));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(1);
      ReferenceAndListParam refParam = (ReferenceAndListParam) result.getFirst().getParam();
      String actualValue =
          refParam
              .getValuesAsQueryTokens()
              .getFirst()
              .getValuesAsQueryTokens()
              .getFirst()
              .getValue();
      assertThat(actualValue).as("Parameter value for: " + description).isEqualTo(expectedValue);
    }

    static Stream<Arguments> provideParametersWithVersions() {
      return Stream.of(
          Arguments.of("http://example.org/vs", "http://example.org/vs", "URL without version"),
          Arguments.of(
              "http://example.org/vs|1.0.0",
              "http://example.org/vs|1.0.0",
              "URL with semantic version"),
          Arguments.of(
              "http://example.org/vs|2023-01-15",
              "http://example.org/vs|2023-01-15",
              "URL with date version"),
          Arguments.of(
              "http://example.org/vs|v2.5.3",
              "http://example.org/vs|v2.5.3",
              "URL with v-prefixed version"),
          Arguments.of(
              "http://example.org/vs|SNAPSHOT",
              "http://example.org/vs|SNAPSHOT",
              "URL with snapshot version"));
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
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
    }

    @Test
    @DisplayName("Should create query with filter for ConceptMap resource type")
    void shouldCreateQueryWithFilterForConceptMapResourceType()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull();
      assertThat(query.toString()).contains(CONCEPT_MAP_RESOURCE_TYPE);
    }

    @Test
    @DisplayName("Should create query with scope filter")
    void shouldCreateQueryWithScopeFilter()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull();
      assertThat(query.toString()).contains(TEST_SCOPE);
    }

    @Test
    @DisplayName("Should create query with ID pre-filter when provided")
    void shouldCreateQueryWithIdPreFilterWhenProvided()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      List<IdType> idPreFilter = List.of(new IdType("ConceptMap/1"), new IdType("ConceptMap/2"));

      // WHEN
      Query query =
          ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, idPreFilter);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
    }

    @Test
    @DisplayName("Should create query with source canonical parameter")
    void shouldCreateQueryWithSourceCanonicalParameter()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertThat(booleanQuery.clauses()).isNotEmpty();
    }

    @Test
    @DisplayName("Should create query with versioned source canonical parameter")
    void shouldCreateQueryWithVersionedSourceCanonicalParameter()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(
          new ReferenceParam("http://example.org/vs/source|1.0.0"));

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertThat(booleanQuery.clauses()).isNotEmpty();
    }

    @Test
    @DisplayName("Should create query combining multiple search parameters")
    void shouldCreateQueryCombiningMultipleSearchParameters()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));
      searchParameter.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target"));
      searchParameter.setStatusParam(new TokenParam("active"));

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertThat(booleanQuery.clauses()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should create query with versioned source and target parameters")
    void shouldCreateQueryWithVersionedSourceAndTargetParameters()
        throws InvalidRequestException, NotImplementedException {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(
          new ReferenceParam("http://example.org/vs/source|1.0"));
      searchParameter.setTargetCanonicalParam(
          new ReferenceParam("http://example.org/vs/target|2.0"));
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs/source|3.0"));
      searchParameter.setTargetSystemParam(new UriParam("http://example.org/cs/target|4.0"));

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query).isNotNull().isInstanceOf(BooleanQuery.class);
      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertThat(booleanQuery.clauses()).hasSizeGreaterThanOrEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("provideSearchParameterCombinations")
    @DisplayName("Should create valid queries for various parameter combinations")
    void shouldCreateValidQueriesForVariousParameterCombinations(
        CmSearchParameter searchParameter, String description)
        throws InvalidRequestException, NotImplementedException {

      // WHEN
      Query query = ConceptMapSearchHelper.prepareMetadataQuery(searchParameter, TEST_SCOPE, null);

      // THEN
      assertThat(query)
          .as("Query should be created for: " + description)
          .isNotNull()
          .isInstanceOf(BooleanQuery.class);
    }

    static Stream<Arguments> provideSearchParameterCombinations() {
      return Stream.of(
          Arguments.of(
              createSearchParameterWithSourceCanonical("http://example.org/vs"),
              "source canonical only"),
          Arguments.of(
              createSearchParameterWithSourceCanonical("http://example.org/vs|1.0.0"),
              "source canonical with version"),
          Arguments.of(
              createSearchParameterWithSourceUri("http://example.org/vs"), "source URI only"),
          Arguments.of(
              createSearchParameterWithSourceUri("http://example.org/vs|2.0.0"),
              "source URI with version"),
          Arguments.of(
              createSearchParameterWithSourceSystem("http://example.org/cs"), "source system only"),
          Arguments.of(
              createSearchParameterWithSourceSystem("http://example.org/cs|3.0.0"),
              "source system with version"),
          Arguments.of(
              createSearchParameterWithTargetCanonical("http://example.org/vs"),
              "target canonical only"),
          Arguments.of(
              createSearchParameterWithTargetCanonical("http://example.org/vs|4.0.0"),
              "target canonical with version"),
          Arguments.of(
              createSearchParameterWithTargetUri("http://example.org/vs"), "target URI only"),
          Arguments.of(
              createSearchParameterWithTargetUri("http://example.org/vs|5.0.0"),
              "target URI with version"),
          Arguments.of(
              createSearchParameterWithTargetSystem("http://example.org/cs"), "target system only"),
          Arguments.of(
              createSearchParameterWithTargetSystem("http://example.org/cs|6.0.0"),
              "target system with version"),
          Arguments.of(
              createSearchParameterWithSourceAndTargetCanonical(
                  "http://example.org/vs/source|1.0", "http://example.org/vs/target|2.0"),
              "source and target canonical with versions"),
          Arguments.of(
              createSearchParameterWithSourceAndTargetSystem(
                  "http://example.org/cs/source|3.0", "http://example.org/cs/target|4.0"),
              "source and target system with versions"),
          Arguments.of(createSearchParameterWithAllSourceParameters(), "all source parameters"),
          Arguments.of(createSearchParameterWithAllTargetParameters(), "all target parameters"),
          Arguments.of(
              createSearchParameterWithAllSourceParametersVersioned(),
              "all source parameters with versions"),
          Arguments.of(
              createSearchParameterWithAllTargetParametersVersioned(),
              "all target parameters with versions"));
    }

    // Helper methods for creating search parameters
    private static CmSearchParameter createSearchParameterWithSourceCanonical(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceCanonicalParam(new ReferenceParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithSourceUri(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceUriParam(new ReferenceParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithSourceSystem(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceSystemParam(new UriParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithTargetCanonical(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setTargetCanonicalParam(new ReferenceParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithTargetUri(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setTargetUriParam(new ReferenceParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithTargetSystem(String value) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setTargetSystemParam(new UriParam(value));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithSourceAndTargetCanonical(
        String source, String target) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceCanonicalParam(new ReferenceParam(source));
      param.setTargetCanonicalParam(new ReferenceParam(target));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithSourceAndTargetSystem(
        String source, String target) {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceSystemParam(new UriParam(source));
      param.setTargetSystemParam(new UriParam(target));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithAllSourceParameters() {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));
      param.setSourceUriParam(new ReferenceParam("http://example.org/vs/source-uri"));
      param.setSourceSystemParam(new UriParam("http://example.org/cs/source"));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithAllTargetParameters() {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target"));
      param.setTargetUriParam(new ReferenceParam("http://example.org/vs/target-uri"));
      param.setTargetSystemParam(new UriParam("http://example.org/cs/target"));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithAllSourceParametersVersioned() {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source|1.0"));
      param.setSourceUriParam(new ReferenceParam("http://example.org/vs/source-uri|2.0"));
      param.setSourceSystemParam(new UriParam("http://example.org/cs/source|3.0"));
      return param;
    }

    private static CmSearchParameter createSearchParameterWithAllTargetParametersVersioned() {
      CmSearchParameter param = CmSearchParameter.builder().build();
      param.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target|4.0"));
      param.setTargetUriParam(new ReferenceParam("http://example.org/vs/target-uri|5.0"));
      param.setTargetSystemParam(new UriParam("http://example.org/cs/target|6.0"));
      return param;
    }
  }

  @Nested
  @DisplayName("Helper Method Tests")
  class HelperMethodTests {

    @Test
    @DisplayName("Should wrap ReferenceParam in ReferenceAndListParam")
    void shouldWrapReferenceParamInReferenceAndListParam() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(1);
      ParamData paramData = result.getFirst();
      assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);

      ReferenceAndListParam andListParam = (ReferenceAndListParam) paramData.getParam();
      assertThat(andListParam.getValuesAsQueryTokens()).hasSize(1);
    }

    @Test
    @DisplayName("Should convert UriParam to ReferenceParam and wrap in ReferenceAndListParam")
    void shouldConvertUriParamToReferenceParamAndWrap() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(1);
      ParamData paramData = result.getFirst();
      // Note: UriParam is converted to ReferenceParam in the implementation
      assertThat(paramData.getParam()).isInstanceOf(ReferenceAndListParam.class);

      ReferenceAndListParam andListParam = (ReferenceAndListParam) paramData.getParam();
      assertThat(andListParam.getValuesAsQueryTokens()).hasSize(1);
    }

    @Test
    @DisplayName("Should set correct query type for all parameters")
    void shouldSetCorrectQueryTypeForAllParameters() {
      // GIVEN
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceCanonicalParam(new ReferenceParam("http://example.org/vs/source"));
      searchParameter.setSourceSystemParam(new UriParam("http://example.org/cs/source"));
      searchParameter.setTargetCanonicalParam(new ReferenceParam("http://example.org/vs/target"));
      searchParameter.setTargetSystemParam(new UriParam("http://example.org/cs/target"));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result)
          .isNotEmpty()
          .allMatch(
              paramData ->
                  ParamData.CANONICAL_REFERENCE_PARAM.equals(paramData.getParamQueryType()));
    }

    @Test
    @DisplayName("Should preserve version in UriParam to ReferenceParam conversion")
    void shouldPreserveVersionInUriParamToReferenceParamConversion() {
      // GIVEN
      String uriWithVersion = "http://example.org/cs/source|1.2.3";
      CmSearchParameter searchParameter = CmSearchParameter.builder().build();
      searchParameter.setSourceSystemParam(new UriParam(uriWithVersion));

      // WHEN
      List<ParamData> result = ConceptMapSearchHelper.prepareParamDataList(searchParameter);

      // THEN
      assertThat(result).hasSize(1);
      ReferenceAndListParam refParam = (ReferenceAndListParam) result.getFirst().getParam();
      String actualValue =
          refParam
              .getValuesAsQueryTokens()
              .getFirst()
              .getValuesAsQueryTokens()
              .getFirst()
              .getValue();
      assertThat(actualValue).isEqualTo(uriWithVersion);
    }
  }
}
