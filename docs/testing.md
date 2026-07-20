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

The pull request for the portfolio cleanup records the exact checks run in its description. Do not infer a pass from this document alone; environment-dependent checks should be rerun after changing dependencies, services, or authentication configuration.
