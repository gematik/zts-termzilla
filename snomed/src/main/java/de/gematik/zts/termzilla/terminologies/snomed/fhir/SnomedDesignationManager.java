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
package de.gematik.zts.termzilla.terminologies.snomed.fhir;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_PREFERRED;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_SYNONYM;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.TERMINOLOGY_CANONICAL_NAME;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.formatExpressionProcessingDisabledMessage;
import static de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants.getDefaultLanguageCodeFromEditionIdAndLanguageTypeRefSetId;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.common.helper.DesignationManager;
import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedLanguage;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ValueSet;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Manages SNOMED CT designation selection for display purposes during ValueSet expansion.
 *
 * <p>This manager collects {@link SnomedDesignation} instances from both CodeSystem (CS) and
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
 *   <li>English fallback — last resort, attempts to find an English designation
 * </ol>
 *
 * <p>Within each priority level, VS-sourced designations with HL7 {@code designation-usage#display}
 * coding are preferred over CS-sourced designations. For CS designations, only preferred synonyms
 * are eligible for display selection (FSN and acceptable terms are excluded).
 *
 * <p>Language matching leverages the {@code LanguageRegistry} to expand base languages to their
 * regional variants (e.g. {@code "de"} → {@code "de-DE"}, {@code "de-AT"}, {@code "de-CH"}) and to
 * resolve regional variants back to their base language for fallback.
 *
 * @see SnomedDesignation
 * @see LanguageSelectionParameter
 */
@Slf4j
public class SnomedDesignationManager extends DesignationManager<SnomedDesignation> {

  /** {@inheritDoc} */
  @Override
  protected SnomedDesignation buildVsDesignation(CodeType language, Coding use, String value) {
    return SnomedDesignation.builder()
        .source(Designation.DesignationSource.VS)
        .language(language)
        .use(use)
        .value(value)
        .build();
  }

  /**
   * Falls back to English when the standard priority chain yields no result.
   *
   * @return an English designation, or {@code null} if none exists
   */
  @Override
  protected SnomedDesignation getDisplayFallback() {
    return resolveDisplay(LanguageParameter.parse("en"));
  }

  /**
   * Selects SNOMED CT designations by matching their {@code context} or {@code role} against the
   * given parameter value.
   *
   * <p>Supports two formats:
   *
   * <ul>
   *   <li>{@code http://snomed.info/sct|<code>} — system must match the SNOMED CT canonical URL
   *   <li>{@code <code>} (no pipe) — matched directly against context and role values
   * </ul>
   *
   * <p>A designation is included if its {@code context} or {@code role} equals the extracted code.
   *
   * @param paramValue the raw designation parameter value
   * @return designations matching by context or role, or an empty list if not applicable
   */
  @Override
  protected List<SnomedDesignation> selectDesignationsByCustomCriteria(String paramValue) {
    String code = extractSnomedCode(paramValue);
    if (code == null) {
      return Collections.emptyList();
    }

    return getFullDesignationList().stream()
        .filter(d -> code.equals(d.getContext()) || code.equals(d.getRole()))
        .toList();
  }

  /**
   * Extracts the SNOMED CT code from a designation parameter value.
   *
   * <p>For pipe-containing values, the system part must equal the SNOMED CT canonical URL ({@code
   * http://snomed.info/sct}); otherwise {@code null} is returned. For plain values without a pipe,
   * the value itself is returned as-is.
   *
   * @param paramValue the raw designation parameter value
   * @return the extracted code, or {@code null} if the value contains a pipe with a non-SNOMED
   *     system
   */
  private String extractSnomedCode(String paramValue) {
    if (paramValue.contains("|")) {
      String[] parts = paramValue.split("\\|", -1);
      if (parts.length == 2 && TERMINOLOGY_CANONICAL_NAME.equals(parts[0]) && !parts[1].isBlank()) {
        return parts[1];
      }
      return null;
    }
    return paramValue;
  }

  /**
   * {@inheritDoc}
   *
   * <p>For SNOMED CT, VS-sourced designations with HL7 {@code designation-usage#display} coding are
   * checked first. For CS-sourced designations, only preferred synonyms are eligible (FSN and
   * acceptable terms are excluded).
   */
  @Override
  protected SnomedDesignation findDisplayForLanguage(
      Designation.DesignationSource source, String language) {
    if (source == Designation.DesignationSource.VS) {
      return getFullDesignationList().stream()
          .filter(d -> d.getSource() == Designation.DesignationSource.VS)
          .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
          .filter(this::hasDisplayUse)
          .findFirst()
          .orElse(null);
    }
    // CS: only preferred synonyms are eligible for display
    return getFullDesignationList().stream()
        .filter(d -> d.getSource() == Designation.DesignationSource.CS)
        .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
        .filter(d -> d.getType() != null && d.getType().equals(SC_SYNONYM))
        .filter(d -> d.getRole() != null && d.getRole().equals(SC_PREFERRED))
        .findFirst()
        .orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  protected SnomedDesignation findDisplayWithoutLanguage(Designation.DesignationSource source) {
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
   * Checks whether a designation carries the HL7 {@code designation-usage#display} use coding.
   *
   * @param d the designation to check
   * @return {@code true} if the designation has display use coding, {@code false} otherwise
   */
  private boolean hasDisplayUse(SnomedDesignation d) {
    return d.getUse() != null
        && DESIGNATION_USAGE_SYSTEM.equals(d.getUse().getSystem())
        && DISPLAY_USE_CODE.equals(d.getUse().getCode());
  }

  /**
   * Converts a {@link SnomedDesignation} into a FHIR {@link
   * ValueSet.ConceptReferenceDesignationComponent} suitable for inclusion in a ValueSet expansion.
   *
   * <p>When {@code includeContextExtensionInDesignations} is {@code true}, the component is
   * enriched with a {@code designation-use-context} extension containing SNOMED-specific metadata
   * (context, role, and type) as nested extensions with codings from the SNOMED CT system.
   *
   * @param designation the designation to convert
   * @param includeContextExtensionInDesignations whether to add the {@code designation-use-context}
   *     extension with context (language reference set), role (acceptability), and type
   *     (description type) information
   * @return a new {@link ValueSet.ConceptReferenceDesignationComponent} with language, use, and
   *     value copied from the designation
   */
  public static ValueSet.ConceptReferenceDesignationComponent toDesignationComponent(
      SnomedDesignation designation, boolean includeContextExtensionInDesignations) {

    ValueSet.ConceptReferenceDesignationComponent conceptDesignation =
        new ValueSet.ConceptReferenceDesignationComponent()
            .setValue(designation.getValue())
            .setLanguage(
                designation.getLanguage() != null ? designation.getLanguage().getCode() : null)
            .setUse(designation.getUse());

    if (includeContextExtensionInDesignations) {
      Extension contextExtension = conceptDesignation.addExtension();
      contextExtension.setUrl(
          "http://snomed.info/fhir/StructureDefinition/designation-use-context");
      contextExtension.addExtension(
          "context", new Coding(TERMINOLOGY_CANONICAL_NAME, designation.getContext(), null));
      contextExtension.addExtension(
          "role", new Coding(TERMINOLOGY_CANONICAL_NAME, designation.getRole(), null));
      contextExtension.addExtension(
          "type", new Coding(TERMINOLOGY_CANONICAL_NAME, designation.getType(), null));
    }

    return conceptDesignation;
  }

  /**
   * Collects designations from a SNOMED CT Compositional Grammar expression and registers them in
   * this manager.
   *
   * <p>Adds two designations:
   *
   * <ol>
   *   <li>A human-readable term string derived via {@link
   *       DemisSCGProcessor#toTermString},
   *       carrying HL7 {@code designation-usage#display} use coding
   *   <li>The raw normalized expression string without use coding
   * </ol>
   *
   * <p>Both designations are sourced from {@link Designation.DesignationSource#CS}.
   *
   * @param expression the parsed SCG expression to derive designations from
   * @param releaseVersion the SNOMED CT release version used for term resolution
   * @param snomedTerminologyManager the terminology manager used to resolve the expression to a
   *     term string
   * @throws InactiveConceptException if a concept referenced by the expression is inactive
   * @throws UnknownConceptException if a concept referenced by the expression is unknown
   * @throws UnsupportedTerminologyException if the terminology version is not supported
   */
  public void collectDesignationsFromSnomedExpression(
      Expression expression,
      SnomedReleaseVersion releaseVersion,
      SnomedTerminologyManager snomedTerminologyManager)
      throws InactiveConceptException, UnknownConceptException, UnsupportedTerminologyException {

    if (!snomedTerminologyManager.isExpressionProcessingEnabled()) {
      throw new InvalidRequestException(
          formatExpressionProcessingDisabledMessage(
              expression.toString(), snomedTerminologyManager.isRestrictedMode()));
    }

    String designationString =
        snomedTerminologyManager.getScgProcessor().toTermString(expression, releaseVersion);

    this.getFullDesignationList()
        .add(
            SnomedDesignation.builder()
                .source(Designation.DesignationSource.CS)
                .use(new Coding(DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, null))
                .value(designationString)
                .build());

    this.getFullDesignationList()
        .add(
            SnomedDesignation.builder()
                .source(Designation.DesignationSource.CS)
                .value(expression.toString())
                .build());
  }

  /**
   * Collects all active SNOMED CT descriptions from the given concept and registers them as {@link
   * SnomedDesignation} instances in this manager.
   *
   * <p>Each active {@link SnomedConceptDescription} is processed for every active {@link
   * SnomedLanguage} entry it belongs to. The effective language tag is determined by combining the
   * description's own language code with the more specific tag that can be derived from the
   * language reference set membership (e.g. {@code "de-DE"} instead of the generic {@code "de"}).
   * If the refset-derived tag starts with the description's language code it takes precedence;
   * otherwise the description's language code is used as-is.
   *
   * <p>The designation's {@code use} coding is set to the description's type identifier within the
   * SNOMED CT system, so callers can distinguish Fully Specified Names (FSN) from synonyms. The
   * language reference set identifier is stored as the designation's {@code context}, the
   * acceptability identifier as its {@code role}, and the type identifier as its {@code type}.
   *
   * @param concept the SNOMED concept whose descriptions are to be collected
   */
  public void collectDesignationsFromSnomedConcept(SnomedConcept concept) {

    // Add CS designations from SNOMED concept descriptions. Each description can appear in
    // multiple language refsets (e.g. en-GB and en-US), each with its own acceptability level.
    for (SnomedConceptDescription description : concept.getSnomedConceptDescriptionList()) {
      if (!description.isActive()) {
        continue;
      }
      for (SnomedLanguage language : description.getLanguageList()) {
        if (language.isActive()) {
          this.getFullDesignationList().add(toDesignation(description, language));
        }
      }
    }
  }

  /**
   * Creates the designation for a description in the context of one language refset.
   *
   * @param description the active description
   * @param language the active language refset membership of the description
   * @return the designation carrying term, language, use and refset context
   */
  private static SnomedDesignation toDesignation(
      SnomedConceptDescription description, SnomedLanguage language) {
    return SnomedDesignation.builder()
        .source(Designation.DesignationSource.CS)
        .language(new CodeType(resolveLanguageTag(description, language)))
        .use(new Coding(SnomedConstants.TERMINOLOGY_CANONICAL_NAME, description.getTypeId(), null))
        .value(description.getTerm())
        .context(language.getRefsetId())
        .role(language.getAcceptabilityId())
        .type(description.getTypeId())
        .caseSignificanceId(description.getCaseSignificanceId())
        .build();
  }

  /**
   * Determines the language tag of a description in the context of one language refset.
   *
   * @param description the description providing the general language code
   * @param language the language refset membership providing a possibly more specific tag
   * @return the refset language tag if it refines the description language, otherwise the
   *     description language
   */
  private static String resolveLanguageTag(
      SnomedConceptDescription description, SnomedLanguage language) {
    String descriptionLanguageTag = description.getLanguageCode();

    // Wir versuchen aus den Refset-Informationen der description den passenden Sprachcode
    // zu ermitteln. Dieser kann spezifischer sein als der allgemeine Sprachcode der
    // description, da uns die RefSets einen weiteren Kontext liefern, z.B. 'de-DE' statt
    // nur 'de'.
    String refSetLanguageTag =
        getDefaultLanguageCodeFromEditionIdAndLanguageTypeRefSetId(
            language.getModuleId(), language.getRefsetId());

    return refSetLanguageTag != null && refSetLanguageTag.startsWith(descriptionLanguageTag)
        ? refSetLanguageTag
        : descriptionLanguageTag;
  }
}
