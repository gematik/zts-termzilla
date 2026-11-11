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

import static ca.uhn.fhir.rest.annotation.OperationParam.MAX_UNLIMITED;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.ABSTRACT_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.CODEABLECONCEPT_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.CODE_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.CODING_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.CONTEXT_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.DATE_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.DISPLAY_LANGUAGE_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.DISPLAY_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.INFER_SYSTEM_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.LENIENT_DISPLAY_VALIDATION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.SYSTEM_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.SYSTEM_VERSION_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.URL_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.VALUESET_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter.VALUESET_VERSION_PARAM;

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
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.constants.HttpHeaderConstants;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.provider.bundle.AlternativeResourceBundleProvider;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.server.service.vs.CombiningVsExpandParameterValidator;
import io.micrometer.common.util.StringUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Der ValueSetResourceProvider stellt die REST-Operationen für ValueSets bereit. Er implementiert
 * lediglich Leseoperationen für ValueSets und bietet die Möglichkeit, ValueSets zu suchen und zu
 * expandieren.
 */
@Slf4j
public class ValueSetResourceProvider implements IResourceProvider {

  private final CombiningTerminologyService terminologyService;
  private final FhirContext fhirContext;
  private final ConfigProperties properties;

  // ==================================================================================================

  public ValueSetResourceProvider(
      CombiningTerminologyService terminologyService, ConfigProperties properties) {
    this.terminologyService = terminologyService;
    this.fhirContext = FhirContext.forR4Cached();
    this.properties = properties;
  }

