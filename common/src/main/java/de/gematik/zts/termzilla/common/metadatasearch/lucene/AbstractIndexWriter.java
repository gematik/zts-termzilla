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

import de.gematik.zts.termzilla.common.metadatasearch.exceptions.LuceneIndexWriterInitializationException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.FhirStringAnalyzerSingleToken;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

@Slf4j
public class AbstractIndexWriter implements AutoCloseable {

  protected final IndexWriter writer;

  public AbstractIndexWriter(Directory index, IndexWriterConfig.OpenMode openMode) {

    ResourceSearchParamAnalyzer resourceSearchParamAnalyzer = new ResourceSearchParamAnalyzer();
    Map<String, Analyzer> fieldAnalyzerMap = resourceSearchParamAnalyzer.getFieldAnalyzerMap();

    PerFieldAnalyzerWrapper wrapper =
        new PerFieldAnalyzerWrapper(new FhirStringAnalyzerSingleToken(), fieldAnalyzerMap);

    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(wrapper);
    indexWriterConfig.setMergeScheduler(new ConcurrentMergeScheduler());

    indexWriterConfig.setOpenMode(openMode);
    try {
      writer = new IndexWriter(index, indexWriterConfig);
    } catch (IOException e) {
      throw new LuceneIndexWriterInitializationException(
          "Error while initializing index writer.", e);
    }
  }

  @Override
  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      log.warn("Error while closing IndexReader", e);
    }
  }
}
