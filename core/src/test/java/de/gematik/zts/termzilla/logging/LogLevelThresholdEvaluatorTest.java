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
package de.gematik.zts.termzilla.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link LogLevelThresholdEvaluator}. Tests the evaluation of log events against a
 * configurable threshold level.
 */
@DisplayName("LogLevelThresholdEvaluator Tests")
class LogLevelThresholdEvaluatorTest {

  @Mock private ILoggingEvent loggingEvent;

  private LogLevelThresholdEvaluator evaluator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    evaluator = new LogLevelThresholdEvaluator();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize with ERROR as default threshold")
    void shouldInitializeWithDefaultThreshold() throws EvaluationException {
      // GIVEN
      LogLevelThresholdEvaluator newEvaluator = new LogLevelThresholdEvaluator();
      when(loggingEvent.getLevel()).thenReturn(Level.ERROR);

      // WHEN
      boolean result = newEvaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("ERROR should not be below default ERROR threshold").isFalse();
    }
  }

  @Nested
  @DisplayName("setThreshold() Tests")
  class SetThresholdTests {

    @Test
    @DisplayName("Should set threshold to WARN level")
    void shouldSetThresholdToWarn() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("WARN");
      when(loggingEvent.getLevel()).thenReturn(Level.INFO);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("INFO should be below WARN threshold").isTrue();
    }

    @Test
    @DisplayName("Should set threshold to INFO level")
    void shouldSetThresholdToInfo() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("INFO");
      when(loggingEvent.getLevel()).thenReturn(Level.DEBUG);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("DEBUG should be below INFO threshold").isTrue();
    }

    @Test
    @DisplayName("Should fall back to DEBUG level for invalid threshold")
    void shouldFallBackToDebugForInvalidThreshold() throws EvaluationException {
      // GIVEN
      String invalidLevel = "INVALID_LEVEL";
      evaluator.setThreshold(invalidLevel);
      when(loggingEvent.getLevel()).thenReturn(Level.TRACE);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result)
          .as("Invalid threshold should fall back to DEBUG, and TRACE should be below DEBUG")
          .isTrue();
    }

    @Test
    @DisplayName("Should fall back to DEBUG level for null threshold")
    void shouldFallBackToDebugForNullThreshold() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold(null);
      when(loggingEvent.getLevel()).thenReturn(Level.TRACE);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result)
          .as("Null threshold should fall back to DEBUG, and TRACE should be below DEBUG")
          .isTrue();
    }

    @Test
    @DisplayName("Should handle case-insensitive threshold values")
    void shouldHandleCaseInsensitiveThreshold() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("error");
      when(loggingEvent.getLevel()).thenReturn(Level.WARN);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("Lowercase 'error' should be recognized as ERROR level").isTrue();
    }
  }

  @Nested
  @DisplayName("evaluate() Tests")
  class EvaluateTests {

    @ParameterizedTest
    @MethodSource("levelBelowThresholdCases")
    @DisplayName("Should return true when log level is below threshold")
    void shouldReturnTrueWhenLevelBelowThreshold(Level threshold, Level eventLevel)
        throws EvaluationException {
      // GIVEN
      evaluator.setThreshold(threshold.toString());
      when(loggingEvent.getLevel()).thenReturn(eventLevel);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result)
          .as("Event level %s should be below threshold %s", eventLevel, threshold)
          .isTrue();
    }

    @ParameterizedTest
    @MethodSource("levelAtOrAboveThresholdCases")
    @DisplayName("Should return false when log level is at or above threshold")
    void shouldReturnFalseWhenLevelAtOrAboveThreshold(Level threshold, Level eventLevel)
        throws EvaluationException {
      // GIVEN
      evaluator.setThreshold(threshold.toString());
      when(loggingEvent.getLevel()).thenReturn(eventLevel);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result)
          .as("Event level %s should be at or above threshold %s", eventLevel, threshold)
          .isFalse();
    }

    @Test
    @DisplayName("Should return true when WARN is below ERROR threshold")
    void shouldReturnTrueWhenWarnBelowError() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("ERROR");
      when(loggingEvent.getLevel()).thenReturn(Level.WARN);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("WARN should be below ERROR threshold").isTrue();
    }

    @Test
    @DisplayName("Should return false when ERROR equals ERROR threshold")
    void shouldReturnFalseWhenErrorEqualsError() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("ERROR");
      when(loggingEvent.getLevel()).thenReturn(Level.ERROR);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("ERROR should not be below ERROR threshold (equal levels)").isFalse();
    }

    @Test
    @DisplayName("Should return false when ERROR is above WARN threshold")
    void shouldReturnFalseWhenErrorAboveWarn() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("WARN");
      when(loggingEvent.getLevel()).thenReturn(Level.ERROR);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("ERROR should be above WARN threshold").isFalse();
    }

    @Test
    @DisplayName("Should return true when INFO is below WARN threshold")
    void shouldReturnTrueWhenInfoBelowWarn() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("WARN");
      when(loggingEvent.getLevel()).thenReturn(Level.INFO);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("INFO should be below WARN threshold").isTrue();
    }

    @Test
    @DisplayName("Should return true when TRACE is below DEBUG threshold")
    void shouldReturnTrueWhenTraceBelowDebug() throws EvaluationException {
      // GIVEN
      evaluator.setThreshold("DEBUG");
      when(loggingEvent.getLevel()).thenReturn(Level.TRACE);

      // WHEN
      boolean result = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(result).as("TRACE should be below DEBUG threshold").isTrue();
    }

    @Test
    @DisplayName("Should handle threshold change correctly")
    void shouldHandleThresholdChange() throws EvaluationException {
      // GIVEN - First evaluation with ERROR threshold
      evaluator.setThreshold("ERROR");
      when(loggingEvent.getLevel()).thenReturn(Level.WARN);
      boolean firstResult = evaluator.evaluate(loggingEvent);

      // WHEN - Change threshold to WARN
      evaluator.setThreshold("WARN");
      boolean secondResult = evaluator.evaluate(loggingEvent);

      // THEN
      assertThat(firstResult).as("WARN should be below ERROR threshold").isTrue();
      assertThat(secondResult).as("WARN should not be below WARN threshold after change").isFalse();
    }

    /**
     * Provides test data for cases where event level is below threshold.
     *
     * @return Stream of Arguments containing threshold level and event level
     */
    static Stream<Arguments> levelBelowThresholdCases() {
      return Stream.of(
          Arguments.of(Level.ERROR, Level.WARN),
          Arguments.of(Level.ERROR, Level.INFO),
          Arguments.of(Level.ERROR, Level.DEBUG),
          Arguments.of(Level.ERROR, Level.TRACE),
          Arguments.of(Level.WARN, Level.INFO),
          Arguments.of(Level.WARN, Level.DEBUG),
          Arguments.of(Level.WARN, Level.TRACE),
          Arguments.of(Level.INFO, Level.DEBUG),
          Arguments.of(Level.INFO, Level.TRACE),
          Arguments.of(Level.DEBUG, Level.TRACE));
    }

    /**
     * Provides test data for cases where event level is at or above threshold.
     *
     * @return Stream of Arguments containing threshold level and event level
     */
    static Stream<Arguments> levelAtOrAboveThresholdCases() {
      return Stream.of(
          Arguments.of(Level.ERROR, Level.ERROR),
          Arguments.of(Level.WARN, Level.ERROR),
          Arguments.of(Level.WARN, Level.WARN),
          Arguments.of(Level.INFO, Level.ERROR),
          Arguments.of(Level.INFO, Level.WARN),
          Arguments.of(Level.INFO, Level.INFO),
          Arguments.of(Level.DEBUG, Level.ERROR),
          Arguments.of(Level.DEBUG, Level.WARN),
          Arguments.of(Level.DEBUG, Level.INFO),
          Arguments.of(Level.DEBUG, Level.DEBUG),
          Arguments.of(Level.TRACE, Level.DEBUG),
          Arguments.of(Level.TRACE, Level.INFO),
          Arguments.of(Level.TRACE, Level.WARN),
          Arguments.of(Level.TRACE, Level.ERROR),
          Arguments.of(Level.TRACE, Level.TRACE));
    }
  }
}
