# Testing and quality

## Available checks

| Area | Command | Notes |
| --- | --- | --- |
| Backend verification | `cd backend; .\mvnw.cmd verify` | Runs Maven tests, creates JaCoCo output, and enforces configured core-service coverage thresholds. |
| Frontend type/Svelte checks | `cd frontend; npm run check` | Requires installed frontend dependencies. |
| Frontend production build | `cd frontend; npm run build` | Requires frontend environment variables. |
| Cypress E2E | `cd frontend; npm run test:e2e` | Covers anonymous route protection and, when role secrets are supplied, a complete three-role business lifecycle. |
| Docker images | See [README](../README.md#3-optional-docker-builds) | Requires Docker, which is not bundled with this repository. |

GitHub Actions runs frontend checks, backend verification, and a full-stack Cypress environment backed by
an isolated MongoDB service. On `main`, protected Auth0 test credentials are required and Cypress executes
the complete lifecycle: fund-manager creation and submission, auditor assignment/evidence/comment/approval,
compliance verification, and negative cross-role authorization checks. Pull requests without repository
secrets still execute all anonymous tests and skip only the protected lifecycle.

## Local validation record

The release-hardening validation record is updated whenever the public deployment changes:

| Check | Result |
| --- | --- |
| `npm audit --omit=dev` | Passed with 0 vulnerabilities after replacing Axios with the platform Fetch API. |
| `npm audit` | Passed with 0 vulnerabilities, including development tooling. |
| `npm run check` | Passed with 0 errors and 0 warnings. |
| `npm run build` | Passed for the production Node adapter. |
| `backend\\mvnw.cmd --batch-mode verify` | Passed: 483 tests and all configured JaCoCo thresholds. |
| `npm run test:e2e` | Passed locally: 24 anonymous/UI checks; 10 credential-dependent checks skipped. CI supplies protected three-role credentials and an isolated backend/database. |
| Docker builds | Not run because Docker was unavailable in the local environment. |

The pull request records these results as well. Environment-dependent checks should be rerun after changing dependencies, services, or authentication configuration.
