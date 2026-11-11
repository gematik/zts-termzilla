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

import de.gematik.zts.termzilla.common.packages.downloader.AuthenticationMode;

/**
 * Authentication configuration for a package server.
 *
 * @param type authentication mode used for package requests
 * @param user optional username for basic authentication
 * @param password optional password for basic authentication
 * @param token optional bearer token for {@link AuthenticationMode#BEARER_TOKEN}
 * @param apiKey optional API key value for {@link AuthenticationMode#API_KEY}
 * @param apiKeyHeader optional API key header name for {@link AuthenticationMode#API_KEY}
 */
public record PackageServerAuthConfig(
    AuthenticationMode type,
    String user,
    String password,
    String token,
    String apiKey,
    String apiKeyHeader) {

  /**
   * Creates an auth configuration with mode {@link AuthenticationMode#NONE}.
   *
   * @return auth configuration without credentials
   */
  public static PackageServerAuthConfig none() {
    return new PackageServerAuthConfig(AuthenticationMode.NONE, null, null, null, null, null);
  }
}
