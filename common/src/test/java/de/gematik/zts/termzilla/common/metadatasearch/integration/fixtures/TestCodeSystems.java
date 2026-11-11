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

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;

/**
 * Test fixture factory for creating CodeSystem resources for integration testing.
 *
 * <p>This class provides factory methods for creating FHIR CodeSystem resources with various
 * configurations commonly needed in integration tests. All methods return fully configured
 * CodeSystem instances that can be directly indexed and searched.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * CodeSystem snomed = TestCodeSystems.createSimple("snomed-ct", "SNOMED CT");
 * CodeSystem loinc = TestCodeSystems.createWithVersion("loinc", "LOINC", "2.77");
 * CodeSystem retired = TestCodeSystems.createWithStatus("old-cs", "Old CodeSystem", PublicationStatus.RETIRED);
 * }</pre>
 */
public final class TestCodeSystems {

  /** Private constructor to prevent instantiation of utility class. */
  private TestCodeSystems() {
    throw new AssertionError("TestCodeSystems is a utility class and should not be instantiated");
  }

  /**
   * Creates a simple CodeSystem with minimal required fields.
   *
   * <p>The created CodeSystem has:
   *
   * <ul>
   *   <li>ID set to the provided id
   *   <li>Name set to the provided name
   *   <li>URL set to http://example.org/{id}
   *   <li>Status set to ACTIVE
   *   <li>Content set to COMPLETE
   * </ul>
   *
   * @param id the resource ID
   * @param name the CodeSystem name
   * @return a fully configured CodeSystem
   */
  public static CodeSystem createSimple(String id, String name) {
    CodeSystem cs = new CodeSystem();
    cs.setId(id);
    cs.setName(name);
    cs.setUrl("http://example.org/CodeSystem/" + id);
    cs.setStatus(PublicationStatus.ACTIVE);
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    return cs;
  }

  /**
   * Creates a CodeSystem with an explicit version.
   *
   * @param id the resource ID
   * @param name the CodeSystem name
   * @param version the version string
   * @return a CodeSystem with version
   */
  public static CodeSystem createWithVersion(String id, String name, String version) {
    CodeSystem cs = createSimple(id, name);
    cs.setVersion(version);
    return cs;
  }

  /**
   * Creates a fully populated CodeSystem with all common fields set.
   *
   * @param id the resource ID
   * @param name the CodeSystem name
   * @param version the version
   * @param title the title
   * @param description the description
   * @param status the publication status
   * @return a fully populated CodeSystem
   */
  public static CodeSystem createComplete(
      String id,
      String name,
      String version,
      String title,
      String description,
      PublicationStatus status) {
    CodeSystem cs = new CodeSystem();
    cs.setId(id);
    cs.setName(name);
    cs.setVersion(version);
    cs.setTitle(title);
    cs.setDescription(description);
    cs.setUrl("http://example.org/CodeSystem/" + id);
    cs.setStatus(status);
    cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
    return cs;
  }
}
