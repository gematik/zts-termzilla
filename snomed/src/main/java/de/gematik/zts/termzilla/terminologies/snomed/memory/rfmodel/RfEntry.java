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
package de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class for all SNOMED CT RF2 release file entities.
 *
 * <p>Holds the four columns shared across every RF2 entity type: component identifier, effective
 * time, active flag, and module identifier.
 */
@Getter
@SuperBuilder
@ToString
public abstract class RfEntry {

  /** Component identifier (SNOMED CT ID). */
  String id;

  /** Effective time in {@code yyyyMMdd} format, stored as a {@code long}. */
  long effectiveTime;

  /** Whether this component version is active ({@code true}) or inactive ({@code false}). */
  boolean active;

  /** Identifier of the module this component belongs to. */
  String moduleId;
}
