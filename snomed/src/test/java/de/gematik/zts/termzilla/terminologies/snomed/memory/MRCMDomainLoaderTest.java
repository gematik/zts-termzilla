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
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.MRCMDomainLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link MRCMDomainLoader}. */
@DisplayName("MRCMDomainLoader Tests")
class MRCMDomainLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId"
          + "\tdomainConstraint\tparentDomain\tproximalPrimitiveConstraint"
          + "\tproximalPrimitiveRefinement\tdomainTemplateForPrecoordination"
          + "\tdomainTemplateForPostcoordination\tguideURL";
  private static final String ENTRY_ID = "11223344";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String REFSET_ID = "723589008";
  private static final String REFERENCED_COMPONENT_ID = "404684003";
  private static final String DOMAIN_CONSTRAINT = "<< 404684003 |Clinical finding|";
  private static final String PARENT_DOMAIN = "<< 138875005 |SNOMED CT Concept|";
  private static final String PROXIMAL_PRIMITIVE_CONSTRAINT = "<< 404684003 |Clinical finding|";
  private static final String PROXIMAL_PRIMITIVE_REFINEMENT = "";
  private static final String TEMPLATE_PRE = "[[+id(<< 404684003 |Clinical finding|)]]";
  private static final String TEMPLATE_POST = "[[+id(<< 404684003 |Clinical finding|)]]";
  private static final String GUIDE_URL = "http://example.com/guide";

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private MRCMDomainLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new MRCMDomainLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all 13 fields correctly")
    void shouldParseAllFieldsCorrectly() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              REFERENCED_COMPONENT_ID,
              DOMAIN_CONSTRAINT,
              PARENT_DOMAIN,
              PROXIMAL_PRIMITIVE_CONSTRAINT,
              PROXIMAL_PRIMITIVE_REFINEMENT,
              TEMPLATE_PRE,
              TEMPLATE_POST,
              GUIDE_URL);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_domain.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<MrcmDomain> domains =
          model.getConceptIdToMRCMDomainMap().get(REFERENCED_COMPONENT_ID);
      assertThat(domains).hasSize(1);
      MrcmDomain result = domains.iterator().next();
      assertThat(result.getId()).isEqualTo(ENTRY_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo(REFERENCED_COMPONENT_ID);
      assertThat(result.getDomainConstraint()).isEqualTo(DOMAIN_CONSTRAINT);
      assertThat(result.getParentDomain()).isEqualTo(PARENT_DOMAIN);
      assertThat(result.getProximalPrimitiveConstraint()).isEqualTo(PROXIMAL_PRIMITIVE_CONSTRAINT);
      assertThat(result.getProximalPrimitiveRefinement()).isEqualTo(PROXIMAL_PRIMITIVE_REFINEMENT);
      assertThat(result.getDomainTemplateForPrecoordination()).isEqualTo(TEMPLATE_PRE);
      assertThat(result.getDomainTemplateForPostcoordination()).isEqualTo(TEMPLATE_POST);
      assertThat(result.getGuideURL()).isEqualTo(GUIDE_URL);
    }

    @Test
    @DisplayName("Should index by referencedComponentId")
    void shouldIndexByReferencedComponentId() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              REFERENCED_COMPONENT_ID,
              DOMAIN_CONSTRAINT,
              PARENT_DOMAIN,
              PROXIMAL_PRIMITIVE_CONSTRAINT,
              PROXIMAL_PRIMITIVE_REFINEMENT,
              TEMPLATE_PRE,
              TEMPLATE_POST,
              GUIDE_URL);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_domain.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      assertThat(model.getConceptIdToMRCMDomainMap().containsKey(REFERENCED_COMPONENT_ID))
          .as("MRCM domain should be indexed by referencedComponentId")
          .isTrue();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine =
          SnomedTestData.rf2Line(ENTRY_ID, EFFECTIVE_TIME, "1", MODULE_ID, REFSET_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_domain.txt", HEADER, truncatedLine);

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
