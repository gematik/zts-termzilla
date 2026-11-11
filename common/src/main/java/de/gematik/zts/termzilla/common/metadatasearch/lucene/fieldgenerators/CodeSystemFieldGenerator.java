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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper.addCanonicalReferenceParamFieldToDocument;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper.addCodeTokenParamFieldToDocument;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper.addUriParamFieldToDocument;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_CONTENT_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SUPPLEMENTS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_SYSTEM;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Identifier;

/**
 * Field generator for FHIR CodeSystem resources. Creates Lucene document fields based on CodeSystem
 * search parameters defined in the FHIR specification.
 */
@Slf4j
public class CodeSystemFieldGenerator extends MetadataResourceFieldGenerator<CodeSystem> {

  @Override
  public void createFields(Document document, CodeSystem codeSystem, Field.Store fieldStore) {
    super.createFields(document, codeSystem, fieldStore);
    indexContentModeField(document, codeSystem, fieldStore);
    indexSupplementsField(document, codeSystem, fieldStore);
    indexSystemField(document, codeSystem, fieldStore);

    // SEARCHPARAM: language (token)
    // ELEMENT: CodeSystem.concept.designation.language [0..1] (code)
    // TODO: Implementation missing
  }

  @Override
  protected void appendAdditionalContentFields(StringBuilder content, CodeSystem codeSystem) {
    // Add CanonicalResource-specific fields
    appendIfPresent(content, codeSystem.hasPurpose(), codeSystem::getPurpose);
    appendIfPresent(content, codeSystem.hasCopyright(), codeSystem::getCopyright);
  }

  /** Indexes the content-mode field specific to CodeSystem. */
  private void indexContentModeField(
      Document document, CodeSystem codeSystem, Field.Store fieldStore) {
    // SEARCHPARAM: content-mode (token)
    // ELEMENT: CodeSystem.content [0..1] (token)
    addFieldWithMissingCheck(
        document,
        NAME_CONTENT_MODE,
        codeSystem.hasContent(),
        () ->
            addCodeTokenParamFieldToDocument(
                document,
                NAME_CONTENT_MODE,
                "http://hl7.org/fhir/codesystem-content-mode",
                codeSystem.getContent().toCode(),
                fieldStore));
  }

  /** Indexes the supplements field specific to CodeSystem. */
  private void indexSupplementsField(
      Document document, CodeSystem codeSystem, Field.Store fieldStore) {
    // SEARCHPARAM: supplements (reference)
    // ELEMENT: CodeSystem.supplements [0..1] (reference)
    addFieldWithMissingCheck(
        document,
        NAME_SUPPLEMENTS,
        codeSystem.hasSupplements(),
        () ->
            addCanonicalReferenceParamFieldToDocument(
                document, NAME_SUPPLEMENTS, codeSystem.getSupplements(), fieldStore, true));
  }

  /** Indexes the system field (CodeSystem.url) specific to CodeSystem. */
  private void indexSystemField(Document document, CodeSystem codeSystem, Field.Store fieldStore) {
    // SEARCHPARAM: system (uri)
    // ELEMENT: CodeSystem.url [1..1] (uri)
    // Note: This is the same as URL but indexed under a different search parameter name
    addFieldWithMissingCheck(
        document,
        NAME_SYSTEM,
        codeSystem.hasUrl(),
        () -> addUriParamFieldToDocument(document, NAME_SYSTEM, codeSystem.getUrl(), fieldStore));
  }

  @Override
  protected boolean hasIdentifier(CodeSystem codeSystem) {
    return codeSystem.hasIdentifier();
  }

  @Override
  protected List<Identifier> getIdentifiers(CodeSystem codeSystem) {
    return codeSystem.getIdentifier();
  }
}
