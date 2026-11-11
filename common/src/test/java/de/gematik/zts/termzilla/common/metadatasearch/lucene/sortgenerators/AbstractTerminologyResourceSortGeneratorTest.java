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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.sortgenerators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import java.util.List;
import java.util.Set;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link AbstractTerminologyResourceSortGenerator}. */
@DisplayName("AbstractTerminologyResourceSortGenerator Tests")
class AbstractTerminologyResourceSortGeneratorTest {

  @Nested
  @DisplayName("getDefaultSort() Tests")
  class GetDefaultSortTests {

    @Test
    @DisplayName("Should return ascending sort by name_search")
    void shouldReturnAscendingSortByNameSearch() {
      // GIVEN
      // No explicit sort parameters

      // WHEN
      Sort sort = AbstractTerminologyResourceSortGenerator.getDefaultSort();

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
    }
  }

  @Nested
  @DisplayName("createCommonSortField() Tests")
  class CreateCommonSortFieldTests {

    @ParameterizedTest(name = "param={0}")
    @MethodSource(
        "de.gematik.zts.termzilla.common.metadatasearch.lucene.sortgenerators.SortGeneratorTestDataHelper#commonSortFieldCases")
    @DisplayName("Should map supported parameters to expected Lucene field definitions")
    void shouldMapSupportedParametersToExpectedLuceneFieldDefinitions(
        String sortParam, String expectedFieldName, SortField.Type expectedType) {
      // GIVEN
      boolean isDescending = false;

      // WHEN
      SortField sortField =
          AbstractTerminologyResourceSortGenerator.createCommonSortField(sortParam, isDescending);

      // THEN
      SortGeneratorTestDataHelper.assertSortField(
          sortField, expectedFieldName, expectedType, false);
    }

    @Test
    @DisplayName("Should create descending field when direction flag is true")
    void shouldCreateDescendingFieldWhenDirectionFlagIsTrue() {
      // GIVEN
      String sortParam = TerminologyResourceSearchParameter.SP_DATE;
      boolean isDescending = true;

      // WHEN
      SortField sortField =
          AbstractTerminologyResourceSortGenerator.createCommonSortField(sortParam, isDescending);

      // THEN
      SortGeneratorTestDataHelper.assertSortField(
          sortField, SortGeneratorTestDataHelper.DATE_SORT_FIELD, SortField.Type.LONG, true);
    }

    @Test
    @DisplayName("Should return null for unsupported parameter")
    void shouldReturnNullForUnsupportedParameter() {
      // GIVEN
      String unsupportedSortParam = "unsupported";

      // WHEN
      SortField sortField =
          AbstractTerminologyResourceSortGenerator.createCommonSortField(
              unsupportedSortParam, false);

      // THEN
      assertThat(sortField)
          .as("Unsupported sort parameter should not produce a sort field")
          .isNull();
    }
  }

  @Nested
  @DisplayName("generateSort() Tests")
  class GenerateSortTests {

    @Test
    @DisplayName("Should return default sort for null sort parameter list")
    void shouldReturnDefaultSortForNullSortParameterList() {
      // GIVEN
      List<String> sortParamList = null;
      Set<String> supportedSortFields = SortGeneratorTestDataHelper.commonSupportedSortFields();
      AbstractTerminologyResourceSortGenerator.SortFieldCreator creator =
          AbstractTerminologyResourceSortGenerator::createCommonSortField;

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList, supportedSortFields, creator);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should return default sort for empty sort parameter list")
    void shouldReturnDefaultSortForEmptySortParameterList() {
      // GIVEN
      List<String> sortParamList = List.of();
      Set<String> supportedSortFields = SortGeneratorTestDataHelper.commonSupportedSortFields();
      AbstractTerminologyResourceSortGenerator.SortFieldCreator creator =
          AbstractTerminologyResourceSortGenerator::createCommonSortField;

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList, supportedSortFields, creator);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should preserve parameter order and sorting direction")
    void shouldPreserveParameterOrderAndSortingDirection() {
      // GIVEN
      List<String> sortParamList = List.of("status", "-date", "name");

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList,
              SortGeneratorTestDataHelper.commonSupportedSortFields(),
              AbstractTerminologyResourceSortGenerator::createCommonSortField);

