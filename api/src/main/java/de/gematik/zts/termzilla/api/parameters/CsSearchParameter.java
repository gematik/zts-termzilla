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
import org.hl7.fhir.r4.model.CodeSystem;

@Setter
@SuperBuilder(buildMethodName = "buildInternal")
public class CsSearchParameter extends TerminologyResourceSearchParameter implements Serializable {

  public static final String SP_SYSTEM = CodeSystem.SP_SYSTEM;
  public static final String SP_CONTENT_MODE = CodeSystem.SP_CONTENT_MODE;
  public static final String SP_SUPPLEMENTS = CodeSystem.SP_SUPPLEMENTS;
  public static final String SP_CODE = CodeSystem.SP_CODE;
  public static final String SP_LANGUAGE = CodeSystem.SP_LANGUAGE;

  protected UriParam systemParam;
  protected TokenParam contentModeParam;
  protected ReferenceParam supplementsParam;
  protected TokenParam codeParam;
  protected TokenParam languageParam;

  // Override setters to extract version from system and url params
  @Override
  public void setUrlParam(UriParam urlParam) {
    this.urlParam = urlParam;
    extractVersionFromParam(urlParam);
  }

  public void setSystemParam(UriParam systemParam) {
    this.systemParam = systemParam;
    extractVersionFromParam(systemParam);
  }

  public Optional<UriParam> getSystem() {
    return Optional.ofNullable(systemParam);
  }

  public Optional<TokenParam> getContentMode() {
    return Optional.ofNullable(contentModeParam);
  }

  public Optional<ReferenceParam> getSupplements() {
    return Optional.ofNullable(supplementsParam);
  }

  public Optional<TokenParam> getCode() {
    return Optional.ofNullable(codeParam);
  }

  public Optional<TokenParam> getLanguage() {
    return Optional.ofNullable(languageParam);
  }

  // ----------------------------------------------
  // Helper Methods
  // ----------------------------------------------

  @Override
  public boolean isMetadataSearch() {
    return idParam != null
        || sourceParam != null
        || urlParam != null
        || systemParam != null
        || contentModeParam != null
        || supplementsParam != null
        || versionParam != null
        || dateParam != null
        || statusParam != null
        || nameParam != null
        || titleParam != null
        || descriptionParam != null
        || publisherParam != null;
  }

  public boolean isCodeSearch() {
    return codeParam != null;
  }

  @Override
  protected void postBuildNormalize() {
    super.postBuildNormalize();
    extractVersionFromParam(systemParam);
  }
}
