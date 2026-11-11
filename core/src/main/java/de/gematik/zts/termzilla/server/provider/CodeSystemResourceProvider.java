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

import static de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter.COMPOSITIONAL_PARAM;
import static de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter.EXACT_PARAM;
import static de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter.PROPERTY_PARAM;
import static de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter.SYSTEM_PARAM;
import static de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter.VERSION_PARAM;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.constants.HttpHeaderConstants;
import de.gematik.zts.termzilla.api.exceptions.InvalidParameterException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.provider.bundle.AlternativeResourceBundleProvider;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import io.micrometer.common.util.StringUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * FHIR Resource Provider for CodeSystem resources.
 *
 * <p>Provides REST operations and custom operations for CodeSystem resources according to the FHIR
 * R4 specification. This includes basic CRUD operations, search capabilities, and terminology
 * operations such as $lookup, $validate-code, and $find-matches.
 *
 * <p>All operations are tenant-aware and require a valid scope identifier.
 *
 * @see <a href="http://hl7.org/fhir/R4/codesystem.html">FHIR CodeSystem Resource</a>
 */
@Slf4j
public class CodeSystemResourceProvider implements IResourceProvider {

  private final CombiningTerminologyService terminologyService;
  private final FhirContext fhirContext;
  private final ConfigProperties properties;

  /**
   * Constructs a new CodeSystemResourceProvider.
   *
   * @param terminologyService the terminology service to delegate operations to
   */
  public CodeSystemResourceProvider(
      CombiningTerminologyService terminologyService, ConfigProperties properties) {
    this.terminologyService = terminologyService;
    this.fhirContext = FhirContext.forR4Cached();
    this.properties = properties;
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return CodeSystem.class;
  }

  // ==================================================================================================
  // Basic REST
  // ==================================================================================================

