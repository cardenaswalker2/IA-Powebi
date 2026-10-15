package com.nexainsights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NexaInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexaInsightsApplication.class, args);
    }
}
