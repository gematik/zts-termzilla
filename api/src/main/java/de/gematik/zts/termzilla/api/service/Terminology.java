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
package de.gematik.zts.termzilla.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Terminology {

  private Terminology() {
    // Private constructor to prevent instantiation
  }

  private static final String DEFAULT_PROVIDER =
      "de.gematik.zts.termzilla.terminologies.fhirterms.provider.FhirTermsTerminologyProvider";

  /**
   * Zurückgeben aller verfügbaren Provider
   *
   * @return Liste aller verfügbaren Provider
   */
  public static List<TerminologyProvider> providers() {
    List<TerminologyProvider> services = new ArrayList<>();
    ServiceLoader<TerminologyProvider> loader = ServiceLoader.load(TerminologyProvider.class);
    loader.forEach(services::add);
    return services;
  }

  /**
   * Zurückgeben des Default-Providers
   *
   * @return Default-Provider
   */
  public static TerminologyProvider provider() {
    return provider(DEFAULT_PROVIDER);
  }

  /**
   * Zurückgeben eines bestimmten Providers
   *
   * @param providerName Name des Providers
   * @return Provider
   */
  public static TerminologyProvider provider(String providerName) {
    ServiceLoader<TerminologyProvider> loader = ServiceLoader.load(TerminologyProvider.class);
    for (TerminologyProvider provider : loader) {
      if (providerName.equals(provider.getClass().getName())) {
        return provider;
      }
    }
    throw new ProviderNotFoundException("TerminologyProvider " + providerName + " not found");
  }
}
