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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MemoryCacheKeys.ReleaseVersionInactiveCacheKey}. */
@DisplayName("MemoryCacheKeys Tests")
class MemoryCacheKeysTest {

  @Nested
  @DisplayName("ReleaseVersionInactiveCacheKey constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should keep provided release version and inactive flag")
    void shouldKeepProvidedValues() throws UnsupportedTerminologyException {
      // GIVEN
      SnomedReleaseVersion releaseVersion =
          new SnomedReleaseVersion("900000000000207008", "20240131", java.util.Map.of());

      // WHEN
      MemoryCacheKeys.ReleaseVersionInactiveCacheKey key =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersion, true);

      // THEN
      assertThat(key.snomedReleaseVersion()).isEqualTo(releaseVersion);
      assertThat(key.inactive()).isTrue();
    }
  }

  @Nested
  @DisplayName("ReleaseVersionInactiveCacheKey.equals and hashCode")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same version value and same inactive flag")
    void shouldBeEqualForSameVersionValueAndFlag() throws UnsupportedTerminologyException {
      // GIVEN
      SnomedReleaseVersion releaseVersionA =
          new SnomedReleaseVersion("900000000000207008", "20240131", java.util.Map.of());
      SnomedReleaseVersion releaseVersionB =
          new SnomedReleaseVersion("900000000000207008", "20240131", java.util.Map.of());

      MemoryCacheKeys.ReleaseVersionInactiveCacheKey left =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersionA, false);
      MemoryCacheKeys.ReleaseVersionInactiveCacheKey right =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersionB, false);

      // WHEN / THEN
      assertThat(left).isEqualTo(right).hasSameHashCodeAs(right);
    }

    @Test
    @DisplayName("should not be equal for different inactive flag")
    void shouldNotBeEqualForDifferentInactiveFlag() throws UnsupportedTerminologyException {
      // GIVEN
      SnomedReleaseVersion releaseVersion =
          new SnomedReleaseVersion("900000000000207008", "20240131", java.util.Map.of());

      MemoryCacheKeys.ReleaseVersionInactiveCacheKey left =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersion, false);
      MemoryCacheKeys.ReleaseVersionInactiveCacheKey right =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersion, true);

      // WHEN / THEN
      assertThat(left).isNotEqualTo(right);
    }

    @Test
    @DisplayName("should not be equal for different version values")
    void shouldNotBeEqualForDifferentVersionValues() throws UnsupportedTerminologyException {
      // GIVEN
      SnomedReleaseVersion releaseVersionA =
          new SnomedReleaseVersion("900000000000207008", "20240131", java.util.Map.of());
      SnomedReleaseVersion releaseVersionB =
          new SnomedReleaseVersion("900000000000207008", "20240229", java.util.Map.of());

      MemoryCacheKeys.ReleaseVersionInactiveCacheKey left =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersionA, false);
      MemoryCacheKeys.ReleaseVersionInactiveCacheKey right =
          new MemoryCacheKeys.ReleaseVersionInactiveCacheKey(releaseVersionB, false);

      // WHEN / THEN
      assertThat(left).isNotEqualTo(right);
    }
  }
}
