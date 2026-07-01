# Development

## Java 21 On Windows

QAFlow targets Java 21. Do not change the Maven project target to Java 24.

Preferred local JDK:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --exact
```

Set `JAVA_HOME` before running backend Maven commands:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
cd apps/api
.\mvnw.cmd test
.\mvnw.cmd verify
```

Expected Java output should report Temurin/OpenJDK 21.x for both `java` and `javac`.

## Docker And Testcontainers

Docker Desktop may be reachable from the Docker CLI while Testcontainers cannot use the Windows named-pipe endpoint from Java. Docker-dependent tests should use `@Testcontainers(disabledWithoutDocker = true)` so they run in compatible Docker environments and skip locally when Java cannot access Docker.

## Demo Seed Profile

Run the backend with the `demo` profile to seed local demo data:

```powershell
cd apps/api
$env:SPRING_PROFILES_ACTIVE='dev,demo'
.\mvnw.cmd spring-boot:run
```

The demo seed is idempotent and creates:

- `owner@example.com` / `password123`
- `tester@example.com` / `password123`
- `viewer@example.com` / `password123`

It also creates the `QAFlow Demo Workspace`, a `SHOP` storefront QA project, realistic suites, test cases, active and completed test runs, and open defects so dashboard/report views have useful data.

## Frontend Auth Development

The Vue dev server proxies `/api` requests to `http://localhost:8080`, so the frontend auth pages can call the Spring Boot API without hard-coded browser URLs.

Run the backend first, then the frontend:

```powershell
cd apps/api
.\mvnw.cmd spring-boot:run

cd apps/web
pnpm dev
```

Login and registration store the access token, refresh token, current user, and workspace membership in browser local storage under `qaflow.auth`.

## OpenAPI TypeScript Client

The backend exposes OpenAPI JSON at:

```text
http://localhost:8080/v3/api-docs
```

After backend API contract changes, run the backend and refresh the committed snapshot:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs -OutFile openapi/qaflow.openapi.json
```

Then regenerate the frontend client:

```powershell
cd apps/web
pnpm api:generate
pnpm typecheck
```

Generated files live under `apps/web/src/app/api/generated/`. Keep hand-written API modules thin by re-exporting or wrapping generated operations where practical.
