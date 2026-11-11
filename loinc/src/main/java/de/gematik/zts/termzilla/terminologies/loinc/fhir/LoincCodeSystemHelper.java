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
package de.gematik.zts.termzilla.terminologies.loinc.fhir;

import java.util.UUID;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;

public class LoincCodeSystemHelper {

  private LoincCodeSystemHelper() {
    // private constructor to prevent instantiation
  }

  public static CodeSystem getLoincCodeSystem(String version) {
    CodeSystem cs = new CodeSystem();
    cs.setId(generateId("http://loinc.org", version));
    cs.setUrl("http://loinc.org");
    cs.addIdentifier(
        new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:oid:2.16.840.1.113883.6.1"));
    cs.setName("LOINC");
    cs.setVersion(version);
    cs.setTitle("LOINC Code System");
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setExperimental(false);
    cs.setPublisher("Regenstrief Institute, Inc.");
    cs.setDescription(
        "LOINC is a freely available international standard for tests, measurements, and observations");
    cs.setCopyright(
        "This material contains content from LOINC (http://loinc.org). LOINC is copyright © 1995-2023, Regenstrief Institute, Inc. and the Logical Observation Identifiers Names and Codes (LOINC) Committee and is available at no cost under the license at http://loinc.org/license. LOINC® is a registered United States trademark of Regenstrief Institute, Inc.");
    cs.setCaseSensitive(true);
    cs.setContent(CodeSystem.CodeSystemContentMode.NOTPRESENT);
    cs.setCompositional(false);
    cs.setVersionNeeded(false);

    // TODO: change to NameBasedUUID
    cs.setId(UUID.randomUUID().toString());
    return cs;
  }

  private static String generateId(String url, String version) {

    if (url != null) {
      // if name and version are present concat both, else just use name
      String composedId = version == null ? url : url.concat("-").concat(version);
      // create a named UUID
      return UUID.nameUUIDFromBytes(composedId.getBytes()).toString();
    }
    // neither url nor version present, use random uuid
    else {
      return UUID.randomUUID().toString();
    }
  }
}
