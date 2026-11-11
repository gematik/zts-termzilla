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
package de.gematik.zts.termzilla.testing.common.asserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.MetadataResource;

public class CommonMetadateResourceAsserts {

  public static void asserMetadataResourceHasCorrectUrlAndVersion(
      MetadataResource resource, String expectedUrl, String expectedVersion) {
    assertTrue(resource.hasUrl(), "MetadataResource should have a url element");
    assertEquals(expectedUrl, resource.getUrl(), "MetadataResource url should match expected url");
    assertTrue(resource.hasVersion(), "MetadataResource should have a version element");
    assertEquals(
        expectedVersion,
        resource.getVersion(),
        "MetadataResource version should match expected version");
  }
}
