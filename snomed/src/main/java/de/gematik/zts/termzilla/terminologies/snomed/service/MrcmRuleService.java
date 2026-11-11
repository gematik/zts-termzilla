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

import de.gematik.zts.termzilla.terminologies.snomed.mapping.SnomedMapper;
import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeDomainRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmAttributeRangeRule;
import de.gematik.zts.termzilla.terminologies.snomed.scg.MrcmDomainRule;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving MRCM (Machine Readable Concept Model) rules from the in-memory
 * representation and mapping them to their domain model equivalents.
 *
 * <p>This service encapsulates all MRCM-related query logic that was previously part of {@code
 * Snomed.java}, providing access to domain rules, attribute domain rules, and attribute range
 * rules.
 */
@Slf4j
public class MrcmRuleService {

  private final SnomedInMemoryRepresentation snomedInMemoryRepresentation;
  private final SnomedReleaseVersion defaultSnomedReleaseVersion;

  /**
   * Creates a new {@link MrcmRuleService}.
   *
   * @param snomedInMemoryRepresentation the in-memory representation to query MRCM data from
   * @param defaultSnomedReleaseVersion the default release version used when no version is
   *     specified
   */
  public MrcmRuleService(
      SnomedInMemoryRepresentation snomedInMemoryRepresentation,
      SnomedReleaseVersion defaultSnomedReleaseVersion) {
    this.snomedInMemoryRepresentation = snomedInMemoryRepresentation;
    this.defaultSnomedReleaseVersion = defaultSnomedReleaseVersion;
  }

  /**
   * Retrieves all MRCM domain rules for the given release version.
   *
   * @param version the release version to query; falls back to the default version if {@code null}
   * @param restrictToExactVersion whether to restrict results to the exact version
   * @return a set of mapped {@link MrcmDomainRule} instances
   */
  public Set<MrcmDomainRule> getMrcmDomainRules(
      SnomedReleaseVersion version, boolean restrictToExactVersion) {

    log.debug(
        "Getting Mrcm Domain Rules for version {} (restrictToExactVersion: {})",
        version != null ? version.getFhirVersion() : "null",
        restrictToExactVersion);

    Set<MrcmDomainRule> mrcmDomainRules = new HashSet<>();

    if (version == null) {
      version = defaultSnomedReleaseVersion;
    }

    List<MrcmDomain> domainList = snomedInMemoryRepresentation.getMrcmDomainList(version, false);

    for (MrcmDomain domain : domainList) {
      mrcmDomainRules.add(SnomedMapper.toMrcmDomainRule(domain));
    }

    return new HashSet<>(mrcmDomainRules);
  }

  /**
   * Retrieves all MRCM attribute domain rules for the given attribute and release version.
   *
   * <p>Only active rules (with active flag {@code "1"}) are included in the result.
   *
   * @param version the release version to query; falls back to the default version if {@code null}
   * @param attribute the attribute concept identifier to query rules for
   * @return a set of active {@link MrcmAttributeDomainRule} instances
   */
  public Set<MrcmAttributeDomainRule> getMrcmAttributeDomainRules(
      SnomedReleaseVersion version, String attribute) {

    Map<String, MrcmAttributeDomainRule> rules = new HashMap<>();

    if (version == null) {
      version = defaultSnomedReleaseVersion;
    }

    List<MrcmAttributeDomain> attributeDomainRuleList =
        snomedInMemoryRepresentation.getMrcmAttributeDomainList(Set.of(attribute), version, false);

    for (MrcmAttributeDomain attributeDomainRule : attributeDomainRuleList) {
      rules.put(
          attributeDomainRule.getId(), SnomedMapper.toMrcmAttributeDomainRule(attributeDomainRule));
    }

    Set<MrcmAttributeDomainRule> resultSet = new HashSet<>(rules.values());
    return resultSet.stream()
        .filter(rule -> rule.getActive().equals("1"))
        .collect(Collectors.toSet());
  }

  /**
   * Retrieves all MRCM attribute range rules for the given attribute and release version.
   *
   * <p>Results are de-duplicated by entry identifier, with later entries in the source list
   * overwriting earlier ones (ensuring the most recent version of each rule is kept).
   *
   * @param releaseVersion the release version to query; falls back to the default version if {@code
   *     null}
   * @param attributeId the attribute concept identifier to query rules for
   * @return a set of {@link MrcmAttributeRangeRule} instances
   */
  public Set<MrcmAttributeRangeRule> getMrcmAttributeRangeRules(
      SnomedReleaseVersion releaseVersion, String attributeId) {

    // Setzen einer Default-ReleaseVersion, falls keine angegeben wurde
    if (releaseVersion == null) {
      releaseVersion = defaultSnomedReleaseVersion;
    }

    // Anfrage an das In-Memory-Modell stellen
    List<MrcmAttributeRange> attributeRangeList =
        snomedInMemoryRepresentation.getMrcmAttributeRangeList(
            Set.of(attributeId), releaseVersion, false);

    // Vorbereiten einer Map, die die Ergebnisse aufnehmen soll. Wir verwenden hier eine Map, um
    // doppelte Einträge zu vermeiden. Durch die Reihenfolge der Einträge in der Ergebnisliste
    // können wir sicherstellen, dass die aktuellste Version der Regel immer am Ende in die Map
    // eingefügt wird und somit ältere Einträge überschreibt.
    Map<String, MrcmAttributeRangeRule> ruleMap = new HashMap<>();
    for (MrcmAttributeRange attributeRange : attributeRangeList) {
      ruleMap.put(attributeRange.getId(), SnomedMapper.toMrcmAttributeRangeRule(attributeRange));
    }

    // Übernahme der Ergebnisse in eine Set-Struktur.
    // Hinweis: Inaktive Regeln filtern wir später heraus
    return new HashSet<>(ruleMap.values());
  }
}
