package com.liaison.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private final MockMvcTester mvc =
            MockMvcTester.create(
                    MockMvcBuilders.standaloneSetup(new SampleController())
                            .setControllerAdvice(new GlobalExceptionHandler())
                            .build());

    @Test
    void 업무_예외는_ErrorCode의_상태와_문구로_바뀐다() {
        assertThat(mvc.get().uri("/sample/not-found"))
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                        {"status":404,"detail":"요청한 정보를 찾을 수 없어요.","code":"NOT_FOUND"}
                        """);
    }

    @Test
    void 업무_예외의_로그용_메시지는_응답에_나가지_않는다() {
        assertThat(mvc.get().uri("/sample/not-found"))
                .bodyText()
                .doesNotContain("홍길동");
    }

    @Test
    void 처리하지_못한_예외는_500과_일반_문구만_돌려준다() {
        assertThat(mvc.get().uri("/sample/boom"))
                .hasStatus(500)
                .bodyJson()
                .isLenientlyEqualTo("{\"code\":\"INTERNAL_ERROR\",\"detail\":\"잠시 후 다시 시도해 주세요.\"}");
        assertThat(mvc.get().uri("/sample/boom")).bodyText().doesNotContain("secret");
    }

    @Test
    void 입력_검증_실패는_필드와_사유만_돌려주고_입력값은_싣지_않는다() {
        assertThat(
                        mvc.post()
                                .uri("/sample/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                .hasStatus(400)
                .bodyJson()
                .isLenientlyEqualTo(
                        """
                        {"code":"INVALID_REQUEST","errors":[{"field":"name"}]}
                        """);
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample/not-found")
        String notFound() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "학생 홍길동을 찾지 못함");
        }

        @GetMapping("/sample/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @PostMapping("/sample/students")
        String create(@Valid @RequestBody StudentRequest request) {
            return request.name();
        }
    }

    record StudentRequest(@NotBlank String name) {}
}
