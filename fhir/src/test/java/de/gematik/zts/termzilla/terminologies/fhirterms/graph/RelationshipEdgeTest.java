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
package de.gematik.zts.termzilla.terminologies.fhirterms.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link RelationshipEdge}. */
@DisplayName("RelationshipEdge Tests")
class RelationshipEdgeTest {

  private static final String SOURCE_CODE = "SOURCE";
  private static final String TARGET_CODE = "TARGET";

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create edge with PARENT relationship type")
    void shouldCreateEdgeWithParentType() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.PARENT;

      // WHEN
      RelationshipEdge edge = new RelationshipEdge(type, SOURCE_CODE, TARGET_CODE);

      // THEN
      assertThat(edge.getRelationshipType()).isEqualTo(type);
      assertThat(edge.getSourceCode()).isEqualTo(SOURCE_CODE);
      assertThat(edge.getTargetCode()).isEqualTo(TARGET_CODE);
    }

    @Test
    @DisplayName("Should create edge with CHILD relationship type")
    void shouldCreateEdgeWithChildType() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.CHILD;

      // WHEN
      RelationshipEdge edge = new RelationshipEdge(type, SOURCE_CODE, TARGET_CODE);

      // THEN
      assertThat(edge.getRelationshipType()).isEqualTo(type);
      assertThat(edge.getSourceCode()).isEqualTo(SOURCE_CODE);
      assertThat(edge.getTargetCode()).isEqualTo(TARGET_CODE);
    }

    @ParameterizedTest
    @EnumSource(RelationshipEdge.RelationshipType.class)
    @DisplayName("Should create edge for all relationship types")
    void shouldCreateEdgeForAllTypes(RelationshipEdge.RelationshipType type) {
      // GIVEN & WHEN
      RelationshipEdge edge = new RelationshipEdge(type, SOURCE_CODE, TARGET_CODE);

      // THEN
      assertThat(edge.getRelationshipType()).isEqualTo(type);
    }
  }

  @Nested
  @DisplayName("toDetailedString Tests")
  class ToDetailedStringTests {

    @Test
    @DisplayName("Should provide detailed string with all information")
    void shouldProvideDetailedString() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(RelationshipEdge.RelationshipType.PARENT, SOURCE_CODE, TARGET_CODE);

      // WHEN
      String result = edge.toDetailedString();

      // THEN
      assertThat(result)
          .contains("RelationshipEdge")
          .contains("source='SOURCE'")
          .contains("target='TARGET'")
          .contains("type=parent");
    }

    @Test
    @DisplayName("Should format CHILD relationship in detailed string")
    void shouldFormatChildInDetailedString() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(RelationshipEdge.RelationshipType.CHILD, "A", "B");

      // WHEN
      String result = edge.toDetailedString();

      // THEN
      assertThat(result).isEqualTo("RelationshipEdge[source='A', target='B', type=child]");
    }
  }

  @Nested
  @DisplayName("RelationshipType Tests")
  class RelationshipTypeTests {

    @Test
    @DisplayName("Should have correct string representation for PARENT")
    void shouldHaveCorrectStringForParent() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.PARENT;

      // WHEN
      String result = type.toString();

      // THEN
      assertThat(result).isEqualTo("parent");
    }

    @Test
    @DisplayName("Should have correct string representation for CHILD")
    void shouldHaveCorrectStringForChild() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.CHILD;

      // WHEN
      String result = type.toString();

      // THEN
      assertThat(result).isEqualTo("child");
    }

    @Test
    @DisplayName("Should return name from getName method for PARENT")
    void shouldReturnNameForParent() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.PARENT;

      // WHEN
      String result = type.getName();

      // THEN
      assertThat(result).isEqualTo("parent");
    }

    @Test
    @DisplayName("Should return name from getName method for CHILD")
    void shouldReturnNameForChild() {
      // GIVEN
      RelationshipEdge.RelationshipType type = RelationshipEdge.RelationshipType.CHILD;

      // WHEN
      String result = type.getName();

      // THEN
      assertThat(result).isEqualTo("child");
    }

    @Test
    @DisplayName("Should have exactly two relationship types")
    void shouldHaveTwoTypes() {
      // GIVEN & WHEN
      RelationshipEdge.RelationshipType[] types = RelationshipEdge.RelationshipType.values();

      // THEN
      assertThat(types)
          .hasSize(2)
          .containsExactlyInAnyOrder(
              RelationshipEdge.RelationshipType.PARENT, RelationshipEdge.RelationshipType.CHILD);
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("Should return correct source code")
    void shouldReturnSourceCode() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(RelationshipEdge.RelationshipType.CHILD, "SOURCE123", TARGET_CODE);

      // WHEN
      String result = edge.getSourceCode();

      // THEN
      assertThat(result).isEqualTo("SOURCE123");
    }

    @Test
    @DisplayName("Should return correct target code")
    void shouldReturnTargetCode() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(RelationshipEdge.RelationshipType.CHILD, SOURCE_CODE, "TARGET456");

      // WHEN
      String result = edge.getTargetCode();

      // THEN
      assertThat(result).isEqualTo("TARGET456");
    }

    @Test
    @DisplayName("Should return correct relationship type")
    void shouldReturnRelationshipType() {
      // GIVEN
      RelationshipEdge edge =
          new RelationshipEdge(RelationshipEdge.RelationshipType.PARENT, SOURCE_CODE, TARGET_CODE);

      // WHEN
      RelationshipEdge.RelationshipType result = edge.getRelationshipType();

      // THEN
      assertThat(result).isEqualTo(RelationshipEdge.RelationshipType.PARENT);
    }
  }
}
