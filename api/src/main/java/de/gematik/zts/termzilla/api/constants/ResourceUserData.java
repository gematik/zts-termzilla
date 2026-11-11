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
package de.gematik.zts.termzilla.api.constants;

/**
 * Constants for user data keys embedded into FHIR resources via {@link
 * org.hl7.fhir.r4.model.Resource#setUserData(String, Object) Resource.setUserData}.
 *
 * <p>These keys are used to attach internal metadata to FHIR resources throughout their lifecycle
 * in the terminology server, such as origin package information, language, and provision mode.
 */
public class ResourceUserData {

  private ResourceUserData() {
    // Utility class, prevent instantiation
  }

  /** Key for the language associated with the resource. */
  public static final String USERDATA_LANGUAGE = "language";

  /** Key for the scope set the resource belongs to. */
  public static final String USERDATA_SCOPE_SET = "scope";

  /** Key for the name of the FHIR package the resource originates from. */
  public static final String USERDATA_PACKAGENAME = "packageName";

  /** Key for the hash of the resource content. */
  public static final String USERDATA_RESOURCEHASH = "resourceHash";

  /** Key for the version of the FHIR package the resource originates from. */
  public static final String USERDATA_PACKAGEVERSION = "packageVersion";

  /** Key indicating whether the resource is a reduced (partial) representation. */
  public static final String USERDATA_REDUCED_RESOURCE = "reducedResource";

  /** Key for alternative codes associated with a {@code CodeSystem} concept. */
  public static final String USERDATA_CODESYSTEM_ALTCODES = "altcodes";

  /** Key for synonyms associated with a {@code CodeSystem} concept. */
  public static final String USERDATA_CODESYSTEM_SYNONYMS = "synonyms";

  /** Key for the provision mode of the resource ({@code SUPPORTED} or {@code PREDEFINED}). */
  public static final String USERDATA_PROVISION_MODE = "provisionMode";

  /**
   * Key for the map of concept codes to their corresponding concept objects, used for efficient
   * lookup.
   */
  public static final String USERDATA_CONCEPTS_AS_MAP = "conceptsAsMap";

  /**
   * Key for the flattened list of all concepts (including nested sub-concepts), used to avoid
   * repeated recursive traversal of the concept hierarchy.
   */
  public static final String USERDATA_CONCEPTS_AS_LIST = "conceptsAsList";

  public static final String USERDATA_COMPOSE_PART_TYPE = "composePartType";
}
