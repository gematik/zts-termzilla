# FHIR `ValueSet/$expand`

> ZTS implementation of the FHIR R4 `ValueSet/$expand` operation.
> See also: `expand-implementation-plan.md` (analysis) and `expand-traceability.md` (requirement
> traceability).

## 1. Overview

`$expand` transforms a ValueSet *definition* (`ValueSet.compose`) into a ValueSet *expansion*
(`ValueSet.expansion.contains`) — the flat, deterministic list of concepts that belong to the
ValueSet.

- **Supported FHIR version:** R4 only (HAPI `FhirContext.forR4`).
- **Definition vs. expansion:** the engine always evaluates `compose`; a pre-existing
  `ValueSet.expansion` on the input is discarded and recomputed.
- **Supplements:** a CodeSystem supplement enriches metadata (display, designations) of existing
  codes. It never changes ValueSet membership or `expansion.total`.

## 2. Supported Endpoints

| Form           | Method | Path                          |
|----------------|--------|-------------------------------|
| Type-level     | GET    | `/fhir/ValueSet/$expand`      |
| Type-level     | POST   | `/fhir/ValueSet/$expand`      |
| Instance-level | GET    | `/fhir/ValueSet/{id}/$expand` |
| Instance-level | POST   | `/fhir/ValueSet/{id}/$expand` |

- GET uses primitive query parameters; a serialized `valueSet` resource is not accepted on GET.
- POST accepts a FHIR `Parameters` resource (`Content-Type: application/fhir+json`).
- Instance-level resolves the ValueSet by the server-local `{id}`. `url`, `valueSetVersion`, and
  `valueSet` must not be combined with `{id}` (contradictory identification → 400).
- Type-level and instance-level share the same core engine (`CombiningVsProvidedExpandOperation`).

## 3. Supported Parameters

| Parameter                | Card. | Type      | Default | Supported | Effect                                                                                                                 | In `expansion.parameter`                                                                                                                                                                                                                    |
|--------------------------|-------|-----------|---------|-----------|------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `url`                    | 0..1  | uri       | —       | ✅        | Selects the ValueSet by canonical URL. Requires `valueSetVersion`.                                                     | ✅                                                                                                                                                                                                                                          |
| `valueSet`               | 0..1  | Resource  | —       | ✅        | Inline ValueSet to expand (POST only).                                                                                 | —                                                                                                                                                                                                                                           |
| `valueSetVersion`        | 0..1  | string    | —       | ✅        | Version of the ValueSet selected by `url`. Mandatory with `url`.                                                       | ✅                                                                                                                                                                                                                                          |
| `context`                | 0..1  | uri       | —       | ❌        | Rejected (400).                                                                                                        | —                                                                                                                                                                                                                                           |
| `contextDirection`       | 0..1  | code      | —       | ❌        | Rejected (400).                                                                                                        | —                                                                                                                                                                                                                                           |
| `filter`                 | 0..1  | string    | —       | ✅        | Text filter over code/display/designations.                                                                            | ✅                                                                                                                                                                                                                                          |
| `date`                   | 0..1  | dateTime  | —       | ❌        | Rejected (400).                                                                                                        | —                                                                                                                                                                                                                                           |
| `offset`                 | 0..1  | integer   | 0       | ✅        | Record offset for paging (≥ 0).                                                                                        | ✅                                                                                                                                                                                                                                          |
| `count`                  | 0..1  | integer   | 100     | ✅        | Page size (≥ 0, capped at 1 000 000). `count=0` returns only the total.                                                | ✅                                                                                                                                                                                                                                          |
| `includeDesignations`    | 0..1  | boolean   | false   | ✅        | Include designations in `contains`.                                                                                    | ✅                                                                                                                                                                                                                                          |
| `designation`            | 0..*  | string    | —       | ✅        | Restrict returned designations by language/use token.                                                                  | ✅                                                                                                                                                                                                                                          |
| `includeDefinition`      | 0..1  | boolean   | false   | ✅        | Retain `compose` in the response when `true`.                                                                          | ✅                                                                                                                                                                                                                                          |
| `activeOnly`             | 0..1  | boolean   | false   | ✅        | Exclude inactive concepts when `true`.                                                                                 | ✅                                                                                                                                                                                                                                          |
| `excludeNested`          | 0..1  | boolean   | false   | ✅        | Accepted; the ZTS expansion is always flat, so no nested `contains` are produced.                                      | ✅                                                                                                                                                                                                                                          |
| `excludeNotForUI`        | 0..1  | boolean   | —       | ❌        | Rejected (400).                                                                                                        | —                                                                                                                                                                                                                                           |
| `excludePostCoordinated` | 0..1  | boolean   | —       | ❌        | Rejected (400).                                                                                                        | —                                                                                                                                                                                                                                           |
| `displayLanguage`        | 0..1  | code      | —       | ✅        | Preferred display language (BCP-47).                                                                                   | ✅                                                                                                                                                                                                                                          |
| `exclude-system`         | 0..*  | canonical | —       | ✅        | Remove all concepts of the given system (optionally `\|version`). Applied before paging.                               | ✅                                                                                                                                                                                                                                          |
| `system-version`         | 0..*  | canonical | —       | ✅        | Fallback version `[system]\|[version]` when the ValueSet does not pin one.                                             | ✅                                                                                                                                                                                                                                          |
| `check-system-version`   | 0..*  | canonical | —       | ✅        | Fail if the effective version differs.                                                                                 | ✅                                                                                                                                                                                                                                          |
| `force-system-version`   | 0..*  | canonical | —       | ✅        | Override the version throughout the dependency graph.                                                                  | ✅                                                                                                                                                                                                                                          |
| `useSupplement` (R5)     | 0..*  | canonical | —       | ✅        | Accepted as a ZTS compatibility extension on this R4 server. Each value is a canonical `[supplement]` or `[supplement] | [version]` referencing a CodeSystem supplement to apply. Request-level `useSupplement` values are merged with ValueSet-declared `valueset-supplement` extensions; conflicting versions of the same supplement canonical are rejected (400). | ✅ |

