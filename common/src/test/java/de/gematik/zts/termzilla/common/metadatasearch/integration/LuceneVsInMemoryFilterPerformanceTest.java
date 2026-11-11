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
package de.gematik.zts.termzilla.common.metadatasearch.integration;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_CODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_PREFIX_PROPERTY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_ID;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.common.metadatasearch.helper.FhirSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ConceptComponentFieldGeneratorHelper;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Performance comparison between Lucene-based and in-memory filter execution for ValueSet expand.
 *
 * <p>This benchmark measures the relative performance of two filter strategies for the ValueSet
 * {@code $expand} operation across varying CodeSystem sizes. The Lucene path uses a pre-built index
 * with {@link SortedDocValues}-based result extraction, while the in-memory path flattens the
 * concept hierarchy and scans properties sequentially.
 *
 * <p><b>Production-realistic conditions:</b>
 *
 * <ul>
 *   <li>{@link MMapDirectory} backed by the filesystem with OS page cache (as in production)
 *   <li>Single batch commit with {@code forceMerge(1)} for optimal segment layout
 *   <li>Reusable {@link IndexSearcher} (created once, shared across queries)
 *   <li>Result extraction via {@link SortedDocValues} on the {@code code_sort} field (column-
 *       oriented, no per-hit document deserialization)
 *   <li>Background noise from {@value #BACKGROUND_DOCUMENT_COUNT} documents of other CodeSystems
 *   <li>Pre-filter (scope + resource type + CodeSystem ID) as used in production
 *   <li>Two-filter AND scenario combining category and status property clauses
 *   <li>3-level nested concept hierarchy to test realistic in-memory traversal cost
 *   <li>{@value #FILLER_PROPERTIES_PER_CONCEPT} filler properties per concept to simulate realistic
 *       per-concept scan overhead
 * </ul>
 *
 * <p><b>Configurable parameters:</b>
 *
 * <ul>
 *   <li>{@link #CATEGORY_MATCH_EVERY} and {@link #STATUS_MATCH_EVERY} control the filter hit rate —
 *       the effective AND hit rate is {@code 1 / lcm(N, M)}
 *   <li>{@link #FILLER_PROPERTIES_PER_CONCEPT} controls the per-concept scan cost for the in-memory
 *       path
 *   <li>{@link #BACKGROUND_DOCUMENT_COUNT} controls the amount of index noise
 *   <li>{@link #WARMUP_ITERATIONS} and {@link #MEASURED_ITERATIONS} control benchmark stability
 * </ul>
 */
@Disabled(
    "Only Used to Estimate the Threshold when Lucene is Faster Than directly working on Objects")
@DisplayName("Lucene vs In-Memory Filter Performance Comparison")
class LuceneVsInMemoryFilterPerformanceTest {

  private static final String TEST_SCOPE = "perf-test";
  private static final String TEST_CS_ID = "perf-cs";
  private static final String OTHER_CS_ID = "other-cs";

  private static final String PROPERTY_CATEGORY = "category";
  private static final String PROPERTY_STATUS = "status";
  private static final String TARGET_CATEGORY = "target-category";
  private static final String TARGET_STATUS = "target-status";

  /**
   * Controls the hit rate for the two AND filters.
   *
   * <p>Every Nth concept matches the category filter, and every Mth concept matches the status
   * filter. A concept matches both filters only at LCM(N, M), giving an effective AND hit rate of
   * {@code 1 / lcm(N, M)}.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>{@code CATEGORY_MATCH_EVERY = 20, STATUS_MATCH_EVERY = 25} → LCM = 100 → ~1% hit rate
   *   <li>{@code CATEGORY_MATCH_EVERY = 10, STATUS_MATCH_EVERY = 10} → LCM = 10 → ~10% hit rate
   *   <li>{@code CATEGORY_MATCH_EVERY = 50, STATUS_MATCH_EVERY = 50} → LCM = 50 → ~2% hit rate
   *   <li>{@code CATEGORY_MATCH_EVERY = 100, STATUS_MATCH_EVERY = 100} → LCM = 100 → ~1% hit rate
   * </ul>
   */
  private static final int CATEGORY_MATCH_EVERY = 5;

  private static final int STATUS_MATCH_EVERY = 5;

  /** Number of additional "filler" properties per concept to simulate realistic property scan. */
  private static final int FILLER_PROPERTIES_PER_CONCEPT = 10;

  /** Number of warmup iterations to eliminate JIT compilation and cold-cache effects. */
  private static final int WARMUP_ITERATIONS = 100;

  /** Number of measured iterations whose average is reported as the benchmark result. */
  private static final int MEASURED_ITERATIONS = 500;

  /** Number of background documents from other CodeSystems to add noise to the index. */
  private static final int BACKGROUND_DOCUMENT_COUNT = 5000;

  @TempDir Path tempDir;

  @ParameterizedTest(name = "conceptCount={0}")
  @ValueSource(
      ints = {10, 50, 100, 500, 750, 1000, 1500, 2000, 3000, 4000, 5000, 10000, 20000, 50000})
  @DisplayName("Should compare Lucene vs in-memory filter performance with two AND filters")
  void shouldCompareLuceneVsInMemoryPerformance(int conceptCount) throws IOException {
    // GIVEN - Create CodeSystem with nested concepts and multiple properties
    CodeSystem codeSystem = createCodeSystemWithNestedConcepts(conceptCount);

    // Create a subdirectory per test run to avoid conflicts between parameterized runs
    Path indexPath = tempDir.resolve("index-" + conceptCount);

    try (Directory directory = new MMapDirectory(indexPath)) {

      // Index all documents with a single writer and force-merge to one segment
      try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig())) {
        for (int i = 0; i < conceptCount; i++) {
          writer.addDocument(createConceptDocument(TEST_CS_ID, i));
        }
        for (int i = 0; i < BACKGROUND_DOCUMENT_COUNT; i++) {
          writer.addDocument(createConceptDocument(OTHER_CS_ID, i));
        }
        writer.forceMerge(1);
        writer.commit();
      }

      // Open a single reusable reader + searcher (as in production)
      try (DirectoryReader reader = DirectoryReader.open(directory)) {
        IndexSearcher reusableSearcher = new IndexSearcher(reader);
        int totalDocs = reader.numDocs();

        // Build the Lucene query once (query construction is not part of the measurement)
        Query luceneQuery = buildTwoFilterLuceneQuery();

        // Warmup both paths to eliminate JIT and cold-cache effects
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
          executeInMemoryTwoFilters(codeSystem);
          executeLuceneFilter(reusableSearcher, luceneQuery);
        }

        // WHEN - Measure in-memory filter over multiple iterations
        long inMemoryTotal = 0;
        Set<String> inMemoryResults = null;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
          long start = System.nanoTime();
          inMemoryResults = executeInMemoryTwoFilters(codeSystem);
          inMemoryTotal += System.nanoTime() - start;
        }

        // WHEN - Measure Lucene filter over multiple iterations
        long luceneTotal = 0;
        Set<String> luceneResults = null;
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
          long start = System.nanoTime();
          luceneResults = executeLuceneFilter(reusableSearcher, luceneQuery);
          luceneTotal += System.nanoTime() - start;
        }

        // THEN - Both should return the same results
        assertThat(luceneResults)
            .as("Lucene and in-memory should return the same result codes")
            .isEqualTo(inMemoryResults);

        // Log performance comparison (averaged)
        double inMemoryAvgUs = (inMemoryTotal / (double) MEASURED_ITERATIONS) / 1_000.0;
        double luceneAvgUs = (luceneTotal / (double) MEASURED_ITERATIONS) / 1_000.0;
        double speedup = inMemoryAvgUs / luceneAvgUs;

        System.out.printf(
            "| %6d concepts | In-Memory: %10.1f us | Lucene: %10.1f us | Speedup: %6.2fx "
                + "| Matches: %d | Index docs: %d |%n",
            conceptCount, inMemoryAvgUs, luceneAvgUs, speedup, luceneResults.size(), totalDocs);
      }
    }
  }

  // ================================================================================================
  // CodeSystem creation — nested hierarchy with multiple properties
  // ================================================================================================

  /**
   * Creates a CodeSystem with a 3-level nested concept hierarchy and multiple properties per
   * concept.
   *
   * <p>The nesting distributes concepts across 3 levels:
   *
   * <ul>
   *   <li>Level 0: ~10% of concepts as top-level
   *   <li>Level 1: ~30% as children of level 0
   *   <li>Level 2: ~60% as children of level 1
   * </ul>
   *
   * <p>Each concept has {@value #FILLER_PROPERTIES_PER_CONCEPT} filler properties plus the two
   * filter properties ({@code category} and {@code status}). The hit rate is controlled by {@link
   * #CATEGORY_MATCH_EVERY} and {@link #STATUS_MATCH_EVERY}.
   *
   * @param conceptCount the total number of concepts to create
   * @return a CodeSystem with nested concepts and declared properties
   */
  @SuppressWarnings("java:S1994")
  private CodeSystem createCodeSystemWithNestedConcepts(int conceptCount) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(TEST_CS_ID);
    codeSystem.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.ISA);

    // Declare the two filter properties + filler properties
    codeSystem.addProperty().setCode(PROPERTY_CATEGORY).setType(CodeSystem.PropertyType.STRING);
    codeSystem.addProperty().setCode(PROPERTY_STATUS).setType(CodeSystem.PropertyType.STRING);
    for (int f = 0; f < FILLER_PROPERTIES_PER_CONCEPT; f++) {
      codeSystem.addProperty().setCode("filler-" + f).setType(CodeSystem.PropertyType.STRING);
    }

    // Distribute concepts across a 3-level hierarchy
    int topLevelCount = Math.max(1, conceptCount / 10);
    int midLevelPerTop = Math.max(1, conceptCount / (topLevelCount * 3));

    int globalIndex = 0;
    for (int t = 0; t < topLevelCount && globalIndex < conceptCount; t++) {
      CodeSystem.ConceptDefinitionComponent topConcept = createConceptWithProperties(globalIndex);
      codeSystem.addConcept(topConcept);
      globalIndex++;

      for (int m = 0; m < midLevelPerTop && globalIndex < conceptCount; m++) {
        CodeSystem.ConceptDefinitionComponent midConcept = createConceptWithProperties(globalIndex);
        topConcept.addConcept(midConcept);
        globalIndex++;

        // Remaining concepts fill the deepest level
        int leafCount =
            Math.max(
                0,
                (conceptCount - globalIndex)
                    / Math.max(1, (topLevelCount - t) * midLevelPerTop - m));
        for (int l = 0; l < leafCount && globalIndex < conceptCount; l++) {
          CodeSystem.ConceptDefinitionComponent leafConcept =
              createConceptWithProperties(globalIndex);
          midConcept.addConcept(leafConcept);
          globalIndex++;
        }
      }
    }

    return codeSystem;
  }

  /**
   * Creates a single concept with two filter properties and filler properties.
   *
   * <p>Hit criteria for two-filter AND:
   *
   * <ul>
   *   <li>Every {@link #CATEGORY_MATCH_EVERY}th concept has the target category value
   *   <li>Every {@link #STATUS_MATCH_EVERY}th concept has the target status value
   *   <li>Both match simultaneously at LCM of those values
   * </ul>
   */
  private static CodeSystem.ConceptDefinitionComponent createConceptWithProperties(int index) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("concept-" + index);
    concept.setDisplay("Concept " + index + " display text for searching");

    // Category property: every Nth matches
    String categoryValue =
        (index % CATEGORY_MATCH_EVERY == 0) ? TARGET_CATEGORY : "cat-" + (index % 7);
    concept.addProperty().setCode(PROPERTY_CATEGORY).setValue(new StringType(categoryValue));

    // Status property: every Mth matches
    String statusValue =
        (index % STATUS_MATCH_EVERY == 0) ? TARGET_STATUS : "status-" + (index % 4);
    concept.addProperty().setCode(PROPERTY_STATUS).setValue(new StringType(statusValue));

    // Filler properties to increase per-concept scan cost
    for (int f = 0; f < FILLER_PROPERTIES_PER_CONCEPT; f++) {
      concept
          .addProperty()
          .setCode("filler-" + f)
          .setValue(new StringType("filler-value-" + index + "-" + f));
    }

    return concept;
  }

  // ================================================================================================
  // Document creation
  // ================================================================================================

  /**
   * Creates a Lucene {@link Document} representing a single concept with scope, resource type,
   * CodeSystem ID, two filter property fields, filler properties, and a {@link SortedDocValues}
   * field for code extraction.
   *
   * @param codeSystemId the CodeSystem ID for the pre-filter field
   * @param index the concept index, used to derive the code and property values
   * @return a Lucene document ready for indexing
   */
  private Document createConceptDocument(String codeSystemId, int index) {
    Document doc = new Document();

    String code = "concept-" + index;
    ConceptComponentFieldGeneratorHelper.addCodeFieldToDocument(doc, code, Field.Store.YES);

    // Scope, resource type, and CodeSystem ID — matches the pre-filter used in production
    doc.add(new StringField(NAME_SCOPE, TEST_SCOPE, Field.Store.YES));
    doc.add(
        new StringField(
            FhirSearchHelper.RESOURCE_TYPE_FIELD, "ConceptDefinitionComponent", Field.Store.YES));
    doc.add(
        new StringField(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT, codeSystemId, Field.Store.YES));

    // Category property field
    String categoryFieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_CATEGORY + SEPARATOR + SUFFIX_STRING_EXACT;
    String categoryValue =
        (index % CATEGORY_MATCH_EVERY == 0) ? TARGET_CATEGORY : "cat-" + (index % 7);
    doc.add(new StringField(categoryFieldName, categoryValue, Field.Store.YES));

    // Status property field
    String statusFieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_STATUS + SEPARATOR + SUFFIX_STRING_EXACT;
    String statusValue =
        (index % STATUS_MATCH_EVERY == 0) ? TARGET_STATUS : "status-" + (index % 4);
    doc.add(new StringField(statusFieldName, statusValue, Field.Store.YES));

    // Filler property fields (not stored — no retrieval overhead)
    for (int f = 0; f < FILLER_PROPERTIES_PER_CONCEPT; f++) {
      String fillerFieldName =
          NAME_PREFIX_PROPERTY + SEPARATOR + "filler-" + f + SEPARATOR + SUFFIX_STRING_EXACT;
      doc.add(new StringField(fillerFieldName, "filler-value-" + index + "-" + f, Field.Store.NO));
    }

    return doc;
  }

  // ================================================================================================
  // In-memory filter execution — two AND filters with full concept tree traversal
  // ================================================================================================

  /**
   * Simulates the production in-memory path: flatten the concept tree, then for each concept scan
   * all properties for both filter criteria. A concept is included only if both filters match.
   */
  private Set<String> executeInMemoryTwoFilters(CodeSystem codeSystem) {
    List<CodeSystem.ConceptDefinitionComponent> allConcepts = new ArrayList<>();
    getAllConceptsAsList(codeSystem.getConcept(), allConcepts);

    // First filter: category = target
    Set<String> categoryMatches = new HashSet<>();
    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {
      for (CodeSystem.ConceptPropertyComponent prop : concept.getProperty()) {
        if (prop.getCode().equals(PROPERTY_CATEGORY)
            && prop.hasValueStringType()
            && prop.getValueStringType().getValue().equals(TARGET_CATEGORY)) {
          categoryMatches.add(concept.getCode());
          break;
        }
      }
    }

    // Second filter: status = target (only among category matches for AND semantics)
    Set<String> results = new HashSet<>();
    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {
      if (!categoryMatches.contains(concept.getCode())) {
        continue;
      }
      for (CodeSystem.ConceptPropertyComponent prop : concept.getProperty()) {
        if (prop.getCode().equals(PROPERTY_STATUS)
            && prop.hasValueStringType()
            && prop.getValueStringType().getValue().equals(TARGET_STATUS)) {
          results.add(concept.getCode());
          break;
        }
      }
    }

    return results;
  }

  // ================================================================================================
  // Lucene filter execution — single combined query with two AND clauses
  // ================================================================================================

  /**
   * Builds the full Lucene query: pre-filter (scope + type + codeSystem ID) combined with two
   * property MUST clauses — matching the production combined-filter path in {@code
   * VsExpandLuceneHelper.executeCombinedFilters()}.
   */
  private Query buildTwoFilterLuceneQuery() {
    // Pre-filter: scope + resource type + CodeSystem ID
    List<Query> filterClauses =
        List.of(
            new TermQuery(new Term(NAME_SCOPE, TEST_SCOPE)),
            new TermQuery(
                new Term(FhirSearchHelper.RESOURCE_TYPE_FIELD, "ConceptDefinitionComponent")),
            new TermQuery(new Term(NAME_ID + SEPARATOR + SUFFIX_TOKEN_EXACT, TEST_CS_ID)));
    Query preFilter = QueryCombinators.prepareFilterQuery(filterClauses);

    // Two AND property filters
    String categoryFieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_CATEGORY + SEPARATOR + SUFFIX_STRING_EXACT;
    String statusFieldName =
        NAME_PREFIX_PROPERTY + SEPARATOR + PROPERTY_STATUS + SEPARATOR + SUFFIX_STRING_EXACT;

    Query combinedPropertyQuery =
        QueryCombinators.prepareAndQuery(
            List.of(
                new TermQuery(new Term(categoryFieldName, TARGET_CATEGORY)),
                new TermQuery(new Term(statusFieldName, TARGET_STATUS))));

    return QueryCombinators.prepareCombinedFilterMustQuery(preFilter, combinedPropertyQuery);
  }

  /**
   * Executes the Lucene query using a pre-created, reusable {@link IndexSearcher} and extracts
   * concept codes via {@link SortedDocValues} on the {@code code_sort} field instead of loading
   * stored documents.
   */
  private Set<String> executeLuceneFilter(IndexSearcher searcher, Query query) throws IOException {
    TopDocs topDocs = searcher.search(query, 100_000);
    Set<String> results = new HashSet<>();
    String docValuesFieldName = NAME_CODE + SEPARATOR + SUFFIX_SORT;
    List<LeafReaderContext> leaves = searcher.getIndexReader().leaves();

    // Cache DocValues per leaf segment to avoid repeated lookups
    LeafReaderContext currentLeaf = null;
    SortedDocValues currentDocValues = null;

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      LeafReaderContext leafContext = findLeafContext(leaves, scoreDoc.doc);

      if (leafContext != currentLeaf) {
        currentLeaf = leafContext;
        currentDocValues = leafContext.reader().getSortedDocValues(docValuesFieldName);
      }

      if (currentDocValues != null) {
        int localDocId = scoreDoc.doc - leafContext.docBase;
        if (currentDocValues.advanceExact(localDocId)) {
          results.add(currentDocValues.lookupOrd(currentDocValues.ordValue()).utf8ToString());
        }
      }
    }

    return results;
  }

  /**
   * Finds the {@link LeafReaderContext} that contains the given global document ID.
   *
   * @param leaves the leaf reader contexts of the index
   * @param globalDocId the global document ID to locate
   * @return the leaf context containing the document
   */
  private static LeafReaderContext findLeafContext(
      List<LeafReaderContext> leaves, int globalDocId) {
    for (LeafReaderContext leaf : leaves) {
      if (globalDocId < leaf.docBase + leaf.reader().maxDoc()) {
        return leaf;
      }
    }
    throw new IllegalStateException("No leaf context found for document ID: " + globalDocId);
  }

  // ================================================================================================
  // Utility
  // ================================================================================================

  /**
   * Recursively flattens a nested concept hierarchy into a single list.
   *
   * @param codeSystemConcepts the concepts at the current hierarchy level
   * @param allConcepts the accumulator list for all concepts
   */
  private static void getAllConceptsAsList(
      List<CodeSystem.ConceptDefinitionComponent> codeSystemConcepts,
      List<CodeSystem.ConceptDefinitionComponent> allConcepts) {
    for (CodeSystem.ConceptDefinitionComponent concept : codeSystemConcepts) {
      allConcepts.add(concept);
      if (concept.hasConcept()) {
        getAllConceptsAsList(concept.getConcept(), allConcepts);
      }
    }
  }
}
