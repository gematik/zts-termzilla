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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_SOURCE_URI;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_CANONICAL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_SYSTEM;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptMapFieldNames.NAME_TARGET_URI;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.utilities.CanonicalPair;

/**
 * Field generator for FHIR ConceptMap resources. Creates Lucene document fields based on ConceptMap
 * search parameters defined in the FHIR specification.
 */
@Slf4j
public class ConceptMapFieldGenerator extends MetadataResourceFieldGenerator<ConceptMap> {

  @Override
  public void createFields(Document document, ConceptMap conceptMap, Field.Store fieldStore) {
    super.createFields(document, conceptMap, fieldStore);
    indexSourceAndTargetUriAndCanonical(document, conceptMap, fieldStore);
    indexGroupSourceAndTarget(document, conceptMap, fieldStore);
  }

  @Override
  protected void appendAdditionalContentFields(StringBuilder content, ConceptMap conceptMap) {
    // Add CanonicalResource-specific fields
    appendIfPresent(content, conceptMap.hasPurpose(), conceptMap::getPurpose);
    appendIfPresent(content, conceptMap.hasCopyright(), conceptMap::getCopyright);
  }

  /** Indexes source and target system fields from ConceptMap groups. */
  private void indexGroupSourceAndTarget(
      Document document, ConceptMap conceptMap, Field.Store fieldStore) {
    SourceTargetCanonicalPairs canonicalPairs = getConceptMapGroupCanonicalPairs(conceptMap);

    // Add missing indicators if no source or target pairs exist
    addInformationMissingFieldToDocument(
        document, NAME_SOURCE_SYSTEM, canonicalPairs.sourcePairs().isEmpty());
    addInformationMissingFieldToDocument(
        document, NAME_TARGET_SYSTEM, canonicalPairs.targetPairs().isEmpty());

    // SEARCHPARAM: source-system
    // ELEMENT: ConceptMap.group.source [0..1] (uri)
    indexCanonicalPairs(document, canonicalPairs.sourcePairs(), NAME_SOURCE_SYSTEM, fieldStore);

    // SEARCHPARAM: target-system
    // ELEMENT: ConceptMap.group.target [0..1] (uri)
    indexCanonicalPairs(document, canonicalPairs.targetPairs(), NAME_TARGET_SYSTEM, fieldStore);
  }

  /** Indexes a set of canonical pairs, avoiding duplicates. */
  private void indexCanonicalPairs(
      Document document,
      Set<CanonicalPair> canonicalPairs,
      String searchParam,
      Field.Store fieldStore) {
    Set<String> indexedValues = new HashSet<>();

    for (CanonicalPair pair : canonicalPairs) {
      String url = pair.getUrl();

      // Index the URL only if not already indexed
      if (indexedValues.add(url)) {
        addCanonicalReferenceParamFieldToDocument(document, searchParam, url, fieldStore, false);
      }

      // Index URL and version if version exists
      if (pair.hasNonEmptyVersion()) {
        String urlAndVersion = url + "|" + pair.getVersion();
        if (indexedValues.add(urlAndVersion)) {
          addCanonicalReferenceParamFieldToDocument(
              document, searchParam, urlAndVersion, fieldStore, false);
        }
      }
    }
  }

  /** Indexes source and target URI and canonical fields from the ConceptMap root elements. */
  private void indexSourceAndTargetUriAndCanonical(
      Document document, ConceptMap conceptMap, Field.Store fieldStore) {
    // SEARCHPARAM: source-uri (reference)
    // ELEMENT: ConceptMap.sourceUri [0..1] (uri)
    // SEARCHPARAM: source (reference)
    // ELEMENT: ConceptMap.sourceCanonical [0..1] (canonical)
    SourceTargetIndexParams sourceParams =
        new SourceTargetIndexParams(
            conceptMap.hasSourceUriType(),
            conceptMap.hasSourceCanonicalType(),
            conceptMap::getSourceUriType,
            conceptMap::getSourceCanonicalType,
            NAME_SOURCE_URI,
            NAME_SOURCE_CANONICAL);
    indexSourceOrTarget(document, sourceParams, fieldStore);

    // SEARCHPARAM: target-uri (reference)
    // ELEMENT: ConceptMap.targetUri [0..1] (uri)
    // SEARCHPARAM: target (reference)
    // ELEMENT: ConceptMap.targetCanonical [0..1] (canonical)
    SourceTargetIndexParams targetParams =
        new SourceTargetIndexParams(
            conceptMap.hasTargetUriType(),
            conceptMap.hasTargetCanonicalType(),
            conceptMap::getTargetUriType,
            conceptMap::getTargetCanonicalType,
            NAME_TARGET_URI,
            NAME_TARGET_CANONICAL);
    indexSourceOrTarget(document, targetParams, fieldStore);
  }

