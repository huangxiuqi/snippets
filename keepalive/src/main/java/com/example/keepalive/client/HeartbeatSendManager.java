package com.example.keepalive.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class HeartbeatSendManager implements ApplicationListener<ContextRefreshedEvent> {

    private final ScheduledThreadPoolExecutor scheduler;

    private final ThreadPoolExecutor executor;

    private final RestTemplate restTemplate;

    private int serverPort;

    public HeartbeatSendManager(ScheduledThreadPoolExecutor scheduledThreadPoolExecutor,
                                ThreadPoolExecutor threadPoolExecutor,
                                RestTemplate restTemplate) {
        this.scheduler = scheduledThreadPoolExecutor;
        this.executor = threadPoolExecutor;
        this.restTemplate = restTemplate;
    }

    @Value("${server.port}")
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void init() {
        executor.execute(() -> {
            try {
                String baseUrl = UriComponentsBuilder
                        .fromHttpUrl("http://localhost:{port}/heartbeat")
                        .buildAndExpand(serverPort)
                        .toUriString();

                HeartbeatClient client1 = new HeartbeatClient("client-1111", baseUrl, scheduler, executor, restTemplate);
                client1.init();

                Thread.sleep(3000);
                HeartbeatClient client2 = new HeartbeatClient("client-2222", baseUrl, scheduler, executor, restTemplate);
                client2.init();

                Thread.sleep(30000);
                client1.init();
                client2.init();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init();
    }
}
