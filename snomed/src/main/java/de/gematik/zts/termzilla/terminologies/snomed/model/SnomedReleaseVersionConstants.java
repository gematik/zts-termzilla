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
package de.gematik.zts.termzilla.terminologies.snomed.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central constant definitions for SNOMED release version handling.
 *
 * <p>This class groups module identifiers, language/reference-set mappings, validation patterns,
 * and release-specific allow/block lists used across SNOMED version parsing and lookup logic.
 */
public class SnomedReleaseVersionConstants {

  /** Prevents instantiation of this constants utility class. */
  private SnomedReleaseVersionConstants() {
    // private constructor to prevent instantiation of this Constants class
  }

  public static final String SNOMED_VERSION_PREFIX = "http://snomed.info/sct/";

  public static final String SNOMED_MODEL_COMPONENT_MODULEID = "900000000000012004";
  public static final String SNOMED_CORE_MODULEID = "900000000000207008";
  public static final String SNOMED_ICD10_MAPPING_MODULEID = "449080006";
  public static final String SNOMED_GERMANY_EDITION_MODULEID = "11000274103";

  public static final String LANGUAGE_EN = "en";
  public static final String LANGUAGE_DE = "de";
  // Default fallback language when no language is defined for a module
  public static final String LANGUAGE_DEFAULT = LANGUAGE_EN;
  public static final String LANGUAGE_ALL = "*";

  public static final String LANGUAGE_DE_AT = "de-AT";
  public static final String LANGUAGE_DE_CH = "de-CH";
  public static final String LANGUAGE_EN_GB = "en-GB";
  public static final String LANGUAGE_EN_US = "en-US";
  public static final String LANGUAGE_DE_DE = "de-DE";

  // Defines the explicitly supported modules and thus the supported SNOMED CT releases
  public static final Set<String> SUPPORTED_MODULES =
      Set.of(SNOMED_CORE_MODULEID, SNOMED_GERMANY_EDITION_MODULEID);

  // Maps supported modules to their default language codes
  public static final Map<String, String> MODULE_LANGUAGE_MAP =
      Map.of(
          SNOMED_CORE_MODULEID, LANGUAGE_EN,
          SNOMED_GERMANY_EDITION_MODULEID, LANGUAGE_DE_DE);

  public static final String DE_DE = "31000274107";
  public static final String DE_AT = "21000234103";
  public static final String DE_CH = "2041000195100";
  public static final String EN_GB = "900000000000508004";
  public static final String EN_US = "900000000000509007";

  public static final Map<String, String> REFSET_LANGUAGE_MAP =
      Map.of(
          DE_DE, LANGUAGE_DE_DE,
          DE_AT, LANGUAGE_DE_AT,
          DE_CH, LANGUAGE_DE_CH,
          EN_GB, LANGUAGE_EN_GB,
          EN_US, LANGUAGE_EN_US);

  // Maps language codes to their corresponding language reference set IDs
  public static final Map<String, List<String>> LANGUAGE_REFSET_MAP =
      Map.of(
          LANGUAGE_ALL,
          List.of(DE_DE, DE_AT, DE_CH, EN_GB, EN_US),
          LANGUAGE_EN,
          List.of(EN_GB, EN_US),
          LANGUAGE_EN_GB,
          List.of(EN_GB),
          LANGUAGE_EN_US,
          List.of(EN_US),
          LANGUAGE_DE,
          List.of(DE_DE, DE_AT, DE_CH),
          LANGUAGE_DE_DE,
          List.of(DE_DE),
          LANGUAGE_DE_AT,
          List.of(DE_AT),
          LANGUAGE_DE_CH,
          List.of(DE_CH));

  // Maps language codes (including complex codes like de-DE) to their base language codes
  public static final Map<String, String> LANGUAGE_LANGUAGE_SHORT_MAP =
      Map.of(
          LANGUAGE_EN, LANGUAGE_EN,
          LANGUAGE_EN_GB, LANGUAGE_EN,
          LANGUAGE_EN_US, LANGUAGE_EN,
          LANGUAGE_DE, LANGUAGE_DE,
          LANGUAGE_DE_DE, LANGUAGE_DE,
          LANGUAGE_DE_AT, LANGUAGE_DE,
          LANGUAGE_DE_CH, LANGUAGE_DE);

  // Module order matters: modules are evaluated in this order when computing the default version.
  // Higher index means higher priority.
  public static final List<String> MODULE_ORDER =
      List.of(SNOMED_CORE_MODULEID, SNOMED_GERMANY_EDITION_MODULEID);

  public static final String FHIR_VERSION_PATTERN =
      "^http://snomed\\.info/sct/([0-9]+)/version/([0-9]{8})$";
  public static final String MODULEID_PATTERN = "^[0-9]+$";
  public static final String DATE_PATTERN = "^[0-9]{8}$";

  public static final Map<String, String> MODULE_NAME_MAP =
      Map.of(
          SNOMED_MODEL_COMPONENT_MODULEID, "SNOMED CT model component",
          SNOMED_CORE_MODULEID, "SNOMED CT core",
          SNOMED_ICD10_MAPPING_MODULEID, "SNOMED CT to ICD-10 rule-based mapping module",
          SNOMED_GERMANY_EDITION_MODULEID, "German module");

  // Defines versions that are not supported because they appear in RF2 files but are not official
  // releases (a.k.a. "ghost versions"). The vendor producing RF2 files for BfArM confirmed that
  // future releases will no longer contain ghost versions due to a changed synchronisation process
  // with Austria and Switzerland. This explicit blocklist is therefore sufficient to handle legacy.
  public static final Map<String, Set<String>> INVALID_VERSIONS =
      Map.of(SNOMED_GERMANY_EDITION_MODULEID, Set.of("20230919", "20240115", "20240711"));

  // Regexp-based filter for unsupported versions that appear in RF2 files but are not official
  public static final Map<String, String> INVALID_VERSIONS_REGEXP =
      Map.of(SNOMED_GERMANY_EDITION_MODULEID, "^(?!.*(20\\d\\d0515)|(20\\d\\d1115)).*$");

  /**
   * Resolves a default language code from edition id and/or language reference set id.
   *
   * <p>Resolution order is:
   *
   * <ol>
   *   <li>Use {@code refsetId} via {@link #REFSET_LANGUAGE_MAP} when available.
   *   <li>Fallback to {@code editionId} via {@link #MODULE_LANGUAGE_MAP}.
   * </ol>
   *
   * @param editionId SNOMED edition/module id
   * @param refsetId SNOMED language type reference set id
   * @return resolved language code, or {@code null} if no mapping exists
   */
  public static String getDefaultLanguageCodeFromEditionIdAndLanguageTypeRefSetId(
      String editionId, String refsetId) {
    String languageCode = null;
    if (refsetId != null) {
      languageCode = REFSET_LANGUAGE_MAP.getOrDefault(refsetId, null);
    }
    if (languageCode == null && editionId != null) {
      languageCode = MODULE_LANGUAGE_MAP.getOrDefault(editionId, null);
    }
    return languageCode;
  }
}
