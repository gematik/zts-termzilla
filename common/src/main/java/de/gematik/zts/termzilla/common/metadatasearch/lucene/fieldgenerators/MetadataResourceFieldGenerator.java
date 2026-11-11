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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_CONTENT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_DATE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_DESCRIPTION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_IDENTIFIER;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_NAME;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_PUBLISHER;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_STATUS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_TITLE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.TerminologyResourceFieldNames.NAME_VERSION;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MetadataResource;

/**
 * Abstract base class for FHIR terminology resource field generators. Provides common functionality
 * for indexing CodeSystem, ValueSet, and ConceptMap resources.
 *
 * @param <T> the FHIR resource type (must extend MetadataResource)
 */
@Slf4j
public abstract class MetadataResourceFieldGenerator<T extends MetadataResource>
    extends DomainResourceFieldGenerator<T> {

  @Override
  public void createFields(Document document, T resource, Field.Store fieldStore) {
    super.createFields(document, resource, fieldStore);

    indexBasicMetadataFields(document, resource, fieldStore);
    indexIdentifierField(document, resource, fieldStore);
    indexContentField(document, resource, fieldStore);
  }

  /**
   * Indexes basic metadata fields common to all terminology resources. Subclasses can override this
   * to add resource-specific fields.
   *
   * @param document the Lucene document
   * @param resource the terminology resource
   * @param fieldStore the field storage strategy
   */
  protected void indexBasicMetadataFields(Document document, T resource, Field.Store fieldStore) {
    // SEARCHPARAM: date (date)
    // ELEMENT: Resource.date [0..1] (dateTime)
    addFieldWithMissingCheck(
        document,
        NAME_DATE,
        resource.hasDate(),
        () ->
            ResourceFieldGeneratorHelper.addDateTimeDateParamFieldToDocument(
                document, NAME_DATE, resource.getDateElement()));

    // SEARCHPARAM: description (string)
    // ELEMENT: Resource.description [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_DESCRIPTION,
        resource.hasDescription(),
        () ->
            ResourceFieldGeneratorHelper.addStringParamFieldToDocument(
                document, NAME_DESCRIPTION, resource.getDescription(), fieldStore));

    // SEARCHPARAM: name (string)
    // ELEMENT: Resource.name [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_NAME,
        resource.hasName(),
        () ->
            ResourceFieldGeneratorHelper.addStringParamFieldToDocument(
                document, NAME_NAME, resource.getName(), fieldStore));

    // SEARCHPARAM: publisher (string)
    // ELEMENT: Resource.publisher [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_PUBLISHER,
        resource.hasPublisher(),
        () ->
            ResourceFieldGeneratorHelper.addStringParamFieldToDocument(
                document, NAME_PUBLISHER, resource.getPublisher(), fieldStore));

    // SEARCHPARAM: status (token)
    // ELEMENT: Resource.status [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_STATUS,
        resource.hasStatus(),
        () ->
            ResourceFieldGeneratorHelper.addCodeTokenParamFieldToDocument(
                document,
                NAME_STATUS,
                "http://hl7.org/fhir/publication-status",
                resource.getStatus().toCode(),
                fieldStore));

    // SEARCHPARAM: title (string)
    // ELEMENT: Resource.title [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_TITLE,
        resource.hasTitle(),
        () ->
            ResourceFieldGeneratorHelper.addStringParamFieldToDocument(
                document, NAME_TITLE, resource.getTitle(), fieldStore));

    // SEARCHPARAM: url (uri)
    // ELEMENT: Resource.url [1..1] (uri)
    addFieldWithMissingCheck(
        document,
        NAME_URL,
        resource.hasUrl(),
        () ->
            ResourceFieldGeneratorHelper.addUriParamFieldToDocument(
                document, NAME_URL, resource.getUrl(), fieldStore));

    // SEARCHPARAM: version (string)
    // ELEMENT: Resource.version [0..1] (string)
    addFieldWithMissingCheck(
        document,
        NAME_VERSION,
        resource.hasVersionElement(),
        () ->
            ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(
                document, NAME_VERSION, resource.getVersion(), false));

    indexCommonTodoFields(document, resource, fieldStore);
  }

  /**
   * Placeholder for common TODO fields across all terminology resources. Documents unimplemented
   * search parameters.
   *
   * @param document the Lucene document
   * @param resource the terminology resource
   * @param fieldStore the field storage strategy
   */
  protected void indexCommonTodoFields(Document document, T resource, Field.Store fieldStore) {
    // SEARCHPARAM: context (token)
    // ELEMENT: (Resource.useContext.value as CodeableConcept)
    // TODO: Implementation missing

    // SEARCHPARAM: context-quantity (quantity)
    // ELEMENT: (Resource.useContext.value as Quantity) | (Resource.useContext.value as Range)
    // TODO: Implementation missing

    // SEARCHPARAM: context-type (token)
    // ELEMENT: Resource.useContext.code
    // TODO: Implementation missing

    // SEARCHPARAM: context-type-quantity (composite)
    // ELEMENT: On Resource.useContext: context-type: code context-quantity: value.as(Quantity) |
    // value.as(Range)
    // TODO: Implementation missing

    // SEARCHPARAM: context-type-value (composite)
    // ELEMENT: On Resource.useContext: context-type: code context: value.as(CodeableConcept)
    // TODO: Implementation missing

    // SEARCHPARAM: jurisdiction (token)
    // ELEMENT: Resource.jurisdiction [0..*] (CodeableConcept)
    // TODO: Implementation missing
  }

  /**
   * Indexes the identifier field with system and value. Handles both single identifier (ConceptMap)
   * and multiple identifiers (CodeSystem, ValueSet).
   *
   * @param document the Lucene document
   * @param resource the terminology resource
   * @param fieldStore the field storage strategy
   */
  protected void indexIdentifierField(Document document, T resource, Field.Store fieldStore) {
    // SEARCHPARAM: identifier (token)
    // ELEMENT: Resource.identifier [0..*] (Identifier)
    boolean hasIdentifier = hasIdentifier(resource);

    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
        document, NAME_IDENTIFIER, !hasIdentifier);

    if (hasIdentifier) {
      List<Identifier> identifiers = getIdentifiers(resource);
      for (Identifier identifier : identifiers) {
        ResourceFieldGeneratorHelper.addIdentifierTokenParamFieldToDocument(
            document,
            NAME_IDENTIFIER,
            true,
            identifier.getSystem(),
            identifier.getValue(),
            fieldStore);
      }
    }
  }

  /**
   * Indexes the content field for full-text search. Aggregates selected metadata fields into a
   * single searchable content field.
   *
   * @param document the Lucene document
   * @param resource the terminology resource
   * @param fieldStore the field storage strategy
   */
  protected void indexContentField(Document document, T resource, Field.Store fieldStore) {
    // SEARCHPARAM: _content (string)
    // ELEMENT: [Resource-Content]
    // Note: For initial implementation, only selected fields are included in the content field

    String content = buildContentString(resource);

    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
        document, NAME_CONTENT, StringUtils.isEmpty(content));

    if (!StringUtils.isEmpty(content)) {
      ResourceFieldGeneratorHelper.addStringParamFieldToDocument(
          document, NAME_CONTENT, content, fieldStore);
    }
  }

  /**
   * Builds a concatenated content string from various resource fields for full-text search. Uses
   * template method pattern to allow subclasses to add additional fields.
   *
   * @param resource the terminology resource
   * @return the concatenated content string
   */
  protected String buildContentString(T resource) {
    StringBuilder content = new StringBuilder();

    // Common fields for all metadata resources
    appendIfPresent(content, resource.hasUrl(), resource::getUrl);
    appendIfPresent(content, resource.hasVersion(), resource::getVersion);
    appendIfPresent(content, resource.hasName(), resource::getName);
    appendIfPresent(content, resource.hasTitle(), resource::getTitle);
    appendIfPresent(content, resource.hasDescription(), resource::getDescription);
    appendIfPresent(content, resource.hasPublisher(), resource::getPublisher);

    // Allow subclasses to add additional fields (e.g., purpose, copyright for CanonicalResource)
    appendAdditionalContentFields(content, resource);

    return content.toString().trim();
  }

  /**
   * Hook method for subclasses to append additional fields to the content string. Default
   * implementation does nothing. Subclasses should override this to add resource-specific fields
   * like purpose and copyright.
   *
   * @param content the StringBuilder to append to
   * @param resource the terminology resource
   */
  protected void appendAdditionalContentFields(StringBuilder content, T resource) {
    // Override in subclasses if needed
  }

  /**
   * Appends a value to the StringBuilder if present.
   *
   * @param builder the StringBuilder to append to
   * @param hasValue whether the value is present
   * @param valueSupplier supplier for the value
   */
  protected void appendIfPresent(
      StringBuilder builder, boolean hasValue, Supplier<String> valueSupplier) {
    if (hasValue) {
      builder.append(valueSupplier.get()).append(" ");
    }
  }

  /**
   * Adds a field to the document with a missing information indicator.
   *
   * @param document the Lucene document
   * @param fieldName the field name
   * @param hasValue whether the value is present
   * @param addFieldAction action to add the field if value is present
   */
  protected void addFieldWithMissingCheck(
      Document document, String fieldName, boolean hasValue, Runnable addFieldAction) {
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
        document, fieldName, !hasValue);
    if (hasValue) {
      addFieldAction.run();
    }
  }

  // ========== Abstract methods for identifier handling ==========

  /**
   * Checks if the resource has identifier(s). ConceptMap has a single identifier, while CodeSystem
   * and ValueSet have lists.
   *
   * @param resource the terminology resource
   * @return true if the resource has at least one identifier
   */
  protected abstract boolean hasIdentifier(T resource);

  /**
   * Gets the list of identifiers from the resource. ConceptMap returns a single-element list, while
   * CodeSystem and ValueSet return their lists.
   *
   * @param resource the terminology resource
   * @return list of identifiers (empty list if none)
   */
  protected abstract List<Identifier> getIdentifiers(T resource);
}