Cardinality (`0..1`) and datatype validation are enforced identically for GET and POST by the HAPI
operation binding. A quoted boolean (`"valueBoolean": "true"`) is not coerced.

## 4. ValueSet Resolution

- **By URL + version:** `url` + `valueSetVersion` resolve via the responsible
  `TerminologyManager`. `url` without `valueSetVersion` fails with 400 at the public
  API boundary.
- **Inline:** a `valueSet` resource must contain an expandable `compose`.
- **Instance-level:** resolved by server-local `{id}`.
- **Nested references:** `compose.include.valueSet` / `compose.exclude.valueSet` are resolved
  recursively. Each reference must be a fully qualified canonical `url|version`.
- **Cycle detection:** the dependency graph is traversed with a resolution path keyed by
  `canonical URL + effective version`. A cycle fails with 400 instead of ah
  `StackOverflowError`.
- **Origin:** ValueSets resolved by URL and nested references are treated as part of the
  service's content (`STORED`), inline ValueSets as provided within the request (`PROVIDED`).
  Terminology modules may restrict filters for provided ValueSets (see Limitations).

## 5. CodeSystem Resolution

- An include/exclude with explicit concepts or filters requires `system`; under ZTS rules an
  explicit `version` is required.
- Version selection priority: `force-system-version` → include version → `system-version` →
  `check-system-version` → system default.
- Every explicitly referenced code must exist in the resolved CodeSystem version;
  otherwise the whole expansion fails.

## 6. Compose Semantics

- **Includes** are unioned; **excludes** are subtracted from the union.
- Supported include/exclude forms: whole CodeSystem, explicit concepts, supported filters,
  referenced ValueSets.
- A single include/exclude must not mix `concept` and `filter`.
- **Deduplication** by `system + effective version + code` (never by code alone).
- **Ordering** is deterministic: `ConceptBaseInfo` natural order (system, version, code).
- **Active concepts:** `activeOnly=true` removes inactive concepts from the result; it cannot
  reintroduce concepts the ValueSet definition excludes.
- **Nesting:** the ZTS expansion is always flat; `excludeNested` is accepted for compatibility.

## 7. Paging

- `offset` is a record offset (default `0`, must be ≥ 0).
- `count` is the page size (default `100`, must be ≥ 0, capped at 1 000 000).
- `count=0` returns no concepts but the full `expansion.total`.
- `expansion.total` is the semantic membership **before** page slicing.
- Slicing happens after membership calculation, exclusion, active filtering, and
  `exclude-system`.
- Ordering is deterministic (see §6), so repeated calls are stable.

## 8. Supplements

