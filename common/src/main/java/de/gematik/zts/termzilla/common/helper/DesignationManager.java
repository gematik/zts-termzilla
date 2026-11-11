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
package de.gematik.zts.termzilla.common.helper;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;

/**
 * Abstract base class for terminology-specific designation managers.
 *
 * <p>Provides common functionality for collecting designations and selecting them based on FHIR
 * {@code $expand} designation parameter values. Subclasses implement terminology-specific display
 * selection logic via {@link #getDisplay(LanguageSelectionParameter)}.
 *
 * @param <D> the concrete designation type managed by this instance
 */
@Slf4j
public abstract class DesignationManager<D extends Designation> {

  public static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  public static final String DISPLAY_USE_CODE = "display";

  @Getter private final List<D> fullDesignationList = new ArrayList<>();

  /**
   * Adds a designation to the internal collection for subsequent display selection.
   *
   * @param designation the designation to add
   */
  public void addDesignation(D designation) {
    fullDesignationList.add(designation);
  }

  /**
   * Selects the most appropriate display designation based on the given language selection
   * parameters.
   *
   * <p>Evaluates the following priority chain and returns the first matching designation:
   *
   * <ol>
   *   <li>{@code displayLanguage} parameter — strict: returns the result (even {@code null})
   *       without falling through to lower levels
   *   <li>{@code Accept-Language} header — soft: falls through on mismatch
   *   <li>System default language — soft: falls through on mismatch
   *   <li>ValueSet language — soft: falls through on mismatch
   *   <li>{@link #getDisplayFallback()} — terminology-specific last resort
   * </ol>
   *
   * <p>Each level delegates to {@link #resolveDisplay(LanguageParameter)} which subclasses
   * implement with their terminology-specific lookup logic.
   *
   * @param lsp the language selection parameters defining the priority chain
   * @return the best matching display designation, or {@code null} if none could be resolved
   */
  public D getDisplay(LanguageSelectionParameter lsp) {

    D result;

    // Höchste Priorität hat der displayLanguage Parameter. Ist er gesetzt und wir finden keinen
    // passenden Display, dann geben wir null zurück. Im Ergebnis wird kein display angezeigt
    if (lsp.getDisplayLanguageParameter() != null) {
      return resolveDisplay(lsp.getDisplayLanguageParameter());
    }
    // Der Accept-Language-Header hat die zweit-höchste Priorität. Hier sind wir jedoch nicht so
    // strikt bei der Prüfung. Finden wir keinen passenden Display-Wert, haben wir einen Fallback
    // auf system default
    if (lsp.getAcceptLanguageHeader() != null) {
      result = resolveDisplay(lsp.getAcceptLanguageHeader());
      if (result != null) {
        return result;
      }
    }
    // Die Basiseinstellungen des Systems haben die dritt-höchste Priorität. Wird auch hier nichts
    // gefunden, folgt der Fallback auf die Sprache des ValueSets, sofern gesetzt
    if (lsp.getSystemDefaultLanguage() != null) {
      result = resolveDisplay(lsp.getSystemDefaultLanguage());
      if (result != null) {
        return result;
      }
    }
    // Fallback auf Sprache des ValueSets
    if (lsp.getVsLanguage() != null) {
      result = resolveDisplay(lsp.getVsLanguage());
      if (result != null) {
        return result;
      }
    }

    // Terminologiemodul-spezifischer Fallback (z.B. Englisch für SNOMED)
    return getDisplayFallback();
  }

