package com.example.serverdiscover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer

public class ServerdiscoverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerdiscoverApplication.class, args);
    }

}
