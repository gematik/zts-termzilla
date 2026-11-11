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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.terminologies.snomed.exceptions.SimpleExpressionConstraintException;
import de.gematik.zts.termzilla.terminologies.snomed.graph.SnomedGraphRepresentation;
import de.gematik.zts.termzilla.terminologies.snomed.graph.caching.GraphCacheKeys;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.snomed.langauges.ecl.ECLException;
import org.snomed.langauges.ecl.ECLObjectFactory;
import org.snomed.langauges.ecl.ECLQueryBuilder;
import org.snomed.langauges.ecl.domain.expressionconstraint.CompoundExpressionConstraint;
import org.snomed.langauges.ecl.domain.expressionconstraint.ExpressionConstraint;
import org.snomed.langauges.ecl.domain.expressionconstraint.SubExpressionConstraint;
import org.snomed.langauges.ecl.domain.refinement.Operator;

/**
 * Service for evaluating SNOMED CT Expression Constraint Language (ECL) expressions against the
 * concept graph.
 *
 * <p>This service encapsulates all ECL parsing, evaluation, and caching logic. It uses the {@link
 * SnomedGraphRepresentation} for graph traversal and maintains a {@link LoadingCache} for resolved
 * expression results.
 */
@Slf4j
public class EclEvaluationService {

  private final SnomedGraphRepresentation snomedGraph;
  private final ECLQueryBuilder eclQueryBuilder;
  private final LoadingCache<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>>
      versionExpressionCache;

  /**
   * Creates a new {@link EclEvaluationService}.
   *
   * @param snomedGraph the graph representation used for concept hierarchy traversal
   */
  public EclEvaluationService(SnomedGraphRepresentation snomedGraph) {
    this.snomedGraph = snomedGraph;
    this.eclQueryBuilder = new ECLQueryBuilder(new ECLObjectFactory());
    this.versionExpressionCache = buildExpressionCache();
  }

  /** Package-private constructor for tests allowing a custom {@link ECLQueryBuilder}. */
  EclEvaluationService(SnomedGraphRepresentation snomedGraph, ECLQueryBuilder eclQueryBuilder) {
    this.snomedGraph = snomedGraph;
    this.eclQueryBuilder = eclQueryBuilder;
    this.versionExpressionCache = buildExpressionCache();
  }

