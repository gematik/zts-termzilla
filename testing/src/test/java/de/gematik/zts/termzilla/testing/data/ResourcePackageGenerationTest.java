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
package de.gematik.zts.termzilla.testing.data;

import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS1_2025_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS1_2026_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_CS2_2026_deDE_deDE;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_A;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_B;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_C;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCS00_D;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCSS00_C;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createCodeSystem_LCSS00_D;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A0;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_A2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B0;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_B2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_C1;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_LVS00_D2;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS00_A;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS00_B;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS01;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS02;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS03;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS04;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS05;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS06;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS07;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS08;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS09;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS10;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS11;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS12;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS13;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS14;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS15;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS16;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS17;
import static de.gematik.zts.termzilla.testing.data.ExpandTestResourceGenerator.createValueSet_VS18;

import de.gematik.zts.termzilla.testutils.packages.generator.PackageGeneration;
import de.gematik.zts.termzilla.testutils.packages.generator.PackageGenerationException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.MetadataResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResourcePackageGenerationTest {

  @Disabled("Only used from time to time to generate new testdata")
  @Test
  @DisplayName("Generate a test package for expansion tests")
  @SuppressWarnings("java:S2699") // No assertions, we just want to generate the package
  void generateExpansionTestPackage() throws PackageGenerationException {

    PackageGeneration packageGeneration =
        PackageGeneration.builder()
            .packageName("bfarm.terminologien.expandtests")
            .packageVersion("1.0.0")
            .build();

    List<MetadataResource> resources = new ArrayList<>();
    resources.add(createCodeSystem_CS1_2025_deDE_deDE());
    resources.add(createCodeSystem_CS1_2026_deDE_deDE());
    resources.add(createCodeSystem_CS2_2026_deDE_deDE());
    resources.add(createValueSet_VS00_A());
    resources.add(createValueSet_VS00_B());
    resources.add(createValueSet_VS01());
    resources.add(createValueSet_VS02());
    resources.add(createValueSet_VS03());
    resources.add(createValueSet_VS04());
    resources.add(createValueSet_VS05());
    resources.add(createValueSet_VS06());
    resources.add(createValueSet_VS07());
    resources.add(createValueSet_VS08());
    resources.add(createValueSet_VS09());
    resources.add(createValueSet_VS10());
    resources.add(createValueSet_VS11());
    resources.add(createValueSet_VS12());
    resources.add(createValueSet_VS13());
    resources.add(createValueSet_VS14());
    resources.add(createValueSet_VS15());
    resources.add(createValueSet_VS16());
    resources.add(createValueSet_VS17());
    resources.add(createValueSet_VS18());

    // LCS Scenario Data
    resources.add(createCodeSystem_LCS00_A());
    resources.add(createCodeSystem_LCS00_B());
    resources.add(createCodeSystem_LCS00_C());
    resources.add(createCodeSystem_LCSS00_C());
    resources.add(createCodeSystem_LCS00_D());
    resources.add(createCodeSystem_LCSS00_D());

    resources.add(createValueSet_LVS00_A0());
    resources.add(createValueSet_LVS00_A1());
    resources.add(createValueSet_LVS00_A2());
    resources.add(createValueSet_LVS00_B0());
    resources.add(createValueSet_LVS00_B1());
    resources.add(createValueSet_LVS00_B2());
    resources.add(createValueSet_LVS00_C1());
    resources.add(createValueSet_LVS00_D2());

    packageGeneration.generatePackage(resources, Paths.get("/Users/oro/Projekte/TMP/gen"));
  }
}
