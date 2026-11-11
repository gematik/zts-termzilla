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

import java.util.UUID;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;

public class ValuSetBuilder {

  public static ValueSet createAllConceptsValueSet(String url, String version) {
    ValueSet vs = new ValueSet();
    vs.setUrl("http://example.com/fhir/ValueSet/all-concepts-" + UUID.randomUUID().toString());
    vs.setName("All Concepts of '" + url + "'");
    vs.setTitle("All Concepts ValueSet for " + url);
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    vs.setDescription("This ValueSet contains all concepts of the CodeSystem with URL: " + url);

    ValueSet.ValueSetComposeComponent vsc = new ValueSet.ValueSetComposeComponent();
    vsc.addInclude().setSystem(url).setVersion(version);
    vs.setCompose(vsc);

    return vs;
  }
}
