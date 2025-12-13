package com.example.remotetests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.remotetests")
public class RemoteIntegrationTestsApplication {
    public static void main(String[] args) {
        SpringApplication.run(RemoteIntegrationTestsApplication.class, args);
    }
}

