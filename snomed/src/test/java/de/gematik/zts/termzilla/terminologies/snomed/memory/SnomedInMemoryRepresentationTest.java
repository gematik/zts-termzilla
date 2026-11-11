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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SnomedInMemoryRepresentation} query and filtering behavior. */
@DisplayName("SnomedInMemoryRepresentation Tests")
class SnomedInMemoryRepresentationTest {

  private static final String ATTRIBUTE_FINDING_SITE = "363698007";
  private static final String ATTRIBUTE_ASSOCIATED_MORPHOLOGY = "116676008";
  private static final String DOMAIN_CLINICAL_FINDING = SnomedTestData.CONCEPT_CLINICAL_FINDING;
  private static final String DOMAIN_DISORDER = SnomedTestData.CONCEPT_DISORDER;

  private SnomedInMemoryRepresentation representation;
  private SnomedReleaseVersion coreReleaseV2;

  @BeforeEach
  void setUp() {
    representation = new SnomedInMemoryRepresentation();
    coreReleaseV2 = SnomedTestData.coreVersion(SnomedTestData.DATE_V2);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize empty collections when using package-private constructor")
    void shouldInitializeEmptyCollections() {
      // GIVEN
      SnomedInMemoryRepresentation emptyRepresentation = new SnomedInMemoryRepresentation();

      // WHEN
      Set<String> refSetIds = emptyRepresentation.getRefSetIdList();

      // THEN
      assertThat(refSetIds).isEmpty();
      assertThat(emptyRepresentation.getConceptIdToConceptMap().keySet()).isEmpty();
      assertThat(emptyRepresentation.getDescriptionIdToDescriptionMap().keySet()).isEmpty();
      assertThat(emptyRepresentation.getDescriptionIdToLanguageMap().keySet()).isEmpty();
      assertThat(emptyRepresentation.getAttributeIdToMRCMAttributeDomainMap().keySet()).isEmpty();
    }
  }

  @Nested
  @DisplayName("getConceptList() Tests")
  class GetConceptListTests {

    @Test
    @DisplayName(
        "Should keep latest valid concept per id and exclude inactive concepts when requested")
    void shouldKeepLatestValidConceptPerIdAndExcludeInactive() {
      // GIVEN
      Concept conceptAOld =
          SnomedTestData.concept(
              "100001", SnomedTestData.EFFECTIVE_TIME_V1, true, SnomedTestData.MODULE_CORE);
      Concept conceptANewerInactive =
          SnomedTestData.concept(
              "100001", SnomedTestData.EFFECTIVE_TIME_V2, false, SnomedTestData.MODULE_CORE);
      Concept conceptAFuture =
          SnomedTestData.concept(
              "100001", SnomedTestData.EFFECTIVE_TIME_V3, true, SnomedTestData.MODULE_CORE);
      Concept conceptBActive =
          SnomedTestData.concept(
              "100002", SnomedTestData.EFFECTIVE_TIME_V1, true, SnomedTestData.MODULE_CORE);
      Concept conceptFromDifferentModule =
          SnomedTestData.concept(
              "100003", SnomedTestData.EFFECTIVE_TIME_V1, true, SnomedTestData.MODULE_GERMAN);

      representation.addConcept(conceptAOld);
      representation.addConcept(conceptANewerInactive);
      representation.addConcept(conceptAFuture);
      representation.addConcept(conceptBActive);
      representation.addConcept(conceptFromDifferentModule);

      // WHEN
      List<Concept> concepts = representation.getConceptList(coreReleaseV2, false);

      // THEN
      assertThat(concepts)
          .extracting(Concept::getId, Concept::isActive, Concept::getEffectiveTime)
          .containsExactly(tuple("100002", true, SnomedTestData.EFFECTIVE_TIME_V1));
    }

    @Test
    @DisplayName("Should include inactive latest concept versions when includeInactive is true")
    void shouldIncludeInactiveLatestConceptVersionsWhenRequested() {
      // GIVEN
      Concept inactiveLatestConcept =
          SnomedTestData.concept(
              "100004", SnomedTestData.EFFECTIVE_TIME_V2, false, SnomedTestData.MODULE_CORE);
      Concept activeConcept =
          SnomedTestData.concept(
              "100005", SnomedTestData.EFFECTIVE_TIME_V1, true, SnomedTestData.MODULE_CORE);
      representation.addConcept(inactiveLatestConcept);
      representation.addConcept(activeConcept);

      // WHEN
      List<Concept> concepts = representation.getConceptList(coreReleaseV2, true);

      // THEN
      assertThat(concepts)
          .extracting(Concept::getId, Concept::isActive)
          .containsExactly(tuple("100004", false), tuple("100005", true));
    }
  }

  @Nested
  @DisplayName("getDescriptionListByDescriptionIdSet() Tests")
  class GetDescriptionListByDescriptionIdSetTests {

