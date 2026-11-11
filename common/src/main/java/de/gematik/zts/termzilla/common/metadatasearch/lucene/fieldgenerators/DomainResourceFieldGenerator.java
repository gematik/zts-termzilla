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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators;

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.DomainResourceFieldNames.NAME_EXT_EFFECTIVE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.DomainResourceFieldNames.NAME_TEXT;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

public abstract class DomainResourceFieldGenerator<R extends DomainResource>
    extends ResourceFieldGenerator<R> {

  @Override
  public void createFields(Document document, R resource, Field.Store fieldStore) {

    super.createFields(document, resource, fieldStore);

    // ELEMENT: DomainResource.text [0..1] (Narrative) --> SEARCHPARAM: _text (string)
    if (!StringUtils.isEmpty(resource.getText().toString()))
    // TODO
    {
      document.add(new TextField(NAME_TEXT, resource.getText().toString(), fieldStore));
    }

    // ------------------------------------------------------------------------------------------------
    // Zusätzliche Suchparameter aus R5 abgebildet auf R4

    // SEARCHPARAM: effective (date)
    // ELEMENT: CodeSystem.extension
    // http://hl7.org/fhir/StructureDefinition/resource-effectivePeriod [0..*] (Period)
    List<Extension> effectivePeriodExtensions =
        resource.getExtensionsByUrl(
            "http://hl7.org/fhir/StructureDefinition/resource-effectivePeriod");

    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
        document, NAME_EXT_EFFECTIVE, effectivePeriodExtensions.isEmpty());

    if (!effectivePeriodExtensions.isEmpty()) {
      for (Extension extension : effectivePeriodExtensions) {
        if (extension.getValue() instanceof Period period) {
          ResourceFieldGeneratorHelper.addPeriodDateParamFieldToDocument(
              document, NAME_EXT_EFFECTIVE, period);
        }
      }
    }
  }
}
