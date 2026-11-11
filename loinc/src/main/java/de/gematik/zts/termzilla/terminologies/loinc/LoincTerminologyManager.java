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
package de.gematik.zts.termzilla.terminologies.loinc;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.service.CodeSystemSvcInterface;
import de.gematik.zts.termzilla.api.service.ConceptMapSvcInterface;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.common.manager.AbstractTerminologyManager;
import de.gematik.zts.termzilla.terminologies.loinc.config.ConfigManager;
import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import java.nio.file.Path;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link TerminologyManager} implementation for LOINC.
 *
 * <p>Initializes and wires all LOINC subsystems: the in-memory data model ({@link Loinc}) and the
 * service implementations for CodeSystem, ValueSet, and ConceptMap operations. Initialization is
 * conditional on the {@code loinc.start} configuration flag; if disabled, all service references
 * remain {@code null} and {@link #isInitialized()} returns {@code false}.
 */
@Slf4j
public class LoincTerminologyManager extends AbstractTerminologyManager {

  private static final String TERMINOLOGY_MANAGER_NAME = "Basic LOINC Terminology Manager";

  @Getter private final LoincConfigProperties properties;
  private final String defaultScope;

  @Getter private final Loinc loinc;

  @Getter private final FhirContext context;

  /**
   * Creates and fully initializes a new {@code LoincTerminologyManager}.
   *
   * <p>If {@code configProperties} is {@code null}, configuration is loaded via {@link
   * ConfigManager}. LOINC subsystems are only started when the resolved configuration has {@code
   * loinc.start} set to {@code true}.
   *
   * @param baseWorkingDirectory the base directory used for indexes and temporary files
   * @param defaultScope the default scope under which this manager operates
   * @param configProperties the LOINC configuration to use, or {@code null} to load from the
   *     default {@link ConfigManager}
   * @throws InvalidConfigurationException if required configuration values are missing or invalid
   */
  public LoincTerminologyManager(
      Path baseWorkingDirectory, String defaultScope, LoincConfigProperties configProperties)
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

    // Initialize data model when LOINC support is enabled
    boolean shouldStart = properties.isLoincStart();
    if (shouldStart) {
      this.loinc = new Loinc(properties);
      this.context = FhirContext.forR4Cached();
    } else {
      this.loinc = null;
      this.context = null;
    }

    // Initialize manager and create services in correct dependency order
    initializeManagerAndServices(TERMINOLOGY_MANAGER_NAME, shouldStart);
  }

  @Override
  protected CodeSystemSvcInterface createCodeSystemService() {
    return new LoincCodeSystemService(this);
  }

  @Override
  protected ValueSetSvcInterface createValueSetService() {
    // Can now safely call this.cs() during ValueSet service construction
    return new LoincValueSetService(this);
  }

  @Override
  protected ConceptMapSvcInterface createConceptMapService() {
    return new LoincConceptMapService();
  }

  @Override
  public Set<String> getSupportedScopes() {
    return Set.of(defaultScope);
  }

  @Override
  public String getConfigurationAndDataStateHash() {
    return buildConfigurationAndDataStateHash(properties.toString(), loinc.getReleaseDataHash());
  }
}
