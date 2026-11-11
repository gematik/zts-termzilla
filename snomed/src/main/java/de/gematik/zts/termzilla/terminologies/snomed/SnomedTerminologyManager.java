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
package de.gematik.zts.termzilla.terminologies.snomed;

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.common.manager.AbstractTerminologyManager;
import de.gematik.zts.termzilla.terminologies.snomed.config.ConfigManager;
import de.gematik.zts.termzilla.terminologies.snomed.config.SnomedConfigProperties;
import de.gematik.zts.termzilla.terminologies.snomed.scg.DemisSCGProcessor;
import java.nio.file.Path;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.snomed.languages.scg.SCGExpressionParser;
import org.snomed.languages.scg.SCGObjectFactory;

/**
 * {@link TerminologyManager} implementation for SNOMED CT.
 *
 * <p>Initializes and wires all SNOMED CT subsystems: the in-memory data model ({@link Snomed}), the
 * SCG expression parser, and the service implementations for CodeSystem, ValueSet, and ConceptMap
 * operations. Initialization is conditional on the {@code snomed.start} configuration flag; if
 * disabled, all service references remain {@code null} and {@link #isInitialized()} returns {@code
 * false}.
 */
@Slf4j
public class SnomedTerminologyManager extends AbstractTerminologyManager {

  private static final String TERMINOLOGY_MANAGER_NAME = "Basic SNOMED Terminology Manager";

  @Getter private final SnomedConfigProperties properties;

  private final String defaultScope;

  @Getter private final boolean expressionProcessingEnabled;

  @Getter private final boolean restrictedMode;

  @Getter private final Snomed snomed;
  @Getter private final SCGExpressionParser expressionParser;
  @Getter private final DemisSCGProcessor scgProcessor;

  /**
   * Creates and fully initializes a new {@code SnomedTerminologyManager}.
   *
   * <p>If {@code configProperties} is {@code null}, configuration is loaded via {@link
   * ConfigManager}. SNOMED CT subsystems are only started when the resolved configuration has
   * {@code snomed.start} set to {@code true}.
   *
   * @param baseWorkingDirectory the base directory used for indexes and temporary files
   * @param defaultScope the default scope under which this manager operates
   * @param configProperties the SNOMED configuration to use, or {@code null} to load from the
   *     default {@link ConfigManager}
   * @throws InvalidConfigurationException if required configuration values are missing or invalid
   */
  public SnomedTerminologyManager(
      Path baseWorkingDirectory, String defaultScope, SnomedConfigProperties configProperties)
      throws InvalidConfigurationException {

    // Initialize configuration
    if (configProperties != null) {
      this.properties = configProperties;
    } else {
      this.properties = new ConfigManager().getConfiguration();
    }

    if (properties == null) {
      throw new InvalidConfigurationException("Config properties must not be null");
    }

    // Apply base working directory to configuration
    if (baseWorkingDirectory == null) {
      throw new InvalidConfigurationException("BaseWorkingPath must not be null");
    } else {
      this.properties.setBaseWorkingDirectory(baseWorkingDirectory);
    }

    if (StringUtils.isEmpty(defaultScope)) {
      throw new InvalidConfigurationException("Default scope must not be empty");
    }
    this.defaultScope = defaultScope;

    // Initialize data model and SNOMED-specific features when enabled
    boolean shouldStart = properties.isSnomedStart();
    if (shouldStart) {
      this.snomed = new Snomed(properties);
      this.restrictedMode = properties.isRestrictedMode();
      this.expressionProcessingEnabled = resolveExpressionProcessingEnabled(properties);
      this.expressionParser =
          expressionProcessingEnabled ? new SCGExpressionParser(new SCGObjectFactory()) : null;
      this.scgProcessor = expressionProcessingEnabled ? new DemisSCGProcessor(snomed) : null;

      logFeatureState();
    } else {
      this.snomed = null;
      this.restrictedMode = false;
      this.expressionProcessingEnabled = false;
      this.expressionParser = null;
      this.scgProcessor = null;
    }

    // Initialize manager and create services in correct dependency order
    initializeManagerAndServices(TERMINOLOGY_MANAGER_NAME, shouldStart);
  }

  /** Logs which optional SNOMED features are restricted or disabled. */
  private void logFeatureState() {
    if (restrictedMode) {
      log.info(
          "SNOMED restricted mode is enabled: concept relationships and definition status are not"
              + " exposed.");
    }
    if (!expressionProcessingEnabled) {
      log.info("SNOMED expression processing is disabled by configuration.");
    }
  }

  /**
   * Determines whether expression processing is effectively enabled.
   *
   * <p>Restricted mode always disables expression processing, because SCG validation against the
   * MRCM and term rendering expose concept relationships.
   *
   * @param properties the SNOMED configuration
   * @return {@code true} if expression processing is enabled and restricted mode is disabled
   */
  static boolean resolveExpressionProcessingEnabled(SnomedConfigProperties properties) {
    if (properties.isRestrictedMode()) {
      if (properties.isExpressionProcessingEnabled()) {
        log.warn(
            "SNOMED expression processing is enabled by configuration, but is disabled because"
                + " restricted mode is active.");
      }
      return false;
    }
    return properties.isExpressionProcessingEnabled();
  }

  /** {@inheritDoc} */
  @Override
  protected CodeSystemSvcInterface createCodeSystemService() {
    return new SnomedCodeSystemService(this);
  }

  /** {@inheritDoc} */
  @Override
  protected ValueSetSvcInterface createValueSetService() {
    // Can now safely call this.cs() during ValueSet service construction
    return new SnomedValueSetService(this);
  }

  /** {@inheritDoc} */
  @Override
  protected ConceptMapSvcInterface createConceptMapService() {
    return new SnomedConceptMapService();
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getSupportedScopes() {
    return Set.of(defaultScope);
  }

  /** {@inheritDoc} */
  @Override
  public String getConfigurationAndDataStateHash() {
    return buildConfigurationAndDataStateHash(properties.toString(), snomed.getReleaseDataHash());
  }
}
