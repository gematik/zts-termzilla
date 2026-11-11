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
package de.gematik.zts.termzilla.common.metadatasearch.lucene;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.FieldGenerator;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
public class FhirModelIndexWriter extends AbstractIndexWriter {

  private final FieldGenerator fieldGenerator;

  public FhirModelIndexWriter(Directory index, IndexWriterConfig.OpenMode openMode) {
    super(index, openMode);
    this.fieldGenerator = new FieldGenerator();
  }

  // ==============================================================================================

  /**
   * Indiziert die Metadaten einer FHIR-Ressource
   *
   * @param resource FHIR-Ressource
   * @return true, wenn die Ressource erfolgreich indiziert wurde, sonst false
   */
  public boolean indexResourceMetadata(Resource resource) {
    return indexResourceMetadata(resource, Field.Store.YES);
  }

  /**
   * Indiziert die Metadaten einer FHIR-Ressource
   *
   * @param resource FHIR-Ressource
   * @param fieldStore Art der Feldspeicherung (YES oder NO)
   * @return true, wenn die Ressource erfolgreich indiziert wurde, sonst false
   */
  public boolean indexResourceMetadata(Resource resource, Field.Store fieldStore) {

    log.debug("Indexing resource of type: {}", resource.getResourceType());

    // Erzeugt ein Lucene-Dokument aus der FHIR-Ressource
    Document document = createResourceMetadataDocument(resource, fieldStore);

    // Dokument zum Index hinzufügen
    try {
      writer.addDocument(document);
      return true;
    } catch (IOException e) {
      log.warn("Error while adding document to IndexWriter.", e);
      return false;
    }
  }

  /**
   * Indiziert eine ConceptDefinitionComponent
   *
   * @param concept ConceptDefinitionComponent
   * @param codeSystem CodeSystem, aus dem die Konzepte stammen
   * @return true, wenn die Ressource erfolgreich indiziert wurde, sonst false
   */
  public boolean indexConcept(
      CodeSystem.ConceptDefinitionComponent concept, CodeSystem codeSystem) {

    // Erzeugt ein Lucene-Dokument aus der FHIR-Ressource
    Document document = createConceptDocument(codeSystem, concept);

    // Dokument zum Index hinzufügen
    try {
      writer.addDocument(document);
      return true;
    } catch (IOException e) {
      log.warn("Error while adding document to IndexWriter.", e);
      return false;
    }
  }

  // ==============================================================================================

  /**
   * Erzeugt ein indizierbares Lucene-Dokument aus einer FHIR-Ressource
   *
   * @param resource FHIR-Ressource
   * @param fieldStore Art der Feldspeicherung (YES oder NO)
   * @return Dokument
   */
  private Document createResourceMetadataDocument(Resource resource, Field.Store fieldStore) {
    // Anlegen des Lucene-Dokuments
    Document document = new Document();

    // Erzeugen der Felder auf Grundlage der übergebenen FHIR-Ressource
    fieldGenerator.createFieldsFromResource(document, resource, fieldStore);

    return document;
  }

  /**
   * Erzeugt ein indizierbares Lucene-Dokument aus einer ConceptDefinitionComponent
   *
   * @param codeSystem
   * @param concept FHIR-Ressource
   * @return Dokument
   */
  private Document createConceptDocument(
      CodeSystem codeSystem, CodeSystem.ConceptDefinitionComponent concept) {
    // Anlegen des Lucene-Dokuments
    Document document = new Document();

    // Erzeugen der Standard-Felder auf Grundlage der übergebenen FHIR-Ressource
    fieldGenerator.createFieldsFromConcept(document, codeSystem, concept);

    return document;
  }
}
