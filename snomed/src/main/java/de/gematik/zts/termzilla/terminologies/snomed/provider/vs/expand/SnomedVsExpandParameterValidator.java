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
package de.gematik.zts.termzilla.terminologies.snomed.provider.vs.expand;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CONCEPT;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.FILTER_PROPERTY_CONSTRAINT;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.common.validator.CommonParameterValidator;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Validates SNOMED-specific parameters for ValueSet $expand operations.
 *
 * <p>The validator enforces structural constraints for compose/include/filter elements and rejects
 * unsupported SNOMED-specific combinations early with {@link InvalidRequestException}.
 */
@Getter
@Setter
public class SnomedVsExpandParameterValidator extends CommonParameterValidator {

  /** Filter properties that evaluate concept relationships (hierarchy or ECL). */
  private static final Set<String> HIERARCHY_BASED_FILTER_PROPERTIES =
      Set.of(FILTER_PROPERTY_CONSTRAINT, PROPERTY_NAME_PARENT, PROPERTY_NAME_CHILD);

  /** Operators of the {@code concept} filter that evaluate the concept hierarchy. */
  private static final Set<ValueSet.FilterOperator> HIERARCHY_BASED_CONCEPT_FILTER_OPERATORS =
      Set.of(ValueSet.FilterOperator.ISA, ValueSet.FilterOperator.DESCENDENTOF);

  /** Error message template for hierarchy-based filters rejected in restricted mode. */
  static final String HIERARCHY_BASED_FILTER_RESTRICTED_MESSAGE =
      "The filter '%s %s' is not available for ValueSets provided within the request, because"
          + " SNOMED CT is provided in restricted mode. Only ValueSets that are part of the"
          + " terminology service's content may use hierarchy-based filters.";

  /** Prevents instantiation of this utility-style validator. */
  private SnomedVsExpandParameterValidator() {
    // private constructor to prevent instantiation
  }

  /**
   * Validates top-level $expand parameters for SNOMED ValueSet expansion.
   *
   * @param parameters parsed ValueSet expansion parameters
   * @throws InvalidRequestException if the provided ValueSet violates required constraints
   */
  public static void validateParameters(VsExpandParameter parameters)
      throws InvalidRequestException {
    failOnImproperValueSet(parameters);
  }

  /** Rejects unsupported or incomplete ValueSet structures. */
  private static void failOnImproperValueSet(VsExpandParameter parameters)
      throws InvalidRequestException {

    ValueSet valueSet = parameters.getValueSetParam();
    if (valueSet != null && !valueSet.isEmpty()) {
      failOnValueSetMissingCompose(valueSet);
      failOnValueSetNonSnomedIncludes(valueSet);
      failOnValueSetHasExclude(valueSet);
    }
  }

  /** Ensures that a provided ValueSet contains a {@code compose} element. */
  private static void failOnValueSetMissingCompose(ValueSet valueSet)
      throws InvalidRequestException {
    // Without a compose element, ValueSet expansion cannot be evaluated.
    if (!valueSet.hasCompose()) {
      throw new InvalidRequestException(
          "ValueSet Expansion requires a compose-element in the resource");
    }
  }

  /** Ensures all include elements reference the SNOMED system. */
  private static void failOnValueSetNonSnomedIncludes(ValueSet valueSet)
      throws InvalidRequestException {
    if (valueSet.hasCompose() && valueSet.getCompose().hasInclude()) {
      // Only SNOMED-based includes are currently supported.

      for (ValueSet.ConceptSetComponent include : valueSet.getCompose().getInclude()) {
        if (!include.getSystem().equals(SnomedConstants.TERMINOLOGY_CANONICAL_NAME)) {
          throw new InvalidRequestException(
              "ValueSet Expansion currently supports only the expansion of SNOMED-based ValueSets");
        }
      }
    }
  }

  /** Rejects ValueSets that define compose exclusions. */
  private static void failOnValueSetHasExclude(ValueSet valueSet) throws InvalidRequestException {
    if (valueSet.hasCompose() && valueSet.getCompose().hasExclude()) {
      // Exclusions are currently not supported.
      throw new InvalidRequestException(
          "ValueSet Expansion currently does not support the exclusion of codes");
    }
  }

  /**
   * Validates one include element of a compose section.
   *
   * @param conceptSetComponent include element to validate
   * @param versionMap supported SNOMED versions keyed by canonical version string
   * @throws InvalidRequestException if include constraints are violated
   */
  public static void validateIncludeElement(
      ValueSet.ConceptSetComponent conceptSetComponent,
      Map<String, SnomedReleaseVersion> versionMap)
      throws InvalidRequestException {

    failOnConceptSetComponentHasNoCodeSystem(conceptSetComponent);
    failOnInvalidSnomedVersion(conceptSetComponent, versionMap);

    failOnConceptSetComponentHasNoConceptOrFilter(conceptSetComponent);
    failOnConceptSetComponentHasConceptAndFilter(conceptSetComponent);
  }

  /** Rejects unsupported SNOMED versions in include elements. */
  private static void failOnInvalidSnomedVersion(
      ValueSet.ConceptSetComponent conceptSetComponent,
      Map<String, SnomedReleaseVersion> versionMap) {

    if (conceptSetComponent.hasVersion()
        && !versionMap.containsKey(conceptSetComponent.getVersion())) {

      throw new InvalidRequestException(
          "ValueSet Expansion does not support the requested version '"
              + conceptSetComponent.getVersion()
              + "' of the code system '"
              + conceptSetComponent.getSystem()
              + "'");
    }
  }

