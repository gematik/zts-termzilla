# Expansion

## Quellen

- https://hl7.org/fhir/R4/valueset-operation-expand.html

## Zielsetzung

parameter in Expansion
A parameter that controlled the expansion process. These parameters may be used by users of expanded value sets to check
whether the expansion is suitable for a particular purpose, or to pick the correct expansion.
The server decides which parameters to include here, but at a minimum, the list SHOULD include all of the parameters
that affect the $expand operation. If the expansion will be persisted all of these parameters SHALL be included. If the
codeSystem on the server has a specified version then this version SHALL be provided as a parameter in the expansion (
note that not all code systems have a version).


Wiederholbare Expansions, d.h. gleiche Parameter führen zu gleichen Expansions.
--> Wir geben dem Nutzer alle Informationen mit, die zu dieser Expansion geführt haben.

Problem: Wir haben ein paar Parameter, die sich ganz gemein widersprechen könnten:

- system-version, check-system-version und force-system-version

Für diese muss man vermutlich ein paar Parameter-Checks mit einbauen

## Prämissen

- Wir behandeln noch nicht alle Extremfälle/Sonderfälle, da wir sonst noch ewig sitzen
- Wir konzentrieren uns auf die Anwendungsfälle, die wir für den ZTS haben
- Bereits implementierte Funktionalität für bestimmte Parameter wird durch zeitiges prüfen von Parametern und failen
  nicht erlaubt. Das sollte möglichst konfigurierbar sein

## Fragen

- Frage: Sollten, wir Elemente, die eine DEFAULT-Definition in der Spec oder unserer Implementierung haben, trotzdem mit
  angeben?
- --> Ich denke JA!
- --> Wir haben hier natürlich das Problem der UNSUPPORTED Parameters: Diese haben ja ggf auch ein Default in der Spec,
  aber wir supporten sie nicht.Wenn sich das in ein paar Jahren ändert. ändert sich auch das Verhalten...
- --> Wir müssen sichergehen, dass alte Expansionsparameterkombinationen auch zukünftig verlässliche Ergebnisse
  liefern...

- Frage: Für TFC-Umsetzung -> Wir müssen ein System-Default für die zu verwendenden Expansion-Parameter setzen

- Frage: Können wir ggf. in system-version immer die aktuellste im Server vorgehaltene Version eines CS-positionieren?
- --> Warum nicht? Diese Version greift ja eh nur, wenn Version nicht gesetzt ist ...

## In R4 definierte Parameter

- url
- valueSet
- valueSetVersion
- context --> UNSUPPORTED
- contextDirection --> UNSUPPORTED
- filter
- date --> UNSUPPORTED
- offset
- count
- includeDesignations
- designation
- includeDefinition
- activeOnly
- excludeNested --> ????
- excludeNotForUI --> UNSUPPORTED
- excludePostCoordinated --> UNSUPPORTED
- displayLanguage
- exclude-system --> UNSUPPORTED
- system-version
- check-system-version
- force-system-version

## includeDefinition

Kardinalität: 0..1
Typ: boolean
Definition: Controls whether the value set definition is included or excluded in value set expansions
DEFAULT: FALSE

## activeOnly

Kardinalität: 0..1
Typ: boolean
Definition: Controls whether inactive concepts are included or excluded in value set expansions. Note that if the value
set explicitly specifies that inactive codes are included, this parameter can still remove them from a specific
expansion, but this parameter cannot include them if the value set excludes them
Default: FALSE

## includeDesignations

Kardinalität: 0..1
Typ: boolean
Definition: Controls whether concept designations are to be included or excluded in value set expansions
Default: FALSE

## designation

Kardinalität: 0..*
Typ: string
Definition: A token that specifies a system+code that is either a use or a language. Designations that match by language
or use are included in the expansion. If no designation is specified, it is at the server discretion which designations
to return

## system-version

Kardinalität: 0..*
Typ: caononical
Definition: Specifies a version to use for a system, if the value set does not specify which one to use. The format is
the same as a canonical URL: [system]|[version] - e.g. http://loinc.org|2.56

