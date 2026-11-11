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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand.VsExpandLuceneHelper.LUCENE_THRESHOLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.BytesRef;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link VsExpandLuceneHelper}.
 *
 * <p>Tests cover threshold logic, query building for different property types, and filter
 * execution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VsExpandLuceneHelper Tests")
class VsExpandLuceneHelperTest {

  private static final String TEST_SCOPE = "test-scope";
  private static final String TEST_CS_ID = "test-cs-1";
  private static final String TEST_SYSTEM = "http://example.org/fhir/CodeSystem/test";

  @Mock private IndexSearcher indexSearcher;

  @Nested
  @DisplayName("shouldUseLucene")
  class ShouldUseLuceneTests {

    @Test
    @DisplayName("Should return false when concept count is below threshold")
    void shouldReturnFalseWhenConceptCountBelowThreshold() {
      // GIVEN - CodeSystem with fewer than 500 concepts
      CodeSystem codeSystem = createCodeSystemWithConcepts(100);

      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      boolean result = helper.shouldUseLucene();

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when concept count meets threshold")
    void shouldReturnTrueWhenConceptCountMeetsThreshold() {
      // GIVEN - CodeSystem with exactly LUCENE_THRESHOLD concepts
      CodeSystem codeSystem = createCodeSystemWithConcepts(LUCENE_THRESHOLD);

      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      boolean result = helper.shouldUseLucene();

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true when concept count exceeds threshold")
    void shouldReturnTrueWhenConceptCountExceedsThreshold() {
      // GIVEN - CodeSystem with more than LUCENE_THRESHOLD concepts
      CodeSystem codeSystem = createCodeSystemWithConcepts(LUCENE_THRESHOLD + 100);

      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      boolean result = helper.shouldUseLucene();

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when searcher is null")
    void shouldReturnFalseWhenSearcherIsNull() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(LUCENE_THRESHOLD + 100);

      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(null, TEST_SCOPE, codeSystem);

      // WHEN
      boolean result = helper.shouldUseLucene();

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should count nested concepts towards threshold")
    void shouldCountNestedConceptsTowardsThreshold() {
      // GIVEN - CodeSystem with LUCENE_THRESHOLD top-level concepts, each with 1 nested concept =
      // LUCENE_THRESHOLD *2 total
      CodeSystem codeSystem = createCodeSystemWithNestedConcepts(LUCENE_THRESHOLD, 1);

      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      boolean result = helper.shouldUseLucene();

      // THEN
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("buildPropertyEqualsQuery")
  class BuildPropertyEqualsQueryTests {

    @Test
    @DisplayName("Should build TermQuery for code property")
    void shouldBuildTermQueryForCodeProperty() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithStringProperty("status");
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("code", "12345");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("code_tokenexact");
      assertThat(query.toString()).contains("12345");
    }

    @Test
    @DisplayName("Should build TermQuery for string property")
    void shouldBuildTermQueryForStringProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("myProp", CodeSystem.PropertyType.STRING);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("myProp", "someValue");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("property_myProp_exact");
      assertThat(query.toString()).contains("someValue");
    }

    @Test
    @DisplayName("Should build TermQuery for code-type property")
    void shouldBuildTermQueryForCodeTypeProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("status", CodeSystem.PropertyType.CODE);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("status", "active");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("property_status_tokenexact");
      assertThat(query.toString()).contains("active");
    }

    @Test
    @DisplayName("Should build TermQuery for boolean property")
    void shouldBuildTermQueryForBooleanProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("inactive", CodeSystem.PropertyType.BOOLEAN);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("inactive", "true");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("property_inactive_tokenexact");
      assertThat(query.toString()).contains("true");
    }

    @Test
    @DisplayName("Should build LongPoint query for integer property")
    void shouldBuildLongPointQueryForIntegerProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("weight", CodeSystem.PropertyType.INTEGER);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("weight", "42");

      // THEN
      assertThat(query).isNotNull();
      assertThat(query.toString()).contains("property_weight_long");
    }

    @Test
    @DisplayName("Should build TermQuery for decimal property")
    void shouldBuildTermQueryForDecimalProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("price", CodeSystem.PropertyType.DECIMAL);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("price", "12.34");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("property_price_exact");
      assertThat(query.toString()).contains("12.34");
    }

    @Test
    @DisplayName("Should build overlap query for dateTime property")
    void shouldBuildOverlapQueryForDateTimeProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("effective", CodeSystem.PropertyType.DATETIME);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("effective", "2024-06-15");

