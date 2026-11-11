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
package de.gematik.zts.termzilla.terminologies.snomed.rf2;

/**
 * Named column indices for SNOMED CT RF2 release file formats.
 *
 * <p>Each inner class corresponds to one RF2 entity type and defines the zero-based column
 * positions as they appear in the tab-separated full-release files. The first four columns ({@code
 * ID}, {@code EFFECTIVE_TIME}, {@code ACTIVE}, {@code MODULE_ID}) are shared across all RF2
 * entities; entity-specific columns follow from index 4 onwards.
 *
 * <p>Each inner class also provides a {@code MIN_COLUMNS} constant that specifies the minimum
 * number of tab-separated fields expected per line. This can be used for input validation in
 * loaders.
 *
 * @see <a href="https://confluence.ihtsdotools.org/display/DOCRELFMT">SNOMED CT Release File
 *     Specifications</a>
 */
public final class Rf2Columns {

  private Rf2Columns() {}

  // ──────────────────────────────────────────────────────────────────────────
  // Shared columns (common to all RF2 entities)
  // ──────────────────────────────────────────────────────────────────────────

  /** Column 0 – Component identifier. */
  public static final int ID = 0;

  /** Column 1 – Effective time ({@code yyyyMMdd}). */
  public static final int EFFECTIVE_TIME = 1;

  /** Column 2 – Active flag ({@code "1"} = active, {@code "0"} = inactive). */
  public static final int ACTIVE = 2;

  /** Column 3 – Module identifier. */
  public static final int MODULE_ID = 3;

  // ──────────────────────────────────────────────────────────────────────────
  // sct2_Concept_Full
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code sct2_Concept_Full} files. */
  public static final class Concept {

    private Concept() {}

    /** Column 4 – Definition status identifier. */
    public static final int DEFINITION_STATUS_ID = 4;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 5;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // sct2_Description_Full
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code sct2_Description_Full} files. */
  public static final class Description {

    private Description() {}

    /** Column 4 – Concept identifier that this description belongs to. */
    public static final int CONCEPT_ID = 4;

    /** Column 5 – Language code (e.g., {@code "en"}, {@code "de"}). */
    public static final int LANGUAGE_CODE = 5;

    /** Column 6 – Description type identifier (FSN, synonym). */
    public static final int TYPE_ID = 6;

    /** Column 7 – The description term text. */
    public static final int TERM = 7;

    /** Column 8 – Case significance identifier. */
    public static final int CASE_SIGNIFICANCE_ID = 8;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 9;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // sct2_Relationship_Full
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code sct2_Relationship_Full} files. */
  public static final class Relationship {

    private Relationship() {}

    /** Column 4 – Source concept identifier. */
    public static final int SOURCE_ID = 4;

    /** Column 5 – Destination concept identifier. */
    public static final int DESTINATION_ID = 5;

    /** Column 6 – Relationship group. */
    public static final int RELATIONSHIP_GROUP = 6;

    /** Column 7 – Relationship type identifier (e.g., IS-A). */
    public static final int TYPE_ID = 7;

    /** Column 8 – Characteristic type identifier. */
    public static final int CHARACTERISTIC_TYPE_ID = 8;

    /** Column 9 – Modifier identifier. */
    public static final int MODIFIER_ID = 9;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 10;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // der2_cRefset_LanguageFull
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code der2_cRefset_LanguageFull} files. */
  public static final class Language {

    private Language() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier (description id). */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Column 6 – Acceptability identifier (preferred / acceptable). */
    public static final int ACCEPTABILITY_ID = 6;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 7;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // der2_cissccRefset_MRCMAttributeDomainFull
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code der2_cissccRefset_MRCMAttributeDomainFull} files. */
  public static final class MrcmAttributeDomain {

    private MrcmAttributeDomain() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier (attribute concept). */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Column 6 – Domain identifier. */
    public static final int DOMAIN_ID = 6;

    /** Column 7 – Whether the attribute is grouped. */
    public static final int GROUPED = 7;

    /** Column 8 – Attribute cardinality (e.g., {@code "0..*"}). */
    public static final int ATTRIBUTE_CARDINALITY = 8;

    /** Column 9 – Attribute in-group cardinality (e.g., {@code "0..1"}). */
    public static final int ATTRIBUTE_IN_GROUP_CARDINALITY = 9;

    /** Column 10 – Rule strength identifier. */
    public static final int RULE_STRENGTH_ID = 10;

    /** Column 11 – Content type identifier. */
    public static final int CONTENT_TYPE_ID = 11;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 12;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // der2_ssccRefset_MRCMAttributeRangeFull
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code der2_ssccRefset_MRCMAttributeRangeFull} files. */
  public static final class MrcmAttributeRange {

    private MrcmAttributeRange() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier (attribute concept). */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Column 6 – Range constraint (ECL expression). */
    public static final int RANGE_CONSTRAINT = 6;

    /** Column 7 – Attribute rule. */
    public static final int ATTRIBUTE_RULE = 7;

    /** Column 8 – Rule strength identifier. */
    public static final int RULE_STRENGTH_ID = 8;

    /** Column 9 – Content type identifier. */
    public static final int CONTENT_TYPE_ID = 9;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 10;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // der2_sssssssRefset_MRCMDomainFull
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code der2_sssssssRefset_MRCMDomainFull} files. */
  public static final class MrcmDomain {

    private MrcmDomain() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier (domain concept). */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Column 6 – Domain constraint (ECL expression). */
    public static final int DOMAIN_CONSTRAINT = 6;

    /** Column 7 – Parent domain. */
    public static final int PARENT_DOMAIN = 7;

    /** Column 8 – Proximal primitive constraint. */
    public static final int PROXIMAL_PRIMITIVE_CONSTRAINT = 8;

    /** Column 9 – Proximal primitive refinement. */
    public static final int PROXIMAL_PRIMITIVE_REFINEMENT = 9;

    /** Column 10 – Domain template for pre-coordination. */
    public static final int DOMAIN_TEMPLATE_FOR_PRECOORDINATION = 10;

    /** Column 11 – Domain template for post-coordination. */
    public static final int DOMAIN_TEMPLATE_FOR_POSTCOORDINATION = 11;

    /** Column 12 – Guide URL. */
    public static final int GUIDE_URL = 12;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 13;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // der2_ssRefset_ModuleDependencyFull
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for {@code der2_ssRefset_ModuleDependencyFull} files. */
  public static final class ModuleDependency {

    private ModuleDependency() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier (target module). */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Column 6 – Source effective time ({@code yyyyMMdd}). */
    public static final int SOURCE_EFFECTIVE_TIME = 6;

    /** Column 7 – Target effective time ({@code yyyyMMdd}). */
    public static final int TARGET_EFFECTIVE_TIME = 7;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 8;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Generic Reference Set (der2_Refset_Full)
  // ──────────────────────────────────────────────────────────────────────────

  /** Column indices for generic simple reference set files. */
  public static final class RefSet {

    private RefSet() {}

    /** Column 4 – Reference set identifier. */
    public static final int REFSET_ID = 4;

    /** Column 5 – Referenced component identifier. */
    public static final int REFERENCED_COMPONENT_ID = 5;

    /** Minimum number of columns expected per line. */
    public static final int MIN_COLUMNS = 6;
  }
}