## displayLanguage Parameter

Es gibt zwei Möglichkeiten, die gewünschte Sprache anzufordern: entweder direkt über den displayLanguage Parameter oder
über den Accept-Language Header. Ist keine der beiden Varianten gesetz, wird die default-Sprach des Servers oder des
Moduls verwendet.

### displayLanguage Parameter

Kardinalität: 0..1
Typ: code			
Definition: Specifies the language to be used for description in the expansions i.e. the language to be used for
ValueSet.expansion.contains.display

### Accept-Language Header

Accept-Language: <sprache>-<region>, <sprache>;q=<gewichtung>

Accept-Language: de                                                                        
Accept-Language: de-DE                                                                     
Accept-Language: de-DE, de;q=0.9, en;q=0.8                                                 
Accept-Language: en-US, en;q=0.9, de;q=0.8, *;q=0.5

Bestandteile                                                                               
┌───────┬────────────────────────────────────────────────────────────────┐                 
│ Teil │ Beschreibung │                 
├───────┼────────────────────────────────────────────────────────────────┤                 
│ de │ Sprachcode (ISO 639-1)                                         │                 
├───────┼────────────────────────────────────────────────────────────────┤                 
│ DE │ Regionscode (ISO 3166-1 alpha-2), optional │                 
├───────┼────────────────────────────────────────────────────────────────┤                 
│ q=0.8 │ Qualitätswert/Gewichtung (0.0-1.0), optional. Standard ist 1.0 │                 
├───────┼────────────────────────────────────────────────────────────────┤                 
│ * │ Wildcard für alle Sprachen │                 
└───────┴────────────────────────────────────────────────────────────────┘                 
In FHIR-Kontexten

Bei FHIR-Terminologieservern wird Accept-Language verwendet, um die bevorzugte Sprache für
display-Werte bei Operationen wie $lookup, $expand oder $validate-code anzufordern. Der    
Server liefert dann – sofern verfügbar – die Bezeichnungen in der gewünschten Sprache.

### Gewünschtes Systemverhalten

# Display Language Selection Strategy

- Vermutlich müssen wir pro selektiertem Konzept die Strategie individuell definieren, da wir durchaus unterwegs verschiedene Selektionsstrategien haben können, wenn wir uns z.B. aus verschiedenen VS mit verschiedenen Sprachen bedienen
- Eklig wird es, wenn die gleichen Konzepte in verschiedenen Sprachen selektiert werden


Client-Defined-Language-Values (CDLV)
  displayLanguage-Parameter
  Accept-Language Header (nochmal gestaffelt)
-> CDLVs müssen wir erst um letzten Schritt des Renderings berücksichtigen, da diese keinerlei Einfluss auf die Zusammensetzung der Expansion haben


Data-Definied-Language-Values (DDLV)
  ValueSet.language
  CodeSystem.language (ggf. versionsspezifisch)
  ConceptDesignation.language
-> DDLVs ergeben sich aus dem Zusammenspiel der unterwegs gefundenen Zustandswert
-> hier benötigen wir ein sinnvolles Regelwerk: 
   - die Sprache eines Concept Displays wird zuallererst durch das zugrundeliegende CodeSystem definiert, welches das Concept definiert
   - hat das CodeSystem kein language-Tag gesetzt, wird das Concept mit language=unknown belegt
   - das ändert sich auch nicht wenn das Koncept durch ein VS, dass eine Sprache hat importiert wird
   - definiert sich das das Konzept übernehmende ValueSet jedoch ein eigenes display, dann bekommt dieser display-Wert den language-tag des VS, sonst den Wert unknown 

System-Defined-Language-Values (SDLV) 
  Default System Language
-> Es muss möglich sein, die default system language zu definieren


Festlegung für LOINC

Festlegungen für Snomed-CT
- Das preferred synonym (sofern vorhanden) wird immer als Display genutzt in der jeweiligen Sprache verwendet



