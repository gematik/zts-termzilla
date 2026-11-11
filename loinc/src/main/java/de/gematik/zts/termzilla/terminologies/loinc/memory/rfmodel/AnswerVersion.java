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
public class AnswerVersion {

  // Constants for CSV column names (AnswerList.csv)
  public static final String NAME_ANSWER_LIST_ID = "AnswerListId";
  public static final String NAME_ANSWER_LIST_NAME = "AnswerListName";
  public static final String NAME_ANSWER_LIST_OID = "AnswerListOID";
  public static final String NAME_EXT_DEFINED_YN = "ExtDefinedYN";
  public static final String NAME_EXT_DEFINED_ANSWER_LIST_CODE_SYSTEM =
      "ExtDefinedAnswerListCodeSystem";
  public static final String NAME_EXT_DEFINED_ANSWER_LIST_LINK = "ExtDefinedAnswerListLink";
  public static final String NAME_ANSWER_STRING_ID = "AnswerStringId";
  public static final String NAME_LOCAL_ANSWER_CODE = "LocalAnswerCode";
  public static final String NAME_LOCAL_ANSWER_CODE_SYSTEM = "LocalAnswerCodeSystem";
  public static final String NAME_SEQUENCE_NUMBER = "SequenceNumber";
  public static final String NAME_DISPLAY_TEXT = "DisplayText";
  public static final String NAME_EXT_CODE_ID = "ExtCodeId";
  public static final String NAME_EXT_CODE_DISPLAY_NAME = "ExtCodeDisplayName";
  public static final String NAME_EXT_CODE_SYSTEM = "ExtCodeSystem";
  public static final String NAME_EXT_CODE_SYSTEM_VERSION = "ExtCodeSystemVersion";
  public static final String NAME_EXT_CODESYSTEM_COPYRIGHT_NOTICE = "ExtCodeSystemCopyrightNotice";
  public static final String NAME_SUBSEQUENT_TEXT_PROMPT = "SubsequentTextPrompt";
  public static final String NAME_DESCRIPTION = "Description";
  public static final String NAME_SCORE = "Score";

  // Fields for AnswerVersion
  private int validFromVersion;
  private String status;

  @CsvBindByName(column = NAME_ANSWER_STRING_ID)
  private String answerStringId;

  @CsvBindByName(column = NAME_DISPLAY_TEXT)
  private String displayText;

  @Override
  public String toString() {
    return "AnswerVersion{"
        + "validFromVersion="
        + validFromVersion
        + ", status="
        + status
        + ", answerStringId='"
        + answerStringId
        + '\''
        + ", displayText='"
        + displayText
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AnswerVersion other)) {
      return false;
    }
    return Objects.equals(answerStringId, other.answerStringId)
        && Objects.equals(displayText, other.displayText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(answerStringId, displayText);
  }
}
