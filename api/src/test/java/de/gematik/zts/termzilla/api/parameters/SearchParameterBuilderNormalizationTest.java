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
import static org.assertj.core.api.Assertions.catchThrowable;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.exceptions.InvalidSearchParameterException;
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
    @DisplayName("Rejects duplicate explicit and embedded versions during build")
    void rejectsDuplicateVersionDuringBuild() {
      // GIVEN
      var builder =
          TerminologyResourceSearchParameter.builder()
              .urlParam(TestDataHelper.uriWithVersion())
              .versionParam(new TokenParam(TestDataHelper.EXPLICIT_VERSION));

      // WHEN
      Throwable thrown = catchThrowable(builder::build);

      // THEN
      assertThat(thrown)
          .isInstanceOf(InvalidSearchParameterException.class)
          .hasMessageContaining("Version must not be set in 'url' parameter");
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

  @Test
  void rejectsSystemAndUrlDuringCodeSystemSearchBuild() {
    // GIVEN
    var builder =
        CsSearchParameter.builder()
            .systemParam(new UriParam("http://example.org/system"))
            .urlParam(new UriParam("http://example.org/url"));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown)
        .isInstanceOf(InvalidSearchParameterException.class)
        .hasMessageContaining("Either use");
  }

  @Test
  void rejectsDuplicateSystemVersionDuringCodeSystemSearchBuild() {
    // GIVEN
    var builder =
        CsSearchParameter.builder()
            .systemParam(TestDataHelper.uriWithVersion())
            .versionParam(new TokenParam(TestDataHelper.EXPLICIT_VERSION));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown)
        .isInstanceOf(InvalidSearchParameterException.class)
        .hasMessageContaining("Version must not be set in 'system' parameter");
  }

  @Test
  void rejectsDuplicateVersionDuringValueSetSearchBuild() {
    // GIVEN
    var builder =
        VsSearchParameter.builder()
            .urlParam(TestDataHelper.uriWithVersion())
            .versionParam(new TokenParam(TestDataHelper.EXPLICIT_VERSION));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown).isInstanceOf(InvalidSearchParameterException.class);
  }

  @Test
  void rejectsConflictingConceptMapSourceDuringBuild() {
    // GIVEN
    var builder =
        CmSearchParameter.builder()
            .sourceCanonicalParam(new ReferenceParam("ValueSet/source"))
            .sourceUriParam(new ReferenceParam("http://example.org/source"));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown)
        .isInstanceOf(InvalidSearchParameterException.class)
        .hasMessageContaining(CmSearchParameter.SP_SOURCE_CANONICAL);
  }

  @Test
  void contentSearchIsNotAnApiStructuralRestriction() {
    // GIVEN
    var builder =
        CsSearchParameter.builder()
            .contentParam(new StringParam("not-present"))
            .codeParam(new TokenParam("12345"));

    // WHEN
    CsSearchParameter parameter = builder.build();

    // THEN
    assertThat(parameter.getContent()).isPresent();
    assertThat(parameter.getCode()).isPresent();
  }

  @Test
  void reusedBuilderKeepsVersionAndDoesNotMutateInput() {
    // GIVEN
    UriParam system = TestDataHelper.uriWithVersion();
    var builder = CsSearchParameter.builder().systemParam(system);

    // WHEN
    CsSearchParameter first = builder.build();
    CsSearchParameter second = builder.build();

    // THEN
    assertThat(system.getValue()).isEqualTo(TestDataHelper.CANONICAL_WITH_VERSION);
    for (CsSearchParameter parameter : java.util.List.of(first, second)) {
      assertThat(parameter.getSystem().orElseThrow().getValue())
          .isEqualTo(TestDataHelper.CANONICAL_URL);
      assertThat(parameter.getVersion().orElseThrow().getValue())
          .isEqualTo(TestDataHelper.VERSION);
    }
  }

  @Test
  void reusedUrlBuilderKeepsVersionAndDoesNotMutateInput() {
    // GIVEN
    UriParam url = TestDataHelper.uriWithVersion();
    var builder = VsSearchParameter.builder().urlParam(url);

    // WHEN
    var first = builder.build();
    var second = builder.build();

    // THEN
    assertThat(url.getValue()).isEqualTo(TestDataHelper.CANONICAL_WITH_VERSION);
    assertThat(first.getVersion().orElseThrow().getValue()).isEqualTo(TestDataHelper.VERSION);
    assertThat(second.getVersion().orElseThrow().getValue()).isEqualTo(TestDataHelper.VERSION);
  }

  @Test
  void preservesMissingModifierWithoutValue() {
    // GIVEN
    UriParam url = new UriParam();
    url.setMissing(true);

    // WHEN
    var parameter = VsSearchParameter.builder().urlParam(url).build();

    // THEN
    assertThat(parameter.getUrl().orElseThrow().getMissing()).isTrue();
    assertThat(parameter.getVersion()).isEmpty();
  }

  @Test
  void rejectsConflictingConceptMapTargetDuringBuild() {
    // GIVEN
    var builder =
        CmSearchParameter.builder()
            .targetCanonicalParam(new ReferenceParam("ValueSet/target"))
            .targetUriParam(new ReferenceParam("http://example.org/target"));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown)
        .isInstanceOf(InvalidSearchParameterException.class)
        .hasMessageContaining(CmSearchParameter.SP_TARGET_CANONICAL);
  }

  @Test
  void rejectsDuplicateVersionDuringConceptMapBuild() {
    // GIVEN
    var builder =
        CmSearchParameter.builder()
            .urlParam(TestDataHelper.uriWithVersion())
            .versionParam(new TokenParam(TestDataHelper.EXPLICIT_VERSION));

    // WHEN
    Throwable thrown = catchThrowable(builder::build);

    // THEN
    assertThat(thrown).isInstanceOf(InvalidSearchParameterException.class);
  }

  @Test
  void validatesParametersChangedAfterBuild() {
    // GIVEN
    CsSearchParameter parameter =
        CsSearchParameter.builder().urlParam(new UriParam("http://example.org/url")).build();
    parameter.setSystemParam(new UriParam("http://example.org/system"));

    // WHEN
    Throwable thrown = catchThrowable(parameter::validate);

    // THEN
    assertThat(thrown).isInstanceOf(InvalidSearchParameterException.class);
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
