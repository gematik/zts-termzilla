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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

/** Factory methods for creating FHIR property {@link Type} instances for testing. */
public class ConceptPropertyTestDataHelper {

  private ConceptPropertyTestDataHelper() {
    // Utility class
  }

  /**
   * Creates a FHIR string property value.
   *
   * @param value the string value
   * @return a {@link StringType} instance
   */
  public static Type createStringProperty(String value) {
    return new StringType(value);
  }

  /**
   * Creates a FHIR code property value.
   *
   * @param value the code value
   * @return a {@link CodeType} instance
   */
  public static Type createCodeProperty(String value) {
    return new CodeType(value);
  }

  /**
   * Creates a FHIR Coding property value.
   *
   * @param system the coding system URI
   * @param version the coding version
   * @param code the coding code
   * @return a {@link Coding} instance
   */
  public static Type createCodingProperty(String system, String version, String code) {
    Coding coding = new Coding();
    coding.setSystem(system);
    coding.setVersion(version);
    coding.setCode(code);
    return coding;
  }

  /**
   * Creates a FHIR boolean property value.
   *
   * @param value the boolean value
   * @return a {@link BooleanType} instance
   */
  public static Type createBooleanProperty(boolean value) {
    return new BooleanType(value);
  }

  /**
   * Creates a FHIR integer property value.
   *
   * @param value the integer value
   * @return an {@link IntegerType} instance
   */
  public static Type createIntegerProperty(int value) {
    return new IntegerType(value);
  }

  /**
   * Creates a FHIR decimal property value from a string representation to preserve precision.
   *
   * @param value the decimal value as string
   * @return a {@link DecimalType} instance
   */
  public static Type createDecimalProperty(String value) {
    return new DecimalType(value);
  }

  /**
   * Creates a FHIR dateTime property value.
   *
   * @param isoDate the date in ISO-8601 format
   * @return a {@link DateTimeType} instance
   */
  public static Type createDateTimeProperty(String isoDate) {
    return new DateTimeType(isoDate);
  }
}