    @Test
    @DisplayName("Should keep latest valid description per id and ignore unknown ids")
    void shouldKeepLatestValidDescriptionPerIdAndIgnoreUnknownIds() {
      // GIVEN
      Description descriptionOld =
          SnomedTestData.description(
              "200001",
              SnomedTestData.CONCEPT_DIABETES,
              "en",
              SnomedTestData.TYPE_SYNONYM,
              "Diabetes mellitus",
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);
      Description descriptionNewInactive =
          SnomedTestData.description(
              "200001",
              SnomedTestData.CONCEPT_DIABETES,
              "en",
              SnomedTestData.TYPE_SYNONYM,
              "Diabetes mellitus inactive",
              SnomedTestData.EFFECTIVE_TIME_V2,
              false,
              SnomedTestData.MODULE_CORE);
      Description descriptionFuture =
          SnomedTestData.description(
              "200001",
              SnomedTestData.CONCEPT_DIABETES,
              "en",
              SnomedTestData.TYPE_SYNONYM,
              "Diabetes mellitus future",
              SnomedTestData.EFFECTIVE_TIME_V3,
              true,
              SnomedTestData.MODULE_CORE);
      Description otherDescription =
          SnomedTestData.description(
              "200002",
              SnomedTestData.CONCEPT_HYPERTENSION,
              "en",
              SnomedTestData.TYPE_FSN,
              "Hypertension (disorder)",
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);
      Description differentModuleDescription =
          SnomedTestData.description(
              "200003",
              SnomedTestData.CONCEPT_HYPERTENSION,
              "de",
              SnomedTestData.TYPE_SYNONYM,
              "Hypertonie",
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_GERMAN);

      representation.addDescription(descriptionOld);
      representation.addDescription(descriptionNewInactive);
      representation.addDescription(descriptionFuture);
      representation.addDescription(otherDescription);
      representation.addDescription(differentModuleDescription);

      Set<String> descriptionIds = Set.of("200001", "200002", "200003", "unknown-id");

      // WHEN
      List<Description> descriptions =
          representation.getDescriptionListByDescriptionIdSet(descriptionIds, coreReleaseV2, false);

      // THEN
      assertThat(descriptions)
          .extracting(Description::getId, Description::getTerm, Description::isActive)
          .containsExactlyInAnyOrder(tuple("200002", "Hypertension (disorder)", true));
    }

    @Test
    @DisplayName("Should include inactive latest descriptions when includeInactive is true")
    void shouldIncludeInactiveLatestDescriptionsWhenRequested() {
      // GIVEN
      Description inactiveLatest =
          SnomedTestData.description(
              "200004",
              SnomedTestData.CONCEPT_DIABETES,
              "en",
              SnomedTestData.TYPE_SYNONYM,
              "Inactive latest",
              SnomedTestData.EFFECTIVE_TIME_V2,
              false,
              SnomedTestData.MODULE_CORE);
      Description activeDescription =
          SnomedTestData.description(
              "200005",
              SnomedTestData.CONCEPT_HYPERTENSION,
              "en",
              SnomedTestData.TYPE_SYNONYM,
              "Active description",
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);

      representation.addDescription(inactiveLatest);
      representation.addDescription(activeDescription);

      // WHEN
      List<Description> descriptions =
          representation.getDescriptionListByDescriptionIdSet(
              Set.of("200004", "200005"), coreReleaseV2, true);

      // THEN
      assertThat(descriptions)
          .extracting(Description::getId, Description::isActive)
          .containsExactlyInAnyOrder(tuple("200004", false), tuple("200005", true));
    }
  }

  @Nested
  @DisplayName("getRefSetIdList() Tests")
  class GetRefSetIdListTests {

    @Test
    @DisplayName("Should return empty list when no refset entries exist")
    void shouldReturnEmptyListWhenNoRefSetEntriesExist() {
      // GIVEN
      SnomedInMemoryRepresentation emptyRepresentation = new SnomedInMemoryRepresentation();

      // WHEN
      Set<String> refSetIds = emptyRepresentation.getRefSetIdList();

      // THEN
      assertThat(refSetIds).isEmpty();
    }

    @Test
    @DisplayName("Should return unique refset ids from both language and refset entries")
    void shouldReturnUniqueRefSetIdsFromMixedEntries() {
      // GIVEN
      RefSet simpleRefSet = refSet("300001", SnomedTestData.REFSET_EN_GB, "73211009");
      RefSet additionalRefSet = refSet("300002", "733073007", "38341003");
      Language language =
          SnomedTestData.language(
              "300003",
              "200100",
              SnomedTestData.REFSET_EN_US,
              SnomedTestData.ACCEPTABILITY_PREFERRED,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);

      representation.addRefSet(simpleRefSet);
      representation.addRefSet(additionalRefSet);
      representation.addLanguage(language);

      // WHEN
      Set<String> refSetIds = representation.getRefSetIdList();

      // THEN
      assertThat(refSetIds)
          .containsExactlyInAnyOrder(
              SnomedTestData.REFSET_EN_GB, "733073007", SnomedTestData.REFSET_EN_US);
    }
  }

