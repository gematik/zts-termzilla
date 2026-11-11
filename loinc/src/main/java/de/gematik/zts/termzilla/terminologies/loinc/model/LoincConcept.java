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

import static de.gematik.zts.termzilla.terminologies.loinc.LoincConstants.LOINC_LANGUAGE;
import static de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincDesignationManager.resolveDefaultDesignationType;

import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;


/**
 * Repräsentiert ein LOINC-Konzept. Ein LOINC-Konzept kann ein LOINC-Term, ein LOINC-Part, eine
 * LOINC-AnswerList oder eine LOINC-AnswerOption sein. Die Objektstruktur ähnelt ein wenig der
 * FHIR-Struktur für Konzepte, ist jedoch nicht identisch.
 */
@Getter
@Setter
public class LoincConcept {

  private String code;
  private LoincVersion version;
  private String definition;
  private boolean isActive;
  private List<String> parentList = new ArrayList<>();
  private List<String> childrenList = new ArrayList<>();
  private MultiValuedMap<String, String> propertyMap = new ArrayListValuedHashMap<>();
  private List<LoincDesignation> designationList = new ArrayList<>();

  /**
   * Gibt die 'Standardbezeichnung' für den angegebenen Sprachcode zurück. Sofern keine Sprache
   * angegeben wird, wird die LOINC-Standardsprache (en-US) verwendet.
   *
   * @param language Sprachcode (z.B. 'de-DE', 'en-US')
   * @return Die 'Standardbezeichnung' für den angegebenen Sprachcode
   */
  public LoincDesignation getDefaultDesignation(String language) {

    // Sofern keine Sprache angegeben wurde, wird der Wert aus der LoincTable verwendet
    if (language == null) {
      language = LoincConstants.LOINC_LANGUAGE;
    }

    // Hier müssen wir zwischen LoincTerms, Parts, AnswerLists und AnswerOptions unterscheiden, da
    // jeder eine andere Art von Bezeichnung als Standard hat
    String defaultDesignationType = resolveDefaultDesignationType(code);
    return getDefaultDesignationForDesignationType(language, defaultDesignationType);
  }

  /**
   * Gibt die 'Standardbezeichnung' für den angegebenen Sprachcode und den angegebenen
   * Bezeichnungstyp zurück. Sofern keine Sprache angegeben wird, wird die LOINC-Standardsprache
   * (en-US) verwendet.
   *
   * @param language        Sprachcode (z.B. 'de-DE', 'en-US')
   * @param designationType Bezeichnungstyp (z.B. 'LONG_COMMON_NAME') Der Bezeichnungstyp ist dabei
   *                        auch stark abhängig von der Art des Konzeptes (LoincTerm, LoincPart,
   *                        LoincAnswerList ...)
   * @return Die 'Standardbezeichnung' für den angegebenen Sprachcode und Designationstyp.
   */
  private LoincDesignation getDefaultDesignationForDesignationType(
      String language, String designationType) {

    // Ermitteln einer Sprachenliste für den angegebenen Sprachcode
    Set<String> languageSet = LanguageRegistry.getLanguageVariants(language);

    // Ermitteln der Bezeichnung für den angegebenen Bezeichnungstyp und die angegebene Sprache. Im
    // LOINC-Kontext sollte es immer nur eine Bezeichnung für einen Bezeichnungstyp und eine Sprache
    // geben. Entsprechend können wir den ersten Hit verwenden.
    if (languageSet != null && !languageSet.isEmpty()) {

      for (String languageListEntry : languageSet) {
        Optional<LoincDesignation> designation =
            designationList.stream()
                .filter(d -> d.getLanguage().equals(languageListEntry))
                .filter(d -> d.getType().equals(designationType))
                .findFirst();

        if (designation.isPresent()) {
          return designation.get();
        }
      }
    }

    // Für den Fall, dass wir keine Übersetzung für die angegebene Sprache finden, nehmen wir die
    // englische Bezeichnung für den entsprechenden Bezeichnungstyp
    Optional<LoincDesignation> defaultDesignation =
        designationList.stream()
            .filter(d -> d.getLanguage().equals(LOINC_LANGUAGE))
            .filter(d -> d.getType().equals(designationType))
            .findFirst();

    // Wurde ein Wert gefunden, geben wir diesen zurück. Andernfalls wird 'null' zurückgegeben. Dies
    // sollte aber eigentlich nicht passieren, da wir immer eine englische Bezeichnung haben
    // sollten.
    return defaultDesignation.orElse(null);
  }

  @Override
  public String toString() {
    return "LoincConcept{"
        + "code='"
        + code
        + '\''
        + ", version='"
        + version
        + '\''
        + ", definition='"
        + definition
        + '\''
        + ", defaultDesignation='"
        + getDefaultDesignation(null)
        + '\''
        + ", isActive="
        + isActive
        + ", parentList="
        + parentList
        + ", childrenList="
        + childrenList
        + ", propertyMapSize="
        + propertyMap.size()
        + ", designationListSize="
        + designationList.size()
        + '}';
  }
}
