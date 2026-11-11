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
package de.gematik.zts.termzilla.terminologies.snomed.provider.cs.search;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.parameters.CsSearchParameter;
import de.gematik.zts.termzilla.common.codesystem.search.InMemoryCodeSystemSearchHelper;
import de.gematik.zts.termzilla.terminologies.snomed.SnomedCodeSystemService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.IdType;

/**
 * Executes SNOMED CodeSystem search requests.
 *
 * <p>The operation delegates filtering to {@link InMemoryCodeSystemSearchHelper} using the
 * in-memory list of SNOMED code systems provided by {@link SnomedCodeSystemService}.
 */
@Slf4j
public class SnomedCodeSystemSearchOperation {

  /** In-memory SNOMED code systems available for search. */
  private final List<CodeSystem> codeSystemList;

  /**
   * Creates a search operation backed by SNOMED code systems from the service.
   *
   * @param snomedsnomedCodeSystemService SNOMED CodeSystem service
   */
  public SnomedCodeSystemSearchOperation(SnomedCodeSystemService snomedsnomedCodeSystemService) {
    this.codeSystemList = snomedsnomedCodeSystemService.getCodeSystemList();
  }

  /**
   * Searches SNOMED CodeSystems using the provided search parameters.
   *
   * @param csSearchParameter parsed CodeSystem search parameters
   * @return list of matching CodeSystem resource ids
   * @throws InvalidRequestException if request parameters are invalid
   * @throws NotImplementedException if an unsupported search feature is requested
   */
  public List<IdType> searchCodeSystems(CsSearchParameter csSearchParameter)
      throws InvalidRequestException, NotImplementedException {

    return InMemoryCodeSystemSearchHelper.search(
        codeSystemList, csSearchParameter, cs -> log.debug("CodeSystem: {}", cs.getId()));
  }
}
