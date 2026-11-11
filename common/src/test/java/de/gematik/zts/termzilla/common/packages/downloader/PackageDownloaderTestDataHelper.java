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

import java.util.List;

/** Test data helper for {@link PackageDownloader} tests. */
public final class PackageDownloaderTestDataHelper {

  private static final String VALID_PACKAGE_NAME = "bfarm.terminologien.icd10gm";
  private static final String VALID_PACKAGE_VERSION = "2024.0.0";
  private static final String PACKAGE_CONTENT_SHA_SUM = "ca3cc32692594fef390ca56591832d7e73b2963f";
  private static final String DIFFERENT_CONTENT_SHA_SUM =
      "e7f828b26224b4a9105c50ccbd809fd5fb626f5d";

  private PackageDownloaderTestDataHelper() {}

  public static String validPackageName() {
    return VALID_PACKAGE_NAME;
  }

  public static String validPackageVersion() {
    return VALID_PACKAGE_VERSION;
  }

  public static String secondValidPackageName() {
    return "bfarm.terminologien.loinc";
  }

  public static String secondValidPackageVersion() {
    return "2.81.0";
  }

  public static List<String> invalidPackageNames() {
    return List.of("", " ", "INVALID", "onlyonesegment", "bfarm..invalid");
  }

  public static List<String> invalidPackageVersions() {
    return List.of("", " ", "v1", "2024", "1.0");
  }

  public static String packageContentShaSum() {
    return PACKAGE_CONTENT_SHA_SUM;
  }

  public static String differentContentShaSum() {
    return DIFFERENT_CONTENT_SHA_SUM;
  }

  public static String packageMetadataPayload(
      String packageName, String packageVersion, boolean isProtected) {
    return packageMetadataPayload(packageName, packageVersion, isProtected, packageContentShaSum());
  }

  public static String packageMetadataPayload(
      String packageName, String packageVersion, boolean isProtected, String shaSum) {
    return """
        {
          "name": "%s",
          "versions": {
            "%s": {
              "protected": %s,
              "dist": {
                "shasum": "%s"
              }
            }
          }
        }
        """
        .formatted(packageName, packageVersion, isProtected, shaSum);
  }

  public static String packageCatalogPayload() {
    return """
        [
          {
            "name": "%s",
            "packageVersions": ["%s", "2025.0.0"]
          },
          {
            "name": "%s",
            "packageVersions": ["%s"]
          }
        ]
        """
        .formatted(
            validPackageName(),
            validPackageVersion(),
            secondValidPackageName(),
            secondValidPackageVersion());
  }
}
