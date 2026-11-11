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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.lookup;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;

/**
 * Utility class for validating input parameters of the SNOMED CT {@code CodeSystem/$lookup}
 * operation.
 *
 * <p>Checks for parameters that are syntactically valid according to the FHIR specification but not
 * yet supported by the SNOMED implementation. Unsupported parameters cause a {@link
 * NotImplementedException} to be thrown early, before any terminology lookup is attempted.
 *
 * <p>In restricted mode, properties exposing concept relationships or the definition status are
 * rejected with an {@link InvalidRequestException}.
 */
public class SnomedCsLookupParameterValidator {

  /** Properties that must not be returned in restricted mode. */
  private static final Set<String> RESTRICTED_MODE_PROPERTIES =
      Set.of(
          ConceptProperties.PROPERTY_NAME_CHILD,
          ConceptProperties.PROPERTY_NAME_PARENT,
          ConceptProperties.PROPERTY_NAME_SUFFICIENTLY_DEFINED);

  /** Private constructor to prevent instantiation of this utility class. */
  private SnomedCsLookupParameterValidator() {}

  /**
   * Validates the given {@link CsLookupParameter} for unsupported parameter combinations.
   *
   * <p>Rejects the {@code date} parameter with a {@link NotImplementedException}. In restricted
   * mode, requesting a property that exposes concept relationships or the definition status is
   * rejected with an {@link InvalidRequestException}.
   *
   * @param parameters the lookup parameters to check
   * @param restrictedMode whether SNOMED CT is provided in restricted mode
   * @throws NotImplementedException if any unsupported parameter is present
   * @throws InvalidRequestException if a restricted property is requested in restricted mode
   */
  public static void validateParameters(CsLookupParameter parameters, boolean restrictedMode)
      throws NotImplementedException, InvalidRequestException {
    failOnDateParam(parameters.getDateParam());
    if (restrictedMode) {
      failOnRestrictedProperties(parameters.getPropertyParam());
    }
  }

  /**
   * Throws a {@link NotImplementedException} if the {@code date} parameter is set.
   *
   * @param dateParam the date parameter value, or {@code null} if not provided
   * @throws NotImplementedException if {@code dateParam} is not {@code null}
   */
  private static void failOnDateParam(DateTimeType dateParam) throws NotImplementedException {
    if (dateParam != null) {
      throw new NotImplementedException(
          "Parameter '" + CsLookupParameter.DATE_PARAM + "' currently not supported.");
    }
  }

  /**
   * Throws an {@link InvalidRequestException} if any property is requested that is not available in
   * restricted mode.
   *
   * @param propertyParams the requested {@code property} parameters, or {@code null}
   * @throws InvalidRequestException if a restricted property is requested
   */
  private static void failOnRestrictedProperties(List<CodeType> propertyParams)
      throws InvalidRequestException {
    if (propertyParams == null) {
      return;
    }
    Set<String> restricted =
        propertyParams.stream()
            .filter(Objects::nonNull)
            .map(CodeType::getValue)
            .filter(RESTRICTED_MODE_PROPERTIES::contains)
            .collect(Collectors.toCollection(TreeSet::new));
    if (!restricted.isEmpty()) {
      throw new InvalidRequestException(
          "The requested properties "
              + restricted
              + " are not available, because SNOMED CT is provided in restricted mode.");
    }
  }
}
