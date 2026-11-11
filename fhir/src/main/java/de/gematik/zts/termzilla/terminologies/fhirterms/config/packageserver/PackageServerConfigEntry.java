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

import java.util.Set;

/**
 * Configuration entry for one package server.
 *
 * @param name display name used in logs
 * @param url server base URL
 * @param auth authentication configuration
 * @param endpoints optional endpoint path configuration
 * @param skipDeprecatedUnlistedPackageVersions whether deprecated/unlisted package versions should
 *     be skipped
 * @param packages package name/version entries in format {@code package.name@x.y.z} or the wildcard
 *     {@code *}
 */
public record PackageServerConfigEntry(
    String name,
    String url,
    PackageServerAuthConfig auth,
    PackageServerEndpointConfig endpoints,
    boolean skipDeprecatedUnlistedPackageVersions,
    Set<String> packages) {

  /**
   * Creates a server configuration with default package filtering behavior.
   *
   * @param name display name used in logs
   * @param url server base URL
   * @param auth authentication configuration
   * @param endpoints optional endpoint path configuration
   * @param packages package entries
   */
  public PackageServerConfigEntry(
      String name,
      String url,
      PackageServerAuthConfig auth,
      PackageServerEndpointConfig endpoints,
      Set<String> packages) {
    this(name, url, auth, endpoints, false, packages);
  }
}
