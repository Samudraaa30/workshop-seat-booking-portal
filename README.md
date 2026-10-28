# Workshop Seat Booking Portal

A responsive Java 21 / Spring Boot site for workshop discovery, seat booking, booking lookup and cancellation. The supplied source archive is the project base; its original structure and working UI have been retained and extended.

## Features and design

- Workshop listings show start time, capacity, and live remaining-seat counts. Server-sent events push seat availability to every open workshop page after a booking or cancellation commits, without requiring a refresh.
- Booking validates name and email, serializes capacity updates with a database row lock, and rejects full or missing workshops.
- Attendees can find and cancel bookings by email. Cancellation is idempotent and restores one seat.
- The public attendee site and admin console are separate: attendees browse/book at `/`, while `/admin` requires an authenticated `ADMIN` role. Admins can review capacity, inspect the booking register, cancel a booking, and see activity alerts.
- A persistent in-app notification center records booking confirmations and cancellations for attendees, plus booking activity for admins. Notifications appear on My bookings and in the admin dashboard; this is not email/SMS delivery.
- Thymeleaf templates and responsive CSS serve the web UI. `/health` and `/actuator/health` provide health endpoints.
- `GET /api/workshops` returns the current public availability snapshot; `GET /api/workshops/events` streams initial and changed snapshots over Server-Sent Events. EventSource reconnects automatically and the server sends keep-alive comments during idle periods.
- In the `prod` profile, the app imports future listings from VIT's public [Events & Updates page](https://vit.edu.in/news-update/) through its WordPress REST page endpoint at startup and hourly while the service is running. It uses each announcement's title, date text, and official link; missing/invalid dates and past events are skipped. Local/test profiles keep the sample catalog and do not call the external feed. On Render's free instance, scheduled refreshes pause while the service sleeps and resume after it wakes.
- VIT does not publish seat capacity or booking counts through this feed. Imported events use a portal-managed capacity (`VIT_IMPORT_DEFAULT_CAPACITY`, default `30`), and their date is shown with “time TBA” when no time is published. Bookings/counts in this app are separate from VIT/AICTE registration; attendees may need to register with the official event organizer as well. The portal links to the announcement for that purpose.
- H2 is used locally; PostgreSQL is used in production. Liquibase owns the schema; Hibernate validates it rather than mutating production schemas at startup.
- Docker uses a Java 21 multi-stage build and a non-root runtime user. Jenkins runs Maven verification and optionally publishes/deploys. Ansible configures a Docker host and runs the image.

The attendee booking lookup and notification panel use email as an identifier, not verified authentication. Admin routes use Spring Security form login with a dedicated `ADMIN` role. If admin environment credentials are absent, admin sign-in fails closed while public booking remains available. Before storing real personal data, add attendee authentication, rate limiting, privacy controls, and a security review.

### Live availability flow

```text
Browser A ── EventSource (/api/workshops/events) ──┐
                                                   ├── Spring Boot broadcasts the latest seat snapshot
Browser B ── EventSource (/api/workshops/events) ──┘               ▲
       └── booking/cancellation POST → database transaction commits ┘
```

Each browser receives the current snapshot when it connects. A successful booking or cancellation publishes an application event; Spring delivers it only after the database transaction commits, and all connected browsers receive the new seat count. The page updates the progress bar and disables or re-enables the booking button immediately. If a browser disconnects, EventSource reconnects and receives a fresh snapshot. This is real-time availability across browsers connected to this app instance; the static workshop catalog itself has no admin/create-workshop workflow.

## Requirements

- JDK 21
- Maven 3.9+
- Git
- Docker Engine (container build and run)
- Jenkins with Pipeline, Git, JUnit, Credentials Binding, SSH Agent, Docker CLI, Ansible, JDK 21 and Maven configured (CI/CD)
- Ansible 2.14+ on a Linux, macOS, or WSL control node and the `community.docker` collection (deployment; native Windows is not supported as the Ansible control node)

