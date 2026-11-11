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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.validatecode;

import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_INITIALCHARACTERCASEINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.formatExpressionProcessingDisabledMessage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.RuleValidationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignation;
import de.gematik.zts.termzilla.terminologies.snomed.fhir.SnomedDesignationManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.snomed.languages.scg.SCGException;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Implements the {@code CodeSystem/$validate-code} operation for SNOMED CT.
 *
 * <p>Accepts a SNOMED concept identifier or a post-coordinated SCG expression as the code to
 * validate. The operation resolves the appropriate SNOMED release version, looks up the concept or
 * parses and MRCM-validates the expression, and optionally checks a provided display value against
 * the available designations.
 *
 * <p>Display matching respects the SNOMED case significance of each description:
 *
 * <ul>
 *   <li>{@code caseSignificanceId} entire term case-insensitive — compared with {@link
 *       String#equalsIgnoreCase}
 *   <li>{@code caseSignificanceId} entire term case-sensitive — compared with {@link String#equals}
 *   <li>{@code caseSignificanceId} initial character case-insensitive — first character compared
 *       case-insensitively, remainder compared case-sensitively
 * </ul>
 *
 * <p>When a {@code displayLanguage} is provided, only designations for that language are considered
 * during display validation. Without {@code displayLanguage}, all available designations are
 * checked. For post-coordinated expressions the {@code display} parameter is not supported and
 * causes a negative result with an explanatory message.
 *
 * <p>The display returned in the result is selected according to the language preference derived
 * from the {@code Accept-Language} header and the system default language. If no matching
 * designation is found, the default language of the resolved release version is used as a fallback.
 *
 * <p>The parameter {@code date} is not yet supported and will cause a {@link
 * NotImplementedException} to be thrown.
 */
@Slf4j
public class SnomedCodeSystemValidateCodeOperation {

  private final SnomedTerminologyManager snomedTerminologyManager;
  private final SnomedCodeSystemService snomedCodeSystemService;
  private final SCGExpressionParser expressionParser;
  private final DemisSCGProcessor demisSCGProcessor;
  private final Snomed snomed;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new instance backed by the given {@link SnomedCodeSystemService}.
   *
   * @param snomedCodeSystemService the service providing access to the loaded SNOMED release
   */
  public SnomedCodeSystemValidateCodeOperation(SnomedCodeSystemService snomedCodeSystemService) {
    this.snomedTerminologyManager = snomedCodeSystemService.getSnomedTerminologyManager();
    this.snomedCodeSystemService = snomedCodeSystemService;
    this.snomed = snomedTerminologyManager.getSnomed();
    this.expressionParser = snomedTerminologyManager.getExpressionParser();
    this.demisSCGProcessor = snomedTerminologyManager.getScgProcessor();
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Validates a SNOMED concept code or post-coordinated expression against the requested release
   * version.
   *
   * <p>Resolves the release version from the {@code version} parameter, or uses the default release
   * version when none is specified. The code is first looked up as a plain SNOMED concept. If no
   * concept is found, the code is parsed and MRCM-validated as an SCG expression. If neither
   * succeeds, a negative result with an error message is returned.
   *
   * <p>When a {@code display} value is provided for a plain concept, it is validated against the
   * available designations respecting case significance. For expressions, display validation is not
   * supported and always yields a negative result.
   *
   * <p>The display value in the result is selected based on the language preference derived from
   * the {@code Accept-Language} header and {@code languageSystemDefault}, with the release
   * version's default language as final fallback.
   *
   * @param param the validate-code parameters including system URL, code, display, and optional
   *     displayLanguage
   * @param languageSystemDefault the system default language used for display selection
   * @return the validation result; {@code result} is {@code false} if the code is unknown, the
   *     expression is invalid, or the provided display does not match any designation
   * @throws InvalidRequestException if the requested SNOMED version is unknown or the request is
   *     malformed
   * @throws NotImplementedException if the {@code date} parameter is provided
   */
  public ValidateCodeResult validateCode(
      CsValidateCodeParameter param, LanguageParameter languageSystemDefault)
      throws InvalidRequestException, NotImplementedException {

    log.debug("invoke $validate-code with: {}", param);

    // Parametervalidierung
    SnomedCsValidateCodeParameterValidator.validateParameters(param);

    // Extrahieren der relevanten Parameterwerte aus dem CsValidateCodeParameter
    String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;
    String code = param.getCodeParam() != null ? param.getCodeParam().getValue() : null;
    String display = param.getDisplayParam() != null ? param.getDisplayParam().getValue() : null;
    String displayLanguage =
        param.getDisplayLanguageParam() != null ? param.getDisplayLanguageParam().getValue() : null;

    // Vorbereiten des Ergebnis-Objekts
    ValidateCodeResult validateCodeResult = new ValidateCodeResult();

    // Kümmern um Versionierung
    SnomedReleaseVersion releaseVersion = resolveReleaseVersion(version);

    // Vorbereiten des SnomedDesignationManagerds
    SnomedDesignationManager designationManager = new SnomedDesignationManager();
    boolean isExpression = false;

    try {
      // Abrufen des Konzepts aus dem Modell. Wir suchen explizit auch nach inaktiven Konzepten, da
      // hierzu im FHIR-Standard nichts weiter gesagt wird
      SnomedConcept concept = snomed.getSnomedConceptWithDescription(code, releaseVersion, true);

      // Ermitteln aller relevanten Designations
      designationManager.collectDesignationsFromSnomedConcept(concept);

    } catch (UnknownConceptException _) {
      // Es könnte sich immernoch um eine Expression handeln. Wir prüfen das einfach mal
      Optional<ValidateCodeResult> invalidExpressionResult =
          findExpressionValidationFailure(code, display, releaseVersion, designationManager);
      if (invalidExpressionResult.isPresent()) {
        return invalidExpressionResult.get();
      }
      isExpression = true;
    }

    // Konzept oder Expression scheint es zu geben
    if (!isExpression) {
      // Wenn es sich um ein Konzept handelt, validieren wir ggf. den display-Parameter
      validateDisplay(designationManager, code, display, validateCodeResult, displayLanguage);
    } else {
      validateCodeResult.setResult(true);
    }

    // LanguageSelectionParameter erstellen (benötigen wir für die Rückgabe eines Displays in der
    // richtigen Sprache)
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
            .systemDefaultLanguage(languageSystemDefault)
            .build();

    // Display für die Rückgabe auswählen
    SnomedDesignation displayDesignation = selectDisplay(designationManager, lsp, releaseVersion);

    // Ergebnis vorbereiten
    if (displayDesignation != null) {
      validateCodeResult.setDisplay(displayDesignation.getValue());
    }

    return validateCodeResult;
  }

  /**
   * Resolves the SNOMED release version for the requested FHIR version.
   *
   * @param version the requested FHIR version, or {@code null} for the default release version
   * @return the matching release version
   * @throws InvalidRequestException if the requested version is not supported
   */
  private SnomedReleaseVersion resolveReleaseVersion(String version) {
    if (version == null) {
      return snomed.getDefaultSnomedReleaseVersion();
    }
    SnomedReleaseVersion releaseVersion =
        snomedCodeSystemService.getFhirVersionToSnomedReleaseVersionMap().get(version);
    if (releaseVersion == null) {
      throw new InvalidRequestException(
          "The requested SNOMED version '" + version + "' is not supported.");
    }
    return releaseVersion;
  }

  /**
   * Validates a code that is not a known concept as an SCG expression and collects its
   * designations.
   *
   * <p>Only a failed validation produces a result. A valid expression yields an empty {@link
   * Optional}; the positive result including the display is then created by the caller, in the
   * same way as for concepts.
   *
   * @param code the provided code
   * @param display the provided display value, or {@code null}
   * @param releaseVersion the SNOMED release version to validate against
   * @param designationManager the manager collecting the designations of the expression
   * @return a result with {@code result=false} if the code is not a valid expression, cannot be
   *     validated as one or is combined with a display; an empty {@link Optional} if the expression
   *     is valid
   */
  private Optional<ValidateCodeResult> findExpressionValidationFailure(
      String code,
      String display,
      SnomedReleaseVersion releaseVersion,
      SnomedDesignationManager designationManager) {
    // Ohne Expression-Verarbeitung ist ein unbekannter Code schlicht ungültig
    if (!snomedTerminologyManager.isExpressionProcessingEnabled()) {
      return Optional.of(
          negativeResult(
              formatExpressionProcessingDisabledMessage(
                  code, snomedTerminologyManager.isRestrictedMode())));
    }
    try {
      // try to parse the expression
      Expression expression = expressionParser.parseExpression(code);

      demisSCGProcessor.validateAgainstMRCM(expression, releaseVersion);

      // Ermitteln aller relevanten Designations für die Expression - für den Fall erforderlich, dass die Expression valide ist
      designationManager.collectDesignationsFromSnomedExpression(
          expression, releaseVersion, snomedTerminologyManager);

    } catch (UnknownConceptException
        | UnsupportedTerminologyException
        | RuleValidationException
        | SimpleExpressionConstraintException
        | InactiveConceptException
        | IllegalArgumentException // TODO Spezialfall: 1828780040
        | SCGException ex) {

      return Optional.of(
          negativeResult(
              "The provided code is neither a valid SNOMED conceptId nor a valid SNOMED"
                  + " expression: "
                  + ex.getMessage()));
    }

    // für Expressions unterstützen wir nicht den display-Parameter
    if (display != null) {
      ValidateCodeResult result =
          negativeResult("The display parameter is currently not supported for SNOMED expressions");
      result.setDisplay(display);
      return Optional.of(result);
    }
    return Optional.empty();
  }

  private static ValidateCodeResult negativeResult(String message) {
    ValidateCodeResult result = new ValidateCodeResult();
    result.setResult(false);
    result.setMessage(message);
    return result;
  }

  /**
   * Selects the designation returned as display, falling back to the default language of the
   * release version.
   *
   * @param designationManager the manager holding the collected designations
   * @param lsp the language selection derived from the request
   * @param releaseVersion the SNOMED release version providing the fallback language
   * @return the selected designation, or {@code null} if none is available
   */
  private static SnomedDesignation selectDisplay(
      SnomedDesignationManager designationManager,
      LanguageSelectionParameter lsp,
      SnomedReleaseVersion releaseVersion) {
    // im besten Fall haben wir eine Designation mit der korrekten Use (display) und Sprache, die
    // wir zurückgeben können
    SnomedDesignation displayDesignation = designationManager.getDisplay(lsp);

    // Fallback 1: Wenn keine Designation mit display-Use und passender Sprache gefunden wurde,
    // nehmen wir die default der jeweiligen ReleaseVersion
    if (displayDesignation == null) {
      LanguageSelectionParameter lspFallback =
          LanguageSelectionParameter.builder()
              .systemDefaultLanguage(LanguageParameter.parse(releaseVersion.getDefaultLanguage()))
              .build();
      displayDesignation = designationManager.getDisplay(lspFallback);
    }
    return displayDesignation;
  }

  /**
   * Validates the provided display value against the concept's pre-collected designations and
   * updates the result accordingly.
   *
   * <p>When {@code displayLanguage} is given, only designations for that language are considered;
   * validation fails immediately if none exists for that language or if no value matches. Without
   * {@code displayLanguage}, the display is matched against all collected designations.
   *
   * <p>When {@code display} is {@code null}, the result is set to {@code true} without further
   * checks.
   *
   * @param designationManager the manager holding the pre-collected designations for the concept
   * @param code the concept code, used in failure messages
   * @param display the display value to validate, or {@code null} to skip validation
   * @param validateCodeResult the result object to update
   * @param displayLanguage the requested language tag, or {@code null} if not specified
   */
  private void validateDisplay(
      SnomedDesignationManager designationManager,
      String code,
      String display,
      ValidateCodeResult validateCodeResult,
      String displayLanguage) {

    if (display == null) {
      validateCodeResult.setResult(true);
      return;
    }

    if (displayLanguage != null) {
      List<SnomedDesignation> candidates =
          designationManager.getDesignationsByLanguages(List.of(displayLanguage));
      if (candidates.isEmpty()) {
        String message =
            String.format(
                "The displayLanguage '%s' is not available for the code '%s'.",
                displayLanguage, code);
        validateCodeResult.setResult(false);
        validateCodeResult.setMessage(message);
        return;
      }
      if (candidates.stream().anyMatch(d -> isDisplayMatching(display, d))) {
        validateCodeResult.setResult(true);
      } else {
        String message =
            String.format(
                "The provided display value '%s' does not match the display value(s) in the concept with requested displayLanguage '%s'.",
                display, displayLanguage);
        validateCodeResult.setResult(false);
        validateCodeResult.setMessage(message);
      }
    } else {
      if (matchesAnyDesignation(designationManager, display)) {
        validateCodeResult.setResult(true);
      } else {
        String message =
            String.format(
                "The provided display value '%s' does not match any display value in the concept.",
                display);
        validateCodeResult.setResult(false);
        validateCodeResult.setMessage(message);
      }
    }
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Returns {@code true} if any designation in the manager's full list matches the provided
   * display, respecting the SNOMED case significance of each description.
   *
   * @param designationManager the manager holding the collected designations
   * @param display the display value to match against
   * @return {@code true} if at least one designation value matches
   */
  private boolean matchesAnyDesignation(
      SnomedDesignationManager designationManager, String display) {

    return designationManager.getFullDesignationList().stream()
        .anyMatch(d -> isDisplayMatching(display, d));
  }

  /**
   * Returns {@code true} if {@code providedDisplay} matches the value of {@code designation},
   * applying the matching rule defined by the designation's {@code caseSignificanceId}.
   *
   * @param providedDisplay the display value supplied by the caller
   * @param designation the SNOMED designation to compare against
   * @return {@code true} if the values match under the designation's case significance rule
   */
  private boolean isDisplayMatching(String providedDisplay, SnomedDesignation designation) {

    switch (designation.getCaseSignificanceId()) {
      case SC_CASESINSENSITIVE: // Case Insensitive
        return designation.getValue().equalsIgnoreCase(providedDisplay);
      case SC_CASESENSITIVE: // Case Sensitive
        return designation.getValue().equals(providedDisplay);
      case SC_INITIALCHARACTERCASEINSENSITIVE: // Initial Character Case Insensitive
        String designationFirstChar = designation.getValue().substring(0, 1);
        String providedDisplayFirstChar = providedDisplay.substring(0, 1);
        boolean firstCharMatches = designationFirstChar.equalsIgnoreCase(providedDisplayFirstChar);
        boolean restMatches =
            designation.getValue().substring(1).equals(providedDisplay.substring(1));
        return firstCharMatches && restMatches;
      default:
        return false; // Unbekannte Case Significance, kein Match
    }
  }
}