- Typ von Canonical auf String ändern








# Designation Language Selection Strategy


# Parameter Validation and Error Handling

This section describes all cases where the ValueSet `$expand` operation aborts and which exceptions
are thrown. The validation flow starts at `CombiningTerminologyService.expandValueSet()` and
involves multiple validation stages.

## Exception Types

The expansion operation uses the following exception types:

| Exception                          | HTTP Status | Description                                                |
|------------------------------------|-------------|------------------------------------------------------------|
| `InvalidRequestException`          | 400         | Invalid or unsupported parameter values/combinations       |
| `UnsupportedTerminologyException`  | 400         | Referenced terminology (CodeSystem/ValueSet) not supported |
| `UnknownScopeException`            | 400         | Specified scope is not recognized                          |
| `NotImplementedException`          | 501         | Required feature is not yet implemented                    |
| `UnknownConceptException`          | 404         | Referenced concept cannot be found                         |
| `ResourceNotFoundException`        | 404         | Referenced ValueSet resource cannot be found               |

## Validation Stages

### Stage 1: Parameter Validation (CombiningVsExpandParameterValidator)

The first validation stage is performed by `CombiningVsExpandParameterValidator.validateParameters()`
immediately after the cache lookup. This stage validates request parameters before any processing
begins.

#### Missing Required Parameters

**Condition:** Neither `url` nor `valueSet` parameter is provided.

**Exception:** `InvalidRequestException`

**Message:** `"Either the 'url' or the 'valueSet' parameter must be provided."`

#### Conflicting Parameters

**Condition:** Both `url` and `valueSet` parameters are provided simultaneously.

**Exception:** `InvalidRequestException`

**Message:** `"The 'url' and the 'valueSet' parameter must not be provided at the same time."`

#### Unsupported Parameters

**Condition:** Any of the following parameters is provided:
- `context`
- `filter`
- `contextDirection`
- `date`
- `excludeNested`
- `excludeNotForUI`
- `excludePostCoordinated`
- `exclude-system`

**Exception:** `InvalidRequestException`

**Message:** Lists all unsupported parameters with `"... parameters are not supported yet."`

#### Invalid system-version Parameter Format

**Condition:** The `system-version` parameter value does not match the format `system|version`.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'system-version' Parameter: <value>"`

#### Duplicate system-version Values

**Condition:** Multiple `system-version` values reference the same CodeSystem.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'system-version' Parameter. Duplicate values for: <system>"`

#### Invalid check-system-version Parameter Format

**Condition:** The `check-system-version` parameter value does not match the format `system|version`.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'check-system-version' Parameter: <value>"`

#### Duplicate check-system-version Values

**Condition:** Multiple `check-system-version` values reference the same CodeSystem.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'check-system-version' Parameter. Duplicate values for: <system>"`

#### Invalid force-system-version Parameter Format

**Condition:** The `force-system-version` parameter value does not match the format `system|version`.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'force-system-version' Parameter: <value>"`

#### Duplicate force-system-version Values

**Condition:** Multiple `force-system-version` values reference the same CodeSystem.

**Exception:** `InvalidRequestException`

**Message:** `"Invalid 'force-system-version' Parameter. Duplicate values for: <system>"`

#### Conflicting Version Parameter Combinations

The following combinations are invalid when they reference the same CodeSystem:

| Combination                                      | Message                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------|
| `system-version` + `force-system-version`        | `"Invalid 'system-version/force-system-version' parameter combination: <systems>"`       |
| `system-version` + `check-system-version`        | `"Invalid 'system-version/check-system-version' parameter combination: <systems>"`       |
| `check-system-version` + `force-system-version`  | `"Invalid 'check-system-version/force-system-version' parameter combination: <systems>"` |

**Exception:** `InvalidRequestException`

### Stage 2: ValueSet Content Validation

When an explicit ValueSet is provided, the following content validations are performed.

#### Missing compose Element

**Condition:** The provided ValueSet has no `compose` element.

