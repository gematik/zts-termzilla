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

import static de.gematik.zts.termzilla.terminologies.loinc.LoincConstants.LOINC_STATUSCODE_ACTIVE;

import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.InvalidConfigurationException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.common.helper.LanguageRegistry;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Central in-memory model for LOINC terminology data.
 *
 * <p>Initializes and holds the {@link LoincInMemoryRepresentation} covering LOINC observations,
 * parts, answers, answer lists, and linguistic variants. On construction the set of supported
 * release versions and the default version are determined, a release data hash is computed for
 * cache identification, and the configured languages are registered.
 *
 * <p>Concepts are resolved by code across all LOINC concept types via {@link
 * #getLoincConcept(String, LoincVersion, boolean, Set)}.
 */
@Slf4j
public class Loinc {

  @Getter
  private final LoincInMemoryRepresentation loincInMemoryRepresentation;

  @Getter
  private final List<LoincVersion> supportedLoincVersions = new ArrayList<>();
  @Getter
  private LoincVersion defaultLoincVersion;
  @Getter
  private final String defaultLanguage;
  @Getter
  private final List<String> supportedLanguages;
  @Getter
  private final String releaseDataHash;

  /**
   * Creates and fully initializes the LOINC in-memory model from the given configuration.
   *
   * <p>Loads the in-memory representation, determines supported release versions, sets the default
   * version and language, and computes a release data hash for cache identification.
   *
   * @param properties the LOINC configuration properties
   * @throws InvalidConfigurationException if the release files cannot be located or read
   */
  public Loinc(LoincConfigProperties properties) throws InvalidConfigurationException {

    // In-Memory-Objektmodell initialisieren
    loincInMemoryRepresentation = new LoincInMemoryRepresentation(properties);

    // Unterstützte Versionen auf Grundlage der Daten aus dem Index ermitteln
    setSupportedReleaseVersions();

    // Setzen der Default-Version (sollte immer die aktuelle sein)
    setDefaultReleaseVersion();

    // Setzen der Default-Sprache
    defaultLanguage = properties.getLoincDefaultLanguage();
    supportedLanguages = properties.getLoincLanguages();

    // en-US ist immer unterstützt
    supportedLanguages.add(LoincConstants.LOINC_LANGUAGE);

    // Berechnen eines Hashs der Release-Dateien, um diese im Rahmen der Caching-Strategie eindeutig
    // identifizieren zu können
    releaseDataHash =
        LoincReleaseFileHelper.calculateReleaseFileHash(properties.getLoincRfCollectionLocation());
  }

  // =======================================================================
  // ReleaseVersion-management related methods
  // =======================================================================

  /* TODO: Deprecated German documentation - replaced by English JavaDoc below.
   * Index abfragen, um eine Liste unterstützter LOINC Versionen ermitteln zu können
   */

  /**
   * Populates the list of supported LOINC versions from the in-memory representation.
   */
  private void setSupportedReleaseVersions() {
    this.supportedLoincVersions.addAll(loincInMemoryRepresentation.getLoincVersionList());
  }

  /**
   * Determines and sets the default LOINC version as the latest available version.
   */
  private void setDefaultReleaseVersion() {
    this.defaultLoincVersion = Collections.max(this.supportedLoincVersions);
    log.info("Default version for LOINC set to '{}'.", this.defaultLoincVersion);
  }

  // =======================================================================
  // Working with versions
  // =======================================================================

  /**
   * Returns the {@link LoincVersion} for the given version string.
   *
   * @param versionString the LOINC version identifier to look up
   * @return the matching {@link LoincVersion}
   * @throws UnsupportedTerminologyException if no version with the given identifier is known
   */
  public LoincVersion getLoincVersion(String versionString) throws UnsupportedTerminologyException {

    Optional<LoincVersion> loincVersion =
        loincInMemoryRepresentation.getLoincVersion(versionString);
    if (loincVersion.isPresent()) {
      return loincVersion.get();
    } else {
      throw new UnsupportedTerminologyException(
          "LOINC version '" + versionString + "' is not supported.");
    }
  }

  // =======================================================================
  // Working with concepts
  // =======================================================================

  /**
   * Resolves a LOINC concept by code within the given version.
   *
   * <p>The lookup is performed across all LOINC concept types in the following order: parts
   * (LP-prefixed codes), answers (LA-prefixed codes), answer lists (LL-prefixed codes), and finally
   * the main LOINC observation table. If a code is not found in its type-specific store, the search
   * falls through to the next. For concepts found in the main LOINC table, linguistic variant
   * designations are added for each language in {@code languageSet} when provided.
   *
   * @param code            the LOINC code to look up
   * @param version         the LOINC version to search in
   * @param includeInactive {@code true} to return inactive concepts; {@code false} to throw for
   *                        inactive concepts
   * @param languageSet     an optional set of language tags for which additional designations
   *                        should be included; may be {@code null} or empty
   * @return the resolved {@link LoincConcept}
   * @throws UnknownConceptException  if no concept with the given code exists in the given version
   * @throws InactiveConceptException if the concept is inactive and {@code includeInactive} is
   *                                  {@code false}
   */
  public LoincConcept getLoincConcept(
      String code, LoincVersion version, boolean includeInactive, Set<String> languageSet)
      throws UnknownConceptException, InactiveConceptException {

    // Suche in den Parts
    // TODO: Nochmal über den Umgang mit dem Status nachdenken. Das ist irgendwie nicht so ganz
    //  klar, was da passieren soll LOINC selbst sagt in ihrer FHIR-Umsetzung zu deprecated Parts
    //  zum Teil, dass diese gar nicht existieren....
    if (code.startsWith("LP")) {
      Optional<PartVersion> partVersion =
          loincInMemoryRepresentation.getPartVersionByPartNumber(code, version);
      if (partVersion.isPresent()) {
        return requireActive(
            getLoincConcept(partVersion.get(), version), code, version, includeInactive);
      }
    }

    // Suche in den Answers
    // TODO: Nochmal über den Umgang mit dem Status nachdenken. Das ist irgendwie nicht so ganz
    //  klar, was da passieren soll. LOINC selbst sagt in ihrer FHIR-Umsetzung zu deprecated LAs
    //  zum Teil, dass diese gar nicht existieren....
    if (code.startsWith("LA")) {
      Optional<AnswerVersion> answerVersion =
          loincInMemoryRepresentation.getAnswerVersion(code, version);
      if (answerVersion.isPresent()) {
        return requireActive(
            getLoincConcept(answerVersion.get(), version), code, version, includeInactive);
      }
    }

    // Suche in den AnswerLists
    // TODO: Nochmal über den Umgang mit dem Status nachdenken. Das ist irgendwie nicht so ganz
    // klar, was da passieren soll
    //  LOINC selbst sagt in ihrer FHIR-Umsetzung zu deprecated LLs zum Teil, dass diese gar nicht
    // existieren....
    if (code.startsWith("LL")) {
      Optional<AnswerListVersion> answerListVersion =
          loincInMemoryRepresentation.getAnswerListVersion(code, version);
      if (answerListVersion.isPresent()) {
        return requireActive(
            getLoincConcept(answerListVersion.get(), version), code, version, includeInactive);
      }
    }

    // Suche in der LoincTable
    Optional<ConceptVersion> conceptVersion =
        loincInMemoryRepresentation.getConceptVersion(code, version);
    if (conceptVersion.isPresent()) {

      // Konzept erstellen - in den Designation werden mindestens die en-US-Designationen eingefügt
      LoincConcept returnConcept = getLoincConcept(conceptVersion.get(), version);

      // Bei Bedarf zusätzliche linguistische Varianten hinzufügen
      if (languageSet != null && !languageSet.isEmpty()) {
        Set<String> resolvedLanguageSet =
            languageSet.stream()
                .filter(language -> LanguageRegistry.getBaseLanguages().contains(language))
                .flatMap(language -> LanguageRegistry.getLanguageVariants(language).stream())
                .collect(Collectors.toSet());
        resolvedLanguageSet.addAll(languageSet);
        returnConcept =
            enrichLoincConceptWithSelectedLinguisticVariants(
                returnConcept, version, resolvedLanguageSet);
      }

      return requireActive(returnConcept, code, version, includeInactive);
    }

    throw new UnknownConceptException(
        "Concept with code '"
            + code
            + "' not found in LOINC version '"
            + version.getVersionString()
            + "'.");
  }

  /**
   * Returns the given concept if active or if inactive concepts are permitted, otherwise throws.
   *
   * @param concept         the concept to check
   * @param code            the code identifying the concept, used in the exception message
   * @param version         the version context, used in the exception message
   * @param includeInactive {@code true} to allow inactive concepts
   * @return the concept unchanged
   * @throws InactiveConceptException if the concept is inactive and {@code includeInactive} is
   *                                  {@code false}
   */
  private LoincConcept requireActive(
      LoincConcept concept, String code, LoincVersion version, boolean includeInactive)
      throws InactiveConceptException {
    if (!includeInactive && !concept.isActive()) {
      throw new InactiveConceptException(
          "Concept with code '"
              + code
              + "' is not active in LOINC version '"
              + version.getVersionString()
              + "'.");
    }
    return concept;
  }

  /**
   * Builds a {@link LoincConcept} from a main-table {@link ConceptVersion}.
   *
   * <p>Populates all available en-US designations (long common name, short name, fully specified
   * name, consumer name) and the full set of LOINC properties from the given version record.
   *
   * @param conceptVersion the source data record from the LOINC main table
   * @param loincVersion   the LOINC version this record belongs to
   * @return a fully populated {@link LoincConcept}
   */
  private LoincConcept getLoincConcept(ConceptVersion conceptVersion, LoincVersion loincVersion) {

    LoincConcept loincConcept = new LoincConcept();
    loincConcept.setCode(conceptVersion.getLoincNum());
    loincConcept.setVersion(loincVersion);
    loincConcept.setActive(conceptVersion.getStatus().equals(LOINC_STATUSCODE_ACTIVE));
    loincConcept.setDefinition(conceptVersion.getDefinitionDescription());

    // Add default "en-US" designations
    if (StringUtils.isNoneEmpty(conceptVersion.getLongCommonName())) {
      LoincDesignation lcnDesignation = new LoincDesignation();
      lcnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      lcnDesignation.setType(LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME);
      lcnDesignation.setDesignation(conceptVersion.getLongCommonName());
      loincConcept.getDesignationList().add(lcnDesignation);
    }

    if (StringUtils.isNoneEmpty(conceptVersion.getShortName())) {
      LoincDesignation snDesignation = new LoincDesignation();
      snDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      snDesignation.setType(LoincConstants.LOINC_DESIGNATION_SHORT_NAME);
      snDesignation.setDesignation(conceptVersion.getShortName());
      loincConcept.getDesignationList().add(snDesignation);
    }

    if (StringUtils.isNoneEmpty(conceptVersion.getComponent())) {
      LoincDesignation fsnDesignation = new LoincDesignation();
      fsnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      fsnDesignation.setType(LoincConstants.LOINC_DESIGNATION_FULLY_SPECIFIED_NAME);
      String fsn =
          conceptVersion.getComponent()
              + ":"
              + conceptVersion.getProperty()
              + ":"
              + conceptVersion.getTimeAspect()
              + ":"
              + conceptVersion.getSystem()
              + ":"
              + conceptVersion.getScaleType()
              + ":"
              + conceptVersion.getMethodType();
      fsnDesignation.setDesignation(fsn);
      loincConcept.getDesignationList().add(fsnDesignation);
    }

    if (StringUtils.isNoneEmpty(conceptVersion.getConsumerName())) {
      LoincDesignation cnDesignation = new LoincDesignation();
      cnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      cnDesignation.setType(LoincConstants.LOINC_DESIGNATION_CONSUMER_NAME);
      cnDesignation.setDesignation(conceptVersion.getConsumerName());
      loincConcept.getDesignationList().add(cnDesignation);
    }

    // Add properties
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_COMPONENT, conceptVersion.getComponent());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_PROPERTY, conceptVersion.getProperty());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_TIME_ASPCT, conceptVersion.getTimeAspect());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_SYSTEM, conceptVersion.getSystem());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_SCALE_TYP, conceptVersion.getScaleType());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_METHOD_TYP, conceptVersion.getMethodType());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_CLASS, conceptVersion.getClasstype());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_VERSION_LAST_CHANGED, conceptVersion.getVersionLastChanged());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_CHNG_TYPE, conceptVersion.getChangeType());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_DEFINITION_DESCRIPTION, conceptVersion.getDefinitionDescription());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_STATUS, conceptVersion.getStatus());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_CONSUMER_NAME, conceptVersion.getConsumerName());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_CLASSTYPE, conceptVersion.getClasstype());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_FORMULA, conceptVersion.getFormula());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_EXMPL_ANSWERS, conceptVersion.getExampleAnswers());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_SURVEY_QUEST_TEXT, conceptVersion.getSurveyQuestText());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_SURVEY_QUEST_SRC, conceptVersion.getSurveyQuestSrc());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_UNITSREQUIRED, conceptVersion.getUnitsRequired());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_RELATEDNAMES2, conceptVersion.getRelatedNames2());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_SHORTNAME, conceptVersion.getShortName());
    loincConcept.getPropertyMap().put(ConceptVersion.NAME_ORDER_OBS, conceptVersion.getOrderObs());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_HL7_FIELD_SUBFIELD_ID, conceptVersion.getHl7FieldSubfieldId());
    loincConcept
        .getPropertyMap()
        .put(
            ConceptVersion.NAME_EXTERNAL_COPYRIGHT_NOTICE,
            conceptVersion.getExternalCopyRightNotice());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_EXAMPLE_UNITS, conceptVersion.getExampleUnits());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_LONG_COMMON_NAME, conceptVersion.getLongCommonName());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_EXAMPLE_UCUM_UNITS, conceptVersion.getExampleUcumUnits());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_STATUS_REASON, conceptVersion.getStatusReason());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_STATUS_TEXT, conceptVersion.getStatusText());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_CHANGE_REASON_PUBLIC, conceptVersion.getChangeReasonPublic());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_COMMON_TEST_RANK, conceptVersion.getCommonTestRank());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_COMMON_ORDER_RANK, conceptVersion.getCommonOrderRank());
    loincConcept
        .getPropertyMap()
        .put(
            ConceptVersion.NAME_HL7_ATTACHMENT_STRUCTURE,
            conceptVersion.getHl7AttachmentStructure());
    loincConcept
        .getPropertyMap()
        .put(
            ConceptVersion.NAME_EXTERNAL_COPYRIGHT_LINK, conceptVersion.getExternalCopyRightLink());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_PANEL_TYPE, conceptVersion.getPanelType());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_ASK_AT_ORDER_ENTRY, conceptVersion.getAskAtOrderEntry());
    loincConcept
        .getPropertyMap()
        .put(
            ConceptVersion.NAME_ASSOCIATED_OBSERVATIONS,
            conceptVersion.getAssociatedObservations());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_VERSION_FIRST_RELEASED, conceptVersion.getVersionFirstReleased());
    loincConcept
        .getPropertyMap()
        .put(
            ConceptVersion.NAME_VALID_HL7_ATTACHMENT_REQUEST,
            conceptVersion.getValidHL7AttachmentRequest());
    loincConcept
        .getPropertyMap()
        .put(ConceptVersion.NAME_DISPLAY_NAME, conceptVersion.getDisplayName());

    return loincConcept;
  }

  /**
   * Builds a {@link LoincConcept} from a {@link PartVersion}.
   *
   * <p>Populates the part name and part display name as en-US designations when present.
   *
   * @param partVersion  the source data record for the LOINC part
   * @param loincVersion the LOINC version this record belongs to
   * @return a {@link LoincConcept} representing the LOINC part
   */
  private LoincConcept getLoincConcept(PartVersion partVersion, LoincVersion loincVersion) {

    LoincConcept loincConcept = new LoincConcept();
    loincConcept.setCode(partVersion.getPartNumber());
    loincConcept.setVersion(loincVersion);
    loincConcept.setActive(partVersion.getStatus().equals(LOINC_STATUSCODE_ACTIVE));

    // Add default "en-US" designations
    if (StringUtils.isNoneEmpty(partVersion.getPartName())) {
      LoincDesignation lcnDesignation = new LoincDesignation();
      lcnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      lcnDesignation.setType(LoincConstants.LOINC_DESIGNATION_PART_NAME);
      lcnDesignation.setDesignation(partVersion.getPartName());
      loincConcept.getDesignationList().add(lcnDesignation);
    }

    if (StringUtils.isNoneEmpty(partVersion.getPartDisplayName())) {
      LoincDesignation lcnDesignation = new LoincDesignation();
      lcnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      lcnDesignation.setType(LoincConstants.LOINC_DESIGNATION_PARTDISPLAY_NAME);
      lcnDesignation.setDesignation(partVersion.getPartDisplayName());
      loincConcept.getDesignationList().add(lcnDesignation);
    }

    return loincConcept;
  }

  /**
   * Builds a {@link LoincConcept} from an {@link AnswerVersion}.
   *
   * <p>Populates the display text as an en-US designation when present.
   *
   * @param answerVersion the source data record for the LOINC answer
   * @param loincVersion  the LOINC version this record belongs to
   * @return a {@link LoincConcept} representing the LOINC answer
   */
  private LoincConcept getLoincConcept(AnswerVersion answerVersion, LoincVersion loincVersion) {

    LoincConcept loincConcept = new LoincConcept();
    loincConcept.setCode(answerVersion.getAnswerStringId());
    loincConcept.setVersion(loincVersion);
    loincConcept.setActive(answerVersion.getStatus().equals(LOINC_STATUSCODE_ACTIVE));

    // Add default "en-US" designations
    if (StringUtils.isNoneEmpty(answerVersion.getDisplayText())) {
      LoincDesignation lcnDesignation = new LoincDesignation();
      lcnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      lcnDesignation.setType(LoincConstants.LOINC_DESIGNATION_DISPLAYTEXT);
      lcnDesignation.setDesignation(answerVersion.getDisplayText());
      loincConcept.getDesignationList().add(lcnDesignation);
    }

    return loincConcept;
  }

  /**
   * Builds a {@link LoincConcept} from an {@link AnswerListVersion}.
   *
   * <p>Populates the answer list name as an en-US designation when present.
   *
   * @param answerListVersion the source data record for the LOINC answer list
   * @param loincVersion      the LOINC version this record belongs to
   * @return a {@link LoincConcept} representing the LOINC answer list
   */
  private LoincConcept getLoincConcept(
      AnswerListVersion answerListVersion, LoincVersion loincVersion) {

    LoincConcept loincConcept = new LoincConcept();
    loincConcept.setCode(answerListVersion.getAnswerListId());
    loincConcept.setVersion(loincVersion);
    loincConcept.setActive(answerListVersion.getStatus().equals(LOINC_STATUSCODE_ACTIVE));

    // Add default "en-US" designations
    if (StringUtils.isNoneEmpty(answerListVersion.getAnswerListName())) {
      LoincDesignation lcnDesignation = new LoincDesignation();
      lcnDesignation.setLanguage(LoincConstants.LOINC_LANGUAGE);
      lcnDesignation.setType(LoincConstants.LOINC_DESIGNATION_ANSWERLIST_NAME);
      lcnDesignation.setDesignation(answerListVersion.getAnswerListName());
      loincConcept.getDesignationList().add(lcnDesignation);
    }

    return loincConcept;
  }

  /**
   * Adds language-specific designations to the given concept from the matching linguistic
   * variants.
   *
   * <p>For each language in {@code languageSet}, the corresponding
   * {@link LinguisticVariantVersion} is looked up and its long common name, short name, and fully
   * specified name are appended as additional designations when present.
   *
   * @param loincConcept the concept to enrich
   * @param loincVersion the LOINC version to look up linguistic variants in
   * @param languageSet  the set of language tags for which designations should be added
   * @return the enriched concept
   */
  private LoincConcept enrichLoincConceptWithSelectedLinguisticVariants(
      LoincConcept loincConcept, LoincVersion loincVersion, Set<String> languageSet) {

    for (String language : languageSet) {

      Optional<LinguisticVariantVersion> lvVersion =
          loincInMemoryRepresentation.getLinguisticVariantVersion(
              loincConcept.getCode(), loincVersion, language);

      if (lvVersion.isPresent()) {
        // LongCommonName hinzufügen
        if (StringUtils.isNoneEmpty(lvVersion.get().getLongCommonName())) {
          LoincDesignation designation = new LoincDesignation();
          designation.setLanguage(lvVersion.get().getLanguage());
          designation.setType(LoincConstants.LOINC_DESIGNATION_LONG_COMMON_NAME);
          designation.setDesignation(lvVersion.get().getLongCommonName());
          loincConcept.getDesignationList().add(designation);
        }
        // ShortName hinzufügen
        if (StringUtils.isNoneEmpty(lvVersion.get().getShortName())) {
          LoincDesignation designation = new LoincDesignation();
          designation.setLanguage(lvVersion.get().getLanguage());
          designation.setType(LoincConstants.LOINC_DESIGNATION_SHORT_NAME);
          designation.setDesignation(lvVersion.get().getShortName());
          loincConcept.getDesignationList().add(designation);
        }
        // FullySpecifiedName hinzufügen
        if (StringUtils.isNoneEmpty(lvVersion.get().getComponent())) {
          LoincDesignation designation = new LoincDesignation();
          designation.setLanguage(lvVersion.get().getLanguage());
          designation.setType(LoincConstants.LOINC_DESIGNATION_FULLY_SPECIFIED_NAME);
          String fsn =
              lvVersion.get().getComponent()
                  + ":"
                  + lvVersion.get().getProperty()
                  + ":"
                  + lvVersion.get().getTimeAspect()
                  + ":"
                  + lvVersion.get().getSystem()
                  + ":"
                  + lvVersion.get().getScaleType()
                  + ":"
                  + lvVersion.get().getMethodType();
          designation.setDesignation(fsn);
          loincConcept.getDesignationList().add(designation);
        }
      }
    }

    return loincConcept;
  }
}
