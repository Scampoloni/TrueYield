# Architecture

## Purpose

TrueYield combines a role-oriented web interface with an API that stores portfolios, evidence, and audit-workflow data. It explores an evidence-first review process in which AI and external news sources can assist a human reviewer.

## Components

```text
Browser
  -> SvelteKit frontend (port 5173 in local development)
  -> Auth0 authentication and role claim
  -> Spring Boot API (port 8080)
  -> MongoDB

Spring Boot API
  -> optional Anthropic API
  -> optional news providers
```

The SvelteKit server reads the Auth0 configuration from private environment variables and stores authentication tokens in HTTP-only cookies. API route handlers forward the bearer token to Spring Boot. The backend is configured as an OAuth2 resource server, validates the JWT, and maps `user_roles` claim values to Spring Security roles.

## Workflow and roles

| Role | Prototype responsibilities |
| --- | --- |
| Fund manager | Creates portfolios and holdings, adds evidence, and submits work for review. |
| Auditor | Reviews portfolio evidence and audit reports, adds comments, and records an outcome. |
| Compliance officer | Views read-only, cross-portfolio summaries and reports. |

The application requires an authenticated user. Roles are assigned in Auth0 rather than selected by the user in the interface. Role names expected by the backend are `fund-manager`, `auditor`, and `compliance-officer`.

## Data and controls

MongoDB stores portfolio, holding, evidence, audit-report, audit-event, and audit-comment records. The backend applies authenticated access to `/api/**` and role checks at the controller layer. CORS and JWT issuer settings must match the local or hosted deployment.

This architecture is a prototype design. It has not been assessed as a production compliance architecture.
