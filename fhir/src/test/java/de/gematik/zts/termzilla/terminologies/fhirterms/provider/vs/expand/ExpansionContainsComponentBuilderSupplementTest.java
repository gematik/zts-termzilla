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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for supplement application in {@link ExpansionContainsComponentBuilder}.
 *
 * <p>Verifies that a CodeSystem supplement enriches the display/designations of an expanded concept
 * without changing membership.
 */
@DisplayName("ExpansionContainsComponentBuilder Supplement Tests")
class ExpansionContainsComponentBuilderSupplementTest {

  private static final String SCOPE = "fhir";
  private static final String BASE_URL = "http://terminology.hl7.org/CodeSystem/data-absent-reason";
  private static final String BASE_VERSION = "2.0.0";
  private static final String SUPPLEMENT_URL =
      "https://terminologien.bfarm.de/fhir/CodeSystem/data-absent-reason-de-de";
  private static final String SUPPLEMENT_VERSION = "0.1.0";

  private FhirTermsInMemoryRepresentation inMemory;
  private ExpansionContainsComponentBuilder builder;

  @BeforeEach
  void setUp() {
    inMemory = Mockito.mock(FhirTermsInMemoryRepresentation.class);
    builder = new ExpansionContainsComponentBuilder(inMemory);
  }

  @Test
  @DisplayName("SUP: Supplement display is used for the requested displayLanguage")
  void supplementDisplayIsUsedForRequestedLanguage() throws Exception {
    // GIVEN: base CodeSystem with English display, supplement with German display
    CodeSystem base = baseCodeSystem();
    CodeSystem supplement = germanSupplement();

    when(inMemory.getResourceByUrlAndVersion(
            eq(CodeSystem.class), eq(SCOPE), eq(BASE_URL), eq(BASE_VERSION), any()))
        .thenReturn(base);

    ConceptBaseInfo concept =
        ConceptBaseInfo.builder()
            .system(BASE_URL)
            .version(BASE_VERSION)
            .code("unknown")
            .active(true)
            .build();

    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .displayLanguageParameter(LanguageParameter.parse("de-DE"))
            .build();

    Map<String, SupplementRequest> supplements =
        Map.of(
            BASE_URL,
            new SupplementRequest(SUPPLEMENT_URL, SUPPLEMENT_VERSION, BASE_URL, supplement));

    // WHEN
    ValueSet.ValueSetExpansionContainsComponent component =
        builder.createExpansionContainsComponent(
            concept, true, List.of(), lsp, new ArrayListValuedHashMap<>(), SCOPE, supplements);

    // THEN: the German supplement display wins for displayLanguage=de-DE
    assertThat(component.getDisplay()).isEqualTo("Unbekannt");
    // and the designation is included
    assertThat(component.getDesignation())
        .anySatisfy(
            d -> {
              assertThat(d.getLanguage()).isEqualTo("de-DE");
              assertThat(d.getValue()).isEqualTo("Unbekannt");
            });
  }

  @Test
  @DisplayName("SUP: parsed designation without use is suitable as display under FHIR R4")
  void parsedSupplementDesignationWithoutUseIsUsedAsDisplay() throws Exception {
    CodeSystem base = baseCodeSystem();
    CodeSystem supplement =
        FhirContext.forR4()
            .newJsonParser()
            .parseResource(
                CodeSystem.class,
                """
                {
                  "resourceType": "CodeSystem",
                  "url": "%s",
                  "version": "%s",
                  "status": "active",
                  "content": "supplement",
                  "supplements": "%s",
                  "language": "de-DE",
                  "concept": [{
                    "code": "unknown",
                    "designation": [{
                      "language": "de-DE",
                      "value": "Unbekannt"
                    }]
                  }]
                }
                """.formatted(SUPPLEMENT_URL, SUPPLEMENT_VERSION, BASE_URL));
    assertThat(supplement.getConceptFirstRep().getDesignationFirstRep().getUse().isEmpty())
        .isTrue();

    when(inMemory.getResourceByUrlAndVersion(
            eq(CodeSystem.class), eq(SCOPE), eq(BASE_URL), eq(BASE_VERSION), any()))
        .thenReturn(base);

    ConceptBaseInfo concept =
        ConceptBaseInfo.builder()
            .system(BASE_URL)
            .version(BASE_VERSION)
            .code("unknown")
            .active(true)
            .build();
    LanguageSelectionParameter languageSelection =
        LanguageSelectionParameter.builder()
            .displayLanguageParameter(LanguageParameter.parse("de-DE"))
            .build();
    Map<String, SupplementRequest> supplements =
        Map.of(
            BASE_URL,
            new SupplementRequest(SUPPLEMENT_URL, SUPPLEMENT_VERSION, BASE_URL, supplement));

    ValueSet.ValueSetExpansionContainsComponent component =
        builder.createExpansionContainsComponent(
            concept,
            true,
            List.of(),
            languageSelection,
            new ArrayListValuedHashMap<>(),
            SCOPE,
            supplements);

    assertThat(component.getDisplay()).isEqualTo("Unbekannt");
  }

