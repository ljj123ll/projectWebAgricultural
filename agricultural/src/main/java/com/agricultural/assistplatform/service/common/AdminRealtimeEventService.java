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
 * 管理端实时事件推送（SSE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRealtimeEventService {

    private final RealtimeWebSocketService realtimeWebSocketService;
    private final Map<Long, Set<SseEmitter>> adminEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long adminId) {
        SseEmitter emitter = new SseEmitter(0L);
        adminEmitters.computeIfAbsent(adminId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(adminId, emitter));
        emitter.onTimeout(() -> removeEmitter(adminId, emitter));
        emitter.onError((ex) -> removeEmitter(adminId, emitter));

        sendConnected(emitter, adminId);
        return emitter;
    }

    public void publishRefresh(Long adminId, String reason, String refNo) {
        realtimeWebSocketService.publishAdminRefresh(adminId, reason, refNo);
        if (adminId == null) return;
        Set<SseEmitter> emitters = adminEmitters.get(adminId);
        if (emitters == null || emitters.isEmpty()) return;

        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("reason", reason);
        payload.put("refNo", refNo);
        payload.put("timestamp", System.currentTimeMillis());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(payload));
            } catch (IOException e) {
                removeEmitter(adminId, emitter);
            }
        }
    }

    public void publishRefreshToAll(String reason, String refNo) {
        realtimeWebSocketService.publishAdminRefreshToAll(reason, refNo);
        adminEmitters.keySet().forEach(adminId -> {
            Set<SseEmitter> emitters = adminEmitters.get(adminId);
            if (emitters == null || emitters.isEmpty()) return;

            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("reason", reason);
            payload.put("refNo", refNo);
            payload.put("timestamp", System.currentTimeMillis());

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("refresh").data(payload));
                } catch (IOException e) {
                    removeEmitter(adminId, emitter);
                }
            }
        });
    }

    private void sendConnected(SseEmitter emitter, Long adminId) {
        try {
            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("adminId", adminId);
            payload.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event().name("connected").data(payload));
        } catch (IOException e) {
            log.warn("Admin SSE connected event send failed, adminId={}", adminId);
        }
    }

    private void removeEmitter(Long adminId, SseEmitter emitter) {
        Set<SseEmitter> emitters = adminEmitters.get(adminId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            adminEmitters.remove(adminId);
        }
    }
}
