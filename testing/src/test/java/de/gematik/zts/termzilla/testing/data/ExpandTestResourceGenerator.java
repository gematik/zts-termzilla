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
package de.gematik.zts.termzilla.testing.data;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;

public class ExpandTestResourceGenerator {

  // ==================================================================================================================

  public static final String DEFAULT_CS_URL_PREFIX = "http://example.org/fhir/CodeSystem/";
  public static final String DEFAULT_VS_URL_PREFIX = "http://example.org/fhir/ValueSet/";

  public static final String CS_SEX_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-sexreject";
  public static final String VS_SEX_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-sexreject";

  public static final String CS_AGE_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/CodeSystem/icd10gm-agereject";
  public static final String VS_AGE_REJECT_URL =
      "https://terminologien.bfarm.de/fhir/ValueSet/icd10gm-agereject";

  public static final String CS_ICD10GM_URL = "http://fhir.de/CodeSystem/bfarm/icd-10-gm";
  public static final String VS_ICD10GM_URL = "http://fhir.de/ValueSet/bfarm/icd-10-gm";

  // ==================================================================================================================

  public static CodeSystem createCodeSystem_CS1_2025_deDE_deDE() {
    CodeSystem cs = createCodeSystem("CS1", "2025", "de-DE");
    cs.addConcept(createConcept("A01", "de-DE"));
    cs.addConcept(createConcept("A02", "de-DE"));
    cs.addConcept(createConcept("A03", "de-DE"));
    return cs;
  }

  public static CodeSystem createCodeSystem_CS1_2026_deDE_deDE() {
    CodeSystem cs = createCodeSystem("CS1", "2026", "de-DE");
    cs.addConcept(createConcept("A01", "de-DE"));
    cs.addConcept(createConcept("A02", "de-DE"));
    cs.addConcept(createConcept("A03", "de-DE"));
    cs.addConcept(createConcept("A04", "de-DE"));
    return cs;
  }

  public static CodeSystem createCodeSystem_CS2_2026_deDE_deDE() {
    CodeSystem cs = createCodeSystem("CS2", "2026", "de-DE");
    cs.addConcept(createConcept("B01", "de-DE"));
    cs.addConcept(createConcept("B02", "de-DE"));
    cs.addConcept(createConcept("B03", "de-DE"));
    return cs;
  }

  private static CodeSystem createCodeSystem(String id, String version, String csLanguage) {
    CodeSystem cs = new CodeSystem();
    cs.setLanguage(csLanguage);
    cs.setId(id + "-" + version);
    cs.setUrl(DEFAULT_CS_URL_PREFIX + id);
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setVersionNeeded(true);
    cs.setExperimental(false);
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    cs.setVersion(version);
    cs.setName(id + " Name");
    cs.setTitle(id + " Title");
    cs.setDateElement(new DateTimeType("2024-01-01"));
    cs.setDescription(id + " Description");
    cs.setCount(5);

    return cs;
  }

  private static CodeSystem.ConceptDefinitionComponent createConcept(
      String code, String designationLanguage) {
    CodeSystem.ConceptDefinitionComponent cdc = new CodeSystem.ConceptDefinitionComponent();
    cdc.setCode(code);
    cdc.setDisplay(code + " Display");
    cdc.setDefinition(code + " Definition");

    CodeSystem.ConceptDefinitionDesignationComponent dc = cdc.addDesignation();
    dc.setLanguage(designationLanguage);
    dc.setValue(code + " Designation");

    return cdc;
  }

  // ==================================================================================================================

  public static ValueSet createValueSetGeneric(String idPrefix, String version) {
    ValueSet vs = new ValueSet();
    vs.setId(idPrefix + "-" + version);
    vs.setUrl(DEFAULT_VS_URL_PREFIX + idPrefix);
    vs.setVersion(version);
    return vs;
  }

  // ==================================================================================================================

