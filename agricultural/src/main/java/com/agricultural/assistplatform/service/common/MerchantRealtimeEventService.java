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
 * 商家端实时事件推送（SSE）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantRealtimeEventService {

    private final RealtimeWebSocketService realtimeWebSocketService;
    private final Map<Long, Set<SseEmitter>> merchantEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long merchantId) {
        SseEmitter emitter = new SseEmitter(0L);
        merchantEmitters.computeIfAbsent(merchantId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(merchantId, emitter));
        emitter.onTimeout(() -> removeEmitter(merchantId, emitter));
        emitter.onError((ex) -> removeEmitter(merchantId, emitter));

        sendConnected(emitter, merchantId);
        return emitter;
    }

    public void publishRefresh(Long merchantId, String reason, String refNo) {
        realtimeWebSocketService.publishMerchantRefresh(merchantId, reason, refNo);
        if (merchantId == null) return;
        Set<SseEmitter> emitters = merchantEmitters.get(merchantId);
        if (emitters == null || emitters.isEmpty()) return;

        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("reason", reason);
        payload.put("refNo", refNo);
        payload.put("timestamp", System.currentTimeMillis());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(payload));
            } catch (IOException e) {
                removeEmitter(merchantId, emitter);
            }
        }
    }

    private void sendConnected(SseEmitter emitter, Long merchantId) {
        try {
            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("merchantId", merchantId);
            payload.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event().name("connected").data(payload));
        } catch (IOException e) {
            log.warn("SSE connected event send failed, merchantId={}", merchantId);
        }
    }

    private void removeEmitter(Long merchantId, SseEmitter emitter) {
        Set<SseEmitter> emitters = merchantEmitters.get(merchantId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            merchantEmitters.remove(merchantId);
        }
    }
}
