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
package de.gematik.zts.termzilla.api.constants;

/**
 * Constants for standard HTTP header names used across the terminology server.
 *
 * <p>Provides a single source of truth for HTTP header name strings to avoid duplication and typos
 * throughout the codebase.
 */
public class HttpHeaderConstants {

  private HttpHeaderConstants() {
    // Utility class, prevent instantiation
  }

  /** The {@code Accept} HTTP header, used to specify acceptable response media types. */
  public static final String ACCEPT = "Accept";

  /**
   * The {@code Accept-Language} HTTP header, used to express the client's preferred natural
   * language for the response.
   */
  public static final String ACCEPT_LANGUAGE = "Accept-Language";

  /** The {@code Content-Type} HTTP header, used to indicate the media type of the request body. */
  public static final String CONTENT_TYPE = "Content-Type";

  /**
   * The {@code ETag} HTTP header, used to identify a specific version of a resource for conditional
   * requests and cache validation.
   */
  public static final String ETAG = "ETag";

  /**
   * The {@code Cache-Control} HTTP header, used to specify directives for caching mechanisms in
   * requests and responses.
   */
  public static final String CACHE_CONTROL = "Cache-Control";

  /**
   * The {@code Vary} HTTP header, used to indicate which request headers were used to determine the
   * response, enabling correct caching of content-negotiated responses.
   */
  public static final String VARY = "Vary";

  /**
   * The {@code If-None-Match} HTTP header, used to make a conditional request that is only
   * fulfilled if the resource's {@code ETag} does not match any of the provided values.
   */
  public static final String IF_NONE_MATCH = "If-None-Match";

  /**
   * The {@code Authorization} HTTP header, used to supply credentials for authenticating the client
   * with the server.
   */
  public static final String AUTHORIZATION = "Authorization";

  /**
   * The {@code Location} HTTP header, used to indicate the URL to redirect to or the URL of a newly
   * created resource.
   */
  public static final String LOCATION = "Location";
}
