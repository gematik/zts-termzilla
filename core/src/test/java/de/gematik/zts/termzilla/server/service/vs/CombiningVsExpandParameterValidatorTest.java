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
package de.gematik.zts.termzilla.server.service.vs;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CombiningVsExpandParameterValidator}.
 *
 * <p>Tests verify the validation logic for ValueSet $expand operation parameters, including:
 *
 * <ul>
 *   <li>Required parameter validation (url or valueSet)
 *   <li>Mutually exclusive parameter validation (url and valueSet)
 *   <li>Unsupported parameter detection
 *   <li>System version parameter format and combination validation
 *   <li>ValueSet content validation
 * </ul>
 */
@DisplayName("CombiningVsExpandParameterValidator Tests")
class CombiningVsExpandParameterValidatorTest {

  private static final String TEST_URL = "http://example.com/ValueSet/test";
  private static final String TEST_SYSTEM = "http://example.com/CodeSystem/test";
  private static final String TEST_VERSION = "1.0.0";

  @Nested
  @DisplayName("validateParameters Tests")
  class ValidateParametersTests {

    @Test
    @DisplayName("Should pass validation when only URL is provided")
    void shouldPassValidationWhenOnlyUrlIsProvided() {
      // GIVEN
      VsExpandParameter parameters = urlWithVersion().build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should pass validation when only ValueSet is provided")
    void shouldPassValidationWhenOnlyValueSetIsProvided() {
      // GIVEN
      ValueSet valueSet = createValidValueSet();
      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should pass validation with URL and valid system-version parameters")
    void shouldPassValidationWithUrlAndValidSystemVersionParameters() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion()
              .systemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|" + TEST_VERSION)))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("failOnMissingUrlOrValueSetParameter Tests")
  class MissingUrlOrValueSetTests {

    @Test
    @DisplayName("Should throw exception when neither URL nor ValueSet is provided")
    void shouldThrowExceptionWhenNeitherUrlNorValueSetProvided() {
      // GIVEN
      VsExpandParameter parameters = VsExpandParameter.builder().build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.URL_PARAM)
          .hasMessageContaining(VsExpandParameter.VALUESET_PARAM)
          .hasMessageContaining("must be provided");
    }

    @Test
    @DisplayName("Should throw exception when URL is empty")
    void shouldThrowExceptionWhenUrlIsEmpty() {
      // GIVEN
      VsExpandParameter parameters = VsExpandParameter.builder().urlParam(new UriType()).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("must be provided");
    }

    @Test
    @DisplayName("Should throw exception when ValueSet is empty")
    void shouldThrowExceptionWhenValueSetIsEmpty() {
      // GIVEN
      VsExpandParameter parameters =
          VsExpandParameter.builder().valueSetParam(new ValueSet()).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("must be provided");
    }
  }

  @Nested
  @DisplayName("failOnUrlAndValueSetParameter Tests")
  class UrlAndValueSetTests {

    @Test
    @DisplayName("Should throw exception when both URL and ValueSet are provided")
    void shouldThrowExceptionWhenBothUrlAndValueSetProvided() {
      // GIVEN
      ValueSet valueSet = createValidValueSet();
      VsExpandParameter parameters = urlWithVersion().valueSetParam(valueSet).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.URL_PARAM)
          .hasMessageContaining(VsExpandParameter.VALUESET_PARAM)
          .hasMessageContaining("must not be provided at the same time");
    }
  }

  @Nested
  @DisplayName("ValueSet Selection Constraint Tests")
  class ValueSetSelectionConstraintTests {

    @Test
    @DisplayName("Should throw exception when valueSetVersion is provided without url")
    void shouldThrowExceptionWhenValueSetVersionWithoutUrl() {
      // GIVEN
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(createValidValueSet())
              .valueSetVersionParam(new org.hl7.fhir.r4.model.StringType(TEST_VERSION))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.VALUESET_VERSION_PARAM)
          .hasMessageContaining(VsExpandParameter.URL_PARAM);
    }

    @Test
    @DisplayName("Should throw exception when url is provided without valueSetVersion")
    void shouldThrowExceptionWhenUrlWithoutValueSetVersion() {
      // GIVEN
      VsExpandParameter parameters =
          VsExpandParameter.builder().urlParam(new UriType(TEST_URL)).build();

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  CombiningVsExpandParameterValidator.validateRequestBoundaryParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.URL_PARAM)
          .hasMessageContaining(VsExpandParameter.VALUESET_VERSION_PARAM);
    }

