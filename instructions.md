# Instructions for AI Agent: Project TrueYield (FS 2026)

---

## 1. Project Mission (Non-Negotiable Context)

**TrueYield** ist eine ESG-Liability-Engine zur Identifikation von Greenwashing-Risiken in Investmentfonds.

- **Ziel:** Automatisierte ESG-News-Analyse via Spring AI, revisionssicherer Audit-Workflow, rollenbasiertes Review.
- **Grundsatz:** Die KI liefert Evidenz und Risk-Scores; der menschliche ESG-Auditor validiert und entscheidet (Human-in-the-Loop).
- **Prinzip:** Explainability vor Automatisierung. Jede KI-Entscheidung muss durch gespeicherte `Evidence` nachvollziehbar sein.

---

## 2. Hard Academic Constraints (Grading Relevant – Do Not Violate)

Diese Anforderungen sind benotungsrelevant und haben höchste Priorität.

### Bewertungsgewichtung

| Kategorie | Gewicht |
|-----------|---------|
| Projektinhalt | 60% |
| Präsentation (Pitch 5% + Schlusspräsentation 20%) | 25% |
| Dokumentation (README.md) | 15% |

> Jeder nicht erfüllte pass/fail Leistungsnachweis gibt **¼ Note Abzug**.

### Stack

- **Frontend:** SvelteKit
- **Backend:** Spring Boot 3.x (Java 17+)
- **Datenbank:** MongoDB Atlas

### Authentication & Authorization

- Auth0 Integration
- Mindestens zwei Rollen:
  - `Fund Manager`
  - `ESG Auditor`
- Frontend und Backend sind durch Login geschützt.

### AI Integration

- Nutzung der Spring AI API
- KI ist sinnvoll in den fachlichen Workflow integriert (News-Analyse, Evidence-Generierung).

### Testing

- JUnit 5 mit Assertions, parametrisierten Tests, Exception Testing
- Mockito
- Möglichst alle Endpoints via `@SpringBootTest` + `@MockMvc` rollenabhängig getestet
- JSON-Resultate sinnvoll geprüft
- JaCoCo-Testabdeckung gemessen, als Badge im README und als HTML-Bericht
- Ziel: ~90% Testabdeckung

### Deployment

- Dockerfile
- Azure App Service
- GitHub Actions (CI/CD Pipeline)
- Erfolgreiche Deployment-Logs im Repository

### Datenmodell (Pflicht)

- Mindestens 3 Entitätstypen mit Beziehungen in separaten Collections (wir haben 5)
- Mindestens eine Entität durchläuft mehrere Zustände (`AuditReport`)
- Mindestens eine Entität ist einem User zugeordnet und nur von diesem bearbeitbar (`Portfolio`)
- Alle Entitäten können über das Frontend erstellt (und wenn fachlich sinnvoll bearbeitet/gelöscht) werden

### API-Dokumentation

- API-Endpoints sind mit **Postman** dokumentiert und veröffentlicht
- Veröffentlichungs-URL: `https://documenter.getpostman.com/...`

### GitHub-Anforderungen

- Privates GitHub-Repository
- SCRUM-Board erstellt und verwendet
- Sprints als **Iterations** in GitHub abgebildet (1 Woche Dauer)
- Issues mit Beschreibung, überprüfbaren Anforderungen, mindestens 3 verschiedene Labels
- Issues durchlaufen: `Ready` → `In Progress` → `Done`
- Abgeschlossene Issues haben Status `Closed`

> Diese Constraints dürfen niemals durch Refactoring oder Optimierung verletzt werden.

---

## 3. Optionale Anforderungen (Bonus – Erwartungen übertreffen)

Folgende optionale Features sind geplant:

- [x] **Komplexes Datenmodell** (5 Entitäten statt 3)
- [x] **Zugriff auf Drittsysteme** (externe News-APIs für ESG-Analyse)
- [x] **Detaillierte Dokumentation auf GitHub** (Issues, Labels, Sprints)
- [x] **Mehrere Branches sinnvoll verwendet**
- [ ] **Codeanalyse mit SonarQube**
- [ ] **End-to-End Tests** (Cypress)
- [ ] **Backend mit MCP-Server** (optional, falls Zeit)

---

## 4. Non-Functional Constraints (Security & Quality)

### Security

- **No Secrets in Code:** Keine Passwörter, Tokens oder Connection Strings im Code speichern. Nutzung von Environment Variables.
- Keine Sicherheitslogik ausschließlich im Frontend.

### Architecture Discipline

- **Thin Controllers:** Controller übernehmen nur Request/Response Handling.
- **No Business Logic in Controllers.**
- Keine direkten Repository-Zugriffe aus Controllern.
- Geschäftslogik befindet sich ausschließlich im Service-Layer.

### Validation & Integrity

- Alle Ownership-Prüfungen sind serverseitig umzusetzen.
- Alle State-Transitions werden im Service validiert.
- Keine Statusmanipulation im Controller.

### Error Handling

- Exceptions dürfen nicht verschluckt werden.
- Verwendung korrekter HTTP-Statuscodes:
  - `400` Bad Request
  - `401` Unauthorized
  - `403` Forbidden
  - `404` Not Found
  - `500` Internal Server Error
- Zentrale Fehlerbehandlung via `@ControllerAdvice` empfohlen.

### Code Quality

- Keine redundante Logik.
- Keine toten Methoden.
- Keine ungenutzten Dependencies.
- Lesbare, konsistente Naming Conventions.

---

## 5. Domain Model & Ownership Rules

### 5.1 Data Isolation Rule

Ein `Fund Manager` darf ausschließlich auf Portfolios zugreifen, bei denen:

