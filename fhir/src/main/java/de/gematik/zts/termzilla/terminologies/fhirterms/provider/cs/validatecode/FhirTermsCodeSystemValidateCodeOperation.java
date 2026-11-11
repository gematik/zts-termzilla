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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.validatecode;

import static de.gematik.zts.termzilla.common.helper.DesignationManager.DESIGNATION_USAGE_SYSTEM;
import static de.gematik.zts.termzilla.common.helper.DesignationManager.DISPLAY_USE_CODE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignation;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignationManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.jspecify.annotations.NonNull;

// TODO handle abstract parameter

/**
 * Implements the {@code CodeSystem/$validate-code} operation for FHIR package terminologies.
 *
 * <p>Resolves the requested CodeSystem and looks up the given code. If the code is not found, a
 * negative result is returned without throwing an exception. When a display value is provided, it
 * is validated against the concept's designations collected via {@link FhirDesignationManager},
 * which considers both the concept's primary display and its explicit designation entries
 * uniformly.
 *
 * <p>Display validation follows two paths depending on whether a {@code displayLanguage} is
 * requested:
 *
 * <ul>
 *   <li>With {@code displayLanguage}: all designations for the requested language (expanded via BCP
 *       47 hierarchy) are checked; validation fails if none is found for that language or if no
 *       value matches the provided display.
 *   <li>Without {@code displayLanguage}: the provided display is matched against all collected
 *       designations regardless of language or use coding.
 * </ul>
 *
 * <p>The display value returned in the result is selected by language preference: first a
 * designation with {@code designation-usage#display} use and matching language is preferred, then
 * any designation with that use, and finally any available designation.
 *
 * <p>The parameters {@code date} and {@code abstract} are not yet supported and will cause a {@link
 * NotImplementedException} to be thrown.
 */
@Slf4j
public class FhirTermsCodeSystemValidateCodeOperation {

  private static final String CASE_SENSITIVE_HINT = " Hint: The CodeSystem is case-sensitive.";
  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates a new instance backed by the in-memory representation of the given terminology manager.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to loaded FHIR
   *     packages
   */
  public FhirTermsCodeSystemValidateCodeOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  // ----------------------------------------------------------------------------------------
  // CodeSystem $validate-code
  // ----------------------------------------------------------------------------------------

