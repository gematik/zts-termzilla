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
package de.gematik.zts.termzilla.terminologies.loinc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.lookup.LoincCsLookupOperation;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.search.LoincCsSearchOperation;
import de.gematik.zts.termzilla.terminologies.loinc.provider.cs.validatecode.LoincCsValidateCodeOperation;
import java.lang.reflect.Field;
import java.util.List;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link LoincCodeSystemService}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoincCodeSystemService tests")
class LoincCodeSystemServiceTest {

  private static final String TEST_SCOPE = "test-scope";

  @Mock private LoincTerminologyManager loincTerminologyManager;
  @Mock private Loinc loinc;
  @Mock private LoincConfigProperties loincConfigProperties;

  private LoincCodeSystemService createService(ProvisionMode provisionMode) {
    LoincVersion version281 = LoincTestDataHelper.loincVersion(281, "2.81");
    LoincVersion version282 = LoincTestDataHelper.loincVersion(282, "2.82");

    // Constructor always reads manager + properties + supported versions.
    when(loincTerminologyManager.getLoinc()).thenReturn(loinc);
    when(loincTerminologyManager.getProperties()).thenReturn(loincConfigProperties);
    when(loincConfigProperties.getProvisionMode()).thenReturn(provisionMode);
    when(loinc.getSupportedLoincVersions()).thenReturn(List.of(version281, version282));

    // Only a subset of tests exercises default-version resolution.
    lenient().when(loinc.getDefaultLoincVersion()).thenReturn(version282);

    return new LoincCodeSystemService(loincTerminologyManager);
  }

