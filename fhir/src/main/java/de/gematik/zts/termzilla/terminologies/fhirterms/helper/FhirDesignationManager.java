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
package de.gematik.zts.termzilla.terminologies.fhirterms.helper;

import de.gematik.zts.termzilla.common.helper.DesignationManager;
import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.Designation.DesignationSource;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Selects the most appropriate display {@link FhirDesignation} from a collected set of designations
 * based on a prioritized language selection strategy.
 *
 * <p>Designations are added from different sources ({@link Designation.DesignationSource#VS},
 * {@link Designation.DesignationSource#CS}, {@link Designation.DesignationSource#CSS}) and this
 * manager resolves the best matching display value according to the following priority chain:
 *
 * <ol>
 *   <li>{@code displayLanguage} parameter (strict — returns {@code null} if no match)
 *   <li>{@code Accept-Language} header (with fallback to next level)
 *   <li>System default language
 *   <li>ValueSet language
 * </ol>
 *
 * <p>Within each level, designations from ValueSet sources are preferred over CodeSystem sources.
 * Designations with no {@code use} are suitable for general display under FHIR R4; when a use is
 * present, it must be {@code display} from the HL7 designation-usage CodeSystem.
 */
@Slf4j
public class FhirDesignationManager extends DesignationManager<FhirDesignation> {

  /** {@inheritDoc} */
  @Override
  protected FhirDesignation buildVsDesignation(CodeType language, Coding use, String value) {
    return FhirDesignation.builder()
        .source(Designation.DesignationSource.VS)
        .language(language)
        .use(use)
        .value(value)
        .build();
  }

  /**
   * {@inheritDoc}
   *
   * <p>For FHIR packages, designations are selected based on the HL7 {@code
   * designation-usage#display} use coding, regardless of source.
   */
  @Override
  protected FhirDesignation findDisplayForLanguage(
      Designation.DesignationSource source, String language) {
    return getFullDesignationList().stream()
        .filter(d -> d.getSource() == source)
        .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
        .filter(this::hasDisplayUse)
        .findFirst()
        .orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  protected FhirDesignation findDisplayWithoutLanguage(Designation.DesignationSource source) {
    return getFullDesignationList().stream()
        .filter(d -> d.getSource() == source)
        .filter(
            d ->
                d.getLanguage() == null
                    || d.getLanguage().getValue() == null
                    || d.getLanguage().getValue().isBlank())
        .filter(this::hasDisplayUse)
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks whether the designation is suitable for general display: FHIR R4 treats an absent
   * {@code use} as general display, while a present use must be {@code display} from the HL7
   * designation-usage CodeSystem.
   *
   * @param d the designation to check
   * @return {@code true} if the designation has no use or has a display use coding
   */
  private boolean hasDisplayUse(FhirDesignation d) {
    return d.getUse() == null
        || d.getUse().isEmpty()
        || (DESIGNATION_USAGE_SYSTEM.equals(d.getUse().getSystem())
            && DISPLAY_USE_CODE.equals(d.getUse().getCode()));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Applies a four-level fallback chain to find the best available designation regardless of
   * language:
   *
   * <ol>
   *   <li>VS-sourced designation with {@code designation-usage#display} use coding
   *   <li>CS-sourced designation with {@code designation-usage#display} use coding
   *   <li>Any VS-sourced designation (regardless of use coding)
   *   <li>Any CS-sourced designation (regardless of use coding)
   * </ol>
   *
   * <p>Returns {@code null} if no designation exists at all.
   */
  @Override
  protected FhirDesignation getDisplayFallback() {

    FhirDesignation fallbackDesignation;

    // First try to find a fallback designation from the ValueSet source (any language)
    fallbackDesignation =
        getFullDesignationList().stream()
            .filter(d -> d.getSource() == DesignationSource.VS)
            .filter(this::hasDisplayUse)
            .findFirst()
            .orElse(null);

    // Then try a supplement (CSS) display designation
    if (fallbackDesignation == null) {
      fallbackDesignation =
          getFullDesignationList().stream()
              .filter(d -> d.getSource() == DesignationSource.CSS)
              .filter(this::hasDisplayUse)
              .findFirst()
              .orElse(null);
    }

    // If no ValueSet designation is found, try to find a fallback from the CodeSystem source (any
    // language)
    if (fallbackDesignation == null) {
      fallbackDesignation =
          getFullDesignationList().stream()
              .filter(d -> d.getSource() == DesignationSource.CS)
              .filter(this::hasDisplayUse)
              .findFirst()
              .orElse(null);
    }

    // If still no fallback designation is found, return any designation from the ValueSet source
    // (any language, any use)
    if (fallbackDesignation == null) {
      fallbackDesignation =
          getFullDesignationList().stream()
              .filter(d -> d.getSource() == DesignationSource.VS)
              .findFirst()
              .orElse(null);
    }

    // Then any supplement (CSS) designation
    if (fallbackDesignation == null) {
      fallbackDesignation =
          getFullDesignationList().stream()
              .filter(d -> d.getSource() == DesignationSource.CSS)
              .findFirst()
              .orElse(null);
    }

    // If still no fallback designation is found, return any designation from the CodeSystem source
    // (any language, any use)
    if (fallbackDesignation == null) {
      fallbackDesignation =
          getFullDesignationList().stream()
              .filter(d -> d.getSource() == DesignationSource.CS)
              .findFirst()
              .orElse(null);
    }

    return fallbackDesignation;
  }

  /**
   * Converts a {@link FhirDesignation} into a FHIR {@link
   * ValueSet.ConceptReferenceDesignationComponent} suitable for inclusion in a ValueSet expansion.
   *
   * @param designation the designation to convert
   * @return a new {@link ValueSet.ConceptReferenceDesignationComponent} with language, use, and
   *     value copied from the designation
   */
  public static ValueSet.ConceptReferenceDesignationComponent toDesignationComponent(
      FhirDesignation designation) {
    return new ValueSet.ConceptReferenceDesignationComponent()
        .setLanguage(designation.getLanguage() != null ? designation.getLanguage().getCode() : null)
        .setUse(designation.getUse())
        .setValue(designation.getValue());
  }

  /**
   * Collects designations from a FHIR {@link CodeSystem.ConceptDefinitionComponent}.
   *
   * <p>Adds the concept's display value as a designation with HL7 {@code designation-usage#display}
   * use coding, if present. Additionally, all explicit {@link
   * CodeSystem.ConceptDefinitionDesignationComponent} entries are added. Designations without an
   * explicit language fall back to {@code csLanguage}. All resulting designations are sourced as
   * {@link Designation.DesignationSource#CS}.
   *
   * @param concept the FHIR CodeSystem concept definition to extract designations from
   * @param csLanguage the fallback language tag applied when a designation has no explicit
   *     language, typically derived from the CodeSystem's content language
   */
  public void collectDesignationsFromFhirConcept(
      CodeSystem.ConceptDefinitionComponent concept, String csLanguage) {

    // Add CS display as a designation with display use
    if (concept.hasDisplay()) {
      this.addDesignation(
          FhirDesignation.builder()
              .source(Designation.DesignationSource.CS)
              .language(new CodeType(csLanguage))
              .value(concept.getDisplay())
              .build());
    }

    // Add all CS concept designations
    for (CodeSystem.ConceptDefinitionDesignationComponent conceptDesignation :
        concept.getDesignation()) {
      this.addDesignation(
          FhirDesignation.builder()
              .source(Designation.DesignationSource.CS)
              .language(
                  conceptDesignation.hasLanguage()
                      ? conceptDesignation.getLanguageElement()
                      : new CodeType(csLanguage))
              .use(conceptDesignation.getUse())
              .value(conceptDesignation.getValue())
              .build());
    }
  }
}
