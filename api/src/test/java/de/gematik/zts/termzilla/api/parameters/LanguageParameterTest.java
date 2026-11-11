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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter.LanguageRange;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link LanguageParameter}.
 *
 * <p>Verifies parsing of HTTP {@code Accept-Language} header values into structured {@link
 * LanguageRange} entries, including quality value handling and priority sorting.
 */
@DisplayName("AcceptLanguageParameter Tests")
class LanguageParameterTest {

  private static final double QUALITY_PRECISION = 0.001;
  private static final double DEFAULT_QUALITY = 1.0;

  @Nested
  @DisplayName("parse() Tests")
  class ParseTests {

    @ParameterizedTest
    @DisplayName("Should return empty parameter for null, empty, and blank input")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldReturnEmptyParameterForBlankInput(String headerValue) {
      // GIVEN
      // A null, empty, or blank header value

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      assertThat(result.isEmpty())
          .as("Parameter should be empty for blank input '%s'", headerValue)
          .isTrue();
      assertThat(result.getRanges()).as("Ranges should be empty").isEmpty();
    }

    @Test
    @DisplayName("Should parse single language tag without quality")
    void shouldParseSingleLanguageTagWithoutQuality() {
      // GIVEN
      String headerValue = "de-DE";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      assertThat(result.isEmpty()).isFalse();
      assertThat(result.getRanges()).hasSize(1);

      LanguageRange range = result.getRanges().getFirst();
      assertThat(range.tag()).isEqualTo("de-DE");
      assertThat(range.quality()).isCloseTo(DEFAULT_QUALITY, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should parse single language tag with explicit quality")
    void shouldParseSingleLanguageTagWithQuality() {
      // GIVEN
      String headerValue = "en-US;q=0.8";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      assertThat(result.getRanges()).hasSize(1);

      LanguageRange range = result.getRanges().getFirst();
      assertThat(range.tag()).isEqualTo("en-US");
      assertThat(range.quality()).isCloseTo(0.8, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should parse multiple language tags and sort by quality descending")
    void shouldParseMultipleTagsAndSortByQuality() {
      // GIVEN
      String headerValue = "de-DE, de;q=0.9, en-US;q=0.8, en;q=0.7";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges).hasSize(4);

      assertThat(ranges.get(0).tag()).isEqualTo("de-DE");
      assertThat(ranges.get(0).quality()).isCloseTo(1.0, within(QUALITY_PRECISION));

      assertThat(ranges.get(1).tag()).isEqualTo("de");
      assertThat(ranges.get(1).quality()).isCloseTo(0.9, within(QUALITY_PRECISION));

      assertThat(ranges.get(2).tag()).isEqualTo("en-US");
      assertThat(ranges.get(2).quality()).isCloseTo(0.8, within(QUALITY_PRECISION));

      assertThat(ranges.get(3).tag()).isEqualTo("en");
      assertThat(ranges.get(3).quality()).isCloseTo(0.7, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should parse wildcard language tag")
    void shouldParseWildcardLanguageTag() {
      // GIVEN
      String headerValue = "de-DE, *;q=0.5";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges).hasSize(2);

      assertThat(ranges.get(0).tag()).isEqualTo("de-DE");
      assertThat(ranges.get(1).tag()).isEqualTo("*");
      assertThat(ranges.get(1).quality()).isCloseTo(0.5, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should assign default quality 1.0 when quality parameter is omitted")
    void shouldAssignDefaultQualityWhenOmitted() {
      // GIVEN
      String headerValue = "fr, en-GB";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges)
          .hasSize(2)
          .allSatisfy(
              range ->
                  assertThat(range.quality())
                      .as("Quality for '%s' should be default 1.0", range.tag())
                      .isCloseTo(DEFAULT_QUALITY, within(QUALITY_PRECISION)));
    }

    @Test
    @DisplayName("Should handle quality value of zero")
    void shouldHandleQualityValueOfZero() {
      // GIVEN
      String headerValue = "de-DE, en;q=0";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges).hasSize(2);

      assertThat(ranges.get(0).tag()).isEqualTo("de-DE");
      assertThat(ranges.get(1).tag()).isEqualTo("en");
      assertThat(ranges.get(1).quality()).isCloseTo(0.0, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should reorder entries so that highest quality comes first")
    void shouldReorderEntriesByQuality() {
      // GIVEN
      String headerValue = "en;q=0.5, de;q=0.9, fr;q=0.7";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges)
          .extracting(LanguageRange::tag)
          .as("Tags should be sorted by quality descending")
          .containsExactly("de", "fr", "en");
    }

    @Test
    @DisplayName("Should handle extra whitespace around entries")
    void shouldHandleExtraWhitespace() {
      // GIVEN
      String headerValue = "  de-DE  ,  en ; q=0.8  ";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      List<LanguageRange> ranges = result.getRanges();
      assertThat(ranges).hasSize(2);
      assertThat(ranges.get(0).tag()).isEqualTo("de-DE");
      assertThat(ranges.get(1).tag()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should parse base language without region")
    void shouldParseBaseLanguageWithoutRegion() {
      // GIVEN
      String headerValue = "de";

      // WHEN
      LanguageParameter result = LanguageParameter.parse(headerValue);

      // THEN
      assertThat(result.getRanges()).hasSize(1);
      assertThat(result.getRanges().get(0).tag()).isEqualTo("de");
    }
  }

  @Nested
  @DisplayName("isEmpty() Tests")
  class IsEmptyTests {

    @Test
    @DisplayName("Should return true for parameter parsed from null")
    void shouldReturnTrueForNull() {
      // GIVEN
      LanguageParameter result = LanguageParameter.parse(null);

      // WHEN
      boolean empty = result.isEmpty();

      // THEN
      assertThat(empty).isTrue();
    }

    @Test
    @DisplayName("Should return false for parameter with at least one language range")
    void shouldReturnFalseForNonEmptyParameter() {
      // GIVEN
      LanguageParameter result = LanguageParameter.parse("de");

      // WHEN
      boolean empty = result.isEmpty();

      // THEN
      assertThat(empty).isFalse();
    }
  }

  @Nested
  @DisplayName("LanguageRange Record Tests")
  class LanguageRangeTests {

    @Test
    @DisplayName("Should provide tag and quality via accessors")
    void shouldProvideTagAndQualityViaAccessors() {
      // GIVEN
      LanguageRange range = new LanguageRange("de-AT", 0.9);

      // WHEN & THEN
      assertThat(range.tag()).isEqualTo("de-AT");
      assertThat(range.quality()).isCloseTo(0.9, within(QUALITY_PRECISION));
    }

    @Test
    @DisplayName("Should implement equals based on tag and quality")
    void shouldImplementEquals() {
      // GIVEN
      LanguageRange range1 = new LanguageRange("en-GB", 0.8);
      LanguageRange range2 = new LanguageRange("en-GB", 0.8);
      LanguageRange range3 = new LanguageRange("en-US", 0.8);

      // WHEN & THEN
      assertThat(range1).isEqualTo(range2).isNotEqualTo(range3);
    }

    @Test
    @DisplayName("Should implement toString with tag and quality")
    void shouldImplementToString() {
      // GIVEN
      LanguageRange range = new LanguageRange("de-DE", 1.0);

      // WHEN
      String toString = range.toString();

      // THEN
      assertThat(toString).contains("de-DE").contains("1.0");
    }
  }
}
