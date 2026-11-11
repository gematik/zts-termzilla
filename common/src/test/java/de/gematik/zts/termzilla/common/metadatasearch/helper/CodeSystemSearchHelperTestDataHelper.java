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
package de.gematik.zts.termzilla.common.metadatasearch.helper;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;

/** Provides reusable test fixtures for {@link CodeSystemSearchHelperTest}. */
final class CodeSystemSearchHelperTestDataHelper {

  static final String SYSTEM_VALUE = "http://example.org/cs/system";
  static final String SYSTEM_WITH_VERSION = "http://example.org/cs/system|2.3.4";
  static final String CONTENT_MODE_VALUE = "complete";
  static final String SUPPLEMENTS_VALUE = "http://example.org/cs/base";
  static final String NAME_VALUE = "TestCodeSystemName";
  static final String STATUS_VALUE = "active";
  static final String URL_VALUE = "http://example.org/fhir/CodeSystem/test";
  static final String ID_VALUE = "cs-123";

  private CodeSystemSearchHelperTestDataHelper() {}

  static CsSearchParameter emptySearchParameter() {
    return CsSearchParameter.builder().build();
  }

  static UriParam uriParam(String value) {
    return new UriParam(value);
  }

  static TokenParam tokenParam(String value) {
    return new TokenParam(value);
  }

  static ReferenceParam referenceParam(String value) {
    return new ReferenceParam(value);
  }

  static StringParam stringParam(String value) {
    return new StringParam(value);
  }
}
