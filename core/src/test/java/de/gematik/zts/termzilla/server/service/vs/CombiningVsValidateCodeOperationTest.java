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
package de.gematik.zts.termzilla.server.service.vs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.parameters.CsValidateCodeParameter;
import de.gematik.zts.termzilla.api.parameters.ValidateCodeResult;
import de.gematik.zts.termzilla.api.parameters.VsValidateCodeParameter;
import de.gematik.zts.termzilla.server.cache.TerminologyCacheManager;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link CombiningVsValidateCodeOperation}. */
@DisplayName("CombiningVsValidateCodeOperation")
class CombiningVsValidateCodeOperationTest {

  private static final String SCOPE = "fhir";
  private static final String VALUE_SET_URL = "http://example.org/ValueSet/test";
  private static final String CODE_SYSTEM_URL = "http://example.org/CodeSystem/test";
  private static final String CODE = "12345";
  private static final String DISPLAY = "Wrong display";
  private static final String DISPLAY_LANGUAGE = "de";

  @Nested
  @DisplayName("validateCode")
  class ValidateCodeTests {

    @Test
    @DisplayName(
        "returns strict failure when display is invalid and lenient-display-validation is absent")
    void returnsStrictFailureWhenDisplayIsInvalidAndLenientDisplayValidationIsAbsent()
        throws Exception {
      // GIVEN
      TerminologyCacheManager cacheManager = new TerminologyCacheManager();
      CombiningTerminologyService terminologyService = mock(CombiningTerminologyService.class);
      when(terminologyService.getCacheManager()).thenReturn(cacheManager);
      when(terminologyService.expandValueSet(eq(SCOPE), any()))
          .thenReturn(VsValidateCodeTestDataHelper.createExpansionValueSet());
      when(terminologyService.getDefaultCodeSystemByScopeAndUrl(SCOPE, CODE_SYSTEM_URL))
          .thenReturn(VsValidateCodeTestDataHelper.createCodeSystem());

      ValidateCodeResult strictFailure = new ValidateCodeResult();
      strictFailure.setResult(false);
      strictFailure.setMessage("Display does not match the concept display.");
      when(terminologyService.validateCode(eq(SCOPE), any(CsValidateCodeParameter.class)))
          .thenReturn(strictFailure);

      VsValidateCodeParameter request =
          VsValidateCodeTestDataHelper.createVsValidateCodeParameterWithDisplay(false);

      // WHEN
      ValidateCodeResult result =
          CombiningVsValidateCodeOperation.validateCode(terminologyService, request, SCOPE);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).contains("Display does not match");
      verify(terminologyService).validateCode(eq(SCOPE), any(CsValidateCodeParameter.class));
    }

    @Test
    @DisplayName(
        "returns success with warning when display is invalid and lenient-display-validation is true")
    void returnsSuccessWithWarningWhenDisplayIsInvalidAndLenientDisplayValidationIsTrue()
        throws Exception {
      // GIVEN
      TerminologyCacheManager cacheManager = new TerminologyCacheManager();
      CombiningTerminologyService terminologyService = mock(CombiningTerminologyService.class);
      when(terminologyService.getCacheManager()).thenReturn(cacheManager);
      when(terminologyService.expandValueSet(eq(SCOPE), any()))
          .thenReturn(VsValidateCodeTestDataHelper.createExpansionValueSet());
      when(terminologyService.getDefaultCodeSystemByScopeAndUrl(SCOPE, CODE_SYSTEM_URL))
          .thenReturn(VsValidateCodeTestDataHelper.createCodeSystem());

      ValidateCodeResult strictFailure = new ValidateCodeResult();
      strictFailure.setResult(false);
      strictFailure.setMessage("Display does not match the concept display.");

      ValidateCodeResult fallbackSuccess = new ValidateCodeResult();
      fallbackSuccess.setResult(true);
      fallbackSuccess.setCode(CODE);
      fallbackSuccess.setSystem(CODE_SYSTEM_URL);

      when(terminologyService.validateCode(eq(SCOPE), any(CsValidateCodeParameter.class)))
          .thenReturn(strictFailure, fallbackSuccess);

      VsValidateCodeParameter request =
          VsValidateCodeTestDataHelper.createVsValidateCodeParameterWithDisplay(true);

      // WHEN
      ValidateCodeResult result =
          CombiningVsValidateCodeOperation.validateCode(terminologyService, request, SCOPE);

      // THEN
      assertThat(result.isResult()).isTrue();
      assertThat(result.getMessage()).contains("lenient-display-validation");

      ArgumentCaptor<CsValidateCodeParameter> parameterCaptor =
          ArgumentCaptor.forClass(CsValidateCodeParameter.class);
      verify(terminologyService, times(2)).validateCode(eq(SCOPE), parameterCaptor.capture());

      CsValidateCodeParameter firstInvocation = parameterCaptor.getAllValues().get(0);
      CsValidateCodeParameter secondInvocation = parameterCaptor.getAllValues().get(1);
      assertThat(firstInvocation.getDisplayParam()).isNotNull();
      assertThat(firstInvocation.getDisplayLanguageParam()).isNotNull();
      assertThat(secondInvocation.getDisplayParam()).isNull();
      assertThat(secondInvocation.getDisplayLanguageParam()).isNull();
    }

