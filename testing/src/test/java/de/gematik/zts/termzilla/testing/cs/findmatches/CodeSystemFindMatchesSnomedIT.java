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
package de.gematik.zts.termzilla.testing.cs.findmatches;

import static de.gematik.zts.termzilla.testing.common.asserts.CommonFindMatchesAsserts.assertCorrectResponseParameterStructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.common.constants.CanonicalUrl;
import de.gematik.zts.termzilla.testing.BaseFhirIT;
import de.gematik.zts.termzilla.testutils.packages.TestPackageHelper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/** Integration tests for SNOMED CT {@code CodeSystem/$find-matches} in the SNOMED module. */
@Slf4j
@DisplayName("CodeSystem $find-matches SNOMED Integration Tests")
class CodeSystemFindMatchesSnomedIT extends BaseFhirIT {

  private static final String SNOMED_ARTIFICIAL_RELEASE =
      "testresources/SnomedCT_Germany-EditionRelease_ARTIFICIAL_20251115T120000Z.zip";
  private static final String SCOPE = "fhir";
  private static final String SNOMED_VERSION =
      "http://snomed.info/sct/11000274103/version/20251115";
  private static final String SNOMED_DYSPNOE = "267036007";
  private static final String SNOMED_DYSPNOE_PARENT = "230145002";

  private Path snomedLoadTempDir;

  @Override
  protected void copyData(Path packageLoadTempDir) {
    TestPackageHelper.copyTestPackage(SNOMED_ARTIFICIAL_RELEASE, packageLoadTempDir);
    snomedLoadTempDir = packageLoadTempDir;
  }

  @Override
  protected void configureEnvironment(EnvironmentVariables env) {
    super.configureEnvironment(env);
    env.set("FHIRTERMS_START", "false");
    env.set("SNOMED_START", "true");
    env.set("SNOMED_RESTRICTEDMODE", "false");
    env.set(
        "SNOMED_RELEASEFILE",
        snomedLoadTempDir.toString()
            + "/"
            + SNOMED_ARTIFICIAL_RELEASE.substring(SNOMED_ARTIFICIAL_RELEASE.lastIndexOf('/') + 1));
  }

  @Nested
  @DisplayName("Successful matching")
  class SuccessfulMatchingTests {

    @Test
    @DisplayName("Returns the same concept for exact code matching")
    void shouldReturnSameConceptForExactCodeMatch() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              true,
              List.of(property("code", new CodeType(SNOMED_DYSPNOE))));

      // WHEN
      Parameters result = executeFindMatches(client, params);

