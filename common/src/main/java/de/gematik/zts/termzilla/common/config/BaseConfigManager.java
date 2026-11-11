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
package de.gematik.zts.termzilla.common.config;

/**
 * Base class for module configuration managers. Provides configuration injection and management
 * capabilities. Subclasses should use the initialization-on-demand holder pattern for singleton
 * instances.
 *
 * @param <T> the type of configuration properties
 */
public abstract class BaseConfigManager<T> {

  private final ConfigurationInjector injector;
  private final T configProperties;

  protected BaseConfigManager() {
    this.injector = new ConfigurationInjector();
    configureInjector(this.injector); // Hook for subclasses to register converters
    this.configProperties = createConfigProperties();
    this.injector.inject(configProperties);
  }

  /**
   * Hook method to configure the injector before property injection. Subclasses can override this
   * to register custom converters.
   *
   * @param injector the configuration injector
   */
  protected void configureInjector(ConfigurationInjector injector) {
    // Default: no additional configuration
  }

  /**
   * Creates the configuration properties object specific to the module.
   *
   * @return a new instance of the configuration properties
   */
  protected abstract T createConfigProperties();

  /**
   * Returns the module name for logging and error messages.
   *
   * @return the module name
   */
  protected abstract String getModuleName();

  /**
   * Returns the configuration properties for this module.
   *
   * @return the configuration properties instance
   */
  public T getConfiguration() {
    return configProperties;
  }

  /**
   * Injects configuration values into an existing object.
   *
   * @param object the object to inject values into
   * @param <U> the type of the object
   * @return the object with injected values
   */
  public <U> U inject(U object) {
    return injector.inject(object);
  }

  // Expose injector for custom converter registration
  protected ConfigurationInjector getInjector() {
    return injector;
  }
}