  /**
   * Validates a code against the CodeSystem identified by the parameters.
   *
   * <p>Resolves the CodeSystem and looks up the given code. If the code is not found, a negative
   * result is returned without throwing an exception. When a display value is provided, it is
   * additionally validated against the concept's designations.
   *
   * <p>The display value in the result is selected based on the language preference derived from
   * {@code languageSystemDefault} and the request's Accept-Language header.
   *
   * @param scope the tenant scope used to resolve the CodeSystem
   * @param param the validate-code parameters including system URL, code, display, and optional
   *     displayLanguage
   * @param languageSystemDefault the system default language, used for display selection in the
   *     result
   * @return the validation result; {@code result} is {@code false} if the code does not exist or if
   *     a provided display does not match any concept designation
   * @throws InvalidRequestException if the request is malformed
   * @throws UnsupportedTerminologyException if the requested CodeSystem is not supported
   * @throws NotImplementedException if an unsupported parameter ({@code date} or {@code abstract})
   *     is provided
   * @throws UnknownScopeException if the given scope is not known
   */
  public ValidateCodeResult validateCode(
      String scope, CsValidateCodeParameter param, LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException {

    log.debug("invoke $validate-code with: {}", param);

    // Parameterkombinationen prüfen
    FhirTermsCsValidateCodeParameterValidator.validateParameters(param);

    // Erforderliche Parameter extrahieren
    String system = param.getUrlParam() != null ? param.getUrlParam().getValue() : null;
    String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;
    String code = param.getCodeParam() != null ? param.getCodeParam().getValue() : null;
    if (code == null) {
      throw new InvalidRequestException("Code Parameter Missing");
    }
    String display = param.getDisplayParam() != null ? param.getDisplayParam().getValue() : null;
    CodeType displayLanguage = param.getDisplayLanguageParam();

    // Resultatobjekt vorbereiten (brauchen wir auf jeden Fall)
    ValidateCodeResult validateCodeResult = new ValidateCodeResult();

    // Codesystem laden
    CodeSystem codeSystem = getCodeSystem(scope, system, version);

    // Konzept suchen
    CodeSystem.ConceptDefinitionComponent concept;

    try {
      // Laden des Konzeptes aus dem CodeSystem
      concept = CodeSystemHelper.findCodeInCodeSystem(codeSystem, code);
    } catch (UnknownConceptException _) {
      // Ergebnisobjekt vorbereiten

      String message =
          "The requested code '"
              + code
              + "' is not supported in the requested CodeSystem '"
              + system
              + "'.";
      // return negative validation result otherwise
      validateCodeResult.setResult(false);
      validateCodeResult.setDisplay(null);
      validateCodeResult.setMessage(message);
      return validateCodeResult;
    }

    // Designations sammeln (brauchen wir für die Display-Validierung und die Rückgabe eines
    // passenden Displays)
    FhirDesignationManager designationManager = new FhirDesignationManager();
    designationManager.collectDesignationsFromFhirConcept(concept, codeSystem.getLanguage());

    // Validierung des übergebenen Display-Wertes
    validateDisplay(
        concept,
        designationManager,
        display,
        displayLanguage != null ? displayLanguage.getCode() : null,
        codeSystem.getCaseSensitive(),
        validateCodeResult);

    // LanguageSelectionParameter erstellen (benötigen wir für die Rückgabe eines Displays in der
    // richtigen Sprache)
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
            .systemDefaultLanguage(languageSystemDefault)
            .build();

    // Display für die Rückgabe auswählen
    // im besten Fall haben wir eine Designation mit der korrekten Use (display) und Sprache, die
    // wir zurückgeben können
    FhirDesignation displayDesignation = designationManager.getDisplay(lsp);

    // Fallback 1: Wenn keine Designation mit display-Use und passender Sprache gefunden wurde,
    // nehmen wir die erste verfügbare Designation, bei der die use passt
    if (displayDesignation == null) {
      displayDesignation =
          designationManager
              .getDesignationsByUse(DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE)
              .stream()
              .findFirst()
              .orElse(null);
    }

    // Fallback 2: Wenn es überhaupt keine Designation mit passender use gibt, nehmen wir die erste
    // verfügbare Designation unabhängig von der use
    if (displayDesignation == null) {
      displayDesignation =
          designationManager.getFullDesignationList().stream().findFirst().orElse(null);
    }

    // Ergebnis vorbereiten
    if (displayDesignation != null) {
      validateCodeResult.setDisplay(displayDesignation.getValue());
    } else {
      // Falls die Prüfung erfolgreich war und kein Display zurückgegeben werden kann, sollte eine
      // entsprechende Nachricht mitgegeben werden
      if (validateCodeResult.isResult()) {
        validateCodeResult.setMessage(
            "No display available for the concept with code '" + code + "'.");
      }
    }

    return validateCodeResult;
  }

  private @NonNull CodeSystem getCodeSystem(String scope, String system, String version)
    throws UnknownScopeException, UnsupportedTerminologyException {
    CodeSystem codeSystem =
        fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class, scope, system, version, ResourceRetrievalMode.FULL);
    log.debug("CodeSystem: {}|{}", codeSystem.getUrl(), codeSystem.getVersion());

    // Wenn es sich beim CS, um ein Supplement handelt, lassen wir die Validierung nicht zu
    if ((codeSystem.hasContent()
            && codeSystem.getContent() == CodeSystem.CodeSystemContentMode.SUPPLEMENT)
        || (codeSystem.hasSupplements())) {
      throw new InvalidRequestException(
          "Validation against CodeSystem Supplements is not supported.");
    }
    return codeSystem;
  }

