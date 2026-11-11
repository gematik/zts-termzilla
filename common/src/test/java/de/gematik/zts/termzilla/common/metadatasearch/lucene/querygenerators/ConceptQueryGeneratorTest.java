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

import static de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper.RESOURCE_TYPE_FIELD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE_LOWERCASE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DEFINITION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DISPLAY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_STANDARD;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_TOKEN_EXACT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link ConceptQueryGenerator}. */
@DisplayName("ConceptQueryGenerator Tests")
class ConceptQueryGeneratorTest {

  @Nested
  @DisplayName("prepareCodeParamQuery")
  class PrepareCodeParamQueryTests {

    @Test
    @DisplayName("Should include exact and lowercase code query when no modifier is set")
    void shouldIncludeExactAndLowercaseCodeQueryWhenNoModifierIsSet()
        throws NotImplementedException {
      // GIVEN
      TokenParam codeParam = new TokenParam("AbC-123");

      // WHEN
      Query query = ConceptQueryGenerator.prepareCodeParamQuery(codeParam);

      // THEN
      BooleanQuery rootQuery = assertAndCastBooleanQuery(query);
      assertThat(rootQuery.clauses())
          .hasSize(1)
          .allSatisfy(clause -> assertThat(clause.occur()).isEqualTo(BooleanClause.Occur.MUST));

      BooleanQuery orQuery = assertAndCastBooleanQuery(rootQuery.clauses().getFirst().query());
      assertThat(orQuery.clauses())
          .hasSize(2)
          .allSatisfy(clause -> assertThat(clause.occur()).isEqualTo(BooleanClause.Occur.SHOULD));
      assertThat(orQuery.toString())
          .contains(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT)
          .contains(NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_TOKEN_EXACT)
          .contains("AbC-123")
          .contains("abc-123");
    }

    @Test
    @DisplayName("Should build display text query when :text modifier is used")
    void shouldBuildDisplayTextQueryWhenTextModifierIsUsed() throws NotImplementedException {
      // GIVEN
      TokenParam codeParam = new TokenParam("Heart failure");
      codeParam.setModifier(TokenParamModifier.TEXT);

      // WHEN
      Query query = ConceptQueryGenerator.prepareCodeParamQuery(codeParam);

      // THEN
      BooleanQuery rootQuery = assertAndCastBooleanQuery(query);
      BooleanQuery orQuery = assertAndCastBooleanQuery(rootQuery.clauses().getFirst().query());
      Query textQuery = orQuery.clauses().getFirst().query();

      assertThat(textQuery).isInstanceOf(PrefixQuery.class);
      assertThat(textQuery.toString()).contains(NAME_DISPLAY);
    }

    @Test
    @DisplayName("Should throw NotImplementedException for unsupported modifier")
    void shouldThrowForUnsupportedModifier() {
      // GIVEN
      TokenParam codeParam = new TokenParam("123");
      codeParam.setModifier(TokenParamModifier.NOT);

      // WHEN / THEN
      assertThatThrownBy(() -> ConceptQueryGenerator.prepareCodeParamQuery(codeParam))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("NOT modifier");
    }

    @ParameterizedTest
    @MethodSource("unsupportedModifiers")
    @DisplayName(
        "Should throw NotImplementedException for unsupported hierarchy and type modifiers")
    void shouldThrowForUnsupportedHierarchyAndTypeModifiers(
        TokenParamModifier modifier, String expectedMessagePart) {
      // GIVEN
      TokenParam codeParam = new TokenParam("123");
      codeParam.setModifier(modifier);

      // WHEN / THEN
      assertThatThrownBy(() -> ConceptQueryGenerator.prepareCodeParamQuery(codeParam))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining(expectedMessagePart + " modifier");
    }

    private static Stream<Arguments> unsupportedModifiers() {
      return Stream.of(
          arguments(TokenParamModifier.ABOVE, "ABOVE"),
          arguments(TokenParamModifier.BELOW, "BELOW"),
          arguments(TokenParamModifier.IN, "IN"),
          arguments(TokenParamModifier.NOT_IN, "NOT_IN"),
          arguments(TokenParamModifier.OF_TYPE, "OF_TYPE"));
    }
  }

  @Nested
  @DisplayName("prepareContentParamQuery")
  class PrepareContentParamQueryTests {

    @Test
    @DisplayName("Should combine five OR clauses for a non-empty content value")
    void shouldCombineFiveOrClausesForNonEmptyContentValue() throws NotImplementedException {
      // GIVEN
      StringParam contentParam = new StringParam("hypertension");

      // WHEN
      Query query = ConceptQueryGenerator.prepareContentParamQuery(contentParam);

      // THEN
      BooleanQuery rootQuery = assertAndCastBooleanQuery(query);
      assertThat(rootQuery.clauses())
          .hasSize(1)
          .allSatisfy(clause -> assertThat(clause.occur()).isEqualTo(BooleanClause.Occur.MUST));

      BooleanQuery orQuery = assertAndCastBooleanQuery(rootQuery.clauses().getFirst().query());
      assertThat(orQuery.clauses())
          .hasSize(5)
          .allSatisfy(clause -> assertThat(clause.occur()).isEqualTo(BooleanClause.Occur.SHOULD));
    }

