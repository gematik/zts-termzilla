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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SnomedVsExplicitExpandOperation#deduplicateDesignationComponents(List)}.
 *
 * <p>Verifies that duplicate designation components (identical language, use, and value) are
 * removed in-place, retaining the first occurrence. This deduplication is required when context
 * extensions are disabled, because descriptions appearing in multiple language-type reference sets
 * may produce identical {@link ValueSet.ConceptReferenceDesignationComponent} entries.
 */
@DisplayName("deduplicateDesignationComponents Tests")
class DeduplicateDesignationComponentsTest {

  private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
  private static final String SYNONYM_TYPE_ID = "900000000000013009";
  private static final String FSN_TYPE_ID = "900000000000003001";

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a {@link ValueSet.ConceptReferenceDesignationComponent} with the given properties. */
  private static ValueSet.ConceptReferenceDesignationComponent createComponent(
      String language, String useSystem, String useCode, String value) {

    ValueSet.ConceptReferenceDesignationComponent component =
        new ValueSet.ConceptReferenceDesignationComponent();
    component.setLanguage(language);
    component.setValue(value);

    if (useSystem != null || useCode != null) {
      component.setUse(new Coding(useSystem, useCode, null));
    }

    return component;
  }

  /** Creates a SNOMED-typed designation component with the given language and value. */
  private static ValueSet.ConceptReferenceDesignationComponent createSnomedComponent(
      String language, String value, String typeId) {

    return createComponent(language, SNOMED_SYSTEM, typeId, value);
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("Empty and single-element lists")
  class EmptyAndSingleElementTests {

    @Test
    @DisplayName("Should handle empty list without error")
    void shouldHandleEmptyList() {
      // GIVEN
      List<ValueSet.ConceptReferenceDesignationComponent> components = new ArrayList<>();

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components).as("Empty list should remain empty").isEmpty();
    }

    @Test
    @DisplayName("Should keep single element unchanged")
    void shouldKeepSingleElement() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent component =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(component));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components).as("Single element should be retained").containsExactly(component);
    }
  }

  @Nested
  @DisplayName("Duplicate removal")
  class DuplicateRemovalTests {

    @Test
    @DisplayName(
        "Should remove exact duplicate when two components have same language, use, and value")
    void shouldRemoveExactDuplicate() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent first =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent duplicate =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(first, duplicate));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Duplicate should be removed, keeping only the first occurrence")
          .hasSize(1);
      assertThat(components.get(0))
          .as("Retained component should be the first one")
          .isSameAs(first);
    }

    @Test
    @DisplayName("Should remove all duplicates when same component appears three times")
    void shouldRemoveAllDuplicatesOfSameComponent() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent first =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent second =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent third =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(first, second, third));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("All duplicates should be removed, keeping only the first occurrence")
          .hasSize(1);
      assertThat(components.get(0))
          .as("Retained component should be the first one")
          .isSameAs(first);
    }

    @Test
    @DisplayName("Should remove duplicates from multiple groups independently")
    void shouldDeduplicateMultipleGroups() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent synonym1 =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent synonym1Dup =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent fsn1 =
          createSnomedComponent("en", "Diabetes mellitus (disorder)", FSN_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent fsn1Dup =
          createSnomedComponent("en", "Diabetes mellitus (disorder)", FSN_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(synonym1, synonym1Dup, fsn1, fsn1Dup));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Each duplicate group should keep only the first occurrence")
          .hasSize(2);
      assertThat(components.get(0)).as("First retained should be the synonym").isSameAs(synonym1);
      assertThat(components.get(1)).as("Second retained should be the FSN").isSameAs(fsn1);
    }
  }

  @Nested
  @DisplayName("Non-duplicate preservation")
  class NonDuplicatePreservationTests {

    @Test
    @DisplayName("Should keep components with different values")
    void shouldKeepComponentsWithDifferentValues() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent first =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent second =
          createSnomedComponent("en", "Sugar diabetes", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(first, second));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Components with different values should both be retained")
          .containsExactly(first, second);
    }

    @Test
    @DisplayName("Should keep components with different languages")
    void shouldKeepComponentsWithDifferentLanguages() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent english =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent german =
          createSnomedComponent("de", "Diabetes mellitus", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(english, german));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Components with different languages should both be retained")
          .containsExactly(english, german);
    }

    @Test
    @DisplayName("Should keep components with different use codings")
    void shouldKeepComponentsWithDifferentUseCodings() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent synonym =
          createSnomedComponent("en", "Diabetes mellitus", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent fsn =
          createSnomedComponent("en", "Diabetes mellitus", FSN_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(synonym, fsn));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Components with different use codings should both be retained")
          .containsExactly(synonym, fsn);
    }

    @Test
    @DisplayName("Should keep components with different use systems")
    void shouldKeepComponentsWithDifferentUseSystems() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent snomedUse =
          createComponent("en", SNOMED_SYSTEM, SYNONYM_TYPE_ID, "Diabetes mellitus");
      ValueSet.ConceptReferenceDesignationComponent hl7Use =
          createComponent(
              "en",
              "http://terminology.hl7.org/CodeSystem/designation-usage",
              "display",
              "Diabetes mellitus");
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(snomedUse, hl7Use));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Components with different use systems should both be retained")
          .containsExactly(snomedUse, hl7Use);
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle components without use coding")
    void shouldHandleComponentsWithoutUseCoding() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent first =
          createComponent("en", null, null, "Diabetes mellitus");
      ValueSet.ConceptReferenceDesignationComponent duplicate =
          createComponent("en", null, null, "Diabetes mellitus");
      ValueSet.ConceptReferenceDesignationComponent different =
          createComponent("en", null, null, "Sugar diabetes");
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(first, duplicate, different));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components)
          .as("Should deduplicate even without use coding and keep distinct entries")
          .hasSize(2);
      assertThat(components.get(0)).isSameAs(first);
      assertThat(components.get(1)).isSameAs(different);
    }

    @Test
    @DisplayName("Should preserve order of unique elements after deduplication")
    void shouldPreserveOrderOfUniqueElements() {
      // GIVEN
      ValueSet.ConceptReferenceDesignationComponent a =
          createSnomedComponent("en", "Alpha", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent b =
          createSnomedComponent("de", "Beta", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent aDup =
          createSnomedComponent("en", "Alpha", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent c =
          createSnomedComponent("fr", "Gamma", SYNONYM_TYPE_ID);
      ValueSet.ConceptReferenceDesignationComponent bDup =
          createSnomedComponent("de", "Beta", SYNONYM_TYPE_ID);
      List<ValueSet.ConceptReferenceDesignationComponent> components =
          new ArrayList<>(List.of(a, b, aDup, c, bDup));

      // WHEN
      SnomedVsExplicitExpandOperation.deduplicateDesignationComponents(components);

      // THEN
      assertThat(components).as("Should preserve insertion order of first occurrences").hasSize(3);
      assertThat(components.get(0)).isSameAs(a);
      assertThat(components.get(1)).isSameAs(b);
      assertThat(components.get(2)).isSameAs(c);
    }
  }
}
