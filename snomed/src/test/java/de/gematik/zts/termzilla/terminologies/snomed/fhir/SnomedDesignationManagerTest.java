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
package de.gematik.zts.termzilla.terminologies.snomed.fhir;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createConcept;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createDescription;
import static de.gematik.zts.termzilla.terminologies.snomed.SnomedDesignationTestDataBuilder.createLanguage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.common.Designation.DesignationSource;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedConstants;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConceptDescription;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersionConstants;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Unit tests for {@link SnomedDesignationManager}.
 *
 * <p>Verifies SNOMED-specific behavior: English fallback, CS designation selection logic (preferred
 * synonyms only), and VS display use filtering. Generic base class behavior (priority chain,
 * language matching, source preference, etc.) is covered by {@code DesignationManagerTest} in the
 * {@code termzilla-common} module.
 */
@DisplayName("SnomedDesignationManager Tests")
class SnomedDesignationManagerTest {

  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";

  private static final String SNOMED_SYSTEM = SnomedConstants.TERMINOLOGY_CANONICAL_NAME;
  private static final String FSN_TYPE_ID = SnomedConstants.SC_FULLYSPECIFIEDNAME;
  private static final String SYNONYM_TYPE_ID = SnomedConstants.SC_SYNONYM;
  private static final String PREFERRED = SnomedConstants.SC_PREFERRED;
  private static final String ACCEPTABLE = SnomedConstants.SC_ACCEPTABLE;

  private static final String REFSET_DE_DE = SnomedReleaseVersionConstants.DE_DE;
  private static final String REFSET_EN_GB = SnomedReleaseVersionConstants.EN_GB;
  private static final String REFSET_EN_US = SnomedReleaseVersionConstants.EN_US;
  private static final String MODULE_CORE = SnomedReleaseVersionConstants.SNOMED_CORE_MODULEID;
  private static final String MODULE_DE =
      SnomedReleaseVersionConstants.SNOMED_GERMANY_EDITION_MODULEID;

  private SnomedDesignationManager manager;

