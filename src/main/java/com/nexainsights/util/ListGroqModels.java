package com.nexainsights.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class ListGroqModels {
    public static void main(String[] args) {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            if (args.length > 0) apiKey = args[0];
            else {
                System.out.println("Especifica la variable de entorno GROQ_API_KEY");
                return;
            }
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange("https://api.groq.com/openai/v1/models", HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            System.out.println("Modelos disponibles en Groq:");
            for (Map<String, Object> m : data) {
                System.out.println("-> " + m.get("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
