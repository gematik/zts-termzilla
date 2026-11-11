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

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.UriType;

/**
 * Test data helper class for creating ConceptMap test instances. Provides factory methods for
 * various ConceptMap configurations used in tests.
 */
public class ConceptMapTestDataHelper {

  private static final String DEFAULT_URL = "http://example.org/fhir/ConceptMap/test";
  private static final String DEFAULT_VERSION = "1.0.0";
  private static final String DEFAULT_NAME = "TestConceptMap";
  private static final String DEFAULT_ID = "TestConceptMap";

  /**
   * Creates a minimal ConceptMap with only required fields.
   *
   * @return minimal ConceptMap instance
   */
  public static ConceptMap createMinimalConceptMap() {
    ConceptMap conceptMap = new ConceptMap();
    conceptMap.setId(DEFAULT_ID);
    conceptMap.setUrl(DEFAULT_URL);
    conceptMap.setVersion(DEFAULT_VERSION);
    conceptMap.setName(DEFAULT_NAME);
    conceptMap.setStatus(Enumerations.PublicationStatus.ACTIVE);
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with source URI.
   *
   * @param sourceUri the source URI
   * @return ConceptMap with source URI
   */
  public static ConceptMap createConceptMapWithSourceUri(String sourceUri) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setSource(new UriType(sourceUri));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with source canonical.
   *
   * @param sourceCanonical the source canonical (may include version with |)
   * @return ConceptMap with source canonical
   */
  public static ConceptMap createConceptMapWithSourceCanonical(String sourceCanonical) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setSource(new CanonicalType(sourceCanonical));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with target URI.
   *
   * @param targetUri the target URI
   * @return ConceptMap with target URI
   */
  public static ConceptMap createConceptMapWithTargetUri(String targetUri) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setTarget(new UriType(targetUri));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with target canonical.
   *
   * @param targetCanonical the target canonical (may include version with |)
   * @return ConceptMap with target canonical
   */
  public static ConceptMap createConceptMapWithTargetCanonical(String targetCanonical) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setTarget(new CanonicalType(targetCanonical));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with a group containing source system.
   *
   * @param sourceSystem the source system URL
   * @param sourceVersion the source version (may be null)
   * @return ConceptMap with group source
   */
  public static ConceptMap createConceptMapWithGroupSource(
      String sourceSystem, String sourceVersion) {
    ConceptMap conceptMap = createMinimalConceptMap();
    ConceptMap.ConceptMapGroupComponent group = conceptMap.addGroup();
    group.setSource(sourceSystem);
    if (sourceVersion != null) {
      group.setSourceVersion(sourceVersion);
    }
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with a group containing target system.
   *
   * @param targetSystem the target system URL
   * @param targetVersion the target version (may be null)
   * @return ConceptMap with group target
   */
  public static ConceptMap createConceptMapWithGroupTarget(
      String targetSystem, String targetVersion) {
    ConceptMap conceptMap = createMinimalConceptMap();
    ConceptMap.ConceptMapGroupComponent group = conceptMap.addGroup();
    group.setTarget(targetSystem);
    if (targetVersion != null) {
      group.setTargetVersion(targetVersion);
    }
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with multiple groups containing different source systems.
   *
   * @param sourceSystem1 first source system
   * @param sourceVersion1 first source version (may be null)
   * @param sourceSystem2 second source system
   * @param sourceVersion2 second source version (may be null)
   * @return ConceptMap with multiple group sources
   */
  public static ConceptMap createConceptMapWithMultipleGroupSources(
      String sourceSystem1, String sourceVersion1, String sourceSystem2, String sourceVersion2) {
    ConceptMap conceptMap = createMinimalConceptMap();

    ConceptMap.ConceptMapGroupComponent group1 = conceptMap.addGroup();
    group1.setSource(sourceSystem1);
    if (sourceVersion1 != null) {
      group1.setSourceVersion(sourceVersion1);
    }

    ConceptMap.ConceptMapGroupComponent group2 = conceptMap.addGroup();
    group2.setSource(sourceSystem2);
    if (sourceVersion2 != null) {
      group2.setSourceVersion(sourceVersion2);
    }

    return conceptMap;
  }

  /**
   * Creates a ConceptMap with multiple groups containing different target systems.
   *
   * @param targetSystem1 first target system
   * @param targetVersion1 first target version (may be null)
   * @param targetSystem2 second target system
   * @param targetVersion2 second target version (may be null)
   * @return ConceptMap with multiple group targets
   */
  public static ConceptMap createConceptMapWithMultipleGroupTargets(
      String targetSystem1, String targetVersion1, String targetSystem2, String targetVersion2) {
    ConceptMap conceptMap = createMinimalConceptMap();

    ConceptMap.ConceptMapGroupComponent group1 = conceptMap.addGroup();
    group1.setTarget(targetSystem1);
    if (targetVersion1 != null) {
      group1.setTargetVersion(targetVersion1);
    }

    ConceptMap.ConceptMapGroupComponent group2 = conceptMap.addGroup();
    group2.setTarget(targetSystem2);
    if (targetVersion2 != null) {
      group2.setTargetVersion(targetVersion2);
    }

    return conceptMap;
  }

  /**
   * Creates a ConceptMap with both source and target URI.
   *
   * @param sourceUri source URI
   * @param targetUri target URI
   * @return ConceptMap with source and target URI
   */
  public static ConceptMap createConceptMapWithSourceAndTargetUri(
      String sourceUri, String targetUri) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setSource(new UriType(sourceUri));
    conceptMap.setTarget(new UriType(targetUri));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with both source and target canonical.
   *
   * @param sourceCanonical source canonical
   * @param targetCanonical target canonical
   * @return ConceptMap with source and target canonical
   */
  public static ConceptMap createConceptMapWithSourceAndTargetCanonical(
      String sourceCanonical, String targetCanonical) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setSource(new CanonicalType(sourceCanonical));
    conceptMap.setTarget(new CanonicalType(targetCanonical));
    return conceptMap;
  }

  /**
   * Creates a ConceptMap with source URI and target canonical.
   *
   * @param sourceUri source URI
   * @param targetCanonical target canonical
   * @return ConceptMap with mixed source and target
   */
  public static ConceptMap createConceptMapWithSourceUriAndTargetCanonical(
      String sourceUri, String targetCanonical) {
    ConceptMap conceptMap = createMinimalConceptMap();
    conceptMap.setSource(new UriType(sourceUri));
    conceptMap.setTarget(new CanonicalType(targetCanonical));
    return conceptMap;
  }

  /**
   * Creates a complex ConceptMap with all types of source and target references.
   *
   * @return complex ConceptMap instance
   */
  public static ConceptMap createComplexConceptMap() {
    ConceptMap conceptMap = createMinimalConceptMap();

    // Set source canonical (will also be indexed as URI)
    conceptMap.setSource(new CanonicalType("http://example.org/source|1.0.0"));

    // Set target canonical (will also be indexed as URI)
    conceptMap.setTarget(new CanonicalType("http://example.org/target|2.0.0"));

    // Add group with source system
    ConceptMap.ConceptMapGroupComponent group1 = conceptMap.addGroup();
    group1.setSource("http://example.org/cs/source");
    group1.setSourceVersion("1.0");
    group1.setTarget("http://example.org/cs/target");
    group1.setTargetVersion("2.0");

    // Add another group
    ConceptMap.ConceptMapGroupComponent group2 = conceptMap.addGroup();
    group2.setSource("http://example.org/cs/source2");
    group2.setTarget("http://example.org/cs/target2");

    return conceptMap;
  }
}
