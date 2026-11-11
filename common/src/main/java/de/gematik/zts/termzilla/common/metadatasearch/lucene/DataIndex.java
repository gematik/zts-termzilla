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

import de.gematik.zts.termzilla.common.metadatasearch.exceptions.DataIndexCreationException;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Provides access to a Lucene-backed metadata index and factory methods for search and writer
 * helpers.
 *
 * <p>The instance owns the underlying {@link Directory} and must be closed via {@link #close()}.
 */
@Slf4j
public record DataIndex(Directory index) implements AutoCloseable {

  /**
   * Creates a new index wrapper from a directory path string.
   *
   * @param indexDir the filesystem path to the index directory
   * @throws DataIndexCreationException if the Lucene directory cannot be opened
   * @deprecated Use {@link #DataIndex(Path)} to avoid implicit string-to-path conversion.
   */
  @Deprecated(forRemoval = true)
  public DataIndex(String indexDir) {
    this(openDirectory(Path.of(indexDir)));
  }

  /**
   * Creates a new index wrapper from a filesystem path.
   *
   * @param indexDir the filesystem path to the index directory
   * @throws DataIndexCreationException if the Lucene directory cannot be opened
   */
  public DataIndex(Path indexDir) {
    this(openDirectory(indexDir));
  }

  /**
   * Tries to create a new index wrapper from a filesystem path.
   *
   * @param indexDir the filesystem path to the index directory
   * @throws DataIndexCreationException if the Lucene directory cannot be opened
   */
  private static Directory openDirectory(Path indexDir) {
    try {
      log.info("Try to open index at {}", indexDir);
      return FSDirectory.open(indexDir);
    } catch (IOException e) {
      throw new DataIndexCreationException("Error while opening resource index.", e);
    }
  }

  /**
   * Creates a {@link Search} helper for this index.
   *
   * @return a search helper bound to this index
   * @throws IOException if the search helper cannot be created
   */
  public Search createSearch() throws IOException {
    return new Search(index);
  }

  /**
   * Creates a low-level Lucene {@link IndexSearcher} for this index.
   *
   * @return an {@link IndexSearcher} backed by a new {@link DirectoryReader}
   * @throws IOException if the reader cannot be opened
   */
  public IndexSearcher createSearcher() throws IOException {
    DirectoryReader reader = DirectoryReader.open(index);
    return new IndexSearcher(reader);
  }

  /**
   * Creates a writer for generic Lucene document operations.
   *
   * @param openMode the Lucene open mode to use
   * @return a configured {@link DocumentIndexWriter}
   */
  public DocumentIndexWriter createDocumentIndexWriter(IndexWriterConfig.OpenMode openMode) {
    return new DocumentIndexWriter(index, openMode);
  }

  /**
   * Creates a writer for terminology resource indexing.
   *
   * @param openMode the Lucene open mode to use
   * @return a configured {@link ResourceIndexWriter}
   */
  public ResourceIndexWriter createResourceIndexWriter(IndexWriterConfig.OpenMode openMode) {
    return new ResourceIndexWriter(index, openMode);
  }

  /**
   * Creates a writer for FHIR model indexing.
   *
   * @param openMode the Lucene open mode to use
   * @return a configured {@link FhirModelIndexWriter}
   */
  public FhirModelIndexWriter createFhirModelIndexWriter(IndexWriterConfig.OpenMode openMode) {
    return new FhirModelIndexWriter(index, openMode);
  }

  /**
   * Closes the underlying index directory.
   *
   * @throws IOException if closing the directory fails
   */
  @Override
  public void close() throws IOException {
    index.close();
  }
}
