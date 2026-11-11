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
package de.gematik.zts.termzilla.common.codesystem.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import java.util.List;
import java.util.function.Consumer;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Unit tests for {@link InMemoryCodeSystemSearchHelper}. */
@DisplayName("InMemoryCodeSystemSearchHelper Tests")
class InMemoryCodeSystemSearchHelperTest {

  @Nested
  @DisplayName("search")
  class SearchTests {

    @Test
    @DisplayName("Should return all ids and notify observer in input order for empty parameters")
    void shouldReturnAllIdsAndNotifyObserverInInputOrderForEmptyParameters() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.emptySearchParameter();
      @SuppressWarnings("unchecked")
      Consumer<CodeSystem> observer = mock(Consumer.class);

      // WHEN
      List<IdType> result =
          InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, observer);

      // THEN
      assertThat(result).extracting(IdType::getIdPart).containsExactly("alpha", "beta", "gamma");

      InOrder inOrder = inOrder(observer);
      inOrder.verify(observer).accept(codeSystems.getFirst());
      inOrder.verify(observer).accept(codeSystems.get(1));
      inOrder.verify(observer).accept(codeSystems.get(2));
    }

    @Test
    @DisplayName("Should perform content union and remove duplicates")
    void shouldPerformContentUnionAndRemoveDuplicates() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withContent(
              InMemoryCodeSystemSearchHelperTestDataHelper.VERSION_SHARED);
      @SuppressWarnings("unchecked")
      Consumer<CodeSystem> observer = mock(Consumer.class);

      // WHEN
      List<IdType> result =
          InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, observer);

      // THEN
      assertThat(result).extracting(IdType::getIdPart).containsExactly("alpha");
      InOrder inOrder = inOrder(observer);
      inOrder.verify(observer).accept(codeSystems.getFirst());
    }

    @Test
    @DisplayName("Should combine metadata filters with AND semantics")
    void shouldCombineMetadataFiltersWithAndSemantics() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withMetadataNameAndPublisher(
              "Alpha", "Alpha");
      @SuppressWarnings("unchecked")
      Consumer<CodeSystem> observer = mock(Consumer.class);

      // WHEN
      List<IdType> result =
          InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, observer);

      // THEN
      assertThat(result).extracting(IdType::getIdPart).containsExactly("alpha");
      InOrder inOrder = inOrder(observer);
      inOrder.verify(observer).accept(codeSystems.getFirst());
    }

    @Test
    @DisplayName("Should return empty results when non-empty source parameter is provided")
    void shouldReturnEmptyResultsWhenNonEmptySourceParameterIsProvided() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withMetadataSource(
              "http://example.org/source");
      @SuppressWarnings("unchecked")
      Consumer<CodeSystem> observer = mock(Consumer.class);

      // WHEN
      List<IdType> result =
          InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, observer);

      // THEN
      assertThat(result).isEmpty();
      verifyNoInteractions(observer);
    }

    @Test
    @DisplayName("Should throw NotImplementedException when code search is requested")
    void shouldThrowWhenCodeSearchIsRequested() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withCode("abc");

      // WHEN / THEN
      assertThatThrownBy(
              () -> InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, null))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Code search not implemented");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when content mode search is requested")
    void shouldThrowWhenContentModeSearchIsRequested() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withContentMode("fragment");

      // WHEN / THEN
      assertThatThrownBy(
              () -> InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, null))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Content-Mode");
    }

    @Test
    @DisplayName("Should throw NotImplementedException when status search is requested")
    void shouldThrowWhenStatusSearchIsRequested() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      CsSearchParameter searchParameter =
          InMemoryCodeSystemSearchHelperTestDataHelper.withStatus("active");

      // WHEN / THEN
      assertThatThrownBy(
              () -> InMemoryCodeSystemSearchHelper.search(codeSystems, searchParameter, null))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Status not implemented");
    }
  }

  @Nested
  @DisplayName("filterByUrl")
  class FilterByUrlTests {

    @Test
    @DisplayName("Should filter by exact URL value")
    void shouldFilterByExactUrlValue() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      UriParam urlParam = new UriParam(InMemoryCodeSystemSearchHelperTestDataHelper.URL_ALPHA);

      // WHEN
      List<CodeSystem> result = InMemoryCodeSystemSearchHelper.filterByUrl(codeSystems, urlParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }

    @Test
    @DisplayName("Should support missing=true for URL")
    void shouldSupportMissingTrueForUrl() throws Exception {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      UriParam urlParam = new UriParam();
      urlParam.setMissing(Boolean.TRUE);

      // WHEN
      List<CodeSystem> result = InMemoryCodeSystemSearchHelper.filterByUrl(codeSystems, urlParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/gamma");
    }

    @Test
    @DisplayName("Should reject :above qualifier")
    void shouldRejectAboveQualifier() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      UriParam urlParam = new UriParam(InMemoryCodeSystemSearchHelperTestDataHelper.URL_ALPHA);
      urlParam.setQualifier(UriParamQualifierEnum.ABOVE);

      // WHEN / THEN
      assertThatThrownBy(() -> InMemoryCodeSystemSearchHelper.filterByUrl(codeSystems, urlParam))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("ABOVE not implemented");
    }

    @Test
    @DisplayName("Should reject :below qualifier")
    void shouldRejectBelowQualifier() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      UriParam urlParam = new UriParam(InMemoryCodeSystemSearchHelperTestDataHelper.URL_ALPHA);
      urlParam.setQualifier(UriParamQualifierEnum.BELOW);

      // WHEN / THEN
      assertThatThrownBy(() -> InMemoryCodeSystemSearchHelper.filterByUrl(codeSystems, urlParam))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("BELOW not implemented");
    }
  }

  @Nested
  @DisplayName("filterByVersion")
  class FilterByVersionTests {

    @Test
    @DisplayName("Should filter by version value")
    void shouldFilterByVersionValue() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      TokenParam versionParam =
          new TokenParam(InMemoryCodeSystemSearchHelperTestDataHelper.VERSION_SHARED);

      // WHEN
      List<CodeSystem> result =
          InMemoryCodeSystemSearchHelper.filterByVersion(codeSystems, versionParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }

    @Test
    @DisplayName("Should support missing=false for version")
    void shouldSupportMissingFalseForVersion() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      TokenParam versionParam = new TokenParam();
      versionParam.setMissing(Boolean.FALSE);

      // WHEN
      List<CodeSystem> result =
          InMemoryCodeSystemSearchHelper.filterByVersion(codeSystems, versionParam);

      // THEN
      assertThat(result)
          .extracting(CodeSystem::getId)
          .containsExactly("CodeSystem/alpha", "CodeSystem/beta");
    }
  }

  @Nested
  @DisplayName("filterByName")
  class FilterByNameTests {

    @Test
    @DisplayName("Should use prefix matching by default")
    void shouldUsePrefixMatchingByDefault() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam nameParam = new StringParam("Alpha");

      // WHEN
      List<CodeSystem> result = InMemoryCodeSystemSearchHelper.filterByName(codeSystems, nameParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }

    @Test
    @DisplayName("Should use exact matching when exact modifier is set")
    void shouldUseExactMatchingWhenExactModifierIsSet() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam nameParam =
          new StringParam(InMemoryCodeSystemSearchHelperTestDataHelper.NAME_ALPHA);
      nameParam.setExact(Boolean.TRUE);

      // WHEN
      List<CodeSystem> result = InMemoryCodeSystemSearchHelper.filterByName(codeSystems, nameParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }

    @Test
    @DisplayName("Should use contains matching when contains modifier is set")
    void shouldUseContainsMatchingWhenContainsModifierIsSet() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam nameParam = new StringParam("phan");
      nameParam.setContains(Boolean.TRUE);

      // WHEN
      List<CodeSystem> result = InMemoryCodeSystemSearchHelper.filterByName(codeSystems, nameParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }
  }

  @Nested
  @DisplayName("filterByTitle")
  class FilterByTitleTests {

    @Test
    @DisplayName("Should filter by title with default prefix behavior")
    void shouldFilterByTitleWithDefaultPrefixBehavior() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam titleParam = new StringParam("Alpha");

      // WHEN
      List<CodeSystem> result =
          InMemoryCodeSystemSearchHelper.filterByTitle(codeSystems, titleParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/alpha");
    }
  }

  @Nested
  @DisplayName("filterByDescription")
  class FilterByDescriptionTests {

    @Test
    @DisplayName("Should filter by description with contains behavior")
    void shouldFilterByDescriptionWithContainsBehavior() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam descriptionParam = new StringParam("description");
      descriptionParam.setContains(Boolean.TRUE);

      // WHEN
      List<CodeSystem> result =
          InMemoryCodeSystemSearchHelper.filterByDescription(codeSystems, descriptionParam);

      // THEN
      assertThat(result)
          .extracting(CodeSystem::getId)
          .containsExactly("CodeSystem/alpha", "CodeSystem/beta");
    }
  }

  @Nested
  @DisplayName("filterByPublisher")
  class FilterByPublisherTests {

    @Test
    @DisplayName("Should support missing=true for publisher")
    void shouldSupportMissingTrueForPublisher() {
      // GIVEN
      List<CodeSystem> codeSystems =
          InMemoryCodeSystemSearchHelperTestDataHelper.createDefaultCodeSystems();
      StringParam publisherParam = new StringParam();
      publisherParam.setMissing(Boolean.TRUE);

      // WHEN
      List<CodeSystem> result =
          InMemoryCodeSystemSearchHelper.filterByPublisher(codeSystems, publisherParam);

      // THEN
      assertThat(result).extracting(CodeSystem::getId).containsExactly("CodeSystem/gamma");
    }
  }
}
