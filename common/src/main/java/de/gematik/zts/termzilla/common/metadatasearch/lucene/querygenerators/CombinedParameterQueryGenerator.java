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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.InstantParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.date.PeriodParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.reference.CanonicalReferenceParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.reference.ReferenceParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.string.StringParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token.IdTokenParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.token.TokenParameterQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.uri.UriParameterQueryGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;

@Slf4j
public class CombinedParameterQueryGenerator {

  private StringParameterQueryGenerator stringParameterQueryGenerator;

  private ReferenceParameterQueryGenerator referenceParameterQueryGenerator;
  private CanonicalReferenceParameterQueryGenerator canonicalReferenceParameterQueryGenerator;

  public CombinedParameterQueryGenerator() {
    stringParameterQueryGenerator = new StringParameterQueryGenerator();
    canonicalReferenceParameterQueryGenerator = new CanonicalReferenceParameterQueryGenerator();
    referenceParameterQueryGenerator = new ReferenceParameterQueryGenerator();
  }

  public Query prepareAndListQuery(List<ParamData> paramDataList) throws NotImplementedException {

    List<Query> queryList = new ArrayList<>();

    // TODO   // Auseinandersetzen mit _content-Parameter und Aufbereiten für die Suche
    //    // Grundsätzlich erlauben wir am _content-Parameter KEINE Modifier wie :exact, :contains
    // etc.
    //    // Dies wird im Zuge der Parametervalidierung geprüft. Dennoch behandeln wir in der
    //    // Metadatensuche den _content-Parameter wie jeden anderen Parameter auch. Deshalb fügen
    // wir ihm
    //    // ein :contains an, um Inhalte überhaupt zu finden. Danach wird er zu den anderen
    //    // Suchparametern geschmissen. Perspektivisch ließe sich das natürlich auch mit
    // individuellen
    //    // QueryGeneratoren für die einzelnen Resourcentypen abfrühstücken. Dann müsste auch kein
    //    // content-Feld mehr erzeugt werden.

    // Berücksichtigen der Suchparameter
    for (ParamData paramData : paramDataList) {
      switch (paramData.getParamQueryType()) {
        case ParamData.ID_TOKEN_PARAM:
          queryList.add(IdTokenParameterQueryGenerator.prepareQuery(paramData));
          break;
        case ParamData.IDENTIFIER_TOKEN_PARAM,
        ParamData.BOOLEAN_TOKEN_PARAM,
        ParamData.CODE_TOKEN_PARAM,
        ParamData.CODING_TOKEN_PARAM,
        ParamData.CODABLECONCEPT_TOKEN_PARAM:
          queryList.add(TokenParameterQueryGenerator.prepareQuery(paramData));
          break;
        case ParamData.CANONICAL_REFERENCE_PARAM:
          queryList.add(canonicalReferenceParameterQueryGenerator.prepareQuery(paramData));
          break;
        case ParamData.INSTANT_DATE_PARAM:
          queryList.add(new InstantParameterQueryGenerator().prepareQuery(paramData));
          break;
        case ParamData.PERIOD_DATE_PARAM:
          queryList.add(new PeriodParameterQueryGenerator().prepareQuery(paramData));
          break;
        case ParamData.STRING_PARAM:
          queryList.add(stringParameterQueryGenerator.prepareQuery(paramData));
          break;
        case ParamData.REFERENCE_PARAM:
          queryList.add(referenceParameterQueryGenerator.prepareQuery(paramData));
          break;
        case ParamData.URI_PARAM:
          queryList.add(UriParameterQueryGenerator.prepareQuery(paramData));
          break;
        default:
          break;
      }
    }

    return QueryCombinators.prepareAndQuery(queryList);
  }
}
