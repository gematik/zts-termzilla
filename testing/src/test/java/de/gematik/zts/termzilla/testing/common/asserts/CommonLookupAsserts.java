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
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;

public class CommonLookupAsserts {

  public static void assertCorrectLookupResponseParameterStructure(Parameters lookupResult) {
    assertThat(lookupResult).as("$lookup result should not be null").isNotNull();
    assertThat(lookupResult.getParameter())
        .as("no 'name' parameter found")
        .anyMatch(
            parameter ->
                parameter.getName().equals("name") && parameter.getValue() instanceof StringType);
    assertThat(lookupResult.getParameter())
        .as("no 'version' parameter found")
        .anyMatch(
            parameter ->
                parameter.getName().equals("version")
                    && parameter.getValue() instanceof StringType);
    assertThat(lookupResult.getParameter())
        .as("no 'display' parameter found")
        .anyMatch(
            parameter ->
                parameter.getName().equals("display")
                    && parameter.getValue() instanceof StringType);
  }

  public static void assertExpectedResultInLookupResponse(
      Parameters lookupResult,
      String system,
      String version,
      String codesystemName,
      String display,
      List<String> expectedPropertyList) {

    assertThat(lookupResult.getParameter())
        .as(
            "The returned 'name' parameter should match the expected value: %s for %s",
            codesystemName, system)
        .anyMatch(
            parameter ->
                parameter.getName().equals("name")
                    && parameter.getValue().toString().equals(codesystemName))
        .isNotEmpty();
    assertThat(lookupResult.getParameter())
        .as(
            "The returned 'version' parameter should match the expected value: %s for %s",
            version, system)
        .anyMatch(
            parameter ->
                parameter.getName().equals("version")
                    && parameter.getValue().toString().equals(version))
        .isNotEmpty();
    assertThat(lookupResult.getParameter())
        .as(
            "The returned 'display' parameter should match the expected value: %s for %s",
            display, system)
        .anyMatch(
            parameter ->
                parameter.getName().equals("display")
                    && parameter.getValue().toString().equals(display))
        .isNotEmpty();

    if (expectedPropertyList != null && !expectedPropertyList.isEmpty()) {

      for (String expectedProperty : expectedPropertyList) {

        assertThat(lookupResult.getParameter())
            .as(
                "The expected properties should be included in the returned $lookup response: %s for %s",
                expectedPropertyList, system)
            .anyMatch(parameter -> parameter.getName().equals("property"))
            .anyMatch(
                parameter ->
                    parameter.hasPart()
                        && parameter.getPart().getFirst().getName().equals("code")
                        && parameter
                            .getPart()
                            .getFirst()
                            .getValue()
                            .toString()
                            .equals(expectedProperty))
            .isNotEmpty();
      }
    }
  }
}
