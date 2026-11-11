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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fields;

public abstract class BaseFieldNames {

  protected BaseFieldNames() {
    // private constructor to prevent instantiation
  }

  public static final String SEPARATOR = "_";

  // Feldpräfixe
  public static final String NAME_SCOPE = "scope";
  public static final String NAME_PROVISION_MODE = "provisionmode";
  public static final String NAME_TYPE = "_type";

  // Suffixe
  public static final String SUFFIX_MISSING = "missing";
  public static final String SUFFIX_TOKEN_LOWERCASE_EXACT = "lowercaseexact";
  public static final String SUFFIX_URI_EXACT = "uriexact";
  public static final String SUFFIX_URI_SEARCH = "urisearch";
  public static final String SUFFIX_URI_SCHEME_LOWERCASE_EXACT = "urischemelowercaseexact";
  public static final String SUFFIX_URL_HOST_LOWERCASE_EXACT = "urlhostlowercaseexact";
  public static final String SUFFIX_URL_PORT_EXACT = "urlportexact";
  public static final String SUFFIX_URL_PATH_EXACT = "urlpathexact";
  public static final String SUFFIX_URL_PATH_BELOW_EXACT = "urlpathbelowexact";
  public static final String SUFFIX_URL_QUERY_EXACT = "urlqueryexact";
  public static final String SUFFIX_REFERENCE_EXACT = "referenceexact";
  public static final String SUFFIX_REFERENCE_SEARCH = "referencesearch";
  public static final String SUFFIX_TOKEN_EXACT = "tokenexact";
  public static final String SUFFIX_TOKEN_SEARCH = "tokensearch";
  public static final String SUFFIX_DATE_INSTANT = "date_instant";
  public static final String SUFFIX_DATE_START = "date_period_start";
  public static final String SUFFIX_DATE_END = "date_period_end";
  public static final String SUFFIX_DATE_SEARCH = "date_search";
  public static final String SUFFIX_LOWERCASE_EXACT = "lowercaseexact";
  public static final String SUFFIX_SORT = "sort";
  public static final String SUFFIX_STRING_EXACT = "exact";
  public static final String SUFFIX_STRING_SEARCH = "search";
  public static final String SUFFIX_STRING_CONTAINS = "contains";
  public static final String SUFFIX_STRING_STANDARD = "standard";
}
