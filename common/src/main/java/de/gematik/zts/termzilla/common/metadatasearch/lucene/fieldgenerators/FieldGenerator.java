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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class FieldGenerator {

  private final ConceptComponentFieldGenerator conceptComponentFieldGenerator;
  private final CodeSystemFieldGenerator codeSystemFieldGenerator;
  private final ValueSetFieldGenerator valueSetFieldGenerator;
  private final ConceptMapFieldGenerator conceptMapFieldGenerator;

  public FieldGenerator() {
    conceptComponentFieldGenerator = new ConceptComponentFieldGenerator();
    codeSystemFieldGenerator = new CodeSystemFieldGenerator();
    valueSetFieldGenerator = new ValueSetFieldGenerator();
    conceptMapFieldGenerator = new ConceptMapFieldGenerator();
  }

  public void createFieldsFromResource(
      Document document, Resource resource, Field.Store fieldStore) {
    switch (resource.getResourceType()) {
      case CodeSystem:
        codeSystemFieldGenerator.createFields(document, (CodeSystem) resource, fieldStore);
        break;
      case ValueSet:
        valueSetFieldGenerator.createFields(document, (ValueSet) resource, fieldStore);
        break;
      case ConceptMap:
        conceptMapFieldGenerator.createFields(document, (ConceptMap) resource, fieldStore);
        break;
      default:
        break;
    }
  }

  public void createFieldsFromConcept(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {
    conceptComponentFieldGenerator.createFields(document, codeSystem, concept);
  }
}
