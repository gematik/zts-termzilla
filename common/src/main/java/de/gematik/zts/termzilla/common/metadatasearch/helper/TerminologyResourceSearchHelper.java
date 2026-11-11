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

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Hilfsklasse für die Suche nach ValueSet-Ressourcen /{scope}/ValueSet?param=value */
@Slf4j
public class TerminologyResourceSearchHelper {

  private TerminologyResourceSearchHelper() {
    // private constructor to prevent instantiation
  }

  /**
   * Bereitet den Suchparameter 'publisher' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'publisher' hinzugefügt.
   */
  public static void preparePublisherParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getPublisher()
        .ifPresent(
            publisher -> {
              StringAndListParam publisherParam = new StringAndListParam().addAnd(publisher);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_PUBLISHER)
                      .paramQueryType(ParamData.STRING_PARAM)
                      .param(publisherParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'description' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'description' hinzugefügt.
   */
  public static void prepareDescriptionParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getDescription()
        .ifPresent(
            description -> {
              StringAndListParam descriptionParam = new StringAndListParam().addAnd(description);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_DESCRIPTION)
                      .paramQueryType(ParamData.STRING_PARAM)
                      .param(descriptionParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'title' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'title' hinzugefügt.
   */
  public static void prepareTitleParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getTitle()
        .ifPresent(
            title -> {
              StringAndListParam titleParam = new StringAndListParam().addAnd(title);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_TITLE)
                      .paramQueryType(ParamData.STRING_PARAM)
                      .param(titleParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'name' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'name' hinzugefügt.
   */
  public static void prepareNameParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getName()
        .ifPresent(
            name -> {
              StringAndListParam nameParam = new StringAndListParam().addAnd(name);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_NAME)
                      .paramQueryType(ParamData.STRING_PARAM)
                      .param(nameParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'status' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'status' hinzugefügt.
   */
  public static void prepareStatusParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getStatus()
        .ifPresent(
            status -> {
              TokenAndListParam statusParam = new TokenAndListParam().addAnd(status);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_STATUS)
                      .paramQueryType(ParamData.CODE_TOKEN_PARAM)
                      .param(statusParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'effective' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'effective' hinzugefügt.
   */
  public static void prepareEffectiveParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getEffective()
        .ifPresent(
            effective -> {
              DateAndListParam effectiveParam =
                  new DateAndListParam().addAnd(new DateOrListParam().add(effective));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_EXT_EFFECTIVE)
                      .paramQueryType(ParamData.PERIOD_DATE_PARAM)
                      .param(effectiveParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'date' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'date' hinzugefügt.
   */
  public static void prepareDateParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getDate()
        .ifPresent(
            date -> {
              DateAndListParam dateParam =
                  new DateAndListParam().addAnd(new DateOrListParam().add(date));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_DATE)
                      .paramQueryType(ParamData.PERIOD_DATE_PARAM)
                      .param(dateParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'version' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'version' hinzugefügt.
   */
  public static void prepareVersionParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getVersion()
        .ifPresent(
            version -> {
              TokenAndListParam versionParam = new TokenAndListParam().addAnd(version);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_VERSION)
                      .paramQueryType(ParamData.ID_TOKEN_PARAM)
                      .param(versionParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter 'url' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'url' hinzugefügt.
   */
  public static void prepareUrlParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getUrl()
        .ifPresent(
            url -> {
              UriAndListParam urlParam =
                  new UriAndListParam().addAnd(new UriOrListParam().addOr(url));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_URL)
                      .paramQueryType(ParamData.URI_PARAM)
                      .param(urlParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter '_source' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     '_source' hinzugefügt.
   */
  public static void prepareSourceParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getSource()
        .ifPresent(
            source -> {
              UriAndListParam sourceParam =
                  new UriAndListParam().addAnd(new UriOrListParam().addOr(source));

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_SOURCE)
                      .paramQueryType(ParamData.URI_PARAM)
                      .param(sourceParam)
                      .build());
            });
  }

  /**
   * Bereitet den Suchparameter '_id' vor.
   *
   * @param searchParameter Gesamt-Suchparameter
   * @param searchParamList Liste mit ParamData-Objekten. In dieser Liste wird der Suchparameter
   *     'version' hinzugefügt.
   */
  public static void prepareIdParam(
      TerminologyResourceSearchParameter searchParameter, List<ParamData> searchParamList) {

    searchParameter
        .getId()
        .ifPresent(
            id -> {
              TokenAndListParam idParam = new TokenAndListParam().addAnd(id);

              searchParamList.add(
                  ParamData.builder()
                      .paramName(NAME_ID)
                      .paramQueryType(ParamData.ID_TOKEN_PARAM)
                      .param(idParam)
                      .build());
            });
  }
}
