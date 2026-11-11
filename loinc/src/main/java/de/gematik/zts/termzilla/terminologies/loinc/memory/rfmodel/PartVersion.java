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
public class PartVersion {

  // Constants for CSV column names (Part.csv)
  public static final String NAME_PART_NUMBER = "PartNumber";
  public static final String NAME_PART_TYPE_NAME = "PartTypeName";
  public static final String NAME_PART_NAME = "PartName";
  public static final String NAME_PART_DISPLAY_NAME = "PartDisplayName";
  public static final String NAME_STATUS = "Status";

  // Fields for PartVersion
  private int validFromVersion;

  @CsvBindByName(column = NAME_PART_NUMBER)
  private String partNumber;

  @CsvBindByName(column = NAME_PART_TYPE_NAME)
  private String partTypeName;

  @CsvBindByName(column = NAME_PART_NAME)
  private String partName;

  @CsvBindByName(column = NAME_PART_DISPLAY_NAME)
  private String partDisplayName;

  @CsvBindByName(column = NAME_STATUS)
  private String status;

  @Override
  public String toString() {
    return "PartVersion{"
        + "validFromVersion="
        + validFromVersion
        + ", partNumber='"
        + partNumber
        + '\''
        + ", partTypeName='"
        + getFirstNChars(partTypeName, 10)
        + '\''
        + ", partName='"
        + getFirstNChars(partName, 50)
        + '\''
        + ", partDisplayName='"
        + getFirstNChars(partDisplayName, 50)
        + '\''
        + ", status='"
        + status
        + '\''
        + '}';
  }

  private String getFirstNChars(String s, int charCount) {
    if (s.length() > charCount) {
      return s.substring(0, charCount) + "...";
    } else {
      return s;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof PartVersion other)) {
      return false;
    }

    return Objects.equals(partNumber, other.partNumber)
        && Objects.equals(partTypeName, other.partTypeName)
        && Objects.equals(partName, other.partName)
        && Objects.equals(partDisplayName, other.partDisplayName)
        && Objects.equals(status, other.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partNumber, partTypeName, partName, partDisplayName, status);
  }
}
