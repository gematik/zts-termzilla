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
package de.gematik.zts.termzilla.terminologies.snomed.provider;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.TERMINOLOGY_CANONICAL_NAME;

import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;

/**
 * Utility class providing conversion methods between SNOMED CT internal model types and their
 * corresponding FHIR representations.
 */
public class AdapterHelper {

  private AdapterHelper() {
    // Private constructor to prevent instantiation
  }

  /**
   * Converts a {@link SnomedReleaseVersion} to a FHIR-compatible version string.
   *
   * <p>The resulting string follows the format {@code <canonical>/<moduleId>/version/<date>}, where
   * {@code canonical} is the SNOMED CT canonical URL ({@value
   * SnomedConstants#TERMINOLOGY_CANONICAL_NAME}).
   *
   * @param snomedReleaseVersion the SNOMED CT release version to convert
   * @return a FHIR version string identifying the given release
   */
  public static String convertSnomedReleaseVersionToFhirVersion(
      SnomedReleaseVersion snomedReleaseVersion) {
    return TERMINOLOGY_CANONICAL_NAME
        + "/"
        + snomedReleaseVersion.getModuleId()
        + "/version/"
        + snomedReleaseVersion.getDate();
  }
}
