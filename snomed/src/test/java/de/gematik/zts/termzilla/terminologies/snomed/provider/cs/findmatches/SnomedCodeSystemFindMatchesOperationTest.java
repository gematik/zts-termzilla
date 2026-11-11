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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.findmatches;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_CASESINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants.SC_INITIALCHARACTERCASEINSENSITIVE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createConcept;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createDescription;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createLanguage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import java.util.HashMap;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SnomedCodeSystemFindMatchesOperation}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("SnomedCodeSystemFindMatchesOperation Tests")
class SnomedCodeSystemFindMatchesOperationTest {

  private static final String SCOPE = "snomed";
  private static final String CONCEPT_ID = "73211009";

  @Mock private SnomedCodeSystemService snomedCodeSystemService;
  @Mock private SnomedTerminologyManager snomedTerminologyManager;
  @Mock private Snomed snomed;
  @Mock private SnomedGraphRepresentation snomedGraphRepresentation;

  private SnomedCodeSystemFindMatchesOperation operation;

  @BeforeEach
  void setUp() throws Exception {
    SnomedReleaseVersion releaseVersion =
        new SnomedReleaseVersion("900000000000207008", "20240131", new HashMap<>());

    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setUrl(SnomedConstants.TERMINOLOGY_CANONICAL_NAME);
    codeSystem.setVersion(releaseVersion.getFhirVersion());

    when(snomedCodeSystemService.getSnomedTerminologyManager())
        .thenReturn(snomedTerminologyManager);
    when(snomedTerminologyManager.getSnomed()).thenReturn(snomed);
    lenient().when(snomedCodeSystemService.getSnomed()).thenReturn(snomed);
    lenient().when(snomed.getDefaultSnomedReleaseVersion()).thenReturn(releaseVersion);
    lenient().when(snomed.getSnomedGraph()).thenReturn(snomedGraphRepresentation);
    lenient()
        .when(
            snomedCodeSystemService.getCodeSystem(
                eq(SCOPE), eq(SnomedConstants.TERMINOLOGY_CANONICAL_NAME), eq((String) null)))
        .thenReturn(codeSystem);

    operation = new SnomedCodeSystemFindMatchesOperation(snomedCodeSystemService);
  }

  @Nested
  @DisplayName("UnknownConceptException handling")
  class UnknownConceptExceptionHandling {

