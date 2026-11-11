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
package de.gematik.zts.termzilla.terminologies.snomed.graph.loader;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.graph.RelationshipEdge;
import de.gematik.zts.termzilla.terminologies.snomed.graph.RelationshipState;
import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.rf2.Rf2Columns;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jgrapht.Graph;

/**
 * Loads IS-A relationships from a SNOMED CT {@code sct2_Relationship_Full} RF2 file into a JGraphT
 * directed graph.
 *
 * <p>Only relationships whose type matches {@link SnomedConstants#SC_IS_A_RELATIONSHIP_TYPE_ID} are
 * loaded. All other relationship types are skipped. For each IS-A relationship, a {@link
 * RelationshipEdge} is created (or updated with an additional {@link RelationshipState} if the same
 * relationship ID already exists between the same source and target vertices).
 */
@Slf4j
public class RelationsLoader {

  private static final int BUFFER_SIZE = 64 * 1024;

  long linesRead = 0;
  StopWatch sw = new StopWatch();

  /**
   * Reads the specified RF2 relationship file and populates the given graph with IS-A edges.
   *
   * <p>The first line (header) is skipped. Progress is logged periodically while loading.
   *
   * @param file the path to the {@code sct2_Relationship_Full} RF2 file
   * @param graph the directed graph to populate with vertices and IS-A edges
   */
  public void load(String file, Graph<String, RelationshipEdge> graph) {

    log.info("Loading: {}", file);
    linesRead = 0;

    sw.reset();
    sw.start();
    TimerTask timerTask =
        new TimerTask() {
          @Override
          public void run() {
            log.debug(
                "{} lines read --> {} lines/sec",
                linesRead,
                linesRead / sw.getTime(TimeUnit.SECONDS));
          }
        };

    Timer timer = new Timer("SnomedLoadTimer");
    timer.scheduleAtFixedRate(timerTask, 2000, 1000);

    try (FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, Charset.defaultCharset());
        BufferedReader reader = new BufferedReader(isr, BUFFER_SIZE)) {
      // Skip header line
      @SuppressWarnings("java:S1481") // skipped header line
      String ignoredHeader = reader.readLine();

      String line;
      while ((line = reader.readLine()) != null) {
        linesRead++;
        process(line, graph);
      }

    } catch (IOException e) {
      log.error("Error loading file: {}", file, e);
    }

    sw.stop();
    timer.cancel();
    log.info("Loaded {} items in {} seconds", linesRead, sw.getTime(TimeUnit.SECONDS));
  }

  /**
   * Processes a single RF2 relationship line. If the relationship type is IS-A, the source and
   * target concepts are added as vertices and connected with a {@link RelationshipEdge}. If an edge
   * with the same relationship ID already exists, its state history is extended with a new {@link
   * RelationshipState}; otherwise a new edge is created.
   *
   * @param line the tab-separated RF2 line to process
   * @param versionGraph the graph to add the relationship to
   */
  private void process(String line, Graph<String, RelationshipEdge> versionGraph) {
    linesRead += 1;
    String[] lineParts =
        SnomedReleaseFileHelper.splitLine(line, Rf2Columns.Relationship.MIN_COLUMNS);

    String id = lineParts[Rf2Columns.ID];
    String effectiveTime = lineParts[Rf2Columns.EFFECTIVE_TIME];
    String active = lineParts[Rf2Columns.ACTIVE];
    String moduleId = lineParts[Rf2Columns.MODULE_ID];
    String source = lineParts[Rf2Columns.Relationship.SOURCE_ID];
    String target = lineParts[Rf2Columns.Relationship.DESTINATION_ID];
    String relType = lineParts[Rf2Columns.Relationship.TYPE_ID];

    // Only IS-A relationships are relevant for the concept hierarchy graph
    if (SnomedConstants.SC_IS_A_RELATIONSHIP_TYPE_ID.equals(relType)) {
      // Add vertices (no-op if they already exist)
      versionGraph.addVertex(source);
      versionGraph.addVertex(target);

      // Check if an edge with the same relationship ID already exists between source and target
      Set<RelationshipEdge> rseSet = versionGraph.getAllEdges(source, target);
      RelationshipEdge selectedRse = null;

      for (RelationshipEdge rse : rseSet) {
        if (rse.getId().equals(id)) {
          selectedRse = rse;
          break;
        }
      }

      if (selectedRse != null) {
        // Edge exists — add a new state entry for this effective time
        selectedRse.addState(
            new RelationshipState(Long.parseLong(effectiveTime), "1".equals(active)));

      } else {
        RelationshipEdge edge =
            new RelationshipEdge(
                id,
                moduleId,
                new RelationshipState(Long.parseLong(effectiveTime), "1".equals(active)));
        versionGraph.addEdge(source, target, edge);
      }
    }
  }
}
