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

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Utility class for applying the {@code filter} text parameter from the {@code $expand} operation.
 *
 * <p>Performs case-insensitive substring matching against concept properties such as system, code,
 * display, definition, and designations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ExpandTextFilter {

  /**
   * Checks whether a concept definition matches the given text filter.
   *
   * <p>Matches against system URL, code, display, definition, and all designation values.
   *
   * @param system the CodeSystem URL
   * @param concept the concept definition to check
   * @param filter the filter text (case-insensitive substring match)
   * @return {@code true} if the filter is blank/null or the concept matches
   */
  static boolean matchesFilter(
      String system, CodeSystem.ConceptDefinitionComponent concept, String filter) {

    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(system, filter)
        || containsFilterValue(concept.getCode(), filter)
        || containsFilterValue(concept.getDisplay(), filter)
        || containsFilterValue(concept.getDefinition(), filter)
        || concept.getDesignation().stream()
            .filter(CodeSystem.ConceptDefinitionDesignationComponent::hasValue)
            .map(CodeSystem.ConceptDefinitionDesignationComponent::getValue)
            .anyMatch(designationValue -> containsFilterValue(designationValue, filter));
  }

  /**
   * Checks whether a concept reference matches the given text filter.
   *
   * <p>Matches against code, display, and all designation values.
   *
   * @param conceptReferenceComponent the concept reference to check
   * @param filter the filter text (case-insensitive substring match)
   * @return {@code true} if the filter is blank/null or the reference matches
   */
  static boolean matchesFilter(
      ValueSet.ConceptReferenceComponent conceptReferenceComponent, String filter) {

    if (filter == null || filter.isBlank()) {
      return true;
    }

    return containsFilterValue(conceptReferenceComponent.getCode(), filter)
        || containsFilterValue(conceptReferenceComponent.getDisplay(), filter)
        || conceptReferenceComponent.getDesignation().stream()
            .filter(ValueSet.ConceptReferenceDesignationComponent::hasValue)
            .map(ValueSet.ConceptReferenceDesignationComponent::getValue)
            .anyMatch(designationValue -> containsFilterValue(designationValue, filter));
  }

  private static boolean containsFilterValue(String candidate, String filter) {
    return candidate != null
        && candidate.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
  }
}