  /**
   * Resolves a display designation for the language ranges defined in the given {@link
   * LanguageParameter}.
   *
   * <p>Iterates over prioritized language ranges, expanding each to a set of candidate language
   * tags via {@link LanguageRegistry}. For each expanded set, VS-sourced designations are checked
   * before CS-sourced designations. Falls back to designations without a language code when no
   * language-matched designation is found.
   *
   * <p>Subclasses provide terminology-specific matching logic through {@link
   * #findDisplayForLanguage(Designation.DesignationSource, String)} and {@link
   * #findDisplayWithoutLanguage(Designation.DesignationSource)}.
   *
   * @param languageParameter the parsed language parameter containing prioritized language ranges
   * @return the best matching designation, or {@code null} if none matched
   */
  protected D resolveDisplay(LanguageParameter languageParameter) {
    for (LanguageParameter.LanguageRange range : languageParameter.getRanges()) {
      Set<String> languageSet = expandLanguageTag(range.tag());
      final var foundDesignation = resolveDisplayForLanguage(languageSet);
      if (foundDesignation.isPresent()) {
        return foundDesignation.get();
      }
    }

    return resolveDisplayWithoutLanguage();
  }

  protected Optional<D> resolveDisplayForLanguage(Set<String> languageSet) {
    for (String language : languageSet) {
      D designation = findDisplayForLanguage(Designation.DesignationSource.VS, language);
      if (designation != null) {
        return Optional.of(designation);
      }
    }

    // Supplement (CSS) display designations rank between ValueSet and base CodeSystem
    // designations.
    for (String language : languageSet) {
      D designation = findDisplayForLanguage(Designation.DesignationSource.CSS, language);
      if (designation != null) {
        return Optional.of(designation);
      }
    }

    for (String language : languageSet) {
      D designation = findDisplayForLanguage(Designation.DesignationSource.CS, language);
      if (designation != null) {
        return Optional.of(designation);
      }
    }

    return Optional.empty();
  }

  protected D resolveDisplayWithoutLanguage() {
    D designation = findDisplayWithoutLanguage(Designation.DesignationSource.VS);
    if (designation != null) {
      return designation;
    }

    designation = findDisplayWithoutLanguage(Designation.DesignationSource.CSS);
    if (designation != null) {
      return designation;
    }

    return findDisplayWithoutLanguage(Designation.DesignationSource.CS);
  }

  /**
   * Provides a terminology-specific last-resort fallback when the standard priority chain yields no
   * result.
   *
   * <p>The default implementation returns {@code null}. Subclasses may override this to provide a
   * fallback strategy (e.g. SNOMED falls back to English).
   *
   * @return a fallback designation, or {@code null} if no fallback is available
   */
  protected D getDisplayFallback() {
    return null;
  }

  /**
   * Extension point for terminology-specific designation selection by custom criteria.
   *
   * <p>Called during {@link #selectDesignations(List)} for each designation parameter value, after
   * standard language, use-code, and system|code matching has been applied. Subclasses may override
   * this to handle parameter values with terminology-specific semantics (e.g. SNOMED expression
   * constraints, custom filter tokens).
   *
   * <p>The default implementation returns an empty list (no custom criteria recognized).
   *
   * @param paramValue the raw designation parameter value
   * @return designations matching the custom criteria, or an empty list if not applicable
   */
  protected List<D> selectDesignationsByCustomCriteria(String paramValue) {
    return Collections.emptyList();
  }

  /**
   * Finds a display designation matching the given source and language.
   *
   * <p>Subclasses implement terminology-specific matching logic (e.g. display-use coding for FHIR,
   * preferred synonyms for SNOMED CT CodeSystem designations).
   *
   * @param source the designation source to filter by ({@code VS} or {@code CS})
   * @param language the BCP 47 language tag to match against
   * @return the best matching designation, or {@code null} if none found
   */
  protected abstract D findDisplayForLanguage(
      Designation.DesignationSource source, String language);

  /**
   * Finds a display designation from the given source when no language-matched designation exists.
   *
   * <p>Considers designations with {@code null}, empty, or blank language codes as candidates.
   *
   * @param source the designation source to filter by ({@code VS} or {@code CS})
   * @return the best matching designation, or {@code null} if none found
   */
  protected abstract D findDisplayWithoutLanguage(Designation.DesignationSource source);

