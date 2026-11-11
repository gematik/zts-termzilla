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
package de.gematik.zts.termzilla.terminologies.snomed.lucene.queries;

import de.gematik.zts.termzilla.terminologies.snomed.lucene.loader.DescriptionLoader;
import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedReleaseVersion;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;

/**
 * Factory for commonly used Lucene queries in the SNOMED CT index.
 *
 * <p>Provides static methods for constructing version-aware range queries, module-ID filters, and
 * boolean query compositions used throughout the SNOMED Lucene layer.
 */
public final class LuceneQueryFactory {

  /** Prevents instantiation of this utility class. */
  private LuceneQueryFactory() {}

  /**
   * Creates a query that matches all indexed documents.
   *
   * @return a {@link MatchAllDocsQuery}
   */
  public static Query prepareSearchAllQuery() {
    return MatchAllDocsQuery.INSTANCE;
  }

  /**
   * Creates a disjunction of module/effective-time constraints for the base release and all
   * configured dependencies.
   *
   * @param releaseVersion the SNOMED release version including dependent module versions
   * @return an OR-composed query covering all relevant module/version pairs
   */
  public static Query prepareReleaseVersionRangeQuery(SnomedReleaseVersion releaseVersion) {

    List<Query> orQueryList = new ArrayList<>();

    // search for entries with the moduleId/effective of the version
    orQueryList.add(
        prepareModuleIdEffectiveTimeRangeQuery(
            releaseVersion.getModuleId(), releaseVersion.getDate()));

    // search for entries with the moduleId/effective of the dependencies
    for (String dependency : releaseVersion.getDependencies().keySet()) {
      orQueryList.add(
          prepareModuleIdEffectiveTimeRangeQuery(
              dependency, releaseVersion.getDependencies().get(dependency)));
    }

    return prepareOrQuery(orQueryList);
  }

  /**
   * Creates a conjunction that matches one module and all entries up to a specific effective time.
   *
   * @param moduleId the SNOMED module identifier
   * @param effectiveTime the inclusive upper bound effective time in {@code yyyyMMdd} format
   * @return an AND-composed query for module and effective-time constraints
   */
  public static Query prepareModuleIdEffectiveTimeRangeQuery(
      String moduleId, String effectiveTime) {
    List<Query> andQueryList = new ArrayList<>();
    andQueryList.add(prepareModuleIdQuery(moduleId));
    andQueryList.add(prepareEffectiveTimeRangeQuery(effectiveTime));
    return prepareAndQuery(andQueryList);
  }

  /**
   * Creates an exact-term query for the module identifier field.
   *
   * @param moduleId the SNOMED module identifier
   * @return a term query for {@link DescriptionLoader#INDEX_FIELDNAME_MODULE_ID}
   */
  public static Query prepareModuleIdQuery(String moduleId) {
    return new TermQuery(new Term(DescriptionLoader.INDEX_FIELDNAME_MODULE_ID, moduleId));
  }

  /**
   * Creates an inclusive effective-time range query from the minimal bound to the provided bound.
   *
   * @param effectiveTime the inclusive upper bound effective time in {@code yyyyMMdd} format
   * @return a range query for {@link DescriptionLoader#INDEX_FIELDNAME_EFFECTIVE_TIME}
   */
  public static Query prepareEffectiveTimeRangeQuery(String effectiveTime) {
    return new TermRangeQuery(
        DescriptionLoader.INDEX_FIELDNAME_EFFECTIVE_TIME,
        new BytesRef("00000000"),
        new BytesRef(effectiveTime),
        true,
        true);
  }

  /**
   * Combines queries with boolean {@link BooleanClause.Occur#MUST} semantics.
   *
   * @param queryList queries to combine
   * @return an AND-composed boolean query
   */
  public static Query prepareAndQuery(List<Query> queryList) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (Query query : queryList) {
      builder.add(query, BooleanClause.Occur.MUST);
    }
    return builder.build();
  }

  /**
   * Combines queries with boolean {@link BooleanClause.Occur#SHOULD} semantics.
   *
   * @param queryList queries to combine
   * @return an OR-composed boolean query
   */
  public static Query prepareOrQuery(List<Query> queryList) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (Query query : queryList) {
      builder.add(query, BooleanClause.Occur.SHOULD);
    }
    return builder.build();
  }
}
