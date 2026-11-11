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
public class LinguisticVariantVersion {

  // Constants for CSV column names
  public static final String NAME_LOINC_NUM = "LOINC_NUM";
  public static final String NAME_COMPONENT = "COMPONENT";
  public static final String NAME_PROPERTY = "PROPERTY";
  public static final String NAME_TIME_ASPCT = "TIME_ASPCT";
  public static final String NAME_SYSTEM = "SYSTEM";
  public static final String NAME_SCALE_TYP = "SCALE_TYP";
  public static final String NAME_METHOD_TYP = "METHOD_TYP";
  public static final String NAME_CLASS = "CLASS";
  public static final String NAME_SHORTNAME = "SHORTNAME";
  public static final String NAME_LONG_COMMON_NAME = "LONG_COMMON_NAME";
  public static final String NAME_RELATEDNAMES2 = "RELATEDNAMES2";
  public static final String NAME_LINGUISTIC_VARIANT_DISPLAY_NAME = "LinguisticVariantDisplayName";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_INACTIVE = "INACTIVE";

  private int validFromVersion;

  private String language;

  private String status = STATUS_ACTIVE;

  @CsvBindByName(column = NAME_LOINC_NUM)
  private String loincNumber;

  @CsvBindByName(column = NAME_COMPONENT)
  private String component;

  @CsvBindByName(column = NAME_PROPERTY)
  private String property;

  @CsvBindByName(column = NAME_TIME_ASPCT)
  private String timeAspect;

  @CsvBindByName(column = NAME_SYSTEM)
  private String system;

  @CsvBindByName(column = NAME_SCALE_TYP)
  private String scaleType;

  @CsvBindByName(column = NAME_METHOD_TYP)
  private String methodType;

  @CsvBindByName(column = NAME_CLASS)
  private String clazz;

  @CsvBindByName(column = NAME_LINGUISTIC_VARIANT_DISPLAY_NAME)
  private String linguisticVariantDisplayName;

  @CsvBindByName(column = NAME_LONG_COMMON_NAME)
  private String longCommonName;

  @CsvBindByName(column = NAME_RELATEDNAMES2)
  private String relatedNames2;

  @CsvBindByName(column = NAME_SHORTNAME)
  private String shortName;

  @Override
  public String toString() {
    return "DescriptionVersion{"
        + "validFromVersion="
        + validFromVersion
        + ", language='"
        + language
        + '\''
        + ", status='"
        + status
        + '\''
        + ", loincNumber='"
        + loincNumber
        + '\''
        + ", component='"
        + getFirstNChars(component, 10)
        + '\''
        + ", property='"
        + getFirstNChars(property, 10)
        + '\''
        + ", timeAspect='"
        + getFirstNChars(timeAspect, 10)
        + '\''
        + ", system='"
        + getFirstNChars(system, 10)
        + '\''
        + ", scaleType='"
        + getFirstNChars(scaleType, 10)
        + '\''
        + ", methodType='"
        + getFirstNChars(methodType, 10)
        + '\''
        + ", clazz='"
        + getFirstNChars(clazz, 10)
        + '\''
        + ", linguisticVariantDisplayName='"
        + getFirstNChars(linguisticVariantDisplayName, 20)
        + '\''
        + ", longCommonName='"
        + getFirstNChars(longCommonName, 10)
        + '\''
        + ", relatedNames2='"
        + getFirstNChars(relatedNames2, 10)
        + '\''
        + ", shortName='"
        + getFirstNChars(shortName, 10)
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
    if (!(obj instanceof LinguisticVariantVersion other)) {
      return false;
    }

    return Objects.equals(language, other.language)
        && Objects.equals(status, other.status)
        && Objects.equals(loincNumber, other.loincNumber)
        && Objects.equals(component, other.component)
        && Objects.equals(property, other.property)
        && Objects.equals(timeAspect, other.timeAspect)
        && Objects.equals(system, other.system)
        && Objects.equals(scaleType, other.scaleType)
        && Objects.equals(methodType, other.methodType)
        && Objects.equals(clazz, other.clazz)
        && Objects.equals(linguisticVariantDisplayName, other.linguisticVariantDisplayName)
        && Objects.equals(longCommonName, other.longCommonName)
        && Objects.equals(relatedNames2, other.relatedNames2)
        && Objects.equals(shortName, other.shortName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        language,
        status,
        loincNumber,
        component,
        property,
        timeAspect,
        system,
        scaleType,
        methodType,
        clazz,
        linguisticVariantDisplayName,
        longCommonName,
        relatedNames2,
        shortName);
  }
}
