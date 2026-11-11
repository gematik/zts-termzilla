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

import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.rf2.Rf2Columns;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.BytesRef;

/**
 * Loads SNOMED RF2 description rows into a Lucene index.
 *
 * <p>Each data row is mapped to a reusable Lucene {@link Document} and written via an {@link
 * IndexWriter}.
 */
@Slf4j
public class DescriptionLoader {

  private static final int BUFFER_SIZE = 64 * 1024;

  public static final String INDEX_FIELDNAME_CONCEPT_ID = "conceptId";
  public static final String INDEX_FIELDNAME_TERM = "term";
  public static final String INDEX_FIELDNAME_ACTIVE = "active";
  public static final String INDEX_FIELDNAME_EFFECTIVE_TIME = "effectiveTime";
  public static final String INDEX_FIELDNAME_EFFECTIVE_TIME_SORTED = "effectiveTime_sorted";
  public static final String INDEX_FIELDNAME_MODULE_ID = "moduleId";

  private final StringField conceptIdField =
      new StringField(INDEX_FIELDNAME_CONCEPT_ID, "", Field.Store.YES);
  private final TextField termField = new TextField(INDEX_FIELDNAME_TERM, "", Field.Store.NO);
  private final Field activeField = new StringField(INDEX_FIELDNAME_ACTIVE, "", Field.Store.YES);
  private final StringField effectiveTimeField =
      new StringField(INDEX_FIELDNAME_EFFECTIVE_TIME, "", Field.Store.YES);
  private final SortedDocValuesField effectiveTimeFieldSorted =
      new SortedDocValuesField(INDEX_FIELDNAME_EFFECTIVE_TIME_SORTED, new BytesRef(""));

  private final Field moduleIdField =
      new StringField(INDEX_FIELDNAME_MODULE_ID, "", Field.Store.YES);

  private final Document document;

  private long linesRead = 0;
  private final StopWatch sw = new StopWatch();

  private final Timer timer = new Timer("SnomedLoadTimer");
  private final TimerTask timerTask =
      new TimerTask() {
        @Override
        public void run() {
          log.debug(
              "{} lines read --> {} lines/sec",
              linesRead,
              linesRead / sw.getTime(TimeUnit.SECONDS));
        }
      };

  /** Creates a loader with a preconfigured reusable Lucene {@link Document}. */
  public DescriptionLoader() {
    document = new Document();
    document.add(activeField);
    document.add(effectiveTimeField);
    document.add(effectiveTimeFieldSorted);
    document.add(moduleIdField);
    document.add(conceptIdField);
    document.add(termField);
  }

  /**
   * Reads a SNOMED RF2 description file and indexes all data rows.
   *
   * @param file path to the RF2 description file
   * @param writer Lucene index writer used to persist documents
   */
  public void load(String file, IndexWriter writer) {

    log.info("Loading: {}", file);
    linesRead = 0;

    sw.reset();
    sw.start();

    timer.scheduleAtFixedRate(timerTask, 2000, 1000);

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()),
            BUFFER_SIZE)) {
      // Skip header line
      @SuppressWarnings("java:S1481") // skipped header line
      String headerLine = reader.readLine();

      String line;
      while ((line = reader.readLine()) != null) {
        linesRead++;
        process(line, writer);
      }

    } catch (IOException e) {
      log.error("Error loading file: {}", file, e);
    }

    sw.stop();
    timer.cancel();

    log.info("Loaded {} items in {} seconds", linesRead, sw.getTime(TimeUnit.SECONDS));
  }

  /**
   * Parses and indexes a single RF2 description row.
   *
   * @param line RF2 line content
   * @param writer Lucene index writer used to add the document
   * @throws SnomedDataLoadingException if writing to the index fails
   */
  private void process(String line, IndexWriter writer) {
    linesRead += 1;

    String[] lineParts =
        SnomedReleaseFileHelper.splitLine(line, Rf2Columns.Description.MIN_COLUMNS);

    try {
      effectiveTimeField.setStringValue(lineParts[Rf2Columns.EFFECTIVE_TIME]);
      effectiveTimeFieldSorted.setBytesValue(new BytesRef(lineParts[Rf2Columns.EFFECTIVE_TIME]));
      activeField.setStringValue(lineParts[Rf2Columns.ACTIVE]);
      moduleIdField.setStringValue(lineParts[Rf2Columns.MODULE_ID]);
      conceptIdField.setStringValue(lineParts[Rf2Columns.Description.CONCEPT_ID]);
      termField.setStringValue(lineParts[Rf2Columns.Description.TERM]);
      writer.addDocument(document);
    } catch (IOException e) {
      throw new SnomedDataLoadingException(e.getMessage(), e);
    }
  }
}
