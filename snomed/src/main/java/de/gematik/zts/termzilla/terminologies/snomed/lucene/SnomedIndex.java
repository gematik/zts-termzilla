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

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.LuceneQuerySanitizer;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.lucene.loader.DescriptionLoader;
import de.gematik.zts.termzilla.terminologies.snomed.lucene.queries.LuceneQueryFactory;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUQueryCache;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.UsageTrackingQueryCachingPolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;

/**
 * Lucene-based index for SNOMED CT description data.
 *
 * <p>Builds an in-memory Lucene index from RF2 description files at construction time and provides
 * query methods for searching descriptions by term, effective time, and release version. Supports
 * both flat and grouped queries with configurable sorting and caching.
 */
@Slf4j
public class SnomedIndex {

  private final IndexWriter writer;
  private final IndexSearcher searcher;
  private final QueryParser termQueryParser;

  /**
   * Creates a new index, loading RF2 description data from the configured release files.
   *
   * @param properties SNOMED configuration providing file paths and cache settings
   * @throws InvalidConfigurationException if the release files cannot be found or read
   */
  public SnomedIndex(SnomedConfigProperties properties) throws InvalidConfigurationException {

    log.info("");
    log.info(
        "######################################################################################");
    log.info("Generating Lucene-Model");

    try {
      log.info("Index-Storage-Location: {}", properties.getSnomedIndexLocation());

      // Index-Verzeichnis anlegen
      Directory fsIndex = FSDirectory.open(properties.getSnomedIndexLocation());

      Directory luceneIndex = new NRTCachingDirectory(fsIndex, 5.0, 500.0);

      log.info("Creating empty index ...");
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      writer = new IndexWriter(luceneIndex, indexWriterConfig);

      // Index mit Daten befüllen
      initializeFromRfFiles(properties.getSnomedUnzipLocation());

      // Indexwriter schließen
      writer.close();

      // Index für Suche vorbereiten
      IndexReader indexReader = DirectoryReader.open(luceneIndex);
      searcher = new IndexSearcher(indexReader);

      // Anzahl der gespeicherten Dokumente im Index ausgeben
      log.info("Lucene-Index contains {} documents", indexReader.numDocs());

      // these cache and policy instances can be shared across several queries and readers
      // it is fine to eg. store them into static variables
      final int maxNumberOfCachedQueries = properties.getIndexMaxCachedQueries();
      final long maxRamBytesUsed = properties.getIndexMaxRamBytesMb() * 1024L * 1024L;
      final QueryCache queryCache = new LRUQueryCache(maxNumberOfCachedQueries, maxRamBytesUsed);
      final QueryCachingPolicy defaultCachingPolicy = new UsageTrackingQueryCachingPolicy();
      searcher.setQueryCache(queryCache);
      searcher.setQueryCachingPolicy(defaultCachingPolicy);

      termQueryParser = new QueryParser(DescriptionLoader.INDEX_FIELDNAME_TERM, analyzer);
      termQueryParser.setDefaultOperator(QueryParser.Operator.AND);

    } catch (IOException e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  /**
   * Populates the Lucene index with description data from unpacked RF2 release files.
   *
   * @param snomedUnzipLocation path to the directory containing the unpacked RF2 release files
   * @throws InvalidConfigurationException if the directory does not exist or loading fails
   */
  public void initializeFromRfFiles(Path snomedUnzipLocation) throws InvalidConfigurationException {

    log.info(
        "Initializing Lucene-Index-Model from unpacked Release Files in: {}", snomedUnzipLocation);

    String descriptionsFile;

    try {
      log.info("Trying to load SNOMED-CT from {}", snomedUnzipLocation);
      if (!Files.exists(snomedUnzipLocation)) {
        throw new InvalidConfigurationException(
            "SNOMED unzip directory not found. Please adjust configuration");
      }

      descriptionsFile =
          SnomedReleaseFileHelper.getSingleReleaseFilePath(
                  snomedUnzipLocation, SnomedReleaseFileHelper.FILE_DESCRIPTIONS_PATTERN)
              .toAbsolutePath()
              .toString();

      long start = System.currentTimeMillis();
      DescriptionLoader loader = new DescriptionLoader();
      loader.load(descriptionsFile, writer);
      long stop = System.currentTimeMillis();

      log.info(
          "Duration to load SNOMED CT artifacts into Lucene-Index-Model: {} ms", (stop - start));

    } catch (Exception e) {
      throw new InvalidConfigurationException(e.getMessage());
    }
  }

  // =================================================================================================
  // Index-Abfragen
  // =================================================================================================

  /**
   * Searches for active 'descriptions' of a specific release version that contain the given search
   * term and returns the associated 'conceptIds'.
   *
   * @param filter Search term (e.g., "Kopf*")
   * @param snomedReleaseVersion Release version
   * @return Set of conceptIds
   * @throws InvalidRequestException if the filter contains unsafe patterns
   */
  public Set<String> queryActiveTerms(String filter, SnomedReleaseVersion snomedReleaseVersion) {

    StopWatch sw = new StopWatch();

    if (log.isDebugEnabled()) {
      sw.start();
    }

    // Validate and sanitize the filter input
    if (!LuceneQuerySanitizer.isSafeInput(filter)) {
      throw new InvalidRequestException("Filter contains unsafe patterns: " + filter);
    }

    // Sanitize the filter for use with QueryParser
    // Note: We use sanitizeForWildcardQuery because the filter supports wildcards (e.g., "Kopf*")
    String sanitizedFilter = LuceneQuerySanitizer.sanitizeForWildcardQuery(filter);

    if (log.isDebugEnabled() && !filter.equals(sanitizedFilter)) {
      log.debug("Sanitized filter: '{}' -> '{}'", filter, sanitizedFilter);
    }

    // Prepare result set
    Set<String> resultSet = new HashSet<>();

    try {

      // 1. Prepare search query:
      // We filter by the specified release version and the search term.
      List<Query> andQueryList = new ArrayList<>();
      Query baseQuery = LuceneQueryFactory.prepareReleaseVersionRangeQuery(snomedReleaseVersion);
      Query termQuery = termQueryParser.parse(sanitizedFilter);
      andQueryList.add(baseQuery);
      andQueryList.add(termQuery);
      Query finalQuery = LuceneQueryFactory.prepareAndQuery(andQueryList);

      if (log.isDebugEnabled()) {
        sw.stop();
        log.debug(
            "Prepared query '{}' in {} µs",
            finalQuery.toString(),
            sw.getTime(TimeUnit.MICROSECONDS));
        sw.reset();
        sw.start();
      }

      // 2. Search in Lucene index:
      // Results are sorted by effectiveTime in ascending order. This should enable us in the next
      // step to find the most current 'description'.
      List<Document> documents = queryIndexSortEffectiveTimeAscending(finalQuery, 1000000);

      if (log.isDebugEnabled()) {
        sw.stop();
        log.debug(
            "{} matching terms found in {} µs",
            documents.size(),
            sw.getTime(TimeUnit.MICROSECONDS));
        sw.reset();
        sw.start();
      }

      // 3. Reduce result set to the most current 'description' per 'conceptId':
      // For this we map the results to a Map where the 'conceptId' associated with the
      // 'description'
      // is used as the key
      Map<String, Document> uniqueDocuments =
          documents.stream()
              .collect(
                  Collectors.toMap(
                      document -> document.get(DescriptionLoader.INDEX_FIELDNAME_CONCEPT_ID),
                      document -> document,
                      (existing, replacement) -> replacement));

      if (log.isDebugEnabled()) {
        sw.stop();
        log.debug(
            "Reduced to {} unique concepts in {} µs",
            uniqueDocuments.size(),
            sw.getTime(TimeUnit.MICROSECONDS));
        sw.reset();
        sw.start();
      }

      // 4. Remove 'descriptions' that are no longer active for the selected release version:
      // For this we filter the map to 'descriptions' with the field 'active' = 1. The
      // remaining 'documents' are reduced to 'conceptIds' and added to the result set
      uniqueDocuments.values().stream()
          .filter(document -> document.get(DescriptionLoader.INDEX_FIELDNAME_ACTIVE).equals("1"))
          .forEach(
              document ->
                  resultSet.add(document.get(DescriptionLoader.INDEX_FIELDNAME_CONCEPT_ID)));

      if (log.isDebugEnabled()) {
        sw.stop();
        log.debug(
            "Reduced to {} concepts with active matching description in {} µs",
            resultSet.size(),
            sw.getTime(TimeUnit.MICROSECONDS));
      }

    } catch (ParseException e) {
      log.error("Failed to parse filter query: {}", sanitizedFilter, e);
      throw new InvalidRequestException("Invalid filter syntax: " + e.getMessage());
    }

    return resultSet;
  }

  // =================================================================================================

  /**
   * Queries the index without sorting and returns up to {@code maxNumberOfResults} documents.
   *
   * @param query the Lucene query to execute
   * @param maxNumberOfResults the maximum number of documents to return
   * @return the matching documents, or an empty list if an error occurs
   */
  public List<Document> queryIndex(Query query, int maxNumberOfResults) {
    try {
      TopDocs topDocs = searcher.search(query, maxNumberOfResults);
      return collectDocuments(topDocs);
    } catch (IOException e) {
      log.error("Error querying index", e);
    }
    return new ArrayList<>();
  }

  /**
   * Queries the index with the given sort order and returns up to {@code maxNumberOfResults}
   * documents.
   *
   * @param query the Lucene query to execute
   * @param maxNumberOfResults the maximum number of documents to return
   * @param sort the sort order for results
   * @return the matching documents, or an empty list if an error occurs
   */
  public List<Document> queryIndex(Query query, int maxNumberOfResults, Sort sort) {
    try {
      TopDocs topDocs = searcher.search(query, maxNumberOfResults, sort);
      return collectDocuments(topDocs);
    } catch (IOException e) {
      log.error("Error querying index", e);
    }
    return new ArrayList<>();
  }

  /**
   * Queries the index sorted by effective time in ascending order.
   *
   * @param query the Lucene query to execute
   * @param maxNumberOfResults the maximum number of documents to return
   * @return the matching documents sorted by ascending effective time
   */
  public List<Document> queryIndexSortEffectiveTimeAscending(Query query, int maxNumberOfResults) {
    return queryIndex(query, maxNumberOfResults, effectiveTimeSort(false));
  }

  /**
   * Queries the index sorted by effective time in descending order.
   *
   * @param query the Lucene query to execute
   * @param maxNumberOfResults the maximum number of documents to return
   * @return the matching documents sorted by descending effective time
   */
  public List<Document> queryIndexSortEffectiveTimeDecending(Query query, int maxNumberOfResults) {
    return queryIndex(query, maxNumberOfResults, effectiveTimeSort(true));
  }

  /**
   * Creates a {@link Sort} on the effective time field.
   *
   * @param reverse {@code true} for descending order, {@code false} for ascending
   * @return the configured sort instance
   */
  private Sort effectiveTimeSort(boolean reverse) {
    return new Sort(
        new SortField(
            DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME_SORTED,
            SortField.Type.STRING,
            reverse));
  }

  /**
   * Collects stored documents from the given search results.
   *
   * @param topDocs the Lucene search results
   * @return list of stored documents
   * @throws IOException if reading stored fields fails
   */
  private List<Document> collectDocuments(TopDocs topDocs) throws IOException {
    List<Document> documents = new ArrayList<>();
    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      documents.add(searcher.storedFields().document(scoreDoc.doc));
    }
    return documents;
  }
}
