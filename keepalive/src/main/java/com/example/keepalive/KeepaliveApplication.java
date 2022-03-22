package com.example.keepalive;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.*;

@SpringBootApplication
public class KeepaliveApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeepaliveApplication.class, args);
    }

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ThreadPoolExecutor-Keepalive-%d")
                .build();
        return new ThreadPoolExecutor(
                4,
                8,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                threadFactory);
    }

    @Bean
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ScheduledThreadPoolExecutor-Keepalive-%d")
                .build();
        return new ScheduledThreadPoolExecutor(2, threadFactory);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
