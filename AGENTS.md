# AGENTS.md — liaison-back

AI 도구(Claude Code, Cursor, Copilot, Codex 등)와 사람 모두 작업 전에 읽습니다.

## 1. 공통 문서 (반드시 먼저)
`docs/common/`은 공통 문서 저장소 [502-lab/liaison-docs](https://github.com/502-lab/liaison-docs)를 연결한 git submodule입니다. 비어 있으면 `git submodule update --init`을 실행합니다.

- `docs/common/AI_RULES.md` — 절대 하지 않는 것, 반드시 하는 것
- `docs/common/GLOSSARY.md` — 표준어와 코드 이름
- `docs/common/CODE_CONVENTIONS.md` — 이름·API·시간·로그·테스트 규칙
- `docs/common/GIT_CONVENTIONS.md` — 커밋·브랜치·PR 규칙
- `docs/common/DOMAIN.md` — 도메인 모델 요약
- `docs/common/PRIVACY_RULES.md` — 개인정보 규칙

공통 문서를 고칠 때는 이 저장소가 아니라 liaison-docs에 PR을 올립니다.

## 2. 이 저장소
- **역할:** Liaison 백엔드 API(학생 앱·선생님 앱·관리자 웹이 함께 씀)
- **스택:** Spring Boot 4 · Java 25 · PostgreSQL 17 · Flyway · JPA · Gradle
- **실행·빌드:** README 참고(`docker compose up --build`, `./gradlew build`)
- **스키마:** Flyway 마이그레이션으로만 바꿉니다. 스키마·API 변경은 개발 총괄 검증을 거칩니다.
- **커밋 전 비밀키 검사:** `git config core.hooksPath .githooks` (gitleaks 필요: `brew install gitleaks`). CI(Secret Scan)도 검사합니다.
- **공개 저장소입니다.** 실제 데이터·비밀값·법무 원문을 올리지 않습니다.
