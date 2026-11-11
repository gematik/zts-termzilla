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
 * MRCM attribute-domain rule model.
 *
 * <p>Defines how an attribute may be used in a given domain, including grouping and cardinalities.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class MrcmAttributeDomainRule extends MrcmRule {

  /** Domain identifier in which the attribute is applicable. */
  private String domainId;

  /** Indicates whether the attribute is groupable. */
  private String grouped;

  /** Cardinality constraint for the attribute. */
  private String attributeCardinality;

  /** Cardinality constraint for grouped usage of the attribute. */
  private String attributeInGroupCardinality;
}
