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
package de.gematik.zts.termzilla.terminologies.fhirterms.config.packageserver;

import java.util.LinkedHashMap;
import java.util.Map;

/** Test data helper for package server configuration properties. */
public final class PackageServerConfigDataHelper {

  private PackageServerConfigDataHelper() {}

  /**
   * Builds indexed properties for two package servers.
   *
   * @return properties suitable for SmallRyeConfig
   */
  public static Map<String, String> twoServerProperties() {
    Map<String, String> properties = new LinkedHashMap<>();

    properties.put("fhirterms.packageServerConfig.servers[0].name", "ZTS BfArM");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].url", "https://terminologien.bfarm.de");
    properties.put("fhirterms.packageServerConfig.servers[0].auth.type", "BFARM_AUTO_TOKEN");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].endpoints.packagePathPrefix", "/packages");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].endpoints.catalogPath", "/packages/catalog");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].skipDeprecatedUnlistedPackageVersions", "true");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].packages[0]",
        "bfarm.terminologien.icd10gm@2024.0.0");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].packages[1]", "bfarm.terminologien.ops@2024.0.0");

    properties.put("fhirterms.packageServerConfig.servers[1].name", "FHIR Simplifier");
    properties.put("fhirterms.packageServerConfig.servers[1].url", "https://simplifier.net");
    properties.put("fhirterms.packageServerConfig.servers[1].auth.type", "BASIC");
    properties.put("fhirterms.packageServerConfig.servers[1].auth.user", "user");
    properties.put("fhirterms.packageServerConfig.servers[1].auth.password", "password");
    properties.put("fhirterms.packageServerConfig.servers[1].endpoints.packagePathPrefix", "");
    properties.put("fhirterms.packageServerConfig.servers[1].endpoints.catalogPath", "/catalog");
    properties.put("fhirterms.packageServerConfig.servers[1].packages[0]", "my.package.core@1.2.3");

    return properties;
  }

  /**
   * Builds properties for one server using comma-separated package fallback.
   *
   * @return properties suitable for SmallRyeConfig
   */
  public static Map<String, String> commaSeparatedPackageProperties() {
    Map<String, String> properties = new LinkedHashMap<>();
    properties.put(
        "fhirterms.packageServerConfig.servers[0].url", "https://terminologien.bfarm.de");
    properties.put(
        "fhirterms.packageServerConfig.servers[0].packages",
        "bfarm.terminologien.icd10gm@2024.0.0,bfarm.terminologien.ops@2024.0.0");
    return properties;
  }
}
