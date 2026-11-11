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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import java.util.BitSet;
import org.hl7.fhir.r4.model.Type;

// TODO: Prüfen, ob das ein guter Ort für diese Klasse ist
public record PropertyFilter(String code, Type value, int position) {

  public float getExactMatchBoost() {
    BitSet bitSet = new BitSet(32);
    bitSet.set(2 * position);
    return bitSetToFloat(bitSet);
  }

  public float getPartialMatchBoost() {
    BitSet bitSet = new BitSet(32);
    bitSet.set(2 * position + 1);
    return bitSetToFloat(bitSet);
  }

  private static float bitSetToFloat(BitSet bitSet) {
    long bits = bitSet.toLongArray()[0];
    return Float.intBitsToFloat((int) bits);
  }
}
