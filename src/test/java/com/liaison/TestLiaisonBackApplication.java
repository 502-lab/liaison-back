package com.liaison;

import org.springframework.boot.SpringApplication;

/** docker compose 없이 Testcontainers DB로 앱을 띄울 때 쓴다(IDE에서 이 클래스를 실행). */
public class TestLiaisonBackApplication {

    public static void main(String[] args) {
        SpringApplication.from(LiaisonBackApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
