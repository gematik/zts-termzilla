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
package de.gematik.zts.termzilla.terminologies.snomed.testutil;

import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.graph.RelationshipEdge;
import de.gematik.zts.termzilla.terminologies.snomed.graph.RelationshipState;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.ModuleDependency;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmAttributeRange;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Concept;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 * Provides reusable factory methods for creating SNOMED CT test fixtures.
 *
 * <p>All factory methods produce small, self-contained data objects that are suitable for unit
 * tests. The test data models a minimal concept hierarchy with two languages (English and German)
 * across two release versions.
 *
 * <p>The default hierarchy is:
 *
 * <pre>
 *   ROOT (138875005)
 *    ├── CLINICAL_FINDING (404684003)
 *    │    ├── DISORDER (64572001)
 *    │    │    ├── DIABETES (73211009)
 *    │    │    └── HYPERTENSION (38341003)
 *    │    └── FINDING (441742003)
 *    └── PROCEDURE (71388002)
 *         └── SURGERY (387713003)
 * </pre>
 */
public final class SnomedTestData {

  private SnomedTestData() {}

  // ──────────────────────────────────────────────────────────────────────────
  // RF2 file helpers
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Joins fields into a tab-separated RF2 line.
   *
   * @param fields the field values to join
   * @return a single tab-separated string
   */
  public static String rf2Line(String... fields) {
    return String.join("\t", fields);
  }

  /**
   * Writes an RF2 file with a header line followed by data lines.
   *
   * @param dir the directory to create the file in
   * @param fileName the name of the file
   * @param header the header line
   * @param dataLines the data lines
   * @return the path to the created file
   * @throws IOException if writing fails
   */
  public static Path writeRf2File(Path dir, String fileName, String header, String... dataLines)
      throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add(header);
    lines.addAll(Arrays.asList(dataLines));
    return Files.write(dir.resolve(fileName), lines);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Concept IDs – small realistic hierarchy
  // ──────────────────────────────────────────────────────────────────────────

  public static final String CONCEPT_ROOT = "138875005";
  public static final String CONCEPT_CLINICAL_FINDING = "404684003";
  public static final String CONCEPT_DISORDER = "64572001";
  public static final String CONCEPT_DIABETES = "73211009";
  public static final String CONCEPT_HYPERTENSION = "38341003";
  public static final String CONCEPT_FINDING = "441742003";
  public static final String CONCEPT_PROCEDURE = "71388002";
  public static final String CONCEPT_SURGERY = "387713003";

  public static final String CONCEPT_UNKNOWN = "999999999";

  // ──────────────────────────────────────────────────────────────────────────
  // Module and version constants
  // ──────────────────────────────────────────────────────────────────────────

  public static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  public static final String MODULE_GERMAN =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  public static final String DATE_V1 = "20240515";
  public static final String DATE_V2 = "20241115";
  public static final String DATE_V3 = "20250515";

  public static final long EFFECTIVE_TIME_V1 = 20240515L;
  public static final long EFFECTIVE_TIME_V2 = 20241115L;
  public static final long EFFECTIVE_TIME_V3 = 20250515L;

  // ──────────────────────────────────────────────────────────────────────────
  // Description type IDs (re-exported for convenience)
  // ──────────────────────────────────────────────────────────────────────────

  public static final String TYPE_FSN = SnomedConstants.SC_FULLYSPECIFIEDNAME;
  public static final String TYPE_SYNONYM = SnomedConstants.SC_SYNONYM;

  // ──────────────────────────────────────────────────────────────────────────
  // Acceptability IDs (re-exported for convenience)
  // ──────────────────────────────────────────────────────────────────────────

  public static final String ACCEPTABILITY_PREFERRED = SnomedConstants.SC_PREFERRED;
  public static final String ACCEPTABILITY_ACCEPTABLE = SnomedConstants.SC_ACCEPTABLE;

  // ──────────────────────────────────────────────────────────────────────────
  // Language reference set IDs (re-exported for convenience)
  // ──────────────────────────────────────────────────────────────────────────

  public static final String REFSET_EN_US = SnomedReleaseVersionConstants.EN_US;
  public static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  public static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;

  // ──────────────────────────────────────────────────────────────────────────
  // MRCM reference set IDs
  // ──────────────────────────────────────────────────────────────────────────

  public static final String MRCM_DOMAIN_REFSET_ID = "723589008";
  public static final String MRCM_ATTRIBUTE_DOMAIN_REFSET_ID = "723604009";
  public static final String MRCM_ATTRIBUTE_RANGE_REFSET_ID = "723592007";

