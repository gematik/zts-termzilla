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

import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.common.helper.CachingKeyHelper;
import java.io.Serializable;

/**
 * Cache key for CodeSystem $validate-code operations.
 *
 * <p>Captures all parameters that affect the validation result.
 */
public record CsValidationCacheKey(String scope, String cacheKeyString) implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a cache key from CodeSystem validation parameters.
   *
   * @param scope The terminology scope (tenant)
   * @param params The validation parameters
   */
  public CsValidationCacheKey(String scope, CsValidateCodeParameter params) {
    this(scope, CachingKeyHelper.createKey(params.createUniqueByteArrayFromParameterValues()));
  }
}
