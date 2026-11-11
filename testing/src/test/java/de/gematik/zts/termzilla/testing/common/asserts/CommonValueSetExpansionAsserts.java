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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.ValueSet;

public class CommonValueSetExpansionAsserts {

  public static void assertMinimumExpansionMetadataDoesExist(ValueSet expandedValueSet) {
    assertNotNull(expandedValueSet, "Expanded ValueSet should not be null");
    assertTrue(
        expandedValueSet.hasExpansion(), "Expanded ValueSet should have an expansion element");
    assertTrue(
        expandedValueSet.getExpansion().hasIdentifier(),
        "Expanded ValueSet expansion should have an identifier");
    assertTrue(
        expandedValueSet.getExpansion().hasTimestamp(),
        "Expanded ValueSet expansion must have a timestamp");
    assertTrue(
        expandedValueSet.getExpansion().hasTotal(),
        "Expanded ValueSet expansion should have a total count");
  }

  public static void assertExpansionContentMatchesUrlAndVersion(
      ValueSet expandedValueSet, String expectedUrl, String expectedVersion) {

    for (ValueSet.ValueSetExpansionContainsComponent cc :
        expandedValueSet.getExpansion().getContains()) {
      assertThat(cc.getSystem())
          .as("Concepts in Expanded ValueSet should match expected code system url")
          .isEqualTo(expectedUrl);

      assertThat(cc.getVersion())
          .as("Concepts in Expanded ValueSet should match expected code system version")
          .isEqualTo(expectedVersion);
    }
  }

  public static void assertExpansionContentMatchesExpectedCodeList(
      ValueSet expandedValueSet, List<String> expectedCodeList) {

    Set<String> expectedCodeSet = new HashSet<>(expectedCodeList);
    Set<String> expansionCodeSet = new HashSet<>();
    expandedValueSet.getExpansion().getContains().stream()
        .forEach(
            cc -> {
              if (cc.getCode() != null) {
                expansionCodeSet.add(cc.getCode());
              }
            });

    assertThat(expansionCodeSet)
        .as("Expanded ValueSet codes should match the expected codes")
        .isEqualTo(expectedCodeSet);
  }
}
