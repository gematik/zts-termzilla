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

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;

public class CodeSystemLookupTestResourceGenerator {

  // ==================================================================================================================
  // URL / version / language constants
  // ==================================================================================================================

  public static final String DEFAULT_CS_URL_PREFIX = "http://example.org/fhir/CodeSystem/";

  /** Canonical URL of the CodeSystem used for property-type tests. */
  public static final String CS1_URL = DEFAULT_CS_URL_PREFIX + "CS1";

  /** Version of {@link #CS1_URL} used in tests. */
  public static final String CS1_VERSION = "2025";

  /** Language of the CodeSystem used for property-type tests. */
  public static final String CS1_LANGUAGE = "de-DE";

  /** Code of the single concept in {@link #CS1_URL} that carries all supported property types. */
  public static final String CS1_CONCEPT_CODE = "A03";

  /** Display value of {@link #CS1_CONCEPT_CODE} as stored in the CodeSystem. */
  public static final String CS1_CONCEPT_DISPLAY = "A03 Display";

  /** Definition text of {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_CONCEPT_DEFINITION = "A03 Definition";

  /** Expected value of the {@code propCodeType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_PROP_CODE_VALUE = "codeTypeValue";

  /** System URL of the {@code propCodingType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_PROP_CODING_SYSTEM = "http://example.org/coding-system";

  /** Code of the {@code propCodingType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_PROP_CODING_CODE = "codingCode";

  /** Display text of the {@code propCodingType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_PROP_CODING_DISPLAY = "Coding Display";

  /** Expected value of the {@code propStringType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final String CS1_PROP_STRING_VALUE = "stringTypeValue";

  /** Expected value of the {@code propBooleanType} property on {@link #CS1_CONCEPT_CODE}. */
  public static final boolean CS1_PROP_BOOLEAN_VALUE = true;

  /**
   * Expected integer value of the {@code propIntegerType} property on {@link #CS1_CONCEPT_CODE}.
   */
  public static final int CS1_PROP_INTEGER_VALUE = 42;

  /**
   * Expected date-time value of the {@code propDateTimeType} property on {@link #CS1_CONCEPT_CODE}.
   */
  public static final String CS1_PROP_DATETIME_VALUE = "2026-02-28";

  /**
   * Expected decimal value of the {@code propDecimalType} property on {@link #CS1_CONCEPT_CODE}.
   */
  public static final BigDecimal CS1_PROP_DECIMAL_VALUE = new BigDecimal("123.45");

  // ------------------------------------------------------------------------------------------------------------------

  /**
   * Canonical URL of the CodeSystem used for multi-language designation / display-language tests.
   */
  public static final String CS2_URL = DEFAULT_CS_URL_PREFIX + "CS2";

  /** Version of {@link #CS2_URL} used in tests. */
  public static final String CS2_VERSION = "2025";

  /** Language of the CodeSystem used for designation tests. */
  public static final String CS2_LANGUAGE = "de-DE";

  /** Code of the concept in {@link #CS2_URL} that carries multi-language designations. */
  public static final String CS2_CONCEPT_CODE = "B01";

  /** German display value (= CS default language) for {@link #CS2_CONCEPT_CODE}. */
  public static final String CS2_CONCEPT_DISPLAY_DE = "B01 Display Deutsch";

  /** English designation value for {@link #CS2_CONCEPT_CODE}. */
  public static final String CS2_CONCEPT_DISPLAY_EN = "B01 Display English";

  /** French designation value for {@link #CS2_CONCEPT_CODE}. */
  public static final String CS2_CONCEPT_DISPLAY_FR = "B01 Affichage";

  // ------------------------------------------------------------------------------------------------------------------

  /** Canonical URL of the CodeSystem used for case-sensitivity tests. */
  public static final String CS3_URL = DEFAULT_CS_URL_PREFIX + "CS3";

  /** Version of {@link #CS3_URL} used in tests. */
  public static final String CS3_VERSION = "2025";

  /** Language of the CodeSystem used for case-sensitivity tests. */
  public static final String CS3_LANGUAGE = "de-DE";

  /** Code of the single concept in {@link #CS3_URL}. */
  public static final String CS3_CONCEPT_CODE = "C01";

  /** Display value of {@link #CS3_CONCEPT_CODE} as stored in the CodeSystem. */
  public static final String CS3_CONCEPT_DISPLAY = "C01 Display";

  // ==================================================================================================================
  // Factory methods
  // ==================================================================================================================

