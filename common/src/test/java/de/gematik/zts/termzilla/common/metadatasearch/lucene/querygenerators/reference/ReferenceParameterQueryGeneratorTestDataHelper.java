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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.reference;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;

/** Test data helper for {@link ReferenceParameterQueryGeneratorTest}. */
final class ReferenceParameterQueryGeneratorTestDataHelper {

  private ReferenceParameterQueryGeneratorTestDataHelper() {
    // Utility class.
  }

  static ReferenceParam createIdentifierReferenceParam(String value) {
    return new ReferenceParam("identifier/" + value);
  }

  static ReferenceParam createMissingReferenceParam(boolean missing) {
    ReferenceParam referenceParam = new ReferenceParam();
    referenceParam.setMissing(missing);
    return referenceParam;
  }

  static ReferenceAndListParam createAndOrParam() {
    ReferenceOrListParam firstOr = new ReferenceOrListParam();
    firstOr.addOr(createIdentifierReferenceParam("sys|a"));
    firstOr.addOr(createIdentifierReferenceParam("sys|b"));

    ReferenceOrListParam secondOr = new ReferenceOrListParam();
    secondOr.addOr(createMissingReferenceParam(false));

    ReferenceAndListParam andListParam = new ReferenceAndListParam();
    andListParam.addAnd(firstOr);
    andListParam.addAnd(secondOr);
    return andListParam;
  }

  static ParamData createParamData(String paramName, ReferenceAndListParam referenceAndListParam) {
    return ParamData.builder()
        .paramName(paramName)
        .paramQueryType(ParamData.REFERENCE_PARAM)
        .param(referenceAndListParam)
        .build();
  }
}
