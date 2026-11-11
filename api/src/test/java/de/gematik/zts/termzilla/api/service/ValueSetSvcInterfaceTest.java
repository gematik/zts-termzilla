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
package de.gematik.zts.termzilla.api.service;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.ProcessingContext;
import de.gematik.zts.termzilla.api.parameters.VsExpandIncludeParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValueSetSvcInterfaceTest {
  @Test
  void validateInterfaceDefaults() {
    ValueSetSvcInterface valueSetSvcInterface = exampleImplementation();
    Assertions.assertTrue(valueSetSvcInterface.getSupportedValueSetCanonicalList().isEmpty());
    Assertions.assertTrue(valueSetSvcInterface.getValueSetList().isEmpty());
    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> valueSetSvcInterface.getValueSetList("Dummy", "https://example.com"));
    final var valueSetSupported =
        Assertions.assertDoesNotThrow(
            () ->
                valueSetSvcInterface.isValueSetSupported("Dummy", "https://example.com", "1.0.0"));
    Assertions.assertFalse(valueSetSupported);
    final var testIdType = new IdType("Test");
    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> valueSetSvcInterface.getValueSet("Dummy", testIdType));

    final var vsSearchParams =
        VsSearchParameter.builder().codeParam(new TokenParam("myCode")).build();
    final var resultSearch =
        Assertions.assertDoesNotThrow(
            () -> valueSetSvcInterface.searchValueSet("Test", vsSearchParams));
    Assertions.assertTrue(resultSearch.isEmpty());
    Assertions.assertThrows(
        NotImplementedException.class, () -> valueSetSvcInterface.expandValueSet("VsScope", null));
    Assertions.assertThrows(
        NotImplementedException.class,
        () -> valueSetSvcInterface.expandInclude(null, null, true, null, null, null, null));
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            valueSetSvcInterface.createExpansionContainsComponent(
                null, null, false, null, null, null, null));
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            valueSetSvcInterface.createExpansionContainsComponent(
                null, null, false, null, null, null, null));
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            valueSetSvcInterface.createExpansionContainsComponent(
                null, null, false, null, null, null, null, null));
  }

  @Test
  void expandIncludeWithParameterObjectDelegatesToExpandIncludeWithSingleParameters()
      throws Exception {
    VsExpandPartResult expected = new VsExpandPartResult();
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    ValueSetSvcInterface valueSetSvcInterface =
        new ValueSetSvcInterface() {
          @Override
          public VsExpandPartResult expandInclude(
              String scope,
              ValueSet.ConceptSetComponent include,
              boolean includeInactiveConcepts,
              String version,
              String checkVersion,
              String forceVersion,
              String filter) {
            return "scope".equals(scope)
                    && includeInactiveConcepts
                    && "1.0".equals(version)
                    && "abc".equals(filter)
                ? expected
                : null;
          }
        };

    VsExpandIncludeParameter parameter =
        VsExpandIncludeParameter.builder()
            .includeInactiveConcepts(true)
            .version("1.0")
            .filter("abc")
            .processingContext(ProcessingContext.stored())
            .build();

    VsExpandPartResult result = valueSetSvcInterface.expandInclude("scope", include, parameter);

    Assertions.assertSame(expected, result);
  }

  private ValueSetSvcInterface exampleImplementation() {
    return new ValueSetSvcInterface() {};
  }
}
