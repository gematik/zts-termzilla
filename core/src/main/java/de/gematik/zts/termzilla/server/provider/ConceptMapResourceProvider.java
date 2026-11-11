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
import ca.uhn.fhir.rest.annotation.Operation;
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
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.parameters.CmSearchParameter;
import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import de.gematik.zts.termzilla.configuration.ConfigProperties;
import de.gematik.zts.termzilla.server.provider.bundle.AlternativeResourceBundleProvider;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;

/**
 * This class implements the FHIR ResourceProvider for ConceptMap.
 *
 * <p>It is responsible for handling requests related to ConceptMap resources, including reading and
 * searching for ConceptMaps.
 */
@Slf4j
public class ConceptMapResourceProvider implements IResourceProvider {

  private final CombiningTerminologyService terminologyService;

  private final ConfigProperties properties;

  public ConceptMapResourceProvider(
      CombiningTerminologyService terminologyService, ConfigProperties properties) {
    this.terminologyService = terminologyService;
    this.properties = properties;
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return ConceptMap.class;
  }

  // ==================================================================================================
  // Basic REST
  // ==================================================================================================

  // return ConceptMap by its id
  @Read()
  public ConceptMap read(RequestDetails requestDetails, @IdParam IdType theId) {

    try {
      final String scope = requestDetails.getTenantId();
      log.debug("ConceptMap read request - scope: {}, id: {}", scope, theId.getIdPart());
      terminologyService.checkScope(scope);
      ConceptMap result = terminologyService.getConceptMap(scope, theId);
      log.debug("ConceptMap read successful - id: {}", theId.getIdPart());
      return result;
    } catch (UnknownScopeException e) {
      log.error(
          "ConceptMap read failed - scope: {}, id: {}, error: {}",
          requestDetails.getTenantId(),
          theId.getIdPart(),
          e.getMessage());
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================

  // search ConceptMap
  @Search
  @SuppressWarnings("java:S107")
  // Rest API method is supposed to have many parameters, so we ignore the warning about too many
  // parameters for this method
  public IBundleProvider searchConceptMap(
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
      @OptionalParam(name = CmSearchParameter.SP_SOURCE_CANONICAL)
          ReferenceParam sourceCanonicalParam,
      @OptionalParam(name = CmSearchParameter.SP_SOURCE_URI) ReferenceParam sourceUriParam,
      @OptionalParam(name = CmSearchParameter.SP_SOURCE_SYSTEM) UriParam sourceSystemParam,
      @OptionalParam(name = CmSearchParameter.SP_SOURCE_CODE) TokenParam sourceCodeParam,
      @OptionalParam(name = CmSearchParameter.SP_TARGET_CANONICAL)
          ReferenceParam targetCanonicalParam,
      @OptionalParam(name = CmSearchParameter.SP_TARGET_URI) ReferenceParam targetUriParam,
      @OptionalParam(name = CmSearchParameter.SP_TARGET_SYSTEM) UriParam targetSystemParam,
      @OptionalParam(name = CmSearchParameter.SP_TARGET_CODE) TokenParam targetCodeParam,
      @OptionalParam(name = CmSearchParameter.SP_DEPENDS_ON) UriParam dependsOnParam,
      @OptionalParam(name = CmSearchParameter.SP_PRODUCT) UriParam productParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_SORT) TokenOrListParam sortParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_COUNT) NumberParam countParam,
      @OptionalParam(name = TerminologyResourceSearchParameter.SP_OFFSET) NumberParam offsetParam) {

    // TODO HAPI is not handling the %7C character in the search for Conceptmap correctly ... in
    // CodeSystem and ValueSet it works fine and is translated to a pipe character |

    try {
      final String scope = requestDetails.getTenantId();
      terminologyService.checkScope(scope);

      // Perform search (caching is handled in CombiningTerminologyService)
      final List<IdType> matchingResourceIds =
          terminologyService.searchConceptMap(
              requestDetails.getTenantId(),
              CmSearchParameter.builder()
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
                  .sourceCanonicalParam(sourceCanonicalParam)
                  .sourceUriParam(sourceUriParam)
                  .sourceSystemParam(sourceSystemParam)
                  .sourceCodeParam(sourceCodeParam)
                  .targetCanonicalParam(targetCanonicalParam)
                  .targetUriParam(targetUriParam)
                  .targetSystemParam(targetSystemParam)
                  .targetCodeParam(targetCodeParam)
                  .dependsOnParam(dependsOnParam)
                  .productParam(productParam)
                  .sortParam(sortParam)
                  .build());

      return new AlternativeResourceBundleProvider(
          matchingResourceIds,
          id -> terminologyService.getConceptMap(scope, id, properties.getResourceRetrievalMode()),
          offsetParam != null ? offsetParam.getValue().intValue() : null,
          countParam != null ? countParam.getValue().intValue() : null);
    } catch (NotImplementedException | UnknownScopeException e) {
      throw new InvalidRequestException(e);
    }
  }

  // ==================================================================================================
  // Operations
  // ==================================================================================================

  // Translate ConceptMap (Operation $translate)
  @Operation(name = "$translate", idempotent = true)
  public Parameters translateConceptMap(RequestDetails requestDetails)
      throws NotImplementedException {

    throw new NotImplementedException("Operation $translate is not implemented yet");
  }

  // Closure ConceptMap (Operation $closure)
  @Operation(name = "$closure", idempotent = true)
  public ConceptMap closureConceptMap(RequestDetails requestDetails)
      throws NotImplementedException {

    throw new NotImplementedException("Operation $closure is not implemented yet");
  }
}
