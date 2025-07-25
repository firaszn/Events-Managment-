package com.example.serverdiscover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableEurekaServer
public class ServerdiscoverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerdiscoverApplication.class, args);
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        // This empty configuration ensures that Spring MVC is properly configured
        // for handling actuator endpoints
    }
}
