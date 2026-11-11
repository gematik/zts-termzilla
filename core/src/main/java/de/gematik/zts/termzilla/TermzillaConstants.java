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
package de.gematik.zts.termzilla;

public class TermzillaConstants {

  private TermzillaConstants() {
    // private constructor to prevent instantiation
  }

  public static final String TERMINOLOGY_CACHE_PATH_NAME = "termzilla";
  public static final String ENV_TEMP_DIR = "java.io.tmpdir";
  public static final String MESSAGE_CODE_SYSTEM_INCLUDE_NOT_SUPPORTED =
      "The CodeSystem '%s' is not supported";
  public static final String MESSAGE_VALUE_SET_INCLUDE_NOT_SUPPORTED =
      "Das ValueSet referenced in the include ('%s') is not supported";
  public static final String MESSAGE_VALUE_SET_INCLUDE_HAS_NO_SYSTEM =
      "The ValueSet referenced in the include ('%s') has no system part set.";
  public static final String MESSAGE_VALUE_SET_INCLUDE_HAS_NO_VERSION =
      "The ValueSet referenced in the include ('%s') has no version part set. We won't support expansions for this "
          + "scenario because we can't guarantee for stable results over time due to restrictions of the standard.";
}
