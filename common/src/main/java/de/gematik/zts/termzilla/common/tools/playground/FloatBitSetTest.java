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
package de.gematik.zts.termzilla.common.tools.playground;

import de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators.PropertyFilter;
import java.util.BitSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeType;

@Slf4j
public class FloatBitSetTest {

  // TODO @Olaf rework to real test case
  public static void main(String[] args) {

    List<PropertyFilter> pFilter =
        List.of(
            new PropertyFilter("code", new CodeType("A00"), 0),
            new PropertyFilter("code", new CodeType("A01"), 1));

    float sum = 0;

    for (PropertyFilter filter : pFilter) {
      sum = sum + filter.getExactMatchBoost() + filter.getPartialMatchBoost();
    }

    printBitSet(floatToBitSet(sum));
    printBitSetAsFloat(floatToBitSet(sum));
  }

  public static BitSet floatToBitSet(float value) {
    int bits = Float.floatToIntBits(value);
    return BitSet.valueOf(new long[] {bits & 0xFFFFFFFFL});
  }

  public static float bitSetToFloat(BitSet bitSet) {
    long bits = bitSet.toLongArray()[0];
    return Float.intBitsToFloat((int) bits);
  }

  public static void printBitSet(BitSet bitSet) {
    log.info("BitSet: {}", bitSet);
  }

  public static void printBitSetAsFloat(BitSet bitSet) {
    log.info("BitSet as float: {}", bitSetToFloat(bitSet));
  }
}
