package com.datingsocial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized Server-Sent Events (SSE) notification service.
 * Delivers real-time pushes (new messages, likes, matches, stories, video calls)
 * to authenticated client browsers, replacing continuous polling.
 */
@Service
public class AppEventService {

    private static final Logger logger = LoggerFactory.getLogger(AppEventService.class);

    // User ID -> List of active emitters (supports multiple browser tabs)
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * Subscribe a user to the real-time event stream.
     * Uses a 30-minute timeout; heartbeat pings keep the connection active.
     */
    public SseEmitter subscribe(Long userId) {
        // 30-minute timeout (1,800,000 ms)
        SseEmitter emitter = new SseEmitter(1800000L);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = userEmitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    userEmitters.remove(userId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        });
        emitter.onError(e -> cleanup.run());

        // Send initial connected handshake
        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data("{\"status\":\"ok\"}"));
        } catch (IOException e) {
            cleanup.run();
        }

        logger.debug("User {} subscribed to unified SSE event stream", userId);
        return emitter;
    }

    /**
     * Send event to a specific user across all their open sessions
     */
    public void sendEvent(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * Broadcast an event to all connected users
     */
    public void broadcastEvent(String eventName, Object data) {
        userEmitters.keySet().forEach(userId -> sendEvent(userId, eventName, data));
    }

    /**
     * Send periodic ping comment every 25 seconds to keep HTTP connections alive
     * through cloud proxies, reverse proxies, and browser sleep timeouts.
     */
    @Scheduled(fixedRate = 25000)
    public void sendKeepAlive() {
        for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
            List<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
            }
        }
    }
}
