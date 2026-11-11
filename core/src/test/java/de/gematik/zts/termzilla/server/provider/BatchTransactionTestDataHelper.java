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
package de.gematik.zts.termzilla.server.provider;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;

/** Test data helper for batch transaction provider unit tests. */
final class BatchTransactionTestDataHelper {

  private BatchTransactionTestDataHelper() {
    // Utility class
  }

  static Bundle createBatchBundle(Bundle.BundleEntryComponent... entries) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.BATCH);
    for (Bundle.BundleEntryComponent entry : entries) {
      bundle.addEntry(entry);
    }
    return bundle;
  }

  static Bundle.BundleEntryComponent createGetEntry(String url) {
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setRequest(
        new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.GET).setUrl(url));
    return entry;
  }

  static Bundle.BundleEntryComponent createPostEntry(String url, Parameters parameters) {
    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
    entry.setRequest(
        new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl(url));
    entry.setResource(parameters);
    return entry;
  }

  static Parameters createCodeSystemValidateCodeParameters(
      String url, String code, String display) {
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("url").setValue(new UriType(url));
    parameters.addParameter().setName("code").setValue(new CodeType(code));
    parameters.addParameter().setName("display").setValue(new StringType(display));
    return parameters;
  }

  static Parameters createValueSetValidateCodeParameters(String url, String code, String system) {
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("url").setValue(new UriType(url));
    parameters.addParameter().setName("code").setValue(new CodeType(code));
    parameters.addParameter().setName("system").setValue(new UriType(system));
    return parameters;
  }
}
