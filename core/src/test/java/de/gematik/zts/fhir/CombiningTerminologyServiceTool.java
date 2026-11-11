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
package de.gematik.zts.fhir;

import de.gematik.zts.termzilla.api.exceptions.NotImplementedException;
import de.gematik.zts.termzilla.api.exceptions.UnknownConceptException;
import de.gematik.zts.termzilla.api.exceptions.UnknownScopeException;
import de.gematik.zts.termzilla.api.exceptions.UnsupportedTerminologyException;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;
import de.gematik.zts.termzilla.server.service.CombiningTerminologyService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class CombiningTerminologyServiceTool {

  private CombiningTerminologyService combiningTerminologyService;

  public CombiningTerminologyServiceTool(CombiningTerminologyService combiningTerminologyService) {
    this.combiningTerminologyService = combiningTerminologyService;
  }

  public List<ValueSet.ValueSetExpansionContainsComponent> expandVs(
      VsExpandParameter expandParam, String scope)
      throws NotImplementedException,
          UnknownConceptException,
          UnsupportedTerminologyException,
          UnknownScopeException {

    List<ValueSet.ValueSetExpansionContainsComponent> completeContainsList = new ArrayList<>();
    int total;

    do {

      int offset = completeContainsList.size();
      expandParam.setOffsetParam(new IntegerType(offset));
      log.info("Expanding ValueSet with offset: {}", offset);

      // Execute the expansion operation
      ValueSet vs = combiningTerminologyService.expandValueSet(scope, expandParam);

      // Add the contains components to the complete list
      completeContainsList.addAll(vs.getExpansion().getContains());

      // Check if there are more pages to fetch
      total = vs.getExpansion().getTotalElement().getValue();
    } while (total > completeContainsList.size());

    log.info("Expanded ValueSet contains {} concepts", completeContainsList.size());
    return completeContainsList;
  }
}
