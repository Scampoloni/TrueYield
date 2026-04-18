package ch.zhaw.trueyield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TrueyieldApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrueyieldApplication.class, args);
	}

}
