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
package de.gematik.zts.termzilla.testing.data;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.UriType;

public class ConceptMapSearchTestResourceGenerator {

  // ==================================================================================================================
  // Constants
  // ==================================================================================================================

  public static final String DEFAULT_CS_URL_PREFIX = "http://example.org/fhir/CodeSystem/";
  public static final String DEFAULT_VS_URL_PREFIX = "http://example.org/fhir/ValueSet/";
  public static final String DEFAULT_CM_URL_PREFIX = "http://example.org/fhir/ConceptMap/";

  // ==================================================================================================================
  // Helper Methods
  // ==================================================================================================================

  private static ConceptMap createConceptMapBase(String id, String version) {
    ConceptMap cm = new ConceptMap();
    cm.setId(id + "-" + version);
    cm.setUrl(DEFAULT_CM_URL_PREFIX + id);
    cm.setVersion(version);
    cm.setName(id + "_Name");
    cm.setTitle(id + " Title");
    cm.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cm.setExperimental(false);
    cm.setDateElement(new DateTimeType("2024-01-01"));
    cm.setDescription(id + " Description");
    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - sourceUri / targetUri variations
  // ==================================================================================================================

  // CM01: sourceUri/targetUri
  public static ConceptMap createConceptMap_CM01() {
    ConceptMap cm = createConceptMapBase("CM01", "2025");
    cm.setSource(new UriType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new UriType(DEFAULT_VS_URL_PREFIX + "VS02"));
    return cm;
  }

  // CM02: sourceUri only (no targetUri)
  public static ConceptMap createConceptMap_CM02() {
    ConceptMap cm = createConceptMapBase("CM02", "2025");
    cm.setSource(new UriType(DEFAULT_VS_URL_PREFIX + "VS01"));
    return cm;
  }

  // CM03: targetUri only (no sourceUri)
  public static ConceptMap createConceptMap_CM03() {
    ConceptMap cm = createConceptMapBase("CM03", "2025");
    cm.setTarget(new UriType(DEFAULT_VS_URL_PREFIX + "VS02"));
    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - sourceCanonical / targetCanonical variations (WITHOUT version in URI)
  // ==================================================================================================================

  // CM04: sourceCanonical / targetCanonical (ValueSet) without version in URI
  public static ConceptMap createConceptMap_CM04() {
    ConceptMap cm = createConceptMapBase("CM04", "2026");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS02"));
    return cm;
  }

  // CM05: sourceCanonical (ValueSet) only without version in URI
  public static ConceptMap createConceptMap_CM05() {
    ConceptMap cm = createConceptMapBase("CM05", "2026");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS01"));
    return cm;
  }

  // CM06: targetCanonical (ValueSet) only without version in URI
  public static ConceptMap createConceptMap_CM06() {
    ConceptMap cm = createConceptMapBase("CM06", "2026");
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS02"));
    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - sourceCanonical variations (WITH version in URI)
  // ==================================================================================================================

  // CM07: sourceCanonical with version in URI (|2025)
  // Maps versioned ValueSet to another versioned ValueSet
  public static ConceptMap createConceptMap_CM07() {
    ConceptMap cm = createConceptMapBase("CM07", "2025");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS00|2025"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS00|2026"));

    return cm;
  }

  // CM08: sourceCanonical with version, targetCanonical without version
  public static ConceptMap createConceptMap_CM08() {
    ConceptMap cm = createConceptMapBase("CM08", "2025");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS00|2025"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS01"));

