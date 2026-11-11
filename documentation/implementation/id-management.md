# ID-Management

## Grundlagen
Wir unterscheiden folgende IDs:

- Resource-Id (Resource.id) - The logical id of the resource, as used in the URL for the resource. Once assigned, this value never changes. The only time that a resource does not have an id is when it is being submitted to the server using a create operation.
- Version-Id (Resource.meta.versionId) - Changes each time the content of the resource changes. Can be referenced in a resource reference. Can be used to ensure that updates are based on the latest version of the resource.
  The version can be globally unique, or scoped by the Logical Id of the resource. Version identifiers are generally either a serially incrementing id scoped by the logical id, or a uuid, though neither of these approaches is required. There is no fixed order for version ids - clients cannot assume that a versionId that comes after another one either numerically or alphabetically represents a later version. The same versionId can never be used for more than one version of the same resource.
  On the RESTful API: On receiving a write operation, the server SHALL update this item to the current value, or remove it. Note that servers SHOULD support versions, but some are unable to ()

## Erzeugen der Resource-Id
### fhir-Modul

Die Eingabe für die name-based UUID-Generierung setzt sich zusammen aus folgenden Elementen:
- Canonical-URL der Ressource
- Version der Ressource (falls vorhanden)
- Name des Pakets aus dem die Ressource geladen wurde
- Version des Pakets aus dem die Ressource geladen wurde

nameBasedUUID(url(-version)-packageName-packageVersion)

Wird momentan in der Klasse LoaderHelper abgebildet

```
private static String generateId(String url, String version, String packageName, String packageVersion) {

    if (url != null) {
      // Zusammenstellen der für die Genreierung einer eindeutigen ID notwendigen Informationen
      String composedId = "";
      composedId = version == null ? url : url.concat("-").concat(version);
      composedId = packageName == null ? composedId : composedId.concat("-").concat(packageName);
      composedId =
          packageVersion == null ? composedId : composedId.concat("-").concat(packageVersion);

      // Generieren einer name-based-UUID
      return UUID.nameUUIDFromBytes(composedId.getBytes()).toString();
    } else {
      // Für den Fall, dass wir keine URL haben, generieren wir eine zufällige UUID
      return UUID.randomUUID().toString();
    }
}
```

### SNOMED-Modul
???? --> ggf. für SUPPORTED work mode nicht notwendig

### LOINC-Modul
???? --> ggf. für SUPPORTED work mode nicht notwendig

## Erzeugen der Version-Id
Bisher wird diese Information nicht gesetzt