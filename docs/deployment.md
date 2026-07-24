# Deployment and public-demo safety

The repository contains GitHub Actions workflows for CI and Azure App Service deployment. Deployment credentials and service configuration are referenced through GitHub Actions secrets; no values should be committed to the repository.

## Current production controls

- Both Azure App Services use `Always On` to prevent long idle cold starts.
- The backend health check targets `/actuator/health`, including an application-database ping.
- The frontend health check targets `/login`.
- HTTPS-only transport, TLS 1.2 minimum, and disabled FTPS are enforced on both applications.
- CD deploys the exact commit that passed CI and then verifies backend readiness, the login page, and an anonymous API rejection.
- The backend validates both the Auth0 issuer and the intended API audience.

## Before sharing a live deployment

- Confirm that Auth0 allows only intended users and role assignments.
- Review whether anonymous visitors can create accounts, alter sample data, or reach protected APIs.
- Verify that Anthropic and news-provider calls have cost limits and suitable monitoring.
- Review MongoDB network access, application logs, health/actuator exposure, CORS, and error responses.
- Confirm screenshots and seeded data contain no personal, customer, or test-account information.
- Rotate any credentials that may have appeared in a past commit, then assess whether Git history needs remediation.

The main README links the hosted login page, but credentials remain private. This permits a controlled
review while preventing anonymous writes and unrestricted use of provider-backed features.