**Exception:** `InvalidRequestException`

**Message:** `"No 'compose' element is specified in the ValueSet: Nothing to expand ..."`

#### Unsupported lockedDate Element

**Condition:** The ValueSet contains a `compose.lockedDate` element.

**Exception:** `InvalidRequestException`

**Message:** `"ValueSets with 'locked-date' elements are currently not supported for expansion"`

#### Invalid include Element

**Condition:** An `include` element has neither `system` nor `valueSet` specified.

**Exception:** `InvalidRequestException`

**Message:** `"Neither 'system' nor 'valueSet' is specified in the ValueSet 'include'"`

### Stage 3: URL Resolution Errors (CombiningTerminologyService)

When expanding by URL reference, the following errors can occur.

#### Unsupported ValueSet URL

**Condition:** No `TerminologyManager` supports the requested ValueSet URL.

**Exception:** `UnsupportedTerminologyException`

**Message:** `"The ValueSet with the url '<url>' is not supported by the service."`

#### ValueSet Not Found in Scope

**Condition:** The `TerminologyManager` reports support but returns `null` for the specific scope.

**Exception:** `UnsupportedTerminologyException`

**Message:** `"The ValueSet with the url '<url>' is not supported in scope '<scope>'."`

### Stage 4: Expansion Processing Errors (CombiningVsProvidedExpandOperation)

During the actual expansion processing, the following errors can occur.

#### Unsupported CodeSystem in include

**Condition:** An `include.system` references a CodeSystem not supported by any `TerminologyManager`.

**Exception:** `UnsupportedTerminologyException`

**Message:** `"The CodeSystem '<system>' referenced in include is not supported."`

#### Invalid valueSet Reference Format (Missing URL)

**Condition:** An `include.valueSet` canonical reference has no system URL component.

**Exception:** `InvalidRequestException`

**Message:** `"The ValueSet include has no valid system URL: <canonical>"`

#### Invalid valueSet Reference Format (Missing Version)

**Condition:** An `include.valueSet` canonical reference has no version component.

**Exception:** `InvalidRequestException`

**Message:** `"The ValueSet include has no valid version: <canonical>"`

#### Unsupported ValueSet in include

**Condition:** An `include.valueSet` references a ValueSet not supported by any `TerminologyManager`.

**Exception:** `UnsupportedTerminologyException`

**Message:** `"The ValueSet '<url>' referenced in include is not supported."`

#### Hierarchy-based Filter in a Provided ValueSet (SNOMED CT Restricted Mode)

**Condition:** SNOMED CT runs in restricted mode (`snomed.restrictedMode=true`) and an include or
exclude of a ValueSet provided within the request (resource origin `PROVIDED`) uses one of the
filters `constraint`, `parent`, `child`, `concept is-a` or `concept descendent-of`. The filter
`concept in` remains available. ValueSets that are part of the service's content are not affected.

**Exception:** `InvalidRequestException`

**Message:** `"The filter '<property> <op>' is not available for ValueSets provided within the request, because SNOMED CT is provided in restricted mode. Only ValueSets that are part of the terminology service's content may use hierarchy-based filters."`

#### Duplicate Version in xxx-system-version Parameters

**Condition:** During parameter extraction, multiple versions are specified for the same system
within `system-version`, `check-system-version`, or `force-system-version`.

**Exception:** `InvalidRequestException`

**Message:** `"The given '<param-name>' parameter contains multiple versions for the same system. Please provide only one version per system."`

#### Invalid xxx-system-version Parameter Format

**Condition:** A version parameter value cannot be parsed as `system|version`.

**Exception:** `InvalidRequestException`

**Message:** `"The given '<param-name>' parameter is not in the correct format. Please use the format 'system|version'."`

### Stage 5: Paging Errors

#### Negative offset Value

**Condition:** The `offset` parameter is less than 0.

**Exception:** `InvalidRequestException`

**Message:** `"The given offset value is less than 0. This is not for ValueSet Expansion."`

