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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.analyzers;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

@Slf4j
// TODO can this be removed? What are these analyzers used for?
public class AnalyzerDemo {

  private static final String[] examples = {
    "Charité",
    "Charité Krankenhaus",
    "Charité Ärztehaus",
    "Charité",
    "   Charité   ",
    "Charite",
    "charite",
    "Universitätsklinikum Charité",
    "Universitätsklinikum  Charité",
    "Universitätsklinikum\tCharité",
    "Außenstelle am Immanuel-Krankenhaus Berlin (Charité Universitätsmedizin Berlin)",
    "Klinikum *Kaulsdorf*",
    "Kaulsdorf Klinik"
  };
  private static final Analyzer[] analyzers =
      new Analyzer[] {
        new KeywordAnalyzer(),
        new FhirStringAnalyzerSingleToken(),
        new FhirStringAnalyzerMultipleToken(),
        new SimpleAnalyzer(),
        new StandardAnalyzer()
      };

  public static void main(String[] args) {
    String[] strings = examples;
    if (args.length > 0) {
      strings = args;
    }
    for (String text : strings) {
      analyze(text);
    }
  }

  private static void analyze(String text) {
    log.info("Analyzing \"{}\"", text);
    for (Analyzer analyzer : analyzers) {
      String name = analyzer.getClass().getSimpleName();
      List<String> tokenDisplay = AnalyzerUtils.displayTokens(analyzer, text);
      log.info(" {}: {}", name, tokenDisplay);
    }
  }
}
