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
package de.gematik.zts.termzilla.terminologies.snomed.memory.loader;

import de.gematik.zts.termzilla.terminologies.snomed.helper.SnomedReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.snomed.memory.SnomedInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology.Description;
import de.gematik.zts.termzilla.terminologies.snomed.rf2.Rf2Columns;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader for SNOMED CT Description files. Parses description data including terms and language
 * information.
 */
@Slf4j
public class DescriptionLoader extends AbstractSnomedLoader {

  /**
   * {@inheritDoc}
   *
   * <p>Parses one RF2 description record and adds a {@link Description} entity to the in-memory
   * model.
   */
  @Override
  protected void processLine(String line, SnomedInMemoryRepresentation model) {

    String[] lineParts =
        SnomedReleaseFileHelper.splitLine(line, Rf2Columns.Description.MIN_COLUMNS);

    model.addDescription(
        Description.builder()
            .id(lineParts[Rf2Columns.ID])
            .effectiveTime(Long.parseLong(lineParts[Rf2Columns.EFFECTIVE_TIME]))
            .active(parseBoolean(lineParts[Rf2Columns.ACTIVE]))
            .moduleId(lineParts[Rf2Columns.MODULE_ID])
            .conceptId(lineParts[Rf2Columns.Description.CONCEPT_ID])
            .languageCode(lineParts[Rf2Columns.Description.LANGUAGE_CODE])
            .typeId(lineParts[Rf2Columns.Description.TYPE_ID])
            .term(lineParts[Rf2Columns.Description.TERM])
            .caseSignificanceId(lineParts[Rf2Columns.Description.CASE_SIGNIFICANCE_ID])
            .build());
  }
}
