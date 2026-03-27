package com.agricultural.assistplatform.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 用户端实时事件推送（SSE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRealtimeEventService {

    private final RealtimeWebSocketService realtimeWebSocketService;
    private final Map<Long, Set<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((ex) -> removeEmitter(userId, emitter));

        sendConnected(emitter, userId);
        return emitter;
    }

    public void publishRefresh(Long userId, String reason, String refNo) {
        realtimeWebSocketService.publishUserRefresh(userId, reason, refNo);
        if (userId == null) return;
        Set<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) return;

        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("reason", reason);
        payload.put("refNo", refNo);
        payload.put("timestamp", System.currentTimeMillis());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(payload));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    public void publishRefreshToAll(String reason, String refNo) {
        realtimeWebSocketService.publishUserRefreshToAll(reason, refNo);
        userEmitters.keySet().forEach(userId -> {
            Set<SseEmitter> emitters = userEmitters.get(userId);
            if (emitters == null || emitters.isEmpty()) return;

            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("reason", reason);
            payload.put("refNo", refNo);
            payload.put("timestamp", System.currentTimeMillis());

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("refresh").data(payload));
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    private void sendConnected(SseEmitter emitter, Long userId) {
        try {
            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("userId", userId);
            payload.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event().name("connected").data(payload));
        } catch (IOException e) {
            log.warn("User SSE connected event send failed, userId={}", userId);
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        Set<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }
}
