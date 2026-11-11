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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.stream.Stream;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.core.JsonGenerator;

/**
 * Unit tests for {@link CustomLogLevelJsonProvider}. Tests the mapping of Logback log levels to
 * RFC5424/GCP-compatible severity levels.
 */
@DisplayName("CustomLogLevelJsonProvider Tests")
class CustomLogLevelJsonProviderTest {

  @Mock private JsonGenerator jsonGenerator;

  @Mock private ILoggingEvent loggingEvent;

  private CustomLogLevelJsonProvider customLogLevelJsonProvider;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    customLogLevelJsonProvider = new CustomLogLevelJsonProvider();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should initialize with 'severity' as default field name")
    void shouldInitializeWithDefaultFieldName() {
      // GIVEN
      CustomLogLevelJsonProvider provider = new CustomLogLevelJsonProvider();

      // WHEN
      String fieldName = provider.getFieldName();

      // THEN
      assertThat(fieldName)
          .as("Field name should be initialized to 'severity'")
          .isEqualTo(CustomLogLevelJsonProvider.SEVERITY_LEVEL);
    }
  }

  @Nested
  @DisplayName("writeTo() Tests")
  class WriteToTests {

    @ParameterizedTest
    @MethodSource("logLevelMappings")
    @DisplayName("Should map Logback levels to RFC5424/GCP severity levels correctly")
    void shouldMapLogLevelCorrectly(Level logbackLevel, String expectedSeverity) {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(logbackLevel);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator)
          .writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, expectedSeverity);
    }

    @Test
    @DisplayName("Should write 'DEBUG' severity for TRACE level")
    void shouldWriteDebugForTraceLevel() {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(Level.TRACE);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator).writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, "DEBUG");
    }

    @Test
    @DisplayName("Should write 'DEBUG' severity for DEBUG level")
    void shouldWriteDebugForDebugLevel() {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(Level.DEBUG);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator).writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, "DEBUG");
    }

    @Test
    @DisplayName("Should write 'INFO' severity for INFO level")
    void shouldWriteInfoForInfoLevel() {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(Level.INFO);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator).writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, "INFO");
    }

    @Test
    @DisplayName("Should write 'WARNING' severity for WARN level")
    void shouldWriteWarningForWarnLevel() {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(Level.WARN);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator)
          .writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, "WARNING");
    }

    @Test
    @DisplayName("Should write 'ERROR' severity for ERROR level")
    void shouldWriteErrorForErrorLevel() {
      // GIVEN
      when(loggingEvent.getLevel()).thenReturn(Level.ERROR);

      // WHEN
      customLogLevelJsonProvider.writeTo(jsonGenerator, loggingEvent);

      // THEN
      verify(jsonGenerator).writeStringProperty(CustomLogLevelJsonProvider.SEVERITY_LEVEL, "ERROR");
    }

    /**
     * Provides test data for parameterized log level mapping tests.
     *
     * @return Stream of Arguments containing Logback Level and expected severity string
     */
    static Stream<Arguments> logLevelMappings() {
      return Stream.of(
          Arguments.of(Level.TRACE, "DEBUG"),
          Arguments.of(Level.DEBUG, "DEBUG"),
          Arguments.of(Level.INFO, "INFO"),
          Arguments.of(Level.WARN, "WARNING"),
          Arguments.of(Level.ERROR, "ERROR"));
    }
  }

  @Nested
  @DisplayName("setFieldNames() Tests")
  class SetFieldNamesTests {

    @Test
    @DisplayName("Should always set field name to 'severity' regardless of input")
    void shouldAlwaysSetFieldNameToSeverity() {
      // GIVEN
      LogstashFieldNames fieldNames = new LogstashFieldNames();

      // WHEN
      customLogLevelJsonProvider.setFieldNames(fieldNames);

      // THEN
      assertThat(customLogLevelJsonProvider.getFieldName())
          .as("Field name should always be 'severity' after setFieldNames() call")
          .isEqualTo(CustomLogLevelJsonProvider.SEVERITY_LEVEL);
    }
  }
}
