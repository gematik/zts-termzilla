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
package de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.metadata;

import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.refset.RefSet;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a row from the {@code der2_sssssssRefset_MRCMDomainFull} RF2 release file.
 *
 * <p>Extends the generic reference set fields with MRCM domain metadata including domain
 * constraints, proximal primitive information, and domain templates for pre- and post-coordination.
 */
@Getter
@SuperBuilder
public class MrcmDomain extends RefSet {

  /** Domain constraint expressed as an ECL expression. */
  String domainConstraint;

  /** Parent domain identifier. */
  String parentDomain;

  /** Proximal primitive constraint expressed as an ECL expression. */
  String proximalPrimitiveConstraint;

  /** Proximal primitive refinement expression. */
  String proximalPrimitiveRefinement;

  /** Domain template for pre-coordination authoring. */
  String domainTemplateForPrecoordination;

  /** Domain template for post-coordination authoring. */
  String domainTemplateForPostcoordination;

  /** URL to the editorial guide for this domain. */
  String guideURL;
}
