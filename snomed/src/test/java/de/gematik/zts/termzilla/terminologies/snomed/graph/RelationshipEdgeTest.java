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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests for {@link RelationshipEdge}.
 *
 * <p>These tests document the current behaviour of the edge's state-tracking logic, including edge
 * cases around timestamp boundaries and multiple state transitions. They serve as a safety net for
 * future refactoring.
 */
@DisplayName("RelationshipEdge Tests")
class RelationshipEdgeTest {

  private static final String EDGE_ID = "rel-100";
  private static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_GERMAN =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  // ──────────────────────────────────────────────────────────────────────────
  // Constructor
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should store id, moduleId and initial state")
    void shouldStoreIdModuleIdAndInitialState() {
      // GIVEN
      RelationshipState initialState = new RelationshipState(20240101L, true);

      // WHEN
      RelationshipEdge edge = new RelationshipEdge(EDGE_ID, MODULE_CORE, initialState);

      // THEN
      assertThat(edge.getId()).as("Edge id should match constructor argument").isEqualTo(EDGE_ID);
      assertThat(edge.getModuleId())
          .as("Module id should match constructor argument")
          .isEqualTo(MODULE_CORE);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // isActive()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("isActive() Tests")
  class IsActiveTests {

    @Test
    @DisplayName("Should return true when queried at exact timestamp of active state")
    void shouldReturnTrueAtExactActiveTimestamp() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.isActive(20240101L);

      // THEN
      assertThat(result).as("Edge should be active at its exact activation timestamp").isTrue();
    }

    @Test
    @DisplayName("Should return true when queried after the activation timestamp")
    void shouldReturnTrueAfterActivation() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.isActive(20241231L);

      // THEN
      assertThat(result).as("Edge should remain active after its activation timestamp").isTrue();
    }

    @Test
    @DisplayName("Should return false when queried before the earliest state timestamp")
    void shouldReturnFalseBeforeEarliestState() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.isActive(20230101L);

