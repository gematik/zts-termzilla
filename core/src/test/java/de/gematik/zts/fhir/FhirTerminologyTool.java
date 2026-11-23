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
package de.gematik.zts.fhir;

import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.OFFSET_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.URL_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.VALUESET_PARAM;
import static de.gematik.zts.termzilla.api.parameters.VsExpandParameter.VALUESET_VERSION_PARAM;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.gematik.zts.termzilla.api.parameters.VsExpandParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Used in E2E Test (CombiningTerminologyServiceTest)
public class FhirTerminologyTool {

    private static final Logger log = LoggerFactory.getLogger(FhirTerminologyTool.class);
    private final IGenericClient client;

    public FhirTerminologyTool(String fhirBaseUrl) {
        this(createClient(fhirBaseUrl));
    }

    public FhirTerminologyTool(IGenericClient client) {
        this.client = Objects.requireNonNull(client);
    }

    private static IGenericClient createClient(String fhirBaseUrl) {
        FhirContext ctx = FhirContext.forR4Cached();
        ApacheRestfulClientFactory factory = (ApacheRestfulClientFactory) ctx.getRestfulClientFactory();
        factory.setSocketTimeout(120000);
        factory.setConnectTimeout(120000);
        factory.setConnectionRequestTimeout(120000);
        return ctx.newRestfulGenericClient(fhirBaseUrl);
    }

    public List<ValueSet.ValueSetExpansionContainsComponent> expandVs(VsExpandParameter expandParam) {

        Parameters parameters = new Parameters();

        if (expandParam.getValueSetParam() != null && !expandParam.getValueSetParam().isEmpty()) {

            ValueSet valueSet = expandParam.getValueSetParam().copy();
            valueSet.setExpansion(null);

            parameters.addParameter().setName(VALUESET_PARAM).setResource(valueSet);
        }

        if (expandParam.getUrlParam() != null && !expandParam.getUrlParam().isEmpty()) {
            parameters.addParameter().setName(URL_PARAM).setValue(expandParam.getUrlParam());
        }

        if (expandParam.getValueSetVersionParam() != null
                && !expandParam.getValueSetVersionParam().isEmpty()) {
            parameters
                    .addParameter()
                    .setName(VALUESET_VERSION_PARAM)
                    .setValue(expandParam.getValueSetVersionParam());
        }

        List<ValueSet.ValueSetExpansionContainsComponent> completeContainsList = new ArrayList<>();
        long total;

        do {
            long offset = completeContainsList.size();
            log.info("Expanding ValueSet with offset: {}", offset);

            // Execute the expansion operation
            ValueSet vs = executeExpandVs(parameters, offset);

            // Add the contains components to the complete list
            ValueSet.ValueSetExpansionComponent expansion = vs.getExpansion();
            int previousSize = completeContainsList.size();
            completeContainsList.addAll(expansion.getContains());
            // Check if there are more pages to fetch
            total =
                    expansion.hasTotal()
                            ? expansion.getTotalElement().getValue().longValue()
                            : completeContainsList.size();
            if (total > completeContainsList.size() && completeContainsList.size() == previousSize) {
                throw new IllegalStateException(
                        "ValueSet expansion returned no concepts before reaching its reported total of "
                                + total);
            }
        } while (total > completeContainsList.size());

        log.info("Expanded ValueSet contains {} concepts", completeContainsList.size());

        return completeContainsList;
    }

    private ValueSet executeExpandVs(Parameters parameters, long startFrom) {

        Parameters newParam = parameters.copy();
        newParam.addParameter(OFFSET_PARAM, new IntegerType(startFrom));

        Parameters returnParams;
        try {
            returnParams =
                    client
                            .operation()
                            .onType(ValueSet.class)
                            .named("$expand")
                            .withParameters(newParam)
                            .execute();
        } catch (Exception e) {
            log.error("Failed to expand ValueSet at offset {}", startFrom, e);
            throw new RuntimeException("Failed to expand ValueSet at offset " + startFrom, e);
        }

        if (returnParams == null
                || !returnParams.hasParameter()
                || !(returnParams.getParameterFirstRep().getResource() instanceof ValueSet valueSet)
                || !valueSet.hasExpansion()) {
            throw new IllegalStateException(
                    "FHIR $expand response did not contain a ValueSet with an expansion");
        }

        return valueSet;
    }
}
