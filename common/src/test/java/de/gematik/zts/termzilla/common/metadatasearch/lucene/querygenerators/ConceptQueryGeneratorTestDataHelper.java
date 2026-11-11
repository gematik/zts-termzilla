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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;

/** Test data factory for ConceptQueryGenerator unit tests. */
public final class ConceptQueryGeneratorTestDataHelper {

  private ConceptQueryGeneratorTestDataHelper() {
    throw new AssertionError("ConceptQueryGeneratorTestDataHelper is a utility class");
  }

  static CodeSystem createCodeSystem(String idPart, Boolean caseSensitive) {
    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(idPart);
    if (caseSensitive != null) {
      codeSystem.setCaseSensitive(caseSensitive);
    }
    return codeSystem;
  }

  static PropertyFilter createCodePropertyFilter(String code, int position) {
    return new PropertyFilter("code", new CodeType(code), position);
  }

  static PropertyFilter createCodeValuePropertyFilter(
      String propertyName, String code, int position) {
    return new PropertyFilter(propertyName, new CodeType(code), position);
  }

  static PropertyFilter createStringPropertyFilter(
      String propertyName, String value, int position) {
    return new PropertyFilter(propertyName, new StringType(value), position);
  }

  static PropertyFilter createCodingPropertyFilter(
      String propertyName, String system, String version, String code, int position) {
    Coding coding = new Coding();
    coding.setSystem(system);
    coding.setVersion(version);
    coding.setCode(code);
    return new PropertyFilter(propertyName, coding, position);
  }
}
