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
package de.gematik.zts.termzilla.terminologies.snomed.mapping;

import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeRangeRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SnomedMapper}. */
@DisplayName("SnomedMapper Tests")
class SnomedMapperTest {

  private static final String CASE_INSENSITIVE = SnomedConstants.SC_CASESINSENSITIVE;
  private static final String RULE_STRENGTH_ID = "723597001";
  private static final String CONTENT_TYPE_ID = "723596005";

  // ──────────────────────────────────────────────────────────────────────────
  // toSnomedConcept
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toSnomedConcept")
  class ToSnomedConcept {

    @Test
    @DisplayName("should map all fields from an active concept")
    void shouldMapAllFieldsFromActiveConcept() {
      // GIVEN
      Concept concept = SnomedTestData.concept(CONCEPT_DIABETES, EFFECTIVE_TIME_V1);

      // WHEN
      SnomedConcept result = SnomedMapper.toSnomedConcept(concept);

      // THEN
      assertThat(result.getId()).isEqualTo(CONCEPT_DIABETES);
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getDefinitionStatusId()).isEqualTo(SnomedConstants.SC_SUFFICENTLYDEFINED);
      assertThat(result.getSnomedConceptDescriptionList()).isEmpty();
    }

    @Test
    @DisplayName("should map inactive concept with active flag set to false")
    void shouldMapInactiveConcept() {
      // GIVEN
      Concept concept = SnomedTestData.inactiveConcept(CONCEPT_DIABETES, EFFECTIVE_TIME_V1);

      // WHEN
      SnomedConcept result = SnomedMapper.toSnomedConcept(concept);

      // THEN
      assertThat(result.isActive()).isFalse();
      assertThat(result.getId()).isEqualTo(CONCEPT_DIABETES);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toSnomedConceptDescription
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toSnomedConceptDescription")
  class ToSnomedConceptDescription {

    @Test
    @DisplayName("should map all fields from a description")
    void shouldMapAllFields() {
      // GIVEN
      Description description =
          SnomedTestData.fsnDescription(
              "desc-1", CONCEPT_DIABETES, "Diabetes mellitus (disorder)", EFFECTIVE_TIME_V1);

      // WHEN
      SnomedConceptDescription result = SnomedMapper.toSnomedConceptDescription(description);

      // THEN
      assertThat(result.getId()).isEqualTo("desc-1");
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getConceptId()).isEqualTo(CONCEPT_DIABETES);
      assertThat(result.getLanguageCode()).isEqualTo("en");
      assertThat(result.getTypeId()).isEqualTo(TYPE_FSN);
      assertThat(result.getTerm()).isEqualTo("Diabetes mellitus (disorder)");
      assertThat(result.getCaseSignificanceId()).isEqualTo(CASE_INSENSITIVE);
    }

    @Test
    @DisplayName("should initialise language list as empty")
    void shouldInitialiseEmptyLanguageList() {
      // GIVEN
      Description description =
          SnomedTestData.fsnDescription(
              "desc-1", CONCEPT_DIABETES, "Diabetes mellitus (disorder)", EFFECTIVE_TIME_V1);

      // WHEN
      SnomedConceptDescription result = SnomedMapper.toSnomedConceptDescription(description);

      // THEN
      assertThat(result.getLanguageList()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("should map inactive description")
    void shouldMapInactiveDescription() {
      // GIVEN
      Description description =
          SnomedTestData.description(
              "desc-2",
              CONCEPT_DIABETES,
              "en",
              TYPE_FSN,
              "Old term",
              EFFECTIVE_TIME_V1,
              false,
              MODULE_CORE);

      // WHEN
      SnomedConceptDescription result = SnomedMapper.toSnomedConceptDescription(description);

      // THEN
      assertThat(result.isActive()).isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toSnomedLanguage
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toSnomedLanguage")
  class ToSnomedLanguage {

    @Test
    @DisplayName("should map all 7 fields correctly")
    void shouldMapAllFields() {
      // GIVEN
      Language language =
          SnomedTestData.preferredLanguage("lang-1", "desc-1", REFSET_EN_US, EFFECTIVE_TIME_V1);

      // WHEN
      SnomedLanguage result = SnomedMapper.toSnomedLanguage(language);

      // THEN
      assertThat(result.getId()).isEqualTo("lang-1");
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.isActive()).isTrue();
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getRefsetId()).isEqualTo(REFSET_EN_US);
      assertThat(result.getReferencedComponentId()).isEqualTo("desc-1");
      assertThat(result.getAcceptabilityId()).isEqualTo(ACCEPTABILITY_PREFERRED);
    }

    @Test
    @DisplayName("should map inactive language entry")
    void shouldMapInactiveLanguageEntry() {
      // GIVEN
      Language language =
          SnomedTestData.language(
              "lang-2",
              "desc-1",
              REFSET_EN_US,
              ACCEPTABILITY_PREFERRED,
              EFFECTIVE_TIME_V1,
              false,
              MODULE_CORE);

      // WHEN
      SnomedLanguage result = SnomedMapper.toSnomedLanguage(language);

      // THEN
      assertThat(result.isActive()).isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toSnomedLanguageList
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toSnomedLanguageList")
  class ToSnomedLanguageList {

    @Test
    @DisplayName("should map all entries in the list")
    void shouldMapAllEntries() {
      // GIVEN
      Language lang1 =
          SnomedTestData.preferredLanguage("lang-1", "desc-1", REFSET_EN_US, EFFECTIVE_TIME_V1);
      Language lang2 =
          SnomedTestData.acceptableLanguage("lang-2", "desc-1", REFSET_EN_US, EFFECTIVE_TIME_V1);

      // WHEN
      List<SnomedLanguage> result = SnomedMapper.toSnomedLanguageList(List.of(lang1, lang2));

      // THEN
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo("lang-1");
      assertThat(result.get(1).getId()).isEqualTo("lang-2");
    }

    @Test
    @DisplayName("should return empty list for empty input")
    void shouldReturnEmptyListForEmptyInput() {
      // WHEN
      List<SnomedLanguage> result = SnomedMapper.toSnomedLanguageList(List.of());

      // THEN
      assertThat(result).isEmpty();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toMrcmDomainRule
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toMrcmDomainRule")
  class ToMrcmDomainRule {

    @Test
    @DisplayName("should map all fields from an active MRCM domain")
    void shouldMapAllFields() {
      // GIVEN
      MrcmDomain domain =
          MrcmDomain.builder()
              .id("domain-1")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(true)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_DOMAIN_REFSET_ID)
              .referencedComponentId("404684003")
              .domainConstraint("<< 404684003 |Clinical finding|")
              .parentDomain("138875005")
              .proximalPrimitiveConstraint("<< 404684003")
              .proximalPrimitiveRefinement("")
              .domainTemplateForPrecoordination("[[+id(<< 404684003)]]")
              .domainTemplateForPostcoordination("[[+id(<< 404684003)]]")
              .guideURL("http://example.com/guide")
              .build();

      // WHEN
      MrcmDomainRule result = SnomedMapper.toMrcmDomainRule(domain);

      // THEN
      assertThat(result.getId()).isEqualTo("domain-1");
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.getActive()).isEqualTo("1");
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getRefsetIdField()).isEqualTo(MRCM_DOMAIN_REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo("404684003");
      assertThat(result.getDomainConstraint()).isEqualTo("<< 404684003 |Clinical finding|");
      assertThat(result.getParentDomain()).isEqualTo("138875005");
      assertThat(result.getProximalPrimitiveConstraint()).isEqualTo("<< 404684003");
      assertThat(result.getProximalPrimitiveRefinement()).isEmpty();
      assertThat(result.getDomainTemplateForPrecoordination()).isEqualTo("[[+id(<< 404684003)]]");
      assertThat(result.getDomainTemplateForPostcoordination()).isEqualTo("[[+id(<< 404684003)]]");
      assertThat(result.getGuideURL()).isEqualTo("http://example.com/guide");
    }

    @Test
    @DisplayName("should convert active=true to '1'")
    void shouldConvertActiveTrueToOne() {
      // GIVEN
      MrcmDomain domain =
          SnomedTestData.mrcmDomain("domain-1", "404684003", "<< 404684003", EFFECTIVE_TIME_V1);

      // WHEN
      MrcmDomainRule result = SnomedMapper.toMrcmDomainRule(domain);

      // THEN
      assertThat(result.getActive()).isEqualTo("1");
    }

    @Test
    @DisplayName("should convert active=false to '0'")
    void shouldConvertActiveFalseToZero() {
      // GIVEN
      MrcmDomain domain =
          MrcmDomain.builder()
              .id("domain-2")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(false)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_DOMAIN_REFSET_ID)
              .referencedComponentId("404684003")
              .domainConstraint("")
              .parentDomain("")
              .proximalPrimitiveConstraint("")
              .proximalPrimitiveRefinement("")
              .domainTemplateForPrecoordination("")
              .domainTemplateForPostcoordination("")
              .guideURL("")
              .build();

      // WHEN
      MrcmDomainRule result = SnomedMapper.toMrcmDomainRule(domain);

      // THEN
      assertThat(result.getActive()).isEqualTo("0");
    }

    @Test
    @DisplayName(
        "should leave ruleStrengthId and contentTypeId as null (not present in MrcmDomain)")
    void shouldLeaveRuleStrengthAndContentTypeNull() {
      // GIVEN
      MrcmDomain domain =
          SnomedTestData.mrcmDomain("domain-1", "404684003", "<< 404684003", EFFECTIVE_TIME_V1);

      // WHEN
      MrcmDomainRule result = SnomedMapper.toMrcmDomainRule(domain);

      // THEN
      assertThat(result.getRuleStrengthId()).isNull();
      assertThat(result.getContentTypeId()).isNull();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toMrcmAttributeDomainRule
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toMrcmAttributeDomainRule")
  class ToMrcmAttributeDomainRule {

    @Test
    @DisplayName("should map all fields including grouped boolean to string")
    void shouldMapAllFields() {
      // GIVEN
      MrcmAttributeDomain attrDomain =
          MrcmAttributeDomain.builder()
              .id("attr-domain-1")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(true)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
              .referencedComponentId("246075003")
              .domainId("404684003")
              .grouped(true)
              .attributeCardinality("0..*")
              .attributeInGroupCardinality("0..1")
              .ruleStrengthId(RULE_STRENGTH_ID)
              .contentTypeId(CONTENT_TYPE_ID)
              .build();

      // WHEN
      MrcmAttributeDomainRule result = SnomedMapper.toMrcmAttributeDomainRule(attrDomain);

      // THEN
      assertThat(result.getId()).isEqualTo("attr-domain-1");
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.getActive()).isEqualTo("1");
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getRefsetIdField()).isEqualTo(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo("246075003");
      assertThat(result.getDomainId()).isEqualTo("404684003");
      assertThat(result.getGrouped()).isEqualTo("1");
      assertThat(result.getAttributeCardinality()).isEqualTo("0..*");
      assertThat(result.getAttributeInGroupCardinality()).isEqualTo("0..1");
      assertThat(result.getRuleStrengthId()).isEqualTo(RULE_STRENGTH_ID);
      assertThat(result.getContentTypeId()).isEqualTo(CONTENT_TYPE_ID);
    }

    @Test
    @DisplayName("should convert grouped=false to '0'")
    void shouldConvertGroupedFalseToZero() {
      // GIVEN
      MrcmAttributeDomain attrDomain =
          SnomedTestData.mrcmAttributeDomain(
              "attr-domain-2", "246075003", "404684003", EFFECTIVE_TIME_V1);

      // WHEN
      MrcmAttributeDomainRule result = SnomedMapper.toMrcmAttributeDomainRule(attrDomain);

      // THEN
      assertThat(result.getGrouped()).isEqualTo("0");
    }

    @Test
    @DisplayName("should convert inactive attribute domain")
    void shouldConvertInactiveAttributeDomain() {
      // GIVEN
      MrcmAttributeDomain attrDomain =
          MrcmAttributeDomain.builder()
              .id("attr-domain-3")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(false)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
              .referencedComponentId("246075003")
              .domainId("404684003")
              .grouped(false)
              .attributeCardinality("0..*")
              .attributeInGroupCardinality("0..1")
              .ruleStrengthId(RULE_STRENGTH_ID)
              .contentTypeId(CONTENT_TYPE_ID)
              .build();

      // WHEN
      MrcmAttributeDomainRule result = SnomedMapper.toMrcmAttributeDomainRule(attrDomain);

      // THEN
      assertThat(result.getActive()).isEqualTo("0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // toMrcmAttributeRangeRule
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toMrcmAttributeRangeRule")
  class ToMrcmAttributeRangeRule {

    @Test
    @DisplayName("should map all fields including refsetId to refsetIdField")
    void shouldMapAllFields() {
      // GIVEN
      MrcmAttributeRange attrRange =
          MrcmAttributeRange.builder()
              .id("attr-range-1")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(true)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_RANGE_REFSET_ID)
              .referencedComponentId("246075003")
              .rangeConstraint("<< 404684003 |Clinical finding|")
              .attributeRule("(< 404684003)")
              .ruleStrengthId(RULE_STRENGTH_ID)
              .contentTypeId(CONTENT_TYPE_ID)
              .build();

      // WHEN
      MrcmAttributeRangeRule result = SnomedMapper.toMrcmAttributeRangeRule(attrRange);

      // THEN
      assertThat(result.getId()).isEqualTo("attr-range-1");
      assertThat(result.getEffectiveTime()).isEqualTo(String.valueOf(EFFECTIVE_TIME_V1));
      assertThat(result.getActive()).isEqualTo("1");
      assertThat(result.getModuleId()).isEqualTo(MODULE_CORE);
      assertThat(result.getRefsetIdField()).isEqualTo(MRCM_ATTRIBUTE_RANGE_REFSET_ID);
      assertThat(result.getReferencedComponentId()).isEqualTo("246075003");
      assertThat(result.getRangeConstraint()).isEqualTo("<< 404684003 |Clinical finding|");
      assertThat(result.getAttributeRule()).isEqualTo("(< 404684003)");
      assertThat(result.getRuleStrengthId()).isEqualTo(RULE_STRENGTH_ID);
      assertThat(result.getContentTypeId()).isEqualTo(CONTENT_TYPE_ID);
    }

    @Test
    @DisplayName("should convert active=false to '0'")
    void shouldConvertActiveFalseToZero() {
      // GIVEN
      MrcmAttributeRange attrRange =
          MrcmAttributeRange.builder()
              .id("attr-range-2")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(false)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_RANGE_REFSET_ID)
              .referencedComponentId("246075003")
              .rangeConstraint("")
              .attributeRule("")
              .ruleStrengthId(RULE_STRENGTH_ID)
              .contentTypeId(CONTENT_TYPE_ID)
              .build();

      // WHEN
      MrcmAttributeRangeRule result = SnomedMapper.toMrcmAttributeRangeRule(attrRange);

      // THEN
      assertThat(result.getActive()).isEqualTo("0");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Helper methods
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("booleanToActiveString")
  class BooleanToActiveString {

    @Test
    @DisplayName("should return '1' for true")
    void shouldReturnOneForTrue() {
      assertThat(SnomedMapper.booleanToActiveString(true)).isEqualTo("1");
    }

    @Test
    @DisplayName("should return '0' for false")
    void shouldReturnZeroForFalse() {
      assertThat(SnomedMapper.booleanToActiveString(false)).isEqualTo("0");
    }
  }

  @Nested
  @DisplayName("effectiveTimeToString")
  class EffectiveTimeToString {

    @Test
    @DisplayName("should convert long to string")
    void shouldConvertLongToString() {
      assertThat(SnomedMapper.effectiveTimeToString(20240515L)).isEqualTo("20240515");
    }

    @Test
    @DisplayName("should handle zero effective time")
    void shouldHandleZeroEffectiveTime() {
      assertThat(SnomedMapper.effectiveTimeToString(0L)).isEqualTo("0");
    }
  }
}
