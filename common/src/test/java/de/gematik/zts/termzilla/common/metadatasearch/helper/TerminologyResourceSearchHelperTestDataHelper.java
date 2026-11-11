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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;

/** Provides reusable test fixtures for {@link TerminologyResourceSearchHelperTest}. */
final class TerminologyResourceSearchHelperTestDataHelper {

  static final String PUBLISHER_VALUE = "Gematik Publisher";
  static final String DESCRIPTION_VALUE = "ValueSet description";
  static final String TITLE_VALUE = "Test ValueSet Title";
  static final String NAME_VALUE = "TestValueSetName";
  static final String STATUS_VALUE = "active";
  static final String VERSION_VALUE = "1.2.3";
  static final String URL_VALUE = "http://example.org/fhir/ValueSet/test";
  static final String SOURCE_VALUE = "http://example.org/source/server";
  static final String ID_VALUE = "vs-123";

  private TerminologyResourceSearchHelperTestDataHelper() {}

  static VsSearchParameter emptySearchParameter() {
    return VsSearchParameter.builder().build();
  }

  static StringParam stringParam(String value) {
    return new StringParam(value);
  }

  static DateParam dateParam(String value) {
    return new DateParam(value);
  }

  static TokenParam tokenParam(String value) {
    return new TokenParam(value);
  }

  static UriParam uriParam(String value) {
    return new UriParam(value);
  }
}
