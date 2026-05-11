# TrueYield – KI-gestützte ESG-Verifikation gegen Greenwashing

![Backend CI](https://github.com/Scampoloni/trueyield/actions/workflows/ci.yml/badge.svg?branch=main)
![Coverage](https://github.com/Scampoloni/trueyield/raw/main/.github/badges/jacoco.svg)

Finanzinstitute verkaufen Fonds als «nachhaltig» — doch die regulatorisch geforderte Prüfung auf Greenwashing ist manuell, langsam und fehleranfällig. TrueYield löst das: Eine KI-gestützte Plattform analysiert automatisiert globale Nachrichtenquellen, bewertet ESG-Risiken und liefert Auditoren eine revisionssichere Entscheidungsgrundlage. Der Zeitpunkt ist jetzt, weil EU-Regulierungen (SFDR, EU-Taxonomie) seit 2021 scharfe Nachweispflichten fordern und Greenwashing-Bussen in Milliardenhöhe drohen.

## Testing & Coverage

- Backend CI führt `mvn verify` aus (inkl. Unit- und Integrationstests mit Testcontainers).
- JaCoCo HTML-Report wird unter `backend/target/site/jacoco/index.html` erzeugt.
- Coverage-Gate: Build failt wenn Instruction Coverage der Core Services (`PortfolioService`, `HoldingService`, `AuditReportService`, `AuditCommentService`, `EvidenceService`, `UserService`, `ComplianceService`) unter **90%** fällt.
- Aktueller Stand: alle Core Services ≥ 90% abgedeckt (Gate aktiv und grün). Gesamt-Projektabdeckung inkl. Controller, Modelle und Konfigurationsklassen: siehe Badge oben (~94%).

## Deployment

Deployment ist aktiv auf Azure App Service (Docker + GitHub Actions). Erfolgreiche Runs sind als GitHub Actions Screenshots dokumentiert.

**Deployment-Logs (GitHub Actions, 2026-04-18):**
- Run Overview: ![CD Run Overview](doc/deployment/deploy-2026-04-18-gh-actions-run-overview.png)
- Backend Build & Push: ![Backend Build](doc/deployment/deploy-2026-04-18-gh-actions-backend-build.png)
- Backend Deploy: ![Backend Deploy](doc/deployment/deploy-2026-04-18-gh-actions-backend-deploy.png)
- Frontend Build & Push: ![Frontend Build](doc/deployment/deploy-2026-04-18-gh-actions-frontend-build.png)
- Frontend Deploy: ![Frontend Deploy](doc/deployment/deploy-2026-04-18-gh-actions-frontend-deploy.png)

| Service | URL |
|---------|-----|
| Frontend | https://trueyield-frontend.azurewebsites.net |
| Backend | https://trueyield-backend.azurewebsites.net |
| API Health | https://trueyield-backend.azurewebsites.net/actuator/health |

## Inhaltsverzeichnis
- [Einleitung](#einleitung)
    - [Explore-Board](#explore-board)
    - [Create-Board](#create-board)
    - [Evaluate-Board](#evaluate-board)
    - [Feedback aus Pitch und Board-Beurteilung](#feedback-aus-pitch-und-board-beurteilung)
- [Anforderungen](#anforderungen)
    - [Use Case Diagram](#use-case-diagram)
    - [AuditStatus State Machine](#auditstatus-state-machine)
    - [Use-Case Beschreibungen](#use-case-beschreibungen)
    - [UI-Mockup](#ui-mockup)
    - [Entity-Relations Diagram](#entity-relations-diagram)
- [Implementation](#implementation)
    - [Auth0-Konfiguration](#auth0-konfiguration)
    - [API-Dokumentation](#api-dokumentation)
    - [End-to-End Tests (Cypress)](#end-to-end-tests-cypress)
    - [KI-Integration (Spring AI)](#ki-integration-spring-ai)
    - [MCP Server — ESG Tools](#mcp-server--esg-tools-anforderung-22)
    - [Frontend](#frontend)
    - [Umgesetzte optionale Anforderungen](#umgesetzte-optionale-anforderungen)
- [Fazit](#fazit)
- [Backlog & Nächste Schritte](#backlog--nächste-schritte)

---

## Einleitung

TrueYield ist eine KI-gestützte ESG-Verifikationsplattform, die Banken und Asset Managern hilft, Greenwashing in Investmentfonds systematisch aufzudecken. Die Plattform kombiniert automatisierte News-Analyse mit einem auditierbaren Human-in-the-Loop-Workflow: KI liefert Evidenz und Risk-Scores, der Mensch trifft die finale Entscheidung. So wird ESG-Compliance schneller, günstiger und regulatorisch belastbar.

## Explore-Board

### TRENDS & TECHNOLOGIE
- **ESG-Regulierung verschärft sich drastisch:** EU SFDR seit März 2021 verbindlich, technische Standards ab Januar 2023 verpflichtend für alle Finanzmarktteilnehmer mit >500 Mitarbeitern
- **EU-Taxonomie definiert "nachhaltige Wirtschaftstätigkeiten"** ab 2022, zwingt Asset Manager zu detaillierter Offenlegung der Taxonomie-Konformität ihrer Portfolios
- **Greenwashing-Skandale eskalieren:** DWS zahlte $19M (SEC, 2023) + €25M (Frankfurt, 2025) — grösste Greenwashing-Strafe gegen Asset Manager in der Geschichte
- **SFDR-Überarbeitung 2025:** Einführung verbindlicher Produktkategorien statt freiwilliger Labels (Artikel 8/9) — Regulierung wird schärfer, nicht lockerer
- **KI-gestützte Compliance-Automatisierung nimmt zu:** Finanzinstitute investieren massiv in NLP und Machine Learning für regulatorische Prozesse
- **Real-Time-News-APIs ermöglichen kontinuierliches Monitoring:** NewsAPI, Bloomberg Terminal, Reuters bieten Echtzeit-Zugang zu globalen Nachrichtenquellen
- **Compliance-Kosten explodierten seit Finanzkrise:** Retail- und Corporate-Banken melden über 60% höhere Compliance-Betriebskosten vs. Pre-2008-Niveau
- **99% der US/CA-Finanzinstitute berichten steigende Compliance-Ausgaben** (LexisNexis 2024), Trend setzt sich fort
- **Schweizer FINMA verschärft ESG-Oversight ab 2022:** Auch in der Schweiz steigt regulatorischer Druck auf Asset Manager
- **Institutionelle Anleger verlangen Nachweise:** 78% bereit, höhere Gebühren für echte ESG-Fonds zu zahlen, aber nur mit transparenter Verifikation

### POTENTIELLE PARTNER & WETTBEWERB

**Strategische Partner:**
- **NewsAPI / Bloomberg Terminal / Reuters** — Globale Nachrichtendatenbanken mit ESG-relevanten Artikeln, benötigt für Evidence-Kette
- **Auth0 / Okta** — Enterprise-Authentifizierung und Rollenmanagement (Fund Manager vs. Auditor vs. Compliance Officer)
- **Microsoft Azure / AWS** — Cloud-Infrastruktur für Skalierung, Compliance-Zertifizierungen (ISO 27001, SOC 2)
- **Anthropic Claude / OpenAI** — LLM-APIs für automatisierte Sentiment-Analyse und ESG-Risiko-Klassifizierung
- **ESG-Beratungsfirmen (z.B. PwC, Deloitte Sustainability)** — Go-to-Market-Partner, haben direkte Beziehungen zu Compliance-Teams bei Banken

**Wettbewerb & Differenzierung:**
- **MSCI ESG Ratings** — Marktführer mit $1.4Mrd Umsatz, ABER: Ratings basieren auf Fragebogen + manueller Analyse, kein Echtzeit-Monitoring, kein Audit-Workflow
- **Sustainalytics (Morningstar)** — Starke Research-Coverage, ABER: Scores werden quartalsweise aktualisiert, keine automatisierte Greenwashing-Erkennung
- **ISS ESG (Institutional Shareholder Services)** — Fokus auf Proxy Voting + Corporate Governance, ABER: Kein Tool für kontinuierliches Portfolio-Monitoring
- **RepRisk** — News-basiertes Risiko-Screening, ABER: Keine Integration in Audit-Workflows, keine Human-in-the-Loop-Validierung

**TrueYield's Differenzierung:**
- Einzige Lösung die News-Monitoring + KI-Analyse + auditierbaren Workflow + menschliche Finalentscheidung kombiniert
- Echtzeit-Alerts statt quartalsweiser Updates
- Revisionssichere Evidence-Kette (jede News-Quelle ist nachvollziehbar)
- SFDR/EU-Taxonomie-konform durch Human-in-the-Loop (Regulatoren akzeptieren keine rein automatischen Entscheidungen)

### FAKTEN
- **EU SFDR:** In Kraft seit 10. März 2021, technische Standards (Level 2) ab 1. Januar 2023 verpflichtend
- **DWS Greenwashing-Strafen:** $19M SEC-Busse (Sept 2023) + €25M Frankfurt-Busse (April 2025) = insgesamt ~$46M
- **Globales ESG-Fondsvolumen:** $30 Billionen in 2022, prognostiziert auf $40 Billionen bis 2030 (Bloomberg Intelligence)
- **SFDR-Coverage:** Gilt für ~20.000 Finanzmarktteilnehmer in der EU mit >500 Mitarbeitern
- **Compliance-Kostensteigerung:** Über 60% Anstieg seit 2008-Finanzkrise (Deloitte), 99% der Institute berichten weiter steigende Ausgaben
- **ESG-Fonds-Reklassifizierungen:** 307 Fonds von Artikel 9 zu Artikel 8 herabgestuft (Sept-Dez 2022) — 40% aller damaligen Artikel-9-Fonds
- **Schweizer FINMA:** Verschärfte ESG-Aufsicht seit 2022, plant weitere Massnahmen analog zu EU-Standards
- **Greenwashing-Prävalenz:** Studie zeigte 64% der untersuchten ESG-Fonds entsprachen nicht ihren ESG-Versprechen (As You Sow, 2022)
- **Typische Portfolio-Prüfungsdauer (manuell):** Branchenberichte deuten auf mehrere Wochen für umfassende ESG-Due-Diligence hin
- **Bloomberg ESG-Daten-Gap:** Nur 60% der S&P 500-Unternehmen veröffentlichten 2020 vollständige GHG-Emissionsdaten — Datenlücken sind massiv

### USER

**Primäre Nutzergruppe 1: ESG Fund Manager**
- **Demografie:** 30-50 Jahre, BWL/Finance/Economics-Hintergrund, Master-Abschluss, 5-15 Jahre Berufserfahrung
- **Jobprofil:** Verantwortlich für Portfolio-Zusammenstellung nachhaltiger Fonds, reportet an Investment Committee
- **Arbeitskontext:**
  - Arbeitet unter Zeitdruck (Launch-Deadlines für neue Fonds, quartalsweise Reporting-Zyklen)
  - Muss 10-50 Holdings pro Portfolio kontinuierlich monitoren
  - Jongliert zwischen Performance-Druck und ESG-Compliance
- **Pain Points:**
  - Manuelle Recherche zu jedem Holding ist zeitintensiv (Google News, Bloomberg, NGO-Reports)
  - Angst vor Greenwashing-Vorwürfen und Reputationsschaden
  - Muss Compliance-Team überzeugen können, dass ESG-Claims vertretbar sind
- **Tech-Affinität:** Mittel-Hoch, nutzt täglich Bloomberg Terminal, Excel, Portfolio-Management-Software
- **Bedürfnisse:** Schnelle ESG-Freigabe ohne manuelle Recherche-Arbeit, juristisch sichere Dokumentation für Regulatoren

**Primäre Nutzergruppe 2: ESG Auditor / Compliance Officer**
- **Demografie:** 35-55 Jahre, oft CFA/CPA-zertifiziert, Legal/Compliance-Hintergrund
- **Jobprofil:** Unabhängige Prüfung von ESG-Claims, finale Freigabe-Entscheidung, Risikoabwägung
- **Arbeitskontext:**
  - Arbeitet in zentraler Compliance-Abteilung, prüft Fonds aus verschiedenen Geschäftseinheiten
  - Muss 20-30 Portfolios pro Monat/Quartal freigeben
  - Trägt persönliche Haftung bei Fehlentscheidungen (Karriererisiko bei Skandalen)
- **Pain Points:**
  - Überflutet mit Fund-Manager-Requests ("bitte schnell freigeben")
  - Schwer nachvollziehbar welche News-Quellen verlässlich sind
  - Muss Entscheidungen gegenüber Regulatoren verteidigen können
  - Keine Zeit für tiefe Recherche, aber Qualität darf nicht leiden
- **Tech-Affinität:** Mittel, nutzt Compliance-Software, oft skeptisch gegenüber reiner KI-Automation ("Black Box")
- **Bedürfnisse:** Evidenzbasierte Entscheidungsgrundlage, nachvollziehbare Beweiskette, finale Kontrolle behalten (kein Autopilot)

**Sekundäre Nutzergruppe: C-Level Compliance / Risk Officers**
- Überwachen Gesamtprozess, interessiert an KPIs (Durchlaufzeiten, Rejection-Rate)
- Brauchen Reporting für Board / Regulatoren

### POTENZIALFELDER
- **Automatisierte ESG-Compliance-Workflows:** Reduzierung manueller Arbeit um 70-80% durch KI-gestützte Vorselektion
- **Kontinuierliches Portfolio-Monitoring:** Echtzeit-Alerts bei negativen ESG-Events (statt quartalsweiser Checks)
- **Regulatorische Berichterstattung (SFDR Article 8/9):** Automatische Generierung von Template-konformen Disclosure-Reports
- **Due Diligence für M&A / Neuinvestitionen:** ESG-Screening neuer Holdings bevor sie ins Portfolio aufgenommen werden
- **White-Label-Lösungen für Tier-1-Banken:** Grosse Finanzinstitute wollen proprietäre Lösung unter eigener Marke
- **ESG-Scoring für Retail-Investoren (Phase 2):** Vereinfachte Version für Endkunden (B2C-Expansion möglich, aber nicht primäres Ziel)
- **Integration in bestehende Compliance-Suites:** APIs für SAP GRC, ServiceNow, etc.
- **Cross-Border-Compliance:** Unternehmen mit EU + US + APAC-Exposure brauchen Multi-Jurisdictions-Support

### ERKENNTNISSE
- **60-80% der Compliance-Zeit geht in manuelle Recherche:** Interviews mit ESG-Analysten zeigen: Haupttätigkeit ist Google-Suche + Bloomberg-Screening pro Holding
- **Regulatoren verlangen "Human in the Loop":** SFDR-Guidance macht klar: Rein algorithmische Entscheidungen ohne menschliche Validierung werden nicht akzeptiert
- **Der Markt will keine weiteren ESG-Ratings:** MSCI, Sustainalytics, ISS liefern bereits Scores — Problem ist nicht "Score fehlt", sondern "Score ist nicht nachvollziehbar und nicht aktuell"
- **Greenwashing entsteht oft durch Zeitverzug:** Negative ESG-Events (Umweltverstösse, Kinderarbeit-Vorwürfe) tauchen in Medien auf, BEVOR sie in ESG-Ratings reflektiert werden (Lag: 3-6 Monate)
- **Bestehende Tools liefern Scores, keine Beweisketten:** Asset Manager können gegenüber Regulatoren nicht erklären WARUM ein Fonds als "nachhaltig" gilt — fehlt an Audit Trail
- **Compliance-Teams sind unterbesetzt:** 42% der C-Suite-Zeit geht in Regulatory Affairs (Bank Policy Institute 2023) — Automatisierung ist nicht "nice to have" sondern "existenziell"
- **KI-Skepsis ist real, aber adressierbar:** Compliance-Teams trauen KI nicht blind, ABER: akzeptieren KI als "Research-Assistent" wenn finale Entscheidung beim Menschen liegt
- **SFDR-Reklassifizierungen zeigen Unsicherheit:** 40% der Artikel-9-Fonds wurden herabgestuft — zeigt dass Fund Manager selbst unsicher sind ob ihre ESG-Claims haltbar sind

### BEDÜRFNISSE
**Primäres Bedürfnis:**
Schnelle, kostengünstige und regulatorisch akzeptable ESG-Verifikation von Investmentfonds — ohne dass Compliance-Teams Wochen in manuelle Recherche investieren müssen, mit vollständiger Nachvollziehbarkeit jeder Entscheidung und lückenloser Evidence-Kette für den Audit-Trail.

**Spezifische Bedürfnisse:**
- **Zeitersparnis:** Von Wochen auf Minuten/Stunden reduzieren
- **Rechtssicherheit:** Jede Entscheidung muss vor Gericht/Regulatoren verteidigbar sein
- **Transparenz:** "Warum wurde Fonds X abgelehnt?" muss mit konkreten News-Artikeln belegbar sein
- **Kontrolle:** Finale Entscheidung muss beim Menschen bleiben (regulatorische + psychologische Anforderung)
- **Integration:** Muss in bestehende Workflows passen (nicht weiteres Silo-Tool)

### TOUCHPOINTS

> **Hinweis:** Dieser Abschnitt beschreibt den angestrebten Produktzustand (Zielzustand MVP+). Im aktuell implementierten MVP sind die Kern-Touchpoints 1–5 und 8 umgesetzt. Touchpoints 6 (E-Mail-Benachrichtigungen), 7 (Audit-PDF-Export), 9 (Admin Panel) und 10 (Support-Integrationen) sind als Backlog-Items vorgemerkt.

**1. Authentifizierung & Onboarding**
- Login über Auth0 (Universal Login) — *implementiert*
- Rolle wird automatisch aus dem JWT-Token gelesen (`fund-manager`, `auditor`, `compliance-officer`); Rollen werden in Auth0 zugewiesen, nicht vom Nutzer selbst gewählt
- SSO-Integration (Azure AD / Okta) sowie Onboarding-Tutorial *(geplant, noch nicht implementiert)*

**2. Portfolio-Dashboard (Fund Manager)**
- Übersicht aller Portfolios mit Status-Badge (Pending / Under Review / Approved / Rejected)
- "Create New Portfolio"-Button → Holdings manuell per Formular erfassen
- KPI-Cards: "Total: 5", "Pending Review: 2", "Approved: 3" — implementiert (B-10 ✅); "Average Approval Time" ist als zukünftige Kennzahl vorgesehen

**3. Portfolio-Submission-Flow (Fund Manager)**
- Holdings werden manuell per Formular (Symbol, ISIN, Name, Gewichtung) hinzugefügt
- CSV/Excel-Upload mit Drag & Drop *(geplant, noch nicht implementiert — siehe Backlog B-07)*
- Beschreibungs-Felder: Portfolio-Name, ESG-Zielsetzung, Ziel-Artikel (8 oder 9)
- Audit-Trigger → Status wechselt zu `AI_ANALYZING`

**4. Audit-Dashboard (Auditor)**
- Queue-View: Liste aller Portfolios die auf Review warten, sortiert nach Priorität/Deadline
- Filter: "Show only High-Risk", "My assigned reviews"
- Click auf Portfolio → Öffnet Detail-View

**5. Portfolio-Detail-View (Auditor) — KERN-TOUCHPOINT**
- **Header:** Portfolio-Name, Fund Manager, Submission-Date, Deadline
- **Holdings-Table:**
  - Spalten: Company Name, ISIN, Risk Score (1-10), Latest News (Count), Status
  - Risk Score visualisiert: Grün (<3), Gelb (3-7), Rot (>7)
- **Evidence-Feed (rechte Sidebar):**
  - Chronologischer News-Stream pro Holding
  - Jeder Artikel: Headline, Source, Date, Sentiment-Badge (Positive/Neutral/Negative)
  - Click auf Artikel → Full-Text-Preview mit Highlighting der ESG-relevanten Passagen
- **KI-Recommendation-Panel:**
  - "AI suggests: REJECT — 3 holdings show high ESG risk"
  - Begründung in Stichworten: "Company X: Labor violations (Bloomberg, 2024-02-15)"
- **Final Decision Buttons:**
  - "Approve Portfolio" (grün) → `PUT /api/service/auditreport/complete` → `APPROVED`
  - "Reject Portfolio" (rot) → `PUT /api/service/auditreport/reject` → `REJECTED`
  - "Request Revision" *(geplant, noch nicht implementiert — kein REVISION-Status in der State Machine; siehe Backlog B-11)*
- **Audit Trail Sidebar (collapsible):**
  - Zeigt alle Actions: "2024-03-01 10:32: Portfolio submitted by John Doe", "2024-03-01 14:15: AI analysis completed", etc.

**6. E-Mail-Benachrichtigungen** *(geplant, noch nicht implementiert — siehe Backlog B-08)*
- Fund Manager erhält E-Mail bei Statuswechsel:
  - "Portfolio XY wurde approved" (mit PDF-Attachment des Audit-Reports)
  - "Portfolio XY wurde rejected" (mit detaillierter Begründung + News-Links)
- Auditor erhält E-Mail bei neuer Submission:
  - "New portfolio awaiting your review: ABC Sustainable Fund"

**7. Audit-PDF-Export** *(geplant, noch nicht implementiert — siehe Backlog B-09)*
- Download-Button generiert PDF-Report mit:
  - Portfolio-Übersicht
  - Risk-Scores pro Holding
  - Alle verwendeten News-Quellen (vollständige Bibliografie)
  - Auditor-Entscheidung + Begründung
  - Timestamps (revisionssicher)
  - Digital signiert (optional, für behördliche Vorlage)

**8. API-Endpoints (für Integration)**
- Vollständige REST-API dokumentiert in Swagger/OpenAPI und Postman (siehe Abschnitt [API-Dokumentation](#api-dokumentation))
- Kernendpoints: Portfolios, Holdings, Evidence, AuditReport, AuditComment, Compliance — alle implementiert
- Rate Limits *(geplant, noch nicht implementiert)*

**9. Compliance Dashboard (Compliance Officer)**
- Systemweite KPIs: Portfolios / Holdings / Audit-Reports nach Status — *implementiert unter `/compliance`*
- SFDR Article 8/9 Klassifizierung pro Portfolio — *implementiert*
- Lesezugriff auf alle Portfolios und Audit-Reports — *implementiert*
- User-Management, KI-Threshold-Anpassungen *(geplant für Admin Panel, noch nicht implementiert)*

**10. Support-Touchpoints** *(geplant, noch nicht implementiert)*
- In-App Chat-Widget (Live-Support während Business Hours)
- Knowledge Base / FAQ (Self-Service für häufige Fragen)
- Dedicated Slack-Channel für Enterprise-Kunden

### WIE KÖNNEN WIR?
**Wie können wir Compliance-Teams ermöglichen, ESG-Risiken in Investmentfonds in Minuten statt Wochen zu prüfen — mit KI als Recherche-Assistent und Mensch als finalem Entscheider, sodass jede Entscheidung revisionssicher dokumentiert und vor Regulatoren verteidigbar ist?**

---

## Create-Board

### IDEEN-BESCHREIBUNG
TrueYield automatisiert die ESG-Greenwashing-Prüfung von Investmentfonds durch KI-gestützte News-Analyse in einem revisionssicheren Audit-Workflow. KI liefert Evidenz und Risk-Scores, der Auditor behält die finale Entscheidung — so wird ESG-Compliance schneller, günstiger und regulatorisch belastbar.

### ADRESSIERTE NUTZER
ESG Fund Manager und ESG Auditoren / Compliance Officers bei Banken, Asset Managern und institutionellen Investoren (Pensionskassen, Versicherungen) — primär im EU-Raum (SFDR-pflichtig), sekundär in der Schweiz und UK.

### ADRESSIERTE BEDÜRFNISSE
- **Zeitersparnis bei ESG-Due-Diligence:** Von mehreren Wochen auf Minuten/Stunden reduzieren
- **Regulatorische Absicherung:** SFDR/EU-Taxonomie-konforme Dokumentation mit Human-in-the-Loop
- **Lückenlose Nachvollziehbarkeit:** Jede Entscheidung ist mit konkreten News-Quellen belegbar (Audit Trail)
- **Schutz vor Greenwashing-Haftung:** Proaktive Erkennung von ESG-Risiken bevor sie zu Skandalen werden
- **Kontrolle behalten:** Finale Entscheidung liegt beim Auditor, nicht bei KI (psychologisch + regulatorisch wichtig)

### PROBLEME
**Problem 1: Manuelle ESG-Prüfung ist zu langsam und zu teuer**
Compliance-Teams durchsuchen hunderte Nachrichtenquellen manuell für jedes einzelne Holding. Ein Portfolio mit 30 Holdings benötigt bei manueller Recherche mehrere Arbeitstage bis Wochen. Resultat: Verzögerungen bei Fund-Launches, frustrierte Fund Manager, Opportunity Costs.

**Problem 2: Reine KI-Entscheidungen sind regulatorisch nicht akzeptabel**
SFDR-Regulierung verlangt nachweisbare menschliche Validierung. Tools die nur "ESG-Scores" liefern ohne Audit-Workflow werden von Regulatoren nicht als Compliance-Nachweis akzeptiert. Automatische Approvals ohne dokumentierte menschliche Prüfung = Regulatory Risk.

**Problem 3: Fehlende Nachvollziehbarkeit führt zu Haftungsrisiken**
Wenn ein Fonds später als "Greenwashing" entlarvt wird, muss die Bank beweisen können WARUM sie ihn zuvor als ESG-konform eingestuft hat. Ohne dokumentierte Beweiskette (welche Quellen wurden konsultiert, welche Überlegungen flossen ein?) sind Ablehnungen oder Zertifizierungen anfechtbar. DWS-Fall zeigt: Fehlende Dokumentation = Millionen-Bussen.

### IDEENPOTENZIAL

**User Value (Mehrwert für Nutzer): 8/10**

🔵🔵🔵🔵🔵🔵🔵🔵⚪⚪

*Begründung:*
- Massive Zeitersparnis (Wochen → Minuten) = direkte Kostensenkung
- Haftungsschutz (Greenwashing-Bussen vermeiden) = potenziell Millionen-Einsparungen
- Nicht 10/10 weil: Kein "lebensrettend", sondern "Geschäftsprozess-Optimierung"

**Scalability (Übertragbarkeit): 7/10**

🔵🔵🔵🔵🔵🔵🔵⚪⚪⚪

*Begründung:*
- Horizontal skalierbar: Jede Branche mit Compliance-Prüfpflicht (Versicherungen, Pensionskassen, Banken-Credit-Assessment)
- Geografisch skalierbar: EU → Schweiz → UK → US (verschiedene Regulierungen, aber ähnliche Bedürfnisse)
- Nicht 10/10 weil: B2B-Tool, primär für Finanzsektor (nicht "alle Menschen, alle Bedürfnisse")

**Feasibility (Machbarkeit): 3/10**

🔵🔵🔵⚪⚪⚪⚪⚪⚪⚪

*Begründung:*
- Technologie ist vorhanden: NewsAPI, Spring AI, Claude LLM, MongoDB, SvelteKit
- Im Projekt bereits implementiert: Portfolio-Entities, Audit-Workflow, News-Integration
- 3/10 = "Leicht umsetzbar mit aktueller Technologie"
- Nicht 1/10 weil: Regulatorische Compliance-Anforderungen, Datenschutz, Enterprise-Security erhöhen Komplexität leicht

### DAS WOW
**TrueYield generiert für jeden geprüften Fonds automatisch eine lückenlose, KI-gestützte Beweiskette aus echten, archivierten Nachrichtenartikeln — revisionssicher, auditierbar und vor Gericht verwendbar. Jede Entscheidung ist auf die Quellen zurückverfolgbar, die zum Zeitpunkt der Prüfung existierten. Kein anderes ESG-Tool bietet diese forensische Dokumentationsqualität.**

*Was macht es besonders:*
- Konkurrent MSCI: Score ohne Beweiskette
- Konkurrent Sustainalytics: Research-Report, aber keine News-Archive
- TrueYield: Jeder Artikel wird mit Headline, Source-URL, Publikationsdatum und Snippet archiviert — vollständig nachvollziehbar und auditierbar

### HIGH-LEVEL-KONZEPT
**«TrueYield ist das CARFAX für Investmentfonds — es zeigt dir die versteckte Schadens-Historie (ESG-Verstösse, Greenwashing-Risiken), bevor du investierst oder den Fonds zertifizierst.»**

*Warum funktioniert die Analogie:*
- CARFAX: Zeigt Unfallhistorie von Gebrauchtwagen anhand von Reparatur-Records
- TrueYield: Zeigt ESG-Skandal-Historie von Holdings anhand von News-Records
- Beide: Decken auf was "hidden" ist, machen Risiken transparent

### WERTVERSPRECHEN
**TrueYield reduziert den manuellen Aufwand für ESG-Compliance-Prüfungen um bis zu 80%, schützt Finanzinstitute vor Greenwashing-Strafen in Millionenhöhe und macht jede Prüfentscheidung lückenlos nachvollziehbar — durch KI als Recherche-Assistent, Mensch als Entscheider und automatische Generierung einer forensisch belastbaren Evidence-Kette.**

*Wie löst es die "Wie können wir"-Frage:*
- Zeitersparnis: KI durchsucht News in Sekunden (statt Tage manuelle Arbeit)
- Mensch bleibt Entscheider: Regulatorisch compliant (SFDR-konform)
- Revisionssicher: Jede Quelle archiviert, jede Entscheidung dokumentiert

---

## Evaluate-Board

### ASSESSMENT — Brand Fit Chart

**Bewertung auf 6 Dimensionen (jeweils 0-10 Skala):**

**1. Marktgrösse: 8/10**
- ESG-Compliance-Markt ist riesig: $30 Billionen ESG-Assets (2022), ~20.000 SFDR-pflichtige Firmen in EU
- Compliance-Ausgaben: $61 Milliarden jährlich (US/CA), €32.5Mrd Deutschland allein
- Nicht 10/10 weil: Nicht "jeder Mensch", sondern B2B-Finanzsektor

**2. Investment: 5/10**
- Cloud-Infrastruktur (Azure/AWS): ~$2.000-5.000/Monat für MVP
- LLM-API-Kosten (Claude/OpenAI): ~$1.000-3.000/Monat bei moderatem Volumen
- Sales/Marketing für Enterprise-Kunden: ~$50.000-100.000 für erste 12 Monate
- Nicht 1/10 (minimal) weil: Enterprise-Sales braucht Kapital
- Nicht 9/10 (sehr hoch) weil: Kein Hardware, keine Fab, keine Pharma-Trials

**3. Asset Fit: 7/10**
- Team hat Spring Boot, MongoDB, SvelteKit, LLM-Integration bereits implementiert
- ESG/Finance-Domain-Knowledge teilweise vorhanden (muss vertieft werden)
- Nicht 10/10 weil: Sales-Expertise für Enterprise-Kunden fehlt noch, regulatorisches Fachwissen muss aufgebaut werden

**4. Virales Potenzial: 3/10**
- B2B-Tool, keine virale Schleife (kein "Share mit Freunden")
- Word-of-Mouth in Compliance-Community möglich, aber langsam
- Konferenzen/Whitepapers wichtiger als Social Media
- Nicht 1/10 weil: Erfolgreiche Case Studies könnten Branchenpresse erreichen

**5. Neuer Kunde: 10/10**
- JEDE Bank, jeder Asset Manager, jede Pensionskasse mit ESG-Fonds ist potentieller Kunde
- SFDR gilt für >20.000 Unternehmen in EU
- Schweiz/UK/US haben ähnliche Regulations-Trends
- Grosse Addressable Market, wenig Overlap mit anderen Produkten

**6. Brand Fit: 9/10**
- "Compliance + Technologie" passt perfekt zu aktuellen Trends (RegTech-Boom)
- ESG ist politisch/gesellschaftlich relevant (Klimakrise, soziale Verantwortung)
- Nicht 10/10 weil: ESG hat 2024 auch Gegenwind (Anti-ESG-Bewegung in US), Brand muss neutral bleiben

**Durchschnitt: 7.0/10** — Starkes Business-Potenzial

### KANÄLE

**1. Direktvertrieb (B2B Sales)**
- Persönliche Ansprache von Head of Compliance, CROs bei Schweizer/EU-Banken
- Konkrete Taktik: LinkedIn-Recherche → Cold Email mit konkretem Use Case → Demo-Call
- Ziel: 5-10 Pilot-Kunden im ersten Jahr (Paid Pilots à CHF 10.000-20.000)

**2. LinkedIn Thought Leadership**
- Wöchentliche Posts zu Greenwashing-Skandalen, SFDR-Updates, Case Studies
- Targeting: Compliance Officers, ESG Managers, Risk Managers, CFA-Holder
- Content-Typen: "DWS zahlte €25M — So hätte TrueYield den Skandal frühzeitig erkannt", "SFDR-Überarbeitung 2025: Was ändert sich?"
- Ziel: Sichtbarkeit in Compliance-Community, Lead-Gen via Inbound

**3. Fachkonferenzen & Events**
- Swiss Sustainable Finance Forum (Zürich), CFA Institute ESG Investing Conference (London)
- Compliance Summit Europe, RegTech Summit
- Taktik: Speaking Slots ("How AI transforms ESG Compliance"), Booth für Demos
- Ziel: Face-to-Face mit Entscheidern, Trust-Building

**4. Partnerschaften mit ESG-Beratungen**
- Kooperationen mit PwC, Deloitte Sustainability, EY Climate Change & Sustainability
- Taktik: TrueYield als "Powered by"-Tool in deren Compliance-Offerings
- Ziel: Zugang zu deren Kundenstamm, Co-Selling

**5. Content Marketing & SEO**
- Whitepaper: "The True Cost of Manual ESG Compliance" (mit Benchmark-Daten)
- Case Studies: "How Bank X reduced ESG audit time by 75%"
- Blog: "SFDR Compliance Checklist 2025", "Greenwashing Detection Best Practices"
- SEO für Keywords: "ESG compliance tool", "SFDR audit software", "Greenwashing detection"

**6. Demo-Environment & Free Trial**
- Live-Demo auf Website: Upload einer Beispiel-Portfolio-CSV, sofortiges Ergebnis
- 14-Tage Free Trial für qualifizierte Leads (nach Sales-Call)
- Ziel: Product-Led-Growth (wenn Tool überzeugt, kaufen sie)

### UNFAIRER VORTEIL

**1. Proprietäre Evidence-Datenbank wächst mit jeder Prüfung**
- Jeder geprüfte Fonds = neue News-Artikel werden archiviert und indexiert
- Nach 12 Monaten: TrueYield hat 10.000+ ESG-relevante News-Artikel strukturiert
- Konkurrenten müssen bei Null anfangen, können diesen Datenvorsprung nicht aufholen
- Moat: Historische Daten sind wertvoll (Trendanalyse, Pattern Recognition)

**2. Human-in-the-Loop ist regulatorisch erforderlich, Wettbewerber bieten nur Scores**
- SFDR verlangt nachweisbare menschliche Validierung
- MSCI, Sustainalytics liefern "Scores", aber keinen Audit-Workflow
- TrueYield ist das einzige Tool das regulatorische Anforderung erfüllt UND Zeit spart
- Moat: Regulierung ist Barrier-to-Entry für reine KI-Lösungen

**3. Kombinierter Tech-Stack ist schwer zu replizieren**
- Multi-Provider-News-Integration (Guardian, NewsAPI.org, Newsdata.io) + Spring AI + Audit-Workflow + RBAC + SFDR-Scoring
- Einzelne Komponenten sind verfügbar, aber die Integration ist Custom-Built
- Konkurrent müsste 12-18 Monate investieren um ähnliches System zu bauen
- Moat: First-Mover-Advantage in spezifischem Nischen-Workflow

**4. Enterprise-Sales-Beziehungen als Barrier**
- Sobald TrueYield bei 5-10 Banken etabliert ist: Switching Costs sind hoch (Training, Integration)
- Compliance-Tools werden nicht alle 6 Monate gewechselt (3-5 Jahre Verträge üblich)
- Moat: Customer Lock-In durch Integration in bestehende Workflows

### KPI

**1. Anzahl geprüfter Portfolios pro Monat**
- Target: 50 Portfolios/Monat nach 12 Monaten
- Benchmark: 10 Portfolios/Kunde/Monat bei 5 Kunden

**2. Durchschnittliche Prüfzeit pro Portfolio**
- Target: <30 Minuten (vs. Branchendurchschnitt >2 Wochen)
- KPI zeigt Effizienzgewinn, wichtig für ROI-Berechnung

**3. Anzahl aufgedeckter Greenwashing-Risiken**
- Target: 15-20% der Portfolios zeigen mindestens 1 High-Risk-Holding
- Zeigt: Tool funktioniert (findet Probleme, die manuell übersehen würden)

**4. Anzahl aktiver Enterprise-Kunden**
- Target: 5 Paid Pilots im Jahr 1, 15-20 Kunden im Jahr 2
- Benchmark: Annual Recurring Revenue (ARR) von CHF 500.000 im Jahr 2

**5. Audit-Abschlussrate (APPROVED vs. REJECTED vs. REVISION)**
- Target: 70% Approved, 20% Revision, 10% Rejected
- Zeigt: Balanciertes System (nicht zu lasch, nicht zu streng)

**6. Customer Retention Rate (Monthly/Annual)**
- Target: >90% Retention nach 12 Monaten
- Zeigt: Kunden bleiben, weil Tool Wert liefert

**7. Net Promoter Score (NPS)**
- Target: NPS >40 (in B2B-SaaS als "gut" betrachtet)
- Frage: "Würden Sie TrueYield einem Kollegen empfehlen?"

**8. Time-to-Value (Onboarding → Erste Prüfung)**
- Target: <1 Woche von Contract Signing bis erstes Portfolio geprüft
- Zeigt: Schnelles Onboarding = weniger Churn

### REVENUE STREAM (EINNAHMEQUELLEN)

**1. B2B SaaS-Abonnement (Primäre Revenue)**
- **Tier 1 (Small Asset Manager):** CHF 2.000-3.000/Monat
  - 1-50 Portfolios/Monat, 3 User-Seats, Standard-Support
- **Tier 2 (Mid-Size Bank):** CHF 8.000-12.000/Monat
  - 50-200 Portfolios/Monat, 10 User-Seats, Priority-Support, API-Access
- **Tier 3 (Enterprise / Tier-1-Bank):** CHF 25.000-50.000/Monat
  - Unlimited Portfolios, Unlimited Users, Dedicated Account Manager, Custom Integrations
- **Preisgestaltung:** Jährliche Verträge (10% Discount vs. monatlich), Quartalsweise Abrechnung

**2. Pay-per-Audit (Sekundäre Revenue)**
- CHF 200-500 pro Portfolio-Prüfung
- Für kleinere Institute die nur 5-10 Prüfungen/Jahr brauchen (kein Abo lohnt sich)
- Credits-System: Kunde kauft "100 Audit-Credits" für CHF 15.000

**3. API-Zugang (Add-On Revenue)**
- CHF 1.000-2.000/Monat zusätzlich zu Base-Subscription
- Für Kunden die TrueYield in eigene Systeme integrieren wollen (SAP, ServiceNow)
- Rate Limits: 10.000 API-Calls/Monat (Enterprise: Unlimited)

**4. Professional Services (Einmalige Umsätze)**
- Custom-Integration: CHF 10.000-30.000 einmalig
- Training/Workshops: CHF 2.000-5.000 pro Schulungstag
- White-Label-Setup: CHF 50.000-100.000 einmalig

**5. White-Label-Lizenzierung (Strategische Revenue)**
- Grosse Finanzinstitute (z.B. UBS, Credit Suisse) lizenzieren TrueYield unter eigener Marke
- CHF 200.000-500.000/Jahr + Rev-Share (5-10% der Subscription-Umsätze die damit generiert werden)
- Nur für Top-3-Banken pro Region (Exklusivität erhöht Wert)

**6. Zukünftige Revenue-Streams (Phase 2)**
- Data-as-a-Service: Aggregierte ESG-Risiko-Insights (anonymisiert) an Research-Firmen verkaufen
- Retail-Investor-Version: Vereinfachte App für Endkunden (CHF 10-20/Monat)
- Regulatory Reporting Service: Automatische SFDR-Template-Generierung (Add-On CHF 500/Monat)

### VALUE PROPOSITION SCORE

**1. Nutzer aktivieren (0-10): 7/10**
- **Wie viele Zielkunden würden es ausprobieren?**
- Compliance-Teams haben hohen Leidensdruck (manuelle Arbeit, Regulierungs-Druck)
- Demo-Calls zeigen sofort ROI (Zeitersparnis visualisiert)
- Nicht 10/10 weil: Enterprise-Sales ist langsam (6-12 Monate Sales-Cycle), nicht jeder testet sofort

**2. Präferenz gegenüber Substitutionsprodukten (0-10): 8/10**
- **Ist TrueYield besser als MSCI, Sustainalytics, ISS?**
- Ja, weil: Einziger Audit-Workflow, einzige Evidence-Kette, einzige Human-in-the-Loop-Compliance
- Nicht 10/10 weil: Etablierte Brands (MSCI) haben Vertrauensvorsprung, höhere Anfangskosten vs. reine Rating-Services

**3. Kaufbereitschaft (0-10): 8/10**
- **Würden Kunden dafür zahlen?**
- Ja, weil: Greenwashing-Bussen kosten Millionen (DWS: $46M), TrueYield kostet CHF 50.000-100.000/Jahr → ROI ist klar
- 78% der Institutionen bereit, für echte ESG-Tools Premium zu zahlen
- Nicht 10/10 weil: Budget-Zyklen bei Banken sind jährlich, Procurement-Prozesse sind lang

### EMOTION & WEITEREMPFEHLUNG

**Emotion (0-10): 5/10**
- **Wie emotional ist die Bindung zum Produkt?**
- B2B-Compliance-Tool → eher rational als emotional
- Compliance Officers schätzen Sicherheit/Zuverlässigkeit, nicht "Freude"
- Positive Emotion: "Relief" (Erleichterung dass manuelle Arbeit wegfällt)
- Nicht 8-10 weil: Kein Consumer-Produkt, keine Lifestyle-Brand

**Weiterempfehlung (0-10): 7/10**
- **Würden User es weiterempfehlen?**
- Ja, weil: Gute Compliance-Tools sprechen sich in der Branche herum (Konferenzen, LinkedIn)
- Compliance-Community ist eng vernetzt (CFA-Holder kennen sich untereinander)
- Nicht 10/10 weil: Vertraulichkeit (Banken geben nicht gerne Details ihrer Compliance-Prozesse preis)

---

## Feedback aus Pitch und Board-Beurteilung

Dieses Kapitel dokumentiert zusammengefasstes Peer-Feedback (anonymisiert) und leitet daraus konkrete Konsequenzen für TrueYield ab.

### Wichtigste Punkte aus dem Pitch-Feedback

1. Unklarheit bei Datenbeschaffung für KI (API vs. Scraping, Datenpipeline).
  Konsequenz für TrueYield:
  Wir priorisieren eine klar dokumentierte Datenarchitektur mit API-first Ansatz (News-APIs als Primärquelle, kein ungeklärtes Scraping im MVP) und beschreiben den Datenfluss Ende-zu-Ende im Implementation-Kapitel.

2. Unklarheit, was die KI konkret macht und was nicht.
  Konsequenz für TrueYield:
  Wir trennen explizit zwischen KI-Aufgaben (News-Klassifikation, Risiko-Hinweise, Zusammenfassung) und menschlichen Aufgaben (Audit-Entscheid, Freigabe/Ablehnung). Damit wird Human-in-the-Loop als Kernprinzip sichtbarer.

3. Frage nach MVP-Fokus (zu breiter Scope für Start).
  Konsequenz für TrueYield:
  MVP-Fokus bleibt auf einem klaren Kernflow: Portfolio -> Holdings -> AuditReport -> Evidence-gestützte Entscheidung durch Auditor. Erweiterungen (z.B. tiefere Multilingualität, erweitertes Partnernetzwerk) kommen iterativ.

4. Frage nach Abdeckung kleiner/lokaler und nicht-englischer Quellen.
  Konsequenz für TrueYield:
  Wir definieren das als explizites Folgeziel mit priorisierten Quellenklassen und dokumentieren bekannte Coverage-Limits transparent im MVP.

5. Frage nach Differenzierung zu allgemeiner Deep-Research-KI.
  Konsequenz für TrueYield:
  Das Value-Proposition-Narrativ wird geschärft: TrueYield ist kein allgemeiner Prompt-Output, sondern ein revisionssicherer Workflow mit Rollen, Ownership-Regeln, State Machine und nachvollziehbarer Evidence-Kette.

6. Frage nach Quellenvertrauen und Qualitätssicherung.
  Konsequenz für TrueYield:
  Wir ergänzen Qualitätskriterien für Quellen (Reputation, Aktualität, Duplikatkontrolle, Nachvollziehbarkeit) und machen diese Regeln als Governance-Baustein sichtbar.

7. Frage nach Preislogik und Go-to-Market (insb. grosser Sprung zwischen Tiers).
  Konsequenz für TrueYield:
  Das Pricing wird mit nachvollziehbaren Annahmen (Volumen, SLA, Integrationsaufwand, Risiko-Exposure) begründet und in der Dokumentation klarer vom Pilot- in den Enterprise-Modus überführt.

8. Frage nach Betriebsmodell und Update-Frequenz.
  Konsequenz für TrueYield:
  Wir ergänzen ein einfaches Operating-Modell (Release-Zyklus, Monitoring, Kosten-/Leistungsgrenzen), damit Skalierungs- und Wartungsaspekte früh adressiert sind.

### Wichtigste Punkte aus der Board-Beurteilung

1. Sehr starke Marktrecherche, kohärente Boards, überzeugender regulatorischer Moat.
  Konsequenz für TrueYield:
  Diese Stärken bleiben Kern der Projekterzählung und werden in der Schlusspräsentation als Primärargumente priorisiert.

2. Board ist teilweise zu umfangreich, Kernaussage geht stellenweise unter.
  Konsequenz für TrueYield:
  Wir komprimieren die Kommunikation auf wenige Leitbotschaften: Problemgrösse, differenzierender Mechanismus (Human-in-the-Loop + Evidence), messbare Wirkung (Zeit/Risiko).

3. Nutzergruppen sind gut differenziert, aber in der WKW-Frage zu stark zusammengefasst.
  Konsequenz für TrueYield:
  Die WKW-Frage wurde präzisiert, sodass Fund Manager und Auditor mit ihren unterschiedlichen Jobs-to-be-done klarer getrennt adressiert werden.

---

## Anforderungen

### Use Case Diagram
![Use Case Diagram](doc/uc-diagram.drawio.svg)

> **Hinweis:** Das UC-Diagramm und das ER-Diagramm dokumentieren den ursprünglichen Projektscope mit zwei Primärrollen (Fund Manager, ESG Auditor). Die Rolle **Compliance Officer** wurde nachträglich als optionale Anforderung (Anforderung 23) ergänzt und ist in den Diagrammen nicht enthalten — sie gilt als spätere Erweiterung des Produkts und ist im Abschnitt [Umgesetzte optionale Anforderungen](#umgesetzte-optionale-anforderungen) beschrieben.

### AuditStatus State Machine

Der Lebenszyklus eines `AuditReport`-Dokuments folgt einer strikten Zustandsmaschine. Ungültige Übergänge werden mit `400 Bad Request` abgewiesen.

```
                         POST /api/service/auditreport
                                      │
                                      ▼
                              ┌───────────────┐
                              │ AI_ANALYZING  │  (KI generiert aiRiskSummary)
                              └───────┬───────┘
                                      │  KI fertig
                                      ▼
                            ┌──────────────────┐
                            │  PENDING_REVIEW   │  (sichtbar in Audit-Queue)
                            └────────┬─────────┘
                                     │  PUT /assign  (ESG Auditor)
                                     ▼
                            ┌──────────────────┐
                            │   UNDER_REVIEW    │  (Auditor zugewiesen)
                            └────────┬─────────┘
                           ┌─────────┴─────────┐
                           │                   │
              PUT /complete │                   │ PUT /reject
                           ▼                   ▼
                    ┌──────────┐         ┌──────────┐
                    │ APPROVED │         │ REJECTED │
                    └──────────┘         └──────────┘
```

| Übergang | Auslöser | HTTP-Endpunkt |
|---|---|---|
| → `AI_ANALYZING` | Fund Manager erstellt Audit | `POST /api/service/auditreport` |
| `AI_ANALYZING` → `PENDING_REVIEW` | KI-Analyse abgeschlossen | intern (AiAnalysisService) |
| `PENDING_REVIEW` → `UNDER_REVIEW` | ESG Auditor übernimmt | `PUT /api/service/auditreport/assign` |
| `UNDER_REVIEW` → `APPROVED` | ESG Auditor genehmigt | `PUT /api/service/auditreport/complete` |
| `UNDER_REVIEW` → `REJECTED` | ESG Auditor lehnt ab | `PUT /api/service/auditreport/reject` |

### Use-Case Beschreibungen

---

**UC-01: System Login durchführen**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | Fund Manager, ESG Auditor, Compliance Officer |
| **Vorbedingung** | Benutzer hat ein gültiges Auth0-Konto mit zugewiesener Rolle |
| **Normalablauf** | 1. Benutzer öffnet die Applikation im Browser. 2. System leitet auf Auth0-Login-Seite weiter. 3. Benutzer gibt E-Mail und Passwort ein. 4. Auth0 authentifiziert den Benutzer und gibt ein JWT-Token zurück. 5. System liest die Rolle aus dem Token (`fund-manager`, `auditor` oder `compliance-officer`). 6. Benutzer wird auf die rollenspezifische Startseite weitergeleitet. |
| **Ausnahmen** | Falsches Passwort → Auth0 zeigt Fehlermeldung. Kein Konto vorhanden → Weiterleitung zur Registrierung. |
| **Nachbedingung** | Benutzer ist authentifiziert und kann auf die ihm zugewiesenen Funktionen zugreifen. |

---

**UC-02: Portfolio & Holdings verwalten (CRUD)**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | Fund Manager |
| **Vorbedingung** | Fund Manager ist eingeloggt (UC-01). |
| **Normalablauf** | 1. Fund Manager navigiert zur Portfolio-Übersichtsseite. 2. System zeigt alle Portfolios des eingeloggten Fund Managers. 3. Fund Manager erstellt ein neues Portfolio (Name, Beschreibung, ESG-Zielartikel). 4. System speichert das Portfolio mit der `fundManagerId` des Benutzers. 5. Fund Manager öffnet ein Portfolio und fügt Holdings hinzu (Symbol, ISIN, Name, Gewichtung). 6. Fund Manager kann bestehende Portfolios bearbeiten oder löschen. |
| **Ausnahmen** | Pflichtfelder fehlen → Validierungsfehler (400). Zugriff auf fremdes Portfolio → 403 Forbidden. Portfolio nicht gefunden → 404 Not Found. |
| **Nachbedingung** | Portfolio mit Holdings ist in der Datenbank gespeichert und dem Fund Manager zugeordnet. |

---

**UC-03: ESG Audit anfordern**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | Fund Manager |
| **Vorbedingung** | Fund Manager ist eingeloggt (UC-01). Mindestens ein Portfolio mit Holdings existiert. |
| **Normalablauf** | 1. Fund Manager wählt ein Portfolio aus. 2. Fund Manager löst die ESG-Prüfung aus. 3. System erstellt einen `AuditReport` mit Status `AI_ANALYZING`. 4. System (KI/API) ruft automatisch Marktdaten und News zu den Holdings ab (UC-07). 5. System generiert Risiko-Score und KI-Zusammenfassung (UC-08). 6. System speichert Evidence-Einträge pro Holding (UC-09). 7. System setzt den Status auf `PENDING_REVIEW` und der Report ist in der globalen Audit-Queue sichtbar. |
| **Ausnahmen** | News-API nicht erreichbar → Audit-Report wird ohne Evidence erstellt, Fehlermeldung im Log. |
| **Nachbedingung** | `AuditReport` hat Status `PENDING_REVIEW` und liegt in der Audit-Queue für ESG Auditoren bereit. |

---

**UC-04: Globale Audit-Queue einsehen**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | ESG Auditor |
| **Vorbedingung** | ESG Auditor ist eingeloggt (UC-01). |
| **Normalablauf** | 1. ESG Auditor navigiert zum Audit-Dashboard. 2. System zeigt alle AuditReports mit Status `PENDING_REVIEW` und `UNDER_REVIEW`. 3. ESG Auditor kann nach Portfolio, Status oder Datum filtern. 4. ESG Auditor wählt einen Report aus und öffnet die Detailansicht (UC-05). |
| **Ausnahmen** | Keine offenen AuditReports → leere Liste mit Hinweis. |
| **Nachbedingung** | ESG Auditor hat einen Überblick über alle offenen ESG-Prüfungen. |

---

**UC-05: Audit-Report im Detail ansehen**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | Fund Manager, ESG Auditor |
| **Vorbedingung** | AuditReport existiert. Benutzer ist eingeloggt (UC-01). |
| **Normalablauf** | 1. Benutzer öffnet einen AuditReport aus der Übersicht oder Queue. 2. System zeigt Portfolio-Informationen, Holdings-Liste, KI-Zusammenfassung und Risiko-Score. 3. System zeigt alle gespeicherten Evidence-Einträge (News-Artikel mit Quelle, Datum, Sentiment). 4. ESG Auditor sieht zusätzlich die Entscheidungs-Buttons. |
| **Ausnahmen** | Report nicht gefunden → 404. Kein Zugriff auf fremdes Portfolio → 403. |
| **Nachbedingung** | Benutzer hat vollständigen Überblick über den ESG-Prüfstand inkl. Evidence-Kette. |

---

**UC-06: KI-Befunde validieren & Audit zertifizieren oder ablehnen**

| Attribut | Beschreibung |
|---|---|
| **Akteur** | ESG Auditor |
| **Vorbedingung** | ESG Auditor ist eingeloggt (UC-01). AuditReport hat Status `PENDING_REVIEW`. |
| **Normalablauf** | 1. ESG Auditor öffnet einen AuditReport aus der Queue (UC-04). 2. ESG Auditor prüft die KI-Zusammenfassung, Risiko-Scores und Evidence-Einträge. 3. ESG Auditor übernimmt den Report: `PUT /api/service/auditreport/assign` → Status wechselt auf `UNDER_REVIEW`. 4. ESG Auditor erfasst mindestens eine Begründung (AuditComment) — die Approve/Reject-Buttons sind erst aktiv, wenn mindestens ein Kommentar vorhanden ist. 5a. ESG Auditor genehmigt den Report: `PUT /api/service/auditreport/complete` → Status wechselt auf `APPROVED`. 5b. ESG Auditor lehnt den Report ab: `PUT /api/service/auditreport/reject` → Status wechselt auf `REJECTED`. |
| **Ausnahmen** | Report nicht mehr im Status `PENDING_REVIEW` → 400 Bad Request. Falscher Auditor versucht abzuschliessen → 400 (auditorId mismatch). Kein Kommentar erfasst → Approve/Reject-Buttons deaktiviert. |
| **Nachbedingung** | AuditReport hat finalen Status (`APPROVED` oder `REJECTED`). AuditComment ist gespeichert und für den Fund Manager einsehbar. |

---

**UC-07: Marktdaten und News abrufen** *(System)*

| Attribut | Beschreibung |
|---|---|
| **Akteur** | System (KI/API) |
| **Vorbedingung** | ESG Audit wurde angefordert (UC-03). Holdings mit Symbolen/ISINs sind vorhanden. |
| **Normalablauf** | 1. System liest die Holdings des Portfolios. 2. System ruft pro Holding aktuelle ESG-relevante News über vier sequentielle Provider ab (asynchron zum HTTP-Thread): The Guardian API, NewsAPI.org, Newsdata.io und AlphaVantage (symbol-basiert). 3. System dedupliziert Artikel URL-basiert pro Holding. 4. KI-Relevanzfilter (`analyzeRelevance`, Schwellenwert 0.35) verwirft nicht ESG-spezifische Artikel. 5. System übergibt die gefilterten Artikel an die KI (UC-08). |
| **Ausnahmen** | Provider nicht erreichbar → Fallback auf verfügbare Provider; alle Provider ausgefallen → Evidence-Liste bleibt leer. Keine ESG-relevanten Artikel gefunden → Evidence-Liste bleibt leer. |
| **Nachbedingung** | Rohdaten der News liegen vor und sind zur KI-Analyse bereit. |

---

**UC-08: Risiko-Score & KI-Zusammenfassung generieren** *(System)*

| Attribut | Beschreibung |
|---|---|
| **Akteur** | System (KI/API) |
| **Vorbedingung** | News-Daten wurden abgerufen (UC-07). Spring AI ist konfiguriert. |
| **Normalablauf** | 1. System übergibt News-Texte an Spring AI (Claude/OpenAI). 2. KI-Modell bewertet die ESG-Risiken (Sentiment, Schweregrad, Kategorie). 3. KI generiert eine strukturierte Zusammenfassung (`aiRiskSummary`) pro AuditReport. 4. System berechnet einen aggregierten Risiko-Score. 5. System speichert Summary und Score im AuditReport. |
| **Ausnahmen** | KI-API nicht erreichbar → Fehlermeldung, AuditReport bleibt ohne Summary. |
| **Nachbedingung** | `AuditReport.aiRiskSummary` enthält die KI-generierte Einschätzung. |

---

**UC-09: Evidence speichern** *(System)*

| Attribut | Beschreibung |
|---|---|
| **Akteur** | System (KI/API) |
| **Vorbedingung** | News-Artikel wurden abgerufen und von KI bewertet. |
| **Normalablauf** | 1. System erstellt pro relevantem News-Artikel einen `Evidence`-Eintrag. 2. Evidence enthält: Headline, Quellenname, URL, Datum, Sentiment-Bewertung, ESG-Kategorie, Holding-Referenz. 3. System speichert alle Evidence-Einträge in der MongoDB-Collection `evidence`. |
| **Ausnahmen** | Duplikate (gleiche URL) → bestehender Eintrag wird nicht überschrieben. |
| **Nachbedingung** | Lückenlose, archivierte Evidence-Kette ist für Auditoren und Regulatoren einsehbar. |

---

### UI-Mockup

Interaktiver Klick-Prototyp (Figma): [https://bear-disco-77148489.figma.site/](https://bear-disco-77148489.figma.site/)

### Entity-Relations Diagram
![ER Diagram](doc/er-diagram.drawio.svg)

---

## Implementation

### Auth0-Konfiguration

Damit Rollen (`fund-manager`, `auditor`, `compliance-officer`) korrekt im JWT landen und von Backend und Frontend ausgewertet werden können, sind folgende Schritte in Auth0 erforderlich.

#### 1. Rollen anlegen

Auth0 Dashboard → **User Management → Roles → + Create Role**

| Rollenname | Beschreibung |
|---|---|
| `fund-manager` | ESG Fund Manager — verwaltet Portfolios und Holdings |
| `auditor` | ESG Auditor — prüft und entscheidet über Audit-Reports |
| `compliance-officer` | Compliance Officer — Lesezugriff auf alle Daten, eigenes Compliance-Dashboard |

#### 2. Post-Login Action erstellen

Auth0 Dashboard → **Actions → Library → Build Custom** (Trigger: **Login / Post Login**)

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const roles = event.authorization?.roles ?? [];
  // Inject into access token — read by Spring Security and SvelteKit frontend
  api.accessToken.setCustomClaim('user_roles', roles);
};
```

Action deployen und unter **Actions → Flows → Login** in die Flow-Pipeline ziehen (nach "Start", vor "Complete").

#### 3. Benutzer einer Rolle zuweisen

Auth0 Dashboard → **User Management → Users** → Benutzer auswählen → Tab **Roles** → **Assign Roles**

Jedem Testbenutzer genau eine Rolle zuweisen (`fund-manager`, `auditor` oder `compliance-officer`).

#### 4. Umgebungsvariablen setzen

**Backend** (`backend/src/main/resources/application.properties` oder als Env-Var im Deployment):

```properties
spring.data.mongodb.uri=YOUR_MONGODB_URI
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://YOUR_AUTH0_DOMAIN/
spring.ai.anthropic.api-key=YOUR_ANTHROPIC_API_KEY
news.api.guardian.key=YOUR_GUARDIAN_API_KEY
news.api.newsdata.key=YOUR_NEWSDATA_API_KEY
news.api.newsapiorg.key=YOUR_NEWSAPIORG_API_KEY
news.api.alphavantage.key=YOUR_ALPHAVANTAGE_API_KEY
```

**Frontend** (`.env` oder Deployment-Vars):

```env
AUTH0_DOMAIN=YOUR_AUTH0_DOMAIN
AUTH0_CLIENT_ID=YOUR_AUTH0_CLIENT_ID
AUTH0_AUDIENCE=https://trueyield.api
API_BASE_URL=https://trueyield-backend.azurewebsites.net
```

#### 5. GitHub Secret für SonarQube

Repository → **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Wert |
|---|---|
| `SONAR_TOKEN` | Token aus SonarCloud (Account → Security → Generate Token) |

> Ohne dieses Secret wird der SonarCloud-Step in CI übersprungen.

---

### API-Dokumentation

Vollständige Postman-Dokumentation (veröffentlicht): [https://documenter.getpostman.com/view/52455816/2sBXqJMMCz](https://documenter.getpostman.com/view/52455816/2sBXqJMMCz)

Swagger UI (OpenAPI 3): [https://trueyield-backend.azurewebsites.net/swagger-ui.html](https://trueyield-backend.azurewebsites.net/swagger-ui.html) — lokal unter [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Alle Endpoints sind mit Beispiel-Requests und -Responses dokumentiert.

#### Portfolio (`/api/portfolio`)

| Methode | Endpoint | Beschreibung | Status Codes |
|---|---|---|---|
| POST | `/api/portfolio` | Portfolio erstellen | 201 Created, 400 Bad Request |
| GET | `/api/portfolio` | Alle Portfolios abrufen | 200 OK |
| GET | `/api/portfolio/{id}` | Portfolio by ID | 200 OK, 403 Forbidden, 404 Not Found |
| PUT | `/api/portfolio/{id}` | Portfolio aktualisieren | 200 OK, 404 Not Found |
| DELETE | `/api/portfolio/{id}` | Portfolio löschen | 204 No Content, 403 Forbidden, 404 Not Found |

#### Holding (`/api/holding`)

| Methode | Endpoint | Beschreibung | Status Codes |
|---|---|---|---|
| POST | `/api/holding` | Holding hinzufügen | 201 Created, 400 Bad Request |
| GET | `/api/holding?portfolioId={id}` | Holdings eines Portfolios abrufen | 200 OK |
| DELETE | `/api/holding/{id}` | Holding löschen | 204 No Content, 403 Forbidden, 404 Not Found |
| GET | `/api/holding/news-provider-status` | Konfigurationsstatus aller News-Provider | 200 OK |
| POST | `/api/holding/{id}/ingest-news` | News-Ingestion für eine Holding manuell auslösen | 202 Accepted |

#### Evidence (`/api/evidence`)

| Methode | Endpoint | Beschreibung | Status Codes |
|---|---|---|---|
| POST | `/api/evidence` | Evidence erstellen (inkl. KI-Sentiment-Analyse) | 201 Created, 400 Bad Request |
| GET | `/api/evidence?holdingId={id}` | Evidence einer Holding abrufen | 200 OK |
| GET | `/api/evidence/{id}` | Evidence by ID | 200 OK, 404 Not Found |
| DELETE | `/api/evidence/{id}` | Evidence löschen | 204 No Content, 403 Forbidden, 404 Not Found |

#### AuditReport Service (`/api/service/auditreport`)

| Methode | Endpoint | Beschreibung | Status Codes |
|---|---|---|---|
| POST | `/api/service/auditreport` | Audit-Report erstellen (KI-Analyse wird ausgelöst) | 201 Created, 400 Bad Request |
| GET | `/api/service/auditreport/{id}` | Audit-Report by ID | 200 OK, 404 Not Found |
| PUT | `/api/service/auditreport/assign` | AuditReport zuweisen (PENDING_REVIEW → UNDER_REVIEW) | 200 OK, 400 Bad Request |
| PUT | `/api/service/auditreport/complete` | AuditReport abschliessen (UNDER_REVIEW → APPROVED) | 200 OK, 400 Bad Request |
| PUT | `/api/service/auditreport/reject` | AuditReport ablehnen (UNDER_REVIEW → REJECTED) | 200 OK, 400 Bad Request |
| GET | `/api/service/auditreport/dashboard?portfolioId={id}` | Dashboard-Aggregation per Portfolio | 200 OK |
| GET | `/api/service/auditreport/auditor-queue` | Offene und zugewiesene Reports für eingeloggten Auditor | 200 OK |

#### AuditComment (`/api/service/auditcomment`)

| Methode | Endpoint | Beschreibung | Status Codes |
|---|---|---|---|
| POST | `/api/service/auditcomment` | Kommentar hinzufügen (Auditor) | 201 Created, 400 Bad Request |
| GET | `/api/service/auditcomment?auditReportId={id}` | Alle Kommentare eines Reports | 200 OK |

#### Compliance Overview (`/api/compliance`) — Anforderung 23

| Methode | Endpoint | Beschreibung | Zugriff |
|---|---|---|---|
| GET | `/api/compliance/overview` | Systemweite KPIs (Portfolios, Holdings, AuditReports nach Status) | `compliance-officer` |
| GET | `/api/compliance/sfdr` | SFDR Article 8/9 Klassifizierung pro Portfolio (Sentiment-Aggregation) | `compliance-officer`, `fund-manager` |
| GET | `/api/compliance/portfolios` | Alle Portfolios (Lesezugriff, ohne Einschränkung auf Fund-Manager-ID) | `compliance-officer` |
| GET | `/api/compliance/reports` | Alle Audit-Reports (systemweit, inkl. Statusinformation) | `compliance-officer` |

---

### End-to-End Tests (Cypress)

Im Frontend liegen E2E-Tests unter `frontend/cypress/e2e` mit 5 Testdateien und über 113 Testfällen. Abgedeckte Flows: Login & Rollenbasierter Zugriff, Portfolio CRUD, Audit Workflow, Evidence, Compliance Dashboard.

Ausführung:
- `cd frontend`
- `npm run dev` (separates Terminal)
- `npm run test:e2e`

Standardmässig erwartet Cypress die App unter `http://localhost:5173`. Alternativ kann `E2E_BASE_URL` gesetzt werden.
Authentifizierte Tests nutzen optional `E2E_TEST_EMAIL` und `E2E_TEST_PASSWORD`.

### KI-Integration (Spring AI)

TrueYield nutzt **Spring AI 1.0.0** (`spring-ai-starter-model-anthropic`) mit **AnthropicChatModel** und dem Modell **Claude Haiku (`claude-haiku-4-5-20251001`)** für drei KI-Funktionen im ESG-Workflow sowie einen KI-gestützten Chat-Assistenten:

#### Funktion 1: ESG-Risikozusammenfassung beim Audit-Erstellen

Wenn ein Fund Manager einen Audit-Report erstellt (`POST /api/service/auditreport`), wechselt der Report zunächst in den Status `AI_ANALYZING`. `AiAnalysisService.generateRiskSummary()` sendet einen Prompt an Claude Haiku:

> *"You are an ESG risk analyst. Provide a concise 2-3 sentence risk summary for the investment portfolio … Focus on potential greenwashing risks and ESG compliance concerns."*

Die generierte Zusammenfassung wird als `aiRiskSummary` im `AuditReport` gespeichert und ist für den Auditor auf der Detailseite sichtbar. Danach wechselt der Status automatisch zu `PENDING_REVIEW`.

#### Funktion 2: Sentiment-Analyse für Evidence

Beim Erstellen eines Evidence-Eintrags (`POST /api/evidence`) analysiert `AiAnalysisService.analyzeSentiment()` das `contentSnippet` und gibt einen Dezimalwert zwischen `-1.0` (sehr negative ESG-Nachricht) und `+1.0` (sehr positiv) zurück. Dieser Wert (`aiSentimentScore`) wird persistiert und im Frontend als:
- **Risk-Score** (0–10 Skala, invertiert)
- **Sentiment-Badge** (POSITIVE / NEUTRAL / NEGATIVE)
- **Farbige Risk-Bar** (grün / gelb / rot)

dargestellt. Dies ermöglicht dem Auditor eine schnelle visuelle Einschätzung der ESG-Nachrichtenlage je Holding.

#### Funktion 3: ESG-Relevanzfilter für News-Artikel

Bevor ein News-Artikel als Evidence gespeichert wird, prüft `AiAnalysisService.analyzeRelevance()` ob der Artikel wirklich ESG-relevant für die spezifische Firma ist. Claude Haiku bewertet auf einer Skala von 0.0 bis 1.0:

- **0.7–1.0:** Artikel behandelt direkt ESG-Risiken, Greenwashing, Governance oder Umweltverstösse dieser Firma
- **0.3–0.6:** Teilweiser ESG-Bezug oder branchenweite ESG-Themen mit Firmenrelevanz
- **0.0–0.2:** Nur tangential verwandt, generisches Business-News ohne ESG-Winkel

Artikel mit einem Score unter dem **Schwellenwert 0.35** werden verworfen und nicht als Evidence gespeichert. Dies reduziert Rauschen durch irrelevante Artikel erheblich.

#### Fallback-Verhalten

Ist kein Anthropic API-Key konfiguriert (oder der API-Aufruf schlägt fehl), verhält sich `AiAnalysisService` graceful:
- `isAvailable()` gibt `false` zurück → kein API-Call
- `generateRiskSummary()` liefert `"AI analysis unavailable."` — Audit-Workflow wird nicht blockiert
- `analyzeSentiment()` liefert `0.0` (neutral) — Evidence wird trotzdem gespeichert
- `analyzeRelevance()` liefert `1.0` (relevant) — kein Artikel wird fälschlicherweise gefiltert

#### KI-Chat-Assistent (`/chat`)

Alle drei Rollen haben Zugriff auf einen KI-gestützten Chat-Assistenten unter `/chat`. `ChatService` verwendet `ChatClient` (Spring AI) mit einem systemweiten ESG-Analyst-Prompt und greift via `EsgChatTools` (@Tool-Annotationen) direkt auf die Service-Layer zu. Verfügbare Tool-Operationen:

| Tool | Beschreibung | Rollen |
|---|---|---|
| `getAllPortfolios` | Listet alle sichtbaren Portfolios | alle |
| `getHoldingsByPortfolioName` | Holdings eines Portfolios per Name | alle |
| `getEvidenceByHoldingName` | Evidence-Einträge mit Sentiment-Scores | alle |
| `createPortfolio` | Erstellt ein neues Portfolio | nur `fund-manager` |
| `createHolding` | Fügt ein Holding zu einem Portfolio hinzu | nur `fund-manager` |

Schreibende Operationen sind durch eine Rollenprüfung in `EsgChatTools` abgesichert (`AccessDeniedException` für Nicht-Fund-Manager). Ist kein API-Key konfiguriert, gibt der Chat `"Chat is currently unavailable."` zurück.

---

### News-Datenqualität & Quellenvertrauen (Pitch-Feedback 6)

TrueYield implementiert folgende Qualitätskriterien für News-Quellen, um die Nachvollziehbarkeit der Evidence-Kette sicherzustellen:

| Kriterium | Umsetzung |
|---|---|
| **Quellenreputation** | Drei Provider parallel: **The Guardian API** (keyword-basiert), **NewsAPI.org** und **Newsdata.io** (ticker-basiert) — redaktionell geprüfte Quellen. Premium-Quellen (Reuters, Bloomberg, FT, WSJ, Guardian) werden mit vollem Sentiment-Gewicht gewertet, andere mit Faktor 0.5 gedämpft. |
| **ESG-Relevanz** | Zweistufiger KI-Filter: Guardian-Abfragen enthalten `ESG` als Pflicht-Keyword; alle Artikel durchlaufen anschliessend `analyzeRelevance()` (Claude Haiku, Schwellenwert 0.35) |
| **Firmennamen-Normalisierung** | Rechtliche Suffixe (`Inc.`, `PLC`, `Ltd.`, `AG`, `SE`, etc.) werden vor der Suche entfernt für bessere Trefferqualität |
| **Duplikatkontrolle** | URL-basierte Deduplizierung in-memory (cross-provider) und gegen DB (`existsByHoldingIdAndSourceUrl`) vor AI-Calls |
| **Mengenbegrenzung** | Maximal 5 Artikel pro Provider-Abfrage (`MAX_ARTICLES = 5`); nach Deduplizierung und Relevanzfilter werden bis zu 10 Evidence-Einträge pro Holding gespeichert (cap nach Relevanz-Score priorisiert) |
| **Nachvollziehbarkeit** | Jeder Evidence-Eintrag speichert Quellenname, URL, Publikationsdatum und Snippet |

**Bekannte Einschränkungen (Coverage Limits):**
- Primär englischsprachige Artikel; mehrsprachige Quellen sind als Backlog-Item vorgesehen (B-04)
- Kein expliziter Aktualitätsfilter auf Artikeldatum — ältere Artikel können in den Resultaten erscheinen

---

### MCP Server — ESG Tools (Anforderung 22)

TrueYield exponiert drei ESG-Analyse-Tools über das **Model Context Protocol (MCP)** via Spring AI 1.0.0 (`spring-ai-starter-mcp-server-webmvc`). MCP-kompatible AI-Clients (z. B. Claude Desktop) können sich mit dem Backend verbinden und die Tools direkt aufrufen.

#### SSE-Endpoint

```
GET https://trueyield-backend.azurewebsites.net/sse
```

#### Verfügbare Tools

| Tool | Beschreibung | Parameter |
|---|---|---|
| `generateEsgRiskSummary` | Concise ESG-Risikozusammenfassung für eine oder mehrere Firmen | `companies` — kommaseparierte Firmennamen |
| `analyseEsgSentiment` | Sentiment-Score (-1.0 bis +1.0) für einen ESG-Textausschnitt | `text` — Textausschnitt |
| `fetchEsgNews` | Aktuelle ESG-Nachrichtenartikel über The Guardian API | `company` — Firmenname |

#### Claude Desktop konfigurieren (lokal)

`~/.config/claude/claude_desktop_config.json` (macOS/Linux) bzw. `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "trueyield-esg": {
      "url": "http://localhost:8080/sse",
      "transport": "sse"
    }
  }
}
```

Nach Neustart von Claude Desktop erscheinen die drei Tools im Tool-Panel.

---

### Frontend

#### Rolle: Fund Manager

**Login**
![Login](doc/screenshots/login-page.png)

**Portfolio-Übersicht** — Tabelle aller eigenen Portfolios mit Status-Badge (PENDING_REVIEW / UNDER_REVIEW / APPROVED / REJECTED), SFDR-Ampel (Art. 9 / Art. 8 / —) direkt pro Zeile, und drei KPI-Cards (Total / Pending Review / Approved) oben
![Portfolio-Übersicht](doc/screenshots/portfolios-list.png)

**Portfolio erstellen** — Formular für neues Portfolio (Name, Beschreibung, ESG-Zielartikel)
![Portfolio erstellen](doc/screenshots/portfolio-create.png)

**Portfolio-Detail** — Holdings-Tabelle mit Risk-Score-Balken und SFDR-Badge pro Holding, SVG-Donut-Chart für Asset Allocation (Hover-Tooltip, Legende, grauer "Ungewichtet"-Slice), Audit-Report-Trigger-Button
![Portfolio-Detail](doc/screenshots/portfolio-detail.png)

**Portfolio bearbeiten** — Inline-Formular zum Ändern von Portfolio-Name, Beschreibung und ESG-Zielartikel; nur für den eigenen Fund Manager zugänglich
![Portfolio bearbeiten](doc/screenshots/portfolio-edit.png)

**Holding hinzufügen** — Formular mit Symbol-Autocomplete (debounced, 280 ms, Yahoo Finance API — Dropdown filtert auf EQUITY/ETF und füllt Name automatisch aus), ISIN und Gewichtung
![Holding hinzufügen](doc/screenshots/holding-create.png)

**Holdings-Übersicht** — Aggregierte Ansicht aller Holdings über alle Portfolios
![Holdings-Übersicht](doc/screenshots/holdings-overview.png)

**Holding-Detail / Evidence & Risk Analysis** — Einzelansicht eines Holdings mit SFDR-Ampel, ISIN, Gewichtung, KI-generierten Evidence-Cards (Sentiment-Badge POSITIVE/NEUTRAL/NEGATIVE, Confidence-Badge HIGH/MEDIUM/LOW, Risk-Score 0–10) und Ingest-News-Button
![Evidence & Risk Analysis](doc/screenshots/evidence-page.png)

**Evidence erfassen** — Manuelles Erstellen eines Evidence-Eintrags mit KI-Sentiment-Analyse
![Evidence erfassen](doc/screenshots/evidence-create.png)

**KI-Chat-Assistent** — Alle Rollen haben Zugang zum Chat unter `/chat`. Der Assistent kann Portfolios und Holdings auflisten, Evidence-Scores abfragen und — für Fund Manager — neue Portfolios und Holdings anlegen. *(Kein Screenshot vorhanden; Feature unter `/chat` nach Login erreichbar.)*

**Account** — Benutzerprofil mit Rolle (fund-manager)
![Account Fund Manager](doc/screenshots/account-manager.png)

---

#### Rolle: ESG Auditor

**Audit-Dashboard** — Metrics-Karten, Filter nach Status, Tabelle aller Reports
![Audit-Dashboard](doc/screenshots/audit-dashboard.png)

**Audit-Detail: AI Risk Summary** — KI-generierte Risikozusammenfassung und Status-Timeline
![Audit-Detail AI Summary](doc/screenshots/audit-detail-ai-summary.png)

**Audit-Detail: Assign & Actions** — Assign-Button (PENDING_REVIEW → UNDER_REVIEW), Approve/Reject
![Audit-Detail Actions](doc/screenshots/audit-detail-actions.png)

**Audit-Detail: Kommentare** — Auditor-Begründung hinzufügen
![Audit-Kommentare](doc/screenshots/audit-detail-comments.png)

**Account** — Benutzerprofil mit Rolle (auditor)
![Account Auditor](doc/screenshots/account-auditor.png)

---

#### Rolle: Compliance Officer

**Compliance Dashboard — Overview** — Systemweite KPIs, Reports nach Status, SFDR-Klassifizierung
![Compliance Overview](doc/screenshots/compliance-dashboard-overview.png)

**Compliance Dashboard — Portfolios** — Alle Portfolios mit SFDR-Badge
![Compliance Portfolios](doc/screenshots/compliance-dashboard-portfolios.png)

**Compliance Dashboard — Audit Reports** — Alle Reports mit Statusbadge und Link zum Audit-Detail
![Compliance Reports](doc/screenshots/compliance-dashboard-reports.png)

**Account** — Benutzerprofil mit Rolle (compliance-officer)
![Account Compliance Officer](doc/screenshots/account-compliance.png)

---

### Umgesetzte optionale Anforderungen

| Anforderung | Beschreibung |
|---|---|
| Codeanalyse mit SonarQube | SonarCloud aktiv auf `main`-Branch, Analyse via `sonar-maven-plugin` in CI (non-blocking). Token via Secret `SONAR_TOKEN`. |
| Komplexes Datenmodell (5 Entitäten) | Portfolio, Holding, Evidence, AuditReport, AuditComment — übererfüllt gegenüber Mindestanforderung (3) |
| Komplexes Frontend | 3 Rollen mit rollenspezifischen Dashboards, State-Machine-Visualisierung, Sentiment-Badges, Risk-Scores, SVG-Donut-Chart (Asset Allocation), KPI-Karten, SFDR-Ampel, Evidence-Confidence-Badges, Symbol-Autocomplete |
| Zugriff auf Drittsysteme | Multi-Provider News-Aggregation: The Guardian API, NewsAPI.org und Newsdata.io — automatische ESG-News-Abfrage pro Holding, gespeichert als Evidence mit KI-Relevanzfilter und Source-Weighting |
| Komplexe Abfragen auf der Datenbank | MongoDB Aggregation Pipeline für Audit-Dashboard (gruppiert nach Status pro Portfolio) |
| Komplexe Benutzerverwaltung | 3 RBAC-Rollen (`fund-manager`, `auditor`, `compliance-officer`) mit unterschiedlichen Berechtigungen auf Endpunkt-Ebene (`@PreAuthorize`) und im Frontend (Route Guards) |
| Detaillierte Dokumentation auf GitHub | Issues mit Beschreibungen und überprüfbaren Anforderungen, 3+ Labels, Sprints als Iterations, SCRUM-Board mit Ready/In Progress/Done |
| Mehrere Branches sinnvoll verwendet | Jedes Feature in eigenem `feature/issue-<nr>-<titel>`-Branch entwickelt und via Pull Request gemerged |
| End-to-End Tests (Cypress) | 5 Testdateien, 113+ Testfälle: auth.cy.js, portfolio.cy.js, audit.cy.js, evidence.cy.js, compliance.cy.js — ausgeführt in CI |
| **MCP Server (Anforderung 22)** | Spring AI MCP Server exponiert drei ESG-Analyse-Tools (`generateEsgRiskSummary`, `analyseEsgSentiment`, `fetchEsgNews`) via SSE — verbindbar mit Claude Desktop oder jedem MCP-Client |
| **Dritte Rolle: Compliance Officer (Anforderung 23)** | RBAC-Rolle `compliance-officer` mit systemweitem Lesezugriff. Eigene Endpoints: `/api/compliance/overview`, `/sfdr`, `/portfolios`, `/reports`. Frontend-Dashboard unter `/compliance` mit Tabs (Overview / Portfolios / Audit Reports). Rollenbasierte Sidebar-Navigation. |

---

## Fazit

TrueYield wurde als vollständige ESG-Verification-Plattform mit KI-Unterstützung implementiert.
Das Backend basiert auf Spring Boot 3.4.5 mit MongoDB Atlas und Auth0 JWT-Authentifizierung.
Alle Kernfunktionen — Portfolio-Verwaltung, Holdings, Evidence-Erfassung, Audit-Workflow und
KI-gestützte Risikoanalyse — sind vollständig umgesetzt und getestet; Deployment ist vorbereitet (siehe Deployment-Sektion).

**KI-Integration (Spring AI):** Spring AI 1.0.0 (`spring-ai-starter-model-anthropic`) mit `AnthropicChatModel`
und Claude Haiku analysiert beim Erstellen eines Audit-Berichts das Portfolio und generiert automatisch eine
ESG-Risikozusammenfassung (`AI_ANALYZING → PENDING_REVIEW`). Evidence-Einträge erhalten KI-basierte Sentimentwerte
(-1.0 bis +1.0), die Greenwashing-relevante Nachrichten klassifizieren und als Risk-Score (0–10)
sowie Sentiment-Badge (POSITIVE / NEUTRAL / NEGATIVE) im Frontend visualisiert werden.

**Drittsystem-Integration (Multi-Provider News):** TrueYield aggregiert ESG-Nachrichten aus drei Quellen parallel: The Guardian API, NewsAPI.org und Newsdata.io. Alle Artikel durchlaufen einen zweistufigen KI-Filter: zuerst ESG-Relevanz-Scoring (Schwellenwert 0.35, Claude Haiku), dann Sentiment-Analyse. Premium-Quellen (Reuters, Bloomberg, FT, WSJ, Guardian) werden mit vollem Gewicht gewertet, andere mit Faktor 0.5 gedämpft. URL-Deduplizierung verhindert doppelte Evidence cross-provider. Evidence-Cap bei 10 Einträgen pro Holding, nach Relevanz priorisiert.

**Symbol-Autocomplete (Ticker → Firmenname):** Beim Erstellen eines Holdings tippt der Fund Manager nur das Ticker-Symbol (z.B. "AAPL") und erhält sofort Dropdown-Vorschläge (debounced, 280ms) via Yahoo Finance Search API — gefiltert auf EQUITY und ETF. Auswahl füllt Symbol und Firmenname automatisch aus.

**Portfolio-Visualisierung:** SVG-Donut-Chart auf der Portfolio-Detailseite visualisiert die Asset Allocation nach `weightPercent`. Hover-Effekt (scale + brightness), Tooltip, Legende, und grauer "Ungewichtet"-Slice für Holdings ohne Gewichtung. Keine externe Chart-Library — reines SVG.

**Dashboard KPI-Karten (Fund Manager):** Drei Cards (Total / Pending Review / Approved) auf der Portfolios-Übersicht zeigen den Audit-Status aller Portfolios auf einen Blick. `auditStatus` wird über erweitertes `PortfolioResponseDTO` direkt aus dem verknüpften `AuditReport` mitgeliefert.

**SFDR-Ampel (Portfolio-Liste):** Farbiger Dot + Label ("Art. 9" / "Art. 8" / "—") direkt auf jeder Portfolio-Tabellenzeile — sichtbar für Fund Manager und Compliance Officer, ohne ins Portfolio klicken zu müssen. Daten kommen aus `/api/compliance/sfdr` parallel zum Portfolio-Fetch.

**Evidence Confidence Badge:** Jede Evidence-Karte zeigt ein Konfidenz-Badge (HIGH / MEDIUM / LOW) in Grün/Amber/Rot — berechnet aus Quell-Tier (Premium vs. Other) und Sentiment-Stärke. Sichtbar auf der Holding-Detailseite und im Audit-Report.

**KI-Chat-Assistent:** `ChatService` mit Spring AI `ChatClient` und `EsgChatTools` (@Tool-Annotationen) ermöglicht allen drei Rollen unter `/chat` natürlichsprachliche Abfragen zu Portfolios, Holdings und Evidence. Fund Manager können per Chat neue Portfolios und Holdings anlegen. Schreibrechte sind durch `AccessDeniedException` in `EsgChatTools` abgesichert.

**MCP Server (Anforderung 22):** Spring AI 1.0.0 MCP Server (`spring-ai-starter-mcp-server-webmvc`) exponiert drei ESG-Analyse-Tools via SSE-Endpoint `/sse`. MCP-kompatible Clients (Claude Desktop) können sich verbinden und `generateEsgRiskSummary`, `analyseEsgSentiment` und `fetchEsgNews` direkt aufrufen. Konfiguration: `spring.ai.mcp.server.name=trueyield-esg`, `type=SYNC`.

**Compliance Officer (Anforderung 23):** Dritte RBAC-Rolle `compliance-officer` mit systemweitem Lesezugriff. Vier dedizierte Endpoints: `GET /api/compliance/overview` (KPI-Übersicht), `/sfdr` (SFDR Article 8/9 Klassifizierung per Portfolio), `/portfolios` (alle Portfolios), `/reports` (alle Audit-Reports). Alle schreibenden Operationen sind blockiert (403 per `@PreAuthorize`). Frontend-Dashboard unter `/compliance` mit drei Tabs (Overview / Portfolios / Audit Reports). JWT-Claim-Extraktion ist resilient gegenüber Auth0-Namespace-Varianten.

**Code-Qualität:** `DRAFT`-Status existiert nicht im `AuditStatus`-Enum und wurde bereinigt. SonarCloud aktiv auf `main` (non-blocking, `continue-on-error: true`). ReDoS-Risiken in News-Provider-Regex eliminiert. Security-Hardening: Ownership-Checks auf allen schreibenden Endpoints, rollenbasierte Zugriffsprüfung auf Controller-Ebene.

**Testabdeckung:** JUnit 5 + Mockito für alle Core-Services mit JaCoCo-Gate >= 90 % auf PortfolioService, HoldingService, AuditReportService, AuditCommentService, EvidenceService, UserService und ComplianceService. **421 Testmethoden in 32 Testklassen** — parametrisierte Tests (`@ParameterizedTest`, `@CsvSource`, `@ValueSource`) und Spring MVC MockMvc-Tests für alle Controller mit rollenbasierter Zugriffsprüfung. Cypress E2E: 5 Testdateien, 113+ Testfälle.

**Deployment:** CI/CD via GitHub Actions. Frontend und Backend laufen produktiv auf Azure App Service (Details im Deployment-Abschnitt).

**Stand der Implementation:** Alle Kernfunktionen (Portfolio-Verwaltung, Holdings, Evidence-Erfassung, Audit-Workflow, KI-Risikoanalyse, SFDR-Compliance, 3-Rollen-RBAC, Chat-Assistent) sind vollständig umgesetzt, getestet und deployed. Offene Backlog-Items für die Weiterentwicklung: [B-24 Manueller Risk Score Override](#backlog--nächste-schritte), [B-03 Longitudinales Risk Tracking (Timeseries-Chart)](#backlog--nächste-schritte) und [B-16 Realtime-Updates via SSE](#backlog--nächste-schritte).

---

## Backlog & Nächste Schritte

Die folgenden Erweiterungen sind priorisiert, um die Lösung von einem funktionalen MVP zu einem "Enterprise-Ready" Produkt aufzuwerten, das reale Geschäftsprobleme löst und visuell überzeugt. Sie bilden zudem mögliche Anknüpfungspunkte für eine Bachelorarbeit oder einen produktiven Piloten.

### Bereits umgesetzt
| # | Feature | Beschreibung |
|---|---------|-------------|
| B-01 | **Automatisches News-Monitoring** | Multi-Provider-Aggregation (Guardian, NewsAPI.org, Newsdata.io) liefert bei Holding-Erstellung automatisch ESG-News als Evidence |
| B-02 | **SFDR Article 8/9 Scoring** | Sentiment-Aggregation klassifiziert Portfolios regulatorisch; SFDR-Klasse sichtbar im Compliance-Dashboard und als Ampel auf der Portfolio-Liste |
| B-14 | **Duplicate-Detection für Evidence** | URL-basierte Deduplizierung in-memory (cross-provider) und gegen DB, bevor AI-Calls stattfinden; Evidence-Cap bei 10 pro Holding |
| B-19 | **Dedizierte Finanz-News-APIs** | NewsAPI.org und Newsdata.io als zusätzliche ticker-basierte Quellen neben Guardian; generisches Branchenrauschen deutlich reduziert |
| B-20 | **Zweistufiger KI-Relevanzfilter** | ESG-spezifisches Relevanz-Prompt mit Scoring-Skala 0.0–1.0; Artikel unter Schwellenwert 0.35 werden als Evidence verworfen |
| B-21 | **Source Reliability Weighting** | Sentiment-Scores von Premium-Quellen (Reuters, Bloomberg, Financial Times, WSJ, Guardian) werden voll gewichtet; andere Quellen mit Faktor 0.5 gedämpft |
| B-22 | **Holding Auto-Complete (Ticker)** | Debounced Ticker-Suche via Yahoo Finance mit Dropdown-Vorschlägen (EQUITY + ETF); wählt automatisch Name aus |
| B-23 | **Portfolio Asset Allocation (Donut-Chart)** | Reines SVG-Donut-Chart auf der Portfolio-Detailseite; Hover-Effekt, Tooltip und Legende; grauer "Ungewichtet"-Slice für Holdings ohne Gewichtung |
| B-10 | **KPI-Karten für Fund Manager** | Drei Dashboard-Cards (Total / Pending Review / Approved) auf der Portfolios-Übersicht; Daten live aus AuditReport-Status via erweitertem PortfolioResponseDTO |
| B-25 | **Evidence Confidence Badge** | HIGH / MEDIUM / LOW Badge auf jeder Evidence-Karte (Grün/Amber/Rot) — berechnet aus Quell-Tier und Sentiment-Stärke; sichtbar auf Holding-Detail und Audit-Report |
| B-26 | **SFDR-Ampel auf Portfolio-Liste** | Farbiger Dot + Label ("Art. 9" / "Art. 8" / "—") direkt auf der Tabellenzeile; parallel fetch von `/api/compliance/sfdr`; nur für Fund Manager und Compliance Officer |

### Offen: Demo-Impact (Abgabe 24.05.2026)
*Features die in der Live-Demo den Human-in-the-Loop Ansatz greifbar machen.*
| # | Feature | Mehrwert | Aufwand |
|---|---------|----------|---------|
| B-24 | **Manueller Risk Score Override (Auditor)** | Auditor überschreibt den AI-generierten Risk Score mit Pflichtkommentar — zeigt Human-in-the-Loop live in der Demo. Feld `overrideRiskScore` auf AuditReport, Inline-Edit im Frontend. | 2–3h |
| B-03 | **Longitudinales Risk Tracking** | Sentiment-Zeitreihe pro Holding als Timeseries-Chart. | 1–2 Tage |
| B-16 | **Realtime-Updates via SSE/WebSocket** | Statuswechsel (`AI_ANALYZING` → `PENDING_REVIEW`) ohne Page-Reload sichtbar. | 1–2 Tage |

### Priorität 2: Usability & Enterprise Workflow
| # | Feature | Mehrwert | Aufwand |
|---|---------|----------|---------|
| B-07 | **CSV/Excel-Upload für Holdings** | Drag & Drop Import ganzer Portfolios statt manueller Einzelerfassung. | 1–2 Tage |

### Priorität 3: Polish, Reporting & Tests
| # | Feature | Mehrwert | Aufwand |
|---|---------|----------|---------|
| B-09 | **Audit-PDF-Export** | Revisionssicherer Export des genehmigten Reports inkl. Evidence-Bibliografie für den Regulator. | 2–3 Tage |
| B-08 | **E-Mail-Benachrichtigungen** | Automatische Benachrichtigung bei Statuswechsel (Spring Mail). | 1 Tag |
| B-06 | **Erweiterte Cypress E2E Tests** | Automatisierte Tests für neue Flows (Evidence-Delete, SFDR-Dashboard). | 1 Tag |

**Bachelorarbeit-Anknüpfungspunkte:**
- *AI-Assisted ESG Risk Assessment unter SFDR: Evaluation einer Human-in-the-Loop Architektur für Greenwashing-Erkennung* (direkt auf TrueYield aufbaubar)
- *Automated ESG Compliance Monitoring for Insurance Investment Portfolios under SFDR Article 8/9* (Swiss Re/AIG-Netzwerk als Pilot)
- *Sentiment-Based Early Warning Signals for Greenwashing Risk: A News-Driven Approach* (quantitative Analyse mit öffentlichen Daten)
