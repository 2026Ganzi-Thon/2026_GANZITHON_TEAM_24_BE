package com.ganzithon.toro.global.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 서버 시간은 한국 시간(Asia/Seoul)으로 다룬다. 응답 날짜에는 +09:00을 붙여서
 * 프론트가 new Date()로 읽어도 시간이 틀어지지 않게 한다.
 */
public final class KoreaTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KoreaTime() {
    }

    public static OffsetDateTime withOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toOffsetDateTime();
    }
}
