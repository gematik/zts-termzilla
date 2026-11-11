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
package de.gematik.zts.termzilla.common.tools;

import jakarta.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

public class UriHelper {

  private UriHelper() {
    throw new AssertionError("UriHelper is a utility class and should not be instantiated");
  }

  // TODO: NEeds major reworking, this is a mess
  public static String normalizeUrl(String url) throws URISyntaxException {
    URI uri = new URI(url).normalize(); // Normalisiert den Pfad, entfernt `.` und `..`

    if (isConvertibleToUrl(uri)) {

      // Protokoll und Hostname in Kleinbuchstaben
      String scheme = null;
      if (uri.getScheme() != null) {
        scheme = uri.getScheme().toLowerCase();
      }

      String host = null;
      if (uri.getHost() != null) {
        host = uri.getHost().toLowerCase();
      }

      // Entferne den Standardport, falls vorhanden (80 für HTTP und 443 für HTTPS)
      int port = uri.getPort();
      if (port == -1
          || (Objects.equals(scheme, "http") && port == 80)
          || (Objects.equals(scheme, "https") && port == 443)) {
        port = -1; // Standardport ignorieren
      }

      // Normalisierten Pfad erhalten (falls leer, setze auf "/")
      // TODO: Hier haben wir nen ekeliges Problem, wenn der Pfad leer ist, dann wird er auf "/"
      // gesetzt. Das sorgt
      //  dafür, dass die URL nicht mehr mit der Original-URL übereinstimmt, wenn der Pfad leer ist.
      // Das passiert
      //  häufiger als man denkt: z.B. EDQM
      //  wurde erstmal auskommentiert, ich kann mir aber vorstellen, dass wir das irgendwann mal
      // mit absicht reingemacht haben
      String path = uri.getPath();

      // Sortiere die Query-Parameter alphabetisch
      String query = uri.getQuery();
      if (query != null) {
        String[] queryParams = query.split("&");
        Arrays.sort(queryParams);
        query = String.join("&", queryParams);
      }

      // Baue die URL wieder zusammen (ohne Fragment)
      URI normalizedUri = new URI(scheme, null, host, port, path, query, null);
      return normalizedUri.toString();
    } else {
      return uri.toString();
    }
  }

  /**
   * Checks if the given URI can be converted to a valid URL.
   *
   * @param uri the URI to check
   * @return {@code true} if the URI is convertible to a URL, {@code false} otherwise
   */
  public static boolean isConvertibleToUrl(@Nonnull URI uri) {
    try {
      uri.toURL();
      return true;
    } catch (MalformedURLException _) {
      return false;
    }
  }
}
