[![Apache 2.0 License](https://img.shields.io/github/license/bcgov/nr-lrmops-api.svg)](/LICENSE)
[![Merge](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/merge.yml)
[![Analysis](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml/badge.svg)](https://github.com/bcgov/nr-lrmops-api/actions/workflows/analysis.yml)

# LRM Ops API

A read-only **Spring Boot** service that exposes LRMOPS road data from the TFM Oracle database
(DBQ05 for TEST, DBP05 for PROD) to other applications.

- **No frontend, no database of its own.** It reads TFM as `PROXY_FSA_LRM_READ_WRITE_USER`,
  which has SELECT grants only (scripts in [`scripts/db/`](./scripts/db/)).
- **Incoming auth:** Keycloak-issued **JWT bearer tokens**, authorized per endpoint with
  **OAuth2 scopes**.
- **CORS:** browser apps calling directly need their origin in `ALLOWED_ORIGINS`.

## Endpoints

All endpoints are under `/api/v1` and need a bearer token carrying the listed scope.
Interactive docs are at `/swagger-ui/index.html`.

| Method | Path | Required scope | Description |
|---|---|---|---|
| `GET` | `/api/v1/roads` | `lrmops:roads:read` | Search road sections (partial match), one page at a time with the total. |
| `GET` | `/api/v1/roads/{roadId}/sections/{sectionId}` | `lrmops:roads:read` | One road section (exact match, case-insensitive), or 404. |
| `GET` | `/api/v1/road-tenure-types` | `lrmops:roads:read` | The road tenure types, with descriptions — the values `tenureType` matches exactly. |

### `GET /api/v1/roads`

Road sections from `TFM.TFM_ROAD`, one row per road and section.

| Parameter | Matches |
|---|---|
| `roadName` | `ROAD_NAME`; runs of spaces count as one |
| `roadId` | `ROAD_ID`, e.g. `R08317` |
| `sectionId` | `SECTION_ID` |
| `tenureType` | `ROADTENURETYPE`, **exactly** (case-insensitive) — a code from `/api/v1/road-tenure-types`: `RP` active road permits only, `RP-RETIRE` retired ones |
| `clientName` | `CLIENT_NAME` |
| `pageNumber`, `pageSize` | Zero-based page (default `0`); rows per page (default `15`, at most `200`) |
| `sortBy`, `sortDirection` | `ROAD_NAME`, `ROAD_ID`, `SECTION_ID`, `TENURE_TYPE`, `CLIENT_NAME` with `ASC`/`DESC`; repeat both to sort by several |

Every criterion is optional and case-insensitive. `NEWGEOMETRY` rows (an edit in progress) are
left out. Retired roads are included, with `retired: true`.

```json
{
  "content": [
    { "roadId": "R08317", "sectionId": "I", "roadName": "AM 4", "tenureType": "RP", "retired": true, "clientName": "..." }
  ],
  "totalElements": 1, "totalPages": 1, "pageNumber": 0, "pageSize": 15
}
```

### `GET /api/v1/roads/{roadId}/sections/{sectionId}`

Whether LRMOPS has this road section, and its details; **404** when it doesn't. Use it to check a
road a user has entered. `districts` usually has one entry, and two when the section crosses a
district boundary.

```json
{
  "roadId": "R07571", "sectionId": "AA", "roadName": "BIG CREEK FSR",
  "tenureType": "FSR", "retired": false,
  "districts": [ { "orgUnitNo": 18, "code": "DPG" }, { "orgUnitNo": 19, "code": "DMH" } ],
  "bctsOffice": null
}
```

### `GET /api/v1/road-tenure-types`

Every road tenure type, by code, with its description, from `TFM_VT_ROAD_TENURE_TYPE` — for a
tenure type filter. Retired variants (`RP-RETIRE`) are included, to filter on retired roads; `!`
("Geometry only") is left out. In road results a retired road still comes back with its base type
and `"retired": true`.

```json
[ { "code": "FSR", "description": "Forest Service Road" }, { "code": "FSR-RETIRE", "description": "FSR (Retired)" } ]
```

## Consuming the API — provision your own Keycloak client

This service does **not** hand out credentials. Each consuming team registers its own
confidential client in the same Keycloak realm as this API and calls with a
`client_credentials` token. This repo creates the client scopes in each realm at deploy time
(see [Keycloak scope provisioning](#keycloak-scope-provisioning)); assigning them to your
client is yours to do.

1. **Create a confidential service-account client** in the realm (`serviceAccountsEnabled: true`,
   no browser flows).
2. **Assign the scopes you need** as **default** client scopes:
   - `lrmops:roads:read` — road search, road section lookup and road tenure types
   - `lrmops:road-locations:read` — what LRMOPS would assign at a coordinate (not built yet)
   - `lrmops:cbr-sites:read` — LRMOPS's details for a CBR crossing site (not built yet)
3. **Mint a token** and send it as a bearer token:

   ```bash
   TOKEN=$(curl -sS -X POST "$KEYCLOAK_ISSUER_URI/protocol/openid-connect/token" \
     -d grant_type=client_credentials \
     -d client_id="$YOUR_CLIENT_ID" \
     --data-urlencode "client_secret=$YOUR_CLIENT_SECRET" | jq -r .access_token)

   curl -sS -H "Authorization: Bearer $TOKEN" "$LRMOPS_API_BASE_URL/api/v1/roads?roadId=R08317"
   ```

   Cache the token and refresh it shortly before expiry. Missing or expired token → **401**;
   valid token without the endpoint's scope → **403**.

## Configuration

All configuration is via environment variables (see `backend/src/main/resources/application.yml`
and [`backend/.env.example`](./backend/.env.example)):

| Variable | Description | Default |
|---|---|---|
| `KEYCLOAK_ISSUER_URI` | Realm issuer URI for token validation | — |
| `ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API (CORS) | empty (none) |
| `DATABASE_HOST` | TFM Oracle host | — |
| `DATABASE_PORT` | TCPS port | `1543` |
| `DATABASE_SERVICE_NAME` | Oracle service name | — |
| `DATABASE_USER` | `PROXY_FSA_LRM_READ_WRITE_USER` | — |
| `DATABASE_PASSWORD` | Its password | — |
| `TRUSTSTORE_PATH` | TCPS truststore | `/cert/jssecacerts` |
| `KEYSTORE_SECRET` | Truststore password | — |
| `SERVER_PORT` | HTTP port | `8080` |
| `APP_LOG_LEVEL` | Log level for `ca.bc.gov.nrs.lrmops` | `INFO` |

## Local development

Requires JDK 21 (not newer: Java 24+ fails the Oracle TCPS handshake), Maven 3.9 and the
BC Gov VPN. Put the values above in `backend/src/main/resources/application-local.yml`
(gitignored).

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Then:
- Health: `curl http://localhost:8080/actuator/health`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

Run the tests (H2 stands in for Oracle):

```bash
cd backend
mvn -B -DskipITs clean verify
```

## Deployment

Containerized via `backend/Dockerfile` and deployed to OpenShift (Gold) via
`backend/openshift.deploy.yml` and the GitHub Actions workflows in `.github/workflows`.

### Environments

| Zone | Host | Deployed by |
| --- | --- | --- |
| PR | `nr-lrmops-api-<pr>.apps.gold.devops.gov.bc.ca` | `pr-open.yml`, on each PR push; removed by `pr-close.yml` |
| TEST | `nr-lrmops-api-test.apps.gold.devops.gov.bc.ca` | `merge.yml`, on every merge to `main` |
| PROD | `nr-lrmops-api-prod.apps.gold.devops.gov.bc.ca` | Disabled until go-live (see the comment in `merge.yml`) |

### Secrets and variables

PR deploys read repository-level values; TEST reads the `test` environment's, which override
the repository's.

- **variables:** `OC_SERVER`, `KEYCLOAK_SA_ISSUER_URI`, `ALLOWED_ORIGINS` (optional)
- **secrets:** `OC_NAMESPACE`, `OC_TOKEN`, `DATABASE_HOST`, `DATABASE_SERVICE_NAME`,
  `DATABASE_USER`, `DATABASE_PASSWORD`, `KEYSTORE_SECRET`, `KEYCLOAK_SA_CLIENT_ID` and
  `KEYCLOAK_SA_CLIENT_SECRET` (optional)

### Keycloak scope provisioning

During deploy, `.github/scripts/ensure-keycloak-scopes.sh` creates this API's client scopes in
the realm behind `KEYCLOAK_SA_ISSUER_URI`; existing scopes are left untouched. It runs only
when `KEYCLOAK_SA_CLIENT_ID` is set. That service-account client must hold the
realm-management **`manage-clients`** role.

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Security (OAuth2 resource server) · Spring Data JPA (Hibernate) ·
Oracle (ojdbc11, TCPS) · springdoc OpenAPI · Actuator + Micrometer/Prometheus · Undertow · Maven.
