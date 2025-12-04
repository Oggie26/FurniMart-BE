package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
@EnableFeignClients(basePackages = "com.example.userservice.feign")
public class UserServiceApplication {

	public static void main(String[] args) {
		System.out.println("Timezone: " + TimeZone.getDefault().getID());
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
