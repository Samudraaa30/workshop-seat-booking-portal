# Workshop Seat Booking Portal

A responsive Java 21 / Spring Boot site for workshop discovery, seat booking, booking lookup and cancellation. The supplied source archive is the project base; its original structure and working UI have been retained and extended.

## Features and design

- Workshop listings show start time, capacity, and live remaining-seat counts.
- Booking validates name and email, serializes capacity updates with a database row lock, and rejects full or missing workshops.
- Attendees can find and cancel bookings by email. Cancellation is idempotent and restores one seat.
- Thymeleaf templates and responsive CSS serve the web UI. `/health` and `/actuator/health` provide health endpoints.
- H2 is used locally; PostgreSQL is used in production. Liquibase owns the schema; Hibernate validates it rather than mutating production schemas at startup.
- Docker uses a Java 21 multi-stage build and a non-root runtime user. Jenkins runs Maven verification and optionally publishes/deploys. Ansible configures a Docker host and runs the image.

The booking lookup flow uses email as an identifier, not authentication. It is suitable for a coursework/demo MVP. Before storing real personal data, add authentication/authorization, CSRF protection, rate limiting, privacy controls, and a security review.

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
mvn spring-boot:run
```

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

Open `http://localhost:8080`. The local profile uses an H2 file under `data/`, which is ignored by Git. The seeded workshops are inserted on first startup.

## Build and tests

```bash
mvn clean verify
```

The suite includes a Spring context check, service unit tests, and MockMvc integration journeys for workshop listing, health, booking, lookup, cancellation, and full-capacity handling. Test reports are written to `target/surefire-reports/`.

## Configuration and production database

Set the following environment variables in the host or secret manager. Never commit `.env`, passwords, JDBC credentials, registry tokens, or SSH private keys.

| Variable | Example / purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db-host:5432/workshops?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL application user |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL application password |
| `PORT` | Optional; defaults to `8080` |

Create an empty PostgreSQL database and user, then start the app. Liquibase applies the versioned schema at `src/main/resources/db/changelog/db.changelog-master.xml`; Hibernate checks the resulting schema. Use a managed PostgreSQL service with encrypted connections and backups for public deployments.

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

## Public deployment and evidence

No GitHub remote, hosting account, deployment host, or database credentials were included in the supplied ZIP. Therefore no public URL or live deployment can be claimed yet. A suitable low-ops route is a container host such as Railway or Render paired with managed PostgreSQL; the Jenkins/Ansible path supports a Linux VM with Docker. Select/create the hosting account, provision PostgreSQL, store the variables above, and deploy the image before publishing a URL. Paid services and account access require the owner's choice and authorization.

Runtime verification is recorded below. Save hosting, Jenkins, registry, and Ansible screenshots under `docs/evidence/` after those external runs. Do not mark public deployment, registry push, Jenkins execution, or Ansible host execution complete until the corresponding run has been performed and recorded.

## Validation status for this source snapshot

- Verified with Java 21.0.12.1 and Maven 3.9.16: `mvn -B clean verify` passed with 7 tests and 0 failures/errors.
- Started the packaged JAR with the local profile; `/health` returned HTTP 200 and `UP`. The HTTP booking, lookup, and cancellation flow completed successfully.
- Docker and Jenkins were not installed in the authoring environment, so image build/run and pipeline execution remain unverified here.
- The Ansible playbook and Liquibase changelog parse as YAML/XML. Ansible was installed in a scratch virtual environment, but its CLI could not run on this native Windows control node; use Linux, macOS, or WSL for `ansible-playbook --syntax-check` and host execution.
- The provided ZIP did not include a `.git` directory, GitHub remote, or repository credentials. No GitHub push or public deployment was performed.
