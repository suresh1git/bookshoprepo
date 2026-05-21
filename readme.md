# bookshop — CAP Java sample (OData v4 + SAP HANA Cloud)

A minimal, deployable SAP Cloud Application Programming Model (CAP) **Java**
service exposing an OData v4 API, backed by **SAP HANA Cloud**, packaged as an
**MTA** for SAP BTP Cloud Foundry. No UI module (service only).

## What it contains

| Path | Purpose |
|------|---------|
| `db/schema.cds` | Domain model: `Books`, `Authors`, `Genres` |
| `db/data/*.csv` | Sample data loaded into HANA on deploy |
| `srv/cat-service.cds` | `CatalogService` (`/odata/v4/browse`, public read) and `AdminService` (`/odata/v4/admin`, authenticated) |
| `srv/src/main/java/.../Application.java` | Spring Boot entry point |
| `srv/src/main/java/.../CatalogServiceHandler.java` | Custom `submitOrder` action logic |
| `srv/pom.xml` | Service module build (cds-maven-plugin + Spring Boot) |
| `pom.xml` | Parent POM, pins CAP Java `4.4.2` |
| `mta.yaml` | MTA descriptor: `bookshop-srv` (java) + `bookshop-db-deployer` (hdb) + HANA + XSUAA |
| `xs-security.json` | XSUAA configuration |

## Prerequisites (important)

1. **A running SAP HANA Cloud instance** in your target CF org/space.
   On a trial, HANA Cloud auto-stops after a few hours — **restart it from the
   BTP cockpit before deploying**, or the `bookshop-db-deployer` will fail at
   staging (this is the usual cause of `BuildpackCompileFailed`).
2. Entitlements for `hana` (hdi-shared) and `xsuaa` (application) plans.
3. Build environment with: **JDK 17 or 21**, **Maven 3.9.x**, **Node.js 20+**,
   and the **MTA Build Tool** (`npm i -g mbt`).

## Build locally (recommended before pushing to ReleaseOwl)

Verify the project compiles end-to-end. The first Maven build downloads CAP
dependencies from Maven Central (`com.sap.cds`):

```bash
# 1. compile + generate CDS/Java artifacts + Spring Boot jar
mvn -B clean package        # produces srv/target/bookshop-srv.jar

# 2. build the MTA archive (what gets deployed)
mbt build                   # produces mta_archives/bookshop_1.0.0.mtar
```

If `mvn clean package` succeeds, the deploy will too (assuming HANA is up).
This is the one step worth running locally, because it confirms the CAP Java
dependency versions and the generated handler code compile against your
environment.

## Deploy

### Via ReleaseOwl / Piper (your existing pipeline)

This slots into the same `cloudFoundryDeploy` step you already use:

- Push these sources to your build repo branch.
- Build job runs `mbt build` → `bookshop_1.0.0.mtar`.
- Deploy job runs `cloudFoundryDeploy` with `deployTool: mtaDeployPlugin` on
  that `.mtar` — identical flow to your previous MTA deployment.

### Manually (to test outside the pipeline)

```bash
cf login -a <your-api-endpoint> -o <org> -s <space>
mbt build
cf deploy mta_archives/bookshop_1.0.0.mtar
```

## Try the service after deploy

```
GET  https://<bookshop-srv-url>/odata/v4/browse/Books
GET  https://<bookshop-srv-url>/odata/v4/browse/Authors
POST https://<bookshop-srv-url>/odata/v4/browse/submitOrder
     { "book": 201, "quantity": 2 }
```

`/odata/v4/admin/*` requires an authenticated user (XSUAA token).

## Notes

- CAP Java version is pinned to `4.4.2` in the parent `pom.xml`
  (`cds.services.version`). CAP Java 5.x requires Maven ≥ 3.9.10 and has
  `cds-maven-plugin` changes; stay on 4.x unless you intend to migrate.
- `SPRING_PROFILES_ACTIVE: cloud` is set on the deployed app so it binds to
  HANA via the `cds-starter-cloudfoundry` auto-configuration.
- The `submitOrder` handler uses the **generic CQN API** (no generated typed
  accessors), so it compiles independently of the cds-maven-plugin `generate`
  step. If you prefer typed access, enable the `generate` goal and switch to
  the generated `Books_` / `SubmitOrderContext` classes.
- Java buildpack is pinned to `sap_java_buildpack_jakarta` with SAPMachine JRE
  21, matching SAP's April 2026 default.
