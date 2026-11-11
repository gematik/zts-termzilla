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
package de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.findmatches;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.api.parameters.model.Match;
import de.gematik.zts.termzilla.api.parameters.model.PropertyValuePair;
import de.gematik.zts.termzilla.api.service.ResourceRetrievalMode;
import de.gematik.zts.termzilla.common.metadatasearch.helper.CodeSystemConceptSearchHelper;
import de.gematik.zts.termzilla.common.metadatasearch.helper.model.ScoredCoding;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ConceptQueryGenerator;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.PropertyFilter;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import de.gematik.zts.termzilla.terminologies.common.LanguageSelectionParameter;
import de.gematik.zts.termzilla.terminologies.fhirterms.FhirTermsTerminologyManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.helper.FhirDesignationManager;
import de.gematik.zts.termzilla.terminologies.fhirterms.memory.FhirTermsInMemoryRepresentation;
import de.gematik.zts.termzilla.terminologies.fhirterms.provider.cs.CodeSystemHelper;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.Query;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Type;

// TODO: separaten Refactoring-Task für diese Klasse aufmachen! Das ist aktuell eine PoC
// Implementierung,
//  die wir in den nächsten Wochen noch deutlich verbessern und erweitern wollen/müssen.

/**
 * Implements the FHIR {@code CodeSystem/$find-matches} operation for FHIR package-based code
 * systems.
 *
 * <p>Translates property-based search criteria into Lucene queries against the in-memory concept
 * index. Each property filter produces a scored query; results are combined, graded into exact,
 * partial, and unmatched categories using bitwise boost-score analysis, and returned as a {@link
 * CsFindMatchesResult}.
 *
 * @see <a href="https://hl7.org/fhir/R4/codesystem-operation-find-matches.html">FHIR R4
 *     CodeSystem/$find-matches</a>
 */
@Slf4j
public class FhirTermsCodeSystemFindMatchesOperation {

  private final FhirTermsInMemoryRepresentation fhirTermsInMemoryRepresentation;

  /**
   * Creates a new instance backed by the given {@link FhirTermsTerminologyManager}.
   *
   * @param fhirTermsTerminologyManager the terminology manager providing access to the in-memory
   *     representation and Lucene index
   */
  public FhirTermsCodeSystemFindMatchesOperation(
      FhirTermsTerminologyManager fhirTermsTerminologyManager) {
    this.fhirTermsInMemoryRepresentation =
        fhirTermsTerminologyManager.getFhirTermsInMemoryRepresentation();
  }

  // ----------------------------------------------------------------------------------------
  // CodeSystem/$find-matches
  // ----------------------------------------------------------------------------------------

  /**
   * Finds all concepts in a code system that match the given property-based search criteria.
   *
   * <p>Validates the parameters, resolves the target code system, builds Lucene queries for each
   * property filter, executes the combined query, and assembles the graded result.
   *
   * @param param the search criteria including system URL, version, match mode, and properties
   * @param scope the scope of the request
   * @return the result containing all matched concepts with their graded match details
   * @throws InvalidRequestException if the request parameters are invalid or incomplete
   * @throws UnsupportedTerminologyException if the requested code system or version is not
   *     supported
   * @throws NotImplementedException if a requested feature is not yet implemented
   * @throws UnknownScopeException if the requested scope is not known
   */
  public CsFindMatchesResult findMatches(CsFindMatchesParameter param, String scope)
      throws InvalidRequestException,
          UnsupportedTerminologyException,
          NotImplementedException,
          UnknownScopeException {

    // Validierung der übergebenen $find-matches Parameter
    new FhirTermsCodeSystemFindMatchesParameterValidator().validateParameters(param);

    // Überführen der Suchkriterien in eine für uns angenehmere Form
    String system = param.getSystemParam().getValue();
    String version = param.getVersionParam() != null ? param.getVersionParam().getValue() : null;
    LanguageSelectionParameter lsp =
        LanguageSelectionParameter.builder()
            .acceptLanguageHeader(param.getLanguageAcceptLanguageHeader())
            .systemDefaultLanguage(param.getLanguageDefaultSystemLanguage())
            .build();

    // Ermitteln des CodeSystems, auf dem die Suche angewendet werden soll
    CodeSystem cs =
        fhirTermsInMemoryRepresentation.getResourceByUrlAndVersion(
            CodeSystem.class, scope, system, version, ResourceRetrievalMode.FULL);
    log.debug("CodeSystem to use: {}|{}", cs.getUrl(), cs.getVersion());

    // Extrahieren des exact-Parameters über den wir die Art der Suche steuern
    boolean exact = param.getExactParam() != null && param.getExactParam().getValue();

    // Extrahieren der Property-Filter aus dem Param-Objekt
    List<PropertyFilter> propertyFilterList =
        preparePropertyFilter(
            param.getPropertyParamList() != null
                ? param.getPropertyParamList()
                : new ArrayList<>());

    // Überprüfen, ob die Properties in der übergebenen Form vom CodeSystem unterstützt werden
    validateMatchPropertiesAgainstCodeSystem(cs, propertyFilterList);

    // TODO: caseSensitivity Test schreiben
    // Vorbereiten des Vorselektierungsfilters: Suche nach Konzepten, in einem bestimmten CS in
    // einem bestimmten Scope
    Query preFilterQuery = ConceptQueryGenerator.prepareFindMatchesPreFilterQuery(scope, cs);

    // Erzeugen individueller Lucene-Queries für jede Property-Wert-Kombination
    List<Pair<String, Query>> queryList =
        ConceptQueryGenerator.prepareFindMatchesPropertyQueries(cs, propertyFilterList, exact);

    // Erzeugen einer Gesamt-Query aus den Queries
    Query combinedQuery =
        QueryCombinators.prepareOrQuery(queryList.stream().map(Pair::getValue).toList());

    // Query ausführen
    List<ScoredCoding> resultList =
        CodeSystemConceptSearchHelper.processQueryAndReturnConceptSet(
            QueryCombinators.prepareCombinedFilterMustQuery(preFilterQuery, combinedQuery),
            fhirTermsInMemoryRepresentation.getLuceneSearcher());

    // Ergebnis-Objekt erzeugen
    return generateMatchesResultFromFilterAndResultList(
        cs, exact, propertyFilterList, resultList, lsp);
  }

