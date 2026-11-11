# Caching von Request/Response

## Grundlagen

### Allgemeines

Bevor wir in die FHIR-Spezifika eintauchen, schauen wir uns das Fundament an. Beim HTTP-Caching geht es im Kern um zwei
Fragen:

Ist die Antwort noch "frisch"? (Freshness)
Wenn sie nicht mehr frisch ist, hat sie sich wirklich geändert? (Validation)

Hier ist der Überblick über die Akteure und die wichtigsten Header.
Das Zusammenspiel: Client, Proxy und Server

Wenn wir von "vorgelagerten Proxies" sprechen (wie Nginx oder CDNs), fungieren diese als Shared Caches.
Das Ziel ist, dass der Proxy die Antwort liefert, ohne dass der HAPI-Server überhaupt davon erfährt.

### Die wichtigsten Caching-Header

Cache-Control
Das ist der wichtigste Header. Er gibt Anweisungen, wer wie lange was speichern darf.
public: Die Antwort darf von jedem Cache (auch Proxies) gespeichert werden.
private: Nur der End-Browser darf cachen (wichtig bei sensiblen Patientendaten, bei Terminologie aber meist public).
max-age=[Sekunden]: Bestimmt die Lebensdauer (Freshness).
s-maxage=[Sekunden]: Speziell für Shared Caches (Proxies). Überschreibt max-age.
no-cache: Zwingt den Cache, vor der Auslieferung beim Server nachzufragen (Validierung), ob die Daten noch aktuell sind.

ETag (Fingerabdruck-ID)
Der Server sendet einen Hashwert oder eine Versionskennung der Ressource mit.
Ablauf: Der Client/Proxy sendet beim nächsten Mal den Header If-None-Match: "hash123".
Ergebnis: Wenn der Hash auf dem Server noch gleich ist, antwortet der HAPI-Server nur mit einem winzigen 304 Not
Modified, statt die ganze Ressource (z.B. ein riesiges ValueSet) erneut zu schicken.

Last-Modified & If-Modified-Since
Ähnlich wie der ETag, aber zeitbasiert. Der Server sagt, wann die Ressource zuletzt geändert wurde.
Der Proxy fragt: "Hast du was Neueres als Datum X?"

Vary (Der "Es kommt drauf an"-Header)
Dieser Header ist für FHIR extrem wichtig! Er sagt dem Proxy: "Cache diese Antwort nur dann, wenn auch dieser
spezifische Request-Header gleich ist.
"Vary: Accept, Accept-Encoding: Da FHIR-Server oft JSON oder XML liefern, verhindert dieser Header, dass ein Proxy einem
XML-anfragenden Client versehentlich ein gecachtes JSON schickt.
--> Das Thema haben wir auch nochmal beim Umgang mit language bezogenen Headern, da diese auch Einfluss auf die
Operation haben kann ...

Expires (Der "Oldschool"-Weg)
Er gibt ein festes Datum/Uhrzeit an. Da Cache-Control: max-age flexibler ist, wird Expires heute meist nur noch
als Fallback mitgeliefert.

Zusammenfassung der StrategienStrategieeaderZielExpirationsbasiert
Cache-Control: max-age=3600 -> Vermeidet jeglichen Kontakt zum Server für 1 Std.Validierungsbasiert
ETag / Last-Modified: Reduziert Bandbreite (nur 304er Antwort), benötigt aber Server-Kontakt.

Ein kleiner Hinweis am Rande: Terminologiedaten ändern sich selten, sind aber oft groß. Das macht sie zum perfekten
Kandidaten für aggressives Caching mit langen max-age-Zeiten! Anders sieht es jedoch bei den dynamischen Operationen
aus ...

### Eingebaute HAPI Features

Was HAPI FHIR bereits "out-of-the-box" mitbringt

Der HAPI RestfulServer besitzt eine eingebaute Logik für Conditional Requests (Validierung).

ETag-Generierung: Wenn die Ressource, die der Resource Provider zurückgibt, eine versionId im meta-Element hat, erzeugt
HAPI automatisch den ETag-Header (Format: W/"[versionId]").

Last-Modified-Generierung: Wenn das lastUpdated-Feld in den Metadaten gefüllt ist, setzt HAPI den Last-Modified-Header.

304 Not Modified Logik:
Wenn ein Client einen Request mit If-None-Match (ETag) oder If-Modified-Since sendet, vergleicht
HAPI diese Werte automatisch mit der Ressource, die dein Provider zurückgibt. Wenn sie übereinstimmen, bricht HAPI die
Serialisierung ab und sendet sofort einen HTTP 304 Statuscode ohne Body. Das spart CPU-Last für das Rendern von
JSON/XML.

### Fehlende Funktionalität

