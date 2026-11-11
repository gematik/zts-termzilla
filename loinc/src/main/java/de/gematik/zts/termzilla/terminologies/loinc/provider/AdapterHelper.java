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
package de.gematik.zts.termzilla.terminologies.loinc.provider;

import de.gematik.zts.termzilla.api.interfaces.ConceptDesignation;
import de.gematik.zts.termzilla.api.interfaces.properties.BaseTypeConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.CodingConceptProperty;
import de.gematik.zts.termzilla.api.interfaces.properties.ConceptProperties;
import de.gematik.zts.termzilla.api.interfaces.properties.GenericConceptProperty;
import de.gematik.zts.termzilla.terminologies.loinc.Loinc;
import de.gematik.zts.termzilla.terminologies.loinc.LoincConstants;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.ConceptVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.LoincPartLinkVersion;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.PartVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincConcept;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincDesignation;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.BooleanType;

public class AdapterHelper {

  private AdapterHelper() {
    // Hilfsklasse mit statischen Methoden - Instanziierung nicht notwendig
  }

  // ----------------------------------------------------------------------------------------
  // Umgang mit Designations
  // ----------------------------------------------------------------------------------------

  public static List<ConceptDesignation> convertLoincConceptDesignationListToFhirDesignationList(
      List<LoincDesignation> loincConceptDesignationList) {

    // Vorbereiten der Ergebnisliste
    List<ConceptDesignation> fhirDesignationList = new ArrayList<>();

    // Iterieren über die Liste der SnomedConceptDescription-Objekte
    loincConceptDesignationList.stream()
        .forEach(
            designation ->
                fhirDesignationList.add(
                    convertLoincConceptDesignationToFhirDesignation(designation)));
    return fhirDesignationList;
  }

  /**
   * Generiert aus einer LoincConceptDescription eine FHIR-ConceptDesignation.
   *
   * @param loincConceptDesignation Das LoincConceptDescription-Objekt, aus dem die
   *     FHIR-ConceptDesignation generiert werden soll
   * @return FHIR-ConceptDesignation
   */
  private static ConceptDesignation convertLoincConceptDesignationToFhirDesignation(
      LoincDesignation loincConceptDesignation) {
    ConceptDesignation fhirDesignation = new ConceptDesignation();

    fhirDesignation.setLanguage(loincConceptDesignation.getLanguage());
    fhirDesignation.setUseSystem(LoincConstants.TERMINOLOGY_CANONICAL_NAME);
    fhirDesignation.setUseCode(loincConceptDesignation.getType());
    fhirDesignation.setValue(loincConceptDesignation.getDesignation());
    return fhirDesignation;
  }

  // ----------------------------------------------------------------------------------------
  // Umgang mit Properties
  // ----------------------------------------------------------------------------------------

  /**
   * Generiert aus einem LoincConcept eine Liste von FHIR-ConceptProperties.
   *
   * @param loincConcept Das LoincConcept, aus dem die Properties generiert werden sollen
   * @param requestedProperties Die Liste der gewünschten Properties, für die eine
   *     FHIR-Repräsentation erstellt werden soll
   * @param version Die LoincVersion, für die die Properties generiert werden sollen
   * @param loinc Das Loinc-Objekt, das die LoincInMemoryRepresentation enthält
   * @return Eine Liste von FHIR-ConceptProperties
   */
  public static List<GenericConceptProperty> convertLoincConceptToFhirConceptPropertyList(
      LoincConcept loincConcept,
      Set<String> requestedProperties,
      LoincVersion version,
      Loinc loinc) {

    // Vorbereiten einer leeren Ergebnisliste
    List<GenericConceptProperty> resultList = new ArrayList<>();

    // Hinzufügen der 'inactive'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptProperties.PROPERTY_NAME_INACTIVE)) {
      GenericConceptProperty inactiveProperty =
          new BaseTypeConceptProperty(
              ConceptProperties.PROPERTY_NAME_INACTIVE, new BooleanType(!loincConcept.isActive()));
      resultList.add(inactiveProperty);
    }

    // Hinzufügen der 'COMPONENT'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptVersion.NAME_COMPONENT)) {

      addPropertyWithDisplay(
          resultList,
          loinc
              .getLoincInMemoryRepresentation()
              .getComponentLoincNumberToLoincPartLinkVersionMap()
              .get(loincConcept.getCode()),
          ConceptVersion.NAME_COMPONENT,
          version,
          loinc);
    }

    // Hinzufügen der 'PROPERTY'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptVersion.NAME_PROPERTY)) {

      addPropertyWithDisplay(
          resultList,
          loinc
              .getLoincInMemoryRepresentation()
              .getPropertyLoincNumberToLoincPartLinkVersionMap()
              .get(loincConcept.getCode()),
          ConceptVersion.NAME_PROPERTY,
          version,
          loinc);
    }

    // Hinzufügen der 'TIME_ASPCT'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptVersion.NAME_TIME_ASPCT)) {

      addPropertyWithDisplay(
          resultList,
          loinc
              .getLoincInMemoryRepresentation()
              .getTimeLoincNumberToLoincPartLinkVersionMap()
              .get(loincConcept.getCode()),
          ConceptVersion.NAME_TIME_ASPCT,
          version,
          loinc);
    }

    // Hinzufügen der 'SYSTEM'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptVersion.NAME_SYSTEM)) {

      addPropertyWithDisplay(
          resultList,
          loinc
              .getLoincInMemoryRepresentation()
              .getSystemLoincNumberToLoincPartLinkVersionMap()
              .get(loincConcept.getCode()),
          ConceptVersion.NAME_SYSTEM,
          version,
          loinc);
    }

    // Hinzufügen der 'SCALE_TYP'-Property, falls gewünscht
    if (requestedProperties.contains(ConceptVersion.NAME_SCALE_TYP)) {

      addPropertyWithDisplay(
          resultList,
          loinc
              .getLoincInMemoryRepresentation()
              .getScaleLoincNumberToLoincPartLinkVersionMap()
              .get(loincConcept.getCode()),
          ConceptVersion.NAME_SCALE_TYP,
          version,
          loinc);
    }

    return resultList;
  }

  private static List<GenericConceptProperty> addPropertyWithDisplay(
      List<GenericConceptProperty> resultList,
      Collection<LoincPartLinkVersion> partLinkCollection,
      String propertyName,
      LoincVersion version,
      Loinc loinc) {

    partLinkCollection.stream()
        // Heraussuchen der grundsätzlich in Frage kommenden PartLinks
        .filter(lpl -> lpl.getValidFromVersion() <= version.getVersionNumber())
        // Vom Ergebnis nutzen wir die neueste Version
        .max(Comparator.comparing(LoincPartLinkVersion::getValidFromVersion))
        // Sofern wir einen Wert haben, fügen wir diesen hinzu
        .ifPresent(
            pl ->
                loinc
                    .getLoincInMemoryRepresentation()
                    .getPartNumberToPartVersionMap()
                    // Heraussuchen der infrage kommenden LoincPartVersionen
                    .get(pl.getPartNumber())
                    .stream()
                    // Selektieren der PartVersion mit der neuesten Version
                    .filter(pv -> pv.getValidFromVersion() <= version.getVersionNumber())
                    .max(Comparator.comparing(PartVersion::getValidFromVersion))
                    .ifPresent(
                        pv ->
                            resultList.add(
                                new CodingConceptProperty(
                                    propertyName,
                                    pl.getPartNumber(),
                                    LoincConstants.TERMINOLOGY_CANONICAL_NAME,
                                    null,
                                    pv.getPartName()))));

    return resultList;
  }
}