    @Test
    @DisplayName("findMatches should convert unknown concept from child filter graph lookup")
    void findMatchesShouldConvertUnknownConceptFromChildFilterGraphLookup() throws Exception {
      // GIVEN
      when(snomedGraphRepresentation.getDirectParents(eq(CONCEPT_ID), any()))
          .thenThrow(new UnknownConceptException("unknown concept"));

      CsFindMatchesParameter parameter =
          createFindMatchesParameter(PROPERTY_NAME_CHILD, CONCEPT_ID);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.findMatches(SCOPE, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unknown concept");
    }

    @Test
    @DisplayName("findMatches should convert unknown concept from parent filter graph lookup")
    void findMatchesShouldConvertUnknownConceptFromParentFilterGraphLookup() throws Exception {
      // GIVEN
      when(snomedGraphRepresentation.getDirectChildren(eq(CONCEPT_ID), any()))
          .thenThrow(new UnknownConceptException("unknown concept"));

      CsFindMatchesParameter parameter =
          createFindMatchesParameter(PROPERTY_NAME_PARENT, CONCEPT_ID);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.findMatches(SCOPE, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unknown concept");
    }
  }

  @Nested
  @DisplayName("Restricted mode")
  class RestrictedMode {

    @BeforeEach
    void setUpRestrictedMode() {
      when(snomedTerminologyManager.isRestrictedMode()).thenReturn(true);
      operation = new SnomedCodeSystemFindMatchesOperation(snomedCodeSystemService);
    }

    @ParameterizedTest(name = "property ''{0}''")
    @ValueSource(strings = {PROPERTY_NAME_PARENT, PROPERTY_NAME_CHILD})
    @DisplayName("findMatches should reject relationship property filters")
    void findMatchesShouldRejectRelationshipPropertyFilters(String propertyCode) {
      // GIVEN
      CsFindMatchesParameter parameter = createFindMatchesParameter(propertyCode, CONCEPT_ID);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.findMatches(SCOPE, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("'" + propertyCode + "'")
          .hasMessageContaining("restricted mode");

      verifyNoInteractions(snomedGraphRepresentation);
    }

    @Test
    @DisplayName("findMatches should not offer relationship properties for unknown filters")
    void findMatchesShouldNotOfferRelationshipPropertiesForUnknownFilters() {
      // GIVEN
      CsFindMatchesParameter parameter = createFindMatchesParameter("unknownProperty", CONCEPT_ID);

      // WHEN / THEN
      assertThatThrownBy(() -> operation.findMatches(SCOPE, parameter))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("unknownProperty")
          .hasMessageNotContaining(PROPERTY_NAME_PARENT)
          .hasMessageNotContaining(PROPERTY_NAME_CHILD);
    }

    @Test
    @DisplayName("findMatches should still support code filters")
    void findMatchesShouldSupportCodeFilters() throws Exception {
      // GIVEN
      when(snomed.getSnomedConceptWithDescription(eq(CONCEPT_ID), any(), anyBoolean()))
          .thenReturn(createConceptWithPreferredSynonym());

      CsFindMatchesParameter parameter = createFindMatchesParameter("code", CONCEPT_ID);

      // WHEN
      CsFindMatchesResult result = operation.findMatches(SCOPE, parameter);

      // THEN
      assertThat(result.getMatchList())
          .singleElement()
          .satisfies(match -> assertThat(match.getCoding().getCode()).isEqualTo(CONCEPT_ID));
      verifyNoInteractions(snomedGraphRepresentation);
    }

    private SnomedConcept createConceptWithPreferredSynonym() {
      SnomedConcept concept = createConcept(CONCEPT_ID);
      concept.setActive(true);
      SnomedConceptDescription description =
          createDescription("en", SnomedConstants.SC_SYNONYM, "Diabetes mellitus");
      description
          .getLanguageList()
          .add(
              createLanguage(
                  SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID,
                  SnomedReleaseVersionConstants.EN_GB,
                  SnomedConstants.SC_PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);
      return concept;
    }
  }

  @Nested
  @DisplayName("matchWithCaseSignificance")
  class MatchWithCaseSignificance {

    @Test
    @DisplayName("should return true for case-insensitive matches")
    void shouldReturnTrueForCaseInsensitiveMatches() {
      // GIVEN
      String left = "Diabetes Mellitus";
      String right = "diabetes mellitus";

      // WHEN
      boolean result =
          SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
              left, right, SC_CASESINSENSITIVE);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when only the first character may differ by case")
    void shouldReturnTrueForInitialCharacterCaseInsensitiveMatch() {
      // GIVEN
      String left = "Diabetes";
      String right = "diabetes";

      // WHEN
      boolean result =
          SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
              left, right, SC_INITIALCHARACTERCASEINSENSITIVE);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName(
        "should return false when non-initial character differs for initial-character mode")
    void shouldReturnFalseWhenNonInitialCharacterDiffers() {
      // GIVEN
      String left = "Diabetes";
      String right = "DiabEtes";

      // WHEN
      boolean result =
          SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
              left, right, SC_INITIALCHARACTERCASEINSENSITIVE);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return true for exactly equal terms in case-sensitive mode")
    void shouldReturnTrueForCaseSensitiveExactMatch() {
      // GIVEN
      String left = "Diabetes";
      String right = "Diabetes";

      // WHEN
      boolean result =
          SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
              left, right, SC_CASESENSITIVE);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when input contains null")
    void shouldReturnFalseWhenInputContainsNull() {
      // GIVEN
      String right = "Diabetes";

      // WHEN
      boolean result =
          SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
              null, right, SC_CASESENSITIVE);

      // THEN
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown caseSignificanceId")
    void shouldThrowForUnknownCaseSignificanceId() {
      // GIVEN
      String unknownCaseSignificance = "UNKNOWN";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  SnomedCodeSystemFindMatchesOperation.matchWithCaseSignificance(
                      "a", "a", unknownCaseSignificance))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown caseSignificanceId");
    }
  }

  private static CsFindMatchesParameter createFindMatchesParameter(
      String propertyCode, String value) {
    Parameters.ParametersParameterComponent propertyParameter =
        new Parameters.ParametersParameterComponent();
    propertyParameter.setName(CsFindMatchesParameter.PROPERTY_PARAM);
    propertyParameter.addPart().setName("code").setValue(new CodeType(propertyCode));
    propertyParameter.addPart().setName("value").setValue(new CodeType(value));

    return CsFindMatchesParameter.builder()
        .systemParam(new UriType(SnomedConstants.TERMINOLOGY_CANONICAL_NAME))
        .exactParam(new BooleanType(false))
        .propertyParamList(List.of(propertyParameter))
        .build();
  }
}
