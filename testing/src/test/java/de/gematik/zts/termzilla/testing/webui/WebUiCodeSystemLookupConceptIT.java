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

import static de.gematik.zts.termzilla.testing.common.asserts.CommonLookupAsserts.assertCorrectLookupResponseParameterStructure;
import static de.gematik.zts.termzilla.testing.common.asserts.CommonLookupAsserts.assertExpectedResultInLookupResponse;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiCodeSystemLookupConceptIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("CodeSystem concept lookup by $lookup")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| A00| child,parent,Para295| Cholera| ICD10GM| child,parent,Para295",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| A00.1| child,parent| Cholera durch Vibrio cholerae O:1, Biovar eltor| ICD10GM| parent",
        "bfarm.terminologien.icd10gm-2025.0.0| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| A00.1| child,parent,Para301| Cholera durch Vibrio cholerae O:1, Biovar eltor| ICD10GM| parent,Para301",
      })
  void testLookupConceptInCodeSystem(
      String scope,
      String canonicalUrl,
      String version,
      String code,
      String requestedProperties,
      String expectedDisplay,
      String expectedCodeSystemName,
      String expectedProperties) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create lookup parameters
    Parameters lookupParams = prepareLookupParams(canonicalUrl, version, code, requestedProperties);

    // WHEN
    Parameters lookupResponseGet =
        client
            .operation()
            .onType("CodeSystem")
            .named("lookup")
            .withParameters(lookupParams)
            .useHttpGet()
            .returnResourceType(Parameters.class)
            .execute();

    Parameters lookupResponsePost =
        client
            .operation()
            .onType("CodeSystem")
            .named("lookup")
            .withParameters(lookupParams)
            .returnResourceType(Parameters.class)
            .execute();

    // THEN
    List<String> expectedPropertiesList = List.of(expectedProperties.split(","));
    assertCorrectLookupResponseParameterStructure(lookupResponseGet);
    assertCorrectLookupResponseParameterStructure(lookupResponsePost);
    assertExpectedResultInLookupResponse(
        lookupResponseGet,
        canonicalUrl,
        version,
        expectedCodeSystemName,
        expectedDisplay,
        expectedPropertiesList);
    assertExpectedResultInLookupResponse(
        lookupResponsePost,
        canonicalUrl,
        version,
        expectedCodeSystemName,
        expectedDisplay,
        expectedPropertiesList);
  }

  // ---------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareLookupParams(
      String system, String version, String code, String requestedProperties) {
    Parameters lookupParams = new Parameters();
    lookupParams.addParameter().setName("system").setValue(new UriType(system));
    lookupParams.addParameter().setName("version").setValue(new StringType(version));
    lookupParams.addParameter().setName("code").setValue(new CodeType(code));
    if (requestedProperties != null) {
      List<String> requestedPropertyList = List.of(requestedProperties.split(","));
      for (String requestedProperty : requestedPropertyList) {
        lookupParams.addParameter().setName("property").setValue(new CodeType(requestedProperty));
      }
    }

    return lookupParams;
  }
}
