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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper.addUriParamFieldToDocument;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ValueSetFieldNames.NAME_EXPANSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ValueSetFieldNames.NAME_REFERENCE;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.CanonicalPair;

/**
 * Field generator for FHIR ValueSet resources. Creates Lucene document fields based on ValueSet
 * search parameters defined in the FHIR specification.
 */
@Slf4j
public class ValueSetFieldGenerator extends MetadataResourceFieldGenerator<ValueSet> {

  @Override
  public void createFields(Document document, ValueSet valueSet, Field.Store fieldStore) {
    super.createFields(document, valueSet, fieldStore);

    indexExpansionField(document, valueSet, fieldStore);
    indexReferenceField(document, valueSet, fieldStore);

    // SEARCHPARAM: code (token)
    // ELEMENT: ValueSet.expansion.contains.code
    //          ValueSet.compose.include.concept.code
    // Note: Code search is delegated to specific implementations as it can be better mapped to
    // native models.
  }

  @Override
  protected void appendAdditionalContentFields(StringBuilder content, ValueSet valueSet) {
    // Add CanonicalResource-specific fields
    appendIfPresent(content, valueSet.hasPurpose(), valueSet::getPurpose);
    appendIfPresent(content, valueSet.hasCopyright(), valueSet::getCopyright);
  }

  /** Indexes the expansion identifier field specific to ValueSet. */
  private void indexExpansionField(Document document, ValueSet valueSet, Field.Store fieldStore) {
    // SEARCHPARAM: expansion (uri)
    // ELEMENT: ValueSet.expansion.identifier [0..1] (uri)
    boolean hasExpansionIdentifier =
        valueSet.hasExpansion() && valueSet.getExpansion().hasIdentifier();

    addFieldWithMissingCheck(
        document,
        NAME_EXPANSION,
        hasExpansionIdentifier,
        () ->
            addUriParamFieldToDocument(
                document, NAME_EXPANSION, valueSet.getExpansion().getIdentifier(), fieldStore));
  }

  /** Indexes the reference field containing code systems included or excluded in the value set. */
  private void indexReferenceField(Document document, ValueSet valueSet, Field.Store fieldStore) {
    // SEARCHPARAM: reference (uri) - A code system included or excluded in the value set or an
    // imported value set
    // ELEMENT: ValueSet.compose.include.system [0..1] (uri)
    // ELEMENT: ValueSet.compose.exclude.system [0..1] (uri)
    Set<CanonicalPair> canonicalPairs = getIncludeExcludeCanonicalPairs(valueSet);

    addInformationMissingFieldToDocument(document, NAME_REFERENCE, canonicalPairs.isEmpty());

    if (!canonicalPairs.isEmpty()) {
      indexCanonicalPairs(document, canonicalPairs, NAME_REFERENCE, fieldStore);
    }
  }

  /** Indexes canonical pairs (system URLs with optional versions). */
  private void indexCanonicalPairs(
      Document document,
      Set<CanonicalPair> canonicalPairs,
      String fieldName,
      Field.Store fieldStore) {
    for (CanonicalPair canonicalPair : canonicalPairs) {
      // Add system URL
      addCanonicalReferenceParamFieldToDocument(
          document, fieldName, canonicalPair.getUrl(), fieldStore, false);
      // Add URL|version if version is present
      if (canonicalPair.hasNonEmptyVersion()) {
        String urlWithVersion = canonicalPair.getUrl() + "|" + canonicalPair.getVersion();
        addCanonicalReferenceParamFieldToDocument(
            document, fieldName, urlWithVersion, fieldStore, false);
      }
    }
  }

  /** Extracts canonical pairs from ValueSet compose include and exclude components. */
  private Set<CanonicalPair> getIncludeExcludeCanonicalPairs(ValueSet valueSet) {
    if (!valueSet.hasCompose()) {
      return Collections.emptySet();
    }

    ValueSet.ValueSetComposeComponent compose = valueSet.getCompose();

    return Stream.concat(compose.getInclude().stream(), compose.getExclude().stream())
        .filter(ValueSet.ConceptSetComponent::hasSystem)
        .map(this::toCanonicalPair)
        .collect(Collectors.toSet());
  }

  /** Converts a ConceptSetComponent to a CanonicalPair. */
  private CanonicalPair toCanonicalPair(ValueSet.ConceptSetComponent component) {
    String canonical =
        component.hasVersion()
            ? component.getSystem() + "|" + component.getVersion()
            : component.getSystem();
    return CanonicalPair.of(canonical);
  }

  @Override
  protected boolean hasIdentifier(ValueSet valueSet) {
    return valueSet.hasIdentifier();
  }

  @Override
  protected List<Identifier> getIdentifiers(ValueSet valueSet) {
    return valueSet.getIdentifier();
  }
}
