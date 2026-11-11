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
package de.gematik.zts.termzilla.server.search;

import static de.gematik.zts.termzilla.api.constants.FhirExtensionUrls.EXTENSION_RESOURCE_EFFECTIVE_PERIOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.exceptions.UnsupportedSortFieldException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ConceptMap.ConceptMapGroupComponent;
import org.hl7.fhir.r4.model.ConceptMap.SourceElementComponent;
import org.hl7.fhir.r4.model.ConceptMap.TargetElementComponent;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FhirResourceComparator}.
 *
 * <p>Tests the FHIR resource sorting functionality including:
 *
 * <ul>
 *   <li>Building comparators from sort criteria
 *   <li>Sorting by common resource fields
 *   <li>Sorting by resource-specific fields
 *   <li>Handling of null values and missing fields
 *   <li>Ascending and descending order
 *   <li>Multiple sort criteria
 *   <li>Repeating elements (using earliest value)
 *   <li>Exception handling for unsupported sort fields
 * </ul>
 */
class FhirResourceComparatorTest {

  @Nested
  @DisplayName("buildComparator")
  class BuildComparatorTests {

    @Test
    @DisplayName("should return null when sort criteria is empty")
    void shouldReturnNullWhenSortCriteriaIsEmpty() {
      // GIVEN
      List<String> sortCriteria = List.of();

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

      // THEN
      assertThat(comparator).isNull();
    }

    @Test
    @DisplayName("should build comparator for single ascending criterion")
    void shouldBuildComparatorForSingleAscendingCriterion() {
      // GIVEN
      List<String> sortCriteria = List.of("name");
      CodeSystem cs1 = new CodeSystem().setName("Beta");
      CodeSystem cs2 = new CodeSystem().setName("Alpha");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1);
    }

