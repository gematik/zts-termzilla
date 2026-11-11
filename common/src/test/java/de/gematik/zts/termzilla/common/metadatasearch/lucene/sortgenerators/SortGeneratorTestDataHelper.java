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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_CONTENT_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_DESCRIPTION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_NAME;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_PUBLISHER;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_STATUS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_TITLE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_STRING_SEARCH;

import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.jupiter.params.provider.Arguments;

/** Provides reusable test data and assertions for sort generator tests. */
final class SortGeneratorTestDataHelper {

  static final String DEFAULT_NAME_SORT_FIELD = NAME_NAME + SEPARATOR + SUFFIX_STRING_SEARCH;
  static final String NAME_SORT_FIELD = NAME_NAME + SEPARATOR + SUFFIX_SORT;
  static final String URL_SORT_FIELD = NAME_URL + SEPARATOR + SUFFIX_SORT;
  static final String VERSION_SORT_FIELD = NAME_VERSION + SEPARATOR + SUFFIX_SORT;
  static final String TITLE_SORT_FIELD = NAME_TITLE + SEPARATOR + SUFFIX_SORT;
  static final String DATE_SORT_FIELD = CodeSystem.SP_DATE + SEPARATOR + SUFFIX_SORT;
  static final String PUBLISHER_SORT_FIELD = NAME_PUBLISHER + SEPARATOR + SUFFIX_SORT;
  static final String STATUS_SORT_FIELD = NAME_STATUS + SEPARATOR + SUFFIX_SORT;
  static final String DESCRIPTION_SORT_FIELD = NAME_DESCRIPTION + SEPARATOR + SUFFIX_SORT;
  static final String CONTENT_MODE_SORT_FIELD = NAME_CONTENT_MODE + SEPARATOR + SUFFIX_SORT;

  private SortGeneratorTestDataHelper() {}

  static Set<String> commonSupportedSortFields() {
    return Set.of(
        TerminologyResourceSearchParameter.SP_NAME,
        TerminologyResourceSearchParameter.SP_URL,
        TerminologyResourceSearchParameter.SP_VERSION,
        TerminologyResourceSearchParameter.SP_TITLE,
        TerminologyResourceSearchParameter.SP_DATE,
        TerminologyResourceSearchParameter.SP_PUBLISHER,
        TerminologyResourceSearchParameter.SP_STATUS,
        TerminologyResourceSearchParameter.SP_DESCRIPTION);
  }

  static Set<String> codeSystemSupportedSortFields() {
    return Set.of(
        TerminologyResourceSearchParameter.SP_NAME,
        TerminologyResourceSearchParameter.SP_URL,
        TerminologyResourceSearchParameter.SP_VERSION,
        TerminologyResourceSearchParameter.SP_TITLE,
        TerminologyResourceSearchParameter.SP_DATE,
        TerminologyResourceSearchParameter.SP_PUBLISHER,
        TerminologyResourceSearchParameter.SP_STATUS,
        TerminologyResourceSearchParameter.SP_DESCRIPTION,
        CsSearchParameter.SP_CONTENT_MODE);
  }

  static Stream<Arguments> commonSortFieldCases() {
    return Stream.of(
        Arguments.of(
            TerminologyResourceSearchParameter.SP_NAME, NAME_SORT_FIELD, SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_URL, URL_SORT_FIELD, SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_VERSION,
            VERSION_SORT_FIELD,
            SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_TITLE, TITLE_SORT_FIELD, SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_DATE, DATE_SORT_FIELD, SortField.Type.LONG),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_PUBLISHER,
            PUBLISHER_SORT_FIELD,
            SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_STATUS, STATUS_SORT_FIELD, SortField.Type.STRING),
        Arguments.of(
            TerminologyResourceSearchParameter.SP_DESCRIPTION,
            DESCRIPTION_SORT_FIELD,
            SortField.Type.STRING));
  }

  static void assertSingleSortField(
      Sort sort, String expectedFieldName, SortField.Type expectedType, boolean expectedReverse) {

    SortField[] actualSortFields = sort.getSort();
    org.assertj.core.api.Assertions.assertThat(actualSortFields)
        .as("Sort should contain exactly one sort field")
        .hasSize(1);

    assertSortField(actualSortFields[0], expectedFieldName, expectedType, expectedReverse);
  }

  static void assertSortField(
      SortField sortField,
      String expectedFieldName,
      SortField.Type expectedType,
      boolean expectedReverse) {

    org.assertj.core.api.Assertions.assertThat(sortField.getField())
        .as("Unexpected sort field name")
        .isEqualTo(expectedFieldName);
    org.assertj.core.api.Assertions.assertThat(sortField.getType())
        .as("Unexpected sort field type for '%s'", expectedFieldName)
        .isEqualTo(expectedType);
    org.assertj.core.api.Assertions.assertThat(sortField.getReverse())
        .as("Unexpected sort direction for '%s'", expectedFieldName)
        .isEqualTo(expectedReverse);
  }

  static List<String> mixedCaseCommonSortParams() {
    return List.of("NaMe", "-DaTe");
  }
}
