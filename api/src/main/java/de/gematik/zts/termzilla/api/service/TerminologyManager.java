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

import java.util.Set;

/**
 * Central access point for all terminology services provided by a single terminology source.
 *
 * <p>Each implementation represents one terminology (e.g. SNOMED CT, LOINC, or a FHIR package) and
 * exposes service interfaces for CodeSystem, ValueSet, and ConceptMap operations. A manager may be
 * disabled via configuration, in which case {@link #isInitialized()} returns {@code false} and the
 * service accessors may return {@code null}.
 */
public interface TerminologyManager {

  /**
   * Returns whether this manager has been successfully initialized and is ready to serve requests.
   *
   * @return {@code true} if the manager is initialized, {@code false} otherwise
   */
  boolean isInitialized();

  /**
   * Returns the human-readable name identifying this terminology manager.
   *
   * @return the name of this terminology manager
   */
  String getTerminologyManagerName();

  /**
   * Returns the set of scopes this manager is responsible for.
   *
   * @return the supported scopes
   */
  Set<String> getSupportedScopes();

  /**
   * Computes a hash representing the current configuration and underlying data state.
   *
   * <p>The hash changes whenever the effective configuration or the loaded terminology data
   * changes, allowing callers to detect whether a reload or cache invalidation is required.
   *
   * @return a hex-encoded hash of the combined configuration and data state
   */
  String getConfigurationAndDataStateHash();

  /**
   * Returns the service interface for CodeSystem operations.
   *
   * @return the {@link CodeSystemSvcInterface}, or {@code null} if not initialized
   */
  CodeSystemSvcInterface cs();

  /**
   * Returns the service interface for ValueSet operations.
   *
   * @return the {@link ValueSetSvcInterface}, or {@code null} if not initialized
   */
  ValueSetSvcInterface vs();

  /**
   * Returns the service interface for ConceptMap operations.
   *
   * @return the {@link ConceptMapSvcInterface}, or {@code null} if not initialized
   */
  ConceptMapSvcInterface cm();
}
