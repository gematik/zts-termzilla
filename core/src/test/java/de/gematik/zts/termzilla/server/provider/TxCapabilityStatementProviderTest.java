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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerConfiguration;
import ca.uhn.fhir.rest.server.method.IMethodBinding;
import ca.uhn.fhir.rest.server.method.OperationMethodBinding;
import ca.uhn.fhir.rest.server.method.OperationMethodBinding.ReturnType;
import ca.uhn.fhir.rest.server.method.OperationParameter;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TxCapabilityStatementProvider} focusing on validate-code
 * OperationDefinition link generation and resource-specific resolution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TxCapabilityStatementProvider tests")
class TxCapabilityStatementProviderTest {

  private static final String SERVER_BASE = "https://tx-dev.apps.osc.fokus.fraunhofer.de/fhir";
  private static final String ADVERTISED_BASE = "http://locahost:8080/tx/fhir";

  @Mock private RestfulServer server;

  @Mock private RestfulServerConfiguration serverConfiguration;

  @Mock private CombiningTerminologyService terminologyService;

  @Mock private ConfigProperties configProperties;

  @Mock private RequestDetails requestDetails;

  private TxCapabilityStatementProvider provider;

  @BeforeEach
  void setUp() {
    when(server.getFhirContext()).thenReturn(FhirContext.forR4Cached());
    when(requestDetails.getFhirServerBase()).thenReturn(SERVER_BASE);
    when(configProperties.getCapabilitiesAdvertisedUrl()).thenReturn(ADVERTISED_BASE);

    provider = new TxCapabilityStatementProvider(server, terminologyService, configProperties);
  }

  @AfterEach
  void tearDown() {
    Mockito.reset(server, requestDetails, configProperties);
  }

  @Nested
  @DisplayName("rewriteValidateCodeOperationDefinitionLinks")
  class RewriteValidateCodeOperationDefinitionLinksTests {

    @Test
    @DisplayName("should replace merged validate-code links with resource-scoped links")
    void shouldReplaceMergedValidateCodeLinksWithResourceScopedLinks() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      OperationMethodBinding codeSystemBinding =
          createValidateCodeBindingForRewriteTest(true, false, false);
      OperationMethodBinding valueSetBinding =
          createValidateCodeBindingForRewriteTest(true, false, false);

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(codeSystemBinding));
      methodBindingsByResource.put("ValueSet", List.of(valueSetBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      CapabilityStatement capabilityStatement = new CapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent restComponent =
          capabilityStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);

      CapabilityStatement.CapabilityStatementRestResourceComponent codeSystem =
          restComponent.addResource().setType("CodeSystem");
      codeSystem
          .addOperation()
          .setName("validate-code")
          .setDefinition(SERVER_BASE + "/OperationDefinition/CodeSystemValueSet-t-validate-code");

      CapabilityStatement.CapabilityStatementRestResourceComponent valueSet =
          restComponent.addResource().setType("ValueSet");
      valueSet
          .addOperation()
          .setName("validate-code")
          .setDefinition(SERVER_BASE + "/OperationDefinition/CodeSystemValueSet-t-validate-code");

      // WHEN
      provider.rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);

