# One-day implementation and demo plan

## Time-boxed execution
- Hour 1: Verify JDK 21/Maven; run locally; fix startup errors.
- Hour 2: Verify booking, full-capacity behavior, cancellation and lookup.
- Hour 3: Create GitHub repo; push main; create feature branch/PR; add issues and tags.
- Hour 4: Deploy app with PostgreSQL to a public host; verify public URL from another device.
- Hour 5: Configure Jenkins Maven pipeline, test results and archived JAR.
- Hour 6: Add Selenium journeys and publish reports; fix any failures.
- Hour 7: Build/run Docker image; demonstrate logs, stop/start/remove.
- Hour 8: Validate Ansible syntax and host configuration; collect evidence and rehearse demo.

## Critical path
1. `mvn clean verify`
2. Local browser test
3. GitHub push
4. Public deployment + PostgreSQL environment
5. Verify `/health`
6. Jenkins pipeline
7. Docker/Ansible evidence

## Demo script
1. Open public portal on phone and laptop.
2. Book a workshop and show remaining seats update.
3. Look up booking by email and cancel.
4. Open `/health`.
5. Show Git history, feature branch/PR and tag.
6. Run Jenkins and show Maven build/test report/JAR artifact.
7. Show Docker image and container logs.
8. Run Ansible syntax check or playbook and explain idempotency/recovery limits.

## Be honest in the report
Do not claim Selenium, registry publication, production deployment, Ansible provisioning, or rollback was successful unless you have run it and retained evidence. For a one-day submission, record any incomplete automation as a limitation and demonstrate the working parts.
