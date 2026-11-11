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

import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.common.helper.CachingKeyHelper;
import java.io.Serializable;

/**
 * Cache key for search operations (CodeSystem, ValueSet, ConceptMap searches).
 *
 * <p>Uses scope and a string-based cache key derived from the complete request URL. This ensures
 * cache hits for identical searches while being simple and reliable.
 */
public record SearchCacheKey(String scope, String cacheKeyString) implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Creates a search cache key from scope and request URL. */
  public SearchCacheKey(String scope, TerminologyResourceSearchParameter params) {
    this(scope, CachingKeyHelper.createKey(params.createUniqueByteArrayFromParameterValues()));
  }
}
