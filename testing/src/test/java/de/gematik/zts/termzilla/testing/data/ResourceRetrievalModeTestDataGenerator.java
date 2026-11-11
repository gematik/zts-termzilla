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

import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Test data generator for resource retrieval mode integration tests.
 *
 * <p>Creates a test package containing CodeSystem, ValueSet, and ConceptMap resources with
 * sufficient content to validate both FULL and STRIPPED retrieval modes.
 */
@Slf4j
public class ResourceRetrievalModeTestDataGenerator {

  public static final String PACKAGE_NAME = "de.gematik.zts.test.resourceretrieval";
  public static final String PACKAGE_VERSION = "1.0.0";

  public static final String TEST_CODESYSTEM_URL =
      "http://test.fhir.de/CodeSystem/retrieval-test-cs";
  public static final String TEST_VALUESET_URL = "http://test.fhir.de/ValueSet/retrieval-test-vs";
  public static final String TEST_CONCEPTMAP_URL =
      "http://test.fhir.de/ConceptMap/retrieval-test-cm";

  private ResourceRetrievalModeTestDataGenerator() {
    // Private constructor to prevent instantiation
  }

  /**
   * Generates a test package with sample resources for retrieval mode testing.
   *
   * @param packageLoadTempDir The directory where the package should be generated
   * @throws PackageGenerationException if package generation fails
   */
  public static void generateTestPackage(Path packageLoadTempDir)
      throws PackageGenerationException {
    log.info("Generating test package for resource retrieval mode tests");

    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName(PACKAGE_NAME)
            .packageVersion(PACKAGE_VERSION)
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createTestCodeSystem());
    resources.add(createTestValueSet());
    resources.add(createTestConceptMap());

    packageGeneration.generatePackage(resources, packageLoadTempDir);

