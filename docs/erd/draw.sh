#!/usr/bin/env bash
# ERD 그림을 각자 PC에서 만든다(README 3장). 결과물은 docs/erd/build/에 두고 커밋하지 않는다.
#   docs/erd/draw.sh design   DBML 설계 그림. 임시 PostgreSQL에 넣어 보므로 설계 SQL이 실제로 들어가는지도 확인된다
#   docs/erd/draw.sh actual   Flyway를 적용한 로컬 DB(docker compose의 db)의 실제 그림
#   docs/erd/draw.sh design consent   영역 하나(그 영역 표 + 그 표가 가리키는 다른 표)만. 영역은 docs/erd의 파일 이름
#   docs/erd/draw.sh design v1.0      버전 하나(v1.0 표 + 그 표가 가리키는 다른 표)만. v1이면 v1.x 전부. 표 note의 ' · vX.Y'로 고름
#   docs/erd/draw.sh design --ko      그림 위 이름에 한글 이름·버전을 붙임(note 첫 마디에서 뽑음, README 4장 규칙)
#   영역·버전·--ko는 함께 쓸 수 있다. 예: docs/erd/draw.sh design consent v1.0 --ko
# 필요한 것: Docker, Node.js(npx), python3
#
# Liam의 SQL 읽기는 큰 파일에서 표·칸 설명과 인덱스를 빠뜨린다. 그래서 DB 카탈로그를 tbls로 JSON으로 뽑아 Liam에 준다.
set -euo pipefail

DBML_CLI="@dbml/cli@12.0.0"
LIAM_CLI="@liam-hq/cli@0.7.24"
TBLS_IMAGE="ghcr.io/k1low/tbls:v1.96.1@sha256:0d90f07aed0c423a6012d542358c10b13fcdf15a1074ceee3d73676c87cc2f79"
PG_IMAGE="postgres:17-alpine"

mode="${1:-}"
if [[ "$mode" != "design" && "$mode" != "actual" ]]; then
  echo "사용법: docs/erd/draw.sh design|actual [영역] [vX.Y] [--ko]   영역은 docs/erd의 파일 이름(예: consent)" >&2
  exit 2
fi
shift
area="" ver="" ko=""
for arg in "$@"; do
  case "$arg" in
    --ko) ko=1 ;;
    v[0-9]*) ver="${arg#v}" ;;
    *) area="$arg" ;;
  esac
done

root="$(git rev-parse --show-toplevel)"
cd "$root"
if [[ -n "$area" && ( "$area" == "project" || ! -f "docs/erd/$area.dbml" ) ]]; then
  echo "영역 파일이 없습니다: docs/erd/$area.dbml" >&2
  exit 2
fi
if [[ -n "$ver" && ! "$ver" =~ ^[0-9]+(\.[0-9]+)?$ ]]; then
  echo "버전은 v1.0이나 v1처럼 적습니다: v$ver" >&2
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
  # printenv가 붙인 마지막 줄바꿈 하나만 떼고, 값의 공백·특수 문자는 그대로 둔 채 주소용으로 인코딩한다.
  env_enc() {
    docker exec "$cid" printenv "$1" | python3 -I -c 'import sys, urllib.parse
v = sys.stdin.read()
print(urllib.parse.quote(v[:-1] if v.endswith("\n") else v, safe=""))'
  }
  user="$(env_enc POSTGRES_USER)"
  db="$(env_enc POSTGRES_DB)"
  pw="$(env_enc POSTGRES_PASSWORD)"
  tbls_json "$cid" "postgres://$user:$pw@localhost:5432/$db?sslmode=disable"
fi

# 표 이름 앞의 public.을 떼고, 고른 영역·버전의 표와 그 표가 가리키는 표만 남기고, --ko면 한글 이름을 붙인다.
out="$mode${area:+-$area}${ver:+-v$ver}${ko:+-ko}"
python3 -I - "$build/$mode.tbls.json" "$build/$out.json" "${area:+docs/erd/$area.dbml}" "$ver" "$ko" << 'PY'
import json, re, sys

src, dst, area_file, ver, ko = sys.argv[1:6]
schema = json.load(open(src, encoding="utf-8"))
# 표가 하나도 없는 DB(마이그레이션 전)면 tbls가 목록 대신 null을 준다.
schema["tables"], schema["relations"] = schema.get("tables") or [], schema.get("relations") or []
tables, relations = schema["tables"], schema["relations"]


def bare(name):
    return name[len("public."):] if name.startswith("public.") else name


