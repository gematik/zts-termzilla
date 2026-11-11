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
package de.gematik.zts.termzilla.terminologies.snomed.memory.caching;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.Objects;

/** Provides cache key types used by in-memory SNOMED caches. */
public class MemoryCacheKeys {

  /** Prevents instantiation of this utility class. */
  private MemoryCacheKeys() {
    // private constructor to prevent instantiation of this utility class
  }

  /**
   * Cache key combining SNOMED release identity and inactive flag.
   *
   * @param snomedReleaseVersion SNOMED release version associated with the cached value.
   * @param inactive Indicates whether inactive concepts are part of the cache scope.
   */
  public record ReleaseVersionInactiveCacheKey(
      SnomedReleaseVersion snomedReleaseVersion, boolean inactive) {

    /**
     * Compares cache keys by inactive flag and FHIR release version.
     *
     * @param o the object to compare with
     * @return {@code true} if both keys represent the same cache scope
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ReleaseVersionInactiveCacheKey(SnomedReleaseVersion releaseVersion, boolean inactive1))) {
        return false;
      }
      return inactive == inactive1
          && Objects.equals(
              snomedReleaseVersion.getFhirVersion(),
              releaseVersion.getFhirVersion());
    }

    /**
     * Computes a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code based on FHIR release version and inactive flag
     */
    @Override
    public int hashCode() {
      return Objects.hash(snomedReleaseVersion.getFhirVersion(), inactive);
    }
  }
}
