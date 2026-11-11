# Expansion Scenarios

Die folgende Seite beschreibt Szenarien, die uns bei der Umsetzung der $expand-Operation im Terminologieserver begegnen
können. Diese Szenarien sollen anhand von Beispielen näher erläutert werden und als Input für die Generierung von
Testdaten und Testfällen dienen.

## Ausgangssituation

Wir unterscheiden zwei CodeSysteme: CS1 + CS2
CS1 hat zwei Versionen: 2025, 2026
CS2 hat eine Version: 2026
Wir referenzieren die CodeSystem-Versionen wie folgt: CS1-2025, CS1-2026, CS2-2026

CS1-2025 beinhaltet folgende Konzepte: A01, A02, A03
CS1-2026 beinhaltet folgende Konzepte: A01, A02, A03, A04
CS2-2026 beinhaltet folgende Konzepte: B01, B02, B03
Wir referenzieren die Konzepte aus CodeSystem-versionen wie folgt: CS1-2025#A01, CS1-2025#A02, CS1-2025#A03,
CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03,

## Szenarien

Jede Zeile in der Tabelle repräsentiert ein Szenario. Die description-Spalte beschreibt die Ausgestaltung des
ValueSet.compose Elements, welches die Grundlage der Expandierung bildet. Nur die angegebenen Elemente werden gesetzt.
Folgende Elemente werden vorerst nicht behandelt: compose.lockedDate, compose.inactive

| id     | url-suffix | version | description (ValueSet.compose with)                                                                                                                                                                                                                                                | expected expansion results                                                                       |
|--------|------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| VS00_A | VS00       | 2025    | 1 include: include[0].system=CS1 include[0].version=2025                                                                                                                                                                                                                           | CS1-2025#A01, CS1-2025#A02, CS1-2025#A03                                                         |
| VS00_B | VS00       | 2026    | 1 include: include[0].system=CS1 include[0].version=2026                                                                                                                                                                                                                           | CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04                                           |
| VS01   | VS01       | 2026    | 1 include: include[0].system=CS1                                                                                                                                                                                                                                                   | CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04                                           |
| VS02   | VS02       | 2026    | 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS1 include[1].version=2026                                                                                                                                                                            | CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04 |
| VS03   | VS03       | 2026    | 2 includes: include[0].system=CS1 include[0].version=2025 include[1].system=CS2 include[1].version=2026                                                                                                                                                                            | CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03               |
| VS04   | VS04       | 2025    | 1 include (extensional): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[0].concept[1].code=A02                                                                                                                                               | CS1-2025#A01, CS1-2025#A02                                                                       |
| VS05   | VS05       | 2025    | 1 include (extensional): include[0].system=CS1 include[0].concept[0].code=A01                                                                                                                                                                                                      | CS1-2026#A01                                                                                     |
| VS06   | VS06       | 2025    | 1 include (extensional with display): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[0].concept[0].display="Custom Display"                                                                                                                  | CS1-2025#A01 (with custom display)                                                               |
| VS07   | VS07       | 2025    | 1 include + 1 exclude: include[0].system=CS1 include[0].version=2025 exclude[0].system=CS1 exclude[0].version=2025 exclude[0].concept[0].code=A02                                                                                                                                  | CS1-2025#A01, CS1-2025#A03                                                                       |
| VS08   | VS08       | 2026    | 1 include + 1 exclude: include[0].system=CS1 include[0].version=2026 exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A01 exclude[0].concept[1].code=A04                                                                                                   | CS1-2026#A02, CS1-2026#A03                                                                       |
| VS09   | VS09       | 2026    | 2 includes + 1 exclude: include[0].system=CS1 include[0].version=2025 include[1].system=CS1 include[1].version=2026 exclude[0].system=CS1 exclude[0].version=2025                                                                                                                  | CS1-2026#A01, CS1-2026#A02, CS1-2026#A03, CS1-2026#A04                                           |
| VS10   | VS10       | 2025    | 1 include (intensional with filter '='): include[0].system=CS1 include[0].version=2025 include[0].filter[0].property=code include[0].filter[0].op='=' include[0].filter[0].value=A01                                                                                               | CS1-2025#A01                                                                                     |
| VS11   | VS11       | 2026    | 1 include (intensional with filter regex): include[0].system=CS1 include[0].version=2026 include[0].filter[0].property=code include[0].filter[0].op=regex include[0].filter[0].value=A0[12]                                                                                        | CS1-2026#A01, CS1-2026#A02                                                                       |
| VS12   | VS12       | 2025    | 1 include (valueSet import): include[0].valueSet=VS00_A                                                                                                                                                                                                                            | CS1-2025#A01, CS1-2025#A02, CS1-2025#A03                                                         |
| VS13   | VS13       | 2026    | 2 includes (valueSet import + system): include[0].valueSet=VS00_A include[1].system=CS2 include[1].version=2026                                                                                                                                                                    | CS1-2025#A01, CS1-2025#A02, CS1-2025#A03, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03               |
| VS14   | VS14       | 2026    | 1 include (extensional) + 1 exclude (extensional): include[0].system=CS1 include[0].version=2026 include[0].concept[0].code=A01 include[0].concept[1].code=A02 include[0].concept[2].code=A03 exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A02         | CS1-2026#A01, CS1-2026#A03                                                                       |
| VS15   | VS15       | 2026    | 2 includes (mixed): include[0].system=CS1 include[0].version=2025 include[0].concept[0].code=A01 include[1].system=CS2 include[1].version=2026                                                                                                                                     | CS1-2025#A01, CS2-2026#B01, CS2-2026#B02, CS2-2026#B03                                           |
| VS16   | VS16       | 2026    | 1 include (multiple filters AND): include[0].system=CS1 include[0].version=2026 include[0].filter[0].property=code include[0].filter[0].op=regex include[0].filter[0].value=A.* include[0].filter[1].property=code include[0].filter[1].op=regex include[0].filter[1].value=.*[34] | CS1-2026#A03, CS1-2026#A04                                                                       |
| VS17   | VS17       | 2026    | 1 include (valueSet import) + 1 exclude: include[0].valueSet=VS00_B exclude[0].system=CS1 exclude[0].version=2026 exclude[0].concept[0].code=A04                                                                                                                                   | CS1-2026#A01, CS1-2026#A02, CS1-2026#A03                                                         |
| VS18   | VS18       | 2026    | 1 include (2 valueSet imports): include[0].valueSet[0]=VS00_B include[0].valueSet[1]=VS08                                                                                                                                                                                          | CS1-2026#A02, CS1-2026#A03                                                                       |

