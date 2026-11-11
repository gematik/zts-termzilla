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
package de.gematik.zts.termzilla.api.parameters;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import de.gematik.zts.termzilla.api.parameters.model.Match;
import de.gematik.zts.termzilla.api.parameters.model.PropertyValuePair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Coding;

public class CsFindMatchesResult {

  @Getter @Setter private List<Match> matchList = new ArrayList<>();

  // ==============================================================================================

  /**
   * Konvertiert das Objekt in ein Parameters-Objekt, welches über die FHIR-Schnittstelle
   * zurückgegeben werden kann.
   *
   * @param context FHIR-Kontext
   * @return Parameters-Objekt
   */
  public IBaseParameters toParameters(FhirContext context) {

    // Parameters Ressource anlegen
    IBaseParameters returnParameters = ParametersUtil.newInstance(context);

    for (Match match : matchList) {
      addMatch(context, returnParameters, match);
    }

    return returnParameters;
  }

  // ==============================================================================================
  // Helper Methoden
  // ==============================================================================================

  /**
   * Fügt ein Match-Objekt zu einem bestehenden Parameters-Objekt hinzu.
   *
   * @param context FHIR-Kontext
   * @param parameter Parameters-Objekt
   * @param theMatch Match-Objekt
   */
  private static void addMatch(FhirContext context, IBaseParameters parameter, Match theMatch) {

    // Match-Parameter erzeugen
    IBase matchParam = ParametersUtil.addParameterToParameters(context, parameter, "match");

    // Coding-Parameter hinzufügen
    addPartCoding(
        context,
        matchParam,
        "code",
        theMatch.getCoding().getSystem(),
        Optional.ofNullable(theMatch).map(Match::getCoding).map(Coding::getVersion),
        theMatch.getCoding().getCode(),
        Optional.ofNullable(theMatch).map(Match::getCoding).map(Coding::getDisplay));

    // Unmatched-Parameter hinzufügen
    if (theMatch.getUnmatchedList() != null) {
      for (PropertyValuePair unmatched : theMatch.getUnmatchedList()) {
        IBase unmatchedParam = ParametersUtil.addPart(context, matchParam, "unmatched", null);

        // Code-Parameter hinzufügen
        ParametersUtil.addPartCode(context, unmatchedParam, "code", unmatched.getCode());

        // Value-Parameter hinzufügen
        ParametersUtil.addPart(context, unmatchedParam, "value", unmatched.getValue());
      }
    }

    // TODO, if both exist, exact and possible match, things get overritten, is this correct?
    String comment = "";

    // ExactMatch-Parameter zur Info hinzufügen
    if (theMatch.getExactMatchList() != null && !theMatch.getExactMatchList().isEmpty()) {
      comment = "Exact Matches: ";
      for (PropertyValuePair exactMatch : theMatch.getExactMatchList()) {
        comment += "(" + exactMatch.getCode() + "=" + exactMatch.getValue() + ") ";
      }
    }

    // ExactMatch-Parameter zur Info hinzufügen
    if (theMatch.getPartialMatchList() != null && !theMatch.getPartialMatchList().isEmpty()) {
      comment = "Possible Matches: ";
      for (PropertyValuePair partialMatch : theMatch.getPartialMatchList()) {
        comment += "(" + partialMatch.getCode() + "=" + partialMatch.getValue() + ") ";
      }
    }

    // Comment-Parameter hinzufügen
    if (StringUtils.isNotEmpty(comment)) {
      ParametersUtil.addPartString(context, matchParam, "comment", comment.trim());
    }
  }

  /**
   * Fügt einen Coding-Parameter zu einem bestehenden Parameter hinzu.
   *
   * @param theContext FHIR-Kontext
   * @param theParameter Parameter, zu dem der Coding-Parameter hinzugefügt werden soll
   * @param theName Name des Coding-Parameters
   * @param theSystem System des Coding-Parameters
   * @param theVersion Version des Coding-Parameters
   * @param theCode Code des Coding-Parameters
   * @param theDisplay Display des Coding-Parameters
   */
  private static void addPartCoding(
      FhirContext theContext,
      IBase theParameter,
      String theName,
      String theSystem,
      Optional<String> theVersion,
      String theCode,
      Optional<String> theDisplay) {

    IBase coding = theContext.getElementDefinition("coding").newInstance();
    BaseRuntimeElementCompositeDefinition<?> codingDef =
        (BaseRuntimeElementCompositeDefinition) theContext.getElementDefinition(coding.getClass());

    // system-Element
    codingDef
        .getChildByName("system")
        .getMutator()
        .addValue(coding, ParametersUtil.createUri(theContext, theSystem));

    // version-Element
    if (theVersion.isPresent()) {
      codingDef
          .getChildByName("version")
          .getMutator()
          .addValue(coding, ParametersUtil.createString(theContext, theVersion.get()));
    }

    // code-Element
    codingDef
        .getChildByName("code")
        .getMutator()
        .addValue(coding, ParametersUtil.createCode(theContext, theCode));

    // display-Element
    if (theDisplay.isPresent()) {
      codingDef
          .getChildByName("display")
          .getMutator()
          .addValue(coding, ParametersUtil.createString(theContext, theDisplay.get()));
    }
    ParametersUtil.addPart(theContext, theParameter, theName, coding);
  }

  // ==============================================================================================
  // Methoden zur Bearbeitung der Match-Liste
  // ==============================================================================================

}
