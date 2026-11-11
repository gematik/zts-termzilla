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
package de.gematik.zts.termzilla.terminologies.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Represents the parsed contents of an HTTP {@code Accept-Language} header.
 *
 * <p>The header value is parsed into a list of {@link LanguageRange} entries, each consisting of a
 * BCP 47 language tag and a quality value. The entries are sorted by quality in descending order so
 * that the most preferred language comes first.
 */
@Getter
public class LanguageParameter implements Serializable {

  private static final double DEFAULT_QUALITY = 1.0;
  private static final String QUALITY_PREFIX = "q=";
  private static final LanguageParameter EMPTY = new LanguageParameter(Collections.emptyList());

  private final List<LanguageRange> ranges;

  private LanguageParameter(List<LanguageRange> ranges) {
    this.ranges = ranges;
  }

  /**
   * Parses an {@code Accept-Language} header value into an {@link LanguageParameter}.
   *
   * <p>Example input: {@code "de-DE, de;q=0.9, en-US;q=0.8, en;q=0.7"}
   *
   * @param headerValue the raw header value, may be {@code null} or blank
   * @return a parsed {@link LanguageParameter} with entries sorted by quality descending
   */
  public static LanguageParameter parse(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return EMPTY;
    }

    List<LanguageRange> ranges =
        Stream.of(headerValue.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .map(LanguageParameter::parseEntry)
            .sorted(Comparator.comparingDouble(LanguageRange::quality).reversed())
            .toList();

    return new LanguageParameter(ranges);
  }

  /**
   * Returns whether this parameter contains no language ranges.
   *
   * @return {@code true} if no language ranges are present
   */
  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  private static LanguageRange parseEntry(String entry) {
    String[] parts = entry.split(";");
    String tag = parts[0].trim();
    double quality = DEFAULT_QUALITY;

    for (int i = 1; i < parts.length; i++) {
      String param = parts[i].trim();
      if (param.startsWith(QUALITY_PREFIX)) {
        quality = Double.parseDouble(param.substring(QUALITY_PREFIX.length()));
      }
    }

    return new LanguageRange(tag, quality);
  }

  /**
   * Represents a single entry from the {@code Accept-Language} header, consisting of a BCP 47
   * language tag and its associated quality value.
   *
   * @param tag the BCP 47 language tag (e.g., "de-DE", "en", "*")
   * @param quality the quality value between 0.0 and 1.0, where 1.0 is the default
   */
  public record LanguageRange(String tag, double quality) implements Serializable {}
}
