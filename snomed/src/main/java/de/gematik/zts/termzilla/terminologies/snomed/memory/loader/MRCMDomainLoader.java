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
import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata.MrcmDomain;
import de.gematik.zts.termzilla.terminologies.snomed.rf2.Rf2Columns;
import lombok.extern.slf4j.Slf4j;

/**
 * Loader for SNOMED CT MRCM Domain Reference Set files. Parses MRCM domain constraints including
 * templates and constraints for pre- and post-coordination.
 */
@Slf4j
public class MRCMDomainLoader extends AbstractSnomedLoader {

  /**
   * {@inheritDoc}
   *
   * <p>Parses one RF2 MRCM domain record and adds a {@link MrcmDomain} entity to the in-memory
   * model.
   */
  @Override
  protected void processLine(String line, SnomedInMemoryRepresentation model) {

    String[] lineParts = SnomedReleaseFileHelper.splitLine(line, Rf2Columns.MrcmDomain.MIN_COLUMNS);

    model.addMRCMDomain(
        MrcmDomain.builder()
            .id(lineParts[Rf2Columns.ID])
            .effectiveTime(Long.parseLong(lineParts[Rf2Columns.EFFECTIVE_TIME]))
            .active(parseBoolean(lineParts[Rf2Columns.ACTIVE]))
            .moduleId(lineParts[Rf2Columns.MODULE_ID])
            .refsetId(lineParts[Rf2Columns.MrcmDomain.REFSET_ID])
            .referencedComponentId(lineParts[Rf2Columns.MrcmDomain.REFERENCED_COMPONENT_ID])
            .domainConstraint(lineParts[Rf2Columns.MrcmDomain.DOMAIN_CONSTRAINT])
            .parentDomain(lineParts[Rf2Columns.MrcmDomain.PARENT_DOMAIN])
            .proximalPrimitiveConstraint(
                lineParts[Rf2Columns.MrcmDomain.PROXIMAL_PRIMITIVE_CONSTRAINT])
            .proximalPrimitiveRefinement(
                lineParts[Rf2Columns.MrcmDomain.PROXIMAL_PRIMITIVE_REFINEMENT])
            .domainTemplateForPrecoordination(
                lineParts[Rf2Columns.MrcmDomain.DOMAIN_TEMPLATE_FOR_PRECOORDINATION])
            .domainTemplateForPostcoordination(
                lineParts[Rf2Columns.MrcmDomain.DOMAIN_TEMPLATE_FOR_POSTCOORDINATION])
            .guideURL(lineParts[Rf2Columns.MrcmDomain.GUIDE_URL])
            .build());
  }
}