  @Test
  @DisplayName("SUP: Base display is used when no supplement matches the requested language")
  void baseDisplayIsUsedWhenSupplementDoesNotMatchLanguage() throws Exception {
    // GIVEN
    CodeSystem base = baseCodeSystem();
    CodeSystem supplement = germanSupplement();

    when(inMemory.getResourceByUrlAndVersion(
            eq(CodeSystem.class), eq(SCOPE), eq(BASE_URL), eq(BASE_VERSION), any()))
        .thenReturn(base);

    ConceptBaseInfo concept =
        ConceptBaseInfo.builder()
            .system(BASE_URL)
            .version(BASE_VERSION)
            .code("unknown")
            .active(true)
            .build();

    // Request a language the supplement does not provide
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .displayLanguageParameter(LanguageParameter.parse("fr"))
            .build();

    Map<String, SupplementRequest> supplements =
        Map.of(
            BASE_URL,
            new SupplementRequest(SUPPLEMENT_URL, SUPPLEMENT_VERSION, BASE_URL, supplement));

    // WHEN
    ValueSet.ValueSetExpansionContainsComponent component =
        builder.createExpansionContainsComponent(
            concept, false, List.of(), lsp, new ArrayListValuedHashMap<>(), SCOPE, supplements);

    // THEN: no display (strict displayLanguage), supplement does not invent one
    assertThat(component.getDisplay()).isNull();
  }

  @Test
  @DisplayName("SUP: Membership is unchanged — supplement does not add codes")
  void supplementDoesNotChangeMembership() throws Exception {
    // GIVEN
    CodeSystem base = baseCodeSystem();
    CodeSystem supplement = germanSupplement();

    when(inMemory.getResourceByUrlAndVersion(
            eq(CodeSystem.class), eq(SCOPE), eq(BASE_URL), eq(BASE_VERSION), any()))
        .thenReturn(base);

    ConceptBaseInfo concept =
        ConceptBaseInfo.builder()
            .system(BASE_URL)
            .version(BASE_VERSION)
            .code("unknown")
            .active(true)
            .build();

    LanguageSelectionParameter lsp = LanguageSelectionParameter.builder().build();

    // WHEN: expand without and with supplement
    ValueSet.ValueSetExpansionContainsComponent withoutSupplement =
        builder.createExpansionContainsComponent(
            concept, false, List.of(), lsp, new ArrayListValuedHashMap<>(), SCOPE);

    ValueSet.ValueSetExpansionContainsComponent withSupplement =
        builder.createExpansionContainsComponent(
            concept,
            false,
            List.of(),
            lsp,
            new ArrayListValuedHashMap<>(),
            SCOPE,
            Map.of(
                BASE_URL,
                new SupplementRequest(SUPPLEMENT_URL, SUPPLEMENT_VERSION, BASE_URL, supplement)));

    // THEN: same system/version/code — membership invariant holds
    assertThat(withSupplement.getSystem()).isEqualTo(withoutSupplement.getSystem());
    assertThat(withSupplement.getVersion()).isEqualTo(withoutSupplement.getVersion());
    assertThat(withSupplement.getCode()).isEqualTo(withoutSupplement.getCode());
  }

  // ----------------------------------------------------------------------------------------------
  // Fixtures
  // ----------------------------------------------------------------------------------------------

  private static CodeSystem baseCodeSystem() {
    CodeSystem cs = new CodeSystem();
    cs.setUrl(BASE_URL);
    cs.setVersion(BASE_VERSION);
    cs.setContent(CodeSystemContentMode.COMPLETE);
    cs.setCaseSensitive(true);
    cs.setLanguage("en");
    cs.addConcept(
        new CodeSystem.ConceptDefinitionComponent().setCode("unknown").setDisplay("Unknown"));
    return cs;
  }

  private static CodeSystem germanSupplement() {
    CodeSystem supplement = new CodeSystem();
    supplement.setUrl(SUPPLEMENT_URL);
    supplement.setVersion(SUPPLEMENT_VERSION);
    supplement.setContent(CodeSystemContentMode.SUPPLEMENT);
    supplement.setSupplements(BASE_URL);
    supplement.setLanguage("de-DE");
    supplement.addConcept(
        new CodeSystem.ConceptDefinitionComponent().setCode("unknown").setDisplay("Unbekannt"));
    return supplement;
  }
}
