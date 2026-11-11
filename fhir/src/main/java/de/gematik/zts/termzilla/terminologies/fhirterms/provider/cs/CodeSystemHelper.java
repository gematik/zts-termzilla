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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs;

import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CHILD;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CODE;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_CONCEPT;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DEFINITION;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_DISPLAY;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_PARENT;
import static de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties.PROPERTY_NAME_STATUS;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.ResourceStripHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;

public class CodeSystemHelper {

  private CodeSystemHelper() {
    // Private constructor to prevent instantiation
  }

  // TODO: nochmal wegen ACTIVE überlegen
  /**
   * Implicit concept properties defined by the FHIR specification that must be supported even when
   * not explicitly declared in a {@link CodeSystem}. Used by {@link #getProperty(CodeSystem,
   * String)} as a fallback when no explicit property definition matches.
   *
   * @see <a href="https://hl7.org/fhir/R4/codesystem.html#defined-props">FHIR Defined
   *     Properties</a>
   */
  private static final Map<String, CodeSystem.PropertyComponent> IMPLICIT_PROPERTIES =
      Map.of(
          PROPERTY_NAME_CODE,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_CODE)
                  .setType(CodeSystem.PropertyType.CODE),
          PROPERTY_NAME_CONCEPT,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_CONCEPT)
                  .setType(CodeSystem.PropertyType.CODE),
          PROPERTY_NAME_DISPLAY,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_DISPLAY)
                  .setType(CodeSystem.PropertyType.STRING),
          PROPERTY_NAME_DEFINITION,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_DEFINITION)
                  .setType(CodeSystem.PropertyType.STRING),
          PROPERTY_NAME_STATUS,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_STATUS)
                  .setType(CodeSystem.PropertyType.CODE),
          PROPERTY_NAME_PARENT,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_PARENT)
                  .setType(CodeSystem.PropertyType.CODE),
          PROPERTY_NAME_CHILD,
              new CodeSystem.PropertyComponent()
                  .setCode(PROPERTY_NAME_CHILD)
                  .setType(CodeSystem.PropertyType.CODE));

  /**
   * Looks up a single concept by code in the given {@link CodeSystem}.
   *
   * <p>Uses {@link #getCachedAllConceptsAsMap(CodeSystem)} for O(1) lookups. Case sensitivity is
   * derived from {@link CodeSystem#getCaseSensitive()}.
   *
   * @param codeSystem the CodeSystem to search in; must not be {@code null}
   * @param code the code to look up
   * @return the matching {@link CodeSystem.ConceptDefinitionComponent}; never {@code null}
   * @throws UnknownConceptException if no concept with the given code exists in the CodeSystem
   */
  public static CodeSystem.ConceptDefinitionComponent findCodeInCodeSystem(
      CodeSystem codeSystem, String code) throws UnknownConceptException {
    boolean isCaseSensitive = codeSystem.hasCaseSensitive() && codeSystem.getCaseSensitive();
    Map<String, ConceptDefinitionComponent> conceptsAsMap = getCachedAllConceptsAsMap(codeSystem);

    // If the CodeSystem is not case-sensitive, we need to lower-case the code for lookup
    String lookupCode = isCaseSensitive ? code : code.toLowerCase();
    ConceptDefinitionComponent result = conceptsAsMap.get(lookupCode);
    if (result == null) {
      throw new UnknownConceptException(
          "Code '"
              + code
              + "' not found in CodeSystem '"
              + codeSystem.getUrl()
              + (codeSystem.hasVersion() ? "|" + codeSystem.getVersion() : "")
              + "'");
    }
    return result;
  }

  /**
   * Recursively flattens a concept hierarchy into a map keyed by concept code.
   *
   * <p>This is the internal recursion helper for {@link #getCachedAllConceptsAsMap(CodeSystem)}. It
   * traverses sub-concepts at any depth and adds each visited concept to the resulting map.
   *
   * <p>When {@code isCaseSensitive} is {@code false}, all keys are stored in lower case so that
   * lookups can be performed with a lower-cased query code without additional branching.
   *
   * @param codeSystemConcepts the concepts at the current hierarchy level
   * @param isCaseSensitive whether the CodeSystem is case-sensitive; if {@code false}, map keys are
   *     stored in lower case
   * @return a map from (normalised) concept code to its {@link ConceptDefinitionComponent}
   */
  private static Map<String, ConceptDefinitionComponent> getAllConceptsAsMap(
      List<CodeSystem.ConceptDefinitionComponent> codeSystemConcepts, boolean isCaseSensitive) {

    Map<String, ConceptDefinitionComponent> allConcepts = new HashMap<>();

    for (var concept : codeSystemConcepts) {
      if (!isCaseSensitive) {
        allConcepts.put(concept.getCode().toLowerCase(), concept);
      } else {
        allConcepts.put(concept.getCode(), concept);
      }
      // recursively call method if concept has sub concepts
      if (concept.hasConcept()) {
        allConcepts.putAll(getAllConceptsAsMap(concept.getConcept(), isCaseSensitive));
      }
    }
    return allConcepts;
  }

  /**
   * Recursively flattens a concept hierarchy into the provided accumulator list.
   *
   * <p>This is the internal recursion helper for {@link #getCachedAllConceptsAsList(CodeSystem)}.
   * It traverses sub-concepts at any depth and appends each visited concept to {@code allConcepts}.
   *
   * @param codeSystemConcepts the concepts at the current hierarchy level
   * @param allConcepts the accumulator list that collects all visited concepts
   */
  private static void getAllConceptsAsList(
      List<CodeSystem.ConceptDefinitionComponent> codeSystemConcepts,
      List<CodeSystem.ConceptDefinitionComponent> allConcepts) {
    for (var concept : codeSystemConcepts) {
      allConcepts.add(concept);
      // recursively call method if concept has sub concepts
      if (concept.hasConcept()) {
        getAllConceptsAsList(concept.getConcept(), allConcepts);
      }
    }
  }

  /**
   * Returns a cached, unmodifiable list of all concepts (including nested sub-concepts) for the
   * given {@link CodeSystem}.
   *
   * <p>On the first invocation for a given CodeSystem instance, the full concept hierarchy is
   * flattened recursively and stored as {@link ResourceUserData#USERDATA_CONCEPTS_AS_LIST} user
   * data. Subsequent calls return the cached list without re-traversal.
   *
   * @param codeSystem the CodeSystem whose concepts should be flattened
   * @return an unmodifiable list of all concepts at any hierarchy depth
   */
  public static List<ConceptDefinitionComponent> getCachedAllConceptsAsList(CodeSystem codeSystem) {

    if (codeSystem.hasUserData(ResourceUserData.USERDATA_CONCEPTS_AS_LIST)) {
      @SuppressWarnings("unchecked")
      List<ConceptDefinitionComponent> cached =
          (List<ConceptDefinitionComponent>)
              codeSystem.getUserData(ResourceUserData.USERDATA_CONCEPTS_AS_LIST);
      return cached;
    }

    List<ConceptDefinitionComponent> allConcepts = new ArrayList<>();
    getAllConceptsAsList(codeSystem.getConcept(), allConcepts);

    List<ConceptDefinitionComponent> unmodifiable = Collections.unmodifiableList(allConcepts);
    codeSystem.setUserData(ResourceUserData.USERDATA_CONCEPTS_AS_LIST, unmodifiable);
    return unmodifiable;
  }

  /**
   * Returns a cached, unmodifiable map of all concepts (including nested sub-concepts) for the
   * given {@link CodeSystem}, keyed by concept code.
   *
   * <p>On the first invocation for a given CodeSystem instance, the full concept hierarchy is
   * flattened recursively and stored as {@link ResourceUserData#USERDATA_CONCEPTS_AS_MAP} user
   * data. Subsequent calls return the cached map without re-traversal.
   *
   * <p>Case sensitivity is derived from {@link CodeSystem#getCaseSensitive()}. When the CodeSystem
   * is not case-sensitive, map keys are normalised to lower case.
   *
   * @param codeSystem the CodeSystem whose concepts should be flattened into a map
   * @return an unmodifiable map from (normalised) concept code to its {@link
   *     ConceptDefinitionComponent}
   */
  public static Map<String, ConceptDefinitionComponent> getCachedAllConceptsAsMap(
      CodeSystem codeSystem) {

    if (codeSystem.hasUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP)) {
      @SuppressWarnings("unchecked")
      Map<String, ConceptDefinitionComponent> cached =
          (Map<String, ConceptDefinitionComponent>)
              codeSystem.getUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP);
      return cached;
    }

    boolean isCaseSensitive = codeSystem.hasCaseSensitive() && codeSystem.getCaseSensitive();
    Map<String, ConceptDefinitionComponent> conceptsAsMap =
        getAllConceptsAsMap(codeSystem.getConcept(), isCaseSensitive);

    Map<String, ConceptDefinitionComponent> unmodifiable =
        Collections.unmodifiableMap(conceptsAsMap);
    codeSystem.setUserData(ResourceUserData.USERDATA_CONCEPTS_AS_MAP, unmodifiable);
    return unmodifiable;
  }

  // TODO what if a concept is deprecated / retired --> are sub-concepts automatically retired?
  public static boolean isConceptActive(CodeSystem.ConceptDefinitionComponent concept) {

    // determine if concept status is active
    boolean isActive = true;
    if (concept.hasProperty()) {
      // The HL7 defined code systems (whether defined by the FHIR project, the V3 framework, or
      // the V2 standard) all use this property to define their status. HL7 uses "active" (and
      // considers this the default status), and deprecated and retired (= inactive)
      isActive =
          concept.getProperty().stream()
              .noneMatch(
                  conceptPropertyComponent ->
                      (conceptPropertyComponent.getCode().equals("status")
                              || conceptPropertyComponent
                                  .getCode()
                                  .equals("http://hl7.org/fhir/concept-properties#status"))
                          && (conceptPropertyComponent
                                  .getValueCodeType()
                                  .getCode()
                                  .equals("retired")
                              || conceptPropertyComponent
                                  .getValueCodeType()
                                  .getCode()
                                  .equals("deprecated")));
    }
    return isActive;
  }

  /**
   * Creates a stripped version of a CodeSystem by removing concepts while preserving metadata and
   * user data.
   *
   * @param codeSystem The original CodeSystem
   * @return A stripped version without concepts
   */
  public static CodeSystem createStrippedVersion(CodeSystem codeSystem) {
    return ResourceStripHelper.createStrippedVersion(codeSystem, cs -> cs.copy().setConcept(null));
  }

  /**
   * Resolves a property definition for the given property code, checking explicit declarations on
   * the {@link CodeSystem} first, then falling back to {@link #IMPLICIT_PROPERTIES}.
   *
   * @param cs the CodeSystem to search in
   * @param propertyCode the code of the property to find
   * @return the matching property component, or empty if neither explicitly declared nor implicitly
   *     defined
   */
  public static Optional<CodeSystem.PropertyComponent> getProperty(
      CodeSystem cs, String propertyCode) {

    // Prüfen, auf explizit definierte Properties
    Optional<CodeSystem.PropertyComponent> explicitProperty =
        cs.getProperty().stream()
            .filter(propertyComponent -> propertyComponent.getCode().equals(propertyCode))
            .findFirst();

    if (explicitProperty.isPresent()) {
      return explicitProperty;
    }

    // Prüfen, auf implizit definierte Properties
    return Optional.ofNullable(IMPLICIT_PROPERTIES.get(propertyCode));
  }
}
