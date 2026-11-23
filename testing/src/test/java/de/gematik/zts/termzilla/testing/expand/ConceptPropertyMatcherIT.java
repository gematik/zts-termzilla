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

import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for integer property value filters ({@code op = "="}) during {@code
 * ValueSet/$expand}, which are evaluated by {@code ConceptPropertyMatcher}.
 */
@DisplayName("ConceptPropertyMatcher Integration Tests")
public class ConceptPropertyMatcherIT extends BaseFhirIT {

  private static final String SCOPE = "fhir";
  private static final String CS_URL = "http://example.org/fhir/CodeSystem/PropFilter";
  private static final String INT_PROP = "intProp";
  private static final String DEC_PROP = "decProp";

  @Override
  protected void copyData(Path packageLoadTempDir) {
    try {
      PackageGeneration.builder()
          .packageName("bfarm.terminologien.propertyfiltertests")
          .packageVersion("2026.0.0")
          .build()
          .generatePackage(List.of(createCodeSystem()), packageLoadTempDir);
    } catch (PackageGenerationException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest(name = "[{index}] {0} should return an empty expansion")
  @ValueSource(strings = {"abc", "42.1"})
  @DisplayName("should return an empty expansion for non-integer filter values")
  void shouldReturnEmptyExpansionForNonIntegerFilterValues(String filterValue) {
    // GIVEN
    Parameters params = createExpandParameters(INT_PROP, filterValue);

    // WHEN
    ValueSet result = getFhirClient(SCOPE)
        .operation()
        .onType("ValueSet")
        .named("expand")
        .withParameters(params)
        .returnResourceType(ValueSet.class)
        .execute();

    // THEN
    assertThat(result.getExpansion().getContains())
        .as("Filter %s = %s should return an empty expansion", INT_PROP, filterValue)
        .isEmpty();
  }

  @ParameterizedTest(name = "[{index}] {0} should return an empty expansion")
  @ValueSource(strings = {"abc"})
  @DisplayName("should return an empty expansion for non-decimal filter values")
  void shouldReturnEmptyExpansionForNonDecimalFilterValues(String filterValue) {
    // GIVEN
    Parameters params = createExpandParameters(DEC_PROP, filterValue);

    // WHEN
    ValueSet result = getFhirClient(SCOPE)
        .operation()
        .onType("ValueSet")
        .named("expand")
        .withParameters(params)
        .returnResourceType(ValueSet.class)
        .execute();

    // THEN
    assertThat(result.getExpansion().getContains())
        .as("Filter %s = %s should return an empty expansion", DEC_PROP, filterValue)
        .isEmpty();
  }

  // Create a ValueSet with a filter on the given property, e.g. intProp = 42
  private static Parameters createExpandParameters(String property, String filterValue) {
    ValueSet vs = new ValueSet();
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    vs.getCompose()
        .addInclude()
        .setSystem(CS_URL)
        .addFilter()
        .setProperty(property)
        .setOp(ValueSet.FilterOperator.EQUAL)
        .setValue(filterValue);

    Parameters params = new Parameters();
    params.addParameter().setName("valueSet").setResource(vs);
    return params;
  }

  // Creates a small CodeSystem with one concept that has intProp = 42 and decProp = 42.1.
  private static CodeSystem createCodeSystem() {
    CodeSystem cs = new CodeSystem();
    cs.setId("PropFilter-2026");
    cs.setUrl(CS_URL);
    cs.setVersion("2026");
    cs.setName("PropFilter");
    cs.setTitle("PropFilter");
    cs.setLanguage("de-DE");
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setExperimental(false);
    cs.setDateElement(new DateTimeType("2026-01-01"));
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    cs.addProperty().setCode(INT_PROP).setType(CodeSystem.PropertyType.INTEGER);
    cs.addProperty().setCode(DEC_PROP).setType(CodeSystem.PropertyType.DECIMAL);

    CodeSystem.ConceptDefinitionComponent concept = cs.addConcept();
    concept.setCode("A1");
    concept.setDisplay("A1 Display");
    concept.addProperty().setCode(INT_PROP).setValue(new IntegerType(42));
    concept.addProperty().setCode(DEC_PROP).setValue(new DecimalType(42.1));
    return cs;
  }
}