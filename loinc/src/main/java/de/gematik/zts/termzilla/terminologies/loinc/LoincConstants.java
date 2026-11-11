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
package de.gematik.zts.termzilla.terminologies.loinc;

import java.util.Map;

public class LoincConstants {

  private LoincConstants() {
    // private constructor to prevent instantiation
  }

  public static final String LOINC_LANGUAGE = "en-US";
  public static final String TERMINOLOGY_CANONICAL_NAME = "http://loinc.org";
  public static final String LOINC_DESIGNATION_LONG_COMMON_NAME = "LONG_COMMON_NAME";
  public static final String LOINC_DESIGNATION_FULLY_SPECIFIED_NAME = "FullySpecifiedName";
  public static final String LOINC_DESIGNATION_CONSUMER_NAME = "ConsumerName";
  public static final String LOINC_DESIGNATION_SHORT_NAME = "ShortName";
  public static final String LOINC_DESIGNATION_PART_NAME = "PartName";
  public static final String LOINC_DESIGNATION_PARTDISPLAY_NAME = "PartDisplayName";
  public static final String LOINC_DESIGNATION_DISPLAYTEXT = "DisplayText";
  public static final String LOINC_DESIGNATION_ANSWERLIST_NAME = "AnswerListName";

  public static final String LOINC_PARTTYPENAME_COMPONENT = "COMPONENT";
  public static final String LOINC_PARTTYPENAME_PROPERTY = "PROPERTY";
  public static final String LOINC_PARTTYPENAME_TIME = "TIME";
  public static final String LOINC_PARTTYPENAME_SYSTEM = "SYSTEM";
  public static final String LOINC_PARTTYPENAME_SCALE = "SCALE";
  public static final String LOINC_PARTTYPENAME_NOT_IN_PART_FILE = "NOT_IN_PART_FILE";

  public static final String LOINC_STATUSCODE_ACTIVE = "ACTIVE";

  public static final Map<String, String> LANGUAGE_TO_LV_PATTERN_MAP =
      Map.ofEntries(
          Map.entry("de-DE", "^deDE15LinguisticVariant\\.csv"),
          Map.entry("de-AT", "^deAT24LinguisticVariant\\.csv"),
          Map.entry("el-GR", "^elGR17LinguisticVariant\\.csv"),
          Map.entry("es-AR", "^esAR7LinguisticVariant\\.csv"),
          Map.entry("es-ES", "^esES12LinguisticVariant\\.csv"),
          Map.entry("es-MX", "^esMX28LinguisticVariant\\.csv"),
          Map.entry("et-EE", "^etEE10LinguisticVariant\\.csv"),
          Map.entry("fr-BE", "^frBE23LinguisticVariant\\.csv"),
          Map.entry("fr-CA", "^frCA8LinguisticVariant\\.csv"),
          Map.entry("fr-FR", "^frFR18LinguisticVariant\\.csv"),
          Map.entry("it-IT", "^itIT16LinguisticVariant\\.csv"),
          Map.entry("ko-KR", "^koKR13LinguisticVariant\\.csv"),
          Map.entry("nl-NL", "^nlNL22LinguisticVariant\\.csv"),
          Map.entry("pl-PL", "^plPL29LinguisticVariant\\.csv"),
          Map.entry("pt-BR", "^ptBR11LinguisticVariant\\.csv"),
          Map.entry("ru-RU", "^ruRU20LinguisticVariant\\.csv"),
          Map.entry("tr-TR", "^trTR19LinguisticVariant\\.csv"),
          Map.entry("uk-UA", "^ukUA30LinguisticVariant\\.csv"),
          Map.entry("zh-CN", "^zhCN5LinguisticVariant\\.csv"));
}
