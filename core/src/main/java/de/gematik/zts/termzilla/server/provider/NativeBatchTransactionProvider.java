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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

/**
 * Transaction provider for FHIR batch processing with native HAPI method dispatch.
 *
 * <p>This provider routes each batch entry through HAPI's method binding pipeline using a synthetic
 * servlet sub-request. It keeps entry-level isolation so one failing entry is returned as an error
 * response without aborting processing of other entries.
 */
public class NativeBatchTransactionProvider {

  private static final String STATUS_OK = "200 OK";
  private static final String STATUS_BAD_REQUEST = "400 Bad Request";
  private static final String STATUS_UNPROCESSABLE = "422 Unprocessable Entity";
  private static final String STATUS_NOT_IMPLEMENTED = "501 Not Implemented";
  private static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";
  private static final String STATUS_NOT_FOUND = "404 Not Found";
  private static final String STATUS_METHOD_NOT_ALLOWED = "405 Method Not Allowed";

  private final CombiningTerminologyService terminologyService;
  private final RestfulServer restfulServer;
  private final ConfigProperties configProperties;
  private final FhirContext fhirContext = FhirContext.forR4Cached();

  /**
   * Creates a native dispatch batch transaction provider.
   *
   * @param terminologyService terminology service used by delegated processing
   * @param restfulServer restful server used for native sub-request method resolution
   * @param configProperties configuration properties used for batch limits
   */
  public NativeBatchTransactionProvider(
      CombiningTerminologyService terminologyService,
      RestfulServer restfulServer,
      ConfigProperties configProperties) {
    this.terminologyService = terminologyService;
    this.restfulServer = restfulServer;
    this.configProperties = configProperties;
  }

  /**
   * Processes a FHIR batch bundle through native HAPI dispatch.
   *
   * @param requestDetails request details including tenant scope
   * @param requestBundle incoming batch bundle
   * @return batch-response bundle
   */
  @Transaction
  public Bundle processBatch(
      RequestDetails requestDetails, @TransactionParam Bundle requestBundle) {

    if (requestBundle == null || requestBundle.getType() != Bundle.BundleType.BATCH) {
      throw new InvalidRequestException("A Bundle with type 'batch' is required.");
    }

    if (configProperties.isSystemLevelBatchEnabled()) {
      int maxBatchSize = configProperties.getSystemLevelBatchMaxBatchSize();
      if (requestBundle.getEntry().size() > maxBatchSize) {
        throw new InvalidRequestException(
            "Batch contains "
                + requestBundle.getEntry().size()
                + " entries which exceeds the configured maximum of "
                + maxBatchSize
                + ".");
      }
    }

    final String scope = requestDetails.getTenantId();
    final Bundle responseBundle = new Bundle();
    responseBundle.setType(Bundle.BundleType.BATCHRESPONSE);

    try {
      terminologyService.checkScope(scope);
    } catch (UnknownScopeException exception) {
      for (var _ : requestBundle.getEntry()) {
        responseBundle.addEntry(buildErrorEntry(STATUS_BAD_REQUEST, exception.getMessage()));
      }
      return responseBundle;
    }

    for (Bundle.BundleEntryComponent entry : requestBundle.getEntry()) {
      responseBundle.addEntry(processEntryWithNativeDispatch(scope, requestDetails, entry));
    }

    return responseBundle;
  }

