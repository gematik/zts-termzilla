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

import de.gematik.zts.termzilla.common.metadatasearch.exceptions.TokenStreamException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

@Slf4j
public class AnalyzerUtils {

  private AnalyzerUtils() {
    // private constructor to prevent instantiation
  }

  private static final FhirStringAnalyzerSingleToken analyzer = new FhirStringAnalyzerSingleToken();

  public static String normalizePhrase(String phrase) {
    try {
      TokenStream stream = analyzer.tokenStream(null, new StringReader(phrase));
      stream.reset();
      stream.incrementToken();
      String returnString = stream.getAttribute(CharTermAttribute.class).toString();
      stream.close();
      return returnString;
    } catch (IOException e) {
      log.error("Error normalizing phrase: {}", phrase, e);
    }

    return null;
  }

  public static List<String> displayTokens(Analyzer analyzer, String string) {
    List<String> result = new ArrayList<>();
    try {
      TokenStream stream = analyzer.tokenStream(null, new StringReader(string));
      stream.reset();
      while (stream.incrementToken()) {
        result.add(stream.getAttribute(CharTermAttribute.class).toString());
      }
      stream.close();
    } catch (IOException e) {
      // not thrown b/c we're using a string reader...
      throw new TokenStreamException(e);
    }

    StringBuilder termsBuilder = new StringBuilder("|");
    for (String term : result) {
      termsBuilder.append(term).append('|');
    }
    log.debug(termsBuilder.toString());

    return result;
  }
}
