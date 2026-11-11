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
package de.gematik.zts.termzilla.terminologies.snomed.lucene.loader;

import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the Lucene {@link DescriptionLoader}. */
@DisplayName("Lucene DescriptionLoader Tests")
class LuceneDescriptionLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String TYPE_FSN = SnomedConstants.SC_FULLYSPECIFIEDNAME;
  private static final String CASE_SIGNIFICANCE_ID = SnomedConstants.SC_CASESINSENSITIVE;

  @TempDir Path tempDir;

  private Directory directory;
  private IndexWriter writer;

  @BeforeEach
  void setUp() throws IOException {
    directory = new ByteBuffersDirectory();
    IndexWriterConfig config = new IndexWriterConfig();
    writer = new IndexWriter(directory, config);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (writer.isOpen()) {
      writer.close();
    }
    directory.close();
  }

  private String descriptionLine(
      String id, String effectiveTime, String active, String conceptId, String term) {
    return SnomedTestData.rf2Line(
        id,
        effectiveTime,
        active,
        MODULE_ID,
        conceptId,
        "en",
        TYPE_FSN,
        term,
        CASE_SIGNIFICANCE_ID);
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should index a description document with correct fields")
    void shouldIndexDescriptionDocumentWithCorrectFields() throws IOException {
      // GIVEN
      String dataLine =
          descriptionLine("12345678", "20240515", "1", "73211009", "Diabetes mellitus (disorder)");
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, dataLine);

      // WHEN
      DescriptionLoader loader = new DescriptionLoader();
      loader.load(file.toString(), writer);
      writer.commit();

      // THEN
      try (IndexReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).as("Index should contain exactly one document").isEqualTo(1);

        Document doc = reader.storedFields().document(0);
        assertThat(doc.get(DescriptionLoader.INDEX_FIELDNAME_CONCEPT_ID)).isEqualTo("73211009");
        assertThat(doc.get(DescriptionLoader.INDEX_FIELDNAME_ACTIVE)).isEqualTo("1");
        assertThat(doc.get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME)).isEqualTo("20240515");
        assertThat(doc.get(DescriptionLoader.INDEX_FIELDNAME_MODULE_ID)).isEqualTo(MODULE_ID);
      }
    }

    @Test
    @DisplayName("Should index multiple descriptions")
    void shouldIndexMultipleDescriptions() throws IOException {
      // GIVEN
      String line1 = descriptionLine("1001", "20240515", "1", "73211009", "Diabetes mellitus");
      String line2 = descriptionLine("1002", "20240515", "1", "38341003", "Hypertension");
      String line3 = descriptionLine("1003", "20240515", "1", "64572001", "Disease");
      Path file =
          SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, line1, line2, line3);

      // WHEN
      DescriptionLoader loader = new DescriptionLoader();
      loader.load(file.toString(), writer);
      writer.commit();

      // THEN
      try (IndexReader reader = DirectoryReader.open(directory)) {
        assertThat(reader.numDocs()).as("Index should contain three documents").isEqualTo(3);
      }
    }

    @Test
    @DisplayName("Should store conceptId as searchable StringField")
    void shouldStoreConceptIdAsSearchableStringField() throws IOException {
      // GIVEN
      String dataLine =
          descriptionLine("12345678", "20240515", "1", "73211009", "Diabetes mellitus (disorder)");
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, dataLine);

      // WHEN
      DescriptionLoader loader = new DescriptionLoader();
      loader.load(file.toString(), writer);
      writer.commit();

      // THEN
      try (IndexReader reader = DirectoryReader.open(directory)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        TermQuery query =
            new TermQuery(new Term(DescriptionLoader.INDEX_FIELDNAME_CONCEPT_ID, "73211009"));
        TopDocs results = searcher.search(query, 10);
        assertThat(results.totalHits.value())
            .as("TermQuery on conceptId should find the document")
            .isEqualTo(1);
      }
    }

    @Test
    @DisplayName("Should index effectiveTime as SortedDocValuesField")
    void shouldIndexEffectiveTimeAsSortedDocValuesField() throws IOException {
      // GIVEN
      String line1 = descriptionLine("1001", "20240515", "1", "73211009", "Diabetes");
      String line2 = descriptionLine("1002", "20240101", "1", "38341003", "Hypertension");
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, line1, line2);

      // WHEN
      DescriptionLoader loader = new DescriptionLoader();
      loader.load(file.toString(), writer);
      writer.commit();

      // THEN
      try (IndexReader reader = DirectoryReader.open(directory)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        Sort sort =
            new Sort(
                new SortField(
                    DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME_SORTED,
                    SortField.Type.STRING));
        TopDocs results =
            searcher.search(
                new TermQuery(new Term(DescriptionLoader.INDEX_FIELDNAME_ACTIVE, "1")), 10, sort);
        assertThat(results.totalHits.value()).isEqualTo(2);

        // First result should have the earlier effectiveTime
        Document firstDoc = searcher.storedFields().document(results.scoreDocs[0].doc);
        assertThat(firstDoc.get(DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME))
            .as("First document sorted by effectiveTime should be the earlier one")
            .isEqualTo("20240101");
      }
    }
  }
}
