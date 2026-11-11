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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.semver4j.Semver;
import org.semver4j.SemverException;

/**
 * Provision settings for a specific FHIR package version range.
 *
 * <p>Defines how resources within a package should be provisioned, with support for version
 * constraints, default modes, and resource-specific exceptions.
 */
@Getter
@Setter
@ToString
public class PackageProvisionSettings {

  /**
   * Version constraint for this configuration.
   *
   * <p>Supports exact versions ({@code "4.0.1"}), wildcards ({@code "*"}), and semver ranges
   * ({@code "^1.0.0"}, {@code "~1.2.3"}, {@code ">=1.0.0 <2.0.0"}).
   */
  private String version;

  /** Default provision mode for resources in this package. */
  private ProvisionMode mode;

  /**
   * Resource URLs that should be {@link ProvisionMode#SUPPORTED} regardless of the default mode.
   *
   * <p>Supports wildcard patterns (e.g., {@code "http://example.org/fhir/CodeSystem/*"}).
   */
  private List<String> supported = new ArrayList<>();

  /**
   * Resource URLs that should be {@link ProvisionMode#PREDEFINED} regardless of the default mode.
   *
   * <p>Supports wildcard patterns (e.g., {@code
   * "http://example.org/fhir/ValueSet/administrative-*"}).
   */
  private List<String> predefined = new ArrayList<>();

  /**
   * Determines the effective provision mode for a specific resource.
   *
   * <p>Checks exception lists first ({@link #supported}, {@link #predefined}), then falls back to
   * the default {@link #mode}.
   *
   * @param resourceUrl the canonical URL of the resource
   * @return the effective provision mode, defaults to {@link ProvisionMode#SUPPORTED} if no mode is
   *     configured
   */
  public ProvisionMode getEffectiveMode(String resourceUrl) {
    if (matchesPattern(resourceUrl, supported)) {
      return ProvisionMode.SUPPORTED;
    }
    if (matchesPattern(resourceUrl, predefined)) {
      return ProvisionMode.PREDEFINED;
    }

    return mode != null ? mode : ProvisionMode.SUPPORTED;
  }

  /**
   * Checks if the version constraint matches a given version using semantic versioning.
   *
   * <p>Falls back to exact string matching if semver parsing fails.
   *
   * @param actualVersion the version to check (e.g., {@code "1.5.0"})
   * @return {@code true} if the version matches the constraint, {@code false} otherwise
   */
  public boolean matchesVersion(String actualVersion) {
    if (version == null || version.equals("*")) {
      return true;
    }

    try {
      Semver semver = new Semver(actualVersion);
      return semver.satisfies(version);
    } catch (SemverException _) {
      // If version parsing fails, try exact string match as fallback
      return version.equals(actualVersion);
    }
  }

  /**
   * Checks if a URL matches any pattern in the given list.
   *
   * @param url the URL to check
   * @param patterns the list of patterns to match against
   * @return {@code true} if the URL matches any pattern, {@code false} otherwise
   */
  private boolean matchesPattern(String url, List<String> patterns) {
    if (patterns == null || patterns.isEmpty()) {
      return false;
    }

    for (String pattern : patterns) {
      if (matchesWildcard(url, pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a URL matches a wildcard pattern.
   *
   * <p>Converts wildcard patterns to regular expressions for matching.
   *
   * @param url the URL to check
   * @param pattern the wildcard pattern (e.g., {@code "http://example.org/*"})
   * @return {@code true} if the URL matches the pattern, {@code false} otherwise
   */
  private boolean matchesWildcard(String url, String pattern) {
    if (pattern.equals(url)) {
      return true;
    }

    // Convert wildcard pattern to regex
    // Escape special regex characters except *
    String regex =
        pattern
            .replace(".", "\\.")
            .replace("?", "\\?")
            .replace("+", "\\+")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("|", "\\|")
            .replace("*", ".*");

    return url.matches(regex);
  }
}
