package com.liaison.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

/**
 * 로그 설정은 보안 핵심 경로인 logback-spring.xml에만 둔다.
 * application*.yml·properties는 운영자 승인 경로가 아니므로 logging.* 키가 들어오면 실패한다.
 */
class LoggingConfigLocationTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void 설정_파일에_logging_키가_없다() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(RESOURCES)) {
            for (Path file : files.filter(LoggingConfigLocationTest::isApplicationConfig).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                List<String> keys =
                        file.getFileName().toString().endsWith(".properties")
                                ? loggingKeysInProperties(text)
                                : loggingKeysInYaml(text);
                keys.forEach(key -> found.add(file.getFileName() + ": " + key));
            }
        }
        assertThat(found).as("로그 설정은 logback-spring.xml로 옮기세요").isEmpty();
    }

    @Test
    void 검사기는_yaml과_properties의_logging_키를_찾는다() {
        String yaml =
                """
                server:
                  port: 8080
                ---
                logging:
                  level:
                    org.hibernate.orm.jdbc.bind: TRACE
                ---
                logging.level.root: DEBUG
                """;
        assertThat(loggingKeysInYaml(yaml)).containsExactly("logging", "logging.level.root");
        assertThat(loggingKeysInProperties("logging.level.com.liaison=DEBUG\nserver.port=8080"))
                .containsExactly("logging.level.com.liaison");
        assertThat(loggingKeysInYaml("server:\n  port: 8080\n")).isEmpty();
    }

    @Test
    void SQL_바인딩_값_로그는_늘_꺼져_있다() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        NodeList loggers =
                factory.newDocumentBuilder()
                        .parse(RESOURCES.resolve("logback-spring.xml").toFile())
                        .getElementsByTagName("logger");
        String bindLevel = null;
        for (int i = 0; i < loggers.getLength(); i++) {
            Element logger = (Element) loggers.item(i);
            if ("org.hibernate.orm.jdbc.bind".equals(logger.getAttribute("name"))) {
                bindLevel = logger.getAttribute("level");
            }
        }
        assertThat(bindLevel).isEqualTo("OFF");
    }

    private static boolean isApplicationConfig(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith("application")
                && (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties"));
    }

    static List<String> loggingKeysInYaml(String text) {
        List<String> keys = new ArrayList<>();
        for (Object document : new Yaml().loadAll(new StringReader(text))) {
            if (document instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    String name = String.valueOf(key);
                    if (name.equals("logging") || name.startsWith("logging.")) {
                        keys.add(name);
                    }
                }
            }
        }
        return keys;
    }

    static List<String> loggingKeysInProperties(String text) {
        Properties properties = new Properties();
        try (Reader reader = new StringReader(text)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties.stringPropertyNames().stream()
                .filter(name -> name.startsWith("logging."))
                .sorted()
                .toList();
    }
}