HAPI FHIR setzt standardmäßig keine Cache-Control: max-age=... Header, da der Server nicht wissen kann, wie volatil
unsere Daten sind. Für die Strategie mit vorgelagerten Proxies ist dies aber der wichtigste Schritt.

Da wir dies wahrscheinlich global oder pro Ressourcentyp steuern wollen, ist ein HAPI Interceptor der sauberste Weg in
Spring Boot

@Interceptor
public class CachingInterceptor {

@Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
public void handleOutgoingResponse(RequestDetails theRequest, HttpServletResponse theResponse) {
// Nur bei erfolgreichen GET-Requests cachen
if (theRequest.getRequestType() == RequestTypeEnum.GET) {
// Beispiel: Terminologie-Ressourcen 1 Tag cachen
if (theRequest.getResourceName().equals("ValueSet") ||
theRequest.getResourceName().equals("CodeSystem")) {
theResponse.addHeader("Cache-Control", "public, max-age=86400, s-maxage=86400");
theResponse.addHeader("Vary", "Accept, Accept-Encoding");
}
}
}
}

Wichtig: Darauf achten, dass Vary: Accept, Accept-Encoding gesetzt ist, damit der Proxy JSON-Anfragen nicht mit
gecachtem XML beantwortet und umgekehrt

## Szenarien

- GET-Operationen sollten relativ problemlos realisierbar sein.
- POST-Operationen sind etwas problematischer, da sich Clients und Caches hier eh nicht um das Caching kümmern

### Szenario A - GET-Operationen

Vorüberlegungen:

- GET ist nicht gleich GET! Auch hier haben wir verschiedene Szenarien und Besonderheiten
  --> HAPI-Read Operationen sind stabil und hier haben wir maximal einen Bezug zum verfügbaren Datenbestand und der zum
  Einsatz kommenden Version
  --> HAPI-Search ist potentiell etwas problematischer, da wir hier auch mit Paging leben müssen und die Verfügbarkeit
  der Pages über die Cache-Lebenszeit ggf ein Problem ist
  --> HAPI-Operations ist relativ stabil und wir haben hier neben dem Bezug zum verfügbaren Datenbestand und der
  Softwareversion eigentlich nur noch das Problem bestimmter hhtp-Header, die auch als Parameter interpretiert werden
  können (lösbar über Vary)

Lösungsansätze:

Ansatz A - Wir überlassen HAPI FHIR die Arbeit, müssen dann aber damit leben, dass wir trotzdem Datenobjekte
durchleiten.

1. setzen vom Resource.meta.versionId --> diese nutzt HAPI FHIR für das Erzeugen eines weak ETags
   1a. Wir müssen uns noch Gedanken zur Erzeugung der VersionId machen: vermutlich kann das zum Zeitpunkt des
   Backside-Loadings passieren. Relevante Eingabeparameter für FHIR modul:
   -> TermZilla-Version, Package-Name, Package-Version, Ressource-url, Resource-version
2. setzen vom Resource.meta.lastUpdated --> diese Info nutzt HAPI, um den Last-modified Header zu setzen

Zumindest für die Read-Operation können wir diese Informationen bereits beim Laden der Terminologie-Ressourcen setzen.
Entweder wird das der Zeitpunkt des Ladens oder der Zeitpunkt der Paketerstellung ....

Bewertung -> Mit geringem Aufwand umsetzbar aber keine gute allgemeine Lösung. Wir sollten trotzdem so starten und
darauf aufbauend weitere Mechanismen etablieren ...

Ansatz B - Wir implementieren in den Resource-Providern oder irgendwelchen Interceptoren unsere eigene Logik

Dafür müssen wir auf die Request-Informationen zugreifen und den Header selbst auswerten und vermutlich einen kleinen
Cache-halten oder Hashes dynamisch über die verschiedenen Parameter berechnen und dann vergleichen.

ACHTUNG: Nochmal prüfen! Wie sieht das mit dem automatischen Setzen von E-Tags aus, wenn wir eine Expand-Operation über
eine valueSetURL aufrufen? Wird trotzdem die aus dem VS genommen, obwohl wir vielleicht eine ganz andere Expansion
haben? Ist das ein Problem?

Abläufe Szenarien:

@startuml
skinparam ParticipantPadding 20
skinparam BoxPadding 10

title HTTP Caching Szenarien für HAPI FHIR ($lookup)

participant "Client (Browser/App)" as Client
participant "CDN / Proxy " as CDN
participant "HAPI FHIR Service" as HAPI

== 1. Initialer Request (Cache Miss) ==
Client -> CDN : GET /CodeSystem/$lookup?code=A
note right of CDN: Cache leer
CDN -> HAPI : GET /CodeSystem/$lookup?code=A
HAPI -> HAPI : Berechne ETag (Caffeine Key-Check)
HAPI -> CDN : 200 OK\nETag: W/"hash123"\nCache-Control: public, s-maxage=3600\nVary: Accept
CDN -> CDN : Speichere Response (Key: URL)
CDN -> Client : 200 OK\nETag: W/"hash123"

