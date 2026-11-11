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

import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.DisplayTermSelector;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeRangeRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralises the conversion from RF2 in-memory models to application domain models.
 *
 * <p>The RF2 models ({@link Concept}, {@link Description}, {@link Language}, {@link MrcmDomain},
 * {@link MrcmAttributeDomain}, {@link MrcmAttributeRange}) differ from the domain models ({@link
 * SnomedConcept}, {@link SnomedConceptDescription}, {@link SnomedLanguage}, {@link MrcmDomainRule},
 * {@link MrcmAttributeDomainRule}, {@link MrcmAttributeRangeRule}) in several ways: {@code
 * effectiveTime} is {@code long} vs. {@code String}, {@code active} is {@code boolean} vs. {@code
 * String} (MRCM rules), and field names differ (e.g. {@code refsetId} vs. {@code refsetIdField}).
 *
 * <p>All methods are stateless and static, consistent with {@link
 * DisplayTermSelector}.
 */
public final class SnomedMapper {

  private SnomedMapper() {}

  // ──────────────────────────────────────────────────────────────────────────
  // Concept / Description / Language mapping
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Maps an RF2 {@link Concept} to a {@link SnomedConcept} without descriptions.
   *
   * <p>Descriptions should be added separately via {@link #toSnomedConceptDescription} to allow
   * callers to control filtering (active/inactive) and language attachment independently.
   *
   * @param concept the RF2 concept
   * @return a new {@link SnomedConcept} with all scalar fields mapped
   */
  public static SnomedConcept toSnomedConcept(Concept concept) {
    SnomedConcept result = new SnomedConcept();
    result.setId(concept.getId());
    result.setEffectiveTime(effectiveTimeToString(concept.getEffectiveTime()));
    result.setActive(concept.isActive());
    result.setModuleId(concept.getModuleId());
    result.setDefinitionStatusId(concept.getDefinitionStatusId());
    return result;
  }

  /**
   * Maps an RF2 {@link Description} to a {@link SnomedConceptDescription}.
   *
   * <p>The resulting description has an empty language list. Use {@link #toSnomedLanguage} to
   * convert and attach language entries separately.
   *
   * @param description the RF2 description
   * @return a new {@link SnomedConceptDescription} with all scalar fields mapped
   */
  public static SnomedConceptDescription toSnomedConceptDescription(Description description) {
    SnomedConceptDescription result = new SnomedConceptDescription();
    result.setId(description.getId());
    result.setEffectiveTime(effectiveTimeToString(description.getEffectiveTime()));
    result.setActive(description.isActive());
    result.setModuleId(description.getModuleId());
    result.setConceptId(description.getConceptId());
    result.setLanguageCode(description.getLanguageCode());
    result.setTypeId(description.getTypeId());
    result.setTerm(description.getTerm());
    result.setCaseSignificanceId(description.getCaseSignificanceId());
    result.setLanguageList(new ArrayList<>());
    return result;
  }

  /**
   * Maps an RF2 {@link Language} entry to a {@link SnomedLanguage}.
   *
   * @param language the RF2 language reference set entry
   * @return a new {@link SnomedLanguage} with all fields mapped
   */
  public static SnomedLanguage toSnomedLanguage(Language language) {
    SnomedLanguage result = new SnomedLanguage();
    result.setId(language.getId());
    result.setEffectiveTime(effectiveTimeToString(language.getEffectiveTime()));
    result.setActive(language.isActive());
    result.setModuleId(language.getModuleId());
    result.setRefsetId(language.getRefsetId());
    result.setReferencedComponentId(language.getReferencedComponentId());
    result.setAcceptabilityId(language.getAcceptabilityId());
    return result;
  }

  /**
   * Maps a list of RF2 {@link Language} entries to a list of {@link SnomedLanguage}.
   *
   * @param languages the RF2 language reference set entries
   * @return a new list of mapped {@link SnomedLanguage} instances
   */
  public static List<SnomedLanguage> toSnomedLanguageList(List<Language> languages) {
    List<SnomedLanguage> result = new ArrayList<>(languages.size());
    for (Language language : languages) {
      result.add(toSnomedLanguage(language));
    }
    return result;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // MRCM rule mapping
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Maps an RF2 {@link MrcmDomain} to a {@link MrcmDomainRule}.
   *
   * <p>Note: {@link MrcmDomainRule} inherits {@code ruleStrengthId} and {@code contentTypeId} from
   * {@link MrcmRule}, but {@link MrcmDomain} does not have these fields. They are left at their
   * default ({@code null}).
   *
   * @param domain the RF2 MRCM domain entry
   * @return a new {@link MrcmDomainRule} with all applicable fields mapped
   */
  public static MrcmDomainRule toMrcmDomainRule(MrcmDomain domain) {
    MrcmDomainRule rule = new MrcmDomainRule();
    mapMrcmRuleBase(
        rule,
        domain.getId(),
        domain.getEffectiveTime(),
        domain.isActive(),
        domain.getModuleId(),
        domain.getRefsetId(),
        domain.getReferencedComponentId());
    rule.setDomainConstraint(domain.getDomainConstraint());
    rule.setParentDomain(domain.getParentDomain());
    rule.setProximalPrimitiveConstraint(domain.getProximalPrimitiveConstraint());
    rule.setProximalPrimitiveRefinement(domain.getProximalPrimitiveRefinement());
    rule.setDomainTemplateForPrecoordination(domain.getDomainTemplateForPrecoordination());
    rule.setDomainTemplateForPostcoordination(domain.getDomainTemplateForPostcoordination());
    rule.setGuideURL(domain.getGuideURL());
    return rule;
  }

  /**
   * Maps an RF2 {@link MrcmAttributeDomain} to a {@link MrcmAttributeDomainRule}.
   *
   * @param attributeDomain the RF2 MRCM attribute domain entry
   * @return a new {@link MrcmAttributeDomainRule} with all fields mapped
   */
  public static MrcmAttributeDomainRule toMrcmAttributeDomainRule(
      MrcmAttributeDomain attributeDomain) {
    MrcmAttributeDomainRule rule = new MrcmAttributeDomainRule();
    mapMrcmRuleBase(
        rule,
        attributeDomain.getId(),
        attributeDomain.getEffectiveTime(),
        attributeDomain.isActive(),
        attributeDomain.getModuleId(),
        attributeDomain.getRefsetId(),
        attributeDomain.getReferencedComponentId());
    rule.setRuleStrengthId(attributeDomain.getRuleStrengthId());
    rule.setContentTypeId(attributeDomain.getContentTypeId());
    rule.setDomainId(attributeDomain.getDomainId());
    rule.setGrouped(booleanToActiveString(attributeDomain.isGrouped()));
    rule.setAttributeCardinality(attributeDomain.getAttributeCardinality());
    rule.setAttributeInGroupCardinality(attributeDomain.getAttributeInGroupCardinality());
    return rule;
  }

  /**
   * Maps an RF2 {@link MrcmAttributeRange} to a {@link MrcmAttributeRangeRule}.
   *
   * @param attributeRange the RF2 MRCM attribute range entry
   * @return a new {@link MrcmAttributeRangeRule} with all fields mapped
   */
  public static MrcmAttributeRangeRule toMrcmAttributeRangeRule(MrcmAttributeRange attributeRange) {
    MrcmAttributeRangeRule rule = new MrcmAttributeRangeRule();
    mapMrcmRuleBase(
        rule,
        attributeRange.getId(),
        attributeRange.getEffectiveTime(),
        attributeRange.isActive(),
        attributeRange.getModuleId(),
        attributeRange.getRefsetId(),
        attributeRange.getReferencedComponentId());
    rule.setRuleStrengthId(attributeRange.getRuleStrengthId());
    rule.setContentTypeId(attributeRange.getContentTypeId());
    rule.setRangeConstraint(attributeRange.getRangeConstraint());
    rule.setAttributeRule(attributeRange.getAttributeRule());
    return rule;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ──────────────────────────────────────────────────────────────────────────

  /** Maps the common fields shared by all {@link MrcmRule} subclasses. */
  private static void mapMrcmRuleBase(
      MrcmRule rule,
      String id,
      long effectiveTime,
      boolean active,
      String moduleId,
      String refsetId,
      String referencedComponentId) {
    rule.setId(id);
    rule.setEffectiveTime(effectiveTimeToString(effectiveTime));
    rule.setActive(booleanToActiveString(active));
    rule.setModuleId(moduleId);
    rule.setRefsetIdField(refsetId);
    rule.setReferencedComponentId(referencedComponentId);
  }

  /**
   * Converts a {@code boolean} active flag to the string representation used in MRCM domain models:
   * {@code "1"} for {@code true}, {@code "0"} for {@code false}.
   */
  static String booleanToActiveString(boolean value) {
    return value ? "1" : "0";
  }

  /** Converts an RF2 {@code long} effective time to its {@link String} representation. */
  static String effectiveTimeToString(long effectiveTime) {
    return String.valueOf(effectiveTime);
  }
}