## Run locally

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:PORTAL_ADMIN_USERNAME = 'choose-a-local-admin-name'
$env:PORTAL_ADMIN_PASSWORD = 'choose-a-long-unique-password'
mvn spring-boot:run
```

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=local
export PORTAL_ADMIN_USERNAME=choose-a-local-admin-name
export PORTAL_ADMIN_PASSWORD=choose-a-long-unique-password
mvn spring-boot:run
```

Open `http://localhost:8080`. The local profile uses an H2 file under `data/`, which is ignored by Git. The seeded workshops are inserted on first startup.

## Build and tests

```bash
mvn clean verify
```

The suite includes a Spring context check, service unit tests, and MockMvc integration journeys for workshop listing, health, booking, lookup, cancellation, full-capacity handling, admin authorization/cancellation, and notification delivery. Test reports are written to `target/surefire-reports/`.

## Configuration and production database

Set the following environment variables in the host or secret manager. Never commit `.env`, passwords, JDBC credentials, registry tokens, or SSH private keys.

| Variable | Example / purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db-host:5432/workshops?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL application user |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL application password |
| `PORT` | Optional; defaults to `8080` |
| `VIT_IMPORT_ENABLED` | Enable the VIT announcement importer in production (default `true`) |
| `VIT_IMPORT_DEFAULT_CAPACITY` | Portal-only capacity assigned to newly imported events (default `30`; choose an organizer-approved limit) |
| `VIT_IMPORT_INTERVAL_MS` | Time between successful/failed refresh attempts while running (default `3600000`, one hour) |
| `PORTAL_ADMIN_USERNAME` | Admin sign-in name; configure in the host environment, not source control. |
| `PORTAL_ADMIN_PASSWORD` | Long, unique admin password; configure in the host's secret settings. Spring Security keeps a BCrypt hash in memory. |

Create an empty PostgreSQL database and user, then start the app. Liquibase applies the versioned schema at `src/main/resources/db/changelog/db.changelog-master.xml`; Hibernate checks the resulting schema. Use a managed PostgreSQL service with encrypted connections and backups for public deployments.

The importer starts about 10 seconds after application startup. If VIT is temporarily unavailable, the existing workshop catalog stays available and the importer retries on its next scheduled run. No external attendee/seat data is inferred or imported.

## Docker

Build (the Docker build runs `mvn clean verify`):

```bash
docker build -t workshop-seat-booking:local .
```

Run with production environment variables supplied by your secret manager:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL \
  -e SPRING_DATASOURCE_USERNAME \
  -e SPRING_DATASOURCE_PASSWORD \
  -e PORTAL_ADMIN_USERNAME \
  -e PORTAL_ADMIN_PASSWORD \
  workshop-seat-booking:local
```

Verify `http://localhost:8080/health`. Do not put real values directly into shell history, Dockerfiles, or Compose files committed to Git.

## Jenkins declarative pipeline

Create a multibranch Pipeline job (or Pipeline from SCM) using `Jenkinsfile`. The agent must be Linux with Docker access, Ansible, and the `community.docker` Ansible collection. Configure Jenkins tools with IDs `jdk-21` and `maven-3.9`. Pipeline stages check out source, run `mvn -B clean verify`, archive the JAR, and build a Docker image. Image publishing and deployment are opt-in build parameters and restricted to the `main` branch.

For publishing/deployment, set `DOCKERHUB_NAMESPACE` in the Jenkinsfile and create these Jenkins credentials:

| Credential ID | Jenkins type | Contents |
| --- | --- | --- |
| `dockerhub-token` | Username with password | Docker Hub username and access token |
| `workshop-production-db` | Username with password | PostgreSQL application username and password |
| `workshop-production-jdbc-url` | Secret text | PostgreSQL JDBC URL |
| `workshop-deploy-ssh-key` | SSH Username with private key | Deploy key (the username field may be unused; set `DEPLOY_SSH_USER` build parameter) |

