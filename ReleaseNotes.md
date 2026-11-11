<img align="right" width="250" height="47" src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" /> <br />     

# Release Notes termzilla

## Release 1.1.0 (2026-10)

### added

- Operation `$expand` for ValueSets
- Use activeOnly-parameter or ValueSet.compose.inactive to filter inactive concepts
- SNOMED CT restricted mode (`snomed.restrictedMode`): only concepts, descriptions, language refsets
  and refset membership are provided; concept relationships, the definition status and
  expressions are not available, and ValueSets provided within a request must not use
  hierarchy-based filters. Restricted mode is enabled by default; set `SNOMED_RESTRICTEDMODE=false`
  to provide the full SNOMED CT content

### changed

- changed package name from "de.fraunhofer.fokus.espri.ehealth" to "de.gematik.zts"
- SNOMED CT `CodeSystem/$validate-code` returns `result=false` for unknown codes instead of a 400
  error when expression processing is disabled
- Updated Docker Image
- Updated dependencies

## Release 1.0.0 (2026-08)

### added

- initial release
- Java 25
- Added parallel loading of the terminology-providers
- Extended Project with OSPO resources
