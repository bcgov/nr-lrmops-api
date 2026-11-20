# ADR 0001: Run as an API only, without the template's frontend or Postgres

- **Status:** Proposed
- **Date:** 2026-11-19
- **Deciders:** SodhiA1
- **Product team:** [team or product name]
- **Related:** [bcgov/quickstart-openshift](https://github.com/bcgov/quickstart-openshift) (the template this repository was created from)

## Context

This repository was created from `bcgov/quickstart-openshift`, which deploys three components: a React/Vite frontend served by Caddy, a NestJS backend, and a Postgres database with Flyway migrations, nightly S3 backups and SchemaSpy documentation. The template also ships an example `/api/v1/users` endpoint backed by a `users` table.

The LRM Ops API serves the CBR team. It reads data held in the TFM Oracle database (road search and lookup, LRMOPS details for a CBR site, road details at a location). It has no user interface and stores no data of its own. Access to Oracle is through a read-only proxy user and role, created by the DBA scripts in [`scripts/db/`](../../scripts/db/).

## Decision

We will run this repository as a single API with no frontend, no Postgres database and no Flyway migrations. The backend's OpenShift Route is the public entry point.

## Options considered

### Option 1: Keep the template as is

- Pros: No changes to template-managed files, so template updates are easier to pull in.
- Cons: Deploys and maintains a frontend and a database that nothing uses. It adds attack surface, OpenShift quota, image builds, secrets (`DB_PASSWORD`, S3 credentials) and Renovate updates, and an example `users` endpoint that would be exposed in PROD.

### Option 2: Remove the frontend, Postgres and the example endpoint (chosen)

- Pros: Only what the API needs is built, deployed and patched. Fewer secrets and less quota. Easier to see how it works.
- Cons: The repository differs from the template, so template updates must be merged by hand. The Caddy reverse proxy and its Coraza WAF and security headers, which sat in front of the backend, are gone.

## Consequences

- **Positive:** One image (`backend`) per pull request. Deploys apply only `common/openshift.init.yml` (NetworkPolicies) and `backend/openshift.deploy.yml`. No database secrets or backup configuration to manage.
- **Negative:** No WAF in front of the API.
- **Follow-up work:**
  - Decide whether the API needs a WAF or extra security headers now that the API is no longer behind Caddy.
  - Set the `SONAR_PROJECT_KEY_BACKEND` variable once the SonarCloud project is provisioned.

## Notes

What was removed: `frontend/`, `migrations/`, `common/openshift.database.yml`, `common/openshift.backup.yml`, Prisma (`backend/prisma/`, `prisma.config.ts`, `src/prisma.*`) and the `pg` packages, the `backend/src/users/` example module, the `database`, `migrations`, `schemaspy`, `frontend` and `caddy` Docker Compose services, and the matching workflow jobs (frontend tests, E2E tests, database and backup deploys, SchemaSpy).