  /**
   * Retrieves a single CodeSystem resource by its ID.
   *
   * <p>This implements the FHIR read operation for CodeSystem resources.
   *
   * @param requestDetails the FHIR request details containing tenant information
   * @param theId the logical ID of the CodeSystem resource to retrieve
   * @return the requested CodeSystem resource
   * @throws InvalidRequestException if the scope is invalid or the resource cannot be found
   */
  @Read()
  public CodeSystem read(RequestDetails requestDetails, @IdParam IdType theId) {

    try {
      final String scope = requestDetails.getTenantId();
      log.debug("CodeSystem read request - scope: {}, id: {}", scope, theId.getIdPart());
      terminologyService.checkScope(scope);
      CodeSystem result = terminologyService.getCodeSystem(scope, theId);
      log.debug("CodeSystem read successful - id: {}", theId.getIdPart());
      return result;
    } catch (UnknownScopeException e) {
      log.error(
          "CodeSystem read failed - scope: {}, id: {}, error: {}",
          requestDetails.getTenantId(),
          theId.getIdPart(),
          e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================

  /**
   * Searches for CodeSystem resources based on FHIR-defined search parameters.
   *
   * <p>Supports various search parameters including URL, name, title, code, version, and more.
   * Results are returned as a paginated bundle.
   *
   * @param requestDetails the FHIR request details containing tenant information
   * @param idParam search by resource ID
   * @param sourceParam search by source
   * @param systemParam search by system URL
   * @param urlParam search by canonical URL
   * @param versionParam search by version
   * @param dateParam search by date
   * @param effectiveParam search by effective date
   * @param statusParam search by status (draft, active, retired)
   * @param contentModeParam search by content mode
   * @param nameParam search by name
   * @param titleParam search by title
   * @param descriptionParam search by description
   * @param publisherParam search by publisher
   * @param supplementsParam search by supplements reference
   * @param contentParam full-text search in content
   * @param codeParam search by code
   * @param sortParam sort parameters
   * @return a bundle provider for paginated results
   * @throws InvalidRequestException if the scope is invalid
   */
  @Search
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public IBundleProvider searchCodeSystem(
      RequestDetails requestDetails,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_ID) TokenParam idParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_SOURCE) UriParam sourceParam,
      @OptionalParam(name = CsSearchParameter.SP_SYSTEM) UriParam systemParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_URL) UriParam urlParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_VERSION) TokenParam versionParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_DATE) DateParam dateParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_EFFECTIVE)
          DateParam effectiveParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_STATUS) TokenParam statusParam,
      @OptionalParam(name = CsSearchParameter.SP_CONTENT_MODE) TokenParam contentModeParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_NAME) StringParam nameParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_TITLE) StringParam titleParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_DESCRIPTION)
          StringParam descriptionParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_PUBLISHER)
          StringParam publisherParam,
      @OptionalParam(name = CsSearchParameter.SP_SUPPLEMENTS) ReferenceParam supplementsParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_CONTENT) StringParam contentParam,
      @OptionalParam(name = CsSearchParameter.SP_CODE) TokenParam codeParam,
      @OptionalParam(name = CsSearchParameter.SP_LANGUAGE) TokenParam languageParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_SORT) TokenOrListParam sortParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_COUNT) NumberParam countParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_OFFSET) NumberParam offsetParam) {

    final String scope = requestDetails.getTenantId();
    try {
      log.debug("CodeSystem search request - scope: {}", scope);
      terminologyService.checkScope(scope);

      // Perform search (exceptions are handled in CombiningTerminologyService)
      final List<IdType> matchingResourceIds =
          terminologyService.searchCodeSystem(
              scope,
              CsSearchParameter.builder()
                  .idParam(idParam)
                  .sourceParam(sourceParam)
                  .systemParam(systemParam)
                  .urlParam(urlParam)
                  .versionParam(versionParam)
                  .dateParam(dateParam)
                  .effectiveParam(effectiveParam)
                  .statusParam(statusParam)
                  .contentModeParam(contentModeParam)
                  .nameParam(nameParam)
                  .titleParam(titleParam)
                  .descriptionParam(descriptionParam)
                  .publisherParam(publisherParam)
                  .supplementsParam(supplementsParam)
                  .contentParam(contentParam)
                  .codeParam(codeParam)
                  .languageParam(languageParam)
                  .sortParam(sortParam)
                  .build(),
              false);

      log.debug(
          "CodeSystem search successful - scope: {}, found: {} results",
          scope,
          matchingResourceIds.size());

      return new AlternativeResourceBundleProvider(
          matchingResourceIds,
          id -> terminologyService.getCodeSystem(scope, id, properties.getResourceRetrievalMode()),
          offsetParam != null ? offsetParam.getValue().intValue() : null,
          countParam != null ? countParam.getValue().intValue() : null);
    } catch (UnknownScopeException e) {
      log.error("CodeSystem search failed - scope: {}, error: {}", scope, e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================
  // Operations
  // ==================================================================================================

  /**
   * Performs a concept lookup operation ($lookup).
   *
   * <p>Given a code/system or Coding, returns additional information about the concept including
   * display name, definition, designations, and properties.
   *
   * @param requestDetails the FHIR request details containing tenant information
   * @param codeParam the code to look up
   * @param systemParam the code system URL
   * @param versionParam the code system version
   * @param codingParam a Coding to look up (alternative to code+system)
   * @param dateParam the date for which the information should be returned
   * @param displayLanguageParam the preferred display language
   * @param propertyListParam which properties to include in the response
   * @return Parameters resource containing the lookup results
   * @throws InvalidRequestException if the operation fails or parameters are invalid
   * @see <a href="http://hl7.org/fhir/R4/codesystem-operation-lookup.html">$lookup Operation</a>
   */
  @Operation(name = "$lookup", idempotent = true)
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public Parameters lookupCode(
      RequestDetails requestDetails,
      @OperationParam(name = "code", min = 0, max = 1) CodeType codeParam,
      @OperationParam(name = "system", min = 0, max = 1) UriType systemParam,
      @OperationParam(name = "version", min = 0, max = 1) StringType versionParam,
      @OperationParam(name = "coding", min = 0, max = 1) Coding codingParam,
      @OperationParam(name = "date", min = 0, max = 1) DateTimeType dateParam,
      @OperationParam(name = "displayLanguage", min = 0, max = 1) CodeType displayLanguageParam,
      @OperationParam(name = "property", min = 0, max = -1) List<CodeType> propertyListParam) {

    final String scope = requestDetails.getTenantId();

    try {

      log.debug("CodeSystem $lookup request - scope: {}", scope);
      terminologyService.checkScope(scope);

      // Sprachcode aus dem http header extrahieren
      final String acceptLanguageHeader =
          requestDetails.getHeader(HttpHeaderConstants.ACCEPT_LANGUAGE);
      LanguageParameter languageAcceptLanguageHeader = null;
      if (StringUtils.isNotEmpty(acceptLanguageHeader)) {
        languageAcceptLanguageHeader = LanguageParameter.parse(acceptLanguageHeader);
      }

      // Parameterobjekt bauen und Operation delegieren
      Parameters result =
          (Parameters)
              (terminologyService
                  .lookupCode(
                      scope,
                      CsLookupParameter.builder()
                          .codeParam(codeParam)
                          .systemParam(systemParam)
                          .versionParam(versionParam)
                          .codingParam(codingParam)
                          .dateParam(dateParam)
                          .displayLanguageParam(displayLanguageParam)
                          .propertyParam(propertyListParam)
                          .languageAcceptLanguageHeader(languageAcceptLanguageHeader)
                          .build())
                  .toParameters(fhirContext));

      log.debug("CodeSystem $lookup successful - scope: {}", scope);
      return result;

    } catch (NotImplementedException
        | UnsupportedTerminologyException
        | UnknownScopeException
        | InvalidParameterException
        | UnknownConceptException e) {
      log.error("CodeSystem $lookup failed - scope: {}, error: {}", scope, e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  // ---------------------------------------------------------------------------------------------------

  /**
   * Validates whether a code is in a CodeSystem ($validate-code).
   *
   * <p>Validates that a code exists in the specified CodeSystem and optionally checks the display
   * name. Can validate using code+system, a Coding, or a CodeableConcept.
   *
   * @param requestDetails the FHIR request details containing tenant information
   * @param urlParam the canonical URL of the CodeSystem
   * @param codeSystemParam the CodeSystem resource to validate against
   * @param codeParam the code to validate
   * @param versionParam the version of the CodeSystem
   * @param displayParam the display text to validate
   * @param codingParamParam a Coding to validate
   * @param codeableConceptParam a CodeableConcept to validate
   * @param dateParam the date for which validation should be performed
   * @param abstractParam whether abstract codes are acceptable
   * @param displayLanguageParam the language for display validation
   * @return Parameters resource with validation results (result, message, display)
   * @throws InvalidRequestException if validation fails or parameters are invalid
   * @see <a href="http://hl7.org/fhir/R4/codesystem-operation-validate-code.html">$validate-code
   *     Operation</a>
   */
  @Operation(name = "$validate-code", idempotent = true)
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public Parameters validateCode(
      RequestDetails requestDetails,
      @OperationParam(name = "url", min = 0, max = 1) UriType urlParam,
      @OperationParam(name = "codeSystem", min = 0, max = 1) CodeSystem codeSystemParam,
      @OperationParam(name = "code", min = 0, max = 1) CodeType codeParam,
      @OperationParam(name = "version", min = 0, max = 1) StringType versionParam,
      @OperationParam(name = "display", min = 0, max = 1) StringType displayParam,
      @OperationParam(name = "coding", min = 0, max = 1) Coding codingParamParam,
      @OperationParam(name = "codeableConcept", min = 0, max = 1)
          CodeableConcept codeableConceptParam,
      @OperationParam(name = "date", min = 0, max = 1) DateTimeType dateParam,
      @OperationParam(name = "abstract", min = 0, max = 1) BooleanType abstractParam,
      @OperationParam(name = "displayLanguage", min = 0, max = 1) CodeType displayLanguageParam) {

    final String scope = requestDetails.getTenantId();
    try {
      log.debug("CodeSystem $validate-code request - scope: {}", scope);
      terminologyService.checkScope(scope);

      // Sprachcode aus dem http header extrahieren
      final String acceptLanguageHeader =
          requestDetails.getHeader(HttpHeaderConstants.ACCEPT_LANGUAGE);
      LanguageParameter languageAcceptLanguageHeader = null;
      if (StringUtils.isNotEmpty(acceptLanguageHeader)) {
        languageAcceptLanguageHeader = LanguageParameter.parse(acceptLanguageHeader);
      }

      Parameters result =
          (Parameters)
              terminologyService
                  .validateCode(
                      scope,
                      CsValidateCodeParameter.builder()
                          .urlParam(urlParam)
                          .codeSystemParam(codeSystemParam)
                          .codeParam(codeParam)
                          .versionParam(versionParam)
                          .displayParam(displayParam)
                          .codingParam(codingParamParam)
                          .codeableConceptParam(codeableConceptParam)
                          .dateParam(dateParam)
                          .abstractParam(abstractParam)
                          .displayLanguageParam(displayLanguageParam)
                          .languageAcceptLanguageHeader(languageAcceptLanguageHeader)
                          .build())
                  .toParameters(fhirContext);

      log.debug("CodeSystem $validate-code successful - scope: {}", scope);
      return result;

    } catch (NotImplementedException
        | UnsupportedTerminologyException
        | UnknownScopeException
        | UnknownConceptException e) {
      log.error(
          "CodeSystem $validate-code failed - scope: {}, error: {}",
          scope,
          urlParam,
          codeParam,
          e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  // ---------------------------------------------------------------------------------------------------

  /**
   * Finds codes in a CodeSystem that match specified properties ($find-matches).
   *
   * <p>This operation searches for concepts that match one or more specified property values. It
   * supports both exact and partial matching.
   *
   * @param requestDetails the FHIR request details containing tenant information
   * @param systemParam the CodeSystem URL to search in
   * @param versionParam the version of the CodeSystem
   * @param propertyParamList list of property filters (property code and value)
   * @param exactParam whether to use exact matching (true) or partial matching (false)
   * @param compositionalParam whether to include compositional grammar matches
   * @return Parameters resource containing matching concepts
   * @throws InvalidRequestException if the operation fails or parameters are invalid
   * @see <a href="http://hl7.org/fhir/R4/codesystem-operation-find-matches.html">$find-matches
   *     Operation</a>
   */
  @Operation(name = "$find-matches", idempotent = true)
  public Parameters findMatches(
      RequestDetails requestDetails,
      @OperationParam(name = SYSTEM_PARAM, min = 0, max = 1) UriType systemParam,
      @OperationParam(name = VERSION_PARAM, min = 0, max = 1) StringType versionParam,
      @OperationParam(name = PROPERTY_PARAM, min = 1)
          List<Parameters.ParametersParameterComponent> propertyParamList,
      @OperationParam(name = EXACT_PARAM, min = 1, max = 1) BooleanType exactParam,
      @OperationParam(name = COMPOSITIONAL_PARAM, min = 0, max = 1)
          BooleanType compositionalParam) {

    final String scope = requestDetails.getTenantId();
    try {
      log.debug("CodeSystem $find-matches request - scope: {}", scope);
      terminologyService.checkScope(scope);

      // Sprachcode aus dem http header extrahieren
      final String acceptLanguageHeader =
          requestDetails.getHeader(HttpHeaderConstants.ACCEPT_LANGUAGE);
      LanguageParameter languageAcceptLanguageHeader = null;
      if (StringUtils.isNotEmpty(acceptLanguageHeader)) {
        languageAcceptLanguageHeader = LanguageParameter.parse(acceptLanguageHeader);
      }

      Parameters result =
          (Parameters)
              terminologyService
                  .findMatches(
                      scope,
                      CsFindMatchesParameter.builder()
                          .systemParam(systemParam)
                          .versionParam(versionParam)
                          .propertyParamList(propertyParamList)
                          .exactParam(exactParam)
                          .compositionalParam(compositionalParam)
                          .languageAcceptLanguageHeader(languageAcceptLanguageHeader)
                          .build())
                  .toParameters(fhirContext);

      log.debug("CodeSystem $find-matches successful - scope: {}", scope);
      return result;

    } catch (NotImplementedException | UnsupportedTerminologyException | UnknownScopeException e) {
      log.error("CodeSystem $find-matches failed - scope: {}, error: {}", scope, e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  @Operation(name = "$subsumes", idempotent = true)
  public Parameters subsumes(RequestDetails requestDetails) throws NotImplementedException {
    throw new NotImplementedException("Operation $subsumes is not implemented yet");
  }
  // ---------------------------------------------------------------------------------------------------

}
