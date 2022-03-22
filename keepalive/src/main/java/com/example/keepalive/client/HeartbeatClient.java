package com.example.keepalive.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HeartbeatClient {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatClient.class);

    private final ThreadPoolExecutor executor;

    private final ScheduledThreadPoolExecutor scheduler;

    private final RestTemplate restTemplate;

    /**
     * 心跳发送间隔
     */
    private final long timeoutMill = 3000;

    /**
     * 最大发送间隔
     */
    private final long maxDelay = timeoutMill * 10;

    private final AtomicLong delay = new AtomicLong(0);

    private final String clientSerial;

    private final String baseUrl;

    private String heartbeatUrl;

    private final AtomicBoolean isClose = new AtomicBoolean(false);

    private final AtomicInteger sendCount = new AtomicInteger(0);

    public HeartbeatClient(String clientSerial,
                           String baseUrl,
                           ScheduledThreadPoolExecutor scheduledThreadPoolExecutor,
                           ThreadPoolExecutor threadPoolExecutor,
                           RestTemplate restTemplate) {
        this.clientSerial = clientSerial;
        this.baseUrl = baseUrl;
        this.scheduler = scheduledThreadPoolExecutor;
        this.executor = threadPoolExecutor;
        this.restTemplate = restTemplate;
    }

    public void init() {
        isClose.set(false);
        sendCount.set(0);
        executor.execute(() -> {
            log.info("客户端：{} 3秒后启动心跳线程，心跳间隔：{}毫秒，发送5次心跳后停止", clientSerial, timeoutMill);
            heartbeatUrl = baseUrl + "?serial=" + clientSerial;
            scheduler.schedule(new HeartbeatTimerTask(new HeartbeatTask()), 3, TimeUnit.SECONDS);
        });
    }

    /**
     * 发送心跳任务
     */
    private class HeartbeatTask implements Runnable {

        @Override
        public void run() {
            String response = restTemplate.getForObject(heartbeatUrl, String.class);
            log.debug("收到心跳响应：{}", response);

            // 发送5次心跳后停止发送
            if (sendCount.incrementAndGet() >= 5) {
                log.info("客户端：{} 停止发送心跳", clientSerial);
                isClose.set(true);
            }
        }
    }

    /**
     * 心跳定时任务
     */
    private class HeartbeatTimerTask implements Runnable {

        private final Runnable task;

        private HeartbeatTimerTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            if (isClose.get()) {
                return;
            }

            Future<?> future = null;
            try {
                future = executor.submit(task);
                future.get(timeoutMill, TimeUnit.MILLISECONDS);
                delay.set(timeoutMill);
            } catch (TimeoutException e) {
                log.warn("发送心跳超时", e);

                // 下次发送时间延长一倍，直到最大间隔
                long newDelay = Math.max(delay.get() * 2, maxDelay);
                delay.set(newDelay);
            } catch (Throwable e) {
                log.error("发送心跳失败", e);
            } finally {
                if (future != null) {
                    future.cancel(true);
                }
                scheduler.schedule(this, delay.get(), TimeUnit.MILLISECONDS);
            }
        }
    }
}
