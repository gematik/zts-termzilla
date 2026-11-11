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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

/**
 * Interceptor that validates operation parameters against configured allowed parameters.
 *
 * <p>This interceptor ensures that only explicitly allowed parameters are used in FHIR operations,
 * supporting both query string parameters and POST body parameters. It also handles nested
 * parameters (e.g., "property.code") and array parameters (e.g., "property[0]").
 */
@Slf4j
@Interceptor
public class OperationParameterValidationInterceptor {

  private final ConfigProperties config;
  private final FhirContext fhirContext;

  /**
   * Constructs a new OperationParameterValidationInterceptor with the specified configuration.
   *
   * @param config the configuration properties containing allowed operation parameters
   * @param fhirContext the FHIR context used to parse request bodies
   */
  public OperationParameterValidationInterceptor(ConfigProperties config, FhirContext fhirContext) {
    this.config = config;
    this.fhirContext = fhirContext;
  }

  /**
   * Immutable validation context for a request-specific operation.
   *
   * @param operation the FHIR operation name (e.g. {@code $expand})
   * @param resourceType the resource type the operation is executed on
   * @param allowedParams the configured set of allowed parameter names
   */
  private record ValidationContext(
      String operation, String resourceType, Set<String> allowedParams) {}

  /**
   * Intercepts incoming requests after initial processing to validate operation parameters.
   *
   * <p>This method validates both query string parameters and POST body parameters against the
   * configured allowed parameters for the specific operation and resource type.
   *
   * @param theRequestDetails the details of the incoming request
   * @param theRequest the HTTP servlet request
   * @param theResponse the HTTP servlet response
   * @throws InvalidRequestException if any parameter is not allowed for the operation
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void incomingRequestPostProcessed(
      RequestDetails theRequestDetails,
      HttpServletRequest theRequest,
      HttpServletResponse theResponse) {
    final ValidationContext context = resolveValidationContext(theRequestDetails);
    if (context == null) {
      return;
    }

    // Query parameters are always validated when restrictions exist.
    validateParameterNames(theRequest.getParameterMap().keySet(), context, false);

    // Body parameter validation only applies to POST operation calls.
    if ("POST".equalsIgnoreCase(theRequest.getMethod())) {
      validatePostBodyParameters(theRequestDetails, context);
    }

    log.debug(
        "Operation parameters validated successfully for {} on {}",
        context.operation(),
        context.resourceType());
  }

  /**
   * Resolves the validation context for the current request.
   *
   * @param requestDetails details of the incoming request
   * @return a populated context if validation should run, otherwise {@code null}
   */
  private ValidationContext resolveValidationContext(RequestDetails requestDetails) {
    final String operation = requestDetails.getOperation();

    // Only validate if this is an operation request
    if (operation == null || operation.isEmpty()) {
      return null;
    }

    final String resourceType = requestDetails.getResourceName();
    if (resourceType == null) {
      return null;
    }

    final Set<String> allowedParams = config.getAllowedOperationParams(resourceType, operation);
    if (allowedParams == null || allowedParams.isEmpty()) {
      log.debug("No parameter restrictions for operation {} on {}", operation, resourceType);
      return null;
    }

    return new ValidationContext(operation, resourceType, allowedParams);
  }

  /**
   * Validates POST body parameters if the body can be parsed as a FHIR {@link Parameters} resource.
   *
   * @param requestDetails details of the incoming request
   * @param context the resolved operation validation context
   */
  private void validatePostBodyParameters(
      RequestDetails requestDetails, ValidationContext context) {
    final byte[] bodyBytes = requestDetails.loadRequestContents();
    if (bodyBytes == null || bodyBytes.length == 0) {
      return;
    }

    try {
      final Resource parsed =
          (Resource)
              fhirContext
                  .newJsonParser()
                  .parseResource(new String(bodyBytes, StandardCharsets.UTF_8));

      // Only Parameters bodies contribute operation parameter names.
      if (parsed instanceof Parameters params) {
        validateParameterNames(
            params.getParameter().stream()
                .map(Parameters.ParametersParameterComponent::getName)
                .toList(),
            context,
            true);
      }
    } catch (DataFormatException e) {
      // Body is not valid FHIR JSON — let HAPI's own parser handle reporting this
      log.debug("Could not parse request body for parameter validation: {}", e.getMessage());
    }
  }

  /**
   * Validates a list of parameter names against the allowed set for an operation.
   *
   * @param paramNames parameter names to validate
   * @param context the resolved operation validation context
   * @param fromBody whether the parameter names come from the POST body
   * @throws InvalidRequestException if at least one parameter name is not allowed
   */
  private void validateParameterNames(
      Iterable<String> paramNames, ValidationContext context, boolean fromBody) {
    for (String paramName : paramNames) {
      if (!isAllowedOperationParameter(paramName, context.allowedParams())) {
        if (fromBody) {
          log.warn(
              "Unauthorized parameter '{}' in body for operation {} on resource {}",
              paramName,
              context.operation(),
              context.resourceType());
        } else {
          log.warn(
              "Unauthorized parameter '{}' for operation {} on resource {}",
              paramName,
              context.operation(),
              context.resourceType());
        }
        throw new InvalidRequestException(
            "Parameter '"
                + paramName
                + "' is not allowed for operation "
                + context.operation()
                + " on "
                + context.resourceType()
                + ". Allowed parameters: "
                + context.allowedParams());
      }
    }
  }

  /**
   * Checks if a parameter is allowed, considering nested and array parameters.
   *
   * <p>This method supports:
   *
   * <ul>
   *   <li>Direct parameter matches
   *   <li>Nested parameters (e.g., "property.code" matches if "property" is allowed)
   *   <li>Array parameters (e.g., "property[0]" matches if "property" is allowed)
   * </ul>
   *
   * @param paramName the parameter name to check
   * @param allowedParams the set of allowed parameter names
   * @return {@code true} if the parameter is allowed, {@code false} otherwise
   */
  private boolean isAllowedOperationParameter(String paramName, Set<String> allowedParams) {
    // Direct match
    if (allowedParams.contains(paramName)) {
      return true;
    }

    // Check for nested parameters (e.g., "property.code" should match if "property" is allowed)
    if (paramName.contains(".")) {
      String baseParam = paramName.substring(0, paramName.indexOf('.'));
      if (allowedParams.contains(baseParam)) {
        return true;
      }
    }

    // Check for array parameters (e.g., "property[0]" should match if "property" is allowed)
    if (paramName.contains("[")) {
      String baseParam = paramName.substring(0, paramName.indexOf('['));
      return allowedParams.contains(baseParam);
    }

    return false;
  }
}