  /**
   * Expands a single language tag to the full set of candidate language tags for matching.
   *
   * <p>For base language tags (e.g. {@code "de"}), all known regional variants are included. For
   * regional variants (e.g. {@code "de-DE"}), the tag itself and its base language are included.
   *
   * @param languageTag the BCP 47 language tag to expand (e.g. {@code "de"}, {@code "de-DE"})
   * @return an ordered set of language tags to match against, preserving priority order
   */
  private Set<String> expandLanguageTag(String languageTag) {
    Set<String> languageSet = new LinkedHashSet<>();
    if (LanguageRegistry.getBaseLanguages().contains(languageTag)) {
      languageSet.addAll(LanguageRegistry.getLanguageVariants(languageTag));
    } else {
      languageSet.addAll(LanguageRegistry.getLanguageHierarchy(languageTag));
    }
    return languageSet;
  }

  /**
   * Selects designations for inclusion in a ValueSet expansion based on the {@code designation}
   * parameter values.
   *
   * <p>If the parameter list is empty, all collected designations are returned. Otherwise, each
   * parameter value is interpreted as either a language code, a use code, or a {@code system|code}
   * token filter, following the FHIR expand operation specification.
   *
   * @param designationParamValueList the designation filter values from the expand request (empty
   *     means include all)
   * @return the set of designations matching the filter criteria
   * @see <a href="https://hl7.org/fhir/R4/valueset-operation-expand.html">ValueSet $expand</a>
   */
  public Set<D> selectDesignations(List<String> designationParamValueList) {

    Set<D> selectedDesignationSet = new HashSet<>();

    if (designationParamValueList.isEmpty()) {
      // The designation parameter is not set, or the list is empty.
      // Default behavior: embed all available designations.
      selectedDesignationSet.addAll(fullDesignationList);
    } else {
      // The designation parameter is set and the list is not empty.
      // Default behavior: embed only designations that match the parameter values.
      for (String designationParamValue : designationParamValueList) {
        processDesignationParamValue(designationParamValue, selectedDesignationSet);
      }
    }

    return selectedDesignationSet;
  }

  /**
   * Processes one raw {@code designation} parameter value and accumulates matching designations.
   *
   * <p>Delegates to language/use-code handling for values without {@code |} and to
   * system|code-token handling otherwise. If token parsing fails, processing for this value is
   * aborted and no custom criteria are applied.
   *
   * @param designationParamValue the raw designation parameter value from the expand request
   * @param selectedDesignationSet the mutable result set receiving all matches for this value
   */
  private void processDesignationParamValue(
      String designationParamValue, Set<D> selectedDesignationSet) {

    // We distinguish two base cases: with/without '|'.
    // Base case 1: no '|' in the parameter value, i.e. either a language
    // or a use code (e.g. "display").
    if (!designationParamValue.contains("|")) {
      selectDesignationsByLanguageOrUseCode(designationParamValue, selectedDesignationSet);
    } else if (!selectDesignationsBySystemCodeToken(
        designationParamValue, selectedDesignationSet)) {
      return;
    }

    // Extension point: terminology-specific custom criteria
    selectedDesignationSet.addAll(selectDesignationsByCustomCriteria(designationParamValue));
  }

  /**
   * Selects designations for a parameter value interpreted as language or plain use code.
   *
   * <p>If the value is a supported language tag, language-based matching is applied. Independently,
   * the same value is evaluated as a use code via {@link #getDesignationsByUse(String, String)}.
   *
   * @param designationParamValue the raw designation parameter value without {@code |}
   * @param selectedDesignationSet the mutable result set receiving all matches
   */
  private void selectDesignationsByLanguageOrUseCode(
      String designationParamValue, Set<D> selectedDesignationSet) {

    // First, try selecting by language.
    if (LanguageRegistry.isSupported(designationParamValue)) {
      // We now know this is a valid language and ask the manager to return
      // matching designations.
      selectedDesignationSet.addAll(getDesignationsByLanguage(designationParamValue));
    }

    // Then check use codes.
    selectedDesignationSet.addAll(getDesignationsByUse(null, designationParamValue));
  }

