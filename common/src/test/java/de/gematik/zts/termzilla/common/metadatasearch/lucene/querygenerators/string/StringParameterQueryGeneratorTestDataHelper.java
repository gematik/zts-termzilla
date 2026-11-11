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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.string;

import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;

/** Test data helper for {@link StringParameterQueryGeneratorTest}. */
final class StringParameterQueryGeneratorTestDataHelper {

  private StringParameterQueryGeneratorTestDataHelper() {
    // Utility class.
  }

  static StringParam createStringParam(String value) {
    return new StringParam(value);
  }

  static StringParam createExactParam(String value) {
    StringParam param = new StringParam(value);
    param.setExact(true);
    return param;
  }

  static StringParam createContainsParam(String value) {
    StringParam param = new StringParam(value);
    param.setContains(true);
    return param;
  }

  static StringParam createMissingParam(boolean missing) {
    StringParam param = new StringParam();
    param.setMissing(missing);
    return param;
  }

  static StringAndListParam createAndOrParam() {
    StringOrListParam firstOr = new StringOrListParam();
    firstOr.addOr(new StringParam("alpha"));
    firstOr.addOr(new StringParam("beta"));

    StringOrListParam secondOr = new StringOrListParam();
    secondOr.addOr(new StringParam("gamma"));

    StringAndListParam andListParam = new StringAndListParam();
    andListParam.addAnd(firstOr);
    andListParam.addAnd(secondOr);
    return andListParam;
  }

  static ParamData createParamData(String paramName, StringAndListParam stringAndListParam) {
    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.STRING_PARAM)
        .param(stringAndListParam)
        .build();
  }
}
