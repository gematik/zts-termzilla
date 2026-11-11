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
package de.gematik.zts.termzilla.server;

// TODO can we get rid of this?
public class TerminologyServerConstants {

  private TerminologyServerConstants() {
    // private constructor to prevent instantiation
  }

  public static final boolean CONFIGURATION_DEFAULT_ACTIVE_ONLY = true;
  public static final boolean CONFIGURATION_DEFAULT_INCLUDE_DESIGNATIONS = false;
  public static final long CONFIGURATION_DEFAULT_OFFSET = 0;
  public static final long CONFIGURATION_DEFAULT_PAGESIZE = 100;
  public static final long CONFIGURATION_DEFAULT_PAGESIZE_MAX = 100;
  public static final long CONFIGURATION_DEFAULT_MIN_FILTER_SIZE = 3;
}