#### offset Exceeds Total Count (with inactive concepts)

**Condition:** The `offset` value exceeds the total number of concepts when `activeOnly=false`.

**Exception:** `InvalidRequestException`

**Message:** `"The given offset value is greater than the number of concepts that can be returned by the SNOMED ValueSet Expansion."`

#### offset Exceeds Active Count (without inactive concepts)

**Condition:** The `offset` value exceeds the number of active concepts when `activeOnly=true`.

**Exception:** `InvalidRequestException`

**Message:** `"The given offset value is greater than the number of active concepts that can be returned by the SNOMED ValueSet Expansion."`

## Error Flow Summary

```
expandValueSet()
    │
    ├── Cache lookup (no errors)
    │
    ├── CombiningVsExpandParameterValidator.validateParameters()
    │   ├── Missing url/valueSet → InvalidRequestException
    │   ├── Both url and valueSet → InvalidRequestException
    │   ├── Unsupported parameters → InvalidRequestException
    │   ├── Invalid version params → InvalidRequestException
    │   └── Invalid ValueSet content → InvalidRequestException
    │
    ├── [URL Mode] URL Resolution
    │   ├── No supporting manager → UnsupportedTerminologyException
    │   └── ValueSet not found → UnsupportedTerminologyException
    │
    └── [ValueSet Mode] expandExplicitValueSet()
        │
        ├── processValueSetCompose()
        │   └── expandValueSetInternal()
        │       ├── expandIncludeBySystem()
        │       │   ├── Unknown CodeSystem → UnsupportedTerminologyException
        │       │   └── Hierarchy-based filter in provided ValueSet
        │       │       (SNOMED restricted mode) → InvalidRequestException
        │       │
        │       └── expandIncludeByValueSetList()
        │           ├── Invalid canonical → InvalidRequestException
        │           └── Unknown ValueSet → UnsupportedTerminologyException
        │
        └── createExpansionElement()
            └── Invalid offset → InvalidRequestException
```


# Implementation Flow Description

This section describes the implementation flow of the ValueSet `$expand` operation, starting from the
entry point `CombiningTerminologyService.expandValueSet`.

## Overview

The expansion process transforms a ValueSet definition (`ValueSet.compose`) into a flat list of
concepts (`ValueSet.expansion.contains`). The implementation follows a layered architecture where
the `CombiningTerminologyService` orchestrates the process and delegates terminology-specific
operations to the appropriate `TerminologyManager` implementations.

```
CombiningTerminologyService.expandValueSet()
    │
    ├── Cache lookup
    ├── Parameter validation
    │
    ├── Case 1: URL reference provided
    │   ├── Find responsible TerminologyManager
    │   ├── Load ValueSet by URL
    │   └── Recursive call with loaded ValueSet
    │
    └── Case 2: Explicit ValueSet provided
        └── CombiningVsProvidedExpandOperation.expandExplicitValueSet()
            └── processValueSetCompose()
                ├── Process all include elements (union)
                └── Process all exclude elements (removal)
```

## Step 1: Entry Point - CombiningTerminologyService.expandValueSet()

**Location:** `core/.../server/service/CombiningTerminologyService.java`

The method receives a `scope` parameter and a `VsExpandParameter` object containing all expansion
parameters. It performs the following steps:

1. **Cache Lookup:** Checks if an expansion for the given parameters already exists in the cache.
   If found, returns the cached result immediately.

2. **Parameter Validation:** Delegates to `CombiningVsExpandParameterValidator.validateParameters()`
   to ensure the request parameters are valid and supported.

3. **Input Determination:** The method distinguishes between two input modes:
   - **URL Mode:** A ValueSet URL is provided via the `url` parameter
   - **ValueSet Mode:** An explicit ValueSet resource is provided via the `valueSet` parameter

## Step 2: URL Resolution (Case 1)

When a `url` parameter is provided:

