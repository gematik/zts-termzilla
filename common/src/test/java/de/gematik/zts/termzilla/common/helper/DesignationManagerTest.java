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
package de.gematik.zts.termzilla.common.helper;

import static de.gematik.zts.termzilla.api.constants.ResourceUserData.USERDATA_LANGUAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.Designation.DesignationSource;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import java.util.List;
import java.util.Set;
import lombok.experimental.SuperBuilder;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for the generic behavior of {@link DesignationManager}.
 *
 * <p>Uses a minimal {@link TestDesignationManager} that applies simple display-use filtering
 * (identical for both VS and CS sources) so that only the base class logic is exercised: priority
 * chain, language matching, source preference, and no-language fallback.
 */
@DisplayName("DesignationManager Tests")
class DesignationManagerTest {

  private static final String DESIGNATION_USAGE_SYSTEM =
      "http://terminology.hl7.org/CodeSystem/designation-usage";
  private static final String DISPLAY_USE_CODE = "display";

  private TestDesignationManager manager;

  @BeforeEach
  void setUp() {
    manager = new TestDesignationManager();
  }

  // ==========================================================================
  // Minimal inner types for testing
  // ==========================================================================

  /** Minimal concrete designation with no extra fields. */
  @SuperBuilder
  static class TestDesignation extends Designation {}

  /**
   * Minimal concrete manager that selects designations solely by HL7 {@code
   * designation-usage#display} use coding, treating VS and CS sources identically.
   */
  static class TestDesignationManager extends DesignationManager<TestDesignation> {

    @Override
    protected TestDesignation findDisplayForLanguage(DesignationSource source, String language) {
      return getFullDesignationList().stream()
          .filter(d -> d.getSource() == source)
          .filter(d -> d.getLanguage() != null && language.equals(d.getLanguage().getValue()))
          .filter(this::hasDisplayUse)
          .findFirst()
          .orElse(null);
    }

    @Override
    protected TestDesignation buildVsDesignation(CodeType language, Coding use, String value) {
      return TestDesignation.builder()
          .source(Designation.DesignationSource.VS)
          .language(language)
          .use(use)
          .value(value)
          .build();
    }

    @Override
    protected TestDesignation findDisplayWithoutLanguage(DesignationSource source) {
      return getFullDesignationList().stream()
          .filter(d -> d.getSource() == source)
          .filter(
              d ->
                  d.getLanguage() == null
                      || d.getLanguage().getValue() == null
                      || d.getLanguage().getValue().isBlank())
          .filter(this::hasDisplayUse)
          .findFirst()
          .orElse(null);
    }

    private boolean hasDisplayUse(TestDesignation d) {
      return d.getUse() != null
          && DESIGNATION_USAGE_SYSTEM.equals(d.getUse().getSystem())
          && DISPLAY_USE_CODE.equals(d.getUse().getCode());
    }
  }

  // ==========================================================================
  // Helper methods
  // ==========================================================================

  private static TestDesignation createDisplayDesignation(
      DesignationSource source, String language, String value) {
    return createDesignation(source, language, value, DISPLAY_USE_CODE);
  }

  private static TestDesignation createDesignation(
      DesignationSource source, String language, String value, String useCode) {
    return createDesignation(source, language, value, DESIGNATION_USAGE_SYSTEM, useCode);
  }

  private static TestDesignation createDesignation(
      DesignationSource source, String language, String value, String useSystem, String useCode) {
    TestDesignation.TestDesignationBuilder<?, ?> builder =
        TestDesignation.builder().source(source).value(value);

    if (language != null) {
      builder.language(new CodeType(language));
    }

    if (useSystem != null || useCode != null) {
      builder.use(new Coding(useSystem, useCode, null));
    }

    return builder.build();
  }

  private static LanguageSelectionParameter lspWithDisplayLanguage(String language) {
    return LanguageSelectionParameter.builder()
        .displayLanguageParameter(LanguageParameter.parse(language))
        .build();
  }

  private static LanguageSelectionParameter lspWithAcceptLanguage(String headerValue) {
    return LanguageSelectionParameter.builder()
        .acceptLanguageHeader(LanguageParameter.parse(headerValue))
        .build();
  }

