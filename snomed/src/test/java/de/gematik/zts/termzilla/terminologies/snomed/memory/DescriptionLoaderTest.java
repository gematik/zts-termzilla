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
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.DescriptionLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link DescriptionLoader}. */
@DisplayName("DescriptionLoader (Memory) Tests")
class DescriptionLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\tconceptId\tlanguageCode\ttypeId\tterm\tcaseSignificanceId";
  private static final String DESC_ID = "12345678";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String CONCEPT_ID = "73211009";
  private static final String LANGUAGE_CODE = "en";
  private static final String TYPE_FSN = SnomedConstants.SC_FULLYSPECIFIEDNAME;
  private static final String TERM = "Diabetes mellitus (disorder)";
  private static final String CASE_SIGNIFICANCE_ID = SnomedConstants.SC_CASESINSENSITIVE;

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private DescriptionLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new DescriptionLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all 9 fields of a valid description line")
    void shouldParseAllFieldsOfValidDescriptionLine() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              DESC_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              CONCEPT_ID,
              LANGUAGE_CODE,
              TYPE_FSN,
              TERM,
              CASE_SIGNIFICANCE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<Description> descriptions = model.getConceptIdToDescriptionMap().get(CONCEPT_ID);
      assertThat(descriptions).hasSize(1);
      Description result = descriptions.iterator().next();
      assertThat(result.getId()).isEqualTo(DESC_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getConceptId()).isEqualTo(CONCEPT_ID);
      assertThat(result.getLanguageCode()).isEqualTo(LANGUAGE_CODE);
      assertThat(result.getTypeId()).isEqualTo(TYPE_FSN);
      assertThat(result.getTerm()).isEqualTo(TERM);
      assertThat(result.getCaseSignificanceId()).isEqualTo(CASE_SIGNIFICANCE_ID);
    }

    @Test
    @DisplayName("Should handle terms with special characters")
    void shouldHandleTermsWithSpecialCharacters() throws IOException {
      // GIVEN
      String specialTerm = "Röntgenuntersuchung (Körperstruktur)";
      String dataLine =
          SnomedTestData.rf2Line(
              DESC_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              CONCEPT_ID,
              "de",
              TYPE_FSN,
              specialTerm,
              CASE_SIGNIFICANCE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<Description> descriptions = model.getConceptIdToDescriptionMap().get(CONCEPT_ID);
      assertThat(descriptions).hasSize(1);
      assertThat(descriptions.iterator().next().getTerm())
          .as("Term with umlauts and special characters should be preserved")
          .isEqualTo(specialTerm);
    }

    @Test
    @DisplayName("Should index by both conceptId and descriptionId")
    void shouldIndexByBothConceptIdAndDescriptionId() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              DESC_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              CONCEPT_ID,
              LANGUAGE_CODE,
              TYPE_FSN,
              TERM,
              CASE_SIGNIFICANCE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getConceptIdToDescriptionMap().containsKey(CONCEPT_ID))
          .as("Description should be indexed by conceptId")
          .isTrue();
      assertThat(model.getDescriptionIdToDescriptionMap().containsKey(DESC_ID))
          .as("Description should be indexed by descriptionId")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine = SnomedTestData.rf2Line(DESC_ID, EFFECTIVE_TIME, "1", MODULE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "descriptions.txt", HEADER, truncatedLine);

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
