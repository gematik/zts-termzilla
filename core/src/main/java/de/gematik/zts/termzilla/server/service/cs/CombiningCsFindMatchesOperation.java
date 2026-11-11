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
package de.gematik.zts.termzilla.server.service.cs;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesParameter;
import de.gematik.zts.termzilla.api.parameters.CsFindMatchesResult;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Coordinates the {@code CodeSystem/$find-matches} operation by routing requests to the responsible
 * {@link TerminologyManager}.
 */
@Slf4j
public class CombiningCsFindMatchesOperation {

  /** Utility class constructor. */
  private CombiningCsFindMatchesOperation() {}

  /**
   * Resolves the responsible terminology manager for the requested system and delegates the {@code
   * $find-matches} execution.
   *
   * @param combiningTerminologyService service used to resolve the responsible terminology manager
   * @param scope scope value used for the matching request
   * @param csFindMatchesParameter operation parameters including the required system
   * @return the matching result returned by the responsible terminology manager
   * @throws UnsupportedTerminologyException if no manager supports the requested terminology
   * @throws NotImplementedException if the target manager does not implement the operation
   * @throws UnknownScopeException if the given scope is not recognized by the target manager
   * @throws InvalidRequestException if the required {@code system} parameter is missing
   */
  public static CsFindMatchesResult findMatches(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      CsFindMatchesParameter csFindMatchesParameter)
      throws UnsupportedTerminologyException, NotImplementedException, UnknownScopeException {

    // Ermitteln des zuständigen TerminologyManagers anhand der übergebenen System-URL
    TerminologyManager manager =
        combiningTerminologyService.getResponsibleTerminologyManager(
            csFindMatchesParameter.getSystemParam().getValue());
    log.debug("Responsible TerminologyManager: {}", manager.getTerminologyManagerName());

    // FindMatches-Operation durch den zuständigen TerminologyManager ausführen lassen und das
    // Ergebnis zurückgeben
    return manager.cs().findMatches(scope, csFindMatchesParameter);
  }
}