  /**
   * Generates a {@link CsFindMatchesResult} from the search results and the given property filters.
   *
   * <p>Each scored coding is enriched with system/version metadata and a preferred display value,
   * then categorized into exact, partial, or unmatched based on the boost scores encoded in the
   * Lucene result score.
   *
   * @param cs the code system in which the search was performed
   * @param exact whether an exact match was requested
   * @param propertyFilterList the list of property filters extracted from the request
   * @param resultList the list of scored codings returned by the Lucene search
   * @param languageSelectionParameter the language selection criteria for resolving display values
   * @return a {@link CsFindMatchesResult} containing the categorized matches
   */
  private CsFindMatchesResult generateMatchesResultFromFilterAndResultList(
      CodeSystem cs,
      boolean exact,
      List<PropertyFilter> propertyFilterList,
      List<ScoredCoding> resultList,
      LanguageSelectionParameter languageSelectionParameter) {

    CsFindMatchesResult result = new CsFindMatchesResult();

    // Iterieren über Ergebnissliste
    for (ScoredCoding scoredCoding : resultList) {
      enrichCodingWithSystemInfo(scoredCoding.getCoding(), cs);
      enrichCodingWithPreferredDisplay(scoredCoding.getCoding(), cs, languageSelectionParameter);
      Match match =
          createMatchFromPropertyFilters(
              scoredCoding.getCoding(), scoredCoding.getScore(), propertyFilterList, exact);
      result.getMatchList().add(match);
    }

    // Sortieren der Liste nach Relevanz der Treffer - Hierfür benötigen wir einen eigenen Ansatz,
    // der berücksichtigt, welche Art von Match wir haben

    return result;
  }

  /**
   * Enriches the given coding with the system URL and version from the code system.
   *
   * @param coding the coding to enrich
   * @param cs the code system providing system URL and version metadata
   */
  private void enrichCodingWithSystemInfo(Coding coding, CodeSystem cs) {
    // Ergänzen des codings um die System- und Version-Informationen
    coding.setSystem(cs.getUrl());
    coding.setVersion(cs.getVersion());
  }

  /**
   * Enriches the given coding with the preferred display value resolved from the code system
   * concept designations according to the language selection criteria.
   *
   * @param coding the coding to enrich
   * @param cs the code system containing the concept definitions
   * @param languageSelectionParameter the language selection criteria for resolving display values
   */
  private void enrichCodingWithPreferredDisplay(
      Coding coding, CodeSystem cs, LanguageSelectionParameter languageSelectionParameter) {
    // Ergänzen des codings um Display in gewünschter Sprache
    try {
      CodeSystem.ConceptDefinitionComponent concept =
          CodeSystemHelper.findCodeInCodeSystem(cs, coding.getCode());
      FhirDesignationManager fhirDesignationManager = new FhirDesignationManager();
      fhirDesignationManager.collectDesignationsFromFhirConcept(concept, cs.getLanguage());
      coding.setDisplay(fhirDesignationManager.getDisplay(languageSelectionParameter).getValue());
    } catch (UnknownConceptException _) {
      log.error(
          "THIS SHOULD NOT HAPPEN: Could not find concept for code {} in CodeSystem {}. Display cannot be enriched.",
          coding.getCode(),
          cs.getUrl());
    }
  }