    @Test
    @DisplayName("Should reject blank input after contains sanitization")
    void shouldRejectBlankInputAfterContainsSanitization() {
      // GIVEN
      StringParam contentParam = new StringParam("   ");

      // WHEN / THEN
      assertThatThrownBy(() -> ConceptQueryGenerator.prepareContentParamQuery(contentParam))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("Empty query after sanitization");
    }

    @Test
    @DisplayName("Should reject modifiers for _content")
    void shouldRejectModifiersForContent() {
      // GIVEN
      StringParam contentParam = new StringParam("x");
      contentParam.setExact(true);

      // WHEN / THEN
      assertThatThrownBy(() -> ConceptQueryGenerator.prepareContentParamQuery(contentParam))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("No modifiers are supported");
    }
  }

  @Nested
  @DisplayName("prepareFindMatchesPreFilterQuery")
  class PrepareFindMatchesPreFilterQueryTests {

    @Test
    @DisplayName("Should create filter query for resource type, scope, and code system id")
    void shouldCreateFilterQueryForResourceTypeScopeAndCodeSystemId() {
      // GIVEN
      String scope = "default-scope";
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/test-system", true);

      // WHEN
      Query query = ConceptQueryGenerator.prepareFindMatchesPreFilterQuery(scope, codeSystem);

      // THEN
      BooleanQuery filterQuery = assertAndCastBooleanQuery(query);
      assertThat(filterQuery.clauses())
          .hasSize(3)
          .allSatisfy(clause -> assertThat(clause.occur()).isEqualTo(BooleanClause.Occur.FILTER));

      assertThat(filterQuery.toString())
          .contains(RESOURCE_TYPE_FIELD + ":ConceptDefinitionComponent")
          .contains(NAME_SCOPE + ":" + scope)
          .contains(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT + ":test-system");
    }
  }

  @Nested
  @DisplayName("prepareFindMatchesPropertyQueries")
  class PrepareFindMatchesPropertyQueriesTests {

