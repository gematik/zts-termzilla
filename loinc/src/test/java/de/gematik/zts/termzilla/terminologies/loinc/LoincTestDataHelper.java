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
package de.gematik.zts.termzilla.terminologies.loinc;

import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LinguisticVariantVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.Set;
import java.util.TreeSet;

/** Factory helper for reusable LOINC unit-test fixtures. */
final class LoincTestDataHelper {

  private LoincTestDataHelper() {}

  static LoincVersion loincVersion(int number, String versionString) {
    return LoincVersion.builder().versionNumber(number).versionString(versionString).build();
  }

  static ConceptVersion activeConceptVersion(String code) {
    ConceptVersion conceptVersion = new ConceptVersion();
    conceptVersion.setLoincNum(code);
    conceptVersion.setStatus(LoincConstants.LOINC_STATUSCODE_ACTIVE);
    conceptVersion.setDefinitionDescription("Definition text");
    conceptVersion.setLongCommonName("Long common name");
    conceptVersion.setShortName("Short name");
    conceptVersion.setConsumerName("Consumer name");
    conceptVersion.setComponent("Component");
    conceptVersion.setProperty("Property");
    conceptVersion.setTimeAspect("Time");
    conceptVersion.setSystem("System");
    conceptVersion.setScaleType("Scale");
    conceptVersion.setMethodType("Method");
    conceptVersion.setValidFromVersion(276);
    return conceptVersion;
  }

  static PartVersion partVersion(
      String partNumber, String status, String partName, String partDisplayName) {
    PartVersion partVersion = new PartVersion();
    partVersion.setPartNumber(partNumber);
    partVersion.setStatus(status);
    partVersion.setPartName(partName);
    partVersion.setPartDisplayName(partDisplayName);
    partVersion.setValidFromVersion(276);
    return partVersion;
  }

  static AnswerVersion answerVersion(String answerStringId, String status, String displayText) {
    AnswerVersion answerVersion = new AnswerVersion();
    answerVersion.setAnswerStringId(answerStringId);
    answerVersion.setStatus(status);
    answerVersion.setDisplayText(displayText);
    answerVersion.setValidFromVersion(276);
    return answerVersion;
  }

  static AnswerListVersion answerListVersion(
      String answerListId, String status, String answerListName, Set<String> answerIds) {
    AnswerListVersion answerListVersion = new AnswerListVersion();
    answerListVersion.setAnswerListId(answerListId);
    answerListVersion.setStatus(status);
    answerListVersion.setAnswerListName(answerListName);
    answerListVersion.setAnswerIds(new TreeSet<>(answerIds));
    answerListVersion.setValidFromVersion(276);
    return answerListVersion;
  }

  static LinguisticVariantVersion linguisticVariantVersion(String loincCode, String language) {
    LinguisticVariantVersion linguisticVariantVersion = new LinguisticVariantVersion();
    linguisticVariantVersion.setLoincNumber(loincCode);
    linguisticVariantVersion.setLanguage(language);
    linguisticVariantVersion.setLongCommonName("Langer Name");
    linguisticVariantVersion.setShortName("Kurzname");
    linguisticVariantVersion.setComponent("Komponente");
    linguisticVariantVersion.setProperty("Eigenschaft");
    linguisticVariantVersion.setTimeAspect("Zeit");
    linguisticVariantVersion.setSystem("System");
    linguisticVariantVersion.setScaleType("Skala");
    linguisticVariantVersion.setMethodType("Methode");
    linguisticVariantVersion.setValidFromVersion(276);
    return linguisticVariantVersion;
  }
}
