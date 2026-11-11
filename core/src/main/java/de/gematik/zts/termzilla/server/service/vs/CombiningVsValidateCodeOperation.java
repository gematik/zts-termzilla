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
package de.gematik.zts.termzilla.server.service.vs;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.server.cache.ValidationCacheKey;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class CombiningVsValidateCodeOperation {

  private static final String LENIENT_DISPLAY_WARNING_MESSAGE =
      "The provided display is not valid for the code in the CodeSystem. Validation succeeded because 'lenient-display-validation' is true.";

  private CombiningVsValidateCodeOperation() {
    // private constructor to prevent instantiation
  }

  /**
   * @param combiningTerminologyService TerminologyService
   * @param vsValidateCodeParameter Validierungsanfrage
   * @param scope
   * @return Validierungsergebnis
   * @throws InvalidRequestException Anfrage ungültig
   * @throws UnsupportedTerminologyException Die im Request angegebene Terminologie wird nicht
   *     unterstützt
   * @throws NotImplementedException Die Methode ist noch nicht implementiert
   */
  public static ValidateCodeResult validateCode(
      CombiningTerminologyService combiningTerminologyService,
      VsValidateCodeParameter vsValidateCodeParameter,
      String scope)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    log.debug("CombiningVsValidateCodeOperation.validateCode called for scope {}", scope);

    // Caching-Key erzeugen und prüfen, ob wir das Validierungsergebnis schon kennen
    ValidationCacheKey cacheKey = new ValidationCacheKey(scope, vsValidateCodeParameter);
    ValidateCodeResult result =
        combiningTerminologyService.getCacheManager().getValidationCache().getIfPresent(cacheKey);
    if (result != null) {
      log.debug("Found cached validation result for key {}", cacheKey);
      return result;
    }

    // Fall 1: code-Parameter und optional system, systemVersion, display-Parameter werden
    if (vsValidateCodeParameter.getCode().isPresent()) {
      return validateByCodeParameter(
          combiningTerminologyService, vsValidateCodeParameter, scope, cacheKey);
    }

    // ----------------------------------------------------------------------------------------
    // Fall 2: coding-Parameter
    Optional<ValidateCodeResult> codingValidationResult =
        validateByCodingParameter(combiningTerminologyService, vsValidateCodeParameter, scope);
    if (codingValidationResult.isPresent()) {
      return codingValidationResult.get();
    }

    // ----------------------------------------------------------------------------------------
    // Fall 3: CodeableConcept-Parameter
    // Spec: "A full codeableConcept to validate. The server returns true if one of the coding
    // values is in the value set, and may also validate that the codings are not in conflict with
    // each other if more than one is present"
    Optional<ValidateCodeResult> codeableConceptValidationResult =
        validateByCodeableConceptParameter(
            combiningTerminologyService, vsValidateCodeParameter, scope);
    if (codeableConceptValidationResult.isPresent()) {
      return codeableConceptValidationResult.get();
    }

    ValidateCodeResult negativeResult = new ValidateCodeResult();
    negativeResult.setResult(false);
    negativeResult.setMessage("None of the provided Codings is valid in the ValueSet.");

    return negativeResult;

    // TODO Fall 4: Es scheint noch den Fall zu geben, dass lediglich ein Code gesetzt wird, ohne
    // das ein
    // system angegeben wird. Dann muss das System wohl erraten werden.

    // Wenn keiner der Parameter gesetzt ist, können wir keine Terminologie bestimmen
  }

  /**
   * Validates a request that carries a direct {@code code} parameter and optional related fields.
   *
   * @param combiningTerminologyService terminology service used for expansion and code validation
   * @param vsValidateCodeParameter validate-code request parameters
   * @param scope tenant or namespace scope used for terminology access
   * @param cacheKey cache key for this validation request
   * @return validation result for the code-based scenario
   * @throws UnsupportedTerminologyException if referenced terminology artifacts are not supported
   * @throws NotImplementedException if delegated operations are not implemented by a backend
   * @throws UnknownScopeException if the provided scope is unknown
   * @throws UnknownConceptException if delegated code validation reports an unknown concept
   */
  private static ValidateCodeResult validateByCodeParameter(
      CombiningTerminologyService combiningTerminologyService,
      VsValidateCodeParameter vsValidateCodeParameter,
      String scope,
      ValidationCacheKey cacheKey)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    String system = vsValidateCodeParameter.getSystem().orElse(null);
    String systemVersion = vsValidateCodeParameter.getSystemVersion().orElse(null);
    String code = vsValidateCodeParameter.getCode().orElse(null);
    String display = vsValidateCodeParameter.getDisplay().orElse(null);
    CodeSystem codeSystem = null;

    log.debug(
        "Validate code: {}|{}#{}",
        vsValidateCodeParameter.getSystem().orElse("n/a"),
        vsValidateCodeParameter.getSystemVersion().orElse("n/a"),
        vsValidateCodeParameter.getCode().orElse("n/a"));

    // Sofern ein 'system' angegeben ist, können wir prüfen, ob es unterstützt wird. Das machen
    // wir bewusst bereits vor der Expansion, da diese sehr teuer sein kann und wir somit
    // unnötige Aufrufe vermeiden können
    if (vsValidateCodeParameter.getSystem().isPresent()) {

      // CodeSystem laden
      codeSystem = loadCodeSystem(scope, system, systemVersion, combiningTerminologyService);

      // Prüfen, ob das referenzierte CodeSystem versionNeeded=true gesetzt hat. In diesem Fall
      // wird zwingend eine Version benötigt
      if (codeSystem.getVersionNeeded() && systemVersion == null) {
        throw new InvalidRequestException(
            "The underlying CodeSystem of the concept to validate is versionAware (versionNeeded = true). Please provide a version.");
      }
    } else {
      // Wenn kein System angegeben ist, dann müssen wir prüfen, ob inferSystem=true gesetzt ist.
      if (!vsValidateCodeParameter.getInferSystem().orElse(false)) {
        throw new InvalidRequestException(
            "The 'system' parameter is required when validating a code without inferSystem=true.");
      }
    }

    // ValueSetExpansion durchführen
    ValueSet.ValueSetExpansionComponent expansion =
        loadExpansion(scope, vsValidateCodeParameter, combiningTerminologyService);
    log.debug("Expansion Elements: {}", expansion.getContains().size());

    // Wenn die Expansion leer ist, dann ist das Ergebnis von $validate-code immer false
    if (expansion.getContains().isEmpty()) {
      ValidateCodeResult emptyResult = new ValidateCodeResult();
      emptyResult.setResult(false);
      emptyResult.setMessage("The expansion of the provided/referenced ValueSet is empty.");
      combiningTerminologyService.getCacheManager().getValidationCache().put(cacheKey, emptyResult);
      return emptyResult;
    }

    // Wenn kein System angegeben ist, aber inferSystem=true gesetzt ist, dann müssen wir das
    // System aus der Expansion ableiten. Das ist ein spezieller Fall, der erst in R6 dazugekommen
    // ist, aber bereits vom Standardvalidator verwendet wird.
    if (vsValidateCodeParameter.getSystem().isEmpty()
        && vsValidateCodeParameter.getInferSystem().orElse(false)) {

      // Wenn in der Expansion nur ein CodeSystem enthalten ist, dann können wir dieses als
      // gesetzt annehmen. Deshalb durchlaufen wir die einzelnen Expansion-Elemente in einem
      // Stream und zählen die verschiedenen CodeSystem-urls.
      Set<String> distinctCsSet =
          expansion.getContains().stream()
              .map(ValueSet.ValueSetExpansionContainsComponent::getSystem)
              .collect(Collectors.toSet());

      if (distinctCsSet.size() != 1) {
        throw new InvalidRequestException(
            "The ValueSet-Expansion contains concepts from more than one CodeSystem. Infering not possible.");
      } else {
        // Übernehmen des 'inferierten' Systems
        system = distinctCsSet.iterator().next();
      }
    }

    // Laden des inferierten CodeSystems
    // TODO: müssen wir ggf. auch die Version des CodeSystems inferieren?
    codeSystem = loadCodeSystem(scope, system, systemVersion, combiningTerminologyService);
    // Prüfen, ob das geladene CodeSystem versionNeeded=true gesetzt hat. In diesem Fall
    // wird zwingend eine Version benötigt
    if (codeSystem.getVersionNeeded() && systemVersion == null) {
      throw new InvalidRequestException(
          "The underlying CodeSystem of the concept to validate is versionAware (versionNeeded = true). "
              + "Please provide a version.");
    }

    // TODO: Wir haben hier noch ein Issue mit SNOMED POCCs, da hier ja die Schreibweise beliebig
    //  variieren darf. Das muss noch abgefangen werden, bzw. wir machen einfach nochmal nen
    //  $lookup und bekommen dann die vereinheitlichte Schreibweise

    Optional<ValueSet.ValueSetExpansionContainsComponent> match =
        findMatchingExpansionEntry(expansion, codeSystem, system, systemVersion, code);

    ValidateCodeResult validateCodeResult =
        buildValidateCodeResultFromMatch(
            match, display, scope, vsValidateCodeParameter, combiningTerminologyService);

    combiningTerminologyService
        .getCacheManager()
        .getValidationCache()
        .put(cacheKey, validateCodeResult);
    return validateCodeResult;
  }

  /**
   * Validates a request that carries a {@link Coding} by converting it into code parameters.
   *
   * @param combiningTerminologyService terminology service used for delegated validation
   * @param vsValidateCodeParameter validate-code request parameters
   * @param scope tenant or namespace scope used for terminology access
   * @return delegated validation result if a coding with system is present, otherwise empty
   * @throws UnsupportedTerminologyException if referenced terminology artifacts are not supported
   * @throws NotImplementedException if delegated operations are not implemented by a backend
   * @throws UnknownScopeException if the provided scope is unknown
   * @throws UnknownConceptException if delegated code validation reports an unknown concept
   */
  private static Optional<ValidateCodeResult> validateByCodingParameter(
      CombiningTerminologyService combiningTerminologyService,
      VsValidateCodeParameter vsValidateCodeParameter,
      String scope)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    Coding coding =
        vsValidateCodeParameter.getCoding().isPresent()
            ? vsValidateCodeParameter.getCoding().get()
            : null;
    if (coding != null && StringUtils.isNoneEmpty(coding.getSystem())) {

      log.debug("Validate Coding: {}", coding.getSystem());

      // Arbeitshypothese: Wir schreiben hier die Parameter um, sodass Fall 1 relevant wird und
      // rufen die Methode abermals auf

      // Erstellen einer Kopie des Parameters, um die Originalparameter nicht zu verändern
      VsValidateCodeParameter vsValidateCodeParameterCopy = vsValidateCodeParameter.shallowCopy();

      // system, systemVersion und code-Parameter setzen, sofern vorhanden
      if (coding.hasSystem()) {
        vsValidateCodeParameterCopy.setSystemParam(new UriParam(coding.getSystem()));
      }
      if (coding.hasVersion()) {
        vsValidateCodeParameterCopy.setSystemVersionParam(new StringParam(coding.getVersion()));
      }
      if (coding.hasCode()) {
        vsValidateCodeParameterCopy.setCodeParam(new TokenParam(coding.getCode()));
      }
      if (coding.hasDisplay()) {
        vsValidateCodeParameterCopy.setDisplayParam(new StringParam(coding.getDisplay()));
      }

      // coding-Parameter entfernen, da wir mit system, systemVersion und code arbeiten
      vsValidateCodeParameterCopy.setCodingParam(null);

      log.debug("validateCode: coding-Parameter: {}", vsValidateCodeParameterCopy);

      return Optional.of(
          validateCode(combiningTerminologyService, vsValidateCodeParameterCopy, scope));
    }

    return Optional.empty();
  }

  /**
   * Validates a request that carries a {@link CodeableConcept} by checking each contained coding.
   *
   * @param combiningTerminologyService terminology service used for delegated validation
   * @param vsValidateCodeParameter validate-code request parameters
   * @param scope tenant or namespace scope used for terminology access
   * @return first successful delegated result, otherwise empty
   * @throws UnsupportedTerminologyException if referenced terminology artifacts are not supported
   * @throws NotImplementedException if delegated operations are not implemented by a backend
   * @throws UnknownScopeException if the provided scope is unknown
   * @throws UnknownConceptException if delegated code validation reports an unknown concept
   */
  private static Optional<ValidateCodeResult> validateByCodeableConceptParameter(
      CombiningTerminologyService combiningTerminologyService,
      VsValidateCodeParameter vsValidateCodeParameter,
      String scope)
      throws UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    CodeableConcept codeableConcept =
        vsValidateCodeParameter.getCodeableConcept().isPresent()
            ? vsValidateCodeParameter.getCodeableConcept().get()
            : null;
    if (codeableConcept != null && !codeableConcept.getCoding().isEmpty()) {

      // Prinzipiell kann der CodeableConcept-Parameter mehrere Codings enthalten, die wir alle
      // prüfen müssen. Nur ein Coding muss im VS enthalten sein
      for (Coding codingElement : codeableConcept.getCoding()) {

        log.debug("Validate CodeableConcept: {}", codingElement.getSystem());

        VsValidateCodeParameter vsValidateCodeParameterCopy = vsValidateCodeParameter.shallowCopy();

        vsValidateCodeParameterCopy.setCodingParam(codingElement);
        vsValidateCodeParameterCopy.setCodeableConceptParam(null);

        ValidateCodeResult tempResult =
            validateCode(combiningTerminologyService, vsValidateCodeParameterCopy, scope);
        if (tempResult.isResult()) {
          return Optional.of(tempResult);
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Finds the first expansion entry that matches system, version and code constraints.
   *
   * @param expansion value set expansion to inspect
   * @param codeSystem referenced code system metadata
   * @param system code system URL to match
   * @param systemVersion requested code system version, may be {@code null}
   * @param code code to match
   * @return first matching expansion entry if present
   */
  private static Optional<ValueSet.ValueSetExpansionContainsComponent> findMatchingExpansionEntry(
      ValueSet.ValueSetExpansionComponent expansion,
      CodeSystem codeSystem,
      String system,
      String systemVersion,
      String code) {

    // Iterieren durch die Expansion-Elemente und überprüfen auf System, Version und
    // Code-Gleichheit
    CodeSystem finalCodeSystem = codeSystem;
    String finalSystem = system;
    return expansion.getContains().stream()
        .filter(c -> c.getSystem().equals(finalSystem))
        .filter(
            c -> {
              // Wir müssen zwingend die Version prüfen, wenn das Codesystem
              // versionNeeded=true hat
              if (finalCodeSystem.getVersionNeeded()) {
                return c.getVersion().equals(systemVersion);
              }

              // Wenn keine konkrete Version im zu validierende Konzept angegeben ist, dann
              // ist uns jede Version recht
              if (systemVersion == null) {
                return true;
              }

              // Wenn eine Version angegeben ist, dann muss diese mit der Version in der
              // Expansionkomponente übereinstimmen
              return c.getVersion().equals(systemVersion);
            })
        .filter(
            c -> {
              if (finalCodeSystem.getCaseSensitive()) {
                // Wenn das Codesystem caseSensitive=true hat, dann muss der Code exakt
                // übereinstimmen
                return c.getCode().equals(code);
              } else {
                // Wenn das Codesystem caseSensitive=false hat, dann ist die
                // Groß-/Kleinschreibung
                // egal
                return c.getCode().equalsIgnoreCase(code);
              }
            })
        .findFirst();
  }

  /**
   * Creates the final validation result from the optional matching expansion entry.
   *
   * @param match matching expansion entry if available
   * @param display requested display value, may be {@code null}
   * @param scope tenant or namespace scope used for terminology access
   * @param vsValidateCodeParameter validate-code request parameters
   * @param combiningTerminologyService terminology service used for delegated validation
   * @return validation result derived from delegated CodeSystem validation or a negative fallback
   * @throws UnknownScopeException if the provided scope is unknown
   * @throws UnsupportedTerminologyException if referenced terminology artifacts are not supported
   * @throws NotImplementedException if delegated operations are not implemented by a backend
   * @throws UnknownConceptException if delegated code validation reports an unknown concept
   */
  private static ValidateCodeResult buildValidateCodeResultFromMatch(
      Optional<ValueSet.ValueSetExpansionContainsComponent> match,
      String display,
      String scope,
      VsValidateCodeParameter vsValidateCodeParameter,
      CombiningTerminologyService combiningTerminologyService)
      throws UnknownScopeException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownConceptException {

    ValidateCodeResult validateCodeResult = new ValidateCodeResult();

    // Es wurde ein passendes Expansion-Element gefunden, das zum zu prüfenden Konzept passt
    if (match.isPresent()) {

      // TODO: Um die Validierung des Display-Wertes kümmern
      // TODO: Prüfen gegen Wert aus ValueSet-Expansion
      // TODO: Prüfen gegen diverse Ausprägungen des Codesystems
      // TODO: Passenden Display-Wert ermitteln und im Result-Objekt setzen
      // TODO: Sprache berücksichtigen usw.

      // TODO: Wie sah das nochmal mit den aus der VS-Definition übernommenen Displays aus?
      //  Display gegen Expansion prüfen

      CsValidateCodeParameter csValidateCodeParameter =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType(match.get().getSystem()))
              .codeParam(new CodeType(match.get().getCode()))
              .build();

      if (StringUtils.isNoneEmpty(match.get().getVersion())) {
        csValidateCodeParameter.setVersionParam(new StringType(match.get().getVersion()));
      }

      // Szenario: Display-Parameter gesetzt und er matcht nicht mit dem Wert aus der Expansion
      // Entsprechend müssen wir den Display-Wert nochmal gegen das Codesystem prüfen andernfalls
      if (StringUtils.isNoneEmpty(display)) {
        csValidateCodeParameter.setDisplayParam(new StringType(display));
      }

      // displayLanguage-Parameter berücksichtigen
      // TODO: Prüfen ob das auch wirklich in den TerminologyManagern korrekt umgesetzt ist
      TokenParam displayLang = vsValidateCodeParameter.getDisplayLanguageParam();
      if (displayLang != null && StringUtils.isNotEmpty(displayLang.getValue())) {
        csValidateCodeParameter.setDisplayLanguageParam(new CodeType(displayLang.getValue()));
      }

      // Validieren des Konzeptes gegen das Codesystem
      validateCodeResult = combiningTerminologyService.validateCode(scope, csValidateCodeParameter);


      // If only the display is invalid and lenient mode is requested, keep the code validation
      // positive and return a warning.
      if (isLenientDisplayRecoveryRequired(validateCodeResult, display, vsValidateCodeParameter)) {
        validateCodeResult =
            recoverLenientDisplayValidationResult(
                combiningTerminologyService,
                scope,
                csValidateCodeParameter,
                validateCodeResult.getMessage());
      }

    } else {
      validateCodeResult.setResult(false);
      validateCodeResult.setMessage("The provided code is not valid in the ValueSet.");
    }

    return validateCodeResult;
  }

  /**
   * Returns {@code true} when strict display validation failed but lenient display validation is
   * enabled for this request.
   *
   * @param strictValidationResult result produced with the original strict display check
   * @param display requested display value, may be {@code null}
   * @param parameters original ValueSet validate-code parameters
   * @return {@code true} if a lenient re-validation without display should be attempted
   */
  private static boolean isLenientDisplayRecoveryRequired(
      ValidateCodeResult strictValidationResult,
      String display,
      VsValidateCodeParameter parameters) {

    return !strictValidationResult.isResult()
        && StringUtils.isNotBlank(display)
        && parameters.getLenientDisplayValidation().orElse(false);
  }

  /**
   * Re-validates a concept without display and, if successful, converts the strict display failure
   * into a warning according to the lenient-display-validation semantics.
   *
   * @param combiningTerminologyService terminology service used for delegated validation
   * @param scope tenant or namespace scope used for terminology access
   * @param strictParameters CodeSystem validation parameters used for strict validation
   * @param strictFailureMessage message from the strict validation attempt, may be {@code null}
   * @return recovered result with {@code result=true} and warning message when only display failed,
   *     or the fallback result without display when code-level validation still fails
   * @throws UnknownScopeException if the provided scope is unknown
   * @throws UnsupportedTerminologyException if referenced terminology artifacts are not supported
   * @throws NotImplementedException if delegated operations are not implemented by a backend
   * @throws UnknownConceptException if delegated code validation reports an unknown concept
   */
  private static ValidateCodeResult recoverLenientDisplayValidationResult(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      CsValidateCodeParameter strictParameters,
      String strictFailureMessage)
      throws UnknownScopeException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownConceptException {

    CsValidateCodeParameter parametersWithoutDisplay = strictParameters.toBuilder().build();
    // remove display and displayLanguage parameters for lenient re-validation
    parametersWithoutDisplay.setDisplayParam(null);
    parametersWithoutDisplay.setDisplayLanguageParam(null);

    ValidateCodeResult fallbackResult =
        combiningTerminologyService.validateCode(scope, parametersWithoutDisplay);

    if (!fallbackResult.isResult()) {
      return fallbackResult;
    }

    String warningMessage = LENIENT_DISPLAY_WARNING_MESSAGE;
    if (StringUtils.isNotBlank(strictFailureMessage)) {
      warningMessage = strictFailureMessage + " " + LENIENT_DISPLAY_WARNING_MESSAGE;
    }

    fallbackResult.setMessage(warningMessage);
    return fallbackResult;
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

  /**
   * Prüft, ob das referenzierte CodeSystem existiert und ob eine Version benötigt und angegeben
   * wird.
   *
   * @param combiningTerminologyService
   * @param scope
   * @param system
   * @param systemVersion
   * @throws UnsupportedTerminologyException
   */
  @SuppressWarnings("java:S1144") // might be used later
  private static void checkIfReferencedCodeSystemVersionExistsAndAVersionIsNeeded(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      String system,
      String systemVersion)
      throws UnsupportedTerminologyException {

    // CodeSystem laden
    CodeSystem codeSystem =
        loadCodeSystem(scope, system, systemVersion, combiningTerminologyService);

    // Prüfen, ob das referenzierte CodeSystem versionNeeded=true gesetzt hat. In diesem Fall
    // wird zwingend eine Version benötigt
    if (codeSystem.getVersionNeeded() && systemVersion == null) {
      throw new InvalidRequestException(
          "The underlying CodeSystem of the concept to validate is versionAware (versionNeeded = true). Please provide a version.");
    }
  }

  /**
   * Erzeugt die ValueSet-Expansion für das angegebene/enthaltene ValueSet
   *
   * @param scope
   * @param vsValidateCodeParameter
   * @param combiningTerminologyService
   * @return
   * @throws UnsupportedTerminologyException
   * @throws NotImplementedException
   */
  private static ValueSet.ValueSetExpansionComponent loadExpansion(
      String scope,
      VsValidateCodeParameter vsValidateCodeParameter,
      CombiningTerminologyService combiningTerminologyService)
      throws UnsupportedTerminologyException, NotImplementedException {

    // TODO: Zahlenbegrenzung sauber implementieren
    // TODO: Sprachkram und andere Dinge implementieren
    try {

      VsExpandParameter expandParam = null;

      // Unterscheidung verschiedener Szenarien, wie das ValueSet zu finden ist
      if (vsValidateCodeParameter.getUrl().isPresent()) {
        // Szenario 1: Das ValueSet ist über eine URL referenziert

        // system und version des ValueSets ermitteln
        expandParam =
            VsExpandParameter.builder()
                .urlParam(
                    vsValidateCodeParameter.getUrlParam() != null
                        ? new UriType(vsValidateCodeParameter.getUrlParam().getValue())
                        : null)
                .valueSetVersionParam(
                    vsValidateCodeParameter.getValueSetVersionParam() != null
                        ? new StringType(
                            vsValidateCodeParameter.getValueSetVersionParam().getValue())
                        : null)
                .countParam(new IntegerType("1000000"))
                .build();

      } else if (vsValidateCodeParameter.getValueSet().isPresent()) {
        // Szenario 2: Das ValueSet wird im Request übergeben

        expandParam =
            VsExpandParameter.builder()
                .valueSetParam(vsValidateCodeParameter.getValueSetParam())
                .countParam(new IntegerType("1000000"))
                .build();
      } else {
        throw new UnsupportedTerminologyException(
            "Unexpected scenario ($validate-code): Unable to discover ValueSet.");
      }

      // ValueSet expandieren und Ergebnise zurückgeben
      return combiningTerminologyService.expandValueSet(scope, expandParam).getExpansion();

    } catch (Exception e) {
      throw new UnsupportedTerminologyException("Expansion of ValueSet failed: " + e.getMessage());
    }
  }

  private static CodeSystem loadCodeSystem(
      String scope,
      String system,
      String version,
      CombiningTerminologyService combiningTerminologyService)
      throws UnsupportedTerminologyException {

    if (version == null) {
      return combiningTerminologyService.getDefaultCodeSystemByScopeAndUrl(scope, system);
    } else {

      CsSearchParameter csSearchParameter = CsSearchParameter.builder().build();
      csSearchParameter.setSystemParam(new UriParam(system));
      csSearchParameter.setVersionParam(new TokenParam(version));

      List<IdType> csList =
          combiningTerminologyService.searchCodeSystem(scope, csSearchParameter, false);

      if (csList.size() > 1) {
        throw new InvalidRequestException(
            "The CodeSystem(-Version) referenced in the Request is not unique. That should not happen. Talk to the developers.");
      }

      if (csList.isEmpty()) {
        throw new UnsupportedTerminologyException(
            "The CodeSystem(-Version) referenced in the request is not supported.");
      }

      // return the first element in the list or throw an exception
      IdType csId = csList.getFirst();
      if (csId == null) {
        throw new UnsupportedTerminologyException(
            "The CodeSystem(-Version) referenced in the request is not supported.");
      } else {
        return combiningTerminologyService.getCodeSystem(scope, csId);
      }
    }
  }
}
