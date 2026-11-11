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
package de.gematik.zts.termzilla.testutils.valueset;

import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;

public class ValueSetGenerator {

  private ValueSetGenerator() {
    // private constructor to prevent instantiation
  }

  public static ValueSet generateAllConceptsValueSet(String csUrl, String csVersion) {
    return generateValueSetWithFilter(csUrl, csVersion, null);
  }

  public static ValueSet generateAllConceptsValueSetWithLockedDate(
      String csUrl, String lockedDate) {

    ValueSet vs = generateValueSetWithFilter(csUrl, null, null);
    vs.getCompose().setLockedDateElement(new DateType(lockedDate));
    return vs;
  }

  public static ValueSet generateParentExistsFalseValueSet(String csUrl, String csVersion) {

    ValueSet.ConceptSetFilterComponent filter = new ValueSet.ConceptSetFilterComponent();
    filter.setProperty("parent");
    filter.setOp(ValueSet.FilterOperator.EXISTS);
    filter.setValue("false");

    return generateValueSetWithFilter(csUrl, csVersion, filter);
  }

  public static ValueSet generateParentIsConceptValueSet(
      String csUrl, String csVersion, String parentCode) {

    ValueSet.ConceptSetFilterComponent filter = new ValueSet.ConceptSetFilterComponent();
    filter.setProperty("parent");
    filter.setOp(ValueSet.FilterOperator.EQUAL);
    filter.setValue(parentCode);

    return generateValueSetWithFilter(csUrl, csVersion, filter);
  }

  public static ValueSet generateValueSetWithFilter(
      String csUrl, String csVersion, ValueSet.ConceptSetFilterComponent filter) {

    ValueSet vs = new ValueSet();
    vs.setStatus(Enumerations.PublicationStatus.UNKNOWN);

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem(csUrl);
    include.setVersion(csVersion);
    if (filter != null) {
      include.addFilter(filter);
    }
    compose.addInclude(include);

    vs.setCompose(compose);

    return vs; // Placeholder return
  }
}
