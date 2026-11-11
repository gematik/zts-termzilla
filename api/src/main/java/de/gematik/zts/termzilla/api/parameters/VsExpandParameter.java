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
import de.gematik.zts.termzilla.terminologies.common.SupplementRequest;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;

@Getter
@Setter
@Builder
public class VsExpandParameter implements Serializable {

  public static final String URL_PARAM = "url";
  public static final String VALUESET_PARAM = "valueSet";
  public static final String VALUESET_VERSION_PARAM = "valueSetVersion";
  public static final String CONTEXT_PARAM = "context";
  public static final String CONTEXT_DIRECTION_PARAM = "contextDirection";
  public static final String FILTER_PARAM = "filter";
  public static final String DATE_PARAM = "date";
  public static final String OFFSET_PARAM = "offset";
  public static final String COUNT_PARAM = "count";
  public static final String INCLUDE_DESIGNATIONS_PARAM = "includeDesignations";
  public static final String DESIGNATION_PARAM = "designation";
  public static final String INCLUDE_DEFINITION_PARAM = "includeDefinition";
  public static final String ACTIVE_ONLY_PARAM = "activeOnly";
  public static final String EXCLUDE_NESTED_PARAM = "excludeNested";
  public static final String EXCLUDE_NOT_FOR_UI_PARAM = "excludeNotForUI";
  public static final String EXCLUDE_POSTCOORDINATED_PARAM = "excludePostCoordinated";
  public static final String DISPLAY_LANGUAGE_PARAM = "displayLanguage";
  public static final String EXCLUDE_SYSTEM_PARAM = "exclude-system";
  public static final String SYSTEM_VERSION_PARAM = "system-version";
  public static final String CHECK_SYSTEM_VERSION_PARAM = "check-system-version";
  public static final String FORCE_SYSTEM_VERSION_PARAM = "force-system-version";

  /**
   * The {@code useSupplement} parameter (repeatable canonical).
   *
   * <p>Natively an R5 parameter; on this R4 server it is accepted as a documented ZTS compatibility
   * extension. Each value is a canonical {@code [supplement]} or {@code [supplement]|[version]}
   * referencing a CodeSystem supplement to apply to the expansion.
   */
  public static final String USE_SUPPLEMENT_PARAM = "useSupplement";

  private UriType urlParam;
  private ValueSet valueSetParam;
  private StringType valueSetVersionParam;
  private UriType contextParam;
  private CodeType contextDirectionParam;
  private StringType filterParam;
  private DateTimeType dateParam;
  private IntegerType offsetParam;
  private IntegerType countParam;
  private BooleanType includeDesignationsParam;
  private List<StringType> designationParam;
  private BooleanType includeDefinitionParam;
  private BooleanType activeOnlyParam;
  private BooleanType excludeNestedParam;
  private BooleanType excludeNotForUIParam;
  private BooleanType excludePostcoordinatedParam;
  private CodeType displayLanguageParam;
  private List<CanonicalType> excludeSystemParam;
  private List<CanonicalType> systemVersionParam;
  private List<CanonicalType> checkSystemVersionParam;
  private List<CanonicalType> forceSystemVersionParam;
  private List<CanonicalType> useSupplementParam;

  private LanguageParameter languageAcceptLanguageHeader;

  /**
   * Origin of the ValueSet to expand.
   *
   * <p>Defaults to {@link ResourceOrigin#PROVIDED}, so a ValueSet is only treated as stored if the
   * core explicitly resolved it from the service's content. Not {@code transient}, because the
   * origin may change the expansion result and is therefore part of the cache key.
   */
  @Builder.Default private ResourceOrigin resourceOrigin = ResourceOrigin.PROVIDED;

  /**
   * Resolved CodeSystem supplements applicable to this expansion, keyed by the base CodeSystem
   * canonical URL.
   *
   * <p>Populated by the expansion engine after resolving and validating the {@code
   * valueset-supplement} extensions. Marked {@code transient} so it is excluded from the
   * serialization-based cache key.
   */
  private transient java.util.Map<
          String,
    SupplementRequest>
      resolvedSupplements;

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

  @Override
  public String toString() {
    return "VsExpandParameter{"
        + "urlParam="
        + urlParam
        + ", valueSetParam="
        + valueSetParam
        + ", valueSetVersionParam="
        + valueSetVersionParam
        + ", contextParam="
        + contextParam
        + ", contextDirectionParam="
        + contextDirectionParam
        + ", filterParam="
        + filterParam
        + ", dateParam="
        + dateParam
        + ", offsetParam="
        + offsetParam
        + ", countParam="
        + countParam
        + ", includeDesignationsParam="
        + includeDesignationsParam
        + ", designationParam="
        + designationParam
        + ", includeDefinitionParam="
        + includeDefinitionParam
        + ", activeOnlyParam="
        + activeOnlyParam
        + ", excludeNestedParam="
        + excludeNestedParam
        + ", excludeNotForUIParam="
        + excludeNotForUIParam
        + ", excludePostcoordinatedParam="
        + excludePostcoordinatedParam
        + ", displayLanguageParam="
        + displayLanguageParam
        + ", excludeSystemParam="
        + excludeSystemParam
        + ", systemVersionParam="
        + systemVersionParam
        + ", checkSystemVersionParam="
        + checkSystemVersionParam
        + ", forceSystemVersionParam="
        + forceSystemVersionParam
        + ", languageAcceptLanguageHeader="
        + languageAcceptLanguageHeader
        + ", resourceOrigin="
        + resourceOrigin
        + '}';
  }
}
