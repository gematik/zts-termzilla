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
package de.gematik.zts.termzilla.common.metadatasearch.integration.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.CodeSystemFieldGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.ConceptMapFieldGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.ValueSetFieldGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Helper class for creating and managing in-memory Lucene indices for integration testing.
 *
 * <p>This class provides a convenient API for testing the complete Lucene indexing and search cycle
 * without requiring disk I/O. It uses Lucene's {@link ByteBuffersDirectory} to maintain an
 * in-memory index that is fast and isolated between tests.
 *
 * <p><b>Key Features:</b>
 *
 * <ul>
 *   <li>In-memory index using {@link ByteBuffersDirectory} for fast test execution
 *   <li>Uses production {@link CodeSystemFieldGenerator}, {@link ValueSetFieldGenerator}, and
 *       {@link ConceptMapFieldGenerator} for indexing
 *   <li>Query execution with result deserialization back to FHIR resources
 *   <li>AutoCloseable for proper resource cleanup
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * try (InMemoryLuceneTestHelper helper = new InMemoryLuceneTestHelper()) {
 *   // Index a CodeSystem using production field generator
 *   CodeSystem cs = new CodeSystem();
 *   cs.setId("snomed-ct");
 *   cs.setName("SNOMED CT");
 *   helper.indexResource(cs);
 *
 *   // Search by ID using production query generator
 *   Query query = IdTokenParameterQueryGenerator.prepareQuery("_id", new TokenParam("snomed-ct"));
 *   List<CodeSystem> results = helper.search(query, CodeSystem.class);
 *
 *   // Verify
 *   assertEquals(1, results.size());
 *   assertEquals("snomed-ct", results.get(0).getId());
 * }
 * }</pre>
 *
 * @see CodeSystemFieldGenerator
 * @see ValueSetFieldGenerator
 * @see ConceptMapFieldGenerator
 */
public class InMemoryLuceneTestHelper implements AutoCloseable {

  private final Directory directory;
  private final IndexWriter indexWriter;
  private final FhirContext fhirContext;
  private final IParser jsonParser;

  // Production field generators
  private final CodeSystemFieldGenerator codeSystemFieldGenerator;
  private final ValueSetFieldGenerator valueSetFieldGenerator;
  private final ConceptMapFieldGenerator conceptMapFieldGenerator;

  /**
   * Creates a new in-memory Lucene test helper.
   *
   * <p>Initializes the in-memory index and creates instances of all production field generators.
   *
   * @throws IOException if the index writer cannot be created
   */
  public InMemoryLuceneTestHelper() throws IOException {
    this.directory = new ByteBuffersDirectory();
    this.fhirContext = FhirContext.forR4Cached();
    this.jsonParser = fhirContext.newJsonParser();

    // Initialize production field generators
    this.codeSystemFieldGenerator = new CodeSystemFieldGenerator();
    this.valueSetFieldGenerator = new ValueSetFieldGenerator();
    this.conceptMapFieldGenerator = new ConceptMapFieldGenerator();

    IndexWriterConfig config = new IndexWriterConfig();
    this.indexWriter = new IndexWriter(directory, config);
  }

  /**
   * Indexes a FHIR resource into the in-memory Lucene index using production field generators.
   *
   * <p>This method delegates to the appropriate production {@code FieldGenerator} class based on
   * the resource type:
   *
   * <ul>
   *   <li>{@link CodeSystem} → {@link CodeSystemFieldGenerator}
   *   <li>{@link ValueSet} → {@link ValueSetFieldGenerator}
   *   <li>{@link ConceptMap} → {@link ConceptMapFieldGenerator}
   * </ul>
   *
   * <p><b>Important:</b> This uses {@link Field.Store#YES} to ensure all indexed fields are also
   * stored for retrieval during search.
   *
   * @param resource the FHIR resource to index
   * @throws IOException if indexing fails
   * @throws IllegalArgumentException if the resource type is not supported
   */
  public void indexResource(IBaseResource resource) throws IOException {
    Document doc = new Document();

    if (resource instanceof CodeSystem) {
      codeSystemFieldGenerator.createFields(doc, (CodeSystem) resource, Field.Store.YES);
    } else if (resource instanceof ValueSet) {
      valueSetFieldGenerator.createFields(doc, (ValueSet) resource, Field.Store.YES);
    } else if (resource instanceof ConceptMap) {
      conceptMapFieldGenerator.createFields(doc, (ConceptMap) resource, Field.Store.YES);
    } else {
      throw new IllegalArgumentException(
          "Unsupported resource type: " + resource.getClass().getName());
    }

    indexWriter.addDocument(doc);
    indexWriter.commit();
  }

