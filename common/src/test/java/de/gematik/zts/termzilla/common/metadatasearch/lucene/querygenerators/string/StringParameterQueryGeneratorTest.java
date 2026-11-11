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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.string;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_STRING_STANDARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StringParameterQueryGenerator}. */
@DisplayName("StringParameterQueryGenerator Tests")
class StringParameterQueryGeneratorTest {

  private static final String PARAM_NAME = "name";

  @Nested
  @DisplayName("Constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should create generator instance")
    void shouldCreateGeneratorInstance() {
      // GIVEN WHEN
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

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
      StringAndListParam andListParam =
          StringParameterQueryGeneratorTestDataHelper.createAndOrParam();
      ParamData paramData =
          StringParameterQueryGeneratorTestDataHelper.createParamData(PARAM_NAME, andListParam);
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

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
  @DisplayName("prepareQuery StringParam")
  class PrepareQueryStringParamTests {

    @Test
    @DisplayName("should return match-all query for null parameter")
    void shouldReturnMatchAllQueryForNullParameter() throws NotImplementedException {
      // GIVEN
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, null);

      // THEN
      assertThat(query).isInstanceOf(MatchAllDocsQuery.class);
    }

    @Test
    @DisplayName("should return match-all query for empty parameter")
    void shouldReturnMatchAllQueryForEmptyParameter() throws NotImplementedException {
      // GIVEN
      StringParam param = new StringParam();
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(MatchAllDocsQuery.class);
    }

    @Test
    @DisplayName("should route to exact query when exact modifier is set")
    void shouldRouteToExactQueryWhenModifierIsSet() throws NotImplementedException {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createExactParam("Alpha");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("name_exact");
      assertThat(query.toString()).contains("Alpha");
    }

    @Test
    @DisplayName("should route to contains query when contains modifier is set")
    void shouldRouteToContainsQueryWhenModifierIsSet() throws NotImplementedException {
      // GIVEN
      StringParam param =
          StringParameterQueryGeneratorTestDataHelper.createContainsParam("Alpha Beta");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query.toString()).contains("name_contains");
      assertThat(query.toString()).contains("alpha");
      assertThat(query.toString()).contains("beta");
    }

    @Test
    @DisplayName("should route to missing query when missing modifier is set")
    void shouldRouteToMissingQueryWhenModifierIsSet() throws NotImplementedException {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createMissingParam(true);
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("name_missing");
      assertThat(query.toString()).contains("true");
    }

    @Test
    @DisplayName("should route to standard query when no modifier is set")
    void shouldRouteToStandardQueryWhenNoModifierIsSet() throws NotImplementedException {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createStringParam("Alpha");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(PrefixQuery.class);
      assertThat(query.toString()).contains("name_standard");
    }
  }

  @Nested
  @DisplayName("prepareExactModifierQuery")
  class PrepareExactModifierQueryTests {

    @Test
    @DisplayName("should create exact term query")
    void shouldCreateExactTermQuery() {
      // GIVEN
      StringParam param =
          StringParameterQueryGeneratorTestDataHelper.createStringParam("Exact Value");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareExactModifierQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      TermQuery termQuery = (TermQuery) query;
      assertThat(termQuery.getTerm().field())
          .isEqualTo(PARAM_NAME + SEPARATOR + SUFFIX_STRING_EXACT);
      assertThat(termQuery.getTerm().text()).isEqualTo("Exact Value");
    }
  }

  @Nested
  @DisplayName("prepareContainsModifierQuery")
  class PrepareContainsModifierQueryTests {

    @Test
    @DisplayName("should reject unsafe contains input")
    void shouldRejectUnsafeContainsInput() {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createStringParam("*unsafe");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareContainsModifierQuery(PARAM_NAME, param))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("unsafe patterns");
    }

    @Test
    @DisplayName("should reject empty contains query after sanitization")
    void shouldRejectEmptyContainsQueryAfterSanitization() {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createStringParam("   ");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareContainsModifierQuery(PARAM_NAME, param))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Empty query after sanitization");
    }
  }

  @Nested
  @DisplayName("prepareTextModifierQuery")
  class PrepareTextModifierQueryTests {

    @Test
    @DisplayName("should throw not implemented exception")
    void shouldThrowNotImplementedException() {
      // GIVEN
      StringParam param = StringParameterQueryGeneratorTestDataHelper.createStringParam("text");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN THEN
      assertThatThrownBy(() -> generator.prepareTextModifierQuery(PARAM_NAME, param))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining(":text modifier not implemented yet");
    }
  }

  @Nested
  @DisplayName("prepareStandardQuery")
  class PrepareStandardQueryTests {

    @Test
    @DisplayName("should create prefix query with normalized value")
    void shouldCreatePrefixQueryWithNormalizedValue() {
      // GIVEN
      StringParam param =
          StringParameterQueryGeneratorTestDataHelper.createStringParam("Universitatsklinik Char");
      StringParameterQueryGenerator generator = new StringParameterQueryGenerator();

      // WHEN
      Query query = generator.prepareStandardQuery(PARAM_NAME, param);

      // THEN
      assertThat(query).isInstanceOf(PrefixQuery.class);
      PrefixQuery prefixQuery = (PrefixQuery) query;
      assertThat(prefixQuery.getPrefix().field())
          .isEqualTo(PARAM_NAME + SEPARATOR + SUFFIX_STRING_STANDARD);
      assertThat(prefixQuery.getPrefix().text()).isEqualTo("universitatsklinik char");
    }
  }
}
