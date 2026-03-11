package com.mizan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MizanApplication {
    public static void main(String[] args) {
        SpringApplication.run(MizanApplication.class, args);
    }
}
