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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * Encapsulates the input parameters for a FHIR {@code CodeSystem/$find-matches} operation.
 *
 * <p>Corresponds to the parameters defined by the <a
 * href="https://hl7.org/fhir/R4/codesystem-operation-find-matches.html">FHIR R4
 * CodeSystem/$find-matches</a> operation. The operation finds codes matching a set of properties.
 *
 * <p>The class is serializable to support cache key generation via {@link
 * #createUniqueByteArrayFromParameterValues()}.
 */
@Getter
@Setter
@SuperBuilder
public class CsFindMatchesParameter extends CommonOperationParameter implements Serializable {

  /** FHIR parameter name for the code system URL. */
  public static final String SYSTEM_PARAM = "system";

  /** FHIR parameter name for the code system version. */
  public static final String VERSION_PARAM = "version";

  /** FHIR parameter name for the list of properties to match against. */
  public static final String PROPERTY_PARAM = "property";

  /** FHIR parameter name for the exact matching flag. */
  public static final String EXACT_PARAM = "exact";

  /** FHIR parameter name for the compositional grammar flag. */
  public static final String COMPOSITIONAL_PARAM = "compositional";

  /** The canonical URL of the code system to search in. */
  private UriType systemParam;

  /** The version of the code system. */
  private StringType versionParam;

  /** The list of property filters to match concepts against. */
  private List<Parameters.ParametersParameterComponent> propertyParamList;

  /** Whether to require exact matches ({@code true}) or allow partial matches ({@code false}). */
  private BooleanType exactParam;

  /** Whether to include compositional grammar matches. */
  private BooleanType compositionalParam;

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
