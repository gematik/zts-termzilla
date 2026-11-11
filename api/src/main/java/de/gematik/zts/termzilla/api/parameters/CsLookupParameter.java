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

import de.gematik.zts.termzilla.api.exceptions.CacheKeyCreationException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * Encapsulates the input parameters for a FHIR {@code CodeSystem/$lookup} operation.
 *
 * <p>Corresponds to the parameters defined by the <a
 * href="https://hl7.org/fhir/R4/codesystem-operation-lookup.html">FHIR R4 CodeSystem/$lookup</a>
 * operation. A lookup can be performed either by providing {@link #codeParam} (with an optional
 * {@link #systemParam} and {@link #versionParam}) or by providing a {@link #codingParam}.
 *
 * <p>The class is serializable to support cache key generation via {@link
 * #createUniqueByteArrayFromParameterValues()}.
 */
@Getter
@Setter
@SuperBuilder
public class CsLookupParameter extends CommonOperationParameter implements Serializable {

  /** FHIR parameter name for the code to look up. */
  public static final String CODE_PARAM = "code";

  /** FHIR parameter name for the code system URL. */
  public static final String SYSTEM_PARAM = "system";

  /** FHIR parameter name for the code system version. */
  public static final String VERSION_PARAM = "version";

  /** FHIR parameter name for a {@link Coding} that combines code, system, and version. */
  public static final String CODING_PARAM = "coding";

  /** FHIR parameter name for the date used to select the applicable code system version. */
  public static final String DATE_PARAM = "date";

  /** FHIR parameter name for the preferred language of display values in the response. */
  public static final String DISPLAY_LANGUAGE_PARAM = "displayLanguage";

  /** FHIR parameter name for the list of properties to include in the response. */
  public static final String PROPERTY_PARAM = "property";

  /** The code to look up within the code system. */
  private CodeType codeParam;

  /** The canonical URL of the code system. */
  private UriType systemParam;

  /** The version of the code system. */
  private StringType versionParam;

  /** A {@link Coding} that specifies code, system, and optionally version in a single value. */
  private Coding codingParam;

  /** The date used to determine the applicable version of the code system. */
  private DateTimeType dateParam;

  /** The preferred language for display values returned in the response. */
  private CodeType displayLanguageParam;

  /** The list of additional properties to include in the lookup response. */
  private List<CodeType> propertyParam;

  /**
   * Serializes this parameter object into a unique byte array.
   *
   * <p>The resulting byte array can be used as a stable cache key that reflects all current
   * parameter values.
   *
   * @return a byte array representing the serialized state of this object
   * @throws CacheKeyCreationException if serialization fails
   */
  public byte[] createUniqueByteArrayFromParameterValues() {

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {

      oos.writeObject(this);
      return baos.toByteArray();

    } catch (Exception e) {
      throw new CacheKeyCreationException(
          "Error creating unique byte array from parameter values", e);
    }
  }
}
