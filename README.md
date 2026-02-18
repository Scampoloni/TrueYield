# TrueYield: AI-Powered ESG Verification Platform

## Die Idee in Kürze
TrueYield ist eine Web-Plattform, die Banken und Asset Managern hilft zu überprüfen, ob ihre "grünen" Investmentfonds wirklich nachhaltig sind. Die Applikation sucht mithilfe von Künstlicher Intelligenz automatisch nach Umweltskandalen (Greenwashing) bei den Firmen, die in einem Fonds enthalten sind.

## Das Problem
Wenn ein Finanzinstitut einen Fonds als "nachhaltig" (ESG-konform) verkauft, verlangen Regulatoren strenge Beweise dafür. Die manuelle Prüfung, ob jede einzelne Firma im Fonds umweltfreundlich agiert oder heimlich in Skandale verwickelt ist, ist für Compliance-Teams extrem zeitaufwendig und fehleranfällig. 

## Die Lösung & Der Workflow
TrueYield automatisiert die mühsame Recherchearbeit, überlässt die finale rechtliche Entscheidung aber einem menschlichen Prüfer. Der Ablauf sieht wie folgt aus:

1. Ein **Fund Manager** legt auf der Plattform ein Portfolio an und fügt die entsprechenden Firmen (Holdings) hinzu. Dann fordert er eine ESG-Prüfung (Audit) an.
2. Das System sucht über externe Schnittstellen (wie z.B. NewsAPI) automatisch nach aktuellen globalen Nachrichten zu diesen Firmen. Diese Artikel werden als Beweismaterial (Evidence) gespeichert.
3. Die Künstliche Intelligenz (Spring AI) liest diese Nachrichten, bewertet, ob sie negativ oder positiv für die Umwelt sind, und fasst die Risiken für das gesamte Portfolio zusammen.
4. Ein unabhängiger **ESG Auditor** loggt sich ein, liest die Zusammenfassung der KI und sichtet bei Bedarf die Original-Nachrichten. Basierend darauf trifft er die finale Entscheidung: Er zertifiziert das Portfolio oder lehnt es ab und hinterlegt dafür zwingend eine Begründung (AuditComment).

## Datenmodell & Architektur (ER-Diagramm)
Das System nutzt eine dokumentenbasierte Datenbank (MongoDB) und baut auf fünf logisch verknüpften Bereichen auf:
* **Portfolio & Holding:** Das Grundgerüst des Fonds. Das Portfolio gehört dem Fund Manager und enthält mehrere Holdings (die einzelnen Firmen/Aktien).
* **AuditReport:** Der eigentliche Prüfauftrag. Dieser durchläuft verschiedene Zustände, damit man immer weiss, wo man steht (Entwurf -> KI analysiert -> Warten auf Prüfer -> Zertifiziert/Abgelehnt).
* **Evidence:** Die von der API abgerufenen Nachrichtenartikel, die der KI als Entscheidungsgrundlage dienen.
* **AuditComment:** Die obligatorische Notiz des Auditors, die erklärt, warum ein Fonds abgelehnt oder akzeptiert wurde.

## Use Case Diagram
![Use Case Diagram](doc/uc-diagram.drawio.svg)

## Entity-Relations Diagram
![ER Diagram](doc/er-diagram.drawio.svg)


## Mehrwert für die Kunden
TrueYield nimmt den Compliance-Teams das stundenlange Lesen von Nachrichtenartikeln ab. Das spart massiv Zeit, senkt die Kosten für die Zertifizierung und schützt die Bank vor Strafen, weil Greenwashing-Risiken durch die KI systematisch aufgedeckt werden.