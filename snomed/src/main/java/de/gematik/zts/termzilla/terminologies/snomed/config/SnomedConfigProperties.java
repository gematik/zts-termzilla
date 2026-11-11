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
package de.gematik.zts.termzilla.terminologies.snomed.config;

import de.gematik.zts.termzilla.common.config.Value;
import de.gematik.zts.termzilla.common.config.provision.ProvisionAware;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for SNOMED terminology loading and indexing.
 *
 * <p>This class provides runtime settings for startup behavior, release file handling, expression
 * processing, restricted mode, provisioning mode, and index cache limits.
 */
@Getter
@Setter
@EqualsAndHashCode
public class SnomedConfigProperties implements ProvisionAware {

  /** Subdirectory name used for the persisted Lucene index. */
  private static final String INDEX_DIR = "snomed_index";

  /** Subdirectory name used for unpacked SNOMED release files. */
  private static final String UNZIP_DIR = "snomed_unzip";

  /** Base directory that contains both index and unzip working folders. */
  private Path baseWorkingDirectory;

  /** Enables or disables SNOMED initialization during startup. */
  @Value("${snomed.start:false}")
  private boolean snomedStart;

  /** Location of the SNOMED release archive that will be loaded. */
  @Value("${snomed.releasefile}")
  private String snomedReleaseFileLocation;

  /** Provisioning strategy used by components that implement {@link ProvisionAware}. */
  @Value("${snomed.provisionMode:SUPPORTED}")
  private ProvisionMode provisionMode;

  /** Controls whether context extension text is added to designation outputs. */
  @Value("${snomed.contextExtensionInDesignations:false}")
  private boolean contextExtensionInDesignations;

  /** Enables processing of SNOMED expressions (for example ECL/post-coordinated expressions). */
  @Value("${snomed.expressionProcessingEnabled:true}")
  private boolean expressionProcessingEnabled;

  /**
   * Enables the restricted mode, in which only concept, description, language refset and refset
   * membership information is exposed. Information about concept relationships (hierarchy,
   * attributes, MRCM) and the definition status is not available. Restricted mode implicitly
   * disables expression processing.
   */
  @Value("${snomed.restrictedMode:true}")
  private boolean restrictedMode;

  /** Maximum number of parsed Lucene queries cached by the SNOMED index. */
  @Value("${snomed.index.maxCachedQueries:1000}")
  private int indexMaxCachedQueries;

  /** Memory limit in MB for Lucene query caching in the SNOMED index. */
  @Value("${snomed.index.maxRamBytesMb:500}")
  private int indexMaxRamBytesMb;

  /**
   * Returns the working directory location used to persist the SNOMED Lucene index.
   *
   * @return absolute or relative {@link Path} to the index directory
   */
  public Path getSnomedIndexLocation() {
    return baseWorkingDirectory.resolve(INDEX_DIR);
  }

  /**
   * Returns the working directory location used for unpacked SNOMED release files.
   *
   * @return absolute or relative {@link Path} to the unzip directory
   */
  public Path getSnomedUnzipLocation() {
    return baseWorkingDirectory.resolve(UNZIP_DIR);
  }
}
