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
package de.gematik.zts.termzilla.testing.common.asserts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;

public class CommonFindMatchesAsserts {

  public static void assertCorrectResponseParameterStructure(Parameters findMatchesResult) {

    assertThat(findMatchesResult).as("$find-matches result should not be null").isNotNull();

    // make sure, that if we have matches, they follow the correct structure
    for (Parameters.ParametersParameterComponent pc : findMatchesResult.getParameter()) {

      assertThat(pc.getName()).as("Parameter name must be named 'match'").isEqualTo("match");
      assertThat(pc.hasValue())
          .as("Parameter 'value' must be null. We expect a part here.")
          .isFalse();
      assertThat(pc.hasResource())
          .as("Parameter 'resource' must be null. We expect a part here.")
          .isFalse();
      assertThat(pc.hasPart()).as("Parameter 'part' must be present").isTrue();
      assertThat(pc.getPart())
          .as("Parameter 'part' must have at least one part")
          .hasSizeGreaterThanOrEqualTo(1);
      // assert that we have at least a part named code with a valueCoding
      assertThat(pc.getPart())
          .anyMatch(
              p -> p.getName().equals("code") && p.hasValue() && p.getValue() instanceof Coding);
    }
  }

  public static void assertExpectedResultsinFindMatchesResponse(
      Parameters findMatchesResult, String system, String version, List<String> expectedCodeList) {

    // make sure we have the exact number of expected results
    assertThat(findMatchesResult.getParameter())
        .as("Wrong number of returned results")
        .size()
        .isEqualTo(expectedCodeList.size());

    // make sure expected results are returned
    for (Parameters.ParametersParameterComponent pc : findMatchesResult.getParameter()) {
      assertThat(pc.getPart())
          .filteredOn(
              p -> p.getName().equals("code") && p.hasValue() && p.getValue() instanceof Coding)
          .matches(
              p -> ((Coding) p.getFirst().getValue()).getSystem().equals(system),
              "Wrong system in result match")
          .matches(
              p -> ((Coding) p.getFirst().getValue()).getVersion().equals(version),
              "Wrong version in result match")
          .matches(
              p -> expectedCodeList.contains(((Coding) p.getFirst().getValue()).getCode()),
              "Wrong code in result match");
    }
  }
}