```text
portfolio.fundManagerId == authenticatedUserId
```

- Diese Validierung erfolgt zwingend serverseitig.
- Kein Zugriff darf ausschließlich durch Frontend-Filterung kontrolliert werden.

---

### 5.2 Collections (Strict)

#### `portfolio`
- Basis-Daten eines Fonds
- Enthält `fundManagerId`

#### `holding`
- Aktien- oder Fonds-Position
- Gehört zu genau einem Portfolio

#### `evidence`
- KI-generierte News-Snippets
- Sentiment- oder Risk-Score
- Gehört zu genau einer Holding

#### `auditReport`
- Zentrales Workflow-Objekt
- Enthält Status + KI-Zusammenfassung

#### `auditComment`
- Fachliche Begründung des Auditors
- Gehört zu genau einem AuditReport

---

### 5.3 State Machine (AuditReport)

#### Workflow

```text
DRAFT → AI_ANALYZING → PENDING_REVIEW → APPROVED | REJECTED
```

#### State Machine Rules
- **Transition Logic:** Zustände dürfen nicht übersprungen werden.
- **Validation:** Statuswechsel werden ausschließlich im Service-Layer validiert.
- **Constraint:** Kein direkter Statuswechsel im Controller.
- **Terminal States:** `APPROVED` und `REJECTED` sind terminale Zustände.

---

## 6. Architectural Standards

### Layering
- `Controller` → `Service` → `Repository`
- **Strict Rule:** Keine Abkürzungen oder direkte Umgehungen.

### DTO Separation
- Nutzung von DTOs für API-Input und API-Output.
- **Constraint:** Entities/DAOs werden niemals direkt über die API exponiert.
- **Mapping:** Erfolgt im Service oder über dedizierte Mapper.

### Validation
- Nutzung von `jakarta.validation.constraints`.
- Verwendung von `@Valid` in Controllern.
- Keine manuellen Null-Checks im Controller.

### Security Implementation
- Nutzung von `SecurityFilterChain`.
- Rollenprüfung via `@PreAuthorize`.
- Keine Nutzung veralteter Konfigurationen.
- Keine alleinige Absicherung über das Frontend.

### Jakarta Awareness
- Ausschließlich `jakarta.*` Imports verwenden (nicht `javax.*`).

---

## 7. Testing – Definition of Done (DoD)

Ein Feature oder Issue gilt nur als abgeschlossen, wenn:
- Jede neue Service-Methode über mindestens einen **Unit-Test** verfügt.
- Der **Happy Path** getestet ist.
- Mindestens ein **Fehlerfall** getestet ist.
- Rollenberechtigungen via `@SpringBootTest` + `@MockMvc` verifiziert wurden.
- JSON-Responses sinnvoll geprüft werden.
- Die **JaCoCo-Gesamtabdeckung** nicht sinkt.
- Keine Business-Logik ungetestet bleibt.
- Keine ungetesteten Service-Methoden erlaubt sind.

---

## 8. AI Agent Working Rules

### Never
- Keine `// TODO` Kommentare generieren.
- Keine erfundenen APIs verwenden.
- Keine neuen Maven-Dependencies ohne explizite Erlaubnis.
- Keine Breaking Changes ohne klaren Hinweis.
- Keine großflächigen Refactorings ohne Begründung.

### Always
- Vor neuem Code bestehende Patterns analysieren (**Reference Check**).
- Konsistente Naming Conventions einhalten.
- Exceptions korrekt behandeln.
- HTTP-Statuscodes fachlich korrekt setzen.
- Tests mitdenken, bevor neue Logik implementiert wird.

### Refactoring Rules
- Refactoring darf niemals **Hard Academic Constraints** gefährden.
- Bestehende Tests dürfen nur angepasst werden, wenn:
  - Die fachliche Logik bewusst geändert wurde.
  - Die Anpassung begründet ist.
- Keine unnötige Umstrukturierung funktionierender Module.

### Uncertainty Handling
- Wenn eine API oder Anforderung unklar ist:
  - Keine Annahmen treffen.
  - Keine Halluzinationen erzeugen.
  - Explizit Rückfrage stellen.

---

## 9. Git, Branching & Sprint Governance

- Projekt ist in **14 wöchentliche Iterationen** unterteilt (Woche 1–14, Abgabe Ende Woche 14).
- Jedes Feature wird als GitHub Issue erfasst.
- Jedes Issue enthält:
  - Zielbeschreibung
  - Akzeptanzkriterien
  - Testkriterien

---

### 9.1 Branch-Strategie: Branch-and-Pull-Modell

Wir nutzen das **Branch-and-Pull-Modell** (wie in der Vorlesung SE2 vermittelt): Jedes Feature wird in einem eigenen Branch entwickelt und erst nach Prüfung via Pull Request in `main` gemerged. Dadurch bleibt `main` immer stabil und deploybar.

```text
main ─────●────────────●────────────●────────────●──── (immer stabil)
            \          ↑ PR+Merge    \          ↑ PR+Merge
             \        /               \        /
  feature/    ●──●──●                  ●──●──●   feature/
  issue-7     (commits)                (commits)  issue-13
```

#### Warum Branches?

- `main` bleibt immer lauffähig → wichtig für Deployment und Dozenten-Demo.
- Jedes Feature ist isoliert → kein kaputter Code auf `main`.
- Pull Requests erzeugen eine sichtbare Historie → zeigt den Dozenten sauberes Vorgehen.
- Ist eine **optionale Anforderung** ("mehrere Branches sinnvoll verwendet") → Bonus-Punkte.

---

### 9.2 Branch Naming Convention

```text
feature/issue-<nr>-<kurzer-titel>
```

