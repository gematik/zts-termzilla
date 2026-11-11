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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Repräsentiert eine LOINC-Bezeichnung. Eine LOINC-Bezeichnung besitzt grundsätzlich eine Sprache
 * und einen Typ. Der Typ kann z.B. 'FULLY_SPECIFIED_NAME' oder 'SHORT_NAME' sein. Er ist dabei
 * jedoch von der Art des LOINC-Konzeptes (LOINC-Term, LOINC-Part, LOINC-AnswerList,
 * LOINC-AnswerOption) abhängig. Nicht alle Typen sind für alle Konzeptarten verfügbar.
 */
public class LoincDesignation {

  private String language;
  private String type;
  private String designation;

  @Override
  public String toString() {
    return "LoincDesignation{"
        + "language='"
        + language
        + '\''
        + ", type='"
        + type
        + '\''
        + ", designation='"
        + designation
        + '\''
        + '}';
  }
}
