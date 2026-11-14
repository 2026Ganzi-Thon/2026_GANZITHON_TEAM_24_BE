package com.ganzithon.toro;

import com.ganzithon.toro.global.time.KoreaTime;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ToroApplication {

	public static void main(String[] args) {
		// 배포 서버가 UTC여도 LocalDateTime.now()가 한국 시간이 되게 한다
		TimeZone.setDefault(TimeZone.getTimeZone(KoreaTime.ZONE));
		SpringApplication.run(ToroApplication.class, args);
	}

}
