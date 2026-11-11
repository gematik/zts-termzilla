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
package de.gematik.zts.termzilla.terminologies.fhirterms.helper;

import de.gematik.zts.termzilla.terminologies.fhirterms.memory.ResourcePackageVersionComparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.MetadataResource;

@Getter
@Setter
public class Scope {

  private String id;

  // Sortierte Liste mit allen Ressourcen, die in den Scope gehören. Die Sortierung erfolgt nach
  // Packagename und Version.
  private ArrayList<MetadataResource> sortedResourceList = new ArrayList<>();

  // Set mit ids, die wir im jeweiligen Scope unterstützen.
  private Set<String> includedResourcesById = new HashSet<>();

  // Map, die über Url und Version den Ressourcen-Id abbildet.
  private Map<TRKey, String> resourceMap = new HashMap<>();

  public Scope(String id) {
    this.id = id;
  }

  /**
   * @param resourceToAdd
   */
  public void addTerminologyResource(MetadataResource resourceToAdd) {
    // Ressource in Liste einfügen
    sortedResourceList.add(resourceToAdd);
  }

  /** Sort and configure all resources for a given scope. */
  public void configureTerminologyResources() {
    // Ressource in Liste sortieren
    sortedResourceList.sort(new ResourcePackageVersionComparator());

    includedResourcesById.clear();
    resourceMap.clear();

    // Über die sortierte Liste iterieren und die default-Ressourcen in die Map einfügen. Die
    // Sortierreihenfolge wird dafür sorgen, dass die aktuellste Ressource die default-Ressource
    // wird.
    for (MetadataResource resource : sortedResourceList) {
      includedResourcesById.add(resource.getIdPart());

      // Url-Version-Key für die Ressource erstellen und in die Map einfügen
      TRKey key = new TRKey(resource);
      resourceMap.put(key, resource.getIdPart());

      TRKey defaultKey = new TRKey(resource.getUrl(), null);
      resourceMap.put(defaultKey, resource.getIdPart());
    }
  }

  /**
   * @param url
   * @return Set of resourceIds
   */
  public Set<String> getResourceIdSet(String url) {
    Set<String> resourceIdSet = new HashSet<>();

    Set<TRKey> keySet = resourceMap.keySet();
    for (TRKey key : keySet) {
      if (key.getUrl().contentEquals(url)) {
        resourceIdSet.add(resourceMap.get(key));
      }
    }

    return resourceIdSet;
  }
}