  /**
   * Selects designations for a parameter value interpreted as {@code system|code} token.
   *
   * <p>Supported forms are {@code [system]|[code]}, {@code |[code]}, and {@code [system]|}. For
   * {@code |[code]}, only designations with a {@code use} coding that has no system and matching
   * code are selected.
   *
   * @param designationParamValue the raw designation parameter value containing {@code |}
   * @param selectedDesignationSet the mutable result set receiving all matches
   * @return {@code true} if parsing succeeded, otherwise {@code false}
   */
  private boolean selectDesignationsBySystemCodeToken(
      String designationParamValue, Set<D> selectedDesignationSet) {

    // Base case 2: there is a '|' in the parameter value, i.e. a
    // system|code combination. Possible variants:
    // [system]|[code] - both system and code must match
    // |[code]         - code matches, coding must not have a system property
    // [system]|       - system matches, code is ignored
    String[] parts = designationParamValue.split("\\|", -1);
    if (parts.length != 2) {
      log.warn("Invalid designation parameter value: {}", designationParamValue);
      return false;
    }

    String useSystem = parts[0].isBlank() ? null : parts[0];
    String useCode = parts[1].isBlank() ? null : parts[1];

    if (useSystem == null && useCode != null) {
      // |[code] - code matches, but the designation must not have a system property
      selectedDesignationSet.addAll(
          fullDesignationList.stream()
              .filter(d -> d.getUse() != null)
              .filter(d -> !d.getUse().hasSystem())
              .filter(d -> d.getUse().hasCode() && useCode.equals(d.getUse().getCode()))
              .toList());
    } else {
      // [system]|[code] or [system]| - delegate to getDesignationsByUse
      selectedDesignationSet.addAll(getDesignationsByUse(useSystem, useCode));
    }

    return true;
  }

  // ------------------------------

  /**
   * Returns all designations whose language matches the given language code.
   *
   * <p>Language matching is expanded via {@link LanguageRegistry}: for a base language like {@code
   * "de"}, all known regional variants (e.g. {@code "de-DE"}, {@code "de-AT"}, {@code "de-CH"}) are
   * included. For a regional variant like {@code "de-DE"}, the base language {@code "de"} is also
   * considered.
   *
   * @param languageCode the BCP 47 language tag to match (e.g. {@code "de"}, {@code "de-DE"})
   * @return all designations matching the expanded language set, in list order
   */
  private List<D> getDesignationsByLanguage(String languageCode) {
    Set<String> languageSet = expandLanguageTag(languageCode);

    return fullDesignationList.stream()
        .filter(
            d ->
                d.getLanguage() != null
                    && d.getLanguage().getValue() != null
                    && languageSet.contains(d.getLanguage().getValue()))
        .toList();
  }

  /**
   * Returns all designations whose language matches any of the given language codes.
   *
   * <p>Each code is expanded via BCP 47 hierarchy (see {@link #getDesignationsByLanguage}), and the
   * resulting sets are unioned. Order and uniqueness are preserved.
   *
   * @param languageCodes the BCP 47 language tags to match against
   * @return all matching designations, deduplicated, in list order
   */
  public List<D> getDesignationsByLanguages(Collection<String> languageCodes) {
    return languageCodes.stream()
        .flatMap(code -> getDesignationsByLanguage(code).stream())
        .distinct()
        .toList();
  }

