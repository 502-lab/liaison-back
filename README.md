# liaison-back

Liaison 백엔드 API. Spring Boot 4 · Java 25 · PostgreSQL 17 · Flyway · JPA.

## 빠르게 띄우기

### 웹·앱 개발자: API와 DB를 함께
```bash
docker compose up --build
# http://localhost:8080/actuator/health → {"status":"UP"}
```
DB 데이터는 `db-data` 볼륨에 남습니다. 볼륨까지 지우려면 `docker compose down -v`를 실행합니다.

### 백엔드 개발자: DB만 띄우고 IDE에서 실행
```bash
docker compose up db
./gradlew bootRun
```
PC에 이미 PostgreSQL이 5432 포트를 쓰고 있으면 `.env`에 `DB_PORT=15432`를 넣고, 앱은 `DB_URL=jdbc:postgresql://localhost:15432/liaison ./gradlew bootRun`으로 실행합니다.
Docker Compose 없이 띄우려면 IDE에서 `src/test/java/com/liaison/TestLiaisonBackApplication`을 실행합니다(Testcontainers가 DB를 띄웁니다).

## 자주 쓰는 명령
| 명령 | 하는 일 |
|---|---|
| `./gradlew build` | 형식 검사 + 테스트(PostgreSQL 17 컨테이너) + 실행 jar |
| `./gradlew test` | 테스트만 |
| `./gradlew spotlessApply` | 코드 형식 자동 정리 |

테스트에는 Docker가 필요합니다.

## 규칙
- **실제 학생 데이터를 로컬·개발 서버에 넣지 않습니다.** 시드는 실제 학생과 닮지 않은 가짜 이름·학교만 씁니다.
- **비밀값은 커밋하지 않습니다.** 환경변수로 넣습니다. 로컬 예시는 `.env.example`입니다.
- **시간:** DB와 서버는 UTC, 마감·알림·만 나이 계산은 `AppTime.SEOUL`(한국 시간)을 명시합니다. 현재 시각은 주입받은 `Clock`으로 얻습니다.
- **스키마:** Flyway 마이그레이션(`src/main/resources/db/migration`)으로만 바꿉니다. JPA는 `validate`입니다.
- **응답:** 성공은 `ApiResponse`(code·status·message·data), 실패는 RFC 9457 Problem Details + `code`입니다. 오류 응답과 로그에 개인정보(이름, 입력값, 토큰, 사진 주소)를 넣지 않습니다.

## 📚 컨벤션

작업 전에 아래 문서를 확인해주세요.

공통 규칙은 공통 문서 저장소 [liaison-docs](https://github.com/502-lab/liaison-docs)에 있습니다(이 저장소의 `docs/common/`). AI 도구는 `AGENTS.md`·`CLAUDE.md`를 통해 자동으로 읽습니다.

- [AI 작업 규칙](docs/common/AI_RULES.md)
- [용어집](docs/common/GLOSSARY.md)
- [코드·API 규칙](docs/common/CODE_CONVENTIONS.md)
- [커밋·브랜치·PR 규칙](docs/common/GIT_CONVENTIONS.md)
- [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)

처음 받을 때는 `git clone --recursive`, 이미 받았다면 `git submodule update --init`을 실행합니다.
