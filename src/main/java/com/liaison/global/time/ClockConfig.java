package com.liaison.global.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 항상 이 Clock으로 얻는다. 테스트에서 고정 Clock으로 바꿔 마감·삭제 예정일 같은 시간 규칙을 검증한다.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