  /** Builds the cache used for expression-to-result resolution. */
  @SuppressWarnings("NullableProblems")
  private LoadingCache<GraphCacheKeys.SnomedVersionStringCacheKey, Set<String>>
      buildExpressionCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(100)
        .build(
            new CacheLoader<>() {
              @Override
              public Set<String> load(GraphCacheKeys.SnomedVersionStringCacheKey key)
                  throws Exception {
                log.debug("Loading ECL expression result for cache");
                return internalResolveExpressionConstraint(
                    key.cacheValue(), key.snomedReleaseVersion());
              }
            });
  }

  /**
   * Checks whether a concept is included in the result of a resolved expression constraint.
   *
   * @param conceptId the concept identifier to check
   * @param expression the ECL expression to evaluate
   * @param releaseVersion the release version to evaluate against
   * @return {@code true} if the concept is included in the expression result
   * @throws SimpleExpressionConstraintException if the expression cannot be evaluated
   */
  public boolean checkIfConceptIsIncludedInExpressionConstraintResult(
      String conceptId, String expression, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {
    return resolveExpressionConstraint(expression, releaseVersion).contains(conceptId);
  }

  /**
   * Resolves an ECL expression and returns the matching concept identifiers. Results are cached.
   *
   * @param expression the ECL expression to resolve
   * @param releaseVersion the release version to evaluate against
   * @return a set of concept identifiers matching the expression
   * @throws SimpleExpressionConstraintException if the expression cannot be evaluated
   */
  public Set<String> resolveExpressionConstraint(
      String expression, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {
    GraphCacheKeys.SnomedVersionStringCacheKey key =
        new GraphCacheKeys.SnomedVersionStringCacheKey(releaseVersion, expression);
    // Returns the cached result or computes and caches it on cache miss.
    try {
      return versionExpressionCache.get(key);
    } catch (ExecutionException e) {
      throw new SimpleExpressionConstraintException(
          "Error while resolving expression: " + e.getMessage());
    }
  }

  /** Resolves an ECL expression without caching (internal implementation). */
  private Set<String> internalResolveExpressionConstraint(
      String expression, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {

    Set<String> result;
    try {
      ExpressionConstraint ec = eclQueryBuilder.createQuery(expression);
      result = resolveExpressionConstraint(ec, releaseVersion);
    } catch (ECLException e) {
      log.warn("ECL error: {}", e.getMessage());
      throw new SimpleExpressionConstraintException(e.getMessage());
    }

    return result;
  }

  /** Resolves a parsed {@link ExpressionConstraint} against the graph. */
  private Set<String> resolveExpressionConstraint(
      ExpressionConstraint expressionConstraint, SnomedReleaseVersion releaseVersion)
      throws SimpleExpressionConstraintException {

    Set<String> results = new HashSet<>();

    // Check whether the expression constraint is supported.
    // Currently only the subset required by MRCM usage is implemented.
    if (!isSupportedExpressionConstraint(expressionConstraint)) {
      throw new SimpleExpressionConstraintException(
          "The ExpressionConstraint can't be evaluated. "
              + "Currently only a minimum functionality is supported in this context.");
    }

    // case one: we have a simple SubExpression (e.g. << 102290001)
    // case two: we have a CompoundExpression (e.g. << 102290001 OR <<...)
    // case 3: we have something else that is not supported
    switch (expressionConstraint) {
      case SubExpressionConstraint sub ->
          results.addAll(resolveSubExpressionConstraint(sub, releaseVersion));
      case CompoundExpressionConstraint compound ->
          results.addAll(resolveCompoundExpressionConstraint(compound, releaseVersion));
      default ->
          throw new SimpleExpressionConstraintException(
              "The ExpressionConstraint can't be evaluated. "
                  + "Currently only a minimum functionality is supported in this context.");
    }

    return results;
  }

  /** Resolves a disjunction-based compound expression constraint. */
  private Set<String> resolveCompoundExpressionConstraint(
      CompoundExpressionConstraint cec, SnomedReleaseVersion version)
      throws SimpleExpressionConstraintException {
    Set<String> results = new HashSet<>();
    for (SubExpressionConstraint sec : cec.getDisjunctionExpressionConstraints()) {
      results.addAll(resolveSubExpressionConstraint(sec, version));
    }
    return results;
  }

  /** Resolves a single sub-expression constraint using graph traversal operators. */
  private Set<String> resolveSubExpressionConstraint(
      SubExpressionConstraint sec, SnomedReleaseVersion version)
      throws SimpleExpressionConstraintException {
    Set<String> results = new HashSet<>();

    String conceptId = sec.getConceptId();
    Operator operator = sec.getOperator();

    try {
      if (operator == Operator.descendantof) {
        results = snomedGraph.getAllDescendantsOrSelf(conceptId, version);
        results.remove(conceptId);
      }

      if (operator == Operator.descendantorselfof) {
        results = snomedGraph.getAllDescendantsOrSelf(conceptId, version);
        results.add(conceptId); // Usually already included, but kept explicitly for robustness.
      }

      if (operator == Operator.ancestorof) {
        results = snomedGraph.getAllAncestorsOrSelf(conceptId, version);
        results.remove(conceptId);
      }

      if (operator == Operator.ancestororselfof) {
        results = snomedGraph.getAllAncestorsOrSelf(conceptId, version);
        results.add(conceptId); // Usually already included, but kept explicitly for robustness.
      }

      if (operator == Operator.childof) {
        results = snomedGraph.getDirectChildren(conceptId, version);
        results.remove(conceptId);
      }

      if (operator == Operator.childorselfof) {
        results = snomedGraph.getDirectChildren(conceptId, version);
        results.add(conceptId);
      }

      if (operator == Operator.parentof) {
        results = snomedGraph.getDirectParents(conceptId, version);
        results.remove(conceptId);
      }

      if (operator == Operator.parentorselfof) {
        results = snomedGraph.getDirectParents(conceptId, version);
        results.add(conceptId);
      }
    } catch (UnknownConceptException e) {
      throw new SimpleExpressionConstraintException(e.getMessage());
    }

    // TODO: implement additional operators when needed.

    // Special case: without an operator, the expected result is the concept itself.
    // We do not directly know whether the concept is active, so we use active relations as a
    // proxy. If active relations exist, the concept is treated as active in this context.
    try {
      if (operator == null && snomedGraph.hasActiveRelations(conceptId, version)) {
        results.add(conceptId);
      }
    } catch (UnknownConceptException e) {
      throw new SimpleExpressionConstraintException(e.getMessage());
    }

    return results;
  }

  /** Checks whether an {@link ExpressionConstraint} is supported by this service. */
  static boolean isSupportedExpressionConstraint(ExpressionConstraint ec) {

    // case one: we have a simple SubExpression (e.g. << 102290001)
    // case two: we have a CompoundExpression (e.g. << 102290001 OR <<...)
    // case three: we have something else that is not supported
    return switch (ec) {
      case SubExpressionConstraint sub -> isSupportedSubExpressionConstraint(sub);
      case CompoundExpressionConstraint compound ->
          isSupportedCompoundExpressionConstraint(compound);
      default -> false;
    };
  }

  /** Checks whether a {@link SubExpressionConstraint} is supported. */
  static boolean isSupportedSubExpressionConstraint(SubExpressionConstraint sec) {

    // Default assumption: expression is supported.
    boolean isSupported = true;

    // Unsupported sub-expression features.
    if (sec.isWildcard()) {
      return false;
    }
    if (sec.getNestedExpressionConstraint() != null) {
      return false;
    }
    if (sec.getConceptFilterConstraints() != null) {
      return false;
    }
    if (sec.getDescriptionFilterConstraints() != null) {
      return false;
    }
    if (sec.getMemberFilterConstraints() != null) {
      return false;
    }
    if (sec.getHistorySupplement() != null) {
      return false;
    }
    if (sec.getMemberFieldsToReturn() != null) {
      return false;
    }
    if (sec.isReturnAllMemberFields()) {
      return false;
    }
    if (sec.getOperator() != null
        && sec.getOperator() != Operator.childof
        && sec.getOperator() != Operator.childorselfof
        && sec.getOperator() != Operator.parentof
        && sec.getOperator() != Operator.parentorselfof
        && sec.getOperator() != Operator.descendantof
        && sec.getOperator() != Operator.descendantorselfof
        && sec.getOperator() != Operator.ancestorof
        && sec.getOperator() != Operator.ancestororselfof) {
      return false;
    }

    return isSupported;
  }

  /** Checks whether a {@link CompoundExpressionConstraint} is supported. */
  static boolean isSupportedCompoundExpressionConstraint(CompoundExpressionConstraint cec) {

    // Default assumption: expression is supported.
    boolean isSupported = true;

    // Unsupported compound-expression features.
    if (cec.getConjunctionExpressionConstraints() != null) {
      return false;
    }
    if (cec.getExclusionExpressionConstraints() != null) {
      return false;
    }
    if (cec.getDisjunctionExpressionConstraints() == null) {
      return false;
    }
    for (SubExpressionConstraint sec : cec.getDisjunctionExpressionConstraints()) {
      if (!isSupportedSubExpressionConstraint(sec)) {
        return false;
      }
    }

    return isSupported;
  }
}