      // THEN
      SortField[] actualSortFields = sort.getSort();
      assertThat(actualSortFields)
          .as("Sort should contain the same amount of supported parameters")
          .hasSize(3);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[0],
          SortGeneratorTestDataHelper.STATUS_SORT_FIELD,
          SortField.Type.STRING,
          false);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[1],
          SortGeneratorTestDataHelper.DATE_SORT_FIELD,
          SortField.Type.LONG,
          true);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[2],
          SortGeneratorTestDataHelper.NAME_SORT_FIELD,
          SortField.Type.STRING,
          false);
    }

    @Test
    @DisplayName("Should ignore unsupported sort parameters")
    void shouldIgnoreUnsupportedSortParameters() {
      // GIVEN
      List<String> sortParamList = List.of("unsupported", "name", "-invalid");

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList,
              SortGeneratorTestDataHelper.commonSupportedSortFields(),
              AbstractTerminologyResourceSortGenerator::createCommonSortField);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should normalize sort parameter casing")
    void shouldNormalizeSortParameterCasing() {
      // GIVEN
      List<String> sortParamList = SortGeneratorTestDataHelper.mixedCaseCommonSortParams();

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList,
              SortGeneratorTestDataHelper.commonSupportedSortFields(),
              AbstractTerminologyResourceSortGenerator::createCommonSortField);

      // THEN
      SortField[] actualSortFields = sort.getSort();
      assertThat(actualSortFields)
          .as("Casing differences should not affect sort parameter handling")
          .hasSize(2);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[0],
          SortGeneratorTestDataHelper.NAME_SORT_FIELD,
          SortField.Type.STRING,
          false);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[1],
          SortGeneratorTestDataHelper.DATE_SORT_FIELD,
          SortField.Type.LONG,
          true);
    }

    @Test
    @DisplayName("Should return default sort when all parameters are unsupported")
    void shouldReturnDefaultSortWhenAllParametersAreUnsupported() {
      // GIVEN
      AbstractTerminologyResourceSortGenerator.SortFieldCreator creator =
          mock(AbstractTerminologyResourceSortGenerator.SortFieldCreator.class);
      List<String> sortParamList = List.of("unsupported");

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList, Set.of("name"), creator);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
      verifyNoInteractions(creator);
    }

    @Test
    @DisplayName("Should skip creator results that are null")
    void shouldSkipCreatorResultsThatAreNull() {
      // GIVEN
      AbstractTerminologyResourceSortGenerator.SortFieldCreator creator =
          mock(AbstractTerminologyResourceSortGenerator.SortFieldCreator.class);
      SortField expectedNameSort =
          new SortField(SortGeneratorTestDataHelper.NAME_SORT_FIELD, SortField.Type.STRING, false);
      when(creator.create("name", false)).thenReturn(expectedNameSort);
      when(creator.create("status", true)).thenReturn(null);
      List<String> sortParamList = List.of("name", "-status");

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList, Set.of("name", "status"), creator);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.NAME_SORT_FIELD, SortField.Type.STRING, false);
      verify(creator).create("name", false);
      verify(creator).create("status", true);
    }

    @Test
    @DisplayName("Should ignore null entries and still process valid sort parameters")
    void shouldIgnoreNullEntriesAndStillProcessValidSortParameters() {
      // GIVEN
      List<String> sortParamList = java.util.Arrays.asList("name", null);

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList,
              SortGeneratorTestDataHelper.commonSupportedSortFields(),
              AbstractTerminologyResourceSortGenerator::createCommonSortField);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should return default sort when sort list contains only null entries")
    void shouldReturnDefaultSortWhenSortListContainsOnlyNullEntries() {
      // GIVEN
      List<String> sortParamList = java.util.Arrays.asList(null, null);

      // WHEN
      Sort sort =
          AbstractTerminologyResourceSortGenerator.generateSort(
              sortParamList,
              SortGeneratorTestDataHelper.commonSupportedSortFields(),
              AbstractTerminologyResourceSortGenerator::createCommonSortField);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
    }
  }

  private static final class TestSortGenerator extends AbstractTerminologyResourceSortGenerator {

    private TestSortGenerator() {
      super();
    }
  }
}
