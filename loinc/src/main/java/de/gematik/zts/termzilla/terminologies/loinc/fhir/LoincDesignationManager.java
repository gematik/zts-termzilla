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

import de.gematik.zts.termzilla.common.helper.DesignationManager;
import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Manages LOINC designation selection for display purposes during ValueSet expansion.
 *
 * <p>This manager collects {@link LoincFhirDesignation} instances from both CodeSystem (CS) and
 * ValueSet (VS) sources and selects the most appropriate one for display based on a prioritized
 * language selection strategy.
 *
 * <p>The display selection follows a strict priority chain:
 *
 * <ol>
 *   <li>{@code displayLanguage} parameter — strict, returns {@code null} if no match is found
 *   <li>{@code Accept-Language} header — soft, falls through to lower priorities on mismatch
 *   <li>System default language — configured server-wide default
 *   <li>ValueSet language — language declared on the ValueSet resource
 *   <li>en-US fallback — last resort, LOINC standard language
 * </ol>
 *
 * <p>Within each priority level, VS-sourced designations with HL7 {@code designation-usage#display}
 * coding are preferred. For CS designations, only those matching the {@code defaultDesignationType}
 * (determined by the concept code prefix) are eligible for display selection.
 *
 * @see LoincFhirDesignation
 * @see DesignationManager
 */
@Slf4j
public class LoincDesignationManager extends DesignationManager<LoincFhirDesignation> {

  private final String defaultDesignationType;

  /**
   * Creates a new LOINC designation manager with the given default designation type.
   *
   * @param defaultDesignationType the LOINC designation type used for display selection from CS
   *     designations (e.g. {@code LONG_COMMON_NAME} for Terms, {@code PartName} for Parts)
   */
  public LoincDesignationManager(String defaultDesignationType) {
    this.defaultDesignationType = defaultDesignationType;
  }

  /**
   * Determines the default designation type for a LOINC concept based on its code prefix.
   *
   * @param code the LOINC concept code
   * @return the default designation type constant for the concept kind
   */
  public static String resolveDefaultDesignationType(String code) {
    if (code == null) {
      return LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME;
    } else if (code.startsWith("LP")) {
      return LoincConstants.LOINC_DESIGNATION_PART_NAME;
    } else if (code.startsWith("LA")) {
      return LoincConstants.LOINC_DESIGNATION_DISPLAYTEXT;
    } else if (code.startsWith("LL")) {
      return LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME;
    } else {
      return LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected LoincFhirDesignation buildVsDesignation(CodeType language, Coding use, String value) {
    return LoincFhirDesignation.builder()
        .source(Designation.DesignationSource.VS)
        .language(language)
        .use(use)
        .value(value)
        .build();
  }

  /**
   * Falls back to en-US (LOINC standard language) when the standard priority chain yields no
   * result.
   *
   * @return an en-US designation, or {@code null} if none exists
   */
  @Override
  protected LoincFhirDesignation getDisplayFallback() {
    return resolveDisplay(LanguageParameter.parse(LoincConstants.LOINC_LANGUAGE));
  }

  /**
   * {@inheritDoc}
   *
   * <p>For LOINC, VS-sourced designations with HL7 {@code designation-usage#display} coding are
   * checked first. For CS-sourced designations, only those matching the {@code
   * defaultDesignationType} are eligible for display selection.
   */
  @Override
  protected LoincFhirDesignation findDisplayForLanguage(
      Designation.DesignationSource source, String language) {
    if (source == Designation.DesignationSource.VS) {
      return getFullDesignationList().stream()
          .filter(d -> d.getSource() == Designation.DesignationSource.VS)
          .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
          .filter(this::hasDisplayUse)
          .findFirst()
          .orElse(null);
    }
    // CS: only designations matching the default designation type are eligible
    return getFullDesignationList().stream()
        .filter(d -> d.getSource() == Designation.DesignationSource.CS)
        .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
        .filter(d -> d.getType() != null && d.getType().equals(defaultDesignationType))
        .findFirst()
        .orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  protected LoincFhirDesignation findDisplayWithoutLanguage(Designation.DesignationSource source) {
    if (source == Designation.DesignationSource.VS) {
      return getFullDesignationList().stream()
          .filter(d -> d.getSource() == Designation.DesignationSource.VS)
          .filter(
              d ->
                  d.getLanguage() == null
                      || d.getLanguage().getValue() == null
                      || d.getLanguage().getValue().isBlank())
          .filter(this::hasDisplayUse)
          .findFirst()
          .orElse(null);
    }
    // CS: only designations matching the default designation type are eligible
    return getFullDesignationList().stream()
        .filter(d -> d.getSource() == Designation.DesignationSource.CS)
        .filter(
            d ->
                d.getLanguage() == null
                    || d.getLanguage().getValue() == null
                    || d.getLanguage().getValue().isBlank())
        .filter(d -> d.getType() != null && d.getType().equals(defaultDesignationType))
        .findFirst()
        .orElse(null);
  }

  /**
   * Checks whether a designation carries the HL7 {@code designation-usage#display} use coding.
   *
   * @param d the designation to check
   * @return {@code true} if the designation has display use coding, {@code false} otherwise
   */
  private boolean hasDisplayUse(LoincFhirDesignation d) {
    return d.getUse() != null
        && DESIGNATION_USAGE_SYSTEM.equals(d.getUse().getSystem())
        && DISPLAY_USE_CODE.equals(d.getUse().getCode());
  }

  /**
   * Converts a {@link LoincFhirDesignation} into a FHIR {@link
   * ValueSet.ConceptReferenceDesignationComponent} suitable for inclusion in a ValueSet expansion.
   *
   * @param designation the designation to convert
   * @return a new {@link ValueSet.ConceptReferenceDesignationComponent} with language, use, and
   *     value copied from the designation
   */
  public static ValueSet.ConceptReferenceDesignationComponent toDesignationComponent(
      LoincFhirDesignation designation) {
    return new ValueSet.ConceptReferenceDesignationComponent()
        .setLanguage(designation.getLanguage() != null ? designation.getLanguage().getCode() : null)
        .setUse(designation.getUse())
        .setValue(designation.getValue());
  }

  /**
   * Collects all designations from the given LOINC concept and registers them as {@link
   * LoincFhirDesignation} instances in this manager.
   *
   * <p>Each {@link LoincDesignation} from the concept's designation list is mapped to a CS-sourced
   * {@link LoincFhirDesignation}. The designation's language, type, and value are preserved; the
   * {@code use} coding is set to the LOINC system URL with the designation type as code (e.g.
   * {@code LONG_COMMON_NAME}, {@code PartName}).
   *
   * @param concept the LOINC concept whose designations are to be collected
   */
  public void collectDesignationsFromLoincConcept(LoincConcept concept) {

    for (LoincDesignation loincDesignation : concept.getDesignationList()) {
      this.addDesignation(
          LoincFhirDesignation.builder()
              .source(Designation.DesignationSource.CS)
              .language(new CodeType(loincDesignation.getLanguage()))
              .use(
                  new Coding()
                      .setSystem(LoincConstants.TERMINOLOGY_CANONICAL_NAME)
                      .setCode(loincDesignation.getType()))
              .value(loincDesignation.getDesignation())
              .type(loincDesignation.getType())
              .build());
    }
  }
}
