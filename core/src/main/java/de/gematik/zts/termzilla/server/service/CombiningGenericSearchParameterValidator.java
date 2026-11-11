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
package de.gematik.zts.termzilla.server.service;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.hl7.fhir.utilities.CanonicalPair;

/** Shared validation utilities for terminology search parameter validators. */
public abstract class CombiningGenericSearchParameterValidator extends CommonParameterValidator {

  /** Protected constructor for inheritance-only utility classes. */
  protected CombiningGenericSearchParameterValidator() {}

  /**
   * Validates that {@code _content} does not use unsupported modifiers.
   *
   * @param parameters the search parameters to validate
   * @throws InvalidRequestException if modifiers are used with {@code _content}
   */
  protected static void validateContent(TerminologyResourceSearchParameter parameters)
      throws InvalidRequestException {
    var content = parameters.getContent();
    if (content.isPresent() && hasContentModifiers(content.get())) {
      throw new InvalidRequestException("No modifiers allowed with '_content'-Search-Parameter");
    }
  }

  /**
   * Throws when at least one unsupported parameter is present.
   *
   * @param unsupported whether unsupported parameters are present
   * @param parameterNames unsupported parameter names
   * @throws InvalidRequestException if {@code unsupported} is {@code true}
   */
  protected static void failOnUnsupportedParameters(boolean unsupported, String... parameterNames)
      throws InvalidRequestException {
    if (!unsupported) {
      return;
    }

    if (parameterNames.length == 1) {
      throw new InvalidRequestException(
          "The '" + parameterNames[0] + "' parameter is not supported yet.");
    }

    String joinedNames =
        Arrays.stream(parameterNames)
            .map(name -> "'" + name + "'")
            .collect(Collectors.joining(", "));

    int lastSeparator = joinedNames.lastIndexOf(", ");
    if (lastSeparator > 0) {
      joinedNames =
          joinedNames.substring(0, lastSeparator)
              + " and "
              + joinedNames.substring(lastSeparator + 2);
    }

    throw new InvalidRequestException("The " + joinedNames + " parameters are not supported yet.");
  }

  /**
   * Validates that an explicit {@code version} parameter is not combined with a version embedded in
   * a canonical parameter (for example {@code url|version}).
   *
   * @param parameters the search parameters to validate
   * @param canonicalParam canonical parameter that may include an embedded version (nullable)
   * @param parameterName logical parameter name for error messages (for example {@code url})
   * @throws IllegalArgumentException if both version sources are provided
   */
  protected static void validateVersionNotDuplicated(
      TerminologyResourceSearchParameter parameters, UriParam canonicalParam, String parameterName)
      throws IllegalArgumentException {
    if (parameters.getVersion().isEmpty() || canonicalParam == null) {
      return;
    }

    CanonicalPair canonicalPair = new CanonicalPair(canonicalParam.getValue());
    if (canonicalPair.hasNonEmptyVersion()) {
      throw new IllegalArgumentException(
          "Version must not be set in '"
              + parameterName
              + "' parameter if 'version' parameter is used.");
    }
  }

  /**
   * Convenience helper that validates duplicate version definitions for the {@code url} parameter.
   *
   * @param parameters the search parameters to validate
   * @throws IllegalArgumentException if {@code url} embeds a version while {@code version} is set
   */
  protected static void validateVersionAgainstUrl(TerminologyResourceSearchParameter parameters)
      throws IllegalArgumentException {
    validateVersionNotDuplicated(
        parameters, parameters.getUrl().orElse(null), TerminologyResourceSearchParameter.SP_URL);
  }

  private static boolean hasContentModifiers(StringParam content) {
    return content.isExact()
        || content.isContains()
        || content.isText()
        || (content.getMissing() != null && content.getMissing());
  }
}
