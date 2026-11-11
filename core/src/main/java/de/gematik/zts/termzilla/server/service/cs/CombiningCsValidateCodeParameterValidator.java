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
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Validates the input parameters for the {@code CodeSystem/$validate-code} operation.
 *
 * <p>Enforces the following rules in order:
 *
 * <ol>
 *   <li>The {@code codeSystem} and {@code date} parameters are not yet supported and cause an
 *       immediate rejection.
 *   <li>At least one of {@code url}, {@code coding}, or {@code codeableConcept} must be present.
 *   <li>The parameter variants {@code url}/{@code code}/{@code version}, {@code coding}, and {@code
 *       codeableConcept} are mutually exclusive.
 *   <li>If {@code url} is present, {@code code} must also be provided.
 * </ol>
 */
@Getter
@Setter
public class CombiningCsValidateCodeParameterValidator extends CommonParameterValidator {

  private CombiningCsValidateCodeParameterValidator() {}

  /**
   * Validates the given {@link CsValidateCodeParameter} against all applicable rules.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if any validation rule is violated
   */
  public static void validateParameters(CsValidateCodeParameter parameters)
      throws InvalidRequestException {
    failOnUnsupportedParameters(parameters);
    failOnMissingUrlOrCodingOrCodeableConceptParameter(parameters);
    failOnUrlXorCodableConceptXorCodingParameter(parameters);
    failOnUrlWithoutCodeParameter(parameters);
    failOnDisplayLanguageWithoutDisplayParameter(parameters);
    // TODO: Keep validateDateNotInFuture for later activation once 'date' becomes supported.
  }

  // Entweder eine url oder ein valueSet muss angegeben sein

