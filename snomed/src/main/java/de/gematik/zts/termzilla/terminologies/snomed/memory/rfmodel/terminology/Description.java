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
package de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.terminology;

import de.gematik.zts.termzilla.terminologies.snomed.memory.rfmodel.RfEntry;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a row from the {@code sct2_Description_Full} RF2 release file.
 *
 * <p>Extends the common RF2 fields with description-specific columns such as the associated
 * concept, language, type, term text, and case significance.
 */
@Getter
@SuperBuilder
@ToString(callSuper = true)
public class Description extends RfEntry {

  /** Identifier of the concept this description belongs to. */
  String conceptId;

  /** ISO 639-1 language code (e.g., {@code "en"}, {@code "de"}). */
  String languageCode;

  /** Description type identifier (e.g., fully specified name or synonym). */
  String typeId;

  /** The human-readable description term text. */
  String term;

  /** Identifier indicating the case significance of this description. */
  String caseSignificanceId;
}
