# AGENTS.md

## Project Overview

This repository is for the graduation project:
"AI-based Real-time Security Threat Detection and Analysis System".

The system collects security-related logs, streams them through Kafka,
analyzes them with a Spring Boot backend and an AI inference service,
stores results in MySQL, and visualizes detected threats on a React dashboard.

The goal of this project is not only to make the application work,
but also to keep the architecture, code structure, documentation, and commit history clean enough
to be used as a portfolio project.

## Main Architecture

Expected core flow:

1. Log source or log generator produces HTTP/security logs.
2. Fluent Bit or a test producer sends logs to Kafka.
3. Spring Boot Kafka consumer receives logs from `log-topic`.
4. Spring Boot parses logs and calls the AI service when needed.
5. AI service returns prediction results such as threat score, attack type, or anomaly status.
6. Spring Boot stores parsed logs and detected threat results in MySQL.
7. React dashboard displays logs, threat events, and summary statistics.

Target stack:

- Backend: Java 17, Spring Boot, Spring Kafka, Spring Data JPA
- Message broker: Kafka
- Database: MySQL
- AI service: Python, FastAPI
- Frontend: React
- Infra/dev environment: Docker, Docker Compose, WSL2, Windows local development

## Repository Structure

Typical structure:

- `BackEnd/`: Spring Boot backend
- `frontend/`: React frontend
- `ai/` or `AI/`: FastAPI AI inference server
- `docs/`: project documents, architecture diagrams, API specs, ERD, planning docs
- `dev_tools/`: test log generators, parsed logs, scripts, local development utilities
- `docker-compose.yml`: local Kafka, Zookeeper, MySQL, or related infrastructure

If the actual folder name differs, inspect the repository before making changes.

## Development Rules

### General

- Do not rewrite large parts of the project unless the task explicitly asks for it.
- Prefer small, reviewable changes.
- Preserve the existing architecture unless there is a clear reason to change it.
- When modifying code, explain which layer is affected: frontend, backend, AI, infra, or docs.
- Keep portfolio readability in mind. Code and documents should be understandable to reviewers.

### Backend Rules

- Follow the existing Spring Boot package structure.
- Use constructor injection with `@RequiredArgsConstructor` where appropriate.
- Keep business logic in service classes, not controllers.
- Keep Kafka consumer logic thin; delegate parsing, AI calls, and DB save logic to services.
- Use DTOs for external API requests/responses.
- Do not expose entity objects directly through controllers unless the existing code already does so.
- Use meaningful method names such as:
  - `parseLogMessage`
  - `analyzeThreat`
  - `saveLogEntry`
  - `createDetectedThreat`
- For database time fields, prefer `LocalDateTime`.
- Be careful with transactional boundaries. Use `@Transactional` in service methods that save or update DB records.

### Kafka Rules

- Default Kafka topic is `log-topic` unless the project config says otherwise.
- Do not randomly change consumer group IDs, topic names, or serializer settings.
- If changing Kafka configuration, also update documentation or comments explaining why.
- Handle malformed messages safely so one bad log does not crash the consumer.

### AI Service Rules

- FastAPI inference endpoints should return predictable JSON responses.
- The backend should be resilient when the AI service is unavailable.
- If mocking AI responses for tests, clearly mark them as test or local-development logic.
- Do not hardcode production-like AI results in business logic.

### Frontend Rules

- Keep React components simple and readable.
- Separate API calls from UI rendering when possible.
- Dashboard pages should prioritize:
  - recent log list
  - detected threat list
  - severity/threat score
  - timestamp
  - source IP or URL path if available
- Avoid unnecessary UI libraries unless already used in the project.

### Documentation Rules

- Update `README.md` or files under `docs/` when architecture, setup commands, API paths, or execution flow changes.
- Prefer Markdown diagrams or image references under `docs/images/`.
- Documentation should explain:
  - what the system does
  - why Kafka/Spring/AI/MySQL/React are used
  - how to run the project locally
  - how the full pipeline works
- Write documentation in Korean unless the existing file is mostly English.

## Test and Verification Commands

Before finishing backend-related changes, run one or more of the following if possible:

```bash
cd BackEnd
./gradlew test
```
```bash
cd frontend
npm install
npm start
```

```bash
cd ai
python -m pytest
```

If a command fails because the environment is missing dependencies, report the failure clearly and explain what was attempted.

## Git Workflow
- Main working branch is usually develop.
- For new work, prefer feature branches such as:
    - feature/log-ai-pipeline
    - feature/dashboard-threat-list
    - docs/update-architecture
    - refactor/backend-log-service
- Do not force-push or delete branches unless explicitly asked.
- Do not modify unrelated files.
- Before finishing, summarize:
  1. changed files
  2. main changes
  3. tests or checks performed
  4. remaining TODOs or risks

## Commit Message Style

Use concise conventional-style commit messages:
- feat: add Kafka log consumer
- fix: handle malformed log message
- docs: update architecture flow
- test: add LogService unit test
- refactor: separate AI analysis logic

## Safety Rules
- Do not commit .env, credentials, API keys, database passwords, or large datasets.
- Do not commit AI model binaries or large generated files unless explicitly required.
- Respect .gitignore.
- If a task requires secrets, use placeholders and document where the user should configure them.

## Preferred Response Style
When answering the user:
- Be concise but explain the reason behind important changes.
- For bugs, explain the cause and the fix.
- For architecture changes, include the before/after flow.
- For commands, provide Windows-friendly commands when possible.
- Avoid pretending that tests passed if they were not actually run.