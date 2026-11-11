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
package de.gematik.zts.termzilla.terminologies.snomed.graph.caching;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;

/** Utility holder for cache key types used by SNOMED graph caching. */
public class GraphCacheKeys {

  /** Prevents instantiation of this utility class. */
  private GraphCacheKeys() {
    // private constructor to prevent instantiation of this utility class
  }

  /** Cache key combining a {@link SnomedReleaseVersion} and a string-based cache value.
   * Creates a cache key for the given release version and cache value.
   * @param snomedReleaseVersion release version segment of the cache key
   * @param cacheValue string segment of the cache key
   */
  public record SnomedVersionStringCacheKey(
      SnomedReleaseVersion snomedReleaseVersion, String cacheValue) {

    /**
     * Compares this key with another object.
     *
     * @param o object to compare
     * @return {@code true} if both keys contain equal cache values and the same release version
     *     instance
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SnomedVersionStringCacheKey(SnomedReleaseVersion releaseVersion, String value))) {
        return false;
      }
      return cacheValue.contentEquals(value)
          && snomedReleaseVersion == releaseVersion;
    }
  }
}
