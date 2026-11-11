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
package de.gematik.zts.termzilla.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ResourceOrigin;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for all cache key classes.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Equals/HashCode contract
 *   <li>Serialization compatibility
 *   <li>Key uniqueness for different parameters
 *   <li>Key consistency for identical parameters
 * </ul>
 */
@DisplayName("Cache Key Tests")
class CacheKeyTest {

  private static final String SCOPE_1 = "scope1";
  private static final String SCOPE_2 = "scope2";

  @Nested
  @DisplayName("ExpansionCacheKey Tests")
  class ExpansionCacheKeyTests {

    @Test
    @DisplayName("Should create equal keys for identical parameters")
    void shouldCreateEqualKeysForIdenticalParameters() {
      // Given
      VsExpandParameter params1 =
          VsExpandParameter.builder()
              .urlParam(new UriType("http://test.com/vs"))
              .valueSetVersionParam(new StringType("1.0"))
              .build();

      VsExpandParameter params2 =
          VsExpandParameter.builder()
              .urlParam(new UriType("http://test.com/vs"))
              .valueSetVersionParam(new StringType("1.0"))
              .build();

      // When
      ExpansionCacheKey key1 = new ExpansionCacheKey(SCOPE_1, params1);
      ExpansionCacheKey key2 = new ExpansionCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2);
    }

