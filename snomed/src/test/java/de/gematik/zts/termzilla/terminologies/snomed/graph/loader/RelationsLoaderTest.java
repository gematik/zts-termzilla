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

import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.graph.RelationshipEdge;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.jgrapht.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link RelationsLoader}. */
@DisplayName("RelationsLoader Tests")
class RelationsLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId"
          + "\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String IS_A_TYPE = SnomedConstants.SC_IS_A_RELATIONSHIP_TYPE_ID;
  private static final String NON_IS_A_TYPE = "363698007";
  private static final String CHARACTERISTIC_TYPE = "900000000000011006";
  private static final String MODIFIER_ID = "900000000000451002";

  @TempDir Path tempDir;

  private Graph<String, RelationshipEdge> graph;
  private RelationsLoader loader;

  @BeforeEach
  void setUp() {
    graph = SnomedTestData.emptyGraph();
    loader = new RelationsLoader();
  }

  private String isALine(
      String id, String effectiveTime, String active, String source, String target) {
    return SnomedTestData.rf2Line(
        id,
        effectiveTime,
        active,
        MODULE_ID,
        source,
        target,
        "0",
        IS_A_TYPE,
        CHARACTERISTIC_TYPE,
        MODIFIER_ID);
  }

  private String nonIsALine(
      String id, String effectiveTime, String active, String source, String target) {
    return SnomedTestData.rf2Line(
        id,
        effectiveTime,
        active,
        MODULE_ID,
        source,
        target,
        "0",
        NON_IS_A_TYPE,
        CHARACTERISTIC_TYPE,
        MODIFIER_ID);
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should load IS-A relationship as graph edge")
    void shouldLoadIsARelationshipAsGraphEdge() throws IOException {
      // GIVEN
      String dataLine = isALine("rel-1", "20240515", "1", "73211009", "64572001");
      Path file = SnomedTestData.writeRf2File(tempDir, "relationships.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      assertThat(graph.containsVertex("73211009")).as("Source vertex should exist").isTrue();
      assertThat(graph.containsVertex("64572001")).as("Target vertex should exist").isTrue();
      Set<RelationshipEdge> edges = graph.getAllEdges("73211009", "64572001");
      assertThat(edges).hasSize(1);
      RelationshipEdge edge = edges.iterator().next();
      assertThat(edge.getId()).isEqualTo("rel-1");
      assertThat(edge.isActive(20240515L)).isTrue();
    }

    @Test
    @DisplayName("Should skip non-IS-A relationships")
    void shouldSkipNonIsARelationships() throws IOException {
      // GIVEN
      String dataLine = nonIsALine("rel-1", "20240515", "1", "73211009", "64572001");
      Path file = SnomedTestData.writeRf2File(tempDir, "relationships.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      assertThat(graph.edgeSet()).as("Non-IS-A relationships should be skipped").isEmpty();
    }

    @Test
    @DisplayName("Should add state to existing edge on duplicate relationship ID")
    void shouldAddStateToExistingEdge() throws IOException {
      // GIVEN - same relationship ID, same source/target, different effective times
      String line1 = isALine("rel-1", "20240101", "1", "73211009", "64572001");
      String line2 = isALine("rel-1", "20240601", "0", "73211009", "64572001");
      Path file = SnomedTestData.writeRf2File(tempDir, "relationships.txt", HEADER, line1, line2);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      Set<RelationshipEdge> edges = graph.getAllEdges("73211009", "64572001");
      assertThat(edges).hasSize(1);
      RelationshipEdge edge = edges.iterator().next();
      assertThat(edge.isActive(20240101L))
          .as("Edge should be active at first effective time")
          .isTrue();
      assertThat(edge.isActive(20240601L))
          .as("Edge should be inactive at second effective time")
          .isFalse();
    }

    @Test
    @DisplayName("Should create separate edges for different relationship IDs")
    void shouldCreateSeparateEdgesForDifferentIds() throws IOException {
      // GIVEN - different relationship IDs between same source and target
      String line1 = isALine("rel-1", "20240515", "1", "73211009", "64572001");
      String line2 = isALine("rel-2", "20240515", "1", "73211009", "64572001");
      Path file = SnomedTestData.writeRf2File(tempDir, "relationships.txt", HEADER, line1, line2);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      Set<RelationshipEdge> edges = graph.getAllEdges("73211009", "64572001");
      assertThat(edges)
          .as("Two different relationship IDs should create two separate edges")
          .hasSize(2);
    }

    @Test
    @DisplayName("Should parse active=0 correctly")
    void shouldParseInactiveRelationship() throws IOException {
      // GIVEN
      String dataLine = isALine("rel-1", "20240515", "0", "73211009", "64572001");
      Path file = SnomedTestData.writeRf2File(tempDir, "relationships.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      RelationshipEdge edge = graph.getAllEdges("73211009", "64572001").iterator().next();
      assertThat(edge.isActive(20240515L)).as("Edge with active=0 should be inactive").isFalse();
    }

    @Test
    @DisplayName("Should handle mixed IS-A and non-IS-A lines")
    void shouldHandleMixedIsAAndNonIsALines() throws IOException {
      // GIVEN
      String isALine1 = isALine("rel-1", "20240515", "1", "73211009", "64572001");
      String nonIsA = nonIsALine("rel-2", "20240515", "1", "73211009", "404684003");
      String isALine2 = isALine("rel-3", "20240515", "1", "64572001", "404684003");
      Path file =
          SnomedTestData.writeRf2File(
              tempDir, "relationships.txt", HEADER, isALine1, nonIsA, isALine2);

      // WHEN
      loader.load(file.toString(), graph);

      // THEN
      assertThat(graph.edgeSet()).as("Only IS-A edges should be in the graph").hasSize(2);
    }
  }
}
