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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonValueSetExpansionAsserts;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiValueSetPreviewContentIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("ValueSet preview by expansion - expansion by VS-url and VS-version")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-terminale-codes| 2025| NULL| 3| A00.0,A00.1,A00.9",
        "fhir| https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-sexreject| 2025| 1| NULL|  K",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-sexreject| 2025| NULL| NULL| 9,K",
      })
  void testExpandPredefiniedValueSet(
      String scope,
      String valueSetUrl,
      String valueSetVersion,
      Integer offset,
      Integer count,
      String expectedCodes) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    Parameters expansionParams =
        prepareExpansionParams(offset, count, valueSetUrl, valueSetVersion);

    // WHEN
    ValueSet expandedValueSetGet =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .useHttpGet()
            .returnResourceType(ValueSet.class)
            .execute();

    // WHEN
    ValueSet expandedValueSetPost =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class)
            .execute();

    // THEN
    CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSetGet);
    // Check total count
    List<String> expectedCodeList = List.of(expectedCodes.split(","));

    assertThat(expandedValueSetGet.getExpansion().getContains())
        .as("Expanded ValueSet contains count should match expected count")
        .hasSameSizeAs(expectedCodeList);
    assertThat(expandedValueSetPost.getExpansion().getContains())
        .as("Expanded ValueSet contains count should match expected count")
        .hasSameSizeAs(expectedCodeList);
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesExpectedCodeList(
        expandedValueSetGet, expectedCodeList);
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesExpectedCodeList(
        expandedValueSetPost, expectedCodeList);
  }

  // ---------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareExpansionParams(
      Integer offset, Integer count, String url, String valueSetVersion) {
    Parameters expansionParams = new Parameters();
    expansionParams.addParameter().setName("url").setValue(new UriType(url));
    expansionParams
        .addParameter()
        .setName("valueSetVersion")
        .setValue(new StringType(valueSetVersion));
    if (offset != null) {
      expansionParams.addParameter().setName("offset").setValue(new IntegerType(offset));
    }
    if (count != null) {
      expansionParams.addParameter().setName("count").setValue(new IntegerType(count));
    }
    return expansionParams;
  }
}
