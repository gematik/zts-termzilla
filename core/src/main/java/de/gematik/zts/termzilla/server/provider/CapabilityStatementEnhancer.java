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
package de.gematik.zts.termzilla.server.provider;

import de.gematik.zts.termzilla.configuration.ConfigProperties;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;

/**
 * Enhances the default CapabilityStatement of the server with additional information (e.g. ...) to
 * be compatible with the default FHIR TX Server Specification <a
 * href="https://build.fhir.org/ig/HL7/fhir-tx-ecosystem-ig/requirements.html">...</a>
 */
@Slf4j
public final class CapabilityStatementEnhancer {

  private CapabilityStatementEnhancer() {
    // Private constructor to prevent instantiation
  }

  /**
   * Enhances the given CapabilityStatement with additional information to be compatible with the
   * FHIR TX Server Specification (see class comment)
   *
   * @param capabilities the CapabilityStatement to enhance
   */
  public static void enhanceStatement(CapabilityStatement capabilities, ConfigProperties config) {

    // CapabilityStatement.software
    final CapabilityStatement.CapabilityStatementSoftwareComponent software =
        new CapabilityStatement.CapabilityStatementSoftwareComponent();
    software
        .setName(config.getCapabilitiesSoftwareName())
        .setVersion(config.getCapabilitiesSoftwareVersion())
        .setReleaseDateElement(new DateTimeType(config.getCapabilitiesSoftwareReleaseDate()));

    // CapabilityStatement.implementation
    final CapabilityStatement.CapabilityStatementImplementationComponent implementation =
        new CapabilityStatement.CapabilityStatementImplementationComponent();
    implementation.setDescription(config.getCapabilitiesDescription());
    implementation.setUrl(config.getCapabilitiesAdvertisedUrl());

    // CapabilityStatement.rest
    List<CapabilityStatement.CapabilityStatementRestComponent> restComponentList =
        capabilities.getRest();
    for (CapabilityStatement.CapabilityStatementRestComponent rest : restComponentList) {
      // We must make sure, that security information will be set for the server mode (TX
      // requirement)
      if (rest.getMode() == CapabilityStatement.RestfulCapabilityMode.SERVER) {
        // HAPI always advertises @Transaction as "transaction" at the system level.
        // We replace it with "batch" since this server only accepts batch bundles.
        boolean hadTransaction =
            rest.getInteraction()
                .removeIf(
                    i -> i.getCode() == CapabilityStatement.SystemRestfulInteraction.TRANSACTION);
        boolean hasBatch =
            rest.getInteraction().stream()
                .anyMatch(i -> i.getCode() == CapabilityStatement.SystemRestfulInteraction.BATCH);
        if (hadTransaction && !hasBatch) {
          rest.getInteraction()
              .add(
                  new CapabilityStatement.SystemInteractionComponent()
                      .setCode(CapabilityStatement.SystemRestfulInteraction.BATCH));
        }

        // we make use of CORS
        rest.getSecurity().setCors(true);
        rest.getSecurity().setDescription(config.getCapabilitiesRestSecurityDescription());
        // Filter resources based on configuration
        filterResourceCapabilities(rest, config);
      }
    }

    // Falls diese Erweiterung nicht gesetzt wird, meckert der HL7 Validator
    Extension featureExtension1 =
        new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");
    featureExtension1.addExtension(
        new Extension(
            "definition",
            new CanonicalType("http://hl7.org/fhir/uv/tx-tests/FeatureDefinition/test-version")));

    featureExtension1.addExtension(new Extension("value", new CodeType("1.7.6")));

    // Falls diese Erweiterung nicht gesetzt wird, meckert der HL7 Validator
    // TODO: Aktuell können wir keine CodeSystem-Parameter übergeben, da wir diese Funktionalität
    //  noch nicht unterstützen und die Unterstützung eigentlich nicht beabsichtigen
    Extension featureExtension2 =
        new Extension("http://hl7.org/fhir/uv/application-feature/StructureDefinition/feature");
    featureExtension2.addExtension(
        new Extension(
            "definition",
            new CanonicalType(
                "http://hl7.org/fhir/uv/tx-ecosystem/FeatureDefinition/CodeSystemAsParameter")));
    featureExtension2.addExtension(new Extension("value", new BooleanType(true)));

    // CapabilityStatement
    capabilities
        .setName(config.getCapabilitiesName())
        .setTitle(config.getCapabilitiesTitle())
        .setDescription(config.getCapabilitiesDescription())
        .setExperimental(config.isCapabilitiesExperimental())
        .setPublisher(config.getCapabilitiesPublisher())
        .setStatus(Enumerations.PublicationStatus.fromCode(config.getCapabilitiesStatus()))
        .setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE)
        .setFhirVersion(Enumerations.FHIRVersion._4_0_1)
        .setSoftware(software)
        .setImplementation(implementation)
    //        .setRest(List.of(rest))
    // TODO: Wir müssen noch recherchieren, was diese Extensions bewirken (z.B. beim HL7 Validator)
    //        .addExtension(featureExtension1)
    //        .addExtension(featureExtension2)
    ;
  }

  /**
   * Filters resource capabilities based on configuration settings.
   *
   * @param rest the REST component to filter
   * @param config the configuration
   */
  private static void filterResourceCapabilities(
      CapabilityStatement.CapabilityStatementRestComponent rest, ConfigProperties config) {

    List<CapabilityStatement.CapabilityStatementRestResourceComponent> resources =
        rest.getResource();

    for (CapabilityStatement.CapabilityStatementRestResourceComponent resource : resources) {
      String resourceType = resource.getType();

      // Filter interactions (READ, SEARCH)
      filterInteractions(resource, resourceType, config);

      // Filter search parameters
      filterSearchParameters(resource, resourceType, config);

      // Filter operations
      filterOperations(resource, resourceType, config);
    }
  }

  /**
   * Filters interactions (READ, SEARCH) based on configuration.
   *
   * @param resource the resource component
   * @param resourceType the resource type
   * @param config the configuration
   */
  private static void filterInteractions(
      CapabilityStatement.CapabilityStatementRestResourceComponent resource,
      String resourceType,
      ConfigProperties config) {

    List<CapabilityStatement.ResourceInteractionComponent> interactions = resource.getInteraction();
    Iterator<CapabilityStatement.ResourceInteractionComponent> iterator = interactions.iterator();

    while (iterator.hasNext()) {
      CapabilityStatement.ResourceInteractionComponent interaction = iterator.next();
      CapabilityStatement.TypeRestfulInteraction code = interaction.getCode();

      boolean shouldRemove = false;

      if (code == CapabilityStatement.TypeRestfulInteraction.READ) {
        shouldRemove = !config.isReadEnabledForResource(resourceType);
        if (shouldRemove) {
          log.debug("Removing READ interaction for resource type: {}", resourceType);
        }
      } else if (code == CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE) {
        shouldRemove = !config.isSearchEnabledForResource(resourceType);
        if (shouldRemove) {
          log.debug("Removing SEARCH interaction for resource type: {}", resourceType);
        }
      }

      if (shouldRemove) {
        iterator.remove();
      }
    }
  }

  /**
   * Filters search parameters based on configuration.
   *
   * @param resource the resource component
   * @param resourceType the resource type
   * @param config the configuration
   */
  private static void filterSearchParameters(
      CapabilityStatement.CapabilityStatementRestResourceComponent resource,
      String resourceType,
      ConfigProperties config) {

    // Only filter if search is enabled
    if (!config.isSearchEnabledForResource(resourceType)) {
      // If search is disabled, remove all search parameters
      resource.getSearchParam().clear();
      log.debug(
          "Cleared all search parameters for resource type: {} (search disabled)", resourceType);
      return;
    }

    Set<String> allowedParams = config.getAllowedSearchParamsForResource(resourceType);

    // If no restrictions are configured, keep all parameters
    if (allowedParams == null || allowedParams.isEmpty()) {
      log.debug("No search parameter restrictions for resource type: {}", resourceType);
      return;
    }

    List<CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent> searchParams =
        resource.getSearchParam();
    Iterator<CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent> iterator =
        searchParams.iterator();

    while (iterator.hasNext()) {
      CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent searchParam =
          iterator.next();
      String paramName = searchParam.getName();

      // Keep FHIR control parameters and configured parameters
      if (!isFhirControlParameter(paramName) && !allowedParams.contains(paramName)) {
        log.debug("Removing search parameter '{}' for resource type: {}", paramName, resourceType);
        iterator.remove();
      }
    }
  }

  /**
   * Filters operations based on configuration.
   *
   * @param resource the resource component
   * @param resourceType the resource type
   * @param config the configuration
   */
  private static void filterOperations(
      CapabilityStatement.CapabilityStatementRestResourceComponent resource,
      String resourceType,
      ConfigProperties config) {

    List<CapabilityStatement.CapabilityStatementRestResourceOperationComponent> operations =
        resource.getOperation();
    Iterator<CapabilityStatement.CapabilityStatementRestResourceOperationComponent> iterator =
        operations.iterator();

    while (iterator.hasNext()) {
      CapabilityStatement.CapabilityStatementRestResourceOperationComponent operation =
          iterator.next();
      String operationName = operation.getName();

      // Convert operation name to format used in config (e.g., "validate-code" -> "$validate-code")
      String operationKey = operationName.startsWith("$") ? operationName : "$" + operationName;

      if (!config.isOperationEnabled(resourceType, operationKey)) {
        log.debug("Removing operation '{}' for resource type: {}", operationName, resourceType);
        iterator.remove();
      }
    }
  }

  /**
   * Checks if a parameter is a FHIR control parameter.
   *
   * @param paramName the parameter name
   * @return true if it's a control parameter
   */
  private static boolean isFhirControlParameter(String paramName) {
    return paramName.startsWith("_count")
        || paramName.startsWith("_offset")
        || paramName.startsWith("_page")
        || paramName.startsWith("_sort")
        || paramName.startsWith("_format")
        || paramName.startsWith("_pretty")
        || paramName.startsWith("_total")
        || paramName.startsWith("_summary")
        || paramName.startsWith("_elements")
        || paramName.startsWith("_include")
        || paramName.startsWith("_revinclude")
        || paramName.startsWith("_contained")
        || paramName.startsWith("_containedType")
        || paramName.equals("_content")
        || paramName.equals("_id")
        || paramName.equals("_source");
  }
}
