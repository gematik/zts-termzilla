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
package de.gematik.zts.termzilla.common.packages.downloader;

/**
 * Combined configuration for authentication and endpoint settings used by {@link
 * PackageDownloader}.
 *
 * @param authConfig authentication credential values
 * @param endpointConfig endpoint path settings
 * @param skipDeprecatedUnlistedPackageVersions whether deprecated/unlisted versions should be
 *     skipped when resolving package entries
 */
public record PackageDownloaderConfig(
    PackageDownloaderAuthConfig authConfig,
    PackageDownloaderEndpointConfig endpointConfig,
    boolean skipDeprecatedUnlistedPackageVersions) {

  /**
   * Creates a configuration with auth and endpoint values and default package filtering.
   *
   * @param authConfig authentication credential values
   * @param endpointConfig endpoint path settings
   */
  public PackageDownloaderConfig(
      PackageDownloaderAuthConfig authConfig, PackageDownloaderEndpointConfig endpointConfig) {
    this(authConfig, endpointConfig, false);
  }

  /**
   * Creates a configuration with default auth and endpoint values.
   *
   * @return default downloader configuration
   */
  public static PackageDownloaderConfig defaults() {
    return new PackageDownloaderConfig(
        PackageDownloaderAuthConfig.none(), PackageDownloaderEndpointConfig.defaults(), false);
  }
}