  public void indexDocument(Document doc) throws IOException {
    indexWriter.addDocument(doc);
    indexWriter.commit();
  }

  /**
   * Indexes multiple documents in a single batch and commits once.
   *
   * <p>This is significantly more efficient than calling {@link #indexDocument(Document)}
   * repeatedly, because it avoids per-document commits and produces an optimized index with fewer
   * segments.
   *
   * @param docs the documents to index
   * @throws IOException if indexing fails
   */
  public void indexDocumentsBatch(List<Document> docs) throws IOException {
    for (Document doc : docs) {
      indexWriter.addDocument(doc);
    }
    indexWriter.commit();
  }

  /**
   * Creates a reusable {@link IndexSearcher} backed by the current index state.
   *
   * <p>Unlike {@link #search(Query)}, which opens a new reader per call, this method returns a
   * searcher that can be reused for multiple queries — matching production behavior where the
   * searcher is created once and shared.
   *
   * <p><b>Important:</b> The caller is responsible for closing the underlying reader when done. Use
   * the returned searcher only while the helper is still open.
   *
   * @return an {@link IndexSearcher} over the current index
   * @throws IOException if the reader cannot be opened
   */
  public IndexSearcher createReusableSearcher() throws IOException {
    DirectoryReader reader = DirectoryReader.open(directory);
    return new IndexSearcher(reader);
  }

  /**
   * Searches the in-memory index and returns matching documents.
   *
   * <p>This method executes the provided Lucene query against the in-memory index, retrieves
   * matching documents.
   *
   * <p><b>Note:</b> The query should use the same field names that the production field generators
   * create (e.g., "_id_tokenexact", "name_exact", etc.).
   *
   * @param query the Lucene query to execute
   * @return a list of documents (may be empty)
   * @throws IOException if search or deserialization fails
   */
  public List<Document> search(Query query) throws IOException {
    DirectoryReader reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);

    int docCount = reader.numDocs();
    System.out.println("Anzahl der Dokumente im Index: " + docCount);

    TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
    List<Document> results = new ArrayList<>();

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      results.add(searcher.storedFields().document(scoreDoc.doc));
    }

    reader.close();
    return results;
  }

  /**
   * Searches the in-memory index and returns raw Lucene documents.
   *
   * <p>This method is useful when you need to verify the indexed field values directly without
   * deserializing to FHIR resources. You can inspect field names and values to ensure correct
   * indexing.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * List<Document> docs = helper.searchDocuments(query);
   * assertEquals("snomed-ct", docs.get(0).get("_id_tokenexact"));
   * assertEquals("SNOMED CT", docs.get(0).get("name_exact"));
   * }</pre>
   *
   * @param query the Lucene query to execute
   * @return a list of matching Lucene documents (may be empty)
   * @throws IOException if search fails
   */
  public List<Document> searchDocuments(Query query) throws IOException {
    DirectoryReader reader = DirectoryReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);

    TopDocs topDocs = searcher.search(query, 100);
    List<Document> results = new ArrayList<>();

    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      results.add(searcher.storedFields().document(scoreDoc.doc));
    }

    reader.close();
    return results;
  }

  /**
   * Returns the total number of documents in the index.
   *
   * <p>Useful for verifying that resources were indexed correctly.
   *
   * @return the document count
   * @throws IOException if reading the index fails
   */
  public int getDocumentCount() throws IOException {
    DirectoryReader reader = DirectoryReader.open(directory);
    int count = reader.numDocs();
    reader.close();
    return count;
  }

  /**
   * Closes the index writer and directory, releasing all resources.
   *
   * <p>This method is automatically called when using try-with-resources.
   *
   * @throws IOException if closing fails
   */
  @Override
  public void close() throws IOException {
    if (indexWriter != null) {
      indexWriter.close();
    }
    if (directory != null) {
      directory.close();
    }
  }

  // ================================================================================
  // Private Helper Methods
  // ================================================================================

  /**
   * Deserializes a FHIR resource from the stored "resource" field in a Lucene document.
   *
   * @param <T> the resource type
   * @param doc the Lucene document
   * @param resourceType the expected resource class
   * @return the deserialized resource, or null if the stored field is missing
   */
  @SuppressWarnings("unchecked")
  private <T extends IBaseResource> T deserializeResource(Document doc, Class<T> resourceType) {
    String storedJson = doc.get("resource");
    if (storedJson == null) {
      return null;
    }

    return (T) jsonParser.parseResource(resourceType, storedJson);
  }
}