    @Test
    @DisplayName("should build comparator for single descending criterion")
    void shouldBuildComparatorForSingleDescendingCriterion() {
      // GIVEN
      List<String> sortCriteria = List.of("-name");
      CodeSystem cs1 = new CodeSystem().setName("Alpha");
      CodeSystem cs2 = new CodeSystem().setName("Beta");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1);
    }

    @Test
    @DisplayName("should build comparator chain for multiple criteria")
    void shouldBuildComparatorChainForMultipleCriteria() {
      // GIVEN
      List<String> sortCriteria = List.of("status", "name");
      CodeSystem cs1 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setName("Beta");
      CodeSystem cs2 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setName("Alpha");
      CodeSystem cs3 = new CodeSystem().setStatus(PublicationStatus.DRAFT).setName("Gamma");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1, cs3);
    }
  }

  @Nested
  @DisplayName("Unsupported Sort Field Exception Handling")
  class UnsupportedSortFieldExceptionTests {

    @Nested
    @DisplayName("CodeSystem unsupported fields")
    class CodeSystemUnsupportedFieldsTests {

      @Test
      @DisplayName("should throw exception for unsupported CodeSystem field")
      void shouldThrowExceptionForUnsupportedCodeSystemField() {
        // GIVEN
        List<String> sortCriteria = List.of("unsupportedField");
        CodeSystem cs1 = new CodeSystem().setName("Test1");
        CodeSystem cs2 = new CodeSystem().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'unsupportedField' for CodeSystem");
      }

      @Test
      @DisplayName("should throw exception for ValueSet-specific field on CodeSystem")
      void shouldThrowExceptionForValueSetSpecificFieldOnCodeSystem() {
        // GIVEN
        List<String> sortCriteria = List.of(VsSearchParameter.SP_REFERENCE);
        CodeSystem cs1 = new CodeSystem().setName("Test1");
        CodeSystem cs2 = new CodeSystem().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("CodeSystem");
      }

      @Test
      @DisplayName("should throw exception for ConceptMap-specific field on CodeSystem")
      void shouldThrowExceptionForConceptMapSpecificFieldOnCodeSystem() {
        // GIVEN
        List<String> sortCriteria = List.of(CmSearchParameter.SP_SOURCE_SYSTEM);
        CodeSystem cs1 = new CodeSystem().setName("Test1");
        CodeSystem cs2 = new CodeSystem().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("CodeSystem");
      }
    }

    @Nested
    @DisplayName("ValueSet unsupported fields")
    class ValueSetUnsupportedFieldsTests {

      @Test
      @DisplayName("should throw exception for unsupported ValueSet field")
      void shouldThrowExceptionForUnsupportedValueSetField() {
        // GIVEN
        List<String> sortCriteria = List.of("invalidField");
        ValueSet vs1 = new ValueSet().setName("Test1");
        ValueSet vs2 = new ValueSet().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, vs1, vs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'invalidField' for ValueSet");
      }

      @Test
      @DisplayName("should throw exception for CodeSystem-specific field on ValueSet")
      void shouldThrowExceptionForCodeSystemSpecificFieldOnValueSet() {
        // GIVEN
        List<String> sortCriteria = List.of(CsSearchParameter.SP_CONTENT_MODE);
        ValueSet vs1 = new ValueSet().setName("Test1");
        ValueSet vs2 = new ValueSet().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, vs1, vs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("ValueSet");
      }

      @Test
      @DisplayName("should throw exception for ConceptMap-specific field on ValueSet")
      void shouldThrowExceptionForConceptMapSpecificFieldOnValueSet() {
        // GIVEN
        List<String> sortCriteria = List.of(CmSearchParameter.SP_DEPENDS_ON);
        ValueSet vs1 = new ValueSet().setName("Test1");
        ValueSet vs2 = new ValueSet().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, vs1, vs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("ValueSet");
      }
    }

    @Nested
    @DisplayName("ConceptMap unsupported fields")
    class ConceptMapUnsupportedFieldsTests {

      @Test
      @DisplayName("should throw exception for unsupported ConceptMap field")
      void shouldThrowExceptionForUnsupportedConceptMapField() {
        // GIVEN
        List<String> sortCriteria = List.of("unknownField");
        ConceptMap cm1 = new ConceptMap().setName("Test1");
        ConceptMap cm2 = new ConceptMap().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cm1, cm2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'unknownField' for ConceptMap");
      }

      @Test
      @DisplayName("should throw exception for CodeSystem-specific field on ConceptMap")
      void shouldThrowExceptionForCodeSystemSpecificFieldOnConceptMap() {
        // GIVEN
        List<String> sortCriteria = List.of(CsSearchParameter.SP_SUPPLEMENTS);
        ConceptMap cm1 = new ConceptMap().setName("Test1");
        ConceptMap cm2 = new ConceptMap().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cm1, cm2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("ConceptMap");
      }

      @Test
      @DisplayName("should throw exception for ValueSet-specific field on ConceptMap")
      void shouldThrowExceptionForValueSetSpecificFieldOnConceptMap() {
        // GIVEN
        List<String> sortCriteria = List.of(VsSearchParameter.SP_EXPANSION);
        ConceptMap cm1 = new ConceptMap().setName("Test1");
        ConceptMap cm2 = new ConceptMap().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cm1, cm2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field")
            .hasMessageContaining("ConceptMap");
      }
    }

    @Nested
    @DisplayName("Multiple criteria with unsupported field")
    class MultipleCriteriaWithUnsupportedFieldTests {

      @Test
      @DisplayName("should throw exception when first criterion is unsupported")
      void shouldThrowExceptionWhenFirstCriterionIsUnsupported() {
        // GIVEN
        List<String> sortCriteria = List.of("invalidField", "name");
        CodeSystem cs1 = new CodeSystem().setName("Test1");
        CodeSystem cs2 = new CodeSystem().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'invalidField'");
      }

      @Test
      @DisplayName("should throw exception when second criterion is unsupported")
      void shouldThrowExceptionWhenSecondCriterionIsUnsupported() {
        // GIVEN
        List<String> sortCriteria = List.of("name", "invalidField");
        CodeSystem cs1 = new CodeSystem().setName("Alpha");
        CodeSystem cs2 = new CodeSystem().setName("Alpha");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'invalidField'");
      }

      @Test
      @DisplayName("should throw exception for unsupported field with descending prefix")
      void shouldThrowExceptionForUnsupportedFieldWithDescendingPrefix() {
        // GIVEN
        List<String> sortCriteria = List.of("-invalidField");
        CodeSystem cs1 = new CodeSystem().setName("Test1");
        CodeSystem cs2 = new CodeSystem().setName("Test2");
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, cs1, cs2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'invalidField'");
      }
    }

    @Nested
    @DisplayName("Exception with unknown resource type")
    class ExceptionWithUnknownResourceTypeTests {

      @Test
      @DisplayName("should throw exception for unsupported resource type")
      void shouldThrowExceptionForUnsupportedResourceType() {
        // GIVEN
        List<String> sortCriteria = List.of("someField");
        // Using a different resource type that's not CodeSystem, ValueSet, or ConceptMap
        org.hl7.fhir.r4.model.Patient patient1 = new org.hl7.fhir.r4.model.Patient();
        org.hl7.fhir.r4.model.Patient patient2 = new org.hl7.fhir.r4.model.Patient();
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);

        // WHEN / THEN
        assertThatThrownBy(() -> sortTwoResources(comparator, patient1, patient2))
            .isInstanceOf(UnsupportedSortFieldException.class)
            .hasMessageContaining("Unsupported sort field 'someField' for resource type: Patient");
      }
    }
  }

  @Nested
  @DisplayName("Common Field Sorting")
  class CommonFieldSortingTests {

    @Nested
    @DisplayName("_id sorting")
    class IdSortingTests {

      @Test
      @DisplayName("should sort by id ascending")
      void shouldSortByIdAscending() {
        // GIVEN
        CodeSystem cs1 = (CodeSystem) new CodeSystem().setId("cs-2");
        CodeSystem cs2 = (CodeSystem) new CodeSystem().setId("cs-1");
        CodeSystem cs3 = (CodeSystem) new CodeSystem().setId("cs-3");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_ID);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1, cs3);
      }

      @Test
      @DisplayName("should sort by id descending")
      void shouldSortByIdDescending() {
        // GIVEN
        CodeSystem cs1 = (CodeSystem) new CodeSystem().setId("cs-2");
        CodeSystem cs2 = (CodeSystem) new CodeSystem().setId("cs-1");
        CodeSystem cs3 = (CodeSystem) new CodeSystem().setId("cs-3");
        List<String> sortCriteria = List.of("-" + TerminologyResourceSearchParameter.SP_ID);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs3, cs1, cs2);
      }

      @Test
      @DisplayName("should handle null ids")
      void shouldHandleNullIds() {
        // GIVEN
        CodeSystem cs1 = (CodeSystem) new CodeSystem().setId("cs-1");
        CodeSystem cs2 = new CodeSystem();
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_ID);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs1, cs2);
      }
    }

    @Nested
    @DisplayName("_lastUpdated sorting")
    class LastUpdatedSortingTests {

      @Test
      @DisplayName("should sort by lastUpdated ascending")
      void shouldSortByLastUpdatedAscending() {
        // GIVEN
        Date date1 = new Date(1000);
        Date date2 = new Date(2000);
        Date date3 = new Date(3000);
        CodeSystem cs1 = (CodeSystem) new CodeSystem().setMeta(new Meta().setLastUpdated(date2));
        CodeSystem cs2 = (CodeSystem) new CodeSystem().setMeta(new Meta().setLastUpdated(date1));
        CodeSystem cs3 = (CodeSystem) new CodeSystem().setMeta(new Meta().setLastUpdated(date3));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_LAST_UPDATED);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1, cs3);
      }

      @Test
      @DisplayName("should handle missing lastUpdated")
      void shouldHandleMissingLastUpdated() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem) new CodeSystem().setMeta(new Meta().setLastUpdated(new Date()));
        CodeSystem cs2 = new CodeSystem();
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_LAST_UPDATED);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs1, cs2);
      }
    }

    @Nested
    @DisplayName("_tag sorting")
    class TagSortingTests {

      @Test
      @DisplayName("should sort by tag ascending using earliest tag")
      void shouldSortByTagAscendingUsingEarliestTag() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .setUrl("cs1")
                    .setMeta(
                        new Meta()
                            .addTag(new Coding("http://system", "tag-2", null))
                            .addTag(new Coding("http://system", "tag-3", null)));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem()
                    .setUrl("cs2")
                    .setMeta(new Meta().addTag(new Coding("http://system", "tag-1", null)));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_TAG);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }

      @Test
      @DisplayName("should sort by tag descending using latest tag")
      void shouldSortByTagDescendingUsingLatestTag() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .setMeta(
                        new Meta()
                            .addTag(new Coding("http://system", "tag-2", null))
                            .addTag(new Coding("http://system", "tag-3", null)));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem()
                    .setMeta(new Meta().addTag(new Coding("http://system", "tag-1", null)));
        List<String> sortCriteria = List.of("-" + TerminologyResourceSearchParameter.SP_TAG);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs1, cs2);
      }

      @Test
      @DisplayName("should handle tag without system")
      void shouldHandleTagWithoutSystem() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem().setMeta(new Meta().addTag(new Coding(null, "tag-2", null)));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem().setMeta(new Meta().addTag(new Coding(null, "tag-1", null)));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_TAG);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("_profile sorting")
    class ProfileSortingTests {

      @Test
      @DisplayName("should sort by profile ascending")
      void shouldSortByProfileAscending() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .setMeta(
                        new Meta().addProfile("http://profile-b").addProfile("http://profile-c"));
        CodeSystem cs2 =
            (CodeSystem) new CodeSystem().setMeta(new Meta().addProfile("http://profile-a"));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_PROFILE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("_security sorting")
    class SecuritySortingTests {

      @Test
      @DisplayName("should sort by security label ascending")
      void shouldSortBySecurityLabelAscending() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .setMeta(new Meta().addSecurity(new Coding("http://system", "sec-2", null)));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem()
                    .setMeta(new Meta().addSecurity(new Coding("http://system", "sec-1", null)));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_SECURITY);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("_source sorting")
    class SourceSortingTests {

      @Test
      @DisplayName("should sort by source ascending")
      void shouldSortBySourceAscending() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem) new CodeSystem().setMeta(new Meta().setSource("http://source-b"));
        CodeSystem cs2 =
            (CodeSystem) new CodeSystem().setMeta(new Meta().setSource("http://source-a"));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_SOURCE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }
  }

  @Nested
  @DisplayName("MetadataResource Field Sorting")
  class MetadataResourceFieldSortingTests {

    @Nested
    @DisplayName("url sorting")
    class UrlSortingTests {

      @Test
      @DisplayName("should sort by url ascending")
      void shouldSortByUrlAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setUrl("http://url-b");
        CodeSystem cs2 = new CodeSystem().setUrl("http://url-a");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_URL);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("version sorting")
    class VersionSortingTests {

      @Test
      @DisplayName("should sort by version ascending")
      void shouldSortByVersionAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setVersion("2.0");
        CodeSystem cs2 = new CodeSystem().setVersion("1.0");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_VERSION);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("name sorting")
    class NameSortingTests {

      @Test
      @DisplayName("should sort by name case-insensitive")
      void shouldSortByNameCaseInsensitive() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setName("BETA");
        CodeSystem cs2 = new CodeSystem().setName("alpha");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_NAME);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("title sorting")
    class TitleSortingTests {

      @Test
      @DisplayName("should sort by title ascending")
      void shouldSortByTitleAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setTitle("Title B");
        CodeSystem cs2 = new CodeSystem().setTitle("Title A");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_TITLE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("status sorting")
    class StatusSortingTests {

      @Test
      @DisplayName("should sort by status ascending")
      void shouldSortByStatusAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setStatus(PublicationStatus.DRAFT);
        CodeSystem cs2 = new CodeSystem().setStatus(PublicationStatus.ACTIVE);
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_STATUS);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("date sorting")
    class DateSortingTests {

      @Test
      @DisplayName("should sort by date ascending")
      void shouldSortByDateAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setDate(new Date(2000));
        CodeSystem cs2 = new CodeSystem().setDate(new Date(1000));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_DATE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("publisher sorting")
    class PublisherSortingTests {

      @Test
      @DisplayName("should sort by publisher ascending")
      void shouldSortByPublisherAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setPublisher("Publisher B");
        CodeSystem cs2 = new CodeSystem().setPublisher("Publisher A");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_PUBLISHER);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("description sorting")
    class DescriptionSortingTests {

      @Test
      @DisplayName("should sort by description ascending")
      void shouldSortByDescriptionAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setDescription("Description B");
        CodeSystem cs2 = new CodeSystem().setDescription("Description A");
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_DESCRIPTION);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("jurisdiction sorting")
    class JurisdictionSortingTests {

      @Test
      @DisplayName("should sort by jurisdiction ascending using earliest code")
      void shouldSortByJurisdictionAscendingUsingEarliestCode() {
        // GIVEN
        CodeSystem cs1 =
            new CodeSystem()
                .addJurisdiction(
                    new CodeableConcept()
                        .addCoding(new Coding("http://system", "DE", null))
                        .addCoding(new Coding("http://system", "FR", null)));
        CodeSystem cs2 =
            new CodeSystem()
                .addJurisdiction(
                    new CodeableConcept().addCoding(new Coding("http://system", "AT", null)));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_JURISDICTION);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("context sorting")
    class ContextSortingTests {

      @Test
      @DisplayName("should sort by usage context ascending")
      void shouldSortByUsageContextAscending() {
        // GIVEN
        CodeSystem cs1 =
            new CodeSystem()
                .addUseContext(
                    new UsageContext()
                        .setValue(
                            new CodeableConcept()
                                .addCoding(new Coding("http://system", "context-2", null))));
        CodeSystem cs2 =
            new CodeSystem()
                .addUseContext(
                    new UsageContext()
                        .setValue(
                            new CodeableConcept()
                                .addCoding(new Coding("http://system", "context-1", null))));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_CONTENT);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("effective period sorting")
    class EffectivePeriodSortingTests {

      @Test
      @DisplayName("should sort by effective period start date")
      void shouldSortByEffectivePeriodStartDate() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .addExtension(
                        new Extension(
                            EXTENSION_RESOURCE_EFFECTIVE_PERIOD,
                            new Period().setStart(new Date(2000))));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem()
                    .addExtension(
                        new Extension(
                            EXTENSION_RESOURCE_EFFECTIVE_PERIOD,
                            new Period().setStart(new Date(1000))));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_EFFECTIVE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }

      @Test
      @DisplayName("should use end date if start date is missing")
      void shouldUseEndDateIfStartDateIsMissing() {
        // GIVEN
        CodeSystem cs1 =
            (CodeSystem)
                new CodeSystem()
                    .addExtension(
                        new Extension(
                            EXTENSION_RESOURCE_EFFECTIVE_PERIOD,
                            new Period().setEnd(new Date(2000))));
        CodeSystem cs2 =
            (CodeSystem)
                new CodeSystem()
                    .addExtension(
                        new Extension(
                            EXTENSION_RESOURCE_EFFECTIVE_PERIOD,
                            new Period().setEnd(new Date(1000))));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_EFFECTIVE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }
  }

  @Nested
  @DisplayName("CodeSystem Specific Field Sorting")
  class CodeSystemSpecificFieldSortingTests {

    @Nested
    @DisplayName("content-mode sorting")
    class ContentModeSortingTests {

      @Test
      @DisplayName("should sort by content mode ascending")
      void shouldSortByContentModeAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setContent(CodeSystemContentMode.FRAGMENT);
        CodeSystem cs2 = new CodeSystem().setContent(CodeSystemContentMode.COMPLETE);
        List<String> sortCriteria = List.of(CsSearchParameter.SP_CONTENT_MODE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("supplements sorting")
    class SupplementsSortingTests {

      @Test
      @DisplayName("should sort by supplements ascending")
      void shouldSortBySupplementsAscending() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setSupplements("http://supplement-b");
        CodeSystem cs2 = new CodeSystem().setSupplements("http://supplement-a");
        List<String> sortCriteria = List.of(CsSearchParameter.SP_SUPPLEMENTS);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("system sorting")
    class SystemSortingTests {

      @Test
      @DisplayName("should sort by system using url")
      void shouldSortBySystemUsingUrl() {
        // GIVEN
        CodeSystem cs1 = new CodeSystem().setUrl("http://system-b");
        CodeSystem cs2 = new CodeSystem().setUrl("http://system-a");
        List<String> sortCriteria = List.of(CsSearchParameter.SP_SYSTEM);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }

    @Nested
    @DisplayName("identifier sorting")
    class IdentifierSortingTests {

      @Test
      @DisplayName("should sort by identifier ascending")
      void shouldSortByIdentifierAscending() {
        // GIVEN
        CodeSystem cs1 =
            new CodeSystem()
                .addIdentifier(new Identifier().setSystem("http://system").setValue("id-2"));
        CodeSystem cs2 =
            new CodeSystem()
                .addIdentifier(new Identifier().setSystem("http://system").setValue("id-1"));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_IDENTIFIER);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }

      @Test
      @DisplayName("should use earliest identifier for sorting")
      void shouldUseEarliestIdentifierForSorting() {
        // GIVEN
        CodeSystem cs1 =
            new CodeSystem()
                .addIdentifier(new Identifier().setSystem("http://system").setValue("id-3"))
                .addIdentifier(new Identifier().setSystem("http://system").setValue("id-2"));
        CodeSystem cs2 =
            new CodeSystem()
                .addIdentifier(new Identifier().setSystem("http://system").setValue("id-1"));
        List<String> sortCriteria = List.of(TerminologyResourceSearchParameter.SP_IDENTIFIER);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cs2, cs1);
      }
    }
  }

  @Nested
  @DisplayName("ValueSet Specific Field Sorting")
  class ValueSetSpecificFieldSortingTests {

    @Nested
    @DisplayName("reference sorting")
    class ReferenceSortingTests {

      @Test
      @DisplayName("should sort by reference system ascending")
      void shouldSortByReferenceSystemAscending() {
        // GIVEN
        ValueSet vs1 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addInclude(new ConceptSetComponent().setSystem("http://system-b")));
        ValueSet vs2 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addInclude(new ConceptSetComponent().setSystem("http://system-a")));
        List<String> sortCriteria = List.of(VsSearchParameter.SP_REFERENCE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ValueSet> sorted = Stream.of(vs1, vs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(vs2, vs1);
      }

      @Test
      @DisplayName("should use earliest reference for sorting")
      void shouldUseEarliestReferenceForSorting() {
        // GIVEN
        ValueSet vs1 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addInclude(new ConceptSetComponent().setSystem("http://system-c"))
                        .addInclude(new ConceptSetComponent().setSystem("http://system-b")));
        ValueSet vs2 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addInclude(new ConceptSetComponent().setSystem("http://system-a")));
        List<String> sortCriteria = List.of(VsSearchParameter.SP_REFERENCE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ValueSet> sorted = Stream.of(vs1, vs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(vs2, vs1);
      }

      @Test
      @DisplayName("should include exclude systems in reference sorting")
      void shouldIncludeExcludeSystemsInReferenceSorting() {
        // GIVEN
        ValueSet vs1 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addExclude(new ConceptSetComponent().setSystem("http://system-b")));
        ValueSet vs2 =
            new ValueSet()
                .setCompose(
                    new ValueSet.ValueSetComposeComponent()
                        .addInclude(new ConceptSetComponent().setSystem("http://system-a")));
        List<String> sortCriteria = List.of(VsSearchParameter.SP_REFERENCE);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ValueSet> sorted = Stream.of(vs1, vs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(vs2, vs1);
      }
    }

    @Nested
    @DisplayName("expansion sorting")
    class ExpansionSortingTests {

      @Test
      @DisplayName("should sort by expansion identifier ascending")
      void shouldSortByExpansionIdentifierAscending() {
        // GIVEN
        ValueSet vs1 =
            new ValueSet()
                .setExpansion(new ValueSet.ValueSetExpansionComponent().setIdentifier("exp-2"));
        ValueSet vs2 =
            new ValueSet()
                .setExpansion(new ValueSet.ValueSetExpansionComponent().setIdentifier("exp-1"));
        List<String> sortCriteria = List.of(VsSearchParameter.SP_EXPANSION);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ValueSet> sorted = Stream.of(vs1, vs2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(vs2, vs1);
      }
    }
  }

  @Nested
  @DisplayName("ConceptMap Specific Field Sorting")
  class ConceptMapSpecificFieldSortingTests {

    @Nested
    @DisplayName("source sorting")
    class SourceSortingTests {

      @Test
      @DisplayName("should sort by source canonical ascending")
      void shouldSortBySourceCanonicalAscending() {
        // GIVEN
        ConceptMap cm1 = new ConceptMap().setSource(new CanonicalType("http://source-b"));
        ConceptMap cm2 = new ConceptMap().setSource(new CanonicalType("http://source-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_SOURCE_CANONICAL);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }

      @Test
      @DisplayName("should sort by source uri ascending")
      void shouldSortBySourceUriAscending() {
        // GIVEN
        ConceptMap cm1 = new ConceptMap().setSource(new UriType("http://source-b"));
        ConceptMap cm2 = new ConceptMap().setSource(new UriType("http://source-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_SOURCE_URI);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }

    @Nested
    @DisplayName("target sorting")
    class TargetSortingTests {

      @Test
      @DisplayName("should sort by target canonical ascending")
      void shouldSortByTargetCanonicalAscending() {
        // GIVEN
        ConceptMap cm1 = new ConceptMap().setTarget(new CanonicalType("http://target-b"));
        ConceptMap cm2 = new ConceptMap().setTarget(new CanonicalType("http://target-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_TARGET_CANONICAL);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }

      @Test
      @DisplayName("should sort by target uri ascending")
      void shouldSortByTargetUriAscending() {
        // GIVEN
        ConceptMap cm1 = new ConceptMap().setTarget(new UriType("http://target-b"));
        ConceptMap cm2 = new ConceptMap().setTarget(new UriType("http://target-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_TARGET_URI);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }

    @Nested
    @DisplayName("source-system sorting")
    class SourceSystemSortingTests {

      @Test
      @DisplayName("should sort by source system ascending")
      void shouldSortBySourceSystemAscending() {
        // GIVEN
        ConceptMap cm1 =
            new ConceptMap().addGroup(new ConceptMapGroupComponent().setSource("http://source-b"));
        ConceptMap cm2 =
            new ConceptMap().addGroup(new ConceptMapGroupComponent().setSource("http://source-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_SOURCE_SYSTEM);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }

      @Test
      @DisplayName("should use earliest source system for sorting")
      void shouldUseEarliestSourceSystemForSorting() {
        // GIVEN
        ConceptMap cm1 =
            new ConceptMap()
                .addGroup(new ConceptMapGroupComponent().setSource("http://source-c"))
                .addGroup(new ConceptMapGroupComponent().setSource("http://source-b"));
        ConceptMap cm2 =
            new ConceptMap().addGroup(new ConceptMapGroupComponent().setSource("http://source-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_SOURCE_SYSTEM);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }

    @Nested
    @DisplayName("target-system sorting")
    class TargetSystemSortingTests {

      @Test
      @DisplayName("should sort by target system ascending")
      void shouldSortByTargetSystemAscending() {
        // GIVEN
        ConceptMap cm1 =
            new ConceptMap().addGroup(new ConceptMapGroupComponent().setTarget("http://target-b"));
        ConceptMap cm2 =
            new ConceptMap().addGroup(new ConceptMapGroupComponent().setTarget("http://target-a"));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_TARGET_SYSTEM);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }

    @Nested
    @DisplayName("dependsOn sorting")
    class DependsOnSortingTests {

      @Test
      @DisplayName("should sort by dependsOn property ascending")
      void shouldSortByDependsOnPropertyAscending() {
        // GIVEN
        ConceptMap cm1 =
            new ConceptMap()
                .addGroup(
                    new ConceptMapGroupComponent()
                        .addElement(
                            new SourceElementComponent()
                                .addTarget(
                                    new TargetElementComponent()
                                        .addDependsOn(
                                            new ConceptMap.OtherElementComponent()
                                                .setProperty("prop-b")))));
        ConceptMap cm2 =
            new ConceptMap()
                .addGroup(
                    new ConceptMapGroupComponent()
                        .addElement(
                            new SourceElementComponent()
                                .addTarget(
                                    new TargetElementComponent()
                                        .addDependsOn(
                                            new ConceptMap.OtherElementComponent()
                                                .setProperty("prop-a")))));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_DEPENDS_ON);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }

    @Nested
    @DisplayName("product sorting")
    class ProductSortingTests {

      @Test
      @DisplayName("should sort by product property ascending")
      void shouldSortByProductPropertyAscending() {
        // GIVEN
        ConceptMap cm1 =
            new ConceptMap()
                .addGroup(
                    new ConceptMapGroupComponent()
                        .addElement(
                            new SourceElementComponent()
                                .addTarget(
                                    new TargetElementComponent()
                                        .addProduct(
                                            new ConceptMap.OtherElementComponent()
                                                .setProperty("prod-b")))));
        ConceptMap cm2 =
            new ConceptMap()
                .addGroup(
                    new ConceptMapGroupComponent()
                        .addElement(
                            new SourceElementComponent()
                                .addTarget(
                                    new TargetElementComponent()
                                        .addProduct(
                                            new ConceptMap.OtherElementComponent()
                                                .setProperty("prod-a")))));
        List<String> sortCriteria = List.of(CmSearchParameter.SP_PRODUCT);

        // WHEN
        Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
        List<ConceptMap> sorted = Stream.of(cm1, cm2).sorted(comparator).toList();

        // THEN
        assertThat(sorted).containsExactly(cm2, cm1);
      }
    }
  }

  @Nested
  @DisplayName("Null Handling")
  class NullHandlingTests {

    @Test
    @DisplayName("should sort null resource last")
    void shouldSortNullResourceLast() {
      // GIVEN
      CodeSystem cs = new CodeSystem().setName("Test");
      List<String> sortCriteria = List.of("name");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs, null).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs, null);
    }

    @Test
    @DisplayName("should handle null field values")
    void shouldHandleNullFieldValues() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setName("Test");
      CodeSystem cs2 = new CodeSystem();
      List<String> sortCriteria = List.of("name");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs1, cs2);
    }
  }

  @Nested
  @DisplayName("Complex Sorting Scenarios")
  class ComplexSortingScenariosTests {

    @Test
    @DisplayName("should apply multiple sort criteria in order")
    void shouldApplyMultipleSortCriteriaInOrder() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setName("Charlie");
      CodeSystem cs2 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setName("Alpha");
      CodeSystem cs3 = new CodeSystem().setStatus(PublicationStatus.DRAFT).setName("Beta");
      List<String> sortCriteria = List.of("status", "name");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1, cs3);
    }

    @Test
    @DisplayName("should handle mixed ascending and descending criteria")
    void shouldHandleMixedAscendingAndDescendingCriteria() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setDate(new Date(1000));
      CodeSystem cs2 = new CodeSystem().setStatus(PublicationStatus.ACTIVE).setDate(new Date(2000));
      CodeSystem cs3 = new CodeSystem().setStatus(PublicationStatus.DRAFT).setDate(new Date(3000));
      List<String> sortCriteria = List.of("status", "-date");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2, cs3).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1, cs3);
    }
  }

  @Nested
  @DisplayName("Type-Specific Comparison")
  class TypeSpecificComparisonTests {

    @Test
    @DisplayName("should compare dates correctly")
    void shouldCompareDatesCorrectly() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setDate(new Date(2000));
      CodeSystem cs2 = new CodeSystem().setDate(new Date(1000));
      List<String> sortCriteria = List.of("date");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1);
    }

    @Test
    @DisplayName("should compare strings case-insensitively")
    void shouldCompareStringsCaseInsensitively() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setName("BETA");
      CodeSystem cs2 = new CodeSystem().setName("alpha");
      List<String> sortCriteria = List.of("name");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1);
    }

    @Test
    @DisplayName("should compare enums by their code")
    void shouldCompareEnumsByTheirCode() {
      // GIVEN
      CodeSystem cs1 = new CodeSystem().setStatus(PublicationStatus.RETIRED);
      CodeSystem cs2 = new CodeSystem().setStatus(PublicationStatus.ACTIVE);
      List<String> sortCriteria = List.of("status");

      // WHEN
      Comparator<IBaseResource> comparator = FhirResourceComparator.buildComparator(sortCriteria);
      List<CodeSystem> sorted = Stream.of(cs1, cs2).sorted(comparator).toList();

      // THEN
      assertThat(sorted).containsExactly(cs2, cs1);
    }
  }

  private static void sortTwoResources(
      Comparator<IBaseResource> comparator,
      IBaseResource leftResource,
      IBaseResource rightResource) {
    Stream.of(leftResource, rightResource).sorted(comparator).toList();
  }
}
