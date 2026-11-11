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
package de.gematik.zts.termzilla.api.interfaces;

import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object representing the core identification of a terminology concept within a
 * given scope.
 *
 * <p>This class serves as a lightweight identifier for concepts during ValueSet expansion
 * operations. It contains the minimal information needed to uniquely identify a concept within a
 * specific CodeSystem version and track its activation status.
 *
 * <p>Instances are naturally ordered by system, then version, then code, making them suitable for
 * use in sorted collections like {@link java.util.TreeSet}.
 *
 * <p>Two instances are considered equal if all four fields match: system, version, code, and active
 * status.
 *
 * @see VsExpandPartResult
 */
@Builder
@Getter
public class ConceptBaseInfo implements Comparable<ConceptBaseInfo> {

  /** The canonical URL of the CodeSystem containing this concept. */
  private String system;

  /** The version of the CodeSystem. */
  private String version;

  /** The concept code within the CodeSystem. */
  private String code;

  /** Whether the concept is active in the CodeSystem. */
  private boolean active;

  /**
   * Compares this object with another for equality based on all fields.
   *
   * @param o the object to compare with
   * @return {@code true} if all fields (system, version, code, active) are equal
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptBaseInfo that = (ConceptBaseInfo) o;
    return active == that.active
        && Objects.equals(system, that.system)
        && Objects.equals(version, that.version)
        && Objects.equals(code, that.code);
  }

  /**
   * Returns a hash code based on all fields.
   *
   * @return hash code computed from system, version, code, and active status
   */
  @Override
  public int hashCode() {
    return Objects.hash(system, version, code, active);
  }

  /**
   * Compares this concept with another for ordering.
   *
   * <p>The natural ordering is: first by system, then by version, then by code. The active status
   * is not considered in the ordering.
   *
   * @param o the concept to compare with
   * @return negative if this precedes {@code o}, positive if this follows {@code o}, zero if equal
   */
  @Override
  public int compareTo(ConceptBaseInfo o) {
    if (this.system.equals(o.system)) {
      if (this.version.equals(o.version)) {
        return this.code.compareTo(o.code);
      } else {
        return this.version.compareTo(o.version);
      }
    } else {
      return this.system.compareTo(o.system);
    }
  }
}
