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
 * Base model for MRCM rule entries used by SCG validation and processing.
 *
 * <p>The class contains metadata shared by all MRCM rule types from SNOMED reference sets.
 */
@Getter
@Setter
@ToString
public class MrcmRule {

  /** Unique identifier of the MRCM reference set row. */
  private String id;

  /** Effective time of the row in RF2 date format. */
  private String effectiveTime;

  /** Active flag of the row (RF2 encoded). */
  private String active;

  /** Module identifier that owns this row. */
  private String moduleId;

  /** Reference set identifier of the MRCM rule row. */
  private String refsetIdField;

  /** Referenced component identifier this rule applies to. */
  private String referencedComponentId;

  /** Rule strength concept identifier. */
  private String ruleStrengthId;

  /** Content type concept identifier. */
  private String contentTypeId;
}
