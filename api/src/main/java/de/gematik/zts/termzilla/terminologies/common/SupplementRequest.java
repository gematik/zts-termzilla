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
package de.gematik.zts.termzilla.terminologies.common;

import java.util.Objects;
import org.hl7.fhir.r4.model.CodeSystem;

/**
 * A normalized, resolved CodeSystem supplement request for a ValueSet expansion.
 *
 * <p>A supplement enriches metadata (display, designations) of codes that already exist in a base
 * CodeSystem. It never changes ValueSet membership. This record carries the resolved supplement
 * {@link CodeSystem} together with the canonical identity it was requested under, so the expansion
 * can both apply its designations and record it in {@code ValueSet.expansion.parameter}.
 *
 * @param canonicalUrl the canonical URL of the supplement CodeSystem
 * @param version the version of the supplement CodeSystem (may be {@code null} if not pinned)
 * @param baseSystemUrl the canonical URL of the base CodeSystem the supplement applies to
 * @param supplement the resolved supplement CodeSystem resource
 */
public record SupplementRequest(
    String canonicalUrl, String version, String baseSystemUrl, CodeSystem supplement) {

  /**
   * The expansion parameter name suffix used to record an applied supplement.
   *
   * <p>The full parameter name is {@code [base CodeSystem canonical]#supplement}.
   */
  public static final String SUPPLEMENT_PARAMETER_SUFFIX = "#supplement";

  public SupplementRequest {
    Objects.requireNonNull(canonicalUrl, "canonicalUrl must not be null");
    Objects.requireNonNull(baseSystemUrl, "baseSystemUrl must not be null");
    Objects.requireNonNull(supplement, "supplement must not be null");
  }

  /**
   * The expansion parameter name under which this supplement is recorded.
   *
   * @return {@code [baseSystemUrl]#supplement}
   */
  public String expansionParameterName() {
    return baseSystemUrl + SUPPLEMENT_PARAMETER_SUFFIX;
  }

  /**
   * The expansion parameter value under which this supplement is recorded.
   *
   * @return {@code canonicalUrl|version}, or just {@code canonicalUrl} when no version is pinned
   */
  public String expansionParameterValue() {
    return version != null && !version.isEmpty() ? canonicalUrl + "|" + version : canonicalUrl;
  }
}
