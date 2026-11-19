[![Apache 2.0 License](https://img.shields.io/github/license/bcgov/nr-lrmops-api.svg)](/LICENSE)
[![Merge](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml)
[![Analysis](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml)
[![Scheduled](https://github.com/bcgov/nr-lrmops-api/actions/workflows/scheduled.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/scheduled.yml)

# LRM Ops API

A read-only API that gives the CBR team access to LRM data held in the TFM Oracle database: road search and lookup, LRMOPS details for a CBR site, and road details at a location.

It is a NestJS service deployed to OpenShift through GitHub Actions. It was created from [bcgov/quickstart-openshift](https://github.com/bcgov/quickstart-openshift); the frontend, Postgres database and Flyway migrations from that template have been removed.

## Repository Layout

| Path | Contents |
|------|----------|
| [`backend/`](./backend/) | NestJS API, its `Dockerfile` and OpenShift template (`openshift.deploy.yml`, including the public Route) |
| [`common/openshift.init.yml`](./common/openshift.init.yml) | NetworkPolicies applied before each deploy |
| [`common/tests/`](./common/tests/) | Integration tests (run against deployed environments) and k6 load tests |
| [`scripts/`](./scripts/) | One-time GitHub and OpenShift setup scripts |
| [`scripts/db/`](./scripts/db/) | Oracle (TFM) role, proxy user and grant scripts, deployed by a DBA; see [`scripts/db/readme.txt`](./scripts/db/readme.txt) |
| [`monitoring/alerts/`](./monitoring/alerts/) | Sysdig alert templates synced to PROD |
| [`docs/adr/`](./docs/adr/) | Architecture decision records |
| [`.github/workflows/`](./.github/workflows/) | CI/CD pipelines |

## Local Development

Requires Node.js 24 (Node 22.12+ also works).

```bash
cd backend
npm ci
npm run start:dev     # http://localhost:3000/api
```

Or with Docker: `docker compose up` (serves the API on http://localhost:3001/api).

| Endpoint | Purpose |
|----------|---------|
| `/api` | Liveness message |
| `/api/health` | Health check used by OpenShift probes |
| `/api/metrics` | Prometheus metrics |
| `/api/docs` | Swagger UI |

### Build, Lint and Test

```bash
cd backend
npm run build
npm run lint
npm run format:check
npm test              # unit tests (Vitest)
npm run test:cov      # with coverage
```

## Environments

| Environment | Trigger | URL |
|-------------|---------|-----|
| PR | Pull request opened or updated | `https://nr-lrmops-api-<pr#>.apps.silver.devops.gov.bc.ca/api` |
| TEST | Merge to `main` | `https://nr-lrmops-api-test.apps.silver.devops.gov.bc.ca/api` |
| PROD | Merge to `main` after TEST passes, or a published release | `https://nr-lrmops-api-prod.apps.silver.devops.gov.bc.ca/api` |

Each environment lives in its own OpenShift namespace: `<plate>-dev` (PRs), `<plate>-test` and `<plate>-prod`.

## CI/CD

| Workflow | Runs on | What it does |
|----------|---------|--------------|
| [`pr-open.yml`](./.github/workflows/pr-open.yml) | Pull request | Builds the backend image to ghcr.io, deploys a PR environment, runs integration tests, comments the deployment link |
| [`pr-validate.yml`](./.github/workflows/pr-validate.yml) | Pull request | Enforces a Conventional Commits PR title |
| [`analysis.yml`](./.github/workflows/analysis.yml) | Pull request, merge | Lint, format check, unit tests with coverage, SonarCloud (optional), Trivy scan |
| [`pr-close.yml`](./.github/workflows/pr-close.yml) | Pull request closed | Removes the PR environment and retags images |
| [`merge.yml`](./.github/workflows/merge.yml) | Merge to `main` | Deploys TEST, runs tests, deploys PROD, syncs Sysdig alerts, tags images as `prod` |
| [`release.yml`](./.github/workflows/release.yml) | Published release | Deploys an already-tested PR image to PROD (no rebuild) |
| [`scheduled.yml`](./.github/workflows/scheduled.yml) | Saturdays, or by hand | Purges stale PR environments, ZAP scan and k6 load test against TEST |
| [`cert-keeper.yml`](./.github/workflows/cert-keeper.yml) | Daily, or by hand | Keeps the PROD vanity URL certificate valid; does nothing until `ROUTE_HOST` is set |

To make PROD release-only, comment out the `deploy-prod`, `monitor-prod` and `promote` jobs in `merge.yml`. Merges then stop at TEST, and PROD changes only when a release is published from a commit on `main` that came from a PR.

## First-Time Setup

1. **OpenShift namespaces**: request a project set (DEV, TEST, PROD) from Platform Services at the [registry](https://registry.developer.gov.bc.ca). This gives a licence plate, e.g. `abc123`.
2. **Log in** with your own accounts: `gh auth login`, and `oc login --token=... --server=...` (copy the command from the OpenShift console). You need admin on the repository and edit rights in all three namespaces.
3. **Run the setup scripts.** They preview by default and change things only with `--apply`. They are safe to re-run.
    ```bash
    scripts/setup.sh --plate abc123            # preview
    scripts/setup.sh --plate abc123 --apply    # apply
    ```
    This runs, in order:
    * `scripts/setup-github.sh`: the `main` ruleset, pull request settings and private vulnerability reporting.
    * `scripts/setup-openshift.sh`: a `github-actions` service account, `edit` role binding and token Secret in each namespace.
    * `scripts/setup-environments.sh`: the `test` and `prod` GitHub environments, the `OC_NAMESPACE` and `OC_SERVER` variables and the `OC_TOKEN` secrets.
4. **Open a first pull request** to run the pipeline, then make the `backend` package public under the repository's Packages so OpenShift can pull it.

### Secrets and Variables

| Environment | Name | Description |
|-------------|------|-------------|
| repository | `vars.OC_SERVER` | OpenShift API server URL, e.g. `https://api.silver.devops.gov.bc.ca:6443` |
| repository | `vars.OC_NAMESPACE` | PR namespace, `<plate>-dev` |
| repository | `secrets.OC_TOKEN` | Deploy token for `<plate>-dev` |
| test | `vars.OC_NAMESPACE` | `<plate>-test` |
| test | `secrets.OC_TOKEN` | Deploy token for `<plate>-test` |
| prod | `vars.OC_NAMESPACE` | `<plate>-prod` |
| prod | `secrets.OC_TOKEN` | Deploy token for `<plate>-prod` |
| repository (optional) | `secrets.SONAR_TOKEN_BACKEND` | SonarCloud analysis |
| prod (optional) | `secrets.SYSDIG_API_TOKEN` | Sysdig alert sync; skipped with a warning when unset |
| repository (optional) | `vars.ROUTE_HOST` | Vanity hostname, e.g. `lrmops-api.gov.bc.ca` |

To create an `OC_TOKEN` by hand instead of with the scripts, in each namespace:

```bash
oc create sa github-actions
oc create rolebinding github-actions-edit --clusterrole=edit --serviceaccount=$(oc project -q):github-actions
oc create token github-actions --duration=87600h | gh secret set OC_TOKEN   # add --env test or --env prod
```

### Recommended: Restrict PROD Deployments

Under `Settings > Environments > prod > Deployment branches and tags`, choose **Selected branches and tags** and allow `main` (and a tag rule such as `v*` if you deploy from releases). This stops pull-request jobs from reading PROD secrets.

## Optional Integrations

**SonarCloud.** Request a project through [devops-requests](https://github.com/bcgov/devops-requests/issues/new/choose) ("Request for import GitHub repository on SonarCloud"). Then save the token as `SONAR_TOKEN_BACKEND` and change `-Dsonar.projectKey` in [`analysis.yml`](./.github/workflows/analysis.yml) from `quickstart-openshift_backend` to this project's key. Analysis runs without it.

**Renovate.** [`renovate.json`](./renovate.json) extends [bcgov/renovate-config](https://github.com/bcgov/renovate-config). BC Government teams can request the Renovate and Renovate Approve GitHub Apps through the [CITZ Service Desk](https://citz-do.atlassian.net/servicedesk/customer/portal/2/group/9/create/10).

**Sysdig.** Set `SYSDIG_API_TOKEN` in the `prod` environment (from the Secret created by your `SysdigTeam` in `<plate>-tools`). The templates in [`monitoring/alerts/`](./monitoring/alerts/) are synced on every PROD deploy; see [bcgov/action-sysdig-monitor](https://github.com/bcgov/action-sysdig-monitor) for the format.

**Vanity URL.** Point the hostname's DNS at the cluster router, then `gh variable set ROUTE_HOST --body '<hostname>'`. [`cert-keeper.yml`](./.github/workflows/cert-keeper.yml) then issues and renews a Let's Encrypt certificate on the `nr-lrmops-api-vanity-url` Route. See the [cert-keeper README](https://github.com/bcgov/actions-openshift/blob/main/cert-keeper/README.md) for limits and troubleshooting.

**Knip.** Unused-dependency scanning runs inside the Analysis workflow in warning mode. Report false positives upstream in [bcgov/action-test-and-analyse](https://github.com/bcgov/action-test-and-analyse) (`.knip.json`) rather than adding a local config.

## Security

Deployments run with read-only root filesystems, as non-root, with privilege escalation blocked, all capabilities dropped and the default seccomp profile. If the app needs to write at runtime, mount a memory-backed `emptyDir` with a `sizeLimit` (as `/tmp` already is in [`backend/openshift.deploy.yml`](./backend/openshift.deploy.yml)) rather than relaxing the security context.

NetworkPolicies allow traffic to the backend only from the OpenShift router. Trivy, CodeQL and a weekly OWASP ZAP scan report to the repository's Security tab.

See [SECURITY.md](./SECURITY.md) for vulnerability reporting and the OWASP ASVS mapping.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md). Pull requests need a Conventional Commits title (e.g. `feat: add road search endpoint`), one approval and passing `PR Results`, `Analysis Results` and `Validate Results` checks, and are squash-merged.

## License

[Apache 2.0](./LICENSE)
