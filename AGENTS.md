# AGENTS.md

Repository facts for automated coding assistants. Teams may edit or remove this file.

## Layout
- `backend/` (Spring Boot 3.5, Java 21, Maven; package `ca.bc.gov.nrs.lrmops`) — API only; no frontend and no Postgres. Reads the TFM Oracle schema read-only as `PROXY_FSA_LRM_READ_WRITE_USER`; callers (several apps, server to server or from the browser) present Keycloak-issued JWTs; access per endpoint by OAuth2 scope (`security/ApiScopes`, `@PreAuthorize`), scopes created per realm at deploy by `.github/scripts/ensure-keycloak-scopes.sh` (keep its list in step with `ApiScopes`); browser origins via `ALLOWED_ORIGINS`; OpenAPI via springdoc (`/swagger-ui/index.html`, `/v3/api-docs`, anonymous) — annotate controllers with `@Tag`, `@Operation` and `@SecurityRequirement(name = "bearerAuth")`
- OpenShift templates (not Helm): `common/openshift.init.yml` (NetworkPolicies), `backend/openshift.deploy.yml` (includes the public Route)
- Workflows: `.github/workflows/` (PR deploys via `reusable-deploy.yml`, tests via `reusable-tests.yml`); integration and load tests in `common/tests/`
- Stay on Java 21: Java 24+ fails the Oracle TCPS handshake (see `renovate.json`, `OracleTlsCompatibility`)

## Build, test, run
- Full stack locally: `docker compose up`
- Backend (`cd backend`): `mvn -B -DskipITs verify` (build, unit tests, Jacoco coverage); `SPRING_PROFILES_ACTIVE=local mvn spring-boot:run` to run
- Deploys to OpenShift run from GitHub Actions (PR open, merge), not from a workstation

## Shared actions
- Uses bcgov shared actions and workflows (`bcgov/action-*`, `bcgov/actions/*`, `bcgov/actions-openshift/*`, `bcgov/quickstart-openshift-helpers`). Use them as provided; don't copy or fork them.
- Never pin `@main`. Pin bcgov shared actions to a published release SHA with a `# vX.Y.Z` comment.
- Vanity Route TLS (`bcgov/actions-openshift/cert-keeper`) is strictly managed via the standalone workflow `.github/workflows/cert-keeper.yml` (daily schedule and `workflow_dispatch`, runs only when `vars.ROUTE_HOST` is set, uses the `prod` environment). Never embed `cert-keeper` into `pr-open.yml`, `merge.yml`, or continuous deployment pipelines.

## Settings
- Automated agents must not change repository or organization settings. Settings, environments, secrets and deploy tokens are set by hand by a person on the team (see the README's First-Time Setup).
