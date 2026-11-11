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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.reference;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ReferenceParameterQueryGenerator}. */
@DisplayName("ReferenceParameterQueryGenerator Tests")
class ReferenceParameterQueryGeneratorTest {

  private static final String PARAM_NAME = "source";

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should create generator instance")
    void shouldCreateGeneratorInstance() {
      // GIVEN WHEN
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // THEN
      assertThat(generator).isNotNull();
    }
  }

  @Nested
  @DisplayName("prepareQuery ParamData")
  class PrepareQueryParamDataTests {

    @Test
    @DisplayName("should combine and/or list semantics")
    void shouldCombineAndOrListSemantics() throws NotImplementedException {
      // GIVEN
      ReferenceAndListParam andListParam =
          ReferenceParameterQueryGeneratorTestDataHelper.createAndOrParam();
      ParamData paramData =
          ReferenceParameterQueryGeneratorTestDataHelper.createParamData(PARAM_NAME, andListParam);
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(paramData);

      // THEN
      assertThat(query).isInstanceOf(BooleanQuery.class);
      BooleanQuery outer = (BooleanQuery) query;
      assertThat(outer.clauses())
          .hasSize(2)
          .allMatch(clause -> clause.occur() == BooleanClause.Occur.MUST);
      assertThat(outer.clauses().getFirst().query()).isInstanceOf(BooleanQuery.class);
      assertThat(outer.clauses().get(1).query()).isInstanceOf(BooleanQuery.class);
    }
  }

  @Nested
  @DisplayName("prepareQuery ReferenceParam")
  class PrepareQueryReferenceParamTests {

    @Test
    @DisplayName("should return match-all query for null parameter")
    void shouldReturnMatchAllQueryForNullParameter() throws NotImplementedException {
      // GIVEN
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, null);

      // THEN
      assertThat(query).isInstanceOf(MatchAllDocsQuery.class);
    }

    @Test
    @DisplayName("should create missing query when missing modifier is set")
    void shouldCreateMissingQueryWhenMissingModifierIsSet() throws NotImplementedException {
      // GIVEN
      ReferenceParam param =
          ReferenceParameterQueryGeneratorTestDataHelper.createMissingReferenceParam(true);
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("source_missing");
      assertThat(query.toString()).contains("true");
    }

    @Test
    @DisplayName("should create identifier query when identifier chain is used")
    void shouldCreateIdentifierQueryWhenIdentifierChainIsUsed() throws NotImplementedException {
      // GIVEN
      ReferenceParam param =
          ReferenceParameterQueryGeneratorTestDataHelper.createIdentifierReferenceParam(
              "System|ABC");
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("source_lowercaseexact");
      assertThat(query.toString()).contains("identifier/system|abc");
    }

    @Test
    @DisplayName("should throw for unsupported modifier chain")
    void shouldThrowForUnsupportedModifierChain() {
      // GIVEN
      ReferenceParam param = new ReferenceParam("Patient/123");
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareQuery(PARAM_NAME, param))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Modifier not supported");
    }

    @Test
    @DisplayName("should throw when parameter has no supported chain")
    void shouldThrowWhenParameterHasNoSupportedChain() {
      // GIVEN
      ReferenceParam param = new ReferenceParam("123");
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareQuery(PARAM_NAME, param))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("without modifier chain is not supported");
    }
  }

  @Nested
  @DisplayName("prepareIdentifierQuery")
  class PrepareIdentifierQueryTests {

    @Test
    @DisplayName("should create exact token lowercase term query")
    void shouldCreateExactTokenLowercaseTermQuery() {
      // GIVEN
      ReferenceParam param = new ReferenceParam("Sys|ABC");
      ReferenceParameterQueryGenerator generator = new ReferenceParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareIdentifierQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      TermQuery termQuery = (TermQuery) query;
      assertThat(termQuery.getTerm().field())
          .isEqualTo(PARAM_NAME + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT);
      assertThat(termQuery.getTerm().text()).isEqualTo("sys|abc");
    }
  }
}
