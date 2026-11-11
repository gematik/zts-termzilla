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

import de.gematik.zts.termzilla.api.parameters.TerminologyResourceSearchParameter;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Sort;

/**
 * Sort generator for FHIR ConceptMap resources in Lucene-based searches.
 *
 * <p>This class provides sorting functionality for ConceptMap metadata searches, supporting all
 * common terminology resource sort fields.
 *
 * <p><b>Supported Sort Fields:</b>
 *
 * <ul>
 *   <li>{@code name} - ConceptMap name
 *   <li>{@code url} - Canonical URL
 *   <li>{@code version} - ConceptMap version
 *   <li>{@code title} - Human-readable title
 *   <li>{@code date} - Publication/last update date
 *   <li>{@code publisher} - Organization responsible for the ConceptMap
 *   <li>{@code status} - Publication status (draft, active, retired, etc.)
 *   <li>{@code description} - Natural language description
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 *
 * <ul>
 *   <li>{@code ?_sort=name} - Sort by name ascending
 *   <li>{@code ?_sort=-date} - Sort by date descending (newest first)
 *   <li>{@code ?_sort=status,-date,name} - Multi-field sort
 * </ul>
 *
 * <p><b>Default Behavior:</b>
 *
 * <p>If no sort parameters are provided, ConceptMaps are sorted by {@code name} in ascending order.
 *
 * @see AbstractTerminologyResourceSortGenerator
 * @see CodeSystemSortGenerator
 * @see ValueSetSortGenerator
 */
@Slf4j
public final class ConceptMapSortGenerator extends AbstractTerminologyResourceSortGenerator {

  // Set of supported sort fields for ConceptMap resources
  private static final Set<String> SUPPORTED_SORT_FIELDS =
      Set.of(
          TerminologyResourceSearchParameter.SP_NAME,
          TerminologyResourceSearchParameter.SP_URL,
          TerminologyResourceSearchParameter.SP_VERSION,
          TerminologyResourceSearchParameter.SP_TITLE,
          TerminologyResourceSearchParameter.SP_DATE,
          TerminologyResourceSearchParameter.SP_PUBLISHER,
          TerminologyResourceSearchParameter.SP_STATUS,
          TerminologyResourceSearchParameter.SP_DESCRIPTION);

  /** Private constructor to prevent instantiation of utility class. */
  private ConceptMapSortGenerator() {
    super();
  }

  /**
   * Generates a Lucene Sort specification from FHIR _sort parameters for ConceptMap resources.
   *
   * @param sortParamList list of FHIR _sort parameter values (may be null or empty)
   * @return a Lucene Sort object containing all requested sort fields
   */
  public static Sort generateSort(List<String> sortParamList) {
    return AbstractTerminologyResourceSortGenerator.generateSort(
        sortParamList,
        SUPPORTED_SORT_FIELDS,
        AbstractTerminologyResourceSortGenerator::createCommonSortField);
  }
}
