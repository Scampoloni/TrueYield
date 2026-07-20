# Deployment and public-demo safety

The repository contains GitHub Actions workflows for CI and Azure App Service deployment. Deployment credentials and service configuration are referenced through GitHub Actions secrets; no values should be committed to the repository.

## Before sharing a live deployment

- Confirm that Auth0 allows only intended users and role assignments.
- Review whether anonymous visitors can create accounts, alter sample data, or reach protected APIs.
- Verify that Anthropic and news-provider calls have cost limits and suitable monitoring.
- Review MongoDB network access, application logs, health/actuator exposure, CORS, and error responses.
- Confirm screenshots and seeded data contain no personal, customer, or test-account information.
- Rotate any credentials that may have appeared in a past commit, then assess whether Git history needs remediation.

Because this review cannot be established from repository contents alone, the main README does not promote a public demo URL. The supplied screenshots communicate the prototype safely without inviting uncontrolled use of hosted services.
