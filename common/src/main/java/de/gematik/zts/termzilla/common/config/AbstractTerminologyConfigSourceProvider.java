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

import io.smallrye.config.source.yaml.YamlConfigSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * Abstract base class for terminology-specific configuration source providers.
 *
 * <p>Discovers and loads YAML configuration files from the classpath as {@link ConfigSource}
 * instances. Provides convention-based defaults for standard terminology configurations while
 * allowing subclasses to override specific behavior.
 *
 * <p>Default conventions:
 *
 * <ul>
 *   <li>Configuration files: {@code {terminology}-application.yaml/yml}, {@code
 *       application.yaml/yml}
 *   <li>Property prefix: {@code {terminology}} (lowercase)
 *   <li>Valid URL patterns: {@code /termzilla-{terminology}/}, {@code /{terminology}/}, {@code
 *       {terminology}-application}
 * </ul>
 *
 * <p>Subclasses typically only need to provide the terminology name via the constructor. Override
 * protected methods to customize file discovery, validation, or property filtering.
 *
 * @see ConfigSourceProvider
 * @see YamlConfigSource
 */
@Slf4j
public abstract class AbstractTerminologyConfigSourceProvider implements ConfigSourceProvider {

  private static final int ORDINAL = 250;
  private final String terminology;

  /**
   * Constructs a provider using convention-based configuration.
   *
   * @param terminology the terminology name (e.g., "Fhir", "Loinc", "Snomed")
   */
  protected AbstractTerminologyConfigSourceProvider(String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the display name used in log messages.
   *
   * <p>Default: {@code "{Terminology}ConfigSourceProvider"}
   *
   * @return the terminology display name
   */
  protected String getTerminologyName() {
    return terminology + "ConfigSourceProvider";
  }

  /**
   * Returns the property key prefix for filtering configuration properties in logs.
   *
   * <p>Default: terminology name in lowercase
   *
   * @return the property prefix (e.g., "fhir", "loinc", "snomed")
   */
  protected String getPropertyPrefix() {
    return terminology.toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the ordered list of configuration file names to search for.
   *
   * <p>Files are searched in the specified order. The first valid match stops the search.
   *
   * <p>Default: {@code {terminology}-application.yaml/yml}, then {@code application.yaml/yml}
   *
   * @return array of configuration file names
   */
  protected String[] getConfigFiles() {
    String lower = terminology.toLowerCase(Locale.ROOT);
    return new String[] {
      lower + "-application.yaml", lower + "-application.yml", "application.yaml", "application.yml"
    };
  }

  /**
   * Validates whether the given URL represents a configuration file for this terminology.
   *
   * <p>Default implementation checks URL path for:
   *
   * <ul>
   *   <li>{@code /termzilla-{terminology}/}
   *   <li>{@code /{terminology}/}
   *   <li>{@code {terminology}-application}
   * </ul>
   *
   * @param yamlUrl the URL to validate
   * @return {@code true} if the URL is valid for this terminology
   */
  protected boolean isValidConfigFile(URL yamlUrl) {
    String urlPath = yamlUrl.toString();
    String lower = terminology.toLowerCase(Locale.ROOT);
    return urlPath.contains("/termzilla-" + lower + "/")
        || urlPath.contains("/" + lower + "/")
        || urlPath.contains(lower + "-application");
  }

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
    List<ConfigSource> configSources = new ArrayList<>();

    log.info("=== {}: Searching for config files ===", getTerminologyName());

    try {
      loadConfigSources(forClassLoader, configSources);

      if (configSources.isEmpty()) {
        handleNoConfigFound();
      }

    } catch (IOException e) {
      log.error(
          "Failed to load {} YAML configuration: {}", getTerminologyName(), e.getMessage(), e);
    }

    return configSources;
  }

  /**
   * Searches for and loads configuration sources from the classpath.
   *
   * <p>Iterates through configured file names and attempts to load matching resources. Stops
   * searching once a valid configuration is successfully loaded.
   *
   * @param forClassLoader the classloader to use for resource lookup
   * @param configSources the list to populate with loaded configuration sources
   * @throws IOException if an error occurs during resource enumeration
   */
  private void loadConfigSources(ClassLoader forClassLoader, List<ConfigSource> configSources)
      throws IOException {
    if (forClassLoader == null) {
      log.warn("ClassLoader is null, cannot load configuration sources");
      return;
    }

    for (String configFile : getConfigFiles()) {
      Enumeration<URL> resources = forClassLoader.getResources(configFile);

      if (resources == null) {
        log.warn("getResources() returned null for: {}", configFile);
        continue;
      }

      while (resources.hasMoreElements()) {
        URL yamlUrl = resources.nextElement();
        log.debug("Found config file: {}", yamlUrl);

        if (isValidConfigFile(yamlUrl) && tryLoadConfigSource(yamlUrl, configSources)) {
          return; // Successfully loaded, stop searching
        }
      }

      if (!configSources.isEmpty()) {
        return; // Found config, stop trying other filenames
      }
    }
  }

  /**
   * Attempts to load a YAML configuration source from the specified URL.
   *
   * @param yamlUrl the URL of the YAML configuration file
   * @param configSources the list to add the loaded source to
   * @return {@code true} if loading succeeded
   */
  private boolean tryLoadConfigSource(URL yamlUrl, List<ConfigSource> configSources) {
    try {
      YamlConfigSource yamlSource = new YamlConfigSource(yamlUrl, ORDINAL);
      configSources.add(yamlSource);
      log.info("✓ Loaded {} configuration from: {}", getTerminologyName(), yamlUrl);

      logLoadedProperties(yamlSource);
      return true;

    } catch (IOException e) {
      log.error("✗ Failed to load: {} - {}", yamlUrl, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Logs all configuration properties matching the terminology's property prefix.
   *
   * @param yamlSource the loaded YAML configuration source
   */
  private void logLoadedProperties(YamlConfigSource yamlSource) {
    String prefix = getPropertyPrefix();
    yamlSource
        .getPropertyNames()
        .forEach(
            key -> {
              if (key.startsWith(prefix)) {
                log.info("  {} = {}", key, yamlSource.getValue(key));
              }
            });
  }

  /**
   * Handles the case where no configuration file was found.
   *
   * <p>Logs a warning and attempts to verify if a generic {@code application.yaml} is accessible
   * via direct classloader access.
   */
  private void handleNoConfigFound() {
    log.warn("✗ Warning: No {} configuration file found!", getTerminologyName());

    try (InputStream is =
        this.getClass().getClassLoader().getResourceAsStream("application.yaml")) {
      if (is != null) {
        log.debug("Found via direct classloader access");
      }
    } catch (IOException e) {
      log.debug("Could not access application.yaml via direct classloader", e);
    }
  }
}
