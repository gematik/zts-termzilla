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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader;

import java.util.Date;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Helper class for creating test data for FHIR resource loader tests. Provides reusable methods to
 * generate CodeSystem, ValueSet, and ConceptMap resources.
 */
public class FhirResourceTestDataHelper {

  private FhirResourceTestDataHelper() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a basic CodeSystem with the specified parameters.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured CodeSystem instance
   */
  public static CodeSystem createCodeSystem(String id, String url, String version) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(id);
    codeSystem.setUrl(url);
    codeSystem.setVersion(version);
    codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
    codeSystem.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    codeSystem.setMeta(createMeta());
    return codeSystem;
  }

  /**
   * Creates a CodeSystem with concepts.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured CodeSystem with sample concepts
   */
  public static CodeSystem createCodeSystemWithConcepts(String id, String url, String version) {
    CodeSystem codeSystem = createCodeSystem(id, url, version);

    CodeSystem.ConceptDefinitionComponent concept1 = new CodeSystem.ConceptDefinitionComponent();
    concept1.setCode("code1");
    concept1.setDisplay("Display 1");

    CodeSystem.ConceptDefinitionComponent concept2 = new CodeSystem.ConceptDefinitionComponent();
    concept2.setCode("code2");
    concept2.setDisplay("Display 2");

    codeSystem.addConcept(concept1);
    codeSystem.addConcept(concept2);

    return codeSystem;
  }

  /**
   * Creates a basic ValueSet with the specified parameters.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured ValueSet instance
   */
  public static ValueSet createValueSet(String id, String url, String version) {
    ValueSet valueSet = new ValueSet();
    valueSet.setId(id);
    valueSet.setUrl(url);
    valueSet.setVersion(version);
    valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    valueSet.setMeta(createMeta());
    return valueSet;
  }

  /**
   * Creates a ValueSet with compose element.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured ValueSet with compose
   */
  public static ValueSet createValueSetWithCompose(String id, String url, String version) {
    ValueSet valueSet = createValueSet(id, url, version);

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem("http://example.com/codesystem");
    compose.addInclude(include);

    valueSet.setCompose(compose);
    return valueSet;
  }

  /**
   * Creates a basic ConceptMap with the specified parameters.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured ConceptMap instance
   */
  public static ConceptMap createConceptMap(String id, String url, String version) {
    ConceptMap conceptMap = new ConceptMap();
    conceptMap.setId(id);
    conceptMap.setUrl(url);
    conceptMap.setVersion(version);
    conceptMap.setStatus(Enumerations.PublicationStatus.ACTIVE);
    conceptMap.setMeta(createMeta());
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with mapping groups.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured ConceptMap with sample mappings
   */
  public static ConceptMap createConceptMapWithMappings(String id, String url, String version) {
    ConceptMap conceptMap = createConceptMap(id, url, version);

    ConceptMap.ConceptMapGroupComponent group = new ConceptMap.ConceptMapGroupComponent();
    group.setSource("http://example.com/source");
    group.setTarget("http://example.com/target");

    ConceptMap.SourceElementComponent element = new ConceptMap.SourceElementComponent();
    element.setCode("sourceCode");

    ConceptMap.TargetElementComponent target = new ConceptMap.TargetElementComponent();
    target.setCode("targetCode");
    target.setEquivalence(Enumerations.ConceptMapEquivalence.EQUIVALENT);

    element.addTarget(target);
    group.addElement(element);
    conceptMap.addGroup(group);

    return conceptMap;
  }

  /**
   * Creates a Meta element with current timestamp.
   *
   * @return configured Meta instance
   */
  private static Meta createMeta() {
    Meta meta = new Meta();
    meta.setLastUpdated(new Date());
    return meta;
  }

  /**
   * Creates a CodeSystem for BfArM package testing.
   *
   * @param id resource ID
   * @param url canonical URL
   * @param version resource version
   * @return configured CodeSystem for BfArM package
   */
  public static CodeSystem createBfarmCodeSystem(String id, String url, String version) {
    CodeSystem codeSystem = createCodeSystem(id, url, version);
    Meta meta = codeSystem.getMeta();
    meta.setSource("https://terminologien.bfarm.de/packages/bfarm.terminologien.test/" + version);
    return codeSystem;
  }
}