  // ──────────────────────────────────────────────────────────────────────────
  // SnomedReleaseVersion factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates a {@link SnomedReleaseVersion} for the international core module at the given date.
   *
   * @param date the effective date in {@code yyyyMMdd} format
   * @return a release version with no dependencies
   */
  public static SnomedReleaseVersion coreVersion(String date) {
    try {
      return new SnomedReleaseVersion(MODULE_CORE, date, Map.of());
    } catch (UnsupportedTerminologyException e) {
      throw new IllegalArgumentException("Invalid test version: " + date, e);
    }
  }

  /**
   * Creates a {@link SnomedReleaseVersion} for the German edition at the given date, with a
   * dependency on the international core module.
   *
   * @param date the effective date of the German edition
   * @param coreDependencyDate the effective date of the core dependency
   * @return a German edition release version
   */
  public static SnomedReleaseVersion germanVersion(String date, String coreDependencyDate) {
    try {
      return new SnomedReleaseVersion(MODULE_GERMAN, date, Map.of(MODULE_CORE, coreDependencyDate));
    } catch (UnsupportedTerminologyException e) {
      throw new IllegalArgumentException("Invalid test version: " + date, e);
    }
  }

  /** Creates a default core release version at {@link #DATE_V1}. */
  public static SnomedReleaseVersion defaultCoreVersion() {
    return coreVersion(DATE_V1);
  }

