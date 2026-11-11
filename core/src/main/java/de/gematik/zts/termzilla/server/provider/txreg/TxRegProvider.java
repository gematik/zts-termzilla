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
package de.gematik.zts.termzilla.server.provider.txreg;

import de.gematik.zts.termzilla.server.provider.txreg.model.Candidate;
import de.gematik.zts.termzilla.server.provider.txreg.model.ResolveResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/termzilla-reg")
public class TxRegProvider {

  @GetMapping("/resolve")
  public ResponseEntity<ResolveResponse> resolveOperation(HttpServletRequest request) {

    String scheme = request.getHeader("X-Forwarded-Proto");
    if (scheme == null) {
      scheme = request.getScheme();
    }

    String host = request.getHeader("X-Forwarded-Host");
    if (host == null) {
      host = request.getHeader("Host");
    }

    String fullUrl = scheme + "://" + host + "/fhir";

    ResolveResponse response =
        ResolveResponse.builder()
            .formatVersion("1.0")
            .registryUrl("")
            .candidates(
                List.of(
                    Candidate.builder()
                        .serverName("TX Example Server")
                        .url(fullUrl)
                        .open(true)
                        .accessInfo(fullUrl)
                        .build()))
            .build();
    return ResponseEntity.ok(response);
  }
}
