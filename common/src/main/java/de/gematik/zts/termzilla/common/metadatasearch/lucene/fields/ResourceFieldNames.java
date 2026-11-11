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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.fields;

import org.hl7.fhir.instance.model.api.IAnyResource;

public class ResourceFieldNames extends BaseFieldNames {

  public static final String NAME_ID = IAnyResource.SP_RES_ID;
  public static final String NAME_LASTUPDATED = IAnyResource.SP_RES_LAST_UPDATED;
  public static final String NAME_TAG = IAnyResource.SP_RES_TAG;
  public static final String NAME_PROFILE = IAnyResource.SP_RES_PROFILE;
  public static final String NAME_SECURITY = IAnyResource.SP_RES_SECURITY;
  public static final String NAME_SOURCE = "_source";
  public static final String NAME_CONTENT = "_content";
}
