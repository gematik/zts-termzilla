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

import static de.gematik.zts.termzilla.terminologies.snomed.scg.DemisScgProcessorTestDataHelper.attribute;
import static de.gematik.zts.termzilla.terminologies.snomed.scg.DemisScgProcessorTestDataHelper.conceptValue;
import static de.gematik.zts.termzilla.terminologies.snomed.scg.DemisScgProcessorTestDataHelper.conceptWithDisplay;
import static de.gematik.zts.termzilla.terminologies.snomed.scg.DemisScgProcessorTestDataHelper.domainRule;
import static de.gematik.zts.termzilla.terminologies.snomed.scg.DemisScgProcessorTestDataHelper.nestedExpressionValue;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.defaultCoreVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.terminologies.snomed.Snomed;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.RuleValidationException;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import de.gematik.zts.termzilla.terminologies.snomed.service.MrcmRuleService;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snomed.languages.scg.domain.model.Attribute;
import org.snomed.languages.scg.domain.model.AttributeGroup;
import org.snomed.languages.scg.domain.model.AttributeValue;
import org.snomed.languages.scg.domain.model.Expression;

/** Unit tests for {@link DemisSCGProcessor}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DemisSCGProcessor tests")
class DemisSCGProcessorTest {

  private static final String ROOT_CONCEPT = "138875005";
  private static final String PHARMA_BIOLOGIC_PRODUCT = "373873005";
  private static final String PHYSICAL_OBJECT = "260787004";
  private static final String ATTRIBUTE_ID = "246454002";
  private static final String DOMAIN_ID = "404684003";

  @Mock private Snomed snomed;

  @Mock private MrcmRuleService mrcmRuleService;

  private SnomedReleaseVersion releaseVersion;

  @BeforeEach
  void setUp() {
    releaseVersion = defaultCoreVersion();
    when(snomed.getMrcmRuleService()).thenReturn(mrcmRuleService);
  }

  @Nested
  @DisplayName("constructor")
  class ConstructorTest {

    @Test
    @DisplayName("should initialize domain concepts from MRCM domain rules")
    void shouldInitializeDomainConceptsFromMrcmDomainRules() throws Exception {
      // GIVEN
      Set<MrcmDomainRule> domainRules =
          Set.of(domainRule("rule-1", "404684003"), domainRule("rule-2", "71388002"));
      when(mrcmRuleService.getMrcmDomainRules(null, true)).thenReturn(domainRules);

      // WHEN
      DemisSCGProcessor processor = new DemisSCGProcessor(snomed);

      // THEN
      assertThat(readDomainConcepts(processor)).containsExactlyInAnyOrder("404684003", "71388002");
    }
  }

  @Nested
  @DisplayName("validateAgainstMRCM")
  class ValidateAgainstMrcmTest {

    @Test
    @DisplayName("should return an empty hint when focus concepts stay in one top-level hierarchy")
    void shouldReturnEmptyHintWhenFocusConceptsStayInOneTopLevelHierarchy() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Expression expression = mock(Expression.class);
      when(expression.getFocusConcepts()).thenReturn(List.of("focus-a", "focus-b"));
      when(expression.getAttributes()).thenReturn(null);
      when(expression.getAttributeGroups()).thenReturn(null);

      when(snomed.getDirectChildren(ROOT_CONCEPT, releaseVersion)).thenReturn(Set.of("111111"));
      when(snomed.getAncestorsOrSelf("focus-a", releaseVersion)).thenReturn(Set.of("111111"));
      when(snomed.getAncestorsOrSelf("focus-b", releaseVersion)).thenReturn(Set.of("111111"));

      // WHEN
      String validationHint = processor.validateAgainstMRCM(expression, releaseVersion);

      // THEN
      assertThat(validationHint).isEmpty();
    }

    @Test
    @DisplayName("should return validation hint for the allowed cross-hierarchy exception")
    void shouldReturnValidationHintForAllowedCrossHierarchyException() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Expression expression = mock(Expression.class);
      when(expression.getFocusConcepts()).thenReturn(List.of("focus-a", "focus-b"));
      when(expression.getAttributes()).thenReturn(null);
      when(expression.getAttributeGroups()).thenReturn(null);

      when(snomed.getDirectChildren(ROOT_CONCEPT, releaseVersion))
          .thenReturn(Set.of(PHARMA_BIOLOGIC_PRODUCT, PHYSICAL_OBJECT));
      when(snomed.getAncestorsOrSelf("focus-a", releaseVersion))
          .thenReturn(Set.of(PHARMA_BIOLOGIC_PRODUCT));
      when(snomed.getAncestorsOrSelf("focus-b", releaseVersion))
          .thenReturn(Set.of(PHYSICAL_OBJECT));

      // WHEN
      String validationHint = processor.validateAgainstMRCM(expression, releaseVersion);

      // THEN
      assertThat(validationHint)
          .contains("verschiedenen Top Level Hierarchien")
          .contains("möglich");
    }

    @Test
    @DisplayName("should throw when focus concepts come from non-allowed top-level hierarchies")
    void shouldThrowWhenFocusConceptsComeFromNonAllowedTopLevelHierarchies() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Expression expression = mock(Expression.class);
      when(expression.getFocusConcepts()).thenReturn(List.of("focus-a", "focus-b"));

      when(snomed.getDirectChildren(ROOT_CONCEPT, releaseVersion))
          .thenReturn(Set.of("111111", "222222"));
      when(snomed.getAncestorsOrSelf("focus-a", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("111111")));
      when(snomed.getAncestorsOrSelf("focus-b", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("222222")));

      // WHEN / THEN
      assertThatThrownBy(() -> processor.validateAgainstMRCM(expression, releaseVersion))
          .isInstanceOf(RuleValidationException.class)
          .hasMessageContaining("verschiedenen Top Level Hierarchien");
    }

    @Test
    @DisplayName("should throw when duplicate attribute-value pairs are present")
    void shouldThrowWhenDuplicateAttributeValuePairsArePresent() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Expression expression = mock(Expression.class);
      when(expression.getFocusConcepts()).thenReturn(List.of("focus-a"));

      Attribute firstAttribute = mock(Attribute.class);
      when(firstAttribute.toString()).thenReturn("246454002=255407002");
      Attribute secondAttribute = mock(Attribute.class);
      when(secondAttribute.toString()).thenReturn("246454002=255407002");
      when(expression.getAttributes()).thenReturn(List.of(firstAttribute, secondAttribute));

      when(snomed.getAncestorsOrSelf("focus-a", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("111111")));

      // WHEN / THEN
      assertThatThrownBy(() -> processor.validateAgainstMRCM(expression, releaseVersion))
          .isInstanceOf(RuleValidationException.class)
          .hasMessageContaining("dürfen nicht identische Werte haben");
    }

    @Test
    @DisplayName("should throw when focus concepts touch more than two top-level hierarchies")
    void shouldThrowWhenFocusConceptsTouchMoreThanTwoTopLevelHierarchies() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Expression expression = mock(Expression.class);
      when(expression.getFocusConcepts()).thenReturn(List.of("focus-a", "focus-b", "focus-c"));

      when(snomed.getDirectChildren(ROOT_CONCEPT, releaseVersion))
          .thenReturn(Set.of("111111", "222222", "333333"));
      when(snomed.getAncestorsOrSelf("focus-a", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("111111")));
      when(snomed.getAncestorsOrSelf("focus-b", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("222222")));
      when(snomed.getAncestorsOrSelf("focus-c", releaseVersion))
          .thenReturn(new HashSet<>(Set.of("333333")));

      // WHEN / THEN
      assertThatThrownBy(() -> processor.validateAgainstMRCM(expression, releaseVersion))
          .isInstanceOf(RuleValidationException.class)
          .hasMessageContaining("verschiedenen Top Level Hierarchien");
    }
  }

  @Nested
  @DisplayName("toTermString")
  class ToTermStringTest {

    @Test
    @DisplayName("should join multiple focus concepts with plus separator")
    void shouldJoinMultipleFocusConceptsWithPlusSeparator() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      stubTerm("100001", "Focus concept A");
      stubTerm("100002", "Focus concept B");

      Expression expression = mock(Expression.class);
      when(expression.getDefinitionStatus()).thenReturn(null);
      when(expression.getFocusConcepts()).thenReturn(List.of("100001", "100002"));
      when(expression.getAttributes()).thenReturn(null);
      when(expression.getAttributeGroups()).thenReturn(null);

      // WHEN
      String result = processor.toTermString(expression, releaseVersion);

      // THEN
      assertThat(result).isEqualTo("100001 | Focus concept A | + 100002 | Focus concept B |");
    }

    @Test
    @DisplayName("should render focus concepts and ungrouped attributes with resolved terms")
    void shouldRenderFocusConceptsAndUngroupedAttributesWithResolvedTerms() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      stubTerm("100001", "Focus concept");
      stubTerm("200001", "Attribute concept");
      stubTerm("300001", "Attribute value concept");

      AttributeValue attributeValue = conceptValue("300001");
      Attribute attribute = attribute("200001", attributeValue);
      Expression expression = mock(Expression.class);
      when(expression.getDefinitionStatus()).thenReturn(null);
      when(expression.getFocusConcepts()).thenReturn(List.of("100001"));
      when(expression.getAttributes()).thenReturn(List.of(attribute));
      when(expression.getAttributeGroups()).thenReturn(null);

      // WHEN
      String result = processor.toTermString(expression, releaseVersion);

      // THEN
      assertThat(result)
          .isEqualTo(
              "100001 | Focus concept | : 200001 | Attribute concept | = 300001 | Attribute value concept |");
    }

    @Test
    @DisplayName("should render grouped attributes containing nested expressions")
    void shouldRenderGroupedAttributesContainingNestedExpressions() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      stubTerm("100001", "Focus concept");
      stubTerm("200001", "Attribute concept");
      stubTerm("500001", "Nested focus concept");

      Expression nestedExpression = mock(Expression.class);
      when(nestedExpression.getDefinitionStatus()).thenReturn(null);
      when(nestedExpression.getFocusConcepts()).thenReturn(List.of("500001"));
      when(nestedExpression.getAttributes()).thenReturn(null);
      when(nestedExpression.getAttributeGroups()).thenReturn(null);

      AttributeValue nestedValue = nestedExpressionValue(nestedExpression);
      Attribute groupedAttribute = attribute("200001", nestedValue);
      AttributeGroup attributeGroup = mock(AttributeGroup.class);
      when(attributeGroup.getAttributes()).thenReturn(List.of(groupedAttribute));

      Set<AttributeGroup> groups = new LinkedHashSet<>();
      groups.add(attributeGroup);

      Expression expression = mock(Expression.class);
      when(expression.getDefinitionStatus()).thenReturn(null);
      when(expression.getFocusConcepts()).thenReturn(List.of("100001"));
      when(expression.getAttributes()).thenReturn(null);
      when(expression.getAttributeGroups()).thenReturn(groups);

      // WHEN
      String result = processor.toTermString(expression, releaseVersion);

      // THEN
      assertThat(result)
          .isEqualTo(
              "100001 | Focus concept | : {200001 | Attribute concept | = ( 500001 | Nested focus concept | )}");
    }

    @Test
    @DisplayName("should not append refinement separator when both refinement sections are empty")
    void shouldNotAppendRefinementSeparatorWhenBothRefinementSectionsAreEmpty() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      stubTerm("100001", "Focus concept");

      Expression expression = mock(Expression.class);
      when(expression.getDefinitionStatus()).thenReturn(null);
      when(expression.getFocusConcepts()).thenReturn(List.of("100001"));
      when(expression.getAttributes()).thenReturn(new ArrayList<>());
      when(expression.getAttributeGroups()).thenReturn(new LinkedHashSet<>());

      // WHEN
      String result = processor.toTermString(expression, releaseVersion);

      // THEN
      assertThat(result).isEqualTo("100001 | Focus concept |");
    }
  }

  @Nested
  @DisplayName("isMatchingCardinalityConstraint")
  class IsMatchingCardinalityConstraintTest {

    @Test
    @DisplayName("should support multi-digit lower and upper bounds")
    void shouldSupportMultiDigitLowerAndUpperBounds() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      List<String> attributeIds = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        attributeIds.add("246454002");
      }

      // WHEN
      boolean result = invokeCardinalityConstraint(processor, attributeIds, "246454002", "10..12");

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false for invalid cardinality format")
    void shouldReturnFalseForInvalidCardinalityFormat() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      List<String> attributeIds = List.of("246454002");

      // WHEN
      boolean result = invokeCardinalityConstraint(processor, attributeIds, "246454002", "1-2");

      // THEN
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("getApplicableAttributeDomainRulesByDomainConcepts")
  class GetApplicableAttributeDomainRulesByDomainConceptsTest {

    @Test
    @DisplayName("should keep only rules with matching domain id")
    void shouldKeepOnlyRulesWithMatchingDomainId() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      MrcmAttributeDomainRule matchingRule =
          attributeDomainRule(
              "domain-rule-1",
              ATTRIBUTE_ID,
              DOMAIN_ID,
              "1",
              "1..1",
              "0..1",
              "723596005",
              "20240101");
      MrcmAttributeDomainRule nonMatchingRule =
          attributeDomainRule(
              "domain-rule-2",
              ATTRIBUTE_ID,
              "999999",
              "1",
              "1..1",
              "0..1",
              "723596005",
              "20240101");

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          invokeApplicableDomainRulesByDomainConcepts(
              processor, Set.of(matchingRule, nonMatchingRule), Set.of(DOMAIN_ID));

      // THEN
      assertThat(result).containsExactly(matchingRule);
    }
  }

  @Nested
  @DisplayName("getApplicableAttributeRangeRulesByContentType")
  class GetApplicableAttributeRangeRulesByContentTypeTest {

    @Test
    @DisplayName("should keep only range rules with matching content type")
    void shouldKeepOnlyRangeRulesWithMatchingContentType() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      MrcmAttributeRangeRule matchingRule =
          attributeRangeRule("range-rule-1", ATTRIBUTE_ID, "1", "723596005", "20240101", "<< 123");
      MrcmAttributeRangeRule nonMatchingRule =
          attributeRangeRule("range-rule-2", ATTRIBUTE_ID, "1", "999999999", "20240101", "<< 123");

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          invokeApplicableRangeRulesByContentType(
              processor, Set.of(matchingRule, nonMatchingRule), Set.of("723596005"));

      // THEN
      assertThat(result).containsExactly(matchingRule);
    }
  }

  @Nested
  @DisplayName("getApplicableAttributeDomainRules")
  class GetApplicableAttributeDomainRulesTest {

    @Test
    @DisplayName("should return filtered active and domain-matching domain rules")
    void shouldReturnFilteredActiveAndDomainMatchingDomainRules() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      MrcmAttributeDomainRule matchingRule =
          attributeDomainRule(
              "domain-rule-1",
              ATTRIBUTE_ID,
              DOMAIN_ID,
              "1",
              "1..1",
              "0..1",
              "723596005",
              "20240101");
      MrcmAttributeDomainRule inactiveRule =
          attributeDomainRule(
              "domain-rule-2",
              ATTRIBUTE_ID,
              DOMAIN_ID,
              "0",
              "1..1",
              "0..1",
              "723596005",
              "20240101");
      MrcmAttributeDomainRule otherDomainRule =
          attributeDomainRule(
              "domain-rule-3",
              ATTRIBUTE_ID,
              "999999",
              "1",
              "1..1",
              "0..1",
              "723596005",
              "20240101");
      when(mrcmRuleService.getMrcmAttributeDomainRules(releaseVersion, ATTRIBUTE_ID))
          .thenReturn(Set.of(matchingRule, inactiveRule, otherDomainRule));

      // WHEN
      Set<MrcmAttributeDomainRule> result =
          invokeApplicableDomainRules(processor, ATTRIBUTE_ID, Set.of(DOMAIN_ID), releaseVersion);

      // THEN
      assertThat(result).containsExactly(matchingRule);
    }
  }

  @Nested
  @DisplayName("getApplicableAttributeRangeRules")
  class GetApplicableAttributeRangeRulesTest {

    @Test
    @DisplayName("should return active newest range rules with relevant content type")
    void shouldReturnActiveNewestRangeRulesWithRelevantContentType() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      MrcmAttributeRangeRule newestRelevantRule =
          attributeRangeRule("range-rule-1", ATTRIBUTE_ID, "1", "723596005", "20240201", "<< 123");
      MrcmAttributeRangeRule olderSameIdRule =
          attributeRangeRule("range-rule-1", ATTRIBUTE_ID, "1", "723596005", "20240101", "<< 999");
      MrcmAttributeRangeRule inactiveRule =
          attributeRangeRule("range-rule-2", ATTRIBUTE_ID, "0", "723596005", "20240101", "<< 123");
      MrcmAttributeRangeRule wrongContentTypeRule =
          attributeRangeRule("range-rule-3", ATTRIBUTE_ID, "1", "999999", "20240101", "<< 123");
      when(mrcmRuleService.getMrcmAttributeRangeRules(releaseVersion, ATTRIBUTE_ID))
          .thenReturn(
              Set.of(newestRelevantRule, olderSameIdRule, inactiveRule, wrongContentTypeRule));

      // WHEN
      Set<MrcmAttributeRangeRule> result =
          invokeApplicableRangeRules(processor, ATTRIBUTE_ID, releaseVersion);

      // THEN
      assertThat(result).containsExactly(newestRelevantRule);
    }
  }

  @Nested
  @DisplayName("validateAttributeAgainstDomainRules")
  class ValidateAttributeAgainstDomainRulesTest {

    @Test
    @DisplayName("should throw when cardinality is violated")
    void shouldThrowWhenCardinalityIsViolated() {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      Attribute attribute = mock(Attribute.class);
      when(attribute.getAttributeId()).thenReturn(ATTRIBUTE_ID);
      MrcmAttributeDomainRule rule =
          attributeDomainRule(
              "domain-rule-1",
              ATTRIBUTE_ID,
              DOMAIN_ID,
              "1",
              "2..2",
              "0..1",
              "723596005",
              "20240101");

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  invokeValidateAttributeAgainstDomainRules(
                      processor, attribute, ATTRIBUTE_ID, List.of(attribute), false, Set.of(rule)))
          .isInstanceOf(RuleValidationException.class)
          .hasMessageContaining("Cardinality");
    }
  }

  @Nested
  @DisplayName("validateAttributeAgainstRangeRules")
  class ValidateAttributeAgainstRangeRulesTest {

    @Test
    @DisplayName("should throw when concept value is outside allowed range")
    void shouldThrowWhenConceptValueIsOutsideAllowedRange() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      AttributeValue attributeValue = conceptValue("300001");
      MrcmAttributeRangeRule rule =
          attributeRangeRule("range-rule-1", ATTRIBUTE_ID, "1", "723596005", "20240101", "<< 999");
      when(snomed.checkIfConceptIsIncludedInExpressionConstraintResult(
              "300001", "<< 999", releaseVersion))
          .thenReturn(false);

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  invokeValidateAttributeAgainstRangeRules(
                      processor, attributeValue, ATTRIBUTE_ID, releaseVersion, Set.of(rule)))
          .isInstanceOf(RuleValidationException.class)
          .hasMessageContaining("not included in defined range");
    }
  }

  @Nested
  @DisplayName("validateSingleAttribute")
  class ValidateSingleAttributeTest {

    @Test
    @DisplayName("should validate single attribute successfully when domain and range rules match")
    void shouldValidateSingleAttributeSuccessfullyWhenDomainAndRangeRulesMatch() throws Exception {
      // GIVEN
      DemisSCGProcessor processor = newProcessorWithDomainRules(Set.of());
      AttributeValue attributeValue = conceptValue("300001");
      Attribute attribute = attribute(ATTRIBUTE_ID, attributeValue);
      List<Attribute> attributeList = List.of(attribute);

      MrcmAttributeDomainRule domainRule =
          attributeDomainRule(
              "domain-rule-1",
              ATTRIBUTE_ID,
              DOMAIN_ID,
              "1",
              "1..1",
              "0..1",
              "723596005",
              "20240101");
      when(mrcmRuleService.getMrcmAttributeDomainRules(releaseVersion, ATTRIBUTE_ID))
          .thenReturn(Set.of(domainRule));

      MrcmAttributeRangeRule rangeRule =
          attributeRangeRule("range-rule-1", ATTRIBUTE_ID, "1", "723596005", "20240101", "<< 123");
      when(mrcmRuleService.getMrcmAttributeRangeRules(releaseVersion, ATTRIBUTE_ID))
          .thenReturn(Set.of(rangeRule));

      when(snomed.checkIfConceptIsIncludedInExpressionConstraintResult(
              "300001", "<< 123", releaseVersion))
          .thenReturn(true);

      // WHEN / THEN
      assertThatCode(
              () ->
                  invokeValidateSingleAttribute(
                      processor,
                      attribute,
                      attributeList,
                      Set.of(DOMAIN_ID),
                      false,
                      releaseVersion))
          .doesNotThrowAnyException();
    }
  }

  private DemisSCGProcessor newProcessorWithDomainRules(Set<MrcmDomainRule> domainRules) {
    when(mrcmRuleService.getMrcmDomainRules(null, true)).thenReturn(domainRules);
    return new DemisSCGProcessor(snomed);
  }

  private Set<String> readDomainConcepts(DemisSCGProcessor processor) throws Exception {
    Field domainConceptsField = DemisSCGProcessor.class.getDeclaredField("domainConcepts");
    domainConceptsField.setAccessible(true);
    Object value = domainConceptsField.get(processor);
    return (Set<String>) value;
  }

  private void stubTerm(String conceptId, String displayTerm) throws Exception {
    SnomedConcept concept = conceptWithDisplay(displayTerm);
    when(snomed.getSnomedConceptWithDescription(conceptId, releaseVersion, false))
        .thenReturn(concept);
  }

  private boolean invokeCardinalityConstraint(
      DemisSCGProcessor processor,
      List<String> attributeIdList,
      String attributeId,
      String cardinalityConstraint)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "isMatchingCardinalityConstraint", List.class, String.class, String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(processor, attributeIdList, attributeId, cardinalityConstraint);
  }

  private MrcmAttributeDomainRule attributeDomainRule(
      String id,
      String referencedComponentId,
      String domainId,
      String active,
      String attributeCardinality,
      String attributeInGroupCardinality,
      String contentTypeId,
      String effectiveTime) {
    MrcmAttributeDomainRule rule = new MrcmAttributeDomainRule();
    rule.setId(id);
    rule.setReferencedComponentId(referencedComponentId);
    rule.setDomainId(domainId);
    rule.setActive(active);
    rule.setAttributeCardinality(attributeCardinality);
    rule.setAttributeInGroupCardinality(attributeInGroupCardinality);
    rule.setContentTypeId(contentTypeId);
    rule.setEffectiveTime(effectiveTime);
    rule.setGrouped("0");
    return rule;
  }

  private MrcmAttributeRangeRule attributeRangeRule(
      String id,
      String referencedComponentId,
      String active,
      String contentTypeId,
      String effectiveTime,
      String rangeConstraint) {
    MrcmAttributeRangeRule rule = new MrcmAttributeRangeRule();
    rule.setId(id);
    rule.setReferencedComponentId(referencedComponentId);
    rule.setActive(active);
    rule.setContentTypeId(contentTypeId);
    rule.setEffectiveTime(effectiveTime);
    rule.setRangeConstraint(rangeConstraint);
    return rule;
  }

  @SuppressWarnings("unchecked")
  private Set<MrcmAttributeDomainRule> invokeApplicableDomainRulesByDomainConcepts(
      DemisSCGProcessor processor, Set<MrcmAttributeDomainRule> rules, Set<String> domainConcepts)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "getApplicableAttributeDomainRulesByDomainConcepts", Set.class, Set.class);
    method.setAccessible(true);
    return (Set<MrcmAttributeDomainRule>) method.invoke(processor, rules, domainConcepts);
  }

  @SuppressWarnings("unchecked")
  private Set<MrcmAttributeRangeRule> invokeApplicableRangeRulesByContentType(
      DemisSCGProcessor processor, Set<MrcmAttributeRangeRule> rules, Set<String> contentTypeIds)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "getApplicableAttributeRangeRulesByContentType", Set.class, Set.class);
    method.setAccessible(true);
    return (Set<MrcmAttributeRangeRule>) method.invoke(processor, rules, contentTypeIds);
  }

  @SuppressWarnings("unchecked")
  private Set<MrcmAttributeDomainRule> invokeApplicableDomainRules(
      DemisSCGProcessor processor,
      String attributeId,
      Set<String> applicableDomainConcepts,
      SnomedReleaseVersion snomedReleaseVersion)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "getApplicableAttributeDomainRules",
            String.class,
            Set.class,
            SnomedReleaseVersion.class);
    method.setAccessible(true);
    return (Set<MrcmAttributeDomainRule>)
        method.invoke(processor, attributeId, applicableDomainConcepts, snomedReleaseVersion);
  }

  @SuppressWarnings("unchecked")
  private Set<MrcmAttributeRangeRule> invokeApplicableRangeRules(
      DemisSCGProcessor processor, String attributeId, SnomedReleaseVersion snomedReleaseVersion)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "getApplicableAttributeRangeRules", String.class, SnomedReleaseVersion.class);
    method.setAccessible(true);
    return (Set<MrcmAttributeRangeRule>)
        method.invoke(processor, attributeId, snomedReleaseVersion);
  }

  private void invokeValidateAttributeAgainstDomainRules(
      DemisSCGProcessor processor,
      Attribute attribute,
      String attributeId,
      List<Attribute> attributeList,
      boolean isGrouped,
      Set<MrcmAttributeDomainRule> rules)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "validateAttributeAgainstDomainRules",
            Attribute.class,
            String.class,
            List.class,
            boolean.class,
            Set.class);
    method.setAccessible(true);
    try {
      method.invoke(processor, attribute, attributeId, attributeList, isGrouped, rules);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception exception) {
        throw exception;
      }
      throw e;
    }
  }

  private void invokeValidateAttributeAgainstRangeRules(
      DemisSCGProcessor processor,
      AttributeValue attributeValue,
      String attributeId,
      SnomedReleaseVersion snomedReleaseVersion,
      Set<MrcmAttributeRangeRule> rules)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "validateAttributeAgainstRangeRules",
            AttributeValue.class,
            String.class,
            SnomedReleaseVersion.class,
            Set.class);
    method.setAccessible(true);
    try {
      method.invoke(processor, attributeValue, attributeId, snomedReleaseVersion, rules);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception exception) {
        throw exception;
      }
      throw e;
    }
  }

  private void invokeValidateSingleAttribute(
      DemisSCGProcessor processor,
      Attribute attribute,
      List<Attribute> attributeList,
      Set<String> applicableDomainConcepts,
      boolean isGrouped,
      SnomedReleaseVersion snomedReleaseVersion)
      throws Exception {
    Method method =
        DemisSCGProcessor.class.getDeclaredMethod(
            "validateSingleAttribute",
            Attribute.class,
            List.class,
            Set.class,
            boolean.class,
            SnomedReleaseVersion.class);
    method.setAccessible(true);
    try {
      method.invoke(
          processor,
          attribute,
          attributeList,
          applicableDomainConcepts,
          isGrouped,
          snomedReleaseVersion);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception exception) {
        throw exception;
      }
      throw e;
    }
  }
}
