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
package de.gematik.zts.termzilla.terminologies.loinc.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * Repräsentiert eine konkrete LOINC-Version und zugehörige Metadaten.
 */
@Getter
@SuperBuilder
@EqualsAndHashCode
public class LoincVersion implements Comparable<LoincVersion> {

  private int versionNumber;
  private String versionString;
  private String versionDate;

  // Wir bieten eine eigene Sortierung an, da die Module in einer bestimmten Reihenfolge,
  // die sich nicht nur am effectiveTime-Feld orientiert, angeordnet werden müssen.
  @Override
  public int compareTo(LoincVersion otherVersion) {
    return Integer.compare(versionNumber, otherVersion.versionNumber);
  }

  @Override
  public String toString() {
    return "LoincVersion{"
        + "versionNumber="
        + versionNumber
        + ", versionString='"
        + versionString
        + '\''
        + ", versionDate='"
        + versionDate
        + '\''
        + '}';
  }
}