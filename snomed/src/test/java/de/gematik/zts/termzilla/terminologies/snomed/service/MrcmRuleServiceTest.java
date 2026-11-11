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
package de.gematik.zts.termzilla.terminologies.snomed.service;

import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_CLINICAL_FINDING;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_PROCEDURE;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.DATE_V2;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.EFFECTIVE_TIME_V1;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.EFFECTIVE_TIME_V2;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.MODULE_CORE;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.MRCM_ATTRIBUTE_DOMAIN_REFSET_ID;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.MRCM_ATTRIBUTE_RANGE_REFSET_ID;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.coreVersion;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.defaultCoreVersion;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.mrcmAttributeDomain;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.mrcmAttributeRange;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.mrcmDomain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeRangeRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmDomainRule;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO: nochmal mit echten Regeln hinterlegen

/** Unit tests for {@link MrcmRuleService}. */
@DisplayName("MrcmRuleService Tests")
@ExtendWith(MockitoExtension.class)
class MrcmRuleServiceTest {

  private static final String ATTRIBUTE_FINDING_SITE = "363698007";
  private static final String ATTRIBUTE_CAUSATIVE_AGENT = "246075003";

  @Mock private SnomedInMemoryRepresentation snomedInMemoryRepresentation;

  private SnomedReleaseVersion defaultVersion;
  private SnomedReleaseVersion explicitVersion;
  private MrcmRuleService mrcmRuleService;

