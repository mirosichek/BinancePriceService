package com.example.BinancePriceService;

import org.springframework.boot.SpringApplication;

public class TestBinancePriceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(BinancePriceServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
