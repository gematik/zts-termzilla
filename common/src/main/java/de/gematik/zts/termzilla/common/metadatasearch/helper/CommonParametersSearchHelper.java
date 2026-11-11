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

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for adding FHIR search parameters to a search parameter list.
 *
 * <p>This class provides helper methods to convert FHIR parameter types into {@link ParamData}
 * objects and add them to a search parameter list. It supports transformation of reference and URI
 * parameters into their corresponding AND/OR list parameter structures.
 */
public class CommonParametersSearchHelper {

  /** Private constructor to prevent instantiation of this utility class. */
  private CommonParametersSearchHelper() {
    // Private constructor to prevent instantiation
  }

  /**
   * Adds a reference parameter to the search parameter list if present.
   *
   * <p>Transforms the provided {@link ReferenceParam} into a {@link ReferenceAndListParam}
   * structure and wraps it in a {@link ParamData} object before adding it to the list. If the
   * parameter is not present, no action is taken.
   *
   * @param paramOpt the optional reference parameter to add
   * @param paramName the name of the search parameter
   * @param searchParamList the list to which the parameter data should be added
   */
  public static void addReferenceParam(
      Optional<ReferenceParam> paramOpt, String paramName, List<ParamData> searchParamList) {

    paramOpt.ifPresent(
        param -> {
          ReferenceAndListParam andListParam =
              new ReferenceAndListParam().addAnd(new ReferenceOrListParam().addOr(param));

          searchParamList.add(
              ParamData.builder()
                  .paramName(paramName)
                  .paramQueryType(ParamData.CANONICAL_REFERENCE_PARAM)
                  .param(andListParam)
                  .build());
        });
  }

  /**
   * Adds a URI parameter to the search parameter list if present.
   *
   * <p>Transforms the provided {@link UriParam} into a {@link UriAndListParam} structure and wraps
   * it in a {@link ParamData} object before adding it to the list. If the parameter is not present,
   * no action is taken.
   *
   * @param paramOpt the optional URI parameter to add
   * @param paramName the name of the search parameter
   * @param searchParamList the list to which the parameter data should be added
   */
  public static void addUriParam(
      Optional<UriParam> paramOpt, String paramName, List<ParamData> searchParamList) {

    paramOpt.ifPresent(
        param -> {
          UriAndListParam andListParam =
              new UriAndListParam().addAnd(new UriOrListParam().addOr(param));

          searchParamList.add(
              ParamData.builder()
                  .paramName(paramName)
                  .paramQueryType(ParamData.CANONICAL_REFERENCE_PARAM)
                  .param(andListParam)
                  .build());
        });
  }
}
