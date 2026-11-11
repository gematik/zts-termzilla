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

import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_ID;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_LASTUPDATED;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_PROFILE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_PROVISION_MODE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_SCOPE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_SECURITY;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_SOURCE;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_TAG;
import static de.gematik.zts.termzilla.common.metadatasearch.lucene.fields.ResourceFieldNames.NAME_TYPE;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import de.gematik.zts.termzilla.common.config.provision.ProvisionMode;
import de.gematik.zts.termzilla.common.metadatasearch.lucene.fieldgenerators.helper.ResourceFieldGeneratorHelper;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
public abstract class ResourceFieldGenerator<R extends Resource> {

  /**
   * Erzeugt für ausgewählte Elemente der FHIR-Ressource die entsprechenden Felder im übergebenen
   * Lucene-Dokument, die später für die Suche benötigt werden. Der Index des Inhalts orientiert
   * sich somit weniger an der natürlichen Struktur und Benennung der Elemente, sondern der zu
   * unterstützenden Suchparameter.
   *
   * @param document Lucene-Dokument
   * @param resource FHIR-Ressource
   * @param fieldStore Art der Feldspeicherung (YES oder NO)
   */
  public void createFields(Document document, R resource, Field.Store fieldStore) {

    // Wir benötigen die Scope-Informationen, um eine Einschränkung der Ressourcensichtbarkeit
    // vorzunehmen
    Set<String> scopeSet = (Set<String>) resource.getUserData(ResourceUserData.USERDATA_SCOPE_SET);
    if (scopeSet != null && !scopeSet.isEmpty()) {
      scopeSet.stream()
          .forEach(scope -> document.add(new StringField(NAME_SCOPE, scope, Field.Store.YES)));
    }

    // get the provision mode from the resource user data
    ProvisionMode provisionMode =
        (ProvisionMode) resource.getUserData(ResourceUserData.USERDATA_PROVISION_MODE);
    if (provisionMode != null) {
      document.add(new StringField(NAME_PROVISION_MODE, provisionMode.name(), Field.Store.YES));
    } else {
      log.warn("Provision Mode is null for resource of type: {}", resource.fhirType());
    }

    // ---------------------------------------------------------------
    // ELEMENT: [Resource-Type] --> SEARCHPARAM: _type
    ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(
        document, NAME_TYPE, resource.fhirType(), false);

    // ---------------------------------------------------------------
    // ELEMENT: Resource.id [0..1] (id) --> SEARCHPARAM: _id (token)
    // _id is a bit special. Though it is a token type, it has to be treated differently
    // no support for missing modifier?!
    ResourceFieldGeneratorHelper.addIdTokenParamFieldToDocument(
        document, NAME_ID, resource.getId(), false);


    // ---------------------------------------------------------------
    // ELEMENT: [Resource-Content] --> SEARCHPARAM: _content (string)
    // TODO this means we need to add this for every string content we find in the resource?

    // ---------------------------------------------------------------
    // Process ELEMENTS that are associated with the Meta information of the resource
    if (resource.hasMeta()) {
      processMetaElements(document, resource, fieldStore);
    } else {
      // Meta not present - add missing information fields for all meta related search params
      ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
          document, NAME_LASTUPDATED, true);
      ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
          document, NAME_SOURCE, true);
      ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(document, NAME_TAG, true);
      ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
          document, NAME_PROFILE, true);
      ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
          document, NAME_SECURITY, true);
    }
  }

  private static <R extends Resource> void processMetaElements(Document document, R resource, Store fieldStore) {
    Meta meta = resource.getMeta();

    // ---------------------------------------------------------------
    // ELEMENT: Resource.meta.lastUpdated [0..1] (instant) --> SEARCHPARAM: _lastUpdated (date)
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
      document, NAME_LASTUPDATED, !meta.hasLastUpdated());
    if (meta.hasLastUpdated()) {
      ResourceFieldGeneratorHelper.addInstantDateParamFieldToDocument(
        document, NAME_LASTUPDATED, meta.getLastUpdatedElement());
    }

    // ---------------------------------------------------------------
    // ELEMENT: Resource.meta.source [0..1] (uri) --> SEARCHPARAM: _source (uri)
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
      document, NAME_SOURCE, !meta.hasSourceElement());
    if (meta.hasSourceElement()) {
      ResourceFieldGeneratorHelper.addUriParamFieldToDocument(
        document, NAME_SOURCE, meta.getSource(), fieldStore);
    }

    // ---------------------------------------------------------------
    // ELEMENT: Resource.meta.tag [0..*] (Coding) --> SEARCHPARAM: _tag (token)
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
      document, NAME_TAG, !meta.hasTag());
    for (Coding tag : meta.getTag()) {
      ResourceFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
        document, NAME_TAG, tag.getSystem(), tag.getCode(), tag.getDisplay(), fieldStore);
    }

    // ---------------------------------------------------------------
    // ELEMENT: Resource.meta.profile [0..*] (canonical) --> SEARCHPARAM: _profile (reference)
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
      document, NAME_PROFILE, !meta.hasProfile());

    // Wir können das Feld nicht sortable machen, da es mehrfach vorkommen kann
    for (CanonicalType profile : meta.getProfile()) {
      ResourceFieldGeneratorHelper.addCanonicalReferenceParamFieldToDocument(
        document, NAME_PROFILE, profile.getValue(), fieldStore, false);
    }
    // ---------------------------------------------------------------
    // ELEMENT: Resource.meta.security [0..*] (Coding) --> SEARCHPARAM: _security (token)
    ResourceFieldGeneratorHelper.addInformationMissingFieldToDocument(
      document, NAME_SECURITY, !meta.hasSecurity());
    for (Coding security : meta.getSecurity()) {
      ResourceFieldGeneratorHelper.addCodingTokenParamFieldToDocument(
        document,
          NAME_SECURITY,
          security.getSystem(),
          security.getCode(),
          security.getDisplay(),
        fieldStore);
    }
  }
}
