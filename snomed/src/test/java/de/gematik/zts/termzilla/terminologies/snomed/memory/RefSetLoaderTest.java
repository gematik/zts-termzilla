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

import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.RefSetLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.RfEntry;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link RefSetLoader}. */
@DisplayName("RefSetLoader Tests")
class RefSetLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";
  private static final String ENTRY_ID = "44556677";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String REFSET_ID = "733073007";
  private static final String REFERENCED_COMPONENT_ID = "73211009";

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private RefSetLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new RefSetLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all fields of a valid refset line")
    void shouldParseAllFieldsOfValidRefSetLine() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID, EFFECTIVE_TIME, "1", MODULE_ID, REFSET_ID, REFERENCED_COMPONENT_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "refset.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<RfEntry> entries = model.getRefSetIdToConceptIdMap().get(REFSET_ID);
      assertThat(entries).hasSize(1);
      RefSet result = (RefSet) entries.iterator().next();
      assertThat(result.getId()).isEqualTo(ENTRY_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo(REFERENCED_COMPONENT_ID);
    }

    @Test
    @DisplayName("Should index by refsetId")
    void shouldIndexByRefsetId() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID, EFFECTIVE_TIME, "1", MODULE_ID, REFSET_ID, REFERENCED_COMPONENT_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "refset.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getRefSetIdToConceptIdMap().containsKey(REFSET_ID))
          .as("RefSet should be indexed by refsetId")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine = SnomedTestData.rf2Line(ENTRY_ID, EFFECTIVE_TIME, "1");
      Path file = SnomedTestData.writeRf2File(tempDir, "refset.txt", HEADER, truncatedLine);

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