Beispiele:
- `feature/issue-7-get-portfolio-by-id`
- `feature/issue-27-auth0-backend-security`
- `feature/issue-37-audit-state-machine`
- `fix/issue-42-ci-pipeline-test-failure`
- `docs/issue-70-readme-finalization`

Regeln:
- Immer lowercase, Wörter mit `-` trennen.
- Immer die Issue-Nummer referenzieren.
- Präfix nach Typ: `feature/`, `fix/`, `docs/`, `chore/`.

---

### 9.3 Kompletter Git-Workflow (Schritt für Schritt)

Dieses ist der vollständige Workflow für jede Aufgabe, vom Issue-Start bis zum Merge:

#### Schritt 1: Issue in "In Progress" schieben
Im GitHub Project Board das Issue von `Ready` nach `In Progress` verschieben.

#### Schritt 2: Neuen Branch erstellen

**Im Terminal (VS Code, Claude Code):**
```bash
# Sicherstellen, dass main aktuell ist
git checkout main
git pull origin main

# Neuen Feature-Branch erstellen und wechseln
git checkout -b feature/issue-7-get-portfolio-by-id
```

**In VS Code (GUI):**
1. Unten links auf den Branch-Namen klicken (z.B. `main`).
2. `Create new branch...` wählen.
3. Name eingeben: `feature/issue-7-get-portfolio-by-id`.
4. VS Code wechselt automatisch auf den neuen Branch.

#### Schritt 3: Arbeiten und Committen

```bash
# Änderungen stagen
git add .

# Commit mit Convention (Issue-Referenz im Body oder Footer)
git commit -m "feat: add GET /api/portfolio/{id} endpoint

Implements portfolio lookup by ID with ownership validation.
Closes #7"
```

**Wichtig:** Das Wort `Closes #7` im Commit-Message schliesst das Issue automatisch beim Merge in `main`.

#### Schritt 4: Branch auf GitHub pushen

```bash
# Ersten Push mit Upstream-Tracking
git push -u origin feature/issue-7-get-portfolio-by-id

# Alle weiteren Pushes
git push
```

#### Schritt 5: Pull Request erstellen

1. Auf GitHub erscheint automatisch ein Banner: "Compare & pull request".
2. **Titel:** `feat: GET /api/portfolio/{id} – Closes #7`
3. **Description:** Akzeptanzkriterien aus dem Issue referenzieren.
4. Pull Request erstellen.

#### Schritt 6: Merge nach main

1. Auf GitHub den Pull Request reviewen (grüner "Merge" Button).
2. **Merge-Methode:** `Merge commit` (erzeugt einen sichtbaren Merge-Punkt im Graph).
3. **Optional:** Branch nach Merge löschen (GitHub bietet das automatisch an).

#### Schritt 7: Lokal aufräumen

```bash
# Zurück zu main wechseln und aktualisieren
git checkout main
git pull origin main

# Alten Branch lokal löschen (optional, aber sauber)
git branch -d feature/issue-7-get-portfolio-by-id
```

---

### 9.4 Commit Convention

Format: `<typ>: <beschreibung>`

| Präfix | Verwendung | Beispiel |
|--------|-----------|----------|
| `feat` | Neue Features | `feat: add portfolio creation endpoint` |
| `fix` | Bugfixes | `fix: resolve null pointer in holding service` |
| `test` | Tests hinzufügen/ändern | `test: add unit tests for AuditReportService` |
| `docs` | Dokumentation | `docs: update README with ER diagram` |
| `chore` | Wartung, Config | `chore: configure JaCoCo Maven plugin` |
| `refactor` | Code-Struktur | `refactor: extract validation to service layer` |
| `ci` | CI/CD Pipeline | `ci: add GitHub Actions test workflow` |

#### Issue-Referenzierung in Commits

```text
# Issue automatisch schliessen (nur bei Merge in main):
Closes #7
Fixes #42
Resolves #15

# Issue nur referenzieren (ohne zu schliessen):
Related to #7
Part of #42
```

---

### 9.5 Git Graph Visualisierung

Die Branch-Historie soll für die Dozenten sichtbar und nachvollziehbar sein. Dazu nutzen wir Visualisierungstools:

#### VS Code: Git Graph Extension

**Installation:**
1. VS Code Extension Marketplace öffnen (`Ctrl+Shift+X`).
2. Suche: `Git Graph` (von mhutchie).
3. Installieren.

**Verwendung:**
1. In VS Code: `Ctrl+Shift+P` → `Git Graph: View Git Graph` eingeben.
2. Oder: In der Source Control Sidebar auf das Git-Graph-Icon klicken.
3. Der Graph zeigt alle Branches, Merges und Commits als farbige Linien.

**Was man im Graph sehen sollte (für die Dozenten):**
- `main` als durchgehende Linie.
- Feature-Branches die von `main` abzweigen und wieder zurückmergen.
- Merge-Commits an den Zusammenführungspunkten.
- Commit-Messages mit Issue-Referenzen (`#7`, `#13`, etc.).

#### GitHub: Network Graph

- Im Repository: Tab `Insights` → `Network`.
- Zeigt denselben Graph online, nützlich für die Abgabe.

#### Terminal: ASCII-Graph

```bash
# Kompakte Ansicht aller Branches
git log --oneline --graph --all --decorate

# Nur die letzten 20 Commits
git log --oneline --graph --all --decorate -20
```

