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
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ComponentHierarchyBySystem;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoincPartVersionFromHierarchyLoader {

  public void load(String file, int validFromVersion, LoincInMemoryRepresentation model) {

    log.info("Loading: " + file);

    try {
      Reader reader = Files.newBufferedReader(Paths.get(file));
      CsvToBean<ComponentHierarchyBySystem> csvToBean =
          new CsvToBeanBuilder<ComponentHierarchyBySystem>(reader)
              .withType(ComponentHierarchyBySystem.class)
              .withStrictQuotes(true)
              .withSeparator(',')
              .withIgnoreLeadingWhiteSpace(true)
              .build();

      try (Stream<ComponentHierarchyBySystem> stream = csvToBean.stream()) {
        stream.forEachOrdered(hierachy -> process(hierachy, validFromVersion, model));
      }

    } catch (IOException e) {
      throw new LoincInitializationException(e);
    }

    log.info("Loaded LOINC Parts from Hierarchy");
  }

  private void process(
      ComponentHierarchyBySystem hierarchy,
      int validFromVersion,
      LoincInMemoryRepresentation model) {

    if (hierarchy.getCode().startsWith("LP")) {

      PartVersion partVersion = new PartVersion();
      partVersion.setValidFromVersion(validFromVersion);
      partVersion.setPartNumber(hierarchy.getCode());
      partVersion.setPartName(hierarchy.getCodeText());
      partVersion.setStatus("ACTIVE");
      model.addPartHierarchy(partVersion);
    }
  }
}
