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
import de.gematik.zts.termzilla.terminologies.snomed.memory.loader.MRCMAttributeDomainLoader;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link MRCMAttributeDomainLoader}. */
@DisplayName("MRCMAttributeDomainLoader Tests")
class MRCMAttributeDomainLoaderTest {

  private static final String HEADER =
      "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId"
          + "\tdomainId\tgrouped\tattributeCardinality\tattributeInGroupCardinality"
          + "\truleStrengthId\tcontentTypeId";
  private static final String ENTRY_ID = "33445566";
  private static final String EFFECTIVE_TIME = "20240515";
  private static final String MODULE_ID = SNOMED_CORE_MODULEID;
  private static final String REFSET_ID = "723604009";
  private static final String REFERENCED_COMPONENT_ID = "363698007";
  private static final String DOMAIN_ID = "404684003";
  private static final String ATTRIBUTE_CARDINALITY = "0..*";
  private static final String ATTRIBUTE_IN_GROUP_CARDINALITY = "0..1";
  private static final String RULE_STRENGTH_ID = "723597001";
  private static final String CONTENT_TYPE_ID = "723596005";

  @TempDir Path tempDir;

  private SnomedInMemoryRepresentation model;
  private MRCMAttributeDomainLoader loader;

  @BeforeEach
  void setUp() {
    model = new SnomedInMemoryRepresentation();
    loader = new MRCMAttributeDomainLoader();
  }

  @Nested
  @DisplayName("load() Tests")
  class LoadTests {

    @Test
    @DisplayName("Should parse all 12 fields correctly")
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
              DOMAIN_ID,
              "1",
              ATTRIBUTE_CARDINALITY,
              ATTRIBUTE_IN_GROUP_CARDINALITY,
              RULE_STRENGTH_ID,
              CONTENT_TYPE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_attr_domain.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      Collection<MrcmAttributeDomain> attrs =
          model.getAttributeIdToMRCMAttributeDomainMap().get(REFERENCED_COMPONENT_ID);
      assertThat(attrs).hasSize(1);
      MrcmAttributeDomain result = attrs.iterator().next();
      assertThat(result.getId()).isEqualTo(ENTRY_ID);
      assertThat(result.getEffectiveTime()).isEqualTo(20240515L);
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo(REFERENCED_COMPONENT_ID);
      assertThat(result.getDomainId()).isEqualTo(DOMAIN_ID);
      assertThat(result.isGrouped()).isTrue();
      assertThat(result.getAttributeCardinality()).isEqualTo(ATTRIBUTE_CARDINALITY);
      assertThat(result.getAttributeInGroupCardinality()).isEqualTo(ATTRIBUTE_IN_GROUP_CARDINALITY);
      assertThat(result.getRuleStrengthId()).isEqualTo(RULE_STRENGTH_ID);
      assertThat(result.getContentTypeId()).isEqualTo(CONTENT_TYPE_ID);
    }

    @Test
    @DisplayName("Should parse grouped=1 as true")
    void shouldParseGroupedOneAsTrue() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              REFERENCED_COMPONENT_ID,
              DOMAIN_ID,
              "1",
              ATTRIBUTE_CARDINALITY,
              ATTRIBUTE_IN_GROUP_CARDINALITY,
              RULE_STRENGTH_ID,
              CONTENT_TYPE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_attr_domain.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      MrcmAttributeDomain result =
          model
              .getAttributeIdToMRCMAttributeDomainMap()
              .get(REFERENCED_COMPONENT_ID)
              .iterator()
              .next();
      assertThat(result.isGrouped()).as("grouped=1 should be parsed as true").isTrue();
    }

    @Test
    @DisplayName("Should parse grouped=0 as false")
    void shouldParseGroupedZeroAsFalse() throws IOException {
      // GIVEN
      String dataLine =
          SnomedTestData.rf2Line(
              ENTRY_ID,
              EFFECTIVE_TIME,
              "1",
              MODULE_ID,
              REFSET_ID,
              REFERENCED_COMPONENT_ID,
              DOMAIN_ID,
              "0",
              ATTRIBUTE_CARDINALITY,
              ATTRIBUTE_IN_GROUP_CARDINALITY,
              RULE_STRENGTH_ID,
              CONTENT_TYPE_ID);
      Path file = SnomedTestData.writeRf2File(tempDir, "mrcm_attr_domain.txt", HEADER, dataLine);

      // WHEN
      loader.load(file.toString(), model);

      // THEN
      MrcmAttributeDomain result =
          model
              .getAttributeIdToMRCMAttributeDomainMap()
              .get(REFERENCED_COMPONENT_ID)
              .iterator()
              .next();
      assertThat(result.isGrouped()).as("grouped=0 should be parsed as false").isFalse();
    }

    @Test
    @DisplayName("Should throw on truncated line with too few fields")
    void shouldThrowOnTruncatedLine() throws IOException {
      // GIVEN
      String truncatedLine =
          SnomedTestData.rf2Line(ENTRY_ID, EFFECTIVE_TIME, "1", MODULE_ID, REFSET_ID);
      Path file =
          SnomedTestData.writeRf2File(tempDir, "mrcm_attr_domain.txt", HEADER, truncatedLine);

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