Ausgabe-Beispiel:
```text
*   abc1234 (HEAD -> main) Merge PR #3: feat: add holding API
|\
| * def5678 (origin/feature/issue-13-create-holding) feat: add POST /api/holding
| * ghi9012 feat: add HoldingService with ownership check
|/
*   jkl3456 Merge PR #2: feat: portfolio CRUD endpoints
|\
| * mno7890 feat: add DELETE /api/portfolio/{id}
| * pqr1234 feat: add PUT /api/portfolio/{id}
|/
* stu5678 (tag: v0.1.0) Initial domain layer. Closes #4
```

---

### 9.6 Merge-Kriterien

Ein Merge (Pull Request) in `main` ist nur erlaubt, wenn:

1. Alle Tests erfolgreich sind (lokal und/oder CI).
2. Coverage nicht gesunken ist.
3. Keine Secrets im Code enthalten sind.
4. Der Branch ist aktuell mit `main` (keine Konflikte).
5. Die Commit-Messages folgen der Convention.

---

### 9.7 Häufige Situationen & Lösungen

#### Main hat sich weiterentwickelt, während ich am Feature gearbeitet habe

```bash
# Auf dem Feature-Branch:
git fetch origin
git merge origin/main
# Konflikte lösen falls nötig, dann committen
```

#### Ich habe versehentlich auf main committet

```bash
# Letzten Commit von main auf neuen Branch verschieben
git branch feature/issue-X-mein-feature
git reset --hard HEAD~1
git checkout feature/issue-X-mein-feature
```

#### Ich will sehen, auf welchem Branch ich bin

```bash
git branch          # Lokale Branches (* = aktuell)
git branch -a       # Alle Branches inkl. Remote
```

In VS Code: Unten links in der Statusleiste steht immer der aktuelle Branch-Name.

---

### 9.8 AI-Agent-spezifische Git-Anweisungen

Dieser Abschnitt richtet sich an die KI-Agenten (GitHub Copilot, Claude Code, Anthropic Claude in VS Code / Windsurf / Cursor), die im Projekt mitarbeiten. Jeder Agent **muss** diese Regeln befolgen.

### Universelle Regeln (für ALLE AI Agents)

#### Vor jeder Code-Änderung
1. **Branch prüfen:** Stelle sicher, dass du NICHT auf `main` arbeitest. Prüfe mit `git branch` oder lies die Statusleiste.
2. **Richtigen Branch nutzen:** Wenn kein passender Feature-Branch existiert, erstelle einen nach der Naming Convention (`feature/issue-<nr>-<titel>`).
3. **Main aktuell halten:** Vor dem Erstellen eines neuen Branches immer `git pull origin main` ausführen.

#### Beim Committen
1. **Commit Convention einhalten:** Immer das Format `<typ>: <beschreibung>` verwenden.
2. **Issue referenzieren:** Im Commit-Body oder -Footer immer die Issue-Nummer angeben.
3. **Keine Auto-Merges auf main:** Niemals direkt auf `main` committen oder pushen.
4. **Atomare Commits:** Ein Commit = eine logische Änderung. Keine Mega-Commits mit 20 Dateien.

#### Beim Umgang mit Branches
1. **Niemals `main` force-pushen:** `git push --force` auf `main` ist **verboten**.
2. **Branch nach Merge löschen:** Nach erfolgreichem Merge den Feature-Branch lokal und remote löschen.
3. **Keine verwaisten Branches:** Offene Branches sollten einem aktiven Issue zugeordnet sein.

---

### 9.8.1 Anweisungen für Claude Code (Terminal-Agent)

Claude Code arbeitet direkt im Terminal und hat vollen Git-Zugriff. Folgende Regeln gelten:

```text
WORKFLOW FÜR CLAUDE CODE:
─────────────────────────
1. PRÜFE den aktuellen Branch:
   → git branch --show-current
   → Falls "main" → SOFORT neuen Branch erstellen

2. ERSTELLE Branch falls nötig:
   → git checkout main && git pull origin main
   → git checkout -b feature/issue-<nr>-<titel>

3. ARBEITE auf dem Feature-Branch:
   → Code schreiben, Tests schreiben
   → git add <geänderte-dateien>  (NICHT "git add .")
   → git commit -m "<typ>: <beschreibung>\n\nCloses #<nr>"

4. PUSHE den Branch:
   → git push -u origin feature/issue-<nr>-<titel>

5. ERSTELLE Pull Request (wenn git CLI vorhanden):
   → gh pr create --title "feat: <titel> – Closes #<nr>" --body "..."
   
6. MERGE NIEMALS selbstständig in main.
   → Der User entscheidet über den Merge.
```

**Spezifische Regeln:**
- Claude Code darf `git` Befehle ausführen, aber **niemals** `git push --force` auf `main`.
- Claude Code soll bei jedem Task-Start den aktuellen Branch ausgeben und bestätigen.
- Wenn Claude Code auf `main` arbeiten soll (z.B. README-Update), muss der User das **explizit** bestätigen.
- Claude Code soll nach Abschluss einer Aufgabe `git log --oneline -5` ausgeben, um dem User die letzten Commits zu zeigen.

---

### 9.8.2 Anweisungen für VS Code AI Agents (Copilot, Cline, Windsurf, Cursor)

VS Code AI Agents (GitHub Copilot Chat, Cline, Windsurf, Continue, Cursor) arbeiten innerhalb des VS Code Editors und haben typischerweise keinen direkten Git-Zugriff über das Terminal. Folgende Regeln gelten:

**Vor Code-Generierung:**
- Prüfe den aktuellen Branch über die VS Code Statusleiste (unten links).
- Wenn der Agent Terminal-Zugriff hat (z.B. Cline, Windsurf): gleiche Regeln wie Claude Code (9.8.1).
- Wenn der Agent keinen Terminal-Zugriff hat (z.B. Copilot Inline): weise den User an, den Branch zu wechseln, bevor Code generiert wird.

