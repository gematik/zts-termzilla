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
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a row from the {@code der2_ssRefset_ModuleDependencyFull} RF2 release file.
 *
 * <p>Extends the generic reference set fields with the source and target effective times that
 * describe the version dependency between two SNOMED CT modules.
 */
@Getter
@SuperBuilder
@ToString(callSuper = true)
public class ModuleDependency extends RefSet {

  /** Effective time of the source module ({@code yyyyMMdd} as {@code long}). */
  long sourceEffectiveTime;

  /** Effective time of the target (dependent) module ({@code yyyyMMdd} as {@code long}). */
  long targetEffectiveTime;
}
