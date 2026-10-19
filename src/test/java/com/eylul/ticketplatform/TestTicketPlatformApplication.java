package com.eylul.ticketplatform;

import org.springframework.boot.SpringApplication;

public class TestTicketPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(TicketPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
