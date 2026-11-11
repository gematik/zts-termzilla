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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.reference;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_REFERENCE_EXACT;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Specializes reference query generation for canonical reference parameters.
 *
 * <p>Canonical references are indexed as exact reference values and support {@code :missing}
 * checks.
 */
@Slf4j
public class CanonicalReferenceParameterQueryGenerator extends ReferenceParameterQueryGenerator {

  /**
   * Creates a query for canonical references using FHIR AND/OR semantics.
   *
   * @param andListParamData parameter wrapper containing the parameter name and value
   * @return composed Lucene query for all AND/OR branches
   * @throws NotImplementedException if a contained modifier is not supported
   */
  @Override
  public Query prepareQuery(ParamData andListParamData) throws NotImplementedException {

    ReferenceAndListParam andListParam = (ReferenceAndListParam) andListParamData.getParam();
    List<Query> andQueryList = new ArrayList<>();

    for (ReferenceOrListParam orListParam : andListParam.getValuesAsQueryTokens()) {
      List<Query> orQueryList = new ArrayList<>();
      for (ReferenceParam param : orListParam.getValuesAsQueryTokens()) {
        orQueryList.add(prepareQuery(andListParamData.getParamName(), param));
      }
      andQueryList.add(QueryCombinators.prepareOrQuery(orQueryList));
    }
    return QueryCombinators.prepareAndQuery(andQueryList);
  }

  /**
   * Creates a query for one canonical reference value.
   *
   * @param paramName search parameter name
   * @param param canonical reference value
   * @return missing-modifier query or exact canonical term query
   * @throws NotImplementedException if query generation fails for a supported branch
   */
  @Override
  public Query prepareQuery(String paramName, ReferenceParam param) throws NotImplementedException {

    return param.getMissing() != null && param.getMissing()
        ? QueryCombinators.prepareMissingModifierQuery(paramName, param)
        : prepareExactQuery(paramName, param);
  }

  /**
   * Creates an exact-term query for canonical reference values.
   *
   * @param paramName search parameter name
   * @param param canonical reference parameter
   * @return exact {@link TermQuery}, or {@link MatchAllDocsQuery} when no value is provided
   */
  public Query prepareExactQuery(String paramName, ReferenceParam param) {

    String termString;

    if (!StringUtils.isEmpty(param.getValue())) {
      termString = param.getValue();
    } else {
      return MatchAllDocsQuery.INSTANCE;
    }

    String inField = paramName + SEPARATOR + SUFFIX_REFERENCE_EXACT;

    return new TermQuery(new Term(inField, termString));
  }
}
