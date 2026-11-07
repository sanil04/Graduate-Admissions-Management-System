#!/usr/bin/env bash
# Creates Sprint 1 labels, milestone and issues for the Graduate Admissions project.
# Usage (from inside your cloned repo):  gh auth login   then   bash create_sprint1_issues.sh
set -euo pipefail

MILESTONE="Sprint 1"
DUE="2026-11-12T23:59:59Z"

echo "== Labels =="
gh label create setup    --color 5319e7 --description "Project setup / infrastructure" --force
gh label create feature  --color 0e8a16 --description "User-facing feature"           --force
gh label create backend  --color 1d76db --description "Backend work"                  --force
gh label create frontend --color fbca04 --description "Frontend / Thymeleaf work"      --force
gh label create test     --color d93f0b --description "Tests"                         --force
gh label create docs     --color 0075ca --description "Documentation / README"        --force
gh label create process  --color c5def5 --description "Agile process"                 --force

echo "== Milestone =="
gh api "repos/{owner}/{repo}/milestones" -f title="$MILESTONE" -f due_on="$DUE" >/dev/null 2>&1 \
  && echo "Created milestone '$MILESTONE'" \
  || echo "Milestone '$MILESTONE' already exists, reusing it"

# create_issue <title> <labels> <body>  -> prints the new issue number
create_issue() {
  local url
  url=$(gh issue create --title "$1" --label "$2" --milestone "$MILESTONE" --body "$3")
  echo "  created: $1 -> $url" >&2
  echo "${url##*/}"
}

echo "== Issues =="

I1=$(create_issue "Initialize Spring Boot project with Maven" "setup" "$(cat <<'EOF'
Generate the project with Spring Initializr (Web, Thymeleaf, Data JPA, Validation, H2, PostgreSQL driver) and commit `pom.xml` at the repo root.

- [ ] `mvn clean package` produces a runnable JAR
- [ ] `java -jar target/*.jar` starts the app locally
- [ ] `.gitignore` excludes `target/` and IDE files
EOF
)")

I2=$(create_issue "Set up CI with GitHub Actions" "setup" "Blocked by #$I1

- [ ] Workflow runs \`mvn verify\` on every PR and every push to \`main\`
- [ ] Failing tests block the PR from merging
- [ ] CI status badge added to README")

I3=$(create_issue "Deploy to Azure App Service with continuous deployment" "setup" "Blocked by #$I2

- [ ] App Service created with a Java runtime
- [ ] Workflow deploys the JAR automatically after each merge to \`main\`
- [ ] Publish profile stored in GitHub Secrets
- [ ] Live URL added to README")

I4=$(create_issue "Provision production database and configure Spring profiles" "setup,backend" "Blocked by #$I3

- [ ] Azure PostgreSQL (or Azure SQL) instance created
- [ ] H2 in \`application.properties\`, production DB in \`application-prod.properties\`
- [ ] DB credentials come from App Service environment variables, not the repo
- [ ] Deployed app connects to the production DB")

I5=$(create_issue "Configure repository workflow" "setup,process" "$(cat <<'EOF'
- [ ] Branch protection on `main`: PRs required, 2 approvals, CI must pass
- [ ] Projects board with Backlog / In Progress / Completed columns
- [ ] Labels and Sprint 1 milestone set up
- [ ] Branch naming convention agreed (e.g. `feature/12-applicant-form`)
EOF
)")

I6=$(create_issue "Create README skeleton" "docs" "$(cat <<'EOF'
- [ ] Sections: description, live URL, build/run instructions, current status, next-sprint plan, UML model diagram, DB schema
- [ ] Short summary of the team's PR/review workflow
EOF
)")

I7=$(create_issue "Implement core domain entities" "backend" "Blocked by #$I1

- [ ] Abstract \`User\` with \`Applicant\`, \`Professor\`, \`Admin\` subclasses (JOINED inheritance, documented)
- [ ] \`ResearchField\` and \`AdmissionCycle\` entities
- [ ] \`Application\` entity: personal info, desired field, preferred professors (many-to-many), status enum, submission date
- [ ] Spring Data repositories for each entity
- [ ] \`@DataJpaTest\` tests for the repositories")

I8=$(create_issue "Add UML class diagram and DB schema to README" "docs" "Blocked by #$I7

- [ ] UML of the models only (Mermaid or PlantUML), committed to the repo
- [ ] DB schema generated from the ORM (e.g. IntelliJ database diagram)
- [ ] Short note on ORM patterns used (inheritance mapping, join tables)")

I9=$(create_issue "Seed initial data" "backend" "Blocked by #$I7

- [ ] Research fields seeded (e.g. AI, Software Engineering, Networks, Security)
- [ ] One admin and a few sample professors seeded
- [ ] Seeding is idempotent (restarts don't duplicate data)")

I10=$(create_issue "Professor profile: create and edit research fields" "feature,backend,frontend" "Blocked by #$I7, #$I9

- [ ] Form to set name, email and research fields (multi-select)
- [ ] Profile view page
- [ ] Validation errors shown on the form
- [ ] \`@WebMvcTest\` controller test and service unit test")

I11=$(create_issue "Applicant: submit an application" "feature,backend,frontend" "Blocked by #$I7, #$I9

- [ ] Form with personal info, desired research field and 1-3 preferred professors
- [ ] Professor dropdown populated from the DB, filtered by the chosen field
- [ ] On submit, status = SUBMITTED and timestamp recorded
- [ ] Confirmation page after submit
- [ ] Controller and service tests")

I12=$(create_issue "Admin: view list of submitted applications" "feature,backend,frontend" "Blocked by #$I11

- [ ] Table: applicant name, desired field, preferred professors, submission date, status
- [ ] Detail page for a single application
- [ ] Controller and service tests")

I13=$(create_issue "Home page and navigation" "frontend" "$(cat <<'EOF'
- [ ] Landing page with links to Applicant, Professor and Admin sections
- [ ] Shared Thymeleaf layout (header + nav)
- [ ] No links to features that aren't built yet
EOF
)")

I14=$(create_issue "End-to-end integration test: submit application -> visible to admin" "test" "Blocked by #$I11, #$I12

- [ ] \`@SpringBootTest\` + MockMvc: submit an application, then check it appears in the admin list
- [ ] Runs in CI")

I15=$(create_issue "Weekly scrum – Nov 9" "process" "$(cat <<'EOF'
Each team member comments with:
- What I did this week (link issues)
- What I'll do next week (link issues)
- What's holding me back (if anything)

1-2 sentences per item. Open a new issue like this every week.
EOF
)")

I16=$(create_issue "Prepare Milestone 1 demo (Nov 12)" "process" "Blocked by #$I1 – #$I14

- [ ] README status and Sprint 2 plan match the Kanban board
- [ ] Production app verified working on Azure
- [ ] Fresh clone + \`mvn package\` verified
- [ ] Demo script written and rehearsed (under 10 minutes)")

echo
echo "Done: created issues #$I1 to #$I16 in milestone '$MILESTONE'."
echo "Tip: add them to your Kanban board with:  gh project item-add <project-number> --owner <owner> --url <issue-url>"
echo "     or in the Project settings, turn on the 'Auto-add to project' workflow for this repo."
