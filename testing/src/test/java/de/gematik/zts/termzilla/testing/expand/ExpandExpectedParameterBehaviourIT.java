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
package de.gematik.zts.termzilla.testing.expand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testing.common.asserts.CommonValueSetExpansionAsserts;
import de.gematik.zts.termzilla.testutils.valueset.ValueSetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
public class ExpandExpectedParameterBehaviourIT extends BaseFhirIT {

  public static final String SYSTEM_VERSION = "system-version";
  public static final String CHECK_SYSTEM_VERSION = "check-system-version";
  public static final String FORCE_SYSTEM_VERSION = "force-system-version";

  @ParameterizedTest
  @DisplayName("ValueSet expansion - expansion by VS-url and VS-version")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | NULL | 2026 | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2026 | NULL | NULL | 2026 | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | NULL | NULL | 2025 | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2026 | NULL | NULL | 2026 | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'system-version/force-system-version' parameter combination",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | true | Invalid 'system-version/check-system-version' parameter combination",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'check-system-version/force-system-version' parameter combination",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | XYZ | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2026 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | XYZ | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2025 | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2026 | NULL | NULL | NULL | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2026 | NULL | NULL | NULL | NULL | 2026 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "fhir| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2026 | NULL | NULL | 2026 | 2026 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | NULL | NULL | 2025 | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'system-version/force-system-version' parameter combination",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | true | Invalid 'system-version/check-system-version' parameter combination",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| NULL | NULL | 2025 | 2025 | NULL | NULL | NULL | NULL | true | Invalid 'check-system-version/force-system-version' parameter combination",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| XYZ | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | XYZ | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2025 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | 2026 | NULL | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2025 | NULL | NULL | NULL | NULL | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2025 | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | XYZ | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2025 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | 2026 | NULL | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | XYZ | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2025 | NULL | NULL | NULL | NULL | NULL | true | defined in the include/exclude does not match the version defined in the 'check-system-version' parameter",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | 2026 | NULL | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | XYZ | NULL | NULL | NULL | NULL | true | not supported in scope",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2025 | NULL | NULL | 2025 | 2025 | false | NULL",
        "bfarm.terminologien.icd10gm-2025.0.0| https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject| 2026 | NULL | NULL | 2026 | NULL | NULL | NULL | NULL | true | not supported in scope",
      })
  void testExpandPredefiniedValueSet(
      String scope,
      String referencedCsUrl,
      String referencedCsVersionInVS,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion,
      String expectedSystemVersion,
      String expectedCheckSystemVersion,
      String expectedForceSystemVersion,
      String expectedResultVersion,
      boolean expectException,
      String expectedErrorMessage) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    Parameters expansionParams =
        prepareExpansionParams(
            referencedCsUrl,
            referencedCsVersionInVS,
            systemVersion,
            checkSystemVersion,
            forceSystemVersion);

    // WHEN

    if (expectException) {

      var operation =
          client
              .operation()
              .onType("ValueSet")
              .named("expand")
              .withParameters(expansionParams)
              .returnResourceType(ValueSet.class);
      assertThatThrownBy(operation::execute)
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(expectedErrorMessage);

    } else {

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

      // make sure we have the expected CS Versions for all expandednconcepts
      assertThat(expandedValueSet.getExpansion().getContains())
          .as("Concepts in Expansion should have the correct CodeSystem version")
          .allSatisfy(
              concept -> {
                assertThat(concept.getSystem()).isEqualTo(referencedCsUrl);
                assertThat(concept.getVersion()).isEqualTo(expectedResultVersion);
              });

      // system-version
      if (expectedSystemVersion != null) {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'system-version' parameter expected, but not found")
            .filteredOn(parameter -> parameter.getName().equals(SYSTEM_VERSION))
            .isNotEmpty()
            .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
            .toString()
            .equals(referencedCsUrl + "|" + systemVersion);
      } else {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'system-version' parameter NOT expected, but found")
            .filteredOn(parameter -> parameter.getName().equals(SYSTEM_VERSION))
            .isEmpty();
      }

      // check-system-version
      if (expectedCheckSystemVersion != null) {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'check-system-version' parameter expected, but not found")
            .filteredOn(parameter -> parameter.getName().equals(CHECK_SYSTEM_VERSION))
            .isNotEmpty()
            .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
            .toString()
            .equals(referencedCsUrl + "|" + expectedCheckSystemVersion);
      } else {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'check-system-version' parameter NOT expected, but found")
            .filteredOn(parameter -> parameter.getName().equals(CHECK_SYSTEM_VERSION))
            .isEmpty();
      }

      // force-system-version
      if (expectedForceSystemVersion != null) {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'force-system-version' parameter expected, but not found")
            .filteredOn(parameter -> parameter.getName().equals(FORCE_SYSTEM_VERSION))
            .isNotEmpty()
            .extracting(ValueSet.ValueSetExpansionParameterComponent::getValueStringType)
            .toString()
            .equals(referencedCsUrl + "|" + expectedForceSystemVersion);
      } else {
        assertThat(expandedValueSet.getExpansion().getParameter())
            .as("'force-system-version' parameter NOT expected, but found")
            .filteredOn(parameter -> parameter.getName().equals(FORCE_SYSTEM_VERSION))
            .isEmpty();
      }
    }
  }

  // ---------------------------------------------------------------------------------------------

  private static @NonNull Parameters prepareExpansionParams(
      String referencedCsUrl,
      String referencedCsVersionInVS,
      String systemVersion,
      String checkSystemVersion,
      String forceSystemVersion) {
    Parameters expansionParams = new Parameters();
    expansionParams
        .addParameter()
        .setName("valueSet")
        .setResource(
            ValueSetGenerator.generateAllConceptsValueSet(
                referencedCsUrl, referencedCsVersionInVS));
    if (systemVersion != null) {
      expansionParams
          .addParameter()
          .setName(SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + systemVersion));
    }
    if (checkSystemVersion != null) {
      expansionParams
          .addParameter()
          .setName(CHECK_SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + checkSystemVersion));
    }
    if (forceSystemVersion != null) {
      expansionParams
          .addParameter()
          .setName(FORCE_SYSTEM_VERSION)
          .setValue(new CanonicalType(referencedCsUrl + "|" + forceSystemVersion));
    }

    return expansionParams;
  }
}
