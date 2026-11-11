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
package de.gematik.zts.termzilla.terminologies.snomed.scg;

import de.gematik.zts.termzilla.api.exceptions.InactiveConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.RuleValidationException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.service.MrcmRuleService;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.snomed.languages.scg.domain.model.Attribute;
import org.snomed.languages.scg.domain.model.AttributeGroup;
import org.snomed.languages.scg.domain.model.AttributeValue;
import org.snomed.languages.scg.domain.model.Expression;

/**
 * Processes SNOMED CT compositional grammar (SCG) expressions for term rendering and MRCM-based
 * validation.
 *
 * <p>The processor resolves human-readable terms for concept identifiers and verifies expression
 * structure and values against machine-readable concept model rules.
 */
@Slf4j
public class DemisSCGProcessor {

  private static final String CONCEPT_SNOMED_CT_CONCEPT = "138875005";
  private static final String CONCEPT_PHARMA_BIOLOGIC_PRODUCT = "373873005";
  private static final String CONCEPT_PHYSICAL_OBJECT = "260787004";

  // 723596005=All SNOMED CT content
  // 723594008=All precoordinated SNOMED CT content
  // 723593002=All new precoordinated SNOMED CT content
  // 723595009=All postcoordinated SNOMED CT content
  private static final Set<String> RELEVANT_CONTENT_TYPE_IDS =
      Stream.of("723596005", "723595009").collect(Collectors.toUnmodifiableSet());

  private final Snomed snomed;
  private final MrcmRuleService mrcmRuleService;

  // beinhaltet die ConceptIds des MRCMDomain RefSets
  private Set<String> domainConcepts;

  /**
   * Creates a processor bound to one SNOMED service facade.
   *
   * @param snomed SNOMED facade used for terminology lookups and rule access
   */
  public DemisSCGProcessor(Snomed snomed) {
    this.snomed = snomed;
    this.mrcmRuleService = snomed.getMrcmRuleService();
    this.initializeProcessor();
  }

  /** Initializes internal domain concept caches from MRCM domain rules. */
  private void initializeProcessor() {

    try {
      // Abrufen alle relevanten Domain-Rules aus dem entsprechenden RefSet
      // Versionen interessieren uns hier erstmal nicht, wir wollen einen Überblick
      // TODO: Versionen berücksichtigen
      // beinhaltet die im MRCMDomain RefSet abgebildeten Regeln zu Domain-Konzepten
      Set<MrcmDomainRule> domainRules = mrcmRuleService.getMrcmDomainRules(null, true);
      domainConcepts = new HashSet<>();

      // Aus den Rules extrahieren wir die Domain-Konzepte
      for (MrcmDomainRule rule : domainRules) {
        domainConcepts.add(rule.getReferencedComponentId());
        log.debug(
            "RefSet MRCM Domain - enthaltenes Domainkonzept: {}", rule.getReferencedComponentId());
      }
    } catch (Exception e) {
      log.warn("Exception: {}", e.getMessage());
    }
  }

