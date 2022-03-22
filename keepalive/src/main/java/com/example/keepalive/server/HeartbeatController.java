package com.example.keepalive.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeartbeatController {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatController.class);

    private final HeartbeatTimerManager heartbeatTimerManager;

    public HeartbeatController(HeartbeatTimerManager heartbeatTimerManager) {
        this.heartbeatTimerManager = heartbeatTimerManager;
    }

    @GetMapping("/heartbeat")
    public String heartbeat(String serial) {
        log.info("接收到心跳：{}", serial);
        heartbeatTimerManager.receiveHeartbeat(serial);
        return "hello " + serial;
    }
}
