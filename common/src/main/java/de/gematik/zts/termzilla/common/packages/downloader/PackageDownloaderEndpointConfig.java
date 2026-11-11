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
 * Aggregates optional endpoint path settings for {@link PackageDownloader}.
 *
 * @param packagePathPrefix path prefix used for package metadata and downloads
 * @param packageCatalogPath path used for package catalog retrieval
 */
public record PackageDownloaderEndpointConfig(String packagePathPrefix, String packageCatalogPath) {

  /**
   * Creates an empty endpoint configuration so downloader defaults are applied.
   *
   * @return config with unset path values
   */
  public static PackageDownloaderEndpointConfig defaults() {
    return new PackageDownloaderEndpointConfig(null, null);
  }
}
