# Graduate Admissions Management System

![CI](https://github.com/sanil04/SYSC-4806-Group-Project/actions/workflows/ci.yml/badge.svg)

Contributors include:

1) Qurb E Muhammad Syed
2) Alexander Gardiner
3) Sanil Srivastava
4) Ahmed Al-Khazaali


SYSC 4806 term project (topic 7). A Spring MVC web app for graduate admissions:

- **Applicants** submit a file with personal info, a desired research field, 1-3 preferred professors and documents (CV, diploma, grade audit) before the submission deadline.
- **Professors** keep a profile listing their research fields. They're emailed when applicants are routed to them, then enter an assessment before the evaluation deadline.
- **Admins** screen applications, route the good ones to professors, read the recommendations and make the final decision.

| | |
|---|---|
| **Production** | TBA |
| **Kanban** | [Projects tab](https://github.com/sanil04/SYSC-4806-Group-Project/projects) |
| **CI / deploys** | [Actions tab](https://github.com/sanil04/SYSC-4806-Group-Project/actions) |

**Stack:** Java 21, Spring Boot 4.1 (Spring MVC, Thymeleaf + Bootstrap, Spring Data JPA, Bean Validation). H2 locally and in tests, managed PostgreSQL on Azure in production. Spring Security and Spring Mail come in Sprint 2. Deployed to Azure App Service by GitHub Actions.

---

## Current status

_Mirrors the Kanban. Updated at the end of each sprint._

| Issue | Item | Status |
|---|---|---|
| #1 | Spring Boot project with Maven | Done |
| #2 | CI with GitHub Actions | In progress |
| #3 | Deploy to Azure App Service | In progress |
| #4 | Production database | Backlog |
| #5 | Branch protection + Kanban | In progress |
| #6 | README skeleton | In review |
| #7 | Domain entities | Backlog |
| #10 | Professor profile | Backlog |
| #11 | Application form | Backlog |
| #12 | Admin application list | Backlog |
| #13 | Home page and navigation | Backlog |

## Sprint plan

### Sprint 1: Milestone 1, early prototype (demo Nov 12)

Goal: one use case working end to end, deployed on Azure.

> A professor sets up their profile with research fields. An applicant fills in the application, picks that professor, and submits. The admin opens the applications list and sees it.

There's no login in Sprint 1. Users are seeded, and the home page has a role picker (Applicant / Professor / Admin).

Supporting work: the entities and seed data, CI on every PR, a deploy on every merge to `main`, unit tests per controller, and one end-to-end test (submit an application, then see it in the admin list).

### Sprint 2: Milestone 2, alpha (demo Nov 26)

_Finalized at the end of Sprint 1._ Candidates:
- Login and role-based access with Spring Security
- Document upload and download (CV, diploma, grade audit)
- Admin screening (screen out or move to review) and routing to professors
- Email notification to professors when applicants are routed to them
- Professor assessment form
- Submission and evaluation deadlines enforced

---

## Domain model (UML)

Models only. Whoever changes an entity updates this diagram **in the same PR**.

> **Status:** matches the entities built in #7 (`src/main/java/com/sysc4806/gradadmissions/model`). `Document` and `Evaluation` are still planned and are added by the issues that need them.

```mermaid
classDiagram
    direction LR

    class User {
        <<abstract>>
        Long id
        String name
        String email
    }
    class Applicant
    class Professor
    class Admin

    User <|-- Applicant
    User <|-- Professor
    User <|-- Admin

    class ResearchField {
        Long id
        String name
    }

    class AdmissionCycle {
        Long id
        String name
        LocalDate applicationDeadline
        LocalDate evaluationDeadline
        boolean active
    }

    class Application {
        Long id
        ApplicationStatus status
        LocalDateTime submittedAt
        addPreferredProfessor(Professor)
        submit(LocalDateTime)
    }

    class PersonalInfo {
        <<embeddable>>
        String firstName
        String lastName
        String email
        String phoneNumber
        String citizenship
    }

    class ApplicationStatus {
        <<enumeration>>
        DRAFT
        SUBMITTED
        SCREENED_OUT
        UNDER_REVIEW
        ADMITTED
        REJECTED
    }

    Application "*" --> "1" Applicant : applicant
    Application "*" --> "1" AdmissionCycle : cycle
    Application *-- "1" PersonalInfo : personalInfo
    Application "*" --> "0..1" ResearchField : desiredField
    Application "*" --> "0..3" Professor : preferredProfessors
    Professor "*" --> "*" ResearchField : researchFields
    Application --> ApplicationStatus
```

**Rules enforced in `Application`:**
- At most 3 preferred professors (`MAX_PREFERRED_PROFESSORS`). This is our team's assumption: the project description only says "a list of profs", so the limit is one constant if we change it.
- A draft can be missing its desired field and professors. `submit()` requires a desired field and at least one professor, and only a `DRAFT` can be submitted.
- `PersonalInfo` is embedded, so its fields are columns in the `application` table rather than a separate table.

**Status flow.** Only the admin changes an application's status:

```mermaid
stateDiagram-v2
    direction LR
    [*] --> DRAFT
    DRAFT --> SUBMITTED : applicant submits
    SUBMITTED --> SCREENED_OUT : admin screens out
    SUBMITTED --> UNDER_REVIEW : admin routes to profs
    UNDER_REVIEW --> ADMITTED : admin decides
    UNDER_REVIEW --> REJECTED : admin decides
```

`SCREENED_OUT` through `REJECTED` are in the enum now, but the admin actions that set them come in later issues. The planned design: an `Evaluation` row is created when the admin routes an application to a professor, its `recommendation` stays empty until the professor submits an assessment, and a professor's assessment never changes the application's status.

### ORM patterns

| Pattern | Where | Why |
|---|---|---|
| Class Table Inheritance (`@Inheritance(JOINED)`) | `users` + `applicant`, `professor`, `admin` tables. Each subclass PK is also an FK to `users.id` | Subclasses have their own relationships. SINGLE_TABLE can't put NOT NULL on subclass-only columns. TABLE_PER_CLASS makes polymorphic queries a UNION. Cost: one join per subclass load. |
| Association Table Mapping (`@ManyToMany` + `@JoinTable`) | `professor_research_fields`, `application_preferred_professors` | Many-to-many needs a join table with a composite PK. |
| Foreign Key Mapping (`@ManyToOne`) | `application.applicant_id`, `application.cycle_id`, `application.desired_field_id` | The "many" side owns the FK column. |
| Embedded Value (`@Embeddable` / `@Embedded`) | `PersonalInfo` columns inside `application` | A value object with no identity of its own, so it doesn't need its own table. |
| Identity Field (`GenerationType.IDENTITY`) | Every root entity | Surrogate keys generated by the database. |
| Enum mapping (`@Enumerated(STRING)`) | `application.status` | Reordering an enum can't corrupt stored rows. |

## Database schema

> **Status:** generated by Hibernate from the #7 entities (PostgreSQL dialect). The full DDL is in [`docs/schema.sql`](docs/schema.sql).

```mermaid
erDiagram
    users ||--o| applicant : "id"
    users ||--o| professor : "id"
    users ||--o| admin : "id"
    applicant ||--o{ application : "applicant_id"
    admission_cycle ||--o{ application : "cycle_id"
    research_field |o--o{ application : "desired_field_id"
    application ||--o{ application_preferred_professors : "application_id"
    professor ||--o{ application_preferred_professors : "professor_id"
    professor ||--o{ professor_research_fields : "professor_id"
    research_field ||--o{ professor_research_fields : "research_field_id"

    users {
        bigint id PK
        varchar name
        varchar email UK
    }
    applicant { bigint id PK,FK }
    professor { bigint id PK,FK }
    admin { bigint id PK,FK }
    research_field {
        bigint id PK
        varchar name UK
    }
    admission_cycle {
        bigint id PK
        varchar name UK
        date application_deadline
        date evaluation_deadline
        boolean active
    }
    application {
        bigint id PK
        bigint applicant_id FK
        bigint cycle_id FK
        bigint desired_field_id FK "nullable until submit"
        varchar first_name
        varchar last_name
        varchar email
        varchar phone_number
        varchar citizenship
        varchar status
        timestamp submitted_at
    }
    application_preferred_professors {
        bigint application_id PK,FK
        bigint professor_id PK,FK
    }
    professor_research_fields {
        bigint professor_id PK,FK
        bigint research_field_id PK,FK
    }
```

**Generating the real DDL** after a model change (PostgreSQL dialect, writes `docs/schema.sql`):

```bash
./mvnw -q package -DskipTests
java -jar target/gradadmissions-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=docs/schema.sql
```

---

## Build and run

Requires JDK 21 or newer. Maven isn't needed: the repo includes the Maven wrapper.

```bash
git clone https://github.com/sanil04/SYSC-4806-Group-Project.git
cd SYSC-4806-Group-Project
./mvnw clean package                                  # Windows: .\mvnw.cmd clean package
java -jar target/gradadmissions-0.0.1-SNAPSHOT.jar    # http://localhost:8080
```

`./mvnw clean package` compiles the code, runs all tests and builds the runnable JAR. To run the tests only, use `./mvnw verify`.

Local runs use an in-memory H2 database, so there's nothing to set up. Data resets on every restart.

**Production:** the managed PostgreSQL database and its Spring profile are tracked in #4. Credentials will be set as Azure App Service environment variables, never committed.

## How we work

1. Take an issue from **Backlog**, assign yourself, move it to **In Progress**.
2. Branch from `main`, one branch per issue: `git checkout -b feature/<issue#>-short-name`
3. Open a PR with `Closes #<issue>` and request **2 reviewers**.
4. `main` is protected. Merging needs 2 approvals and a green CI run.
5. The author merges their own PR. Each merge to `main` deploys to Azure, so every member deploys. Target: 2-3 deploys per week.
6. If you change a model, update the UML and schema above in the same PR.
7. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org): `feat:`, `fix:`, `test:`, `docs:`, `ci:`, `build:`, `refactor:`, `chore:`.

**Weekly scrum:** one member opens an issue titled `Weekly scrum – YYYY-MM-DD`. Everyone comments with *Done this week / Next week / Blockers*, linking issues by number.