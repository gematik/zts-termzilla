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
package de.gematik.zts.termzilla.api.parameters;

/**
 * Origin of a resource that is processed by a terminology operation.
 *
 * <p>The origin determines which content a terminology may use while processing the resource. For
 * example, a terminology provided in restricted mode may evaluate hierarchy-based filters only for
 * stored ValueSets.
 */
public enum ResourceOrigin {

  /** The resource is part of the service's content, e.g. loaded from a FHIR package. */
  STORED,

  /** The resource was provided within the request, e.g. as an inline ValueSet. */
  PROVIDED
}
