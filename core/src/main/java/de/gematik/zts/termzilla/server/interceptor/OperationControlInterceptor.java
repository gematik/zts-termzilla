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
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Interceptor that controls access to FHIR operations based on configuration settings.
 *
 * <p>This interceptor restricts READ, SEARCH, and custom operations for specific FHIR resource
 * types (CodeSystem, ValueSet, ConceptMap) based on the application configuration.
 */
@Slf4j
@Component
@Interceptor
public class OperationControlInterceptor {

  private final ConfigProperties config;
  private final Set<String> restrictedResources = Set.of("CodeSystem", "ValueSet", "ConceptMap");

  /**
   * Constructs a new OperationControlInterceptor with the specified configuration.
   *
   * @param config the configuration properties for operation control
   */
  public OperationControlInterceptor(ConfigProperties config) {
    this.config = config;
  }

  /**
   * Intercepts incoming requests before they are processed to enforce operation restrictions.
   *
   * <p>This method is invoked before the request is handled by the server. It checks whether the
   * requested operation is allowed based on the configuration.
   *
   * @param theOperation the type of REST operation being performed
   * @param theProcessedRequest the details of the incoming request
   * @throws MethodNotAllowedException if the operation is not allowed for the resource type
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void incomingRequestPreHandled(
      RestOperationTypeEnum theOperation, RequestDetails theProcessedRequest) {

    String resourceType = theProcessedRequest.getResourceName();
    String operation = theProcessedRequest.getOperation();

    // If no resource type is specified or not in restricted resources, allow (might be metadata
    // request) or any other resource type (OperationDefinition, etc.)
    if (resourceType == null || !restrictedResources.contains(resourceType)) {
      return;
    }

    // Check READ operations
    if (theOperation == RestOperationTypeEnum.READ
        && !config.isReadEnabledForResource(resourceType)) {
      log.warn("READ operation disabled for {} but requested", resourceType);
      throw new MethodNotAllowedException("READ operation is not enabled for " + resourceType);
    }

    // Check SEARCH operations
    if (theOperation == RestOperationTypeEnum.SEARCH_TYPE
        && !config.isSearchEnabledForResource(resourceType)) {
      log.warn("SEARCH operation disabled for {} but requested", resourceType);
      throw new MethodNotAllowedException("SEARCH operation is not enabled for " + resourceType);
    }

    // Check custom operations
    if (operation != null && !config.isOperationEnabled(resourceType, operation)) {
      log.warn("{} operation disabled for {} but requested", operation, resourceType);
      throw new MethodNotAllowedException(
          operation + " operation is not enabled for " + resourceType);
    }
  }
}
