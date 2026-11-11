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

import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import java.nio.file.Path;

/** Provides factory methods for creating configured {@link TerminologyManager} instances. */
public interface TerminologyProvider {

  /**
   * Creates a terminology manager for the given working directory and default scope.
   *
   * @param baseWorkingDirectory the base directory used for indexes, caches, and temporary files
   * @param defaultScope the default scope under which terminology operations are executed
   * @return a fully configured {@link TerminologyManager}
   * @throws InvalidConfigurationException if required configuration is missing or invalid
   */
  TerminologyManager create(Path baseWorkingDirectory, String defaultScope)
      throws InvalidConfigurationException;
}
