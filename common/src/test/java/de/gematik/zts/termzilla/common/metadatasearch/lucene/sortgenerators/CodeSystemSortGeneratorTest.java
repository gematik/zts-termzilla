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

import java.util.List;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CodeSystemSortGenerator}. */
@DisplayName("CodeSystemSortGenerator Tests")
class CodeSystemSortGeneratorTest {

  @Nested
  @DisplayName("generateSort() Tests")
  class GenerateSortTests {

    @Test
    @DisplayName("Should return default sort when parameters are null")
    void shouldReturnDefaultSortWhenParametersAreNull() {
      // GIVEN
      List<String> sortParamList = null;

      // WHEN
      Sort sort = CodeSystemSortGenerator.generateSort(sortParamList);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.DEFAULT_NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should support CodeSystem specific content-mode sort")
    void shouldSupportCodeSystemSpecificContentModeSort() {
      // GIVEN
      List<String> sortParamList = List.of("-content-mode");

      // WHEN
      Sort sort = CodeSystemSortGenerator.generateSort(sortParamList);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.CONTENT_MODE_SORT_FIELD, SortField.Type.STRING, true);
    }

    @Test
    @DisplayName("Should combine common and CodeSystem specific sort fields in declared order")
    void shouldCombineCommonAndCodeSystemSpecificSortFieldsInDeclaredOrder() {
      // GIVEN
      List<String> sortParamList = List.of("content-mode", "-date", "name");

      // WHEN
      Sort sort = CodeSystemSortGenerator.generateSort(sortParamList);

      // THEN
      SortField[] actualSortFields = sort.getSort();
      assertThat(actualSortFields)
          .as("All supported fields should be kept in request order")
          .hasSize(3);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[0],
          SortGeneratorTestDataHelper.CONTENT_MODE_SORT_FIELD,
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
    @DisplayName("Should ignore unsupported parameters")
    void shouldIgnoreUnsupportedParameters() {
      // GIVEN
      List<String> sortParamList = List.of("unknown", "name", "-invalid");

      // WHEN
      Sort sort = CodeSystemSortGenerator.generateSort(sortParamList);

      // THEN
      SortGeneratorTestDataHelper.assertSingleSortField(
          sort, SortGeneratorTestDataHelper.NAME_SORT_FIELD, SortField.Type.STRING, false);
    }

    @Test
    @DisplayName("Should normalize mixed-case parameters")
    void shouldNormalizeMixedCaseParameters() {
      // GIVEN
      List<String> sortParamList = List.of("ConTenT-MOde", "-DaTe");

      // WHEN
      Sort sort = CodeSystemSortGenerator.generateSort(sortParamList);

      // THEN
      SortField[] actualSortFields = sort.getSort();
      assertThat(actualSortFields)
          .as("Mixed-case sort parameters should be handled case-insensitively")
          .hasSize(2);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[0],
          SortGeneratorTestDataHelper.CONTENT_MODE_SORT_FIELD,
          SortField.Type.STRING,
          false);
      SortGeneratorTestDataHelper.assertSortField(
          actualSortFields[1],
          SortGeneratorTestDataHelper.DATE_SORT_FIELD,
          SortField.Type.LONG,
          true);
    }
  }
}
