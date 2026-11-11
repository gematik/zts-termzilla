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
package de.gematik.zts.termzilla.terminologies.loinc.provider.cs.validatecode;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincCodeSystemService;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincDesignationManager;
import de.gematik.zts.termzilla.terminologies.loinc.fhir.LoincFhirDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@code CodeSystem/$validate-code} operation for LOINC.
 *
 * <p>Supports validation of LOINC Terms (numeric codes), Parts ({@code LP*}), Answers ({@code
 * LA*}), and Answer Lists ({@code LL*}). The operation resolves the appropriate LOINC version,
 * looks up the concept, and optionally validates a provided display value against the available
 * designations.
 *
 * <p>Display matching is performed case-insensitively ({@link String#equalsIgnoreCase}), as LOINC
 * does not define concept-specific case significance rules.
 *
 * <p>When a {@code displayLanguage} is provided, only designations for that language are considered
 * during display validation; BCP-47 expansion is applied via {@link LoincDesignationManager}. If no
 * designation exists for the requested language, the result is set to {@code false}. Without {@code
 * displayLanguage}, all available designations are checked.
 *
 * <p>The display returned in the result is selected according to the language preference derived
 * from the {@code Accept-Language} header and the system default language, with en-US as a final
 * fallback (LOINC standard language).
 *
 * <p>The parameter {@code date} is not yet supported and will cause a {@link
 * NotImplementedException} to be thrown.
 */
@Slf4j
public class LoincCsValidateCodeOperation {

  private final LoincCodeSystemService loincCodeSystemService;
  private final Loinc loinc;

  // ----------------------------------------------------------------------------------------

  /**
   * Creates a new instance backed by the given {@link LoincCodeSystemService}.
   *
   * @param loincCodeSystemService the service providing access to the loaded LOINC data
   */
  public LoincCsValidateCodeOperation(LoincCodeSystemService loincCodeSystemService) {
    this.loincCodeSystemService = loincCodeSystemService;
    this.loinc = loincCodeSystemService.getLoincTerminologyManager().getLoinc();
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Validates a LOINC concept code against the requested version.
   *
   * <p>Resolves the LOINC version from the {@code version} parameter, or uses the default version
   * when none is specified. The code is looked up in the LOINC model; inactive concepts are
   * included as the FHIR standard does not restrict this.
   *
   * <p>When a {@code display} value is provided, it is validated against the available designations
   * case-insensitively. The display returned in the result is selected based on the language
   * preference chain.
   *
   * @param param the validate-code parameters including system URL, code, display, and optional
   *     displayLanguage
   * @param languageSystemDefault the system default language used for display selection
   * @return the validation result; {@code result} is {@code false} if the code is unknown or the
   *     provided display does not match any designation
   * @throws InvalidRequestException if the requested LOINC version is unknown
   * @throws NotImplementedException if the {@code date} parameter is provided
   */
  public ValidateCodeResult validateCode(
      CsValidateCodeParameter param, LanguageParameter languageSystemDefault)
      throws InvalidRequestException, NotImplementedException {

    log.debug("invoke $validate-code with: {}", param);

    // Validieren der übergebenen Parameter (z.B. Ausschluss von aktuell nicht unterstützten
    // Parametern)
    LoincCsValidateCodeParameterValidator.validateParameters(param);

    // Extrahieren der relevanten Parameterwerte
    String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;
    String code = param.getCodeParam() != null ? param.getCodeParam().getValue() : null;
    if (code == null) {
      // Wird eigentlich vorher getestet
      throw new InvalidRequestException(
          "No "
              + CsValidateCodeParameter.CODE_PARAM
              + " was provided in $validate-code parameters.");
    }
    String display = param.getDisplayParam() != null ? param.getDisplayParam().getValue() : null;
    String displayLanguage =
        param.getDisplayLanguageParam() != null ? param.getDisplayLanguageParam().getValue() : null;

    // Ermitteln der zu verwendenden LOINC-Version
    LoincVersion loincVersion;
    if (version != null) {
      loincVersion = loincCodeSystemService.getFhirVersionToLoincVersionMap().get(version);
      if (loincVersion == null) {
        throw new InvalidRequestException(
            "The requested LOINC version '" + version + "' is not supported.");
      }
    } else {
      // Keine Version angegeben — Standardversion verwenden
      loincVersion = loinc.getDefaultLoincVersion();
    }

    // Vorbereiten des Ergebnis-Objekts
    ValidateCodeResult validateCodeResult = new ValidateCodeResult();

    // Vorbereiten des LoincDesignationManagers, um den Umgang mit Designations zu erleichtern
    String defaultDesignationType = LoincDesignationManager.resolveDefaultDesignationType(code);
    LoincDesignationManager designationManager =
        new LoincDesignationManager(defaultDesignationType);

    try {
      // Abrufen des Konzepts aus dem Modell. Wir suchen explizit auch nach inaktiven Konzepten, da
      // hierzu im FHIR-Standard nichts weiter in Bezug zur $validate-code Operation gesagt wird
      LoincConcept concept =
          loinc.getLoincConcept(
              code, loincVersion, true, new HashSet<>(loinc.getSupportedLanguages()));

      // Ermitteln aller relevanten Designations
      designationManager.collectDesignationsFromLoincConcept(concept);

    } catch (UnknownConceptException _) {
      // Für den Fall, dass der Code nicht gefunden wird, setzen wir das Ergebnis auf false und
      // geben die Fehlermeldung zurück.
      String message =
          String.format(
              "The code '%s' was not found in LOINC version '%s'.",
              code, loincVersion.getVersionString());
      log.debug(message);
      validateCodeResult.setResult(false);
      validateCodeResult.setMessage(message);
      return validateCodeResult;
    } catch (InactiveConceptException e) {
      // Sollte niemals geworfen werden, da wir explizit auch inaktive Konzepte anfragen
      log.info(e.getMessage());
      throw new InvalidRequestException(e.getMessage());
    }

    // Konzept gefunden — ggf. den display-Parameter validieren
    validateDisplay(designationManager, code, display, validateCodeResult, displayLanguage);

    // LanguageSelectionParameter erstellen (benötigen wir für die Rückgabe eines Displays in der
    // richtigen Sprache)
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
            .systemDefaultLanguage(languageSystemDefault)
            .build();

    // Display für die Rückgabe auswählen
    LoincFhirDesignation displayDesignation = designationManager.getDisplay(lsp);
    if (displayDesignation != null) {
      validateCodeResult.setDisplay(displayDesignation.getValue());
    }

    return validateCodeResult;
  }

  /**
   * Validates the provided display value against the concept's pre-collected designations and
   * updates the result accordingly.
   *
   * <p>When {@code displayLanguage} is given, only designations for that language are considered
   * via BCP-47 expansion; validation fails immediately if none exists for that language or if no
   * value matches. Without {@code displayLanguage}, the display is matched against all collected
   * designations.
   *
   * <p>When {@code display} is {@code null}, the result is set to {@code true} without further
   * checks.
   *
   * <p>Matching is performed case-insensitively, as LOINC does not define concept-specific case
   * significance.
   *
   * @param designationManager the manager holding the pre-collected designations for the concept
   * @param code the concept code, used in failure messages
   * @param display the display value to validate, or {@code null} to skip validation
   * @param validateCodeResult the result object to update
   * @param displayLanguage the requested language tag, or {@code null} if not specified
   */
  private void validateDisplay(
      LoincDesignationManager designationManager,
      String code,
      String display,
      ValidateCodeResult validateCodeResult,
      String displayLanguage) {

    if (display == null) {
      validateCodeResult.setResult(true);
      return;
    }

    if (displayLanguage != null) {
      List<LoincFhirDesignation> candidates =
          designationManager.getDesignationsByLanguages(List.of(displayLanguage));
      if (candidates.isEmpty()) {
        String message =
            String.format(
                "The requested displayLanguage '%s' is not available for the code '%s'.",
                displayLanguage, code);
        validateCodeResult.setResult(false);
        validateCodeResult.setMessage(message);
        return;
      }
      if (candidates.stream().anyMatch(d -> d.getValue().equalsIgnoreCase(display))) {
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
      if (designationManager.getFullDesignationList().stream()
          .anyMatch(d -> d.getValue().equalsIgnoreCase(display))) {
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
}