  // ==================================================================================================

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return ValueSet.class;
  }

  // ==================================================================================================
  // Basic REST
  // ==================================================================================================

  /**
   * Liefert ein ValueSet anhand seiner Id zurück.
   *
   * @param theId Id des ValueSets
   * @return ValueSet
   */
  @Read()
  public ValueSet read(RequestDetails requestDetails, @IdParam IdType theId) {
    try {
      final String scope = requestDetails.getTenantId();
      terminologyService.checkScope(scope);
      return terminologyService.getValueSet(scope, theId);
    } catch (UnknownScopeException e) {
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================
  // Search
  // ==================================================================================================

  // Implementiert die Suche nach CodeSystemen anhand der durch FHIR definierten Suchparameter
  @Search
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public IBundleProvider searchValueSet(
      RequestDetails requestDetails,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_ID) TokenParam idParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_SOURCE) UriParam sourceParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_URL) UriParam urlParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_VERSION) TokenParam versionParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_DATE) DateParam dateParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_EFFECTIVE)
          DateParam effectiveParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_STATUS) TokenParam statusParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_NAME) StringParam nameParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_TITLE) StringParam titleParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_DESCRIPTION)
          StringParam descriptionParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_PUBLISHER)
          StringParam publisherParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_CONTENT) StringParam contentParam,
      @OptionalParam(name = VsSearchParameter.SP_CODE) TokenParam codeParam,
      @OptionalParam(name = VsSearchParameter.SP_REFERENCE) UriParam referenceParam,
      @OptionalParam(name = VsSearchParameter.SP_EXPANSION) UriParam expansionParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_SORT) TokenOrListParam sortParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_COUNT) NumberParam countParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_OFFSET) NumberParam offsetParam) {

    try {
      final String scope = requestDetails.getTenantId();
      terminologyService.checkScope(scope);
      final List<IdType> matchingResourceIds =
          terminologyService.searchValueSet(
              requestDetails.getTenantId(),
              VsSearchParameter.builder()
                  .idParam(idParam)
                  .sourceParam(sourceParam)
                  .urlParam(urlParam)
                  .versionParam(versionParam)
                  .dateParam(dateParam)
                  .effectiveParam(effectiveParam)
                  .statusParam(statusParam)
                  .nameParam(nameParam)
                  .titleParam(titleParam)
                  .descriptionParam(descriptionParam)
                  .publisherParam(publisherParam)
                  .contentParam(contentParam)
                  .codeParam(codeParam)
                  .referenceParam(referenceParam)
                  .expansionParam(expansionParam)
                  .sortParam(sortParam)
                  .build());

      return new AlternativeResourceBundleProvider(
          matchingResourceIds,
          id -> terminologyService.getValueSet(scope, id, properties.getResourceRetrievalMode()),
          offsetParam != null ? offsetParam.getValue().intValue() : null,
          countParam != null ? countParam.getValue().intValue() : null);

    } catch (NotImplementedException | UnknownScopeException e) {
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================
  // Operations
  // ==================================================================================================

  // Expandieren von ValueSets
  @Operation(name = "$expand", idempotent = true)
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public ValueSet expandValueSet(
      RequestDetails requestDetails,
      @OperationParam(name = "url", max = 1) UriType urlParam,
      @OperationParam(name = "valueSet", max = 1) ValueSet valueSetParam,
      @OperationParam(name = "valueSetVersion", max = 1) StringType valueSetVersionParam,
      @OperationParam(name = "context", max = 1) UriType contextParam,
      @OperationParam(name = "contextDirection", max = 1) CodeType contextDirectionParam,
      @OperationParam(name = "filter", max = 1) StringType filterParam,
      @OperationParam(name = "date", max = 1) DateTimeType dateParam,
      @OperationParam(name = "offset", max = 1) UnsignedIntType offsetParam,
      @OperationParam(name = "count", max = 1) UnsignedIntType countParam,
      @OperationParam(name = "includeDesignations", max = 1) BooleanType includeDesignationsParam,
      @OperationParam(name = "designation", max = MAX_UNLIMITED) List<StringType> designationParam,
      @OperationParam(name = "includeDefinition", max = 1) BooleanType includeDefinitionParam,
      @OperationParam(name = "activeOnly", max = 1) BooleanType activeOnlyParam,
      @OperationParam(name = "excludeNested", max = 1) BooleanType excludeNestedParam,
      @OperationParam(name = "excludeNotForUI", max = 1) BooleanType excludeNotForUIParam,
      @OperationParam(name = "excludePostCoordinated", max = 1)
          BooleanType excludePostcoordinatedParam,
      @OperationParam(name = "displayLanguage", max = 1) CodeType displayLanguageParam,
      @OperationParam(name = "exclude-system", max = MAX_UNLIMITED)
          List<CanonicalType> excludeSystemParam,
      @OperationParam(name = "system-version", max = MAX_UNLIMITED)
          List<CanonicalType> systemVersionParam,
      @OperationParam(name = "check-system-version", max = MAX_UNLIMITED)
          List<CanonicalType> checkSystemVersionParam,
      @OperationParam(name = "force-system-version", max = MAX_UNLIMITED)
          List<CanonicalType> forceSystemVersionParam,
      @OperationParam(name = "useSupplement", max = MAX_UNLIMITED)
          List<CanonicalType> useSupplementParam) {
    try {
      final String scope = requestDetails.getTenantId();
      terminologyService.checkScope(scope);

      final String acceptLanguageHeader =
          requestDetails.getHeader(HttpHeaderConstants.ACCEPT_LANGUAGE);
      LanguageParameter languageAcceptLanguageHeader = null;
      if (StringUtils.isNotEmpty(acceptLanguageHeader)) {
        languageAcceptLanguageHeader = LanguageParameter.parse(acceptLanguageHeader);
      }

      VsExpandParameter vsExpandParameter =
          VsExpandParameter.builder()
              .urlParam(urlParam)
              .valueSetParam(valueSetParam)
              .valueSetVersionParam(valueSetVersionParam)
              .contextParam(contextParam)
              .contextDirectionParam(contextDirectionParam)
              .filterParam(filterParam)
              .dateParam(dateParam)
              .offsetParam(offsetParam)
              .countParam(countParam)
              .includeDesignationsParam(includeDesignationsParam)
              .designationParam(designationParam)
              .includeDefinitionParam(includeDefinitionParam)
              .activeOnlyParam(activeOnlyParam)
              .excludeNestedParam(excludeNestedParam)
              .excludeNotForUIParam(excludeNotForUIParam)
              .excludePostcoordinatedParam(excludePostcoordinatedParam)
              .displayLanguageParam(displayLanguageParam)
              .excludeSystemParam(excludeSystemParam)
              .systemVersionParam(systemVersionParam)
              .checkSystemVersionParam(checkSystemVersionParam)
              .forceSystemVersionParam(forceSystemVersionParam)
              .useSupplementParam(useSupplementParam)
              .languageAcceptLanguageHeader(languageAcceptLanguageHeader)
              .build();

      // A url-based $expand requires an explicit
      // valueSetVersion. This is enforced only at the public API boundary, not for internal
      // reuse (e.g. $validate-code).
      CombiningVsExpandParameterValidator.validateRequestBoundaryParameters(vsExpandParameter);

      return terminologyService.expandValueSet(requestDetails.getTenantId(), vsExpandParameter);

    } catch (NotImplementedException
        | UnsupportedTerminologyException
        | UnknownScopeException
        | UnknownConceptException e) {
      log.error("Error while processing request: {}", e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  @Operation(name = "$validate-code", idempotent = true)
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public Parameters validateCodeVs(
      RequestDetails requestDetails,
      @OperationParam(name = URL_PARAM, max = 1) UriParam urlParam,
      @OperationParam(name = CONTEXT_PARAM, max = 1) UriParam contextParam,
      @OperationParam(name = VALUESET_PARAM, max = 1) ValueSet valueSetParam,
      @OperationParam(name = VALUESET_VERSION_PARAM, max = 1) StringParam valueSetVersionParam,
      @OperationParam(name = CODE_PARAM, max = 1) CodeType codeParam, // Changed from TokenParam
      @OperationParam(name = SYSTEM_PARAM, max = 1) UriParam systemParam,
      @OperationParam(name = SYSTEM_VERSION_PARAM, max = 1) StringParam systemVersionParam,
      @OperationParam(name = DISPLAY_PARAM, max = 1) StringType displayParam,
      // Changed from StringParam
      @OperationParam(name = CODING_PARAM, max = 1) Coding coding,
      @OperationParam(name = CODEABLECONCEPT_PARAM, max = 1) CodeableConcept codeableConcept,
      @OperationParam(name = DATE_PARAM, max = 1) DateParam dateParam,
      @OperationParam(name = ABSTRACT_PARAM, max = 1) TokenParam abstractParam,
      @OperationParam(name = DISPLAY_LANGUAGE_PARAM, max = 1) TokenParam displayLanguageParam,
      @OperationParam(name = LENIENT_DISPLAY_VALIDATION_PARAM, max = 1)
          BooleanType lenientDisplayValidation,
      @OperationParam(name = INFER_SYSTEM_PARAM, max = 1) BooleanType inferSystem) {

    // Convert CodeType
    TokenParam codeTokenParam = null;
    if (codeParam != null && codeParam.hasValue()) {
      codeTokenParam = new TokenParam(null, codeParam.getValue());
    }

    StringParam displayStringParam = null;
    if (displayParam != null && displayParam.hasValue()) {
      displayStringParam = new StringParam(displayParam.getValue());
    }

    // Prüfen, ob der Scope des Requests gültig ist

    try {
      final String scope = requestDetails.getTenantId();
      terminologyService.checkScope(scope);

      VsValidateCodeParameter vsValidateCodeParameter =
          VsValidateCodeParameter.builder()
              .urlParam(urlParam)
              .contextParam(contextParam)
              .valueSetParam(valueSetParam)
              .valueSetVersionParam(valueSetVersionParam)
              .codeParam(codeTokenParam)
              .systemParam(systemParam)
              .systemVersionParam(systemVersionParam)
              .displayParam(displayStringParam)
              .codingParam(coding)
              .codeableConceptParam(codeableConcept)
              .dateParam(dateParam)
              .abstractParam(abstractParam)
              .displayLanguageParam(displayLanguageParam)
              .lenientDisplayValidationParam(lenientDisplayValidation)
              .inferSystemParam(inferSystem)
              .build();

      return (Parameters)
          terminologyService
              .validateCodeVs(scope, vsValidateCodeParameter)
              .toParameters(fhirContext);

    } catch (NotImplementedException
        | UnsupportedTerminologyException
        | UnknownScopeException
        | UnknownConceptException e) {
      log.error("Error while processing request: {}", e.getMessage());
      throw new InvalidRequestException(e);
    }
  }
}
