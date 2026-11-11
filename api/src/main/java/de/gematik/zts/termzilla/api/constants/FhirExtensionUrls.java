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
package de.gematik.zts.termzilla.api.constants;

/**
 * Canonical URLs of FHIR extensions used throughout the terminology server.
 *
 * <p>All constants refer to standard FHIR R4 extension definitions unless noted otherwise.
 */
public class FhirExtensionUrls {

  private FhirExtensionUrls() {
    // Utility class, prevent instantiation
  }

  /**
   * Extension URL for the <a href="https://hl7.org/fhir/R4/extension-data-absent-reason.html">
   * data-absent-reason</a> extension, used to indicate why a value is missing in a resource.
   */
  public static final String EXTENSION_DATA_ABSENT_REASON =
      "http://hl7.org/fhir/StructureDefinition/data-absent-reason";

  /**
   * Extension URL for the <a
   * href="https://build.fhir.org/ig/HL7/fhir-extensions/StructureDefinition-resource-effectivePeriod.html">
   * resource-effectivePeriod</a> extension, used to indicate the period during which the resource
   * content was or is planned to be effective.
   */
  public static final String EXTENSION_RESOURCE_EFFECTIVE_PERIOD =
      "http://hl7.org/fhir/StructureDefinition/resource-effectivePeriod";

  /**
   * Extension URL for the <a
   * href="https://hl7.org/fhir/R4/extension-valueset-supplement.html">valueset-supplement</a>
   * extension, used to reference a CodeSystem supplement that enriches the expansion with
   * additional designations or properties without changing membership.
   */
  public static final String EXTENSION_VALUESET_SUPPLEMENT =
      "http://hl7.org/fhir/StructureDefinition/valueset-supplement";
}