  /**
   * Creates a CodeSystem that contains a single concept with one property for each supported
   * property type: {@code code}, {@code Coding}, {@code string}, {@code boolean}, {@code integer},
   * {@code dateTime}, and {@code decimal}.
   *
   * <p>The CodeSystem is identified by {@link #CS1_URL} / {@link #CS1_VERSION}, uses {@link
   * #CS1_LANGUAGE} as its language, and contains exactly one concept ({@link #CS1_CONCEPT_CODE}).
   */
  public static CodeSystem createCodeSystemWithAllPropertyTypes() {
    CodeSystem cs = createCodeSystem("CS1", CS1_VERSION, CS1_LANGUAGE);

    ConceptDefinitionComponent concept = createConcept(CS1_CONCEPT_CODE, CS1_LANGUAGE);
    concept.addProperty().setCode("propCodeType").setValue(new CodeType(CS1_PROP_CODE_VALUE));
    concept
        .addProperty()
        .setCode("propCodingType")
        .setValue(
            new Coding(CS1_PROP_CODING_SYSTEM, CS1_PROP_CODING_CODE, CS1_PROP_CODING_DISPLAY));
    concept.addProperty().setCode("propStringType").setValue(new StringType(CS1_PROP_STRING_VALUE));
    concept
        .addProperty()
        .setCode("propBooleanType")
        .setValue(new BooleanType(CS1_PROP_BOOLEAN_VALUE));
    concept
        .addProperty()
        .setCode("propIntegerType")
        .setValue(new IntegerType(CS1_PROP_INTEGER_VALUE));
    concept
        .addProperty()
        .setCode("propDateTimeType")
        .setValue(new DateTimeType(CS1_PROP_DATETIME_VALUE));
    concept
        .addProperty()
        .setCode("propDecimalType")
        .setValue(new DecimalType(CS1_PROP_DECIMAL_VALUE));

    cs.addConcept(concept);
    return cs;
  }

  /**
   * Creates a case-sensitive CodeSystem that contains a single concept without designations.
   *
   * <p>The CodeSystem is identified by {@link #CS3_URL} / {@link #CS3_VERSION}, uses {@link
   * #CS3_LANGUAGE} as its language, has {@code caseSensitive} set to {@code true}, and contains
   * exactly one concept ({@link #CS3_CONCEPT_CODE}).
   */
  public static CodeSystem createCaseSensitiveCodeSystem() {
    CodeSystem cs = createCodeSystem("CS3", CS3_VERSION, CS3_LANGUAGE);
    cs.setCaseSensitive(true);
    ConceptDefinitionComponent concept = new ConceptDefinitionComponent();
    concept.setCode(CS3_CONCEPT_CODE);
    concept.setDisplay(CS3_CONCEPT_DISPLAY);
    cs.addConcept(concept);
    return cs;
  }

  /**
   * Creates a CodeSystem that contains a single concept with designations in three languages
   * ({@code de-DE}, {@code en}, {@code fr}).
   *
   * <p>The CodeSystem is identified by {@link #CS2_URL} / {@link #CS2_VERSION}, uses {@link
   * #CS2_LANGUAGE} as its language, and contains exactly one concept ({@link #CS2_CONCEPT_CODE}).
   */
  public static CodeSystem createCodeSystemWithMultiLanguageDesignations() {
    CodeSystem cs = createCodeSystem("CS2", CS2_VERSION, CS2_LANGUAGE);

    ConceptDefinitionComponent concept = new ConceptDefinitionComponent();
    concept.setCode(CS2_CONCEPT_CODE);
    concept.setDisplay(CS2_CONCEPT_DISPLAY_DE);
    concept.setDefinition("B01 Definition");

    addDisplayDesignation(concept, "de-DE", CS2_CONCEPT_DISPLAY_DE);
    addDisplayDesignation(concept, "en", CS2_CONCEPT_DISPLAY_EN);
    addDisplayDesignation(concept, "fr", CS2_CONCEPT_DISPLAY_FR);

    cs.addConcept(concept);
    return cs;
  }

  // ==================================================================================================================
  // Private helpers
  // ==================================================================================================================

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

  private static void addDisplayDesignation(
      ConceptDefinitionComponent concept, String language, String value) {
    CodeSystem.ConceptDefinitionDesignationComponent d = concept.addDesignation();
    d.setLanguage(language);
    d.setUse(
        new Coding(
            "http://terminology.hl7.org/CodeSystem/designation-usage", "display", "Display"));
    d.setValue(value);
  }
}
