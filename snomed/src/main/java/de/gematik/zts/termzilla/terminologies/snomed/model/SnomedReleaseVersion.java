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

import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.common.GenericVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/**
 * Represents a SNOMED release version identified by module id, effective date, and module
 * dependencies.
 *
 * <p>A release version combines three pieces of information: the primary module, its effective-time
 * date, and dependency module versions required for consistent lookups. The class also derives the
 * canonical SNOMED FHIR version string and defines a module-aware ordering strategy used when
 * versions from different modules are compared.
 */
@Getter
public class SnomedReleaseVersion extends GenericVersion {

  private final String moduleId;
  private final String date;
  private Map<String, String> dependencies;

  /**
   * Creates a SNOMED release version from module id, effective date, and dependency versions.
   *
   * <p>The constructor validates and normalizes these values through {@link
   * #createFhirVersionStringFromModuleIdAndDate(String, String)} before storing them.
   *
   * @param moduleId SNOMED module identifier
   * @param date release effective date in {@code yyyyMMdd} format
   * @param dependencies dependent module versions keyed by module id
   * @throws UnsupportedTerminologyException if module id or date is invalid or unsupported
   */
  public SnomedReleaseVersion(String moduleId, String date, Map<String, String> dependencies)
      throws UnsupportedTerminologyException {
    super(createFhirVersionStringFromModuleIdAndDate(moduleId, date));
    this.moduleId = moduleId;
    this.date = date;
    this.dependencies = dependencies;
  }

  /**
   * Returns all module versions relevant for release-specific processing.
   *
   * <p>The resulting map contains the release module itself and all dependency modules declared for
   * this release. Consumers use this structure to apply module-specific effective-time cutoffs
   * consistently across concepts, descriptions, and refset entries.
   *
   * @return map of relevant module ids to effective dates
   */
  public Map<String, String> getRelevantModuleVersions() {
    Map<String, String> relevantModuleVersions = new HashMap<>();
    relevantModuleVersions.put(moduleId, date);
    relevantModuleVersions.putAll(dependencies);
    return relevantModuleVersions;
  }

  /**
   * Returns the default language for this release version. If the release is not known, the default
   * language for the default module is returned.
   *
   * @return the default language for this version
   */
  public String getDefaultLanguage() {
    String language = SnomedReleaseVersionConstants.MODULE_LANGUAGE_MAP.get(moduleId);
    return Objects.requireNonNullElse(language, SnomedReleaseVersionConstants.LANGUAGE_DEFAULT);
  }

  /**
   * Returns a compact textual representation of this release version.
   *
   * @return string in {@code <moduleId>-<date>} format
   */
  @Override
  public String toString() {
    return getModuleId() + "-" + getDate();
  }

  /**
   * Compares release versions by module precedence and then by effective date.
   *
   * <p>Module precedence is defined by {@link SnomedReleaseVersionConstants#MODULE_ORDER}. If both
   * versions belong to the same module, ordering falls back to lexical date comparison in {@code
   * yyyyMMdd} format.
   *
   * @param o version to compare with
   * @return positive if this version is greater, negative if smaller, or zero if equal
   */
  @Override
  public int compareTo(GenericVersion o) {
    try {
      SnomedReleaseVersion otherVersion = (SnomedReleaseVersion) o;
      int otherVersionModuleIndex =
          SnomedReleaseVersionConstants.MODULE_ORDER.indexOf(otherVersion.getModuleId());
      int thisVersionModuleIndex =
          SnomedReleaseVersionConstants.MODULE_ORDER.indexOf(this.getModuleId());
      if (thisVersionModuleIndex > otherVersionModuleIndex) {
        return 1;
      } else if (thisVersionModuleIndex < otherVersionModuleIndex) {
        return -1;
      } else {
        return this.getDate().compareTo(otherVersion.getDate());
      }

    } catch (ClassCastException _) {
      return -1;
    }
  }

  // ------------------------------------------------------------------------

  /**
   * Creates the canonical SNOMED FHIR version string from module id and date.
   *
   * <p>The method validates both input values against SNOMED-specific patterns and verifies that
   * the module is part of the supported module set before generating the version URL fragment.
   *
   * @param moduleId SNOMED module id
   * @param date SNOMED effective date
   * @return FHIR version string
   * @throws UnsupportedTerminologyException if module id or date format is invalid, or module is
   *     not supported
   */
  private static String createFhirVersionStringFromModuleIdAndDate(String moduleId, String date)
      throws UnsupportedTerminologyException {
    if (!moduleId.matches(SnomedReleaseVersionConstants.MODULEID_PATTERN)
        || !date.matches(SnomedReleaseVersionConstants.DATE_PATTERN)) {
      throw new UnsupportedTerminologyException("Invalid module id: " + moduleId);
    }
    if (!SnomedReleaseVersionConstants.SUPPORTED_MODULES.contains(moduleId)) {
      throw new UnsupportedTerminologyException("Unsupported module id: " + moduleId);
    }
    return SnomedReleaseVersionConstants.SNOMED_VERSION_PREFIX + moduleId + "/version/" + date;
  }
}
