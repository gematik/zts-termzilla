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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CODE;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DEFINITION;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DISPLAY;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.fhirterms.graph.FhirTermGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Processes FHIR ValueSet filter operations using in-memory data structures.
 *
 * <p>Supports the following filter operators:
 *
 * <ul>
 *   <li>{@code =} (equals) - Property value equality matching
 *   <li>{@code regex} - Regular expression matching on string/code properties
 *   <li>{@code exists} - Property existence check (for parent/child relationships)
 *   <li>{@code is-a} - Subsumption relationship (not yet implemented)
 *   <li>{@code is-not-a} - Negated subsumption (not yet implemented)
 *   <li>{@code generalizes} - Ancestor relationship (not yet implemented)
 *   <li>{@code descendent-of} - Descendants only (not yet implemented)
 *   <li>{@code in} - Set membership (not yet implemented)
 * </ul>
 *
 * <p>For hierarchy-based filters ({@code exists}, equals on parent/child), this processor uses
 * {@link FhirTermGraphRepresentation} for graph-based lookups.
 */
@Slf4j
class InMemoryFilterProcessor {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates a new in-memory filter processor.
   *
   * @param fhirTermsInMemoryRepresentation the in-memory representation for graph access
   */
  InMemoryFilterProcessor(FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation) {
    this.fhirTermsInMemoryRepresentation = fhirTermsInMemoryRepresentation;
  }

