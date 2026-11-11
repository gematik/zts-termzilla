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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ProcessingContext}. */
@DisplayName("ProcessingContext")
class ProcessingContextTest {

  @Test
  @DisplayName("should create a context for provided resources")
  void shouldCreateProvidedContext() {
    // WHEN
    ProcessingContext context = ProcessingContext.provided();

    // THEN
    assertThat(context.origin()).isEqualTo(ResourceOrigin.PROVIDED);
    assertThat(context.isStored()).isFalse();
  }

  @Test
  @DisplayName("should create a context for stored resources")
  void shouldCreateStoredContext() {
    // WHEN
    ProcessingContext context = ProcessingContext.stored();

    // THEN
    assertThat(context.origin()).isEqualTo(ResourceOrigin.STORED);
    assertThat(context.isStored()).isTrue();
  }

  @Test
  @DisplayName("should reject a missing origin")
  void shouldRejectMissingOrigin() {
    // WHEN / THEN
    assertThatThrownBy(() -> new ProcessingContext(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("origin");
  }
}
