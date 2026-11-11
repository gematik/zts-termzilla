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
package de.gematik.zts.termzilla.common.metadatasearch.helper;

import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.DESCRIPTION_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.ID_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.NAME_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.PUBLISHER_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.SOURCE_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.STATUS_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.TITLE_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.URL_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.helper.TerminologyResourceSearchHelperTestDataHelper.VERSION_VALUE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_DATE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_DESCRIPTION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_EXT_EFFECTIVE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_NAME;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_PUBLISHER;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_SOURCE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_STATUS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_TITLE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for selected prepare methods in {@link TerminologyResourceSearchHelper}. */
@DisplayName("TerminologyResourceSearchHelper Tests")
class TerminologyResourceSearchHelperTest {

  @Nested
  @DisplayName("preparePublisherParam Tests")
  class PreparePublisherParamTests {

    @Test
    @DisplayName("Should add publisher ParamData when publisher is provided")
    void shouldAddPublisherParamDataWhenPublisherIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      StringParam publisher =
          TerminologyResourceSearchHelperTestDataHelper.stringParam(PUBLISHER_VALUE);
      searchParameter.setPublisherParam(publisher);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.preparePublisherParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_PUBLISHER);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.STRING_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(StringAndListParam.class);
                StringAndListParam andListParam = (StringAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(StringParam::getValue)
                    .containsExactly(PUBLISHER_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add publisher ParamData when publisher is missing")
    void shouldNotAddPublisherParamDataWhenPublisherIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.preparePublisherParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareDescriptionParam Tests")
  class PrepareDescriptionParamTests {

    @Test
    @DisplayName("Should add description ParamData when description is provided")
    void shouldAddDescriptionParamDataWhenDescriptionIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      StringParam description =
          TerminologyResourceSearchHelperTestDataHelper.stringParam(DESCRIPTION_VALUE);
      searchParameter.setDescriptionParam(description);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareDescriptionParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_DESCRIPTION);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.STRING_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(StringAndListParam.class);
                StringAndListParam andListParam = (StringAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(StringParam::getValue)
                    .containsExactly(DESCRIPTION_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add description ParamData when description is missing")
    void shouldNotAddDescriptionParamDataWhenDescriptionIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareDescriptionParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareTitleParam Tests")
  class PrepareTitleParamTests {

    @Test
    @DisplayName("Should add title ParamData when title is provided")
    void shouldAddTitleParamDataWhenTitleIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      StringParam title = TerminologyResourceSearchHelperTestDataHelper.stringParam(TITLE_VALUE);
      searchParameter.setTitleParam(title);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareTitleParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_TITLE);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.STRING_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(StringAndListParam.class);
                StringAndListParam andListParam = (StringAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(StringParam::getValue)
                    .containsExactly(TITLE_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add title ParamData when title is missing")
    void shouldNotAddTitleParamDataWhenTitleIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareTitleParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareEffectiveParam Tests")
  class PrepareEffectiveParamTests {

    @Test
    @DisplayName("Should add effective ParamData when effective is provided")
    void shouldAddEffectiveParamDataWhenEffectiveIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      DateParam effective = TerminologyResourceSearchHelperTestDataHelper.dateParam("2024-01-20");
      searchParameter.setEffectiveParam(effective);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareEffectiveParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_EXT_EFFECTIVE);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.PERIOD_DATE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(DateAndListParam.class);
                DateAndListParam andListParam = (DateAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(DateParam::getValueAsString)
                    .containsExactly(effective.getValueAsString());
              });
    }

    @Test
    @DisplayName("Should not add effective ParamData when effective is missing")
    void shouldNotAddEffectiveParamDataWhenEffectiveIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareEffectiveParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareDateParam Tests")
  class PrepareDateParamTests {

    @Test
    @DisplayName("Should add date ParamData when date is provided")
    void shouldAddDateParamDataWhenDateIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      DateParam date = TerminologyResourceSearchHelperTestDataHelper.dateParam("2024-02-15");
      searchParameter.setDateParam(date);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareDateParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_DATE);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.PERIOD_DATE_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(DateAndListParam.class);
                DateAndListParam andListParam = (DateAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(DateParam::getValueAsString)
                    .containsExactly(date.getValueAsString());
              });
    }

    @Test
    @DisplayName("Should not add date ParamData when date is missing")
    void shouldNotAddDateParamDataWhenDateIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareDateParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareNameParam Tests")
  class PrepareNameParamTests {

    @Test
    @DisplayName("Should add name ParamData when name is provided")
    void shouldAddNameParamDataWhenNameIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      StringParam name = TerminologyResourceSearchHelperTestDataHelper.stringParam(NAME_VALUE);
      searchParameter.setNameParam(name);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareNameParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_NAME);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.STRING_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(StringAndListParam.class);
                StringAndListParam andListParam = (StringAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(StringParam::getValue)
                    .containsExactly(NAME_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add name ParamData when name is missing")
    void shouldNotAddNameParamDataWhenNameIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareNameParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareStatusParam Tests")
  class PrepareStatusParamTests {

    @Test
    @DisplayName("Should add status ParamData when status is provided")
    void shouldAddStatusParamDataWhenStatusIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      TokenParam status = TerminologyResourceSearchHelperTestDataHelper.tokenParam(STATUS_VALUE);
      searchParameter.setStatusParam(status);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareStatusParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_STATUS);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.CODE_TOKEN_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(TokenAndListParam.class);
                TokenAndListParam andListParam = (TokenAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(TokenParam::getValue)
                    .containsExactly(STATUS_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add status ParamData when status is missing")
    void shouldNotAddStatusParamDataWhenStatusIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareStatusParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareVersionParam Tests")
  class PrepareVersionParamTests {

    @Test
    @DisplayName("Should add version ParamData when version is provided")
    void shouldAddVersionParamDataWhenVersionIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      TokenParam version = TerminologyResourceSearchHelperTestDataHelper.tokenParam(VERSION_VALUE);
      searchParameter.setVersionParam(version);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareVersionParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_VERSION);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.ID_TOKEN_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(TokenAndListParam.class);
                TokenAndListParam andListParam = (TokenAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(TokenParam::getValue)
                    .containsExactly(VERSION_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add version ParamData when version is missing")
    void shouldNotAddVersionParamDataWhenVersionIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareVersionParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareUrlParam Tests")
  class PrepareUrlParamTests {

    @Test
    @DisplayName("Should add url ParamData when url is provided")
    void shouldAddUrlParamDataWhenUrlIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      UriParam url = TerminologyResourceSearchHelperTestDataHelper.uriParam(URL_VALUE);
      searchParameter.setUrlParam(url);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareUrlParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_URL);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.URI_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(UriAndListParam.class);
                UriAndListParam andListParam = (UriAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(UriParam::getValue)
                    .containsExactly(URL_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add url ParamData when url is missing")
    void shouldNotAddUrlParamDataWhenUrlIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareUrlParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareSourceParam Tests")
  class PrepareSourceParamTests {

    @Test
    @DisplayName("Should add source ParamData when source is provided")
    void shouldAddSourceParamDataWhenSourceIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      UriParam source = TerminologyResourceSearchHelperTestDataHelper.uriParam(SOURCE_VALUE);
      searchParameter.setSourceParam(source);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareSourceParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_SOURCE);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.URI_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(UriAndListParam.class);
                UriAndListParam andListParam = (UriAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(UriParam::getValue)
                    .containsExactly(SOURCE_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add source ParamData when source is missing")
    void shouldNotAddSourceParamDataWhenSourceIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareSourceParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }

  @Nested
  @DisplayName("prepareIdParam Tests")
  class PrepareIdParamTests {

    @Test
    @DisplayName("Should add id ParamData when id is provided")
    void shouldAddIdParamDataWhenIdIsProvided() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      TokenParam id = TerminologyResourceSearchHelperTestDataHelper.tokenParam(ID_VALUE);
      searchParameter.setIdParam(id);
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareIdParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList)
          .hasSize(1)
          .first()
          .satisfies(
              paramData -> {
                assertThat(paramData.getParamName()).isEqualTo(NAME_ID);
                assertThat(paramData.getParamQueryType()).isEqualTo(ParamData.ID_TOKEN_PARAM);
                assertThat(paramData.getParam()).isInstanceOf(TokenAndListParam.class);
                TokenAndListParam andListParam = (TokenAndListParam) paramData.getParam();
                assertThat(
                        andListParam.getValuesAsQueryTokens().getFirst().getValuesAsQueryTokens())
                    .extracting(TokenParam::getValue)
                    .containsExactly(ID_VALUE);
              });
    }

    @Test
    @DisplayName("Should not add id ParamData when id is missing")
    void shouldNotAddIdParamDataWhenIdIsMissing() {
      // GIVEN
      VsSearchParameter searchParameter =
          TerminologyResourceSearchHelperTestDataHelper.emptySearchParameter();
      List<ParamData> searchParamList = new ArrayList<>();

      // WHEN
      TerminologyResourceSearchHelper.prepareIdParam(searchParameter, searchParamList);

      // THEN
      assertThat(searchParamList).isEmpty();
    }
  }
}
