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
package de.gematik.zts.termzilla.testutils.packages.generator.model;

import static de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationConstants.PACKAGE_JSON_FILE_NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Slf4j
public class NpmPackage {

  public static NpmPackage load(Path packageJsonPath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(packageJsonPath.toFile(), NpmPackage.class);
  }

  public static Path write(Path outputDirectory, NpmPackage packageManifest) throws IOException {
    // create temporary package manifest file from object
    ObjectMapper mapper = new ObjectMapper();
    Path packageManifestFile = outputDirectory.resolve(PACKAGE_JSON_FILE_NAME);
    log.debug("Writing package manifest to: " + packageManifestFile.toAbsolutePath());
    mapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(packageManifestFile.toFile(), packageManifest);
    return packageManifestFile;
  }

  private String name;
  private String version;

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  private Map<String, String> dependencies;

  private String canonical;
  private String homepage;
  private String url;
  private String title;
  private String description;
  private String date;
  private String tfcVersion;

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonInclude(Include.NON_EMPTY)
  private List<String> fhirVersions;

  private String author;

  @JsonSetter(nulls = Nulls.AS_EMPTY)
  @JsonInclude(Include.NON_EMPTY)
  private List<Maintainer> maintainers;

  private String license;
  private FhirPackageType type;
  private String jurisdiction;

  @JsonIgnore
  public Stream<NpmDependency> getNpmDependencies() {
    if (dependencies == null || dependencies.isEmpty()) {
      return Stream.empty();
    }
    return dependencies.entrySet().stream().map(e -> new NpmDependency(e.getKey(), e.getValue()));
  }

  public NpmDependency toDependency() {
    return new NpmDependency(this.name, this.version);
  }

  public void logMissingFields() {
    warnIfNull(version, "version");
    warnIfNull(name, "name");
  }

  private void warnIfNull(String propVal, String propName) {
    if (propVal == null) {
      log.warn("FHIR package manifest property {} is not set, but is mandatory.", propName);
    }
  }
}
