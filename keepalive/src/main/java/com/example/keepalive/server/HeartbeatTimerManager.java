package com.example.keepalive.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class HeartbeatTimerManager {

    /**
     * 新的心跳要取消之前的定时任务，使用Map快速找到前一个定时任务
     */
    private final Map<String, HeartbeatCheckTimer> timerMap = new ConcurrentHashMap<>();

    private final ThreadPoolExecutor executor;

    private final ScheduledThreadPoolExecutor scheduler;

    /**
     * 两次心跳间隔超过正常值两倍则认为离线
     */
    private final long checkTimeout = 3000 * 2;

    private static final Logger log = LoggerFactory.getLogger(HeartbeatTimerManager.class);

    public HeartbeatTimerManager(ThreadPoolExecutor threadPoolExecutor, ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        this.executor = threadPoolExecutor;
        this.scheduler = scheduledThreadPoolExecutor;
    }

    public void receiveHeartbeat(String serial) {
        // Map中不存在则认为客户端是新上线
        if (!timerMap.containsKey(serial)) {
            executor.execute(() -> {
                online(serial);
            });
        }

        // 更新Map中的定时任务，并取消掉旧的定时任务
        HeartbeatCheckTimer task = new HeartbeatCheckTimer(serial);
        timerMap.merge(serial, task, (oldVal, newVal) -> {
            oldVal.cancel();
            return newVal;
        });
        scheduler.schedule(task, checkTimeout, TimeUnit.MILLISECONDS);
    }

    private void online(String serial) {
        log.info("客户端：{} 上线", serial);
    }

    private void offline(String serial) {
        log.info("客户端：{} 下线", serial);
    }

    private class HeartbeatCheckTimer implements Runnable {

        private volatile boolean isCancel = false;

        private final String serial;

        private HeartbeatCheckTimer(String serial) {
            this.serial = serial;
        }

        public void cancel() {
            isCancel = true;
        }

        @Override
        public void run() {
            if (isCancel) {
                return;
            }

            // 超时未收到新的心跳，认为客户端已下线
            timerMap.remove(serial);
            executor.execute(() -> {
                offline(serial);
            });
        }
    }
}
