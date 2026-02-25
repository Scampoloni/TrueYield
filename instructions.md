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

- JUnit 5
- Mockito
- JaCoCo
- Ziel: ~90% Testabdeckung

### Deployment

- Dockerfile
- Azure App Service
- GitHub Actions (CI/CD Pipeline)

> Diese Constraints dürfen niemals durch Refactoring oder Optimierung verletzt werden.

---

## 3. Non-Functional Constraints (Security & Quality)

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

## 4. Domain Model & Ownership Rules

### 4.1 Data Isolation Rule

Ein `Fund Manager` darf ausschließlich auf Portfolios zugreifen, bei denen:

```text
portfolio.fundManagerId == authenticatedUserId
```

- Diese Validierung erfolgt zwingend serverseitig.
- Kein Zugriff darf ausschließlich durch Frontend-Filterung kontrolliert werden.

---

### 4.2 Collections (Strict)

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

### 4.3 State Machine (AuditReport)

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

## 5. Architectural Standards

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

## 6. Testing – Definition of Done (DoD)

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

## 7. AI Agent Working Rules

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

## 8. Git & Sprint Governance

- Projekt ist in **14 wöchentliche Sprints** unterteilt.
- Jedes Feature wird als GitHub Issue erfasst.
- Jedes Issue enthält:
  - Zielbeschreibung
  - Akzeptanzkriterien
  - Testkriterien

### Branch Naming
`feature/issue-<nr>-<titel>`

### Commit Convention
- `feat`: neue Features
- `fix`: Bugfixes
- `chore`: Wartung
- `refactor`: Strukturverbesserung

### Merge-Kriterien
Merge ist nur erlaubt, wenn:
1. Alle Tests erfolgreich sind.
2. Coverage nicht gesunken ist.
3. Keine Secrets im Code enthalten sind.

---

## 9. Strategic Roadmap (14 Sprints)

- **Sprint 1–2: Foundation**
  - Backend Setup, MongoDB Atlas Anbindung, Use-Case und ER-Diagramme.
- **Sprint 3–4: Core Models**
  - Portfolio API, Holding API, DTO-Struktur.
- **Sprint 5–6: Security**
  - Auth0 Integration, Rollenbasierte Zugriffskontrolle.
- **Sprint 7–9: Testing & Workflow**
  - State Machine Implementierung, JUnit & Mockito Tests, Coverage-Optimierung.
- **Sprint 10–11: AI Integration**
  - Spring AI Anbindung, News-Analyse, Evidence-Generierung.
- **Sprint 12: Deployment**
  - Dockerfile, Azure App Service, GitHub Actions Pipeline.
- **Sprint 13–14: Final Polish**
  - README Finalisierung, Postman-Dokumentation, Präsentationsvorbereitung.

---

## 10. Communication

- **Code und Kommentare:** Englisch.
- **Kommunikation mit User:** Deutsch.
- **Referenzierung:** Jeder Task referenziert eine GitHub Issue-Nummer.