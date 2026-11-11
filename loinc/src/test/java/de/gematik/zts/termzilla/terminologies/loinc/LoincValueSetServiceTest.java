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
package de.gematik.zts.termzilla.terminologies.loinc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.loinc.memory.LoincInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link LoincValueSetService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoincValueSetService tests")
class LoincValueSetServiceTest {

  private static final String TEST_SCOPE = "test-scope";
  private static final String ALL_LOINC_CONCEPTS_URL = "http://loinc.org/vs";
  private static final String ANSWER_LIST_ID = "LL715-4";
  private static final String ANSWER_LIST_URL = "http://loinc.org/vs/" + ANSWER_LIST_ID;
  private static final String VERSION_281 = "2.81";
  private static final String VERSION_282 = "2.82";

  @Mock private LoincTerminologyManager loincTerminologyManager;
  @Mock private Loinc loinc;
  @Mock private LoincInMemoryRepresentation loincInMemoryRepresentation;
  @Mock private LoincCodeSystemService loincCodeSystemService;

  private LoincValueSetService loincValueSetService;
  private LoincVersion loincVersion281;
  private LoincVersion loincVersion282;

  @BeforeEach
  void setUp() {
    // GIVEN
    this.loincVersion281 = LoincTestDataHelper.loincVersion(281, VERSION_281);
    this.loincVersion282 = LoincTestDataHelper.loincVersion(282, VERSION_282);

    AnswerListVersion answerListVersion281 =
        LoincTestDataHelper.answerListVersion(
            ANSWER_LIST_ID,
            LoincConstants.LOINC_STATUSCODE_ACTIVE,
            "Answer list 2.81",
            Set.of("LA1", "LA2"));
    AnswerListVersion answerListVersion282 =
        LoincTestDataHelper.answerListVersion(
            ANSWER_LIST_ID,
            LoincConstants.LOINC_STATUSCODE_ACTIVE,
            "Answer list 2.82",
            Set.of("LA3", "LA4"));

    MultiValuedMap<String, AnswerListVersion> answerListMap = new ArrayListValuedHashMap<>();
    answerListMap.put(ANSWER_LIST_ID, answerListVersion281);

    Map<String, LoincVersion> fhirVersionToLoincVersionMap = new HashMap<>();

    when(loincTerminologyManager.getLoinc()).thenReturn(loinc);
    when(loincTerminologyManager.cs()).thenReturn(loincCodeSystemService);
    when(loincCodeSystemService.getFhirVersionToLoincVersionMap())
        .thenReturn(fhirVersionToLoincVersionMap);
    when(loinc.getSupportedLoincVersions()).thenReturn(List.of(loincVersion281, loincVersion282));
    lenient().when(loinc.getDefaultLoincVersion()).thenReturn(loincVersion282);
    when(loinc.getLoincInMemoryRepresentation()).thenReturn(loincInMemoryRepresentation);
    when(loincInMemoryRepresentation.getAnswerListIdToAnswerListVersionMap())
        .thenReturn(answerListMap);
    when(loincInMemoryRepresentation.getAnswerListVersion(ANSWER_LIST_ID, loincVersion281))
        .thenReturn(Optional.of(answerListVersion281));
    when(loincInMemoryRepresentation.getAnswerListVersion(ANSWER_LIST_ID, loincVersion282))
        .thenReturn(Optional.of(answerListVersion282));

    // WHEN
    this.loincValueSetService = new LoincValueSetService(loincTerminologyManager);
  }

