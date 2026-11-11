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
package de.gematik.zts.termzilla.terminologies.snomed.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.terminologies.snomed.lucene.loader.DescriptionLoader;
import de.gematik.zts.termzilla.terminologies.snomed.lucene.queries.LuceneQueryFactory;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

/**
 * Characterisation tests for {@link SnomedIndex}.
 *
 * <p>The production constructor loads RF2 files from the file system, making unit testing
 * impossible without bypassing it. These tests use Objenesis to create an instance without calling
 * the constructor, then inject an in-memory Lucene index via reflection. This approach confirms the
 * motivation for the planned Lucene layer refactoring.
 *
 * <p>The test index contains a small set of SNOMED CT description documents with known field
 * values, covering multiple modules, effective times, and active/inactive states.
 */
@DisplayName("SnomedIndex Tests")
class SnomedIndexTest {

  private static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_GERMAN =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  private static SnomedIndex snomedIndex;

  // ──────────────────────────────────────────────────────────────────────────
  // Test data setup
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an in-memory Lucene index with test documents and injects it into a {@link SnomedIndex}
   * instance created via Objenesis.
   *
   * <p>Test documents:
   *
   * <ul>
   *   <li>Doc 1: concept=73211009 (Diabetes), module=core, effectiveTime=20240101, active=1,
   *       term="Diabetes mellitus"
   *   <li>Doc 2: concept=73211009 (Diabetes), module=core, effectiveTime=20240601, active=1,
   *       term="Diabetes mellitus type 2"
   *   <li>Doc 3: concept=38341003 (Hypertension), module=core, effectiveTime=20240101, active=1,
   *       term="Hypertensive disorder"
   *   <li>Doc 4: concept=38341003 (Hypertension), module=core, effectiveTime=20240601, active=0,
   *       term="Hypertensive disorder"
   *   <li>Doc 5: concept=73211009 (Diabetes), module=german, effectiveTime=20240515, active=1,
   *       term="Zuckerkrankheit"
   *   <li>Doc 6: concept=64572001 (Disorder), module=core, effectiveTime=20240101, active=1,
   *       term="Disorder"
   *   <li>Doc 7: concept=387713003 (Surgery), module=core, effectiveTime=20240101, active=1,
   *       term="Surgical procedure"
   * </ul>
   */
  @BeforeAll
  static void setUp() throws Exception {
    Directory directory = new ByteBuffersDirectory();
    StandardAnalyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

    try (IndexWriter writer = new IndexWriter(directory, config)) {
      // Use DescriptionLoader to write documents with the same field layout as production
      DescriptionLoader loader = new DescriptionLoader();

      // RF2 description line format:
      // id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId
      String[] testLines = {
        "desc-1\t20240101\t1\t"
            + MODULE_CORE
            + "\t73211009\ten\t900000000000003001\tDiabetes mellitus\t900000000000448009",
        "desc-2\t20240601\t1\t"
            + MODULE_CORE
            + "\t73211009\ten\t900000000000003001\tDiabetes mellitus type 2\t900000000000448009",
        "desc-3\t20240101\t1\t"
            + MODULE_CORE
            + "\t38341003\ten\t900000000000003001\tHypertensive disorder\t900000000000448009",
        "desc-4\t20240601\t0\t"
            + MODULE_CORE
            + "\t38341003\ten\t900000000000003001\tHypertensive disorder\t900000000000448009",
        "desc-5\t20240515\t1\t"
            + MODULE_GERMAN
            + "\t73211009\tde\t900000000000013009\tZuckerkrankheit\t900000000000448009",
        "desc-6\t20240101\t1\t"
            + MODULE_CORE
            + "\t64572001\ten\t900000000000003001\tDisorder\t900000000000448009",
        "desc-7\t20240101\t1\t"
            + MODULE_CORE
            + "\t387713003\ten\t900000000000003001\tSurgical procedure\t900000000000448009",
      };

      for (String line : testLines) {
        // Call process() via reflection since it's private
        java.lang.reflect.Method processMethod =
            DescriptionLoader.class.getDeclaredMethod("process", String.class, IndexWriter.class);
        processMethod.setAccessible(true);
        processMethod.invoke(loader, line, writer);
      }
    }

    // Create SnomedIndex instance via Objenesis (bypass constructor)
    snomedIndex = new ObjenesisStd().newInstance(SnomedIndex.class);

    // Inject in-memory searcher and query parser via reflection
    DirectoryReader reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);
    setPrivateField(snomedIndex, "searcher", searcher);