Install Jenkins plugins: Pipeline, Git, JUnit, Credentials Binding, and SSH Agent. Configure the remote SSH host key in the agent's `known_hosts`. Run publishing with `PUBLISH_IMAGE=true`; run deployment with both `PUBLISH_IMAGE=true` and `DEPLOY=true`, and supply `DEPLOY_HOST` and `DEPLOY_SSH_USER`. Secrets are exposed to Ansible through the build environment and suppressed in deployment task output. Review and restrict Jenkins build access accordingly.

## Ansible deployment

The included playbook installs Docker on Debian/Ubuntu, logs into Docker Hub, and runs the application as a restartable container. It expects Jenkins to provide the image and database/registry secrets. For direct use, export `PORTAL_IMAGE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `REGISTRY_USER`, and `REGISTRY_TOKEN` in the control-node environment (do not put them in inventory).

Install prerequisites on the Ansible control node:

```bash
python -m pip install ansible
ansible-galaxy collection install community.docker
```

Copy `ansible/inventory.ini.example` to `ansible/inventory.ini`, set the host and SSH user, then:

```bash
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml --syntax-check
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml --private-key ~/.ssh/workshop-deploy
```

The SSH account needs sudo access. Open TCP port 8080 for a direct demo, or place a TLS reverse proxy in front of the container for public use. The playbook does not create a PostgreSQL service, DNS name, TLS certificate, or cloud account.

## Render deployment

The source is published at [Samudraaa30/workshop-seat-booking-portal](https://github.com/Samudraaa30/workshop-seat-booking-portal). The live demo is [workshop-seat-booking-portal.onrender.com](https://workshop-seat-booking-portal.onrender.com/). Render built the checked-in Dockerfile, started Java 21/Spring Boot, and the public `/health` endpoint returned `{"application":"Workshop Seat Booking Portal","status":"UP"}`. Set `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the Render service environment; secrets stay in Render and are not stored in this repository.

**Free demo limits:** The current web service and PostgreSQL database use Render's free plans, so the web service can sleep when idle. Render states that free PostgreSQL databases expire after 30 days; this demo database is scheduled for deletion on **November 23, 2026** unless upgraded. Do not use it for important or long-lived data. The included `render.yaml` is an alternative Blueprint for a free Render web service connected to a separate Neon Free PostgreSQL database; Neon has its own storage and compute quotas. Follow the Blueprint comments and secret prompts when choosing that option. Review [Render free-tier limitations](https://render.com/docs/free), [Neon Free plan limits](https://github.com/neondatabase/website/blob/main/content/faqs/free-plan-limits-and-quotas.md), and [Neon's JDBC connection guidance](https://neon.com/docs/connect/connect-from-any-app). No paid plan is required to view this temporary demo.

To enable admin access on Render, add `PORTAL_ADMIN_USERNAME` and `PORTAL_ADMIN_PASSWORD` to the web service's Environment settings as secret values, then redeploy. Use a unique password. Until both values are present, `/admin/login` shows a setup notice and `/admin` remains protected. The attendee site remains public.

## Validation status

- `mvn clean verify` passed on Java 21.0.12.1 and Maven 3.10.0: **14 tests passed; 0 failures, errors, or skips**. The packaged JAR was produced.
- The SSE stream was exercised with a connected local HTTP client: a booking changed availability from 30 to 29, and cancellation restored it from 29 to 30; both updates arrived on the existing stream without reconnecting.
- Render's Docker build completed and the service was marked live. The public `/health` endpoint returned HTTP 200 with status `UP`.
- The booking, lookup, and cancellation flow is covered by `PortalFlowIntegrationTest` and was rechecked on the live UI after the redirect fix. Both synthetic test bookings were cancelled and the workshop seats restored.
- The Jenkins declarative pipeline is checked in, but a Jenkins server run and registry publishing were not performed in this environment.
- The Ansible playbook was reviewed and its YAML parsed. `ansible-playbook --syntax-check` and a remote host deployment were not run from this Windows control node; use Linux, macOS, or WSL for the documented Ansible commands.
- The first live smoke test exposed session-dependent feedback redirects. Fix and regression test are published on `main` and Render deployment `51071a1` is live.