  // 1 include: include[0].system=CS1 include[0].version=2025
  public static ValueSet createValueSet_VS00_A() {
    ValueSet vs = createValueSetGeneric("VS00", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    vs.getCompose().addInclude(include);
    return vs;
  }

  // 1 include: include[0].system=CS1 include[0].version=2026
  public static ValueSet createValueSet_VS00_B() {
    ValueSet vs = createValueSetGeneric("VS00", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    vs.getCompose().addInclude(include);
    return vs;
  }

  // 1 include: include[0].system=CS1
  public static ValueSet createValueSet_VS01() {
    ValueSet vs = createValueSetGeneric("VS01", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(DEFAULT_CS_URL_PREFIX + "CS1");
    vs.getCompose().addInclude(include);
    return vs;
  }

  // 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS1
  // include[1].version=2026
  public static ValueSet createValueSet_VS02() {
    ValueSet vs = createValueSetGeneric("VS02", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    ValueSet.ConceptSetComponent include1 = vs.getCompose().addInclude();
    include1.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    return vs;
  }

  // 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS2
  // include[1].version=2026
  public static ValueSet createValueSet_VS03() {
    ValueSet vs = createValueSetGeneric("VS03", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    ValueSet.ConceptSetComponent include1 = vs.getCompose().addInclude();
    include1.setSystem(DEFAULT_CS_URL_PREFIX + "CS2").setVersion("2026");
    return vs;
  }

  // 1 include (extensional): include[0].system=CS1 include[0].version=2025
  // include[0].concept[0].code=A01 include[0].concept[1].code=A02
  public static ValueSet createValueSet_VS04() {
    ValueSet vs = createValueSetGeneric("VS04", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    include0.addConcept().setCode("A01");
    include0.addConcept().setCode("A02");
    return vs;
  }

  // 1 include (extensional): include[0].system=CS1 include[0].concept[0].code=A01
  public static ValueSet createValueSet_VS05() {
    ValueSet vs = createValueSetGeneric("VS05", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1");
    include0.addConcept().setCode("A01");
    return vs;
  }

  // 1 include (extensional with display): include[0].system=CS1 include[0].version=2025
  // include[0].concept[0].code=A01 include[0].concept[0].display="Custom Display"
  public static ValueSet createValueSet_VS06() {
    ValueSet vs = createValueSetGeneric("VS06", "2025");
    vs.setLanguage("de-DE");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    include0.addConcept().setCode("A01").setDisplay("Custom Display");
    return vs;
  }

  // 1 include + 1 exclude: include[0].system=CS1 include[0].version=2025 exclude[0].system=CS1
  // exclude[0].version=2025 exclude[0].concept[0].code=A02
  public static ValueSet createValueSet_VS07() {
    ValueSet vs = createValueSetGeneric("VS07", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    ValueSet.ConceptSetComponent exclude0 = vs.getCompose().addExclude();
    exclude0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    exclude0.addConcept().setCode("A02");
    return vs;
  }

  // 1 include + 1 exclude: include[0].system=CS1 include[0].version=2026 exclude[0].system=CS1
  // exclude[0].version=2026 exclude[0].concept[0].code=A01 exclude[0].concept[1].code=A04
  public static ValueSet createValueSet_VS08() {
    ValueSet vs = createValueSetGeneric("VS08", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    ValueSet.ConceptSetComponent exclude0 = vs.getCompose().addExclude();
    exclude0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    exclude0.addConcept().setCode("A01");
    exclude0.addConcept().setCode("A04");
    return vs;
  }

  // 2 includes + 1 exclude: include[0].system=CS1 include[0].version=2025 include[1].system=CS1
  // include[1].version=2026 exclude[0].system=CS1 exclude[0].version=2025
  public static ValueSet createValueSet_VS09() {
    ValueSet vs = createValueSetGeneric("VS09", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    ValueSet.ConceptSetComponent include1 = vs.getCompose().addInclude();
    include1.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    ValueSet.ConceptSetComponent exclude0 = vs.getCompose().addExclude();
    exclude0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    return vs;
  }

  // 1 include (intensional with filter '='): include[0].system=CS1 include[0].version=2025
  // include[0].filter[0].property=code include[0].filter[0].op== include[0].filter[0].value=A01
  public static ValueSet createValueSet_VS10() {
    ValueSet vs = createValueSetGeneric("VS10", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    include0.addFilter().setProperty("code").setOp(ValueSet.FilterOperator.EQUAL).setValue("A01");
    return vs;
  }

  // 1 include (intensional with filter regex): include[0].system=CS1 include[0].version=2026
  // include[0].filter[0].property=code include[0].filter[0].op=regex
  // include[0].filter[0].value=A0[12]
  public static ValueSet createValueSet_VS11() {
    ValueSet vs = createValueSetGeneric("VS11", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    include0
        .addFilter()
        .setProperty("code")
        .setOp(ValueSet.FilterOperator.REGEX)
        .setValue("A0[12]");
    return vs;
  }

  // 1 include (valueSet import): include[0].valueSet=VS00_A
  public static ValueSet createValueSet_VS12() {
    ValueSet vs = createValueSetGeneric("VS12", "2025");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.addValueSet(DEFAULT_VS_URL_PREFIX + "VS00" + "|" + "2025");
    return vs;
  }

  // 2 includes (valueSet import + system): include[0].valueSet=VS00_A include[1].system=CS2
  // include[1].version=2026
  public static ValueSet createValueSet_VS13() {
    ValueSet vs = createValueSetGeneric("VS13", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.addValueSet(DEFAULT_VS_URL_PREFIX + "VS00" + "|" + "2025");
    ValueSet.ConceptSetComponent include1 = vs.getCompose().addInclude();
    include1.setSystem(DEFAULT_CS_URL_PREFIX + "CS2").setVersion("2026");
    return vs;
  }

  // 1 include (extensional) + 1 exclude (extensional): include[0].system=CS1
  // include[0].version=2026 include[0].concept[0].code=A01 include[0].concept[1].code=A02
  // include[0].concept[2].code=A03 exclude[0].system=CS1 exclude[0].version=2026
  // exclude[0].concept[0].code=A02
  public static ValueSet createValueSet_VS14() {
    ValueSet vs = createValueSetGeneric("VS14", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    include0.addConcept().setCode("A01");
    include0.addConcept().setCode("A02");
    include0.addConcept().setCode("A03");
    ValueSet.ConceptSetComponent exclude0 = vs.getCompose().addExclude();
    exclude0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    exclude0.addConcept().setCode("A02");
    return vs;
  }

  // 2 includes (mixed): include[0].system=CS1 include[0].version=2025
  // include[0].concept[0].code=A01 include[1].system=CS2 include[1].version=2026
  public static ValueSet createValueSet_VS15() {
    ValueSet vs = createValueSetGeneric("VS15", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2025");
    include0.addConcept().setCode("A01");
    ValueSet.ConceptSetComponent include1 = vs.getCompose().addInclude();
    include1.setSystem(DEFAULT_CS_URL_PREFIX + "CS2").setVersion("2026");
    return vs;
  }

  // 1 include (multiple filters AND): include[0].system=CS1 include[0].version=2026
  // include[0].filter[0].property=code include[0].filter[0].op=regex include[0].filter[0].value=A.*
  // include[0].filter[1].property=code include[0].filter[1].op=regex
  // include[0].filter[1].value=.*[34]
  public static ValueSet createValueSet_VS16() {
    ValueSet vs = createValueSetGeneric("VS16", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    include0.addFilter().setProperty("code").setOp(ValueSet.FilterOperator.REGEX).setValue("A.*");
    include0
        .addFilter()
        .setProperty("code")
        .setOp(ValueSet.FilterOperator.REGEX)
        .setValue(".*[34]");
    return vs;
  }

  // 1 include (valueSet import) + 1 exclude: include[0].valueSet=VS00_B exclude[0].system=CS1
  // exclude[0].version=2026 exclude[0].concept[0].code=A04
  public static ValueSet createValueSet_VS17() {
    ValueSet vs = createValueSetGeneric("VS17", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.addValueSet(DEFAULT_VS_URL_PREFIX + "VS00" + "|" + "2026");
    ValueSet.ConceptSetComponent exclude0 = vs.getCompose().addExclude();
    exclude0.setSystem(DEFAULT_CS_URL_PREFIX + "CS1").setVersion("2026");
    exclude0.addConcept().setCode("A04");
    return vs;
  }

  // 1 include (2 valueSet imports): include[0].valueSet[0]=VS00_B include[0].valueSet[1]=VS08
  public static ValueSet createValueSet_VS18() {
    ValueSet vs = createValueSetGeneric("VS18", "2026");
    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include0 = vs.getCompose().addInclude();
    include0.addValueSet(DEFAULT_VS_URL_PREFIX + "VS00" + "|" + "2026");
    include0.addValueSet(DEFAULT_VS_URL_PREFIX + "VS08" + "|" + "2026");
    return vs;
  }

  // ==================================================================================================================
  // Language Scenario CodeSystems (LCS prefix)
  // ==================================================================================================================

  public static CodeSystem createCodeSystem_LCS00_A() {
    CodeSystem cs = createLanguageCodeSystem("LCS00", "A", "de", null);
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    concept.setDisplay("Müll");
    concept.addDesignation().setValue("Abfall").setLanguage("de");
    cs.addConcept(concept);
    return cs;
  }

  public static CodeSystem createCodeSystem_LCS00_B() {
    CodeSystem cs = createLanguageCodeSystem("LCS00", "B", "de-DE", null);
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    concept.setDisplay("Müll");
    cs.addConcept(concept);
    return cs;
  }

  public static CodeSystem createCodeSystem_LCS00_C() {
    CodeSystem cs = createLanguageCodeSystem("LCS00", "C", "en", null);
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    cs.addConcept(concept);
    return cs;
  }

  public static CodeSystem createCodeSystem_LCSS00_C() {
    CodeSystem cs = createLanguageCodeSystem("LCSS00", "C", "de", "LCS00|C");
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    cs.addConcept(concept);
    concept.addDesignation().setLanguage("de").setValue("Müll");
    return cs;
  }

  public static CodeSystem createCodeSystem_LCS00_D() {
    CodeSystem cs = createLanguageCodeSystem("LCS00", "D", "en-US", null);
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    concept
        .addDesignation()
        .setLanguage("de-DE")
        .setValue("Müll")
        .setUse(
            new Coding("http://terminology.hl7.org/CodeSystem/designation-usage", "display", null));
    concept
        .addDesignation()
        .setLanguage("de-AT")
        .setValue("Mist")
        .setUse(
            new Coding("http://terminology.hl7.org/CodeSystem/designation-usage", "display", null));
    concept
        .addDesignation()
        .setLanguage("en-US")
        .setValue("trash")
        .setUse(
            new Coding("http://terminology.hl7.org/CodeSystem/designation-usage", "display", null));
    concept
        .addDesignation()
        .setLanguage("en-GB")
        .setValue("rubbish")
        .setUse(
            new Coding("http://terminology.hl7.org/CodeSystem/designation-usage", "display", null));
    cs.addConcept(concept);
    return cs;
  }

  public static CodeSystem createCodeSystem_LCSS00_D() {
    CodeSystem cs = createLanguageCodeSystem("LCSS00", "D", "de-DE", "LCS00|D");
    CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
    concept.setCode("M");
    cs.addConcept(concept);
    concept.addDesignation().setLanguage("de-DE").setValue("Müll");
    return cs;
  }

  /**
   * Creates a CodeSystem for language scenario testing.
   *
   * @param urlSuffix the URL suffix (e.g., "LCS0")
   * @param version the version (e.g., "A")
   * @param language the language code or null if no language
   * @param supplements
   * @return a configured CodeSystem
   */
  private static CodeSystem createLanguageCodeSystem(
      String urlSuffix, String version, String language, String supplements) {
    CodeSystem cs = new CodeSystem();
    cs.setId(urlSuffix + "-" + version);
    cs.setUrl(DEFAULT_CS_URL_PREFIX + urlSuffix);
    cs.setVersion(version);
    if (language != null) {
      cs.setLanguage(language);
    }
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    if (supplements != null) {
      cs.setContent(CodeSystem.CodeSystemContentMode.SUPPLEMENT);
      cs.setSupplements(DEFAULT_CS_URL_PREFIX + supplements);
    }
    cs.setName(urlSuffix + " Name");
    cs.setTitle(urlSuffix + " Title");
    return cs;
  }

  // ==================================================================================================================
  // Language Scenario ValueSets (LVS prefix)
  // ==================================================================================================================

  public static ValueSet createValueSet_LVS00_A0() {
    return createLanguageValueSet("LVS00", "A0", null, "LCS00", "A");
  }

  public static ValueSet createValueSet_LVS00_A1() {
    return createLanguageValueSet("LVS00", "A1", "de", "LCS00", "A");
  }

  public static ValueSet createValueSet_LVS00_A2() {
    return createLanguageValueSet("LVS00", "A2", "de-DE", "LCS00", "A");
  }

  public static ValueSet createValueSet_LVS00_B0() {
    return createLanguageValueSet("LVS00", "B0", null, "LCS00", "B");
  }

  public static ValueSet createValueSet_LVS00_B1() {
    return createLanguageValueSet("LVS00", "B1", "de", "LCS00", "B");
  }

  public static ValueSet createValueSet_LVS00_B2() {
    return createLanguageValueSet("LVS00", "B2", "de-DE", "LCS00", "B");
  }

  public static ValueSet createValueSet_LVS00_C1() {
    return createLanguageValueSet("LVS00", "C1", "de", "LCS00", "C");
  }

  public static ValueSet createValueSet_LVS00_D2() {
    ValueSet vs = createLanguageValueSet("LVS00", "D2", "de-DE", "LCS00", "D");
    ValueSet.ConceptSetComponent include = vs.getCompose().getIncludeFirstRep();
    ValueSet.ConceptReferenceComponent concept = include.addConcept();
    concept.setCode("M");
    return vs;
  }

  public static ValueSet createValueSet_LVS00_G2X() {
    ValueSet vs = createLanguageValueSet("LVS00", "G2X", "de-DE", "LCS00", "G");
    ValueSet.ConceptSetComponent include = vs.getCompose().getIncludeFirstRep();
    ValueSet.ConceptReferenceComponent concept = include.addConcept();
    concept.setCode("M");
    concept.setDisplay("Unrat");
    ValueSet.ConceptReferenceDesignationComponent designation = concept.addDesignation();
    designation.setLanguage("es-ES");
    designation.setValue("basura");
    designation.setUse(
        new Coding("http://terminology.hl7.org/CodeSystem/designation-usage", "display", null));
    designation = concept.addDesignation();
    designation.setLanguage("es-ES");
    designation.setValue("inmundicias");

    return vs;
  }

  // ----------------------

  /**
   * Creates an all-concepts ValueSet for language scenario testing.
   *
   * @param urlSuffix the URL suffix for the ValueSet (e.g., "LVS00")
   * @param version the version of the ValueSet (e.g., "A0")
   * @param language the language code or null if no language
   * @param csUrlSuffix the URL suffix of the referenced CodeSystem (e.g., "LCS0")
   * @param csVersion the version of the referenced CodeSystem (e.g., "A")
   * @return a configured ValueSet including all concepts from the specified CodeSystem
   */
  private static ValueSet createLanguageValueSet(
      String urlSuffix, String version, String language, String csUrlSuffix, String csVersion) {
    ValueSet vs = new ValueSet();
    vs.setId(urlSuffix + "-" + version);
    vs.setUrl(DEFAULT_VS_URL_PREFIX + urlSuffix);
    vs.setVersion(version);
    if (language != null) {
      vs.setLanguage(language);
    }
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    vs.setName(urlSuffix + " Name");
    vs.setTitle(urlSuffix + " Title");

    vs.setCompose(new ValueSet.ValueSetComposeComponent());
    ValueSet.ConceptSetComponent include = vs.getCompose().addInclude();
    include.setSystem(DEFAULT_CS_URL_PREFIX + csUrlSuffix);
    include.setVersion(csVersion);

    return vs;
  }
}
