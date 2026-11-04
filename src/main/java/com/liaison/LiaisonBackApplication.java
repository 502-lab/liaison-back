package com.liaison;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiaisonBackApplication {

    public static void main(String[] args) {
        // 저장·계산은 UTC로 통일한다. 한국 시간 계산은 AppTime.SEOUL을 명시해서 쓴다.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(LiaisonBackApplication.class, args);
    }
}
