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
package de.gematik.zts.termzilla.server.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal security interceptor for the terminology server.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Request size validation (when configured)
 *   <li>Basic parameter length validation (when configured)
 * </ul>
 *
 * <p>Validation can be disabled by setting:
 *
 * <ul>
 *   <li>{@code maxRequestSizeBytes = 0} to disable request size validation
 *   <li>{@code maxParameterLength = 0} to disable parameter length validation
 * </ul>
 *
 * <p>Note: Lucene query sanitization is handled at the point of use (in query preparation methods)
 * rather than here, as most parameters are used with safe constructors like TermQuery.
 */
@Slf4j
@Interceptor
public class BasicSecurityInterceptor {

  private final ConfigProperties configProperties;

  /**
   * Creates a new BasicSecurityInterceptor with the given configuration.
   *
   * @param configProperties the configuration properties
   */
  public BasicSecurityInterceptor(ConfigProperties configProperties) {
    this.configProperties = configProperties;

    if (configProperties.getMaxRequestSizeBytes() == 0) {
      log.info("Request size validation is disabled (maxRequestSizeBytes = 0)");
    } else {
      log.info(
          "Request size validation enabled: max {} bytes",
          configProperties.getMaxRequestSizeBytes());
    }

    if (configProperties.getMaxParameterLength() == 0) {
      log.info("Parameter length validation is disabled (maxParameterLength = 0)");
    } else {
      log.info(
          "Parameter length validation enabled: max {} characters",
          configProperties.getMaxParameterLength());
    }
  }

  /**
   * Validates basic security constraints on incoming requests.
   *
   * @param requestDetails the request details
   * @throws InvalidRequestException if validation fails
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
  public void validateRequest(RequestDetails requestDetails) {

    try {
      // Validate request size for POST requests (if enabled)
      if (requestDetails.getRequestType() != null
          && "POST".equalsIgnoreCase(requestDetails.getRequestType().name())) {
        validateRequestSize(requestDetails);
      }

      // Validate parameter lengths (if enabled)
      validateParameterLengths(requestDetails);

    } catch (SecurityException e) {
      log.warn("Security validation failed: {}", e.getMessage());
      throw new InvalidRequestException(e.getMessage());
    }
  }

  /**
   * Validates the size of the request body.
   *
   * <p>Validation is skipped if {@code maxRequestSizeBytes} is set to 0.
   *
   * @param requestDetails the request details
   * @throws SecurityException if the request is too large
   */
  private void validateRequestSize(RequestDetails requestDetails) {
    long maxSize = configProperties.getMaxRequestSizeBytes();

    // Skip validation if max size is 0 (disabled)
    if (maxSize == 0) {
      return;
    }

    String contentLength = requestDetails.getHeader("Content-Length");
    if (contentLength != null) {
      try {
        long size = Long.parseLong(contentLength);

        if (size > maxSize) {
          throw new SecurityException(
              String.format("Request body too large: %d bytes (max: %d)", size, maxSize));
        }

        log.debug("Request size validation passed: {} bytes (max: {} bytes)", size, maxSize);

      } catch (NumberFormatException _) {
        log.warn("Invalid Content-Length header: {}", contentLength);
      }
    }
  }

  /**
   * Validates that parameter values don't exceed reasonable lengths.
   *
   * <p>Validation is skipped if {@code maxParameterLength} is set to 0.
   *
   * @param requestDetails the request details
   * @throws SecurityException if any parameter is too long
   */
  private void validateParameterLengths(RequestDetails requestDetails) {
    int maxParamLength = configProperties.getMaxParameterLength();

    // Skip validation if max length is 0 (disabled)
    if (maxParamLength == 0) {
      return;
    }

    var params = requestDetails.getParameters();
    if (params == null || params.isEmpty()) {
      return;
    }

    for (var entry : params.entrySet()) {
      String paramName = entry.getKey();
      String[] values = entry.getValue();

      if (values != null) {
        for (String value : values) {
          if (value != null && value.length() > maxParamLength) {
            throw new SecurityException(
                String.format(
                    "Parameter '%s' too long: %d characters (max: %d)",
                    paramName, value.length(), maxParamLength));
          }
        }
      }
    }

    log.debug("Parameter length validation passed for {} parameters", params.size());
  }
}
