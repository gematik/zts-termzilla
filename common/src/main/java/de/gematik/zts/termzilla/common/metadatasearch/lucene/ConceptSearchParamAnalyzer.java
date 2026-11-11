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
package de.gematik.zts.termzilla.common.metadatasearch.lucene;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DEFINITION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DESIGNATION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.NAME_DISPLAY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_CONTAINS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ConceptComponentFieldNames.SUFFIX_STRING_STANDARD;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.FhirStringAnalyzerMultipleToken;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers.FhirStringAnalyzerSingleToken;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;

// TODO can this be removed? What are these analyzers used for?
public class ConceptSearchParamAnalyzer {

  private FhirStringAnalyzerSingleToken customAnalyzerSingleToken;
  private FhirStringAnalyzerMultipleToken customAnalyzerMultipleToken;
  private Map<String, Analyzer> analyzerMap;

  public ConceptSearchParamAnalyzer() {
    this.customAnalyzerSingleToken = new FhirStringAnalyzerSingleToken();
    this.customAnalyzerMultipleToken = new FhirStringAnalyzerMultipleToken();
    this.analyzerMap = new HashMap<>();
    initializeAnalyzerMap(this.analyzerMap);
  }

  private void initializeAnalyzerMap(Map<String, Analyzer> analyzerMap) {
    initializeAnalyzerMapForConcepts(analyzerMap);
  }

  private void initializeAnalyzerMapForConcepts(Map<String, Analyzer> analyzerMap) {
    addMapEntriesForStringParam(analyzerMap, NAME_DISPLAY);
    addMapEntriesForStringParam(analyzerMap, NAME_DESIGNATION);
    addMapEntriesForStringParam(analyzerMap, NAME_DEFINITION);
  }

  private void addMapEntriesForStringParam(Map<String, Analyzer> analyzerMap, String paramName) {
    analyzerMap.put(paramName + SEPARATOR + SUFFIX_STRING_STANDARD, customAnalyzerSingleToken);
    analyzerMap.put(paramName + SEPARATOR + SUFFIX_STRING_CONTAINS, customAnalyzerMultipleToken);
  }

  public Map<String, Analyzer> getFieldAnalyzerMap() {
    return analyzerMap;
  }
}