      // THEN
      assertThat(result).as("Edge should not be active before its first state timestamp").isFalse();
    }

    @Test
    @DisplayName("Should return false when the only state is inactive")
    void shouldReturnFalseWhenOnlyStateIsInactive() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, false));

      // WHEN
      boolean result = edge.isActive(20240601L);

      // THEN
      assertThat(result).as("Edge should be inactive when its only state is inactive").isFalse();
    }

    @Test
    @DisplayName("Should reflect state change from active to inactive")
    void shouldReflectStateChangeActiveToInactive() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));
      edge.addState(new RelationshipState(20240601L, false));

      // WHEN / THEN
      assertThat(edge.isActive(20240101L))
          .as("Should be active at the first state's timestamp")
          .isTrue();
      assertThat(edge.isActive(20240515L))
          .as("Should still be active between first and second state")
          .isTrue();
      assertThat(edge.isActive(20240601L))
          .as("Should be inactive at the deactivation timestamp")
          .isFalse();
      assertThat(edge.isActive(20241231L))
          .as("Should remain inactive after deactivation")
          .isFalse();
    }

    @Test
    @DisplayName("Should reflect state change from inactive to active")
    void shouldReflectStateChangeInactiveToActive() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, false));
      edge.addState(new RelationshipState(20240601L, true));

      // WHEN / THEN
      assertThat(edge.isActive(20240101L))
          .as("Should be inactive at the first state's timestamp")
          .isFalse();
      assertThat(edge.isActive(20240515L))
          .as("Should still be inactive between first and second state")
          .isFalse();
      assertThat(edge.isActive(20240601L))
          .as("Should be active at the reactivation timestamp")
          .isTrue();
    }

    @Test
    @DisplayName("Should handle multiple state transitions correctly")
    void shouldHandleMultipleStateTransitions() {
      // GIVEN – active → inactive → active
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20230101L, true));
      edge.addState(new RelationshipState(20240101L, false));
      edge.addState(new RelationshipState(20240601L, true));

      // WHEN / THEN
      assertThat(edge.isActive(20220101L)).as("Before any state: should be inactive").isFalse();
      assertThat(edge.isActive(20230601L))
          .as("Between first and second state: should be active")
          .isTrue();
      assertThat(edge.isActive(20240301L))
          .as("Between second and third state: should be inactive")
          .isFalse();
      assertThat(edge.isActive(20240601L))
          .as("At third state timestamp: should be active again")
          .isTrue();
      assertThat(edge.isActive(20250101L)).as("After last state: should still be active").isTrue();
    }

    @Test
    @DisplayName("Should handle states added in non-chronological order")
    void shouldHandleStatesAddedOutOfOrder() {
      // GIVEN – add states out of chronological order
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240601L, false));
      edge.addState(new RelationshipState(20230101L, true));

      // WHEN / THEN – addState sorts, so isActive should still work correctly
      assertThat(edge.isActive(20230601L))
          .as("Should be active in the period of the earlier active state")
          .isTrue();
      assertThat(edge.isActive(20240601L))
          .as("Should be inactive at the later deactivation timestamp")
          .isFalse();
    }

    // EDGE CASE: Could this really happen in real RF2 data? Two states with the same timestamp but
    // different active flags?
    @Test
    @DisplayName("Should return active when duplicate timestamps exist and first state is active")
    void shouldHandleDuplicateTimestampsFirstActive() {
      // GIVEN – two states at the same timestamp (edge case in RF2 data)
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));
      edge.addState(new RelationshipState(20240101L, false));

      // WHEN
      boolean result = edge.isActive(20240101L);

      // THEN
      // NOTE: With stream().filter(<=).max(comparingLong), when two states share the
      // same timestamp, the behaviour of max() on equal elements is implementation-
      // dependent. The current JDK returns the first element encountered in stream
      // order, which is the active(true) state because Collections.sort is stable.
      // This is UNDEFINED BEHAVIOUR – the result could change with a different JDK.
      assertThat(result)
          .as("Current JDK returns first element on tie → active wins (undefined behaviour)")
          .isTrue();
    }

    // EDGE CASE: Could this really happen in real RF2 data? Two states with the same timestamp but
    // different active flags?
    @Test
    @DisplayName(
        "Should return inactive when duplicate timestamps exist and first state is inactive")
    void shouldHandleDuplicateTimestampsFirstInactive() {
      // GIVEN – reversed order: inactive first, then active
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, false));
      edge.addState(new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.isActive(20240101L);

      // THEN
      // Stable sort preserves insertion order → inactive(false) is first in the list.
      // max() on equal timestamps returns the first element → inactive wins.
      assertThat(result)
          .as("Current JDK returns first element on tie → inactive wins (undefined behaviour)")
          .isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // addState()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("addState() Tests")
  class AddStateTests {

    @Test
    @DisplayName("Should maintain chronological order after adding states")
    void shouldMaintainChronologicalOrder() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240601L, true));

      // WHEN – add an earlier state
      edge.addState(new RelationshipState(20240101L, false));

      // THEN – the earlier state should affect queries at its timestamp
      assertThat(edge.isActive(20240101L))
          .as("Earlier added state should be queryable at its timestamp")
          .isFalse();
      assertThat(edge.isActive(20240601L))
          .as("Original state should still be queryable at its timestamp")
          .isTrue();
    }

    @Test
    @DisplayName("Should support adding many states without errors")
    void shouldSupportManyStates() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20200101L, true));

      // WHEN – add alternating states for 5 years
      for (int year = 2021; year <= 2025; year++) {
        boolean active = (year % 2 == 0);
        edge.addState(new RelationshipState(year * 10000L + 101L, active));
      }

      // THEN – check a few known points
      assertThat(edge.isActive(20200601L)).as("2020 (initial active)").isTrue();
      assertThat(edge.isActive(20210601L)).as("2021 (odd year = inactive)").isFalse();
      assertThat(edge.isActive(20220601L)).as("2022 (even year = active)").isTrue();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // seal()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("seal() Tests")
  class SealTests {

    @Test
    @DisplayName("Should prevent addState() after seal()")
    void shouldPreventAddStateAfterSeal() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));
      edge.seal();

      RelationshipState otherState = new RelationshipState(20240601L, false);

      // WHEN / THEN
      assertThatThrownBy(() -> edge.addState(otherState))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(EDGE_ID);
    }

    @Test
    @DisplayName("Should preserve state queries after seal()")
    void shouldPreserveStateQueriesAfterSeal() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));
      edge.addState(new RelationshipState(20240601L, false));
      edge.seal();

      // WHEN / THEN
      assertThat(edge.isActive(20240301L))
          .as("Should still return active between first and second state after seal")
          .isTrue();
      assertThat(edge.isActive(20240701L))
          .as("Should still return inactive after deactivation after seal")
          .isFalse();
    }

    @Test
    @DisplayName("Should be idempotent — calling seal() twice does not throw")
    void shouldBeIdempotent() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      edge.seal();
      edge.seal();

      // THEN
      assertThat(edge.isActive(20240101L))
          .as("Edge should still be queryable after double seal")
          .isTrue();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // hasModuleId()
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("hasModuleId() Tests")
  class HasModuleIdTests {

    @Test
    @DisplayName("Should return true for matching module id")
    void shouldReturnTrueForMatchingModuleId() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.hasModuleId(MODULE_CORE);

      // THEN
      assertThat(result).as("Should match the module id set in constructor").isTrue();
    }

    @Test
    @DisplayName("Should return false for non-matching module id")
    void shouldReturnFalseForNonMatchingModuleId() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.hasModuleId(MODULE_GERMAN);

      // THEN
      assertThat(result).as("Should not match a different module id").isFalse();
    }

    @Test
    @DisplayName("Should return false for empty string module id")
    void shouldReturnFalseForEmptyModuleId() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(EDGE_ID, MODULE_CORE, new RelationshipState(20240101L, true));

      // WHEN
      boolean result = edge.hasModuleId("");

      // THEN
      assertThat(result).as("Empty string should not match any module id").isFalse();
    }
  }
}
