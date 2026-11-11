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
package de.gematik.zts.termzilla.terminologies.fhirterms.fhirpackage.model.packageindex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the {@code .index.json} inside a NPM package. The single file entries in the
 * index.json are represented by the class {@link PackageIndexFile}. The following example shows the
 * basic structure of an index.json:
 *
 * <pre>
 *  {
 *    "index-version": 1,             // a fixed number that identifies the version of this file;
 *                                       tools should rebuild the .index.json file if they encounter
 *                                       an unrecognized number
 *    "files": [                      // an array, with one entry per file in the same folder as the .index.json
 *      {
 *        "filename": "[name].json",  // the name of the file
 *        "resourceType": "[Type]",   // the type of the resource
 *        "id": "[id]",               // the id assigned to the resources. Note: resources SHOULD have an id, but
 *                                        in some workflows, none is assigned in the package
 *        "url": "url",               // the canonical url, if the resource has one (e.g. a property "url" which
 *                                        is a primitive)
 *        "version": "[version]",     // the business version, if the resource has one (e.g. a property
 *                                        "version" which is a primitive)
 *        "kind": "[kind]",           // the value of a the "kind" property in the resource, if it has
 *                                        one and it's a primitive
 *        "type": "[type]"            // the value of a the "type" property in the resource, if it has one
 *                                        and it's a primitive
 *      }
 *    ]
 *  }
 * </pre>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PackageIndex {

  @JsonProperty("index-version")
  long indexVersion;

  @JsonProperty("files")
  List<PackageIndexFile> files;

  public PackageIndex() {
    indexVersion = System.currentTimeMillis();
    files = new ArrayList<>();
  }

  @Override
  public String toString() {
    return String.valueOf(indexVersion);
  }

  public PackageIndex addFileToIndex(PackageIndexFile additionalFile) {
    files.add(additionalFile);
    return this;
  }
}
