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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.findmatches;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.common.validator.BaseCodeSystemFindMatchesParameterValidator;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;

/**
 * Validator for SNOMED CT CodeSystem $find-matches operation. Adds SNOMED-specific validation:
 * maximum 3 properties and no subproperties. In restricted mode, property filters on concept
 * relationships ({@code parent}, {@code child}) are rejected.
 */
public class SnomedCodeSystemFindMatchesParameterValidator
    extends BaseCodeSystemFindMatchesParameterValidator {

  /** Property filters that must not be used in restricted mode. */
  private static final Set<String> RESTRICTED_MODE_PROPERTIES =
      Set.of(ConceptProperties.PROPERTY_NAME_PARENT, ConceptProperties.PROPERTY_NAME_CHILD);

  private final boolean restrictedMode;

  /**
   * Creates a validator for the given operating mode.
   *
   * @param restrictedMode whether SNOMED CT is provided in restricted mode
   */
  public SnomedCodeSystemFindMatchesParameterValidator(boolean restrictedMode) {
    this.restrictedMode = restrictedMode;
  }

  /**
   * {@inheritDoc}
   *
   * <p>In addition to base validation, SNOMED requires at least one {@code property} parameter and
   * allows at most three property parameters.
   */
  @Override
  protected void validateProperties(CsFindMatchesParameter parameters) {
    validateAtLeastOnePropertyExists(parameters);
    validateNotMoreThanThreePropertiesExists(parameters);

    if (parameters.getPropertyParamList() != null) {
      parameters.getPropertyParamList().forEach(this::validateProperty);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>SNOMED-specific validation forbids nested subproperties. In restricted mode, property
   * filters on concept relationships are rejected.
   */
  @Override
  protected void validateProperty(Parameters.ParametersParameterComponent property) {
    super.validateProperty(property);
    validateNoSubProperty(property);
    if (restrictedMode) {
      failOnRestrictedProperty(property);
    }
  }

  /**
   * Throws an {@link InvalidRequestException} if the property filter refers to a property that is
   * not available in restricted mode.
   *
   * @param property a single {@code property} parameter (already validated by the base class)
   * @throws InvalidRequestException if a restricted property is used
   */
  private void failOnRestrictedProperty(Parameters.ParametersParameterComponent property) {
    property.getPart().stream()
        .filter(part -> part.getName().equals("code"))
        .map(part -> ((CodeType) part.getValue()).getValue())
        .filter(RESTRICTED_MODE_PROPERTIES::contains)
        .findFirst()
        .ifPresent(
            code -> {
              throw new InvalidRequestException(
                  "The property '"
                      + code
                      + "' is not available, because SNOMED CT is provided in restricted mode.");
            });
  }

  /**
   * Ensures that no more than three {@code property} parameters are provided.
   *
   * @param parameters parsed $find-matches parameters
   * @throws InvalidRequestException if more than three property parameters are present
   */
  private void validateNotMoreThanThreePropertiesExists(CsFindMatchesParameter parameters) {
    if (parameters.getPropertyParamList() != null && parameters.getPropertyParamList().size() > 3) {
      throw new InvalidRequestException(
          "At most three 'property' parameters are allowed when using $find-matches");
    }
  }
}
