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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.exceptions.CacheKeyCreationException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ValueSet;

@Getter
@Setter
@Builder
public class VsValidateCodeParameter implements Serializable {

  // Value set Canonical URL. The server must know the value set (e.g. it is defined explicitly in
  // the server's value sets, or it is defined implicitly by some code system known to the server
  public static final String URL_PARAM = "url";

  // The context of the value set, so that the server can resolve this to a value set to validate
  // against.
  public static final String CONTEXT_PARAM = "context";

  // The value set is provided directly as part of the request. Servers may choose not to accept
  // value sets in this fashion. This parameter is used when the client wants the server to expand a
  // value set that is not stored on the server
  public static final String VALUESET_PARAM = "valueSet";

  // The identifier that is used to identify a specific version of the value set to be used when
  // validating the code. This is an arbitrary value managed by the value set author and is not
  // expected to be globally unique. For example, it might be a timestamp (e.g. yyyymmdd) if a
  // managed version is not available.
  public static final String VALUESET_VERSION_PARAM = "valueSetVersion";

  // The code that is to be validated. If a code is provided, a system or a context must be provided
  // (if a context is provided, then the server SHALL ensure that the code is not ambiguous without
  // a system)
  public static final String CODE_PARAM = "code";

  // The system for the code that is to be validated
  public static final String SYSTEM_PARAM = "system";

  // R4 The version of the system, if one was provided in the source data
  public static final String SYSTEM_VERSION_PARAM = "systemVersion";

  // R4 The display associated with the code, if provided. If a display is provided a code must be
  // provided. If no display is provided, the server cannot validate the display value, but may
  // choose to return a recommended display name using the display parameter in the outcome. Whether
  // displays are case sensitive is code system dependent
  public static final String DISPLAY_PARAM = "display";

  // R4 A coding to validate
  public static final String CODING_PARAM = "coding";

  // R4 A full codeableConcept to validate. The server returns true if one of the coding values is
  // in
  // the value set, and may also validate that the codings are not in conflict with each other if
  // more than one is present
  public static final String CODEABLECONCEPT_PARAM = "codeableConcept";

  // R4 The date for which the validation should be checked. Normally, this is the current
  // conditions
  // (which is the default values) but under some circumstances, systems need to validate that a
  // correct code was used at some point in the past. A typical example of this would be where code
  // selection is constrained to the set of codes that were available when the patient was treated,
  // not when the record is being edited. Note that which date is appropriate is a matter for
  // implementation policy.
  public static final String DATE_PARAM = "date";

  // R4 If this parameter has a value of true, the client is stating that the validation is being
  // performed in a context where a concept designated as 'abstract' is appropriate/allowed to be
  // used, and the server should regard abstract codes as valid. If this parameter is false,
  // abstract codes are not considered to be valid.
  public static final String ABSTRACT_PARAM = "abstract";

  // R4 Specifies the language to be used for description when validating the display property
  public static final String DISPLAY_LANGUAGE_PARAM = "displayLanguage";

  // R6 The supplement must be used when validating the code. Use of this parameter should result in
  // $validate-code behaving the same way as if the supplements were included in the value set
  // definition using the http://hl7.org/fhir/StructureDefinition/valueset-supplement
  public static final String USE_SUPPLEMENT_PARAM = "useSupplement";

  // R6 When the 'lenient-display-validation' parameter is true, an invalid display string will not
  // cause the 'result' output parameter to be 'false'. If the 'lenient-display-validation'
  // parameter is false or absent, then an invalid display will cause the 'result' output parameter
  // to be 'false', i.e. the validation will fail.
  public static final String LENIENT_DISPLAY_VALIDATION_PARAM = "lenient-display-validation";

  // R6 When 'true', the server will not perform the additional validation tasks beyond validating
  // membership in the value set (e.g. the server won't check displays, etc.)
  public static final String VALUESET_MEMBERSHIP_ONLY_PARAM = "valueset-membership-only";

  // R6 If true, the terminology server is required to infer the system from evaluation of the value
  // set definition. The inferSystem parameter is only to be used with the code parameter, and not
  // with the coding nor codeableConcept parameters.
  public static final String INFER_SYSTEM_PARAM = "inferSystem";

  // R6 .. hat noch viele weitere Parameter, die hier nicht aufgeführt sind
  // https://build.fhir.org/valueset-operation-validate-code.html

