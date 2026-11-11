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

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import de.gematik.zts.termzilla.api.exceptions.CacheKeyCreationException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Setter
@SuperBuilder(buildMethodName = "buildInternal")
public class TerminologyResourceSearchParameter implements Serializable {

  public static final String SP_ID = Constants.PARAM_ID;
  public static final String SP_LAST_UPDATED = Constants.PARAM_LASTUPDATED;
  public static final String SP_TAG = Constants.PARAM_TAG;
  public static final String SP_PROFILE = Constants.PARAM_PROFILE;
  public static final String SP_SECURITY = Constants.PARAM_SECURITY;
  public static final String SP_TEXT = Constants.PARAM_TEXT;
  public static final String SP_SOURCE = Constants.PARAM_SOURCE;
  public static final String SP_CONTENT = Constants.PARAM_CONTENT;

  public static final String SP_SORT = Constants.PARAM_SORT;
  public static final String SP_COUNT = Constants.PARAM_COUNT;
  public static final String SP_OFFSET = Constants.PARAM_OFFSET;

  // special case (search for effective-period extension)
  public static final String SP_EFFECTIVE = "effective";

  // these parameters are only defined in the Resource classes (CodeSystem, ValueSet, ConceptMap),
  // but not in the base Resource class. As they are common among the MetadataResource,
  // we define them here as well for easier handling in the search operations

  public static final String SP_CONTEXT = "context";
  public static final String SP_CONTEXT_QUANTITY = "context-quantity";
  public static final String SP_CONTEXT_TYPE = "context-type";
  public static final String SP_CONTEXT_TYPE_QUANTITY = "context-type-quantity";
  public static final String SP_CONTEXT_TYPE_VALUE = "context-type-value";
  public static final String SP_DATE = "date";
  public static final String SP_DESCRIPTION = "description";
  public static final String SP_IDENTIFIER = "identifier";
  public static final String SP_JURISDICTION = "jurisdiction";
  public static final String SP_NAME = "name";
  public static final String SP_PUBLISHER = "publisher";
  public static final String SP_STATUS = "status";
  public static final String SP_TITLE = "title";
  public static final String SP_URL = "url";
  public static final String SP_VERSION = "version";

  protected TokenParam idParam;
  protected UriParam sourceParam;
  protected UriParam urlParam;
  protected TokenParam versionParam;
  protected DateParam dateParam;
  protected DateParam effectiveParam;
  protected TokenParam statusParam;
  protected StringParam nameParam;
  protected StringParam titleParam;
  protected StringParam descriptionParam;
  protected StringParam publisherParam;
  protected StringParam contentParam;
  protected TokenOrListParam sortParam;

  public Optional<TokenParam> getId() {
    return Optional.ofNullable(idParam);
  }

  public Optional<UriParam> getSource() {
    return Optional.ofNullable(sourceParam);
  }

  public Optional<UriParam> getUrl() {
    return Optional.ofNullable(urlParam);
  }

  public Optional<TokenParam> getVersion() {
    return Optional.ofNullable(versionParam);
  }

  public Optional<DateParam> getDate() {
    return Optional.ofNullable(dateParam);
  }

  public Optional<DateParam> getEffective() {
    return Optional.ofNullable(effectiveParam);
  }

  public Optional<TokenParam> getStatus() {
    return Optional.ofNullable(statusParam);
  }

  public Optional<StringParam> getName() {
    return Optional.ofNullable(nameParam);
  }

  public Optional<StringParam> getTitle() {
    return Optional.ofNullable(titleParam);
  }

  public Optional<StringParam> getDescription() {
    return Optional.ofNullable(descriptionParam);
  }

  public Optional<StringParam> getPublisher() {
    return Optional.ofNullable(publisherParam);
  }

  public Optional<StringParam> getContent() {
    return Optional.ofNullable(contentParam);
  }

  public Optional<TokenOrListParam> getSort() {
    return Optional.ofNullable(sortParam);
  }

  // ----------------------------------------------
  // Helper Methods
  // ----------------------------------------------

  public boolean isContentSearch() {
    return contentParam != null;
  }

  public boolean isMetadataSearch() {
    return idParam != null
        || sourceParam != null
        || urlParam != null
        || versionParam != null
        || dateParam != null
        || statusParam != null
        || nameParam != null
        || titleParam != null
        || descriptionParam != null
        || publisherParam != null;
  }

  public boolean isSortSearch() {
    return sortParam != null;
  }

  // Splits a parameter in the format "url|version" and stores version separately.
  protected void extractVersionFromParam(UriParam param) {
    if (param != null && versionParam == null) {
      String[] parts = param.getValue().split("\\|", 2);
      if (parts.length == 2
          && StringUtils.isNotEmpty(parts[0])
          && StringUtils.isNotEmpty(parts[1])) {
        param.setValue(parts[0]);
        this.versionParam = new TokenParam(parts[1]);
      }
    }
  }

  /** Applies normalization after builder-based instantiation. */
  protected void postBuildNormalize() {
    extractVersionFromParam(urlParam);
  }

  public abstract static class TerminologyResourceSearchParameterBuilder<
      C extends TerminologyResourceSearchParameter,
      B extends TerminologyResourceSearchParameterBuilder<C, B>> {

    public C build() {
      C parameter = buildInternal();
      parameter.postBuildNormalize();
      return parameter;
    }
  }

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
