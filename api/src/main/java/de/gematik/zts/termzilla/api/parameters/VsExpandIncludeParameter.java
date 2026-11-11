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
package de.gematik.zts.termzilla.api.parameters;

import java.util.Objects;
import lombok.Builder;

/**
 * Parameters for expanding a single {@code include} or {@code exclude} element of a ValueSet
 * definition.
 *
 * @param includeInactiveConcepts whether to include inactive concepts
 * @param version the version to use for expansion ({@code system-version})
 * @param checkVersion the version to check against ({@code check-system-version})
 * @param forceVersion the version to force ({@code force-system-version})
 * @param filter the text filter applied to the expansion
 * @param processingContext the context in which the element is processed
 */
@Builder
public record VsExpandIncludeParameter(
    boolean includeInactiveConcepts,
    String version,
    String checkVersion,
    String forceVersion,
    String filter,
    ProcessingContext processingContext) {

  /**
   * Creates the parameters for expanding an include or exclude element.
   *
   * @throws NullPointerException if {@code processingContext} is {@code null}
   */
  public VsExpandIncludeParameter {
    Objects.requireNonNull(processingContext, "processingContext must not be null");
  }
}
