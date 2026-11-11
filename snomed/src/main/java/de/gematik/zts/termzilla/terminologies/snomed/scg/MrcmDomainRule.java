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
package de.gematik.zts.termzilla.terminologies.snomed.scg;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * MRCM domain rule model.
 *
 * <p>Represents domain-level constraints and templates for pre- and post-coordination.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class MrcmDomainRule extends MrcmRule {

  /** ECL expression defining the domain scope. */
  private String domainConstraint;

  /** Parent domain identifier if a domain hierarchy is defined. */
  private String parentDomain;

  /** Constraint for proximal primitive concepts. */
  private String proximalPrimitiveConstraint;

  /** Refinement expression for proximal primitive concepts. */
  private String proximalPrimitiveRefinement;

  /** Template definition for pre-coordinated expressions. */
  private String domainTemplateForPrecoordination;

  /** Template definition for post-coordinated expressions. */
  private String domainTemplateForPostcoordination;

  /** Guide URL associated with this domain rule. */
  private String guideURL;
}