- **R4 mechanism:** the `valueset-supplement` extension (`http://hl7.org/fhir/StructureDefinition/valueset-supplement`)
  on the ValueSet declares a
  supplement by canonical URL.
- **R5 `useSupplement`:** `useSupplement=<supplement-canonical>|<version>` applies the
  supplement (ZTS compatibility extension on R4).
- **Validation** (`SupplementValidator`): a supplement must be a CodeSystem with
  `content = supplement`, declare `supplements`, target the base CodeSystem (and version) actually
  used by the expansion, and contain only codes present in the base CodeSystem.
- **Membership invariance:** a supplement never adds or removes codes and never changes
  `expansion.total`.
- **Recording:** an applied supplement is recorded in `expansion.parameter` as
  `[base CodeSystem canonical]#supplement` with the supplement canonical and version as value.

> Full supplement *application* (display/designation enrichment from supplements) is implemented
> for validation and membership invariance; display-selection precedence from supplements is
> documented in §11 of the plan and deferred for the merge of multiple supplements.

## 9. Error Handling

All controlled failures return a FHIR `OperationOutcome` with at least one issue of
`severity = error` (or `fatal` for unexpected failures). No partial `ValueSet.expansion` is
returned on error.

| HTTP | When                                                                                                         |
|------|--------------------------------------------------------------------------------------------------------------|
| 400  | Malformed, structurally invalid, wrongly typed, duplicated, contradictory, or unsupported request parameters |
| 422  | Formally valid request that cannot be expanded due to semantic/terminology dependency problems               |
| 500  | Unexpected internal failure (sanitized; no stack traces, paths, or DB details)                               |
| 503  | Timeout, overload, or temporarily unavailable infrastructure                                                 |

Example (400):

```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "invalid",
      "diagnostics": "The 'url' parameter requires an explicit 'valueSetVersion' parameter."
    }
  ]
}
```

## 10. Examples

Examples are exercised by the integration tests in
`testing/src/test/java/.../expand/` (`ExpandExpectedBehaviourIT`,
`ExpandExpectedParameterBehaviourIT`) and the unit tests referenced in
`expand-traceability.md`.

1. **GET without supplement:** `GET /fhir/ValueSet/$expand?url=<vs>&valueSetVersion=<v>`
2. **POST inline ValueSet:** `Parameters` with a `valueSet` resource parameter.
3. **R4 supplement extension:** ValueSet carries `valueset-supplement` extension.
4. **R5 `useSupplement`:** `useSupplement=<supplement-canonical>|<version>` applies the
   supplement (ZTS compatibility extension on R4).
5. **`displayLanguage`:** `displayLanguage=de-DE` selects the German display when available.
6. **`includeDesignations`:** `includeDesignations=true` adds `contains.designation`.
7. **Paging:** `offset=0&count=50`, `count=0` for total-only.
8. **Invalid parameter:** `context=...` → 400.
9. **Missing dependency:** referenced ValueSet not resolvable → 400/422.
10. **Invalid supplement:** supplement targeting another base system → 400 .

## 11. Limitations

Intentionally unsupported / deferred:

- `context`, `contextDirection`, `date`, `excludeNotForUI`, `excludePostCoordinated` — rejected.
- Historical expansion (`date`) — not supported.
- Full multi-supplement merge and display-selection precedence from supplements — deferred.
- Dedicated timeout/overload/unavailable mapping — deferred.
- `expansion.timestamp` uses the system clock (not yet an injected clock).
- SNOMED CT restricted mode (`snomed.restrictedMode=true`): ValueSets provided within the request
  must not use hierarchy-based filters (`constraint`, `parent`, `child`, `concept is-a`,
  `concept descendent-of`) → 400. `concept in` remains available; stored ValueSets are not
  affected. SNOMED CT expressions are not supported in restricted mode.

## 12. Test and Compliance Notes

- **Test categories:** unit tests (`core`, `fhir`), integration tests (`testing` module,
  `ExpandExpectedBehaviourIT` and siblings).
- **Traceability:** `documentation/implementation/expand-traceability.md`.
- **ZTS deviations from base FHIR:** explicit `valueSetVersion` mandatory with `url`; cyclic
  ValueSet dependencies rejected; `exclude-system`/`activeOnly`/`excludeNested` supported;
  `date`/`context*`/`excludeNotForUI`/`excludePostCoordinated` rejected.
- **Known ambiguities and resolutions:** see `expand-implementation-plan.md` §4 (Open decisions).