**Bei der Code-Generierung:**
- Generierter Code muss zum aktuellen Issue passen (Branch-Name = Issue-Kontext).
- Keine Änderungen generieren, die andere Issues betreffen.
- Keine Dateien erstellen, die nicht zum aktuellen Feature-Branch gehören.

**Commit-Message Vorschläge:**
- Wenn der Agent Commit-Messages vorschlägt, **immer** die Convention einhalten.
- **Immer** die Issue-Nummer in der Message referenzieren.

**Beispiel-Hinweis an den User:**
```text
⚠️ Du bist aktuell auf Branch "main". Für Issue #13 solltest du zuerst 
einen Feature-Branch erstellen:
  git checkout -b feature/issue-13-create-holding
Soll ich den Code dann auf dem neuen Branch generieren?
```

---

### 9.8.3 Anweisungen für Claude (claude.ai / Anthropic Chat)

Claude in der Chat-Oberfläche (claude.ai, Claude App) hat **keinen** direkten Git-Zugriff. Folgende Regeln gelten:

**Bei Code-Generierung:**
- Wenn Claude Code-Dateien oder Änderungen generiert, immer angeben:
  - Für welches Issue der Code ist.
  - Auf welchem Branch der Code eingefügt werden soll.
  - Welche Commit-Message der User verwenden soll.

**Beispiel-Output-Format:**
```text
📁 Branch: feature/issue-37-audit-state-machine
📝 Dateien:
  - src/main/java/.../service/AuditReportService.java (NEU)
  - src/main/java/.../controller/AuditReportController.java (NEU)
  - src/test/java/.../service/AuditReportServiceTest.java (NEU)

💬 Commit-Message:
  feat: implement AuditReport state machine

  Adds state transition logic (DRAFT → AI_ANALYZING → PENDING_REVIEW → APPROVED/REJECTED)
  with validation in the service layer.

  Closes #37
```

**Bei Mehreren Issues in einer Session:**
- Immer klar trennen, welcher Code zu welchem Issue/Branch gehört.
- Niemals Code für mehrere Issues in einem einzigen Commit-Vorschlag mischen.

---

### 9.9 Empfohlene VS Code Extensions für Git

| Extension | Zweck | Hinweis |
|-----------|-------|---------|
| **Git Graph** (mhutchie) | Branch-Visualisierung, Merge-Historie | **Pflicht** – zeigt den Dozenten saubere Branch-Struktur |
| **GitLens** (gitkraken) | Blame-Annotationen, Commit-Details | Sehr nützlich für Code-Review |
| **GitHub Pull Requests** (GitHub) | PRs direkt in VS Code erstellen/reviewen | Optional, aber praktisch |
| **Git History** (donjayamanne) | Datei-Historie, Diff-Ansicht | Optional |

#### Git Graph konfigurieren (empfohlen)

In VS Code Settings (`Ctrl+,`), suche nach `git graph` und setze:
- `Git Graph: Default Column Visibility` → alle auf `true`
- `Git Graph: Show Current Branch By Default` → `true`

Dann: `Ctrl+Shift+P` → `Git Graph: View Git Graph` → zeigt den vollständigen Branch-Graphen.

---

## 10. Abgabe-Checkliste (Ende Woche 14 – 24.05.2026)

Alle Dozierenden als Collaborators hinzufügen (Repo + Project):
- `DavidZhaw`, `mosazhaw`, `mmeisterhans`, `bkuehnis`

Über Moodle abzugeben:
- [ ] Zip-Ordner des GitHub-Repos
- [ ] `jacoco.zip` (Verzeichnis `target/site/jacoco`)
- [ ] `surefire-reports.zip` (Verzeichnis `target/surefire-reports`)
- [ ] URL des GitHub-Repos
- [ ] URL der deployten Applikation
- [ ] Login/Passwort pro Benutzerrolle (Fund Manager + ESG Auditor)
- [ ] URL der Postman API-Dokumentation
- [ ] URL der Roadmap-History (GitHub)
- [ ] URL des Charts mit Iterations und Labels (GitHub)
- [ ] URL von SonarQube

---

## 11. README.md Struktur (Pflicht)

Die Dokumentation im README.md muss folgende Kapitel enthalten:

### Einleitung
- Explore-, Create- und Evaluate-Boards
- Diskussion des Feedbacks aus dem Pitch (bezogen auf Projektinhalt)

### Anforderungen
- Use-Case Diagramm
- Use-Case Beschreibung (wie in RE gelernt)
- Fachliches Datenmodell (ER-Modell), Beschreibung der Zustände
- Mockup oder Skizze des UIs

### Implementation
- Beschreibung des Frontends mit Screenshots der fertigen Applikation
- Alle GUI-Teile, die bewertet werden sollen, müssen abgebildet sein
- Aufgaben und Funktionen des eingebundenen KI-Modells
- Umgesetzte optionale Anforderungen mit Beschreibung

### Fazit
- Stand der Implementation, nächste Schritte (mit Referenz auf den Backlog)

> Bilder im Ordner `/doc` ablegen und in die MD-Datei einbinden.

---

## 12. Pass/Fail Leistungsnachweise (Termingebunden)