def cut(text, marks):
    end = len(text)
    for mark in marks:
        i = text.find(mark)
        if i != -1:
            end = min(end, i)
    return text[:end].strip()


def version(table):
    m = re.search(r" · v(\d+\.\d+)", table.get("comment", ""))
    return m.group(1) if m else ""


for t in tables:
    t["name"] = bare(t["name"])
    for item in t.get("constraints", []) + t.get("indexes", []):
        for key in ("table", "referenced_table"):
            if key in item:
                item[key] = bare(item[key])
for r in relations:
    r["table"], r["parent_table"] = bare(r["table"]), bare(r["parent_table"])

# 고르기: 영역·버전에 맞는 표 + 그 표가 가리키는 표. 선은 고른 표에서 나가는 것만 둔다.
if area_file or ver:
    focus = {t["name"] for t in tables}
    if area_file:
        focus &= set(re.findall(r"^Table (\w+) \{$", open(area_file, encoding="utf-8").read(), re.M))
    if ver:
        focus = {t["name"] for t in tables if t["name"] in focus and (version(t) == ver or version(t).startswith(ver + "."))}
    if not focus:
        sys.exit("고른 표가 없습니다")
    keep = focus | {r["parent_table"] for r in relations if r["table"] in focus}
    schema["tables"] = tables = [t for t in tables if t["name"] in keep]
    for t in tables:
        t["constraints"] = [
            c for c in t.get("constraints", [])
            if c.get("type") != "FOREIGN KEY" or (t["name"] in focus and c.get("referenced_table") in keep)
        ]
    schema["relations"] = relations = [r for r in relations if r["table"] in focus]
    print(f"고른 표 {len(focus)}개 + 가리키는 다른 표 {len(keep - focus)}개")

# --ko: 표는 '[v1.0·가칭] 한글 이름 · 코드 이름', 칸은 '한글 이름 · 코드 이름'.
# 표보다 나중 버전에 더하는 칸은 앞에 '[v1.3부터]'를 붙인다. 관계·제약·인덱스의 이름도 같이 바꾼다.
if ko:
    fixed = {"id": "고유 번호", "organization_id": "학원", "created_at": "만든 시각", "updated_at": "바꾼 시각"}
    tmap, cmap = {}, {}
    for t in tables:
        code, note, v = t["name"], t.get("comment", ""), version(t)
        tags = ([f"v{v}"] if v else []) + (["가칭"] if re.search(r" · 가칭(\.| ·|$)", note) else [])
        name = cut(note, ["(", " · ", ". "])
        label = f"{name} · {code}" if name else code
        tmap[code] = f"[{'·'.join(tags)}] {label}" if tags else label
        cmap[code] = {}
        for c in t["columns"]:
            cnote = c.get("comment", "")
            cname = fixed.get(c["name"]) or ("" if cnote.startswith("→") else cut(cnote, [". ", "(", " → ", " · "]))
            later = re.match(r"^[^.]+\. v(\d+\.\d+) ·", cnote)
            prefix = f"[v{later.group(1)}부터] " if later and later.group(1) != v else ""
            cmap[code][c["name"]] = f"{prefix}{cname} · {c['name']}" if cname else c["name"]

    def cols(table, names):
        return [cmap.get(table, {}).get(n, n) for n in names]

    for t in tables:
        code = t["name"]
        for c in t["columns"]:
            c["name"] = cmap[code][c["name"]]
        for item in t.get("constraints", []) + t.get("indexes", []):
            if "columns" in item:
                item["columns"] = cols(code, item["columns"])
            ref = item.get("referenced_table")
            if ref:
                item["referenced_columns"] = cols(ref, item.get("referenced_columns", []))
                item["referenced_table"] = tmap.get(ref, ref)
            if "table" in item:
                item["table"] = tmap.get(item["table"], item["table"])
        t["name"] = tmap[code]
    for r in relations:
        r["columns"], r["parent_columns"] = cols(r["table"], r["columns"]), cols(r["parent_table"], r["parent_columns"])
        r["table"], r["parent_table"] = tmap.get(r["table"], r["table"]), tmap.get(r["parent_table"], r["parent_table"])

json.dump(schema, open(dst, "w", encoding="utf-8"), ensure_ascii=False)
PY

rm -rf "${build:?}/$out"
npx -y "$LIAM_CLI" erd build --format tbls --input "$build/$out.json" --output-dir "$build/$out"
echo "보기: python3 -m http.server --bind 127.0.0.1 --directory $build/$out 8000 그리고 브라우저에서 http://localhost:8000/?showMode=KEY_ONLY (키 칸만)"
