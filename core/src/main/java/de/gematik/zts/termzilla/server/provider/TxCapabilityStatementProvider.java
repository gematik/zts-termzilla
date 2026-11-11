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

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.method.IMethodBinding;
import ca.uhn.fhir.rest.server.method.IParameter;
import ca.uhn.fhir.rest.server.method.OperationMethodBinding;
import ca.uhn.fhir.rest.server.method.OperationMethodBinding.ReturnType;
import ca.uhn.fhir.rest.server.method.OperationParameter;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import ca.uhn.fhir.util.ExtensionUtil;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.util.HapiExtensions;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.TerminologyCapabilities;

/**
 * Custom CapabilityStatement provider that extends the default HAPI FHIR CapabilityStatement
 * provider to add TerminologyCapabilities and filter capabilities based on configuration.
 *
 * <p>This provider also customizes {@code OperationDefinition} handling for {@code $validate-code}.
 * HAPI can group operation bindings with the same operation name across multiple resource types,
 * which would merge {@code CodeSystem} and {@code ValueSet} {@code $validate-code} parameters into
 * one definition. This implementation keeps these definitions resource-specific.
 */
@Slf4j
public class TxCapabilityStatementProvider extends ServerCapabilityStatementProvider {

  private static final String RESOURCE_CODE_SYSTEM = "CodeSystem";
  private static final String RESOURCE_VALUE_SET = "ValueSet";
  private static final String OPERATION_VALIDATE_CODE = "validate-code";
  private static final String OPERATION_VALIDATE_CODE_METHOD_NAME = "$" + OPERATION_VALIDATE_CODE;

  private final CombiningTerminologyService terminologyService;
  private final ConfigProperties config;
  private final RestfulServer server;

  // TerminologyCapabilities of the server, keyed by scope
  private final ConcurrentHashMap<String, MyTerminologyCapabilities> terminologyCapabilities =
      new ConcurrentHashMap<>();

  // CapabilityStatement of the server
  private CapabilityStatement capabilityStatement = null;

  public TxCapabilityStatementProvider(
      RestfulServer theServer,
      CombiningTerminologyService terminologyService,
      ConfigProperties config) {
    super(theServer);
    this.server = theServer;
    this.terminologyService = terminologyService;
    this.config = config;
  }

  /**
   * Returns the CapabilityStatement for this server. We override this method to add our own
   * TerminologyCapabilities to the CapabilityStatement and filter based on configuration.
   *
   * @param httpServletRequest httpServletRequest
   * @param requestDetails requestDetails
   * @param modeParam requested mode (if 'terminology' is requested, the TerminologyCapabilities
   *     will be returned)
   * @return the CapabilityStatement or TerminologyCapabilities of the server
   */
  @Metadata(cacheMillis = 0)
  public IBaseConformance getServerConformance(
      HttpServletRequest httpServletRequest,
      RequestDetails requestDetails,
      @OptionalParam(name = CapabilityStatement.SP_MODE) TokenParam modeParam) {

    // if not already created, we create the CapabilityStatement and enhance it
    if (capabilityStatement == null) {
      capabilityStatement =
          (CapabilityStatement) super.getServerConformance(httpServletRequest, requestDetails);
      CapabilityStatementEnhancer.enhanceStatement(capabilityStatement, config);
      rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);
    }

    // Extract scope from requestDetails
    String scope = requestDetails.getTenantId();

    // if not already created, we create the TerminologyCapabilities for the given scope
    terminologyCapabilities.computeIfAbsent(
        scope, k -> createTerminologyCapabilities(requestDetails.getTenantId()));