  /**
   * Creates a {@link Match} for the given coding by categorizing each property filter into exact,
   * partial, or unmatched based on the Lucene result score.
   *
   * @param coding the coding that was matched
   * @param resultScore the Lucene score encoding which property filters matched
   * @param propertyFilterList the list of all property filters from the request
   * @param exact whether exact matching mode is enabled
   * @return a {@link Match} with the property filters assigned to the appropriate categories
   */
  private Match createMatchFromPropertyFilters(
      Coding coding, float resultScore, List<PropertyFilter> propertyFilterList, boolean exact) {

    // Erzeugen eines Match-Objekts
    Match match = new Match(coding);

    // Ermitteln, welche Properties gematcht haben und welche nicht. Hierbei müssen wir
    // unterscheiden, welcher Arbeitsmodus (exakt vs nicht-exakt) gewählt wurde
    for (PropertyFilter propertyFilter : propertyFilterList) {
      categorizePropertyMatch(match, propertyFilter, resultScore, exact);
    }

    return match;
  }

  /**
   * Categorizes a single property filter as exact match, partial match, or unmatched for the given
   * result score and adds it to the corresponding list in the {@link Match}.
   *
   * @param match the match object to populate
   * @param propertyFilter the property filter to categorize
   * @param resultScore the Lucene score encoding which property filters matched
   * @param exact whether exact matching mode is enabled (disables partial matches)
   */
  private void categorizePropertyMatch(
      Match match, PropertyFilter propertyFilter, float resultScore, boolean exact) {

    PropertyValuePair pair = new PropertyValuePair(propertyFilter.code(), propertyFilter.value());

    boolean isExactContained = isContained(propertyFilter.getExactMatchBoost(), resultScore);

    if (isExactContained) {
      match.getExactMatchList().add(pair);
    } else if (!exact && isContained(propertyFilter.getPartialMatchBoost(), resultScore)) {
      // Prüfung auf partielle Matches nur im nicht-exakten Modus
      match.getPartialMatchList().add(pair);
    } else {
      match.getUnmatchedList().add(pair);
    }
  }

  // ----------------------------------------------------------------------------------------

  /**
   * Extracts property code-value pairs from the given FHIR parameter list and wraps them as {@link
   * PropertyFilter} instances with positional boost scores.
   *
   * <p>The position of each property in the request determines the boost values assigned to its
   * exact and partial match scores. These scores are later used to determine which properties
   * matched via bitwise analysis of the Lucene result score.
   *
   * @param propertyParameterList the list of raw FHIR {@code property} parameters
   * @return an ordered list of {@link PropertyFilter} instances ready for query generation
   * @throws InvalidRequestException if a required parameter is missing or empty
   */
  private List<PropertyFilter> preparePropertyFilter(
      List<Parameters.ParametersParameterComponent> propertyParameterList)
      throws InvalidRequestException {

    // Laden der property-Properties aus den übergebenen Parametern
    List<Parameters.ParametersParameterComponent> propertyParamList =
        propertyParameterList.stream().filter(p -> p.getName().equals("property")).toList();
    if (propertyParamList.isEmpty()) {
      throw new InvalidRequestException("Parameter 'property' is missing");
    }

    // Ermitteln der Property-Namen und der angefragten Typen
    List<PropertyFilter> propertyFilterList = new ArrayList<>();

    int position = 0;

    for (Parameters.ParametersParameterComponent propertyParam : propertyParamList) {
      if (propertyParam.getPart() == null || propertyParam.getPart().isEmpty()) {
        throw new InvalidRequestException("Parameter 'property' is empty");
      }

      List<Parameters.ParametersParameterComponent> partList = propertyParam.getPart();

      Optional<String> tempPropertyCode =
          partList.stream()
              .filter(p -> p.getName().equals("code"))
              .filter(p -> p.getValue() instanceof CodeType)
              .map(p -> ((CodeType) p.getValue()).getValue())
              .findFirst();

      Optional<Type> tempPropertyValue =
          partList.stream()
              .filter(p -> p.getName().equals("value"))
              .map(ParametersParameterComponent::getValue)
              .findFirst();

      // Hinzufügen der gefundenen Property-Namen zur Liste
      if (tempPropertyCode.isPresent()) {
        propertyFilterList.add(
            new PropertyFilter(tempPropertyCode.get(), tempPropertyValue.orElse(null), position));
        position++;
      }
    }

    return propertyFilterList;
  }

