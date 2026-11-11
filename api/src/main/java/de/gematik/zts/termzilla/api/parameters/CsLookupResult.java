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

import static ca.uhn.fhir.util.ParametersUtil.addParameterToParameters;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_CODE;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_CODING;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_DESIGNATION;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_DISPLAY;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_LANGUAGE;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_NAME;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_PROPERTY;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_SYSTEM;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_USE;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_VALUE;
import static de.gematik.zts.termzilla.api.parameters.model.ParametersConstants.PARAM_NAME_VERSION;

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.ParametersUtil;
import de.gematik.zts.termzilla.api.constants.FhirExtensionUrls;
import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.CodingConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.GenericConceptProperty;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.StringType;

@Getter
@Setter
@Slf4j
public class CsLookupResult {

  // Informationen über die Suche
  private String searchedForSystem;
  private String searchedForCode;
  private Set<String> searchedForProperties;
  private LanguageSelectionParameter lsp;

  // Informationen über das Ergebnis
  private boolean found;
  private String codeDisplay;
  private String codeSystemDisplayName;
  private String codeSystemVersion;
  private List<GenericConceptProperty> properties = new ArrayList<>();
  private List<ConceptDesignation> designations = new ArrayList<>();

  // -------------------------------------------------------------------

  public IBaseParameters toParameters(FhirContext context) {

    if (!this.found) {
      throw new ResourceNotFoundException(
          Msg.code(1738)
              + "Unable to find code '"
              + this.getSearchedForCode()
              + "' in system '"
              + this.getSearchedForSystem()
              + "|"
              + this.getCodeSystemVersion()
              + "'");
    }

    // Parameters Ressource anlegen
    IBaseParameters returnParameters = ParametersUtil.newInstance(context);

    // Displayname des CodeSystems
    if (StringUtils.isNotBlank(this.getCodeSystemDisplayName())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, PARAM_NAME_NAME, this.getCodeSystemDisplayName());
    }

    // Version des CodeSystems hinzufügen
    if (StringUtils.isNotBlank(this.getCodeSystemVersion())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, PARAM_NAME_VERSION, this.getCodeSystemVersion());
    }

    // Displayname des Codes
    // Für den, dass es keinen zur Sprache passenden Displaynamen gibt, geben wir einen Data Absent
    // Reason zurück,
    // damit der Client weiß, dass es nicht einfach nur leer ist, sondern tatsächlich keinen
    // passenden Displaynamen
    // (in der geforderten Sprache) gibt
    if (StringUtils.isNotBlank(this.getCodeDisplay())) {
      ParametersUtil.addParameterToParametersString(
          context, returnParameters, PARAM_NAME_DISPLAY, this.getCodeDisplay());
    } else {
      StringType value = new StringType();
      value.addExtension(FhirExtensionUrls.EXTENSION_DATA_ABSENT_REASON, new CodeType("unknown"));
      addParameterToParameters(context, returnParameters, PARAM_NAME_DISPLAY, value);
    }

    // Sofern wir Designations haben, fügen wir diese zum Ergebnis hinzu
    if (this.designations != null) {

      designations.forEach(
          designation -> {
            IBase property =
                addParameterToParameters(context, returnParameters, PARAM_NAME_DESIGNATION);
            ParametersUtil.addPartCode(
                context, property, PARAM_NAME_LANGUAGE, designation.getLanguage());
            ParametersUtil.addPartCoding(
                context,
                property,
                PARAM_NAME_USE,
                designation.getUseSystem(),
                designation.getUseCode(),
                designation.getUseDisplay());
            ParametersUtil.addPartString(
                context, property, PARAM_NAME_VALUE, designation.getValue());
          });
    }

    // Sofern wir Properties haben, fügen wir diese nach Bedarf zum Ergebnis hinzu
    if (this.properties != null) {

      // Filtern der zum Konzept zugehörigen properties entsprechend der angefragten Liste
      List<GenericConceptProperty> filteredProperties =
          this.properties.stream()
              .filter(p -> searchedForProperties.contains(p.getPropertyName()))
              .toList();

      for (GenericConceptProperty selectedProperty : filteredProperties) {

        // Anlegen des 'property'-Parameters
        IBase property = addParameterToParameters(context, returnParameters, PARAM_NAME_PROPERTY);

        // Hinzufügen des 'code'-Parameter-Parts mit dem Namen der Property
        ParametersUtil.addPartCode(
            context, property, PARAM_NAME_CODE, selectedProperty.getPropertyName());

        // Hinzufügen des 'value'-Parameter-Parts in Abhängigkeit vom Typ der Property
        switch (selectedProperty) {
          case BaseTypeConceptProperty prop ->
              // DateTime-Property
              ParametersUtil.addPart(context, property, PARAM_NAME_VALUE, prop.getValue());
          case CodingConceptProperty prop ->
              // Coding-Property
              addPartCoding(
                  context,
                  property,
                  PARAM_NAME_VALUE,
                  prop.getCodeSystem(),
                  prop.getVersion(),
                  prop.getCode(),
                  prop.getDisplay());
          default ->
              log.warn(
                  "Unsupported property type {} for property '{}'",
                  selectedProperty.getClass(),
                  selectedProperty.getPropertyName());
        }
      }
    }

    return returnParameters;
  }

  public Optional<String> getDesignation(String useSystem, String useCode, String language) {
    return this.designations.stream()
        .filter(
            designation ->
                designation.getUseSystem().equals(useSystem)
                    && designation.getUseCode().equals(useCode)
                    && designation.getLanguage().equals(language))
        .map(ConceptDesignation::getValue)
        .findFirst();
  }

  // -------------------------------------------------------------------

  /**
   * Fügt einen Coding-Parameter zu einem Parameter hinzu. Leider können wir die entsprechende
   * Methode des ParametersUtil nicht direkt übernehmen, da dort das Coding-Objekt keine Version
   * bekommt.
   *
   * @param theContext FhirContext
   * @param theParameter Parameter, zu dem der Coding-Parameter hinzugefügt werden soll
   * @param theName Name des Parameters
   * @param theSystem System des Codings
   * @param theCode Code des Codings
   * @param theDisplay Display des Codings
   */
  public static void addPartCoding(
      FhirContext theContext,
      IBase theParameter,
      String theName,
      String theSystem,
      String theVersion,
      String theCode,
      String theDisplay) {
    IBase coding = theContext.getElementDefinition(PARAM_NAME_CODING).newInstance();

    BaseRuntimeElementCompositeDefinition<?> codingDef =
        (BaseRuntimeElementCompositeDefinition<?>)
            theContext.getElementDefinition(coding.getClass());
    codingDef
        .getChildByName(PARAM_NAME_SYSTEM)
        .getMutator()
        .addValue(coding, ParametersUtil.createUri(theContext, theSystem));
    if (theVersion != null) {
      codingDef
          .getChildByName(PARAM_NAME_VERSION)
          .getMutator()
          .addValue(coding, ParametersUtil.createString(theContext, theVersion));
    }
    codingDef
        .getChildByName(PARAM_NAME_CODE)
        .getMutator()
        .addValue(coding, ParametersUtil.createCode(theContext, theCode));
    codingDef
        .getChildByName(PARAM_NAME_DISPLAY)
        .getMutator()
        .addValue(coding, ParametersUtil.createString(theContext, theDisplay));

    ParametersUtil.addPart(theContext, theParameter, theName, coding);
  }
}
