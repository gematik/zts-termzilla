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
package de.gematik.zts.termzilla.testing.webui;

import static de.gematik.zts.termzilla.testing.common.asserts.CommonFindMatchesAsserts.assertCorrectResponseParameterStructure;
import static de.gematik.zts.termzilla.testing.common.asserts.CommonFindMatchesAsserts.assertExpectedResultsinFindMatchesResponse;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiCodeSystemSearchConceptIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("CodeSystem search by $find-matches")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| true| A00| NULL| A00",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| false| A00| NULL| A00,A00-A09,A00.0,A00.1,A00.9",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| true| NULL| Cholera| A00",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| false| NULL| Cholera| A00,A00.0,A00.1,A00.9,Z23.0,Z27.0",
        "bfarm.terminologien.icd10gm-2025.0.0| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| false| NULL| Cholera| A00,A00.0,A00.1,A00.9,Z23.0,Z27.0",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| false| NULL| Primär| P",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| false| P| NULL| P",
      })
  void testFindMatchesCodeSystemConcepts(
      String scope,
      String canonicalUrl,
      String version,
      Boolean exact,
      String code,
      String display,
      String expectedCodes) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create find-matches parameters
    Parameters findMatchesParams =
        prepareFindMatchesParams(canonicalUrl, version, exact, code, display);

    // WHEN
    Parameters findMatchesResult =
        client
            .operation()
            .onType("CodeSystem")
            .named("find-matches")
            .withParameters(findMatchesParams)
            .returnResourceType(Parameters.class)
            .execute();

    // THEN
    assertCorrectResponseParameterStructure(findMatchesResult);

    List<String> expectedCodeList = List.of(expectedCodes.split(","));
    assertExpectedResultsinFindMatchesResponse(
        findMatchesResult, canonicalUrl, version, expectedCodeList);
  }

  // ---------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareFindMatchesParams(
      String system, String version, Boolean exact, String code, String display) {
    Parameters expansionParams = new Parameters();
    expansionParams.addParameter().setName("system").setValue(new UriType(system));
    expansionParams.addParameter().setName("version").setValue(new StringType(version));
    if (exact != null) {
      expansionParams.addParameter().setName("exact").setValue(new BooleanType(exact));
    }
    if (code != null) {
      Parameters.ParametersParameterComponent codeParam =
          new Parameters.ParametersParameterComponent(new StringType("code"));
      codeParam.setValue(new CodeType("code"));

      Parameters.ParametersParameterComponent valueParam =
          new Parameters.ParametersParameterComponent(new StringType("value"));
      valueParam.setValue(new CodeType(code));

      Parameters.ParametersParameterComponent propertyParam =
          expansionParams.addParameter().setName("property");
      propertyParam.addPart(codeParam);
      propertyParam.addPart(valueParam);
    }
    if (display != null) {
      Parameters.ParametersParameterComponent codeParam =
          new Parameters.ParametersParameterComponent(new StringType("code"));
      codeParam.setValue(new CodeType("display"));

      Parameters.ParametersParameterComponent valueParam =
          new Parameters.ParametersParameterComponent(new StringType("value"));
      valueParam.setValue(new StringType(display));

      Parameters.ParametersParameterComponent propertyParam =
          expansionParams.addParameter().setName("property");
      propertyParam.addPart(codeParam);
      propertyParam.addPart(valueParam);
    }

    return expansionParams;
  }
}