== 2. Folgerequest: Noch frisch (Cache Hit) ==
Client -> CDN : GET /CodeSystem/$lookup?code=A
note right of CDN: s-maxage noch nicht abgelaufen
CDN -> Client : 200 OK (aus Cache)\nETag: W/"hash123"\nAge: 500
note left: HAPI Service wird nicht kontaktiert

== 3. Folgerequest: Nicht mehr frisch, aber noch valide (Revalidation) ==
note over CDN: Zeit vergeht... (s-maxage abgelaufen)
Client -> CDN : GET /CodeSystem/$lookup?code=A
CDN -> HAPI : GET /... (Conditional Request)\nIf-None-Match: W/"hash123"
HAPI -> HAPI : ETag Check (Caffeine: "hash123" == "hash123")
HAPI -> CDN : 304 Not Modified + (nochmal ETag, Cache-Control + Vary Header)
note right of CDN: Cache wird aufgefrischt
CDN -> Client : 200 OK (aus Cache)\nETag: W/"hash123"

== 4. Folgerequest: Nicht mehr valide (Resource geändert) ==
note over HAPI: Terminologie-Update erfolgt
Client -> CDN : GET /CodeSystem/$lookup?code=A
CDN -> HAPI : GET /...\nIf-None-Match: W/"hash123"
HAPI -> HAPI : ETag Check (Caffeine: "hash123" != "hash456")
HAPI -> CDN : 200 OK\nETag: W/"hash456"\nCache-Control: ...
CDN -> CDN : Überschreibe Cache mit neuen Daten
CDN -> Client : 200 OK\nETag: W/"hash456"

@enduml

### Szenario B - POST-Operationen

- das müssen wir auf zwei Ebenen umsetzen, (1) ausserhalb des Fachkontextes mit IO-Unterstützung und (2) innerhalb des
  Fachkontextes mit in-memory-Unterstützung
- das IO-Feature sollte ggf auch an und abschaltbar sein

## Anpassungsbedarfe am TermZilla

- Jedes Content-Modul benötigt an der Schnittstelle eine Fingerprint-Operation, die einen Hash über den geladenen
  Gesamtdatenbestand zurückliefert. Haben sich die geladenen Inhalte zwischen zwei Serverstarts nicht verändert, wird
  derselbe Hash zurückgegeben.
- Der TermZilla als Solches benötigt ebenfalls einen Versionshash, der sich ändert, sobald Änderungen am Quelltext
  vorgenommen wurden, da diese natürlich auch die nach Außen weitergereichten Inhalte beeinflussen können. Vermutlich
  muss hier aber auch noch die Konfiguration und die verwendete JRE in die Hashwertberechnung einfließen, da hierüber
  auch das Systemverhalten beeinflusst werden kann (z.B. Anzahl zurückgegebener Konzepte bei Expand, Page Sizes usw.)
- Alternativansatz für schnelle Umsetzung: Jeder Serverneustart erzeugt einen neuen Fingerprint, der in die ETag-Bildung mit einfliesst. D.h. wir müssen diesen Fingerprint aber auch in die Module hineinreichen damit diese die versionId setzen können --> Schnelle
  Lösung, aber nicht ressourcenschonend, da CDN sich dann neu füllen muss

## Offene Punkte

- Wir müssen auch bei Parameter-Objekten die Version-Id und ggf. andere Angaben setzen, um sinnvoll mit Caching umgehen
  zu können
- Wir müssen bei ValueSet-Objekten ggf. die bereits gesetzten Versions und Zeitangaben wieder neu anpassen ...
- Klar kommunizieren, dass wir GET-Operationen vor POST-Operationen bevorzugen, da sich dan das CDN um Caching kümmert
  und das bestimmt besser als wir das jemals könnten
- Wir müssen die max-age-Zeit über die Konfiguration setzen können
- Accept-language Header kann grundsätzlich ein Ersatz für den language-Parameter sein. Das müssen wir auch noch
  mitbetrachten
- Wie gehen wir mit unserem PagingProvider um? Das kann hier eklige Szenarien geben, in denen im Cache nicht
  gespeicherter aber über Cacheeinträge verlinkter Content nicht mehr korrekt geladen werden kann ...
- --> Cache vergrößern und dafür sorgen, dass wir den Eintrag wieder vorne an den Buffer schreiben, wenn ein
  If-None-Match gesendet wird?!
- Ids und versionIds für SNOMED und LOINC-basierte Ressourcen generieren/setzen
- Wie können wir zur Laufzeit die Version des TX berechnen?
- Wie können wir zum Systemstart einen Hash über den Gesamtdatenbestand berechnen