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
package de.gematik.zts.termzilla.api.interfaces.properties;

public class ConceptProperties {

  private ConceptProperties() {
    // Constants class - private constructor to prevent instantiation
  }

  // Common Properties
  public static final String PROPERTY_NAME_URL = "url";
  public static final String PROPERTY_NAME_INACTIVE = "inactive";
  public static final String PROPERTY_NAME_CHILD = "child";
  public static final String PROPERTY_NAME_PARENT = "parent";
  public static final String PROPERTY_NAME_CODE = "code";
  public static final String PROPERTY_NAME_CONCEPT = "concept";
  public static final String PROPERTY_NAME_DISPLAY = "display";
  public static final String PROPERTY_NAME_DEFINITION = "definition";
  public static final String PROPERTY_NAME_DESIGNATION = "designation";
  public static final String PROPERTY_NAME_STATUS = "status";

  // Snomed Properties
  public static final String PROPERTY_NAME_SUFFICIENTLY_DEFINED = "sufficientlyDefined";
  public static final String PROPERTY_NAME_MODULE_ID = "moduleId";

  // Loinc Properties
  public static final String PROPERTY_NAME_COMPONENT = "COMPONENT";
  public static final String PROPERTY_NAME_SYSTEM = "SYSTEM";
  public static final String PROPERTY_NAME_PROPERTY = "PROPERTY";
}
