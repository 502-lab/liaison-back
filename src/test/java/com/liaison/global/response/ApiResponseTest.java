package com.liaison.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void success는_200과_success를_담는다() {
        ApiResponse<String> response = ApiResponse.success("데이터");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("데이터");
    }

    @Test
    void created와_accepted는_각각_201과_202다() {
        assertThat(ApiResponse.created(null).code()).isEqualTo(201);
        assertThat(ApiResponse.accepted(null).code()).isEqualTo(202);
    }
}
