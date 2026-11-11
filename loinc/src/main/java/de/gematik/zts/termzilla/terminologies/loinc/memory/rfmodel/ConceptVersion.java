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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ConceptVersion {

  // Constants for CSV column names
  public static final String NAME_LOINC_NUM = "LOINC_NUM";
  public static final String NAME_COMPONENT = "COMPONENT";
  public static final String NAME_PROPERTY = "PROPERTY";
  public static final String NAME_TIME_ASPCT = "TIME_ASPCT";
  public static final String NAME_SYSTEM = "SYSTEM";
  public static final String NAME_SCALE_TYP = "SCALE_TYP";
  public static final String NAME_METHOD_TYP = "METHOD_TYP";
  public static final String NAME_CLASS = "CLASS";
  public static final String NAME_VERSION_LAST_CHANGED = "VersionLastChanged";
  public static final String NAME_CHNG_TYPE = "CHNG_TYPE";
  public static final String NAME_DEFINITION_DESCRIPTION = "DefinitionDescription";
  public static final String NAME_STATUS = "STATUS";
  public static final String NAME_CONSUMER_NAME = "CONSUMER_NAME";
  public static final String NAME_CLASSTYPE = "CLASSTYPE";
  public static final String NAME_FORMULA = "FORMULA";
  public static final String NAME_EXMPL_ANSWERS = "EXMPL_ANSWERS";
  public static final String NAME_SURVEY_QUEST_TEXT = "SURVEY_QUEST_TEXT";
  public static final String NAME_SURVEY_QUEST_SRC = "SURVEY_QUEST_SRC";
  public static final String NAME_UNITSREQUIRED = "UNITSREQUIRED";
  public static final String NAME_RELATEDNAMES2 = "RELATEDNAMES2";
  public static final String NAME_SHORTNAME = "SHORTNAME";
  public static final String NAME_ORDER_OBS = "ORDER_OBS";
  public static final String NAME_HL7_FIELD_SUBFIELD_ID = "HL7_FIELD_SUBFIELD_ID";
  public static final String NAME_EXTERNAL_COPYRIGHT_NOTICE = "EXTERNAL_COPYRIGHT_NOTICE";
  public static final String NAME_EXAMPLE_UNITS = "EXAMPLE_UNITS";
  public static final String NAME_LONG_COMMON_NAME = "LONG_COMMON_NAME";
  public static final String NAME_EXAMPLE_UCUM_UNITS = "EXAMPLE_UCUM_UNITS";
  public static final String NAME_STATUS_REASON = "STATUS_REASON";
  public static final String NAME_STATUS_TEXT = "STATUS_TEXT";
  public static final String NAME_CHANGE_REASON_PUBLIC = "CHANGE_REASON_PUBLIC";
  public static final String NAME_COMMON_TEST_RANK = "COMMON_TEST_RANK";
  public static final String NAME_COMMON_ORDER_RANK = "COMMON_ORDER_RANK";
  public static final String NAME_HL7_ATTACHMENT_STRUCTURE = "HL7_ATTACHMENT_STRUCTURE";
  public static final String NAME_EXTERNAL_COPYRIGHT_LINK = "EXTERNAL_COPYRIGHT_LINK";
  public static final String NAME_PANEL_TYPE = "PANEL_TYPE";
  public static final String NAME_ASK_AT_ORDER_ENTRY = "AskAtOrderEntry";
  public static final String NAME_ASSOCIATED_OBSERVATIONS = "AssociatedObservations";
  public static final String NAME_VERSION_FIRST_RELEASED = "VersionFirstReleased";
  public static final String NAME_VALID_HL7_ATTACHMENT_REQUEST = "ValidHL7AttachmentRequest";
  public static final String NAME_DISPLAY_NAME = "DisplayName";

  @EqualsAndHashCode.Exclude private int validFromVersion;

  // #00 code
  @CsvBindByName(column = NAME_LOINC_NUM)
  private String loincNum;

  // #01 Property: http://loinc.org/property/COMPONENT
  @CsvBindByName(column = NAME_COMPONENT)
  private String component;

  // #02 Property: http://loinc.org/property/PROPERTY
  @CsvBindByName(column = NAME_PROPERTY)
  private String property;

  // #03 Property: http://loinc.org/property/TIME_ASPCT
  @CsvBindByName(column = NAME_TIME_ASPCT)
  private String timeAspect;

  // #04 Property: http://loinc.org/property/SYSTEM
  @CsvBindByName(column = NAME_SYSTEM)
  private String system;

  // #05 Property: http://loinc.org/property/SCALE_TYP
  @CsvBindByName(column = NAME_SCALE_TYP)
  private String scaleType;

  // #06 Property: http://loinc.org/property/METHOD_TYP
  @CsvBindByName(column = NAME_METHOD_TYP)
  private String methodType;

  // #07 Property: http://loinc.org/property/CLASS
  @CsvBindByName(column = NAME_CLASS)
  private String clazz;

  // #08 Property: http://loinc.org/property/VersionLastChanged
  @CsvBindByName(column = NAME_VERSION_LAST_CHANGED)
  private String versionLastChanged;

  // #09 Property: http://loinc.org/property/CHNG_TYPE
  @CsvBindByName(column = NAME_CHNG_TYPE)
  private String changeType;

  // #10 Property: http://loinc.org/property/DefinitionDescription
  @CsvBindByName(column = NAME_DEFINITION_DESCRIPTION)
  private String definitionDescription;

  // #11 Property: http://loinc.org/property/STATUS
  @CsvBindByName(column = NAME_STATUS)
  private String status;

  // #12 Property: http://loinc.org/property/CONSUMER_NAME
  @CsvBindByName(column = NAME_CONSUMER_NAME)
  private String consumerName;

  // #13 Property: http://loinc.org/property/CLASSTYPE
  @CsvBindByName(column = NAME_CLASSTYPE)
  private String classtype;

  // #14 Property: http://loinc.org/property/FORMULA
  @CsvBindByName(column = NAME_FORMULA)
  private String formula;

  // #15 Property: http://loinc.org/property/EXMPL_ANSWERS
  @CsvBindByName(column = NAME_EXMPL_ANSWERS)
  private String exampleAnswers;

  // #16 Property: http://loinc.org/property/SURVEY_QUEST_TEXT
  @CsvBindByName(column = NAME_SURVEY_QUEST_TEXT)
  private String surveyQuestText;

  // #17 Property: http://loinc.org/property/SURVEY_QUEST_SRC
  @CsvBindByName(column = NAME_SURVEY_QUEST_SRC)
  private String surveyQuestSrc;

  // #18 Property: http://loinc.org/property/UNITSREQUIRED
  @CsvBindByName(column = NAME_UNITSREQUIRED)
  private String unitsRequired;

  // #19 DEPRECATED!!!!
  //    private String submittedUnits
  // #20 #19 Property: http://loinc.org/property/RELATEDNAMES2
  @CsvBindByName(column = NAME_RELATEDNAMES2)
  private String relatedNames2;

  // #21 Property: http://loinc.org/property/SHORTNAME
  @CsvBindByName(column = NAME_SHORTNAME)
  private String shortName;

  // #22 Property: http://loinc.org/property/ORDER_OBS
  @CsvBindByName(column = NAME_ORDER_OBS)
  private String orderObs;

  // #23 DEPRECATED!!!!
  //    private String cdiscCommonTests
  // #24 Property: http://loinc.org/property/HL7_FIELD_SUBFIELD_ID
  @CsvBindByName(column = NAME_HL7_FIELD_SUBFIELD_ID)
  private String hl7FieldSubfieldId;

  // #25 Property: http://loinc.org/property/EXTERNAL_COPYRIGHT_NOTICE
  @CsvBindByName(column = NAME_EXTERNAL_COPYRIGHT_NOTICE)
  private String externalCopyRightNotice;

  // #26 Property: http://loinc.org/property/EXAMPLE_UNITS
  @CsvBindByName(column = NAME_EXAMPLE_UNITS)
  private String exampleUnits;

  // #27 Property: http://loinc.org/property/LONG_COMMON_NAME
  @CsvBindByName(column = NAME_LONG_COMMON_NAME)
  private String longCommonName;

  // #28 DEPRECATED!!!!
  //    private String unitsAndRange
  // #29 Property: http://loinc.org/property/EXAMPLE_UCUM_UNITS
  @CsvBindByName(column = NAME_EXAMPLE_UCUM_UNITS)
  private String exampleUcumUnits;

  // #30 DEPRECATED!!!!
  //    private String exampleSiUcumUnits
  // #31 Property: http://loinc.org/property/STATUS_REASON
  @CsvBindByName(column = NAME_STATUS_REASON)
  private String statusReason;

  // #32 Property: http://loinc.org/property/STATUS_TEXT
  @CsvBindByName(column = NAME_STATUS_TEXT)
  private String statusText;

  // #33 Property: http://loinc.org/property/CHANGE_REASON_PUBLIC
  @CsvBindByName(column = NAME_CHANGE_REASON_PUBLIC)
  private String changeReasonPublic;

  // #34 Property: http://loinc.org/property/COMMON_TEST_RANK
  @CsvBindByName(column = NAME_COMMON_TEST_RANK)
  private String commonTestRank;

  // #35 Property: http://loinc.org/property/COMMON_ORDER_RANK
  @CsvBindByName(column = NAME_COMMON_ORDER_RANK)
  private String commonOrderRank;

  // #36 DEPRECATED!!!!
  //  private String commonSiTestRank
  // #37 Property: http://loinc.org/property/HL7_ATTACHMENT_STRUCTURE
  @CsvBindByName(column = NAME_HL7_ATTACHMENT_STRUCTURE)
  private String hl7AttachmentStructure;

  // #38 Property: http://loinc.org/property/EXTERNAL_COPYRIGHT_LINK
  @CsvBindByName(column = NAME_EXTERNAL_COPYRIGHT_LINK)
  private String externalCopyRightLink;

  // #39 Property: http://loinc.org/property/PanelType
  @CsvBindByName(column = NAME_PANEL_TYPE)
  private String panelType;

  // #40 Property: http://loinc.org/property/AskAtOrderEntry
  @CsvBindByName(column = NAME_ASK_AT_ORDER_ENTRY)
  private String askAtOrderEntry;

  // #41 Property: http://loinc.org/property/AssociatedObservations
  @CsvBindByName(column = NAME_ASSOCIATED_OBSERVATIONS)
  private String associatedObservations;

  // #42 Property: http://loinc.org/property/VersionFirstReleased
  @CsvBindByName(column = NAME_VERSION_FIRST_RELEASED)
  private String versionFirstReleased;

  // #43 Property: http://loinc.org/property/ValidHL7AttachmentRequest
  @CsvBindByName(column = NAME_VALID_HL7_ATTACHMENT_REQUEST)
  private String validHL7AttachmentRequest;

  // #44 Property: http://loinc.org/property/DisplayName
  @CsvBindByName(column = NAME_DISPLAY_NAME)
  private String displayName;

  @Override
  public String toString() {
    return "ConceptVersion{"
        + "validFromVersion="
        + validFromVersion
        + ", loincNum='"
        + loincNum
        + '\''
        + ", component='"
        + getFirstNChars(component, 20)
        + '\''
        + ", property='"
        + property
        + '\''
        + ", timeAspect='"
        + timeAspect
        + '\''
        + ", system='"
        + system
        + '\''
        + ", scaleType='"
        + scaleType
        + '\''
        + ", methodType='"
        + methodType
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + ", versionLastChanged='"
        + versionLastChanged
        + '\''
        + ", changeType='"
        + changeType
        + '\''
        + ", definitionDescription='"
        + getFirstNChars(definitionDescription, 10)
        + '\''
        + ", status='"
        + status
        + '\''
        + ", consumerName='"
        + getFirstNChars(consumerName, 10)
        + '\''
        + ", classtype='"
        + classtype
        + '\''
        + ", formula='"
        + formula
        + '\''
        + ", exampleAnswers='"
        + exampleAnswers
        + '\''
        + ", surveyQuestText='"
        + surveyQuestText
        + '\''
        + ", surveyQuestSrc='"
        + surveyQuestSrc
        + '\''
        + ", unitsRequired='"
        + unitsRequired
        + '\''
        + ", relatedNames2='"
        + getFirstNChars(relatedNames2, 10)
        + '\''
        + ", shortName='"
        + getFirstNChars(shortName, 10)
        + '\''
        + ", orderObs='"
        + orderObs
        + '\''
        + ", hl7FieldSubfieldId='"
        + hl7FieldSubfieldId
        + '\''
        + ", externalCopyRightNotice='"
        + getFirstNChars(externalCopyRightNotice, 10)
        + '\''
        + ", exampleUnits='"
        + exampleUnits
        + '\''
        + ", longCommonName='"
        + getFirstNChars(longCommonName, 10)
        + '\''
        + ", exampleUcumUnits='"
        + exampleUcumUnits
        + '\''
        + ", statusReason='"
        + getFirstNChars(statusReason, 10)
        + '\''
        + ", statusText='"
        + getFirstNChars(statusText, 10)
        + '\''
        + ", changeReasonPublic='"
        + getFirstNChars(changeReasonPublic, 10)
        + '\''
        + ", commonTestRank='"
        + commonTestRank
        + '\''
        + ", commonOrderRank='"
        + commonOrderRank
        + '\''
        + ", hl7AttachmentStructure='"
        + hl7AttachmentStructure
        + '\''
        + ", externalCopyRightLink='"
        + getFirstNChars(externalCopyRightLink, 10)
        + '\''
        + ", panelType='"
        + panelType
        + '\''
        + ", askAtOrderEntry='"
        + askAtOrderEntry
        + '\''
        + ", associatedObservations='"
        + associatedObservations
        + '\''
        + ", versionFirstReleased='"
        + versionFirstReleased
        + '\''
        + ", validHL7AttachmentRequest='"
        + validHL7AttachmentRequest
        + '\''
        + ", displayName='"
        + getFirstNChars(displayName, 10)
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
}
