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
package de.gematik.zts.termzilla.terminologies.snomed.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import org.jgrapht.graph.DefaultEdge;

/**
 * Represents a directed IS-A relationship edge in the SNOMED CT concept hierarchy graph.
 *
 * <p>Each edge carries a unique relationship identifier, a module identifier, and a chronological
 * list of {@link RelationshipState} entries that track when the relationship was activated or
 * deactivated across different RF2 release versions.
 *
 * <p>The state list is maintained in ascending timestamp order. Querying {@link #isActive(long)}
 * returns the active/inactive status at a given effective time by finding the most recent state
 * entry at or before that timestamp.
 */
public class RelationshipEdge extends DefaultEdge {

  @Getter private final String id;
  @Getter private final String moduleId;
  private transient List<RelationshipState> stateList;
  private boolean sealed;

  /**
   * Creates a new relationship edge with the given identifier, module, and initial state.
   *
   * @param id the RF2 relationship identifier
   * @param moduleId the module this relationship belongs to
   * @param state the initial {@link RelationshipState} (effective time + active flag)
   */
  public RelationshipEdge(String id, String moduleId, RelationshipState state) {
    this.id = id;
    this.moduleId = moduleId;
    stateList = new ArrayList<>();
    stateList.add(state);
  }

  @Override
  public String toString() {
    return "("
        + getSource()
        + " : "
        + getTarget()
        + " : "
        + id
        + " "
        + moduleId
        + " "
        + stateList
        + ")";
  }

  /**
   * Determines whether this relationship is active at the given effective time.
   *
   * <p>Finds the most recent {@link RelationshipState} with a timestamp at or before {@code
   * effectiveTime} and returns its active flag. Returns {@code false} if no state exists at or
   * before the given time.
   *
   * @param effectiveTime the point in time to query ({@code yyyyMMdd} as long)
   * @return {@code true} if the relationship is active at the given time
   */
  public boolean isActive(long effectiveTime) {

    return stateList.stream()
        .filter(state -> state.timestamp() <= effectiveTime)
        .max(Comparator.comparingLong(RelationshipState::timestamp))
        .map(RelationshipState::state)
        .orElse(false);
  }

  /**
   * Checks whether this relationship belongs to the specified module.
   *
   * @param moduleId the module identifier to check against
   * @return {@code true} if this edge's module matches the given identifier
   */
  public boolean hasModuleId(String moduleId) {
    return this.moduleId.equals(moduleId);
  }

  /**
   * Adds a new state entry and re-sorts the state list in chronological order.
   *
   * <p>This is called during RF2 loading when the same relationship ID appears in multiple release
   * versions (e.g., activated in one release, deactivated in a later release).
   *
   * @param state the new {@link RelationshipState} to add
   * @throws IllegalStateException if this edge has been {@linkplain #seal() sealed}
   */
  public void addState(RelationshipState state) {
    if (sealed) {
      throw new IllegalStateException("Cannot add state to sealed RelationshipEdge '" + id + "'");
    }
    stateList.add(state);
    stateList.sort(Comparator.comparingLong(RelationshipState::timestamp));
  }

  /**
   * Freezes the state list, making this edge immutable.
   *
   * <p>After sealing, the internal state list is replaced with an unmodifiable copy. Any subsequent
   * call to {@link #addState(RelationshipState)} will throw {@link IllegalStateException}. Calling
   * {@code seal()} on an already sealed edge is a no-op.
   *
   * <p>This should be called after all RF2 relationship files have been loaded to prevent
   * accidental mutation of the graph during query operations.
   */
  public void seal() {
    if (!sealed) {
      stateList = List.copyOf(stateList);
      sealed = true;
    }
  }
}
