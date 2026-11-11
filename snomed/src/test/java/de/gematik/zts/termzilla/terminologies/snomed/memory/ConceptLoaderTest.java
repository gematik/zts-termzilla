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
package de.gematik.zts.termzilla.terminologies.snomed.memory;

import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.ConceptLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link ConceptLoader}. */
@DisplayName("ConceptLoader Tests")
class ConceptLoaderTest {

  private static final String HEADER = "id\teffectiveTime\tactive\tmoduleId\tdefinitionStatusId";
  private static final String CONCEPT_ID = "73211009";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String DEFINITION_STATUS_ID = SnomedConstants.SC_SUFFICENTLYDEFINED;

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private ConceptLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new ConceptLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all fields of a valid concept line")
    void shouldParseAllFieldsOfValidConceptLine() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(CONCEPT_ID, EFFECTIVE_TIME, "1", MODULE_ID, DEFINITION_STATUS_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "concepts.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<Concept> concepts = model.getConceptIdToConceptMap().get(CONCEPT_ID);
      assertThat(concepts).hasSize(1);
      Concept result = concepts.iterator().next();
      assertThat(result.getId()).isEqualTo(CONCEPT_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getDefinitionStatusId()).isEqualTo(DEFINITION_STATUS_ID);
    }

    @Test
    @DisplayName("Should parse inactive concept with active=0")
    void shouldParseInactiveConcept() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(CONCEPT_ID, EFFECTIVE_TIME, "0", MODULE_ID, DEFINITION_STATUS_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "concepts.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<Concept> concepts = model.getConceptIdToConceptMap().get(CONCEPT_ID);
      assertThat(concepts).hasSize(1);
      assertThat(concepts.iterator().next().isActive())
          .as("Concept with active=0 should be inactive")
          .isFalse();
    }

    @Test
    @DisplayName("Should load multiple concept lines")
    void shouldLoadMultipleConceptLines() throws IOException {
      // GIVEN
      String line1 =
          SnomedTestData.rf2Line("111111", EFFECTIVE_TIME, "1", MODULE_ID, DEFINITION_STATUS_ID);
      String line2 =
          SnomedTestData.rf2Line("222222", EFFECTIVE_TIME, "1", MODULE_ID, DEFINITION_STATUS_ID);
      String line3 =
          SnomedTestData.rf2Line("333333", EFFECTIVE_TIME, "1", MODULE_ID, DEFINITION_STATUS_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "concepts.txt", HEADER, line1, line2, line3);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getConceptIdToConceptMap().keySet()).hasSize(3);
      assertThat(model.getConceptIdToConceptMap().containsKey("111111")).isTrue();
      assertThat(model.getConceptIdToConceptMap().containsKey("222222")).isTrue();
      assertThat(model.getConceptIdToConceptMap().containsKey("333333")).isTrue();
    }

    @Test
    @DisplayName("Should skip header line automatically")
    void shouldSkipHeaderLineAutomatically() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(CONCEPT_ID, EFFECTIVE_TIME, "1", MODULE_ID, DEFINITION_STATUS_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "concepts.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getConceptIdToConceptMap().keySet())
          .as("Only the data line should be parsed, not the header")
          .hasSize(1);
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine = SnomedTestData.rf2Line(CONCEPT_ID, EFFECTIVE_TIME, "1");
      Path file = SnomedTestData.writeRf2File(tempDir, "concepts.txt", HEADER, truncatedLine);

      String fileName = file.toString();
      // WHEN / THEN
      assertThatThrownBy(() -> loader.load(fileName, model))
          .isInstanceOf(SnomedDataLoadingException.class);
    }

    @Test
    @DisplayName("Should throw on non-existent file")
    void shouldThrowOnNonExistentFile() {
      // GIVEN
      String nonExistentFile = tempDir.resolve("non_existent.txt").toString();

      // WHEN / THEN
      assertThatThrownBy(() -> loader.load(nonExistentFile, model))
          .isInstanceOf(SnomedDataLoadingException.class);
    }
  }
}
