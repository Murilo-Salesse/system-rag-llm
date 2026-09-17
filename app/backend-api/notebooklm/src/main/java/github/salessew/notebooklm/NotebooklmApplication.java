package github.salessew.notebooklm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class NotebooklmApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotebooklmApplication.class, args);
	}

}