Hier sind sicherlich noch mehr Szenarien denkbar

## Umgang mit Sprachen

### Annahmen

- Sofern ein Nutzer zum Ausdruck bringt, dass er eine bestimmte Sprache haben möchte, folgen wir seiner Bitte nach
  Möglichkeit
- der Client-Parameter displayLanguage wird prioritär verwendet, da wir hier davon ausgehen kännen, dass es eine
  bewusste Entscheidung des Nutzers ist (z.B. durch eine Sprachselektion in einer zugreifenden Anwendung) -> in diesem
  Zusammenhang könnten wir sogar überlegen, kein Display zurückzugeben, wenn der Nutzerwunsch nicht erfüllt werden kann.
- Auch der Browser des Clients gibt gerne Hinweise zur bevorzugten Sprache. Das ist aber meist eher der indirekte
  Nutzerwunsch, der dann durch den Accept-Language Header formuliert wird. Auf diesen kann man sicherlich eingehen, ich
  würde hier jedoch nicht soweit gehen wollen gar keinen Display anzuzeigen, wenn es grundsätzlich Displaywerte in
  anderen Sprachen gibt. --> Vieleicht ist dann die default-language des Servers ein mögliches Fallback und danach die
  default-language des VS bzw. CS.

- Zusätzlich haben wir noch eine default-language für den Server, die bei fehlendem Nutzerwunsch eine Präferenz
  darstellt. Aber auch hier müssen wir uns überlegen, wie stark diese Präferenz durchgesetzt wird. Auch hier würde ich
  sagen, dass wir lieber der Präferenz NICHT folgen und irgendwas (VS/CS-default) anzeigen, als gar keine Wert
  zurückgeben

- Sofern ein ValueSet-Autor/Publisher für sein ValueSet eine Sprache definiert, gehen wir davon aus,
    - dass sämtlich im include.concept angegebene display-Werte in der definierten VS-Sprache sind und
    - dass sämtlich im include.concept angegebenen designation-Werte in der definierten VS-Sprache sind, sofern kein
      anderer Sprachcode angegeben wurde und
    - dass er üblicherweise auch Displays in der angegebenen Sprache in der Expansion haben möchte, sofern der Nutzer
      keine eigenen Wünsche über den displayLanguage Parameter oder den Accept-Header äußert und das System-default
      nicht bedient werden kann

