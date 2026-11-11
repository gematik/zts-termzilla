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
package de.gematik.zts.termzilla.terminologies.snomed.scg;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.gematik.zts.termzilla.terminologies.snomed.model.SnomedConcept;
import org.snomed.languages.scg.domain.model.Attribute;
import org.snomed.languages.scg.domain.model.AttributeValue;

/** Provides reusable test fixtures for DemisSCGProcessor tests. */
public final class DemisScgProcessorTestDataHelper {

  private DemisScgProcessorTestDataHelper() {}

  static MrcmDomainRule domainRule(String id, String referencedComponentId) {
    MrcmDomainRule rule = new MrcmDomainRule();
    rule.setId(id);
    rule.setReferencedComponentId(referencedComponentId);
    return rule;
  }

  static SnomedConcept conceptWithDisplay(String displayValue) {
    SnomedConcept concept = mock(SnomedConcept.class);
    doReturn(displayValue).when(concept).getDefaultDisplayValue(anyString());
    return concept;
  }

  static AttributeValue conceptValue(String conceptId) {
    AttributeValue attributeValue = mock(AttributeValue.class);
    doReturn(conceptId).when(attributeValue).getConceptId();
    return attributeValue;
  }

  static AttributeValue nestedExpressionValue(
      org.snomed.languages.scg.domain.model.Expression expression) {
    AttributeValue attributeValue = mock(AttributeValue.class);
    doReturn(expression).when(attributeValue).getNestedExpression();
    return attributeValue;
  }

  static Attribute attribute(String attributeId, AttributeValue attributeValue) {
    Attribute attribute = mock(Attribute.class);
    doReturn(attributeId).when(attribute).getAttributeId();
    doReturn(attributeValue).when(attribute).getAttributeValue();
    return attribute;
  }
}
