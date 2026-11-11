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
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.CsLookupParameter;
import de.gematik.zts.termzilla.api.parameters.CsLookupResult;
import de.gematik.zts.termzilla.api.service.TerminologyManager;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class implementing the {@code CodeSystem $lookup} operation across all registered
 * terminology providers.
 *
 * <p>Validates the incoming {@link CsLookupParameter}, resolves the responsible {@link
 * TerminologyManager} via {@link
 * CombiningTerminologyService}, and delegates the lookup to that manager.
 */
@Slf4j
public class CombiningCsLookupOperation {

  private CombiningCsLookupOperation() {}

  /**
   * Performs a {@code $lookup} for the code identified by the given parameters.
   *
   * <p>Parameter validation is applied before dispatching. The {@code coding} parameter is
   * currently not supported and will result in an {@link InvalidRequestException}.
   *
   * @param combiningTerminologyService the service used to resolve the responsible terminology
   *     manager
   * @param scope the terminology scope (tenant identifier)
   * @param csLookupParameter the lookup parameters, including the code system URL and code
   * @return the lookup result produced by the responsible terminology manager
   * @throws NotImplementedException if the operation is not implemented for the target terminology
   * @throws InvalidRequestException if the parameters are invalid or use an unsupported combination
   * @throws UnsupportedTerminologyException if no manager is registered for the given code system
   *     URL
   * @throws UnknownScopeException if the given scope is not known
   */
  public static CsLookupResult lookupCode(
      CombiningTerminologyService combiningTerminologyService,
      String scope,
      CsLookupParameter csLookupParameter)
      throws NotImplementedException,
          InvalidRequestException,
          UnsupportedTerminologyException,
          UnknownScopeException,
          UnknownConceptException {

    // Parametervalidierung
    CombiningCsLookupParameterValidator.validateParameters(csLookupParameter);

    // Codings unterstützen wir derzeit noch nicht
    if (csLookupParameter.getCodingParam() != null
        && !csLookupParameter.getCodingParam().isEmpty()) {
      throw new InvalidRequestException("This parameter combination is currently not supported.");
    }

    // Ermitteln des zuständigen Terminologiemanagers
    TerminologyManager terminologyManager =
        combiningTerminologyService.getResponsibleTerminologyManager(
            csLookupParameter.getSystemParam().getValue());

    // Lookup an den zuständigen Terminologiemanager delegieren
    return terminologyManager.cs().lookupCode(scope, csLookupParameter);
  }
}