  /**
   * Dispatches a single filter to the appropriate in-memory processing method.
   *
   * @param includeFilter the filter component to process
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter concepts from
   */
  void processFilter(
      ValueSet.ConceptSetFilterComponent includeFilter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    /*
    Code	        Display	                            Definition
    =	            Equals	                            The specified property of the code equals the provided value.
    is-a	        Is A (by subsumption)	            Includes all concept ids that have a transitive is-a relationship with the concept Id provided as the value, including the provided concept itself (include descendant codes and self).
    descendent-of	Descendent Of (by subsumption)	    Includes all concept ids that have a transitive is-a relationship with the concept Id provided as the value, excluding the provided concept itself i.e. include descendant codes only).
    is-not-a	    Not (Is A) (by subsumption)	        The specified property of the code does not have an is-a relationship with the provided value.
    regex	        Regular Expression	                The specified property of the code matches the regex specified in the provided value.
    in	            In Set	                            The specified property of the code is in the set of codes or concepts specified in the provided value (comma separated list).
    not-in	        Not in Set	                        The specified property of the code is not in the set of codes or concepts specified in the provided value (comma separated list).
    generalizes	    Generalizes (by Subsumption)	    Includes all concept ids that have a transitive is-a relationship from the concept Id provided as the value, including the provided concept itself (i.e. include ancestor codes and self).
    exists	        Exists	                            The specified property of the code has at least one value (if the specified value is true; if the specified value is false, then matches when the specified property of the code has no values).
     */

    // Ab hier startet die Verarbeitung der Filter. Es bietet sich eine Unterscheidung nach der
    // Operation an.
    ValueSet.FilterOperator operator = includeFilter.getOp();

    switch (operator) {
      case EQUAL:
        processEqualsFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case REGEX:
        processRegexpFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case EXISTS:
        processExistsFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case ISA:
        processIsAFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case ISNOTA:
        processIsNotAFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case GENERALIZES:
        processGeneralizesFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case DESCENDENTOF:
        processDecendentOfFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      case IN:
        processInFilter(
            includeFilter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
        break;
      default:
        // Alle anderen Filter sind nicht unterstützt
        throw new InvalidRequestException(
            "The given ValueSet definition includes a filter with an unsupported operation: "
                + operator.toCode());
    }
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes an exists filter operation.
   *
   * <p>Currently supports only {@code parent} and {@code child} properties for exists filtering.
   *
   * @param filter the filter component containing the exists operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processExistsFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    // Property-Existenz wurde bereits in ExpandFilterValidator.validateFilters() geprüft

    String propertyCode = filter.getProperty();

    // Sonderbehandlung von 'parent' und 'child'-Filtern (graph-basiert)
    if (Objects.equals(propertyCode, PROPERTY_NAME_CHILD)
        || Objects.equals(propertyCode, PROPERTY_NAME_PARENT)) {
      processParentChildExistsFilter(
          filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
      return;
    }

    // Alle anderen Properties: Iteration über alle Konzepte
    boolean exists = Objects.equals(filter.getValue(), "true");
    String system = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    List<CodeSystem.ConceptDefinitionComponent> allConcepts =
        CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {
      boolean propertyExists = conceptHasProperty(concept, propertyCode);
      if (propertyExists == exists
          && ExpandTextFilter.matchesFilter(system, concept, resultFilter)) {
        ExpandResultHelper.extractBaseConcept(
            system, version, includeInactiveConcepts, filterResultSet, concept);
      }
    }
  }

  /**
   * Checks whether a concept has a value for the specified property.
   *
   * <p>Handles implicit properties ({@code code}, {@code display}, {@code definition}) via direct
   * field access on the concept. All other properties are checked against the concept's explicit
   * property list.
   *
   * @param concept the concept to check
   * @param propertyCode the property code to look for
   * @return {@code true} if the concept has at least one value for the property
   */
  private static boolean conceptHasProperty(
      CodeSystem.ConceptDefinitionComponent concept, String propertyCode) {

    return switch (propertyCode) {
      case PROPERTY_NAME_CODE -> concept.hasCode();
      case PROPERTY_NAME_DISPLAY -> concept.hasDisplay();
      case PROPERTY_NAME_DEFINITION ->
          concept.getDefinition() != null && !concept.getDefinition().isEmpty();
      default ->
          concept.getProperty().stream()
              .anyMatch(p -> p.hasCode() && p.getCode().equals(propertyCode));
    };
  }

  /**
   * Processes a regex filter operation.
   *
   * <p>Applies regular expression matching to concept property values. Supports {@code code}
   * property as a special case matching directly against concept codes. Only string and code type
   * properties are supported for regex filtering.
   *
   * @param filter the filter component containing the regex operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processRegexpFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    String system = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    // Property-Existenz und Regex-Kompatibilität wurden bereits in
    // ExpandFilterValidator.validateFilters() geprüft
    String propertyCode = filter.getProperty();
    String regexValue = filter.getValue();

    // Property-Definition aus CodeSystem laden (bereits validiert)
    CodeSystem.PropertyComponent property =
        CodeSystemHelper.getProperty(codeSystem, propertyCode).orElse(null);

    // Alle Konzepte des CodeSystems flach auflösen (inklusive nested concepts)
    List<CodeSystem.ConceptDefinitionComponent> allConcepts =
        CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

    // Sonderbehandlung von 'code'-Filtern
    // Hier wird der Regex-Filter auf die Codes der Konzepte angewendet
    if (Objects.equals(propertyCode, "code")) {
      allConcepts.stream()
          .filter(CodeSystem.ConceptDefinitionComponent::hasCode)
          .filter(c -> c.getCode().matches(regexValue))
          .filter(concept -> ExpandTextFilter.matchesFilter(system, concept, resultFilter))
          .forEach(
              concept ->
                  ExpandResultHelper.extractBaseConcept(
                      system, version, includeInactiveConcepts, filterResultSet, concept));
      return;
    }

    // TODO: Sonderbehandlung von 'display'-Filtern

    // TODO: Sonderbehandlung von 'definition'-Filtern

    // Property-Typ-Kompatibilität wurde bereits in ExpandFilterValidator.validateRegexFilter()
    // geprüft

    // Iterieren über alle Konzepte des CodeSystems (inklusive nested concepts)
    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {

      // Verarbeiten der explizit am Konzept definierten Properties
      concept.getProperty().stream()
          .filter(CodeSystem.ConceptPropertyComponent::hasCode)
          .filter(p -> p.getCode().equals(propertyCode))
          .filter(p -> ConceptPropertyMatcher.hasMatchingPropertyType(p, property))
          .filter(p -> ConceptPropertyMatcher.hasRegExMatchingPropertyValue(p, regexValue))
          .forEach(
              cpc -> {
                if (ExpandTextFilter.matchesFilter(system, concept, resultFilter)) {
                  ExpandResultHelper.extractBaseConcept(
                      system, version, includeInactiveConcepts, filterResultSet, concept);
                }
              });
    }
  }

  /**
   * Processes an equals filter operation.
   *
   * <p>Matches concepts where the specified property value equals the filter value. Includes
   * special handling for {@code parent} and {@code child} properties.
   *
   * @param filter the filter component containing the equals operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processEqualsFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    String system = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    // Property-Existenz wurde bereits in ExpandFilterValidator.validateFilters() geprüft
    String propertyCode = filter.getProperty();
    String value = filter.getValue();

    // Property-Definition aus CodeSystem laden (bereits validiert)
    CodeSystem.PropertyComponent property =
        CodeSystemHelper.getProperty(codeSystem, propertyCode).orElse(null);

    // Sonderbehandlung von 'parent =' und 'child ='-Filtern
    if (Objects.equals(propertyCode, PROPERTY_NAME_CHILD)
        || Objects.equals(propertyCode, PROPERTY_NAME_PARENT)) {

      // Verarbeiten von "parent" und "child"-Filtern
      processParentChildEqualsFilter(
          filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
      return;
    }

    // Alle Konzepte des CodeSystems flach auflösen (inklusive nested concepts)
    List<CodeSystem.ConceptDefinitionComponent> allConcepts =
        CodeSystemHelper.getCachedAllConceptsAsList(codeSystem);

    // Sonderbehandlung von 'code'-Filtern
    // TODO: Aktuell wird nicht berücksichtigt, ob codes caseSensitive sind oder nicht. Das muss
    //  sich ändern!
    if (Objects.equals(propertyCode, "code")) {
      allConcepts.stream()
          .filter(CodeSystem.ConceptDefinitionComponent::hasCode)
          .filter(c -> c.getCode().equals(value))
          .filter(concept -> ExpandTextFilter.matchesFilter(system, concept, resultFilter))
          .forEach(
              concept ->
                  ExpandResultHelper.extractBaseConcept(
                      system, version, includeInactiveConcepts, filterResultSet, concept));
      return;
    }

    // Iterieren über alle Konzepte des CodeSystems (inklusive nested concepts)
    for (CodeSystem.ConceptDefinitionComponent concept : allConcepts) {

      concept.getProperty().stream()
          .filter(CodeSystem.ConceptPropertyComponent::hasCode)
          .filter(p -> p.getCode().equals(propertyCode))
          .filter(p -> ConceptPropertyMatcher.hasMatchingPropertyType(p, property))
          .filter(p -> ConceptPropertyMatcher.hasMatchingPropertyValue(p, value))
          .forEach(
              cpc -> {
                if (ExpandTextFilter.matchesFilter(system, concept, resultFilter)) {
                  ExpandResultHelper.extractBaseConcept(
                      system, version, includeInactiveConcepts, filterResultSet, concept);
                }
              });

      // TODO: Sonderbehandlung  für implizite Properties (code, display, definition, inactive, ...)

    }
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes an is-a filter operation (subsumption with descendants and self).
   *
   * <p>Includes all concepts that have a transitive is-a relationship with the specified concept,
   * including the concept itself.
   *
   * @param filter the filter component containing the is-a operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processIsAFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    // Hierarchie-Voraussetzungen wurden bereits in ExpandFilterValidator.validateFilters() geprüft

    // TODO: implement as soon as needed
    failBecauseOfUnsupportedFilter(
        filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes an is-not-a filter operation (negated subsumption).
   *
   * <p>Excludes all concepts that have a transitive is-a relationship with the specified concept.
   *
   * @param filter the filter component containing the is-not-a operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processIsNotAFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    // Hierarchie-Voraussetzungen wurden bereits in ExpandFilterValidator.validateFilters() geprüft

    // TODO: implement as soon as needed
    failBecauseOfUnsupportedFilter(
        filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes a generalizes filter operation (ancestors and self).
   *
   * <p>Includes all concepts that have a transitive is-a relationship from the specified concept,
   * including the concept itself (i.e., ancestor codes and self).
   *
   * @param filter the filter component containing the generalizes operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processGeneralizesFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    // Hierarchie-Voraussetzungen wurden bereits in ExpandFilterValidator.validateFilters() geprüft

    // TODO: implement as soon as needed
    failBecauseOfUnsupportedFilter(
        filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes a descendent-of filter operation (descendants only, excluding self).
   *
   * <p>Includes all concepts that have a transitive is-a relationship with the specified concept,
   * excluding the concept itself.
   *
   * @param filter the filter component containing the descendent-of operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processDecendentOfFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    // Hierarchie-Voraussetzungen wurden bereits in ExpandFilterValidator.validateFilters() geprüft

    // TODO: implement as soon as needed
    failBecauseOfUnsupportedFilter(
        filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes an in filter operation (set membership).
   *
   * <p>Matches concepts where the specified property value is in the provided comma-separated list.
   *
   * @param filter the filter component containing the in operation
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processInFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    // TODO: implement as soon as needed
    failBecauseOfUnsupportedFilter(
        filter, resultFilter, includeInactiveConcepts, filterResultSet, codeSystem);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Logs a warning and throws an {@link InvalidRequestException} for unsupported filter operations.
   *
   * @param filter the unsupported filter component
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether inactive concepts were requested
   * @param filterResultSet the filter result set (included for diagnostic logging)
   * @param codeSystem the CodeSystem being filtered
   * @throws InvalidRequestException always thrown to signal the unsupported filter
   */
  private void failBecauseOfUnsupportedFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem) {

    String system = codeSystem.getUrl();
    String version = codeSystem.getVersion();

    log.warn(
        "The given ValueSet definition on {}|{} includes an unsupported filter operation: '{}' (resultFilter = {}, includeInactiveConcepts = {}, filterResultSet = {}, codeSystem = {})",
        system,
        version,
        filter.getOp(),
        resultFilter,
        includeInactiveConcepts,
        filterResultSet,
        codeSystem);
    throw new InvalidRequestException(
        "The given ValueSet definition on "
            + system
            + "|"
            + version
            + " includes an unsupported filter operation: "
            + filter.getOp());
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes parent/child equals filter using graph-based lookups.
   *
   * @param filter the filter component containing the equals operation for parent/child
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processParentChildEqualsFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    String value = filter.getValue();

    // whether the filter property refers to child or parent
    boolean refersToChild = filter.getProperty().equals(PROPERTY_NAME_CHILD);

    FhirTermGraphRepresentation graph =
        this.fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem);

    Set<String> resultCodes =
        refersToChild
            ? graph.getAllConceptsWithDirectChild(value)
            : graph.getAllConceptsWithDirectParent(value);

    ExpandResultHelper.addCodesToResultSet(
        resultCodes, filterResultSet, codeSystem, includeInactiveConcepts, resultFilter);
  }

