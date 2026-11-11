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

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.Designation;
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignation;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignationManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ValueSet;

/**
 * Builds {@link ValueSet.ValueSetExpansionContainsComponent} instances for expanded concepts.
 *
 * <p>Creates complete expansion contains components including system, version, code, display value
 * (considering language selection), inactive status, designations, and extensions from explicit
 * concept references.
 */
@Slf4j
public class ExpansionContainsComponentBuilder {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates a new expansion contains component builder.
   *
   * @param fhirTermsInMemoryRepresentation the in-memory representation for CodeSystem lookup
   */
  public ExpansionContainsComponentBuilder(
      FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation) {
    this.fhirTermsInMemoryRepresentation = fhirTermsInMemoryRepresentation;
  }

  /**
   * Creates an expansion contains component for a concept.
   *
   * <p>Builds a complete {@link ValueSet.ValueSetExpansionContainsComponent} including:
   *
   * <ul>
   *   <li>System, version, and code from the concept base info
   *   <li>Display value (considering displayLanguage parameter)
   *   <li>Inactive status flag
   *   <li>Designations (if requested, filtered by designation parameter)
   *   <li>Extensions from explicit concept references
   * </ul>
   *
   * @param conceptBaseInfo the base concept information
   * @param includeDesignations whether to include designations in the expansion
   * @param designationParamValueList list of designation filter values (empty for all)
   * @param languageSelectionParameter the language selection parameter for display designation
   *     selection
   * @param explicitConceptReferenceMap map of explicit concept references from ValueSet definition
   * @param scope the scope for CodeSystem lookup
   * @return a populated expansion contains component
   * @throws UnsupportedTerminologyException if the CodeSystem is not supported
   * @throws UnknownScopeException if the scope is not recognized
   */
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      String scope)
      throws UnsupportedTerminologyException, UnknownScopeException {
    return createExpansionContainsComponent(
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        languageSelectionParameter,
        explicitConceptReferenceMap,
        scope,
        null);
  }

  /**
   * Creates an expansion contains component, additionally applying resolved CodeSystem supplements.
   *
   * <p>When a supplement is registered for the concept's base CodeSystem, its designations are
   * collected with source {@code CSS} (CodeSystem Supplement) so they participate in display and
   * designation selection. Supplements never change membership — they only enrich metadata of codes
   * that already exist in the base CodeSystem.
   *
   * @param conceptBaseInfo the base concept information
   * @param includeDesignations whether to include designations in the expansion
   * @param designationParamValueList list of designation filter values (empty for all)
   * @param languageSelectionParameter the language selection parameter for display designation
   *     selection
   * @param explicitConceptReferenceMap map of explicit concept references from ValueSet definition
   * @param scope the scope for CodeSystem lookup
   * @param supplements resolved supplements keyed by base CodeSystem canonical URL (may be {@code
   *     null} or empty)
   * @return a populated expansion contains component
   * @throws UnsupportedTerminologyException if the CodeSystem is not supported
   * @throws UnknownScopeException if the scope is not recognized
   */
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap,
      String scope,
      java.util.Map<
              String,
        SupplementRequest>
          supplements)
      throws UnsupportedTerminologyException, UnknownScopeException {

    // Erstellen der ExpansionContainsComponent
    ValueSet.ValueSetExpansionContainsComponent expansionContainsComponent =
        new ValueSet.ValueSetExpansionContainsComponent();

    expansionContainsComponent.setSystem(conceptBaseInfo.getSystem());
    expansionContainsComponent.setVersion(conceptBaseInfo.getVersion());
    expansionContainsComponent.setCode(conceptBaseInfo.getCode());
    expansionContainsComponent.setInactive(!conceptBaseInfo.isActive());

    // Angefragtes System
    String system = conceptBaseInfo.getSystem();

    // select the appropriate CodeSystem
    CodeSystem codeSystem =
        fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class,
            scope,
            system,
            conceptBaseInfo.getVersion(),
            ResourceRetrievalMode.FULL);

    String csLanguage = codeSystem.getLanguage();

    try {
      // Abruf des Konzeptes aus dem Modell
      CodeSystem.ConceptDefinitionComponent concept =
          CodeSystemHelper.findCodeInCodeSystem(codeSystem, conceptBaseInfo.getCode());

      // Zusammensuchen aller Designations, die wir aus den vorhandenen Codesystem und
      // ValueSet-Informationen ableiten können
      FhirDesignationManager designationManager = new FhirDesignationManager();

      // Designations, die aus den ValueSet-Definitionen abgeleitet wurden, zum DesignationManager
      // hinzufügen.
      designationManager.collectDesignationsFromConceptReferenceComponents(
          explicitConceptReferenceMap.get(conceptBaseInfo.hashCode()));

      designationManager.collectDesignationsFromFhirConcept(concept, csLanguage);

      // Collect designations from an applicable CodeSystem supplement (source CSS). The supplement
      // only contributes metadata for codes that already exist in the base CodeSystem.
      collectSupplementDesignations(
          designationManager, supplements, system, conceptBaseInfo.getCode());

      // Setzen des Display-Wertes unter Berücksichtigung der displayLanguage-Informationen
      FhirDesignation displayDesignation =
          designationManager.getDisplay(languageSelectionParameter);
      if (displayDesignation != null) {
        expansionContainsComponent.setDisplay(displayDesignation.getValue());
      }

      // Setzen zusätzlicher Designations, falls gewünscht
      if (includeDesignations) {
        for (FhirDesignation designation :
            designationManager.selectDesignations(designationParamValueList)) {
          expansionContainsComponent.addDesignation(
              FhirDesignationManager.toDesignationComponent(designation));
        }
      }
    } catch (UnknownConceptException e) {
      log.debug("UnknownConceptException: {}", e.getMessage());
      throw new InvalidRequestException(e.getMessage(), e);
    }

    return expansionContainsComponent;
  }

  /**
   * Collects designations from the supplement applicable to the given base CodeSystem and code.
   *
   * <p>The supplement concept matching {@code code} contributes its display and designations with
   * source {@code CSS}. The supplement's own language is used as the fallback language for
   * designations without an explicit language tag.
   *
   * @param designationManager the designation manager to enrich
   * @param supplements resolved supplements keyed by base CodeSystem canonical URL
   * @param baseSystemUrl the base CodeSystem canonical URL of the current concept
   * @param code the concept code
   */
  private void collectSupplementDesignations(
      FhirDesignationManager designationManager,
      java.util.Map<
              String,
        SupplementRequest>
          supplements,
      String baseSystemUrl,
      String code) {

    if (supplements == null || supplements.isEmpty()) {
      return;
    }

    var supplementRequest = supplements.get(baseSystemUrl);
    if (supplementRequest == null) {
      return;
    }

    CodeSystem supplement = supplementRequest.supplement();
    String supplementLanguage = supplement.getLanguage();

    CodeSystem.ConceptDefinitionComponent supplementConcept = findConcept(supplement, code);
    if (supplementConcept == null) {
      return;
    }

    // The supplement display becomes a CSS-sourced display designation.
    if (supplementConcept.hasDisplay()) {
      designationManager.addDesignation(
          FhirDesignation.builder()
              .source(
                  Designation
                      .DesignationSource.CSS)
              .language(
                  new org.hl7.fhir.r4.model.CodeType(
                      supplementLanguage != null ? supplementLanguage : ""))
              .use(
                  new org.hl7.fhir.r4.model.Coding(
                      "http://terminology.hl7.org/CodeSystem/designation-usage", "display", null))
              .value(supplementConcept.getDisplay())
              .build());
    }

    // All explicit supplement designations become CSS-sourced designations.
    for (CodeSystem.ConceptDefinitionDesignationComponent designation :
        supplementConcept.getDesignation()) {
      designationManager.addDesignation(
          FhirDesignation.builder()
              .source(
                  Designation
                      .DesignationSource.CSS)
              .language(
                  designation.hasLanguage()
                      ? designation.getLanguageElement()
                      : new org.hl7.fhir.r4.model.CodeType(supplementLanguage))
              .use(designation.getUse())
              .value(designation.getValue())
              .build());
    }
  }

  /**
   * Finds a concept by code in a CodeSystem, searching recursively through nested concepts.
   *
   * @param codeSystem the CodeSystem to search
   * @param code the code to find
   * @return the matching concept, or {@code null} if not present
   */
  private static CodeSystem.ConceptDefinitionComponent findConcept(
      CodeSystem codeSystem, String code) {
    for (CodeSystem.ConceptDefinitionComponent concept : codeSystem.getConcept()) {
      CodeSystem.ConceptDefinitionComponent found = findConceptRecursive(concept, code);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private static CodeSystem.ConceptDefinitionComponent findConceptRecursive(
      CodeSystem.ConceptDefinitionComponent concept, String code) {
    if (code.equals(concept.getCode())) {
      return concept;
    }
    for (CodeSystem.ConceptDefinitionComponent child : concept.getConcept()) {
      CodeSystem.ConceptDefinitionComponent found = findConceptRecursive(child, code);
      if (found != null) {
        return found;
      }
    }
    return null;
  }
}