  /**
   * Returns all designations whose {@code use} coding matches the given system and/or code.
   *
   * <p>The matching behavior depends on which parameters are provided:
   *
   * <ul>
   *   <li>Both {@code system} and {@code code} set — matches designations where both match
   *   <li>Only {@code system} set — matches designations with that system (any code)
   *   <li>Only {@code code} set — matches designations with that code (any system)
   * </ul>
   *
   * <p>Designations without a {@code use} coding are never returned.
   *
   * @param system the use coding system to match, or {@code null} to ignore
   * @param code the use coding code to match, or {@code null} to ignore
   * @return all designations matching the given use criteria, in list order
   */
  public List<D> getDesignationsByUse(String system, String code) {
    return fullDesignationList.stream()
        .filter(d -> d.getUse() != null)
        .filter(d -> matchesSystem(d, system))
        .filter(d -> matchesCode(d, code))
        .toList();
  }

  /**
   * Checks whether a designation's {@code use.system} matches the requested system.
   *
   * @param d the designation to evaluate
   * @param system the requested system, or {@code null} to skip system filtering
   * @return {@code true} if the designation matches the system criterion
   */
  private boolean matchesSystem(Designation d, String system) {
    return system == null || (d.getUse().hasSystem() && system.equals(d.getUse().getSystem()));
  }

  /**
   * Checks whether a designation's {@code use.code} matches the requested code.
   *
   * @param d the designation to evaluate
   * @param code the requested code, or {@code null} to skip code filtering
   * @return {@code true} if the designation matches the code criterion
   */
  private boolean matchesCode(Designation d, String code) {
    return code == null || (d.getUse().hasCode() && code.equals(d.getUse().getCode()));
  }

  /**
   * Creates a new VS-sourced designation with the given language, use coding, and value.
   *
   * <p>Implementations must set {@link Designation.DesignationSource#VS} as the source and populate
   * the remaining fields from the given parameters.
   *
   * @param language the BCP 47 language code element, or {@code null} if no language is available
   * @param use the use coding describing the designation role
   * @param value the designation text
   * @return a new terminology-specific VS designation
   */
  protected abstract D buildVsDesignation(CodeType language, Coding use, String value);

  /**
   * Collects VS-sourced designations from a collection of explicit {@link
   * ConceptReferenceComponent} entries and registers them in this manager.
   *
   * <p>For each concept reference, two kinds of designations are extracted:
   *
   * <ul>
   *   <li><b>Display value</b> — when the reference has a {@code display} element, it is added as a
   *       VS designation with {@code designation-usage#display} use coding. The language is taken
   *       from the {@link
   *       ResourceUserData#USERDATA_LANGUAGE}
   *       user-data attached to the concept reference.
   *   <li><b>Explicit designations</b> — when the reference has {@code designation} elements, each
   *       is added as a VS designation preserving its own use coding and language. If a designation
   *       element carries no language, the concept reference's user-data language is used as
   *       fallback.
   * </ul>
   *
   * @param explicitConceptReferences the concept references from which to extract designations
   */
  public void collectDesignationsFromConceptReferenceComponents(
      Collection<ConceptReferenceComponent> explicitConceptReferences) {

    for (ValueSet.ConceptReferenceComponent explicitConceptReference : explicitConceptReferences) {
      if (explicitConceptReference.hasDisplay()) {
        this.getFullDesignationList()
            .add(
                buildVsDesignation(
                    new CodeType((String) explicitConceptReference.getUserData(USERDATA_LANGUAGE)),
                    null,
                    explicitConceptReference.getDisplay()));
      }
      if (explicitConceptReference.hasDesignation()) {
        for (ValueSet.ConceptReferenceDesignationComponent designationComponent :
            explicitConceptReference.getDesignation()) {
          this.getFullDesignationList()
              .add(
                  buildVsDesignation(
                      designationComponent.hasLanguage()
                          ? designationComponent.getLanguageElement()
                          : new CodeType(
                              (String) explicitConceptReference.getUserData(USERDATA_LANGUAGE)),
                      designationComponent.getUse(),
                      designationComponent.getValue()));
        }
      }
    }
  }
}
