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
package de.gematik.zts.termzilla.terminologies.loinc.memory;

import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LinguisticVariantVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LoincPartLinkVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Provides reusable test objects for LoincInMemoryRepresentation unit tests. */
final class LoincInMemoryRepresentationTestDataHelper {

  private LoincInMemoryRepresentationTestDataHelper() {}

  static LoincConfigProperties configProperties(
      String releaseCollectionLocation, Path baseWorkingDirectory, List<String> languages) {
    LoincConfigProperties properties = new LoincConfigProperties();
    properties.setLoincRfCollectionLocation(releaseCollectionLocation);
    properties.setBaseWorkingDirectory(baseWorkingDirectory);
    properties.setLoincLanguages(languages);
    return properties;
  }

  static LoincVersion loincVersion(int versionNumber, String versionString) {
    return LoincVersion.builder().versionNumber(versionNumber).versionString(versionString).build();
  }

  static ConceptVersion conceptVersion(
      String loincNumber, String versionLastChanged, int validFrom) {
    ConceptVersion conceptVersion = new ConceptVersion();
    conceptVersion.setLoincNum(loincNumber);
    conceptVersion.setVersionLastChanged(versionLastChanged);
    conceptVersion.setValidFromVersion(validFrom);
    return conceptVersion;
  }

  static PartVersion partVersion(
      String partNumber,
      String partTypeName,
      String partName,
      String partDisplayName,
      String status,
      int validFromVersion) {
    PartVersion partVersion = new PartVersion();
    partVersion.setPartNumber(partNumber);
    partVersion.setPartTypeName(partTypeName);
    partVersion.setPartName(partName);
    partVersion.setPartDisplayName(partDisplayName);
    partVersion.setStatus(status);
    partVersion.setValidFromVersion(validFromVersion);
    return partVersion;
  }

  static LoincPartLinkVersion loincPartLinkVersion(
      String loincNumber, String partNumber, String partTypeName, int validFromVersion) {
    LoincPartLinkVersion loincPartLinkVersion = new LoincPartLinkVersion();
    loincPartLinkVersion.setLoincNumber(loincNumber);
    loincPartLinkVersion.setPartNumber(partNumber);
    loincPartLinkVersion.setPartTypeName(partTypeName);
    loincPartLinkVersion.setValidFromVersion(validFromVersion);
    return loincPartLinkVersion;
  }

  static LinguisticVariantVersion linguisticVariantVersion(
      String loincNumber, String language, int validFromVersion, String longCommonName) {
    LinguisticVariantVersion linguisticVariantVersion = new LinguisticVariantVersion();
    linguisticVariantVersion.setLoincNumber(loincNumber);
    linguisticVariantVersion.setLanguage(language);
    linguisticVariantVersion.setValidFromVersion(validFromVersion);
    linguisticVariantVersion.setLongCommonName(longCommonName);
    return linguisticVariantVersion;
  }

  static AnswerVersion answerVersion(
      String answerStringId, String displayText, String status, int validFromVersion) {
    AnswerVersion answerVersion = new AnswerVersion();
    answerVersion.setAnswerStringId(answerStringId);
    answerVersion.setDisplayText(displayText);
    answerVersion.setStatus(status);
    answerVersion.setValidFromVersion(validFromVersion);
    return answerVersion;
  }

  static AnswerListVersion answerListVersion(
      String answerListId,
      String answerListName,
      String status,
      int validFromVersion,
      Set<String> answerIds) {
    AnswerListVersion answerListVersion = new AnswerListVersion();
    answerListVersion.setAnswerListId(answerListId);
    answerListVersion.setAnswerListName(answerListName);
    answerListVersion.setStatus(status);
    answerListVersion.setValidFromVersion(validFromVersion);
    answerListVersion.setAnswerIds(new TreeSet<>(answerIds));
    return answerListVersion;
  }
}
