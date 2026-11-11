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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.utils.DateUtil;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests für den abstrakten DomainResourceFieldGenerator.
 *
 * <p>Diese Klasse testet die Funktionalität, die alle DomainResources gemeinsam haben, zusätzlich
 * zu den Basis-Feldern aus ResourceFieldGenerator:
 *
 * <ul>
 *   <li>text (_text) - die Narrative
 *   <li>effective (extension) - R5 Backport für effectivePeriod
 * </ul>
 *
 * <p><strong>Hinweis zur Vererbungshierarchie:</strong> <br>
 * DomainResource erweitert Resource um zusätzliche Elemente wie text, contained, extension. In FHIR
 * R4 sind alle Terminologie-Ressourcen DomainResources (CodeSystem, ValueSet, ConceptMap)
 */
@DisplayName("DomainResourceFieldGenerator Tests")
class DomainResourceFieldGeneratorTest {

  /**
   * Konkrete Test-Implementierung des abstrakten DomainResourceFieldGenerator. Wir verwenden
   * CodeSystem, da es eine DomainResource ist.
   */
  private static class TestDomainResourceFieldGenerator
      extends DomainResourceFieldGenerator<CodeSystem> {
    // Keine zusätzliche Implementierung nötig
  }

  private TestDomainResourceFieldGenerator fieldGenerator;

  @BeforeEach
  void setUp() {
    fieldGenerator = new TestDomainResourceFieldGenerator();
  }

  // ========================================================================
  // Helper-Methoden
  // ========================================================================

  private static final String RESOURCE_ID = "test-resource";
  private static final String RESOURCE_SCOPE_1 = "fhir";
  private static final String RESOURCE_SCOPE_2 = "org.terminologies.test-1.0.0";

  private CodeSystem createMinimalDomainResource() {
    CodeSystem cs = new CodeSystem();
    cs.setId(RESOURCE_ID);
    cs.setUserData(NAME_SCOPE, Set.of(RESOURCE_SCOPE_1, RESOURCE_SCOPE_2));
    return cs;
  }

  private String getFieldValue(Document doc, String fieldName) {
    return doc.get(fieldName);
  }

  private long getFieldLongValue(Document doc, String fieldName) {
    return doc.getField(fieldName).numericValue().longValue();
  }

  private boolean hasField(Document doc, String fieldName) {
    return doc.getField(fieldName) != null;
  }

  // ========================================================================
  // TEIL 1: Text/Narrative Tests
  // ========================================================================

  @Nested
  @DisplayName("Text/Narrative (_text)")
  class TextNarrativeTests {
    // TODO: Implementiere Tests für das text Feld. Aktuell verwenden wir dieses Feld nicht aktiv
    //  und auch der Suchparameter ist meines Wissens nach nicht in Gebrauch.
  }

  // ========================================================================
  // TEIL 2: Effective Period Extension Tests (R5 Backport)
  // ========================================================================

  @Nested
  @DisplayName("Effective Period Extension (R5 Backport)")
  class EffectivePeriodTests {

    @Test
    @DisplayName("'effective_missing' Feld wird korrekt indiziert bei fehlenden Meta-Informationen")
    void shouldHandleMissingEffectiveField() {
      // GIVEN: Ressource OHNE effectivePeriod Extension
      CodeSystem resource = createMinimalDomainResource();
      Document doc = new Document();

      // WHEN
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: Es sollte ein _missing Feld für effective geben (und der Wert ist
      // 'true')
      assertTrue(hasField(doc, "effective_missing"), "effective_missing Feld fehlt");
      assertEquals(
          true,
          Boolean.valueOf(getFieldValue(doc, "effective_missing")),
          "effective_missing muss 'true' sein");
    }

    @Test
    @DisplayName("Effective Period Extension wird korrekt indiziert")
    void shouldIndexEffectivePeriodExtension() {
      // WHY: In R5 gibt es CodeSystem.effectivePeriod
      // Für R4 wird dies als Extension abgebildet

      // GIVEN
      CodeSystem resource = createMinimalDomainResource();

      // Füge die effectivePeriod Extension hinzu
      Extension effectivePeriodExt = new Extension();
      effectivePeriodExt.setUrl("http://hl7.org/fhir/StructureDefinition/resource-effectivePeriod");
      Period period = new Period();
      period.setStartElement(new DateTimeType("2025-01-01"));
      period.setEndElement(new DateTimeType("2025-12-31"));
      // End nicht gesetzt -> noch gültig
      effectivePeriodExt.setValue(period);

      resource.addExtension(effectivePeriodExt);
      Document doc = new Document();

      // WHEN
      fieldGenerator.createFields(doc, resource, Field.Store.YES);

      // THEN: Es sollte ein "effective" Feld geben
      assertTrue(
          hasField(doc, "effective_date_period_start"), "'effective_date_period_start' Feld fehlt");
      assertEquals(
          DateUtil.getFirstInstantOfDateTime(new DateTimeType("2025-01-01")),
          getFieldLongValue(doc, "effective_date_period_start"),
          "'effective_date_period_start' hat falschen Wert");

      assertTrue(
          hasField(doc, "effective_date_period_end"), "'effective_date_period_end' Feld fehlt");
      assertEquals(
          DateUtil.getLastInstantOfDateTime(new DateTimeType("2025-12-31")),
          getFieldLongValue(doc, "effective_date_period_end"),
          "'effective_date_period_end' hat falschen Wert");
    }
  }
}
