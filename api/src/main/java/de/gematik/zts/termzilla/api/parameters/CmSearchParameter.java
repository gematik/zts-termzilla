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
package de.gematik.zts.termzilla.api.parameters;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import java.io.Serializable;
import java.util.Optional;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hl7.fhir.r4.model.ConceptMap;

@Setter
@SuperBuilder(buildMethodName = "buildInternal")
public class CmSearchParameter extends TerminologyResourceSearchParameter implements Serializable {

  public static final String SP_SOURCE_CANONICAL = ConceptMap.SP_SOURCE;
  public static final String SP_SOURCE_URI = ConceptMap.SP_SOURCE_URI;
  public static final String SP_SOURCE_SYSTEM = ConceptMap.SP_SOURCE_SYSTEM;

  public static final String SP_SOURCE_CODE = ConceptMap.SP_SOURCE_CODE;

  public static final String SP_TARGET_CANONICAL = ConceptMap.SP_TARGET;
  public static final String SP_TARGET_URI = ConceptMap.SP_TARGET_URI;
  public static final String SP_TARGET_SYSTEM = ConceptMap.SP_TARGET_SYSTEM;

  public static final String SP_TARGET_CODE = ConceptMap.SP_TARGET_CODE;

  public static final String SP_DEPENDS_ON = ConceptMap.SP_DEPENDSON;
  public static final String SP_PRODUCT = ConceptMap.SP_PRODUCT;

  protected ReferenceParam sourceCanonicalParam;
  protected ReferenceParam sourceUriParam;
  protected UriParam sourceSystemParam;

  protected TokenParam sourceCodeParam;

  protected ReferenceParam targetCanonicalParam;
  protected ReferenceParam targetUriParam;
  protected UriParam targetSystemParam;

  protected TokenParam targetCodeParam;

  protected UriParam dependsOnParam;
  protected UriParam productParam;

  @Override
  public void setUrlParam(UriParam urlParam) {
    this.urlParam = urlParam;
    extractVersionFromParam(urlParam);
  }

  public Optional<ReferenceParam> getSourceCanonical() {
    return Optional.ofNullable(sourceCanonicalParam);
  }

  public Optional<ReferenceParam> getSourceUri() {
    return Optional.ofNullable(sourceUriParam);
  }

  public Optional<UriParam> getSourceSystem() {
    return Optional.ofNullable(sourceSystemParam);
  }

  public Optional<TokenParam> getSourceCode() {
    return Optional.ofNullable(sourceCodeParam);
  }

  public Optional<ReferenceParam> getTargetCanonical() {
    return Optional.ofNullable(targetCanonicalParam);
  }

  public Optional<ReferenceParam> getTargetUri() {
    return Optional.ofNullable(targetUriParam);
  }

  public Optional<UriParam> getTargetSystem() {
    return Optional.ofNullable(targetSystemParam);
  }

  public Optional<TokenParam> getTargetCode() {
    return Optional.ofNullable(targetCodeParam);
  }

  public Optional<UriParam> getDependsOn() {
    return Optional.ofNullable(dependsOnParam);
  }

  public Optional<UriParam> getProduct() {
    return Optional.ofNullable(productParam);
  }
}
