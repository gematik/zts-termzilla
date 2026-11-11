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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonValueSetExpansionAsserts;
import de.gematik.zts.termzilla.testutils.valueset.ValueSetGenerator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class WebUiCodeSystemPreviewContentIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName(
      "CodeSystem preview by expansion - root concepts in hierarchical CodeSystem (concepts with no parents)")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| NULL| I,II,III,IV,V,VI,VII,VIII,IX,X,XI,XII,XIII,XIV,XV,XVI,XVII,XVIII,XIX,XX,XXI,XXII",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| 1| I",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 1| 1| II",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| 10| I,II,III,IV,V,VI,VII,VIII,IX,X",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 10| 10| XI,XII,XIII,XIV,XV,XVI,XVII,XVIII,XIX,XX",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 20| 10| XXI,XXII",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| NULL| NULL|  9,K",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| 0| NULL|  9,K",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| NULL| 2|  9,K",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| 0| 2|  9,K",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| NULL| 1|  9",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| 0| 1|  9",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| 1| 1|  K",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| 1| NULL|  K",
      })
  void testExpandHierarchicalCodeSystemRootConcepts(
      String scope,
      String canonicalUrl,
      String version,
      Integer offset,
      Integer count,
      String expectedCodes) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create expansion parameters
    Parameters expansionParams =
        prepareExpansionParams(
            offset,
            count,
            ValueSetGenerator.generateParentExistsFalseValueSet(canonicalUrl, version));

    // WHEN
    ValueSet expandedValueSet =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class)
            .execute();

    // THEN
    CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);
    // Check total count
    List<String> expectedCodeList = List.of(expectedCodes.split(","));
    assertEquals(
        expectedCodeList.size(),
        expandedValueSet.getExpansion().getContains().size(),
        "Expanded ValueSet contains count should match expected count");
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesUrlAndVersion(
        expandedValueSet, canonicalUrl, version);
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesExpectedCodeList(
        expandedValueSet, expectedCodeList);
  }

  @ParameterizedTest
  @DisplayName("CodeSystem preview by expansion - child concepts in hierarchical CodeSystem")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| NULL| A00| A00.0,A00.1,A00.9",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| 2| A00| A00.0,A00.1",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 0| NULL| A00| A00.0,A00.1,A00.9",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 1| NULL| A00| A00.1,A00.9",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 1| 1| A00| A00.1",
        "fhir| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| NULL| A00-A09| A00,A01,A02,A03,A04,A05,A06,A07,A08,A09",
        "bfarm.terminologien.icd10gm-2025.0.0| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| NULL| NULL| A00| A00.0,A00.1,A00.9",
        "bfarm.terminologien.icd10gm-2025.0.0| http://fhir.de/CodeSystem/bfarm/icd-10-gm| 2025| 1| 1| A00| A00.1",
      })
  void testExpandHierarchicalCodeSystemChildConcepts(
      String scope,
      String canonicalUrl,
      String version,
      Integer offset,
      Integer count,
      String parentConcept,
      String expectedCodes) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create expansion parameters
    Parameters expansionParams =
        prepareExpansionParams(
            offset,
            count,
            ValueSetGenerator.generateParentIsConceptValueSet(
                canonicalUrl, version, parentConcept));

    // WHEN
    ValueSet expandedValueSet =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class)
            .execute();

    // THEN
    List<String> expectedCodeList = List.of(expectedCodes.split(","));

    CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);
    // Check total count
    assertEquals(
        expectedCodeList.size(),
        expandedValueSet.getExpansion().getContains().size(),
        "Expanded ValueSet total count should match expected count");
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesUrlAndVersion(
        expandedValueSet, canonicalUrl, version);
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesExpectedCodeList(
        expandedValueSet, expectedCodeList);
  }

  @ParameterizedTest
  @DisplayName("CodeSystem preview by expansion - all concepts")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| NULL| NULL| O,P,V,Z",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| NULL| 4| O,P,V,Z",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| NULL| 2| O,P",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| 0| NULL| O,P,V,Z",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| 2| NULL| V,Z",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| 1| 2| P,V",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| NULL| NULL| O,P,V,Z",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-para295| 2025| 1| 2| P,V",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025| NULL| NULL| 9,K",
      })
  void testExpandAllConceptsNoPaging(
      String scope,
      String canonicalUrl,
      String version,
      Integer offset,
      Integer count,
      String expectedCodes) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create expansion parameters
    Parameters expansionParams =
        prepareExpansionParams(
            offset, count, ValueSetGenerator.generateAllConceptsValueSet(canonicalUrl, version));

    // WHEN
    ValueSet expandedValueSet =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class)
            .execute();

    // THEN
    CommonValueSetExpansionAsserts.assertMinimumExpansionMetadataDoesExist(expandedValueSet);
    // Check total count
    List<String> expectedCodeList = List.of(expectedCodes.split(","));
    assertEquals(
        expectedCodeList.size(),
        expandedValueSet.getExpansion().getContains().size(),
        "Expanded ValueSet total count should match expected count");
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesUrlAndVersion(
        expandedValueSet, canonicalUrl, version);
    CommonValueSetExpansionAsserts.assertExpansionContentMatchesExpectedCodeList(
        expandedValueSet, expectedCodeList);
  }

  // ---------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareExpansionParams(
      Integer offset, Integer count, ValueSet vs) {
    Parameters expansionParams = new Parameters();
    expansionParams.addParameter().setName("valueSet").setResource(vs);
    if (offset != null) {
      expansionParams.addParameter().setName("offset").setValue(new IntegerType(offset));
    }
    if (count != null) {
      expansionParams.addParameter().setName("count").setValue(new IntegerType(count));
    }
    return expansionParams;
  }
}
