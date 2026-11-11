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
package de.gematik.zts.termzilla.terminologies.snomed;

/**
 * Defines shared SNOMED CT constants used across terminology processing.
 *
 * <p>This class contains canonical system identifiers, SNOMED concept IDs for important description
 * and acceptability semantics, and shared error messages.
 */
public class SnomedConstants {

  /** Prevents instantiation of this constants holder. */
  private SnomedConstants() {
    // private constructor to prevent instantiation of this utility class
  }

  /** Canonical URI of the SNOMED CT code system ({@code system}). */
  public static final String TERMINOLOGY_CANONICAL_NAME = "http://snomed.info/sct";

  /** Human-readable name of the SNOMED CT terminology. */
  public static final String TERMINOLOGY_DISPLAY_NAME = "SNOMED_CT";

  /** Concept ID for the description type "Fully specified name". */
  public static final String SC_FULLYSPECIFIEDNAME = "900000000000003001";

  /** Concept ID for the description type "Synonym". */
  public static final String SC_SYNONYM = "900000000000013009";

  /** Concept ID for the definition status "Sufficiently defined". */
  public static final String SC_SUFFICENTLYDEFINED = "900000000000073002";

  /** Concept ID for case significance "Entire term case sensitive". */
  public static final String SC_CASESENSITIVE = "900000000000017005";

  /** Concept ID for case significance "Case insensitive". */
  public static final String SC_CASESINSENSITIVE = "900000000000448009";

  /** Concept ID for case significance "Initial character case insensitive". */
  public static final String SC_INITIALCHARACTERCASEINSENSITIVE = "900000000000020002";

  /** Concept ID for language reference set acceptability "Preferred". */
  public static final String SC_PREFERRED = "900000000000548007";

  /** Concept ID for language reference set acceptability "Acceptable". */
  public static final String SC_ACCEPTABLE = "900000000000549004";

  /** Concept ID for the compositional grammar reference set. */
  public static final String SC_COMPOSITIONAL_GRAMMAR = "1119454002";

  /** Concept ID for the SNOMED CT "is a" relationship type. */
  public static final String SC_IS_A_RELATIONSHIP_TYPE_ID = "116680003";

  /** ValueSet filter property for SNOMED CT Expression Constraint Language (ECL) expressions. */
  public static final String FILTER_PROPERTY_CONSTRAINT = "constraint";

  /**
   * Error message template used when expression processing is disabled and the provided value is
   * not a plain concept code.
   */
  public static final String EXPRESSION_PROCESSING_DISABLED_MESSAGE =
      "The provided code '%s' is not a valid SNOMED CT concept code. It might be an expression, but expression processing is disabled by configuration (snomed.expressionProcessingEnabled=false).";

  /**
   * Error message template used in restricted mode when the provided value is not a plain concept
   * code. Expression processing is always disabled in restricted mode.
   */
  public static final String EXPRESSION_PROCESSING_RESTRICTED_MESSAGE =
      "The provided code '%s' is not a valid SNOMED CT concept code. SNOMED CT expressions are not supported, because SNOMED CT is provided in restricted mode.";

  /**
   * Creates the message used when a code is not a concept code and expression processing is
   * disabled.
   *
   * @param code the provided code
   * @param restrictedMode whether SNOMED CT is provided in restricted mode
   * @return the message referring to restricted mode or to the expression processing configuration
   */
  public static String formatExpressionProcessingDisabledMessage(
      String code, boolean restrictedMode) {
    String template =
        restrictedMode
            ? EXPRESSION_PROCESSING_RESTRICTED_MESSAGE
            : EXPRESSION_PROCESSING_DISABLED_MESSAGE;
    return String.format(template, code);
  }
}