  private static void setPrivateField(Object target, String fieldName, Object fieldValue) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, fieldValue);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Unable to set field '" + fieldName + "'", exception);
    }
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTest {

    @Test
    @DisplayName("should initialize lists, maps, and version mapping")
    void shouldInitializeListsMapsAndVersionMapping() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN
      List<CodeSystem> codeSystemList = service.getCodeSystemList();

      // THEN
      assertThat(codeSystemList).hasSize(2);
      assertThat(service.getVersionToCodeSystemMap()).hasSize(2);
      assertThat(service.getIdToCodeSystemMap()).hasSize(2);
      assertThat(service.getFhirVersionToLoincVersionMap().keySet())
          .containsExactlyInAnyOrder("2.81", "2.82");
      assertThat(service.getSupportedCodeSystemCanonicalsList())
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME)
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME + "|2.81")
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME + "|2.82");
    }

    @Test
    @DisplayName("should default provision mode to SUPPORTED when configuration value is null")
    void shouldDefaultProvisionModeToSupportedWhenProvisionModeIsNull() {
      // GIVEN
      LoincCodeSystemService service = createService(null);

      // WHEN
      List<CodeSystem> codeSystemList = service.getCodeSystemList();

      // THEN
      assertThat(codeSystemList)
          .extracting(
              codeSystem -> codeSystem.getUserData(ResourceUserData.USERDATA_PROVISION_MODE))
          .containsOnly(ProvisionMode.SUPPORTED);
    }
  }

  @Nested
  @DisplayName("getSupportedCodeSystemCanonicalsList")
  class GetSupportedCodeSystemCanonicalsListTest {

    @Test
    @DisplayName("should return canonical base and versioned canonical entries")
    void shouldReturnCanonicalBaseAndVersionedEntries() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN
      List<String> canonicalList = service.getSupportedCodeSystemCanonicalsList();

      // THEN
      assertThat(canonicalList)
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME)
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME + "|2.81")
          .contains(LoincConstants.TERMINOLOGY_CANONICAL_NAME + "|2.82");
    }
  }

  @Nested
  @DisplayName("getCodeSystemList")
  class GetCodeSystemListTest {

    @Test
    @DisplayName("should return all generated CodeSystems")
    void shouldReturnAllGeneratedCodeSystems() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN
      List<CodeSystem> codeSystemList = service.getCodeSystemList();

      // THEN
      assertThat(codeSystemList)
          .hasSize(2)
          .extracting(CodeSystem::getUrl)
          .containsOnly(LoincConstants.TERMINOLOGY_CANONICAL_NAME);
    }
  }

  @Nested
  @DisplayName("getDefaultCodeSystemByUrlAndScope")
  class GetDefaultCodeSystemByUrlAndScopeTest {

    @Test
    @DisplayName("should return default version CodeSystem for matching canonical URL")
    void shouldReturnDefaultVersionCodeSystemForMatchingCanonicalUrl()
        throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN
      CodeSystem codeSystem =
          service.getDefaultCodeSystemByUrlAndScope(
              TEST_SCOPE, LoincConstants.TERMINOLOGY_CANONICAL_NAME, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(codeSystem.getVersion()).isEqualTo("2.82");
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when canonical URL is unknown")
    void shouldThrowResourceNotFoundWhenCanonicalUrlIsUnknown() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      String unknownCanonical = "http://loinc.org/unknown";

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  service.getDefaultCodeSystemByUrlAndScope(
                      TEST_SCOPE, unknownCanonical, ResourceRetrievalMode.FULL))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining(unknownCanonical)
          .hasMessageContaining(TEST_SCOPE);
    }

    @Test
    @DisplayName("should delegate overload without retrieval mode")
    void shouldDelegateOverloadWithoutRetrievalMode() throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN
      CodeSystem codeSystem =
          service.getDefaultCodeSystemByUrlAndScope(
              TEST_SCOPE, LoincConstants.TERMINOLOGY_CANONICAL_NAME);

      // THEN
      assertThat(codeSystem.getVersion()).isEqualTo("2.82");
    }
  }

  @Nested
  @DisplayName("getCodeSystem")
  class GetCodeSystemTest {

    @Test
    @DisplayName("should return CodeSystem by id with retrieval mode")
    void shouldReturnCodeSystemByIdWithRetrievalMode() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      CodeSystem referenceCodeSystem = service.getCodeSystemList().getFirst();
      IdType idType = new IdType(referenceCodeSystem.getId());

      // WHEN
      CodeSystem result = service.getCodeSystem(TEST_SCOPE, idType, ResourceRetrievalMode.FULL);

      // THEN
      assertThat(result).isEqualTo(referenceCodeSystem);
    }

    @Test
    @DisplayName("should delegate overload without retrieval mode")
    void shouldDelegateOverloadWithoutRetrievalMode() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      CodeSystem referenceCodeSystem = service.getCodeSystemList().getFirst();
      IdType idType = new IdType(referenceCodeSystem.getId());

      // WHEN
      CodeSystem result = service.getCodeSystem(TEST_SCOPE, idType);

      // THEN
      assertThat(result).isEqualTo(referenceCodeSystem);
    }
  }

  @Nested
  @DisplayName("searchCodeSystem")
  class SearchCodeSystemTest {

    @Test
    @DisplayName("should return empty list for non-capability search in SUPPORTED mode")
    void shouldReturnEmptyListForNonCapabilitySearchInSupportedMode()
        throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.SUPPORTED);
      CsSearchParameter searchParameter = CsSearchParameter.builder().build();

      // WHEN
      List<IdType> result = service.searchCodeSystem(TEST_SCOPE, searchParameter, false);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should delegate capability search even in SUPPORTED mode")
    void shouldDelegateCapabilitySearchInSupportedMode() throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.SUPPORTED);
      LoincCsSearchOperation searchOperationMock =
          org.mockito.Mockito.mock(LoincCsSearchOperation.class);
      List<IdType> expectedResult = List.of(new IdType("cs-capability-id"));
      CsSearchParameter searchParameter = CsSearchParameter.builder().build();
      when(searchOperationMock.searchCodeSystems(any(CsSearchParameter.class)))
          .thenReturn(expectedResult);
      setPrivateField(service, "loincCsSearchOperation", searchOperationMock);

      // WHEN
      List<IdType> result = service.searchCodeSystem(TEST_SCOPE, searchParameter, true);

      // THEN
      assertThat(result).isEqualTo(expectedResult);
      verify(searchOperationMock).searchCodeSystems(searchParameter);
    }

    @Test
    @DisplayName("should delegate non-capability search in PREDEFINED mode")
    void shouldDelegateNonCapabilitySearchInPredefinedMode() throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      LoincCsSearchOperation searchOperationMock =
          org.mockito.Mockito.mock(LoincCsSearchOperation.class);
      List<IdType> expectedResult = List.of(new IdType("cs-predefined-id"));
      CsSearchParameter searchParameter = CsSearchParameter.builder().build();
      when(searchOperationMock.searchCodeSystems(any(CsSearchParameter.class)))
          .thenReturn(expectedResult);
      setPrivateField(service, "loincCsSearchOperation", searchOperationMock);

      // WHEN
      List<IdType> result = service.searchCodeSystem(TEST_SCOPE, searchParameter, false);

      // THEN
      assertThat(result).isEqualTo(expectedResult);
      verify(searchOperationMock).searchCodeSystems(searchParameter);
    }

    @Test
    @DisplayName("should not call search operation for SUPPORTED non-capability short-circuit")
    void shouldNotCallSearchOperationForSupportedNonCapabilityShortCircuit()
        throws NotImplementedException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.SUPPORTED);
      LoincCsSearchOperation searchOperationMock =
          org.mockito.Mockito.mock(LoincCsSearchOperation.class);
      CsSearchParameter searchParameter = CsSearchParameter.builder().build();
      setPrivateField(service, "loincCsSearchOperation", searchOperationMock);

      // WHEN
      List<IdType> result = service.searchCodeSystem(TEST_SCOPE, searchParameter, false);

      // THEN
      assertThat(result).isEmpty();
      verifyNoInteractions(searchOperationMock);
    }
  }

  @Nested
  @DisplayName("searchCodeSystemsByCode")
  class SearchCodeSystemsByCodeTest {

    @Test
    @DisplayName("should throw NotImplementedException")
    void shouldThrowNotImplementedException() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN / THEN
      assertThatThrownBy(() -> service.searchCodeSystemsByCode(TEST_SCOPE, null, List.of()))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("not yet implemented");
    }
  }

  @Nested
  @DisplayName("searchCodeSystemsByContent")
  class SearchCodeSystemsByContentTest {

    @Test
    @DisplayName("should throw NotImplementedException")
    void shouldThrowNotImplementedException() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);

      // WHEN / THEN
      assertThatThrownBy(() -> service.searchCodeSystemsByContent(TEST_SCOPE, null, List.of()))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("not yet implemented");
    }
  }

  @Nested
  @DisplayName("validateCode")
  class ValidateCodeTest {

    @Test
    @DisplayName("should delegate to LoincCsValidateCodeOperation")
    void shouldDelegateToValidateCodeOperation()
        throws NotImplementedException, UnsupportedTerminologyException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      LoincCsValidateCodeOperation validateCodeOperationMock =
          org.mockito.Mockito.mock(LoincCsValidateCodeOperation.class);
      CsValidateCodeParameter parameter = CsValidateCodeParameter.builder().build();
      LanguageParameter languageSystemDefault = LanguageParameter.parse("de-DE");
      ValidateCodeResult expectedResult = new ValidateCodeResult();
      expectedResult.setResult(true);

      when(validateCodeOperationMock.validateCode(parameter, languageSystemDefault))
          .thenReturn(expectedResult);
      setPrivateField(service, "loincCsValidateCodeOperation", validateCodeOperationMock);

      // WHEN
      ValidateCodeResult result =
          service.validateCode(TEST_SCOPE, parameter, languageSystemDefault);

      // THEN
      assertThat(result).isEqualTo(expectedResult);
      verify(validateCodeOperationMock).validateCode(parameter, languageSystemDefault);
    }
  }

  @Nested
  @DisplayName("lookupCode")
  class LookupCodeTest {

    @Test
    @DisplayName("should delegate to LoincCsLookupOperation")
    void shouldDelegateToLookupOperation()
        throws NotImplementedException, UnsupportedTerminologyException {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      LoincCsLookupOperation lookupOperationMock =
          org.mockito.Mockito.mock(LoincCsLookupOperation.class);
      CsLookupParameter parameter = CsLookupParameter.builder().build();
      CsLookupResult expectedResult = new CsLookupResult();
      expectedResult.setFound(true);

      when(lookupOperationMock.lookupCode(parameter)).thenReturn(expectedResult);
      setPrivateField(service, "loincCsLookupOperation", lookupOperationMock);

      // WHEN
      CsLookupResult result = service.lookupCode(TEST_SCOPE, parameter);

      // THEN
      assertThat(result).isEqualTo(expectedResult);
      verify(lookupOperationMock).lookupCode(parameter);
    }
  }

  @Nested
  @DisplayName("findMatches")
  class FindMatchesTest {

    @Test
    @DisplayName("should throw NotImplementedException")
    void shouldThrowNotImplementedException() {
      // GIVEN
      LoincCodeSystemService service = createService(ProvisionMode.PREDEFINED);
      CsFindMatchesParameter parameter = CsFindMatchesParameter.builder().build();

      // WHEN / THEN
      assertThatThrownBy(() -> service.findMatches(TEST_SCOPE, parameter))
          .isInstanceOf(NotImplementedException.class)
          .hasMessageContaining("Not yet implemented");
    }
  }
}
