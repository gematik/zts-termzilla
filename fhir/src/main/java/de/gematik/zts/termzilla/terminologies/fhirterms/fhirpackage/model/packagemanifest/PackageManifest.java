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
package de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packagemanifest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the {@code package.json} inside a NPM package.
 *
 * <p>The following shows a example template for a package.json:
 *
 * <pre>
 * {
 *  "name": "fokus.yaftp.common",
 *  "version": "WILL BE SET BY TOOLING",
 *  "canonical": "https://ehealth.fokus.fraunhofer.de/fhir/yaftp/common",
 *  "url": "https://fhir.apps.osc.fokus.fraunhofer.de/guides/fokus-yaftp-common",
 *  "title": "Implementierungsleitfaden YAFTP Common (Basisinformationen)",
 *  "description": "Ich bin eine tolle Beschreibung des YAFTP Common Packages",
 *  "fhirVersions": [
 *    "4.0.1"
 *  ],
 *  "dependencies": {
 *    "hl7.fhir.r4.core": "4.0.1",
 *    "fokus.yaftp.common.profiles": "0.0.1",
 *    "fokus.yaftp.common.termzilla": "0.0.1"
 *  },
 *  "author": "Fraunhofer FOKUS - ESPRI",
 *  "maintainers": [
 *    {
 *      "name": "ESPRI - E-Health",
 *      "email": "ehealth@fokus.fraunhofer.de"
 *    }
 *  ]
 * }
 *
 * </pre>
 */
@Getter
@Setter
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageManifest {

  @JsonProperty("name")
  String name;

  @JsonProperty("version")
  String version;

  @JsonProperty("canonical")
  String canonical;

  @JsonProperty("homepage")
  String homepage;

  @JsonProperty("url")
  String url;

  @JsonProperty("title")
  String title;

  @JsonProperty("description")
  String description;

  @JsonProperty("fhirVersions")
  List<String> fhirVersions;

  @JsonProperty("dependencies")
  Map<String, String> dependencies;

  @JsonProperty("author")
  String author;

  @JsonProperty("maintainers")
  List<Maintainer> maintainers;

  @Override
  public String toString() {
    return name + "-" + version;
  }
}
