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
package de.gematik.zts.termzilla.terminologies.fhirterms.config;

import de.gematik.zts.termzilla.common.config.AbstractTerminologyConfigSourceProvider;

/**
 * Configuration source provider for FHIR terminology services.
 *
 * <p>Searches for FHIR-specific YAML configuration files in the classpath, prioritizing files with
 * the "fhir-application" naming pattern.
 *
 * @see AbstractTerminologyConfigSourceProvider
 */
public class FhirConfigSourceProvider extends AbstractTerminologyConfigSourceProvider {

  public FhirConfigSourceProvider() {
    super("Fhir");
  }

  @Override
  protected String getPropertyPrefix() {
    return "fhirterms"; // Only override because it differs from convention
  }
}