- Sofern ein CodeSystem-Autor/Publisher für sein CodeSystem eine Sprache definiert, gehen wir davon aus,
    - dass sämtliche angegebenen display-Werte in der definierten CS-Sprache sind und
    - dass sämtlich angegebenen designation-Werte in der definierten CS-Sprache sind, sofern kein anderer Sprachcode
      angegeben wurde und
    - dass im Zweifelsfall bei Expansions, die Sprache des CodeSystems eine entscheidende Rolle spielt

### Grundsatzfragen

- müssen wir uns bei der Sprachselektion überhaupt um die defualts von VS und CS kümmern oder interessieren uns nur die
  Client-Parameter und das Server-Default??? --> wir müssen uns um diese Werte primär als Fallback-Strategie kümmern
- brauchen wir eine Regel für die Region-Order, z.B wenn nach en gefragt wird, wir aber Terme in en-GB und en-US
  haben??? -> alphabetisch? konfigurierbar? in Abstufungen, z.B. die mit display-use zuerst ...
- sollte bei verschachtelten VS nur das oberste steuernd auf die zu selektierende Konzeptsprache einwirken --> Ja!

### Implemented Language Selection Strategy

This section describes in detail how the terminology server selects the appropriate display value for
each concept during a `$expand` operation. The process involves three major phases:

1. **Assembling the language selection parameters** from multiple sources
2. **Collecting designations** for each concept from CodeSystem and ValueSet definitions
3. **Selecting the best matching display** via a prioritized fallback strategy

#### Phase 1: Assembling the Language Selection Parameters

The entry point for the `$expand` operation is the `expandValueSet` method of
`ValueSetResourceProvider`. When a client sends an expansion request, the provider extracts
language-relevant information from two request-level sources:

- **`displayLanguage` parameter** — An explicit FHIR operation parameter (`$expand?displayLanguage=de`).
  If present, it is forwarded as `displayLanguageParam` on the `VsExpandParameter` object.
- **`Accept-Language` HTTP header** — The standard HTTP content negotiation header
  (e.g. `de-DE, de;q=0.9, en;q=0.7`). If present, it is parsed into a `LanguageParameter` object
  using `LanguageParameter.parse()`, which splits the header value into a list of `LanguageRange`
  entries sorted by quality factor in descending order.

Both values are embedded into the `VsExpandParameter` and passed to
`CombiningTerminologyService.expandValueSet()`.

The `CombiningTerminologyService` holds a third language source:

- **System default language** — A server-wide BCP 47 language code configured via
  `core.systemLanguage` (default: `de-DE`). It is parsed into a `LanguageParameter` at server
  startup and validated against the `LanguageRegistry`.

Once the request reaches `CombiningVsProvidedExpandOperation.expandExplicitValueSet()`, the fourth
and final language source is determined:

- **ValueSet language** — The `ValueSet.language` element of the resource being expanded. If the
  ValueSet declares a language and that language is recognized by the `LanguageRegistry`, it is
  parsed into a `LanguageParameter`.

All four sources are combined into a single `LanguageSelectionParameter` object:

```java
LanguageSelectionParameter.builder()
    .

displayLanguageParameter(languageDisplayLanguageParameter)  // from $expand parameter
    .

acceptLanguageHeader(acceptLanguageHeader)                  // from HTTP header
    .

systemDefaultLanguage(languageSystemDefault)                // from server config
    .

vsLanguage(vsLanguageParameter)                             // from ValueSet.language
    .

build();
```

This `LanguageSelectionParameter` is then passed through the expansion pipeline to the point where
individual expansion contains components are created.

#### Phase 2: Collecting Designations

For each concept that ends up in the expansion result, the responsible `TerminologyManager` creates
an `ExpansionContainsComponent` via its `createExpansionContainsComponent` method. During this
process, a `DesignationManager` instance is created and populated with all available designations
from two sources:

##### CodeSystem Designations (Source: `CS`)

1. **Concept display** — If the concept has a `display` element, it is added as a `Designation`
   with:
    - `source = CS`
    - `language` = the CodeSystem's `language` element (e.g. `de-DE`)
    - `use` = `display` from `http://terminology.hl7.org/CodeSystem/designation-usage`
    - `value` = the display text

2. **Concept designations** — All entries from `ConceptDefinitionComponent.designation` are added
   with:
    - `source = CS`
    - `language` = the designation's own `language` if present, otherwise the CodeSystem's `language`
    - `use` = the designation's `use` coding
    - `value` = the designation text

