package com.example.eventservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import feign.RequestInterceptor;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FeignClientConfigTest {
    @Autowired
    private RequestInterceptor requestInterceptor;

    @Test
    void contextLoads() {
        assertThat(requestInterceptor).isNotNull();
    }
} 