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
public class ComponentHierarchyBySystem {

  // Constants for CSV column names (ComponentHierachyBySystem.csv)
  public static final String NAME_PATH_TO_ROOT = "PATH_TO_ROOT";
  public static final String NAME_SEQUENCE = "SEQUENCE";
  public static final String NAME_IMMEDIATE_PARENT = "IMMEDIATE_PARENT";
  public static final String NAME_CODE = "CODE";
  public static final String NAME_CODE_TEXT = "CODE_TEXT";

  // Fields for ComponentHierachyBySystem
  @CsvBindByName(column = NAME_PATH_TO_ROOT)
  private String pathToRoot;

  @CsvBindByName(column = NAME_SEQUENCE)
  private String sequence;

  @CsvBindByName(column = NAME_IMMEDIATE_PARENT)
  private String immediateParent;

  @CsvBindByName(column = NAME_CODE)
  private String code;

  @CsvBindByName(column = NAME_CODE_TEXT)
  private String codeText;
}
