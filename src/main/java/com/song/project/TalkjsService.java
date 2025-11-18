package com.song.project;

import com.song.project.TalkjsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TalkjsService {

    @Value("${talkjs.appId}")
    private String appId;

    @Value("${talkjs.secret}")
    private String secret;

    private static final String API_URL_TEMPLATE =
        "https://api.talkjs.com/v1/%s/conversations/%s";

    public boolean deleteConversation(String conversationId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            
            // TalkJS Admin API Basic Auth: username=secret, password=""
            String authStr = secret + ":";
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(authStr.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
            
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            String url = String.format(API_URL_TEMPLATE, appId, conversationId);
            System.out.println("DELETE URL: " + url);  // 로그 확인용
            System.out.println("Secret: " + secret);   // 로그 확인용
            
            ResponseEntity<String> response =
              restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

// 대화 삭제 기능 서비스