  private UriParam urlParam;
  private UriParam contextParam;
  private ValueSet valueSetParam;
  private StringParam valueSetVersionParam;
  private TokenParam codeParam;
  private UriParam systemParam;
  private StringParam systemVersionParam;
  private StringParam displayParam;
  private Coding codingParam;
  private CodeableConcept codeableConceptParam;
  private DateParam dateParam;
  private TokenParam abstractParam;
  private TokenParam displayLanguageParam;
  private BooleanType lenientDisplayValidationParam;
  private BooleanType inferSystemParam;

  public Optional<String> getUrl() {
    if (urlParam == null) {
      return Optional.empty();
    }
    if (urlParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(urlParam.getValue());
  }

  public Optional<String> getContext() {
    if (contextParam == null) {
      return Optional.empty();
    }
    if (contextParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(contextParam.getValue());
  }

  public Optional<ValueSet> getValueSet() {
    if (valueSetParam == null) {
      return Optional.empty();
    }
    if (valueSetParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(valueSetParam);
  }

  public Optional<String> getValueSetVersion() {
    if (valueSetVersionParam == null) {
      return Optional.empty();
    }
    if (valueSetVersionParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(valueSetVersionParam.getValue());
  }

  public Optional<String> getCode() {
    if (codeParam == null) {
      return Optional.empty();
    }
    if (codeParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(codeParam.getValue());
  }

  public Optional<String> getSystem() {
    if (systemParam == null) {
      return Optional.empty();
    }
    if (systemParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(systemParam.getValue());
  }

  public Optional<String> getSystemVersion() {
    if (systemVersionParam == null) {
      return Optional.empty();
    }
    if (systemVersionParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(systemVersionParam.getValue());
  }

  public Optional<String> getDisplay() {
    if (displayParam == null) {
      return Optional.empty();
    }
    if (displayParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(displayParam.getValue());
  }

  public Optional<Coding> getCoding() {
    if (codingParam == null) {
      return Optional.empty();
    }
    if (StringUtils.isEmpty(codingParam.getSystem())) {
      return Optional.empty();
    }
    return Optional.of(codingParam);
  }

  public Optional<CodeableConcept> getCodeableConcept() {
    if (codeableConceptParam == null) {
      return Optional.empty();
    }
    if (codeableConceptParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(codeableConceptParam);
  }

  public Optional<String> getDate() {
    if (dateParam == null) {
      return Optional.empty();
    }
    if (dateParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(dateParam.getValueAsString());
  }

  public Optional<Boolean> getAbstract() {
    if (abstractParam == null) {
      return Optional.empty();
    }
    if (abstractParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(abstractParam.getValue().equalsIgnoreCase("true"));
  }

  public Optional<String> getDisplayLanguage() {
    if (displayLanguageParam == null) {
      return Optional.empty();
    }
    if (displayLanguageParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(displayLanguageParam.getValue());
  }

  public Optional<Boolean> getLenientDisplayValidation() {
    if (lenientDisplayValidationParam == null) {
      return Optional.empty();
    }
    if (lenientDisplayValidationParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(lenientDisplayValidationParam.getValue());
  }

  public Optional<Boolean> getInferSystem() {
    if (inferSystemParam == null) {
      return Optional.empty();
    }
    if (inferSystemParam.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(inferSystemParam.getValue());
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
          "Error creating unique byte array from $validate-code parameter values", e);
    }
  }

  @Override
  public String toString() {
    return "VsValidateCodeParameter{"
        + "urlParam="
        + urlParam
        + ", contextParam="
        + contextParam
        + ", valueSetParam="
        + valueSetParam
        + ", valueSetVersionParam="
        + valueSetVersionParam
        + ", codeParam="
        + codeParam
        + ", systemParam="
        + systemParam
        + ", systemVersionParam="
        + systemVersionParam
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
        + ", lenientDisplayValidationParam="
        + lenientDisplayValidationParam
        + ", inferSystemParam="
        + inferSystemParam
        + '}';
  }

  public VsValidateCodeParameter shallowCopy() {

    return VsValidateCodeParameter.builder()
        .urlParam(this.urlParam)
        .contextParam(this.contextParam)
        .valueSetParam(this.valueSetParam)
        .valueSetVersionParam(this.valueSetVersionParam)
        .codeParam(this.codeParam)
        .systemParam(this.systemParam)
        .systemVersionParam(this.systemVersionParam)
        .displayParam(this.displayParam)
        .codingParam(this.codingParam)
        .codeableConceptParam(this.codeableConceptParam)
        .dateParam(this.dateParam)
        .abstractParam(this.abstractParam)
        .displayLanguageParam(this.displayLanguageParam)
        .lenientDisplayValidationParam(this.lenientDisplayValidationParam)
        .inferSystemParam(this.inferSystemParam)
        .build();
  }
}
