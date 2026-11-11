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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import java.util.List;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CombinedParameterQueryGenerator}. */
@DisplayName("CombinedParameterQueryGenerator Tests")
class CombinedParameterQueryGeneratorTest {

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should create generator instance")
    void shouldCreateGeneratorInstance() {
      // GIVEN WHEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();

      // THEN
      assertThat(generator).isNotNull();
    }
  }

  @Nested
  @DisplayName("prepareAndListQuery")
  class PrepareAndListQueryTests {

    @Test
    @DisplayName("should combine string and uri queries using AND semantics")
    void shouldCombineStringAndUriQueriesUsingAndSemantics() throws NotImplementedException {
      // GIVEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();
      ParamData stringParam =
          CombinedParameterQueryGeneratorTestDataHelper.createStringParamData("name", "Alpha");
      ParamData uriParam =
          CombinedParameterQueryGeneratorTestDataHelper.createUriParamData(
              "url", "http://example.org/CodeSystem/test");
      List<ParamData> paramDataList = List.of(stringParam, uriParam);

      // WHEN
      Query query = generator.prepareAndListQuery(paramDataList);

      // THEN
      assertThat(query).isInstanceOf(BooleanQuery.class);
      BooleanQuery booleanQuery = (BooleanQuery) query;
      assertThat(booleanQuery.clauses())
          .hasSize(2)
          .allMatch(clause -> clause.occur() == BooleanClause.Occur.MUST);
      assertThat(query.toString()).contains("name_standard").contains("url_uriexact");
    }

    @Test
    @DisplayName("should route id token query to id token generator")
    void shouldRouteIdTokenQueryToIdTokenGenerator() throws NotImplementedException {
      // GIVEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();
      ParamData idTokenParam =
          CombinedParameterQueryGeneratorTestDataHelper.createIdTokenParamData("_id", "cs-123");

      // WHEN
      Query query = generator.prepareAndListQuery(List.of(idTokenParam));

      // THEN
      assertThat(query.toString()).contains("_id_tokenexact").contains("cs-123");
    }

    @Test
    @DisplayName("should ignore unknown param query types")
    void shouldIgnoreUnknownParamQueryTypes() throws NotImplementedException {
      // GIVEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();
      ParamData mockedParamData = mock(ParamData.class);
      when(mockedParamData.getParamQueryType()).thenReturn("unknown_type");

      // WHEN
      Query query = generator.prepareAndListQuery(List.of(mockedParamData));

      // THEN
      assertThat(query).isInstanceOf(BooleanQuery.class);
      assertThat(((BooleanQuery) query).clauses()).isEmpty();
    }

    @Test
    @DisplayName("should return empty boolean query for empty input")
    void shouldReturnEmptyBooleanQueryForEmptyInput() throws NotImplementedException {
      // GIVEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareAndListQuery(List.of());

      // THEN
      assertThat(query).isInstanceOf(BooleanQuery.class);
      assertThat(((BooleanQuery) query).clauses()).isEmpty();
    }

    @Test
    @DisplayName("should propagate exception from delegated generator")
    void shouldPropagateExceptionFromDelegatedGenerator() {
      // GIVEN
      CombinedParameterQueryGenerator generator = new CombinedParameterQueryGenerator();
      ParamData unsupportedReference =
          CombinedParameterQueryGeneratorTestDataHelper.createReferenceParamData(
              "source", "Patient/123");

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareAndListQuery(List.of(unsupportedReference)))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Modifier not supported");
    }
  }
}
