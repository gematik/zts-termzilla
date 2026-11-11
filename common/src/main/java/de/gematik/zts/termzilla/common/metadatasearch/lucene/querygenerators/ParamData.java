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
package de.gematik.zts.termzilla.common.metadatasearch.lucene.querygenerators;

import ca.uhn.fhir.rest.param.BaseAndListParam;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParamData {

  // number
  // TODO

  // date
  public static final String INSTANT_DATE_PARAM = "instant_date_param";
  public static final String PERIOD_DATE_PARAM = "period_date_param";

  // string
  public static final String STRING_PARAM = "string_param";

  // token
  public static final String ID_TOKEN_PARAM = "id_token_param";
  public static final String IDENTIFIER_TOKEN_PARAM = "identifier_token_param";
  public static final String BOOLEAN_TOKEN_PARAM = "boolean_token_param";
  public static final String CODE_TOKEN_PARAM = "code_token_param";
  public static final String CODING_TOKEN_PARAM = "coding_token_param";
  public static final String CODABLECONCEPT_TOKEN_PARAM = "codableconcept_token_param";

  // reference
  public static final String REFERENCE_PARAM = "reference_param";
  public static final String CANONICAL_REFERENCE_PARAM = "canonical_reference_param";

  // composite
  // TODO

  // quantity
  // TODO

  // uri
  public static final String URI_PARAM = "uri_param";

  // special

  private String paramName;
  private String paramQueryType;
  private BaseAndListParam<?> param;
}
