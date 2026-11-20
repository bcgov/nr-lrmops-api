[![Apache 2.0 License](https://img.shields.io/github/license/bcgov/nr-lrmops-api.svg)](/LICENSE)
[![Merge](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml)
[![Analysis](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml)
[![Scheduled](https://github.com/bcgov/nr-lrmops-api/actions/workflows/scheduled.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/scheduled.yml)

# LRM Ops API

A read-only API that gives the CBR team access to LRM data held in the TFM Oracle database: road search and lookup, LRMOPS details for a CBR site, and road details at a location.

It is a Spring Boot (Java 21) service, deployed to OpenShift through GitHub Actions. Several applications use it, from their backends or directly from the browser. Every call carries a Keycloak-issued JWT, and each endpoint requires an OAuth2 scope that the calling client must be assigned in Keycloak (see [Consuming the API](#consuming-the-api--provision-your-own-keycloak-client)); browser apps also need their origin in `ALLOWED_ORIGINS` (CORS). It was created from [bcgov/quickstart-openshift](https://github.com/bcgov/quickstart-openshift); the frontend, Postgres database and Flyway migrations from that template have been removed.

## Repository Layout

| Path | Contents |
|------|----------|
| [`backend/`](./backend/) | Spring Boot API (Maven), its `Dockerfile` and OpenShift template (`openshift.deploy.yml`, including the public Route) |
| [`common/openshift.init.yml`](./common/openshift.init.yml) | NetworkPolicies applied before each deploy |
| [`common/tests/`](./common/tests/) | Integration tests (run against deployed environments) and k6 load tests |
| [`scripts/db/`](./scripts/db/) | Oracle (TFM) role, proxy user and grant scripts, deployed by a DBA; see [`scripts/db/readme.txt`](./scripts/db/readme.txt) |
| [`monitoring/alerts/`](./monitoring/alerts/) | Sysdig alert templates synced to PROD |
| [`docs/adr/`](./docs/adr/) | Architecture decision records |
| [`.github/workflows/`](./.github/workflows/) | CI/CD pipelines |

## Local Development

Requires Java 21 (not newer: Java 24+ cannot complete the Oracle TCPS handshake) and Maven 3.9, plus the BC Gov VPN to reach the database.

1. Put the database and Keycloak values in `backend/src/main/resources/application-local.yml` (gitignored). The names are in [`backend/.env.example`](./backend/.env.example).
2. Put the Oracle TCPS truststore at `backend/src/main/resources/cert/jssecacerts` (gitignored) and point `TRUSTSTORE_PATH` at it.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run   # http://localhost:8080
```

Or with Docker: `docker compose up` (serves the API on http://localhost:8080).

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Health check (anonymous) |
| `/actuator/health/liveness`, `/actuator/health/readiness` | OpenShift probes (anonymous) |
| `/actuator/prometheus` | Prometheus metrics (anonymous) |
| `/swagger-ui/index.html` | Swagger UI (anonymous); use **Authorize** to paste a Keycloak access token (no `Bearer ` prefix) and try the endpoints |
| `/v3/api-docs` | OpenAPI JSON (anonymous) |
| `/api/v1/**` | The API; needs a Bearer token carrying the endpoint's scope |

### Build and Test

```bash
cd backend
mvn -B -DskipITs verify   # compile, unit tests, Jacoco coverage (target/site/jacoco)
```

## Consuming the API — provision your own Keycloak client

This service does **not** hand out credentials. Each consuming team registers its own client in the same Keycloak realm as this API (the realm behind `KEYCLOAK_ISSUER_URI`) and calls with a token that carries the scopes it needs.

**Division of ownership:** this repo creates the *client scopes* in each realm at deploy time (`.github/scripts/ensure-keycloak-scopes.sh`, run by `reusable-deploy.yml` when `KEYCLOAK_SA_CLIENT_ID` is set). Assigning them to *your* client is yours to do — we never touch consumer clients.

1. **Create a client** in the realm. For a backend, a confidential service-account client (`publicClient: false`, `serviceAccountsEnabled: true`, no browser flows) calling with `client_credentials`. A browser app calling directly also needs its origin in `ALLOWED_ORIGINS`.
2. **Assign the scopes you need** as **default** client scopes (not *optional*), so every token carries them without your code asking. Request only the endpoints you call:
   - `lrmops:roads:read` — road search, and a road section by road and section id
   - `lrmops:road-locations:read` — what LRMOPS would assign at a coordinate
   - `lrmops:cbr-sites:read` — LRMOPS's details for a CBR crossing site

   The scopes must already exist in that realm — if one is missing, this API hasn't been deployed to that environment yet (or its scope step was skipped).
3. **Mint a token** and send it as a bearer token:

   ```bash
   TOKEN=$(curl -sS -X POST "$KEYCLOAK_ISSUER_URI/protocol/openid-connect/token" \
     -d grant_type=client_credentials \
     -d client_id="$YOUR_CLIENT_ID" \
     --data-urlencode "client_secret=$YOUR_CLIENT_SECRET" | jq -r .access_token)

   curl -sS -H "Authorization: Bearer $TOKEN" "$LRMOPS_API_BASE_URL/api/v1/..."
   ```

   Tokens are short-lived — cache and refresh shortly before expiry rather than fetching one per request. Missing or expired token → **401**; valid token without the endpoint's scope → **403**.

Keep one client per app per environment, and keep the client secret in a secret store (OpenShift `Secret`, GitHub Actions secret), never in the image or repo.

## Environments

| Environment | Trigger | URL |
|-------------|---------|-----|
| PR | Pull request opened or updated | `https://nr-lrmops-api-<pr#>.apps.gold.devops.gov.bc.ca` |
| TEST | Merge to `main` | `https://nr-lrmops-api-test.apps.gold.devops.gov.bc.ca` |
| PROD | None until go-live (see below) | `https://nr-lrmops-api-prod.apps.gold.devops.gov.bc.ca` |

Each environment lives in its own OpenShift namespace: `<plate>-dev` (PRs), `<plate>-test` and `<plate>-prod`.

## CI/CD

| Workflow | Runs on | What it does |
|----------|---------|--------------|
| [`pr-open.yml`](./.github/workflows/pr-open.yml) | Pull request | Builds the backend image to ghcr.io, deploys a PR environment, runs integration tests, comments the deployment link |
| [`pr-validate.yml`](./.github/workflows/pr-validate.yml) | Pull request | Enforces a Conventional Commits PR title |
| [`analysis.yml`](./.github/workflows/analysis.yml) | Pull request, merge | Maven build, unit tests with coverage, SonarCloud (optional), Trivy scan |
| [`pr-close.yml`](./.github/workflows/pr-close.yml) | Pull request closed | Removes the PR environment and retags images |
| [`merge.yml`](./.github/workflows/merge.yml) | Merge to `main` | Deploys TEST and runs tests (the PROD jobs are commented out for now) |
| [`scheduled.yml`](./.github/workflows/scheduled.yml) | Saturdays, or by hand | Purges stale PR environments, ZAP scan and k6 load test against TEST |
| [`cert-keeper.yml`](./.github/workflows/cert-keeper.yml) | Daily, or by hand | Keeps the PROD vanity URL certificate valid; does nothing until `ROUTE_HOST` is set |

Nothing deploys PROD until go-live: the PROD jobs in `merge.yml` are commented out, and there is no release workflow. To enable PROD, uncomment `deploy-prod`, `monitor-prod` and `promote` together, add them back to the `results` job's `needs`, and work through the checklist in the comment above them first.

## First-Time Setup

1. **OpenShift namespaces**: request a project set (DEV, TEST, PROD) from Platform Services at the [registry](https://registry.developer.gov.bc.ca). This gives a licence plate, e.g. `abc123`.
2. **Create the deploy tokens by hand.** In each namespace, a `github-actions` service account with `edit`, and its token as the `OC_TOKEN` secret (repository for `<plate>-dev`, the `test` and `prod` environments for the others):
    ```bash
    oc project <plate>-test
    oc create sa github-actions
    oc create rolebinding github-actions-edit --clusterrole=edit --serviceaccount=<plate>-test:github-actions
    oc create token github-actions --duration=87600h   # paste into GitHub as OC_TOKEN
    ```
3. **Create the `test` and `prod` GitHub environments** and set the variables and secrets below. Protect `main` with a ruleset requiring the `PR Results`, `Analysis Results` and `Validate Results` checks.
4. **Open a first pull request** to run the pipeline, then make the `backend` package public under the repository's Packages so OpenShift can pull it.

### Secrets and Variables

| Environment | Name | Description |
|-------------|------|-------------|
| repository | `vars.OC_SERVER` | OpenShift API server URL, e.g. `https://api.gold.devops.gov.bc.ca:6443` |
| repository | `vars.OC_NAMESPACE` | PR namespace, `<plate>-dev` |
| repository | `secrets.OC_TOKEN` | Deploy token for `<plate>-dev` |
| test | `vars.OC_NAMESPACE` | `<plate>-test` |
| test | `secrets.OC_TOKEN` | Deploy token for `<plate>-test` |
| prod | `vars.OC_NAMESPACE` | `<plate>-prod` |
| prod | `secrets.OC_TOKEN` | Deploy token for `<plate>-prod` |
| each | `vars.KEYCLOAK_SA_ISSUER_URI` | Keycloak realm issuer that callers' tokens come from, e.g. `https://test.loginproxy.gov.bc.ca/auth/realms/<realm>` |
| each (optional) | `vars.ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API directly (CORS), e.g. `https://app.gov.bc.ca`; empty allows none |
| each | `secrets.DATABASE_HOST` | TFM Oracle host (DBQ05 for PRs and TEST, DBP05 for PROD) |
| each | `secrets.DATABASE_SERVICE_NAME` | Oracle service name |
| each | `secrets.DATABASE_USER` | `PROXY_FSA_LRM_READ_WRITE_USER` |
| each | `secrets.DATABASE_PASSWORD` | Its password |
| each | `secrets.KEYSTORE_SECRET` | Password for the TCPS truststore the init container builds |
| each (optional) | `secrets.KEYCLOAK_SA_CLIENT_ID`, `secrets.KEYCLOAK_SA_CLIENT_SECRET` | Service account with the realm-management `manage-clients` role, used at deploy to create the API's client scopes; the step is skipped when unset |
| repository (optional) | `secrets.SONAR_TOKEN_BACKEND`, `vars.SONAR_PROJECT_KEY_BACKEND` | SonarCloud analysis; skipped unless both are set |
| prod (optional) | `secrets.SYSDIG_API_TOKEN` | Sysdig alert sync; skipped with a warning when unset |
| repository (optional) | `vars.ROUTE_HOST` | Vanity hostname, e.g. `lrmops-api.gov.bc.ca` |

### Recommended: Restrict PROD Deployments

Under `Settings > Environments > prod > Deployment branches and tags`, choose **Selected branches and tags** and allow `main`. This stops pull-request jobs from reading PROD secrets.

## Optional Integrations

**SonarCloud.** Request a project through [devops-requests](https://github.com/bcgov/devops-requests/issues/new/choose) ("Request for import GitHub repository on SonarCloud"). Then save the token as the `SONAR_TOKEN_BACKEND` secret and the project key as the `SONAR_PROJECT_KEY_BACKEND` variable. Analysis runs without them.

**Renovate.** [`renovate.json`](./renovate.json) extends [bcgov/renovate-config](https://github.com/bcgov/renovate-config). BC Government teams can request the Renovate and Renovate Approve GitHub Apps through the [CITZ Service Desk](https://citz-do.atlassian.net/servicedesk/customer/portal/2/group/9/create/10).

**Sysdig.** Set `SYSDIG_API_TOKEN` in the `prod` environment (from the Secret created by your `SysdigTeam` in `<plate>-tools`). The templates in [`monitoring/alerts/`](./monitoring/alerts/) are synced on every PROD deploy; see [bcgov/action-sysdig-monitor](https://github.com/bcgov/action-sysdig-monitor) for the format.

**Vanity URL.** Point the hostname's DNS at the cluster router, then `gh variable set ROUTE_HOST --body '<hostname>'`. [`cert-keeper.yml`](./.github/workflows/cert-keeper.yml) then issues and renews a Let's Encrypt certificate on the `nr-lrmops-api-vanity-url` Route. See the [cert-keeper README](https://github.com/bcgov/actions-openshift/blob/main/cert-keeper/README.md) for limits and troubleshooting.

## Security

Deployments run with read-only root filesystems, as non-root, with privilege escalation blocked, all capabilities dropped and the default seccomp profile. If the app needs to write at runtime, mount a memory-backed `emptyDir` with a `sizeLimit` (as `/tmp` already is in [`backend/openshift.deploy.yml`](./backend/openshift.deploy.yml)) rather than relaxing the security context.

NetworkPolicies allow traffic to the backend only from the OpenShift router. Trivy, CodeQL and a weekly OWASP ZAP scan report to the repository's Security tab.

See [SECURITY.md](./SECURITY.md) for vulnerability reporting and the OWASP ASVS mapping.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md). Pull requests need a Conventional Commits title (e.g. `feat: add road search endpoint`), one approval and passing `PR Results`, `Analysis Results` and `Validate Results` checks, and are squash-merged.

## License

[Apache 2.0](./LICENSE)