    @Test
    @DisplayName("Should throw exception when valueSetVersion accompanies inline valueSet")
    void shouldThrowExceptionWhenValueSetVersionWithInlineValueSet() {
      // GIVEN
      VsExpandParameter parameters =
          VsExpandParameter.builder()
              .valueSetParam(createValidValueSet())
              .valueSetVersionParam(new StringType(TEST_VERSION))
              .build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.VALUESET_VERSION_PARAM);
    }

    @Test
    @DisplayName("Should pass validation when url and valueSetVersion are provided together")
    void shouldPassValidationWhenUrlAndValueSetVersionProvidedTogether() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().valueSetVersionParam(new StringType(TEST_VERSION)).build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Compose Content Constraint Tests")
  class ComposeContentConstraintTests {

    @Test
    @DisplayName("Should throw exception when include has both concept and filter")
    void shouldThrowExceptionWhenIncludeHasBothConceptAndFilter() {
      // GIVEN
      ValueSet valueSet = new ValueSet();
      valueSet.setUrl(TEST_URL);
      ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.setSystem(TEST_SYSTEM);
      include.setVersion(TEST_VERSION);
      include.addConcept(new ValueSet.ConceptReferenceComponent().setCode("A01"));
      include.addFilter(
          new ValueSet.ConceptSetFilterComponent()
              .setProperty("code")
              .setOp(ValueSet.FilterOperator.EQUAL)
              .setValue("A01"));
      compose.addInclude(include);
      valueSet.setCompose(compose);

      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("concept")
          .hasMessageContaining("filter");
    }

    @Test
    @DisplayName("Should throw exception when exclude has both concept and filter")
    void shouldThrowExceptionWhenExcludeHasBothConceptAndFilter() {
      // GIVEN
      ValueSet valueSet = new ValueSet();
      valueSet.setUrl(TEST_URL);
      ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.setSystem(TEST_SYSTEM);
      include.setVersion(TEST_VERSION);
      compose.addInclude(include);
      ValueSet.ConceptSetComponent exclude = new ValueSet.ConceptSetComponent();
      exclude.setSystem(TEST_SYSTEM);
      exclude.setVersion(TEST_VERSION);
      exclude.addConcept(new ValueSet.ConceptReferenceComponent().setCode("A01"));
      exclude.addFilter(
          new ValueSet.ConceptSetFilterComponent()
              .setProperty("code")
              .setOp(ValueSet.FilterOperator.EQUAL)
              .setValue("A01"));
      compose.addExclude(exclude);
      valueSet.setCompose(compose);

      VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("concept")
          .hasMessageContaining("filter");
    }
  }

  @Nested
  @DisplayName("failOnUnsupportedParameters Tests")
  class UnsupportedParametersTests {

    @Test
    @DisplayName("Should throw exception when context parameter is provided")
    void shouldThrowExceptionWhenContextParameterProvided() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().contextParam(new UriType("http://context.example.com")).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.CONTEXT_PARAM)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should throw exception when contextDirection parameter is provided")
    void shouldThrowExceptionWhenContextDirectionParameterProvided() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().contextDirectionParam(new CodeType("incoming")).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.CONTEXT_DIRECTION_PARAM)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should throw exception when date parameter is provided")
    void shouldThrowExceptionWhenDateParameterProvided() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().dateParam(new DateTimeType("2024-01-01")).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.DATE_PARAM)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should throw exception when excludeNotForUI parameter is provided")
    void shouldThrowExceptionWhenExcludeNotForUIParameterProvided() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().excludeNotForUIParam(new BooleanType(true)).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.EXCLUDE_NOT_FOR_UI_PARAM)
          .hasMessageContaining("not supported");
    }

    @Test
    @DisplayName("Should throw exception when excludePostCoordinated parameter is provided")
    void shouldThrowExceptionWhenExcludePostcoordinatedParameterProvided() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().excludePostcoordinatedParam(new BooleanType(true)).build();

      // WHEN / THEN
      assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining(VsExpandParameter.EXCLUDE_POSTCOORDINATED_PARAM)
          .hasMessageContaining("not supported");
    }
  }

  @Nested
  @DisplayName("failOnInvalidSystemVersionParameterCombinations Tests")
  class SystemVersionParameterTests {

    @Nested
    @DisplayName("system-version Parameter Tests")
    class SystemVersionTests {

      @Test
      @DisplayName("Should pass validation with valid system-version parameter")
      void shouldPassValidationWithValidSystemVersionParameter() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .systemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|" + TEST_VERSION)))
                .build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName(
          "Should pass validation with multiple valid system-version parameters for different systems")
      void shouldPassValidationWithMultipleValidSystemVersionParametersForDifferentSystems() {
        // GIVEN
        List<CanonicalType> systemVersionParam =
            List.of(
                new CanonicalType("http://system1.com|1.0"),
                new CanonicalType("http://system2.com|2.0"));

        VsExpandParameter parameters =
            urlWithVersion().systemVersionParam(systemVersionParam).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @ParameterizedTest
      @ValueSource(strings = {"invalid", "system-without-version", "http://example.com"})
      @DisplayName("Should throw exception when system-version parameter has invalid format")
      void shouldThrowExceptionWhenSystemVersionParameterHasInvalidFormat(String invalidValue) {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion().systemVersionParam(List.of(new CanonicalType(invalidValue))).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'system-version' Parameter");
      }

      @Test
      @DisplayName("Should throw exception when system-version has duplicate system values")
      void shouldThrowExceptionWhenSystemVersionHasDuplicateSystemValues() {
        // GIVEN
        List<CanonicalType> systemVersionParam =
            List.of(
                new CanonicalType(TEST_SYSTEM + "|1.0"), new CanonicalType(TEST_SYSTEM + "|2.0"));

        VsExpandParameter parameters =
            urlWithVersion().systemVersionParam(systemVersionParam).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'system-version' Parameter")
            .hasMessageContaining("Duplicate values");
      }
    }

    @Nested
    @DisplayName("check-system-version Parameter Tests")
    class CheckSystemVersionTests {

      @Test
      @DisplayName("Should pass validation with valid check-system-version parameter")
      void shouldPassValidationWithValidCheckSystemVersionParameter() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .checkSystemVersionParam(
                    List.of(new CanonicalType(TEST_SYSTEM + "|" + TEST_VERSION)))
                .build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @ParameterizedTest
      @ValueSource(strings = {"invalid", "system-without-version", "http://example.com"})
      @DisplayName("Should throw exception when check-system-version parameter has invalid format")
      void shouldThrowExceptionWhenCheckSystemVersionParameterHasInvalidFormat(
          String invalidValue) {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .checkSystemVersionParam(List.of(new CanonicalType(invalidValue)))
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'check-system-version' Parameter");
      }

      @Test
      @DisplayName("Should throw exception when check-system-version has duplicate system values")
      void shouldThrowExceptionWhenCheckSystemVersionHasDuplicateSystemValues() {
        // GIVEN
        List<CanonicalType> checkSystemVersionParam =
            List.of(
                new CanonicalType(TEST_SYSTEM + "|1.0"), new CanonicalType(TEST_SYSTEM + "|2.0"));

        VsExpandParameter parameters =
            urlWithVersion().checkSystemVersionParam(checkSystemVersionParam).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'check-system-version' Parameter")
            .hasMessageContaining("Duplicate values");
      }
    }

    @Nested
    @DisplayName("force-system-version Parameter Tests")
    class ForceSystemVersionTests {

      @Test
      @DisplayName("Should pass validation with valid force-system-version parameter")
      void shouldPassValidationWithValidForceSystemVersionParameter() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .forceSystemVersionParam(
                    List.of(new CanonicalType(TEST_SYSTEM + "|" + TEST_VERSION)))
                .build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @ParameterizedTest
      @ValueSource(strings = {"invalid", "system-without-version", "http://example.com"})
      @DisplayName("Should throw exception when force-system-version parameter has invalid format")
      void shouldThrowExceptionWhenForceSystemVersionParameterHasInvalidFormat(
          String invalidValue) {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .forceSystemVersionParam(List.of(new CanonicalType(invalidValue)))
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'force-system-version' Parameter");
      }

      @Test
      @DisplayName("Should throw exception when force-system-version has duplicate system values")
      void shouldThrowExceptionWhenForceSystemVersionHasDuplicateSystemValues() {
        // GIVEN
        List<CanonicalType> forceSystemVersionParam =
            List.of(
                new CanonicalType(TEST_SYSTEM + "|1.0"), new CanonicalType(TEST_SYSTEM + "|2.0"));

        VsExpandParameter parameters =
            urlWithVersion().forceSystemVersionParam(forceSystemVersionParam).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("Invalid 'force-system-version' Parameter")
            .hasMessageContaining("Duplicate values");
      }
    }

    @Nested
    @DisplayName("Parameter Combination Tests")
    class ParameterCombinationTests {

      @Test
      @DisplayName(
          "Should throw exception when system-version and force-system-version have overlapping systems")
      void shouldThrowExceptionWhenSystemVersionAndForceSystemVersionHaveOverlappingSystems() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .systemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|1.0")))
                .forceSystemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|2.0")))
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "Invalid 'system-version/force-system-version' parameter combination");
      }

      @Test
      @DisplayName(
          "Should throw exception when system-version and check-system-version have overlapping systems")
      void shouldThrowExceptionWhenSystemVersionAndCheckSystemVersionHaveOverlappingSystems() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .systemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|1.0")))
                .checkSystemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|2.0")))
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "Invalid 'system-version/check-system-version' parameter combination");
      }

      @Test
      @DisplayName(
          "Should throw exception when check-system-version and force-system-version have overlapping systems")
      void shouldThrowExceptionWhenCheckSystemVersionAndForceSystemVersionHaveOverlappingSystems() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .checkSystemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|1.0")))
                .forceSystemVersionParam(List.of(new CanonicalType(TEST_SYSTEM + "|2.0")))
                .build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "Invalid 'check-system-version/force-system-version' parameter combination");
      }

      @Test
      @DisplayName(
          "Should pass validation when system-version, check-system-version and force-system-version have non-overlapping systems")
      void shouldPassValidationWhenAllVersionParametersHaveNonOverlappingSystems() {
        // GIVEN
        VsExpandParameter parameters =
            urlWithVersion()
                .systemVersionParam(List.of(new CanonicalType("http://system1.com|1.0")))
                .checkSystemVersionParam(List.of(new CanonicalType("http://system2.com|2.0")))
                .forceSystemVersionParam(List.of(new CanonicalType("http://system3.com|3.0")))
                .build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }
    }
  }

  @Nested
  @DisplayName("failOnUnsupportedValueSetContent Tests")
  class UnsupportedValueSetContentTests {

    @Nested
    @DisplayName("Compose Element Tests")
    class ComposeElementTests {

      @Test
      @DisplayName("Should throw exception when ValueSet has no compose element")
      void shouldThrowExceptionWhenValueSetHasNoComposeElement() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        // No compose element set

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("No 'compose' element is specified in the ValueSet")
            .hasMessageContaining("Nothing to expand");
      }

      @Test
      @DisplayName("Should pass validation when ValueSet has compose element with valid include")
      void shouldPassValidationWhenValueSetHasComposeElementWithValidInclude() {
        // GIVEN
        ValueSet valueSet = createValidValueSet();

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    @DisplayName("Locked Date Tests")
    class LockedDateTests {

      @Test
      @DisplayName("Should throw exception when ValueSet compose has locked-date element")
      void shouldThrowExceptionWhenValueSetComposeHasLockedDateElement() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        compose.setLockedDate(new java.util.Date());
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.setSystem(TEST_SYSTEM);
        compose.addInclude(include);
        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("locked-date")
            .hasMessageContaining("not supported");
      }

      @Test
      @DisplayName("Should pass validation when ValueSet compose has no locked-date element")
      void shouldPassValidationWhenValueSetComposeHasNoLockedDateElement() {
        // GIVEN
        ValueSet valueSet = createValidValueSet();

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    @DisplayName("Include Element Tests")
    class IncludeElementTests {

      @Test
      @DisplayName("Should pass validation when ValueSet include has system")
      void shouldPassValidationWhenValueSetIncludeHasSystem() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.setSystem(TEST_SYSTEM);
        compose.addInclude(include);
        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName("Should pass validation when ValueSet include has valueSet reference")
      void shouldPassValidationWhenValueSetIncludeHasValueSetReference() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.addValueSet("http://example.com/ValueSet/referenced");
        compose.addInclude(include);
        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName(
          "Should pass validation when ValueSet include has both system and valueSet reference")
      void shouldPassValidationWhenValueSetIncludeHasBothSystemAndValueSetReference() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.setSystem(TEST_SYSTEM);
        include.addValueSet("http://example.com/ValueSet/referenced");
        compose.addInclude(include);
        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .doesNotThrowAnyException();
      }

      @Test
      @DisplayName("Should throw exception when ValueSet include has neither system nor valueSet")
      void shouldThrowExceptionWhenValueSetIncludeHasNeitherSystemNorValueSet() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.setVersion("1.0.0");
        compose.addInclude(include);
        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "Neither 'system' nor 'valueSet' is specified in the ValueSet 'include'");
      }

      @Test
      @DisplayName(
          "Should throw exception when one of multiple includes has neither system nor valueSet")
      void shouldThrowExceptionWhenOneOfMultipleIncludesHasNeitherSystemNorValueSet() {
        // GIVEN
        ValueSet valueSet = new ValueSet();
        valueSet.setUrl(TEST_URL);
        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

        ValueSet.ConceptSetComponent validInclude = new ValueSet.ConceptSetComponent();
        validInclude.setSystem(TEST_SYSTEM);
        compose.addInclude(validInclude);

        ValueSet.ConceptSetComponent invalidInclude = new ValueSet.ConceptSetComponent();
        compose.addInclude(invalidInclude);

        valueSet.setCompose(compose);

        VsExpandParameter parameters = VsExpandParameter.builder().valueSetParam(valueSet).build();

        // WHEN / THEN
        assertThatThrownBy(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining(
                "Neither 'system' nor 'valueSet' is specified in the ValueSet 'include'");
      }
    }

    @Test
    @DisplayName("Should pass validation when URL is provided instead of ValueSet")
    void shouldPassValidationWhenUrlIsProvidedInsteadOfValueSet() {
      // GIVEN
      VsExpandParameter parameters = urlWithVersion().build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Newly Supported Parameter Tests (activeOnly, excludeNested, exclude-system)")
  class NewlySupportedParameterTests {

    @Test
    @DisplayName("Should accept activeOnly parameter (now supported)")
    void shouldAcceptActiveOnlyParameter() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().activeOnlyParam(new BooleanType(true)).build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept excludeNested parameter (now supported)")
    void shouldAcceptExcludeNestedParameter() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion().excludeNestedParam(new BooleanType(true)).build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept exclude-system parameter (now supported)")
    void shouldAcceptExcludeSystemParameter() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion()
              .excludeSystemParam(List.of(new CanonicalType(TEST_SYSTEM + "|" + TEST_VERSION)))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept useSupplement parameter (ZTS compatibility extension)")
    void shouldAcceptUseSupplementParameter() {
      // GIVEN
      VsExpandParameter parameters =
          urlWithVersion()
              .useSupplementParam(
                  List.of(new CanonicalType("http://example.org/fhir/CodeSystem/supplement|1.0.0")))
              .build();

      // WHEN / THEN
      assertThatCode(() -> CombiningVsExpandParameterValidator.validateParameters(parameters))
          .doesNotThrowAnyException();
    }
  }

  /**
   * Creates parameters with url and valueSetVersion (the minimum valid url-based selection).
   *
   * @return a builder pre-filled with url and valueSetVersion
   */
  private static VsExpandParameter.VsExpandParameterBuilder urlWithVersion() {
    return VsExpandParameter.builder()
        .urlParam(new UriType(TEST_URL))
        .valueSetVersionParam(new org.hl7.fhir.r4.model.StringType(TEST_VERSION));
  }

  /**
   * Creates a valid ValueSet with a system in the include.
   *
   * @return a valid ValueSet for testing
   */
  private ValueSet createValidValueSet() {
    ValueSet valueSet = new ValueSet();
    valueSet.setUrl(TEST_URL);
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(TEST_SYSTEM);
    compose.addInclude(include);
    valueSet.setCompose(compose);
    return valueSet;
  }
}
