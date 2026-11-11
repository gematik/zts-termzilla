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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.valueset.ValueSetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class ExpandFailOnUnsupportedParameterCombinationsIT extends BaseFhirIT {

  @ParameterizedTest
  @DisplayName("Test, if we fail on currently unsupported $expand parameters")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir | context | parameters are not supported yet",
        "fhir | contextDirection | parameters are not supported yet",
        "fhir | date | parameters are not supported yet",
        "fhir | excludeNotForUI | parameters are not supported yet",
        "fhir | excludePostCoordinated | parameters are not supported yet",
        "bfarm.terminologien.icd10gm-2025.0.0 | context | parameters are not supported yet",
        "bfarm.terminologien.icd10gm-2025.0.0 | contextDirection | parameters are not supported yet",
      })
  void testFailOnUnsupportedParameter(String scope, String paramName, String expectedMessage) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create expansion parameters
    Parameters expansionParams = new Parameters();
    expansionParams
        .addParameter()
        .setName("valueSet")
        .setResource(
            ValueSetGenerator.generateParentExistsFalseValueSet(
                "http://fhir.de/CodeSystem/bfarm/icd-10-gm", "2025"));

    if (paramName.equals("context")) {
      expansionParams
          .addParameter()
          .setName(paramName)
          .setValue(new UriType("http://example.com/context"));
    }

    if (paramName.equals("contextDirection")) {
      expansionParams.addParameter().setName(paramName).setValue(new CodeType("incoming"));
    }

    if (paramName.equals("date")) {
      expansionParams.addParameter().setName(paramName).setValue(new DateTimeType("2025-01-01"));
    }

    if (paramName.equals("excludeNotForUI")) {
      expansionParams.addParameter().setName(paramName).setValue(new BooleanType(true));
    }

    if (paramName.equals("excludePostCoordinated")) {
      expansionParams.addParameter().setName(paramName).setValue(new BooleanType(true));
    }

    // WHEN + THEN
    var operation =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class);
    assertThatThrownBy(operation::execute)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(expectedMessage);
  }

  @ParameterizedTest
  @DisplayName("Test, if we fail on ValueSet with lockedDate-Element in ValueSet.compose")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir",
        "bfarm.terminologien.icd10gm-2025.0.0",
      })
  void testFailOnUnsupportedValueSetContentLockedDate(String scope) {

    // GIVEN
    // Create FHIR client for the given path
    IGenericClient client = getFhirClient(scope);

    // Create expansion parameters
    Parameters expansionParams = new Parameters();
    expansionParams
        .addParameter()
        .setName("valueSet")
        .setResource(
            ValueSetGenerator.generateAllConceptsValueSetWithLockedDate(
                "http://fhir.de/CodeSystem/bfarm/icd-10-gm", "2025-12-12"));

    // WHEN + THEN
    var operation =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class);
    assertThatThrownBy(operation::execute)
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(
            "ValueSets with 'locked-date' elements are currently not supported for expansion");
  }

  @ParameterizedTest
  @DisplayName("Test, if newly supported $expand parameters are accepted")
  @CsvSource(
      delimiter = '|',
      nullValues = {"NULL"},
      value = {
        "fhir | activeOnly",
        "fhir | excludeNested",
        "fhir | exclude-system",
      })
  void testNewlySupportedParametersAreAccepted(String scope, String paramName) {

    // GIVEN
    IGenericClient client = getFhirClient(scope);

    Parameters expansionParams = new Parameters();
    expansionParams
        .addParameter()
        .setName("valueSet")
        .setResource(
            ValueSetGenerator.generateParentExistsFalseValueSet(
                "http://fhir.de/CodeSystem/bfarm/icd-10-gm", "2025"));

    if (paramName.equals("activeOnly")) {
      expansionParams.addParameter().setName(paramName).setValue(new BooleanType(true));
    }
    if (paramName.equals("excludeNested")) {
      expansionParams.addParameter().setName(paramName).setValue(new BooleanType(true));
    }
    if (paramName.equals("exclude-system")) {
      expansionParams
          .addParameter()
          .setName(paramName)
          .setValue(new CanonicalType("http://fhir.de/CodeSystem/bfarm/icd-10-gm|2025"));
    }

    // WHEN + THEN: the request must not fail with an "unsupported parameter" error
    var operation =
        client
            .operation()
            .onType("ValueSet")
            .named("expand")
            .withParameters(expansionParams)
            .returnResourceType(ValueSet.class);

    org.assertj.core.api.Assertions.assertThatCode(operation::execute)
        .as("Parameter '%s' should be accepted", paramName)
        .doesNotThrowAnyException();
  }
}
