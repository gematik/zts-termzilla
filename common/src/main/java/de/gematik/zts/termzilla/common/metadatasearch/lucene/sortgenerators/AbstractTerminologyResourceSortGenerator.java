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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.sortgenerators;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_DESCRIPTION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_NAME;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_PUBLISHER;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_STATUS;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_TITLE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_URL;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.NAME_VERSION;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SEPARATOR;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_SORT;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.CodeSystemFieldNames.SUFFIX_STRING_SEARCH;

import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hl7.fhir.r4.model.CodeSystem;

/**
 * Abstract base class for generating Lucene sort specifications for FHIR terminology resources.
 *
 * <p>This class provides common functionality for sorting terminology resources (CodeSystem,
 * ValueSet, ConceptMap) in Lucene-based searches. Subclasses can customize the set of supported
 * sort fields and the default sort behavior.
 *
 * <p><b>Common Supported Sort Fields:</b>
 *
 * <ul>
 *   <li>{@code name} - Resource name (e.g., CodeSystem.name)
 *   <li>{@code url} - Canonical URL
 *   <li>{@code version} - Resource version
 *   <li>{@code title} - Human-readable title
 *   <li>{@code date} - Publication/last update date
 *   <li>{@code publisher} - Organization responsible for the resource
 *   <li>{@code status} - Publication status (draft, active, retired, etc.)
 *   <li>{@code description} - Natural language description
 * </ul>
 *
 * <p><b>Sort Parameter Syntax:</b>
 *
 * <ul>
 *   <li>Ascending order: {@code ?_sort=name}
 *   <li>Descending order: {@code ?_sort=-name}
 *   <li>Multiple fields: {@code ?_sort=status,-date,name}
 * </ul>
 *
 * <p><b>Default Behavior:</b>
 *
 * <p>If no sort parameters are provided, resources are sorted by {@code name} in ascending order.
 *
 * <p><b>Field Mapping:</b>
 *
 * <p>Each FHIR search parameter is mapped to a Lucene field with a {@code _sort} suffix for
 * efficient sorting. String fields use case-insensitive sorting, while date fields use numeric
 * (LONG) sorting.
 *
 * @see CodeSystemSortGenerator
 * @see ValueSetSortGenerator
 * @see ConceptMapSortGenerator
 */
@Slf4j
public abstract class AbstractTerminologyResourceSortGenerator {

  /** Protected constructor to prevent direct instantiation. */
  protected AbstractTerminologyResourceSortGenerator() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Returns the default sort field configuration when no sort parameters are provided.
   *
   * <p>The default implementation sorts by {@code name} in ascending order.
   *
   * @return a Sort object representing the default sort configuration
   */
  protected static Sort getDefaultSort() {
    return new Sort(
        new SortField(
            NAME_NAME + SEPARATOR + SUFFIX_STRING_SEARCH,
            SortField.Type.STRING,
            false)); // false = ascending
  }

