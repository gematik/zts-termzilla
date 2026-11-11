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
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_TOKEN_LOWERCASE_EXACT;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Builds Lucene queries for FHIR reference search parameters.
 *
 * <p>The generator supports FHIR AND/OR token structures and currently handles {@code :missing} as
 * well as the chained {@code :identifier} variant.
 */
@Slf4j
public class ReferenceParameterQueryGenerator {

  /**
   * Creates a query for a {@link ReferenceAndListParam} using FHIR AND/OR semantics.
   *
   * @param andListParamData parameter wrapper containing the parameter name and value
   * @return composed Lucene query for all AND/OR branches
   * @throws NotImplementedException if a contained modifier is not supported
   */
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
   * Creates a query for a single reference parameter value.
   *
   * <p>Supported cases are {@code :missing} and {@code :identifier}. Unsupported modifiers raise a
   * {@link NotImplementedException}.
   *
   * @param paramName search parameter name
   * @param param reference value to convert
   * @return Lucene query for the provided reference value, or match-all for null input
   * @throws NotImplementedException if the modifier or chain is not supported
   */
  public Query prepareQuery(String paramName, ReferenceParam param) throws NotImplementedException {

    if (param == null) {
      return MatchAllDocsQuery.INSTANCE;
    } else if (param.getMissing() != null) {
      // :missing search modifier
      return QueryCombinators.prepareMissingModifierQuery(paramName, param);
    } else if (param.hasResourceType()) {
      if (param.getResourceType().contentEquals("identifier")) {
        return prepareIdentifierQuery(paramName, param);
      } else {
        throw new NotImplementedException("Modifier not supported");
      }
    }
    throw new NotImplementedException("Reference search without modifier chain is not supported");
  }

  /**
   * Creates an identifier-based reference query.
   *
   * @param paramName search parameter name
   * @param param reference parameter containing the identifier value
   * @return exact {@link TermQuery} against the token lowercase exact field
   */
  public Query prepareIdentifierQuery(String paramName, ReferenceParam param) {

    String termString = param.getValue().toLowerCase(Locale.ROOT);
    String inField = paramName + SEPARATOR + SUFFIX_TOKEN_LOWERCASE_EXACT;

    return new TermQuery(new Term(inField, termString));
  }
}
