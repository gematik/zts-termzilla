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

/**
 * Context in which a terminology processes a part of a request.
 *
 * <p>The context is handed from the core to the terminology modules, so they can decide which
 * content they may use. It is designed to be extended by further aspects, e.g. the permissions of
 * the requesting client.
 *
 * @param origin the origin of the processed resource
 */
public record ProcessingContext(ResourceOrigin origin) {

  /**
   * Creates a processing context.
   *
   * @param origin the origin of the processed resource
   * @throws NullPointerException if {@code origin} is {@code null}
   */
  public ProcessingContext {
    Objects.requireNonNull(origin, "origin must not be null");
  }

  /**
   * Creates the context for a resource that was provided within the request.
   *
   * @return the context with origin {@link ResourceOrigin#PROVIDED}
   */
  public static ProcessingContext provided() {
    return new ProcessingContext(ResourceOrigin.PROVIDED);
  }

  /**
   * Creates the context for a resource that is part of the service's content.
   *
   * @return the context with origin {@link ResourceOrigin#STORED}
   */
  public static ProcessingContext stored() {
    return new ProcessingContext(ResourceOrigin.STORED);
  }

  /**
   * Indicates whether the processed resource is part of the service's content.
   *
   * @return {@code true} if the origin is {@link ResourceOrigin#STORED}
   */
  public boolean isStored() {
    return origin == ResourceOrigin.STORED;
  }
}
