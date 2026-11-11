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
package de.gematik.zts.termzilla.terminologies.snomed.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a SNOMED language reference set entry (RF2).
 *
 * <p>The entry links a description component to a language reference set and stores its
 * acceptability state for a specific release timestamp.
 */
@Getter
@Setter
public class SnomedLanguage {

  /** Unique identifier of the language reference set entry. */
  private String id;

  /** Effective time of the entry in RF2 date format. */
  private String effectiveTime;

  /** Indicates whether the entry is active in this version. */
  private boolean active;

  /** Module identifier that owns this entry. */
  private String moduleId;

  /** Language reference set identifier (for example en-GB or en-US refset). */
  private String refsetId;

  /** Referenced description component identifier. */
  private String referencedComponentId;

  /** Acceptability concept identifier (for example preferred or acceptable). */
  private String acceptabilityId;
}