  @BeforeEach
  void setUp() {
    manager = new SnomedDesignationManager();
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  /** Creates a VS designation with HL7 display use coding. */
  private static SnomedDesignation createVsDisplayDesignation(String language, String value) {
    SnomedDesignation.SnomedDesignationBuilder<?, ?> builder =
        SnomedDesignation.builder()
            .source(DesignationSource.VS)
            .value(value)
            .use(new Coding(DESIGNATION_USAGE_SYSTEM, DISPLAY_USE_CODE, null));

    if (language != null) {
      builder.language(new CodeType(language));
    }

    return builder.build();
  }

  /** Creates a CS designation with SNOMED-native use coding and SNOMED-specific metadata. */
  private static SnomedDesignation createCsDesignation(
      String language, String value, String typeId, String role) {
    return createCsDesignation(language, value, typeId, role, null);
  }

  /**
   * Creates a CS designation with SNOMED-native use coding, SNOMED-specific metadata, and context.
   */
  private static SnomedDesignation createCsDesignation(
      String language, String value, String typeId, String role, String context) {
    SnomedDesignation.SnomedDesignationBuilder<?, ?> builder =
        SnomedDesignation.builder()
            .source(DesignationSource.CS)
            .value(value)
            .use(new Coding(SNOMED_SYSTEM, typeId, null))
            .type(typeId)
            .role(role)
            .context(context);

    if (language != null) {
      builder.language(new CodeType(language));
    }

    return builder.build();
  }

  private static LanguageSelectionParameter lspWithDisplayLanguage(String language) {
    return LanguageSelectionParameter.builder()
        .displayLanguageParameter(LanguageParameter.parse(language))
        .build();
  }

  private static String languageOf(SnomedDesignation d) {
    return d.getLanguage() != null ? d.getLanguage().getValue() : null;
  }

  private static ValueSet.ConceptReferenceComponent createVsConceptReference(
      String display, String language) {
    ValueSet.ConceptReferenceComponent ref = new ValueSet.ConceptReferenceComponent();
    if (display != null) {
      ref.setDisplay(display);
    }
    ref.setUserData(USERDATA_LANGUAGE, language);
    return ref;
  }

  private static ValueSet.ConceptReferenceDesignationComponent createVsDesignationComponent(
      String language, String value) {
    ValueSet.ConceptReferenceDesignationComponent d =
        new ValueSet.ConceptReferenceDesignationComponent();
    if (language != null) {
      d.setLanguage(language);
    }
    d.setValue(value);
    return d;
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("getDisplay() - English Fallback Priority Tests")
  class EnglishFallbackPriorityTests {

    @Test
    @DisplayName("Should fall back to English as last resort when all other parameters miss")
    void shouldFallBackToEnglishAsLastResort() {
      // GIVEN
      SnomedDesignation englishDesignation =
          createCsDesignation("en-GB", "Diabetes mellitus", SYNONYM_TYPE_ID, PREFERRED);
      manager.addDesignation(englishDesignation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(LanguageParameter.parse("fr-FR"))
              .systemDefaultLanguage(LanguageParameter.parse("ja-JP"))
              .vsLanguage(LanguageParameter.parse("es-ES"))
              .build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to English as the ultimate last resort")
          .isEqualTo(englishDesignation);
    }

    @Test
    @DisplayName("Should fall back to English when all parameters are null")
    void shouldFallBackToEnglishWhenAllParametersNull() {
      // GIVEN
      SnomedDesignation englishDesignation =
          createCsDesignation("en-US", "Diabetes mellitus", SYNONYM_TYPE_ID, PREFERRED);
      manager.addDesignation(englishDesignation);
      LanguageSelectionParameter lsp = LanguageSelectionParameter.builder().build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to English when no selection parameters are set")
          .isEqualTo(englishDesignation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - displayLanguage English Strict Tests")
  class DisplayLanguageEnglishStrictTests {

    @Test
    @DisplayName(
        "Should not fall through to English fallback when displayLanguage is set but misses")
    void shouldNotFallThroughToEnglishFallbackWhenDisplayLanguageMisses() {
      // GIVEN
      manager.addDesignation(
          createCsDesignation("en-GB", "Diabetes mellitus", SYNONYM_TYPE_ID, PREFERRED));

      LanguageSelectionParameter lsp = lspWithDisplayLanguage("fr-FR");

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("displayLanguage is strict - must not fall through to English fallback")
          .isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - CS Designation Selection Tests")
  class CsDesignationSelectionTests {

    @Test
    @DisplayName("Should only select preferred synonyms from CS designations")
    void shouldOnlySelectPreferredSynonyms() {
      // GIVEN
      SnomedDesignation preferredSynonym =
          createCsDesignation("de-DE", "Zuckerkrankheit", SYNONYM_TYPE_ID, PREFERRED);
      SnomedDesignation acceptableSynonym =
          createCsDesignation("de-DE", "Diabetes", SYNONYM_TYPE_ID, ACCEPTABLE);
      SnomedDesignation preferredFsn =
          createCsDesignation("de-DE", "Diabetes mellitus (Störung)", FSN_TYPE_ID, PREFERRED);
      manager.addDesignation(acceptableSynonym);
      manager.addDesignation(preferredFsn);
      manager.addDesignation(preferredSynonym);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Should select preferred synonym, not acceptable synonym or FSN")
          .isEqualTo(preferredSynonym);
    }

    @Test
    @DisplayName("Should not select FSN even when it is preferred")
    void shouldNotSelectFsnEvenWhenPreferred() {
      // GIVEN
      SnomedDesignation preferredFsn =
          createCsDesignation("de-DE", "Diabetes mellitus (Störung)", FSN_TYPE_ID, PREFERRED);
      manager.addDesignation(preferredFsn);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("FSN should never be selected as display even when preferred").isNull();
    }

    @Test
    @DisplayName("Should not select acceptable synonyms")
    void shouldNotSelectAcceptableSynonyms() {
      // GIVEN
      SnomedDesignation acceptableSynonym =
          createCsDesignation("de-DE", "Diabetes", SYNONYM_TYPE_ID, ACCEPTABLE);
      manager.addDesignation(acceptableSynonym);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("Acceptable synonyms should not be selected as display").isNull();
    }

    @Test
    @DisplayName("Should not select CS designation with null type")
    void shouldNotSelectCsDesignationWithNullType() {
      // GIVEN
      SnomedDesignation noType =
          SnomedDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("No type")
              .use(new Coding(SNOMED_SYSTEM, SYNONYM_TYPE_ID, null))
              .role(PREFERRED)
              .build();
      manager.addDesignation(noType);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("CS designation without type should not be selected").isNull();
    }

    @Test
    @DisplayName("Should not select CS designation with null role")
    void shouldNotSelectCsDesignationWithNullRole() {
      // GIVEN
      SnomedDesignation noRole =
          SnomedDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("No role")
              .use(new Coding(SNOMED_SYSTEM, SYNONYM_TYPE_ID, null))
              .type(SYNONYM_TYPE_ID)
              .build();
      manager.addDesignation(noRole);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("CS designation without role should not be selected").isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - VS Display Use Filter Tests")
  class VsDisplayUseFilterTests {

    @Test
    @DisplayName("Should select VS designation with correct display use coding")
    void shouldSelectVsDesignationWithDisplayUse() {
      // GIVEN
      SnomedDesignation vsDesignation = createVsDisplayDesignation("de-DE", "VS Display");
      manager.addDesignation(vsDesignation);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("VS designation with display use should be selected")
          .isEqualTo(vsDesignation);
    }

    @Test
    @DisplayName("Should ignore VS designations without display use coding")
    void shouldIgnoreVsDesignationsWithoutDisplayUse() {
      // GIVEN
      SnomedDesignation vsNoUse =
          SnomedDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("VS without use")
              .build();
      manager.addDesignation(vsNoUse);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation without display use should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore VS designations with wrong use system")
    void shouldIgnoreVsDesignationsWithWrongUseSystem() {
      // GIVEN
      SnomedDesignation wrongSystem =
          SnomedDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("Wrong system")
              .use(new Coding("http://other-system.org", DISPLAY_USE_CODE, null))
              .build();
      manager.addDesignation(wrongSystem);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation with wrong use system should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore VS designations with wrong use code")
    void shouldIgnoreVsDesignationsWithWrongUseCode() {
      // GIVEN
      SnomedDesignation wrongCode =
          SnomedDesignation.builder()
              .source(DesignationSource.VS)
              .language(new CodeType("de-DE"))
              .value("Wrong code")
              .use(new Coding(DESIGNATION_USAGE_SYSTEM, "definition", null))
              .build();
      manager.addDesignation(wrongCode);

      // WHEN
      SnomedDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("VS designation with wrong use code should be ignored").isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - English Fallback Tests")
  class EnglishFallbackTests {

    @Test
    @DisplayName("Should fall back to English CS preferred synonym when VS language misses")
    void shouldFallBackToEnglishPreferredSynonym() {
      // GIVEN
      SnomedDesignation englishDesignation =
          createCsDesignation("en-GB", "Diabetes mellitus", SYNONYM_TYPE_ID, PREFERRED);
      manager.addDesignation(englishDesignation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .systemDefaultLanguage(LanguageParameter.parse("ja-JP"))
              .vsLanguage(LanguageParameter.parse("es-ES"))
              .build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to English preferred synonym")
          .isEqualTo(englishDesignation);
    }

    @Test
    @DisplayName("Should prefer VS English display over CS English synonym in English fallback")
    void shouldPreferVsEnglishOverCsEnglishInFallback() {
      // GIVEN
      SnomedDesignation csEnglish =
          createCsDesignation("en-US", "CS English", SYNONYM_TYPE_ID, PREFERRED);
      SnomedDesignation vsEnglish = createVsDisplayDesignation("en-US", "VS English");
      manager.addDesignation(csEnglish);
      manager.addDesignation(vsEnglish);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder().vsLanguage(LanguageParameter.parse("ja-JP")).build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should prefer VS English display in English fallback")
          .isEqualTo(vsEnglish);
    }

    @Test
    @DisplayName("Should return null when no English designation exists and all else fails")
    void shouldReturnNullWhenNoEnglishDesignationExists() {
      // GIVEN
      SnomedDesignation japaneseDesignation =
          createCsDesignation("ja-JP", "糖尿病", SYNONYM_TYPE_ID, PREFERRED);
      manager.addDesignation(japaneseDesignation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder().vsLanguage(LanguageParameter.parse("fr-FR")).build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should return null when no English designation exists for last resort fallback")
          .isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - Combination Language Scenario Tests")
  class CombinationLanguageScenarioTests {

    @ParameterizedTest
    @DisplayName("Should return expected display for CS preferred synonyms and VS display")
    @CsvSource(
        nullValues = "NULL",
        delimiter = '|',
        value = {
          // systemDefault | acceptLanguage | displayLanguage | vsLanguage | expected
          // --- displayLanguage strict tests ---
          "de-DE | NULL | de-DE | NULL | CS preferred synonym (de-DE)",
          "de-DE | NULL | de-DE | de-DE | VS display (de-DE)",
          "de-DE | NULL | de-DE | en-US | CS preferred synonym (de-DE)",
          "de-DE | NULL | de-AT | en-US | NULL",
          "de-DE | NULL | en-US | NULL | CS preferred synonym (en-US)",
          "de-DE | NULL | fr-FR | NULL | NULL",
          // --- acceptLanguage tests ---
          "de-DE | de-DE | NULL | NULL | CS preferred synonym (de-DE)",
          "de-DE | de-DE | NULL | de-DE | VS display (de-DE)",
          "de-DE | en-US | NULL | NULL | CS preferred synonym (en-US)",
          "de-DE | en-US | NULL | en-US | VS display (en-US)",
          "de-DE | fr-FR | NULL | NULL | CS preferred synonym (de-DE)",
          // --- systemDefault tests ---
          "de-DE | NULL | NULL | NULL | CS preferred synonym (de-DE)",
          "de-DE | NULL | NULL | de-DE | VS display (de-DE)",
          "de-DE | NULL | NULL | en-US | CS preferred synonym (de-DE)",
          // --- vsLanguage tests ---
          "NULL | NULL | NULL | de-DE | VS display (de-DE)",
          "NULL | NULL | NULL | en-US | VS display (en-US)",
          "NULL | NULL | NULL | fr-FR | VS display (fr-FR)",
          // --- base language expansion ---
          "de | NULL | NULL | NULL | CS preferred synonym (de-DE)",
          "en | NULL | NULL | NULL | CS preferred synonym (en-US)",
          "NULL | de | NULL | NULL | CS preferred synonym (de-DE)",
          "NULL | en | NULL | NULL | CS preferred synonym (en-US)",
          // --- English fallback ---
          "NULL | NULL | NULL | NULL | CS preferred synonym (en-US)",
          "ja-JP | NULL | NULL | NULL | CS preferred synonym (en-US)",
          "ja-JP | NULL | NULL | es-ES | VS display (es-ES)",
        })
    void shouldReturnExpectedDisplay(
        String systemDefaultLanguage,
        String acceptLanguageHeader,
        String displayLanguageParameter,
        String vsLanguage,
        String expectedTerm) {
      // GIVEN
      manager.addDesignation(
          createCsDesignation("de-DE", "CS preferred synonym (de-DE)", SYNONYM_TYPE_ID, PREFERRED));
      manager.addDesignation(
          createCsDesignation(
              "de-DE", "CS acceptable synonym (de-DE)", SYNONYM_TYPE_ID, ACCEPTABLE));
      manager.addDesignation(
          createCsDesignation("de-DE", "CS preferred FSN (de-DE)", FSN_TYPE_ID, PREFERRED));
      manager.addDesignation(
          createCsDesignation("en-US", "CS preferred synonym (en-US)", SYNONYM_TYPE_ID, PREFERRED));
      manager.addDesignation(
          createCsDesignation(
              "en-US", "CS acceptable synonym (en-US)", SYNONYM_TYPE_ID, ACCEPTABLE));
      manager.addDesignation(
          createCsDesignation("en-US", "CS preferred FSN (en-US)", FSN_TYPE_ID, PREFERRED));

      if (vsLanguage != null) {
        manager.addDesignation(
            createVsDisplayDesignation(vsLanguage, "VS display (" + vsLanguage + ")"));
      }

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .systemDefaultLanguage(
                  systemDefaultLanguage != null
                      ? LanguageParameter.parse(systemDefaultLanguage)
                      : null)
              .acceptLanguageHeader(
                  acceptLanguageHeader != null
                      ? LanguageParameter.parse(acceptLanguageHeader)
                      : null)
              .displayLanguageParameter(
                  displayLanguageParameter != null
                      ? LanguageParameter.parse(displayLanguageParameter)
                      : null)
              .vsLanguage(vsLanguage != null ? LanguageParameter.parse(vsLanguage) : null)
              .build();

      // WHEN
      SnomedDesignation result = manager.getDisplay(lsp);

      // THEN
      if (expectedTerm == null) {
        assertThat(result).as("Expected no display designation to be found").isNull();
      } else {
        assertThat(result).as("Expected a designation to be found").isNotNull();
        assertThat(result.getValue())
            .as("Should have display: " + expectedTerm)
            .isEqualTo(expectedTerm);
      }
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Custom Criteria (context/role) Tests")
  class SelectDesignationsCustomCriteriaTests {

    private static final String GB_LANG_REFSET = "900000000000508004";
    private static final String US_LANG_REFSET = "900000000000509007";

    @Test
    @DisplayName("Should select designations by context when plain code is provided")
    void shouldSelectByContextWithPlainCode() {
      // GIVEN
      SnomedDesignation gbDesignation =
          createCsDesignation("en-GB", "GB term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      SnomedDesignation usDesignation =
          createCsDesignation("en-US", "US term", SYNONYM_TYPE_ID, PREFERRED, US_LANG_REFSET);
      manager.addDesignation(gbDesignation);
      manager.addDesignation(usDesignation);

      // WHEN
      Set<SnomedDesignation> result = manager.selectDesignations(List.of(GB_LANG_REFSET));

      // THEN
      assertThat(result)
          .as("Should select only designation with matching context")
          .containsExactly(gbDesignation);
    }

    @Test
    @DisplayName("Should select designations by role when plain code is provided")
    void shouldSelectByRoleWithPlainCode() {
      // GIVEN
      SnomedDesignation preferred =
          createCsDesignation("de-DE", "Bevorzugt", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      SnomedDesignation acceptable =
          createCsDesignation("de-DE", "Akzeptabel", SYNONYM_TYPE_ID, ACCEPTABLE, GB_LANG_REFSET);
      manager.addDesignation(preferred);
      manager.addDesignation(acceptable);

      // WHEN
      Set<SnomedDesignation> result = manager.selectDesignations(List.of(ACCEPTABLE));

      // THEN
      assertThat(result)
          .as("Should select only designation with matching role")
          .containsExactly(acceptable);
    }

    @Test
    @DisplayName("Should select designations by context when SNOMED system|code is provided")
    void shouldSelectByContextWithSnomedSystemPipe() {
      // GIVEN
      SnomedDesignation gbDesignation =
          createCsDesignation("en-GB", "GB term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      SnomedDesignation usDesignation =
          createCsDesignation("en-US", "US term", SYNONYM_TYPE_ID, PREFERRED, US_LANG_REFSET);
      manager.addDesignation(gbDesignation);
      manager.addDesignation(usDesignation);

      // WHEN
      Set<SnomedDesignation> result =
          manager.selectDesignations(List.of(SNOMED_SYSTEM + "|" + US_LANG_REFSET));

      // THEN
      assertThat(result)
          .as("Should select designation by context via system|code")
          .containsExactly(usDesignation);
    }

    @Test
    @DisplayName("Should select designations by role when SNOMED system|code is provided")
    void shouldSelectByRoleWithSnomedSystemPipe() {
      // GIVEN
      SnomedDesignation preferred =
          createCsDesignation("de-DE", "Bevorzugt", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      SnomedDesignation acceptable =
          createCsDesignation("de-DE", "Akzeptabel", SYNONYM_TYPE_ID, ACCEPTABLE, GB_LANG_REFSET);
      manager.addDesignation(preferred);
      manager.addDesignation(acceptable);

      // WHEN
      Set<SnomedDesignation> result =
          manager.selectDesignations(List.of(SNOMED_SYSTEM + "|" + PREFERRED));

      // THEN
      assertThat(result)
          .as("Should select designation by role via system|code")
          .containsExactly(preferred);
    }

    @Test
    @DisplayName("Should not select by custom criteria when system is not SNOMED")
    void shouldNotSelectWhenSystemIsNotSnomed() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation("en-GB", "GB term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      manager.addDesignation(designation);

      // WHEN
      Set<SnomedDesignation> result =
          manager.selectDesignations(List.of("http://other-system.org|" + GB_LANG_REFSET));

      // THEN
      assertThat(result)
          .as("Non-SNOMED system should not trigger custom criteria matching")
          .isEmpty();
    }

    @Test
    @DisplayName("Should select designations matching both context and role for same code")
    void shouldSelectByBothContextAndRole() {
      // GIVEN — context matches on one, role matches on another
      SnomedDesignation contextMatch =
          createCsDesignation("en-GB", "Context match", SYNONYM_TYPE_ID, ACCEPTABLE, PREFERRED);
      SnomedDesignation roleMatch =
          createCsDesignation("en-US", "Role match", SYNONYM_TYPE_ID, PREFERRED, US_LANG_REFSET);
      SnomedDesignation noMatch =
          createCsDesignation("de-DE", "No match", SYNONYM_TYPE_ID, ACCEPTABLE, GB_LANG_REFSET);
      manager.addDesignation(contextMatch);
      manager.addDesignation(roleMatch);
      manager.addDesignation(noMatch);

      // WHEN — PREFERRED matches contextMatch.context and roleMatch.role
      Set<SnomedDesignation> result = manager.selectDesignations(List.of(PREFERRED));

      // THEN
      assertThat(result)
          .as("Should match designations where either context or role equals the code")
          .containsExactlyInAnyOrder(contextMatch, roleMatch);
    }

    @Test
    @DisplayName("Should return empty when code matches neither context nor role")
    void shouldReturnEmptyWhenCodeMatchesNothing() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation("en-GB", "Some term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);
      manager.addDesignation(designation);

      // WHEN
      Set<SnomedDesignation> result = manager.selectDesignations(List.of("999999999999999999"));

      // THEN
      assertThat(result).as("Unmatched code should yield empty result").isEmpty();
    }

    @Test
    @DisplayName("Should not select by custom criteria when SNOMED system|code has blank code part")
    void shouldIgnoreSnomedSystemPipeWithBlankCode() {
      // GIVEN — designation without use coding so only the custom hook could match it
      SnomedDesignation designation =
          SnomedDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("en-GB"))
              .value("Some term")
              .context(GB_LANG_REFSET)
              .role(PREFERRED)
              .build();
      manager.addDesignation(designation);

      // WHEN
      Set<SnomedDesignation> result = manager.selectDesignations(List.of(SNOMED_SYSTEM + "| "));

      // THEN
      assertThat(result)
          .as("Blank code after SNOMED system pipe should not trigger custom criteria matching")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("toDesignationComponent() Tests")
  class ToDesignationComponentTests {

    private static final String DESIGNATION_USE_CONTEXT_URL =
        "http://snomed.info/fhir/StructureDefinition/designation-use-context";
    private static final String GB_LANG_REFSET = "900000000000508004";

    @Test
    @DisplayName("Should set language, use, and value from designation")
    void shouldSetLanguageUseAndValue() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation(
              "de-DE", "Zuckerkrankheit", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);

      // WHEN
      ValueSet.ConceptReferenceDesignationComponent result =
          SnomedDesignationManager.toDesignationComponent(designation, false);

      // THEN
      assertThat(result.getLanguage())
          .as("Language should be copied from designation")
          .isEqualTo("de-DE");
      assertThat(result.getUse())
          .as("Use coding should be copied from designation")
          .isEqualTo(designation.getUse());
    }

    @Test
    @DisplayName("Should handle null language gracefully")
    void shouldHandleNullLanguage() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation(null, "No language", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);

      // WHEN
      ValueSet.ConceptReferenceDesignationComponent result =
          SnomedDesignationManager.toDesignationComponent(designation, false);

      // THEN
      assertThat(result.getLanguage())
          .as("Language should be null when designation has no language")
          .isNull();
    }

    @Test
    @DisplayName("Should not add extension when includeContextExtension is false")
    void shouldNotAddExtensionWhenFlagIsFalse() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation("en-GB", "Some term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);

      // WHEN
      ValueSet.ConceptReferenceDesignationComponent result =
          SnomedDesignationManager.toDesignationComponent(designation, false);

      // THEN
      assertThat(result.getExtension())
          .as("No extensions should be added when flag is false")
          .isEmpty();
    }

    @Test
    @DisplayName(
        "Should add designation-use-context extension when includeContextExtension is true")
    void shouldAddDesignationUseContextExtension() {
      // GIVEN
      SnomedDesignation designation =
          createCsDesignation("en-GB", "Some term", SYNONYM_TYPE_ID, PREFERRED, GB_LANG_REFSET);

      // WHEN
      ValueSet.ConceptReferenceDesignationComponent result =
          SnomedDesignationManager.toDesignationComponent(designation, true);

      // THEN
      assertThat(result.getExtension())
          .as("Should have exactly one top-level extension")
          .hasSize(1);

      Extension contextExtension = result.getExtensionByUrl(DESIGNATION_USE_CONTEXT_URL);
      assertThat(contextExtension)
          .as("Extension with designation-use-context URL should be present")
          .isNotNull();
    }

    @Test
    @DisplayName("Should include context, role, and type as nested extensions with SNOMED codings")
    void shouldIncludeContextRoleAndTypeAsNestedExtensions() {
      // GIVEN
      String contextId = GB_LANG_REFSET;
      String roleId = PREFERRED;
      String typeId = SYNONYM_TYPE_ID;
      SnomedDesignation designation =
          createCsDesignation("en-GB", "Some term", typeId, roleId, contextId);

      // WHEN
      ValueSet.ConceptReferenceDesignationComponent result =
          SnomedDesignationManager.toDesignationComponent(designation, true);

      // THEN
      Extension contextExtension = result.getExtensionByUrl(DESIGNATION_USE_CONTEXT_URL);
      List<Extension> nestedExtensions = contextExtension.getExtension();

      assertThat(nestedExtensions)
          .as("Should have exactly three nested extensions (context, role, type)")
          .hasSize(3);

      // Verify context extension
      Extension contextNested = contextExtension.getExtensionByUrl("context");
      assertThat(contextNested).as("Context nested extension should be present").isNotNull();
      Coding contextCoding = (Coding) contextNested.getValue();
      assertThat(contextCoding.getSystem()).isEqualTo(SNOMED_SYSTEM);
      assertThat(contextCoding.getCode()).isEqualTo(contextId);

      // Verify role extension
      Extension roleNested = contextExtension.getExtensionByUrl("role");
      assertThat(roleNested).as("Role nested extension should be present").isNotNull();
      Coding roleCoding = (Coding) roleNested.getValue();
      assertThat(roleCoding.getSystem()).isEqualTo(SNOMED_SYSTEM);
      assertThat(roleCoding.getCode()).isEqualTo(roleId);

      // Verify type extension
      Extension typeNested = contextExtension.getExtensionByUrl("type");
      assertThat(typeNested).as("Type nested extension should be present").isNotNull();
      Coding typeCoding = (Coding) typeNested.getValue();
      assertThat(typeCoding.getSystem()).isEqualTo(SNOMED_SYSTEM);
      assertThat(typeCoding.getCode()).isEqualTo(typeId);
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromSnomedConcept() Tests")
  class SnomedDescriptionCollectionTests {

    @Test
    @DisplayName("Should create one designation per language refset membership")
    void shouldCreateOneDesignationPerLanguageRefsetMembership() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", FSN_TYPE_ID, "Diabetes mellitus (disorder)");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_US, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(SnomedDesignationManagerTest::languageOf, SnomedDesignation::getValue)
          .containsExactly(
              tuple("en-GB", "Diabetes mellitus (disorder)"),
              tuple("en-US", "Diabetes mellitus (disorder)"));
    }

    @Test
    @DisplayName("Should resolve language tag from refset ID")
    void shouldResolveLanguageTagFromRefsetId() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("de", SYNONYM_TYPE_ID, "Zuckerkrankheit");
      description.getLanguageList().add(createLanguage(MODULE_DE, REFSET_DE_DE, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      SnomedDesignation designation = result.getFirst();
      assertThat(languageOf(designation))
          .as("RefSet DE_DE should resolve to 'de-DE'")
          .isEqualTo("de-DE");
    }

    @Test
    @DisplayName("Should fall back to description languageCode when refset lookup returns null")
    void shouldFallBackToDescriptionLanguageCode() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description = createDescription("en", SYNONYM_TYPE_ID, "Some term");
      String unknownRefsetId = "999999999999";
      String unknownModuleId = "999999999998";
      description
          .getLanguageList()
          .add(createLanguage(unknownModuleId, unknownRefsetId, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst()))
          .as("Should fall back to description languageCode 'en'")
          .isEqualTo("en");
    }

    @Test
    @DisplayName(
        "Should fall back to description languageCode when refset language does not match description language prefix")
    void shouldFallBackWhenRefsetLanguageDoesNotMatchDescriptionLanguage() {
      // GIVEN - description says "en" but refset resolves to "de-DE" (inconsistent data)
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "English term");
      description.getLanguageList().add(createLanguage(MODULE_DE, REFSET_DE_DE, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst()))
          .as(
              "Should fall back to description languageCode 'en' because 'de-DE' does not start with 'en'")
          .isEqualTo("en");
    }

    @Test
    @DisplayName(
        "Should use refset language when it is a more specific variant of description language")
    void shouldUseRefsetLanguageWhenConsistentWithDescriptionLanguage() {
      // GIVEN - description says "en" and refset resolves to "en-GB" (consistent refinement)
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "English term");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst()))
          .as("Should use refset language 'en-GB' because it starts with description language 'en'")
          .isEqualTo("en-GB");
    }

    @Test
    @DisplayName(
        "Should use description languageCode when refset language is from a different language family")
    void shouldUseDescriptionLanguageWhenRefsetFromDifferentFamily() {
      // GIVEN - description says "de" but module resolves to "en" (cross-language inconsistency)
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("de", SYNONYM_TYPE_ID, "Deutscher Begriff");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_US, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst()))
          .as(
              "Should fall back to description languageCode 'de' because 'en-US' does not start with 'de'")
          .isEqualTo("de");
    }

    @Test
    @DisplayName("Should set SNOMED use coding with description typeId")
    void shouldSetSnomedUseCodingWithTypeId() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription fsnDescription =
          createDescription("en", FSN_TYPE_ID, "Diabetes mellitus (disorder)");
      fsnDescription.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      SnomedConceptDescription synonymDescription =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      synonymDescription
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      concept.getSnomedConceptDescriptionList().add(fsnDescription);
      concept.getSnomedConceptDescriptionList().add(synonymDescription);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(d -> d.getUse().getSystem(), d -> d.getUse().getCode())
          .containsExactly(
              tuple(SNOMED_SYSTEM, FSN_TYPE_ID), tuple(SNOMED_SYSTEM, SYNONYM_TYPE_ID));
    }

    @Test
    @DisplayName("Should preserve context, role, and type in SnomedDesignation fields")
    void shouldPreserveContextRoleAndType() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      SnomedDesignation designation = result.getFirst();
      assertThat(designation.getContext())
          .as("context should be the refsetId")
          .isEqualTo(REFSET_EN_GB);
      assertThat(designation.getRole())
          .as("role should be the acceptabilityId")
          .isEqualTo(PREFERRED);
      assertThat(designation.getType())
          .as("type should be the description typeId")
          .isEqualTo(SYNONYM_TYPE_ID);
    }

    @Test
    @DisplayName("Should set source to CS for all concept descriptions")
    void shouldSetSourceToCs() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result.getFirst().getSource()).isEqualTo(DesignationSource.CS);
    }

    @Test
    @DisplayName("Should return empty list when concept has no descriptions")
    void shouldReturnEmptyListWhenConceptHasNoDescriptions() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should skip descriptions without language refset entries")
    void shouldSkipDescriptionsWithoutLanguageRefsetEntries() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Orphaned term");
      // No language entries added
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should distinguish preferred and acceptable acceptability in role field")
    void shouldDistinguishPreferredAndAcceptable() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_US, ACCEPTABLE));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(SnomedDesignation::getRole)
          .containsExactly(PREFERRED, ACCEPTABLE);
    }

    @Test
    @DisplayName("Should collect designations from multiple descriptions")
    void shouldCollectDesignationsFromMultipleDescriptions() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");

      SnomedConceptDescription fsnEn =
          createDescription("en", FSN_TYPE_ID, "Diabetes mellitus (disorder)");
      fsnEn.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      SnomedConceptDescription synEn =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      synEn.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      SnomedConceptDescription synDe = createDescription("de", SYNONYM_TYPE_ID, "Zuckerkrankheit");
      synDe.getLanguageList().add(createLanguage(MODULE_DE, REFSET_DE_DE, PREFERRED));

      concept.getSnomedConceptDescriptionList().addAll(List.of(fsnEn, synEn, synDe));

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(3)
          .extracting(
              SnomedDesignationManagerTest::languageOf,
              SnomedDesignation::getType,
              SnomedDesignation::getValue)
          .containsExactly(
              tuple("en-GB", FSN_TYPE_ID, "Diabetes mellitus (disorder)"),
              tuple("en-GB", SYNONYM_TYPE_ID, "Diabetes mellitus"),
              tuple("de-DE", SYNONYM_TYPE_ID, "Zuckerkrankheit"));
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromSnomedConcept() - Active Status Filtering Tests")
  class ActiveStatusFilteringTests {

    @Test
    @DisplayName("Should skip inactive descriptions entirely")
    void shouldSkipInactiveDescriptions() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription inactiveDescription =
          createDescription("en", SYNONYM_TYPE_ID, "Retired term", false);
      inactiveDescription
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(inactiveDescription);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should skip inactive language refset entries within active descriptions")
    void shouldSkipInactiveLanguageEntries() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED, false));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should only collect active language entries from active descriptions")
    void shouldOnlyCollectActiveLanguageEntriesFromActiveDescriptions() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      description
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_US, ACCEPTABLE, false));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(1)
          .extracting(SnomedDesignationManagerTest::languageOf, SnomedDesignation::getRole)
          .containsExactly(tuple("en-GB", PREFERRED));
    }

    @Test
    @DisplayName("Should collect active descriptions and skip inactive ones in mixed list")
    void shouldCollectActiveAndSkipInactiveDescriptions() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");

      SnomedConceptDescription activeDescription =
          createDescription("en", SYNONYM_TYPE_ID, "Active term");
      activeDescription.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      SnomedConceptDescription inactiveDescription =
          createDescription("en", SYNONYM_TYPE_ID, "Inactive term", false);
      inactiveDescription
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_US, PREFERRED));

      concept
          .getSnomedConceptDescriptionList()
          .addAll(List.of(activeDescription, inactiveDescription));

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(1)
          .extracting(SnomedDesignation::getValue)
          .containsExactly("Active term");
    }

    @Test
    @DisplayName("Should return empty result when all descriptions are inactive")
    void shouldReturnEmptyWhenAllDescriptionsInactive() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");

      SnomedConceptDescription inactive1 =
          createDescription("en", FSN_TYPE_ID, "Retired FSN", false);
      inactive1.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));

      SnomedConceptDescription inactive2 =
          createDescription("de", SYNONYM_TYPE_ID, "Veraltetes Synonym", false);
      inactive2.getLanguageList().add(createLanguage(MODULE_DE, REFSET_DE_DE, PREFERRED));

      concept.getSnomedConceptDescriptionList().addAll(List.of(inactive1, inactive2));

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName(
        "Should return empty result when all language entries are inactive for active description")
    void shouldReturnEmptyWhenAllLanguageEntriesInactive() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description = createDescription("en", SYNONYM_TYPE_ID, "Valid term");
      description
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED, false));
      description
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_US, ACCEPTABLE, false));
      concept.getSnomedConceptDescriptionList().add(description);

      // WHEN
      manager.collectDesignationsFromSnomedConcept(concept);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromSnomedExpression() Tests")
  class ExpressionDesignationCollectionTests {

    private SnomedTerminologyManager mockTerminologyManager;
    private DemisSCGProcessor mockScgProcessor;
    private SnomedReleaseVersion mockReleaseVersion;
    private Expression mockExpression;

    @BeforeEach
    void setUpMocks() {
      mockTerminologyManager = Mockito.mock(SnomedTerminologyManager.class);
      mockScgProcessor = Mockito.mock(DemisSCGProcessor.class);
      mockReleaseVersion = Mockito.mock(SnomedReleaseVersion.class);
      mockExpression = Mockito.mock(Expression.class);
      when(mockTerminologyManager.isExpressionProcessingEnabled()).thenReturn(true);
      when(mockTerminologyManager.getScgProcessor()).thenReturn(mockScgProcessor);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when expression processing is disabled")
    void shouldThrowWhenExpressionProcessingIsDisabled() {
      // GIVEN
      when(mockTerminologyManager.isExpressionProcessingEnabled()).thenReturn(false);

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  manager.collectDesignationsFromSnomedExpression(
                      mockExpression, mockReleaseVersion, mockTerminologyManager))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("expression processing is disabled by configuration");
    }

    @Test
    @DisplayName(
        "Should add display designation with HL7 display use coding and resolved term string")
    void shouldAddDisplayDesignationWithTermString()
        throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
      // GIVEN
      String termString = "Diabetes mellitus (disorder)";
      when(mockScgProcessor.toTermString(mockExpression, mockReleaseVersion))
          .thenReturn(termString);

      // WHEN
      manager.collectDesignationsFromSnomedExpression(
          mockExpression, mockReleaseVersion, mockTerminologyManager);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .filteredOn(d -> d.getUse() != null)
          .hasSize(1)
          .first()
          .satisfies(
              d -> {
                assertThat(d.getSource()).isEqualTo(DesignationSource.CS);
                assertThat(d.getUse().getSystem()).isEqualTo(DESIGNATION_USAGE_SYSTEM);
                assertThat(d.getUse().getCode()).isEqualTo(DISPLAY_USE_CODE);
                assertThat(d.getValue()).isEqualTo(termString);
              });
    }

    @Test
    @DisplayName("Should add raw expression designation without use coding")
    void shouldAddRawExpressionDesignationWithoutUseCoding()
        throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
      // GIVEN
      String expressionString = "64572001 |Disease|";
      when(mockScgProcessor.toTermString(mockExpression, mockReleaseVersion))
          .thenReturn("Some term");
      when(mockExpression.toString()).thenReturn(expressionString);

      // WHEN
      manager.collectDesignationsFromSnomedExpression(
          mockExpression, mockReleaseVersion, mockTerminologyManager);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .filteredOn(d -> d.getUse() == null)
          .hasSize(1)
          .first()
          .satisfies(
              d -> {
                assertThat(d.getSource()).isEqualTo(DesignationSource.CS);
                assertThat(d.getValue()).isEqualTo(expressionString);
              });
    }

    @Test
    @DisplayName("Should add exactly two designations per expression")
    void shouldAddExactlyTwoDesignations()
        throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
      // GIVEN
      when(mockScgProcessor.toTermString(mockExpression, mockReleaseVersion))
          .thenReturn("Some term");

      // WHEN
      manager.collectDesignationsFromSnomedExpression(
          mockExpression, mockReleaseVersion, mockTerminologyManager);

      // THEN
      assertThat(manager.getFullDesignationList()).hasSize(2);
    }

    @Test
    @DisplayName("Should set source to CS for all expression designations")
    void shouldSetSourceToCs()
        throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
      // GIVEN
      when(mockScgProcessor.toTermString(mockExpression, mockReleaseVersion))
          .thenReturn("Some term");

      // WHEN
      manager.collectDesignationsFromSnomedExpression(
          mockExpression, mockReleaseVersion, mockTerminologyManager);
      List<SnomedDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .extracting(SnomedDesignation::getSource)
          .containsOnly(DesignationSource.CS);
    }

    @Test
    @DisplayName("Should propagate UnknownConceptException thrown during term resolution")
    void shouldPropagateUnknownConceptException() throws Exception {
      // GIVEN
      when(mockScgProcessor.toTermString(mockExpression, mockReleaseVersion))
          .thenThrow(new UnknownConceptException("Unknown concept"));

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  manager.collectDesignationsFromSnomedExpression(
                      mockExpression, mockReleaseVersion, mockTerminologyManager))
          .isInstanceOf(UnknownConceptException.class);
    }
  }

  @Nested
  @DisplayName("getDesignationsByLanguages() Tests")
  class GetDesignationsByLanguagesTests {

    private static final String DE_DE_VALUE = "Diabetes mellitus (de-DE)";
    private static final String DE_VALUE = "Diabetes mellitus (de)";
    private static final String EN_GB_VALUE = "Diabetes mellitus (en-GB)";
    private static final String EN_US_VALUE = "Diabetes mellitus (en-US)";

    @BeforeEach
    void setUpDesignations() {
      manager.addDesignation(createCsDesignation("de-DE", DE_DE_VALUE, SYNONYM_TYPE_ID, PREFERRED));
      manager.addDesignation(createCsDesignation("de", DE_VALUE, SYNONYM_TYPE_ID, PREFERRED));
      manager.addDesignation(createCsDesignation("en-GB", EN_GB_VALUE, SYNONYM_TYPE_ID, PREFERRED));
      manager.addDesignation(createCsDesignation("en-US", EN_US_VALUE, SYNONYM_TYPE_ID, PREFERRED));
    }

    @Test
    @DisplayName("Should return designations for a single language code")
    void shouldReturnDesignationsForSingleLanguageCode() {
      // GIVEN
      List<String> langCodes = List.of("de");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN
      assertThat(result)
          .extracting(SnomedDesignation::getValue)
          .containsExactlyInAnyOrder(DE_VALUE, DE_DE_VALUE);
    }

    @Test
    @DisplayName("Should return designations for multiple language codes as union")
    void shouldReturnDesignationsForMultipleLanguageCodes() {
      // GIVEN
      List<String> langCodes = List.of("de", "en");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN
      assertThat(result)
          .extracting(SnomedDesignation::getValue)
          .containsExactlyInAnyOrder(DE_VALUE, DE_DE_VALUE, EN_GB_VALUE, EN_US_VALUE);
    }

    @Test
    @DisplayName("Should expand base language code to include regional variant designations")
    void shouldExpandBaseLanguageToVariants() {
      // GIVEN - "de" is requested; "de-DE" designation should be included via BCP 47 expansion
      List<String> langCodes = List.of("de");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN
      assertThat(result).extracting(SnomedDesignation::getValue).contains(DE_DE_VALUE);
    }

    @Test
    @DisplayName("Should expand regional variant to include base language designation")
    void shouldExpandVariantToBaseLanguage() {
      // GIVEN - "de-DE" is requested; "de" designation should also be included via BCP 47 hierarchy
      List<String> langCodes = List.of("de-DE");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN
      assertThat(result).extracting(SnomedDesignation::getValue).contains(DE_VALUE);
    }

    @Test
    @DisplayName("Should return empty list when no designations match the requested language")
    void shouldReturnEmptyListWhenNoMatch() {
      // GIVEN - "fr" is a supported language but no French designations exist
      List<String> langCodes = List.of("fr");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should deduplicate designations matched by multiple overlapping language codes")
    void shouldDeduplicateDesignationsMatchedByMultipleCodes() {
      // GIVEN - "de" and "de-DE" both expand to include "de" and "de-DE" designations
      List<String> langCodes = List.of("de", "de-DE");

      // WHEN
      List<SnomedDesignation> result = manager.getDesignationsByLanguages(langCodes);

      // THEN - each designation appears exactly once despite matching both codes
      assertThat(result)
          .extracting(SnomedDesignation::getValue)
          .containsExactlyInAnyOrder(DE_VALUE, DE_DE_VALUE);
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromConceptReferenceComponents() Tests")
  class CollectDesignationsFromConceptReferenceComponentsTests {

    private SnomedDesignationManager designationManager;

    @BeforeEach
    void setUp() {
      designationManager = new SnomedDesignationManager();
    }

    @Test
    @DisplayName("Should still collect VS designations when all CS descriptions are inactive")
    void shouldStillCollectVsDesignationsWhenAllCsDescriptionsInactive() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription inactiveDescription =
          createDescription("en", SYNONYM_TYPE_ID, "Retired term", false);
      inactiveDescription
          .getLanguageList()
          .add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(inactiveDescription);

      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference("VS Display", "de");

      // WHEN
      designationManager.collectDesignationsFromSnomedConcept(concept);
      designationManager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<SnomedDesignation> result = designationManager.getFullDesignationList();

      // THEN — inactive CS descriptions contribute nothing; VS designation is collected
      assertThat(result)
          .hasSize(1)
          .extracting(SnomedDesignation::getSource, SnomedDesignation::getValue)
          .containsExactly(tuple(DesignationSource.VS, "VS Display"));
    }

    @Test
    @DisplayName("Should collect CS and VS designations combined")
    void shouldCollectCsAndVsDesignationsCombined() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");
      SnomedConceptDescription description =
          createDescription("en", SYNONYM_TYPE_ID, "Diabetes mellitus");
      description.getLanguageList().add(createLanguage(MODULE_CORE, REFSET_EN_GB, PREFERRED));
      concept.getSnomedConceptDescriptionList().add(description);

      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference("VS Display", "de");
      vsRef.addDesignation(createVsDesignationComponent("de", "VS Designation"));

      // WHEN
      designationManager.collectDesignationsFromSnomedConcept(concept);
      designationManager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<SnomedDesignation> result = designationManager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(3)
          .extracting(SnomedDesignation::getSource, SnomedDesignation::getValue)
          .containsExactly(
              tuple(DesignationSource.CS, "Diabetes mellitus"),
              tuple(DesignationSource.VS, "VS Display"),
              tuple(DesignationSource.VS, "VS Designation"));
    }

    @Test
    @DisplayName(
        "Should return empty list when concept has no descriptions and no references exist")
    void shouldReturnEmptyListWhenNoDescriptionsAndNoReferences() {
      // GIVEN
      SnomedConcept concept = createConcept("12345");

      // WHEN
      designationManager.collectDesignationsFromSnomedConcept(concept);
      designationManager.collectDesignationsFromConceptReferenceComponents(List.of());
      List<SnomedDesignation> result = designationManager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }
  }
}
