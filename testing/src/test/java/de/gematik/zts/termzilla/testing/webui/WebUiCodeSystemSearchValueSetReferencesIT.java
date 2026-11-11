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
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.CanonicalPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class WebUiCodeSystemSearchValueSetReferencesIT extends BaseFhirIT {

  @DisplayName("Search for ValueSets referencing CodeSystems")
  @ParameterizedTest(
      name =
          "Search for ValueSets in scope ''{0}'' referencing CodeSystem: ''{1}'' with version ''{2}''")
  @MethodSource("provideTestData")
  void testSearchForCodeSystemUsageInSupportedValueSets(
      String scope, String canonicalUrl, String version, List<CanonicalPair> expectedVsResults) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);
    String reference = canonicalUrl;
    if (version != null) {
      reference = reference.concat("|").concat(version);
    }

    // WHEN
    // Execute search with reference=...
    Bundle searchBundle =
        client
            .search()
            .byUrl("ValueSet?reference=" + reference)
            .elementsSubset("url", "version", "name", "title")
            .returnBundle(Bundle.class)
            .execute();

    // THEN
    assertThat(searchBundle).isNotNull();

    List<String> actualCanonicals =
        searchBundle.getEntry().stream()
            .filter(e -> e.getResource() instanceof ValueSet)
            .map(
                e -> {
                  ValueSet vs = (ValueSet) e.getResource();
                  return vs.getUrl() + "|" + vs.getVersion();
                })
            .toList();

    List<String> expectedCanonicals =
        expectedVsResults.stream().map(cp -> cp.getUrl() + "|" + cp.getVersion()).toList();

    assertThat(actualCanonicals).containsExactlyInAnyOrderElementsOf(expectedCanonicals);
  }

  private static Stream<org.junit.jupiter.params.provider.Arguments> provideTestData() {
    return Stream.of(
        // specific version
        Arguments.of(
            "fhir",
            "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
            "2025",
            List.of(
                new CanonicalPair("http://fhir.de/ValueSet/bfarm/icd-10-gm|2025"),
                new CanonicalPair(
                    "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-terminale-codes|2025"))),
        // no version
        Arguments.of(
            "fhir",
            "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
            null,
            List.of(
                new CanonicalPair("http://fhir.de/ValueSet/bfarm/icd-10-gm|2025"),
                new CanonicalPair(
                    "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-terminale-codes|2025"),
                new CanonicalPair("http://fhir.de/ValueSet/bfarm/icd-10-gm|2026"),
                new CanonicalPair(
                    "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-terminale-codes|2026"))),
        // unknown version
        Arguments.of("fhir", "http://fhir.de/CodeSystem/bfarm/icd-10-gm", "2025.0.0", List.of()),
        // unknown code system
        Arguments.of("fhir", "http://fhir.de/CodeSystem/bfarm/unknown", "2026", List.of()),
        // unknown code system no version
        Arguments.of("fhir", "http://fhir.de/CodeSystem/bfarm/unknown", null, List.of()));
  }
}
