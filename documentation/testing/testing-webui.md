# Web-UI Testing

Testfälle, die ausschließlich die Web-Benutzeroberfläche (Web-UI) betreffen, erfordern spezielle Überlegungen und
Ansätze. Hier sind einige Ideen und Strategien, um Web-UI Tests effektiv durchzuführen:

- Wir denken entlang der Benutzerreise (User Journey) und identifizieren kritische Pfade, die getestet werden müssen.
-

## Metadaten

## TerminologyResource (CodeSystem/ValueSet/ConceptMap) WebUI Tests

### TerminologyResource - Überblicksseite (alle TR in einem bestimmten Scope)

Wir benötigen Tests, die die Operationen widerspiegeln, die genutzt werden, um die in einem Package enthaltenen
TerminologyResources (CodeSystems, ValueSets, ConceptMaps) abzurufen.
Prinzipiell haben wir hier verschiedene Wege dies zu tun:

- Variante 1 - Suche nach allen CS/VS/CM-Ressourcen innerhalb eines spezifischen 'Package-Version-Scope' oder
- Variante 2 - Suche über alle CS/VS/CM-Ressourcen, die aus einem bestimmten Download-URL stammen (innerhalb des '
  fhir-Scopes').

#### Variante 1 - Suche innerhalb eines spezifischen 'Package-Version-Scope'

- GET {{base_url}}/{{package_version_scope}}/{CodeSystem|ValueSet|ConceptMap}
  extra Param: _summary=count, um nur die Gesamtanzahl der CodeSystems abzurufen
- extra Param: _summary=true, um nur ausgewählte CodeSystem-Metadaten (Summary-Subset) abzurufen
- extra Param: _elements=...,...,..., um nur ausgewählte Metadaten abzurufen (einzeln angebbar)
- extra Param: _count=n (paging support)

Herausforderungen:

- Sollten im neuesten FHIR-Paket TerminologyResources entfallen sein, werden diese auch nicht mehr in der Ergebnisliste
  angezeigt. --> ggf. ist das aber auch so gewollt

#### Variante 2 - Suche über alle CS/VS/CM-Ressourcen, die aus einem bestimmten Download-URL stammen

- a) GET {{base_url}}/fhir/{CodeSystem|ValueSet|ConceptMap}?_source={{package_version_download_url}}
- b) GET {{base_url}}/fhir/{CodeSystem|ValueSet|ConceptMap}?_source:below={{package_metadata_url}}
- extra Param: _summary=count, um nur die Gesamtanzahl der CodeSystems abzurufen
- extra Param: _summary=true, um nur ausgewählte CodeSystem-Metadaten (Summary-Subset) abzurufen
- extra Param: _elements=...,...,..., um nur ausgewählte Metadaten abzurufen (einzeln angebbar)
- extra Param: _count=n (paging support)

Herausforderungen:

- Wenn mit einer konkreten Version (vollständige URL) gesucht wird, dann werden nur TerminologyResources dieser exakten
  Version gefunden. D.h. sollten im referenzierten FHIR-Paket TerminologyResources entfallen sein, werden diese auch
  nicht mehr in der Ergebnisliste angezeigt. --> ggf. ist das aber auch so gewollt
- Wenn mit einer unvollständigen URL (nur bis zum Package-URL) mit':below' Modifier gesucht wird, dann werden alle
  jemals publizierten TerminologyResources aller Versionen gefunden. D.h. es können auch TerminologyResources angezeigt
  werden, die in der aktuellsten Version des referenzierten FHIR-Pakets nicht mehr enthalten sind. --> ggf. ist das aber
  auch so gewollt

### CodeSystem - Einzelseite (Information zu einem bestimmten CS)

#### Metadaten abfragen

- GET {{base_url}}/fhir/CodeSystem?system={{canonical}}&version={{version}}
- GET {{base_url}}/{{package_version_scope}}/CodeSystem?system={{canonical}}&version={{version}}
- extra Param: _summary=true, um nur ausgewählte CodeSystem-Metadaten (Summary-Subset) abzurufen
- extra Param: _elements=...,...,..., um nur ausgewählte Metadaten abzurufen (einzeln angebbar)

#### ValueSets ermitteln, die das CodeSystem verwenden

- GET {{base_url}}/fhir/ValueSet?reference={{canonical}}
- GET {{base_url}}/{{package_version_scope}}/ValueSet?reference={{canonical}}

#### Download des CodeSystem
Direkter Abruf des CodeSystem über die ID (Read-Operation):
- GET {{base_url}}/fhir/CodeSystem/{{id}}

#### Preview des CodeSystem

- POST {{base_url}}/fhir/ValueSet/$expand (Dynamisches ValueSet erzeugen und expanden lassen)
- POST {{base_url}}/{{package_version_scope}}/ValueSet/$expand (Dynamisches ValueSet erzeugen und expanden lassen)



#### Suche im CodeSystem

- POST {{base_url}}/fhir/CodeSystem/$find-matches
- POST {{base_url}}/{{package_version_scope}}/CodeSystem/$find-matches
- POST {{base_url}}/fhir/ValueSet/$expand (Dynamisches ValueSet erzeugen und expanden lassen) + Filter definieren (
  Warnung)
- POST {{base_url}}/{{package_version_scope}}/ValueSet/$expand (Dynamisches ValueSet erzeugen und expanden lassen) +
  Filter definieren (Warnung)

## ValueSet

## ConceptMap



