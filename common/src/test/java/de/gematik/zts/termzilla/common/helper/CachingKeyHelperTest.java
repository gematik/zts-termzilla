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
package de.gematik.zts.termzilla.common.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CachingKeyHelper}.
 *
 * <p>Verifies key determinism, uniqueness, output format, multi-array combination, and that the
 * utility class cannot be instantiated.
 */
@DisplayName("CachingKeyHelper Tests")
class CachingKeyHelperTest {

  private static final byte[] INPUT_A = "alpha".getBytes(StandardCharsets.UTF_8);
  private static final byte[] INPUT_B = "beta".getBytes(StandardCharsets.UTF_8);

  // ============================================================
  // createKey() Tests
  // ============================================================

  @Nested
  @DisplayName("createKey() Tests")
  class CreateKeyTests {

    @Test
    @DisplayName("Should return the same key for identical single-array input")
    void shouldReturnDeterministicKeyForSameInput() {
      // GIVEN
      byte[] input = "consistent-input".getBytes(StandardCharsets.UTF_8);

      // WHEN
      String key1 = CachingKeyHelper.createKey(input);
      String key2 = CachingKeyHelper.createKey(input);

      // THEN
      assertThat(key1)
          .as("Repeated call with same input must produce the same key")
          .isEqualTo(key2);
    }

    @Test
    @DisplayName("Should return different keys for different inputs")
    void shouldReturnDifferentKeysForDifferentInputs() {
      // GIVEN / WHEN
      String keyA = CachingKeyHelper.createKey(INPUT_A);
      String keyB = CachingKeyHelper.createKey(INPUT_B);

      // THEN
      assertThat(keyA).as("Different inputs must produce different keys").isNotEqualTo(keyB);
    }

    @Test
    @DisplayName("Should return different keys for the same arrays in different order")
    void shouldReturnDifferentKeysForDifferentOrdering() {
      // GIVEN / WHEN
      String keyAB = CachingKeyHelper.createKey(INPUT_A, INPUT_B);
      String keyBA = CachingKeyHelper.createKey(INPUT_B, INPUT_A);

      // THEN
      assertThat(keyAB).as("Array order must affect the resulting key").isNotEqualTo(keyBA);
    }

    @Test
    @DisplayName("Should produce a 64-character lowercase hex string (SHA-256 output)")
    void shouldReturnHexEncodedSha256Hash() {
      // GIVEN
      byte[] input = "hash-format-check".getBytes(StandardCharsets.UTF_8);

      // WHEN
      String key = CachingKeyHelper.createKey(input);

      // THEN
      assertThat(key)
          .as("SHA-256 digest must be represented as 64 hex characters")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Should produce the known SHA-256 digest for a fixed input")
    void shouldProduceKnownDigestForFixedInput() {
      // GIVEN
      // SHA-256("test") = 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
      byte[] input = "test".getBytes(StandardCharsets.UTF_8);
      String expectedDigest = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

      // WHEN
      String key = CachingKeyHelper.createKey(input);

      // THEN
      assertThat(key)
          .as("Digest of 'test' must match the known SHA-256 value")
          .isEqualTo(expectedDigest);
    }

    @Test
    @DisplayName("Should handle an empty byte array without error")
    void shouldHandleEmptyByteArrayWithoutError() {
      // GIVEN
      byte[] empty = new byte[0];

      // WHEN
      String key = CachingKeyHelper.createKey(empty);

      // THEN
      assertThat(key)
          .as("Empty byte array must still produce a valid 64-character hex key")
          .hasSize(64)
          .matches("[0-9a-f]+");
    }

    @ParameterizedTest(name = "input: \"{0}\"")
    @DisplayName("Should return a valid key for various string inputs")
    @ValueSource(strings = {"a", "12345", "http://snomed.info/sct", "   ", "\n"})
    void shouldReturnValidKeyForVariousInputs(String value) {
      // GIVEN
      byte[] input = value.getBytes(StandardCharsets.UTF_8);

      // WHEN
      String key = CachingKeyHelper.createKey(input);

      // THEN
      assertThat(key)
          .as("Key for input '%s' must be a valid 64-character hex string", value)
          .hasSize(64)
          .matches("[0-9a-f]+");
    }
  }

  // ============================================================
  // Constructor Tests
  // ============================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should not be publicly instantiable (private constructor)")
    void shouldNotBePubliclyInstantiable() throws NoSuchMethodException {
      // GIVEN
      Constructor<CachingKeyHelper> constructor = CachingKeyHelper.class.getDeclaredConstructor();

      // THEN
      assertThat(constructor.canAccess(null))
          .as("CachingKeyHelper must have a private constructor")
          .isFalse();
    }
  }
}
