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
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.language.Language;
import de.gematik.zts.termzilla.terminologies.snomed.rf2.Rf2Columns;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader for SNOMED CT Language Reference Set files. Parses language refset data including
 * acceptability information.
 */
@Slf4j
public class LanguageLoader extends AbstractSnomedLoader {

  /**
   * {@inheritDoc}
   *
   * <p>Parses one RF2 language reference set record and adds a {@link Language} entity to the
   * in-memory model.
   */
  @Override
  protected void processLine(String line, SnomedInMemoryRepresentation model) {

    String[] lineParts = SnomedReleaseFileHelper.splitLine(line, Rf2Columns.Language.MIN_COLUMNS);

    model.addLanguage(
        Language.builder()
            .id(lineParts[Rf2Columns.ID])
            .effectiveTime(Long.parseLong(lineParts[Rf2Columns.EFFECTIVE_TIME]))
            .active(parseBoolean(lineParts[Rf2Columns.ACTIVE]))
            .moduleId(lineParts[Rf2Columns.MODULE_ID])
            .refsetId(lineParts[Rf2Columns.Language.REFSET_ID])
            .referencedComponentId(lineParts[Rf2Columns.Language.REFERENCED_COMPONENT_ID])
            .acceptabilityId(lineParts[Rf2Columns.Language.ACCEPTABILITY_ID])
            .build());
  }
}
