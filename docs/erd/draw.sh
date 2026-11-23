#!/usr/bin/env bash
# ERD 그림을 각자 PC에서 만든다(README 3장). 결과물은 docs/erd/build/에 두고 커밋하지 않는다.
#   docs/erd/draw.sh design   DBML 설계 그림. 임시 PostgreSQL에 넣어 보므로 설계 SQL이 실제로 들어가는지도 확인된다
#   docs/erd/draw.sh actual   Flyway를 적용한 로컬 DB(docker compose의 db)의 실제 그림
#   docs/erd/draw.sh design consent   영역 하나(그 영역 표 + 그 표가 가리키는 다른 영역 표)만. 표가 많으면 이쪽이 읽기 쉽다
# 필요한 것: Docker, Node.js(npx), python3
#
# Liam의 SQL 읽기는 큰 파일에서 표·칸 설명과 인덱스를 빠뜨린다. 그래서 DB 카탈로그를 tbls로 JSON으로 뽑아 Liam에 준다.
set -euo pipefail

DBML_CLI="@dbml/cli@12.0.0"
LIAM_CLI="@liam-hq/cli@0.7.24"
TBLS_IMAGE="ghcr.io/k1low/tbls:v1.96.1@sha256:0d90f07aed0c423a6012d542358c10b13fcdf15a1074ceee3d73676c87cc2f79"
PG_IMAGE="postgres:17-alpine"

mode="${1:-}"
area="${2:-}"
if [[ "$mode" != "design" && "$mode" != "actual" ]]; then
  echo "사용법: docs/erd/draw.sh design|actual [영역]   영역은 docs/erd의 파일 이름(예: consent)" >&2
  exit 2
fi

root="$(git rev-parse --show-toplevel)"
cd "$root"
if [[ -n "$area" && ( "$area" == "project" || ! -f "docs/erd/$area.dbml" ) ]]; then
  echo "영역 파일이 없습니다: docs/erd/$area.dbml" >&2
  exit 2
fi
build="docs/erd/build"
mkdir -p "$build"

# $1: DB가 도는 컨테이너, $2: 그 컨테이너 안에서 본 DB 주소
tbls_json() {
  docker run --rm --network "container:$1" --user "$(id -u):$(id -g)" \
    -v "$root/$build:/out" "$TBLS_IMAGE" \
    out -t json -o "/out/$mode.tbls.json" "$2"
}

if [[ "$mode" == "design" ]]; then
  cat docs/erd/*.dbml > "$build/design.dbml"
  npx -y -p "$DBML_CLI" dbml2sql --postgres -o "$build/design.sql" "$build/design.dbml"

  name="liaison-erd-design-$$"
  # 그림만 그리고 지우는 임시 DB다. 포트를 열지 않고 비밀번호도 두지 않는다.
  docker run -d --rm --name "$name" -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=erd "$PG_IMAGE" > /dev/null
  trap 'docker rm -f "$name" > /dev/null 2>&1 || true' EXIT
  # 초기화 중의 임시 서버는 TCP를 열지 않으므로, 127.0.0.1로 응답하면 진짜 서버가 뜬 것이다.
  for _ in $(seq 1 60); do
    docker exec "$name" pg_isready -q -h 127.0.0.1 -U postgres -d erd && break
    sleep 1
  done
  docker exec -i "$name" psql -q -v ON_ERROR_STOP=1 -h 127.0.0.1 -U postgres -d erd < "$build/design.sql"
  tbls_json "$name" "postgres://postgres@localhost:5432/erd?sslmode=disable"
else
  cid="$(docker compose ps -q db)"
  if [[ -z "$cid" ]]; then
    echo "먼저 docker compose up --build -d 로 앱과 DB를 띄우세요(앱이 뜨면서 Flyway를 적용합니다)." >&2
    exit 1
  fi
  user="$(docker exec "$cid" printenv POSTGRES_USER)"
  db="$(docker exec "$cid" printenv POSTGRES_DB)"
  pw="$(docker exec "$cid" printenv POSTGRES_PASSWORD | python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.stdin.read().strip(), safe=""))')"
  tbls_json "$cid" "postgres://$user:$pw@localhost:5432/$db?sslmode=disable"
fi

# 표 이름 앞의 public.을 떼고, 영역을 골랐으면 그 영역 표와 그 표가 가리키는 표만 남긴다.
out="$mode${area:+-$area}"
python3 -I - "$build/$mode.tbls.json" "$build/$out.json" "${area:+docs/erd/$area.dbml}" << 'PY'
import json, re, sys

src, dst, area_file = sys.argv[1], sys.argv[2], sys.argv[3]
schema = json.load(open(src, encoding="utf-8"))


def bare(name):
    return name[len("public."):] if name.startswith("public.") else name


for t in schema["tables"]:
    t["name"] = bare(t["name"])
    for item in t.get("constraints", []) + t.get("indexes", []):
        for key in ("table", "referenced_table"):
            if key in item:
                item[key] = bare(item[key])
for r in schema.get("relations", []):
    r["table"], r["parent_table"] = bare(r["table"]), bare(r["parent_table"])

if area_file:
    area = set(re.findall(r"^Table (\w+) \{$", open(area_file, encoding="utf-8").read(), re.M))
    keep = area | {r["parent_table"] for r in schema.get("relations", []) if r["table"] in area}
    schema["tables"] = [t for t in schema["tables"] if t["name"] in keep]
    for t in schema["tables"]:
        t["constraints"] = [
            c for c in t.get("constraints", [])
            if c.get("type") != "FOREIGN KEY" or (t["name"] in area and c.get("referenced_table") in keep)
        ]
    schema["relations"] = [r for r in schema.get("relations", []) if r["table"] in area]
    print(f"영역 표 {len(area & keep)}개 + 가리키는 다른 영역 표 {len(keep - area)}개")

json.dump(schema, open(dst, "w", encoding="utf-8"), ensure_ascii=False)
PY

rm -rf "${build:?}/$out"
npx -y "$LIAM_CLI" erd build --format tbls --input "$build/$out.json" --output-dir "$build/$out"
echo "보기: python3 -m http.server --directory $build/$out 8000 그리고 브라우저에서 http://localhost:8000/?showMode=KEY_ONLY (키 칸만)"
