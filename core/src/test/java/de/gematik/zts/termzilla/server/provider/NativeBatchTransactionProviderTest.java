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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.method.IMethodBinding;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link NativeBatchTransactionProvider}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("NativeBatchTransactionProvider")
class NativeBatchTransactionProviderTest {

  private static final String SCOPE = "fhir";

  @Mock private CombiningTerminologyService terminologyService;

  @Mock private RestfulServer restfulServer;

  @Mock private ServletRequestDetails servletRequestDetails;

  @Mock private HttpServletRequest servletRequest;

  @Mock private HttpServletResponse servletResponse;

  @Mock private IInterceptorBroadcaster interceptorBroadcaster;

  @Mock private IMethodBinding methodBinding;

  @Mock private ConfigProperties configProperties;

  private NativeBatchTransactionProvider provider;

  @BeforeEach
  void setUp() {
    provider =
        new NativeBatchTransactionProvider(terminologyService, restfulServer, configProperties);
  }

  @Nested
  @DisplayName("processBatch")
  class ProcessBatchTests {

    @Test
    @DisplayName("Throws InvalidRequestException when bundle is null")
    void throwsWhenBundleIsNull() {
      // GIVEN

      // WHEN / THEN
      assertThatThrownBy(() -> provider.processBatch(servletRequestDetails, null))
          .isInstanceOf(ca.uhn.fhir.rest.server.exceptions.InvalidRequestException.class)
          .hasMessage("A Bundle with type 'batch' is required.");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when bundle type is not batch")
    void throwsWhenBundleTypeIsNotBatch() {
      // GIVEN
      Bundle requestBundle = new Bundle();
      requestBundle.setType(BundleType.COLLECTION);

      // WHEN / THEN
      assertThatThrownBy(() -> provider.processBatch(servletRequestDetails, requestBundle))
          .isInstanceOf(ca.uhn.fhir.rest.server.exceptions.InvalidRequestException.class)
          .hasMessage("A Bundle with type 'batch' is required.");
    }

    @Test
    @DisplayName("Throws InvalidRequestException when batch size exceeds configured maximum")
    void throwsWhenBatchSizeExceedsConfiguredMaximum() {
      // GIVEN
      when(configProperties.isSystemLevelBatchEnabled()).thenReturn(true);
      when(configProperties.getSystemLevelBatchMaxBatchSize()).thenReturn(1);

      Bundle.BundleEntryComponent firstEntry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle.BundleEntryComponent secondEntry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$lookup?system=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(firstEntry, secondEntry);

      // WHEN / THEN
      assertThatThrownBy(() -> provider.processBatch(servletRequestDetails, requestBundle))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("exceeds the configured maximum of 1");
    }

    @Test
    @DisplayName("Returns 200 with parsed resource when native invocation writes JSON")
    void returnsSuccessWhenNativeInvocationWritesJson() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      Parameters resultParameters = new Parameters();
      resultParameters.addParameter().setName("result").setValue(new BooleanType(true));
      FhirContext fhirContext = FhirContext.forR4Cached();
      String json = fhirContext.newJsonParser().encodeResourceToString(resultParameters);

      doAnswer(
              invocation -> {
                RequestDetails subRequest = invocation.getArgument(1, RequestDetails.class);
                ServletRequestDetails castSubRequest = (ServletRequestDetails) subRequest;
                castSubRequest.getServletResponse().getWriter().write(json);
                return null;
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getType()).isEqualTo(Bundle.BundleType.BATCHRESPONSE);
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus()).isEqualTo("200 OK");
      assertThat(responseBundle.getEntryFirstRep().getResource()).isInstanceOf(Parameters.class);
      verify(restfulServer)
          .determineResourceMethod(any(RequestDetails.class), eq("CodeSystem/$validate-code"));
    }

    @Test
    @DisplayName("Returns 500 when request details are not servlet based")
    void returnsInternalServerErrorForNonServletRequestDetails() {
      // GIVEN
      RequestDetails requestDetails = org.mockito.Mockito.mock(RequestDetails.class);
      when(requestDetails.getTenantId()).thenReturn(SCOPE);

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(requestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("500 Internal Server Error");
      assertThat(responseBundle.getEntryFirstRep().getResource())
          .isInstanceOf(OperationOutcome.class);
    }

    @Test
    @DisplayName("Maps native MethodNotAllowedException to 405 response entry")
    void mapsMethodNotAllowedExceptionTo405() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new MethodNotAllowedException("Operation is disabled"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("405 Method Not Allowed");
      assertThat(responseBundle.getEntryFirstRep().getResource())
          .isInstanceOf(OperationOutcome.class);
    }

    @Test
    @DisplayName("Returns 400 for all entries when scope is unknown")
    void returnsBadRequestForUnknownScope() throws Exception {
      // GIVEN
      when(servletRequestDetails.getTenantId()).thenReturn("unknown-scope");
      doThrow(new UnknownScopeException("Scope 'unknown-scope' is not supported."))
          .when(terminologyService)
          .checkScope("unknown-scope");

      Bundle.BundleEntryComponent firstEntry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle.BundleEntryComponent secondEntry =
          BatchTransactionTestDataHelper.createGetEntry("Patient/123");
      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(firstEntry, secondEntry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(2);
      assertThat(responseBundle.getEntry().get(0).getResponse().getStatus())
          .isEqualTo("400 Bad Request");
      assertThat(responseBundle.getEntry().get(1).getResponse().getStatus())
          .isEqualTo("400 Bad Request");
    }

    @Test
    @DisplayName("Returns 400 when entry request url is missing")
    void returnsBadRequestWhenEntryUrlIsMissing() {
      // GIVEN
      when(servletRequestDetails.getTenantId()).thenReturn(SCOPE);
      Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
      entry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.GET));
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("400 Bad Request");
      assertThat(responseBundle.getEntryFirstRep().getResource())
          .isInstanceOf(OperationOutcome.class);
    }