    // if 'mode=terminology' is requested, we return the TerminologyCapabilities otherwise the
    // CapabilityStatement of the server will be returned
    if (modeParam != null
        && modeParam.getMissing() == null
        && modeParam.getValue().contentEquals("terminology")) {
      return terminologyCapabilities.get(scope);
    } else {
      return capabilityStatement;
    }
  }

  /**
   * Returns an {@code OperationDefinition} for the requested ID.
   *
   * <p>For {@code $validate-code}, this method returns a manually built, resource-specific
   * definition in order to keep {@code CodeSystem} and {@code ValueSet} parameter sets separate.
   * Other operation IDs are delegated to {@link ServerCapabilityStatementProvider}.
   *
   * @param theId the requested operation definition ID
   * @param theRequestDetails the current request details
   * @return the resolved operation definition resource
   */
  @Override
  @Read(typeName = "OperationDefinition")
  public IBaseResource readOperationDefinition(
      @IdParam IIdType theId, RequestDetails theRequestDetails) {
    String operationDefinitionId = theId != null ? theId.getIdPart() : null;
    if (isValidateCodeOperationDefinitionId(operationDefinitionId)) {
      String resourceName =
          Objects.requireNonNull(operationDefinitionId).startsWith(RESOURCE_CODE_SYSTEM)
              ? RESOURCE_CODE_SYSTEM
              : RESOURCE_VALUE_SET;
      IBaseResource resourceSpecificOperationDefinition =
          createResourceSpecificOperationDefinition(
              resourceName, operationDefinitionId, theRequestDetails);
      if (resourceSpecificOperationDefinition != null) {
        return resourceSpecificOperationDefinition;
      }
    }
    return super.readOperationDefinition(theId, theRequestDetails);
  }

  /**
   * Resolves the canonical prefix used for generated {@code OperationDefinition} URLs.
   *
   * @param theRequestDetails the current request details
   * @return server base URL with trailing slash, or empty string if unavailable
   */
  @Override
  protected String getOperationDefinitionPrefix(RequestDetails theRequestDetails) {
    final String advertisedUrl = config.getCapabilitiesAdvertisedUrl();
    if (!advertisedUrl.endsWith("/")) {
      return advertisedUrl + "/";
    }
    return advertisedUrl;
  }

  /**
   * Rewrites {@code CapabilityStatement.rest.resource.operation.definition} for {@code
   * $validate-code} so each supported resource points to its own {@code OperationDefinition} ID.
   *
   * @param theCapabilityStatement the capability statement to update
   * @param theRequestDetails the current request details
   */
  void rewriteValidateCodeOperationDefinitionLinks(
      CapabilityStatement theCapabilityStatement, RequestDetails theRequestDetails) {

    final String operationDefinitionPrefix =
        getOperationDefinitionPrefix(theRequestDetails) + "OperationDefinition/";

    for (CapabilityStatement.CapabilityStatementRestComponent restComponent :
        theCapabilityStatement.getRest()) {
      if (!isServerRestComponent(restComponent)) {
        log.warn("{} is not a Server REST Component", restComponent.getId());
        continue;
      }

      for (CapabilityStatement.CapabilityStatementRestResourceComponent resourceComponent :
          restComponent.getResource()) {
        rewriteResourceValidateCodeOperationDefinitionLinks(
            resourceComponent, operationDefinitionPrefix);
      }
    }
  }

  /**
   * Checks whether the given REST component represents the server mode section.
   *
   * @param theRestComponent the REST component to inspect
   * @return {@code true} if the component mode is {@code server}
   */
  private boolean isServerRestComponent(
      CapabilityStatement.CapabilityStatementRestComponent theRestComponent) {
    return theRestComponent.getMode() == CapabilityStatement.RestfulCapabilityMode.SERVER;
  }

  /**
   * Rewrites the {@code $validate-code} operation link for a single resource component.
   *
   * @param theResourceComponent resource section to inspect
   * @param theOperationDefinitionPrefix operation definition base URL prefix
   */
  private void rewriteResourceValidateCodeOperationDefinitionLinks(
      CapabilityStatement.CapabilityStatementRestResourceComponent theResourceComponent,
      String theOperationDefinitionPrefix) {
    if (!isTerminologyResource(theResourceComponent)) {
      return;
    }

    for (CapabilityStatement.CapabilityStatementRestResourceOperationComponent operationComponent :
        theResourceComponent.getOperation()) {
      if (OPERATION_VALIDATE_CODE.equals(operationComponent.getName())) {
        String resourceScopedId =
            createValidateCodeOperationDefinitionId(theResourceComponent.getType());
        operationComponent.setDefinition(theOperationDefinitionPrefix + resourceScopedId);
      }
    }
  }

  /**
   * Checks whether the resource component belongs to the terminology resources that expose
   * resource-specific {@code $validate-code} contracts.
   *
   * @param theResourceComponent the resource component to inspect
   * @return {@code true} for {@code CodeSystem} or {@code ValueSet}
   */
  private boolean isTerminologyResource(
      CapabilityStatement.CapabilityStatementRestResourceComponent theResourceComponent) {
    return RESOURCE_CODE_SYSTEM.equals(theResourceComponent.getType())
        || RESOURCE_VALUE_SET.equals(theResourceComponent.getType());
  }

  /**
   * Checks whether the requested operation definition ID is one of the resource-specific {@code
   * $validate-code} identifiers managed by this provider.
   *
   * @param theOperationDefinitionId operation definition ID to evaluate
   * @return {@code true} if the ID belongs to a resource-specific {@code $validate-code} definition
   */
  boolean isValidateCodeOperationDefinitionId(String theOperationDefinitionId) {
    if (StringUtils.isBlank(theOperationDefinitionId)) {
      return false;
    }

    for (String resourceName : List.of(RESOURCE_CODE_SYSTEM, RESOURCE_VALUE_SET)) {
      String prefix = resourceName + "-";
      String suffix = "-" + OPERATION_VALIDATE_CODE;
      if (theOperationDefinitionId.startsWith(prefix)
          && theOperationDefinitionId.endsWith(suffix)) {
        String levels =
            theOperationDefinitionId.substring(
                prefix.length(), theOperationDefinitionId.length() - suffix.length());
        return levels.equals(getValidateCodeOperationLevelToken(resourceName));
      }
    }

    return false;
  }

  /**
   * Builds the resource-specific operation definition ID for {@code $validate-code}.
   *
   * @param theResourceName resource name, e.g. {@code CodeSystem} or {@code ValueSet}
   * @return resource-scoped operation definition ID
   */
  String createValidateCodeOperationDefinitionId(String theResourceName) {
    return theResourceName
        + "-"
        + getValidateCodeOperationLevelToken(theResourceName)
        + "-"
        + OPERATION_VALIDATE_CODE;
  }

  private String getValidateCodeOperationLevelToken(String theResourceName) {
    List<OperationMethodBinding> operationMethodBindings =
        findValidateCodeOperationBindings(theResourceName);
    if (operationMethodBindings.isEmpty()) {
      // Fallback keeps backward-compatible IDs in test and edge scenarios without available
      // bindings.
      return "t";
    }

    boolean instance = false;
    boolean type = false;
    boolean system = false;

    for (OperationMethodBinding methodBinding : operationMethodBindings) {
      instance |= methodBinding.isCanOperateAtInstanceLevel();
      type |= methodBinding.isCanOperateAtTypeLevel();
      system |= methodBinding.isCanOperateAtServerLevel();
    }

    StringBuilder levelToken = new StringBuilder();
    if (instance) {
      levelToken.append('i');
    }
    if (type) {
      levelToken.append('t');
    }
    if (system) {
      levelToken.append('s');
    }

    return levelToken.isEmpty() ? "t" : levelToken.toString();
  }

  /**
   * Creates a resource-specific {@code OperationDefinition} for {@code $validate-code}.
   *
   * <p>This manual assembly is required because default binding grouping may merge {@code
   * CodeSystem} and {@code ValueSet} operation metadata into a single mixed definition, while both
   * resources expose different parameter contracts.
   *
   * @param theResourceName resource name to build the definition for
   * @param theOperationDefinitionId resulting operation definition ID
   * @param theRequestDetails the current request details
   * @return a resource-specific operation definition, or {@code null} if no matching bindings exist
   */
  private IBaseResource createResourceSpecificOperationDefinition(
      String theResourceName, String theOperationDefinitionId, RequestDetails theRequestDetails) {
    List<OperationMethodBinding> operationMethodBindings =
        findValidateCodeOperationBindings(theResourceName);
    if (operationMethodBindings.isEmpty()) {
      return null;
    }

    IBaseResource operationDefinition = createOperationDefinitionResource();
    FhirTerser terser = server.getFhirContext().newTerser();
    OperationDefinitionBuildState buildState = new OperationDefinitionBuildState();

    initializeOperationDefinition(terser, operationDefinition);

    for (OperationMethodBinding operationMethodBinding : operationMethodBindings) {
      mergeOperationBinding(terser, operationDefinition, buildState, operationMethodBinding);
    }

    finalizeOperationDefinition(
        terser, operationDefinition, theOperationDefinitionId, theRequestDetails, buildState);

    return operationDefinition;
  }

  /**
   * Finds {@code OperationMethodBinding}s for resource-level {@code $validate-code}.
   *
   * @param theResourceName resource name to search
   * @return matching operation bindings
   */
  private List<OperationMethodBinding> findValidateCodeOperationBindings(String theResourceName) {
    RestfulServerConfiguration configuration = server.createConfiguration();
    if (configuration == null) {
      return List.of();
    }
    List<IMethodBinding> methodBindings =
        configuration.collectMethodBindings().getOrDefault(theResourceName, List.of());

    return methodBindings.stream()
        .filter(OperationMethodBinding.class::isInstance)
        .map(OperationMethodBinding.class::cast)
        .filter(binding -> OPERATION_VALIDATE_CODE_METHOD_NAME.equals(binding.getName()))
        .toList();
  }

  /**
   * Creates a new {@code OperationDefinition} resource instance using the server FHIR context.
   *
   * @return empty operation definition resource instance
   */
  private IBaseResource createOperationDefinitionResource() {
    return server.getFhirContext().getResourceDefinition("OperationDefinition").newInstance();
  }

  /**
   * Initializes mandatory top-level fields for a generated operation definition.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition operation definition to initialize
   */
  private void initializeOperationDefinition(
      FhirTerser theTerser, IBaseResource theOperationDefinition) {
    theTerser.addElement(theOperationDefinition, "status", "active");
    theTerser.addElement(theOperationDefinition, "kind", "operation");
  }

  /**
   * Merges one operation binding into the aggregated build state and generated operation definition
   * parameter sections.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition target operation definition
   * @param theBuildState mutable aggregation state
   * @param theOperationMethodBinding source binding to merge
   */
  private void mergeOperationBinding(
      FhirTerser theTerser,
      IBaseResource theOperationDefinition,
      OperationDefinitionBuildState theBuildState,
      OperationMethodBinding theOperationMethodBinding) {
    mergeOperationMetadata(theBuildState, theOperationMethodBinding);
    mergeResourceName(theBuildState, theOperationMethodBinding);
    mergeInputParameters(
        theTerser, theOperationDefinition, theBuildState, theOperationMethodBinding);
    mergeOutputParameters(
        theTerser, theOperationDefinition, theBuildState, theOperationMethodBinding);
  }

  /**
   * Aggregates metadata flags and narrative fields from one operation binding.
   *
   * @param theBuildState mutable aggregation state
   * @param theOperationMethodBinding source binding
   */
  private void mergeOperationMetadata(
      OperationDefinitionBuildState theBuildState,
      OperationMethodBinding theOperationMethodBinding) {
    if (StringUtils.isNotBlank(theOperationMethodBinding.getDescription())
        && StringUtils.isBlank(theBuildState.description)) {
      theBuildState.description = theOperationMethodBinding.getDescription();
    }
    if (StringUtils.isNotBlank(theOperationMethodBinding.getShortDescription())
        && StringUtils.isBlank(theBuildState.title)) {
      theBuildState.title = theOperationMethodBinding.getShortDescription();
    }

    theBuildState.instanceLevel |= theOperationMethodBinding.isCanOperateAtInstanceLevel();
    theBuildState.systemLevel |= theOperationMethodBinding.isCanOperateAtServerLevel();
    theBuildState.typeLevel |= theOperationMethodBinding.isCanOperateAtTypeLevel();
    theBuildState.affectsState |= !theOperationMethodBinding.isIdempotent();
  }

  /**
   * Adds the binding resource name to the accumulated resource scope when present.
   *
   * @param theBuildState mutable aggregation state
   * @param theOperationMethodBinding source binding
   */
  private void mergeResourceName(
      OperationDefinitionBuildState theBuildState,
      OperationMethodBinding theOperationMethodBinding) {
    if (StringUtils.isNotBlank(theOperationMethodBinding.getResourceName())) {
      theBuildState.resourceNames.add(theOperationMethodBinding.getResourceName());
    }
  }

  /**
   * Merges input parameters from one operation binding into the aggregated definition. Existing
   * parameters are reused by name.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition target operation definition
   * @param theBuildState mutable aggregation state
   * @param theOperationMethodBinding source binding
   */
  private void mergeInputParameters(
      FhirTerser theTerser,
      IBaseResource theOperationDefinition,
      OperationDefinitionBuildState theBuildState,
      OperationMethodBinding theOperationMethodBinding) {
    for (IParameter nextParamUntyped : theOperationMethodBinding.getParameters()) {
      if (nextParamUntyped instanceof OperationParameter nextParam) {
        IBase param =
            getOrCreateInputParam(
                theTerser, theOperationDefinition, theBuildState, nextParam.getName());
        updateInputParamMetadata(theTerser, param, nextParam);
        appendMissingExamples(param, nextParam);
      }
    }
  }

  /**
   * Returns an existing input parameter element by name or creates a new one.
   *
   * @param theTerser terser used for element creation
   * @param theOperationDefinition target operation definition
   * @param theBuildState mutable aggregation state
   * @param theParamName input parameter name
   * @return existing or newly created parameter element
   */
  private IBase getOrCreateInputParam(
      FhirTerser theTerser,
      IBaseResource theOperationDefinition,
      OperationDefinitionBuildState theBuildState,
      String theParamName) {
    IBase existingParam = theBuildState.inParams.get(theParamName);
    if (existingParam != null) {
      return existingParam;
    }

    IBase createdParam = theTerser.addElement(theOperationDefinition, "parameter");
    theBuildState.inParams.put(theParamName, createdParam);
    return createdParam;
  }

  /**
   * Updates the generated input parameter element from one operation parameter binding.
   *
   * @param theTerser terser used for field population
   * @param theParam generated parameter element
   * @param theOperationParameter source operation parameter
   */
  private void updateInputParamMetadata(
      FhirTerser theTerser, IBase theParam, OperationParameter theOperationParameter) {
    addDocumentationIfMissing(theTerser, theParam, theOperationParameter);
    updateParamType(theTerser, theParam, theOperationParameter);

    theTerser.setElement(theParam, "use", "in");
    if (theOperationParameter.getSearchParamType() != null) {
      theTerser.setElement(theParam, "searchType", theOperationParameter.getSearchParamType());
    }
    theTerser.setElement(theParam, "min", Integer.toString(theOperationParameter.getMin()));
    theTerser.setElement(
        theParam,
        "max",
        theOperationParameter.getMax() == -1
            ? "*"
            : Integer.toString(theOperationParameter.getMax()));
    theTerser.setElement(theParam, "name", theOperationParameter.getName());
  }

  /**
   * Adds parameter documentation when available and not already set.
   *
   * @param theTerser terser used for field population
   * @param theParam generated parameter element
   * @param theOperationParameter source operation parameter
   */
  private void addDocumentationIfMissing(
      FhirTerser theTerser, IBase theParam, OperationParameter theOperationParameter) {
    if (StringUtils.isNotBlank(theOperationParameter.getDescription())
        && theTerser.getValues(theParam, "documentation").isEmpty()) {
      theTerser.addElement(theParam, "documentation", theOperationParameter.getDescription());
    }
  }

  /**
   * Reconciles parameter type assignments for merged parameters with the same name. If conflicting
   * types are encountered, {@code Resource} is used.
   *
   * @param theTerser terser used for field population
   * @param theParam generated parameter element
   * @param theOperationParameter source operation parameter
   */
  private void updateParamType(
      FhirTerser theTerser, IBase theParam, OperationParameter theOperationParameter) {
    if (theOperationParameter.getParamType() == null) {
      return;
    }

    String existingType = theTerser.getSinglePrimitiveValueOrNull(theParam, "type");
    if (theOperationParameter.getParamType().equals(existingType)) {
      return;
    }
    if (existingType == null) {
      theTerser.setElement(theParam, "type", theOperationParameter.getParamType());
      return;
    }
    theTerser.setElement(theParam, "type", "Resource");
  }

  /**
   * Adds missing example-value extensions from the source parameter to the generated parameter.
   *
   * @param theParam generated parameter element
   * @param theOperationParameter source operation parameter
   */
  private void appendMissingExamples(IBase theParam, OperationParameter theOperationParameter) {
    List<IBaseExtension<?, ?>> existingExampleExtensions =
        ExtensionUtil.getExtensionsByUrl(
            (IBaseHasExtensions) theParam, HapiExtensions.EXT_OP_PARAMETER_EXAMPLE_VALUE);
    Set<String> existingExamples =
        existingExampleExtensions.stream()
            .map(IBaseExtension::getValue)
            .filter(Objects::nonNull)
            .map(value -> (IPrimitiveType<?>) value)
            .map(IPrimitiveType::getValueAsString)
            .collect(Collectors.toSet());

    for (String nextExample : theOperationParameter.getExampleValues()) {
      if (!existingExamples.contains(nextExample)) {
        ExtensionUtil.addExtension(
            server.getFhirContext(),
            theParam,
            HapiExtensions.EXT_OP_PARAMETER_EXAMPLE_VALUE,
            "string",
            nextExample);
      }
    }
  }

  /**
   * Merges output parameters from one binding into the generated operation definition. Output
   * parameters are deduplicated by name.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition target operation definition
   * @param theBuildState mutable aggregation state
   * @param theOperationMethodBinding source binding
   */
  private void mergeOutputParameters(
      FhirTerser theTerser,
      IBaseResource theOperationDefinition,
      OperationDefinitionBuildState theBuildState,
      OperationMethodBinding theOperationMethodBinding) {
    for (ReturnType nextParam : theOperationMethodBinding.getReturnParams()) {
      if (theBuildState.outParams.containsKey(nextParam.getName())) {
        continue;
      }
      IBase param = createOutputParam(theTerser, theOperationDefinition, nextParam);
      theBuildState.outParams.put(nextParam.getName(), param);
    }
  }

  /**
   * Creates and populates one output parameter element from a return parameter definition.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition target operation definition
   * @param theReturnType source return type metadata
   * @return created output parameter element
   */
  private IBase createOutputParam(
      FhirTerser theTerser, IBaseResource theOperationDefinition, ReturnType theReturnType) {
    IBase param = theTerser.addElement(theOperationDefinition, "parameter");
    theTerser.addElement(param, "use", "out");
    if (theReturnType.getType() != null) {
      theTerser.addElement(param, "type", theReturnType.getType());
    }
    theTerser.addElement(param, "min", Integer.toString(theReturnType.getMin()));
    theTerser.addElement(
        param,
        "max",
        theReturnType.getMax() == -1 ? "*" : Integer.toString(theReturnType.getMax()));
    theTerser.addElement(param, "name", theReturnType.getName());
    return param;
  }

  /**
   * Finalizes top-level fields of the generated operation definition from aggregated state.
   *
   * @param theTerser terser used for field population
   * @param theOperationDefinition target operation definition
   * @param theOperationDefinitionId operation definition ID used in URL generation
   * @param theRequestDetails current request details
   * @param theBuildState mutable aggregation state
   */
  private void finalizeOperationDefinition(
      FhirTerser theTerser,
      IBaseResource theOperationDefinition,
      String theOperationDefinitionId,
      RequestDetails theRequestDetails,
      OperationDefinitionBuildState theBuildState) {
    String url =
        getOperationDefinitionPrefix(theRequestDetails)
            + "OperationDefinition/"
            + theOperationDefinitionId;

    theTerser.addElements(theOperationDefinition, "resource", theBuildState.resourceNames);
    theTerser.addElement(theOperationDefinition, "name", "Validate-code");
    theTerser.addElement(theOperationDefinition, "url", url);
    theTerser.addElement(theOperationDefinition, "code", OPERATION_VALIDATE_CODE);
    theTerser.addElement(theOperationDefinition, "description", theBuildState.description);
    theTerser.addElement(theOperationDefinition, "title", theBuildState.title);
    theTerser.addElement(
        theOperationDefinition, "affectsState", Boolean.toString(theBuildState.affectsState));
    theTerser.addElement(
        theOperationDefinition, "system", Boolean.toString(theBuildState.systemLevel));
    theTerser.addElement(theOperationDefinition, "type", Boolean.toString(theBuildState.typeLevel));
    theTerser.addElement(
        theOperationDefinition, "instance", Boolean.toString(theBuildState.instanceLevel));
  }

  /**
   * Mutable aggregation state used while merging multiple operation bindings into one
   * resource-specific {@code OperationDefinition}.
   */
  private static class OperationDefinitionBuildState {

    private boolean systemLevel = false;
    private boolean typeLevel = false;
    private boolean instanceLevel = false;
    private boolean affectsState = false;
    private String description = null;
    private String title = null;
    private final Set<String> resourceNames = new TreeSet<>();
    private final Map<String, IBase> inParams = new HashMap<>();
    private final Map<String, IBase> outParams = new HashMap<>();
  }

  /**
   * Creates a TerminologyCapabilities resource for the given scope. The TerminologyCapabilities
   * will contain all CodeSystems that are available in the given scope.
   *
   * @param scope the scope for which the TerminologyCapabilities should be created
   * @return the created TerminologyCapabilities
   */
  private MyTerminologyCapabilities createTerminologyCapabilities(String scope) {

    MyTerminologyCapabilities myTerminologyCapabilities = new MyTerminologyCapabilities();
    myTerminologyCapabilities.setStatus(
        Enumerations.PublicationStatus.fromCode(config.getCapabilitiesStatus()));
    myTerminologyCapabilities.setExperimental(config.isCapabilitiesExperimental());
    myTerminologyCapabilities.setPublisher(config.getCapabilitiesPublisher());
    myTerminologyCapabilities.setDescription(config.getCapabilitiesDescription());

    // all CodeSystems that are supported within the given scope will be searched
    List<IdType> csList =
        terminologyService.searchCodeSystem(scope, CsSearchParameter.builder().build(), true);

    // create a map with the URL of the CodeSystem as key and a set of versions as value
    Map<String, Set<String>> csUrlVersionMap = new HashMap<>();
    for (IdType csId : csList) {

      CodeSystem cs = terminologyService.getCodeSystem(scope, csId, ResourceRetrievalMode.STRIPPED);

      csUrlVersionMap.computeIfAbsent(cs.getUrl(), k -> new HashSet<>());

      if (StringUtils.isNoneEmpty(cs.getVersion())) {
        csUrlVersionMap.get(cs.getUrl()).add(cs.getVersion());
      }
    }

    // Build the TerminologyCapabilities
    for (Map.Entry<String, Set<String>> entry : csUrlVersionMap.entrySet()) {
      String csUrl = entry.getKey();
      TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent cap =
          new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemComponent();
      cap.setUri(csUrl);

      // Determine the default version
      String defaultVersion =
          terminologyService.getDefaultCodeSystemByScopeAndUrl(scope, csUrl).getVersion();

      for (String version : csUrlVersionMap.get(csUrl)) {
        if (version.contentEquals(defaultVersion)) {
          cap.addVersion(
              new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent()
                  .setCode(version)
                  .setIsDefault(true));
        } else {
          cap.addVersion(
              new TerminologyCapabilities.TerminologyCapabilitiesCodeSystemVersionComponent()
                  .setCode(version)
                  .setIsDefault(false));
        }
      }
      myTerminologyCapabilities.addCodeSystem(cap);
    }

    return myTerminologyCapabilities;
  }
}