| Termin | Leistungsnachweis | Status |
|--------|-------------------|--------|
| Woche 1–13 | Wöchentliche Übungsabgabe (Dienstag 10:00) | 🔄 Laufend |
| Woche 2/3 | Besprechung Projektidee mit Dozent (UC + ER mitbringen) | ✅ Done |
| Woche 3 | Abgabe Explore-, Create- und Evaluate-Boards | ⬜ Offen |
| Woche 4 | Gegenseitige Beurteilung der Boards | ⬜ Offen |
| Woche 4 | Pitch Probelauf in der KK (Präsenzpflicht) | ⬜ Offen |
| Woche 5/6 | Pitch Semesterprojekt (benotet, 5% Gesamtnote) | ⬜ Offen |
| Woche 5/6 | Feedback zu Pitches der Mitstudierenden (Präsenzpflicht) | ⬜ Offen |
| Woche 6 | Abgabe Pitch-Deck | ⬜ Offen |
| Woche 11 | Gegenseitiges Feedback zum Projektstand | ⬜ Offen |
| Ende Woche 14 | Abgabe schriftliche Arbeit + Implementation (24.05.2026) | ⬜ Offen |
| KW 23 | Präsentation des Projekts (10 Min + 10 Min Befragung) | ⬜ Offen |

---

## 13. Communication

- **Code und Kommentare:** Englisch.
- **Kommunikation mit User:** Deutsch.
- **Referenzierung:** Jeder Task referenziert eine GitHub Issue-Nummer.

---

## 14. Current Implementation Status (End of Issue #11)

Issues #1–#11 sind vollständig abgeschlossen:

- **Domain Model:** Alle 5 Kern-Entitäten als MongoDB-DAOs mit Lombok-Annotationen (`@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@NonNull`):
  1. `Portfolio`: Basis für Fondsdaten, enthält `fundManagerId`.
  2. `Holding`: Aktien/Positionen innerhalb eines Portfolios.
  3. `Evidence`: KI-Befunde (News, Sentiment) zu Holdings.
  4. `AuditReport`: Zentrales Workflow-Objekt mit `AuditStatus` Enum.
  5. `AuditComment`: Begründungen des Auditors.

- **Repositories:** Für alle Entitäten existieren `MongoRepository`-Interfaces inklusive Derived Queries (z.B. `findByFundManagerId`, `findByPortfolioId`).

- **Portfolio API (vollständig):** Alle CRUD-Endpoints implementiert (POST, GET, GET/{id}, PUT/{id}, DELETE/{id}).

- **Service Layer:** `PortfolioService` mit vollständiger Ownership-Validierung (nur eigene Portfolios zugänglich).

- **Postman:** Portfolio API Collection dokumentiert und verifiziert.

