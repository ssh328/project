package com.song.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Toss Payments 결제 승인 클라이언트
 * 결제 승인 API 호출 제공
 */
@Component
@RequiredArgsConstructor
public class TossPaymentsClient {
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    @Value("${toss.secret-key}")
    private String secretKey;

    /**
     * Toss Payments 결제 승인 요청
     * @param paymentKey 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 결제 승인 응답 (JsonNode)
     */
    public JsonNode confirm(String paymentKey, String orderId, Integer amount) {
        String basicAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("토스 결제 승인 실패: " + e.getResponseBodyAsString(), e);
        }
    }
}
