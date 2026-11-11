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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.vs.expand;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SupplementValidator}. */
@DisplayName("SupplementValidator Tests")
class SupplementValidatorTest {

  private static final String BASE_URL = "http://example.org/fhir/CodeSystem/base";
  private static final String BASE_VERSION = "1.0.0";
  private static final String SUPPLEMENT_URL = "http://example.org/fhir/CodeSystem/base-de";

  @Nested
  @DisplayName("validateSupplement Tests")
  class ValidateSupplementTests {

    @Test
    @DisplayName("Should accept a valid supplement applicable to the base CodeSystem")
    void shouldAcceptValidSupplement() {
      CodeSystem base = baseCodeSystem();
      CodeSystem supplement = validSupplement();

      assertThatCode(() -> SupplementValidator.validateSupplement(supplement, base))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject a null supplement resource")
    void ERR_0404_shouldRejectNullSupplement() {
      CodeSystem codeSystem = baseCodeSystem();

      assertThatThrownBy(() ->
        SupplementValidator.validateSupplement(null, codeSystem))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("not a CodeSystem");
    }

    @Test
    @DisplayName("Should reject a CodeSystem that is not a supplement")
    void ERR_0405_shouldRejectNonSupplementCodeSystem() {
      CodeSystem notASupplement = baseCodeSystem(); // content = complete
      CodeSystem codeSystem = baseCodeSystem();

      assertThatThrownBy(
              () -> SupplementValidator.validateSupplement(notASupplement, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not a supplement");
    }

    @Test
    @DisplayName("Should reject a supplement missing the supplements reference")
    void ERR_0406_shouldRejectSupplementMissingSupplements() {
      CodeSystem supplement = validSupplement();
      CodeSystem codeSystem = baseCodeSystem();

      supplement.setSupplements(null);
      assertThatThrownBy(() -> SupplementValidator.validateSupplement(supplement, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("supplements");
    }

    @Test
    @DisplayName("Should reject a supplement targeting another base CodeSystem")
    void ERR_0408_shouldRejectSupplementForDifferentBaseSystem() {
      CodeSystem supplement = validSupplement();
      CodeSystem codeSystem = baseCodeSystem();

      supplement.setSupplements("http://example.org/fhir/CodeSystem/other");
      assertThatThrownBy(() -> SupplementValidator.validateSupplement(supplement, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("Should reject a supplement targeting another base version")
    void ERR_0409_shouldRejectSupplementForDifferentBaseVersion() {
      CodeSystem supplement = validSupplement();
      CodeSystem codeSystem = baseCodeSystem();

      supplement.setSupplements(BASE_URL + "|9.9.9");
      assertThatThrownBy(() -> SupplementValidator.validateSupplement(supplement, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("base version");
    }

    @Test
    @DisplayName("Should reject a supplement containing an unknown base code")
    void ERR_0410_shouldRejectSupplementWithUnknownCode() {
      CodeSystem supplement = validSupplement();
      CodeSystem codeSystem = baseCodeSystem();

      supplement.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("UNKNOWN"));
      assertThatThrownBy(() -> SupplementValidator.validateSupplement(supplement, codeSystem))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("not present in the base CodeSystem");
    }
  }

  @Nested
  @DisplayName("failIfSupplementUsedAsSystem Tests")
  class FailIfSupplementUsedAsSystemTests {

    @Test
    @DisplayName("Should reject a supplement used as an independent system")
    void ERR_0411_shouldRejectSupplementUsedAsSystem() {
      CodeSystem supplement = validSupplement();

      assertThatThrownBy(() -> SupplementValidator.failIfSupplementUsedAsSystem(supplement))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("supplement")
          .hasMessageContaining("independent");
    }

    @Test
    @DisplayName("Should accept a complete CodeSystem used as an independent system")
    void shouldAcceptCompleteCodeSystemAsSystem() {
      assertThatCode(() -> SupplementValidator.failIfSupplementUsedAsSystem(baseCodeSystem()))
          .doesNotThrowAnyException();
    }
  }

  // ----------------------------------------------------------------------------------------------
  // Fixtures
  // ----------------------------------------------------------------------------------------------

  private static CodeSystem baseCodeSystem() {
    CodeSystem cs = new CodeSystem();
    cs.setUrl(BASE_URL);
    cs.setVersion(BASE_VERSION);
    cs.setContent(CodeSystemContentMode.COMPLETE);
    cs.setCaseSensitive(true);
    cs.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("A").setDisplay("Alpha"));
    cs.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("B").setDisplay("Beta"));
    return cs;
  }

  private static CodeSystem validSupplement() {
    CodeSystem supplement = new CodeSystem();
    supplement.setUrl(SUPPLEMENT_URL);
    supplement.setVersion(BASE_VERSION);
    supplement.setContent(CodeSystemContentMode.SUPPLEMENT);
    supplement.setSupplements(BASE_URL);
    supplement.addConcept(new CodeSystem.ConceptDefinitionComponent().setCode("A"));
    return supplement;
  }
}
