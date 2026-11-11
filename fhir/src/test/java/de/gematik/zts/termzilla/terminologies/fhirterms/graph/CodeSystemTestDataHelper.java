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
package de.gematik.zts.termzilla.terminologies.fhirterms.graph;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;

/** Helper class for creating test data for {@link FhirTermGraphRepresentation} tests. */
class CodeSystemTestDataHelper {

  private static final String TEST_URL = "http://test.example.com/CodeSystem/test";
  private static final String DEFAULT_VERSION = "1.0.0";

  /**
   * Creates a simple hierarchical code system with structure:
   *
   * <pre>
   * A
   * ├── A1
   * └── A2
   * B
   * </pre>
   */
  static CodeSystem createSimpleHierarchicalCodeSystem() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent conceptA = createConcept("A", "Concept A");
    conceptA.addConcept(createConcept("A1", "Concept A1"));
    conceptA.addConcept(createConcept("A2", "Concept A2"));

    CodeSystem.ConceptDefinitionComponent conceptB = createConcept("B", "Concept B");

    codeSystem.addConcept(conceptA);
    codeSystem.addConcept(conceptB);

    return codeSystem;
  }

  /** Creates a code system with explicit parent properties. */
  static CodeSystem createCodeSystemWithParentProperties() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent root = createConcept("ROOT", "Root Concept");

    CodeSystem.ConceptDefinitionComponent child1 = createConcept("CHILD1", "Child 1");
    child1.addProperty().setCode("parent").setValue(new CodeType("ROOT"));

    CodeSystem.ConceptDefinitionComponent child2 = createConcept("CHILD2", "Child 2");
    child2.addProperty().setCode("parent").setValue(new CodeType("ROOT"));

    codeSystem.addConcept(root);
    codeSystem.addConcept(child1);
    codeSystem.addConcept(child2);

    return codeSystem;
  }

  /** Creates a code system with explicit child properties. */
  static CodeSystem createCodeSystemWithChildProperties() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent root = createConcept("ROOT", "Root Concept");
    root.addProperty().setCode("child").setValue(new CodeType("CHILD1"));
    root.addProperty().setCode("child").setValue(new CodeType("CHILD2"));

    CodeSystem.ConceptDefinitionComponent child1 = createConcept("CHILD1", "Child 1");
    CodeSystem.ConceptDefinitionComponent child2 = createConcept("CHILD2", "Child 2");

    codeSystem.addConcept(root);
    codeSystem.addConcept(child1);
    codeSystem.addConcept(child2);

    return codeSystem;
  }

  /** Creates an empty code system with no concepts. */
  static CodeSystem createEmptyCodeSystem() {
    return createBaseCodeSystem();
  }

  /** Creates a code system with some concepts missing codes. */
  static CodeSystem createCodeSystemWithMissingCodes() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent validConcept =
        createConcept("VALID_CODE", "Valid Concept");

    CodeSystem.ConceptDefinitionComponent invalidConcept =
        new CodeSystem.ConceptDefinitionComponent();
    invalidConcept.setDisplay("Invalid Concept - No Code");

    codeSystem.addConcept(validConcept);
    codeSystem.addConcept(invalidConcept);

    return codeSystem;
  }

  /**
   * Creates a forest structure with multiple root nodes:
   *
   * <pre>
   * ROOT1
   * ├── CHILD1A
   * └── CHILD1B
   * ROOT2
   * └── CHILD2A
   * </pre>
   */
  static CodeSystem createForestStructureCodeSystem() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent root1 = createConcept("ROOT1", "Root 1");
    root1.addConcept(createConcept("CHILD1A", "Child 1A"));
    root1.addConcept(createConcept("CHILD1B", "Child 1B"));

    CodeSystem.ConceptDefinitionComponent root2 = createConcept("ROOT2", "Root 2");
    root2.addConcept(createConcept("CHILD2A", "Child 2A"));

    codeSystem.addConcept(root1);
    codeSystem.addConcept(root2);

    return codeSystem;
  }

  /** Creates a flat code system with no hierarchical relationships. */
  static CodeSystem createFlatCodeSystem() {
    CodeSystem codeSystem = createBaseCodeSystem();

    codeSystem.addConcept(createConcept("CODE1", "Code 1"));
    codeSystem.addConcept(createConcept("CODE2", "Code 2"));
    codeSystem.addConcept(createConcept("CODE3", "Code 3"));

    return codeSystem;
  }

  /**
   * Creates a deep hierarchy:
   *
   * <pre>
   * LEVEL1
   * └── LEVEL2
   *     └── LEVEL3
   *         └── LEVEL4
   * </pre>
   */
  static CodeSystem createDeepHierarchyCodeSystem() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent level1 = createConcept("LEVEL1", "Level 1");
    CodeSystem.ConceptDefinitionComponent level2 = createConcept("LEVEL2", "Level 2");
    CodeSystem.ConceptDefinitionComponent level3 = createConcept("LEVEL3", "Level 3");
    CodeSystem.ConceptDefinitionComponent level4 = createConcept("LEVEL4", "Level 4");

    level3.addConcept(level4);
    level2.addConcept(level3);
    level1.addConcept(level2);

    codeSystem.addConcept(level1);

    return codeSystem;
  }

  /**
   * Creates a diamond structure (multiple inheritance):
   *
   * <pre>
   *     TOP
   *    /   \
   * LEFT   RIGHT
   *    \   /
   *    BOTTOM
   * </pre>
   */
  static CodeSystem createDiamondStructureCodeSystem() {
    CodeSystem codeSystem = createBaseCodeSystem();

    CodeSystem.ConceptDefinitionComponent top = createConcept("TOP", "Top");
    CodeSystem.ConceptDefinitionComponent left = createConcept("LEFT", "Left");
    CodeSystem.ConceptDefinitionComponent right = createConcept("RIGHT", "Right");
    CodeSystem.ConceptDefinitionComponent bottom = createConcept("BOTTOM", "Bottom");

    // Create diamond using properties
    left.addProperty().setCode("parent").setValue(new CodeType("TOP"));
    right.addProperty().setCode("parent").setValue(new CodeType("TOP"));
    bottom.addProperty().setCode("parent").setValue(new CodeType("LEFT"));
    bottom.addProperty().setCode("parent").setValue(new CodeType("RIGHT"));

    codeSystem.addConcept(top);
    codeSystem.addConcept(left);
    codeSystem.addConcept(right);
    codeSystem.addConcept(bottom);

    return codeSystem;
  }

  private static CodeSystem createBaseCodeSystem() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(TEST_URL);
    codeSystem.setVersion(DEFAULT_VERSION);
    codeSystem.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
    return codeSystem;
  }

  private static CodeSystem.ConceptDefinitionComponent createConcept(String code, String display) {
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode(code);
    concept.setDisplay(display);
    return concept;
  }
}