  private static LanguageSelectionParameter lspWithSystemDefault(String language) {
    return LanguageSelectionParameter.builder()
        .systemDefaultLanguage(LanguageParameter.parse(language))
        .build();
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

  private static String languageOf(Designation d) {
    return d.getLanguage() != null ? d.getLanguage().getValue() : null;
  }

  // ==========================================================================
  // Tests
  // ==========================================================================

  @Nested
  @DisplayName("addDesignation() Tests")
  class AddDesignationTests {

    @Test
    @DisplayName("Should accept designation and make it available for selection")
    void shouldAcceptDesignation() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Testbegriff");

      // WHEN
      manager.addDesignation(designation);
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Added designation should be retrievable")
          .isNotNull()
          .isEqualTo(designation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - Priority Chain Tests")
  class PriorityChainTests {

    @Test
    @DisplayName("Should return null when no designations are present")
    void shouldReturnNullWhenNoDesignations() {
      // GIVEN
      LanguageSelectionParameter lsp = lspWithDisplayLanguage("de-DE");

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result).as("Should return null when no designations exist").isNull();
    }

    @Test
    @DisplayName("Should return null when all parameters are null and no fallback exists")
    void shouldReturnNullWhenAllParametersNull() {
      // GIVEN
      manager.addDesignation(
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Testbegriff"));
      LanguageSelectionParameter lsp = LanguageSelectionParameter.builder().build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result).as("Should return null when no selection parameters are set").isNull();
    }

    @Test
    @DisplayName("Should prefer displayLanguage over Accept-Language")
    void shouldPreferDisplayLanguageOverAcceptLanguage() {
      // GIVEN
      TestDesignation germanDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      TestDesignation englishDesignation =
          createDisplayDesignation(DesignationSource.CS, "en-US", "English");
      manager.addDesignation(germanDesignation);
      manager.addDesignation(englishDesignation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .displayLanguageParameter(LanguageParameter.parse("en-US"))
              .acceptLanguageHeader(LanguageParameter.parse("de-DE"))
              .build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("displayLanguage should take priority over Accept-Language")
          .isEqualTo(englishDesignation);
    }

    @Test
    @DisplayName("Should fall back to Accept-Language when displayLanguage is not set")
    void shouldFallBackToAcceptLanguage() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "en-GB", "British English");
      manager.addDesignation(designation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(LanguageParameter.parse("en-GB"))
              .systemDefaultLanguage(LanguageParameter.parse("de-DE"))
              .build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should use Accept-Language when displayLanguage is absent")
          .isEqualTo(designation);
    }

    @Test
    @DisplayName("Should fall back to system default when Accept-Language finds no match")
    void shouldFallBackToSystemDefault() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      manager.addDesignation(designation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(LanguageParameter.parse("fr-FR"))
              .systemDefaultLanguage(LanguageParameter.parse("de-DE"))
              .build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to system default when Accept-Language mismatches")
          .isEqualTo(designation);
    }

    @Test
    @DisplayName("Should fall back to VS language as last resort before getDisplayFallback()")
    void shouldFallBackToVsLanguage() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "de-AT", "Austrian");
      manager.addDesignation(designation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .acceptLanguageHeader(LanguageParameter.parse("fr-FR"))
              .systemDefaultLanguage(LanguageParameter.parse("en-US"))
              .vsLanguage(LanguageParameter.parse("de-AT"))
              .build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall back to VS language when all higher priorities miss")
          .isEqualTo(designation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - displayLanguage Strict Behavior Tests")
  class DisplayLanguageStrictTests {

    @Test
    @DisplayName(
        "Should return null when displayLanguage is set but no matching designation exists")
    void shouldReturnNullWhenDisplayLanguageHasNoMatch() {
      // GIVEN
      manager.addDesignation(createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch"));
      LanguageSelectionParameter lsp = lspWithDisplayLanguage("fr-FR");

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should return null strictly when displayLanguage cannot be satisfied")
          .isNull();
    }

    @Test
    @DisplayName(
        "Should not fall through to Accept-Language when displayLanguage is set but misses")
    void shouldNotFallThroughWhenDisplayLanguageMisses() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      manager.addDesignation(designation);

      LanguageSelectionParameter lsp =
          LanguageSelectionParameter.builder()
              .displayLanguageParameter(LanguageParameter.parse("fr-FR"))
              .acceptLanguageHeader(LanguageParameter.parse("de-DE"))
              .build();

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("displayLanguage is strict — must not fall through to Accept-Language")
          .isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - Source Preference Tests")
  class SourcePreferenceTests {

    @Test
    @DisplayName("Should prefer VS-sourced designation over CS-sourced for same language")
    void shouldPreferVsOverCs() {
      // GIVEN
      TestDesignation csDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "CS value");
      TestDesignation vsDesignation =
          createDisplayDesignation(DesignationSource.VS, "de-DE", "VS value");
      manager.addDesignation(csDesignation);
      manager.addDesignation(vsDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("VS-sourced designation should be preferred over CS-sourced")
          .isEqualTo(vsDesignation);
    }

    @Test
    @DisplayName("Should return CS-sourced designation when no VS-sourced exists")
    void shouldReturnCsWhenNoVsExists() {
      // GIVEN
      TestDesignation csDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "CS value");
      manager.addDesignation(csDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Should return CS designation when no VS designation is available")
          .isEqualTo(csDesignation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - Language Matching Tests")
  class LanguageMatchingTests {

    @DisplayName("Should match language directly and through language hierarchy")
    @ParameterizedTest(name = "stored={0}, requested={1}")
    @CsvSource({"de-DE,de-DE", "de-DE,de", "de,de-DE"})
    void shouldMatchLanguageUsingExactAndHierarchy(
        String storedLanguage, String requestedLanguage) {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, storedLanguage, "Deutsch");
      manager.addDesignation(designation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage(requestedLanguage));

      // THEN
      assertThat(result)
          .as(
              "Requested language '%s' should match stored language '%s'",
              requestedLanguage, storedLanguage)
          .isEqualTo(designation);
    }

    @Test
    @DisplayName("Should not match unrelated language")
    void shouldNotMatchUnrelatedLanguage() {
      // GIVEN
      TestDesignation designation =
          createDisplayDesignation(DesignationSource.CS, "fr-FR", "Fran\u00e7ais");
      manager.addDesignation(designation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result)
          .as("Should not match unrelated language 'fr-FR' for request 'de-DE'")
          .isNull();
    }
  }

  @Nested
  @DisplayName("getDisplay() - No-Language Fallback Tests")
  class NoLanguageFallbackTests {

    @Test
    @DisplayName("Should use designation without language as fallback when language search fails")
    void shouldFallBackToDesignationWithoutLanguage() {
      // GIVEN
      TestDesignation noLangDesignation =
          createDisplayDesignation(DesignationSource.CS, null, "fallback value");
      manager.addDesignation(noLangDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithSystemDefault("de-DE"));

      // THEN
      assertThat(result)
          .as("Should fall back to designation without language code")
          .isEqualTo(noLangDesignation);
    }

    @Test
    @DisplayName("Should prefer VS-sourced no-language designation over CS-sourced")
    void shouldPreferVsNoLanguageOverCsNoLanguage() {
      // GIVEN
      TestDesignation csNoLang =
          createDisplayDesignation(DesignationSource.CS, null, "CS fallback");
      TestDesignation vsNoLang =
          createDisplayDesignation(DesignationSource.VS, null, "VS fallback");
      manager.addDesignation(csNoLang);
      manager.addDesignation(vsNoLang);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithSystemDefault("fr-FR"));

      // THEN
      assertThat(result)
          .as("VS no-language designation should be preferred over CS no-language")
          .isEqualTo(vsNoLang);
    }

    @Test
    @DisplayName("Should prefer language-matched designation over no-language fallback")
    void shouldPreferLanguageMatchOverNoLanguageFallback() {
      // GIVEN
      TestDesignation noLangDesignation =
          createDisplayDesignation(DesignationSource.CS, null, "no-lang fallback");
      TestDesignation matchedDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      manager.addDesignation(noLangDesignation);
      manager.addDesignation(matchedDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithSystemDefault("de-DE"));

      // THEN
      assertThat(result)
          .as("Language-matched designation should be preferred over no-language fallback")
          .isEqualTo(matchedDesignation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - Accept-Language Multi-Range Tests")
  class AcceptLanguageMultiRangeTests {

    @Test
    @DisplayName("Should respect quality order in Accept-Language header")
    void shouldRespectQualityOrder() {
      // GIVEN
      TestDesignation germanDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      TestDesignation englishDesignation =
          createDisplayDesignation(DesignationSource.CS, "en-US", "English");
      manager.addDesignation(germanDesignation);
      manager.addDesignation(englishDesignation);

      // Accept-Language: en-US, de-DE;q=0.8 -> en-US has higher priority
      LanguageSelectionParameter lsp = lspWithAcceptLanguage("en-US, de-DE;q=0.8");

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should select language with highest quality value")
          .isEqualTo(englishDesignation);
    }

    @Test
    @DisplayName("Should fall through to second range when first has no match")
    void shouldFallThroughToSecondRange() {
      // GIVEN
      TestDesignation germanDesignation =
          createDisplayDesignation(DesignationSource.CS, "de-DE", "Deutsch");
      manager.addDesignation(germanDesignation);

      // Accept-Language: fr-FR, de-DE;q=0.8 -> fr-FR misses, de-DE should match
      LanguageSelectionParameter lsp = lspWithAcceptLanguage("fr-FR, de-DE;q=0.8");

      // WHEN
      TestDesignation result = manager.getDisplay(lsp);

      // THEN
      assertThat(result)
          .as("Should fall through to second range when first misses")
          .isEqualTo(germanDesignation);
    }
  }

  @Nested
  @DisplayName("getDisplay() - Display Use Filter Tests")
  class DisplayUseFilterTests {

    @Test
    @DisplayName("Should ignore designations without display use coding")
    void shouldIgnoreDesignationsWithoutDisplayUse() {
      // GIVEN
      TestDesignation noUseDesignation =
          createDesignation(DesignationSource.CS, "de-DE", "no-use", null);
      manager.addDesignation(noUseDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("Designations without display use should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore designations with wrong use system")
    void shouldIgnoreDesignationsWithWrongUseSystem() {
      // GIVEN
      TestDesignation wrongSystemDesignation =
          TestDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("wrong system")
              .use(new Coding("http://other-system.org", DISPLAY_USE_CODE, null))
              .build();
      manager.addDesignation(wrongSystemDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("Designations with wrong use system should be ignored").isNull();
    }

    @Test
    @DisplayName("Should ignore designations with wrong use code")
    void shouldIgnoreDesignationsWithWrongUseCode() {
      // GIVEN
      TestDesignation wrongCodeDesignation =
          TestDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("wrong code")
              .use(new Coding(DESIGNATION_USAGE_SYSTEM, "definition", null))
              .build();
      manager.addDesignation(wrongCodeDesignation);

      // WHEN
      TestDesignation result = manager.getDisplay(lspWithDisplayLanguage("de-DE"));

      // THEN
      assertThat(result).as("Designations with wrong use code should be ignored").isNull();
    }
  }

  // ==========================================================================
  // selectDesignations() Tests
  // ==========================================================================

  @Nested
  @DisplayName("selectDesignations() - Empty Parameter List Tests")
  class SelectDesignationsEmptyParamTests {

    @Test
    @DisplayName("Should return all designations when parameter list is empty")
    void shouldReturnAllDesignationsWhenParamListIsEmpty() {
      // GIVEN
      TestDesignation first =
          createDesignation(DesignationSource.CS, "de-DE", "Deutsch", DISPLAY_USE_CODE);
      TestDesignation second =
          createDesignation(DesignationSource.CS, "en-US", "English", DISPLAY_USE_CODE);
      TestDesignation third =
          createDesignation(DesignationSource.VS, "fr-FR", "Fran\u00e7ais", null);
      manager.addDesignation(first);
      manager.addDesignation(second);
      manager.addDesignation(third);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of());

      // THEN
      assertThat(result)
          .as("Empty param list should return all designations")
          .containsExactlyInAnyOrder(first, second, third);
    }

    @Test
    @DisplayName("Should return empty set when no designations exist and parameter list is empty")
    void shouldReturnEmptySetWhenNoDesignationsAndParamListIsEmpty() {
      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of());

      // THEN
      assertThat(result)
          .as("Empty param list with no designations should return empty set")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Language Selection Tests")
  class SelectDesignationsByLanguageTests {

    @Test
    @DisplayName("Should select designations matching exact language code")
    void shouldSelectDesignationsByExactLanguage() {
      // GIVEN
      TestDesignation german =
          createDesignation(DesignationSource.CS, "de-DE", "Deutsch", DISPLAY_USE_CODE);
      TestDesignation english =
          createDesignation(DesignationSource.CS, "en-US", "English", DISPLAY_USE_CODE);
      manager.addDesignation(german);
      manager.addDesignation(english);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("de-DE"));

      // THEN
      assertThat(result)
          .as("Should select only designations matching 'de-DE'")
          .containsExactly(german);
    }

    @Test
    @DisplayName("Should expand base language to all regional variants")
    void shouldSelectDesignationsByBaseLanguageExpandingToVariants() {
      // GIVEN
      TestDesignation deDE = createDesignation(DesignationSource.CS, "de-DE", "Deutsch (DE)", null);
      TestDesignation deAT = createDesignation(DesignationSource.CS, "de-AT", "Deutsch (AT)", null);
      TestDesignation english = createDesignation(DesignationSource.CS, "en-US", "English", null);
      manager.addDesignation(deDE);
      manager.addDesignation(deAT);
      manager.addDesignation(english);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("de"));

      // THEN
      assertThat(result)
          .as("Base language 'de' should match all German variants")
          .containsExactlyInAnyOrder(deDE, deAT);
    }

    @Test
    @DisplayName("Should expand regional variant to include base language")
    void shouldSelectDesignationsByRegionalVariantIncludingBase() {
      // GIVEN
      TestDesignation baseGerman = createDesignation(DesignationSource.CS, "de", "Deutsch", null);
      TestDesignation english = createDesignation(DesignationSource.CS, "en-US", "English", null);
      manager.addDesignation(baseGerman);
      manager.addDesignation(english);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("de-DE"));

      // THEN
      assertThat(result)
          .as("Regional variant 'de-DE' should also match base language 'de'")
          .containsExactly(baseGerman);
    }

    @Test
    @DisplayName("Should return empty set when language has no match")
    void shouldReturnEmptyWhenLanguageHasNoMatch() {
      // GIVEN
      TestDesignation english = createDesignation(DesignationSource.CS, "en-US", "English", null);
      manager.addDesignation(english);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("fr-FR"));

      // THEN
      assertThat(result).as("Unmatched language should yield empty result").isEmpty();
    }

    @Test
    @DisplayName("Should not select designations with null language")
    void shouldNotSelectDesignationsWithNullLanguage() {
      // GIVEN
      TestDesignation noLang = createDesignation(DesignationSource.CS, null, "no language", null);
      manager.addDesignation(noLang);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("de-DE"));

      // THEN
      assertThat(result)
          .as("Designations without language should not be matched by language param")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Use Code Selection Tests (no pipe)")
  class SelectDesignationsByUseCodeTests {

    @Test
    @DisplayName("Should select designations by use code")
    void shouldSelectDesignationsByUseCode() {
      // GIVEN
      TestDesignation displayDesignation =
          createDesignation(DesignationSource.CS, "de-DE", "Anzeige", DISPLAY_USE_CODE);
      TestDesignation definitionDesignation =
          createDesignation(DesignationSource.CS, "de-DE", "Definition", "definition");
      manager.addDesignation(displayDesignation);
      manager.addDesignation(definitionDesignation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of(DISPLAY_USE_CODE));

      // THEN
      assertThat(result)
          .as("Should select only designations with matching use code")
          .containsExactly(displayDesignation);
    }

    @Test
    @DisplayName("Should not include designations without use coding")
    void shouldNotIncludeDesignationsWithoutUse() {
      // GIVEN
      TestDesignation noUse = createDesignation(DesignationSource.CS, "de-DE", "no use", null);
      manager.addDesignation(noUse);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of(DISPLAY_USE_CODE));

      // THEN
      assertThat(result)
          .as("Designations without use coding should not be selected by use code param")
          .isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when use code has no match")
    void shouldReturnEmptyWhenUseCodeHasNoMatch() {
      // GIVEN
      TestDesignation designation =
          createDesignation(DesignationSource.CS, "de-DE", "Anzeige", DISPLAY_USE_CODE);
      manager.addDesignation(designation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("definition"));

      // THEN
      assertThat(result).as("Non-matching use code should yield empty result").isEmpty();
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Combined Language and Use Code Tests")
  class SelectDesignationsByLanguageAndUseCodeCombinedTests {

    @Test
    @DisplayName("Should combine language and use code matches into union")
    void shouldCombineLanguageAndUseCodeMatches() {
      // GIVEN — "de-DE" is a valid language AND "display" is a valid use code
      TestDesignation germanNoUse =
          createDesignation(DesignationSource.CS, "de-DE", "German no use", null);
      TestDesignation englishDisplay =
          createDesignation(DesignationSource.CS, "en-US", "English display", DISPLAY_USE_CODE);
      TestDesignation germanDisplay =
          createDesignation(DesignationSource.CS, "de-DE", "German display", DISPLAY_USE_CODE);
      manager.addDesignation(germanNoUse);
      manager.addDesignation(englishDisplay);
      manager.addDesignation(germanDisplay);

      // WHEN — "de-DE" selects by language, "display" selects by use code
      Set<TestDesignation> result = manager.selectDesignations(List.of("de-DE", DISPLAY_USE_CODE));

      // THEN — union: germanNoUse (language match), englishDisplay (use match), germanDisplay
      // (both)
      assertThat(result)
          .as("Should combine results from language and use code matching")
          .containsExactlyInAnyOrder(germanNoUse, englishDisplay, germanDisplay);
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Pipe (system|code) Selection Tests")
  class SelectDesignationsBySystemCodePipeTests {

    private static final String CUSTOM_SYSTEM = "http://example.org/usage";

    @Test
    @DisplayName("Should select by system and code when both are provided")
    void shouldSelectBySystemAndCode() {
      // GIVEN
      TestDesignation matching =
          createDesignation(DesignationSource.CS, "de-DE", "match", CUSTOM_SYSTEM, "synonym");
      TestDesignation wrongCode =
          createDesignation(DesignationSource.CS, "de-DE", "wrong code", CUSTOM_SYSTEM, "other");
      TestDesignation wrongSystem =
          createDesignation(
              DesignationSource.CS, "de-DE", "wrong system", DESIGNATION_USAGE_SYSTEM, "synonym");
      manager.addDesignation(matching);
      manager.addDesignation(wrongCode);
      manager.addDesignation(wrongSystem);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of(CUSTOM_SYSTEM + "|synonym"));

      // THEN
      assertThat(result)
          .as("Should match only designations with both system and code")
          .containsExactly(matching);
    }

    @Test
    @DisplayName("Should select by system only when code is empty after pipe")
    void shouldSelectBySystemOnly() {
      // GIVEN
      TestDesignation matchA =
          createDesignation(DesignationSource.CS, "de-DE", "match A", CUSTOM_SYSTEM, "synonym");
      TestDesignation matchB =
          createDesignation(DesignationSource.CS, "de-DE", "match B", CUSTOM_SYSTEM, "definition");
      TestDesignation noMatch =
          createDesignation(
              DesignationSource.CS, "de-DE", "no match", DESIGNATION_USAGE_SYSTEM, "display");
      manager.addDesignation(matchA);
      manager.addDesignation(matchB);
      manager.addDesignation(noMatch);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of(CUSTOM_SYSTEM + "|"));

      // THEN
      assertThat(result)
          .as("System-only pipe should match all designations with that system")
          .containsExactlyInAnyOrder(matchA, matchB);
    }

    @Test
    @DisplayName("Should select by code without system when system is empty before pipe")
    void shouldSelectByCodeWithoutSystem() {
      // GIVEN — designation with code but explicitly no system
      TestDesignation noSystemDesignation =
          TestDesignation.builder()
              .source(DesignationSource.CS)
              .language(new CodeType("de-DE"))
              .value("no-system match")
              .use(new Coding(null, "synonym", null))
              .build();
      TestDesignation withSystemDesignation =
          createDesignation(DesignationSource.CS, "de-DE", "has system", CUSTOM_SYSTEM, "synonym");
      manager.addDesignation(noSystemDesignation);
      manager.addDesignation(withSystemDesignation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("|synonym"));

      // THEN
      assertThat(result)
          .as("Pipe with empty system should only match designations without a system")
          .containsExactly(noSystemDesignation);
    }

    @Test
    @DisplayName("Should not match designations with system when pipe has empty system")
    void shouldNotMatchCodeWithoutSystemWhenDesignationHasSystem() {
      // GIVEN
      TestDesignation withSystem =
          createDesignation(DesignationSource.CS, "de-DE", "has system", CUSTOM_SYSTEM, "synonym");
      manager.addDesignation(withSystem);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("|synonym"));

      // THEN
      assertThat(result)
          .as("Designation with a system should not be matched by |code pattern")
          .isEmpty();
    }

    @Test
    @DisplayName("Should skip invalid pipe values with more than one separator")
    void shouldSkipInvalidPipeValues() {
      // GIVEN
      TestDesignation designation =
          createDesignation(DesignationSource.CS, "de-DE", "value", CUSTOM_SYSTEM, "synonym");
      manager.addDesignation(designation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("a|b|c"));

      // THEN
      assertThat(result)
          .as("Invalid pipe value with multiple separators should be skipped")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Multiple Parameters and Deduplication Tests")
  class SelectDesignationsMultipleParamsTests {

    @Test
    @DisplayName("Should combine results from multiple parameters into union")
    void shouldCombineResultsFromMultipleParams() {
      // GIVEN
      TestDesignation german = createDesignation(DesignationSource.CS, "de-DE", "Deutsch", null);
      TestDesignation englishDisplay =
          createDesignation(DesignationSource.CS, "en-US", "English", DISPLAY_USE_CODE);
      manager.addDesignation(german);
      manager.addDesignation(englishDisplay);

      // WHEN — language param + pipe param
      Set<TestDesignation> result =
          manager.selectDesignations(
              List.of("de-DE", DESIGNATION_USAGE_SYSTEM + "|" + DISPLAY_USE_CODE));

      // THEN
      assertThat(result)
          .as("Should combine results from language and pipe params")
          .containsExactlyInAnyOrder(german, englishDisplay);
    }

    @Test
    @DisplayName("Should deduplicate when same designation matches multiple parameters")
    void shouldDeduplicateWhenSameDesignationMatchesMultipleParams() {
      // GIVEN — this designation matches both "de-DE" (language) and "display" (use code)
      TestDesignation designation =
          createDesignation(DesignationSource.CS, "de-DE", "Deutsch", DISPLAY_USE_CODE);
      manager.addDesignation(designation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("de-DE", DISPLAY_USE_CODE));

      // THEN
      assertThat(result)
          .as("Same designation matched by multiple params should appear only once")
          .hasSize(1)
          .containsExactly(designation);
    }
  }

  @Nested
  @DisplayName("collectDesignationsFromConceptReferenceComponents() Tests")
  class CollectDesignationsFromConceptReferenceComponentsTests {

    @Test
    @DisplayName("Should add VS display as designation with VS language")
    void shouldAddVsDisplayAsDesignation() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference("VS Display", "de");

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      TestDesignation designation = result.getFirst();
      assertThat(designation.getSource()).isEqualTo(DesignationSource.VS);
      assertThat(languageOf(designation)).isEqualTo("de");
      assertThat(designation.getUse()).isNull();
      assertThat(designation.getValue()).isEqualTo("VS Display");
    }

    @Test
    @DisplayName("Should not add VS display designation when concept reference has no display")
    void shouldNotAddVsDisplayWhenReferenceHasNoDisplay() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference(null, "de");

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should add VS designations from explicit concept references")
    void shouldAddVsDesignationsFromConceptReferences() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference(null, "de");
      vsRef.addDesignation(createVsDesignationComponent("de-AT", "Österreichischer Anzeigename"));
      vsRef.addDesignation(createVsDesignationComponent("de-CH", "Schweizer Anzeigename"));

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(
              Designation::getSource, DesignationManagerTest::languageOf, Designation::getValue)
          .containsExactly(
              tuple(DesignationSource.VS, "de-AT", "Österreichischer Anzeigename"),
              tuple(DesignationSource.VS, "de-CH", "Schweizer Anzeigename"));
    }

    @Test
    @DisplayName(
        "Should use USERDATA_LANGUAGE as fallback when VS designation has no explicit language")
    void shouldUseUserDataLanguageFallbackForVsDesignationWithoutLanguage() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference(null, "de");
      vsRef.addDesignation(createVsDesignationComponent(null, "Anzeigename ohne Sprache"));

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst())).isEqualTo("de");
    }

    @Test
    @DisplayName("Should process multiple concept references")
    void shouldProcessMultipleConceptReferences() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef1 = createVsConceptReference("Display 1", "de");
      ValueSet.ConceptReferenceComponent vsRef2 = createVsConceptReference("Display 2", "fr");

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef1, vsRef2));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(DesignationManagerTest::languageOf, Designation::getValue)
          .containsExactly(tuple("de", "Display 1"), tuple("fr", "Display 2"));
    }

    @Test
    @DisplayName("Should handle null USERDATA_LANGUAGE gracefully")
    void shouldHandleNullUserDataLanguage() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference("VS Display", null);

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result).hasSize(1);
      assertThat(languageOf(result.getFirst())).isNull();
    }

    @Test
    @DisplayName("Should collect VS display and VS designations from same concept reference")
    void shouldCollectVsDisplayAndDesignationsFromSameReference() {
      // GIVEN
      ValueSet.ConceptReferenceComponent vsRef = createVsConceptReference("VS Display", "de");
      vsRef.addDesignation(createVsDesignationComponent("fr", "Désignation VS"));

      // WHEN
      manager.collectDesignationsFromConceptReferenceComponents(List.of(vsRef));
      List<TestDesignation> result = manager.getFullDesignationList();

      // THEN
      assertThat(result)
          .hasSize(2)
          .extracting(Designation::getSource, Designation::getValue)
          .containsExactly(
              tuple(DesignationSource.VS, "VS Display"),
              tuple(DesignationSource.VS, "Désignation VS"));
    }
  }

  @Nested
  @DisplayName("selectDesignations() - Custom Criteria Hook Tests")
  class SelectDesignationsCustomCriteriaTests {

    private static final String CUSTOM_FILTER_TOKEN = "custom-filter";
    private static final String CUSTOM_SYSTEM = "http://example.org/custom";

    /**
     * Test subclass that overrides the custom criteria hook to recognize a specific token (with or
     * without pipe) and return matching designations tagged with that token as their value prefix.
     */
    static class CustomCriteriaManager extends TestDesignationManager {

      @Override
      protected List<TestDesignation> selectDesignationsByCustomCriteria(String paramValue) {
        if (CUSTOM_FILTER_TOKEN.equals(paramValue) || paramValue.contains(CUSTOM_FILTER_TOKEN)) {
          return getFullDesignationList().stream()
              .filter(d -> d.getValue() != null && d.getValue().startsWith(CUSTOM_FILTER_TOKEN))
              .toList();
        }
        return super.selectDesignationsByCustomCriteria(paramValue);
      }
    }

    @Test
    @DisplayName("Should return empty selection for unknown param value when default hook is used")
    void shouldReturnEmptyByDefaultForUnknownParamValue() {
      // GIVEN
      TestDesignation designation =
          createDesignation(DesignationSource.CS, "de-DE", "some value", null);
      manager.addDesignation(designation);

      // WHEN
      Set<TestDesignation> result = manager.selectDesignations(List.of("unknown-token"));

      // THEN
      assertThat(result).as("Default hook should not match unknown param values").isEmpty();
    }

    @Test
    @DisplayName(
        "Should include custom criteria results alongside standard language and use matches")
    void shouldIncludeCustomCriteriaResultsAlongsideStandardMatches() {
      // GIVEN
      CustomCriteriaManager customManager = new CustomCriteriaManager();

      TestDesignation germanDesignation =
          createDesignation(DesignationSource.CS, "de-DE", "German term", null);
      TestDesignation customDesignation =
          createDesignation(DesignationSource.CS, "en-US", CUSTOM_FILTER_TOKEN + ":matched", null);
      customManager.addDesignation(germanDesignation);
      customManager.addDesignation(customDesignation);

      // WHEN — "de-DE" selects by language, "custom-filter" selects via custom hook
      Set<TestDesignation> result =
          customManager.selectDesignations(List.of("de-DE", CUSTOM_FILTER_TOKEN));

      // THEN
      assertThat(result)
          .as("Should contain both language-matched and custom-criteria-matched designations")
          .containsExactlyInAnyOrder(germanDesignation, customDesignation);
    }

    @Test
    @DisplayName("Should invoke hook for each param value independently")
    void shouldCallHookForEachParamValue() {
      // GIVEN
      CustomCriteriaManager customManager = new CustomCriteriaManager();

      TestDesignation customDesignation =
          createDesignation(DesignationSource.CS, "en-US", CUSTOM_FILTER_TOKEN + ":one", null);
      customManager.addDesignation(customDesignation);

      // WHEN — two param values: one unrecognized, one matching the custom hook
      Set<TestDesignation> result =
          customManager.selectDesignations(List.of("unrecognized-token", CUSTOM_FILTER_TOKEN));

      // THEN — only the custom-filter value should produce results
      assertThat(result)
          .as("Hook should be called per param value; only matching token should produce results")
          .containsExactly(customDesignation);
    }

    @Test
    @DisplayName("Should invoke hook for pipe-containing param values as well")
    void shouldCallHookForPipeParamValues() {
      // GIVEN
      CustomCriteriaManager customManager = new CustomCriteriaManager();

      TestDesignation customDesignation =
          createDesignation(DesignationSource.CS, "en-US", CUSTOM_FILTER_TOKEN + ":piped", null);
      customManager.addDesignation(customDesignation);

      // WHEN — a pipe-containing param value that the custom hook recognizes
      Set<TestDesignation> result =
          customManager.selectDesignations(List.of(CUSTOM_SYSTEM + "|" + CUSTOM_FILTER_TOKEN));

      // THEN — the hook should still be invoked and return the matching designation
      assertThat(result)
          .as("Hook should be called even for pipe-containing param values")
          .containsExactly(customDesignation);
    }
  }
}
