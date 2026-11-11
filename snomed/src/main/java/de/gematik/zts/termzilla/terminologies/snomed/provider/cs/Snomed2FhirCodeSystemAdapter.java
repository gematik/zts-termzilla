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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs;

import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.provider.AdapterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;

@Slf4j
public class Snomed2FhirCodeSystemAdapter {

  private Snomed2FhirCodeSystemAdapter() {
    // private constructor to prevent instantiation of this utility class
  }

  private static final String CS_URL = SnomedConstants.TERMINOLOGY_CANONICAL_NAME;
  private static final String CS_NAME = SnomedConstants.TERMINOLOGY_DISPLAY_NAME;
  private static final String CS_TITLE = "SNOMED CT";
  private static final String CS_PUBLISHER = "SNOMED International";
  private static final String CS_DESCRIPTION =
      "SNOMED CT is a clinical terminology with comprehensive coverage of clinical concepts. It is owned and "
          + "distributed around the world by SNOMED International. SNOMED CT provides the core general terminology "
          + "for electronic health records.";
  private static final String CS_COPYRIGHT =
      "This material includes SNOMED Clinical Terms® (SNOMED CT®) which is used by permission of the International "
          + "Health Terminology Standards Development Organisation (IHTSDO). All rights reserved. SNOMED CT®, was "
          + "originally created by The College of American Pathologists. “SNOMED” and “SNOMED CT” are registered "
          + "trademarks of the IHTSDO.";

  private static final boolean IS_EXPERIMENTAL = true;
  private static final boolean IS_CASESENSITIVE = true;
  private static final boolean IS_COMPOSITIONAL = true;
  private static final boolean IS_VERSIONNEEDED = false;

  /**
   * Returns a list of all supported SNOMED CT Release Versions as FHIR CodeSystem resources.
   *
   * @param snomedHelper SnomedReleaseTerminologyHelper
   * @return List of CodeSystem resources
   */
  public static List<CodeSystem> calculateSupportedCodeSystems(Snomed snomedHelper) {
    List<CodeSystem> codeSystemList = new ArrayList<>();
    for (SnomedReleaseVersion releaseVersion : snomedHelper.getSupportedSnomedReleaseVersions()) {
      String fhirVersion = AdapterHelper.convertSnomedReleaseVersionToFhirVersion(releaseVersion);
      codeSystemList.add(getSnomedCodeSystem(fhirVersion));
    }
    return codeSystemList;
  }

  /**
   * Generates a FHIR CodeSystem resource for a given SNOMED CT Release Version.
   *
   * @param version FHIR version of SNOMED CT Release Version
   * @return CodeSystem resource
   */
  public static CodeSystem getSnomedCodeSystem(String version) {
    CodeSystem cs = new CodeSystem();
    cs.setUrl(CS_URL);
    cs.setName(CS_NAME);
    cs.setVersion(version);
    cs.setTitle(CS_TITLE);
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setExperimental(IS_EXPERIMENTAL);
    cs.setPublisher(CS_PUBLISHER);
    cs.setDescription(CS_DESCRIPTION);
    cs.setCopyright(CS_COPYRIGHT);
    cs.setCaseSensitive(IS_CASESENSITIVE);
    cs.setContent(CodeSystem.CodeSystemContentMode.NOTPRESENT);
    cs.setCompositional(IS_COMPOSITIONAL);
    cs.setVersionNeeded(IS_VERSIONNEEDED);

    // TODO: change to NameBasedUUID
    cs.setId(UUID.randomUUID().toString());
    return cs;
  }
}