  /**
   * Creates a default German release version at {@link #DATE_V2}, depending on core {@link
   * #DATE_V1}.
   */
  public static SnomedReleaseVersion defaultGermanVersion() {
    return germanVersion(DATE_V2, DATE_V1);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Concept factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an active {@link Concept} in the core module at the given effective time.
   *
   * @param id the concept identifier
   * @param effectiveTime the effective time as {@code yyyyMMdd} long
   * @return an active concept with {@code definitionStatusId} set to sufficiently defined
   */
  public static Concept concept(String id, long effectiveTime) {
    return concept(id, effectiveTime, true, MODULE_CORE);
  }

  /**
   * Creates a {@link Concept} with full control over all fields.
   *
   * @param id the concept identifier
   * @param effectiveTime the effective time
   * @param active whether the concept is active
   * @param moduleId the module identifier
   * @return a concept with the specified properties
   */
  public static Concept concept(String id, long effectiveTime, boolean active, String moduleId) {
    return Concept.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(active)
        .moduleId(moduleId)
        .definitionStatusId(SnomedConstants.SC_SUFFICENTLYDEFINED)
        .build();
  }

  /**
   * Creates an inactive {@link Concept} in the core module at the given effective time.
   *
   * @param id the concept identifier
   * @param effectiveTime the effective time
   * @return an inactive concept
   */
  public static Concept inactiveConcept(String id, long effectiveTime) {
    return concept(id, effectiveTime, false, MODULE_CORE);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Description factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an active FSN {@link Description} in English.
   *
   * @param id the description identifier
   * @param conceptId the owning concept identifier
   * @param term the description text
   * @param effectiveTime the effective time
   * @return an active FSN description
   */
  public static Description fsnDescription(
      String id, String conceptId, String term, long effectiveTime) {
    return description(id, conceptId, "en", TYPE_FSN, term, effectiveTime);
  }

  /**
   * Creates an active synonym {@link Description} in the given language.
   *
   * @param id the description identifier
   * @param conceptId the owning concept identifier
   * @param languageCode the BCP 47 language code
   * @param term the description text
   * @param effectiveTime the effective time
   * @return an active synonym description
   */
  public static Description synonymDescription(
      String id, String conceptId, String languageCode, String term, long effectiveTime) {
    return description(id, conceptId, languageCode, TYPE_SYNONYM, term, effectiveTime);
  }

  /**
   * Creates a {@link Description} with full control over all fields.
   *
   * @param id the description identifier
   * @param conceptId the owning concept identifier
   * @param languageCode the BCP 47 language code
   * @param typeId the description type identifier
   * @param term the description text
   * @param effectiveTime the effective time
   * @return an active description in the core module
   */
  public static Description description(
      String id,
      String conceptId,
      String languageCode,
      String typeId,
      String term,
      long effectiveTime) {
    return description(id, conceptId, languageCode, typeId, term, effectiveTime, true, MODULE_CORE);
  }

  /**
   * Creates a {@link Description} with full control over all fields including active status and
   * module.
   */
  public static Description description(
      String id,
      String conceptId,
      String languageCode,
      String typeId,
      String term,
      long effectiveTime,
      boolean active,
      String moduleId) {
    return Description.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(active)
        .moduleId(moduleId)
        .conceptId(conceptId)
        .languageCode(languageCode)
        .typeId(typeId)
        .term(term)
        .caseSignificanceId(SnomedConstants.SC_CASESINSENSITIVE)
        .build();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Language (acceptability) factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an active preferred {@link Language} entry for a description in the given refset.
   *
   * @param id the language entry identifier
   * @param descriptionId the referenced description identifier
   * @param refsetId the language reference set identifier
   * @param effectiveTime the effective time
   * @return an active preferred language entry in the core module
   */
  public static Language preferredLanguage(
      String id, String descriptionId, String refsetId, long effectiveTime) {
    return language(id, descriptionId, refsetId, ACCEPTABILITY_PREFERRED, effectiveTime);
  }

  /**
   * Creates an active acceptable {@link Language} entry for a description in the given refset.
   *
   * @param id the language entry identifier
   * @param descriptionId the referenced description identifier
   * @param refsetId the language reference set identifier
   * @param effectiveTime the effective time
   * @return an active acceptable language entry in the core module
   */
  public static Language acceptableLanguage(
      String id, String descriptionId, String refsetId, long effectiveTime) {
    return language(id, descriptionId, refsetId, ACCEPTABILITY_ACCEPTABLE, effectiveTime);
  }

  /** Creates a {@link Language} entry with full control over all fields. */
  public static Language language(
      String id,
      String descriptionId,
      String refsetId,
      String acceptabilityId,
      long effectiveTime) {
    return language(id, descriptionId, refsetId, acceptabilityId, effectiveTime, true, MODULE_CORE);
  }

  /**
   * Creates a {@link Language} entry with full control over all fields including active status and
   * module.
   */
  public static Language language(
      String id,
      String descriptionId,
      String refsetId,
      String acceptabilityId,
      long effectiveTime,
      boolean active,
      String moduleId) {
    return Language.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(active)
        .moduleId(moduleId)
        .refsetId(refsetId)
        .referencedComponentId(descriptionId)
        .acceptabilityId(acceptabilityId)
        .build();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // MRCM factories
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an active {@link MrcmDomain} entry.
   *
   * @param id the entry identifier
   * @param referencedComponentId the referenced domain concept
   * @param domainConstraint the ECL domain constraint
   * @param effectiveTime the effective time
   * @return an active MRCM domain entry
   */
  public static MrcmDomain mrcmDomain(
      String id, String referencedComponentId, String domainConstraint, long effectiveTime) {
    return MrcmDomain.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(true)
        .moduleId(MODULE_CORE)
        .refsetId(MRCM_DOMAIN_REFSET_ID)
        .referencedComponentId(referencedComponentId)
        .domainConstraint(domainConstraint)
        .parentDomain("")
        .proximalPrimitiveConstraint("")
        .proximalPrimitiveRefinement("")
        .domainTemplateForPrecoordination("")
        .domainTemplateForPostcoordination("")
        .guideURL("")
        .build();
  }

  /**
   * Creates an active {@link MrcmAttributeDomain} entry.
   *
   * @param id the entry identifier
   * @param referencedComponentId the referenced attribute concept
   * @param domainId the domain concept this attribute applies to
   * @param effectiveTime the effective time
   * @return an active MRCM attribute domain entry
   */
  public static MrcmAttributeDomain mrcmAttributeDomain(
      String id, String referencedComponentId, String domainId, long effectiveTime) {
    return MrcmAttributeDomain.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(true)
        .moduleId(MODULE_CORE)
        .refsetId(MRCM_ATTRIBUTE_DOMAIN_REFSET_ID)
        .referencedComponentId(referencedComponentId)
        .domainId(domainId)
        .grouped(false)
        .attributeCardinality("0..*")
        .attributeInGroupCardinality("0..1")
        .ruleStrengthId("723597001")
        .contentTypeId("723596005")
        .build();
  }

  /**
   * Creates an active {@link MrcmAttributeRange} entry.
   *
   * @param id the entry identifier
   * @param referencedComponentId the referenced attribute concept
   * @param rangeConstraint the ECL range constraint
   * @param effectiveTime the effective time
   * @return an active MRCM attribute range entry
   */
  public static MrcmAttributeRange mrcmAttributeRange(
      String id, String referencedComponentId, String rangeConstraint, long effectiveTime) {
    return MrcmAttributeRange.builder()
        .id(id)
        .effectiveTime(effectiveTime)
        .active(true)
        .moduleId(MODULE_CORE)
        .refsetId(MRCM_ATTRIBUTE_RANGE_REFSET_ID)
        .referencedComponentId(referencedComponentId)
        .rangeConstraint(rangeConstraint)
        .attributeRule("")
        .ruleStrengthId("723597001")
        .contentTypeId("723596005")
        .build();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Module dependency factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates an active {@link ModuleDependency} entry.
   *
   * @param id the entry identifier
   * @param sourceModuleId the module that has the dependency
   * @param targetModuleId the module being depended on
   * @param sourceEffectiveTime the source module effective time
   * @param targetEffectiveTime the target module effective time
   * @return an active module dependency
   */
  public static ModuleDependency moduleDependency(
      String id,
      String sourceModuleId,
      String targetModuleId,
      long sourceEffectiveTime,
      long targetEffectiveTime) {
    return ModuleDependency.builder()
        .id(id)
        .effectiveTime(sourceEffectiveTime)
        .active(true)
        .moduleId(sourceModuleId)
        .refsetId("900000000000534007")
        .referencedComponentId(targetModuleId)
        .sourceEffectiveTime(sourceEffectiveTime)
        .targetEffectiveTime(targetEffectiveTime)
        .build();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Graph factory
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates a new empty directed graph suitable for SNOMED IS-A relationships.
   *
   * @return a directed multigraph without self-loops
   */
  public static Graph<String, RelationshipEdge> emptyGraph() {
    return GraphTypeBuilder.directed()
        .allowingMultipleEdges(true)
        .allowingSelfLoops(false)
        .vertexClass(String.class)
        .edgeClass(RelationshipEdge.class)
        .buildGraph();
  }

  /**
   * Adds an active IS-A relationship edge between two concepts at the given effective time.
   *
   * @param graph the graph to modify
   * @param edgeId the relationship identifier
   * @param sourceId the child concept
   * @param targetId the parent concept
   * @param effectiveTime the effective time
   */
  public static void addIsARelationship(
      Graph<String, RelationshipEdge> graph,
      String edgeId,
      String sourceId,
      String targetId,
      long effectiveTime) {
    addIsARelationship(graph, edgeId, sourceId, targetId, effectiveTime, true);
  }

  /**
   * Adds an IS-A relationship edge between two concepts with the given active status.
   *
   * @param graph the graph to modify
   * @param edgeId the relationship identifier
   * @param sourceId the child concept
   * @param targetId the parent concept
   * @param effectiveTime the effective time
   * @param active whether the relationship is active
   */
  public static void addIsARelationship(
      Graph<String, RelationshipEdge> graph,
      String edgeId,
      String sourceId,
      String targetId,
      long effectiveTime,
      boolean active) {
    graph.addVertex(sourceId);
    graph.addVertex(targetId);
    RelationshipEdge edge =
        new RelationshipEdge(edgeId, MODULE_CORE, new RelationshipState(effectiveTime, active));
    graph.addEdge(sourceId, targetId, edge);
  }

  /**
   * Builds the default test hierarchy as a directed graph.
   *
   * <p>All relationships are active at {@link #EFFECTIVE_TIME_V1}. The hierarchy is:
   *
   * <pre>
   *   ROOT
   *    ├── CLINICAL_FINDING
   *    │    ├── DISORDER
   *    │    │    ├── DIABETES
   *    │    │    └── HYPERTENSION
   *    │    └── FINDING
   *    └── PROCEDURE
   *         └── SURGERY
   * </pre>
   *
   * @return a graph with 8 concepts and 7 IS-A relationships
   */
  public static Graph<String, RelationshipEdge> defaultHierarchy() {
    Graph<String, RelationshipEdge> graph = emptyGraph();

    addIsARelationship(graph, "rel-1", CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT, EFFECTIVE_TIME_V1);
    addIsARelationship(graph, "rel-2", CONCEPT_PROCEDURE, CONCEPT_ROOT, EFFECTIVE_TIME_V1);
    addIsARelationship(
        graph, "rel-3", CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, EFFECTIVE_TIME_V1);
    addIsARelationship(
        graph, "rel-4", CONCEPT_FINDING, CONCEPT_CLINICAL_FINDING, EFFECTIVE_TIME_V1);
    addIsARelationship(graph, "rel-5", CONCEPT_DIABETES, CONCEPT_DISORDER, EFFECTIVE_TIME_V1);
    addIsARelationship(graph, "rel-6", CONCEPT_HYPERTENSION, CONCEPT_DISORDER, EFFECTIVE_TIME_V1);
    addIsARelationship(graph, "rel-7", CONCEPT_SURGERY, CONCEPT_PROCEDURE, EFFECTIVE_TIME_V1);

    return graph;
  }
}
