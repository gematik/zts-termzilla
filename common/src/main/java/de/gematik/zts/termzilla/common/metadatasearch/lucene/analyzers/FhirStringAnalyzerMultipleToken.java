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

import java.io.Reader;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilter;

/**
 * Diese Klasse implementiert einen Analyzer, den wir im Kontext von FHIR String Suchparametern bei
 * der ':contains'-Suche verwenden. (vgl. https://www.hl7.org/fhir/R4/search.html#string) Hinweis:
 * Wir implementieren :contains ein wenig anders als die FHIR-Spezifikation es vorsieht und zerlegen
 * die strings in mehrere Tokens, indem wir Leerzeichen als Token-Trenner verwenden. Vermutlich
 * könnten wir aber auf den Feldern auch eine n-gram-basierte Suche verwenden. Das sollten wir uns
 * perspektivisch anschauen.
 */
@Slf4j
public class FhirStringAnalyzerMultipleToken extends Analyzer {

  @Override
  protected Reader initReader(String fieldName, Reader reader) {

    // This char map will replace special characters with spaces to enable tokenization by
    // whitespace. This approach is not perfect, but should work reasonably well for most use cases.
    NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
    builder.add("[", " ");
    builder.add("]", " ");
    builder.add("-", " ");
    builder.add("*", " ");
    builder.add("+", " ");
    builder.add("\\", " ");
    builder.add("|", " ");
    builder.add("/", " ");
    builder.add("(", " ");
    builder.add(")", " ");
    builder.add("{", " ");
    builder.add("}", " ");

    NormalizeCharMap ncm = builder.build();
    Reader currentReader = new MappingCharFilter(ncm, reader);

    currentReader = new PatternReplaceCharFilter(Pattern.compile("\\s+"), " ", currentReader);

    return currentReader;
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {

    Tokenizer source = new WhitespaceTokenizer();
    TokenStream filter = new LowerCaseFilter(source);
    filter = new ASCIIFoldingFilter(filter);
    return new TokenStreamComponents(source, filter);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {

    TokenStream filter = new LowerCaseFilter(in);
    filter = new ASCIIFoldingFilter(filter);
    return filter;
  }
}
