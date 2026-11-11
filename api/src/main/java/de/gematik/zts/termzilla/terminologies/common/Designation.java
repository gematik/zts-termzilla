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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;

/**
 * Abstract base class for terminology designations used during ValueSet expansion.
 *
 * <p>A designation represents a human-readable term for a coded concept, along with metadata about
 * its origin, language, and intended use. Concrete subclasses add terminology-specific fields (e.g.
 * SNOMED CT acceptability and description type).
 *
 * <p>Designations are collected from multiple sources during expansion and then evaluated by a
 * designation manager to select the most appropriate display value based on language preferences.
 *
 * @see DesignationSource
 */
@Getter
@Setter
@SuperBuilder
public abstract class Designation {

  /** The source from which this designation was obtained. */
  DesignationSource source;

  /** The BCP 47 language tag of this designation (e.g. {@code "de-DE"}, {@code "en-US"}). */
  CodeType language;

  /**
   * The intended use of this designation, expressed as a {@link Coding}. Typically references
   * either the HL7 {@code designation-usage} CodeSystem (e.g. {@code display}) or a
   * terminology-native coding (e.g. SNOMED CT description type).
   */
  Coding use;

  /** The human-readable designation text. */
  String value;

  /** Default constructor required by Lombok {@code @SuperBuilder} for subclass instantiation. */
  protected Designation() {}

  /**
   * Identifies the source from which a designation was obtained during ValueSet expansion.
   *
   * <ul>
   *   <li>{@link #CS} — derived from the underlying CodeSystem definition
   *   <li>{@link #CSS} — derived from a CodeSystem Supplement
   *   <li>{@link #VS} — explicitly defined in the ValueSet compose (e.g. display or designation on
   *       a concept reference)
   * </ul>
   */
  public enum DesignationSource {
    /** CodeSystem-sourced designation. */
    CS,
    /** CodeSystem Supplement-sourced designation. */
    CSS,
    /** ValueSet-sourced designation. */
    VS
  }
}
