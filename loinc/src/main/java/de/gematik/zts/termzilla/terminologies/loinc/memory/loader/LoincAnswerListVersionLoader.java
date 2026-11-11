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
package de.gematik.zts.termzilla.terminologies.loinc.memory.loader;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import de.gematik.zts.termzilla.terminologies.loinc.exceptions.LoincInitializationException;
import de.gematik.zts.termzilla.terminologies.loinc.memory.LoincInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoincAnswerListVersionLoader {

  Map<String, AnswerListVersion> alvMap = new HashMap<>();

  public void load(String file, int validFromVersion, LoincInMemoryRepresentation model) {

    log.info("Loading: " + file);

    try {
      Reader reader = Files.newBufferedReader(Paths.get(file));
      CsvToBean<AnswerListVersion> csvToBean =
          new CsvToBeanBuilder<AnswerListVersion>(reader)
              .withType(AnswerListVersion.class)
              .withStrictQuotes(true)
              .withSeparator(',')
              .withIgnoreLeadingWhiteSpace(true)
              .build();

      try (Stream<AnswerListVersion> stream = csvToBean.stream()) {
        stream.forEachOrdered(answerListVersion -> process(answerListVersion, validFromVersion));
      }

    } catch (IOException e) {
      throw new LoincInitializationException(e);
    }

    model.addAnswerListsForCurrentVersion(alvMap);

    log.info("Loaded LOINC AnswerLists");
  }

  private void process(AnswerListVersion answerListVersion, int validFromVersion) {

    String answerListId = answerListVersion.getAnswerListId();
    String answerListAnswer = answerListVersion.getTempAnswerStringId();

    // Check if the answerListId already exists in the map
    if (alvMap.containsKey(answerListId)) {
      // If it exists, retrieve the existing AnswerListVersion
      answerListVersion = alvMap.get(answerListId);
    } else {
      // Create a new AnswerListVersion if it doesn't exist
      answerListVersion.setValidFromVersion(validFromVersion);
      // jede in der CSV Datei enthaltene AntwortListe ist per Definition aktiv
      answerListVersion.setStatus("ACTIVE");
      alvMap.put(answerListId, answerListVersion);
    }

    // Add the answer to the existing or new AnswerListVersion
    answerListVersion.getAnswerIds().add(answerListAnswer);
    // Clear the temporary answer string ID as it is not needed anymore
    answerListVersion.setTempAnswerStringId(null);
  }
}
