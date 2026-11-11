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

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import java.io.Serializable;
import java.util.Optional;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hl7.fhir.r4.model.ValueSet;

@Setter
@SuperBuilder(buildMethodName = "buildInternal")
public class VsSearchParameter extends TerminologyResourceSearchParameter implements Serializable {

  public static final String SP_CODE = ValueSet.SP_CODE;
  public static final String SP_REFERENCE = ValueSet.SP_REFERENCE;
  public static final String SP_EXPANSION = ValueSet.SP_EXPANSION;

  protected TokenParam codeParam;
  protected UriParam referenceParam;
  protected UriParam expansionParam;

  @Override
  public void setUrlParam(UriParam urlParam) {
    this.urlParam = urlParam;
    extractVersionFromParam(urlParam);
  }

  public Optional<TokenParam> getCode() {
    return Optional.ofNullable(codeParam);
  }

  public Optional<UriParam> getReference() {
    return Optional.ofNullable(referenceParam);
  }

  public Optional<UriParam> getExpansion() {
    return Optional.ofNullable(expansionParam);
  }
}