    @Test
    @DisplayName(
        "keeps failure when lenient-display-validation is true but code validation also fails without display")
    void keepsFailureWhenLenientDisplayValidationIsTrueButCodeValidationAlsoFailsWithoutDisplay()
        throws Exception {
      // GIVEN
      TerminologyCacheManager cacheManager = new TerminologyCacheManager();
      CombiningTerminologyService terminologyService = mock(CombiningTerminologyService.class);
      when(terminologyService.getCacheManager()).thenReturn(cacheManager);
      when(terminologyService.expandValueSet(eq(SCOPE), any()))
          .thenReturn(VsValidateCodeTestDataHelper.createExpansionValueSet());
      when(terminologyService.getDefaultCodeSystemByScopeAndUrl(SCOPE, CODE_SYSTEM_URL))
          .thenReturn(VsValidateCodeTestDataHelper.createCodeSystem());

      ValidateCodeResult strictFailure = new ValidateCodeResult();
      strictFailure.setResult(false);
      strictFailure.setMessage("Display does not match the concept display.");

      ValidateCodeResult fallbackFailure = new ValidateCodeResult();
      fallbackFailure.setResult(false);
      fallbackFailure.setMessage("The code does not exist in the CodeSystem.");

      when(terminologyService.validateCode(eq(SCOPE), any(CsValidateCodeParameter.class)))
          .thenReturn(strictFailure, fallbackFailure);

      VsValidateCodeParameter request =
          VsValidateCodeTestDataHelper.createVsValidateCodeParameterWithDisplay(true);

      // WHEN
      ValidateCodeResult result =
          CombiningVsValidateCodeOperation.validateCode(terminologyService, request, SCOPE);

      // THEN
      assertThat(result.isResult()).isFalse();
      assertThat(result.getMessage()).isEqualTo("The code does not exist in the CodeSystem.");
    }

    @Test
    @DisplayName("does not run lenient recovery when no display value is provided")
    void doesNotRunLenientRecoveryWhenNoDisplayValueIsProvided() throws Exception {
      // GIVEN
      TerminologyCacheManager cacheManager = new TerminologyCacheManager();
      CombiningTerminologyService terminologyService = mock(CombiningTerminologyService.class);
      when(terminologyService.getCacheManager()).thenReturn(cacheManager);
      when(terminologyService.expandValueSet(eq(SCOPE), any()))
          .thenReturn(VsValidateCodeTestDataHelper.createExpansionValueSet());
      when(terminologyService.getDefaultCodeSystemByScopeAndUrl(SCOPE, CODE_SYSTEM_URL))
          .thenReturn(VsValidateCodeTestDataHelper.createCodeSystem());

      ValidateCodeResult validationResult = new ValidateCodeResult();
      validationResult.setResult(true);
      when(terminologyService.validateCode(eq(SCOPE), any(CsValidateCodeParameter.class)))
          .thenReturn(validationResult);

      VsValidateCodeParameter request =
          VsValidateCodeTestDataHelper.createVsValidateCodeParameterWithoutDisplay();

      // WHEN
      ValidateCodeResult result =
          CombiningVsValidateCodeOperation.validateCode(terminologyService, request, SCOPE);

      // THEN
      assertThat(result.isResult()).isTrue();
      verify(terminologyService, times(1))
          .validateCode(eq(SCOPE), any(CsValidateCodeParameter.class));
    }
  }

  /** Data helper for validate-code operation test fixtures. */
  private static final class VsValidateCodeTestDataHelper {

    private VsValidateCodeTestDataHelper() {}

    private static VsValidateCodeParameter createVsValidateCodeParameterWithDisplay(
        boolean lenientDisplayValidation) {
      return VsValidateCodeParameter.builder()
          .urlParam(new UriParam(VALUE_SET_URL))
          .codeParam(new TokenParam(CODE))
          .systemParam(new UriParam(CODE_SYSTEM_URL))
          .displayParam(new StringParam(DISPLAY))
          .displayLanguageParam(new TokenParam(DISPLAY_LANGUAGE))
          .lenientDisplayValidationParam(new BooleanType(lenientDisplayValidation))
          .build();
    }

    private static VsValidateCodeParameter createVsValidateCodeParameterWithoutDisplay() {
      return VsValidateCodeParameter.builder()
          .urlParam(new UriParam(VALUE_SET_URL))
          .codeParam(new TokenParam(CODE))
          .systemParam(new UriParam(CODE_SYSTEM_URL))
          .lenientDisplayValidationParam(new BooleanType(true))
          .build();
    }

    private static ValueSet createExpansionValueSet() {
      ValueSet valueSet = new ValueSet();
      ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
      ValueSet.ValueSetExpansionContainsComponent contains =
          new ValueSet.ValueSetExpansionContainsComponent();
      contains.setSystem(CODE_SYSTEM_URL);
      contains.setCode(CODE);
      expansion.addContains(contains);
      valueSet.setExpansion(expansion);
      return valueSet;
    }

    private static CodeSystem createCodeSystem() {
      CodeSystem codeSystem = new CodeSystem();
      codeSystem.setUrl(CODE_SYSTEM_URL);
      codeSystem.setVersionNeeded(false);
      codeSystem.setCaseSensitive(true);
      return codeSystem;
    }
  }
}