  /**
   * Processes one batch entry through HAPI's native method binding pipeline.
   *
   * @param scope tenant scope
   * @param requestDetails outer request details
   * @param entry current batch entry
   * @return one batch-response entry for the given request entry
   */
  private Bundle.BundleEntryComponent processEntryWithNativeDispatch(
      String scope, RequestDetails requestDetails, Bundle.BundleEntryComponent entry) {
    try {
      final Bundle.BundleEntryRequestComponent request = entry.getRequest();
      if (request == null || request.getMethod() == null || StringUtils.isEmpty(request.getUrl())) {
        return buildErrorEntry(
            STATUS_BAD_REQUEST, "Each batch entry must define request.method and request.url.");
      }

      if (!(requestDetails instanceof ServletRequestDetails outerDetails)) {
        return buildErrorEntry(
            STATUS_INTERNAL_SERVER_ERROR, "Native dispatch requires ServletRequestDetails.");
      }

      final String normalizedUrl =
          request.getUrl().startsWith("/") ? request.getUrl().substring(1) : request.getUrl();
      final String path = normalizedUrl.split("\\?", 2)[0];
      final String serverBase = requestDetails.getFhirServerBase();
      final String completeUrl =
          serverBase.endsWith("/") ? serverBase + normalizedUrl : serverBase + "/" + normalizedUrl;

      final HttpServletRequest wrappedServletRequest =
          new HttpServletRequestWrapper(outerDetails.getServletRequest()) {
            @Override
            public String getHeader(String name) {
              if ("Content-Type".equalsIgnoreCase(name)) {
                return "application/fhir+json";
              }
              return super.getHeader(name);
            }

            @Override
            public String getContentType() {
              return "application/fhir+json";
            }
          };

      final ByteArrayOutputStream captureBuffer = new ByteArrayOutputStream();

      // HAPI operation invocation writes directly to the servlet response stream.
      // We capture those bytes per batch entry so we can parse them back into a Resource
      // and avoid leaking entry-local output into the outer HTTP response stream.
      final HttpServletResponse capturingResponse =
          new HttpServletResponseWrapper(outerDetails.getServletResponse()) {
            private PrintWriter cachedWriter;
            private ServletOutputStream cachedStream;

            @Override
            public PrintWriter getWriter() {
              if (cachedWriter == null) {
                cachedWriter =
                    new PrintWriter(new OutputStreamWriter(captureBuffer, StandardCharsets.UTF_8));
              }
              return cachedWriter;
            }

            @Override
            public ServletOutputStream getOutputStream() {
              if (cachedStream == null) {
                cachedStream =
                    new ServletOutputStream() {
                      @Override
                      public boolean isReady() {
                        return true;
                      }

                      @Override
                      public void setWriteListener(WriteListener listener) {
                        // no-op by design for capturing in-memory response bytes
                      }

                      @Override
                      public void write(int value) {
                        captureBuffer.write(value);
                      }
                    };
              }
              return cachedStream;
            }

            @Override
            public void flushBuffer() {
              // no-op to prevent leaking entry-local state to outer response
            }

            @Override
            public boolean isCommitted() {
              return false;
            }

            @Override
            public void reset() {
              captureBuffer.reset();
            }

            @Override
            public void resetBuffer() {
              captureBuffer.reset();
            }

            @Override
            public void addHeader(String name, String value) {
              // no-op by design for entry-local capture
            }

            @Override
            public void setHeader(String name, String value) {
              // no-op by design for entry-local capture
            }

            @Override
            public void setStatus(int statusCode) {
              // no-op by design for entry-local capture
            }

            @Override
            public void sendError(int statusCode, String message) {
              // no-op by design for entry-local capture
            }

            @Override
            public void sendError(int statusCode) {
              // no-op by design for entry-local capture
            }
          };

      final ServletRequestDetails subRequest =
          new ServletRequestDetails(outerDetails.getInterceptorBroadcaster());
      subRequest.setServletRequest(wrappedServletRequest);
      subRequest.setServletResponse(capturingResponse);
      subRequest.setServer(restfulServer);
      subRequest.setTenantId(scope);
      subRequest.setFhirServerBase(serverBase);
      subRequest.setCompleteUrl(completeUrl);
      subRequest.setRequestType(toRequestType(request.getMethod()));
      subRequest.setSubRequest(true);
      subRequest.setParameters(toRequestParameterMap(normalizedUrl));

      restfulServer.populateRequestDetailsFromRequestPath(subRequest, path);

      // if the entry has a resource, serialize it to JSON and set it as the request body for native
      // processing.
      if (entry.getResource() != null) {
        final byte[] bodyBytes =
            fhirContext
                .newJsonParser()
                .encodeResourceToString(entry.getResource())
                .getBytes(StandardCharsets.UTF_8);
        subRequest.setRequestContents(bodyBytes);
      }

      // SERVER_INCOMING_REQUEST_POST_PROCESSED is fired by RestfulServer.handleRequest() which
      // we bypass. Manually broadcast it so interceptors like
      // OperationParameterValidationInterceptor
      // run per entry exactly as they would for a direct request.
      final HookParams postProcessedParams =
          new HookParams()
              .add(RequestDetails.class, subRequest)
              .add(HttpServletRequest.class, wrappedServletRequest)
              .add(HttpServletResponse.class, capturingResponse);
      subRequest
          .getInterceptorBroadcaster()
          .callHooks(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED, postProcessedParams);

      // invoke the server method directly through the method binding to preserve all of HAPI's
      // native dispatch logic
      final var methodBinding = restfulServer.determineResourceMethod(subRequest, path);
      methodBinding.invokeServer(restfulServer, subRequest);

      // ensure all response bytes are written to the capture buffer before we parse it
      capturingResponse.getWriter().flush();

      // parse the captured response bytes back into a Resource for the batch entry response. If
      // there is no body, return a success entry with no resource.
      final byte[] capturedBytes = captureBuffer.toByteArray();
      if (capturedBytes.length > 0) {
        final Resource resource =
            (Resource)
                fhirContext
                    .newJsonParser()
                    .parseResource(new String(capturedBytes, StandardCharsets.UTF_8));
        return buildSuccessEntry(resource);
      }

      return buildSuccessEntry(null);
    } catch (Exception exception) {
      return buildErrorEntry(resolveNativeStatusCode(exception), exception.getMessage());
    }
  }

  /**
   * Converts a bundle HTTP verb into the corresponding HAPI request type.
   *
   * @param httpVerb request method from the batch entry
   * @return matching HAPI request type
   */
  private RequestTypeEnum toRequestType(Bundle.HTTPVerb httpVerb) {
    return switch (httpVerb) {
      case GET -> RequestTypeEnum.GET;
      case POST -> RequestTypeEnum.POST;
      case PUT -> RequestTypeEnum.PUT;
      case DELETE -> RequestTypeEnum.DELETE;
      default -> RequestTypeEnum.GET;
    };
  }