      // THEN
      assertCorrectResponseParameterStructure(result);
      assertThat(extractMatchCodes(result)).containsExactly(SNOMED_DYSPNOE);
      assertThat(extractCodePart(result).getSystem()).isEqualTo(CanonicalUrl.SNOMEDCT);
      assertThat(extractCodePart(result).getVersion()).isEqualTo(SNOMED_VERSION);
    }

    @Test
    @DisplayName("Returns direct children when searching with parent property")
    void shouldReturnDirectChildrenForParentProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              true,
              List.of(property("parent", new CodeType(SNOMED_DYSPNOE_PARENT))));

      // WHEN
      Parameters result = executeFindMatches(client, params);

      // THEN
      assertCorrectResponseParameterStructure(result);
      assertThat(extractMatchCodes(result)).contains(SNOMED_DYSPNOE);
    }

    @Test
    @DisplayName("Returns direct parents when searching with child property")
    void shouldReturnDirectParentsForChildProperty() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              true,
              List.of(property("child", new CodeType(SNOMED_DYSPNOE))));

      // WHEN
      Parameters result = executeFindMatches(client, params);

      // THEN
      assertCorrectResponseParameterStructure(result);
      assertThat(extractMatchCodes(result)).contains(SNOMED_DYSPNOE_PARENT);
    }

    @Test
    @DisplayName("Returns matches for inexact display search")
    void shouldReturnMatchesForInexactDisplaySearch() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              false,
              List.of(property("display", new StringType("Dyspnoe"))));

      // WHEN
      Parameters result = executeFindMatches(client, params);

      // THEN
      assertCorrectResponseParameterStructure(result);
      assertThat(extractMatchCodes(result)).contains(SNOMED_DYSPNOE);
    }
  }

  @Nested
  @DisplayName("Validation and error handling")
  class ValidationAndErrorHandlingTests {

    @Test
    @DisplayName("Throws InvalidRequestException when exact parameter is missing")
    void shouldThrowWhenExactIsMissing() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = new Parameters();
      params.addParameter().setName("system").setValue(new UriType(CanonicalUrl.SNOMEDCT));
      params.addParameter().setName("version").setValue(new StringType(SNOMED_VERSION));
      params.addParameter(property("code", new CodeType(SNOMED_DYSPNOE)));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("exact");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when property parameter is missing")
    void shouldThrowWhenPropertyIsMissing() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params = new Parameters();
      params.addParameter().setName("system").setValue(new UriType(CanonicalUrl.SNOMEDCT));
      params.addParameter().setName("version").setValue(new StringType(SNOMED_VERSION));
      params.addParameter().setName("exact").setValue(new BooleanType(true));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("property");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when more than three property parameters are used")
    void shouldThrowWhenMoreThanThreePropertiesAreUsed() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              false,
              List.of(
                  property("code", new CodeType(SNOMED_DYSPNOE)),
                  property("display", new StringType("Dysp")),
                  property("child", new CodeType(SNOMED_DYSPNOE)),
                  property("parent", new CodeType(SNOMED_DYSPNOE_PARENT))));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("At most three 'property'");
    }

    @Test
    @DisplayName("Throws InvalidRequestException for unsupported property code")
    void shouldThrowForUnsupportedPropertyCode() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              false,
              List.of(property("designation", new StringType("Dyspnoe"))));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not supported by the referenced CodeSystem");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when display value has wrong FHIR type")
    void shouldThrowWhenDisplayValueHasWrongType() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              false,
              List.of(property("display", new CodeType("Dyspnoe"))));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("proper type would be: 'string'");
    }

    @Test
    @DisplayName(
        "Throws InvalidRequestException when display search text is shorter than 3 characters")
    void shouldThrowWhenDisplaySearchTextIsTooShort() {
      // GIVEN
      IGenericClient client = getFhirClient(SCOPE);
      Parameters params =
          prepareFindMatchesParams(
              CanonicalUrl.SNOMEDCT,
              SNOMED_VERSION,
              false,
              List.of(property("display", new StringType("Dy"))));

      // WHEN / THEN
      assertThatThrownBy(() -> executeFindMatches(client, params))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("minimum length of 3 characters");
    }
  }

  private Parameters executeFindMatches(IGenericClient client, Parameters params) {
    return client
        .operation()
        .onType("CodeSystem")
        .named("find-matches")
        .withParameters(params)
        .returnResourceType(Parameters.class)
        .execute();
  }

  private static @NonNull Parameters prepareFindMatchesParams(
      String system,
      String version,
      boolean exact,
      List<Parameters.ParametersParameterComponent> properties) {
    Parameters params = new Parameters();
    params.addParameter().setName("system").setValue(new UriType(system));
    params.addParameter().setName("version").setValue(new StringType(version));
    params.addParameter().setName("exact").setValue(new BooleanType(exact));
    for (Parameters.ParametersParameterComponent property : properties) {
      params.addParameter(property);
    }
    return params;
  }

  private static Parameters.ParametersParameterComponent property(String code, Type value) {
    Parameters.ParametersParameterComponent propertyParam =
        new Parameters.ParametersParameterComponent().setName("property");
    propertyParam.addPart().setName("code").setValue(new CodeType(code));
    propertyParam.addPart().setName("value").setValue(value);
    return propertyParam;
  }

  private static List<String> extractMatchCodes(Parameters result) {
    List<String> codes = new ArrayList<>();
    for (Parameters.ParametersParameterComponent matchParam : result.getParameter()) {
      codes.add(extractCodePart(matchParam).getCode());
    }
    return codes;
  }

  private static Coding extractCodePart(Parameters result) {
    return extractCodePart(result.getParameter().getFirst());
  }

  private static Coding extractCodePart(Parameters.ParametersParameterComponent matchParam) {
    return matchParam.getPart().stream()
        .filter(part -> "code".equals(part.getName()) && part.getValue() instanceof Coding)
        .map(part -> (Coding) part.getValue())
        .findFirst()
        .orElseThrow(() -> new AssertionError("No coding part named 'code' found in match"));
  }
}