  /** Ensures that an include element defines a code system. */
  private static void failOnConceptSetComponentHasNoCodeSystem(
      ValueSet.ConceptSetComponent conceptSetComponent) {
    if (!conceptSetComponent.hasSystem()) {
      throw new InvalidRequestException(
          "ValueSet Expansion currently requires a code system in the compose-element");
    }
  }

  /** Ensures that an include element defines either concepts or filters. */
  private static void failOnConceptSetComponentHasNoConceptOrFilter(
      ValueSet.ConceptSetComponent conceptSetComponent) {
    if (!conceptSetComponent.hasConcept() && !conceptSetComponent.hasFilter()) {
      throw new InvalidRequestException(
          "ValueSet Expansion currently requires either a concept or a filter in the compose-element");
    }
  }

  /** Rejects include elements that mix explicit concepts and filters. */
  private static void failOnConceptSetComponentHasConceptAndFilter(
      ValueSet.ConceptSetComponent conceptSetComponent) {
    if (conceptSetComponent.hasConcept() && conceptSetComponent.hasFilter()) {
      throw new InvalidRequestException(
          "ValueSet Expansion does not support the inclusion of concepts and filters at the same time in the compose-element");
    }
  }

  /**
   * Validates one compose filter component for supported SNOMED usage.
   *
   * @param conceptSetFilterComponent filter component to validate
   * @throws InvalidRequestException if required fields are missing or combination is unsupported
   */
  public static void validateConceptSetFilterComponent(
      ValueSet.ConceptSetFilterComponent conceptSetFilterComponent) throws InvalidRequestException {
    failOnMissingProperty(conceptSetFilterComponent);
    failOnMissingOperation(conceptSetFilterComponent);
    failOnMissingValue(conceptSetFilterComponent);
    failOnUnsupportedPropertyOperationCombination(conceptSetFilterComponent);
  }

  /** Ensures that a filter component defines a value. */
  private static void failOnMissingValue(
      ValueSet.ConceptSetFilterComponent conceptSetFilterComponent) {
    if (!conceptSetFilterComponent.hasValue()) {
      throw new InvalidRequestException(
          "ValueSet Expansion requires a value in the filter-element");
    }
  }

  /** Ensures that a filter component defines an operator. */
  private static void failOnMissingOperation(
      ValueSet.ConceptSetFilterComponent conceptSetFilterComponent) {
    if (!conceptSetFilterComponent.hasOp()) {
      throw new InvalidRequestException(
          "ValueSet Expansion requires an operation in the filter-element");
    }
  }

  /** Ensures that a filter component defines a property. */
  private static void failOnMissingProperty(
      ValueSet.ConceptSetFilterComponent conceptSetFilterComponent) {
    if (!conceptSetFilterComponent.hasProperty()) {
      throw new InvalidRequestException(
          "ValueSet Expansion requires a property in the filter-element");
    }
  }

  /** Ensures that only supported property/operator combinations are used. */
  private static void failOnUnsupportedPropertyOperationCombination(
      ValueSet.ConceptSetFilterComponent conceptSetFilterComponent) {
    if (conceptSetFilterComponent.hasProperty() && conceptSetFilterComponent.hasOp()) {
      boolean isConceptIsa =
          conceptSetFilterComponent.getProperty().equals(PROPERTY_NAME_CONCEPT)
              && conceptSetFilterComponent.getOp().equals(ValueSet.FilterOperator.ISA);
      boolean isConstraintEqual =
          conceptSetFilterComponent.getProperty().equals(FILTER_PROPERTY_CONSTRAINT)
              && conceptSetFilterComponent.getOp().equals(ValueSet.FilterOperator.EQUAL);

      if (!isConceptIsa && !isConstraintEqual) {
        throw new InvalidRequestException(
            "ValueSet Expansion does not support the combination of property and operation in the filter-element");
      }
    }
  }

  /**
   * Rejects filters that evaluate concept relationships. Used in restricted mode for ValueSets
   * provided within the request, before any access to the concept graph.
   *
   * <p>Rejected are the properties {@code constraint}, {@code parent} and {@code child} as well as
   * {@code concept} with the operators {@code is-a} and {@code descendent-of}. The filter {@code
   * concept in} (reference set membership) remains available.
   *
   * @param conceptSetComponent include or exclude element whose filters are checked
   * @throws InvalidRequestException if a hierarchy-based filter is used
   */
  public static void failOnHierarchyBasedFilters(ValueSet.ConceptSetComponent conceptSetComponent)
      throws InvalidRequestException {
    for (ValueSet.ConceptSetFilterComponent filter : conceptSetComponent.getFilter()) {
      if (isHierarchyBasedFilter(filter)) {
        throw new InvalidRequestException(
            String.format(
                HIERARCHY_BASED_FILTER_RESTRICTED_MESSAGE,
                filter.getProperty(),
                filter.hasOp() ? filter.getOp().toCode() : ""));
      }
    }
  }

  /** Checks whether a filter evaluates concept relationships. */
  private static boolean isHierarchyBasedFilter(ValueSet.ConceptSetFilterComponent filter) {
    // Set.of(...) lehnt contains(null) ab, daher fehlende Angaben vorher abfangen
    if (!filter.hasProperty()) {
      return false;
    }
    String property = filter.getProperty();
    if (HIERARCHY_BASED_FILTER_PROPERTIES.contains(property)) {
      return true;
    }
    return PROPERTY_NAME_CONCEPT.equals(property)
        && filter.hasOp()
        && HIERARCHY_BASED_CONCEPT_FILTER_OPERATORS.contains(filter.getOp());
  }
}
