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
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.loinc.config.LoincConfigProperties;
import de.gematik.zts.termzilla.terminologies.loinc.helper.LoincReleaseFileHelper;
import de.gematik.zts.termzilla.terminologies.loinc.memory.LoincInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LinguisticVariantVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Unit tests for {@link Loinc}. */
@DisplayName("Loinc Tests")
class LoincTest {

  private static final String RELEASE_LOCATION = "/tmp/loinc-release";
  private static final String RELEASE_HASH = "release-hash";

  private static final String VERSION_275 = "2.75";
  private static final String VERSION_276 = "2.76";

  private static final String TERM_CODE = "1234-5";
  private static final String PART_CODE = "LP123-4";
  private static final String ANSWER_CODE = "LA1234-5";
  private static final String ANSWER_LIST_CODE = "LL123-4";

  private static final String DEFAULT_LANGUAGE = "de-DE";

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize versions, languages and release hash")
    void shouldInitializeVersionsLanguagesAndReleaseHash() throws InvalidConfigurationException {
      // GIVEN
      LoincVersion olderVersion = LoincTestDataHelper.loincVersion(275, VERSION_275);
      LoincVersion newerVersion = LoincTestDataHelper.loincVersion(276, VERSION_276);
      Set<LoincVersion> supportedVersions = Set.of(olderVersion, newerVersion);

      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) ->
                      when(mock.getLoincVersionList()).thenReturn(supportedVersions))) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);

        // THEN
        assertThat(loinc.getSupportedLoincVersions())
            .containsExactlyInAnyOrder(olderVersion, newerVersion);
        assertThat(loinc.getDefaultLoincVersion()).isEqualTo(newerVersion);
        assertThat(loinc.getDefaultLanguage()).isEqualTo(DEFAULT_LANGUAGE);
        assertThat(loinc.getSupportedLanguages())
            .containsExactlyInAnyOrder(DEFAULT_LANGUAGE, LoincConstants.LOINC_LANGUAGE);
        assertThat(loinc.getReleaseDataHash()).isEqualTo(RELEASE_HASH);
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }
  }

  @Nested
  @DisplayName("getLoincVersion() Tests")
  class GetLoincVersionTests {

    @Test
    @DisplayName("Should return configured version when it exists")
    void shouldReturnConfiguredVersionWhenItExists()
        throws InvalidConfigurationException, UnsupportedTerminologyException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getLoincVersion(VERSION_276)).thenReturn(Optional.of(version));
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);
        LoincVersion foundVersion = loinc.getLoincVersion(VERSION_276);

        // THEN
        assertThat(foundVersion).isEqualTo(version);
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }

    @Test
    @DisplayName("Should throw UnsupportedTerminologyException when version does not exist")
    void shouldThrowWhenVersionDoesNotExist() throws InvalidConfigurationException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getLoincVersion("9.99")).thenReturn(Optional.empty());
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);

        // THEN
        assertThatThrownBy(() -> loinc.getLoincVersion("9.99"))
            .isInstanceOf(UnsupportedTerminologyException.class)
            .hasMessageContaining("9.99");
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }
  }

  @Nested
  @DisplayName("getLoincConcept() Tests")
  class GetLoincConceptTests {

    @Test
    @DisplayName("Should resolve LP code from part data")
    void shouldResolvePartCode()
        throws InvalidConfigurationException, UnknownConceptException, InactiveConceptException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      PartVersion partVersion =
          LoincTestDataHelper.partVersion(
              PART_CODE, LoincConstants.LOINC_STATUSCODE_ACTIVE, "Part Name", "Part Display");
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getPartVersionByPartNumber(PART_CODE, version))
                        .thenReturn(Optional.of(partVersion));
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);
        LoincConcept concept = loinc.getLoincConcept(PART_CODE, version, false, Set.of());

        // THEN
        assertThat(concept.getCode()).isEqualTo(PART_CODE);
        assertThat(concept.isActive()).isTrue();
        assertThat(concept.getDesignationList())
            .extracting(LoincDesignation::getType, LoincDesignation::getDesignation)
            .containsAnyOf(
                tuple(LoincConstants.LOINC_DESIGNATION_PART_NAME, "Part Name"),
                tuple(LoincConstants.LOINC_DESIGNATION_PARTDISPLAY_NAME, "Part Display"));
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }

    @Test
    @DisplayName(
        "Should throw InactiveConceptException for inactive answer when includeInactive is false")
    void shouldThrowForInactiveAnswerWhenIncludeInactiveIsFalse()
        throws InvalidConfigurationException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      AnswerVersion inactiveAnswer =
          LoincTestDataHelper.answerVersion(ANSWER_CODE, "INACTIVE", "Display Text");
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getAnswerVersion(ANSWER_CODE, version))
                        .thenReturn(Optional.of(inactiveAnswer));
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);

        // THEN
        assertThatThrownBy(() -> loinc.getLoincConcept(ANSWER_CODE, version, false, null))
            .isInstanceOf(InactiveConceptException.class)
            .hasMessageContaining(ANSWER_CODE);
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }

    @Test
    @DisplayName("Should resolve LL code from answer list data")
    void shouldResolveAnswerListCode()
        throws InvalidConfigurationException, UnknownConceptException, InactiveConceptException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      AnswerListVersion answerListVersion =
          LoincTestDataHelper.answerListVersion(
              ANSWER_LIST_CODE,
              LoincConstants.LOINC_STATUSCODE_ACTIVE,
              "Answer List Name",
              Set.of("LA1", "LA2"));
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getAnswerListVersion(ANSWER_LIST_CODE, version))
                        .thenReturn(Optional.of(answerListVersion));
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);
        LoincConcept concept = loinc.getLoincConcept(ANSWER_LIST_CODE, version, false, Set.of());

        // THEN
        assertThat(concept.getCode()).isEqualTo(ANSWER_LIST_CODE);
        assertThat(concept.isActive()).isTrue();
        assertThat(concept.getDesignationList())
            .extracting(LoincDesignation::getType, LoincDesignation::getDesignation)
            .contains(tuple(LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME, "Answer List Name"));
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }

    @Test
    @DisplayName("Should resolve main table concept and enrich requested language variants")
    void shouldResolveMainConceptAndEnrichLanguageVariants()
        throws InvalidConfigurationException, UnknownConceptException, InactiveConceptException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      ConceptVersion conceptVersion = LoincTestDataHelper.activeConceptVersion(TERM_CODE);
      LinguisticVariantVersion deVariant =
          LoincTestDataHelper.linguisticVariantVersion(TERM_CODE, "de-DE");
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getConceptVersion(TERM_CODE, version))
                        .thenReturn(Optional.of(conceptVersion));
                    when(mock.getLinguisticVariantVersion(TERM_CODE, version, "de-DE"))
                        .thenReturn(Optional.of(deVariant));
                    when(mock.getLinguisticVariantVersion(TERM_CODE, version, "de"))
                        .thenReturn(Optional.empty());
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);
        LoincConcept concept = loinc.getLoincConcept(TERM_CODE, version, false, Set.of("de"));

        // THEN
        assertThat(concept.getCode()).isEqualTo(TERM_CODE);
        assertThat(concept.getDefinition()).isEqualTo("Definition text");
        assertThat(concept.getDesignationList())
            .extracting(LoincDesignation::getLanguage, LoincDesignation::getType)
            .contains(
                tuple(
                    LoincConstants.LOINC_LANGUAGE,
                    LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME),
                tuple(LoincConstants.LOINC_LANGUAGE, LoincConstants.LOINC_DESIGNATION_SHORT_NAME),
                tuple("de-DE", LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME),
                tuple("de-DE", LoincConstants.LOINC_DESIGNATION_FULLY_SPECIFIED_NAME));

        LoincInMemoryRepresentation representationMock =
            representationConstruction.constructed().getFirst();
        verify(representationMock).getLinguisticVariantVersion(TERM_CODE, version, "de-DE");
      }
    }

    @Test
    @DisplayName("Should throw UnknownConceptException when concept cannot be resolved")
    void shouldThrowWhenConceptCannotBeResolved() throws InvalidConfigurationException {
      // GIVEN
      LoincVersion version = LoincTestDataHelper.loincVersion(276, VERSION_276);
      LoincConfigProperties properties = createProperties();

      // WHEN
      try (MockedStatic<LoincReleaseFileHelper> releaseFileHelperMock =
              mockStatic(LoincReleaseFileHelper.class);
          MockedConstruction<LoincInMemoryRepresentation> representationConstruction =
              mockConstruction(
                  LoincInMemoryRepresentation.class,
                  (mock, context) -> {
                    when(mock.getLoincVersionList()).thenReturn(Set.of(version));
                    when(mock.getConceptVersion(TERM_CODE, version)).thenReturn(Optional.empty());
                  })) {

        releaseFileHelperMock
            .when(() -> LoincReleaseFileHelper.calculateReleaseFileHash(RELEASE_LOCATION))
            .thenReturn(RELEASE_HASH);

        Loinc loinc = new Loinc(properties);

        // THEN
        assertThatThrownBy(() -> loinc.getLoincConcept(TERM_CODE, version, true, Set.of("de-DE")))
            .isInstanceOf(UnknownConceptException.class)
            .hasMessageContaining(TERM_CODE);
        assertThat(representationConstruction.constructed()).hasSize(1);
      }
    }
  }

  private static LoincConfigProperties createProperties() {
    LoincConfigProperties properties = new LoincConfigProperties();
    properties.setLoincDefaultLanguage(DEFAULT_LANGUAGE);
    properties.setLoincLanguages(new ArrayList<>(List.of(DEFAULT_LANGUAGE)));
    properties.setLoincRfCollectionLocation(RELEASE_LOCATION);
    properties.setBaseWorkingDirectory(Path.of("/tmp/work"));
    return properties;
  }
}
