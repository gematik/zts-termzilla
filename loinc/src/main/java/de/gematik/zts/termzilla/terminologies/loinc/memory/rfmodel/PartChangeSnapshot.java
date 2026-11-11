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
package de.gematik.zts.termzilla.terminologies.loinc.memory.rfmodel;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartChangeSnapshot {

  // Constants for CSV column names (Part.csv)
  public static final String NAME_VERSION_EFFECTIVE = "VersionEffective";
  public static final String NAME_PART_NUMBER = "PartNumber";
  public static final String NAME_PROPERTY = "PROPERTY";
  public static final String NAME_VALUE_PRIOR = "ValuePrior";
  public static final String NAME_VALUE_CURRENT = "ValueCurrent";
  public static final String NAME_CHANGE_REASON = "ChangeReason";

  // Fields for PartVersion
  @CsvBindByName(column = NAME_VERSION_EFFECTIVE)
  private String versionEffective;

  @CsvBindByName(column = NAME_PART_NUMBER)
  private String partNumber;

  @CsvBindByName(column = NAME_PROPERTY)
  private String property;

  @CsvBindByName(column = NAME_VALUE_PRIOR)
  private String valuePrior;

  @CsvBindByName(column = NAME_VALUE_CURRENT)
  private String valueCurrent;

  @CsvBindByName(column = NAME_CHANGE_REASON)
  private String changeReason;
}
