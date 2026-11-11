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
package de.gematik.zts.termzilla.terminologies.fhirterms.memory;

import de.gematik.zts.termzilla.api.constants.ResourceUserData;
import java.util.Comparator;
import org.hl7.fhir.r4.model.Resource;
import org.semver4j.Semver;

public class ResourcePackageVersionComparator implements Comparator<Resource> {

  @Override
  public int compare(Resource s1, Resource s2) {
    // Zuerst nach Name sortieren
    String packageName1 = (String) s1.getUserData(ResourceUserData.USERDATA_PACKAGENAME);
    String packageName2 = (String) s2.getUserData(ResourceUserData.USERDATA_PACKAGENAME);

    int nameCompare = packageName1.compareTo(packageName2);
    if (nameCompare != 0) {
      return nameCompare;
    }

    // Dann nach Version sortieren
    String packageVersion1 = (String) s1.getUserData(ResourceUserData.USERDATA_PACKAGEVERSION);
    String packageVersion2 = (String) s2.getUserData(ResourceUserData.USERDATA_PACKAGEVERSION);

    Semver v1 = new Semver(packageVersion1);
    Semver v2 = new Semver(packageVersion2);

    return v1.compareTo(v2);
  }
}