  /**
   * Indexes either source or target URI and canonical fields. Handles both URI and canonical types,
   * indexing them under both search parameters for flexibility.
   */
  private void indexSourceOrTarget(
      Document document, SourceTargetIndexParams params, Field.Store fieldStore) {
    // Add missing field only if both are missing
    boolean hasUriOrCanonical = params.hasUriType() || params.hasCanonicalType();
    addInformationMissingFieldToDocument(document, params.uriSearchParam(), !hasUriOrCanonical);
    addInformationMissingFieldToDocument(
        document, params.canonicalSearchParam(), !hasUriOrCanonical);

    // Process URI type (cannot have a version)
    if (params.hasUriType()) {
      UriType uri = params.uriTypeSupplier().get();
      addBothUriAndCanonical(
          document,
          uri.getValue(),
          params.uriSearchParam(),
          params.canonicalSearchParam(),
          fieldStore);
    }

    // Process Canonical type
    if (params.hasCanonicalType()) {
      indexCanonicalType(
          document,
          params.canonicalTypeSupplier().get(),
          params.canonicalSearchParam(),
          params.uriSearchParam(),
          fieldStore);
    }
  }

  /**
   * Indexes a canonical type, including both the full canonical value and the URI part separately
   * if versioned.
   */
  private void indexCanonicalType(
      Document document,
      CanonicalType canonical,
      String canonicalSearchParam,
      String uriSearchParam,
      Field.Store fieldStore) {
    CanonicalPair canonicalPair = new CanonicalPair(canonical.getValue());

    // Add full canonical value
    addBothUriAndCanonical(
        document, canonical.getValue(), canonicalSearchParam, uriSearchParam, fieldStore);

    // Also index the URI part separately if a version exists
    if (canonicalPair.hasNonEmptyVersion()) {
      String uriPart = canonicalPair.getUrl();
      addBothUriAndCanonical(document, uriPart, canonicalSearchParam, uriSearchParam, fieldStore);
    }
  }

  /**
   * Adds a value to both primary and secondary search parameters. This allows searching by either
   * parameter for the same value.
   */
  private void addBothUriAndCanonical(
      Document document,
      String value,
      String primaryParam,
      String secondaryParam,
      Field.Store fieldStore) {
    addCanonicalReferenceParamFieldToDocument(document, primaryParam, value, fieldStore, false);
    addCanonicalReferenceParamFieldToDocument(document, secondaryParam, value, fieldStore, false);
  }

  /** Extracts source and target canonical pairs from ConceptMap groups. */
  private SourceTargetCanonicalPairs getConceptMapGroupCanonicalPairs(ConceptMap conceptMap) {
    Set<CanonicalPair> sourcePairs = new HashSet<>();
    Set<CanonicalPair> targetPairs = new HashSet<>();

    if (conceptMap.hasGroup()) {
      conceptMap
          .getGroup()
          .forEach(
              group -> {
                if (group.hasSource()) {
                  String urlAndVersion =
                      group.hasSourceVersion()
                          ? group.getSource() + "|" + group.getSourceVersion()
                          : group.getSource();
                  sourcePairs.add(new CanonicalPair(urlAndVersion));
                }
                if (group.hasTarget()) {
                  String urlAndVersion =
                      group.hasTargetVersion()
                          ? group.getTarget() + "|" + group.getTargetVersion()
                          : group.getTarget();
                  targetPairs.add(new CanonicalPair(urlAndVersion));
                }
              });
    }

    return new SourceTargetCanonicalPairs(sourcePairs, targetPairs);
  }

  @Override
  protected boolean hasIdentifier(ConceptMap conceptMap) {
    return conceptMap.hasIdentifier();
  }

  @Override
  protected List<Identifier> getIdentifiers(ConceptMap conceptMap) {
    // ConceptMap has a single identifier, not a list
    return conceptMap.hasIdentifier()
        ? Collections.singletonList(conceptMap.getIdentifier())
        : Collections.emptyList();
  }

  // ========== Inner records ==========

  /** Container for source or target indexing parameters. */
  private record SourceTargetIndexParams(
      boolean hasUriType,
      boolean hasCanonicalType,
      Supplier<UriType> uriTypeSupplier,
      Supplier<CanonicalType> canonicalTypeSupplier,
      String uriSearchParam,
      String canonicalSearchParam) {}

  /** Container for source and target canonical pairs extracted from ConceptMap groups. */
  public record SourceTargetCanonicalPairs(
      Set<CanonicalPair> sourcePairs, Set<CanonicalPair> targetPairs) {}
}