  /**
   * Validates that all requested property filters are supported by the code system and that their
   * value types are compatible with the expected property types.
   *
   * @param cs the code system defining the supported properties
   * @param propertyFilterList the list of property filters to validate
   * @throws InvalidRequestException if an unsupported property is requested or a value type does
   *     not match the expected type for that property
   */
  private void validateMatchPropertiesAgainstCodeSystem(
      CodeSystem cs, List<PropertyFilter> propertyFilterList) throws InvalidRequestException {

    Map<String, CodeSystem.PropertyComponent> propertiesSupportedByCodeSystem =
        getAllSupportedCodeSystemProperties(cs);

    for (PropertyFilter propertyFilter : propertyFilterList) {
      String requestPropertyCode = propertyFilter.code();
      Type requestPropertyValue = propertyFilter.value();

      CodeSystem.PropertyComponent codeSystemProperty =
          propertiesSupportedByCodeSystem.get(requestPropertyCode);

      // Sofern die Property nicht unterstützt wird, wird eine Exception geworfen
      if (codeSystemProperty == null) {
        throw new InvalidRequestException(
            "Property '"
                + requestPropertyCode
                + "' is not supported by the referenced CodeSystem. Please use one of the following properties: "
                + propertiesSupportedByCodeSystem.keySet());
      }

      // Sofern die Property unterstützt wird, wird überprüft, ob der Typ der Property mit dem Typ
      // des übergebenen Wertes übereinstimmt
      if (!codeSystemProperty.getType().toCode().equals(requestPropertyValue.fhirType())) {
        log.debug("RQ Property: {}|{}", requestPropertyCode, requestPropertyValue.fhirType());
        log.debug(
            "CS Property: {}|{}",
            codeSystemProperty.getCode(),
            codeSystemProperty.getType().toCode());
        throw new InvalidRequestException(
            "Property '"
                + requestPropertyCode
                + "' with provided value type '"
                + requestPropertyValue.fhirType()
                + "' is not supported by the referenced CodeSystem. The proper type would be: '"
                + codeSystemProperty.getType().toCode()
                + "'");
      }
    }
  }

  /**
   * Collects all properties supported by the given code system, including the implicitly available
   * properties {@code code}, {@code display}, and {@code definition}.
   *
   * @param cs the code system whose explicitly and implicitly defined properties are collected
   * @return a map from property code to {@link CodeSystem.PropertyComponent}
   */
  private Map<String, CodeSystem.PropertyComponent> getAllSupportedCodeSystemProperties(
      CodeSystem cs) {

    Map<String, CodeSystem.PropertyComponent> propertyMap = new HashMap<>();
    cs.getProperty().forEach(property -> propertyMap.put(property.getCode(), property));

    propertyMap.put(
        "code",
        new CodeSystem.PropertyComponent().setCode("code").setType(CodeSystem.PropertyType.CODE));
    propertyMap.put(
        "display",
        new CodeSystem.PropertyComponent()
            .setCode("display")
            .setType(CodeSystem.PropertyType.STRING));
    propertyMap.put(
        "definition",
        new CodeSystem.PropertyComponent()
            .setCode("definition")
            .setType(CodeSystem.PropertyType.STRING));
    return propertyMap;
  }

  // ----------------------------------------------------------------------------------------
  // Helper Methods
  // ----------------------------------------------------------------------------------------

  /**
   * Converts a float value to a {@link BitSet} by interpreting its IEEE 754 bit representation.
   *
   * @param value the float value to convert
   * @return a {@link BitSet} representing the bit pattern of the given float
   */
  private static BitSet floatToBitSet(float value) {
    int bits = Float.floatToIntBits(value);
    return BitSet.valueOf(new long[] {bits & 0xFFFFFFFFL});
  }

  /**
   * Checks whether the bits of a property boost score are contained in the result score, indicating
   * that the property matched.
   *
   * <p>The check is performed by bitwise AND of both scores: if all bits of the property score are
   * set in the result score, the property is considered matched.
   *
   * @param propertyMatchBoost the boost score assigned to a specific property match type
   * @param resultScore the combined Lucene result score
   * @return {@code true} if the property score bits are fully contained in the result score, {@code
   *     false} otherwise
   */
  private static boolean isContained(float propertyMatchBoost, float resultScore) {
    // Umwandeln der float-Werte in BitSets
    BitSet propertyBitSet = floatToBitSet(propertyMatchBoost);
    BitSet resultBitSet = floatToBitSet(resultScore);

    // Kopieren des Property-BitSets
    BitSet compareBitSet = (BitSet) propertyBitSet.clone();

    // UND Verknüpfung der Kopie mit dem result-BitSet
    compareBitSet.and(resultBitSet);

    // Prüfen, ob die beiden BitSets gleich sind. Dies ist der eindeutige Indikator, dass ein Match
    // vorliegt
    return compareBitSet.equals(propertyBitSet);
  }
}
