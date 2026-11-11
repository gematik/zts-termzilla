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
package de.gematik.zts.termzilla.terminologies.fhirterms.helper;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.hl7.fhir.r4.model.Resource;

/**
 * Utility class for creating stripped versions of FHIR resources. Provides common functionality for
 * removing content while preserving metadata and user data.
 */
public class ResourceStripHelper {

  private ResourceStripHelper() {
    // Private constructor to prevent instantiation
  }

  /**
   * Creates a stripped version of a FHIR resource by copying metadata and user data while removing
   * specific content using the provided content remover function.
   *
   * @param <T> The type of FHIR resource
   * @param resource The original resource to strip
   * @param contentRemover A unary operator that removes specific content from the copied resource
   * @return A stripped version of the resource with preserved metadata and user data
   */
  public static <T extends Resource> T createStrippedVersion(
      T resource, UnaryOperator<T> contentRemover) {

    // Apply content removal
    T reduced = contentRemover.apply(resource);

    // Copy user data (package name, version, scope) to reduced resource
    copyUserData(resource, reduced);

    return reduced;
  }

  /**
   * Copies standard user data fields from source to target resource.
   *
   * @param source The source resource containing user data
   * @param target The target resource to receive user data
   */
  private static void copyUserData(Resource source, Resource target) {
    String packageName = (String) source.getUserData(ResourceUserData.USERDATA_PACKAGENAME);
    String packageVersion = (String) source.getUserData(ResourceUserData.USERDATA_PACKAGEVERSION);
    Set<String> scope = (Set<String>) source.getUserData(ResourceUserData.USERDATA_SCOPE_SET);

    target.setUserData(ResourceUserData.USERDATA_PACKAGENAME, packageName);
    target.setUserData(ResourceUserData.USERDATA_PACKAGEVERSION, packageVersion);
    target.setUserData(ResourceUserData.USERDATA_SCOPE_SET, scope);
  }
}
