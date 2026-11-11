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
package de.gematik.zts.termzilla.configuration;

import lombok.Data;

/**
 * Configuration properties for terminology service caching.
 *
 * <p>Provides default values for cache sizes and TTLs. Can be customized by creating an instance
 * with different values.
 */
@Data
public class CacheConfigProperties {

  private ExpansionCacheConfig expansion = new ExpansionCacheConfig();
  private ValidationCacheConfig validation = new ValidationCacheConfig();
  private LookupCacheConfig lookup = new LookupCacheConfig();
  private SearchCacheConfig search = new SearchCacheConfig();

  @Data
  public static class ExpansionCacheConfig {

    /** Maximum number of entries in the expansion cache */
    private int maxSize = 500;

    /** Time-to-live in minutes for expansion cache entries */
    private int ttlMinutes = 30;
  }

  @Data
  public static class ValidationCacheConfig {

    /** Maximum number of entries in the validation cache */
    private int maxSize = 2000;

    /** Time-to-live in minutes for validation cache entries */
    private int ttlMinutes = 15;
  }

  @Data
  public static class LookupCacheConfig {

    /** Maximum number of entries in the lookup cache */
    private int maxSize = 1000;

    /** Time-to-live in minutes for lookup cache entries */
    private int ttlMinutes = 60;
  }

  @Data
  public static class SearchCacheConfig {

    /** Maximum number of entries in the search cache */
    private int maxSize = 200;

    /** Time-to-live in minutes for search cache entries */
    private int ttlMinutes = 5;
  }
}
