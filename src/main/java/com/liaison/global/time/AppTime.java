package com.liaison.global.time;

import java.time.ZoneId;

/** 서비스 전체에서 쓰는 시간 기준. DB와 서버는 UTC, 마감·알림·만 나이 계산은 한국 시간으로 한다. */
public final class AppTime {

    /** 마감(09:00~23:59), 리마인드(21:00), 미제출 통보, 만 14세 판정 등 업무 시각의 기준. */
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private AppTime() {}
}