  @BeforeEach
  void setUp() {
    defaultVersion = defaultCoreVersion();
    explicitVersion = coreVersion(DATE_V2);
    mrcmRuleService = new MrcmRuleService(snomedInMemoryRepresentation, defaultVersion);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getMrcmDomainRules
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getMrcmDomainRules")
  class GetMrcmDomainRules {

    @Test
    @DisplayName("should return mapped domain rules for the given version")
    void shouldReturnMappedDomainRulesForGivenVersion() {
      // GIVEN
      MrcmDomain domain =
          mrcmDomain(
              "d1", CONCEPT_CLINICAL_FINDING, "<< 404684003 |Clinical finding|", EFFECTIVE_TIME_V1);
      when(snomedInMemoryRepresentation.getMrcmDomainList(explicitVersion, false))
          .thenReturn(List.of(domain));

      // WHEN
      Set<MrcmDomainRule> result = mrcmRuleService.getMrcmDomainRules(explicitVersion, false);

      // THEN
      assertThat(result).hasSize(1);
      MrcmDomainRule rule = result.iterator().next();
      assertThat(rule.getReferencedComponentId()).isEqualTo(CONCEPT_CLINICAL_FINDING);
      assertThat(rule.getDomainConstraint()).isEqualTo("<< 404684003 |Clinical finding|");
      assertThat(rule.getActive()).isEqualTo("1");
    }

    @Test
    @DisplayName("should fall back to default version when version is null")
    void shouldFallBackToDefaultVersionWhenNull() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmDomainList(defaultVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmDomainRule> result = mrcmRuleService.getMrcmDomainRules(null, false);

      // THEN
      verify(snomedInMemoryRepresentation).getMrcmDomainList(defaultVersion, false);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty set when no domain rules exist")
    void shouldReturnEmptySetWhenNoDomainRulesExist() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmDomainList(explicitVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmDomainRule> result = mrcmRuleService.getMrcmDomainRules(explicitVersion, false);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return multiple domain rules when multiple domains exist")
    void shouldReturnMultipleDomainRules() {
      // GIVEN
      MrcmDomain domain1 =
          mrcmDomain(
              "d1", CONCEPT_CLINICAL_FINDING, "<< 404684003 |Clinical finding|", EFFECTIVE_TIME_V1);
      MrcmDomain domain2 =
          mrcmDomain("d2", CONCEPT_PROCEDURE, "<< 71388002 |Procedure|", EFFECTIVE_TIME_V1);
      when(snomedInMemoryRepresentation.getMrcmDomainList(explicitVersion, false))
          .thenReturn(List.of(domain1, domain2));

      // WHEN
      Set<MrcmDomainRule> result = mrcmRuleService.getMrcmDomainRules(explicitVersion, false);

      // THEN
      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(MrcmDomainRule::getReferencedComponentId)
          .containsExactlyInAnyOrder(CONCEPT_CLINICAL_FINDING, CONCEPT_PROCEDURE);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getMrcmAttributeDomainRules
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getMrcmAttributeDomainRules")
  class GetMrcmAttributeDomainRules {

    @Test
    @DisplayName("should return only active attribute domain rules")
    void shouldReturnOnlyActiveAttributeDomainRules() {
      // GIVEN
      MrcmAttributeDomain activeAttr =
          mrcmAttributeDomain(
              "ad1", ATTRIBUTE_FINDING_SITE, CONCEPT_CLINICAL_FINDING, EFFECTIVE_TIME_V1);
      MrcmAttributeDomain inactiveAttr =
          MrcmAttributeDomain.builder()
              .id("ad2")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(false)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
              .referencedComponentId(ATTRIBUTE_CAUSATIVE_AGENT)
              .domainId(CONCEPT_CLINICAL_FINDING)
              .grouped(false)
              .attributeCardinality("0..*")
              .attributeInGroupCardinality("0..1")
              .ruleStrengthId("723597001")
              .contentTypeId("723596005")
              .build();
      when(snomedInMemoryRepresentation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(List.of(activeAttr, inactiveAttr));

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          mrcmRuleService.getMrcmAttributeDomainRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN
      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getActive()).isEqualTo("1");
      assertThat(result.iterator().next().getReferencedComponentId())
          .isEqualTo(ATTRIBUTE_FINDING_SITE);
    }

    @Test
    @DisplayName("should fall back to default version when version is null")
    void shouldFallBackToDefaultVersionWhenNull() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE), defaultVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          mrcmRuleService.getMrcmAttributeDomainRules(null, ATTRIBUTE_FINDING_SITE);

      // THEN
      verify(snomedInMemoryRepresentation)
          .getMrcmAttributeDomainList(Set.of(ATTRIBUTE_FINDING_SITE), defaultVersion, false);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty set when no attribute domain rules exist")
    void shouldReturnEmptySetWhenNoAttributeDomainRulesExist() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          mrcmRuleService.getMrcmAttributeDomainRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should deduplicate rules by identifier")
    void shouldDeduplicateRulesById() {
      // GIVEN – two entries with the same id but different effective times
      MrcmAttributeDomain olderEntry =
          MrcmAttributeDomain.builder()
              .id("ad1")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(true)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
              .referencedComponentId(ATTRIBUTE_FINDING_SITE)
              .domainId(CONCEPT_CLINICAL_FINDING)
              .grouped(false)
              .attributeCardinality("0..*")
              .attributeInGroupCardinality("0..1")
              .ruleStrengthId("723597001")
              .contentTypeId("723596005")
              .build();
      MrcmAttributeDomain newerEntry =
          MrcmAttributeDomain.builder()
              .id("ad1")
              .effectiveTime(EFFECTIVE_TIME_V2)
              .active(true)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
              .referencedComponentId(ATTRIBUTE_FINDING_SITE)
              .domainId(CONCEPT_CLINICAL_FINDING)
              .grouped(true)
              .attributeCardinality("1..*")
              .attributeInGroupCardinality("0..1")
              .ruleStrengthId("723597001")
              .contentTypeId("723596005")
              .build();
      when(snomedInMemoryRepresentation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(List.of(olderEntry, newerEntry));

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          mrcmRuleService.getMrcmAttributeDomainRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN – the map deduplication keeps only the last entry (newerEntry)
      assertThat(result).hasSize(1);
      MrcmAttributeDomainRule rule = result.iterator().next();
      assertThat(rule.getAttributeCardinality()).isEqualTo("1..*");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // getMrcmAttributeRangeRules
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getMrcmAttributeRangeRules")
  class GetMrcmAttributeRangeRules {

    @Test
    @DisplayName("should return mapped attribute range rules for the given version")
    void shouldReturnMappedAttributeRangeRules() {
      // GIVEN
      MrcmAttributeRange range =
          mrcmAttributeRange(
              "ar1",
              ATTRIBUTE_FINDING_SITE,
              "<< 442083009 |Anatomical or acquired body structure|",
              EFFECTIVE_TIME_V1);
      when(snomedInMemoryRepresentation.getMrcmAttributeRangeList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(List.of(range));

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          mrcmRuleService.getMrcmAttributeRangeRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN
      assertThat(result).hasSize(1);
      MrcmAttributeRangeRule rule = result.iterator().next();
      assertThat(rule.getReferencedComponentId()).isEqualTo(ATTRIBUTE_FINDING_SITE);
      assertThat(rule.getRangeConstraint())
          .isEqualTo("<< 442083009 |Anatomical or acquired body structure|");
    }

    @Test
    @DisplayName("should fall back to default version when version is null")
    void shouldFallBackToDefaultVersionWhenNull() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmAttributeRangeList(
              Set.of(ATTRIBUTE_FINDING_SITE), defaultVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          mrcmRuleService.getMrcmAttributeRangeRules(null, ATTRIBUTE_FINDING_SITE);

      // THEN
      verify(snomedInMemoryRepresentation)
          .getMrcmAttributeRangeList(Set.of(ATTRIBUTE_FINDING_SITE), defaultVersion, false);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty set when no attribute range rules exist")
    void shouldReturnEmptySetWhenNoAttributeRangeRulesExist() {
      // GIVEN
      when(snomedInMemoryRepresentation.getMrcmAttributeRangeList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(Collections.emptyList());

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          mrcmRuleService.getMrcmAttributeRangeRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should deduplicate range rules by identifier keeping the latest entry")
    void shouldDeduplicateRangeRulesById() {
      // GIVEN – two entries with the same id, newer entry overwrites older
      MrcmAttributeRange olderEntry =
          mrcmAttributeRange(
              "ar1", ATTRIBUTE_FINDING_SITE, "<< 442083009 |old constraint|", EFFECTIVE_TIME_V1);
      MrcmAttributeRange newerEntry =
          mrcmAttributeRange(
              "ar1", ATTRIBUTE_FINDING_SITE, "<< 442083009 |new constraint|", EFFECTIVE_TIME_V2);
      when(snomedInMemoryRepresentation.getMrcmAttributeRangeList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(List.of(olderEntry, newerEntry));

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          mrcmRuleService.getMrcmAttributeRangeRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN – the map keeps only the newer entry
      assertThat(result).hasSize(1);
      MrcmAttributeRangeRule rule = result.iterator().next();
      assertThat(rule.getRangeConstraint()).isEqualTo("<< 442083009 |new constraint|");
    }

    @Test
    @DisplayName("should include inactive rules in the result (filtering happens elsewhere)")
    void shouldIncludeInactiveRules() {
      // GIVEN
      MrcmAttributeRange inactiveRange =
          MrcmAttributeRange.builder()
              .id("ar1")
              .effectiveTime(EFFECTIVE_TIME_V1)
              .active(false)
              .moduleId(MODULE_CORE)
              .refsetId(MRCM_ATTRIBUTE_RANGE_REFSET_ID)
              .referencedComponentId(ATTRIBUTE_FINDING_SITE)
              .rangeConstraint("<< 442083009")
              .attributeRule("")
              .ruleStrengthId("723597001")
              .contentTypeId("723596005")
              .build();
      when(snomedInMemoryRepresentation.getMrcmAttributeRangeList(
              Set.of(ATTRIBUTE_FINDING_SITE), explicitVersion, false))
          .thenReturn(List.of(inactiveRange));

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          mrcmRuleService.getMrcmAttributeRangeRules(explicitVersion, ATTRIBUTE_FINDING_SITE);

      // THEN – inactive rules are included (as per the comment in the original code)
      assertThat(result).hasSize(1);
      assertThat(result.iterator().next().getActive()).isEqualTo("0");
    }
  }
}
