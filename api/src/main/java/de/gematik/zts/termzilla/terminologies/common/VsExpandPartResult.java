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
package de.gematik.zts.termzilla.terminologies.common;

import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Container for partial results of a ValueSet expansion operation.
 *
 * <p>This class holds the results of expanding a single {@code include} or {@code exclude} element
 * from a {@code ValueSet.compose}. Multiple partial results are combined during the expansion
 * process: include results are unioned, exclude results are subtracted from the union.
 *
 * <p>The class maintains three types of information:
 *
 * <ul>
 *   <li>{@link #conceptBaseInfos} - The core concept identifiers in the result set
 *   <li>{@link #explicitConceptReferences} - Additional metadata from extensional definitions
 *   <li>{@link #systemVersionSelectionStrategy} - How the CodeSystem version was determined
 * </ul>
 *
 * @see ConceptBaseInfo
 * @see VersionSelectionStrategy
 */
@Getter
@Setter
public class VsExpandPartResult {

  /**
   * Indicates how the CodeSystem version was selected for expansion.
   *
   * <p>This information is included in the expansion parameters to ensure reproducible expansions.
   */
  public enum VersionSelectionStrategy {
    /** Version selection strategy is not known or not applicable. */
    UNKNOWN,
    /** Version was forced externally via {@code force-system-version} parameter. */
    EXTERNALLY_FORCE,
    /** Version was verified externally via {@code check-system-version} parameter. */
    EXTERNALLY_CHECK,
    /** Version was provided externally via {@code system-version} parameter. */
    EXTERNALLY_SET,
    /** Version was explicitly defined in the ValueSet compose element. */
    DEFINED,
    /** Version was selected as the system default (typically the latest available). */
    SYSTEM_DEFAULT
  }

  /**
   * The set of concepts included in this partial expansion result.
   *
   * <p>Uses a {@link TreeSet} to maintain natural ordering by system, version, and code.
   */
  private SortedSet<ConceptBaseInfo> conceptBaseInfos = new TreeSet<>();

  /**
   * Additional concept metadata from extensional ValueSet definitions.
   *
   * <p>Maps the {@link ConceptBaseInfo#hashCode()} to the original {@link
   * ValueSet.ConceptReferenceComponent} from the ValueSet definition. This preserves custom display
   * values and designations specified in the ValueSet.
   *
   * <p>Only populated for extensional definitions (where {@code include.concept} explicitly lists
   * codes). Empty for intensional definitions (filter-based).
   */
  private ArrayListValuedHashMap<Integer, ValueSet.ConceptReferenceComponent>
      explicitConceptReferences = new ArrayListValuedHashMap<>();

  /**
   * Records how the CodeSystem version was determined for each system in this result.
   *
   * <p>Maps CodeSystem URL to the {@link VersionSelectionStrategy} used. This information is
   * included in the final expansion parameters to document version selection decisions.
   */
  private Map<String, VersionSelectionStrategy> systemVersionSelectionStrategy = new HashMap<>();
}
