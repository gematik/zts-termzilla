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
package de.gematik.zts.termzilla.terminologies.snomed.service;

import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_CLINICAL_FINDING;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_DIABETES;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_DISORDER;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_HYPERTENSION;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.CONCEPT_ROOT;
import static de.gematik.zts.termzilla.terminologies.snomed.testutil.SnomedTestData.defaultCoreVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.snomed.langauges.ecl.ECLObjectFactory;
import org.snomed.langauges.ecl.ECLQueryBuilder;
import org.snomed.langauges.ecl.domain.expressionconstraint.CompoundExpressionConstraint;
import org.snomed.langauges.ecl.domain.expressionconstraint.SubExpressionConstraint;
import org.snomed.langauges.ecl.domain.refinement.Operator;

/** Unit tests for {@link EclEvaluationService}. */
@DisplayName("EclEvaluationService Tests")
@ExtendWith(MockitoExtension.class)
class EclEvaluationServiceTest {

  @Mock private SnomedGraphRepresentation snomedGraph;

  private SnomedReleaseVersion version;
  private EclEvaluationService eclEvaluationService;

  @BeforeEach
  void setUp() {
    version = defaultCoreVersion();
    ECLQueryBuilder eclQueryBuilder = new ECLQueryBuilder(new ECLObjectFactory());
    eclEvaluationService = new EclEvaluationService(snomedGraph, eclQueryBuilder);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // resolveExpressionConstraint – SubExpressionConstraint operators
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("resolveExpressionConstraint with SubExpressionConstraint")
  class ResolveSubExpressionConstraint {

    @Test
    @DisplayName("should resolve descendantOf operator (< concept)")
    void shouldResolveDescendantOf() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(
              new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES, CONCEPT_HYPERTENSION)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint("< " + CONCEPT_DISORDER, version);

      // THEN – self is excluded
      assertThat(result)
          .containsExactlyInAnyOrder(CONCEPT_DIABETES, CONCEPT_HYPERTENSION)
          .doesNotContain(CONCEPT_DISORDER);
    }

    @Test
    @DisplayName("should resolve descendantOrSelfOf operator (<< concept)")
    void shouldResolveDescendantOrSelfOf() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(
              new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES, CONCEPT_HYPERTENSION)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint("<< " + CONCEPT_DISORDER, version);

      // THEN – self is included
      assertThat(result)
          .containsExactlyInAnyOrder(CONCEPT_DISORDER, CONCEPT_DIABETES, CONCEPT_HYPERTENSION);
    }