  /**
   * Generates a Lucene Sort specification from FHIR _sort parameters.
   *
   * <p>This method processes a list of sort parameters and converts them into Lucene SortField
   * objects. Each parameter can optionally be prefixed with a minus sign (-) to indicate descending
   * order.
   *
   * <p><b>Processing Steps:</b>
   *
   * <ol>
   *   <li>If no sort parameters provided, return the default sort
   *   <li>Convert all parameters to lowercase for case-insensitive matching
   *   <li>Ignore null or unsupported sort parameters
   *   <li>For each parameter:
   *       <ul>
   *         <li>Check for descending order prefix (-)
   *         <li>Validate against supported fields
   *         <li>Map to appropriate Lucene field name and type
   *       </ul>
   *   <li>Combine all sort fields into a single Sort object
   *   <li>If no valid sort fields remain, return the default sort
   * </ol>
   *
   * <p><b>Examples:</b>
   *
   * <ul>
   *   <li>{@code generateSort(null, supportedFields)} → Sort by name ascending (default)
   *   <li>{@code generateSort(List.of("status"), supportedFields)} → Sort by status ascending
   *   <li>{@code generateSort(List.of("-date"), supportedFields)} → Sort by date descending
   *   <li>{@code generateSort(List.of("status", "-date", "name"), supportedFields)} → Multi-field
   *       sort
   * </ul>
   *
   * @param sortParamList list of FHIR _sort parameter values (may be null or empty)
   * @param supportedSortFields set of supported sort field names for validation
   * @param createSortFieldFunction functional interface to create resource-specific sort fields
   * @return a Lucene Sort object containing all requested sort fields
   */
  protected static Sort generateSort(
      List<String> sortParamList,
      Set<String> supportedSortFields,
      SortFieldCreator createSortFieldFunction) {

    // Use default sort if no parameters provided
    if (sortParamList == null || sortParamList.isEmpty()) {
      return getDefaultSort();
    }

    // Build list of sort fields
    List<SortField> resultSortList = new ArrayList<>();

    // Process each sort parameter
    for (String sortParam : sortParamList) {
      SortField sortField =
          createSortField(sortParam, supportedSortFields, createSortFieldFunction);
      if (sortField != null) {
        resultSortList.add(sortField);
      }
    }

    if (resultSortList.isEmpty()) {
      return getDefaultSort();
    }

    return new Sort(resultSortList.toArray(new SortField[0]));
  }

  private static SortField createSortField(
      String rawSortParam,
      Set<String> supportedSortFields,
      SortFieldCreator createSortFieldFunction) {

    if (rawSortParam == null) {
      log.warn("Ignoring null sort parameter");
      return null;
    }

    // Normalize to lowercase for case-insensitive matching
    String normalizedSortParam = rawSortParam.toLowerCase(Locale.ROOT);

    // Determine sort order (default is ascending)
    boolean isDescending = normalizedSortParam.startsWith("-");
    String sortParam = isDescending ? normalizedSortParam.substring(1) : normalizedSortParam;

    // Validate that the sort field is supported
    if (!supportedSortFields.contains(sortParam)) {
      log.warn("Unsupported sort parameter: {}", sortParam);
      return null;
    }

    return createSortFieldFunction.create(sortParam, isDescending);
  }

  /**
   * Creates a Lucene SortField for common FHIR terminology resource search parameters.
   *
   * <p>This method maps FHIR search parameter names to their corresponding Lucene field names and
   * types.
   *
   * <p><b>Field Type Mapping:</b>
   *
   * <ul>
   *   <li>String fields (name, url, version, etc.) → SortField.Type.STRING
   *   <li>Date fields → SortField.Type.LONG (milliseconds since epoch)
   * </ul>
   *
   * @param sortParam the FHIR search parameter name (e.g., "name", "date")
   * @param isDescending true for descending order, false for ascending
   * @return a SortField object, or null if the parameter is not recognized
   */
  protected static SortField createCommonSortField(String sortParam, boolean isDescending) {
    return switch (sortParam) {
      case TerminologyResourceSearchParameter.SP_NAME ->
          new SortField(NAME_NAME + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_URL ->
          new SortField(NAME_URL + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_VERSION ->
          new SortField(
              NAME_VERSION + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_TITLE ->
          new SortField(NAME_TITLE + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_DATE ->
          new SortField(
              CodeSystem.SP_DATE + SEPARATOR + SUFFIX_SORT, SortField.Type.LONG, isDescending);
      case TerminologyResourceSearchParameter.SP_PUBLISHER ->
          new SortField(
              NAME_PUBLISHER + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_STATUS ->
          new SortField(NAME_STATUS + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      case TerminologyResourceSearchParameter.SP_DESCRIPTION ->
          new SortField(
              NAME_DESCRIPTION + SEPARATOR + SUFFIX_SORT, SortField.Type.STRING, isDescending);
      default -> null;
    };
  }

  /** Functional interface for creating resource-specific sort fields. */
  @FunctionalInterface
  protected interface SortFieldCreator {

    SortField create(String sortParam, boolean isDescending);
  }
}
