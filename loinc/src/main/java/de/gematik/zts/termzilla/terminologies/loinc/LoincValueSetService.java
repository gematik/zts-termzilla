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
package de.gematik.zts.termzilla.terminologies.loinc;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.interfaces.ConceptBaseInfo;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.api.service.ValueSetSvcInterface;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.common.VsExpandPartResult;
import de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel.AnswerListVersion;
import de.gematik.zts.termzilla.terminologies.loinc.model.LoincVersion;
import de.gematik.zts.termzilla.terminologies.loinc.provider.vs.expand.LoincVsExplicitExpandOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class LoincValueSetService implements ValueSetSvcInterface {

  @Getter private final Loinc loinc;
  @Getter private final LoincTerminologyManager loincTerminologyManager;

  // from the Spec:
  // http://loinc.org/vs is a value set that contains all LOINC codes.
  // http://loinc.org/vs/[id]: LOINC defines a set of Answer lists, each of which contains a set
  // of LOINC codes. LOINC
  // answer lists are value sets. The value set identifier "http://loinc.org/vs/[id]" identifies a
  // value set that contains a set of LOINC codes. (e.g. http://loinc.org/vs/LL715-4)
  private final Map<String, ValueSet> valueSetMap = new HashMap<>();

  // Operationen
  private final LoincVsExplicitExpandOperation loincVsExplicitExpandOperation;

  // --------------------------------------------------------------------------------

  public LoincValueSetService(LoincTerminologyManager loincTerminologyManager) {

    this.loincTerminologyManager = loincTerminologyManager;
    this.loinc = loincTerminologyManager.getLoinc();

    // Initialisieren der ValueSets
    this.generateImplicitValueSetsForAllLoincConceptsInAllSupportedVersions();
    this.generateImplicitValueSetsForAllAnswerListsInAllSupportedVersions();

    // Initialize Operations
    this.loincVsExplicitExpandOperation = new LoincVsExplicitExpandOperation(this);
  }

  // ----------------------------------------------------------------------------------------
  // Initialization methods
  // ----------------------------------------------------------------------------------------

  private void generateImplicitValueSetsForAllLoincConceptsInAllSupportedVersions() {

    for (LoincVersion loincVersion : loinc.getSupportedLoincVersions()) {
      String implicitVsUri = "http://loinc.org/vs";

      ValueSet valueSet = new ValueSet();
      valueSet.setId(
          UUID.nameUUIDFromBytes((implicitVsUri + "|" + loincVersion.getVersionString()).getBytes())
              .toString());
      valueSet.setUrl(implicitVsUri);
      valueSet.setVersion(loincVersion.getVersionString());
      valueSet.setName("LoincConcepts");
      valueSet.setTitle("Loinc Concepts");
      valueSet.setDescription("Contains all LOINC Concepts");

      valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

      ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
      include.setSystem("http://loinc.org");
      include.setVersion(loincVersion.getVersionString());

      ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
      compose.addInclude(include);
      valueSet.setCompose(compose);

      valueSetMap.put(valueSet.getId(), valueSet);
    }
  }

  private void generateImplicitValueSetsForAllAnswerListsInAllSupportedVersions() {

    for (LoincVersion loincVersion : loinc.getSupportedLoincVersions()) {
      for (String answerListId :
          loinc.getLoincInMemoryRepresentation().getAnswerListIdToAnswerListVersionMap().keySet()) {

        AnswerListVersion answerListVersion =
            loinc
                .getLoincInMemoryRepresentation()
                .getAnswerListVersion(answerListId, loincVersion)
                .orElse(null);

        if (answerListVersion != null) {
          String implicitVsUri = "http://loinc.org/vs/" + answerListId;

          ValueSet valueSet = new ValueSet();
          valueSet.setId(
              UUID.nameUUIDFromBytes(
                      (implicitVsUri + "|" + loincVersion.getVersionString()).getBytes())
                  .toString());
          valueSet.setUrl(implicitVsUri);
          valueSet.setVersion(loincVersion.getVersionString());
          valueSet.setName(answerListVersion.getAnswerListName());
          valueSet.setTitle(answerListVersion.getAnswerListName());
          valueSet.setDescription("LOINC Answer List for " + answerListVersion.getAnswerListName());

          // TODO: set status based on answerListVersion.getStatus()
          valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

          ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
          include.setSystem("http://loinc.org");
          include.setVersion(loincVersion.getVersionString());
          for (String answerId : answerListVersion.getAnswerIds()) {
            include.addConcept().setCode(answerId);
          }

          ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
          compose.addInclude(include);
          valueSet.setCompose(compose);

          valueSetMap.put(valueSet.getId(), valueSet);
        }
      }
    }
  }

  // --------------------------------------------------------------------------------

  @Override
  public boolean isValueSetSupported(String scope, String url, String version)
      throws UnknownScopeException {

    if (version != null) {
      return valueSetMap.values().stream()
          .filter(valueSet -> valueSet.getUrl().equals(url))
          .anyMatch(valueSet -> valueSet.getVersion().equals(version));
    } else {
      return valueSetMap.values().stream().anyMatch(valueSet -> valueSet.getUrl().equals(url));
    }
  }

  @Override
  public List<String> getSupportedValueSetCanonicalList() {

    // Iterieren über alle ValueSets und sammeln der Canonical URLs in einer Liste
    return valueSetMap.values().stream().map(ValueSet::getUrl).distinct().toList();
  }

  /**
   * Abfrage aller vom jeweiligen TerminologyManager verwalteten ValueSets. Hinweis: Er wird
   * keinerlei Einschränkung bezüglich des Scopes vorgenommen, d.h. die ValueSets aller Scopes
   * werden zurückgegeben.
   *
   * @return Liste mit ValueSets
   */
  @Override
  public List<ValueSet> getValueSetList() {
    return new ArrayList<>(valueSetMap.values());
  }

  /**
   * Abfrage eines einzelnen ValueSets anhand seiner Id und des Scopes
   *
   * @param scope Scope der Anfrage
   * @param theId Id des CodeSystems
   * @return ValueSet
   */
  @Override
  public ValueSet getValueSet(String scope, IdType theId, ResourceRetrievalMode retrievalMode) {
    return valueSetMap.get(theId.getIdPart());
  }

  /**
   * Abfrage eines einzelnen ValueSets anhand seiner Id und des Scopes
   *
   * @param scope Scope der Anfrage
   * @param theId Id des CodeSystems
   * @return ValueSet
   */
  @Override
  public ValueSet getValueSet(String scope, IdType theId) {
    return getValueSet(scope, theId, ResourceRetrievalMode.FULL);
  }

  /**
   * Abfrage von ValueSets anhand einer Canonical Url. Da wir verschiedene Versionen des ValueSets
   * haben können, wird eine Liste zurückgegeben. Diese Liste wird gefiltert, um nur solche
   * ValueSets zurückzugeben, die dem übergebenen Scope entsprechen.
   *
   * @param scope Scope der Anfrage
   * @param url Canonical Url der ValueSets
   * @return Liste mit ValueSets
   */
  @Override
  public List<ValueSet> getValueSetList(String scope, String url) {
    return valueSetMap.values().stream().filter(vs -> vs.getUrl().equals(url)).toList();
  }

  /**
   * Abfrage eines ValueSets anhand seiner Canonical Url und Version. Da wir verschiedene Varianten
   * des ValueSets für verschiedne Scopes haben können, müsen wir sicherstellen, dass das
   * zurückgegebene ValueSet den Scope-Kriterien entspricht.
   *
   * @param scope Scope der Anfrage
   * @param url Canonical Url des ValueSets
   * @param version Version des ValueSets
   * @return ValueSet mit der entsprechenden Canonical Url und Version
   */
  @Override
  public ValueSet getValueSet(
      String scope, String url, String version, ResourceRetrievalMode retrievalMode)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {

    // TODO: Scope behandeln oder zumindest überlegen, ob wir LOINC in jedem Scope unterstützen
    String selectedVersion;

    if (version == null) {
      // Wenn keine Version angegeben ist, nehmen wir die aktuelle Version
      selectedVersion = loinc.getDefaultLoincVersion().getVersionString();
    } else {
      selectedVersion = version;
    }

    return valueSetMap.values().stream()
        .filter(valueSet -> valueSet.getUrl().equals(url))
        .filter(valueSet -> valueSet.getVersion().equals(selectedVersion))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "ValueSet with 'url' = "
                        + url
                        + " and 'version' = "
                        + version
                        + " not found."));
  }

  /**
   * Abfrage eines ValueSets anhand seiner Canonical Url und Version. Da wir verschiedene Varianten
   * des ValueSets für verschiedne Scopes haben können, müsen wir sicherstellen, dass das
   * zurückgegebene ValueSet den Scope-Kriterien entspricht.
   *
   * @param scope Scope der Anfrage
   * @param url Canonical Url des ValueSets
   * @param version Version des ValueSets
   * @return ValueSet mit der entsprechenden Canonical Url und Version
   */
  @Override
  public ValueSet getValueSet(String scope, String url, String version)
      throws ResourceNotFoundException, UnknownScopeException, UnsupportedTerminologyException {

    return getValueSet(scope, url, version, ResourceRetrievalMode.FULL);
  }

  /**
   * Suchen nach ValueSets anhand übergebener Suchparameter innerhalb eines Scopes
   *
   * @param scope Scope der Suche
   * @param vsSearchParameter Suchparameter in Anlehnung an die FHIR-Spezifikation (Suchparameter)
   * @return Liste der 'id's der gefundenen ValueSets
   * @throws NotImplementedException Suchfunktion oder Teil der Suchfunktion nicht implementiert
   */
  @Override
  public List<IdType> searchValueSet(String scope, VsSearchParameter vsSearchParameter)
      throws NotImplementedException, UnknownScopeException {
    return List.of();
  }

  /**
   * Erstellen eines ValueSet.ValueSetExpansionContainsComponent-Objektes (Repräsentation eines
   * Konzeptes)
   *
   * @param scope scope in dem die Expansion durchgeführt wird
   * @param conceptBaseInfo Konzept, das in die Expansion aufgenommen werden soll
   * @param includeDesignations Flag, ob auch Designationen in die Expansion aufgenommen werden
   *     sollen
   * @param designationParamValueList Liste der Designationsparameter
   * @param displayLanguage Sprache, in der die Designationen zurückgegeben werden sollen
   * @param languageSelectionParameter Parameter zur Sprachauswahl für die Ermittlung
   *     des Display-Wertes und der Designationen
   * @param explicitConceptReferenceMap Map mit expliziten Konzeptreferenzen
   *     aus der ValueSet-Definition, die unter anderem zur Übernahme von
   *     Extensions verwendet werden
   * @return ValueSet.ValueSetExpansionContainsComponent-Objekt
   */
  @Override
  public ValueSet.ValueSetExpansionContainsComponent createExpansionContainsComponent(
      String scope,
      ConceptBaseInfo conceptBaseInfo,
      boolean includeDesignations,
      List<String> designationParamValueList,
      String displayLanguage,
      LanguageSelectionParameter languageSelectionParameter,
      MultiValuedMap<Integer, ValueSet.ConceptReferenceComponent> explicitConceptReferenceMap)
      throws UnsupportedTerminologyException {

    return this.loincVsExplicitExpandOperation.createExpansionContainsComponent(
        conceptBaseInfo,
        includeDesignations,
        designationParamValueList,
        languageSelectionParameter,
        explicitConceptReferenceMap);
  }

  /**
   * Expansion eines includes/excludes einer potentiell komplexen ValueSet-Definition
   *
   * @param scope scope in dem die Expansion durchgeführt wird
   * @param include ValueSet.compose.include-Element
   * @param includeInactiveConcepts Inkludieren von inaktiven Konzepten
   * @param version zu verwendende Version, falls nicht explizit angegeben
   * @param checkVersion zu verwendende Version, falls nicht explizit angegeben, fail, wenn andere
   *     Version angegeben wird
   * @param forceVersion verpflichtend zu verwendende Version (egal was sonst definiert wurde)
   * @param filter Filter-String, der bei der Expansion berücksichtigt werden soll
   * @return Ergebnis der Expansion des include/exclude-Elements
   */
  @Override
  public VsExpandPartResult expandInclude(
      String scope,
      ValueSet.ConceptSetComponent include,
      boolean includeInactiveConcepts,
      String version,
      String checkVersion,
      String forceVersion,
      String filter)
      throws UnsupportedTerminologyException, NotImplementedException {

    // Durchführen der Expansion
    return this.loincVsExplicitExpandOperation.expandInclude(
        include, includeInactiveConcepts, version, checkVersion, forceVersion, filter);
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

}