    @Test
    @DisplayName("Returns 200 with null resource when native invocation writes no body")
    void returnsSuccessWithNullResourceWhenNoBodyIsWritten() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doAnswer(invocation -> null)
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus()).isEqualTo("200 OK");
      assertThat(responseBundle.getEntryFirstRep().getResource()).isNull();
    }

    @Test
    @DisplayName("Maps native ResourceNotFoundException to 404 response entry")
    void mapsResourceNotFoundTo404() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new ResourceNotFoundException("Not found"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("404 Not Found");
    }

    @Test
    @DisplayName("Maps native UnprocessableEntityException to 422 response entry")
    void mapsUnprocessableEntityTo422() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new UnprocessableEntityException("Unprocessable"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("422 Unprocessable Entity");
    }

    @Test
    @DisplayName("Maps native NotImplementedOperationException to 501 response entry")
    void mapsNotImplementedOperationTo501() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new NotImplementedOperationException("Not implemented"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("501 Not Implemented");
    }

    @Test
    @DisplayName("Returns 200 with parsed resource when native invocation writes via output stream")
    void returnsSuccessWhenNativeInvocationWritesViaOutputStream() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      Parameters resultParameters = new Parameters();
      resultParameters.addParameter().setName("result").setValue(new BooleanType(true));
      FhirContext fhirContext = FhirContext.forR4Cached();
      String json = fhirContext.newJsonParser().encodeResourceToString(resultParameters);

      doAnswer(
              invocation -> {
                RequestDetails subRequest = invocation.getArgument(1, RequestDetails.class);
                ServletRequestDetails castSubRequest = (ServletRequestDetails) subRequest;
                WriteListener writeListener =
                    new WriteListener() {
                      @Override
                      public void onWritePossible() throws IOException {
                        // no-op for test
                      }

                      @Override
                      public void onError(Throwable throwable) {
                        // no-op for test
                      }
                    };

                assertThat(castSubRequest.getServletResponse().getOutputStream().isReady())
                    .isTrue();
                castSubRequest
                    .getServletResponse()
                    .getOutputStream()
                    .setWriteListener(writeListener);

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                for (byte nextByte : bytes) {
                  castSubRequest.getServletResponse().getOutputStream().write(nextByte);
                }
                return null;
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createGetEntry(
              "CodeSystem/$validate-code?url=http%3A%2F%2Floinc.org&code=8302-2");
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus()).isEqualTo("200 OK");
      assertThat(responseBundle.getEntryFirstRep().getResource()).isInstanceOf(Parameters.class);
    }

    @Test
    @DisplayName("Serializes entry resource and forwards it as sub-request body")
    void serializesEntryResourceAndForwardsItAsSubRequestBody() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      Parameters inputParameters =
          BatchTransactionTestDataHelper.createCodeSystemValidateCodeParameters(
              "http://loinc.org", "8302-2", "Body Height");
      Bundle.BundleEntryComponent entry =
          BatchTransactionTestDataHelper.createPostEntry(
              "CodeSystem/$validate-code", inputParameters);
      Bundle requestBundle = BatchTransactionTestDataHelper.createBatchBundle(entry);

      FhirContext fhirContext = FhirContext.forR4Cached();
      doAnswer(
              invocation -> {
                RequestDetails subRequest = invocation.getArgument(1, RequestDetails.class);
                byte[] requestContents = subRequest.loadRequestContents();
                assertThat(requestContents).isNotNull();

                Parameters parsed =
                    (Parameters)
                        fhirContext
                            .newJsonParser()
                            .parseResource(new String(requestContents, StandardCharsets.UTF_8));
                assertThat(parsed.getParameter()).hasSize(3);

                Parameters resultParameters = new Parameters();
                resultParameters.addParameter().setName("result").setValue(new BooleanType(true));

                ServletRequestDetails castSubRequest = (ServletRequestDetails) subRequest;
                castSubRequest
                    .getServletResponse()
                    .getWriter()
                    .write(fhirContext.newJsonParser().encodeResourceToString(resultParameters));
                return null;
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntry()).hasSize(1);
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus()).isEqualTo("200 OK");
      assertThat(responseBundle.getEntryFirstRep().getResource()).isInstanceOf(Parameters.class);
    }

    @Test
    @DisplayName("Maps POST PUT DELETE and default verbs to expected request types")
    void mapsVerbsToExpectedRequestTypes() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      Bundle.BundleEntryComponent postEntry =
          createEntryWithMethod(Bundle.HTTPVerb.POST, "CodeSystem/$lookup");
      Bundle.BundleEntryComponent putEntry =
          createEntryWithMethod(Bundle.HTTPVerb.PUT, "CodeSystem/123");
      Bundle.BundleEntryComponent deleteEntry =
          createEntryWithMethod(Bundle.HTTPVerb.DELETE, "CodeSystem/123");
      Bundle.BundleEntryComponent defaultEntry =
          createEntryWithMethod(Bundle.HTTPVerb.PATCH, "CodeSystem/123");
      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              postEntry, putEntry, deleteEntry, defaultEntry);

      ArgumentCaptor<RequestDetails> detailsCaptor = ArgumentCaptor.forClass(RequestDetails.class);
      doAnswer(
              invocation -> {
                RequestDetails subRequest = invocation.getArgument(1, RequestDetails.class);
                ServletRequestDetails castSubRequest = (ServletRequestDetails) subRequest;
                castSubRequest
                    .getServletResponse()
                    .getWriter()
                    .write("{\"resourceType\":\"OperationOutcome\"}");
                return null;
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), detailsCaptor.capture());

      // WHEN
      provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      List<RequestDetails> captured = detailsCaptor.getAllValues();
      assertThat(captured).hasSize(4);
      assertThat(captured.get(0).getRequestType().name()).isEqualTo("POST");
      assertThat(captured.get(1).getRequestType().name()).isEqualTo("PUT");
      assertThat(captured.get(2).getRequestType().name()).isEqualTo("DELETE");
      assertThat(captured.get(3).getRequestType().name()).isEqualTo("GET");
    }

    @Test
    @DisplayName("Maps InvalidRequestException to 400 response entry")
    void mapsInvalidRequestExceptionTo400() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new InvalidRequestException("Invalid request"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("400 Bad Request");
    }

    @Test
    @DisplayName("Maps UnsupportedTerminologyException to 400 response entry")
    void mapsUnsupportedTerminologyExceptionTo400() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doAnswer(
              invocation -> {
                throw new UnsupportedTerminologyException("Unsupported terminology");
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("400 Bad Request");
    }

    @Test
    @DisplayName("Maps NotImplementedException to 501 response entry")
    void mapsNotImplementedExceptionTo501() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doAnswer(
              invocation -> {
                throw new NotImplementedException("Not implemented");
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("501 Not Implemented");
    }

    @Test
    @DisplayName("Maps UnknownConceptException to 422 response entry")
    void mapsUnknownConceptExceptionTo422() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doAnswer(
              invocation -> {
                throw new UnknownConceptException("Unknown concept");
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("422 Unprocessable Entity");
    }

    @Test
    @DisplayName("Maps UnknownScopeException to 400 response entry via entry processing")
    void mapsUnknownScopeExceptionTo400ViaEntryProcessing() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doAnswer(
              invocation -> {
                throw new UnknownScopeException("Unknown scope");
              })
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("400 Bad Request");
    }

    @Test
    @DisplayName("Maps generic exception to 500 response entry")
    void mapsGenericExceptionTo500() throws Exception {
      // GIVEN
      stubNativeServletDispatch();

      doThrow(new RuntimeException("Boom"))
          .when(methodBinding)
          .invokeServer(eq(restfulServer), any(RequestDetails.class));

      Bundle requestBundle =
          BatchTransactionTestDataHelper.createBatchBundle(
              BatchTransactionTestDataHelper.createGetEntry("CodeSystem/$lookup?code=123"));

      // WHEN
      Bundle responseBundle = provider.processBatch(servletRequestDetails, requestBundle);

      // THEN
      assertThat(responseBundle.getEntryFirstRep().getResponse().getStatus())
          .isEqualTo("500 Internal Server Error");
    }
  }

  private Bundle.BundleEntryComponent createEntryWithMethod(Bundle.HTTPVerb method, String url) {
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(method).setUrl(url));
    return entry;
  }

  private void stubNativeServletDispatch() {
    when(servletRequestDetails.getTenantId()).thenReturn(SCOPE);
    when(servletRequestDetails.getFhirServerBase()).thenReturn("http://localhost:8080/fhir");
    when(servletRequestDetails.getServletRequest()).thenReturn(servletRequest);
    when(servletRequestDetails.getServletResponse()).thenReturn(servletResponse);
    when(servletRequestDetails.getInterceptorBroadcaster()).thenReturn(interceptorBroadcaster);

    doNothing()
        .when(restfulServer)
        .populateRequestDetailsFromRequestPath(any(ServletRequestDetails.class), any(String.class));
    when(restfulServer.determineResourceMethod(any(RequestDetails.class), any(String.class)))
        .thenReturn(methodBinding);
  }
}
