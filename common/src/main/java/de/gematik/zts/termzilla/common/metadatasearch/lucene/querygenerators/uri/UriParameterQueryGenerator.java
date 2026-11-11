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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.uri;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_URI_EXACT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.BaseFieldNames.SUFFIX_URL_PATH_BELOW_EXACT;

import ca.uhn.fhir.rest.param.UriAndListParam;
import ca.uhn.fhir.rest.param.UriOrListParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.param.UriParamQualifierEnum;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.ParamData;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.QueryCombinators;
import de.gematik.zts.termzilla.common.tools.UriHelper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Static utility class for generating Lucene queries for FHIR URI search parameters.
 *
 * <p>This class provides static methods for building queries for FHIR URI-type search parameters
 * such as url, identifier systems, and other canonical references. URI parameters support both
 * exact matching and hierarchical prefix matching.
 *
 * <p><b>Supported URI types:</b>
 *
 * <ul>
 *   <li>HTTP/HTTPS URLs - Web-based canonical references
 *   <li>URNs and other URI schemes - Alternative identification schemes
 * </ul>
 *
 * <p><b>Supported search modifiers:</b>
 *
 * <ul>
 *   <li>Standard search - Exact URI match (case-sensitive)
 *   <li>{@code :below} - Hierarchical prefix matching (implemented for HTTP/HTTPS URLs)
 *   <li>{@code :missing} - Search for presence/absence of the parameter
 * </ul>
 *
 * <p><b>Unsupported modifiers (throw {@link NotImplementedException}):</b>
 *
 * <ul>
 *   <li>{@code :above} - Hierarchical parent matching (not yet implemented)
 * </ul>
 *
 * <p><b>Query Structure:</b>
 *
 * <p>URI queries are indexed in different fields depending on the operation:
 *
 * <ul>
 *   <li>{@code paramName_uriexact} - Exact URI matching
 *   <li>{@code paramName_urlpathbelowexact} - Hierarchical prefix matching for URLs
 * </ul>
 *
 * <p><b>URI Normalization:</b>
 *
 * <p>All URIs are normalized using {@link UriHelper#normalizeUrl(String)} before indexing and
 * querying. This ensures consistent matching across different URI representations.
 *
 * <p><b>FHIR Examples:</b>
 *
 * <ul>
 *   <li>{@code ?url=http://hl7.org/fhir/ValueSet/example} - Exact URL match
 *   <li>{@code ?url:below=http://hl7.org/fhir} - All URLs under this path
 *   <li>{@code ?url:missing=true} - Resources without a URL
 * </ul>
 *
 * @see QueryCombinators
 * @see UriHelper
 * @see org.apache.lucene.search.TermQuery
 */
@Slf4j
public final class UriParameterQueryGenerator {

  /** Private constructor to prevent instantiation of utility class. */
  private UriParameterQueryGenerator() {
    throw new AssertionError(
        "UriParameterQueryGenerator is a utility class and should not be instantiated");
  }

  /**
   * Prepares a Lucene query for FHIR URI search parameters from ParamData.
   *
   * <p>This method handles AND/OR combinations of URI parameters. FHIR allows comma-separated
   * values (OR) and multiple parameter instances (AND):
   *
   * <ul>
   *   <li>{@code ?url=val1,val2} - OR combination (at least one must match)
   *   <li>{@code ?url=val1&url=val2} - AND combination (both must match)
   * </ul>
   *
   * @param andListParamData the parameter data containing UriAndListParam
   * @return a Lucene query combining all URI conditions with appropriate AND/OR logic
   * @throws NotImplementedException if an unsupported modifier is encountered
   */
  public static Query prepareQuery(ParamData andListParamData) throws NotImplementedException {

    UriAndListParam andListParam = (UriAndListParam) andListParamData.getParam();
    List<Query> andQueryList = new ArrayList<>();

    for (UriOrListParam orListParam : andListParam.getValuesAsQueryTokens()) {
      List<Query> orQueryList = new ArrayList<>();
      for (UriParam param : orListParam.getValuesAsQueryTokens()) {
        orQueryList.add(prepareQuery(andListParamData.getParamName(), param));
      }
      if (!orQueryList.isEmpty()) {
        andQueryList.add(QueryCombinators.prepareOrQuery(orQueryList));
      }
    }
    return QueryCombinators.prepareAndQuery(andQueryList);
  }

  /**
   * Prepares a Lucene query for a single FHIR URI parameter.
   *
   * <p>This method routes the query generation based on the parameter's qualifier:
   *
   * <ul>
   *   <li>No qualifier - Standard exact match search
   *   <li>{@code :below} - Hierarchical prefix matching (for HTTP/HTTPS URLs)
   *   <li>{@code :missing} - Presence/absence search
   *   <li>{@code :above} - Throws NotImplementedException (not yet implemented)
   * </ul>
   *
   * @param paramName the FHIR search parameter name (e.g., "url", "system")
   * @param param the URI parameter containing the value and optional qualifier
   * @return a Lucene query for the URI parameter
   * @throws NotImplementedException if an unsupported qualifier is used
   */
  public static Query prepareQuery(String paramName, UriParam param)
      throws NotImplementedException {

    Query query;
    if (param == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.isEmpty() && param.getMissing() == null) {
      query = MatchAllDocsQuery.INSTANCE;
    } else if (param.getMissing() != null) {
      // :missing search modifier
      query = QueryCombinators.prepareMissingModifierQuery(paramName, param);
    } else if (param.getQualifier() == UriParamQualifierEnum.ABOVE) {
      // :above search modifier
      query = prepareAboveModifierQuery(paramName, param);
    } else if (param.getQualifier() == UriParamQualifierEnum.BELOW) {
      // :below search modifier
      query = prepareBelowModifierQuery(paramName, param);
    } else {
      // standard search
      query = prepareStandardQuery(paramName, param);
    }

    return query;
  }

  /**
   * Prepares a query for the {@code :above} modifier (not yet implemented).
   *
   * <p>The :above modifier would perform hierarchical parent matching, finding URIs that are
   * ancestors of the specified URI in a hierarchical namespace.
   *
   * @param paramName the search parameter name
   * @param param the URI parameter
   * @return a Lucene query (not implemented)
   * @throws NotImplementedException always thrown - modifier not yet supported
   */
  @SuppressWarnings("java:S1172") // When the method is implemented, the parameters will be used
  public static Query prepareAboveModifierQuery(String paramName, UriParam param)
      throws NotImplementedException {
    throw new NotImplementedException(":above modifier not implemented yet in search engine");
  }

  /**
   * Prepares a query for the {@code :below} modifier for hierarchical prefix matching.
   *
   * <p>The :below modifier performs hierarchical prefix matching on URIs. For HTTP/HTTPS URLs, this
   * matches all URIs that start with the specified prefix, enabling searches like "find all
   * resources under http://hl7.org/fhir".
   *
   * <p><b>Implementation notes:</b>
   *
   * <ul>
   *   <li>Only HTTP and HTTPS URLs are supported
   *   <li>The URI is normalized using {@link UriHelper#normalizeUrl(String)}
   *   <li>Uses the {@code paramName_urlpathbelowexact} field for matching
   * </ul>
   *
   * <p><b>FHIR Example:</b> {@code ?url:below=http://hl7.org/fhir} matches all URLs under
   * http://hl7.org/fhir, such as http://hl7.org/fhir/ValueSet/example
   *
   * @param paramName the search parameter name
   * @param param the URI parameter containing the prefix URI
   * @return a TermQuery for hierarchical prefix matching
   * @throws NotImplementedException if the URI scheme is not HTTP/HTTPS
   * @throws IllegalArgumentException if the URI is malformed
   */
  public static Query prepareBelowModifierQuery(String paramName, UriParam param)
      throws NotImplementedException {

    String paramValueValue = param.getValue();
    URI uri;
    try {
      uri = URI.create(UriHelper.normalizeUrl(paramValueValue));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URI: " + paramValueValue, e);
    }

    // Distinguish between different URI types
    if (uri.getScheme() != null
        && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {

      String inField = paramName + SEPARATOR + SUFFIX_URL_PATH_BELOW_EXACT;

      return new TermQuery(new Term(inField, uri.toString()));

    } else {
      throw new NotImplementedException(":below modifier can only be used with http/https-URLs");
    }
  }

  /**
   * Prepares a standard exact-match query for a URI parameter.
   *
   * <p>This method creates a {@link TermQuery} for exact matching on URI values. The URI is
   * normalized before querying to ensure consistent matching regardless of variations in URI
   * representation (e.g., trailing slashes, URL encoding).
   *
   * <p><b>URI Handling:</b>
   *
   * <ul>
   *   <li>HTTP/HTTPS URLs - Web-based identifiers (most common for FHIR canonical references)
   *   <li>Other URI schemes - URNs and alternative identification schemes
   *   <li>All URIs are normalized via {@link UriHelper#normalizeUrl(String)}
   * </ul>
   *
   * <p>The query targets the {@code paramName_uriexact} field.
   *
   * <p><b>FHIR Examples:</b>
   *
   * <ul>
   *   <li>{@code ?url=http://hl7.org/fhir/ValueSet/example} - Exact match
   *   <li>{@code ?system=http://snomed.info/sct} - SNOMED CT system URI
   *   <li>{@code ?url=urn:oid:2.16.840.1.113883.6.96} - OID-based URN
   * </ul>
   *
   * @param paramName the search parameter name
   * @param param the URI parameter containing the value
   * @return a TermQuery for exact URI matching
   * @throws IllegalArgumentException if the URI is malformed
   */
  public static Query prepareStandardQuery(String paramName, UriParam param) {

    String paramValueValue = param.getValue();
    URI uri;
    try {
      uri = URI.create(UriHelper.normalizeUrl(paramValueValue));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URI: " + paramValueValue, e);
    }

    // Use the same field for all URI types (HTTP/HTTPS and others)
    String inField = paramName + SEPARATOR + SUFFIX_URI_EXACT;

    return new TermQuery(new Term(inField, uri.toString()));
  }
}
