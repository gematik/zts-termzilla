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
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.LanguageLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link LanguageLoader}. */
@DisplayName("LanguageLoader Tests")
class LanguageLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tacceptabilityId";
  private static final String LANG_ID = "99887766";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String REFSET_ID = "900000000000509007";
  private static final String DESCRIPTION_ID = "12345678";
  private static final String ACCEPTABILITY_PREFERRED = SnomedConstants.SC_PREFERRED;

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private LanguageLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new LanguageLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all fields of a valid language line")
    void shouldParseAllFieldsOfValidLanguageLine() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              LANG_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              DESCRIPTION_ID,
              ACCEPTABILITY_PREFERRED);
      Path file = SnomedTestData.writeRf2File(tempDir, "language.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<Language> languages = model.getDescriptionIdToLanguageMap().get(DESCRIPTION_ID);
      assertThat(languages).hasSize(1);
      Language result = languages.iterator().next();
      assertThat(result.getId()).isEqualTo(LANG_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo(DESCRIPTION_ID);
      assertThat(result.getAcceptabilityId()).isEqualTo(ACCEPTABILITY_PREFERRED);
    }

    @Test
    @DisplayName("Should index by descriptionId and refsetId")
    void shouldIndexByDescriptionIdAndRefsetId() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              LANG_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              DESCRIPTION_ID,
              ACCEPTABILITY_PREFERRED);
      Path file = SnomedTestData.writeRf2File(tempDir, "language.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getDescriptionIdToLanguageMap().containsKey(DESCRIPTION_ID))
          .as("Language should be indexed by descriptionId")
          .isTrue();
      assertThat(model.getRefSetIdToConceptIdMap().containsKey(REFSET_ID))
          .as("Language should be indexed by refsetId")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine = SnomedTestData.rf2Line(LANG_ID, EFFECTIVE_TIME, "1");
      Path file = SnomedTestData.writeRf2File(tempDir, "language.txt", HEADER, truncatedLine);

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