  /**
   * Builds the query parameter map for a native sub-request.
   *
   * @param normalizedUrl request URL without leading slash
   * @return parameter map compatible with {@link ServletRequestDetails#setParameters(Map)}
   */
  private Map<String, String[]> toRequestParameterMap(String normalizedUrl) {
    final Map<String, List<String>> queryMap = parseQueryParams(normalizedUrl);
    final Map<String, String[]> result = new HashMap<>();
    queryMap.forEach((key, value) -> result.put(key, value.toArray(String[]::new)));
    return result;
  }

  /**
   * Resolves the per-entry response status for native dispatch exceptions.
   *
   * @param exception exception thrown during native dispatch
   * @return status string for the response entry
   */
  private String resolveNativeStatusCode(Exception exception) {
    if (exception instanceof BaseServerResponseException baseServerResponseException) {
      final int statusCode = baseServerResponseException.getStatusCode();
      return switch (statusCode) {
        case 400 -> STATUS_BAD_REQUEST;
        case 404 -> STATUS_NOT_FOUND;
        case 405 -> STATUS_METHOD_NOT_ALLOWED;
        case 422 -> STATUS_UNPROCESSABLE;
        case 501 -> STATUS_NOT_IMPLEMENTED;
        default -> STATUS_INTERNAL_SERVER_ERROR;
      };
    }
    return resolveStatusCode(exception);
  }

  /**
   * Creates a successful batch-response entry.
   *
   * @param resource response resource, may be {@code null}
   * @return success entry with status {@code 200 OK}
   */
  private Bundle.BundleEntryComponent buildSuccessEntry(Resource resource) {
    final Bundle.BundleEntryComponent responseEntry = new Bundle.BundleEntryComponent();
    responseEntry.setResponse(new Bundle.BundleEntryResponseComponent().setStatus(STATUS_OK));
    responseEntry.setResource(resource);
    return responseEntry;
  }

  /**
   * Creates an error batch-response entry with an {@link OperationOutcome} payload.
   *
   * @param status response status string
   * @param diagnostics diagnostics message
   * @return error response entry
   */
  private Bundle.BundleEntryComponent buildErrorEntry(String status, String diagnostics) {
    final Bundle.BundleEntryComponent responseEntry = new Bundle.BundleEntryComponent();
    responseEntry.setResponse(new Bundle.BundleEntryResponseComponent().setStatus(status));

    final OperationOutcome outcome = new OperationOutcome();
    outcome
        .addIssue()
        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
        .setCode(OperationOutcome.IssueType.PROCESSING)
        .setDiagnostics(diagnostics == null ? "Unknown error" : diagnostics);

    responseEntry.setResource(outcome);
    return responseEntry;
  }

  /**
   * Resolves status mapping for non-native exceptions.
   *
   * @param exception exception thrown while processing an entry
   * @return status string for the response entry
   */
  private String resolveStatusCode(Exception exception) {
    if (exception instanceof InvalidRequestException
        || exception instanceof UnsupportedTerminologyException
        || exception instanceof UnknownScopeException) {
      return STATUS_BAD_REQUEST;
    }
    if (exception instanceof NotImplementedException) {
      return STATUS_NOT_IMPLEMENTED;
    }
    if (exception instanceof UnknownConceptException) {
      return STATUS_UNPROCESSABLE;
    }
    return STATUS_INTERNAL_SERVER_ERROR;
  }

  /**
   * Parses query parameters from the given URL.
   *
   * @param url entry URL that may include query parameters
   * @return decoded query parameters
   */
  private Map<String, List<String>> parseQueryParams(String url) {
    final Map<String, List<String>> params = new HashMap<>();
    final String rawQuery = extractRawQuery(url);

    if (rawQuery == null || rawQuery.isEmpty()) {
      return params;
    }

    for (String keyValue : rawQuery.split("&")) {
      if (StringUtils.isEmpty(keyValue)) {
        continue;
      }

      final String[] pair = keyValue.split("=", 2);
      final String key = decodeQueryComponent(pair[0]);
      final String value = pair.length > 1 ? decodeQueryComponent(pair[1]) : "";
      params.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    return params;
  }

  /**
   * Extracts the raw query fragment from a URL.
   *
   * @param url URL to inspect
   * @return raw query string without leading {@code ?}, or {@code null}
   */
  private String extractRawQuery(String url) {
    try {
      return new URI(url).getRawQuery();
    } catch (URISyntaxException _) {
      final String[] urlAndQuery = url.split("\\?", 2);
      if (urlAndQuery.length < 2 || StringUtils.isEmpty(urlAndQuery[1])) {
        return null;
      }
      return urlAndQuery[1];
    }
  }

  /**
   * Decodes one query component using UTF-8.
   *
   * @param value encoded query part
   * @return decoded query part
   */
  private String decodeQueryComponent(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
