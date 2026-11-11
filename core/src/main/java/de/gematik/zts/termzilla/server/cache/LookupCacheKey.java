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
package de.gematik.zts.termzilla.server.cache;

import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.common.helper.CachingKeyHelper;
import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable cache key for {@code CodeSystem $lookup} operations.
 *
 * <p>Uniquely identifies a lookup result by combining the terminology scope and a SHA-256 hash over
 * all {@link CsLookupParameter} values that influence the result. Two instances are equal when they
 * represent the same scope and parameter combination.
 */
public record LookupCacheKey(String scope, String cacheKeyString) implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates a new cache key for the given lookup parameters.
   *
   * @param scope the terminology scope (tenant identifier)
   * @param params the lookup parameters used to compute the cache key hash
   */
  public LookupCacheKey(String scope, CsLookupParameter params) {
    this(scope, CachingKeyHelper.createKey(params.createUniqueByteArrayFromParameterValues()));
  }
}
