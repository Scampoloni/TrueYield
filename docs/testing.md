# Testing and quality

## Available checks

| Area | Command | Notes |
| --- | --- | --- |
| Backend verification | `cd backend; .\mvnw.cmd verify` | Runs Maven tests, creates JaCoCo output, and enforces configured core-service coverage thresholds. |
| Frontend type/Svelte checks | `cd frontend; npm run check` | Requires installed frontend dependencies. |
| Frontend production build | `cd frontend; npm run build` | Requires frontend environment variables. |
| Cypress E2E | `cd frontend; npm run test:e2e` | Requires an appropriate running frontend/backend setup; authenticated paths need Auth0 test users. |
| Docker images | See [README](../README.md#3-optional-docker-builds) | Requires Docker, which is not bundled with this repository. |

GitHub Actions runs frontend check/build, an unauthenticated Cypress pass, and backend Maven verification. The CI workflow uses placeholders for frontend Auth0 settings and a MongoDB service container for backend tests.

## Local validation record

On 2026-07-20, the following checks were attempted for the portfolio-publication cleanup:

| Check | Result |
| --- | --- |
| `npm ci` | Passed; npm reported dependency-audit findings that were not changed as part of this documentation-focused cleanup. |
| `npm run check` | Passed with 0 errors and 0 warnings. |
| `npm run build` | Passed. |
| `backend\\mvnw.cmd --batch-mode verify` | Maven reported `BUILD SUCCESS`, 480 tests with 0 failures/errors, and all JaCoCo coverage checks met. The outer execution harness timed out shortly after Maven printed its success summary, so its process status should not be read as a clean harness exit. |
| `npm run test:e2e` | Could not run because no frontend server was listening on Cypress's configured `http://localhost:5173` base URL. |
| Docker builds | Not run because Docker was unavailable in the local environment. |

The pull request records these results as well. Environment-dependent checks should be rerun after changing dependencies, services, or authentication configuration.
