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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import java.nio.file.Path;

/** Shared test data and factory methods for FhirTermsInMemoryRepresentation tests. */
final class FhirTermsInMemoryRepresentationTestDataHelper {

  static final String DEFAULT_SCOPE = "fhir";
  static final String PACKAGE_SCOPE = "bfarm.terminologien.test-2024.0.0";
  static final String TEST_PACKAGE_FILE = "testpackages/bfarm.terminologien.test-2024.0.0.tgz";

  private FhirTermsInMemoryRepresentationTestDataHelper() {
    // Utility class.
  }

  static FhirConfigProperties createProperties(
      Path packageFolder, Path indexFolder, Path unzipFolder) {
    FhirConfigProperties properties = mock(FhirConfigProperties.class);
    when(properties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
    when(properties.getFhirTermsIndex()).thenReturn(indexFolder);
    when(properties.getFhirTermsUnzipLocation()).thenReturn(unzipFolder);
    return properties;
  }
}
