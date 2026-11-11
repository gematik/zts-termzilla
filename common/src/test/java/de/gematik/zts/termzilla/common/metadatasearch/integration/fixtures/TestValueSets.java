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
package de.gematik.zts.termzilla.common.metadatasearch.integration.fixtures;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Test fixture factory for creating ValueSet resources for integration testing.
 *
 * <p>This class provides factory methods for creating FHIR ValueSet resources with various
 * configurations commonly needed in integration tests.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * ValueSet vs = TestValueSets.createSimple("common-languages", "Common Languages");
 * ValueSet vsWithVersion = TestValueSets.createWithVersion("vs-id", "VS Name", "1.0.0");
 * }</pre>
 */
public final class TestValueSets {

  /** Private constructor to prevent instantiation of utility class. */
  private TestValueSets() {
    throw new AssertionError("TestValueSets is a utility class and should not be instantiated");
  }

  /**
   * Creates a simple ValueSet with minimal required fields.
   *
   * @param id the resource ID
   * @param name the ValueSet name
   * @return a fully configured ValueSet
   */
  public static ValueSet createSimple(String id, String name) {
    ValueSet vs = new ValueSet();
    vs.setId(id);
    vs.setName(name);
    vs.setUrl("http://example.org/" + id);
    vs.setStatus(PublicationStatus.ACTIVE);
    return vs;
  }

  /**
   * Creates a ValueSet with an explicit version.
   *
   * @param id the resource ID
   * @param name the ValueSet name
   * @param version the version string
   * @return a ValueSet with version
   */
  public static ValueSet createWithVersion(String id, String name, String version) {
    ValueSet vs = createSimple(id, name);
    vs.setVersion(version);
    return vs;
  }

  /**
   * Creates a ValueSet with a specific publication status.
   *
   * @param id the resource ID
   * @param name the ValueSet name
   * @param status the publication status
   * @return a ValueSet with the specified status
   */
  public static ValueSet createWithStatus(String id, String name, PublicationStatus status) {
    ValueSet vs = createSimple(id, name);
    vs.setStatus(status);
    return vs;
  }

  /**
   * Creates a ValueSet with a description.
   *
   * @param id the resource ID
   * @param name the ValueSet name
   * @param description the textual description
   * @return a ValueSet with description
   */
  public static ValueSet createWithDescription(String id, String name, String description) {
    ValueSet vs = createSimple(id, name);
    vs.setDescription(description);
    return vs;
  }
}
