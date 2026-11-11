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

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregates the different language sources that influence designation selection during expansion.
 *
 * <p>When a terminology operation needs to choose which designations to include, several language
 * sources may apply simultaneously. This class bundles them in a single parameter object so that
 * the selection logic can evaluate them according to a defined priority order.
 *
 * @see LanguageParameter
 */
@Getter
@Setter
@Builder
public class LanguageSelectionParameter implements Serializable {

  /** Language requested via the FHIR {@code displayLanguage} $expand operation parameter. */
  private LanguageParameter displayLanguageParameter;

  /** Language preferences derived from the HTTP {@code Accept-Language} request header. */
  private LanguageParameter acceptLanguageHeader;

  /** Server-wide default language configured in the application properties. */
  private LanguageParameter systemDefaultLanguage;

  /** Language declared on the ValueSet resource itself (e.g. {@code ValueSet.language}). */
  private LanguageParameter vsLanguage;
}
