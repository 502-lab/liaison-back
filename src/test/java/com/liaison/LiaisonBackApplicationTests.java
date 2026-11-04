package com.liaison;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LiaisonBackApplicationTests {

    @Autowired MockMvcTester mvc;
    @Autowired Clock clock;

    @Test
    void 컨텍스트가_PostgreSQL_17과_Flyway로_뜬다() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
    }

    @Test
    void 헬스체크는_상세_정보_없이_UP을_돌려준다() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("{\"status\":\"UP\"}");
        assertThat(mvc.get().uri("/actuator/health"))
                .bodyJson()
                .doesNotHavePath("$.components");
    }

    @Test
    void 노출하지_않은_actuator_엔드포인트는_404다() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(404);
    }
}
