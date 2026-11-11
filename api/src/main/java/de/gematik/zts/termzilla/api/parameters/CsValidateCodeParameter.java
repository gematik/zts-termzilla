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
import de.gematik.zts.termzilla.terminologies.common.LanguageParameter;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/**
 * Encapsulates the input parameters for a FHIR {@code CodeSystem/$validate-code} operation.
 *
 * <p>Corresponds to the parameters defined by the <a
 * href="https://hl7.org/fhir/R4/codesystem-operation-validate-code.html">FHIR R4
 * CodeSystem/$validate-code</a> operation. Validation can be performed by providing {@link
 * #codeParam} (with an optional {@link #urlParam} and {@link #versionParam}), a {@link
 * #codingParam}, or a {@link #codeableConceptParam}.
 *
 * <p>The class is serializable to support cache key generation via {@link
 * #createUniqueByteArrayFromParameterValues()}.
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class CsValidateCodeParameter implements Serializable {

  /** FHIR parameter name for the canonical URL of the CodeSystem. */
  public static final String URL_PARAM = "url";

  /** FHIR parameter name for an inline CodeSystem resource. */
  public static final String CODESYSTEM_PARAM = "codeSystem";

  /** FHIR parameter name for the code to validate. */
  public static final String CODE_PARAM = "code";

  /** FHIR parameter name for the code system version. */
  public static final String VERSION_PARAM = "version";

  /** FHIR parameter name for the display text to validate. */
  public static final String DISPLAY_PARAM = "display";

  /** FHIR parameter name for a {@link Coding} to validate. */
  public static final String CODING_PARAM = "coding";

  /** FHIR parameter name for a {@link CodeableConcept} to validate. */
  public static final String CODEABLECONCEPT_PARAM = "codeableConcept";

  /** FHIR parameter name for the date used to select the applicable code system version. */
  public static final String DATE_PARAM = "date";

  /** FHIR parameter name for the abstract flag. */
  public static final String ABSTRACT_PARAM = "abstract";

  /** FHIR parameter name for the preferred display language. */
  public static final String DISPLAY_LANGUAGE_PARAM = "displayLanguage";

  /** The canonical URL of the CodeSystem. */
  private UriType urlParam;

  /** An inline CodeSystem resource to validate against. */
  private CodeSystem codeSystemParam;

  /** The code to validate. */
  private CodeType codeParam;

  /** The version of the CodeSystem. */
  private StringType versionParam;

  /** The display text to validate. */
  private StringType displayParam;

  /** A {@link Coding} to validate. */
  private Coding codingParam;

  /** A {@link CodeableConcept} to validate. */
  private CodeableConcept codeableConceptParam;

  /** The date used to determine the applicable version of the code system. */
  private DateTimeType dateParam;

  /** Whether abstract codes are acceptable. */
  private BooleanType abstractParam;

  /** The preferred language for display validation. */
  private CodeType displayLanguageParam;

  /** Language preferences derived from the HTTP {@code Accept-Language} request header. */
  private LanguageParameter languageAcceptLanguageHeader;

  @Override
  public String toString() {
    return "CsValidateCodeParameter{"
        + "urlParam="
        + urlParam
        + ", codeSystemParam="
        + codeSystemParam
        + ", codeParam="
        + codeParam
        + ", versionParam="
        + versionParam
        + ", displayParam="
        + displayParam
        + ", codingParam="
        + codingParam
        + ", codeableConceptParam="
        + codeableConceptParam
        + ", dateParam="
        + dateParam
        + ", abstractParam="
        + abstractParam
        + ", displayLanguageParam="
        + displayLanguageParam
        + '}';
  }

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