  /**
   * Rejects the request if none of {@code url}, {@code coding}, or {@code codeableConcept} is
   * present, as at least one of them is required to identify the code to validate.
   *
   * @throws InvalidRequestException if all three parameters are absent or empty
   */
  private static void failOnMissingUrlOrCodingOrCodeableConceptParameter(
      CsValidateCodeParameter parameters) throws InvalidRequestException {

    if ((parameters.getUrlParam() == null || !parameters.getUrlParam().hasValue())
        && (parameters.getCodingParam() == null || parameters.getCodingParam().isEmpty())
        && (parameters.getCodeableConceptParam() == null
            || parameters.getCodeableConceptParam().isEmpty())) {
      throw new InvalidRequestException(
          "Either the '"
              + CsValidateCodeParameter.URL_PARAM
              + "' or the '"
              + CsValidateCodeParameter.CODING_PARAM
              + "' or the '"
              + CsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameter must be provided.");
    }
  }

  // Bestimmte Parameter unterstützen wir bisher einfach nicht

  /**
   * Rejects the request if it contains parameters that are not yet supported by this
   * implementation, specifically {@code codeSystem} and {@code date}.
   *
   * @throws InvalidRequestException if {@code codeSystem} or {@code date} is present
   */
  private static void failOnUnsupportedParameters(CsValidateCodeParameter parameters)
      throws InvalidRequestException {

    if ((parameters.getCodeSystemParam() != null && !parameters.getCodeSystemParam().isEmpty())
        || (parameters.getDateParam() != null && parameters.getDateParam().hasValue())) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.CODESYSTEM_PARAM
              + "' and '"
              + CsValidateCodeParameter.DATE_PARAM
              + "' parameters are not supported yet.");
    }
  }

  /**
   * Rejects the request if more than one of the three mutually exclusive parameter variants is
   * present simultaneously: {@code url}/{@code code}/{@code version}, {@code coding}, and {@code
   * codeableConcept} must not be combined with each other.
   *
   * @throws InvalidRequestException if any two of the three variants are present at the same time
   */
  private static void failOnUrlXorCodableConceptXorCodingParameter(
      CsValidateCodeParameter parameters) throws InvalidRequestException {
    if (((parameters.getUrlParam() != null && parameters.getUrlParam().hasValue())
            || parameters.getCodeParam() != null
            || (parameters.getVersionParam() != null && parameters.getVersionParam().hasValue()))
        && (parameters.getCodingParam() != null && !parameters.getCodingParam().isEmpty())) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.CODE_PARAM
              + "' and the '"
              + CsValidateCodeParameter.CODING_PARAM
              + "' parameter must not be provided at the same time.");
    }
    if (((parameters.getUrlParam() != null && parameters.getUrlParam().hasValue())
            || parameters.getCodeParam() != null
            || (parameters.getVersionParam() != null && parameters.getVersionParam().hasValue()))
        && (parameters.getCodeableConceptParam() != null
            && !parameters.getCodeableConceptParam().isEmpty())) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.CODE_PARAM
              + "' and the '"
              + CsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameter must not be provided at the same time.");
    }
    if ((parameters.getCodingParam() != null && !parameters.getCodingParam().isEmpty())
        && (parameters.getCodeableConceptParam() != null
            && !parameters.getCodeableConceptParam().isEmpty())) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.CODING_PARAM
              + "' and the '"
              + CsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + "' parameter must not be provided at the same time.");
    }
  }

  /**
   * Rejects the request if {@code url} is provided without a corresponding {@code code} parameter,
   * since a URL alone is insufficient to identify the concept to validate.
   *
   * @throws InvalidRequestException if {@code url} is present but {@code code} is absent
   */
  private static void failOnUrlWithoutCodeParameter(CsValidateCodeParameter parameters)
      throws InvalidRequestException {
    if ((parameters.getUrlParam() != null && parameters.getUrlParam().hasValue())
        && parameters.getCodeParam() == null) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.CODE_PARAM
              + "' parameter must be provided if the '"
              + CsValidateCodeParameter.URL_PARAM
              + "' parameter is provided.");
    }
  }

  /**
   * Rejects the request if {@code displayLanguage} is provided without any display information.
   * Display information can be provided via {@code display}, {@code coding.display}, or via at
   * least one {@code codeableConcept.coding.display} value.
   *
   * @throws InvalidRequestException if {@code displayLanguage} is set but no display is available
   */
  private static void failOnDisplayLanguageWithoutDisplayParameter(
      CsValidateCodeParameter parameters) throws InvalidRequestException {
    if ((parameters.getDisplayLanguageParam() != null
            && parameters.getDisplayLanguageParam().hasValue())
        && !hasAnyDisplayInformation(parameters)) {
      throw new InvalidRequestException(
          "The '"
              + CsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM
              + "' parameter requires display information in '"
              + CsValidateCodeParameter.DISPLAY_PARAM
              + "', '"
              + CsValidateCodeParameter.CODING_PARAM
              + ".display', or '"
              + CsValidateCodeParameter.CODEABLECONCEPT_PARAM
              + ".coding.display'.");
    }
  }

  /**
   * Checks whether display information is present in any supported source. Supported sources are
   * {@code display}, {@code coding.display}, and {@code codeableConcept.coding.display}.
   *
   * @param parameters the parameters to inspect
   * @return {@code true} if at least one supported display source contains a value
   */
  private static boolean hasAnyDisplayInformation(CsValidateCodeParameter parameters) {
    if (parameters.getDisplayParam() != null && parameters.getDisplayParam().hasValue()) {
      return true;
    }

    if (parameters.getCodingParam() != null
        && parameters.getCodingParam().hasDisplayElement()
        && parameters.getCodingParam().getDisplayElement().hasValue()) {
      return true;
    }

    if (parameters.getCodeableConceptParam() == null
        || parameters.getCodeableConceptParam().isEmpty()) {
      return false;
    }

    return parameters.getCodeableConceptParam().getCoding().stream()
        .anyMatch(coding -> coding.hasDisplayElement() && coding.getDisplayElement().hasValue());
  }

  /**
   * Rejects the request if {@code date} is set to a future instant.
   *
   * <p>This method is currently not invoked because {@code date} is handled as unsupported.
   *
   * @param parameters the parameters to validate
   * @throws InvalidRequestException if {@code date} is in the future
   */
  @SuppressWarnings("unused")
  private static void validateDateNotInFuture(CsValidateCodeParameter parameters)
      throws InvalidRequestException {
    if (parameters.getDateParam() == null || !parameters.getDateParam().hasValue()) {
      return;
    }

    if (parameters.getDateParam().getValue().after(new Date())) {
      throw new InvalidRequestException(
          "Parameter " + CsValidateCodeParameter.DATE_PARAM + " must not be in the future.");
    }
  }
}