##### ValueSet Designations (Source: `VS`)

Designations from ValueSet sources originate from explicit concept references defined in
`ValueSet.compose.include.concept`. The `ValueSet.language` is propagated through the element
hierarchy via HAPI FHIR's `UserData` mechanism: `ValueSet.language` → `compose` → `include` →
individual `ConceptReferenceComponent`.

1. **Explicit display** — If a `ConceptReferenceComponent` has a `display` element, it is added
   with:
    - `source = VS`
    - `language` = the ValueSet's `language` (inherited via `UserData`)
    - `use` = `display` from `http://terminology.hl7.org/CodeSystem/designation-usage`
    - `value` = the display text

2. **Explicit designations** — All entries from `ConceptReferenceComponent.designation` are added
   with:
    - `source = VS`
    - `language` = the designation's own `language` if present, otherwise the ValueSet's `language`
    - `use` = `display` from `http://terminology.hl7.org/CodeSystem/designation-usage`
    - `value` = the designation text

All collected designations are passed to the `DesignationManager`, which then performs the display
selection.

#### Phase 3: Display Selection in `DesignationManager.getDisplay()`

The `DesignationManager.getDisplay(LanguageSelectionParameter)` method implements a four-level
priority chain with different strictness behavior per level.

##### Priority Level 1: `displayLanguage` Parameter (Strict)

If the `displayLanguageParameter` is set, **only** this language is considered. The method delegates
to the internal `getDisplay(LanguageParameter)` method and returns whatever it finds — including
`null`. This is the **strict** level: if the requested language cannot be served, no display is
returned at all, and the expansion concept will have no `display` element. The rationale is that
the `displayLanguage` parameter represents an explicit, conscious decision by the user.

**There is no fallback** from this level.

##### Priority Level 2: `Accept-Language` Header (With Fallback)

If no `displayLanguage` was specified but an `Accept-Language` header is present, the method
attempts to find a matching display. If a match is found, it is returned immediately. If no match
is found, the method **falls through** to the next priority level. The rationale is that the
`Accept-Language` header is typically set by the browser and represents an indirect user preference
that should not suppress display values entirely.

##### Priority Level 3: System Default Language (With Fallback)

If the previous levels did not produce a result, the system default language is tried next. Again,
if a match is found it is returned; otherwise the method falls through to the final level.

##### Priority Level 4: ValueSet Language (Terminal)

As the last resort, the language declared on the ValueSet resource itself is used. Whatever the
outcome (match or `null`), it is returned without further fallback.

If none of the four levels is configured (all `null`), the method returns `null`, meaning no
display will be set. That normally would be a highly unusual situation, as at least the system default language should
be configured.

#### Language Matching Within a Single Priority Level

Each priority level delegates to the private `getDisplay(LanguageParameter)` method, which
implements the following matching algorithm:

##### Step 1: Iterate Over Language Ranges

The `LanguageParameter` may contain multiple language ranges sorted by quality factor (most relevant
for `Accept-Language` headers with multiple entries like `de-DE;q=1.0, en;q=0.8`). Each range is
processed in priority order.

##### Step 2: Build the Language Search Set

For each language range, a `LinkedHashSet` of candidate language codes is built using the
`LanguageRegistry`:

- **Base language** (e.g. `de`): The set is expanded to include all known regional variants
  (e.g. `de`, `de-DE`, `de-AT`, `de-CH`). This uses `LanguageRegistry.getLanguageVariants()`.
- **Regional variant** (e.g. `de-DE`): The set includes the variant itself and its base language
  (e.g. `de-DE`, `de`). This uses `LanguageRegistry.getLanguageHierarchy()`.

The `LinkedHashSet` preserves insertion order and eliminates duplicates, ensuring each language code
is checked exactly once.

##### Step 3: Search Designations by Source Priority

For each candidate language code, designations are searched in a defined order:

1. **VS-sourced designations first** — Iterate over all language codes and look for a designation
   with `source = VS`, the matching `language`, and `use = display`. Return the first match.
2. **CS-sourced designations second** — If no VS match was found, iterate over all language codes
   again and look for a designation with `source = CS`, the matching `language`, and
   `use = display`. Return the first match.

This ordering ensures that display values explicitly defined in the ValueSet definition take
precedence over those inherited from the CodeSystem.

##### Step 4: Fallback to Designations Without Language