- **Boards:** Explore-, Create- und Evaluate-Boards in README.md integriert (Issue #12).

**Abgeschlossen in Sprint 4 (Issues #13–#16):** HoldingService mit FK-Validierung, AuditReport State Machine (PENDING_REVIEW → UNDER_REVIEW → APPROVED), MongoDB Aggregation Dashboard.

**Nächste Schritte (Issues #17–#21):** Pitch-Vorbereitung, Global Exception Handler, DTO Validation, Postman-Dokumentation für Holding + AuditReport Service API.

---

## 15. Strategic Roadmap (14 Iterationen, aligned mit Semesterprogramm)

Die Roadmap orientiert sich am offiziellen Semesterprogramm der ZHAW (Vorlesungsinhalte pro Woche) und den pass/fail-Deadlines.

---

### Iteration 1 (KW 38) — Foundation & Setup
**Vorlesung:** Einführung 3SE2, Versionskontrolle, Projektdokumentation

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #1 | Initialize Spring Boot Backend & SvelteKit Frontend | `backend`, `frontend`, `setup` | ✅ Done |
| #2 | Documentation: Design ER-Model and Use Case Diagram | `design`, `documentation` | ✅ Done |
| #3 | Backend: Configure MongoDB Atlas Connection | `backend`, `database` | ✅ Done |
| #4 | Backend: Initial Domain Layer & Repositories | `backend`, `database`, `enhancement` | ✅ Done |
| #5 | API Endpoint: Create Portfolio (POST) | `api`, `backend`, `enhancement` | ✅ Done |
| #6 | API Endpoint: Read All Portfolios (GET) | `api`, `backend`, `enhancement` | ✅ Done |

---

### Iteration 2 (KW 39) — Core APIs & Projektidee
**Vorlesung:** Explore-/Create-/Evaluate-Boards, Backend Teil 1
**⚠️ Deadline:** Besprechung Projektidee mit Dozent (pass/fail)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #7 | API Endpoint: Get Portfolio by ID (GET) | `api`, `backend` | ✅ Done |
| #8 | API Endpoint: Update Portfolio (PUT) | `api`, `backend` | ✅ Done |
| #9 | API Endpoint: Delete Portfolio (DELETE) | `api`, `backend` | ✅ Done |
| #10 | Service Layer: PortfolioService mit Ownership-Validierung | `backend`, `security` | ✅ Done |
| #11 | Postman: Portfolio API Collection dokumentieren | `documentation`, `api` | ✅ Done |

---

### Iteration 3 (KW 40) — Service Layer, State Machine & Aggregation
**Vorlesung:** Backend Teil 2
**⚠️ Deadline:** Abgabe Explore-/Create-/Evaluate-Boards (pass/fail)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #12 | Documentation: Create Explore-, Create- and Evaluate-Boards | `documentation`, `milestone` | ✅ Done |
| #13 | Bug: FK-Validierung beim Erstellen einer Holding | `bug`, `backend` | ✅ Done |
| #14 | Feature: AuditReport assign-Endpoint (PENDING_REVIEW → UNDER_REVIEW) | `enhancement`, `backend` | ✅ Done |
| #15 | Feature: AuditReport complete-Endpoint (UNDER_REVIEW → APPROVED) | `enhancement`, `backend` | ✅ Done |
| #16 | Feature: AuditReport Dashboard Aggregation | `enhancement`, `backend`, `api` | ✅ Done |

---

### Iteration 4 (KW 41) — Pitch-Vorbereitung & Error Handling
**Vorlesung:** Backend Teil 3
**⚠️ Deadline:** Pitch Probelauf (Präsenzpflicht, pass/fail) + Gegenseitige Board-Beurteilung (pass/fail)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #17 | Pitch vorbereiten (3 Min, max 5 Wörter/Slide) | `documentation`, `milestone` | ⬜ |
| #18 | Global Exception Handler: @ControllerAdvice | `backend`, `enhancement` | ⬜ |
| #19 | DTO Validation: jakarta.validation für alle Inputs | `backend`, `enhancement` | ⬜ |
| #20 | Postman: Holding API Collection dokumentieren | `documentation`, `api` | ⬜ |
| #21 | Postman: AuditReport Service API Collection dokumentieren | `documentation`, `api` | ⬜ |

---

### Iteration 5 (KW 42) — Frontend Start & Pitch
**Vorlesung:** Frontend Teil 1 (SvelteKit)
**⚠️ Deadline:** Pitch Semesterprojekt (benotet, 5% Gesamtnote) + Feedback Mitstud. (Präsenzpflicht)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #22 | SvelteKit: Projekt-Setup & Routing-Struktur | `frontend`, `setup` | ⬜ |
| #23 | SvelteKit: Portfolio-Übersichtsseite (List View) | `frontend`, `enhancement` | ⬜ |
| #24 | SvelteKit: Portfolio erstellen (Create Form) | `frontend`, `enhancement` | ⬜ |
| #25 | SvelteKit: Portfolio-Detailseite mit Holdings | `frontend`, `enhancement` | ⬜ |

---

### Iteration 6 (KW 43) — Auth0 Integration
**Vorlesung:** Authentisierung, Benutzerverwaltung
**⚠️ Deadline:** Pitch-Deck abgeben (pass/fail) + Feedback Mitstud. (Präsenzpflicht)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #26 | Pitch-Deck auf Moodle abgeben | `documentation`, `milestone` | ⬜ |
| #27 | Auth0: Backend SecurityFilterChain konfigurieren | `backend`, `security` | ⬜ |
| #28 | Auth0: Frontend Login/Logout Flow | `frontend`, `security` | ⬜ |
| #29 | Auth0: Rollen-Setup (Fund Manager + ESG Auditor) | `backend`, `security` | ⬜ |
| #30 | Backend: @PreAuthorize Rollenschutz auf allen Endpoints | `backend`, `security` | ⬜ |
| #31 | Postman: Auth0 Token-basierte Requests dokumentieren | `documentation`, `api` | ⬜ |

---

### Iteration 7 (KW 44) — Backend Tests Teil 1
**Vorlesung:** Backend Tests: Testing mit JUnit

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #32 | Unit Tests: PortfolioService (Happy Path + Fehler) | `testing`, `backend` | ⬜ |
| #33 | Unit Tests: HoldingService (Happy Path + Fehler) | `testing`, `backend` | ⬜ |
| #34 | Integration Tests: Portfolio-Endpoints mit @MockMvc | `testing`, `backend` | ⬜ |
| #35 | Integration Tests: Holding-Endpoints mit @MockMvc | `testing`, `backend` | ⬜ |
| #36 | JaCoCo: Maven-Plugin konfigurieren & Badge im README | `testing`, `chore` | ⬜ |

---

### Iteration 8 (KW 45) — Backend Tests Teil 2 & AuditComment
**Vorlesung:** Backend Tests: Mocking, Service Testing, CI

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #37 | AuditReport: State Machine Implementierung im Service | `backend`, `enhancement` | ✅ Done |
| #38 | AuditReport: Service API Endpoints (assign, complete, dashboard) | `api`, `backend` | ✅ Done |
| #39 | AuditComment: API Endpoints (Create, Read by Report) | `api`, `backend` | ⬜ |
| #40 | Unit Tests: AuditReportService (State Transitions) | `testing`, `backend` | ⬜ |
| #41 | Integration Tests: Rollenbasierter Zugriff (Fund Manager vs Auditor) | `testing`, `security` | ⬜ |
| #42 | GitHub Actions: CI Pipeline (Build + Test auf Push) | `chore`, `deployment` | ⬜ |

> **Hinweis:** #37 und #38 wurden vorgezogen und in Sprint 4 (Issues #13–#16) implementiert. AuditReportService (assignAuditReport, completeAuditReport, getAuditReportDashboard) und AuditReportServiceController (PUT /assign, PUT /complete, GET /dashboard) sind vollständig vorhanden. Fokus dieser Iteration liegt auf AuditComment (#39) und Tests (#40–#42).

---

### Iteration 9 (KW 46) — Frontend Teil 2
**Vorlesung:** Frontend Teil 2

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #43 | SvelteKit: Audit-Dashboard für ESG Auditor | `frontend`, `enhancement` | ⬜ |
| #44 | SvelteKit: AuditReport-Detailansicht mit Evidence | `frontend`, `enhancement` | ⬜ |
| #45 | SvelteKit: AuditComment-Formular (Auditor-Begründung) | `frontend`, `enhancement` | ⬜ |
| #46 | SvelteKit: Rollenbasierte Navigation (Fund Manager vs Auditor) | `frontend`, `security` | ⬜ |
| #47 | Postman: Audit & Comment API Collection dokumentieren | `documentation`, `api` | ⬜ |

---

### Iteration 10 (KW 47) — Deployment & Coverage
**Vorlesung:** Backend Tests: MockBean, Reports, DevOps & Azure Deployment

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #48 | Dockerfile erstellen (Multi-Stage Build) | `deployment`, `chore` | ⬜ |
| #49 | Azure App Service: Deployment konfigurieren | `deployment`, `chore` | ⬜ |
| #50 | GitHub Actions: CD Pipeline (Build → Push → Deploy) | `deployment`, `chore` | ⬜ |
| #51 | Coverage-Optimierung: Lücken schliessen (Ziel 90%) | `testing`, `backend` | ⬜ |
| #52 | SonarQube: Projekt aufsetzen & CI-Integration | `testing`, `quality` | ⬜ |

---

### Iteration 11 (KW 48) — Spring AI Integration
**Vorlesung:** Spring AI API
**⚠️ Deadline:** Gegenseitiges Feedback zum Projektstand (pass/fail)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #53 | Feedback zum Projektstand vorbereiten & durchführen | `documentation`, `milestone` | ⬜ |
| #54 | Spring AI: Service für News-Analyse konfigurieren | `backend`, `ai` | ⬜ |
| #55 | Spring AI: Evidence-Generierung aus News-Analyse | `backend`, `ai` | ⬜ |
| #56 | Spring AI: KI-Zusammenfassung für AuditReport | `backend`, `ai` | ⬜ |
| #57 | Evidence: API Endpoints (Read by Holding) | `api`, `backend` | ⬜ |
| #58 | Unit Tests: AI Service + Evidence Service | `testing`, `ai` | ⬜ |

---

### Iteration 12 (KW 49) — Drittsysteme & Code-Analyse
**Vorlesung:** Anbinden von Drittsystemen, Code-Analyse

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #59 | Externe News-API: Integration (z.B. NewsAPI, GNews) | `backend`, `integration` | ⬜ |
| #60 | SvelteKit: Evidence-Ansicht in Portfolio-Detail | `frontend`, `enhancement` | ⬜ |
| #61 | SonarQube: Code Smells & Bugs beheben | `quality`, `chore` | ⬜ |
| #62 | Postman: Evidence & AI Endpoints dokumentieren | `documentation`, `api` | ⬜ |
| #63 | SvelteKit: Loading States & Error Handling | `frontend`, `enhancement` | ⬜ |

---

### Iteration 13 (KW 50) — E2E Tests & Polish
**Vorlesung:** MCP Server und Client, Frontend Tests: End-to-End Testing

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #64 | Cypress: Setup & Konfiguration | `testing`, `frontend` | ⬜ |
| #65 | Cypress: E2E Test – Portfolio CRUD Flow (Fund Manager) | `testing`, `frontend` | ⬜ |
| #66 | Cypress: E2E Test – Audit Workflow (Auditor) | `testing`, `frontend` | ⬜ |
| #67 | Cypress: E2E Test – Login & Rollenbasierter Zugriff | `testing`, `security` | ⬜ |
| #68 | Postman: Vollständige API-Dokumentation veröffentlichen | `documentation`, `api` | ⬜ |
| #69 | README.md: Implementation-Kapitel mit Screenshots | `documentation` | ⬜ |

---

### Iteration 14 (KW 51) — Final Polish & Abgabe
**Vorlesung:** Betrieb von Web-Applikationen
**🔴 Deadline:** Abgabe schriftliche Arbeit + Implementation (24.05.2026)

| Issue # | Titel | Labels | Status |
|---------|-------|--------|--------|
| #70 | README.md: Finalisierung aller Kapitel (Einleitung, Anforderungen, Fazit) | `documentation`, `milestone` | ⬜ |
| #71 | README.md: JaCoCo Badge + SonarQube Badge einbinden | `documentation`, `chore` | ⬜ |
| #72 | Dozenten als Collaborators hinzufügen (Repo + Project) | `chore`, `milestone` | ⬜ |
| #73 | Abgabe-Artefakte erstellen (Zips, URLs, Credentials) | `chore`, `milestone` | ⬜ |
| #74 | Smoke Test: Deployment auf Azure verifizieren | `testing`, `deployment` | ⬜ |
| #75 | Abschlusspräsentation vorbereiten (10 Min + Live-Demo) | `documentation`, `milestone` | ⬜ |

---

### Präsentation (KW 23 — 01.06.–05.06.2026)

| Titel | Details |
|-------|---------|
| Abschlusspräsentation | 10 Min Präsentation + 10 Min Befragung durch Dozierende |
| Inhalt | Ziel der Anwendung (1 Min), Live-Demo (mind. 2 Rollen), Projektstand (1 Min) |
| Wichtig | Live-Demo, kein Video! |

---

## Anhang: Label-Übersicht

| Label | Farbe | Verwendung |
|-------|-------|------------|
| `backend` | 🔵 | Spring Boot Backend-Code |
| `frontend` | 🟣 | SvelteKit Frontend-Code |
| `api` | 🟢 | REST-Endpoint Implementierung |
| `security` | 🔴 | Auth0, Rollen, Ownership |
| `testing` | 🟡 | JUnit, Mockito, Cypress, JaCoCo |
| `database` | 🟠 | MongoDB, Repository-Layer |
| `deployment` | ⚪ | Docker, Azure, GitHub Actions |
| `documentation` | 📄 | README, Postman, Boards, Pitch |
| `design` | 🎨 | UC-Diagramm, ER-Modell, Mockups |
| `enhancement` | 💡 | Neue Features, Erweiterungen |
| `chore` | 🔧 | Wartung, Konfiguration |
| `ai` | 🤖 | Spring AI Integration |
| `quality` | 📊 | SonarQube, Code-Analyse |
| `integration` | 🔗 | Drittsysteme, externe APIs |
| `milestone` | 🏁 | Pass/Fail Deadlines, Abgaben |
| `setup` | ⚙️ | Projekt-Initialisierung |
