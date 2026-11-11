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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import ca.uhn.fhir.rest.param.UriParam;

/** Test data helper for CombinedParameterQueryGenerator tests. */
final class CombinedParameterQueryGeneratorTestDataHelper {

  private CombinedParameterQueryGeneratorTestDataHelper() {
    // Utility class.
  }

  static ParamData createStringParamData(String paramName, String value) {
    StringAndListParam andListParam =
        new StringAndListParam().addAnd(new StringOrListParam().addOr(new StringParam(value)));

    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.STRING_PARAM)
        .param(andListParam)
        .build();
  }

  static ParamData createUriParamData(String paramName, String value) {
    UriAndListParam andListParam =
        new UriAndListParam().addAnd(new UriOrListParam().addOr(new UriParam(value)));

    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.URI_PARAM)
        .param(andListParam)
        .build();
  }

  static ParamData createIdTokenParamData(String paramName, String value) {
    TokenAndListParam andListParam =
        new TokenAndListParam().addAnd(new TokenOrListParam().addOr(new TokenParam(value)));

    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.ID_TOKEN_PARAM)
        .param(andListParam)
        .build();
  }

  static ParamData createReferenceParamData(String paramName, String value) {
    ReferenceAndListParam andListParam =
        new ReferenceAndListParam()
            .addAnd(new ReferenceOrListParam().addOr(new ReferenceParam(value)));

    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.REFERENCE_PARAM)
        .param(andListParam)
        .build();
  }
}