  /**
   * Retrieves the fully specified term of a SNOMED CT concept.
   *
   * @param conceptId id/code of the concept
   * @param snomedReleaseVersion SNOMED release version used for term resolution
   * @return localized display value or fallback default display value
   * @throws UnknownConceptException if the concept cannot be resolved
   */
  private String getTerm(String conceptId, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException {

    // TODO: care about includeInactive
    // TODO: care about language

    SnomedConcept concept =
        snomed.getSnomedConceptWithDescription(conceptId, snomedReleaseVersion, false);
    String displayValue = concept.getDefaultDisplayValue(snomedReleaseVersion.getDefaultLanguage());

    // Sofern es keinen Display-Wert für die Sprache gibt, müssen wir wieder auf den
    // Originalausdruck zurückgreifen
    if (StringUtils.isEmpty(displayValue)) {
      displayValue = concept.getDefaultDisplayValue();
    }

    return displayValue;
  }

  /**
   * Checks whether an attribute cardinality constraint is met in the current context.
   *
   * @param attributeIdList list of all attribute identifiers in the current context
   * @param attributeId attribute identifier to validate
   * @param cardinalityConstraint cardinality constraint to evaluate (for example {@code 0..*})
   * @return {@code true} if the number of occurrences is within the configured bounds
   */
  private boolean isMatchingCardinalityConstraint(
      List<String> attributeIdList, String attributeId, String cardinalityConstraint) {

    int attributeCount = 0;

    // count how often the attribute is contained within the attributeList
    for (String attribute : attributeIdList) {
      if (attribute.contentEquals(attributeId)) {
        attributeCount++;
      }
    }

    if (StringUtils.isBlank(cardinalityConstraint)) {
      log.warn("Missing cardinality constraint for attribute {}", attributeId);
      return false;
    }

    // Cardinality format is expected as "lower..upper", where upper can be '*'.
    String[] boundaries = cardinalityConstraint.trim().split("\\.\\.", -1);
    if (boundaries.length != 2
        || StringUtils.isBlank(boundaries[0])
        || StringUtils.isBlank(boundaries[1])) {
      log.warn(
          "Invalid cardinality constraint '{}' for attribute {}",
          cardinalityConstraint,
          attributeId);
      return false;
    }

    int lowerBoundary;
    int upperBoundary;
    try {
      lowerBoundary = Integer.parseInt(boundaries[0].trim());
      String upperBoundaryString = boundaries[1].trim();
      if (upperBoundaryString.contentEquals("*")) {
        upperBoundary = Integer.MAX_VALUE;
      } else {
        upperBoundary = Integer.parseInt(upperBoundaryString);
      }
    } catch (NumberFormatException _) {
      log.warn(
          "Invalid numeric cardinality boundary '{}' for attribute {}",
          cardinalityConstraint,
          attributeId);
      return false;
    }

    if (lowerBoundary < 0 || upperBoundary < lowerBoundary) {
      log.warn(
          "Invalid cardinality range '{}' for attribute {}", cardinalityConstraint, attributeId);
      return false;
    }

    log.debug("{} {} {}..{}", attributeId, attributeCount, lowerBoundary, upperBoundary);

    // finally check if the counted number is within the defined range
    return (attributeCount >= lowerBoundary) && (attributeCount <= upperBoundary);
  }

  // TODO duplicate code

  /**
   * Filters domain rules by applicable domain concepts.
   *
   * @param rules candidate attribute-domain rules
   * @param domainConcepts domain concept identifiers that are valid in the current context
   * @return subset of rules whose domain id is contained in {@code domainConcepts}
   */
  private Set<MrcmAttributeDomainRule> getApplicableAttributeDomainRulesByDomainConcepts(
      Set<MrcmAttributeDomainRule> rules, Set<String> domainConcepts) {
    Set<MrcmAttributeDomainRule> applicableRules = new HashSet<>();
    for (MrcmAttributeDomainRule attributeDomainRule : rules) {
      String domainId = attributeDomainRule.getDomainId();
      if (domainConcepts.contains(domainId)) {
        applicableRules.add(attributeDomainRule);
      }
    }
    return applicableRules;
  }

  // TODO duplicate code

  /**
   * Filters selected attribute domain rules by content type.
   *
   * @param rules rules selected from the corresponding refset
   * @param applicableContentTypeIds content type identifiers relevant for the current use case
   * @return subset of rules matching one of the allowed content type identifiers
   */
  private Set<MrcmAttributeDomainRule> getApplicableAttributeDomainRulesByContentType(
      Set<MrcmAttributeDomainRule> rules, Set<String> applicableContentTypeIds) {

    Set<MrcmAttributeDomainRule> applicableRules = new HashSet<>();
    for (MrcmAttributeDomainRule attributeDomainRule : rules) {
      String contentTypeId = attributeDomainRule.getContentTypeId();
      if (applicableContentTypeIds.contains(contentTypeId)) {
        applicableRules.add(attributeDomainRule);
      }
    }
    return applicableRules;
  }

  /**
   * Removes inactive attribute domain rules.
   *
   * @param rules candidate attribute-domain rules
   * @return subset that is marked as active
   */
  private Set<MrcmAttributeDomainRule> removeInactiveRulesFromApplicableAttributeDomainRules(
      Set<MrcmAttributeDomainRule> rules) {
    Set<MrcmAttributeDomainRule> applicableRules = new HashSet<>();
    for (MrcmAttributeDomainRule attributeDomainRule : rules) {
      if (attributeDomainRule.getActive().contentEquals("1")) {
        applicableRules.add(attributeDomainRule);
      }
    }
    return applicableRules;
  }

  // TODO duplicate code

  /**
   * Filters selected attribute range rules by content type.
   *
   * @param rules rules selected from the corresponding refset
   * @param applicableContentTypeIds content type identifiers relevant for the current use case
   * @return subset of range rules that match the allowed content type identifiers
   */
  private Set<MrcmAttributeRangeRule> getApplicableAttributeRangeRulesByContentType(
      Set<MrcmAttributeRangeRule> rules, Set<String> applicableContentTypeIds) {

    Set<MrcmAttributeRangeRule> applicableRules = new HashSet<>();
    for (MrcmAttributeRangeRule attributeRangeRule : rules) {
      String contentTypeId = attributeRangeRule.getContentTypeId();
      if (applicableContentTypeIds.contains(contentTypeId)) {
        applicableRules.add(attributeRangeRule);
      }
    }
    return applicableRules;
  }

  // TODO duplicate code

  /**
   * Selects the latest effective version for each attribute range rule id.
   *
   * @param rules candidate attribute-range rules
   * @return map-reduced set containing only newest effective versions per id
   */
  private Set<MrcmAttributeRangeRule> getNewestApplicableAttributeRangeRule(
      Set<MrcmAttributeRangeRule> rules) {
    Map<String, MrcmAttributeRangeRule> applicableRulesMap = new HashMap<>();
    for (MrcmAttributeRangeRule rule : rules) {
      String id = rule.getId();
      String effectiveTime = rule.getEffectiveTime();

      // check if we already hava a rule with the given id in the map
      if (applicableRulesMap.containsKey(id)) {
        // check if we have a younger version that should replace the old one in the Map
        if (effectiveTime.compareTo(applicableRulesMap.get(id).getEffectiveTime()) > 0) {
          applicableRulesMap.put(id, rule);
        }
      } else {
        // no entry for this id so far, let's add it to the Map
        applicableRulesMap.put(id, rule);
      }
    }
    Set<MrcmAttributeRangeRule> applicableRules = new HashSet<>();
    for (MrcmAttributeRangeRule rule : applicableRulesMap.values()) {
      applicableRules.add(rule);
    }
    return applicableRules;
  }

  // TODO duplicate code

  /**
   * Selects the latest effective version for each attribute domain rule id.
   *
   * @param rules candidate attribute-domain rules
   * @return map-reduced set containing only newest effective versions per id
   */
  private Set<MrcmAttributeDomainRule> getNewestApplicableAttributeDomainRule(
      Set<MrcmAttributeDomainRule> rules) {
    Map<String, MrcmAttributeDomainRule> applicableRulesMap = new HashMap<>();
    for (MrcmAttributeDomainRule rule : rules) {
      String id = rule.getId();
      String effectiveTime = rule.getEffectiveTime();

      // check if we already hava a rule with the given id in the map
      if (applicableRulesMap.containsKey(id)) {
        // check if we have a younger version that should replace the old one in the Map
        if (effectiveTime.compareTo(applicableRulesMap.get(id).getEffectiveTime()) > 0) {
          applicableRulesMap.put(id, rule);
        }
      } else {
        // no entry for this id so far, let's add it to the Map
        applicableRulesMap.put(id, rule);
      }
    }
    return new HashSet<>(applicableRulesMap.values());
  }

  /**
   * returns all SNOMED Top Level Concepts (concepts below 138875005)
   *
   * @param snomedReleaseVersion SNOMED version that should be used
   * @return Set of relevant Top Level Concepts
   * @throws UnknownConceptException if a concept referenced by the expression is unknown
   */
  private Set<String> getTopLevelHierarchyConcepts(SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException {
    return snomed.getDirectChildren(CONCEPT_SNOMED_CT_CONCEPT, snomedReleaseVersion);
  }

  /**
   * Resolves domain concepts related to the provided focus concepts.
   *
   * <p>Deprecated note: Previous German JavaDoc kept for historical context: "Ermittelt für einen
   * Satz von Fokus-Konzepten die jeweils zugeordneten Domain-Konzepte".
   *
   * @param focusConcepts focus concepts to analyze
   * @param snomedReleaseVersion SNOMED release version used for ancestor lookup
   * @return set of related domain concept identifiers
   * @throws UnsupportedTerminologyException if the configured terminology is unsupported
   * @throws UnknownConceptException if a concept referenced by the expression is unknown
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private Set<String> getRelatedDomainConcepts(
      List<String> focusConcepts, SnomedReleaseVersion snomedReleaseVersion)
      throws UnsupportedTerminologyException, UnknownConceptException, InactiveConceptException {

    // Set mit Domänen-Konzepten, die für die spätere Auswahl der anzuwendenden Regeln relevant sind
    Set<String> applicableDomainConcepts = new HashSet<>();

    // Für jedes der Fokus-Konzepte suchen wir jetzt das passende Domain-Konzept. Dazu
    // lassen wir uns für jedes Fokus-Konzept seine Vorfahren liefern und prüfen, ob diese
    // im Set mit den Domain-Konzepten enthalten sind.
    for (String conceptId : focusConcepts) {
      // Vorfahren des Fokus-Konzeptes suchen
      Set<String> ancestorOrSelfConcepts =
          snomed.getAncestorsOrSelf(conceptId, snomedReleaseVersion);

      // das Fokus-Konzept selbst könnte ein Domain-Konzept sein, also nehmen wir es mit in die
      // Liste auf, sofern noch nicht enthalten
      ancestorOrSelfConcepts.add(conceptId);

      // Für jeden Eintrag im Set prüfen, ob er in der Liste der Domain-Konzepte enthalten ist.
      // Sobald
      // wir einen Eintrag haben, der ein Domain-Konzept ist, nehmen wir ihn in die Liste der
      // Regel-selektierenden Konzepte mit auf.
      for (String ancestor : ancestorOrSelfConcepts) {
        if (domainConcepts.contains(ancestor)) {
          applicableDomainConcepts.add(ancestor);
          log.debug(
              "Applicable Domain Concept: {} |{}|",
              ancestor,
              getTerm(ancestor, snomedReleaseVersion));
        }
      }
    }
    return applicableDomainConcepts;
  }

  /**
   * Checks whether all focus concepts belong to the same top-level hierarchy.
   *
   * <p>Deprecated note: Previous German JavaDoc kept for historical context: "Alle Fokuskonzepte in
   * einer Expression sollen eigentlich aus einer Top Level Hierarchie kommen. Es gibt nur wenige
   * Ausnahmefälle.".
   *
   * @param focusConcepts focus concepts from the expression
   * @param snomedReleaseVersion SNOMED release version used for hierarchy lookup
   * @return optional validation hint when an allowed cross-hierarchy exception is detected
   * @throws RuleValidationException if focus concepts violate hierarchy constraints
   */
  private String testForSameTopLevelHierachy(
      List<String> focusConcepts, SnomedReleaseVersion snomedReleaseVersion)
      throws RuleValidationException, UnknownConceptException {

    String validationHint = null;

    // Top Level Hierarchy Konzepte für diese SNOMED Version heraussuchen
    Set<String> topLevelHierarchyConceptSet = getTopLevelHierarchyConcepts(snomedReleaseVersion);

    // Top Level Hierarchy Konzepte der einzelnen Fokuskonzepte werden in diesem Set gesammelt
    Set<String> usedTopLevelHierarchyConceptSet = new HashSet<>();

    // über alle Fokuskonzepte iterieren
    for (String conceptId : focusConcepts) {
      // Laden der Vorfahren des jeweiligen Fokuskonzeptes
      Set<String> ancestorOrSelfConceptSet =
          snomed.getAncestorsOrSelf(conceptId, snomedReleaseVersion);
      for (String ancestorOrSelfConcept : ancestorOrSelfConceptSet) {
        // nur wenn einer der Vorfahren ein Top Level Hierarchy Konzept ist, nehmen wir es in die
        // Liste auf
        if (topLevelHierarchyConceptSet.contains(ancestorOrSelfConcept)) {
          usedTopLevelHierarchyConceptSet.add(ancestorOrSelfConcept);
        }
      }
    }

    // Sollte mehr als eine Top Level Hierarchy selektiert sein, haben wir vermutlich ein Problem,
    // um welches wir uns kümmern müssen.
    if (usedTopLevelHierarchyConceptSet.size() > 1) {
      if (usedTopLevelHierarchyConceptSet.size() == 2) {
        if (usedTopLevelHierarchyConceptSet.contains(CONCEPT_PHARMA_BIOLOGIC_PRODUCT)
            && usedTopLevelHierarchyConceptSet.contains(CONCEPT_PHYSICAL_OBJECT)) {
          // Hier haben wir einen der im SCG Konzept erwähnten Ausnahmefälle. Wir warnen den Nutzer
          // lediglich
          validationHint =
              "Die Fokuskonzepte kommen aus verschiedenen Top Level Hierarchien. "
                  + "Das ist untypisch, aber in dieser Konstellation möglich. Trotzdem bitte nochmal kritisch prüfen.";
          log.warn(validationHint);
        } else {
          // Mehr als zwei betroffene Top Level Hierachien werden wir wohl nie haben. Entsprechend
          // werfen wir eine Exception
          throw new RuleValidationException(
              "Die Fokuskonzepte kommen aus verschiedenen Top Level Hierarchien ("
                  + usedTopLevelHierarchyConceptSet
                  + "). Dies ist nur in bestimmten Ausnahmefällen zulässig, die anscheinend nicht zutreffen.");
        }
      } else {
        throw new RuleValidationException(
            "Die Fokuskonzepte kommen aus verschiedenen Top Level Hierarchien ("
                + usedTopLevelHierarchyConceptSet
                + "). Dies ist nur in bestimmten Ausnahmefällen zulässig, die anscheinend nicht zutreffen.");
      }
    }
    return validationHint;
  }

  /**
   * Checks focus concepts for cross-domain hierarchy issues.
   *
   * @param focusConcepts focus concepts from the expression
   * @param snomedReleaseVersion SNOMED release version used for hierarchy lookup
   * @return optional validation hint for non-critical exceptional cases
   * @throws RuleValidationException if hierarchy constraints are violated
   */
  private String checkFocusConcepts(
      List<String> focusConcepts, SnomedReleaseVersion snomedReleaseVersion)
      throws RuleValidationException, UnknownConceptException {

    // Although not stipulated by the syntax, the two concepts joined in this way
    // must (in nearly every case) come from the same top level hierarchy. A rare
    // exception to this rule is those expressions which refer to products that are
    // both drugs and devices, and are therefore descendants of both
    // 373873005 |pharmaceutical / biologic product| and 260787004 |physical object|.

    return testForSameTopLevelHierachy(focusConcepts, snomedReleaseVersion);
  }

  /**
   * Retrieves all applicable attribute domain rules for one attribute identifier.
   *
   * <p>Deprecated note: Previous German JavaDoc kept for historical context and folded into this
   * description.
   *
   * @param attributeId attribute identifier to validate
   * @param applicableDomainConcepts domain concepts derived from the current focus concepts
   * @param snomedReleaseVersion SNOMED release version used for rule lookup
   * @return filtered set of applicable attribute domain rules
   * @throws RuleValidationException if no applicable domain rules can be derived
   * @throws UnknownConceptException if the attribute concept cannot be resolved
   */
  private Set<MrcmAttributeDomainRule> getApplicableAttributeDomainRules(
      String attributeId,
      Set<String> applicableDomainConcepts,
      SnomedReleaseVersion snomedReleaseVersion)
      throws RuleValidationException, UnknownConceptException {

    // TODO ruleStrengthId berücksichtigen
    //  - 723597001=Mandatory concept model rule
    //  - 723598006=Optional concept model rule

    // select all Attribute Domain Rules that might be relevant for us based on the attributeId
    Set<MrcmAttributeDomainRule> applicableAttributeDomainRules =
        mrcmRuleService.getMrcmAttributeDomainRules(snomedReleaseVersion, attributeId);

    if (applicableAttributeDomainRules.isEmpty()) {
      throw new RuleValidationException(
          "Keine MrcmAttributeDomainRule im RefSet für die attributId '"
              + attributeId
              + "' gefunden. "
              + "Handelt es sich bei '"
              + getTerm(attributeId, snomedReleaseVersion)
              + "' überhaupt um ein Attribut?");
    }

    // filter Attribute Domain Rules by effective time - was actually done before while searching
    applicableAttributeDomainRules =
        getNewestApplicableAttributeDomainRule(applicableAttributeDomainRules);

    if (applicableAttributeDomainRules.isEmpty()) {
      throw new RuleValidationException(
          "Keine MrcmAttributeDomainRule nach zeitlicher Sortierung für attributeId '"
              + attributeId
              + "' gefunden. "
              + "Das sollte nicht passieren. Programmierer fragen!");
    }

    // filter the selected set to only contain the Rules that are relevant for postcoordinated
    // expressions
    applicableAttributeDomainRules =
        getApplicableAttributeDomainRulesByContentType(
            applicableAttributeDomainRules, RELEVANT_CONTENT_TYPE_IDS);

    if (applicableAttributeDomainRules.isEmpty()) {
      throw new RuleValidationException(
          "Keine MrcmAttributeDomainRule nach Filterung (Regeln für postkoordinierte Konzepte) für attributeId '"
              + attributeId
              + "' gefunden. "
              + "Das sollte nicht passieren. Programmierer fragen!");
    }

    // remove inactive rules from set
    applicableAttributeDomainRules =
        removeInactiveRulesFromApplicableAttributeDomainRules(applicableAttributeDomainRules);

    // filter the selected set to only contain the Rules that are relevant for the Domains of the
    // focus concepts
    applicableAttributeDomainRules =
        getApplicableAttributeDomainRulesByDomainConcepts(
            applicableAttributeDomainRules, applicableDomainConcepts);

    if (applicableAttributeDomainRules.isEmpty()) {
      throw new RuleValidationException(
          "Keine MrcmAttributeDomainRule nach Filterung (Fokuskonzept-Domänen) für attributeId '"
              + attributeId
              + "' gefunden. "
              + "Bitte prüfen Sie, ob das Attribut überhaupt für die verwendeten Fokuskonzepte bzw. deren Domäne genutzt werden darf. https://browser.ihtsdotools.org/mrcm/");
    }

    return applicableAttributeDomainRules;
  }

  /**
   * Retrieves all applicable attribute range rules for one attribute identifier.
   *
   * @param attributeId attribute identifier to validate
   * @param snomedReleaseVersion SNOMED release version used for rule lookup
   * @return filtered set of applicable attribute range rules
   */
  private Set<MrcmAttributeRangeRule> getApplicableAttributeRangeRules(
      String attributeId, SnomedReleaseVersion snomedReleaseVersion) {

    // TODO ruleStrengthId berücksichtigen
    //  - 723597001=Mandatory concept model rule
    //  - 723598006=Optional concept model rule

    // select all Attribute Range Rules that might be relevant for us based on the attributeId
    Set<MrcmAttributeRangeRule> applicableAttributeRangeRules =
        mrcmRuleService.getMrcmAttributeRangeRules(snomedReleaseVersion, attributeId);

    // filter Attribute Range Rules by effective time
    // actually this is already done before
    applicableAttributeRangeRules =
        getNewestApplicableAttributeRangeRule(applicableAttributeRangeRules);

    // filter the selected set to only contain the Rules that are relevant for
    // postcoordinated expressions or all expressions
    applicableAttributeRangeRules =
        getApplicableAttributeRangeRulesByContentType(
            applicableAttributeRangeRules, RELEVANT_CONTENT_TYPE_IDS);

    // remove rules that aren't active anymore
    applicableAttributeRangeRules =
        removeInactiveAttributeRangeRules(applicableAttributeRangeRules);

    return applicableAttributeRangeRules;
  }

  /**
   * Removes inactive attribute range rules.
   *
   * @param rules candidate attribute-range rules
   * @return subset that is marked as active
   */
  private Set<MrcmAttributeRangeRule> removeInactiveAttributeRangeRules(
      Set<MrcmAttributeRangeRule> rules) {
    Set<MrcmAttributeRangeRule> applicableRules = new HashSet<>();
    for (MrcmAttributeRangeRule attributeRangeRule : rules) {
      if (attributeRangeRule.getActive().contentEquals("1")) {
        applicableRules.add(attributeRangeRule);
      }
    }
    return applicableRules;
  }

  /**
   * Validates all attributes of the current expression context.
   *
   * @param attributeList attributes to validate
   * @param focusConcepts focus concepts that define the current domain context
   * @param isGrouped {@code true} if attributes belong to an explicit attribute group
   * @param snomedReleaseVersion SNOMED release version used for validation
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the configured terminology is unsupported
   * @throws RuleValidationException if MRCM constraints are violated
   * @throws SimpleExpressionConstraintException if expression-constraint parsing fails
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private void validateAttributes(
      List<Attribute> attributeList,
      List<String> focusConcepts,
      boolean isGrouped,
      SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException,
          UnsupportedTerminologyException,
          RuleValidationException,
          SimpleExpressionConstraintException,
          InactiveConceptException {

    if (attributeList == null) {
      return;
    }

    // relevante Domain-Konzepte, die das anzuwendende Regelwerk beeinflussen
    Set<String> applicableDomainConcepts =
        getRelatedDomainConcepts(focusConcepts, snomedReleaseVersion);

    validateNoDuplicateAttributes(attributeList);

    // Iterieren über alle Attribute der Liste, um diverse Regeln zu prüfen
    for (Attribute attribute : attributeList) {
      validateSingleAttribute(
          attribute, attributeList, applicableDomainConcepts, isGrouped, snomedReleaseVersion);
    }
  }

  /**
   * Ensures that no duplicate attribute-value combinations exist within one attribute collection.
   *
   * @param attributeList attributes to validate for duplicates
   * @throws RuleValidationException if duplicate attribute-value combinations are present
   */
  private void validateNoDuplicateAttributes(List<Attribute> attributeList)
      throws RuleValidationException {
    // es dürfen keine identischen Attribute (gleiche attributId mit gleichem attributValue)
    // existieren
    // deshalb geben wir die Attribute als String aus (attribute=attributeValue) und prüfen auf
    // Dopplungen
    Set<String> attributeStringSet = new HashSet<>();
    for (Attribute attribute : attributeList) {
      attributeStringSet.add(attribute.toString());
    }
    if (attributeStringSet.size() != attributeList.size()) {
      throw new RuleValidationException(
          "Attribute (innerhalb einer Gruppe) dürfen nicht identische Werte haben. Bitte überprüfen Sie die Expression nochmal.");
    }
  }

  /**
   * Validates one attribute against applicable domain and range rules.
   *
   * @param attribute attribute to validate
   * @param attributeList all attributes of the current context
   * @param applicableDomainConcepts domain concepts used to select domain rules
   * @param isGrouped whether attributes are currently validated in a grouped context
   * @param snomedReleaseVersion SNOMED release version used for validation
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the underlying terminology is not supported
   * @throws RuleValidationException if any domain or range rule is violated
   * @throws SimpleExpressionConstraintException if expression-constraint parsing fails
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private void validateSingleAttribute(
      Attribute attribute,
      List<Attribute> attributeList,
      Set<String> applicableDomainConcepts,
      boolean isGrouped,
      SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException,
          UnsupportedTerminologyException,
          RuleValidationException,
          SimpleExpressionConstraintException,
          InactiveConceptException {

    log.debug("Checking: " + attribute);

    // get the attributeId from the attribute
    String attributeId = attribute.getAttributeId();

    // get the attributeValue from the attribute
    AttributeValue attributeValue = attribute.getAttributeValue();

    // Selektieren und Filtern der anzuwendenden AttributeDomainRules
    // Prinzipiell werden auch hier Exceptions geworfen, z.B. wenn keine Regeln gefunden werden
    Set<MrcmAttributeDomainRule> applicableAttributeDomainRules =
        getApplicableAttributeDomainRules(
            attributeId, applicableDomainConcepts, snomedReleaseVersion);

    // sollten keine Regeln gefunden worden sein, haben wir ein Problem
    // TODO: prüfen wir das doppelt?
    if (applicableAttributeDomainRules.isEmpty()) {
      String message =
          MessageFormat.format(
              "Für das Attribut {0} wurden keine anwendbaren "
                  + "AttributeDomainRules gefunden. Das sollte nicht passieren.",
              attributeId);
      log.error(message);
      throw new RuleValidationException(message);
    }

    validateAttributeAgainstDomainRules(
        attribute, attributeId, attributeList, isGrouped, applicableAttributeDomainRules);

    // Selektieren und Filtern der anzuwendenden AttributeRangeRules
    Set<MrcmAttributeRangeRule> applicableAttributeRangeRules =
        getApplicableAttributeRangeRules(attributeId, snomedReleaseVersion);

    validateAttributeAgainstRangeRules(
        attributeValue, attributeId, snomedReleaseVersion, applicableAttributeRangeRules);
  }

  /**
   * Applies domain rules including grouping and cardinality checks for one attribute.
   *
   * @param attribute attribute currently being validated
   * @param attributeId identifier of the attribute
   * @param attributeList all attributes of the current context
   * @param isGrouped whether attributes are currently validated in a grouped context
   * @param applicableAttributeDomainRules selected domain rules for the attribute
   * @throws RuleValidationException if a domain rule is violated
   */
  private void validateAttributeAgainstDomainRules(
      Attribute attribute,
      String attributeId,
      List<Attribute> attributeList,
      boolean isGrouped,
      Set<MrcmAttributeDomainRule> applicableAttributeDomainRules)
      throws RuleValidationException {

    // Anwenden des Regelwerkes (AttributeDomainRules) - jede Regel wird geprüft (vermutlich ist
    // es aber immer nur eine ...)
    for (MrcmAttributeDomainRule rule : applicableAttributeDomainRules) {

      log.debug("Validating '{}' against rule: {}", attribute, rule.toString());

      // TODO: UNBEDINGT DISKUTIEREN
      // We take the easy route and demand that attributes that are marked as "grouped" must be
      // in a group
      // BUT: the SCG document states at the end of section 6.4 that this is not always the case
      // and curly braces may be omitted
      if (rule.getGrouped().contentEquals("1") && !isGrouped) {
        String message =
            MessageFormat.format(
                "Das Attribut {0} ist in den MRCM-AttributeDomainRules als 'grouped' gekennzeichnet. "
                    + "Andere Validatoren erwarten für diesen Fall z.T. eine Einbettung in eine Attibutgruppe. "
                    + "Eigentlich sollte es auch anders gehen (vgl. SCG document section 6.4), aber die Spezifikation ist hier nicht sonderlich klar. Es wäre also besser, an dieser Stelle '{}' zu verwenden. Sicher ist sicher.",
                attributeId);
        log.error(message);
        // TODO throw an RuleValidationException
      }
      String cardinalityConstraint;
      if (isGrouped) {
        cardinalityConstraint = rule.getAttributeInGroupCardinality();
      } else {
        cardinalityConstraint = rule.getAttributeCardinality();
      }

      // Prüfen der Kardinalitätsregel
      List<String> attributeIdList = new ArrayList<>();
      for (Attribute attr : attributeList) {
        attributeIdList.add(attr.getAttributeId());
      }

      if (!isMatchingCardinalityConstraint(attributeIdList, attributeId, cardinalityConstraint)) {
        String message =
            MessageFormat.format(
                "Die attribute(InGroup)Cardinality ({0}) für das Attribut {1} wird nicht respektiert",
                cardinalityConstraint, attributeId);
        log.error(message);
        throw new RuleValidationException(message);
      }
    }
  }

  /**
   * Applies range rules for one attribute value, including nested expression validation.
   *
   * @param attributeValue value of the attribute to validate
   * @param attributeId identifier of the attribute
   * @param snomedReleaseVersion SNOMED release version used for validation
   * @param applicableAttributeRangeRules selected range rules for the attribute
   * @throws RuleValidationException if a range rule is violated
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws SimpleExpressionConstraintException if expression-constraint parsing fails
   * @throws InactiveConceptException if a referenced concept is inactive
   * @throws UnsupportedTerminologyException if the underlying terminology is not supported
   */
  private void validateAttributeAgainstRangeRules(
      AttributeValue attributeValue,
      String attributeId,
      SnomedReleaseVersion snomedReleaseVersion,
      Set<MrcmAttributeRangeRule> applicableAttributeRangeRules)
      throws RuleValidationException,
          UnknownConceptException,
          SimpleExpressionConstraintException,
          InactiveConceptException,
          UnsupportedTerminologyException {

    // Anwenden des Regelwerkes (AttributeRangeRules)
    for (MrcmAttributeRangeRule rule : applicableAttributeRangeRules) {
      log.debug(rule.toString());

      String rangeConstraint = rule.getRangeConstraint();

      // Die Prüfung der Range erfolgt gegen den Attributwert
      // Der Attributwert kann selbst eine Expression sein oder ein einfaches Konzept
      if (attributeValue.isNested()) {

        // Attributwert ist eine Expression
        log.warn("Checking of nested expressions is currently incomplete");

        Expression nestedExpression = attributeValue.getNestedExpression();

        // Prüfen der Fokuskonzepte der Expression gegen die Range-Constraints
        // Annahme: alle Fokuskonzepte müssen in der Range enthalten sein ...
        List<String> nestedExpressionFocusConcepts = nestedExpression.getFocusConcepts();
        for (String focusConcept : nestedExpressionFocusConcepts) {
          boolean isValidAttributeValue =
              snomed.checkIfConceptIsIncludedInExpressionConstraintResult(
                  focusConcept, rangeConstraint, snomedReleaseVersion);

          if (!isValidAttributeValue) {
            String message =
                "Attribute value '"
                    + focusConcept
                    + "' (as focus concept of a nested expression) is not included in defined range ("
                    + rangeConstraint
                    + ") for attribute '"
                    + attributeId
                    + "'";
            log.error(message);
            throw new RuleValidationException(message);
          }
        }

        // Prüfen der nested expression (rekursiver Aufruf)
        log.debug("---------------");
        validateAgainstMRCM(nestedExpression, snomedReleaseVersion);
        log.debug("---------------");

      } else {
        // Attributwert ist einfaches Konzept
        String attributeValueConceptId = attributeValue.getConceptId();

        // Überprüfen, ob der Attributwert in der durch die Regel definierte Range zu finden ist
        boolean isValidAttributeValue =
            snomed.checkIfConceptIsIncludedInExpressionConstraintResult(
                attributeValueConceptId, rangeConstraint, snomedReleaseVersion);

        if (!isValidAttributeValue) {
          String message =
              "Attribute value '"
                  + attributeValueConceptId
                  + "' is not included in defined range ("
                  + rangeConstraint
                  + ") for attribute '"
                  + attributeId
                  + "'";
          log.error(message);
          throw new RuleValidationException(message);
        }
      }
    }
  }

  // TODO Requirement C.1: Attribute domains
  // The MRCM must be able to specify the set of concepts to which a given attribute may be
  // applied.

  // TODO Requirement C.2: Attribute ranges
  // The MRCM must be able to specify the set of concepts which may be used as the value for a
  // given attribute.

  // TODO Requirement C.3: Attribute cardinality
  // The MRCM must be able to specify the minimum and maximum number of times that a given
  // attribute may appear
  // in a concept definition. Additionally, it must be able to specify the minimum and maximum
  // number of times that a
  // given attribute may appear in each relationship group in a concept definition.

  // TODO Requirement C.4: Grouping
  // The MRCM must be able to specify whether an attribute may or may not belong to a relationship
  // group.

  // TODO Requirement C.5: Rule strength
  // The MRCM should indicate the strength with which each rule should be applied – for example,
  // whether a rule is
  // mandatory (resulting in an error), or optional (resulting in a warning).

  // TODO Requirement C.6: Rule scope
  // There should be a clear specification of which concept model rules apply to a given SNOMED CT
  // module.

  /**
   * Validates an expression against SNOMED MRCM rules.
   *
   * <p>Deprecated note: Previous German JavaDoc kept for historical context: "Validierung einer
   * Expression gegen das von SNOMED vorgegebene Regelwerk (MRCM - Machine Readable Concept Model)".
   *
   * @param expression expression to validate
   * @param snomedReleaseVersion SNOMED release version used for rule and concept lookup
   * @return optional validation hint for tolerated but uncommon constellation checks
   * @throws UnknownConceptException if a concept within the expression is unknown
   * @throws RuleValidationException if rules are violated
   * @throws SimpleExpressionConstraintException if expression-constraint parsing fails
   * @throws InactiveConceptException if a referenced concept is inactive
   * @throws UnsupportedTerminologyException if the terminology is unsupported
   */
  public String validateAgainstMRCM(
      Expression expression, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException,
          RuleValidationException,
          SimpleExpressionConstraintException,
          InactiveConceptException,
          UnsupportedTerminologyException {

    String validationHint = "";

    // TODO correct handling of SNOMED versions

    // Fokus-Konzepte der Expression
    List<String> focusConcepts = expression.getFocusConcepts();

    // Bis auf wenige Ausnahmen müssen Fokuskonzepte immer aus einer Domain stammen.
    // https://confluence.ihtsdotools.org/display/DOCSCG/6.2+Multiple+Focus+Concepts
    // Ist dies nicht der Fall, warnen wir den Nutzer zumindest und ergänzen die
    // Validierungshinweise ...
    log.debug("Checking focus concepts");
    String resultHint = checkFocusConcepts(focusConcepts, snomedReleaseVersion);
    if (resultHint != null) {
      validationHint += resultHint;
    }

    // Expressions können Attribute beinhalten, die auf den ersten Blick nicht gruppiert sind.
    // Diese Attribute testen wir zuerst. Im Prinzip müssen sie aber wie eine Gruppe behandelt
    // werden
    // vgl. https://confluence.ihtsdotools.org/display/DOCSCG/6.4+Expressions+With+Attribute+Groups
    log.debug("Checking non grouped attributes");
    validateAttributes(expression.getAttributes(), focusConcepts, false, snomedReleaseVersion);

    // Jetzt kümmern wir uns um die expliziten Attributgruppen, von denen es beliebig viele geben
    // kann
    log.debug("Checking grouped attributes");
    if (expression.getAttributeGroups() != null) {
      for (AttributeGroup group : expression.getAttributeGroups()) {
        validateAttributes(group.getAttributes(), focusConcepts, true, snomedReleaseVersion);
      }
    }

    // Nachdem wir die Attributgruppen einzeln betrachtet haben, müssen wir für bestimmte
    // Kardinalitäts-Constraints
    // noch einen übergreifenden Blick auf die Attributverwendung werfen

    // check complete attribute list against rules
    List<String> allAttributes = getFullAttributeList(expression);

    log.debug("Checking all attributes against cardinality constraints");
    if (allAttributes != null) {
      for (String attributeId : allAttributes) {

        log.debug("Checking: " + attributeId + " " + getTerm(attributeId, snomedReleaseVersion));

        // relevante Domain-Konzepte, die das anzuwendende Regelwerk beeinflussen
        Set<String> applicableDomainConcepts =
            getRelatedDomainConcepts(focusConcepts, snomedReleaseVersion);

        // Selektieren und Filtern der anzuwendenden AttributeDomainRules
        Set<MrcmAttributeDomainRule> applicableAttributeDomainRules =
            getApplicableAttributeDomainRules(
                attributeId, applicableDomainConcepts, snomedReleaseVersion);

        // Anwenden des Regelwerkes (AttributeDomainRules)
        for (MrcmAttributeDomainRule rule : applicableAttributeDomainRules) {
          log.debug(rule.toString());

          // diesmal nutzen wir das andere Constraint (attributeCardinality)
          String cardinalityConstraint = rule.getAttributeCardinality();

          // Prüfen der Kardinalitätsregel
          if (!isMatchingCardinalityConstraint(allAttributes, attributeId, cardinalityConstraint)) {
            String message =
                "attributeCardinality ("
                    + cardinalityConstraint
                    + ") not respected for attribute: "
                    + attributeId;
            log.error(message);
            throw new RuleValidationException(message);
          }
        }
      }
    }
    return validationHint;
  }

  /**
   * Returns all attribute identifiers used by the expression across grouped and ungrouped sections.
   *
   * @param expression expression containing attributes and attribute groups
   * @return flattened list of attribute identifiers with per-group duplicates removed
   */
  private List<String> getFullAttributeList(Expression expression) {

    List<String> allAttributes = new ArrayList<>();
    Set<String> attributesInGroup;

    // Auswahl aller Attribute, die in keiner Gruppe stecken
    // wir behandeln diese trotzdem so, als ob sie gemeinsam eine Gruppe darstellen
    // https://confluence.ihtsdotools.org/display/DOCSCG/6.4+Expressions+With+Attribute+Groups
    if (expression.getAttributes() != null) {
      attributesInGroup = new HashSet<>();
      // Iterate over attributes and add attributeIds to set. This will illiminate duplicates
      for (Attribute attribute : expression.getAttributes()) {
        attributesInGroup.add(attribute.getAttributeId());
      }
      // add all attributes to List
      for (String attributeId : attributesInGroup) {
        allAttributes.add(attributeId);
      }
    }

    // Jetzt machen wir das Ganze nochmal für alle anderen Attributgruppen
    if (expression.getAttributeGroups() != null) {
      for (AttributeGroup group : expression.getAttributeGroups()) {
        attributesInGroup = new HashSet<>();
        // Iterate over attributes and add attributeIds to set. This will illiminate duplicates
        for (Attribute attribute : group.getAttributes()) {
          attributesInGroup.add(attribute.getAttributeId());
        }
        // add all attributes to List
        for (String attributeId : attributesInGroup) {
          allAttributes.add(attributeId);
        }
      }
    }
    return allAttributes;
  }

  /**
   * Serializes an expression into SCG term-string form.
   *
   * <p>Deprecated note: Previous German JavaDoc kept for historical context: "Über die Methode
   * können wir uns zu einer conceptId einen menschenlesbaren Term holen".
   *
   * @param expression expression to serialize
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @return term string representation of the expression
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the configured terminology is not supported
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  public String toTermString(Expression expression, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {

    StringBuilder builder = new StringBuilder();

    appendDefinitionStatus(builder, expression);
    appendFocusConcepts(builder, expression.getFocusConcepts(), snomedReleaseVersion);

    List<Attribute> attributes = expression.getAttributes();
    Set<AttributeGroup> attributeGroups = expression.getAttributeGroups();
    appendRefinementSeparator(builder, attributes, attributeGroups);
    appendAttributes(builder, attributes, snomedReleaseVersion);
    appendSeparatorBetweenAttributesAndGroups(builder, attributes, attributeGroups);
    appendAttributeGroups(builder, attributeGroups, snomedReleaseVersion);

    return builder.toString();
  }

  /**
   * Appends the definition status prefix of an expression if present.
   *
   * @param builder target builder receiving the serialized output
   * @param expression expression containing optional definition status
   */
  private void appendDefinitionStatus(StringBuilder builder, Expression expression) {
    if (expression.getDefinitionStatus() != null) {
      builder.append(expression.getDefinitionStatus().toString()).append(" ");
    }
  }

  /**
   * Appends focus concepts in SCG term-string format.
   *
   * @param builder target builder receiving the serialized output
   * @param focusConcepts focus concept IDs of the expression
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   */
  private void appendFocusConcepts(
      StringBuilder builder, List<String> focusConcepts, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException {

    if (focusConcepts != null) {
      for (int i = 0; i < focusConcepts.size(); ++i) {
        if (i > 0) {
          builder.append(" + ");
        }

        String focusConceptId = focusConcepts.get(i);
        builder.append(focusConceptId);
        builder.append(" | ").append(getTerm(focusConceptId, snomedReleaseVersion)).append(" |");
      }
    }
  }

  /**
   * Appends the refinement separator if at least one refinement section exists.
   *
   * @param builder target builder receiving the serialized output
   * @param attributes ungrouped attributes of the expression
   * @param attributeGroups grouped attributes of the expression
   */
  private void appendRefinementSeparator(
      StringBuilder builder, List<Attribute> attributes, Set<AttributeGroup> attributeGroups) {
    if ((attributes != null && !attributes.isEmpty())
        || (attributeGroups != null && !attributeGroups.isEmpty())) {
      builder.append(" : ");
    }
  }

  /**
   * Appends ungrouped attributes as a comma-separated list.
   *
   * @param builder target builder receiving the serialized output
   * @param attributes ungrouped attributes of the expression
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the underlying terminology is not supported
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private void appendAttributes(
      StringBuilder builder, List<Attribute> attributes, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {

    if (attributes != null) {
      for (int i = 0; i < attributes.size(); ++i) {
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(attributeToString(attributes.get(i), snomedReleaseVersion));
      }
    }
  }

  /**
   * Appends a delimiter between ungrouped attributes and attribute groups when required.
   *
   * @param builder target builder receiving the serialized output
   * @param attributes ungrouped attributes of the expression
   * @param attributeGroups grouped attributes of the expression
   */
  private void appendSeparatorBetweenAttributesAndGroups(
      StringBuilder builder, List<Attribute> attributes, Set<AttributeGroup> attributeGroups) {
    if (attributes != null && attributeGroups != null && !attributeGroups.isEmpty()) {
      builder.append(", ");
    }
  }

  /**
   * Appends attribute groups as a comma-separated list.
   *
   * @param builder target builder receiving the serialized output
   * @param attributeGroups grouped attributes of the expression
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the underlying terminology is not supported
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private void appendAttributeGroups(
      StringBuilder builder,
      Set<AttributeGroup> attributeGroups,
      SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {

    if (attributeGroups != null) {
      Iterator<AttributeGroup> iterator = attributeGroups.iterator();
      while (iterator.hasNext()) {
        AttributeGroup attributeGroup = iterator.next();
        builder.append(attributeGroupToString(attributeGroup, snomedReleaseVersion));
        if (iterator.hasNext()) {
          builder.append(", ");
        }
      }
    }
  }

  /**
   * Serializes a single attribute to SCG term-string form.
   *
   * @param attribute attribute to serialize
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @return term string representation of the attribute
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the configured terminology is not supported
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private String attributeToString(Attribute attribute, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
    StringBuilder builder = new StringBuilder();
    if (attribute.getAttributeId() != null) {
      builder.append(attribute.getAttributeId());
      builder.append(" | " + getTerm(attribute.getAttributeId(), snomedReleaseVersion) + " |");
      builder.append(" = ");
    }

    if (attribute.getAttributeValue() != null) {

      if (attribute.getAttributeValue().getConceptId() != null) {
        builder.append(attribute.getAttributeValue().getConceptId());
        builder.append(
            " | "
                + getTerm(attribute.getAttributeValue().getConceptId(), snomedReleaseVersion)
                + " |");
      }

      if (attribute.getAttributeValue().getNestedExpression() != null) {
        builder
            .append("( ")
            .append(
                toTermString(
                    attribute.getAttributeValue().getNestedExpression(), snomedReleaseVersion))
            .append(" )");
      }
    }
    return builder.toString();
  }

  /**
   * Serializes one attribute group to SCG term-string form.
   *
   * @param attributeGroup attribute group to serialize
   * @param snomedReleaseVersion SNOMED release version used for term lookup
   * @return term string representation of the attribute group
   * @throws UnknownConceptException if a referenced concept cannot be resolved
   * @throws UnsupportedTerminologyException if the configured terminology is not supported
   * @throws InactiveConceptException if a referenced concept is inactive
   */
  private String attributeGroupToString(
      AttributeGroup attributeGroup, SnomedReleaseVersion snomedReleaseVersion)
      throws UnknownConceptException, UnsupportedTerminologyException, InactiveConceptException {
    StringBuilder builder = new StringBuilder();
    builder.append("{");
    List<Attribute> attributes = attributeGroup.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.size(); ++i) {
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(attributeToString(attributes.get(i), snomedReleaseVersion));
      }
    }
    builder.append("}");
    return builder.toString();
  }
}