  // ----------------------------------------------------------------------------------------------------

  /**
   * Processes parent/child exists filter using graph-based lookups.
   *
   * @param filter the filter component containing the exists operation for parent/child
   * @param resultFilter the text filter to apply on concept definition content
   * @param includeInactiveConcepts whether to include inactive concepts
   * @param filterResultSet the set to add matching concepts to
   * @param codeSystem the CodeSystem to filter
   */
  private void processParentChildExistsFilter(
      ValueSet.ConceptSetFilterComponent filter,
      String resultFilter,
      boolean includeInactiveConcepts,
      Set<ConceptBaseInfo> filterResultSet,
      CodeSystem codeSystem)
      throws UnknownConceptException {

    // whether the filter property refers to child or parent
    boolean refersToChild = filter.getProperty().equals(PROPERTY_NAME_CHILD);

    boolean exists = Objects.equals(filter.getValue(), "true");

    FhirTermGraphRepresentation graph =
        this.fhirTermsInMemoryRepresentation.getGraphForCodeSystem(codeSystem);

    Set<String> resultCodes = graph.getConceptsByRelationshipExistence(refersToChild, exists);
    ExpandResultHelper.addCodesToResultSet(
        resultCodes, filterResultSet, codeSystem, includeInactiveConcepts, resultFilter);
  }
}
