# 설계 ERD (DBML)

v1~v3에 쓸 DB 표의 설계 전체입니다. 설계하는 동안은 이 폴더의 DBML이 표 정의의 원본입니다. 담당 영역과 결정 이유는 노션 ERD 명세서(내부)에 둡니다.

## 1. 원본 규칙 (운영자 결정, 11/23)

- **DBML은 설계 목표, Flyway는 지금 DB입니다.** 설계 목표는 v1~v3 전체이고, 지금 DB는 `src/main/resources/db/migration/`에 쌓인 마이그레이션입니다.
- **설계와 다르게 만들 때만 DBML을 고칩니다.** 설계대로 만든 마이그레이션 PR은 DBML을 고치지 않아도 됩니다. 설계와 다르게 만들면 같은 PR에서 DBML도 고칩니다(PR 템플릿 체크 항목).
- **둘이 어긋나면 Flyway가 맞습니다.** 어긋난 것을 찾으면 DBML을 Flyway에 맞춰 고칩니다.
- **v1.0을 마칠 때 한 번 대조합니다.** 실제 그림(아래 3-2)과 설계 그림(3-1)을 나란히 봅니다.
- 표를 새로 설계하거나 바꾸는 결정은 개발 총괄의 검증을 거칩니다. 데이터를 새로 모으거나 보관 기간이 바뀌면 법무 확인도 필요합니다(AI_RULES 2-2).

## 2. 파일

영역마다 파일 하나입니다. `Project` 블록은 `project.dbml`에만 둡니다. 관계(`Ref`)는 참조하는 쪽(자식 표)의 파일에 둡니다. 다른 파일의 표를 가리켜도 되므로, 변환할 때는 파일을 모두 합칩니다(3-1).

| 파일 | 영역 | 표 |
|---|---|---|
| `project.dbml` | 프로젝트 설정 | — |
| `consent.dbml` | 동의 | 동의서 버전·동의 항목, 약관 동의, 개인정보 동의, 14세 미만 서면 동의서, 선생님 가입 고지 확인 |
| `agreement.dbml` | 위탁·해지 | 위탁 문서 동의, 재수탁자 변경과 응답·재안내, 학원 해지 기록 |
| `audit.dbml` | 기록 | 접근 기록, 권한 변경 기록, 열람·정정·삭제 요청 기록 |
| `settings.dbml` | 설정 | 기능 스위치·켠 버전 목록과 변경 기록 |
| `deletion.dbml` | 삭제 | 자동 삭제 작업 실행 기록, 지운 데이터 ID 목록 |
| `notification.dbml` | 알림 | 알림·알림 항목, 운영자 안내와 받는 사람 |
| `performance.dbml` | 수행 기록 | 수행 기록과 수정 이력 |
| `exam.dbml` | 시험 점수 | 시험, 시험 점수 |
| `counseling.dbml` | 원장 도구·상담 | 선생님 메모·인수인계 메모, 상담 기록, 주의 학생 기준·알림 |
| `notice.dbml` | 공지 | 공지·받는 역할·수정 기록·읽음, 바뀐 내용 안내 확인 |
| `screening.dbml` | 판독 | AI 판독 결과, 유형 분석, 재촬영 요청 |
| `message.dbml` | 메시지 | 대화·메시지·신고·차단·연락 시간 |

- 아직 이 폴더에 없는 영역(계정·학원·소속·숙제·제출·교재 등)의 표는 칸 note에 `→ [외부] 표.칸 참조 예정`으로만 적었습니다. 그 영역의 파일이 들어오면 같은 PR에서 `Ref` 줄로 바꿉니다.
- 새 영역은 영어 소문자 파일 이름으로 더하고, 이 표에도 한 줄 더합니다.

## 3. 그림 보기

각자 PC에서만 만들어 봅니다. 결과물은 `docs/erd/build/`에 두고 커밋하지 않습니다(`.gitignore`). 그림을 외부에 올리거나 게시하지 않고, Liam의 호스팅·GitHub 앱은 쓰지 않습니다(외부 서비스).

도구는 Node.js의 `npx`로 그때그때 받아 씁니다. 저장소 의존성으로 넣지 않습니다. 버전은 아래처럼 고정하고, 올릴 때는 나온 지 7일이 지난 버전으로 이 문서를 고칩니다(AI_RULES 3-8).

- `@dbml/cli` 12.0.0 (배포: Holistics, DBML 공식)
- `@liam-hq/cli` 0.7.24 (배포: Liam)

Liam은 DBML을 바로 읽지 못합니다. 그래서 설계 그림은 DBML을 PostgreSQL SQL로 바꾼 뒤 그립니다.

### 3-1. 설계 그림 (DBML)

저장소 맨 위 폴더에서 실행합니다.

```bash
mkdir -p docs/erd/build
cat docs/erd/*.dbml > docs/erd/build/design.dbml
npx -y -p @dbml/cli@12.0.0 dbml2sql --postgres -o docs/erd/build/design.sql docs/erd/build/design.dbml
npx -y @liam-hq/cli@0.7.24 erd build --format postgres --input docs/erd/build/design.sql --output-dir docs/erd/build/design
python3 -m http.server --directory docs/erd/build/design 8000
```

브라우저에서 `http://localhost:8000`을 엽니다(파일을 바로 열면 그려지지 않음). 파일을 하나씩 따로 변환하면 다른 파일을 가리키는 `Ref`에서 실패하므로 꼭 합쳐서 변환합니다.

### 3-2. 실제 그림 (Flyway를 적용한 로컬 DB)

```bash
docker compose up --build -d        # 앱이 뜨면서 Flyway를 적용합니다
mkdir -p docs/erd/build
docker compose exec db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --schema-only --no-owner --no-privileges' > docs/erd/build/actual.sql
npx -y @liam-hq/cli@0.7.24 erd build --format postgres --input docs/erd/build/actual.sql --output-dir docs/erd/build/actual
python3 -m http.server --directory docs/erd/build/actual 8001
```

로컬 DB에는 가짜 데이터만 둡니다. `--schema-only`라 데이터는 나오지 않습니다.

## 4. 쓰는 법

- **이름:** 용어집의 코드 이름 규칙을 따릅니다(테이블 snake_case 복수, 시각 `*_at`, 사람을 가리키는 외래 키는 `user_id` 또는 역할을 붙인 소속 id). 아직 확정하지 않은 이름은 용어집 11장의 가칭이고, 확정은 해당 WBS 설계 때입니다.
- **note:** 칸·표가 무엇인지, 버전, 보관 기간, 지켜야 할 제약(CHECK·트리거·RLS)을 짧게 적습니다. 결정 메모에는 날짜와 역할(PM·개발 총괄·법무·운영자)만 적고 사람 이름은 적지 않습니다.
- **공개 저장소입니다(AI_RULES 1-3).** 아래는 적지 않습니다.
  - 사람 이름, 노션·내부 문서 링크와 경로
  - DB 역할 비밀번호, 서버 주소·계정, 운영자 접근 경로
  - 탐지 기준·임계값, 잠금·시도 횟수, 코드 형식·길이 값(서버 설정으로 둠)
- 실제 마이그레이션의 `COMMENT ON`은 좁게 씁니다. 표마다 한 줄과, 이름만으로 뜻이 안 보이는 칸(값만 저장하는 ID, 보관 기간을 세는 기준 칸, 가칭 enum 칸)에만 씁니다. 그 표를 만드는 마이그레이션 안에서 쓰고, 주석만 고치는 마이그레이션은 만들지 않습니다(마이그레이션은 운영자 승인 경로라서).