  /**
   * Validates the provided display value against the concept's pre-collected designations and
   * updates the result accordingly.
   *
   * <p>When {@code displayLanguage} is given, only designations for that language (BCP 47 expanded)
   * are considered; validation fails immediately if no designation exists for that language or if
   * no value matches the provided display. Without {@code displayLanguage}, the display is matched
   * against all collected designations regardless of language or use coding.
   *
   * <p>When {@code display} is {@code null}, the result is set to {@code true} without further
   * checks.
   *
   * @param concept the concept whose code is used in failure messages
   * @param designationManager the manager holding the already-collected designations for {@code
   *     concept}
   * @param display the display value to validate, or {@code null} to skip validation
   * @param displayLanguage the requested language tag, or {@code null} if not specified
   * @param isCaseSensitive whether display matching is case-sensitive
   * @param validateCodeResult the result object to update
   */
  private void validateDisplay(
      ConceptDefinitionComponent concept,
      FhirDesignationManager designationManager,
      String display,
      String displayLanguage,
      boolean isCaseSensitive,
      ValidateCodeResult validateCodeResult) {

    if (display == null) {
      validateCodeResult.setResult(true);
      return;
    }

    if (displayLanguage != null) {
      List<FhirDesignation> candidates =
          designationManager.getDesignationsByLanguages(List.of(displayLanguage));
      if (candidates.isEmpty()) {
        String message =
            String.format(
                "The requested displayLanguage '%s' is not available for the code '%s'.",
                displayLanguage, concept.getCode());
        setValidationFailure(validateCodeResult, message, false);
        return;
      }
      if (candidates.stream()
          .anyMatch(d -> isDisplayMatching(display, d.getValue(), isCaseSensitive))) {
        validateCodeResult.setResult(true);
      } else {
        String message =
            String.format(
                "The provided display value '%s' does not match the display value '%s' "
                    + "in the concept with requested displayLanguage '%s'.",
                display, candidates.getFirst().getValue(), displayLanguage);
        setValidationFailure(validateCodeResult, message, isCaseSensitive);
      }
    } else {
      if (matchesAnyDesignation(designationManager, display, isCaseSensitive)) {
        validateCodeResult.setResult(true);
      } else {
        String message =
            String.format(
                "The provided display value '%s' does not match any display value in the concept.",
                display);
        setValidationFailure(validateCodeResult, message, isCaseSensitive);
      }
    }
  }

  /**
   * Returns {@code true} if any designation in the manager's full list has a value that matches the
   * provided display, respecting case sensitivity.
   *
   * @param designationManager the manager holding the collected designations
   * @param display the display value to match against
   * @param isCaseSensitive whether matching is case-sensitive
   * @return {@code true} if at least one designation value matches
   */
  private boolean matchesAnyDesignation(
      FhirDesignationManager designationManager, String display, boolean isCaseSensitive) {

    return designationManager.getFullDesignationList().stream()
        .filter(d -> d.getValue() != null)
        .anyMatch(
            d ->
                isCaseSensitive
                    ? d.getValue().equals(display)
                    : d.getValue().equalsIgnoreCase(display));
  }

  /**
   * Returns {@code true} if {@code providedDisplay} matches {@code conceptDisplay}, respecting case
   * sensitivity.
   *
   * @param providedDisplay the display value supplied by the caller
   * @param conceptDisplay the reference display value from the concept
   * @param isCaseSensitive whether matching is case-sensitive
   * @return {@code true} if the values match under the given case rule
   */
  private boolean isDisplayMatching(
      String providedDisplay, String conceptDisplay, boolean isCaseSensitive) {
    return isCaseSensitive
        ? conceptDisplay.equals(providedDisplay)
        : conceptDisplay.equalsIgnoreCase(providedDisplay);
  }

  /**
   * Sets the result to a failed validation with the given message. When {@code isCaseSensitive} is
   * {@code true}, a hint about case sensitivity is appended to the message.
   *
   * @param validateCodeResult the result object to update
   * @param message the human-readable failure reason
   * @param isCaseSensitive whether to append the case-sensitivity hint
   */
  private void setValidationFailure(
      ValidateCodeResult validateCodeResult, String message, boolean isCaseSensitive) {

    String fullMessage = message;
    if (isCaseSensitive) {
      fullMessage += CASE_SENSITIVE_HINT;
    }

    validateCodeResult.setResult(false);
    validateCodeResult.setMessage(fullMessage);
  }
}
