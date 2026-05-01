# TrueYield – Abgabe-Checkliste & Nachweise (Note 6 Readiness)

## 🔗 Wichtige Links & Deployment

| Ressource | URL | Bemerkung |
|---|---|---|
| **GitHub Repository** | [GitHub Repo URL hier einfügen] | Beinhaltet Frontend, Backend und Dokumentation. |
| **Frontend (Azure)** | [https://trueyield-frontend.azurewebsites.net](https://trueyield-frontend.azurewebsites.net) | Produktivsystem |
| **Backend API (Azure)**| [https://trueyield-backend.azurewebsites.net](https://trueyield-backend.azurewebsites.net) | Produktivsystem API |
| **Backend Health** | [https://trueyield-backend.azurewebsites.net/actuator/health](https://trueyield-backend.azurewebsites.net/actuator/health) | Status der API |
| **API Dokumentation** | [https://documenter.getpostman.com/view/52455816/2sBXqJMMCz](https://documenter.getpostman.com/view/52455816/2sBXqJMMCz) | Postman Public Workspace |
| **Swagger UI** | [https://trueyield-backend.azurewebsites.net/swagger-ui.html](https://trueyield-backend.azurewebsites.net/swagger-ui.html) | OpenAPI 3 Spec |
| **SonarCloud Report** | [SonarCloud URL hier einfügen] | Sofern vorhanden |
| **Roadmap & History** | [GitHub Projects / Issues URL einfügen] | Backlog und Sprints |

---

## 🔐 Login-Daten für die Dozierenden
> **WICHTIG:** Keine Passwörter im Repository speichern! 
> Bitte reiche ein separates Dokument (PDF oder via Moodle-Kommentarfeld) ein, welches diese Logins enthält:

* **Rolle: Fund Manager**
  * E-Mail: `[E-Mail des Test-Fund-Managers]`
  * Passwort: `[Passwort]`
* **Rolle: ESG Auditor**
  * E-Mail: `[E-Mail des Test-Auditors]`
  * Passwort: `[Passwort]`
* **Rolle: Compliance Officer**
  * E-Mail: `[E-Mail des Test-Compliance-Officers]`
  * Passwort: `[Passwort]`

*Hinweis: Bitte füge die Dozierenden auch als "Collaborators" zu deinem GitHub-Repository und dem dazugehörigen GitHub Project (Board) hinzu, falls es privat ist.*

---

## 🚀 Demo-Szenario für die Bewertung (Happy Path)
1. **Fund Manager Login:** Einloggen als Fund Manager.
2. **Portfolio & Holding anlegen:** Ein neues Portfolio erstellen und 2-3 Holdings hinzufügen (am besten echte Firmennamen, die in den News vorkommen, z.B. "Tesla", "Shell").
3. **Audit triggern:** Auf "Audit anfordern" klicken (Status geht von `DRAFT` (falls vorhanden) bzw. direkt zu `AI_ANALYZING` → `PENDING_REVIEW`). KI-Risikozusammenfassung und Evidence (Nachrichten) werden dabei automatisch geladen.
4. **Auditor Login:** Einloggen als ESG Auditor.
5. **Dashboard prüfen:** Das Audit-Dashboard öffnen, das soeben eingereichte Portfolio in der Queue finden.
6. **Assign & Review:** Den Report aufrufen, auf "Assign to me" klicken (Status → `UNDER_REVIEW`). Die generierte AI Risk Summary und die Evidence-Einträge anschauen.
7. **Entscheidung fällen:** Einen Kommentar hinzufügen und den Report mit "Approve" oder "Reject" abschliessen.
8. **Compliance Officer Login:** Einloggen als Compliance Officer und das Compliance Dashboard prüfen (Zahlen müssen sich entsprechend der getroffenen Entscheidung aktualisiert haben).

---

## 🛠️ Technische Abgabe-Artefakte
Die folgenden Artefakte sollten idealerweise in Moodle mit abgegeben werden (als `.zip`):
- [ ] `jacoco.zip` (Inhalt von `backend/target/site/jacoco`) – belegt die >90% Test-Coverage
- [ ] `surefire-reports.zip` (Inhalt von `backend/target/surefire-reports`) – belegt die grünen Unit/Integration-Tests
- [ ] `Anforderungen-Projektarbeit-3SE2.pdf` (oder eine Checkliste der abgedeckten Kriterien)
