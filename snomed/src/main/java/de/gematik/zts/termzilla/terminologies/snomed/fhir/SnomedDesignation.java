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
package de.gematik.zts.termzilla.terminologies.snomed.fhir;

import de.gematik.zts.termzilla.terminologies.common.Designation;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * SNOMED CT specific designation extending the base {@link Designation} with metadata from the
 * SNOMED CT RF2 model.
 *
 * <p>In addition to the inherited fields ({@code source}, {@code language}, {@code use}, {@code
 * value}), this class captures SNOMED CT specific information derived from description and language
 * reference set memberships:
 *
 * <ul>
 *   <li>{@link #context} — the language type reference set in which this designation participates
 *   <li>{@link #role} — the acceptability of the term within that reference set
 *   <li>{@link #type} — the SNOMED CT description type (e.g. Fully Specified Name, Synonym)
 * </ul>
 *
 * @see Designation
 * @see SnomedDesignationManager
 */
@Getter
@Setter
@SuperBuilder
public class SnomedDesignation extends Designation {

  /**
   * The language type reference set ID in which this designation is a member (e.g. {@code
   * "900000000000508004"} for the GB English language reference set).
   */
  String context;

  /**
   * The acceptability of this term within the language reference set, typically {@code
   * "900000000000548007"} (preferred) or {@code "900000000000549004"} (acceptable).
   */
  String role;

  /**
   * The SNOMED CT description type ID, e.g. {@code "900000000000003001"} for Fully Specified Name
   * or {@code "900000000000013009"} for Synonym.
   */
  String type;

  /**
   * The SNOMED CT significance ID, e.g. {@code "900000000000017005"} for Case Sensitive, {@code
   * "900000000000448009"} for Case Insensitive or {@code "900000000000020002"} Initial Character
   * Case Insensitive.
   */
  String caseSignificanceId;
}
