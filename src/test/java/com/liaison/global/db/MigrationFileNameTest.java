package com.liaison.global.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Flyway 파일 이름 규칙(BACKEND.md 5장)을 검사한다. 버전은 UTC 연월일시분초 14자리다. */
class MigrationFileNameTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
    private static final Pattern NAME =
            Pattern.compile("^V(\\d{14})__[a-z0-9]+(?:_[a-z0-9]+)*\\.sql$");
    private static final DateTimeFormatter VERSION =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    // 시계가 조금 어긋난 PC를 봐준다. 한국 시간으로 잘못 적으면 9시간 미래라 걸린다.
    private static final Duration FUTURE_ALLOWANCE = Duration.ofMinutes(10);

    @Test
    void 마이그레이션_파일_이름이_규칙에_맞다() throws IOException {
        List<String> names;
        try (Stream<Path> files = Files.list(MIGRATION_DIR)) {
            names =
                    files.map(p -> p.getFileName().toString())
                            .filter(n -> !n.equals(".gitkeep"))
                            .toList();
        }

        assertThat(problems(names, Clock.systemUTC())).isEmpty();
    }

    @Test
    void 검사기는_틀린_이름을_찾는다() {
        Clock now = Clock.fixed(Instant.parse("2026-11-23T05:30:05Z"), ZoneOffset.UTC);

        assertThat(problems(List.of("V20261123053005__create_consent_documents.sql"), now))
                .isEmpty();
        assertThat(problems(List.of("V001__create_consent_documents.sql"), now)).hasSize(1);
        assertThat(problems(List.of("V20261123053005_create_x.sql"), now)).hasSize(1);
        assertThat(problems(List.of("V20261123053005__Create_X.sql"), now)).hasSize(1);
        assertThat(problems(List.of("V20261123053005__create_x.SQL"), now)).hasSize(1);
        assertThat(problems(List.of("V20261332000000__create_x.sql"), now)).hasSize(1);
        assertThat(problems(List.of("V20261123143005__create_x.sql"), now))
                .as("한국 시간으로 적어 9시간 미래인 버전")
                .hasSize(1);
        assertThat(
                        problems(
                                List.of(
                                        "V20261123053005__create_a.sql",
                                        "V20261123053005__create_b.sql"),
                                now))
                .as("겹친 버전")
                .hasSize(1);
    }

    static List<String> problems(List<String> names, Clock clock) {
        LocalDateTime latestAllowed =
                LocalDateTime.ofInstant(clock.instant().plus(FUTURE_ALLOWANCE), ZoneOffset.UTC);
        List<String> problems = new ArrayList<>();
        Map<String, String> seen = new HashMap<>();
        for (String name : names) {
            Matcher m = NAME.matcher(name);
            if (!m.matches()) {
                problems.add(name + ": V<UTC 연월일시분초 14자리>__<영어_snake_case>.sql 형식이 아님");
                continue;
            }
            String version = m.group(1);
            LocalDateTime at;
            try {
                at = LocalDateTime.parse(version, VERSION);
            } catch (DateTimeParseException e) {
                problems.add(name + ": 버전이 실제로 있는 날짜·시각이 아님");
                continue;
            }
            if (at.isAfter(latestAllowed)) {
                problems.add(name + ": 버전이 지금(UTC)보다 미래임. date -u +%Y%m%d%H%M%S 로 다시 만들 것");
            }
            String other = seen.putIfAbsent(version, name);
            if (other != null) {
                problems.add(name + ": " + other + " 와 버전이 겹침");
            }
        }
        return problems;
    }
}
