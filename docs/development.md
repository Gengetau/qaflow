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
