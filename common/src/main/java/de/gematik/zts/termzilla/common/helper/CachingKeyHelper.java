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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utility class for generating cache keys from byte arrays using SHA-256 hashing. */
public class CachingKeyHelper {

  private CachingKeyHelper() {}

  /**
   * Creates a hex-encoded SHA-256 hash from one or more byte arrays.
   *
   * @param cachingInput one or more byte arrays to include in the hash
   * @return the hex-encoded SHA-256 digest
   */
  public static String createKey(byte[]... cachingInput) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      for (byte[] c : cachingInput) {
        messageDigest.update(c);
      }
      return HexFormat.of().formatHex(messageDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
