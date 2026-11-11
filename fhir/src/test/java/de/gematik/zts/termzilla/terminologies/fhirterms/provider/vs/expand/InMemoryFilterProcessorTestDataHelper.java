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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;

/** Test data helper for InMemoryFilterProcessor tests. */
final class InMemoryFilterProcessorTestDataHelper {

  static final String SYSTEM = "http://example.org/fhir/CodeSystem/filter";
  static final String VERSION = "1.0.0";

  private InMemoryFilterProcessorTestDataHelper() {
    // Utility class.
  }

  static CodeSystem createCodeSystemWithPropertyValues() {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(SYSTEM);
    codeSystem.setVersion(VERSION);
    codeSystem.setId("test-filter-cs");

    codeSystem.addProperty().setCode("stringProp").setType(CodeSystem.PropertyType.STRING);
    codeSystem.addProperty().setCode("codeProp").setType(CodeSystem.PropertyType.CODE);
    codeSystem.addProperty().setCode("boolProp").setType(CodeSystem.PropertyType.BOOLEAN);
    codeSystem.addProperty().setCode("decimalProp").setType(CodeSystem.PropertyType.DECIMAL);
    codeSystem.addProperty().setCode("intProp").setType(CodeSystem.PropertyType.INTEGER);
    codeSystem.addProperty().setCode("codingProp").setType(CodeSystem.PropertyType.CODING);
    codeSystem.addProperty().setCode("dateProp").setType(CodeSystem.PropertyType.DATETIME);

    CodeSystem.ConceptDefinitionComponent conceptOne =
        codeSystem.addConcept().setCode("C1").setDisplay("Concept One");
    conceptOne.addProperty().setCode("stringProp").setValue(new StringType("alpha-beta"));
    conceptOne.addProperty().setCode("codeProp").setValue(new org.hl7.fhir.r4.model.CodeType("A1"));
    conceptOne.addProperty().setCode("boolProp").setValue(new BooleanType(true));
    conceptOne.addProperty().setCode("decimalProp").setValue(new DecimalType("1.50"));
    conceptOne.addProperty().setCode("intProp").setValue(new IntegerType(42));
    conceptOne
        .addProperty()
        .setCode("codingProp")
        .setValue(new Coding("http://example.org/system", "1", "X1"));
    conceptOne.addProperty().setCode("dateProp").setValue(new DateTimeType("2024-01-01"));

    CodeSystem.ConceptDefinitionComponent conceptTwo =
        codeSystem.addConcept().setCode("C2").setDisplay("Concept Two");
    conceptTwo.addProperty().setCode("stringProp").setValue(new StringType("other"));
    conceptTwo.addProperty().setCode("codeProp").setValue(new org.hl7.fhir.r4.model.CodeType("B1"));
    conceptTwo.addProperty().setCode("boolProp").setValue(new BooleanType(false));
    conceptTwo.addProperty().setCode("decimalProp").setValue(new DecimalType("2.00"));
    conceptTwo.addProperty().setCode("intProp").setValue(new IntegerType(7));

    // Intentionally wrong type for stringProp to verify type-check filtering.
    conceptTwo
        .addProperty()
        .setCode("stringProp")
        .setValue(new org.hl7.fhir.r4.model.CodeType("WRONG"));

    return codeSystem;
  }

  static ValueSet.ConceptSetFilterComponent createFilter(
      ValueSet.FilterOperator operator, String property, String value) {
    return new ValueSet.ConceptSetFilterComponent()
        .setOp(operator)
        .setProperty(property)
        .setValue(value);
  }

  static Set<String> extractCodes(Set<ConceptBaseInfo> conceptBaseInfos) {
    return conceptBaseInfos.stream().map(ConceptBaseInfo::getCode).collect(Collectors.toSet());
  }
}