      // THEN
      assertThat(query.toString()).contains("property_effective_date_period_start");
      assertThat(query.toString()).contains("property_effective_date_period_end");
    }

    @Test
    @DisplayName("Should build TermQuery for coding property")
    void shouldBuildTermQueryForCodingProperty() {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("map", CodeSystem.PropertyType.CODING);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyEqualsQuery("map", "http://sys|1#A");

      // THEN
      assertThat(query).isInstanceOf(TermQuery.class);
      assertThat(query.toString()).contains("property_map_tokenexact");
      assertThat(query.toString()).contains("http://sys|1#A");
    }

    @Test
    @DisplayName("Should fail fast when property type is null")
    void shouldFailFastWhenPropertyTypeIsNull() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithPropertyOfType("broken", null);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN THEN
      assertThatThrownBy(() -> helper.buildPropertyEqualsQuery("broken", "value"))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw for unknown property")
    void shouldThrowForUnknownProperty() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN / THEN
      assertThatThrownBy(() -> helper.buildPropertyEqualsQuery("unknownProp", "value"))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unsupported property");
    }
  }

  @Nested
  @DisplayName("buildPropertyRegexQuery")
  class BuildPropertyRegexQueryTests {

    @Test
    @DisplayName("Should build RegexpQuery for code property")
    void shouldBuildRegexpQueryForCodeProperty() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyRegexQuery("code", "A[0-9]+");

      // THEN
      assertThat(query).isInstanceOf(RegexpQuery.class);
      assertThat(query.toString()).contains("code_tokenexact");
    }

    @Test
    @DisplayName("Should build RegexpQuery for named property")
    void shouldBuildRegexpQueryForNamedProperty() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = helper.buildPropertyRegexQuery("myProp", "test.*");

      // THEN
      assertThat(query).isInstanceOf(RegexpQuery.class);
      assertThat(query.toString()).contains("property_myProp_exact");
    }
  }

  @Nested
  @DisplayName("executeCombinedFilters")
  class ExecuteCombinedFiltersTests {

    @Test
    @DisplayName("Should combine equals and regex filters and return matching codes")
    void shouldCombineEqualsAndRegexFiltersAndReturnMatchingCodes() throws IOException {
      // GIVEN
      CodeSystem codeSystem =
          createCodeSystemWithPropertyOfType("status", CodeSystem.PropertyType.STRING);

      Document matchingDocument =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "A-100", "active", null, null, null);
      Document nonMatchingDocument =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "B-100", "active", null, null, null);

      try (TestLuceneIndex testIndex =
          createTestLuceneIndex(List.of(matchingDocument, nonMatchingDocument))) {
        VsExpandLuceneHelper helper =
            new VsExpandLuceneHelper(testIndex.searcher(), TEST_SCOPE, codeSystem);

        List<ValueSet.ConceptSetFilterComponent> equalsFilters =
            List.of(createFilter("status", "active"));
        List<ValueSet.ConceptSetFilterComponent> regexFilters =
            List.of(createFilter("code", "A-.*"));

        // WHEN
        Set<String> result = helper.executeCombinedFilters(equalsFilters, regexFilters);

        // THEN
        assertThat(result).containsExactly("A-100");
      }
    }
  }

  @Nested
  @DisplayName("executeAndReturnCodes")
  class ExecuteAndReturnCodesTests {

    @Test
    @DisplayName("Should return all matching codes from sorted doc values")
    void shouldReturnAllMatchingCodesFromSortedDocValues() throws IOException {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);

      Document first =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "C-1", "active", null, null, null);
      Document second =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "C-2", "active", null, null, null);

      try (TestLuceneIndex testIndex = createTestLuceneIndex(List.of(first, second))) {
        VsExpandLuceneHelper helper =
            new VsExpandLuceneHelper(testIndex.searcher(), TEST_SCOPE, codeSystem);
        Query query = new TermQuery(new Term("property_status_exact", "active"));

        // WHEN
        Set<String> result = helper.executeAndReturnCodes(query);

        // THEN
        assertThat(result).containsExactlyInAnyOrder("C-1", "C-2");
      }
    }
  }

  @Nested
  @DisplayName("private query helpers")
  class PrivateQueryHelpersTests {

    @Test
    @DisplayName("buildPreFilterQuery should include scope type and CodeSystem id")
    void buildPreFilterQueryShouldIncludeScopeTypeAndCodeSystemId() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = invokeBuildPreFilterQuery(helper);

      // THEN
      assertThat(query.toString()).contains(TEST_SCOPE);
      assertThat(query.toString()).contains(FhirSearchHelper.RESOURCE_TYPE_FIELD);
      assertThat(query.toString()).contains("ConceptDefinitionComponent");
      assertThat(query.toString()).contains(TEST_CS_ID);
    }

    @Test
    @DisplayName("buildFinalQuery should return prefilter when must clauses are empty")
    void buildFinalQueryShouldReturnPreFilterWhenMustClausesAreEmpty() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);
      Query preFilter = invokeBuildPreFilterQuery(helper);

      // WHEN
      Query finalQuery = invokeBuildFinalQuery(helper, List.of());

      // THEN
      assertThat(finalQuery.toString()).hasToString(preFilter.toString());
    }

    @Test
    @DisplayName("buildFinalQuery should combine prefilter with must clauses")
    void buildFinalQueryShouldCombinePreFilterWithMustClauses() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);
      Query mustQuery = new TermQuery(new Term("property_status_exact", "active"));

      // WHEN
      Query finalQuery = invokeBuildFinalQuery(helper, List.of(mustQuery));

      // THEN
      assertThat(finalQuery.toString()).contains(TEST_SCOPE);
      assertThat(finalQuery.toString()).contains("property_status_exact");
      assertThat(finalQuery.toString()).contains("active");
    }

    @Test
    @DisplayName("buildDateTimeOverlapQuery should include start and end range clauses")
    void buildDateTimeOverlapQueryShouldIncludeStartAndEndRangeClauses() {
      // GIVEN
      CodeSystem codeSystem = createCodeSystemWithConcepts(1);
      VsExpandLuceneHelper helper = new VsExpandLuceneHelper(indexSearcher, TEST_SCOPE, codeSystem);

      // WHEN
      Query query = invokeBuildDateTimeOverlapQuery(helper, "property_effective_", "2024-06");

      // THEN
      assertThat(query.toString()).contains("property_effective_date_period_start");
      assertThat(query.toString()).contains("property_effective_date_period_end");
    }
  }

  @Nested
  @DisplayName("findLeafContext")
  class FindLeafContextTests {

    @Test
    @DisplayName("Should return matching leaf context for valid global doc id")
    void shouldReturnMatchingLeafContextForValidGlobalDocId() throws IOException {
      // GIVEN
      Document conceptDocument =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "L-1", "active", null, null, null);

      try (TestLuceneIndex testIndex = createTestLuceneIndex(List.of(conceptDocument))) {
        List<LeafReaderContext> leaves = testIndex.searcher().getIndexReader().leaves();

        // WHEN
        LeafReaderContext leaf = invokeFindLeafContext(leaves, 0);

        // THEN
        assertThat(leaf.docBase).isZero();
      }
    }

    @Test
    @DisplayName("Should throw when no leaf exists for global doc id")
    void shouldThrowWhenNoLeafExistsForGlobalDocId() throws IOException {
      // GIVEN
      Document conceptDocument =
          createConceptDocument(TEST_SCOPE, TEST_CS_ID, "L-1", "active", null, null, null);

      try (TestLuceneIndex testIndex = createTestLuceneIndex(List.of(conceptDocument))) {
        List<LeafReaderContext> leaves = testIndex.searcher().getIndexReader().leaves();

        // WHEN THEN
        assertThatThrownBy(() -> invokeFindLeafContext(leaves, 1_000))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No leaf context found");
      }
    }
  }

  // ================================================================================================
  // Helper methods
  // ================================================================================================

  private static ValueSet.ConceptSetFilterComponent createFilter(String property, String value) {
    ValueSet.ConceptSetFilterComponent filter = new ValueSet.ConceptSetFilterComponent();
    filter.setProperty(property);
    filter.setValue(value);
    return filter;
  }

  private static Document createConceptDocument(
      String scope,
      String codeSystemId,
      String code,
      String status,
      String codingToken,
      Long dateStart,
      Long dateEnd) {

    Document document = new Document();
    document.add(new StringField("scope", scope, Field.Store.NO));
    document.add(
        new StringField(
            FhirSearchHelper.RESOURCE_TYPE_FIELD, "ConceptDefinitionComponent", Field.Store.NO));
    document.add(
        new StringField(ResourceFieldNames.NAME_ID + "_tokenexact", codeSystemId, Field.Store.NO));

    document.add(new StringField("code_tokenexact", code, Field.Store.NO));
    document.add(new SortedDocValuesField("code_sort", new BytesRef(code)));

    if (status != null) {
      document.add(new StringField("property_status_exact", status, Field.Store.NO));
    }
    if (codingToken != null) {
      document.add(new StringField("property_map_tokenexact", codingToken, Field.Store.NO));
    }
    if (dateStart != null) {
      document.add(new LongPoint("property_effective_date_period_start", dateStart));
    }
    if (dateEnd != null) {
      document.add(new LongPoint("property_effective_date_period_end", dateEnd));
    }
    return document;
  }

  private static TestLuceneIndex createTestLuceneIndex(List<Document> documents)
      throws IOException {
    ByteBuffersDirectory directory = new ByteBuffersDirectory();
    IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());

    try (IndexWriter writer = new IndexWriter(directory, config)) {
      for (Document document : documents) {
        writer.addDocument(document);
      }
      writer.commit();
    }

    DirectoryReader reader = DirectoryReader.open(directory);
    return new TestLuceneIndex(directory, reader, new IndexSearcher(reader));
  }

  private static Query invokeBuildPreFilterQuery(VsExpandLuceneHelper helper) {
    return invokePrivateInstanceMethod(helper, "buildPreFilterQuery", new Class<?>[0]);
  }

  private static Query invokeBuildFinalQuery(VsExpandLuceneHelper helper, List<Query> mustClauses) {
    return invokePrivateInstanceMethod(
        helper, "buildFinalQuery", new Class<?>[] {List.class}, mustClauses);
  }

  private static Query invokeBuildDateTimeOverlapQuery(
      VsExpandLuceneHelper helper, String fieldPrefix, String value) {
    return invokePrivateInstanceMethod(
        helper,
        "buildDateTimeOverlapQuery",
        new Class<?>[] {String.class, String.class},
        fieldPrefix,
        value);
  }

  private static LeafReaderContext invokeFindLeafContext(
      List<LeafReaderContext> leaves, int globalDocId) {
    return invokePrivateStaticMethod(
        VsExpandLuceneHelper.class,
        "findLeafContext",
        new Class<?>[] {List.class, int.class},
        leaves,
        globalDocId);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invokePrivateInstanceMethod(
      Object target, String methodName, Class<?>[] parameterTypes, Object... args) {

    try {
      Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(target, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invokePrivateStaticMethod(
      Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object... args) {

    try {
      Method method = targetClass.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(null, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private record TestLuceneIndex(
      ByteBuffersDirectory directory, DirectoryReader reader, IndexSearcher searcher)
      implements AutoCloseable {

    @Override
    public void close() throws IOException {
      reader.close();
      directory.close();
    }
  }

  private static CodeSystem createCodeSystemWithConcepts(int count) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(TEST_CS_ID);
    codeSystem.setUrl(TEST_SYSTEM);
    for (int i = 0; i < count; i++) {
      codeSystem.addConcept().setCode("concept-" + i).setDisplay("Concept " + i);
    }
    return codeSystem;
  }

  private static CodeSystem createCodeSystemWithNestedConcepts(
      int topLevelCount, int nestedPerTopLevel) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(TEST_CS_ID);
    codeSystem.setUrl(TEST_SYSTEM);
    for (int i = 0; i < topLevelCount; i++) {
      CodeSystem.ConceptDefinitionComponent parent =
          codeSystem.addConcept().setCode("parent-" + i).setDisplay("Parent " + i);
      for (int j = 0; j < nestedPerTopLevel; j++) {
        parent.addConcept().setCode("child-" + i + "-" + j).setDisplay("Child " + i + "-" + j);
      }
    }
    return codeSystem;
  }

  private static CodeSystem createCodeSystemWithStringProperty(String propertyCode) {
    return createCodeSystemWithPropertyOfType(propertyCode, CodeSystem.PropertyType.STRING);
  }

  private static CodeSystem createCodeSystemWithPropertyOfType(
      String propertyCode, CodeSystem.PropertyType type) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(TEST_CS_ID);
    codeSystem.setUrl(TEST_SYSTEM);
    codeSystem.addConcept().setCode("concept-1").setDisplay("Concept 1");

    CodeSystem.PropertyComponent property = codeSystem.addProperty();
    property.setCode(propertyCode);
    property.setType(type);

    return codeSystem;
  }
}
