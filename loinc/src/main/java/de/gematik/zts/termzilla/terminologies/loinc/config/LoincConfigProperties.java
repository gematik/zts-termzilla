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
package de.gematik.zts.termzilla.terminologies.loinc.config;

import de.gematik.zts.termzilla.common.config.Value;
import de.gematik.zts.termzilla.common.config.provision.ProvisionAware;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.nio.file.Path;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class LoincConfigProperties implements ProvisionAware {

  private static final String INDEX_DIR = "loinc_index";
  private static final String UNZIP_DIR = "loinc_unzip";

  private Path baseWorkingDirectory;

  @Value("${loinc.start:false}")
  private boolean loincStart;

  @Value("${loinc.rfCollectionLocation}")
  private String loincRfCollectionLocation;

  @Value("${loinc.defaultLanguage:de-DE}")
  private String loincDefaultLanguage;

  @Value("${loinc.languages:de-DE,de-AT,es-ES}")
  private List<String> loincLanguages;

  @Value("${loinc.provisionMode:SUPPORTED}")
  private ProvisionMode provisionMode;

  @Value("${loinc.version}")
  private String version;

  public Path getLoincIndexLocation() {
    return baseWorkingDirectory.resolve(INDEX_DIR);
  }

  public Path getLoincUnzipLocation() {
    return baseWorkingDirectory.resolve(UNZIP_DIR);
  }
}
