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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.terminologies.fhirterms.config.FhirConfigProperties;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.MetadataResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Integration tests for {@link FhirTermsResourceLoader}. Tests complex scenarios involving multiple
 * resources and package structures.
 */
@DisplayName("FhirTermsResourceLoader Integration Tests")
class FhirTermsResourceLoaderIT {

  @Mock private FhirConfigProperties mockProperties;

  @Mock private FhirTermsInMemoryRepresentation mockInMemoryRepresentation;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("Package Structure Tests")
  class PackageStructureTests {

    private FhirTermsResourceLoader loader;
    private Path packageFolder;
    private Path unzipLocation;
    private Map<String, MetadataResource> resourceMap;

    @BeforeEach
    void setUp() throws InvalidConfigurationException, IOException {
      packageFolder = tempDir.resolve("packages");
      unzipLocation = tempDir.resolve("unzipped");
      Files.createDirectories(packageFolder);
      Files.createDirectories(unzipLocation);

      resourceMap = new HashMap<>();

      when(mockProperties.getFhirTermsPackageFolderLocation()).thenReturn(packageFolder.toString());
      when(mockProperties.getFhirTermsUnzipLocation()).thenReturn(unzipLocation);
      when(mockInMemoryRepresentation.getIdToResourceMap()).thenReturn(resourceMap);

      loader = new FhirTermsResourceLoader(mockProperties, mockInMemoryRepresentation);
    }

    @Test
    @DisplayName("Should handle package folder with subdirectories")
    void shouldHandlePackageFolderWithSubdirectories() throws IOException {
      // GIVEN
      Path subDir = packageFolder.resolve("subfolder");
      Files.createDirectories(subDir);
      Files.createFile(subDir.resolve("test.tgz"));

      // WHEN
      loader.load();

      // THEN
      assertThat(resourceMap).isEmpty();
    }
  }
}
