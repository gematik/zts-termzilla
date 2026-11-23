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
package de.gematik.zts.termzilla.common.metadatasearch.helper.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.BitSet;
import java.util.List;
import org.hl7.fhir.r4.model.CodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PropertyFilter Tests")
class PropertyFilterTest {

  private static final List<PropertyFilter> DEFAULT_PROPERTY_FILTERS =
      List.of(
          new PropertyFilter("code", new CodeType("A00"), 0),
          new PropertyFilter("code", new CodeType("A01"), 1));

  @Test
  @DisplayName("Should encode exact and partial boosts at deterministic bit positions")
  void shouldEncodeExactAndPartialBoostsAtDeterministicBitPositions() {
    // GIVEN
    int position = 3;
    PropertyFilter propertyFilter = new PropertyFilter("code", new CodeType("A00"), position);

    // WHEN
    int exactBits = Float.floatToIntBits(propertyFilter.getExactMatchBoost());
    int partialBits = Float.floatToIntBits(propertyFilter.getPartialMatchBoost());

    // THEN
    assertThat(exactBits).isEqualTo(1 << (2 * position));
    assertThat(partialBits).isEqualTo(1 << (2 * position + 1));
  }

  @Test
  @DisplayName("Should preserve all property flags when boosts are summed")
  void shouldPreserveAllPropertyFlagsWhenBoostsAreSummed() {
    // GIVEN
    List<PropertyFilter> propertyFilters = DEFAULT_PROPERTY_FILTERS;

    // WHEN
    float sum = calculateBoostSum(propertyFilters);

    // THEN
    assertThat(Float.floatToIntBits(sum)).isEqualTo(0b1111);
  }

  @Test
  @DisplayName("Should convert summed boost float to BitSet and back without information loss")
  void shouldConvertSummedBoostFloatToBitSetAndBackWithoutInformationLoss() {
    // GIVEN
    float sum = calculateBoostSum(DEFAULT_PROPERTY_FILTERS);

    // WHEN
    BitSet bitSet = floatToBitSet(sum);
    float restored = bitSetToFloat(bitSet);

    // THEN
    assertThat(bitSet).isEqualTo(BitSet.valueOf(new long[] {0b1111L}));
    assertThat(Float.floatToIntBits(restored)).isEqualTo(Float.floatToIntBits(sum));
  }

  private static BitSet floatToBitSet(float value) {
    int bits = Float.floatToIntBits(value);
    return BitSet.valueOf(new long[] {bits & 0xFFFFFFFFL});
  }

  private static float bitSetToFloat(BitSet bitSet) {
    long bits = bitSet.toLongArray()[0];
    return Float.intBitsToFloat((int) bits);
  }

  private static float calculateBoostSum(List<PropertyFilter> propertyFilters) {
    float sum = 0;
    for (PropertyFilter propertyFilter : propertyFilters) {
      sum += propertyFilter.getExactMatchBoost() + propertyFilter.getPartialMatchBoost();
    }
    return sum;
  }
}