1. **Find Responsible TerminologyManager:**
   - First checks the `terminologyManagerValueSetMap` for explicitly defined ValueSets
   - If not found, iterates through all registered `TerminologyManager` instances and asks each
     if it supports the requested ValueSet via `manager.vs().isValueSetSupported()`

2. **Load ValueSet:** Calls `terminologyManager.vs().getValueSet(scope, url, version)` to retrieve
   the ValueSet definition.

3. **Transform Parameters:** Embeds the loaded ValueSet into the `VsExpandParameter` object and
   removes the URL reference. The resource origin of the parameter is set to `STORED`, because the
   ValueSet is part of the service's content and was not provided within the request (see
   [Resource Origin](#resource-origin)).

4. **Recursive Call:** Calls `expandValueSet()` again, now with the explicit ValueSet embedded.

## Step 3: Explicit ValueSet Expansion (Case 2)

When an explicit ValueSet is provided, the method delegates to:

```
CombiningVsProvidedExpandOperation.expandExplicitValueSet()
```

**Location:** `core/.../server/service/vs/CombiningVsProvidedExpandOperation.java`

This static method performs the core expansion logic:

1. **Secondary Cache Check:** Checks the expansion cache using a computed `ExpansionCacheKey`.

2. **Extract ValueSet:** Retrieves the ValueSet from the parameters.

3. **Clear Existing Expansion:** Removes any pre-existing expansion element.

4. **Process Version Parameters:** Extracts and validates the three version control parameters:
   - `system-version`: Fallback version for systems without explicit version
   - `check-system-version`: Version that must match (error if different)
   - `force-system-version`: Version that overrides all definitions

5. **Process Compose Element:** Delegates to `processValueSetCompose()`.

6. **Build Expansion Element:** Creates the `ValueSet.expansion` with all result concepts.

7. **Add Metadata:** Sets expansion identifier, timestamp, and used parameters.

8. **Cache Result:** Stores the expansion in the cache.

## Step 4: Processing the Compose Element

**Method:** `processValueSetCompose()`

This method processes the `ValueSet.compose` element according to FHIR R4 composition rules:

### Include Processing (Union)

For each `compose.include` element:
1. Calls `expandValueSetInternal()` to expand the include definition
2. Adds all resulting concepts to the overall result set (union operation)

### Exclude Processing (Removal)

For each `compose.exclude` element:
1. Calls `expandValueSetInternal()` to expand the exclude definition
2. Removes all resulting concepts from the overall result set

## Step 5: Expanding Individual Include/Exclude Elements

**Method:** `expandValueSetInternal()`

This method handles three scenarios according to FHIR R4 specification:

### Scenario 1: System Reference Only

When only `include.system` is specified (no `include.valueSet`):

```
expandIncludeBySystem()
```

- Finds the responsible `TerminologyManager` for the CodeSystem
- Delegates expansion to `terminologyManager.vs().expandInclude()`, passing the expansion settings
  and the processing context (resource origin) as `VsExpandIncludeParameter`
- The TerminologyManager handles:
  - No concept/filter: Returns all codes in the system
  - Concept list: Returns only enumerated codes
  - Filter: Returns codes matching filter criteria

### Scenario 2: ValueSet Reference Only

When only `include.valueSet` is specified (no `include.system`):

```
expandIncludeByValueSetList()
```

- For each referenced ValueSet:
  - Finds the responsible `TerminologyManager`
  - Loads the referenced ValueSet
  - Recursively calls `processValueSetCompose()` to expand it, with the resource origin `STORED`
- If multiple ValueSets are referenced within the same include, computes the **intersection**
  (only concepts present in all referenced ValueSets)

### Scenario 3: Both System and ValueSet Reference

When both `include.system` and `include.valueSet` are specified:

- Expands the system reference via `expandIncludeBySystem()`
- Expands the valueSet references via `expandIncludeByValueSetList()`
- Computes the **intersection** of both results

## Resource Origin

The terminology modules can distinguish where a ValueSet comes from. This is used by the SNOMED CT
restricted mode, which only allows hierarchy-based filters for ValueSets that are part of the
service's content.

| Origin     | Meaning                                   | Set by                                                                       |
|------------|-------------------------------------------|------------------------------------------------------------------------------|
| `STORED`   | ValueSet is part of the service's content | URL resolution (Step 2), ValueSets referenced via `compose.include.valueSet` |
| `PROVIDED` | ValueSet was provided within the request  | Default for the `valueSet` parameter                                         |

- The origin is part of `VsExpandParameter` (`resourceOrigin`, default `PROVIDED`) and therefore
  part of the expansion cache key.
- `CombiningVsProvidedExpandOperation` passes it as `ProcessingContext` to
  `ValueSetSvcInterface.expandInclude(scope, include, VsExpandIncludeParameter)`. Excludes inherit
  the origin of their ValueSet.
- The default implementation of this method ignores the origin, so terminologies without usage
  restrictions behave as before.
- `ValueSet/$validate-code` expands the ValueSet internally and therefore follows the same rules.

## Step 6: Building the Expansion Element

**Method:** `createExpansionElement()`

Creates the `ValueSet.expansion` element and populates it with concepts:

1. **Apply Paging:** Processes `count` and `offset` parameters
   - Default count: 100
   - Maximum count: 1,000,000

2. **Apply Filters:** Filters based on `activeOnly` parameter

3. **Determine Display Language:** Uses `displayLanguage` parameter or ValueSet language

4. **Create Contains Components:** For each concept in the result:
   - Delegates to `terminologyManager.vs().createExpansionContainsComponent()`
   - The TerminologyManager enriches concepts with designations, properties, etc.

5. **Set Totals:** Sets `expansion.total` and `expansion.offset` for paging support

## Sequence Diagram

```
Client                  CombiningTermSvc              CombiningVsProvided...           TerminologyManager
   │                          │                              │                              │
   │  expandValueSet(params)  │                              │                              │
   │─────────────────────────>│                              │                              │
   │                          │                              │                              │
   │                          │  [if URL provided]           │                              │
   │                          │  getValueSet()               │                              │
   │                          │─────────────────────────────────────────────────────────────>│
   │                          │<─────────────────────────────────────────────────────────────│
   │                          │                              │                              │
   │                          │  expandExplicitValueSet()    │                              │
   │                          │─────────────────────────────>│                              │
   │                          │                              │                              │
   │                          │                              │  processValueSetCompose()    │
   │                          │                              │──────┐                       │
   │                          │                              │      │ for each include      │
   │                          │                              │<─────┘                       │
   │                          │                              │                              │
   │                          │                              │  expandInclude()             │
   │                          │                              │─────────────────────────────>│
   │                          │                              │<─────────────────────────────│
   │                          │                              │                              │
   │                          │                              │  createExpansionContains()   │
   │                          │                              │─────────────────────────────>│
   │                          │                              │<─────────────────────────────│
   │                          │                              │                              │
   │                          │  ValueSet (with expansion)   │                              │
   │                          │<─────────────────────────────│                              │
   │                          │                              │                              │
   │  ValueSet (with expansion)                              │                              │
   │<─────────────────────────│                              │                              │
```

## Key Classes Summary

| Class                                  | Responsibility                                                                       |
|----------------------------------------|--------------------------------------------------------------------------------------|
| `CombiningTerminologyService`          | Entry point, orchestration, TerminologyManager discovery                             |
| `CombiningVsExpandParameterValidator`  | Parameter validation                                                                 |
| `CombiningVsProvidedExpandOperation`   | Core expansion logic for explicit ValueSets                                          |
| `VsExpandParameter`                    | Encapsulates all expansion request parameters                                        |
| `VsExpandIncludeResult`                | Holds expansion results (concepts, references, version strategies)                   |
| `TerminologyManager`                   | SPI interface for terminology-specific operations                                    |
| `ValueSetSvcInterface`                 | Interface for ValueSet operations (expandInclude, createExpansionContainsComponent)  |
