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
package de.gematik.zts.termzilla.common.metadatasearch.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Search implements AutoCloseable {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DirectoryReader reader;
  private final IndexSearcher searcher;

  public Search(Directory index) throws IOException {
    reader = DirectoryReader.open(index);
    searcher = new IndexSearcher(reader);
  }

  /**
   * Returns Stream of documents that are the result of the given {@code query}. Since the documents
   * are loaded lazily this method can result in a lower memory spikes. This version implicitly
   * limits results to 100 entries.
   *
   * <p>Note that when running the returned stream to completion an {@link UncheckedIOException}
   * might be thrown. This is due to the fact that the result documents are read lazily on-demand.
   *
   * @param query the query to find documents
   * @return lazily loaded stream of result documents
   */
  public Stream<Document> runQueryStream(Query query) {
    return runQueryStream(query, 10000);
  }

  /**
   * Returns Stream of documents that are the result of the given {@code query}. Since the documents
   * are loaded lazily this method can result in a lower memory spikes.
   *
   * <p>Note that when running the returned stream to completion an {@link UncheckedIOException}
   * might be thrown. This is due to the fact that the result documents are read lazily on-demand.
   *
   * @param query the query to find documents
   * @param maxNumberOfResults maximum number of search results collected
   * @return lazily loaded stream of result documents
   */
  public Stream<Document> runQueryStream(Query query, int maxNumberOfResults) {

    LOGGER.debug("Running query: {}", query);

    try {
      TopDocs topDocs = searcher.search(query, maxNumberOfResults);
      LOGGER.debug("Number of results found: {}", topDocs.totalHits.value());

      return Stream.of(topDocs.scoreDocs).map(scoreDoc -> scoreDoc.doc).map(this::getDoc);

    } catch (IOException e) {
      LOGGER.warn("Error while executing query.", e);
    }

    return Stream.empty();
  }

  private Document getDoc(int doc) throws UncheckedIOException {
    try {
      return searcher.storedFields().document(doc);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public List<Document> runQuery(Query query) {
    return runQuery(query, 100);
  }

  public List<Document> runQuery(Query query, int maxNumberOfResults) {

    LOGGER.debug("Running query: {}", query);
    try {
      var result = new ArrayList<Document>();
      runQueryStream(query, maxNumberOfResults).forEach(result::add);
      return result;
    } catch (UncheckedIOException e) {
      LOGGER.warn("Error while executing query: {}", e.getMessage());
    }

    // return empty list
    return List.of();
  }

  public long getNumberOfDocuments() {
    return reader.numDocs();
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