      // THEN
      assertThat(codeSystem.getOperationFirstRep().getDefinition())
          .isEqualTo(ADVERTISED_BASE + "/OperationDefinition/CodeSystem-t-validate-code");
      assertThat(valueSet.getOperationFirstRep().getDefinition())
          .isEqualTo(ADVERTISED_BASE + "/OperationDefinition/ValueSet-t-validate-code");
    }

    @Test
    @DisplayName("should compose validate-code definition id with dynamic operation level token")
    void shouldComposeValidateCodeDefinitionIdWithDynamicOperationLevelToken() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      OperationMethodBinding codeSystemBinding =
          createValidateCodeBindingForRewriteTest(true, false, true);

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(codeSystemBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      CapabilityStatement capabilityStatement = new CapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent restComponent =
          capabilityStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      CapabilityStatement.CapabilityStatementRestResourceComponent codeSystem =
          restComponent.addResource().setType("CodeSystem");
      codeSystem
          .addOperation()
          .setName("validate-code")
          .setDefinition(SERVER_BASE + "/OperationDefinition/CodeSystemValueSet-t-validate-code");

      // WHEN
      provider.rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);

      // THEN
      assertThat(codeSystem.getOperationFirstRep().getDefinition())
          .isEqualTo(ADVERTISED_BASE + "/OperationDefinition/CodeSystem-it-validate-code");
    }

    @Test
    @DisplayName("should not rewrite links for non-server rest components")
    void shouldNotRewriteLinksForNonServerRestComponents() {
      // GIVEN
      CapabilityStatement capabilityStatement = new CapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent clientRest =
          capabilityStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.CLIENT);
      CapabilityStatement.CapabilityStatementRestResourceComponent codeSystem =
          clientRest.addResource().setType("CodeSystem");
      String originalDefinition =
          SERVER_BASE + "/OperationDefinition/CodeSystemValueSet-t-validate-code";
      codeSystem.addOperation().setName("validate-code").setDefinition(originalDefinition);

      // WHEN
      provider.rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);

      // THEN
      assertThat(codeSystem.getOperationFirstRep().getDefinition()).isEqualTo(originalDefinition);
    }

    @Test
    @DisplayName("should not rewrite links for non-terminology resources")
    void shouldNotRewriteLinksForNonTerminologyResources() {
      // GIVEN
      CapabilityStatement capabilityStatement = new CapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent serverRest =
          capabilityStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      CapabilityStatement.CapabilityStatementRestResourceComponent conceptMap =
          serverRest.addResource().setType("ConceptMap");
      String originalDefinition = SERVER_BASE + "/OperationDefinition/ConceptMap-t-validate-code";
      conceptMap.addOperation().setName("validate-code").setDefinition(originalDefinition);

      // WHEN
      provider.rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);

      // THEN
      assertThat(conceptMap.getOperationFirstRep().getDefinition()).isEqualTo(originalDefinition);
    }

    @Test
    @DisplayName("should not rewrite non validate-code operations")
    void shouldNotRewriteNonValidateCodeOperations() {
      // GIVEN
      CapabilityStatement capabilityStatement = new CapabilityStatement();
      CapabilityStatement.CapabilityStatementRestComponent serverRest =
          capabilityStatement.addRest().setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
      CapabilityStatement.CapabilityStatementRestResourceComponent codeSystem =
          serverRest.addResource().setType("CodeSystem");
      String originalDefinition = SERVER_BASE + "/OperationDefinition/CodeSystem-i-lookup";
      codeSystem.addOperation().setName("lookup").setDefinition(originalDefinition);

      // WHEN
      provider.rewriteValidateCodeOperationDefinitionLinks(capabilityStatement, requestDetails);

      // THEN
      assertThat(codeSystem.getOperationFirstRep().getDefinition()).isEqualTo(originalDefinition);
    }
  }

  @Nested
  @DisplayName("readOperationDefinition")
  class ReadOperationDefinitionTests {

    @Test
    @DisplayName("should return resource-specific OperationDefinition for CodeSystem validate-code")
    void shouldReturnResourceSpecificOperationDefinitionForCodeSystemValidateCode() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      OperationMethodBinding codeSystemBinding = createValidateCodeBinding("CodeSystem", "code");

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(codeSystemBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      String operationDefinitionId = provider.createValidateCodeOperationDefinitionId("CodeSystem");

      // WHEN
      IBaseResource response =
          provider.readOperationDefinition(
              new IdType("OperationDefinition/" + operationDefinitionId), requestDetails);

      // THEN
      OperationDefinition operationDefinition = (OperationDefinition) response;
      List<String> inParameterNames =
          operationDefinition.getParameter().stream()
              .filter(
                  parameter -> parameter.getUse() == OperationDefinition.OperationParameterUse.IN)
              .map(OperationDefinition.OperationDefinitionParameterComponent::getName)
              .toList();

      List<String> resourceNames =
          operationDefinition.getResource().stream().map(PrimitiveType::getValue).toList();

      assertThat(operationDefinition.getCode()).isEqualTo("validate-code");
      assertThat(operationDefinition.getUrl())
          .isEqualTo(ADVERTISED_BASE + "/OperationDefinition/" + operationDefinitionId);
      assertThat(resourceNames).containsExactly("CodeSystem");
      assertThat(inParameterNames).contains("code").doesNotContain("coding");
    }

    @Test
    @DisplayName("should merge metadata and boolean flags across validate-code bindings")
    void shouldMergeMetadataAndBooleanFlagsAcrossValidateCodeBindings() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      OperationMethodBinding firstBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("code", "code")),
              List.of(),
              "First description",
              "First title",
              true,
              false,
              false,
              true);
      OperationMethodBinding secondBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("code", "code")),
              List.of(),
              "Second description",
              "Second title",
              false,
              true,
              true,
              false);

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(firstBinding, secondBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      String operationDefinitionId = provider.createValidateCodeOperationDefinitionId("CodeSystem");

      // WHEN
      IBaseResource response =
          provider.readOperationDefinition(
              new IdType("OperationDefinition/" + operationDefinitionId), requestDetails);

      // THEN
      OperationDefinition operationDefinition = (OperationDefinition) response;
      assertThat(operationDefinition.getDescription()).isEqualTo("First description");
      assertThat(operationDefinition.getTitle()).isEqualTo("First title");
      assertThat(operationDefinition.getType()).isTrue();
      assertThat(operationDefinition.getSystem()).isTrue();
      assertThat(operationDefinition.getInstance()).isTrue();
      assertThat(operationDefinition.getAffectsState()).isTrue();
    }

    @Test
    @DisplayName("should set input parameter type to Resource when merged types conflict")
    void shouldSetInputParameterTypeToResourceWhenMergedTypesConflict() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      OperationMethodBinding firstBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("code", "code")),
              List.of(),
              null,
              null,
              true,
              false,
              false,
              true);
      OperationMethodBinding secondBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("code", "string")),
              List.of(),
              null,
              null,
              true,
              false,
              false,
              true);

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(firstBinding, secondBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      String operationDefinitionId = provider.createValidateCodeOperationDefinitionId("CodeSystem");

      // WHEN
      IBaseResource response =
          provider.readOperationDefinition(
              new IdType("OperationDefinition/" + operationDefinitionId), requestDetails);

      // THEN
      OperationDefinition operationDefinition = (OperationDefinition) response;
      OperationDefinition.OperationDefinitionParameterComponent inParam =
          operationDefinition.getParameter().stream()
              .filter(
                  parameter -> parameter.getUse() == OperationDefinition.OperationParameterUse.IN)
              .filter(parameter -> "code".equals(parameter.getName()))
              .findFirst()
              .orElseThrow();
      assertThat(inParam.getTypeElement().getValueAsString()).isEqualTo("Resource");
    }

    @Test
    @DisplayName("should deduplicate output parameters by name")
    void shouldDeduplicateOutputParametersByName() {
      // GIVEN
      when(server.createConfiguration()).thenReturn(serverConfiguration);

      ReturnType duplicateOne = createReturnType("result", "string", 0, 1);
      ReturnType duplicateTwo = createReturnTypeNameOnly("result");

      OperationMethodBinding firstBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("code", "code")),
              List.of(duplicateOne),
              null,
              null,
              true,
              false,
              false,
              true);
      OperationMethodBinding secondBinding =
          createValidateCodeBindingWithConfig(
              "CodeSystem",
              List.of(createOperationParameter("coding", "Coding")),
              List.of(duplicateTwo),
              null,
              null,
              true,
              false,
              false,
              true);

      Map<String, List<IMethodBinding>> methodBindingsByResource = new HashMap<>();
      methodBindingsByResource.put("CodeSystem", List.of(firstBinding, secondBinding));
      when(serverConfiguration.collectMethodBindings()).thenReturn(methodBindingsByResource);

      String operationDefinitionId = provider.createValidateCodeOperationDefinitionId("CodeSystem");

      // WHEN
      IBaseResource response =
          provider.readOperationDefinition(
              new IdType("OperationDefinition/" + operationDefinitionId), requestDetails);

      // THEN
      OperationDefinition operationDefinition = (OperationDefinition) response;
      List<String> outParamNames =
          operationDefinition.getParameter().stream()
              .filter(
                  parameter -> parameter.getUse() == OperationDefinition.OperationParameterUse.OUT)
              .map(OperationDefinition.OperationDefinitionParameterComponent::getName)
              .toList();
      assertThat(outParamNames).containsExactly("result");
    }

    private OperationMethodBinding createValidateCodeBinding(
        String resourceType, String parameterName) {
      OperationParameter operationParameter = createOperationParameter(parameterName, "code");
      return createValidateCodeBindingWithConfig(
          resourceType,
          List.of(operationParameter),
          List.of(),
          null,
          null,
          true,
          false,
          false,
          true);
    }

    private OperationMethodBinding createValidateCodeBindingWithConfig(
        String resourceType,
        List<OperationParameter> parameters,
        List<ReturnType> returnParams,
        String description,
        String shortDescription,
        boolean typeLevel,
        boolean serverLevel,
        boolean instanceLevel,
        boolean idempotent) {
      OperationMethodBinding methodBinding = org.mockito.Mockito.mock(OperationMethodBinding.class);

      when(methodBinding.getName()).thenReturn("$validate-code");
      when(methodBinding.getResourceName()).thenReturn(resourceType);
      when(methodBinding.getParameters()).thenReturn(List.copyOf(parameters));
      when(methodBinding.getReturnParams()).thenReturn(List.copyOf(returnParams));
      when(methodBinding.getDescription()).thenReturn(description);
      when(methodBinding.getShortDescription()).thenReturn(shortDescription);
      when(methodBinding.isCanOperateAtTypeLevel()).thenReturn(typeLevel);
      when(methodBinding.isCanOperateAtServerLevel()).thenReturn(serverLevel);
      when(methodBinding.isCanOperateAtInstanceLevel()).thenReturn(instanceLevel);
      when(methodBinding.isIdempotent()).thenReturn(idempotent);

      return methodBinding;
    }

    private OperationParameter createOperationParameter(String name, String paramType) {
      OperationParameter operationParameter = org.mockito.Mockito.mock(OperationParameter.class);

      when(operationParameter.getName()).thenReturn(name);
      when(operationParameter.getParamType()).thenReturn(paramType);
      when(operationParameter.getMin()).thenReturn(0);
      when(operationParameter.getMax()).thenReturn(1);
      when(operationParameter.getExampleValues()).thenReturn(List.of());

      return operationParameter;
    }

    private ReturnType createReturnType(String name, String type, int min, int max) {
      ReturnType returnType = org.mockito.Mockito.mock(ReturnType.class);
      when(returnType.getName()).thenReturn(name);
      when(returnType.getType()).thenReturn(type);
      when(returnType.getMin()).thenReturn(min);
      when(returnType.getMax()).thenReturn(max);
      return returnType;
    }

    private ReturnType createReturnTypeNameOnly(String name) {
      ReturnType returnType = org.mockito.Mockito.mock(ReturnType.class);
      when(returnType.getName()).thenReturn(name);
      return returnType;
    }
  }

  private OperationMethodBinding createValidateCodeBindingForRewriteTest(
      boolean typeLevel, boolean serverLevel, boolean instanceLevel) {
    OperationMethodBinding methodBinding = org.mockito.Mockito.mock(OperationMethodBinding.class);
    when(methodBinding.getName()).thenReturn("$validate-code");
    when(methodBinding.isCanOperateAtTypeLevel()).thenReturn(typeLevel);
    when(methodBinding.isCanOperateAtServerLevel()).thenReturn(serverLevel);
    when(methodBinding.isCanOperateAtInstanceLevel()).thenReturn(instanceLevel);
    return methodBinding;
  }
}
