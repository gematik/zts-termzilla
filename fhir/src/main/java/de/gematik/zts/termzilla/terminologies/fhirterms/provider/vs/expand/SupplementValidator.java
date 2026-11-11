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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;

/**
 * Validates CodeSystem supplements referenced by a ValueSet expansion.
 *
 * <p>A supplement is a CodeSystem with {@code content = supplement} that enriches an existing base
 * CodeSystem with additional designations or properties. A supplement must never change ValueSet
 * membership. This validator enforces the structural rules for a supplement to be applicable to a
 * base CodeSystem used by an expansion.
 */
public final class SupplementValidator {

  private SupplementValidator() {
    // Utility class
  }

  /**
   * Validates that a CodeSystem is a structurally valid supplement applicable to the given base
   * CodeSystem.
   *
   * @param supplement the candidate supplement CodeSystem (must not be {@code null})
   * @param baseCodeSystem the base CodeSystem actually used by the expansion (must not be {@code
   *     null})
   * @throws InvalidRequestException if the supplement is invalid or not applicable
   */
  public static void validateSupplement(CodeSystem supplement, CodeSystem baseCodeSystem) {
    if (supplement == null) {
      throw new InvalidRequestException("The referenced supplement resource is not a CodeSystem.");
    }

    if (!supplement.hasContent() || supplement.getContent() != CodeSystemContentMode.SUPPLEMENT) {
      throw new InvalidRequestException(
          "The CodeSystem '"
              + supplement.getUrl()
              + "' is not a supplement (content != 'supplement').");
    }

    // must declare which CodeSystem it supplements
    if (!supplement.hasSupplements()) {
      throw new InvalidRequestException(
          "The supplement '"
              + supplement.getUrl()
              + "' does not declare a base CodeSystem ('supplements' is missing).");
    }

    // Split the supplements canonical into URL and optional version part.
    String supplementsRef = supplement.getSupplements();
    String supplementsUrl =
        supplementsRef.contains("|")
            ? supplementsRef.substring(0, supplementsRef.indexOf('|'))
            : supplementsRef;
    String supplementsVersion =
        supplementsRef.contains("|")
            ? supplementsRef.substring(supplementsRef.indexOf('|') + 1)
            : null;

    // must target the base CodeSystem actually used
    String baseUrl = baseCodeSystem.getUrl();
    if (!supplementsUrl.equals(baseUrl)) {
      throw new InvalidRequestException(
          "The supplement '"
              + supplement.getUrl()
              + "' targets the base CodeSystem '"
              + supplementsUrl
              + "' which does not match the expanded CodeSystem '"
              + baseUrl
              + "'.");
    }

    // if the supplement declares a base version, it must match the expanded version
    String baseVersion = baseCodeSystem.getVersion();
    if (supplementsVersion != null
        && !supplementsVersion.isEmpty()
        && baseVersion != null
        && !supplementsVersion.equals(baseVersion)) {
      throw new InvalidRequestException(
          "The supplement '"
              + supplement.getUrl()
              + "' targets base version '"
              + supplementsVersion
              + "' which does not match the expanded base version '"
              + baseVersion
              + "'.");
    }

    // every code in the supplement must exist in the base CodeSystem
    for (CodeSystem.ConceptDefinitionComponent concept : supplement.getConcept()) {
      validateSupplementConceptCodes(concept, baseCodeSystem, supplement.getUrl());
    }
  }

  /**
   * Recursively validates that all concept codes defined by a supplement exist in the base
   * CodeSystem.
   *
   * @param concept the supplement concept to validate
   * @param baseCodeSystem the base CodeSystem
   * @param supplementUrl the supplement URL for error messages
   * @throws InvalidRequestException if a code is unknown to the base CodeSystem
   */
  private static void validateSupplementConceptCodes(
      CodeSystem.ConceptDefinitionComponent concept,
      CodeSystem baseCodeSystem,
      String supplementUrl) {
    String code = concept.getCode();
    boolean exists;
    try {
      CodeSystemHelper.findCodeInCodeSystem(baseCodeSystem, code);
      exists = true;
    } catch (Exception _) {
      exists = false;
    }
    if (!exists) {
      throw new InvalidRequestException(
          "The supplement '"
              + supplementUrl
              + "' contains the code '"
              + code
              + "' which is not present in the base CodeSystem '"
              + baseCodeSystem.getUrl()
              + "'.");
    }
    for (CodeSystem.ConceptDefinitionComponent child : concept.getConcept()) {
      validateSupplementConceptCodes(child, baseCodeSystem, supplementUrl);
    }
  }

  /**
   * Validates that a CodeSystem used as an independent include/exclude system is not a supplement.
   *
   * <p>A supplement cannot be expanded on its own.
   *
   * @param codeSystem the CodeSystem referenced by an include/exclude
   * @throws InvalidRequestException if the CodeSystem is a supplement
   */
  public static void failIfSupplementUsedAsSystem(CodeSystem codeSystem) {
    if (codeSystem != null
        && codeSystem.hasContent()
        && codeSystem.getContent() == CodeSystemContentMode.SUPPLEMENT) {
      throw new InvalidRequestException(
          "The CodeSystem '"
              + codeSystem.getUrl()
              + "' is a supplement and cannot be used as an independent include/exclude system.");
    }
  }
}