    log.info("Test package generated successfully at: {}", packageLoadTempDir);
  }

  /**
   * Creates a test CodeSystem with multiple concepts for retrieval mode testing.
   *
   * @return A CodeSystem with hierarchical concept structure
   */
  private static CodeSystem createTestCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId("retrieval-test-cs");
    codeSystem.setUrl(TEST_CODESYSTEM_URL);
    codeSystem.setVersion("1.0.0");
    codeSystem.setName("RetrievalTestCodeSystem");
    codeSystem.setTitle("Resource Retrieval Test CodeSystem");
    codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
    codeSystem.setExperimental(true);
    codeSystem.setDescription("CodeSystem for testing FULL vs STRIPPED retrieval modes");
    codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    codeSystem.setCaseSensitive(true);

    // Add properties
    codeSystem
        .addProperty()
        .setCode("parent")
        .setType(CodeSystem.PropertyType.CODE)
        .setDescription("Parent concept code");

    codeSystem
        .addProperty()
        .setCode("status")
        .setType(CodeSystem.PropertyType.CODE)
        .setDescription("Concept status");

    // Add root concepts
    CodeSystem.ConceptDefinitionComponent concept1 =
        new CodeSystem.ConceptDefinitionComponent()
            .setCode("A00")
            .setDisplay("Root Concept A")
            .setDefinition("First root level concept");

    // Add child concepts
    concept1
        .addConcept()
        .setCode("A01")
        .setDisplay("Child Concept A.1")
        .setDefinition("First child of A");

    concept1
        .addConcept()
        .setCode("A02")
        .setDisplay("Child Concept A.2")
        .setDefinition("Second child of A");

    CodeSystem.ConceptDefinitionComponent concept2 =
        new CodeSystem.ConceptDefinitionComponent()
            .setCode("B00")
            .setDisplay("Root Concept B")
            .setDefinition("Second root level concept");

    concept2
        .addConcept()
        .setCode("B01")
        .setDisplay("Child Concept B.1")
        .setDefinition("First child of B");

    codeSystem.addConcept(concept1);
    codeSystem.addConcept(concept2);

    log.debug("Created test CodeSystem with {} root concepts", codeSystem.getConcept().size());
    return codeSystem;
  }

  /**
   * Creates a test ValueSet with compose and expansion for retrieval mode testing.
   *
   * @return A ValueSet with compose including the test CodeSystem
   */
  private static ValueSet createTestValueSet() {
    ValueSet valueSet = new ValueSet();
    valueSet.setId("retrieval-test-vs");
    valueSet.setUrl(TEST_VALUESET_URL);
    valueSet.setVersion("1.0.0");
    valueSet.setName("RetrievalTestValueSet");
    valueSet.setTitle("Resource Retrieval Test ValueSet");
    valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    valueSet.setExperimental(true);
    valueSet.setDescription("ValueSet for testing FULL vs STRIPPED retrieval modes");

    // Add compose
    ValueSet.ConceptSetComponent include = valueSet.getCompose().addInclude();
    include.setSystem(TEST_CODESYSTEM_URL);
    include.addConcept().setCode("A00").setDisplay("Root Concept A");
    include.addConcept().setCode("A01").setDisplay("Child Concept A.1");
    include.addConcept().setCode("B00").setDisplay("Root Concept B");

    // Add expansion
    ValueSet.ValueSetExpansionComponent expansion = valueSet.getExpansion();
    expansion.setTimestamp(new java.util.Date());
    expansion.setTotal(3);

    expansion
        .addContains()
        .setSystem(TEST_CODESYSTEM_URL)
        .setCode("A00")
        .setDisplay("Root Concept A");

    expansion
        .addContains()
        .setSystem(TEST_CODESYSTEM_URL)
        .setCode("A01")
        .setDisplay("Child Concept A.1");

    expansion
        .addContains()
        .setSystem(TEST_CODESYSTEM_URL)
        .setCode("B00")
        .setDisplay("Root Concept B");

    log.debug(
        "Created test ValueSet with {} includes and {} expansion entries",
        include.getConcept().size(),
        expansion.getContains().size());
    return valueSet;
  }

  /**
   * Creates a test ConceptMap with multiple groups for retrieval mode testing.
   *
   * @return A ConceptMap with source and target mappings
   */
  private static ConceptMap createTestConceptMap() {
    ConceptMap conceptMap = new ConceptMap();
    conceptMap.setId("retrieval-test-cm");
    conceptMap.setUrl(TEST_CONCEPTMAP_URL);
    conceptMap.setVersion("1.0.0");
    conceptMap.setName("RetrievalTestConceptMap");
    conceptMap.setTitle("Resource Retrieval Test ConceptMap");
    conceptMap.setStatus(Enumerations.PublicationStatus.ACTIVE);
    conceptMap.setExperimental(true);
    conceptMap.setDescription("ConceptMap for testing FULL vs STRIPPED retrieval modes");

    conceptMap.setSource(new UriType(TEST_CODESYSTEM_URL));
    conceptMap.setTarget(new UriType("http://test.fhir.de/CodeSystem/target-cs"));

    // Add first group
    ConceptMap.ConceptMapGroupComponent group1 = conceptMap.addGroup();
    group1.setSource(TEST_CODESYSTEM_URL);
    group1.setTarget("http://test.fhir.de/CodeSystem/target-cs");

    ConceptMap.SourceElementComponent element1 = group1.addElement();
    element1.setCode("A00");
    element1.setDisplay("Root Concept A");
    element1
        .addTarget()
        .setCode("T00")
        .setDisplay("Target Concept T00")
        .setEquivalence(Enumerations.ConceptMapEquivalence.EQUIVALENT);

    ConceptMap.SourceElementComponent element2 = group1.addElement();
    element2.setCode("A01");
    element2.setDisplay("Child Concept A.1");
    element2
        .addTarget()
        .setCode("T01")
        .setDisplay("Target Concept T01")
        .setEquivalence(Enumerations.ConceptMapEquivalence.EQUIVALENT);

    // Add second group
    ConceptMap.ConceptMapGroupComponent group2 = conceptMap.addGroup();
    group2.setSource(TEST_CODESYSTEM_URL);
    group2.setTarget("http://test.fhir.de/CodeSystem/target-cs-2");

    ConceptMap.SourceElementComponent element3 = group2.addElement();
    element3.setCode("B00");
    element3.setDisplay("Root Concept B");
    element3
        .addTarget()
        .setCode("T10")
        .setDisplay("Target Concept T10")
        .setEquivalence(Enumerations.ConceptMapEquivalence.RELATEDTO);

    log.debug("Created test ConceptMap with {} groups", conceptMap.getGroup().size());
    return conceptMap;
  }
}
