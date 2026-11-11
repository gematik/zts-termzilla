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
package de.gematik.zts.termzilla.server.service.vs;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.VsSearchParameter;
import de.gematik.zts.termzilla.server.service.CombiningGenericSearchOperation;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class CombiningVsSearchOperation {

  private CombiningVsSearchOperation() {
    // Private constructor to prevent instantiation
  }

  /**
   * Searches for ValueSets matching the search parameters.
   *
   * @param combiningTerminologyService the combining terminology service
   * @param vsSearchParameter           the search parameters
   * @param scope                       the search scope
   * @return list of matching ValueSet IDs, sorted according to search parameters
   * @throws InvalidRequestException if the request is invalid
   * @throws NotImplementedException if the operation is not implemented
   */
  public static List<IdType> searchValueSets(
      CombiningTerminologyService combiningTerminologyService,
      VsSearchParameter vsSearchParameter,
      String scope)
      throws InvalidRequestException, NotImplementedException {

    log.debug("CombiningVsSearchOperation.searchValueSetReturnIds called for scope {}", scope);

    CombiningGenericSearchOperation<VsSearchParameter, ValueSet> searchOperation =
        new CombiningGenericSearchOperation<>(combiningTerminologyService, ValueSet.class);

    return searchOperation.searchAndSort(
        vsSearchParameter, scope, (manager, s, param) -> manager.vs().searchValueSet(s, param));
  }
}
