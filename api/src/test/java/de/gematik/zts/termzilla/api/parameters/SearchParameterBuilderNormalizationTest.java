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

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Verifies post-build normalization for search parameter builders. */
@DisplayName("SearchParameter builder normalization tests")
class SearchParameterBuilderNormalizationTest {

  @Nested
  @DisplayName("TerminologyResourceSearchParameter.builder()")
  class TerminologyResourceBuilderTests {

    @Test
    @DisplayName("Extracts version from url when only url contains canonical version")
    void extractsVersionFromUrlDuringBuild() {
      // GIVEN
      UriParam urlParam = TestDataHelper.uriWithVersion();

      // WHEN
      TerminologyResourceSearchParameter parameter =
          TerminologyResourceSearchParameter.builder().urlParam(urlParam).build();

      // THEN
      assertThat(parameter.getUrl())
          .get()
          .extracting(UriParam::getValue)
          .isEqualTo(TestDataHelper.CANONICAL_URL);
      assertThat(parameter.getVersion())
          .get()
          .extracting(TokenParam::getValue)
          .isEqualTo(TestDataHelper.VERSION);
    }

    @Test
    @DisplayName(
        "Keeps explicit version untouched and does not split url when version is already set")
    void keepsExplicitVersionWhenVersionParamAlreadyPresent() {
      // GIVEN
      UriParam urlParam = TestDataHelper.uriWithVersion();
      TokenParam explicitVersion = new TokenParam(TestDataHelper.EXPLICIT_VERSION);

      // WHEN
      TerminologyResourceSearchParameter parameter =
          TerminologyResourceSearchParameter.builder()
              .urlParam(urlParam)
              .versionParam(explicitVersion)
              .build();

      // THEN
      assertThat(parameter.getUrl())
          .get()
          .extracting(UriParam::getValue)
          .isEqualTo(TestDataHelper.CANONICAL_WITH_VERSION);
      assertThat(parameter.getVersion())
          .get()
          .extracting(TokenParam::getValue)
          .isEqualTo(TestDataHelper.EXPLICIT_VERSION);
    }
  }

  @Nested
  @DisplayName("CsSearchParameter.builder()")
  class CsSearchParameterBuilderTests {

    @Test
    @DisplayName("Extracts version from system when url is not provided")
    void extractsVersionFromSystemDuringBuild() {
      // GIVEN
      UriParam systemParam = TestDataHelper.uriWithVersion();

      // WHEN
      CsSearchParameter parameter = CsSearchParameter.builder().systemParam(systemParam).build();

      // THEN
      assertThat(parameter.getSystem())
          .get()
          .extracting(UriParam::getValue)
          .isEqualTo(TestDataHelper.CANONICAL_URL);
      assertThat(parameter.getVersion())
          .get()
          .extracting(TokenParam::getValue)
          .isEqualTo(TestDataHelper.VERSION);
    }
  }

  @Nested
  @DisplayName("VsSearchParameter.builder()")
  class VsSearchParameterBuilderTests {

    @Test
    @DisplayName("Extracts version from url during build")
    void extractsVersionFromUrlDuringBuild() {
      // GIVEN
      UriParam urlParam = TestDataHelper.uriWithVersion();

      // WHEN
      VsSearchParameter parameter = VsSearchParameter.builder().urlParam(urlParam).build();

      // THEN
      assertThat(parameter.getUrl())
          .get()
          .extracting(UriParam::getValue)
          .isEqualTo(TestDataHelper.CANONICAL_URL);
      assertThat(parameter.getVersion())
          .get()
          .extracting(TokenParam::getValue)
          .isEqualTo(TestDataHelper.VERSION);
    }
  }

  @Nested
  @DisplayName("CmSearchParameter.builder()")
  class CmSearchParameterBuilderTests {

    @Test
    @DisplayName("Extracts version from url during build")
    void extractsVersionFromUrlDuringBuild() {
      // GIVEN
      UriParam urlParam = TestDataHelper.uriWithVersion();

      // WHEN
      CmSearchParameter parameter = CmSearchParameter.builder().urlParam(urlParam).build();

      // THEN
      assertThat(parameter.getUrl())
          .get()
          .extracting(UriParam::getValue)
          .isEqualTo(TestDataHelper.CANONICAL_URL);
      assertThat(parameter.getVersion())
          .get()
          .extracting(TokenParam::getValue)
          .isEqualTo(TestDataHelper.VERSION);
    }
  }

  private static final class TestDataHelper {

    private static final String CANONICAL_URL = "http://example.org/fhir/CodeSystem/example";
    private static final String VERSION = "2026.1";
    private static final String EXPLICIT_VERSION = "explicit-version";
    private static final String CANONICAL_WITH_VERSION = CANONICAL_URL + "|" + VERSION;

    private TestDataHelper() {
      // utility class
    }

    private static UriParam uriWithVersion() {
      return new UriParam(CANONICAL_WITH_VERSION);
    }
  }
}
