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
package de.gematik.zts.termzilla.testutils.packages.generator;

/**
 * Central repository of constant values used during FHIR package generation. These constants define
 * file names, directory names and JSON field keys required to create npm-compatible FHIR package
 * structures.
 *
 * <p>The class is non-instantiable; use the provided {@code public static final} fields directly.
 */
public final class PackageGenerationConstants {

  public static final String PACKAGE_TEMPLATE_FILE_NAME = "package.template.json";
  public static final String TEMPLATE_RULES_FILE_NAME = "template.rules.json";

  public static final String PACKAGE_DIR_NAME = "package";

  public static final String PACKAGE_JSON_FILE_NAME = "package.json";
  public static final String PACKAGE_JSON_FIELD_AUTHOR = "author";
  public static final String PACKAGE_JSON_FIELD_TITLE = "title";
  public static final String PACKAGE_JSON_FIELD_DESCRIPTION = "description";
  public static final String PACKAGE_JSON_FIELD_CANONICAL = "canonical";
  public static final String PACKAGE_JSON_FIELD_URL = "url";
  public static final String PACKAGE_JSON_FIELD_TYPE = "type";

  public static final String PACKAGE_INDEX_FILENAME = ".index.json";

  private PackageGenerationConstants() {
    throw new IllegalStateException("Constant class");
  }
}
