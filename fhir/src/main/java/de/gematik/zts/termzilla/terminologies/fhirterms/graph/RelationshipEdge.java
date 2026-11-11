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

import lombok.Getter;
import org.jgrapht.graph.DefaultEdge;

/**
 * Represents a directed edge in a FHIR terminology graph, defining parent-child relationships
 * between concepts. Extends {@link DefaultEdge} to provide typed relationship information.
 */
public class RelationshipEdge extends DefaultEdge {

  @Getter private final RelationshipType relationshipType;

  @Getter private final String sourceCode;

  @Getter private final String targetCode;

  /**
   * Creates a new relationship edge between two concepts.
   *
   * @param relationshipType the type of relationship (PARENT or CHILD)
   * @param sourceCode the source concept code
   * @param targetCode the target concept code
   */
  public RelationshipEdge(RelationshipType relationshipType, String sourceCode, String targetCode) {
    this.relationshipType = relationshipType;
    this.sourceCode = sourceCode;
    this.targetCode = targetCode;
  }

  /**
   * Returns a string representation of the edge showing the relationship.
   *
   * @return formatted string in the form "(source relationshipType target)"
   */
  @Override
  public String toString() {
    return "(" + getSource() + " " + relationshipType + " " + getTarget() + ")";
  }

  /**
   * Returns a detailed description of the relationship including codes and type.
   *
   * @return detailed relationship description
   */
  public String toDetailedString() {
    return String.format(
        "RelationshipEdge[source='%s', target='%s', type=%s]",
        sourceCode, targetCode, relationshipType);
  }

  /** Defines the type of relationship between concepts in a terminology hierarchy. */
  public enum RelationshipType {
    /** Indicates a child relationship (parent → child) */
    CHILD("child"),

    /** Indicates a parent relationship (child → parent) */
    PARENT("parent");

    @Getter private final String name;

    /**
     * Creates a relationship type with the specified name.
     *
     * @param name the string representation of the relationship type
     */
    RelationshipType(String name) {
      this.name = name;
    }

    /**
     * Returns the string representation of the relationship type.
     *
     * @return the relationship type name
     */
    @Override
    public String toString() {
      return this.name;
    }
  }
}