  private String findValueSetIdByUrlAndVersion(String url, String version) {
    return loincValueSetService.getValueSetList().stream()
        .filter(valueSet -> valueSet.getUrl().equals(url))
        .filter(valueSet -> valueSet.getVersion().equals(version))
        .map(ValueSet::getId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Expected test ValueSet was not generated."));
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTest {

    @Test
    @DisplayName("should generate implicit ValueSets for each supported version and answer list")
    void shouldGenerateImplicitValueSetsForAllSupportedVersions() {
      // GIVEN
      int expectedValueSetCount = 4;

      // WHEN
      List<ValueSet> generatedValueSets = loincValueSetService.getValueSetList();

      // THEN
      assertThat(generatedValueSets)
          .hasSize(expectedValueSetCount)
          .allMatch(valueSet -> valueSet.getStatus() == Enumerations.PublicationStatus.ACTIVE);
    }
  }

  @Nested
  @DisplayName("isValueSetSupported")
  class IsValueSetSupportedTest {

    @Test
    @DisplayName("should return true for existing canonical URL without version")
    void shouldReturnTrueForExistingCanonicalWithoutVersion() throws UnknownScopeException {
      // GIVEN
      String canonical = ALL_LOINC_CONCEPTS_URL;

      // WHEN
      boolean result = loincValueSetService.isValueSetSupported(TEST_SCOPE, canonical, null);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true for existing canonical URL and existing version")
    void shouldReturnTrueForExistingCanonicalWithVersion() throws UnknownScopeException {
      // GIVEN
      String canonical = ALL_LOINC_CONCEPTS_URL;

      // WHEN
      boolean result = loincValueSetService.isValueSetSupported(TEST_SCOPE, canonical, VERSION_282);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for unknown canonical URL")
    void shouldReturnFalseForUnknownCanonical() throws UnknownScopeException {
      // GIVEN
      String unknownCanonical = "http://loinc.org/vs/not-supported";

      // WHEN
      boolean result = loincValueSetService.isValueSetSupported(TEST_SCOPE, unknownCanonical, null);

      // THEN
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("getSupportedValueSetCanonicalList")
  class GetSupportedValueSetCanonicalListTest {

    @Test
    @DisplayName("should return distinct canonical URLs")
    void shouldReturnDistinctCanonicalUrls() {
      // GIVEN
      int expectedCanonicalCount = 2;

      // WHEN
      List<String> canonicalList = loincValueSetService.getSupportedValueSetCanonicalList();

      // THEN
      assertThat(canonicalList)
          .containsExactlyInAnyOrder(ALL_LOINC_CONCEPTS_URL, ANSWER_LIST_URL)
          .hasSize(expectedCanonicalCount);
    }
  }

  @Nested
  @DisplayName("getValueSetList")
  class GetValueSetListTest {

    @Test
    @DisplayName("should return all generated ValueSets")
    void shouldReturnAllGeneratedValueSets() {
      // GIVEN
      int expectedValueSetCount = 4;

      // WHEN
      List<ValueSet> valueSetList = loincValueSetService.getValueSetList();

      // THEN
      assertThat(valueSetList).hasSize(expectedValueSetCount);
    }
  }

  @Nested
  @DisplayName("getValueSet by id")
  class GetValueSetByIdTest {

    @Test
    @DisplayName("should return ValueSet by id and retrieval mode")
    void shouldReturnValueSetByIdAndRetrievalMode() {
      // GIVEN
      String valueSetId = findValueSetIdByUrlAndVersion(ALL_LOINC_CONCEPTS_URL, VERSION_281);
      IdType idType = new IdType(valueSetId);

      // WHEN
      ValueSet valueSet =
          loincValueSetService.getValueSet(TEST_SCOPE, idType, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(valueSet)
          .isNotNull()
          .extracting(ValueSet::getUrl, ValueSet::getVersion)
          .containsExactly(ALL_LOINC_CONCEPTS_URL, VERSION_281);
    }

    @Test
    @DisplayName("should return ValueSet by id using overload without retrieval mode")
    void shouldReturnValueSetByIdUsingOverload() {
      // GIVEN
      String valueSetId = findValueSetIdByUrlAndVersion(ANSWER_LIST_URL, VERSION_282);
      IdType idType = new IdType(valueSetId);

      // WHEN
      ValueSet valueSet = loincValueSetService.getValueSet(TEST_SCOPE, idType);

      // THEN
      assertThat(valueSet)
          .isNotNull()
          .extracting(ValueSet::getUrl, ValueSet::getVersion)
          .containsExactly(ANSWER_LIST_URL, VERSION_282);
    }
  }

  @Nested
  @DisplayName("getValueSetList by URL")
  class GetValueSetListByUrlTest {

    @Test
    @DisplayName("should return all versions for matching canonical URL")
    void shouldReturnAllVersionsForCanonicalUrl() {
      // GIVEN
      String canonical = ALL_LOINC_CONCEPTS_URL;

      // WHEN
      List<ValueSet> valueSetList = loincValueSetService.getValueSetList(TEST_SCOPE, canonical);

      // THEN
      assertThat(valueSetList)
          .hasSize(2)
          .extracting(ValueSet::getVersion)
          .containsExactlyInAnyOrder(VERSION_281, VERSION_282);
    }
  }

  @Nested
  @DisplayName("getValueSet by URL and version")
  class GetValueSetByUrlAndVersionTest {

    @Test
    @DisplayName("should return default version when version parameter is null")
    void shouldReturnDefaultVersionWhenVersionIsNull()
        throws UnknownScopeException, UnsupportedTerminologyException {
      // GIVEN
      String canonical = ALL_LOINC_CONCEPTS_URL;

      // WHEN
      ValueSet valueSet =
          loincValueSetService.getValueSet(TEST_SCOPE, canonical, null, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(valueSet).isNotNull().extracting(ValueSet::getVersion).isEqualTo(VERSION_282);
    }

    @Test
    @DisplayName("should return matching ValueSet for explicit version")
    void shouldReturnMatchingValueSetForExplicitVersion()
        throws UnknownScopeException, UnsupportedTerminologyException {
      // GIVEN
      String canonical = ANSWER_LIST_URL;

      // WHEN
      ValueSet valueSet =
          loincValueSetService.getValueSet(
              TEST_SCOPE, canonical, VERSION_281, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(valueSet)
          .isNotNull()
          .extracting(ValueSet::getUrl, ValueSet::getVersion)
          .containsExactly(ANSWER_LIST_URL, VERSION_281);
    }

    @Test
    @DisplayName(
        "should throw ResourceNotFoundException when URL and version combination is unknown")
    void shouldThrowWhenUrlAndVersionCombinationIsUnknown() {
      // GIVEN
      String canonical = ANSWER_LIST_URL;
      String unknownVersion = "9.99";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  loincValueSetService.getValueSet(
                      TEST_SCOPE, canonical, unknownVersion, ResourceRetrievalMode.FULL))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining(canonical)
          .hasMessageContaining(unknownVersion);
    }
  }

  @Nested
  @DisplayName("getValueSet by URL and version overload")
  class GetValueSetByUrlAndVersionOverloadTest {

    @Test
    @DisplayName("should delegate overload without retrieval mode")
    void shouldDelegateOverloadWithoutRetrievalMode()
        throws UnknownScopeException, UnsupportedTerminologyException {
      // GIVEN
      String canonical = ALL_LOINC_CONCEPTS_URL;

      // WHEN
      ValueSet valueSet = loincValueSetService.getValueSet(TEST_SCOPE, canonical, VERSION_281);

      // THEN
      assertThat(valueSet).isNotNull().extracting(ValueSet::getVersion).isEqualTo(VERSION_281);
    }
  }

  @Nested
  @DisplayName("searchValueSet")
  class SearchValueSetTest {

    @Test
    @DisplayName("should return empty list because search is not yet implemented")
    void shouldReturnEmptyList() throws NotImplementedException, UnknownScopeException {
      // GIVEN
      VsSearchParameter searchParameter = VsSearchParameter.builder().build();

      // WHEN
      List<IdType> result = loincValueSetService.searchValueSet(TEST_SCOPE, searchParameter);

      // THEN
      assertThat(result).isEmpty();
    }
  }
}