    return cm;
  }

  // CM09: sourceCanonical without version, targetCanonical with version
  public static ConceptMap createConceptMap_CM09() {
    ConceptMap cm = createConceptMapBase("CM09", "2026");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS00|2026"));

    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - Mixed sourceUri and targetCanonical
  // ==================================================================================================================

  // CM10: sourceUri + targetCanonical (ValueSet) without version
  public static ConceptMap createConceptMap_CM10() {
    ConceptMap cm = createConceptMapBase("CM10", "2026");
    cm.setSource(new UriType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS02"));
    return cm;
  }

  // CM11: sourceUri + targetCanonical (ValueSet) WITH version
  public static ConceptMap createConceptMap_CM11() {
    ConceptMap cm = createConceptMapBase("CM11", "2026");
    cm.setSource(new UriType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS02|2026"));

    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - sourceCanonical + targetUri
  // ==================================================================================================================

  // CM12: sourceCanonical (ValueSet) without version + targetUri
  public static ConceptMap createConceptMap_CM12() {
    ConceptMap cm = createConceptMapBase("CM12", "2025");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS01"));
    cm.setTarget(new UriType(DEFAULT_VS_URL_PREFIX + "VS02"));

    return cm;
  }

  // CM13: sourceCanonical (ValueSet) WITH version + targetUri
  public static ConceptMap createConceptMap_CM13() {
    ConceptMap cm = createConceptMapBase("CM13", "2025");
    cm.setSource(new CanonicalType(DEFAULT_VS_URL_PREFIX + "VS04|2025"));
    cm.setTarget(new UriType(DEFAULT_VS_URL_PREFIX + "VS02"));

    return cm;
  }

  // ==================================================================================================================
  // Test ConceptMaps - Neither source nor target
  // ==================================================================================================================

  // CM14: Neither source nor target set
  public static ConceptMap createConceptMap_CM14() {
    // No source or target set
    return createConceptMapBase("CM14", "2025");
  }

  // Add to ConceptMapTestResourceGenerator class

  // ==================================================================================================================
  // Helper Methods for Groups
  // ==================================================================================================================

  private static ConceptMap.ConceptMapGroupComponent createGroup(
      String source, String sourceVersion, String target, String targetVersion) {
    ConceptMap.ConceptMapGroupComponent group = new ConceptMap.ConceptMapGroupComponent();
    if (source != null) {
      group.setSource(source);
    }
    if (sourceVersion != null) {
      group.setSourceVersion(sourceVersion);
    }
    if (target != null) {
      group.setTarget(target);
    }
    if (targetVersion != null) {
      group.setTargetVersion(targetVersion);
    }
    return group;
  }

  // ==================================================================================================================
  // Test ConceptMaps - Group source/target variations
  // ==================================================================================================================

  // CM15: Single group with source and target (no versions)
  public static ConceptMap createConceptMap_CM15() {
    ConceptMap cm = createConceptMapBase("CM15", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    return cm;
  }

  // CM16: Single group with source only (no version)
  public static ConceptMap createConceptMap_CM16() {
    ConceptMap cm = createConceptMapBase("CM16", "2025");
    cm.addGroup(createGroup(DEFAULT_CS_URL_PREFIX + "CS01", null, null, null));
    return cm;
  }

  // CM17: Single group with target only (no version)
  public static ConceptMap createConceptMap_CM17() {
    ConceptMap cm = createConceptMapBase("CM17", "2025");
    cm.addGroup(createGroup(null, null, DEFAULT_CS_URL_PREFIX + "CS02", null));
    return cm;
  }

  // CM18: Single group with source and sourceVersion
  public static ConceptMap createConceptMap_CM18() {
    ConceptMap cm = createConceptMapBase("CM18", "2025");
    cm.addGroup(
        createGroup(DEFAULT_CS_URL_PREFIX + "CS03", "2025", DEFAULT_CS_URL_PREFIX + "CS04", null));
    return cm;
  }

  // CM19: Single group with target and targetVersion
  public static ConceptMap createConceptMap_CM19() {
    ConceptMap cm = createConceptMapBase("CM19", "2025");
    cm.addGroup(
        createGroup(DEFAULT_CS_URL_PREFIX + "CS01", null, DEFAULT_CS_URL_PREFIX + "CS04", "2025"));
    return cm;
  }

  // CM20: Single group with source+sourceVersion and target+targetVersion
  public static ConceptMap createConceptMap_CM20() {
    ConceptMap cm = createConceptMapBase("CM20", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS03", "2025",
            DEFAULT_CS_URL_PREFIX + "CS04", "2025"));
    return cm;
  }

  // CM21: Single group with source+sourceVersion and target+targetVersion (different versions)
  public static ConceptMap createConceptMap_CM21() {
    ConceptMap cm = createConceptMapBase("CM21", "2026");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS03", "2026",
            DEFAULT_CS_URL_PREFIX + "CS04", "2026"));
    return cm;
  }

  // CM22: Multiple groups with same source, different targets
  public static ConceptMap createConceptMap_CM22() {
    ConceptMap cm = createConceptMapBase("CM22", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS05", null));
    return cm;
  }

  // CM23: Multiple groups with different sources, same target
  public static ConceptMap createConceptMap_CM23() {
    ConceptMap cm = createConceptMapBase("CM23", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS06", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    return cm;
  }

  // CM24: Multiple groups with versioned and non-versioned sources
  public static ConceptMap createConceptMap_CM24() {
    ConceptMap cm = createConceptMapBase("CM24", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS03", "2025",
            DEFAULT_CS_URL_PREFIX + "CS04", "2025"));
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS03", "2026",
            DEFAULT_CS_URL_PREFIX + "CS04", "2026"));
    return cm;
  }

  // CM25: Multiple groups with mixed version scenarios
  public static ConceptMap createConceptMap_CM25() {
    ConceptMap cm = createConceptMapBase("CM25", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS03", "2025",
            DEFAULT_CS_URL_PREFIX + "CS04", "2025"));
    return cm;
  }

  // CM26: Group without source or target
  public static ConceptMap createConceptMap_CM26() {
    ConceptMap cm = createConceptMapBase("CM26", "2025");
    cm.addGroup(createGroup(null, null, null, null));
    return cm;
  }

  // CM27: Multiple groups, one without source/target
  public static ConceptMap createConceptMap_CM27() {
    ConceptMap cm = createConceptMapBase("CM27", "2025");
    cm.addGroup(
        createGroup(
            DEFAULT_CS_URL_PREFIX + "CS01", null,
            DEFAULT_CS_URL_PREFIX + "CS02", null));
    cm.addGroup(createGroup(null, null, null, null));
    return cm;
  }

  // CM28: Group with sourceVersion but no source (edge case)
  public static ConceptMap createConceptMap_CM28() {
    ConceptMap cm = createConceptMapBase("CM28", "2025");
    cm.addGroup(createGroup(null, "2025", null, null));
    return cm;
  }

  // CM29: Group with targetVersion but no target (edge case)
  public static ConceptMap createConceptMap_CM29() {
    ConceptMap cm = createConceptMapBase("CM29", "2025");
    cm.addGroup(createGroup(null, null, null, "2025"));
    return cm;
  }

  // CM30: ConceptMap with no groups at all
  public static ConceptMap createConceptMap_CM30() {
    // No groups added
    return createConceptMapBase("CM30", "2025");
  }
}
