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
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoincPartLinkVersion {

  // Constants for CSV column names (LoincPartLink...)
  public static final String NAME_LOINC_NUMBER = "LoincNumber";
  public static final String NAME_LONG_COMMON_NAME = "LongCommonName";
  public static final String NAME_PART_NUMBER = "PartNumber";
  public static final String NAME_PART_NAME = "PartName";
  public static final String NAME_PART_CODE_SYSTEM = "PartCodeSystem";
  public static final String NAME_PART_TYPE_NAME = "PartTypeName";
  public static final String NAME_LINK_TYPE_NAME = "LinkTypeName";
  public static final String NAME_PROPERTY = "Property";

  // fields for the LoincPartLinkVersion class
  private int validFromVersion;

  @CsvBindByName(column = NAME_LOINC_NUMBER)
  private String loincNumber;

  @CsvBindByName(column = NAME_PART_NUMBER)
  private String partNumber;

  @CsvBindByName(column = NAME_PART_TYPE_NAME)
  private String partTypeName;

  @Override
  public String toString() {
    return "LoincPartLinkVersion{"
        + "validFromVersion="
        + validFromVersion
        + ", loincNumber='"
        + loincNumber
        + '\''
        + ", partNumber='"
        + partNumber
        + '\''
        + ", partTypeName='"
        + partTypeName
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LoincPartLinkVersion other)) {
      return false;
    }

    return Objects.equals(loincNumber, other.loincNumber)
        && Objects.equals(partNumber, other.partNumber)
        && Objects.equals(partTypeName, other.partTypeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(loincNumber, partNumber, partTypeName);
  }
}
