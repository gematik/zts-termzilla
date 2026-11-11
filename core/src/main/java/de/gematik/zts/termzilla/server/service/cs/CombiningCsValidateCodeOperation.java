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
package de.gematik.zts.termzilla.server.service.cs;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * Utility class implementing the {@code CodeSystem/$validate-code} operation.
 *
 * <p>Routes validation requests to the responsible terminology provider, resolved via {@link
 * CombiningTerminologyService}, based on the supplied parameter variant. Three mutually exclusive
 * input combinations are supported:
 *
 * <ul>
 *   <li><b>system parameter</b> — validates a code directly against the CodeSystem identified by
 *       {@link CsValidateCodeParameter#getUrlParam()}
 *   <li><b>coding parameter</b> — expands the {@link org.hl7.fhir.r4.model.Coding} into individual
 *       system, version, code, and display parameters before delegating to case 1
 *   <li><b>codeableConcept parameter</b> — iterates over all contained codings and returns success
 *       if at least one coding is valid
 * </ul>
 */
@Slf4j
public class CombiningCsValidateCodeOperation {

  private CombiningCsValidateCodeOperation() {
    // private constructor to prevent instantiation
  }

  /**
   * Validates a code against a CodeSystem by dispatching to the appropriate handler based on the
   * supplied parameter variant (system, coding, or codeableConcept).
   *
   * @param combiningTerminologyService the terminology service used to resolve the responsible
   *     provider and delegate validation
   * @param csValidateCodeParameter the parameters describing the validation request
   * @param scope the scope restricting the set of available terminologies
   * @param languageSystemDefault the system default language
   * @return the {@link ValidateCodeResult} indicating whether the code (and optional display) is
   *     valid
   * @throws InvalidRequestException if none of the expected parameter combinations is present or no
   *     CodeSystem URL can be determined
   * @throws UnsupportedTerminologyException if the requested terminology is not supported
   * @throws NotImplementedException if the requested parameter combination is not yet implemented
   * @throws UnknownScopeException if the given scope is not known
   * @throws UnknownConceptException if no concept with the given code exists in the CodeSystem
   */
  public static ValidateCodeResult validateCode(
      CombiningTerminologyService combiningTerminologyService,
      CsValidateCodeParameter csValidateCodeParameter,
      String scope,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Case 1: system parameter
    UriType urlParam = csValidateCodeParameter.getUrlParam();
    if (urlParam != null && urlParam.hasValue() && StringUtils.isNotEmpty(urlParam.getValue())) {
      return validateWithSystemParameter(
          combiningTerminologyService,
          urlParam.getValue(),
          csValidateCodeParameter,
          scope,
          languageSystemDefault);
    }

    // Case 2: coding parameter
    Coding codingParam = csValidateCodeParameter.getCodingParam();
    if (codingParam != null
        && !codingParam.isEmpty()
        && codingParam.hasSystem()
        && StringUtils.isNotEmpty(codingParam.getSystem())) {
      return validateWithCodingParameter(
          combiningTerminologyService,
          codingParam,
          csValidateCodeParameter,
          scope,
          languageSystemDefault);
    }

    // Case 3: codeableConcept parameter
    CodeableConcept codeableConceptParam = csValidateCodeParameter.getCodeableConceptParam();
    if (codeableConceptParam != null
        && !codeableConceptParam.isEmpty()
        && !codeableConceptParam.getCoding().isEmpty()) {
      return validateWithCodeableConceptParameter(
          combiningTerminologyService,
          codeableConceptParam,
          csValidateCodeParameter,
          scope,
          languageSystemDefault);
    }

    // If none of the cases apply, no terminology can be determined
    throw new InvalidRequestException("No Url found in $validate-code Parameters");
  }

  // ----------------------------------------------------------------------------------------
  // Validation Methods
  // ----------------------------------------------------------------------------------------

  /**
   * Validates a code using the system URL parameter by routing directly to the responsible
   * terminology provider.
   *
   * @param combiningTerminologyService the terminology service used to resolve the provider
   * @param system the CodeSystem URL identifying the target terminology
   * @param parameters the full validate-code parameters
   * @param scope the scope restricting available terminologies
   * @param languageSystemDefault the system default language
   * @return the validation result from the responsible terminology provider
   * @throws UnsupportedTerminologyException if no provider supports the given system URL
   * @throws UnknownScopeException if the given scope is not known
   * @throws NotImplementedException if the operation is not implemented by the provider
   * @throws UnknownConceptException if no concept with the given code exists
   */
  private static ValidateCodeResult validateWithSystemParameter(
      CombiningTerminologyService combiningTerminologyService,
      String system,
      CsValidateCodeParameter parameters,
      String scope,
      LanguageParameter languageSystemDefault)
      throws UnsupportedTerminologyException,
          UnknownScopeException,
          NotImplementedException,
          UnknownConceptException {

    return combiningTerminologyService
        .getResponsibleTerminologyManager(system)
        .cs()
        .validateCode(scope, parameters, languageSystemDefault);
  }

  /**
   * Validates a code using a {@link Coding} parameter by first expanding the coding into individual
   * system, version, code, and display parameters, then delegating to the system parameter path. A
   * copy of the original parameters is used to avoid side effects.
   *
   * @param combiningTerminologyService the terminology service used to resolve the provider
   * @param coding the coding containing system, version, code, and display
   * @param parameters the original validate-code parameters
   * @param scope the scope restricting available terminologies
   * @param languageSystemDefault the system default language
   * @return the validation result from the responsible terminology provider
   * @throws InvalidRequestException if no CodeSystem URL can be determined from the coding
   * @throws UnsupportedTerminologyException if no provider supports the given system URL
   * @throws NotImplementedException if the operation is not implemented by the provider
   * @throws UnknownScopeException if the given scope is not known
   * @throws UnknownConceptException if no concept with the given code exists
   */
  private static ValidateCodeResult validateWithCodingParameter(
      CombiningTerminologyService combiningTerminologyService,
      Coding coding,
      CsValidateCodeParameter parameters,
      String scope,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Create a copy to avoid modifying original parameters
    CsValidateCodeParameter parametersCopy = parameters.toBuilder().build();

    // Transform coding into individual parameters (system, version, code, display)
    mapCodingToParameters(parametersCopy, coding);

    // Clear coding parameter to avoid falling into case 3
    parametersCopy.setCodingParam(null);

    log.debug("validateCode: coding-Parameter: {}", parametersCopy);

    return validateCode(combiningTerminologyService, parametersCopy, scope, languageSystemDefault);
  }

  /**
   * Validates a code using a {@link CodeableConcept} parameter by iterating over all contained
   * {@link Coding} elements. The overall result is successful if at least one coding validates
   * successfully; the result is updated with the first valid coding found.
   *
   * @param combiningTerminologyService the terminology service used to resolve the provider
   * @param codeableConcept the codeableConcept containing one or more codings
   * @param parameters the original validate-code parameters
   * @param scope the scope restricting available terminologies
   * @param languageSystemDefault the system default language
   * @return the validation result; {@code result} is {@code true} if at least one coding is valid
   * @throws InvalidRequestException if no CodeSystem URL can be determined for a coding
   * @throws UnsupportedTerminologyException if no provider supports the given system URL
   * @throws NotImplementedException if the operation is not implemented by the provider
   * @throws UnknownScopeException if the given scope is not known
   * @throws UnknownConceptException if no concept with the given code exists
   */
  private static ValidateCodeResult validateWithCodeableConceptParameter(
      CombiningTerminologyService combiningTerminologyService,
      CodeableConcept codeableConcept,
      CsValidateCodeParameter parameters,
      String scope,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    // Initialize result object
    ValidateCodeResult result = createInitialCodeableConceptResult(codeableConcept);

    // Validate each coding in the codeableConcept; only one needs to be valid
    for (Coding coding : codeableConcept.getCoding()) {
      ValidateCodeResult codingResult =
          validateSingleCodingFromCodeableConcept(
              combiningTerminologyService, parameters, coding, scope, languageSystemDefault);

      if (codingResult.isResult()) {
        updateResultWithValidCoding(result, codingResult);
        // TODO: Issues should still be collected even for invalid codings. Logic needs adjustment.
      }
    }

    return result;
  }

  /**
   * Validates a single {@link Coding} extracted from a {@link CodeableConcept} by setting it as the
   * coding parameter on a parameter copy and delegating back to {@link #validateCode}.
   *
   * @param combiningTerminologyService the terminology service used to resolve the provider
   * @param parameters the original validate-code parameters
   * @param coding the coding to validate
   * @param scope the scope restricting available terminologies
   * @param languageSystemDefault the system default language
   * @return the validation result for the given coding
   * @throws InvalidRequestException if no CodeSystem URL can be determined
   * @throws UnsupportedTerminologyException if no provider supports the given system URL
   * @throws NotImplementedException if the operation is not implemented by the provider
   * @throws UnknownScopeException if the given scope is not known
   * @throws UnknownConceptException if no concept with the given code exists
   */
  private static ValidateCodeResult validateSingleCodingFromCodeableConcept(
      CombiningTerminologyService combiningTerminologyService,
      CsValidateCodeParameter parameters,
      Coding coding,
      String scope,
      LanguageParameter languageSystemDefault)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException,
          UnknownConceptException {

    CsValidateCodeParameter parametersCopy = parameters.toBuilder().build();
    parametersCopy.setCodingParam(coding);
    parametersCopy.setCodeableConceptParam(null);

    return validateCode(combiningTerminologyService, parametersCopy, scope, languageSystemDefault);
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

  /**
   * Copies the system, version, code, and display properties from a {@link Coding} into the
   * corresponding fields of the given {@link CsValidateCodeParameter}.
   *
   * @param parameters the parameter object to populate; modified in place
   * @param coding the source coding whose fields are copied
   */
  private static void mapCodingToParameters(CsValidateCodeParameter parameters, Coding coding) {
    if (coding.hasSystem()) {
      parameters.setUrlParam(new UriType(coding.getSystem()));
    }
    if (coding.hasVersion()) {
      parameters.setVersionParam(new StringType(coding.getVersion()));
    }
    if (coding.hasCode()) {
      parameters.setCodeParam(new CodeType(coding.getCode()));
    }
    if (coding.hasDisplay()) {
      parameters.setDisplayParam(new StringType(coding.getDisplay()));
    }
  }

  /**
   * Creates an initial {@link ValidateCodeResult} for a {@link CodeableConcept} validation, preset
   * to {@code false} and referencing the given codeableConcept.
   *
   * @param codeableConcept the codeableConcept to attach to the result
   * @return a new result with {@code result=false} and the given codeableConcept set
   */
  private static ValidateCodeResult createInitialCodeableConceptResult(
      CodeableConcept codeableConcept) {
    ValidateCodeResult result = new ValidateCodeResult();
    result.setResult(false);
    result.setCodeableConcept(codeableConcept);
    return result;
  }

  /**
   * Transfers code, display, system, and version from a successful coding result into the aggregate
   * result and marks it as valid.
   *
   * @param result the aggregate result to update; modified in place
   * @param codingResult the successful single-coding result to transfer values from
   */
  private static void updateResultWithValidCoding(
      ValidateCodeResult result, ValidateCodeResult codingResult) {
    result.setResult(true);
    result.setCode(codingResult.getCode());
    result.setDisplay(codingResult.getDisplay());
    result.setSystem(codingResult.getSystem());
    result.setVersion(codingResult.getVersion());
  }
}
