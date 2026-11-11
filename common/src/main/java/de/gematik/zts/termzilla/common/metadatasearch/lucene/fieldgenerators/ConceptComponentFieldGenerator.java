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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.NAME_PROVISION_MODE;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ConceptComponentFieldGeneratorHelper;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hl7.fhir.r4.model.CodeSystem;

@Slf4j
public class ConceptComponentFieldGenerator {

  /**
   * Erzeugt die Felder für die ConceptDefinitionComponent. Die Erzeugung der Felder orientiert sich
   * primär an den in der Spezifikation definierten Filterkriterien.
   *
   * @param document Lucene-Dokument
   * @param codeSystem
   * @param concept ConceptDefinitionComponent Element
   */
  public void createFields(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    indexMetadataFields(document, codeSystem);
    indexBasicConceptFields(document, codeSystem, concept);
    indexAlternativeCodesAndSynonyms(document, codeSystem, concept);
    indexConceptProperties(document, concept);
  }

  /** Indexes metadata fields (type, scopes, provision mode). */
  private void indexMetadataFields(Document document, CodeSystem codeSystem) {
    // Hauptsuchkriterium, falls wir noch andere Sachen in den Index werfen
    ConceptComponentFieldGeneratorHelper.addTypeFieldToDocument(
        document, "ConceptDefinitionComponent", Field.Store.NO);

    // Scopes hinzufügen
    Set<String> scopeSet =
        (Set<String>) codeSystem.getUserData(ResourceUserData.USERDATA_SCOPE_SET);
    if (scopeSet != null) {
      ConceptComponentFieldGeneratorHelper.addScopeSetToDocument(
          document, scopeSet, Field.Store.NO);
    }

    // Get the provision mode from the resource user data
    ProvisionMode provisionMode =
        (ProvisionMode) codeSystem.getUserData(ResourceUserData.USERDATA_PROVISION_MODE);
    if (provisionMode != null) {
      document.add(new StringField(NAME_PROVISION_MODE, provisionMode.name(), Field.Store.NO));
    } else {
      log.warn("Provision Mode is null for resource of type: {}", codeSystem.fhirType());
    }
  }

  /** Indexes basic concept fields (ID, code, display, definition). */
  private void indexBasicConceptFields(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    // ID hinzufügen
    if (codeSystem.hasIdElement()) {
      ConceptComponentFieldGeneratorHelper.addIdFieldToDocument(
          document, codeSystem.getIdElement().getIdPart(), Field.Store.YES);
    }

    indexCodeField(document, codeSystem, concept);

    // Display hinzufügen
    if (concept.hasDisplay()) {
      ConceptComponentFieldGeneratorHelper.addDisplayFieldToDocument(
          document, concept.getDisplay(), Field.Store.YES);
    }

    // Definition hinzufügen
    if (concept.hasDefinition()) {
      ConceptComponentFieldGeneratorHelper.addDefinitionFieldToDocument(
          document, concept.getDefinition(), Field.Store.YES);
    }
  }

  /** Indexes the code field, including case-insensitive variant if applicable. */
  private void indexCodeField(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    if (!concept.hasCode()) {
      return;
    }

    // Code hinzufügen
    // TODO: Wir müssen unbedingt noch darüber diskutieren, was das erwartete Suchverhalten
    //  bei "caseSensitive=true|false" ist

    // Indizieren des codes als exakte Zeichenfolge
    ConceptComponentFieldGeneratorHelper.addCodeFieldToDocument(
        document, concept.getCode(), Field.Store.YES);

    // Sofern das CodeSystem nicht caseSensitive ist, wird der Code zusätzlich in Kleinbuchstaben
    // indiziert
    if (codeSystem.hasCaseSensitive() && !codeSystem.getCaseSensitive()) {
      ConceptComponentFieldGeneratorHelper.addCodeLowerCaseFieldToDocument(
          document, concept.getCode().toLowerCase(), Field.Store.NO);
    }
  }

  /** Indexes alternative codes and synonyms from CodeSystem user data. */
  private void indexAlternativeCodesAndSynonyms(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    indexAlternativeCodes(document, codeSystem, concept);
    indexSynonyms(document, codeSystem, concept);
  }

  /** Indexes alternative codes if present in CodeSystem user data. */
  private void indexAlternativeCodes(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    // Sofern Alternativcode im CodeSystem hinterlegt sind, verarbeiten wir diese
    Map<String, Set<String>> alternativeCodesMap =
        (Map<String, Set<String>>)
            codeSystem.getUserData(ResourceUserData.USERDATA_CODESYSTEM_ALTCODES);

    if (alternativeCodesMap == null) {
      return;
    }

    // Selektieren der zum aktuellen Code passenden Alternativcodes
    Set<String> alternativeCodeSet = alternativeCodesMap.get(concept.getCode());

    // Indizieren der Alternativcodes
    if (alternativeCodeSet != null) {
      alternativeCodeSet.forEach(
          altCode ->
              ConceptComponentFieldGeneratorHelper.addCodeFieldNoSortToDocument(
                  document, altCode, Field.Store.NO));
    }
  }

  /** Indexes synonyms if present in CodeSystem user data. */
  private void indexSynonyms(
      Document document, CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {

    // Sofern Synonyme im CodeSystem hinterlegt sind, verarbeiten wir diese
    Map<String, Set<String>> synonymMap =
        (Map<String, Set<String>>)
            codeSystem.getUserData(ResourceUserData.USERDATA_CODESYSTEM_SYNONYMS);

    if (synonymMap == null) {
      return;
    }

    Set<String> synonymSet = synonymMap.get(concept.getCode());

    if (synonymSet != null) {
      synonymSet.forEach(
          synonym ->
              ConceptComponentFieldGeneratorHelper.addSynonymFieldToDocument(
                  document, synonym, Field.Store.NO));
    }
  }

  /** Indexes concept properties if present. */
  private void indexConceptProperties(
      Document document, CodeSystem.ConceptDefinitionComponent concept) {

    if (!concept.hasProperty() || concept.getProperty().isEmpty()) {
      return;
    }

    for (CodeSystem.ConceptPropertyComponent property : concept.getProperty()) {
      if (property.hasCode() && property.hasValue()) {
        ConceptComponentFieldGeneratorHelper.addPropertyFieldToDocument(
            document, property.getCode(), property.getValue(), Field.Store.NO);
      }
    }
  }
}