    @Test
    @DisplayName("Should create an exact-only code query for case-sensitive code systems")
    void shouldCreateExactOnlyCodeQueryForCaseSensitiveCodeSystems()
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodePropertyFilter("Code-1", 0);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), true);

      // THEN
      Pair<String, Query> queryEntry = result.getFirst();
      BooleanQuery codeQuery = assertAndCastBooleanQuery(queryEntry.getRight());
      assertThat(result).hasSize(1);
      assertThat(queryEntry.getLeft()).isEqualTo("code");
      assertThat(codeQuery.clauses()).hasSize(1);
      assertThat(codeQuery.toString())
          .contains(NAME_CODE + SEPARATOR + SUFFIX_TOKEN_EXACT + ":Code-1");
    }

    @Test
    @DisplayName(
        "Should include partial query for case-insensitive code systems when exact is false")
    void shouldIncludePartialQueryForCaseInsensitiveCodeSystemsWhenExactIsFalse()
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", false);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodePropertyFilter("Code-2", 0);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), false);

      // THEN
      BooleanQuery codeQuery = assertAndCastBooleanQuery(result.getFirst().getRight());
      assertThat(codeQuery.clauses()).hasSize(2);
      assertThat(codeQuery.toString())
          .contains(NAME_CODE_LOWERCASE + SEPARATOR + SUFFIX_TOKEN_EXACT + ":code-2")
          .contains(NAME_CODE_LOWERCASE);
    }

    @Test
    @DisplayName("Should support Coding typed property values")
    void shouldSupportCodingTypedPropertyValues() throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodingPropertyFilter(
              "property-coding", "http://example.org/system", "2026", "A1", 2);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), true);

      // THEN
      Query propertyQuery = result.getFirst().getRight();
      assertThat(propertyQuery).isInstanceOf(BoostQuery.class);

      BoostQuery boostQuery = (BoostQuery) propertyQuery;
      assertThat(boostQuery.getQuery()).isInstanceOf(ConstantScoreQuery.class);
      assertThat(boostQuery.toString()).contains("http://example.org/system|2026#A1");
    }

    @Test
    @DisplayName("Should create definition query with exact, standard and contains branches")
    void shouldCreateDefinitionQueryWithExactStandardAndContainsBranches()
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createStringPropertyFilter(
              "definition", "chronic pain", 1);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), false);

      // THEN
      Pair<String, Query> queryEntry = result.getFirst();
      BooleanQuery definitionQuery = assertAndCastBooleanQuery(queryEntry.getRight());
      assertThat(queryEntry.getLeft()).isEqualTo("definition");
      assertThat(definitionQuery.clauses()).hasSize(2);
      assertThat(definitionQuery.toString())
          .contains(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_EXACT)
          .contains(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_STANDARD)
          .contains(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_CONTAINS);
    }

    @Test
    @DisplayName("Should create exact-only definition query when exact flag is true")
    void shouldCreateExactOnlyDefinitionQueryWhenExactFlagIsTrue() throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createStringPropertyFilter(
              "definition", "diabetes", 1);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), true);

      // THEN
      BooleanQuery definitionQuery = assertAndCastBooleanQuery(result.getFirst().getRight());
      assertThat(definitionQuery.clauses()).hasSize(1);
      assertThat(definitionQuery.toString())
          .contains(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_EXACT)
          .doesNotContain(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_STANDARD)
          .doesNotContain(NAME_DEFINITION + SEPARATOR + SUFFIX_STRING_CONTAINS);
    }

    @Test
    @DisplayName("Should route non-special string property filters to exact property string query")
    void shouldRouteNonSpecialStringPropertyFiltersToExactPropertyStringQuery()
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createStringPropertyFilter(
              "property-string", "priority", 3);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), false);

      // THEN
      Pair<String, Query> queryEntry = result.getFirst();
      assertThat(queryEntry.getLeft()).isEqualTo("property-string");
      assertThat(queryEntry.getRight()).isInstanceOf(BoostQuery.class);
      assertThat(queryEntry.getRight().toString())
          .contains(
              NAME_PREFIX_PROPERTY
                  + SEPARATOR
                  + "property-string"
                  + SEPARATOR
                  + SUFFIX_STRING_EXACT);
    }

    @Test
    @DisplayName("Should route non-special code property filters to exact property code query")
    void shouldRouteNonSpecialCodePropertyFiltersToExactPropertyCodeQuery()
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodeValuePropertyFilter(
              "property-code", "A-4711", 4);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), false);

      // THEN
      Pair<String, Query> queryEntry = result.getFirst();
      assertThat(queryEntry.getLeft()).isEqualTo("property-code");
      assertThat(queryEntry.getRight()).isInstanceOf(BoostQuery.class);
      assertThat(queryEntry.getRight().toString())
          .contains(
              NAME_PREFIX_PROPERTY + SEPARATOR + "property-code" + SEPARATOR + SUFFIX_STRING_EXACT);
    }

    @ParameterizedTest
    @MethodSource("supportedCodingConstellations")
    @DisplayName("Should support all non-empty Coding constellations for exact property query")
    void shouldSupportAllNonEmptyCodingConstellationsForExactPropertyQuery(
        String system, String version, String code, String expectedToken)
        throws NotImplementedException {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodingPropertyFilter(
              "property-coding", system, version, code, 2);

      // WHEN
      List<Pair<String, Query>> result =
          ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
              codeSystem, List.of(propertyFilter), true);

      // THEN
      Query propertyQuery = result.getFirst().getRight();
      assertThat(propertyQuery).isInstanceOf(BoostQuery.class);

      BoostQuery boostQuery = (BoostQuery) propertyQuery;
      assertThat(boostQuery.getQuery()).isInstanceOf(ConstantScoreQuery.class);
      assertThat(boostQuery.toString()).contains(expectedToken);
    }

    @Test
    @DisplayName(
        "Should reject Coding property search when system, version and code are all missing")
    void shouldRejectCodingPropertySearchWhenSystemVersionAndCodeAreAllMissing() {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter propertyFilter =
          ConceptQueryGeneratorTestDataHelper.createCodingPropertyFilter(
              "property-coding", null, null, null, 2);

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
                      codeSystem, List.of(propertyFilter), true))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Coding constellation");
    }

    private static Stream<Arguments> supportedCodingConstellations() {
      return Stream.of(
          arguments("http://example.org/system", "2026", "A1", "http://example.org/system|2026#A1"),
          arguments("http://example.org/system", null, "A1", "http://example.org/system|#A1"),
          arguments("http://example.org/system", "2026", null, "http://example.org/system|2026#"),
          arguments("http://example.org/system", null, null, "http://example.org/system|#"),
          arguments(null, "2026", "A1", "|2026#A1"),
          arguments(null, "2026", null, "|2026#"),
          arguments(null, null, "A1", "|#A1"));
    }

    @Test
    @DisplayName("Should reject unsupported property value types")
    void shouldRejectUnsupportedPropertyValueTypes() {
      // GIVEN
      CodeSystem codeSystem =
          ConceptQueryGeneratorTestDataHelper.createCodeSystem("CodeSystem/cs", true);
      PropertyFilter unsupportedFilter = new PropertyFilter("flag", new BooleanType(true), 1);

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  ConceptQueryGenerator.prepareFindMatchesPropertyQueries(
                      codeSystem, List.of(unsupportedFilter), false))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("small subset of properties is supported");
    }
  }

  private static BooleanQuery assertAndCastBooleanQuery(Query query) {
    assertThat(query).isInstanceOf(BooleanQuery.class);
    return (BooleanQuery) query;
  }
}
