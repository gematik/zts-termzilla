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
package de.gematik.zts.termzilla.terminologies.snomed.memory.loader;

import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
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

/**
 * Abstract base class for loading SNOMED CT data files into memory. Provides common functionality
 * for file reading, progress tracking, and timing. Subclasses must implement the {@link
 * #processLine(String, SnomedInMemoryRepresentation)} method to handle entity-specific parsing.
 */
@Slf4j
public abstract class AbstractSnomedLoader {

  private static final int BUFFER_SIZE = 64 * 1024;

  private long linesRead = 0;
  private final StopWatch sw = new StopWatch();
  private Timer timer;

  /**
   * Loads SNOMED CT data from the specified file into the model. Skips the header line and
   * processes each subsequent line.
   *
   * @param file the path to the SNOMED CT file to load
   * @param model the in-memory representation to populate
   */
  public void load(String file, SnomedInMemoryRepresentation model) {
    log.info("Loading: {}", file);
    linesRead = 0;

    sw.reset();
    sw.start();
    startProgressTimer();

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
        processLine(line, model);
      }

    } catch (IOException e) {
      log.error("Error loading file: {}", file, e);
      throw new SnomedDataLoadingException("Failed to load SNOMED file: " + file, e);
    } finally {
      sw.stop();
      if (timer != null) {
        timer.cancel();
      }
    }

    log.info("Loaded {} items in {} seconds", linesRead, sw.getTime(TimeUnit.SECONDS));
  }

  /** Starts a timer that periodically logs loading progress. */
  private void startProgressTimer() {
    timer = new Timer("SnomedLoadTimer", true);
    TimerTask timerTask =
        new TimerTask() {
          @Override
          public void run() {
            long seconds = sw.getTime(TimeUnit.SECONDS);
            if (seconds > 0) {
              log.debug("{} lines read --> {} lines/sec", linesRead, linesRead / seconds);
            }
          }
        };
    timer.scheduleAtFixedRate(timerTask, 2000, 1000);
  }

  /**
   * Processes a single line from the SNOMED CT file. Subclasses must implement this method to parse
   * the line and add the entity to the model.
   *
   * @param line the tab-separated line to process
   * @param model the SNOMED in-memory representation to add the entity to
   */
  protected abstract void processLine(String line, SnomedInMemoryRepresentation model);

  /**
   * Utility method to parse a boolean value from "1" or "0".
   *
   * @param value the string value to parse
   * @return {@code true} if value is "1", {@code false} otherwise
   */
  protected boolean parseBoolean(String value) {
    return "1".equals(value);
  }
}
