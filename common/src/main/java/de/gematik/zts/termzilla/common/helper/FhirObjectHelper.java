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

import de.gematik.zts.termzilla.terminologies.common.SystemVersionPair;
import java.util.Optional;

// TODO do we still need this? it was used in the CombiningVsProvidedExpandOperation operation, but
// now CanonicalPair is being used
public class FhirObjectHelper {

  private FhirObjectHelper() {
    // Private constructor to prevent instantiation
  }

  public static SystemVersionPair processCanonical(String canonical) {

    // canonical ist null oder leer
    if (canonical == null || canonical.isEmpty()) {
      return SystemVersionPair.builder().system(Optional.empty()).version(Optional.empty()).build();
    }

    // canonical enthält kein Pipe-Zeichen
    if (!canonical.contains("|")) {
      return SystemVersionPair.builder()
          .system(Optional.of(canonical.trim()))
          .version(Optional.empty())
          .build();
    }

    // Wir splitten den String an dem Pipe-Zeichen
    String[] parts = canonical.split("\\|");

    if (parts.length > 2) {
      // Wir haben mehr als zwei Teile
      throw new IllegalArgumentException("Invalid canonical format: " + canonical);
    } else if (parts.length == 1) {
      // Wir haben nur einen Teil, also nur das System
      return SystemVersionPair.builder()
          .system(Optional.of(parts[0].trim()))
          .version(Optional.empty())
          .build();
    } else if (parts.length == 2) {
      // Wir haben zwei Teile, also System und Version
      return SystemVersionPair.builder()
          .system(Optional.of(parts[0].trim()))
          .version(Optional.of(parts[1].trim()))
          .build();
    }

    // Fallback, falls etwas schiefgeht
    return SystemVersionPair.builder().system(Optional.empty()).version(Optional.empty()).build();
  }
}