    @Test
    @DisplayName("should resolve ancestorOf operator (> concept)")
    void shouldResolveAncestorOf() throws Exception {
      // GIVEN
      when(snomedGraph.getAllAncestorsOrSelf(CONCEPT_DIABETES, version))
          .thenReturn(
              new HashSet<>(
                  Set.of(
                      CONCEPT_DIABETES, CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint("> " + CONCEPT_DIABETES, version);

      // THEN – self is excluded
      assertThat(result)
          .containsExactlyInAnyOrder(CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT)
          .doesNotContain(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("should resolve ancestorOrSelfOf operator (>> concept)")
    void shouldResolveAncestorOrSelfOf() throws Exception {
      // GIVEN
      when(snomedGraph.getAllAncestorsOrSelf(CONCEPT_DIABETES, version))
          .thenReturn(
              new HashSet<>(
                  Set.of(
                      CONCEPT_DIABETES, CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint(">> " + CONCEPT_DIABETES, version);

      // THEN – self is included
      assertThat(result)
          .containsExactlyInAnyOrder(
              CONCEPT_DIABETES, CONCEPT_DISORDER, CONCEPT_CLINICAL_FINDING, CONCEPT_ROOT);
    }

    @Test
    @DisplayName("should resolve childOf operator (<! concept)")
    void shouldResolveChildOf() throws Exception {
      // GIVEN
      when(snomedGraph.getDirectChildren(CONCEPT_DISORDER, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DIABETES, CONCEPT_HYPERTENSION)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint("<! " + CONCEPT_DISORDER, version);

      // THEN
      assertThat(result).containsExactlyInAnyOrder(CONCEPT_DIABETES, CONCEPT_HYPERTENSION);
    }

    @Test
    @DisplayName("should resolve parentOf operator (>! concept)")
    void shouldResolveParentOf() throws Exception {
      // GIVEN
      when(snomedGraph.getDirectParents(CONCEPT_DIABETES, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DISORDER)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint(">! " + CONCEPT_DIABETES, version);

      // THEN
      assertThat(result).containsExactly(CONCEPT_DISORDER);
    }

    @Test
    @DisplayName("should resolve concept without operator when it has active relations")
    void shouldResolveConceptWithoutOperator() throws Exception {
      // GIVEN
      when(snomedGraph.hasActiveRelations(CONCEPT_DIABETES, version)).thenReturn(true);

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint(CONCEPT_DIABETES, version);

      // THEN
      assertThat(result).containsExactly(CONCEPT_DIABETES);
    }

    @Test
    @DisplayName("should return empty set for concept without operator and no active relations")
    void shouldReturnEmptySetForConceptWithoutActiveRelations() throws Exception {
      // GIVEN
      when(snomedGraph.hasActiveRelations(CONCEPT_DIABETES, version)).thenReturn(false);

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint(CONCEPT_DIABETES, version);

      // THEN
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should translate UnknownConceptException from descendants lookup")
    void shouldTranslateUnknownConceptFromDescendantsLookup() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenThrow(new UnknownConceptException("unknown concept"));

      // WHEN / THEN
      assertThatThrownBy(
              () ->
                  eclEvaluationService.resolveExpressionConstraint(
                      "<< " + CONCEPT_DISORDER, version))
          .isInstanceOf(SimpleExpressionConstraintException.class)
          .hasMessageContaining("unknown concept");
    }

    @Test
    @DisplayName("should translate UnknownConceptException from active-relation lookup")
    void shouldTranslateUnknownConceptFromActiveRelationLookup() throws Exception {
      // GIVEN
      when(snomedGraph.hasActiveRelations(CONCEPT_DIABETES, version))
          .thenThrow(new UnknownConceptException("unknown concept"));

      // WHEN / THEN
      assertThatThrownBy(
              () -> eclEvaluationService.resolveExpressionConstraint(CONCEPT_DIABETES, version))
          .isInstanceOf(SimpleExpressionConstraintException.class)
          .hasMessageContaining("unknown concept");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // resolveExpressionConstraint – CompoundExpressionConstraint (OR)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("resolveExpressionConstraint with CompoundExpressionConstraint")
  class ResolveCompoundExpressionConstraint {

    @Test
    @DisplayName("should resolve disjunction (OR) of two descendantOrSelfOf expressions")
    void shouldResolveDisjunction() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES)));
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_CLINICAL_FINDING, version))
          .thenReturn(
              new HashSet<>(Set.of(CONCEPT_CLINICAL_FINDING, CONCEPT_DISORDER, CONCEPT_DIABETES)));

      // WHEN
      Set<String> result =
          eclEvaluationService.resolveExpressionConstraint(
              "<< " + CONCEPT_DISORDER + " OR << " + CONCEPT_CLINICAL_FINDING, version);

      // THEN – union of both results
      assertThat(result)
          .containsExactlyInAnyOrder(CONCEPT_DISORDER, CONCEPT_DIABETES, CONCEPT_CLINICAL_FINDING);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // checkIfConceptIsIncludedInExpressionConstraintResult
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("checkIfConceptIsIncludedInExpressionConstraintResult")
  class CheckIfConceptIncluded {

    @Test
    @DisplayName("should return true when concept is in the expression result")
    void shouldReturnTrueWhenConceptIncluded() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES)));

      // WHEN
      boolean result =
          eclEvaluationService.checkIfConceptIsIncludedInExpressionConstraintResult(
              CONCEPT_DIABETES, "<< " + CONCEPT_DISORDER, version);

      // THEN
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when concept is not in the expression result")
    void shouldReturnFalseWhenConceptNotIncluded() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES)));

      // WHEN
      boolean result =
          eclEvaluationService.checkIfConceptIsIncludedInExpressionConstraintResult(
              CONCEPT_HYPERTENSION, "<< " + CONCEPT_DISORDER, version);

      // THEN
      assertThat(result).isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Unsupported expressions
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("unsupported expression constraints")
  class UnsupportedExpressions {

    @Test
    @DisplayName("should throw exception for invalid ECL syntax")
    void shouldThrowForInvalidEclSyntax() {
      // WHEN / THEN
      assertThatThrownBy(
              () -> eclEvaluationService.resolveExpressionConstraint("NOT_VALID_ECL !!!", version))
          .isInstanceOf(SimpleExpressionConstraintException.class);
    }

    @Test
    @DisplayName("should throw exception for wildcard expression")
    void shouldThrowForWildcardExpression() {
      // WHEN / THEN
      assertThatThrownBy(() -> eclEvaluationService.resolveExpressionConstraint("*", version))
          .isInstanceOf(SimpleExpressionConstraintException.class);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // isSupportedExpressionConstraint (static helpers)
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("isSupportedExpressionConstraint")
  class IsSupportedExpressionConstraint {

    @Test
    @DisplayName("should accept a simple SubExpressionConstraint with descendantOrSelfOf")
    void shouldAcceptSimpleSubExpression() {
      // GIVEN
      SubExpressionConstraint sec = new SubExpressionConstraint(Operator.descendantorselfof);
      sec.setConceptId(CONCEPT_DISORDER);

      // WHEN / THEN
      assertThat(EclEvaluationService.isSupportedExpressionConstraint(sec)).isTrue();
    }

    @Test
    @DisplayName("should reject a wildcard SubExpressionConstraint")
    void shouldRejectWildcardSubExpression() {
      // GIVEN
      SubExpressionConstraint sec = new SubExpressionConstraint(null);
      sec.setWildcard(true);

      // WHEN / THEN
      assertThat(EclEvaluationService.isSupportedSubExpressionConstraint(sec)).isFalse();
    }

    @Test
    @DisplayName("should accept a CompoundExpressionConstraint with supported disjunctions")
    void shouldAcceptSupportedCompound() {
      // GIVEN
      SubExpressionConstraint sec1 = new SubExpressionConstraint(Operator.descendantorselfof);
      sec1.setConceptId(CONCEPT_DISORDER);

      SubExpressionConstraint sec2 = new SubExpressionConstraint(Operator.ancestorof);
      sec2.setConceptId(CONCEPT_CLINICAL_FINDING);

      CompoundExpressionConstraint cec = new CompoundExpressionConstraint();
      cec.setDisjunctionExpressionConstraints(java.util.List.of(sec1, sec2));

      // WHEN / THEN
      assertThat(EclEvaluationService.isSupportedCompoundExpressionConstraint(cec)).isTrue();
    }

    @Test
    @DisplayName("should reject CompoundExpressionConstraint with conjunction")
    void shouldRejectConjunctionCompound() {
      // GIVEN
      SubExpressionConstraint sec = new SubExpressionConstraint(Operator.descendantorselfof);
      sec.setConceptId(CONCEPT_DISORDER);

      CompoundExpressionConstraint cec = new CompoundExpressionConstraint();
      cec.setConjunctionExpressionConstraints(java.util.List.of(sec));

      // WHEN / THEN
      assertThat(EclEvaluationService.isSupportedCompoundExpressionConstraint(cec)).isFalse();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Caching
  // ──────────────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("caching behavior")
  class CachingBehavior {

    @Test
    @DisplayName("should return cached result on second call with same expression and version")
    void shouldReturnCachedResult() throws Exception {
      // GIVEN
      when(snomedGraph.getAllDescendantsOrSelf(CONCEPT_DISORDER, version))
          .thenReturn(new HashSet<>(Set.of(CONCEPT_DISORDER, CONCEPT_DIABETES)));

      // WHEN – call twice
      Set<String> firstResult =
          eclEvaluationService.resolveExpressionConstraint("<< " + CONCEPT_DISORDER, version);
      Set<String> secondResult =
          eclEvaluationService.resolveExpressionConstraint("<< " + CONCEPT_DISORDER, version);

      // THEN – same result, graph only called once
      assertThat(firstResult).isEqualTo(secondResult);
    }
  }
}
