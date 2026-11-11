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
package de.gematik.zts.termzilla.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hl7.fhir.r4.model.ValueSet;

/** Utility methods for set-like operations on {@link Map} and {@link MultiValuedMap} instances. */
public class MapUtils {

  /** Prevents instantiation of this utility class. */
  private MapUtils() {}

  /**
   * Computes the deep intersection of two maps containing FHIR concept references.
   *
   * @param leftMap first source map
   * @param rightMap second source map
   * @return a new map containing only keys present in both maps and values equal by {@code
   *     ValueSet.ConceptReferenceComponent.equalsDeep(...)}
   */
  public static ArrayListValuedHashMap<Integer, ValueSet.ConceptReferenceComponent>
      findIntersection(
          ArrayListValuedHashMap<Integer, ValueSet.ConceptReferenceComponent> leftMap,
          ArrayListValuedHashMap<Integer, ValueSet.ConceptReferenceComponent> rightMap) {

    ArrayListValuedHashMap<Integer, ValueSet.ConceptReferenceComponent> intersectionMap =
        new ArrayListValuedHashMap<>();

    for (Integer key : leftMap.keySet()) {
      if (rightMap.containsKey(key)) {
        Collection<ValueSet.ConceptReferenceComponent> values1 = leftMap.get(key);
        Collection<ValueSet.ConceptReferenceComponent> values2 = rightMap.get(key);

        // Find the intersection of the values
        Collection<ValueSet.ConceptReferenceComponent> intersectionValues = new ArrayList<>();
        for (var value1 : values1) {
          for (var value2 : values2) {
            if (value1.equalsDeep(value2)) {
              intersectionValues.add(value1);
            }
          }
        }

        // Add the intersection values to the intersection map
        intersectionMap.putAll(key, intersectionValues);
      }
    }

    return intersectionMap;
  }

  /**
   * Computes the intersection of two {@link MultiValuedMap} instances.
   *
   * @param leftMap first source map
   * @param rightMap second source map
   * @param <K> key type
   * @param <V> value type
   * @return a new map containing only values that are present for matching keys in both maps
   */
  public static <K, V> MultiValuedMap<K, V> findIntersection(
      MultiValuedMap<K, V> leftMap, MultiValuedMap<K, V> rightMap) {

    MultiValuedMap<K, V> intersectionMap = new ArrayListValuedHashMap<>();

    for (K key : leftMap.keySet()) {
      if (rightMap.containsKey(key)) {
        Collection<V> values1 = leftMap.get(key);
        Collection<V> values2 = rightMap.get(key);

        // Find the intersection of the values
        Collection<V> intersectionValues = new ArrayList<>(values1);

        // we cannot use retainAll here (ValueSet.ConceptReferenceComponent does not override
        // equals)
        intersectionValues.retainAll(values2);

        // Add the intersection values to the intersection map
        intersectionMap.putAll(key, intersectionValues);
      }
    }

    return intersectionMap;
  }

  /**
   * Replaces values in {@code leftMap} with their intersection against {@code rightMap} for keys
   * that exist in both maps.
   *
   * @param leftMap map that is modified in place
   * @param rightMap map used as intersection source
   * @param <K> key type
   * @param <V> value type
   */
  public static <K, V> void intersectElements(
      Map<K, Collection<V>> leftMap, Map<K, Collection<V>> rightMap) {

    for (Map.Entry<K, Collection<V>> entry : leftMap.entrySet()) {
      K key = entry.getKey();
      if (rightMap.containsKey(key)) {
        Collection<V> values1 = entry.getValue();
        Collection<V> values2 = rightMap.get(key);

        // Find the intersection of the values
        Collection<V> intersectionValues = new ArrayList<>(values1);
        intersectionValues.retainAll(values2);

        // Replace the values in leftMap for the key
        entry.setValue(intersectionValues);
      }
    }
  }

  /**
   * Removes all keys from {@code leftMap} that are present in {@code rightMap}.
   *
   * @param leftMap map that is modified in place
   * @param rightMap map providing keys to remove
   * @param <K> key type
   * @param <V> value type
   */
  public static <K, V> void removeElements(
      MultiValuedMap<K, V> leftMap, MultiValuedMap<K, V> rightMap) {
    for (Map.Entry<K, V> entry : rightMap.entries()) {
      K key = entry.getKey();
      if (leftMap.containsKey(key)) {
        leftMap.remove(key);
      }
    }
  }
}
