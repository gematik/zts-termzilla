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
package de.gematik.zts.termzilla.common.manager;

import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Base class for terminology managers with shared lifecycle and service access behavior.
 *
 * <p>Subclasses must implement factory methods for creating their specific service implementations.
 * The abstract base class orchestrates service initialization in the correct dependency order,
 * ensuring that {@link #cs()}, {@link #vs()}, and {@link #cm()} are safe to call during service
 * construction according to the initialization sequence.
 */
public abstract class AbstractTerminologyManager implements TerminologyManager {

  private boolean initialized;
  private String terminologyManagerName;
  private CodeSystemSvcInterface codeSystemService;
  private ValueSetSvcInterface valueSetService;
  private ConceptMapSvcInterface conceptMapService;

  /**
   * Initializes the manager and creates all services in dependency order.
   *
   * <p>When {@code shouldStart} is {@code true}, services are created by calling the abstract
   * factory methods in the following order:
   *
   * <ol>
   *   <li>{@link #createCodeSystemService()} - CodeSystem service is created first
   *   <li>{@link #createValueSetService()} - ValueSet service can safely access {@link #cs()}
   *   <li>{@link #createConceptMapService()} - ConceptMap service can safely access {@link #cs()}
   *       and {@link #vs()}
   * </ol>
   *
   * <p>When {@code shouldStart} is {@code false}, all services remain {@code null} and {@link
   * #isInitialized()} returns {@code false}.
   *
   * @param terminologyManagerName the human-readable manager name
   * @param shouldStart whether to initialize and create the services
   */
  protected final void initializeManagerAndServices(
      String terminologyManagerName, boolean shouldStart) {

    this.terminologyManagerName = terminologyManagerName;

    if (!shouldStart) {
      this.initialized = false;
      this.codeSystemService = null;
      this.valueSetService = null;
      this.conceptMapService = null;
      return;
    }

    // Create services in dependency order
    // Each subsequent service can safely access previously created services via cs()/vs()/cm()
    this.codeSystemService = createCodeSystemService();
    this.valueSetService = createValueSetService();
    this.conceptMapService = createConceptMapService();

    this.initialized = true;
  }

  /**
   * Creates the CodeSystem service implementation.
   *
   * <p>This method is called first during service initialization. The returned service should not
   * attempt to access {@link #vs()} or {@link #cm()} as they are not yet created.
   *
   * @return the CodeSystem service implementation, never {@code null}
   */
  protected abstract CodeSystemSvcInterface createCodeSystemService();

  /**
   * Creates the ValueSet service implementation.
   *
   * <p>This method is called after {@link #createCodeSystemService()}. The returned service can
   * safely access {@link #cs()} but should not attempt to access {@link #cm()} as it is not yet
   * created.
   *
   * @return the ValueSet service implementation, never {@code null}
   */
  protected abstract ValueSetSvcInterface createValueSetService();

  /**
   * Creates the ConceptMap service implementation.
   *
   * <p>This method is called last during service initialization. The returned service can safely
   * access both {@link #cs()} and {@link #vs()}.
   *
   * @return the ConceptMap service implementation, never {@code null}
   */
  protected abstract ConceptMapSvcInterface createConceptMapService();

  /**
   * Creates a hash from configuration and data-state hash inputs using the shared algorithm.
   *
   * @param configurationState textual representation of effective configuration state
   * @param dataState hash or textual representation of loaded data state
   * @return a hex-encoded SHA-256 hash of the combined state
   */
  protected static String buildConfigurationAndDataStateHash(
      String configurationState, String dataState) {
    return DigestUtils.sha256Hex(DigestUtils.sha256Hex(configurationState) + dataState);
  }

  @Override
  public final boolean isInitialized() {
    return initialized;
  }

  @Override
  public final String getTerminologyManagerName() {
    return terminologyManagerName;
  }

  @Override
  public final CodeSystemSvcInterface cs() {
    return codeSystemService;
  }

  @Override
  public final ValueSetSvcInterface vs() {
    return valueSetService;
  }

  @Override
  public final ConceptMapSvcInterface cm() {
    return conceptMapService;
  }
}