    @Test
    @DisplayName("Should create different keys for different scopes")
    void shouldCreateDifferentKeysForDifferentScopes() {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      // When
      ExpansionCacheKey key1 = new ExpansionCacheKey(SCOPE_1, params);
      ExpansionCacheKey key2 = new ExpansionCacheKey(SCOPE_2, params);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should create different keys for different URLs")
    void shouldCreateDifferentKeysForDifferentUrls() {
      // Given
      VsExpandParameter params1 =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs1")).build();

      VsExpandParameter params2 =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs2")).build();

      // When
      ExpansionCacheKey key1 = new ExpansionCacheKey(SCOPE_1, params1);
      ExpansionCacheKey key2 = new ExpansionCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      // Given
      VsExpandParameter params =
          VsExpandParameter.builder()
              .urlParam(new UriType("http://test.com/vs"))
              .valueSetVersionParam(new StringType("1.0"))
              .build();
      ExpansionCacheKey original = new ExpansionCacheKey(SCOPE_1, params);

      // When
      ExpansionCacheKey deserialized = serializeAndDeserialize(original);

      // Then
      assertThat(deserialized).isEqualTo(original).hasSameHashCodeAs(original);
    }

    @Test
    @DisplayName("Should create different keys for provided ValueSet vs URL")
    void shouldCreateDifferentKeysForValueSetVsUrl() {
      // Given
      ValueSet vs = new ValueSet();
      vs.setUrl("http://test.com/vs");

      VsExpandParameter paramsWithUrl =
          VsExpandParameter.builder().urlParam(new UriType("http://test.com/vs")).build();

      VsExpandParameter paramsWithValueSet = VsExpandParameter.builder().valueSetParam(vs).build();

      // When
      ExpansionCacheKey key1 = new ExpansionCacheKey(SCOPE_1, paramsWithUrl);
      ExpansionCacheKey key2 = new ExpansionCacheKey(SCOPE_1, paramsWithValueSet);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should create different keys for stored and provided ValueSets")
    void shouldCreateDifferentKeysForDifferentResourceOrigins() {
      // Given
      ValueSet vs = new ValueSet();
      vs.setUrl("http://test.com/vs");

      VsExpandParameter providedParams = VsExpandParameter.builder().valueSetParam(vs).build();
      VsExpandParameter storedParams =
          VsExpandParameter.builder()
              .valueSetParam(vs)
              .resourceOrigin(ResourceOrigin.STORED)
              .build();

      // When
      ExpansionCacheKey providedKey = new ExpansionCacheKey(SCOPE_1, providedParams);
      ExpansionCacheKey storedKey = new ExpansionCacheKey(SCOPE_1, storedParams);

      // Then
      assertThat(providedKey).isNotEqualTo(storedKey);
    }
  }

  @Nested
  @DisplayName("ValidationCacheKey Tests")
  class ValidationCacheKeyTests {

    @Test
    @DisplayName("Should create equal keys for identical parameters")
    void shouldCreateEqualKeysForIdenticalParameters() {
      // Given
      VsValidateCodeParameter params1 =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .systemParam(new UriParam("http://test.com/cs"))
              .build();

      VsValidateCodeParameter params2 =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .systemParam(new UriParam("http://test.com/cs"))
              .build();

      // When
      ValidationCacheKey key1 = new ValidationCacheKey(SCOPE_1, params1);
      ValidationCacheKey key2 = new ValidationCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2);
    }

    @Test
    @DisplayName("Should create different keys for different codes")
    void shouldCreateDifferentKeysForDifferentCodes() {
      // Given
      VsValidateCodeParameter params1 =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .build();

      VsValidateCodeParameter params2 =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("67890"))
              .build();

      // When
      ValidationCacheKey key1 = new ValidationCacheKey(SCOPE_1, params1);
      ValidationCacheKey key2 = new ValidationCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      // Given
      VsValidateCodeParameter params =
          VsValidateCodeParameter.builder()
              .urlParam(new UriParam("http://test.com/vs"))
              .codeParam(new TokenParam("12345"))
              .build();
      ValidationCacheKey original = new ValidationCacheKey(SCOPE_1, params);

      // When
      ValidationCacheKey deserialized = serializeAndDeserialize(original);

      // Then
      assertThat(deserialized).isEqualTo(original);
    }
  }

  @Nested
  @DisplayName("LookupCacheKey Tests")
  class LookupCacheKeyTests {

    @Test
    @DisplayName("Should create equal keys for identical parameters")
    void shouldCreateEqualKeysForIdenticalParameters() {
      // Given
      CsLookupParameter params1 =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();

      CsLookupParameter params2 =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();

      // When
      LookupCacheKey key1 = new LookupCacheKey(SCOPE_1, params1);
      LookupCacheKey key2 = new LookupCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2);
    }

    @Test
    @DisplayName("Should create different keys for different systems")
    void shouldCreateDifferentKeysForDifferentSystems() {
      // Given
      CsLookupParameter params1 =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();

      CsLookupParameter params2 =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://loinc.org"))
              .build();

      // When
      LookupCacheKey key1 = new LookupCacheKey(SCOPE_1, params1);
      LookupCacheKey key2 = new LookupCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      // Given
      CsLookupParameter params =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .build();
      LookupCacheKey original = new LookupCacheKey(SCOPE_1, params);

      // When
      LookupCacheKey deserialized = serializeAndDeserialize(original);

      // Then
      assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("Should create different keys for different Accept-Language headers")
    void shouldCreateDifferentKeysForDifferentAcceptLanguageHeaders() {
      // GIVEN
      CsLookupParameter paramsDeDE =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .languageAcceptLanguageHeader(LanguageParameter.parse("de-DE"))
              .build();

      CsLookupParameter paramsEnUS =
          CsLookupParameter.builder()
              .codeParam(new CodeType("12345"))
              .systemParam(new UriType("http://snomed.info/sct"))
              .languageAcceptLanguageHeader(LanguageParameter.parse("en-US"))
              .build();

      // WHEN
      LookupCacheKey key1 = new LookupCacheKey(SCOPE_1, paramsDeDE);
      LookupCacheKey key2 = new LookupCacheKey(SCOPE_1, paramsEnUS);

      // THEN
      assertThat(key1).isNotEqualTo(key2);
    }
  }

  @Nested
  @DisplayName("CsValidationCacheKey Tests")
  class CsValidationCacheKeyTests {

    @Test
    @DisplayName("Should create equal keys for identical parameters")
    void shouldCreateEqualKeysForIdenticalParameters() {
      // Given
      CsValidateCodeParameter params1 =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType("http://snomed.info/sct"))
              .codeParam(new CodeType("12345"))
              .build();

      CsValidateCodeParameter params2 =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType("http://snomed.info/sct"))
              .codeParam(new CodeType("12345"))
              .build();

      // When
      CsValidationCacheKey key1 = new CsValidationCacheKey(SCOPE_1, params1);
      CsValidationCacheKey key2 = new CsValidationCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2);
    }

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      // Given
      CsValidateCodeParameter params =
          CsValidateCodeParameter.builder()
              .urlParam(new UriType("http://snomed.info/sct"))
              .codeParam(new CodeType("12345"))
              .build();
      CsValidationCacheKey original = new CsValidationCacheKey(SCOPE_1, params);

      // When
      CsValidationCacheKey deserialized = serializeAndDeserialize(original);

      // Then
      assertThat(deserialized).isEqualTo(original);
    }
  }

  @Nested
  @DisplayName("SearchCacheKey Tests")
  class SearchCacheKeyTests {

    @Test
    @DisplayName("Should create equal keys for identical parameters")
    void shouldCreateEqualKeysForIdenticalParameters() {
      // Given
      VsSearchParameter params1 = VsSearchParameter.builder().build();
      params1.setUrlParam(new UriParam("http://test.com/vs"));
      params1.setNameParam(new StringParam("test"));

      VsSearchParameter params2 = VsSearchParameter.builder().build();
      params2.setUrlParam(new UriParam("http://test.com/vs"));
      params2.setNameParam(new StringParam("test"));

      // When
      SearchCacheKey key1 = new SearchCacheKey(SCOPE_1, params1);
      SearchCacheKey key2 = new SearchCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2).hasSameHashCodeAs(key2);
    }

    @Test
    @DisplayName("Should create different keys for different search parameters")
    void shouldCreateDifferentKeysForDifferentSearchParameters() {
      // Given
      VsSearchParameter params1 = VsSearchParameter.builder().build();
      params1.setUrlParam(new UriParam("http://test.com/vs1"));

      VsSearchParameter params2 = VsSearchParameter.builder().build();
      params2.setUrlParam(new UriParam("http://test.com/vs2"));

      // When
      SearchCacheKey key1 = new SearchCacheKey(SCOPE_1, params1);
      SearchCacheKey key2 = new SearchCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should work with CodeSystem search parameters")
    void shouldWorkWithCodeSystemSearchParameters() {
      // Given
      CsSearchParameter params1 = CsSearchParameter.builder().build();
      params1.setSystemParam(new UriParam("http://snomed.info/sct"));

      CsSearchParameter params2 = CsSearchParameter.builder().build();
      params2.setSystemParam(new UriParam("http://snomed.info/sct"));

      // When
      SearchCacheKey key1 = new SearchCacheKey(SCOPE_1, params1);
      SearchCacheKey key2 = new SearchCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName(
        "Should create different keys for identical CodeSystem and ValueSet search parameters")
    void shouldCreateDifferentKeysForIdenticalCodeSystemAndValueSetSearchParameters() {
      // Given
      CsSearchParameter params1 = CsSearchParameter.builder().build();
      params1.setUrlParam(new UriParam("http://snomed.info/sct"));

      VsSearchParameter params2 = VsSearchParameter.builder().build();
      params2.setUrlParam(new UriParam("http://snomed.info/sct"));

      // When
      SearchCacheKey key1 = new SearchCacheKey(SCOPE_1, params1);
      SearchCacheKey key2 = new SearchCacheKey(SCOPE_1, params2);

      // Then
      assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      // Given
      VsSearchParameter params = VsSearchParameter.builder().build();
      params.setUrlParam(new UriParam("http://test.com/vs"));
      SearchCacheKey original = new SearchCacheKey(SCOPE_1, params);

      // When
      SearchCacheKey deserialized = serializeAndDeserialize(original);

      // Then
      assertThat(deserialized).isEqualTo(original);
    }
  }

  // Helper method for serialization testing
  @SuppressWarnings("unchecked")
  private <T extends Serializable> T serializeAndDeserialize(T object) throws Exception {
    // Serialize
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
    }

    // Deserialize
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    try (ObjectInputStream ois = new ObjectInputStream(bais)) {
      return (T) ois.readObject();
    }
  }
}