If no language-matched designation was found across all ranges, a final fallback attempts to find
a designation with **no language code** (i.e., `language` is `null` or blank) that still carries
the `display` use coding. Again, VS-sourced designations are preferred over CS-sourced ones.

#### Designation Filtering Criteria

Across all steps, only designations that satisfy all of the following conditions are considered:

- The `source` matches the currently searched source (`VS` or `CS`)
- The `language` matches the candidate language code (or is absent in the final fallback)
- The `use` coding has `system = http://terminology.hl7.org/CodeSystem/designation-usage` and
  `code = display`

#### Summary: Complete Flow

```
HTTP Request
  │
  ├── displayLanguage parameter ──────────────┐
  ├── Accept-Language header ─────────────────┤
  │                                           │
  ▼                                           ▼
ValueSetResourceProvider.expandValueSet()     LanguageParameter.parse()
  │
  ▼
CombiningTerminologyService.expandValueSet()
  │  ← holds systemDefaultLanguage from ConfigProperties
  │
  ▼
CombiningVsProvidedExpandOperation.expandExplicitValueSet()
  │  ← reads ValueSet.language
  │  ← builds LanguageSelectionParameter
  │
  ├── processValueSetCompose()        → resolves includes/excludes → concept list
  │
  ▼
createExpansionElement()
  │  ← iterates over concepts
  │
  ▼ (per concept)
TerminologyManager.vs().createExpansionContainsComponent()
  │
  ├── new DesignationManager()
  │     ├── addDesignation(CS display)
  │     ├── addDesignation(CS designations)
  │     ├── addDesignation(VS display)
  │     └── addDesignation(VS designations)
  │
  ├── designationManager.getDisplay(languageSelectionParameter)
  │     ├── Level 1: displayLanguage    → strict, no fallback
  │     ├── Level 2: Accept-Language    → with fallback
  │     ├── Level 3: system default     → with fallback
  │     └── Level 4: VS language        → terminal
  │
  └── expansionContainsComponent.setDisplay(result)
```

### Szenarien

#### Ausgangssituation

Im folgenden definieren wir Beispiel-CodeSysteme und ValueSets, die wir für unsere Szenarien benötigen. Die Struktur der
Benennung
folgt folgendem Muster: NAME-VERSION. Das CodeSystem ABC0-A hätte entsprechend den URL-Suffix: 'ABC0' und die Version: '
A'- Im Ergebnis ergibt sich CodeSystem.url='https://example.org/fhir/CodeSystem/ABC0' CodeSystem.version='A'

Orphacode / ATC-GM
LCS00-A hat den languageCode 'de' und ein Konzept M mit Display 'Müll' und einer Designation: 'Abfall' [de]
LVS00-A0 hat KEINEN languageCode und ist ein all-concepts-VS von LCS00-A ('de')
LVS00-A1 hat den languageCode 'de' und ist ein all-concepts-VS von LCS00-A ('de')
LVS00-A2 hat den languageCode 'de-DE' und ist ein all-concepts-VS von LCS00-A ('de')

ICD10GM / OPS
LCS00-B hat den languageCode 'de-DE' und ein Konzept M mit Display 'Müll'
LVS00-B0 hat KEINEN languageCode und ist ein all-concepts-VS von LCS00-B ('de-DE')
LVS00-B1 hat den languageCode 'de' und ist ein all-concepts-VS von LCS00-B ('de-DE')
LVS00-B2 hat den languageCode 'de-DE' und ist ein all-concepts-VS von LCS00-B ('de-DE')

ICF / ICD-O-3 / ICD-10-WHO
LCS00-C hat den languageCode 'en' und ein Konzept M OHNE Display
LCSS00-C supplementiert LCS00-C hat den languageCode 'de' und ein Konzept M OHNE Display aber mit einer Designation: '
Müll' [de]
LVS00-C1 hat den languageCode 'de' und ist ein all-concepts-VS von LCS00-C ('en')

LOINC Supplement
LCS00-D hat den languageCode 'en-US' und ein Konzept M OHN Display und mehreren Designations: 'Müll' [de-DE], '
Mist' [de-AT], 'rubbish' [en-GB], 'trash' [en-US] (jeweils display use)
LCSS00-D supplementiert LCS00-D hat den languageCode 'de-DE' und ein Konzept M OHNE Display aber mit einer
Designation: 'Müll' [de-DE]
LVS00-D2 hat den languageCode 'de-DE' und included das Konzept M OHNE Display aus LCS00-D