    QueryParser termQueryParser = new QueryParser(DescriptionLoader.INDEX_FIELDNAME_TERM, analyzer);
    termQueryParser.setDefaultOperator(QueryParser.Operator.AND);
    setPrivateField(snomedIndex, "termQueryParser", termQueryParser);
  }

  private static void setPrivateField(Object target, String fieldName, Object value)
      throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // queryIndex()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("queryIndex() Tests")
  class QueryIndexTests {

    @Test
    @DisplayName("Should return all documents for MatchAllDocsQuery")
    void shouldReturnAllDocumentsForMatchAll() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareSearchAllQuery();

      // WHEN
      List<Document> result = snomedIndex.queryIndex(query, 100);

      // THEN
      assertThat(result).as("MatchAllDocsQuery should return all 7 test documents").hasSize(7);
    }

    @Test
    @DisplayName("Should respect maxNumberOfResults limit")
    void shouldRespectMaxNumberOfResults() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareSearchAllQuery();

      // WHEN
      List<Document> result = snomedIndex.queryIndex(query, 3);

      // THEN
      assertThat(result).as("Should return at most 3 documents when limit is 3").hasSize(3);
    }

    @Test
    @DisplayName("Should return empty list when no documents match")
    void shouldReturnEmptyListWhenNoMatch() {
      // GIVEN – query for a module that does not exist in test data
      Query query = LuceneQueryFactory.prepareModuleIdQuery("999999999");

      // WHEN
      List<Document> result = snomedIndex.queryIndex(query, 100);

      // THEN
      assertThat(result).as("Non-matching query should return empty list").isEmpty();
    }

    @Test
    @DisplayName("Should filter by moduleId")
    void shouldFilterByModuleId() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareModuleIdQuery(MODULE_GERMAN);

      // WHEN
      List<Document> result = snomedIndex.queryIndex(query, 100);

      // THEN
      assertThat(result).as("Should return only the German module document").hasSize(1);
      assertThat(result.get(0).get(DescriptionLoader.INDEX_FIELDNAME_CONCEPT_ID))
          .as("German module document should be for concept 73211009 (Diabetes)")
          .isEqualTo("73211009");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // queryIndexSortEffectiveTimeAscending() / Descending()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("queryIndexSortEffectiveTime Tests")
  class QueryIndexSortTests {

    @Test
    @DisplayName("Should sort documents by effectiveTime ascending")
    void shouldSortByEffectiveTimeAscending() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareSearchAllQuery();

      // WHEN
      List<Document> result = snomedIndex.queryIndexSortEffectiveTimeAscending(query, 100);

      // THEN
      assertThat(result).as("All documents should be returned").hasSize(7);
      // First documents should have effectiveTime 20240101, last should be 20240601
      assertThat(result.get(0).get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME))
          .as("First document should have earliest effectiveTime")
          .isEqualTo("20240101");
      assertThat(
              result.get(result.size() - 1).get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME))
          .as("Last document should have latest effectiveTime")
          .isEqualTo("20240601");
    }

    @Test
    @DisplayName("Should sort documents by effectiveTime descending")
    void shouldSortByEffectiveTimeDescending() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareSearchAllQuery();

      // WHEN
      List<Document> result = snomedIndex.queryIndexSortEffectiveTimeDecending(query, 100);

      // THEN
      assertThat(result.get(0).get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME))
          .as("First document should have latest effectiveTime")
          .isEqualTo("20240601");
      assertThat(
              result.get(result.size() - 1).get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME))
          .as("Last document should have earliest effectiveTime")
          .isEqualTo("20240101");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // queryActiveTerms()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("queryActiveTerms() Tests")
  class QueryActiveTermsTests {

    @Test
    @DisplayName("Should return concept IDs matching the filter term")
    void shouldReturnConceptIdsMatchingFilter() {
      // GIVEN
      SnomedReleaseVersion coreVersion = SnomedTestData.coreVersion("20240601");

      // WHEN
      Set<String> result = snomedIndex.queryActiveTerms("Diabetes", coreVersion);

      // THEN
      assertThat(result)
          .as("Should find concept 73211009 (Diabetes) matching 'Diabetes'")
          .contains("73211009");
    }

    @Test
    @DisplayName("Should exclude inactive descriptions from results")
    void shouldExcludeInactiveDescriptions() {
      // GIVEN – Hypertension has active=1 at 20240101 and active=0 at 20240601
      // When querying at version 20240601, the most recent state (active=0) should be used,
      // resulting in an empty result set for "Hypertensive"
      SnomedReleaseVersion coreVersion = SnomedTestData.coreVersion("20240601");

      // WHEN
      Set<String> result = snomedIndex.queryActiveTerms("Hypertensive", coreVersion);

      // THEN
      assertThat(result)
          .as("Hypertension should be excluded because its most recent description is inactive")
          .isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when filter matches no terms")
    void shouldReturnEmptySetForNoMatch() {
      // GIVEN
      SnomedReleaseVersion coreVersion = SnomedTestData.coreVersion("20240601");

      // WHEN
      Set<String> result = snomedIndex.queryActiveTerms("Nonexistent", coreVersion);

      // THEN
      assertThat(result).as("Should return empty set when no terms match the filter").isEmpty();
    }

    @Test
    @DisplayName("Should throw InvalidRequestException for leading wildcard filter")
    void shouldThrowForLeadingWildcardFilter() {
      // GIVEN – LuceneQuerySanitizer.isSafeInput rejects leading wildcards
      SnomedReleaseVersion coreVersion = SnomedTestData.coreVersion("20240101");

      // WHEN / THEN
      assertThatThrownBy(() -> snomedIndex.queryActiveTerms("*term", coreVersion))
          .as("Should reject filter starting with wildcard")
          .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should respect effectiveTime boundary of the release version")
    void shouldRespectEffectiveTimeBoundary() {
      // GIVEN – query at version 20240101 (before doc-2 "Diabetes mellitus type 2" at 20240601)
      SnomedReleaseVersion coreVersion = SnomedTestData.coreVersion("20240101");

      // WHEN
      Set<String> result = snomedIndex.queryActiveTerms("Diabetes", coreVersion);

      // THEN – should still find Diabetes via the 20240101 description
      assertThat(result)
          .as("Should find Diabetes at version 20240101 via the earlier description")
          .contains("73211009");
    }

    @Test
    @DisplayName("Should find terms across module dependencies in German edition")
    void shouldFindTermsAcrossModuleDependencies() {
      // GIVEN – German version with dependency on core
      SnomedReleaseVersion germanVersion = SnomedTestData.germanVersion("20240515", "20240101");

      // WHEN
      Set<String> result = snomedIndex.queryActiveTerms("Disorder", germanVersion);

      // THEN – should find "Disorder" (concept 64572001) from core module at 20240101
      assertThat(result)
          .as("German edition should find core module concepts via dependencies")
          .contains("64572001");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // LuceneQueryFactory (tested indirectly but also directly)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("LuceneQueryFactory Tests")
  class LuceneQueryFactoryTests {

    @Test
    @DisplayName("Should build effectiveTime range query that includes boundary")
    void shouldBuildEffectiveTimeRangeIncludingBoundary() {
      // GIVEN
      Query query = LuceneQueryFactory.prepareEffectiveTimeRangeQuery("20240101");

      // WHEN – execute against the index, should include docs at exactly 20240101
      List<Document> result = snomedIndex.queryIndex(query, 100);

      // THEN – 5 documents have effectiveTime 20240101
      // 4 core docs at 20240101: desc-1, desc-3, desc-6, desc-7
      assertThat(result)
          .as("Range query up to 20240101 should include 4 documents at that date")
          .hasSize(4);
    }

    @Test
    @DisplayName("Should combine AND queries correctly")
    void shouldCombineAndQueriesCorrectly() {
      // GIVEN – moduleId = core AND effectiveTime <= 20240101
      Query moduleQuery = LuceneQueryFactory.prepareModuleIdQuery(MODULE_CORE);
      Query timeQuery = LuceneQueryFactory.prepareEffectiveTimeRangeQuery("20240101");
      Query andQuery = LuceneQueryFactory.prepareAndQuery(List.of(moduleQuery, timeQuery));

      // WHEN
      List<Document> result = snomedIndex.queryIndex(andQuery, 100);

      // THEN – 4 core module docs at 20240101 (desc-1, desc-3, desc-6, desc-7)
      assertThat(result)
          .as("AND query should return only core module documents at 20240101")
          .hasSize(4);
    }

    @Test
    @DisplayName("Should build release version range query including dependencies")
    void shouldBuildReleaseVersionRangeQueryIncludingDependencies() {
      // GIVEN – German version at 20240515 with dependency on core at 20240101
      SnomedReleaseVersion germanVersion = SnomedTestData.germanVersion("20240515", "20240101");
      Query query = LuceneQueryFactory.prepareReleaseVersionRangeQuery(germanVersion);

      // WHEN
      List<Document> result = snomedIndex.queryIndex(query, 100);

      // THEN – should include: German module docs <= 20240515 (desc-5) + core module docs <=
      // 20240101 (desc-1, desc-3, desc-6, desc-7)
      assertThat(result)
          .as("Release version range should include both German and core dependency documents")
          .hasSize(5);
    }
  }
}
