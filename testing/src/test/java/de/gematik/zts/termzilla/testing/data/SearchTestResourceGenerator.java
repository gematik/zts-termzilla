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
package de.gematik.zts.termzilla.testing.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;

public class SearchTestResourceGenerator {

  // ==================================================================================================================

  public static final String DEFAULT_VS_URL_PREFIX = "http://example.org/fhir/ValueSet/";

  // ==================================================================================================================

  public static List<ValueSet> generateValueSetsForSearchTests() {
    List<ValueSet> valueSetList = new ArrayList<>();
    valueSetList.add(generateAllDataValueSet("id-01", "AAA 01"));
    valueSetList.add(generateAllDataValueSet("id-02", "AAA 02"));
    valueSetList.add(generateAllDataValueSet("id-03", "BBB 01"));
    valueSetList.add(generateAllDataValueSet("id-04", "CCC 01"));
    valueSetList.add(generateAllDataValueSet("id-05", "DDD 01"));
    return valueSetList;
  }

  private static ValueSet generateAllDataValueSet(String id, String name) {
    ValueSet valueSet = new ValueSet();
    valueSet.setId(id);
    Meta meta = new Meta();
    meta.setVersionId(null);
    meta.setLastUpdated(new Date());
    meta.setSource("https://example.org/Sources/source01");
    meta.addProfile("https://example.org/fhir/StructureDefinition/profile01");
    meta.addSecurity(
        new Coding().setSystem("https://example.org/fhir/CodeSystem/security01").setCode("s01"));
    meta.addTag(new Coding().setSystem("https://example.org/CodeSystem/tags01").setCode("t01"));
    valueSet.setMeta(meta);
    valueSet.setImplicitRules("https://example.org/ImplicitRules/rule01");
    valueSet.setLanguage("de-DE");
    valueSet.setText(null);
    valueSet.setContained(null);
    valueSet
        .addExtension()
        .setUrl("https://example.org/Extensions/extension01")
        .setValue(new StringType("extensionvalue01"));
    valueSet
        .addModifierExtension()
        .setUrl("https://example.org/Extensions/extension02")
        .setValue(new StringType("extensionvalue02"));
    valueSet.setUrl(DEFAULT_VS_URL_PREFIX + id);
    valueSet.addIdentifier(
        new Identifier()
            .setUse(Identifier.IdentifierUse.OFFICIAL)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem("https://example.org/CodeSystem/identifierTypes")
                            .setCode("type01")))
            .setSystem("https://example.org/IdentifierSystem/identifier01")
            .setValue(name.toLowerCase()));
    valueSet.setVersion("2024.0.0");
    valueSet.setName(name.replaceAll(" ", "_"));
    valueSet.setTitle(name + " (Title)");
    valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    valueSet.setExperimental(false);
    valueSet.setDateElement(new DateTimeType("2024-01-01"));
    valueSet.setPublisher(name + " (Publisher)");
    valueSet.addContact().setName(name + " (Contact)");
    valueSet.setDescription(name + " (Description)");
    valueSet.setImmutable(false);
    valueSet.setPurpose(name + " (Purpose)");
    valueSet.setCopyright(name + " (Copyright)");

    // CONTINUE

    return valueSet;
  }

  // ==================================================================================================================
}