  @Nested
  @DisplayName("getMrcmAttributeDomainList() Tests")
  class GetMrcmAttributeDomainListTests {

    @Test
    @DisplayName(
        "Should keep latest valid domain per id and exclude inactive entries when requested")
    void shouldKeepLatestValidDomainPerIdAndExcludeInactive() {
      // GIVEN
      MrcmAttributeDomain domainOld =
          mrcmAttributeDomain(
              "400001",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);
      MrcmAttributeDomain domainNewInactive =
          mrcmAttributeDomain(
              "400001",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V2,
              false,
              SnomedTestData.MODULE_CORE);
      MrcmAttributeDomain domainFuture =
          mrcmAttributeDomain(
              "400001",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V3,
              true,
              SnomedTestData.MODULE_CORE);
      MrcmAttributeDomain secondDomain =
          mrcmAttributeDomain(
              "400002",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_DISORDER,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);
      MrcmAttributeDomain differentModule =
          mrcmAttributeDomain(
              "400003",
              ATTRIBUTE_ASSOCIATED_MORPHOLOGY,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_GERMAN);

      representation.addMRCMAttributeDomain(domainOld);
      representation.addMRCMAttributeDomain(domainNewInactive);
      representation.addMRCMAttributeDomain(domainFuture);
      representation.addMRCMAttributeDomain(secondDomain);
      representation.addMRCMAttributeDomain(differentModule);

      // WHEN
      List<MrcmAttributeDomain> domains =
          representation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE, ATTRIBUTE_ASSOCIATED_MORPHOLOGY),
              coreReleaseV2,
              false);

      // THEN
      assertThat(domains)
          .extracting(
              MrcmAttributeDomain::getId,
              MrcmAttributeDomain::getReferencedComponentId,
              MrcmAttributeDomain::isActive)
          .containsExactlyInAnyOrder(tuple("400002", ATTRIBUTE_FINDING_SITE, true));
    }

    @Test
    @DisplayName("Should include inactive latest entries when includeInactive is true")
    void shouldIncludeInactiveLatestEntriesWhenRequested() {
      // GIVEN
      MrcmAttributeDomain inactiveLatest =
          mrcmAttributeDomain(
              "400004",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V2,
              false,
              SnomedTestData.MODULE_CORE);
      MrcmAttributeDomain activeEntry =
          mrcmAttributeDomain(
              "400005",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_DISORDER,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);

      representation.addMRCMAttributeDomain(inactiveLatest);
      representation.addMRCMAttributeDomain(activeEntry);

      // WHEN
      List<MrcmAttributeDomain> domains =
          representation.getMrcmAttributeDomainList(
              Set.of(ATTRIBUTE_FINDING_SITE), coreReleaseV2, true);

      // THEN
      assertThat(domains)
          .extracting(MrcmAttributeDomain::getId, MrcmAttributeDomain::isActive)
          .containsExactlyInAnyOrder(tuple("400004", false), tuple("400005", true));
    }

    @Test
    @DisplayName("Should return empty list when attribute set does not match any entries")
    void shouldReturnEmptyListWhenAttributeSetDoesNotMatch() {
      // GIVEN
      MrcmAttributeDomain existingDomain =
          mrcmAttributeDomain(
              "400006",
              ATTRIBUTE_FINDING_SITE,
              DOMAIN_CLINICAL_FINDING,
              SnomedTestData.EFFECTIVE_TIME_V1,
              true,
              SnomedTestData.MODULE_CORE);
      representation.addMRCMAttributeDomain(existingDomain);

      // WHEN
      List<MrcmAttributeDomain> domains =
          representation.getMrcmAttributeDomainList(
              Set.of("not-existing-attribute"), coreReleaseV2, true);

      // THEN
      assertThat(domains).isEmpty();
    }
  }

  private static RefSet refSet(String id, String refSetId, String referencedComponentId) {
    return RefSet.builder()
        .id(id)
        .effectiveTime(SnomedTestData.EFFECTIVE_TIME_V1)
        .active(true)
        .moduleId(SnomedTestData.MODULE_CORE)
        .refsetId(refSetId)
        .referencedComponentId(referencedComponentId)
        .build();
  }

  private static MrcmAttributeDomain mrcmAttributeDomain(
      String id,
      String referencedComponentId,
      String domainId,
      long effectiveTime,
      boolean active,
      String moduleId) {
    return MrcmAttributeDomain.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(active)
        .moduleId(moduleId)
        .refsetId(SnomedTestData.MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
        .referencedComponentId(referencedComponentId)
        .domainId(domainId)
        .grouped(false)
        .attributeCardinality("0..*")
        .attributeInGroupCardinality("0..1")
        .ruleStrengthId("723597001")
        .contentTypeId("723596005")
        .build();
  }
}
