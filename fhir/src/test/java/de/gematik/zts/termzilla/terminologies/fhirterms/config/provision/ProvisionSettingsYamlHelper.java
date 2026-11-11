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
package de.gematik.zts.termzilla.terminologies.fhirterms.config.provision;

import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import java.util.List;

/** Helper for creating YAML test data for {@link ProvisionSettings}. */
public class ProvisionSettingsYamlHelper {

  private ProvisionSettingsYamlHelper() {
    // Utility class
  }

  /** Creates a complete YAML configuration matching the example file. */
  public static String createCompleteYaml() {
    return """
        provision-settings:
          packages:
            - id: bfarm.terminologien.orphacodes

              version: "*"
              mode: supported

            - id: bfarm.terminologien.loinc

              mode: supported

            - id: bfarm.terminologien.ops

              version: "^2022.0.0"
              mode: predefined
              supported:
                - "http://fhir.de/CodeSystem/bfarm/ops"

            - id: bfarm.terminologien.ops

              version: "^2023.0.0"
              mode: supported

            - id: bfarm.terminologien.icd10gm

              version: "*"
              mode: predefined
              supported:
                - "https://terminologien.bfarm.de/fhir/ValueSet/*"

        """;
  }

  /** Creates a YAML with default settings. */
  public static String createYamlWithDefaultSettings(ProvisionMode mode) {
    return String.format(
        """
        provision-settings:
          default:
            mode: %s
        """,
        mode.name().toLowerCase());
  }

  /** Creates a YAML with a single package. */
  public static String createYamlWithSinglePackage(
      String packageId,
      String version,
      ProvisionMode mode,
      List<String> supported,
      List<String> predefined) {
    StringBuilder yaml = new StringBuilder();
    yaml.append("provision-settings:\n");
    yaml.append("  packages:\n");
    yaml.append("    - id: ").append(packageId).append("\n");

    if (version != null) {
      yaml.append("      version: \"").append(version).append("\"\n");
    }

    if (mode != null) {
      yaml.append("      mode: ").append(mode.name().toLowerCase()).append("\n");
    }

    if (supported != null && !supported.isEmpty()) {
      yaml.append("      supported:\n");
      for (String url : supported) {
        yaml.append("        - \"").append(url).append("\"\n");
      }
    }

    if (predefined != null && !predefined.isEmpty()) {
      yaml.append("      predefined:\n");
      for (String url : predefined) {
        yaml.append("        - \"").append(url).append("\"\n");
      }
    }

    return yaml.toString();
  }

  /** Creates a YAML with a single package using string mode (for case insensitivity testing). */
  public static String createYamlWithSinglePackage(
      String packageId,
      String version,
      String mode,
      List<String> supported,
      List<String> predefined) {
    StringBuilder yaml = new StringBuilder();
    yaml.append("provision-settings:\n");
    yaml.append("  packages:\n");
    yaml.append("    - id: ").append(packageId).append("\n");

    if (version != null) {
      yaml.append("      version: \"").append(version).append("\"\n");
    }

    if (mode != null) {
      yaml.append("      mode: ").append(mode).append("\n");
    }

    if (supported != null && !supported.isEmpty()) {
      yaml.append("      supported:\n");
      for (String url : supported) {
        yaml.append("        - \"").append(url).append("\"\n");
      }
    }

    if (predefined != null && !predefined.isEmpty()) {
      yaml.append("      predefined:\n");
      for (String url : predefined) {
        yaml.append("        - \"").append(url).append("\"\n");
      }
    }

    return yaml.toString();
  }

  /** Creates a YAML with multiple versions of the same package. */
  public static String createYamlWithMultipleVersionsOfSamePackage() {
    return """
        provision-settings:
          packages:
            - id: bfarm.terminologien.ops

              version: "^2022.0.0"
              mode: predefined
              supported:
                - "http://fhir.de/CodeSystem/bfarm/ops"

            - id: bfarm.terminologien.ops

              version: "^2023.0.0"
              mode: supported
        """;
  }

  /** Creates a YAML with a package without version constraint. */
  public static String createYamlWithPackageWithoutVersion(String packageId, ProvisionMode mode) {
    return String.format(
        """
        provision-settings:
          packages:
            - id: %s

              mode: %s
        """,
        packageId, mode.name().toLowerCase());
  }

  /** Creates a YAML with an invalid mode value. */
  public static String createYamlWithInvalidMode(String packageId, String invalidMode) {
    return String.format(
        """
        provision-settings:
          packages:
            - id: %s

              mode: %s
        """,
        packageId, invalidMode);
  }

  /** Creates a YAML without default settings. */
  public static String createYamlWithoutDefaultSettings() {
    return """
        provision-settings:
          packages:
            - id: test.package

              version: "*"
              mode: supported
        """;
  }

  /** Creates a YAML with only default settings. */
  public static String createYamlWithOnlyDefaultSettings() {
    return """
        provision-settings:
          default:
            mode: supported
        """;
  }
}
