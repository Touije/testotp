package com.exemple.testotp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TestotpApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestotpApplication.class, args);
    }

}
