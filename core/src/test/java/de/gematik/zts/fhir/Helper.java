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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ValueSet;

@Slf4j
public class Helper {

  public static boolean hasSameContent(
      List<ValueSet.ValueSetExpansionContainsComponent> listA,
      List<ValueSet.ValueSetExpansionContainsComponent> listB) {

    List<String> systemCodesA =
        listA.stream().map(e -> e.getSystem() + "#" + e.getCode()).sorted().toList();

    List<String> systemCodesB =
        listB.stream().map(e -> e.getSystem() + "#" + e.getCode()).sorted().toList();

    // Differenz bilden
    Set<String> diffA =
        systemCodesA.stream().filter(e -> !systemCodesB.contains(e)).collect(Collectors.toSet());
    log.info("Difference in A: {}", diffA);
    Set<String> diffB =
        systemCodesB.stream().filter(e -> !systemCodesA.contains(e)).collect(Collectors.toSet());
    log.info("Difference in B: {}", diffB);

    return diffA.isEmpty() && diffB.isEmpty();
  }
}
