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
package de.gematik.zts.termzilla.common.codesystem.search;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;

/** Test data helper for InMemoryCodeSystemSearchHelper unit tests. */
public final class InMemoryCodeSystemSearchHelperTestDataHelper {

  static final String URL_ALPHA = "http://example.org/fhir/CodeSystem/alpha";
  static final String URL_BETA = "http://example.org/fhir/CodeSystem/beta";
  static final String VERSION_SHARED = "shared-value";
  static final String NAME_ALPHA = "AlphaName";
  static final String NAME_BETA = "BetaName";
  static final String TITLE_ALPHA = "Alpha Title";
  static final String DESCRIPTION_ALPHA = "Alpha Description";
  static final String DESCRIPTION_BETA = "Beta Description";
  static final String PUBLISHER_ALPHA = "AlphaPublisher";
  static final String PUBLISHER_BETA = "BetaPublisher";

  private InMemoryCodeSystemSearchHelperTestDataHelper() {
    throw new AssertionError("InMemoryCodeSystemSearchHelperTestDataHelper is a utility class");
  }

  static CodeSystem createCodeSystem(
      String id,
      String url,
      String version,
      String name,
      String title,
      String description,
      String publisher) {

    CodeSystem codeSystem = new CodeSystem();
    codeSystem.setId(id);

    if (url != null) {
      codeSystem.setUrl(url);
    }
    if (version != null) {
      codeSystem.setVersion(version);
    }
    if (name != null) {
      codeSystem.setName(name);
    }
    if (title != null) {
      codeSystem.setTitle(title);
    }
    if (description != null) {
      codeSystem.setDescription(description);
    }
    if (publisher != null) {
      codeSystem.setPublisher(publisher);
    }

    return codeSystem;
  }

  static List<CodeSystem> createDefaultCodeSystems() {
    CodeSystem alpha =
        createCodeSystem(
            "CodeSystem/alpha",
            URL_ALPHA,
            VERSION_SHARED,
            NAME_ALPHA,
            TITLE_ALPHA,
            DESCRIPTION_ALPHA,
            PUBLISHER_ALPHA);

    CodeSystem beta =
        createCodeSystem(
            "CodeSystem/beta",
            URL_BETA,
            "2.0.0",
            NAME_BETA,
            "Beta Title",
            DESCRIPTION_BETA,
            PUBLISHER_BETA);

    CodeSystem gamma = createCodeSystem("CodeSystem/gamma", null, null, null, null, null, null);

    return List.of(alpha, beta, gamma);
  }

  static CsSearchParameter emptySearchParameter() {
    return CsSearchParameter.builder().build();
  }

  static CsSearchParameter withContent(String contentValue) {
    return CsSearchParameter.builder().contentParam(new StringParam(contentValue)).build();
  }

  static CsSearchParameter withCode(String codeValue) {
    return CsSearchParameter.builder().codeParam(new TokenParam(codeValue)).build();
  }

  static CsSearchParameter withContentMode(String mode) {
    return CsSearchParameter.builder().contentModeParam(new TokenParam(mode)).build();
  }

  static CsSearchParameter withStatus(String status) {
    return CsSearchParameter.builder().statusParam(new TokenParam(status)).build();
  }

  static CsSearchParameter withMetadataNameAndPublisher(String namePrefix, String publisherPrefix) {
    return CsSearchParameter.builder()
        .nameParam(new StringParam(namePrefix))
        .publisherParam(new StringParam(publisherPrefix))
        .build();
  }

  static CsSearchParameter withMetadataSource(String sourceValue) {
    return CsSearchParameter.builder().sourceParam(new UriParam(sourceValue)).build();
  }
}
