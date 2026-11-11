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
import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SnomedDataLoadingException;
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.ModuleDependencyLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.ModuleDependency;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link ModuleDependencyLoader}. */
@DisplayName("ModuleDependencyLoader Tests")
class ModuleDependencyLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tsourceEffectiveTime\ttargetEffectiveTime";
  private static final String ENTRY_ID = "55667788";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_GERMANY_EDITION_MODULEID;
  private static final String REFSET_ID = "900000000000534007";
  private static final String TARGET_MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String SOURCE_EFFECTIVE_TIME = "20240515";
  private static final String TARGET_EFFECTIVE_TIME = "20240131";

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private ModuleDependencyLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new ModuleDependencyLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName(
        "Should parse all fields including sourceEffectiveTime and targetEffectiveTime as long")
    void shouldParseAllFieldsCorrectly() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              TARGET_MODULE_ID,
              SOURCE_EFFECTIVE_TIME,
              TARGET_EFFECTIVE_TIME);
      Path file = SnomedTestData.writeRf2File(tempDir, "moduledep.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<ModuleDependency> deps = model.getModuleIdToModuleDependencyMap().get(MODULE_ID);
      assertThat(deps).hasSize(1);
      ModuleDependency result = deps.iterator().next();
      assertThat(result.getId()).isEqualTo(ENTRY_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo(TARGET_MODULE_ID);
      assertThat(result.getSourceEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.getTargetEffectiveTime()).isEqualTo(20240131L);
    }

    @Test
    @DisplayName("Should index by moduleId")
    void shouldIndexByModuleId() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              TARGET_MODULE_ID,
              SOURCE_EFFECTIVE_TIME,
              TARGET_EFFECTIVE_TIME);
      Path file = SnomedTestData.writeRf2File(tempDir, "moduledep.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getModuleIdToModuleDependencyMap().containsKey(MODULE_ID))
          .as("Module dependency should be indexed by moduleId")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine = SnomedTestData.rf2Line(ENTRY_ID, EFFECTIVE_TIME, "1", MODULE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "moduledep.txt", HEADER, truncatedLine);

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
