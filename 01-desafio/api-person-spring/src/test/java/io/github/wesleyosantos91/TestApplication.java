package io.github.wesleyosantos91;

import org.springframework.boot.SpringApplication;

public class TestApiPersonApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiPersonApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
